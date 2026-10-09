#!/usr/bin/env python3
"""Generate RuneCraft client assets (textures, models, blockstates, lang) and block loot tables.

Reads build/catalog.json (produced by tools/run_core_tests.sh) and writes everything under
src/main/resources/. Deterministic (seeded randomness) and idempotent (overwrites outputs).

Usage: python3 tools/gen_assets.py
Requires: Python 3 + Pillow.
"""
import json
import math
import os
import random
import sys
import zlib
from collections import Counter

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "build", "catalog.json")
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "runecraft")
NS = "runecraft"

COUNTS = Counter()
WRITTEN_JSON = []
FALLBACK_SHAPES = set()


# ----------------------------------------------------------------------------------------------
# I/O helpers
# ----------------------------------------------------------------------------------------------

def _ensure(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)


def write_json(path, obj, category, sort_keys=False):
    _ensure(path)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, sort_keys=sort_keys, ensure_ascii=False)
        f.write("\n")
    WRITTEN_JSON.append(path)
    COUNTS[category] += 1


def save_png(img, path, category):
    _ensure(path)
    img.save(path, optimize=True)
    COUNTS[category] += 1


def rng_for(*keys):
    """Deterministic RNG per asset (independent of generation order)."""
    return random.Random(zlib.crc32("|".join(map(str, keys)).encode()))


# ----------------------------------------------------------------------------------------------
# Colour helpers
# ----------------------------------------------------------------------------------------------

def rgb(c):
    if isinstance(c, int):
        return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
    return tuple(c[:3])


def rgba(c, a=255):
    r, g, b = rgb(c)
    return (r, g, b, a)


def shade(c, f):
    r, g, b = rgb(c)
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))), max(0, min(255, int(b * f))))


def mix(c1, c2, t):
    a, b = rgb(c1), rgb(c2)
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


WHITE = (255, 255, 255)
BLACK = (0, 0, 0)
WOOD = (0x8A, 0x5A, 0x2A)
DARK_WOOD = (0x5A, 0x3A, 0x1A)
GOLD = (0xE8, 0xC0, 0x30)
STEEL = (0xA8, 0xA8, 0xB0)
GLASS = (0xC8, 0xE0, 0xF0)


class Pal:
    """Five-step ramp derived from one base colour."""

    def __init__(self, c):
        c = rgb(c)
        lum = (c[0] * 299 + c[1] * 587 + c[2] * 114) / 1000
        self.m = c
        self.l = mix(c, WHITE, 0.30)
        self.h = mix(c, WHITE, 0.60)
        self.d = shade(c, 0.70) if lum > 40 else mix(c, BLACK, 0.3)
        self.dd = shade(c, 0.45)
        if lum < 50:  # very dark colours need lifted highlights to read at all
            self.l = mix(c, WHITE, 0.22)
            self.h = mix(c, WHITE, 0.45)
            self.m = mix(c, WHITE, 0.08)


def new_img(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((int(x), int(y)), rgba(c, a))


def opaque(img, x, y):
    return 0 <= x < img.width and 0 <= y < img.height and img.getpixel((x, y))[3] > 0


def line(img, x0, y0, x1, y1, c, a=255):
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        put(img, x0, y0, c, a)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def mask(fn, w=16, h=16):
    m = Image.new("L", (w, h), 0)
    fn(ImageDraw.Draw(m))
    return {(x, y) for y in range(h) for x in range(w) if m.getpixel((x, y)) > 0}


def bevel(img, pix, pal, noise=None, hl=True):
    """Fill a pixel set with top-left lighting: light rim on the up/left, dark on the down/right."""
    pix = set(pix)
    for (x, y) in pix:
        up_left = (x - 1, y) in pix and (x, y - 1) in pix
        down_right = (x + 1, y) in pix and (x, y + 1) in pix
        if not up_left:
            c = pal.h if hl and (x - 1, y) not in pix and (x, y - 1) not in pix else pal.l
        elif not down_right:
            c = pal.d
        else:
            c = pal.m
            if noise is not None and noise.random() < 0.18:
                c = pal.l if noise.random() < 0.5 else pal.d
        put(img, x, y, c)


def outline(img, factor=0.38, diag=False):
    """Add a 1px dark outline (tinted by the neighbouring pixel) around opaque pixels."""
    src = img.copy()
    w, h = img.size
    nbrs = [(1, 0), (-1, 0), (0, 1), (0, -1)]
    if diag:
        nbrs += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] > 0:
                continue
            for dx, dy in nbrs:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h:
                    p = src.getpixel((nx, ny))
                    if p[3] > 200:
                        img.putpixel((x, y), rgba(shade(p, factor)))
                        break


def rdiag(img, fn, x0=1, y0=14):
    """Rasterise a shape defined in a diagonal local frame.

    u runs along the handle->tip axis (bottom-left to top-right), v is perpendicular
    (v<0 = towards top-left). Both take half-integer values on the pixel grid.
    """
    for y in range(img.height):
        for x in range(img.width):
            u = ((x - x0) - (y - y0)) / 2.0
            v = ((x - x0) + (y - y0)) / 2.0
            c = fn(u, v)
            if c is not None:
                put(img, x, y, c)


def metal_shade(v, pal, w):
    if v < -w / 2:
        return pal.h
    if v < 0:
        return pal.l
    if v == 0:
        return pal.l
    if v <= w / 2:
        return pal.m
    return pal.d


# ----------------------------------------------------------------------------------------------
# Item shape drawers: draw(img, pal, item, rng) -> optional list of post-outline pixels
# ----------------------------------------------------------------------------------------------

def _handle(u, v, u0, u1, col=WOOD):
    if u0 <= u <= u1 and v == 0:
        return col
    return None


def sword_like(blade_end, blade_w, guard_u, guard_w, handle_end=None, curve=0.0):
    def draw(img, pal, item, rng):
        he = handle_end if handle_end is not None else guard_u - 0.5
        gp = Pal(shade(pal.m, 0.8))

        def fn(u, v):
            if u < 0:
                return None
            if u <= 0.5 and v == 0:
                return GOLD if item.get("tier") != "black" else pal.d
            c = _handle(u, v, 0.5, he, DARK_WOOD if u % 1 == 0 else WOOD)
            if c:
                return c
            if guard_u <= u <= guard_u + 0.5 and abs(v) <= guard_w:
                return gp.l if v < 0 else gp.m
            if guard_u + 0.5 < u <= blade_end:
                off = curve * math.sin(math.pi * (u - guard_u) / (blade_end - guard_u + 1)) if curve else 0
                vv = v - off
                w = blade_w
                if u > blade_end - 1.5:
                    w = min(w, 0.5)
                if u > blade_end - 0.5:
                    w = 0
                if abs(vv) <= w:
                    if vv < 0:
                        return pal.h
                    if vv == 0:
                        return pal.l
                    return pal.m if vv < w else pal.d
            return None

        rdiag(img, fn)
    return draw


def draw_warhammer(img, pal, item, rng):
    def fn(u, v):
        if 8.5 <= u <= 11.5 and abs(v) <= 2.5:
            if u == 8.5 or u == 11.5:
                return pal.d
            return pal.l if v < 0 else pal.m
        if 11.5 < u <= 12.5 and v == 0:
            return pal.l
        return _handle(u, v, 0, 8.5, WOOD)
    rdiag(img, fn)


def draw_battleaxe(img, pal, item, rng):
    def fn(u, v):
        if 7 <= u <= 12.5 and 0 < abs(v) <= 4:
            t = (u - 9.75) / 2.75
            lim = 1 + 3 * max(0.0, 1 - t * t)
            if abs(v) <= lim:
                if abs(v) > lim - 0.7:
                    return pal.h
                return pal.l if v < 0 else pal.m
        if 12.5 < u <= 13 and v == 0:
            return pal.l
        return _handle(u, v, 0, 12.5, WOOD)
    rdiag(img, fn)


def draw_axe(img, pal, item, rng):
    def fn(u, v):
        if 8 <= u <= 11.5 and 0 < v <= 4:
            t = (u - 9.75) / 1.9
            lim = 1 + 3 * max(0.0, 1 - t * t * 0.8)
            if v <= lim:
                if v > lim - 0.7:
                    return pal.h
                return pal.l if v < 1.5 else pal.m
        if 9 <= u <= 11 and -1.5 <= v <= 0 and v != 0:
            return pal.d
        return _handle(u, v, 0, 11.5, WOOD)
    rdiag(img, fn)


def draw_pickaxe(img, pal, item, rng):
    def fn(u, v):
        if abs(v) <= 5.5:
            centre = 11.2 - 0.12 * v * v
            thick = 1.0 if abs(v) < 3.5 else 0.5
            if abs(u - centre) <= thick / 2 + 0.01 or (abs(v) < 3.5 and abs(u - centre) <= 0.5):
                return pal.l if u >= centre else pal.m
        return _handle(u, v, 0, 11, WOOD)
    rdiag(img, fn)


def draw_hammer(img, pal, item, rng):
    p = Pal(STEEL)

    def fn(u, v):
        if 9 <= u <= 11 and abs(v) <= 2:
            return p.l if u < 10.5 and v < 0 else p.m if u < 10.5 else p.d
        return _handle(u, v, 0, 9, WOOD)
    rdiag(img, fn)


def draw_knife(img, pal, item, rng):
    p = Pal(STEEL)

    def fn(u, v):
        if 0.5 <= u <= 4 and abs(v) <= 0.5:
            return DARK_WOOD if v > 0 else WOOD
        if 4.5 <= u <= 10.5 and -0.5 <= v <= 0.5:
            if u > 9 and v != 0:
                return None
            return p.h if v < 0 else p.l if v == 0 else p.m
        return None
    rdiag(img, fn, 2, 13)


def draw_chisel(img, pal, item, rng):
    p = Pal(STEEL)

    def fn(u, v):
        if 1 <= u <= 5 and abs(v) <= 0.5:
            return WOOD if v <= 0 else DARK_WOOD
        if 5.5 <= u <= 10 and v == 0:
            return p.l
        if u == 10.5 and abs(v) <= 0.5:
            return p.h
        return None
    rdiag(img, fn, 2, 13)


def draw_needle(img, pal, item, rng):
    p = Pal(STEEL)

    def fn(u, v):
        if 2 <= u <= 12 and v == 0 and u != 3:
            return p.h if u > 10 else p.l
        if 2.5 <= u <= 3.5 and abs(v) == 0.5:
            return p.m
        return None
    rdiag(img, fn)


def draw_harpoon(img, pal, item, rng):
    p = Pal(STEEL)

    def fn(u, v):
        if 9.5 <= u <= 13.5 and abs(v) <= (13.5 - u) * 0.45:
            return p.l if v <= 0 else p.m
        if 9 <= u <= 10 and abs(v) == 1.5:
            return p.m
        return _handle(u, v, 0, 9.5, WOOD)
    rdiag(img, fn)


def draw_rod(img, pal, item, rng):
    p = Pal(pal.m)

    def fn(u, v):
        if 1.5 <= u <= 3 and 0 < v <= 1.5:
            return STEEL if v < 1.5 else shade(STEEL, .7)
        if 0 <= u <= 2 and v == 0:
            return DARK_WOOD
        if 2 < u <= 13 and v == 0:
            return p.m if u < 8 else p.l
        return None
    rdiag(img, fn)
    return [(14, y, (230, 230, 230)) for y in range(3, 10)] + [(14, 10, (200, 60, 60))]


def draw_dibber(img, pal, item, rng):
    def fn(u, v):
        if 3 <= u <= 11.5 and v == 0:
            return WOOD if u < 10 else shade(WOOD, 0.8)
        if 2 <= u <= 2.5 and abs(v) <= 2:
            return DARK_WOOD if v > 0 else WOOD
        return None
    rdiag(img, fn)


def draw_staff(img, pal, item, rng):
    def fn(u, v):
        if 12 <= u <= 13 and abs(v) <= 0.5:
            return pal.l if v <= 0 else pal.d
        return _handle(u, v, 0, 12, WOOD if u % 2 else mix(WOOD, pal.m, 0.3))
    rdiag(img, fn)


def draw_staff_orb(img, pal, item, rng):
    def fn(u, v):
        d = math.hypot(u - 11.5, v)
        if d <= 1.6:
            if u - 11.5 + v < -0.8:
                return pal.h
            return pal.l if d < 0.9 else pal.m
        if 10 <= u <= 10.5 and abs(v) <= 1.5:
            return GOLD
        return _handle(u, v, 0, 10, WOOD)
    rdiag(img, fn)


def draw_arrow(img, pal, item, rng):
    fl = (235, 235, 235)

    def fn(u, v):
        if 10.5 <= u <= 13.5 and abs(v) <= (13.5 - u) * 0.5:
            return pal.l if v <= 0 else pal.m
        if 0 <= u <= 3 and 0.5 <= abs(v) <= 1.5 - (u * 0.2) and abs(v) >= 0.5:
            return fl if v < 0 else (200, 60, 50)
        return _handle(u, v, 0, 10.5, WOOD)
    rdiag(img, fn)


def draw_headless(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 3 and 0.5 <= abs(v) <= 1.5 - (u * 0.2):
            return (235, 235, 235) if v < 0 else (200, 200, 200)
        return _handle(u, v, 0, 11, WOOD)
    rdiag(img, fn)


def draw_shaft(img, pal, item, rng):
    c = pal.m

    def fn(u, v):
        for off, a, b in ((-2.5, 2.5, 11), (0, 1, 12), (2.5, 2, 10.5)):
            if v == off and a <= u <= b:
                return c if u % 2 else shade(c, 0.85)
        return None
    rdiag(img, fn)


def draw_arrowtips(img, pal, item, rng):
    for (x, y) in ((3, 4), (9, 3), (5, 10), (11, 9)):
        put(img, x, y, pal.h)
        for dx in (-1, 0, 1):
            put(img, x + dx, y + 1, pal.l if dx < 1 else pal.m)
        for dx in (-1, 0, 1):
            put(img, x + dx, y + 2, pal.m if dx < 1 else pal.d)
        put(img, x, y + 3, WOOD)


def bow(long, strung):
    def draw(img, pal, item, rng):
        u_end = 13.5 if long else 12.5
        bulge = (3.6 if long else 3.0) if strung else 1.6

        def fn(u, v):
            if not (0.5 <= u <= u_end):
                return None
            vc = -bulge * math.sin(math.pi * (u - 0.5) / (u_end - 0.5))
            if abs(v - vc) <= 0.5:
                mid = abs(u - (u_end + 0.5) / 2) < 1.2
                if mid:
                    return DARK_WOOD
                return pal.l if v < vc else pal.m
            return None
        rdiag(img, fn)
        if strung:
            post = []

            def sfn(u, v):
                if 0.5 <= u <= u_end and v == 0:
                    post.append(None)
                return None
            pts = []
            for y in range(16):
                for x in range(16):
                    u = ((x - 1) - (y - 14)) / 2.0
                    v = ((x - 1) + (y - 14)) / 2.0
                    if 1 <= u <= u_end - 0.5 and v == 0.5 and not opaque(img, x, y):
                        pts.append((x, y, (225, 225, 210)))
            return pts
    return draw


def draw_log(img, pal, item, rng):
    p = pal

    def fn(u, v):
        if 1.5 <= u <= 12 and abs(v) <= 2.5:
            if u >= 11.5:
                ring = abs(v) % 1.5 < 0.5
                return mix(p.h, (230, 200, 150), 0.5) if not ring else mix(p.l, (200, 160, 110), 0.5)
            if v <= -2:
                return p.l
            if v >= 2:
                return p.dd
            return p.d if (u * 2 + v * 3) % 5 < 1 else p.m
        return None
    rdiag(img, fn)


def draw_plank(img, pal, item, rng):
    def fn(u, v):
        if 0.5 <= u <= 13 and abs(v) <= 2:
            if v == -2:
                return pal.l
            if v == 2:
                return pal.d
            if v == 0 and int(u) % 4 != 0:
                return pal.d
            return pal.m
        return None
    rdiag(img, fn)


def draw_bones(img, pal, item, rng, big=False):
    r = 1.6 if big else 1.2
    a, b = (2, 11.5) if big else (3, 10.5)

    def fn(u, v):
        for uu in (a, b):
            for vv in (-1, 1):
                if math.hypot(u - uu, v - vv) <= r:
                    return pal.h if v < vv else pal.l
        if a <= u <= b and abs(v) <= (1 if big else 0.5):
            return pal.l if v < 0 else pal.m
        return None
    rdiag(img, fn)


def draw_big_bones(img, pal, item, rng):
    draw_bones(img, pal, item, rng, big=True)


def draw_coins(img, pal, item, rng):
    p = Pal(GOLD)
    for (cx, cy) in ((5, 11), (10, 10), (7, 7), (9, 4)):
        for y in range(cy - 2, cy + 3):
            for x in range(cx - 3, cx + 4):
                dx, dy = (x - cx) / 3.4, (y - cy) / 2.4
                if dx * dx + dy * dy <= 1:
                    c = p.m
                    if y == cy - 2 or (y == cy - 1 and abs(x - cx) == 3):
                        c = p.h
                    elif y == cy + 2 or (x - cx) == 3:
                        c = p.d
                    put(img, x, y, c)
        put(img, cx, cy, p.l)


def draw_ore(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(3, 6), (6, 3), (11, 3), (13, 7), (13, 11), (10, 13), (5, 13), (2, 10)], fill=255))
    bevel(img, pix, Pal(mix(pal.m, (110, 105, 100), 0.35)), noise=rng)
    for _ in range(6):
        x, y = rng.randint(4, 11), rng.randint(5, 11)
        put(img, x, y, pal.l)
        put(img, x + 1, y, pal.m)


