package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TextureDecodeServiceTest {
    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    @Test
    void decodesAsynchronouslyAndCachesByResourceId() throws Exception {
        TexturePixelCache cache = new TexturePixelCache(1024);
        try (TextureDecodeService service = new TextureDecodeService(TextureDecodeLimits.DEFAULT, cache)) {
            byte[] data = png();
            AtomicInteger opens = new AtomicInteger();
            var first = service.decode("minecraft:textures/test.png", () -> {
                opens.incrementAndGet();
                return new ByteArrayInputStream(data);
            }).get(5, TimeUnit.SECONDS);
            var second = service.decode("minecraft:textures/test.png", () -> {
                opens.incrementAndGet();
                return new ByteArrayInputStream(data);
            }).get(5, TimeUnit.SECONDS);
            assertSame(first, second);
            assertEquals(1, opens.get());
        }
    }

    @Test
    void resourceReloadInvalidatesCachedPixels() throws Exception {
        TexturePixelCache cache = new TexturePixelCache(1024);
        byte[] data = png();
        try (TextureDecodeService service = new TextureDecodeService(TextureDecodeLimits.DEFAULT, cache)) {
            service.decode("minecraft:textures/test.png",
                    () -> new ByteArrayInputStream(data)).get(5, TimeUnit.SECONDS);
            assertEquals(1, cache.size());
            service.onResourceReload();
            assertEquals(0, cache.size());
            assertEquals(1, cache.generation());
        }
    }
    @Test
    void shutdownCompletesQueuedDecodeFuturesInsteadOfLeavingThemPending() throws Exception {
        TexturePixelCache cache = new TexturePixelCache(1024);
        TextureDecodeService service = new TextureDecodeService(TextureDecodeLimits.DEFAULT, cache);
        CountDownLatch workersStarted = new CountDownLatch(2);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        try {
            TextureDecodeService.InputStreamSource blocker = () -> {
                workersStarted.countDown();
                try {
                    if (!releaseWorkers.await(5, TimeUnit.SECONDS)) {
                        throw new java.io.IOException("Timed out waiting for test release");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("Interrupted", interrupted);
                }
                return new ByteArrayInputStream(new byte[0]);
            };

            var first = service.decode("test:first.png", blocker);
            var second = service.decode("test:second.png", blocker);
            assertTrue(workersStarted.await(5, TimeUnit.SECONDS));

            var queued = service.decode("test:queued.png", () -> new ByteArrayInputStream(new byte[0]));
            service.close();

            assertThrows(CancellationException.class, queued::join);
            assertTrue(first.isCompletedExceptionally());
            assertTrue(second.isCompletedExceptionally());
        } finally {
            releaseWorkers.countDown();
            service.close();
        }
    }

}
