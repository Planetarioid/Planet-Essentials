package com.planetstudios.essentials.rendering.shaders;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
/** Immutable, validated shader source bundle. */
public final class ShaderProgramSpec {
 public static final int MAX_SOURCE_BYTES=1_048_576;
 private static final String ID_PATTERN="[a-z0-9_.-]+:[a-z0-9_./-]+";
 private final String id; private final Map<ShaderStage,String> sources; private final String contentHash;
 public ShaderProgramSpec(String id,Map<ShaderStage,String> sources){
  if(id==null||!id.matches(ID_PATTERN))throw new IllegalArgumentException("Invalid shader program identifier");
  Objects.requireNonNull(sources,"sources");
  if(sources.size()!=2||!sources.containsKey(ShaderStage.VERTEX)||!sources.containsKey(ShaderStage.FRAGMENT))
   throw new IllegalArgumentException("A program requires exactly one vertex and one fragment source");
  Map<ShaderStage,String> copy=new TreeMap<>();
  for(ShaderStage stage:ShaderStage.values()){
   String source=Objects.requireNonNull(sources.get(stage),stage+" source");
   int bytes=source.getBytes(StandardCharsets.UTF_8).length;
   if(source.isBlank()||bytes>MAX_SOURCE_BYTES)throw new IllegalArgumentException("Shader source is empty or exceeds 1 MiB: "+stage);
   copy.put(stage,source);
  }
  this.id=id;this.sources=Map.copyOf(copy);this.contentHash=hash(copy);
 }
 public String id(){return id;} public Map<ShaderStage,String> sources(){return sources;}
 public String source(ShaderStage stage){return sources.get(stage);} public String contentHash(){return contentHash;}
 private static String hash(Map<ShaderStage,String> values){
  try{MessageDigest d=MessageDigest.getInstance("SHA-256");for(ShaderStage s:ShaderStage.values()){
   d.update(s.name().getBytes(StandardCharsets.UTF_8));d.update((byte)0);
   d.update(values.get(s).getBytes(StandardCharsets.UTF_8));d.update((byte)255);
  }return HexFormat.of().formatHex(d.digest());}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}
 }
 @Override public boolean equals(Object o){return o instanceof ShaderProgramSpec s&&id.equals(s.id)&&contentHash.equals(s.contentHash);}
 @Override public int hashCode(){return Objects.hash(id,contentHash);}
}
