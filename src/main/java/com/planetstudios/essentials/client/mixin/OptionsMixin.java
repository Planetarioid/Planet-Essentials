package com.planetstudios.essentials.client.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Extends both vanilla chunk-distance controls to the same inclusive range.
 * The server's view-distance limit can still cap effective multiplayer rendering.
 *
 * OptionInstance.ValueSet is package-private in Minecraft 1.21.11. Resolve its
 * backing field by checking which interface accepts the replacement IntRange,
 * not by its simple class name: production remapping can change that name.
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

    private static void planetEssentials$extend(OptionInstance<Integer> option) {
        OptionInstance.IntRange range = new OptionInstance.IntRange(
                PLANET_ESSENTIALS$MIN_DISTANCE, PLANET_ESSENTIALS$MAX_DISTANCE);
        try {
            Field valuesField = Arrays.stream(OptionInstance.class.getDeclaredFields())
                    .filter(field -> field.getType().isInterface())
                    .filter(field -> field.getType().isInstance(range))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchFieldException("OptionInstance value-set field"));
            Field codecField = Arrays.stream(OptionInstance.class.getDeclaredFields())
                    .filter(field -> Codec.class.isAssignableFrom(field.getType()))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchFieldException("OptionInstance codec field"));
            valuesField.setAccessible(true);
            codecField.setAccessible(true);
            valuesField.set(option, range);
            codecField.set(option, range.codec());
        } catch (ReflectiveOperationException | RuntimeException failure) {
            throw new IllegalStateException("Unable to extend Minecraft chunk-distance option bounds", failure);
        }

        int current = option.get();
        option.set(Math.max(PLANET_ESSENTIALS$MIN_DISTANCE,
                Math.min(PLANET_ESSENTIALS$MAX_DISTANCE, current)));
    }
}
