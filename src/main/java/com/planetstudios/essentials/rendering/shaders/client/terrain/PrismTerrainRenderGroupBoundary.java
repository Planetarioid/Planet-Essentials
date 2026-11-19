package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Observes the synchronous vanilla terrain render-group call boundary.
 * This captures invocation identity only, not the complete GPU pass state.
 */
public final class PrismTerrainRenderGroupBoundary {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();

    private PrismTerrainRenderGroupBoundary() {}

    public static void begin(ChunkSectionLayerGroup group, GpuSampler sampler) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(sampler, "sampler");
        CURRENT.set(new Snapshot(group, sampler, SEQUENCE.incrementAndGet()));
    }

    public static void end() {
        CURRENT.remove();
    }

    /** Present only while vanilla is executing renderGroup on the current thread. */
    public static Snapshot current() {
        return CURRENT.get();
    }

    public record Snapshot(ChunkSectionLayerGroup group, GpuSampler sampler, long sequence) {
        public Snapshot {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(sampler, "sampler");
            if (sequence <= 0) throw new IllegalArgumentException("Invalid render-group sequence");
        }
    }
}
