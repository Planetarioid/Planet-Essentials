package com.planetstudios.essentials.hud;

/** Pure placement math shared by HUD widgets and future layout editors. */
public final class HudWidgetLayout {
    private HudWidgetLayout() {}

    public static Position resolve(HudAnchor anchor, int screenWidth, int screenHeight,
                                   int widgetWidth, int widgetHeight, int offsetX, int offsetY) {
        if (anchor == null) throw new IllegalArgumentException("Anchor is required");
        if (screenWidth < 0 || screenHeight < 0 || widgetWidth < 0 || widgetHeight < 0) {
            throw new IllegalArgumentException("Screen and widget dimensions cannot be negative");
        }

        int baseX = switch (anchor) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (screenWidth - widgetWidth) / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenWidth - widgetWidth;
        };
        int baseY = switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> (screenHeight - widgetHeight) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - widgetHeight;
        };
        return new Position(baseX + offsetX, baseY + offsetY);
    }

    public record Position(int x, int y) {}
}
