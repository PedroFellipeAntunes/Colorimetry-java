package colorimetry.grayscale.simple;

import colorimetry.engine.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.xyz.Xyz;
import colorimetry.spaces.rgb.SRgb;

/**
 * Max decomposition grayscale.
 *
 * Uses the maximum of the three sRGB channels: max(R, G, B).
 */
public final class Max implements Grayscale {
    public static final Max INSTANCE = new Max();

    private Max() {}

    @Override
    public String displayName() {
        return "Max";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double v = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{v, v, v});
    }
}