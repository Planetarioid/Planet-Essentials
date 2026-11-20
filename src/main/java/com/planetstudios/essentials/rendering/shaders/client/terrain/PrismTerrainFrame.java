package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable extraction-to-draw handoff for one world frame. */
public record PrismTerrainFrame(long generation, List<PrismTerrainMesh> meshes, int vertexCount) {
    private static final Comparator<PrismTerrainMesh> ORDER =
            Comparator.comparingInt(PrismTerrainMesh::sectionY)
                    .thenComparingInt(PrismTerrainMesh::sectionZ)
                    .thenComparingInt(PrismTerrainMesh::sectionX)
                    .thenComparing(PrismTerrainMesh::stage)
                    .thenComparing(PrismTerrainMesh::materialId);

    public PrismTerrainFrame {
        if (generation < 0 || vertexCount < 0) {
            throw new IllegalArgumentException("Invalid terrain frame generation or vertex count");
        }
        meshes = List.copyOf(Objects.requireNonNull(meshes, "meshes"));
        int actualVertices = meshes.stream().mapToInt(PrismTerrainMesh::vertexCount).sum();
        if (actualVertices != vertexCount) {
            throw new IllegalArgumentException("Terrain frame vertex count does not match its meshes");
        }
    }

    public List<PrismTerrainMesh> meshes(PrismFrameStage stage) {
        Objects.requireNonNull(stage, "stage");
        return meshes.stream().filter(mesh -> mesh.stage() == stage).toList();
    }

    public static Comparator<PrismTerrainMesh> deterministicOrder() { return ORDER; }
}
