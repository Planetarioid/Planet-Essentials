package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Bridges Prism's immutable terrain frame queue to Fabric's 1.21.11
 * extraction/drawing lifecycle. This schedules cooperating terrain providers;
 * it does not replace or intercept vanilla chunk meshing.
 */
public final class PrismTerrainFrameCoordinator {
    private static final PrismTerrainFrameQueue QUEUE = new PrismTerrainFrameQueue();
    private static final CopyOnWriteArrayList<ExtractionEntry> EXTRACTORS = new CopyOnWriteArrayList<>();
    private static final Map<PrismFrameStage, CopyOnWriteArrayList<DrawEntry>> DRAWERS =
            new EnumMap<>(PrismFrameStage.class);
    private static final AtomicBoolean EVENTS_REGISTERED = new AtomicBoolean();
    private static volatile PrismTerrainFrame publishedFrame =
            new PrismTerrainFrame(0, List.of(), 0);
    private static volatile boolean extractionActive;

    static {
        DRAWERS.put(PrismFrameStage.BEFORE_ENTITIES, new CopyOnWriteArrayList<>());
        DRAWERS.put(PrismFrameStage.BEFORE_TRANSLUCENT, new CopyOnWriteArrayList<>());
    }

    private PrismTerrainFrameCoordinator() {}

    /** Installs the Fabric lifecycle bridge once during client initialization. */
    public static void registerFabricEvents() {
        if (!EVENTS_REGISTERED.compareAndSet(false, true)) return;
        WorldRenderEvents.END_EXTRACTION.register(PrismTerrainFrameCoordinator::extract);
        WorldRenderEvents.BEFORE_ENTITIES.register(
                context -> draw(PrismFrameStage.BEFORE_ENTITIES, context));
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(
                context -> draw(PrismFrameStage.BEFORE_TRANSLUCENT, context));
    }

    public static Registration registerExtractor(Consumer<WorldExtractionContext> extractor) {
        ExtractionEntry entry = new ExtractionEntry(Objects.requireNonNull(extractor, "extractor"));
        EXTRACTORS.add(entry);
        return () -> {
            entry.enabled.set(false);
            EXTRACTORS.remove(entry);
        };
    }

    public static Registration registerRenderer(
            PrismFrameStage stage, BiConsumer<WorldRenderContext, PrismTerrainFrame> renderer) {
        Objects.requireNonNull(stage, "stage");
        CopyOnWriteArrayList<DrawEntry> entries = DRAWERS.get(stage);
        if (entries == null) {
            throw new IllegalArgumentException("Terrain rendering supports BEFORE_ENTITIES and BEFORE_TRANSLUCENT only");
        }
        DrawEntry entry = new DrawEntry(Objects.requireNonNull(renderer, "renderer"));
        entries.add(entry);
        return () -> {
            entry.enabled.set(false);
            entries.remove(entry);
        };
    }

    public static void submit(PrismTerrainMesh mesh) {
        if (!extractionActive) {
            throw new IllegalStateException("Terrain meshes may only be submitted during END_EXTRACTION callbacks");
        }
        QUEUE.submit(Objects.requireNonNull(mesh, "mesh"));
    }

    public static PrismTerrainFrame visibleFrame(PrismTerrainFrustum frustum) {
        return PrismTerrainCulling.visibleFrame(publishedFrame, Objects.requireNonNull(frustum, "frustum"));
    }

    /** Removes stale section meshes for a chunk immediately after its unload event. */
    public static void invalidateChunk(int chunkX, int chunkZ) {
        PrismTerrainFrame frame = publishedFrame;
        List<PrismTerrainMesh> retained = frame.meshes().stream()
                .filter(mesh -> mesh.sectionX() != chunkX || mesh.sectionZ() != chunkZ)
                .toList();
        if (retained.size() == frame.meshes().size()) return;
        int vertices = retained.stream().mapToInt(PrismTerrainMesh::vertexCount).sum();
        publishedFrame = new PrismTerrainFrame(frame.generation(), retained, vertices);
    }

    public static PrismTerrainFrame currentFrame() {
        return publishedFrame;
    }

    private static void extract(WorldExtractionContext context) {
        long generation = QUEUE.beginFrame();
        extractionActive = true;
        try {
            for (ExtractionEntry entry : EXTRACTORS) {
                if (!entry.enabled.get()) continue;
                try {
                    entry.callback.accept(context);
                } catch (RuntimeException failure) {
                    entry.enabled.set(false);
                    EXTRACTORS.remove(entry);
                    PlanetEssentials.LOGGER.error(
                            "Disabled a failing Prism terrain extraction provider", failure);
                }
            }
        } finally {
            extractionActive = false;
        }
        try {
            publishedFrame = QUEUE.publish();
        } catch (RuntimeException failure) {
            QUEUE.clear();
            publishedFrame = new PrismTerrainFrame(generation, List.of(), 0);
            PlanetEssentials.LOGGER.error("Failed to publish Prism terrain frame {}", generation, failure);
        }
    }

    private static void draw(PrismFrameStage stage, WorldRenderContext context) {
        PrismTerrainFrame frame = publishedFrame;
        if (frame.meshes(stage).isEmpty()) return;
        for (DrawEntry entry : DRAWERS.get(stage)) {
            if (!entry.enabled.get()) continue;
            try {
                entry.callback.accept(context, frame);
            } catch (RuntimeException failure) {
                entry.enabled.set(false);
                DRAWERS.get(stage).remove(entry);
                PlanetEssentials.LOGGER.error(
                        "Disabled a failing Prism terrain renderer at {}", stage, failure);
            }
        }
    }

    private static final class ExtractionEntry {
        private final Consumer<WorldExtractionContext> callback;
        private final AtomicBoolean enabled = new AtomicBoolean(true);
        private ExtractionEntry(Consumer<WorldExtractionContext> callback) { this.callback = callback; }
    }

    private static final class DrawEntry {
        private final BiConsumer<WorldRenderContext, PrismTerrainFrame> callback;
        private final AtomicBoolean enabled = new AtomicBoolean(true);
        private DrawEntry(BiConsumer<WorldRenderContext, PrismTerrainFrame> callback) { this.callback = callback; }
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override void close();
    }
}
