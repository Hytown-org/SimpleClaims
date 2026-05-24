package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.protocol.ExtraResources;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.io.ChannelConnection;
import com.hypixel.hytale.protocol.packets.window.CloseWindow;
import com.hypixel.hytale.protocol.packets.window.OpenWindow;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.netty.NettyUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import io.netty.channel.Channel;

import java.lang.reflect.Field;
import java.util.Map;

public final class WindowPacketAdapters {

    private WindowPacketAdapters() {}

    private static PacketFilter installed;

    private static volatile Field UPDATE_ID_FIELD;
    private static volatile Field UPDATE_EXTRA_FIELD;

    public static void install() {
        if (installed != null) return;

        installed = PacketAdapters.registerOutbound((PlayerRef playerRef, Packet serverPacket) -> {
            ChannelConnection connection = playerRef.getPacketHandler().getChannel();
            Channel ch = WindowExtraResourcesState.getNettyChannel(connection);
            if (ch == null) return false;

            if (serverPacket instanceof OpenWindow ow) {
                ExtraResources primed = ch.attr(WindowExtraResourcesState.NEXT_OPEN_EXTRA).getAndSet(null);
                if (primed != null) {
                    ow.extraResources = primed;

                    var map = WindowExtraResourcesState.getOrCreateMap(connection);
                    if (map != null) map.put(ow.id, primed);

                    var benchSet = WindowExtraResourcesState.getOrCreateBenchSet(connection);
                    if (benchSet != null) benchSet.add(ow.id);
                }
                return false;
            }

            if (serverPacket != null && serverPacket.getClass().getName().equals("com.hypixel.hytale.protocol.packets.window.UpdateWindow")) {
                Map<Integer, ExtraResources> map = ch.attr(WindowExtraResourcesState.EXTRA_BY_WINDOW_ID).get();
                if (map == null || map.isEmpty()) return false;

                try {
                    Class<?> cls = serverPacket.getClass();
                    int id = (int) getUpdateIdField(cls).get(serverPacket);
                    if (id == 0) return false;

                    var benchIds = ch.attr(WindowExtraResourcesState.BENCH_WINDOW_IDS).get();
                    if (benchIds == null || !benchIds.contains(id)) return false;

                    ExtraResources forced = map.get(id);
                    if (forced != null) {
                        getUpdateExtraField(cls).set(serverPacket, forced);
                    }
                } catch (Throwable ignored) {}
                return false;
            }

            if (serverPacket instanceof CloseWindow cw) {
                var map = ch.attr(WindowExtraResourcesState.EXTRA_BY_WINDOW_ID).get();
                if (map != null) map.remove(cw.id);

                var benchIds = ch.attr(WindowExtraResourcesState.BENCH_WINDOW_IDS).get();
                if (benchIds != null) benchIds.remove(cw.id);
            }

            return false;
        });
    }

    public static void uninstall() {
        if (installed != null) {
            PacketAdapters.deregisterOutbound(installed);
            installed = null;
        }
    }

    private static Field getUpdateIdField(Class<?> cls) throws NoSuchFieldException {
        Field f = UPDATE_ID_FIELD;
        if (f != null) return f;
        f = cls.getDeclaredField("id");
        f.setAccessible(true);
        UPDATE_ID_FIELD = f;
        return f;
    }

    private static Field getUpdateExtraField(Class<?> cls) throws NoSuchFieldException {
        Field f = UPDATE_EXTRA_FIELD;
        if (f != null) return f;
        for (Field fld : cls.getDeclaredFields()) {
            if (fld.getType() == ExtraResources.class) {
                fld.setAccessible(true);
                UPDATE_EXTRA_FIELD = fld;
                return fld;
            }
        }
        throw new NoSuchFieldException("No ExtraResources field on UpdateWindow");
    }
}