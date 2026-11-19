package com.planetstudios.essentials.rendering.shaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Immutable, validated post-processing dependency graph.
 *
 * Inputs declared as external are supplied by the renderer (for example, the scene
 * color texture). Every other input must be produced by an earlier dependency in
 * the graph. Pass order is topologically sorted and deterministic.
 */
public final class PrismPostProcessGraph {
    public record Pass(String id, String programId, List<String> inputs, String output) {
        public Pass {
            requireId(id, "pass");
            requireId(programId, "program");
            requireId(output, "output");
            Objects.requireNonNull(inputs, "inputs");
            if (inputs.isEmpty()) throw new IllegalArgumentException("A post-process pass needs at least one input");
            Set<String> unique = new LinkedHashSet<>();
            for (String input : inputs) {
                requireId(input, "input");
                if (!unique.add(input)) throw new IllegalArgumentException("Duplicate pass input: " + input);
            }
            if (unique.contains(output)) {
                throw new IllegalArgumentException("A post-process pass cannot read and write the same resource: " + output);
            }
            inputs = List.copyOf(unique);
        }
    }

    private final Set<String> externalInputs;
    private final List<Pass> orderedPasses;
    private final String output;

    private PrismPostProcessGraph(Set<String> externalInputs, List<Pass> orderedPasses, String output) {
        this.externalInputs = Set.copyOf(externalInputs);
        this.orderedPasses = List.copyOf(orderedPasses);
        this.output = output;
    }

    public static Builder builder() { return new Builder(); }
    public Set<String> externalInputs() { return externalInputs; }
    public List<Pass> passes() { return orderedPasses; }
    public String output() { return output; }

    public void execute(Consumer<Pass> executor) {
        Objects.requireNonNull(executor, "executor");
        for (Pass pass : orderedPasses) executor.accept(pass);
    }

    private static void requireId(String value, String kind) {
        if (value == null || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                || value.contains("..") || value.contains("//")) {
            throw new IllegalArgumentException("Invalid Prism " + kind + " resource id: " + value);
        }
    }

    public static final class Builder {
        private final Set<String> externalInputs = new LinkedHashSet<>();
        private final List<Pass> passes = new ArrayList<>();
        private String output;

        public Builder externalInput(String resourceId) {
            requireId(resourceId, "external input");
            if (!externalInputs.add(resourceId)) throw new IllegalArgumentException("Duplicate external input: " + resourceId);
            return this;
        }

        public Builder pass(Pass pass) {
            passes.add(Objects.requireNonNull(pass, "pass"));
            return this;
        }

        public Builder output(String resourceId) {
            requireId(resourceId, "graph output");
            output = resourceId;
            return this;
        }

        public PrismPostProcessGraph build() {
            if (passes.isEmpty()) throw new IllegalArgumentException("Post-process graph must contain at least one pass");
            if (output == null) throw new IllegalArgumentException("Post-process graph output is not declared");

            Map<String, Pass> producerByResource = new LinkedHashMap<>();
            Set<String> passIds = new HashSet<>();
            for (Pass pass : passes) {
                if (!passIds.add(pass.id())) throw new IllegalArgumentException("Duplicate post-process pass id: " + pass.id());
                if (externalInputs.contains(pass.output())) throw new IllegalArgumentException(
                        "Pass output collides with external input: " + pass.output());
                if (producerByResource.putIfAbsent(pass.output(), pass) != null) {
                    throw new IllegalArgumentException("Multiple passes produce resource: " + pass.output());
                }
            }

            if (!externalInputs.contains(output) && !producerByResource.containsKey(output)) {
                throw new IllegalArgumentException("Graph output has no producer: " + output);
            }

            List<Pass> ordered = new ArrayList<>();
            Set<String> completed = new HashSet<>(externalInputs);
            Set<Pass> remaining = new LinkedHashSet<>(passes);
            while (!remaining.isEmpty()) {
                boolean progressed = false;
                var iterator = remaining.iterator();
                while (iterator.hasNext()) {
                    Pass pass = iterator.next();
                    boolean ready = pass.inputs().stream().allMatch(completed::contains);
                    if (!ready) continue;
                    ordered.add(pass);
                    completed.add(pass.output());
                    iterator.remove();
                    progressed = true;
                }
                if (!progressed) {
                    Set<String> missing = new LinkedHashSet<>();
                    for (Pass pass : remaining) {
                        for (String input : pass.inputs()) {
                            if (!completed.contains(input)) missing.add(input);
                        }
                    }
                    throw new IllegalArgumentException("Post-process graph has unresolved inputs or a cycle: " + missing);
                }
            }
            return new PrismPostProcessGraph(externalInputs, ordered, output);
        }
    }
}
