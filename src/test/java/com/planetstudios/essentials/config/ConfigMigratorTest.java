package com.planetstudios.essentials.config;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class ConfigMigratorTest {
 @Test void migratesUnversionedConfigToCurrentSchema()throws Exception{
  JsonObject old=new JsonObject();old.addProperty("diagnosticsEnabled",false);
  JsonObject migrated=ConfigMigrator.migrate(old);
  assertEquals(ConfigMigrator.CURRENT_SCHEMA,migrated.get("schemaVersion").getAsInt());
  assertEquals("default",migrated.get("activeProfile").getAsString());
  assertTrue(migrated.get("featurePriorityOverrides").isJsonObject());
  assertTrue(migrated.get("performanceMonitoringEnabled").getAsBoolean());
  assertTrue(migrated.get("eventCollectionEnabled").getAsBoolean());
  assertFalse(migrated.get("diagnosticsEnabled").getAsBoolean());
  assertFalse(migrated.get("shadersEnabled").getAsBoolean());
  assertEquals("",migrated.get("selectedShaderPack").getAsString());
  assertTrue(migrated.get("comboCounterEnabled").getAsBoolean());
 }
 @Test void migratesSchemaTwoAndPreservesExistingOverrides()throws Exception{
  JsonObject old=new JsonObject();old.addProperty("schemaVersion",2);
  JsonObject overrides=new JsonObject();overrides.addProperty("planet:rendering/entity","planet_alpha:renderer");old.add("featurePriorityOverrides",overrides);
  JsonObject migrated=ConfigMigrator.migrate(old);
  assertEquals(ConfigMigrator.CURRENT_SCHEMA,migrated.get("schemaVersion").getAsInt());
  assertEquals("planet_alpha:renderer",migrated.getAsJsonObject("featurePriorityOverrides").get("planet:rendering/entity").getAsString());
 }
 @Test void preservesExplicitSubsystemPreferences()throws Exception{
  JsonObject old=new JsonObject();old.addProperty("schemaVersion",3);old.addProperty("performanceMonitoringEnabled",false);
  JsonObject migrated=ConfigMigrator.migrate(old);
  assertFalse(migrated.get("performanceMonitoringEnabled").getAsBoolean());
  assertTrue(migrated.get("eventCollectionEnabled").getAsBoolean());
  assertFalse(migrated.get("shadersEnabled").getAsBoolean());
  assertTrue(migrated.get("comboCounterEnabled").getAsBoolean());
 }
 @Test void migratesSchemaFourToOptInShaderSettings()throws Exception{
  JsonObject old=new JsonObject();old.addProperty("schemaVersion",4);old.addProperty("diagnosticsEnabled",true);
  old.addProperty("shadersEnabled",true);old.addProperty("selectedShaderPack","planet:test");
  JsonObject migrated=ConfigMigrator.migrate(old);
  assertEquals(5,migrated.get("schemaVersion").getAsInt());
  assertTrue(migrated.get("shadersEnabled").getAsBoolean());
  assertEquals("planet:test",migrated.get("selectedShaderPack").getAsString());
  assertTrue(migrated.get("comboCounterEnabled").getAsBoolean());
 }
 @Test void migratesSchemaFiveToComboWidgetSetting()throws Exception{
  JsonObject old=new JsonObject();old.addProperty("schemaVersion",5);
  JsonObject migrated=ConfigMigrator.migrate(old);
  assertEquals(6,migrated.get("schemaVersion").getAsInt());
  assertTrue(migrated.get("comboCounterEnabled").getAsBoolean());
 }
 @Test void rejectsFutureSchemaInsteadOfOverwritingIt(){
  JsonObject future=new JsonObject();future.addProperty("schemaVersion",999);
  assertThrows(IOException.class,()->ConfigMigrator.migrate(future));
 }
}
