package colorimetry.registry;

import colorimetry.Grayscale;
import colorimetry.grayscale.channel.*;
import colorimetry.grayscale.luma.*;
import colorimetry.grayscale.perceptual.*;
import colorimetry.grayscale.simple.*;

/**
 * Central registry for grayscale conversion methods.
 * Pre-populated with all built-in methods in the static initializer.
 * Users can add custom methods via {@link Registry#register(Object)}.
 */
public final class GrayscaleRegistry {
    public static final Registry<Grayscale> INSTANCE = new Registry<>("Grayscale", Grayscale::displayName);

    static {
        // Channel isolation
        INSTANCE.register(RedChannel.INSTANCE);
        INSTANCE.register(GreenChannel.INSTANCE);
        INSTANCE.register(BlueChannel.INSTANCE);

        // Simple combinations
        INSTANCE.register(Average.INSTANCE);
        INSTANCE.register(Median.INSTANCE);
        INSTANCE.register(Max.INSTANCE);
        INSTANCE.register(Min.INSTANCE);
        INSTANCE.register(LightnessHsl.INSTANCE);

        // Weighted luma
        INSTANCE.register(Bt601Luma.INSTANCE);
        INSTANCE.register(Bt709Luma.INSTANCE);
        INSTANCE.register(Bt2020Luma.INSTANCE);
        INSTANCE.register(Smpte240mLuma.INSTANCE);

        // Perceptual
        INSTANCE.register(RelativeLuminance.INSTANCE);
        INSTANCE.register(CieLightness.INSTANCE);
        INSTANCE.register(OklabLightness.INSTANCE);
        INSTANCE.register(HspBrightness.INSTANCE);
    }

    private GrayscaleRegistry() {}
}