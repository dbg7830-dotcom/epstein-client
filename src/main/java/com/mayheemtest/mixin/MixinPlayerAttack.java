package com.mayheemtest.mixin;

import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.impl.ReachModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinPlayerAttack
 *
 * Fires on your click via wasPressed() — one call per physical click.
 *
 * Mace smash fix:
 * ───────────────
 * Previously we always called attackEntity() on top of vanilla's own call.
 * The problem: when you're falling and click, vanilla processes your click
 * first and resets fallDistance to 0. Our second attackEntity() then fires
 * with fallDistance=0 so the server never sees a smash.
 *
 * Fix: if vanilla's crosshair is already on a valid target within vanilla
 * range (~3.0 blocks), skip our call entirely and let vanilla handle it.
 * We only call attackEntity() when vanilla WOULDN'T reach the target.
 * This means mace smashes at normal range are always handled by vanilla
 * (fallDistance intact), and reach extension only fires when the target
 * is genuinely outside vanilla range.
 */
@Mixin(MinecraftClient.class)
public class MixinPlayerAttack {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) return;
        if (!mc.options.attackKey.wasPressed()) return;

        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        ReachModule reach = mgr.reach;
        if (!reach.isEnabled()) return;

        PlayerEntity target = reach.findTarget();
        if (target == null) return;

        // If target is already within vanilla reach, let vanilla handle the
        // attack — this preserves fallDistance for mace smash, crits, etc.
        // We only extend when vanilla genuinely can't reach.
        if (reach.isWithinVanillaRange(target)) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }
}
