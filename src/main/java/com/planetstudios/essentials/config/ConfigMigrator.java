package com.planetstudios.essentials.config;

import com.google.gson.JsonObject;

import java.io.IOException;

/** Explicit, forward-only configuration schema migrations. */
public final class ConfigMigrator {
    public static final int CURRENT_SCHEMA = 2;

    private ConfigMigrator() {}

    public static JsonObject migrate(JsonObject json) throws IOException {
        JsonObject migrated = json.deepCopy();
        int version;
        try {
            version = migrated.has("schemaVersion") ? migrated.get("schemaVersion").getAsInt() : 0;
        } catch (RuntimeException exception) {
            throw new IOException("Configuration schema version is invalid", exception);
        }
        if (version > CURRENT_SCHEMA) {
            throw new IOException("Configuration schema " + version + " is newer than supported schema " + CURRENT_SCHEMA);
        }
        if (version < 1) {
            migrated.addProperty("schemaVersion", 1);
            version = 1;
        }
        if (version < 2) {
            if (!migrated.has("activeProfile") || migrated.get("activeProfile").isJsonNull()) {
                migrated.addProperty("activeProfile", "default");
            }
            migrated.addProperty("schemaVersion", 2);
        }
        return migrated;
    }
}
