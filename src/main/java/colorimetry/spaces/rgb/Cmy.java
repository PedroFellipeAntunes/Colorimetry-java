package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;

/**
 * CMY (Cyan, Magenta, Yellow) color space descriptor.
 *
 * Subtractive complement of RGB: C = 100 - R%, M = 100 - G%, Y = 100 - B%.
 */
public final class Cmy implements ColorSpace {
    public static final Cmy INSTANCE = new Cmy(Bt709RgbLinear.INSTANCE);

    private final RgbLike parent;

    private static final String[] NAMES = {"Cyan", "Magenta", "Yellow"};
    private static final double[] MAXS = {100.0, 100.0, 100.0};

    private Cmy(RgbLike parent) {
        this.parent = parent;
    }

    /**
     * Creates an instance whose parent is the given linear RGB space.
     *
     * @param parent the linear RGB space this CMY derives from
     * @return a new CMY descriptor parented to the given RGB
     */
    public static Cmy of(RgbLike parent) {
        return new Cmy(parent);
    }

    @Override
    public String displayName() {
        if (parent == Bt709RgbLinear.INSTANCE) {
            return "CMY";
        }

        return "CMY (" + parent.displayName() + ")";
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
        return 0.0;
    }

    @Override
    public double componentMax(int i) {
        return MAXS[i];
    }

    @Override
    public double componentDefault(int i) {
        return 0.0;
    }
    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return parent;
    }

    @Override
    public double[] toParent(double[] raw) {
        // CMY [0,100] -> RGB [0,255]
        return new double[] {
            (1.0 - raw[0] / 100.0) * 255.0,
            (1.0 - raw[1] / 100.0) * 255.0,
            (1.0 - raw[2] / 100.0) * 255.0
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // RGB [0,255] -> CMY [0,100]
        return new double[] {
            (1.0 - parentRaw[0] / 255.0) * 100.0,
            (1.0 - parentRaw[1] / 255.0) * 100.0,
            (1.0 - parentRaw[2] / 255.0) * 100.0
        };
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / MAXS[0],
            raw[1] / MAXS[1],
            raw[2] / MAXS[2]
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
    public Class<? extends ColorSpace> acceptedParentType() {
        return RgbLike.class;
    }
}