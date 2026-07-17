package colorimetry_test;

import colorimetry.ColorValue;
import colorimetry.Grayscale;
import colorimetry.GrayscaleRegistry;
import colorimetry_test.utils.BatchRunner;
import colorimetry_test.utils.ErrorLog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Applies all registered grayscale methods to each atlas image found in
 * color_tests/color_spaces/.
 *
 * For each atlas image, creates a folder in color_tests/grayscale/ named after
 * the atlas (without extension), containing one grayscale image per method.
 *
 * Output: color_tests/grayscale/{atlas_name}/gray_{methodName}.png
 *
 * Requires: ColorSpaceAtlasTest to have been run first.
 */
public final class GrayscaleAtlasTest {
    public static void main(String[] args) throws Exception {
        File atlasDir = new File("color_tests/color_spaces");
        
        if (!atlasDir.isDirectory()) {
            System.err.println("Atlas directory not found: " + atlasDir.getAbsolutePath());
            System.err.println("Run ColorSpaceAtlasTest first.");
            
            return;
        }

        File[] atlasFiles = atlasDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (atlasFiles == null || atlasFiles.length == 0) {
            System.err.println("No atlas images found in " + atlasDir.getAbsolutePath());
            
            return;
        }

        List<Grayscale> methods = GrayscaleRegistry.getMethods();
        int workers = Runtime.getRuntime().availableProcessors();
        int concurrentMethods = Math.min(workers, methods.size());

        System.out.println("=== Grayscale Atlas Test ===");
        System.out.println("Atlases: " + atlasFiles.length + " | Methods: " + methods.size());

        // Read dimensions from first atlas to estimate memory cost
        BufferedImage sample = ImageIO.read(atlasFiles[0]);
        long pixelCount = (long) sample.getWidth() * sample.getHeight();
        sample = null;

        // Per atlas: 1 source int[] + concurrent method output int[] arrays
        long bytesPerAtlas = pixelCount * 4 * (1 + concurrentMethods);

        BatchRunner.run(Arrays.asList(atlasFiles), bytesPerAtlas, atlasFile -> {
            processAtlas(atlasFile, methods);
        });

        System.out.println("Done.");
    }

    /**
     * Loads one atlas image as a flat pixel array, then applies every grayscale
     * method in parallel. Each method converts pixels on-the-fly from the shared
     * source array.
     *
     * @param atlasFile source atlas PNG
     * @param methods grayscale methods to apply
     * @throws IOException if reading the atlas fails
     */
    private static void processAtlas(File atlasFile, List<Grayscale> methods) throws IOException {
        String atlasName = atlasFile.getName().replace(".png", "");
        System.out.println("--- " + atlasName + " ---");

        BufferedImage atlas = ImageIO.read(atlasFile);
        int width = atlas.getWidth();
        int height = atlas.getHeight();

        // Read source pixels once as flat int[] (shared, read-only by all methods)
        int[] sourcePixels = atlas.getRGB(0, 0, width, height, null, 0, width);

        // Free the BufferedImage — only the int[] is needed from here
        atlas = null;

        File outputDir = new File("color_tests/grayscale/" + atlasName);
        outputDir.mkdirs();

        // Level 2: methods in parallel, each converts pixels on-the-fly
        methods.parallelStream().forEach(method -> {
            long start = System.currentTimeMillis();
            String methodName = ColorTestUtil.sanitizeName(method.displayName());
            ErrorLog log = new ErrorLog();

            int[] outPixels = new int[width * height];

            for (int i = 0; i < sourcePixels.length; i++) {
                try {
                    ColorValue source = ColorTestUtil.pixelToColor(sourcePixels[i]);
                    ColorValue gray = source.toGrayscale(method);
                    outPixels[i] = gray.toAWT().getRGB();
                } catch (Exception e) {
                    int x = i % width;
                    int y = i / width;
                    log.log("pixel[" + x + "," + y + "]", e);
                    outPixels[i] = 0xFFFF00FF;
                }
            }

            BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            grayImage.setRGB(0, 0, width, height, outPixels, 0, width);

            File outputFile = new File(outputDir, "gray_" + methodName + ".png");
            try {
                ImageIO.write(grayImage, "PNG", outputFile);
            } catch (IOException e) {
                System.err.println("Failed to write: " + outputFile.getName() + ": " + e.getMessage());
            }

            ColorTestUtil.printResult("  " + outputFile.getName(), System.currentTimeMillis() - start, log);
        });
    }
}