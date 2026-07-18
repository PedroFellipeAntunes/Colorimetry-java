# Contributing to Colorimetry

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
```

## Code style

**Formatting:**
- Every brace on its own line, even for single-statement bodies. Never compress to one line.
- One space after commas, no column alignment.
- Blank line between methods.
- Blank line before `return`, `break`, `continue` when there is code above at the same indentation level. Not needed if the statement is the only thing in the block.
- Blank line before any block statement (`if`, `for`, `try`, `switch`, etc.) when there is code above at the same indentation level.

```java
// correct
public double[] toParent(double[] raw) {
    if (raw[0] == 0.0) {
        return new double[] {0.0, 0.0, 0.0};
    }

    double x = compute(raw[0]);
    double y = compute(raw[1]);

    return new double[] {x, y, 0.0};
}

// correct
File outputFile = new File(outputDir, "result.png");

try {
    ImageIO.write(image, "PNG", outputFile);
} catch (IOException e) {
    System.err.println("Failed: " + e.getMessage());
}
```

```java
// wrong — compressed to one line
public double[] toParent(double[] raw) { return compute(raw[0], raw[1], raw[2]); }

// wrong — missing blank line before return
double x = compute(raw[0]);
double y = compute(raw[1]);
return new double[] {x, y, 0.0};

// wrong — missing blank line before try block
File outputFile = new File(outputDir, "result.png");
try {
    ImageIO.write(image, "PNG", outputFile);
} catch (IOException e) {
    System.err.println("Failed: " + e.getMessage());
}
```

**Comments:**
- Every public and private method gets a javadoc header with description, `@param`, `@return`, and `@throws` (if applicable).
- Inline comments on complex logic explaining what it does and why.
- `@Override` methods that inherit from `ColorSpace` or `Grayscale` don't need headers (inherited from the interface), but do need inline comments if the implementation has non-obvious math.

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

1. Create a class in `colorimetry/spaces/` implementing `ColorSpace`.
2. Declare `parentSpace()` — pick the closest existing space in the hierarchy.
3. Implement `toParent()` and `fromParent()` with the conversion math.
4. Implement `normalize()` and `denormalize()` for the 0-1 range mapping.
5. Implement metadata: `displayName`, `componentCount`, `componentName`, `componentMin`, `componentMax`, `componentDefault`, `componentStep`.
6. Root spaces (parent = `Xyz`) must also override `isBounded()`, `isInGamut()`, and `neutralXyz()`. Child spaces inherit these.
7. Register in `ColorSpaceRegistry` static block.
8. Run `ColorSpaceAtlasTest` and verify the atlas visually.

## Adding a grayscale method

1. Create a class in `colorimetry/grayscale/` implementing `Grayscale`.
2. Implement `toGrayXyz()` — receives XYZ, returns achromatic XYZ.
3. Use `ColorConverter.convert()` to go to/from whatever space the method needs.
4. Implement `displayName()`.
5. Register in `GrayscaleRegistry` static block.
6. Run `GrayscaleGradientTest` and verify the gradient visually.

## Pull requests

- One feature per PR.
- Branch from `main`.
- Follow the code style above.
- Run the visual tests for the specific space or method you changed and check the output before submitting:

```bash
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceAtlasTest -Dexec.args="SpaceName"
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleGradientTest -Dexec.args="SpaceName 0 MethodName"
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleAtlasTest -Dexec.args="AtlasName MethodName"
```

- Describe what was changed and why in the PR description.
