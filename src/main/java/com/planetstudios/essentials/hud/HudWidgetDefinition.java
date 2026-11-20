package com.planetstudios.essentials.hud;

/**
 * Immutable metadata for a HUD widget. Rendering remains owned by the client-side
 * widget implementation; this contract centralizes identity, ordering and placement.
 */
public record HudWidgetDefinition(
        String id,
        HudAnchor defaultAnchor,
        int defaultOffsetX,
        int defaultOffsetY,
        int priority,
        boolean enabledByDefault
) {
    public HudWidgetDefinition {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Widget id must use namespace:path format");
        }
        if (defaultAnchor == null) {
            throw new IllegalArgumentException("Widget anchor is required");
        }
    }
}
