package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextureAssetCatalogTest {
    private static TextureAssetDescriptor asset(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        String namespace = id.substring(0, id.indexOf(':'));
        return new TextureAssetDescriptor(id, namespace, "textures/" + path + ".png",
                TextureAssetDescriptor.TextureFormat.PNG);
    }

    @Test
    void startsWithEmptyImmutableSnapshot() {
        TextureAssetCatalog catalog = new TextureAssetCatalog();
        assertEquals(0, catalog.snapshot().generation());
        assertTrue(catalog.snapshot().assets().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.snapshot().assets().add(asset("minecraft:stone")));
    }

    @Test
    void replacementIsSortedDeduplicatedAndAdvancesGeneration() {
        TextureAssetCatalog catalog = new TextureAssetCatalog();
        TextureAssetCatalog.Snapshot snapshot = catalog.replace(
                List.of(asset("zeta:second"), asset("alpha:first"), asset("zeta:second")), false);
        assertEquals(1, snapshot.generation());
        assertEquals(List.of("alpha:first", "zeta:second"),
                snapshot.assets().stream().map(TextureAssetDescriptor::resourceId).toList());
        assertFalse(snapshot.truncated());
    }

    @Test
    void catalogEnforcesConfiguredBound() {
        TextureAssetCatalog catalog = new TextureAssetCatalog(2);
        TextureAssetCatalog.Snapshot snapshot = catalog.replace(
                List.of(asset("a:first"), asset("b:second"), asset("c:third")), false);
        assertEquals(2, snapshot.size());
        assertTrue(snapshot.truncated());
        assertEquals(List.of("a:first", "b:second"),
                snapshot.assets().stream().map(TextureAssetDescriptor::resourceId).toList());
    }

    @Test
    void sourceTruncationIsPreserved() {
        TextureAssetCatalog catalog = new TextureAssetCatalog(10);
        TextureAssetCatalog.Snapshot snapshot = catalog.replace(List.of(asset("minecraft:stone")), true);
        assertTrue(snapshot.truncated());
        assertEquals(1, snapshot.size());
    }

    @Test
    void clearDropsMetadataAndAdvancesGeneration() {
        TextureAssetCatalog catalog = new TextureAssetCatalog();
        catalog.replace(List.of(asset("minecraft:stone")), false);
        TextureAssetCatalog.Snapshot cleared = catalog.clear();
        assertEquals(2, cleared.generation());
        assertTrue(cleared.assets().isEmpty());
        assertFalse(cleared.truncated());
    }

    @Test
    void validatesNamespaceAndResourceIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new TextureAssetDescriptor(
                "Minecraft:stone", "Minecraft", "textures/stone.png",
                TextureAssetDescriptor.TextureFormat.PNG));
        assertThrows(IllegalArgumentException.class, () -> new TextureAssetDescriptor(
                "minecraft:other", "minecraft", "textures/stone.png",
                TextureAssetDescriptor.TextureFormat.PNG));
    }

    @Test
    void rejectsTraversalMalformedPathsAndUnsupportedFormats() {
        assertThrows(IllegalArgumentException.class, () -> new TextureAssetDescriptor(
                "minecraft:../outside", "minecraft", "textures/../outside.png",
                TextureAssetDescriptor.TextureFormat.PNG));
        assertThrows(IllegalArgumentException.class, () -> new TextureAssetDescriptor(
                "minecraft:bad//path", "minecraft", "textures/bad//path.png",
                TextureAssetDescriptor.TextureFormat.PNG));
        assertThrows(IllegalArgumentException.class, () -> new TextureAssetDescriptor(
                "minecraft:stone.jpg", "minecraft", "textures/stone.jpg",
                TextureAssetDescriptor.TextureFormat.PNG));
    }

    @Test
    void classifiesCommonTextureDirectories() {
        assertEquals(TextureAssetDescriptor.TextureCategory.GUI,
                asset("minecraft:gui/widgets").category());
        assertEquals(TextureAssetDescriptor.TextureCategory.BLOCK,
                asset("minecraft:block/stone").category());
        assertEquals(TextureAssetDescriptor.TextureCategory.ENTITY,
                asset("minecraft:entity/creeper/creeper").category());
        assertEquals(TextureAssetDescriptor.TextureCategory.OTHER,
                asset("minecraft:custom").category());
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new TextureAssetCatalog(0));
    }
}
