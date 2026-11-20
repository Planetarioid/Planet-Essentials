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
                "Diagnostics master switch", "Master switch for all local diagnostics collection.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.diagnosticsEnabled, (config, value) -> config.diagnosticsEnabled = value));
        register(new SettingDefinition<>("diagnostics.performance_screen", "diagnostics",
                "Show performance summary", "Show the FPS summary in the Essentials hub.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.showPerformanceScreen, (config, value) -> config.showPerformanceScreen = value));
        register(new SettingDefinition<>("diagnostics.history_size", "diagnostics",
                "History capacity", "Maximum number of samples retained (20 to 1200).",
                SettingDefinition.Kind.INTEGER, 120, value -> value != null && value >= 20 && value <= 1200,
                config -> config.diagnosticsHistorySize, (config, value) -> config.diagnosticsHistorySize = value));
        register(new SettingDefinition<>("systems.performance_monitoring", "systems",
                "FPS monitoring", "Independently enable or disable FPS and frame-time sampling.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.performanceMonitoringEnabled, (config, value) -> config.performanceMonitoringEnabled = value));
        register(new SettingDefinition<>("systems.event_collection", "systems",
                "Event collection", "Independently enable or disable local lifecycle and slow-tick event collection.",
                SettingDefinition.Kind.BOOLEAN, false, value -> value != null,
                config -> config.eventCollectionEnabled, (config, value) -> config.eventCollectionEnabled = value));
        register(new SettingDefinition<>("systems.smart_chunk_scheduler", "systems",
                "Smart chunk scheduler", "Prioritize nearby forward-facing chunk rebuilds while preserving vanilla recompile fairness.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.smartChunkSchedulerEnabled, (config, value) -> config.smartChunkSchedulerEnabled = value));
        register(new SettingDefinition<>("hud.fps", "hud",
                "FPS HUD", "Show the live FPS widget during gameplay.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.fpsHudEnabled, (config, value) -> config.fpsHudEnabled = value));
        register(new SettingDefinition<>("combat.crosshair_target_indicator", "combat",
                "Player hit indicator", "Turn the crosshair red when a player is directly targeted and inside melee attack range.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.crosshairTargetIndicator, (config, value) -> config.crosshairTargetIndicator = value));
        register(new SettingDefinition<>("combat.combo_counter", "combat",
                "Combo counter", "Show the signed combat streak HUD widget. Positive values count landed hits; negative values count received hits.",
                SettingDefinition.Kind.BOOLEAN, true, value -> value != null,
                config -> config.comboCounterEnabled, (config, value) -> config.comboCounterEnabled = value));
        register(new SettingDefinition<>("shaders.enabled", "shaders",
                "Enable Prism shaders", "Enable the selected Prism shader pack. Turning this off immediately bypasses Prism effects.",
                SettingDefinition.Kind.BOOLEAN, false, value -> value != null,
                config -> config.shadersEnabled, (config, value) -> config.shadersEnabled = value));
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
