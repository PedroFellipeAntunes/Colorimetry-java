# Grayscale Methods

All grayscale methods in Colorimetry. Each method receives a CIE XYZ triplet and returns an achromatic (gray) XYZ triplet. See the [grayscale pipeline](../README.md#grayscale) for how they work.

## Table of contents

- [Channel-based](#channel-based)
- [Simple statistics](#simple-statistics)
- [Luma (gamma-weighted)](#luma-gamma-weighted)
- [Perceptual](#perceptual)

## Channel-based

Extracts a single sRGB channel and uses it as the gray value.

<div align="center">

| Name | Formula | Source |
|------|---------|--------|
| Red Channel | R | - |
| Green Channel | G | - |
| Blue Channel | B | - |

</div>

## Simple statistics

Computes gray from basic statistics of sRGB channels.

<div align="center">

| Name | Formula | Source |
|------|---------|--------|
| Average | (R+G+B) / 3 | - |
| Median | median(R, G, B) | - |
| Max | max(R, G, B) | - |
| Min | min(R, G, B) | - |
| Lightness (HSL) | (max+min) / 2 | Joblove & Greenberg, 1978 |

</div>

## Luma (gamma-weighted)

Weighted sum of gamma-encoded sRGB channels. Each standard defines different coefficients tuned for its target display.

<div align="center">

| Name | Formula | Source |
|------|---------|--------|
| BT.601 Luma | 0.299R + 0.587G + 0.114B | ITU-R BT.601, 1982 |
| BT.709 Luma | 0.2126R + 0.7152G + 0.0722B | ITU-R BT.709, 1990 |
| BT.2020 Luma | 0.2627R + 0.6780G + 0.0593B | ITU-R BT.2020, 2012 |
| SMPTE 240M Luma | 0.212R + 0.701G + 0.087B | SMPTE 240M, 1988 |

</div>

## Perceptual

Methods that use linearized or perceptual color spaces for more accurate brightness representation.

<div align="center">

| Name | Formula | Source |
|------|---------|--------|
| Relative Luminance (CIE Y) | 0.2126Rl + 0.7152Gl + 0.0722Bl | CIE 1931 |
| CIE L\* Lightness | L\* from CIE Lab | CIE 1976 |
| Oklab L Lightness | L from Oklab | B. Ottosson, 2020 |
| HSP Perceived Brightness | √(wr·R² + wg·G² + wb·B²) | D. R. Finley |

</div>
