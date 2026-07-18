# Colorimetry

<p align="center">
  <img src="/images/logo.jpg" alt="Colorimetry Logo" />
</p>

<p align="center">
  <a href="https://jitpack.io/#PedroFellipeAntunes/Colorimetry-java">
    <img src="https://jitpack.io/v/PedroFellipeAntunes/Colorimetry-java.svg" alt="JitPack" />
  </a>
</p>

A Java library for color space conversion and grayscale transformation. Any color in any space can be converted to any other space through a parent hierarchy rooted at CIE XYZ D65, and any color can be desaturated using different grayscale methods.

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
    <version>v1.0.0</version>
</dependency>
```

### Manual

Clone the repository and install locally:

```bash
git clone https://github.com/PedroFellipeAntunes/Colorimetry-java.git
cd Colorimetry-java
mvn install
```

## Usage

```java
import colorimetry.ColorValue;
import colorimetry.spaces.*;
import colorimetry.grayscale.*;

// Create a color in any space using its native values
ColorValue sky = ColorValue.of(Hsb.INSTANCE, 210, 80, 90);

// Convert to any other space — the result uses that space's native values
ColorValue lab = sky.to(CieLab.INSTANCE);
double lightness = lab.get(0); // L* in [0, 100]
double greenRed  = lab.get(1); // a* in [-128, 127]

// Or create with normalized 0-1 values without knowing each space's ranges
ColorValue color = ColorValue.ofNormalized(Oklab.INSTANCE, 0.7, 0.5, 0.3);

// Hex — always works, from any space
String hex = sky.toHex(); // "#2E8BE6"
ColorValue fromHex = sky.fromHex("#FF6600"); // stays in HSB
ColorValue parsed = ColorValue.parseHex("#FF6600"); // returns sRGB

// java.awt.Color bridge
java.awt.Color awt = sky.toAWT();
ColorValue back = ColorValue.fromAWT(awt);

// Grayscale — result stays in the same space
ColorValue gray = sky.toGrayscale(Bt709Luma.INSTANCE);
```

`ColorValue` stores colors in the native units of their space. HSB stores hue in degrees (0–360), saturation and brightness in percent (0–100). sRGB stores channels as integers (0–255). CIE Lab stores L\* in 0–100 and a\*, b\* in -128 to 127. The `normalize` / `denormalize` methods provide a uniform 0–1 range when native units aren't needed.

## Color conversion

### Hierarchy

All color spaces form a tree rooted at `Xyz`. Each space declares its parent and implements `toParent()` / `fromParent()` to convert between its own raw values and the parent's raw values. Adding a new space requires only these two methods — it automatically works with every other space in the library.

```
Xyz (root)
├── Rgb (linear)
│   ├── sRGB (gamma)
│   ├── HSB, HSL, HSI, HCB, HCY, HWB, HSP
│   └── CMYK
├── CIE Lab
│   └── CIE LCh
├── CIE Luv
│   └── CIE LCHuv
├── OkLab
│   └── OkLCh
└── JzAzBz
    └── JzCzHz
```

### Pipeline

When you call `color.to(targetSpace)`, `ColorConverter` finds the **Lowest Common Ancestor (LCA)** of the two spaces in the tree, walks up from the source via `toParent()`, then down to the target via `fromParent()`. Spaces in the same family never touch XYZ:

```
HSB → HSL (same family, LCA = Rgb)
  HSB ──toParent──▷ Rgb ──fromParent──▷ HSL

OkLab → OkLCh (parent to child)
  OkLab ──fromParent──▷ OkLCh

HSB → OkLab (cross-family, LCA = Xyz)
  HSB ──toParent──▷ Rgb ──toParent──▷ Xyz ──fromParent──▷ OkLab
```

Gamut mapping triggers when the walk-down enters a bounded space (like Rgb) from Xyz, ensuring out-of-gamut XYZ points are clamped before conversion.

### Creating a color space

Implement `ColorSpace`, declare a parent, and register it:

```java
public final class MySpace implements ColorSpace {
    public static final MySpace INSTANCE = new MySpace();

    // Metadata: displayName, componentCount, componentName, min, max, ...

    // Declare parent
    public ColorSpace parentSpace() { return Rgb.INSTANCE; }

    // Convert raw values to parent's raw values
    public double[] toParent(double[] raw) { ... }

    // Convert parent's raw values to this space's raw values
    public double[] fromParent(double[] parentRaw) { ... }

