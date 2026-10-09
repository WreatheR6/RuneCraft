#!/usr/bin/env python3
"""Generates the Gielinor dimension data pack (dimension, noise, biomes, features, tags, recipes).

Run from the repository root: python3 tools/gen_worldgen.py
Block ids must match the Java registries (ModBlocks); see build/catalog.json.
"""
import json
import os

ROOT = "src/main/resources/data"
written = 0


def write(path, obj):
    global written
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")
    written += 1


# ---------------------------------------------------------------- dimension
write("runecraft/dimension_type/gielinor.json", {
    "ultrawarm": False, "natural": True, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
    "ambient_light": 0.0, "monster_spawn_light_level": {"type": "minecraft:uniform", "value": {"min_inclusive": 0, "max_inclusive": 7}},
    "monster_spawn_block_light_limit": 0, "piglin_safe": False, "bed_works": True, "respawn_anchor_works": False,
    "has_raids": False, "logical_height": 384, "min_y": -64, "height": 384,
    "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:overworld"
})

REGIONS = ["misthalin", "lumbridge_swamp", "asgarnia", "wilderness", "kharidian_desert", "morytania", "kandarin",
           "feldip_hills", "fremennik_province", "troll_country", "karamja", "tirannwn", "uncharted_isles"]

write("runecraft/dimension/gielinor.json", {
    "type": "runecraft:gielinor",
    "generator": {
        "type": "minecraft:noise",
        "settings": "runecraft:gielinor",
        "biome_source": {"type": "runecraft:gielinor", "biomes": {r: "runecraft:" + r for r in REGIONS}}
    }
})

# ---------------------------------------------------------------- noise: gentle OSRS-like hills with lakes
write("runecraft/worldgen/noise/gielinor_terrain.json", {"firstOctave": -8, "amplitudes": [1.0, 1.0, 0.5, 0.25]})
write("runecraft/worldgen/noise/gielinor_hills.json", {"firstOctave": -6, "amplitudes": [1.0, 0.5, 0.25]})

final_density = {
    "type": "minecraft:interpolated",
    "argument": {
        "type": "minecraft:add",
        "argument1": {"type": "minecraft:y_clamped_gradient", "from_y": 0, "to_y": 128, "from_value": 1.0, "to_value": -1.0},
        "argument2": {
            "type": "minecraft:add",
            "argument1": {"type": "minecraft:mul", "argument1": 0.16,
                          "argument2": {"type": "minecraft:noise", "noise": "runecraft:gielinor_terrain", "xz_scale": 1.0, "y_scale": 0.0}},
            "argument2": {"type": "minecraft:mul", "argument1": 0.05,
                          "argument2": {"type": "minecraft:noise", "noise": "runecraft:gielinor_hills", "xz_scale": 1.0, "y_scale": 0.0}}
        }
    }
}


def block(name, props=None):
    d = {"Name": name}
    if props:
        d["Properties"] = props
    return d


def b(name):
    return {"type": "minecraft:block", "result_state": block(name)}


def cond(if_true, then_run):
    return {"type": "minecraft:condition", "if_true": if_true, "then_run": then_run}


def biome_is(*names):
    return {"type": "minecraft:biome", "biome_is": ["runecraft:" + n for n in names]}


ON_FLOOR = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
UNDER_FLOOR = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True, "secondary_depth_range": 0, "surface_type": "floor"}
ABOVE_WATER = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}
NEAR_SURFACE = {"type": "minecraft:above_preliminary_surface"}

