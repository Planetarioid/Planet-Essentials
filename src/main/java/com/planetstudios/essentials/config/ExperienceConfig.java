package com.planetstudios.essentials.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User-facing presentation and HUD customization owned by Planet Essentials.
 * Provider-owned settings are deliberately not stored here.
 */
public final class ExperienceConfig {
    public String theme = "midnight";
    public String accent = "cyan";
    public String background = "gradient";
    public float uiScale = 1.0f;
    public float panelOpacity = 0.92f;
    public boolean animations = true;
    public boolean compactLayout = false;
    public boolean showFpsWidget = true;
    public boolean showPingWidget = true;
    public boolean showFrameTimeWidget = false;
    public boolean showMemoryWidget = false;
    public boolean showCpuWidget = false;
    public boolean showGpuWidget = false;
    public final Map<String, HudWidget> hudWidgets = new LinkedHashMap<>();

    public void validate() {
        if (!oneOf(theme, "midnight", "obsidian", "light")) theme = "midnight";
        if (!oneOf(accent, "cyan", "violet", "lime", "amber", "rose")) accent = "cyan";
        if (!oneOf(background, "gradient", "solid", "dimmed")) background = "gradient";
        if (!Float.isFinite(uiScale)) uiScale = 1.0f;
        uiScale = clamp(uiScale, 0.75f, 1.5f);
        if (!Float.isFinite(panelOpacity)) panelOpacity = 0.92f;
        panelOpacity = clamp(panelOpacity, 0.45f, 1.0f);
        if (hudWidgets == null) {
            // Gson can assign null when a user-edited config explicitly contains null.
            // The field is replaced through the helper below to keep the config recoverable.
            return;
        }
        hudWidgets.entrySet().removeIf(entry -> entry.getKey() == null
                || !entry.getKey().matches("[a-z0-9_.-]{1,64}") || entry.getValue() == null);
        hudWidgets.values().forEach(HudWidget::validate);
    }

    private static boolean oneOf(String value, String... allowed) {
        if (value == null) return false;
        for (String candidate : allowed) if (candidate.equals(value)) return true;
        return false;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Per-widget layout settings, ready for the unified HUD renderer. */
    public static final class HudWidget {
        public boolean visible = true;
        public float x = 0.02f;
        public float y = 0.02f;
        public float scale = 1.0f;
        public float opacity = 1.0f;
        public String anchor = "top_left";

        public void validate() {
            if (!Float.isFinite(x)) x = 0.02f;
            if (!Float.isFinite(y)) y = 0.02f;
            if (!Float.isFinite(scale)) scale = 1.0f;
            if (!Float.isFinite(opacity)) opacity = 1.0f;
            x = clamp(x, 0.0f, 1.0f);
            y = clamp(y, 0.0f, 1.0f);
            scale = clamp(scale, 0.5f, 2.0f);
            opacity = clamp(opacity, 0.1f, 1.0f);
            if (!oneOf(anchor, "top_left", "top_right", "bottom_left", "bottom_right", "center")) {
                anchor = "top_left";
            }
        }
    }
}
