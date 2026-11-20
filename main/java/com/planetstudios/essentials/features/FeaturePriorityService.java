package com.planetstudios.essentials.features;
import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.planetlib.feature.*;
import net.fabricmc.loader.api.FabricLoader;
import java.util.Map;
import java.util.TreeMap;
/** Essentials persistence adapter for Planet Lib feature priorities. */
public final class FeaturePriorityService {
 private static final FeaturePriorityService INSTANCE=new FeaturePriorityService();
 private FeaturePriorityService(){}
 public static FeaturePriorityService getInstance(){return INSTANCE;}
 public boolean isAvailable(){return FabricLoader.getInstance().isModLoaded("planet_lib");}
 public synchronized FeaturePrioritySnapshot refresh(){
  if(!isAvailable())return null;
  FeaturePriorityManager manager=FeaturePriorityManager.getInstance();
  EssentialsRuntime.config().featurePriorityOverrides.forEach((cap,encoded)->{
   FeatureId id=parse(encoded); if(id!=null)try{manager.preferImplementation(cap,id);}catch(IllegalArgumentException ignored){}
  });
  return manager.snapshot();
 }
 public synchronized void select(String cap,FeatureId id){
  if(!isAvailable())throw new IllegalStateException("Planet Lib is not installed.");
  FeaturePriorityManager.getInstance().preferImplementation(cap,id);
  EssentialsRuntime.config().featurePriorityOverrides.put(cap,id.toString()); EssentialsRuntime.save();
 }
 public synchronized void reset(String cap){
  if(!isAvailable())return;
  FeaturePriorityManager.getInstance().clearPreference(cap);
  EssentialsRuntime.config().featurePriorityOverrides.remove(cap); EssentialsRuntime.save();
 }
 public Map<FeatureId,FeatureDescriptor> descriptors(){
  if(!isAvailable())return Map.of();
  Map<FeatureId,FeatureDescriptor> out=new TreeMap<>();
  FeatureRegistry.getInstance().snapshot().getFeatures().forEach(d->out.put(d.id(),d));
  return Map.copyOf(out);
 }
 private static FeatureId parse(String value){
  if(value==null)return null; int i=value.indexOf(':');
  if(i<=0||i!=value.lastIndexOf(':')||i==value.length()-1)return null;
  try{return new FeatureId(value.substring(0,i),value.substring(i+1));}catch(IllegalArgumentException ex){return null;}
 }
}
