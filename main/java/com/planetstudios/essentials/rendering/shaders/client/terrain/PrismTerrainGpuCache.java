package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Render-thread-confined, byte-bounded LRU cache for uploaded terrain vertex buffers. */
public final class PrismTerrainGpuCache implements AutoCloseable {
    public static final long DEFAULT_MAX_BYTES=128L*1024L*1024L;
    private final long maxBytes;
    private final LinkedHashMap<Key,PrismTerrainGpuMesh> entries=new LinkedHashMap<>(16,0.75f,true);
    private long currentBytes;
    private boolean closed;

    public PrismTerrainGpuCache(){this(DEFAULT_MAX_BYTES);}
    public PrismTerrainGpuCache(long maxBytes){
        if(maxBytes<=0)throw new IllegalArgumentException("GPU cache byte budget must be positive");
        this.maxBytes=maxBytes;
    }

    /** Returns an existing allocation when its immutable source payload is unchanged. */
    public PrismTerrainGpuMesh getOrUpload(GpuDevice device, PrismTerrainVertexData data) {
        RenderSystem.assertOnRenderThread(); ensureOpen();
        Objects.requireNonNull(data, "data");
        PrismTerrainGpuMesh existing = entries.get(Key.of(data));
        if (existing != null && existing.matches(data)) return existing;
        if (!put(device, data)) return null;
        return entries.get(Key.of(data));
    }

    /** Uploads first, then atomically replaces/evicts entries after successful allocation. */
    public boolean put(GpuDevice device,PrismTerrainVertexData data){
        RenderSystem.assertOnRenderThread(); ensureOpen();
        Objects.requireNonNull(data,"data");
        // The GPU budget accounts only for the uploaded vertex allocation.
        // estimatedCpuBytes() also includes optional CPU-side translucent sort
        // centers, which are not uploaded and must not reject a valid mesh.
        PrismTerrainGpuMesh replacement=PrismTerrainGpuMesh.upload(device,data);
        if (replacement.gpuBytes() > maxBytes) {
            replacement.close();
            return false;
        }
        Key key=Key.of(data);
        PrismTerrainGpuMesh previous=entries.remove(key);
        if(previous!=null){
            currentBytes-=previous.gpuBytes();
            try {
                previous.close();
            } catch (RuntimeException | Error failure) {
                closeAfterFailure(replacement, failure);
                throw failure;
            }
        }
        while(currentBytes>maxBytes-replacement.gpuBytes()){
            Iterator<Map.Entry<Key,PrismTerrainGpuMesh>> it=entries.entrySet().iterator();
            if(!it.hasNext())break;
            PrismTerrainGpuMesh eldest=it.next().getValue();
            currentBytes-=eldest.gpuBytes();
            it.remove();
            try {
                eldest.close();
            } catch (RuntimeException | Error failure) {
                closeAfterFailure(replacement, failure);
                throw failure;
            }
        }
        entries.put(key,replacement);
        currentBytes+=replacement.gpuBytes();
        return true;
    }

    /** Borrowed mesh; do not close it. Access updates LRU order. */
    public PrismTerrainGpuMesh get(int x,int y,int z,String material,PrismFrameStage stage){
        RenderSystem.assertOnRenderThread(); ensureOpen();
        return entries.get(new Key(x,y,z,Objects.requireNonNull(material,"material"),Objects.requireNonNull(stage,"stage")));
    }

    public int invalidateSection(int x,int y,int z){
        RenderSystem.assertOnRenderThread(); ensureOpen();
        List<PrismTerrainGpuMesh> removed = new ArrayList<>();
        Iterator<Map.Entry<Key,PrismTerrainGpuMesh>> it=entries.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Key,PrismTerrainGpuMesh> entry=it.next();
            Key key=entry.getKey();
            if(key.x==x&&key.y==y&&key.z==z){
                PrismTerrainGpuMesh mesh=entry.getValue();
                currentBytes-=mesh.gpuBytes();
                it.remove();
                removed.add(mesh);
            }
        }
        closeAll(removed);
        return removed.size();
    }

    /** Invalidates all vertical section allocations for one chunk. */
    public int invalidateChunk(int chunkX, int chunkZ) {
        RenderSystem.assertOnRenderThread(); ensureOpen();
        List<PrismTerrainGpuMesh> removed = new ArrayList<>();
        Iterator<Map.Entry<Key,PrismTerrainGpuMesh>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Key,PrismTerrainGpuMesh> entry = it.next();
            Key key = entry.getKey();
            if (key.x == chunkX && key.z == chunkZ) {
                PrismTerrainGpuMesh mesh=entry.getValue();
                currentBytes -= mesh.gpuBytes();
                it.remove();
                removed.add(mesh);
            }
        }
        closeAll(removed);
        return removed.size();
    }

    public void clear(){
        RenderSystem.assertOnRenderThread();
        if(closed)return;
        releaseAll();
    }

    public int size(){RenderSystem.assertOnRenderThread();ensureOpen();return entries.size();}
    public long currentBytes(){RenderSystem.assertOnRenderThread();ensureOpen();return currentBytes;}
    public long maxBytes(){return maxBytes;}
    private void ensureOpen(){if(closed)throw new IllegalStateException("Prism terrain GPU cache is closed");}

    private void releaseAll() {
        List<PrismTerrainGpuMesh> removed = new ArrayList<>(entries.values());
        entries.clear();
        currentBytes = 0;
        closeAll(removed);
    }

    private static void closeAll(Iterable<PrismTerrainGpuMesh> meshes) {
        Throwable failure = null;
        for (PrismTerrainGpuMesh mesh : meshes) {
            try {
                mesh.close();
            } catch (RuntimeException | Error closeFailure) {
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }
        }
        rethrow(failure);
    }

    private static void closeAfterFailure(PrismTerrainGpuMesh mesh, Throwable failure) {
        try {
            mesh.close();
        } catch (RuntimeException | Error closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof Error error) throw error;
        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
    }

    @Override public void close(){
        RenderSystem.assertOnRenderThread();
        if(closed)return;
        closed=true;
        releaseAll();
    }

    private record Key(int x,int y,int z,String material,PrismFrameStage stage){
        static Key of(PrismTerrainVertexData data){
            return new Key(data.sectionX(),data.sectionY(),data.sectionZ(),data.materialId(),data.stage());
        }
    }
}
