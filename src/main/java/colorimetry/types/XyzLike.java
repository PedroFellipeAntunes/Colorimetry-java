package colorimetry.types;

import colorimetry.ColorSpace;

/**
 * Interface for adapted XYZ color spaces that carry a reference white point.
 *
 * Implemented by Xyz variants. The {@link #referenceWhite()} method
 * returns the tristimulus values of the adapted illuminant, allowing child
 * spaces like CIE Lab and CIE Luv to extract Xn, Yn, Zn generically
 * without hardcoding a specific illuminant.
 *
 * Used as the parameter type in factory methods like
 * {@code CieLab.of(XyzLike parent)} to enforce compile-time type safety.
 */
public interface XyzLike extends ColorSpace {
    /**
     * Returns the XYZ tristimulus values of the reference white point
     * for this adapted XYZ space.
     *
     * @return array of {Xn, Yn, Zn} tristimulus values
     */
    double[] referenceWhite();
}