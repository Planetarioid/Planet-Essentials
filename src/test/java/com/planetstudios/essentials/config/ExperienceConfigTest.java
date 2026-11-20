package com.planetstudios.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExperienceConfigTest {
    @Test
    void validatesThemeAndPresentationBounds() {
        ExperienceConfig config = new ExperienceConfig();
        config.theme = "not-a-theme";
        config.accent = "ultraviolet";
        config.uiScale = 9.0f;
        config.panelOpacity = Float.NaN;

        config.validate();

        assertEquals("midnight", config.theme);
        assertEquals("cyan", config.accent);
        assertEquals(1.5f, config.uiScale);
        assertEquals(0.92f, config.panelOpacity);
    }

    @Test
    void validatesAndRecoversHudWidgetLayout() {
        ExperienceConfig config = new ExperienceConfig();
        ExperienceConfig.HudWidget widget = new ExperienceConfig.HudWidget();
        widget.x = -2.0f;
        widget.y = Float.POSITIVE_INFINITY;
        widget.scale = 8.0f;
        widget.opacity = 0.0f;
        widget.anchor = "outside";
        config.hudWidgets.put("fps", widget);
        config.hudWidgets.put("../invalid", new ExperienceConfig.HudWidget());

        config.validate();

        assertEquals(1, config.hudWidgets.size());
        assertEquals(0.0f, widget.x);
        assertEquals(0.02f, widget.y);
        assertEquals(2.0f, widget.scale);
        assertEquals(0.1f, widget.opacity);
        assertEquals("top_left", widget.anchor);
    }

    @Test
    void recoversNullHudWidgetMap() {
        ExperienceConfig config = new ExperienceConfig();
        config.hudWidgets = null;
        assertDoesNotThrow(config::validate);
        assertNotNull(config.hudWidgets);
    }
}
