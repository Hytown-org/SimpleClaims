package com.buuz135.simpleclaims.claim.component;

import com.buuz135.simpleclaims.Main;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PlaytimeClaimRewardComponent implements Component<EntityStore> {

    @Nonnull
    public static final BuilderCodec<PlaytimeClaimRewardComponent> CODEC = BuilderCodec.builder(PlaytimeClaimRewardComponent.class, PlaytimeClaimRewardComponent::new)
            .append(new KeyedCodec<>("LastRewardedPlaytimeThreshold", Codec.INTEGER), (component, value) -> component.lastRewardedPlaytimeThreshold = value, PlaytimeClaimRewardComponent::getLastRewardedPlaytimeThreshold)
            .addValidator(Validators.greaterThanOrEqual(0))
            .add()
            .build();

    private int lastRewardedPlaytimeThreshold;

    public PlaytimeClaimRewardComponent() {
        this(0);
    }

    public PlaytimeClaimRewardComponent(int lastRewardedPlaytimeThreshold) {
        this.lastRewardedPlaytimeThreshold = lastRewardedPlaytimeThreshold;
    }

    public int getLastRewardedPlaytimeThreshold() {
        return lastRewardedPlaytimeThreshold;
    }

    public void setLastRewardedPlaytimeThreshold(int lastRewardedPlaytimeThreshold) {
        this.lastRewardedPlaytimeThreshold = lastRewardedPlaytimeThreshold;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlaytimeClaimRewardComponent(lastRewardedPlaytimeThreshold);
    }

    @Nonnull
    public static ComponentType<EntityStore, PlaytimeClaimRewardComponent> getComponentType() {
        return Main.getPlaytimeClaimRewardComponentType();
    }
}
