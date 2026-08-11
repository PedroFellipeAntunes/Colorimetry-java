package colorimetry_test;

import colorimetry.*;
import colorimetry.registry.ColorSpaceRegistry;
import colorimetry.spaces.xyz.Xyz;

import java.util.*;

/**
 * Prints the parent hierarchy tree of all registered color spaces
 * with metadata flags for each space: bounded, cylindrical, palette.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceTreeTest
 */
public final class ColorSpaceTreeTest {
    /**
     * Builds the parent-children map from the registry and prints the tree
     * starting from the absolute Xyz root.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // Build parent -> children map
        Map<ColorSpace, List<ColorSpace>> children = new LinkedHashMap<>();
        children.put(Xyz.INSTANCE, new ArrayList<>());

        for (ColorSpace space : ColorSpaceRegistry.INSTANCE.getEntries()) {
            children.putIfAbsent(space, new ArrayList<>());
            ColorSpace parent = space.parentSpace();

            if (parent != null) {
                children.computeIfAbsent(parent, k -> new ArrayList<>()).add(space);
            }
        }

        // Print header
        System.out.println("Color Space Tree");
        System.out.println();
        System.out.println(formatNode(Xyz.INSTANCE));
        printChildren(Xyz.INSTANCE, children, "");

        // Summary
        List<ColorSpace> all = ColorSpaceRegistry.INSTANCE.getEntries();
        long bounded = all.stream().filter(ColorSpace::isBounded).count();
        long cylindrical = all.stream().filter(ColorSpace::isCylindrical).count();
        long palette = all.stream().filter(ColorSpace::hasPalette).count();

        System.out.println();
        System.out.println("Total: " + (all.size() + 1) + " spaces (+" + 1 + " root)");
        System.out.println("  Bounded: " + bounded + " | Cylindrical: " + cylindrical + " | Palette: " + palette);
    }

    /**
     * Formats a color space node with its metadata flags.
     *
     * @param space the color space to format
     * @return formatted string with name and flags
     */
    private static String formatNode(ColorSpace space) {
        StringBuilder sb = new StringBuilder(space.displayName());
        List<String> flags = new ArrayList<>();

        if (space.isBounded()) {
            flags.add("bounded");
        }

        if (space.isCylindrical()) {
            flags.add("cylindrical");
        }

        if (space.hasPalette()) {
            flags.add("palette");
        }

        if (!flags.isEmpty()) {
            sb.append("  [").append(String.join(", ", flags)).append("]");
        }

        return sb.toString();
    }

    /**
     * Recursively prints children of a color space with tree-drawing prefixes.
     *
     * @param parent the parent space whose children to print
     * @param children map of parent -> list of child spaces
     * @param prefix indentation prefix for the current depth level
     */
    private static void printChildren(ColorSpace parent, Map<ColorSpace, List<ColorSpace>> children, String prefix) {
        List<ColorSpace> kids = children.getOrDefault(parent, Collections.emptyList());

        for (int i = 0; i < kids.size(); i++) {
            ColorSpace child = kids.get(i);
            boolean last = (i == kids.size() - 1);

            System.out.println(prefix + (last ? "└── " : "├── ") + formatNode(child));

            String nextPrefix = prefix + (last ? "    " : "│   ");
            printChildren(child, children, nextPrefix);
        }
    }
}