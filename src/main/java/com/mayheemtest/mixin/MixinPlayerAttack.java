package com.mayheemtest.mixin;

import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.impl.HitboxModule;
import com.mayheemtest.module.impl.ReachModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into the per-tick key-press handling that checks whether the player
 * is holding the Attack key.
 *
 * Reach  – when active, finds a nearby player and attacks them directly,
 *           bypassing the vanilla crosshair ray-cast distance limit.
 * Hitbox – arms the yaw/pitch spoof on HitboxModule before the vanilla attack
 *           fires, then disarms it immediately after.
 */
@Mixin(MinecraftClient.class)
public class MixinPlayerAttack {

    /**
     * Injected at the start of the MinecraftClient tick.
     * We handle Reach here by forcibly attacking the target every tick
     * while the attack key is held, bypassing the vanilla distance gate.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) return;
        if (!mc.options.attackKey.isPressed()) return;

        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        ReachModule reach = mgr.reach;
        HitboxModule hitbox = mgr.hitbox;

        // ── Reach module ────────────────────────────────────────────────────
        if (reach.isEnabled()) {
            PlayerEntity target = reach.findTarget();
            if (target != null) {
                // Arm hitbox spoof first if that module is also enabled,
                // so both modules can be tested simultaneously.
                if (hitbox.isEnabled()) hitbox.armSpoof();
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                if (hitbox.isEnabled()) hitbox.disarmSpoof();
            }
            // Reach has its own target logic; skip vanilla attack below.
            return;
        }

        // ── Hitbox module only (no Reach) ───────────────────────────────────
        // Let vanilla handle targeting but arm the spoof so the look vector
        // sent is offset from where the player is actually looking.
        // The vanilla attack fires in a separate path (handleBlockBreaking /
        // attack key handling), so we arm here and the mixin on
        // ClientPlayerInteractionManager.attackEntity disarms.
        if (hitbox.isEnabled()) {
            hitbox.armSpoof();
            // The disarm is handled in MixinGameRenderer after rotation is read.
        }
    }
}
