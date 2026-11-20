package com.planetstudios.essentials.textures;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Lightweight, validated identity metadata for an effective PNG texture.
 * No decoded pixels, streams, pack handles, or filesystem paths are retained.
 */
public record TextureAssetDescriptor(
        String resourceId,
        String namespace,
        String path,
        TextureFormat format
) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern RESOURCE_PATH = Pattern.compile("[a-z0-9/._-]+");

    public TextureAssetDescriptor {
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");

        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid texture namespace");
        }
        if (!RESOURCE_PATH.matcher(path).matches()
                || !path.startsWith("textures/")
                || !path.endsWith(".png")
                || path.contains("//")
                || path.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Invalid texture resource path");
        }
        for (String segment : path.split("/")) {
            if (segment.equals(".") || segment.equals("..") || segment.isEmpty()) {
                throw new IllegalArgumentException("Texture path contains an invalid segment");
            }
        }
        if (!resourceId.equals(namespace + ":" + path)) {
            throw new IllegalArgumentException("Resource ID does not match namespace and path");
        }
        if (format != TextureFormat.PNG) {
            throw new IllegalArgumentException("Only PNG texture resources are currently supported");
        }
    }

    /** A coarse path-based grouping for diagnostics and future policy selection. */
    public TextureCategory category() {
        String relativePath = path.substring("textures/".length());
        if (relativePath.startsWith("gui/")) return TextureCategory.GUI;
        if (relativePath.startsWith("block/")) return TextureCategory.BLOCK;
        if (relativePath.startsWith("entity/")) return TextureCategory.ENTITY;
        if (relativePath.startsWith("item/")) return TextureCategory.ITEM;
        if (relativePath.startsWith("particle/")) return TextureCategory.PARTICLE;
        if (relativePath.startsWith("environment/")) return TextureCategory.ENVIRONMENT;
        if (relativePath.startsWith("mob_effect/")) return TextureCategory.MOB_EFFECT;
        return TextureCategory.OTHER;
    }

    public enum TextureFormat {
        PNG
    }

    public enum TextureCategory {
        GUI, BLOCK, ENTITY, ITEM, PARTICLE, ENVIRONMENT, MOB_EFFECT, OTHER
    }
}
