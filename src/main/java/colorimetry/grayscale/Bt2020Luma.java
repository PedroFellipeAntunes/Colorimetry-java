package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.SRgb;

/**
 * BT.2020 Luma grayscale.
 *
 * Source: ITU-R BT.2020, 2012.
 *
 * Y = 0.2627*R + 0.6780*G + 0.0593*B
 * Applied to gamma-encoded sRGB values.
 * UHDTV 4K/8K luma formula. BT.2100 (HDR) uses the same weights.
 */
public final class Bt2020Luma implements Grayscale {
    public static final Bt2020Luma INSTANCE = new Bt2020Luma();

    private static final double WR = 0.2627;
    private static final double WG = 0.6780;
    private static final double WB = 0.0593;

    private Bt2020Luma() {}

    @Override
    public String displayName() {
        return "BT.2020 Luma";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double y = WR * rgb[0] + WG * rgb[1] + WB * rgb[2];
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{y, y, y});
    }
}