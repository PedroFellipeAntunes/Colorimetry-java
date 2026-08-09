# Color Spaces

Full catalog of all color spaces in Colorimetry, grouped by family. See the [hierarchy tree](../README.md#hierarchy) for how they connect.

## Table of contents

- [XYZ adapted](#xyz-adapted)
- [Linear RGB](#linear-rgb)
- [Gamma RGB](#gamma-rgb)
- [Subtractive and artistic](#subtractive-and-artistic)
- [Hue-based (cylindrical from RGB)](#hue-based-cylindrical-from-rgb)
- [CIE perceptual](#cie-perceptual)
- [LMS and derivatives](#lms-and-derivatives)
- [Modern perceptual](#modern-perceptual)
- [ACES](#aces)
- [Color Appearance Models](#color-appearance-models)

## XYZ adapted

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| CIE XYZ D65 | X, Y, Z | No | CIE 1931, D65 illuminant |
| CIE XYZ D50 | X, Y, Z | No | CIE 1931, D50 illuminant |
| CIE XYZ D60 | X, Y, Z | No | CIE 1931, D60 illuminant |
| xyY | x [0, 1], y [0, 1], Y [0, 1] | No | CIE 1931 chromaticity |

</div>

## Linear RGB

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| BT.709 RGB Linear | R, G, B [0, 255] | Yes | ITU-R BT.709 |
| BT.601 RGB Linear | R, G, B [0, 255] | Yes | ITU-R BT.601 |
| BT.2020 RGB Linear | R, G, B [0, 255] | Yes | ITU-R BT.2020 |
| Display P3 Linear | R, G, B [0, 255] | Yes | Apple / DCI-P3 D65 |
| Adobe RGB Linear | R, G, B [0, 255] | Yes | Adobe Systems, 1998 |
| ProPhoto RGB Linear | R, G, B [0, 255] | Yes | Kodak ROMM RGB |

</div>

## Gamma RGB

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| sRGB | R, G, B [0, 255] | Yes | IEC 61966-2-1, 1999 |
| Adobe RGB | R, G, B [0, 255] | Yes | Adobe Systems, 1998 |
| Display P3 | R, G, B [0, 255] | Yes | Apple / DCI-P3 D65 |
| ProPhoto RGB | R, G, B [0, 255] | Yes | Kodak ROMM RGB |
| Rec. 2020 | R, G, B [0, 255] | Yes | ITU-R BT.2020, 2012 |

</div>

## Subtractive and artistic

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| CMY | C, M, Y [0, 100] | Yes | Subtractive model |
| CMYK | C, M, Y, K [0, 100] | Yes | Subtractive model (naive UCR) |
| RYB | R, Y, B [0, 255] | Yes | Trilinear interpolation |

</div>

## Hue-based (cylindrical from RGB)

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| HSB | H [0, 360), S [0, 100], B [0, 100] | Yes | A. R. Smith, SIGGRAPH 1978 |
| HSL | H [0, 360), S [0, 100], L [0, 100] | Yes | Joblove & Greenberg, SIGGRAPH 1978 |
| HSI | H [0, 360), S [0, 100], I [0, 100] | Yes | Gonzalez & Woods, 1992 |
| HCB | H [0, 360), C [0, 100], B [0, 100] | Yes | Derived from HSB (raw chroma) |
| HCL | H [0, 360), C [0, 100], L [0, 100] | Yes | Derived from HSL (raw chroma) |
| HCY | H [0, 360), C [0, 100], Y [0, 100] | Yes | K. Shapran, 2009 |
| HWB | H [0, 360), W [0, 100], B [0, 100] | Yes | A. R. Smith, 1996 |
| HSP | H [0, 360), S [0, 100], P [0, 100] | Yes | D. R. Finley |

</div>

## CIE perceptual

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| CIE Lab | L [0, 100], a [-128, 127], b [-128, 127] | No | CIE 1976 |
| CIE LCh | L [0, 100], C [0, 150], H [0, 360) | No | CIE 1976 (cylindrical Lab) |
| CIE Luv | L [0, 100], u [-200, 200], v [-200, 200] | No | CIE 1976 |
| CIE LCHuv | L [0, 100], C [0, 180], H [0, 360) | No | CIE 1976 (cylindrical Luv) |
| HSLuv | H [0, 360), S [0, 100], L [0, 100] | No | Boronine, based on CIE LCHuv |
| HPLuv | H [0, 360), S [0, 100], L [0, 100] | No | Boronine, pastel variant |

</div>

## LMS and derivatives

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| LMS (HPE) | L, M, S [0, 1] | No | Hunt-Pointer-Estevez transform |
| IPT | I [0, 1], P [-1, 1], T [-1, 1] | No | Ebner & Fairchild, 1998 |
| IgPgTg | Ig [0, 1], Pg [-1, 1], Tg [-1, 1] | No | Hellwig & Fairchild, 2022 |

</div>

## Modern perceptual

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| OK Lab | L [0, 1], a [-0.28, 0.28], b [-0.31, 0.31] | No | B. Ottosson, 2020 |
| OK LCh | L [0, 1], C [0, 0.33], H [0, 360) | No | B. Ottosson, 2020 (cylindrical) |
| Ok HSL | H [0, 360), S [0, 1], L [0, 1] | No | B. Ottosson, 2020 |
| Ok HSV | H [0, 360), S [0, 1], V [0, 1] | No | B. Ottosson, 2020 |
| JzAzBz | Jz [0, 0.25], Az [-0.15, 0.15], Bz [-0.20, 0.15] | No | Safdar et al., 2017 |
| JzCzHz | Jz [0, 0.25], Cz [0, 0.22], Hz [0, 360) | No | Safdar et al., 2017 (cylindrical) |
| ICtCp | I [0, 1], Ct [-0.5, 0.5], Cp [-0.5, 0.5] | No | ITU-R BT.2100, 2016 |
| XYB | X [-0.04, 0.04], Y [0, 0.85], B [-0.4, 0.4] | No | JPEG XL |
| MSH | M [0, 180], S [0, 180], H [0, 360) | No | Moreland, 2009 (diverging colormaps) |

</div>

## ACES

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| ACES 2065-1 | R, G, B [0, 1] | No | SMPTE ST 2065-1 |
| ACEScg | R, G, B [0, 1] | No | SMPTE / ACEScg |

</div>

## Color Appearance Models

<div align="center">

| Name | Channels | Bounded | Source |
|------|----------|---------|--------|
| CAM16 | J [0, 100], C [0, 120], h [0, 360) | No | Li et al., 2017 |
| CAM16 UCS | J' [0, 100], a' [-100, 100], b' [-100, 100] | No | Li et al., 2017 (uniform) |
| CAM02 | J [0, 100], C [0, 120], h [0, 360) | No | CIE 159:2004 |
| CAM02 UCS | J' [0, 100], a' [-100, 100], b' [-100, 100] | No | Luo et al., 2006 (uniform) |
| HCT | H [0, 360), C [0, 120], T [0, 100] | No | Material Design 3 (Google) |
| ZCAM | J [0, 100], M [0, 65], h [0, 360) | No | Safdar et al., 2021 |

</div>
