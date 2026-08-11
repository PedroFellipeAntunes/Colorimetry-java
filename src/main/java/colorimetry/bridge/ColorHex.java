package colorimetry.bridge;

import colorimetry.*;
import colorimetry.spaces.rgb.SRgb;

/**
 * Hex string conversion utilities for colors.
 * All hex values are interpreted as sRGB. Conversions to other spaces
 * go through the standard ColorValue pipeline.
 */
public final class ColorHex {
    private ColorHex() {}

    /**
     * Formats a color as an uppercase hex string.
     * Returns "#RRGGBB" for fully opaque colors, "#RRGGBBAA" when alpha is present.
     * Converts through sRGB regardless of the color's current space.
     *
     * @param color source color in any space
     * @return uppercase hex string with leading '#'
     */
    public static String toHex(ColorValue color) {
        ColorValue rgb = color.to(SRgb.INSTANCE);

        int r = (int) Math.round(ColorSpace.clamp(rgb.get(0), 0.0, 255.0));
        int g = (int) Math.round(ColorSpace.clamp(rgb.get(1), 0.0, 255.0));
        int b = (int) Math.round(ColorSpace.clamp(rgb.get(2), 0.0, 255.0));
        int a = (int) Math.round(color.alpha() / ColorValue.ALPHA_MAX * 255.0);

        if (a < 255) {
            return String.format("#%02X%02X%02X%02X", r, g, b, a);
        }

        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Parses a hex string into a ColorValue in sRGB.
     * Accepts "#RRGGBB", "RRGGBB", "#RRGGBBAA", or "RRGGBBAA".
     *
     * @param hex hex string
     * @return new ColorValue in sRGB with alpha, or null if parsing fails
     */
    public static ColorValue parse(String hex) {
        try {
            String clean = hex.trim().replace("#", "");

            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);

            if (clean.length() >= 8) {
                double a = Integer.parseInt(clean.substring(6, 8), 16) / 255.0 * ColorValue.ALPHA_MAX;

                return ColorValue.of(SRgb.INSTANCE, new double[] {r, g, b}, a);
            }

            return ColorValue.of(SRgb.INSTANCE, r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}