surface_rule = {"type": "minecraft:sequence", "sequence": [
    cond({"type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_floor",
          "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}}, b("minecraft:bedrock")),
    cond(NEAR_SURFACE, {"type": "minecraft:sequence", "sequence": [
        cond(biome_is("kharidian_desert"), {"type": "minecraft:sequence", "sequence": [
            cond(ON_FLOOR, b("minecraft:sand")),
            cond(UNDER_FLOOR, b("minecraft:sandstone"))]}),
        cond(biome_is("troll_country"), {"type": "minecraft:sequence", "sequence": [
            cond(ON_FLOOR, cond(ABOVE_WATER, b("minecraft:snow_block"))),
            cond(UNDER_FLOOR, b("minecraft:stone"))]}),
        cond(biome_is("wilderness"), {"type": "minecraft:sequence", "sequence": [
            cond(ON_FLOOR, cond(ABOVE_WATER, cond({"type": "minecraft:noise_threshold", "noise": "minecraft:surface",
                                                    "min_threshold": 0.1, "max_threshold": 10.0}, b("minecraft:coarse_dirt")))),
            cond(ON_FLOOR, cond(ABOVE_WATER, b("minecraft:grass_block"))),
            cond(UNDER_FLOOR, b("minecraft:dirt"))]}),
        cond(biome_is("lumbridge_swamp", "morytania"), {"type": "minecraft:sequence", "sequence": [
            cond(ON_FLOOR, cond(ABOVE_WATER, cond({"type": "minecraft:noise_threshold", "noise": "minecraft:surface",
                                                    "min_threshold": 0.25, "max_threshold": 10.0}, b("minecraft:mud")))),
            cond(ON_FLOOR, cond(ABOVE_WATER, b("minecraft:grass_block"))),
            cond(UNDER_FLOOR, b("minecraft:dirt"))]}),
        cond(ON_FLOOR, cond(ABOVE_WATER, b("minecraft:grass_block"))),
        cond(ON_FLOOR, b("minecraft:sand")),
        cond(UNDER_FLOOR, b("minecraft:dirt"))
    ]})
]}

write("runecraft/worldgen/noise_settings/gielinor.json", {
    "sea_level": 62,
    "disable_mob_generation": False,
    "aquifers_enabled": False,
    "ore_veins_enabled": False,
    "legacy_random_source": False,
    "default_block": block("minecraft:stone"),
    "default_fluid": block("minecraft:water", {"level": "0"}),
    "noise": {"min_y": -64, "height": 384, "size_horizontal": 1, "size_vertical": 2},
    "noise_router": {
        "barrier": 0.0, "fluid_level_floodedness": 0.0, "fluid_level_spread": 0.0, "lava": 0.0,
        "temperature": 0.0, "vegetation": 0.0, "continents": 0.0, "erosion": 0.0, "depth": 0.0, "ridges": 0.0,
        "initial_density_without_jaggedness": final_density,
        "final_density": final_density,
        "vein_toggle": 0.0, "vein_ridged": 0.0, "vein_gap": 0.0
    },
    "spawn_target": [{"continentalness": [-1.0, 1.0], "depth": 0.0, "erosion": [-1.0, 1.0], "humidity": [-1.0, 1.0],
                      "offset": 0.0, "temperature": [-1.0, 1.0], "weirdness": [-1.0, 1.0]}],
    "surface_rule": surface_rule
})

# ---------------------------------------------------------------- features
TREES = {  # id: (leaves, base height, rand, foliage radius, foliage height, placement)
    "tree": ("minecraft:oak_leaves", 4, 2, 2, 3, ("count", 2)),
    "oak": ("minecraft:dark_oak_leaves", 5, 2, 3, 3, ("count", 1)),
    "willow": ("minecraft:jungle_leaves", 5, 1, 3, 4, ("rarity", 2)),
    "teak": ("minecraft:jungle_leaves", 6, 2, 2, 3, ("count", 1)),
    "maple": ("minecraft:acacia_leaves", 6, 2, 3, 3, ("rarity", 2)),
    "mahogany": ("minecraft:dark_oak_leaves", 7, 2, 3, 3, ("rarity", 2)),
    "yew": ("minecraft:spruce_leaves", 6, 3, 3, 4, ("rarity", 4)),
    "magic": ("minecraft:flowering_azalea_leaves", 7, 3, 3, 3, ("rarity", 10)),
    "redwood": ("minecraft:spruce_leaves", 12, 6, 2, 10, ("rarity", 12)),
}
for tid, (leaves, h, r, rad, fh, place) in TREES.items():
    foliage = {"type": "minecraft:blob_foliage_placer", "radius": rad, "offset": 0, "height": fh}
    if tid == "redwood":
        foliage = {"type": "minecraft:spruce_foliage_placer", "radius": {"type": "minecraft:uniform", "value": {"min_inclusive": 2, "max_inclusive": 3}},
                   "offset": {"type": "minecraft:uniform", "value": {"min_inclusive": 0, "max_inclusive": 2}},
                   "trunk_height": {"type": "minecraft:uniform", "value": {"min_inclusive": 1, "max_inclusive": 2}}}
    write(f"runecraft/worldgen/configured_feature/{tid}_tree.json", {"type": "minecraft:tree", "config": {
        "ignore_vines": True, "force_dirt": False,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": block("minecraft:dirt")},
        "trunk_provider": {"type": "minecraft:simple_state_provider", "state": block(f"runecraft:{tid}_tree_log", {"depleted": "false"})},
        "foliage_provider": {"type": "minecraft:simple_state_provider",
                             "state": block(leaves, {"distance": "7", "persistent": "false", "waterlogged": "false"})},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": h, "height_rand_a": r, "height_rand_b": 0},
        "foliage_placer": foliage,
        "decorators": []}})
    first = {"type": "minecraft:count", "count": place[1]} if place[0] == "count" else {"type": "minecraft:rarity_filter", "chance": place[1]}
    write(f"runecraft/worldgen/placed_feature/{tid}_tree.json", {"feature": f"runecraft:{tid}_tree", "placement": [
        first, {"type": "minecraft:in_square"}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                    "state": block("minecraft:oak_sapling", {"stage": "0"})}},
        {"type": "minecraft:biome"}]})

