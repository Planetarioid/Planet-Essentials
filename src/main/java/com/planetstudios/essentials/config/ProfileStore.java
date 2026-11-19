package com.planetstudios.essentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Safe profile storage. Profiles are snapshots of Essentials-owned settings only. */
public final class ProfileStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private final Path directory;

    public ProfileStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public static String normalizeName(String name) {
        if (name == null) throw new IllegalArgumentException("Profile name is required");
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (!VALID_NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Profile names must use 1-32 lowercase letters, numbers, '_' or '-'");
        }
        return normalized;
    }

    public synchronized List<String> listProfiles() {
        try {
            Files.createDirectories(directory);
            try (Stream<Path> files = Files.list(directory)) {
                return files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString().replaceFirst("\\.json$", ""))
                        .filter(name -> VALID_NAME.matcher(name).matches())
                        .sorted().toList();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not list Planet Essentials profiles", exception);
        }
    }

    public synchronized void save(String name, EssentialsConfig config) {
        String safeName = normalizeName(name);
        config.validate();
        writeAtomically(profilePath(safeName), GSON.toJson(config));
    }

    public synchronized EssentialsConfig load(String name) {
        String safeName = normalizeName(name);
        Path path = profilePath(safeName);
        try (var reader = Files.newBufferedReader(path)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) throw new IOException("Profile is empty");
            EssentialsConfig config = GSON.fromJson(ConfigMigrator.migrate(json), EssentialsConfig.class);
            if (config == null) throw new IOException("Profile did not contain a configuration object");
            config.validate();
            return config;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load profile '" + safeName + "'", exception);
        }
    }

    /** Imports a validated JSON profile into the managed profile directory. */
    public synchronized void importProfile(String name, Path source) {
        String safeName = normalizeName(name);
        try (var reader = Files.newBufferedReader(source)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) throw new IOException("Import file is empty");
            EssentialsConfig config = GSON.fromJson(ConfigMigrator.migrate(json), EssentialsConfig.class);
            if (config == null) throw new IOException("Import file is not a Planet Essentials profile");
            config.validate();
            save(safeName, config);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Profile import was rejected; existing profiles were not changed", exception);
        }
    }

    /** Exports a profile to a user-selected path. */
    public synchronized void exportProfile(String name, Path destination) {
        EssentialsConfig config = load(name);
        writeAtomically(destination, GSON.toJson(config));
    }

    public synchronized boolean delete(String name) {
        String safeName = normalizeName(name);
        try {
            return Files.deleteIfExists(profilePath(safeName));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete profile '" + safeName + "'", exception);
        }
    }

    private Path profilePath(String safeName) {
        return directory.resolve(safeName + ".json").normalize();
    }

    private static void writeAtomically(Path destination, String contents) {
        Path absolute = destination.toAbsolutePath().normalize();
        Path temporary = null;
        try {
            Files.createDirectories(absolute.getParent());
            temporary = Files.createTempFile(absolute.getParent(), "planet-profile-", ".tmp");
            Files.writeString(temporary, contents);
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write profile file " + absolute, exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }
    }
}
