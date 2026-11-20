package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.RenderTargetSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Render-thread-owned target pool with transactional resize and deterministic cleanup. */
public final class PrismRenderTargetManager implements AutoCloseable {
    private final Map<String, PrismRenderTarget> targets = new HashMap<>();
    private boolean closed;

    public PrismRenderTarget ensureTarget(
            GpuDevice device, String id, int width, int height, boolean depthAttachment) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        Objects.requireNonNull(id, "id");
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || id.contains("..") || id.contains("//")) {
            throw new IllegalArgumentException("Invalid Prism render-target id");
        }
        RenderTargetSpec requested = new RenderTargetSpec(width, height, depthAttachment);
        PrismRenderTarget existing = targets.get(id);
        if (existing != null && existing.spec().equals(requested)) return existing;

        PrismRenderTarget replacement = PrismRenderTarget.create(
                device, "prism-" + id.replace(':', '-'), requested);
        PrismRenderTarget previous = targets.put(id, replacement);
        if (previous != null) previous.close();
        return replacement;
    }

    public PrismRenderTarget get(String id) {
        ensureOpen();
        return targets.get(id);
    }

    public void release(String id) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        PrismRenderTarget removed = targets.remove(id);
        if (removed != null) removed.close();
    }

    public int size() { return targets.size(); }

    public void clear() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        for (PrismRenderTarget target : targets.values()) {
            try { target.close(); } catch (RuntimeException ignored) {}
        }
        targets.clear();
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Prism render-target manager is closed");
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        clear();
        closed = true;
    }
}
