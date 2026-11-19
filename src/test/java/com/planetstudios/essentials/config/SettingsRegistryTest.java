package com.planetstudios.essentials.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SettingsRegistryTest {
 @Test void exposesOnlyImplementedSettingsWithStableIds(){
  assertTrue(SettingsRegistry.find("diagnostics.enabled").isPresent());
  assertTrue(SettingsRegistry.find("systems.performance_monitoring").isPresent());
  assertTrue(SettingsRegistry.find("systems.event_collection").isPresent());
  assertTrue(SettingsRegistry.find("shaders.enabled").isPresent());
  assertEquals(6,SettingsRegistry.all().size());
  assertEquals(3,SettingsRegistry.inCategory("diagnostics").size());
  assertEquals(2,SettingsRegistry.inCategory("systems").size());
  assertEquals(1,SettingsRegistry.inCategory("shaders").size());
 }
 @Test void settingWritesAreTypeAndRangeChecked(){
  EssentialsConfig config=new EssentialsConfig();
  SettingDefinition<?> history=SettingsRegistry.find("diagnostics.history_size").orElseThrow();
  assertThrows(IllegalArgumentException.class,()->history.set(config,"fast"));
  assertThrows(IllegalArgumentException.class,()->history.set(config,5));
  history.set(config,240);assertEquals(240,config.diagnosticsHistorySize);
 }
 @Test void subsystemSwitchesAreIndependent(){
  EssentialsConfig config=new EssentialsConfig();
  SettingsRegistry.find("systems.performance_monitoring").orElseThrow().set(config,false);
  assertFalse(config.performanceMonitoringEnabled);
  assertTrue(config.eventCollectionEnabled);
  SettingsRegistry.find("systems.event_collection").orElseThrow().set(config,false);
  assertFalse(config.performanceMonitoringEnabled);
  assertFalse(config.eventCollectionEnabled);
 }
 @Test void shaderSwitchIsOptInAndIndependentFromDiagnostics(){
  EssentialsConfig config=new EssentialsConfig();
  assertFalse(config.shadersEnabled);
  SettingsRegistry.find("shaders.enabled").orElseThrow().set(config,true);
  assertTrue(config.shadersEnabled);
  assertTrue(config.diagnosticsEnabled);
 }
}
