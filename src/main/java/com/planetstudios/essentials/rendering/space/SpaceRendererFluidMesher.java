package com.planetstudios.essentials.rendering.space;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds CPU-only fluid geometry from immutable section snapshots.
 *
 * <p>Surface corner heights use a weighted neighborhood average so sloped
 * fluid surfaces follow the same broad shape as vanilla fluid rendering.</p>
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
                    CornerHeights heights = cornerHeights(snapshot, sample.typeId(), x, y, z);
                    int light = snapshot.packedLight(x, y, z);
                    var visual = snapshot.fluidVisual(x, y, z);
                    var stillSprite = visual == null ? null : visual.still();
                    var flowingSprite = visual == null ? null : visual.flowing();
                    int tintColor = visual == null ? -1 : visual.tintColor();

                    var above = snapshot.fluidSample(x, y + 1, z);
                    boolean sameFluidAbove = above != null && sample.typeId().equals(above.typeId());
                    boolean solidAbove = snapshot.blockState(x, y + 1, z).isSolidRender();
                    if (!sameFluidAbove && !solidAbove) {
                        quads.add(topQuad(x, y, z, heights,
                                sample.flowX(), sample.flowZ(), light, stillSprite, tintColor));
                    }

                    appendSide(snapshot, quads, sample.typeId(), x, y, z,
                            0, -1, heights.northEast(), heights.northWest(), light, flowingSprite, tintColor);
                    appendSide(snapshot, quads, sample.typeId(), x, y, z,
                            0, 1, heights.southWest(), heights.southEast(), light, flowingSprite, tintColor);
                    appendSide(snapshot, quads, sample.typeId(), x, y, z,
                            -1, 0, heights.northWest(), heights.southWest(), light, flowingSprite, tintColor);
                    appendSide(snapshot, quads, sample.typeId(), x, y, z,
                            1, 0, heights.southEast(), heights.northEast(), light, flowingSprite, tintColor);
                }
            }
        }

        List<FluidGeometry> result = new ArrayList<>(byFluid.size());
        for (var entry : byFluid.entrySet()) {
            if (!entry.getValue().isEmpty()) result.add(new FluidGeometry(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }

    static CornerHeights cornerHeights(SpaceRendererSectionSnapshot snapshot,
            String typeId, int x, int y, int z) {
        float self = surfaceHeight(snapshot, typeId, x, y, z);
        float north = surfaceHeight(snapshot, typeId, x, y, z - 1);
        float south = surfaceHeight(snapshot, typeId, x, y, z + 1);
        float west = surfaceHeight(snapshot, typeId, x - 1, y, z);
        float east = surfaceHeight(snapshot, typeId, x + 1, y, z);

        float northEast = averageCorner(snapshot, typeId, self, north, east, x + 1, y, z - 1);
        float northWest = averageCorner(snapshot, typeId, self, north, west, x - 1, y, z - 1);
        float southEast = averageCorner(snapshot, typeId, self, south, east, x + 1, y, z + 1);
        float southWest = averageCorner(snapshot, typeId, self, south, west, x - 1, y, z + 1);
        return new CornerHeights(northWest, southWest, southEast, northEast);
    }

    private static float surfaceHeight(SpaceRendererSectionSnapshot snapshot,
            String typeId, int x, int y, int z) {
        var sample = snapshot.fluidSample(x, y, z);
        if (sample != null && typeId.equals(sample.typeId())) {
            var above = snapshot.fluidSample(x, y + 1, z);
            return above != null && typeId.equals(above.typeId())
                    ? 1.0f : clampHeight(sample.ownHeight());
        }
        return snapshot.blockState(x, y, z).isSolid() ? -1.0f : 0.0f;
    }

    private static float averageCorner(SpaceRendererSectionSnapshot snapshot, String typeId,
            float self, float firstSide, float secondSide, int cornerX, int y, int cornerZ) {
        if (firstSide >= 1.0f || secondSide >= 1.0f) return 1.0f;

        float weightedTotal = 0.0f;
        float totalWeight = 0.0f;
        if (firstSide > 0.0f || secondSide > 0.0f) {
            float diagonal = surfaceHeight(snapshot, typeId, cornerX, y, cornerZ);
            if (diagonal >= 1.0f) return 1.0f;
            if (diagonal >= 0.8f) {
                weightedTotal += diagonal * 10.0f;
                totalWeight += 10.0f;
            } else if (diagonal >= 0.0f) {
                weightedTotal += diagonal;
                totalWeight += 1.0f;
            }
        }

        if (self >= 0.8f) {
            weightedTotal += self * 10.0f;
            totalWeight += 10.0f;
        } else if (self >= 0.0f) {
            weightedTotal += self;
            totalWeight += 1.0f;
        }
        if (firstSide >= 0.8f) {
            weightedTotal += firstSide * 10.0f;
            totalWeight += 10.0f;
        } else if (firstSide >= 0.0f) {
            weightedTotal += firstSide;
            totalWeight += 1.0f;
        }
        if (secondSide >= 0.8f) {
            weightedTotal += secondSide * 10.0f;
            totalWeight += 10.0f;
        } else if (secondSide >= 0.0f) {
            weightedTotal += secondSide;
            totalWeight += 1.0f;
        }

        return totalWeight == 0.0f ? clampHeight(self) : weightedTotal / totalWeight;
    }

    private static void appendSide(SpaceRendererSectionSnapshot snapshot, List<Quad> output,
            String typeId, int x, int y, int z, int dx, int dz,
            float firstTop, float secondTop, int light,
            SpaceRendererSectionSnapshot.FluidSprite sprite, int tintColor) {
        int neighborX = x + dx;
        int neighborZ = z + dz;
        if (snapshot.blockState(neighborX, y, neighborZ).isSolidRender()) return;

        var neighbor = snapshot.fluidSample(neighborX, y, neighborZ);
        if (neighbor != null && typeId.equals(neighbor.typeId())) return;
        if (firstTop <= 0.0f && secondTop <= 0.0f) return;

        float faceX = x + (dx > 0 ? 1.0f : 0.0f);
        float faceZ = z + (dz > 0 ? 1.0f : 0.0f);
        float nx = dx;
        float nz = dz;
        Vertex bottomA;
        Vertex bottomB;
        Vertex topA;
        Vertex topB;

        if (dx > 0) {
            bottomA = vertex(faceX, y, z + 1, 0, 1, light, nx, 0, nz);
            bottomB = vertex(faceX, y, z, 1, 1, light, nx, 0, nz);
            topA = vertex(faceX, y + firstTop, z + 1, 0, 0, light, nx, 0, nz);
            topB = vertex(faceX, y + secondTop, z, 1, 0, light, nx, 0, nz);
        } else if (dx < 0) {
            bottomA = vertex(faceX, y, z, 0, 1, light, nx, 0, nz);
            bottomB = vertex(faceX, y, z + 1, 1, 1, light, nx, 0, nz);
            topA = vertex(faceX, y + firstTop, z, 0, 0, light, nx, 0, nz);
            topB = vertex(faceX, y + secondTop, z + 1, 1, 0, light, nx, 0, nz);
        } else if (dz > 0) {
            bottomA = vertex(x, y, faceZ, 0, 1, light, nx, 0, nz);
            bottomB = vertex(x + 1, y, faceZ, 1, 1, light, nx, 0, nz);
            topA = vertex(x, y + firstTop, faceZ, 0, 0, light, nx, 0, nz);
            topB = vertex(x + 1, y + secondTop, faceZ, 1, 0, light, nx, 0, nz);
        } else {
            bottomA = vertex(x + 1, y, faceZ, 0, 1, light, nx, 0, nz);
            bottomB = vertex(x, y, faceZ, 1, 1, light, nx, 0, nz);
            topA = vertex(x + 1, y + firstTop, faceZ, 0, 0, light, nx, 0, nz);
            topB = vertex(x, y + secondTop, faceZ, 1, 0, light, nx, 0, nz);
        }

        output.add(new Quad(withSpriteUv(bottomA, sprite), withSpriteUv(bottomB, sprite),
                withSpriteUv(topB, sprite), withSpriteUv(topA, sprite),
                sprite == null ? null : sprite.atlasId(), tintColor));
    }

    private static Quad topQuad(int x, int y, int z, CornerHeights heights,
            float flowX, float flowZ, int light,
            SpaceRendererSectionSnapshot.FluidSprite sprite, int tintColor) {
        double angle = Math.atan2(flowZ, flowX);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Quad(
                topVertex(x, y + heights.northWest(), z, 0, 0, cos, sin, light, sprite),
                topVertex(x, y + heights.southWest(), z + 1, 0, 1, cos, sin, light, sprite),
                topVertex(x + 1, y + heights.southEast(), z + 1, 1, 1, cos, sin, light, sprite),
                topVertex(x + 1, y + heights.northEast(), z, 1, 0, cos, sin, light, sprite),
                sprite == null ? null : sprite.atlasId(), tintColor);
    }

    private static Vertex topVertex(float x, float y, float z, float u, float v,
            float cos, float sin, int light,
            SpaceRendererSectionSnapshot.FluidSprite sprite) {
        float centeredU = u - 0.5f;
        float centeredV = v - 0.5f;
        float rotatedU = centeredU * cos - centeredV * sin + 0.5f;
        float rotatedV = centeredU * sin + centeredV * cos + 0.5f;
        return vertex(x, y, z, mapU(rotatedU, sprite), mapV(rotatedV, sprite), light, 0, 1, 0);
    }

    private static float mapU(float u, SpaceRendererSectionSnapshot.FluidSprite sprite) {
        return sprite == null ? u : sprite.u0() + u * (sprite.u1() - sprite.u0());
    }

    private static float mapV(float v, SpaceRendererSectionSnapshot.FluidSprite sprite) {
        return sprite == null ? v : sprite.v0() + v * (sprite.v1() - sprite.v0());
    }

    private static Vertex withSpriteUv(Vertex vertex, SpaceRendererSectionSnapshot.FluidSprite sprite) {
        return new Vertex(vertex.x(), vertex.y(), vertex.z(),
                mapU(vertex.u(), sprite), mapV(vertex.v(), sprite), vertex.packedLight(),
                vertex.normalX(), vertex.normalY(), vertex.normalZ());
    }

    private static Vertex vertex(float x, float y, float z, float u, float v,
            int light, float nx, float ny, float nz) {
        return new Vertex(x, y, z, u, v, light, nx, ny, nz);
    }

    private static float clampHeight(float height) {
        return Math.max(0.0f, Math.min(1.0f, height));
    }

    record CornerHeights(float northWest, float southWest, float southEast, float northEast) {}

    public record Vertex(float x, float y, float z, float u, float v,
            int packedLight, float normalX, float normalY, float normalZ) {}

    public record Quad(Vertex a, Vertex b, Vertex c, Vertex d, String atlasId, int tintColor) {
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
