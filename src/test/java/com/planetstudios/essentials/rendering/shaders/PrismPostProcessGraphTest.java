package com.planetstudios.essentials.rendering.shaders;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrismPostProcessGraphTest {
    @Test
    void topologicallyOrdersPassesAndExecutesInDependencyOrder() {
        var graph = PrismPostProcessGraph.builder()
                .externalInput("planet:scene")
                .pass(new PrismPostProcessGraph.Pass("planet:composite", "planet:composite",
                        List.of("planet:blurred", "planet:scene"), "planet:final"))
                .pass(new PrismPostProcessGraph.Pass("planet:blur", "planet:blur",
                        List.of("planet:scene"), "planet:blurred"))
                .output("planet:final")
                .build();

        assertEquals(List.of("planet:blur", "planet:composite"),
                graph.passes().stream().map(PrismPostProcessGraph.Pass::id).toList());
        List<String> executed = new ArrayList<>();
        graph.execute(pass -> executed.add(pass.id()));
        assertEquals(List.of("planet:blur", "planet:composite"), executed);
    }

    @Test
    void rejectsUnknownInputsAndCycles() {
        assertThrows(IllegalArgumentException.class, () -> PrismPostProcessGraph.builder()
                .externalInput("planet:scene")
                .pass(new PrismPostProcessGraph.Pass("planet:fx", "planet:fx",
                        List.of("planet:missing"), "planet:out"))
                .output("planet:out").build());

        assertThrows(IllegalArgumentException.class, () -> PrismPostProcessGraph.builder()
                .pass(new PrismPostProcessGraph.Pass("planet:a", "planet:a",
                        List.of("planet:b"), "planet:a_out"))
                .pass(new PrismPostProcessGraph.Pass("planet:b", "planet:b",
                        List.of("planet:a_out"), "planet:b"))
                .output("planet:b").build());
    }

    @Test
    void rejectsDuplicateOutputsAndInPlaceFeedback() {
        assertThrows(IllegalArgumentException.class, () ->
                new PrismPostProcessGraph.Pass("planet:copy", "planet:copy",
                        List.of("planet:scene"), "planet:scene"));
        assertThrows(IllegalArgumentException.class, () -> PrismPostProcessGraph.builder()
                .externalInput("planet:scene")
                .pass(new PrismPostProcessGraph.Pass("planet:a", "planet:a",
                        List.of("planet:scene"), "planet:out"))
                .pass(new PrismPostProcessGraph.Pass("planet:b", "planet:b",
                        List.of("planet:scene"), "planet:out"))
                .output("planet:out").build());
    }
}
