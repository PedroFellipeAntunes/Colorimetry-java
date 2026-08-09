# Testing

All test programs, what they generate, and how to run them. Every test outputs to `color_tests/` (gitignored).

## Table of contents

- [ColorSpaceAtlasTest](#colorspaceatlastest)
- [ColorSpaceTreeTest](#colorspacetreetest)
- [ColorSpaceGamutExport](#colorspacegamutexport)
- [ParentAtlasTest](#parentatlastest)
- [GrayscaleGradientTest](#grayscalegradienttest)
- [GrayscaleAtlasTest](#grayscaleatlastest)

## ColorSpaceAtlasTest

Generates a 4096x4096 atlas PNG for each registered color space. The atlas is a 16x16 grid of 256x256 tiles. One channel varies across the grid (tile 0 = 0.0, tile 255 = 1.0), another varies across X within each tile, and the remaining channel varies across Y. All values are generated via `ofNormalized` (0-1 range) in the target space and converted to sRGB for display.

**Output:** `color_tests/color_spaces/atlas_{spaceName}.png`

**Arguments:**

| # | Name | Default | Description |
|---|------|---------|-------------|
| 0 | spaceName | `""` (all) | Filter by space display name. Empty runs all registered spaces. |
| 1 | gridChannel | `0` | Which channel (0, 1, or 2) varies across the tile grid. |

**Examples:**

```bash
# All spaces, channel 0 across grid
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceAtlasTest

# Only HSB, channel 1 across grid
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceAtlasTest -Dexec.args="HSB 1"
```

## ColorSpaceTreeTest

Prints the full parent hierarchy tree of all registered color spaces to the console. Each space shows metadata flags: `bounded`, `cylindrical`, `palette`. Also prints totals at the end.

**Output:** console only (no files).

**Arguments:** none.

```bash
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceTreeTest
```

**Example output:**

```
Color Space Tree
CIE XYZ
├── CIE XYZ D65
│   ├── xyY
│   ├── BT.709 RGB Linear  [bounded]
│   │   ├── sRGB  [bounded]
│   │   ├── HSB  [bounded, cylindrical, palette]
...
Total: 53 spaces (+1 root)
  Bounded: 22 | Cylindrical: 21 | Palette: 21
```

## ColorSpaceGamutExport

Iterates the sRGB cube, converts each sample from sRGB to a target color space, and writes a CSV with the normalized coordinates. This is the opposite direction of `ColorSpaceAtlasTest` (which goes from the target space to sRGB). The CSV has a metadata header line and columns `x;y;z;r;g;b` where x/y/z are the normalized [0,1] target space coordinates and r/g/b are the original sRGB values.

This test is used to verify that `normalize`/`denormalize` and `componentMin`/`componentMax` are correct. If any x/y/z value falls outside [0, 1], the bounds or normalization of that space are wrong.

**Output:** `color_tests/gamut_export/gamut_{spaceName}.csv`

**Arguments:**

| # | Name | Default | Description |
|---|------|---------|-------------|
| 0 | spaceName | `""` (all) | Filter by space display name. Empty runs all registered spaces. |
| 1 | step | `1` | Sampling step for the RGB cube. `1` samples every value (256^3 = 16M samples), `4` samples every 4th value (64^3 = 262K samples). Higher values run faster but produce coarser data. |
| 2 | volume | `true` | Sampling mode. `true` processes the entire sRGB cube (all interior + surface points). `false` processes only the 6 faces of the cube (surface only). |

The `volume` argument controls how much of the sRGB cube is sampled:
- **`true` (volume):** samples every point inside and on the surface of the cube. Slower, but captures the complete internal structure of the gamut. Useful for spaces with non-linear internal mappings (CAM models, perceptual spaces) where the interior shape matters.
- **`false` (surface):** samples only the 6 boundary faces of the cube, skipping any point where all three RGB channels are strictly between 0 and 255. Much faster and produces smaller CSVs. Sufficient for most spaces, since gamut boundary issues show up on the surface.

**Examples:**

```bash
# All spaces, step 1, full volume
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceGamutExport

# Only OKLab, step 4, full volume
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceGamutExport -Dexec.args="OKLab 4 true"

# Only CAM16, step 2, surface only
mvn exec:java -Dexec.mainClass=colorimetry_test.ColorSpaceGamutExport -Dexec.args="CAM16 2 false"
```

**Python visualization:** the exported CSVs can be visualized as 3D point clouds using the `main.py` script in the `python/` directory. It reads the CSV metadata header to determine axis labels and renders the gamut shape interactively.

## ParentAtlasTest

Generates atlas PNGs for a color space with every valid parent. Uses `acceptedParentType()` to discover which parents from the registry are compatible, then calls the space's `of(parent)` factory via reflection to create each variant. Each variant gets its own atlas in the same format as `ColorSpaceAtlasTest`.

Only useful for spaces that have a configurable parent (e.g. HSB, HSL, HCY, CIE Lab).

**Output:** `color_tests/parent_atlas/atlas_{SpaceName}_{ParentName}.png`

**Arguments:**

| # | Name | Default | Description |
|---|------|---------|-------------|
| 0 | spaceName | `"HCY"` | Which space to test. Must have `acceptedParentType() != null`. |
| 1 | gridChannel | `0` | Which channel varies across the tile grid. |

**Examples:**

```bash
# HCY with all valid parents, channel 0
mvn exec:java -Dexec.mainClass=colorimetry_test.ParentAtlasTest

# HSB with all valid parents, channel 1
mvn exec:java -Dexec.mainClass=colorimetry_test.ParentAtlasTest -Dexec.args="HSB 1"
```

## GrayscaleGradientTest

Generates a reference color gradient from a color space, then applies each registered grayscale method and saves a separate image for each. The gradient varies one channel across X while keeping the others at their default values.

**Output:** `color_tests/grayscale/gradient/reference_{spaceName}.png` and `color_tests/grayscale/gradient/gray_{methodName}.png`

**Arguments:**

| # | Name | Default | Description |
|---|------|---------|-------------|
| 0 | spaceName | `"HSL"` | Color space for the reference gradient. |
| 1 | gradientChannel | `0` | Which channel varies across X. |
| 2 | methodName | `""` (all) | Filter by grayscale method display name. Empty runs all methods. |

**Examples:**

```bash
# HSL gradient, channel 0, all methods
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleGradientTest

# HSB gradient, channel 0, only BT.709
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleGradientTest -Dexec.args="HSB 0 BT.709"
```

## GrayscaleAtlasTest

Applies all registered grayscale methods to each atlas PNG previously generated by `ColorSpaceAtlasTest`. For each atlas, creates a folder with one grayscale image per method. Requires `ColorSpaceAtlasTest` to have been run first.

**Output:** `color_tests/grayscale/{atlas_name}/gray_{methodName}.png`

**Arguments:**

| # | Name | Default | Description |
|---|------|---------|-------------|
| 0 | atlasName | `""` (all) | Filter by atlas filename. Empty processes all atlases found. |
| 1 | methodName | `""` (all) | Filter by grayscale method display name. Empty runs all methods. |

**Examples:**

```bash
# All atlases, all methods
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleAtlasTest

# Only sRGB atlas, only BT.709 method
mvn exec:java -Dexec.mainClass=colorimetry_test.GrayscaleAtlasTest -Dexec.args="sRGB BT.709"
```

**Note:** this test can be slow and memory-intensive when processing all atlases with all methods. Filter by atlas and method name when possible.
