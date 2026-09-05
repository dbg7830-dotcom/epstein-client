package com.mayheemtest.mixin;

import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.impl.HitboxModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinGameRenderer
 *
 * Hooks into GameRenderer#renderWorld (the outermost per-frame render call)
 * rather than the renamed updateTargetedEntity method.
 *
 * When HitboxModule is armed we shift yaw/pitch before the frame renders
 * (which includes the crosshair ray-cast that decides what entity is targeted),
 * then restore them immediately after so the camera never visually moves.
 *
 * yarn 1.21.11+build.6 name: method_3194  →  renderWorld(RenderTickCounter)
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(
        method = "renderWorld",
        at = @At("HEAD")
    )
    private void preLookSpoof(CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled() || !hitbox.active) return;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        player.setYaw(player.getYaw() + hitbox.spoofYaw);
        player.setPitch(player.getPitch() + hitbox.spoofPitch);
    }

    @Inject(
        method = "renderWorld",
        at = @At("RETURN")
    )
    private void postLookRestore(CallbackInfo ci) {
        ModuleManager mgr = ModuleManager.get();
        if (mgr == null) return;

        HitboxModule hitbox = mgr.hitbox;
        if (!hitbox.isEnabled() || !hitbox.active) return;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        player.setYaw(player.getYaw() - hitbox.spoofYaw);
        player.setPitch(player.getPitch() - hitbox.spoofPitch);

        hitbox.disarmSpoof();
    }
}
