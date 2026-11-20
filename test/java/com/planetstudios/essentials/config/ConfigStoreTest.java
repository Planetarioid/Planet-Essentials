package com.planetstudios.essentials.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    @TempDir Path directory;

    @Test
    void loadAndSaveAreSerializedAcrossCallers() throws Exception {
        assertTrue(Modifier.isSynchronized(ConfigStore.class.getMethod("load").getModifiers()));
        assertTrue(Modifier.isSynchronized(ConfigStore.class.getMethod("save", EssentialsConfig.class).getModifiers()));
    }

    @Test
    void configValidationClampsBeforePersistence() {
        EssentialsConfig config = new EssentialsConfig();
        config.diagnosticsHistorySize = Integer.MAX_VALUE;
        config.validate();
        assertEquals(1200, config.diagnosticsHistorySize);
    }
}
