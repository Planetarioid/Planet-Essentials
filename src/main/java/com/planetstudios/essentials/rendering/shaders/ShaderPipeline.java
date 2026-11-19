package com.planetstudios.essentials.rendering.shaders;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Transactional program registry. Invoke reload on the backend's graphics-context
 * thread. Candidate programs are fully compiled before publication; failures keep
 * the previous active generation intact.
 */
public final class ShaderPipeline implements AutoCloseable{
 public record Snapshot(long generation,String backend,int programCount,Map<String,String> sourceHashes,String lastFailure){}
 private final AtomicLong generation=new AtomicLong();
 private Map<String,ShaderBackend.CompiledProgram> active=Map.of();
 private ShaderBackend backend;private String lastFailure="";private boolean closed;
 public synchronized boolean reload(Collection<ShaderProgramSpec> specs,ShaderBackend candidateBackend){
  Objects.requireNonNull(specs,"specs");Objects.requireNonNull(candidateBackend,"candidateBackend");
  if(closed)throw new IllegalStateException("Shader pipeline is closed");
  long requested=generation.incrementAndGet();
  Map<String,ShaderBackend.CompiledProgram> candidate=new LinkedHashMap<>();
  try{
   for(ShaderProgramSpec spec:specs){
    Objects.requireNonNull(spec,"shader spec");
    if(candidate.containsKey(spec.id()))throw new IllegalArgumentException("Duplicate shader program: "+spec.id());
    ShaderBackend.CompiledProgram compiled=candidateBackend.compile(spec);
    if(compiled==null||!spec.id().equals(compiled.programId())||!spec.contentHash().equals(compiled.sourceHash())){
     if(compiled!=null)compiled.close();throw new IllegalStateException("Backend returned invalid handle for "+spec.id());
    }
    candidate.put(spec.id(),compiled);
   }
   if(closed||requested!=generation.get()){closeAll(candidate);return false;}
   Map<String,ShaderBackend.CompiledProgram> previous=active;
   active=Map.copyOf(candidate);backend=candidateBackend;lastFailure="";closeAll(previous);return true;
  }catch(ShaderCompileException|RuntimeException failure){
   closeAll(candidate);lastFailure=failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage();return false;
  }
 }
 public synchronized ShaderBackend.CompiledProgram program(String id){return active.get(id);}
 public synchronized Snapshot snapshot(){
  Map<String,String> hashes=new LinkedHashMap<>();active.forEach((id,p)->hashes.put(id,p.sourceHash()));
  return new Snapshot(generation.get(),backend==null?"none":backend.name(),active.size(),Map.copyOf(hashes),lastFailure);
 }
 public synchronized void invalidate(){
  if(closed)return;generation.incrementAndGet();Map<String,ShaderBackend.CompiledProgram> old=active;
  active=Map.of();backend=null;closeAll(old);
 }
 @Override public synchronized void close(){
  if(closed)return;closed=true;generation.incrementAndGet();closeAll(active);active=Map.of();backend=null;
 }
 private static void closeAll(Map<String,ShaderBackend.CompiledProgram> programs){
  for(ShaderBackend.CompiledProgram p:programs.values())try{p.close();}catch(RuntimeException ignored){}
 }
}
