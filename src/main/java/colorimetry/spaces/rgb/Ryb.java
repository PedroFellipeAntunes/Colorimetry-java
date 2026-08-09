package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;

/**
 * RYB (Red, Yellow, Blue) color space descriptor.
 *
 * Source: Gossett and Chen, "Paint Inspired Color Mixing and Compositing
 *         for Visualization", IEEE INFOVIS, 2004.
 *
 * Uses trilinear interpolation of 8 RGB corners to map the RYB cube to RGB.
 * Simulates subtractive paint mixing.
 */
public final class Ryb implements ColorSpace {
    public static final Ryb INSTANCE = new Ryb();

    private static final String[] NAMES = {"Red", "Yellow", "Blue"};

    // Full 8-corner lookup: corners[r][y][b] = {R, G, B}
    private static final double[][][][] CORNERS = {
        { // r=0
            {{1.0, 1.0, 1.0}, {0.163, 0.373, 0.6}}, // y=0, b=0..1
            {{1.0, 1.0, 0.0}, {0.0, 0.66, 0.2}} // y=1, b=0..1
        },
        { // r=1
            {{1.0, 0.0, 0.0}, {0.5, 0.0, 0.5}}, // y=0, b=0..1
            {{1.0, 0.5, 0.0}, {0.0, 0.0, 0.0}} // y=1, b=0..1
        }
    };

    private Ryb() {}

    @Override
    public String displayName() {
        return "RYB";
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
        return 255.0;
    }

    @Override
    public double componentDefault(int i) {
        return 0.0;
    }
    // ===== MATH =====

    /**
     * Trilinear interpolation of the RYB cube corners to produce RGB.
     */
    private static double[] rybToRgb(double r, double y, double b) {
        double[] result = new double[3];

        for (int ch = 0; ch < 3; ch++) {
            double c000 = CORNERS[0][0][0][ch];
            double c001 = CORNERS[0][0][1][ch];
            double c010 = CORNERS[0][1][0][ch];
            double c011 = CORNERS[0][1][1][ch];
            double c100 = CORNERS[1][0][0][ch];
            double c101 = CORNERS[1][0][1][ch];
            double c110 = CORNERS[1][1][0][ch];
            double c111 = CORNERS[1][1][1][ch];

            double c00 = c000 * (1 - r) + c100 * r;
            double c01 = c001 * (1 - r) + c101 * r;
            double c10 = c010 * (1 - r) + c110 * r;
            double c11 = c011 * (1 - r) + c111 * r;

            double c0 = c00 * (1 - y) + c10 * y;
            double c1 = c01 * (1 - y) + c11 * y;

            result[ch] = c0 * (1 - b) + c1 * b;
        }

        return result;
    }

    /**
     * Inverse mapping from RGB to RYB using iterative Newton-Raphson.
     */
    private static double[] rgbToRyb(double r, double g, double b) {
        // Initial guess
        double ry = 0.5, yy = 0.5, by = 0.5;

        for (int iter = 0; iter < 20; iter++) {
            double[] rgb = rybToRgb(ry, yy, by);
            double er = r - rgb[0];
            double eg = g - rgb[1];
            double eb = b - rgb[2];

            if (Math.abs(er) < 1e-10 && Math.abs(eg) < 1e-10 && Math.abs(eb) < 1e-10) {
                break;
            }

            // Numerical Jacobian
            double eps = 1e-6;
            double[] dr = rybToRgb(ry + eps, yy, by);
            double[] dy = rybToRgb(ry, yy + eps, by);
            double[] db = rybToRgb(ry, yy, by + eps);

            double j00 = (dr[0] - rgb[0]) / eps, j01 = (dy[0] - rgb[0]) / eps, j02 = (db[0] - rgb[0]) / eps;
            double j10 = (dr[1] - rgb[1]) / eps, j11 = (dy[1] - rgb[1]) / eps, j12 = (db[1] - rgb[1]) / eps;
            double j20 = (dr[2] - rgb[2]) / eps, j21 = (dy[2] - rgb[2]) / eps, j22 = (db[2] - rgb[2]) / eps;

            double det = j00 * (j11 * j22 - j12 * j21) - j01 * (j10 * j22 - j12 * j20) + j02 * (j10 * j21 - j11 * j20);

            if (Math.abs(det) < 1e-15) {
                break;
            }

            double inv = 1.0 / det;
            ry += inv * ((j11 * j22 - j12 * j21) * er + (j02 * j21 - j01 * j22) * eg + (j01 * j12 - j02 * j11) * eb);
            yy += inv * ((j12 * j20 - j10 * j22) * er + (j00 * j22 - j02 * j20) * eg + (j02 * j10 - j00 * j12) * eb);
            by += inv * ((j10 * j21 - j11 * j20) * er + (j01 * j20 - j00 * j21) * eg + (j00 * j11 - j01 * j10) * eb);

            ry = ColorSpace.clamp(ry, 0.0, 1.0);
            yy = ColorSpace.clamp(yy, 0.0, 1.0);
            by = ColorSpace.clamp(by, 0.0, 1.0);
        }

        return new double[] {ry, yy, by};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return SRgb.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double[] rgb = rybToRgb(raw[0] / 255.0, raw[1] / 255.0, raw[2] / 255.0);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double[] ryb = rgbToRyb(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
        
        return new double[] {ryb[0] * 255.0, ryb[1] * 255.0, ryb[2] * 255.0};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double formatRaw(double value) {
        return Math.round(value);
    }

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {raw[0] / 255.0, raw[1] / 255.0, raw[2] / 255.0};
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {normalized[0] * 255.0, normalized[1] * 255.0, normalized[2] * 255.0};
    }
}