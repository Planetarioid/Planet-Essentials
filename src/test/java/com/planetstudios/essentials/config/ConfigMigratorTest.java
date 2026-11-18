package com.planetstudios.essentials.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorTest {
    @Test
    void migratesUnversionedConfigToCurrentSchema() throws Exception {
        JsonObject old = new JsonObject();
        old.addProperty("diagnosticsEnabled", false);
        JsonObject migrated = ConfigMigrator.migrate(old);
        assertEquals(ConfigMigrator.CURRENT_SCHEMA, migrated.get("schemaVersion").getAsInt());
        assertEquals("default", migrated.get("activeProfile").getAsString());
        assertFalse(migrated.get("diagnosticsEnabled").getAsBoolean());
    }

    @Test
    void rejectsFutureSchemaInsteadOfOverwritingIt() {
        JsonObject future = new JsonObject();
        future.addProperty("schemaVersion", 999);
        assertThrows(IOException.class, () -> ConfigMigrator.migrate(future));
    }
}
