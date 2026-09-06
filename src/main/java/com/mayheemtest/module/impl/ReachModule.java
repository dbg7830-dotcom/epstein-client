package com.mayheemtest.module.impl;

import com.mayheemtest.command.FriendManager;
import com.mayheemtest.module.AbstractModule;
import com.mayheemtest.module.setting.BooleanSetting;
import com.mayheemtest.module.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * ReachModule
 *
 * Settings layout in GUI:
 * ─────────────────────────────────────────
 *  Distance    [══════════╌╌]  3.50
 *  Blatant mode            [ toggle ]
 *    Level     [══╌╌╌╌╌╌╌╌]  1        ← only visible when blatant mode ON
 * ─────────────────────────────────────────
 *
 * Distance slider — fine control, always visible.
 *
 * Blatant mode OFF  → use distance as-is, subtle.
 * Blatant mode ON   → distance slider is overridden by the level preset:
 *   Level 1 = 3.2 blocks  subtle, near detection threshold
 *   Level 2 = 3.6 blocks  moderate
 *   Level 3 = 4.2 blocks  clear flag
 *   Level 4 = 5.0 blocks  obvious
 *   Level 5 = 6.0 blocks  extreme, guaranteed flag
 */
public class ReachModule extends AbstractModule {

    private static final double[] BLATANT_DISTANCES = { 3.2, 3.6, 4.2, 5.0, 6.0 };

    public final DoubleSetting distance = addSetting(new DoubleSetting(
            "Distance",
            "Attack range in blocks. Vanilla = 3.0",
            3.5, 3.0, 6.0, 0.05
    ));

    public final BooleanSetting blatantMode = addSetting(new BooleanSetting(
            "Blatant mode",
            "Override distance with a blatancy level preset",
            false
    ));

    public final DoubleSetting blatantLevel = addSetting(new DoubleSetting(
            "Level",
            "1 = subtle  5 = guaranteed flag",
            1.0, 1.0, 5.0, 1.0
    ));

    public ReachModule() {
        super("Reach", "Extends your attack range");
    }

    /** Effective range — blatant preset if blatant mode on, else raw slider. */
    public double getEffectiveRange() {
        if (blatantMode.getValue()) {
            int idx = Math.max(0, Math.min(4, (int) blatantLevel.getValue() - 1));
            return BLATANT_DISTANCES[idx];
        }
        return distance.getValue();
    }

    /**
     * Returns nearest valid target within effective range.
     * Returns null if no target found — caller handles vanilla fallback.
     */
    public PlayerEntity findTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;

        double range = getEffectiveRange();
        Vec3d eyePos = mc.player.getEyePos();
        FriendManager friends = FriendManager.get();

        PlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity target)) continue;
            if (target == mc.player) continue;
            if (target.isDead() || target.getHealth() <= 0f) continue;
            if (friends.isFriend(target.getGameProfile().name())) continue;

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

    /**
     * Returns true if the target is already within vanilla reach (~3.0).
     * Used by the mixin to avoid firing a second attackEntity when vanilla
     * will already handle the hit — critical for mace smash to work correctly.
     */
    public boolean isWithinVanillaRange(PlayerEntity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        Vec3d eyePos = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        double dx = Math.max(box.minX - eyePos.x, Math.max(0, eyePos.x - box.maxX));
        double dy = Math.max(box.minY - eyePos.y, Math.max(0, eyePos.y - box.maxY));
        double dz = Math.max(box.minZ - eyePos.z, Math.max(0, eyePos.z - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= 3.0;
    }
}
