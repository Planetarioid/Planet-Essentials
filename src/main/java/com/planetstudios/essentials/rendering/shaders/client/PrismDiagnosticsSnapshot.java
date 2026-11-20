package com.planetstudios.essentials.rendering.shaders.client;

import com.planetstudios.essentials.rendering.shaders.ShaderPackCatalog;
import com.planetstudios.essentials.rendering.shaders.ShaderPipeline;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, read-only view of Prism's current discovery, compilation, and
 * renderer-negotiation state. This reports facts; it does not certify runtime
 * visual compatibility or GPU performance.
 */
public record PrismDiagnosticsSnapshot(
        boolean enabled,
        String selectedPackId,
        String compiledPackId,
        long catalogGeneration,
        int discoveredPackCount,
        List<String> discoveryErrors,
        long pipelineGeneration,
        String backend,
        int compiledProgramCount,
        Map<String, String> programHashes,
        String lastCompileFailure,
        PrismRendererCompatibility.Snapshot renderer,
        int worldPostProcessPassCount) {

    public PrismDiagnosticsSnapshot {
        selectedPackId = Objects.requireNonNull(selectedPackId, "selectedPackId");
        compiledPackId = Objects.requireNonNull(compiledPackId, "compiledPackId");
        discoveryErrors = List.copyOf(discoveryErrors);
        backend = Objects.requireNonNull(backend, "backend");
        programHashes = Map.copyOf(programHashes);
        lastCompileFailure = Objects.requireNonNull(lastCompileFailure, "lastCompileFailure");
        Objects.requireNonNull(renderer, "renderer");
        if (catalogGeneration < 0 || discoveredPackCount < 0 || pipelineGeneration < 0
                || compiledProgramCount < 0 || worldPostProcessPassCount < 0) {
            throw new IllegalArgumentException("Prism diagnostic counts and generations must not be negative");
        }
    }

    public static PrismDiagnosticsSnapshot capture() {
        ShaderPackCatalog.Snapshot catalog = ShaderPackRuntime.catalogSnapshot();
        ShaderPipeline.Snapshot pipeline = ShaderPackRuntime.pipelineSnapshot();
        var graph = ShaderPackRuntime.worldPostProcessGraph();
        return new PrismDiagnosticsSnapshot(
                ShaderPackRuntime.isEnabled(),
                ShaderPackRuntime.selectedPackId(),
                ShaderPackRuntime.compiledPackId(),
                catalog.generation(),
                catalog.packs().size(),
                catalog.errors(),
                pipeline.generation(),
                pipeline.backend(),
                pipeline.programCount(),
                pipeline.sourceHashes(),
                pipeline.lastFailure(),
                PrismRendererCompatibility.detectInstalled(),
                graph == null ? 0 : graph.passes().size());
    }
}
