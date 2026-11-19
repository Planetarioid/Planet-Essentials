package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBackend;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBindings;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;

/** Opt-in GPU terrain adapter for cooperating providers and captured vanilla payloads. */
public final class PrismTerrainRendererAdapter implements AutoCloseable {
    private static final String DYNAMIC_TRANSFORMS = "DynamicTransforms";
    private static final Set<String> AUTOMATIC_UNIFORMS = Set.of("Projection", "Globals", "Fog");
    private static final int MAX_FRAME_PAYLOADS = 512;
    private static final int MAX_FRAME_VERTICES = 1_048_576;
    private static final int SECTION_UNIFORM_BYTES = PrismTerrainSectionUniform.BYTE_SIZE;
    private static final int UNIFORM_RING_BYTES = MAX_FRAME_PAYLOADS * SECTION_UNIFORM_BYTES;
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private final String programId;
    private final PrismFrameStage stage;
    private final Provider provider;
    private final Map<ClientLevel, PrismTerrainGpuCache> worldCaches = new IdentityHashMap<>();
    private volatile List<PrismTerrainVertexData> frame = List.of();
    private boolean extracting;
    private volatile boolean closed;
    private MappableRingBuffer sectionUniformRing;
    private GpuDevice uniformOwnerDevice;
    private PrismTerrainFrameCoordinator.Registration extractionRegistration;
    private PrismTerrainFrameCoordinator.Registration drawRegistration;
    private PrismTerrainChunkLifecycleEvents.Registration invalidatorRegistration;

    private PrismTerrainRendererAdapter(String programId, PrismFrameStage stage, Provider provider) {
        this.programId = Objects.requireNonNull(programId, "programId");
        this.stage = Objects.requireNonNull(stage, "stage");
        this.provider = Objects.requireNonNull(provider, "provider");
        if (programId.isBlank()) throw new IllegalArgumentException("Terrain program ID cannot be blank");
        if (stage != PrismFrameStage.BEFORE_ENTITIES && stage != PrismFrameStage.BEFORE_TRANSLUCENT) {
            throw new IllegalArgumentException("Terrain adapter stage must be BEFORE_ENTITIES or BEFORE_TRANSLUCENT");
        }
    }

    public static PrismTerrainRendererAdapter register(String programId, PrismFrameStage stage, Provider provider) {
        PrismTerrainRendererAdapter adapter = new PrismTerrainRendererAdapter(programId, stage, provider);
        adapter.extractionRegistration = PrismTerrainFrameCoordinator.registerExtractor(adapter::extract);
        adapter.drawRegistration = PrismTerrainFrameCoordinator.registerRenderer(stage, adapter::draw);
        adapter.invalidatorRegistration = PrismTerrainChunkLifecycleEvents.registerGpuInvalidator(
                new PrismTerrainChunkLifecycleEvents.GpuInvalidator() {
                    @Override public void invalidateChunk(ClientLevel level, int chunkX, int chunkZ,
                            int minSectionY, int maxSectionY) {
                        PrismTerrainGpuCache cache = adapter.cacheIfPresent(level);
                        if (cache != null) cache.invalidateChunk(chunkX, chunkZ);
                    }
                    @Override public void clearWorld(ClientLevel level) { adapter.clearWorldCache(level); }
                });
        return adapter;
    }

