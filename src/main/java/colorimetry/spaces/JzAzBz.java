package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * JzAzBz color space descriptor.
 *
 * Source: Safdar, Cui, Kim and Luo (2017), "Perceptually uniform color space for image
 *         signals including high dynamic range and wide gamut", Optics Express
 *         25(13), 15131.
 *
 * A perceptually uniform space designed for HDR/WCG content. Uses the Perceptual
 * Quantizer (SMPTE ST 2084) transfer function internally, which operates on absolute
 * luminance. For SDR content (XYZ ≈ [0,1]), Jz values are small (~0.005 for white)
 * because PQ's domain is [0, 10000] nits.
 */
public final class JzAzBz implements ColorSpace {
    public static final JzAzBz INSTANCE = new JzAzBz();

    private static final String[] NAMES = {"Jz", "az", "bz"};
    private static final double[] MINS = {0.0, -0.02, -0.02};
    private static final double[] MAXS = {0.02, 0.02, 0.02};
    private static final double[] DEFAULTS = {0.005, 0.0, 0.0};

    // JzAzBz-specific constants
    private static final double B = 1.15;
    private static final double G = 0.66;
    private static final double D = -0.56;
    private static final double D0 = 1.6295499532821566e-11;

    // PQ (ST 2084) constants — m2 re-optimized for JzAzBz
    private static final double PQ_M1 = 2610.0 / 16384.0;
    private static final double PQ_M2 = 1.7 * 2523.0 / 32.0;
    private static final double PQ_C1 = 3424.0 / 4096.0;
    private static final double PQ_C2 = 2413.0 / 128.0;
    private static final double PQ_C3 = 2392.0 / 128.0;
    private static final double PQ_LMAX = 10000.0;

    // XYZ' → LMS matrix
    private static final double[][] XYZ_TO_LMS = {
        { 0.41478972,  0.579999,  0.0146480},
        {-0.2015100,   1.120649,  0.0531008},
        {-0.0166008,   0.264800,  0.6684799}
    };

    // PQ-encoded LMS' → IzAzBz matrix
    private static final double[][] LMS_P_TO_IZAZBZ = {
        {0.500000,  0.500000,  0.000000},
        {3.524000, -4.066708,  0.542708},
        {0.199076,  1.096799, -1.295875}
    };

    // Precomputed inverse matrices
    private static final double[][] LMS_TO_XYZ = invert3x3(XYZ_TO_LMS);
    private static final double[][] IZAZBZ_TO_LMS_P = invert3x3(LMS_P_TO_IZAZBZ);

    private JzAzBz() {}

    @Override
    public String displayName() {
        return "JzAzBz";
    }

    @Override
    public int componentCount() {
        return 3;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
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
        return 0.0001;
    }

    // ===== MATH =====

    /**
     * PQ inverse EOTF (ST 2084): compresses absolute luminance to [0,1] signal.
     * Uses JzAzBz's re-optimized m2 exponent.
     *
     * @param x absolute luminance value
     * @return PQ-encoded signal in [0, 1]
     */
    private static double pqEncode(double x) {
        double xp = Math.pow(Math.max(x, 0.0) / PQ_LMAX, PQ_M1);
        
        return Math.pow((PQ_C1 + PQ_C2 * xp) / (1.0 + PQ_C3 * xp), PQ_M2);
    }

    /**
     * PQ EOTF (ST 2084): recovers absolute luminance from [0,1] signal.
     * Uses JzAzBz's re-optimized m2 exponent.
     *
     * @param x PQ-encoded signal
     * @return absolute luminance value
     */
    private static double pqDecode(double x) {
        double xp = Math.pow(Math.max(x, 0.0), 1.0 / PQ_M2);
        double num = Math.max(xp - PQ_C1, 0.0);
        double den = PQ_C2 - PQ_C3 * xp;
        
        return PQ_LMAX * Math.pow(num / den, 1.0 / PQ_M1);
    }

    /**
     * 3×3 matrix × 3-vector multiplication.
     *
     * @param m 3×3 matrix
     * @param v 3-element vector
     * @return result vector
     */
    private static double[] matMul(double[][] m, double[] v) {
        return new double[] {
            m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]
        };
    }

    /**
     * Computes the inverse of a 3×3 matrix via cofactor expansion.
     *
     * @param m 3×3 matrix
     * @return inverted 3×3 matrix
     */
    private static double[][] invert3x3(double[][] m) {
        double a = m[0][0], b = m[0][1], c = m[0][2];
        double d = m[1][0], e = m[1][1], f = m[1][2];
        double g = m[2][0], h = m[2][1], k = m[2][2];

        double det = a * (e * k - f * h)
                   - b * (d * k - f * g)
                   + c * (d * h - e * g);
        double inv = 1.0 / det;

        return new double[][] {
            {(e * k - f * h) * inv, (c * h - b * k) * inv, (b * f - c * e) * inv},
            {(f * g - d * k) * inv, (a * k - c * g) * inv, (c * d - a * f) * inv},
            {(d * h - e * g) * inv, (b * g - a * h) * inv, (a * e - b * d) * inv}
        };
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Xyz.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double Jz = raw[0];
        double az = raw[1];
        double bz = raw[2];

        // Jz → Iz: undo the perceptual adjustment
        double Iz = (Jz + D0) / (1.0 + D - D * (Jz + D0));

        // IzAzBz → PQ-encoded LMS
        double[] lmsP = matMul(IZAZBZ_TO_LMS_P, new double[] {Iz, az, bz});

        // Undo PQ encoding to recover absolute LMS
        double L = pqDecode(lmsP[0]);
        double M = pqDecode(lmsP[1]);
        double S = pqDecode(lmsP[2]);

        // LMS → modified XYZ'
        double[] xyzP = matMul(LMS_TO_XYZ, new double[] {L, M, S});

        // Undo the XYZ pre-modification to recover D65 XYZ
        double X = (xyzP[0] + (B - 1.0) * xyzP[2]) / B;
        double Y = (xyzP[1] + (G - 1.0) * X) / G;

        return new double[] {X, Y, xyzP[2]};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double X = parentRaw[0];
        double Y = parentRaw[1];
        double Z = parentRaw[2];

        // Pre-modify XYZ to enhance blue accuracy
        double Xp = B * X - (B - 1.0) * Z;
        double Yp = G * Y - (G - 1.0) * X;

        // Modified XYZ' → LMS
        double[] lms = matMul(XYZ_TO_LMS, new double[] {Xp, Yp, Z});

        // Apply PQ encoding to each LMS channel
        double Lp = pqEncode(lms[0]);
        double Mp = pqEncode(lms[1]);
        double Sp = pqEncode(lms[2]);

        // PQ-encoded LMS → IzAzBz
        double[] iab = matMul(LMS_P_TO_IZAZBZ, new double[] {Lp, Mp, Sp});

        // Iz → Jz: perceptual adjustment for lightness
        double Jz = ((1.0 + D) * iab[0]) / (1.0 + D * iab[0]) - D0;

        return new double[] {Jz, iab[1], iab[2]};
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
        return toParent(new double[] {DEFAULTS[0], 0.0, 0.0});
    }
}
