# Architecture

Internal design of the Colorimetry conversion engine.

## Table of contents

- [Conversion pipeline](#conversion-pipeline)
- [Gamut mapping](#gamut-mapping)

## Conversion pipeline

When `color.to(targetSpace)` is called, `ColorConverter` finds the **Lowest Common Ancestor (LCA)** of the two spaces in the hierarchy tree, walks up from the source via `toParent()`, then down to the target via `fromParent()`. Spaces in the same family never touch XYZ:

```
HSB → HSL (same family, LCA = BT.709 RGB Linear)
  HSB ──toParent──▷ BT.709 RGB Linear ──fromParent──▷ HSL

OK Lab → OK LCh (parent to child)
  OK Lab ──fromParent──▷ OK LCh

HSB → OK Lab (cross-family, LCA = CIE XYZ D65)
  HSB ──toParent──▷ BT.709 RGB Linear ──toParent──▷ XYZ D65 ──fromParent──▷ OK Lab
```

## Gamut mapping

Gamut mapping triggers automatically during conversion when the walk-down enters a bounded space (like a linear RGB) from an unbounded parent (like XYZ). A color that exists in OkLab may not be representable in sRGB, so the `GamutMapper` uses binary search between the out-of-gamut point and the space's `neutralXyz()` anchor to find the closest in-gamut color.

This is separate from `ValidationMode`, which catches invalid user input at construction time (e.g. `HSB hue = 400`). Gamut mapping handles colors that are mathematically valid in one space but fall outside the gamut of another.
