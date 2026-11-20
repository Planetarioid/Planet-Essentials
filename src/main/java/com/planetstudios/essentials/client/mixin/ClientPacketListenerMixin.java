package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.hud.HudWidgetRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/** Attributes server-confirmed PvP damage events to the local player. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    private static long spaceClient$dedupeTick = Long.MIN_VALUE;
    private static final Set<Long> SPACE_CLIENT_COUNTED_HITS = new HashSet<>();

    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void planetEssentials$trackConfirmedPlayerDamage(
            ClientboundDamageEventPacket packet, CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        Entity victim = client.level.getEntity(packet.entityId());
        if (!(victim instanceof Player victimPlayer)) return;

        DamageSource source = packet.getSource(client.level);
        Entity causingEntity = source.getEntity();
        if (!(causingEntity instanceof Player attacker)) return;

        Player localPlayer = client.player;
        if (attacker != localPlayer && victimPlayer != localPlayer) return;

        // A server can deliver more than one damage notification for the same
        // attacker/victim pair during a client tick. Count that pair once so a
        // single apparent hit cannot advance the HUD twice.
        long tick = client.level.getGameTime();
        if (tick != spaceClient$dedupeTick) {
            SPACE_CLIENT_COUNTED_HITS.clear();
            spaceClient$dedupeTick = tick;
        }
        long hitKey = ((long) attacker.getId() << 32) ^ (victimPlayer.getId() & 0xFFFFFFFFL);
        if (!SPACE_CLIENT_COUNTED_HITS.add(hitKey)) return;

        if (attacker == localPlayer) {
            HudWidgetRenderer.comboCounter().recordOutgoingHit();
        } else {
            HudWidgetRenderer.comboCounter().recordIncomingHit();
        }
    }
}
