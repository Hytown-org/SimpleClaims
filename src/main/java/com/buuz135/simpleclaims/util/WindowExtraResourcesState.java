package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.protocol.ExtraResources;
import com.hypixel.hytale.protocol.io.ChannelConnection;
import com.hypixel.hytale.server.core.io.netty.NettyUtil;
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

    public static Channel getNettyChannel(ChannelConnection connection) {
        if (connection instanceof NettyUtil.NettyChannelConnection nettyChannelConnection) {
            return nettyChannelConnection.channel();
        }
        return null;
    }

    public static Set<Integer> getOrCreateBenchSet(Channel ch) {
        Set<Integer> benchIds = ch.attr(BENCH_WINDOW_IDS).get();
        if (benchIds == null) {
            benchIds = ConcurrentHashMap.newKeySet();
            ch.attr(BENCH_WINDOW_IDS).set(benchIds);
        }
        return benchIds;
    }

    public static Map<Integer, ExtraResources> getOrCreateMap(Channel ch) {
        Map<Integer, ExtraResources> map = ch.attr(EXTRA_BY_WINDOW_ID).get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            ch.attr(EXTRA_BY_WINDOW_ID).set(map);
        }
        return map;
    }

    public static Set<Integer> getOrCreateBenchSet(ChannelConnection connection) {
        Channel ch = getNettyChannel(connection);
        return ch == null ? null : getOrCreateBenchSet(ch);
    }

    public static Set<Integer> getBenchSet(ChannelConnection connection) {
        Channel ch = getNettyChannel(connection);
        return ch == null ? null : ch.attr(BENCH_WINDOW_IDS).get();
    }

    public static Map<Integer, ExtraResources> getOrCreateMap(ChannelConnection connection) {
        Channel ch = getNettyChannel(connection);
        return ch == null ? null : getOrCreateMap(ch);
    }

    public static Map<Integer, ExtraResources> getMap(ChannelConnection connection) {
        Channel ch = getNettyChannel(connection);
        return ch == null ? null : ch.attr(EXTRA_BY_WINDOW_ID).get();
    }

    public static ExtraResources takeNextOpenExtra(ChannelConnection connection) {
        Channel ch = getNettyChannel(connection);
        if (ch == null) return null;

        ExtraResources extra = ch.attr(NEXT_OPEN_EXTRA).get();
        ch.attr(NEXT_OPEN_EXTRA).set(null);
        return extra;
    }

    public static void clear(ChannelConnection connection) {
        Channel ch = getNettyChannel(connection);
        if (ch == null) return;

        ch.attr(EXTRA_BY_WINDOW_ID).set(null);
        ch.attr(NEXT_OPEN_EXTRA).set(null);
        ch.attr(BENCH_WINDOW_IDS).set(null);
    }
}
