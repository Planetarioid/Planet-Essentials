package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Observes vanilla terrain render-group inputs at the synchronous call boundary.
 * It does not change or suppress any vanilla draw.
 */
public final class PrismTerrainRenderGroupBoundary {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();

    private PrismTerrainRenderGroupBoundary() {}

    public static void begin(ChunkSectionsToRender input, ChunkSectionLayerGroup group, GpuSampler sampler) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(sampler, "sampler");

        EnumMap<ChunkSectionLayer, List<?>> draws = new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : group.layers()) {
            List<?> layerDraws = input.drawsPerLayer().get(layer);
            draws.put(layer, layerDraws == null ? List.of() : List.copyOf(layerDraws));
        }

        GpuBufferSlice[] sectionInfos = input.chunkSectionInfos();
        PrismTerrainVanillaPassSnapshot pass = PrismTerrainVanillaPassSnapshot.capture(input, group, sampler);
        CURRENT.set(new Snapshot(group, sampler, input.textureView(), draws,
                input.maxIndicesRequired(), sectionInfos, pass, SEQUENCE.incrementAndGet()));
    }

    public static void end() {
        CURRENT.remove();
    }

    /** Present only while vanilla is executing renderGroup on the current thread. */
    public static Snapshot current() {
        return CURRENT.get();
    }

    public record Snapshot(
            ChunkSectionLayerGroup group,
            GpuSampler sampler,
            GpuTextureView textureView,
            Map<ChunkSectionLayer, List<?>> drawsByLayer,
            int maxIndicesRequired,
            GpuBufferSlice[] chunkSectionInfos,
            PrismTerrainVanillaPassSnapshot passInputs,
            long sequence) {
        public Snapshot {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(sampler, "sampler");
            Objects.requireNonNull(textureView, "textureView");
            Objects.requireNonNull(drawsByLayer, "drawsByLayer");
            Objects.requireNonNull(chunkSectionInfos, "chunkSectionInfos");
            Objects.requireNonNull(passInputs, "passInputs");
            if (maxIndicesRequired < 0) throw new IllegalArgumentException("Negative maximum index count");
            if (sequence <= 0) throw new IllegalArgumentException("Invalid render-group sequence");

            EnumMap<ChunkSectionLayer, List<?>> copy = new EnumMap<>(ChunkSectionLayer.class);
            drawsByLayer.forEach((layer, draws) ->
                    copy.put(Objects.requireNonNull(layer, "layer"), List.copyOf(draws)));
            drawsByLayer = Collections.unmodifiableMap(copy);
            chunkSectionInfos = chunkSectionInfos.clone();
        }

        @Override
        public GpuBufferSlice[] chunkSectionInfos() {
            return chunkSectionInfos.clone();
        }

        public int drawCount(ChunkSectionLayer layer) {
            List<?> draws = drawsByLayer.get(layer);
            return draws == null ? 0 : draws.size();
        }

        public int totalGroupDrawCount() {
            int count = 0;
            for (ChunkSectionLayer layer : group.layers()) count += drawCount(layer);
            return count;
        }
    }
}
