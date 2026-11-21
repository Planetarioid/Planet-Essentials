package com.planetstudios.essentials.rendering.space;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertex;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.EnumMap;
import java.util.Map;

/**
 * Builds independent section geometry from Minecraft's baked block models.
 *
 * <p>This pass preserves baked quad geometry, atlas UVs, face normals, deterministic
 * model variants, corner-sampled block/sky light, biome/mod block tint, and a
 * conservative ambient-occlusion approximation. Translucent layers, fluids,
 * and block entities remain separate work.</p>
 */
public final class SpaceRendererBlockModelMesher {
    /** The vanilla block atlas used by baked block-model UV coordinates. */
    public static final String BLOCK_ATLAS_MATERIAL = "minecraft:textures/atlas/blocks.png";
    private static final int NO_OVERLAY = 0;

    private SpaceRendererBlockModelMesher() {}

    /**
     * Builds one independent payload per vanilla chunk render layer.
     * Layer selection is delegated to Minecraft rather than inferred from solidity.
     */
    public static Optional<List<LayeredPayload>> build(SpaceRendererSectionSnapshot snapshot) {
        if (snapshot == null) throw new NullPointerException("snapshot");
        var key = snapshot.key();
        var builders = new EnumMap<ChunkSectionLayer, PrismTerrainVertexData.Builder>(ChunkSectionLayer.class);
        var quadCounts = new EnumMap<ChunkSectionLayer, Integer>(ChunkSectionLayer.class);
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = snapshot.blockState(x, y, z);
                    if (!isSupportedModelState(state)) continue;

                    ChunkSectionLayer layer = ItemBlockRenderTypes.getChunkRenderType(state);
                    var builder = builders.computeIfAbsent(layer, selected ->
                            PrismTerrainVertexData.builder(key.x(), key.y(), key.z(),
                                    BLOCK_ATLAS_MATERIAL, stageFor(selected)));
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
                            quadCounts.merge(layer, 1, Integer::sum);
                        }
                        for (Direction side : Direction.values()) {
                            if (snapshot.blockState(x + side.getStepX(), y + side.getStepY(),
                                    z + side.getStepZ()).isSolidRender()) continue;
                            for (BakedQuad quad : part.getQuads(side)) {
                                appendQuad(builder, snapshot, quad, x, y, z, packedLight);
                                quadCounts.merge(layer, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }

        if (builders.isEmpty()) return Optional.empty();
        List<LayeredPayload> batches = new java.util.ArrayList<>(builders.size());
        for (Map.Entry<ChunkSectionLayer, PrismTerrainVertexData.Builder> entry : builders.entrySet()) {
            if (quadCounts.getOrDefault(entry.getKey(), 0) == 0) continue;
            batches.add(new LayeredPayload(entry.getKey(), entry.getValue().build()));
        }
        if (batches.isEmpty()) return Optional.empty();
        return Optional.of(List.copyOf(batches));
    }

    static PrismFrameStage stageFor(ChunkSectionLayer layer) {
        return layer == ChunkSectionLayer.TRANSLUCENT || layer == ChunkSectionLayer.TRIPWIRE
                ? PrismFrameStage.BEFORE_TRANSLUCENT : PrismFrameStage.BEFORE_ENTITIES;
    }

    public record LayeredPayload(ChunkSectionLayer layer, PrismTerrainVertexData data) {
        public LayeredPayload {
            java.util.Objects.requireNonNull(layer, "layer");
            java.util.Objects.requireNonNull(data, "data");
        }
    }

    static boolean isSupportedModelState(BlockState state) {
        return state != null && !state.hasBlockEntity()
                && state.getRenderShape() == RenderShape.MODEL;
    }

    private static void appendQuad(PrismTerrainVertexData.Builder builder,
            SpaceRendererSectionSnapshot snapshot, BakedQuad quad,
            int blockX, int blockY, int blockZ, int packedLight) {
        Direction face = quad.direction();
        int packedNormal = (face.getStepX() & 0xFF)
                | ((face.getStepY() & 0xFF) << 8)
                | ((face.getStepZ() & 0xFF) << 16);
        int tint = quad.tintIndex() >= 0
                ? snapshot.tintColor(blockX, blockY, blockZ, quad.tintIndex()) : -1;
        PrismTerrainVertex[] vertices = new PrismTerrainVertex[4];
        for (int i = 0; i < 4; i++) {
            var position = quad.position(i);
            long packedUv = quad.packedUV(i);
            VertexLighting lighting = vertexLighting(snapshot, blockX, blockY, blockZ,
                    position.x(), position.y(), position.z(), face, packedLight);
            vertices[i] = new PrismTerrainVertex(
                    blockX + position.x(), blockY + position.y(), blockZ + position.z(),
                    unpackU(packedUv), unpackV(packedUv),
                    multiplyTintAndAo(tint, lighting.color()),
                    lighting.packedLight(), NO_OVERLAY, packedNormal);
        }
        builder.addQuad(vertices[0], vertices[1], vertices[2], vertices[3]);
    }

    /** Multiplies resolved RGB tint by AO while keeping the AO alpha channel intact. */
    static int multiplyTintAndAo(int tintRgb, int aoRgba) {
        if (tintRgb == -1) return aoRgba;
        int alpha = (aoRgba >>> 24) & 0xFF;
        int red = (((tintRgb >>> 16) & 0xFF) * ((aoRgba >>> 16) & 0xFF) + 127) / 255;
        int green = (((tintRgb >>> 8) & 0xFF) * ((aoRgba >>> 8) & 0xFF) + 127) / 255;
        int blue = ((tintRgb & 0xFF) * (aoRgba & 0xFF) + 127) / 255;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
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
