package com.buuz135.simpleclaims.claim.component;

import com.buuz135.simpleclaims.Main;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class PlaytimeClaimRewardComponent implements Component<EntityStore> {

    @Nonnull
    public static final BuilderCodec<PlaytimeClaimRewardComponent> CODEC = BuilderCodec.builder(PlaytimeClaimRewardComponent.class, PlaytimeClaimRewardComponent::new)
            .append(new KeyedCodec<>("Servers", new MapCodec<>(Codec.INTEGER, HashMap::new, false)), (component, value) -> component.serverThresholds = value, PlaytimeClaimRewardComponent::getServerThresholds)
            .add()
            .append(new KeyedCodec<>("LastRewardedPlaytimeThreshold", Codec.INTEGER), (component, value) -> component.legacyLastRewardedPlaytimeThreshold = value, PlaytimeClaimRewardComponent::getLegacyLastRewardedPlaytimeThreshold)
            .addValidator(Validators.greaterThanOrEqual(0))
            .add()
            .build();

    @Nonnull
    private Map<String, Integer> serverThresholds;
    private int legacyLastRewardedPlaytimeThreshold;

    public PlaytimeClaimRewardComponent() {
        this(new HashMap<>(), 0);
    }

    public PlaytimeClaimRewardComponent(int legacyLastRewardedPlaytimeThreshold) {
        this(new HashMap<>(), legacyLastRewardedPlaytimeThreshold);
    }

    public PlaytimeClaimRewardComponent(@Nonnull Map<String, Integer> serverThresholds, int legacyLastRewardedPlaytimeThreshold) {
        this.serverThresholds = serverThresholds;
        this.legacyLastRewardedPlaytimeThreshold = legacyLastRewardedPlaytimeThreshold;
    }

    @Nonnull
    public Map<String, Integer> getServerThresholds() {
        return serverThresholds;
    }

    public int getRewardedPlaytimeThreshold(@Nonnull String serverId) {
        return serverThresholds.getOrDefault(serverId, 0);
    }

    public void setRewardedPlaytimeThreshold(@Nonnull String serverId, int threshold) {
        if (threshold <= 0) {
            serverThresholds.remove(serverId);
            return;
        }
        serverThresholds.put(serverId, threshold);
    }

    public void removeServer(@Nonnull String serverId) {
        serverThresholds.remove(serverId);
    }

    public boolean hasServer(@Nonnull String serverId) {
        return serverThresholds.containsKey(serverId);
    }

    public boolean hasAnyServer() {
        return !serverThresholds.isEmpty();
    }

    public boolean hasLegacyReward() {
        return legacyLastRewardedPlaytimeThreshold > 0;
    }

    public int getLegacyLastRewardedPlaytimeThreshold() {
        return legacyLastRewardedPlaytimeThreshold;
    }

    public void clearLegacyReward() {
        legacyLastRewardedPlaytimeThreshold = 0;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlaytimeClaimRewardComponent(new HashMap<>(serverThresholds), legacyLastRewardedPlaytimeThreshold);
    }

    @Nonnull
    public static ComponentType<EntityStore, PlaytimeClaimRewardComponent> getComponentType() {
        return Main.getPlaytimeClaimRewardComponentType();
    }
}
