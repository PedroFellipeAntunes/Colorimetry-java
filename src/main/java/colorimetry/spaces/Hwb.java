package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * HWB (Hue, Whiteness, Blackness) color space descriptor.
 *
 * Source: A. R. Smith, "HWB - A More Intuitive Hue-Based Color Model",
 *         Journal of Graphics Tools, Vol. 1, No. 1, 1996.
 */
public final class Hwb implements ColorSpace {
    public static final Hwb INSTANCE = new Hwb();

    // ===== METADATA =====

    private static final String[] NAMES = {"Hue", "Whiteness", "Blackness"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 0.0, 0.0};

    private Hwb() {}

    @Override
    public String displayName() {
        return "HWB";
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
     * Converts HWB to linear RGB [0,1].
     * Derives HSB saturation and brightness from whiteness/blackness,
     * then applies standard sector logic.
     *
     * @param hue hue in degrees [0, 360)
     * @param whiteness whiteness percentage [0, 100]
     * @param blackness blackness percentage [0, 100]
     * @return linear RGB in [0, 1]
     */
    private static double[] hwbToRgb(double hue, double whiteness, double blackness) {
        double w = whiteness / 100.0;
        double k = blackness / 100.0;
        double sum = w + k;
        
        // When W + B >= 1 the color is fully desaturated; normalize to a gray
        if (sum >= 1.0) {
            double gray = w / sum;
            
            return new double[] {gray, gray, gray};
        }
        
        // Recover HSB brightness and saturation from whiteness/blackness
        double brightness = 1.0 - k;
        double saturation = brightness > 0.0 ? 1.0 - w / brightness : 0.0;
        double h = ((hue % 360.0) + 360.0) % 360.0 / 360.0;
        
        // Standard HSB sector logic from here
        int sector = (int) (h * 6.0);
        double fraction = h * 6.0 - sector;
        double low = brightness * (1.0 - saturation);
        double falling = brightness * (1.0 - fraction * saturation);
        double rising = brightness * (1.0 - (1.0 - fraction) * saturation);
        
        return switch (sector % 6) {
            case 0 -> new double[] {brightness, rising, low};
            case 1 -> new double[] {falling, brightness, low};
            case 2 -> new double[] {low, brightness, rising};
            case 3 -> new double[] {low, falling, brightness};
            case 4 -> new double[] {rising, low, brightness};
            default -> new double[] {brightness, low, falling};
        };
    }

    /**
     * Converts linear RGB [0,1] to HWB.
     * Whiteness = min channel, blackness = 1 - max channel.
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return HWB as [hue 0-360, whiteness 0-100, blackness 0-100]
     */
    private static double[] rgbToHwb(double red, double green, double blue) {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double delta = max - min;
        
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
        
        // Whiteness = min channel, blackness = 1 - max channel
        return new double[] {hue * 360.0, min * 100.0, (1.0 - max) * 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Rgb.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // HWB → linear RGB [0,1] → Rgb raw [0,255]
        double[] rgb = hwbToRgb(raw[0], raw[1], raw[2]);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear RGB [0,1] → HWB
        return rgbToHwb(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
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