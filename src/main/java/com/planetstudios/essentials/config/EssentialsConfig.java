package com.planetstudios.essentials.config;

/** User-owned Planet Essentials settings. Only implemented behavior is exposed. */
public final class EssentialsConfig {
    public int schemaVersion = ConfigMigrator.CURRENT_SCHEMA;
    public boolean diagnosticsEnabled = true;
    public boolean showPerformanceScreen = true;
    public int diagnosticsHistorySize = 120;
    public String activeProfile = "default";

    public void validate() {
        schemaVersion = ConfigMigrator.CURRENT_SCHEMA;
        if (activeProfile == null || !activeProfile.matches("[a-z0-9_-]{1,32}")) {
            activeProfile = "default";
        }
        SettingsRegistry.validate(this);
    }
}
