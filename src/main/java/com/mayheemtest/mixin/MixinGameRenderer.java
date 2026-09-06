package com.mayheemtest.mixin;

import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.impl.HitboxModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinGameRenderer (repurposed — now hooks attackEntity, not renderWorld)
 *
 * Wraps ClientPlayerInteractionManager#attackEntity which is the exact call
 * that serialises and sends the UseEntity/Attack packet to the server.
 *
 * By shifting yaw/pitch HERE, right before the packet is built, and restoring
 * them on RETURN, the server sees an attack whose look-vector is offset by
 * spoofYaw/spoofPitch degrees while the client camera never moves visually.
 *
 * This is the only correct injection point for hitbox spoofing — hooking
 * renderWorld or the tick was unreliable because the rotation was read at a
 * different time than the packet was constructed.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class MixinGameRenderer {

    @Inject(
        method = "attackEntity",
        at = @At("HEAD")
    )
    private void preAttackSpoof(net.minecraft.entity.player.PlayerEntity player,
                                Entity target, CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled()) return;

        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null) return;

        hitbox.armSpoof();
        localPlayer.setYaw(localPlayer.getYaw() + hitbox.spoofYaw);
        localPlayer.setPitch(localPlayer.getPitch() + hitbox.spoofPitch);
    }

    @Inject(
        method = "attackEntity",
        at = @At("RETURN")
    )
    private void postAttackRestore(net.minecraft.entity.player.PlayerEntity player,
                                   Entity target, CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled() || !hitbox.active) return;

        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null) return;

        localPlayer.setYaw(localPlayer.getYaw() - hitbox.spoofYaw);
        localPlayer.setPitch(localPlayer.getPitch() - hitbox.spoofPitch);
        hitbox.disarmSpoof();
    }
}
