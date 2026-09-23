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
SCALE = 8


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


# --- Wings ---------------------------------------------------------------------------------------
# Each wing shape has its own layout; the constants mirror CosmeticModels.java, keep them in sync.
# Flat parts are 0-thick boxes: front face at (u, v), back face at (u + w, v), mirrored.

def flat_part(img, u, v, w, h, shader):
    """Paints both faces of a 0-thick box. shader(x, y) gets texel coordinates within the part."""
    img.fill_region(u, v, w, h, lambda fx, fy: shader(fx * w, fy * h))
    img.fill_region(u + w, v, w, h, lambda fx, fy: shader((1 - fx) * w, fy * h))


def darken(color, amount):
    return mix(color, (0, 0, 0, color[3]), amount)


# Feathered wings (angel, phoenix, raven). Texture 64x32.
FEATHERS = {
    # name: (u, v, width, length, asymmetry of the leading vane)
    "primary": (0, 0, 4, 20, 0.55),
    "secondary": (8, 0, 4, 16, 0.8),
    "covert": (16, 0, 3, 9, 1.0),
    "small_covert": (22, 0, 3, 6, 1.0),
}


def feather_shader(palette, width, length, asymmetry, seed):
    rng = random.Random(seed)
    notches = [(rng.uniform(0.35, 0.8), rng.uniform(0.02, 0.05)) for _ in range(2)]

    def shader(x, y):
        fx = x / width
        fy = y / length
        # Half-width of the vane along the feather: a bare quill at the root, a rounded tip.
        if fy < 0.1:
            half = 0.0
        else:
            half = 0.47 * math.sin(min(1.0, (fy - 0.1) / 0.3) * math.pi / 2)
        if fy > 0.75:
            half *= math.sqrt(max(0.0, 1 - ((fy - 0.75) / 0.25) ** 2))
        offset = fx - 0.5
        if offset < 0:
            half *= asymmetry
        for notch_y, notch_size in notches:
            if abs(fy - notch_y) < notch_size and offset > 0:
                half *= 0.7
        shaft = abs(offset) < 0.045 and fy < 0.97
        if not shaft and abs(offset) >= half:
            return None
        if shaft:
            return palette["shaft"]
        t = fy
        color = mix(palette["root"], palette["tip"], t) if "mid" not in palette else (
            mix(palette["root"], palette["mid"], t * 2) if t < 0.5 else mix(palette["mid"], palette["tip"], t * 2 - 1))
        # Barbs slant away from the shaft.
        barb = math.sin((fy * 55) - abs(offset) * 30)
        if barb > 0.6:
            color = darken(color, 0.08)
        if "sheen" in palette and barb < -0.7:
            color = mix(color, palette["sheen"], 0.35)
        # Darker rim so overlapping feathers read as separate.
        if abs(offset) > half - 0.07:
            color = darken(color, 0.18)
        return color
    return shader


def make_feathered_wings(name, palette):
    img = Image(64 * SCALE, 32 * SCALE)
    for index, (kind, (u, v, w, h, asym)) in enumerate(FEATHERS.items()):
        flat_part(img, u, v, w, h, feather_shader(palette, w, h, asym, index))
    # Bones: humerus, forearm and hand boxes live in u 32..64, v 0..12.
    img.fill_region(32, 0, 32, 12, lambda fx, fy: mix(palette["bone"], darken(palette["bone"], 0.15), fy))
    img.save(f"{OUT}/wings/{name}.png")


# Dragon wings (demon, void). Texture 64x64.
DRAGON_FINGER_ANGLES = [0.25, 0.7, 1.15, 1.6]
DRAGON_FINGER_LENGTHS = [16, 15, 13, 10]
DRAGON_ARM = 6
DRAGON_FOREARM = 9
# Membranes between neighbouring fingers, attached to the first one: (u, v, width, height)
DRAGON_MEMBRANES = [(0, 0, 16, 8), (0, 8, 15, 8), (0, 16, 13, 7)]
# Trailing membrane from the last finger back to the body, attached to the arm.
DRAGON_TRAILING = (0, 24, 15, 11)


def point_in_triangle(p, a, b, c):
    def sign(p1, p2, p3):
        return (p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1])
    d1, d2, d3 = sign(p, a, b), sign(p, b, c), sign(p, c, a)
    return not ((d1 < 0 or d2 < 0 or d3 < 0) and (d1 > 0 or d2 > 0 or d3 > 0))


def scallop_inside(p, edge_a, edge_b, depth):
    """True if p is on the inner side of a concave (scalloped) edge from edge_a to edge_b."""
    ex, ey = edge_b[0] - edge_a[0], edge_b[1] - edge_a[1]
    length = math.hypot(ex, ey)
    t = ((p[0] - edge_a[0]) * ex + (p[1] - edge_a[1]) * ey) / (length * length)
    dist = ((p[0] - edge_a[0]) * ey - (p[1] - edge_a[1]) * ex) / length
    return dist < -depth * length * math.sin(math.pi * max(0.0, min(1.0, t)))


def membrane_color(palette, x, y, span):
    t = min(1.0, math.hypot(x, y) / span)
    color = mix(palette["membrane_root"], palette["membrane_edge"], t)
    vein = math.sin(math.atan2(y, x) * 26 + math.hypot(x, y) * 0.5)
    if vein > 0.93:
        color = mix(color, palette["vein"], 0.6)
    return color


