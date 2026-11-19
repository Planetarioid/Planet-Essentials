package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextureMipChainTest {
    @Test
    void generatesConventionalDimensionsUntilOneByOne() {
        DecodedTexture base = new DecodedTexture(8, 4, new int[32]);
        List<DecodedTexture> levels = TextureMipChain.generate(
                base, new MipmapPolicy(MipmapPolicy.Mode.BOX_LINEAR_SRGB, 8, 1024));

        assertEquals(4, levels.size());
        assertEquals(8, levels.get(0).width());
        assertEquals(4, levels.get(0).height());
        assertEquals(4, levels.get(1).width());
        assertEquals(2, levels.get(1).height());
        assertEquals(2, levels.get(2).width());
        assertEquals(1, levels.get(2).height());
        assertEquals(1, levels.get(3).width());
        assertEquals(1, levels.get(3).height());
    }

    @Test
    void disabledPolicyReturnsOnlyOriginalLevel() {
        DecodedTexture base = new DecodedTexture(16, 16, new int[256]);
        List<DecodedTexture> levels = TextureMipChain.generate(base, MipmapPolicy.DISABLED);
        assertEquals(1, levels.size());
        assertSame(base, levels.get(0));
    }

    @Test
    void generatedByteBudgetStopsBeforeExceedingLimit() {
        DecodedTexture base = new DecodedTexture(8, 8, new int[64]);
        List<DecodedTexture> levels = TextureMipChain.generate(
                base, new MipmapPolicy(MipmapPolicy.Mode.BOX_LINEAR_SRGB, 8, 64));

        assertEquals(2, levels.size());
        assertEquals(64, levels.get(1).estimatedBytes());
    }

    @Test
    void transparentColorDoesNotBleedIntoOpaqueNeighbor() {
        // Fully transparent red beside fully opaque blue.
        DecodedTexture base = new DecodedTexture(2, 1,
                new int[]{0x00FF0000, 0xFF0000FF});
        DecodedTexture mip = TextureMipChain.generate(
                base, new MipmapPolicy(MipmapPolicy.Mode.BOX_LINEAR_SRGB, 2, 16)).get(1);

        assertEquals(0x800000FF, mip.pixel(0, 0));
    }

    @Test
    void oddDimensionsIncludeTheFinalSourceEdge() {
        DecodedTexture base = new DecodedTexture(3, 1,
                new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF});
        DecodedTexture mip = TextureMipChain.generate(
                base, new MipmapPolicy(MipmapPolicy.Mode.BOX_LINEAR_SRGB, 2, 16)).get(1);

        assertEquals(1, mip.width());
        assertEquals(0xFFBCBCBC, mip.pixel(0, 0));
    }

    @Test
    void nearestPolicySelectsAStableSourceSample() {
        DecodedTexture base = new DecodedTexture(2, 2,
                new int[]{0xFF010101, 0xFF020202, 0xFF030303, 0xFF040404});
        DecodedTexture mip = TextureMipChain.generate(
                base, new MipmapPolicy(MipmapPolicy.Mode.NEAREST, 2, 16)).get(1);

        assertEquals(0xFF040404, mip.pixel(0, 0));
    }

    @Test
    void validatesPolicyBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> new MipmapPolicy(MipmapPolicy.Mode.NONE, 2, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new MipmapPolicy(MipmapPolicy.Mode.NEAREST, 0, 100));
    }
}
