package colorimetry_test;

import colorimetry.*;
import colorimetry_test.utils.ErrorLog;

/**
 * Pixel conversion and reporting utilities for color space and grayscale tests.
 */
public final class ColorTestUtil {
    private static final int ERROR_PIXEL = 0xFFFF00FF;

    private ColorTestUtil() {}
    
    /**
     * Replaces spaces and special characters in a display name for use as filename.
     *
     * @param name display name (e.g. "CIE L* Lightness")
     * @return sanitized filename-safe string
     */
    public static String sanitizeName(String name) {
        return name.replace(" ", "_").replace("(", "").replace(")", "").replace("*", "");
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
        // Extract 8-bit channels from packed ARGB int
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        
        return ColorValue.of(colorimetry.spaces.SRgb.INSTANCE, red, green, blue);
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