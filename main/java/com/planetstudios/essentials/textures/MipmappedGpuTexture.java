package com.planetstudios.essentials.textures;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

/**
 * Minecraft TextureManager-owned texture backed by an explicitly allocated
 * GPU texture with a complete, precomputed mip chain.
 */
final class MipmappedGpuTexture extends AbstractTexture {
    MipmappedGpuTexture(GpuTexture texture, GpuTextureView view, GpuSampler sampler) {
        this.texture = texture;
        this.textureView = view;
        this.sampler = sampler;
    }
}
