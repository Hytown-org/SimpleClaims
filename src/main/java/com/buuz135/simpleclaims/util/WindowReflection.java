package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.window.BenchWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.MaterialExtraResourcesSection;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WindowReflection {
    private WindowReflection() {}

    private static volatile boolean swapFailureLogged;

    public static final Field BENCHSTATE_FIELD = field(BenchWindow.class, "benchBlock");
    public static final Field EXTRARES_FIELD = field(BenchWindow.class, "extraResourcesSection");
    public static final Method INVALIDATE_METHOD = method(Window.class, "invalidate");

    private static Field field(Class<?> c, String name) {
        try {
            Field f = c.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            throw new RuntimeException("Missing field " + c.getName() + "." + name, e);
        }
    }

    private static Method method(Class<?> c, String name) {
        try {
            Method m = c.getDeclaredMethod(name);
            m.setAccessible(true);
            return m;
        } catch (Exception e) {
            throw new RuntimeException("Missing method " + c.getName() + "." + name, e);
        }
    }

    public static BenchBlock getBenchBlock(BenchWindow bw) {
        try {
            return (BenchBlock) BENCHSTATE_FIELD.get(bw);
        }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public static MaterialExtraResourcesSection getExtraSection(BenchWindow bw) {
        try { return (MaterialExtraResourcesSection) EXTRARES_FIELD.get(bw); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Replaces the window's extra-resources section (a {@code private final} instance field, which
     * reflection may write once {@code setAccessible(true)} has succeeded). Returns false when the
     * JVM refuses the write; callers then stay on the tick-based re-filter fallback.
     */
    public static boolean swapExtraSection(BenchWindow bw, MaterialExtraResourcesSection section) {
        try {
            EXTRARES_FIELD.set(bw, section);
            return true;
        } catch (Throwable t) {
            if (!swapFailureLogged) {
                swapFailureLogged = true;
                Logger.getLogger("SimpleClaims").log(Level.WARNING,
                        "Could not replace BenchWindow.extraResourcesSection; falling back to tick-based crafting claim filter", t);
            }
            return false;
        }
    }

    public static void invalidate(Window w) {
        try { INVALIDATE_METHOD.invoke(w); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
