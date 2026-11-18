package com.planetstudios.essentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) throw new IOException("Configuration file was empty.");
                EssentialsConfig config = GSON.fromJson(ConfigMigrator.migrate(json), EssentialsConfig.class);
                if (config == null) throw new IOException("Configuration did not contain an object.");
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
        writeAtomically(FILE, GSON.toJson(config));
    }

    private static void writeAtomically(Path destination, String contents) {
        Path temporary = null;
        try {
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), "config-", ".tmp");
            Files.writeString(temporary, contents);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save Planet Essentials configuration at " + destination, exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }
    }
}
