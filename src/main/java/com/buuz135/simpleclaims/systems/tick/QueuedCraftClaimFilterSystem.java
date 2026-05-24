package com.buuz135.simpleclaims.systems.tick;

import com.buuz135.simpleclaims.util.BenchChestCache;
import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.EmptyItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class QueuedCraftClaimFilterSystem extends EntityTickingSystem<EntityStore> {

    private static final Field CM_X;
    private static final Field CM_Y;
    private static final Field CM_Z;
    private static final Field CM_BLOCKTYPE;
    private static final Field CM_QUEUE;

    private static final Class<?> JOB_CLASS;
    private static final Field JOB_INPUT_CONTAINER;
    private static final Field JOB_WINDOW;
    private static final Field JOB_INPUT_REMOVAL_TYPE;

    static {
        try {
            CM_X = CraftingManager.class.getDeclaredField("x");
            CM_Y = CraftingManager.class.getDeclaredField("y");
            CM_Z = CraftingManager.class.getDeclaredField("z");
            CM_BLOCKTYPE = CraftingManager.class.getDeclaredField("blockType");
            CM_QUEUE = CraftingManager.class.getDeclaredField("queuedCraftingJobs");
            CM_X.setAccessible(true);
            CM_Y.setAccessible(true);
            CM_Z.setAccessible(true);
            CM_BLOCKTYPE.setAccessible(true);
            CM_QUEUE.setAccessible(true);

            JOB_CLASS = Class.forName("com.hypixel.hytale.builtin.crafting.component.CraftingManager$CraftingJob");
            JOB_INPUT_CONTAINER = JOB_CLASS.getDeclaredField("inputItemContainer");
            JOB_WINDOW = JOB_CLASS.getDeclaredField("window");
            JOB_INPUT_REMOVAL_TYPE = JOB_CLASS.getDeclaredField("inputRemovalType");
            JOB_INPUT_CONTAINER.setAccessible(true);
            JOB_WINDOW.setAccessible(true);
            JOB_INPUT_REMOVAL_TYPE.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init reflection for crafting job filtering", e);
        }
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (!ref.isValid()) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        CraftingManager cm = store.getComponent(ref, CraftingManager.getComponentType());
        if (player == null || playerRef == null || cm == null) return;

        int bx, by, bz;
        final BlockingQueue<?> queue;
        try {
            if (CM_BLOCKTYPE.get(cm) == null) return;

            bx = (int) CM_X.get(cm);
            by = (int) CM_Y.get(cm);
            bz = (int) CM_Z.get(cm);

            queue = (BlockingQueue<?>) CM_QUEUE.get(cm);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CraftingManager state", e);
        }

        if (queue.isEmpty()) return;

        World world = player.getWorld();

        var chests = BenchChestCache.getAllowedChests(world, playerRef, bx, by, bz);
        ItemContainer inv = InventoryComponent.getCombined(store, ref, InventoryComponent.BACKPACK_STORAGE_HOTBAR);
        ItemContainer allowedInput = buildAllowedInput(inv, chests);

        for (Object job : queue) {
            if (!JOB_CLASS.isInstance(job)) continue;
            try {
                Object windowObj = JOB_WINDOW.get(job);
                if (!(windowObj instanceof com.hypixel.hytale.builtin.crafting.window.SimpleCraftingWindow)) {
                    continue;
                }
                Object removalType = JOB_INPUT_REMOVAL_TYPE.get(job);
                if (removalType != null && !removalType.toString().equals("NORMAL")) {
                    continue;
                }
                JOB_INPUT_CONTAINER.set(job, allowedInput);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to read/replace CraftingJob fields", e);
            }
        }
    }

    private static ItemContainer buildAllowedInput(ItemContainer playerInvCombined, List<ItemContainer> chests) {
        ItemContainer chestCombined;
        if (chests.isEmpty()) chestCombined = EmptyItemContainer.INSTANCE;
        else chestCombined = new CombinedItemContainer(chests.toArray(ItemContainer[]::new));
        return new CombinedItemContainer(playerInvCombined, chestCombined);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}