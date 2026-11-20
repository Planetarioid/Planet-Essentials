package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/** Client-only predicate for the optional player hit crosshair indicator. */
public final class CrosshairTargetIndicator {
    private CrosshairTargetIndicator() {}

    public static boolean shouldHighlight() {
        if (!EssentialsRuntime.config().crosshairTargetIndicator) return false;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null || client.screen != null) return false;
        if (!(client.hitResult instanceof EntityHitResult entityHit)) return false;
        if (!(entityHit.getEntity() instanceof Player target)) return false;
        if (target == player || !target.isAlive() || target.isSpectator()) return false;
        return player.isWithinAttackRange(target.getBoundingBox(), 0.0);
    }
}
