package colorimetry.grayscale.luma;

import colorimetry.engine.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.rgb.Bt601RgbLinear;
import colorimetry.spaces.xyz.Xyz;
import colorimetry.spaces.rgb.SRgb;

/**
 * BT.601 Luma grayscale.
 *
 * Source: ITU-R BT.601, 1982.
 *
 * Y = 0.299*R + 0.587*G + 0.114*B
 * Applied to gamma-encoded sRGB values.
 * The classic SDTV/NTSC/PAL/JPEG luma formula.
 */
public final class Bt601Luma implements Grayscale {
    public static final Bt601Luma INSTANCE = new Bt601Luma();

    private static final double WR = 0.299;
    private static final double WG = 0.587;
    private static final double WB = 0.114;

    private Bt601Luma() {}

    @Override
    public String displayName() {
        return "BT.601 Luma";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.of(Bt601RgbLinear.INSTANCE), xyz);
        double y = WR * rgb[0] + WG * rgb[1] + WB * rgb[2];
        
        return ColorConverter.convert(SRgb.of(Bt601RgbLinear.INSTANCE), Xyz.INSTANCE, new double[]{y, y, y});
    }
}