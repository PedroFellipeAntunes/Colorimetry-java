package colorimetry;

import colorimetry.grayscale.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for grayscale conversion methods.
 * Pre-populated with all built-in methods in the static initializer.
 * Users can add custom methods via {@link #register(Grayscale)}.
 */
public final class GrayscaleRegistry {
    private static final List<Grayscale> METHODS = new ArrayList<>();

    static {
        // Channel isolation
        register(RedChannel.INSTANCE);
        register(GreenChannel.INSTANCE);
        register(BlueChannel.INSTANCE);

        // Simple combinations
        register(Average.INSTANCE);
        register(Median.INSTANCE);
        register(Max.INSTANCE);
        register(Min.INSTANCE);
        register(LightnessHsl.INSTANCE);

        // Weighted luma
        register(Bt601Luma.INSTANCE);
        register(Bt709Luma.INSTANCE);
        register(Bt2020Luma.INSTANCE);
        register(Smpte240mLuma.INSTANCE);

        // Perceptual
        register(RelativeLuminance.INSTANCE);
        register(CieLightness.INSTANCE);
        register(OklabLightness.INSTANCE);
        register(HspBrightness.INSTANCE);
    }

    private GrayscaleRegistry() {}

    /**
     * Adds a grayscale method to the registry.
     *
     * @param method grayscale method to register
     * @throws IllegalArgumentException if method is null or already registered
     */
    public static void register(Grayscale method) {
        if (method == null) {
            throw new IllegalArgumentException("Grayscale cannot be null");
        }

        if (METHODS.contains(method)) {
            throw new IllegalArgumentException("Grayscale already registered: " + method.displayName());
        }

        METHODS.add(method);
    }

    /**
     * Removes a specific grayscale method from the registry.
     *
     * @param method grayscale method to remove
     * @throws IllegalArgumentException if method is null or not registered
     */
    public static void unregister(Grayscale method) {
        if (method == null) {
            throw new IllegalArgumentException("Grayscale cannot be null");
        }

        if (!METHODS.remove(method)) {
            throw new IllegalArgumentException("Grayscale not registered: " + method.displayName());
        }
    }

    /**
     * Checks whether a grayscale method is already registered.
     *
     * @param method grayscale method to look up
     * @return true if the method is in the registry
     * @throws IllegalArgumentException if method is null
     */
    public static boolean contains(Grayscale method) {
        if (method == null) {
            throw new IllegalArgumentException("Grayscale cannot be null");
        }

        return METHODS.contains(method);
    }

    /**
     * Returns the number of registered grayscale methods.
     *
     * @return registry size
     */
    public static int size() {
        return METHODS.size();
    }

    /**
     * Returns all registered grayscale methods.
     *
     * @return unmodifiable list of grayscale methods
     */
    public static List<Grayscale> getMethods() {
        return Collections.unmodifiableList(METHODS);
    }

    /**
     * Removes all registered methods. Intended for testing.
     */
    public static void clear() {
        METHODS.clear();
    }
}