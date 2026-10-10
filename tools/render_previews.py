#!/usr/bin/env python3
"""Render contact sheets of generated assets into docs/previews/.

  items_sheet.png     representative inventory icons at 4x
  monsters_sheet.png  common monsters (ModelSpec boxes + generated textures, isometric)
  bosses_sheet.png    bosses
  npcs_sheet.png      NPC biped skins
  blocks_sheet.png    RuneCraft block textures as isometric cubes

Reads build/catalog.json (tools/run_core_tests.sh) and the textures written by tools/gen_assets.py.
Usage: python3 tools/render_previews.py [sheet ...]   (default: all sheets)
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "build", "catalog.json")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "runecraft")
OUT = os.path.join(ROOT, "docs", "previews")
BG = (0x3E, 0x35, 0x29)
TEXT = (255, 255, 0)


# ----------------------------------------------------------------------------------------------
# Minimal software renderer for ModelSpec models (mirrors ModelPart/Cuboid in Minecraft 1.20.1)
# ----------------------------------------------------------------------------------------------

def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def mat_vec(m, v):
    x, y, z = v
    return (m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3],
            m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3],
            m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3])


def translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


IDENT = translate(0, 0, 0)


def part_matrices(model):
    parts = {p["name"]: p for p in model["parts"]}
    cache = {}

    def get(name):
        if name in cache:
            return cache[name]
        p = parts[name]
        parent = get(p["parent"]) if p.get("parent") else IDENT
        m = mat_mul(parent, translate(p.get("px", 0), p.get("py", 0), p.get("pz", 0)))
        # Quaternionf.rotationZYX(roll, yaw, pitch) == Rz * Ry * Rx
        m = mat_mul(m, mat_mul(rot_z(p.get("roll", 0)), mat_mul(rot_y(p.get("yaw", 0)), rot_x(p.get("pitch", 0)))))
        cache[name] = m
        return m

    for n in parts:
        get(n)
    return cache


def box_quads(box):
    """[(corner_fn(i, j) -> 4 corners, tex rect)] for the six faces, using Minecraft's cuboid UV layout."""
    x0, y0, z0 = box["x"] - box.get("dil", 0), box["y"] - box.get("dil", 0), box["z"] - box.get("dil", 0)
    w, h, d = box["w"], box["h"], box["d"]
    dl = box.get("dil", 0)
    sx, sy, sz = (w + 2 * dl) / max(w, 1e-6), (h + 2 * dl) / max(h, 1e-6), (d + 2 * dl) / max(d, 1e-6)
    u, v = box["u"], box["v"]

    def P(x, y, z):
        return (x0 + x * sx, y0 + y * sy, z0 + z * sz)

    faces = []
    # texel span [i0, i1] x [j0, j1] of a face -> its four 3D corners (Minecraft cuboid UV layout)
    faces.append(((u + d, v + d, w, h), lambda i0, i1, j0, j1: [P(i0, j0, 0), P(i1, j0, 0), P(i1, j1, 0), P(i0, j1, 0)], (0, 0, -1)))
    faces.append(((u + 2 * d + w, v + d, w, h), lambda i0, i1, j0, j1: [P(w - i0, j0, d), P(w - i1, j0, d), P(w - i1, j1, d), P(w - i0, j1, d)], (0, 0, 1)))
    faces.append(((u, v + d, d, h), lambda i0, i1, j0, j1: [P(0, j0, d - i0), P(0, j0, d - i1), P(0, j1, d - i1), P(0, j1, d - i0)], (-1, 0, 0)))
    faces.append(((u + d + w, v + d, d, h), lambda i0, i1, j0, j1: [P(w, j0, i0), P(w, j0, i1), P(w, j1, i1), P(w, j1, i0)], (1, 0, 0)))
    faces.append(((u + d, v, w, d), lambda i0, i1, j0, j1: [P(i0, 0, d - j0), P(i1, 0, d - j0), P(i1, 0, d - j1), P(i0, 0, d - j1)], (0, -1, 0)))
    faces.append(((u + d + w, v, w, d), lambda i0, i1, j0, j1: [P(i0, h, d - j0), P(i1, h, d - j0), P(i1, h, d - j1), P(i0, h, d - j1)], (0, 1, 0)))
    return faces


