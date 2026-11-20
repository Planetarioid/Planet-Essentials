package com.planetstudios.essentials.client;
import com.planetstudios.essentials.features.FeaturePriorityService;
import com.planetstudios.planetlib.feature.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
/** UI for selecting one implementation per shared capability. */
public final class FeaturePriorityScreen extends Screen {
 private final Screen parent;
 private final FeaturePriorityService service=FeaturePriorityService.getInstance();
 private List<FeatureResolution> resolutions=List.of();
 public FeaturePriorityScreen(Screen parent){super(Component.literal("Feature Priority Management"));this.parent=parent;}
 @Override protected void init(){
  FeaturePrioritySnapshot snapshot=service.refresh();resolutions=snapshot==null?List.of():snapshot.getResolutions();
  int center=width/2,y=height/4+26,visible=Math.min(resolutions.size(),8);
  for(int i=0;i<visible;i++){
   String cap=resolutions.get(i).capabilityId();
   addRenderableWidget(Button.builder(label(resolutions.get(i)),b->{cycle(cap);minecraft.setScreen(new FeaturePriorityScreen(parent));})
    .bounds(center-155,y,250,20).build());
   addRenderableWidget(Button.builder(Component.literal("Reset"),b->{service.reset(cap);minecraft.setScreen(new FeaturePriorityScreen(parent));})
    .bounds(center+105,y,50,20).build()); y+=25;
  }
  addRenderableWidget(Button.builder(Component.literal("Back"),b->minecraft.setScreen(parent)).bounds(center-100,height-38,200,20).build());
 }
 private Component label(FeatureResolution r){return Component.literal(r.capabilityId()+" -> "+r.selected().featureId()+" ["+(r.isUserPreferred()?"Manual":"Automatic")+"]");}
 private void cycle(String cap){
  FeatureResolution r=service.refresh().find(cap).orElse(null);if(r==null||r.implementations().isEmpty())return;
  List<FeatureImplementation> candidates=r.implementations();int at=candidates.indexOf(r.selected());
  service.select(cap,candidates.get((at+1)%candidates.size()).featureId());
 }
 @Override public void render(GuiGraphics g,int mx,int my,float tick){
  // The screen framework renders the background before this method; do not request another blur.
  super.render(g,mx,my,tick);g.drawCenteredString(font,title,width/2,height/4,0xFFFFFF);
  if(!service.isAvailable())g.drawCenteredString(font,"Planet Lib is not installed.",width/2,height/2,0xAAAAAA);
  else if(resolutions.isEmpty())g.drawCenteredString(font,"No feature providers have registered yet.",width/2,height/2,0xAAAAAA);
  else{
   g.drawCenteredString(font,"Selections are published to Planet Lib; providers must honor them.",width/2,height-54,0xAAAAAA);
   if(resolutions.size()>8)g.drawCenteredString(font,"Showing first 8 of "+resolutions.size()+" capabilities.",width/2,height-66,0xAAAAAA);
  }
 }
}
