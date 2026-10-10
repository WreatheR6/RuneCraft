#!/usr/bin/env python3
"""Generate RuneCraft client assets (textures, models, blockstates, lang) and block loot tables.

Reads build/catalog.json (produced by tools/run_core_tests.sh) and writes everything under
src/main/resources/. Deterministic (seeded randomness) and idempotent (overwrites outputs).

Usage: python3 tools/gen_assets.py [--catalog build/catalog.json] [--out src/main/resources]
Requires: Python 3 + Pillow.
"""
import argparse
import json
import math
import os
import random
import re
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
WARNINGS = []


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


def lum(c):
    r, g, b = rgb(c)
    return (r * 299 + g * 587 + b * 114) / 1000


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


# ---- extra shapes for content modules -------------------------------------------------------

def blob_poly(img, pts, pal, noise=None, hl=True):
    pix = mask(lambda d: d.polygon(pts, fill=255))
    bevel(img, pix, pal, noise=noise, hl=hl)
    return pix


def blob_ellipse(img, box, pal, noise=None):
    pix = mask(lambda d: d.ellipse(box, fill=255))
    bevel(img, pix, pal, noise=noise)
    return pix


def polyline(img, pts, col):
    for k in range(len(pts) - 1):
        line(img, *pts[k], *pts[k + 1], col)


def sphere(img, cx, cy, r, pal, spec=True):
    """Radially shaded ball, lit from the top-left."""
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                lit = math.hypot(x - (cx - r * 0.4), y - (cy - r * 0.4)) / (r * 1.6)
                c = pal.h if lit < 0.25 else pal.l if lit < 0.5 else pal.m if lit < 0.85 else pal.d
                put(img, x, y, c)
    if spec:
        put(img, int(round(cx - r * 0.45)), int(round(cy - r * 0.45)), WHITE)


def draw_whip(img, pal, item, rng):
    grip = Pal(mix(pal.m, (60, 40, 30), 0.5))
    for i in range(5):
        put(img, 1 + i, 14 - i, grip.l if i % 2 else grip.m)
        put(img, 2 + i, 14 - i, grip.d)
    for (x, y) in ((6, 9), (7, 9), (6, 8)):
        put(img, x, y, pal.h)
    pts = [(7, 8), (9, 6), (11, 4), (13, 2), (14, 4), (13, 6), (11, 7), (10, 9), (11, 11), (13, 12)]
    for k in range(len(pts) - 1):
        line(img, *pts[k], *pts[k + 1], pal.l if k < 3 else pal.m if k < 6 else pal.d)


def draw_godsword(img, pal, item, rng):
    blade = Pal(mix(STEEL, WHITE, 0.2))

    def fn(u, v):
        if u < 0:
            return None
        if u <= 1 and abs(v) <= 0.5:
            return pal.h
        if 1 < u <= 3.5 and v == 0:
            return pal.d if u % 1 == 0 else pal.m
        if 3.5 < u <= 5 and abs(v) <= 3:
            if u >= 4.5 and v == 0:
                return (230, 50, 40)  # hilt gem
            return pal.h if abs(v) >= 2.5 else pal.l if v < 0 else pal.m
        if 5 < u <= 13.5:
            w = 1.5 if u < 11.5 else max(0, (13.5 - u) * 0.75)
            if abs(v) <= w:
                return metal_shade(v, blade, w)
        return None
    rdiag(img, fn)


def draw_trident(img, pal, item, rng):
    head = pal

    def fn(u, v):
        if 9.5 <= u <= 10 and abs(v) <= 2:
            return head.d
        if v == 0 and 10 < u <= 13.5:
            return head.h if u > 12.5 else head.l
        if abs(v) == 2 and 10 < u <= 12.5:
            return head.l if v < 0 else head.m
        if 0 <= u < 9.5 and v == 0:
            return mix(head.d, DARK_WOOD, 0.4) if u % 2 else head.m
        return None
    rdiag(img, fn)


def draw_blowpipe(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 1 and v == 0:
            return (60, 50, 40)
        if 5 <= u <= 8 and abs(v) <= 1.5:
            return pal.h if v < -0.5 else pal.l if v <= 0.5 else pal.d
        if 1 < u <= 12.5 and abs(v) <= 0.5:
            return pal.l if v < 0 else pal.m
        if 12.5 < u <= 13 and abs(v) <= 1:
            return pal.d
        return None
    rdiag(img, fn)


def draw_dart(img, pal, item, rng):
    def fn(u, v):
        if 6.5 <= u <= 9 and abs(v) <= (9 - u) * 0.5:
            return pal.l if v <= 0 else pal.m
        if 2 <= u < 6.5 and v == 0:
            return pal.d
        if 1 <= u <= 3 and abs(v) == 1:
            return (230, 230, 230) if v < 0 else (190, 60, 50)
        return None
    rdiag(img, fn, 3, 12)


def draw_javelin(img, pal, item, rng):
    def fn(u, v):
        if 11 <= u <= 13.5 and abs(v) <= min(1, (13.5 - u) * 0.5):
            return pal.l if v <= 0 else pal.m
        if 0 <= u < 11 and v == 0:
            return WOOD if u % 2 else DARK_WOOD
        return None
    rdiag(img, fn)


def draw_bolt(img, pal, item, rng):
    shaft = Pal(STEEL) if item.get("tier") not in ("bronze",) else Pal(WOOD)

    def fn(u, v):
        for off in (-3, 0, 3):
            if v == off:
                if 8 <= u <= 9.5:
                    return pal.h if u == 9.5 else pal.l
                if 3 <= u < 8:
                    return shaft.m if u % 2 else shaft.d
            if abs(v - off) == 0.5 and 3 <= u <= 4.5:
                return (220, 220, 220)
        return None
    rdiag(img, fn, 1, 13)


def draw_crossbow_u(img, pal, item, rng):
    draw_crossbow(img, pal, item, rng)


def draw_stock(img, pal, item, rng):
    def fn(u, v):
        if 1 <= u <= 11.5 and abs(v) <= 0.5:
            return pal.l if v < 0 else pal.m
        if 9.5 <= u <= 11.5 and abs(v) <= 1.5:
            return pal.d
        if 4 <= u <= 5 and 0.5 < v <= 2 and u - 4 <= v - 0.5:
            return pal.d
        return None
    rdiag(img, fn)


def draw_limbs(img, pal, item, rng):
    def fn(u, v):
        if abs(v) <= 5.5:
            uc = 8.5 - 0.14 * v * v
            if abs(u - uc) <= 0.5 or (abs(u - uc) <= 1.0 and abs(v) < 2.5):
                return pal.h if v < -3 else pal.l if v < 0 else pal.m
        return None
    rdiag(img, fn)


def draw_wand(img, pal, item, rng):
    def fn(u, v):
        if 2 <= u <= 9 and v == 0:
            return mix(pal.d, DARK_WOOD, 0.5) if u % 2 else pal.d
        if 9.5 <= u <= 11 and abs(v) <= 0.5:
            return pal.h if v < 0 else pal.l
        return None
    rdiag(img, fn, 2, 13)
    return [(12, 3, WHITE), (13, 5, mix(pal.h, WHITE, 0.5)), (10, 2, mix(pal.h, WHITE, 0.5))]


def draw_book(img, pal, item, rng):
    pix = {(x, y) for x in range(4, 13) for y in range(2, 15)}
    bevel(img, pix, pal)
    for y in range(2, 15):
        put(img, 3, y, pal.dd)
        put(img, 4, y, pal.d)
    for y in range(3, 14):
        put(img, 13, y, (235, 230, 210) if y % 2 else (210, 205, 185))
    for (x, y) in ((8, 5), (7, 6), (8, 6), (9, 6), (6, 7), (8, 7), (10, 7), (7, 8), (8, 8), (9, 8), (8, 9)):
        put(img, x, y, GOLD)
    for x in range(6, 12):
        put(img, x, 12, shade(GOLD, 0.8))


def draw_orb(img, pal, item, rng):
    sphere(img, 7.5, 7.5, 5.6, pal)
    put(img, 9, 10, pal.l)


def draw_shard(img, pal, item, rng):
    pts = [(6, 1), (10, 4), (12, 11), (8, 14), (4, 9)]
    pix = mask(lambda d: d.polygon(pts, fill=255))
    for (x, y) in pix:
        put(img, x, y, pal.l if x < 7 + (y - 1) * 0.15 else pal.d if y > 10 else pal.m)
    line(img, 6, 2, 8, 13, pal.h)
    put(img, 6, 3, WHITE)


def draw_casket(img, pal, item, rng):
    body = Pal(mix(pal.m, WOOD, 0.4))
    bevel(img, {(x, y) for x in range(2, 14) for y in range(7, 14)}, body, noise=rng)
    lid = mask(lambda d: d.chord((2, 2, 13, 11), 180, 360, fill=255)) | {(x, 6) for x in range(2, 14)}
    bevel(img, lid, Pal(body.l))
    steel = Pal(STEEL)
    for x in (4, 11):
        for y in range(3, 14):
            if opaque(img, x, y):
                put(img, x, y, steel.l if y < 7 else steel.m)
    for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8)):
        put(img, x, y, GOLD if y < 8 else shade(GOLD, 0.7))


def scroll_shape(img, pal, ink=True):
    paper = Pal(mix(pal.m, (230, 215, 170), 0.6))
    bevel(img, {(x, y) for x in range(4, 12) for y in range(3, 13)}, paper)
    for y in (2, 13):
        for x in range(3, 13):
            put(img, x, y, paper.d if x in (3, 12) else paper.l)
        put(img, 3, y, paper.dd)
    if ink:
        for y in range(5, 12, 2):
            for x in range(5, 11):
                if (x * 7 + y * 3) % 5:
                    put(img, x, y, (90, 70, 50))
    return paper


def draw_scroll(img, pal, item, rng):
    scroll_shape(img, pal)
    put(img, 8, 13, (180, 30, 30))
    put(img, 8, 14, (150, 20, 20))


def draw_map(img, pal, item, rng):
    paper = Pal(mix(pal.m, (225, 205, 150), 0.6))
    bevel(img, {(x, y) for x in range(2, 14) for y in range(3, 13)}, paper)
    for y in range(3, 13):
        put(img, 6, y, paper.d)
        put(img, 10, y, paper.l)
    for (x, y) in ((3, 10), (4, 9), (6, 9), (7, 8), (8, 6), (9, 6), (10, 5)):
        put(img, x, y, (150, 40, 30))
    for (x, y) in ((11, 4), (12, 5), (12, 4), (11, 5)):
        put(img, x, y, (200, 30, 30))
    for (x, y) in ((4, 5), (5, 5), (4, 6)):
        put(img, x, y, (60, 120, 60))


def draw_gloves(img, pal, item, rng):
    pix = {(x, y) for x in range(5, 11) for y in range(6, 14)}
    for k, x in enumerate((5, 7, 9)):
        pix |= {(x, y) for y in range(2 + (k == 1) * -1 + 1, 7)} | {(x + 1, y) for y in range(3, 7)}
    pix |= {(3, 8), (4, 8), (3, 9), (4, 9), (4, 10), (4, 7)}
    bevel(img, pix, pal)
    for x in range(5, 11):
        put(img, x, 12, pal.dd)
        put(img, x, 13, pal.d)


def draw_vambraces(img, pal, item, rng):
    for ox in (2, 9):
        pix = {(x, y) for x in range(ox, ox + 5) for y in range(3, 14)}
        bevel(img, pix, pal, noise=rng)
        for x in range(ox, ox + 5):
            put(img, x, 4, pal.dd)
            put(img, x, 12, pal.dd)
        put(img, ox + 2, 8, pal.h)


def draw_bracelet(img, pal, item, rng):
    band = Pal(GOLD) if item.get("id", "").find("gold") >= 0 or pal.m == rgb(0x808080) else pal
    for (x, y) in mask(lambda d: d.ellipse((2, 4, 13, 12), outline=255, width=3)):
        put(img, x, y, band.l if y < 7 else band.m if y < 10 else band.d)
    for (x, y, c) in ((7, 4, pal.h), (8, 4, pal.l), (7, 5, pal.m), (8, 5, pal.d)):
        put(img, x, y, c)


def draw_tiara(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(1, 10), (3, 6), (5, 9), (8, 4), (11, 9), (13, 6), (14, 10), (14, 12), (1, 12)],
                                   fill=255))
    bevel(img, pix, pal)
    put(img, 8, 7, (230, 60, 60))
    put(img, 8, 6, WHITE)


def draw_pouch(img, pal, item, rng):
    bag = blob_ellipse(img, (3, 6, 12, 14), pal, noise=rng)
    bevel(img, {(x, y) for x in range(6, 10) for y in range(3, 7)} - bag, Pal(pal.l))
    for x in range(5, 11):
        put(img, x, 6, (200, 180, 120))
    put(img, 10, 7, (200, 180, 120))
    put(img, 11, 8, (200, 180, 120))
    for (x, y) in ((7, 9), (8, 9), (7, 10), (8, 10), (6, 11), (9, 11)):
        put(img, x, y, pal.h)


def draw_jar(img, pal, item, rng):
    glass = Pal(GLASS)
    body = {(x, y) for x in range(4, 12) for y in range(4, 15)} - {(4, 14), (11, 14)}
    for (x, y) in body:
        put(img, x, y, glass.m, 110)
    if item.get("id") not in ("jar", "empty_jar"):
        lp = Pal(pal.m)
        for (x, y) in body:
            if y >= 7:
                put(img, x, y, lp.l if y == 7 else (lp.m if x < 9 else lp.d))
    for (x, y) in body:
        if (x - 1, y) not in body or (x + 1, y) not in body or (x, y + 1) not in body:
            put(img, x, y, glass.l, 230)
    for x in range(4, 12):
        put(img, x, 2, (150, 110, 70))
        put(img, x, 3, (120, 85, 50))
    put(img, 5, 6, WHITE)
    put(img, 5, 7, WHITE, 200)
    return "no_outline"


