package com.buuz135.simpleclaims.interactions;

import com.buuz135.simpleclaims.util.ClaimActorResolver;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChangeBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.joml.Vector3i;

public class ClaimChangeBlockInteraction extends ChangeBlockInteraction {

    public static final BuilderCodec<ClaimChangeBlockInteraction> CUSTOM_CODEC = BuilderCodec.builder(ClaimChangeBlockInteraction.class, ClaimChangeBlockInteraction::new, ChangeBlockInteraction.CODEC).build();

    @Override
    protected void interactWithBlock(@NonNullDecl World world, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NullableDecl ItemStack heldItemStack, @NonNullDecl Vector3i targetBlock, @NonNullDecl CooldownHandler cooldownHandler) {
        if (canBreakBlock(context, world, targetBlock)) {
            super.interactWithBlock(world, commandBuffer, type, context, heldItemStack, targetBlock, cooldownHandler);
        } else {
            cancelInteraction(context);
        }

    }

    @Override
    protected void simulateInteractWithBlock(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NullableDecl ItemStack itemInHand, @NonNullDecl World world, @NonNullDecl Vector3i targetBlock) {
        if (canBreakBlock(context, world, targetBlock)) {
            super.simulateInteractWithBlock(type, context, itemInHand, world, targetBlock);
        } else {
            cancelInteraction(context);
        }
    }

    private static boolean canBreakBlock(@NonNullDecl InteractionContext context, @NonNullDecl World world, @NonNullDecl Vector3i targetBlock) {
        var blockType = world.getBlockType(targetBlock);
        var blockId = blockType != null ? blockType.getId() : "";
        return ClaimActorResolver.isBlockBreakAllowed(ClaimActorResolver.resolvePlayerUuid(context), world.getName(), targetBlock.x(), targetBlock.z(), blockId);
    }

    private static void cancelInteraction(@NonNullDecl InteractionContext context) {
        context.getState().state = InteractionState.Failed;
        InteractionManager manager = context.getInteractionManager();
        if (manager != null && context.getChain() != null) {
            manager.cancelChains(context.getChain());
        }
    }
}
