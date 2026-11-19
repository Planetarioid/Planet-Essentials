package com.planetstudios.essentials.config;
import java.util.Map;
import java.util.TreeMap;
/** User-owned Planet Essentials settings. */
public final class EssentialsConfig {
 public int schemaVersion=ConfigMigrator.CURRENT_SCHEMA;
 public boolean diagnosticsEnabled=true, showPerformanceScreen=true;
 public int diagnosticsHistorySize=120;
 public String activeProfile="default";
 /** Capability ID to explicitly preferred FeatureId (modId:featureId). */
 public Map<String,String> featurePriorityOverrides=new TreeMap<>();
 public void validate(){
  schemaVersion=ConfigMigrator.CURRENT_SCHEMA;
  if(activeProfile==null||!activeProfile.matches("[a-z0-9_-]{1,32}")) activeProfile="default";
  diagnosticsHistorySize=Math.max(20,Math.min(1200,diagnosticsHistorySize));
  if(featurePriorityOverrides==null) featurePriorityOverrides=new TreeMap<>();
  Map<String,String> valid=new TreeMap<>();
  featurePriorityOverrides.forEach((cap,feature)->{
   if(cap!=null&&cap.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")&&feature!=null&&feature.matches("[a-z0-9_.-]+:[a-z0-9_.-]+")) valid.put(cap,feature);
  });
  featurePriorityOverrides=valid;
  SettingsRegistry.validate(this);
 }
}
