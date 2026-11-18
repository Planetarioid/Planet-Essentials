package com.planetstudios.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EssentialsConfigTest {
    @Test
    void clampsDiagnosticsHistoryToSupportedRange() {
        EssentialsConfig config = new EssentialsConfig();
        config.diagnosticsHistorySize = 1;
        config.validate();
        assertEquals(20, config.diagnosticsHistorySize);

        config.diagnosticsHistorySize = 5000;
        config.validate();
        assertEquals(1200, config.diagnosticsHistorySize);
    }

    @Test
    void rejectsInvalidProfileNames() {
        EssentialsConfig config = new EssentialsConfig();
        config.activeProfile = "../unsafe";
        config.validate();
        assertEquals("default", config.activeProfile);
    }

    @Test
    void restoresSchemaVersion() {
        EssentialsConfig config = new EssentialsConfig();
        config.schemaVersion = 999;
        config.validate();
        assertEquals(1, config.schemaVersion);
    }
}
