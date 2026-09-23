#!/usr/bin/env python3
"""Generates the cosmetic textures under src/client/resources/assets/lucentclient/textures/cosmetic.

Uses only the standard library. Every texture is drawn at SCALE times the model's texture size;
Minecraft normalizes UVs, so the extra resolution just adds detail.

Run from the repository root: python3 tools/gen_cosmetic_textures.py
"""
import math
import os
import random
import struct
import zlib

OUT = "src/client/resources/assets/lucentclient/textures/cosmetic"
SCALE = 4


class Image:
    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.pixels = [(0, 0, 0, 0)] * (width * height)

    def set(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y * self.width + x] = color

    def fill_region(self, u, v, w, h, shader):
        """Calls shader(fx, fy) with 0..1 coordinates for each pixel of a texel region."""
        for py in range(v * SCALE, (v + h) * SCALE):
            for px in range(u * SCALE, (u + w) * SCALE):
                fx = (px - u * SCALE + 0.5) / (w * SCALE)
                fy = (py - v * SCALE + 0.5) / (h * SCALE)
                color = shader(fx, fy)
                if color is not None:
                    self.set(px, py, color)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        raw = b"".join(
            b"\x00" + b"".join(struct.pack("4B", *self.pixels[y * self.width + x]) for x in range(self.width))
            for y in range(self.height)
        )

        def chunk(tag, data):
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(raw, 9))
        png += chunk(b"IEND", b"")
        with open(path, "wb") as f:
            f.write(png)
        print("wrote", path)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def rgb(hex_color, alpha=255):
    return ((hex_color >> 16) & 0xFF, (hex_color >> 8) & 0xFF, hex_color & 0xFF, alpha)


def star_field(seed, count):
    rng = random.Random(seed)
    return [(rng.random(), rng.random(), rng.uniform(0.012, 0.025)) for _ in range(count)]


def near_star(stars, fx, fy, aspect):
    for sx, sy, r in stars:
        if (fx - sx) ** 2 + ((fy - sy) / aspect) ** 2 < r * r:
            return True
    return False


# --- Capes: vanilla cape layout, 64x32. Front face at (1,1), back face at (12,1), both 10x16. ---

def cape_lucent():
    stars = star_field(1, 14)

    def shader(fx, fy):
        color = mix(rgb(0x2B1055), rgb(0x5EC8F2), fy)
        # Diamond emblem in the upper middle.
        d = abs(fx - 0.5) * 1.6 + abs(fy - 0.32)
        if d < 0.12:
            return mix(rgb(0xFFFFFF), rgb(0xC4F1FF), d / 0.12)
        if d < 0.15:
            return rgb(0x1A0B3A)
        if near_star(stars, fx, fy, 1.6):
            return rgb(0xFFFFFF)
        return color
    return shader


def cape_flame():
    def shader(fx, fy):
        base = mix(rgb(0x3A0A05), rgb(0x8C1C0B), fy)
        flame = 0.55 + 0.12 * math.sin(fx * 18) + 0.08 * math.sin(fx * 41 + 1.3)
        if fy > flame:
            t = (fy - flame) / (1 - flame + 1e-6)
            return mix(rgb(0xFF6A00), rgb(0xFFE36B), t)
        if fy > flame - 0.06:
            return rgb(0xFF3B1F)
        return base
    return shader


def cape_ocean():
    def shader(fx, fy):
        color = mix(rgb(0x0B3D91), rgb(0x0A1F44), fy)
        wave = math.sin(fx * math.pi * 4 + fy * 20)
        if abs(math.sin(fy * 26 + math.sin(fx * math.pi * 2) * 1.5)) > 0.94:
            return mix(color, rgb(0x9BE7FF), 0.7)
        if wave > 0.85:
            return mix(color, rgb(0x2E86DE), 0.5)
        return color
    return shader


def cape_galaxy():
    stars = star_field(7, 40)

    def shader(fx, fy):
        n = 0.5 + 0.5 * math.sin(fx * 7 + fy * 5) * math.cos(fx * 3 - fy * 9)
        color = mix(rgb(0x05030F), rgb(0x4B1D6E), n * 0.8)
        color = mix(color, rgb(0xD1437A), max(0.0, n - 0.75) * 2.5)
        if near_star(stars, fx, fy, 1.6):
            return rgb(0xFFF6D5)
        return color
    return shader


def make_cape(name, shader):
    img = Image(64 * SCALE, 32 * SCALE)
    # Edges and the elytra region (22,0 .. 46,22) get the cape colors too.
    img.fill_region(0, 0, 64, 32, lambda fx, fy: shader(0.5, fy * 2 % 1))
    img.fill_region(22, 0, 24, 22, shader)
    img.fill_region(1, 1, 10, 16, shader)
    img.fill_region(12, 1, 10, 16, lambda fx, fy: shader(1 - fx, fy))
    img.save(f"{OUT}/cape/{name}.png")


