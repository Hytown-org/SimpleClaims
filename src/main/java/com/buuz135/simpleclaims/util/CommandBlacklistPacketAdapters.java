package com.buuz135.simpleclaims.util;

import com.buuz135.simpleclaims.Main;
import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.commands.CommandMessages;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CommandBlacklistPacketAdapters {

    private CommandBlacklistPacketAdapters() {}

    private static PacketFilter installed;

    public static void install() {
        if (installed != null) return;

        installed = PacketAdapters.registerInbound((PlayerRef playerRef, Packet clientPacket) -> {
            if (!(clientPacket instanceof ChatMessage chatMessage)) return false;

            String token = extractCommandToken(chatMessage.message);
            if (token == null || !isBlacklisted(token)) return false;

            if (!shouldBlockInClaim(playerRef)) return false;

            playerRef.sendMessage(CommandMessages.COMMAND_BLOCKED_IN_CLAIM);
            return true;
        });
    }

    public static void uninstall() {
        if (installed != null) {
            PacketAdapters.deregisterInbound(installed);
            installed = null;
        }
    }

    private static String extractCommandToken(String message) {
        if (message == null) return null;

        String trimmed = message.trim();
        if (!trimmed.startsWith("/")) return null;

        String withoutSlash = trimmed.substring(1).trim();
        if (withoutSlash.isEmpty()) return null;

        int spaceIndex = withoutSlash.indexOf(' ');
        String token = spaceIndex == -1 ? withoutSlash : withoutSlash.substring(0, spaceIndex);
        return normalizeToken(token);
    }

    private static String normalizeToken(String token) {
        if (token == null) return "";
        String normalized = token.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static boolean isBlacklisted(String commandToken) {
        String[] blocked = Main.CONFIG.get().getBlockedCommandsInClaims();
        if (blocked == null || blocked.length == 0) return false;

        for (String entry : blocked) {
            String normalizedEntry = normalizeToken(entry);
            if (normalizedEntry.isEmpty()) continue;

            if (normalizedEntry.endsWith("*")) {
                String prefix = normalizedEntry.substring(0, normalizedEntry.length() - 1);
                if (prefix.isEmpty() || commandToken.startsWith(prefix)) {
                    return true;
                }
            } else if (commandToken.equals(normalizedEntry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldBlockInClaim(PlayerRef playerRef) {
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) return false;

        World world = Universe.get().getWorld(worldUuid);
        if (world == null) return false;

        return CompletableFuture.supplyAsync(() -> {
            var ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) return false;

            var store = ref.getStore();
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return false;

            if (ClaimManager.getInstance().getAdminClaimOverrides().contains(playerRef.getUuid())) return false;

            var position = playerRef.getTransform().getPosition();
            int blockX = (int) Math.floor(position.getX());
            int blockZ = (int) Math.floor(position.getZ());
            String dimension = player.getWorld().getName();

            var chunk = ClaimManager.getInstance().getChunkRawCoords(dimension, blockX, blockZ);
            if (chunk == null) return false;

            var party = ClaimManager.getInstance().getPartyById(chunk.getPartyOwner());
            if (party == null) return false;

            return party.isCommandBlacklistEnabled();
        }, world).join();
    }
}
