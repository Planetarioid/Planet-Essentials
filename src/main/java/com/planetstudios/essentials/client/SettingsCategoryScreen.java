package com.planetstudios.essentials.client;
import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.SettingDefinition;
import com.planetstudios.essentials.config.SettingsRegistry;
import com.planetstudios.essentials.diagnostics.DiagnosticEvent;
import com.planetstudios.essentials.diagnostics.DiagnosticsExporter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.util.List;
/** Native settings page generated from the typed settings registry. */
public final class SettingsCategoryScreen extends Screen {
 private final Screen parent;private final String category;
 private String exportStatus="Export creates a local file only when requested.";
 public SettingsCategoryScreen(Screen parent,String category){super(Component.literal("Performance & Diagnostics"));this.parent=parent;this.category=category;}
 @Override protected void init(){
  List<SettingDefinition<?>> settings=SettingsRegistry.inCategory(category);int center=width/2,y=height/4+24;
  for(SettingDefinition<?> setting:settings){addRenderableWidget(createButton(setting,center-155,y,310,20));y+=27;}
  addRenderableWidget(Button.builder(Component.literal("Export diagnostics"),button->{
   try{
    Path file=DiagnosticsExporter.export(FabricLoader.getInstance().getConfigDir(),
      EssentialsRuntime.performance().snapshot(),EssentialsRuntime.diagnosticEvents().snapshot());
    exportStatus="Exported: "+file.getFileName();
   }catch(RuntimeException exception){exportStatus="Export failed: "+exception.getMessage();}
  }).bounds(center-100,height-96,200,20).build());
  addRenderableWidget(Button.builder(Component.literal("Clear diagnostic history"),button->{
   EssentialsRuntime.performance().clear();EssentialsRuntime.diagnosticEvents().clear();
  }).bounds(center-100,height-68,200,20).build());
  addRenderableWidget(Button.builder(Component.literal("Back"),button->minecraft.setScreen(parent)).bounds(center-100,height-40,200,20).build());
 }
 private Button createButton(SettingDefinition<?> setting,int x,int y,int w,int h){
  return Button.builder(label(setting),button->{
   Object current=setting.get(EssentialsRuntime.config());
   if(setting.kind()==SettingDefinition.Kind.BOOLEAN)setting.set(EssentialsRuntime.config(),!((Boolean)current));
   else if(setting.kind()==SettingDefinition.Kind.INTEGER){int value=(Integer)current+20;if(value>1200)value=20;setting.set(EssentialsRuntime.config(),value);}
   EssentialsRuntime.performance().setCapacity(EssentialsRuntime.config().diagnosticsHistorySize);
   EssentialsRuntime.diagnosticEvents().setCapacity(EssentialsRuntime.config().diagnosticsHistorySize);
   EssentialsRuntime.save();button.setMessage(label(setting));
  }).bounds(x,y,w,h).build();
 }
 private Component label(SettingDefinition<?> setting){return Component.literal(setting.displayName()+": "+setting.get(EssentialsRuntime.config()));}
 @Override public void render(GuiGraphics g,int mx,int my,float tick){
  renderBackground(g,mx,my,tick);super.render(g,mx,my,tick);g.drawCenteredString(font,title,width/2,height/4,0xFFFFFF);
  int y=height/4+48+SettingsRegistry.inCategory(category).size()*27;
  g.drawCenteredString(font,"Select a setting to cycle its available value.",width/2,y,0xAAAAAA);
  if(EssentialsRuntime.config().diagnosticsEnabled){
   var collector=EssentialsRuntime.diagnosticEvents();DiagnosticEvent latest=collector.latest();
   String summary=latest==null?"No diagnostic events collected":"Events: "+collector.eventCount()+" | Latest: "+latest.type();
   g.drawCenteredString(font,summary,width/2,y+16,0xAAAAAA);
  }else g.drawCenteredString(font,"Diagnostics collection is disabled.",width/2,y+16,0xAAAAAA);
  g.drawCenteredString(font,exportStatus,width/2,height-108,0xAAAAAA);
 }
 @Override public void onClose(){minecraft.setScreen(parent);}
}
