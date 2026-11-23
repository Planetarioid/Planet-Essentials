package com.planetstudios.essentials.client.mixin;

import com.mojang.blaze3d.textures.GpuSampler;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainRenderGroupBoundary;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainReplacementRuntime;
import com.planetstudios.essentials.rendering.space.SpaceRendererRenderGroupBridge;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaves vanilla terrain rendering entirely untouched unless Prism is actively selected. */
@Mixin(ChunkSectionsToRender.class)
public abstract class ChunkSectionsToRenderMixin {
    @Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
    private void planetEssentials$beginTerrainRenderGroup(
            ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo callback) {
        if (!ShaderPackRuntime.isEnabled()) {
            SpaceRendererRenderGroupBridge.preflight(
                    (ChunkSectionsToRender) (Object) this, group, sampler);
            return;
        }
        PrismTerrainRenderGroupBoundary.begin((ChunkSectionsToRender) (Object) this, group, sampler);
        if (PrismTerrainReplacementRuntime.tryReplace(PrismTerrainRenderGroupBoundary.current())) {
            PrismTerrainRenderGroupBoundary.end();
            callback.cancel();
        }
    }

    @Inject(method = "renderGroup", at = @At("RETURN"))
    private void planetEssentials$endTerrainRenderGroup(
            ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo callback) {
        PrismTerrainRenderGroupBoundary.end();
    }
}
