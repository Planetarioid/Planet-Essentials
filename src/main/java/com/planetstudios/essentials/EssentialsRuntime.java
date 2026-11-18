package com.planetstudios.essentials;

import com.planetstudios.essentials.config.ConfigStore;
import com.planetstudios.essentials.config.EssentialsConfig;
import com.planetstudios.essentials.diagnostics.PerformanceMonitor;

/** Process-wide Essentials state shared by common and client entrypoints. */
public final class EssentialsRuntime {
    private static EssentialsConfig config;
    private static PerformanceMonitor performanceMonitor;

    private EssentialsRuntime() {}

    public static synchronized void initialize() {
        if (config != null) return;
        config = ConfigStore.load();
        performanceMonitor = new PerformanceMonitor(config.diagnosticsHistorySize);
    }

    public static EssentialsConfig config() {
        ensureInitialized();
        return config;
    }

    public static PerformanceMonitor performance() {
        ensureInitialized();
        return performanceMonitor;
    }

    public static synchronized void save() {
        ensureInitialized();
        ConfigStore.save(config);
    }

    private static void ensureInitialized() {
        if (config == null) initialize();
    }
}
