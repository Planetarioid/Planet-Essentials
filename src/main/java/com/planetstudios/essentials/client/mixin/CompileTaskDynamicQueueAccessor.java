package com.planetstudios.essentials.client.mixin;

import net.minecraft.client.renderer.chunk.CompileTaskDynamicQueue;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Narrow access to vanilla's existing bounded chunk compile queue. */
@Mixin(CompileTaskDynamicQueue.class)
public interface CompileTaskDynamicQueueAccessor {
    @Accessor("tasks")
    List<SectionRenderDispatcher.RenderSection.CompileTask> spaceClient$getTasks();

    @Accessor("recompileQuota")
    int spaceClient$getRecompileQuota();

    @Accessor("recompileQuota")
    void spaceClient$setRecompileQuota(int quota);
}
