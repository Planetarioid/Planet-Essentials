package com.planetstudios.essentials.rendering.shaders.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrismMaterialRegistryTest {
    @Test
    void registersAndResolvesMaterialMetadata() {
        PrismMaterial material = new PrismMaterial(
                "example:stone", "example:terrain", PrismFrameStage.BEFORE_ENTITIES);
        try (var ignored = PrismMaterialRegistry.register(material)) {
            assertEquals(material, PrismMaterialRegistry.find("example:stone").orElseThrow());
            assertEquals(material, PrismMaterialRegistry.snapshot().get("example:stone"));
        }
        assertTrue(PrismMaterialRegistry.find("example:stone").isEmpty());
    }

    @Test
    void rejectsDuplicateMaterialIds() {
        PrismMaterial first = new PrismMaterial(
                "example:stone", "example:terrain", PrismFrameStage.BEFORE_ENTITIES);
        PrismMaterial second = new PrismMaterial(
                "example:stone", "example:other", PrismFrameStage.AFTER_ENTITIES);
        try (var ignored = PrismMaterialRegistry.register(first)) {
            assertThrows(IllegalArgumentException.class,
                    () -> PrismMaterialRegistry.register(second));
        }
    }

    @Test
    void validatesNamespacedMaterialAndProgramIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new PrismMaterial("../stone", "example:terrain", PrismFrameStage.BEFORE_ENTITIES));
        assertThrows(IllegalArgumentException.class,
                () -> new PrismMaterial("example:stone", "example:../terrain", PrismFrameStage.BEFORE_ENTITIES));
        assertThrows(NullPointerException.class,
                () -> new PrismMaterial("example:stone", "example:terrain", null));
    }
}
