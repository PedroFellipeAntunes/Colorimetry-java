import numpy as np


def voxel_boundary_filter(points, rgb, resolution, pbar):
    """Filters a point cloud to boundary voxels and collapses each to a centroid.

    Builds a 3D occupancy grid over the point cloud's bounding box. A voxel is
    classified as boundary if at least one of its 6 face-neighbors is unoccupied.
    All points inside each boundary voxel are then collapsed into a single centroid
    (mean XYZ position, mean RGB color), dramatically reducing point count while
    preserving the surface shape.

    Args:
        points:     (N, 3) float array of XYZ coordinates in the target space.
        rgb:        (N, 3) int array of RGB colors [0-255].
        resolution: Number of voxels per axis (e.g. 128 -> 128^3 grid).
        pbar:       tqdm progress bar for status updates.

    Returns:
        centroid_xyz: (M, 3) float array of centroid positions.
        centroid_rgb: (M, 3) int array of averaged RGB colors [0-255].
        stats:        Dict with filtering statistics.
    """
    n_total = len(points)
    pbar.set_postfix_str(f"{n_total} points, voxelizing at res {resolution}")

    # Compute bounding box with small padding to prevent edge points
    # from landing exactly on the grid boundary
    mins = points.min(axis=0)
    maxs = points.max(axis=0)
    span = maxs - mins
    padding = span * 0.001
    mins -= padding
    maxs += padding
    span = maxs - mins

    # Map continuous coordinates to discrete voxel indices
    normalized = (points - mins) / span
    voxel_idx = np.clip((normalized * resolution).astype(int), 0, resolution - 1)

    # Build boolean occupancy grid
    grid = np.zeros((resolution, resolution, resolution), dtype=bool)
    grid[voxel_idx[:, 0], voxel_idx[:, 1], voxel_idx[:, 2]] = True

    n_occupied = int(grid.sum())
    pbar.set_postfix_str(f"{n_total} points, {n_occupied} occupied voxels")

    # Detect interior voxels: occupied with all 6 face-neighbors also occupied.
    # Pad with False so voxels at the grid edge are always treated as boundary.
    padded = np.pad(grid, 1, constant_values=False)

    interior = (
        grid
        & padded[0:-2, 1:-1, 1:-1]  # -x neighbor
        & padded[2:,   1:-1, 1:-1]  # +x neighbor
        & padded[1:-1, 0:-2, 1:-1]  # -y neighbor
        & padded[1:-1, 2:,   1:-1]  # +y neighbor
        & padded[1:-1, 1:-1, 0:-2]  # -z neighbor
        & padded[1:-1, 1:-1, 2:]    # +z neighbor
    )

    # Boundary = occupied but not interior
    boundary = grid & ~interior

    # Select points whose voxel is on the boundary
    mask = boundary[voxel_idx[:, 0], voxel_idx[:, 1], voxel_idx[:, 2]]
    n_boundary_voxels = int(boundary.sum())
    n_boundary_pts = int(mask.sum())

    pbar.set_postfix_str(
        f"{n_occupied} voxels, {n_boundary_voxels} boundary, collapsing {n_boundary_pts} pts"
    )

    # Collapse all points in each boundary voxel to a single centroid
    b_points = points[mask]
    b_rgb = rgb[mask].astype(np.float64)
    b_voxels = voxel_idx[mask]

    # Encode (ix, iy, iz) as a single integer for grouping
    flat_key = (
        b_voxels[:, 0].astype(np.int64) * resolution * resolution
        + b_voxels[:, 1].astype(np.int64) * resolution
        + b_voxels[:, 2].astype(np.int64)
    )

    unique_keys, inverse = np.unique(flat_key, return_inverse=True)
    n_centroids = len(unique_keys)

    # Sum XYZ and RGB per voxel, then divide by count to get the mean
    centroid_xyz = np.zeros((n_centroids, 3), dtype=np.float64)
    centroid_rgb = np.zeros((n_centroids, 3), dtype=np.float64)
    counts = np.zeros(n_centroids, dtype=np.int64)

    np.add.at(centroid_xyz, inverse, b_points)
    np.add.at(centroid_rgb, inverse, b_rgb)
    np.add.at(counts, inverse, 1)

    centroid_xyz /= counts[:, None]
    centroid_rgb /= counts[:, None]
    centroid_rgb = np.clip(np.round(centroid_rgb), 0, 255).astype(int)

    stats = {
        "total_points": n_total,
        "occupied_voxels": n_occupied,
        "boundary_voxels": n_boundary_voxels,
        "boundary_points": n_boundary_pts,
        "centroids": n_centroids,
    }

    pbar.set_postfix_str(
        f"{n_total} pts -> {n_boundary_voxels} boundary voxels -> {n_centroids} centroids"
    )
    pbar.update(1)

    return centroid_xyz, centroid_rgb, stats
