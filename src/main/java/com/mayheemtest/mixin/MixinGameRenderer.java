package com.mayheemtest.mixin;

import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.impl.HitboxModule;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When HitboxModule is armed (active == true), temporarily adds the configured
 * offset to the player's yaw and pitch immediately before the game performs
 * its crosshair ray-cast / entity interaction check.
 *
 * The offset is applied to player.setYaw / player.setPitch, which is what
 * the interaction manager reads when it sends the UseEntity packet.
 * We restore the original values in the same injection point's tail so the
 * visual camera is never actually moved.
 *
 * Injection target: GameRenderer.updateTargetedEntity – called once per tick
 * to determine what the crosshair is pointing at.  By shifting yaw/pitch here,
 * the server receives an attack whose look-vector is offset by spoofYaw/spoofPitch
 * degrees from where the entity actually is.
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "updateTargetedEntity", at = @At("HEAD"))
    private void preLookSpoof(float tickDelta, CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled() || !hitbox.active) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Shift look before vanilla reads it for the interaction ray-cast
        mc.player.setYaw(mc.player.getYaw() + hitbox.spoofYaw);
        mc.player.setPitch(mc.player.getPitch() + hitbox.spoofPitch);
    }

    @Inject(method = "updateTargetedEntity", at = @At("RETURN"))
    private void postLookRestore(float tickDelta, CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        HitboxModule hitbox = mgr.hitbox;
        // Restore only if we actually shifted (active flag was set)
        if (!hitbox.isEnabled() || !hitbox.active) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Undo the spoof so the visual camera stays correct
        mc.player.setYaw(mc.player.getYaw() - hitbox.spoofYaw);
        mc.player.setPitch(mc.player.getPitch() - hitbox.spoofPitch);

        // Disarm so subsequent ticks don't reapply until the next attack
        hitbox.disarmSpoof();
    }
}
