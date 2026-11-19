package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Declarative completeness record for a vanilla terrain render-pass capture.
 *
 * <p>The values in this record are evidence that the future render-group hook
 * captured each category from the active vanilla pass. It does not synthesize
 * or guess state. A snapshot is not complete merely because a category is
 * expected to have a default value.</p>
 */
public record PrismTerrainRenderPassState(
        ChunkSectionLayer layer,
        Set<Facet> capturedFacets,
        Set<String> capturedSamplers) {

    public enum Facet {
        RENDER_TARGET,
        VIEWPORT,
        SCISSOR,
        DEPTH_STENCIL,
        BLEND,
        CULL,
        COLOR_MASK,
        LIGHTMAP,
        OVERLAY,
        FOG,
        MODEL_VIEW_MATRIX,
        PROJECTION_MATRIX,
        DEFAULT_UNIFORMS,
        LAYER_PIPELINE,
        DRAW_ORDER
    }

    private static final Set<Facet> REQUIRED = Set.copyOf(EnumSet.allOf(Facet.class));

    public PrismTerrainRenderPassState {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(capturedFacets, "capturedFacets");
        Objects.requireNonNull(capturedSamplers, "capturedSamplers");
        capturedFacets = Set.copyOf(capturedFacets);
        capturedSamplers = Set.copyOf(capturedSamplers);
    }

    /** True only when every required vanilla state category and declared sampler was captured. */
    public boolean isCompleteFor(ChunkSectionLayer expectedLayer, Set<String> requiredSamplers) {
        Objects.requireNonNull(expectedLayer, "expectedLayer");
        Objects.requireNonNull(requiredSamplers, "requiredSamplers");
        return layer == expectedLayer
                && capturedFacets.containsAll(REQUIRED)
                && capturedSamplers.containsAll(requiredSamplers);
    }

    public static Set<Facet> requiredFacets() {
        return REQUIRED;
    }
}
