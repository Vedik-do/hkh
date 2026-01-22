package com.pavithra.cataclysmomcompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Best-effort reflection bridge into OverhauledMusic.
 *
 * Goal:
 * - While our boss music is active, keep OverhauledMusic's fading instances "alive" but silent,
 *   so it doesn't hard-stop / overlap / restart weirdly.
 * - When boss music ends, restore OverhauledMusic volume.
 */
final class OverhauledMusicBridge {
    private static boolean initTried = false;
    private static boolean available = false;

    private static Object director;
    private static Field instancesField;
    private static Field currentField;

    private static Method fadeToMethod;
    private static Method setActiveMethod;
    private static Field inactiveTicksField;

    private OverhauledMusicBridge() {}

    private static void init() {
        if (initTried) return;
        initTried = true;

        try {
            Class<?> clientEvents = Class.forName("com.overhauledmusic.client.ClientEvents");
            Field directorField = clientEvents.getDeclaredField("DIRECTOR");
            directorField.setAccessible(true);
            director = directorField.get(null);

            Class<?> directorClass = Class.forName("com.overhauledmusic.client.MusicDirector");
            instancesField = directorClass.getDeclaredField("instances");
            instancesField.setAccessible(true);

            currentField = directorClass.getDeclaredField("current");
            currentField.setAccessible(true);

            Class<?> fmi = Class.forName("com.overhauledmusic.client.FadingMusicInstance");
            fadeToMethod = fmi.getDeclaredMethod("fadeTo", float.class, int.class);
            fadeToMethod.setAccessible(true);

            setActiveMethod = fmi.getDeclaredMethod("setActive", boolean.class);
            setActiveMethod.setAccessible(true);

            inactiveTicksField = fmi.getDeclaredField("inactiveTicks");
            inactiveTicksField.setAccessible(true);

            available = true;
            CataclysmOMCompat.LOGGER.info("[cataclysmomcompat] OverhauledMusic bridge enabled.");
        } catch (Throwable t) {
            available = false;
            CataclysmOMCompat.LOGGER.warn("[cataclysmomcompat] OverhauledMusic bridge unavailable: {}", t.toString());
        }
    }

    static void muteTick() {
        init();
        if (!available) return;

        try {
            @SuppressWarnings("unchecked")
            Map<Object, Object> instances = (Map<Object, Object>) instancesField.get(director);
            if (instances == null || instances.isEmpty()) return;

            Collection<Object> values = instances.values();
            for (Object inst : values) {
                if (inst == null) continue;

                try {
                    fadeToMethod.invoke(inst, 0.0f, 8);
                } catch (Throwable ignored) {}

                try {
                    inactiveTicksField.setInt(inst, 0);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            available = false;
            CataclysmOMCompat.LOGGER.warn("[cataclysmomcompat] OverhauledMusic bridge failed; disabling: {}", t.toString());
        }
    }

    static void unmuteNow() {
        init();
        if (!available) return;

        try {
            Object current = currentField.get(director);
            if (current != null) {
                try {
                    setActiveMethod.invoke(current, true);
                } catch (Throwable ignored) {}

                try {
                    fadeToMethod.invoke(current, 1.0f, 10);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            available = false;
            CataclysmOMCompat.LOGGER.warn("[cataclysmomcompat] OverhauledMusic bridge failed; disabling: {}", t.toString());
        }
    }
}
