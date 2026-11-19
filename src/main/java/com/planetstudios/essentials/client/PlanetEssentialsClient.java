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
 @Override public void onInitializeClient(){
  EssentialsRuntime.initialize();
  ClientLifecycleEvents.CLIENT_STARTED.register(client->record(DiagnosticEventType.CLIENT_STARTED));
  ClientLifecycleEvents.CLIENT_STOPPING.register(client->record(DiagnosticEventType.CLIENT_STOPPING));
  ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->record(DiagnosticEventType.WORLD_JOINED));
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->record(DiagnosticEventType.WORLD_LEFT));
  ClientTickEvents.START_CLIENT_TICK.register(client->{
   tickStartedAtNanos=EssentialsRuntime.config().diagnosticsEnabled?System.nanoTime():0L;
  });
  ClientTickEvents.END_CLIENT_TICK.register(client->{
   if(!EssentialsRuntime.config().diagnosticsEnabled){tickStartedAtNanos=0L;return;}
   if(client.level!=null)EssentialsRuntime.performance().recordFps(Minecraft.getInstance().getFps());
   if(tickStartedAtNanos!=0L){
    long elapsed=System.nanoTime()-tickStartedAtNanos;
    if(elapsed>=SLOW_TICK_THRESHOLD_NANOS)
     EssentialsRuntime.diagnosticEvents().record(DiagnosticEventType.SLOW_CLIENT_TICK,elapsed/1_000_000L);
   }
   tickStartedAtNanos=0L;
  });
 }
 private static void record(DiagnosticEventType type){
  if(EssentialsRuntime.config().diagnosticsEnabled)EssentialsRuntime.diagnosticEvents().record(type);
 }
}
