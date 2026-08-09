package colorimetry.spaces.perceptual;

import colorimetry.ColorSpace;
import colorimetry.types.LabLike;
import colorimetry.spaces.cie.CieLab;

/**
 * MSH color space descriptor. Spherical coordinates of a Lab space.
 *
 * Source: Moreland, "Diverging Color Maps for Scientific Visualization",
 *         ISVC 2009. DOI: 10.1007/978-3-642-10520-3_9.
 *
 * M (magnitude) is the distance from the origin in Lab space,
 * s (saturation angle) is the inclination from the L axis,
 * h (hue) is the azimuth in the ab plane.
 */
public final class Msh implements ColorSpace {
    public static final Msh INSTANCE = new Msh(CieLab.INSTANCE);

    private final LabLike parent;

    private static final String[] NAMES = {"Magnitude", "Saturation", "Hue"};
    private static final double[] MINS = {0.0, 0.0, 0.0};
    private static final double[] MAXS = {180.0, 180.0, 360.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    private Msh(LabLike parent) {
        this.parent = parent;
    }

    /**
     * Creates an instance whose parent is the given Lab space.
     *
     * @param parent the Lab space this MSH derives from
     * @return a new MSH descriptor parented to the given Lab
     */
    public static Msh of(LabLike parent) {
        return new Msh(parent);
    }

    @Override
    public String displayName() {
        if (parent == CieLab.INSTANCE) {
            return "MSH";
        }

        return "MSH (" + parent.displayName() + ")";
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
        return i == 2 ? 1.0 : 0.1;
    }
    @Override
    public boolean isChannelBounded(int i) {
        return i == 2;
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
        return parent;
    }

    @Override
    public double[] toParent(double[] raw) {
        double M = raw[0];
        double s = Math.toRadians(raw[1]);
        double h = Math.toRadians(ColorSpace.wrap(raw[2], 360.0));

        double L = M * Math.cos(s);
        double a = M * Math.sin(s) * Math.cos(h);
        double b = M * Math.sin(s) * Math.sin(h);

        return new double[] {L, a, b};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double L = parentRaw[0];
        double a = parentRaw[1];
        double b = parentRaw[2];

        double M = Math.sqrt(L * L + a * a + b * b);
        double s = (M == 0.0) ? 0.0 : Math.toDegrees(Math.acos(ColorSpace.clamp(L / M, -1.0, 1.0)));
        double h = Math.toDegrees(Math.atan2(b, a));

        if (h < 0.0) {
            h += 360.0;
        }

        return new double[] {M, s, h};
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
        return 0;
    }

    @Override
    public Class<? extends ColorSpace> acceptedParentType() {
        return LabLike.class;
    }
}