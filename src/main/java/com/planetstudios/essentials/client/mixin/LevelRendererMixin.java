package com.planetstudios.essentials.client.mixin;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import com.planetstudios.essentials.rendering.space.SpaceRendererCaptureRuntime;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainActiveFrustum;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainChunkLifecycleEvents;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVanillaDrawIndex;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVisibleSections;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures terrain snapshots and advances bounded Space Renderer GPU uploads on the render thread. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    private static final int SPACE_UPLOADS_PER_FRAME = 24;
    private static final long SPACE_UPLOAD_BYTES_PER_FRAME = 8L * 1024L * 1024L;

    @Shadow @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    private ClientLevel level;

    @Inject(method = "prepareCullFrustum", at = @At("RETURN"))
    private void planetEssentials$captureActiveFrustum(Matrix4f modelViewMatrix,
            Matrix4f projectionMatrixForCulling, net.minecraft.world.phys.Vec3 cameraPosition,
            CallbackInfoReturnable<Frustum> callback) {
        if (!ShaderPackRuntime.isEnabled()) return;
        PrismTerrainActiveFrustum.publish(callback.getReturnValue());
    }

    @Inject(method = "cullTerrain", at = @At("RETURN"))
    private void planetEssentials$captureVisibleSections(Camera camera, Frustum frustum,
            boolean spectator, CallbackInfo callback) {
        if (!ShaderPackRuntime.isEnabled()) return;
        PrismTerrainVisibleSections.publish(Minecraft.getInstance().level, visibleSections);
    }

    /** Advances uploads and captures the complete vanilla draw order for Space Renderer preflight. */
    @Inject(method = "prepareChunkRenders", at = @At("RETURN"))
    private void planetEssentials$indexVanillaTerrainDraws(Matrix4fc modelViewMatrix,
            double camX, double camY, double camZ,
            CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        SpaceRendererCaptureRuntime.drainCompletions();
        GpuDevice device = RenderSystem.getDevice();
        if (device != null) {
            SpaceRendererCaptureRuntime.uploadPending(
                    device, SPACE_UPLOADS_PER_FRAME, SPACE_UPLOAD_BYTES_PER_FRAME);
        }
        PrismTerrainVanillaDrawIndex.capture(visibleSections, callback.getReturnValue());
        SpaceRendererCaptureRuntime.captureVisibleFrame(
                this.level, callback.getReturnValue(), PrismTerrainVanillaDrawIndex.snapshot());
        if (!ShaderPackRuntime.isEnabled()) return;
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void planetEssentials$clearWorldSnapshots(ClientLevel level, CallbackInfo callback) {
        ClientLevel previousLevel = this.level;
        if (previousLevel != null && previousLevel != level) {
            PrismTerrainChunkLifecycleEvents.clearWorld(previousLevel);
            SpaceRendererCaptureRuntime.clearWorld(previousLevel);
        }
        PrismTerrainVisibleSections.clear();
        PrismTerrainActiveFrustum.clear();
        PrismTerrainVanillaDrawIndex.clear();
    }
}
