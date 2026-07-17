package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * HCB (Hue, Chroma, Brightness) color space descriptor.
 * Also known as HCV (Hue, Chroma, Value).
 *
 * Source: Derived from HSB/HSV (A. R. Smith, SIGGRAPH 1978) by exposing
 *         the raw chroma instead of the relative saturation.
 */
public final class Hcb implements ColorSpace {
    public static final Hcb INSTANCE = new Hcb();

    // ===== METADATA =====

    private static final String[] NAMES = {"Hue", "Chroma", "Brightness"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 100.0, 100.0};

    private Hcb() {}

    @Override
    public String displayName() {
        return "HCB";
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
     * Converts HCB to linear RGB [0,1].
     * Internally recovers saturation as chroma/brightness, then applies
     * standard HSB sector logic.
     *
     * @param hue hue in degrees [0, 360)
     * @param chroma raw chroma percentage [0, 100]
     * @param brightness brightness percentage [0, 100]
     * @return linear RGB in [0, 1]
     */
    private static double[] hcbToRgb(double hue, double chroma, double brightness) {
        double v = brightness / 100.0;
        
        if (v == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }
        
        // Recover relative saturation from raw chroma; clamp to 1.0 since C can't exceed V
        double c = chroma / 100.0;
        double s = Math.min(1.0, c / v);
        double h = ((hue % 360.0) + 360.0) % 360.0 / 360.0;
        
        // Standard HSB sector logic from here
        int sector = (int) (h * 6.0);
        double fraction = h * 6.0 - sector;
        double low = v * (1.0 - s);
        double falling = v * (1.0 - fraction * s);
        double rising = v * (1.0 - (1.0 - fraction) * s);
        
        return switch (sector % 6) {
            case 0 -> new double[] {v, rising, low};
            case 1 -> new double[] {falling, v, low};
            case 2 -> new double[] {low, v, rising};
            case 3 -> new double[] {low, falling, v};
            case 4 -> new double[] {rising, low, v};
            default -> new double[] {v, low, falling};
        };
    }

    /**
     * Converts linear RGB [0,1] to HCB.
     * Unlike HSB which outputs relative saturation (chroma/max), HCB outputs
     * raw chroma (max - min) directly.
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return HCB as [hue 0-360, chroma 0-100, brightness 0-100]
     */
    private static double[] rgbToHcb(double red, double green, double blue) {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        // Raw chroma instead of relative saturation (the key difference from HSB)
        double chroma = max - min;
        double hue;
        
        // Hue extraction identical to HSB
        if (chroma == 0.0) {
            hue = 0.0;
        } else if (max == red) {
            hue = ((green - blue) / chroma + (green < blue ? 6.0 : 0.0)) / 6.0;
        } else if (max == green) {
            hue = ((blue - red) / chroma + 2.0) / 6.0;
        } else {
            hue = ((red - green) / chroma + 4.0) / 6.0;
        }
        
        return new double[] {hue * 360.0, chroma * 100.0, max * 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Rgb.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // HCB → linear RGB [0,1] → Rgb raw [0,255]
        double[] rgb = hcbToRgb(raw[0], raw[1], raw[2]);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear RGB [0,1] → HCB
        return rgbToHcb(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
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