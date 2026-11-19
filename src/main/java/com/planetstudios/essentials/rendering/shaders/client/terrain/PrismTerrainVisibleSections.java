package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Snapshot of the sections accepted by Minecraft's own SectionOcclusionGraph.
 * Unknown/stale world snapshots fail open so Prism can never hide terrain merely
 * because its visibility hook has not run yet.
 */
public final class PrismTerrainVisibleSections {
    private record Snapshot(ClientLevel level, Set<SectionKey> sections) {}
    private record SectionKey(int x, int y, int z) {}

    private static volatile Snapshot current;

    private PrismTerrainVisibleSections() {}

    public static void publish(ClientLevel level,
            List<SectionRenderDispatcher.RenderSection> visibleSections) {
        if (level == null) {
            current = null;
            return;
        }
        Set<SectionKey> visible = new HashSet<>(visibleSections.size());
        for (SectionRenderDispatcher.RenderSection section : visibleSections) {
            long node = section.getSectionNode();
            visible.add(new SectionKey(SectionPos.x(node), SectionPos.y(node), SectionPos.z(node)));
        }
        current = new Snapshot(level, Set.copyOf(visible));
    }

    public static void clear() {
        current = null;
    }

    /** Returns true when the graph has not supplied a snapshot for this world. */
    public static boolean isVisible(ClientLevel level, int sectionX, int sectionY, int sectionZ) {
        Snapshot snapshot = current;
        return snapshot == null || snapshot.level() != level
                || snapshot.sections().contains(new SectionKey(sectionX, sectionY, sectionZ));
    }

    public static int visibleSectionCount(ClientLevel level) {
        Snapshot snapshot = current;
        return snapshot != null && snapshot.level() == level ? snapshot.sections().size() : -1;
    }
}
