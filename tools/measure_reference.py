#!/usr/bin/env python3
"""Measure the reference artwork instead of eyeballing it.

No PIL and no ImageMagick exist on this machine, and "strict structural parity" cannot be
honest if every number comes from looking at a picture. This decodes the PNG with the
standard library only and reports geometry in dp, using a declared reference width.

Usage:
    python tools/measure_reference.py assets/reference/04-connected-session.png \\
        --width-dp 411 --edges-x 600 --edges-y 470 --profile-y 600

What each mode answers:
    --edges-x Y    vertical boundaries (card left/right) crossed along row Y
    --edges-y X    horizontal boundaries (card top/bottom) crossed along column X
    --profile-y Y  luminance samples down a column, for spacing work
    --box          the bounding box of everything brighter than --thresh

Nothing here is a claim about the app; it is a ruler.
"""

import argparse
import struct
import sys
import zlib


def decode_png(path):
    """Return (width, height, rows) where rows is a list of bytearray RGBA."""
    data = open(path, "rb").read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit("not a PNG: %s" % path)
    pos = 8
    idat = bytearray()
    width = height = depth = color_type = None
    palette = None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, color_type = struct.unpack(">IIBB", chunk[:10])[:4]
        elif kind == b"PLTE":
            palette = chunk
        elif kind == b"IDAT":
            idat += chunk
        elif kind == b"IEND":
            break
    if depth != 8:
        raise SystemExit("only 8-bit depth supported, got %d" % depth)

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    raw = zlib.decompress(bytes(idat))
    stride = width * channels
    prev = bytearray(stride)
    rows = []
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        line = bytearray(raw[offset:offset + stride])
        offset += stride
        # Un-filter, per scanline.
        if filter_type == 1:
            for i in range(channels, stride):
                line[i] = (line[i] + line[i - channels]) & 0xFF
        elif filter_type == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif filter_type == 3:
            for i in range(stride):
                a = line[i - channels] if i >= channels else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif filter_type == 4:
            for i in range(stride):
                a = line[i - channels] if i >= channels else 0
                b = prev[i]
                c = prev[i - channels] if i >= channels else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        prev = line

        if color_type == 3:
            out = bytearray(width * 4)
            for x in range(width):
                idx = line[x] * 3
                out[x * 4:x * 4 + 4] = bytearray(
                    (palette[idx], palette[idx + 1], palette[idx + 2], 255))
        else:
            out = bytearray(width * 4)
            for x in range(width):
                base = x * channels
                out[x * 4] = line[base]
                out[x * 4 + 1] = line[base + 1] if channels >= 3 else line[base]
                out[x * 4 + 2] = line[base + 2] if channels >= 3 else line[base]
                out[x * 4 + 3] = line[base + 3] if channels == 4 else (
                    line[base + 1] if channels == 2 else 255)
        rows.append(out)
    return width, height, rows


def lum(rows, x, y):
    row = rows[y]
    i = x * 4
    # Rec.709 luma, which matches perceived brightness better than a plain average.
    return (0.2126 * row[i] + 0.7152 * row[i + 1] + 0.0722 * row[i + 2])


def dp(value, px_per_dp):
    return round(value / px_per_dp, 1)


def edges_along_x(rows, y, px_per_dp, thresh):
    width = len(rows[0]) // 4
    hits = []
    last = lum(rows, 0, y)
    for x in range(1, width):
        now = lum(rows, x, y)
        if abs(now - last) >= thresh:
            hits.append((dp(x, px_per_dp), "L" if now > last else "R"))
        last = now
    return hits


def edges_along_y(rows, x, px_per_dp, thresh):
    height = len(rows)
    hits = []
    last = lum(rows, x, 0)
    for y in range(1, height):
        now = lum(rows, x, y)
        if abs(now - last) >= thresh:
            hits.append((dp(y, px_per_dp), "D" if now > last else "U"))
        last = now
    return hits


def profile_column(rows, x, px_per_dp, step):
    height = len(rows)
    return [(dp(y, px_per_dp), round(lum(rows, x, y), 1)) for y in range(0, height, step)]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("png")
    ap.add_argument("--width-dp", type=float, default=411.0,
                    help="the dp width the artwork represents (default 411)")
    ap.add_argument("--edges-x", type=int, action="append", default=[],
                    help="pixel row(s) to scan for vertical boundaries")
    ap.add_argument("--edges-y", type=int, action="append", default=[],
                    help="pixel column(s) to scan for horizontal boundaries")
    ap.add_argument("--profile-y", type=int, default=None)
    ap.add_argument("--step", type=int, default=12)
    ap.add_argument("--thresh", type=float, default=18.0)
    args = ap.parse_args()

    width, height, rows = decode_png(args.png)
    px_per_dp = width / args.width_dp
    print("%s  %dx%d px  =  %.1f x %.1f dp  (%.3f px/dp)"
          % (args.png, width, height, args.width_dp, height / px_per_dp, px_per_dp))

    for y in args.edges_x:
        if y >= height:
            print("  row %d out of range" % y)
            continue
        hits = edges_along_x(rows, y, px_per_dp, args.thresh)
        print("\nvertical boundaries at pixel row %d (dp):" % y)
        print("  " + ", ".join("%s%s" % (pos, side) for pos, side in hits[:60]))

    for x in args.edges_y:
        if x >= width:
            print("  column %d out of range" % x)
            continue
        hits = edges_along_y(rows, x, px_per_dp, args.thresh)
        print("\nhorizontal boundaries at pixel column %d (dp):" % x)
        print("  " + ", ".join("%s%s" % (pos, side) for pos, side in hits[:80]))

    if args.profile_y is not None:
        for pos, value in profile_column(rows, args.profile_y, px_per_dp, args.step):
            print("  y=%6.1f dp  luma=%5.1f" % (pos, value))


if __name__ == "__main__":
    main()
