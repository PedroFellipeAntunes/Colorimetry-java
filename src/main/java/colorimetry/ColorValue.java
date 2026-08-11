package colorimetry;

import colorimetry.bridge.*;
import colorimetry.engine.*;
import colorimetry.spaces.xyz.XyzD65;

/**
 * An immutable color value in a specific color space, stored as raw doubles
 * in the native units of that space. Every operation returns a new instance.
 *
 * Alpha is carried as a separate field, never used in color math.
 * Conversions preserve it unchanged.
 *
 * Creation:
 *   of(space, values...)               -- from raw values
 *   of(space, values, alpha)           -- from raw values with explicit alpha
 *   ofNormalized(space, values...)     -- from normalized values
 *   ofNormalized(space, values, alpha) -- from normalized values with explicit alpha
 *   fromHex(hex)                       -- from hex string, returns sRGB
 *   fromAWT(color)                     -- from java.awt.Color, returns sRGB
 *
 * Reading:
 *   get(i)            -- raw value for component i
 *   get()             -- all raw values
 *   getNormalized(i)  -- normalized value for component i
 *   getNormalized()   -- all normalized values
 *   alpha()           -- alpha value
 *   getSpace()        -- color space descriptor
 *
 * Conversion:
 *   to(target)          -- converts via parent hierarchy
 *   toGrayscale(method) -- achromatic equivalent, stays in same space
 *   toHex()             -- hex string via ColorHex
 *   toAWT()             -- java.awt.Color via ColorAwt
 *
 * Comparison:
 *   distance(other, metric) -- distance using a DistanceMetric
 *   lerp(other, t)          -- linear interpolation by factor t
 *
 * All factory methods are affected by {@link ValidationMode}.
 */
public final class ColorValue {
    // Basic metadata for alpha channel used in all colorspaces
    public static final String ALPHA_LABEL = "Alpha";
    public static final double ALPHA_MIN = 0.0;
    public static final double ALPHA_MAX = 1.0;

    private static volatile ValidationMode validationMode = ValidationMode.NONE;

    private final double[] raw;
    private final ColorSpace space;
    private final double alpha;

    /**
     * Internal constructor. Clones the array to preserve immutability.
     * Alpha is validated according to the current {@link ValidationMode}.
     *
     * @param raw channel values in native units
     * @param space color space descriptor
     * @param alpha opacity in [0.0, 1.0]
     */
    private ColorValue(double[] raw, ColorSpace space, double alpha) {
        this.raw = raw.clone();
        this.space = space;
        this.alpha = validateBounded(alpha, ALPHA_MIN, ALPHA_MAX, ALPHA_LABEL);
    }

    // ===== VALIDATION =====

    /**
     * Sets the global validation mode for all factory methods.
     *
     * @param mode validation mode to apply
     */
    public static void setValidationMode(ValidationMode mode) {
        validationMode = mode;
    }

    /**
     * Returns the current global validation mode.
     *
     * @return current validation mode
     */
    public static ValidationMode getValidationMode() {
        return validationMode;
    }

    /**
     * Validates a single value against a bounded range.
     * Affected by the current {@link ValidationMode}.
     *
     * @param value input value
     * @param min range minimum
     * @param max range maximum
     * @param label display label for error messages
     * @return validated value
     * @throws IllegalArgumentException in ERROR mode if outside [min, max]
     */
    private static double validateBounded(double value, double min, double max, String label) {
        if (validationMode == ValidationMode.NONE) {
            return value;
        }

        if (value < min || value > max) {
            if (validationMode == ValidationMode.ERROR) {
                throw new IllegalArgumentException(
                    label + ": " + value + " outside [" + min + ", " + max + "]"
                );
            }
        }

        return ColorSpace.clamp(value, min, max);
    }

    /**
     * Validates raw channel values against the space's bounds.
     * Only bounded channels ({@link ColorSpace#isChannelBounded(int)}) are checked.
     * Affected by the current {@link ValidationMode}.
     *
     * @param space color space descriptor
     * @param values raw channel values
     * @return validated values (cloned if modified)
     * @throws IllegalArgumentException in ERROR mode if a bounded channel
     *         is outside its range
     */
    private static double[] validate(ColorSpace space, double[] values) {
        if (validationMode == ValidationMode.NONE) {
            return values;
        }

        double[] result = values.clone();

        for (int i = 0; i < result.length; i++) {
            if (!space.isChannelBounded(i)) {
                continue;
            }

            result[i] = validateBounded(
                result[i],
                space.componentMin(i),
                space.componentMax(i),
                space.displayName() + " " + space.componentName(i, false)
            );
        }

        return result;
    }

