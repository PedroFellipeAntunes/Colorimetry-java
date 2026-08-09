package colorimetry_test;

import colorimetry.*;
import colorimetry.spaces.rgb.SRgb;
import colorimetry_test.utils.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Iterates the sRGB cube in normalized steps, converts each sample to a
 * target color space, and writes a CSV with the normalized coordinates.
 *
 * First line is a metadata comment:
 *   # spaceName;ch0;ch1;ch2[;cylindrical;hueChannel;radialChannel]
 *
 * Output columns: x;y;z;r;g;b
 *   - x, y, z: normalized [0,1] coordinates in the target space
 *   - r, g, b: original sRGB values (0-255)
 *
 * Parallelism:
 *   Level 1: BatchRunner across multiple color spaces, batched by heap.
 *   Level 2: IntStream.parallel() for the RGB cube conversion within each space.
 *   Results are stored in a pre-allocated double[] (non-overlapping regions),
 *   then written sequentially.
 *
 * Output: color_tests/gamut_export/gamut_{spaceName}.csv
 *
 * Usage: java ColorSpaceGamutExport [spaceName] [step] [volume]
 */
public final class ColorSpaceGamutExport {
    private static final String DEFAULT_SPACE = "";
    private static final int DEFAULT_STEP = 1;
    private static final boolean DEFAULT_VOLUME = true;

    public static void main(String[] args) throws Exception {
        String filterSpace = args.length > 0 ? args[0] : DEFAULT_SPACE;
        int step = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_STEP;
        boolean volume = args.length > 2 ? Boolean.parseBoolean(args[2]) : DEFAULT_VOLUME;

        List<ColorSpace> spaces = ColorTestUtil.filterSpaces(filterSpace);

        if (spaces.isEmpty()) {
            System.err.println("No color space found matching: " + filterSpace);

            return;
        }

        File outputDir = new File("color_tests/gamut_export");
        outputDir.mkdirs();

        String mode = volume ? "Volume" : "Surface";
        System.out.println("=== Gamut Export ===");
        System.out.println("Spaces: " + spaces.size() + " | Step: " + step + " | Mode: " + mode + "\n");

        // Estimate peak memory per concurrent space export:
        // double[total * 3] for XYZ results + boolean[total] for valid mask
        int maxVal = (255 / step) * step;
        int nPerCh = maxVal / step + 1;
        long total = (long) nPerCh * nPerCh * nPerCh;
        long bytesPerTask = total * 3 * 8 + total;

        BatchRunner.run(spaces, bytesPerTask, space -> {
            exportSpace(space, outputDir, step, volume);
        });

        System.out.println("Done. Output: " + outputDir.getAbsolutePath());
    }

    /**
     * Exports a single color space gamut to CSV.
     *
     * Converts the entire RGB cube (or surface) in parallel using IntStream,
     * storing results in a flat double[] where each thread writes to its own
     * non-overlapping region (indexed by linear sample index). After all
     * conversions complete, writes the CSV sequentially.
     *
     * @param space target color space
     * @param outputDir directory to write the CSV
     * @param step RGB sampling step
     * @param volume if true, samples the entire cube; if false, only the 6 faces
     * @throws IOException if writing fails
     */
    private static void exportSpace(ColorSpace space, File outputDir, int step, boolean volume) throws IOException {
        long start = System.currentTimeMillis();
        String spaceName = ColorTestUtil.sanitizeName(space.displayName());
        File outputFile = new File(outputDir, "gamut_" + spaceName + ".csv");
        ErrorLog log = new ErrorLog();

        // Metadata header: name;ch0;ch1;ch2[;cylindrical;hueChannel;radialChannel]
        StringBuilder meta = new StringBuilder("# " + space.displayName()
            + ";" + space.componentName(0, false)
            + ";" + space.componentName(1, false)
            + ";" + space.componentName(2, false));

        if (space.isCylindrical()) {
            meta.append(";cylindrical;").append(space.hueChannel()).append(";").append(space.radialChannel());
        }

        int maxVal = (255 / step) * step;
        int nPerCh = maxVal / step + 1;
        int total = nPerCh * nPerCh * nPerCh;

        // Pre-allocate arrays for parallel computation.
        // Each linear index maps to a unique (ri, gi, bi) triple,
        // so threads write to non-overlapping positions without locks.
        double[] xyz = new double[total * 3];
        boolean[] valid = new boolean[total];

        int nGxB = nPerCh * nPerCh;

        // Parallel color space conversion across the full RGB cube
        IntStream.range(0, total).parallel().forEach(idx -> {
            int ri = (idx / nGxB) * step;
            int gi = ((idx / nPerCh) % nPerCh) * step;
            int bi = (idx % nPerCh) * step;

            // Surface mode: skip interior points (no channel at min or max)
            if (!volume && ri != 0 && ri != maxVal && gi != 0 && gi != maxVal && bi != 0 && bi != maxVal) {
                return;
            }

            try {
                double[] norm = {ri / 255.0, gi / 255.0, bi / 255.0};

                ColorValue srgb = ColorValue.ofNormalized(SRgb.INSTANCE, norm);
                ColorValue target = srgb.to(space);
                double[] out = target.getNormalized();

                int base = idx * 3;
                xyz[base] = out[0];
                xyz[base + 1] = out[1];
                xyz[base + 2] = out[2];
                valid[idx] = true;
            }
            catch (Exception e) {
                String context = String.format("rgb=(%d,%d,%d)", ri, gi, bi);
                log.log(context, e);
            }
        });

        // Sequential write from the pre-computed arrays
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile), 1 << 20)) {
            writer.write(meta.toString());
            writer.newLine();

            writer.write("x;y;z;r;g;b");
            writer.newLine();

            for (int idx = 0; idx < total; idx++) {
                if (!valid[idx]) {
                    continue;
                }

                int ri = (idx / nGxB) * step;
                int gi = ((idx / nPerCh) % nPerCh) * step;
                int bi = (idx % nPerCh) * step;

                int base = idx * 3;

                writer.write(String.format(java.util.Locale.US, "%.6f;%.6f;%.6f;%d;%d;%d", xyz[base], xyz[base + 1], xyz[base + 2], ri, gi, bi));
                writer.newLine();
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        ColorTestUtil.printResult(outputFile.getName(), elapsed, log);
    }
}