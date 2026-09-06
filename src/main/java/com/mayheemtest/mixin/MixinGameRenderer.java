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
 * MixinGameRenderer (hooks ClientPlayerInteractionManager#attackEntity)
 *
 * Exploit analysis of Reach.java for hitbox bypass:
 *
 * The HITBOX result fires when minDistance == Double.MAX_VALUE, meaning
 * the ray from eye position never intersects the target box at all.
 *
 * The check uses multiple look vectors:
 *   - player.yaw / player.pitch       (current)
 *   - player.lastYaw / player.pitch   (1.8+)
 *   - player.lastYaw / player.lastPitch (1.9+)
 *
 * To trigger HITBOX reliably, ALL of these vectors must miss the hitbox.
 * This means a small offset isn't enough at low levels — the check will
 * find one of the other vectors that still hits.
 *
 * Strategy:
 * - Send a look packet with offset rotation before the attack
 * - The offset must be large enough that even with lastYaw/lastPitch
 *   uncertainty window, none of the vectors intersect
 * - Level 1: small offset, may still be caught by lastYaw fallback
 * - Level 5: large enough that all vectors miss, guaranteed HITBOX flag
 *
 * After the attack we immediately send real rotation back.
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

        // Send spoofed look packet — server uses this for the attack ray
        localPlayer.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(
                localPlayer.getYaw()   + hitbox.spoofYaw,
                localPlayer.getPitch() + hitbox.spoofPitch,
                localPlayer.isOnGround(),
                localPlayer.horizontalCollision
            )
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

        // Restore real rotation immediately after
        localPlayer.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(
                localPlayer.getYaw(),
                localPlayer.getPitch(),
                localPlayer.isOnGround(),
                localPlayer.horizontalCollision
            )
        );

        hitbox.disarmSpoof();
    }
}
