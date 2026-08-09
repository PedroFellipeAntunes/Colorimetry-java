package colorimetry.registry;

import colorimetry.ColorSpace;
import colorimetry.spaces.cam.*;
import colorimetry.spaces.cie.*;
import colorimetry.spaces.hue.*;
import colorimetry.spaces.perceptual.*;
import colorimetry.spaces.rgb.*;
import colorimetry.spaces.xyz.*;

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
        // XYZ adapted spaces (Xyz root is internal, not registered)
        register(XyzD65.INSTANCE);
        register(XyzD50.INSTANCE);
        register(XyzD60.INSTANCE);
        register(Xyy.INSTANCE);

        // Linear RGB spaces
        register(Bt709RgbLinear.INSTANCE);
        register(Bt601RgbLinear.INSTANCE);
        register(Bt2020RgbLinear.INSTANCE);
        register(DisplayP3Linear.INSTANCE);
        register(AdobeRgbLinear.INSTANCE);
        register(ProPhotoRgbLinear.INSTANCE);
        register(Aces2065.INSTANCE);
        register(AcesCg.INSTANCE);

        // Gamma RGB spaces
        register(SRgb.INSTANCE);
        register(DisplayP3.INSTANCE);
        register(AdobeRgb.INSTANCE);
        register(ProPhotoRgb.INSTANCE);
        register(Rec2020.INSTANCE);

        // Subtractive and artistic
        register(Cmy.INSTANCE);
        register(Cmyk.INSTANCE);
        register(Ryb.INSTANCE);

        // Hue-based (cylindrical from RGB)
        register(Hsb.INSTANCE);
        register(Hsl.INSTANCE);
        register(Hsi.INSTANCE);
        register(Hcb.INSTANCE);
        register(Hcl.INSTANCE);
        register(Hcy.INSTANCE);
        register(Hwb.INSTANCE);
        register(Hsp.INSTANCE);

        // CIE perceptual spaces
        register(CieLab.INSTANCE);
        register(CieLch.INSTANCE);
        register(CieLuv.INSTANCE);
        register(CieLchuv.INSTANCE);

        // Luv-based bounded hue spaces
        register(Hsluv.INSTANCE);
        register(Hpluv.INSTANCE);

        // LMS and derivatives
        register(LmsHpe.INSTANCE);
        register(Ipt.INSTANCE);
        register(IgPgTg.INSTANCE);

        // Modern perceptual spaces
        register(Oklab.INSTANCE);
        register(Oklch.INSTANCE);
        register(OkHsl.INSTANCE);
        register(OkHsv.INSTANCE);
        register(JzAzBz.INSTANCE);
        register(JzCzHz.INSTANCE);
        register(ICtCp.INSTANCE);
        register(Xyb.INSTANCE);
        register(Msh.INSTANCE);

        // Color Appearance Models
        register(Cam16.INSTANCE);
        register(Cam16Ucs.INSTANCE);
        register(Cam02.INSTANCE);
        register(Cam02Ucs.INSTANCE);
        register(Hct.INSTANCE);
        register(Zcam.INSTANCE);
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
     * Returns the first registered color space that supports 2D palette display.
     *
     * @return a palette-capable color space, or null if none is registered
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