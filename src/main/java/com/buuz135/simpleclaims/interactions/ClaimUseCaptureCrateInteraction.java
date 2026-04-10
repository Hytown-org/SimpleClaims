package com.buuz135.simpleclaims.interactions;

import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.hypixel.hytale.builtin.adventure.farming.interactions.UseCaptureCrateInteraction;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ClaimUseCaptureCrateInteraction extends UseCaptureCrateInteraction {

    @Nonnull
    public static final BuilderCodec<ClaimUseCaptureCrateInteraction> CUSTOM_CODEC =
            BuilderCodec.builder(ClaimUseCaptureCrateInteraction.class, ClaimUseCaptureCrateInteraction::new, UseCaptureCrateInteraction.CODEC)
                    .build();

    @Override
    protected void tick0(boolean serverSide, float deltaTime, InteractionType type,
                         InteractionContext context, CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player != null && playerRef != null) {
            // use the npc's position for capture, player's position for release
            Ref<EntityStore> targetRef = context.getTargetEntity();
            TransformComponent transform = null;
            if (targetRef != null && targetRef.isValid()) {
                transform = store.getComponent(targetRef, TransformComponent.getComponentType());
            }
            if (transform == null) {
                transform = store.getComponent(ref, TransformComponent.getComponentType());
            }

            if (transform != null) {
                int blockX = (int) transform.getPosition().x;
                int blockZ = (int) transform.getPosition().z;

                if (!ClaimManager.getInstance().isAllowedToInteract(
                        playerRef.getUuid(), player.getWorld().getName(),
                        blockX, blockZ,
                        PartyInfo::isBlockInteractEnabled,
                        PartyOverrides.PARTY_PROTECTION_INTERACT)) {
                    context.getState().state = InteractionState.Failed;
                    InteractionManager manager = context.getInteractionManager();
                    if (manager != null && context.getChain() != null) {
                        manager.cancelChains(context.getChain());
                    }
                    return;
                }
            }
        }

        super.tick0(serverSide, deltaTime, type, context, cooldownHandler);
    }
}
