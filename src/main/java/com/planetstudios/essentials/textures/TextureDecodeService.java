package com.planetstudios.essentials.textures;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounded background decoder. The service never uploads GPU textures and never
 * blocks the caller waiting for a decode.
 */
public final class TextureDecodeService implements AutoCloseable {
    private static final int WORKER_COUNT = 2;
    private static final int QUEUE_CAPACITY = 64;

    @FunctionalInterface
    public interface InputStreamSource {
        InputStream open() throws IOException;
    }

    private final TextureDecodeLimits limits;
    private final TexturePixelCache cache;
    private final ThreadPoolExecutor executor;
    private volatile boolean closed;

    public TextureDecodeService(TextureDecodeLimits limits, TexturePixelCache cache) {
        this.limits = Objects.requireNonNull(limits, "limits");
        this.cache = Objects.requireNonNull(cache, "cache");
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "Planet Essentials texture decoder-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = new ThreadPoolExecutor(WORKER_COUNT, WORKER_COUNT, 0L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                factory, new ThreadPoolExecutor.AbortPolicy());
    }

    public CompletableFuture<DecodedTexture> decode(String resourceId, InputStreamSource source) {
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(source, "source");
        CompletableFuture<DecodedTexture> result = new CompletableFuture<>();
        if (closed) {
            result.completeExceptionally(new IllegalStateException("Texture decoder is closed"));
            return result;
        }

        long requestedGeneration = cache.generation();
        try {
            executor.execute(() -> {
                if (closed || requestedGeneration != cache.generation()) {
                    result.completeExceptionally(new CancellationException("Texture generation changed"));
                    return;
                }
                DecodedTexture cached = cache.get(resourceId).orElse(null);
                if (cached != null) {
                    result.complete(cached);
                    return;
                }
                try (InputStream input = source.open()) {
                    DecodedTexture decoded = PngTextureDecoder.decode(input, limits);
                    if (closed || requestedGeneration != cache.generation()) {
                        result.completeExceptionally(new CancellationException("Texture generation changed during decode"));
                        return;
                    }
                    cache.put(resourceId, decoded);
                    result.complete(decoded);
                } catch (Exception failure) {
                    result.completeExceptionally(failure);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            result.completeExceptionally(rejected);
        }
        return result;
    }

    /**
     * Invalidates stale work on reload. Queued tasks are deliberately retained:
     * each must run its generation check and complete its future as cancelled.
     */
    public long onResourceReload() {
        return cache.invalidateForReload();
    }

    public int queuedTasks() { return executor.getQueue().size(); }
    public int activeTasks() { return executor.getActiveCount(); }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
        cache.clear();
    }
}
