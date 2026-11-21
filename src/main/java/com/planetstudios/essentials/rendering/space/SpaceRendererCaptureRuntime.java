package com.planetstudios.essentials.rendering.space;
import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
/** Bridges vanilla section rebuild output into bounded Space Renderer CPU workers. */
public final class SpaceRendererCaptureRuntime {
 private static final int WORKERS=Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors()-1));
 private static final int MAX_IN_FLIGHT=96,MAX_READY_SECTIONS=4096;
 private static final SpaceRendererSectionManager SECTIONS=new SpaceRendererSectionManager();
 private static final SpaceRendererMeshBuildScheduler<SpaceRendererMesh> SCHEDULER=
  new SpaceRendererMeshBuildScheduler<>(WORKERS,MAX_IN_FLIGHT,threadFactory());
 private static final LinkedHashMap<SpaceRendererSectionManager.SectionKey,LinkedHashMap<String,SpaceRendererMesh>> READY=new LinkedHashMap<>(64,0.75f,true);
 private static volatile boolean closed;
 private SpaceRendererCaptureRuntime(){}
 public static boolean isCaptureEnabled(){return !closed;}
 public static SpaceRendererSectionManager.BuildToken beginRebuild(ClientLevel level,SectionPos pos){
  Objects.requireNonNull(level,"level");Objects.requireNonNull(pos,"pos");
  var key=key(level,pos);var token=SECTIONS.beginBuild(key);synchronized(READY){READY.remove(key);}return token;
 }
 public static boolean publish(SpaceRendererSectionManager.BuildToken token,PrismTerrainVertexData data){
  if(closed||token==null||data==null||!SECTIONS.isCurrent(token))return false;
  return SCHEDULER.submit(token,0.0,()->SpaceRendererMesh.build(data));
 }
 /** Drain on render thread; revision checks discard obsolete worker results. */
 public static int drainCompletions(){
  if(closed)return 0;var results=SCHEDULER.drainCompletions(MAX_IN_FLIGHT);int published=0;
  for(var result:results){if(!result.succeeded()){PlanetEssentials.LOGGER.warn("Space Renderer CPU mesh preparation failed for {}",result.token().key(),result.failure());continue;}
   if(!SECTIONS.isCurrent(result.token()))continue;
   synchronized(READY){var section=READY.computeIfAbsent(result.token().key(),ignored->new LinkedHashMap<>());
    section.put(result.value().materialId(),result.value());while(READY.size()>MAX_READY_SECTIONS)READY.remove(READY.keySet().iterator().next());}
   published++;
  }return published;
 }
 public static List<SpaceRendererMesh> meshesFor(ClientLevel level,SectionPos pos){
  synchronized(READY){Map<String,SpaceRendererMesh> meshes=READY.get(key(level,pos));return meshes==null?List.of():List.copyOf(new ArrayList<>(meshes.values()));}
 }
 public static int readySectionCount(){synchronized(READY){return READY.size();}}
 public static void unloadChunk(ClientLevel level,int chunkX,int minSectionY,int maxSectionY,int chunkZ){
  String dimension=dimensionId(level);synchronized(READY){for(int y=minSectionY;y<maxSectionY;y++){
   var key=new SpaceRendererSectionManager.SectionKey(dimension,chunkX,y,chunkZ);SECTIONS.unload(key);READY.remove(key);
  }}
 }
 public static void clearWorld(ClientLevel level){if(level==null)return;String dimension=dimensionId(level);SECTIONS.unloadDimension(dimension);
  synchronized(READY){READY.keySet().removeIf(key->key.dimensionId().equals(dimension));}}
 public static void clearAll(){SECTIONS.clearAll();synchronized(READY){READY.clear();}}
 public static void shutdown(){if(closed)return;closed=true;clearAll();SCHEDULER.close();}
 private static SpaceRendererSectionManager.SectionKey key(ClientLevel level,SectionPos pos){return new SpaceRendererSectionManager.SectionKey(dimensionId(level),pos.x(),pos.y(),pos.z());}
 private static String dimensionId(ClientLevel level){return level.dimension().location().toString();}
 private static ThreadFactory threadFactory(){AtomicInteger index=new AtomicInteger();return task->{Thread thread=new Thread(task,"Space Renderer Mesh Builder-"+index.incrementAndGet());thread.setDaemon(true);return thread;};}
}
