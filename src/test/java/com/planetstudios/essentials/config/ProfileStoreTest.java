package com.planetstudios.essentials.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileStoreTest {
    @TempDir Path directory;

    @Test
    void savesListsLoadsAndDeletesProfiles() {
        ProfileStore store = new ProfileStore(directory);
        EssentialsConfig config = new EssentialsConfig();
        config.diagnosticsEnabled = false;
        store.save("competitive", config);

        assertEquals(List.of("competitive"), store.listProfiles());
        assertFalse(store.load("competitive").diagnosticsEnabled);
        assertTrue(store.delete("competitive"));
        assertTrue(store.listProfiles().isEmpty());
    }

    @Test
    void rejectsTraversalNamesAndMalformedImportsWithoutChangingProfiles() throws Exception {
        ProfileStore store = new ProfileStore(directory);
        store.save("default", new EssentialsConfig());

        assertThrows(IllegalArgumentException.class, () -> store.save("../outside", new EssentialsConfig()));
        Path invalid = directory.resolve("invalid.json");
        Files.writeString(invalid, "{ definitely not json");
        assertThrows(IllegalArgumentException.class, () -> store.importProfile("default", invalid));
        assertTrue(store.load("default").diagnosticsEnabled);
    }

    @Test
    void exportsAndImportsValidatedProfiles() {
        ProfileStore store = new ProfileStore(directory.resolve("profiles"));
        EssentialsConfig config = new EssentialsConfig();
        config.diagnosticsEnabled = false;
        store.save("source", config);
        Path exported = directory.resolve("backup.json");
        store.exportProfile("source", exported);
        store.importProfile("restored", exported);
        assertFalse(store.load("restored").diagnosticsEnabled);
    }
}
