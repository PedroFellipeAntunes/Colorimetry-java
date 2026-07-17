package colorimetry.grayscale;

import colorimetry.ColorConverter;
import colorimetry.Grayscale;
import colorimetry.spaces.Xyz;
import colorimetry.spaces.CieLab;

/**
 * CIE L* Lightness grayscale.
 *
 * Source: CIE Publication 15:2004, "Colorimetry".
 *         Originally defined in CIE 1976 (L*, a*, b*) recommendation.
 *
 * Extracts L* from CIE Lab and zeroes the chromatic channels.
 * Perceptually uniform: L*=50 appears visually half as bright as L*=100.
 */
public final class CieLightness implements Grayscale {
    public static final CieLightness INSTANCE = new CieLightness();

    private CieLightness() {}

    @Override
    public String displayName() {
        return "CIE L* Lightness";
    }

    @Override
    public double[] toGrayXyz(double[] xyz) {
        double[] lab = ColorConverter.convert(Xyz.INSTANCE, CieLab.INSTANCE, xyz);
        
        return ColorConverter.convert(CieLab.INSTANCE, Xyz.INSTANCE, new double[]{lab[0], 0.0, 0.0});
    }
}