def draw_essence(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(4, 3), (9, 2), (13, 6), (12, 12), (7, 14), (3, 10)], fill=255))
    bevel(img, pix, pal)
    line(img, 5, 5, 10, 11, pal.h)
    put(img, 9, 5, WHITE)


def draw_clay(img, pal, item, rng):
    pix = mask(lambda d: d.ellipse((2, 4, 13, 13), fill=255))
    bevel(img, pix, pal, noise=rng)


def draw_bar(img, pal, item, rng):
    for y in range(5, 8):
        for x in range(5 - (y - 5), 11 + (y - 5)):
            put(img, x, y, pal.l if y > 5 else pal.h)
    for y in range(8, 12):
        for x in range(2, 14):
            put(img, x, y, pal.d if y == 11 else pal.m)
    for x in range(2, 14):
        put(img, x, 8, pal.h)
    put(img, 13, 9, pal.d)
    put(img, 13, 10, pal.d)


def draw_tinderbox(img, pal, item, rng):
    p = Pal(STEEL)
    pix = {(x, y) for x in range(3, 13) for y in range(6, 12)}
    bevel(img, pix, p)
    for x in range(3, 13):
        put(img, x, 8, p.d)
    for (x, y) in ((6, 4), (7, 4), (6, 5), (7, 5), (8, 5)):
        put(img, x, y, (90, 90, 90))


def draw_net(img, pal, item, rng):
    c = Pal(mix(pal.m, (200, 180, 140), 0.3))
    for y in range(2, 15):
        for x in range(2, 15):
            if (x - 8) ** 2 / 36 + (y - 8) ** 2 / 36 <= 1 and ((x + y) % 3 == 0 or (x - y) % 3 == 0):
                put(img, x, y, c.l if x + y < 16 else c.m)
    return []


def draw_pot(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((3, 5, 12, 14), fill=255), d.rectangle((5, 3, 10, 6), fill=255)))
    bevel(img, pix, pal)
    for x in range(5, 11):
        put(img, x, 3, pal.h)
    for x in range(6, 10):
        put(img, x, 4, pal.dd)


def draw_mould(img, pal, item, rng):
    pix = {(x, y) for x in range(2, 14) for y in range(3, 13)}
    bevel(img, pix, pal)
    iid = item["id"]
    dark = pal.dd
    if iid.startswith("ring"):
        for (x, y) in mask(lambda d: d.ellipse((5, 5, 10, 10), outline=255)):
            put(img, x, y, dark)
    elif iid.startswith("necklace"):
        for (x, y) in mask(lambda d: d.arc((4, 2, 11, 11), 20, 160, fill=255)):
            put(img, x, y, dark)
        put(img, 7, 10, dark)
        put(img, 8, 10, dark)
    else:
        for (x, y) in mask(lambda d: d.ellipse((5, 6, 10, 11), fill=255)):
            put(img, x, y, dark)
        line(img, 7, 4, 7, 5, dark)
        line(img, 8, 4, 8, 5, dark)


def draw_snare(img, pal, item, rng):
    for (x, y) in mask(lambda d: d.ellipse((3, 6, 12, 13), outline=255)):
        put(img, x, y, (220, 210, 170))
    line(img, 8, 2, 8, 9, WOOD)
    line(img, 7, 2, 9, 2, DARK_WOOD)


def draw_box_trap(img, pal, item, rng):
    p = Pal(WOOD)
    front = {(x, y) for x in range(2, 12) for y in range(7, 14)}
    bevel(img, front, p)
    for y in range(4, 7):
        for x in range(2 + (7 - y), 12 + (7 - y)):
            put(img, x, y, p.h)
    for y in range(4, 11):
        for x in (12, 13):
            if y - 4 <= (x - 11) * 3 + 3:
                put(img, x, y, p.d)
    for x in range(3, 11, 2):
        line(img, x, 8, x, 12, p.d)


def draw_bait(img, pal, item, rng):
    for k, (ox, oy) in enumerate(((3, 4), (6, 8), (4, 11))):
        for i in range(8):
            x = ox + i
            y = oy + int(round(math.sin(i * 1.1 + k) * 1.2))
            put(img, x, y, pal.l if i % 3 == 0 else pal.m)


def draw_gem(img, pal, item, rng):
    pts = [(4, 3), (11, 3), (14, 7), (8, 14), (7, 14), (1, 7)]
    pix = mask(lambda d: d.polygon(pts, fill=255))
    for (x, y) in pix:
        if y <= 6:
            c = pal.l
        elif x < 7:
            c = pal.m
        else:
            c = pal.d
        put(img, x, y, c)
    for x in range(3, 13):
        put(img, x, 6, pal.h)
    put(img, 6, 4, WHITE)
    put(img, 5, 5, pal.h)


def draw_gem_uncut(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(4, 4), (8, 2), (12, 5), (13, 10), (9, 13), (4, 12), (2, 8)], fill=255))
    bevel(img, pix, Pal(mix(pal.m, (120, 120, 120), 0.25)), noise=rng)
    put(img, 6, 5, pal.h)


def draw_mark(img, pal, item, rng):
    pix = mask(lambda d: d.ellipse((2, 2, 13, 13), fill=255))
    bevel(img, pix, pal)
    dark = pal.dd
    for (x, y) in ((7, 4), (8, 4), (7, 5), (6, 6), (7, 6), (8, 6), (9, 7), (7, 7), (7, 8), (6, 9), (8, 9),
                   (5, 10), (9, 10), (5, 7)):
        put(img, x, y, dark)


def fish(img, body):
    p = body
    pix = mask(lambda d: (d.ellipse((1, 5, 11, 11), fill=255), d.polygon([(10, 8), (14, 4), (14, 12)], fill=255)))
    bevel(img, pix, p)
    line(img, 4, 5, 8, 4, p.d)
    put(img, 3, 7, BLACK)
    for x in range(3, 10):
        put(img, x, 10, p.l)


def draw_fish_raw(img, pal, item, rng):
    fish(img, pal)


def draw_fish_cooked(img, pal, item, rng):
    fish(img, Pal(mix(pal.m, (170, 110, 50), 0.55)))
    for x in (5, 7, 9):
        line(img, x, 6, x - 1, 9, shade(mix(pal.m, (170, 110, 50), 0.55), 0.6))


def draw_fish_burnt(img, pal, item, rng):
    fish(img, Pal((50, 40, 35)))


def meat(img, col):
    p = Pal(col)
    pix = mask(lambda d: d.ellipse((2, 2, 11, 11), fill=255))
    bevel(img, pix, p)
    bone = Pal((235, 228, 210))
    for i in range(4):
        put(img, 10 + i, 10 + i, bone.l)
    for (x, y) in ((13, 12), (12, 13), (14, 13), (13, 14)):
        put(img, x, y, bone.m)


def draw_meat_raw(img, pal, item, rng):
    meat(img, (200, 80, 80))


def draw_meat_cooked(img, pal, item, rng):
    meat(img, (140, 80, 40))


def draw_meat_burnt(img, pal, item, rng):
    meat(img, (45, 35, 30))


def draw_cake(img, pal, item, rng):
    body = Pal(pal.m)
    for y in range(7, 13):
        for x in range(2, 14):
            put(img, x, y, body.m if y < 12 else body.d)
    for x in range(2, 14):
        put(img, x, 9, (240, 240, 240) if item["id"] == "cake" else (200, 150, 100))
    for (x, y) in mask(lambda d: d.ellipse((2, 4, 13, 9), fill=255)):
        put(img, x, y, body.h)
    put(img, 7, 3, (220, 40, 40))
    put(img, 8, 3, (220, 40, 40))
    put(img, 7, 2, (60, 140, 40))


RUNE_SYMBOLS = {
    "air": ["..###..", ".#...#.", ".....#.", "..###..", ".#.....", ".#...#.", "..###.."],
    "water": ["...#...", "..###..", ".#####.", ".#####.", "#######", ".#####.", "..###.."],
    "earth": ["...#...", "..###..", "..###..", ".#####.", ".#####.", "#######", "#######"],
    "fire": ["...#...", "..##...", "..###.#", ".#####.", "##.####", "#######", ".#####."],
    "mind": ["..###..", ".#...#.", "#.#.#.#", "#.....#", "#..#..#", ".#...#.", "..###.."],
    "body": ["..###..", "..###..", "...#...", "#######", "...#...", "..#.#..", ".#...#."],
    "cosmic": ["...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#..."],
    "chaos": ["....##.", "...##..", "..##...", ".######", "...##..", "..##...", ".##...."],
    "astral": ["..###..", ".##....", "##.....", "##.....", "##.....", ".##....", "..###.."],
    "nature": ["....###", "..####.", ".####..", ".###...", ".#.....", "#......", "......."],
    "law": ["...#...", "...#...", "..#.#..", "..#.#..", ".#...#.", ".#...#.", "#######"],
    "death": [".#####.", "#######", "##.#.##", "#######", ".#####.", ".#.#.#.", "......."],
    "blood": ["...#...", "...#...", "..###..", ".##.##.", ".#####.", "..###..", "......."],
    "soul": ["..###..", ".#...#.", "#..#..#", "#.###.#", "#..#..#", ".#...#.", "..###.."],
    "wrath": ["#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#"],
}


def rune_symbol(name):
    return RUNE_SYMBOLS.get(name, RUNE_SYMBOLS["cosmic"])


def draw_rune(img, pal, item, rng):
    stone = Pal((150, 150, 155))
    pix = mask(lambda d: d.rounded_rectangle((1, 1, 14, 14), radius=3, fill=255))
    bevel(img, pix, stone, noise=rng)
    sym = rune_symbol(item["id"].replace("_rune", ""))
    for j, row in enumerate(sym):
        for i, ch in enumerate(row):
            if ch == "#":
                put(img, 4 + i + 1, 4 + j + 1, shade(pal.m, 0.35))
    for j, row in enumerate(sym):
        for i, ch in enumerate(row):
            if ch == "#":
                put(img, 4 + i, 4 + j, pal.l if (i + j) < 5 else pal.m)


def draw_full_helm(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((3, 1, 12, 12), fill=255), d.rectangle((3, 6, 12, 14), fill=255)))
    bevel(img, pix, pal)
    for x in range(5, 11):
        put(img, x, 7, BLACK)
    for y in range(8, 12):
        put(img, 7, y, BLACK)
        put(img, 8, y, BLACK)
    line(img, 7, 2, 7, 5, pal.h)


def draw_platebody(img, pal, item, rng, leather=False):
    pts = [(2, 3), (5, 2), (10, 2), (13, 3), (14, 9), (12, 9), (11, 14), (4, 14), (3, 9), (1, 9)]
    pix = mask(lambda d: d.polygon(pts, fill=255)) - {(x, y) for x in range(6, 10) for y in range(1, 4)}
    bevel(img, pix, pal, noise=rng if leather else None)
    for y in range(5, 14):
        put(img, 7 if not leather else 7, y, pal.d)
        if not leather:
            put(img, 8, y, pal.l)
    if leather:
        for x in range(4, 12, 2):
            put(img, x, 12, pal.h)
    else:
        for x in range(4, 12):
            put(img, x, 9, pal.d)


def draw_platelegs(img, pal, item, rng, leather=False):
    pix = {(x, y) for x in range(3, 13) for y in range(2, 6)}
    pix |= {(x, y) for x in range(3, 7) for y in range(6, 15)}
    pix |= {(x, y) for x in range(9, 13) for y in range(6, 15)}
    bevel(img, pix, pal, noise=rng if leather else None)
    for x in range(3, 13):
        put(img, x, 3, pal.dd if not leather else DARK_WOOD)
    if not leather:
        for y in (9, 10):
            for x in list(range(3, 7)) + list(range(9, 13)):
                put(img, x, y, pal.l if y == 9 else pal.d)


def draw_kiteshield(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(2, 2), (13, 2), (13, 7), (8, 14), (7, 14), (2, 7)], fill=255))
    bevel(img, pix, pal)
    em = Pal(shade(pal.m, 0.6))
    for y in range(3, 13):
        put(img, 7, y, em.m)
        put(img, 8, y, em.d)
    for x in range(3, 13):
        put(img, x, 6, em.m)


def draw_string(img, pal, item, rng):
    c = Pal(pal.m)
    for k, box in enumerate(((2, 4, 13, 12), (4, 5, 11, 11), (5, 6, 10, 10))):
        for (x, y) in mask(lambda d: d.ellipse(box, outline=255)):
            put(img, x, y, c.l if k % 2 == 0 else c.m)


