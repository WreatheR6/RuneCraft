"""Hand-painted-look textures for ModelSpec entity models (used by tools/gen_assets.py).

Every box face of a model is painted per pixel at ``R`` texture pixels per model pixel (R = 4/2/1 depending
on the model's UV size; Minecraft normalises UVs by the declared texture size, so a 2x or 4x image simply
gives more detail). Each part gets a *material* (explicit ``material`` from the spec, or inferred from its
pattern, its name and the model id), lit with face light, ambient occlusion towards the bottom, bevelled
edges and coherent 3D noise so neighbouring faces of a box agree. Emissive details (``glow``: lava cracks,
eyes, maws, runes, whole parts) are also written to a second ``_glow`` image that the client renders
full-bright.

Requires numpy.
"""
import math
import zlib

import numpy as np
from PIL import Image

FACE_LIGHT = {"top": 1.20, "front": 1.0, "right": 0.86, "left": 0.86, "back": 0.78, "bottom": 0.58}
SIDE_FACES = ("front", "back", "right", "left")
LIMB_ROLES = {"LEG_FRONT_LEFT", "LEG_FRONT_RIGHT", "LEG_BACK_LEFT", "LEG_BACK_RIGHT", "LEG_EXTRA"}


def seed_of(*keys):
    return zlib.crc32("|".join(map(str, keys)).encode()) & 0x7FFFFFFF


def rgb(c):
    if c is None:
        return (128, 128, 128)
    if isinstance(c, int):
        return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
    return tuple(c[:3])


def lum(c):
    r, g, b = rgb(c)
    return (r * 299 + g * 587 + b * 114) / 1000


def sat(c):
    r, g, b = rgb(c)
    mx, mn = max(r, g, b), min(r, g, b)
    return 0.0 if mx == 0 else (mx - mn) / mx


def cdist(a, b):
    a, b = rgb(a), rgb(b)
    return math.sqrt(sum((a[i] - b[i]) ** 2 for i in range(3)))


# ----------------------------------------------------------------------------------------------
# Noise
# ----------------------------------------------------------------------------------------------

def _hash(ix, iy, iz, seed):
    h = (ix * 374761393 + iy * 668265263 + iz * 1274126177 + seed * 144269504) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1103515245) & 0xFFFFFFFF
    h = (h ^ (h >> 16)) & 0xFFFF
    return h / 65535.0


def cell(P, size, seed):
    """Blocky noise: one random value per cube of `size` model pixels."""
    q = [np.floor(P[k] / size).astype(np.int64) for k in range(3)]
    return _hash(q[0], q[1], q[2], seed)


def vnoise(P, size, seed):
    """Smooth value noise with feature size `size` model pixels."""
    p = [P[k] / size for k in range(3)]
    i0 = [np.floor(x).astype(np.int64) for x in p]
    f = [x - np.floor(x) for x in p]
    f = [x * x * (3 - 2 * x) for x in f]
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (f[0] if dx else 1 - f[0]) * (f[1] if dy else 1 - f[1]) * (f[2] if dz else 1 - f[2])
                out = out + w * _hash(i0[0] + dx, i0[1] + dy, i0[2] + dz, seed)
    return out


def fbm(P, size, seed, octaves=3):
    tot, amp, norm = 0, 1.0, 0
    for o in range(octaves):
        tot = tot + amp * vnoise(P, size / (2 ** o), seed + o * 101)
        norm += amp
        amp *= 0.5
    return tot / norm


# ----------------------------------------------------------------------------------------------
# Colour helpers on arrays
# ----------------------------------------------------------------------------------------------

def col_arr(c, shape):
    a = np.empty(shape + (3,), dtype=np.float64)
    a[...] = rgb(c)
    return a


def tone(col, v):
    """v in about [-1, 1]: darken (v<0) multiplicatively, brighten (v>0) towards white."""
    v = v[..., None] if np.ndim(v) else v
    dark = col * (1 + np.minimum(v, 0))
    light = col + (255 - col) * np.maximum(v, 0) * 0.85
    return np.where(np.asarray(v) < 0, dark, light) if np.ndim(v) else (dark if v < 0 else light)


def blend(a, b, t):
    t = t[..., None] if np.ndim(t) else t
    return a + (b - a) * t


def shade_c(c, f):
    r, g, b = rgb(c)
    return (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)))


def mix_c(a, b, t):
    a, b = rgb(a), rgb(b)
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# ----------------------------------------------------------------------------------------------
# Material inference
# ----------------------------------------------------------------------------------------------

NAME_MATERIALS = [
    (("horn", "claw", "tooth", "teeth", "tusk", "spike", "fang", "talon", "nail", "beak", "toe_", "hoof", "hooves",
      "spine", "barb", "stinger", "pincer_tip"), "horn"),
    (("bone", "skull", "rib", "pelvis", "femur", "jawbone"), "bone"),
    (("helm", "armour", "armor", "plate", "pauldron", "greave", "gauntlet", "visor", "chest", "cuirass", "tasset",
      "sabaton", "vambrace", "shield", "blade", "sword", "axehead", "mace", "spearhead", "flail", "hammerhead",
      "crown", "buckle", "rivet", "chainmail"), "metal"),
    (("robe", "cloak", "cape", "skirt", "hood", "loin", "cloth", "sash", "tabard", "apron", "shirt", "sleeve",
      "trouser", "dress", "scarf", "hat", "rags", "kilt", "wrap", "banner", "veil", "shawl"), "cloth"),
    (("belt", "strap", "boot", "glove", "quiver", "pouch", "bracer", "sandal", "shoe"), "leather"),
    (("hair", "mane", "beard", "moustache", "fringe", "ponytail", "plait", "brow_hair"), "hair"),
    (("shell", "carapace", "chitin", "mandible", "segment"), "chitin"),
    (("feather", "plume", "crest_f", "tailfeather"), "feathers"),
    (("club", "staff", "shaft", "handle", "bow", "log", "branch", "haft", "plank", "stick"), "wood"),
    (("wing",), "membrane"),
    (("flame", "fire", "lava", "magma", "ember"), "lava"),
    (("slime", "jelly", "goo", "ooze"), "slime"),
    (("eye", "orb", "core", "crystal", "gem", "glow"), "energy"),
    (("moss", "vine", "leaf", "leaves", "fern"), "moss"),
    (("tongue", "flesh", "gum", "brain"), "flesh"),
    (("cow_patch", "patch"), "skin"),
]

