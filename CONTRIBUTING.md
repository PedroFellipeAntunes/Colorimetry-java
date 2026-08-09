# Contributing to Colorimetry

## Table of contents

- [Reporting bugs](#reporting-bugs)
- [Suggesting features](#suggesting-features)
- [Setup](#setup)
- [Code style](#code-style)
- [Adding a color space](#adding-a-color-space)
- [Adding a grayscale method](#adding-a-grayscale-method)
- [Pull requests](#pull-requests)

## Reporting bugs

Open an issue with:
- Java version and OS
- Code snippet that reproduces the problem
- Expected vs actual result
- If it's a color accuracy issue, include the input values, target space, and what the correct output should be (with source/reference)

## Suggesting features

Open an issue describing the use case before writing code. For new color spaces, include a link to the reference paper or specification.

## Setup

```bash
git clone https://github.com/PedroFellipeAntunes/Colorimetry-java.git
cd Colorimetry-java
mvn compile
```

Requires Java 14+.

Visual tests (generates PNGs in `color_tests/`, gitignored):

```bash
# All spaces, channel 0
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceAtlasTest

# Specific space and channel
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceAtlasTest -Dexec.args="HSB 1"

# All methods, default gradient
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleGradientTest

# Specific gradient space, channel, and method
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleGradientTest -Dexec.args="HSB 0 BT.709"

# All atlases, all methods
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleAtlasTest

# Specific atlas and method
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleAtlasTest -Dexec.args="sRGB BT.709"

# Print the full parent hierarchy tree with metadata flags
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceTreeTest

# Generate atlas for each valid parent of a space
mvn exec:java -Dexec.mainClass=colorimetry_test.ParentAtlasTest

# Export gamut CSVs for 3D visualization
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceGamutExport
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceGamutExport -Dexec.args="OKLab 0.01 true"
```

## Code style

**Formatting:**
- Opening brace on the same line as the declaration (K&R style). Never compress a method or block body to one line. Even single-statement bodies use braces and a separate line.
- One space after commas, no column alignment. Exception: numeric matrix constants may use column alignment for readability.
- Blank line between methods.
- Blank line before `return`, `break`, `continue` when there is code above at the same indentation level. Not needed if the statement is the only thing in the block.
- Blank line before any block statement (`if`, `for`, `try`, `switch`, etc.) when there is code above at the same indentation level.
- 4 spaces for indentation, never tabs.

```java
// correct - K&R braces, body on its own line
public double[] toParent(double[] raw) {
    if (raw[0] == 0.0) {
        return new double[] {0.0, 0.0, 0.0};
    }

    double x = compute(raw[0]);
    double y = compute(raw[1]);

    return new double[] {x, y, 0.0};
}

// correct - blank line before try block
File outputFile = new File(outputDir, "result.png");

try {
    ImageIO.write(image, "PNG", outputFile);
} catch (IOException e) {
    System.err.println("Failed: " + e.getMessage());
}
```

```java
// wrong - compressed to one line
public double[] toParent(double[] raw) { return compute(raw[0], raw[1], raw[2]); }

// wrong - missing blank line before return
double x = compute(raw[0]);
double y = compute(raw[1]);
return new double[] {x, y, 0.0};

// wrong - missing blank line before try block
File outputFile = new File(outputDir, "result.png");
try {
    ImageIO.write(image, "PNG", outputFile);
} catch (IOException e) {
    System.err.println("Failed: " + e.getMessage());
}
```

**Section headers:**

Color spaces and other classes use section headers to group related methods:

```java
// ===== METADATA =====
// ===== MATH =====
// ===== PARENT HIERARCHY =====
// ===== COLORSPACE OVERRIDES =====
```

Always leave a blank line before a section header.

**Comments:**
- Every class gets a Javadoc header describing its purpose. For color spaces, include the reference paper or specification as a `Source:` line.
- Core classes (`ColorSpace`, `ColorValue`, engine classes) have full Javadoc on all public methods with `@param`, `@return`, and `@throws`.
- Color spaces: Javadoc on math methods and factories. Trivial metadata overrides (`componentMin`, `componentMax`, etc.) inherited from `ColorSpace` don't need Javadoc.
- Inline comments on complex logic explaining what it does and why, placed on the line above the code.

```java
/**
 * Computes the chromaticity projection of XYZ onto the u'v' plane.
 *
 * @param X X tristimulus value
 * @param Y Y tristimulus value
 * @param Z Z tristimulus value
 * @return u'v' chromaticity pair
 */
private static double[] chromaticity(double X, double Y, double Z) {
    // Denominator for u'v' projection
    double denom = X + 15.0 * Y + 3.0 * Z;
    ...
}
```

## Adding a color space

1. Create a class in the appropriate subpackage under `colorimetry/spaces/` (e.g. `spaces/hue/`, `spaces/perceptual/`, `spaces/cam/`). Implement `ColorSpace`.
2. Add a `public static final INSTANCE` singleton as the first field.
3. Organize the class with section headers: `// ===== METADATA =====`, `// ===== MATH =====`, `// ===== PARENT HIERARCHY =====`, `// ===== COLORSPACE OVERRIDES =====`.
4. Implement metadata: `displayName`, `componentCount`, `componentName`, `componentMin`, `componentMax`, `componentDefault`, `componentStep`.
5. Declare `parentSpace()` - pick the closest existing space in the hierarchy.
6. Implement `toParent()` and `fromParent()` with the conversion math.
7. Implement `normalize()` and `denormalize()` for the 0-1 range mapping.
8. Root spaces (parent = `Xyz`) must also override `isBounded()`, `isInGamut()`, and `neutralXyz()`. Child spaces inherit these from their root ancestor.
9. For cylindrical spaces, override `isCylindrical()` (return `true`), `hueChannel()`, and `radialChannel()`.
10. For spaces where only some channels are bounded, override `isChannelBounded(int i)`.
11. For spaces with a configurable parent, implement a static `of(parent)` factory and override `acceptedParentType()` returning the appropriate marker interface (`RgbLike.class`, `XyzLike.class`, or `LabLike.class`).
12. Register in `ColorSpaceRegistry` static block.
13. Run `ColorSpaceAtlasTest` and `ColorSpaceTreeTest` to verify the atlas visually and confirm the hierarchy is correct.
14. Run `ColorSpaceGamutExport` for the new space and check that all normalized values in the CSV fall within [0, 1]. Values outside this range indicate incorrect `normalize`/`denormalize` or wrong `componentMin`/`componentMax` bounds. Optionally, use the Python visualization script (`plot_gamut_alpha.py`) to inspect the exported gamut in 3D.

## Adding a grayscale method

1. Create a class in the appropriate subpackage under `colorimetry/grayscale/` (e.g. `grayscale/luma/`, `grayscale/perceptual/`, `grayscale/simple/`, `grayscale/channel/`). Implement `Grayscale`.
2. Add a `public static final INSTANCE` singleton.
3. Implement `displayName()`.
4. Implement `toGrayXyz()` - receives XYZ, returns achromatic XYZ. Use `ColorConverter.convert()` to go to/from whatever space the method needs.
5. Register in `GrayscaleRegistry` static block.
6. Run `GrayscaleGradientTest` and `GrayscaleAtlasTest` to verify the result visually.

## Pull requests

- One feature per PR.
- Branch from `main`.
- Follow the code style above.
- Run the relevant tests for what you changed (see [Adding a color space](#adding-a-color-space) or [Adding a grayscale method](#adding-a-grayscale-method)) and check the output before submitting.
- Describe what was changed and why in the PR description.
