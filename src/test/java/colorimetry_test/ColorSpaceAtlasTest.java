package colorimetry_test;

import colorimetry.*;
import colorimetry_test.utils.BatchRunner;
import colorimetry_test.utils.ErrorLog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Generates a 4096x4096 atlas PNG per registered color space.
 *
 * Layout: 16x16 grid of 256x256 tiles.
 *   - GRID_CHANNEL varies across tiles (tile 0 = 0.0, tile 255 = 1.0)
 *   - Next channel varies across X (left = 0.0, right = 1.0)
 *   - Remaining channel varies across Y (top = 1.0, bottom = 0.0)
 *
 * All values are generated via ofNormalized (0-1 range) and converted to AWT Color
 * via toAWT().
 *
 * Output: color_tests/color_spaces/atlas_{spaceName}.png
 */
public final class ColorSpaceAtlasTest {
    private static final int TILE_SIZE = 256;
    private static final int GRID_SIZE = 16;
    private static final int ATLAS_SIZE = TILE_SIZE * GRID_SIZE;
    private static final int TOTAL_TILES = GRID_SIZE * GRID_SIZE;

    /** Which channel (0, 1, or 2) varies across the tile grid. */
    private static final int GRID_CHANNEL = 0;

    public static void main(String[] args) throws Exception {
        File outputDir = new File("color_tests/color_spaces");
        outputDir.mkdirs();

        List<ColorSpace> spaces = ColorSpaceRegistry.getSpaces();
        System.out.println("=== Color Space Atlas Generation ===");
        System.out.println("Spaces: " + spaces.size() + " | Grid channel: " + GRID_CHANNEL + " | Atlas: " + ATLAS_SIZE + "x" + ATLAS_SIZE);

        // One int[] pixel buffer per concurrent atlas
        long bytesPerAtlas = (long) ATLAS_SIZE * ATLAS_SIZE * 4;

        BatchRunner.run(spaces, bytesPerAtlas, space -> {
            generateAtlas(space, outputDir);
        });

        System.out.println("Done. Output: " + outputDir.getAbsolutePath());
    }

    /**
     * Generates a single atlas PNG for one color space.
     * Tiles are rendered in parallel to a shared pixel buffer.
     *
     * @param space color space to visualize
     * @param outputDir directory to write the PNG
     * @throws IOException if writing fails
     */
    private static void generateAtlas(ColorSpace space, File outputDir) throws IOException {
        String name = space.displayName().replace(" ", "_");
        long start = System.currentTimeMillis();
        ErrorLog log = new ErrorLog();

        int channels = space.componentCount();

        // Assign remaining channels to X and Y axes based on which one is the grid channel
        int gridCh = GRID_CHANNEL;
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

        // Flat pixel buffer — tiles write to non-overlapping regions
        int[] pixels = new int[ATLAS_SIZE * ATLAS_SIZE];

        // Capture effectively-final channel indices for lambda
        int fXCh = xCh;
        int fYCh = yCh;

        // Level 2: each tile in parallel
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
                    // Y inverted: top = 1.0
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

        // Build image from pixel buffer and write
        BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        atlas.setRGB(0, 0, ATLAS_SIZE, ATLAS_SIZE, pixels, 0, ATLAS_SIZE);

        File outputFile = new File(outputDir, "atlas_" + name + ".png");
        ImageIO.write(atlas, "PNG", outputFile);

        long elapsed = System.currentTimeMillis() - start;
        ColorTestUtil.printResult(outputFile.getName(), elapsed, log);
    }
}