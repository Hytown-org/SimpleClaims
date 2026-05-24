package com.buuz135.simpleclaims.commands.subcommand.chunk.op;

import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.component.PlaytimeClaimRewardComponent;
import com.buuz135.simpleclaims.claim.party.PartyOverride;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.buuz135.simpleclaims.commands.CommandMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytown.nexus.NexusPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class ResetPlaytimeRewardCommand extends AbstractAsyncPlayerCommand {

    private static final String UNKNOWN_SERVER_ID = "unknown";

    public ResetPlaytimeRewardCommand() {
        super("debug-reset-playtime-reward", "Reset your playtime reward tracking component");
        this.requirePermission(CommandMessages.ADMIN_PERM + "debug-reset-playtime-reward");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        String serverId = getServerId();
        PlaytimeClaimRewardComponent reward = store.getComponent(ref, PlaytimeClaimRewardComponent.getComponentType());
        var party = ClaimManager.getInstance().getPartyFromPlayer(playerRef.getUuid());
        boolean removedServerEntry = false;
        boolean removedPlaytimeBonus = false;

        if (reward != null) {
            removedServerEntry = reward.hasServer(serverId);
            reward.removeServer(serverId);
            if (reward.hasAnyServer() || reward.hasLegacyReward()) {
                store.putComponent(ref, PlaytimeClaimRewardComponent.getComponentType(), reward);
            } else {
                store.removeComponent(ref, PlaytimeClaimRewardComponent.getComponentType());
            }
        }

        if (party != null && party.getPlaytimeBonusChunks() > 0) {
            party.setOverride(new PartyOverride(PartyOverrides.PLAYTIME_CLAIM_CHUNKS, new PartyOverride.PartyOverrideValue("integer", 0)));
            ClaimManager.getInstance().saveParty(party);
            removedPlaytimeBonus = true;
        }

        if (!removedServerEntry && !removedPlaytimeBonus) {
            playerRef.sendMessage(CommandMessages.PLAYTIME_REWARD_RESET_NOTHING);
            return CompletableFuture.completedFuture(null);
        }

        playerRef.sendMessage(CommandMessages.PLAYTIME_REWARD_RESET);
        return CompletableFuture.completedFuture(null);
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
}
