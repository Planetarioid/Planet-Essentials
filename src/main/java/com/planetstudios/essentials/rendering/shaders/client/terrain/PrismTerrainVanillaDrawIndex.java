package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Identity index connecting vanilla Draw records to their source section and layer. */
public final class PrismTerrainVanillaDrawIndex {
    private static volatile Snapshot current = Snapshot.empty();
    private PrismTerrainVanillaDrawIndex() {}

    public static void capture(List<SectionRenderDispatcher.RenderSection> visibleSections,
                               ChunkSectionsToRender rendered) {
        if (visibleSections == null || rendered == null) { current = Snapshot.empty(); return; }
        IdentityHashMap<Object, DrawDescriptor> indexed = new IdentityHashMap<>();
        int drawCount = 0;
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            List<?> draws = (List<?>) rendered.drawsPerLayer().get(layer);
            if (draws == null) { current = Snapshot.empty(); return; }
            int drawIndex = 0;
            for (SectionRenderDispatcher.RenderSection section : visibleSections) {
                if (section.getSectionMesh().getBuffers(layer) == null) continue;
                if (drawIndex >= draws.size()) { current = Snapshot.empty(); return; }
                Object draw = draws.get(drawIndex);
                if (!(draw instanceof RenderPass.Draw<?>)) { current = Snapshot.empty(); return; }
                long node = section.getSectionNode();
                DrawDescriptor descriptor = new DrawDescriptor(SectionPos.x(node), SectionPos.y(node),
                        SectionPos.z(node), layer, drawIndex);
                if (indexed.put(draw, descriptor) != null) { current = Snapshot.empty(); return; }
                drawIndex++;
                drawCount++;
            }
            if (drawIndex != draws.size()) { current = Snapshot.empty(); return; }
        }
        current = new Snapshot(indexed, drawCount);
    }

    public static DrawDescriptor descriptor(Object vanillaDraw) { return current.byDraw().get(vanillaDraw); }
    public static Snapshot snapshot() { return current; }
    public static void clear() { current = Snapshot.empty(); }

    public record DrawDescriptor(int sectionX, int sectionY, int sectionZ,
                                 ChunkSectionLayer layer, int layerDrawIndex) {
        public String materialId() { return "planet_essentials:terrain/vanilla/" + layer.label(); }
    }

    public record Snapshot(Map<Object, DrawDescriptor> byDraw, int drawCount) {
        public Snapshot {
            IdentityHashMap<Object, DrawDescriptor> copy = new IdentityHashMap<>();
            copy.putAll(byDraw);
            byDraw = Collections.unmodifiableMap(copy);
            if (drawCount < 0 || drawCount != byDraw.size()) {
                throw new IllegalArgumentException("Invalid vanilla terrain draw index");
            }
        }
        public static Snapshot empty() { return new Snapshot(Map.of(), 0); }
        public boolean isCompleteFor(List<?> draws) {
            if (draws == null) return false;
            for (Object draw : draws) if (!byDraw.containsKey(draw)) return false;
            return true;
        }
        public List<DrawDescriptor> descriptorsInOrder(List<?> draws, boolean reverse) {
            if (!isCompleteFor(draws)) return List.of();
            List<DrawDescriptor> result = new ArrayList<>(draws.size());
            for (Object draw : draws) result.add(byDraw.get(draw));
            if (reverse) Collections.reverse(result);
            return List.copyOf(result);
        }
    }
}
