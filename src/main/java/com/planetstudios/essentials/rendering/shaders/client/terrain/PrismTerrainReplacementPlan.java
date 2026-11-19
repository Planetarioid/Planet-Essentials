package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Builds an all-or-nothing replacement plan for one vanilla terrain layer group. */
public final class PrismTerrainReplacementPlan {
    private PrismTerrainReplacementPlan() {}

    /**
     * Planning only: drawReady must validate the current CPU payload and live GPU mesh.
     * No GPU work is submitted and vanilla is never suppressed by this class.
     */
    public static Plan build(List<?> vanillaDraws,
            PrismTerrainVanillaDrawIndex.Snapshot drawIndex,
            ChunkSectionLayer expectedLayer,
            Predicate<PrismTerrainVanillaDrawIndex.DrawDescriptor> drawReady,
            boolean programReady, boolean deviceCompatible, boolean frameReady,
            boolean reverseOrder) {
        Objects.requireNonNull(vanillaDraws, "vanillaDraws");
        Objects.requireNonNull(drawIndex, "drawIndex");
        Objects.requireNonNull(expectedLayer, "expectedLayer");
        Objects.requireNonNull(drawReady, "drawReady");

        int required = vanillaDraws.size();
        if (required == 0) return new Plan(List.of(),
                PrismTerrainReplacementGate.FallbackReason.EMPTY_GROUP, 0, 0);
        if (!drawIndex.isCompleteFor(vanillaDraws)) return failed(required, 0,
                PrismTerrainReplacementGate.FallbackReason.INCOMPLETE_COVERAGE);

        List<Entry> entries = new ArrayList<>(required);
        Set<SectionKey> seen = new HashSet<>();
        int ready = 0;
        for (Object vanillaDraw : vanillaDraws) {
            var descriptor = drawIndex.byDraw().get(vanillaDraw);
            if (descriptor == null || descriptor.layer() != expectedLayer) {
                return failed(required, ready, PrismTerrainReplacementGate.FallbackReason.INCOMPLETE_COVERAGE);
            }
            SectionKey key = new SectionKey(descriptor.sectionX(), descriptor.sectionY(),
                    descriptor.sectionZ(), descriptor.layer());
            if (!seen.add(key)) return failed(required, ready,
                    PrismTerrainReplacementGate.FallbackReason.INCOMPLETE_COVERAGE);
            if (drawReady.test(descriptor)) {
                ready++;
                entries.add(new Entry(vanillaDraw, descriptor));
            }
        }

        var reason = PrismTerrainReplacementGate.evaluate(new PrismTerrainReplacementGate.Readiness(
                required, ready, programReady, deviceCompatible, frameReady));
        if (reason != PrismTerrainReplacementGate.FallbackReason.NONE) {
            return new Plan(List.of(), reason, required, ready);
        }
        if (reverseOrder) java.util.Collections.reverse(entries);
        return new Plan(entries, PrismTerrainReplacementGate.FallbackReason.NONE, required, ready);
    }

    private static Plan failed(int required, int ready,
            PrismTerrainReplacementGate.FallbackReason reason) {
        return new Plan(List.of(), reason, required, Math.min(ready, required));
    }

    public record Entry(Object vanillaDraw,
            PrismTerrainVanillaDrawIndex.DrawDescriptor descriptor) {
        public Entry {
            Objects.requireNonNull(vanillaDraw, "vanillaDraw");
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }

    public record Plan(List<Entry> entries,
            PrismTerrainReplacementGate.FallbackReason fallbackReason,
            int requiredDraws, int readyDraws) {
        public Plan {
            entries = List.copyOf(entries);
            Objects.requireNonNull(fallbackReason, "fallbackReason");
            if (requiredDraws < 0 || readyDraws < 0 || readyDraws > requiredDraws)
                throw new IllegalArgumentException("Invalid replacement plan coverage");
            if (fallbackReason == PrismTerrainReplacementGate.FallbackReason.NONE
                    && (requiredDraws == 0 || entries.size() != requiredDraws || readyDraws != requiredDraws))
                throw new IllegalArgumentException("A replacement plan must cover every draw");
            if (fallbackReason != PrismTerrainReplacementGate.FallbackReason.NONE && !entries.isEmpty())
                throw new IllegalArgumentException("Fallback plans cannot contain replacement draws");
        }
        public boolean mayReplace() {
            return fallbackReason == PrismTerrainReplacementGate.FallbackReason.NONE;
        }
    }

    private record SectionKey(int x, int y, int z, ChunkSectionLayer layer) {}
}
