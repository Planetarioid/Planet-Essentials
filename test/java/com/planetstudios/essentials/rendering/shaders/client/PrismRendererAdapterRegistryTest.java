package com.planetstudios.essentials.rendering.shaders.client;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrismRendererAdapterRegistryTest {
    @Test
    void registersAndRemovesExplicitAdapter() {
        PrismRendererAdapter adapter = adapter("sodium");
        try (var ignored = PrismRendererAdapterRegistry.register(adapter)) {
            var registered = PrismRendererAdapterRegistry.find("sodium").orElseThrow();
            assertNotSame(adapter, registered);
            assertEquals("test", registered.testedVersion());
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
    void freezesFeatureDeclarationsAtRegistrationTime() {
        EnumSet<PrismRendererAdapter.Feature> mutable = EnumSet.of(
                PrismRendererAdapter.Feature.CHUNK_TERRAIN_PIPELINE);
        PrismRendererAdapter source = new PrismRendererAdapter() {
            public String rendererId() { return "test_renderer"; }
            public String testedVersion() { return "1.0.x"; }
            public Set<Feature> supportedFeatures() { return mutable; }
        };
        try (var ignored = PrismRendererAdapterRegistry.register(source)) {
            mutable.add(PrismRendererAdapter.Feature.ENTITY_PIPELINE);
            var registered = PrismRendererAdapterRegistry.find("test_renderer").orElseThrow();
            assertTrue(registered.supportedFeatures().contains(PrismRendererAdapter.Feature.CHUNK_TERRAIN_PIPELINE));
            assertFalse(registered.supportedFeatures().contains(PrismRendererAdapter.Feature.ENTITY_PIPELINE));
            assertThrows(UnsupportedOperationException.class,
                    () -> registered.supportedFeatures().add(PrismRendererAdapter.Feature.ENTITY_PIPELINE));
        }
    }

    @Test
    void rejectsBlankVersionAndNullFeatureSet() {
        PrismRendererAdapter blankVersion = new PrismRendererAdapter() {
            public String rendererId() { return "blank_version"; }
            public String testedVersion() { return "  "; }
            public Set<Feature> supportedFeatures() { return Set.of(); }
        };
        PrismRendererAdapter nullFeatures = new PrismRendererAdapter() {
            public String rendererId() { return "null_features"; }
            public String testedVersion() { return "1.x"; }
            public Set<Feature> supportedFeatures() { return null; }
        };
        assertThrows(IllegalArgumentException.class,
                () -> PrismRendererAdapterRegistry.register(blankVersion));
        assertThrows(NullPointerException.class,
                () -> PrismRendererAdapterRegistry.register(nullFeatures));
    }

    private static PrismRendererAdapter adapter(String id) {
        return new PrismRendererAdapter() {
            public String rendererId() { return id; }
            public String testedVersion() { return "test"; }
            public Set<Feature> supportedFeatures() { return Set.of(); }
        };
    }
}
