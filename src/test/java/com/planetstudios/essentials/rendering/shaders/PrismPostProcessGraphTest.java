package com.planetstudios.essentials.rendering.shaders;

import com.planetstudios.essentials.rendering.shaders.client.PrismPostProcessExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrismPostProcessGraphTest {
    @Test
    void topologicallyOrdersPassesAndExecutesInDependencyOrder() {
        var graph = PrismPostProcessGraph.builder()
                .externalInput(PrismPostProcessExecutor.SCENE_COLOR)
                .pass(new PrismPostProcessGraph.Pass("planet:composite", "planet:composite",
                        List.of("planet:blurred", PrismPostProcessExecutor.SCENE_COLOR), "planet:final"))
                .pass(new PrismPostProcessGraph.Pass("planet:blur", "planet:blur",
                        List.of(PrismPostProcessExecutor.SCENE_COLOR), "planet:blurred"))
                .output("planet:final").build();

        assertEquals(List.of("planet:blur", "planet:composite"),
                graph.passes().stream().map(PrismPostProcessGraph.Pass::id).toList());
        List<String> executed = new ArrayList<>();
        graph.execute(pass -> executed.add(pass.id()));
        assertEquals(List.of("planet:blur", "planet:composite"), executed);
    }

    @Test
    void rejectsUnknownInputsAndCycles() {
        assertThrows(IllegalArgumentException.class, () -> PrismPostProcessGraph.builder()
                .externalInput(PrismPostProcessExecutor.SCENE_COLOR)
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
                        List.of(PrismPostProcessExecutor.SCENE_COLOR), PrismPostProcessExecutor.SCENE_COLOR));
        assertThrows(IllegalArgumentException.class, () -> PrismPostProcessGraph.builder()
                .externalInput(PrismPostProcessExecutor.SCENE_COLOR)
                .pass(new PrismPostProcessGraph.Pass("planet:a", "planet:a",
                        List.of(PrismPostProcessExecutor.SCENE_COLOR), "planet:out"))
                .pass(new PrismPostProcessGraph.Pass("planet:b", "planet:b",
                        List.of(PrismPostProcessExecutor.SCENE_COLOR), "planet:out"))
                .output("planet:out").build());
    }

    @Test
    void capsGraphSize() {
        var builder = PrismPostProcessGraph.builder().externalInput(PrismPostProcessExecutor.SCENE_COLOR);
        for (int i = 0; i < PrismPostProcessGraph.MAX_PASSES + 1; i++) {
            String output = "planet:out_" + i;
            String input = i == 0 ? PrismPostProcessExecutor.SCENE_COLOR : "planet:out_" + (i - 1);
            builder.pass(new PrismPostProcessGraph.Pass("planet:pass_" + i,
                    "planet:program_" + i, List.of(input), output));
        }
        assertThrows(IllegalArgumentException.class, () -> builder.output("planet:out_32").build());
    }
}