ROCKS = {  # id: (underground count, surface rarity)
    "copper": (6, 6), "tin": (6, 6), "clay": (3, 10), "rune_essence": (0, 40), "iron": (5, 8), "silver": (2, 24),
    "pure_essence": (0, 60), "coal": (5, 10), "gold": (2, 24), "gem": (1, 40), "mithril": (2, 24), "adamantite": (1, 40),
    "runite": (1, 90), "amethyst": (1, 120),
}
for rid, (count, rarity) in ROCKS.items():
    state = block(f"runecraft:{rid}_rock", {"depleted": "false"})
    write(f"runecraft/worldgen/configured_feature/{rid}_rock_surface.json",
          {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": state}}})
    write(f"runecraft/worldgen/placed_feature/{rid}_rock_surface.json", {"feature": f"runecraft:{rid}_rock_surface", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
            {"type": "minecraft:matching_blocks", "offset": [0, -1, 0],
             "blocks": ["minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:sand",
                        "minecraft:stone", "minecraft:snow_block", "minecraft:mud"]}]}},
        {"type": "minecraft:biome"}]})
    if count > 0:
        write(f"runecraft/worldgen/configured_feature/{rid}_rock_ore.json", {"type": "minecraft:ore", "config": {
            "size": 5, "discard_chance_on_air_exposure": 0.0,
            "targets": [{"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": state}]}})
        write(f"runecraft/worldgen/placed_feature/{rid}_rock_ore.json", {"feature": f"runecraft:{rid}_rock_ore", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                          "min_inclusive": {"absolute": -48}, "max_inclusive": {"absolute": 60}}},
            {"type": "minecraft:biome"}]})

SPOTS = ["net_bait", "lure_bait", "cage_harpoon", "net_harpoon", "angler", "dark_crab"]
for sid in SPOTS:
    name = f"{sid}_fishing_spot"
    write(f"runecraft/worldgen/configured_feature/{name}.json", {"type": "minecraft:simple_block", "config": {
        "to_place": {"type": "minecraft:simple_state_provider", "state": block(f"runecraft:{name}", {"waterlogged": "true"})}}})
    write(f"runecraft/worldgen/placed_feature/{name}.json", {"feature": f"runecraft:{name}", "placement": [
        {"type": "minecraft:count", "count": 6}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": -1},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:water"},
            {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": "minecraft:water"},
            {"type": "minecraft:matching_blocks", "offset": [0, 1, 0], "blocks": "minecraft:air"}]}},
        {"type": "minecraft:biome"}]})

# Global feature order per step (biomes use ordered subsequences to avoid feature-order cycles).
ORDER = {
    6: ["minecraft:ore_dirt", "minecraft:ore_gravel"] + [f"runecraft:{r}_rock_ore" for r in ROCKS if ROCKS[r][0] > 0],
    8: ["minecraft:spring_water"],
    9: [f"runecraft:{s}_fishing_spot" for s in SPOTS] + [f"runecraft:{r}_rock_surface" for r in ROCKS]
       + [f"runecraft:{t}_tree" for t in TREES]
       + ["minecraft:flower_default", "minecraft:flower_swamp", "minecraft:patch_grass_plain", "minecraft:patch_grass_taiga",
          "minecraft:patch_grass_jungle", "minecraft:patch_large_fern", "minecraft:patch_tall_grass_2",
          "minecraft:patch_dead_bush_2", "minecraft:patch_cactus_desert", "minecraft:patch_sugar_cane",
          "minecraft:patch_waterlily", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal"],
    10: ["minecraft:freeze_top_layer"],
}


def features(wanted):
    steps = [[] for _ in range(11)]
    for step, order in ORDER.items():
        steps[step] = [f for f in order if f in wanted]
    return steps


def spawns(monsters=(), creatures=()):
    def entries(lst):
        return [{"type": t, "weight": w, "minCount": lo, "maxCount": hi} for (t, w, lo, hi) in lst]
    return {"monster": entries(monsters), "creature": entries(creatures), "ambient": [], "axolotls": [],
            "underground_water_creature": [], "water_creature": [], "water_ambient": [], "misc": []}


FARM = [("minecraft:chicken", 10, 2, 4), ("minecraft:cow", 8, 2, 4), ("minecraft:sheep", 8, 2, 4), ("minecraft:pig", 4, 1, 3)]
COMMON = ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:spring_water", "minecraft:brown_mushroom_normal",
          "minecraft:red_mushroom_normal"]


