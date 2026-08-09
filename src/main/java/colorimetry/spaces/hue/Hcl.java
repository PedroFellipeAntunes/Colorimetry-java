package colorimetry.spaces.hue;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.rgb.Bt709RgbLinear;

/**
 * HCL (Hue, Chroma, Lightness) color space descriptor.
 *
 * Source: Derived from HSL (Joblove and Greenberg, SIGGRAPH 1978) by exposing
 *         the raw chroma instead of the relative saturation.
 */
public final class Hcl implements ColorSpace {
    public static final Hcl INSTANCE = new Hcl(Bt709RgbLinear.INSTANCE);

    private final RgbLike parent;

    // ===== METADATA =====

    private static final String[] NAMES = {"Hue", "Chroma", "Lightness"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 100.0, 50.0};

    private Hcl(RgbLike parent) {
        this.parent = parent;
    }

    /**
     * Creates an instance whose parent is the given linear RGB space.
     *
     * @param parent the linear RGB space this HCL derives from
     * @return a new HCL descriptor parented to the given RGB
     */
    public static Hcl of(RgbLike parent) {
        return new Hcl(parent);
    }

    @Override
    public String displayName() {
        if (parent == Bt709RgbLinear.INSTANCE) {
            return "HCL";
        }
        
        return "HCL (" + parent.displayName() + ")";
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
     * Converts HCL to linear RGB [0,1].
     * From chroma and lightness: upper = L + C/2, lower = L - C/2,
     * then piecewise hue interpolation identical to HSL.
     *
     * @param hue hue in degrees [0, 360)
     * @param chroma raw chroma percentage [0, 100]
     * @param lightness lightness percentage [0, 100]
     * @return linear RGB in [0, 1]
     */
    private static double[] hclToRgb(double hue, double chroma, double lightness) {
        double c = chroma / 100.0;
        double l = lightness / 100.0;

        if (c == 0.0) {
            return new double[] {l, l, l};
        }

        double upper = ColorSpace.clamp(l + c / 2.0, 0.0, 1.0);
        double lower = ColorSpace.clamp(l - c / 2.0, 0.0, 1.0);
        double h = ColorSpace.wrap(hue, 360.0) / 360.0;

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
        if (hueOffset < 0.0) {
            hueOffset += 1.0;
        }
        
        if (hueOffset > 1.0) {
            hueOffset -= 1.0;
        }
        
        if (hueOffset < 1.0 / 6.0) {
            return lower + (upper - lower) * 6.0 * hueOffset;
        }
        
        if (hueOffset < 0.5) {
            return upper;
        }
        
        if (hueOffset < 2.0 / 3.0) {
            return lower + (upper - lower) * (2.0 / 3.0 - hueOffset) * 6.0;
        }
        
        return lower;
    }

    /**
     * Converts linear RGB [0,1] to HCL.
     * Unlike HSL which outputs relative saturation, HCL outputs
     * raw chroma (max - min) directly alongside lightness (max + min) / 2.
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return HCL as [hue 0-360, chroma 0-100, lightness 0-100]
     */
    private static double[] rgbToHcl(double red, double green, double blue) {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double chroma = max - min;
        double lightness = (max + min) / 2.0;
        double hue;

        if (chroma == 0.0) {
            hue = 0.0;
        } else if (max == red) {
            hue = ((green - blue) / chroma + (green < blue ? 6.0 : 0.0)) / 6.0;
        } else if (max == green) {
            hue = ((blue - red) / chroma + 2.0) / 6.0;
        } else {
            hue = ((red - green) / chroma + 4.0) / 6.0;
        }

        return new double[] {hue * 360.0, chroma * 100.0, lightness * 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return parent;
    }

    @Override
    public double[] toParent(double[] raw) {
        // HCL → RGB [0,1] → raw [0,255]
        double[] rgb = hclToRgb(raw[0], raw[1], raw[2]);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // RGB raw [0,255] → [0,1] → HCL
        return rgbToHcl(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
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

    @Override
    public Class<? extends ColorSpace> acceptedParentType() {
        return RgbLike.class;
    }
}