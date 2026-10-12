package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.joml.Vector3i;

/**
 * Update 7 removed {@code World#getBlockType}; blocks are now read from their chunk section.
 */
public final class WorldBlocks {

    private WorldBlocks() {
    }

    /**
     * @return the block type at the position, or {@code null} if its section isn't in memory.
     */
    @NullableDecl
    public static BlockType getBlockType(@NonNullDecl World world, int x, int y, int z) {
        var chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) return null;
        var section = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
        return section == null ? null : BlockType.getAssetMap().getAsset(section.get(x, y, z));
    }

    @NullableDecl
    public static BlockType getBlockType(@NonNullDecl World world, @NonNullDecl Vector3i pos) {
        return getBlockType(world, pos.x, pos.y, pos.z);
    }
}
