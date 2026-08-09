package colorimetry.spaces.cam;

import colorimetry.ColorSpace;
import colorimetry.spaces.xyz.XyzD65;

/**
 * ZCAM color appearance model descriptor (JMH form).
 *
 * Source: Safdar, Hardeberg and Luo, "ZCAM, a colour appearance model based
 *         on a high dynamic range uniform colour space", Optics Express 29(4),
 *         2021. DOI: 10.1364/OE.413659
 *
 * Based on JzAzBz with chromatic adaptation and appearance attributes.
 * Fixed average-surround viewing conditions: D65 white, La=64, Yb=20.
 * SDR white (Y=1) maps to 203 cd/m^2 per ITU-R BT.2408.
 *
 * Channels: Jz (lightness 0-100), Mz (colorfulness), hz (hue 0-360).
 */
public final class Zcam implements ColorSpace {
    public static final Zcam INSTANCE = new Zcam();

    private static final String[] NAMES = {"Lightness", "Colorfulness", "Hue"};
    private static final double[] MINS = {0.0, 0.0, 0.0};
    private static final double[] MAXS = {100.0, 65.0, 360.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    // SDR reference white luminance (BT.2408)
    private static final double SDR_WHITE = 203.0;

    // Viewing conditions (precomputed La=64, Yb=20, average surround)
    private static final double FB = 0.447213595499958;
    private static final double FL = 0.684;
    private static final double QZ_P = 1.215926360123153;
    private static final double QZ_M = 0.273991265883348;
    private static final double IZ_W = 0.393484180722099;
    private static final double QZ_W = 237.991738487258885;

    // PQ constants (JzAzBz re-optimized m2)
    private static final double PQ_M1 = 2610.0 / 16384.0;
    private static final double PQ_M2 = 1.7 * 2523.0 / 32.0;
    private static final double PQ_C1 = 3424.0 / 4096.0;
    private static final double PQ_C2 = 2413.0 / 128.0;
    private static final double PQ_C3 = 2392.0 / 128.0;
    private static final double PQ_LMAX = 10000.0;

    // JzAzBz XYZ modification constants
    private static final double B = 1.15;
    private static final double G = 0.66;

    // Inverse Mz exponent (50/37, per Safdar 2021 Step 4 inverse)
    private static final double MZ_INV_EXP = 50.0 / 37.0;

    // XYZ' -> LMS
    private static final double[][] XYZ_TO_LMS = {
        { 0.41478972,  0.579999,  0.0146480},
        {-0.2015100,   1.120649,  0.0531008},
        {-0.0166008,   0.264800,  0.6684799}
    };

    // LMS -> XYZ' (precomputed inverse)
    private static final double[][] LMS_TO_XYZ = {
        { 1.9242264358, -1.0047923126,  0.0376514040},
        { 0.3503167621,  0.7264811939, -0.0653844229},
        {-0.0909828110, -0.3127282905,  1.5227665613}
    };

    // PQ-encoded LMS' -> IzAzBz
    private static final double[][] LMS_P_TO_IZAZBZ = {
        {0.500000,  0.500000,  0.000000},
        {3.524000, -4.066708,  0.542708},
        {0.199076,  1.096799, -1.295875}
    };

    // IzAzBz -> PQ-encoded LMS' (precomputed inverse)
    private static final double[][] IZAZBZ_TO_LMS_P = {
        { 1.0000000000,  0.1386050433,  0.0580473162},
        { 1.0000000000, -0.1386050433, -0.0580473162},
        { 1.0000000000, -0.0960192420, -0.8118918961}
    };

    private Zcam() {}

    @Override
    public String displayName() {
        return "ZCAM";
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
        return i == 2 ? 1.0 : 0.1;
    }

    @Override
    public boolean isChannelBounded(int i) {
        return i == 0 || i == 2;
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

    /**
     * Converts absolute XYZ (nits) to IzAzBz.
     * Uses the JzAzBz pipeline without the Jz perceptual adjustment.
     */
    private static double[] xyzToIzAzBz(double[] xyz) {
        double Xp = B * xyz[0] - (B - 1.0) * xyz[2];
        double Yp = G * xyz[1] - (G - 1.0) * xyz[0];

        double[] lms = matMul(XYZ_TO_LMS, new double[] {Xp, Yp, xyz[2]});
        double[] lmsP = {pqEncode(lms[0]), pqEncode(lms[1]), pqEncode(lms[2])};

        return matMul(LMS_P_TO_IZAZBZ, lmsP);
    }

    /**
     * Converts IzAzBz to absolute XYZ (nits).
     */
    private static double[] izAzBzToXyz(double[] iab) {
        double[] lmsP = matMul(IZAZBZ_TO_LMS_P, iab);

        double L = pqDecode(lmsP[0]);
        double M = pqDecode(lmsP[1]);
        double S = pqDecode(lmsP[2]);

        double[] xyzP = matMul(LMS_TO_XYZ, new double[] {L, M, S});
        double X = (xyzP[0] + (B - 1.0) * xyzP[2]) / B;
        double Y = (xyzP[1] + (G - 1.0) * X) / G;

        return new double[] {X, Y, xyzP[2]};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return XyzD65.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double Jz = raw[0];
        double Mz = raw[1];
        double hz = ColorSpace.wrap(raw[2], 360.0);

        if (Jz <= 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        // Step 1 inverse: Jz -> Iz
        double Iz = Math.pow((Jz * QZ_W) / (2700.0 * 100.0 * QZ_M), 1.0 / QZ_P);

        // Step 2 inverse: Mz -> az, bz
        double ez = 1.015 + Math.cos(Math.toRadians(89.038 + hz));
        double t = Math.pow((Mz * Math.pow(IZ_W, 0.78) * Math.pow(FB, 0.1)) / (100.0 * Math.pow(ez, 0.068) * Math.pow(FL, 0.2)), MZ_INV_EXP);

        double hrad = Math.toRadians(hz);
        double az = t * Math.cos(hrad);
        double bz = t * Math.sin(hrad);

        // IzAzBz -> absolute XYZ -> library XYZ
        double[] xyzAbs = izAzBzToXyz(new double[] {Iz, az, bz});

        return new double[] {
            xyzAbs[0] / SDR_WHITE,
            xyzAbs[1] / SDR_WHITE,
            xyzAbs[2] / SDR_WHITE
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double[] xyzAbs = {
            parentRaw[0] * SDR_WHITE,
            parentRaw[1] * SDR_WHITE,
            parentRaw[2] * SDR_WHITE
        };

        double[] iab = xyzToIzAzBz(xyzAbs);
        double Iz = iab[0];
        double az = iab[1];
        double bz = iab[2];

        // Hue angle
        double hz = Math.toDegrees(Math.atan2(bz, az));

        if (hz < 0.0) {
            hz += 360.0;
        }

        // Brightness and lightness
        double Qz = 2700.0 * Math.pow(Iz, QZ_P) * QZ_M;
        double Jz = 100.0 * Qz / QZ_W;

        // Colorfulness
        double ez = 1.015 + Math.cos(Math.toRadians(89.038 + hz));
        double Mz = 100.0 * Math.pow(az * az + bz * bz, 0.37) * ((Math.pow(ez, 0.068) * Math.pow(FL, 0.2)) / (Math.pow(FB, 0.1) * Math.pow(IZ_W, 0.78)));

        return new double[] {Jz, Mz, hz};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / MAXS[0],
            raw[1] / MAXS[1],
            ColorSpace.wrap(raw[2], MAXS[2]) / MAXS[2]
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

    @Override
    public boolean hasPalette() {
        return true;
    }

    @Override
    public int[] paletteChannels() {
        return new int[] {2, 0};
    }

    @Override
    public boolean isCylindrical() {
        return true;
    }

    @Override
    public int hueChannel() {
        return 2;
    }

    @Override
    public int radialChannel() {
        return 1;
    }
}