def render_model(model, tex, size=160, yaw=-35.0, pitch=22.0, glow=None):
    """Isometric-ish render: the creature faces the viewer, turned by `yaw` degrees."""
    mats = part_matrices(model)
    k = max(1, tex.width // model["tex_w"])
    sub = max(1, k // 2) if size <= 200 else k
    step = k // sub
    gpx = glow.load() if glow is not None else None
    view = mat_mul(rot_x(math.radians(-pitch)), rot_y(math.radians(yaw)))
    tris = []
    texpx = tex.load()
    tw, th = tex.size
    for p in model["parts"]:
        m = mat_mul(view, mats[p["name"]])
        for box in p["boxes"]:
            for (fx, fy, fw, fh), corners, normal in box_quads(box):
                n = mat_vec(m, normal)
                n0 = mat_vec(m, (0, 0, 0))
                nz = n[2] - n0[2]
                if nz > 1e-6:  # facing away (viewer looks along +Z)
                    continue
                for j in range(fh * sub):
                    for i in range(fw * sub):
                        tx, ty = fx * k + i * step + step // 2, fy * k + j * step + step // 2
                        if not (0 <= tx < tw and 0 <= ty < th):
                            continue
                        c = texpx[tx, ty]
                        if c[3] < 10:
                            continue
                        if gpx is not None:
                            g = gpx[tx, ty]
                            if g[3]:
                                c = tuple(min(255, c[n] + g[n]) for n in range(3)) + (255,)
                        pts = [mat_vec(m, q) for q in corners(i / sub, (i + 1) / sub, j / sub, (j + 1) / sub)]
                        depth = sum(q[2] for q in pts) / 4
                        tris.append((depth, [(q[0], q[1]) for q in pts], c[:3]))
    if not tris:
        return Image.new("RGBA", (size, size), (0, 0, 0, 0))
    xs = [x for t in tris for x, _ in t[1]]
    ys = [y for t in tris for _, y in t[1]]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    span = max(maxx - minx, maxy - miny, 1e-6)
    ss = 3
    k = (size * ss - 8 * ss) / span
    img = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    ox = (size * ss - (maxx - minx) * k) / 2
    oy = (size * ss - (maxy - miny) * k) / 2
    for depth, pts, c in sorted(tris, key=lambda t: -t[0]):
        poly = [((x - minx) * k + ox, (y - miny) * k + oy) for x, y in pts]
        dr.polygon(poly, fill=c + (255,), outline=c + (255,))
    return img.resize((size, size), Image.LANCZOS)


# ----------------------------------------------------------------------------------------------
# Sheets
# ----------------------------------------------------------------------------------------------

def grid(cells, cols, cw, ch, title):
    rows = (len(cells) + cols - 1) // cols
    img = Image.new("RGB", (cols * cw, rows * ch + 20), BG)
    d = ImageDraw.Draw(img)
    d.text((6, 4), title, fill=(255, 255, 255))
    for n, (label, pic) in enumerate(cells):
        x, y = (n % cols) * cw, (n // cols) * ch + 20
        if pic is not None:
            img.paste(pic, (x + (cw - pic.width) // 2, y + 2), pic)
        else:
            d.text((x + 6, y + ch // 2), "missing", fill=(255, 80, 80))
        d.text((x + 3, y + ch - 13), label[: max(4, cw // 6)], fill=TEXT)
    return img


ITEMS = """bronze_scimitar iron_scimitar steel_scimitar black_scimitar mithril_scimitar adamant_scimitar rune_scimitar dragon_scimitar
bronze_sword iron_longsword steel_2h_sword rune_longsword dragon_dagger abyssal_whip armadyl_godsword rune_battleaxe
bronze_full_helm iron_platebody steel_platelegs black_kiteshield mithril_sq_shield adamant_platebody rune_full_helm rune_platelegs
dragon_med_helm rune_kiteshield rune_sq_shield dragon_platelegs green_dhide_body leather_body wizard_hat rune_pickaxe
shortbow oak_longbow magic_shortbow rune_arrow bronze_arrow air_rune water_rune earth_rune fire_rune mind_rune body_rune
cosmic_rune chaos_rune nature_rune law_rune death_rune blood_rune soul_rune astral_rune wrath_rune attack_potion
strength_potion prayer_potion super_restore saradomin_brew antipoison vial_of_water lobster swordfish shark
monkfish raw_lobster cake bread meat_pie copper_ore iron_ore coal mithril_ore runite_ore bronze_bar iron_bar
steel_bar gold_bar runite_bar logs oak_logs willow_logs maple_logs yew_logs magic_logs grimy_ranarr_weed
ranarr_weed grimy_guam_leaf guam_leaf uncut_sapphire sapphire uncut_diamond diamond dragonstone coins bones big_bones
dragon_bones cowhide green_dragonhide gold_ring ruby_ring sapphire_amulet amulet_of_glory diamond_necklace
dragonstone_bracelet rune_essence fishing_rod bronze_axe hammer tinderbox""".split()


def items_sheet(cat):
    ids = [i for i in ITEMS if os.path.exists(os.path.join(ASSETS, "textures", "item", i + ".png"))]
    cells = []
    for iid in ids:
        t = Image.open(os.path.join(ASSETS, "textures", "item", iid + ".png")).convert("RGBA")
        cells.append((iid, t.resize((64, 64), Image.NEAREST)))
    return grid(cells, 12, 76, 84, "RuneCraft item icons (4x)")


def model_cells(cat, ids):
    models = {m["id"]: m for m in cat["models"]}
    cells = []
    for label, mid in ids:
        m = models.get(mid)
        path = os.path.join(ASSETS, "textures", "entity", "model", mid + ".png")
        if m is None or not os.path.exists(path):
            cells.append((label, None))
            continue
        gpath = path[:-4] + "_glow.png"
        glow = Image.open(gpath).convert("RGBA") if os.path.exists(gpath) else None
        cells.append((label, render_model(m, Image.open(path).convert("RGBA"), 150, glow=glow)))
    return cells


MONSTERS = ["man", "woman", "goblin", "guard", "chicken", "cow", "giant_rat", "giant_spider", "imp", "dark_wizard",
            "barbarian", "al_kharid_warrior", "hill_giant", "moss_giant", "ice_giant", "skeleton", "zombie", "ghost",
            "lesser_demon", "greater_demon", "black_demon", "hellhound", "green_dragon", "red_dragon",
            "black_dragon", "steel_dragon", "rune_dragon", "baby_blue_dragon", "abyssal_demon", "gargoyle", "nechryael",
            "bloodveld", "aberrant_spectre", "dagannoth", "kalphite", "cave_crawler", "crawling_hand", "banshee",
            "rock_crab", "sand_crab", "ankou", "cockatrice", "basilisk", "kurask", "turoth", "jelly", "spitting_wyvern",
            "tzhaar_ket", "ghoul", "sheep", "wolf", "scorpion", "hobgoblin", "fire_giant"]

BOSSES = ["ahrim", "dharok", "guthan", "karil", "torag", "verac", "king_black_dragon", "giant_mole", "kalphite_queen",
          "zulrah", "vorkath", "general_graardor", "kril_tsutsaroth", "commander_zilyana", "kreearra",
          "corporeal_beast", "tztok_jad", "obor", "bryophyta", "cerberus", "callisto", "venenatis", "vetion",
          "dagannoth_rex", "kraken", "abyssal_sire", "alchemical_hydra", "scorpia", "chaos_elemental", "nex",
          "count_draynor", "delrith"]


def model_for(cat, cid):
    for kind in ("monsters", "bosses"):
        for m in cat[kind]:
            if m["id"] == cid and m.get("model"):
                return m["model"]
    return cid


def monsters_sheet(cat):
    return grid(model_cells(cat, [(m, model_for(cat, m)) for m in MONSTERS]), 9, 156, 168, "Monsters (ModelSpec renders)")


def bosses_sheet(cat):
    return grid(model_cells(cat, [(m, model_for(cat, m)) for m in BOSSES]), 8, 156, 168, "Bosses (ModelSpec renders)")


def biped_front(skin, scale=4):
    """Front view of a 64x64 biped skin: head, body, arms, legs."""
    img = Image.new("RGBA", (16, 32), (0, 0, 0, 0))

    def blit(sx, sy, w, h, dx, dy):
        img.paste(skin.crop((sx, sy, sx + w, sy + h)), (dx, dy))

    blit(8, 8, 8, 8, 4, 0)       # head
    blit(20, 20, 8, 12, 4, 8)    # body
    blit(44, 20, 4, 12, 0, 8)    # right arm
    blit(44, 20, 4, 12, 12, 8) if skin.height < 64 else blit(36, 52, 4, 12, 12, 8)
    blit(4, 20, 4, 12, 4, 20)
    blit(4, 20, 4, 12, 8, 20) if skin.height < 64 else blit(20, 52, 4, 12, 8, 20)
    hat = skin.crop((40, 8, 48, 16))
    img.alpha_composite(hat, (4, 0))
    return img.resize((16 * scale, 32 * scale), Image.NEAREST)


NPCS = ["hans", "duke_horacio", "bob", "shopkeeper", "banker", "cook", "father_aereck", "aubury", "zaff", "horvik",
        "thessalia", "gertrude", "romeo", "juliet", "sir_amik_varze", "wise_old_man", "doric", "ned", "aggie",
        "fred_the_farmer", "king_roald", "ellis", "lowe", "gerrant", "wizard_traiborn", "sedridor", "brian", "gaius"]


def npcs_sheet(cat):
    cells = []
    ids = [n for n in NPCS if os.path.exists(os.path.join(ASSETS, "textures", "entity", "npc", n + ".png"))]
    if len(ids) < 24:
        for n in cat["npcs"]:
            if n["id"] not in ids and os.path.exists(os.path.join(ASSETS, "textures", "entity", "npc", n["id"] + ".png")):
                ids.append(n["id"])
            if len(ids) >= 32:
                break
    for n in ids[:32]:
        skin = Image.open(os.path.join(ASSETS, "textures", "entity", "npc", n + ".png")).convert("RGBA")
        cells.append((n, biped_front(skin, 3)))
    return grid(cells, 8, 100, 116, "NPC skins (front view)")


def iso_cube(top, side, front, size=72):
    s = 16
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    k = size / (2 * s) * 0.98
    cx = size / 2
    tex = {"top": top.load(), "left": side.load(), "right": front.load()}
    dr = ImageDraw.Draw(img)
    for face in ("top", "left", "right"):
        px = tex[face]
        for j in range(s):
            for i in range(s):
                c = px[i, j]
                if c[3] < 10:
                    continue
                f = {"top": 1.0, "left": 0.82, "right": 0.66}[face]
                col = tuple(int(c[n] * f) for n in range(3)) + (255,)
                if face == "top":
                    def P(a, b):
                        return (cx + (a - b) * k, (a + b) * k * 0.5)
                    pts = [P(i, j), P(i + 1, j), P(i + 1, j + 1), P(i, j + 1)]
                elif face == "left":
                    def P(a, b):
                        return (cx - s * k + a * k, s * k * 0.5 + a * k * 0.5 + b * k)
                    pts = [P(i, j), P(i + 1, j), P(i + 1, j + 1), P(i, j + 1)]
                else:
                    def P(a, b):
                        return (cx + a * k, s * k - a * k * 0.5 + b * k)
                    pts = [P(i, j), P(i + 1, j), P(i + 1, j + 1), P(i, j + 1)]
                dr.polygon(pts, fill=col, outline=col)
    return img


def block_textures(bid):
    """(top, side, front) texture images for a block id, from its block model json."""
    mp = os.path.join(ASSETS, "models", "block", bid + ".json")
    if not os.path.exists(mp):
        return None
    with open(mp) as f:
        model = json.load(f)
    t = model.get("textures", {})

    def load(key_order):
        for k in key_order:
            ref = t.get(k)
            if isinstance(ref, str) and ref.startswith("runecraft:"):
                p = os.path.join(ASSETS, "textures", ref.split(":", 1)[1] + ".png")
                if os.path.exists(p):
                    im = Image.open(p).convert("RGBA")
                    return im.crop((0, 0, 16, 16)) if im.size != (16, 16) else im
        return None

    top = load(["top", "up", "end", "all", "texture", "cross", "particle"])
    side = load(["side", "west", "east", "all", "texture", "cross", "particle"])
    front = load(["front", "north", "south", "side", "all", "texture", "cross", "particle"])
    if top is None and side is None:
        return None
    return top or side, side or top, front or side or top


BLOCKS = ["bank_booth", "furnace", "anvil", "range", "cooking_range", "spinning_wheel", "altar", "chaos_altar",
          "air_altar", "water_altar", "earth_altar", "fire_altar", "nature_altar", "law_altar", "death_altar",
          "cosmic_altar", "cake_stall", "silk_stall", "gem_stall", "fur_stall", "silver_stall", "spice_stall",
          "copper", "tin", "iron", "coal", "mithril", "adamantite", "runite", "tree_log", "oak_log", "willow_log",
          "yew_log", "magic_log", "pottery_wheel", "loom", "sink", "agility_log_balance", "agility_rope_swing",
          "agility_net", "agility_wall", "agility_pipe", "dairy_churn", "portal", "poll_booth", "deposit_box"]


def blocks_sheet(cat):
    ids = [b for b in BLOCKS if block_textures(b)]
    for b in cat["blocks"]:
        if len(ids) >= 64:
            break
        if b["id"] not in ids and block_textures(b["id"]):
            ids.append(b["id"])
    cells = []
    for b in ids[:64]:
        top, side, front = block_textures(b)
        cells.append((b, iso_cube(top, side, front)))
    return grid(cells, 10, 84, 92, "Blocks")


SHEETS = {"items": items_sheet, "monsters": monsters_sheet, "bosses": bosses_sheet, "npcs": npcs_sheet,
          "blocks": blocks_sheet}


def model_views(cat, mid, out, size=360):
    """Front / three-quarter / side / back views of one model, for close inspection."""
    models = {m["id"]: m for m in cat["models"]}
    m = models[mid]
    path = os.path.join(ASSETS, "textures", "entity", "model", mid + ".png")
    tex = Image.open(path).convert("RGBA")
    gpath = path[:-4] + "_glow.png"
    glow = Image.open(gpath).convert("RGBA") if os.path.exists(gpath) else None
    views = [("front", 0, 8), ("3/4", -35, 22), ("side", -90, 5), ("back", 180, 15), ("top", -20, 70)]
    img = Image.new("RGB", (size * len(views), size + 20), (24, 18, 20))
    d = ImageDraw.Draw(img)
    for n, (label, yaw, pitch) in enumerate(views):
        pic = render_model(m, tex, size, yaw=yaw, pitch=pitch, glow=glow)
        img.paste(pic, (n * size, 0), pic)
        d.text((n * size + 6, size + 4), f"{mid} {label}", fill=TEXT)
    img.save(out)


def main(argv):
    with open(CATALOG) as f:
        cat = json.load(f)
    if argv and argv[0] == "--model":
        for mid in argv[2:]:
            model_views(cat, mid, argv[1].replace("%", mid))
        return
    os.makedirs(OUT, exist_ok=True)
    for name in (argv or list(SHEETS)):
        img = SHEETS[name](cat)
        path = os.path.join(OUT, f"{name}_sheet.png")
        img.save(path, optimize=True)
        print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main(sys.argv[1:])
