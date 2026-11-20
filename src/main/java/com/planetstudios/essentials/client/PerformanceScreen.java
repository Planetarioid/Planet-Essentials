package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.diagnostics.BottleneckAnalyzer;
import com.planetstudios.essentials.diagnostics.DiagnosticsExporter;
import com.planetstudios.essentials.diagnostics.FrameTimeSample;
import com.planetstudios.essentials.diagnostics.PerformanceMonitor;
import com.planetstudios.essentials.diagnostics.PerformanceSample;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** FPS history graph, frame-pacing analysis, performance presets, and benchmark export. */
public final class PerformanceScreen extends Screen {
    private final Screen parent;
    private String exportStatus = "Benchmark exports stay on this computer.";
    private static final int GRAPH_WIDTH = 340;
    private static final int GRAPH_HEIGHT = 104;
    private List<PerformanceSample> fpsSamples = List.of();
    private List<FrameTimeSample> frameSamples = List.of();
    private BottleneckAnalyzer.Finding finding = BottleneckAnalyzer.Finding.INSUFFICIENT_DATA;
    private double averageFps;
    private double averageFrameTime;
    private double worstFrameTime;
    private double p50;
    private double p95;
    private double p99;

    public PerformanceScreen(Screen parent) {
        super(Component.literal("PERFORMANCE ANALYZER"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int presetY = height - 68;
        addRenderableWidget(Button.builder(Component.literal("COMPETITIVE"), button -> applyPreset(PerformancePresetService.Preset.COMPETITIVE))
                .bounds(center - 150, presetY, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("BALANCED"), button -> applyPreset(PerformancePresetService.Preset.BALANCED))
                .bounds(center - 48, presetY, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("VISUAL"), button -> applyPreset(PerformancePresetService.Preset.VISUAL))
                .bounds(center + 54, presetY, 96, 20).build());

        addRenderableWidget(Button.builder(Component.literal("EXPORT BENCHMARK"), button -> export())
                .bounds(center - 155, height - 38, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("CLEAR HISTORY"), button -> {
            EssentialsRuntime.performance().clear();
            refreshData();
            exportStatus = "Performance history cleared.";
        }).bounds(center + 5, height - 38, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), button -> minecraft.setScreen(parent))
                .bounds(center + 113, height - 38, 70, 20).build());
        refreshData();
    }

    @Override
    public void tick() {
        super.tick();
        refreshData();
    }

    private void refreshData() {
        PerformanceMonitor monitor = EssentialsRuntime.performance();
        fpsSamples = monitor.snapshot();
        frameSamples = monitor.frameTimeSnapshot();
        averageFps = monitor.averageFps();
        averageFrameTime = monitor.averageFrameTimeMillis();
        worstFrameTime = monitor.worstFrameTimeMillis();
        p50 = monitor.frameTimePercentileMillis(0.50);
        p95 = monitor.frameTimePercentileMillis(0.95);
        p99 = monitor.frameTimePercentileMillis(0.99);
        finding = BottleneckAnalyzer.analyze(
                fpsSamples, frameSamples, EssentialsRuntime.diagnosticEvents().snapshot());
    }

    private void applyPreset(PerformancePresetService.Preset preset) {
        PerformancePresetService.apply(minecraft, preset);
        EssentialsRuntime.config().performancePreset = preset.id();
        EssentialsRuntime.save();
        exportStatus = "Applied " + preset.id() + " preset. Review the changed vanilla options if needed.";
    }

    private void export() {
        try {
            Map<String, String> metadata = new LinkedHashMap<>();
            var client = Minecraft.getInstance();
            var config = EssentialsRuntime.config();
            metadata.put("minecraftVersion", client.getLaunchedVersion());
            metadata.put("javaVersion", System.getProperty("java.version", "unknown"));
            metadata.put("javaVm", System.getProperty("java.vm.name", "unknown"));
            metadata.put("javaVendor", System.getProperty("java.vm.vendor", "unknown"));
            metadata.put("operatingSystem", System.getProperty("os.name", "unknown"));
            metadata.put("operatingSystemVersion", System.getProperty("os.version", "unknown"));
            metadata.put("architecture", System.getProperty("os.arch", "unknown"));
            metadata.put("availableProcessors", Integer.toString(Runtime.getRuntime().availableProcessors()));
            metadata.put("maximumJvmHeapBytes", Long.toString(Runtime.getRuntime().maxMemory()));
            metadata.put("fabricLoaderVersion", FabricLoader.getInstance().getModContainer("fabricloader")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
            metadata.put("installedModCount", Integer.toString(FabricLoader.getInstance().getAllMods().size()));
            metadata.put("installedMods", FabricLoader.getInstance().getAllMods().stream()
                    .sorted(java.util.Comparator.comparing(mod -> mod.getMetadata().getId()))
                    .map(mod -> mod.getMetadata().getId() + "@"
                            + mod.getMetadata().getVersion().getFriendlyString())
                    .collect(Collectors.joining(",")));
            metadata.put("selectedResourcePackIds", client.getResourcePackRepository().getSelectedIds()
                    .stream().sorted().collect(Collectors.joining(",")));
            for (VanillaSettingsRegistry.Entry entry : VanillaSettingsRegistry.all(client)) {
                metadata.put("vanillaOption." + entry.id(), String.valueOf(entry.option().get()));
            }
            metadata.put("spaceClient.activeProfile", config.activeProfile);
            metadata.put("spaceClient.performancePreset", config.performancePreset);
            metadata.put("spaceClient.smartChunkSchedulerEnabled", Boolean.toString(config.smartChunkSchedulerEnabled));
            metadata.put("spaceClient.automaticServerProfilesEnabled", Boolean.toString(config.automaticServerProfilesEnabled));
            metadata.put("spaceClient.resourcePackFilterCount", Integer.toString(config.resourcePackCategoryOverrides.size()));
            metadata.put("spaceClient.resourcePackFilters", config.resourcePackCategoryOverrides.entrySet().stream()
                    .map(entry -> entry.getKey() + ":textures=" + entry.getValue().textures
                            + ",sounds=" + entry.getValue().sounds + ",animations=" + entry.getValue().animations)
                    .collect(Collectors.joining(";")));

            Path file = DiagnosticsExporter.exportBenchmark(FabricLoader.getInstance().getConfigDir(),
                    EssentialsRuntime.performance().snapshot(),
                    EssentialsRuntime.performance().frameTimeSnapshot(),
                    EssentialsRuntime.diagnosticEvents().snapshot(), metadata);
            exportStatus = "Exported " + file.getFileName();
        } catch (RuntimeException exception) {
            exportStatus = "Export failed: " + exception.getMessage();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFF4FAFF);

        String summary = String.format(Locale.ROOT,
                "FPS %d  |  AVG %.1f  |  Frame time p50 %.2f ms  |  p95 %.2f ms  |  p99 %.2f ms",
                Minecraft.getInstance().getFps(), averageFps, p50, p95, p99);
        graphics.drawCenteredString(font, summary, width / 2, 38, 0xFFB9D4EC);

        int graphX = (width - GRAPH_WIDTH) / 2;
        int graphY = 66;
        graphics.fill(graphX, graphY, graphX + GRAPH_WIDTH, graphY + GRAPH_HEIGHT, 0xDD0B1422);
        graphics.fill(graphX, graphY, graphX + GRAPH_WIDTH, graphY + 2, 0xFF56C8FF);
        for (int line = 1; line <= 3; line++) {
            int y = graphY + line * GRAPH_HEIGHT / 4;
            graphics.fill(graphX + 1, y, graphX + GRAPH_WIDTH - 1, y + 1, 0xFF29384C);
        }

        if (fpsSamples.size() > 1) {
            int plotWidth = GRAPH_WIDTH - 12;
            int plotHeight = GRAPH_HEIGHT - 14;
            int maxFps = Math.max(120, fpsSamples.stream().mapToInt(PerformanceSample::framesPerSecond).max().orElse(120));
            int count = Math.min(fpsSamples.size(), plotWidth);
            int start = fpsSamples.size() - count;
            for (int i = 0; i < count; i++) {
                int fps = fpsSamples.get(start + i).framesPerSecond();
                int barHeight = (int) Math.round(Math.min(1.0, fps / (double) maxFps) * plotHeight);
                int x = graphX + 6 + i;
                graphics.fill(x, graphY + GRAPH_HEIGHT - 6 - barHeight,
                        x + 1, graphY + GRAPH_HEIGHT - 6, fps < 60 ? 0xFFFFA45B : 0xFF65D6A3);
            }
        } else {
            graphics.drawCenteredString(font, "Collecting FPS samples...", width / 2, graphY + 48, 0xFF8FAFC8);
        }

        graphics.drawCenteredString(font, "FPS HISTORY GRAPH  •  ORANGE = BELOW 60 FPS",
                width / 2, graphY + GRAPH_HEIGHT + 8, 0xFF8FAFC8);
        graphics.drawCenteredString(font, String.format(Locale.ROOT,
                "Average frame %.2f ms  |  Worst %.2f ms  |  FPS samples %d  |  Frame samples %d",
                averageFrameTime, worstFrameTime, fpsSamples.size(), frameSamples.size()),
                width / 2, graphY + GRAPH_HEIGHT + 24, 0xFFB9D4EC);

        graphics.drawCenteredString(font, "BOTTLENECK ESTIMATE: " + finding.description(),
                width / 2, graphY + GRAPH_HEIGHT + 42, 0xFFFFC98B);
        graphics.drawCenteredString(font, "PRESET: " + EssentialsRuntime.config().performancePreset.toUpperCase(Locale.ROOT)
                        + "  •  Presets adjust vanilla settings and can be customized afterward.",
                width / 2, graphY + GRAPH_HEIGHT + 58, 0xFF8FAFC8);
        graphics.drawCenteredString(font, exportStatus, width / 2, height - 84, 0xFF8FAFC8);
        graphics.drawCenteredString(font,
                "Benchmark includes mod versions, vanilla options, resource packs, heap, FPS, frame times and events.",
                width / 2, height - 98, 0xFF8FAFC8);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
