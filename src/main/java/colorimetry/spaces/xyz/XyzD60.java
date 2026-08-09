package colorimetry.spaces.xyz;

import colorimetry.ColorSpace;
import colorimetry.types.XyzLike;

/**
 * CIE XYZ color space adapted to the ACES D60 white point.
 *
 * Source: SMPTE ST 2065-1:2012, "Academy Color Encoding Specification".
 *         White point chromaticity: x = 0.32168, y = 0.33767.
 *         Tristimulus: X = 0.95265, Y = 1.00000, Z = 1.00883.
 *
 * Conversion to the absolute Xyz root uses Bradford chromatic adaptation
 * from D60 to Illuminant E.
 */
public final class XyzD60 implements XyzLike {
    public static final XyzD60 INSTANCE = new XyzD60();

    private static final String[] NAMES = {"X", "Y", "Z"};

    // Bradford chromatic adaptation: D60 -> Illuminant E
    private static final double[][] D60_TO_E = {
        { 1.0366114829,  0.0208452296, -0.0082958770},
        { 0.0311722122,  0.9745611329, -0.0042199764},
        { 0.0004189314, -0.0020015282,  0.9928404350}
    };

    // Bradford chromatic adaptation: Illuminant E -> D60
    private static final double[][] E_TO_D60 = {
        { 0.9652987447, -0.0206307280,  0.0079780579},
        {-0.0308779796,  1.0267717883,  0.0041061913},
        {-0.0004695589,  0.0020786377,  1.0072161056}
    };

    // D60 white point tristimulus values
    private static final double[] NEUTRAL = {0.95265, 1.00000, 1.00883};

    private XyzD60() {}

    @Override
    public double[] referenceWhite() {
        return new double[] {0.95265, 1.00000, 1.00883};
    }

    @Override
    public String displayName() {
        return "CIE XYZ D60";
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
        return new double[] {
            D60_TO_E[0][0] * raw[0] + D60_TO_E[0][1] * raw[1] + D60_TO_E[0][2] * raw[2],
            D60_TO_E[1][0] * raw[0] + D60_TO_E[1][1] * raw[1] + D60_TO_E[1][2] * raw[2],
            D60_TO_E[2][0] * raw[0] + D60_TO_E[2][1] * raw[1] + D60_TO_E[2][2] * raw[2]
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        return new double[] {
            E_TO_D60[0][0] * parentRaw[0] + E_TO_D60[0][1] * parentRaw[1] + E_TO_D60[0][2] * parentRaw[2],
            E_TO_D60[1][0] * parentRaw[0] + E_TO_D60[1][1] * parentRaw[1] + E_TO_D60[1][2] * parentRaw[2],
            E_TO_D60[2][0] * parentRaw[0] + E_TO_D60[2][1] * parentRaw[1] + E_TO_D60[2][2] * parentRaw[2]
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