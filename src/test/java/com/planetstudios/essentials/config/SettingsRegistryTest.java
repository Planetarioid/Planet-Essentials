package com.planetstudios.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsRegistryTest {
    @Test
    void exposesOnlyImplementedSettingsWithStableIds() {
        assertTrue(SettingsRegistry.find("diagnostics.enabled").isPresent());
        assertEquals(3, SettingsRegistry.all().size());
        assertEquals(3, SettingsRegistry.inCategory("diagnostics").size());
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
}
