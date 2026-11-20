package com.planetstudios.essentials.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EssentialsConfigTest {
 @Test void clampsDiagnosticsHistoryToSupportedRange(){
  EssentialsConfig c=new EssentialsConfig();c.diagnosticsHistorySize=1;c.validate();assertEquals(20,c.diagnosticsHistorySize);
  c.diagnosticsHistorySize=5000;c.validate();assertEquals(1200,c.diagnosticsHistorySize);
 }
 @Test void rejectsInvalidProfileNames(){EssentialsConfig c=new EssentialsConfig();c.activeProfile="../unsafe";c.validate();assertEquals("default",c.activeProfile);}
 @Test void restoresCurrentSchemaVersion(){EssentialsConfig c=new EssentialsConfig();c.schemaVersion=999;c.validate();assertEquals(ConfigMigrator.CURRENT_SCHEMA,c.schemaVersion);}
 @Test void preservesValidFeatureOverridesAndDropsMalformedEntries(){
  EssentialsConfig c=new EssentialsConfig();c.featurePriorityOverrides.put("planet:rendering/entity","planet_alpha:renderer");
  c.featurePriorityOverrides.put("../bad","planet_beta:renderer");c.featurePriorityOverrides.put("planet:audio","../bad");c.validate();
  assertEquals(1,c.featurePriorityOverrides.size());assertEquals("planet_alpha:renderer",c.featurePriorityOverrides.get("planet:rendering/entity"));
 }
 @Test void shadersAreOffByDefaultAndInvalidPackIdsAreCleared(){
  EssentialsConfig c=new EssentialsConfig();c.validate();assertFalse(c.shadersEnabled);assertEquals("planet:default",c.selectedShaderPack);
  c.shadersEnabled=true;c.selectedShaderPack="../unsafe";c.validate();assertTrue(c.shadersEnabled);assertEquals("",c.selectedShaderPack);
 }
}
