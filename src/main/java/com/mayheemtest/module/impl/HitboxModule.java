package com.mayheemtest.module.impl;

import com.mayheemtest.module.AbstractModule;
import com.mayheemtest.module.setting.DoubleSetting;

/**
 * HitboxModule
 *
 * Spoofs the player's yaw/pitch at the exact moment attackEntity() fires,
 * so the server receives an attack whose look-vector misses the target's
 * hitbox. The spoof is applied and restored inside MixinAttackEntity which
 * wraps ClientPlayerInteractionManager#attackEntity directly — this is the
 * only reliable place since it's the method that actually sends the packet.
 *
 * Level slider (1–5) maps to increasing angular offsets:
 *   1 → ~2°   barely outside hitbox, hardest to detect
 *   2 → ~5°   small miss, should flag intermittently
 *   3 → ~10°  clear miss, reliable flag
 *   4 → ~18°  obvious miss
 *   5 → ~28°  extreme, guaranteed flag every hit
 */
public class HitboxModule extends AbstractModule {

    // Angle offsets per level (degrees)
    private static final float[] LEVEL_OFFSETS = { 2f, 5f, 10f, 18f, 28f };

    public final DoubleSetting level = addSetting(new DoubleSetting(
            "Level",
            "1 = subtle miss,  5 = extreme miss",
            1.0, 1.0, 5.0, 1.0
    ));

    // Read by MixinAttackEntity — volatile for cross-thread visibility
    public volatile float spoofYaw   = 0f;
    public volatile float spoofPitch = 0f;
    public volatile boolean active   = false;

    public HitboxModule() {
        super("Hitbox", "Attacks with look vector offset to miss hitbox");
    }

    /** Called immediately before attackEntity() sends the packet. */
    public void armSpoof() {
        int idx = Math.max(0, Math.min(4, (int) level.getValue() - 1));
        float offset = LEVEL_OFFSETS[idx];
        spoofYaw   = offset;
        spoofPitch = offset * 0.5f; // pitch offset is smaller to feel natural
        active     = true;
    }

    /** Called immediately after attackEntity() returns. */
    public void disarmSpoof() {
        active     = false;
        spoofYaw   = 0f;
        spoofPitch = 0f;
    }

    public float getLevelOffset() {
        int idx = Math.max(0, Math.min(4, (int) level.getValue() - 1));
        return LEVEL_OFFSETS[idx];
    }
}
