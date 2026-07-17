package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.Hsp;

/**
 * HSP Perceived Brightness grayscale.
 *
 * Source: D. R. Finley, "HSP Color Model - Alternative to HSV (HSB) and HSL",
 *         http://alienryderflex.com/hsp.html
 *
 * Extracts P from HSP and zeroes hue and saturation.
 * P = sqrt(0.299*R^2 + 0.587*G^2 + 0.114*B^2).
 */
public final class HspBrightness implements Grayscale {
    public static final HspBrightness INSTANCE = new HspBrightness();

    private HspBrightness() {}

    @Override
    public String displayName() {
        return "HSP Perceived Brightness";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] hsp = ColorConverter.convert(Xyz.INSTANCE, Hsp.INSTANCE, xyz);
        
        return ColorConverter.convert(Hsp.INSTANCE, Xyz.INSTANCE, new double[]{0.0, 0.0, hsp[2]});
    }
}