    // ===== FACTORIES =====

    /**
     * Creates a ColorValue from raw values with alpha=1.0.
     * Affected by {@link ValidationMode}.
     *
     * @param space color space descriptor
     * @param values channel values in native units
     * @return new ColorValue
     */
    public static ColorValue of(ColorSpace space, double... values) {
        return new ColorValue(validate(space, values), space, ALPHA_MAX);
    }

    /**
     * Creates a ColorValue from raw values with explicit alpha.
     * Affected by {@link ValidationMode}.
     *
     * @param space color space descriptor
     * @param values channel values in native units
     * @param alpha opacity in [0.0, 1.0]
     * @return new ColorValue
     */
    public static ColorValue of(ColorSpace space, double[] values, double alpha) {
        return new ColorValue(validate(space, values), space, alpha);
    }

    /**
     * Creates a ColorValue from [0,1] normalized values with alpha=1.0.
     * Values are denormalized to native units via the space's denormalize().
     * Affected by {@link ValidationMode}.
     *
     * @param space color space descriptor
     * @param values normalized channel values in [0, 1]
     * @return new ColorValue
     */
    public static ColorValue ofNormalized(ColorSpace space, double... values) {
        return new ColorValue(validate(space, space.denormalize(values)), space, ALPHA_MAX);
    }

    /**
     * Creates a ColorValue from [0,1] normalized values with explicit alpha.
     * Values are denormalized to native units via the space's denormalize().
     * Affected by {@link ValidationMode}.
     *
     * @param space color space descriptor
     * @param values normalized channel values in [0, 1]
     * @param alpha opacity in [0.0, 1.0]
     * @return new ColorValue
     */
    public static ColorValue ofNormalized(ColorSpace space, double[] values, double alpha) {
        return new ColorValue(validate(space, space.denormalize(values)), space, alpha);
    }

    // ===== CONVERSION =====

    /**
     * Converts this color to a different color space.
     * Delegates to {@link ColorConverter} which uses the parent hierarchy
     * when possible, falling back to the XYZ hub otherwise.
     *
     * @param target destination color space
     * @return new ColorValue in the target space, alpha preserved
     */
    public ColorValue to(ColorSpace target) {
        if (space == target) {
            return new ColorValue(raw, space, alpha);
        }

        return new ColorValue(ColorConverter.convert(space, target, raw), target, alpha);
    }

    /**
     * Converts this color to its achromatic equivalent using the given grayscale
     * method. The result stays in the same color space as the original.
     *
     * @param method grayscale conversion strategy
     * @return new ColorValue with the gray equivalent, alpha preserved
     */
    public ColorValue toGrayscale(Grayscale method) {
        double[] xyz = ColorConverter.convert(space, XyzD65.INSTANCE, raw);
        double[] grayXyz = method.toGrayXyz(xyz);

        // Gray point may land outside the gamut of the original space
        if (space.isBounded() && !space.isInGamut(grayXyz)) {
            grayXyz = GamutMapper.map(grayXyz, space);
        }

        double[] result = ColorConverter.convert(XyzD65.INSTANCE, space, grayXyz);
        
        return new ColorValue(result, space, alpha);
    }

    // ===== COMPARISON =====

    /**
     * Computes the distance between this color and another using the given metric.
     * Affected by {@link ValidationMode}.
     *
     * @param other color to compare against
     * @param metric distance metric to use
     * @return non-negative distance
     */
    public double distance(ColorValue other, DistanceMetric metric) {
        double[] otherRaw = resolveOther(other);

        return metric.compute(raw, otherRaw);
    }

    /**
     * Linearly interpolates between this color and another by factor t.
     * t=0.0 returns this color, t=1.0 returns the other, t=0.5 returns the midpoint.
     * For cylindrical spaces, the hue channel is interpolated along the shortest
     * arc using the space's {@link ColorSpace#isCylindrical()} and
     * {@link ColorSpace#hueChannel()} metadata.
     * Affected by {@link ValidationMode}.
     *
     * @param other target color
     * @param t interpolation factor in [0.0, 1.0]
     * @return new ColorValue in this space, alpha interpolated
     */
    public ColorValue lerp(ColorValue other, double t) {
        double[] otherRaw = resolveOther(other);
        double[] result = new double[raw.length];

        int hueIdx = space.isCylindrical() ? space.hueChannel() : -1;

        for (int i = 0; i < raw.length; i++) {
            if (i == hueIdx) {
                double max = space.componentMax(i);
                result[i] = lerpHue(raw[i], otherRaw[i], t, max);
            } else {
                result[i] = raw[i] + (otherRaw[i] - raw[i]) * t;
            }
        }

        double lerpAlpha = alpha + (other.alpha - alpha) * t;

        return new ColorValue(result, space, lerpAlpha);
    }

