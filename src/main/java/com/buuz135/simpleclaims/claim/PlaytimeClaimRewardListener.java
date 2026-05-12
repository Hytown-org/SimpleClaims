package com.buuz135.simpleclaims.claim;

import com.buuz135.simpleclaims.claim.component.PlaytimeClaimRewardComponent;
import com.buuz135.simpleclaims.claim.party.PartyOverride;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.buuz135.simpleclaims.commands.CommandMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytown.nexus.NexusPlugin;
import com.hytown.nexus.dto.PlayerStatsResponse;
import com.hytown.nexus.service.NexusPlayerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlaytimeClaimRewardListener {

    private static final HytaleLogger log = HytaleLogger.forEnclosingClass();
    private static final int PLAYTIME_MINUTES_PER_CLAIM = 10 * 60;
    private static final int MAX_TOTAL_CLAIMS = 50;

    public PlaytimeClaimRewardListener() {
        log.atInfo().log("SimpleClaims playtime reward integration is active");
    }

    public void refreshPlayer(PlayerRef playerRef) {
        NexusPlayerService playerService = getPlayerService();
        if (playerService == null || playerRef == null) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        log.atInfo().log("requesting Nexus playtime for %s", playerId);
        playerService.getPlayerStatsAsync(playerId)
                .thenAccept(stats -> {
                    if (stats == null) {
                        log.atInfo().log("Nexus returned no playtime stats for %s", playerId);
                        return;
                    }
                    evaluatePlaytime(playerId, stats.getPlaytimeMinutes());
                })
                .exceptionally(error -> {
                    log.atWarning().log("failed to refresh Nexus playtime for %s: %s", playerId, describeError(error));
                    return null;
                });
    }

    public void refreshOnlinePlayers() {
        NexusPlayerService playerService = getPlayerService();
        if (playerService == null) {
            return;
        }

        List<PlayerRef> players = Universe.get().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        List<UUID> playerIds = new ArrayList<>(players.size());
        for (PlayerRef player : players) {
            playerIds.add(player.getUuid());
        }

        log.atInfo().log("requesting Nexus playtime batch for %d player(s)", playerIds.size());
        playerService.getPlayerStatsBatchAsync(playerIds)
                .thenAccept(statsByPlayer -> applyBatchPlaytime(playerIds, statsByPlayer))
                .exceptionally(error -> {
                    log.atWarning().log("failed to refresh Nexus playtime batch: %s", describeError(error));
                    return null;
                });
    }

    private void applyBatchPlaytime(List<UUID> playerIds, Map<UUID, PlayerStatsResponse> statsByPlayer) {
        for (UUID playerId : playerIds) {
            PlayerStatsResponse stats = statsByPlayer.get(playerId);
            if (stats == null) {
                log.atInfo().log("Nexus returned no playtime stats for %s in batch", playerId);
                continue;
            }
            evaluatePlaytime(playerId, stats.getPlaytimeMinutes());
        }
    }

    private void evaluatePlaytime(UUID playerId, int playtimeMinutes) {
        log.atInfo().log("received Nexus playtime for %s: %d minutes", playerId, playtimeMinutes);
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null) {
            log.atInfo().log("skipping playtime reward for %s because the player is offline", playerId);
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            log.atInfo().log("skipping playtime reward for %s because the player has no valid entity ref", playerId);
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> awardPlaytimeClaims(store, ref, playerRef, playtimeMinutes));
    }

    private void awardPlaytimeClaims(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, int playtimeMinutes) {
        if (!ref.isValid()) {
            log.atInfo().log("skipping playtime reward for %s because the entity ref became invalid", playerRef.getUuid());
            return;
        }

        var party = ClaimManager.getInstance().getPartyFromPlayer(playerRef.getUuid());
        if (party == null) {
            log.atInfo().log("skipping playtime reward for %s because they are not in a party", playerRef.getUuid());
            return;
        }

        int currentThreshold = playtimeMinutes / PLAYTIME_MINUTES_PER_CLAIM;
        if (currentThreshold <= 0) {
            log.atInfo().log("skipping playtime reward for %s because %d minutes has no 10h threshold", playerRef.getUuid(), playtimeMinutes);
            return;
        }

        PlaytimeClaimRewardComponent reward = store.getComponent(ref, PlaytimeClaimRewardComponent.getComponentType());
        int lastRewardedThreshold = reward != null ? reward.getLastRewardedPlaytimeThreshold() : 0;
        int thresholdsToGrant = currentThreshold - lastRewardedThreshold;
        if (thresholdsToGrant <= 0) {
            log.atInfo().log(
                    "evaluated playtime reward for %s with no new thresholds (current=%d, lastRewarded=%d)",
                    playerRef.getUuid(), currentThreshold, lastRewardedThreshold
            );
            return;
        }

        int remainingClaims = Math.max(0, MAX_TOTAL_CLAIMS - party.getMaxClaimAmount());
        if (remainingClaims <= 0) {
            log.atInfo().log(
                    "skipping playtime reward for %s because party %s is at the %d claim cap (currentMax=%d)",
                    playerRef.getUuid(), party.getId(), MAX_TOTAL_CLAIMS, party.getMaxClaimAmount()
            );
            return;
        }

        int claimsToGrant = Math.min(thresholdsToGrant, remainingClaims);
        int updatedThreshold = lastRewardedThreshold + claimsToGrant;
        party.setOverride(new PartyOverride(PartyOverrides.BONUS_CLAIM_CHUNKS, new PartyOverride.PartyOverrideValue("integer", party.getBonusChunks() + claimsToGrant)));
        ClaimManager.getInstance().saveParty(party);

        if (reward == null) {
            store.putComponent(ref, PlaytimeClaimRewardComponent.getComponentType(), new PlaytimeClaimRewardComponent(updatedThreshold));
        } else {
            reward.setLastRewardedPlaytimeThreshold(updatedThreshold);
            store.putComponent(ref, PlaytimeClaimRewardComponent.getComponentType(), reward);
        }

        playerRef.sendMessage(CommandMessages.PLAYTIME_CLAIMS_AWARDED
                .param("claims", claimsToGrant)
                .param("max_claims", party.getMaxClaimAmount())
                .param("playtime_hours", playtimeMinutes / 60));
        log.atInfo().log(
                "awarded %d playtime claim(s) to %s for party %s at %d minutes (threshold %d -> %d, maxClaims=%d)",
                claimsToGrant,
                playerRef.getUuid(),
                party.getId(),
                playtimeMinutes,
                lastRewardedThreshold,
                updatedThreshold,
                party.getMaxClaimAmount()
        );
    }

    private NexusPlayerService getPlayerService() {
        try {
            NexusPlugin nexus = NexusPlugin.getInstance();
            if (nexus == null || nexus.getPlayerService() == null) {
                log.atWarning().log("HytownNexus player service unavailable; playtime rewards disabled");
                return null;
            }
            return nexus.getPlayerService();
        } catch (NoClassDefFoundError error) {
            log.atWarning().log("HytownNexus classes unavailable; playtime rewards disabled");
            return null;
        }
    }

    private String describeError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
