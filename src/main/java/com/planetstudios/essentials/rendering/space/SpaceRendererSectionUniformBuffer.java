package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Frame-scoped, triple-buffered GPU storage for Space Renderer section uniforms.
 *
 * <p>Each section receives an aligned 16-byte std140 slice. The ring rotates
 * only after the frame's draws have been submitted, keeping buffers in flight
 * away from writes for the next frame.</p>
 */
public final class SpaceRendererSectionUniformBuffer implements AutoCloseable {
    public static final int DEFAULT_MAX_SECTIONS_PER_FRAME = 4096;

    private final int maxSectionsPerFrame;
    private final int stride;
    private final MappableRingBuffer ring;
    private int nextSection;
    private boolean frameOpen;
    private boolean closed;

    public SpaceRendererSectionUniformBuffer() {
        this(DEFAULT_MAX_SECTIONS_PER_FRAME);
    }

    public SpaceRendererSectionUniformBuffer(int maxSectionsPerFrame) {
        RenderSystem.assertOnRenderThread();
        if (maxSectionsPerFrame <= 0) {
            throw new IllegalArgumentException("Section uniform capacity must be positive");
        }
        this.maxSectionsPerFrame = maxSectionsPerFrame;
        this.stride = alignedStride(RenderSystem.getDevice().getUniformOffsetAlignment());
        int capacityBytes = Math.multiplyExact(stride, maxSectionsPerFrame);
        this.ring = new MappableRingBuffer(
                () -> "Space Renderer section uniform ring",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                capacityBytes);
    }

    /** Starts allocation within the ring buffer selected by the previous endFrame call. */
    public void beginFrame() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (frameOpen) throw new IllegalStateException("Space Renderer uniform frame is already open");
        nextSection = 0;
        frameOpen = true;
    }

    /**
     * Writes one camera-relative section origin and returns the exact UBO slice
     * to bind as the SpaceRendererSection uniform for that section's draw.
     */
    public GpuBufferSlice upload(int sectionX, int sectionY, int sectionZ,
            double cameraX, double cameraY, double cameraZ) {
        RenderSystem.assertOnRenderThread();
        ensureFrameOpen();
        if (nextSection >= maxSectionsPerFrame) {
            throw new IllegalStateException("Space Renderer section uniform frame capacity exceeded");
        }

        int offset = Math.multiplyExact(nextSection, stride);
        ByteBuffer source = SpaceRendererSectionUniformData.encode(
                sectionX, sectionY, sectionZ, cameraX, cameraY, cameraZ);
        GpuBuffer buffer = ring.currentBuffer();
        GpuBufferSlice destination = buffer.slice(offset, SpaceRendererSectionUniformData.BYTE_SIZE);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mapped = encoder.mapBuffer(destination, false, true)) {
            MemoryUtil.memCopy(source, mapped.data());
        }
        nextSection++;
        return destination;
    }

    /** Binds one slice returned by this arena to the custom terrain pipeline. */
    public static void bind(RenderPass pass, GpuBufferSlice sectionUniform) {
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(sectionUniform, "sectionUniform");
        pass.setUniform("SpaceRendererSection", sectionUniform);
    }

    /**
     * Ends this frame and rotates to a fence-protected buffer for the next frame.
     * Call only after all render passes using this frame's slices have been submitted.
     */
    public void endFrame() {
        RenderSystem.assertOnRenderThread();
        ensureFrameOpen();
        ring.rotate();
        frameOpen = false;
    }

    public int uploadedSections() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        return nextSection;
    }

    public int maxSectionsPerFrame() { return maxSectionsPerFrame; }
    public int strideBytes() { return stride; }
    public int capacityBytes() { return Math.multiplyExact(stride, maxSectionsPerFrame); }

    static int alignedStride(int uniformOffsetAlignment) {
        if (uniformOffsetAlignment <= 0) {
            throw new IllegalArgumentException("Uniform offset alignment must be positive");
        }
        int alignment = Math.max(16, uniformOffsetAlignment);
        int remainder = SpaceRendererSectionUniformData.BYTE_SIZE % alignment;
        return remainder == 0 ? SpaceRendererSectionUniformData.BYTE_SIZE
                : Math.addExact(SpaceRendererSectionUniformData.BYTE_SIZE, alignment - remainder);
    }

    private void ensureFrameOpen() {
        ensureOpen();
        if (!frameOpen) throw new IllegalStateException("Space Renderer uniform frame is not open");
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Space Renderer section uniform buffer is closed");
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        if (frameOpen) {
            throw new IllegalStateException("Cannot close Space Renderer uniform buffer during an open frame");
        }
        closed = true;
        ring.close();
    }
}
