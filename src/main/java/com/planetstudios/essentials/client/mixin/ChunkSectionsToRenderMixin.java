package com.planetstudios.essentials.client.mixin;

import com.mojang.blaze3d.textures.GpuSampler;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainRenderGroupBoundary;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes exact vanilla terrain-group inputs without changing their draws. */
@Mixin(ChunkSectionsToRender.class)
public abstract class ChunkSectionsToRenderMixin {
    @Inject(method = "renderGroup", at = @At("HEAD"))
    private void planetEssentials$beginTerrainRenderGroup(
            ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo callback) {
        PrismTerrainRenderGroupBoundary.begin((ChunkSectionsToRender) (Object) this, group, sampler);
    }

    @Inject(method = "renderGroup", at = @At("RETURN"))
    private void planetEssentials$endTerrainRenderGroup(
            ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo callback) {
        PrismTerrainRenderGroupBoundary.end();
    }
}
