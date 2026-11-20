package com.planetstudios.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsRegistryTest {
    @Test
    void exposesOnlyImplementedSettingsWithStableIds() {
        assertTrue(SettingsRegistry.find("diagnostics.enabled").isPresent());
        assertTrue(SettingsRegistry.find("systems.performance_monitoring").isPresent());
        assertTrue(SettingsRegistry.find("systems.event_collection").isPresent());
        assertTrue(SettingsRegistry.find("shaders.enabled").isPresent());
        assertTrue(SettingsRegistry.find("combat.crosshair_target_indicator").isPresent());
        assertTrue(SettingsRegistry.find("hud.fps").isPresent());
        assertTrue(SettingsRegistry.find("combat.combo_counter").isPresent());
        assertEquals(9, SettingsRegistry.all().size());
        assertEquals(3, SettingsRegistry.inCategory("diagnostics").size());
        assertEquals(2, SettingsRegistry.inCategory("systems").size());
        assertEquals(1, SettingsRegistry.inCategory("shaders").size());
        assertEquals(2, SettingsRegistry.inCategory("combat").size());
        assertEquals(1, SettingsRegistry.inCategory("hud").size());
    }

    @Test
    void settingWritesAreTypeAndRangeChecked() {
        EssentialsConfig config = new EssentialsConfig();
        SettingDefinition<?> history = SettingsRegistry.find("diagnostics.history_size").orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> history.set(config, "fast"));
        assertThrows(IllegalArgumentException.class, () -> history.set(config, 5));
        history.set(config, 240);
        assertEquals(240, config.diagnosticsHistorySize);
    }

    @Test
    void subsystemSwitchesAreIndependent() {
        EssentialsConfig config = new EssentialsConfig();
        SettingsRegistry.find("systems.performance_monitoring").orElseThrow().set(config, false);
        assertFalse(config.performanceMonitoringEnabled);
        assertTrue(config.eventCollectionEnabled);
        SettingsRegistry.find("systems.event_collection").orElseThrow().set(config, false);
        assertFalse(config.performanceMonitoringEnabled);
        assertFalse(config.eventCollectionEnabled);
    }

    @Test
    void playerHitIndicatorIsEnabledByDefaultAndToggleable() {
        EssentialsConfig config = new EssentialsConfig();
        assertTrue(config.crosshairTargetIndicator);
        SettingsRegistry.find("combat.crosshair_target_indicator").orElseThrow().set(config, false);
        assertFalse(config.crosshairTargetIndicator);
        SettingsRegistry.find("combat.crosshair_target_indicator").orElseThrow().set(config, true);
        assertTrue(config.crosshairTargetIndicator);
    }

    @Test
    void shaderSwitchIsOptInAndIndependentFromDiagnostics() {
        EssentialsConfig config = new EssentialsConfig();
        assertFalse(config.shadersEnabled);
        SettingsRegistry.find("shaders.enabled").orElseThrow().set(config, true);
        assertTrue(config.shadersEnabled);
        assertTrue(config.diagnosticsEnabled);
    }

    @Test
    void hudSettingsAreIndependentlyToggleable() {
        EssentialsConfig config = new EssentialsConfig();
        SettingsRegistry.find("hud.fps").orElseThrow().set(config, false);
        assertFalse(config.fpsHudEnabled);
        assertTrue(config.comboCounterEnabled);
        SettingsRegistry.find("combat.combo_counter").orElseThrow().set(config, false);
        assertFalse(config.comboCounterEnabled);
        assertFalse(config.fpsHudEnabled);
    }
}
