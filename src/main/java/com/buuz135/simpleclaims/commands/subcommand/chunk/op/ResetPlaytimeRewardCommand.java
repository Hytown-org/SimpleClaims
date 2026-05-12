package com.buuz135.simpleclaims.commands.subcommand.chunk.op;

import com.buuz135.simpleclaims.claim.component.PlaytimeClaimRewardComponent;
import com.buuz135.simpleclaims.commands.CommandMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class ResetPlaytimeRewardCommand extends AbstractAsyncPlayerCommand {

    public ResetPlaytimeRewardCommand() {
        super("debug-reset-playtime-reward", "Reset your playtime reward tracking component");
        this.requirePermission(CommandMessages.ADMIN_PERM + "debug-reset-playtime-reward");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        PlaytimeClaimRewardComponent reward = store.getComponent(ref, PlaytimeClaimRewardComponent.getComponentType());
        if (reward == null) {
            playerRef.sendMessage(CommandMessages.PLAYTIME_REWARD_RESET_NOTHING);
            return CompletableFuture.completedFuture(null);
        }

        store.removeComponent(ref, PlaytimeClaimRewardComponent.getComponentType());
        playerRef.sendMessage(CommandMessages.PLAYTIME_REWARD_RESET);
        return CompletableFuture.completedFuture(null);
    }
}
