package com.planetstudios.essentials.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ProfileStoreConcurrencyTest {
    @TempDir Path directory;

    @Test
    void concurrentProfileOperationsRemainConsistent() throws Exception {
        ProfileStore store = new ProfileStore(directory);
        int workers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                final int index = i;
                tasks.add(executor.submit(() -> {
                    EssentialsConfig config = new EssentialsConfig();
                    config.diagnosticsEnabled = index % 2 == 0;
                    String name = "profile_" + index;
                    store.save(name, config);
                    assertEquals(config.diagnosticsEnabled, store.load(name).diagnosticsEnabled);
                    assertTrue(store.listProfiles().contains(name));
                }));
            }
            for (Future<?> task : tasks) task.get(10, TimeUnit.SECONDS);
            assertEquals(workers, store.listProfiles().size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void profileStoreSerializesOperationsPerInstance() throws Exception {
        assertTrue(java.lang.reflect.Modifier.isSynchronized(
                ProfileStore.class.getMethod("save", String.class, EssentialsConfig.class).getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isSynchronized(
                ProfileStore.class.getMethod("load", String.class).getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isSynchronized(
                ProfileStore.class.getMethod("delete", String.class).getModifiers()));
    }
}
