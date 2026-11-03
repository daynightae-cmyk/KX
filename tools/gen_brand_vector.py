#!/usr/bin/env python3
"""
Generates the vector KX brand mark used for the launcher icon and the Android 12+
system splash icon.

The path arithmetic mirrors the normalised constants in
app/src/main/kotlin/com/kx/vpn/ui/brand/KXOrb.kt (orb radius 0.440 of the canvas,
shield half-width 0.40 of the orb radius, ring tilt -14 degrees), so the XML mark and
the Compose mark cannot drift apart silently.

Usage: python3 tools/gen_brand_vector.py
"""
import math
from pathlib import Path

# Normalised geometry, kept in sync with KXOrb.kt
ORB_RADIUS_RATIO = 0.440
SHIELD_HALF_WIDTH_RATIO = 0.40
SHIELD_TOP_RATIO = -0.72
SHIELD_BOTTOM_RATIO = 0.16

PALETTE = {
    "orb_core": "#EDE9FF",
    "orb_mid": "#B6A5FC",
    "orb_edge": "#2F19BA",
    "shield_violet": "#5932D3",
    "shield_deep": "#2F19BA",
    "ribbon_pearl": "#FDF7FE",
    "ribbon_cyan": "#5BCFFD",
    "rim_cyan": "#C2DAFD",
    "orchid": "#BE69EF",
    "violet": "#6537E8",
    "node_blue": "#6FB3FC",
    "node_orchid": "#D169FC",
}


def f(v: float) -> str:
    return f"{v:.2f}".rstrip("0").rstrip(".")


RING_TILT_DEGREES = -14.0
# Phase offsets and relative radii match drawOrbitNodes() in KXOrb.kt.
NODE_PHASES = [(0.20, 0.052, "node_blue"), (0.62, 0.046, "node_orchid"),
               (0.94, 0.058, "node_blue")]


def node_paths(cx: float, cy: float, r: float) -> str:
    """Parametric node placement on the tilted orbit ellipse, matching the Compose orb."""
    ring_cx, ring_cy = cx, cy - r * 0.06
    rx, ry = r * 0.80, r * 0.30
    tilt = math.radians(RING_TILT_DEGREES)
    out = []
    for phase, radius_ratio, color_key in NODE_PHASES:
        angle = math.radians(phase * 360.0)
        local_x = rx * math.cos(angle)
        local_y = ry * math.sin(angle)
        px = ring_cx + local_x * math.cos(tilt) - local_y * math.sin(tilt)
        py = ring_cy + local_x * math.sin(tilt) + local_y * math.cos(tilt)
        nr = r * radius_ratio
        out.append(
            f'    <path android:pathData="{circle_path(px, py, nr)}"'
            f' android:fillColor="{PALETTE[color_key]}"/>'
        )
    return "\n".join(out)


def circle_path(cx: float, cy: float, r: float) -> str:
    # Two arcs, the standard vector-drawable circle construction.
    return (
        f"M{f(cx - r)},{f(cy)} "
        f"a{f(r)},{f(r)} 0 1,0 {f(r * 2)},0 "
        f"a{f(r)},{f(r)} 0 1,0 {f(-r * 2)},0 Z"
    )


def ellipse_path(cx: float, cy: float, rx: float, ry: float) -> str:
    return (
        f"M{f(cx - rx)},{f(cy)} "
        f"a{f(rx)},{f(ry)} 0 1,0 {f(rx * 2)},0 "
        f"a{f(rx)},{f(ry)} 0 1,0 {f(-rx * 2)},0 Z"
    )


def crest_path(cx: float, half_width: float, top: float, bottom: float) -> str:
    left = cx - half_width
    right = cx + half_width
    shoulder = top + half_width * 0.34
    return (
        f"M{f(cx)},{f(top)} "
        f"C{f(right - half_width * 0.12)},{f(top - half_width * 0.10)} "
        f"{f(right)},{f(top + half_width * 0.06)} {f(right)},{f(shoulder)} "
        f"C{f(right)},{f(bottom - half_width * 0.62)} "
        f"{f(cx + half_width * 0.46)},{f(bottom - half_width * 0.22)} {f(cx)},{f(bottom)} "
        f"C{f(cx - half_width * 0.46)},{f(bottom - half_width * 0.22)} "
        f"{f(left)},{f(bottom - half_width * 0.62)} {f(left)},{f(shoulder)} "
        f"C{f(left)},{f(top + half_width * 0.06)} "
        f"{f(left + half_width * 0.12)},{f(top - half_width * 0.10)} {f(cx)},{f(top)} Z"
    )


