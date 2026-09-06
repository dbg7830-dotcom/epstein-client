package com.mayheemtest.module.impl;

import com.mayheemtest.command.FriendManager;
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
 * Attacks the nearest player within the configured distance.
 * Skips anyone on the FriendManager whitelist entirely.
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
     * Returns the nearest non-whitelisted PlayerEntity within range.
     * Returns null if no valid target found.
     */
    public PlayerEntity findTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;

        double range = distance.getValue();
        Vec3d eyePos = mc.player.getEyePos();
        FriendManager friends = FriendManager.get();

        PlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity target)) continue;
            if (target == mc.player) continue;
            if (target.isDead() || target.getHealth() <= 0f) continue;

            // Skip whitelisted friends — whitelist check is purely local
            if (friends.isFriend(target.getGameProfile().getName())) continue;

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
