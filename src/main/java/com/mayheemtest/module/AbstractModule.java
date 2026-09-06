package com.mayheemtest.module;

import com.mayheemtest.module.setting.BooleanSetting;
import com.mayheemtest.module.setting.DoubleSetting;
import com.mayheemtest.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every test module.
 * Subclasses register their settings in the constructor via addSetting().
 */
public abstract class AbstractModule {

    private final String name;
    private final String description;
    private boolean enabled = false;

    protected final List<DoubleSetting> doubleSettings = new ArrayList<>();
    protected final List<BooleanSetting> booleanSettings = new ArrayList<>();

    protected AbstractModule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // ── Registration helpers ───────────────────────────────────────────────

    protected DoubleSetting addSetting(DoubleSetting s) {
        doubleSettings.add(s);
        return s;
    }

    protected BooleanSetting addSetting(BooleanSetting s) {
        booleanSettings.add(s);
        return s;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    public final void toggle() {
        enabled = !enabled;
        // Client-side only chat notification — never sent to server
        ChatUtil.sendToggle(name, enabled);
        if (enabled) onEnable(); else onDisable();
    }

    public final void setEnabled(boolean v) {
        if (v != enabled) toggle();
    }

    protected void onEnable() {}
    protected void onDisable() {}

    // ── Getters ────────────────────────────────────────────────────────────

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled()     { return enabled; }

    public List<DoubleSetting>  getDoubleSettings()  { return doubleSettings; }
    public List<BooleanSetting> getBooleanSettings() { return booleanSettings; }
}
