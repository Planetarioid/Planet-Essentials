package com.planetstudios.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackCategoryConfigTest {
    @Test void defaultsToVanillaCompatibleAllEnabledBehavior() {
        ResourcePackCategoryConfig config = new ResourcePackCategoryConfig();
        assertTrue(config.allEnabled());
    }

    @Test void reportsAdvancedFilteringWhenAnyCategoryIsDisabled() {
        ResourcePackCategoryConfig config = new ResourcePackCategoryConfig();
        config.animations = false;
        assertFalse(config.allEnabled());
    }
}
