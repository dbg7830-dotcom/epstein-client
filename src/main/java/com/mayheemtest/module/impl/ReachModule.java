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
 * Exploit analysis of Reach.java:
 *
 * threshold    = 0.0005 blocks — check flags above this past maxReach
 * cancelBuffer = after 1 flag, next 4 hits aggressively pre-cancelled
 * isKnownInvalid pre-cancels at ~3.05 in real-time on packet receipt
 * tickBetterReachCheckWithAngle runs AFTER a movement/look packet arrives
 *
 * Blatant mode OFF  — distance slider, manual fine control
 * Blatant mode ON   — level presets:
 *   Level 1 = 3.04   just under the 3.05 pre-cancel threshold — subtle
 *   Level 2 = 3.10   trips post-look check but not pre-cancel
 *   Level 3 = 3.50   clear flag, both checks
 *   Level 4 = 4.50   obvious
 *   Level 5 = 6.00   extreme, guaranteed everything
 */
public class ReachModule extends AbstractModule {

    // Level 1 sits just under isKnownInvalid threshold (~3.05)
    // so attacks queue for post-look check but aren't pre-cancelled
    private static final double[] BLATANT_DISTANCES = { 3.04, 3.10, 3.50, 4.50, 6.00 };

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
            "1 = subtle (under pre-cancel)  5 = guaranteed flag",
            1.0, 1.0, 5.0, 1.0
    ));

    public ReachModule() {
        super("Reach", "Extends your attack range");
    }

    public double getEffectiveRange() {
        if (blatantMode.getValue()) {
            int idx = Math.max(0, Math.min(4, (int) blatantLevel.getValue() - 1));
            return BLATANT_DISTANCES[idx];
        }
        return distance.getValue();
    }

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
     * True if target is within vanilla reach — we skip our call and let
     * vanilla handle it so mace smash / crits fire with fallDistance intact.
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