def make_dragon_wings(name, palette):
    img = Image(64 * SCALE, 64 * SCALE)
    delta = DRAGON_FINGER_ANGLES[1] - DRAGON_FINGER_ANGLES[0]
    for k, (u, v, w, h) in enumerate(DRAGON_MEMBRANES):
        tip = (DRAGON_FINGER_LENGTHS[k], 0.0)
        next_length = DRAGON_FINGER_LENGTHS[k + 1]
        next_tip = (next_length * math.cos(delta), next_length * math.sin(delta))

        def shader(x, y, tip=tip, next_tip=next_tip):
            p = (x, y)
            if not point_in_triangle(p, (0, 0), tip, next_tip):
                return None
            if not scallop_inside(p, tip, next_tip, 0.15):
                return None
            return membrane_color(palette, x, y, tip[0])
        flat_part(img, u, v, w, h, shader)

    u, v, w, h = DRAGON_TRAILING
    reach = DRAGON_ARM + DRAGON_FOREARM
    last_angle = DRAGON_FINGER_ANGLES[-1]
    last_tip = (reach + DRAGON_FINGER_LENGTHS[-1] * math.cos(last_angle), DRAGON_FINGER_LENGTHS[-1] * math.sin(last_angle))
    waist = (0.0, 10.5)

    def trailing(x, y):
        p = (x, y)
        quad = [(0, 0), (reach, 0), last_tip, waist]
        if not (point_in_triangle(p, quad[0], quad[1], quad[2]) or point_in_triangle(p, quad[0], quad[2], quad[3])):
            return None
        if not scallop_inside(p, last_tip, waist, 0.1):
            return None
        return membrane_color(palette, x, y, reach)
    flat_part(img, u, v, w, h, trailing)

    bone = lambda fx, fy: mix(palette["bone"], darken(palette["bone"], 0.4), fy)
    img.fill_region(32, 0, 32, 12, bone)
    img.fill_region(0, 40, 64, 8, bone)
    img.save(f"{OUT}/wings/{name}.png")


# Butterfly wings (morpho, monarch). Texture 64x32.
BUTTERFLY_FORE = (0, 0, 14, 12)
BUTTERFLY_HIND = (0, 12, 10, 10)


def butterfly_shader(palette, w, h, root, angle_from, angle_to, reach):
    def shader(x, y):
        dx, dy = x - root[0], root[1] - y  # y up
        angle = math.degrees(math.atan2(dy, dx))
        if not (angle_from <= angle <= angle_to):
            return None
        a = (angle - angle_from) / (angle_to - angle_from)
        limit = reach * (0.72 + 0.28 * math.sin(a * math.pi))
        r = math.hypot(dx, dy)
        if r > limit:
            return None
        edge = r / limit
        if edge > 0.8:
            spot = math.sin(a * 40) > 0.55 and 0.85 < edge < 0.95
            return palette["spot"] if spot else palette["border"]
        if palette.get("veins") and abs(math.sin(math.radians(angle) * 9)) < 0.08:
            return palette["border"]
        return mix(palette["inner"], palette["outer"], edge / 0.8)
    return shader


def make_butterfly_wings(name, palette):
    img = Image(64 * SCALE, 32 * SCALE)
    u, v, w, h = BUTTERFLY_FORE
    flat_part(img, u, v, w, h, butterfly_shader(palette, w, h, (0.0, h - 1.0), 5, 95, 14.5))
    u, v, w, h = BUTTERFLY_HIND
    flat_part(img, u, v, w, h, butterfly_shader(palette, w, h, (0.0, 1.0), -95, -5, 10.0))
    # The thin body-side hinge box.
    img.fill_region(32, 0, 8, 4, lambda fx, fy: palette["border"])
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
    make_feathered_wings("angel", {"root": rgb(0xFFFFFF), "tip": rgb(0xD5DFEF), "shaft": rgb(0xF2F4FA), "bone": rgb(0xF4F6FB)})
    make_feathered_wings("phoenix", {"root": rgb(0xFFE066), "mid": rgb(0xFF8A00), "tip": rgb(0xC81E14), "shaft": rgb(0xFFF1B8), "bone": rgb(0xFFB02E)})
    make_feathered_wings("raven", {"root": rgb(0x2A2A33), "tip": rgb(0x0C0C12), "shaft": rgb(0x5A5A6E), "bone": rgb(0x1E1E24), "sheen": rgb(0x4B3C8C)})
    make_dragon_wings("demon", {"membrane_root": rgb(0x8E1520), "membrane_edge": rgb(0x3A060C), "vein": rgb(0xD8434F), "bone": rgb(0x2A1414)})
    make_dragon_wings("void", {"membrane_root": rgb(0x3E1466), "membrane_edge": rgb(0x0E0520), "vein": rgb(0xC07CFF), "bone": rgb(0x14101C)})
    make_butterfly_wings("butterfly", {"inner": rgb(0x5BD8FF), "outer": rgb(0x1560D8), "border": rgb(0x0C0F1E), "spot": rgb(0xFFFFFF)})
    make_butterfly_wings("monarch", {"inner": rgb(0xFFB23F), "outer": rgb(0xE8620C), "border": rgb(0x14100C), "spot": rgb(0xFFFFFF), "veins": True})
    make_top_hat()
    make_crown()
    make_halo()
