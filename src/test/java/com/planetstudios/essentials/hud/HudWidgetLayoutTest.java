package com.planetstudios.essentials.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HudWidgetLayoutTest {
    @Test
    void resolvesTopCenterWithOffsets() {
        assertEquals(new HudWidgetLayout.Position(450, 22),
                HudWidgetLayout.resolve(HudAnchor.TOP_CENTER, 1000, 600,
                        100, 20, 0, 22));
    }

    @Test
    void resolvesBottomRightAndCenterAnchors() {
        assertEquals(new HudWidgetLayout.Position(880, 560),
                HudWidgetLayout.resolve(HudAnchor.BOTTOM_RIGHT, 1000, 600,
                        100, 20, -20, -20));
        assertEquals(new HudWidgetLayout.Position(450, 290),
                HudWidgetLayout.resolve(HudAnchor.CENTER, 1000, 600,
                        100, 20, 0, 0));
    }

    @Test
    void rejectsNegativeDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> HudWidgetLayout.resolve(HudAnchor.TOP_LEFT, 100, 100,
                        -1, 10, 0, 0));
    }
}
