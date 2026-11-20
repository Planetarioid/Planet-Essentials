package com.planetstudios.essentials.hud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HudWidgetRegistryTest {
    @AfterEach
    void clearRegistry() {
        HudWidgetRegistry.clearForTests();
    }

    @Test
    void registersAndFindsWidgetByStableId() {
        HudWidgetDefinition definition = new HudWidgetDefinition(
                "planet:combo_counter", HudAnchor.TOP_CENTER, 0, 12, 50, true);

        HudWidgetRegistry.register(definition);

        assertEquals(definition, HudWidgetRegistry.find("planet:combo_counter").orElseThrow());
    }

    @Test
    void rejectsDuplicateIds() {
        HudWidgetDefinition definition = new HudWidgetDefinition(
                "planet:combo_counter", HudAnchor.TOP_CENTER, 0, 12, 50, true);
        HudWidgetRegistry.register(definition);

        assertThrows(IllegalStateException.class, () -> HudWidgetRegistry.register(definition));
    }

    @Test
    void snapshotUsesPriorityThenStableIdAndCannotBeMutated() {
        HudWidgetRegistry.register(new HudWidgetDefinition(
                "planet:zeta", HudAnchor.TOP_LEFT, 0, 0, 1, true));
        HudWidgetRegistry.register(new HudWidgetDefinition(
                "planet:alpha", HudAnchor.TOP_LEFT, 0, 0, 10, true));
        HudWidgetRegistry.register(new HudWidgetDefinition(
                "planet:beta", HudAnchor.TOP_LEFT, 0, 0, 10, true));

        List<HudWidgetDefinition> snapshot = HudWidgetRegistry.snapshot();

        assertEquals(List.of("planet:alpha", "planet:beta", "planet:zeta"),
                snapshot.stream().map(HudWidgetDefinition::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(
                new HudWidgetDefinition("planet:other", HudAnchor.CENTER, 0, 0, 0, true)));
    }

    @Test
    void rejectsInvalidWidgetIdsAndMissingAnchors() {
        assertThrows(IllegalArgumentException.class,
                () -> new HudWidgetDefinition("ComboCounter", HudAnchor.TOP_CENTER, 0, 0, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> new HudWidgetDefinition("planet:combo", null, 0, 0, 0, true));
    }
}