ID_MATERIALS = [
    (("tztok", "tzhaar", "jad", "tz_", "yt_", "ket_zek", "tok_xil", "jal_", "tzkal", "zuk", "obsidian", "golem",
      "slagilith", "rock_", "elemental", "zalcano", "ancestral_glyph", "statue", "gargoyle"), "rock"),
    (("dragon", "wyvern", "drake", "wyrm", "lizard", "snake", "basilisk", "hydra", "zulrah", "vorkath", "galvek",
      "elvarg", "naga", "nagua", "muttadile", "cockatrice", "cockathrice", "dino", "lizardman", "crocodile",
      "snakeling", "serpent", "sea_snake", "amoxliatl", "hueycoatl", "olm"), "scales"),
    (("spider", "scorpion", "scorpia", "kalphite", "crab", "lobster", "bug", "scarab", "araxyte", "araxxor",
      "kephri", "sarachnis", "spindel", "venenatis", "vespula", "vespine", "nylocas", "cave_crawler",
      "chasm_crawler", "kraken"), "chitin"),
    (("bear", "wolf", "dog", "hound", "rat", "yak", "jaguar", "monkey", "mole", "kebbit", "callisto", "artio",
      "sheep", "cerberus", "baboon", "werewolf", "cow", "pig", "minotaur", "suqah", "catablepon", "terror_dog",
      "bloodhound", "beaver", "squirrel", "chinchompa", "fox", "boar", "sourhog", "ba_ba", "baba"), "fur"),
    (("ghost", "spectre", "banshee", "shade", "soul", "ankou", "spirit", "wraith", "phantom", "muspah",
      "revenant", "fear_reaper", "husk", "shadow"), "ghost"),
    (("jelly", "slime", "blob", "ooze", "bloat", "spawn"), "slime"),
    (("skeleton", "skeletal", "bone", "vetion", "calvarion", "boneguard", "mystic"), "bone"),
    (("ice", "frost", "glacies", "icefiend", "titan_ice"), "ice"),
    (("moss", "bryophyta", "tree", "tangle", "hespori", "zygomite", "tree_spirit"), "moss"),
    (("aviansie", "kree", "kreearra", "bird", "chicken", "roc", "terrorbird", "skree", "kilisa", "geerin",
      "phoenix", "heron", "chompy", "firebird", "penguin", "duck", "seagull"), "feathers"),
    (("bloodveld", "nechryael", "abyssal", "flesh", "maiden", "sire", "horror", "abomination", "leech"), "flesh"),
    (("fiend", "pyre", "fire", "lava", "infernal", "muspah_fire"), "lava"),
]


def infer_material(model_id, part):
    explicit = part.get("material")
    if explicit:
        return explicit
    pattern = (part.get("pattern") or "none").lower()
    name = (part.get("name") or "").lower()
    role = part.get("role") or ""
    if pattern == "glow":
        return "energy"
    if role in ("WING_LEFT", "WING_RIGHT"):
        return "membrane" if not any(k in model_id for k in ("aviansie", "kree", "bird", "roc", "skree", "kilisa",
                                                             "geerin", "phoenix", "chicken")) else "feathers"
    for keys, mat in NAME_MATERIALS:
        if any(k in name for k in keys):
            return mat
    if pattern == "scales":
        return "scales"
    if pattern == "plates":
        return "chitin" if any(k in model_id for k in ("kalphite", "scarab", "crab", "spider", "scorp")) else "plates"
    mid = model_id.lower()
    for keys, mat in ID_MATERIALS:
        if any(k in mid for k in keys):
            return mat
    return "hide"


# ----------------------------------------------------------------------------------------------
# Materials: (ctx) -> (colour array HxWx3, glow array or None)
# ----------------------------------------------------------------------------------------------

class Ctx:
    __slots__ = ("P", "face", "s", "t", "fw", "fh", "dims", "base", "accent", "seed", "shape", "R", "long_axis",
                 "role", "loc")


def m_hide(c):
    n = fbm(c.P, 2.5, c.seed) - 0.5
    blk = cell(c.P, 0.5, c.seed + 7) - 0.5
    col = tone(col_arr(c.base, c.shape), n * 0.42 + blk * 0.10)
    return col, None


def m_skin(c):
    n = fbm(c.P, 3.0, c.seed) - 0.5
    blk = cell(c.P, 0.5, c.seed + 7) - 0.5
    return tone(col_arr(c.base, c.shape), n * 0.18 + blk * 0.05), None


def m_rock(c):
    """Mottled igneous rock: blocky dark/base/bright patches (TzHaar, golems, Jad)."""
    base = col_arr(c.base, c.shape)
    dark = col_arr(mix_c(c.base, (14, 8, 10), 0.78), c.shape)
    mid_dark = col_arr(mix_c(c.base, (20, 10, 12), 0.45), c.shape)
    bright = col_arr(mix_c(c.base, (255, 120, 90), 0.18), c.shape)
    a = cell(c.P, 1.5, c.seed)
    b = cell(c.P + 0.75, 1.0, c.seed + 3)
    fine = cell(c.P, 0.5, c.seed + 9) - 0.5
    v = a * 0.6 + b * 0.4
    col = np.where((v < 0.36)[..., None], dark, np.where((v < 0.52)[..., None], mid_dark,
                                                          np.where((v > 0.80)[..., None], bright, base)))
    return tone(col, fine * 0.16), None


def m_scales(c):
    base = col_arr(c.base, c.shape)
    X = c.P[0] + c.P[2]
    Y = c.P[1]
    row = np.floor(Y / 1.0)
    off = np.where(row % 2 == 0, 0.0, 0.75)
    u = ((X + off) / 1.5) % 1.0
    v = (Y / 1.0) % 1.0
    # each scale: lighter upper-centre, dark lower rim
    d = (u - 0.5) ** 2 * 1.6 + (v - 0.35) ** 2
    shade = np.where(v > 0.78, -0.32, np.where(d < 0.08, 0.20, -0.04))
    n = fbm(c.P, 4, c.seed) - 0.5
    col = tone(base, shade + n * 0.25)
    if c.face == "bottom":
        belly = col_arr(mix_c(c.base, c.accent, 0.6), c.shape)
        stripe = np.where((c.P[2] % 2.0) < 0.5, -0.25, 0.08)
        col = tone(belly, stripe + n * 0.15)
    return col, None


def m_plates(c):
    base = col_arr(c.base, c.shape)
    t = c.P[c.long_axis]
    s = t % 3.0
    shade = np.where(s > 2.5, -0.38, np.where(s < 0.6, 0.22, 0.0))
    n = fbm(c.P, 3, c.seed) - 0.5
    col = tone(base, shade + n * 0.2)
    if c.face == "bottom":
        col = tone(col_arr(mix_c(c.base, c.accent, 0.55), c.shape), shade * 0.8 + n * 0.15)
    return col, None


def m_chitin(c):
    base = col_arr(c.base, c.shape)
    t = c.P[c.long_axis]
    seg = (t % 2.5)
    shade = np.where(seg > 2.1, -0.45, 0.0)
    gloss = np.exp(-((c.s - 0.3) ** 2) / 0.01) * 0.35 if c.face in SIDE_FACES else 0.0
    n = fbm(c.P, 3, c.seed) - 0.5
    return tone(base, shade + gloss + n * 0.18), None


