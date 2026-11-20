package com.planetstudios.essentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import java.util.List;
import java.util.Locale;

/** Applies named presets through Minecraft's existing OptionInstance settings. */
public final class PerformancePresetService {
    public enum Preset {
        COMPETITIVE("competitive"),
        BALANCED("balanced"),
        VISUAL("visual");

        private final String id;
        Preset(String id) { this.id = id; }
        public String id() { return id; }
        public static Preset fromId(String id) {
            for (Preset preset : values()) if (preset.id.equalsIgnoreCase(id)) return preset;
            return BALANCED;
        }
    }

    private PerformancePresetService() {}

    public static void apply(Minecraft client, Preset preset) {
        List<VanillaSettingsRegistry.Entry> entries = VanillaSettingsRegistry.all(client);
        int render = switch (preset) { case COMPETITIVE -> 8; case BALANCED -> 12; case VISUAL -> 20; };
        int simulation = switch (preset) { case COMPETITIVE -> 5; case BALANCED -> 8; case VISUAL -> 12; };
        double entityDistance = preset == Preset.COMPETITIVE ? 0.75 : 1.0;
        int mipmaps = switch (preset) { case COMPETITIVE -> 0; case BALANCED -> 2; case VISUAL -> 4; };

        for (VanillaSettingsRegistry.Entry entry : entries) {
            String name = entry.name().toLowerCase(Locale.ROOT);
            OptionInstance<?> option = entry.option();
            Object current = option.get();
            if (name.contains("render distance") && current instanceof Integer) set(option, render);
            else if (name.contains("simulation distance") && current instanceof Integer) set(option, simulation);
            else if (name.contains("entity distance") && current instanceof Number) {
                set(option, current instanceof Float ? (float) entityDistance : entityDistance);
            } else if (name.contains("mipmap") && current instanceof Integer) set(option, mipmaps);
            else if (name.contains("biome blend") && current instanceof Integer) {
                set(option, preset == Preset.COMPETITIVE ? 0 : (preset == Preset.BALANCED ? 2 : 5));
            } else if (name.contains("entity shadows") && current instanceof Boolean) {
                set(option, preset != Preset.COMPETITIVE);
            } else if (name.contains("particles") && current instanceof Enum<?> value) {
                setEnum(option, value, preset == Preset.COMPETITIVE ? "MINIMAL" : "ALL");
            } else if (name.contains("clouds") && current instanceof Enum<?> value) {
                setEnum(option, value, preset == Preset.COMPETITIVE ? "OFF" : "FANCY");
            } else if (name.equals("graphics") && current instanceof Enum<?> value) {
                setEnum(option, value, preset == Preset.COMPETITIVE ? "FAST"
                        : (preset == Preset.BALANCED ? "FANCY" : "FABULOUS"));
            }
        }
        client.options.save();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void set(OptionInstance option, Object value) {
        try { option.set(value); } catch (RuntimeException ignored) { /* Skip unsupported version-specific values. */ }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setEnum(OptionInstance option, Enum<?> current, String desired) {
        Object[] constants = current.getDeclaringClass().getEnumConstants();
        for (Object constant : constants) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(desired)) {
                try { option.set(constant); } catch (RuntimeException ignored) {}
                return;
            }
        }
    }
}
