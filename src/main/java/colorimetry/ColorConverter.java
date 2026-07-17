package colorimetry;

import colorimetry.spaces.Xyz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Color space conversion engine. Walks the parent hierarchy to find the
 * Lowest Common Ancestor (LCA) between two spaces. Since all spaces descend from
 * Xyz, every pair always has an LCA.
 *
 * Gamut mapping triggers when the walk-down enters a bounded space directly from
 * Xyz (the only unbounded→bounded boundary).
 */
public final class ColorConverter {
    private ColorConverter() {}

    /**
     * Converts raw values from one color space to another.
     *
     * @param source origin color space
     * @param target destination color space
     * @param raw values in source's native units
     * @return values in target's native units
     */
    public static double[] convert(ColorSpace source, ColorSpace target, double[] raw) {
        if (source == target) {
            return raw.clone();
        }

        List<ColorSpace> sourceChain = buildChain(source);
        List<ColorSpace> targetChain = buildChain(target);

        // Find LCA: first element in targetChain that also appears in sourceChain
        Set<ColorSpace> sourceSet = new HashSet<>(sourceChain);
        ColorSpace lca = null;
        
        for (ColorSpace cs : targetChain) {
            if (sourceSet.contains(cs)) {
                lca = cs;
                
                break;
            }
        }

        double[] current = raw.clone();

        // Walk source up to LCA via toParent()
        for (ColorSpace cs : sourceChain) {
            if (cs == lca) {
                break;
            }
            
            current = cs.toParent(current);
        }

        // Walk LCA down to target via fromParent()
        int lcaIndex = targetChain.indexOf(lca);
        
        for (int i = lcaIndex - 1; i >= 0; i--) {
            ColorSpace child = targetChain.get(i);
            ColorSpace parent = targetChain.get(i + 1);

            // Gamut map when entering a bounded space from Xyz
            if (parent == Xyz.INSTANCE && child.isBounded() && !child.isInGamut(current)) {
                current = GamutMapper.map(current, child);
            }

            current = child.fromParent(current);
        }

        return current;
    }

    /**
     * Builds the ancestor chain from a space up to its root.
     * e.g. HSB → Rgb → Xyz yields [HSB, Rgb, Xyz].
     *
     * @param cs starting color space
     * @return list from cs to root (inclusive)
     */
    private static List<ColorSpace> buildChain(ColorSpace cs) {
        List<ColorSpace> chain = new ArrayList<>();
        
        while (cs != null) {
            chain.add(cs);
            cs = cs.parentSpace();
        }
        
        return chain;
    }
}