def m_fur(c):
    base = col_arr(c.base, c.shape)
    Ps = np.stack([c.P[0] * 2.2, c.P[1] * 0.45, c.P[2] * 2.2])
    n = vnoise(Ps, 1.0, c.seed) - 0.5
    n2 = cell(np.stack([c.P[0], c.P[1] * 0.3, c.P[2]]), 0.5, c.seed + 5) - 0.5
    shade = n * 0.55 + n2 * 0.18
    if c.face in SIDE_FACES:
        shade = shade - np.clip(c.t - 0.75, 0, 1) * 0.5
    return tone(base, shade), None


def m_hair(c):
    base = col_arr(c.base, c.shape)
    Ps = np.stack([c.P[0] * 3.5, c.P[1] * 0.35, c.P[2] * 3.5])
    n = vnoise(Ps, 1.0, c.seed) - 0.5
    return tone(base, n * 0.6), None


def m_feathers(c):
    base = col_arr(c.base, c.shape)
    X = c.P[0] + c.P[2]
    Y = c.P[1]
    row = np.floor(Y / 1.5)
    off = np.where(row % 2 == 0, 0.0, 0.9)
    u = ((X + off) / 1.8) % 1.0
    v = (Y / 1.5) % 1.0
    shade = np.where(v > 0.75, -0.35, 0.1 - v * 0.25) + np.where(np.abs(u - 0.5) < 0.06, -0.15, 0)
    n = fbm(c.P, 4, c.seed) - 0.5
    return tone(base, shade + n * 0.2), None


def m_cloth(c):
    base = col_arr(c.base, c.shape)
    a = c.P[0] + c.P[2]
    n = fbm(c.P, 3, c.seed) - 0.5
    folds = np.sin(a * 1.6 + n * 3.0) * 0.12
    weave = (cell(c.P, 0.25, c.seed + 2) - 0.5) * 0.06
    shade = folds + weave + n * 0.12
    if c.face in SIDE_FACES:
        shade = shade - np.where(c.t > 1 - 0.6 / max(1, c.fh), 0.22, 0)
    return tone(base, shade), None


def m_leather(c):
    base = col_arr(c.base, c.shape)
    n = fbm(c.P, 2, c.seed) - 0.5
    blk = cell(c.P, 0.5, c.seed + 3) - 0.5
    shade = n * 0.35 + blk * 0.12
    edge = np.minimum(np.minimum(c.s, 1 - c.s) * c.fw, np.minimum(c.t, 1 - c.t) * c.fh)
    stitch = (edge > 0.45) & (edge < 0.75) & ((np.floor((c.s * c.fw + c.t * c.fh) * 2) % 2) == 0) \
        & (c.fw >= 6) & (c.fh >= 5)
    col = tone(base, shade)
    col = np.where(stitch[..., None], col_arr(mix_c(c.base, (235, 215, 170), 0.55), c.shape), col)
    return col, None


def m_metal(c):
    base = col_arr(c.base, c.shape)
    n = fbm(c.P, 4, c.seed) - 0.5
    if c.face in SIDE_FACES:
        spec = np.exp(-((c.s - 0.28) ** 2) / 0.012) * 0.45
        grad = (0.5 - c.t) * 0.35
        shade = spec + grad + n * 0.08
    elif c.face == "top":
        shade = 0.22 + n * 0.08
    else:
        shade = -0.2 + n * 0.05
    edge = np.minimum(np.minimum(c.s, 1 - c.s) * c.fw, np.minimum(c.t, 1 - c.t) * c.fh)
    shade = shade + np.where(edge < 0.3, 0.18, 0)
    rivet = (edge > 0.45) & (edge < 0.85) & ((np.floor(c.s * c.fw) + np.floor(c.t * c.fh)) % 3 == 0) & \
        ((np.minimum(c.s, 1 - c.s) * c.fw < 0.85) & (np.minimum(c.t, 1 - c.t) * c.fh < 0.85))
    shade = shade + np.where(rivet, 0.35, 0)
    return tone(base, shade), None


def m_chain(c):
    base = col_arr(c.base, c.shape)
    a = (c.P[0] + c.P[2]) * 2.0
    b = c.P[1] * 2.0 + np.where(np.floor(a) % 2 == 0, 0, 0.5)
    u, v = a % 1.0, b % 1.0
    ring = ((u - 0.5) ** 2 + (v - 0.5) ** 2)
    shade = np.where(ring < 0.05, -0.55, np.where(ring < 0.16, 0.25, -0.15))
    return tone(base, shade), None


def m_bone(c):
    base = col_arr(c.base, c.shape)
    n = fbm(c.P, 2.5, c.seed)
    groove = np.abs(vnoise(c.P, 2.0, c.seed + 4) - 0.5) < 0.04
    shade = (n - 0.5) * 0.3 - np.where(groove, 0.3, 0)
    if c.face in SIDE_FACES:
        shade = shade - np.clip(c.t - 0.6, 0, 1) * 0.3
    return tone(base, shade), None


def m_horn(c):
    base = col_arr(c.base, c.shape)
    t = c.P[c.long_axis]
    Ps = c.P.copy()
    Ps[c.long_axis] = Ps[c.long_axis] * 0.3
    n = vnoise(Ps * 2.5, 1.0, c.seed) - 0.5
    ring = np.where((t % 1.5) < 0.25, -0.18, 0)
    return tone(base, n * 0.35 + ring), None


def m_membrane(c):
    base = col_arr(c.base, c.shape)
    n = fbm(c.P, 3, c.seed) - 0.5
    veins = np.abs(((c.P[0] + c.P[2] * 0.4) % 4.0) - 2.0) < 0.22
    col = tone(base, n * 0.3 - np.where(veins, 0.35, 0))
    return col, None


def m_slime(c):
    base = col_arr(c.base, c.shape)
    n = fbm(c.P, 4, c.seed) - 0.5
    bub = cell(c.P, 1.5, c.seed + 1)
    loc = [(c.P[k] / 1.5) % 1.0 for k in range(3)]
    r = sum((loc[k] - 0.5) ** 2 for k in range(3))
    bubble = (bub > 0.75) & (r < 0.12)
    shade = n * 0.25 + (0.5 - c.t) * 0.25 + np.where(bubble, 0.4, 0)
    return tone(base, shade), None


def m_wood(c):
    base = col_arr(c.base, c.shape)
    Ps = c.P.copy()
    Ps[c.long_axis] = Ps[c.long_axis] * 0.15
    n = vnoise(Ps * 2.0, 1.0, c.seed)
    grain = np.sin(n * 18.0) * 0.18
    return tone(base, grain + (cell(c.P, 0.5, c.seed) - 0.5) * 0.08), None


def m_ghost(c):
    base = col_arr(c.base, c.shape)
    n = fbm(c.P, 3, c.seed) - 0.5
    wisp = np.sin((c.P[1] + n * 4) * 1.4) * 0.12
    return tone(base, n * 0.25 + wisp + 0.05), None


