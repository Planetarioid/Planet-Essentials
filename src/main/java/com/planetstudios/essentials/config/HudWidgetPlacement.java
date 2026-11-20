package com.planetstudios.essentials.config;

import com.planetstudios.essentials.hud.HudAnchor;

/** Persisted user placement and visibility for one HUD widget. */
public final class HudWidgetPlacement {
    public HudAnchor anchor;
    public int offsetX;
    public int offsetY;
    public boolean enabled = true;

    public HudWidgetPlacement() {}

    public HudWidgetPlacement(HudAnchor anchor, int offsetX, int offsetY, boolean enabled) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.enabled = enabled;
        validate();
    }

    public void validate() {
        if (anchor == null) anchor = HudAnchor.TOP_LEFT;
        offsetX = Math.max(-2000, Math.min(2000, offsetX));
        offsetY = Math.max(-2000, Math.min(2000, offsetY));
    }
}
