package com.planetstudios.essentials.diagnostics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticsExporterTest {
    @TempDir Path temp;

    @Test void exportsOnlyTheProvidedLocalDiagnosticSnapshot() throws Exception {
        PerformanceSample sample = new PerformanceSample(Instant.parse("2026-01-02T03:04:05Z"), 144);
        DiagnosticEvent event = new DiagnosticEvent(Instant.parse("2026-01-02T03:04:06Z"),
                DiagnosticEventType.SLOW_CLIENT_TICK, 73);

        Path output = DiagnosticsExporter.export(temp, List.of(sample), List.of(event));
        assertTrue(Files.isRegularFile(output));
        assertTrue(output.startsWith(temp.resolve("planet-essentials").resolve("exports")));

        JsonObject json = JsonParser.parseString(Files.readString(output)).getAsJsonObject();
        assertEquals(1, json.get("schemaVersion").getAsInt());
        assertEquals(1, json.getAsJsonArray("performanceSamples").size());
        assertEquals(144, json.getAsJsonArray("performanceSamples").get(0)
                .getAsJsonObject().get("framesPerSecond").getAsInt());
        assertEquals("SLOW_CLIENT_TICK", json.getAsJsonArray("events").get(0)
                .getAsJsonObject().get("type").getAsString());
        assertEquals(73, json.getAsJsonArray("events").get(0)
                .getAsJsonObject().get("durationMillis").getAsLong());
        assertFalse(Files.readString(output).contains("serverAddress"));
        assertFalse(Files.readString(output).contains("playerName"));
    }

    @Test void exportsEmptyHistoriesAndNeverOverwritesEarlierExports() {
        Path first = DiagnosticsExporter.export(temp, List.of(), List.of());
        Path second = DiagnosticsExporter.export(temp, List.of(), List.of());
        assertNotEquals(first, second);
        assertTrue(Files.exists(first));
        assertTrue(Files.exists(second));
    }

    @Test void rejectsNullInputs() {
        assertThrows(NullPointerException.class,
                () -> DiagnosticsExporter.export(temp, null, List.of()));
    }
}
