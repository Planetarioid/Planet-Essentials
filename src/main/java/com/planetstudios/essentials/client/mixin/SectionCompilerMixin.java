package com.planetstudios.essentials.client.mixin;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismVanillaSectionRebuildEvents;
import com.planetstudios.essentials.rendering.space.SpaceRendererCaptureRuntime;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures vanilla section geometry for Space Renderer and optional Prism shader packs. */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    @Inject(method = "compile", at = @At("HEAD"))
    private void planetEssentials$onVanillaSectionCompile(SectionPos sectionPos,
            RenderSectionRegion region, VertexSorting vertexSorting,
            SectionBufferBuilderPack buffers,
            CallbackInfoReturnable<SectionCompiler.Results> callback) {
        if (!SpaceRendererCaptureRuntime.isCaptureEnabled() && !ShaderPackRuntime.isEnabled()) return;
        PrismVanillaSectionRebuildEvents.onVanillaCompile(sectionPos, region);
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private void planetEssentials$captureVanillaSectionMesh(SectionPos sectionPos,
            RenderSectionRegion region, VertexSorting vertexSorting,
            SectionBufferBuilderPack buffers,
            CallbackInfoReturnable<SectionCompiler.Results> callback) {
        if (!SpaceRendererCaptureRuntime.isCaptureEnabled() && !ShaderPackRuntime.isEnabled()) return;
        PrismVanillaSectionRebuildEvents.onVanillaCompileComplete(callback.getReturnValue());
    }
}
