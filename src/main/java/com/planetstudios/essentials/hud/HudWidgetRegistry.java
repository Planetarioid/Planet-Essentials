package com.planetstudios.essentials.hud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Process-wide registry for Planet-owned and explicitly integrated HUD widgets. */
public final class HudWidgetRegistry {
    private static final Map<String, HudWidgetDefinition> WIDGETS = new LinkedHashMap<>();
    private static volatile List<HudWidgetDefinition> orderedSnapshot = List.of();

    private HudWidgetRegistry() {}

    public static synchronized void register(HudWidgetDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("Widget definition is required");
        if (WIDGETS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate HUD widget id: " + definition.id());
        }
        refreshSnapshot();
    }

    public static synchronized Optional<HudWidgetDefinition> find(String id) {
        return Optional.ofNullable(WIDGETS.get(id));
    }

    /** Returns an immutable snapshot ordered by descending priority, then stable id. */
    public static List<HudWidgetDefinition> snapshot() {
        return orderedSnapshot;
    }

    private static void refreshSnapshot() {
        List<HudWidgetDefinition> result = new ArrayList<>(WIDGETS.values());
        result.sort(Comparator.comparingInt(HudWidgetDefinition::priority).reversed()
                .thenComparing(HudWidgetDefinition::id));
        orderedSnapshot = List.copyOf(result);
    }

    public static synchronized Collection<String> ids() {
        return List.copyOf(WIDGETS.keySet());
    }

    /** Test-only isolation hook; production code should register widgets once at startup. */
    static synchronized void clearForTests() {
        WIDGETS.clear();
        orderedSnapshot = List.of();
    }
}
