# Contributing to Colorimetry

## Table of contents

- [Reporting bugs](#reporting-bugs)
- [Suggesting features](#suggesting-features)
- [Setup](#setup)
- [Adding a color space](#adding-a-color-space)
- [Adding a grayscale method](#adding-a-grayscale-method)
- [Pull requests](#pull-requests)
- [Further reading](#further-reading)

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

Requires Java 11+.

## Adding a color space

1. Create a class in the appropriate subpackage under `colorimetry/spaces/` (e.g. `spaces/hue/`, `spaces/perceptual/`, `spaces/cam/`). Implement `ColorSpace`.
2. Add a `public static final INSTANCE` singleton as the first field.
3. Implement metadata: `displayName`, `componentCount`, `componentName`, `componentMin`, `componentMax`, `componentDefault`, `componentStep`.
4. Declare `parentSpace()` - pick the closest existing space in the hierarchy.
5. Implement `toParent()` and `fromParent()` with the conversion math.
6. Implement `normalize()` and `denormalize()` for the 0-1 range mapping.
7. Root spaces (parent = `Xyz`) must also override `isBounded()`, `isInGamut()`, and `neutralXyz()`. Child spaces inherit these from their root ancestor.
8. For cylindrical spaces, override `isCylindrical()` (return `true`), `hueChannel()`, and `radialChannel()`.
9. For spaces where only some channels are bounded, override `isChannelBounded(int i)`.
10. For spaces with a configurable parent, implement a static `of(parent)` factory and override `acceptedParentType()` returning the appropriate marker interface (`RgbLike.class`, `XyzLike.class`, or `LabLike.class`).
11. Register in `ColorSpaceRegistry` static block.
12. Run `ColorSpaceAtlasTest`, `ColorSpaceTreeTest`, and `ColorSpaceGamutExport` to verify. See [Testing](docs/TESTING.md) for details on each test and its arguments.

## Adding a grayscale method

1. Create a class in the appropriate subpackage under `colorimetry/grayscale/` (e.g. `grayscale/luma/`, `grayscale/perceptual/`, `grayscale/simple/`, `grayscale/channel/`). Implement `Grayscale`.
2. Add a `public static final INSTANCE` singleton.
3. Implement `displayName()`.
4. Implement `toGrayXyz()` - receives XYZ, returns achromatic XYZ. Use `ColorConverter.convert()` to go to/from whatever space the method needs.
5. Register in `GrayscaleRegistry` static block.
6. Run `GrayscaleGradientTest` and `GrayscaleAtlasTest` to verify the result visually. See [Testing](docs/TESTING.md) for details.

## Pull requests

- One feature per PR.
- Branch from `main`.
- Follow the [code style](docs/CODE_STYLE.md).
- Run the relevant tests for what you changed (see [Adding a color space](#adding-a-color-space) or [Adding a grayscale method](#adding-a-grayscale-method)) and check the output before submitting.
- Describe what was changed and why in the PR description.

## Further reading

- [Code style](docs/CODE_STYLE.md) - formatting rules, section headers, and comment conventions.
- [Architecture](docs/ARCHITECTURE.md) - conversion pipeline, gamut mapping, and internal design.
- [Testing](docs/TESTING.md) - all test programs, what they do, and how to run them.