def draw_butterfly_net(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 8 and v == 0:
            return WOOD if u % 2 else DARK_WOOD
        d = math.hypot(u - 11, v)
        if 2.1 <= d <= 2.9:
            return (200, 200, 205)
        if d < 2.1 and (u * 2 + v * 2) % 2 == 0:
            return (240, 240, 235)
        return None
    rdiag(img, fn)


def pot_base(img, top=8):
    clay = Pal((170, 90, 50))
    pix = mask(lambda d: d.polygon([(4, top), (11, top), (10, 14), (5, 14)], fill=255))
    bevel(img, pix, clay)
    for x in range(3, 13):
        put(img, x, top, clay.l)
        put(img, x, top + 1, clay.d)
    for x in range(5, 11):
        put(img, x, top, (70, 45, 25))


def draw_seedling(img, pal, item, rng):
    pot_base(img, 9)
    leaf = Pal(mix((70, 160, 50), pal.m, 0.3))
    line(img, 7, 8, 7, 5, leaf.d)
    for (x, y) in ((5, 5), (6, 4), (6, 5), (8, 4), (9, 4), (8, 3)):
        put(img, x, y, leaf.l if y < 5 else leaf.m)


def draw_sapling(img, pal, item, rng):
    pot_base(img, 10)
    leaf = Pal(mix((60, 150, 45), pal.m, 0.35))
    line(img, 7, 9, 7, 3, DARK_WOOD)
    for (cx, cy) in ((5, 4), (10, 3), (5, 7), (10, 6), (7, 2)):
        for (x, y) in mask(lambda d, b=(cx - 2, cy - 1, cx + 1, cy + 1): d.ellipse(b, fill=255)):
            put(img, x, y, leaf.l if y < cy else leaf.m)


def bucket_shape(img, pal_inside=None):
    steel = Pal((150, 150, 158))
    pix = mask(lambda d: d.polygon([(3, 5), (12, 5), (11, 14), (4, 14)], fill=255))
    bevel(img, pix, steel)
    for y in (8, 12):
        for x in range(3, 13):
            if opaque(img, x, y):
                put(img, x, y, steel.d)
    for (x, y) in mask(lambda d: d.arc((3, 0, 12, 10), 180, 360, fill=255)):
        put(img, x, y, (110, 110, 115))
    for (x, y) in mask(lambda d: d.ellipse((3, 3, 12, 7), fill=255)):
        put(img, x, y, steel.l)
    inner = mask(lambda d: d.ellipse((4, 4, 11, 6), fill=255))
    for (x, y) in inner:
        put(img, x, y, pal_inside if pal_inside else (45, 45, 50))


def draw_bucket(img, pal, item, rng):
    iid = item.get("id", "")
    bucket_shape(img, None if iid in ("bucket", "empty_bucket") else pal.l)


def draw_compost(img, pal, item, rng):
    bucket_shape(img, None)
    c = Pal(mix(pal.m, (80, 55, 30), 0.5))
    for (x, y) in mask(lambda d: d.ellipse((3, 2, 12, 7), fill=255)):
        put(img, x, y, c.l if rng.random() < 0.3 else c.m if rng.random() < 0.7 else c.d)


def draw_glass(img, pal, item, rng):
    p = Pal(mix(pal.m, GLASS, 0.5))
    pix = mask(lambda d: d.polygon([(3, 9), (5, 4), (9, 3), (13, 6), (12, 12), (7, 13)], fill=255))
    for (x, y) in pix:
        put(img, x, y, p.m, 170)
    for (x, y) in pix:
        if (x - 1, y) not in pix or (x, y - 1) not in pix:
            put(img, x, y, p.h, 230)
        elif (x + 1, y) not in pix or (x, y + 1) not in pix:
            put(img, x, y, p.d, 230)
    put(img, 6, 6, WHITE)
    put(img, 7, 5, WHITE)
    return "no_outline"


def dish(img, y0=7):
    tin = Pal((160, 160, 168))
    for (x, y) in mask(lambda d: d.ellipse((1, y0, 14, 14), fill=255)):
        put(img, x, y, tin.m if y > (y0 + 14) // 2 else tin.l)


def draw_pie(img, pal, item, rng):
    dish(img, 7)
    crust = Pal((215, 160, 80))
    top = mask(lambda d: d.ellipse((2, 4, 13, 11), fill=255))
    for (x, y) in top:
        lattice = (x + y) % 3 == 0 or (x - y) % 3 == 0
        put(img, x, y, crust.l if lattice else pal.m)
    for (x, y) in top:
        if (x, y - 1) not in top or (x - 1, y) not in top or (x + 1, y) not in top:
            put(img, x, y, crust.m)


def draw_pizza(img, pal, item, rng):
    crust = Pal((210, 150, 70))
    pie = mask(lambda d: d.ellipse((1, 2, 14, 14), fill=255))
    for (x, y) in pie:
        edge = any((x + dx, y + dy) not in pie for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        put(img, x, y, crust.m if edge else (235, 200, 80) if (x * 3 + y) % 7 else (220, 90, 40))
    for (x, y) in ((5, 5), (9, 6), (6, 10), (10, 10), (7, 7), (4, 8), (11, 8)):
        put(img, x, y, pal.m)
        put(img, x + 1, y, pal.d)


def draw_stew(img, pal, item, rng):
    bowl = Pal((140, 90, 50))
    pix = mask(lambda d: d.chord((1, 2, 14, 14), 0, 180, fill=255))
    bevel(img, pix, bowl)
    for (x, y) in mask(lambda d: d.ellipse((2, 5, 13, 10), fill=255)):
        put(img, x, y, pal.m if (x * 5 + y * 3) % 7 else pal.l)
    for (x, y) in ((5, 7), (9, 6), (7, 8)):
        put(img, x, y, (200, 120, 60))


def draw_wine(img, pal, item, rng):
    jug = Pal((120, 160, 120) if item.get("id") != "jug_of_wine" else (90, 130, 90))
    body = mask(lambda d: (d.ellipse((3, 6, 12, 14), fill=255), d.rectangle((6, 2, 9, 7), fill=255)))
    bevel(img, body, jug)
    liquid = Pal(pal.m)
    for (x, y) in body:
        if 8 <= y <= 12 and 4 <= x <= 10:
            put(img, x, y, liquid.l if x < 6 else liquid.m)
    for (x, y) in mask(lambda d: d.arc((9, 5, 14, 11), 270, 90, fill=255)):
        if x >= 12:
            put(img, x, y, jug.d)
    put(img, 5, 9, WHITE)


def draw_bread(img, pal, item, rng):
    loaf = Pal(mix(pal.m, (200, 140, 60), 0.6))
    pix = mask(lambda d: d.ellipse((1, 5, 14, 13), fill=255))
    bevel(img, pix, loaf)
    for (x0, y0) in ((4, 7), (7, 6), (10, 7)):
        line(img, x0, y0 + 2, x0 + 2, y0, loaf.h)


def draw_defender(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 3 and v == 0:
            return pal.d
        if 0 <= u <= 3 and v == 1.5:
            return pal.m
        if 3 < u <= 4 and abs(v) <= 2.5:
            return pal.h if v < 0 else pal.l
        if 4 < u <= 10.5:
            w = 1.0 if u < 9 else max(0, (10.5 - u) * 0.6)
            if abs(v) <= w:
                return metal_shade(v, pal, w)
        return None
    rdiag(img, fn, 2, 13)


def draw_halberd(img, pal, item, rng):
    def fn(u, v):
        if 9 <= u <= 12 and 0.5 <= v <= 3.5:
            t = (u - 10.5) / 1.6
            if v <= 1 + 2.5 * max(0.0, 1 - t * t):
                return pal.h if v > 2.8 else pal.l if v < 2 else pal.m
        if 11 <= u <= 13.5 and v == 0:
            return pal.h
        if 10 <= u <= 11 and -2 <= v <= -0.5:
            return pal.d
        return _handle(u, v, 0, 11, WOOD if u % 2 else DARK_WOOD)
    rdiag(img, fn)


def draw_mace(img, pal, item, rng):
    def fn(u, v):
        if 7.5 <= u <= 11.5 and abs(v) <= 2:
            if abs(v) == 2:
                return pal.h if u % 1 == 0 else pal.d
            return pal.l if v < 0 else pal.m
        if 11.5 < u <= 12.5 and v == 0:
            return pal.h
        return _handle(u, v, 0, 7.5, shade(pal.d, 0.8) if u % 2 else DARK_WOOD)
    rdiag(img, fn)


def draw_claws(img, pal, item, rng):
    grip = Pal(shade(pal.m, 0.6))
    bevel(img, {(x, y) for x in range(3, 11) for y in range(10, 14)}, grip)
    for k, pts in enumerate(([(4, 10), (3, 6), (4, 3), (6, 1)], [(7, 10), (7, 5), (9, 2)],
                             [(10, 10), (11, 6), (13, 4)])):
        polyline(img, pts, pal.l if k != 1 else pal.h)
        polyline(img, [(x + 1, y) for (x, y) in pts], pal.m)


def draw_maul(img, pal, item, rng):
    def fn(u, v):
        if 9 <= u <= 13 and abs(v) <= 3:
            if u in (9, 13) or abs(v) == 3:
                return pal.d
            return pal.l if v < 0 else pal.m
        return _handle(u, v, 0, 9, mix(pal.d, DARK_WOOD, 0.5))
    rdiag(img, fn)


def draw_hasta(img, pal, item, rng):
    def fn(u, v):
        if 9.5 <= u <= 13.5 and abs(v) <= min(1.5, (13.5 - u) * 0.5, (u - 9) * 0.8):
            return pal.h if v < 0 else pal.l if v == 0 else pal.m
        if 9 <= u < 9.5 and abs(v) <= 1:
            return GOLD
        return _handle(u, v, 0, 9, shade(pal.m, 0.5) if u % 2 else WOOD)
    rdiag(img, fn)


def draw_rapier(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 2 and v == 0:
            return pal.d
        if math.hypot(u - 3, v) <= 1.6 and u <= 3.5:
            return pal.l if v < 0 else pal.m
        if 3.5 < u <= 13.5 and v == 0:
            return pal.h if u > 11 else STEEL
        if 3.5 < u <= 12 and v == 0.5:
            return shade(STEEL, 0.75)
        return None
    rdiag(img, fn)


def draw_scythe(img, pal, item, rng):
    line(img, 2, 14, 11, 3, DARK_WOOD)
    line(img, 3, 14, 12, 3, WOOD)
    blade = [(12, 3), (9, 1), (6, 1), (3, 2), (1, 4)]
    polyline(img, blade, pal.h)
    polyline(img, [(x, y + 1) for (x, y) in blade[:-1]], pal.m)
    polyline(img, [(11, 5), (8, 3), (5, 3)], pal.d)
    put(img, 7, 9, GOLD)


def draw_shadow(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 9.5 and v == 0:
            return mix(pal.d, GOLD, 0.35) if u % 2 else pal.d
        if 9.5 < u <= 10.5 and abs(v) <= 2:
            return GOLD
        if 10.5 < u <= 12.5 and abs(v) == 2:
            return shade(GOLD, 0.8)
        return None
    rdiag(img, fn)
    sphere(img, 12, 3.5, 1.6, pal)


def draw_crystal(img, pal, item, rng):
    for pts in ([(3, 13), (3, 7), (5, 4), (7, 7), (7, 13)], [(9, 13), (9, 6), (11, 3), (13, 6), (13, 13)],
                [(5, 14), (6, 4), (8, 1), (10, 4), (10, 14)]):
        pix = mask(lambda d, p=pts: d.polygon(p, fill=255))
        mid = (min(x for x, _ in pts) + max(x for x, _ in pts)) / 2
        for (x, y) in pix:
            put(img, x, y, pal.h if x < mid - 0.5 else pal.l if x <= mid else pal.m)
    put(img, 7, 4, WHITE)


def draw_totem(img, pal, item, rng):
    wood = Pal(mix(pal.m, WOOD, 0.5))
    bevel(img, {(x, y) for x in range(5, 11) for y in range(2, 15)}, wood, noise=rng)
    bevel(img, {(x, y) for x in range(2, 14) for y in range(4, 6)}, Pal(wood.l))
    for y0 in (7, 11):
        put(img, 6, y0, BLACK)
        put(img, 9, y0, BLACK)
        for x in range(6, 10):
            put(img, x, y0 + 2, wood.dd)
    put(img, 7, 2, pal.h)
    put(img, 8, 2, pal.h)


def draw_icon(img, pal, item, rng):
    frame = Pal(GOLD)
    bevel(img, mask(lambda d: d.rounded_rectangle((2, 1, 13, 14), radius=3, fill=255)), frame)
    bevel(img, {(x, y) for x in range(4, 12) for y in range(3, 13)}, pal)
    for y in range(4, 12):
        put(img, 7, y, frame.h)
        put(img, 8, y, frame.l)
    for x in range(5, 11):
        put(img, x, 6, frame.h)


def draw_mask(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((3, 2, 12, 13), fill=255)))
    bevel(img, pix, pal)
    for (x, y) in ((5, 6), (6, 6), (9, 6), (10, 6), (5, 7), (10, 7)):
        put(img, x, y, BLACK)
    for x in range(6, 10):
        put(img, x, 10, pal.dd)
    for y in (4, 5):
        put(img, 7, y, pal.h)


def draw_hood(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((2, 1, 13, 12), fill=255), d.polygon([(2, 8), (13, 8), (14, 14), (1, 14)], fill=255)))
    bevel(img, pix, pal, noise=rng)
    for (x, y) in mask(lambda d: d.ellipse((5, 5, 10, 13), fill=255)):
        put(img, x, y, shade(pal.dd, 0.6) if y > 6 else pal.dd)


def draw_robe_top(img, pal, item, rng):
    pts = [(1, 12), (4, 3), (6, 2), (9, 2), (11, 3), (14, 12), (12, 12), (11, 8), (11, 14), (4, 14), (4, 8), (3, 12)]
    pix = mask(lambda d: d.polygon(pts, fill=255))
    bevel(img, pix, pal)
    trim = mix(pal.m, GOLD, 0.6) if lum(pal.m) < 160 else shade(pal.m, 0.6)
    for y in range(3, 14):
        put(img, 7, y, trim)
    for x in range(4, 12):
        if opaque(img, x, 13):
            put(img, x, 13, trim)


def draw_robe_bottom(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(4, 2), (11, 2), (14, 14), (1, 14)], fill=255))
    bevel(img, pix, pal)
    for x in range(4, 12):
        put(img, x, 3, pal.dd)
    for x0 in (5, 8, 10):
        line(img, x0, 5, x0 + (x0 - 7) // 2, 13, pal.d)
    for x in range(1, 15):
        if opaque(img, x, 14):
            put(img, x, 14, mix(pal.m, GOLD, 0.5) if lum(pal.m) < 160 else pal.d)


def draw_coif(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((2, 1, 13, 12), fill=255), d.rectangle((2, 7, 13, 14), fill=255)))
    face = mask(lambda d: d.ellipse((5, 5, 10, 12), fill=255))
    body = pix - face
    bevel(img, body, pal)
    for (x, y) in body:
        if (x + y) % 2 == 0:
            put(img, x, y, pal.d)


def draw_shield_round(img, pal, item, rng):
    pix = blob_ellipse(img, (1, 1, 14, 14), pal, noise=rng)
    rim = Pal(STEEL)
    for (x, y) in pix:
        if any((x + dx, y + dy) not in pix for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            put(img, x, y, rim.l if x + y < 15 else rim.d)
    sphere(img, 7.5, 7.5, 2.0, rim)


def draw_bulwark(img, pal, item, rng):
    pix = {(x, y) for x in range(2, 14) for y in range(3, 15)}
    bevel(img, pix, pal)
    for x in (2, 5, 8, 11):
        put(img, x + 1, 2, pal.h)
        put(img, x + 1, 1, pal.l)
    for (x, y) in ((4, 5), (11, 5), (4, 12), (11, 12), (7, 8), (8, 8)):
        put(img, x, y, pal.h)
    for y in range(4, 14):
        put(img, 7, y, pal.d)


def draw_ward(img, pal, item, rng):
    pts = [(7, 1), (8, 1), (13, 5), (12, 10), (8, 14), (7, 14), (3, 10), (2, 5)]
    pix = mask(lambda d: d.polygon(pts, fill=255))
    frame = Pal(mix(pal.m, (200, 190, 150), 0.5))
    bevel(img, pix, frame)
    inner = mask(lambda d: d.polygon([(7, 4), (8, 4), (10, 7), (8, 11), (7, 11), (5, 7)], fill=255))
    gl = Pal(mix(pal.m, WHITE, 0.25))
    for (x, y) in inner:
        put(img, x, y, gl.h if y < 6 else gl.l if y < 9 else gl.m)


def draw_egg(img, pal, item, rng):
    pix = blob_ellipse(img, (4, 2, 11, 14), pal)
    for _ in range(6):
        x, y = rng.randint(5, 10), rng.randint(4, 12)
        if (x, y) in pix:
            put(img, x, y, pal.d)


def draw_heart(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((1, 2, 8, 9), fill=255), d.ellipse((7, 2, 14, 9), fill=255),
                          d.polygon([(1, 6), (14, 6), (8, 14), (7, 14)], fill=255)))
    bevel(img, pix, pal)
    put(img, 4, 4, WHITE)


def draw_insignia(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(4, 2), (11, 2), (12, 8), (8, 14), (7, 14), (3, 8)], fill=255))
    bevel(img, pix, Pal(GOLD))
    for (x, y) in mask(lambda d: d.polygon([(6, 4), (9, 4), (10, 8), (8, 11), (7, 11), (5, 8)], fill=255)):
        put(img, x, y, pal.l if y < 7 else pal.m)
    for (x, y) in ((1, 4), (2, 5), (14, 4), (13, 5), (1, 6), (14, 6)):
        put(img, x, y, shade(GOLD, 0.8))


def draw_prayer_scroll(img, pal, item, rng):
    scroll_shape(img, pal if lum(pal.m) > 120 else Pal((220, 200, 150)), ink=False)
    glow = Pal(pal.m)
    for (x, y) in ((7, 5), (8, 5), (6, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8), (6, 9), (9, 9), (7, 10), (8, 10)):
        put(img, x, y, glow.l if y < 8 else glow.m)


def draw_horn(img, pal, item, rng):
    for k in range(12):
        t = k / 11
        cx = 3 + t * 9
        cy = 13 - math.sin(t * math.pi * 0.5) * 9 - t * 1
        r = 2.4 * (1 - t) + 0.5
        for (x, y) in mask(lambda d, b=(cx - r, cy - r, cx + r, cy + r): d.ellipse(b, fill=255)):
            put(img, x, y, pal.h if y < cy - r * 0.3 else pal.l if y < cy + r * 0.3 else pal.m)
    for (x, y) in ((4, 12), (5, 11), (7, 9)):
        put(img, x, y, pal.d)


def draw_fang(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(4, 2), (11, 2), (11, 5), (9, 11), (7, 14), (6, 9), (5, 5)], fill=255))
    bevel(img, pix, pal)
    for x in range(4, 12):
        put(img, x, 2, shade(pal.m, 0.7))


def draw_visage(img, pal, item, rng):
    pts = [(2, 2), (5, 4), (10, 4), (13, 2), (12, 8), (10, 13), (5, 13), (3, 8)]
    pix = mask(lambda d: d.polygon(pts, fill=255))
    bevel(img, pix, pal, noise=rng)
    for (x, y) in ((5, 7), (6, 7), (9, 7), (10, 7)):
        put(img, x, y, (255, 150, 40))
    for x in range(6, 10):
        put(img, x, 11, pal.dd)
    put(img, 7, 9, pal.d)
    put(img, 8, 9, pal.d)


def draw_tentacle(img, pal, item, rng):
    for k in range(24):
        t = k / 23
        cx = 2 + t * 11
        cy = 13 - t * 10 + math.sin(t * 7) * 1.6
        r = 1.8 * (1 - t) + 0.4
        for (x, y) in mask(lambda d, b=(cx - r, cy - r, cx + r, cy + r): d.ellipse(b, fill=255)):
            put(img, x, y, pal.l if x - cx < -0.3 else pal.m)
    for (x, y) in ((4, 12), (6, 10), (8, 8), (10, 6)):
        put(img, x, y, pal.h)


def draw_core(img, pal, item, rng):
    sphere(img, 7.5, 7.5, 4.0, Pal(mix(pal.m, WHITE, 0.2)))
    for (x, y) in mask(lambda d: d.ellipse((1, 5, 14, 10), outline=255)):
        if not (4 <= x <= 11 and 5 <= y <= 7):
            put(img, x, y, pal.l)


def draw_sigil(img, pal, item, rng):
    pix = blob_ellipse(img, (2, 2, 13, 13), Pal((90, 88, 95)))
    sym = ["..#..", ".###.", "#####", ".###.", "..#.."]
    for j, row in enumerate(sym):
        for i, ch in enumerate(row):
            if ch == "#":
                put(img, 5 + i, 5 + j, pal.h if j < 2 else pal.l)
    for (x, y) in pix:
        if any((x + dx, y + dy) not in pix for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            put(img, x, y, shade(GOLD, 0.85))


def draw_claw(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(3, 13), (8, 13), (11, 9), (12, 4), (10, 1), (10, 5), (7, 9)], fill=255))
    bevel(img, pix, pal)
    for x in range(3, 9):
        put(img, x, 13, shade(pal.m, 0.6))


def draw_teeth(img, pal, item, rng):
    gum = Pal((170, 70, 70))
    tooth = pal if lum(pal.m) > 150 else Pal((235, 228, 205))
    for (x, y) in mask(lambda d: d.arc((1, 2, 14, 15), 180, 360, fill=255, width=2)):
        put(img, x, y, gum.m if y > 4 else gum.l)
    for k in range(5):
        a = math.radians(200 + k * 35)
        x, y = int(round(7 + 5 * math.cos(a))), int(round(9 + 5.2 * math.sin(a)))
        for (dx, dy, c) in ((0, 1, tooth.h), (1, 1, tooth.l), (0, 2, tooth.l), (1, 2, tooth.m), (0, 3, tooth.m)):
            put(img, x + dx, y + dy, c)


def draw_ash(img, pal, item, rng):
    p = Pal(mix(pal.m, (110, 110, 110), 0.6))
    pix = mask(lambda d: d.pieslice((1, 6, 14, 22), 180, 360, fill=255))
    bevel(img, pix, p, noise=rng)
    for _ in range(8):
        put(img, rng.randint(3, 12), rng.randint(10, 13), p.dd if rng.random() < 0.5 else p.h)


def draw_sack(img, pal, item, rng):
    p = Pal(mix(pal.m, (160, 130, 80), 0.5))
    blob_poly(img, [(4, 5), (11, 5), (14, 11), (12, 14), (3, 14), (1, 11)], p, noise=rng)
    blob_poly(img, [(6, 2), (9, 2), (10, 5), (5, 5)], Pal(p.l))
    for x in range(5, 11):
        put(img, x, 5, DARK_WOOD)


def draw_tool_generic(img, pal, item, rng):
    p = Pal(mix(pal.m, STEEL, 0.5))

    def fn(u, v):
        if 8 <= u <= 12 and abs(v) <= 1.5:
            if u >= 11 and v == 0:
                return None
            return p.l if v < 0 else p.m
        return _handle(u, v, 0, 8, WOOD)
    rdiag(img, fn)


def draw_ingot(img, pal, item, rng):
    draw_bar(img, pal, item, rng)


EXTRA_SHAPES = {
    "whip": draw_whip, "godsword": draw_godsword, "trident": draw_trident, "blowpipe": draw_blowpipe,
    "dart": draw_dart, "javelin": draw_javelin, "bolt": draw_bolt, "bolts": draw_bolt, "crossbow_u": draw_crossbow_u,
    "stock": draw_stock, "limbs": draw_limbs, "wand": draw_wand, "book": draw_book, "orb": draw_orb,
    "shard": draw_shard, "casket": draw_casket, "scroll": draw_scroll, "map": draw_map, "gloves": draw_gloves,
    "vambraces": draw_vambraces, "bracelet": draw_bracelet, "tiara": draw_tiara, "pouch": draw_pouch,
    "jar": draw_jar, "butterfly_net": draw_butterfly_net, "seedling": draw_seedling, "sapling": draw_sapling,
    "compost": draw_compost, "glass": draw_glass, "pie": draw_pie, "pizza": draw_pizza, "stew": draw_stew,
    "wine": draw_wine, "bread": draw_bread, "defender": draw_defender, "halberd": draw_halberd,
    "mace": draw_mace, "claws": draw_claws, "maul": draw_maul, "hasta": draw_hasta, "rapier": draw_rapier,
    "scythe": draw_scythe, "shadow": draw_shadow, "crystal": draw_crystal, "ingot": draw_ingot,
    "totem": draw_totem, "icon": draw_icon, "mask": draw_mask, "hood": draw_hood, "robe_top": draw_robe_top,
    "robe_bottom": draw_robe_bottom, "coif": draw_coif, "shield_round": draw_shield_round,
    "bulwark": draw_bulwark, "ward": draw_ward, "egg": draw_egg, "heart": draw_heart,
    "insignia": draw_insignia, "prayer_scroll": draw_prayer_scroll, "horn": draw_horn, "fang": draw_fang,
    "visage": draw_visage, "tentacle": draw_tentacle, "core": draw_core, "sigil": draw_sigil,
    "claw": draw_claw, "teeth": draw_teeth, "ash": draw_ash, "ashes": draw_ash, "sack": draw_sack,
    "bucket": draw_bucket, "tool_generic": draw_tool_generic, "blob": draw_blob,
}


# ---- shapes introduced by the merged content modules ----------------------------------------

