package colorimetry.grayscale.luma;

import colorimetry.engine.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.rgb.Bt709RgbLinear;
import colorimetry.spaces.xyz.Xyz;
import colorimetry.spaces.rgb.SRgb;

/**
 * BT.709 Luma grayscale.
 *
 * Source: ITU-R BT.709, 1990.
 *
 * Y = 0.2126*R + 0.7152*G + 0.0722*B
 * Applied to gamma-encoded sRGB values.
 * The modern HDTV luma formula.
 */
public final class Bt709Luma implements Grayscale {
    public static final Bt709Luma INSTANCE = new Bt709Luma();

    private static final double WR = 0.2126;
    private static final double WG = 0.7152;
    private static final double WB = 0.0722;

    private Bt709Luma() {}

    @Override
    public String displayName() {
        return "BT.709 Luma";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.of(Bt709RgbLinear.INSTANCE), xyz);
        double y = WR * rgb[0] + WG * rgb[1] + WB * rgb[2];
        
        return ColorConverter.convert(SRgb.of(Bt709RgbLinear.INSTANCE), Xyz.INSTANCE, new double[]{y, y, y});
    }
}