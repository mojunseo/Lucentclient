#!/usr/bin/env python3
"""Draws the launcher icon, a lit redstone lamp, as a 1024x1024 PNG (16x16 pixel art scaled up).

Run from the repository root, then generate every size with:
  python3 tools/gen_launcher_icon.py && (cd launcher && npx tauri icon src-tauri/icons/source.png)
"""
import struct
import zlib

GRID = 16
SCALE = 64
OUT = "launcher/src-tauri/icons/source.png"

FRAME = (0x2E, 0x20, 0x19)
FRAME_LIGHT = (0x4A, 0x33, 0x26)
GLOW = (0xF6, 0xC7, 0x68)
GLOW_DARK = (0xD9, 0x9A, 0x3A)
CORE = (0xFF, 0xEB, 0xB0)
WHITE = (0xFF, 0xF8, 0xE6)


def texel(x, y):
    # Outer frame and the cross that splits the lamp into four panes.
    if x in (0, 15) or y in (0, 15):
        return FRAME
    if x in (1, 14) or y in (1, 14):
        return FRAME_LIGHT
    if x in (7, 8) or y in (7, 8):
        return FRAME_LIGHT if (x + y) % 3 else FRAME
    # Each pane glows brighter towards the center of the lamp.
    dx = min(abs(x - 7.5), 6) / 6
    dy = min(abs(y - 7.5), 6) / 6
    d = max(dx, dy)
    if d < 0.45:
        return WHITE if (x + y) % 4 == 0 else CORE
    if d < 0.75:
        return GLOW
    return GLOW_DARK


def main():
    size = GRID * SCALE
    rows = []
    for py in range(size):
        row = bytearray(b"\x00")
        for px in range(size):
            row += bytes(texel(px // SCALE, py // SCALE)) + b"\xff"
        rows.append(bytes(row))
    raw = b"".join(rows)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(OUT, "wb") as f:
        f.write(png)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
