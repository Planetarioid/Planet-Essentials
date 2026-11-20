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

/** Attributes server-confirmed PvP damage events to the local player. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
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
        if (attacker == localPlayer && victimPlayer != localPlayer) {
            HudWidgetRenderer.comboCounter().recordOutgoingHit();
        } else if (victimPlayer == localPlayer && attacker != localPlayer) {
            HudWidgetRenderer.comboCounter().recordIncomingHit();
        }
    }
}