def draw_flax(img, pal, item, rng):
    g = Pal((90, 140, 50))
    for i in range(10):
        put(img, 4 + i // 2, 14 - i, g.m)
        put(img, 7 + i // 3, 14 - i, g.l)
    for (x, y) in ((8, 3), (9, 3), (8, 2), (10, 4), (9, 4), (11, 3)):
        put(img, x, y, (90, 120, 230))
    put(img, 9, 3, (230, 230, 120))


def hide_shape(img, pal, rng, fur=False):
    pts = [(2, 2), (5, 4), (10, 4), (13, 2), (12, 7), (13, 13), (10, 11), (5, 11), (2, 13), (3, 7)]
    pix = mask(lambda d: d.polygon(pts, fill=255))
    bevel(img, pix, pal, noise=rng if fur else None)
    if fur:
        for _ in range(14):
            x, y = rng.randint(4, 11), rng.randint(4, 10)
            if (x, y) in pix:
                put(img, x, y, pal.h if rng.random() < 0.5 else pal.dd)


def draw_hide(img, pal, item, rng):
    hide_shape(img, pal, rng, fur=True)


def draw_leather(img, pal, item, rng):
    hide_shape(img, pal, rng)


def draw_fur(img, pal, item, rng):
    hide_shape(img, pal, rng, fur=True)


def draw_wool(img, pal, item, rng):
    pix = mask(lambda d: d.ellipse((2, 2, 13, 13), fill=255))
    bevel(img, pix, pal)
    for (x, y) in mask(lambda d: d.arc((4, 4, 11, 11), 200, 360, fill=255)):
        put(img, x, y, pal.d)
    for (x, y) in mask(lambda d: d.arc((5, 3, 12, 13), 30, 150, fill=255)):
        put(img, x, y, pal.d)


def draw_cloth(img, pal, item, rng):
    pix = {(x, y) for x in range(2, 14) for y in range(4, 13)}
    bevel(img, pix, pal)
    for x in range(2, 14):
        put(img, x, 8, pal.d)
    line(img, 2, 4, 6, 8, pal.l)


def draw_spice(img, pal, item, rng):
    pix = mask(lambda d: d.pieslice((2, 5, 13, 20), 180, 360, fill=255))
    bevel(img, pix, pal, noise=rng)
    for _ in range(8):
        x, y = rng.randint(4, 11), rng.randint(9, 12)
        put(img, x, y, (200, 60, 30) if rng.random() < 0.5 else pal.h)


def draw_cowl(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((2, 1, 13, 12), fill=255), d.rectangle((2, 7, 13, 14), fill=255)))
    face = mask(lambda d: d.ellipse((5, 5, 10, 13), fill=255))
    bevel(img, pix - face, pal, noise=rng)


def draw_leather_body(img, pal, item, rng):
    draw_platebody(img, pal, item, rng, leather=True)


def draw_chaps(img, pal, item, rng):
    draw_platelegs(img, pal, item, rng, leather=True)


def draw_boots(img, pal, item, rng):
    pix = set()
    for ox in (1, 8):
        pix |= {(x, y) for x in range(ox + 1, ox + 4) for y in range(4, 12)}
        pix |= {(x, y) for x in range(ox + 1, ox + 7) for y in range(10, 14)}
    bevel(img, pix, pal)
    for ox in (1, 8):
        for x in range(ox + 1, ox + 7):
            put(img, x, 13, pal.dd)


def draw_ring(img, pal, item, rng):
    band = Pal(GOLD)
    for (x, y) in mask(lambda d: d.ellipse((3, 5, 12, 13), outline=255, width=2)):
        put(img, x, y, band.l if x + y < 15 else band.m)
    if item["id"] != "gold_ring":
        g = Pal(pal.m)
        for (x, y, c) in ((7, 3, g.h), (8, 3, g.l), (7, 4, g.m), (8, 4, g.d), (6, 4, g.l), (9, 4, g.d)):
            put(img, x, y, c)


def draw_necklace(img, pal, item, rng):
    band = Pal(GOLD)
    for (x, y) in mask(lambda d: d.arc((2, 0, 13, 13), 10, 170, fill=255)):
        put(img, x, y, band.l if (x + y) % 2 else band.m)
    g = Pal(pal.m if item["id"] != "gold_necklace" else GOLD)
    for (x, y) in ((7, 13), (8, 13), (7, 14), (8, 14)):
        put(img, x, y, g.l if y == 13 else g.m)


def draw_amulet(img, pal, item, rng):
    for (x, y) in mask(lambda d: d.arc((3, -2, 12, 10), 0, 180, fill=255)):
        put(img, x, y, (220, 210, 170))
    pix = mask(lambda d: d.ellipse((5, 8, 10, 13), fill=255))
    bevel(img, pix, Pal(GOLD))
    put(img, 7, 10, pal.m)
    put(img, 8, 10, pal.l)
    put(img, 7, 11, pal.d)
    put(img, 8, 11, pal.m)


def vial_shape(img, liquid, extra=None):
    glass = Pal(GLASS)
    body = mask(lambda d: (d.ellipse((3, 6, 12, 14), fill=255), d.rectangle((6, 2, 9, 7), fill=255)))
    for (x, y) in body:
        put(img, x, y, glass.m, 110)
    if liquid is not None:
        lp = Pal(liquid)
        for (x, y) in body:
            if y >= 8:
                put(img, x, y, lp.l if y == 8 else (lp.m if x < 9 else lp.d))
    # glass rim & highlight
    for (x, y) in body:
        if (x - 1, y) not in body or (x + 1, y) not in body or (x, y + 1) not in body:
            if not (liquid is not None and y >= 8 and (x, y + 1) in body):
                put(img, x, y, glass.l, 230)
    put(img, 5, 9, WHITE)
    put(img, 5, 10, WHITE, 200)
    for x in range(6, 10):
        put(img, x, 1, (150, 110, 70))
    put(img, 6, 2, (150, 110, 70))
    put(img, 9, 2, (150, 110, 70))
    if extra:
        extra(img)


def draw_vial(img, pal, item, rng):
    vial_shape(img, None)
    return "no_outline"


def draw_vial_water(img, pal, item, rng):
    vial_shape(img, (60, 110, 220))
    return "no_outline"


def draw_potion(img, pal, item, rng):
    vial_shape(img, pal.m)
    return "no_outline"


def draw_potion_unf(img, pal, item, rng):
    vial_shape(img, mix(pal.m, (60, 110, 220), 0.4))
    return "no_outline"


def herb_shape(img, pal, rng, grimy=False):
    stem = shade(pal.m, 0.6)
    line(img, 3, 14, 8, 8, stem)
    leaves = [((7, 2, 11, 9), 0), ((2, 5, 7, 10), 0), ((8, 7, 14, 11), 0), ((4, 2, 8, 7), 0)]
    for box, _ in leaves:
        pix = mask(lambda d, b=box: d.ellipse(b, fill=255))
        bevel(img, pix, pal, noise=rng if grimy else None)
    for (x0, y0, x1, y1), _ in leaves:
        line(img, (x0 + x1) // 2 - 1, y1 - 1, (x0 + x1) // 2 + 1, y0 + 1, pal.d)
    if grimy:
        for _ in range(10):
            put(img, rng.randint(3, 12), rng.randint(3, 11), (90, 70, 40))


def draw_herb(img, pal, item, rng):
    herb_shape(img, pal, rng)


def draw_herb_grimy(img, pal, item, rng):
    herb_shape(img, Pal(mix(pal.m, (110, 100, 60), 0.45)), rng, grimy=True)


def draw_secondary(img, pal, item, rng):
    for (cx, cy, r) in ((6, 9, 3), (10, 7, 3), (9, 11, 2), (5, 5, 2)):
        pix = mask(lambda d, a=(cx - r, cy - r, cx + r, cy + r): d.ellipse(a, fill=255))
        bevel(img, pix, pal)


def draw_seed(img, pal, item, rng):
    for (cx, cy) in ((5, 6), (10, 5), (7, 10), (11, 11)):
        pix = mask(lambda d, a=(cx - 2, cy - 1, cx + 1, cy + 2): d.ellipse(a, fill=255))
        bevel(img, pix, pal)


def draw_produce(img, pal, item, rng):
    iid = item["id"]
    leaf = (60, 140, 40)
    if iid == "sweetcorn":
        pix = mask(lambda d: d.ellipse((5, 2, 10, 14), fill=255))
        bevel(img, pix, pal)
        for y in range(3, 14, 2):
            for x in range(6, 10, 2):
                put(img, x, y, pal.h)
        line(img, 4, 14, 4, 6, leaf)
        line(img, 11, 14, 11, 7, leaf)
        return
    if iid == "watermelon":
        pix = mask(lambda d: d.ellipse((1, 3, 14, 13), fill=255))
        bevel(img, pix, pal)
        for x in (4, 7, 10):
            line(img, x, 4, x + 1, 12, shade(pal.m, 0.6))
        return
    if iid == "onion":
        pix = mask(lambda d: (d.ellipse((3, 5, 12, 14), fill=255), d.polygon([(7, 1), (8, 1), (10, 6), (5, 6)], fill=255)))
        bevel(img, pix, pal)
        return
    box = (3, 4, 12, 13) if iid != "potato" else (2, 5, 13, 12)
    pix = mask(lambda d: d.ellipse(box, fill=255))
    bevel(img, pix, pal, noise=rng if iid in ("potato", "cabbage") else None)
    if iid in ("tomato", "strawberry"):
        for (x, y) in ((7, 3), (8, 3), (6, 4), (9, 4), (7, 2)):
            put(img, x, y, leaf)
    if iid == "strawberry":
        for (x, y) in ((6, 7), (9, 8), (7, 10), (10, 11), (5, 10)):
            put(img, x, y, (240, 220, 120))


def draw_creature(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((2, 5, 13, 14), fill=255), d.polygon([(3, 6), (4, 2), (6, 5)], fill=255),
                          d.polygon([(12, 6), (11, 2), (9, 5)], fill=255)))
    bevel(img, pix, pal, noise=rng)
    put(img, 5, 8, BLACK)
    put(img, 10, 8, BLACK)
    put(img, 7, 10, (220, 120, 120))
    put(img, 8, 10, (220, 120, 120))


def draw_blob(img, pal, item, rng):
    pix = mask(lambda d: d.ellipse((3, 3, 12, 12), fill=255))
    bevel(img, pix, pal, noise=rng)


def draw_key(img, pal, item, rng):
    def fn(u, v):
        d = math.hypot(u - 2, v)
        if 0.9 <= d <= 2.0:
            return pal.l if u + v < 2 else pal.m
        if 3.5 <= u <= 12 and v == 0:
            return pal.l
        if u in (9.5, 11.5) and 0 < v <= 1.5:
            return pal.m
        if u in (9, 10, 11, 12) and v == 1:
            return pal.d
        return None
    rdiag(img, fn)


def draw_club(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 4.5 and v == 0:
            return pal.d
        if 4.5 < u <= 13:
            w = 0.5 + (u - 4.5) * 0.22
            if u > 12:
                w -= (u - 12) * 1.2
            if abs(v) <= w:
                if (u * 2 + v * 2) % 7 == 0:
                    return pal.dd  # knots
                return pal.l if v < -w / 3 else (pal.m if v <= w / 3 else pal.d)
        return None
    rdiag(img, fn)


def draw_spear(img, pal, item, rng):
    shaft = shade(pal.m, 0.55) if item.get("tier") == "dragon" else WOOD
    head = Pal(STEEL) if item.get("tier") != "dragon" else pal

    def fn(u, v):
        if 10 <= u <= 13.5 and abs(v) <= min(1.5, (13.5 - u) * 0.6, (u - 9.5) * 1.2):
            return head.h if v < 0 else head.l if v == 0 else head.m
        if 9.5 <= u <= 10 and abs(v) <= 1.5:
            return pal.d if item.get("tier") != "dragon" else GOLD
        return _handle(u, v, 0, 9.5, shaft)
    rdiag(img, fn)


def draw_crossbow(img, pal, item, rng):
    limb = Pal(STEEL if item.get("tier") != "barrows" else (110, 110, 100))

    def fn(u, v):
        if abs(v) <= 4.5:
            uc = 10.5 - 0.12 * v * v
            if abs(u - uc) <= 0.25 or (abs(u - uc) <= 0.5 and abs(v) < 3.5):
                return limb.l if v < 0 else limb.m
        if 1 <= u <= 11.5 and abs(v) <= 0.5:
            return pal.l if v < 0 else (pal.m if v == 0 else pal.d)
        if 4 <= u <= 5 and 0.5 < v <= 2 and u - 4 <= v - 0.5:
            return pal.d  # trigger guard
        return None
    rdiag(img, fn)
    pts = []
    for y in range(16):
        for x in range(16):
            u = ((x - 1) - (y - 14)) / 2.0
            v = ((x - 1) + (y - 14)) / 2.0
            if abs(v) <= 3.5 and abs(u - (8.6 - 0.0 * v)) <= 0.25 and not opaque(img, x, y):
                pts.append((x, y, (220, 220, 200)))
    return pts


def draw_flail(img, pal, item, rng):
    metal = Pal(mix(pal.m, STEEL, 0.4))

    def fn(u, v):
        d = math.hypot(u - 11, v)
        if d <= 1.9:
            return metal.h if u + v < 10.2 else (metal.l if d < 1 else metal.m)
        if d <= 2.6 and (u == 11 or v == 0):
            return metal.d  # spikes
        if 7.5 <= u <= 8.5 and v == 0:
            return (120, 120, 125) if u % 1 else (160, 160, 165)
        if 7 <= u <= 7.5 and abs(v) <= 0.5:
            return metal.d
        return _handle(u, v, 0, 6.5, pal.d)
    rdiag(img, fn)


def draw_cape(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(4, 1), (11, 1), (14, 14), (1, 14)], fill=255))
    if item["id"] == "fire_cape":
        lava = Pal((60, 15, 5))
        for (x, y) in pix:
            n = math.sin(x * 1.3 + y * 0.7) + math.sin(y * 1.9 - x * 0.5) + rng.random() * 0.8
            c = (255, 220, 70) if n > 1.4 else (240, 120, 20) if n > 0.6 else (190, 50, 10) if n > -0.4 else lava.m
            put(img, x, y, c)
    else:
        bevel(img, pix, pal)
    for x in range(4, 12):
        put(img, x, 1, shade(pal.m, 0.5))
    for y in range(3, 14, 3):
        put(img, 7, y, shade(pal.m, 0.6))


def draw_tokkul(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(4, 3), (11, 3), (13, 8), (11, 13), (4, 13), (2, 8)], fill=255))
    bevel(img, pix, Pal((55, 45, 45)))
    for (x, y) in ((6, 6), (7, 7), (8, 8), (9, 7), (7, 9), (6, 10), (9, 10)):
        put(img, x, y, (230, 100, 30))
    put(img, 5, 5, (255, 200, 90))


def draw_pet(img, pal, item, rng):
    body = mask(lambda d: (d.ellipse((4, 7, 11, 14), fill=255),
                           d.polygon([(4, 9), (5, 3), (7, 7), (8, 1), (9, 7), (11, 3), (11, 9)], fill=255)))
    for (x, y) in body:
        t = (y - 1) / 13
        put(img, x, y, mix((255, 230, 90), pal.m, min(1, t * 1.4)))
    for (x, y) in ((6, 10), (9, 10)):
        put(img, x, y, BLACK)
    for x in (6, 7, 8, 9):
        put(img, x, 13, shade(pal.m, 0.5))


def draw_pot_item(img, pal, item, rng):
    draw_pot(img, Pal((150, 100, 60)), item, rng)
    for (x, y) in mask(lambda d: d.ellipse((5, 1, 10, 5), fill=255)):
        put(img, x, y, pal.h if y < 3 else pal.m)


def draw_skull(img, pal, item, rng):
    rows = ["..######..", ".########.", "##########", "##..##..##", "##..##..##", "#####.####",
            ".########.", "..#.##.#..", "..######.."]
    pix = {(3 + i, 3 + j) for j, r in enumerate(rows) for i, ch in enumerate(r) if ch == "#"}
    bevel(img, pix, pal)


def draw_bead(img, pal, item, rng):
    pix = mask(lambda d: d.ellipse((4, 4, 11, 11), fill=255)) - {(7, 7), (8, 7), (7, 8), (8, 8)}
    bevel(img, pix, pal)


def draw_tail(img, pal, item, rng):
    pts = [(2, 12), (4, 12), (6, 11), (8, 9), (9, 7), (9, 5), (10, 3), (12, 2), (13, 3)]
    for i in range(len(pts) - 1):
        line(img, *pts[i], *pts[i + 1], pal.m if i < 5 else pal.l)
    for (x, y) in ((2, 13), (3, 13), (4, 13), (3, 11), (5, 12)):
        put(img, x, y, pal.d)


def draw_note(img, pal, item, rng):
    pix = {(x, y) for x in range(3, 13) for y in range(2, 15)}
    bevel(img, pix, pal)
    ink = (70, 60, 50)
    for y in range(4, 13, 2):
        for x in range(5, 11 if y < 12 else 8):
            if rng.random() < 0.85:
                put(img, x, y, ink)
    if item["id"].startswith("message"):
        for (x, y) in ((10, 12), (11, 12), (10, 13), (11, 13)):
            put(img, x, y, (190, 30, 30))


def draw_package(img, pal, item, rng):
    p = Pal(pal.m)
    front = {(x, y) for x in range(2, 12) for y in range(6, 14)}
    bevel(img, front, p)
    for y in range(3, 6):
        for x in range(2 + (6 - y), 12 + (6 - y)):
            put(img, x, y, p.h)
    for y in range(3, 12):
        for x in (12, 13):
            if y - 3 <= (x - 11) * 3 + 6 and (x, y) not in front:
                put(img, x, y, p.d)
    string = (230, 220, 180)
    for y in range(6, 14):
        put(img, 7, y, string)
    for x in range(2, 12):
        put(img, x, 10, string)
    line(img, 7, 6, 10, 3, string)


def draw_talisman(img, pal, item, rng):
    for (x, y) in mask(lambda d: d.arc((5, -3, 10, 6), 0, 180, fill=255)):
        put(img, x, y, (200, 190, 150))
    pix = mask(lambda d: d.ellipse((2, 4, 13, 15), fill=255))
    bevel(img, pix, Pal((140, 140, 145)), noise=rng)
    sym = rune_symbol(item["id"].replace("_talisman", ""))
    for j, row in enumerate(sym):
        for i, ch in enumerate(row):
            if ch == "#":
                put(img, 4 + i, 6 + j, pal.l if (i + j) < 5 else pal.m)


def draw_stake(img, pal, item, rng):
    def fn(u, v):
        if 1 <= u <= 13 and abs(v) <= min(1.0, (13 - u) * 0.35):
            if u < 2:
                return pal.d
            return pal.l if v < 0 else (pal.m if v == 0 else pal.d)
        return None
    rdiag(img, fn)


def draw_beer(img, pal, item, rng):
    mug = Pal((150, 100, 55))
    body = {(x, y) for x in range(3, 11) for y in range(4, 15)}
    bevel(img, body, mug)
    for y in range(5, 14):
        for x in range(4, 10):
            put(img, x, y, pal.l if x < 6 else pal.m)
    for y in (6, 10):
        for x in range(3, 11):
            put(img, x, y, (110, 110, 115))
    for (x, y) in mask(lambda d: d.arc((8, 6, 14, 12), 270, 90, fill=255, width=2)):
        if x >= 11:
            put(img, x, y, mug.d)
    for (x, y) in mask(lambda d: d.ellipse((2, 1, 11, 6), fill=255)):
        put(img, x, y, (250, 248, 235) if y < 4 else (230, 225, 205))


