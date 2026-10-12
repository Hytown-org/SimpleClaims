package com.buuz135.simpleclaims.worldevents;

import com.hypixel.hytale.builtin.adventure.wilderness.component.WildernessLocation;
import com.hypixel.hytale.builtin.adventure.worldevents.condition.location.EventLocation;
import com.hypixel.hytale.builtin.adventure.worldevents.util.Vector3iList;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Replaces the built-in WildernessLocation so world events (Goblin Breaches, goblin coffers, hunts) never pick a spot
 * in or near a claimed chunk. Event prefabs are rolled back to a snapshot when the event ends, so one landing in a
 * claim would revert whatever the owner built or broke there while it was open.
 */
public class ClaimWildernessLocation extends WildernessLocation {

    public ClaimWildernessLocation(@Nonnull WildernessLocation.Config config) {
        this.config = config;
    }

    @Nonnull
    @Override
    public CompletableFuture<Vector3iList> find(@Nonnull Store<EntityStore> store) {
        String worldName = store.getExternalData().getWorld().getName();
        return super.find(store).thenApply(candidates -> {
            WorldEventClaimFilter.filter(candidates, worldName);
            return candidates;
        });
    }

    public static class Config extends WildernessLocation.Config {

        @Nonnull
        public static final BuilderCodec<Config> CUSTOM_CODEC =
                BuilderCodec.builder(Config.class, Config::new, WildernessLocation.Config.CODEC)
                        .build();

        @Nonnull
        @Override
        public EventLocation create() {
            if (minDistanceSq < 0 || maxDistanceSq < 0) {
                throw new IllegalStateException("ClaimWildernessLocation.Config not fully decoded (#afterDecode not called)");
            }
            return new ClaimWildernessLocation(this);
        }
    }
}
