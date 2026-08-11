package colorimetry_test;

import colorimetry_test.utils.ColorTestUtil;
import colorimetry.*;
import colorimetry_test.utils.ErrorLog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Generates lerp gradient PNGs comparing interpolation across color spaces.
 *
 * For each registered color space, both input colors are converted to that space,
 * interpolated via lerp across X (t = 0.0 to 1.0), and converted back to sRGB
 * for display. Each space produces a separate image.
 *
 * Output: color_tests/lerp/lerp_{spaceName}.png
 */
public final class LerpGradientTest {
    /** First color as hex string. Overridden by args[0]. */
    private static final String DEFAULT_COLOR_1 = "#FF0000";

    /** Second color as hex string. Overridden by args[1]. */
    private static final String DEFAULT_COLOR_2 = "#0000FF";

    /** Optional: filter by space name. Empty = all spaces. Overridden by args[2]. */
    private static final String FILTER_SPACE = "";

    /** Number of interpolation steps (pixels wide). Overridden by args[3]. */
    private static final int DEFAULT_STEPS = 512;

    private static final int HEIGHT = 128;

    public static void main(String[] args) throws IOException {
        File outputDir = new File("color_tests/lerp");
        outputDir.mkdirs();

        String hex1 = args.length > 0 ? args[0] : DEFAULT_COLOR_1;
        String hex2 = args.length > 1 ? args[1] : DEFAULT_COLOR_2;
        String filterSpace = args.length > 2 ? args[2] : FILTER_SPACE;
        int steps = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_STEPS;

        ColorValue color1 = ColorValue.fromHex(hex1);
        ColorValue color2 = ColorValue.fromHex(hex2);

        if (color1 == null || color2 == null) {
            System.err.println("Invalid hex input: " + hex1 + " or " + hex2);

            return;
        }

        List<ColorSpace> spaces = ColorTestUtil.filterSpaces(filterSpace);

        if (spaces.isEmpty()) {
            System.err.println("No matching color spaces found.");

            return;
        }

        System.out.println("=== Lerp Gradient Test ===");
        System.out.println("Color 1: " + hex1 + " | Color 2: " + hex2);
        System.out.println("Spaces: " + spaces.size() + " | Steps: " + steps + " | Size: " + steps + "x" + HEIGHT);
        System.out.println();

        for (ColorSpace space : spaces) {
            generateGradient(space, color1, color2, outputDir, steps);
        }

        System.out.println("Done. Output: " + outputDir.getAbsolutePath());
    }

    /**
     * Generates a single lerp gradient PNG for one color space.
     * X axis varies t from 0.0 (left) to 1.0 (right).
     * Y axis is uniform (same color per column).
     *
     * @param space color space to interpolate in
     * @param color1 start color
     * @param color2 end color
     * @param outputDir directory to write the PNG
     * @param steps number of interpolation steps (image width)
     * @throws IOException if writing fails
     */
    private static void generateGradient(ColorSpace space, ColorValue color1, ColorValue color2, File outputDir, int steps) throws IOException {
        String name = ColorTestUtil.sanitizeName(space.displayName());
        long start = System.currentTimeMillis();
        ErrorLog log = new ErrorLog();

        // Convert both colors to the target space
        ColorValue c1;
        ColorValue c2;

        try {
            c1 = color1.to(space);
            c2 = color2.to(space);
        } catch (Exception e) {
            log.log("conversion to " + space.displayName(), e);
            ColorTestUtil.printResult("lerp_" + name + ".png", System.currentTimeMillis() - start, log);

            return;
        }

        int[] pixels = new int[steps * HEIGHT];

        for (int x = 0; x < steps; x++) {
            double t = steps > 1 ? (double) x / (steps - 1) : 0.0;

            int pixel;

            try {
                ColorValue lerped = c1.lerp(c2, t);
                pixel = lerped.toAWT().getRGB();
            } catch (Exception e) {
                String context = String.format("t=%.4f", t);
                log.log(context, e);
                pixel = 0xFFFF00FF;
            }

            for (int y = 0; y < HEIGHT; y++) {
                pixels[y * steps + x] = pixel;
            }
        }

        BufferedImage image = new BufferedImage(steps, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, steps, HEIGHT, pixels, 0, steps);

        File outputFile = new File(outputDir, "lerp_" + name + ".png");
        ImageIO.write(image, "PNG", outputFile);

        ColorTestUtil.printResult(outputFile.getName(), System.currentTimeMillis() - start, log);
    }
}