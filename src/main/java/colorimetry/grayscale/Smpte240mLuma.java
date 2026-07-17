package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.SRgb;

/**
 * SMPTE 240M Luma grayscale.
 *
 * Source: SMPTE 240M, 1988. Withdrawn 2014.
 *
 * Y = 0.212*R + 0.701*G + 0.087*B
 * Applied to gamma-encoded sRGB values.
 * Early American HDTV standard, predecessor to BT.709.
 */
public final class Smpte240mLuma implements Grayscale {

    public static final Smpte240mLuma INSTANCE = new Smpte240mLuma();

    private static final double WR = 0.212;
    private static final double WG = 0.701;
    private static final double WB = 0.087;

    private Smpte240mLuma() {}

    @Override
    public String displayName() {
        return "SMPTE 240M Luma";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double y = WR * rgb[0] + WG * rgb[1] + WB * rgb[2];
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{y, y, y});
    }
}