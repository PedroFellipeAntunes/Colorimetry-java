import json


def write_html(points, rgb, tris, axis_labels, xy_range, z_range, title, safe_name, out_path):
    """Writes a standalone Plotly HTML file with an interactive 3D mesh.

    The HTML file includes:
    - A mesh3d trace with per-vertex RGB coloring and no lighting effects
      (full ambient, no diffuse/specular) so colors match the original gamut.
    - Orthographic camera projection with dark background.
    - An "Export Views" button that captures 7 camera angles as PNG files
      (default, +/-X, +/-Y, +/-Z).

    Args:
        points:      (N, 3) float array of vertex positions.
        rgb:         (N, 3) int array of vertex colors [0-255].
        tris:        (T, 3) int array of triangle vertex indices.
        axis_labels: List of 3 axis label strings.
        xy_range:    [min, max] range for X and Y axes.
        z_range:     [min, max] range for Z axis.
        title:       Plot title string.
        safe_name:   Filesystem-safe name used for export filenames.
        out_path:    Output file path for the HTML file.
    """
    # Build per-vertex color strings for Plotly
    vertex_colors = [f"rgb({r},{g},{b})" for r, g, b in rgb]

    trace = {
        "type": "mesh3d",
        "x": points[:, 0].tolist(),
        "y": points[:, 1].tolist(),
        "z": points[:, 2].tolist(),
        "i": tris[:, 0].tolist(),
        "j": tris[:, 1].tolist(),
        "k": tris[:, 2].tolist(),
        "vertexcolor": vertex_colors,
        "flatshading": False,
        "lighting": {"ambient": 1.0, "diffuse": 0.0, "specular": 0.0},
    }

    # Shared axis styling for dark theme
    axis_base = {
        "color": "white",
        "gridcolor": "rgba(255, 255, 255, 0.15)",
        "tickfont": {"color": "white", "size": 14},
        "titlefont": {"color": "white", "size": 16},
    }

    layout = {
        "scene": {
            "xaxis": {**axis_base, "title": axis_labels[0], "range": xy_range},
            "yaxis": {**axis_base, "title": axis_labels[1], "range": xy_range},
            "zaxis": {**axis_base, "title": axis_labels[2], "range": z_range},
            "aspectmode": "cube",
            "bgcolor": "rgb(20, 20, 20)",
            "camera": {"projection": {"type": "orthographic"}},
        },
        "paper_bgcolor": "rgb(20, 20, 20)",
        "font": {"color": "white"},
        "title": {"text": title, "font": {"color": "white", "size": 20}},
        "margin": {"l": 0, "r": 0, "t": 40, "b": 0},
    }

    html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
<style>
body {{ margin: 0; background: #141414; }}
#plot {{ width: 100vw; height: 100vh; }}
#export-btn {{
    position: fixed; bottom: 10px; right: 10px; z-index: 1000;
    padding: 8px 16px; background: #333; color: white;
    border: 1px solid #555; border-radius: 4px; cursor: pointer;
    font-size: 14px;
}}
#export-btn:hover {{ background: #555; }}
#export-btn:disabled {{ opacity: 0.5; cursor: wait; }}
</style>
</head>
<body>
<button id="export-btn" onclick="exportViews()">Export Views</button>
<div id="plot"></div>
<script>
Plotly.newPlot("plot", [{json.dumps(trace)}], {json.dumps(layout)});

var views = {{
    "default": {{ eye: {{ x: 1.5, y: 1.5, z: 1.0 }}, up: {{ x: 0, y: 0, z: 1 }} }},
    "pos_x":   {{ eye: {{ x: 2, y: 0, z: 0 }}, up: {{ x: 0, y: 0, z: 1 }} }},
    "neg_x":   {{ eye: {{ x: -2, y: 0, z: 0 }}, up: {{ x: 0, y: 0, z: 1 }} }},
    "pos_y":   {{ eye: {{ x: 0, y: 2, z: 0 }}, up: {{ x: 0, y: 0, z: 1 }} }},
    "neg_y":   {{ eye: {{ x: 0, y: -2, z: 0 }}, up: {{ x: 0, y: 0, z: 1 }} }},
    "pos_z":   {{ eye: {{ x: 0, y: 0, z: 2 }}, up: {{ x: 0, y: 1, z: 0 }} }},
    "neg_z":   {{ eye: {{ x: 0, y: 0, z: -2 }}, up: {{ x: 0, y: 1, z: 0 }} }}
}};

async function exportViews() {{
    var btn = document.getElementById("export-btn");
    btn.disabled = true;
    btn.textContent = "Exporting...";

    var names = Object.keys(views);

    for (var i = 0; i < names.length; i++) {{
        var name = names[i];
        var cam = views[name];
        cam.projection = {{ type: "orthographic" }};

        await Plotly.relayout("plot", {{ "scene.camera": cam }});
        await new Promise(r => setTimeout(r, 300));

        var img = await Plotly.toImage("plot", {{ format: "png", width: 1000, height: 1000 }});

        var a = document.createElement("a");
        a.href = img;
        a.download = "{safe_name}_" + name + ".png";
        a.click();

        await new Promise(r => setTimeout(r, 200));
    }}

    btn.disabled = false;
    btn.textContent = "Export Views";
}}
</script>
</body>
</html>"""

    with open(out_path, 'w') as f:
        f.write(html)
