package colorimetry.spaces.perceptual;

import colorimetry.ColorSpace;
import colorimetry.types.LabLike;

/**
 * JzCzHz color space descriptor. Polar (cylindrical) form of JzAzBz.
 *
 * Source: Safdar, Cui, Kim and Luo (2017). Jz is lightness, Cz is chroma (distance
 *         from neutral axis), Hz is hue angle in degrees.
 */
public final class JzCzHz implements ColorSpace {
    public static final JzCzHz INSTANCE = new JzCzHz();

    private static final String[] NAMES = {"Jz", "Chroma", "Hue"};
    private static final double[] MINS = {0.0, 0.0, 0.0};
    private static final double[] MAXS = {0.25, 0.22, 360.0};
    private static final double[] DEFAULTS = {0.11, 0.0, 0.0};

    private JzCzHz() {}

    @Override
    public String displayName() {
        return "JzCzHz";
    }

    @Override
    public int componentCount() {
        return NAMES.length;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
    }

    @Override
    public double componentMin(int i) {
        return MINS[i];
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
        return i == 2 ? 1.0 : 0.001;
    }

    @Override
    public boolean isChannelBounded(int i) {
        // Jz and Hz are bounded; Cz is not
        return i == 0 || i == 2;
    }

    @Override
    public boolean hasPalette() {
        return true;
    }

    @Override
    public int[] paletteChannels() {
        return new int[] {2, 0};
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return JzAzBz.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // JzCzHz → JzAzBz: polar to cartesian
        double hRad = Math.toRadians(ColorSpace.wrap(raw[2], 360.0));
        
        return new double[] {
            raw[0],
            raw[1] * Math.cos(hRad),
            raw[1] * Math.sin(hRad)
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // JzAzBz → JzCzHz: cartesian to polar
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
            raw[0] / MAXS[0],
            raw[1] / MAXS[1],
            ColorSpace.wrap(raw[2], MAXS[2]) / MAXS[2]
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
        return 2;
    }

    @Override
    public int radialChannel() {
        return 1;
    }

    @Override
    public Class<? extends ColorSpace> acceptedParentType() {
        return LabLike.class;
    }
}