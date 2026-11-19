package com.planetstudios.essentials.textures;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;

/** Bounded PNG-to-CPU-pixels decoder. Call from a worker, never the render thread. */
public final class PngTextureDecoder {
    private static final int HEADER_LENGTH = 33;
    private static final int BUFFER_SIZE = 8 * 1024;

    private PngTextureDecoder() {}

    public static DecodedTexture decode(InputStream source, TextureDecodeLimits limits) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(limits, "limits");
        if (limits.maxEncodedBytes() > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("Encoded byte limit is too large for an in-memory PNG decoder");
        }

        byte[] prefix = source.readNBytes(HEADER_LENGTH);
        if (prefix.length != HEADER_LENGTH) {
            throw new IOException("Truncated PNG header");
        }
        PngHeaderValidator.Header header = PngHeaderValidator.readHeader(
                new ByteArrayInputStream(prefix), limits);

        ByteArrayOutputStream encoded = new ByteArrayOutputStream(
                Math.min((int) limits.maxEncodedBytes(), 64 * 1024));
        encoded.write(prefix);
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = prefix.length;
        while (true) {
            int read = source.read(buffer, 0, (int) Math.min(buffer.length,
                    limits.maxEncodedBytes() - total + 1L));
            if (read < 0) break;
            if (read == 0) {
                int single = source.read();
                if (single < 0) break;
                if (++total > limits.maxEncodedBytes()) {
                    throw new IOException("Encoded PNG exceeds configured byte limit");
                }
                encoded.write(single);
                continue;
            }
            total += read;
            if (total > limits.maxEncodedBytes()) {
                throw new IOException("Encoded PNG exceeds configured byte limit");
            }
            encoded.write(buffer, 0, read);
        }
        limits.validateEncodedLength(total);

        try (MemoryCacheImageInputStream imageInput =
                     new MemoryCacheImageInputStream(new ByteArrayInputStream(encoded.toByteArray()))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            ImageReader reader = null;
            while (readers.hasNext()) {
                ImageReader candidate = readers.next();
                if ("png".equalsIgnoreCase(candidate.getFormatName())) {
                    reader = candidate;
                    break;
                }
                candidate.dispose();
            }
            if (reader == null) {
                throw new IOException("No PNG decoder is available");
            }
            try {
                reader.setInput(imageInput, true, true);
                int decodedWidth = reader.getWidth(0);
                int decodedHeight = reader.getHeight(0);
                limits.validateDimensions(decodedWidth, decodedHeight);
                if (decodedWidth != header.width() || decodedHeight != header.height()) {
                    throw new IOException("Decoded dimensions do not match the validated PNG header");
                }
                BufferedImage image = reader.read(0);
                if (image == null || image.getWidth() != header.width()
                        || image.getHeight() != header.height()) {
                    throw new IOException("PNG decoder returned inconsistent image data");
                }
                int[] pixels = image.getRGB(0, 0, header.width(), header.height(),
                        null, 0, header.width());
                return new DecodedTexture(header.width(), header.height(), pixels);
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("PNG decode rejected by configured limits", exception);
        }
    }
}
