package colorimetry_test;

import colorimetry.*;
import colorimetry.registry.ColorSpaceRegistry;
import colorimetry_test.utils.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Generates atlas PNGs for a color space with every valid parent.
 *
 * Uses {@link ColorSpace#acceptedParentType()} to discover valid parents
 * from the registry, then calls the space's {@code of(parent)} factory
 * via reflection to create each variant.
 *
 * Output: color_tests/parent_atlas/atlas_{SpaceName}.png
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass=colorimetry_test.ParentAtlasTest
 *   mvn exec:java -Dexec.mainClass=colorimetry_test.ParentAtlasTest -Dexec.args="HSB 1"
 */
public final class ParentAtlasTest {
    private static final int TILE_SIZE = 256;
    private static final int GRID_SIZE = 16;
    private static final int ATLAS_SIZE = TILE_SIZE * GRID_SIZE;
    private static final int TOTAL_TILES = GRID_SIZE * GRID_SIZE;

    /** Space to test. Empty = requires args[0]. Overridden by args[0]. */
    private static final String FILTER_SPACE = "HCY";

    /** Which channel (0, 1, or 2) varies across the tile grid. Overridden by args[1]. */
    private static final int GRID_CHANNEL = 0;

    public static void main(String[] args) throws Exception {
        File outputDir = new File("color_tests/parent_atlas");
        outputDir.mkdirs();

        String spaceName = args.length > 0 ? args[0] : FILTER_SPACE;
        int gridChannel = args.length > 1 ? Integer.parseInt(args[1]) : GRID_CHANNEL;

        if (spaceName.isEmpty()) {
            System.err.println("Usage: ParentAtlasTest <spaceName> [gridChannel]");
            System.err.println("Example: ParentAtlasTest HSB 0");

            return;
        }

        // Find the matching INSTANCE in the registry
        List<ColorSpace> matches = ColorTestUtil.filterSpaces(spaceName);

        if (matches.isEmpty()) {
            System.err.println("Space not found in registry: " + spaceName);

            return;
        }

        ColorSpace base = matches.get(0);
        Class<? extends ColorSpace> parentType = base.acceptedParentType();

        if (parentType == null) {
            System.err.println(base.displayName() + " has a fixed parent (acceptedParentType is null).");

            return;
        }

        // Find all registered spaces that match the accepted parent type
        List<ColorSpace> validParents = ColorSpaceRegistry.getSpaces().stream()
            .filter(parentType::isInstance)
            .collect(java.util.stream.Collectors.toList());

        if (validParents.isEmpty()) {
            System.err.println("No valid parents found for " + base.displayName() + " (type: " + parentType.getSimpleName() + ")");

            return;
        }

        // Find the of() factory method on the space class
        Method ofMethod = base.getClass().getMethod("of", parentType);

        // Build variants
        List<ColorSpace> variants = new ArrayList<>();

        for (ColorSpace parent : validParents) {
            ColorSpace variant = (ColorSpace) ofMethod.invoke(null, parent);
            variants.add(variant);
        }

        System.out.println("=== Parent Atlas Test ===");
        System.out.println("Space: " + base.displayName() + " | Parent type: " + parentType.getSimpleName() + " | Variants: " + variants.size() + " | Grid channel: " + gridChannel);

        long bytesPerAtlas = (long) ATLAS_SIZE * ATLAS_SIZE * 4;

        BatchRunner.run(variants, bytesPerAtlas, space -> {
            generateAtlas(space, outputDir, gridChannel);
        });

        System.out.println("Done. Output: " + outputDir.getAbsolutePath());
    }

    /**
     * Generates a single atlas PNG for one color space variant.
     *
     * @param space color space to visualize
     * @param outputDir directory to write the PNG
     * @param gridCh which channel varies across the tile grid
     * @throws IOException if writing fails
     */
    private static void generateAtlas(ColorSpace space, File outputDir, int gridCh) throws IOException {
        String name = ColorTestUtil.sanitizeName(space.displayName());
        long start = System.currentTimeMillis();
        ErrorLog log = new ErrorLog();

        int channels = space.componentCount();

        int xCh;
        int yCh;

        switch (gridCh) {
            case 0 -> {
                xCh = 1;
                yCh = 2;
            }
            case 1 -> {
                xCh = 0;
                yCh = 2;
            }
            default -> {
                xCh = 0;
                yCh = 1;
            }
        }

        int[] pixels = new int[ATLAS_SIZE * ATLAS_SIZE];

        int fXCh = xCh;
        int fYCh = yCh;

        IntStream.range(0, TOTAL_TILES).parallel().forEach(tileIndex -> {
            int tileRow = tileIndex / GRID_SIZE;
            int tileCol = tileIndex % GRID_SIZE;
            int startX = tileCol * TILE_SIZE;
            int startY = tileRow * TILE_SIZE;
            double gridNorm = tileIndex / 255.0;

            for (int localY = 0; localY < TILE_SIZE; localY++) {
                int globalY = startY + localY;

                for (int localX = 0; localX < TILE_SIZE; localX++) {
                    int globalX = startX + localX;
                    double xNorm = localX / 255.0;
                    double yNorm = 1.0 - localY / 255.0;

                    double[] normalized = new double[channels];
                    normalized[gridCh] = gridNorm;
                    normalized[fXCh] = xNorm;
                    normalized[fYCh] = yNorm;

                    if (channels >= 4) {
                        normalized[3] = 0.5;
                    }

                    try {
                        ColorValue color = ColorValue.ofNormalized(space, normalized);
                        pixels[globalY * ATLAS_SIZE + globalX] = color.toAWT().getRGB();
                    } catch (Exception e) {
                        String context = String.format("norm=[%.3f, %.3f, %.3f]", normalized[0], normalized[1], normalized[2]);
                        log.log(context, e);
                        pixels[globalY * ATLAS_SIZE + globalX] = 0xFFFF00FF;
                    }
                }
            }
        });

        BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        atlas.setRGB(0, 0, ATLAS_SIZE, ATLAS_SIZE, pixels, 0, ATLAS_SIZE);

        File outputFile = new File(outputDir, "atlas_" + name + ".png");
        ImageIO.write(atlas, "PNG", outputFile);

        long elapsed = System.currentTimeMillis() - start;
        ColorTestUtil.printResult(outputFile.getName(), elapsed, log);
    }
}