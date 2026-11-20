package com.planetstudios.essentials.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicBoolean;

/** Exposes cancellation state for vanilla compile tasks while preserving their lifecycle. */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
public interface CompileTaskAccessor {
    @Accessor("isCancelled")
    AtomicBoolean spaceClient$isCancelled();
}
