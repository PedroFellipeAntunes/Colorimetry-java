package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.SRgb;

/**
 * Median grayscale.
 *
 * Takes the middle value of the three sRGB channels.
 */
public final class Median implements Grayscale {
    public static final Median INSTANCE = new Median();

    private Median() {}

    @Override
    public String displayName() {
        return "Median";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double[] sorted = {rgb[0], rgb[1], rgb[2]};
        
        java.util.Arrays.sort(sorted);
        
        double v = sorted[1];
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{v, v, v});
    }
}