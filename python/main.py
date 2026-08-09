"""Gamut Plot Generator (Alpha Shape)

Reads gamut CSV files exported from the Colorimetry-java library, builds a 3D
surface mesh of the sRGB gamut in the target color space, and writes interactive
Plotly HTML files for visualization.

Pipeline:
    1. Read CSV (color space coordinates + RGB per point)
    2. Convert cylindrical spaces to cartesian for plotting (optional)
    3. Voxel boundary filter: discard interior points, collapse boundary to centroids
    4. Alpha shape: Delaunay + circumradius filter + boundary face extraction
    5. Write standalone HTML with mesh3d visualization
"""

import os
import sys
import glob

import numpy as np
from tqdm import tqdm

from csv_reader import read_gamut_csv
from voxel_filter import voxel_boundary_filter
from alpha_shape import alpha_shape_3d
from html_writer import write_html

# ---------------------------------------------------------------------------
# Defaults (edit these for quick configuration without CLI args)
# ---------------------------------------------------------------------------
DEFAULT_ALPHA = 2.0
DEFAULT_VOXEL_RES = 128
DEFAULT_CYLINDRICAL = True


# ---------------------------------------------------------------------------
# Pipeline
# ---------------------------------------------------------------------------

def process_csv(csv_path, alpha, voxel_res, use_cyl):
    """Runs the full pipeline for a single gamut CSV file.

    Reads the CSV, optionally converts cylindrical coordinates to cartesian,
    filters to boundary centroids via voxel grid, computes the alpha shape
    surface mesh, and writes the result as a standalone HTML file.

    Args:
        csv_path:  Path to the input gamut CSV file.
        alpha:     Alpha shape tightness parameter.
        voxel_res: Voxel grid resolution per axis.
        use_cyl:   Whether to apply cylindrical-to-cartesian conversion.
    """
    data, space_name, ch_names, cylindrical, hue_ch, radial_ch = read_gamut_csv(csv_path)

    points_raw = data[:, :3]
    rgb_raw = data[:, 3:6].astype(int)

    # Convert cylindrical coordinates (hue, radius, height) to cartesian (x, y, z)
    # for 3D plotting, if the CSV declares the space as cylindrical
    if cylindrical and use_cyl:
        third_ch = 3 - hue_ch - radial_ch

        hue = points_raw[:, hue_ch] * 2.0 * np.pi
        radius = points_raw[:, radial_ch]
        height = points_raw[:, third_ch]

        plot_points = np.column_stack([
            radius * np.cos(hue),
            radius * np.sin(hue),
            height,
        ])

        axis_labels = [
            ch_names[radial_ch] + " * cos(" + ch_names[hue_ch] + ")",
            ch_names[radial_ch] + " * sin(" + ch_names[hue_ch] + ")",
            ch_names[third_ch],
        ]
        xy_range = [-1, 1]
        z_range = [0, 1]
    else:
        plot_points = points_raw
        axis_labels = ch_names
        xy_range = [0, 1]
        z_range = [0, 1]

    # Progress bar: 5 steps (voxel, delaunay, alpha, boundary, html+done)
    pbar = tqdm(
        total=5,
        desc=space_name,
        leave=True,
        bar_format="{desc}: {bar} {n_fmt}/{total_fmt} [{elapsed}] {postfix}",
    )

    # Step 1: Voxel boundary filter on the plot coordinates
    centroid_points, centroid_rgb, voxel_stats = voxel_boundary_filter(
        plot_points, rgb_raw, voxel_res, pbar,
    )

    # Steps 2-3: Alpha shape on centroid points
    tris, alpha_stats = alpha_shape_3d(centroid_points, alpha, pbar)

    if len(tris) == 0:
        pbar.set_postfix_str(
            f"{alpha_stats['boundary_points']} centroids, "
            f"{alpha_stats['tetrahedra']} tets, {alpha_stats['kept']} kept, 0 tris (skipped)"
        )
        pbar.close()
        return

    # Step 4-5: Write HTML output
    safe_name = space_name.replace(" ", "_")
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(script_dir, safe_name)
    os.makedirs(output_dir, exist_ok=True)

    title = "sRGB Gamut in " + space_name
    html_path = os.path.join(output_dir, "gamut_" + safe_name + ".html")

    pbar.set_postfix_str(f"{alpha_stats['triangles']} triangles, writing HTML")
    write_html(
        centroid_points, centroid_rgb, tris,
        axis_labels, xy_range, z_range, title, safe_name, html_path,
    )
    pbar.update(1)

    pbar.set_postfix_str(
        f"{voxel_stats['total_points']} -> {voxel_stats['centroids']} centroids, "
        f"{alpha_stats['tetrahedra']} tets, {alpha_stats['triangles']} tris"
    )
    pbar.update(1)
    pbar.close()
    tqdm.write(f"  -> {html_path}")