def ring_px(x, y, pix):
    return any((x + dx, y + dy) not in pix for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def draw_bell(img, pal, item, rng):
    b = Pal(mix(pal.m, GOLD, 0.5))
    blob_poly(img, [(7, 2), (9, 2), (11, 5), (12, 11), (14, 12), (14, 13), (2, 13), (2, 12), (4, 11), (5, 5)], b)
    for x in range(3, 14):
        put(img, x, 12, b.d)
    put(img, 8, 14, (90, 70, 40))
    put(img, 8, 1, b.d)
    line(img, 6, 5, 6, 10, b.h)


def draw_birdhouse(img, pal, item, rng):
    wood = Pal(mix(pal.m, WOOD, 0.3))
    bevel(img, {(x, y) for x in range(4, 12) for y in range(7, 15)}, wood, noise=rng)
    roof = mask(lambda d: d.polygon([(8, 1), (14, 7), (13, 8), (8, 3), (3, 8), (2, 7)], fill=255))
    for (x, y) in roof:
        put(img, x, y, wood.dd if y > 5 else wood.d)
    for (x, y) in mask(lambda d: d.ellipse((6, 8, 9, 11), fill=255)):
        put(img, x, y, (25, 18, 12))
    line(img, 7, 12, 9, 12, DARK_WOOD)
    for (x, y) in mask(lambda d: d.polygon([(8, 4), (11, 7), (5, 7)], fill=255)):
        put(img, x, y, wood.m)


def draw_boat(img, pal, item, rng):
    wood = Pal(mix(pal.m, WOOD, 0.4))
    hull = mask(lambda d: d.polygon([(1, 9), (14, 9), (12, 13), (3, 13)], fill=255))
    for (x, y) in hull:
        put(img, x, y, wood.l if y == 9 else wood.dd if y == 13 else wood.m if y % 2 else wood.d)
    line(img, 7, 1, 7, 8, DARK_WOOD)
    sail = mask(lambda d: d.polygon([(8, 1), (13, 7), (8, 7)], fill=255))
    for (x, y) in sail:
        put(img, x, y, (235, 228, 205) if x < 11 else (205, 198, 175))
    for (x, y) in mask(lambda d: d.polygon([(6, 3), (6, 7), (3, 7)], fill=255)):
        put(img, x, y, (215, 208, 185))


def draw_bowl(img, pal, item, rng):
    iid = item.get("id", "")
    bowl = Pal((150, 100, 60))
    pix = mask(lambda d: d.chord((1, 1, 14, 14), 0, 180, fill=255))
    bevel(img, pix, bowl)
    rim = mask(lambda d: d.ellipse((1, 5, 14, 10), fill=255))
    if iid == "bowl":
        fill = (60, 38, 22)
    elif "water" in iid:
        fill = (60, 110, 220)
    else:
        fill = pal.m
    for (x, y) in rim:
        put(img, x, y, bowl.l if ring_px(x, y, rim) else fill)
    if iid not in ("bowl",) and "water" not in iid:
        for (x, y) in ((5, 7), (9, 7), (7, 8)):
            put(img, x, y, light(fill, 1.3))
    else:
        put(img, 5, 7, light(fill, 1.4))


def draw_brick(img, pal, item, rng):
    top = [(x, y) for y in range(4, 7) for x in range(3 + (6 - y), 13 + (6 - y)) if x < 15]
    for (x, y) in top:
        put(img, x, y, pal.h if y == 4 else pal.l)
    bevel(img, {(x, y) for x in range(1, 12) for y in range(7, 13)}, pal, noise=rng)
    for y in range(5, 13):
        for x in (12, 13, 14):
            if y - 4 <= (x - 11) * 1 + 6 and not opaque(img, x, y) and y < 13 - (x - 12):
                put(img, x, y, pal.d)


def draw_cannonball(img, pal, item, rng):
    p = Pal((70, 70, 76))
    for (cx, cy) in ((5, 10), (10, 10), (7.5, 5.5)):
        sphere(img, cx, cy, 3.4, p)


def draw_clockwork(img, pal, item, rng):
    p = Pal(mix(pal.m, GOLD, 0.4))
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r, a = math.hypot(dx, dy), math.atan2(dy, dx)
            tooth = (math.cos(a * 8) > 0.2)
            if r <= 4.6 or (r <= 6.2 and tooth):
                if r <= 1.6:
                    continue
                put(img, x, y, p.h if dx + dy < -3 else p.l if dx + dy < 1 else p.m if dx + dy < 4 else p.d)
    for (x, y) in mask(lambda d: d.ellipse((5, 5, 10, 10), outline=255)):
        put(img, x, y, p.dd)


def draw_crate_item(img, pal, item, rng):
    wood = Pal(mix(pal.m, WOOD, 0.5))
    front = {(x, y) for x in range(2, 12) for y in range(6, 14)}
    bevel(img, front, wood)
    for y in range(3, 6):
        for x in range(2 + (6 - y), 12 + (6 - y)):
            put(img, x, y, wood.h)
    for y in range(3, 12):
        for x in (12, 13):
            if y - 3 <= (x - 11) * 3 + 6 and (x, y) not in front:
                put(img, x, y, wood.d)
    line(img, 2, 6, 11, 13, wood.d)
    for x in range(2, 12):
        put(img, x, 9, wood.d)


def draw_cup(img, pal, item, rng):
    iid = item.get("id", "")
    china = Pal((235, 235, 240))
    pix = mask(lambda d: (d.chord((2, 0, 12, 14), 0, 180, fill=255), d.rectangle((2, 6, 12, 8), fill=255)))
    bevel(img, pix, china)
    for (x, y) in mask(lambda d: d.arc((10, 7, 15, 12), 270, 90, fill=255, width=2)):
        if x >= 12:
            put(img, x, y, china.d)
    if iid != "empty_cup":
        liq = (110, 60, 30) if "tea" in iid else (180, 210, 240) if "water" in iid else pal.m
        for (x, y) in mask(lambda d: d.ellipse((3, 5, 11, 8), fill=255)):
            put(img, x, y, liq)
    for x in range(2, 13):
        put(img, x, 14, (200, 200, 205))
    if "hot" in iid or "tea" in iid:
        for (x, y) in ((5, 3), (6, 2), (9, 3), (8, 1)):
            put(img, x, y, (230, 230, 235), 160)


def draw_dough(img, pal, item, rng):
    p = Pal(mix(pal.m, (235, 215, 170), 0.6))
    pix = blob_poly(img, [(2, 9), (4, 5), (8, 4), (12, 5), (14, 9), (12, 13), (4, 13)], p)
    for (x, y) in ((6, 7), (10, 8), (7, 10)):
        put(img, x, y, p.d)


def draw_emblem(img, pal, item, rng):
    pix = blob_ellipse(img, (2, 2, 13, 13), pal)
    gold = Pal(GOLD)
    for (x, y) in pix:
        if ring_px(x, y, pix):
            put(img, x, y, gold.m if x + y < 15 else gold.d)
    for (x, y) in ((7, 4), (8, 4), (6, 5), (9, 5), (5, 7), (10, 7), (6, 9), (9, 9), (7, 10), (8, 10), (7, 7), (8, 7)):
        put(img, x, y, gold.l)


def draw_fangs(img, pal, item, rng):
    for ox, flip in ((2, 1), (8, -1)):
        pts = [(ox, 2), (ox + 5, 2), (ox + 4, 7), (ox + 3 + flip, 13), (ox + 1, 7)]
        blob_poly(img, pts, pal)
        for x in range(ox, ox + 6):
            put(img, x, 2, shade(pal.m, 0.6))


def draw_head(img, pal, item, rng):
    iid = item.get("id", "") + " " + (item.get("name") or "").lower()
    p = Pal(pal.m)
    if "ensouled" in iid:
        p = Pal(mix(pal.m, (110, 120, 100), 0.6))
    pix = mask(lambda d: (d.ellipse((2, 2, 13, 12), fill=255), d.rectangle((4, 9, 11, 14), fill=255)))
    if any(k in iid for k in ("dragon", "vorkath", "hydra", "drake", "wyrm", "kbd")):
        pix |= mask(lambda d: d.polygon([(2, 3), (0, 0), (5, 2)], fill=255)) | \
            mask(lambda d: d.polygon([(13, 3), (15, 0), (10, 2)], fill=255))
    elif any(k in iid for k in ("demon", "abyssal", "kril", "sire")):
        pix |= {(3, 1), (3, 0), (12, 1), (12, 0), (2, 2), (13, 2)}
    bevel(img, pix, p, noise=rng)
    eye = (255, 210, 50) if "ensouled" not in iid else (120, 240, 160)
    for (x, y) in ((5, 6), (6, 6), (9, 6), (10, 6)):
        put(img, x, y, eye)
    for x in range(5, 11):
        put(img, x, 10, p.dd)
    for (x, y) in ((5, 11), (10, 11)):
        put(img, x, y, (240, 235, 220))
    for x in range(4, 12):
        put(img, x, 14, (150, 30, 30))


def draw_kbd_heads(img, pal, item, rng):
    for (ox, oy, c) in ((1, 4, (60, 60, 64)), (9, 4, (60, 60, 64)), (5, 1, (40, 40, 44))):
        p = Pal(c)
        pix = mask(lambda d, o=(ox, oy): (d.ellipse((o[0], o[1] + 2, o[0] + 6, o[1] + 8), fill=255),
                                          d.polygon([(o[0], o[1] + 3), (o[0] + 1, o[1]), (o[0] + 2, o[1] + 3)], fill=255)))
        bevel(img, pix, p)
        put(img, ox + 2, oy + 4, (255, 120, 30))
        put(img, ox + 4, oy + 4, (255, 120, 30))
    for x in range(3, 13):
        put(img, x, 14, (150, 30, 30))


def draw_hilt(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 1 and abs(v) <= 1:
            return pal.h if v < 0 else pal.l
        if 1 < u <= 6 and v == 0:
            return pal.d if u % 1 == 0 else pal.m
        if 6 < u <= 8 and abs(v) <= 3.5:
            if u >= 7 and v == 0:
                return (230, 50, 40)
            return pal.h if abs(v) >= 3 else pal.l if v < 0 else pal.m
        if 8 < u <= 9.5 and abs(v) <= 0.5:
            return shade(STEEL, 0.9)
        return None
    rdiag(img, fn, 2, 13)


def draw_hook(img, pal, item, rng):
    steel = Pal(STEEL)
    line(img, 9, 1, 9, 9, steel.l)
    line(img, 10, 1, 10, 9, steel.m)
    for (x, y) in mask(lambda d: d.arc((3, 6, 11, 14), 0, 200, fill=255, width=2)):
        put(img, x, y, steel.l if y < 11 else steel.m)
    put(img, 3, 8, steel.h)
    put(img, 4, 7, steel.h)
    for x in range(7, 13):
        put(img, x, 1, (190, 170, 120))


def draw_jug(img, pal, item, rng):
    iid = item.get("id", "")
    jug = Pal((180, 120, 80))
    body = mask(lambda d: (d.ellipse((2, 5, 12, 14), fill=255), d.rectangle((5, 2, 9, 7), fill=255)))
    bevel(img, body, jug)
    if iid != "jug":
        liq = (60, 110, 220) if "water" in iid else (110, 140, 60) if "bad" in iid else pal.m
        for (x, y) in mask(lambda d: d.ellipse((5, 2, 9, 4), fill=255)):
            put(img, x, y, liq)
    else:
        for x in range(6, 9):
            put(img, x, 3, (60, 40, 25))
    for (x, y) in mask(lambda d: d.arc((9, 4, 15, 11), 270, 90, fill=255, width=2)):
        if x >= 12:
            put(img, x, y, jug.d)
    for x in range(4, 11):
        put(img, x, 9, jug.d)


def draw_lamp(img, pal, item, rng):
    gold = Pal(GOLD if item.get("id", "").startswith("antique") else mix(pal.m, (190, 170, 120), 0.4))
    body = mask(lambda d: (d.ellipse((3, 7, 12, 13), fill=255), d.polygon([(11, 9), (15, 6), (15, 8), (12, 11)], fill=255)))
    bevel(img, body, gold)
    for (x, y) in mask(lambda d: d.arc((0, 7, 6, 12), 90, 270, fill=255)):
        put(img, x, y, gold.d)
    for (x, y) in mask(lambda d: d.ellipse((5, 5, 10, 8), fill=255)):
        put(img, x, y, gold.l)
    put(img, 7, 4, gold.h)
    for x in range(4, 12):
        put(img, x, 14, gold.d)


def lantern_shape(img, pal, lit):
    frame = Pal(mix(pal.m, (90, 85, 80), 0.5))
    glass = (255, 210, 90) if lit else (200, 215, 225)
    for y in range(5, 13):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (5, 12)
            put(img, x, y, frame.m if edge else glass, 255 if edge or lit else 150)
    for x in range(3, 13):
        put(img, x, 4, frame.l)
        put(img, x, 13, frame.d)
    for (x, y) in mask(lambda d: d.arc((5, 0, 10, 6), 180, 360, fill=255)):
        put(img, x, y, frame.d)
    if lit:
        for (x, y) in ((7, 8), (8, 8), (7, 9), (8, 9), (7, 10)):
            put(img, x, y, (255, 250, 210))
    else:
        line(img, 7, 9, 7, 11, (240, 240, 230))


def draw_lantern(img, pal, item, rng):
    lantern_shape(img, pal, False)


def draw_lantern_lit(img, pal, item, rng):
    lantern_shape(img, pal, True)


def draw_leaf(img, pal, item, rng):
    p = Pal(GOLD if "gold" in item.get("id", "") else pal.m)
    pix = mask(lambda d: d.polygon([(2, 3), (6, 2), (14, 3), (13, 9), (9, 13), (3, 12)], fill=255))
    for (x, y) in pix:
        put(img, x, y, p.h if (x * 3 + y * 5) % 11 == 0 else p.l if x < y + 2 else p.m)
    for x in range(4, 12):
        put(img, x, 14 - x + 2, p.d)


def draw_nails(img, pal, item, rng):
    for (x, y) in ((3, 3), (8, 2), (5, 8), (11, 7)):
        for dx in (-1, 0, 1):
            put(img, x + dx, y, pal.h if dx < 1 else pal.l)
        for k in range(1, 6):
            put(img, x, y + k, pal.l if k < 3 else pal.m)


def draw_nest(img, pal, item, rng):
    iid = item.get("id", "")
    twig = Pal((120, 85, 45))
    pix = mask(lambda d: d.chord((1, 3, 14, 14), 0, 180, fill=255))
    for (x, y) in pix:
        put(img, x, y, twig.l if (x + 2 * y) % 4 == 0 else twig.d if (x - y) % 3 == 0 else twig.m)
    for (x, y) in mask(lambda d: d.ellipse((1, 6, 14, 11), fill=255)):
        put(img, x, y, twig.m if (x * 3 + y) % 4 else twig.l)
    inner = mask(lambda d: d.ellipse((3, 7, 12, 10), fill=255))
    for (x, y) in inner:
        put(img, x, y, (70, 50, 30))
    if "ring" in iid:
        for (x, y) in mask(lambda d: d.ellipse((5, 4, 10, 9), outline=255)):
            put(img, x, y, GOLD)
    elif "seed" in iid:
        for (x, y) in ((5, 8), (7, 7), (9, 8), (8, 9), (6, 9)):
            put(img, x, y, (190, 160, 90))
    else:
        for cx in (5, 9):
            for (x, y) in mask(lambda d, c=cx: d.ellipse((c - 1, 5, c + 2, 9), fill=255)):
                put(img, x, y, (235, 230, 215) if y < 7 else (205, 200, 185))


def draw_nugget(img, pal, item, rng):
    g = Pal(GOLD if "gold" in item.get("id", "") else pal.m)
    blob_poly(img, [(4, 6), (7, 3), (11, 4), (13, 8), (10, 12), (5, 12), (3, 9)], g, noise=rng)
    put(img, 6, 5, WHITE)


def draw_page(img, pal, item, rng):
    paper = Pal((225, 210, 170))
    pix = {(x, y) for x in range(3, 13) for y in range(2, 15)} - {(12, 2), (12, 3), (11, 2)}
    bevel(img, pix, paper)
    put(img, 11, 3, paper.d)
    sym = Pal(pal.m)
    for (x, y) in ((7, 4), (8, 4), (6, 5), (9, 5), (7, 6), (8, 6)):
        put(img, x, y, sym.l)
    for y in range(8, 14, 2):
        for x in range(5, 11):
            if (x + y) % 4:
                put(img, x, y, (100, 80, 60))


def draw_pestle(img, pal, item, rng):
    stone = Pal((150, 145, 140))
    bowl = mask(lambda d: d.chord((2, 4, 13, 15), 0, 180, fill=255))
    bevel(img, bowl, stone)
    for (x, y) in mask(lambda d: d.ellipse((2, 7, 13, 11), fill=255)):
        put(img, x, y, stone.l if ring_px(x, y, mask(lambda d: d.ellipse((2, 7, 13, 11), fill=255))) else stone.dd)
    line(img, 8, 9, 12, 2, stone.l)
    line(img, 9, 9, 13, 2, stone.m)
    put(img, 12, 1, stone.h)


def draw_pipe(img, pal, item, rng):
    steel = Pal((120, 120, 128))

    def fn(u, v):
        if 0 <= u <= 2 and abs(v) <= 0.5:
            return (180, 140, 90)
        if 2 < u <= 12 and v == 0:
            return steel.l if u % 2 else steel.m
        if 12 < u <= 13.5 and abs(v) <= 1:
            return steel.d
        return None
    rdiag(img, fn)


def draw_plant_pot(img, pal, item, rng):
    clay = Pal(mix(pal.m, (170, 95, 55), 0.7) if "unfired" not in item.get("id", "") else (150, 130, 110))
    pix = mask(lambda d: d.polygon([(3, 5), (12, 5), (11, 14), (4, 14)], fill=255))
    bevel(img, pix, clay)
    for x in range(2, 14):
        put(img, x, 4, clay.l)
        put(img, x, 5, clay.d)
    for x in range(4, 12):
        put(img, x, 4, (70, 45, 30))


def draw_pot_lid(img, pal, item, rng):
    clay = Pal((150, 130, 110) if "unfired" in item.get("id", "") else (170, 95, 55))
    pix = mask(lambda d: d.ellipse((1, 6, 14, 12), fill=255))
    bevel(img, pix, clay)
    for (x, y) in mask(lambda d: d.ellipse((6, 4, 9, 7), fill=255)):
        put(img, x, y, clay.l)


def draw_rake(img, pal, item, rng):
    steel = Pal(STEEL)

    def fn(u, v):
        if 0 <= u <= 11 and v == 0:
            return WOOD if u % 2 else DARK_WOOD
        if 11 < u <= 11.5 and abs(v) <= 3.5:
            return steel.l
        if 11.5 < u <= 13 and abs(v) <= 3.5 and (v * 2) % 2 == 1:
            return steel.m
        return None
    rdiag(img, fn)


def draw_rope(img, pal, item, rng):
    c = Pal(mix(pal.m, (190, 160, 100), 0.6))
    for k, box in enumerate(((1, 3, 14, 14), (3, 5, 12, 12), (5, 7, 10, 10))):
        for (x, y) in mask(lambda d, b=box: d.ellipse(b, outline=255, width=2)):
            put(img, x, y, c.l if (x + y + k) % 3 else c.d)
    line(img, 12, 6, 14, 2, c.m)


def draw_salvage(img, pal, item, rng):
    wood = Pal(mix(pal.m, WOOD, 0.4))
    for (x0, y0, x1, y1) in ((2, 12, 12, 8), (3, 9, 13, 12), (5, 5, 11, 13)):
        line(img, x0, y0, x1, y1, wood.m)
        line(img, x0, y0 + 1, x1, y1 + 1, wood.d)
    for (x, y) in ((6, 9), (9, 10), (8, 7)):
        put(img, x, y, (110, 110, 118))
    for (x, y) in mask(lambda d: d.ellipse((9, 3, 13, 7), outline=255)):
        put(img, x, y, (150, 150, 155))


def draw_scales(img, pal, item, rng):
    for (cx, cy) in ((5, 5), (10, 5), (7, 9), (4, 11), (11, 11)):
        pix = mask(lambda d, c=(cx, cy): (d.chord((c[0] - 3, c[1] - 4, c[0] + 3, c[1] + 3), 0, 180, fill=255),
                                          d.rectangle((c[0] - 3, c[1] - 1, c[0] + 3, c[1]), fill=255)))
        bevel(img, pix, pal)


def draw_secateurs(img, pal, item, rng):
    steel = Pal(STEEL)
    line(img, 2, 14, 7, 8, pal.m)
    line(img, 3, 14, 7, 9, pal.d)
    line(img, 9, 14, 8, 8, pal.m)
    line(img, 10, 14, 8, 9, pal.d)
    for (x0, y0, x1, y1) in ((7, 8, 11, 2), (8, 8, 13, 4)):
        line(img, x0, y0, x1, y1, steel.l)
    put(img, 8, 8, GOLD)


def draw_spade(img, pal, item, rng):
    steel = Pal(STEEL)

    def fn(u, v):
        if 0 <= u <= 1 and abs(v) <= 1.5:
            return DARK_WOOD
        if 1 < u <= 9 and v == 0:
            return WOOD if u % 2 else DARK_WOOD
        if 9 < u <= 13.5 and abs(v) <= (2 if u < 12.5 else (13.5 - u) * 2):
            return steel.l if v < 0 else steel.m
        return None
    rdiag(img, fn)


def draw_spray(img, pal, item, rng):
    can = Pal(mix(pal.m, (90, 140, 80), 0.4))
    bevel(img, {(x, y) for x in range(4, 11) for y in range(6, 15)}, can)
    for x in range(4, 11):
        put(img, x, 9, (230, 230, 220))
        put(img, x, 10, (230, 230, 220))
    bevel(img, {(x, y) for x in range(6, 9) for y in range(3, 6)}, Pal((90, 90, 96)))
    line(img, 9, 3, 12, 3, (90, 90, 96))
    for (x, y) in ((13, 2), (14, 3), (13, 4)):
        put(img, x, y, (200, 230, 200), 200)


def draw_spyglass(img, pal, item, rng):
    brass = Pal(mix(pal.m, GOLD, 0.6))

    def fn(u, v):
        w = 0.5 if u < 4 else 1.0 if u < 9 else 1.5
        if 0.5 <= u <= 13 and abs(v) <= w:
            if u in (4, 9, 13):
                return brass.d
            return brass.h if v < 0 else brass.l if v == 0 else brass.m
        return None
    rdiag(img, fn)


def draw_trowel(img, pal, item, rng):
    steel = Pal(STEEL)

    def fn(u, v):
        if 0 <= u <= 5 and abs(v) <= 0.5:
            return WOOD if v < 0 else DARK_WOOD
        if 5 < u <= 6 and v == 0:
            return steel.d
        if 6 < u <= 13 and abs(v) <= min(2, (13 - u) * 0.6 + 0.5):
            return steel.h if v < 0 else steel.l if v == 0 else steel.m
        return None
    rdiag(img, fn)


def draw_statuette(img, pal, item, rng):
    p = Pal(mix(pal.m, GOLD, 0.35))
    bevel(img, {(x, y) for x in range(3, 13) for y in range(12, 15)}, Pal(shade(p.m, 0.7)))
    fig = mask(lambda d: (d.ellipse((6, 1, 9, 4), fill=255), d.polygon([(5, 5), (10, 5), (11, 11), (4, 11)], fill=255)))
    bevel(img, fig, p)
    put(img, 7, 2, p.dd)
    put(img, 8, 2, p.dd)


def draw_tablet(img, pal, item, rng):
    stone = Pal((170, 150, 120))
    pix = mask(lambda d: d.rounded_rectangle((2, 2, 13, 13), radius=2, fill=255))
    bevel(img, pix, stone, noise=rng)
    name = item.get("id", "")
    sym = rune_symbol("law" if "teleport" in name else name.split("_")[0])
    glow = Pal(pal.m if cdist(pal.m, stone.m) > 90 else (150, 70, 200))
    for j, row in enumerate(sym):
        for i, ch in enumerate(row):
            if ch == "#":
                put(img, 5 + i, 5 + j, stone.dd)
                put(img, 4 + i, 4 + j, glow.l if (i + j) < 5 else glow.m)


# improved looks for previously keyword-mapped shapes

def draw_noxious_blade(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 2.5 and abs(v) <= 0.5:
            return pal.d
        if 2.5 < u <= 13.5:
            off = -1.4 * math.sin(math.pi * (u - 2.5) / 11.5)
            vv = v - off
            w = 1.0 if u < 11 else max(0, (13.5 - u) * 0.5)
            if abs(vv) <= w:
                return pal.h if vv < 0 else pal.l if vv == 0 else pal.m
        return None
    rdiag(img, fn)


def draw_bludgeon(img, pal, item, rng):
    def fn(u, v):
        if 0 <= u <= 6 and v == 0:
            return pal.d if u % 2 else shade(pal.d, 0.7)
        if 6 < u <= 13 and abs(v) <= 1.5 + (u - 6) * 0.15:
            if (u * 2) % 3 == 0 and abs(v) > 1:
                return pal.h  # spines
            return pal.l if v < 0 else pal.m
        return None
    rdiag(img, fn)


def draw_buckler(img, pal, item, rng):
    pix = blob_ellipse(img, (2, 2, 13, 13), pal, noise=rng)
    for (x, y) in pix:
        if ring_px(x, y, pix):
            put(img, x, y, GOLD if (x + y) % 2 else shade(GOLD, 0.8))
    sphere(img, 7.5, 7.5, 1.6, Pal(GOLD))


def draw_chainbody(img, pal, item, rng):
    draw_platebody(img, pal, item, rng)
    for y in range(3, 15):
        for x in range(1, 15):
            if opaque(img, x, y) and (x + y) % 2 == 0:
                c = img.getpixel((x, y))
                put(img, x, y, shade(c, 0.78))


def draw_chakram(img, pal, item, rng):
    pix = mask(lambda d: d.ellipse((1, 1, 14, 14), fill=255)) - mask(lambda d: d.ellipse((5, 5, 10, 10), fill=255))
    for (x, y) in pix:
        a = math.atan2(y - 7.5, x - 7.5)
        put(img, x, y, pal.h if math.cos(a * 4) > 0.6 else pal.l if x + y < 15 else pal.m)
    for k in range(4):
        a = k * math.pi / 2 + 0.4
        put(img, int(round(7.5 + 7.6 * math.cos(a))), int(round(7.5 + 7.6 * math.sin(a))), pal.h)


def draw_crown(img, pal, item, rng):
    gold = Pal(mix(pal.m, GOLD, 0.5))
    pix = mask(lambda d: d.polygon([(2, 12), (2, 4), (5, 8), (8, 3), (11, 8), (14, 4), (14, 12)], fill=255))
    bevel(img, pix, gold)
    for x in range(2, 15):
        put(img, x, 12, gold.d)
        put(img, x, 11, gold.l)
    sphere(img, 8, 9, 1.5, Pal(pal.m))


def crystal_tint(pal):
    return Pal(mix(pal.m, (140, 220, 230), 0.5))


def draw_crystal_bow(img, pal, item, rng):
    bow(True, True)(img, crystal_tint(pal), item, rng)
    put(img, 8, 7, WHITE)
    put(img, 9, 6, (200, 255, 255))


def draw_crystal_sword(img, pal, item, rng):
    p = crystal_tint(pal)

    def fn(u, v):
        if 0 <= u <= 3 and abs(v) <= 0.5:
            return p.d
        if 3 < u <= 4 and abs(v) <= 2.5:
            return p.h
        if 4 < u <= 13.5:
            w = 1.5 if u < 11 else max(0, (13.5 - u) * 0.6)
            if abs(v) <= w:
                return p.h if v < -0.5 else (WHITE if v == 0 and u % 3 == 0 else p.l) if v <= 0 else p.m
        return None
    rdiag(img, fn)


def draw_med_helm(img, pal, item, rng):
    pix = mask(lambda d: (d.ellipse((2, 2, 13, 13), fill=255)))
    pix = {(x, y) for (x, y) in pix if y <= 11}
    bevel(img, pix, pal)
    for x in range(2, 14):
        if (x, 11) in pix:
            put(img, x, 11, pal.d)
    for y in range(7, 13):
        put(img, 7, y, pal.l)
        put(img, 8, y, pal.m)
    line(img, 5, 3, 4, 6, pal.h)


def draw_plateskirt(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(4, 2), (11, 2), (14, 14), (1, 14)], fill=255))
    bevel(img, pix, pal)
    for x in range(4, 12):
        put(img, x, 3, pal.dd)
    for y in (6, 10):
        for x in range(1, 15):
            if opaque(img, x, y):
                put(img, x, y, pal.d)
                if opaque(img, x, y + 1):
                    put(img, x, y + 1, pal.l)
    for y in range(4, 15):
        put(img, 7, y, pal.d)


def draw_sq_shield(img, pal, item, rng):
    pix = {(x, y) for x in range(2, 14) for y in range(2, 14)}
    bevel(img, pix, pal)
    for i in range(2, 14):
        put(img, i, i, pal.d)
        put(img, 15 - i, i, pal.d)
    put(img, 7, 7, pal.h)
    put(img, 8, 8, pal.h)


def draw_spirit_shield(img, pal, item, rng):
    pix = mask(lambda d: d.polygon([(2, 1), (13, 1), (13, 9), (8, 14), (7, 14), (2, 9)], fill=255))
    frame = Pal((170, 160, 130))
    bevel(img, pix, frame)
    inner = mask(lambda d: d.polygon([(4, 3), (11, 3), (11, 8), (8, 12), (7, 12), (4, 8)], fill=255))
    sp = Pal(mix(pal.m, (180, 200, 230), 0.3))
    for (x, y) in inner:
        put(img, x, y, sp.h if (x + y) % 5 == 0 else sp.l if y < 7 else sp.m)
    if "spirit_shield" != item.get("id"):
        for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7), (6, 7), (9, 7), (7, 8), (8, 8)):
            put(img, x, y, mix(pal.h, WHITE, 0.4))


