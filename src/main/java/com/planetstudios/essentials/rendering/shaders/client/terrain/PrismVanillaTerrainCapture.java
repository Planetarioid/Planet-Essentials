package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * Converts Minecraft 1.21.11's actual BLOCK-format section meshes into Prism's
 * versioned V1 payload. This is a CPU copy only; vanilla retains ownership of
 * its MeshData and GPU upload lifecycle.
 */
public final class PrismVanillaTerrainCapture {
    private static final int VANILLA_STRIDE = DefaultVertexFormat.BLOCK.getVertexSize();

    private PrismVanillaTerrainCapture() {}

    static void capture(PrismVanillaSectionRebuildEvents.RebuildContext context,
                        SectionCompiler.Results results) {
        if (results.renderedLayers == null || results.renderedLayers.isEmpty()) return;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) results.renderedLayers).entrySet()) {
            if (!(entry.getKey() instanceof ChunkSectionLayer layer)
                    || !(entry.getValue() instanceof MeshData mesh)) continue;
            try {
                PrismTerrainVertexData data = convert(context.sectionPos(), layer, mesh);
                if (data != null && !context.publish(data)) {
                    PlanetEssentials.LOGGER.debug("Discarded stale Prism terrain capture for section {}",
                            context.sectionPos());
                }
            } catch (RuntimeException failure) {
                PlanetEssentials.LOGGER.warn("Could not capture vanilla {} terrain for section {}",
                        layer.label(), context.sectionPos(), failure);
            }
        }
    }

    private static PrismTerrainVertexData convert(SectionPos sectionPos,
            ChunkSectionLayer layer, MeshData mesh) {
        MeshData.DrawState draw = mesh.drawState();
        if (draw.mode() != VertexFormat.Mode.QUADS) return null;
        return convert(sectionPos, layer, mesh.vertexBuffer(), draw.format(), draw.vertexCount());
    }

    /** Package-visible pure conversion entry used by tests. */
    static PrismTerrainVertexData convert(SectionPos sectionPos, ChunkSectionLayer layer,
            ByteBuffer source, VertexFormat format, int vertexCount) {
        if (!DefaultVertexFormat.BLOCK.equals(format) || format.getVertexSize() != VANILLA_STRIDE) {
            throw new IllegalArgumentException("Unsupported vanilla terrain vertex format: " + format);
        }
        if (vertexCount <= 0 || (vertexCount & 3) != 0) {
            throw new IllegalArgumentException("Vanilla terrain quad vertex count must be positive and divisible by four");
        }
        int requiredBytes = Math.multiplyExact(vertexCount, VANILLA_STRIDE);
        ByteBuffer input = source.duplicate().order(ByteOrder.nativeOrder());
        if (input.remaining() < requiredBytes) {
            throw new IllegalArgumentException("Vanilla terrain vertex buffer is shorter than its draw state");
        }

        int positionOffset = format.getOffset(VertexFormatElement.POSITION);
        int colorOffset = format.getOffset(VertexFormatElement.COLOR);
        int uvOffset = format.getOffset(VertexFormatElement.UV0);
        int lightOffset = format.getOffset(VertexFormatElement.UV2);
        int normalOffset = format.getOffset(VertexFormatElement.NORMAL);
        if (positionOffset < 0 || colorOffset < 0 || uvOffset < 0 || lightOffset < 0 || normalOffset < 0) {
            throw new IllegalArgumentException("Vanilla BLOCK vertex format is missing a required terrain attribute");
        }

        PrismFrameStage stage = layer == ChunkSectionLayer.TRANSLUCENT || layer == ChunkSectionLayer.TRIPWIRE
                ? PrismFrameStage.BEFORE_TRANSLUCENT : PrismFrameStage.BEFORE_ENTITIES;
        String material = "planet_essentials:terrain/vanilla/" + layer.label();
        PrismTerrainVertexData.Builder builder = PrismTerrainVertexData.builder(
                sectionPos.x(), sectionPos.y(), sectionPos.z(), material, stage);

        int base = input.position();
        for (int quad = 0; quad < vertexCount; quad += 4) {
            PrismTerrainVertex[] vertices = new PrismTerrainVertex[4];
            for (int corner = 0; corner < 4; corner++) {
                int offset = base + (quad + corner) * VANILLA_STRIDE;
                float x = input.getFloat(offset + positionOffset);
                float y = input.getFloat(offset + positionOffset + 4);
                float z = input.getFloat(offset + positionOffset + 8);
                int color = input.getInt(offset + colorOffset);
                float u = input.getFloat(offset + uvOffset);
                float v = input.getFloat(offset + uvOffset + 4);
                int light = input.getInt(offset + lightOffset);
                int normal = input.getInt(offset + normalOffset);
                // Vanilla BLOCK has no overlay element. Zero is the neutral overlay.
                vertices[corner] = new PrismTerrainVertex(x, y, z, u, v, color, light, 0, normal);
            }
            builder.addQuad(vertices[0], vertices[1], vertices[2], vertices[3]);
        }
        return builder.build();
    }
}
