import numpy as np


def read_gamut_csv(csv_path):
    """Reads a gamut CSV file and extracts metadata from the header comment.

    The first line is a '#' comment with semicolon-separated fields:
        space_name ; ch0 ; ch1 ; ch2 [; cylindrical ; hue_ch ; radial_ch]

    The second line is a column header (also a comment), followed by data rows
    with 6 semicolon-separated values: 3 color space coordinates + 3 RGB values.

    Returns:
        data:        (N, 6) float array, columns [coord0, coord1, coord2, R, G, B].
        space_name:  Name of the color space (e.g. "OKLab", "MSH").
        ch_names:    List of 3 channel names.
        cylindrical: Whether the space uses cylindrical coordinates.
        hue_ch:      Index (0-2) of the hue channel, or -1 if not cylindrical.
        radial_ch:   Index (0-2) of the radial channel, or -1 if not cylindrical.
    """
    # Read the first line to extract metadata
    with open(csv_path, 'r') as f:
        first_line = f.readline().strip()

    space_name = "Unknown"
    ch_names = ["Ch 0", "Ch 1", "Ch 2"]
    cylindrical = False
    hue_ch = -1
    radial_ch = -1

    if first_line.startswith('#'):
        parts = first_line[2:].split(';')
        space_name = parts[0].strip()

        # Channel names
        if len(parts) >= 4:
            ch_names = [parts[1].strip(), parts[2].strip(), parts[3].strip()]

        # Cylindrical metadata: flag, hue index, radial index
        if len(parts) >= 7 and parts[4].strip() == "cylindrical":
            cylindrical = True
            hue_ch = int(parts[5].strip())
            radial_ch = int(parts[6].strip())

    # Load numeric data, skipping the 2 header lines
    data = np.loadtxt(csv_path, delimiter=';', skiprows=2, comments='#')

    return data, space_name, ch_names, cylindrical, hue_ch, radial_ch
