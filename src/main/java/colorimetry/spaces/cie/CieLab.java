package colorimetry.spaces.cie;

import colorimetry.ColorSpace;
import colorimetry.types.LabLike;
import colorimetry.types.XyzLike;
import colorimetry.spaces.xyz.XyzD50;

/**
 * CIE L*a*b* (1976) color space descriptor.
 *
 * Source: CIE Publication 15:2004, "Colorimetry".
 *         Originally defined in CIE 1976 (L*, a*, b*) recommendation.
 */
public final class CieLab implements LabLike {
    public static final CieLab INSTANCE = new CieLab(XyzD50.INSTANCE);

    private final XyzLike parent;

    // ===== METADATA =====

    private static final String[] NAMES = {"Lightness", "Green-Red", "Blue-Yellow"};
    private static final String[] SHORTS = {"L", "A", "B"};
    private static final double[] MINS = {0.0, -128.0, -128.0};
    private static final double[] MAXS = {100.0, 127.0, 127.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    // Reference white point tristimulus values (from parent)
    private final double Xn;
    private final double Yn;
    private final double Zn;

    private CieLab(XyzLike parent) {
        this.parent = parent;
        double[] w = parent.referenceWhite();
        this.Xn = w[0];
        this.Yn = w[1];
        this.Zn = w[2];
    }

    /**
     * Creates an instance whose parent is the given adapted XYZ space.
     * The reference white point is taken from the parent.
     *
     * @param parent the adapted XYZ space this Lab derives from
     * @return a new CIE Lab descriptor parented to the given XYZ
     */
    public static CieLab of(XyzLike parent) {
        return new CieLab(parent);
    }

    @Override
    public String displayName() {
        if (parent == XyzD50.INSTANCE) {
            return "CIE Lab";
        }
        
        return "CIE Lab (" + parent.displayName() + ")";
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
    public boolean isChannelBounded(int i) {
        // L* [0, 100] is physically bounded; a* and b* are not
        return i == 0;
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

    @Override
    public Class<? extends ColorSpace> acceptedParentType() {
        return XyzLike.class;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return parent;
    }

    @Override
    public double[] toParent(double[] raw) {
        // Recover f(x), f(y), f(z) from L*, a*, b*
        double fy = (raw[0] + 16.0) / 116.0;
        double fx = raw[1] / 500.0 + fy;
        double fz = fy - raw[2] / 200.0;

        // Invert f(t) and scale by D50 white point to get D50 XYZ
        return new double[] {
            Xn * labInverse(fx),
            Yn * labInverse(fy),
            Zn * labInverse(fz)
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Parent is XyzD50 — values are already D50-adapted
        double fx = labForward(parentRaw[0] / Xn);
        double fy = labForward(parentRaw[1] / Yn);
        double fz = labForward(parentRaw[2] / Zn);

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
            raw[0] / MAXS[0],
            (raw[1] - MINS[1]) / (MAXS[1] - MINS[1]),
            (raw[2] - MINS[2]) / (MAXS[2] - MINS[2])
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * MAXS[0],
            normalized[1] * (MAXS[1] - MINS[1]) + MINS[1],
            normalized[2] * (MAXS[2] - MINS[2]) + MINS[2]
        };
    }
}