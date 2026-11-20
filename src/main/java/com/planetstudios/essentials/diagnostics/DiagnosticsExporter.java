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

/** Writes a local diagnostics snapshot only when explicitly invoked by the user. */
public final class DiagnosticsExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    private DiagnosticsExporter() {}

    public static Path export(Path configDirectory,
                              List<PerformanceSample> samples,
                              List<DiagnosticEvent> events) {
        if (configDirectory == null || samples == null || events == null) {
            throw new NullPointerException("Export inputs cannot be null");
        }
        Path exportDirectory = configDirectory.resolve("planet-essentials").resolve("exports");
        Path temporary = null;
        try {
            Files.createDirectories(exportDirectory);
            JsonObject root = new JsonObject();
            root.addProperty("schemaVersion", 1);
            root.addProperty("exportedAt", Instant.now().toString());

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

            JsonArray diagnosticEvents = new JsonArray();
            for (DiagnosticEvent event : List.copyOf(events)) {
                JsonObject item = new JsonObject();
                item.addProperty("timestamp", event.timestamp().toString());
                item.addProperty("type", event.type().name());
                item.addProperty("durationMillis", event.durationMillis());
                diagnosticEvents.add(item);
            }
            root.add("events", diagnosticEvents);

            String baseName = "diagnostics-" + FILE_TIME.format(Instant.now());
            Path destination = uniqueDestination(exportDirectory, baseName);
            temporary = Files.createTempFile(exportDirectory, ".diagnostics-", ".tmp");
            Files.writeString(temporary, GSON.toJson(root));
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination);
            }
            return destination;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not export Planet Essentials diagnostics", exception);
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
