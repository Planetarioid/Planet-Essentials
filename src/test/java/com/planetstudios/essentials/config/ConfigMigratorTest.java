package com.planetstudios.essentials.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorTest {
    @Test
    void migratesLegacyConfigurationToCurrentSchema() throws IOException {
        JsonObject legacy = new JsonObject();
        legacy.addProperty("schemaVersion", 5);
        JsonObject migrated = ConfigMigrator.migrate(legacy);
        assertEquals(ConfigMigrator.CURRENT_SCHEMA, migrated.get("schemaVersion").getAsInt());
        assertTrue(migrated.get("comboCounterEnabled").getAsBoolean());
    }

    @Test
    void rejectsFutureSchema() {
        JsonObject future = new JsonObject();
        future.addProperty("schemaVersion", ConfigMigrator.CURRENT_SCHEMA + 1);
        assertThrows(IOException.class, () -> ConfigMigrator.migrate(future));
    }
}