def m_moss(c):
    rock = m_hide(c)[0]
    moss_col = col_arr(c.accent if cdist(c.accent, c.base) > 30 else mix_c(c.base, (60, 120, 40), 0.6), c.shape)
    n = fbm(c.P, 2.0, c.seed + 11)
    clump = cell(c.P, 0.5, c.seed + 2) - 0.5
    mossy = n + (0.25 if c.face == "top" else 0) > 0.52
    col = np.where(mossy[..., None], tone(moss_col, clump * 0.4), rock)
    return col, None


def m_ice(c):
    base = col_arr(c.base, c.shape)
    n = fbm(c.P, 3, c.seed) - 0.5
    streak = np.abs(((c.P[0] - c.P[1] + c.P[2]) % 3.0) - 1.5) < 0.12
    return tone(base, n * 0.2 + np.where(streak, 0.45, 0) + 0.05), None


def m_flesh(c):
    base = col_arr(c.base, c.shape)
    n = fbm(c.P, 2.5, c.seed) - 0.5
    vein = np.abs(vnoise(c.P, 1.8, c.seed + 4) - 0.5) < 0.05
    col = tone(base, n * 0.35 - np.where(vein, 0.35, 0))
    return col, None


def m_energy(c):
    base = col_arr(c.base, c.shape)
    ci = np.abs(2 * c.s - 1)
    cj = np.abs(2 * c.t - 1)
    centre = 1 - np.maximum(ci, cj)
    n = fbm(c.P, 2, c.seed) - 0.5
    col = tone(base, 0.12 + 0.45 * centre + n * 0.2)
    return col, col * 0.9


def m_lava(c):
    """Molten rock: dark crust plates with glowing seams of the base colour between them."""
    base = col_arr(c.base, c.shape)
    crust_col = col_arr(mix_c(c.base, (34, 14, 8), 0.72), c.shape)
    a = cell(c.P, 1.5, c.seed)
    b = cell(c.P + 0.5, 0.75, c.seed + 1)
    v = a * 0.65 + b * 0.35
    is_crust = v < 0.58
    hot = tone(base, 0.05 + (fbm(c.P, 2, c.seed + 5) - 0.5) * 0.3)
    crust = tone(crust_col, (cell(c.P, 0.5, c.seed + 9) - 0.5) * 0.25)
    col = np.where(is_crust[..., None], crust, hot)
    glow = np.where(is_crust[..., None], 0, hot * 0.75)
    return col, glow


def m_patches(c):
    """Piebald hide (cows): blotches of the accent colour over the base."""
    base = col_arr(c.base, c.shape)
    patch = col_arr(c.accent, c.shape)
    n = fbm(c.P, 5.0, c.seed + 3, 2)
    fur = (vnoise(np.stack([c.P[0] * 2, c.P[1] * 0.5, c.P[2] * 2]), 1.0, c.seed) - 0.5) * 0.2
    is_patch = n > 0.56
    return np.where(is_patch[..., None], tone(patch, fur), tone(base, fur)), None


def m_wool(c):
    base = col_arr(c.base, c.shape)
    a = cell(c.P, 1.0, c.seed)
    loc = [(c.P[k] % 1.0) for k in range(3)]
    r = sum((loc[k] - 0.5) ** 2 for k in range(3))
    shade = np.where(r < 0.18, 0.12, -0.12) + (a - 0.5) * 0.2
    return tone(base, shade), None


def m_crystal(c):
    """Cut crystal: diagonal facets with a bright leading edge and a pale glint (Olm's crystals, Zalcano)."""
    base = col_arr(c.base, c.shape)
    a = (c.P[0] * 0.8 + c.P[1] * 0.6 + c.P[2] * 0.7)
    f = (a / 2.2) % 1.0
    facet = cell(np.stack([np.floor(a / 2.2), c.P[1] * 0, c.P[1] * 0]), 1.0, c.seed) - 0.5
    shade = facet * 0.5 + np.where(f < 0.12, 0.55, 0) - f * 0.2
    if c.face == "top":
        shade = shade + 0.25
    col = tone(base, shade)
    return col, None


def m_smoke(c):
    """Billowing smoke: soft swirling puffs, light crowns and dark folds (smoke devils, smoke tails)."""
    base = col_arr(c.base, c.shape)
    n = fbm(np.stack([c.P[0] + np.sin(c.P[1] * 0.7) * 1.5, c.P[1], c.P[2] + np.cos(c.P[1] * 0.7) * 1.5]), 3.0, c.seed)
    puff = cell(c.P, 1.5, c.seed + 4)
    loc = [(c.P[k] / 1.5) % 1.0 for k in range(3)]
    r = (loc[0] - 0.5) ** 2 + (loc[2] - 0.5) ** 2 + (loc[1] - 0.35) ** 2
    crown = (puff > 0.55) & (r < 0.14)
    shade = (n - 0.5) * 1.0 + np.where(crown, 0.32, 0) - np.where(r > 0.3, 0.12, 0)
    return tone(base, shade), None


def m_spectral(c):
    """Spectral flesh: a translucent-looking body with a darker core, drifting wisps and an emissive rim
    of the accent colour along the box edges (Corporeal Beast, wraith-like bosses)."""
    base = col_arr(c.base, c.shape)
    rim_c = col_arr(c.accent, c.shape)
    n = fbm(c.P, 3.0, c.seed) - 0.5
    wisp = np.sin((c.P[0] * 0.6 + c.P[1] * 0.9 + c.P[2] * 0.5) + n * 5.0)
    edge = np.minimum(np.minimum(c.s, 1 - c.s) * c.fw, np.minimum(c.t, 1 - c.t) * c.fh)
    core = np.clip(edge / 3.0, 0, 1)
    blot = cell(c.P, 2.0, c.seed + 3) - 0.5
    col = tone(base, n * 0.45 + wisp * 0.12 + blot * 0.16 - core * 0.22)
    rim = np.clip(1 - edge / 0.5, 0, 1) * (0.55 if c.face in ("top", "front") else 0.35)
    col = blend(col, rim_c, rim)
    glow = rim_c * (rim * 0.5)[..., None]
    return col, glow


MATERIALS = {
    "hide": m_hide, "skin": m_skin, "face": m_skin, "rock": m_rock, "scales": m_scales, "plates": m_plates,
    "chitin": m_chitin, "fur": m_fur, "hair": m_hair, "feathers": m_feathers, "cloth": m_cloth,
    "leather": m_leather, "metal": m_metal, "chain": m_chain, "bone": m_bone, "horn": m_horn,
    "membrane": m_membrane, "slime": m_slime, "wood": m_wood, "ghost": m_ghost, "moss": m_moss, "ice": m_ice,
    "flesh": m_flesh, "energy": m_energy, "lava": m_lava, "obsidian": m_rock, "patches": m_patches, "wool": m_wool,
    "crystal": m_crystal, "smoke": m_smoke, "spectral": m_spectral,
}

