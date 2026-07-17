package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.SRgb;

/**
 * Average grayscale.
 *
 * Y = (R + G + B) / 3
 */
public final class Average implements Grayscale {
    public static final Average INSTANCE = new Average();

    private Average() {}

    @Override
    public String displayName() {
        return "Average";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double y = (rgb[0] + rgb[1] + rgb[2]) / 3.0;
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{y, y, y});
    }
}