package com.planetstudios.essentials.rendering.shaders.client;

import com.planetstudios.essentials.rendering.shaders.ShaderPackLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Resolves shader resources using Minecraft's active, priority-ordered resource manager. */
public final class MinecraftShaderResourceResolver implements ShaderPackLoader.ShaderResourceResolver {
    private final ResourceManager resourceManager;

    public MinecraftShaderResourceResolver(ResourceManager resourceManager) {
        this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
    }

    @Override
    public String readUtf8(String resourceId, int maxBytes) {
        Identifier id = Identifier.tryParse(resourceId);
        if (id == null) {
            throw new IllegalArgumentException("Invalid shader resource identifier: " + resourceId);
        }
        Resource resource = resourceManager.getResource(id)
                .orElseThrow(() -> new IllegalArgumentException("Shader resource not found: " + resourceId));
        try (resource; InputStream input = resource.open()) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new IllegalArgumentException("Shader resource exceeds byte limit: " + resourceId);
            }
            try {
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString();
            } catch (CharacterCodingException malformed) {
                throw new IllegalArgumentException("Shader resource is not valid UTF-8: " + resourceId, malformed);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read shader resource: " + resourceId, exception);
        }
    }
}
