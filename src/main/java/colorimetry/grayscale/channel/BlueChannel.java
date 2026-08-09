package colorimetry.grayscale.channel;

import colorimetry.engine.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.xyz.Xyz;
import colorimetry.spaces.rgb.SRgb;

/**
 * Blue channel isolation grayscale.
 *
 * Extracts the blue channel value as brightness.
 */
public final class BlueChannel implements Grayscale {
    public static final BlueChannel INSTANCE = new BlueChannel();

    private BlueChannel() {}

    @Override
    public String displayName() {
        return "Blue Channel";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] rgb = ColorConverter.convert(Xyz.INSTANCE, SRgb.INSTANCE, xyz);
        double v = rgb[2];
        
        return ColorConverter.convert(SRgb.INSTANCE, Xyz.INSTANCE, new double[]{v, v, v});
    }
}