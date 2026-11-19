package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Reusable four-vertex clip-space quad for post-processing pipelines using POSITION. */
public final class PrismFullscreenQuad implements AutoCloseable {
    private final GpuBuffer vertices;
    private boolean closed;

    private PrismFullscreenQuad(GpuBuffer vertices) {
        this.vertices = vertices;
    }

    public static PrismFullscreenQuad create(GpuDevice device) {
        RenderSystem.assertOnRenderThread();
        ByteBuffer data = ByteBuffer.allocateDirect(4 * 3 * Float.BYTES).order(ByteOrder.nativeOrder());
        data.putFloat(-1.0f).putFloat(-1.0f).putFloat(0.0f);
        data.putFloat( 1.0f).putFloat(-1.0f).putFloat(0.0f);
        data.putFloat( 1.0f).putFloat( 1.0f).putFloat(0.0f);
        data.putFloat(-1.0f).putFloat( 1.0f).putFloat(0.0f);
        data.flip();
        return new PrismFullscreenQuad(device.createBuffer(
                () -> "Planet Prism fullscreen quad", GpuBuffer.USAGE_VERTEX, data));
    }

    public GpuBuffer buffer() {
        if (closed) throw new IllegalStateException("Fullscreen quad is closed");
        return vertices;
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        closed = true;
        vertices.close();
    }
}
