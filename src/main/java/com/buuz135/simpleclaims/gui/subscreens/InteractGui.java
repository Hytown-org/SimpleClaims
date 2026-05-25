package com.buuz135.simpleclaims.gui.subscreens;

import com.buuz135.simpleclaims.Main;
import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.chunk.ChunkInfo;
import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyOverride;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.buuz135.simpleclaims.commands.CommandMessages;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class InteractGui extends GuiWithParent<InteractGui.ChunkListGuiData> {

    private final PartyInfo partyInfo;
    private final boolean isOpEdit;

    public InteractGui(@NonNullDecl PlayerRef playerRef, PartyInfo partyInfo, InteractiveCustomUIPage<?> parent, boolean isOpEdit) {
        super(playerRef, CustomPageLifetime.CanDismiss, ChunkListGuiData.CODEC, parent);
        this.partyInfo = partyInfo;
        this.isOpEdit = isOpEdit;
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl ChunkListGuiData data) {
        super.handleDataEvent(ref, store, data);
        var player = store.getComponent(ref, Player.getComponentType());
        if (data.action != null) {
            if (data.action.equals("InteractBlocksSetting")) {
                this.partyInfo.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_INTERACT, new PartyOverride.PartyOverrideValue("bool", !this.partyInfo.isBlockInteractEnabled())));
            }
            if (data.action.equals("InteractChestSetting")) {
                this.partyInfo.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_INTERACT_CHEST, new PartyOverride.PartyOverrideValue("bool", !this.partyInfo.isChestInteractEnabled())));
            }
            if (data.action.equals("InteractDoorSetting")) {
                this.partyInfo.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_INTERACT_DOOR, new PartyOverride.PartyOverrideValue("bool", !this.partyInfo.isDoorInteractEnabled())));
            }
            if (data.action.equals("InteractBenchSetting")) {
                this.partyInfo.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_INTERACT_BENCH, new PartyOverride.PartyOverrideValue("bool", !this.partyInfo.isBenchInteractEnabled())));
            }
            if (data.action.equals("InteractChairSetting")) {
                this.partyInfo.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_INTERACT_CHAIR, new PartyOverride.PartyOverrideValue("bool", !this.partyInfo.isChairInteractEnabled())));
            }
            if (data.action.equals("InteractPortalSetting")) {
                this.partyInfo.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_INTERACT_PORTAL, new PartyOverride.PartyOverrideValue("bool", !this.partyInfo.isPortalInteractEnabled())));
            }
            if (data.action.equals("TamedDamageSetting")) {
                this.partyInfo.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_TAMED_DAMAGE, new PartyOverride.PartyOverrideValue("bool", !this.partyInfo.isTamedDamageEnabled())));
            }
            ClaimManager.getInstance().saveParty(this.partyInfo);
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.build(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, true);
        }
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        super.build(ref, uiCommandBuilder, uiEventBuilder, store);
        var playerCanModify = this.partyInfo.isOwner(this.playerRef.getUuid()) || this.isOpEdit;
        uiCommandBuilder.append("Pages/Buuz135_SimpleClaims_PartyInteractConfig.ui");

        uiCommandBuilder.set("#InteractBlocksSetting #CheckBox.Value", this.partyInfo.isBlockInteractEnabled());
        uiCommandBuilder.set("#InteractBlocksSetting #CheckBox.Disabled", !playerCanModify || !Main.CONFIG.get().isAllowPartyInteractBlockSetting());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InteractBlocksSetting #CheckBox", EventData.of("Action", "InteractBlocksSetting"), false);

        uiCommandBuilder.set("#InteractChestSetting #CheckBox.Value", this.partyInfo.isChestInteractEnabled());
        uiCommandBuilder.set("#InteractChestSetting #CheckBox.Disabled", !playerCanModify || !Main.CONFIG.get().isAllowPartyInteractChestSetting());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InteractChestSetting #CheckBox", EventData.of("Action", "InteractChestSetting"), false);

        uiCommandBuilder.set("#InteractDoorSetting #CheckBox.Value", this.partyInfo.isDoorInteractEnabled());
        uiCommandBuilder.set("#InteractDoorSetting #CheckBox.Disabled", !playerCanModify || !Main.CONFIG.get().isAllowPartyInteractDoorSetting());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InteractDoorSetting #CheckBox", EventData.of("Action", "InteractDoorSetting"), false);

        uiCommandBuilder.set("#InteractBenchSetting #CheckBox.Value", this.partyInfo.isBenchInteractEnabled());
        uiCommandBuilder.set("#InteractBenchSetting #CheckBox.Disabled", !playerCanModify || !Main.CONFIG.get().isAllowPartyInteractBenchSetting());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InteractBenchSetting #CheckBox", EventData.of("Action", "InteractBenchSetting"), false);

        uiCommandBuilder.set("#InteractChairSetting #CheckBox.Value", this.partyInfo.isChairInteractEnabled());
        uiCommandBuilder.set("#InteractChairSetting #CheckBox.Disabled", !playerCanModify || !Main.CONFIG.get().isAllowPartyInteractChairSetting());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InteractChairSetting #CheckBox", EventData.of("Action", "InteractChairSetting"), false);

        uiCommandBuilder.set("#InteractPortalSetting #CheckBox.Value", this.partyInfo.isPortalInteractEnabled());
        uiCommandBuilder.set("#InteractPortalSetting #CheckBox.Disabled", !playerCanModify || !Main.CONFIG.get().isAllowPartyInteractPortalSetting());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InteractPortalSetting #CheckBox", EventData.of("Action", "InteractPortalSetting"), false);

        uiCommandBuilder.set("#TamedDamageSetting #CheckBox.Value", this.partyInfo.isTamedDamageEnabled());
        uiCommandBuilder.set("#TamedDamageSetting #CheckBox.Disabled", !playerCanModify || !Main.CONFIG.get().isAllowPartyTamedDamageSetting());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TamedDamageSetting #CheckBox", EventData.of("Action", "TamedDamageSetting"), false);
    }

    public static class ChunkListGuiData extends GuiWithParentData {
        static final String KEY_ACTION = "Action";


        public static final BuilderCodec<ChunkListGuiData> CODEC = BuilderCodec.<ChunkListGuiData>builder(ChunkListGuiData.class, ChunkListGuiData::new)
                .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (searchGuiData, s) -> searchGuiData.action = s, searchGuiData -> searchGuiData.action)
                .addField(new KeyedCodec<>(GuiWithParentData.BACK_INTERACTION, Codec.STRING), (searchGuiData, s) -> searchGuiData.backInteraction = s, searchGuiData -> searchGuiData.backInteraction)

                .build();

        private String action;
    }
}
