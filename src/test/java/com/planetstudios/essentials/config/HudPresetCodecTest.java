package com.planetstudios.essentials.config;

import com.planetstudios.essentials.hud.HudAnchor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HudPresetCodecTest {
    @Test void roundTripsWidgetPlacements() {
        Map<String, HudWidgetPlacement> original = Map.of(
                "planet_essentials:keystrokes",
                new HudWidgetPlacement(HudAnchor.BOTTOM_LEFT, 12, -24, true));
        Map<String, HudWidgetPlacement> decoded = HudPresetCodec.decode(HudPresetCodec.encode(original));
        assertEquals(HudAnchor.BOTTOM_LEFT, decoded.get("planet_essentials:keystrokes").anchor);
        assertEquals(12, decoded.get("planet_essentials:keystrokes").offsetX);
        assertEquals(-24, decoded.get("planet_essentials:keystrokes").offsetY);
    }

    @Test void rejectsUnrecognizedPresetFormat() {
        assertThrows(IllegalArgumentException.class, () -> HudPresetCodec.decode("{\"format\":\"other\"}"));
    }
}
