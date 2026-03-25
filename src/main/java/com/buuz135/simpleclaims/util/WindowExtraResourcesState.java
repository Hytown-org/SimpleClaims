package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.protocol.ExtraResources;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WindowExtraResourcesState {
    private WindowExtraResourcesState() {}

    public static final AttributeKey<Map<Integer, ExtraResources>> EXTRA_BY_WINDOW_ID =
            AttributeKey.valueOf("simpleclaims_extra_by_window_id");

    public static final AttributeKey<ExtraResources> NEXT_OPEN_EXTRA =
            AttributeKey.valueOf("simpleclaims_next_open_extra");

    public static final AttributeKey<Set<Integer>> BENCH_WINDOW_IDS =
            AttributeKey.valueOf("simpleclaims_bench_window_ids");

    public static Set<Integer> getOrCreateBenchSet(Channel ch) {
        Set<Integer> s = ch.attr(BENCH_WINDOW_IDS).get();
        if (s == null) {
            s = ConcurrentHashMap.newKeySet();
            ch.attr(BENCH_WINDOW_IDS).set(s);
        }
        return s;
    }

    public static Map<Integer, ExtraResources> getOrCreateMap(Channel ch) {
        Map<Integer, ExtraResources> m = ch.attr(EXTRA_BY_WINDOW_ID).get();
        if (m == null) {
            m = new ConcurrentHashMap<>();
            ch.attr(EXTRA_BY_WINDOW_ID).set(m);
        }
        return m;
    }

    public static void clear(Channel ch) {
        ch.attr(NEXT_OPEN_EXTRA).set(null);

        Map<Integer, ExtraResources> map = ch.attr(EXTRA_BY_WINDOW_ID).get();
        int mapSize = map != null ? map.size() : 0;
        if (map != null) {
            map.clear();
        }

        Set<Integer> benchIds = ch.attr(BENCH_WINDOW_IDS).get();
        int benchSize = benchIds != null ? benchIds.size() : 0;
        if (benchIds != null) {
            benchIds.clear();
        }

        System.out.println("[TEMP-CLAIMS-DEBUG][SimpleClaims][WindowExtraResourcesState] clear channel="
                + ch.id()
                + " extraMapSize=" + mapSize
                + " benchIdCount=" + benchSize);
    }
}
