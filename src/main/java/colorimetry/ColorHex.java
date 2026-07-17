package colorimetry;

import colorimetry.spaces.SRgb;

/**
 * Hex string conversion utilities for colors.
 * All hex values are interpreted as sRGB. Conversions to other spaces
 * go through the standard ColorValue pipeline.
 */
public final class ColorHex {
    private ColorHex() {}

    /**
     * Formats a color as an uppercase hex string (e.g. "#FF8000").
     * Converts through sRGB regardless of the color's current space.
     *
     * @param color source color in any space
     * @return uppercase hex string with leading '#'
     */
    public static String toHex(ColorValue color) {
        ColorValue rgb = color.to(SRgb.INSTANCE);

        int r = clampToByte(rgb.get(0));
        int g = clampToByte(rgb.get(1));
        int b = clampToByte(rgb.get(2));

        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Parses a hex string into a ColorValue in sRGB.
     *
     * @param hex hex string (e.g. "#FF8000" or "FF8000")
     * @return new ColorValue in sRGB, or null if parsing fails
     */
    public static ColorValue parse(String hex) {
        try {
            String clean = hex.trim().replace("#", "");

            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);

            return ColorValue.of(SRgb.INSTANCE, r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a hex string and converts the result to the given color space.
     *
     * @param hex hex string (e.g. "#FF8000" or "FF8000")
     * @param target destination color space
     * @return new ColorValue in target space, or null if parsing fails
     */
    public static ColorValue parse(String hex, ColorSpace target) {
        ColorValue srgb = parse(hex);

        if (srgb == null) {
            return null;
        }

        return srgb.to(target);
    }

    /**
     * Clamps and rounds a channel value to [0, 255].
     *
     * @param value channel value
     * @return integer byte in [0, 255]
     */
    private static int clampToByte(double value) {
        return (int) Math.round(Math.max(0.0, Math.min(255.0, value)));
    }
}