def ribbon_path(cx: float, half_width: float, top: float, bottom: float,
                left_to_right: bool) -> str:
    sign = 1 if left_to_right else -1
    x0 = cx - sign * half_width * 0.92
    x1 = cx + sign * half_width * 0.92
    bend = sign * half_width * 0.30
    mid_y0 = top + (bottom - top) * 0.30
    mid_y1 = top + (bottom - top) * 0.58
    return (
        f"M{f(x0)},{f(top + half_width * 0.18)} "
        f"C{f(cx + bend)},{f(mid_y0)} {f(cx + bend)},{f(mid_y1)} "
        f"{f(x1)},{f(bottom - half_width * 0.10)}"
    )


def star_path(cx: float, cy: float, outer: float, inner: float) -> str:
    pts = [
        (cx, cy - outer), (cx + inner, cy - inner), (cx + outer, cy),
        (cx + inner, cy + inner), (cx, cy + outer), (cx - inner, cy + inner),
        (cx - outer, cy), (cx - inner, cy - inner),
    ]
    body = " ".join(f"L{f(x)},{f(y)}" for x, y in pts[1:])
    return f"M{f(pts[0][0])},{f(pts[0][1])} {body} Z"


def radial_gradient(name: str, stops: list, cx: float, cy: float, r: float) -> str:
    body = "".join(
        f'<item android:offset="{offset}" android:color="{color}"/>'
        for offset, color in stops
    )
    return (
        f'<aapt:attr name="android:fillColor">'
        f'<gradient android:type="radial" android:gradientRadius="{f(r)}"'
        f' android:centerX="{f(cx)}" android:centerY="{f(cy)}">{body}</gradient>'
        f'</aapt:attr>'
    )


def linear_gradient(name: str, stops: list, x0: float, y0: float, x1: float, y1: float,
                    attr: str = "android:fillColor") -> str:
    body = "".join(
        f'<item android:offset="{offset}" android:color="{color}"/>'
        for offset, color in stops
    )
    return (
        f'<aapt:attr name="{attr}">'
        f'<gradient android:type="linear"'
        f' android:startX="{f(x0)}" android:startY="{f(y0)}"'
        f' android:endX="{f(x1)}" android:endY="{f(y1)}">{body}</gradient>'
        f'</aapt:attr>'
    )


