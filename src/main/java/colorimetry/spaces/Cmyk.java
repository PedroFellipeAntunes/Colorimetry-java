package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * CMYK color space descriptor.
 *
 * Source: Standard subtractive color model used in printing.
 *         Naive CMY-from-RGB conversion with Under Color Removal.
 */
public final class Cmyk implements ColorSpace {
    public static final Cmyk INSTANCE = new Cmyk();

    // ===== METADATA =====

    private static final String[] NAMES = {"Cyan", "Magenta", "Yellow", "Key"};

    private Cmyk() {}

    @Override
    public String displayName() {
        return "CMYK";
    }
    
    @Override
    public int componentCount() {
        return NAMES.length;
    }
    
    @Override
    public double componentMin(int i)  {
        return 0.0;
    }
    
    @Override
    public double componentMax(int i) {
        return 100.0;
    }
    
    @Override
    public double componentDefault(int i) {
        return 0.0;
    }
    
    @Override
    public boolean isBounded() {
        return true;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
    }

    // ===== MATH =====

    /**
     * Converts CMYK (0-100 each) to linear RGB [0,1].
     * Each channel is (1 - ink) scaled by (1 - key).
     *
     * @param cyan cyan percentage
     * @param magenta magenta percentage
     * @param yellow yellow percentage
     * @param key black percentage
     * @return linear RGB in [0, 1]
     */
    private static double[] cmykToRgb(double cyan, double magenta, double yellow, double key) {
        double c = cyan / 100.0;
        double m = magenta / 100.0;
        double y = yellow / 100.0;
        double k = key / 100.0;
        
        return new double[] {
            (1.0 - c) * (1.0 - k),
            (1.0 - m) * (1.0 - k),
            (1.0 - y) * (1.0 - k)
        };
    }

    /**
     * Converts linear RGB [0,1] to CMYK (0-100 each) using Under Color Removal.
     * Key (black) is extracted as 1 - max(R,G,B), then CMY are normalized
     * relative to the remaining ink range.
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return CMYK in [0, 100] each
     */
    private static double[] rgbToCmyk(double red, double green, double blue) {
        // Key = amount of black: 1 minus the brightest channel
        double key = 1.0 - Math.max(red, Math.max(green, blue));
        
        // Pure black: no CMY needed
        if (key >= 1.0) {
            return new double[] {0.0, 0.0, 0.0, 100.0};
        }
        
        // Normalize CMY by the non-black range to avoid division by zero
        double divisor = 1.0 - key;
        
        return new double[] {
            (1.0 - red - key) / divisor * 100.0,
            (1.0 - green - key) / divisor * 100.0,
            (1.0 - blue - key) / divisor * 100.0,
            key * 100.0
        };
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Rgb.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // CMYK [0-100] → linear RGB [0,1] → Rgb raw [0,255]
        double[] rgb = cmykToRgb(raw[0], raw[1], raw[2], raw[3]);
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear RGB [0,1] → CMYK [0-100]
        return rgbToCmyk(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / 100.0,
            raw[1] / 100.0,
            raw[2] / 100.0,
            raw[3] / 100.0
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * 100.0,
            normalized[1] * 100.0,
            normalized[2] * 100.0,
            normalized[3] * 100.0
        };
    }
}