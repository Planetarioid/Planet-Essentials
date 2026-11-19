package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainRenderPassStateTest {
    @Test void completeCaptureRequiresEveryFacetAndDeclaredSampler() {
        var state = new PrismTerrainRenderPassState(ChunkSectionLayer.SOLID,
                PrismTerrainRenderPassState.requiredFacets(), Set.of("LightSampler", "OverlaySampler"));
        assertTrue(state.isCompleteFor(ChunkSectionLayer.SOLID,
                Set.of("LightSampler", "OverlaySampler")));
    }

    @Test void missingFacetKeepsVanillaFallback() {
        var facets = EnumSet.allOf(PrismTerrainRenderPassState.Facet.class);
        facets.remove(PrismTerrainRenderPassState.Facet.RENDER_TARGET);
        var state = new PrismTerrainRenderPassState(ChunkSectionLayer.SOLID, facets, Set.of());
        assertFalse(state.isCompleteFor(ChunkSectionLayer.SOLID, Set.of()));
    }

    @Test void missingSamplerKeepsVanillaFallback() {
        var state = new PrismTerrainRenderPassState(ChunkSectionLayer.CUTOUT,
                PrismTerrainRenderPassState.requiredFacets(), Set.of("LightSampler"));
        assertFalse(state.isCompleteFor(ChunkSectionLayer.CUTOUT,
                Set.of("LightSampler", "OverlaySampler")));
    }

    @Test void wrongLayerKeepsVanillaFallback() {
        var state = new PrismTerrainRenderPassState(ChunkSectionLayer.SOLID,
                PrismTerrainRenderPassState.requiredFacets(), Set.of());
        assertFalse(state.isCompleteFor(ChunkSectionLayer.CUTOUT, Set.of()));
    }

    @Test void capturedCollectionsAreImmutableCopies() {
        var facets = EnumSet.allOf(PrismTerrainRenderPassState.Facet.class);
        var samplers = new java.util.HashSet<>(Set.of("LightSampler"));
        var state = new PrismTerrainRenderPassState(ChunkSectionLayer.SOLID, facets, samplers);
        facets.clear();
        samplers.clear();
        assertTrue(state.isCompleteFor(ChunkSectionLayer.SOLID, Set.of("LightSampler")));
        assertThrows(UnsupportedOperationException.class,
                () -> state.capturedSamplers().add("Other"));
    }
}
