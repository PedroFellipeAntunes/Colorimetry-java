package colorimetry.grayscale.perceptual;

import colorimetry.engine.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.xyz.Xyz;
import colorimetry.spaces.perceptual.Oklab;

/**
 * Oklab L Lightness grayscale.
 *
 * Source: B. Ottosson, "A perceptual color space for image processing", 2020.
 *         https://bottosson.github.io/posts/oklab/
 *
 * Extracts L from Oklab and zeroes the chromatic channels.
 * More perceptually uniform than CIE L*, especially in saturated tones.
 */
public final class OklabLightness implements Grayscale {
    public static final OklabLightness INSTANCE = new OklabLightness();

    private OklabLightness() {}

    @Override
    public String displayName() {
        return "Oklab L Lightness";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] oklab = ColorConverter.convert(Xyz.INSTANCE, Oklab.INSTANCE, xyz);
        
        return ColorConverter.convert(Oklab.INSTANCE, Xyz.INSTANCE, new double[]{oklab[0], 0.0, 0.0});
    }
}