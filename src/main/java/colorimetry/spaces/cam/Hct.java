package colorimetry.spaces.cam;

import colorimetry.ColorSpace;
import colorimetry.spaces.xyz.XyzD65;

/**
 * HCT (Hue, Chroma, Tone) color space descriptor.
 *
 * Source: Google Material Design, "The Science of Color and Design", 2021.
 *         James O'Leary, Google LLC.
 *
 * A hybrid space combining CAM16 hue and chroma with CIE Lab L* (tone).
 * Designed for accessible UI color systems.
 *
 * Forward: XYZ to CAM16 (H, C) + Lab L* (T)
 * Inverse: (H, C, T) to binary search on CAM16 J to match target Y from T.
 */
public final class Hct implements ColorSpace {
    public static final Hct INSTANCE = new Hct();

    private static final String[] NAMES = {"Hue", "Chroma", "Tone"};
    private static final double[] MINS = {0.0, 0.0, 0.0};
    private static final double[] MAXS = {360.0, 120.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 50.0, 50.0};

    // CAM16 viewing conditions (precomputed for La=64, Yb=20, average surround)
    private static final double[][] M16 = {
        { 0.401288,  0.650173, -0.051461},
        {-0.250268,  1.204414,  0.045854},
        {-0.002079,  0.048952,  0.953127}
    };

    private static final double[][] M16_INV = {
        { 1.8620678551, -1.0112546305,  0.1491867754},
        { 0.3875265432,  0.6214474419, -0.0089739852},
        {-0.0158414988, -0.0341229380,  1.0499644369}
    };

    private static final double FL = 0.683990384569650;
    private static final double N = 0.2;
    private static final double NBB = 1.000304004559381;
    private static final double NCB = NBB;
    private static final double NC = 1.0;
    private static final double Z = 1.927213595499958;
    private static final double C_SUR = 0.69;
    private static final double A_W = 37.169079667906033;

    private static final double[] D_RGB = {
        1.022860379894422,
        0.985219808282477,
        0.928713331845570
    };

    // CIE Lab constants
    private static final double LAB_DELTA = 6.0 / 29.0;
    private static final double LAB_DELTA_SQ = LAB_DELTA * LAB_DELTA;
    private static final double LAB_KAPPA = 903.2962962962963;

    private Hct() {}

    @Override
    public String displayName() {
        return "HCT";
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
        return i == 0 ? 1.0 : 0.1;
    }

    @Override
    public boolean isChannelBounded(int i) {
        return i == 0 || i == 2;
    }

    // ===== MATH =====

    private static double adaptResponse(double x) {
        double abs = Math.abs(x);
        double p = Math.pow(FL * abs / 100.0, 0.42);

        return Math.signum(x) * 400.0 * p / (p + 27.13) + 0.1;
    }

    private static double invertResponse(double x) {
        double t = x - 0.1;
        double abs = Math.abs(t);

        return Math.signum(t) * 100.0 / FL * Math.pow(27.13 * abs / (400.0 - abs), 1.0 / 0.42);
    }

