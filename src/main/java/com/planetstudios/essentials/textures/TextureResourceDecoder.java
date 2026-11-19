package com.planetstudios.essentials.textures;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Connects the bounded CPU decoder to Minecraft's active resource manager.
 * ResourceManager.open resolves the effective resource-pack winner and returns
 * a stream whose close releases its underlying resource handle.
 */
public final class TextureResourceDecoder {
    private TextureResourceDecoder() {}

    public static CompletableFuture<DecodedTexture> decode(
            ResourceManager resourceManager,
            Identifier resourceId,
            TextureDecodeService decodeService) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(decodeService, "decodeService");

        TextureAssetDescriptor descriptor = new TextureAssetDescriptor(
                resourceId.toString(),
                resourceId.getNamespace(),
                resourceId.getPath(),
                TextureAssetDescriptor.TextureFormat.PNG
        );

        return decodeService.decode(
                descriptor.resourceId(),
                () -> resourceManager.open(resourceId)
        );
    }
}
