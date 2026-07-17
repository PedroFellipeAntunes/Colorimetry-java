package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * CIE XYZ D65 color space. Internal root of the parent hierarchy.
 * Not registered in ColorSpaceRegistry, exists only as the conversion hub.
 *
 * All conversions are identity since raw values ARE XYZ triplets.
 */
public final class Xyz implements ColorSpace {
    public static final Xyz INSTANCE = new Xyz();

    private static final String[] NAMES = {"X", "Y", "Z"};
    private static final double[] MAXS = {1.0, 1.0, 1.0};

    private Xyz() {}

    @Override
    public String displayName() {
        return "CIE XYZ";
    }

    @Override
    public int componentCount() {
        return 3;
    }

    @Override
    public String componentName(int i, boolean full) {
        return NAMES[i];
    }

    @Override
    public double componentMin(int i) {
        return 0.0;
    }

    @Override
    public double componentMax(int i) {
        return MAXS[i];
    }

    @Override
    public double componentDefault(int i) {
        return 0.0;
    }

    @Override
    public double componentStep(int i) {
        return 0.001;
    }

    @Override
    public double[] normalize(double[] raw) {
        return raw.clone();
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return normalized.clone();
    }

    @Override
    public boolean isBounded() {
        return false;
    }

    @Override
    public boolean isInGamut(double[] xyz) {
        return true;
    }

    @Override
    public double[] neutralXyz() {
        return new double[] {0.47524, 0.50000, 0.54442};
    }
}