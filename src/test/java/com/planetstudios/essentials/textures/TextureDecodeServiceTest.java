package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
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
        try (TextureDecodeService service = new TextureDecodeService(TextureDecodeLimits.DEFAULT, cache)) {
            service.decode("minecraft:textures/test.png",
                    () -> new ByteArrayInputStream(png())).get(5, TimeUnit.SECONDS);
            assertEquals(1, cache.size());
            service.onResourceReload();
            assertEquals(0, cache.size());
            assertEquals(1, cache.generation());
        }
    }
}
