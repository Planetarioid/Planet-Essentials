package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Renderer-owned GPU allocation for one terrain section's camera-relative origin.
 *
 * <p>The allocation is immutable after creation. This avoids overwriting a uniform
 * buffer that may still be referenced by queued GPU work. The renderer owns this
 * object and should retain it until the frame's draw submissions have completed,
 * then close it on the render thread.</p>
 */
public final class PrismTerrainSectionUniformAllocation implements AutoCloseable {
    private final GpuBuffer buffer;
    private boolean closed;

    private PrismTerrainSectionUniformAllocation(GpuBuffer buffer) {
        this.buffer = buffer;
    }

    /** Creates and initializes a 16-byte uniform buffer from the canonical payload. */
    public static PrismTerrainSectionUniformAllocation upload(
            GpuDevice device, PrismTerrainCameraTransform transform) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(device, "device");
        ByteBuffer payload = PrismTerrainSectionUniform.encode(transform);
        GpuBuffer buffer = device.createBuffer(
                () -> "Planet Prism terrain section-origin uniform",
                GpuBuffer.USAGE_UNIFORM,
                payload);
        return new PrismTerrainSectionUniformAllocation(buffer);
    }

    /** Borrowed immutable slice. It is valid only while this allocation remains open. */
    public GpuBufferSlice slice() {
        ensureOpen();
        return buffer.slice(0L, PrismTerrainSectionUniform.BYTE_SIZE);
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Prism terrain section uniform is closed");
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        closed = true;
        buffer.close();
    }
}
