package com.buuz135.simpleclaims.commands.subcommand.chunk.op;

import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.commands.CommandMessages;
import com.buuz135.simpleclaims.commands.subcommand.chunk.ClaimChunkCommand;
import com.buuz135.simpleclaims.commands.subcommand.chunk.UnclaimChunkCommand;
import com.buuz135.simpleclaims.gui.ChunkInfoGui;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class OpChunkGuiCommand extends AbstractAsyncCommand {

    public OpChunkGuiCommand() {
        super("admin-chunk", "Opens the chunk claim gui in op mode to claim chunks using the selected admin party");
        this.requirePermission(CommandMessages.ADMIN_PERM + "admin-chunk");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (sender instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player == null) return;
                    var position = store.getComponent(ref, TransformComponent.getComponentType());
                    player.getPageManager().openCustomPage(ref, store, new ChunkInfoGui(playerRef, player.getWorld().getName(), ChunkUtil.chunkCoordinate(position.getPosition().x()), ChunkUtil.chunkCoordinate(position.getPosition().z()), true));
                }, world);
            } else {
                commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}