def rocks(*ids):
    out = []
    for r in ids:
        out.append(f"runecraft:{r}_rock_surface")
        if ROCKS[r][0] > 0:
            out.append(f"runecraft:{r}_rock_ore")
    return out


def trees(*ids):
    return [f"runecraft:{t}_tree" for t in ids]


def spots(*ids):
    return [f"runecraft:{s}_fishing_spot" for s in ids]


BIOMES = {
    "misthalin": dict(temp=0.8, down=0.4, grass=0x79C05A, foliage=0x59AE30, sky=0x78A7FF, fog=0xC0D8FF, water=0x3F76E4,
                      feats=COMMON + trees("tree", "oak", "willow", "yew") + rocks("copper", "tin", "clay", "iron", "coal", "rune_essence", "pure_essence")
                      + spots("net_bait", "lure_bait") + ["minecraft:flower_default", "minecraft:patch_grass_plain", "minecraft:patch_sugar_cane"],
                      monsters=[("runecraft:goblin", 12, 2, 4), ("runecraft:man", 8, 1, 2), ("runecraft:guard", 3, 1, 2),
                                ("runecraft:barbarian", 3, 1, 3), ("runecraft:dark_wizard", 2, 1, 2), ("minecraft:spider", 3, 1, 1),
                                ("minecraft:zombie", 4, 1, 2), ("minecraft:skeleton", 3, 1, 2)], creatures=FARM),
    "lumbridge_swamp": dict(temp=0.8, down=0.9, grass=0x6A7039, foliage=0x6A7039, sky=0x78A7FF, fog=0xA0B0A0, water=0x617B64,
                            feats=COMMON + trees("tree", "willow") + rocks("copper", "tin", "iron", "coal", "mithril")
                            + spots("net_bait") + ["minecraft:flower_swamp", "minecraft:patch_waterlily", "minecraft:patch_grass_plain"],
                            monsters=[("minecraft:silverfish", 8, 2, 4), ("runecraft:goblin", 6, 1, 3), ("minecraft:spider", 4, 1, 2),
                                      ("minecraft:zombie", 4, 1, 2)], creatures=[("minecraft:frog", 6, 2, 4)]),
    "asgarnia": dict(temp=0.7, down=0.5, grass=0x6FB04F, foliage=0x4FA030, sky=0x78A7FF, fog=0xC0D8FF, water=0x3F76E4,
                     feats=COMMON + trees("tree", "oak", "willow", "yew", "maple") + rocks("copper", "tin", "iron", "coal", "gold", "silver", "mithril", "adamantite", "clay")
                     + spots("net_bait", "lure_bait") + ["minecraft:flower_default", "minecraft:patch_grass_plain"],
                     monsters=[("runecraft:man", 8, 1, 2), ("runecraft:guard", 6, 1, 2), ("runecraft:dark_wizard", 4, 1, 3),
                               ("runecraft:goblin", 6, 2, 4), ("runecraft:hill_giant", 2, 1, 2), ("runecraft:barbarian", 3, 1, 2)],
                     creatures=FARM),
    "wilderness": dict(temp=0.6, down=0.2, grass=0x7A6A3A, foliage=0x5A4A2A, sky=0x705A5A, fog=0x5A4040, water=0x3A4A5A,
                       feats=COMMON + trees("tree", "yew", "magic") + rocks("coal", "iron", "mithril", "adamantite", "runite", "gold")
                       + spots("dark_crab") + ["minecraft:patch_dead_bush_2"],
                       monsters=[("minecraft:skeleton", 8, 1, 3), ("minecraft:zombie", 6, 1, 3), ("runecraft:dark_wizard", 5, 1, 3),
                                 ("runecraft:hill_giant", 5, 1, 2), ("runecraft:moss_giant", 4, 1, 2), ("runecraft:lesser_demon", 3, 1, 1),
                                 ("runecraft:greater_demon", 2, 1, 1), ("runecraft:black_demon", 1, 1, 1), ("runecraft:ice_giant", 2, 1, 2)],
                       creatures=[]),
    "kharidian_desert": dict(temp=2.0, down=0.0, grass=0xBFB755, foliage=0xAEA42A, sky=0x6EB1FF, fog=0xFFE0B0, water=0x32A598,
                             feats=COMMON + rocks("copper", "tin", "iron", "silver", "coal", "gold", "clay", "gem")
                             + ["minecraft:patch_dead_bush_2", "minecraft:patch_cactus_desert"],
                             monsters=[("runecraft:al_kharid_warrior", 10, 1, 3), ("runecraft:man", 4, 1, 2), ("minecraft:husk", 6, 1, 3),
                                       ("runecraft:goblin", 2, 1, 2)], creatures=[]),
    "morytania": dict(temp=0.6, down=0.8, grass=0x4A5A3A, foliage=0x3A4A2A, sky=0x506060, fog=0x405050, water=0x405040,
                      feats=COMMON + trees("tree", "willow", "mahogany") + rocks("iron", "coal", "mithril", "adamantite")
                      + spots("lure_bait") + ["minecraft:flower_swamp", "minecraft:patch_waterlily", "minecraft:patch_grass_plain"],
                      monsters=[("runecraft:ghoul", 10, 1, 3), ("minecraft:zombie", 8, 2, 4), ("minecraft:skeleton", 4, 1, 2),
                                ("runecraft:man", 4, 1, 2), ("runecraft:lesser_demon", 1, 1, 1)], creatures=[]),
    "kandarin": dict(temp=0.75, down=0.6, grass=0x6FC050, foliage=0x50A830, sky=0x78A7FF, fog=0xC0D8FF, water=0x3F76E4,
                     feats=COMMON + trees("tree", "oak", "willow", "maple", "yew", "magic") + rocks("coal", "iron", "gold", "mithril", "clay", "silver", "gem")
                     + spots("lure_bait", "cage_harpoon", "net_harpoon", "net_bait") + ["minecraft:flower_default", "minecraft:patch_grass_plain"],
                     monsters=[("runecraft:man", 8, 1, 2), ("runecraft:guard", 4, 1, 2), ("runecraft:hill_giant", 3, 1, 2),
                               ("runecraft:moss_giant", 2, 1, 2), ("runecraft:dark_wizard", 2, 1, 2), ("runecraft:goblin", 4, 2, 3)],
                     creatures=FARM),
    "feldip_hills": dict(temp=0.9, down=0.5, grass=0x8AB050, foliage=0x6A9A30, sky=0x78A7FF, fog=0xC8D8E8, water=0x3F76E4,
                         feats=COMMON + trees("tree", "oak", "teak", "mahogany") + rocks("copper", "tin", "iron", "coal")
                         + spots("cage_harpoon") + ["minecraft:patch_grass_plain", "minecraft:patch_tall_grass_2"],
                         monsters=[("runecraft:goblin", 8, 2, 4), ("runecraft:hill_giant", 6, 1, 3)], creatures=FARM),
    "fremennik_province": dict(temp=-0.3, down=0.5, grass=0x60A17B, foliage=0x619961, sky=0x7FA1FF, fog=0xD8E8FF, water=0x3D57D6,
                               feats=COMMON + trees("tree", "maple", "yew") + rocks("coal", "iron", "adamantite", "runite", "clay")
                               + spots("cage_harpoon", "net_harpoon") + ["minecraft:patch_grass_taiga", "minecraft:patch_large_fern", "minecraft:freeze_top_layer"],
                               monsters=[("runecraft:barbarian", 8, 1, 3), ("runecraft:ice_giant", 3, 1, 2), ("runecraft:man", 4, 1, 2)],
                               creatures=[("minecraft:wolf", 6, 2, 4), ("minecraft:sheep", 6, 2, 3)]),
    "troll_country": dict(temp=-0.5, down=0.3, grass=0x80B497, foliage=0x60A17B, sky=0x8AA0C0, fog=0xD0D8E0, water=0x3D57D6,
                          feats=COMMON + trees("yew") + rocks("coal", "mithril", "adamantite", "runite") + ["minecraft:freeze_top_layer"],
                          monsters=[("runecraft:hill_giant", 6, 1, 2), ("runecraft:ice_giant", 6, 1, 2), ("runecraft:moss_giant", 2, 1, 1)],
                          creatures=[]),
    "karamja": dict(temp=0.95, down=0.9, grass=0x59C93C, foliage=0x30BB0B, sky=0x77A8FF, fog=0xC0E0C0, water=0x30A0C0,
                    feats=COMMON + trees("tree", "teak", "mahogany", "willow") + rocks("gold", "gem", "coal", "iron")
                    + spots("net_bait", "cage_harpoon", "net_harpoon") + ["minecraft:patch_grass_jungle", "minecraft:patch_sugar_cane"],
                    monsters=[("runecraft:moss_giant", 4, 1, 2), ("runecraft:lesser_demon", 2, 1, 1), ("runecraft:goblin", 4, 2, 3),
                              ("minecraft:spider", 4, 1, 2)], creatures=[("minecraft:parrot", 4, 1, 2), ("minecraft:chicken", 4, 1, 2)]),
    "tirannwn": dict(temp=0.7, down=0.8, grass=0x50B060, foliage=0x40A050, sky=0x80B0FF, fog=0xD0F0E0, water=0x3F86E4,
                     feats=COMMON + trees("yew", "magic", "teak", "redwood", "maple") + rocks("coal", "mithril", "adamantite", "amethyst", "pure_essence")
                     + spots("angler", "lure_bait") + ["minecraft:flower_default", "minecraft:patch_large_fern"],
                     monsters=[("runecraft:guard", 6, 1, 2), ("runecraft:moss_giant", 3, 1, 2)], creatures=FARM),
    "uncharted_isles": dict(temp=0.8, down=0.6, grass=0x79C05A, foliage=0x59AE30, sky=0x78A7FF, fog=0xC0D8FF, water=0x3F76E4,
                            feats=COMMON + trees("tree", "oak", "redwood") + rocks("copper", "tin", "iron", "coal", "amethyst")
                            + spots("angler", "net_bait") + ["minecraft:patch_grass_plain"],
                            monsters=[("runecraft:goblin", 6, 1, 3)], creatures=FARM),
}

