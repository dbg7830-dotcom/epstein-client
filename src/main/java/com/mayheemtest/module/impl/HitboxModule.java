package com.mayheemtest.module.impl;

import com.mayheemtest.module.AbstractModule;
import com.mayheemtest.module.setting.DoubleSetting;

/**
 * HitboxModule
 *
 * Exploit analysis of Reach.java:
 *
 * HITBOX triggers when minDistance == Double.MAX_VALUE — ray never hits.
 * The check tests three look vectors: yaw/pitch, lastYaw/pitch, lastYaw/lastPitch.
 * ALL must miss for HITBOX to fire.
 *
 * Level offsets are sized to beat all three vectors:
 *   Level 1 =  5°  may slip through lastYaw fallback — subtle, may not flag
 *   Level 2 = 12°  clears current yaw, risky on lastYaw — intermittent flag
 *   Level 3 = 20°  clears all three vectors on most targets — reliable flag
 *   Level 4 = 30°  guaranteed miss on all vectors for any target size
 *   Level 5 = 45°  extreme — looking almost 90° away, always HITBOX
 *
 * Since Hitboxes.java is currently a stub, this confirms the flag path in
 * Reach.java line 250 fires correctly. Once you add logic to Hitboxes.java
 * it will cancel these too.
 */
public class HitboxModule extends AbstractModule {

    private static final float[] LEVEL_OFFSETS = { 5f, 12f, 20f, 30f, 45f };

    public final DoubleSetting level = addSetting(new DoubleSetting(
            "Level",
            "1 = subtle  5 = guaranteed HITBOX flag",
            1.0, 1.0, 5.0, 1.0
    ));

    public volatile float spoofYaw   = 0f;
    public volatile float spoofPitch = 0f;
    public volatile boolean active   = false;

    public HitboxModule() {
        super("Hitbox", "Sends attacks with look vector that misses hitbox");
    }

    public void armSpoof() {
        int idx = Math.max(0, Math.min(4, (int) level.getValue() - 1));
        float offset = LEVEL_OFFSETS[idx];
        spoofYaw   = offset;
        spoofPitch = offset * 0.4f;
        active     = true;
    }

    public void disarmSpoof() {
        active     = false;
        spoofYaw   = 0f;
        spoofPitch = 0f;
    }
}