    /**
     * Interpolates two hue values along the shortest arc of a circular channel.
     *
     * @param h1 start hue
     * @param h2 end hue
     * @param t interpolation factor
     * @param max hue period (e.g. 360.0)
     * @return interpolated hue in [0, max)
     */
    private static double lerpHue(double h1, double h2, double t, double max) {
        double half = max / 2.0;
        double diff = h2 - h1;

        if (diff > half) {
            diff -= max;
        } else if (diff < -half) {
            diff += max;
        }

        return ColorSpace.wrap(h1 + diff * t, max);
    }

    /**
     * Resolves the raw values of another color for comparison or interpolation.
     * If both colors share the same space, returns the other's raw values directly.
     * Otherwise, behavior depends on the current {@link ValidationMode}.
     *
     * @param other the other color
     * @return raw values in this color's space
     * @throws IllegalArgumentException in ERROR mode if spaces differ
     */
    private double[] resolveOther(ColorValue other) {
        if (other.space == space) {
            return other.raw;
        }

        if (validationMode == ValidationMode.ERROR) {
            throw new IllegalArgumentException(
                "Color space mismatch: "
                + space.displayName() + " and " + other.space.displayName()
            );
        }

        if (validationMode == ValidationMode.RESOLVE) {
            return ColorConverter.convert(other.space, space, other.raw);
        }

        // NONE: use raw values directly
        return other.raw;
    }

    // ===== ACCESSORS =====

    /**
     * Returns the raw value for a single channel, formatted by the space.
     *
     * @param i channel index
     * @return raw value in native units
     */
    public double get(int i) {
        return space.formatRaw(raw[i]);
    }

    /**
     * Returns a copy of all raw channel values, formatted by the space.
     *
     * @return raw values array
     */
    public double[] get() {
        double[] result = new double[raw.length];

        for (int i = 0; i < raw.length; i++) {
            result[i] = space.formatRaw(raw[i]);
        }

        return result;
    }

    /**
     * Returns the normalized [0,1] value for a single channel.
     *
     * @param i channel index
     * @return normalized value in [0, 1]
     */
    public double getNormalized(int i) {
        return space.normalize(raw)[i];
    }

    /**
     * Returns all channel values normalized to [0,1].
     *
     * @return normalized values array
     */
    public double[] getNormalized() {
        return space.normalize(raw);
    }

    /**
     * Returns the alpha (opacity) value.
     *
     * @return alpha in [0.0, 1.0]
     */
    public double alpha() {
        return alpha;
    }

    /**
     * Returns the color space this value belongs to.
     *
     * @return color space descriptor
     */
    public ColorSpace getSpace() {
        return space;
    }

    /**
     * Debug representation: "SpaceName[v0, v1, v2] alpha=A]".
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(space.getClass().getSimpleName()).append('[');

        for (int i = 0; i < raw.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(raw[i]);
        }

        sb.append("] alpha=").append(alpha);

        return sb.append(']').toString();
    }

    // ===== CONVENIENCE DELEGATES =====

    /**
     * Returns the hex string for this color (e.g. "#FF8000").
     * Delegates to {@link ColorHex#toHex(ColorValue)}.
     *
     * @return uppercase hex string with leading '#'
     */
    public String toHex() {
        return ColorHex.toHex(this);
    }

    /**
     * Parses a hex string into a ColorValue in sRGB.
     * Delegates to {@link ColorHex#parse(String)}.
     *
     * @param hex hex string (e.g. "#FF8000" or "FF8000")
     * @return new ColorValue in sRGB, or null if parsing fails
     */
    public static ColorValue fromHex(String hex) {
        return ColorHex.parse(hex);
    }

    /**
     * Converts this color to a java.awt.Color.
     * Delegates to {@link ColorAwt#toAWT(ColorValue)}.
     *
     * @return AWT Color with alpha
     */
    public java.awt.Color toAWT() {
        return ColorAwt.toAWT(this);
    }

    /**
     * Creates a ColorValue in sRGB from a java.awt.Color.
     * Delegates to {@link ColorAwt#fromAWT(java.awt.Color)}.
     *
     * @param color AWT Color source
     * @return new ColorValue in sRGB with normalized alpha
     */
    public static ColorValue fromAWT(java.awt.Color color) {
        return ColorAwt.fromAWT(color);
    }
}