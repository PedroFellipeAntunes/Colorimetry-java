package colorimetry.spaces.perceptual;

import colorimetry.ColorSpace;
import colorimetry.spaces.rgb.Bt2020RgbLinear;

/**
 * ICtCp color space descriptor.
 *
 * Source: ITU-R BT.2100-2, "Image parameter values for high dynamic range
 *         television for use in production and international programme exchange".
 *         Dolby Laboratories, "ICtCp" white paper.
 *
 * A perceptually uniform space for HDR/WCG content. Uses the Perceptual
 * Quantizer (PQ, SMPTE ST 2084) transfer function.
 *
 * SDR white (Y=1 in the library's XYZ scale) is mapped to 203 cd/m² per
 * ITU-R BT.2408 before PQ encoding.
 */
public final class ICtCp implements ColorSpace {
    public static final ICtCp INSTANCE = new ICtCp();

    // ===== METADATA =====

    private static final String[] NAMES = {"Intensity", "Tritan", "Protan"};
    private static final double[] MINS = {0.0, -0.5, -0.5};
    private static final double[] MAXS = {1.0, 0.5, 0.5};
    private static final double[] DEFAULTS = {0.5, 0.0, 0.0};

    // PQ (ST 2084) constants
    private static final double PQ_M1 = 2610.0 / 16384.0;
    private static final double PQ_M2 = 2523.0 / 4096.0 * 128.0;
    private static final double PQ_C1 = 3424.0 / 4096.0;
    private static final double PQ_C2 = 2413.0 / 128.0;
    private static final double PQ_C3 = 2392.0 / 128.0;
    private static final double PQ_LMAX = 10000.0;

    // SDR reference white luminance (BT.2408)
    private static final double SDR_WHITE = 203.0;

    // BT.2020 linear RGB → LMS crosstalk matrix (BT.2100)
    private static final double[][] RGB_TO_LMS = {
        {1688.0 / 4096.0, 2146.0 / 4096.0,  262.0 / 4096.0},
        { 683.0 / 4096.0, 2951.0 / 4096.0,  462.0 / 4096.0},
        {  99.0 / 4096.0,  309.0 / 4096.0, 3688.0 / 4096.0}
    };

    // LMS → BT.2020 linear RGB inverse crosstalk matrix
    private static final double[][] LMS_TO_RGB = {
        { 3.4366066943, -2.5064521187,  0.0698454243},
        {-0.7913295556,  1.9836004518, -0.1922708962},
        {-0.0259498997, -0.0989137147,  1.1248636144}
    };

    // PQ-encoded L'M'S' → ICtCp matrix
    private static final double[][] LMSP_TO_ICTCP = {
        {2048.0 / 4096.0,   2048.0 / 4096.0,     0.0 / 4096.0},
        {6610.0 / 4096.0, -13613.0 / 4096.0,  7003.0 / 4096.0},
        {17933.0 / 4096.0, -17390.0 / 4096.0,  -543.0 / 4096.0}
    };

    // ICtCp → PQ-encoded L'M'S' matrix
    private static final double[][] ICTCP_TO_LMSP = {
        {1.0,  0.0086090370,  0.1110296250},
        {1.0, -0.0086090370, -0.1110296250},
        {1.0,  0.5600313357, -0.3206271749}
    };

    private ICtCp() {}

    @Override
    public String displayName() {
        return "ICtCp";
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
    public boolean isChannelBounded(int i) {
        // I [0, 1] is physically bounded; Ct and Cp are not
        return i == 0;
    }

    // ===== MATH =====

    /**
     * PQ inverse EOTF (ST 2084): compresses absolute luminance to [0,1] signal.
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
     * 3x3 matrix-vector multiplication.
     *
     * @param m 3x3 matrix
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

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Bt2020RgbLinear.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // ICtCp → PQ-encoded L'M'S'
        double[] lmsP = matMul(ICTCP_TO_LMSP, raw);

        // PQ decode to absolute luminance
        double[] lms = new double[] {
            pqDecode(lmsP[0]),
            pqDecode(lmsP[1]),
            pqDecode(lmsP[2])
        };

        // Remove SDR scaling
        lms[0] /= SDR_WHITE;
        lms[1] /= SDR_WHITE;
        lms[2] /= SDR_WHITE;

        // LMS → BT.2020 linear [0,1] → raw [0,255]
        double[] rgb = matMul(LMS_TO_RGB, lms);

        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // BT.2020 linear raw [0,255] → [0,1]
        double[] rgb = {parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0};

        // BT.2020 linear → LMS (crosstalk)
        double[] lms = matMul(RGB_TO_LMS, rgb);

        // Scale to absolute luminance (SDR white = 203 nits per BT.2408)
        lms[0] *= SDR_WHITE;
        lms[1] *= SDR_WHITE;
        lms[2] *= SDR_WHITE;

        // PQ encode each LMS channel
        double[] lmsP = new double[] {
            pqEncode(lms[0]),
            pqEncode(lms[1]),
            pqEncode(lms[2])
        };

        // PQ-encoded L'M'S' → ICtCp
        return matMul(LMSP_TO_ICTCP, lmsP);
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