# How strongly each material keeps the generic bevel/AO lighting (cloth and fur are soft; metal is crisp).
BEVEL = {"metal": 1.3, "rock": 1.2, "chitin": 1.1, "cloth": 0.6, "fur": 0.7, "hair": 0.6, "ghost": 0.5,
         "energy": 0.3, "lava": 0.6, "slime": 0.6,
         "crystal": 1.4, "smoke": 0.4, "spectral": 0.5}


# ----------------------------------------------------------------------------------------------
# Face geometry
# ----------------------------------------------------------------------------------------------

def box_faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def face_local(face, ii, jj, w, h, d):
    """Continuous box-local surface position (X, Y down, Z back) of texture coords (ii, jj) on a face
    (matches Minecraft's Cuboid UV layout)."""
    z = np.zeros_like(ii)
    if face == "front":
        return ii, jj, z
    if face == "back":
        return w - ii, jj, z + d
    if face == "right":
        return z, jj, d - ii
    if face == "left":
        return z + w, jj, ii
    if face == "top":
        return ii, z, d - jj
    return ii, z + h, d - jj  # bottom


def texture_scale(tw, th):
    m = max(tw, th)
    if m <= 128:
        return 4
    if m <= 256:
        return 2
    return 1


# ----------------------------------------------------------------------------------------------
# Painting
# ----------------------------------------------------------------------------------------------

def eye_colour(base, accent):
    a, b = rgb(accent), rgb(base)
    if sat(a) > 0.45 and lum(a) > 110 and cdist(a, b) > 90:
        return mix_c(a, (255, 255, 255), 0.15)
    if lum(b) > 150 or (b[0] > 150 and b[1] > 110 and b[2] < 110 and b[0] > b[2] + 60):
        return (230, 40, 30)
    return (255, 214, 60)


