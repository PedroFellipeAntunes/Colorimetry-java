package colorimetry.spaces.hue;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.rgb.Bt709RgbLinear;

/**
 * HSI (Hue, Saturation, Intensity) color space descriptor.
 *
 * Source: R. Gonzalez and R. Woods, "Digital Image Processing",
 *         Addison-Wesley, 1992.
 */
public final class Hsi implements ColorSpace {
    public static final Hsi INSTANCE = new Hsi(Bt709RgbLinear.INSTANCE);

    private final RgbLike parent;

    // ===== METADATA =====

    private static final String[] NAMES = {"Hue", "Saturation", "Intensity"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {0.0, 100.0, 100.0};

    private Hsi(RgbLike parent) {
        this.parent = parent;
    }

    /**
     * Creates an instance whose parent is the given linear RGB space.
     *
     * @param parent the linear RGB space this HSI derives from
     * @return a new HSI descriptor parented to the given RGB
     */
    public static Hsi of(RgbLike parent) {
        return new Hsi(parent);
    }

    @Override
    public String displayName() {
        if (parent == Bt709RgbLinear.INSTANCE) {
            return "HSI";
        }
        
        return "HSI (" + parent.displayName() + ")";
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

    // Sector boundaries for the trig-based HSI model (3 sectors of 120° each)
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double TWO_PI_THIRDS = TWO_PI / 3.0;    // 120°
    private static final double FOUR_PI_THIRDS = 4.0 * Math.PI / 3.0; // 240°
    private static final double PI_THIRDS = Math.PI / 3.0;       // 60°

    /**
     * Converts HSI to linear RGB [0,1] using the Gonzalez-Woods cosine formula.
     * Unlike HSB/HSL sector arithmetic, HSI uses cos(h)/cos(60°-h) ratios
     * within three 120° sectors, then derives the third channel from I = (R+G+B)/3.
     *
     * @param hue hue in degrees [0, 360)
     * @param saturation saturation percentage [0, 100]
     * @param intensity intensity percentage [0, 100]
     * @return linear RGB in [0, 1]
     */
    private static double[] hsiToRgb(double hue, double saturation, double intensity) {
        double s = saturation / 100.0;
        double i = intensity / 100.0;
        
        // Achromatic: no hue contribution
        if (s == 0.0) {
            return new double[] {i, i, i};
        }
        
        double hRad = ColorSpace.wrap(hue, 360.0) * TWO_PI / 360.0;
        double red;
        double green;
        double blue;
        
        // Each 120° sector computes the dominant channel via cosine ratio,
        // the minimum channel via (1-s), and the third from the intensity constraint
        if (hRad < TWO_PI_THIRDS) {
            // Red-dominant sector (0°–120°)
            blue = i * (1.0 - s);
            red = i * (1.0 + s * Math.cos(hRad) / Math.cos(PI_THIRDS - hRad));
            green = 3.0 * i - red - blue;
        } else if (hRad < FOUR_PI_THIRDS) {
            // Green-dominant sector (120°–240°)
            hRad -= TWO_PI_THIRDS;
            red = i * (1.0 - s);
            green = i * (1.0 + s * Math.cos(hRad) / Math.cos(PI_THIRDS - hRad));
            blue = 3.0 * i - red - green;
        } else {
            // Blue-dominant sector (240°–360°)
            hRad -= FOUR_PI_THIRDS;
            green = i * (1.0 - s);
            blue = i * (1.0 + s * Math.cos(hRad) / Math.cos(PI_THIRDS - hRad));
            red = 3.0 * i - green - blue;
        }
        
        return new double[] {
            ColorSpace.clamp(red, 0.0, 1.0),
            ColorSpace.clamp(green, 0.0, 1.0),
            ColorSpace.clamp(blue, 0.0, 1.0)
        };
    }

    /**
     * Converts linear RGB [0,1] to HSI.
     * Hue is derived from the angle of the (R-G, R-B) projection via arccos.
     *
     * @param red red channel in [0, 1]
     * @param green green channel in [0, 1]
     * @param blue blue channel in [0, 1]
     * @return HSI as [hue 0-360, saturation 0-100, intensity 0-100]
     */
    private static double[] rgbToHsi(double red, double green, double blue) {
        // Intensity = arithmetic mean of the three channels
        double intensity = (red + green + blue) / 3.0;
        
        if (intensity == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }
        
        // Saturation = 1 - (darkest channel / intensity)
        double min = Math.min(red, Math.min(green, blue));
        double saturation = 1.0 - min / intensity;

        // Hue via arccos of the projection of (R-G, R-B) onto the chroma axis
        double numerator = 0.5 * ((red - green) + (red - blue));
        double denominator = Math.sqrt((red - green) * (red - green) + (red - blue) * (green - blue));
        double hue;
        
        if (denominator == 0.0) {
            hue = 0.0;
        } else {
            // Clamp to [-1,1] to guard against floating-point drift in acos
            hue = Math.acos(ColorSpace.clamp(numerator / denominator, -1.0, 1.0));
            
            // arccos gives [0, π]; mirror to [π, 2π] when blue > green
            if (blue > green) {
                hue = TWO_PI - hue;
            }
            
            hue = hue / TWO_PI;
        }
        
        return new double[] {hue * 360.0, saturation * 100.0, intensity * 100.0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return parent;
    }

    @Override
    public double[] toParent(double[] raw) {
        // HSI → linear RGB [0,1] → Rgb raw [0,255]
        double[] rgb = hsiToRgb(raw[0], raw[1], raw[2]);
        
        return new double[] {rgb[0] * 255.0, rgb[1] * 255.0, rgb[2] * 255.0};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear RGB [0,1] → HSI
        return rgbToHsi(parentRaw[0] / 255.0, parentRaw[1] / 255.0, parentRaw[2] / 255.0);
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