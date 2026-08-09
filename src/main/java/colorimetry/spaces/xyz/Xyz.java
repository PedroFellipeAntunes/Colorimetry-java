package colorimetry.spaces.xyz;

import colorimetry.ColorSpace;

/**
 * CIE XYZ absolute color space.
 *
 * This space is illuminant-independent and represents raw tristimulus values
 * as defined by the CIE 1931 2° Standard Observer. It acts as the conversion hub
 * between illuminant-adapted subtrees (D65, D50, etc.).
 *
 * The root uses Illuminant E (equal-energy, X=Y=Z) as its neutral reference.
 * Illuminant-specific children (XyzD65, XyzD50) perform Bradford chromatic
 * adaptation to/from this neutral root.
 */
public final class Xyz implements ColorSpace {
    public static final Xyz INSTANCE = new Xyz();

    private static final String[] NAMES = {"X", "Y", "Z"};

    private Xyz() {}

    @Override
    public String displayName() {
        return "CIE XYZ";
    }

    @Override
    public int componentCount() {
        return NAMES.length;
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
        return 1.0;
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
        // Illuminant E: equal-energy neutral reference
        return new double[] {1.0, 1.0, 1.0};
    }
}