package com.planetstudios.essentials;
import com.planetstudios.essentials.config.ConfigStore;
import com.planetstudios.essentials.config.EssentialsConfig;
import com.planetstudios.essentials.config.ProfileStore;
import com.planetstudios.essentials.diagnostics.DiagnosticsEventCollector;
import com.planetstudios.essentials.diagnostics.PerformanceMonitor;
import com.planetstudios.essentials.session.SessionStatistics;
import net.fabricmc.loader.api.FabricLoader;
/** Process-wide Essentials state shared by common and client entrypoints. */
public final class EssentialsRuntime {
 private static volatile EssentialsConfig config;
 private static PerformanceMonitor performanceMonitor;
 private static DiagnosticsEventCollector diagnosticsEventCollector;
 private static ProfileStore profileStore;
 private static SessionStatistics sessionStatistics;
 private EssentialsRuntime(){}
 public static synchronized void initialize(){
  if(config!=null)return;
  config=ConfigStore.load();performanceMonitor=new PerformanceMonitor(config.diagnosticsHistorySize);
  diagnosticsEventCollector=new DiagnosticsEventCollector(config.diagnosticsHistorySize);
  profileStore=new ProfileStore(FabricLoader.getInstance().getConfigDir().resolve("planet-essentials").resolve("profiles"));
  sessionStatistics=new SessionStatistics();
  if(!profileStore.listProfiles().contains("default"))profileStore.save("default",config);
 }
 public static EssentialsConfig config(){ensureInitialized();return config;}
 public static synchronized PerformanceMonitor performance(){ensureInitialized();return performanceMonitor;}
 public static synchronized DiagnosticsEventCollector diagnosticEvents(){ensureInitialized();return diagnosticsEventCollector;}
 public static synchronized ProfileStore profiles(){ensureInitialized();return profileStore;}
 public static synchronized SessionStatistics sessionStatistics(){ensureInitialized();return sessionStatistics;}
 public static synchronized void replaceConfig(EssentialsConfig replacement){
  ensureInitialized();replacement.validate();config=replacement;
  performanceMonitor.setCapacity(config.diagnosticsHistorySize);
  diagnosticsEventCollector.setCapacity(config.diagnosticsHistorySize);
 }
 public static synchronized void save(){ensureInitialized();ConfigStore.save(config);}
 private static void ensureInitialized(){if(config==null)initialize();}
}
