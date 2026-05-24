package com.buuz135.simpleclaims.commands.subcommand.chunk;

import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.buuz135.simpleclaims.commands.CommandMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class UnclaimChunkCommand extends AbstractAsyncCommand {

    public UnclaimChunkCommand() {
        super("unclaim", "Unclaims the chunk where you are");
        this.requirePermission(CommandMessages.BASE_PERM + "unclaim");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef != null) {
                        var position = playerRef.getTransform().getPosition();
                        var party = ClaimManager.getInstance().getPartyFromPlayer(playerRef.getUuid());
                        if (party == null) {
                            playerRef.sendMessage(CommandMessages.NOT_IN_A_PARTY);
                            return;
                        }
                        if (!party.hasPermission(playerRef.getUuid(), PartyOverrides.PARTY_PROTECTION_CLAIM_UNCLAIM)) {
                            playerRef.sendMessage(CommandMessages.NO_PERMISSION);
                            return;
                        }
                        var chunk = ClaimManager.getInstance().getChunkRawCoords(player.getWorld().getName(), (int) position.x(), (int) position.z());
                        if (chunk == null) {
                            playerRef.sendMessage(CommandMessages.NOT_CLAIMED);
                            return;
                        }
                        if (!chunk.getPartyOwner().equals(party.getId())) {
                            playerRef.sendMessage(CommandMessages.NOT_YOUR_CLAIM);
                            return;
                        }
                        ClaimManager.getInstance().unclaimRawCoords(player.getWorld().getName(), (int) position.x(), (int) position.z());
                        ClaimManager.getInstance().queueMapUpdate(player.getWorld(), chunk.getChunkX(), chunk.getChunkZ());
                        playerRef.sendMessage(CommandMessages.UNCLAIMED);
                    }
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
