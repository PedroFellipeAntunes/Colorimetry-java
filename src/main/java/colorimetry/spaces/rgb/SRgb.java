package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;

/**
 * sRGB color space descriptor.
 *
 * Source: IEC 61966-2-1:1999, "Multimedia systems and equipment -
 *         Colour measurement and management".
 */
public final class SRgb implements ColorSpace {
    public static final SRgb INSTANCE = new SRgb(Bt709RgbLinear.INSTANCE);

    private final RgbLike parent;

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};

    private SRgb(RgbLike parent) {
        this.parent = parent;
    }

    /**
     * Creates an instance whose parent is the given linear RGB space.
     *
     * @param parent the linear RGB space this sRGB derives from
     * @return a new sRGB descriptor parented to the given RGB
     */
    public static SRgb of(RgbLike parent) {
        return new SRgb(parent);
    }

    @Override
    public String displayName() {
        if (parent == Bt709RgbLinear.INSTANCE) {
            return "sRGB";
        }
        
        return "sRGB (" + parent.displayName() + ")";
    }
    
    @Override
    public int componentCount() {
        return NAMES.length;
    }
    
    @Override
    public double componentMin(int i) {
        return 0.0;
    }
    
    @Override
    public double componentMax(int i) {
        return 255.0;
    }
    
    @Override
    public double componentDefault(int i) {
        return 0.0;
    }
    
    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
    }

    // ===== MATH =====

    /**
     * sRGB gamma decode (IEC 61966-2-1). Piecewise: linear below 0.04045,
     * power curve above. Converts a [0,1] gamma-encoded value to linear light.
     *
     * @param v gamma-encoded value in [0, 1]
     * @return linear-light value
     */
    private static double toLinear(double v) {
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /**
     * sRGB gamma encode (IEC 61966-2-1). Piecewise: linear below 0.0031308,
     * power curve above. Mirrors negative inputs so out-of-gamut values
     * round-trip without NaN from fractional exponents.
     *
     * @param v linear-light value
     * @return gamma-encoded value
     */
    private static double toGamma(double v) {
        if (v < 0.0) {
            return -toGamma(-v);
        }
        
        return v <= 0.0031308 ? 12.92 * v : 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055;
    }

    @Override
    public Class<? extends ColorSpace> acceptedParentType() {
        return RgbLike.class;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return parent;
    }

    @Override
    public double[] toParent(double[] raw) {
        // sRGB gamma [0,255] → linear [0,1] → Rgb raw [0,255]
        return new double[] {
            toLinear(raw[0] / 255.0) * 255.0,
            toLinear(raw[1] / 255.0) * 255.0,
            toLinear(raw[2] / 255.0) * 255.0
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear [0,1] → sRGB gamma [0,255]
        return new double[] {
            toGamma(parentRaw[0] / 255.0) * 255.0,
            toGamma(parentRaw[1] / 255.0) * 255.0,
            toGamma(parentRaw[2] / 255.0) * 255.0
        };
    }

    // ===== COLORSPACE OVERRIDES =====
    
    @Override
    public double formatRaw(double value) {
        return Math.round(value);
    }

    @Override
    public double[] normalize(double[] raw) {
        return new double[]{raw[0] / 255.0, raw[1] / 255.0, raw[2] / 255.0};
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[]{
            normalized[0] * 255.0,
            normalized[1] * 255.0,
            normalized[2] * 255.0
        };
    }
}