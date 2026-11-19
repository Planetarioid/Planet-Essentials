package com.planetstudios.essentials.client;
import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.diagnostics.DiagnosticEventType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
/** Client-only registrations. No client classes are loaded by dedicated servers. */
public final class PlanetEssentialsClient implements ClientModInitializer {
 private static final long SLOW_TICK_THRESHOLD_NANOS=50_000_000L;
 private static long tickStartedAtNanos;
 private static boolean diagnosticsEnabledForCurrentTick;
 private static boolean performanceSamplingEnabledForCurrentTick;
 private static boolean eventCollectionEnabledForCurrentTick;
 @Override public void onInitializeClient(){
  EssentialsRuntime.initialize();
  ClientLifecycleEvents.CLIENT_STARTED.register(client->record(DiagnosticEventType.CLIENT_STARTED));
  ClientLifecycleEvents.CLIENT_STOPPING.register(client->record(DiagnosticEventType.CLIENT_STOPPING));
  ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->record(DiagnosticEventType.WORLD_JOINED));
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->record(DiagnosticEventType.WORLD_LEFT));
  ClientTickEvents.START_CLIENT_TICK.register(client->{
   // One config read per tick. Disabled subsystems do not sample or allocate.
   var config=EssentialsRuntime.config();
   diagnosticsEnabledForCurrentTick=config.diagnosticsEnabled;
   performanceSamplingEnabledForCurrentTick=diagnosticsEnabledForCurrentTick&&config.performanceMonitoringEnabled;
   eventCollectionEnabledForCurrentTick=diagnosticsEnabledForCurrentTick&&config.eventCollectionEnabled;
   tickStartedAtNanos=eventCollectionEnabledForCurrentTick?System.nanoTime():0L;
  });
  ClientTickEvents.END_CLIENT_TICK.register(client->{
   if(performanceSamplingEnabledForCurrentTick&&client.level!=null)
    EssentialsRuntime.performance().recordFps(Minecraft.getInstance().getFps());
   if(eventCollectionEnabledForCurrentTick&&tickStartedAtNanos!=0L){
    long elapsed=System.nanoTime()-tickStartedAtNanos;
    if(elapsed>=SLOW_TICK_THRESHOLD_NANOS)
     EssentialsRuntime.diagnosticEvents().record(DiagnosticEventType.SLOW_CLIENT_TICK,elapsed/1_000_000L);
   }
   tickStartedAtNanos=0L;
  });
 }
 private static void record(DiagnosticEventType type){
  var config=EssentialsRuntime.config();
  if(config.diagnosticsEnabled&&config.eventCollectionEnabled)
   EssentialsRuntime.diagnosticEvents().record(type);
 }
}
