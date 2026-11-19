package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/** Immutable, versioned full-attribute terrain payload with a stable little-endian encoding. */
public final class PrismTerrainVertexData {
    public static final int MAX_VERTICES = PrismTerrainMesh.MAX_VERTICES;
    private final int sectionX, sectionY, sectionZ;
    private final String materialId;
    private final PrismFrameStage stage;
    private final byte[] bytes;
    private final int vertexCount;

    private PrismTerrainVertexData(int x, int y, int z, String material, PrismFrameStage stage,
            byte[] bytes, int count) {
        sectionX=x; sectionY=y; sectionZ=z; materialId=material; this.stage=stage;
        this.bytes=bytes.clone(); vertexCount=count;
    }
    public static Builder builder(int x,int y,int z,String material,PrismFrameStage stage) {
        return new Builder(x,y,z,material,stage);
    }
    public int sectionX(){return sectionX;} public int sectionY(){return sectionY;} public int sectionZ(){return sectionZ;}
    public String materialId(){return materialId;} public PrismFrameStage stage(){return stage;}
    public int layoutVersion(){return PrismTerrainVertexLayout.VERSION;}
    public int strideBytes(){return PrismTerrainVertexLayout.STRIDE;}
    public int vertexCount(){return vertexCount;} public int byteSize(){return bytes.length;}
    public long estimatedCpuBytes(){return bytes.length;}
    public byte[] bytes(){return bytes.clone();}
    public ByteBuffer readOnlyBuffer(){return ByteBuffer.wrap(bytes).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);}

    public static final class Builder {
        private final int x,y,z;
        private final String material;
        private final PrismFrameStage stage;
        private final ByteBuffer data=ByteBuffer.allocate(MAX_VERTICES*PrismTerrainVertexLayout.STRIDE)
                .order(ByteOrder.LITTLE_ENDIAN);
        private int vertices;
        private boolean built;
        private Builder(int x,int y,int z,String material,PrismFrameStage stage) {
            this.x=x; this.y=y; this.z=z; this.material=Objects.requireNonNull(material,"material");
            this.stage=Objects.requireNonNull(stage,"stage");
            if(!material.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || material.contains("..") || material.contains("//"))
                throw new IllegalArgumentException("Invalid Prism terrain material ID: "+material);
            if(stage!=PrismFrameStage.BEFORE_ENTITIES && stage!=PrismFrameStage.BEFORE_TRANSLUCENT)
                throw new IllegalArgumentException("Unsupported terrain stage: "+stage);
        }
        public Builder addQuad(PrismTerrainVertex a,PrismTerrainVertex b,PrismTerrainVertex c,PrismTerrainVertex d) {
            ensureMutable();
            Objects.requireNonNull(a,"a"); Objects.requireNonNull(b,"b");
            Objects.requireNonNull(c,"c"); Objects.requireNonNull(d,"d");
            if(vertices>MAX_VERTICES-4) throw new IllegalStateException("Terrain vertex limit exceeded");
            put(a); put(b); put(c); put(d); vertices+=4; return this;
        }
        private void put(PrismTerrainVertex v) {
            data.putFloat(v.x()).putFloat(v.y()).putFloat(v.z()).putFloat(v.u()).putFloat(v.v());
            data.putInt(v.colorRgba8()).putInt(v.packedLight()).putInt(v.packedOverlay()).putInt(v.packedNormal());
        }
        private void ensureMutable(){if(built)throw new IllegalStateException("Terrain vertex builder has already been built");}
        public PrismTerrainVertexData build() {
            ensureMutable(); if(vertices==0)throw new IllegalStateException("Cannot build empty terrain vertex data");
            built=true; byte[] result=new byte[data.position()]; data.flip(); data.get(result);
            return new PrismTerrainVertexData(x,y,z,material,stage,result,vertices);
        }
    }
}