    private static double[] matMul(double[][] m, double[] v) {
        return new double[] {
            m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]
        };
    }

    private static double lstarFromY(double Y) {
        double fy = (Y > LAB_DELTA_SQ * LAB_DELTA) ? Math.cbrt(Y) : (LAB_KAPPA * Y + 16.0) / 116.0;

        return 116.0 * fy - 16.0;
    }

    private static double yFromLstar(double L) {
        double fy = (L + 16.0) / 116.0;

        return (fy > LAB_DELTA) ? fy * fy * fy : (116.0 * fy - 16.0) / LAB_KAPPA;
    }

    private static double[] cam16Forward(double[] xyz) {
        double[] xyz100 = {xyz[0] * 100.0, xyz[1] * 100.0, xyz[2] * 100.0};
        double[] rgb = matMul(M16, xyz100);

        double[] rgbC = {
            rgb[0] * D_RGB[0],
            rgb[1] * D_RGB[1],
            rgb[2] * D_RGB[2]
        };

        double[] rgbA = {
            adaptResponse(rgbC[0]),
            adaptResponse(rgbC[1]),
            adaptResponse(rgbC[2])
        };

        double a = rgbA[0] - 12.0 * rgbA[1] / 11.0 + rgbA[2] / 11.0;
        double b = (rgbA[0] + rgbA[1] - 2.0 * rgbA[2]) / 9.0;

        double hRad = Math.atan2(b, a);
        double hDeg = Math.toDegrees(hRad);

        if (hDeg < 0.0) {
            hDeg += 360.0;
        }

        double et = 0.25 * (Math.cos(hRad + 2.0) + 3.8);
        double A = (2.0 * rgbA[0] + rgbA[1] + rgbA[2] / 20.0 - 0.305) * NBB;
        double J = 100.0 * Math.pow(A / A_W, C_SUR * Z);

        double t = 50000.0 / 13.0 * NC * NCB * et * Math.sqrt(a * a + b * b) / (rgbA[0] + rgbA[1] + 21.0 * rgbA[2] / 20.0);
        double n073 = Math.pow(1.64 - Math.pow(0.29, N), 0.73);
        double C = Math.pow(t, 0.9) * Math.sqrt(J / 100.0) * n073;

        return new double[] {J, C, hDeg};
    }

    private static double[] cam16Inverse(double J, double C, double hDeg) {
        if (J == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        double hRad = Math.toRadians(ColorSpace.wrap(hDeg, 360.0));
        double A = A_W * Math.pow(J / 100.0, 1.0 / (C_SUR * Z));
        double p2 = A / NBB + 0.305;

        double n073 = Math.pow(1.64 - Math.pow(0.29, N), 0.73);
        double sqrtJ = Math.sqrt(J / 100.0);
        double t = (C == 0.0 || sqrtJ == 0.0) ? 0.0 : Math.pow(C / (sqrtJ * n073), 1.0 / 0.9);

        double cosH = Math.cos(hRad);
        double sinH = Math.sin(hRad);
        double a;
        double b;

        if (t == 0.0) {
            a = 0.0;
            b = 0.0;
        } else {
            double et = 0.25 * (Math.cos(hRad + 2.0) + 3.8);
            double p1 = 50000.0 / 13.0 * NC * NCB * et;
            double denom = 1403.0 * p1 + t * (671.0 * cosH + 6588.0 * sinH);
            double r = 1403.0 * t * p2 / denom;
            a = r * cosH;
            b = r * sinH;
        }

        double rgbA0 = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0;
        double rgbA1 = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0;
        double rgbA2 = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0;

        double[] rgbC = {
            invertResponse(rgbA0),
            invertResponse(rgbA1),
            invertResponse(rgbA2)
        };

        double[] rgb = {
            rgbC[0] / D_RGB[0],
            rgbC[1] / D_RGB[1],
            rgbC[2] / D_RGB[2]
        };

        double[] xyz100 = matMul(M16_INV, rgb);

        return new double[] {xyz100[0] / 100.0, xyz100[1] / 100.0, xyz100[2] / 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return XyzD65.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double H = raw[0];
        double C = raw[1];
        double T = raw[2];

        double Y = yFromLstar(T);

        if (Y <= 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        // Binary search on CAM16 J to find XYZ with correct Y
        double jLow = 0.0;
        double jHigh = 100.0;
        double[] result = {0.0, 0.0, 0.0};

        for (int i = 0; i < 30; i++) {
            double jMid = (jLow + jHigh) / 2.0;
            double[] xyz = cam16Inverse(jMid, C, H);

            if (xyz[1] < Y) {
                jLow = jMid;
            } else {
                jHigh = jMid;
            }

            result = xyz;
        }

        return result;
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double[] jch = cam16Forward(parentRaw);
        double T = lstarFromY(parentRaw[1]);

        return new double[] {jch[2], jch[1], T};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            ColorSpace.wrap(raw[0], MAXS[0]) / MAXS[0],
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

    @Override
    public boolean hasPalette() {
        return true;
    }

    @Override
    public int[] paletteChannels() {
        return new int[] {0, 2};
    }

    @Override
    public boolean isCylindrical() {
        return true;
    }

    @Override
    public int hueChannel() {
        return 0;
    }

    @Override
    public int radialChannel() {
        return 1;
    }
}