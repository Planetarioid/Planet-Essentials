package com.planetstudios.essentials.rendering.space;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
/** Immutable CPU-prepared section geometry and local-space bounds. */
public final class SpaceRendererMesh {
 private final PrismTerrainVertexData source;
 private final float minX,minY,minZ,maxX,maxY,maxZ;
 private SpaceRendererMesh(PrismTerrainVertexData source,float minX,float minY,float minZ,float maxX,float maxY,float maxZ){
  this.source=source;this.minX=minX;this.minY=minY;this.minZ=minZ;this.maxX=maxX;this.maxY=maxY;this.maxZ=maxZ;
 }
 public static SpaceRendererMesh build(PrismTerrainVertexData source){
  Objects.requireNonNull(source,"source");
  if(source.vertexCount()<=0||source.strideBytes()<12)throw new IllegalArgumentException("Terrain payload has no valid position stream");
  ByteBuffer vertices=source.readOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);int stride=source.strideBytes();
  float minX=Float.POSITIVE_INFINITY,minY=Float.POSITIVE_INFINITY,minZ=Float.POSITIVE_INFINITY;
  float maxX=Float.NEGATIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;
  for(int i=0;i<source.vertexCount();i++){int o=i*stride;float x=vertices.getFloat(o),y=vertices.getFloat(o+4),z=vertices.getFloat(o+8);
   if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z))throw new IllegalArgumentException("Terrain payload contains a non-finite position");
   minX=Math.min(minX,x);minY=Math.min(minY,y);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);maxZ=Math.max(maxZ,z);
  }
  return new SpaceRendererMesh(source,minX,minY,minZ,maxX,maxY,maxZ);
 }
 public PrismTerrainVertexData source(){return source;} public int vertexCount(){return source.vertexCount();}
 public int quadCount(){return source.quadCount();} public String materialId(){return source.materialId();}
 public int sectionX(){return source.sectionX();} public int sectionY(){return source.sectionY();} public int sectionZ(){return source.sectionZ();}
 public float minX(){return minX;} public float minY(){return minY;} public float minZ(){return minZ;}
 public float maxX(){return maxX;} public float maxY(){return maxY;} public float maxZ(){return maxZ;}
}
