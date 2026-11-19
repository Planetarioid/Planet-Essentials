package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;
import java.util.Objects;

/** One owned GPU vertex allocation uploaded from an immutable Prism terrain payload. */
public final class PrismTerrainGpuMesh implements AutoCloseable {
    private final int sectionX, sectionY, sectionZ;
    private final String materialId;
    private final com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage stage;
    private final int vertexCount;
    private final long gpuBytes;
    private final GpuBuffer vertexBuffer;
    private boolean closed;

    private PrismTerrainGpuMesh(PrismTerrainVertexData data, GpuBuffer vertexBuffer) {
        sectionX=data.sectionX(); sectionY=data.sectionY(); sectionZ=data.sectionZ();
        materialId=data.materialId(); stage=data.stage(); vertexCount=data.vertexCount();
        gpuBytes=data.byteSize(); this.vertexBuffer=vertexBuffer;
    }

    /**
     * Uploads on the render thread. The temporary direct staging buffer is copied
     * into the device-owned allocation by GpuDevice.createBuffer.
     */
    public static PrismTerrainGpuMesh upload(GpuDevice device, PrismTerrainVertexData data) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(data, "data");
        if (data.layoutVersion()!=PrismTerrainVertexLayout.VERSION
                || data.strideBytes()!=PrismTerrainVertexLayout.STRIDE
                || data.byteSize()!=(long)data.vertexCount()*PrismTerrainVertexLayout.STRIDE) {
            throw new IllegalArgumentException("Unsupported or inconsistent Prism terrain vertex payload");
        }
        PrismTerrainVertexLayout.minecraftVertexFormat();
        ByteBuffer staging=ByteBuffer.allocateDirect(data.byteSize());
        staging.put(data.bytes()).flip();
        GpuBuffer buffer=device.createBuffer(
                () -> "Planet Prism terrain section " + data.sectionX()+","+data.sectionY()+","+data.sectionZ(),
                GpuBuffer.USAGE_VERTEX, staging);
        return new PrismTerrainGpuMesh(data, buffer);
    }

    public int sectionX(){return sectionX;} public int sectionY(){return sectionY;} public int sectionZ(){return sectionZ;}
    public String materialId(){return materialId;}
    public com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage stage(){return stage;}
    public int vertexCount(){return vertexCount;}
    public long gpuBytes(){return gpuBytes;}

    /** Borrowed handle. The caller must not close it or retain it past this mesh's lifetime. */
    public GpuBuffer vertexBuffer() {
        if(closed) throw new IllegalStateException("Prism terrain GPU mesh is closed");
        return vertexBuffer;
    }

    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        if(closed) return;
        closed=true;
        vertexBuffer.close();
    }
}
