package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * HSL (Hue, Saturation, Lightness) color space descriptor.
 *
 * Source: A. R. Smith, "Color Gamut Transform Pairs", SIGGRAPH 1978.
 *         G. H. Joblove and D. Greenberg, "Color Spaces for Computer
 *         Graphics", SIGGRAPH 1978.
 */
public final class Hsl implements ColorSpace {
    public static final Hsl INSTANCE = new Hsl();

    // ===== METADATA =====

    private static final String[] NAMES = {"Hue", "Saturation", "Lightness"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 100.0, 50.0};

    private Hsl() {}

    @Override
    public String displayName() {
        return "HSL";
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
     * Converts HSL to linear RGB [0,1].
     *
     * @param hue hue in degrees [0, 360)
     * @param saturation saturation percentage [0, 100]
     * @param lightness lightness percentage [0, 100]
     * @return linear RGB in [0, 1]
     */
    private static double[] hslToRgb(double hue, double saturation, double lightness) {
        double s = saturation / 100.0;
        double l = lightness / 100.0;
        
        // Achromatic: no hue contribution
        if (s == 0.0) {
            return new double[] {l, l, l};
        }
        
        double h = ((hue % 360.0) + 360.0) % 360.0 / 360.0;
        // upper/lower define the channel value range; formula changes at L=0.5
        // because saturation stretches symmetrically around the lightness midpoint
        double upper = l < 0.5 ? l * (1.0 + s) : l + s - l * s;
        double lower = 2.0 * l - upper;
        
        // Each channel is offset by 1/3 of the hue circle (120°)
        return new double[] {
            interpolateChannel(lower, upper, h + 1.0 / 3.0),
            interpolateChannel(lower, upper, h),
            interpolateChannel(lower, upper, h - 1.0 / 3.0)
        };
    }

    /**
     * Piecewise linear interpolation for one RGB channel given its hue offset.
     * The hue circle is split into 4 zones: rising, plateau, falling, floor.
     *
     * @param lower minimum channel value (floor)
     * @param upper maximum channel value (plateau)
     * @param hueOffset hue position for this channel in [0, 1], wrapped
     * @return interpolated channel value in [lower, upper]
     */
    private static double interpolateChannel(double lower, double upper, double hueOffset) {
        // Wrap hue offset into [0, 1)
        if (hueOffset < 0.0) {
            hueOffset += 1.0;
        }
        
        if (hueOffset > 1.0) {
            hueOffset -= 1.0;
        }
        
        if (hueOffset < 1.0 / 6.0) {
            return lower + (upper - lower) * 6.0 * hueOffset; // rising
        }
        
        if (hueOffset < 0.5) {
            return upper; // plateau
        }
        
        if (hueOffset < 2.0 / 3.0) {
            return lower + (upper - lower) * (2.0 / 3.0 - hueOffset) * 6.0; // falling
        }
        
        return lower; // floor
    }

    /**
     * Converts linear RGB [0,1] to HSL.
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return HSL as [hue 0-360, saturation 0-100, lightness 0-100]
     */
    private static double[] rgbToHsl(double red, double green, double blue) {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        
        // Lightness = midpoint between brightest and darkest channel
        double lightness = (max + min) / 2.0;
        double delta = max - min;
        double saturation;
        double hue;
        
        if (delta == 0.0) {
            hue = 0.0;
            saturation = 0.0;
        } else {
            // Saturation formula switches at L=0.5 to stay symmetric around midpoint
            saturation = lightness > 0.5 ? delta / (2.0 - max - min) : delta / (max + min);
            
            // Hue: dominant channel determines the 60° sector offset
            if (max == red) {
                hue = ((green - blue) / delta + (green < blue ? 6.0 : 0.0)) / 6.0;
            } else if (max == green) {
                hue = ((blue - red) / delta + 2.0) / 6.0;
            } else {
                hue = ((red - green) / delta + 4.0) / 6.0;
            }
        }
        
        return new double[] {hue * 360.0, saturation * 100.0, lightness * 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Rgb.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // HSL → linear RGB [0,1] → Rgb raw [0,255]
        double[] rgb = hslToRgb(raw[0], raw[1], raw[2]);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear RGB [0,1] → HSL
        return rgbToHsl(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
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