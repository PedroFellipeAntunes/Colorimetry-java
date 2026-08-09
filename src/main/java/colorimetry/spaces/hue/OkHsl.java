package colorimetry.spaces.hue;

import colorimetry.ColorSpace;
import colorimetry.spaces.perceptual.Oklab;

/**
 * OkHSL color space descriptor.
 *
 * Source: Bjorn Ottosson, "Okhsv and Okhsl - Two new color spaces for
 *         color picking", 2021.
 *         https://bottosson.github.io/posts/colorpicker/
 *
 * Perceptual HSL based on Oklab. Saturation is normalized to the maximum
 * chroma achievable in sRGB at the given lightness and hue.
 */
public final class OkHsl implements ColorSpace {
    public static final OkHsl INSTANCE = new OkHsl();

    private static final String[] NAMES = {"Hue", "Saturation", "Lightness"};
    private static final double[] MAXS = {360.0, 1.0, 1.0};

    private OkHsl() {}

    @Override
    public String displayName() {
        return "Ok HSL";
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
        if (i == 1) {
            return 1.0;
        }

        if (i == 2) {
            return 0.5;
        }

        return 0.0;
    }

    // ===== MATH =====

    private static double toe(double x) {
        double k1 = 0.206;
        double k2 = 0.03;
        double k3 = (1.0 + k1) / (1.0 + k2);

        return 0.5 * (k3 * x - k1 + Math.sqrt((k3 * x - k1) * (k3 * x - k1) + 4.0 * k2 * k3 * x));
    }

    private static double toeInv(double x) {
        double k1 = 0.206;
        double k2 = 0.03;
        double k3 = (1.0 + k1) / (1.0 + k2);

        return (x * x + k1 * x) / (k3 * (x + k2));
    }

    static double[] computeSt(double a, double b) {
        double S = computeMaxSaturation(a, b);
        double[] rgb = oklabToLinearSrgb(1.0, S * a, S * b);
        double cuspL = Math.cbrt(1.0 / Math.max(Math.max(rgb[0], rgb[1]), rgb[2]));
        double cuspC = cuspL * S;

        return new double[] {cuspC / cuspL, cuspC / (1.0 - cuspL)};
    }

    private static double computeMaxSaturation(double a, double b) {
        double k0;
        double k1;
        double k2;
        double k3;
        double k4;
        double wl;
        double wm;
        double ws;

        if (-1.88170328 * a - 0.80936493 * b > 1) {
            k0 = 1.19086277;
            k1 = 1.76576728;
            k2 = 0.59662641;
            k3 = 0.75515197;
            k4 = 0.56771245;
            wl = 4.0767416621;
            wm = -3.3077115913;
            ws = 0.2309699292;
        } else if (1.81444104 * a - 1.19445276 * b > 1) {
            k0 = 0.73956515;
            k1 = -0.45954404;
            k2 = 0.08285427;
            k3 = 0.12541070;
            k4 = -0.14503204;
            wl = -1.2684380046;
            wm = 2.6097574011;
            ws = -0.3413193965;
        } else {
            k0 = 1.35733652;
            k1 = -0.00915799;
            k2 = -1.15130210;
            k3 = -0.50559606;
            k4 = 0.00692167;
            wl = -0.0041960863;
            wm = -0.7034186147;
            ws = 1.7076147010;
        }

        double S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b;

        double kl = 0.3963377774 * a + 0.2158037573 * b;
        double km = -0.1055613458 * a - 0.0638541728 * b;
        double ks = -0.0894841775 * a - 1.2914855480 * b;

        double l_ = 1.0 + S * kl;
        double m_ = 1.0 + S * km;
        double s_ = 1.0 + S * ks;

        double l = l_ * l_ * l_;
        double m = m_ * m_ * m_;
        double s = s_ * s_ * s_;

        double ldS = 3.0 * kl * l_ * l_;
        double mdS = 3.0 * km * m_ * m_;
        double sdS = 3.0 * ks * s_ * s_;

        double ldS2 = 6.0 * kl * kl * l_;
        double mdS2 = 6.0 * km * km * m_;
        double sdS2 = 6.0 * ks * ks * s_;

        double f = wl * l + wm * m + ws * s;
        double f1 = wl * ldS + wm * mdS + ws * sdS;
        double f2 = wl * ldS2 + wm * mdS2 + ws * sdS2;

        S = S - f * f1 / (f1 * f1 - 0.5 * f * f2);

        return S;
    }

    private static double[] oklabToLinearSrgb(double L, double a, double b) {
        double l_ = L + 0.3963377774 * a + 0.2158037573 * b;
        double m_ = L - 0.1055613458 * a - 0.0638541728 * b;
        double s_ = L - 0.0894841775 * a - 1.2914855480 * b;

        double l = l_ * l_ * l_;
        double m = m_ * m_ * m_;
        double s = s_ * s_ * s_;

        return new double[] {
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
        };
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Oklab.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double H = raw[0];
        double S = raw[1];
        double L = raw[2];

        if (L <= 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        if (L >= 1.0) {
            return new double[] {1.0, 0.0, 0.0};
        }

        double a_ = Math.cos(Math.toRadians(H));
        double b_ = Math.sin(Math.toRadians(H));

        double l = toeInv(L);
        double[] st = computeSt(a_, b_);
        double sMax = st[0];
        double tMax = st[1];
        double s0 = 0.5;
        double k = 1.0 - s0 / sMax;

        double lv = 1.0 - S * s0 / (s0 + tMax - tMax * k * S);
        double cv = S * tMax * s0 / (s0 + tMax - tMax * k * S);

        double okL = l * lv;
        double C = l * cv;

        return new double[] {okL, C * a_, C * b_};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double okL = parentRaw[0];
        double a = parentRaw[1];
        double b = parentRaw[2];

        double C = Math.sqrt(a * a + b * b);
        double H = Math.toDegrees(Math.atan2(b, a));

        if (H < 0.0) {
            H += 360.0;
        }

        if (okL <= 0.0) {
            return new double[] {H, 0.0, 0.0};
        }

        if (okL >= 1.0) {
            return new double[] {H, 0.0, 1.0};
        }

        double a_ = (C == 0.0) ? 1.0 : a / C;
        double b_ = (C == 0.0) ? 0.0 : b / C;

        double[] st = computeSt(a_, b_);
        double sMax = st[0];
        double tMax = st[1];
        double s0 = 0.5;
        double k = 1.0 - s0 / sMax;

        double t = tMax / (C + okL * tMax - C * k * tMax);
        double S = (s0 + tMax) * (t * C) / (tMax * s0 + tMax * k * (t * C));
        double L = toe(okL);

        return new double[] {H, S, L};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            ColorSpace.wrap(raw[0], MAXS[0]) / MAXS[0],
            ColorSpace.clamp(raw[1] / MAXS[1], 0.0, 1.0),
            ColorSpace.clamp(raw[2] / MAXS[2], 0.0, 1.0)
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