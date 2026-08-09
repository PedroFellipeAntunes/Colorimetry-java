package colorimetry.spaces.cam;

import colorimetry.ColorSpace;
import colorimetry.spaces.xyz.XyzD65;

/**
 * CIECAM02 color appearance model descriptor.
 *
 * Source: CIE 159:2004, "A Colour Appearance Model for Colour Management
 *         Systems: CIECAM02". Moroney, Fairchild, Hunt, Li, Luo, Newman.
 *
 * Stores J (lightness), C (chroma), h (hue angle). Uses fixed average-surround
 * viewing conditions with D65 adapted white, La = 64 cd/m², Yb = 20.
 *
 * Internally operates in XYZ scale [0, 100] as specified by the paper.
 * The library's XYZ D65 uses [0, 1], so fromParent/toParent scale by 100.
 *
 * Unlike CAM16 which uses M16 for both adaptation and post-adaptation,
 * CIECAM02 uses MCAT02 for chromatic adaptation and then converts to
 * Hunt-Pointer-Estévez (HPE) space for post-adaptation compression.
 */
public final class Cam02 implements ColorSpace {
    public static final Cam02 INSTANCE = new Cam02();

    // ===== METADATA =====

    private static final String[] NAMES = {"Lightness", "Chroma", "Hue"};
    private static final double[] MINS = {0.0, 0.0, 0.0};
    private static final double[] MAXS = {100.0, 120.0, 360.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    // MCAT02 forward matrix (XYZ → sharpened RGB)
    private static final double[][] MCAT02 = {
        { 0.7328,  0.4296, -0.1624},
        {-0.7036,  1.6975,  0.0061},
        { 0.0030,  0.0136,  0.9834}
    };

    // MCAT02 inverse matrix
    private static final double[][] MCAT02_INV = {
        { 1.096124, -0.278869,  0.182745},
        { 0.454369,  0.473533,  0.072098},
        {-0.009628, -0.005698,  1.015326}
    };

    // Hunt-Pointer-Estévez (HPE) matrix for post-adaptation
    private static final double[][] MHPE = {
        { 0.38971, 0.68898, -0.07868},
        {-0.22981, 1.18340,  0.04641},
        { 0.00000, 0.00000,  1.00000}
    };

    // HPE inverse matrix
    private static final double[][] MHPE_INV = {
        { 1.910197, -1.112124,  0.201908},
        { 0.370950,  0.629054, -0.000008},
        { 0.000000,  0.000000,  1.000000}
    };

    // ===== PRECOMPUTED VIEWING CONDITION CONSTANTS =====
    // All in [0, 100] scale per paper
    private static final double C_SUR = 0.69;
    private static final double NC = 1.0;
    private static final double F_SUR = 1.0;
    private static final double LA = 64.0;
    private static final double YB = 20.0;
    private static final double YW = 100.0;

    private static final double FL;
    private static final double N;
    private static final double NBB;
    private static final double NCB;
    private static final double Z;
    private static final double D;
    private static final double[] D_RGB;
    private static final double A_W;

    static {
        double k = 1.0 / (5.0 * LA + 1.0);
        double k4 = k * k * k * k;
        FL = 0.2 * k4 * (5.0 * LA) + 0.1 * (1.0 - k4) * (1.0 - k4) * Math.cbrt(5.0 * LA);
        N = YB / YW;
        NBB = 0.725 * Math.pow(1.0 / N, 0.2);
        NCB = NBB;
        Z = 1.48 + Math.sqrt(N);

        double dRaw = F_SUR * (1.0 - (1.0 / 3.6) * Math.exp(-(LA + 42.0) / 92.0));
        D = Math.max(0.0, Math.min(1.0, dRaw));

        // White point in [0, 100] scale
        double[] rgbW = matMul(MCAT02, new double[] {95.047, 100.0, 108.883});
        D_RGB = new double[] {
            D * YW / rgbW[0] + 1.0 - D,
            D * YW / rgbW[1] + 1.0 - D,
            D * YW / rgbW[2] + 1.0 - D
        };

        // White point achromatic response through CAT02 → HPE path
        double[] rgbCw = new double[] {
            rgbW[0] * D_RGB[0],
            rgbW[1] * D_RGB[1],
            rgbW[2] * D_RGB[2]
        };

        double[] xyzCw = matMul(MCAT02_INV, rgbCw);
        double[] hpeW = matMul(MHPE, xyzCw);
        double[] rgbAw = new double[] {
            adaptResponse(hpeW[0]),
            adaptResponse(hpeW[1]),
            adaptResponse(hpeW[2])
        };

        A_W = (2.0 * rgbAw[0] + rgbAw[1] + rgbAw[2] / 20.0 - 0.305) * NBB;
    }

    private Cam02() {}

    @Override
    public String displayName() {
        return "CAM02";
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
        // J [0, 100] and h [0, 360) are bounded; C is not
        return i == 0 || i == 2;
    }

    // ===== MATH =====

    /**
     * Nonlinear post-adaptation compression (same form as CAM16).
     *
     * @param x adapted cone signal in [0, 100] scale
     * @return compressed response
     */
    private static double adaptResponse(double x) {
        double abs = Math.abs(x);
        double p = Math.pow(FL * abs / 100.0, 0.42);

        return Math.signum(x) * 400.0 * p / (p + 27.13) + 0.1;
    }

    /**
     * Inverse of nonlinear post-adaptation compression.
     *
     * @param x compressed response value
     * @return adapted cone signal in [0, 100] scale
     */
    private static double invertResponse(double x) {
        double t = x - 0.1;
        double abs = Math.abs(t);

        return Math.signum(t) * 100.0 / FL * Math.pow(27.13 * abs / (400.0 - abs), 1.0 / 0.42);
    }

    /**
     * 3x3 matrix-vector multiplication.
     *
     * @param m 3x3 matrix
     * @param v 3-element vector
     * @return result vector
     */
    static double[] matMul(double[][] m, double[] v) {
        return new double[] {
            m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]
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

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return XyzD65.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double J = raw[0];
        double chroma = raw[1];
        double hDeg = ColorSpace.wrap(raw[2], 360.0);
        double hRad = Math.toRadians(hDeg);

        // Black
        if (J == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        double A = A_W * Math.pow(J / 100.0, 1.0 / (C_SUR * Z));
        double p2 = A / NBB + 0.305;

        double n073 = Math.pow(1.64 - Math.pow(0.29, N), 0.73);
        double sqrtJ = Math.sqrt(J / 100.0);
        double t = (chroma == 0.0 || sqrtJ == 0.0) ? 0.0 : Math.pow(chroma / (sqrtJ * n073), 1.0 / 0.9);

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

        // Solve the 3x3 system for post-adaptation RGB from (a, b, p2)
        double rgbA0 = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0;
        double rgbA1 = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0;
        double rgbA2 = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0;

        // Invert post-adaptation (HPE domain, [0,100] scale)
        double[] hpe = new double[] {
            invertResponse(rgbA0),
            invertResponse(rgbA1),
            invertResponse(rgbA2)
        };

        // HPE → XYZ → MCAT02 adapted
        double[] xyzC = matMul(MHPE_INV, hpe);
        double[] rgbC = matMul(MCAT02, xyzC);

        // Undo chromatic adaptation
        double[] rgb = new double[] {
            rgbC[0] / D_RGB[0],
            rgbC[1] / D_RGB[1],
            rgbC[2] / D_RGB[2]
        };

        // MCAT02 inverse → XYZ in [0, 100] scale, divide by 100 for library convention
        double[] xyz100 = matMul(MCAT02_INV, rgb);

        return new double[] {xyz100[0] / 100.0, xyz100[1] / 100.0, xyz100[2] / 100.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Scale XYZ from library [0, 1] to paper [0, 100]
        double[] xyz100 = new double[] {
            parentRaw[0] * 100.0,
            parentRaw[1] * 100.0,
            parentRaw[2] * 100.0
        };

        // XYZ [0,100] → MCAT02 sharpened
        double[] rgb = matMul(MCAT02, xyz100);

        // Chromatic adaptation
        double[] rgbC = new double[] {
            rgb[0] * D_RGB[0],
            rgb[1] * D_RGB[1],
            rgb[2] * D_RGB[2]
        };

        // MCAT02 adapted → XYZ → HPE for post-adaptation
        double[] xyzC = matMul(MCAT02_INV, rgbC);
        double[] hpe = matMul(MHPE, xyzC);

        // Post-adaptation compression
        double[] rgbA = new double[] {
            adaptResponse(hpe[0]),
            adaptResponse(hpe[1]),
            adaptResponse(hpe[2])
        };

        // Opponent signals
        double a = rgbA[0] - 12.0 * rgbA[1] / 11.0 + rgbA[2] / 11.0;
        double b = (rgbA[0] + rgbA[1] - 2.0 * rgbA[2]) / 9.0;

        // Hue
        double hRad = Math.atan2(b, a);
        double hDeg = Math.toDegrees(hRad);

        if (hDeg < 0.0) {
            hDeg += 360.0;
        }

        double et = 0.25 * (Math.cos(hRad + 2.0) + 3.8);
        double A = (2.0 * rgbA[0] + rgbA[1] + rgbA[2] / 20.0 - 0.305) * NBB;
        double J = 100.0 * Math.pow(A / A_W, C_SUR * Z);

        double tVal = 50000.0 / 13.0 * NC * NCB * et * Math.sqrt(a * a + b * b) / (rgbA[0] + rgbA[1] + 21.0 * rgbA[2] / 20.0);
        double n073 = Math.pow(1.64 - Math.pow(0.29, N), 0.73);
        double chroma = Math.pow(tVal, 0.9) * Math.sqrt(J / 100.0) * n073;

        return new double[] {J, chroma, hDeg};
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

    // ===== ACCESSORS FOR CHILD SPACES =====

    /**
     * Returns the luminance adaptation factor FL.
     *
     * @return FL constant for the fixed viewing conditions
     */
    static double fl() {
        return FL;
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