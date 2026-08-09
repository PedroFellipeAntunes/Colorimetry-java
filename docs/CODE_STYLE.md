# Code Style

Formatting rules and conventions for the Colorimetry codebase.

## Table of contents

- [Formatting](#formatting)
- [Section headers](#section-headers)
- [Comments](#comments)

## Formatting

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

## Section headers

Color spaces and other classes use section headers to group related methods:

```java
// ===== METADATA =====
// ===== MATH =====
// ===== PARENT HIERARCHY =====
// ===== COLORSPACE OVERRIDES =====
```

Always leave a blank line before a section header.

## Comments

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
