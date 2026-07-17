package colorimetry;

/**
 * Descriptor for a color space. Each color space is one class in color/spaces/
 * that implements this interface.
 *
 * Parent hierarchy:
 *   All spaces descend from Xyz (the root). Each child declares parentSpace(),
 *   toParent() and fromParent(). Conversions walk up to the Lowest Common
 *   Ancestor (LCA) and back down, handled by {@link ColorConverter}.
 *
 * Normalization contract (user convenience only):
 *   normalize   -- maps raw values to [0, 1] range so the user never needs to know
 *                  channel bounds. Not used in the conversion pipeline.
 *   denormalize -- inverse of normalize, maps [0, 1] back to raw values.
 *
 * Metadata contract:
 *   displayName       -- canonical name ("sRGB", "OKLAB", etc.).
 *   componentCount    -- number of channels (3 for most, 4 for CMYK).
 *   componentName     -- full or short name of channel i.
 *   componentMin/Max  -- raw value range for channel i.
 */
public interface ColorSpace {
    // ===== NORMALIZATION (user convenience) =====

    /**
     * Maps raw values to [0, 1] range. Not used in the conversion pipeline.
     *
     * @param raw values in native units
     * @return normalized values in [0, 1]
     */
    double[] normalize(double[] raw);

    /**
     * Inverse of {@link #normalize}: maps [0, 1] back to raw values.
     *
     * @param normalized values in [0, 1]
     * @return raw values in native units
     */
    double[] denormalize(double[] normalized);

    // ===== GAMUT =====

    /**
     * Whether this space has a finite gamut (e.g. sRGB=true, Lab=false).
     * Bounded spaces require gamut mapping during conversion.
     * Child spaces inherit from their parent.
     *
     * @return true if the gamut is finite
     */
    default boolean isBounded() {
        ColorSpace parent = parentSpace();
        
        if (parent != null) {
            return parent.isBounded();
        }
        
        throw new UnsupportedOperationException(displayName() + " must implement isBounded or declare a parent");
    }

    /**
     * Tests whether a CIE XYZ D65 point is representable in this space without clipping.
     * Child spaces inherit from their parent.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return true if the point is inside the gamut
     */
    default boolean isInGamut(double[] xyz) {
        ColorSpace parent = parentSpace();
        
        if (parent != null) {
            return parent.isInGamut(xyz);
        }
        
        throw new UnsupportedOperationException(displayName() + " must implement isInGamut or declare a parent");
    }

    /**
     * Neutral gray point guaranteed to be in-gamut.
     * Used by {@link GamutMapper} as the bisection anchor.
     * Child spaces inherit from their parent.
     *
     * @return CIE XYZ D65 triplet
     */
    default double[] neutralXyz() {
        ColorSpace parent = parentSpace();
        
        if (parent != null) {
            return parent.neutralXyz();
        }
        
        throw new UnsupportedOperationException(displayName() + " must implement neutralXyz or declare a parent");
    }

    // ===== METADATA =====

    /**
     * Canonical display name of this color space (e.g. "sRGB", "OKLAB").
     *
     * @return display name
     */
    String displayName();

    /**
     * Number of channels (3 for most spaces, 4 for CMYK).
     *
     * @return channel count
     */
    int componentCount();

    /**
     * Name of channel i in full or abbreviated form.
     *
     * @param i channel index
     * @param full true for full name ("Saturation"), false for short ("S")
     * @return channel name
     */
    String componentName(int i, boolean full);

    /**
     * All channel names as an array.
     *
     * @param full true for full names, false for short
     * @return array of channel names
     */
    default String[] componentNames(boolean full) {
        int count = componentCount();
        String[] names = new String[count];
        
        for (int i = 0; i < count; i++) {
            names[i] = componentName(i, full);
        }
        
        return names;
    }

    /**
     * Extracts an abbreviation by keeping only uppercase letters.
     *
     * @param fullName full channel name (e.g. "Saturation")
     * @return abbreviation (e.g. "S")
     */
    static String shortOf(String fullName) {
        StringBuilder sb = new StringBuilder();
        
        for (char c : fullName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }

    /**
     * Minimum raw value for channel i.
     *
     * @param i channel index
     * @return minimum in native units
     */
    double componentMin(int i);

    /**
     * Maximum raw value for channel i.
     *
     * @param i channel index
     * @return maximum in native units
     */
    double componentMax(int i);

    /**
     * Initial raw value for channel i. Defaults to the midpoint of the range.
     *
     * @param i channel index
     * @return default raw value
     */
    default double componentDefault(int i) {
        return (componentMin(i) + componentMax(i)) / 2.0;
    }

    /**
     * Minimum increment for UI controls (spinners, sliders) on channel i.
     *
     * @param i channel index
     * @return step size in raw units
     */
    default double componentStep(int i) {
        return 1.0;
    }

    // ===== PARENT HIERARCHY =====

    /**
     * Returns the parent space in the conversion hierarchy, or null for root spaces.
     * Child spaces convert through their parent instead of XYZ when possible,
     * avoiding redundant round-trips.
     *
     * @return parent color space, or null if this is a root space
     */
    default ColorSpace parentSpace() {
        return null;
    }

    /**
     * Converts raw values in this space to raw values in the parent space.
     * Only meaningful when parentSpace() != null.
     *
     * @param raw values in this space's native units
     * @return values in the parent space's native units
     */
    default double[] toParent(double[] raw) {
        throw new UnsupportedOperationException(displayName() + " is a root space and has no parent conversion");
    }

    /**
     * Converts raw values from the parent space to raw values in this space.
     * Only meaningful when parentSpace() != null.
     *
     * @param parentRaw values in the parent space's native units
     * @return values in this space's native units
     */
    default double[] fromParent(double[] parentRaw) {
        throw new UnsupportedOperationException(displayName() + " is a root space and has no parent conversion");
    }

    // ===== PALETTE =====

    /**
     * Whether this space supports 2D palette display (e.g. hue × lightness).
     *
     * @return true if {@link #paletteChannels()} is meaningful
     */
    default boolean hasPalette() {
        return false;
    }

    /**
     * Channel indices used as axes for the 2D palette.
     *
     * @return {xAxis, yAxis} — e.g. {0, 2} means Hue × Brightness
     */
    default int[] paletteChannels() {
        return new int[]{0, 0};
    }
}