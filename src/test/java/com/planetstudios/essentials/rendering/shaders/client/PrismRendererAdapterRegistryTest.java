package com.planetstudios.essentials.rendering.shaders.client;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrismRendererAdapterRegistryTest {
    @Test
    void registersAndRemovesExplicitAdapter() {
        PrismRendererAdapter adapter = adapter("sodium");
        try (var ignored = PrismRendererAdapterRegistry.register(adapter)) {
            assertSame(adapter, PrismRendererAdapterRegistry.find("sodium").orElseThrow());
            assertTrue(PrismRendererAdapterRegistry.snapshot().containsKey("sodium"));
        }
        assertTrue(PrismRendererAdapterRegistry.find("sodium").isEmpty());
    }

    @Test
    void rejectsDuplicateAndMalformedIds() {
        PrismRendererAdapter first = adapter("sodium");
        try (var ignored = PrismRendererAdapterRegistry.register(first)) {
            assertThrows(IllegalArgumentException.class,
                    () -> PrismRendererAdapterRegistry.register(adapter("sodium")));
        }
        assertThrows(IllegalArgumentException.class,
                () -> PrismRendererAdapterRegistry.register(adapter("../sodium")));
    }

    @Test
    void adapterMustExplicitlyDeclareFeatures() {
        PrismRendererAdapter terrainOnly = new PrismRendererAdapter() {
            public String rendererId() { return "test_renderer"; }
            public String testedVersion() { return "1.0.x"; }
            public Set<Feature> supportedFeatures() { return Set.of(Feature.CHUNK_TERRAIN_PIPELINE); }
        };
        try (var ignored = PrismRendererAdapterRegistry.register(terrainOnly)) {
            var resolved = PrismRendererAdapterRegistry.find("test_renderer").orElseThrow();
            assertTrue(resolved.supportedFeatures().contains(PrismRendererAdapter.Feature.CHUNK_TERRAIN_PIPELINE));
            assertFalse(resolved.supportedFeatures().contains(PrismRendererAdapter.Feature.ENTITY_PIPELINE));
        }
    }

    private static PrismRendererAdapter adapter(String id) {
        return new PrismRendererAdapter() {
            public String rendererId() { return id; }
            public String testedVersion() { return "test"; }
            public Set<Feature> supportedFeatures() { return Set.of(); }
        };
    }
}
