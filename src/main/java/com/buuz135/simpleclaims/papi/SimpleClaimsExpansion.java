package com.buuz135.simpleclaims.papi;

import at.helpch.placeholderapi.PlaceholderAPI;
import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimpleClaimsExpansion extends PlaceholderExpansion {
    private static final ClaimManager CLAIMS = ClaimManager.getInstance();
    private static final Pattern ARGUMENT_DELIMITER = Pattern.compile("_");

    private static final Map<String, InteractionType> INTERACTIONS = Map.ofEntries(
            Map.entry("place_blocks", new InteractionType(PartyInfo::isBlockPlaceEnabled, PartyOverrides.PARTY_PROTECTION_PLACE_BLOCKS)),
            Map.entry("interact_blocks", new InteractionType(PartyInfo::isBlockInteractEnabled, PartyOverrides.PARTY_PROTECTION_INTERACT)),
            Map.entry("break_blocks", new InteractionType(PartyInfo::isBlockBreakEnabled, PartyOverrides.PARTY_PROTECTION_BREAK_BLOCKS)),
            Map.entry("interact_chest", new InteractionType(PartyInfo::isChestInteractEnabled, PartyOverrides.PARTY_PROTECTION_INTERACT_CHEST)),
            Map.entry("interact_bench", new InteractionType(PartyInfo::isBenchInteractEnabled, PartyOverrides.PARTY_PROTECTION_INTERACT_BENCH)),
            Map.entry("interact_chair", new InteractionType(PartyInfo::isChairInteractEnabled, PartyOverrides.PARTY_PROTECTION_INTERACT_CHAIR)),
            Map.entry("interact_door", new InteractionType(PartyInfo::isDoorInteractEnabled, PartyOverrides.PARTY_PROTECTION_INTERACT_DOOR)),
            Map.entry("interact_portal", new InteractionType(PartyInfo::isPortalInteractEnabled, PartyOverrides.PARTY_PROTECTION_INTERACT_PORTAL)),
            Map.entry("tamed_damage", new InteractionType(PartyInfo::isTamedDamageEnabled, PartyOverrides.PARTY_PROTECTION_TAMED_DAMAGE)),
            Map.entry("enter", new InteractionType(PartyInfo::isAllowEntryEnabled, PartyOverrides.PARTY_PROTECTION_ALLOW_ENTRY)),
            Map.entry("friendly_fire", new InteractionType(PartyInfo::isFriendlyFireEnabled, PartyOverrides.PARTY_PROTECTION_FRIENDLY_FIRE)),
            Map.entry("pvp", new InteractionType(PartyInfo::isPVPEnabled, PartyOverrides.PARTY_PROTECTION_PVP))
    );

    @Override
    public @NotNull String getIdentifier() {
        return "simpleclaims";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Buuz135";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.27";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(final PlayerRef player, @NotNull final String params) {
        switch (params) {
            case "parties_total":
                return String.valueOf(CLAIMS.getParties().size());
        }

        if (params.startsWith("can_")) {
            final String args = params.replace("can_", "");

            final InteractionType interaction = INTERACTIONS.get(args);

            if (interaction == null) {
                return null;
            }

            final Vector3d position = player.getTransform().getPosition();
            final World world = Optional.ofNullable(player.getWorldUuid())
                    .map(Universe.get()::getWorld)
                    .orElse(null);

            if (world == null) {
                return "can't find world";
            }

            return PlaceholderAPI.booleanValue(CLAIMS.isAllowedToInteract(
                    player.getUuid(),
                    world.getName(),
                    Double.valueOf(position.x()).intValue(),
                    Double.valueOf(position.z()).intValue(),
                    interaction.interactMethod(),
                    interaction.permission()));
        }

        final PartyInfo party = CLAIMS.getPartyFromPlayer(player.getUuid());

        if (party != null) {
            switch (params) {
                case "party_name":
                    return party.getName();
                case "party_description":
                    return party.getDescription();
                case "party_id":
                    return String.valueOf(party.getId());
                case "party_size":
                    return String.valueOf(party.getMembers().length);
                case "party_color":
                    return String.valueOf(party.getColor());
                case "party_created":
                    return party.getCreatedTracked().getDate();
                case "party_maxclaims":
                    return String.valueOf(party.getMaxClaimAmount());
                case "party_modified":
                    return party.getModifiedTracked().getDate();
                case "party_owner_uuid":
                    return String.valueOf(party.getOwner());
                case "party_owner_name":
                    return CLAIMS.getPlayerNameTracker().getPlayerName(party.getOwner());
                case "party_allies_total":
                    return String.valueOf(party.getPartyAllies().size());
                case "party_allies_uuids":
                    return party.getPartyAllies().stream().map(UUID::toString).collect(Collectors.joining(","));
                case "party_allies_names":
                    return party.getPartyAllies().stream().map(CLAIMS::getPartyById).filter(Objects::nonNull).map(PartyInfo::getName).collect(Collectors.joining(","));
                case "party_claims":
                    return String.valueOf(CLAIMS.getAmountOfClaims(party));
            }

            if (params.startsWith("party_can_")) {
                final String args = params.replace("party_can_", "");

                final InteractionType interaction = INTERACTIONS.get(args);

                if (interaction == null) {
                    return null;
                }

                return PlaceholderAPI.booleanValue(interaction.interactMethod().test(party));
            }

            if (params.startsWith("party_partyallied_")) {
                final String[] args = ARGUMENT_DELIMITER.split(params);

                if (args.length != 3) {
                    return null;
                }

                final Boolean result = Optional.ofNullable(CLAIMS.getParties().get(args[2]))
                        .map(PartyInfo::getId)
                        .map(party::isPartyAllied)
                        .orElse(null);

                return result == null ? null : PlaceholderAPI.booleanValue(result);
            }

            if (params.startsWith("party_playerallied_")) {
                final String[] args = ARGUMENT_DELIMITER.split(params);

                if (args.length != 3) {
                    return null;
                }

                final Boolean result = Optional.ofNullable(CLAIMS.getPlayerNameTracker().getPlayerUUID(args[2]))
                        .map(party::isPlayerAllied)
                        .orElse(null);

                return result == null ? null : PlaceholderAPI.booleanValue(result);
            }
        }

        return null;
    }

    private record InteractionType(Predicate<PartyInfo> interactMethod, String permission) {}
}
