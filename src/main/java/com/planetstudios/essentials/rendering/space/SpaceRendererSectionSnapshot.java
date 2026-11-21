package com.planetstudios.essentials.rendering.space;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable block-state, packed-light, and baked-quad tint snapshot for one section plus a one-block border.
 * It contains no live level/region references and is safe to hand to CPU workers.
 */
public final class SpaceRendererSectionSnapshot {
    public static final int BORDER = 1;
    public static final int EDGE = 16 + BORDER * 2;
    public static final int STATE_COUNT = EDGE * EDGE * EDGE;

    private final SpaceRendererSectionManager.SectionKey key;
    private final BlockState[] states;
    private final int[] packedLights;
    /** Per-cell flat pairs of tint index and resolved RGB color. Null means no registered tint color. */
    private final int[][] tintColors;

    private SpaceRendererSectionSnapshot(SpaceRendererSectionManager.SectionKey key,
            BlockState[] states, int[] packedLights, int[][] tintColors) {
        this.key = Objects.requireNonNull(key, "key");
        if (states == null || states.length != STATE_COUNT) {
            throw new IllegalArgumentException("Section snapshot must contain exactly " + STATE_COUNT + " states");
        }
        if (packedLights == null || packedLights.length != STATE_COUNT) {
            throw new IllegalArgumentException("Section snapshot must contain exactly " + STATE_COUNT + " light values");
        }
        if (tintColors == null || tintColors.length != STATE_COUNT) {
            throw new IllegalArgumentException("Section snapshot must contain exactly " + STATE_COUNT + " tint entries");
        }
        this.states = states.clone();
        this.packedLights = packedLights.clone();
        this.tintColors = new int[STATE_COUNT][];
        for (int i = 0; i < STATE_COUNT; i++) {
            Objects.requireNonNull(this.states[i], "block state");
            this.tintColors[i] = tintColors[i] == null ? null : tintColors[i].clone();
            if (this.tintColors[i] != null && (this.tintColors[i].length & 1) != 0) {
                throw new IllegalArgumentException("Tint data must contain index/color pairs");
            }
        }
    }

    /** Copies block states, vanilla light, and model tint colors while the compile region is valid. */
    public static SpaceRendererSectionSnapshot capture(SectionPos section, RenderSectionRegion region,
            String dimensionId) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(region, "region");
        var key = new SpaceRendererSectionManager.SectionKey(dimensionId,
                section.x(), section.y(), section.z());
        int minX = Math.multiplyExact(section.x(), 16);
        int minY = Math.multiplyExact(section.y(), 16);
        int minZ = Math.multiplyExact(section.z(), 16);
        BlockState[] states = new BlockState[STATE_COUNT];
        int[] lights = new int[STATE_COUNT];
        int[][] tints = new int[STATE_COUNT][];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        var minecraft = Minecraft.getInstance();
        var blockRenderer = minecraft.getBlockRenderer();
        var blockColors = minecraft.getBlockColors();

