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
  assertTrue(migrated.get("fpsHudEnabled").getAsBoolean());
 }
 @Test void migratesSchemaFiveToComboAndHudSettings()throws Exception{
  JsonObject old=new JsonObject();old.addProperty("schemaVersion",5);
  JsonObject migrated=ConfigMigrator.migrate(old);
  assertEquals(7,migrated.get("schemaVersion").getAsInt());
  assertTrue(migrated.get("comboCounterEnabled").getAsBoolean());
  assertTrue(migrated.get("fpsHudEnabled").getAsBoolean());
 }
 @Test void preservesExistingSettingsDuringMigration()throws Exception{
  JsonObject old=new JsonObject();old.addProperty("schemaVersion",6);
  old.addProperty("comboCounterEnabled",false);
  JsonObject migrated=ConfigMigrator.migrate(old);
  assertFalse(migrated.get("comboCounterEnabled").getAsBoolean());
  assertTrue(migrated.get("fpsHudEnabled").getAsBoolean());
 }
 @Test void rejectsFutureSchemaInsteadOfOverwritingIt(){
  JsonObject future=new JsonObject();future.addProperty("schemaVersion",999);
  assertThrows(IOException.class,()->ConfigMigrator.migrate(future));
 }
}
