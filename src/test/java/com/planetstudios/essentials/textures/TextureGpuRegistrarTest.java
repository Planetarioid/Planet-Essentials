package com.planetstudios.essentials.textures;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextureGpuRegistrarTest {
    @Test
    void convertsArgbToAbgrWithoutChangingAlphaOrGreen() {
        assertEquals(0x7F332211, TextureGpuRegistrar.argbToAbgr(0x7F112233));
        assertEquals(0xFF000000, TextureGpuRegistrar.argbToAbgr(0xFF000000));
        assertEquals(0x00FFFFFF, TextureGpuRegistrar.argbToAbgr(0x00FFFFFF));
    }

    @Test
    void createsStableNamespacedIdentifiersWithoutReusingSourceNamespace() {
        Identifier source = Identifier.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
        Identifier first = TextureGpuRegistrar.identifierFor(source);
        Identifier second = TextureGpuRegistrar.identifierFor(source);

        assertEquals(first, second);
        assertEquals("planet_essentials", first.getNamespace());
        assertTrue(first.getPath().startsWith("decoded/"));
        assertNotEquals(source, first);
    }

    @Test
    void differentSourcesReceiveDifferentIdentifiers() {
        Identifier first = TextureGpuRegistrar.identifierFor(
                Identifier.fromNamespaceAndPath("minecraft", "textures/block/stone.png"));
        Identifier second = TextureGpuRegistrar.identifierFor(
                Identifier.fromNamespaceAndPath("minecraft", "textures/block/dirt.png"));

        assertNotEquals(first, second);
    }
}
