package com.planetstudios.essentials;

import com.planetstudios.essentials.config.ConfigStore;
import com.planetstudios.essentials.config.EssentialsConfig;
import com.planetstudios.essentials.config.ProfileStore;
import com.planetstudios.essentials.diagnostics.PerformanceMonitor;
import net.fabricmc.loader.api.FabricLoader;

/** Process-wide Essentials state shared by common and client entrypoints. */
public final class EssentialsRuntime {
    private static EssentialsConfig config;
    private static PerformanceMonitor performanceMonitor;
    private static ProfileStore profileStore;

    private EssentialsRuntime() {}

    public static synchronized void initialize() {
        if (config != null) return;
        config = ConfigStore.load();
        performanceMonitor = new PerformanceMonitor(config.diagnosticsHistorySize);
        profileStore = new ProfileStore(FabricLoader.getInstance().getConfigDir()
                .resolve("planet-essentials").resolve("profiles"));
        if (!profileStore.listProfiles().contains("default")) profileStore.save("default", config);
    }

    public static EssentialsConfig config() {
        ensureInitialized();
        return config;
    }

    public static PerformanceMonitor performance() {
        ensureInitialized();
        return performanceMonitor;
    }

    public static ProfileStore profiles() {
        ensureInitialized();
        return profileStore;
    }

    public static synchronized void replaceConfig(EssentialsConfig replacement) {
        ensureInitialized();
        replacement.validate();
        config = replacement;
        performanceMonitor.setCapacity(config.diagnosticsHistorySize);
    }

    public static synchronized void save() {
        ensureInitialized();
        ConfigStore.save(config);
    }

    private static void ensureInitialized() {
        if (config == null) initialize();
    }
}
