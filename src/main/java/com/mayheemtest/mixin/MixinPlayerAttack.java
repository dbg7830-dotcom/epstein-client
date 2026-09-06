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
 * isPressed() in the tick — the version that worked.
 *
 * Exploit note from Reach.java analysis:
 * - tooManyAttacks check triggers at queue > 10 per tick
 *   so 10-tick cooldown keeps us well under that
 * - cancelBuffer kicks in after 1 flag and decays 0.25 per clean hit
 *   so at level 1 (3.04 blocks) we stay under isKnownInvalid pre-cancel
 *   meaning we only trip the post-look check, not the hard cancel
 *
 * Mace smash fix: skip our call when vanilla can already reach the target
 * so vanilla fires first with fallDistance intact.
 */
@Mixin(MinecraftClient.class)
public class MixinPlayerAttack {

    private int reachCooldown = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) return;

        if (reachCooldown > 0) { reachCooldown--; return; }
        if (!mc.options.attackKey.isPressed()) return;

        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        ReachModule reach = mgr.reach;
        if (!reach.isEnabled()) return;

        PlayerEntity target = reach.findTarget();
        if (target == null) return;

        // Skip if vanilla can reach — let vanilla handle it with fallDistance
        // intact so mace smash, crits, and combos all work normally
        if (reach.isWithinVanillaRange(target)) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        // Stay well under the tooManyAttacks > 10 threshold
        reachCooldown = 10;
    }
}
