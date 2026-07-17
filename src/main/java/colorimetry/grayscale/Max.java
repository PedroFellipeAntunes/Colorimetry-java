package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Rgb;
import colorimetry.spaces.Xyz;

/**
 * Max channel grayscale.
 *
 * Y = max(R, G, B)
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
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, Rgb.INSTANCE, xyz);
        double v = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        
        return ColorConverter.convert(Rgb.INSTANCE, Xyz.INSTANCE, new double[]{v, v, v});
    }
}