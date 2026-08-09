package colorimetry.spaces.perceptual;

import colorimetry.ColorSpace;
import colorimetry.spaces.xyz.XyzD65;

/**
 * LMS color space using the Hunt-Pointer-Estevez (HPE) matrix.
 *
 * Source: Hunt, "The Reproduction of Colour", 6th edition, 2004.
 *         Estevez, "On the Fundamental Data-Base of Normal and Dichromatic
 *         Color Vision", PhD Thesis, University of Amsterdam, 1979.
 *
 * Models the response of the three cone types in the human retina:
 * L (long, red), M (medium, green), S (short, blue).
 */
public final class LmsHpe implements ColorSpace {
    public static final LmsHpe INSTANCE = new LmsHpe();

    private static final String[] NAMES = {"Long", "Medium", "Short"};
    private static final double[] MAXS = {1.0, 1.0, 1.0};

    // XYZ D65 -> LMS (Hunt-Pointer-Estevez)
    private static final double[][] XYZ_TO_LMS = {
        { 0.4002,  0.7076, -0.0808},
        {-0.2263,  1.1653,  0.0457},
        { 0.0000,  0.0000,  0.9182}
    };

    // LMS -> XYZ D65
    private static final double[][] LMS_TO_XYZ = {
        { 1.8600666125, -1.1294800781,  0.2198983030},
        { 0.3612229249,  0.6388043065, -0.0000071275},
        { 0.0000000000,  0.0000000000,  1.0890873448}
    };

    private LmsHpe() {}

    @Override
    public String displayName() {
        return "LMS (HPE)";
    }

    @Override
    public int componentCount() {
        return NAMES.length;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
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
    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return XyzD65.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        return new double[] {
            LMS_TO_XYZ[0][0] * raw[0] + LMS_TO_XYZ[0][1] * raw[1] + LMS_TO_XYZ[0][2] * raw[2],
            LMS_TO_XYZ[1][0] * raw[0] + LMS_TO_XYZ[1][1] * raw[1] + LMS_TO_XYZ[1][2] * raw[2],
            LMS_TO_XYZ[2][0] * raw[0] + LMS_TO_XYZ[2][1] * raw[1] + LMS_TO_XYZ[2][2] * raw[2]
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        return new double[] {
            XYZ_TO_LMS[0][0] * parentRaw[0] + XYZ_TO_LMS[0][1] * parentRaw[1] + XYZ_TO_LMS[0][2] * parentRaw[2],
            XYZ_TO_LMS[1][0] * parentRaw[0] + XYZ_TO_LMS[1][1] * parentRaw[1] + XYZ_TO_LMS[1][2] * parentRaw[2],
            XYZ_TO_LMS[2][0] * parentRaw[0] + XYZ_TO_LMS[2][1] * parentRaw[1] + XYZ_TO_LMS[2][2] * parentRaw[2]
        };
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / MAXS[0],
            raw[1] / MAXS[1],
            raw[2] / MAXS[2]
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * MAXS[0],
            normalized[1] * MAXS[1],
            normalized[2] * MAXS[2]
        };
    }

}