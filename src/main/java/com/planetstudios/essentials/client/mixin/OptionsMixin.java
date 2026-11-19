package com.planetstudios.essentials.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Extends both vanilla chunk-distance controls to the same inclusive range.
 * The server's view-distance limit can still cap effective multiplayer rendering.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
    private static final int PLANET_ESSENTIALS$MIN_DISTANCE = 2;
    private static final int PLANET_ESSENTIALS$MAX_DISTANCE = 64;

    @Shadow public abstract OptionInstance<Integer> renderDistance();
    @Shadow public abstract OptionInstance<Integer> simulationDistance();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void planetEssentials$extendChunkDistanceRanges(Minecraft client, File optionsFile,
            CallbackInfo callback) {
        planetEssentials$extend(renderDistance());
        planetEssentials$extend(simulationDistance());
    }

    @SuppressWarnings("unchecked")
    private static void planetEssentials$extend(OptionInstance<Integer> option) {
        OptionInstance.IntRange range = new OptionInstance.IntRange(
                PLANET_ESSENTIALS$MIN_DISTANCE, PLANET_ESSENTIALS$MAX_DISTANCE);
        PrismOptionInstanceAccessor<Integer> accessor =
                (PrismOptionInstanceAccessor<Integer>) (Object) option;
        accessor.planetEssentials$setValues(range);
        accessor.planetEssentials$setCodec(range.codec());
        int current = option.get();
        option.set(Math.max(PLANET_ESSENTIALS$MIN_DISTANCE,
                Math.min(PLANET_ESSENTIALS$MAX_DISTANCE, current)));
    }
}
