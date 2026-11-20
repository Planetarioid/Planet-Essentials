package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.SettingsRegistry;
import com.planetstudios.essentials.diagnostics.PerformanceMonitor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Planet client-style settings hub. */
public final class EssentialsScreen extends Screen {
    private final Screen parent;

    public EssentialsScreen(Screen parent) {
        super(Component.literal("PLANET CLIENT"));
        this.parent = parent;
    }

    @Override protected void init() {
        int center = width / 2;
        int top = Math.max(72, height / 4 - 12);
        int buttonWidth = Math.min(320, width - 48);
        int left = center - buttonWidth / 2;
        addRenderableWidget(clientButton("SETTINGS", left, top + 40, buttonWidth,
                () -> minecraft.setScreen(new SettingsSearchScreen(this))));
        addRenderableWidget(clientButton("HUD EDITOR", left, top + 64, buttonWidth,
                () -> minecraft.setScreen(new HudEditorScreen(this))));
        addRenderableWidget(clientButton("SEARCH KEYBINDS", left, top + 88, buttonWidth,
                () -> minecraft.setScreen(new KeybindSearchScreen(this))));
        addRenderableWidget(clientButton("PERFORMANCE  /  DIAGNOSTICS", left, top + 112, buttonWidth,
                () -> minecraft.setScreen(new SettingsCategoryScreen(this, "diagnostics"))));
        addRenderableWidget(clientButton("SYSTEM CONTROLS", left, top + 136, buttonWidth,
                () -> minecraft.setScreen(new SettingsCategoryScreen(this, "systems"))));
        addRenderableWidget(clientButton("PRISM  /  SHADER ENGINE", left, top + 160, buttonWidth,
                () -> minecraft.setScreen(new ShaderSettingsScreen(this))));
        addRenderableWidget(clientButton("CONFIGURATION PROFILES", left, top + 184, buttonWidth,
                () -> minecraft.setScreen(new ProfilesScreen(this))));
        addRenderableWidget(clientButton("FEATURE PRIORITIES", left, top + 208, buttonWidth,
                () -> minecraft.setScreen(new FeaturePriorityScreen(this))));
        addRenderableWidget(clientButton("COMBAT FEATURES", left, top + 232, buttonWidth,
                () -> minecraft.setScreen(new SettingsCategoryScreen(this, "combat"))));
        addRenderableWidget(Button.builder(Component.literal("CLOSE"), button -> onClose())
                .bounds(center - 70, top + 259, 140, 20).build());
    }

    private Button clientButton(String label, int x, int y, int width, Runnable action) {
        return Button.builder(Component.literal(label), button -> action.run()).bounds(x, y, width, 22).build();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int center = width / 2;
        int top = Math.max(72, height / 4 - 12);
        int panelWidth = Math.min(380, width - 24);
        int left = center - panelWidth / 2;
        int accent = 0xFF55C7FF;
        int textColor = 0xFFF4FAFF;
        graphics.fill(0, 0, width, height, 0xB5080D18);
        graphics.fill(left, top - 26, left + panelWidth, top + 318, 0xEE111B2B);
        graphics.fill(left, top - 26, left + panelWidth, top - 23, accent);
        graphics.fill(left + 12, top + 28, left + panelWidth - 12, top + 29, accent & 0x55FFFFFF);
        graphics.drawCenteredString(font, title, center, top - 12, textColor);
        graphics.drawCenteredString(font, "ONE SETTINGS LIBRARY  /  PLANET STUDIOS", center, top + 8, 0xFF8FAFC8);
        super.render(graphics, mouseX, mouseY, partialTick);
        var config = EssentialsRuntime.config();
        int infoY = top + 326;
        if (config.showPerformanceScreen && config.diagnosticsEnabled && config.performanceMonitoringEnabled) {
            PerformanceMonitor monitor = EssentialsRuntime.performance();
            graphics.drawCenteredString(font, "FPS  " + monitor.latestFps()
                    + "     AVG  " + String.format(java.util.Locale.ROOT, "%.1f", monitor.averageFps()),
                    center, infoY, textColor);
            infoY += 13;
        }
        var session = EssentialsRuntime.sessionStatistics().snapshot();
        graphics.drawCenteredString(font, (session.active() ? "SESSION" : "LAST SESSION") + "  "
                + formatDuration(session.elapsedSeconds()) + "     DISTANCE  "
                + String.format(java.util.Locale.ROOT, "%.1f blocks", session.distanceTravelled()),
                center, infoY, 0xFF8FAFC8);
        graphics.drawCenteredString(font, "PLANET STUDIOS  •  CLIENT CONFIGURATION", center, height - 14, 0xFF52677F);
    }

    private static String formatDuration(long totalSeconds) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", totalSeconds / 3600L,
                (totalSeconds % 3600L) / 60L, totalSeconds % 60L);
    }

    @Override public void onClose() {
        EssentialsRuntime.save();
        minecraft.setScreen(parent);
    }
}
