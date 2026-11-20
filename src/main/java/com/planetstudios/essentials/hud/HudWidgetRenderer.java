package com.planetstudios.essentials.hud;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.combat.ComboCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Client HUD dispatch layer. Widget content is evaluated only while the HUD is rendered. */
public final class HudWidgetRenderer {
    public record Content(String text, int color) {
        public Content {
            if (text == null || text.isBlank()) throw new IllegalArgumentException("HUD text is required");
        }
    }

    private static final ComboCounter COMBO_COUNTER = new ComboCounter();
    private static final Map<String, Function<Minecraft, Content>> CONTENT = new HashMap<>();

    static {
        register(new HudWidgetDefinition("planet_essentials:performance_fps",
                HudAnchor.TOP_LEFT, 6, 6, 100, true),
                client -> EssentialsRuntime.config().showPerformanceScreen
                        ? new Content("FPS: " + client.getFps(), 0xFFFFFFFF) : null);
        register(new HudWidgetDefinition("planet_essentials:combo_counter",
                HudAnchor.BOTTOM_CENTER, 0, -40, 90, true),
                client -> {
                    if (!EssentialsRuntime.config().comboCounterEnabled) return null;
                    int value = COMBO_COUNTER.value();
                    return value == 0 ? null : new Content("COMBO " + value,
                            value > 0 ? 0xFF70E090 : 0xFFFF7070);
                });
    }

    private HudWidgetRenderer() {}

    private static void register(HudWidgetDefinition definition,
                                 Function<Minecraft, Content> contentProvider) {
        HudWidgetRegistry.register(definition);
        CONTENT.put(definition.id(), contentProvider);
    }

    /** Called from the vanilla HUD render tail on the render thread. */
    public static void render(GuiGraphics graphics, Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) return;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        for (HudWidgetDefinition definition : HudWidgetRegistry.snapshot()) {
            Function<Minecraft, Content> provider = CONTENT.get(definition.id());
            if (provider == null) continue;
            Content content = provider.apply(client);
            if (content == null) continue;

            int width = client.font.width(content.text());
            int height = client.font.lineHeight;
            HudWidgetLayout.Position position = HudWidgetLayout.resolve(
                    definition.defaultAnchor(), screenWidth, screenHeight,
                    width, height, definition.defaultOffsetX(), definition.defaultOffsetY());
            graphics.drawString(client.font, content.text(), position.x(), position.y(),
                    content.color(), true);
        }
    }

    /** Shared state entry point for confirmed combat event listeners. */
    public static ComboCounter comboCounter() {
        return COMBO_COUNTER;
    }
}
