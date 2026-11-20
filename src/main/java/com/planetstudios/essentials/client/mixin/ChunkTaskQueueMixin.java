package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.rendering.ChunkSchedulingPolicy;
import net.minecraft.client.renderer.chunk.CompileTaskDynamicQueue;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Adds camera-facing priority to vanilla's existing distance and recompile-quota scheduler. */
@Mixin(CompileTaskDynamicQueue.class)
public abstract class ChunkTaskQueueMixin {
    @Inject(method = "poll", at = @At("HEAD"), cancellable = true)
    private void spaceClient$prioritizeForwardChunks(Vec3 cameraPosition,
            CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.CompileTask> callback) {
        if (!EssentialsRuntime.config().smartChunkSchedulerEnabled) return;

        CompileTaskDynamicQueueAccessor queue = (CompileTaskDynamicQueueAccessor) this;
        List<SectionRenderDispatcher.RenderSection.CompileTask> tasks = queue.spaceClient$getTasks();
        ChunkSchedulingPolicy.Look look = ChunkSchedulingPolicy.look();

        int normalIndex = -1;
        int recompileIndex = -1;
        double normalScore = Double.MAX_VALUE;
        double recompileScore = Double.MAX_VALUE;

        for (int i = 0; i < tasks.size();) {
            SectionRenderDispatcher.RenderSection.CompileTask task = tasks.get(i);
            if (((CompileTaskAccessor) (Object) task).spaceClient$isCancelled().get()) {
                tasks.remove(i);
                continue;
            }

            BlockPos origin = task.getRenderOrigin();
            double score = ChunkSchedulingPolicy.score(
                    origin.getX() + 8.0 - cameraPosition.x,
                    origin.getY() + 8.0 - cameraPosition.y,
                    origin.getZ() + 8.0 - cameraPosition.z, look);
            if (task.isRecompile()) {
                if (score < recompileScore) {
                    recompileScore = score;
                    recompileIndex = i;
                }
            } else if (score < normalScore) {
                normalScore = score;
                normalIndex = i;
            }
            i++;
        }

        boolean hasRecompile = recompileIndex >= 0;
        boolean hasNormal = normalIndex >= 0;
        if (!hasRecompile && !hasNormal) {
            callback.setReturnValue(null);
            return;
        }

        boolean chooseRecompile = hasRecompile && (!hasNormal
                || (queue.spaceClient$getRecompileQuota() > 0 && recompileScore < normalScore));
        int selectedIndex;
        if (chooseRecompile) {
            queue.spaceClient$setRecompileQuota(queue.spaceClient$getRecompileQuota() - 1);
            selectedIndex = recompileIndex;
        } else {
            queue.spaceClient$setRecompileQuota(2);
            selectedIndex = normalIndex;
        }
        callback.setReturnValue(tasks.remove(selectedIndex));
    }
}
