package com.planetstudios.essentials.rendering.space;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertex;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * Builds independent section geometry from Minecraft's baked block models.
 *
 * <p>This pass preserves baked quad geometry, atlas UVs, face normals, deterministic
 * model variants, and corner-sampled block/sky light with a conservative
 * ambient-occlusion approximation. Biome tint, translucent layers, fluids,
 * and block entities remain separate work.</p>
 */
public final class SpaceRendererBlockModelMesher {
    /** The vanilla block atlas used by baked block-model UV coordinates. */
    public static final String BLOCK_ATLAS_MATERIAL = "minecraft:textures/atlas/blocks.png";
    private static final int NO_OVERLAY = 0;

    private SpaceRendererBlockModelMesher() {}

    /** Returns empty when the section contains no supported solid model quads. */
    public static Optional<PrismTerrainVertexData> build(SpaceRendererSectionSnapshot snapshot) {
        if (snapshot == null) throw new NullPointerException("snapshot");
        var key = snapshot.key();
        var builder = PrismTerrainVertexData.builder(key.x(), key.y(), key.z(),
                BLOCK_ATLAS_MATERIAL, PrismFrameStage.BEFORE_ENTITIES);
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        int quadCount = 0;

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = snapshot.blockState(x, y, z);
                    if (!state.isSolidRender() || state.getRenderShape() != RenderShape.MODEL) continue;

                    int worldX = key.x() * 16 + x;
                    int worldY = key.y() * 16 + y;
                    int worldZ = key.z() * 16 + z;
                    BlockPos worldPos = new BlockPos(worldX, worldY, worldZ);
                    var model = blockRenderer.getBlockModel(state);
                    List<BlockModelPart> parts = model.collectParts(
                            RandomSource.create(state.getSeed(worldPos)));
                    int packedLight = snapshot.packedLight(x, y, z);

                    for (BlockModelPart part : parts) {
                        for (BakedQuad quad : part.getQuads(null)) {
                            appendQuad(builder, snapshot, quad, x, y, z, packedLight);
                            quadCount++;
                        }
                        for (Direction side : Direction.values()) {
                            if (snapshot.blockState(x + side.getStepX(), y + side.getStepY(),
                                    z + side.getStepZ()).isSolidRender()) {
                                continue;
                            }
                            for (BakedQuad quad : part.getQuads(side)) {
                                appendQuad(builder, snapshot, quad, x, y, z, packedLight);
                                quadCount++;
                            }
                        }
                    }
                }
            }
        }
        return quadCount == 0 ? Optional.empty() : Optional.of(builder.build());
    }

    private static void appendQuad(PrismTerrainVertexData.Builder builder,
            SpaceRendererSectionSnapshot snapshot, BakedQuad quad,
            int blockX, int blockY, int blockZ, int packedLight) {
        Direction face = quad.direction();
        int packedNormal = (face.getStepX() & 0xFF)
                | ((face.getStepY() & 0xFF) << 8)
                | ((face.getStepZ() & 0xFF) << 16);
        PrismTerrainVertex[] vertices = new PrismTerrainVertex[4];
        for (int i = 0; i < 4; i++) {
            var position = quad.position(i);
            long packedUv = quad.packedUV(i);
            VertexLighting lighting = vertexLighting(snapshot, blockX, blockY, blockZ,
                    position.x(), position.y(), position.z(), face, packedLight);
            vertices[i] = new PrismTerrainVertex(
                    blockX + position.x(), blockY + position.y(), blockZ + position.z(),
                    unpackU(packedUv), unpackV(packedUv), lighting.color(),
                    lighting.packedLight(), NO_OVERLAY, packedNormal);
        }
        builder.addQuad(vertices[0], vertices[1], vertices[2], vertices[3]);
    }

    record VertexLighting(int packedLight, int color) {}

    /** Samples the four neighboring cells around a face corner for light and AO. */
    static VertexLighting vertexLighting(SpaceRendererSectionSnapshot snapshot,
            int blockX, int blockY, int blockZ, float vx, float vy, float vz,
            Direction face, int fallbackLight) {
        int[] axes = face.getAxis() == Direction.Axis.X ? new int[]{1, 2}
                : face.getAxis() == Direction.Axis.Y ? new int[]{0, 2} : new int[]{0, 1};
        int[] base = {blockX + face.getStepX(), blockY + face.getStepY(), blockZ + face.getStepZ()};
        float[] vertex = {vx, vy, vz};
        int[] a = new int[3], b = new int[3];
        a[axes[0]] = cornerOffset(vertex[axes[0]]);
        b[axes[1]] = cornerOffset(vertex[axes[1]]);
        int[] p0 = base.clone();
        int[] p1 = add(base, a);
        int[] p2 = add(base, b);
        int[] p3 = add(p1, b);
        int l0 = lightAt(snapshot, p0, fallbackLight);
        int l1 = lightAt(snapshot, p1, fallbackLight);
        int l2 = lightAt(snapshot, p2, fallbackLight);
        int l3 = lightAt(snapshot, p3, fallbackLight);
        int packed = averagePackedLight(l0, l1, l2, l3);
        int sides = (solidAt(snapshot, p1) ? 1 : 0) + (solidAt(snapshot, p2) ? 1 : 0);
        int corner = sides == 2 ? 0 : (solidAt(snapshot, p3) ? 1 : 0);
        float shade = 1.0f - (sides + corner) * 0.12f;
        int channel = Math.max(0, Math.min(255, Math.round(255.0f * shade)));
        int color = (channel << 24) | (channel << 16) | (channel << 8) | channel;
        return new VertexLighting(packed, color);
    }

    private static int cornerOffset(float coordinate) {
        return coordinate <= 0.001f ? -1 : coordinate >= 0.999f ? 1 : 0;
    }

    private static int[] add(int[] first, int[] second) {
        return new int[]{first[0] + second[0], first[1] + second[1], first[2] + second[2]};
    }

    private static int lightAt(SpaceRendererSectionSnapshot snapshot, int[] pos, int fallback) {
        try { return snapshot.packedLight(pos[0], pos[1], pos[2]); }
        catch (IndexOutOfBoundsException ignored) { return fallback; }
    }

    private static boolean solidAt(SpaceRendererSectionSnapshot snapshot, int[] pos) {
        try { return snapshot.blockState(pos[0], pos[1], pos[2]).isSolidRender(); }
        catch (IndexOutOfBoundsException ignored) { return false; }
    }

    private static int averagePackedLight(int a, int b, int c, int d) {
        int block = (((a >>> 4) & 15) + ((b >>> 4) & 15)
                + ((c >>> 4) & 15) + ((d >>> 4) & 15) + 2) / 4;
        int sky = (((a >>> 20) & 15) + ((b >>> 20) & 15)
                + ((c >>> 20) & 15) + ((d >>> 20) & 15) + 2) / 4;
        return (block << 4) | (sky << 20);
    }

    /** Minecraft packs U in the high 32 bits and V in the low 32 bits. */
    public static float unpackU(long packedUv) {
        return Float.intBitsToFloat((int) (packedUv >>> 32));
    }

    public static float unpackV(long packedUv) {
        return Float.intBitsToFloat((int) packedUv);
    }
}