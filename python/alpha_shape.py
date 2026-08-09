import numpy as np
from scipy.spatial import Delaunay


def alpha_shape_3d(points, alpha, pbar):
    """Computes the 3D alpha shape surface mesh from a point cloud.

    Performs a Delaunay triangulation on the input points, then filters
    tetrahedra by circumradius (keeping only those with circumradius < 1/alpha).
    Finally, extracts the boundary surface as triangles that appear in exactly
    one kept tetrahedron.

    All operations are fully vectorized with NumPy for performance.

    Args:
        points: (N, 3) float array of XYZ coordinates.
        alpha:  Alpha parameter controlling surface tightness.
                Larger alpha = tighter mesh (discards more tetrahedra).
                Smaller alpha = looser mesh (keeps more tetrahedra).
        pbar:   tqdm progress bar for status updates.

    Returns:
        tris:  (T, 3) int array of triangle vertex indices, or empty (0, 3) array.
        stats: Dict with processing statistics.
    """
    n_points = len(points)

    # Step 1: Delaunay triangulation via QHull
    pbar.set_postfix_str(f"{n_points} boundary points, Delaunay")
    tri = Delaunay(points)
    simplices = tri.simplices
    n_tets = len(simplices)

    # Step 2: Vectorized circumradius computation for all tetrahedra.
    # For tetrahedron (p0, p1, p2, p3), the circumradius formula uses
    # edge vectors a, b, c from p0, their cross products, and dot products.
    pbar.set_postfix_str(f"{n_tets} tetrahedra, alpha filtering")

    p0 = points[simplices[:, 0]]
    p1 = points[simplices[:, 1]]
    p2 = points[simplices[:, 2]]
    p3 = points[simplices[:, 3]]

    a = p1 - p0
    b = p2 - p0
    c = p3 - p0

    cross_bc = np.cross(b, c)
    cross_ca = np.cross(c, a)
    cross_ab = np.cross(a, b)

    # Denominator: 2 * dot(a, cross(b, c)), scalar per tetrahedron
    denom = 2.0 * np.einsum('ij,ij->i', a, cross_bc)

    # Squared lengths of edge vectors
    aa = np.einsum('ij,ij->i', a, a)
    bb = np.einsum('ij,ij->i', b, b)
    cc = np.einsum('ij,ij->i', c, c)

    # Numerator vector: weighted sum of cross products
    num = (
        aa[:, None] * cross_bc
        + bb[:, None] * cross_ca
        + cc[:, None] * cross_ab
    )

    num_norm = np.linalg.norm(num, axis=1)
    abs_denom = np.abs(denom)

    # Compute circumradius, avoiding division by zero for degenerate tetrahedra.
    # Use np.divide with out/where to prevent the RuntimeWarning that np.where
    # would cause (it evaluates both branches before selecting).
    cr = np.full(len(simplices), np.inf)
    nonzero = abs_denom > 1e-15
    np.divide(num_norm, abs_denom, out=cr, where=nonzero)

    # Keep tetrahedra whose circumradius is below the alpha threshold
    keep = cr < (1.0 / alpha)
    n_kept = int(np.sum(keep))
    kept = simplices[keep]

    pbar.set_postfix_str(f"{n_tets} tets, {n_kept} kept after alpha")
    pbar.update(1)

    stats = {
        "boundary_points": n_points,
        "tetrahedra": n_tets,
        "kept": n_kept,
        "triangles": 0,
    }

    if n_kept == 0:
        return np.empty((0, 3), dtype=int), stats

    # Step 3: Extract boundary faces.
    # Each tetrahedron has 4 triangular faces. A face on the boundary appears
    # in exactly one tetrahedron; internal faces are shared by two.
    pbar.set_postfix_str("extracting boundary faces")

    face_indices = np.array([
        [0, 1, 2],
        [0, 1, 3],
        [0, 2, 3],
        [1, 2, 3],
    ])

    # Generate all faces and sort vertex indices so shared faces match
    all_faces = kept[:, face_indices].reshape(-1, 3)
    all_faces.sort(axis=1)

    # Use structured array view + np.unique to count face occurrences
    faces_view = np.ascontiguousarray(all_faces).view(
        np.dtype((np.void, all_faces.dtype.itemsize * 3))
    ).ravel()

    _, inverse, counts = np.unique(faces_view, return_inverse=True, return_counts=True)

    # Boundary faces appear exactly once
    boundary_mask = counts[inverse] == 1
    tris = all_faces[boundary_mask]

    stats["triangles"] = len(tris)
    pbar.update(1)

    if len(tris) == 0:
        return np.empty((0, 3), dtype=int), stats

    return tris, stats
