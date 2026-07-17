package colorimetry;

import colorimetry.spaces.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for color spaces.
 * Pre-populated with all built-in spaces in the static initializer.
 * Users can add custom spaces via {@link #register(ColorSpace)}.
 */
public final class ColorSpaceRegistry {
    private static final List<ColorSpace> SPACES = new ArrayList<>();

    static {
        register(SRgb.INSTANCE);
        register(Rgb.INSTANCE);
        register(Hsl.INSTANCE);
        register(Hsb.INSTANCE);
        register(Hsi.INSTANCE);
        register(Hcb.INSTANCE);
        register(Hcy.INSTANCE);
        register(Hwb.INSTANCE);
        register(Hsp.INSTANCE);
        register(Oklab.INSTANCE);
        register(Oklch.INSTANCE);
        register(CieLab.INSTANCE);
        register(CieLch.INSTANCE);
        register(CieLuv.INSTANCE);
        register(CieLchuv.INSTANCE);
        register(JzAzBz.INSTANCE);
        register(JzCzHz.INSTANCE);
        register(Cmyk.INSTANCE);
    }

    private ColorSpaceRegistry() {}

    /**
     * Adds a color space to the registry.
     *
     * @param space color space to register
     * @throws IllegalArgumentException if space is null or already registered
     */
    public static void register(ColorSpace space) {
        if (space == null) {
            throw new IllegalArgumentException("ColorSpace cannot be null");
        }

        if (SPACES.contains(space)) {
            throw new IllegalArgumentException("ColorSpace already registered: " + space.displayName());
        }

        SPACES.add(space);
    }

    /**
     * Removes a specific color space from the registry.
     *
     * @param space color space to remove
     * @throws IllegalArgumentException if space is null or not registered
     */
    public static void unregister(ColorSpace space) {
        if (space == null) {
            throw new IllegalArgumentException("ColorSpace cannot be null");
        }

        if (!SPACES.remove(space)) {
            throw new IllegalArgumentException("ColorSpace not registered: " + space.displayName());
        }
    }

    /**
     * Checks whether a color space is already registered.
     *
     * @param space color space to look up
     * @return true if the space is in the registry
     * @throws IllegalArgumentException if space is null
     */
    public static boolean contains(ColorSpace space) {
        if (space == null) {
            throw new IllegalArgumentException("ColorSpace cannot be null");
        }

        return SPACES.contains(space);
    }

    /**
     * Returns the number of registered color spaces.
     *
     * @return registry size
     */
    public static int size() {
        return SPACES.size();
    }

    /**
     * Returns all registered color spaces.
     *
     * @return unmodifiable list of color spaces
     */
    public static List<ColorSpace> getSpaces() {
        return Collections.unmodifiableList(SPACES);
    }

    /**
     * Returns the first registered space that supports 2D palette display.
     *
     * @return first palette-capable space, or null if none registered
     */
    public static ColorSpace getPaletteSpace() {
        return SPACES.stream().filter(ColorSpace::hasPalette).findFirst().orElse(null);
    }

    /**
     * Removes all registered spaces. Intended for testing.
     */
    public static void clear() {
        SPACES.clear();
    }
}