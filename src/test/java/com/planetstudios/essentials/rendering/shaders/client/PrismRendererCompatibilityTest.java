package com.planetstudios.essentials.rendering.shaders.client;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrismRendererCompatibilityTest {
    @Test
    void selectsVanillaWhenNoAlternateRendererIsPresent() {
        var result = PrismRendererCompatibility.detect(Set.of(), false);
        assertEquals(PrismRendererCompatibility.Renderer.VANILLA, result.renderer());
        assertFalse(result.supportsAutomaticTerrainReplacement());
        assertFalse(result.supportsAutomaticEntityReplacement());
    }

    @Test
    void detectsSodiumAndKeepsIrisAsAnAddonSignal() {
        var result = PrismRendererCompatibility.detect(Set.of("sodium", "indium"), true);
        assertEquals(PrismRendererCompatibility.Renderer.SODIUM, result.renderer());
        assertTrue(result.irisPresent());
        assertTrue(result.detectedRendererMods().contains("indium"));
        assertFalse(result.supportsAutomaticTerrainReplacement());
    }

    @Test
    void prioritizesExplicitAlternativeRenderersDeterministically() {
        assertEquals(PrismRendererCompatibility.Renderer.VULKANMOD,
                PrismRendererCompatibility.detect(Set.of("sodium", "canvas", "vulkanmod"), false).renderer());
        assertEquals(PrismRendererCompatibility.Renderer.CANVAS,
                PrismRendererCompatibility.detect(Set.of("sodium", "canvas"), false).renderer());
    }

    @Test
    void unknownRendererCompatibilityNeverClaimsPipelineSupport() {
        var result = PrismRendererCompatibility.detect(Set.of("optifabric"), false);
        assertEquals(PrismRendererCompatibility.Renderer.UNKNOWN, result.renderer());
        assertFalse(result.supportsAutomaticTerrainReplacement());
        assertFalse(result.supportsAutomaticEntityReplacement());
    }
}
