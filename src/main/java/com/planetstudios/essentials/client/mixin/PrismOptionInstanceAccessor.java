package com.planetstudios.essentials.client.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Replaces the value validator and codec together when extending vanilla integer option bounds. */
@Mixin(OptionInstance.class)
public interface PrismOptionInstanceAccessor<T> {
    @Mutable
    @Accessor("values")
    void planetEssentials$setValues(OptionInstance.ValueSet<T> values);

    @Mutable
    @Accessor("codec")
    void planetEssentials$setCodec(Codec<T> codec);
}
