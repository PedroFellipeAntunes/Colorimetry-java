package colorimetry.grayscale.channel;

import colorimetry.engine.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.xyz.Xyz;
import colorimetry.spaces.rgb.SRgb;

/**
 * Green channel isolation grayscale.
 *
 * Extracts the green channel value as brightness.
 */
public final class GreenChannel implements Grayscale {
    public static final GreenChannel INSTANCE = new GreenChannel();

    private GreenChannel() {}

    @Override
    public String displayName() {
        return "Green Channel";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double v = rgb[1];
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{v, v, v});
    }
}