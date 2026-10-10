#!/usr/bin/env python3
"""Generate OSRS-style skill icons, side-panel tab icons and the stone panel texture.

Writes (all deterministic, overwritten on every run):
  src/main/resources/assets/runecraft/textures/gui/skills/<skill>.png   32x32 skill icons
  src/main/resources/assets/runecraft/textures/gui/skills/tab_<tab>.png 32x32 side-panel tab icons
  src/main/resources/assets/runecraft/textures/gui/skills/stone.png     64x64 tileable panel stone
  docs/previews/skill_icons.png                                         contact sheet for review
  docs/previews/skills_panel.png                                        mock-up of the in-game side panel

Icons are drawn as vector shapes at 4x, box-downsampled to 32x32 with a hard alpha threshold
(so edges stay crisp like pixel art) and then given the dark 1px outline OSRS icons have.
The mock-up mirrors the layout constants in com.runecraft.osrs.ui.SkillPanel and the drawing in
com.runecraft.client.SidePanel / SkillsTab.

Usage: python3 tools/gen_skill_icons.py
Requires: Python 3 + Pillow. Does not touch tools/gen_assets.py outputs (textures/gui/skill/).
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "runecraft", "textures", "gui", "skills")
PREVIEWS = os.path.join(ROOT, "docs", "previews")

N = 32          # final icon size
S = 4           # supersampling factor
OUTLINE = (20, 14, 8, 255)

# Skill order = OSRS skills-tab layout (three columns, left-to-right, top-to-bottom).
SKILLS = ["attack", "hitpoints", "mining", "strength", "agility", "smithing", "defence", "herblore", "fishing",
          "ranged", "thieving", "cooking", "prayer", "crafting", "firemaking", "magic", "fletching", "woodcutting",
          "runecraft", "slayer", "farming", "construction", "hunter", "sailing"]
NAMES = {s: s.capitalize() for s in SKILLS}


def rgb(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


class Canvas:
    """A 4x supersampled drawing surface addressed in 32x32 icon coordinates."""

    def __init__(self):
        self.img = Image.new("RGBA", (N * S, N * S), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    @staticmethod
    def _p(pts):
        return [(x * S, y * S) for x, y in pts]

    def poly(self, pts, col):
        self.d.polygon(self._p(pts), fill=rgb(col))

    def rect(self, x0, y0, x1, y1, col):
        self.d.rectangle([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], fill=rgb(col))

    def ellipse(self, x0, y0, x1, y1, col):
        self.d.ellipse([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], fill=rgb(col))

    def circle(self, cx, cy, r, col):
        self.ellipse(cx - r, cy - r, cx + r, cy + r, col)

    def line(self, pts, col, w):
        self.d.line(self._p(pts), fill=rgb(col), width=int(w * S), joint="curve")
        for x, y in (pts[0], pts[-1]):
            self.d.ellipse([(x - w / 2) * S, (y - w / 2) * S, (x + w / 2) * S, (y + w / 2) * S], fill=rgb(col))

    def bar(self, p0, p1, w, col):
        """A straight rectangular bar of width w from p0 to p1 (square ends)."""
        (x0, y0), (x1, y1) = p0, p1
        dx, dy = x1 - x0, y1 - y0
        ln = math.hypot(dx, dy) or 1
        nx, ny = -dy / ln * w / 2, dx / ln * w / 2
        self.poly([(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)], col)

    def arc(self, cx, cy, r, a0, a1, col, w):
        pts = [(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
               for a in frange(a0, a1, 4)]
        self.line(pts, col, w)

    def erase_ellipse(self, x0, y0, x1, y1):
        self.d.ellipse([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], fill=(0, 0, 0, 0))

    def erase_poly(self, pts):
        self.d.polygon(self._p(pts), fill=(0, 0, 0, 0))

    def finish(self, outline=True):
        small = self.img.resize((N, N), Image.BOX)
        px = small.load()
        for y in range(N):
            for x in range(N):
                r, g, b, a = px[x, y]
                if a >= 110:
                    # Un-premultiply the box average so edge pixels keep their colour.
                    f = 255.0 / a
                    px[x, y] = (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), 255)
                else:
                    px[x, y] = (0, 0, 0, 0)
        if outline:
            add_outline(small)
        return small


def frange(a, b, step):
    out = []
    v = a
    while (step > 0 and v <= b) or (step < 0 and v >= b):
        out.append(v)
        v += step
    if out[-1] != b:
        out.append(b)
    return out


def add_outline(img, col=OUTLINE):
    px = img.load()
    w, h = img.size
    solid = {(x, y) for y in range(h) for x in range(w) if px[x, y][3] > 0}
    for y in range(h):
        for x in range(w):
            if (x, y) in solid:
                continue
            if any((x + dx, y + dy) in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = col


# ---------------------------------------------------------------------------------------------
# Skill icons
# ---------------------------------------------------------------------------------------------

def icon_attack(c):
    # Steel sword pointing up-right with a gold crossguard (OSRS Attack).
    c.poly([(7.5, 22.5), (24, 6), (28.5, 3.5), (26, 8), (9.5, 24.5)], 0xC8CCD4)
    c.poly([(7.5, 22.5), (24, 6), (28.5, 3.5)], 0xF2F4F8)           # bright edge
    c.bar((8.5, 23.5), (26.5, 5.5), 0.8, 0x8A909C)                   # fuller
    c.bar((4.5, 18.5), (13.5, 27.5), 3.2, 0xD8A830)                  # crossguard
    c.bar((4.5, 18.5), (13.5, 27.5), 1.2, 0xF8D860)
    c.bar((3.5, 28.5), (8.5, 23.5), 3.0, 0x6E4422)                   # grip
    c.circle(3.2, 28.8, 2.2, 0xD8A830)                               # pommel


def icon_hitpoints(c):
    red, dark, light = 0xC81818, 0x8A0C0C, 0xF06060
    c.circle(10, 11, 7, red)
    c.circle(22, 11, 7, red)
    c.poly([(3.4, 13), (16, 28.5), (28.6, 13), (16, 10)], red)
    c.poly([(16, 28.5), (28.6, 13), (27, 18), (18, 27.5)], dark)
    c.ellipse(6.5, 7, 11.5, 12, light)
    c.ellipse(7.5, 8, 9.5, 10, 0xFFC8C8)


def icon_strength(c):
    # Flexed arm (OSRS Strength): forearm raised from the elbow, clenched fist on top and a
    # bulging bicep on the upper arm, separated by a dark inner-elbow crease.
    skin, dark, light = 0xD89058, 0x8A4E24, 0xF4C090
    c.poly([(1, 21), (20, 19), (27, 23), (25, 29.5), (1, 29.5)], skin)   # upper arm + elbow
    c.poly([(19, 21), (19.5, 11), (27.5, 11), (28.5, 25), (25, 29)], skin)  # forearm
    c.poly([(25.5, 11), (27.5, 11), (28.5, 25), (25.5, 28.5)], dark)
    c.ellipse(17, 1.5, 30, 13, skin)                                       # fist
    c.ellipse(18.5, 2.5, 24, 6.5, light)
    for y in (5.5, 8.5):
        c.line([(19, y), (27.5, y)], dark, 0.9)                            # finger creases
    c.line([(23.5, 2), (23.5, 5.5)], dark, 0.9)
    c.ellipse(3, 11, 19.5, 24, skin)                                       # bicep
    c.ellipse(6, 12.5, 13, 17, light)
    c.arc(11.5, 17.5, 7.6, -60, 70, dark, 1.2)                             # crease under the bicep
    c.line([(2, 28), (23, 28)], dark, 1.2)


def icon_defence(c):
    # Kiteshield: steel rim, blue face, lighter vertical band.
    rim, face, band, dark = 0xB8C0CC, 0x4A62B0, 0x7C94D8, 0x2C3C7C
    c.poly([(4, 4), (28, 4), (28, 13), (16, 30), (4, 13)], rim)
    c.poly([(6.5, 6.5), (25.5, 6.5), (25.5, 12.5), (16, 26), (6.5, 12.5)], face)
    c.poly([(16, 6.5), (25.5, 6.5), (25.5, 12.5), (16, 26)], dark)
    c.poly([(13.5, 6.5), (18.5, 6.5), (18.5, 21), (16, 25), (13.5, 21)], band)
    c.rect(6.5, 11, 25.5, 14, rim)
    c.rect(4, 4, 28, 5.2, 0xE8ECF2)


def icon_ranged(c):
    # Short bow with a nocked arrow pointing up-right (OSRS Ranged).
    c.arc(13, 19, 14, 205, 335, 0x8A5A2A, 2.6)                         # bow limb
    c.arc(13, 19, 14, 215, 325, 0xB88040, 1.0)
    p0 = (13 + 14 * math.cos(math.radians(205)), 19 + 14 * math.sin(math.radians(205)))
    p1 = (13 + 14 * math.cos(math.radians(335)), 19 + 14 * math.sin(math.radians(335)))
    c.line([p0, (13, 22), p1], 0xE8E8E0, 1.2)                          # drawn string
    # rotate the whole thing 45 degrees: draw arrow along the bisector
    c.bar((13, 27), (13, 4), 1.4, 0xC8A060)                            # shaft
    c.poly([(10, 6), (13, 0.8), (16, 6)], 0xC0C4CC)                    # head
    c.poly([(13, 6), (13, 0.8), (16, 6)], 0x8A909A)
    c.poly([(10, 30), (13, 24), (13, 30.5)], 0x30A040)                 # fletching
    c.poly([(16, 30), (13, 24), (13, 30.5)], 0x208030)


def rotate(img, deg):
    return img.rotate(deg, resample=Image.NEAREST, center=(N * S / 2, N * S / 2))


def icon_prayer(c):
    # Eight-pointed white star with a soft blue shade (OSRS Prayer).
    cx, cy = 16, 16
    pts = []
    for i in range(16):
        r = 14.5 if i % 2 == 0 else 5.5
        if i % 4 == 2:
            r = 9.5
        a = math.radians(-90 + i * 22.5)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    c.poly(pts, 0xE8EEF4)
    shade = [(cx, cy)] + [p for i, p in enumerate(pts) if 4 <= i <= 12]
    c.poly(shade, 0xA8C8E0)
    c.circle(cx, cy, 3.2, 0xFFFFFF)


def icon_magic(c):
    # Blue wizard hat with a curled tip, a brim and yellow stars.
    blue, dark, light = 0x2E3CB8, 0x1C2470, 0x5868E8
    c.ellipse(1.5, 22, 30.5, 30, blue)                                 # brim
    c.ellipse(4, 24.5, 28, 29.5, dark)
    c.poly([(7, 26), (12, 12), (17, 4), (22, 1.5), (27, 4), (21, 7), (19, 14), (25, 26)], blue)
    c.poly([(16, 26), (19, 14), (21, 7), (27, 4), (22, 2.5), (18.5, 5.5), (25, 26)], dark)
    c.poly([(9, 25), (13, 13), (17, 6), (15, 14), (13, 25)], light)
    for (x, y, r) in ((11.5, 20, 1.6), (17.5, 13, 1.3), (20, 21.5, 1.4)):
        c.poly([(x, y - r * 1.8), (x + r * 0.5, y - r * 0.5), (x + r * 1.8, y), (x + r * 0.5, y + r * 0.5),
                (x, y + r * 1.8), (x - r * 0.5, y + r * 0.5), (x - r * 1.8, y), (x - r * 0.5, y - r * 0.5)], 0xF8E040)


def icon_runecraft(c):
    # A grey rune stone with a glowing engraved glyph.
    c.ellipse(3, 4, 29, 29, 0x8A8878)
    c.ellipse(4, 5, 27, 26, 0xB4B29C)
    c.ellipse(6, 6.5, 15, 13, 0xD4D2C0)
    glyph = 0xE07818
    c.arc(16, 17, 6.5, 200, 520, glyph, 2.0)
    c.bar((16, 8), (16, 26), 2.0, glyph)
    c.bar((10, 17), (22, 17), 2.0, glyph)
    c.circle(16, 17, 2.2, 0xF8D060)


def icon_construction(c):
    # Saw (grey toothed blade, brown handle) crossed with a claw hammer.
    blade, teeth = 0xC0C6CE, 0x8A909A
    c.poly([(4, 25), (21, 8), (26, 11), (9.5, 28)], blade)
    for i in range(7):
        t = i / 6.5
        x, y = 5 + t * 17, 27.5 - t * 17
        c.poly([(x, y), (x + 2.2, y - 2.2), (x + 2.6, y + 0.6)], teeth)
    c.poly([(20, 6), (25, 2), (31, 8), (27, 12)], 0x8A5A2A)            # handle
    c.erase_ellipse(24, 5, 27.5, 8.5)
    c.bar((22, 26), (8, 6), 2.6, 0xA06E36)                             # hammer shaft
    c.poly([(3, 7), (9, 1.5), (13.5, 6), (11, 8), (8, 6), (5.5, 9.5)], 0x5A5E66)
    c.poly([(9, 1.5), (13.5, 6), (12, 7), (8, 3)], 0x8A909A)


def icon_agility(c):
    # Running figure in blue (OSRS Agility).
    col, dark = 0x2840B0, 0x182878
    c.circle(19, 5, 3.4, 0xE0A070)                                     # head
    c.line([(18, 9), (13.5, 18)], col, 4.2)                            # torso
    c.line([(17, 10.5), (23, 13), (26.5, 9.5)], col, 2.4)              # front arm
    c.line([(15.5, 11), (10, 12.5), (6.5, 16.5)], dark, 2.4)           # back arm
    c.line([(13.5, 18), (20, 21.5), (19, 29)], col, 2.8)               # front leg
    c.line([(13, 18.5), (9, 24), (3, 24)], dark, 2.8)                  # back leg
    c.rect(18, 28, 23, 30, 0x6E4422)                                   # boots
    c.rect(1.5, 22.5, 4, 26, 0x6E4422)


def leaf(c, base, tip, width, col, vein):
    (bx, by), (tx, ty) = base, tip
    dx, dy = tx - bx, ty - by
    ln = math.hypot(dx, dy)
    nx, ny = -dy / ln, dx / ln
    pts = []
    for i in range(0, 13):
        t = i / 12
        w = math.sin(math.pi * t) * width / 2 * (1.15 - 0.3 * t)
        pts.append((bx + dx * t + nx * w, by + dy * t + ny * w))
    for i in range(12, -1, -1):
        t = i / 12
        w = math.sin(math.pi * t) * width / 2 * (1.15 - 0.3 * t)
        pts.append((bx + dx * t - nx * w, by + dy * t - ny * w))
    c.poly(pts, col)
    c.line([(bx + dx * 0.1, by + dy * 0.1), (bx + dx * 0.85, by + dy * 0.85)], vein, 0.7)


def icon_herblore(c):
    # A bunch of green herb leaves on a stem.
    c.line([(16, 30), (16, 14)], 0x4A7A20, 1.8)
    leaf(c, (16, 18), (5, 6), 8, 0x2E8A20, 0x1C5A12)
    leaf(c, (16, 17), (27, 6), 8, 0x2E8A20, 0x1C5A12)
    leaf(c, (16, 15), (16, 1.5), 8.5, 0x48B030, 0x2A7A1A)
    leaf(c, (16, 23), (4, 21), 6, 0x48B030, 0x2A7A1A)
    leaf(c, (16, 23), (28, 21), 6, 0x2E8A20, 0x1C5A12)


def icon_thieving(c):
    # Purple domino mask with eye holes and trailing ties.
    col, dark, light = 0x6C3C88, 0x44225A, 0x9A64B8
    c.poly([(1, 20), (5, 17), (5, 19)], dark)
    c.poly([(31, 20), (27, 17), (27, 19)], dark)
    c.line([(1.5, 20), (4, 24.5)], dark, 1.8)
    c.line([(30.5, 20), (28, 24.5)], dark, 1.8)
    c.poly([(3, 12), (9, 9), (16, 11), (23, 9), (29, 12), (28.5, 19), (23, 23), (17.5, 20.5), (16, 22),
            (14.5, 20.5), (9, 23), (3.5, 19)], col)
    c.poly([(4, 12.5), (9, 10), (15, 12), (9, 12)], light)
    c.erase_poly([(6.5, 15), (10, 13), (13.5, 15.5), (12.5, 18), (8, 18)])
    c.erase_poly([(25.5, 15), (22, 13), (18.5, 15.5), (19.5, 18), (24, 18)])


def icon_crafting(c):
    # Chisel: brown wooden handle and a steel blade, diagonal.
    c.bar((4, 28), (15, 17), 5.5, 0x8A5A2A)                            # handle
    c.bar((5, 27), (14, 18), 1.5, 0xB88040)
    c.bar((14, 18), (16.5, 15.5), 6.2, 0x5A5E66)                       # ferrule
    c.poly([(14.8, 14.0), (17.9, 17.1), (27.5, 7.5), (26.5, 4.5), (24.5, 4.5)], 0xC0C6CE)  # blade
    c.poly([(14.8, 14.0), (24.5, 4.5), (26.5, 4.5), (17, 14)], 0xE8ECF2)
    c.bar((24.5, 4.5), (27.5, 7.5), 1.2, 0x8A909A)


def icon_fletching(c):
    # A long arrow with big teal fletching (OSRS Fletching).
    c.bar((5, 27), (25, 7), 1.6, 0xC8A060)
    c.poly([(21, 6), (29, 2), (26, 11)], 0xC0C6CE)
    c.poly([(25, 7), (29, 2), (26, 11)], 0x8A909A)
    for off in (0, 4):
        x, y = 6 + off, 26 - off
        c.poly([(x, y), (x - 4.5, y - 3.5), (x - 3.5, y - 7), (x + 2.5, y - 2.5)], 0x10A8A0)
        c.poly([(x, y), (x + 3.5, y + 4.5), (x + 7, y + 3.5), (x + 2.5, y - 2.5)], 0x0A7470)


def icon_slayer(c):
    # A bone-white skull with dark sockets (OSRS Slayer).
    bone, shade = 0xE0D8C4, 0xA89E86
    c.ellipse(4, 2, 28, 24, bone)
    c.rect(9, 18, 23, 28, bone)
    c.poly([(21, 4), (27, 10), (27.5, 17), (23, 23), (23, 28), (20, 28), (21, 20)], shade)
    c.erase_ellipse(7.5, 11, 14.5, 18.5)
    c.erase_ellipse(17.5, 11, 24.5, 18.5)
    c.erase_poly([(16, 18.5), (14, 22.5), (18, 22.5)])
    for x in (11.5, 15, 18.5):
        c.rect(x, 25, x + 1.2, 28.5, 0x6E6250)
    c.ellipse(7, 4, 13, 9, 0xF8F4E8)


def icon_hunter(c):
    # Brown animal paw print.
    col, light = 0x7A5A30, 0xA68050
    c.ellipse(8.5, 16, 23.5, 29.5, col)
    c.ellipse(10.5, 17.5, 16.5, 21.5, light)
    for (x, y, rx, ry) in ((5.5, 13, 3, 4), (11.5, 7.5, 3.2, 4.3), (20.5, 7.5, 3.2, 4.3), (26.5, 13, 3, 4)):
        c.ellipse(x - rx, y - ry, x + rx, y + ry, col)
        c.ellipse(x - rx + 1, y - ry + 1, x, y, light)


def icon_mining(c):
    # Pickaxe: curved steel head over a brown handle.
    c.bar((6, 29), (22, 9), 3.0, 0x8A5A2A)
    c.bar((7, 27.5), (21, 10), 1.0, 0xB88040)
    pts_top = []
    pts_bot = []
    for i in range(0, 21):
        t = i / 20
        a = math.radians(200 + t * 140)
        pts_top.append((19 + 15 * math.cos(a), 17 + 13 * math.sin(a)))
        w = 3.2 * math.sin(math.pi * t) + 0.6
        pts_bot.append((19 + (15 - w) * math.cos(a), 17 + (13 - w) * math.sin(a)))
    c.poly(pts_top + pts_bot[::-1], 0x9AA0AA)
    c.poly(pts_top[3:12] + pts_bot[3:12][::-1], 0xD0D6DE)
    c.rect(18.5, 4.5, 23.5, 9.5, 0x5A5E66)


def icon_smithing(c):
    # Hammer above an anvil.
    anvil, top, dark = 0x5A5E66, 0x8A909A, 0x3A3E44
    c.poly([(2, 18), (24, 18), (30, 15.5), (30, 18.5), (26, 22), (20, 22), (20, 25), (24, 29), (8, 29), (12, 25),
            (12, 22), (6, 22)], anvil)
    c.poly([(2, 18), (24, 18), (30, 15.5), (30, 17), (25, 19.5), (3, 19.5)], top)
    c.rect(8, 27, 24, 29, dark)
    c.bar((9, 16), (21, 4), 2.4, 0xA06E36)                             # hammer shaft
    c.poly([(16, 2), (21, -1), (27, 5), (22, 8)], 0x7A8088)            # head
    c.poly([(16, 2), (21, -1), (22, 0.5), (17.5, 3.5)], 0xB8BEC6)


def icon_fishing(c):
    # Blue fish swimming left, with a forked tail and fins.
    body, dark, belly = 0x5A84C0, 0x34568C, 0xA8C8E8
    c.poly([(23, 16), (30, 8), (31, 10), (28, 16), (31, 22), (30, 24)], dark)   # tail
    c.ellipse(2, 9, 26, 23, body)
    c.poly([(9, 10), (14, 5), (19, 10)], dark)                                  # dorsal fin
    c.poly([(12, 21), (15, 26), (18, 21.5)], dark)
    c.ellipse(4, 16, 22, 22.5, belly)
    c.circle(7.5, 14, 1.6, 0x101010)
    c.circle(7.0, 13.5, 0.6, 0xFFFFFF)
    c.arc(12, 16, 4, 300, 420, 0x2A4878, 0.9)                                   # gill


def icon_cooking(c):
    # A puffy white chef's hat.
    white, shade = 0xF4F4F0, 0xC0C0BC
    c.circle(9, 12, 6.5, white)
    c.circle(16, 8.5, 7.5, white)
    c.circle(23, 12, 6.5, white)
    c.rect(8, 12, 24, 23, white)
    c.poly([(24, 12), (29.5, 12), (24, 23)], shade)
    c.ellipse(19, 9, 29.5, 18.5, shade)
    c.circle(16, 8.5, 7.5, white)
    c.circle(22, 11.5, 5.8, white)
    c.rect(7, 22, 25, 29, 0xE0E0DC)                                             # band
    c.rect(7, 26.5, 25, 29, shade)
    c.line([(12, 16), (12, 21)], shade, 0.8)
    c.line([(18, 15), (18, 21)], shade, 0.8)


def icon_firemaking(c):
    # A flame: red outer, orange body, yellow core, above two crossed logs.
    c.bar((4, 28), (28, 24), 3.2, 0x7A4A22)
    c.bar((4, 24), (28, 28), 3.2, 0x9A6230)
    outer = [(16, 1), (20, 7), (25, 12), (26, 19), (22, 25), (10, 25), (6, 19), (7, 12), (10, 15), (11, 8)]
    c.poly(outer, 0xD83010)
    c.poly([(16, 6), (19, 11), (22.5, 15), (22.5, 20), (19.5, 24.5), (12.5, 24.5), (9.5, 20), (10, 16), (12.5, 17.5),
            (13.5, 11)], 0xF08818)
    c.poly([(16, 12), (18.5, 16), (19.5, 20.5), (16, 24.5), (12.5, 21), (13, 17), (14.5, 18.5)], 0xF8E048)


def icon_woodcutting(c):
    # A round green tree on a brown trunk with an axe resting against it.
    c.rect(13, 17, 19, 30, 0x6E4422)
    c.rect(13, 17, 15, 30, 0x8A5A2A)
    c.poly([(10, 30), (13, 27), (19, 27), (22, 30)], 0x6E4422)
    for (x, y, r, col) in ((10, 13, 6.5, 0x2E6E1E), (22, 13, 6.5, 0x2E6E1E), (16, 7.5, 7.5, 0x3A8A26),
                           (16, 15, 6.5, 0x2E6E1E), (13, 9, 3.5, 0x58A838)):
        c.circle(x, y, r, col)
    c.bar((29, 30), (22, 18), 1.8, 0xA06E36)                                    # axe handle
    c.poly([(21, 19.5), (24.5, 16), (28, 19), (25.5, 22)], 0xB8BEC6)


def icon_farming(c):
    # A green watering can with a spout and rose, pouring.
    body, dark, light = 0x5C8A48, 0x3A5E2C, 0x8AB870
    c.arc(21, 14, 6.5, 180, 360, dark, 2.4)                                     # handle
    c.poly([(12, 16), (30, 16), (29, 29), (13, 29)], body)
    c.poly([(24, 16), (30, 16), (29, 29), (25, 29)], dark)
    c.rect(14, 18, 17, 27, light)
    c.bar((13, 22), (5, 12), 2.6, body)                                         # spout
    c.poly([(1, 11), (5, 7), (8, 10), (4, 14)], dark)                           # rose
    for (x, y) in ((1, 16), (3, 18.5), (0.5, 20.5)):
        c.circle(x + 0.5, y, 0.9, 0x60A8E8)


def icon_sailing(c):
    # A steel anchor with a ring and a rope wrap.
    steel, dark, light = 0x7A8A9C, 0x4A5666, 0xB0BECC
    c.arc(16, 4.5, 3, 0, 360, steel, 1.8)                                       # ring
    c.bar((16, 7), (16, 26), 3.2, steel)                                        # shank
    c.bar((16.8, 8), (16.8, 25), 1.0, dark)
    c.bar((8, 11), (24, 11), 2.6, steel)                                        # stock
    c.circle(8, 11, 1.6, steel)
    c.circle(24, 11, 1.6, steel)
    c.arc(16, 17, 11, 25, 155, steel, 2.8)                                      # arms
    c.poly([(1.5, 18.5), (5, 16), (7.5, 22.5)], steel)                          # flukes
    c.poly([(30.5, 18.5), (27, 16), (24.5, 22.5)], steel)
    c.arc(16, 17, 11, 70, 110, light, 1.0)
    c.line([(13, 14), (19, 17), (13, 20)], 0xC8A060, 1.2)                       # rope


SKILL_ICONS = {name: globals()["icon_" + name] for name in SKILLS}


# ---------------------------------------------------------------------------------------------
# Tab icons
# ---------------------------------------------------------------------------------------------

def tab_combat(c):
    for flip in (False, True):
        def fx(x):
            return 32 - x if flip else x
        c.poly([(fx(5), 25), (fx(23), 7), (fx(27), 4.5), (fx(24.5), 9), (fx(7), 27)], 0xC8CCD4)
        c.bar((fx(3), 21), (fx(10), 28), 2.6, 0xD8A830)
        c.bar((fx(2), 29.5), (fx(6), 25.5), 2.4, 0x6E4422)


def tab_skills(c):
    # Stats: three green bars rising (like the OSRS stats tab).
    for i, h in enumerate((10, 17, 24)):
        x = 4 + i * 9
        c.rect(x, 28 - h, x + 7, 28, 0x30A030)
        c.rect(x, 28 - h, x + 2.5, 28, 0x60D050)
    c.rect(2, 28, 30, 30, 0x8A7A5A)


def tab_quests(c):
    # Blue quest icon: a rolled parchment with a blue seal.
    c.rect(7, 5, 25, 27, 0xE8D8A8)
    c.ellipse(4, 2, 28, 8, 0xC8B080)
    c.ellipse(4, 24, 28, 30, 0xC8B080)
    for y in (11, 15, 19):
        c.line([(10, y), (22, y)], 0x8A7A5A, 1.0)
    c.circle(20, 22, 4, 0x2A5AC0)
    c.circle(19, 21, 1.5, 0x80A8F0)


def tab_gear(c):
    # Equipment: a steel full helm.
    c.ellipse(5, 2, 27, 26, 0x9AA0AA)
    c.rect(5, 14, 27, 29, 0x9AA0AA)
    c.ellipse(8, 4, 16, 12, 0xD0D6DE)
    c.rect(8, 14, 24, 17.5, 0x202020)
    c.rect(14.5, 17, 17.5, 26, 0x202020)
    c.rect(20, 3, 27, 29, 0x6E747E)


def tab_log(c):
    # Collection log: a brown book with a gold clasp.
    c.rect(5, 3, 26, 29, 0x7A4A22)
    c.rect(5, 3, 8, 29, 0x5A3418)
    c.rect(24, 5, 28, 28, 0xE8E0C8)
    c.rect(12, 10, 22, 14, 0xD8A830)
    c.rect(22, 15, 26, 19, 0xD8A830)


def tab_expand(c):
    # Opens the full interface: four outward arrows.
    col = 0xD8C890
    for (ax, ay, dx, dy) in ((4, 4, 1, 1), (28, 4, -1, 1), (4, 28, 1, -1), (28, 28, -1, -1)):
        c.poly([(ax, ay), (ax + 9 * dx, ay), (ax, ay + 9 * dy)], col)
        c.bar((ax + 3 * dx, ay + 3 * dy), (ax + 10 * dx, ay + 10 * dy), 2.4, col)


def tab_close(c):
    col = 0xC03020
    c.bar((7, 7), (25, 25), 4, col)
    c.bar((25, 7), (7, 25), 4, col)


def gen_tab_icons():
    out = {}
    out["combat"] = draw(tab_combat)
    out["skills"] = draw(tab_skills)
    out["quests"] = draw(tab_quests)
    out["gear"] = draw(tab_gear)
    out["log"] = draw(tab_log)
    out["expand"] = draw(tab_expand)
    out["close"] = draw(tab_close)
    out["prayer"] = draw(icon_prayer)
    out["magic"] = draw(icon_magic)
    return out


def draw(fn):
    c = Canvas()
    fn(c)
    if fn is icon_ranged:
        c.img = rotate(c.img, -45)
    return c.finish()


# ---------------------------------------------------------------------------------------------
# Stone texture (tileable) used for the side panel background and tab bar
# ---------------------------------------------------------------------------------------------

STONE_BASE = (0x3E, 0x35, 0x29)


def gen_stone(size=64, seed=1337):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    # Value noise with wrap-around so the texture tiles seamlessly.
    grid = 8
    vals = [[rnd.random() for _ in range(grid)] for _ in range(grid)]

    def noise(x, y):
        gx, gy = x / size * grid, y / size * grid
        x0, y0 = int(gx) % grid, int(gy) % grid
        x1, y1 = (x0 + 1) % grid, (y0 + 1) % grid
        fx, fy = gx - int(gx), gy - int(gy)
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a = vals[y0][x0] * (1 - fx) + vals[y0][x1] * fx
        b = vals[y1][x0] * (1 - fx) + vals[y1][x1] * fx
        return a * (1 - fy) + b * fy

    for y in range(size):
        for x in range(size):
            n = noise(x, y) * 0.7 + rnd.random() * 0.3
            k = 0.78 + n * 0.42
            px[x, y] = tuple(min(255, int(c * k)) for c in STONE_BASE) + (255,)
    # A few darker cracks between "bricks" (wrapped) to give the OSRS masonry feel.
    d = ImageDraw.Draw(img)
    dark = (0x26, 0x20, 0x18, 255)
    for row in range(4):
        y = row * 16
        d.line([(0, y), (size, y)], fill=dark)
        off = 0 if row % 2 == 0 else 16
        for col in range(2):
            x = (off + col * 32) % size
            d.line([(x, y), (x, y + 15)], fill=dark)
    hi = (0x52, 0x48, 0x38, 255)
    for row in range(4):
        y = row * 16 + 1
        d.line([(0, y), (size, y)], fill=hi)
    return img


# ---------------------------------------------------------------------------------------------
# Contact sheet + mock-up
# ---------------------------------------------------------------------------------------------

def font(size):
    for path in ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                 "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def contact_sheet(icons, tabs):
    cell, cols = 150, 6
    names = list(icons) + ["tab_" + t for t in tabs]
    imgs = list(icons.values()) + list(tabs.values())
    rows = (len(imgs) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (0x3E, 0x35, 0x29, 255))
    d = ImageDraw.Draw(sheet)
    f = font(14)
    for i, (n, im) in enumerate(zip(names, imgs)):
        x, y = (i % cols) * cell, (i // cols) * cell
        d.rectangle([x + 4, y + 4, x + cell - 5, y + cell - 5], fill=(0x2B, 0x25, 0x1C, 255), outline=(0x18, 0x14, 0x0E, 255))
        big = im.resize((N * 3, N * 3), Image.NEAREST)
        sheet.alpha_composite(big, (x + (cell - N * 3) // 2, y + 12))
        sheet.alpha_composite(im, (x + 8, y + 8))
        tw = d.textlength(n, font=f)
        d.text((x + (cell - tw) / 2, y + cell - 30), n, font=f, fill=(255, 255, 0, 255))
    return sheet


# Mirrors com.runecraft.osrs.ui.SkillPanel (panel geometry in OSRS pixels) and SidePanel colours.
PANEL_W, PANEL_H = 241, 334
TAB_W, TAB_H, TAB_X0 = 33, 36, 5
CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H = 25, 37, 190, 261
TILE_W, TILE_H, GRID_X, GRID_Y = 62, 28, 2, 2
TOTAL_H = 26
YELLOW, GREEN, RED, ORANGE = (255, 255, 0), (0, 255, 0), (255, 0, 0), (255, 152, 31)
TOOLTIP_BG, TOOLTIP_BORDER = (255, 255, 160), (0, 0, 0)


K = 3            # mock-up supersampling: OSRS pixel -> K image pixels (downsampled at the end)


def mc_font():
    # Stand-in for the Minecraft font: digits ~7 OSRS px tall like MC's 8px glyph cell.
    return font(int(9.5 * K))


def text_w(d, text, f):
    return d.textlength(text, font=f) / K


def osrs_text(d, x, y, text, f, col, shadow=True):
    """Draws text at OSRS-pixel position (x, y) like DrawContext.drawTextWithShadow (shadow +1,+1)."""
    if shadow:
        d.text(((x + 1) * K, (y + 1) * K - 2 * K), text, font=f, fill=(0, 0, 0, 255))
    d.text((x * K, y * K - 2 * K), text, font=f, fill=col + (255,))


def frect(d, x0, y0, x1, y1, col):
    """DrawContext.fill(x0, y0, x1, y1) in OSRS pixels (x1/y1 exclusive)."""
    d.rectangle([round(x0 * K), round(y0 * K), round(x1 * K) - 1, round(y1 * K) - 1], fill=col)


def border(d, x, y, w, h, col):
    frect(d, x, y, x + w, y + 1, col)
    frect(d, x, y + h - 1, x + w, y + h, col)
    frect(d, x, y, x + 1, y + h, col)
    frect(d, x + w - 1, y, x + w, y + h, col)


EDGE, LIGHT, DARK = (0x18, 0x14, 0x0E), (0x5A, 0x50, 0x40), (0x2B, 0x25, 0x1C)


def inset(d, x, y, w, h):
    # Mirrors OsrsUi.inset.
    frect(d, x, y, x + w, y + h, DARK)
    frect(d, x, y, x + w, y + 1, EDGE)
    frect(d, x, y, x + 1, y + h, EDGE)
    frect(d, x, y + h - 1, x + w, y + h, LIGHT)
    frect(d, x + w - 1, y, x + w, y + h, LIGHT)


def button(d, x, y, w, h, active):
    # Mirrors OsrsUi.button.
    frect(d, x, y, x + w, y + h, (0x6B, 0x24, 0x1C) if active else (0x35, 0x2D, 0x22))
    hi = (0x8E, 0x3A, 0x2C) if active else (0x55, 0x4B, 0x3C)
    frect(d, x, y, x + w, y + 1, hi)
    frect(d, x, y, x + 1, y + h, hi)
    frect(d, x, y + h - 1, x + w, y + h, EDGE)
    frect(d, x + w - 1, y, x + w, y + h, EDGE)


def paste_icon(img, icon, x, y, size):
    img.alpha_composite(icon.resize((size * K, size * K), Image.NEAREST), (int(x * K), int(y * K)))


def tooltip(d, lines, mx, my, bound_w, bound_h, ox, oy, f):
    # Mirrors OsrsUi.tooltip (mouse coordinates relative to the content origin ox, oy).
    w = int(max(text_w(d, l, f) for l in lines)) + 6
    h = len(lines) * 10 + 3
    x = max(0, min(mx + 6, bound_w - w))
    y = my + 12
    if y + h > bound_h:
        y = max(0, my - h - 2)
    frect(d, ox + x, oy + y, ox + x + w, oy + y + h, TOOLTIP_BORDER)
    frect(d, ox + x + 1, oy + y + 1, ox + x + w - 1, oy + y + h - 1, TOOLTIP_BG)
    for i, l in enumerate(lines):
        osrs_text(d, ox + x + 3, oy + y + 2 + i * 10, l, f, (0, 0, 0), shadow=False)


LEVELS = {"attack": (99, 99), "hitpoints": (91, 99), "mining": (85, 85), "strength": (112, 99), "agility": (70, 70),
          "smithing": (72, 72), "defence": (99, 99), "herblore": (78, 78), "fishing": (82, 82), "ranged": (82, 75),
          "thieving": (74, 74), "cooking": (93, 93), "prayer": (52, 77), "crafting": (80, 80), "firemaking": (99, 99),
          "magic": (94, 94), "fletching": (90, 90), "woodcutting": (88, 88), "runecraft": (65, 65), "slayer": (87, 87),
          "farming": (83, 83), "construction": (75, 75), "hunter": (79, 79), "sailing": (41, 41)}


def mock_panel(icons, tabs, stone, hover="ranged"):
    """The side panel as SidePanel/SkillsTab draw it, at K image px per OSRS px.

    Returns the image and the hovered point (panel-local OSRS px) for drawing the cursor."""
    p = Image.new("RGBA", (PANEL_W * K, PANEL_H * K))
    st = stone.resize((64 * K, 64 * K), Image.NEAREST)
    for ty in range(0, PANEL_H * K, 64 * K):
        for tx in range(0, PANEL_W * K, 64 * K):
            p.alpha_composite(st, (tx, ty))
    d = ImageDraw.Draw(p)
    border(d, 0, 0, PANEL_W, PANEL_H, (0x10, 0x0C, 0x08))
    border(d, 1, 1, PANEL_W - 2, PANEL_H - 2, (0x5E, 0x54, 0x44))
    top_tabs = ["combat", "skills", "quests", "log", "gear", "prayer", "magic"]
    bottom = [None, None, None, "expand", None, None, "close"]
    for i in range(14):
        x = TAB_X0 + (i % 7) * TAB_W
        y = 0 if i < 7 else PANEL_H - TAB_H
        name = top_tabs[i] if i < 7 else bottom[i - 7]
        button(d, x, y + 2, TAB_W - 1, TAB_H - 4, name == "skills")
        if name:
            paste_icon(p, tabs[name], x + 4, y + 6, 24)
    cx, cy = CONTENT_X, CONTENT_Y
    border(d, cx - 1, cy - 1, CONTENT_W + 2, CONTENT_H + 2, EDGE)
    f = mc_font()
    hover_pt = None
    for i, s in enumerate(SKILLS):
        x = cx + GRID_X + (i % 3) * TILE_W
        y = cy + GRID_Y + (i // 3) * TILE_H
        inset(d, x, y, TILE_W - 2, TILE_H - 2)
        if s == hover:
            frect(d, x + 1, y + 1, x + TILE_W - 3, y + TILE_H - 3, (0x3A, 0x34, 0x2B))
            hover_pt = (x - cx + 34, y - cy + 10)
        paste_icon(p, icons[s], x + 3, y + 1, 24)
        cur, base = LEVELS[s]
        ccol = GREEN if cur > base else RED if cur < base else YELLOW
        osrs_text(d, x + 41 - text_w(d, str(cur), f) / 2, y + 3, str(cur), f, ccol)
        osrs_text(d, x + 44, y + 9, "/", f, YELLOW)
        osrs_text(d, x + 52 - text_w(d, str(base), f) / 2, y + 15, str(base), f, YELLOW)
    tx, ty, tw = cx + GRID_X, cy + GRID_Y + 8 * TILE_H, 3 * TILE_W - 2
    inset(d, tx, ty, tw, TOTAL_H)
    total = str(sum(b for _, b in LEVELS.values()))
    osrs_text(d, tx + tw / 2 - text_w(d, "Total level:", f) / 2, ty + 4, "Total level:", f, YELLOW)
    osrs_text(d, tx + tw / 2 - text_w(d, total, f) / 2, ty + 15, total, f, YELLOW)
    if hover_pt:
        # Ranged at 82/75 (boosted): the three-line OSRS tooltip.
        lines = ["Ranged XP: 1,234,567", "Next level at: 1,336,443", "Remaining XP: 101,876"]
        tooltip(d, lines, hover_pt[0], hover_pt[1], CONTENT_W, CONTENT_H, cx, cy, f)
        hover_pt = (cx + hover_pt[0], cy + hover_pt[1])
    return p, hover_pt


def fake_world(w, h, g):
    """A blocky Minecraft-ish landscape (GUI scale g) so the mock-up shows the panel over the world."""
    rnd = random.Random(7)
    img = Image.new("RGBA", (w, h), (120, 167, 255, 255))
    d = ImageDraw.Draw(img)
    for y in range(h // 2):
        t = y / (h / 2)
        d.line([(0, y), (w, y)], fill=(int(120 + 60 * t), int(167 + 40 * t), 255))
    horizon = int(h * 0.5)
    blk = 16 * g
    for i, x in enumerate(range(-blk, w + blk, blk)):
        top = horizon - int(3 * math.sin(i * 0.7) + 2 * math.sin(i * 0.23)) * blk // 3
        for y in range(top, h, blk):
            depth = (y - top) // blk
            if depth == 0:
                c = (98 + rnd.randint(-8, 8), 150 + rnd.randint(-8, 8), 60)
            elif depth < 3:
                c = (134 + rnd.randint(-8, 8), 96, 67)
            else:
                v = 120 + rnd.randint(-10, 10)
                c = (v, v, v)
            d.rectangle([x, y, x + blk - 1, y + blk - 1], fill=c, outline=tuple(int(v * 0.85) for v in c))
    for tx in (220, 760, 1210):
        top = horizon - 9 * blk // 2
        d.rectangle([tx, top + 2 * blk, tx + blk - 1, horizon + 4], fill=(102, 76, 46), outline=(80, 58, 34))
        for ly in range(3):
            for lx in range(-2, 3):
                if abs(lx) == 2 and ly == 0:
                    continue
                c = (58 + rnd.randint(-10, 10), 112 + rnd.randint(-12, 12), 38)
                d.rectangle([tx + lx * blk, top + ly * blk, tx + lx * blk + blk - 1, top + ly * blk + blk - 1], fill=c,
                            outline=(40, 84, 26))
    # Hotbar (182x22 GUI) and crosshair.
    hb_w, hb_h = 182 * g, 22 * g
    hx = (w - hb_w) // 2
    d.rectangle([hx, h - hb_h, hx + hb_w - 1, h - 1], fill=(40, 40, 40, 255), outline=(16, 16, 16))
    for i in range(9):
        d.rectangle([hx + (1 + i * 20) * g, h - hb_h + g, hx + (21 + i * 20) * g - 1, h - g - 1], outline=(140, 140, 140), width=g)
    d.rectangle([hx - g, h - hb_h - g, hx + 23 * g, h - 1], outline=(255, 255, 255), width=g)
    cx, cy = w // 2, h // 2
    d.rectangle([cx - 5 * g, cy - g // 2, cx + 5 * g, cy + g // 2], fill=(255, 255, 255))
    d.rectangle([cx - g // 2, cy - 5 * g, cx + g // 2, cy + 5 * g], fill=(255, 255, 255))
    return img


def cursor(img, x, y, s):
    pts = [(0, 0), (0, 16), (4, 12), (7, 19), (9, 18), (6, 11), (11, 11)]
    d = ImageDraw.Draw(img)
    d.polygon([(x + px * s, y + py * s) for px, py in pts], fill=(255, 255, 255), outline=(0, 0, 0))


def mockup(icons, tabs, stone):
    """1920x1080 at GUI scale 3, as HudOverlay + SidePanel lay it out."""
    w, h, g = 1920, 1080, 3
    world = fake_world(w, h, g)
    d = ImageDraw.Draw(world)
    f = font(8 * g)
    # HUD orbs top-right (HudOverlay: x = width - 92 GUI, 88x12 bars).
    ox = w - 92 * g
    for i, (label, col, frac) in enumerate((("HP 91/99", (176, 24, 24), 0.92), ("Pr 52/77", (32, 160, 192), 0.68))):
        y = (4 + i * 14) * g
        d.rectangle([ox, y, ox + 88 * g - 1, y + 12 * g - 1], fill=(32, 24, 16))
        d.rectangle([ox + g, y + g, ox + g + int(86 * g * frac) - 1, y + 11 * g - 1], fill=col)
        d.text((ox + 4 * g, y + g), label, font=f, fill=(0, 0, 0))
        d.text((ox + 3 * g, y), label, font=f, fill=(255, 255, 255))
    y = 32 * g
    d.rectangle([ox, y, ox + 88 * g - 1, y + 12 * g - 1], fill=(32, 24, 16))
    d.text((ox + 4 * g, y + g), "Cb 126  12.4M gp", font=f, fill=(0, 0, 0))
    d.text((ox + 3 * g, y), "Cb 126  12.4M gp", font=f, fill=(255, 215, 0))
    # XP drop rising on the right.
    world.alpha_composite(icons["ranged"].resize((10 * g, 10 * g), Image.NEAREST), (w - 46 * g, 84 * g))
    d.text((w - 33 * g, 84 * g), "+212", font=f, fill=(0, 0, 0))
    d.text((w - 34 * g, 83 * g), "+212", font=f, fill=(255, 255, 255))
    # Side panel: SkillPanel.pixelsPerUnit(1080) = 1.5 real px per OSRS px, anchored bottom-right.
    panel, hover = mock_panel(icons, tabs, stone)
    ppu = max(1.0, math.floor(h * 0.55 / PANEL_H * 2) / 2)
    pw, ph = int(PANEL_W * ppu), int(PANEL_H * ppu)
    world.alpha_composite(panel.resize((pw, ph), Image.LANCZOS), (w - pw, h - ph))
    if hover:
        cursor(world, int(w - pw + hover[0] * ppu), int(h - ph + hover[1] * ppu), 2)
    cap = "Hold Left Alt to use the panel  -  K shows/hides it  -  J opens the full interface"
    fc = font(22)
    d.rectangle([10, 10, 30 + d.textlength(cap, font=fc), 44], fill=(0, 0, 0, 255))
    d.text((20, 14), cap, font=fc, fill=(255, 255, 0))
    return world


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(PREVIEWS, exist_ok=True)
    icons = {}
    for name in SKILLS:
        icons[name] = draw(SKILL_ICONS[name])
        icons[name].save(os.path.join(OUT, name + ".png"))
    tabs = gen_tab_icons()
    for name, im in tabs.items():
        im.save(os.path.join(OUT, "tab_" + name + ".png"))
    stone = gen_stone()
    stone.save(os.path.join(OUT, "stone.png"))
    contact_sheet(icons, tabs).save(os.path.join(PREVIEWS, "skill_icons.png"))
    mockup(icons, tabs, stone).save(os.path.join(PREVIEWS, "skills_panel.png"))
    print("wrote %d skill icons, %d tab icons, stone texture and previews" % (len(icons), len(tabs)))


if __name__ == "__main__":
    main()
