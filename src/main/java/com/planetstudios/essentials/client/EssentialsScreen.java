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

    @Override
    protected void init() {
        int center = width / 2;
        int top = Math.max(54, height / 4 - 12);
        int buttonWidth = Math.min(320, width - 48);
        int left = center - buttonWidth / 2;

        addRenderableWidget(clientButton("Minecraft Settings", left, top + 46,
                buttonWidth, () -> VanillaOptionsAccess.open(minecraft, this)));
        addRenderableWidget(clientButton("Performance  /  Diagnostics", left, top + 73,
                buttonWidth, () -> minecraft.setScreen(new SettingsCategoryScreen(this, "diagnostics"))));
        addRenderableWidget(clientButton("System Controls", left, top + 100,
                buttonWidth, () -> minecraft.setScreen(new SettingsCategoryScreen(this, "systems"))));
        addRenderableWidget(clientButton("Prism  /  Shader Engine", left, top + 127,
                buttonWidth, () -> minecraft.setScreen(new ShaderSettingsScreen(this))));
        addRenderableWidget(clientButton("Configuration Profiles", left, top + 154,
                buttonWidth, () -> minecraft.setScreen(new ProfilesScreen(this))));
        addRenderableWidget(clientButton("Feature Priorities", left, top + 181,
                buttonWidth, () -> minecraft.setScreen(new FeaturePriorityScreen(this))));
        addRenderableWidget(Button.builder(Component.literal("CLOSE"), button -> onClose())
                .bounds(center - 70, top + 215, 140, 20).build());
    }

    private Button clientButton(String label, int x, int y, int width, Runnable action) {
        return Button.builder(Component.literal(label), button -> action.run())
                .bounds(x, y, width, 22).build();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int center = width / 2;
        int top = Math.max(72, height / 4 - 12);
        int panelWidth = Math.min(380, width - 24);
        int left = center - panelWidth / 2;
        int accent = 0xFF55C7FF;
        int textColor = 0xFFF4FAFF;
        int panel = 0xEE111B2B;
        graphics.fill(0, 0, width, height, 0xB5080D18);
        graphics.fill(left, top - 26, left + panelWidth, top + 272, panel);
        graphics.fill(left, top - 26, left + panelWidth, top - 23, accent);
        graphics.fill(left + 12, top + 28, left + panelWidth - 12, top + 29, accent & 0x55FFFFFF);
        graphics.drawCenteredString(font, title, center, top - 12, textColor);
        graphics.drawCenteredString(font, "MINECRAFT  /  SYSTEMS  /  RENDERING", center, top + 8,
                0xFF8FAFC8);
        super.render(graphics, mouseX, mouseY, partialTick);

        var config = EssentialsRuntime.config();
        int infoY = top + 280;
        if (config.showPerformanceScreen && config.diagnosticsEnabled && config.performanceMonitoringEnabled) {
            PerformanceMonitor monitor = EssentialsRuntime.performance();
            graphics.drawCenteredString(font, "FPS  " + monitor.latestFps()
                    + "     AVG  " + String.format(java.util.Locale.ROOT, "%.1f", monitor.averageFps()),
                    center, infoY, textColor);
            infoY += 13;
            var samples = monitor.snapshot();
            var latestSample = samples.isEmpty() ? null : samples.get(samples.size() - 1);
            if (latestSample != null) {
                graphics.drawCenteredString(font, "JAVA HEAP  " + formatBytes(latestSample.usedHeapBytes())
                        + " / " + formatBytes(latestSample.maxHeapBytes()), center, infoY, 0xFF8FAFC8);
                infoY += 13;
            }
        }
        var session = EssentialsRuntime.sessionStatistics().snapshot();
        String sessionLabel = session.active() ? "SESSION" : "LAST SESSION";
        graphics.drawCenteredString(font, sessionLabel + "  " + formatDuration(session.elapsedSeconds())
                        + "     DISTANCE  " + String.format(java.util.Locale.ROOT, "%.1f blocks",
                        session.distanceTravelled()),
                center, infoY, 0xFF8FAFC8);
        infoY += 13;
        graphics.drawCenteredString(font, "SETTINGS REGISTERED  " + SettingsRegistry.all().size(),
                center, infoY, 0xFF71869D);
        graphics.drawCenteredString(font, "PLANET STUDIOS  •  CLIENT CONFIGURATION",
                center, height - 14, 0xFF52677F);
    }

    private static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kib = bytes / 1024.0;
        if (kib < 1024.0) {
            return String.format(java.util.Locale.ROOT, "%.1f KiB", kib);
        }
        double mib = kib / 1024.0;
        if (mib < 1024.0) {
            return String.format(java.util.Locale.ROOT, "%.1f MiB", mib);
        }
        return String.format(java.util.Locale.ROOT, "%.2f GiB", mib / 1024.0);
    }

    @Override
    public void onClose() {
        EssentialsRuntime.save();
        minecraft.setScreen(parent);
    }
}