def build_orb(canvas: int, cx: float, cy: float, wordmark: bool,
              orb_ratio: float = ORB_RADIUS_RATIO) -> str:
    r = canvas * orb_ratio
    hw = r * SHIELD_HALF_WIDTH_RATIO
    top = cy + r * SHIELD_TOP_RATIO
    bottom = cy + r * SHIELD_BOTTOM_RATIO
    band = hw * 0.62

    p = PALETTE
    lines = [
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"'
        f' xmlns:aapt="http://schemas.android.com/aapt"'
        f' android:width="{canvas}dp" android:height="{canvas}dp"'
        f' android:viewportWidth="{canvas}" android:viewportHeight="{canvas}">',
        '    <!-- Generated by tools/gen_brand_vector.py. Do not hand-edit; '
        'rerun the generator. -->',
        '    <!-- Pearl-glass orb body -->',
        f'    <path android:pathData="{circle_path(cx, cy, r)}">',
        "        " + radial_gradient(
            "orb",
            [("0.0", p["orb_core"]), ("0.45", p["orb_mid"]),
             ("0.82", p["orb_mid"]), ("1.0", p["orb_edge"])],
            cx, cy - r * 0.28, r * 1.35,
        ),
        '    </path>',
        '    <!-- Gradient rim -->',
        f'    <path android:pathData="{circle_path(cx, cy, r)}"'
        f' android:strokeWidth="{f(r * 0.010)}">',
        "        " + linear_gradient(
            "rim",
            [("0.0", p["rim_cyan"]), ("0.28", p["violet"]), ("0.55", p["orchid"]),
             ("0.78", p["ribbon_cyan"]), ("1.0", p["rim_cyan"])],
            cx - r, cy - r, cx + r, cy + r,
            attr="android:strokeColor",
        ),
        '    </path>',
        '    <!-- Orbit ring, tilted -14 degrees like the reference -->',
        f'    <group android:rotation="{RING_TILT_DEGREES}" android:pivotX="{f(cx)}"'
        f' android:pivotY="{f(cy - r * 0.06)}">',
        f'        <path android:pathData="'
        f'{ellipse_path(cx, cy - r * 0.06, r * 0.80, r * 0.30)}"'
        f' android:strokeWidth="{f(r * 0.006)}" android:strokeAlpha="0.45"'
        f' android:fillAlpha="0" android:strokeColor="#FFFFFFFF"'
        ' android:fillColor="#00000000"/>',
        '    </group>',
        '    <!-- Shield body -->',
        f'    <path android:pathData="{crest_path(cx, hw, top, bottom)}">',
        "        " + linear_gradient(
            "shield",
            [("0.0", p["shield_violet"]), ("1.0", p["shield_deep"])],
            cx - hw, top, cx + hw, bottom,
        ),
        '    </path>',
        '    <!-- Ribbon fold, left-to-right -->',
        f'    <path android:pathData="{ribbon_path(cx, hw, top, bottom, True)}"'
        f' android:strokeWidth="{f(band)}" android:strokeLineCap="round"'
        f' android:fillColor="#00000000">',
        "        " + linear_gradient(
            "ribbonA",
            [("0.0", p["ribbon_pearl"]), ("0.5", p["ribbon_cyan"]),
             ("1.0", "#6C52FB")],
            cx - hw, top, cx + hw, bottom,
            attr="android:strokeColor",
        ),
        '    </path>',
        '    <!-- Ribbon fold, right-to-left -->',
        f'    <path android:pathData="{ribbon_path(cx, hw, top, bottom, False)}"'
        f' android:strokeWidth="{f(band * 0.92)}" android:strokeLineCap="round"'
        f' android:fillColor="#00000000">',
        "        " + linear_gradient(
            "ribbonB",
            [("0.0", p["orchid"]), ("0.5", p["ribbon_pearl"]), ("1.0", p["node_blue"])],
            cx + hw, top, cx - hw, bottom,
            attr="android:strokeColor",
        ),
        '    </path>',
        '    <!-- Orbiting jewel nodes: same parametric placement as KXOrb.kt -->',
        node_paths(cx, cy, r),
        '    <!-- Sparkles -->',
        f'    <path android:pathData="{star_path(cx + r * 0.52, cy - r * 0.62, r * 0.075, r * 0.0195)}"'
        ' android:fillColor="#E6FFFFFF"/>',
        f'    <path android:pathData="{star_path(cx - r * 0.60, cy - r * 0.06, r * 0.055, r * 0.0143)}"'
        ' android:fillColor="#B3FFFFFF"/>',
    ]
    if wordmark:
        # The wordmark is drawn as geometric strokes so the mark stays legible at
        # 108dp, where real text would be sub-pixel.
        wm_top = cy + r * 0.42
        wm_bottom = cy + r * 0.74
        wm_mid = (wm_top + wm_bottom) / 2
        wm_left = cx - r * 0.34
        wm_right = cx + r * 0.34
        stem = f(r * 0.115)
        ink = "#140A82"

        k_stem = f"M{f(wm_left)},{f(wm_top)} L{f(wm_left)},{f(wm_bottom)}"
        k_arms = (
            f"M{f(wm_left + r * 0.02)},{f(wm_mid)} L{f(wm_left + r * 0.26)},{f(wm_top)} "
            f"M{f(wm_left + r * 0.02)},{f(wm_mid)} L{f(wm_left + r * 0.26)},{f(wm_bottom)}"
        )
        x_strokes = (
            f"M{f(cx + r * 0.06)},{f(wm_top)} L{f(wm_right)},{f(wm_bottom)} "
            f"M{f(cx + r * 0.06)},{f(wm_bottom)} L{f(wm_right)},{f(wm_top)}"
        )

        def stroked(path_data: str) -> str:
            return (
                f'    <path android:pathData="{path_data}"'
                f' android:strokeWidth="{stem}" android:strokeLineCap="round"'
                f' android:fillColor="#00000000" android:strokeColor="{ink}"/>'
            )

        lines += [
            '    <!-- KX wordmark strokes -->',
            stroked(k_stem),
            stroked(k_arms),
            stroked(x_strokes),
        ]
    lines.append('</vector>')
    return "\n".join(lines) + "\n"


def main() -> None:
    res = Path("app/src/main/res/drawable")
    res.mkdir(parents=True, exist_ok=True)

    # Splash / in-app mark: fills the canvas, carries the wordmark.
    splash = build_orb(canvas=108, cx=54.0, cy=54.0, wordmark=True)
    (res / "ic_kx_brand_orb.xml").write_text(splash, encoding="utf-8")

    # Adaptive launcher foreground: the visible safe zone of a 108dp adaptive canvas
    # is a 66dp circle, so the orb is drawn at radius 0.30 of the canvas and the
    # wordmark is dropped — it is unreadable at launcher sizes.
    launcher = build_orb(canvas=108, cx=54.0, cy=54.0, wordmark=False, orb_ratio=0.30)
    (res / "ic_kx_launcher_foreground.xml").write_text(launcher, encoding="utf-8")

    print("wrote app/src/main/res/drawable/ic_kx_brand_orb.xml")
    print("wrote app/src/main/res/drawable/ic_kx_launcher_foreground.xml")


if __name__ == "__main__":
    main()
