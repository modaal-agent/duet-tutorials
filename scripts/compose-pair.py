#!/usr/bin/env python3
# /// script
# dependencies = ["pillow"]
# ///
"""Composite two captures, an iPhone simulator's and an Android emulator's,
into one PNG: the iPhone on the left, the Android on the right, both scaled
to one height, on a white ground with a gutter between them.

    scripts/compose-pair.py <ios.png> <android.png> <out.png> [--height 1200]
    scripts/compose-pair.py <ios.png> <android.png> <out.png> --second <ios2.png> <android2.png>

`--second` adds a second row of the same shape under the first, for a
four-frame pair (one state per row). Run it with `uv run scripts/compose-pair.py …` (the dependency block above
fetches Pillow) or with any Python that has Pillow installed. The captures
come from `xcrun simctl io booted screenshot <file>` and
`adb exec-out screencap -p > <file>`.
"""
from __future__ import annotations

import argparse
import sys

from PIL import Image

GUTTER = 96
MARGIN = 64
GROUND = (255, 255, 255)


def scaled(path: str, height: int) -> Image.Image:
    image = Image.open(path).convert("RGBA")
    width = round(image.width * height / image.height)
    return image.resize((width, height), Image.Resampling.LANCZOS)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("ios")
    parser.add_argument("android")
    parser.add_argument("out")
    parser.add_argument("--height", type=int, default=1200, help="each frame's height in pixels")
    parser.add_argument(
        "--second", nargs=2, metavar=("IOS2", "ANDROID2"), help="a second row: another iPhone and Android capture"
    )
    args = parser.parse_args(argv[1:])

    rows = [(scaled(args.ios, args.height), scaled(args.android, args.height))]
    if args.second:
        rows.append((scaled(args.second[0], args.height), scaled(args.second[1], args.height)))
    width = max(left.width + GUTTER + right.width for left, right in rows)
    canvas = Image.new(
        "RGB",
        (MARGIN + width + MARGIN, MARGIN + len(rows) * args.height + (len(rows) - 1) * GUTTER + MARGIN),
        GROUND,
    )
    for row, (left, right) in enumerate(rows):
        top = MARGIN + row * (args.height + GUTTER)
        canvas.paste(left, (MARGIN, top), left)
        canvas.paste(right, (MARGIN + left.width + GUTTER, top), right)
    canvas.save(args.out, optimize=True)
    print(f"compose-pair: {args.out} {canvas.width}x{canvas.height}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
