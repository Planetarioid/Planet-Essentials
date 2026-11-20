package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import java.util.EnumSet;
import java.util.HashSet;
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
        DEPTH_TEST,
        STENCIL_STATE,
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

    /**
     * Converts only facts present in a live vanilla snapshot. Unobserved state
     * remains absent, so this adapter cannot accidentally open the replacement gate.
     */
    public static PrismTerrainRenderPassState fromVanillaSnapshot(
            ChunkSectionLayer layer, PrismTerrainVanillaPassSnapshot snapshot) {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.group().layers().length == 0
                || java.util.Arrays.stream(snapshot.group().layers()).noneMatch(candidate -> candidate == layer)) {
            throw new IllegalArgumentException("Layer is not part of the captured vanilla group: " + layer);
        }

        EnumSet<Facet> facets = EnumSet.noneOf(Facet.class);
        HashSet<String> samplers = new HashSet<>();
        if (snapshot.target() != null && snapshot.colorAttachment() != null) {
            facets.add(Facet.RENDER_TARGET);
        }
        if (snapshot.passOptions().viewport().isPresent()) facets.add(Facet.VIEWPORT);
        if (snapshot.passOptions().scissor().isPresent()) facets.add(Facet.SCISSOR);
        if (!snapshot.passOptions().stencilAttachment()) facets.add(Facet.STENCIL_STATE);
        PrismTerrainVanillaPassSnapshot.LayerInput layerInput = snapshot.layers().get(layer);
        if (layerInput != null && layerInput.pipelineApplied()) {
            PrismTerrainVanillaPassSnapshot.PipelineState pipeline = layerInput.state();
            if (pipeline.depthTest() != null) facets.add(Facet.DEPTH_TEST);
            if (pipeline.blend() != null) facets.add(Facet.BLEND);
            facets.add(Facet.CULL);
            facets.add(Facet.COLOR_MASK);
            facets.add(Facet.LAYER_PIPELINE);
            if (pipeline.vertexFormat() == DefaultVertexFormat.BLOCK) facets.add(Facet.OVERLAY);
            if (layerInput.orderedDraws() != null) facets.add(Facet.DRAW_ORDER);
        }
        if (snapshot.lightmap() != null && snapshot.lightmapSampler() != null) {
            facets.add(Facet.LIGHTMAP);
            samplers.add("Sampler2");
        }
        if (layerInput != null && layerInput.pipelineApplied()
                && snapshot.blockAtlas() != null && snapshot.blockAtlasSampler() != null) {
            samplers.add("Sampler0");
        }
        // The snapshot retains both present and absent optional default bindings.
        // Default-uniform inputs are captured as nullable resources. A missing
        // projection buffer is not evidence that the projection state was replayed.
        if (snapshot.defaultUniforms() != null) {
            facets.add(Facet.FOG); // Presence or absence is explicitly captured.
            facets.add(Facet.DEFAULT_UNIFORMS);
            if (snapshot.defaultUniforms().projection() != null) {
                facets.add(Facet.PROJECTION_MATRIX);
            }
        }
        if (snapshot.modelView() != null) facets.add(Facet.MODEL_VIEW_MATRIX);
        return new PrismTerrainRenderPassState(layer, facets, samplers);
    }

    /** Returns the exact missing state categories for diagnostics and fail-closed decisions. */
    public Set<Facet> missingFacets() {
        EnumSet<Facet> missing = EnumSet.copyOf(REQUIRED);
        missing.removeAll(capturedFacets);
        return Set.copyOf(missing);
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
