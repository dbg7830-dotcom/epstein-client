package com.mayheemtest.module.setting;

public class BooleanSetting {

    private final String name;
    private final String description;
    private boolean value;

    public BooleanSetting(String name, String description, boolean defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean getValue() { return value; }
    public void setValue(boolean value) { this.value = value; }
    public void toggle() { this.value = !this.value; }
}
