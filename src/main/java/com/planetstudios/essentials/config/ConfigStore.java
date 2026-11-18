package com.planetstudios.essentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Loads and persists the main Essentials configuration safely. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("planet-essentials").resolve("config.json");

    private ConfigStore() {}

    public static EssentialsConfig load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) {
                EssentialsConfig defaults = new EssentialsConfig();
                save(defaults);
                return defaults;
            }
            try (var reader = Files.newBufferedReader(FILE)) {
                EssentialsConfig config = GSON.fromJson(reader, EssentialsConfig.class);
                if (config == null) throw new IOException("Configuration file was empty.");
                config.validate();
                return config;
            }
        } catch (Exception exception) {
            // Preserve the original file for manual recovery.
            throw new IllegalStateException("Could not load Planet Essentials configuration at " + FILE, exception);
        }
    }

    public static void save(EssentialsConfig config) {
        config.validate();
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "config-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save Planet Essentials configuration at " + FILE, exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }
    }
}
