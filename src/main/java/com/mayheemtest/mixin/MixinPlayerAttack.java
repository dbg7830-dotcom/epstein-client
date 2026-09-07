package com.mayheemtest.mixin;

import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.impl.ReachModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks MinecraftClient#doAttack which fires exactly once per physical
 * left-click. This is the correct place — vanilla calls this once per
 * click, checks the crosshair target, and calls attackEntity if it hits.
 *
 * We inject at RETURN so vanilla runs first (preserving mace smash,
 * crits, fallDistance, combos — all untouched). After vanilla's own
 * attack is done we check if there's a player within extended range
 * that vanilla missed, and hit them too.
 */
@Mixin(MinecraftClient.class)
public class MixinPlayerAttack {

    @Inject(method = "doAttack", at = @At("RETURN"))
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) return;

        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        ReachModule reach = mgr.reach;
        if (!reach.isEnabled()) return;

        PlayerEntity target = reach.findTarget();
        if (target == null) return;

        // Skip if vanilla already reached them — let vanilla handle it
        // so mace smash / crits / fallDistance all stay intact
        if (reach.isWithinVanillaRange(target)) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }
}
