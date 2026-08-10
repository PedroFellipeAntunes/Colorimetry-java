package colorimetry.spaces.hue;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.rgb.Bt601RgbLinear;

/**
 * HCY (Hue, Chroma, Luma) color space descriptor.
 *
 * Source: K. Shapran, "HCY Color Space", 2009.
 *         BT.601 luma coefficients (Wr=0.299, Wg=0.587, Wb=0.114).
 */
public final class Hcy implements ColorSpace {
    public static final Hcy INSTANCE = new Hcy(Bt601RgbLinear.INSTANCE);

    private final RgbLike parent;

    // ===== METADATA =====

    private static final String[] NAMES = {"Hue", "Chroma", "Luma"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 100.0, 50.0};

    // Luma coefficients from parent RGB space
    private final double wr;
    private final double wg;
    private final double wb;

    private Hcy(RgbLike parent) {
        this.parent = parent;
        double[] luma = parent.lumaCoefficients();
        this.wr = luma[0];
        this.wg = luma[1];
        this.wb = luma[2];
    }

    /**
     * Creates an instance whose parent is the given linear RGB space.
     *
     * @param parent the linear RGB space this HCY derives from
     * @return a new HCY descriptor parented to the given RGB
     */
    public static Hcy of(RgbLike parent) {
        return new Hcy(parent);
    }

    @Override
    public String displayName() {
        if (parent == Bt601RgbLinear.INSTANCE) {
            return "HCY";
        }
        
        return "HCY (" + parent.displayName() + ")";
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
     * Converts HCY to linear RGB [0,1].
     * Builds a pure-chroma RGB triplet from hue, then shifts all channels
     * uniformly so the weighted luma matches the target Y.
     *
     * @param hue hue in degrees [0, 360)
     * @param chroma raw chroma percentage [0, 100]
     * @param luma luma percentage [0, 100]
     * @return linear RGB in [0, 1]
     */
    private double[] hcyToRgb(double hue, double chroma, double luma) {
        double c = chroma / 100.0;
        double y = luma / 100.0;
        double h = ColorSpace.wrap(hue, 360.0) / 360.0;
        
        double segment = h * 6.0;
        // Secondary = the non-dominant chromatic channel within the sector
        double secondary = c * (1.0 - Math.abs(segment % 2.0 - 1.0));
        
        double red;
        double green;
        double blue;
        
        // Build pure-chroma RGB (no lightness yet): dominant = c, secondary = x, third = 0
        int sector = (int) segment;
        
        switch (sector % 6) {
            case 0: {
                red = c;
                green = secondary;
                blue = 0;
                break;
            }
            case 1: {
                red = secondary;
                green = c;
                blue = 0;
                break;
            }
            case 2: {
                red = 0;
                green = c;
                blue = secondary;
                break;
            }
            case 3: {
                red = 0;
                green = secondary;
                blue = c;
                break;
            }
            case 4: {
                red = secondary;
                green = 0;
                blue = c;
                break;
            }
            default: {
                red = c;
                green = 0;
                blue = secondary;
                break;
            }
        }
        
        // Shift all channels so weighted luma equals target Y
        double offset = y - (wr * red + wg * green + wb * blue);
        
        return new double[] {
            ColorSpace.clamp(red + offset, 0.0, 1.0),
            ColorSpace.clamp(green + offset, 0.0, 1.0),
            ColorSpace.clamp(blue + offset, 0.0, 1.0)
        };
    }

    /**
     * Converts linear RGB [0,1] to HCY.
     * Chroma is raw (max - min), luma is BT.601 weighted sum.
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return HCY as [hue 0-360, chroma 0-100, luma 0-100]
     */
    private double[] rgbToHcy(double red, double green, double blue) {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double chroma = max - min;
        // Luma = BT.601 weighted sum (not max like HSB, not midpoint like HSL)
        double luma = wr * red + wg * green + wb * blue;
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
        
        return new double[] {hue * 360.0, chroma * 100.0, luma * 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return parent;
    }

    @Override
    public double[] toParent(double[] raw) {
        // HCY → linear RGB [0,1] → Rgb raw [0,255]
        double[] rgb = hcyToRgb(raw[0], raw[1], raw[2]);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear RGB [0,1] → HCY
        return rgbToHcy(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
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