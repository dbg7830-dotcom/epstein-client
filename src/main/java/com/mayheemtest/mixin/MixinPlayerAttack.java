package com.mayheemtest.mixin;

import com.mayheemtest.module.ModuleManager;
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
 * MixinPlayerAttack
 *
 * Injects into ClientPlayerInteractionManager#attackEntity which vanilla
 * calls naturally when you click. We never touch the keypress — vanilla
 * handles the full click pipeline normally, then we piggyback on its call
 * to also hit any extended-range targets.
 *
 * This means:
 * - Normal clicking works exactly as before — vanilla processes everything
 * - Mace smash works — vanilla fires its own attackEntity with fallDistance
 *   intact, we only add a second call for targets outside vanilla range
 * - Crits, combos, attribute swaps — all unaffected, vanilla handles them
 *
 * We skip targets already within vanilla range since vanilla's own call
 * already handled them.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class MixinPlayerAttack {

    @Inject(method = "attackEntity", at = @At("RETURN"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        ReachModule reach = mgr.reach;
        if (!reach.isEnabled()) return;

        // Find a target beyond vanilla range
        PlayerEntity extendedTarget = reach.findTarget();
        if (extendedTarget == null) return;

        // Don't double-hit — if vanilla already hit this same target, skip
        if (extendedTarget == target) return;

        // Only fire for targets genuinely outside vanilla range
        if (reach.isWithinVanillaRange(extendedTarget)) return;

        mc.interactionManager.attackEntity(mc.player, extendedTarget);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }
}
