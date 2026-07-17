package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.SRgb;

/**
 * Red channel isolation grayscale.
 *
 * Extracts the red channel value as brightness.
 */
public final class RedChannel implements Grayscale {
    public static final RedChannel INSTANCE = new RedChannel();

    private RedChannel() {}

    @Override
    public String displayName() {
        return "Red Channel";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double v = rgb[0];
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{v, v, v});
    }
}