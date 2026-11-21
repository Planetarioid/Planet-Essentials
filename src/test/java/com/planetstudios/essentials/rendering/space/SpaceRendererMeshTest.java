package com.planetstudios.essentials.rendering.space;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertex;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpaceRendererMeshTest {
 @Test void buildsBoundsFromCapturedVertices(){
  var data=PrismTerrainVertexData.builder(2,3,4,"planet_essentials:terrain/vanilla/solid",PrismFrameStage.BEFORE_ENTITIES)
   .addQuad(new PrismTerrainVertex(1,2,3,0,0,-1,0,0,0),new PrismTerrainVertex(5,2,3,1,0,-1,0,0,0),
    new PrismTerrainVertex(5,6,3,1,1,-1,0,0,0),new PrismTerrainVertex(1,6,3,0,1,-1,0,0,0)).build();
  var mesh=SpaceRendererMesh.build(data);assertSame(data,mesh.source());assertEquals(4,mesh.vertexCount());
  assertEquals(1.0f,mesh.minX());assertEquals(2.0f,mesh.minY());assertEquals(3.0f,mesh.minZ());
  assertEquals(5.0f,mesh.maxX());assertEquals(6.0f,mesh.maxY());assertEquals(3.0f,mesh.maxZ());
 }
 @Test void rejectsNonFiniteVertexPositionsAtPayloadBoundary(){
  assertThrows(IllegalArgumentException.class,()->new PrismTerrainVertex(Float.NaN,0,0,0,0,-1,0,0,0));
 }
}
