package com.buuz135.simpleclaims.ui;

import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Root UI host contract for SimpleClaims.
 * Rendering lives outside the plugin, while SimpleClaims keeps the claim logic.
 */
public interface SimpleClaimsUiHost {

    void openChunkMap(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String dimension,
            int chunkX,
            int chunkZ,
            boolean isOp
    );

    void openPartyEditor(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PartyInfo party,
            boolean isOpEdit
    );

    default void openPartyList(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        throw new UnsupportedOperationException("Party list UI is not hosted by the current claims UI host.");
    }
}
