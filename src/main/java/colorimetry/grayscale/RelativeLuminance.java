package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.Rgb;

/**
 * Relative Luminance (CIE Y) grayscale.
 *
 * Source: CIE 1931 color matching functions.
 *
 * Y = 0.2126*Rl + 0.7152*Gl + 0.0722*Bl
 * BT.709 weights applied to LINEAR (gamma-removed) RGB.
 * This is the physical luminance, not the gamma-encoded luma.
 */
public final class RelativeLuminance implements Grayscale {
    public static final RelativeLuminance INSTANCE = new RelativeLuminance();

    private static final double WR = 0.2126;
    private static final double WG = 0.7152;
    private static final double WB = 0.0722;

    private RelativeLuminance() {}

    @Override
    public String displayName() {
        return "Relative Luminance (CIE Y)";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, Rgb.INSTANCE, xyz);
        double y = WR * rgb[0] + WG * rgb[1] + WB * rgb[2];
        
        return ColorConverter.convert(Rgb.INSTANCE, Xyz.INSTANCE, new double[]{y, y, y});
    }
}