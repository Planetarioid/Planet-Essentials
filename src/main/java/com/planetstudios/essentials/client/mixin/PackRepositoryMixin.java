package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.client.FilteredPackResources;
import com.planetstudios.essentials.config.ResourcePackCategoryConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Applies advanced asset filters only to the client's selected resource packs. */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Inject(method = "openAllSelected", at = @At("RETURN"), cancellable = true)
    private void spaceClient$filterSelectedResourcePackAssets(
            CallbackInfoReturnable<List<PackResources>> callback) {
        if (Minecraft.getInstance().getResourcePackRepository() != (Object) this) return;

        var overrides = EssentialsRuntime.config().resourcePackCategoryOverrides;
        if (overrides.isEmpty()) return;

        List<PackResources> original = callback.getReturnValue();
        List<PackResources> filtered = null;
        for (int i = 0; i < original.size(); i++) {
            PackResources pack = original.get(i);
            ResourcePackCategoryConfig categories = overrides.get(pack.packId());
            if (categories == null || categories.allEnabled()) continue;
            if (filtered == null) filtered = new ArrayList<>(original);
            filtered.set(i, new FilteredPackResources(pack, categories));
        }
        if (filtered != null) callback.setReturnValue(List.copyOf(filtered));
    }
}
