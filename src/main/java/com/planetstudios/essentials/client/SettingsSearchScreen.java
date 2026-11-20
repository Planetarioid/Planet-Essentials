package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.SettingDefinition;
import com.planetstudios.essentials.config.SettingsRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Unified, searchable Space Client settings library with category-labelled option cards. */
public final class SettingsSearchScreen extends Screen {
    private sealed interface Result permits PlanetResult, MinecraftResult {
        String category();
        String name();
        String searchText();
    }
    private record PlanetResult(SettingDefinition<?> setting) implements Result {
        public String category() { return setting.category(); }
        public String name() { return setting.displayName(); }
        public String searchText() { return (setting.id()+" "+setting.category()+" "+setting.displayName()+" "+setting.description()+" "+aliases(setting.id())).toLowerCase(Locale.ROOT); }
    }
    private record MinecraftResult(VanillaSettingsRegistry.Entry entry) implements Result {
        public String category() { return entry.category(); }
        public String name() { return entry.name(); }
        public String searchText() { return entry.searchText(); }
    }

    private final Screen parent;
    private EditBox search;
    private int page;
    private static final int PAGE_SIZE = 6;
    private List<Result> matches = List.of();
    private final List<AbstractWidget> resultWidgets = new ArrayList<>();
    private static final int ACCENT = 0xFF56C8FF;

    public SettingsSearchScreen(Screen parent) {
        super(Component.literal("SETTINGS LIBRARY"));
        this.parent = parent;
    }

    @Override protected void init() {
        int center = width / 2;
        search = new EditBox(font, center - 184, 48, 330, 22, Component.literal("Search settings"));
        search.setHint(Component.literal("Search settings, categories, tags..."));
        search.setMaxLength(100);
        search.setResponder(value -> { page = 0; rebuildResults(); });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("CLEAR"), b -> search.setValue(""))
                .bounds(center + 152, 48, 64, 22).build());
        rebuildResults();
        addRenderableWidget(Button.builder(Component.literal("‹ PREVIOUS"), b -> { page=Math.max(0,page-1); rebuildResults(); })
                .bounds(center - 180, height - 38, 108, 22).build());
        addRenderableWidget(Button.builder(Component.literal("NEXT ›"), b -> { if((page+1)*PAGE_SIZE<matches.size())page++; rebuildResults(); })
                .bounds(center + 72, height - 38, 108, 22).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), b -> minecraft.setScreen(parent))
                .bounds(center - 48, height - 38, 96, 22).build());
    }

    private void rebuildResults() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Result> all = new ArrayList<>();
        for (SettingDefinition<?> setting : SettingsRegistry.all()) all.add(new PlanetResult(setting));
        for (VanillaSettingsRegistry.Entry entry : VanillaSettingsRegistry.all(minecraft)) all.add(new MinecraftResult(entry));
        matches = all.stream().filter(result -> matchesQuery(result.searchText(), query))
                .sorted(Comparator.comparing(Result::category).thenComparing(Result::name)).toList();
        for (AbstractWidget widget : resultWidgets) removeWidget(widget);
        resultWidgets.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(matches.size(), start + PAGE_SIZE);
        int y = 104;
        for (int i=start;i<end;i++) {
            Result result = matches.get(i);
            AbstractWidget control;
            if (result instanceof MinecraftResult vanilla) {
                control = vanilla.entry().option().createButton(minecraft.options, width/2-170, y, 340);
            } else {
                SettingDefinition<?> setting = ((PlanetResult) result).setting();
                control = Button.builder(Component.literal(result.name()+"   ·   "+setting.get(EssentialsRuntime.config())),
                        ignored -> change(setting)).bounds(width/2-170,y,340,22).build();
            }
            resultWidgets.add(control);
            addRenderableWidget(control);
            y += 43;
        }
    }

    private static boolean matchesQuery(String haystack, String query) {
        if (query.isEmpty()) return true;
        for (String token : query.split("\\s+")) if (!haystack.contains(token)) return false;
        return true;
    }

    private static String aliases(String id) {
        return switch(id) {
            case "combat.crosshair_target_indicator" -> "hit indicator crosshair target melee range player";
            case "combat.combo_counter" -> "combo hits streak incoming outgoing negative counter";
            case "hud.fps" -> "frames per second frame rate performance";
            case "diagnostics.history_size" -> "history samples buffer capacity";
            case "diagnostics.performance_screen" -> "fps summary overlay";
            case "systems.performance_monitoring" -> "fps sampling frame rate";
            case "systems.event_collection" -> "events logging";
            case "shaders.enabled" -> "prism graphics rendering effects";
            default -> "";
        };
    }

    private void change(SettingDefinition<?> setting) {
        Object current=setting.get(EssentialsRuntime.config());
        if(setting.kind()==SettingDefinition.Kind.BOOLEAN) setting.set(EssentialsRuntime.config(),!((Boolean)current));
        else if(setting.kind()==SettingDefinition.Kind.INTEGER) {
            int value=(Integer)current+20;
            if(value>1200)value=20;
            setting.set(EssentialsRuntime.config(),value);
        }
        EssentialsRuntime.performance().setCapacity(EssentialsRuntime.config().diagnosticsHistorySize);
        EssentialsRuntime.diagnosticEvents().setCapacity(EssentialsRuntime.config().diagnosticsHistorySize);
        EssentialsRuntime.save();
        rebuildResults();
    }

    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        graphics.fill(0, 0, width, height, 0xE9080D17);
        int panelWidth = Math.min(620, width - 28);
        int left = (width - panelWidth) / 2;
        graphics.fill(left, 22, left + panelWidth, height - 18, 0xF0111A29);
        graphics.fill(left, 22, left + panelWidth, 25, ACCENT);
        graphics.drawString(font, "SPACE CLIENT  /  SETTINGS LIBRARY", left + 18, 34, 0xFFF2F7FF, false);
        graphics.drawString(font, matches.size()+" SETTINGS  ·  PLANET + MINECRAFT", left + 18, 78, 0xFF91A5BD, false);
        int first = page * PAGE_SIZE;
        int rowY = 94;
        for (int i = first; i < Math.min(matches.size(), first + PAGE_SIZE); i++) {
            Result result = matches.get(i);
            graphics.fill(width/2-180, rowY, width/2+180, rowY+36, 0xFF192638);
            graphics.fill(width/2-180, rowY, width/2-177, rowY+36, ACCENT);
            String category = result.category().replace('_', ' ').toUpperCase(Locale.ROOT);
            graphics.drawString(font, category, width/2-168, rowY+2, ACCENT, false);
            rowY += 43;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font,matches.isEmpty()?"No settings found. Try another keyword.":
                "PAGE "+(page+1)+" / "+Math.max(1,(matches.size()+PAGE_SIZE-1)/PAGE_SIZE),
                width/2,height-53,0xFF91A5BD);
    }

    @Override public void onClose(){minecraft.setScreen(parent);}

    @Override public void removed() {
        if (minecraft != null && minecraft.options != null) minecraft.options.save();
        super.removed();
    }
}
