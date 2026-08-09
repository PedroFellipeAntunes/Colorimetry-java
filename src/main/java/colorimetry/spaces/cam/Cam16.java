package colorimetry.spaces.cam;

import colorimetry.ColorSpace;
import colorimetry.spaces.xyz.XyzD65;

/**
 * CAM16 color appearance model descriptor.
 *
 * Source: Li, Li, Wang, Zu, Luo, Cui, "Comprehensive color solutions: CAM16, CAT16,
 *         and s-CIECAM97", Color Research &amp; Application, 2017.
 *
 * Stores J (lightness), C (chroma), h (hue angle). Uses fixed average-surround
 * viewing conditions with D65 adapted white, La = 64 cd/m², Yb = 20.
 *
 * Internally operates in XYZ scale [0, 100] as specified by the paper.
 * The library's XYZ D65 uses [0, 1], so fromParent/toParent scale by 100.
 */
public final class Cam16 implements ColorSpace {
    public static final Cam16 INSTANCE = new Cam16();

    // ===== METADATA =====

    private static final String[] NAMES = {"Lightness", "Chroma", "Hue"};
    private static final double[] MINS = {0.0, 0.0, 0.0};
    private static final double[] MAXS = {100.0, 120.0, 360.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    // CAT16 forward matrix (XYZ → sharpened cone-like RGB)
    private static final double[][] M16 = {
        { 0.401288,  0.650173, -0.051461},
        {-0.250268,  1.204414,  0.045854},
        {-0.002079,  0.048952,  0.953127}
    };

    // CAT16 inverse matrix
    private static final double[][] M16_INV = {
        { 1.862068, -1.011255,  0.149187},
        { 0.387527,  0.621447, -0.008974},
        {-0.015841, -0.034123,  1.049964}
    };

    // ===== PRECOMPUTED VIEWING CONDITION CONSTANTS =====
    // D65 white in [0,100] scale: (95.047, 100, 108.883)
    // La = 64 cd/m², Yb = 20, average surround

    // Surround: average
    private static final double C_SUR = 0.69;
    private static final double NC = 1.0;
    private static final double F_SUR = 1.0;

    // Luminance adaptation — all in [0, 100] scale per paper
    private static final double LA = 64.0;
    private static final double YB = 20.0;
    private static final double YW = 100.0;

    // Derived constants
    private static final double K;
    private static final double FL;
    private static final double N;
    private static final double NBB;
    private static final double NCB;
    private static final double Z;
    private static final double D;
    private static final double[] D_RGB;
    private static final double A_W;

    static {
        K = 1.0 / (5.0 * LA + 1.0);
        double k4 = K * K * K * K;
        FL = 0.2 * k4 * (5.0 * LA) + 0.1 * (1.0 - k4) * (1.0 - k4) * Math.cbrt(5.0 * LA);
        N = YB / YW;
        NBB = 0.725 * Math.pow(1.0 / N, 0.2);
        NCB = NBB;
        Z = 1.48 + Math.sqrt(N);

        // Degree of adaptation
        double dRaw = F_SUR * (1.0 - (1.0 / 3.6) * Math.exp(-(LA + 42.0) / 92.0));
        D = Math.max(0.0, Math.min(1.0, dRaw));

        // White point in [0, 100] scale
        double[] rgbW = matMul(M16, new double[] {95.047, 100.0, 108.883});
        D_RGB = new double[] {
            D * YW / rgbW[0] + 1.0 - D,
            D * YW / rgbW[1] + 1.0 - D,
            D * YW / rgbW[2] + 1.0 - D
        };

        // White point achromatic response
        double[] rgbCw = new double[] {
            rgbW[0] * D_RGB[0],
            rgbW[1] * D_RGB[1],
            rgbW[2] * D_RGB[2]
        };

        double[] rgbAw = new double[] {
            adaptResponse(rgbCw[0]),
            adaptResponse(rgbCw[1]),
            adaptResponse(rgbCw[2])
        };

        A_W = (2.0 * rgbAw[0] + rgbAw[1] + rgbAw[2] / 20.0 - 0.305) * NBB;
    }

    private Cam16() {}

    @Override
    public String displayName() {
        return "CAM16";
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
     * Nonlinear post-adaptation compression. Applies a sigmoidal response
     * curve to a chromatically adapted cone signal in [0, 100] scale.
     *
     * @param x adapted cone signal (may be negative for out-of-gamut)
     * @return compressed response
     */
    static double adaptResponse(double x) {
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
    static double invertResponse(double x) {
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

        // J → achromatic response A
        double A = A_W * Math.pow(J / 100.0, 1.0 / (C_SUR * Z));
        double p2 = A / NBB + 0.305;

        // t from chroma and J
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

        // Invert post-adaptation response — back to [0, 100] scale cone signals
        double[] rgbC = new double[] {
            invertResponse(rgbA0),
            invertResponse(rgbA1),
            invertResponse(rgbA2)
        };

        // Undo chromatic adaptation
        double[] rgb = new double[] {
            rgbC[0] / D_RGB[0],
            rgbC[1] / D_RGB[1],
            rgbC[2] / D_RGB[2]
        };

        // M16 inverse → XYZ in [0, 100] scale, divide by 100 for library convention
        double[] xyz100 = matMul(M16_INV, rgb);

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

        // XYZ [0,100] → sharpened cone response
        double[] rgb = matMul(M16, xyz100);

        // Chromatic adaptation
        double[] rgbC = new double[] {
            rgb[0] * D_RGB[0],
            rgb[1] * D_RGB[1],
            rgb[2] * D_RGB[2]
        };

        // Post-adaptation compression
        double[] rgbA = new double[] {
            adaptResponse(rgbC[0]),
            adaptResponse(rgbC[1]),
            adaptResponse(rgbC[2])
        };

        // Opponent signals
        double a = rgbA[0] - 12.0 * rgbA[1] / 11.0 + rgbA[2] / 11.0;
        double b = (rgbA[0] + rgbA[1] - 2.0 * rgbA[2]) / 9.0;

        // Hue angle
        double hRad = Math.atan2(b, a);
        double hDeg = Math.toDegrees(hRad);

        if (hDeg < 0.0) {
            hDeg += 360.0;
        }

        // Eccentricity factor
        double et = 0.25 * (Math.cos(hRad + 2.0) + 3.8);

        // Achromatic response
        double A = (2.0 * rgbA[0] + rgbA[1] + rgbA[2] / 20.0 - 0.305) * NBB;

        // Lightness
        double J = 100.0 * Math.pow(A / A_W, C_SUR * Z);

        // Chroma
        double t = 50000.0 / 13.0 * NC * NCB * et * Math.sqrt(a * a + b * b) / (rgbA[0] + rgbA[1] + 21.0 * rgbA[2] / 20.0);
        double n073 = Math.pow(1.64 - Math.pow(0.29, N), 0.73);
        double chroma = Math.pow(t, 0.9) * Math.sqrt(J / 100.0) * n073;

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
     * Used by CAM16UCS to compute colorfulness M from chroma C.
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