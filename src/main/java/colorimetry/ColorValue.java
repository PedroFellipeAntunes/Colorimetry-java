package colorimetry;

/**
 * A color value in a specific color space, stored internally as raw doubles in the
 * native units of that space (e.g. sRGB 0-255, HSB 0-360/0-100/0-100, CIE Lab 0-100/-128..127/-128..127).
 *
 * ColorValue is immutable. Every conversion returns a new instance.
 *
 * Alpha is carried as a separate field in [0.0, 1.0], default 1.0. It is never
 * used in any color math and conversions preserve it unchanged.
 *
 * Creation:
 *   of(space, values...)               -- from raw values, alpha=1.0
 *   of(space, rawValues, alpha)        -- from raw values with explicit alpha
 *   ofNormalized(space, values...)     -- from [0,1] normalized values, alpha=1.0
 *   ofNormalized(space, values, alpha) -- from [0,1] normalized with explicit alpha
 *
 * Reading:
 *   get(i)                 -- raw value for component i
 *   getRaw()               -- all raw values
 *   getNormalized(i)       -- normalized [0,1] value for component i
 *   getNormalizedValues()  -- all normalized values
 *   alpha()                -- alpha in [0.0, 1.0]
 *
 * Conversion:
 *   to(target)          -- converts via parent hierarchy or XYZ hub
 *   toGrayscale(method) -- achromatic equivalent, stays in same space
 *   toHex()             -- hex string via ColorHex
 *   toAWT()             -- java.awt.Color via ColorAwt
 */
public final class ColorValue {
    /** Display label for the alpha channel, used by UI components. */
    public static final String ALPHA_LABEL = "Alpha";

    private final double[] raw;
    private final ColorSpace space;
    private final double alpha;

    /**
     * Internal constructor. Clones the array to preserve immutability
     * and clamps alpha to [0, 1].
     *
     * @param raw channel values in native units
     * @param space color space descriptor
     * @param alpha opacity in [0.0, 1.0]
     */
    private ColorValue(double[] raw, ColorSpace space, double alpha) {
        this.raw = raw.clone();
        this.space = space;
        this.alpha = Math.max(0.0, Math.min(1.0, alpha));
    }

    // ===== FACTORIES =====

    /**
     * Creates a ColorValue from raw values with alpha=1.0.
     *
     * @param space color space descriptor
     * @param values channel values in native units
     * @return new ColorValue
     */
    public static ColorValue of(ColorSpace space, double... values) {
        return new ColorValue(values, space, 1.0);
    }

    /**
     * Creates a ColorValue from raw values with explicit alpha.
     *
     * @param space color space descriptor
     * @param values channel values in native units
     * @param alpha opacity in [0.0, 1.0]
     * @return new ColorValue
     */
    public static ColorValue of(ColorSpace space, double[] values, double alpha) {
        return new ColorValue(values, space, alpha);
    }

    /**
     * Creates a ColorValue from [0,1] normalized values with alpha=1.0.
     * Values are denormalized to native units via the space's denormalize().
     *
     * @param space color space descriptor
     * @param values normalized channel values in [0, 1]
     * @return new ColorValue
     */
    public static ColorValue ofNormalized(ColorSpace space, double... values) {
        return new ColorValue(space.denormalize(values), space, 1.0);
    }

    /**
     * Creates a ColorValue from [0,1] normalized values with explicit alpha.
     * Values are denormalized to native units via the space's denormalize().
     *
     * @param space color space descriptor
     * @param values normalized channel values in [0, 1]
     * @param alpha opacity in [0.0, 1.0]
     * @return new ColorValue
     */
    public static ColorValue ofNormalized(ColorSpace space, double[] values, double alpha) {
        return new ColorValue(space.denormalize(values), space, alpha);
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
        double[] xyz = ColorConverter.convert(space, colorimetry.spaces.Xyz.INSTANCE, raw);
        double[] grayXyz = method.toGrayXyz(xyz);

        // Gray point may land outside the gamut of the original space
        if (space.isBounded() && !space.isInGamut(grayXyz)) {
            grayXyz = GamutMapper.map(grayXyz, space);
        }

        double[] result = ColorConverter.convert(colorimetry.spaces.Xyz.INSTANCE, space, grayXyz);
        
        return new ColorValue(result, space, alpha);
    }

    // ===== ACCESSORS =====

    /**
     * Returns the raw value for a single channel.
     *
     * @param i channel index
     * @return raw value in native units
     */
    public double get(int i) {
        return raw[i];
    }

    /**
     * Returns a copy of all raw channel values.
     *
     * @return raw values array (defensive copy)
     */
    public double[] getRaw() {
        return raw.clone();
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
    public double[] getNormalizedValues() {
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
     * Parses a hex string and converts the result to this instance's space.
     * Delegates to {@link ColorHex#parse(String, ColorSpace)}.
     *
     * @param hex hex string (e.g. "#FF8000" or "FF8000")
     * @return new ColorValue in this instance's space, or null if parsing fails
     */
    public ColorValue fromHex(String hex) {
        return ColorHex.parse(hex, space);
    }

    /**
     * Parses a hex string into a ColorValue in sRGB.
     * Delegates to {@link ColorHex#parse(String)}.
     *
     * @param hex hex string (e.g. "#FF8000" or "FF8000")
     * @return new ColorValue in sRGB, or null if parsing fails
     */
    public static ColorValue parseHex(String hex) {
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
     * @return new ColorValue in sRGB, alpha=1.0
     */
    public static ColorValue fromAWT(java.awt.Color color) {
        return ColorAwt.fromAWT(color);
    }
}