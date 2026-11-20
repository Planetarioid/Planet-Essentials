package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.ResourcePackCategoryConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Classic vanilla priority manager plus searchable per-pack texture, sound and animation filtering. */
public final class ResourcePackSettingsScreen extends Screen {
    private static final int PAGE_SIZE = 5;
    private final Screen parent;
    private EditBox search;
    private List<Pack> matches = List.of();
    private int page;
    private final List<AbstractWidget> rows = new ArrayList<>();
    private String status = "Classic mode uses Minecraft's native priority order.";

    public ResourcePackSettingsScreen(Screen parent) {
        super(Component.literal("RESOURCE PACK SETTINGS"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        search = new EditBox(font, width / 2 - 180, 42, 360, 20, Component.literal("Search resource packs"));
        search.setHint(Component.literal("Pack name or ID..."));
        search.setMaxLength(100);
        search.setResponder(value -> { page = 0; rebuildRows(); });
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("CLASSIC PRIORITY MANAGER"), button -> {
            minecraft.setScreen(new PackSelectionScreen(
                    minecraft.getResourcePackRepository(),
                    repository -> {
                        minecraft.options.updateResourcePacks(repository);
                        minecraft.setScreen(this);
                    },
                    minecraft.getResourcePackDirectory(),
                    Component.translatable("resourcePack.title")));
        }).bounds(width / 2 - 180, 68, 176, 20).build());

        addRenderableWidget(Button.builder(Component.literal("APPLY ADVANCED FILTERS"), button -> {
            EssentialsRuntime.save();
            minecraft.reloadResourcePacks();
            status = "Advanced filters applied. Resource packs are reloading.";
        }).bounds(width / 2 + 4, 68, 176, 20).build());

        rebuildRows();
        addRenderableWidget(Button.builder(Component.literal("PREVIOUS"), button -> {
            page = Math.max(0, page - 1);
            rebuildRows();
        }).bounds(width / 2 - 150, height - 36, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("NEXT"), button -> {
            if ((page + 1) * PAGE_SIZE < matches.size()) page++;
            rebuildRows();
        }).bounds(width / 2 + 60, height - 36, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 - 45, height - 36, 90, 20).build());
    }

    private void rebuildRows() {
        var repository = minecraft.getResourcePackRepository();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        matches = repository.getAvailablePacks().stream()
                .filter(pack -> !pack.isRequired())
                .filter(pack -> query.isEmpty()
                        || pack.getId().toLowerCase(Locale.ROOT).contains(query)
                        || pack.getTitle().getString().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(pack -> pack.getTitle().getString().toLowerCase(Locale.ROOT)))
                .toList();

        for (AbstractWidget widget : rows) removeWidget(widget);
        rows.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(matches.size(), start + PAGE_SIZE);
        int y = 112;
        for (int i = start; i < end; i++) {
            Pack pack = matches.get(i);
            String id = pack.getId();
            ResourcePackCategoryConfig config = configFor(id);
            int left = width / 2 - 180;
            addToggle(id, "TEXTURES", left, y + 18, 112,
                    () -> config.textures = !config.textures);
            addToggle(id, "SOUNDS", left + 116, y + 18, 112,
                    () -> config.sounds = !config.sounds);
            addToggle(id, "ANIMATIONS", left + 232, y + 18, 128,
                    () -> config.animations = !config.animations);
            y += 54;
        }
    }

    private void addToggle(String packId, String label, int x, int y, int width, Runnable toggle) {
        ResourcePackCategoryConfig config = configFor(packId);
        Button button = Button.builder(Component.literal(label + ": "
                        + enabled(config, label)), ignored -> {
                    toggle.run();
                    ResourcePackCategoryConfig current = configFor(packId);
                    if (current.allEnabled()) EssentialsRuntime.config().resourcePackCategoryOverrides.remove(packId);
                    EssentialsRuntime.save();
                    rebuildRows();
                }).bounds(x, y, width, 20).build();
        rows.add(button);
        addRenderableWidget(button);
    }

    private ResourcePackCategoryConfig configFor(String id) {
        return EssentialsRuntime.config().resourcePackCategoryOverrides.computeIfAbsent(
                id, ignored -> new ResourcePackCategoryConfig());
    }

    private static String enabled(ResourcePackCategoryConfig config, String category) {
        boolean value = switch (category) {
            case "TEXTURES" -> config.textures;
            case "SOUNDS" -> config.sounds;
            default -> config.animations;
        };
        return value ? "ON" : "OFF";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFF4FAFF);
        graphics.drawCenteredString(font, matches.size() + " packs found  •  Classic priority is managed by Minecraft",
                width / 2, 94, 0xFF8FAFC8);

        int start = page * PAGE_SIZE;
        int end = Math.min(matches.size(), start + PAGE_SIZE);
        int y = 112;
        var selected = minecraft.getResourcePackRepository().getSelectedIds();
        for (int i = start; i < end; i++) {
            Pack pack = matches.get(i);
            graphics.fill(width / 2 - 184, y - 2, width / 2 + 184, y + 42, 0xDD111B2B);
            graphics.fill(width / 2 - 184, y - 2, width / 2 - 181, y + 42, 0xFF56C8FF);
            String active = selected.contains(pack.getId()) ? "ACTIVE" : "AVAILABLE";
            graphics.drawString(font, pack.getTitle().getString() + "  [" + active + "]",
                    width / 2 - 174, y + 1, 0xFFF2F7FF, false);
            graphics.drawString(font, pack.getId(), width / 2 + 4, y + 1, 0xFF91A5BD, false);
            y += 54;
        }
        graphics.drawCenteredString(font, status, width / 2, height - 62, 0xFF8FAFC8);
        graphics.drawCenteredString(font, "Advanced mode filters textures, sounds and .mcmeta animation data per pack.",
                width / 2, height - 76, 0xFF8FAFC8);
        graphics.drawCenteredString(font, "Page " + (page + 1) + " / "
                        + Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE),
                width / 2, height - 50, 0xFF8FAFC8);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