        for (int y = -BORDER; y < 16 + BORDER; y++) {
            for (int z = -BORDER; z < 16 + BORDER; z++) {
                for (int x = -BORDER; x < 16 + BORDER; x++) {
                    pos.set(minX + x, minY + y, minZ + z);
                    int index = index(x, y, z);
                    BlockState state = region.getBlockState(pos);
                    states[index] = state;
                    int block = region.getBrightness(LightLayer.BLOCK, pos);
                    int sky = region.getBrightness(LightLayer.SKY, pos);
                    lights[index] = packLight(block, sky);

                    if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < 16
                            && state.isSolidRender() && state.getRenderShape() == RenderShape.MODEL) {
                        List<BlockModelPart> parts = blockRenderer.getBlockModel(state).collectParts(
                                RandomSource.create(state.getSeed(pos)));
                        Map<Integer, Integer> resolved = new HashMap<>();
                        for (BlockModelPart part : parts) {
                            captureTints(part.getQuads(null), state, region, pos, blockColors, resolved);
                            for (Direction side : Direction.values()) {
                                captureTints(part.getQuads(side), state, region, pos, blockColors, resolved);
                            }
                        }
                        if (!resolved.isEmpty()) {
                            int[] pairs = new int[resolved.size() * 2];
                            int pair = 0;
                            for (var entry : resolved.entrySet()) {
                                pairs[pair++] = entry.getKey();
                                pairs[pair++] = entry.getValue();
                            }
                            tints[index] = pairs;
                        }
                    }
                }
            }
        }
        return new SpaceRendererSectionSnapshot(key, states, lights, tints);
    }

    private static void captureTints(List<BakedQuad> quads, BlockState state, RenderSectionRegion region,
            BlockPos pos, net.minecraft.client.color.block.BlockColors blockColors,
            Map<Integer, Integer> resolved) {
        for (BakedQuad quad : quads) {
            if (!quad.hasTint() || resolved.containsKey(quad.tintIndex())) continue;
            int color = blockColors.getColor(state, region, pos, quad.tintIndex());
            if (color != -1) resolved.put(quad.tintIndex(), color);
        }
    }

    /** Test/adapter factory. The supplied arrays are defensively copied. */
    public static SpaceRendererSectionSnapshot fromStates(
            SpaceRendererSectionManager.SectionKey key, BlockState[] states) {
        int[] lights = new int[STATE_COUNT];
        Arrays.fill(lights, 0x00F000F0);
        return new SpaceRendererSectionSnapshot(key, states, lights, new int[STATE_COUNT][]);
    }

    /** Test/adapter factory with explicit packed block/sky light values. */
    public static SpaceRendererSectionSnapshot fromStates(
            SpaceRendererSectionManager.SectionKey key, BlockState[] states, int[] packedLights) {
        return new SpaceRendererSectionSnapshot(key, states, packedLights, new int[STATE_COUNT][]);
    }

    /** Test/adapter factory with explicit packed light and per-cell tint index/color pairs. */
    public static SpaceRendererSectionSnapshot fromStates(
            SpaceRendererSectionManager.SectionKey key, BlockState[] states, int[] packedLights,
            int[][] tintColors) {
        return new SpaceRendererSectionSnapshot(key, states, packedLights, tintColors);
    }

    private static int packLight(int block, int sky) {
        return (block << 4) | (sky << 20);
    }

    private static int index(int x, int y, int z) {
        if (x < -BORDER || x >= 16 + BORDER
                || y < -BORDER || y >= 16 + BORDER
                || z < -BORDER || z >= 16 + BORDER) {
            throw new IndexOutOfBoundsException("Snapshot coordinates must be within [-1, 16]");
        }
        return ((y + BORDER) * EDGE + (z + BORDER)) * EDGE + (x + BORDER);
    }

    public SpaceRendererSectionManager.SectionKey key() { return key; }
    public BlockState blockState(int localX, int localY, int localZ) {
        return states[index(localX, localY, localZ)];
    }
    public FluidState fluidState(int localX, int localY, int localZ) {
        return blockState(localX, localY, localZ).getFluidState();
    }
    public int packedLight(int localX, int localY, int localZ) {
        return packedLights[index(localX, localY, localZ)];
    }
    /** Returns -1 when the cell has no resolved color for this model tint index. */
    public int tintColor(int localX, int localY, int localZ, int tintIndex) {
        int[] pairs = tintColors[index(localX, localY, localZ)];
        if (pairs == null) return -1;
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i] == tintIndex) return pairs[i + 1];
        }
        return -1;
    }
    public int stateCount() { return states.length; }
    public long estimatedBytes() {
        long bytes = (long) states.length * (Long.BYTES + Integer.BYTES);
        for (int[] tint : tintColors) {
            if (tint != null) bytes += (long) tint.length * Integer.BYTES;
        }
        return bytes;
    }
}
