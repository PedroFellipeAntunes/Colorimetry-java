package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * CIE L*a*b* (1976) color space descriptor.
 *
 * Source: CIE Publication 15:2004, "Colorimetry".
 *         Originally defined in CIE 1976 (L*, a*, b*) recommendation.
 */
public final class CieLab implements ColorSpace {

    public static final CieLab INSTANCE = new CieLab();

    // ===== METADATA =====

    private static final String[] NAMES = {"Lightness", "Green-Red", "Blue-Yellow"};
    private static final String[] SHORTS = {"L", "A", "B"};
    private static final double[] MINS = {0.0, -128.0, -128.0};
    private static final double[] MAXS = {100.0, 127.0, 127.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    // CIE D50 illuminant tristimulus values (reference white for Lab)
    private static final double Xn = 0.96422;
    private static final double Yn = 1.00000;
    private static final double Zn = 0.82521;

    // Bradford chromatic adaptation matrices between D65 (hub) and D50 (Lab)
    private static final double[][] D65_TO_D50 = {
        { 1.0478112, 0.0228866, -0.0501270},
        { 0.0295424, 0.9904844, -0.0170491},
        {-0.0092345, 0.0150436, 0.7521316}
    };

    private static final double[][] D50_TO_D65 = {
        { 0.9555766, -0.0230393, 0.0631636},
        {-0.0282895, 1.0099416, 0.0210077},
        { 0.0122982, -0.0204830, 1.3299098}
    };

    private CieLab() {}

    @Override
    public String displayName() {
        return "CIE Lab";
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
        return 0.1;
    }
    
    @Override
    public boolean isBounded() {
        return false;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : SHORTS[i];
    }

    // ===== MATH =====

    /**
     * CIE f(t) forward function. Piecewise: cube root above epsilon (0.008856),
     * linear approximation below to avoid infinite slope near zero.
     *
     * @param value t/tn ratio (e.g. X/Xn)
     * @return f(t) used to compute L*, a*, b*
     */
    private static double labForward(double value) {
        return value > 0.008856 ? Math.cbrt(value) : 7.787 * value + 16.0 / 116.0;
    }

    /**
     * CIE f(t) inverse function. Recovers t/tn ratio from f(t).
     * Threshold 0.206893 is cbrt(0.008856).
     *
     * @param value f(t) value
     * @return t/tn ratio
     */
    private static double labInverse(double value) {
        return value > 0.206893 ? value * value * value : (value - 16.0 / 116.0) / 7.787;
    }

    /**
     * Applies a 3×3 chromatic adaptation matrix to an XYZ triplet.
     *
     * @param matrix 3×3 adaptation matrix (e.g. D65_TO_D50)
     * @param x X component
     * @param y Y component
     * @param z Z component
     * @return transformed XYZ triplet
     */
    private static double[] chromaticAdapt(double[][] matrix, double x, double y, double z) {
        return new double[] {
            matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z,
            matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z,
            matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z
        };
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Xyz.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double lightness = raw[0];
        double a = raw[1];
        double b = raw[2];
        
        // Recover f(x), f(y), f(z) from L*, a*, b*
        double fy = (lightness + 16.0) / 116.0;
        double fx = a / 500.0 + fy;
        double fz = fy - b / 200.0;
        
        // Invert f(t) and scale by D50 white point to get D50 XYZ
        double x50 = Xn * labInverse(fx);
        double y50 = Yn * labInverse(fy);
        double z50 = Zn * labInverse(fz);
        
        // Adapt from D50 back to D65 (the library's hub illuminant)
        return chromaticAdapt(D50_TO_D65, x50, y50, z50);
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Adapt from D65 hub to D50 (Lab's reference illuminant)
        double[] d50 = chromaticAdapt(D65_TO_D50, parentRaw[0], parentRaw[1], parentRaw[2]);
        
        // Apply CIE f(t) to each channel normalized by D50 white point
        double fx = labForward(d50[0] / Xn);
        double fy = labForward(d50[1] / Yn);
        double fz = labForward(d50[2] / Zn);
        
        // Compute L*, a*, b* from f(x), f(y), f(z)
        return new double[] {
            116.0 * fy - 16.0,
            500.0 * (fx - fy),
            200.0 * (fy - fz)
        };
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / 100.0,
            (raw[1] + 128.0) / 255.0, // shift [-128,127] to [0,1]
            (raw[2] + 128.0) / 255.0
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * 100.0,
            normalized[1] * 255.0 - 128.0, // shift [0,1] back to [-128,127]
            normalized[2] * 255.0 - 128.0
        };
    }

    @Override
    public boolean isInGamut(double[] xyz) {
        return true;
    }

    @Override
    public double[] neutralXyz() {
        // L*=50, a*=0, b*=0 converted to XYZ
        return toParent(new double[] {50.0, 0.0, 0.0});
    }
}