def draw_sickle(img, pal, item, rng):
    line(img, 2, 14, 5, 11, WOOD)
    line(img, 3, 14, 6, 11, DARK_WOOD)
    blade = Pal(mix(pal.m, STEEL, 0.5))
    for (x, y) in mask(lambda d: d.arc((4, 1, 14, 13), 180, 20, fill=255, width=2)):
        if not (x < 7 and y > 9):
            put(img, x, y, blade.l if y < 6 else blade.m)
    put(img, 6, 10, GOLD)


def draw_salamander(img, pal, item, rng):
    body = mask(lambda d: (d.ellipse((4, 5, 12, 10), fill=255), d.ellipse((10, 4, 15, 9), fill=255)))
    bevel(img, body, pal)
    line(img, 4, 8, 1, 12, pal.d)
    line(img, 1, 12, 2, 14, pal.d)
    for (x0, y0, x1, y1) in ((6, 9, 5, 12), (10, 9, 11, 12), (6, 6, 5, 3), (10, 5, 10, 2)):
        line(img, x0, y0, x1, y1, pal.d)
    put(img, 13, 5, BLACK)
    for x in range(5, 11, 2):
        put(img, x, 7, pal.h)


def draw_throwing_knife(img, pal, item, rng):
    def fn(u, v):
        if 1 <= u <= 4 and v == 0:
            return pal.d
        if 4 < u <= 10 and abs(v) <= (0.5 if u < 9 else 0):
            return pal.h if v < 0 else pal.l if v == 0 else pal.m
        return None
    rdiag(img, fn, 3, 12)


def draw_godsword_blade(img, pal, item, rng):
    blade = Pal(mix(pal.m, mix(STEEL, WHITE, 0.2), 0.6))

    def fn(u, v):
        if 1 <= u <= 13.5:
            w = 1.5 if u < 11.5 else max(0, (13.5 - u) * 0.75)
            if u < 2 and abs(v) <= 0.5:
                return blade.d
            if abs(v) <= w:
                return metal_shade(v, blade, w)
        return None
    rdiag(img, fn)


def draw_bolt_tips(img, pal, item, rng):
    for (x, y) in ((3, 4), (9, 3), (5, 10), (11, 9)):
        sphere(img, x, y + 1, 1.3, pal, spec=False)
        put(img, x - 1, y, mix(pal.h, WHITE, 0.4))


def draw_dart_tip(img, pal, item, rng):
    for (x, y) in ((3, 3), (9, 2), (4, 9), (10, 8)):
        for k in range(4):
            put(img, x + k, y + 3 - k, pal.l if k > 1 else pal.m)
        put(img, x + 3, y + 1, pal.h)
        put(img, x + 2, y, pal.h)


def draw_javelin_heads(img, pal, item, rng):
    for (x, y) in ((2, 3), (8, 2), (4, 9), (10, 8)):
        for k in range(4):
            put(img, x + k, y + 3 - k, pal.l)
            put(img, x + k + 1, y + 3 - k, pal.m)
        put(img, x + 4, y - 1, pal.h)


def draw_pie_dish(img, pal, item, rng):
    clay = Pal((150, 130, 110) if "unfired" in item.get("id", "") else (175, 100, 60))
    pix = mask(lambda d: d.ellipse((1, 5, 14, 13), fill=255))
    bevel(img, pix, clay)
    for (x, y) in mask(lambda d: d.ellipse((3, 6, 12, 11), fill=255)):
        put(img, x, y, clay.d)


def draw_cake_tin(img, pal, item, rng):
    tin = Pal((165, 165, 172))
    pix = {(x, y) for x in range(2, 14) for y in range(7, 13)}
    bevel(img, pix, tin)
    for (x, y) in mask(lambda d: d.ellipse((2, 4, 13, 9), fill=255)):
        put(img, x, y, tin.l)
    for (x, y) in mask(lambda d: d.ellipse((4, 5, 11, 8), fill=255)):
        put(img, x, y, tin.dd)


def draw_tooth(img, pal, item, rng):
    p = Pal(mix(pal.m, (235, 228, 205), 0.5))
    pix = mask(lambda d: d.polygon([(4, 2), (12, 2), (12, 6), (10, 10), (8, 14), (7, 10), (5, 6)], fill=255))
    bevel(img, pix, p)
    line(img, 8, 3, 8, 6, p.d)


def draw_bone_spine(img, pal, item, rng):
    p = Pal(mix(pal.m, (230, 222, 200), 0.5))
    for k in range(6):
        cx, cy = 3 + k * 2, 12 - k * 2
        blob_ellipse(img, (cx - 1, cy - 1, cx + 2, cy + 1), p)
        put(img, cx - 1, cy - 2, p.l)
        put(img, cx + 2, cy + 1, p.d)


def draw_statue_item(img, pal, item, rng):
    if "totem" in item.get("id", ""):
        return draw_totem(img, pal, item, rng)
    if "trophy" in item.get("id", ""):
        bevel(img, {(x, y) for x in range(4, 12) for y in range(11, 15)}, Pal(DARK_WOOD))
        fish(img, Pal(pal.m))
        for y in range(10, 12):
            put(img, 7, y, GOLD)
        return None
    return draw_statuette(img, pal, item, rng)


SHAPES2 = {
    "statue": draw_statue_item,
    "bell": draw_bell, "birdhouse": draw_birdhouse, "boat": draw_boat, "bowl": draw_bowl, "brick": draw_brick,
    "cannonball": draw_cannonball, "clockwork": draw_clockwork, "crate": draw_crate_item, "cup": draw_cup,
    "dough": draw_dough, "emblem": draw_emblem, "fangs": draw_fangs, "head": draw_head, "hilt": draw_hilt,
    "jug": draw_jug, "kbd_heads": draw_kbd_heads, "lamp": draw_lamp, "lantern": draw_lantern,
    "lantern_lit": draw_lantern_lit, "leaf": draw_leaf, "nails": draw_nails, "nest": draw_nest,
    "nugget": draw_nugget, "page": draw_page, "plant_pot": draw_plant_pot, "pot_lid": draw_pot_lid,
    "rope": draw_rope, "salvage": draw_salvage, "scales": draw_scales, "statuette": draw_statuette,
    "tablet": draw_tablet, "hook": draw_hook, "pestle": draw_pestle, "pipe": draw_pipe, "rake": draw_rake,
    "secateurs": draw_secateurs, "spade": draw_spade, "spray": draw_spray, "spyglass": draw_spyglass,
    "trowel": draw_trowel,
    "blade": draw_noxious_blade, "bludgeon": draw_bludgeon, "buckler": draw_buckler, "chainbody": draw_chainbody,
    "chakram": draw_chakram, "crown": draw_crown, "crystal_bow": draw_crystal_bow,
    "crystal_sword": draw_crystal_sword, "med_helm": draw_med_helm, "plateskirt": draw_plateskirt,
    "round_shield": draw_shield_round, "sq_shield": draw_sq_shield, "spirit_shield": draw_spirit_shield,
    "sword": sword_like(10.5, 0.5, 3, 1.5, 2.5), "sickle": draw_sickle, "salamander": draw_salamander,
    "throwing_knife": draw_throwing_knife, "godsword_blade": draw_godsword_blade, "bolt_tips": draw_bolt_tips,
    "dart_tip": draw_dart_tip, "javelin_heads": draw_javelin_heads, "pie_dish": draw_pie_dish,
    "cake_tin": draw_cake_tin, "tooth": draw_tooth, "bone": draw_bone_spine,
}


EXTRA_SHAPES.update(SHAPES2)
for _k, _v in EXTRA_SHAPES.items():
    SHAPES.setdefault(_k, _v)


# Keyword heuristics for unknown shapes: checked in order against the tokens of the shape, then the
# id, then the name, so "dragon_crossbow" finds "crossbow" before "bow" and "armadyl_godsword" finds
# "godsword" before "sword". Multi-word keys match as substrings of the space-joined text.
SHAPE_KEYWORDS = [
    ("godsword", "godsword"), ("whip", "whip"), ("trident", "trident"), ("blowpipe", "blowpipe"),
    ("crossbow", "crossbow"), ("javelin", "javelin"), ("javelins", "javelin"), ("dart", "dart"),
    ("darts", "dart"), ("bolt", "bolt"), ("bolts", "bolt"), ("halberd", "halberd"), ("scythe", "scythe"),
    ("rapier", "rapier"), ("hasta", "hasta"), ("maul", "maul"), ("claws", "claws"), ("defender", "defender"),
    ("battleaxe", "battleaxe"), ("pickaxe", "pickaxe"), ("warhammer", "warhammer"), ("hammer", "warhammer"),
    ("2h", "2h_sword"), ("greatsword", "2h_sword"), ("longsword", "longsword"), ("scimitar", "scimitar"),
    ("sabre", "scimitar"), ("cutlass", "scimitar"), ("sword", "longsword"), ("blade", "longsword"),
    ("dagger", "dagger"), ("knife", "knife"), ("mace", "mace"), ("flail", "flail"), ("spear", "spear"),
    ("lance", "spear"), ("axe", "axe"), ("hatchet", "axe"), ("club", "club"), ("longbow", "longbow"),
    ("shortbow", "shortbow"), ("bow", "shortbow"), ("arrow", "arrow"), ("arrows", "arrow"), ("wand", "wand"),
    ("sceptre", "wand"), ("scepter", "wand"), ("staff", "staff_orb"), ("stave", "staff_orb"),
    ("shadow", "shadow"),
    ("full helm", "full_helm"), ("helm", "full_helm"), ("helmet", "full_helm"), ("coif", "coif"),
    ("hood", "hood"), ("cowl", "cowl"), ("mask", "mask"), ("tiara", "tiara"), ("crown", "tiara"),
    ("circlet", "tiara"), ("hat", "hood"), ("platebody", "platebody"), ("chainbody", "platebody"),
    ("chestplate", "platebody"), ("robe top", "robe_top"), ("top", "robe_top"), ("shirt", "robe_top"),
    ("torso", "platebody"), ("body", "platebody"), ("robe bottom", "robe_bottom"),
    ("robe bottoms", "robe_bottom"), ("skirt", "robe_bottom"), ("plateskirt", "robe_bottom"),
    ("robe", "robe_top"), ("platelegs", "platelegs"), ("tassets", "platelegs"), ("chaps", "chaps"),
    ("legs", "platelegs"), ("bottoms", "robe_bottom"), ("bulwark", "bulwark"), ("ward", "ward"),
    ("buckler", "shield_round"), ("kiteshield", "kiteshield"), ("sq shield", "kiteshield"),
    ("shield", "kiteshield"), ("boots", "boots"), ("gloves", "gloves"), ("gauntlets", "gloves"),
    ("vambraces", "vambraces"), ("bracers", "vambraces"), ("bracelet", "bracelet"), ("ring", "ring"),
    ("amulet", "amulet"), ("necklace", "necklace"), ("cape", "cape"), ("cloak", "cape"),
    ("potion", "potion"), ("brew", "potion"), ("flask", "potion"), ("vial", "vial"), ("jar", "jar"),
    ("bucket", "bucket"), ("seedling", "seedling"), ("sapling", "sapling"), ("seed", "seed"),
    ("seeds", "seed"), ("compost", "compost"), ("supercompost", "compost"), ("ultracompost", "compost"),
    ("pie", "pie"), ("pizza", "pizza"), ("stew", "stew"), ("curry", "stew"), ("soup", "stew"),
    ("wine", "wine"), ("bread", "bread"), ("cake", "cake"), ("beer", "beer"), ("ale", "beer"),
    ("rune", "rune"), ("runes", "rune"), ("talisman", "talisman"), ("pouch", "pouch"), ("key", "key"),
    ("prayer scroll", "prayer_scroll"), ("scroll", "scroll"), ("map", "map"), ("book", "book"),
    ("tome", "book"), ("journal", "book"), ("diary", "book"), ("orb", "orb"), ("shard", "shard"),
    ("shards", "shard"), ("fragment", "shard"), ("casket", "casket"), ("crystal", "crystal"),
    ("ingot", "ingot"), ("bar", "bar"), ("ore", "ore"), ("totem", "totem"), ("icon", "icon"),
    ("egg", "egg"), ("heart", "heart"), ("insignia", "insignia"), ("horn", "horn"), ("fang", "fang"),
    ("tooth", "fang"), ("visage", "visage"), ("tentacle", "tentacle"), ("core", "core"),
    ("sigil", "sigil"), ("claw", "claw"), ("tail", "tail"), ("teeth", "teeth"), ("ashes", "ash"),
    ("ash", "ash"), ("essence", "essence"), ("sack", "sack"), ("bones", "bones"), ("bone", "bones"),
    ("hide", "hide"), ("logs", "log"), ("log", "log"), ("plank", "plank"), ("gem", "gem"),
    ("net", "net"), ("glass", "glass"), ("pet", "pet"), ("skull", "skull"), ("note", "note"),
    ("letter", "note"), ("package", "package"), ("parcel", "package"), ("bead", "bead"),
    ("tool", "tool_generic"), ("chisel", "chisel"), ("needle", "needle"), ("tinderbox", "tinderbox"),
]

KIND_SHAPES = {
    "MELEE_WEAPON": "longsword", "ARMOUR": "platebody", "SHIELD": "kiteshield", "BOW": "shortbow",
    "STAFF": "staff_orb", "ARROW": "arrow", "FOOD": "meat_cooked", "POTION": "potion", "RUNE": "rune",
    "SEED": "seed", "TOOL": "tool_generic", "AXE": "axe", "PICKAXE": "pickaxe", "BONES": "bones",
    "GRIMY_HERB": "herb_grimy", "VIAL": "vial", "TRAP": "box_trap", "SLAYER_GEM": "gem", "COINS": "coins",
}


def _tokens(text):
    text = (text or "").lower().replace("(", " ").replace(")", " ")
    toks = [t for t in re.split(r"[^a-z0-9]+", text) if t]
    return set(toks), " ".join(toks)


def guess_shape(item):
    """Best existing drawer for an item whose shape has no drawer of its own."""
    for text in (item.get("shape"), item.get("id"), item.get("name")):
        toks, joined = _tokens(text)
        if not toks:
            continue
        for key, shape in SHAPE_KEYWORDS:
            if (" " in key and f" {key} " in f" {joined} ") or key in toks:
                return shape
    return KIND_SHAPES.get(item.get("kind"), "blob")