    /** Provider that streams the current world's automatically captured vanilla layer into this adapter. */
    public static Provider capturedVanilla(String materialId) {
        Objects.requireNonNull(materialId, "materialId");
        return (context, submit) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            for (PrismTerrainVertexData payload : PrismTerrainChunkLifecycleEvents.snapshot(level)) {
                if (payload.materialId().equals(materialId)) submit.accept(payload);
            }
        };
    }

    private void extract(WorldExtractionContext context) {
        if (closed) return;
        List<PrismTerrainVertexData> next = new ArrayList<>();
        int[] totalVertices = {0};
        extracting = true;
        try {
            provider.extract(context, data -> {
                if (!extracting) throw new IllegalStateException("Terrain payload submitted outside extraction");
                PrismTerrainVertexData payload = Objects.requireNonNull(data, "terrain payload");
                if (payload.stage() != stage) return;
                if (next.size() >= MAX_FRAME_PAYLOADS
                        || (long) totalVertices[0] + payload.vertexCount() > MAX_FRAME_VERTICES) {
                    throw new IllegalStateException("Prism terrain frame exceeded its payload or vertex budget");
                }
                next.add(payload);
                totalVertices[0] += payload.vertexCount();
            });
            frame = List.copyOf(next);
        } catch (RuntimeException failure) {
            frame = List.of();
            PlanetEssentials.LOGGER.error("Prism terrain provider '{}' failed extraction", programId, failure);
        } finally {
            extracting = false;
        }
    }

    private void draw(WorldRenderContext context, PrismTerrainFrame ignored) {
        if (closed || !ShaderPackRuntime.isEnabled()) return;
        RenderSystem.assertOnRenderThread();
        List<PrismTerrainVertexData> payloads = frame;
        if (payloads.isEmpty()) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        MinecraftShaderBackend.MinecraftCompiledProgram program = ShaderPackRuntime.terrainProgram(programId);
        if (program == null || !program.supportsTerrainV1()) return;

        Set<String> declaredUniforms = Set.copyOf(program.bindings().uniformBuffers());
        Set<String> requiredUniforms = Set.of(PrismTerrainSectionUniform.BINDING_NAME, DYNAMIC_TRANSFORMS);
        Set<String> nonDefaultUniforms = new HashSet<>(declaredUniforms);
        nonDefaultUniforms.removeAll(requiredUniforms);
        if (!declaredUniforms.containsAll(requiredUniforms)
                || !AUTOMATIC_UNIFORMS.containsAll(nonDefaultUniforms)
                || !program.bindings().texelBuffers().isEmpty()
                || !program.bindings().samplers().isEmpty()) {
            PlanetEssentials.LOGGER.warn(
                    "Prism terrain adapter '{}' requires PrismSection and DynamicTransforms; only Projection, Globals, and Fog may be supplied by Minecraft",
                    programId);
            return;
        }
        Set<String> defaultUniforms = Set.copyOf(nonDefaultUniforms);

        var camera = context.worldState().cameraRenderState;
        if (camera == null || !camera.initialized || camera.pos == null) return;
        double cameraX = camera.pos.x;
        double cameraY = camera.pos.y;
        double cameraZ = camera.pos.z;

        GpuDevice device = RenderSystem.getDevice();
        PrismTerrainGpuCache cache = cacheFor(level);
        RenderTarget target = OutputTarget.MAIN_TARGET.getRenderTarget();
        if (target == null || target.getColorTextureView() == null) return;
        ensureUniformRing(device);

        var dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        MinecraftShaderBindings baseBindings = MinecraftShaderBindings.builder()
                .uniformBuffer(DYNAMIC_TRANSFORMS, dynamicTransforms)
                .build();

        GpuBuffer uniformBuffer = sectionUniformRing.currentBuffer();
        CommandEncoder encoder = device.createCommandEncoder();
        int uniformOffset = 0;
        boolean passOpened = false;
        try (RenderPass pass = encoder.createRenderPass(
                () -> "Planet Prism terrain " + programId,
                target.getColorTextureView(), OptionalInt.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            passOpened = true;
            RenderSystem.bindDefaultUniforms(pass);
            for (PrismTerrainVertexData data : payloads) {
                if (!PrismTerrainActiveFrustum.isVisible(data.sectionX(), data.sectionY(), data.sectionZ())) continue;
                PrismTerrainGpuMesh mesh = cache.getOrUpload(device, data);
                if (mesh == null) continue;
                PrismTerrainCameraTransform transform = PrismTerrainCameraTransform.forSection(
                        data.sectionX(), data.sectionY(), data.sectionZ(), cameraX, cameraY, cameraZ);
                var uniformPayload = PrismTerrainSectionUniform.encode(transform);
                GpuBufferSlice uniformSlice = uniformBuffer.slice(uniformOffset, SECTION_UNIFORM_BYTES);
                try (GpuBuffer.MappedView mapped = encoder.mapBuffer(uniformSlice, false, true)) {
                    mapped.data().put(uniformPayload);
                }
                MinecraftShaderBindings sectionBindings = baseBindings.withUniformBuffer(
                        PrismTerrainSectionUniform.BINDING_NAME, uniformSlice);
                PrismTerrainDrawExecutor.draw(pass, program, mesh, sectionBindings, defaultUniforms);
                uniformOffset += SECTION_UNIFORM_BYTES;
            }
        } catch (RuntimeException failure) {
            PlanetEssentials.LOGGER.error("Prism terrain adapter '{}' failed draw submission", programId, failure);
        } finally {
            if (passOpened) sectionUniformRing.rotate();
        }
    }

    private synchronized void ensureUniformRing(GpuDevice device) {
        if (uniformOwnerDevice == device && sectionUniformRing != null) return;
        if (sectionUniformRing != null) sectionUniformRing.close();
        uniformOwnerDevice = device;
        sectionUniformRing = new MappableRingBuffer(
                () -> "Planet Prism terrain section uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                UNIFORM_RING_BYTES);
    }

    private synchronized PrismTerrainGpuCache cacheFor(ClientLevel level) {
        if (closed) throw new IllegalStateException("Prism terrain adapter is closed");
        return worldCaches.computeIfAbsent(level, ignored -> new PrismTerrainGpuCache());
    }

    private synchronized PrismTerrainGpuCache cacheIfPresent(ClientLevel level) {
        return worldCaches.get(level);
    }

    private synchronized void clearWorldCache(ClientLevel level) {
        PrismTerrainGpuCache cache = worldCaches.remove(level);
        if (cache != null) cache.close();
    }

    public synchronized int cachedWorldCount() { return worldCaches.size(); }

    @Override public void close() {
        if (closed) return;
        closed = true;
        if (extractionRegistration != null) extractionRegistration.close();
        if (drawRegistration != null) drawRegistration.close();
        if (invalidatorRegistration != null) invalidatorRegistration.close();
        Runnable release = () -> {
            synchronized (PrismTerrainRendererAdapter.this) {
                for (PrismTerrainGpuCache cache : worldCaches.values()) cache.close();
                worldCaches.clear();
                if (sectionUniformRing != null) {
                    sectionUniformRing.close();
                    sectionUniformRing = null;
                    uniformOwnerDevice = null;
                }
                frame = List.of();
            }
        };
        if (RenderSystem.isOnRenderThread()) release.run();
        else RenderSystem.queueFencedTask(release::run);
    }

    @FunctionalInterface
    public interface Provider {
        void extract(WorldExtractionContext context, Consumer<PrismTerrainVertexData> submit);
    }
}
