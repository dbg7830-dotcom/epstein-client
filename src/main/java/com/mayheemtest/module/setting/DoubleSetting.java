package com.mayheemtest.module.setting;

public class DoubleSetting {

    private final String name;
    private final String description;
    private final double min;
    private final double max;
    private final double step;
    private double value;

    public DoubleSetting(String name, String description, double defaultValue, double min, double max, double step) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getValue() { return value; }
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    public void setValue(double v) {
        this.value = Math.max(min, Math.min(max, snapToStep(v)));
    }

    /** Increment value by one step, wrapping at max */
    public void increment() { setValue(value + step); }

    /** Decrement value by one step, wrapping at min */
    public void decrement() { setValue(value - step); }

    /** 0.0 – 1.0 normalised position for GUI slider rendering */
    public float getNormalized() {
        return (float) ((value - min) / (max - min));
    }

    /** Set value from a 0.0 – 1.0 normalised position (used by mouse drag) */
    public void setNormalized(float t) {
        setValue(min + (max - min) * Math.max(0f, Math.min(1f, t)));
    }

    private double snapToStep(double v) {
        return Math.round(v / step) * step;
    }

    @Override
    public String toString() {
        // Two decimal places is enough for reach/angle values
        return String.format("%.2f", value);
    }
}
