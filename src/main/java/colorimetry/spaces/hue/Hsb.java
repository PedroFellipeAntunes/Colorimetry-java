package colorimetry.spaces.hue;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.rgb.Bt709RgbLinear;

/**
 * HSB (Hue, Saturation, Brightness) color space descriptor.
 * Also known as HSV (Hue, Saturation, Value).
 *
 * Source: A. R. Smith, "Color Gamut Transform Pairs", SIGGRAPH 1978.
 */
public final class Hsb implements ColorSpace {
    public static final Hsb INSTANCE = new Hsb(Bt709RgbLinear.INSTANCE);

    private final RgbLike parent;

    // ===== METADATA =====

    private static final String[] NAMES = {"Hue", "Saturation", "Brightness"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 100.0, 100.0};

    private Hsb(RgbLike parent) {
        this.parent = parent;
    }

    /**
     * Creates an HSB instance whose parent is the given linear RGB space.
     *
     * @param parent the linear RGB space this HSB derives from
     * @return a new HSB descriptor parented to the given RGB
     */
    public static Hsb of(RgbLike parent) {
        return new Hsb(parent);
    }

    @Override
    public String displayName() {
        if (parent == Bt709RgbLinear.INSTANCE) {
            return "HSB";
        }
        
        return "HSB (" + parent.displayName() + ")";
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
     * Converts HSB to linear RGB [0,1].
     *
     * @param hue hue in degrees [0, 360)
     * @param saturation saturation percentage [0, 100]
     * @param brightness brightness percentage [0, 100]
     * @return linear RGB in [0, 1]
     */
    private static double[] hsbToRgb(double hue, double saturation, double brightness) {
        double s = saturation / 100.0;
        double v = brightness / 100.0;
        
        // Achromatic: no hue contribution
        if (s == 0.0) {
            return new double[] {v, v, v};
        }
        
        // Wrap hue to [0,1) and split into 6 sectors of 60° each
        double h = ColorSpace.wrap(hue, 360.0) / 360.0;
        int sector = (int) (h * 6.0);
        double fraction = h * 6.0 - sector;
        // low = minimum channel, rising/falling = channels transitioning across the sector
        double low = v * (1.0 - s);
        double falling = v * (1.0 - fraction * s);
        double rising = v * (1.0 - (1.0 - fraction) * s);
        
        // Each sector rotates which channel is max, rising, or low
        switch (sector % 6) {
            case 0: return new double[] {v, rising, low};
            case 1: return new double[] {falling, v, low};
            case 2: return new double[] {low, v, rising};
            case 3: return new double[] {low, falling, v};
            case 4: return new double[] {rising, low, v};
            default: return new double[] {v, low, falling};
        }
    }

    /**
     * Converts linear RGB [0,1] to HSB.
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return HSB as [hue 0-360, saturation 0-100, brightness 0-100]
     */
    private static double[] rgbToHsb(double red, double green, double blue) {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double delta = max - min;
        
        // Brightness = max channel, saturation = chroma relative to brightness
        double brightness = max;
        double saturation = max == 0.0 ? 0.0 : delta / max;
        double hue;
        
        // Hue: which channel is dominant determines the 60° sector offset
        if (delta == 0.0) {
            hue = 0.0;
        } else if (max == red) {
            hue = ((green - blue) / delta + (green < blue ? 6.0 : 0.0)) / 6.0;
        } else if (max == green) {
            hue = ((blue - red) / delta + 2.0) / 6.0;
        } else {
            hue = ((red - green) / delta + 4.0) / 6.0;
        }
        
        return new double[] {hue * 360.0, saturation * 100.0, brightness * 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return parent;
    }

    @Override
    public double[] toParent(double[] raw) {
        // HSB → linear RGB [0,1] → Rgb raw [0,255]
        double[] rgb = hsbToRgb(raw[0], raw[1], raw[2]);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear RGB [0,1] → HSB
        return rgbToHsb(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
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