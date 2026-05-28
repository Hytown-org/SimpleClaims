package com.buuz135.simpleclaims.claim;

import com.buuz135.simpleclaims.claim.component.PlaytimeClaimRewardComponent;
import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyOverride;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.buuz135.simpleclaims.commands.CommandMessages;
import com.buuz135.simpleclaims.config.SimpleClaimsConfig;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlaytimeClaimRewardListener {

    private static final HytaleLogger log = HytaleLogger.forEnclosingClass();
    private static final int PLAYTIME_MINUTES_PER_CLAIM = 10 * 60;
    private static final int MAX_TOTAL_CLAIMS = 50;
    private static final String UNKNOWN_SERVER_ID = "unknown";
    private final boolean playtimeRewards;
    private boolean nexusUnavailableLogged;

    public PlaytimeClaimRewardListener(SimpleClaimsConfig config) {
        this.playtimeRewards = config.getDoPlaytimeRewards();
        log.atInfo().log("SimpleClaims playtime reward integration is active");
    }

    public void refreshPlayer(PlayerRef playerRef) {
        NexusPlayerService playerService = getPlayerService();
        if (playerService == null || playerRef == null) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        log.atInfo().log("requesting Nexus playtime for %s", playerId);
        if (!playtimeRewards) {
            evaluatePlaytime(playerId, 0);
            return;
        }

        playerService.getPlayerStatsAsync(playerId)
                .thenAccept(stats -> {
                    if (stats == null) {
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

        var players = Universe.get().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        List<UUID> playerIds = new ArrayList<>(players.size());
        for (PlayerRef player : players) {
            playerIds.add(player.getUuid());
        }

        log.atInfo().log("requesting Nexus playtime batch for %d player(s)", playerIds.size());

        if (!playtimeRewards) {
            for (UUID playerId : playerIds) {
                evaluatePlaytime(playerId, 0);
            }
            return;
        }

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
                continue;
            }
            evaluatePlaytime(playerId, stats.getPlaytimeMinutes());
        }
    }

    private void evaluatePlaytime(UUID playerId, int playtimeMinutes) {
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> awardPlaytimeClaims(store, ref, playerRef, playtimeMinutes));
    }

    private void awardPlaytimeClaims(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, int playtimeMinutes) {
        if (!ref.isValid()) {
            return;
        }

        String serverId = getServerId();

        var party = ClaimManager.getInstance().getPartyFromPlayer(playerRef.getUuid());
        if (party == null) {
            return;
        }

        int currentThreshold = playtimeMinutes / PLAYTIME_MINUTES_PER_CLAIM;
        if (currentThreshold <= 0) {
            return;
        }

        PlaytimeClaimRewardComponent reward = store.getComponent(ref, PlaytimeClaimRewardComponent.getComponentType());
        if (reward != null && reward.hasLegacyReward()) {
            repairLegacyReward(store, ref, playerRef, party, reward, serverId);
        }

        if (reward == null) {
            reward = new PlaytimeClaimRewardComponent();
        }

        int lastRewardedThreshold = reward.getRewardedPlaytimeThreshold(serverId);
        int thresholdsToGrant = currentThreshold - lastRewardedThreshold;
        if (thresholdsToGrant <= 0) {
            store.putComponent(ref, PlaytimeClaimRewardComponent.getComponentType(), reward);
            return;
        }

        int remainingClaims = Math.max(0, MAX_TOTAL_CLAIMS - party.getMaxClaimAmount());
        if (remainingClaims <= 0) {
            store.putComponent(ref, PlaytimeClaimRewardComponent.getComponentType(), reward);
            return;
        }

        int claimsToGrant = Math.min(thresholdsToGrant, remainingClaims);
        int updatedThreshold = lastRewardedThreshold + claimsToGrant;
        party.setOverride(new PartyOverride(PartyOverrides.PLAYTIME_CLAIM_CHUNKS, new PartyOverride.PartyOverrideValue("integer", party.getPlaytimeBonusChunks() + claimsToGrant)));
        ClaimManager.getInstance().saveParty(party);

        reward.setRewardedPlaytimeThreshold(serverId, updatedThreshold);
        store.putComponent(ref, PlaytimeClaimRewardComponent.getComponentType(), reward);

        playerRef.sendMessage(CommandMessages.PLAYTIME_CLAIMS_AWARDED
                .param("claims", claimsToGrant)
                .param("max_claims", party.getMaxClaimAmount())
                .param("playtime_hours", playtimeMinutes / 60));
        log.atInfo().log(
                "awarded %d playtime claim(s) to %s for party %s on %s at %d minutes (threshold %d -> %d, maxClaims=%d)",
                claimsToGrant,
                playerRef.getUuid(),
                party.getId(),
                serverId,
                playtimeMinutes,
                lastRewardedThreshold,
                updatedThreshold,
                party.getMaxClaimAmount()
        );
    }

    private void repairLegacyReward(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, PartyInfo party, PlaytimeClaimRewardComponent reward, String serverId) {
        int legacyThreshold = reward.getLegacyLastRewardedPlaytimeThreshold();
        int rollback = Math.min(legacyThreshold, party.getBonusChunks());
        log.atInfo().log(
                "repairing legacy playtime reward for %s on %s (legacyThreshold=%d, sharedBonus=%d, rollback=%d)",
                playerRef.getUuid(), serverId, legacyThreshold, party.getBonusChunks(), rollback
        );

        if (rollback > 0) {
            party.setOverride(new PartyOverride(PartyOverrides.BONUS_CLAIM_CHUNKS, new PartyOverride.PartyOverrideValue("integer", party.getBonusChunks() - rollback)));
            ClaimManager.getInstance().saveParty(party);
        }

        reward.clearLegacyReward();
        store.putComponent(ref, PlaytimeClaimRewardComponent.getComponentType(), reward);
    }

    private NexusPlayerService getPlayerService() {
        try {
            NexusPlugin nexus = NexusPlugin.getInstance();
            if (nexus == null || nexus.getPlayerService() == null) {
                logNexusUnavailable("HytownNexus player service unavailable; playtime rewards disabled");
                return null;
            }
            nexusUnavailableLogged = false;
            return nexus.getPlayerService();
        } catch (NoClassDefFoundError error) {
            logNexusUnavailable("HytownNexus classes unavailable; playtime rewards disabled");
            return null;
        }
    }

    private void logNexusUnavailable(String message) {
        if (nexusUnavailableLogged) {
            return;
        }
        nexusUnavailableLogged = true;
        log.atWarning().log(message);
    }

    private String getServerId() {
        try {
            NexusPlugin nexus = NexusPlugin.getInstance();
            if (nexus == null || nexus.getConfig() == null || nexus.getConfig().getServerName() == null || nexus.getConfig().getServerName().isBlank()) {
                return UNKNOWN_SERVER_ID;
            }
            return nexus.getConfig().getServerName();
        } catch (NoClassDefFoundError error) {
            return UNKNOWN_SERVER_ID;
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