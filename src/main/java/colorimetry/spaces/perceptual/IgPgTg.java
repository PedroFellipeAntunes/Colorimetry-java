package colorimetry.spaces.perceptual;

import colorimetry.ColorSpace;

/**
 * IgPgTg color space descriptor.
 *
 * Source: Derived from IPT (Ebner and Fairchild, 1998) with Perceptual
 *         Quantizer (SMPTE ST 2084) replacing the power 0.43 compression,
 *         and the BT.2100 opponent matrix replacing IPT's opponent matrix.
 *
 * SDR white (Y=1) is mapped to 100 cd/m^2 before PQ encoding.
 */
public final class IgPgTg implements ColorSpace {
    public static final IgPgTg INSTANCE = new IgPgTg();

    private static final String[] NAMES = {"Ig", "Pg", "Tg"};
    private static final double[] MINS = {0.0, -1.0, -1.0};
    private static final double[] MAXS = {1.0, 1.0, 1.0};
    private static final double[] DEFAULTS = {0.5, 0.0, 0.0};

    private static final double SDR_WHITE = 100.0;

    // PQ (ST 2084) constants
    private static final double PQ_M1 = 2610.0 / 16384.0;
    private static final double PQ_M2 = 2523.0 / 4096.0 * 128.0;
    private static final double PQ_C1 = 3424.0 / 4096.0;
    private static final double PQ_C2 = 2413.0 / 128.0;
    private static final double PQ_C3 = 2392.0 / 128.0;
    private static final double PQ_LMAX = 10000.0;

    // PQ-encoded LMS' -> IgPgTg (BT.2100 opponent matrix)
    private static final double[][] LMSP_TO_IGPGTG = {
        {2048.0 / 4096.0,  2048.0 / 4096.0,     0.0 / 4096.0},
        {6610.0 / 4096.0, -13613.0 / 4096.0,  7003.0 / 4096.0},
        {17933.0 / 4096.0, -17390.0 / 4096.0, -543.0 / 4096.0}
    };

    // IgPgTg -> PQ-encoded LMS'
    private static final double[][] IGPGTG_TO_LMSP = invert3x3(LMSP_TO_IGPGTG);

    private IgPgTg() {}

    @Override
    public String displayName() {
        return "IgPgTg";
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
    public boolean isChannelBounded(int i) {
        return i == 0;
    }

    // ===== MATH =====

    private static double pqEncode(double x) {
        double xp = Math.pow(Math.max(x, 0.0) / PQ_LMAX, PQ_M1);
        
        return Math.pow((PQ_C1 + PQ_C2 * xp) / (1.0 + PQ_C3 * xp), PQ_M2);
    }

    private static double pqDecode(double x) {
        double xp = Math.pow(Math.max(x, 0.0), 1.0 / PQ_M2);
        double num = Math.max(xp - PQ_C1, 0.0);
        double den = PQ_C2 - PQ_C3 * xp;
        
        return PQ_LMAX * Math.pow(num / den, 1.0 / PQ_M1);
    }

    private static double[] matMul(double[][] m, double[] v) {
        return new double[] {
            m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]
        };
    }

    private static double[][] invert3x3(double[][] m) {
        double a = m[0][0], b = m[0][1], c = m[0][2];
        double d = m[1][0], e = m[1][1], f = m[1][2];
        double g = m[2][0], h = m[2][1], k = m[2][2];
        
        double det = a * (e * k - f * h) - b * (d * k - f * g) + c * (d * h - e * g);
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
        return LmsHpe.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double[] lmsP = matMul(IGPGTG_TO_LMSP, raw);

        return new double[] {
            pqDecode(lmsP[0]) / SDR_WHITE,
            pqDecode(lmsP[1]) / SDR_WHITE,
            pqDecode(lmsP[2]) / SDR_WHITE
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double[] lmsP = new double[] {
            pqEncode(parentRaw[0] * SDR_WHITE),
            pqEncode(parentRaw[1] * SDR_WHITE),
            pqEncode(parentRaw[2] * SDR_WHITE)
        };

        return matMul(LMSP_TO_IGPGTG, lmsP);
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