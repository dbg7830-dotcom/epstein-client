package com.mayheemtest.module;

import com.mayheemtest.module.setting.BooleanSetting;
import com.mayheemtest.module.setting.DoubleSetting;

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

    // Ordered lists so the GUI renders settings in declaration order
    protected final List<DoubleSetting> doubleSettings = new ArrayList<>();
    protected final List<BooleanSetting> booleanSettings = new ArrayList<>();

    protected AbstractModule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // ── Registration helpers ────────────────────────────────────────────────

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
        if (enabled) onEnable(); else onDisable();
    }

    public final void setEnabled(boolean v) {
        if (v != enabled) toggle();
    }

    /** Called once when the module is switched on. Override to reset state. */
    protected void onEnable() {}

    /** Called once when the module is switched off. Override to clean up. */
    protected void onDisable() {}

    // ── Getters ────────────────────────────────────────────────────────────

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled()     { return enabled; }

    public List<DoubleSetting>  getDoubleSettings()  { return doubleSettings; }
    public List<BooleanSetting> getBooleanSettings() { return booleanSettings; }
}
