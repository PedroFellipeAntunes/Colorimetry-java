package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * OKLAB perceptual color space descriptor.
 *
 * Source: B. Ottosson, "A perceptual color space for image processing", 2020.
 *         https://bottosson.github.io/posts/oklab/
 */
public final class Oklab implements ColorSpace {
    public static final Oklab INSTANCE = new Oklab();

    // ===== METADATA =====

    // Approximate practical bounds of a and b for sRGB gamut, used for normalization
    static final double A_MAX = 0.2762;
    static final double B_MAX = 0.3118;

    private static final String[] NAMES = {"Lightness", "Green-Red", "Blue-Yellow"};
    private static final double[] MINS = {0.0, -A_MAX, -B_MAX};
    private static final double[] MAXS = {1.0, A_MAX, B_MAX};
    private static final double[] DEFAULTS = {0.5, 0.0, 0.0};

    private Oklab() {}

    @Override
    public String displayName() {
        return "OK Lab";
    }
    
    @Override
    public int componentCount() {
        return NAMES.length;
    }
    
    @Override
    public double componentMin(int i) {
        return MINS[i];
    }
    
    @Override
    public double componentMax(int i) {
        return MAXS[i];
    }
    
    @Override
    public double componentDefault(int i) {
        return DEFAULTS[i];
    }
    
    @Override
    public double componentStep(int i) {
        return 0.001;
    }
    
    @Override
    public boolean isBounded() {
        return false;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
    }

    // ===== MATH =====

    /**
     * Converts raw Oklab (L, a, b) to CIE XYZ D65.
     * Pipeline: Lab → LMS (cube roots) → LMS (cubed) → XYZ
     *
     * @param L lightness in [0, 1]
     * @param a green-red axis
     * @param b blue-yellow axis
     * @return CIE XYZ D65 triplet
     */
    static double[] rawOklabToXyz(double L, double a, double b) {
        // Inverse of the Lab→LMS matrix to get cone response cube roots
        double l_ = L + 0.3963377774 * a + 0.2158037573 * b;
        double m_ = L - 0.1055613458 * a - 0.0638541728 * b;
        double s_ = L - 0.0894841775 * a - 1.2914855480 * b;
        
        // Undo the cube root (perceptual linearization)
        double l = l_ * l_ * l_;
        double m = m_ * m_ * m_;
        double s = s_ * s_ * s_;
        
        // LMS to XYZ D65
        return new double[]{
             1.2270138511 * l - 0.5577999807 * m + 0.2812561490 * s,
            -0.0405801784 * l + 1.1122568696 * m - 0.0716766787 * s,
            -0.0763812845 * l - 0.4214819784 * m + 1.5861632204 * s
        };
    }

    /**
     * Converts CIE XYZ D65 to raw Oklab (L, a, b).
     * Pipeline: XYZ → LMS → LMS cube roots → Lab
     *
     * @param xyz CIE XYZ D65 triplet
     * @return Oklab [L, a, b]
     */
    static double[] rawXyzToOklab(double[] xyz) {
        // XYZ D65 to LMS cone response
        double l = 0.8189330101 * xyz[0] + 0.3618667424 * xyz[1] - 0.1288597137 * xyz[2];
        double m = 0.0329845436 * xyz[0] + 0.9293118715 * xyz[1] + 0.0361456387 * xyz[2];
        double s = 0.0482003018 * xyz[0] + 0.2643662691 * xyz[1] + 0.6338517070 * xyz[2];
        
        // Cube root for perceptual linearization
        double l_ = Math.cbrt(l);
        double m_ = Math.cbrt(m);
        double s_ = Math.cbrt(s);
        
        // LMS cube roots to Lab
        return new double[]{
            0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
            1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
            0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_
        };
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Xyz.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        return rawOklabToXyz(raw[0], raw[1], raw[2]);
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        return rawXyzToOklab(parentRaw);
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[]{
            raw[0], // L already in [0,1]
            (raw[1] + A_MAX) / (2.0 * A_MAX), // shift [-A_MAX, A_MAX] to [0,1]
            (raw[2] + B_MAX) / (2.0 * B_MAX)
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[]{
            normalized[0],
            normalized[1] * 2.0 * A_MAX - A_MAX, // shift [0,1] back to [-A_MAX, A_MAX]
            normalized[2] * 2.0 * B_MAX - B_MAX
        };
    }

    @Override
    public boolean isInGamut(double[] xyz) {
        return true;
    }

    @Override
    public double[] neutralXyz() {
        // L=0.5, a=0, b=0 converted to XYZ
        return toParent(new double[]{0.5, 0.0, 0.0});
    }
}