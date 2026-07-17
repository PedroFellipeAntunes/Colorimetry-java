package colorimetry;

import colorimetry.spaces.SRgb;

/**
 * Conversion utilities between ColorValue and java.awt.Color.
 * All AWT interop goes through sRGB since java.awt.Color is sRGB-based.
 */
public final class ColorAwt {
    private ColorAwt() {}

    /**
     * Converts a ColorValue to a java.awt.Color via sRGB.
     * Channels are clamped to [0, 255] before rounding.
     *
     * @param color source color in any space
     * @return AWT Color with alpha
     */
    public static java.awt.Color toAWT(ColorValue color) {
        ColorValue rgb = color.to(SRgb.INSTANCE);

        int r = clampToByte(rgb.get(0));
        int g = clampToByte(rgb.get(1));
        int b = clampToByte(rgb.get(2));
        int a = (int) Math.round(color.alpha() * 255.0);

        return new java.awt.Color(r, g, b, a);
    }

    /**
     * Creates a ColorValue in sRGB from a java.awt.Color.
     *
     * @param color AWT Color source
     * @return new ColorValue in sRGB, alpha=1.0
     */
    public static ColorValue fromAWT(java.awt.Color color) {
        return ColorValue.of(SRgb.INSTANCE, color.getRed(), color.getGreen(), color.getBlue());
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