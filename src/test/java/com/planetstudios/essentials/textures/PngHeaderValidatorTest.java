package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class PngHeaderValidatorTest {
    @Test
    void readsValidRgbaHeaderWithoutConsumingFollowingData() throws Exception {
        byte[] png = pngHeader(128, 64, 8, 6, 0, 0, 0);
        ByteArrayInputStream input = new ByteArrayInputStream(concat(png, new byte[]{42, 43}));
        PngHeaderValidator.Header header = PngHeaderValidator.readHeader(input);

        assertEquals(128, header.width());
        assertEquals(64, header.height());
        assertEquals(8, header.bitDepth());
        assertEquals(6, header.colorType());
        assertEquals(33, header.bytesRead());
        assertEquals(42, input.read());
    }

    @Test
    void rejectsBadSignatureAndTruncatedHeader() {
        assertThrows(IOException.class, () -> PngHeaderValidator.readHeader(
                new ByteArrayInputStream(new byte[40])));
        assertThrows(IOException.class, () -> PngHeaderValidator.readHeader(
                new ByteArrayInputStream(new byte[]{(byte) 0x89, 0x50})));
    }

    @Test
    void rejectsInvalidIhdrCrc() throws Exception {
        byte[] png = pngHeader(16, 16, 8, 6, 0, 0, 0);
        png[png.length - 1] ^= 1;
        assertThrows(IOException.class,
                () -> PngHeaderValidator.readHeader(new ByteArrayInputStream(png)));
    }

    @Test
    void rejectsDimensionsAboveConfiguredLimits() throws Exception {
        byte[] png = pngHeader(4096, 4096, 8, 6, 0, 0, 0);
        TextureDecodeLimits limits = new TextureDecodeLimits(8192, 1_000_000, 1024);
        assertThrows(IOException.class, () -> PngHeaderValidator.readHeader(
                new ByteArrayInputStream(png), limits));
    }

    @Test
    void rejectsInvalidBitDepthAndMethods() throws Exception {
        assertThrows(IOException.class, () -> PngHeaderValidator.readHeader(
                new ByteArrayInputStream(pngHeader(8, 8, 4, 6, 0, 0, 0))));
        assertThrows(IOException.class, () -> PngHeaderValidator.readHeader(
                new ByteArrayInputStream(pngHeader(8, 8, 8, 6, 1, 0, 0))));
        assertThrows(IOException.class, () -> PngHeaderValidator.readHeader(
                new ByteArrayInputStream(pngHeader(8, 8, 8, 6, 0, 0, 2))));
    }

    @Test
    void decodeLimitsValidateEncodedLengthAndPixelCount() {
        TextureDecodeLimits limits = new TextureDecodeLimits(1024, 500_000, 4096);
        assertDoesNotThrow(() -> limits.validateDimensions(1000, 500));
        assertThrows(IllegalArgumentException.class, () -> limits.validateDimensions(1000, 501));
        assertDoesNotThrow(() -> limits.validateEncodedLength(4096));
        assertThrows(IllegalArgumentException.class, () -> limits.validateEncodedLength(4097));
    }

    private static byte[] pngHeader(int width, int height, int bitDepth, int colorType,
                                    int compression, int filter, int interlace) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        output.write(signature);
        output.writeInt(13);
        byte[] type = {'I', 'H', 'D', 'R'};
        output.write(type);
        ByteArrayOutputStream dataBytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(dataBytes);
        data.writeInt(width);
        data.writeInt(height);
        data.writeByte(bitDepth);
        data.writeByte(colorType);
        data.writeByte(compression);
        data.writeByte(filter);
        data.writeByte(interlace);
        byte[] ihdr = dataBytes.toByteArray();
        output.write(ihdr);
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(ihdr);
        output.writeInt((int) crc.getValue());
        output.flush();
        return bytes.toByteArray();
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
