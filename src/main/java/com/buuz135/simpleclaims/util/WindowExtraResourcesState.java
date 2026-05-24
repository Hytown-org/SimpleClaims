package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.protocol.ExtraResources;
import com.hypixel.hytale.protocol.io.ChannelConnection;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WindowExtraResourcesState {
    private WindowExtraResourcesState() {}

    private static final Map<ChannelConnection, Map<Integer, ExtraResources>> EXTRA_BY_WINDOW_ID = new ConcurrentHashMap<>();
    private static final Map<ChannelConnection, ExtraResources> NEXT_OPEN_EXTRA = new ConcurrentHashMap<>();
    private static final Map<ChannelConnection, Set<Integer>> BENCH_WINDOW_IDS = new ConcurrentHashMap<>();

    public static Set<Integer> getOrCreateBenchSet(ChannelConnection connection) {
        return BENCH_WINDOW_IDS.computeIfAbsent(connection, ignored -> ConcurrentHashMap.newKeySet());
    }

    public static Set<Integer> getBenchSet(ChannelConnection connection) {
        return BENCH_WINDOW_IDS.get(connection);
    }

    public static Map<Integer, ExtraResources> getOrCreateMap(ChannelConnection connection) {
        return EXTRA_BY_WINDOW_ID.computeIfAbsent(connection, ignored -> new ConcurrentHashMap<>());
    }

    public static Map<Integer, ExtraResources> getMap(ChannelConnection connection) {
        return EXTRA_BY_WINDOW_ID.get(connection);
    }

    public static void setNextOpenExtra(ChannelConnection connection, ExtraResources extraResources) {
        if (extraResources == null) {
            NEXT_OPEN_EXTRA.remove(connection);
            return;
        }
        NEXT_OPEN_EXTRA.put(connection, extraResources);
    }

    public static ExtraResources takeNextOpenExtra(ChannelConnection connection) {
        return NEXT_OPEN_EXTRA.remove(connection);
    }

    public static void clear(ChannelConnection connection) {
        EXTRA_BY_WINDOW_ID.remove(connection);
        NEXT_OPEN_EXTRA.remove(connection);
        BENCH_WINDOW_IDS.remove(connection);
    }
}