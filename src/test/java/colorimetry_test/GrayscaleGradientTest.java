package colorimetry_test;

import colorimetry.*;
import colorimetry.spaces.*;
import colorimetry_test.utils.ErrorLog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Generates grayscale comparison images from a color space gradient.
 *
 * Generates a reference gradient in GRADIENT_SPACE (sequential), then applies
 * each registered grayscale method in parallel and saves a separate image.
 *
 * Output: color_tests/grayscale/reference_{spaceName}.png
 *         color_tests/grayscale/gray_{methodName}.png
 */
public final class GrayscaleGradientTest {
    /** Color space for the reference gradient. Overridden by args[0]. */
    private static final ColorSpace GRADIENT_SPACE = Hsl.INSTANCE;

    /** Which channel (0, 1, or 2) varies across X. Overridden by args[1]. */
    private static final int GRADIENT_CHANNEL = 0;

    /** Optional: filter by method name. Empty = all methods. Overridden by args[2]. */
    private static final String FILTER_METHOD = "";

    private static final int WIDTH = 512;
    private static final int HEIGHT = WIDTH;

    public static void main(String[] args) throws IOException {
        File outputDir = new File("color_tests/grayscale/gradient");
        outputDir.mkdirs();

        // Resolve gradient space: args[0] > GRADIENT_SPACE
        ColorSpace gradientSpace = GRADIENT_SPACE;

        if (args.length > 0) {
            String spaceName = args[0];
            gradientSpace = ColorSpaceRegistry.getSpaces().stream()
                .filter(s -> s.displayName().contains(spaceName))
                .findFirst()
                .orElse(GRADIENT_SPACE);
        }

        // Resolve channel: args[1] > GRADIENT_CHANNEL
        int xCh = args.length > 1 ? Integer.parseInt(args[1]) : GRADIENT_CHANNEL;

        // Resolve method filter: args[2] > FILTER_METHOD > all
        String filterMethod = args.length > 2 ? args[2] : FILTER_METHOD;

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

        double fixedValue = gradientSpace.componentDefault(fixedCh);
        double xMin = gradientSpace.componentMin(xCh);
        double xMax = gradientSpace.componentMax(xCh);
        double yMin = gradientSpace.componentMin(yCh);
        double yMax = gradientSpace.componentMax(yCh);

        List<Grayscale> methods = GrayscaleRegistry.getMethods();

        if (!filterMethod.isEmpty()) {
            methods = methods.stream().filter(m -> m.displayName().contains(filterMethod)).collect(java.util.stream.Collectors.toList());
        }

        if (methods.isEmpty()) {
            System.err.println("No matching grayscale methods found.");

            return;
        }

        System.out.println("=== Grayscale Gradient Test ===");
        System.out.println("Space: " + gradientSpace.displayName() + " | X: " + gradientSpace.componentName(xCh, true)
            + " | Y: " + gradientSpace.componentName(yCh, true) + " | Fixed: " + gradientSpace.componentName(fixedCh, true) + "=" + fixedValue);
        System.out.println("Methods: " + methods.size() + " | Size: " + WIDTH + "x" + HEIGHT + "\n");

        // Generate reference gradient
        int channels = gradientSpace.componentCount();
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
                    ColorValue color = ColorValue.of(gradientSpace, raw);
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

        String spaceName = gradientSpace.displayName().replace(" ", "_");
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