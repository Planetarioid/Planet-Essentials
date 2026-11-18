package com.planetstudios.essentials.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Central registry of settings that have real, connected behavior. */
public final class SettingsRegistry {
    private static final Map<String, SettingDefinition<?>> SETTINGS = new LinkedHashMap<>();

    static {
        register(new SettingDefinition<>("diagnostics.enabled", "diagnostics",
                "Performance diagnostics", "Collect a bounded local FPS history.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.diagnosticsEnabled, (config, value) -> config.diagnosticsEnabled = value));
        register(new SettingDefinition<>("diagnostics.performance_screen", "diagnostics",
                "Show performance summary", "Show the FPS summary in the Essentials hub.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.showPerformanceScreen, (config, value) -> config.showPerformanceScreen = value));
        register(new SettingDefinition<>("diagnostics.history_size", "diagnostics",
                "History capacity", "Maximum number of FPS samples retained (20 to 1200).",
                SettingDefinition.Kind.INTEGER, 120, value -> value != null && value >= 20 && value <= 1200,
                config -> config.diagnosticsHistorySize, (config, value) -> config.diagnosticsHistorySize = value));
    }

    private SettingsRegistry() {}

    private static void register(SettingDefinition<?> setting) {
        if (SETTINGS.putIfAbsent(setting.id(), setting) != null) {
            throw new IllegalStateException("Duplicate setting id: " + setting.id());
        }
    }

    public static Collection<SettingDefinition<?>> all() {
        return Collections.unmodifiableCollection(SETTINGS.values());
    }

    public static List<SettingDefinition<?>> inCategory(String category) {
        return SETTINGS.values().stream().filter(setting -> setting.category().equals(category)).toList();
    }

    public static Optional<SettingDefinition<?>> find(String id) {
        return Optional.ofNullable(SETTINGS.get(id));
    }

    public static List<String> categories() {
        return SETTINGS.values().stream().map(SettingDefinition::category).distinct().toList();
    }

    public static void validate(EssentialsConfig config) {
        for (SettingDefinition<?> setting : SETTINGS.values()) {
            if (!setting.isValid(setting.get(config))) setting.set(config, setting.defaultValue());
        }
    }
}
