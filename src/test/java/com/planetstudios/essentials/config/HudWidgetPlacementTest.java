package com.planetstudios.essentials.config;

import com.planetstudios.essentials.hud.HudAnchor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudWidgetPlacementTest {
    @Test
    void validatesAnchorAndClampsOffsets() {
        HudWidgetPlacement placement = new HudWidgetPlacement(null, 9000, -9000, false);
        assertEquals(HudAnchor.TOP_LEFT, placement.anchor);
        assertEquals(2000, placement.offsetX);
        assertEquals(-2000, placement.offsetY);
        assertTrue(!placement.enabled);
    }

    @Test
    void preservesValidPlacement() {
        HudWidgetPlacement placement = new HudWidgetPlacement(
                HudAnchor.BOTTOM_RIGHT, 35, -20, true);
        assertEquals(HudAnchor.BOTTOM_RIGHT, placement.anchor);
        assertEquals(35, placement.offsetX);
        assertEquals(-20, placement.offsetY);
    }
}
