package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.SRgb;

/**
 * Min decomposition grayscale.
 *
 * Uses the minimum of the three sRGB channels: min(R, G, B).
 */
public final class Min implements Grayscale {
    public static final Min INSTANCE = new Min();

    private Min() {}

    @Override
    public String displayName() {
        return "Min";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double v = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{v, v, v});
    }
}