# --- Wings: a flat 16x20 plane. Layout 32x32: front face at (0,0), back face at (16,0) mirrored. ---
# Shader receives (root, fy): root = 0 at the spine, 1 at the wing tip.

def wing_angel(root, fy):
    top = 0.25 * (1 - math.sin(root * math.pi * 0.9)) + 0.05
    feathers = 7
    k = (root * feathers) % 1
    bottom = 0.55 + 0.4 * math.sin(root * math.pi * 0.85) - 0.12 * (1 - math.sin(k * math.pi))
    if not (top < fy < bottom):
        return None
    shade = mix(rgb(0xFFFFFF), rgb(0xD6DEEB), (fy - top) / (bottom - top))
    if k < 0.06 and fy > 0.45:
        return mix(shade, rgb(0xA9B4C6), 0.6)
    if fy < top + 0.05:
        return rgb(0xF4F7FF)
    return shade


def wing_demon(root, fy):
    top = 0.18 + 0.15 * root * root
    fingers = 4
    k = (root * fingers) % 1
    bottom = 0.55 + 0.3 * root - 0.18 * math.sin(k * math.pi)
    if not (top < fy < bottom):
        return None
    if abs(k - 0.0) < 0.05 or abs(k - 1.0) < 0.05 or fy < top + 0.04:
        return rgb(0x1A0A0A)
    return mix(rgb(0x7A0E16), rgb(0x2E050A), (fy - top) / (bottom - top))


def wing_butterfly(root, fy):
    upper = ((root - 0.55) / 0.5) ** 2 + ((fy - 0.28) / 0.28) ** 2
    lower = ((root - 0.4) / 0.4) ** 2 + ((fy - 0.72) / 0.25) ** 2
    if upper > 1 and lower > 1:
        return None
    d = min(upper, lower)
    if d > 0.8:
        return rgb(0x14142B)
    if 0.35 < d < 0.45:
        return rgb(0xFFFFFF)
    return mix(rgb(0x39D5FF), rgb(0x9B51E0), d / 0.8)


def make_wings(name, shader):
    img = Image(32 * SCALE, 32 * SCALE)
    img.fill_region(0, 0, 16, 20, lambda fx, fy: shader(fx, fy))
    img.fill_region(16, 0, 16, 20, lambda fx, fy: shader(1 - fx, fy))
    img.save(f"{OUT}/wings/{name}.png")


# --- Hats: 64x32. Colors are painted per region; the models use flat colors plus a band. ---

def make_top_hat():
    img = Image(64 * SCALE, 32 * SCALE)
    img.fill_region(0, 0, 64, 32, lambda fx, fy: rgb(0x16161C))
    # Crown box at (0,12), 8x8x8: side faces span v 20..28. Red band near the bottom of the sides.
    img.fill_region(0, 25, 32, 2, lambda fx, fy: rgb(0xB3122E))
    img.fill_region(8, 12, 8, 8, lambda fx, fy: mix(rgb(0x22222A), rgb(0x16161C), fy))
    img.save(f"{OUT}/hat/top_hat.png")


def make_crown():
    rng = random.Random(3)
    img = Image(64 * SCALE, 32 * SCALE)

    def gold(fx, fy):
        return mix(rgb(0xFFE27A), rgb(0xC98A12), fy * 0.8 + rng.random() * 0.2)
    img.fill_region(0, 0, 64, 32, gold)
    for _ in range(18):
        u, v = rng.randrange(0, 60), rng.randrange(0, 28)
        color = rng.choice([rgb(0xE0115F), rgb(0x1E90FF), rgb(0x2ECC71)])
        img.fill_region(u, v, 1, 1, lambda fx, fy, c=color: c)
    img.save(f"{OUT}/hat/crown.png")


# --- Halo: 16x16, pale so the model can tint it. Rendered with an emissive render type. ---

def make_halo():
    img = Image(16 * SCALE, 16 * SCALE)
    img.fill_region(0, 0, 16, 16, lambda fx, fy: mix(rgb(0xFFFFFF), rgb(0xE8E8E8), fy))
    img.save(f"{OUT}/halo/halo.png")


if __name__ == "__main__":
    make_cape("lucent", cape_lucent())
    make_cape("flame", cape_flame())
    make_cape("ocean", cape_ocean())
    make_cape("galaxy", cape_galaxy())
    make_wings("angel", wing_angel)
    make_wings("demon", wing_demon)
    make_wings("butterfly", wing_butterfly)
    make_top_hat()
    make_crown()
    make_halo()
