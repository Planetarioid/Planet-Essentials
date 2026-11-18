package com.planetstudios.essentials.config;

/**
 * User-owned Planet Essentials settings.
 *
 * <p>Only settings with implemented behavior belong here. New optimization
 * controls must not be exposed until their implementation is connected.</p>
 */
public final class EssentialsConfig {
    public int schemaVersion = 1;
    public boolean diagnosticsEnabled = true;
    public boolean showPerformanceScreen = true;
    public int diagnosticsHistorySize = 120;
    public String activeProfile = "default";

    public void validate() {
        schemaVersion = 1;
        diagnosticsHistorySize = Math.max(20, Math.min(1200, diagnosticsHistorySize));
        if (activeProfile == null || !activeProfile.matches("[a-z0-9_-]{1,32}")) {
            activeProfile = "default";
        }
    }
}
