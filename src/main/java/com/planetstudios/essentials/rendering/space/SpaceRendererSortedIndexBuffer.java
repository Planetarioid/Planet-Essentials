package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/** One frame-owned GPU index allocation for a camera-sorted translucent terrain draw. */
public final class SpaceRendererSortedIndexBuffer implements AutoCloseable {
    private final SpaceRendererFrameSnapshot.Draw draw;
    private final GpuBuffer buffer;
    private final int indexCount;
    private final long gpuBytes;
    private boolean closed;

    private SpaceRendererSortedIndexBuffer(SpaceRendererFrameSnapshot.Draw draw, GpuBuffer buffer, int indexCount) {
        this.draw = draw;
        this.buffer = buffer;
        this.indexCount = indexCount;
        this.gpuBytes = (long) indexCount * Short.BYTES;
    }

    public static SpaceRendererSortedIndexBuffer upload(GpuDevice device,
            SpaceRendererFrameSnapshot.SortedTranslucentDraw sorted) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(sorted, "sorted");
        short[] indices = sorted.indices();
        ByteBuffer bytes = encode(indices);
        GpuBuffer buffer = device.createBuffer(
                () -> "Space Renderer sorted translucent indices "
                        + sorted.draw().input().section().x() + ","
                        + sorted.draw().input().section().y() + ","
                        + sorted.draw().input().section().z(),
                GpuBuffer.USAGE_INDEX, bytes);
        return new SpaceRendererSortedIndexBuffer(sorted.draw(), buffer, indices.length);
    }

    /** Encodes unsigned-short index bit patterns in the native little-endian GPU format. */
    static ByteBuffer encode(short[] indices) {
        Objects.requireNonNull(indices, "indices");
        if (indices.length == 0 || indices.length % 6 != 0) {
            throw new IllegalArgumentException("Sorted indices must contain complete quad triangles");
        }
        ByteBuffer bytes = ByteBuffer.allocateDirect(Math.multiplyExact(indices.length, Short.BYTES))
                .order(ByteOrder.LITTLE_ENDIAN);
        for (short index : indices) bytes.putShort(index);
        return bytes.flip();
    }

    public SpaceRendererFrameSnapshot.Draw draw() { ensureOpen(); return draw; }
    public GpuBuffer buffer() { ensureOpen(); return buffer; }
    public int indexCount() { return indexCount; }
    public long gpuBytes() { return gpuBytes; }
    public boolean isOpen() { return !closed; }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Space Renderer sorted index buffer is closed");
    }

    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        closed = true;
        buffer.close();
    }
}
