package colorimetry_test;

import colorimetry.ColorSpace;
import colorimetry.ColorValue;
import colorimetry.Grayscale;
import colorimetry.GrayscaleRegistry;
import colorimetry.spaces.Hsl;
import colorimetry_test.utils.ErrorLog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Generates grayscale comparison images from a color space gradient.
 *
 * Generates a reference gradient in GRADIENT_SPACE, then applies each registered
 * grayscale method and saves a separate image.
 *
 * Output: color_tests/grayscale/reference_{spaceName}.png
 *         color_tests/grayscale/gray_{methodName}.png
 */
public final class GrayscaleGradientTest {
    /** Color space used for the reference gradient. */
    private static final ColorSpace GRADIENT_SPACE = Hsl.INSTANCE;

    /** Which channel (0, 1, or 2) varies across X. */
    private static final int GRADIENT_CHANNEL = 0;

    private static final int WIDTH = 512;
    private static final int HEIGHT = WIDTH;

    public static void main(String[] args) throws IOException {
        File outputDir = new File("color_tests/grayscale/gradient");
        outputDir.mkdirs();

        int xCh = GRADIENT_CHANNEL;
        int yCh;
        int fixedCh;
        
        switch (xCh) {
            case 0 -> {
                yCh = 2;
                fixedCh = 1;
            }
            case 1 -> {
                yCh = 2;
                fixedCh = 0;
            }
            default -> {
                yCh = 1;
                fixedCh = 0;
            }
        }

        double fixedValue = GRADIENT_SPACE.componentDefault(fixedCh);
        double xMin = GRADIENT_SPACE.componentMin(xCh);
        double xMax = GRADIENT_SPACE.componentMax(xCh);
        double yMin = GRADIENT_SPACE.componentMin(yCh);
        double yMax = GRADIENT_SPACE.componentMax(yCh);

        List<Grayscale> methods = GrayscaleRegistry.getMethods();
        System.out.println("=== Grayscale Gradient Test ===");
        System.out.println("Space: " + GRADIENT_SPACE.displayName() + " | X: " + GRADIENT_SPACE.componentName(xCh, true)
            + " | Y: " + GRADIENT_SPACE.componentName(yCh, true) + " | Fixed: " + GRADIENT_SPACE.componentName(fixedCh, true) + "=" + fixedValue);
        System.out.println("Methods: " + methods.size() + " | Size: " + WIDTH + "x" + HEIGHT + "\n");

        // Generate reference gradient (sequential — methods depend on it)
        int channels = GRADIENT_SPACE.componentCount();
        ColorValue[][] reference = new ColorValue[WIDTH][HEIGHT];
        int[] refPixels = new int[WIDTH * HEIGHT];

        ErrorLog refLog = new ErrorLog();
        long refStart = System.currentTimeMillis();

        for (int x = 0; x < WIDTH; x++) {
            double xValue = xMin + (xMax - xMin) * x / (WIDTH - 1);
            
            for (int y = 0; y < HEIGHT; y++) {
                double yValue = yMax - (yMax - yMin) * y / (HEIGHT - 1);

                double[] raw = new double[channels];
                raw[xCh] = xValue;
                raw[yCh] = yValue;
                raw[fixedCh] = fixedValue;

                try {
                    ColorValue color = ColorValue.of(GRADIENT_SPACE, raw);
                    reference[x][y] = color;
                    refPixels[y * WIDTH + x] = color.toAWT().getRGB();
                } catch (Exception e) {
                    String context = String.format("raw=[%.1f, %.1f, %.1f]", raw[0], raw[1], raw[2]);
                    refLog.log(context, e);
                    refPixels[y * WIDTH + x] = 0xFFFF00FF;
                }
            }
        }

        BufferedImage refImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        refImage.setRGB(0, 0, WIDTH, HEIGHT, refPixels, 0, WIDTH);

        String spaceName = GRADIENT_SPACE.displayName().replace(" ", "_");
        File refFile = new File(outputDir, "reference_" + spaceName + ".png");
        ImageIO.write(refImage, "PNG", refFile);
        ColorTestUtil.printResult(refFile.getName(), System.currentTimeMillis() - refStart, refLog);

        // Apply each grayscale method in parallel
        methods.parallelStream().forEach(method -> {
            long start = System.currentTimeMillis();
            String methodName = ColorTestUtil.sanitizeName(method.displayName());
            ErrorLog log = new ErrorLog();

            int[] pixels = new int[WIDTH * HEIGHT];

            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    ColorValue source = reference[x][y];
                    
                    if (source == null) {
                        pixels[y * WIDTH + x] = 0xFFFF00FF;
                        
                        continue;
                    }
                    
                    String context = String.format("raw=[%.1f, %.1f, %.1f]", source.getRaw()[0], source.getRaw()[1], source.getRaw()[2]);
                    pixels[y * WIDTH + x] = ColorTestUtil.toGrayscalePixel(source, method, log, context);
                }
            }

            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);

            File outputFile = new File(outputDir, "gray_" + methodName + ".png");
            try {
                ImageIO.write(image, "PNG", outputFile);
            } catch (IOException e) {
                System.err.println("Failed to write: " + outputFile.getName() + ": " + e.getMessage());
            }

            ColorTestUtil.printResult(outputFile.getName(), System.currentTimeMillis() - start, log);
        });

        System.out.println("\nDone. Output: " + outputDir.getAbsolutePath());
    }
}