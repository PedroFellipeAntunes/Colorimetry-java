package colorimetry.spaces.xyz;

import colorimetry.ColorSpace;
import colorimetry.types.XyzLike;

/**
 * CIE XYZ color space adapted to CIE Standard Illuminant D50 (horizon daylight).
 *
 * Source: CIE 015:2018, "Colorimetry, 4th Edition".
 *         D50 white point chromaticity: x = 0.3457, y = 0.3585.
 *         Tristimulus: X = 0.96422, Y = 1.00000, Z = 0.82521.
 *
 * Parent of color spaces that reference D50 as their adapted white point,
 * including CIE Lab, CIE LCh, and ProPhoto RGB. Also the standard white
 * point for the ICC Profile Connection Space (PCS).
 *
 * Conversion to the absolute Xyz root uses Bradford chromatic adaptation
 * from D50 to Illuminant E.
 */
public final class XyzD50 implements XyzLike {
    public static final XyzD50 INSTANCE = new XyzD50();

    private static final String[] NAMES = {"X", "Y", "Z"};

    // Bradford chromatic adaptation: D50 → Illuminant E
    private static final double[][] D50_TO_E = {
        { 1.0025535214,  0.0036237643,  0.0359836639},
        { 0.0096913857,  0.9819124889,  0.0105947374},
        { 0.0089181318, -0.0160789393,  1.2208769867}
    };

    // Bradford chromatic adaptation: Illuminant E → D50
    private static final double[][] E_TO_D50 = {
        { 0.9977544968, -0.0041631883, -0.0293713085},
        {-0.0097677169,  1.0183167521, -0.0085490352},
        {-0.0074169312,  0.0134416336,  0.8191852976}
    };

    // D50 white point tristimulus values
    private static final double[] NEUTRAL = {0.96422, 1.00000, 0.82521};

    private XyzD50() {}

    @Override
    public double[] referenceWhite() {
        return new double[] {0.96422, 1.00000, 0.82521};
    }

    @Override
    public String displayName() {
        return "CIE XYZ D50";
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
        // Bradford D50 → Illuminant E (absolute root)
        return new double[] {
            D50_TO_E[0][0] * raw[0] + D50_TO_E[0][1] * raw[1] + D50_TO_E[0][2] * raw[2],
            D50_TO_E[1][0] * raw[0] + D50_TO_E[1][1] * raw[1] + D50_TO_E[1][2] * raw[2],
            D50_TO_E[2][0] * raw[0] + D50_TO_E[2][1] * raw[1] + D50_TO_E[2][2] * raw[2]
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Illuminant E (absolute root) → Bradford → D50
        return new double[] {
            E_TO_D50[0][0] * parentRaw[0] + E_TO_D50[0][1] * parentRaw[1] + E_TO_D50[0][2] * parentRaw[2],
            E_TO_D50[1][0] * parentRaw[0] + E_TO_D50[1][1] * parentRaw[1] + E_TO_D50[1][2] * parentRaw[2],
            E_TO_D50[2][0] * parentRaw[0] + E_TO_D50[2][1] * parentRaw[1] + E_TO_D50[2][2] * parentRaw[2]
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