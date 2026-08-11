package colorimetry_test.utils;

import colorimetry.*;
import colorimetry.registry.ColorSpaceRegistry;
import colorimetry.registry.GrayscaleRegistry;
import colorimetry.registry.MetricRegistry;
import colorimetry.spaces.rgb.SRgb;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pixel conversion, name matching, and reporting utilities for color space
 * and grayscale tests.
 */
public final class ColorTestUtil {
    private static final int ERROR_PIXEL = 0xFFFF00FF;

    private ColorTestUtil() {}

    // ===== NAME MATCHING =====

    /**
     * Normalizes a name for fuzzy matching: lowercase, strip all non-alphanumeric.
     * For example, "CIE Lab" becomes "cielab", "Rec. 2020" becomes "rec2020".
     *
     * @param name raw display name
     * @return normalized lowercase alphanumeric string
     */
    public static String normalizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Replaces spaces and special characters in a display name for use as filename.
     *
     * @param name display name (e.g. "CIE L* Lightness")
     * @return sanitized filename-safe string
     */
    public static String sanitizeName(String name) {
        return name.replace(" ", "_").replace("(", "").replace(")", "").replace("*", "");
    }

    /**
     * Filters registered color spaces whose normalized display name contains
     * the normalized input. Returns all spaces if input is null or empty.
     *
     * @param input user filter string (e.g. "rec20", "cielab", "HSB")
     * @return matching color spaces, or all if input is empty
     */
    public static List<ColorSpace> filterSpaces(String input) {
        List<ColorSpace> spaces = ColorSpaceRegistry.INSTANCE.getEntries();

        if (input == null || input.isEmpty()) {
            return spaces;
        }

        String key = normalizeName(input);

        return spaces.stream().filter(s -> normalizeName(s.displayName()).contains(key)).collect(Collectors.toList());
    }

    /**
     * Filters registered grayscale methods whose normalized display name contains
     * the normalized input. Returns all methods if input is null or empty.
     *
     * @param input user filter string
     * @return matching grayscale methods, or all if input is empty
     */
    public static List<Grayscale> filterMethods(String input) {
        List<Grayscale> methods = GrayscaleRegistry.INSTANCE.getEntries();

        if (input == null || input.isEmpty()) {
            return methods;
        }

        String key = normalizeName(input);

        return methods.stream().filter(m -> normalizeName(m.displayName()).contains(key)).collect(Collectors.toList());
    }

    /**
     * Filters registered distance metrics whose normalized display name contains
     * the normalized input. Returns all metrics if input is null or empty.
     *
     * @param input user filter string
     * @return matching distance metrics, or all if input is empty
     */
    public static List<DistanceMetric> filterMetrics(String input) {
        List<DistanceMetric> metrics = MetricRegistry.INSTANCE.getEntries();

        if (input == null || input.isEmpty()) {
            return metrics;
        }

        String key = normalizeName(input);

        return metrics.stream().filter(m -> normalizeName(m.displayName()).contains(key)).collect(Collectors.toList());
    }

    // ===== SAFE PIXEL CONVERSION =====

    /**
     * Converts a ColorValue to an ARGB pixel via toAWT().
     * Returns magenta on error and logs the failure.
     *
     * @param color source color
     * @param log error log to record failures
     * @param context description of the pixel being converted
     * @return ARGB packed int
     */
    public static int toPixel(ColorValue color, ErrorLog log, String context) {
        try {
            return color.toAWT().getRGB();
        } catch (Exception e) {
            log.log(context, e);
            
            return ERROR_PIXEL;
        }
    }

    /**
     * Applies a grayscale method to a ColorValue and returns the ARGB pixel.
     * Returns magenta on error and logs the failure.
     *
     * @param source color to convert
     * @param method grayscale conversion strategy
     * @param log error log to record failures
     * @param context description of the pixel being converted
     * @return ARGB packed int
     */
    public static int toGrayscalePixel(ColorValue source, Grayscale method, ErrorLog log, String context) {
        try {
            ColorValue gray = source.toGrayscale(method);
            
            return gray.toAWT().getRGB();
        } catch (Exception e) {
            log.log(context, e);
            
            return ERROR_PIXEL;
        }
    }

    /**
     * Converts an ARGB packed int to a ColorValue in sRGB.
     *
     * @param argb ARGB packed int
     * @return ColorValue in sRGB
     */
    public static ColorValue pixelToColor(int argb) {
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        
        return ColorValue.of(SRgb.INSTANCE, red, green, blue);
    }

    // ===== REPORTING =====

    /**
     * Prints a summary line for a generated file.
     *
     * @param fileName name of the output file
     * @param elapsedMs generation time in milliseconds
     * @param log error log from the generation
     */
    public static void printResult(String fileName, long elapsedMs, ErrorLog log) {
        System.out.println(fileName + " (" + elapsedMs + "ms)" + (log.count() > 0 ? " ERRORS: " + log.count() : " OK"));
        log.print();
    }
}