def resolve_drawer(item):
    shape = item.get("shape")
    if shape == "net" and "butterfly" in (item.get("id") or ""):
        return SHAPES["butterfly_net"]
    drawer = SHAPES.get(shape)
    if drawer is None:
        guessed = guess_shape(item)
        FALLBACK_SHAPES.add(f"{shape}->{guessed}")
        drawer = SHAPES.get(guessed, draw_blob)
    return drawer


def render_item(item):
    drawer = resolve_drawer(item)
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


def paint_plate(img, rect, pal, rng, leather=False, face="", style=None, cloth=False):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            lx, ly = x - x0, y - y0
            if cloth:
                c = pal.m if (lx + (ly // 3)) % 4 else pal.d  # soft vertical folds
                if rng.random() < 0.06:
                    c = pal.l
                if ly == h - 1 and h > 4:
                    c = pal.dd
            elif leather:
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


LEATHER_TIERS = ("leather", "dhide", "hide", "snakeskin", "prospector", "barbarian", "graceful", "fighter", "rogue",
                 "carapace", "hueycoatl", "blessed", "spined", "yak", "frog", "karil")
CLOTH_TIERS = ("null", "robe", "mystic", "infinity", "ancestral", "void", "tiara", "pyromancer", "wizard", "druid",
               "virtus", "ahrim", "elder", "zealot", "monk", "vestment", "farmer", "lumberjack", "angler", "shayzien")


def armour_style(tier):
    t = tier.lower()
    if any(k in t for k in CLOTH_TIERS):
        return "cloth"
    if any(k in t for k in LEATHER_TIERS):
        return "leather"
    return "metal"


def armour_layer(name, colour, layer, style=None):
    img = new_img(64, 32)
    pal = Pal(colour)
    rng = rng_for("armour", name, layer)
    style = style or ("leather" if name == "leather" else "metal")
    cloth = style == "cloth"
    leather = style == "leather"
    if layer == 1:
        for f, r in box_faces(0, 0, 8, 8, 8).items():
            paint_plate(img, r, pal, rng, leather, f, name, cloth=cloth)
        # visor on the helmet front (metal only), leave face open for leather (cowl)
        fx, fy = 8, 8
        if leather or cloth:
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
            paint_plate(img, r, pal, rng, leather, f, name, cloth=cloth)
        for f, r in box_faces(40, 16, 4, 12, 4).items():
            paint_plate(img, r, pal, rng, leather, f, name, cloth=cloth)
        for f, r in box_faces(0, 16, 4, 12, 4).items():  # boots
            paint_plate(img, r, Pal(shade(colour, 0.85)), rng, leather, f, name, cloth=cloth)
    else:
        for f, r in box_faces(16, 16, 8, 12, 4).items():
            paint_plate(img, r, pal, rng, leather, f, name, cloth=cloth)
        for f, r in box_faces(0, 16, 4, 12, 4).items():
            paint_plate(img, r, pal, rng, leather, f, name, cloth=cloth)
        # belt line on waist
        for x in range(16, 40):
            if opaque(img, x, 20):
                put(img, x, 20, pal.dd if not leather else DARK_WOOD)
    return img


def armour_tiers(cat):
    """{tier: colour} for every tier used by ARMOUR items (the Java material is "runecraft_" + tier, so a
    missing tier becomes "null"). Hand-tuned colours win; otherwise the tier's most common item colour."""
    by_tier = {}
    for it in cat["items"]:
        if it.get("kind") != "ARMOUR":
            continue
        cols = by_tier.setdefault(it.get("tier") or "null", Counter())  # None -> "null", like Java concatenation
        if it.get("color") is not None:
            cols[it["color"]] += 1
    return {t: ARMOUR_COLOURS.get(t, cols.most_common(1)[0][0] if cols else 0x808080) for t, cols in by_tier.items()}


def gen_armour(cat):
    base = os.path.join(RES, "assets", "minecraft", "textures", "models", "armor")
    tiers = dict(ARMOUR_COLOURS)
    tiers.update(armour_tiers(cat))
    for name, col in tiers.items():
        style = None if name in ARMOUR_COLOURS else armour_style(name)
        for layer in (1, 2):
            save_png(armour_layer(name, col, layer, style),
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
        hood = rgb(o["shirt"] if o.get("shirt") is not None else o["hair"])
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
    if o.get("belt"):
        t = rgb(o["belt"])
        for f in sides:
            x, y, w, h = body[f]
            for xx in range(x, x + w):
                put(img, xx, y + h - 3, t)
            if f == "front":
                put(img, x + w // 2 - 1, y + h - 3, light(t, 1.4))
                put(img, x + w // 2, y + h - 3, light(t, 1.4))
            for part in arms:
                ax, ay, aw, ah = part[f]
                for xx in range(ax, ax + aw):
                    put(img, xx, ay + ah - 2, t)
    if o.get("skeleton"):
        bone, gap = (225, 220, 200), (40, 34, 30)
        x, y, w, h = body["front"]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                rib = (yy - y) % 2 == 1 and yy - y < 8
                put(img, xx, yy, gap if (rib and xx not in (x + 3, x + 4)) else bone if not rib else (200, 195, 175))
        for part in arms + legs:
            for f in sides:
                px, py, pw, ph = part[f]
                for yy in range(py, py + ph):
                    for xx in range(px, px + pw):
                        put(img, xx, yy, bone if px + 1 <= xx <= px + 2 else gap)
        for (dx, dy) in ((1, 3), (2, 3), (1, 4), (2, 4), (5, 3), (6, 3), (5, 4), (6, 4), (3, 5), (4, 5)):
            put(img, fx + dx, fy + dy, gap)
        for dx in range(2, 6):
            put(img, fx + dx, fy + 6, gap if dx % 2 else bone)
    if o.get("crown"):
        c = rgb(o["crown"])
        for f in ("front", "back", "right", "left"):
            x, y, w, h = head[f]
            for xx in range(x, x + w):
                put(img, xx, y, c)
                put(img, xx, y + 1, shade(c, 0.75))
        x, y, w, h = head["front"]
        put(img, x + 3, y, (220, 40, 40))
        put(img, x + 4, y, (220, 40, 40))
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
# Custom entity model textures (ModelSpec UV nets, textures/entity/model/<id>.png)
# ----------------------------------------------------------------------------------------------
#
# Every box's net follows vanilla ModelPart.Cuboid (u, v = the box's packed UV origin):
#   top    (u+d,     v)    w x d      bottom (u+d+w,   v)    w x d
#   right  (u,       v+d)  d x h      front  (u+d,     v+d)  w x h
#   left   (u+d+w,   v+d)  d x h      back   (u+2d+w,  v+d)  w x h
# Cuboid builds DOWN from (u+d,v), UP from (u+d+w,v), WEST/NORTH/EAST/SOUTH along row v+d; entity
# models render Y-flipped, so the model's DOWN quad is the visible top and NORTH (-Z) is the face the
# creature looks out of (the same "front" as the player skin's face at (8, 8)). box_faces() encodes
# exactly this layout and is shared with the biped skin painter.

FACE_LIGHT = {"top": 1.22, "front": 1.0, "right": 0.88, "left": 0.88, "back": 0.80, "bottom": 0.62}
SIDE_FACES = ("front", "back", "right", "left")
LIMB_ROLES = {"LEG_FRONT_LEFT", "LEG_FRONT_RIGHT", "LEG_BACK_LEFT", "LEG_BACK_RIGHT", "LEG_EXTRA",
              "ARM_LEFT", "ARM_RIGHT"}


def light(c, f):
    """Scale brightness; values above 1 blend towards white so pale colours still get highlights."""
    if f <= 1:
        return shade(c, f)
    return mix(shade(c, f), WHITE, min(1.0, (f - 1.0) * 0.3))


def sat(c):
    r, g, b = rgb(c)
    mx, mn = max(r, g, b), min(r, g, b)
    return 0.0 if mx == 0 else (mx - mn) / mx


def cdist(a, b):
    a, b = rgb(a), rgb(b)
    return math.sqrt(sum((a[i] - b[i]) ** 2 for i in range(3)))


def hash01(*keys):
    return (zlib.crc32("|".join(map(str, keys)).encode()) & 0xFFFFFF) / float(0x1000000)


def face_xyz(face, i, j, w, h, d):
    """3D surface coordinate (X right, Y down, Z back; the front face is Z=0) of a net pixel."""
    if face == "front":
        return i, j, 0
    if face == "back":
        return w - 1 - i, j, d - 1
    if face == "right":
        return 0, j, d - 1 - i
    if face == "left":
        return w - 1, j, i
    if face == "top":
        return i, 0, d - 1 - j
    return i, h - 1, j  # bottom


def face_axes(face):
    """Which of X/Y/Z (0/1/2) vary across a face."""
    return {"front": (0, 1), "back": (0, 1), "right": (2, 1), "left": (2, 1), "top": (0, 2), "bottom": (0, 2)}[face]


def eye_colour(base, accent):
    a, b = rgb(accent), rgb(base)
    if sat(a) > 0.45 and lum(a) > 110 and cdist(a, b) > 90:
        return mix(a, WHITE, 0.15)
    if lum(b) > 150 or (b[0] > 150 and b[1] > 110 and b[2] < 110 and b[0] > b[2] + 60):
        return (230, 40, 30)
    return (255, 214, 60)


def pattern_colour(pattern, base, accent, face, i, j, fw, fh, xyz, dims, key):
    """Base colour of one net pixel after applying the part's surface pattern (before lighting)."""
    w, h, d = dims
    axis = max(range(3), key=lambda k: dims[k])
    in_plane = axis in face_axes(face)
    t = xyz[axis]
    if pattern == "scales":
        if fw < 3 or fh < 3:
            return base
        r = j // 2
        k = (i + 2 * (r % 2)) % 4
        edge = shade(mix(base, accent, 0.5), 0.72)
        if j % 2 == 1:
            return edge if k in (0, 3) else shade(base, 0.92)
        return light(base, 1.14) if k in (1, 2) else base
    if pattern == "plates":
        if face == "bottom":
            belly = mix(base, accent, 0.55)
            return shade(belly, 0.75) if t % 3 == 2 and in_plane else light(belly, 1.08)
        if dims[axis] < 4 or not in_plane:
            return base
        s = t % 4
        if s == 3:
            return shade(mix(base, accent, 0.45), 0.68)
        return light(base, 1.12) if s == 0 else base
    if pattern == "spots":
        if fw * fh < 6:
            return base
        cell = 4 if min(fw, fh) < 10 else 5
        cx, cy = i // cell, j // cell
        if hash01(key, face, cx, cy) < 0.55:
            sx = cx * cell + int(hash01(key, face, cx, cy, "x") * cell)
            sy = cy * cell + int(hash01(key, face, cx, cy, "y") * cell)
            big = min(fw, fh) >= 8 and hash01(key, face, cx, cy, "r") < 0.5
            dd = max(abs(i - sx), abs(j - sy))
            if dd == 0 or (big and abs(i - sx) + abs(j - sy) == 1):
                return accent if cdist(accent, base) > 40 else light(base, 1.3)
        return base
    if pattern == "stripes":
        if not in_plane or dims[axis] < 3:
            return base
        other = [a for a in face_axes(face) if a != axis][0]
        wob = int(round(math.sin(xyz[other] * 0.9 + hash01(key) * 6) * 0.8))
        period = 4 if dims[axis] < 14 else 5
        stripe_col = accent if cdist(accent, base) > 40 else shade(base, 0.55)
        return stripe_col if (t + wob) % period == 0 else base
    if pattern == "glow":
        ci = abs(2 * i / max(1, fw - 1) - 1) if fw > 1 else 0
        cj = abs(2 * j / max(1, fh - 1) - 1) if fh > 1 else 0
        centre = 1 - max(ci, cj)
        c = mix(base, WHITE, 0.12 + 0.38 * centre)
        if hash01(key, face, i, j) < 0.07:
            c = mix(mix(base, accent, 0.4), WHITE, 0.6)
        return c
    return base


def paint_eyes(img, fx, fy, fw, fh, base, accent):
    eye = eye_colour(base, accent)
    ew = 2 if fw >= 8 and fh >= 5 else 1
    eh = 2 if fw >= 10 and fh >= 8 else 1
    if fw == 1:
        xs = [0]
    else:
        lx = max(0, fw // 2 - ew - max(1, fw // 6))
        xs = [lx, fw - lx - ew]
    ey = int(fh * 0.35) if fh >= 3 else 0
    eye_pix = {(x + a, ey + b) for x in xs for a in range(ew) for b in range(eh)}
    if fw >= 6 and fh >= 4:  # soft glow halo
        for (x, y) in eye_pix:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    p = (x + dx, y + dy)
                    if p not in eye_pix and 0 <= p[0] < fw and 0 <= p[1] < fh:
                        cur = img.getpixel((fx + p[0], fy + p[1]))
                        put(img, fx + p[0], fy + p[1], mix(cur, eye, 0.22))
    if ey >= 1 and fh >= 5:  # brow ridge
        for x in xs:
            for a in range(-1, ew + 1):
                if 0 <= x + a < fw:
                    cur = img.getpixel((fx + x + a, fy + ey - 1))
                    put(img, fx + x + a, fy + ey - 1, shade(cur, 0.62))
    for k, x in enumerate(xs):
        for a in range(ew):
            for b in range(eh):
                inner = (a == ew - 1) if k == 0 else (a == 0)
                c = light(eye, 1.12) if b == 0 and not inner and ew > 1 else eye
                if eh > 1 and b == eh - 1 and inner:
                    c = shade(eye, 0.55)  # pupil slit towards the centre
                put(img, fx + x + a, fy + ey + b, c)
    if fh >= 4 and fw >= 3:
        my = min(fh - 1, ey + eh + max(1, (fh - ey - eh) // 2))
        x0, x1 = fw // 4, fw - fw // 4 - 1
        mouth = mix((35, 10, 10), base, 0.15)
        for x in range(x0, x1 + 1):
            put(img, fx + x, fy + my, mouth)
        if fw >= 6 and my + 1 < fh:
            put(img, fx + x0, fy + my + 1, (240, 236, 220))
            put(img, fx + x1, fy + my + 1, (240, 236, 220))


def paint_snout(img, fx, fy, fw, fh, base):
    dark = shade(base, 0.3)
    if fw >= 4 and fh >= 2:
        nx = max(0, fw // 2 - 1 - (1 if fw >= 6 else 0))
        put(img, fx + nx, fy, dark)
        put(img, fx + fw - 1 - nx, fy, dark)
    if fh >= 3 and fw >= 2:
        for x in range(fw):
            put(img, fx + x, fy + fh - 1, shade(base, 0.45))


def paint_model_box(img, spec_id, part, bi, box, eyes_here, snout_here):
    w, h, d, u, v = box["w"], box["h"], box["d"], box["u"], box["v"]
    base = rgb(part.get("color", 0x808080))
    if lum(base) < 35:
        base = mix(base, WHITE, 0.10)
    acc = part.get("accent")
    accent = rgb(acc) if acc is not None else shade(base, 0.75)
    pattern = (part.get("pattern") or "none").lower()
    role = part.get("role") or "STATIC"
    key = f"{spec_id}|{part.get('name')}|{bi}"
    rng = rng_for("model", key)
    dims = (w, h, d)
    faces = box_faces(u, v, w, h, d)
    for face, (fx, fy, fw, fh) in faces.items():
        if fw <= 0 or fh <= 0:
            continue
        for j in range(fh):
            for i in range(fw):
                xyz = face_xyz(face, i, j, w, h, d)
                c = pattern_colour(pattern, base, accent, face, i, j, fw, fh, xyz, dims, key)
                f = FACE_LIGHT[face]
                if face in SIDE_FACES and fh > 2:
                    f *= 1.07 - 0.14 * j / (fh - 1)
                if fw >= 3 and fh >= 3:
                    if i == 0 or j == 0:
                        f *= 1.06
                    elif i == fw - 1 or j == fh - 1:
                        f *= 0.86
                if pattern == "glow":
                    f = 1 + (f - 1) * 0.3
                f *= 1 + (rng.random() - 0.5) * (0.10 if fw * fh > 4 else 0.04)
                put(img, fx + i, fy + j, light(c, f))
        # role details
        if role in LIMB_ROLES and h >= 4 and pattern != "glow":
            tip = shade(mix(base, accent, 0.5), 0.55)
            if face in SIDE_FACES:
                for j in range(fh - (2 if h >= 8 else 1), fh):
                    for i in range(fw):
                        put(img, fx + i, fy + j, tip if (i + j) % 3 else shade(tip, 0.8))
            elif face == "bottom":
                for j in range(fh):
                    for i in range(fw):
                        put(img, fx + i, fy + j, shade(tip, 0.8))
        if role == "JAW":
            if face == "front" and fw >= 3:
                for i in range(fw):
                    put(img, fx + i, fy, (238, 232, 212) if i % 2 == 0 else (196, 188, 170))
            if face == "top":
                for j in range(fh):
                    for i in range(fw):
                        put(img, fx + i, fy + j, shade((150, 40, 40), 0.75 + rng.random() * 0.3))
        if role in ("WING_LEFT", "WING_RIGHT") and face in ("top", "bottom") and pattern != "glow":
            bone = shade(mix(base, accent, 0.5), 0.62 if face == "top" else 0.5)
            along_x = w >= d
            for j in range(fh):
                for i in range(fw):
                    X, _, Z = face_xyz(face, i, j, w, h, d)
                    if (along_x and (X % 5 == 2 or Z == 0)) or (not along_x and (Z % 5 == 2 or X == 0)):
                        put(img, fx + i, fy + j, bone)
        if role == "TENTACLE" and face == "bottom" and fw >= 2:
            for j in range(fh):
                for i in range(fw):
                    if (i + j) % 3 == 1:
                        put(img, fx + i, fy + j, light(mix(base, accent, 0.4), 1.25))
    if eyes_here:
        fx, fy, fw, fh = faces["front"]
        if fw > 0 and fh > 0:
            paint_eyes(img, fx, fy, fw, fh, base, accent)
    elif snout_here:
        fx, fy, fw, fh = faces["front"]
        if fw > 0 and fh > 0:
            paint_snout(img, fx, fy, fw, fh, base)


def model_texture(m):
    img = new_img(m["tex_w"], m["tex_h"])
    parts = m.get("parts", [])
    auto_eyes = not any((p.get("pattern") or "").lower() == "eyes" for p in parts)
    for p in parts:
        boxes = p.get("boxes", [])
        wants_eyes = (p.get("pattern") or "").lower() == "eyes" or (auto_eyes and p.get("role") == "HEAD")
        eye_box = None
        if wants_eyes and boxes:
            eye_box = max(range(len(boxes)), key=lambda k: (boxes[k]["w"] * boxes[k]["h"], -k))
        for bi, b in enumerate(boxes):
            paint_model_box(img, m["id"], p, bi, b, bi == eye_box, wants_eyes and bi != eye_box)
    return img


def gen_model_textures(cat):
    for m in cat.get("models", []):
        save_png(model_texture(m), os.path.join(ASSETS, "textures", "entity", "model", m["id"] + ".png"),
                 "model textures")


# ----------------------------------------------------------------------------------------------
# Catalog creatures (module monsters/bosses without a model) and module NPC skins
# ----------------------------------------------------------------------------------------------

DEFAULT_SKIN = [0xC8A080, 0x6A4A2A, 0x4A3A2A, 0x202020]


def has_word(text, *keys):
    toks, joined = _tokens(text)
    return any(k in toks or (len(k) > 4 and k in joined.replace(" ", "")) for k in keys)


def human_skin(c):
    r, g, b = rgb(c)
    return r >= g >= b and 120 <= r <= 245 and 25 <= r - b <= 130 and g - b < 70


def creature_options(eid, name, cols, npc=False):
    cols = list(cols or []) + DEFAULT_SKIN[len(cols or []):]
    skin, top, bottom, extra = (rgb(c) for c in cols[:4])
    text = f"{eid} {name}"
    o = dict(skin=skin, shirt=top, pants=bottom, shoes=shade(bottom, 0.6))
    if npc:
        o["hair"] = extra
    else:
        o["hair"] = shade(skin, 0.55) if lum(extra) > 90 else extra
        o["belt"] = extra
    if cdist(top, skin) < 28:
        o["shirt"] = None
    if cdist(bottom, skin) < 28:
        o["pants"] = None
        o["shoes"] = None
    if not human_skin(skin):
        o["eyes"] = eye_colour(skin, extra)
    # costume / anatomy hints from the name
    if has_word(text, "demon", "devil", "imp", "fiend", "abyssal", "zamorak", "tsutsaroth", "infernal"):
        o["horns"] = True
        o.setdefault("eyes", (255, 210, 50))
    if has_word(text, "goblin", "hobgoblin", "orc", "ork", "gnome", "elf", "elven"):
        o["ears"] = True
    if has_word(text, "zombie", "ghoul", "undead", "revenant", "ghast", "rotting", "mummy"):
        o["rotten"] = True
        o["ragged"] = True
    if has_word(text, "skeleton", "skeletal", "bones", "lich", "skull"):
        o["skeleton"] = True
        o.setdefault("eyes", (120, 220, 255))
    if has_word(text, "wizard", "mage", "druid", "shaman", "necromancer", "sorcerer", "sorceress", "warlock",
                "cultist", "monk", "priest", "acolyte", "witch", "magus", "archmage"):
        o["robe"] = True
        if has_word(text, "necromancer", "cultist", "acolyte", "dark", "druid") and not npc:
            o["hood"] = o["shirt"] is not None
    if has_word(text, "knight", "guard", "warrior", "soldier", "paladin", "templar", "champion", "sentinel",
                "warden", "sir", "captain", "squire", "commander"):
        if npc:
            tab = (235, 235, 240) if lum(top) < 140 else (40, 70, 170)
            vivid = sat(extra) > 0.4 and lum(extra) > 80 and cdist(extra, tab) > 80
            o.update(chain=True, helmet=True, tabard=tab, emblem=extra if vivid else GOLD)
        else:
            o.update(plate=True, helmet=True, helmet_col=shade(top, 1.0), trim=extra)
    if has_word(text, "giant", "cyclops", "ogre", "troll", "golem") and o.get("shirt") is None:
        o["loincloth"] = o.get("pants") is not None
        if o.get("pants") is None:
            o["pants"] = shade(skin, 0.8)
            o["loincloth"] = True
    if has_word(text, "ghost", "spirit", "shade", "wraith", "phantom", "banshee", "spectre", "specter"):
        o["robe"] = True
        o["alpha"] = 190
    if has_word(text, "dwarf", "dwarven"):
        o["big_beard"] = o["hair"]
    if has_word(text, "barbarian", "fremennik", "viking", "berserker"):
        o["fur"] = True
        o["beard"] = o["hair"]
    if has_word(text, "tzhaar", "jal", "tzkal", "tzkih", "tzek", "ketzek", "yt", "mejkot", "obsidian", "lava",
                "magma", "molten"):
        o["molten"] = True
    if has_word(text, "moss", "bryophyta", "swamp", "treant", "ent", "leaf", "vine"):
        o["moss"] = True
    if has_word(text, "vampyre", "vampire", "vyrewatch", "vyre", "count"):
        o.update(cape=shade(top, 0.6), collar=(170, 20, 25), long_sleeves=True)
        o.setdefault("eyes", (220, 30, 30))
    if npc:
        if has_word(text, "king", "queen", "prince", "princess", "duke", "duchess", "emperor", "empress", "lord",
                    "baron", "chieftain", "monarch"):
            o.update(crown=GOLD, cape=shade(top, 0.7), collar=GOLD)
        if has_word(text, "cook", "chef", "baker"):
            o.update(apron=True, hat=("chef", (250, 250, 250)))
        if has_word(text, "wizard", "mage", "sorcerer", "witch", "archmage"):
            o.update(hat=("pointy", extra), hair=(200, 200, 205))
            if not has_word(text, "witch", "sorceress"):
                o["beard"] = (215, 215, 220)
        if has_word(text, "druid"):
            o.update(hood=True, beard=(225, 225, 225), trim=(60, 150, 50))
        if has_word(text, "priest", "monk", "brother", "father", "abbot", "saradomin"):
            o["robe"] = True
            o.setdefault("trim", (240, 240, 240) if lum(top) < 100 else GOLD)
        if has_word(text, "lady", "madam", "dame", "duchess", "queen", "princess"):
            o["robe"] = True
    return o


def gen_catalog_creatures(cat):
    """Biped skins for module monsters/bosses that have no ModelSpec, plus module NPC skins."""
    names = {}
    specs = {m["id"] for m in cat.get("models", [])}
    for group in ("monsters", "bosses"):
        for e in cat.get(group, []):
            eid = e["id"]
            names[eid] = e.get("name") or eid
            spec = e.get("model") or eid
            if spec in specs or eid in ENTITIES:
                continue  # custom model, or a hand-tuned legacy skin
            o = creature_options(eid, e.get("name", eid), e.get("skin"))
            save_png(entity_texture(eid, o), os.path.join(ASSETS, "textures", "entity", eid + ".png"),
                     "creature skins")
    for n in cat.get("npcs", []):
        o = creature_options(n["id"], n.get("name", n["id"]), n.get("colors"), npc=True)
        save_png(entity_texture("npc|" + n["id"], o), os.path.join(ASSETS, "textures", "entity", "npc", n["id"] + ".png"),
                 "npc skins")
    return names


# ----------------------------------------------------------------------------------------------
# Module blocks (catalog["blocks"]) - textures from colour + look hint
# ----------------------------------------------------------------------------------------------

LOOK_KEYWORDS = [
    ("portal", "portal"), ("rift", "portal"), ("gateway", "portal"), ("teleport", "portal"), ("vortex", "portal"),
    ("altar", "altar"), ("shrine", "altar"), ("pedestal", "altar"), ("chest", "chest"), ("coffer", "chest"),
    ("trunk", "chest"), ("crate", "crate"), ("box", "crate"), ("statue", "statue"), ("idol", "statue"),
    ("range", "range"), ("stove", "range"), ("oven", "range"), ("machine", "machine"), ("lever", "machine"),
    ("console", "machine"), ("device", "machine"), ("engine", "machine"), ("pump", "machine"),
    ("winch", "machine"), ("obelisk", "obelisk"), ("monolith", "obelisk"), ("totem", "obelisk"),
    ("anvil", "anvil"), ("furnace", "furnace"), ("forge", "furnace"), ("kiln", "furnace"), ("smelter", "furnace"),
    ("brazier", "brazier"), ("fire", "brazier"), ("torch", "brazier"), ("pyre", "pyre"), ("flame", "brazier"),
    ("booth", "booth"), ("bank", "booth"), ("counter", "booth"), ("table", "table"), ("desk", "table"),
    ("bench", "table"), ("lectern", "table"), ("workbench", "table"), ("door", "door"), ("gate", "door"),
    ("hatch", "door"), ("rock", "rock"), ("boulder", "rock"), ("ore", "rock"), ("crystal", "crystal"),
    ("gem", "crystal"), ("plant", "plant"), ("bush", "plant"), ("tree", "plant"), ("vine", "plant"),
    ("flower", "plant"), ("hedge", "plant"), ("fungus", "plant"), ("mushroom", "plant"),
    ("bookcase", "bookcase"), ("bookshelf", "bookcase"), ("shelf", "bookcase"), ("barrel", "barrel"),
    ("coffin", "coffin"), ("sarcophagus", "coffin"), ("tomb", "coffin"), ("cauldron", "cauldron"),
    ("pillar", "pillar"), ("column", "pillar"), ("grave", "grave"), ("tombstone", "grave"),
    ("headstone", "grave"), ("lava", "lava"), ("ice", "ice"), ("water", "water"), ("pool", "water"),
]


def look_kind(look):
    toks, joined = _tokens((look or "").replace("_", " "))
    for key, kind in LOOK_KEYWORDS:
        if key in toks:
            return kind
    for key, kind in LOOK_KEYWORDS:  # compound words like "portalglow"
        if len(key) >= 4 and key in joined.replace(" ", ""):
            return kind
    return "panel"


def border(img, col, inset=0, col2=None):
    for i in range(inset, 16 - inset):
        for (x, y) in ((i, inset), (inset, i)):
            put(img, x, y, col)
        for (x, y) in ((i, 15 - inset), (15 - inset, i)):
            put(img, x, y, col2 if col2 is not None else col)


def bricks_tex(col, rng, mortar=None):
    img = new_img()
    p = Pal(col)
    mortar = mortar or shade(col, 0.55)
    for y in range(16):
        for x in range(16):
            off = 4 if (y // 4) % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7:
                put(img, x, y, mortar)
            else:
                put(img, x, y, shade(p.m, 0.9 + rng.random() * 0.2) if y % 4 else p.l)
    return img


def metal_tex(col, rng):
    img = new_img()
    noise_fill(img, col, rng, 0.05)
    border(img, light(col, 1.2), 0, shade(col, 0.6))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(img, x, y, light(col, 1.35))
        put(img, x + 1, y + 1, shade(col, 0.55))
    return img


def gear(img, cx, cy, r, col):
    for (x, y) in mask(lambda d: d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=255)):
        put(img, x, y, col)
    for a in range(0, 360, 45):
        x = int(round(cx + (r + 1) * math.cos(math.radians(a))))
        y = int(round(cy + (r + 1) * math.sin(math.radians(a))))
        put(img, x, y, col)
    put(img, cx, cy, light(col, 1.3))


def glow_dots(img, col, rng, n=6):
    for _ in range(n):
        put(img, rng.randrange(1, 15), rng.randrange(1, 15), mix(col, WHITE, 0.6))


def portal_swirl(rng, col, frame=None):
    img = new_img()
    dark, bright = shade(col, 0.25), mix(col, WHITE, 0.35)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r, a = math.hypot(dx, dy), math.atan2(dy, dx)
            t = (math.sin(3 * a + r * 1.1) * 0.5 + 0.5) * 0.8 + rng.random() * 0.2
            c = mix(dark, bright, t)
            if r < 2.2:
                c = mix(c, WHITE, 0.55)
            put(img, x, y, c)
    glow_dots(img, col, rng, 5)
    if frame:
        border(img, frame, 0, shade(frame, 0.7))
    return img


# Looks introduced by the merged modules. Checked before LOOK_KEYWORDS (exact look, then fire_* prefix).
LOOK_EXACT = {
    "farming_patch": "soil", "soil": "soil", "stall": "stall", "fishing_spot": "water_spot", "water_spot": "water_spot",
    "pool": "pool", "water_container": "tank", "notice_board": "board", "shipwreck": "wreck", "chair": "chair",
    "bed": "bed", "tree": "tree", "young_tree": "young_tree", "palm": "palm", "meat_tree": "meat_tree",
    "sapling": "sapling", "roots": "roots", "stump": "stump", "pit": "pit", "larder": "larder", "burner": "burner",
    "pot": "urn", "churn": "churn", "wheel": "wheel", "birdhouse": "birdhouse", "mushroom": "mushroom",
    "buoy": "buoy", "rope": "rope", "pressure_plate": "plate", "vent": "vent", "totem": "totem",
    "portal_door": "portal_door", "boulder": "rock", "pyre": "pyre", "crucible": "crucible",
}
FIRE_LOOKS = {"brazier": "brazier", "shrine": "fire_shrine", "pyre": "pyre", "crucible": "crucible", "lava": "lava"}
# Block names that pin down a better texture than a generic look (e.g. a "machine" that is really a GE booth).
NAME_KINDS = [
    ("grand exchange", "booth"), ("booth", "booth"), ("board", "board"), ("fountain", "fountain"),
    ("font", "fountain"), ("sarcophagus", "coffin"), ("flag", "banner"), ("skillcape", "cape_stand"),
    ("door", "door"), ("compost", "barrel"), ("bin", "barrel"), ("sack", "sack"), ("pile", "pile"),
    ("trough", "tank"), ("workbench", "table"), ("table", "table"), ("grindstone", "wheel"), ("wheel", "wheel"), ("vat", "cauldron"),
    ("hopper", "machine"), ("support", "pillar"), ("shed", "door"), ("stairs", "stairs"), ("barrier", "portal"),
]
NAME_REFINABLE = {"crate", "machine", "statue", "altar", "chest", "portal", "portal_door", "rock", "panel"}


def block_kind(b):
    look = (b.get("look") or "").lower()
    if look in LOOK_EXACT:
        kind = LOOK_EXACT[look]
    elif look.startswith("fire_"):
        kind = FIRE_LOOKS.get(look[5:], "brazier")
    else:
        kind = look_kind(look)
    if kind in NAME_REFINABLE:
        toks, joined = _tokens(b.get("name") or "")
        for key, k2 in NAME_KINDS:
            if (" " in key and key in joined) or key in toks:
                return k2
    return kind


def leaves_tex(rng, col, fruit=None):
    img = new_img()
    pl = Pal(col)
    noise_fill(img, pl.d, rng, 0.12)
    for _ in range(30):
        cx, cy = rng.randrange(16), rng.randrange(16)
        for (x, y, c) in ((cx, cy, pl.l), (cx + 1, cy, pl.m), (cx, cy + 1, pl.m), (cx + 1, cy + 1, pl.dd)):
            put(img, x % 16, y % 16, c)
    if fruit:
        for _ in range(6):
            x, y = rng.randrange(15), rng.randrange(15)
            put(img, x, y, fruit)
            put(img, x + 1, y + 1, shade(fruit, 0.7))
    return img


def water_tex(rng, col, ripples=True):
    img = new_img()
    base = mix((40, 90, 190), col, 0.35)
    for y in range(16):
        for x in range(16):
            n = math.sin(x * 0.7 + y * 0.4) + math.sin(y * 0.9 - x * 0.5) + rng.random() * 0.5
            put(img, x, y, light(base, 1.25) if n > 1.2 else base if n > -0.3 else shade(base, 0.8))
    if ripples:
        img.alpha_composite(ripple_tex(rng))
    return img


def soil_tex(rng, col=(70, 45, 25)):
    img = new_img()
    for y in range(16):
        for x in range(16):
            c = col if y % 4 not in (0, 3) else shade(col, 0.72)
            put(img, x, y, shade(c, 0.85 + rng.random() * 0.3))
    return img


def extra_block_textures(kind, b, col, rng):
    """Textures for the kinds in LOOK_EXACT / NAME_KINDS; returns None for kinds handled elsewhere."""
    p = Pal(col)
    grey = mix((120, 118, 122), col, 0.15)
    dark_stone = stone_tex(rng, shade(grey, 0.6), 0.08)
    wood = mix(col, WOOD, 0.5)
    pw = Pal(wood)
    if kind == "soil":
        top = soil_tex(rng)
        border(top, (110, 80, 50))
        for _ in range(5):
            x, y = rng.randrange(2, 14), rng.randrange(2, 14)
            put(top, x, y, (70, 140, 50))
        side = new_img()
        noise_fill(side, (120, 85, 55), rng, 0.15)
        for x in range(16):
            put(side, x, 0, (60, 110, 40))
            put(side, x, 1, (60, 110, 40) if rng.random() < 0.6 else (90, 70, 40))
            put(side, x, 2, (100, 75, 45))
        return side, top, soil_tex(rng, (90, 65, 40))
    if kind == "stall":
        awn = (col, (235, 230, 220))
        return counter_side(rng, WOOD, awn), stall_goods("cake" if lum(col) > 150 else "spice", rng), \
            planks_tex(DARK_WOOD, rng)
    if kind == "water_spot":
        img = water_tex(rng, col)
        return img, img, img
    if kind == "pool":
        top = water_tex(rng, col, ripples=False)
        rim = light(grey, 1.2)
        border(top, rim, 0, shade(rim, 0.8))
        border(top, shade(rim, 0.85), 1)
        glow_dots(top, col, rng, 4)
        side = stone_tex(rng, rim, 0.05)
        for x in range(16):
            put(side, x, 0, light(rim, 1.15))
            put(side, x, 3, shade(rim, 0.75))
        return side, top, dark_stone
    if kind == "tank":
        side = planks_tex(wood, rng)
        for x in range(16):
            put(side, x, 1, (90, 90, 96))
            put(side, x, 14, (90, 90, 96))
        top = water_tex(rng, (60, 110, 220), ripples=False)
        border(top, pw.d)
        return side, top, planks_tex(shade(wood, 0.7), rng)
    if kind == "board":
        side = planks_tex(wood, rng)
        border(side, pw.dd)
        for (x0, y0, w, h, c) in ((2, 2, 5, 6, (235, 228, 205)), (8, 3, 6, 5, (225, 215, 180)),
                                  (3, 9, 6, 5, (230, 220, 195)), (10, 9, 4, 5, (240, 235, 220))):
            for y in range(y0, y0 + h):
                for x in range(x0, x0 + w):
                    put(side, x, y, c if (y - y0) % 2 == 0 or x in (x0, x0 + w - 1) else shade(c, 0.75))
            put(side, x0 + w // 2, y0, (190, 30, 30))
        top = planks_tex(wood, rng)
        return side, top, top
    if kind == "wreck":
        wet = mix(wood, (50, 60, 55), 0.45)
        side = planks_tex(wet, rng)
        for _ in range(14):
            x, y = rng.randrange(16), rng.randrange(16)
            put(side, x, y, (200, 200, 190) if rng.random() < 0.5 else (60, 110, 70))
        for y in range(12, 16):
            for x in range(16):
                if rng.random() < 0.5:
                    put(side, x, y, (50, 100, 150))
        top = planks_tex(wet, rng)
        for x in range(3, 13):
            put(top, x, 7, (90, 90, 96))
        return side, top, water_tex(rng, (40, 80, 140), False)
    if kind == "chair":
        side = new_img()
        noise_fill(side, (35, 28, 22), rng, 0.08)
        for y in range(16):
            for x in (1, 2, 13, 14):
                put(side, x, y, pw.m if x in (1, 13) else pw.d)
        for x in range(1, 15):
            for y in (8, 9):
                put(side, x, y, pw.l if y == 8 else pw.d)
            for y in (1, 2):
                put(side, x, y, pw.m)
        top = planks_tex(wood, rng)
        border(top, pw.dd)
        return side, top, top
    if kind == "bed":
        sheet = mix((170, 40, 40), col, 0.3)
        side = planks_tex(wood, rng)
        for y in range(0, 7):
            for x in range(16):
                put(side, x, y, shade(sheet, 0.9 + rng.random() * 0.15) if y > 0 else light(sheet, 1.2))
        top = new_img()
        noise_fill(top, sheet, rng, 0.06)
        for y in range(1, 6):
            for x in range(2, 14):
                put(top, x, y, (235, 235, 230))
        for x in range(16):
            put(top, x, 7, light(sheet, 1.25))
        border(top, pw.d)
        return side, top, planks_tex(shade(wood, 0.7), rng)
    if kind in ("tree", "young_tree", "palm", "meat_tree"):
        bark = mix(col, (95, 70, 45), 0.6) if kind != "meat_tree" else (140, 70, 60)
        side = bark_tex(bark, rng)
        if kind == "palm":
            for y in range(0, 16, 3):
                for x in range(16):
                    put(side, x, y, shade(bark, 0.6))
        if kind == "meat_tree":
            for _ in range(5):
                x, y = rng.randrange(1, 14), rng.randrange(1, 14)
                for (dx, dy, c) in ((0, 0, (200, 80, 80)), (1, 0, (170, 60, 60)), (0, 1, (230, 220, 200))):
                    put(side, x + dx, y + dy, c)
        leaf = (70, 140, 50) if kind != "palm" else (90, 160, 60)
        if kind == "young_tree":
            img = leaves_tex(rng, leaf)
            for y in range(16):
                put(img, 7, y, bark)
                put(img, 8, y, shade(bark, 0.8))
            return img, leaves_tex(rng, leaf), soil_tex(rng)
        fruit = (200, 80, 80) if kind == "meat_tree" else (230, 200, 80) if kind == "palm" else None
        return side, leaves_tex(rng, leaf, fruit), rings_tex(bark, rng)
    if kind == "sapling":
        side = new_img()
        noise_fill(side, (110, 75, 45), rng, 0.15)
        top = soil_tex(rng)
        leaf = Pal((80, 160, 60))
        line(top, 8, 12, 8, 6, DARK_WOOD)
        for (x, y) in ((6, 5), (7, 4), (9, 4), (10, 5), (7, 7), (9, 7), (6, 8), (10, 8), (8, 3)):
            put(top, x, y, leaf.l if y < 6 else leaf.m)
        return side, top, soil_tex(rng, (90, 65, 40))
    if kind == "roots":
        img = soil_tex(rng, mix((80, 55, 35), col, 0.25))
        root = mix((150, 110, 70), col, 0.3)
        for _ in range(5):
            x, y = rng.randrange(16), rng.randrange(16)
            for k in range(8):
                put(img, x % 16, y % 16, root if k % 3 else light(root, 1.2))
                x += rng.choice((-1, 0, 1))
                y += 1
        return img, img, soil_tex(rng)
    if kind == "stump":
        bark = mix(col, (95, 70, 45), 0.6)
        return stump_side_tex(bark, rng), rings_tex(bark, rng, pale=True), soil_tex(rng)
    if kind == "pit":
        top = new_img()
        for y in range(16):
            for x in range(16):
                r = max(abs(x - 7.5), abs(y - 7.5))
                put(top, x, y, (20, 15, 10) if r < 4.5 else shade((110, 80, 50), 0.6 + (r - 4.5) * 0.12))
        for x in range(3, 13, 2):
            line(top, x, 3, x + 1, 12, (150, 120, 60))  # covering sticks
        side = new_img()
        noise_fill(side, (110, 80, 50), rng, 0.15)
        for x in range(16):
            put(side, x, 0, (70, 120, 45))
        return side, top, soil_tex(rng)
    if kind == "larder":
        side = planks_tex(wood, rng)
        for x in (7, 8):
            for y in range(1, 15):
                put(side, x, y, pw.dd)
        border(side, pw.dd)
        put(side, 6, 8, GOLD)
        put(side, 9, 8, GOLD)
        top = planks_tex(wood, rng)
        return side, top, top
    if kind == "burner":
        side = planks_tex(wood, rng)
        for x in range(16):
            put(side, x, 0, (90, 90, 96))
        top = stone_tex(rng, (70, 68, 72), 0.06)
        for (x, y) in mask(lambda d: d.ellipse((3, 3, 12, 12), fill=255)):
            put(top, x, y, mix((40, 30, 25), (240, 120, 40), rng.random() * 0.8))
        border(top, pw.d)
        return side, top, planks_tex(shade(wood, 0.7), rng)
    if kind == "urn":
        clay = mix(col, (160, 100, 60), 0.5)
        side = new_img()
        noise_fill(side, clay, rng, 0.06)
        for y in (2, 13):
            for x in range(16):
                put(side, x, y, shade(clay, 0.6))
        for x in range(0, 16, 4):
            put(side, x + 1, 7, light(clay, 1.25))
            put(side, x + 2, 8, light(clay, 1.25))
        top = new_img()
        noise_fill(top, clay, rng, 0.06)
        for (x, y) in mask(lambda d: d.ellipse((3, 3, 12, 12), fill=255)):
            put(top, x, y, (30, 22, 16))
        return side, top, top
    if kind == "churn":
        side = planks_tex(wood, rng)
        for y in range(16):
            for x in range(0, 16, 4):
                put(side, x, y, shade(wood, 0.55))
        for y in (3, 12):
            for x in range(16):
                put(side, x, y, (80, 80, 86))
        top = planks_tex(wood, rng)
        for (x, y) in mask(lambda d: d.ellipse((2, 2, 13, 13), fill=255)):
            put(top, x, y, (240, 238, 225))
        put(top, 7, 7, DARK_WOOD)
        put(top, 8, 8, DARK_WOOD)
        return side, top, planks_tex(shade(wood, 0.7), rng)
    if kind == "wheel":
        side = planks_tex(wood, rng)
        for (x, y) in mask(lambda d: d.ellipse((1, 1, 14, 14), outline=255)):
            put(side, x, y, pw.dd)
        for a in range(0, 360, 45):
            line(side, 8, 8, 8 + int(round(6 * math.cos(math.radians(a)))),
                 8 + int(round(6 * math.sin(math.radians(a)))), pw.dd)
        top = stone_tex(rng, mix(col, (150, 140, 130), 0.5), 0.06)
        for (x, y) in mask(lambda d: d.ellipse((2, 2, 13, 13), outline=255)):
            put(top, x, y, shade(col, 0.6))
        return side, top, planks_tex(shade(wood, 0.7), rng)
    if kind == "birdhouse":
        side = planks_tex(wood, rng)
        for (x, y) in mask(lambda d: d.ellipse((5, 5, 10, 10), fill=255)):
            put(side, x, y, (25, 18, 12))
        line(side, 6, 12, 9, 12, pw.dd)
        top = planks_tex(shade(wood, 0.75), rng)
        for x in range(16):
            put(top, x, 7, pw.dd)
            put(top, x, 8, pw.dd)
        return side, top, planks_tex(shade(wood, 0.6), rng)
    if kind == "mushroom":
        cap = col if sat(col) > 0.25 else (180, 60, 50)
        side = new_img()
        noise_fill(side, (225, 215, 195), rng, 0.05)
        for y in range(0, 6):
            for x in range(16):
                put(side, x, y, shade(cap, 0.95 + rng.random() * 0.1))
        for x in range(16):
            put(side, x, 6, shade(cap, 0.6))
        top = new_img()
        noise_fill(top, cap, rng, 0.06)
        for _ in range(7):
            x, y = rng.randrange(1, 14), rng.randrange(1, 14)
            for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
                put(top, x + dx, y + dy, (240, 235, 220))
        return side, top, side
    if kind == "buoy":
        stripe = col if sat(col) > 0.3 else (210, 40, 40)
        side = new_img()
        for y in range(16):
            for x in range(16):
                c = stripe if (y // 4) % 2 == 0 else (235, 235, 230)
                put(side, x, y, shade(c, 0.9 + rng.random() * 0.1))
        top = new_img()
        noise_fill(top, stripe, rng, 0.05)
        sphere(top, 7.5, 7.5, 3, Pal((255, 230, 120)))
        return side, top, water_tex(rng, (40, 80, 140), False)
    if kind == "rope":
        img = new_img()
        rc = mix(col, (190, 160, 100), 0.6)
        for y in range(16):
            for x in range(16):
                put(img, x, y, light(rc, 1.15) if (x + y) % 4 < 2 else shade(rc, 0.75))
        return img, img, img
    if kind == "plate":
        mat = mix(col, (190, 170, 120), 0.4)
        top = stone_tex(rng, mat, 0.05)
        border(top, shade(mat, 0.6), 0)
        border(top, light(mat, 1.15), 1, shade(mat, 0.75))
        glow_sigil(top, ["..#..", ".#.#.", "#.#.#", ".#.#.", "..#.."], mix(col, GOLD, 0.5))
        side = stone_tex(rng, shade(mat, 0.8), 0.05)
        return side, top, dark_stone
    if kind == "vent":
        side = stone_tex(rng, (60, 55, 55), 0.08)
        top = stone_tex(rng, (55, 50, 50), 0.08)
        for y in range(3, 13):
            for x in range(3, 13):
                put(top, x, y, (25, 20, 20) if (x + y) % 3 else mix(col, (240, 120, 40), 0.5))
        for _ in range(4):
            put(top, rng.randrange(4, 12), rng.randrange(4, 12), (230, 230, 230))
        return side, top, dark_stone
    if kind == "totem":
        tw = mix(col, WOOD, 0.4)
        side = bark_tex(tw, rng)
        pt = Pal(tw)
        for y0 in (1, 9):
            for (x, y) in ((4, y0 + 1), (5, y0 + 1), (10, y0 + 1), (11, y0 + 1)):
                put(side, x, y, (240, 230, 200))
            for x in range(5, 11):
                put(side, x, y0 + 4, pt.dd)
            put(side, 7, y0 + 2, pt.h)
            put(side, 8, y0 + 2, pt.h)
        top = rings_tex(tw, rng)
        return side, top, top
    if kind == "portal_door":
        frame = (80, 72, 70)
        side = portal_swirl(rng, col)
        for y in range(16):
            for x in range(16):
                if x < 2 or x > 13 or y < 2:
                    put(side, x, y, shade(frame, 0.85 + rng.random() * 0.25))
        for x in range(2, 14):
            put(side, x, 2, light(frame, 1.3))
        return side, stone_tex(rng, frame, 0.08), dark_stone
    if kind == "pyre":
        side = new_img()
        noise_fill(side, (35, 25, 20), rng, 0.1)
        for y in range(2, 16, 4):
            for x in range(16):
                put(side, x, y, pw.m if x % 5 else pw.dd)
                put(side, x, y + 1, pw.d)
        for x in range(16):
            for y in range(0, 2):
                if rng.random() < 0.7:
                    put(side, x, y, (255, 160, 40) if y == 0 else (240, 90, 20))
        top = new_img()
        for y in range(16):
            for x in range(16):
                n = math.sin(x * 1.3 + y * 0.6) + rng.random()
                put(top, x, y, (255, 220, 100) if n > 1.2 else (240, 120, 30) if n > 0.2 else (120, 40, 15))
        return side, top, planks_tex(shade(wood, 0.6), rng)
    if kind == "crucible":
        iron = (60, 58, 62)
        side = metal_tex(iron, rng)
        for y in range(10, 16):
            for x in range(16):
                if rng.random() < 0.35:
                    put(side, x, y, (220, 90, 20))
        top = metal_tex(iron, rng)
        for (x, y) in mask(lambda d: d.ellipse((2, 2, 13, 13), fill=255)):
            put(top, x, y, mix((255, 210, 80), (230, 90, 20), rng.random()))
        return side, top, metal_tex(shade(iron, 0.7), rng)
    if kind == "fire_shrine":
        side = bricks_tex(mix(col, (110, 80, 70), 0.5), rng)
        glow_sigil(side, DATA_SIGILS["flame"], (255, 140, 40))
        top = stone_tex(rng, (70, 60, 58), 0.06)
        for (x, y) in mask(lambda d: d.ellipse((3, 3, 12, 12), fill=255)):
            put(top, x, y, mix((255, 230, 120), (230, 80, 20), math.hypot(x - 7.5, y - 7.5) / 5))
        return side, top, dark_stone
    if kind == "fountain":
        mat = light(grey, 1.25)
        side = stone_tex(rng, mat, 0.05)
        for x in range(16):
            put(side, x, 0, light(mat, 1.15))
            put(side, x, 3, shade(mat, 0.75))
        for y in range(5, 15):
            put(side, 7, y, (90, 150, 230))
            put(side, 8, y, (140, 190, 245))
        top = water_tex(rng, col)
        border(top, mat, 0, shade(mat, 0.8))
        sphere(top, 7.5, 7.5, 2, Pal(mat))
        return side, top, dark_stone
    if kind == "banner":
        flag = col if sat(col) > 0.2 else (60, 80, 180)
        side = new_img()
        noise_fill(side, (40, 32, 26), rng, 0.08)
        for y in range(16):
            put(side, 7, y, pw.m)
            put(side, 8, y, pw.d)
        for y in range(1, 11):
            for x in range(3, 13):
                if not (y > 8 and abs(x - 7.5) < (y - 8) * 2):
                    put(side, x, y, flag if 3 < x < 12 else shade(flag, 0.75))
        glow_sigil(side, ["..#..", ".###.", "#####", "..#..", "..#.."], GOLD, oy=3)
        top = planks_tex(wood, rng)
        return side, top, top
    if kind == "cape_stand":
        side = new_img()
        noise_fill(side, (40, 32, 26), rng, 0.08)
        for y in range(16):
            put(side, 7, y, pw.m)
            put(side, 8, y, pw.d)
        cols = [(200, 40, 40), (60, 90, 200), (60, 160, 60), (220, 180, 40)]
        for k, cc in enumerate(cols):
            for y in range(2 + k * 3, 5 + k * 3):
                for x in range(2 + k, 14 - k):
                    put(side, x, y, cc)
        for x in range(2, 14):
            put(side, x, 1, GOLD)
        top = planks_tex(wood, rng)
        return side, top, top
    if kind == "sack":
        burlap = mix(col, (170, 140, 90), 0.6)
        img = new_img()
        for y in range(16):
            for x in range(16):
                put(img, x, y, shade(burlap, 0.85 if (x + y) % 2 else 1.0) if (x % 4 and y % 4) else shade(burlap, 0.7))
        return img, img, img
    if kind == "pile":
        img = new_img()
        base = mix(col, (150, 140, 130), 0.4)
        noise_fill(img, base, rng, 0.2)
        for _ in range(12):
            x, y = rng.randrange(15), rng.randrange(15)
            put(img, x, y, light(base, 1.3))
            put(img, x + 1, y + 1, shade(base, 0.55))
        return img, img, img
    if kind == "stairs":
        mat = mix(col, (130, 125, 120), 0.5)
        side = stone_tex(rng, mat, 0.06)
        for k in range(4):
            for x in range(16):
                put(side, x, k * 4, light(mat, 1.2))
                put(side, x, k * 4 + 3, shade(mat, 0.6))
        return side, stone_tex(rng, mat, 0.06), dark_stone
    return None


def module_block_textures(b):
    """Return (side, top, bottom) 16x16 images for a module block."""
    bid, col = b["id"], rgb(b.get("color") if b.get("color") is not None else 0x8A8A8A)
    look = (b.get("look") or "").lower()
    kind = block_kind(b)
    rng = rng_for("module_block", bid)
    extra = extra_block_textures(kind, b, col, rng)
    if extra is not None:
        return extra
    p = Pal(col)
    grey = mix((120, 118, 122), col, 0.15)
    stone = stone_tex(rng, grey, 0.08)
    dark_stone = stone_tex(rng, shade(grey, 0.6), 0.08)
    if kind == "altar":
        side = stone_tex(rng, light(grey, 1.15), 0.05)
        for y in range(0, 10):
            for x in range(2, 14):
                put(side, x, y, p.m if x not in (2, 13) else GOLD)
        for x in range(2, 14):
            put(side, x, 10, GOLD)
            put(side, x, 9, p.d)
        for x in range(4, 12, 2):
            put(side, x, 11, p.d)
        top = stone_tex(rng, light(grey, 1.15), 0.05)
        for y in range(16):
            for x in range(4, 12):
                put(top, x, y, p.m if 0 < y < 15 else p.d)
        glow_sigil(top, ["..#..", ".###.", "#####", ".###.", "..#.."], mix(col, GOLD, 0.5))
        return side, top, dark_stone
    if kind == "chest":
        wood = mix(col, WOOD, 0.5)
        side = planks_tex(wood, rng)
        steel = Pal(STEEL if lum(col) < 170 else (90, 90, 96))
        for y in range(16):
            for x in (2, 13):
                put(side, x, y, steel.m)
        for x in range(16):
            put(side, x, 5, steel.d)
            put(side, x, 0, steel.l)
            put(side, x, 15, steel.d)
        for (x, y, c) in ((7, 5, GOLD), (8, 5, GOLD), (7, 6, GOLD), (8, 6, shade(GOLD, 0.7)), (7, 7, (40, 30, 20)),
                          (8, 7, shade(GOLD, 0.7))):
            put(side, x, y, c)
        top = planks_tex(light(wood, 1.08), rng)
        for y in range(16):
            for x in (2, 13):
                put(top, x, y, steel.l)
        border(top, steel.m, 0, steel.d)
        return side, top, planks_tex(shade(wood, 0.7), rng)
    if kind == "portal":
        frame = (70, 65, 75)
        return portal_swirl(rng, col, frame), portal_swirl(rng_for("module_block", bid, "t"), col), dark_stone
    if kind == "crate":
        wood = mix(col, WOOD, 0.45)
        img = planks_tex(wood, rng)
        pw = Pal(wood)
        border(img, pw.l, 0, pw.dd)
        border(img, pw.d, 1)
        line(img, 2, 2, 13, 13, pw.l)
        line(img, 2, 3, 12, 13, pw.d)
        for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
            put(img, x, y, (90, 90, 96))
        return img, img, img
    if kind == "statue":
        mat = mix(col, (200, 198, 192), 0.35)
        side = stone_tex(rng, shade(mat, 0.72), 0.05)
        pm = Pal(mat)
        fig = mask(lambda d: (d.ellipse((6, 1, 9, 4), fill=255), d.polygon([(5, 5), (10, 5), (11, 10), (9, 10),
                                                                            (9, 14), (6, 14), (6, 10), (4, 10)], fill=255)))
        for (x, y) in fig:
            put(side, x, y, pm.l if x < 8 else pm.m)
            if (x + 1, y) not in fig:
                put(side, x + 1, y, pm.dd)
        for x in range(16):
            put(side, x, 15, pm.d)
            put(side, x, 14, pm.d if x < 4 or x > 11 else side.getpixel((x, 14)))
        top = stone_tex(rng, mat, 0.05)
        border(top, pm.l, 0, pm.d)
        return side, top, stone_tex(rng, shade(mat, 0.7), 0.05)
    if kind == "range":
        side = bricks_tex(mix(col, (110, 100, 95), 0.6), rng)
        for y in range(7, 13):
            for x in range(3, 13):
                edge = x in (3, 12) or y in (7, 12)
                put(side, x, y, (30, 30, 30) if edge else mix((255, 150, 40), (200, 50, 10), rng.random()))
        top = stone_tex(rng, (60, 60, 64), 0.05)
        for (cx, cy) in ((4, 4), (11, 4), (4, 11), (11, 11)):
            for (x, y) in mask(lambda d, bb=(cx - 3, cy - 3, cx + 2, cy + 2): d.ellipse(bb, outline=255)):
                put(top, x, y, (25, 25, 28))
        border(top, p.d)
        return side, top, dark_stone
    if kind == "machine":
        metal = mix(col, (120, 122, 128), 0.4)
        side = metal_tex(metal, rng)
        gear(side, 5, 6, 2, light(metal, 1.35))
        gear(side, 10, 10, 3, shade(metal, 0.55))
        for x in range(1, 15):
            put(side, x, 13, (60, 60, 64) if x % 4 else GOLD)
        put(side, 12, 3, (220, 40, 40))
        put(side, 12, 5, (60, 220, 60))
        top = metal_tex(metal, rng)
        for x in range(3, 13):
            put(top, x, 7, shade(metal, 0.5))
            put(top, x, 8, light(metal, 1.2))
        return side, top, metal_tex(shade(metal, 0.7), rng)
    if kind == "obelisk":
        base = (45, 42, 50)
        side = stone_tex(rng, base, 0.08)
        runes = ["#.#", ".#.", "###", "#..", ".##", "#.#", "##.", ".#.", "#.#", "###"]
        for j, row in enumerate(runes):
            for i, ch in enumerate(row):
                if ch == "#" and 2 + j * 1.3 < 15:
                    put(side, 6 + i * 1 + (1 if i else 0), 2 + int(j * 1.3), mix(col, WHITE, 0.3))
        for y in range(16):
            put(side, 4, y, mix(base, col, 0.25))
            put(side, 11, y, mix(base, col, 0.25))
        top = stone_tex(rng, base, 0.08)
        for k in range(4):
            border(top, light(base, 1.0 + 0.12 * k), k * 2)
        put(top, 7, 7, mix(col, WHITE, 0.5))
        put(top, 8, 8, col)
        return side, top, stone_tex(rng, shade(base, 0.7), 0.08)
    if kind == "anvil":
        darkc = mix((60, 60, 66), col, 0.25)
        side = stone_tex(rng, shade(darkc, 0.55), 0.05)
        pix = mask(lambda d: d.polygon([(1, 2), (15, 2), (15, 5), (11, 7), (11, 11), (14, 14), (2, 14), (5, 11),
                                        (5, 7), (1, 5)], fill=255))
        for (x, y) in pix:
            put(side, x, y, light(darkc, 1.4) if y < 3 else light(darkc, 1.15) if y < 5 else darkc)
        top = stone_tex(rng, light(darkc, 1.4), 0.05)
        border(top, light(darkc, 1.6), 0, darkc)
        return side, top, stone_tex(rng, shade(darkc, 0.5), 0.05)
    if kind == "furnace":
        side = bricks_tex(mix(col, (110, 100, 95), 0.55), rng)
        for y in range(8, 14):
            for x in range(4, 12):
                t = (y - 8) / 6
                put(side, x, y, mix((255, 220, 80), (220, 70, 20), t) if (x + y) % 3 else (255, 160, 40))
        for x in range(3, 13):
            put(side, x, 7, (40, 34, 30))
        top = stone_tex(rng, mix(col, (90, 85, 80), 0.6))
        for (x, y) in mask(lambda d: d.ellipse((4, 4, 11, 11), fill=255)):
            put(top, x, y, (40, 30, 25))
        for (x, y) in mask(lambda d: d.ellipse((6, 6, 9, 9), fill=255)):
            put(top, x, y, (230, 110, 30))
        return side, top, dark_stone
    if kind == "brazier":
        metal = mix((70, 66, 70), col, 0.2)
        side = metal_tex(metal, rng)
        flame = col if sat(col) > 0.3 else (240, 120, 30)
        for y in range(0, 5):
            for x in range(16):
                n = math.sin(x * 1.7 + y) + rng.random()
                if n > 0.2 + y * 0.25:
                    put(side, x, y, mix(flame, (255, 230, 120), 0.5 * (1 - y / 5)))
        for x in range(16):
            put(side, x, 5, light(metal, 1.3))
        for y in range(6, 16):
            for x in (3, 12):
                put(side, x, y, shade(metal, 0.5))
        top = new_img()
        for y in range(16):
            for x in range(16):
                r = math.hypot(x - 7.5, y - 7.5)
                c = mix((255, 240, 160), flame, min(1, r / 6))
                put(top, x, y, mix(c, (60, 20, 10), rng.random() * 0.35) if r > 2 else c)
        border(top, shade(metal, 0.8))
        return side, top, metal_tex(shade(metal, 0.7), rng)
    if kind == "booth":
        wood = mix(col, DARK_WOOD, 0.5)
        side = counter_side(rng, wood)
        for y in range(2, 10):
            for x in range(2, 14):
                put(side, x, y, (30, 25, 20) if (x % 3 == 0 or y % 3 == 0) else (60, 50, 40))
        for x in range(2, 14):
            put(side, x, 2, GOLD)
            put(side, x, 10, GOLD)
        for x in range(16):
            put(side, x, 0, p.m)
            put(side, x, 1, p.d)
        top = planks_tex(wood, rng)
        border(top, p.m)
        return side, top, planks_tex(shade(wood, 0.7), rng)
    if kind == "table":
        wood = mix(col, WOOD, 0.5)
        pw = Pal(wood)
        side = new_img()
        noise_fill(side, (35, 28, 22), rng, 0.08)
        for y in range(0, 4):
            for x in range(16):
                put(side, x, y, pw.l if y == 0 else pw.m if y < 3 else pw.d)
        for y in range(4, 16):
            for x in (1, 2, 13, 14):
                put(side, x, y, pw.m if x in (1, 13) else pw.d)
        top = planks_tex(wood, rng)
        border(top, pw.d)
        return side, top, planks_tex(shade(wood, 0.6), rng)
    if kind == "door":
        wood = mix(col, WOOD, 0.4)
        pw = Pal(wood)
        side = new_img()
        for y in range(16):
            for x in range(16):
                c = pw.dd if x % 4 == 3 else (pw.m if rng.random() > 0.12 else pw.d)
                put(side, x, y, c)
        for y in (2, 12):
            for x in range(1, 12):
                put(side, x, y, (60, 60, 66))
                put(side, x, y + 1, (40, 40, 44))
        put(side, 12, 7, GOLD)
        put(side, 12, 8, shade(GOLD, 0.7))
        border(side, pw.dd)
        top = planks_tex(wood, rng)
        return side, top, top
    if kind == "rock":
        img = stone_tex(rng, mix(col, (120, 115, 110), 0.4), 0.14)
        for _ in range(4):
            x, y = rng.randrange(2, 13), rng.randrange(2, 13)
            line(img, x, y, x + rng.choice((-2, 2)), y + 2, shade(col, 0.45))
        return img, img, img
    if kind == "crystal":
        img = new_img()
        for y in range(16):
            for x in range(16):
                facet = ((x * 3 + y * 5) // 7 + (x * 7 - y * 2) // 9) % 4
                put(img, x, y, (p.h, p.l, p.m, p.d)[facet])
        for k in range(3):
            line(img, rng.randrange(16), 0, rng.randrange(16), 15, mix(col, WHITE, 0.55))
        glow_dots(img, col, rng, 5)
        return img, img, img
    if kind == "plant":
        leaf = col if col[1] >= max(col[0], col[2]) - 20 else mix(col, (70, 150, 50), 0.5)
        img = new_img()
        pl = Pal(leaf)
        noise_fill(img, pl.d, rng, 0.1)
        for _ in range(26):
            cx, cy = rng.randrange(16), rng.randrange(16)
            for (x, y, c) in ((cx, cy, pl.l), (cx + 1, cy, pl.m), (cx, cy + 1, pl.m), (cx + 1, cy + 1, pl.dd)):
                put(img, x % 16, y % 16, c)
        if col != leaf:
            for _ in range(5):
                put(img, rng.randrange(16), rng.randrange(16), mix(col, WHITE, 0.25))
        return img, img, img
    if kind == "bookcase":
        wood = mix(col, WOOD, 0.6)
        side = planks_tex(wood, rng)
        for row in (1, 9):
            for x in range(1, 15):
                bc = (rng.choice([(150, 40, 40), (40, 70, 150), (50, 120, 50), (160, 130, 50), col]))
                for y in range(row, row + 6):
                    put(side, x, y, shade(bc, 0.85 if x % 2 else 1.0))
        top = planks_tex(wood, rng)
        return side, top, top
    if kind == "barrel":
        wood = mix(col, WOOD, 0.5)
        side = planks_tex(wood, rng)
        for y in range(16):
            for x in range(0, 16, 4):
                put(side, x, y, shade(wood, 0.55))
        for y in (2, 13):
            for x in range(16):
                put(side, x, y, (80, 80, 86))
        top = new_img()
        noise_fill(top, wood, rng, 0.08)
        for (x, y) in mask(lambda d: d.ellipse((0, 0, 15, 15), outline=255)):
            put(top, x, y, (80, 80, 86))
        return side, top, top
    if kind == "coffin":
        base = mix(col, (95, 92, 88), 0.5)
        side = stone_tex(rng, base, 0.06)
        for x in range(16):
            put(side, x, 3, shade(base, 0.65))
            put(side, x, 12, shade(base, 0.65))
        top = stone_tex(rng, light(base, 1.05), 0.06)
        coffin_outline(top, shade(base, 0.6))
        glow_sigil(top, SIGILS["cross"], col if sat(col) > 0.25 else (200, 200, 200))
        return side, top, stone_tex(rng, shade(base, 0.6), 0.06)
    if kind == "cauldron":
        iron = (55, 55, 60)
        side = metal_tex(iron, rng)
        for x in range(16):
            put(side, x, 1, light(iron, 1.5))
        top = metal_tex(iron, rng)
        for (x, y) in mask(lambda d: d.ellipse((2, 2, 13, 13), fill=255)):
            put(top, x, y, mix(col, WHITE, 0.25) if rng.random() < 0.15 else col)
        return side, top, metal_tex(shade(iron, 0.7), rng)
    if kind == "pillar":
        mat = mix(col, (190, 186, 178), 0.4)
        side = stone_tex(rng, mat, 0.05)
        for y in range(16):
            for x in (2, 5, 8, 11, 14):
                put(side, x, y, shade(mat, 0.75))
        for x in range(16):
            for y in (0, 1, 14, 15):
                put(side, x, y, light(mat, 1.1) if y in (0, 15) else shade(mat, 0.85))
        top = stone_tex(rng, mat, 0.05)
        border(top, shade(mat, 0.8))
        return side, top, top
    if kind == "grave":
        mat = mix(col, (120, 120, 125), 0.6)
        side = stone_tex(rng, mat, 0.07)
        for x in range(5, 11):
            put(side, x, 6, shade(mat, 0.5))
        for y in range(3, 11):
            put(side, 7, y, shade(mat, 0.5))
            put(side, 8, y, shade(mat, 0.6))
        for x in range(4, 12):
            put(side, x, 13, shade(mat, 0.6))
        top = stone_tex(rng, mat, 0.07)
        return side, top, top
    if kind in ("lava", "water", "ice"):
        base = {"lava": (200, 70, 20), "water": (40, 90, 200), "ice": (170, 210, 240)}[kind]
        base = mix(base, col, 0.4)
        img = new_img()
        for y in range(16):
            for x in range(16):
                n = math.sin(x * 0.8 + y * 0.5) + math.sin(y * 1.1 - x * 0.4) + rng.random() * 0.6
                c = light(base, 1.35) if n > 1.3 else base if n > -0.2 else shade(base, 0.7)
                put(img, x, y, c)
        return img, img, img
    # generic panel
    side = new_img()
    noise_fill(side, p.m, rng, 0.05)
    border(side, p.h, 0, p.dd)
    border(side, p.l, 1, p.d)
    for y in range(3, 13):
        for x in range(3, 13):
            put(side, x, y, shade(p.m, 0.82 + rng.random() * 0.06))
    for x in range(3, 13):
        put(side, x, 3, p.dd)
        put(side, 3, x, p.dd)
        put(side, x, 12, p.l)
        put(side, 12, x, p.l)
    for (x, y) in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        put(side, x, y, mix(p.h, GOLD, 0.3))
    top = new_img()
    noise_fill(top, p.m, rng, 0.05)
    border(top, p.h, 0, p.dd)
    return side, top, stone_tex(rng, shade(p.m, 0.6), 0.06)


def finish_glow(imgs, col, look, key):
    """Looks that mention glow/magic get a few bright motes so emissive blocks read as lit."""
    if not any(k in (look or "") for k in ("glow", "magic", "light", "shine")):
        return imgs
    out = []
    for n, img in enumerate(imgs):
        img = img.copy()
        glow_dots(img, col, rng_for("glow", key, n), 7)
        out.append(img)
    return out


def gen_module_blocks(cat, names):
    for b in cat.get("blocks", []):
        bid = b["id"]
        side, top, bottom = finish_glow(module_block_textures(b), rgb(b.get("color") or 0x8A8A8A), b.get("look"), bid)
        base = f"module/{bid}"
        if side.tobytes() == top.tobytes() == bottom.tobytes():
            m = block_model(base, {"parent": "minecraft:block/cube_all", "textures": {"all": block_tex(side, base)}})
        else:
            m = block_model(base, cube_bottom_top(block_tex(side, base + "_side"), block_tex(top, base + "_top"),
                                                  block_tex(bottom, base + "_bottom")))
        blockstate(bid, {"variants": {"": {"model": m}}})
        block_item_model(bid, {"parent": m})
        names[bid] = b.get("name") or bid
        if b.get("breakable"):
            self_drop_loot(bid)


def self_drop_loot(bid):
    write_json(os.path.join(RES, "data", NS, "loot_tables", "blocks", bid + ".json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:{bid}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }, "loot tables")


# ----------------------------------------------------------------------------------------------
# Summoning stones for data-driven bosses
# ----------------------------------------------------------------------------------------------

DATA_SIGILS = {
    "claw": ["#..#..#.", "#..#..#.", "#..#..#.", ".#.#.#..", ".#.#.#..", "..###...", "..###...", "...#...."],
    "eye": ["........", "..####..", ".#....#.", "#..##..#", "#..##..#", ".#....#.", "..####..", "........"],
    "skull": ["..####..", ".######.", "##.##.##", "##.##.##", "########", ".######.", "..#..#..", "..####.."],
    "flame": ["...#....", "...##...", "..###.#.", ".#####..", ".######.", "########", ".######.", "..####.."],
    "web": ["#..#..#.", ".#.#.#..", "..###...", "#######.", "..###...", ".#.#.#..", "#..#..#.", "........"],
    "wave": ["........", ".##..##.", "#..##..#", "........", ".##..##.", "#..##..#", "........", "........"],
    "star": ["...#....", "...#....", "#######.", ".#####..", "..###...", ".##.##..", "#.....#.", "........"],
    "rune": ["#.####.#", ".#....#.", "#.#..#.#", "#..##..#", "#..##..#", "#.#..#.#", ".#....#.", "#.####.#"],
}
SIGIL_HINTS = [
    (("dragon", "wyrm", "drake", "wyvern", "vorkath", "hydra", "galvek", "elvarg"), "claw"),
    (("demon", "zamorak", "tsutsaroth", "abyssal", "sire", "infernal"), "zamorak"),
    (("bandos", "graardor", "goblin", "ork"), "bandos"),
    (("spider", "sarachnis", "venenatis", "araxxor"), "web"),
    (("kraken", "zulrah", "snake", "serpent", "leviathan", "whale", "fish", "tempoross", "dagannoth"), "wave"),
    (("nightmare", "phosani", "eye", "sotetseg", "verzik"), "eye"),
    (("skeleton", "undead", "vetion", "calvarion", "barrows", "lich", "necro"), "skull"),
    (("jad", "zuk", "fire", "lava", "tzhaar", "infernal", "inferno"), "flame"),
    (("saradomin", "zilyana", "armadyl", "kree", "star", "celestial"), "star"),
]


def creature_accent(entry, specs):
    """Most vivid colour of a boss: from its model parts if it has one, otherwise its biped skin."""
    spec = specs.get(entry.get("model") or entry["id"])
    cands = []
    if spec:
        for p in spec.get("parts", []):
            weight = 1.5 if (p.get("pattern") or "") in ("glow", "eyes") else 1.0
            for c in (p.get("color"), p.get("accent")):
                if c is not None:
                    cands.append((rgb(c), weight))
    else:
        sk = list(entry.get("skin") or DEFAULT_SKIN)
        for k, c in enumerate(sk):
            cands.append((rgb(c), 1.3 if k == 3 else 1.0))
    if not cands:
        return (200, 40, 40)
    best, w = max(cands, key=lambda cw: (sat(cw[0]) + 0.15) * (lum(cw[0]) + 40) * cw[1])
    if sat(best) < 0.2:
        best = mix(best, (200, 40, 40), 0.35) if lum(best) < 140 else best
    return mix(best, WHITE, 0.15) if lum(best) < 90 else best


def boss_sigil(entry):
    text = f"{entry['id']} {entry.get('name', '')}"
    for keys, sig in SIGIL_HINTS:
        if has_word(text, *keys):
            return DATA_SIGILS.get(sig) or SIGILS[sig]
    keys = sorted(DATA_SIGILS)
    return DATA_SIGILS[keys[int(hash01("sigil", entry["id"]) * len(keys))]]


def data_boss_stone(entry, specs):
    col = creature_accent(entry, specs)
    sig = boss_sigil(entry)
    key = entry["id"]
    ominous = mix((40, 37, 44), col, 0.08)
    side = stone_tex(rng_for("dboss", key, "s"), ominous, 0.10)
    for x in range(16):
        put(side, x, 1, shade(col, 0.45))
        put(side, x, 14, shade(col, 0.45))
    for x in range(1, 15, 3):  # carved rune band
        put(side, x, 2, shade(col, 0.7))
        put(side, x + 1, 13, shade(col, 0.7))
    glow_sigil(side, sig, shade(col, 0.85), oy=4)
    top = stone_tex(rng_for("dboss", key, "t"), shade(ominous, 0.9), 0.10)
    border(top, shade(col, 0.4))
    for (x, y) in mask(lambda d: d.ellipse((1, 1, 14, 14), outline=255)):
        put(top, x, y, shade(col, 0.55))
    glow_sigil(top, sig, col)
    bottom = stone_tex(rng_for("dboss", key, "b"), shade(ominous, 0.7), 0.08)
    return side, top, bottom


def gen_data_boss_stones(cat, names):
    legacy = {s[0] for s in boss_stone_specs()}
    specs = {m["id"]: m for m in cat.get("models", [])}
    for e in cat.get("bosses", []):
        if e.get("raid_only"):
            continue
        bid = e.get("summon_block") or e["id"] + "_summoning_stone"
        if bid in legacy:
            continue
        side, top, bottom = data_boss_stone(e, specs)
        m = block_model(bid, cube_bottom_top(block_tex(side, bid + "_side"), block_tex(top, bid + "_top"),
                                             block_tex(bottom, bid + "_bottom")))
        blockstate(bid, {"variants": {"": {"model": m}}})
        block_item_model(bid, {"parent": m})
        names[bid] = f"{e.get('name') or e['id']} summoning stone"


# ----------------------------------------------------------------------------------------------
# Lang
# ----------------------------------------------------------------------------------------------

BIOMES = ["misthalin", "lumbridge_swamp", "asgarnia", "wilderness", "kharidian_desert", "morytania", "kandarin",
          "feldip_hills", "fremennik_province", "troll_country", "karamja", "tirannwn", "uncharted_isles"]


def gen_lang(cat, block_names, entity_names):
    path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = {}
    if os.path.exists(path):  # keep keys added by hand or by earlier runs; generated values win
        try:
            with open(path, encoding="utf-8") as f:
                lang.update(json.load(f))
        except (OSError, ValueError):
            pass
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
    write_json(path, lang, "lang", sort_keys=True)
    return lang


# ----------------------------------------------------------------------------------------------
# Validation
# ----------------------------------------------------------------------------------------------

def resolve(ref, kind):
    ns, _, path = ref.partition(":")
    if not path:
        ns, path = "minecraft", ref
    return ns, os.path.join(RES, "assets", ns, kind, path + (".png" if kind == "textures" else ".json"))


def validate(cat):
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

    # catalog coverage
    def exists(*parts):
        return os.path.exists(os.path.join(ASSETS, *parts))

    for it in cat["items"]:
        if not exists("models", "item", it["id"] + ".json"):
            errors.append(f"item {it['id']}: missing item model")
        if not exists("textures", "item", it["id"] + ".png"):
            errors.append(f"item {it['id']}: missing item texture")
    block_ids = [b["id"] for b in cat.get("blocks", [])]
    block_ids += [b.get("summon_block") or b["id"] + "_summoning_stone" for b in cat.get("bosses", [])
                  if not b.get("raid_only")]
    for bid in block_ids:
        if not exists("blockstates", bid + ".json"):
            errors.append(f"block {bid}: missing blockstate")
        if not exists("models", "item", bid + ".json"):
            errors.append(f"block {bid}: missing block item model")
    for b in cat.get("blocks", []):
        if b.get("breakable") and not os.path.exists(os.path.join(RES, "data", NS, "loot_tables", "blocks",
                                                                  b["id"] + ".json")):
            errors.append(f"block {b['id']}: breakable but has no loot table")
    item_ids = Counter(i["id"] for i in cat["items"])
    for iid, n in item_ids.items():
        if n > 1:
            errors.append(f"duplicate item id {iid}")
    for bid in block_ids:
        if bid in item_ids:
            errors.append(f"id collision: block {bid} is also an item id")
    for bid, n in Counter(block_ids).items():
        if n > 1:
            errors.append(f"duplicate block id {bid}")
    armor_dir = os.path.join(RES, "assets", "minecraft", "textures", "models", "armor")
    for tier in armour_tiers(cat):
        for layer in (1, 2):
            if not os.path.exists(os.path.join(armor_dir, f"runecraft_{tier}_layer_{layer}.png")):
                errors.append(f"armour tier {tier}: missing layer {layer} texture")
    specs = {m["id"]: m for m in cat.get("models", [])}
    for m in specs.values():
        tp = os.path.join(ASSETS, "textures", "entity", "model", m["id"] + ".png")
        if not os.path.exists(tp):
            errors.append(f"model {m['id']}: missing texture")
            continue
        with Image.open(tp) as im:
            if im.size != (m["tex_w"], m["tex_h"]):
                errors.append(f"model {m['id']}: texture is {im.size}, spec wants {(m['tex_w'], m['tex_h'])}")
            px = im.convert("RGBA").load()
            for p in m.get("parts", []):
                for b in p.get("boxes", []):
                    for face, (x, y, w, h) in box_faces(b["u"], b["v"], b["w"], b["h"], b["d"]).items():
                        if w <= 0 or h <= 0:
                            continue
                        if x + w > m["tex_w"] or y + h > m["tex_h"]:
                            errors.append(f"model {m['id']}: part {p['name']} {face} face outside texture")
                        elif any(px[xx, yy][3] == 0 for yy in range(y, y + h) for xx in range(x, x + w)):
                            errors.append(f"model {m['id']}: part {p['name']} {face} face has transparent pixels")
    for group, label in (("monsters", "monster"), ("bosses", "boss")):
        for e in cat.get(group, []):
            if (e.get("model") or e["id"]) in specs:
                continue
            if e.get("model"):
                WARNINGS.append(f"{label} {e['id']}: model {e['model']} has no ModelSpec (renders as biped)")
            if not exists("textures", "entity", e["id"] + ".png"):
                errors.append(f"{label} {e['id']}: missing biped skin")
    for n in cat.get("npcs", []):
        if not exists("textures", "entity", "npc", n["id"] + ".png"):
            errors.append(f"npc {n['id']}: missing skin")
    return errors


def main(argv=None):
    global CATALOG, RES, ASSETS
    ap = argparse.ArgumentParser(description="Generate RuneCraft assets from the content catalog.")
    ap.add_argument("--catalog", default=CATALOG, help="catalog JSON (default: build/catalog.json)")
    ap.add_argument("--out", default=RES, help="resources root to write into (default: src/main/resources)")
    args = ap.parse_args(argv)
    CATALOG = os.path.abspath(args.catalog)
    RES = os.path.abspath(args.out)
    ASSETS = os.path.join(RES, "assets", NS)
    if not os.path.exists(CATALOG):
        sys.exit(f"missing {CATALOG}; run tools/run_core_tests.sh first")
    with open(CATALOG, encoding="utf-8") as f:
        cat = json.load(f)
    for key in ("monsters", "bosses", "npcs", "blocks", "models"):
        cat.setdefault(key, [])
    gen_items(cat)
    gen_armour(cat)
    block_names = gen_blocks(cat)
    gen_module_blocks(cat, block_names)
    gen_data_boss_stones(cat, block_names)
    gen_loot_tables()
    entity_names = gen_entities()
    gen_model_textures(cat)
    entity_names.update(gen_catalog_creatures(cat))
    gen_skill_icons(cat)
    gen_interface()
    lang = gen_lang(cat, block_names, entity_names)

    print(f"RuneCraft asset generation summary ({os.path.relpath(CATALOG, ROOT)} -> {RES}):")
    for k in sorted(COUNTS):
        print(f"  {k:20s} {COUNTS[k]}")
    print(f"  {'TOTAL':20s} {sum(COUNTS.values())}")
    print(f"  lang entries         {len(lang)}")
    print(f"  blocks               {len(block_names)}")
    print(f"  catalog content      {len(cat['monsters'])} monsters, {len(cat['bosses'])} bosses, "
          f"{len(cat['npcs'])} npcs, {len(cat['blocks'])} blocks, {len(cat['models'])} models")
    if FALLBACK_SHAPES:
        print("  shapes drawn by keyword fallback:", ", ".join(sorted(map(str, FALLBACK_SHAPES))))
    else:
        print("  shapes drawn by keyword fallback: none")
    errors = validate(cat)
    for w in WARNINGS:
        print("  warning: " + w)
    if errors:
        print("VALIDATION FAILED:")
        for e in errors[:50]:
            print("  " + e)
        sys.exit(1)
    print("Validation passed: JSON parses, referenced textures/models exist, every catalog item/block/model/"
          "creature has its assets.")


if __name__ == "__main__":
    main()
