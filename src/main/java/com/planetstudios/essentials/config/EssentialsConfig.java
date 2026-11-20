package com.planetstudios.essentials.config;

import java.util.Map;
import java.util.TreeMap;

/** User-owned Space Client settings. Performance sampling is opt-in. */
public final class EssentialsConfig {
 public int schemaVersion=ConfigMigrator.CURRENT_SCHEMA;
 public boolean diagnosticsEnabled=true, showPerformanceScreen=true;
 public boolean fpsHudEnabled=true;
 public boolean crosshairTargetIndicator=true;
 public boolean comboCounterEnabled=true;
 public Map<String,HudWidgetPlacement> hudWidgetPlacements=new TreeMap<>();
 public boolean performanceMonitoringEnabled=false, eventCollectionEnabled=false;
 public boolean shadersEnabled=false;
 public String selectedShaderPack="planet:default";
 public int diagnosticsHistorySize=120;
 public String activeProfile="default";
 public boolean automaticServerProfilesEnabled=true;
 public Map<String,String> serverProfileMappings=new TreeMap<>();
 public String performancePreset="balanced";
 public Map<String,ResourcePackCategoryConfig> resourcePackCategoryOverrides=new TreeMap<>();
 public boolean smartChunkSchedulerEnabled=true;
 public Map<String,String> featurePriorityOverrides=new TreeMap<>();
 public void validate(){
  schemaVersion=ConfigMigrator.CURRENT_SCHEMA;
  if(activeProfile==null||!activeProfile.matches("[a-z0-9_-]{1,32}")) activeProfile="default";
  if(!java.util.Set.of("competitive","balanced","visual").contains(performancePreset)) performancePreset="balanced";
  if(selectedShaderPack==null||selectedShaderPack.length()>128
    ||(!selectedShaderPack.isEmpty()&&!selectedShaderPack.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
    ||selectedShaderPack.contains("..")||selectedShaderPack.contains("//")) selectedShaderPack="";
  diagnosticsHistorySize=Math.max(20,Math.min(1200,diagnosticsHistorySize));
  if(hudWidgetPlacements==null)hudWidgetPlacements=new TreeMap<>();
  Map<String,HudWidgetPlacement> validPlacements=new TreeMap<>();
  hudWidgetPlacements.forEach((id,placement)->{
   if(id!=null&&id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+")&&placement!=null){
    placement.validate();validPlacements.put(id,placement);
   }
  });
  hudWidgetPlacements=validPlacements;
  if(serverProfileMappings==null)serverProfileMappings=new TreeMap<>();
  Map<String,String> validServerMappings=new TreeMap<>();
  serverProfileMappings.forEach((server,profile)->{
   if(server!=null&&server.length()<=255&&server.matches("[a-z0-9._:\\[\\]-]+")
     &&profile!=null&&profile.matches("[a-z0-9_-]{1,32}")) validServerMappings.put(server,profile);
  });
  serverProfileMappings=validServerMappings;
  if(resourcePackCategoryOverrides==null)resourcePackCategoryOverrides=new TreeMap<>();
  Map<String,ResourcePackCategoryConfig> validPackOverrides=new TreeMap<>();
  resourcePackCategoryOverrides.forEach((packId,categories)->{
   if(packId!=null&&packId.length()<=255&&packId.matches("[A-Za-z0-9_.:/-]+")&&categories!=null){
    categories.validate();
    if(!categories.allEnabled()) validPackOverrides.put(packId,categories);
   }
  });
  resourcePackCategoryOverrides=validPackOverrides;
  if(featurePriorityOverrides==null) featurePriorityOverrides=new TreeMap<>();
  Map<String,String> valid=new TreeMap<>();
  featurePriorityOverrides.forEach((cap,feature)->{
   if(cap!=null&&cap.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")&&feature!=null&&feature.matches("[a-z0-9_.-]+:[a-z0-9_.-]+")) valid.put(cap,feature);
  });
  featurePriorityOverrides=valid;
  SettingsRegistry.validate(this);
 }
}
