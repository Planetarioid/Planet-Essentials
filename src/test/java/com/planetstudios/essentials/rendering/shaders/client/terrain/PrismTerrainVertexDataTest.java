package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;
import java.nio.ByteOrder;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainVertexDataTest {
    private static PrismTerrainVertex v(float x,float y,float z,float u,float t) {
        return new PrismTerrainVertex(x,y,z,u,t,0xAABBCCDD,0x00F000F0,0,0x007F0000);
    }
    @Test void encodesVersionedThirtySixByteLayout() {
        var data=PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1)).build();
        assertEquals(1,data.layoutVersion()); assertEquals(36,data.strideBytes());
        assertEquals(144,data.byteSize()); assertEquals(4,data.vertexCount());
        assertEquals(ByteOrder.LITTLE_ENDIAN,data.readOnlyBuffer().order());
        assertTrue(data.readOnlyBuffer().isReadOnly());
        assertEquals(64, data.contentHash().length());
    }
    @Test void payloadIsDefensiveAndBuilderIsSingleUse() {
        var b=PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1));
        var data=b.build(); byte[] copy=data.bytes(); copy[0]=99;
        assertNotEquals(99,data.bytes()[0]); assertThrows(IllegalStateException.class,b::build);
    }
    @Test void contentHashIsStableAndChangesWithVertexPayload() {
        var first = PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1)).build();
        var same = PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1)).build();
        var changed = PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(v(0,0,0,0,0),v(2,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1)).build();
        assertEquals(first.contentHash(), same.contentHash());
        assertNotEquals(first.contentHash(), changed.contentHash());
    }
    @Test void retainsImmutableTranslucentQuadCentersInCpuBudget() {
        float[] centers={0,0,1, 0,0,8};
        var builder=PrismTerrainVertexData.builder(0,0,0,"example:glass",PrismFrameStage.BEFORE_TRANSLUCENT)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1))
                .addQuad(v(0,0,8,0,0),v(1,0,8,1,0),v(1,1,8,1,1),v(0,1,8,0,1))
                .quadCenters(centers);
        centers[2]=99;
        var data=builder.build();
        assertTrue(data.hasQuadCenters());
        assertEquals(2,data.quadCount());
        assertEquals(1,data.quadCenterZ(0));
        assertEquals(8,data.quadCenterZ(1));
        assertEquals(data.byteSize()+6*Float.BYTES,data.estimatedCpuBytes());
    }
    @Test void appendsCompatiblePayloadsAndPreservesTheirTranslucentCenters() {
        var first = PrismTerrainVertexData.builder(2,3,4,"minecraft:water",PrismFrameStage.BEFORE_TRANSLUCENT)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1))
                .quadCenters(new float[]{0.5f,0.5f,0.0f}).build();
        var second = PrismTerrainVertexData.builder(2,3,4,"minecraft:water",PrismFrameStage.BEFORE_TRANSLUCENT)
                .addQuad(v(2,0,0,0,0),v(3,0,0,1,0),v(3,1,0,1,1),v(2,1,0,0,1))
                .quadCenters(new float[]{2.5f,0.5f,0.0f}).build();

        var merged = PrismTerrainVertexData.builder(2,3,4,"minecraft:water",PrismFrameStage.BEFORE_TRANSLUCENT)
                .append(first).append(second).build();

        assertEquals(8, merged.vertexCount());
        assertEquals(2, merged.quadCount());
        assertTrue(merged.hasQuadCenters());
        assertEquals(0.5f, merged.quadCenterX(0));
        assertEquals(2.5f, merged.quadCenterX(1));
        assertEquals(first.byteSize() + second.byteSize(), merged.byteSize());
    }
    @Test void rejectsAppendingPayloadsWithDifferentSectionMaterialStageOrSortMetadata() {
        var first = PrismTerrainVertexData.builder(0,0,0,"example:glass",PrismFrameStage.BEFORE_TRANSLUCENT)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1))
                .quadCenters(new float[]{0.5f,0.5f,0.0f}).build();
        var wrongSection = PrismTerrainVertexData.builder(1,0,0,"example:glass",PrismFrameStage.BEFORE_TRANSLUCENT)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1))
                .quadCenters(new float[]{0.5f,0.5f,0.0f}).build();
        var noCenters = PrismTerrainVertexData.builder(0,0,0,"example:glass",PrismFrameStage.BEFORE_TRANSLUCENT)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1)).build();
        var builder = PrismTerrainVertexData.builder(0,0,0,"example:glass",PrismFrameStage.BEFORE_TRANSLUCENT);
        assertThrows(IllegalArgumentException.class, () -> builder.append(wrongSection));
        assertThrows(IllegalArgumentException.class, () -> builder.append(noCenters));
    }
    @Test void growsItsBackingStorageOnlyAsRequiredForLargerVanillaSections() {
        var builder=PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES);
        for(int i=0;i<20;i++) {
            float x=(i%4);
            builder.addQuad(v(x,0,0,0,0),v(x+1,0,0,1,0),v(x+1,1,0,1,1),v(x,1,0,0,1));
        }
        var data=builder.build();
        assertEquals(80,data.vertexCount());
        assertEquals(80*36,data.byteSize());
    }
    @Test void preservesFiniteGeometryThatExtendsBeyondSectionBounds() {
        assertEquals(-0.25f, v(-0.25f, 0, 0, 0, 0).x());
        assertEquals(16.25f, v(16.25f, 0, 0, 0, 0).x());
        assertEquals(17.0f, v(0, 0, 17, 0, 0).z());
    }
    @Test void rejectsNonFinitePositionsAndUvCoordinates() {
        assertThrows(IllegalArgumentException.class,()->v(Float.NaN,0,0,0,0));
        assertThrows(IllegalArgumentException.class,()->v(0,Float.POSITIVE_INFINITY,0,0,0));
        assertThrows(IllegalArgumentException.class,()->v(0,0,0,Float.POSITIVE_INFINITY,0));
    }
    @Test void rejectsEmptyPayloadAndUnsupportedStage() {
        var empty=PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES);
        assertThrows(IllegalStateException.class,empty::build);
        assertThrows(IllegalArgumentException.class,()->PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.END_MAIN));
    }
}
