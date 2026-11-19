package com.planetstudios.essentials.client.mixin;

import com.mojang.math.Axis;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainActiveFrustum;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the live culling frustum prepared for the current LevelRenderer frame. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "prepareCullFrustum", at = @At("RETURN"))
    private void planetEssentials$captureActiveFrustum(Matrix4f modelViewMatrix,
            Matrix4f projectionMatrixForCulling, Vec3 cameraPosition,
            CallbackInfoReturnable<Frustum> callback) {
        PrismTerrainActiveFrustum.publish(callback.getReturnValue());
    }
}
