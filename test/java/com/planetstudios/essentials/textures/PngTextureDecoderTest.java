package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PngTextureDecoderTest {
    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF123456);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }

    @Test
    void decodesPixelsAndPreservesDimensions() throws Exception {
        DecodedTexture decoded = PngTextureDecoder.decode(
                new ByteArrayInputStream(png(8, 4)), TextureDecodeLimits.DEFAULT);
        assertEquals(8, decoded.width());
        assertEquals(4, decoded.height());
        assertEquals(0xFF123456, decoded.pixel(0, 0));
        assertEquals(32, decoded.copyPixels().length);
    }

    @Test
    void rejectsEncodedStreamOverConfiguredLimit() throws Exception {
        byte[] bytes = png(8, 8);
        TextureDecodeLimits limits = new TextureDecodeLimits(1024, 1_000_000, 40);
        assertThrows(IOException.class, () -> PngTextureDecoder.decode(
                new ByteArrayInputStream(bytes), limits));
    }

    @Test
    void rejectsTruncatedAndNonPngData() {
        assertThrows(IOException.class, () -> PngTextureDecoder.decode(
                new ByteArrayInputStream(new byte[12]), TextureDecodeLimits.DEFAULT));
        assertThrows(IOException.class, () -> PngTextureDecoder.decode(
                new ByteArrayInputStream("not a png".getBytes()), TextureDecodeLimits.DEFAULT));
    }

    @Test
    void refusesMutationOfCachedPixelStorage() throws Exception {
        DecodedTexture decoded = PngTextureDecoder.decode(
                new ByteArrayInputStream(png(2, 2)), TextureDecodeLimits.DEFAULT);
        int[] copy = decoded.copyPixels();
        copy[0] = 0;
        assertNotEquals(0, decoded.pixel(0, 0));
    }
}
