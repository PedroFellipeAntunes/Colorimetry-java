package colorimetry.registry;

import colorimetry.ColorSpace;
import colorimetry.spaces.cam.*;
import colorimetry.spaces.cie.*;
import colorimetry.spaces.hue.*;
import colorimetry.spaces.perceptual.*;
import colorimetry.spaces.rgb.*;
import colorimetry.spaces.xyz.*;

/**
 * Central registry for color spaces.
 * Pre-populated with all built-in spaces in the static initializer.
 * Users can add custom spaces via {@link Registry#register(Object)}.
 */
public final class ColorSpaceRegistry {
    public static final Registry<ColorSpace> INSTANCE = new Registry<>("ColorSpace", ColorSpace::displayName);

    static {
        // XYZ adapted
        INSTANCE.register(XyzD65.INSTANCE);
        INSTANCE.register(XyzD50.INSTANCE);
        INSTANCE.register(XyzD60.INSTANCE);
        INSTANCE.register(Xyy.INSTANCE);

        // Linear RGB
        INSTANCE.register(Bt709RgbLinear.INSTANCE);
        INSTANCE.register(Bt601RgbLinear.INSTANCE);
        INSTANCE.register(Bt2020RgbLinear.INSTANCE);
        INSTANCE.register(DisplayP3Linear.INSTANCE);
        INSTANCE.register(AdobeRgbLinear.INSTANCE);
        INSTANCE.register(ProPhotoRgbLinear.INSTANCE);

        // Gamma RGB
        INSTANCE.register(SRgb.INSTANCE);
        INSTANCE.register(AdobeRgb.INSTANCE);
        INSTANCE.register(DisplayP3.INSTANCE);
        INSTANCE.register(ProPhotoRgb.INSTANCE);
        INSTANCE.register(Rec2020.INSTANCE);

        // Subtractive and artistic
        INSTANCE.register(Cmy.INSTANCE);
        INSTANCE.register(Cmyk.INSTANCE);
        INSTANCE.register(Ryb.INSTANCE);

        // Hue-based
        INSTANCE.register(Hsb.INSTANCE);
        INSTANCE.register(Hsl.INSTANCE);
        INSTANCE.register(Hsi.INSTANCE);
        INSTANCE.register(Hcb.INSTANCE);
        INSTANCE.register(Hcl.INSTANCE);
        INSTANCE.register(Hcy.INSTANCE);
        INSTANCE.register(Hwb.INSTANCE);
        INSTANCE.register(Hsp.INSTANCE);
        INSTANCE.register(Hsluv.INSTANCE);
        INSTANCE.register(Hpluv.INSTANCE);
        INSTANCE.register(OkHsl.INSTANCE);
        INSTANCE.register(OkHsv.INSTANCE);

        // CIE perceptual
        INSTANCE.register(CieLab.INSTANCE);
        INSTANCE.register(CieLch.INSTANCE);
        INSTANCE.register(CieLuv.INSTANCE);
        INSTANCE.register(CieLchuv.INSTANCE);

        // LMS and derivatives
        INSTANCE.register(LmsHpe.INSTANCE);
        INSTANCE.register(Ipt.INSTANCE);
        INSTANCE.register(IgPgTg.INSTANCE);

        // Modern perceptual
        INSTANCE.register(Oklab.INSTANCE);
        INSTANCE.register(Oklch.INSTANCE);
        INSTANCE.register(JzAzBz.INSTANCE);
        INSTANCE.register(JzCzHz.INSTANCE);
        INSTANCE.register(ICtCp.INSTANCE);
        INSTANCE.register(Xyb.INSTANCE);
        INSTANCE.register(Msh.INSTANCE);

        // ACES
        INSTANCE.register(Aces2065.INSTANCE);
        INSTANCE.register(AcesCg.INSTANCE);

        // CAM
        INSTANCE.register(Cam16.INSTANCE);
        INSTANCE.register(Cam16Ucs.INSTANCE);
        INSTANCE.register(Cam02.INSTANCE);
        INSTANCE.register(Cam02Ucs.INSTANCE);
        INSTANCE.register(Hct.INSTANCE);
        INSTANCE.register(Zcam.INSTANCE);
    }

    private ColorSpaceRegistry() {}

    /**
     * Returns the first registered color space that supports 2D palette display.
     *
     * @return a palette-capable color space, or null if none is registered
     */
    public static ColorSpace getPaletteSpace() {
        return INSTANCE.getEntries().stream().filter(ColorSpace::hasPalette).findFirst().orElse(null);
    }
}