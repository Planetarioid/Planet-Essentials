package com.planetstudios.essentials.rendering.space;

import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Objects;

/**
 * Immutable block-state snapshot for one section plus a one-block neighbor border.
 * It contains no live level/region references and is safe to hand to CPU workers.
 */
public final class SpaceRendererSectionSnapshot {
    public static final int BORDER = 1;
    public static final int EDGE = 16 + BORDER * 2;
    public static final int STATE_COUNT = EDGE * EDGE * EDGE;

    private final SpaceRendererSectionManager.SectionKey key;
    private final BlockState[] states;

    private SpaceRendererSectionSnapshot(SpaceRendererSectionManager.SectionKey key, BlockState[] states) {
        this.key = Objects.requireNonNull(key, "key");
        if (states == null || states.length != STATE_COUNT) {
            throw new IllegalArgumentException("Section snapshot must contain exactly " + STATE_COUNT + " states");
        }
        this.states = states.clone();
        for (BlockState state : this.states) Objects.requireNonNull(state, "block state");
    }

    /** Copies the 16-cube and its one-block border from Minecraft's compile-region snapshot. */
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
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = -BORDER; y < 16 + BORDER; y++) {
            for (int z = -BORDER; z < 16 + BORDER; z++) {
                for (int x = -BORDER; x < 16 + BORDER; x++) {
                    pos.set(minX + x, minY + y, minZ + z);
                    states[index(x, y, z)] = region.getBlockState(pos);
                }
            }
        }
        return new SpaceRendererSectionSnapshot(key, states);
    }

    /** Test/adapter factory. The supplied array is defensively copied. */
    public static SpaceRendererSectionSnapshot fromStates(
            SpaceRendererSectionManager.SectionKey key, BlockState[] states) {
        return new SpaceRendererSectionSnapshot(key, states);
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
    public int stateCount() { return states.length; }
    public long estimatedBytes() { return (long) states.length * Long.BYTES; }
}
