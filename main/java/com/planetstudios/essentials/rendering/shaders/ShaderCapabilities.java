package com.planetstudios.essentials.rendering.shaders;
import java.util.Set;
/** Immutable capability snapshot supplied by a real graphics backend. */
public record ShaderCapabilities(String backendName,int maxTextureSize,int maxColorAttachments,
 int maxVertexAttributes,Set<String> extensions,Set<String> features){
 public ShaderCapabilities{
  if(backendName==null||backendName.isBlank())throw new IllegalArgumentException("backendName");
  if(maxTextureSize<0||maxColorAttachments<0||maxVertexAttributes<0)throw new IllegalArgumentException("Negative capability limit");
  extensions=Set.copyOf(extensions==null?Set.of():extensions);
  features=Set.copyOf(features==null?Set.of():features);
 }
 public boolean supports(String feature){return features.contains(feature);}
 public boolean hasExtension(String extension){return extensions.contains(extension);}
 public static ShaderCapabilities conservative(String backend){return new ShaderCapabilities(backend,0,0,0,Set.of(),Set.of());}
}
