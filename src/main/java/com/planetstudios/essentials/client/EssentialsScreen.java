package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.diagnostics.PerformanceMonitor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Reworked Space Client dashboard with a persistent identity rail and modular feature cards. */
public final class EssentialsScreen extends Screen {
    private final Screen parent;
    private static final int ACCENT = 0xFF56C8FF;
    private static final int TEXT = 0xFFF2F7FF;
    private static final int MUTED = 0xFF91A5BD;

    public EssentialsScreen(Screen parent) {
        super(Component.literal("SPACE CLIENT"));
        this.parent = parent;
    }

    @Override protected void init() {
        int panelWidth = Math.min(900, width - 32);
        int left = (width - panelWidth) / 2;
        int railWidth = 174;
        int contentLeft = left + railWidth + 22;
        int contentWidth = panelWidth - railWidth - 42;
        int gap = 12;
        int cardWidth = (contentWidth - gap) / 2;
        int top = 104;
        addCard("SETTINGS", "Vanilla + Planet options", contentLeft, top, cardWidth,
                () -> minecraft.setScreen(new SettingsSearchScreen(this)));
        addCard("HUD EDITOR", "Arrange your interface", contentLeft + cardWidth + gap, top, cardWidth,
                () -> minecraft.setScreen(new HudEditorScreen(this)));
        addCard("KEYBINDS", "Find and manage controls", contentLeft, top + 62, cardWidth,
                () -> minecraft.setScreen(new KeybindSearchScreen(this)));
        addCard("PERFORMANCE", "Frame pacing, presets and benchmarks", contentLeft + cardWidth + gap, top + 62, cardWidth,
                () -> minecraft.setScreen(new PerformanceScreen(this)));
        addCard("SYSTEMS", "Client-side optimizations", contentLeft, top + 124, cardWidth,
                () -> minecraft.setScreen(new SettingsCategoryScreen(this, "systems")));
        addCard("PRISM", "Shader engine and packs", contentLeft + cardWidth + gap, top + 124, cardWidth,
                () -> minecraft.setScreen(new ShaderSettingsScreen(this)));
        addCard("PROFILES", "Save and switch configs", contentLeft, top + 186, cardWidth,
                () -> minecraft.setScreen(new ProfilesScreen(this)));
        addCard("PRIORITIES", "Feature scheduling", contentLeft + cardWidth + gap, top + 186, cardWidth,
                () -> minecraft.setScreen(new FeaturePriorityScreen(this)));
        addCard("COMBAT", "PvP interface features", contentLeft, top + 248, cardWidth,
                () -> minecraft.setScreen(new SettingsCategoryScreen(this, "combat")));
        addCard("RESOURCE PACKS", "Classic order + advanced asset filters",
                contentLeft + cardWidth + gap, top + 248, cardWidth,
                () -> minecraft.setScreen(new ResourcePackSettingsScreen(this)));
    }

    private void addCard(String title, String subtitle, int x, int y, int cardWidth, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(title + "   ›"), button -> action.run())
                .bounds(x, y, cardWidth, 26).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(900, width - 32);
        int panelLeft = (width - panelWidth) / 2;
        int panelTop = 42;
        int panelBottom = height - 28;
        int railWidth = 174;
        int contentLeft = panelLeft + railWidth + 22;
        int contentWidth = panelWidth - railWidth - 42;

        graphics.fill(0, 0, width, height, 0xE9080D17);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xF0111A29);
        graphics.fill(panelLeft, panelTop, panelLeft + 3, panelBottom, ACCENT);
        graphics.fill(panelLeft + 14, panelTop + 14, panelLeft + railWidth - 8, panelBottom - 14, 0xFF0B1220);
        graphics.fill(panelLeft + railWidth, panelTop + 18, panelLeft + railWidth + 1, panelBottom - 18, 0xFF29384C);
        graphics.drawString(font, "SPACE CLIENT", panelLeft + 26, panelTop + 30, TEXT, false);
        graphics.drawString(font, "BY PLANET STUDIOS", panelLeft + 26, panelTop + 46, ACCENT, false);
        graphics.drawString(font, "CONFIGURATION", panelLeft + 26, panelTop + 82, MUTED, false);
        graphics.drawString(font, "One client.", panelLeft + 26, panelTop + 108, TEXT, false);
        graphics.drawString(font, "One settings library.", panelLeft + 26, panelTop + 120, TEXT, false);
        graphics.fill(contentLeft, panelTop + 18, contentLeft + contentWidth, panelTop + 19, 0xFF31445B);
        graphics.drawString(font, "CLIENT CONTROL CENTER", contentLeft, panelTop + 29, TEXT, false);
        graphics.drawString(font, "FEATURES  /  CONFIGURATION  /  PERFORMANCE", contentLeft, panelTop + 45, MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        var config = EssentialsRuntime.config();
        int statusY = panelBottom - 54;
        if (config.showPerformanceScreen && config.diagnosticsEnabled && config.performanceMonitoringEnabled) {
            PerformanceMonitor monitor = EssentialsRuntime.performance();
            graphics.drawString(font, "FPS " + monitor.latestFps()
                    + "   AVG " + String.format(java.util.Locale.ROOT, "%.1f", monitor.averageFps()),
                    panelLeft + 26, statusY, TEXT, false);
        }
        var session = EssentialsRuntime.sessionStatistics().snapshot();
        graphics.drawString(font, (session.active() ? "SESSION " : "LAST SESSION ")
                + formatDuration(session.elapsedSeconds()), panelLeft + 26, panelBottom - 32, MUTED, false);
        graphics.drawString(font, "SPACE CLIENT  /  BY PLANET STUDIOS", contentLeft, panelBottom - 32, MUTED, false);
    }

    private static String formatDuration(long seconds) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", seconds / 3600L,
                (seconds / 60L) % 60L, seconds % 60L);
    }

    @Override public void onClose() {
        EssentialsRuntime.save();
        minecraft.setScreen(parent);
    }
}
