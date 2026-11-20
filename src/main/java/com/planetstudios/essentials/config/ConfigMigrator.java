package com.planetstudios.essentials.config;
import com.google.gson.JsonObject;
import java.io.IOException;
/** Explicit, forward-only configuration schema migrations. */
public final class ConfigMigrator {
 public static final int CURRENT_SCHEMA=8;
 private ConfigMigrator() {}
 public static JsonObject migrate(JsonObject json)throws IOException{
  JsonObject migrated=json.deepCopy(); int version;
  try{version=migrated.has("schemaVersion")?migrated.get("schemaVersion").getAsInt():0;}
  catch(RuntimeException ex){throw new IOException("Configuration schema version is invalid",ex);}
  if(version>CURRENT_SCHEMA) throw new IOException("Configuration schema "+version+" is newer than supported schema "+CURRENT_SCHEMA);
  if(version<1){migrated.addProperty("schemaVersion",1);version=1;}
  if(version<2){if(!migrated.has("activeProfile")||migrated.get("activeProfile").isJsonNull()) migrated.addProperty("activeProfile","default");migrated.addProperty("schemaVersion",2);version=2;}
  if(version<3){if(!migrated.has("featurePriorityOverrides")||migrated.get("featurePriorityOverrides").isJsonNull())migrated.add("featurePriorityOverrides",new JsonObject());migrated.addProperty("schemaVersion",3);version=3;}
  if(version<4){
   if(!migrated.has("performanceMonitoringEnabled"))migrated.addProperty("performanceMonitoringEnabled",true);
   if(!migrated.has("eventCollectionEnabled"))migrated.addProperty("eventCollectionEnabled",false);
   migrated.addProperty("schemaVersion",4);version=4;
  }
  if(version<5){
   if(!migrated.has("shadersEnabled"))migrated.addProperty("shadersEnabled",false);
   if(!migrated.has("selectedShaderPack")||migrated.get("selectedShaderPack").isJsonNull())migrated.addProperty("selectedShaderPack","");
   migrated.addProperty("schemaVersion",5);version=5;
  }
  if(version<6){
   if(!migrated.has("comboCounterEnabled"))migrated.addProperty("comboCounterEnabled",true);
   migrated.addProperty("schemaVersion",6);version=6;
  }
  if(version<7){
   if(!migrated.has("fpsHudEnabled"))migrated.addProperty("fpsHudEnabled",true);
   migrated.addProperty("schemaVersion",7);version=7;
  }
  if(version<8){
   if(!migrated.has("automaticServerProfilesEnabled"))migrated.addProperty("automaticServerProfilesEnabled",true);
   if(!migrated.has("serverProfileMappings")||migrated.get("serverProfileMappings").isJsonNull())
    migrated.add("serverProfileMappings",new JsonObject());
   migrated.addProperty("schemaVersion",8);
  }
  return migrated;
 }
}
