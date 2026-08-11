package colorimetry_test;

import colorimetry_test.utils.ColorTestUtil;
import colorimetry.*;
import colorimetry.registry.ColorSpaceRegistry;

import java.util.List;

/**
 * Prints a distance comparison table for two colors across registered
 * spaces and distance metrics.
 *
 * Each row is a color space, each column is a metric. Both colors are
 * converted to the target space before computing the distance.
 *
 * Output: console only (no files).
 */
public final class DistanceTest {
    /** First color as hex string. Overridden by args[0]. */
    private static final String DEFAULT_COLOR_1 = "#FF0000";

    /** Second color as hex string. Overridden by args[1]. */
    private static final String DEFAULT_COLOR_2 = "#0000FF";

    /** Optional: filter by space name. Empty = all spaces. Overridden by args[2]. */
    private static final String FILTER_SPACE = "";

    /** Optional: filter by metric name. Empty = all metrics. Overridden by args[3]. */
    private static final String FILTER_METRIC = "";

    public static void main(String[] args) {
        String hex1 = args.length > 0 ? args[0] : DEFAULT_COLOR_1;
        String hex2 = args.length > 1 ? args[1] : DEFAULT_COLOR_2;
        String filterSpace = args.length > 2 ? args[2] : FILTER_SPACE;
        String filterMetric = args.length > 3 ? args[3] : FILTER_METRIC;

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

        List<DistanceMetric> metrics = ColorTestUtil.filterMetrics(filterMetric);

        if (metrics.isEmpty()) {
            System.err.println("No matching metrics found.");

            return;
        }

        System.out.println("Distance Comparison");
        System.out.println();
        System.out.println("Color 1: " + hex1 + " | Color 2: " + hex2);
        System.out.println("Spaces: " + spaces.size() + " | Metrics: " + metrics.size());

        for (ColorSpace space : spaces) {
            ColorValue c1 = color1.to(space);
            ColorValue c2 = color2.to(space);

            System.out.println();
            System.out.println(space.displayName() + ":");

            for (DistanceMetric metric : metrics) {
                try {
                    double d = c1.distance(c2, metric);
                    System.out.println("  " + metric.displayName() + ": " + String.format("%.6f", d));
                } catch (Exception e) {
                    System.out.println("  " + metric.displayName() + ": ERROR (" + e.getMessage() + ")");
                }
            }
        }

        System.out.println();
        System.out.println("Done.");
    }
}