package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * CIE LCh(ab) color space descriptor.
 *
 * Source: CIE Publication 15:2004, "Colorimetry".
 *         Cylindrical transform of CIE L*a*b* (1976).
 */
public final class CieLch implements ColorSpace {
    public static final CieLch INSTANCE = new CieLch();

    // ===== METADATA =====

    private static final double C_MAX = 150.0;

    private static final String[] NAMES = {"Lightness", "Chroma", "Hue"};
    private static final String[] SHORTS = {"L", "C", "H"};
    private static final double[] MAXS = {100.0, C_MAX, 360.0};
    private static final double[] DEFAULTS = {50.0, C_MAX * 0.3, 0.0};

    private CieLch() {}

    @Override
    public String displayName() {
        return "CIE LCh";
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
    public double componentStep(int i) {
        return 0.1;
    }
    
    @Override
    public boolean isBounded() {
        return false;
    }
    
    @Override
    public boolean hasPalette() {
        return true;
    }
    
    @Override
    public int[] paletteChannels() {
        return new int[] {2, 0};
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : SHORTS[i];
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return CieLab.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // LCh → Lab: polar to cartesian
        double hRad = Math.toRadians(((raw[2] % 360.0) + 360.0) % 360.0);
        
        return new double[] {raw[0], raw[1] * Math.cos(hRad), raw[1] * Math.sin(hRad)};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Lab → LCh: cartesian to polar
        double C = Math.sqrt(parentRaw[1] * parentRaw[1] + parentRaw[2] * parentRaw[2]);
        double H = Math.toDegrees(Math.atan2(parentRaw[2], parentRaw[1]));

        if (H < 0.0) {
            H += 360.0;
        }

        return new double[] {parentRaw[0], C, H};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / 100.0,
            raw[1] / C_MAX,
            ((raw[2] % 360.0) + 360.0) % 360.0 / 360.0
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * 100.0,
            normalized[1] * C_MAX,
            normalized[2] * 360.0
        };
    }
}