package colorimetry.grayscale.simple;

import colorimetry.engine.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.xyz.Xyz;
import colorimetry.spaces.hue.Hsl;

/**
 * HSL Lightness grayscale.
 *
 * Source: G. H. Joblove and D. Greenberg, "Color Spaces for Computer
 *         Graphics", SIGGRAPH 1978.
 *
 * Uses the Lightness channel of HSL: (max + min) / 2.
 */
public final class LightnessHsl implements Grayscale {
    public static final LightnessHsl INSTANCE = new LightnessHsl();

    private LightnessHsl() {}

    @Override
    public String displayName() {
        return "Lightness (HSL)";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] hsl = ColorConverter.convert(Xyz.INSTANCE, Hsl.INSTANCE, xyz);
        
        return ColorConverter.convert(Hsl.INSTANCE, Xyz.INSTANCE, new double[]{0.0, 0.0, hsl[2]});
    }
}