SHAPES = {
    "key": draw_key, "club": draw_club, "spear": draw_spear, "crossbow": draw_crossbow, "flail": draw_flail,
    "cape": draw_cape, "tokkul": draw_tokkul, "pet": draw_pet, "pot_item": draw_pot_item, "skull": draw_skull,
    "bead": draw_bead, "tail": draw_tail, "note": draw_note, "package": draw_package, "talisman": draw_talisman,
    "stake": draw_stake, "beer": draw_beer,
    "coins": draw_coins, "log": draw_log, "plank": draw_plank, "ore": draw_ore, "essence": draw_essence,
    "clay": draw_clay, "bar": draw_bar, "arrowtips": draw_arrowtips, "axe": draw_axe, "pickaxe": draw_pickaxe,
    "tinderbox": draw_tinderbox, "knife": draw_knife, "chisel": draw_chisel, "hammer": draw_hammer,
    "needle": draw_needle, "net": draw_net, "rod": draw_rod, "pot": draw_pot, "harpoon": draw_harpoon,
    "dibber": draw_dibber, "mould": draw_mould, "snare": draw_snare, "box_trap": draw_box_trap,
    "bait": draw_bait, "gem": draw_gem, "mark": draw_mark, "fish_raw": draw_fish_raw,
    "fish_cooked": draw_fish_cooked, "fish_burnt": draw_fish_burnt, "meat_burnt": draw_meat_burnt,
    "meat_raw": draw_meat_raw, "meat_cooked": draw_meat_cooked, "cake": draw_cake, "bones": draw_bones,
    "big_bones": draw_big_bones, "rune": draw_rune, "staff": draw_staff, "staff_orb": draw_staff_orb,
    "dagger": sword_like(8, 0.5, 2.5, 1.5, 2),
    "scimitar": sword_like(12.5, 0.5, 3, 1.5, 2.5, curve=-1.6),
    "longsword": sword_like(13, 0.5, 3, 2, 2.5),
    "warhammer": draw_warhammer, "battleaxe": draw_battleaxe,
    "2h_sword": sword_like(13.5, 1.0, 4, 2.5, 3.5),
    "full_helm": draw_full_helm, "platebody": draw_platebody, "platelegs": draw_platelegs,
    "kiteshield": draw_kiteshield, "shaft": draw_shaft, "headless": draw_headless, "string": draw_string,
    "flax": draw_flax, "shortbow_u": bow(False, False), "longbow_u": bow(True, False),
    "shortbow": bow(False, True), "longbow": bow(True, True), "arrow": draw_arrow, "hide": draw_hide,
    "leather": draw_leather, "wool": draw_wool, "cloth": draw_cloth, "fur": draw_fur, "spice": draw_spice,
    "cowl": draw_cowl, "leather_body": draw_leather_body, "chaps": draw_chaps, "boots": draw_boots,
    "gem_uncut": draw_gem_uncut, "ring": draw_ring, "necklace": draw_necklace, "amulet": draw_amulet,
    "vial": draw_vial, "vial_water": draw_vial_water, "herb_grimy": draw_herb_grimy, "herb": draw_herb,
    "potion_unf": draw_potion_unf, "secondary": draw_secondary, "potion": draw_potion, "seed": draw_seed,
    "produce": draw_produce, "creature": draw_creature,
}


def render_item(item):
    shape = item.get("shape")
    drawer = SHAPES.get(shape)
    if drawer is None:
        FALLBACK_SHAPES.add(shape)
        drawer = draw_blob
    img = new_img()
    pal = Pal(item.get("color") or 0x808080)
    res = drawer(img, pal, item, rng_for("item", item["id"]))
    if res != "no_outline":
        outline(img)
    if isinstance(res, list):
        for (x, y, c) in res:
            put(img, x, y, c)
    return img


HANDHELD_KINDS = {"AXE", "PICKAXE", "MELEE_WEAPON", "STAFF", "BOW", "TOOL"}


def gen_items(cat):
    for item in cat["items"]:
        iid = item["id"]
        save_png(render_item(item), os.path.join(ASSETS, "textures", "item", iid + ".png"), "item textures")
        parent = "minecraft:item/handheld" if item["kind"] in HANDHELD_KINDS else "minecraft:item/generated"
        write_json(os.path.join(ASSETS, "models", "item", iid + ".json"),
                   {"parent": parent, "textures": {"layer0": f"{NS}:item/{iid}"}}, "item models")


# ----------------------------------------------------------------------------------------------
# Armour layers (64x32, vanilla biped UV layout)
# ----------------------------------------------------------------------------------------------

ARMOUR_COLOURS = {
    "bronze": 0xB08D57, "iron": 0x8A8A8E, "steel": 0xB8B8C0, "black": 0x2E2E34, "mithril": 0x4A5A9C,
    "adamant": 0x4C7A4C, "rune": 0x4FB8C8, "dragon": 0xB02020, "leather": 0x9A6A3A,
    "barrows": 0x6A6A60, "bandos": 0x6A7A3A,
}


