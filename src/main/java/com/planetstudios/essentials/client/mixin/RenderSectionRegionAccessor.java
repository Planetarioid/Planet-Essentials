package com.planetstudios.essentials.client.mixin;

import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the source world for a callback-scoped vanilla section snapshot. */
@Mixin(RenderSectionRegion.class)
public interface RenderSectionRegionAccessor {
    @Accessor("level")
    Level planetEssentials$getLevel();
}