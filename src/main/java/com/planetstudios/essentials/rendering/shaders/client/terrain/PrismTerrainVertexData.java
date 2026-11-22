package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable, versioned full-attribute terrain payload with a stable little-endian encoding. */
public final class PrismTerrainVertexData {
    public static final int MAX_VERTICES = PrismTerrainMesh.MAX_VERTICES;
    private static final int STRIDE = PrismTerrainVertexLayout.STRIDE;
    private static final int MAX_BYTES = Math.multiplyExact(MAX_VERTICES, STRIDE);
    private final int sectionX, sectionY, sectionZ;
    private final String materialId;
    private final PrismFrameStage stage;
    private final byte[] bytes;
    private final int vertexCount;
    private final String contentHash;
    private final float[] quadCenters;

    private PrismTerrainVertexData(int x, int y, int z, String material, PrismFrameStage stage,
            byte[] bytes, int count, float[] quadCenters) {
        sectionX=x; sectionY=y; sectionZ=z; materialId=material; this.stage=stage;
        // Builder transfers ownership of these freshly allocated immutable arrays.
        this.bytes=bytes; vertexCount=count; this.quadCenters=quadCenters; contentHash=hash(this.bytes);
    }
    public static Builder builder(int x,int y,int z,String material,PrismFrameStage stage) {
        return new Builder(x,y,z,material,stage);
    }
    public int sectionX(){return sectionX;} public int sectionY(){return sectionY;} public int sectionZ(){return sectionZ;}
    public String materialId(){return materialId;} public PrismFrameStage stage(){return stage;}
    public int layoutVersion(){return PrismTerrainVertexLayout.VERSION;}
    public int strideBytes(){return STRIDE;}
    public int vertexCount(){return vertexCount;} public int byteSize(){return bytes.length;}
    public long estimatedCpuBytes(){return bytes.length + (quadCenters == null ? 0L : (long) quadCenters.length * Float.BYTES);}
    public boolean hasQuadCenters(){return quadCenters != null;}
    public int quadCount(){return vertexCount / 4;}
    public float quadCenterX(int index){return center(index,0);}
    public float quadCenterY(int index){return center(index,1);}
    public float quadCenterZ(int index){return center(index,2);}
    private float center(int index,int component){
        if(quadCenters==null)throw new IllegalStateException("Terrain payload has no translucent sort centers");
        if(index<0||index>=quadCount())throw new IndexOutOfBoundsException(index);
        return quadCenters[index*3+component];
    }
    public String contentHash(){return contentHash;}
    public byte[] bytes(){return bytes.clone();}
    public ByteBuffer readOnlyBuffer(){return ByteBuffer.wrap(bytes).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);}
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 is unavailable", impossible); }
    }

    public static final class Builder {
        private final int x,y,z;
        private final String material;
        private final PrismFrameStage stage;
        private ByteBuffer data=ByteBuffer.allocate(16*STRIDE).order(ByteOrder.LITTLE_ENDIAN);
        private int vertices;
        private boolean built;
        private float[] quadCenters;
        private Boolean appendedPayloadsHaveCenters;
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
            if (appendedPayloadsHaveCenters != null) throw new IllegalStateException("Cannot add individual quads after appending payloads");
            Objects.requireNonNull(a,"a"); Objects.requireNonNull(b,"b");
            Objects.requireNonNull(c,"c"); Objects.requireNonNull(d,"d");
            if(vertices>MAX_VERTICES-4) throw new IllegalStateException("Terrain vertex limit exceeded");
            ensureCapacityForQuad();
            put(a); put(b); put(c); put(d); vertices+=4; return this;
        }
        /** Stores vanilla translucent quad centroids in section-local XYZ order. */
        public Builder quadCenters(float[] centers) {
            ensureMutable();
            Objects.requireNonNull(centers,"centers");
            if(quadCenters!=null)throw new IllegalStateException("Quad centers have already been supplied");
            if(centers.length==0 || centers.length%3!=0)throw new IllegalArgumentException("Quad centers must contain XYZ triples");
            for(float value:centers)if(!Float.isFinite(value))throw new IllegalArgumentException("Quad centers must be finite");
            quadCenters=centers.clone();
            return this;
        }
        /** Appends a compatible immutable payload while preserving optional translucent sort centers. */
        public Builder append(PrismTerrainVertexData source) {
            ensureMutable();
            Objects.requireNonNull(source, "source");
            if (source.sectionX != x || source.sectionY != y || source.sectionZ != z
                    || !source.materialId.equals(material) || source.stage != stage) {
                throw new IllegalArgumentException("Terrain payload metadata must match before appending");
            }
            if (source.vertexCount == 0 || source.vertexCount % 4 != 0
                    || vertices > MAX_VERTICES - source.vertexCount) {
                throw new IllegalArgumentException("Appended terrain payload has an invalid or oversized vertex count");
            }
            if (quadCenters != null && appendedPayloadsHaveCenters == null) {
                throw new IllegalStateException("Append must be the first geometry operation on this builder");
            }
            boolean hasCenters = source.quadCenters != null;
            if (appendedPayloadsHaveCenters != null && appendedPayloadsHaveCenters != hasCenters) {
                throw new IllegalArgumentException("Appended terrain payloads must agree on translucent sort centers");
            }
            if (vertices > 0 && appendedPayloadsHaveCenters == null) {
                throw new IllegalStateException("Append must be the first geometry operation on this builder");
            }
            if (appendedPayloadsHaveCenters == null) appendedPayloadsHaveCenters = hasCenters;
            ensureCapacityForBytes(Math.addExact(data.position(), source.bytes.length));
            data.put(source.bytes);
            vertices += source.vertexCount;
            if (hasCenters) {
                int oldLength = quadCenters == null ? 0 : quadCenters.length;
                float[] combined = new float[Math.addExact(oldLength, source.quadCenters.length)];
                if (quadCenters != null) System.arraycopy(quadCenters, 0, combined, 0, oldLength);
                System.arraycopy(source.quadCenters, 0, combined, oldLength, source.quadCenters.length);
                quadCenters = combined;
            }
            return this;
        }
        private void ensureCapacityForQuad() {
            ensureCapacityForBytes(Math.addExact(data.position(), 4*STRIDE));
        }
        private void ensureCapacityForBytes(int required) {
            if(required>MAX_BYTES)throw new IllegalStateException("Terrain vertex byte limit exceeded");
            if(required<=data.capacity())return;
            int capacity=Math.min(MAX_BYTES,Math.max(required,Math.multiplyExact(data.capacity(),2)));
            ByteBuffer expanded=ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
            data.flip();
            expanded.put(data);
            data=expanded;
        }
        private void put(PrismTerrainVertex v) {
            data.putFloat(v.x()).putFloat(v.y()).putFloat(v.z()).putFloat(v.u()).putFloat(v.v());
            data.putInt(v.colorRgba8()).putInt(v.packedLight()).putInt(v.packedOverlay()).putInt(v.packedNormal());
        }
        private void ensureMutable(){if(built)throw new IllegalStateException("Terrain vertex builder has already been built");}
        public PrismTerrainVertexData build() {
            ensureMutable(); if(vertices==0)throw new IllegalStateException("Cannot build empty terrain vertex data");
            if(quadCenters!=null && quadCenters.length!=(vertices/4)*3)
                throw new IllegalStateException("Translucent sort-center count must match the terrain quad count");
            built=true; byte[] result=new byte[data.position()]; data.flip(); data.get(result);
            data=null;
            return new PrismTerrainVertexData(x,y,z,material,stage,result,vertices,quadCenters);
        }
    }
}