def box_faces(u, v, w, h, d):
    """Return {face: (x, y, width, height)} for a vanilla model box net."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def paint_plate(img, rect, pal, rng, leather=False, face="", style=None):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            lx, ly = x - x0, y - y0
            if leather:
                c = pal.m if rng.random() > 0.2 else (pal.l if rng.random() < 0.5 else pal.d)
                if lx == 0 or ly == 0 or lx == w - 1 or ly == h - 1:
                    c = pal.d
                elif (lx == 1 or lx == w - 2 or ly == 1 or ly == h - 2) and (lx + ly) % 2 == 0:
                    c = mix(pal.m, (230, 210, 170), 0.5)  # stitching
            else:
                c = pal.m
                if lx == 0 or ly == 0:
                    c = pal.l
                if lx == w - 1 or ly == h - 1:
                    c = pal.dd
                elif ly > 0 and ly % 4 == 0 and h > 4:
                    c = pal.d  # plate seams
                elif ly % 4 == 1 and 0 < lx < w - 1 and h > 4:
                    c = pal.l
                if rng.random() < 0.08 and 0 < lx < w - 1 and 0 < ly < h - 1:
                    c = pal.h
                if style == "barrows" and rng.random() < 0.12:
                    c = mix(c, (90, 70, 50), 0.5)  # weathering / rust
                if style == "bandos" and h > 4 and (ly == 0 or ly == h - 1):
                    c = GOLD if (lx + ly) % 3 else shade(GOLD, 0.75)
            put(img, x, y, c)


def armour_layer(name, colour, layer):
    img = new_img(64, 32)
    pal = Pal(colour)
    rng = rng_for("armour", name, layer)
    leather = name == "leather"
    if layer == 1:
        for f, r in box_faces(0, 0, 8, 8, 8).items():
            paint_plate(img, r, pal, rng, leather, f, name)
        # visor on the helmet front (metal only), leave face open for leather (cowl)
        fx, fy = 8, 8
        if leather:
            for y in range(fy + 3, fy + 8):
                for x in range(fx + 2, fx + 6):
                    img.putpixel((x, y), (0, 0, 0, 0))
        else:
            for x in range(fx + 1, fx + 7):
                put(img, x, fy + 3, BLACK)
            for y in range(fy + 4, fy + 7):
                put(img, fx + 3, y, BLACK)
                put(img, fx + 4, y, BLACK)
        for f, r in box_faces(16, 16, 8, 12, 4).items():
            paint_plate(img, r, pal, rng, leather, f, name)
        for f, r in box_faces(40, 16, 4, 12, 4).items():
            paint_plate(img, r, pal, rng, leather, f, name)
        for f, r in box_faces(0, 16, 4, 12, 4).items():  # boots
            paint_plate(img, r, Pal(shade(colour, 0.85)), rng, leather, f, name)
    else:
        for f, r in box_faces(16, 16, 8, 12, 4).items():
            paint_plate(img, r, pal, rng, leather, f, name)
        for f, r in box_faces(0, 16, 4, 12, 4).items():
            paint_plate(img, r, pal, rng, leather, f, name)
        # belt line on waist
        for x in range(16, 40):
            if opaque(img, x, 20):
                put(img, x, 20, pal.dd if not leather else DARK_WOOD)
    return img


def gen_armour():
    base = os.path.join(RES, "assets", "minecraft", "textures", "models", "armor")
    for name, col in ARMOUR_COLOURS.items():
        for layer in (1, 2):
            save_png(armour_layer(name, col, layer),
                     os.path.join(base, f"runecraft_{name}_layer_{layer}.png"), "armour layers")


# ----------------------------------------------------------------------------------------------
# Block textures
# ----------------------------------------------------------------------------------------------

def noise_fill(img, base, rng, amount=0.12, rect=None):
    x0, y0, w, h = rect or (0, 0, img.width, img.height)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            f = 1 + (rng.random() - 0.5) * 2 * amount
            put(img, x, y, shade(base, f))


def stone_tex(rng, base=(125, 125, 128), amount=0.10):
    img = new_img()
    noise_fill(img, base, rng, amount)
    for _ in range(10):  # cracks / darker speckle clusters
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, shade(base, 0.72))
        put(img, x + 1, y, shade(base, 0.82))
    for _ in range(8):
        put(img, rng.randrange(16), rng.randrange(16), shade(base, 1.18))
    return img


def bark_tex(col, rng):
    img = new_img()
    p = Pal(col)
    for x in range(16):
        stripe = rng.random()
        for y in range(16):
            r = rng.random()
            c = p.m
            if stripe < 0.25 or (x % 4 == 0):
                c = p.d
            if r < 0.08:
                c = p.dd
            elif r > 0.93:
                c = p.l
            put(img, x, y, c)
    return img


def rings_tex(col, rng, pale=False):
    img = new_img()
    bark = Pal(col)
    wood = Pal(mix(col, (225, 190, 130), 0.55 if not pale else 0.75))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                put(img, x, y, bark.d if rng.random() < 0.6 else bark.m)
                continue
            d = max(abs(x - 7.5), abs(y - 7.5)) + 0.4 * math.hypot(x - 7.5, y - 7.5) * 0.2
            ring = int(d) % 2 == 0
            put(img, x, y, wood.m if ring else wood.d)
    return img


def stump_side_tex(col, rng):
    img = bark_tex(mix(col, (200, 180, 150), 0.35), rng)
    p = Pal(mix(col, (230, 200, 150), 0.6))
    for y in range(0, 4):  # pale freshly-cut band at the top
        for x in range(16):
            put(img, x, y, p.l if y < 3 else p.d)
    for x in range(0, 16, 3):
        put(img, x, 4, p.m)
    return img


def planks_tex(col, rng):
    img = new_img()
    p = Pal(col)
    for y in range(16):
        for x in range(16):
            c = p.m if rng.random() > 0.15 else p.d
            if y % 4 == 3:
                c = p.dd
            if y % 4 == 0:
                c = p.l
            off = 0 if (y // 4) % 2 == 0 else 8
            if (x + off) % 16 == 0:
                c = p.dd
            put(img, x, y, c)
    return img


def ore_tex(col, rng, essence=False):
    if essence:
        img = stone_tex(rng, mix(col, (200, 205, 215), 0.4), 0.06)
        p = Pal(col)
        for _ in range(6):
            x, y = rng.randint(1, 13), rng.randint(1, 13)
            for (dx, dy, c) in ((0, 0, p.h), (1, 0, p.l), (0, 1, p.l), (1, 1, p.m), (1, -1, WHITE)):
                put(img, x + dx, y + dy, c)
        return img
    img = stone_tex(rng)
    p = Pal(col)
    for _ in range(7):
        x, y = rng.randint(1, 13), rng.randint(1, 13)
        put(img, x, y, p.l)
        put(img, x + 1, y, p.m)
        put(img, x, y + 1, p.m)
        put(img, x + 1, y + 1, p.d)
        if rng.random() < 0.5:
            put(img, x + 2, y + 1, p.d)
    return img


def ripple_tex(rng):
    img = new_img()
    for (cx, cy, r) in ((5, 5, 3), (11, 10, 4), (4, 12, 2), (12, 3, 2)):
        for (x, y) in mask(lambda d, b=(cx - r, cy - r, cx + r, cy + r): d.ellipse(b, outline=255)):
            put(img, x, y, (230, 245, 255) if (x + y) % 3 else (160, 210, 245), 200)
    for _ in range(9):
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, WHITE, 230)
    return img


def symbol_big(img, sym, col, ox, oy, scale=2):
    p = Pal(col)
    for j, row in enumerate(sym):
        for i, ch in enumerate(row):
            if ch == "#":
                for sy in range(scale):
                    for sx in range(scale):
                        put(img, ox + i * scale + sx, oy + j * scale + sy, p.l if sx + sy == 0 else p.m)


ALTAR_COLOURS = {
    "air": (225, 225, 230), "water": (50, 100, 230), "earth": (140, 90, 40), "fire": (240, 80, 20),
    "mind": (240, 140, 30), "body": (40, 50, 160), "cosmic": (240, 230, 60), "chaos": (240, 120, 20),
    "astral": (200, 160, 230), "nature": (50, 170, 50), "law": (60, 100, 230), "death": (245, 245, 245),
    "blood": (200, 20, 20), "soul": (140, 60, 190), "wrath": (130, 15, 15),
}


def altar_top(name, rng):
    img = stone_tex(rng, (70, 68, 72), 0.08)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(img, x, y, (50, 48, 52))
    col = ALTAR_COLOURS.get(name, (200, 200, 200))
    # glow halo
    sym = rune_symbol(name)
    for j, row in enumerate(sym):
        for i, ch in enumerate(row):
            if ch == "#":
                for dx in (-1, 0, 1, 2):
                    for dy in (-1, 0, 1, 2):
                        x, y = 1 + i * 2 + dx, 1 + j * 2 + dy
                        if img.getpixel((x, y))[:3] != rgb(col):
                            put(img, x, y, mix((70, 68, 72), col, 0.25))
    symbol_big(img, sym, col, 1, 1)
    return img


def counter_side(rng, wood=WOOD, awning=None):
    img = planks_tex(wood, rng)
    if awning:
        for y in range(0, 4):
            for x in range(16):
                put(img, x, y, awning[(x // 2) % 2] if y < 3 else shade(awning[(x // 2) % 2], 0.7))
    for x in range(16):
        put(img, x, 15, shade(wood, 0.5))
    return img


def stall_goods(kind, rng):
    img = planks_tex(WOOD, rng)
    if kind == "cake":
        for (cx, cy) in ((4, 4), (11, 4), (4, 11), (11, 11)):
            for (x, y) in mask(lambda d, b=(cx - 3, cy - 3, cx + 2, cy + 2): d.ellipse(b, fill=255)):
                put(img, x, y, (230, 210, 150) if (x + y) % 5 else (240, 240, 240))
            put(img, cx, cy - 1, (220, 40, 40))
    elif kind == "silk":
        cols = [(220, 60, 60), (240, 230, 220), (80, 120, 220), (230, 200, 60)]
        for y in range(2, 14):
            for x in range(1, 15):
                put(img, x, y, shade(cols[(y - 2) // 3], 1.0 if x % 4 else 0.85))
    elif kind == "fur":
        noise_fill(img, (130, 95, 60), rng, 0.25, (1, 1, 14, 14))
        noise_fill(img, (200, 200, 195), rng, 0.15, (8, 3, 6, 6))
    elif kind == "silver":
        for (cx, cy) in ((4, 4), (11, 5), (6, 11), (12, 12)):
            for (x, y) in mask(lambda d, b=(cx - 2, cy - 2, cx + 2, cy + 2): d.ellipse(b, fill=255)):
                put(img, x, y, (215, 215, 225) if x + y < cx + cy else (160, 160, 170))
    elif kind == "spice":
        for (cx, cy, c) in ((4, 4, (200, 50, 30)), (11, 4, (230, 150, 30)), (4, 11, (140, 90, 40)),
                            (11, 11, (220, 200, 60))):
            for (x, y) in mask(lambda d, b=(cx - 3, cy - 3, cx + 3, cy + 3): d.ellipse(b, fill=255)):
                put(img, x, y, shade(c, 0.85 + rng.random() * 0.3))
    elif kind == "gem":
        noise_fill(img, (40, 30, 80), rng, 0.08, (1, 1, 14, 14))
        for (x, y, c) in ((3, 3, (220, 30, 40)), (8, 4, (40, 80, 230)), (12, 3, (40, 200, 80)),
                          (4, 9, (240, 240, 255)), (10, 10, (40, 200, 80)), (6, 12, (220, 30, 40)),
                          (12, 13, (40, 80, 230))):
            put(img, x, y, c)
            put(img, x + 1, y, shade(c, 0.7))
            put(img, x, y + 1, shade(c, 0.7))
            put(img, x + 1, y + 1, shade(c, 0.5))
    return img


def overlay(base, top):
    out = base.copy()
    out.alpha_composite(top)
    return out


def item_icon(cat_by_id, iid):
    it = cat_by_id.get(iid)
    if it is None:
        return new_img()
    return render_item(it)


def portal_tex(rng, side=False):
    img = new_img()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            v = math.sin(3 * a + r * 1.1) * 0.5 + 0.5
            v = v * 0.8 + rng.random() * 0.2
            c = mix((40, 10, 70), (190, 90, 255), v)
            if r < 2.2:
                c = mix(c, (240, 210, 255), 0.6)
            put(img, x, y, c)
    if side:
        for i in range(16):
            for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
                put(img, x, y, (60, 55, 65) if (i % 3) else (90, 85, 95))
    for _ in range(6):
        put(img, rng.randrange(1, 15), rng.randrange(1, 15), (255, 240, 255))
    return img


def station_textures(sid, cat_by_id):
    """Return (side, top, bottom) images for a station block."""
    rng = rng_for("station", sid)
    stone = stone_tex(rng)
    wood_top = planks_tex(WOOD, rng)
    if sid == "bank_booth":
        side = counter_side(rng, DARK_WOOD)
        for y in range(2, 10):
            for x in range(2, 14):
                put(img := side, x, y, (30, 25, 20) if (x % 3 == 0 or y % 3 == 0) else (60, 50, 40))
        for x in range(2, 14):
            put(side, x, 2, GOLD)
            put(side, x, 10, GOLD)
        top = overlay(planks_tex(DARK_WOOD, rng), item_icon(cat_by_id, "coins"))
        return side, top, planks_tex(DARK_WOOD, rng)
    if sid == "furnace":
        side = stone_tex(rng, (110, 100, 95))
        for y in range(16):
            for x in range(16):
                if y % 4 == 3 or (x + (4 if (y // 4) % 2 else 0)) % 8 == 7:
                    put(side, x, y, (70, 65, 60))
        for y in range(8, 14):
            for x in range(4, 12):
                t = (y - 8) / 6
                put(side, x, y, mix((255, 220, 80), (220, 70, 20), t) if (x + y) % 3 else (255, 160, 40))
        top = stone_tex(rng, (90, 85, 80))
        for (x, y) in mask(lambda d: d.ellipse((4, 4, 11, 11), fill=255)):
            put(top, x, y, (40, 30, 25))
        return side, top, stone
    if sid == "anvil":
        dark = (60, 60, 66)
        side = stone_tex(rng, dark, 0.06)
        pix = mask(lambda d: d.polygon([(1, 2), (15, 2), (15, 5), (11, 7), (11, 11), (14, 14), (2, 14), (5, 11), (5, 7), (1, 5)], fill=255))
        base = stone_tex(rng, (35, 35, 38), 0.05)
        for (x, y) in pix:
            put(base, x, y, (100, 100, 108) if y < 4 else (78, 78, 84))
        top = stone_tex(rng, (95, 95, 102), 0.05)
        for x in range(16):
            put(top, x, 0, (130, 130, 138))
        return base, top, stone_tex(rng, (35, 35, 38), 0.05)
    if sid == "cooking_range":
        side = stone_tex(rng, (100, 95, 92))
        for y in range(7, 13):
            for x in range(3, 13):
                put(side, x, y, (30, 30, 30) if x in (3, 12) or y in (7, 12) else mix((255, 140, 30), (200, 50, 10), rng.random()))
        top = stone_tex(rng, (60, 60, 64), 0.05)
        for (cx, cy) in ((4, 4), (11, 4), (4, 11), (11, 11)):
            for (x, y) in mask(lambda d, b=(cx - 3, cy - 3, cx + 2, cy + 2): d.ellipse(b, outline=255)):
                put(top, x, y, (25, 25, 28))
        return side, top, stone
    if sid == "prayer_altar":
        side = stone_tex(rng, (200, 198, 190), 0.05)
        for y in range(16):
            for x in range(3, 13):
                if y < 10:
                    put(side, x, y, (130, 30, 160) if x not in (3, 12) else GOLD)
        for x in range(3, 13):
            put(side, x, 10, GOLD)
        top = stone_tex(rng, (200, 198, 190), 0.05)
        for y in range(16):
            for x in range(4, 12):
                put(top, x, y, (130, 30, 160))
        for (x, y) in ((7, 4), (8, 4), (7, 5), (8, 5), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (7, 7),
                       (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10)):
            put(top, x, y, GOLD)
        return side, top, stone
    shops = {
        "general_store": ("coins", ((40, 120, 40), (230, 230, 220))),
        "rune_shop": ("air_rune", ((60, 60, 180), (230, 230, 220))),
        "weapon_shop": ("bronze_longsword", ((170, 30, 30), (230, 230, 220))),
        "tool_shop": ("bronze_axe", ((170, 110, 30), (230, 230, 220))),
        "archery_shop": ("shortbow", ((40, 110, 50), (220, 200, 140))),
    }
    if sid in shops:
        icon_id, awning = shops[sid]
        side = counter_side(rng, WOOD, awning)
        top = overlay(wood_top, item_icon(cat_by_id, icon_id))
        return side, top, planks_tex(DARK_WOOD, rng)
    if sid == "spinning_wheel":
        side = planks_tex(mix(WOOD, (200, 170, 120), 0.3), rng)
        for (x, y) in mask(lambda d: d.ellipse((2, 2, 13, 13), outline=255)):
            put(side, x, y, DARK_WOOD)
        for a in range(0, 360, 45):
            line(side, 8, 8, 8 + int(round(5 * math.cos(math.radians(a)))), 8 + int(round(5 * math.sin(math.radians(a)))), DARK_WOOD)
        put(side, 8, 8, GOLD)
        top = planks_tex(mix(WOOD, (200, 170, 120), 0.3), rng)
        for (x, y) in mask(lambda d: d.ellipse((5, 5, 10, 10), fill=255)):
            put(top, x, y, (235, 230, 215))
        return side, top, planks_tex(DARK_WOOD, rng)
    if sid == "tanning_rack":
        side = new_img()
        noise_fill(side, (140, 110, 80), rng, 0.15)
        for i in range(16):
            for (x, y) in ((i, 0), (i, 15), (0, i), (15, i), (1, i), (14, i)):
                put(side, x, y, DARK_WOOD)
        hide = mask(lambda d: d.polygon([(3, 2), (12, 2), (13, 7), (11, 13), (4, 13), (2, 7)], fill=255))
        for (x, y) in hide:
            put(side, x, y, shade((180, 130, 80), 0.85 + rng.random() * 0.3))
        top = planks_tex(DARK_WOOD, rng)
        return side, top, top
    if sid == "construction_workbench":
        side = planks_tex(WOOD, rng)
        for y in range(4, 16):
            for x in (1, 2, 13, 14):
                put(side, x, y, DARK_WOOD)
        for x in range(16):
            put(side, x, 3, DARK_WOOD)
        top = planks_tex(mix(WOOD, (210, 180, 120), 0.3), rng)
        for x in range(2, 13):  # saw
            put(top, x, 5, STEEL)
            put(top, x, 6, shade(STEEL, 0.75) if x % 2 else STEEL)
        for (x, y) in ((12, 4), (13, 4), (13, 5), (13, 6), (12, 7), (13, 7)):
            put(top, x, y, (150, 40, 30))
        line(top, 3, 11, 9, 11, WOOD)  # hammer
        for y in (10, 11, 12):
            put(top, 10, y, (90, 90, 95))
            put(top, 11, y, (90, 90, 95))
        return side, top, planks_tex(DARK_WOOD, rng)
    if sid == "slayer_master":
        side = stone_tex(rng, (55, 45, 45), 0.08)
        skull = ["..####..", ".######.", "##.##.##", "##.##.##", ".######.", "..#..#..", "..####.."]
        for j, row in enumerate(skull):
            for i, ch in enumerate(row):
                if ch == "#":
                    put(side, 4 + i, 4 + j, (225, 220, 205))
        top = stone_tex(rng, (70, 30, 30), 0.08)
        for (x, y) in mask(lambda d: d.ellipse((3, 3, 12, 12), outline=255)):
            put(top, x, y, (180, 30, 30))
        return side, top, stone_tex(rng, (40, 35, 35), 0.08)
    if sid == "apothecary_table":
        side = planks_tex(mix(WOOD, (90, 70, 50), 0.4), rng)
        for (x0, c) in ((2, (200, 40, 40)), (7, (50, 160, 60)), (12, (60, 90, 220))):
            for y in range(3, 9):
                put(side, x0, y, c if y > 4 else GLASS)
                put(side, x0 + 1, y, shade(c, 0.8) if y > 4 else GLASS)
            put(side, x0, 2, (150, 110, 70))
            put(side, x0 + 1, 2, (150, 110, 70))
        top = overlay(planks_tex(WOOD, rng), item_icon(cat_by_id, "guam_leaf"))
        return side, top, planks_tex(DARK_WOOD, rng)
    if sid == "gielinor_portal":
        return portal_tex(rng, side=True), portal_tex(rng_for("portal_top")), stone_tex(rng, (60, 55, 65))
    raise KeyError(sid)


# ----------------------------------------------------------------------------------------------
# Block generation
# ----------------------------------------------------------------------------------------------

def tex_path(name):
    return os.path.join(ASSETS, "textures", "block", name + ".png")


def block_tex(img, name):
    save_png(img, tex_path(name), "block textures")
    return f"{NS}:block/{name}"


def block_model(name, obj):
    write_json(os.path.join(ASSETS, "models", "block", name + ".json"), obj, "block models")
    return f"{NS}:block/{name}"


def blockstate(bid, obj):
    write_json(os.path.join(ASSETS, "blockstates", bid + ".json"), obj, "blockstates")


def block_item_model(bid, obj):
    write_json(os.path.join(ASSETS, "models", "item", bid + ".json"), obj, "block item models")


def cube_bottom_top(side, top, bottom):
    return {"parent": "minecraft:block/cube_bottom_top", "textures": {"side": side, "top": top, "bottom": bottom}}


def plant_model(tex, y0, y1):
    faces = {
        "north": {"uv": [0, 0, 16, 16], "texture": "#plant"},
        "south": {"uv": [0, 0, 16, 16], "texture": "#plant"},
    }
    elems = []
    for angle, (fz, tz) in ((45, (8, 8)), (-45, (8, 8))):
        elems.append({
            "from": [0.8, y0, fz], "to": [15.2, y1, tz], "shade": False,
            "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": angle, "rescale": True},
            "faces": faces,
        })
    return {"ambientocclusion": False, "textures": {"particle": tex, "plant": tex}, "elements": elems}


def plant_tex(rng, leaf, produce=None, tall=False):
    img = new_img()
    p = Pal(leaf)
    top = 1 if tall else 6
    for k, x0 in enumerate((3, 6, 9, 12)):
        h = rng.randint(top, top + 4)
        for y in range(h, 16):
            sway = int(round(math.sin(y * 0.6 + k) * 0.8))
            put(img, x0 + sway, y, p.m if k % 2 else p.d)
            if y % 3 == k % 3 and y < 14:
                put(img, x0 + sway + 1, y, p.l)
                put(img, x0 + sway - 1, y - 1, p.l)
    if produce:
        pp = Pal(produce)
        for (x, y) in ((4, top + 3), (10, top + 2), (7, top + 6), (12, top + 5)):
            put(img, x, y, pp.l)
            put(img, x + 1, y, pp.m)
            put(img, x, y + 1, pp.m)
            put(img, x + 1, y + 1, pp.d)
    return img


def gen_blocks(cat):
    names = {}
    cat_by_id = {i["id"]: i for i in cat["items"]}

    # a) trees
    for t in cat["trees"]:
        tid, col = t["id"], t["color"]
        rng = rng_for("tree", tid)
        side = block_tex(bark_tex(col, rng), f"{tid}_log_side")
        end = block_tex(rings_tex(col, rng), f"{tid}_log_top")
        sside = block_tex(stump_side_tex(col, rng), f"{tid}_stump_side")
        send = block_tex(rings_tex(col, rng, pale=True), f"{tid}_stump_top")
        bid = f"{tid}_tree_log"
        m1 = block_model(bid, {"parent": "minecraft:block/cube_column", "textures": {"side": side, "end": end}})
        m2 = block_model(bid + "_depleted", {"parent": "minecraft:block/cube_column", "textures": {"side": sside, "end": send}})
        blockstate(bid, {"variants": {"depleted=false": {"model": m1}, "depleted=true": {"model": m2}}})
        block_item_model(bid, {"parent": m1})
        names[bid] = t["name"]

    # b) rocks
    dep = block_tex(stone_tex(rng_for("depleted_rock"), (88, 86, 88), 0.08), "depleted_rock")
    dep_model = block_model("depleted_rock", {"parent": "minecraft:block/cube_all", "textures": {"all": dep}})
    for r in cat["rocks"]:
        rid = r["id"]
        essence = "essence" in rid
        tex = block_tex(ore_tex(r["color"], rng_for("rock", rid), essence), f"{rid}_rock")
        bid = f"{rid}_rock"
        m1 = block_model(bid, {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
        blockstate(bid, {"variants": {"depleted=false": {"model": m1}, "depleted=true": {"model": dep_model}}})
        block_item_model(bid, {"parent": m1})
        names[bid] = r["name"]

    # c) fishing spots
    for s in cat["fishing_spots"]:
        sid = s["id"]
        tex = block_tex(ripple_tex(rng_for("spot", sid)), sid)
        m = block_model(sid, {
            "ambientocclusion": False,
            "textures": {"particle": tex, "ripple": tex},
            "elements": [{
                "from": [0, 13.5, 0], "to": [16, 14.5, 16], "shade": False,
                "faces": {
                    "up": {"uv": [0, 0, 16, 16], "texture": "#ripple"},
                    "down": {"uv": [0, 0, 16, 16], "texture": "#ripple"},
                },
            }],
        })
        blockstate(sid, {"variants": {"waterlogged=false": {"model": m}, "waterlogged=true": {"model": m}}})
        block_item_model(sid, {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
        names[sid] = s["name"]

    # d) rune altars
    a_side = block_tex(stone_tex(rng_for("altar_side"), (70, 68, 72), 0.09), "altar_side")
    a_bot = block_tex(stone_tex(rng_for("altar_bottom"), (55, 53, 57), 0.08), "altar_bottom")
    for a in cat["altars"]:
        aid = a["id"]
        top = block_tex(altar_top(aid.replace("_altar", ""), rng_for("altar", aid)), aid + "_top")
        m = block_model(aid, cube_bottom_top(a_side, top, a_bot))
        blockstate(aid, {"variants": {"": {"model": m}}})
        block_item_model(aid, {"parent": m})
        names[aid] = a["name"]

    # e) stalls
    st_side = block_tex(counter_side(rng_for("stall_side"), WOOD, ((170, 30, 30), (235, 230, 220))), "stall_side")
    st_bot = block_tex(planks_tex(DARK_WOOD, rng_for("stall_bottom")), "stall_bottom")
    st_empty = block_tex(planks_tex(WOOD, rng_for("stall_top_empty")), "stall_top_empty")
    for s in cat["stalls"]:
        sid = s["id"]
        top = block_tex(stall_goods(sid.replace("_stall", ""), rng_for("stall", sid)), sid + "_top")
        m1 = block_model(sid, cube_bottom_top(st_side, top, st_bot))
        m2 = block_model(sid + "_empty", cube_bottom_top(st_side, st_empty, st_bot))
        blockstate(sid, {"variants": {"empty=false": {"model": m1}, "empty=true": {"model": m2}}})
        block_item_model(sid, {"parent": m1})
        names[sid] = s["name"]

    # f) stations
    for sid, nm in STATIONS.items():
        side, top, bottom = station_textures(sid, cat_by_id)
        m = block_model(sid, cube_bottom_top(block_tex(side, sid + "_side"), block_tex(top, sid + "_top"),
                                             block_tex(bottom, sid + "_bottom")))
        blockstate(sid, {"variants": {"": {"model": m}}})
        block_item_model(sid, {"parent": m})
        names[sid] = nm

    # g) agility obstacle
    rng = rng_for("agility")
    img = planks_tex((150, 110, 60), rng)
    arrow = ["....#....", "...###...", "..#####..", ".#######.", "...###...", "...###...", "...###..."]
    for j, row in enumerate(arrow):
        for i, ch in enumerate(row):
            if ch == "#":
                put(img, 3 + i, 4 + j, (240, 220, 40) if j < 4 else (90, 190, 50))
    tex = block_tex(img, "agility_obstacle")
    m = block_model("agility_obstacle", {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
    blockstate("agility_obstacle", {"variants": {"": {"model": m}}})
    block_item_model("agility_obstacle", {"parent": m})
    names["agility_obstacle"] = "Agility obstacle"

    # h) farming patches
    rng = rng_for("patch")
    soil = new_img()
    for y in range(16):
        for x in range(16):
            c = (70, 45, 25) if y % 4 not in (0, 3) else (50, 32, 18)
            put(soil, x, y, shade(c, 0.85 + rng.random() * 0.3))
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(soil, x, y, (110, 80, 50))
    soil_t = block_tex(soil, "patch_soil")
    dirt = new_img()
    noise_fill(dirt, (120, 85, 55), rng, 0.15)
    for x in range(16):
        put(dirt, x, 0, (60, 110, 40))
        put(dirt, x, 1, (60, 110, 40) if rng.random() < 0.6 else (90, 70, 40))
    dirt_t = block_tex(dirt, "patch_dirt")
    for pid, nm, leaf, produce, prop_crop in (
            ("herb_patch", "Herb patch", (60, 150, 50), None, 14),
            ("allotment_patch", "Allotment patch", (70, 140, 50), (220, 60, 40), 8)):
        base = block_model(pid, cube_bottom_top(dirt_t, soil_t, dirt_t))
        g_tex = block_tex(plant_tex(rng_for(pid, "growing"), leaf), pid + "_growing")
        f_tex = block_tex(plant_tex(rng_for(pid, "grown"), shade(leaf, 0.9),
                                    produce if produce else (240, 240, 120), tall=True), pid + "_grown")
        gm = block_model(pid + "_growing", plant_model(g_tex, 16, 25))
        fm = block_model(pid + "_grown", plant_model(f_tex, 16, 30))
        blockstate(pid, {"multipart": [
            {"apply": {"model": base}},
            {"when": {"stage": "1|2|3"}, "apply": {"model": gm}},
            {"when": {"stage": "4"}, "apply": {"model": fm}},
        ]})
        block_item_model(pid, {"parent": base})
        names[pid] = nm

    # i) fire (vanilla models)
    blockstate("osrs_fire", {"variants": {"": [{"model": "minecraft:block/fire_floor0"},
                                               {"model": "minecraft:block/fire_floor1"}]}})
    block_item_model("osrs_fire", {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:block/fire_0"}})
    names["osrs_fire"] = "Fire"

    # j) traps
    rng = rng_for("traps")
    snare = new_img()
    for (x, y) in mask(lambda d: d.ellipse((1, 1, 14, 14), outline=255)):
        put(snare, x, y, (225, 215, 175))
    stick = new_img()
    noise_fill(stick, WOOD, rng, 0.12)
    feathers = new_img()
    for (x, y) in ((4, 8), (5, 7), (6, 6), (7, 5), (8, 6), (9, 7), (10, 8), (6, 9), (8, 10), (9, 9), (7, 8),
                   (5, 11), (10, 11)):
        put(feathers, x, y, (200, 120, 60) if (x + y) % 2 else (240, 230, 210))
    snare_t = block_tex(snare, "bird_snare_loop")
    stick_t = block_tex(stick, "trap_stick")
    feath_t = block_tex(feathers, "bird_snare_feathers")
    post = {"from": [7, 0, 7], "to": [9, 9, 9], "faces": {f: {"texture": "#stick", "uv": [7, 0, 9, 9]}
                                                          for f in ("north", "south", "east", "west")}}
    post["faces"]["up"] = {"texture": "#stick", "uv": [7, 7, 9, 9]}
    loop = {"from": [2, 0.25, 2], "to": [14, 0.25, 14],
            "faces": {"up": {"texture": "#loop", "uv": [0, 0, 16, 16]}, "down": {"texture": "#loop", "uv": [0, 0, 16, 16]}}}
    tex_map = {"particle": stick_t, "stick": stick_t, "loop": snare_t, "feathers": feath_t}
    m_open = block_model("bird_snare_trap", {"ambientocclusion": False, "textures": tex_map, "elements": [post, loop]})
    feather_elems = [{
        "from": [1, 0, 8], "to": [15, 10, 8], "shade": False,
        "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": ang, "rescale": False},
        "faces": {"north": {"texture": "#feathers", "uv": [0, 2, 16, 14]},
                  "south": {"texture": "#feathers", "uv": [0, 2, 16, 14]}}} for ang in (45, -45)]
    m_caught = block_model("bird_snare_trap_caught", {"ambientocclusion": False, "textures": tex_map,
                                                      "elements": [post, loop] + feather_elems})
    blockstate("bird_snare_trap", {"variants": {"caught=false": {"model": m_open}, "caught=true": {"model": m_caught}}})
    block_item_model("bird_snare_trap", {"parent": "minecraft:item/generated",
                                         "textures": {"layer0": f"{NS}:item/bird_snare"}})
    names["bird_snare_trap"] = "Bird snare (set)"

    box_side = planks_tex(WOOD, rng)
    for x in range(16):
        put(box_side, x, 0, DARK_WOOD)
    box_open = box_side.copy()
    for y in range(4, 12):
        for x in range(4, 12):
            put(box_open, x, y, (25, 18, 10))
    box_closed = box_side.copy()
    for (x, y) in ((6, 7), (7, 7), (8, 8), (9, 7), (7, 9), (8, 9)):
        put(box_closed, x, y, (190, 170, 140))  # fur poking through the slats
    bs_t = block_tex(box_side, "box_trap_side")
    bo_t = block_tex(box_open, "box_trap_front_open")
    bc_t = block_tex(box_closed, "box_trap_front_closed")

    def box(front):
        faces = {f: {"texture": "#side", "uv": [4, 10, 12, 16]} for f in ("east", "west", "south")}
        faces["north"] = {"texture": "#front", "uv": [4, 6, 12, 12] if front == "open" else [4, 6, 12, 12]}
        faces["up"] = {"texture": "#side", "uv": [4, 4, 12, 12]}
        faces["down"] = {"texture": "#side", "uv": [4, 4, 12, 12]}
        return {"from": [4, 0, 4], "to": [12, 6, 12], "faces": faces}

    prop = {"from": [7.5, 0, 2], "to": [8.5, 7, 3],
            "rotation": {"origin": [8, 0, 2.5], "axis": "x", "angle": 22.5},
            "faces": {f: {"texture": "#stick", "uv": [0, 0, 1, 7]} for f in ("north", "south", "east", "west", "up")}}
    m_bo = block_model("box_trap_trap", {"textures": {"particle": bs_t, "side": bs_t, "front": bo_t, "stick": stick_t},
                                         "elements": [box("open"), prop]})
    m_bc = block_model("box_trap_trap_caught", {"textures": {"particle": bs_t, "side": bs_t, "front": bc_t},
                                                "elements": [box("closed")]})
    blockstate("box_trap_trap", {"variants": {"caught=false": {"model": m_bo}, "caught=true": {"model": m_bc}}})
    block_item_model("box_trap_trap", {"parent": "minecraft:item/generated",
                                       "textures": {"layer0": f"{NS}:item/box_trap"}})
    names["box_trap_trap"] = "Box trap (set)"
    gen_boss_stones(names)
    return names


SIGILS = {
    "club": ["......##", ".....###", "....###.", "...###..", "..##....", ".##.....", "##......", "#......."],
    "leaf": ["....####", "..######", ".######.", ".#####..", ".####...", ".##.....", "#.......", "........"],
    "bandos": ["#..##..#", "#.####.#", "########", ".######.", "..####..", "..####..", ".##..##.", "##....##"],
    "zamorak": ["#..##..#", "##.##.##", ".######.", "...##...", "...##...", "..####..", ".##..##.", "#......#"],
    "cross": ["...##...", "...##...", "########", "########", "...##...", "...##...", "...##...", "...##..."],
    "fang": ["########", "#......#", ".#....#.", ".##..##.", "..#..#..", "..#..#..", "...##...", "...##..."],
}

BARROWS_BROTHERS = {
    "ahrim": ((150, 70, 200), "Ahrim's sarcophagus"), "dharok": ((200, 40, 30), "Dharok's sarcophagus"),
    "guthan": ((60, 170, 70), "Guthan's sarcophagus"), "karil": ((60, 170, 190), "Karil's sarcophagus"),
    "torag": ((220, 140, 40), "Torag's sarcophagus"), "verac": ((210, 200, 80), "Verac's sarcophagus"),
}


def glow_sigil(img, rows, col, scale=1, ox=None, oy=None):
    w, h = len(rows[0]) * scale, len(rows) * scale
    ox = (16 - w) // 2 if ox is None else ox
    oy = (16 - h) // 2 if oy is None else oy
    cells = {(ox + i * scale + sx, oy + j * scale + sy) for j, r in enumerate(rows) for i, ch in enumerate(r)
             if ch == "#" for sx in range(scale) for sy in range(scale)}
    for (x, y) in list(cells):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if (x + dx, y + dy) not in cells and 0 <= x + dx < 16 and 0 <= y + dy < 16:
                    put(img, x + dx, y + dy, mix(img.getpixel((x + dx, y + dy)), col, 0.35))
    p = Pal(col)
    for (x, y) in cells:
        put(img, x, y, p.h if (x + y) % 4 == 0 else p.l)


def coffin_outline(img, col):
    pts = [(5, 1), (10, 1), (13, 4), (11, 14), (4, 14), (2, 4)]
    for i in range(len(pts)):
        line(img, *pts[i], *pts[(i + 1) % len(pts)], col)


def pentagram(img, col):
    cx, cy, r = 7.5, 7.8, 6.3
    pts = [(int(round(cx + r * math.sin(math.radians(a)))), int(round(cy - r * math.cos(math.radians(a)))))
           for a in range(0, 360, 72)]
    for (x, y) in mask(lambda d: d.ellipse((0, 0, 15, 15), outline=255)):
        put(img, x, y, shade(col, 0.75))
    for i in range(5):
        line(img, *pts[i], *pts[(i + 2) % 5], col)


def gen_boss_stones(names):
    for bid, nm, side, top, bottom in boss_stone_specs():
        m = block_model(bid, cube_bottom_top(block_tex(side, bid + "_side"), block_tex(top, bid + "_top"),
                                             block_tex(bottom, bid + "_bottom")))
        blockstate(bid, {"variants": {"": {"model": m}}})
        block_item_model(bid, {"parent": m})
        names[bid] = nm


def boss_stone_specs():
    specs = []
    ominous = (45, 42, 48)

    def stone(key, base=ominous, amt=0.09):
        return stone_tex(rng_for("boss", key), base, amt)

    def frame(img, col):
        for i in range(16):
            for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
                put(img, x, y, col)
        return img

    # Obor / Bryophyta: gates
    for bid, nm, col, sig in (("obor_summoning_stone", "Obor's gate", (150, 100, 50), "club"),
                              ("bryophyta_summoning_stone", "Bryophyta's gate", (70, 170, 50), "leaf")):
        side = stone(bid + "s")
        for x in (3, 6, 9, 12):  # gate bars
            for y in range(1, 15):
                put(side, x, y, shade(col, 0.55) if y % 5 else shade(col, 0.8))
        for x in range(1, 15):
            put(side, x, 2, shade(col, 0.6))
            put(side, x, 13, shade(col, 0.6))
        top = frame(stone(bid + "t"), shade(col, 0.5))
        glow_sigil(top, SIGILS[sig], col)
        specs.append((bid, nm, side, top, stone(bid + "b", (30, 28, 32))))
    # Barrows brothers: grey stone sarcophagi
    for bro, (col, nm) in BARROWS_BROTHERS.items():
        bid = bro + "_summoning_stone"
        grey = (105, 102, 98)
        side = stone(bid + "s", grey, 0.07)
        for x in range(16):
            put(side, x, 3, shade(grey, 0.65))
            put(side, x, 12, shade(grey, 0.65))
        for x in range(2, 14, 4):
            for y in range(5, 11):
                put(side, x, y, shade(grey, 0.75))
        top = stone(bid + "t", (90, 88, 85), 0.07)
        coffin_outline(top, (60, 58, 56))
        glow_sigil(top, SIGILS["cross"], col)
        specs.append((bid, nm, side, top, stone(bid + "b", (60, 58, 56))))
    # General Graardor: Bandos altar
    gb = (106, 122, 58)
    side = stone("graardor_s", shade(gb, 0.55), 0.1)
    for x in range(16):
        put(side, x, 1, GOLD)
        put(side, x, 14, GOLD)
    glow_sigil(side, SIGILS["bandos"], (150, 170, 80), oy=4)
    top = frame(stone("graardor_t", shade(gb, 0.45)), GOLD)
    glow_sigil(top, SIGILS["bandos"], (190, 220, 90))
    specs.append(("general_graardor_summoning_stone", "Bandos altar", side, top, stone("graardor_b", (40, 38, 30))))
    # K'ril: Zamorak altar
    side = stone("kril_s", (35, 20, 22), 0.1)
    for y in range(16):
        put(side, 0, y, (150, 20, 20))
        put(side, 15, y, (150, 20, 20))
    glow_sigil(side, SIGILS["zamorak"], (180, 30, 30), oy=4)
    top = frame(stone("kril_t", (30, 15, 18)), (120, 15, 15))
    glow_sigil(top, SIGILS["zamorak"], (230, 40, 30))
    specs.append(("kril_tsutsaroth_summoning_stone", "Zamorak altar", side, top, stone("kril_b", (20, 12, 14))))
    # TzTok-Jad: obsidian with lava
    rng = rng_for("jad")

    def obsidian(key, lava_frac):
        img = stone_tex(rng_for("jad", key), (28, 22, 30), 0.12)
        for y in range(16):
            for x in range(16):
                n = math.sin(x * 0.9 + y * 0.4 + rng.random()) + math.sin(y * 1.3 - x * 0.7)
                if n > 2 - lava_frac * 3:
                    put(img, x, y, (255, 200, 60) if n > 2.2 - lava_frac * 3 else (230, 90, 20))
        return img
    top = obsidian("t", 0.55)
    for (x, y) in mask(lambda d: d.ellipse((3, 3, 12, 12), fill=255)):
        put(top, x, y, mix((255, 210, 80), (220, 70, 10), rng.random()))
    specs.append(("tztok_jad_summoning_stone", "TzHaar Fight Cave entrance", obsidian("s", 0.35), top,
                  stone("jad_b", (20, 16, 20))))
    # Count Draynor: dark wooden coffin
    dw = (60, 35, 25)
    side = planks_tex(dw, rng_for("draynor_s"))
    for x in range(16):
        put(side, x, 1, (140, 20, 20))
    top = planks_tex(dw, rng_for("draynor_t"))
    coffin_outline(top, (150, 20, 20))
    glow_sigil(top, SIGILS["fang"], (200, 30, 30), oy=5)
    specs.append(("count_draynor_summoning_stone", "Count Draynor's coffin", side, top,
                  planks_tex(shade(dw, 0.7), rng_for("draynor_b"))))
    # Delrith: summoning circle
    side = stone("delrith_s", (40, 30, 32))
    for x in range(1, 15, 2):
        put(side, x, 12, (170, 20, 20))
        put(side, x + 1, 3, (170, 20, 20))
    top = stone("delrith_t", (35, 28, 30))
    pentagram(top, (230, 30, 20))
    specs.append(("delrith_summoning_stone", "Delrith's summoning circle", side, top, stone("delrith_b", (25, 20, 22))))
    return specs


STATIONS = {
    "bank_booth": "Bank booth", "furnace": "Furnace", "anvil": "Anvil", "cooking_range": "Range",
    "prayer_altar": "Altar", "general_store": "General store", "rune_shop": "Rune shop",
    "weapon_shop": "Sword shop", "tool_shop": "Axe shop", "archery_shop": "Archery shop",
    "spinning_wheel": "Spinning wheel", "tanning_rack": "Tanner", "construction_workbench": "Workbench",
    "slayer_master": "Slayer master", "apothecary_table": "Apothecary table", "gielinor_portal": "Gielinor portal",
}


def gen_loot_tables():
    for sid in STATIONS:
        write_json(os.path.join(RES, "data", NS, "loot_tables", "blocks", sid + ".json"), {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"{NS}:{sid}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        }, "loot tables")


# ----------------------------------------------------------------------------------------------
# Entity textures (64x64 player/zombie layout)
# ----------------------------------------------------------------------------------------------

ENTITIES = {
    "man": ("Man", dict(skin=0xC8A080, hair=0x5A3A1A, shirt=0x7A5A3A, pants=0x5A4A3A, shoes=0x3A2A1A)),
    "goblin": ("Goblin", dict(skin=0x6A9A3A, hair=0x4A6A2A, shirt=0x6A4A2A, pants=0x5A3A1A, shoes=0x3A2A1A,
                              ragged=True, eyes=(200, 40, 30), ears=True)),
    "al_kharid_warrior": ("Al-Kharid warrior", dict(skin=0xB08060, hair=0xD8C8A0, shirt=0xD0B888, pants=0xC0A070,
                                                    shoes=0x8A6A3A, sash=(180, 40, 40), hood=True)),
    "barbarian": ("Barbarian", dict(skin=0xD0A080, hair=0xE0C050, shirt=None, pants=0x8A6A4A, shoes=0x5A3A1A,
                                    fur=True, beard=0xE0C050)),
    "dark_wizard": ("Dark wizard", dict(skin=0xC8A080, hair=0x1A1A3A, shirt=0x1E1E4A, pants=0x14143A,
                                        shoes=0x101020, robe=True, hood=True, eyes=(200, 40, 40), beard=0x9A9AA0)),
    "guard": ("Guard", dict(skin=0xC8A080, hair=0x8A8A90, shirt=0x9A9AA2, pants=0x4A4A6A, shoes=0x2A2A2A,
                            chain=True, helmet=True)),
    "hill_giant": ("Hill Giant", dict(skin=0xC09A70, hair=0x5A3A1A, shirt=None, pants=0x7A5A3A, shoes=None,
                                      loincloth=True)),
    "moss_giant": ("Moss giant", dict(skin=0x6A7A3A, hair=0x3A5A1A, shirt=None, pants=0x4A5A2A, shoes=None,
                                      loincloth=True, moss=True)),
    "ghoul": ("Ghoul", dict(skin=0x7A8A70, hair=0x4A5040, shirt=0x5A5A4A, pants=0x4A4A3A, shoes=None,
                            ragged=True, rotten=True, eyes=(230, 230, 120))),
    "ice_giant": ("Ice giant", dict(skin=0xA8C8E8, hair=0xE0F0FF, shirt=None, pants=0x6A8AB0, shoes=None,
                                    loincloth=True, beard=0xE0F0FF)),
    "lesser_demon": ("Lesser demon", dict(skin=0xB02818, hair=0x601008, shirt=None, pants=None, shoes=None,
                                          horns=True, eyes=(255, 220, 60))),
    "greater_demon": ("Greater demon", dict(skin=0x781410, hair=0x400806, shirt=None, pants=None, shoes=None,
                                            horns=True, eyes=(255, 200, 40))),
    "black_demon": ("Black demon", dict(skin=0x222226, hair=0x0A0A0C, shirt=None, pants=None, shoes=None,
                                        horns=True, eyes=(230, 20, 20))),
}


ENTITIES.update({
    "obor": ("Obor", dict(skin=0xC09A70, hair=0x4A3018, shirt=None, pants=0x6A4A28, shoes=None,
                          loincloth=True, beard=0x4A3018)),
    "bryophyta": ("Bryophyta", dict(skin=0x5A7A30, hair=0x2A5A18, shirt=0x3A6A20, pants=0x3A6A20, shoes=None,
                                    robe=True, moss=True, eyes=(200, 240, 120))),
    "ahrim": ("Ahrim the Blighted", dict(skin=0xD8D0C8, hair=0x202024, shirt=0x2A2A30, pants=0x202026,
                                         shoes=0x101012, robe=True, hood=True, eyes=(120, 220, 255))),
    "dharok": ("Dharok the Wretched", dict(skin=0xB8A890, hair=0xB0A088, shirt=0x4A4A48, pants=0x404040,
                                           shoes=0x2A2A2A, plate=True, eyes=(230, 60, 40))),
    "guthan": ("Guthan the Infested", dict(skin=0xB8A890, hair=0x6A6A60, shirt=0x7A7A70, pants=0x4A4A40,
                                           shoes=0x2A2A2A, chain=True, helmet=True, helmet_col=(110, 110, 100),
                                           tabard=(70, 90, 50), eyes=(120, 230, 90))),
    "karil": ("Karil the Tainted", dict(skin=0xB8A890, hair=0x3A2A1A, shirt=0x5A4028, pants=0x4A3420,
                                        shoes=0x2A1A10, helmet=True, coif=True, helmet_col=(80, 60, 40),
                                        trim=(110, 80, 50), eyes=(100, 220, 230))),
    "torag": ("Torag the Corrupted", dict(skin=0xB8A890, hair=0x5A5A50, shirt=0x5A5A52, pants=0x4A4A44,
                                          shoes=0x2A2A2A, plate=True, helmet=True, helmet_col=(90, 90, 82),
                                          eyes=(240, 160, 50))),
    "verac": ("Verac the Defiled", dict(skin=0xB8A890, hair=0x3A3A36, shirt=0x3A3A38, pants=0x343432,
                                        shoes=0x1A1A1A, plate=True, helmet=True, helmet_col=(60, 60, 56),
                                        eyes=(230, 220, 90))),
    "general_graardor": ("General Graardor", dict(skin=0x6A7A3A, hair=0x4A5A28, shirt=0x5A6A30, pants=0x4A5A28,
                                                  shoes=0x3A3020, plate=True, trim=GOLD, ears=True,
                                                  eyes=(230, 200, 40))),
    "kril_tsutsaroth": ("K'ril Tsutsaroth", dict(skin=0x701818, hair=0x300808, shirt=None, pants=0x1A1012,
                                                 shoes=None, horns=True, loincloth=True, eyes=(255, 210, 50))),
    "tztok_jad": ("TzTok-Jad", dict(skin=0x2A1E1C, hair=0x1A1210, shirt=None, pants=None, shoes=None,
                                   molten=True, eyes=(255, 150, 30))),
    "count_draynor": ("Count Draynor", dict(skin=0xE8E4E0, hair=0x101010, shirt=0x1A1A1E, pants=0x141418,
                                            shoes=0x0A0A0A, cape=(15, 15, 18), collar=(170, 20, 25), long_sleeves=True,
                                            eyes=(220, 30, 30))),
    "delrith": ("Delrith", dict(skin=0xC03020, hair=0x701810, shirt=None, pants=None, shoes=None,
                                horns=True, eyes=(255, 230, 80))),
})

NPC_LOOKS = {
    "peasant": dict(skin=0xC8A080, hair=0x6A4A2A, shirt=0x8A7A5A, pants=0x6A5A3A, shoes=0x3A2A1A),
    "cook": dict(skin=0xD0A888, hair=0x5A3A1A, shirt=0xE8E8E8, pants=0x505060, shoes=0x2A2A2A, apron=True,
                 hat=("chef", (250, 250, 250))),
    "wizard": dict(skin=0xC8A080, hair=0xC0C0C8, shirt=0x2A40A0, pants=0x22388A, shoes=0x1A1A3A, robe=True,
                   hat=("pointy", (40, 60, 170)), beard=0xD0D0D8),
    "priest": dict(skin=0xC8A080, hair=0x8A8A88, shirt=0x1A1A1A, pants=0x1A1A1A, shoes=0x101010, robe=True,
                   trim=(240, 240, 240)),
    "noble": dict(skin=0xD8B898, hair=0x8A6A30, shirt=0x6A2A8A, pants=0x4A1A6A, shoes=0x2A1A10, trim=GOLD,
                  cape=(110, 30, 140), collar=GOLD),
    "dwarf": dict(skin=0xC89070, hair=0xC86A20, shirt=0x8A5A2A, pants=0x5A4A3A, shoes=0x3A2A1A,
                  big_beard=0xD07020, helmet=True, helmet_col=(120, 120, 128)),
    "knight": dict(skin=0xC8A080, hair=0x6A4A2A, shirt=0xA8A8B0, pants=0x8A8A92, shoes=0x4A4A50, chain=True,
                   tabard=(240, 240, 245), emblem=(40, 70, 180), helmet=True),
    "witch": dict(skin=0x9AB07A, hair=0x2A2A2A, shirt=0x1A1A1E, pants=0x141418, shoes=0x0A0A0A, robe=True,
                  hat=("pointy", (25, 25, 30)), eyes=(120, 230, 80)),
    "druid": dict(skin=0xC8A080, hair=0xE0E0E0, shirt=0xF0F0EC, pants=0xE8E8E4, shoes=0x6A5A3A, robe=True,
                  trim=(60, 150, 50), beard=0xE8E8E8, hood=True),
    "ghost": dict(skin=0xC8D4E8, hair=0xA8B8D0, shirt=0xB0C0DC, pants=0xA0B0D0, shoes=0x90A0C0, robe=True,
                  eyes=(40, 50, 80), alpha=180),
    "lady": dict(skin=0xE0C0A0, hair=0xB07030, shirt=0xD04060, pants=0xC03050, shoes=0x6A1A2A, robe=True,
                 trim=(250, 180, 200)),
}


def fill_rect(img, rect, col, rng, amount=0.08):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            put(img, x, y, shade(col, 1 + (rng.random() - 0.5) * 2 * amount))


def entity_texture(eid, o):
    img = new_img(64, 64)
    rng = rng_for("entity", eid)
    skin = rgb(o["skin"])
    hair = rgb(o["hair"])
    shirt = rgb(o["shirt"]) if o.get("shirt") is not None else skin
    pants = rgb(o["pants"]) if o.get("pants") is not None else skin
    shoes = rgb(o["shoes"]) if o.get("shoes") is not None else shade(skin, 0.85)
    if o.get("moss") or o.get("rotten"):
        amt = 0.22
    else:
        amt = 0.07

    # head
    head = box_faces(0, 0, 8, 8, 8)
    for f, r in head.items():
        fill_rect(img, r, skin, rng, amt)
    # hair: top, back, and the upper rows of the sides/front
    fill_rect(img, head["top"], hair, rng, 0.1)
    fill_rect(img, head["back"], hair, rng, 0.1)
    for f in ("right", "left"):
        x, y, w, h = head[f]
        fill_rect(img, (x, y, w, 3), hair, rng, 0.1)
        fill_rect(img, (x + (0 if f == "left" else w - 2), y + 3, 2, 3), hair, rng, 0.1)
    fx, fy = 8, 8  # head front face origin
    fill_rect(img, (fx, fy, 8, 2), hair, rng, 0.1)
    if o.get("hood"):
        hood = rgb(o["shirt"])
        fill_rect(img, head["top"], hood, rng, 0.06)
        fill_rect(img, head["back"], hood, rng, 0.06)
        for f in ("right", "left"):
            fill_rect(img, head[f], hood, rng, 0.06)
        fill_rect(img, (fx, fy, 8, 2), hood, rng, 0.06)
        fill_rect(img, (fx, fy, 1, 8), hood, rng, 0.06)
        fill_rect(img, (fx + 7, fy, 1, 8), hood, rng, 0.06)
    if o.get("helmet"):
        steel = rgb(o.get("helmet_col", (150, 150, 158)))
        for f in ("top", "back", "right", "left"):
            x, y, w, h = head[f]
            fill_rect(img, (x, y, w, h if f in ("top", "back") else (8 if o.get("coif") else 4)), steel, rng, 0.05)
        fill_rect(img, (fx, fy, 8, 3), steel, rng, 0.05)
        if o.get("coif"):
            fill_rect(img, (fx, fy, 1, 8), steel, rng, 0.05)
            fill_rect(img, (fx + 7, fy, 1, 8), steel, rng, 0.05)
        else:
            fill_rect(img, (fx + 3, fy + 3, 2, 2), steel, rng, 0.05)  # nose guard
    eyes = o.get("eyes", (255, 255, 255))
    pupil = (30, 30, 60) if "eyes" not in o else shade(eyes, 0.5)
    put(img, fx + 1, fy + 4, eyes)
    put(img, fx + 2, fy + 4, pupil if "eyes" not in o else eyes)
    put(img, fx + 5, fy + 4, pupil if "eyes" not in o else eyes)
    put(img, fx + 6, fy + 4, eyes)
    brow = shade(hair, 0.8)
    for x in (1, 2, 5, 6):
        put(img, fx + x, fy + 3, brow)
    put(img, fx + 3, fy + 5, shade(skin, 0.8))
    put(img, fx + 4, fy + 5, shade(skin, 0.8))
    mouth = (90, 30, 30) if not o.get("horns") else (40, 10, 10)
    for x in range(2, 6):
        put(img, fx + x, fy + 6, mouth)
    if o.get("horns") or o.get("ears") and False:
        pass
    if o.get("beard"):
        b = rgb(o["beard"])
        for x in range(1, 7):
            put(img, fx + x, fy + 7, b)
        put(img, fx + 1, fy + 6, b)
        put(img, fx + 6, fy + 6, b)
    if o.get("horns"):
        horn = (230, 220, 200) if eid != "black_demon" else (90, 20, 20)
        for (x, y) in ((0, 0), (1, 0), (1, 1), (6, 0), (7, 0), (6, 1)):
            put(img, fx + x, fy + y, horn)
        tx, ty, _, _ = head["top"]
        for (x, y) in ((1, 3), (1, 4), (6, 3), (6, 4)):
            put(img, tx + x, ty + y, horn)
        # fangs
        put(img, fx + 2, fy + 7, (240, 240, 230))
        put(img, fx + 5, fy + 7, (240, 240, 230))
    if o.get("ears"):
        for f in ("right", "left"):
            x, y, w, h = head[f]
            fill_rect(img, (x + 3, y + 3, 2, 3), shade(skin, 1.1), rng, 0.04)
    # hat overlay region (32..64 x 0..16) left transparent

    # body / arms / legs (right side at classic positions, left side at 1.8 positions)
    body = box_faces(16, 16, 8, 12, 4)
    arms = [box_faces(40, 16, 4, 12, 4), box_faces(32, 48, 4, 12, 4)]
    legs = [box_faces(0, 16, 4, 12, 4), box_faces(16, 48, 4, 12, 4)]
    for f, r in body.items():
        fill_rect(img, r, shirt, rng, amt)
    for a in arms:
        for f, r in a.items():
            fill_rect(img, r, skin, rng, amt)
            if o.get("shirt") is not None and f not in ("top", "bottom"):
                x, y, w, h = r
                sleeve = h if o.get("robe") or o.get("chain") or o.get("plate") or o.get("long_sleeves") else 4
                fill_rect(img, (x, y, w, sleeve), shirt, rng, amt)
        fill_rect(img, a["top"], shirt if o.get("shirt") is not None else skin, rng, amt)
    for lg in legs:
        for f, r in lg.items():
            fill_rect(img, r, pants, rng, amt)
            if f not in ("top", "bottom"):
                x, y, w, h = r
                fill_rect(img, (x, y + h - 3, w, 3), shoes, rng, amt)
        fill_rect(img, lg["bottom"], shoes, rng, amt)
    if o.get("robe"):
        for lg in legs:
            for f, r in lg.items():
                if f not in ("top", "bottom"):
                    x, y, w, h = r
                    fill_rect(img, (x, y, w, h - 1), shirt, rng, 0.06)
    if o.get("loincloth") or o.get("fur"):
        cloth = (140, 100, 60) if o.get("fur") else rgb(o["pants"])
        for lg in legs:
            for f, r in lg.items():
                if f not in ("top", "bottom"):
                    x, y, w, h = r
                    fill_rect(img, (x, y, w, h), skin, rng, amt)
                    fill_rect(img, (x, y, w, 4 if o.get("loincloth") else 6), cloth, rng, 0.2 if o.get("fur") else 0.06)
                    if o.get("fur"):
                        fill_rect(img, (x, y + h - 3, w, 3), shoes, rng, 0.08)
        for f in ("front", "back", "right", "left"):
            x, y, w, h = body[f]
            fill_rect(img, (x, y + h - 2, w, 2), cloth, rng, 0.15)
            put(img, x, y + h - 3, shade(cloth, 0.6))
    if o.get("shirt") is None:
        # bare chest: shading for pecs / belly
        x, y, w, h = body["front"]
        for (dx, dy) in ((1, 3), (2, 3), (5, 3), (6, 3), (3, 6), (4, 6), (3, 8), (4, 8)):
            put(img, x + dx, y + dy, shade(skin, 0.8))
    if o.get("sash"):
        x, y, w, h = body["front"]
        for i in range(8):
            put(img, x + i, y + i, o["sash"])
            put(img, x + i, y + i + 1, shade(o["sash"], 0.75))
        for f in ("front", "back", "right", "left"):
            bx, by, bw, bh = body[f]
            fill_rect(img, (bx, by + 7, bw, 1), (120, 80, 40), rng, 0.04)
    if o.get("chain"):
        for f, r in body.items():
            x, y, w, h = r
            for yy in range(y, y + h):
                for xx in range(x, x + w):
                    if (xx + yy) % 2 == 0:
                        put(img, xx, yy, shade(shirt, 0.75))
        bx, by, bw, bh = body["front"]
        tab = rgb(o.get("tabard", (180, 30, 30)))
        if o.get("tabard") is not False:
            fill_rect(img, (bx, by, bw, 2), tab, rng, 0.04)  # tabard collar
            fill_rect(img, (bx + 2, by + 2, 4, 8), tab, rng, 0.04)
            emblem = rgb(o.get("emblem", GOLD))
            put(img, bx + 3, by + 4, emblem)
            put(img, bx + 4, by + 4, emblem)
            if o.get("emblem"):
                for dy in range(3, 9):
                    put(img, bx + 3, by + dy, emblem)
                    put(img, bx + 4, by + dy, emblem)
                for dx in range(2, 6):
                    put(img, bx + dx, by + 5, emblem)
    if o.get("ragged"):
        for f in ("front", "back", "right", "left"):
            x, y, w, h = body[f]
            for xx in range(x, x + w):
                if rng.random() < 0.5:
                    put(img, xx, y + h - 1, skin)
    if o.get("moss"):
        for _ in range(140):
            x, y = rng.randrange(64), rng.randrange(64)
            if opaque(img, x, y) and not (32 <= x < 64 and y < 16):
                put(img, x, y, (60, 110, 40) if rng.random() < 0.6 else (90, 140, 50))
    if o.get("rotten"):
        for _ in range(80):
            x, y = rng.randrange(64), rng.randrange(64)
            if opaque(img, x, y) and not (32 <= x < 64 and y < 16):
                put(img, x, y, (80, 90, 60) if rng.random() < 0.5 else (110, 100, 80))
    extra_features(img, o, rng, head, body, arms, legs, skin, shirt, fx, fy)
    if eid == "black_demon":
        put(img, fx + 1, fy + 4, (255, 40, 40))
        put(img, fx + 6, fy + 4, (255, 40, 40))
    # make sure eyes survive noise layers
    if "eyes" in o:
        for x in (1, 2, 5, 6):
            put(img, fx + x, fy + 4, eyes)
    if o.get("alpha"):
        for y in range(64):
            for x in range(64):
                px = img.getpixel((x, y))
                if px[3] > 0:
                    img.putpixel((x, y), px[:3] + (o["alpha"],))
    return img


def extra_features(img, o, rng, head, body, arms, legs, skin, shirt, fx, fy):
    """Optional costume details used by bosses and quest NPCs."""
    sides = ("front", "back", "right", "left")
    if o.get("plate"):
        for part in [body] + arms + legs:
            for f in sides:
                x, y, w, h = part[f]
                for xx in range(x, x + w):
                    put(img, xx, y, mix(img.getpixel((xx, y)), WHITE, 0.25))
                    for yy in range(y + 4, y + h - 1, 4):
                        put(img, xx, yy, shade(img.getpixel((xx, yy)), 0.65))
        if o.get("trim"):
            t = rgb(o["trim"])
            for f in sides:
                x, y, w, h = body[f]
                for xx in range(x, x + w):
                    put(img, xx, y, t)
                    put(img, xx, y + h - 1, t)
                if f == "front":
                    put(img, x + w // 2, y + 5, t)
    elif o.get("trim"):
        t = rgb(o["trim"])
        for f in sides:
            x, y, w, h = body[f]
            for xx in range(x, x + w):
                put(img, xx, y + h - 1, t)
            if f == "front":
                for yy in range(y, y + h):
                    put(img, x + 3, yy, t)
                    put(img, x + 4, yy, t)
            for part in legs + arms:
                lx, ly, lw, lh = part[f]
                for xx in range(lx, lx + lw):
                    put(img, xx, ly + lh - (2 if part in legs else 5), t)
    if o.get("apron"):
        x, y, w, h = body["front"]
        fill_rect(img, (x + 1, y + 2, w - 2, h - 2), (245, 245, 240), rng, 0.03)
        for lg in legs:
            lx, ly, lw, lh = lg["front"]
            fill_rect(img, (lx, ly, lw, 6), (245, 245, 240), rng, 0.03)
    if o.get("cape"):
        c = rgb(o["cape"])
        fill_rect(img, body["back"], c, rng, 0.04)
        for lg in legs:
            fill_rect(img, lg["back"], c, rng, 0.04)
        for part in arms:
            x, y, w, h = part["back"]
            fill_rect(img, (x, y, w, h), c, rng, 0.04)
        col = rgb(o.get("collar", c))
        for f in ("front", "right", "left"):
            x, y, w, h = body[f]
            fill_rect(img, (x, y, w, 2), col, rng, 0.04)
        x, y, w, h = body["front"]
        for i in range(3):
            put(img, x + i, y + 2 + i, col)
            put(img, x + w - 1 - i, y + 2 + i, col)
    if o.get("hat"):
        kind, c = o["hat"]
        c = rgb(c)
        rows = 3 if kind == "chef" else 2
        fill_rect(img, head["top"], c, rng, 0.04)
        for f in ("front", "back", "right", "left"):
            x, y, w, h = head[f]
            fill_rect(img, (x, y, w, rows), c, rng, 0.04)
            for xx in range(x, x + w):
                put(img, xx, y + rows, shade(c, 0.7) if kind != "chef" else (215, 215, 210))
        if kind == "pointy":
            tx, ty, _, _ = head["top"]
            for (dx, dy) in ((3, 3), (4, 4), (3, 4), (4, 3)):
                put(img, tx + dx, ty + dy, mix(c, WHITE, 0.4))
            put(img, fx + 4, fy, (240, 220, 60))
    if o.get("big_beard"):
        b = rgb(o["big_beard"])
        for (dx, dy) in [(x, y) for x in range(0, 8) for y in range(5, 8)]:
            if not (dy == 5 and 2 < dx < 5):
                put(img, fx + dx, fy + dy, b)
        for f in ("right", "left"):
            x, y, w, h = head[f]
            fill_rect(img, (x, y + 5, w, 3), b, rng, 0.08)
        x, y, w, h = body["front"]
        fill_rect(img, (x + 1, y, w - 2, 5), b, rng, 0.08)
        fill_rect(img, (x + 2, y + 5, w - 4, 2), b, rng, 0.08)
    if o.get("molten"):
        for _ in range(55):
            x, y = rng.randrange(64), rng.randrange(64)
            if 32 <= x < 64 and y < 16:
                continue
            for k in range(rng.randint(2, 5)):
                if opaque(img, x, y):
                    put(img, x, y, (255, 200, 60) if k == 0 else (240, 110, 20))
                x += rng.choice((-1, 0, 1))
                y += 1


def gen_entities():
    names = {}
    for eid, (nm, o) in ENTITIES.items():
        save_png(entity_texture(eid, o), os.path.join(ASSETS, "textures", "entity", eid + ".png"), "entity textures")
        names[eid] = nm
    for look, o in NPC_LOOKS.items():
        save_png(entity_texture("npc_" + look, o),
                 os.path.join(ASSETS, "textures", "entity", f"npc_{look}.png"), "entity textures")
    names["quest_npc"] = "Gielinor resident"
    return names


# ----------------------------------------------------------------------------------------------
# GUI skill icons + interface panel
# ----------------------------------------------------------------------------------------------

def bitmap(img, rows, colours, ox=0, oy=0):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in colours:
                put(img, ox + i, oy + j, colours[ch])


SKILL_BITMAPS = {
    "hitpoints": (["..........", ".##...##..", "#ll#.#mm#.", "#lmm#mmmd#", "#mmmmmmmd#", ".#mmmmmd#.",
                   "..#mmmd#..", "...#md#...", "....##....", ".........."],
                  {"#": (90, 10, 10), "l": (255, 120, 120), "m": (220, 30, 30), "d": (150, 15, 15)}),
    "strength": (["..........", "..###.....", ".#lll#....", ".#lmm#.##.", "..#lm##ll#", "..#lmmlmmd#",
                  ".#lmmmmmmd#", ".#lmmmmmd#.", "..#ddddd#..", "...#####..."],
                 {"#": (60, 40, 20), "l": (240, 200, 150), "m": (210, 160, 110), "d": (160, 110, 70)}),
    "agility": (["....##....", "....##....", "..####....", ".#.###.##.", "...###....", "...#.#....",
                 "..#...#...", ".#.....#..", "#.........", "......... "],
                {"#": (40, 50, 150)}),
    "prayer": (["....#.....", "...#l#....", "...#l#....", "#########.", ".#lllllm#.", "..#llm#...",
                ".#lm#lm#..", ".#m#.#m#..", "#m#...#m#.", "##.....##."],
               {"#": (120, 110, 40), "l": (255, 250, 200), "m": (230, 210, 120)}),
    "magic": (["....##....", "...#ll#...", "...#lm#...", "..#lmmm#..", "..#lmym#..", ".#lmmmmm#.",
               ".#lmmmmm#.", "#mmmmmmmm#", "##########", ".........."],
              {"#": (20, 20, 70), "l": (110, 110, 230), "m": (60, 60, 190), "y": (240, 220, 60)}),
    "thieving": ([".........", "##########", "#mmmmmmmm#", "#m##mm##m#", "#m#bmm#bm#", "#mmmmmmmm#",
                  ".#mmmmmm#.", "..######..", ".........."],
                 {"#": (20, 20, 20), "m": (70, 60, 90), "b": (230, 230, 230)}),
    "cooking": (["..#####...", ".#wwwww#..", "#wwwwwww#.", "#wwwwwww#.", ".#wwwww#..", "..#www#...",
                 "..#www#...", "..#ggg#...", "..#####...", ".........."],
                {"#": (90, 90, 90), "w": (245, 245, 245), "g": (200, 200, 200)}),
    "firemaking": (["....#.....", "...#y#....", "...#y#.#..", "..#yoy#o#.", ".#yoooyo#.", ".#yorroy#.",
                    "#oorrrroo#", ".#orrrro#.", "..#wwww#..", ".#wwwwww#."],
                   {"#": (120, 30, 10), "y": (255, 230, 80), "o": (250, 140, 30), "r": (220, 50, 20),
                    "w": WOOD}),
    "slayer": (["..######..", ".#wwwwww#.", "#wwwwwwww#", "#w##ww##w#", "#w##ww##w#", "#wwww wwww"[:10],
                ".#ww##ww#.", "..#wwww#..", "..#w#w##..", "...####..."],
               {"#": (30, 20, 20), "w": (230, 225, 210)}),
    "farming": (["....gg....", "...gllg...", "..glllg...", "..gllg..g.", "...gg..glg", "....#.glg.",
                 "....##gg..", "...#ddd#..", "..#ddddd#.", "..#######."],
                {"g": (40, 110, 30), "l": (110, 200, 70), "#": (70, 45, 20), "d": (140, 90, 40)}),
    "construction": (["##########", "#ssssssss#", "#ssssssss#", "#s#s#s#s##", "..........", "...hhhh...",
                      "...hhhh...", "....ww....", "....ww....", "....ww...."],
                     {"#": (70, 70, 75), "s": (190, 190, 200), "h": (90, 90, 95), "w": WOOD}),
    "hunter": (["..#....#..", ".#l#..#l#.", ".#l#..#l#.", "#l#....#l#", "..........", "...####...",
                "..#llll#..", ".#llllll#.", ".#llllll#.", "..######.."],
               {"#": (60, 40, 20), "l": (170, 120, 70)}),
    "sailing": (["....##....", "...#..#...", "....##....", "##########", "....##....", "....##....",
                 "#...##...#", "##..##..##", ".###..###.", "...####..."],
                {"#": (60, 80, 120)}),
}


def gen_skill_icons(cat):
    by_id = {i["id"]: i for i in cat["items"]}
    item_based = {
        "attack": ("bronze_longsword", None), "mining": ("bronze_pickaxe", None), "smithing": ("hammer", None),
        "defence": ("bronze_kiteshield", None), "herblore": ("guam_leaf", None), "fishing": ("raw_shrimps", None),
        "ranged": ("shortbow", None), "crafting": ("chisel", None), "fletching": ("bronze_arrow", None),
        "woodcutting": ("bronze_axe", None), "runecraft": ("air_rune", None),
    }
    fallback_shapes = {"bronze_longsword": "longsword", "bronze_pickaxe": "pickaxe", "hammer": "hammer",
                       "bronze_kiteshield": "kiteshield", "guam_leaf": "herb", "raw_shrimps": "fish_raw",
                       "shortbow": "shortbow", "chisel": "chisel", "bronze_arrow": "arrow",
                       "bronze_axe": "axe", "air_rune": "rune"}
    skills = ["attack", "hitpoints", "mining", "strength", "agility", "smithing", "defence", "herblore",
              "fishing", "ranged", "thieving", "cooking", "prayer", "crafting", "firemaking", "magic",
              "fletching", "woodcutting", "runecraft", "slayer", "farming", "construction", "hunter", "sailing"]
    tints = {"attack": 0xB0B0B8, "mining": 0x9A9AA0, "defence": 0x8A8AC0, "herblore": 0x3A9A3A,
             "fishing": 0x6A8AB0, "ranged": 0x8A5A2A, "fletching": 0xB0B0B8, "woodcutting": 0xA0A0A8,
             "runecraft": 0xE0E0E8, "crafting": 0, "smithing": 0}
    for sk in skills:
        if sk in item_based:
            iid = item_based[sk][0]
            it = dict(by_id.get(iid) or {"id": iid, "shape": fallback_shapes[iid], "color": 0x909090})
            if it.get("shape") != "rune" and tints.get(sk):
                it["color"] = tints[sk]
            img = render_item(it)
        else:
            rows, cols = SKILL_BITMAPS[sk]
            img = new_img()
            w = max(len(r) for r in rows)
            bitmap(img, rows, cols, (16 - w) // 2, (16 - len(rows)) // 2)
            outline(img, 0.3)
        save_png(img, os.path.join(ASSETS, "textures", "gui", "skill", sk + ".png"), "gui textures")


def gen_interface():
    rng = rng_for("interface")
    img = new_img(256, 256)
    base = (0x5A, 0x4A, 0x38)
    for y in range(256):
        for x in range(256):
            n = (rng.random() - 0.5) * 0.12
            tile = ((x // 8) + (y // 8)) % 2
            c = shade(base, 1 + n + (0.03 if tile else -0.03))
            put(img, x, y, c)
    # bevelled border
    for i in range(256):
        for k, c in ((0, (30, 24, 18)), (1, (130, 110, 80)), (2, (90, 75, 55))):
            put(img, i, k, c)
            put(img, k, i, c)
            put(img, i, 255 - k, shade(c, 0.6))
            put(img, 255 - k, i, shade(c, 0.6))
    # inset slot area (useful as an inventory/skill-panel background)
    for y in range(16, 240):
        for x in range(16, 240):
            if x in (16, 239) or y in (16, 239):
                put(img, x, y, (35, 28, 20) if x == 16 or y == 16 else (120, 100, 75))
            else:
                put(img, x, y, shade(img.getpixel((x, y)), 0.88))
    save_png(img, os.path.join(ASSETS, "textures", "gui", "interface.png"), "gui textures")


# ----------------------------------------------------------------------------------------------
# Lang
# ----------------------------------------------------------------------------------------------

BIOMES = ["misthalin", "lumbridge_swamp", "asgarnia", "wilderness", "kharidian_desert", "morytania", "kandarin",
          "feldip_hills", "fremennik_province", "troll_country", "karamja", "tirannwn", "uncharted_isles"]


def gen_lang(cat, block_names, entity_names):
    lang = {}
    for it in cat["items"]:
        lang[f"item.{NS}.{it['id']}"] = it["name"]
    for bid, nm in block_names.items():
        lang[f"block.{NS}.{bid}"] = nm
    for eid, nm in entity_names.items():
        lang[f"entity.{NS}.{eid}"] = nm
    for b in BIOMES:
        lang[f"biome.{NS}.{b}"] = " ".join(w.capitalize() for w in b.split("_"))
    lang.update({
        "itemGroup.runecraft.items": "RuneCraft Items",
        "itemGroup.runecraft.equipment": "RuneCraft Equipment",
        "itemGroup.runecraft.blocks": "RuneCraft World",
        "key.categories.runecraft": "RuneCraft",
        "key.runecraft.interface": "Open RuneScape interface",
        "key.runecraft.collection_log": "Open collection log",
        "death.attack.runecraft.osrs_hit": "%1$s was slain",
        "death.attack.runecraft.osrs_hit.player": "%1$s was slain by %2$s",
        "dimension.runecraft.gielinor": "Gielinor",
    })
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang, "lang", sort_keys=True)
    return lang


# ----------------------------------------------------------------------------------------------
# Validation
# ----------------------------------------------------------------------------------------------

def resolve(ref, kind):
    ns, _, path = ref.partition(":")
    if not path:
        ns, path = "minecraft", ref
    return ns, os.path.join(RES, "assets", ns, kind, path + (".png" if kind == "textures" else ".json"))


def validate():
    errors = []
    for p in WRITTEN_JSON:
        try:
            with open(p, encoding="utf-8") as f:
                json.load(f)
        except Exception as e:  # noqa: BLE001
            errors.append(f"bad json {p}: {e}")
    model_dir = os.path.join(ASSETS, "models")
    for dp, _, fs in os.walk(model_dir):
        for fn in fs:
            p = os.path.join(dp, fn)
            m = json.load(open(p, encoding="utf-8"))
            for k, ref in m.get("textures", {}).items():
                if ref.startswith("#"):
                    continue
                ns, tp = resolve(ref, "textures")
                if ns == NS and not os.path.exists(tp):
                    errors.append(f"{p}: missing texture {ref}")
            par = m.get("parent")
            if par:
                ns, mp = resolve(par, "models")
                if ns == NS and not os.path.exists(mp):
                    errors.append(f"{p}: missing parent {par}")
            for el in m.get("elements", []):
                for c in el["from"] + el["to"]:
                    if not -16 <= c <= 32:
                        errors.append(f"{p}: element out of range")
                for face in el.get("faces", {}).values():
                    t = face["texture"]
                    if t.startswith("#") and t[1:] not in m.get("textures", {}):
                        errors.append(f"{p}: undefined texture var {t}")
    for dp, _, fs in os.walk(os.path.join(ASSETS, "blockstates")):
        for fn in fs:
            p = os.path.join(dp, fn)
            bs = json.load(open(p, encoding="utf-8"))
            refs = []
            for v in bs.get("variants", {}).values():
                refs += [x["model"] for x in (v if isinstance(v, list) else [v])]
            for part in bs.get("multipart", []):
                a = part["apply"]
                refs += [x["model"] for x in (a if isinstance(a, list) else [a])]
            for r in refs:
                ns, mp = resolve(r, "models")
                if ns == NS and not os.path.exists(mp):
                    errors.append(f"{p}: missing model {r}")
    return errors


def main():
    if not os.path.exists(CATALOG):
        sys.exit(f"missing {CATALOG}; run tools/run_core_tests.sh first")
    with open(CATALOG, encoding="utf-8") as f:
        cat = json.load(f)
    gen_items(cat)
    gen_armour()
    block_names = gen_blocks(cat)
    gen_loot_tables()
    entity_names = gen_entities()
    gen_skill_icons(cat)
    gen_interface()
    lang = gen_lang(cat, block_names, entity_names)

    print("RuneCraft asset generation summary:")
    for k in sorted(COUNTS):
        print(f"  {k:20s} {COUNTS[k]}")
    print(f"  {'TOTAL':20s} {sum(COUNTS.values())}")
    print(f"  lang entries         {len(lang)}")
    print(f"  blocks               {len(block_names)}")
    if FALLBACK_SHAPES:
        print("  shapes using generic fallback:", ", ".join(sorted(map(str, FALLBACK_SHAPES))))
    else:
        print("  shapes using generic fallback: none")
    errors = validate()
    if errors:
        print("VALIDATION FAILED:")
        for e in errors[:50]:
            print("  " + e)
        sys.exit(1)
    print("Validation passed: all JSON parses, all referenced runecraft textures/models exist.")


if __name__ == "__main__":
    main()
