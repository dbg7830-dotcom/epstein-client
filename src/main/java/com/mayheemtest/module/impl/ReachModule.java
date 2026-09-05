package com.mayheemtest.module.impl;

import com.mayheemtest.module.AbstractModule;
import com.mayheemtest.module.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * ReachModule
 *
 * When enabled, the attack mixin will call attemptAttack() targeting the
 * nearest living player within the configured reach distance rather than
 * requiring the player to be within vanilla crosshair range.
 *
 * Settings
 * ─────────
 * distance  – Maximum attack range in blocks (3.0 vanilla, up to 6.0)
 * This directly tests the server anticheat's Reach check threshold.
 */
public class ReachModule extends AbstractModule {

    public final DoubleSetting distance = addSetting(new DoubleSetting(
            "Distance",
            "Attack range in blocks. Vanilla = 3.0",
            3.5, 3.0, 6.0, 0.05
    ));

    public ReachModule() {
        super("Reach", "Attacks entities beyond vanilla reach distance");
    }

    /**
     * Returns the nearest PlayerEntity within distance.getValue() blocks,
     * excluding the local player. Called every tick by the mixin when enabled.
     * Returns null if no valid target is found.
     */
    public PlayerEntity findTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;

        double range = distance.getValue();
        Vec3d eyePos = mc.player.getEyePos();

        PlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity target)) continue;
            if (target == mc.player) continue;
            if (target.isDead() || target.getHealth() <= 0f) continue;

            // Use closest point on the entity's bounding box, matching how
            // the server anticheat measures reach.
            Box box = target.getBoundingBox();
            double dx = Math.max(box.minX - eyePos.x, Math.max(0, eyePos.x - box.maxX));
            double dy = Math.max(box.minY - eyePos.y, Math.max(0, eyePos.y - box.maxY));
            double dz = Math.max(box.minZ - eyePos.z, Math.max(0, eyePos.z - box.maxZ));
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist <= range && dist < nearestDist) {
                nearestDist = dist;
                nearest = target;
            }
        }

        return nearest;
    }
}