def paint_monster_face(arr, glow, x0, y0, W, H, base, accent, R, glow_eyes, maw=False):
    """Fierce creature face on a front face region (subpixel coords)."""
    eye = np.array(eye_colour(base, accent), dtype=np.float64)
    ew = max(1, int(round(W * 0.18)))
    eh = max(1, int(round(min(H * 0.14, ew * 0.8))))
    if W < 3 * R:
        ew = max(1, W // 3)
    gap = max(1, int(round(W * 0.12)))
    ey = int(round(H * 0.30))
    lx = W // 2 - gap // 2 - ew
    rx = W - lx - ew
    region = arr[y0:y0 + H, x0:x0 + W]
    # brow shadow
    by0 = max(0, ey - max(1, R // 2))
    region[by0:ey, max(0, lx - 1):lx + ew + 1] *= 0.55
    region[by0:ey, rx - 1:min(W, rx + ew + 1)] *= 0.55
    for k, ex in enumerate((lx, rx)):
        region[ey:ey + eh, ex:ex + ew] = eye
        # angry slant: inner top corner dark
        if ew >= 3 and eh >= 2:
            ix = ex + ew - 1 if k == 0 else ex
            region[ey, ix] = eye * 0.4
        if ew >= 2 and eh >= 2:
            px = ex + (ew - 1 if k == 0 else 0)
            region[ey + eh - 1, px] = eye * 0.55
            hx = ex + (0 if k == 0 else ew - 1)
            region[ey, hx] = np.minimum(255, eye * 1.25 + 30)
        if glow is not None and glow_eyes:
            glow[y0 + ey:y0 + ey + eh, x0 + ex:x0 + ex + ew] = eye
    # mouth
    if H >= 4 * R or (H >= 4 and R == 1):
        my = min(H - 1, ey + eh + max(1, int((H - ey - eh) * 0.45)))
        mx0, mx1 = W // 5, W - W // 5
        mh = max(1, R // 2) if not maw else max(2, int(H * 0.22))
        mouth = np.array(mix_c((35, 8, 8), base, 0.12), dtype=np.float64)
        region[my:my + mh, mx0:mx1] = mouth
        if maw:
            inner = np.array((255, 140, 30), dtype=np.float64)
            region[my + 1:my + mh - 1, mx0 + 1:mx1 - 1] = inner
            if glow is not None:
                glow[y0 + my + 1:y0 + my + mh - 1, x0 + mx0 + 1:x0 + mx1 - 1] = inner
        tooth = np.array((236, 230, 210), dtype=np.float64)
        step = max(2, R)
        for x in range(mx0, mx1, step):
            region[my:my + max(1, R // 2), x:x + max(1, R // 2)] = tooth
        if W >= 6 * R and my + mh + R <= H:
            region[my + mh:my + mh + R, mx0:mx0 + max(1, R // 2 + 1)] = tooth
            region[my + mh:my + mh + R, mx1 - max(1, R // 2 + 1):mx1] = tooth


def paint_human_face(arr, x0, y0, W, H, base, R, eye_col=(40, 30, 30)):
    """OSRS-style face: small dark eyes with whites, brows, nose shading and a mouth line."""
    region = arr[y0:y0 + H, x0:x0 + W]
    px = W / 8.0  # one "head pixel" (heads are usually 8 wide)
    py = H / 8.0

    def rect(a0, b0, a1, b1, col, f=None):
        xa, xb = int(round(a0 * px)), max(int(round(a0 * px)) + 1, int(round(a1 * px)))
        ya, yb = int(round(b0 * py)), max(int(round(b0 * py)) + 1, int(round(b1 * py)))
        if f is not None:
            region[ya:yb, xa:xb] *= f
        else:
            region[ya:yb, xa:xb] = col

    white = np.array((235, 232, 225), dtype=np.float64)
    pupil = np.array(eye_col, dtype=np.float64)
    brow = np.array(mix_c(base, (40, 25, 15), 0.7), dtype=np.float64)
    rect(1.5, 3.6, 3.3, 4.6, white)
    rect(4.7, 3.6, 6.5, 4.6, white)
    rect(2.4, 3.6, 3.3, 4.6, pupil)
    rect(4.7, 3.6, 5.6, 4.6, pupil)
    rect(1.4, 2.9, 3.4, 3.35, brow)
    rect(4.6, 2.9, 6.6, 3.35, brow)
    rect(3.6, 4.4, 4.4, 5.6, None, 0.82)   # nose shadow
    rect(3.4, 5.4, 4.6, 5.8, None, 0.72)
    rect(2.8, 6.4, 5.2, 6.8, np.array(mix_c(base, (90, 30, 30), 0.55), dtype=np.float64))
    rect(0, 7.3, 8, 8, None, 0.9)          # chin shadow


def paint_skull_face(arr, glow, x0, y0, W, H, base, R, glow_eyes):
    """Skull: deep dark eye sockets (optionally with glowing pinpoints), a nasal hole and a row of teeth."""
    region = arr[y0:y0 + H, x0:x0 + W]
    px, py = W / 8.0, H / 8.0

    def box(a0, b0, a1, b1):
        return (slice(int(round(b0 * py)), max(int(round(b0 * py)) + 1, int(round(b1 * py)))),
                slice(int(round(a0 * px)), max(int(round(a0 * px)) + 1, int(round(a1 * px)))))

    sock = np.array((22, 16, 14), dtype=np.float64)
    for a0, a1 in ((1.2, 3.4), (4.6, 6.8)):
        region[box(a0, 2.6, a1, 4.9)] = sock
        region[box(a0 - 0.3, 2.2, a1 + 0.3, 2.6)] *= 0.8
        if glow_eyes:
            sl = box(a0 + 0.8, 3.3, a1 - 0.8, 4.1)
            region[sl] = (255, 70, 40)
            if glow is not None:
                glow[y0:y0 + H, x0:x0 + W][sl] = (255, 70, 40)
    region[box(3.6, 5.2, 4.4, 6.1)] = sock
    region[box(1.5, 6.6, 6.5, 6.9)] *= 0.45
    for k in range(6):
        region[box(1.6 + k * 0.85, 6.9, 1.9 + k * 0.85, 7.6)] *= 0.55


def paint_jaw(arr, faces, R, base):
    fx, fy, fw, fh = faces["front"]
    if fw >= 2:
        tooth = np.array((238, 232, 212), dtype=np.float64)
        for x in range(0, fw * R, max(2, R)):
            arr[fy * R:fy * R + max(1, R // 2 + 1), fx * R + x:fx * R + x + max(1, R // 2)] = tooth
    tx, ty, tw, th = faces["top"]
    if tw > 0 and th > 0:
        sub = arr[ty * R:(ty + th) * R, tx * R:(tx + tw) * R]
        sub[...] = np.array((130, 30, 35), dtype=np.float64)
        sub[:, ::max(2, R)] *= 0.8


def _rot(axis, a):
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], dtype=np.float64)
    if axis == "y":
        return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], dtype=np.float64)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], dtype=np.float64)


def part_matrices(m):
    """Model-space transform of every part in its rest pose (pivot chain, Rz*Ry*Rx as in Minecraft's ModelPart)."""
    parts = {p["name"]: p for p in m.get("parts", [])}
    cache = {}

    def get(name):
        if name not in cache:
            p = parts[name]
            par = get(p["parent"]) if p.get("parent") in parts else np.eye(4)
            t = np.eye(4)
            t[:3, 3] = (p.get("px", 0.0), p.get("py", 0.0), p.get("pz", 0.0))
            cache[name] = par @ t @ _rot("z", p.get("roll", 0.0)) @ _rot("y", p.get("yaw", 0.0)) @ _rot("x", p.get("pitch", 0.0))
        return cache[name]

    for n in parts:
        get(n)
    return cache


def model_light(m):
    """(part matrices, top Y, bottom Y) of the model at rest, for the model-wide top-to-bottom light."""
    mats = part_matrices(m)
    ys = []
    for p in m.get("parts", []):
        M = mats[p["name"]]
        for b in p.get("boxes", []):
            for cx in (0, b["w"]):
                for cy in (0, b["h"]):
                    for cz in (0, b["d"]):
                        v = M @ np.array([b.get("x", 0.0) + cx, b.get("y", 0.0) + cy, b.get("z", 0.0) + cz, 1.0])
                        ys.append(v[1])
    if not ys:
        return mats, 0.0, 24.0
    return mats, min(ys), max(ys)


def body_light(Y, y0, y1):
    """Model-wide light factor at model-space height Y (Y grows down): the top of the creature catches the light,
    its lower body falls into shade, and the last two pixels above the ground get a contact shadow."""
    t = np.clip((Y - y0) / max(1e-3, y1 - y0), 0, 1)
    g = 1.06 - 0.16 * t
    if y1 > 21.5:
        g = g * (1 - 0.14 * np.clip((Y - (y1 - 2.5)) / 2.5, 0, 1))
    return g


def paint_box(arr, glow, model_id, part, bi, box, R, material, eyes_mode, light=None):
    w, h, d, u, v = box["w"], box["h"], box["d"], box["u"], box["v"]
    base = rgb(part.get("color", 0x808080))
    if lum(base) < 30:
        base = mix_c(base, (255, 255, 255), 0.08)
    acc = part.get("accent")
    accent = rgb(acc) if acc is not None else shade_c(base, 0.75)
    role = part.get("role") or "STATIC"
    glow_mode = (part.get("glow") or "none").lower()
    pattern = (part.get("pattern") or "none").lower()
    seed = seed_of(model_id, part.get("name"), bi)
    pseed = seed_of(model_id, part.get("name"))
    dims = (w, h, d)
    long_axis = int(np.argmax(dims))
    ox, oy, oz = box.get("x", 0.0), box.get("y", 0.0), box.get("z", 0.0)
    fn = MATERIALS.get(material, m_hide)
    bevel_k = BEVEL.get(material, 1.0)
    faces = box_faces(u, v, w, h, d)
    for face, (fx, fy, fw, fh) in faces.items():
        if fw <= 0 or fh <= 0:
            continue
        W, H = fw * R, fh * R
        ii, jj = np.meshgrid((np.arange(W) + 0.5) / R, (np.arange(H) + 0.5) / R)
        lx, ly, lz = face_local(face, ii, jj, w, h, d)
        P = np.stack([lx + ox, ly + oy, lz + oz])
        c = Ctx()
        c.P, c.face, c.s, c.t, c.fw, c.fh = P, face, ii / fw, jj / fh, fw, fh
        c.dims, c.base, c.accent, c.seed, c.shape, c.R = dims, base, accent, pseed, (H, W), R
        c.long_axis, c.role, c.loc = long_axis, role, (lx, ly, lz)
        col, g = fn(c)
        if pattern in ("spots", "stripes"):
            col = apply_pattern(col, pattern, c, accent, seed)
        # lighting
        f = np.full((H, W), FACE_LIGHT[face])
        if face in SIDE_FACES and fh > 1:
            f = f * (1.10 - 0.28 * np.clip(c.t, 0, 1) ** 1.3 * bevel_k ** 0.5)
        e = 1.0 / R
        top_e = (jj < e * 1.01)
        left_e = (ii < e * 1.01)
        bot_e = (jj > fh - e * 1.01)
        right_e = (ii > fw - e * 1.01)
        if fw * fh >= 2:
            f = f * np.where(top_e | left_e, 1 + 0.10 * bevel_k, 1.0)
            f = f * np.where(bot_e | right_e, 1 - 0.20 * bevel_k, 1.0)
        if light is not None:
            M, y0, y1 = light
            Y = M[1, 0] * P[0] + M[1, 1] * P[1] + M[1, 2] * P[2] + M[1, 3]
            f = f * body_light(Y, y0, y1)
        col = np.clip(col * f[..., None] + np.where(f[..., None] > 1, (f[..., None] - 1) * 40, 0), 0, 255)
        sl = (slice(fy * R, fy * R + H), slice(fx * R, fx * R + W))
        # limb tips (claws/hooves) for animal legs
        if role in LIMB_ROLES and h >= 4 and material in ("hide", "fur", "scales", "skin", "flesh", "plates") \
                and not part.get("material"):
            tipc = mix_c(base, accent, 0.5) if cdist(accent, base) < 120 else base
            tip = np.array(shade_c(tipc, 0.5), dtype=np.float64)
            if face in SIDE_FACES:
                band = c.t > 1 - (1.5 if h >= 8 else 1.0) / fh
                col = np.where(band[..., None], tip * (0.85 + 0.3 * (cell(P, 0.5, seed) - 0.5))[..., None], col)
            elif face == "bottom":
                col[...] = tip * 0.8
        if role in ("WING_LEFT", "WING_RIGHT") and face in ("top", "bottom") and material == "membrane":
            bone = np.array(shade_c(mix_c(base, accent, 0.5), 0.6), dtype=np.float64)
            X, Z = lx, lz
            along_x = w >= d
            m = ((X % 5.0) < 0.6) | (Z < 0.6) if along_x else ((Z % 5.0) < 0.6) | (X < 0.6)
            col = np.where(m[..., None], bone, col)
        gl = None
        if glow_mode == "full" or pattern == "glow":
            gl = col * 0.85 if g is None else g
        elif glow_mode in ("cracks", "runes") or g is not None:
            gl = g
        if glow_mode == "cracks":
            n = vnoise(P, 7.0, pseed + 77) - 0.5
            n2 = vnoise(P, 11.0, pseed + 78) - 0.5
            crack = isoline(n, R) | isoline(n2, R)
            hot = np.array(accent if lum(accent) > 120 else (255, 120, 20), dtype=np.float64)
            core = np.minimum(255, hot * 1.15 + 25)
            edge = dilate(crack, max(1, R // 2)) & ~crack
            col = np.where(crack[..., None], core, np.where(edge[..., None], blend(col, hot, 0.45), col))
            gcr = np.where(crack[..., None], core, np.where(edge[..., None], hot * 0.35, 0))
            gl = gcr if gl is None else np.maximum(gl, gcr)
        if glow_mode == "spots":
            # scattered glowing eyes/pores of the accent colour (Nightmare's eyes, Kraken's photophores)
            q = cell(P, 1.6, pseed + 13)
            loc = [(P[k] / 1.6) % 1.0 for k in range(3)]
            r = sum((loc[k] - 0.5) ** 2 for k in range(3))
            spot = (q > 0.72) & (r < 0.07)
            hot = np.array(accent, dtype=np.float64)
            ring = (q > 0.72) & (r >= 0.07) & (r < 0.13)
            col = np.where(spot[..., None], hot * 0.8, np.where(ring[..., None], col * 0.45, col))
            gs = np.where(spot[..., None], hot * 0.6, 0)
            gl = gs if gl is None else np.maximum(gl, gs)
        if glow_mode == "rim":
            # emissive outline along every box edge (spectral or charged parts)
            edge = np.minimum(np.minimum(c.s, 1 - c.s) * c.fw, np.minimum(c.t, 1 - c.t) * c.fh)
            rim = np.clip(1 - edge / 0.6, 0, 1)
            hot = np.array(accent, dtype=np.float64)
            col = blend(col, hot, rim * 0.8)
            gr = hot * (rim * 0.8)[..., None]
            gl = gr if gl is None else np.maximum(gl, gr)
        if glow_mode == "sigil" and face == "front" and fw >= 4 and fh >= 4:
            sig = sigil_mask(c.s, c.t, fw, fh)
            hot = np.array(accent, dtype=np.float64)
            col = np.where(sig[..., None], np.minimum(255, hot * 1.1 + 15), col)
            gsg = np.where(sig[..., None], hot, 0)
            gl = gsg if gl is None else np.maximum(gl, gsg)
        if glow_mode == "runes":
            q = cell(P, 2.0, pseed + 5)
            loc = [(P[k] / 2.0) % 1.0 for k in range(3)]
            mark = (q > 0.7) & ((np.abs(loc[0] - 0.5) < 0.08) | (np.abs(loc[1] - 0.5) < 0.08))
            hot = np.array(accent, dtype=np.float64)
            col = np.where(mark[..., None], hot, col)
            gr = np.where(mark[..., None], hot, 0)
            gl = gr if gl is None else np.maximum(gl, gr)
        arr[sl] = col
        if gl is not None and glow is not None:
            glow[sl] = np.maximum(glow[sl], np.clip(gl, 0, 255))
    if role == "JAW":
        paint_jaw(arr, faces, R, base)
        if glow_mode == "maw":
            fx, fy, fw, fh = faces["front"]
            band = max(1, int(fh * R * 0.45))
            hot = np.array((255, 140, 40), dtype=np.float64)
            arr[fy * R:fy * R + band, fx * R + R // 2:(fx + fw) * R - R // 2] = hot
            for x in range(R // 2, fw * R - R // 2, max(2, R)):
                arr[fy * R:fy * R + max(1, band // 2), fx * R + x:fx * R + x + max(1, R // 2)] = (238, 228, 200)
            if glow is not None:
                g = glow[fy * R:fy * R + band, fx * R + R // 2:(fx + fw) * R - R // 2]
                g[...] = np.maximum(g, hot * 0.9)
        if glow_mode == "maw" and glow is not None:
            tx, ty, tw, th = faces["top"]
            glow[ty * R:(ty + th) * R, tx * R:(tx + tw) * R] = np.array((255, 120, 30)) * 0.8
            arr[ty * R:(ty + th) * R, tx * R:(tx + tw) * R] = np.array((255, 130, 40))
    fx, fy, fw, fh = faces["front"]
    if pattern == "visor" and fw >= 3 and fh >= 3:
        region = arr[fy * R:(fy + fh) * R, fx * R:(fx + fw) * R]
        sy = int(fh * R * 0.42)
        slit = max(1, R // 2)
        region[sy:sy + slit, R:fw * R - R] = region[sy:sy + slit, R:fw * R - R] * 0.15
        cx = fw * R // 2
        region[sy:int(fh * R * 0.8), cx - slit // 2 - (1 if R >= 2 else 0):cx + slit // 2 + 1] *= 0.25
        region[sy + slit:sy + slit + max(1, R // 4), R:fw * R - R] *= 1.25
    if fw > 0 and fh > 0:
        if eyes_mode == "face":
            paint_human_face(arr, fx * R, fy * R, fw * R, fh * R, base, R)
        elif eyes_mode == "eyes":
            paint_monster_face(arr, glow, fx * R, fy * R, fw * R, fh * R, base, accent, R,
                               glow_mode in ("eyes", "maw"), maw=glow_mode == "maw")
        elif eyes_mode == "animal":
            region = arr[fy * R:(fy + fh) * R, fx * R:(fx + fw) * R]
            W, H = fw * R, fh * R
            e = max(1, int(round(W * 0.14)))
            ey = int(H * 0.32)
            for ex in (int(W * 0.12), W - int(W * 0.12) - e):
                region[ey:ey + e, ex:ex + e] = (20, 16, 14)
                if e >= 2:
                    region[ey:ey + max(1, e // 2), ex:ex + max(1, e // 2)] = (230, 230, 230)
        elif eyes_mode == "skull":
            paint_skull_face(arr, glow, fx * R, fy * R, fw * R, fh * R, base, R, glow_mode == "eyes")
        elif eyes_mode == "snout":
            region = arr[fy * R:(fy + fh) * R, fx * R:(fx + fw) * R]
            if fw >= 4 and fh >= 2:
                nx = max(0, fw // 2 - 1 - (1 if fw >= 6 else 0))
                region[0:R, nx * R:(nx + 1) * R] *= 0.3
                region[0:R, (fw - 1 - nx) * R:(fw - nx) * R] *= 0.3
            if fh >= 3:
                region[(fh - 1) * R:fh * R, :] *= 0.5


def sigil_mask(s, t, fw, fh):
    """Corporeal Beast's spirit sigil on a front face: a ring around two upright "nostril" ovals and a
    short stem (fractional face coords s, t)."""
    asp = fw / max(1, fh)
    x = (s - 0.5) * asp * 2
    y = (t - 0.5) * 2
    r = np.sqrt(x * x + y * y)
    ring = (r > 0.62) & (r < 0.78)
    nos = (((np.abs(x) - 0.22) / 0.13) ** 2 + ((y + 0.05) / 0.32) ** 2) < 1.0
    stem = (np.abs(x) < 0.07) & (y > 0.3) & (y < 0.62)
    return ring | nos | stem


def dilate(m, r):
    out = m.copy()
    for _ in range(r):
        o = out.copy()
        o[1:, :] |= out[:-1, :]
        o[:-1, :] |= out[1:, :]
        o[:, 1:] |= out[:, :-1]
        o[:, :-1] |= out[:, 1:]
        out = o
    return out


def isoline(n, R):
    """Pixels where a noise field crosses zero: thin connected lines (widened to ~half a model pixel)."""
    pos = n > 0
    m = np.zeros_like(pos)
    m[:, :-1] |= pos[:, :-1] != pos[:, 1:]
    m[:-1, :] |= pos[:-1, :] != pos[1:, :]
    return dilate(m, 1) if R >= 4 else m


def apply_pattern(col, pattern, c, accent, seed):
    acc = np.array(accent, dtype=np.float64)
    if pattern == "spots":
        q = cell(c.P, 2.0, seed + 31)
        loc = [(c.P[k] / 2.0) % 1.0 for k in range(3)]
        r = sum((loc[k] - 0.5) ** 2 for k in range(3))
        m = (q > 0.5) & (r < 0.09)
        far = cdist(accent, tuple(col[0, 0].astype(int))) > 40
        spot = acc if far else np.minimum(255, col * 1.3)
        return np.where(m[..., None], spot, col)
    if pattern == "stripes":
        t = c.P[c.long_axis]
        other = c.P[(c.long_axis + 1) % 3]
        wob = np.sin(other * 0.9 + (seed % 100) * 0.06) * 0.6
        m = ((t + wob) % 4.0) < 1.0
        return np.where(m[..., None], blend(col, acc, 0.85), col)
    return col


def model_texture(m):
    """(texture, glow texture or None) for a catalog model entry."""
    R = texture_scale(m["tex_w"], m["tex_h"])
    W, H = m["tex_w"] * R, m["tex_h"] * R
    arr = np.zeros((H, W, 3), dtype=np.float64)
    alpha = np.zeros((H, W), dtype=np.uint8)
    has_glow = bool(m.get("has_glow"))
    glow = np.zeros((H, W, 3), dtype=np.float64)
    parts = m.get("parts", [])
    any_face = any((p.get("pattern") or "").lower() in ("eyes", "face", "skull", "eyes_animal") or (p.get("material") or "") == "face"
                   for p in parts)
    mats, top_y, bottom_y = model_light(m)
    for p in parts:
        boxes = p.get("boxes", [])
        material = infer_material(m["id"], p)
        pat = (p.get("pattern") or "").lower()
        is_face = material == "face" or pat == "face"
        is_skull = pat == "skull"
        is_animal = pat == "eyes_animal"
        # "blank": a head with no painted face (slugs and insects whose eyes are separate parts)
        wants_eyes = pat == "eyes" or is_face or is_skull or is_animal or (not any_face and p.get("role") == "HEAD" and pat != "blank")
        eye_box = None
        if wants_eyes and boxes:
            eye_box = max(range(len(boxes)), key=lambda k: (boxes[k]["w"] * boxes[k]["h"], -k))
        for bi, b in enumerate(boxes):
            if bi == eye_box:
                mode = "face" if is_face else "skull" if is_skull else "animal" if is_animal else "eyes"
            elif wants_eyes and not is_face and not is_animal:
                mode = "snout"
            else:
                mode = None
            paint_box(arr, glow, m["id"], p, bi, b, R, "skin" if material == "face" else material, mode,
                      light=(mats[p["name"]], top_y, bottom_y))
            for face, (fx, fy, fw, fh) in box_faces(b["u"], b["v"], b["w"], b["h"], b["d"]).items():
                if fw > 0 and fh > 0:
                    alpha[fy * R:(fy + fh) * R, fx * R:(fx + fw) * R] = 255
    rgba = np.dstack([np.clip(arr, 0, 255).astype(np.uint8), alpha])
    img = Image.fromarray(rgba, "RGBA")
    gimg = None
    if has_glow:
        g = np.clip(glow, 0, 255).astype(np.uint8)
        ga = np.where(g.max(axis=2) > 8, 255, 0).astype(np.uint8)
        g[ga == 0] = 0
        gimg = Image.fromarray(np.dstack([g, ga]), "RGBA")
    return img, gimg


def lit_parts(m, glow_img):
    """Names of the parts with at least one emissive texel in the glow layer (in spec order).

    The client draws only these parts in the full-bright glow pass: every other texel of the glow
    layer is fully transparent black, which adds nothing under the additive eyes blend, so skipping
    those boxes is invisible and saves most of the second pass's vertices.
    """
    R = texture_scale(m["tex_w"], m["tex_h"])
    alpha = np.asarray(glow_img.convert("RGBA"))[..., 3]
    out = []
    for p in m.get("parts", []):
        lit = False
        for b in p.get("boxes", []):
            for fx, fy, fw, fh in box_faces(b["u"], b["v"], b["w"], b["h"], b["d"]).values():
                if fw > 0 and fh > 0 and alpha[fy * R:(fy + fh) * R, fx * R:(fx + fw) * R].any():
                    lit = True
                    break
            if lit:
                break
        if lit:
            out.append(p["name"])
    return out
