package com.mayheemtest.mixin;

import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.impl.HitboxModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinGameRenderer (hooks attackEntity on ClientPlayerInteractionManager)
 *
 * The correct way to spoof look direction for hitbox testing:
 *
 * Minecraft does NOT include yaw/pitch in the attack packet itself.
 * The server tracks the player's look direction from PlayerMove packets
 * sent separately each tick. So to make the server think you're looking
 * somewhere else when you attack, you must:
 *
 *   1. Send a PlayerLook packet with the OFFSET rotation
 *   2. Send the attack packet  (attackEntity handles this)
 *   3. Send a PlayerLook packet RESTORING real rotation
 *
 * Steps 1 and 3 are done here wrapping attackEntity via HEAD/RETURN inject.
 * The server processes packets in order, so it sees: look(offset) → attack → look(real).
 * The anticheat reads the look that was active when the attack arrived = offset look.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class MixinGameRenderer {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void preAttackSpoof(PlayerEntity player, Entity target, CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled()) return;

        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null) return;

        hitbox.armSpoof();

        float spoofedYaw   = localPlayer.getYaw()   + hitbox.spoofYaw;
        float spoofedPitch = localPlayer.getPitch()  + hitbox.spoofPitch;

        // Send a look packet with the offset rotation BEFORE the attack packet
        // The server will use this rotation when processing the attack
        localPlayer.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(spoofedYaw, spoofedPitch, localPlayer.isOnGround(), localPlayer.horizontalCollision)
        );
    }

    @Inject(method = "attackEntity", at = @At("RETURN"))
    private void postAttackRestore(PlayerEntity player, Entity target, CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled() || !hitbox.active) return;

        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null) return;

        // Immediately send real rotation back so movement stays correct
        localPlayer.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(localPlayer.getYaw(), localPlayer.getPitch(), localPlayer.isOnGround(), localPlayer.horizontalCollision)
        );

        hitbox.disarmSpoof();
    }
}
