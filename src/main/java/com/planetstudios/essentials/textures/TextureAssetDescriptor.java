package com.planetstudios.essentials.textures;

import java.util.Objects;

/**
 * Lightweight identity metadata for a texture resource.
 *
 * <p>This descriptor deliberately contains no decoded pixels, open streams,
 * resource-pack handles, or filesystem paths. It is safe to retain across
 * frames and is replaced as a whole when client resources reload.</p>
 */
public record TextureAssetDescriptor(
        String resourceId,
        String namespace,
        String path,
        TextureFormat format
) {
    public TextureAssetDescriptor {
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");

        if (resourceId.isBlank() || namespace.isBlank() || path.isBlank()) {
            throw new IllegalArgumentException("Texture resource identity must not be blank");
        }
        if (!path.startsWith("textures/") || path.contains("../") || path.endsWith("/..")) {
            throw new IllegalArgumentException("Texture path is outside the textures resource tree");
        }
        if (!path.endsWith(".png") || format != TextureFormat.PNG) {
            throw new IllegalArgumentException("Only PNG texture resources are currently catalogued");
        }
    }

    public enum TextureFormat {
        PNG
    }
}
