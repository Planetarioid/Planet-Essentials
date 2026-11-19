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
  assertFalse(migrated.get("diagnosticsEnabled").getAsBoolean());
 }
 @Test void migratesSchemaTwoAndPreservesExistingOverrides()throws Exception{
  JsonObject old=new JsonObject();old.addProperty("schemaVersion",2);
  JsonObject overrides=new JsonObject();overrides.addProperty("planet:rendering/entity","planet_alpha:renderer");
  old.add("featurePriorityOverrides",overrides);
  JsonObject migrated=ConfigMigrator.migrate(old);
  assertEquals(3,migrated.get("schemaVersion").getAsInt());
  assertEquals("planet_alpha:renderer",migrated.getAsJsonObject("featurePriorityOverrides").get("planet:rendering/entity").getAsString());
 }
 @Test void rejectsFutureSchemaInsteadOfOverwritingIt(){
  JsonObject future=new JsonObject();future.addProperty("schemaVersion",999);
  assertThrows(IOException.class,()->ConfigMigrator.migrate(future));
 }
}
