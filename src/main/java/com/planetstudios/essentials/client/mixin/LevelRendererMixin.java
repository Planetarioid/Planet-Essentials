package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainActiveFrustum;
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

/** Captures live culling state and indexes vanilla's final terrain draw records. */
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

    @Inject(method = "cullTerrain", at = @At("RETURN"))
    private void planetEssentials$captureVisibleSections(Camera camera, Frustum frustum,
            boolean spectator, CallbackInfo callback) {
        PrismTerrainVisibleSections.publish(Minecraft.getInstance().level, visibleSections);
    }

    /** Mirrors prepareChunkRenders' section/layer traversal after its Draw lists are complete. */
    @Inject(method = "prepareChunkRenders", at = @At("RETURN"))
    private void planetEssentials$indexVanillaTerrainDraws(Matrix4fc modelViewMatrix,
            double camX, double camY, double camZ,
            CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        PrismTerrainVanillaDrawIndex.capture(visibleSections, callback.getReturnValue());
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void planetEssentials$clearWorldSnapshots(ClientLevel level, CallbackInfo callback) {
        PrismTerrainVisibleSections.clear();
        PrismTerrainActiveFrustum.clear();
        PrismTerrainVanillaDrawIndex.clear();
    }
}
