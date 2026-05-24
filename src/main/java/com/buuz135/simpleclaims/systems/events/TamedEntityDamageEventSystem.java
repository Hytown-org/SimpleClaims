package com.buuz135.simpleclaims.systems.events;

import com.buuz135.simpleclaims.Main;
import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

public class TamedEntityDamageEventSystem extends DamageEventSystem {

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl Damage damage) {

        if (damage.isCancelled()) return;

        // Only care about entity-sourced damage (a player attacking something)
        if (!(damage.getSource() instanceof Damage.EntitySource damageEntitySource)) return;

        Ref<EntityStore> attackerRef = damageEntitySource.getRef();
        if (!attackerRef.isValid()) return;

        // The attacker must be a player
        PlayerRef attackerPlayerRef = (PlayerRef) commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
        if (attackerPlayerRef == null) return;

        // The victim must be an NPC (tamed entity check)
        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
        NPCEntity npcEntity = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (npcEntity == null) return;

        // Check if this NPC is a tamed entity by examining its role name
        String roleName = npcEntity.getRoleName();
        if (roleName == null || !isTamedRole(roleName)) return;

        // Get the victim's position to determine which claim chunk they're in
        TransformComponent transform = store.getComponent(victimRef, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d position = transform.getPosition();
        var world = store.getExternalData().getWorld();
        if (world == null) return;

        String worldName = world.getName();
        if (!ClaimManager.getInstance().isAllowedToInteract(attackerPlayerRef.getUuid(), worldName, (int) position.x(), (int) position.z(), PartyInfo::isTamedDamageEnabled, PartyOverrides.PARTY_PROTECTION_TAMED_DAMAGE)) {
            damage.setAmount(0);
        }
    }

    /**
     * Determines if an NPC role name indicates a tamed/domesticated entity.
     * Checks if the role name contains any of the configured identifiers.
     */
    private static boolean isTamedRole(String roleName) {
        for (String identifier : Main.CONFIG.get().getTamedEntityRoleIdentifiers()) {
            if (roleName.contains(identifier)) return true;
        }
        return false;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        // Match all entities with NPCEntity component (the victim is an NPC)
        return NPCEntity.getComponentType();
    }

    @NonNullDecl
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
