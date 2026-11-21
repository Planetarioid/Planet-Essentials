package com.planetstudios.essentials.rendering.space;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainGpuMesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Immutable render-thread snapshot connecting the current vanilla visible draw
 * list to Space Renderer-owned GPU meshes. Incomplete snapshots are never
 * eligible for takeover; vanilla remains the authoritative fallback.
 */
public final class SpaceRendererFrameSnapshot {
    private final long sequence;
    private final List<Draw> draws;
    private final int requiredDraws;
    private final int missingMeshes;

    private SpaceRendererFrameSnapshot(long sequence, List<Draw> draws,
            int requiredDraws, int missingMeshes) {
        this.sequence = sequence;
        this.draws = List.copyOf(draws);
        this.requiredDraws = requiredDraws;
        this.missingMeshes = missingMeshes;
    }

    public static SpaceRendererFrameSnapshot capture(long sequence,
            List<DrawInput> inputs,
            Function<DrawInput, PrismTerrainGpuMesh> meshLookup) {
        if (sequence <= 0) throw new IllegalArgumentException("Frame sequence must be positive");
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(meshLookup, "meshLookup");
        List<Draw> resolved = new ArrayList<>(inputs.size());
        Set<Object> identities = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int missing = 0;
        for (DrawInput input : inputs) {
            Objects.requireNonNull(input, "draw input");
            if (!identities.add(input.vanillaDraw())) {
                throw new IllegalArgumentException("A vanilla draw identity may appear only once per frame");
            }
            PrismTerrainGpuMesh mesh = meshLookup.apply(input);
            if (mesh == null || !mesh.isOpen() || mesh.vertexCount() <= 0 || mesh.vertexCount() % 4 != 0) {
                missing++;
                continue;
            }
            resolved.add(new Draw(input, mesh));
        }
        return new SpaceRendererFrameSnapshot(sequence, resolved, inputs.size(), missing);
    }

    public static SpaceRendererFrameSnapshot empty(long sequence) {
        if (sequence <= 0) throw new IllegalArgumentException("Frame sequence must be positive");
        return new SpaceRendererFrameSnapshot(sequence, List.of(), 0, 0);
    }

    public long sequence() { return sequence; }
    public List<Draw> draws() { return draws; }
    public int requiredDraws() { return requiredDraws; }
    public int resolvedDraws() { return draws.size(); }
    public int missingMeshes() { return missingMeshes; }
    public boolean isComplete() {
        return requiredDraws > 0 && missingMeshes == 0 && draws.size() == requiredDraws
                && draws.stream().allMatch(draw -> draw.mesh().isOpen());
    }
    public boolean maySubmit() { return isComplete(); }

    public record DrawInput(Object vanillaDraw, SpaceRendererSectionManager.SectionKey section,
            String materialId, PrismFrameStage stage, String layerId, int layerDrawIndex) {
        public DrawInput {
            Objects.requireNonNull(vanillaDraw, "vanillaDraw");
            Objects.requireNonNull(section, "section");
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(stage, "stage");
            Objects.requireNonNull(layerId, "layerId");
            if (layerId.isBlank() || layerDrawIndex < 0) {
                throw new IllegalArgumentException("Invalid vanilla terrain draw metadata");
            }
        }
    }

    public record Draw(DrawInput input, PrismTerrainGpuMesh mesh) {
        public Draw {
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(mesh, "mesh");
        }
    }
}
