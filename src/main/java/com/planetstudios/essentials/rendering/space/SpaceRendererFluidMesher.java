package com.planetstudios.essentials.rendering.space;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds CPU-only fluid surface geometry from immutable section snapshots.
 *
 * <p>This first pass intentionally handles flat per-cell surfaces and exposed
 * vertical boundaries. It does not yet reproduce vanilla corner-height
 * smoothing, fluid sprites, tinting, or animated UV timing.</p>
 */
public final class SpaceRendererFluidMesher {
    private SpaceRendererFluidMesher() {}

    public static List<FluidGeometry> build(SpaceRendererSectionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Map<String, List<Quad>> byFluid = new LinkedHashMap<>();

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    var sample = snapshot.fluidSample(x, y, z);
                    if (sample == null) continue;

                    List<Quad> quads = byFluid.computeIfAbsent(sample.typeId(), ignored -> new ArrayList<>());
                    float height = clampHeight(sample.height());
                    int light = snapshot.packedLight(x, y, z);

                    var above = snapshot.fluidSample(x, y + 1, z);
                    boolean sameFluidAbove = above != null && sample.typeId().equals(above.typeId());
                    boolean solidAbove = snapshot.blockState(x, y + 1, z).isSolidRender();
                    if (!sameFluidAbove && !solidAbove) {
                        quads.add(topQuad(x, y, z, height, sample.flowX(), sample.flowZ(), light));
                    }

                    appendSide(snapshot, quads, sample.typeId(), x, y, z, 0, 0, -1, height, light);
                    appendSide(snapshot, quads, sample.typeId(), x, y, z, 0, 0, 1, height, light);
                    appendSide(snapshot, quads, sample.typeId(), x, y, z, -1, 0, 0, height, light);
                    appendSide(snapshot, quads, sample.typeId(), x, y, z, 1, 0, 0, height, light);
                }
            }
        }

        List<FluidGeometry> result = new ArrayList<>(byFluid.size());
        for (var entry : byFluid.entrySet()) {
            if (!entry.getValue().isEmpty()) result.add(new FluidGeometry(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }

    private static void appendSide(SpaceRendererSectionSnapshot snapshot, List<Quad> output,
            String typeId, int x, int y, int z, int dx, int dy, int dz,
            float ownHeight, int light) {
        int neighborX = x + dx;
        int neighborY = y + dy;
        int neighborZ = z + dz;
        if (snapshot.blockState(neighborX, neighborY, neighborZ).isSolidRender()) return;

        var neighbor = snapshot.fluidSample(neighborX, neighborY, neighborZ);
        float lowerHeight = 0.0f;
        if (neighbor != null && typeId.equals(neighbor.typeId())) {
            lowerHeight = clampHeight(neighbor.height());
            if (lowerHeight >= ownHeight) return;
        }

        float bottom = y + lowerHeight;
        float top = y + ownHeight;
        if (top <= bottom) return;

        if (dx != 0) {
            float faceX = x + (dx > 0 ? 1.0f : 0.0f);
            float nx = dx;
            if (dx > 0) {
                output.add(new Quad(
                        vertex(faceX, bottom, z + 1, 0, 1, light, nx, 0, 0),
                        vertex(faceX, bottom, z, 1, 1, light, nx, 0, 0),
                        vertex(faceX, top, z, 1, 0, light, nx, 0, 0),
                        vertex(faceX, top, z + 1, 0, 0, light, nx, 0, 0)));
            } else {
                output.add(new Quad(
                        vertex(faceX, bottom, z, 0, 1, light, nx, 0, 0),
                        vertex(faceX, bottom, z + 1, 1, 1, light, nx, 0, 0),
                        vertex(faceX, top, z + 1, 1, 0, light, nx, 0, 0),
                        vertex(faceX, top, z, 0, 0, light, nx, 0, 0)));
            }
        } else {
            float faceZ = z + (dz > 0 ? 1.0f : 0.0f);
            float nz = dz;
            if (dz > 0) {
                output.add(new Quad(
                        vertex(x, bottom, faceZ, 0, 1, light, 0, 0, nz),
                        vertex(x + 1, bottom, faceZ, 1, 1, light, 0, 0, nz),
                        vertex(x + 1, top, faceZ, 1, 0, light, 0, 0, nz),
                        vertex(x, top, faceZ, 0, 0, light, 0, 0, nz)));
            } else {
                output.add(new Quad(
                        vertex(x + 1, bottom, faceZ, 0, 1, light, 0, 0, nz),
                        vertex(x, bottom, faceZ, 1, 1, light, 0, 0, nz),
                        vertex(x, top, faceZ, 1, 0, light, 0, 0, nz),
                        vertex(x + 1, top, faceZ, 0, 0, light, 0, 0, nz)));
            }
        }
    }

    private static Quad topQuad(int x, int y, int z, float height,
            float flowX, float flowZ, int light) {
        double angle = Math.atan2(flowZ, flowX);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Quad(
                topVertex(x, y + height, z, 0, 0, cos, sin, light),
                topVertex(x, y + height, z + 1, 0, 1, cos, sin, light),
                topVertex(x + 1, y + height, z + 1, 1, 1, cos, sin, light),
                topVertex(x + 1, y + height, z, 1, 0, cos, sin, light));
    }

    private static Vertex topVertex(float x, float y, float z, float u, float v,
            float cos, float sin, int light) {
        float centeredU = u - 0.5f;
        float centeredV = v - 0.5f;
        float rotatedU = centeredU * cos - centeredV * sin + 0.5f;
        float rotatedV = centeredU * sin + centeredV * cos + 0.5f;
        return vertex(x, y, z, rotatedU, rotatedV, light, 0, 1, 0);
    }

    private static Vertex vertex(float x, float y, float z, float u, float v,
            int light, float nx, float ny, float nz) {
        return new Vertex(x, y, z, u, v, light, nx, ny, nz);
    }

    private static float clampHeight(float height) {
        return Math.max(0.0f, Math.min(1.0f, height));
    }

    public record Vertex(float x, float y, float z, float u, float v,
            int packedLight, float normalX, float normalY, float normalZ) {}

    public record Quad(Vertex a, Vertex b, Vertex c, Vertex d) {
        public Quad {
            Objects.requireNonNull(a, "a");
            Objects.requireNonNull(b, "b");
            Objects.requireNonNull(c, "c");
            Objects.requireNonNull(d, "d");
        }
    }

    public record FluidGeometry(String typeId, List<Quad> quads) {
        public FluidGeometry {
            Objects.requireNonNull(typeId, "typeId");
            quads = List.copyOf(Objects.requireNonNull(quads, "quads"));
        }
        public int quadCount() { return quads.size(); }
    }
}
