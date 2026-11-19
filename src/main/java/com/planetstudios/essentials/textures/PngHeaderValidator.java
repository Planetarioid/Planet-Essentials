package com.planetstudios.essentials.textures;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * Reads and validates only the fixed PNG signature and IHDR chunk.
 * It does not decode pixels or consume the remainder of the resource stream.
 */
public final class PngHeaderValidator {
    private static final byte[] SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final int IHDR_LENGTH = 13;
    private static final int HEADER_BYTES = 8 + 4 + 4 + IHDR_LENGTH + 4;

    private PngHeaderValidator() {
    }

    public static Header readHeader(InputStream input) throws IOException {
        return readHeader(input, TextureDecodeLimits.DEFAULT);
    }

    public static Header readHeader(InputStream input, TextureDecodeLimits limits) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(limits, "limits");

        DataInputStream data = new DataInputStream(input);
        byte[] signature = new byte[SIGNATURE.length];
        data.readFully(signature);
        if (!java.util.Arrays.equals(signature, SIGNATURE)) {
            throw new IOException("Invalid PNG signature");
        }

        int chunkLength = data.readInt();
        if (chunkLength != IHDR_LENGTH) {
            throw new IOException("PNG must begin with a 13-byte IHDR chunk");
        }

        byte[] chunkType = new byte[4];
        data.readFully(chunkType);
        if (chunkType[0] != 'I' || chunkType[1] != 'H'
                || chunkType[2] != 'D' || chunkType[3] != 'R') {
            throw new IOException("PNG is missing its leading IHDR chunk");
        }

        byte[] ihdr = new byte[IHDR_LENGTH];
        data.readFully(ihdr);
        long expectedCrc = Integer.toUnsignedLong(data.readInt());

        CRC32 crc = new CRC32();
        crc.update(chunkType);
        crc.update(ihdr);
        if (crc.getValue() != expectedCrc) {
            throw new IOException("PNG IHDR checksum mismatch");
        }

        long width = Integer.toUnsignedLong(readInt(ihdr, 0));
        long height = Integer.toUnsignedLong(readInt(ihdr, 4));
        int bitDepth = Byte.toUnsignedInt(ihdr[8]);
        int colorType = Byte.toUnsignedInt(ihdr[9]);
        int compression = Byte.toUnsignedInt(ihdr[10]);
        int filter = Byte.toUnsignedInt(ihdr[11]);
        int interlace = Byte.toUnsignedInt(ihdr[12]);

        try {
            limits.validateDimensions(width, height);
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }

        if (!isValidBitDepth(colorType, bitDepth)) {
            throw new IOException("Unsupported PNG color type and bit depth combination");
        }
        if (compression != 0 || filter != 0 || (interlace != 0 && interlace != 1)) {
            throw new IOException("Invalid PNG compression, filter, or interlace method");
        }

        return new Header((int) width, (int) height, bitDepth, colorType, interlace,
                HEADER_BYTES);
    }

    private static int readInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24
                | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8
                | bytes[offset + 3] & 0xFF;
    }

    private static boolean isValidBitDepth(int colorType, int bitDepth) {
        return switch (colorType) {
            case 0 -> bitDepth == 1 || bitDepth == 2 || bitDepth == 4 || bitDepth == 8 || bitDepth == 16;
            case 2 -> bitDepth == 8 || bitDepth == 16;
            case 3 -> bitDepth == 1 || bitDepth == 2 || bitDepth == 4 || bitDepth == 8;
            case 4, 6 -> bitDepth == 8 || bitDepth == 16;
            default -> false;
        };
    }

    public record Header(
            int width,
            int height,
            int bitDepth,
            int colorType,
            int interlaceMethod,
            int bytesRead
    ) {
        public Header {
            if (width < 1 || height < 1 || bytesRead != HEADER_BYTES) {
                throw new IllegalArgumentException("Invalid PNG header metadata");
            }
        }
    }
}
