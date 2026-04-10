package com.buuz135.simpleclaims.util;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.window.BenchWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.MaterialExtraResourcesSection;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class WindowReflection {
    private WindowReflection() {}

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

    public static void invalidate(Window w) {
        try { INVALIDATE_METHOD.invoke(w); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
