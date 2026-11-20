package com.planetstudios.essentials.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Writes a local diagnostics or benchmark snapshot only when explicitly invoked by the user. */
public final class DiagnosticsExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    private DiagnosticsExporter() {}

    public static Path export(Path configDirectory,
                              List<PerformanceSample> samples,
                              List<DiagnosticEvent> events) {
        return exportBenchmark(configDirectory, samples, List.of(), events, Map.of());
    }

    /** Full benchmark bundle. No data is uploaded or transmitted. */
    public static Path exportBenchmark(Path configDirectory,
                                       List<PerformanceSample> samples,
                                       List<FrameTimeSample> frameTimes,
                                       List<DiagnosticEvent> events,
                                       Map<String, String> metadata) {
        if (configDirectory == null || samples == null || frameTimes == null
                || events == null || metadata == null) {
            throw new NullPointerException("Export inputs cannot be null");
        }
        Path exportDirectory = configDirectory.resolve("planet-essentials").resolve("exports");
        Path temporary = null;
        try {
            Files.createDirectories(exportDirectory);
            JsonObject root = new JsonObject();
            root.addProperty("schemaVersion", 2);
            root.addProperty("exportType", "space-client-benchmark");
            root.addProperty("exportedAt", Instant.now().toString());

            JsonObject environment = new JsonObject();
            metadata.forEach(environment::addProperty);
            root.add("environment", environment);

            JsonArray performance = new JsonArray();
            for (PerformanceSample sample : List.copyOf(samples)) {
                JsonObject item = new JsonObject();
                item.addProperty("timestamp", sample.timestamp().toString());
                item.addProperty("framesPerSecond", sample.framesPerSecond());
                item.addProperty("usedHeapBytes", sample.usedHeapBytes());
                item.addProperty("maxHeapBytes", sample.maxHeapBytes());
                performance.add(item);
            }
            root.add("performanceSamples", performance);

            JsonArray frameData = new JsonArray();
            for (FrameTimeSample sample : List.copyOf(frameTimes)) {
                JsonObject item = new JsonObject();
                item.addProperty("timestamp", sample.timestamp().toString());
                item.addProperty("frameTimeNanos", sample.frameTimeNanos());
                item.addProperty("frameTimeMillis", sample.milliseconds());
                item.addProperty("estimatedFps", sample.estimatedFps());
                frameData.add(item);
            }
            root.add("frameTimeSamples", frameData);

            JsonArray diagnosticEvents = new JsonArray();
            for (DiagnosticEvent event : List.copyOf(events)) {
                JsonObject item = new JsonObject();
                item.addProperty("timestamp", event.timestamp().toString());
                item.addProperty("type", event.type().name());
                item.addProperty("durationMillis", event.durationMillis());
                diagnosticEvents.add(item);
            }
            root.add("events", diagnosticEvents);

            String baseName = "space-client-benchmark-" + FILE_TIME.format(Instant.now());
            Path destination = uniqueDestination(exportDirectory, baseName);
            temporary = Files.createTempFile(exportDirectory, ".benchmark-", ".tmp");
            Files.writeString(temporary, GSON.toJson(root));
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination);
            }
            return destination;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not export Space Client benchmark", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }
    }

    private static Path uniqueDestination(Path directory, String baseName) {
        Path candidate = directory.resolve(baseName + ".json");
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(baseName + "-" + suffix++ + ".json");
        }
        return candidate;
    }
}
