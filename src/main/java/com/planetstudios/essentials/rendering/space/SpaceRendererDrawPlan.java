package com.planetstudios.essentials.rendering.space;

import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainGpuMesh;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, fully validated material-aware draw plan for one visible frame.
 *
 * <p>This is a preparation boundary, not a draw executor. It deliberately
 * contains no RenderPass or shader-pack dependency so the built-in Space
 * Renderer pipeline can consume it independently.</p>
 */
public final class SpaceRendererDrawPlan {
    private final long frameSequence;
    private final List<Batch> batches;
    private final int drawCount;
    private final boolean ready;

    private SpaceRendererDrawPlan(long frameSequence, List<Batch> batches, int drawCount, boolean ready) {
        this.frameSequence = frameSequence;
        this.batches = List.copyOf(batches);
        this.drawCount = drawCount;
        this.ready = ready;
    }

    public static SpaceRendererDrawPlan empty(long frameSequence) {
        if (frameSequence <= 0) throw new IllegalArgumentException("Frame sequence must be positive");
        return new SpaceRendererDrawPlan(frameSequence, List.of(), 0, false);
    }

    /**
     * Validates every mesh and translucent index allocation before producing
     * stable material batches. Translucent draws remain individual and retain
     * vanilla section order; opaque/cutout draws may be grouped by material.
     */
    public static SpaceRendererDrawPlan prepare(SpaceRendererFrameSnapshot frame,
            List<SpaceRendererSortedIndexBuffer> sortedBuffers) {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(sortedBuffers, "sortedBuffers");
        if (!frame.isComplete()) {
            throw new IllegalArgumentException("An incomplete visible frame cannot produce a draw plan");
        }

        Map<SpaceRendererFrameSnapshot.Draw, SpaceRendererSortedIndexBuffer> sortedByDraw =
                new IdentityHashMap<>();
        for (SpaceRendererSortedIndexBuffer buffer : sortedBuffers) {
            Objects.requireNonNull(buffer, "sorted index buffer");
            if (!buffer.isOpen()) throw new IllegalArgumentException("Sorted index buffer is closed");
            if (!frame.draws().contains(buffer.draw())) {
                throw new IllegalArgumentException("Sorted index buffer belongs to a different frame");
            }
            if (sortedByDraw.put(buffer.draw(), buffer) != null) {
                throw new IllegalArgumentException("A translucent draw has more than one sorted index buffer");
            }
        }

        List<BatchBuilder> orderedBatches = new ArrayList<>();
        Map<BatchKey, BatchBuilder> opaqueGroups = new LinkedHashMap<>();
        int drawCount = 0;
        for (SpaceRendererFrameSnapshot.Draw draw : frame.draws()) {
            PrismTerrainGpuMesh mesh = draw.mesh();
            if (!mesh.isOpen() || mesh.vertexCount() <= 0 || mesh.vertexCount() % 4 != 0) {
                throw new IllegalArgumentException("Visible frame contains an invalid GPU terrain mesh");
            }
            ChunkSectionLayer layer = layerFromLabel(draw.input().layerId());
            boolean translucent = requiresSortedIndices(layer);
            int indexCount = indexCount(mesh.vertexCount());
            SpaceRendererSortedIndexBuffer sorted = sortedByDraw.remove(draw);
            if (translucent) {
                if (sorted == null || sorted.indexCount() != indexCount || sorted.draw() != draw) {
                    throw new IllegalArgumentException("Translucent draw is missing a matching sorted index buffer");
                }
                orderedBatches.add(new BatchBuilder(layer, draw.input().materialId(), true,
                        new DrawCommand(draw, sorted, indexCount)));
            } else {
                if (sorted != null) {
                    throw new IllegalArgumentException("Opaque or cutout draw unexpectedly has sorted indices");
                }
                BatchKey key = new BatchKey(layer, draw.input().materialId());
                BatchBuilder group = opaqueGroups.get(key);
                if (group == null) {
                    group = new BatchBuilder(layer, draw.input().materialId(), false, null);
                    opaqueGroups.put(key, group);
                    orderedBatches.add(group);
                }
                group.draws.add(new DrawCommand(draw, null, indexCount));
            }
            drawCount++;
        }
        if (!sortedByDraw.isEmpty()) {
            throw new IllegalArgumentException("Sorted index buffers contain draws absent from the visible frame");
        }

        List<Batch> batches = orderedBatches.stream().map(BatchBuilder::build).toList();
        return new SpaceRendererDrawPlan(frame.sequence(), batches, drawCount, true);
    }

    static int indexCount(int vertexCount) {
        if (vertexCount <= 0 || vertexCount % 4 != 0) {
            throw new IllegalArgumentException("Terrain vertex count must be a positive multiple of four");
        }
        return Math.multiplyExact(vertexCount / 4, 6);
    }

    static boolean requiresSortedIndices(ChunkSectionLayer layer) {
        return layer == ChunkSectionLayer.TRANSLUCENT || layer == ChunkSectionLayer.TRIPWIRE;
    }

    private static ChunkSectionLayer layerFromLabel(String label) {
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            if (layer.label().equals(label)) return layer;
        }
        throw new IllegalArgumentException("Unknown vanilla terrain layer: " + label);
    }

    public long frameSequence() { return frameSequence; }
    public List<Batch> batches() { return batches; }
    public int drawCount() { return drawCount; }
    public boolean isReady() { return ready; }

    public record Batch(ChunkSectionLayer layer, String materialId,
            List<DrawCommand> draws, boolean translucent) {
        public Batch {
            Objects.requireNonNull(layer, "layer");
            Objects.requireNonNull(materialId, "materialId");
            draws = List.copyOf(draws);
            if (draws.isEmpty()) throw new IllegalArgumentException("Draw batch cannot be empty");
            if (translucent && draws.size() != 1) {
                throw new IllegalArgumentException("Translucent draws must retain individual ordering");
            }
        }
    }

    public record DrawCommand(SpaceRendererFrameSnapshot.Draw draw,
            SpaceRendererSortedIndexBuffer sortedIndices, int indexCount) {
        public DrawCommand {
            Objects.requireNonNull(draw, "draw");
            if (indexCount <= 0) throw new IllegalArgumentException("Draw index count must be positive");
        }
    }

    private static final class BatchBuilder {
        private final ChunkSectionLayer layer;
        private final String materialId;
        private final boolean translucent;
        private final List<DrawCommand> draws = new ArrayList<>();
        private BatchBuilder(ChunkSectionLayer layer, String materialId, boolean translucent, DrawCommand first) {
            this.layer = layer;
            this.materialId = materialId;
            this.translucent = translucent;
            if (first != null) draws.add(first);
        }
        private Batch build() { return new Batch(layer, materialId, draws, translucent); }
    }

    private record BatchKey(ChunkSectionLayer layer, String materialId) {}
}