    // Map raw values to [0, 1] (user convenience)
    public double[] normalize(double[] raw) { ... }

    // Map [0, 1] back to raw values
    public double[] denormalize(double[] normalized) { ... }
}

ColorSpaceRegistry.register(MySpace.INSTANCE);
```

Root spaces (those whose parent is `Xyz`) must also override `isBounded()`, `isInGamut()`, and `neutralXyz()`. Child spaces inherit these from their parent.

### Color spaces

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| sRGB | R [0, 255], G [0, 255], B [0, 255] | Yes | IEC 61966-2-1, 1999 |
| Linear RGB | R [0, 255], G [0, 255], B [0, 255] | Yes | IEC 61966-2-1 (no gamma) |
| HSB | H [0, 360), S [0, 100], B [0, 100] | Yes | A. R. Smith, SIGGRAPH 1978 |
| HSL | H [0, 360), S [0, 100], L [0, 100] | Yes | Joblove & Greenberg, SIGGRAPH 1978 |
| HSI | H [0, 360), S [0, 100], I [0, 100] | Yes | Gonzalez & Woods, 1992 |
| HCB | H [0, 360), C [0, 100], B [0, 100] | Yes | Derived from HSB (raw chroma) |
| HCY | H [0, 360), C [0, 100], Y [0, 100] | Yes | K. Shapran, 2009 |
| HWB | H [0, 360), W [0, 100], B [0, 100] | Yes | A. R. Smith, 1996 |
| HSP | H [0, 360), S [0, 100], P [0, 100] | Yes | D. R. Finley |
| CMYK | C [0, 100], M [0, 100], Y [0, 100], K [0, 100] | Yes | Subtractive model (naive UCR) |
| CIE Lab | L [0, 100], a [-128, 127], b [-128, 127] | No | CIE 1976 |
| CIE LCh | L [0, 100], C [0, 181], H [0, 360) | No | CIE 1976 (cylindrical) |
| CIE Luv | L [0, 100], u [-200, 200], v [-200, 200] | No | CIE 1976 |
| CIE LCHuv | L [0, 100], C [0, 180], H [0, 360) | No | CIE 1976 (cylindrical) |
| OK Lab | L [0, 1], a [-0.28, 0.28], b [-0.31, 0.31] | No | B. Ottosson, 2020 |
| OK LCh | L [0, 1], C [0, 0.33], H [0, 360) | No | B. Ottosson, 2020 |
| JzAzBz | Jz [0, 0.02], az [-0.02, 0.02], bz [-0.02, 0.02] | No | Safdar et al., 2017 |
| JzCzHz | Jz [0, 0.02], Cz [0, 0.025], Hz [0, 360) | No | Safdar et al., 2017 (cylindrical) |

</div>

## Grayscale

### Pipeline

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

    public String displayName() { return "My Method"; }

    // Receive XYZ, return achromatic XYZ
    public double[] toGrayXyz(double[] xyz) {
        // Convert to whatever space you need via ColorConverter, compute, convert back
    }
}

GrayscaleRegistry.register(MyGrayscale.INSTANCE);
```

### Grayscale methods

<div align="center">

| Name | Formula | Source |
|------|---------|--------|
| Red Channel | R | — |
| Green Channel | G | — |
| Blue Channel | B | — |
| Average | (R+G+B) / 3 | — |
| Median | median(R, G, B) | — |
| Max | max(R, G, B) | — |
| Min | min(R, G, B) | — |
| Lightness (HSL) | (max+min) / 2 | Joblove & Greenberg, 1978 |
| BT.601 Luma | 0.299R + 0.587G + 0.114B | ITU-R BT.601, 1982 |
| BT.709 Luma | 0.2126R + 0.7152G + 0.0722B | ITU-R BT.709, 1990 |
| BT.2020 Luma | 0.2627R + 0.6780G + 0.0593B | ITU-R BT.2020, 2012 |
| SMPTE 240M Luma | 0.212R + 0.701G + 0.087B | SMPTE 240M, 1988 |
| Relative Luminance | 0.2126Rl + 0.7152Gl + 0.0722Bl | CIE 1931 |
| CIE L\* Lightness | L\* from CIE Lab | CIE 1976 |
| Oklab L Lightness | L from Oklab | B. Ottosson, 2020 |
| HSP Perceived Brightness | √(0.299R² + 0.587G² + 0.114B²) | D. R. Finley |

</div>