# ---------------------------------------------------------------------------
# CSV discovery and selection
# ---------------------------------------------------------------------------

def find_csvs():
    """Finds all gamut CSV files in the gamut_export folder.

    Looks for files matching 'gamut_*.csv' in the expected export directory
    relative to this script's location.

    Returns:
        Sorted list of CSV file paths.
    """
    script_dir = os.path.dirname(os.path.abspath(__file__))
    export_dir = os.path.join(script_dir, "..", "color_tests", "gamut_export")

    if not os.path.isdir(export_dir):
        print(f"Folder not found: {export_dir}")
        sys.exit(1)

    csvs = sorted(glob.glob(os.path.join(export_dir, "gamut_*.csv")))

    if not csvs:
        print(f"No gamut CSVs found in {export_dir}")
        sys.exit(1)

    return csvs


def prompt_selection(csvs):
    """Displays available CSVs and prompts the user to select one or all.

    Args:
        csvs: List of CSV file paths.

    Returns:
        List of selected CSV file paths.
    """
    print("=== Gamut Plot Generator (Alpha Shape) ===\n")
    print("  0  All")

    for i, path in enumerate(csvs):
        name = os.path.basename(path).replace("gamut_", "").replace(".csv", "")
        print(f"  {i + 1}  {name}")

    print()

    while True:
        try:
            choice = int(input("Select: "))
        except ValueError:
            print("Invalid input.")
            continue

        if choice == 0:
            return csvs

        if 1 <= choice <= len(csvs):
            return [csvs[choice - 1]]

        print(f"Invalid option. Enter 0-{len(csvs)}.")


# ---------------------------------------------------------------------------
# CLI argument parsing
# ---------------------------------------------------------------------------

def parse_float_arg(argv, flag, default):
    """Extracts a float value following a CLI flag, or returns the default."""
    for i, arg in enumerate(argv):
        if arg == flag and i + 1 < len(argv):
            try:
                return float(argv[i + 1])
            except ValueError:
                print(f"Invalid value for {flag}: {argv[i + 1]}")
                sys.exit(1)

    return default


def parse_int_arg(argv, flag, default):
    """Extracts an int value following a CLI flag, or returns the default."""
    for i, arg in enumerate(argv):
        if arg == flag and i + 1 < len(argv):
            try:
                return int(argv[i + 1])
            except ValueError:
                print(f"Invalid value for {flag}: {argv[i + 1]}")
                sys.exit(1)

    return default


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    csvs = find_csvs()

    alpha = parse_float_arg(sys.argv, "--alpha", DEFAULT_ALPHA)
    voxel_res = parse_int_arg(sys.argv, "--voxel-res", DEFAULT_VOXEL_RES)
    use_cyl = DEFAULT_CYLINDRICAL and ("--no-cyl" not in sys.argv)

    print(f"\nAlpha: {alpha}")
    print(f"Voxel resolution: {voxel_res}")
    print(f"Cylindrical: {use_cyl}\n")

    selected = prompt_selection(csvs)

    for csv_path in selected:
        process_csv(csv_path, alpha, voxel_res, use_cyl)

    print("\nDone.")
