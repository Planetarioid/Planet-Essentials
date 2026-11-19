package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.systems.GpuDevice;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.Objects;

/**
 * Connects vanilla draw descriptors to the current CPU payload and live GPU
 * allocation. Must be called on the render thread because the GPU cache is
 * render-thread confined.
 */
public final class PrismTerrainReplacementReadiness {
    private PrismTerrainReplacementReadiness() {}

    public static PrismFrameStage stageFor(ChunkSectionLayer layer) {
        Objects.requireNonNull(layer, "layer");
        return layer == ChunkSectionLayer.TRANSLUCENT || layer == ChunkSectionLayer.TRIPWIRE
                ? PrismFrameStage.BEFORE_TRANSLUCENT : PrismFrameStage.BEFORE_ENTITIES;
    }

    public static String materialFor(ChunkSectionLayer layer) {
        Objects.requireNonNull(layer, "layer");
        return "planet_essentials:terrain/vanilla/" + layer.label();
    }

    /**
     * True only when the exact captured payload still exists and the GPU cache
     * contains an allocation uploaded from that payload's content hash.
     */
    public static boolean isReady(PrismTerrainVanillaDrawIndex.DrawDescriptor draw,
            PrismTerrainSectionCache cpuCache, PrismTerrainGpuCache gpuCache) {
        Objects.requireNonNull(draw, "draw");
        Objects.requireNonNull(cpuCache, "cpuCache");
        Objects.requireNonNull(gpuCache, "gpuCache");
        PrismFrameStage stage = stageFor(draw.layer());
        String material = materialFor(draw.layer());
        PrismTerrainVertexData payload = cpuCache.get(draw.sectionX(), draw.sectionY(),
                draw.sectionZ(), material, stage);
        if (payload == null) return false;
        PrismTerrainGpuMesh mesh = gpuCache.get(draw.sectionX(), draw.sectionY(),
                draw.sectionZ(), material, stage);
        return mesh != null && mesh.matches(payload)
                && mesh.vertexCount() == payload.vertexCount()
                && mesh.gpuBytes() == payload.byteSize();
    }
}
