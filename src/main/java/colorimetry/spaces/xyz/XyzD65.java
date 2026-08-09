package colorimetry.spaces.xyz;

import colorimetry.ColorSpace;
import colorimetry.types.XyzLike;

/**
 * CIE XYZ color space adapted to CIE Standard Illuminant D65 (midday daylight).
 *
 * Source: CIE 015:2018, "Colorimetry, 4th Edition".
 *         D65 white point chromaticity: x = 0.3127, y = 0.3290.
 *         Tristimulus: X = 0.95047, Y = 1.00000, Z = 1.08883.
 *
 * Parent of all color spaces that reference D65 as their adapted white point,
 * including sRGB/BT.709, Display P3, Adobe RGB, OkLab, CIE Luv, and others.
 *
 * Conversion to the absolute Xyz root uses Bradford chromatic adaptation
 * from D65 to Illuminant E.
 */
public final class XyzD65 implements XyzLike {
    public static final XyzD65 INSTANCE = new XyzD65();

    private static final String[] NAMES = {"X", "Y", "Z"};

    // Bradford chromatic adaptation: D65 → Illuminant E
    private static final double[][] D65_TO_E = {
        { 1.0502616160,  0.0270756503, -0.0232523062},
        { 0.0390649557,  0.9729501919, -0.0092578826},
        {-0.0024046676,  0.0026445727,  0.9180872972}
    };

    // Bradford chromatic adaptation: Illuminant E → D65
    private static final double[][] E_TO_D65 = {
        { 0.9531874262, -0.0265905737,  0.0238731475},
        {-0.0382466561,  1.0288406195,  0.0094060366},
        { 0.0026067728, -0.0030332467,  1.0892564739}
    };

    // D65 white point tristimulus values
    private static final double[] NEUTRAL = {0.95047, 1.00000, 1.08883};

    private XyzD65() {}

    @Override
    public double[] referenceWhite() {
        return new double[] {0.95047, 1.00000, 1.08883};
    }

    @Override
    public String displayName() {
        return "CIE XYZ D65";
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
        return NEUTRAL[i];
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
    public double[] neutralXyz() {
        return NEUTRAL.clone();
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Xyz.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // Bradford D65 → Illuminant E (absolute root)
        return new double[] {
            D65_TO_E[0][0] * raw[0] + D65_TO_E[0][1] * raw[1] + D65_TO_E[0][2] * raw[2],
            D65_TO_E[1][0] * raw[0] + D65_TO_E[1][1] * raw[1] + D65_TO_E[1][2] * raw[2],
            D65_TO_E[2][0] * raw[0] + D65_TO_E[2][1] * raw[1] + D65_TO_E[2][2] * raw[2]
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Illuminant E (absolute root) → Bradford → D65
        return new double[] {
            E_TO_D65[0][0] * parentRaw[0] + E_TO_D65[0][1] * parentRaw[1] + E_TO_D65[0][2] * parentRaw[2],
            E_TO_D65[1][0] * parentRaw[0] + E_TO_D65[1][1] * parentRaw[1] + E_TO_D65[1][2] * parentRaw[2],
            E_TO_D65[2][0] * parentRaw[0] + E_TO_D65[2][1] * parentRaw[1] + E_TO_D65[2][2] * parentRaw[2]
        };
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / NEUTRAL[0],
            raw[1] / NEUTRAL[1],
            raw[2] / NEUTRAL[2]
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * NEUTRAL[0],
            normalized[1] * NEUTRAL[1],
            normalized[2] * NEUTRAL[2]
        };
    }
}