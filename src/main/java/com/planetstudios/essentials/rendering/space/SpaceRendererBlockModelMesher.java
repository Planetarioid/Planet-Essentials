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
 * model variants, and sampled vanilla packed block/sky light. Per-vertex AO,
 * biome tint, translucent layers, fluids, and block entities remain separate work.</p>
 */
public final class SpaceRendererBlockModelMesher {
    /** The vanilla block atlas used by baked block-model UV coordinates. */
    public static final String BLOCK_ATLAS_MATERIAL = "minecraft:textures/atlas/blocks.png";
    private static final int WHITE = 0xFFFFFFFF;
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
                            appendQuad(builder, quad, x, y, z, packedLight);
                            quadCount++;
                        }
                        for (Direction side : Direction.values()) {
                            if (snapshot.blockState(x + side.getStepX(), y + side.getStepY(),
                                    z + side.getStepZ()).isSolidRender()) {
                                continue;
                            }
                            for (BakedQuad quad : part.getQuads(side)) {
                                appendQuad(builder, quad, x, y, z, packedLight);
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
            BakedQuad quad, int blockX, int blockY, int blockZ, int packedLight) {
        Direction face = quad.direction();
        int packedNormal = (face.getStepX() & 0xFF)
                | ((face.getStepY() & 0xFF) << 8)
                | ((face.getStepZ() & 0xFF) << 16);
        PrismTerrainVertex[] vertices = new PrismTerrainVertex[4];
        for (int i = 0; i < 4; i++) {
            var position = quad.position(i);
            long packedUv = quad.packedUV(i);
            vertices[i] = new PrismTerrainVertex(
                    blockX + position.x(), blockY + position.y(), blockZ + position.z(),
                    unpackU(packedUv), unpackV(packedUv), WHITE,
                    packedLight, NO_OVERLAY, packedNormal);
        }
        builder.addQuad(vertices[0], vertices[1], vertices[2], vertices[3]);
    }

    /** Minecraft packs U in the high 32 bits and V in the low 32 bits. */
    public static float unpackU(long packedUv) {
        return Float.intBitsToFloat((int) (packedUv >>> 32));
    }

    public static float unpackV(long packedUv) {
        return Float.intBitsToFloat((int) packedUv);
    }
}
