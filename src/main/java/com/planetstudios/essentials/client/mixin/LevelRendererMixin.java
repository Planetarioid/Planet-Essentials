package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainActiveFrustum;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVisibleSections;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the live culling frustum and vanilla's section-occlusion visibility result. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Inject(method = "prepareCullFrustum", at = @At("RETURN"))
    private void planetEssentials$captureActiveFrustum(Matrix4f modelViewMatrix,
            Matrix4f projectionMatrixForCulling, net.minecraft.world.phys.Vec3 cameraPosition,
            CallbackInfoReturnable<Frustum> callback) {
        PrismTerrainActiveFrustum.publish(callback.getReturnValue());
    }

    /** cullTerrain runs the vanilla SectionOcclusionGraph update before this return hook. */
    @Inject(method = "cullTerrain", at = @At("RETURN"))
    private void planetEssentials$captureVisibleSections(Camera camera, Frustum frustum,
            boolean spectator, CallbackInfo callback) {
        PrismTerrainVisibleSections.publish(Minecraft.getInstance().level, visibleSections);
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void planetEssentials$clearWorldSnapshots(ClientLevel level, CallbackInfo callback) {
        PrismTerrainVisibleSections.clear();
        PrismTerrainActiveFrustum.clear();
    }
}
