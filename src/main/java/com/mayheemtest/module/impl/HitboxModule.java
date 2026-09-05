package com.mayheemtest.module.impl;

import com.mayheemtest.module.AbstractModule;
import com.mayheemtest.module.setting.DoubleSetting;

/**
 * HitboxModule
 *
 * When enabled, MixinGameRenderer injects an angular offset into the player's
 * reported yaw before each attack packet is sent, then immediately restores the
 * real yaw.  The server receives an attack whose look-vector does not intersect
 * the target's hitbox, which is exactly the condition Hitboxes.java flags.
 *
 * Settings
 * ─────────
 * yawOffset   – Horizontal rotation offset in degrees (0 = no offset, 5 = very
 *               noticeable offset away from target).
 * pitchOffset – Vertical rotation offset in degrees.
 *
 * Both default to a small value so the first test is a near-miss that should
 * only barely trigger the check. Increase to confirm the check fires reliably.
 */
public class HitboxModule extends AbstractModule {

    public final DoubleSetting yawOffset = addSetting(new DoubleSetting(
            "Yaw offset",
            "Degrees to rotate yaw away from target before attack. 0 = disabled.",
            2.0, 0.0, 30.0, 0.5
    ));

    public final DoubleSetting pitchOffset = addSetting(new DoubleSetting(
            "Pitch offset",
            "Degrees to shift pitch before attack. 0 = disabled.",
            0.0, 0.0, 30.0, 0.5
    ));

    // Mixin reads these to apply the spoof, then clears them after the packet.
    // Volatile so changes are visible across threads.
    public volatile float spoofYaw   = 0f;
    public volatile float spoofPitch = 0f;
    public volatile boolean active   = false;

    public HitboxModule() {
        super("Hitbox", "Sends attacks with a rotated look vector to miss the hitbox");
    }

    /**
     * Called by MixinPlayerAttack just before sending an attack packet.
     * Sets the spoof values the renderer mixin will return.
     */
    public void armSpoof() {
        spoofYaw   = (float) yawOffset.getValue();
        spoofPitch = (float) pitchOffset.getValue();
        active     = true;
    }

    /**
     * Called by MixinPlayerAttack immediately after the attack packet fires.
     * Restores real look so movement is unaffected.
     */
    public void disarmSpoof() {
        active = false;
        spoofYaw   = 0f;
        spoofPitch = 0f;
    }
}
