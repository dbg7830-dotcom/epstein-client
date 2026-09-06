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
 * Handles Reach — fires attackEntity() at the nearest player within the
 * configured range when the attack key is held.
 *
 * Hitbox spoofing is now handled entirely inside MixinGameRenderer which
 * wraps ClientPlayerInteractionManager#attackEntity directly, so this mixin
 * no longer needs to arm/disarm the hitbox spoof at all. Whether Reach is
 * enabled or not, every attackEntity() call automatically gets the hitbox
 * offset applied if HitboxModule is enabled.
 *
 * Cooldown: attack fires at most once every 10 ticks (~500ms) to avoid
 * spam-flagging the anticheat for attack frequency rather than reach/hitbox.
 */
@Mixin(MinecraftClient.class)
public class MixinPlayerAttack {

    private int reachCooldown = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) return;

        // Tick down cooldown regardless of key state
        if (reachCooldown > 0) {
            reachCooldown--;
            return;
        }

        if (!mc.options.attackKey.isPressed()) return;

        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        ReachModule reach = mgr.reach;
        if (!reach.isEnabled()) return;

        PlayerEntity target = reach.findTarget();
        if (target == null) return;

        // attackEntity() will automatically trigger hitbox spoof via
        // MixinGameRenderer if HitboxModule is enabled — no extra calls needed
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        // 10 tick cooldown — matches vanilla attack speed roughly
        reachCooldown = 10;
    }
}
