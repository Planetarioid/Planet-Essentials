package com.planetstudios.essentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Search, inspect, and rebind vanilla and mod key mappings. */
public final class KeybindSearchScreen extends Screen {
    private final Screen parent;
    private EditBox search;
    private KeyMapping listening;
    private List<KeyMapping> matches = List.of();
    private int page;
    private static final int PAGE_SIZE = 9;
    private final List<Button> resultButtons = new ArrayList<>();

    public KeybindSearchScreen(Screen parent) {
        super(Component.literal("KEYBIND SEARCH"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        search = new EditBox(font, width / 2 - 150, 42, 300, 20, Component.literal("Search keybinds"));
        search.setHint(Component.literal("Action, category, or assigned key..."));
        search.setMaxLength(80);
        search.setResponder(value -> {
            page = 0;
            rebuildRows();
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("CLEAR"), button -> search.setValue(""))
                .bounds(width / 2 + 156, 42, 54, 20).build());
        rebuildRows();
        addRenderableWidget(Button.builder(Component.literal("PREVIOUS"), button -> {
            page = Math.max(0, page - 1);
            rebuildRows();
        }).bounds(width / 2 - 110, height - 34, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("NEXT"), button -> {
            if ((page + 1) * PAGE_SIZE < matches.size()) page++;
            rebuildRows();
        }).bounds(width / 2 + 10, height - 34, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 - 50, height - 60, 100, 20).build());
    }

    private void rebuildRows() {
        matches = Arrays.stream(minecraft.options.keyMappings)
                .filter(this::matches)
                .sorted(Comparator.comparing((KeyMapping mapping) -> String.valueOf(mapping.getCategory()))
                        .thenComparing(KeyMapping::getName))
                .toList();
        for (Button button : resultButtons) removeWidget(button);
        resultButtons.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(matches.size(), start + PAGE_SIZE);
        int y = 75;
        for (int i = start; i < end; i++) {
            KeyMapping mapping = matches.get(i);
            boolean conflict = hasConflict(mapping);
            String label = (listening == mapping ? "PRESS A KEY...  "
                    : mapping.getName() + "  /  " + mapping.getTranslatedKeyMessage().getString())
                    + (conflict ? "  [CONFLICT]" : "");
            Button button = Button.builder(Component.literal(label), ignored -> {
                listening = mapping;
                rebuildRows();
            }).bounds(width / 2 - 190, y, 380, 22).build();
            resultButtons.add(button);
            addRenderableWidget(button);
            y += 25;
        }
    }

    private boolean matches(KeyMapping mapping) {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return true;
        String category = String.valueOf(mapping.getCategory());
        String key = mapping.getTranslatedKeyMessage().getString();
        String haystack = (mapping.getName() + " " + category + " " + key).toLowerCase(Locale.ROOT);
        for (String token : query.split("\\s+")) {
            if (!haystack.contains(token)) return false;
        }
        return true;
    }

    private boolean hasConflict(KeyMapping target) {
        if (target.isUnbound()) return false;
        String targetBinding = target.saveString();
        for (KeyMapping other : minecraft.options.keyMappings) {
            if (other != target && other.saveString().equals(targetBinding)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            if (event.key() == 256) {
                listening = null;
                rebuildRows();
                return true;
            }
            InputConstants.Key key = event.key() == 259
                    ? InputConstants.UNKNOWN : InputConstants.getKey(event);
            listening.setKey(key);
            KeyMapping.resetMapping();
            minecraft.options.save();
            listening = null;
            rebuildRows();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listening != null) {
            listening.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            KeyMapping.resetMapping();
            minecraft.options.save();
            listening = null;
            rebuildRows();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFF4FAFF);
        graphics.drawCenteredString(font, matches.size()
                        + " matching bindings  •  Search action, category, or assigned key",
                width / 2, 66, 0xFF8FAFC8);
        graphics.drawCenteredString(font,
                "ESC cancels  •  BACKSPACE clears  •  Conflicting assignments are flagged",
                width / 2, height - 45, 0xFF8FAFC8);
        graphics.drawCenteredString(font,
                "Page " + (page + 1) + " / " + Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE),
                width / 2, height - 73, 0xFF8FAFC8);
    }

    @Override
    public void onClose() {
        listening = null;
        minecraft.options.save();
        minecraft.setScreen(parent);
    }
}
