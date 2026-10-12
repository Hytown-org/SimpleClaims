package com.buuz135.simpleclaims.worldevents;

import com.buuz135.simpleclaims.Main;
import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.chunk.ChunkInfo;
import com.hypixel.hytale.builtin.adventure.worldevents.util.Vector3iList;
import com.hypixel.hytale.math.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;

/**
 * Keeps world event spawn candidates away from claims, based on the WorldEventClaimBufferChunks config option.
 */
public final class WorldEventClaimFilter {

    private WorldEventClaimFilter() {
    }

    public static void filter(@Nonnull Vector3iList candidates, String worldName) {
        int bufferChunks = Main.CONFIG.get().getWorldEventClaimBufferChunks();
        if (bufferChunks < 0 || candidates.empty()) return;

        if (Arrays.asList(Main.CONFIG.get().getFullWorldProtection()).contains(worldName)) {
            candidates.clear();
            return;
        }

        Map<String, ChunkInfo> claims = ClaimManager.getInstance().getChunks().get(worldName);
        if (claims == null || claims.isEmpty()) return;

        removeCandidatesNearClaims(candidates, claims, bufferChunks);
    }

    /**
     * Removes every candidate whose chunk is within {@code bufferChunks} chunks of a claimed chunk. Only the candidate
     * is moved vertically when placing content, so a buffer of 1 keeps any prefab reaching up to 32 blocks from its
     * origin out of claims.
     */
    public static void removeCandidatesNearClaims(@Nonnull Vector3iList candidates, @Nonnull Map<String, ChunkInfo> claims, int bufferChunks) {
        // Wilderness returns several candidates per chunk section and several sections per column, so cache per column.
        var blockedColumns = new Long2BooleanOpenHashMap();
        var position = new Vector3i();
        for (int i = 0; i < candidates.size(); ) {
            candidates.get(i, position);
            int chunkX = ChunkUtil.chunkCoordinate(position.x);
            int chunkZ = ChunkUtil.chunkCoordinate(position.z);
            long column = ChunkUtil.indexChunk(chunkX, chunkZ);

            boolean blocked;
            if (blockedColumns.containsKey(column)) {
                blocked = blockedColumns.get(column);
            } else {
                blocked = isNearClaim(claims, chunkX, chunkZ, bufferChunks);
                blockedColumns.put(column, blocked);
            }

            if (blocked) {
                // Swaps the last candidate into this index, so don't advance.
                candidates.remove(i);
            } else {
                i++;
            }
        }
    }

    private static boolean isNearClaim(Map<String, ChunkInfo> claims, int chunkX, int chunkZ, int bufferChunks) {
        for (int dx = -bufferChunks; dx <= bufferChunks; dx++) {
            for (int dz = -bufferChunks; dz <= bufferChunks; dz++) {
                if (claims.containsKey(ChunkInfo.formatCoordinates(chunkX + dx, chunkZ + dz))) return true;
            }
        }
        return false;
    }
}
