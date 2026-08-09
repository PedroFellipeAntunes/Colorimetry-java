package colorimetry.types;

import colorimetry.ColorSpace;

/**
 * Marker interface for color spaces with L/a/b channel structure.
 *
 * Implemented by CIE Lab, OkLab, and JzAzBz. The polar conversion math
 * (C = sqrt(a² + b²), h = atan2(b, a)) is identical for any Lab-like space,
 * so cylindrical children like CIE LCh and OkLCh can accept any LabLike
 * parent through factory methods like {@code CieLch.of(LabLike parent)}.
 */
public interface LabLike extends ColorSpace {
}