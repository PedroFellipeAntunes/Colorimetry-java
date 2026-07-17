package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * HSP (Hue, Saturation, Perceived brightness) color space descriptor.
 *
 * Source: D. R. Finley, "HSP Color Model - Alternative to HSV (HSB) and HSL",
 *         http://alienryderflex.com/hsp.html
 */
public final class Hsp implements ColorSpace {
    public static final Hsp INSTANCE = new Hsp();

    // ===== METADATA =====

    private static final String[] NAMES = {"Hue", "Saturation", "Perceived Brightness"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 100.0, 100.0};

    // BT.601 luma coefficients, used to weight P = sqrt(Wr*R² + Wg*G² + Wb*B²)
    private static final double WEIGHT_RED = 0.299;
    private static final double WEIGHT_GREEN = 0.587;
    private static final double WEIGHT_BLUE = 0.114;

    private Hsp() {}

    @Override
    public String displayName() {
        return "HSP";
    }
    
    @Override
    public int componentCount() {
        return NAMES.length;
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
        return DEFAULTS[i];
    }
    
    @Override
    public boolean isBounded() {
        return true;
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
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
    }

    // ===== MATH =====

    /**
     * Solves for the maximum channel value given perceived brightness, saturation,
     * hue fraction, and per-channel luma weights.
     *
     * P^2 = wMax*max^2 + wMid*(max*(1-s*(1-t)))^2 + wMin*(max*(1-s))^2
     * max = P / sqrt(wMax + wMid*(1-s*(1-t))^2 + wMin*(1-s)^2)
     *
     * @param perceived perceived brightness in [0, 1]
     * @param saturation saturation in [0, 1]
     * @param hueFraction position within the current 60° sector [0, 1]
     * @param weightMax luma weight of the dominant channel
     * @param weightMid luma weight of the transitioning channel
     * @param weightMin luma weight of the minimum channel
     * @return maximum channel value
     */
    private static double solveMaxChannel(double perceived, double saturation, double hueFraction, double weightMax, double weightMid, double weightMin) {
        double mid = 1.0 - saturation * (1.0 - hueFraction);
        double min = 1.0 - saturation;
        double denominator = weightMax + weightMid * mid * mid + weightMin * min * min;
        
        if (denominator <= 0.0) {
            return 0.0;
        }
        
        return perceived / Math.sqrt(denominator);
    }

    /**
     * Converts HSP to linear RGB [0,1].
     * Each 60° sector solves for the max channel that satisfies the perceived
     * brightness equation, then derives mid and min channels from saturation.
     *
     * @param hue hue in degrees [0, 360)
     * @param saturation saturation percentage [0, 100]
     * @param perceived perceived brightness percentage [0, 100]
     * @return linear RGB in [0, 1]
     */
    private static double[] hspToRgb(double hue, double saturation, double perceived) {
        double s = saturation / 100.0;
        double p = perceived / 100.0;
        
        if (p == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }
        
        // Achromatic: all channels equal to perceived brightness
        if (s == 0.0) {
            return new double[] {p, p, p};
        }
        
        double h = ((hue % 360.0) + 360.0) % 360.0 / 360.0;
        double segment = h * 6.0;
        int sector = (int) segment;
        double fraction = segment - sector;
        
        double red;
        double green;
        double blue;
        
        // Each sector rotates which channel is max/mid/min and which luma weights apply.
        // solveMaxChannel finds the max value that satisfies P² = Σ(wi * ci²).
        // mid = max*(1-s*(1-t)), min = max*(1-s) where t is the hue fraction.
        switch (sector % 6) {
            case 0 -> {
                double max = solveMaxChannel(p, s, fraction, WEIGHT_RED, WEIGHT_GREEN, WEIGHT_BLUE);
                red = max;
                green = max * (1.0 - s * (1.0 - fraction));
                blue = max * (1.0 - s);
            }
            case 1 -> {
                double max = solveMaxChannel(p, s, 1.0 - fraction, WEIGHT_GREEN, WEIGHT_RED, WEIGHT_BLUE);
                green = max;
                red = max * (1.0 - s * fraction);
                blue = max * (1.0 - s);
            }
            case 2 -> {
                double max = solveMaxChannel(p, s, fraction, WEIGHT_GREEN, WEIGHT_BLUE, WEIGHT_RED);
                green = max;
                blue = max * (1.0 - s * (1.0 - fraction));
                red = max * (1.0 - s);
            }
            case 3 -> {
                double max = solveMaxChannel(p, s, 1.0 - fraction, WEIGHT_BLUE, WEIGHT_GREEN, WEIGHT_RED);
                blue = max;
                green = max * (1.0 - s * fraction);
                red = max * (1.0 - s);
            }
            case 4 -> {
                double max = solveMaxChannel(p, s, fraction, WEIGHT_BLUE, WEIGHT_RED, WEIGHT_GREEN);
                blue = max;
                red = max * (1.0 - s * (1.0 - fraction));
                green = max * (1.0 - s);
            }
            default -> {
                double max = solveMaxChannel(p, s, 1.0 - fraction, WEIGHT_RED, WEIGHT_BLUE, WEIGHT_GREEN);
                red = max;
                blue = max * (1.0 - s * fraction);
                green = max * (1.0 - s);
            }
        }
        
        return new double[] {
            Math.max(0.0, Math.min(1.0, red)),
            Math.max(0.0, Math.min(1.0, green)),
            Math.max(0.0, Math.min(1.0, blue))
        };
    }

    /**
     * Converts linear RGB [0,1] to HSP.
     * Hue and saturation are computed identically to HSB.
     * Perceived brightness uses the weighted root-mean-square: P = sqrt(Σ wi*ci²).
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return HSP as [hue 0-360, saturation 0-100, perceived 0-100]
     */
    private static double[] rgbToHsp(double red, double green, double blue) {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double delta = max - min;
        // Saturation identical to HSB: chroma relative to max channel
        double saturation = max == 0.0 ? 0.0 : delta / max;
        
        // Perceived brightness: weighted RMS instead of simple max
        double perceived = Math.sqrt(WEIGHT_RED * red * red + WEIGHT_GREEN * green * green + WEIGHT_BLUE * blue * blue);
        
        // Hue extraction identical to HSB
        double hue;
        
        if (delta == 0.0) {
            hue = 0.0;
        } else if (max == red) {
            hue = ((green - blue) / delta + (green < blue ? 6.0 : 0.0)) / 6.0;
        } else if (max == green) {
            hue = ((blue - red) / delta + 2.0) / 6.0;
        } else {
            hue = ((red - green) / delta + 4.0) / 6.0;
        }
        
        return new double[] {hue * 360.0, saturation * 100.0, perceived * 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Rgb.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // HSP → linear RGB [0,1] → Rgb raw [0,255]
        double[] rgb = hspToRgb(raw[0], raw[1], raw[2]);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear RGB [0,1] → HSP
        return rgbToHsp(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            ((raw[0] % 360.0) + 360.0) % 360.0 / 360.0,
            Math.max(0.0, Math.min(1.0, raw[1] / 100.0)),
            Math.max(0.0, Math.min(1.0, raw[2] / 100.0))
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