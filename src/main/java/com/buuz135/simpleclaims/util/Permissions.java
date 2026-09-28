package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import org.hytown.nexus.perks.IntPerkKey;
import org.hytown.nexus.perks.PlayerPerks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Permissions {

    public static final String CLAIM_CHUNK_AMOUNT = "simpleclaims.party.claim_chunk_amount";
    public static final String MAX_ADD_CHUNK_AMOUNT = "simpleclaims.admin.max_add_chunk_amount";
    public static final String CLAIM_CHUNK_GAIN_MINUTES = "simpleclaims.player.claim_chunk_gain_minutes";

    /**
     * Highest {@code simpleclaims.party.claim_chunk_amount.<n>} node the player holds,
     * or -1 when they have none (callers fall back to the config default). The
     * HytownNexus rank bonus is deliberately NOT folded in here so that sentinel
     * survives; see {@link #getRankBonusClaimAmount(UUID)} and
     * {@code PartyInfo#getBaseClaimAmount()}.
     */
    public static int getPermissionClaimAmount(UUID uuid) { //TODO Check of admin parties
        return getIntPermission(uuid, CLAIM_CHUNK_AMOUNT);
    }

    /**
     * Extra claim chunks granted by the player's HytownNexus rank, added on top of
     * the permission/config base. Never negative; 0 when Nexus is unavailable.
     */
    public static int getRankBonusClaimAmount(UUID uuid) {
        try {
            return Math.max(0, RankPerks.extraClaimChunks(uuid));
        } catch (NoClassDefFoundError | ExceptionInInitializerError | IllegalStateException e) {
            return 0;
        }
    }

    /**
     * Isolates the Nexus perk classes so a missing plugin surfaces as a catchable
     * linkage error at the call site instead of breaking {@link Permissions} itself.
     */
    private static final class RankPerks {
        // TODO switch to PerkKeys.EXTRA_CLAIM_CHUNKS once HytownNexus is published
        // (the published 1.9.3 jar predates the key; lookups are by id string, so
        // behaviour is identical).
        private static final IntPerkKey EXTRA_CLAIM_CHUNKS = new IntPerkKey("extra_claim_chunks", 0);

        static int extraClaimChunks(UUID uuid) {
            return PlayerPerks.get(uuid, EXTRA_CLAIM_CHUNKS);
        }
    }

    public static int getPermissionMaxAddChunkAmount(UUID uuid) {
        return getIntPermission(uuid, MAX_ADD_CHUNK_AMOUNT);
    }

    public static int getPermissionClaimChunkGainMinutes(UUID uuid) {
        return getIntPermission(uuid, CLAIM_CHUNK_GAIN_MINUTES);
    }

    private static int getIntPermission(UUID uuid, String permissionNode) {
        int amount = -1;
        for (PermissionProvider provider : PermissionsModule.get().getProviders()) {
            if (provider.getName().equals("LuckPerms")) {
                for (String perm : LuckPermsHelper.getPerms(uuid)) {
                    if (perm.startsWith(permissionNode + ".")) {
                        try {
                            var parsed = Integer.parseInt(perm.replace(permissionNode + ".", ""));
                            if (parsed > amount) amount = parsed;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                return amount;
            }
            var userNodes = new HashSet<String>();
            userNodes.addAll(provider.getUserPermissions(uuid));
            for (String s : provider.getGroupsForUser(uuid)) {
                userNodes.addAll(provider.getGroupPermissions(s));
            }
            for (String node : userNodes) {
                if (node.startsWith(permissionNode + ".")) {
                    try {
                        var parsed = Integer.parseInt(node.replace(permissionNode + ".", ""));
                        if (parsed > amount) amount = parsed;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return amount;
    }
}
