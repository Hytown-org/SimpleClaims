package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.TimeUnit;

public final class PageLaunchHelper {
    private static final long REOPEN_DELAY_MS = 100L;

    private PageLaunchHelper() {}

    public static void openFreshPage(
            @NonNullDecl Player player,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl InteractiveCustomUIPage<?> page
    ) {
        if (player.getPageManager() == null) {
            System.out.println("[TEMP-CLAIMS-DEBUG][SimpleClaims][PageLaunchHelper] missing PageManager player="
                    + playerRef.getUsername()
                    + " page=" + page.getClass().getSimpleName());
            return;
        }

        if (playerRef.getPacketHandler() != null) {
            WindowExtraResourcesState.clear(playerRef.getPacketHandler().getChannel());
        }

        System.out.println("[TEMP-CLAIMS-DEBUG][SimpleClaims][PageLaunchHelper] openFreshPage player="
                + playerRef.getUsername()
                + " uuid=" + playerRef.getUuid()
                + " page=" + page.getClass().getSimpleName()
                + " refValid=" + ref.isValid());

        boolean hadCustomPage = player.getPageManager().getCustomPage() != null;
        // Stronger reset than the old helper: if another custom page is already
        // open, force it closed first and only open the next page on the next
        // queued world task. Re-opening /sc while the first claims UI is still
        // open reproduces the same broken behavior as the O-menu route.
        player.getPageManager().setPage(ref, store, Page.None, true);

        World world = store.getExternalData() != null ? store.getExternalData().getWorld() : null;
        if (world != null && hadCustomPage) {
            System.out.println("[TEMP-CLAIMS-DEBUG][SimpleClaims][PageLaunchHelper] delaying reopen player="
                    + playerRef.getUsername()
                    + " page=" + page.getClass().getSimpleName()
                    + " delayMs=" + REOPEN_DELAY_MS);
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
                System.out.println("[TEMP-CLAIMS-DEBUG][SimpleClaims][PageLaunchHelper] opening delayed page player="
                        + playerRef.getUsername()
                        + " page=" + page.getClass().getSimpleName());
                player.getPageManager().openCustomPage(ref, store, page);
            }), REOPEN_DELAY_MS, TimeUnit.MILLISECONDS);
            return;
        }

        if (world != null) {
            world.execute(() -> {
                System.out.println("[TEMP-CLAIMS-DEBUG][SimpleClaims][PageLaunchHelper] opening deferred page player="
                        + playerRef.getUsername()
                        + " page=" + page.getClass().getSimpleName());
                player.getPageManager().openCustomPage(ref, store, page);
            });
            return;
        }

        System.out.println("[TEMP-CLAIMS-DEBUG][SimpleClaims][PageLaunchHelper] opening immediate page player="
                + playerRef.getUsername()
                + " page=" + page.getClass().getSimpleName());
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
