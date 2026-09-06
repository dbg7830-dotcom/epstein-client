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
 * MixinGameRenderer — hooks ClientPlayerInteractionManager#attackEntity
 *
 * Confirmed from yarn 1.21.11+build.6 docs:
 * - attackEntity(PlayerEntity player, Entity target) exists, correct name
 * - PlayerMoveC2SPacket.LookAndOnGround exists
 * - horizontalCollision() is a METHOD not a field — was the bug
 * - sendPacket via MinecraftClient.getInstance().getNetworkHandler().sendPacket()
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

        // Send spoofed look packet before attack
        // horizontalCollision() is a method call — confirmed from docs
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(
                localPlayer.getYaw()   + hitbox.spoofYaw,
                localPlayer.getPitch() + hitbox.spoofPitch,
                localPlayer.isOnGround(),
                localPlayer.horizontalCollision()
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
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(
                localPlayer.getYaw(),
                localPlayer.getPitch(),
                localPlayer.isOnGround(),
                localPlayer.horizontalCollision()
            )
        );

        hitbox.disarmSpoof();
    }
}