for name, d in BIOMES.items():
    effects = {"sky_color": d["sky"], "fog_color": d["fog"], "water_color": d["water"], "water_fog_color": 0x050533,
               "grass_color": d["grass"], "foliage_color": d["foliage"],
               "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}}
    write(f"runecraft/worldgen/biome/{name}.json", {
        "has_precipitation": d["temp"] < 1.5, "temperature": d["temp"], "downfall": d["down"],
        "effects": effects,
        "spawners": spawns(d["monsters"], d["creatures"]),
        "spawn_costs": {},
        "carvers": {"air": ["minecraft:cave", "minecraft:canyon"]},
        "features": features(set(d["feats"]))})

# ---------------------------------------------------------------- damage type & tags
write("runecraft/damage_type/osrs_hit.json", {"exhaustion": 0.0, "message_id": "runecraft.osrs_hit", "scaling": "never"})
for tag in ["bypasses_armor", "bypasses_cooldown", "bypasses_shield", "no_knockback"]:
    if tag == "no_knockback":
        continue
    write(f"minecraft/tags/damage_type/{tag}.json", {"replace": False, "values": ["runecraft:osrs_hit"]})

catalog = json.load(open("build/catalog.json"))
arrows = [f"runecraft:{i['id']}" for i in catalog["items"] if i["kind"] == "ARROW"]
write("minecraft/tags/items/arrows.json", {"replace": False, "values": arrows})
logs = [f"runecraft:{t['id']}_tree_log" for t in catalog["trees"]]
write("minecraft/tags/blocks/logs.json", {"replace": False, "values": logs})
stations = ["bank_booth", "furnace", "anvil", "cooking_range", "prayer_altar", "general_store", "rune_shop", "weapon_shop",
            "tool_shop", "archery_shop", "spinning_wheel", "tanning_rack", "construction_workbench", "slayer_master",
            "apothecary_table", "gielinor_portal"]
write("minecraft/tags/blocks/mineable/pickaxe.json", {"replace": False, "values": [f"runecraft:{s}" for s in stations]})

# ---------------------------------------------------------------- recipes
write("runecraft/recipes/gielinor_portal.json", {
    "type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["OEO", "EDE", "OEO"],
    "key": {"O": {"item": "minecraft:obsidian"}, "E": {"item": "minecraft:ender_pearl"}, "D": {"item": "minecraft:diamond"}},
    "result": {"item": "runecraft:gielinor_portal", "count": 1}})

print(f"Wrote {written} data files")
