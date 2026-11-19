package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;
import java.nio.ByteOrder;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainVertexDataTest {
    private static PrismTerrainVertex v(float x,float y,float z,float u,float t) {
        return new PrismTerrainVertex(x,y,z,u,t,0xAABBCCDD,0x00F000F0,0,0x007F0000);
    }
    @Test void encodesVersionedThirtyTwoByteLayout() {
        var data=PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1)).build();
        assertEquals(1,data.layoutVersion()); assertEquals(32,data.strideBytes());
        assertEquals(128,data.byteSize()); assertEquals(4,data.vertexCount());
        assertEquals(ByteOrder.LITTLE_ENDIAN,data.readOnlyBuffer().order());
        assertTrue(data.readOnlyBuffer().isReadOnly());
    }
    @Test void payloadIsDefensiveAndBuilderIsSingleUse() {
        var b=PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(v(0,0,0,0,0),v(1,0,0,1,0),v(1,1,0,1,1),v(0,1,0,0,1));
        var data=b.build(); byte[] copy=data.bytes(); copy[0]=99;
        assertNotEquals(99,data.bytes()[0]); assertThrows(IllegalStateException.class,b::build);
    }
    @Test void rejectsNonFiniteUvAndOutOfSectionPosition() {
        assertThrows(IllegalArgumentException.class,()->v(Float.NaN,0,0,0,0));
        assertThrows(IllegalArgumentException.class,()->v(0,0,17,0,0));
        assertThrows(IllegalArgumentException.class,()->v(0,0,0,Float.POSITIVE_INFINITY,0));
    }
    @Test void rejectsEmptyPayloadAndUnsupportedStage() {
        var empty=PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES);
        assertThrows(IllegalStateException.class,empty::build);
        assertThrows(IllegalArgumentException.class,()->PrismTerrainVertexData.builder(0,0,0,"example:stone",PrismFrameStage.END_MAIN));
    }
}
