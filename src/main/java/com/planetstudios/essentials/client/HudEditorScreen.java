package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.HudWidgetPlacement;
import com.planetstudios.essentials.hud.HudAnchor;
import com.planetstudios.essentials.hud.HudWidgetDefinition;
import com.planetstudios.essentials.hud.HudWidgetLayout;
import com.planetstudios.essentials.hud.HudWidgetRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Functional first-pass editor for registered Planet HUD widgets. */
public final class HudEditorScreen extends Screen {
    private static final int ACCENT = 0xFF55C7FF;
    private final Screen parent;
    private List<HudWidgetDefinition> widgets = List.of();

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD EDITOR"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        widgets = HudWidgetRegistry.snapshot();
        int rowY = 58;
        int left = Math.max(8, (width - 354) / 2);
        for (HudWidgetDefinition definition : widgets) {
            final String id = definition.id();
            addRenderableWidget(Button.builder(toggleLabel(definition), button -> {
                HudWidgetPlacement placement = placement(definition);
                placement.enabled = !placement.enabled;
                saveAndRefresh();
            }).bounds(left, rowY, 104, 20).build());

            addRenderableWidget(Button.builder(anchorLabel(definition), button -> {
                HudWidgetPlacement placement = placement(definition);
                HudAnchor[] anchors = HudAnchor.values();
                placement.anchor = anchors[(placement.anchor.ordinal() + 1) % anchors.length];
                saveAndRefresh();
            }).bounds(left + 108, rowY, 104, 20).build());

            addRenderableWidget(adjustButton("X-", left + 216, rowY, () -> adjust(id, -5, 0)));
            addRenderableWidget(adjustButton("X+", left + 243, rowY, () -> adjust(id, 5, 0)));
            addRenderableWidget(adjustButton("Y-", left + 270, rowY, () -> adjust(id, 0, -5)));
            addRenderableWidget(adjustButton("Y+", left + 297, rowY, () -> adjust(id, 0, 5)));
            addRenderableWidget(Button.builder(Component.literal("RESET"), button -> {
                EssentialsRuntime.config().hudWidgetPlacements.remove(id);
                EssentialsRuntime.save();
                rebuildWidgets();
            }).bounds(left + 324, rowY, 42, 20).build());
            rowY += 27;
        }

        addRenderableWidget(Button.builder(Component.literal("RESET ALL"), button -> {
            EssentialsRuntime.config().hudWidgetPlacements.clear();
            EssentialsRuntime.save();
            rebuildWidgets();
        }).bounds(width / 2 - 104, height - 34, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), button -> onClose())
                .bounds(width / 2 + 4, height - 34, 100, 20).build());
    }

    private Button adjustButton(String label, int x, int y, Runnable action) {
        return Button.builder(Component.literal(label), button -> action.run())
                .bounds(x, y, 25, 20).build();
    }

    private void adjust(String id, int dx, int dy) {
        HudWidgetPlacement placement = EssentialsRuntime.config().hudWidgetPlacements.get(id);
        if (placement == null) {
            HudWidgetDefinition definition = HudWidgetRegistry.find(id).orElse(null);
            if (definition == null) return;
            placement = placement(definition);
        }
        placement.offsetX += dx;
        placement.offsetY += dy;
        placement.validate();
        saveAndRefresh();
    }

    private HudWidgetPlacement placement(HudWidgetDefinition definition) {
        return EssentialsRuntime.config().hudWidgetPlacements.computeIfAbsent(definition.id(),
                id -> new HudWidgetPlacement(definition.defaultAnchor(),
                        definition.defaultOffsetX(), definition.defaultOffsetY(),
                        definition.enabledByDefault()));
    }

    private Component toggleLabel(HudWidgetDefinition definition) {
        HudWidgetPlacement placement = placement(definition);
        String name = shortName(definition.id());
        return Component.literal((placement.enabled ? "ON  " : "OFF ") + name);
    }

    private Component anchorLabel(HudWidgetDefinition definition) {
        HudWidgetPlacement placement = placement(definition);
        return Component.literal(placement.anchor.name().replace('_', ' '));
    }

    private static String shortName(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        return path.length() > 13 ? path.substring(0, 13) : path;
    }

    private void saveAndRefresh() {
        EssentialsRuntime.save();
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        minecraft.setScreen(new HudEditorScreen(parent));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFF4FAFF);
        graphics.drawCenteredString(font, "Toggle widgets, cycle anchors, and move them in 5-pixel steps.",
                width / 2, 39, 0xFF8FAFC8);

        int previewWidth = Math.min(330, width - 32);
        int previewHeight = 92;
        int previewX = (width - previewWidth) / 2;
        int previewY = Math.max(150, height - 145);
        graphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xDD111B2B);
        graphics.fill(previewX, previewY, previewX + previewWidth, previewY + 2, ACCENT);
        graphics.drawCenteredString(font, "LIVE LAYOUT PREVIEW", width / 2, previewY + 7, 0xFF8FAFC8);

        int innerX = previewX + 8;
        int innerY = previewY + 23;
        int innerWidth = previewWidth - 16;
        int innerHeight = previewHeight - 30;
        for (HudWidgetDefinition definition : widgets) {
            HudWidgetPlacement placement = placement(definition);
            if (!placement.enabled) continue;
            String label = shortName(definition.id());
            int widgetWidth = font.width(label);
            HudWidgetLayout.Position position = HudWidgetLayout.resolve(placement.anchor,
                    innerWidth, innerHeight, widgetWidth, font.lineHeight,
                    placement.offsetX / 4, placement.offsetY / 4);
            graphics.drawString(font, label, innerX + position.x(), innerY + position.y(),
                    0xFF70D7FF, true);
        }

        graphics.drawCenteredString(font, "Offsets: " + widgets.size()
                + " registered widgets  |  Changes save automatically",
                width / 2, previewY + previewHeight + 8, 0xFF8FAFC8);
    }

    @Override
    public void onClose() {
        EssentialsRuntime.save();
        minecraft.setScreen(parent);
    }
}
