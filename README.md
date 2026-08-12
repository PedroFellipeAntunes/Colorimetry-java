# Colorimetry

<p align="center">
  <img src="/images/logo.jpg" alt="Colorimetry Logo" />
</p>

<p align="center">
  <a href="https://jitpack.io/#PedroFellipeAntunes/Colorimetry-java">
    <img src="https://jitpack.io/v/PedroFellipeAntunes/Colorimetry-java.svg" alt="JitPack" />
  </a>
</p>

A Java library for color space conversion and grayscale transformation. Any color in any space can be converted to any other through a parent hierarchy rooted at CIE XYZ, and any color can be desaturated using many different grayscale methods.

## Table of contents

- [Installation](#installation)
- [Usage](#usage)
  - [Validation](#validation)
- [Color conversion](#color-conversion)
  - [Hierarchy](#hierarchy)
  - [Configurable parents](#configurable-parents)
  - [Creating a color space](#creating-a-color-space)
- [Grayscale](#grayscale)
  - [Creating a grayscale method](#creating-a-grayscale-method)
- [Reference](#reference)

## Installation

### JitPack

Add the JitPack repository and the dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.PedroFellipeAntunes</groupId>
    <artifactId>Colorimetry-java</artifactId>
    <version>LATEST</version>
</dependency>
```

### Manual

Clone the repository and install locally:

```bash
git clone https://github.com/PedroFellipeAntunes/Colorimetry-java.git
cd Colorimetry-java
mvn install
```

Requires Java 11+.

## Usage

```java
import colorimetry.ColorValue;
import colorimetry.ValidationMode;
import colorimetry.spaces.hue.Hsb;
import colorimetry.spaces.cie.CieLab;
import colorimetry.spaces.perceptual.Oklab;
import colorimetry.grayscale.luma.Bt709Luma;

// Create a color in any space using its native values
ColorValue sky = ColorValue.of(Hsb.INSTANCE, 210, 80, 90);

// Convert to any other space - the result uses that space's native values
ColorValue lab = sky.to(CieLab.INSTANCE);
double lightness = lab.get(0); // L* in [0, 100]
double greenRed = lab.get(1); // a* in [-128, 127]

// Or create with normalized 0-1 values without knowing each space's ranges
ColorValue color = ColorValue.ofNormalized(Oklab.INSTANCE, 0.7, 0.5, 0.3);

// Alpha support - carried through conversions unchanged
ColorValue semi = ColorValue.of(Hsb.INSTANCE, new double[]{210, 80, 90}, 0.5);
double alpha = semi.alpha(); // 0.5

// Hex - always works, from any space
String hex = sky.toHex(); // "#2E8BE6"
ColorValue fromHex = sky.fromHex("#FF6600"); // stays in HSB
ColorValue parsed = ColorValue.parseHex("#FF6600"); // returns sRGB

// java.awt.Color bridge
java.awt.Color awt = sky.toAWT();
ColorValue back = ColorValue.fromAWT(awt);

// Grayscale - result stays in the same space
ColorValue gray = sky.toGrayscale(Bt709Luma.INSTANCE);
```

`ColorValue` stores colors in the native units of their space. HSB stores hue in degrees (0-360), saturation and brightness in percent (0-100). sRGB stores channels as integers (0-255). CIE Lab stores L\* in 0-100 and a\*, b\* in -128 to 127. The `normalize` / `denormalize` methods provide a uniform 0-1 range when native units aren't needed.

### Validation

`ValidationMode` controls how the library handles invalid or incompatible input, such as out-of-range values on bounded channels or operations between colors in different spaces. By default, issues pass through silently, but you can change that:

```java
// Resolve issues silently (clamps values, converts spaces, etc.)
ColorValue.setValidationMode(ValidationMode.RESOLVE);

// Or throw an exception on invalid input
ColorValue.setValidationMode(ValidationMode.ERROR);

// Disable (default)
ColorValue.setValidationMode(ValidationMode.NONE);
```

Unbounded channels (like CIE Lab a\*/b\*) are never affected regardless of mode.

## Color conversion

Conversions are automatic. Call `.to(targetSpace)` and the library handles the rest, walking the hierarchy tree and applying gamut mapping when needed:

```java
ColorValue sky = ColorValue.of(Hsb.INSTANCE, 210, 80, 90);
ColorValue lab = sky.to(CieLab.INSTANCE);
ColorValue ok = lab.to(Oklab.INSTANCE);
```

### Hierarchy

All color spaces form a tree rooted at `Xyz` (CIE XYZ with Illuminant E). Each space declares its parent and implements `toParent()` / `fromParent()` to convert between its own raw values and the parent's raw values. Adding a new space requires only these two methods. It automatically works with every other space in the library.

```
CIE XYZ (Illuminant E, root)
├── CIE XYZ D65
│   ├── xyY
│   ├── BT.709 RGB Linear  [bounded]
│   │   ├── sRGB  [bounded]
│   │   │   └── RYB  [bounded]
│   │   ├── CMY  [bounded]
│   │   │   └── CMYK  [bounded]
│   │   ├── HSB  [bounded, cylindrical, palette]
│   │   ├── HSL  [bounded, cylindrical, palette]
│   │   ├── HSI  [bounded, cylindrical, palette]
│   │   ├── HCB  [bounded, cylindrical, palette]
│   │   ├── HCL  [bounded, cylindrical, palette]
│   │   ├── HWB  [bounded, cylindrical, palette]
│   │   └── HSP  [bounded, cylindrical, palette]
│   ├── BT.601 RGB Linear  [bounded]
│   │   └── HCY  [bounded, cylindrical, palette]
│   ├── BT.2020 RGB Linear  [bounded]
│   │   ├── Rec. 2020  [bounded]
│   │   └── ICtCp
│   ├── Display P3 Linear  [bounded]
│   │   └── Display P3  [bounded]
│   ├── Adobe RGB Linear  [bounded]
│   │   └── Adobe RGB  [bounded]
│   ├── CIE Luv
│   │   └── CIE LCHuv  [cylindrical, palette]
│   │       ├── HSLuv  [cylindrical, palette]
│   │       └── HPLuv  [cylindrical, palette]
│   ├── LMS (HPE)
│   │   ├── IPT
│   │   └── IgPgTg
│   ├── OK Lab
│   │   ├── OK LCh  [cylindrical, palette]
│   │   ├── Ok HSL  [cylindrical, palette]
│   │   └── Ok HSV  [cylindrical, palette]
│   ├── JzAzBz
│   │   └── JzCzHz  [cylindrical, palette]
│   ├── XYB
│   ├── CAM16  [cylindrical, palette]
│   │   └── CAM16 UCS
│   ├── CAM02  [cylindrical, palette]
│   │   └── CAM02 UCS
│   ├── HCT  [cylindrical, palette]
│   └── ZCAM  [cylindrical, palette]
├── CIE XYZ D50
│   ├── ProPhoto RGB Linear  [bounded]
│   │   └── ProPhoto RGB  [bounded]
│   └── CIE Lab
│       ├── CIE LCh  [cylindrical, palette]
│       └── MSH  [cylindrical, palette]
└── CIE XYZ D60
    ├── ACES 2065-1
    └── ACEScg
```

### Configurable parents

Many spaces accept different parents via a static `of()` factory. This lets you attach the same model to different RGB primaries, XYZ illuminants, or Lab definitions:

```java
import colorimetry.spaces.hue.Hsb;
import colorimetry.spaces.rgb.Bt2020RgbLinear;

// HSB using BT.2020 primaries instead of the default BT.709
ColorSpace hsb2020 = Hsb.of(Bt2020RgbLinear.INSTANCE);
ColorValue color = ColorValue.of(hsb2020, 120, 80, 90);
```

The accepted parent type is enforced at compile time via marker interfaces (`RgbLike`, `XyzLike`, `LabLike`).

### Creating a color space

Implement `ColorSpace`, declare a parent, and register it:

```java
public final class MySpace implements ColorSpace {
    public static final MySpace INSTANCE = new MySpace();

    // ===== METADATA =====
    // displayName, componentCount, componentName, min, max, default, step, ...

    // ===== MATH =====
    // Pure conversion functions

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Bt709RgbLinear.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // Convert this space's raw values to parent's raw values
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Convert parent's raw values to this space's raw values
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        // Map raw values to [0, 1] (user convenience)
    }

    @Override
    public double[] denormalize(double[] normalized) {
        // Map [0, 1] back to raw values
    }
}

ColorSpaceRegistry.register(MySpace.INSTANCE);
```

Root spaces (those whose parent is `Xyz`) must also override `isBounded()`, `isInGamut()`, and `neutralXyz()`. Child spaces inherit these from their root ancestor.

For cylindrical spaces, override `isCylindrical()` (return `true`), `hueChannel()`, and `radialChannel()`.

For spaces where only some channels are bounded, override `isChannelBounded(int i)`.

For spaces with a configurable parent, implement an `of(parent)` factory, `acceptedParentType()`, and have `INSTANCE` use a sensible default parent. The accepted parent type should be one of the marker interfaces: `RgbLike`, `XyzLike`, or `LabLike`.

## Grayscale

Each grayscale method receives a CIE XYZ triplet and returns an achromatic (gray) XYZ triplet. The method converts internally to whatever color space it needs, computes the gray value, and converts back to XYZ. The result stays in the original color space:

```
ColorValue ──convert──▷ XYZ ──toGrayXyz──▷ Gray XYZ ──convert──▷ Gray ColorValue
```

Each method uses the color space that natively represents its concept. BT.709 Luma uses sRGB (weighted average of gamma-encoded channels), Relative Luminance uses linear RGB (linearized channels), CIE L\* Lightness uses CIE Lab (extracts L\*), HSP Perceived Brightness uses HSP (extracts P).

### Creating a grayscale method

Implement `Grayscale` and register it:

```java
public final class MyGrayscale implements Grayscale {
    public static final MyGrayscale INSTANCE = new MyGrayscale();

    public String displayName() {
        return "My Method";
    }

    // Receive XYZ, return achromatic XYZ
    public double[] toGrayXyz(double[] xyz) {
        // Convert to whatever space you need via ColorConverter, compute, convert back
    }
}

GrayscaleRegistry.register(MyGrayscale.INSTANCE);
```

## Reference

- [Color spaces](docs/COLOR_SPACES.md) - full catalog with channels, ranges, and sources for all spaces.
- [Grayscale methods](docs/GRAYSCALE_METHODS.md) - formulas and sources for all methods.
- [Contributing](CONTRIBUTING.md) - how to add new spaces, methods, and submit PRs.
- [Code style](docs/CODE_STYLE.md) - formatting rules and comment conventions.
- [Architecture](docs/ARCHITECTURE.md) - conversion pipeline and gamut mapping internals.
- [Testing](docs/TESTING.md) - all test programs, arguments, and usage examples.
