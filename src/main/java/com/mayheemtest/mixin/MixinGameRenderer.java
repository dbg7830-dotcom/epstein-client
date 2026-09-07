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
 * Hooks attackEntity on ClientPlayerInteractionManager.
 *
 * Sends a LookAndOnGround packet with spoofed yaw/pitch before the attack,
 * then immediately restores real rotation after.
 *
 * The Reach check reads player.yaw / player.lastYaw / player.lastPitch
 * from the most recent movement packets. By sending a look packet first,
 * we set what the server sees as the active look direction at attack time.
 *
 * Level 3+ offsets are large enough to beat all three look vectors the
 * check tests, guaranteeing minDistance == MAX_VALUE → HITBOX flag.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class MixinGameRenderer {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void preAttackSpoof(PlayerEntity player, Entity target, CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;
        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled()) return;

        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;

        hitbox.armSpoof();

        // horizontalCollision is a public boolean field on Entity (confirmed yarn 1.21.11+build.6)
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(
                p.getYaw() + hitbox.spoofYaw,
                p.getPitch() + hitbox.spoofPitch,
                p.isOnGround(),
                p.horizontalCollision
            )
        );
    }

    @Inject(method = "attackEntity", at = @At("RETURN"))
    private void postAttackRestore(PlayerEntity player, Entity target, CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;
        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled() || !hitbox.active) return;

        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;

        // Restore real rotation
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(
                p.getYaw(),
                p.getPitch(),
                p.isOnGround(),
                p.horizontalCollision
            )
        );

        hitbox.disarmSpoof();
    }
}
