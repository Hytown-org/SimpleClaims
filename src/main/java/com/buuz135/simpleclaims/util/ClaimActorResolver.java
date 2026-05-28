package com.buuz135.simpleclaims.util;

import com.buuz135.simpleclaims.Main;
import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;

public final class ClaimActorResolver {

    private ClaimActorResolver() {
    }

    @Nullable
    public static UUID resolvePlayerUuid(@Nullable Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (ref == null || !ref.isValid()) return null;

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) return playerRef.getUuid();

        var standardPhysics = store.getComponent(ref, StandardPhysicsProvider.getComponentType());
        if (standardPhysics != null) {
            var playerUuid = resolvePlayerUuid(standardPhysics.getCreatorUuid(), store);
            if (playerUuid != null) return playerUuid;
        }

        var projectile = store.getComponent(ref, ProjectileComponent.getComponentType());
        if (projectile != null) {
            return resolvePlayerUuid(projectile.getCreatorUuid(), store);
        }

        return null;
    }

    @Nullable
    public static UUID resolvePlayerUuid(@Nonnull InteractionContext context) {
        var entityRef = context.getEntity();
        var playerUuid = resolvePlayerUuid(entityRef, entityRef.getStore());
        if (playerUuid != null) return playerUuid;

        var owningEntity = context.getOwningEntity();
        if (owningEntity == null || owningEntity == entityRef || !owningEntity.isValid()) return null;

        return resolvePlayerUuid(owningEntity, owningEntity.getStore());
    }

    public static boolean isBlockBreakAllowed(@Nullable UUID playerUuid, @Nullable String worldName, int chunkX, int chunkZ, @Nonnull String blockId) {
        var blockName = blockId.toLowerCase(Locale.ROOT);
        for (String ignoredBlock : Main.CONFIG.get().getBlocksThatIgnoreInteractRestrictions()) {
            if (blockName.contains(ignoredBlock.toLowerCase(Locale.ROOT))) return true;
        }

        if (worldName == null) return false;

        return ClaimManager.getInstance()
            .isAllowedToInteract(playerUuid, worldName, chunkX, chunkZ, PartyInfo::isBlockBreakEnabled, PartyOverrides.PARTY_PROTECTION_BREAK_BLOCKS);
    }

    @Nullable
    private static UUID resolvePlayerUuid(@Nullable UUID playerUuid, @Nonnull Store<EntityStore> store) {
        if (playerUuid == null) return null;

        var playerRef = store.getExternalData().getRefFromUUID(playerUuid);
        if (playerRef == null || !playerRef.isValid()) return null;

        var resolvedPlayerRef = store.getComponent(playerRef, PlayerRef.getComponentType());
        return resolvedPlayerRef != null ? resolvedPlayerRef.getUuid() : null;
    }
}
