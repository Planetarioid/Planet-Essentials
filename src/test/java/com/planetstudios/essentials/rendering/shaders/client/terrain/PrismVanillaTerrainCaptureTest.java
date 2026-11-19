package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.SectionPos;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class PrismVanillaTerrainCaptureTest {
    @Test
    void convertsVanillaBlockVerticesIntoPrismV1AttributeOrder() {
        ByteBuffer vanilla = ByteBuffer.allocate(4 * 32).order(ByteOrder.nativeOrder());
        for (int i = 0; i < 4; i++) {
            int base = i * 32;
            vanilla.putFloat(base, i == 1 || i == 2 ? 1.0f : 0.0f);
            vanilla.putFloat(base + 4, i >= 2 ? 1.0f : 0.0f);
            vanilla.putFloat(base + 8, 0.0f);
            vanilla.putInt(base + 12, 0xAABBCCDD);
            vanilla.putFloat(base + 16, i == 1 || i == 2 ? 1.0f : 0.0f);
            vanilla.putFloat(base + 20, i >= 2 ? 1.0f : 0.0f);
            vanilla.putInt(base + 24, 0x00100020);
            vanilla.putInt(base + 28, 0x007F0000);
        }
        vanilla.position(0);
        var payload = PrismVanillaTerrainCapture.convert(SectionPos.of(2, 3, 4),
                ChunkSectionLayer.SOLID, vanilla, DefaultVertexFormat.BLOCK, 4);

        assertEquals(2, payload.sectionX());
        assertEquals(3, payload.sectionY());
        assertEquals(4, payload.sectionZ());
        assertEquals("planet_essentials:terrain/vanilla/solid", payload.materialId());
        assertEquals(PrismTerrainVertexLayout.STRIDE, payload.strideBytes());
        assertEquals(144, payload.byteSize());

        var prism = payload.readOnlyBuffer();
        assertEquals(0.0f, prism.getFloat(0));
        assertEquals(0.0f, prism.getFloat(4));
        assertEquals(0.0f, prism.getFloat(8));
        assertEquals(0.0f, prism.getFloat(12));
        assertEquals(0xAABBCCDD, prism.getInt(20));
        assertEquals(0x00100020, prism.getInt(24));
        assertEquals(0, prism.getInt(28));
        assertEquals(0x007F0000, prism.getInt(32));
    }

    @Test
    void routesTranslucentLayersToTranslucentStage() {
        ByteBuffer vanilla = ByteBuffer.allocate(4 * 32).order(ByteOrder.nativeOrder());
        for (int i = 0; i < 4; i++) {
            int base = i * 32;
            vanilla.putFloat(base, 0.0f);
            vanilla.putFloat(base + 4, 0.0f);
            vanilla.putFloat(base + 8, 0.0f);
            vanilla.putInt(base + 12, -1);
            vanilla.putFloat(base + 16, 0.0f);
            vanilla.putFloat(base + 20, 0.0f);
            vanilla.putInt(base + 24, 0);
            vanilla.putInt(base + 28, 0);
        }
        var payload = PrismVanillaTerrainCapture.convert(SectionPos.of(0, 0, 0),
                ChunkSectionLayer.TRANSLUCENT, vanilla, DefaultVertexFormat.BLOCK, 4);
        assertEquals(com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage.BEFORE_TRANSLUCENT,
                payload.stage());
    }

    @Test
    void rejectsUnknownFormatsAndNonQuadVertexCounts() {
        ByteBuffer bytes = ByteBuffer.allocate(128);
        assertThrows(IllegalArgumentException.class, () -> PrismVanillaTerrainCapture.convert(
                SectionPos.of(0, 0, 0), ChunkSectionLayer.SOLID, bytes,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION, 4));
        assertThrows(IllegalArgumentException.class, () -> PrismVanillaTerrainCapture.convert(
                SectionPos.of(0, 0, 0), ChunkSectionLayer.SOLID, bytes,
                DefaultVertexFormat.BLOCK, 3));
    }
}
