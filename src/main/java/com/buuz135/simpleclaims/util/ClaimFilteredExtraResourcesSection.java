package com.buuz135.simpleclaims.util;

import com.buuz135.simpleclaims.systems.tick.CraftingUiQuantitiesSystem;
import com.hypixel.hytale.builtin.crafting.window.BenchWindow;
import com.hypixel.hytale.protocol.ItemQuantity;
import com.hypixel.hytale.server.core.entity.entities.player.windows.MaterialExtraResourcesSection;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Collections;
import java.util.List;

/**
 * Drop-in replacement for a {@link BenchWindow}'s extra-resources section that applies the claim
 * filter at the moment the game rebuilds the nearby-chest list.
 * <p>
 * Every craft click ends with {@code invalidateExtraResources()}, and the next
 * {@code BenchWindow#getExtraResourcesSection()} call rebuilds the section synchronously via
 * {@code CraftingManager.feedExtraResourcesSection} with every nearby chest, unfiltered — inside
 * the same {@code handleAction} call that consumes the materials, so no tick system can re-filter
 * in time. Bench tier upgrades read the section the same way on completion. By overriding the
 * setters that rebuild pushes through, the unfiltered data is replaced with the claim-allowed
 * chests at that instant.
 */
public class ClaimFilteredExtraResourcesSection extends MaterialExtraResourcesSection {

    private final BenchWindow window;

    public ClaimFilteredExtraResourcesSection(BenchWindow window) {
        this.window = window;
    }

    @Override
    public void setItemContainer(ItemContainer unfiltered) {
        super.setItemContainer(CraftingUiQuantitiesSystem.buildUiContainer(allowedChests()));
    }

    @Override
    public void setExtraMaterials(ItemQuantity[] unfiltered) {
        super.setExtraMaterials(computeAllowedCounts());
    }

    /**
     * Trusted path for SimpleClaims' own systems, which pass data already claim-filtered.
     */
    public void applyFiltered(ItemContainer container, ItemQuantity[] counts) {
        super.setItemContainer(container);
        super.setExtraMaterials(counts);
        setValid(true);
    }

    // Resolves the viewing player's world; null when the window has no valid player (then we
    // fail closed: no chests, crafting falls back to the player's own inventory).
    private World resolveWorld() {
        PlayerRef playerRef = window.getPlayerRef();
        if (playerRef == null) return null;
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return null;
        return ref.getStore().getExternalData().getWorld();
    }

    private List<ItemContainer> allowedChests() {
        World world = resolveWorld();
        if (world == null) return Collections.emptyList();
        return BenchChestCache.getAllowedChests(world, window.getPlayerRef(), window.getX(), window.getY(), window.getZ());
    }

    private ItemQuantity[] computeAllowedCounts() {
        World world = resolveWorld();
        if (world == null) return new ItemQuantity[0];

        var block = WorldBlocks.getBlockType(world, window.getX(), window.getY(), window.getZ());
        var bench = block == null ? null : block.getBench();
        if (bench == null) return new ItemQuantity[0];

        var benchBlock = WindowReflection.getBenchBlock(window);
        List<ItemContainer> chests = BenchChestCache.getAllowedChests(world, window.getPlayerRef(), window.getX(), window.getY(), window.getZ());
        return CraftingUiQuantitiesSystem.computeCounts(bench, benchBlock.getTierLevel(), chests);
    }
}
