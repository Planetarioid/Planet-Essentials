package com.planetstudios.essentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Discovers Minecraft's real OptionInstance controls and exposes searchable Planet metadata. */
public final class VanillaSettingsRegistry {
    public record Entry(String id, String category, String name, String searchText, OptionInstance<?> option) {}

    private VanillaSettingsRegistry() {}

    public static List<Entry> all(Minecraft client) {
        List<Entry> entries = new ArrayList<>();
        for (Field field : client.options.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !OptionInstance.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(client.options);
                if (!(value instanceof OptionInstance<?> option)) continue;
                String name = caption(option);
                String id = slug(name);
                String category = category(name);
                String tags = tags(name, category);
                entries.add(new Entry("minecraft." + id, category, name,
                        (name + " " + id + " " + category + " " + tags).toLowerCase(Locale.ROOT), option));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // One inaccessible option must not prevent the rest of Minecraft's settings from appearing.
            }
        }
        entries.sort(Comparator.comparing(Entry::category).thenComparing(Entry::name));
        return List.copyOf(entries);
    }

    private static String caption(OptionInstance<?> option) {
        for (Field field : OptionInstance.class.getDeclaredFields()) {
            if (!Component.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(option);
                if (value instanceof Component component) return component.getString();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next component field.
            }
        }
        return "Minecraft option";
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private static String category(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        if (contains(value, "sound", "music", "volume", "device")) return "audio";
        if (contains(value, "key", "mouse", "sensitivity", "touchscreen", "toggle")) return "controls";
        if (contains(value, "chat", "narrator", "text", "screen reader", "accessibility", "flash", "transparency")) return "accessibility";
        if (contains(value, "language", "resource pack", "skin", "cape", "model part")) return "personalization";
        if (contains(value, "render", "simulation", "graphics", "cloud", "particle", "entity", "chunk", "fps", "frame", "mipmap", "biome", "weather", "shadow", "lighting", "shader")) return "video";
        return "gameplay";
    }

    private static String tags(String name, String category) {
        String value = name.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(category).append(" minecraft vanilla option setting");
        if (contains(value, "render", "simulation", "chunk")) result.append(" view distance chunks loading world performance fps");
        if (contains(value, "fov", "field of view")) result.append(" camera perspective zoom");
        if (contains(value, "sensitivity", "mouse")) result.append(" aim cursor pointer");
        if (contains(value, "graphics", "cloud", "particle", "entity", "shadow", "lighting")) result.append(" visual quality performance fps");
        if (contains(value, "sound", "music", "volume")) result.append(" sound effects master audio");
        if (contains(value, "chat", "narrator", "text")) result.append(" interface subtitles communication");
        if (contains(value, "fullscreen", "resolution", "gui scale")) result.append(" display screen window");
        return result.toString();
    }

    private static boolean contains(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }
}
