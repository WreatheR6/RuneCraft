package com.runecraft.osrs.data;

import com.runecraft.osrs.content.BossDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OSRS Wiki drop-table corrections for bosses, the boss counterpart of {@link MonsterDropsTable}: applied by
 * {@link com.runecraft.osrs.content.ContentRegistry#bosses()} to module bosses and by {@link Boss#drops()} to the
 * built-in ones (key {@code Boss.<NAME>}).
 *
 * <p>Generated from the wiki drop tables in Flipping-Utilities/parsed-osrs {@code all-monsters.json} (snapshot in
 * {@code src/test/resources/osrs/bosses/boss_drops.tsv}, diffed by {@code BossDropsTableTest}). Each patch replaces
 * every line of the items it names (wiki rarity 1/N rounded to a whole N, one line per wiki quantity; noted
 * quantities drop as plain stacks), removes common invented lines the wiki table doesn't list and appends the rest.
 * The dump often leaves out a boss's unique rows, so rare lines (1/50 or rarer) are never removed; pets, clue
 * scrolls, wilderness/brimstone keys, looting bags, frozen key pieces, awakened-only and other conditional rows are
 * left to the code that hands them out. Bosses whose wiki page has no ordinary per-kill table (Barrows, Gauntlet,
 * Moons, Colosseum, Zalcano, Doom delves) or whose dump row is wrong (Obor, Bryophyta, the Mimic) have no patch.
 *
 * <p>The collection log is not built from these lines: boss pages use the wiki's page contents
 * ({@code OsrsPages}), so adding common wiki drops here never adds log slots.
 */
public final class BossDropsTable {
    private static final Map<String, MonsterDropsTable.Patch> PATCHES = new LinkedHashMap<>();

    static {
        rows0(); rows1(); rows2(); rows3(); rows4(); rows5();
    }

    private BossDropsTable() {
    }

    /** Patch key of a built-in {@link Boss}. */
    public static String key(Boss b) {
        return "Boss." + b.name();
    }

    /** Every patch, keyed by boss id (or {@code Boss.<NAME>}). */
    public static Map<String, MonsterDropsTable.Patch> patches() {
        return Collections.unmodifiableMap(PATCHES);
    }

    /** {@code drops} with the patch for {@code key} applied (the same list when there is none). */
    public static List<Drop> patched(String key, List<Drop> drops) {
        MonsterDropsTable.Patch p = PATCHES.get(key);
        return p == null ? drops : MonsterDropsTable.applied(p, drops);
    }

    /** Applies the patch for a module boss in place; returns false when it has none. */
    public static boolean apply(BossDef boss) {
        if (!PATCHES.containsKey(boss.id)) {
            return false;
        }
        List<Drop> next = patched(boss.id, new ArrayList<>(boss.drops));
        boss.drops.clear();
        boss.drops.addAll(next);
        return true;
    }

    private static void p(String key, String[] remove, Drop... put) {
        PATCHES.put(key, new MonsterDropsTable.Patch(List.of(put), Set.of(remove)));
    }

    private static String[] rm(String... items) {
        return items;
    }

    private static Drop d(String item, int min, int max, int rarity) {
        return new Drop(item, min, max, rarity);
    }

    private static void rows0() {
        // Boss.GENERAL_GRAARDOR <- wiki:General Graardor(624)
        p("Boss.GENERAL_GRAARDOR", rm(),
                d("big_bones", 1, 1, 1), d("bandos_chestplate", 1, 1, 381), d("bandos_tassets", 1, 1, 381),
                d("bandos_boots", 1, 1, 381), d("rune_longsword", 1, 1, 16), d("rune_2h_sword", 1, 1, 16),
                d("rune_platebody", 1, 1, 16), d("rune_pickaxe", 1, 1, 21), d("coins", 19500, 20000, 4),
                d("grimy_snapdragon", 3, 3, 16), d("snapdragon_seed", 1, 1, 16), d("super_restore", 3, 3, 16),
                d("adamantite_ore", 15, 20, 16), d("coal", 115, 120, 16), d("magic_logs", 15, 20, 16),
                d("nature_rune", 65, 70, 16), d("coins", 20100, 20600, 102), d("long_bone", 1, 1, 400),
                d("curved_bone", 1, 1, 5012));
        // Boss.KRIL_TSUTSAROTH <- wiki:K'ril Tsutsaroth(650)
        p("Boss.KRIL_TSUTSAROTH", rm("wine_of_zamorak"),
                d("infernal_ashes", 1, 1, 1), d("steam_battlestaff", 1, 1, 127), d("zamorakian_spear", 1, 1, 127),
                d("staff_of_the_dead", 1, 1, 508), d("rune_scimitar", 1, 1, 16), d("adamant_platebody", 1, 1, 16),
                d("rune_platelegs", 1, 1, 18), d("super_attack(3)", 3, 3, 16), d("super_strength(3)", 3, 3, 16),
                d("super_restore(3)", 3, 3, 16), d("zamorak_brew(3)", 3, 3, 16), d("coins", 19500, 20000, 3),
                d("grimy_lantadyme", 10, 10, 16), d("lantadyme_seed", 3, 3, 16), d("death_rune", 120, 125, 16),
                d("blood_rune", 80, 85, 16));
        // kreearra <- wiki:Kree'arra(580)
        p("kreearra", rm("black_dragonhide", "runite_ore"),
                d("big_bones", 1, 1, 1), d("armadyl_helmet", 1, 1, 381), d("armadyl_chestplate", 1, 1, 381),
                d("armadyl_chainskirt", 1, 1, 381), d("armadyl_hilt", 1, 1, 508), d("godsword_shard_1", 1, 1, 762),
                d("godsword_shard_2", 1, 1, 762), d("godsword_shard_3", 1, 1, 762), d("black_dhide_body", 1, 1, 16),
                d("rune_crossbow", 1, 1, 16), d("mind_rune", 586, 601, 16), d("rune_arrow", 100, 105, 16),
                d("rune_bolts", 20, 25, 16), d("dragonstone_bolts_e", 5, 10, 16), d("coins", 19500, 20000, 3),
                d("ranging_potion(3)", 3, 3, 16), d("super_defence(3)", 3, 3, 16), d("grimy_dwarf_weed", 8, 13, 16),
                d("dwarf_weed_seed", 2, 2, 16), d("coins", 20500, 21000, 102), d("crystal_key", 1, 1, 127),
                d("yew_seed", 1, 1, 127), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // commander_zilyana <- wiki:Commander Zilyana(596)
        p("commander_zilyana", rm(),
                d("bones", 1, 1, 1), d("saradomin_sword", 1, 1, 127), d("saradomins_light", 1, 1, 254),
                d("armadyl_crossbow", 1, 1, 508), d("saradomin_hilt", 1, 1, 508), d("godsword_shard_1", 1, 1, 762),
                d("godsword_shard_2", 1, 1, 762), d("godsword_shard_3", 1, 1, 762), d("adamant_platebody", 1, 1, 16),
                d("rune_dart", 35, 40, 16), d("rune_kiteshield", 1, 1, 16), d("rune_plateskirt", 1, 1, 16),
                d("prayer_potion", 3, 3, 16), d("saradomin_brew(3)", 3, 3, 21), d("super_restore", 3, 3, 21),
                d("super_defence(3)", 3, 3, 16), d("magic_potion(3)", 3, 3, 16), d("coins", 19500, 20000, 4),
                d("diamond", 6, 6, 16), d("law_rune", 95, 100, 16), d("grimy_ranarr_weed", 5, 5, 16), d("ranarr_seed", 2, 2, 16),
                d("magic_seed", 1, 1, 127));
        // nex <- wiki:Nex(1001)
        p("nex", rm("dragon_bones", "magic_logs"),
                d("big_bones", 1, 1, 1), d("ancient_hilt", 1, 1, 516), d("nihil_horn", 1, 1, 258),
                d("torva_full_helm", 1, 1, 258), d("torva_platebody", 1, 1, 258), d("torva_platelegs", 1, 1, 258),
                d("zaryte_vambraces", 1, 1, 172), d("blood_essence", 1, 2, 82), d("rune_sword", 1, 1, 209),
                d("nihil_shard", 80, 85, 16), d("nihil_shard", 85, 95, 26));
        // fumus <- wiki:Fumus(285)
        p("fumus", rm("bones"),
                d("ancient_ceremonial_mask", 1, 1, 1000), d("ancient_ceremonial_top", 1, 1, 1000),
                d("ancient_ceremonial_legs", 1, 1, 1000), d("ancient_ceremonial_gloves", 1, 1, 1000),
                d("ancient_ceremonial_boots", 1, 1, 1000));
        // umbra <- wiki:Umbra(285)
        p("umbra", rm("bones"),
                d("ancient_ceremonial_mask", 1, 1, 1000), d("ancient_ceremonial_top", 1, 1, 1000),
                d("ancient_ceremonial_legs", 1, 1, 1000), d("ancient_ceremonial_gloves", 1, 1, 1000),
                d("ancient_ceremonial_boots", 1, 1, 1000));
        // cruor <- wiki:Cruor(285)
        p("cruor", rm("bones"),
                d("ancient_ceremonial_mask", 1, 1, 1000), d("ancient_ceremonial_top", 1, 1, 1000),
                d("ancient_ceremonial_legs", 1, 1, 1000), d("ancient_ceremonial_gloves", 1, 1, 1000),
                d("ancient_ceremonial_boots", 1, 1, 1000));
        // glacies <- wiki:Glacies(285)
        p("glacies", rm("bones"),
                d("ancient_ceremonial_mask", 1, 1, 1000), d("ancient_ceremonial_top", 1, 1, 1000),
                d("ancient_ceremonial_legs", 1, 1, 1000), d("ancient_ceremonial_gloves", 1, 1, 1000),
                d("ancient_ceremonial_boots", 1, 1, 1000));
        // callisto <- wiki:Callisto(470)
        p("callisto", rm("mahogany_plank", "runite_ore"),
                d("big_bones", 1, 1, 1), d("claws_of_callisto", 1, 1, 196), d("dragon_2h_sword", 1, 1, 256),
                d("dragon_pickaxe", 1, 1, 256), d("voidwaker_hilt", 1, 1, 360), d("tyrannical_ring", 1, 1, 512),
                d("mystic_earth_staff", 4, 4, 63), d("mystic_robe_top", 4, 4, 63), d("mystic_robe_bottom", 4, 4, 63),
                d("rune_pickaxe", 5, 5, 16), d("rune_2h_sword", 3, 3, 42), d("rune_kiteshield", 4, 4, 63),
                d("rune_platebody", 4, 4, 63), d("blood_rune", 500, 500, 18), d("death_rune", 700, 700, 18),
                d("chaos_rune", 900, 900, 18), d("soul_rune", 450, 450, 25), d("cannonball", 600, 600, 32),
                d("uncut_ruby", 50, 50, 32), d("uncut_diamond", 25, 25, 42), d("uncut_dragonstone", 5, 5, 63),
                d("limpwurt_root", 100, 100, 25), d("magic_logs", 225, 225, 25), d("mahogany_logs", 600, 600, 21),
                d("red_dragonhide", 170, 170, 42), d("coconut", 135, 135, 63), d("grimy_toadflax", 100, 100, 126),
                d("grimy_dwarf_weed", 45, 45, 126), d("grimy_ranarr_weed", 45, 45, 126), d("grimy_snapdragon", 45, 45, 126),
                d("ranarr_seed", 11, 11, 25), d("snapdragon_seed", 5, 5, 25), d("coins", 50000, 50000, 13),
                d("dark_crab", 50, 50, 25), d("super_restore", 10, 10, 25), d("supercompost", 225, 225, 32),
                d("dragon_bones", 75, 75, 42), d("anglerfish", 100, 100, 126), d("anglerfish", 5, 6, 18),
                d("cooked_karambwan", 5, 6, 18), d("super_restore(3)", 3, 4, 18), d("super_restore", 3, 4, 18),
                d("ranging_potion(2)", 2, 3, 18), d("super_combat_potion(2)", 2, 3, 18), d("long_bone", 1, 1, 400),
                d("curved_bone", 1, 1, 5012));
        // artio <- wiki:Artio(320)
        p("artio", rm("runite_ore"),
                d("big_bones", 1, 1, 1), d("dragon_2h_sword", 1, 1, 358), d("dragon_pickaxe", 1, 1, 358),
                d("claws_of_callisto", 1, 1, 618), d("tyrannical_ring", 1, 1, 716), d("voidwaker_hilt", 1, 1, 912),
                d("dark_crab", 9, 9, 26), d("super_restore", 3, 3, 26), d("rune_2h_sword", 1, 1, 43),
                d("rune_pickaxe", 1, 1, 11), d("chaos_rune", 300, 300, 18), d("death_rune", 220, 220, 18),
                d("blood_rune", 140, 140, 18), d("soul_rune", 150, 150, 26), d("cannonball", 190, 190, 32),
                d("uncut_ruby", 17, 17, 32), d("uncut_diamond", 7, 7, 43), d("uncut_dragonstone", 1, 1, 64),
                d("limpwurt_root", 20, 20, 26), d("magic_logs", 60, 60, 26), d("mahogany_logs", 200, 200, 21),
                d("dragon_bones", 25, 25, 43), d("grimy_toadflax", 40, 40, 128), d("coconut", 30, 30, 64),
                d("red_dragonhide", 55, 55, 43), d("coins", 12000, 12000, 6), d("dark_fishing_bait", 300, 300, 128),
                d("supercompost", 60, 60, 32), d("yew_seed", 1, 1, 128), d("magic_seed", 1, 1, 128), d("ranarr_seed", 3, 3, 26),
                d("snapdragon_seed", 1, 1, 26), d("palm_seed", 1, 1, 128), d("anglerfish", 5, 6, 18),
                d("cooked_karambwan", 5, 6, 18), d("super_restore(3)", 3, 4, 18), d("super_restore", 3, 4, 18),
                d("ranging_potion(2)", 2, 3, 18), d("super_combat_potion(2)", 2, 3, 18), d("long_bone", 1, 1, 400),
                d("curved_bone", 1, 1, 5012));
        // venenatis <- wiki:Venenatis(464)
        p("venenatis", rm("mahogany_plank", "runite_ore"),
                d("big_bones", 1, 1, 1), d("fangs_of_venenatis", 1, 1, 196), d("dragon_2h_sword", 1, 1, 256),
                d("dragon_pickaxe", 1, 1, 256), d("voidwaker_gem", 1, 1, 360), d("treasonous_ring", 1, 1, 512),
                d("mystic_air_staff", 4, 4, 63), d("rune_pickaxe", 5, 5, 16), d("rune_dart", 150, 150, 42),
                d("rune_knife", 150, 150, 25), d("rune_platelegs", 4, 4, 63), d("rune_sq_shield", 4, 4, 63),
                d("dragon_dagger", 6, 6, 63), d("chaos_rune", 500, 500, 18), d("death_rune", 700, 700, 18),
                d("blood_rune", 900, 900, 18), d("diamond_bolts_e", 300, 300, 25), d("cannonball", 600, 600, 32),
                d("uncut_ruby", 50, 50, 126), d("uncut_diamond", 25, 25, 16), d("gold_ore", 675, 675, 21),
                d("onyx_bolt_tips", 150, 150, 25), d("magic_logs", 225, 225, 25), d("limpwurt_root", 100, 100, 25),
                d("red_spiders_eggs", 500, 500, 42), d("unicorn_horn", 225, 225, 126), d("uncut_dragonstone", 5, 5, 63),
                d("grimy_ranarr_weed", 45, 45, 126), d("grimy_snapdragon", 100, 100, 126), d("grimy_toadflax", 45, 45, 126),
                d("battlestaff", 12, 12, 63), d("coins", 50000, 50000, 13), d("supercompost", 225, 225, 32),
                d("antidote_plus_plus", 20, 20, 42), d("super_restore", 10, 10, 25), d("dark_crab", 50, 50, 25),
                d("anglerfish", 100, 100, 126), d("anglerfish", 5, 6, 18), d("cooked_karambwan", 5, 6, 18),
                d("super_restore(3)", 3, 4, 18), d("super_restore", 3, 4, 18), d("ranging_potion(2)", 2, 3, 18),
                d("super_combat_potion(2)", 2, 3, 18), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
    }

    private static void rows1() {
        // spindel <- wiki:Spindel(302)
        p("spindel", rm("runite_ore"),
                d("big_bones", 1, 1, 1), d("dragon_2h_sword", 1, 1, 358), d("dragon_pickaxe", 1, 1, 358),
                d("fangs_of_venenatis", 1, 1, 618), d("treasonous_ring", 1, 1, 716), d("voidwaker_gem", 1, 1, 912),
                d("dark_crab", 8, 8, 26), d("super_restore", 3, 3, 26), d("rune_knife", 30, 30, 26), d("rune_pickaxe", 1, 1, 11),
                d("rune_2h_sword", 1, 1, 43), d("chaos_rune", 180, 180, 18), d("blood_rune", 150, 150, 18),
                d("death_rune", 220, 220, 18), d("cannonball", 200, 200, 32), d("diamond_bolts_e", 60, 60, 26),
                d("uncut_diamond", 7, 7, 16), d("gold_ore", 180, 180, 21), d("magic_logs", 60, 60, 26),
                d("limpwurt_root", 18, 18, 26), d("onyx_bolt_tips", 35, 35, 26), d("red_spiders_eggs", 250, 250, 43),
                d("uncut_dragonstone", 1, 1, 64), d("uncut_ruby", 14, 14, 128), d("grimy_snapdragon", 40, 40, 128),
                d("unicorn_horn", 60, 60, 128), d("coins", 14000, 14000, 6), d("supercompost", 60, 60, 32),
                d("antidote_plus_plus", 6, 6, 43), d("dark_fishing_bait", 200, 200, 128), d("magic_seed", 1, 1, 128),
                d("palm_seed", 1, 1, 128), d("anglerfish", 5, 6, 11), d("cooked_karambwan", 5, 6, 11),
                d("super_restore(3)", 3, 4, 11), d("super_restore", 3, 4, 11), d("ranging_potion(2)", 2, 3, 11),
                d("super_combat_potion(2)", 2, 3, 11));
        // vetion <- wiki:Vet'ion(454)
        p("vetion", rm("mahogany_plank", "runite_ore"),
                d("big_bones", 1, 1, 1), d("skull_of_vetion", 1, 1, 196), d("dragon_2h_sword", 1, 1, 256),
                d("dragon_pickaxe", 1, 1, 256), d("voidwaker_blade", 1, 1, 360), d("ring_of_the_gods", 1, 1, 512),
                d("mystic_fire_staff", 4, 4, 63), d("mystic_water_staff", 4, 4, 63), d("mystic_robe_top", 4, 4, 63),
                d("mystic_robe_bottom", 4, 4, 63), d("rune_full_helm", 4, 4, 63), d("rune_pickaxe", 5, 5, 16),
                d("rune_dart", 150, 150, 42), d("rune_knife", 150, 150, 42), d("chaos_rune", 900, 900, 18),
                d("death_rune", 700, 700, 18), d("blood_rune", 500, 500, 18), d("cannonball", 550, 550, 32),
                d("uncut_ruby", 50, 50, 32), d("uncut_diamond", 25, 25, 42), d("uncut_dragonstone", 5, 5, 63),
                d("gold_ore", 675, 675, 21), d("limpwurt_root", 60, 60, 25), d("wine_of_zamorak", 100, 100, 25),
                d("magic_logs", 225, 225, 25), d("oak_plank", 400, 400, 25), d("dragon_bones", 150, 150, 42),
                d("mort_myre_fungus", 450, 450, 63), d("grimy_ranarr_weed", 100, 100, 126), d("grimy_dwarf_weed", 45, 45, 126),
                d("grimy_snapdragon", 45, 45, 126), d("grimy_toadflax", 45, 45, 126), d("coins", 50000, 50000, 13),
                d("supercompost", 225, 225, 32), d("super_restore", 10, 10, 25), d("anglerfish", 100, 100, 126),
                d("sanfew_serum", 20, 20, 25), d("dark_crab", 50, 50, 25), d("anglerfish", 5, 6, 18),
                d("cooked_karambwan", 5, 6, 18), d("super_restore(3)", 3, 4, 18), d("super_restore", 3, 4, 18),
                d("ranging_potion(2)", 2, 3, 18), d("super_combat_potion(2)", 2, 3, 18), d("long_bone", 1, 1, 400),
                d("skeleton_champion_scroll", 1, 1, 5000), d("curved_bone", 1, 1, 5012));
        // calvarion <- wiki:Calvar'ion(264)
        p("calvarion", rm("runite_ore"),
                d("big_bones", 1, 1, 1), d("dragon_2h_sword", 1, 1, 358), d("dragon_pickaxe", 1, 1, 358),
                d("skull_of_vetion", 1, 1, 618), d("ring_of_the_gods", 1, 1, 716), d("voidwaker_blade", 1, 1, 912),
                d("dark_crab", 8, 8, 26), d("super_restore", 3, 3, 26), d("ancient_staff", 1, 1, 43),
                d("rune_2h_sword", 1, 1, 43), d("rune_pickaxe", 1, 1, 11), d("chaos_rune", 220, 220, 18),
                d("death_rune", 120, 120, 18), d("blood_rune", 180, 180, 18), d("cannonball", 180, 180, 32),
                d("uncut_ruby", 17, 17, 32), d("uncut_diamond", 7, 7, 43), d("uncut_dragonstone", 1, 1, 64),
                d("gold_ore", 200, 200, 21), d("limpwurt_root", 19, 19, 26), d("magic_logs", 60, 60, 26),
                d("oak_plank", 220, 220, 26), d("dragon_bones", 60, 60, 43), d("mort_myre_fungus", 120, 120, 64),
                d("wine_of_zamorak", 35, 35, 26), d("grimy_ranarr_weed", 60, 60, 128), d("coins", 12000, 12000, 6),
                d("yew_seed", 1, 1, 128), d("magic_seed", 1, 1, 128), d("palm_seed", 1, 1, 128), d("supercompost", 60, 60, 32),
                d("sanfew_serum", 6, 6, 26), d("dark_fishing_bait", 280, 280, 128), d("anglerfish", 5, 6, 18),
                d("cooked_karambwan", 5, 6, 18), d("super_restore(3)", 3, 4, 18), d("super_restore", 3, 4, 18),
                d("ranging_potion(2)", 2, 3, 18), d("super_combat_potion(2)", 2, 3, 18), d("long_bone", 1, 1, 400),
                d("curved_bone", 1, 1, 5012), d("skeleton_champion_scroll", 1, 1, 5000));
        // scorpia <- wiki:Scorpia(225)
        p("scorpia", rm("big_bones", "dark_crab", "rune_platebody"),
                d("odium_shard_3", 1, 1, 256), d("malediction_shard_3", 1, 1, 256), d("battlestaff", 5, 8, 21),
                d("rune_2h_sword", 1, 1, 26), d("rune_pickaxe", 1, 1, 26), d("rune_kiteshield", 1, 1, 26),
                d("rune_chainbody", 1, 1, 32), d("rune_platelegs", 1, 1, 32), d("rune_scimitar", 1, 1, 32),
                d("rune_warhammer", 1, 1, 32), d("mystic_earth_staff", 1, 1, 32), d("mystic_robe_top", 1, 1, 128),
                d("mystic_robe_bottom", 1, 1, 128), d("dragon_scimitar", 1, 1, 128), d("dragon_2h_sword", 1, 1, 128),
                d("death_rune", 100, 150, 16), d("blood_rune", 100, 150, 16), d("chaos_rune", 150, 200, 16),
                d("grimy_kwuarm", 10, 15, 26), d("grimy_dwarf_weed", 10, 15, 26), d("grimy_torstol", 10, 15, 26),
                d("grimy_snapdragon", 4, 7, 26), d("uncut_ruby", 15, 20, 21), d("uncut_diamond", 10, 15, 32),
                d("runite_ore", 3, 3, 32), d("dragon_javelin_heads", 30, 50, 32), d("onyx_bolt_tips", 6, 10, 64),
                d("coins", 25002, 34962, 18), d("anglerfish", 15, 25, 26), d("super_restore", 5, 5, 26),
                d("ensouled_scorpion_head", 1, 1, 18));
        // chaos_elemental <- wiki:Chaos Elemental(305)
        p("chaos_elemental", rm("runite_ore"),
                d("anglerfish", 2, 2, 5), d("cooked_karambwan", 3, 3, 5), d("super_restore", 1, 1, 5),
                d("dragon_bones", 1, 1, 5), d("super_combat_potion(1)", 1, 1, 5), d("dragon_pickaxe", 1, 1, 256),
                d("rune_dart", 100, 100, 26), d("rune_platelegs", 1, 1, 32), d("rune_plateskirt", 1, 1, 32),
                d("rune_full_helm", 1, 1, 43), d("mystic_air_staff", 1, 1, 43), d("mystic_water_staff", 1, 1, 43),
                d("mystic_earth_staff", 1, 1, 43), d("mystic_fire_staff", 1, 1, 43), d("dragon_dagger", 1, 1, 64),
                d("dragon_2h_sword", 1, 1, 64), d("dragon_platelegs", 1, 1, 64), d("dragon_plateskirt", 1, 1, 64),
                d("chaos_rune", 300, 500, 16), d("blood_rune", 100, 250, 16), d("grimy_ranarr_weed", 5, 8, 32),
                d("grimy_snapdragon", 5, 8, 32), d("grimy_avantoe", 5, 8, 43), d("grimy_kwuarm", 5, 8, 43),
                d("coal", 75, 150, 26), d("mahogany_plank", 8, 16, 26), d("adamantite_bar", 8, 12, 32),
                d("cooked_karambwan", 15, 25, 32), d("anglerfish", 10, 15, 32), d("weapon_poison_plus_plus", 1, 1, 13),
                d("#rare", 1, 1, 16));
        // chaos_fanatic <- wiki:Chaos Fanatic(202)
        p("chaos_fanatic", rm(),
                d("bones", 1, 1, 1), d("odium_shard_1", 1, 1, 256), d("malediction_shard_1", 1, 1, 256),
                d("battlestaff", 5, 5, 26), d("ancient_staff", 1, 1, 128), d("fire_rune", 250, 250, 32),
                d("smoke_rune", 30, 30, 32), d("chaos_rune", 175, 175, 32), d("blood_rune", 50, 50, 32), d("monkfish", 3, 3, 16),
                d("shark", 1, 1, 16), d("prayer_potion", 1, 1, 16), d("anchovy_pizza", 8, 8, 32), d("grimy_lantadyme", 4, 4, 16),
                d("ring_of_life", 1, 1, 18), d("chaos_talisman", 1, 1, 21), d("wine_of_zamorak", 10, 10, 21),
                d("pure_essence", 250, 250, 64), d("#rare", 1, 1, 32), d("#gems", 1, 1, 32));
        // crazy_archaeologist <- wiki:Crazy archaeologist(204)
        p("crazy_archaeologist", rm(),
                d("bones", 1, 1, 1), d("odium_shard_2", 1, 1, 256), d("malediction_shard_2", 1, 1, 256),
                d("amulet_of_power", 1, 1, 18), d("rune_crossbow", 2, 2, 26), d("red_dhide_body", 1, 1, 32),
                d("rune_knife", 10, 10, 32), d("mud_rune", 30, 30, 32), d("cannonball", 150, 150, 32),
                d("dragon_arrow", 75, 75, 128), d("shark", 1, 1, 16), d("potato_with_cheese", 3, 3, 16),
                d("prayer_potion", 1, 1, 16), d("anchovy_pizza", 8, 8, 32), d("grimy_dwarf_weed", 4, 4, 16),
                d("white_berries", 10, 10, 21), d("red_dragonhide", 10, 10, 26), d("rusty_sword", 1, 1, 32),
                d("muddy_key", 1, 1, 32), d("onyx_bolt_tips", 12, 12, 32), d("long_bone", 1, 1, 64), d("#rare", 1, 1, 32),
                d("#gems", 1, 1, 32));
        // king_black_dragon <- wiki:King Black Dragon(276)
        p("king_black_dragon", rm(),
                d("dragon_bones", 1, 1, 1), d("black_dragonhide", 2, 2, 1), d("dragon_pickaxe", 1, 1, 1000),
                d("rune_longsword", 1, 1, 13), d("adamant_platebody", 1, 1, 14), d("adamant_kiteshield", 1, 1, 43),
                d("fire_rune", 300, 300, 26), d("air_rune", 300, 300, 13), d("iron_arrow", 690, 690, 13),
                d("rune_bolts", 10, 20, 13), d("blood_rune", 30, 30, 26), d("yew_logs", 150, 150, 13),
                d("adamantite_bar", 3, 3, 26), d("gold_ore", 100, 100, 64), d("amulet_of_power", 1, 1, 18),
                d("dragon_arrowtips", 5, 14, 26), d("dragon_dart_tip", 5, 14, 26), d("dragon_javelin_heads", 15, 15, 26),
                d("rune_limbs", 1, 1, 32), d("shark", 4, 4, 32), d("kbd_heads", 1, 1, 128), d("draconic_visage", 1, 1, 5000),
                d("#rare", 1, 1, 16), d("#gems", 1, 1, 64));
        // giant_mole <- wiki:Giant Mole(230)
        p("giant_mole", rm("#herbs"),
                d("big_bones", 1, 1, 1), d("mole_claw", 1, 1, 1), d("mole_skin", 1, 3, 1), d("adamant_longsword", 1, 1, 13),
                d("mithril_platebody", 1, 1, 15), d("amulet_of_strength", 1, 1, 19), d("mithril_axe", 1, 1, 66),
                d("mithril_battleaxe", 1, 1, 131), d("rune_med_helm", 1, 1, 131), d("air_rune", 105, 105, 7),
                d("blood_rune", 15, 15, 7), d("fire_rune", 105, 105, 12), d("iron_arrow", 690, 690, 13),
                d("yew_logs", 100, 100, 13), d("shark", 4, 4, 33), d("mithril_bar", 1, 1, 44), d("ranarr_seed", 1, 1, 44),
                d("iron_ore", 100, 100, 66), d("oyster_pearls", 1, 1, 131), d("long_bone", 1, 1, 400),
                d("curved_bone", 1, 1, 5012), d("#rare", 1, 1, 33), d("#gems", 1, 1, 22));
        // kalphite_queen <- wiki:Kalphite Queen(333)
        p("kalphite_queen", rm("bones", "rune_platebody"),
                d("monkfish", 3, 3, 9), d("shark", 2, 2, 9), d("dark_crab", 2, 2, 9), d("saradomin_brew", 1, 1, 9),
                d("prayer_potion", 2, 2, 9), d("super_restore", 1, 1, 9), d("super_combat_potion(2)", 1, 1, 9),
                d("ranging_potion(3)", 1, 1, 9), d("superantipoison(2)", 1, 1, 9), d("battlestaff", 10, 10, 25),
                d("rune_chainbody", 1, 1, 32), d("red_dhide_body", 1, 1, 32), d("lava_battlestaff", 1, 1, 63),
                d("blood_rune", 100, 100, 21), d("mithril_arrow", 500, 500, 25), d("grimy_toadflax", 25, 25, 63),
                d("grimy_ranarr_weed", 25, 25, 63), d("grimy_snapdragon", 25, 25, 63), d("grimy_torstol", 25, 25, 63),
                d("torstol_seed", 2, 2, 32), d("watermelon_seed", 25, 25, 42), d("papaya_seed", 2, 2, 42),
                d("palm_seed", 2, 2, 42), d("magic_seed", 2, 2, 42), d("bucket_of_sand", 100, 100, 32),
                d("gold_ore", 250, 250, 32), d("magic_logs", 60, 60, 32), d("wine_of_zamorak", 60, 60, 13),
                d("potato_cactus", 100, 100, 16), d("grapes", 100, 100, 25), d("weapon_poison_plus_plus", 5, 5, 25),
                d("cactus_spine", 10, 10, 42), d("ensouled_kalphite_head", 1, 1, 20), d("kq_head", 1, 1, 128),
                d("dragon_chainbody", 1, 1, 128), d("dragon_2h_sword", 1, 1, 256), d("dragon_pickaxe", 1, 1, 400),
                d("jar_of_sand", 1, 1, 2000), d("#rare", 1, 1, 126));
        // dagannoth_rex <- wiki:Dagannoth Rex(303)
        p("dagannoth_rex", rm("raw_shark"),
                d("dagannoth_bones", 1, 1, 1), d("dagannoth_hide", 1, 1, 1), d("steel_kiteshield", 1, 1, 8),
                d("mithril_warhammer", 1, 1, 12), d("adamant_axe", 1, 1, 18), d("steel_platebody", 1, 1, 32),
                d("mithril_pickaxe", 1, 1, 43), d("adamant_platebody", 1, 1, 64), d("fremennik_blade", 1, 1, 64),
                d("dragon_axe", 1, 1, 128), d("rune_axe", 1, 1, 128), d("fremennik_shield", 1, 1, 128),
                d("fremennik_helm", 1, 1, 128), d("mithril_2h_sword", 1, 1, 128), d("ring_of_life", 1, 1, 128),
                d("rock_shell_plate", 1, 1, 128), d("rock_shell_legs", 1, 1, 128), d("berserker_ring", 1, 1, 128),
                d("warrior_ring", 1, 1, 128), d("antifire_potion(2)", 1, 1, 128), d("prayer_potion(2)", 1, 1, 128),
                d("restore_potion(2)", 1, 1, 128), d("super_attack(2)", 1, 1, 128), d("super_strength(2)", 1, 1, 128),
                d("super_defence(2)", 1, 1, 128), d("zamorak_brew(2)", 1, 1, 128), d("mithril_ore", 25, 25, 13),
                d("adamantite_bar", 1, 1, 43), d("coal", 100, 100, 64), d("iron_ore", 150, 150, 128),
                d("steel_bar", 15, 30, 128), d("grimy_ranarr_weed", 1, 1, 18), d("bass", 5, 5, 18), d("swordfish", 5, 5, 32),
                d("shark", 5, 5, 128), d("ensouled_dagannoth_head", 1, 1, 20), d("#rare", 1, 1, 16), d("#gems", 1, 1, 13));
        // dagannoth_prime <- wiki:Dagannoth Prime(303)
        p("dagannoth_prime", rm("adamant_platebody", "raw_shark", "rune_axe", "steel_bar"),
                d("dagannoth_bones", 1, 1, 1), d("dagannoth_hide", 1, 1, 1), d("earth_battlestaff", 1, 1, 13),
                d("water_battlestaff", 1, 1, 26), d("air_battlestaff", 1, 1, 32), d("battlestaff", 1, 10, 128),
                d("fremennik_blade", 1, 1, 128), d("fremennik_shield", 1, 1, 128), d("fremennik_helm", 1, 1, 128),
                d("mud_battlestaff", 1, 1, 128), d("dragon_axe", 1, 1, 128), d("farseer_helm", 1, 1, 128),
                d("skeletal_top", 1, 1, 128), d("skeletal_bottoms", 1, 1, 128), d("seers_ring", 1, 1, 128),
                d("air_rune", 100, 200, 21), d("earth_rune", 50, 100, 26), d("blood_rune", 25, 75, 64),
                d("mud_rune", 25, 75, 64), d("earth_talisman", 25, 75, 13), d("air_talisman", 25, 75, 18),
                d("water_talisman", 1, 76, 18), d("shark", 5, 5, 13), d("oyster_pearls", 1, 1, 26),
                d("pure_essence", 150, 150, 26), d("grimy_ranarr_weed", 1, 1, 26), d("ensouled_dagannoth_head", 1, 1, 20),
                d("#rare", 1, 1, 16), d("#gems", 1, 1, 13), d("#rare_seeds", 1, 1, 18));
    }

    private static void rows2() {
        // dagannoth_supreme <- wiki:Dagannoth Supreme(303)
        p("dagannoth_supreme", rm("adamant_platebody", "raw_shark", "rune_axe", "steel_bar"),
                d("dagannoth_bones", 1, 1, 1), d("dagannoth_hide", 1, 1, 1), d("mithril_knife", 25, 50, 13),
                d("red_dhide_vambraces", 1, 1, 18), d("rune_thrownaxe", 5, 10, 26), d("adamant_dart", 10, 25, 26),
                d("iron_knife", 200, 500, 26), d("steel_knife", 50, 150, 26), d("fremennik_blade", 1, 1, 128),
                d("fremennik_shield", 1, 1, 128), d("fremennik_helm", 1, 1, 128), d("seercull", 1, 1, 128),
                d("dragon_axe", 1, 1, 128), d("archer_helm", 1, 1, 128), d("spined_body", 1, 1, 128),
                d("spined_chaps", 1, 1, 128), d("archers_ring", 1, 1, 128), d("rune_bolts", 2, 12, 26),
                d("iron_arrow", 200, 700, 32), d("oyster_pearls", 1, 1, 21), d("opal_bolt_tips", 10, 30, 26),
                d("shark", 5, 5, 26), d("yew_logs", 50, 150, 26), d("grimy_ranarr_weed", 1, 1, 26), d("maple_logs", 15, 65, 43),
                d("rune_limbs", 1, 1, 64), d("ensouled_dagannoth_head", 1, 1, 20), d("#rare", 1, 1, 16), d("#gems", 1, 1, 13),
                d("#rare_seeds", 1, 1, 18));
        // sarachnis <- wiki:Sarachnis(318)
        p("sarachnis", rm("adamantite_bar", "big_bones", "coal", "ranarr_seed", "red_spiders_eggs", "rune_full_helm"),
                d("dragon_med_helm", 1, 1, 192), d("sarachnis_cudgel", 1, 1, 384), d("battlestaff", 8, 10, 50),
                d("rune_platebody", 1, 1, 50), d("rune_med_helm", 1, 1, 50), d("rune_2h_sword", 1, 1, 50),
                d("blood_rune", 80, 100, 20), d("chaos_rune", 175, 200, 20), d("cosmic_rune", 125, 150, 20),
                d("death_rune", 80, 100, 20), d("soul_rune", 80, 100, 20), d("mithril_arrow", 450, 600, 50),
                d("mithril_bolts", 175, 225, 50), d("maple_seed", 2, 2, 100), d("papaya_seed", 2, 2, 100),
                d("yew_seed", 1, 1, 100), d("grimy_kwuarm", 10, 15, 53), d("grimy_dwarf_weed", 10, 15, 67),
                d("grimy_cadantine", 10, 15, 67), d("grimy_avantoe", 5, 10, 80), d("grimy_lantadyme", 10, 15, 89),
                d("grimy_snapdragon", 5, 10, 100), d("grimy_ranarr_weed", 5, 10, 100), d("grimy_torstol", 5, 10, 133),
                d("mithril_ore", 60, 90, 17), d("red_dragonhide", 15, 25, 20), d("adamantite_ore", 30, 40, 33),
                d("onyx_bolt_tips", 8, 10, 50), d("runite_ore", 4, 6, 100), d("coins", 17000, 25000, 17),
                d("dragon_bones", 10, 15, 20), d("weapon_poison_plus_plus", 4, 6, 50), d("crystal_key", 1, 1, 100),
                d("spider_carcass", 10, 10, 100), d("giant_egg_sac_full", 1, 1, 20), d("jar_of_eyes", 1, 1, 2000),
                d("#gems", 1, 1, 100), d("#rare_seeds", 1, 1, 17));
        // corporeal_beast <- wiki:Corporeal Beast(785)
        p("corporeal_beast", rm("runite_bar", "shark", "uncut_onyx"),
                d("mystic_robe_top", 1, 1, 28), d("mystic_robe_bottom", 1, 1, 28), d("mystic_air_staff", 1, 1, 43),
                d("mystic_water_staff", 1, 1, 43), d("mystic_earth_staff", 1, 1, 43), d("mystic_fire_staff", 1, 1, 43),
                d("spirit_shield", 1, 1, 64), d("soul_rune", 250, 250, 16), d("rune_bolts", 250, 250, 21),
                d("death_rune", 300, 300, 23), d("onyx_bolts_e", 175, 175, 26), d("cannonball", 2000, 2000, 30),
                d("adamant_arrow", 750, 750, 30), d("law_rune", 250, 250, 30), d("cosmic_rune", 500, 500, 30),
                d("raw_shark", 70, 70, 24), d("pure_essence", 2500, 2500, 24), d("adamantite_bar", 35, 35, 28),
                d("green_dragonhide", 100, 100, 28), d("adamantite_ore", 125, 125, 30), d("runite_ore", 20, 20, 43),
                d("teak_plank", 100, 100, 43), d("mahogany_logs", 150, 150, 43), d("magic_logs", 75, 75, 43),
                d("tuna_potato", 30, 30, 26), d("white_berries", 120, 120, 30), d("goat_horn", 120, 120, 30),
                d("watermelon_seed", 24, 24, 34), d("coins", 20000, 50000, 43), d("antidote_plus_plus", 40, 40, 51),
                d("ranarr_seed", 10, 10, 102), d("holy_elixir", 1, 1, 171), d("jar_of_spirits", 1, 1, 1000), d("#gems", 1, 1, 4));
        // skotizo <- wiki:Skotizo(321)
        p("skotizo", rm("coins"),
                d("infernal_ashes", 1, 1, 1), d("ancient_shard", 1, 1, 1), d("ancient_shard", 2, 2, 7),
                d("ancient_shard", 3, 3, 20), d("ancient_shard", 4, 4, 25), d("ancient_shard", 5, 5, 100),
                d("uncut_dragonstone", 10, 10, 111), d("uncut_onyx", 1, 1, 1000), d("rune_platebody", 3, 3, 14),
                d("rune_platelegs", 3, 3, 14), d("rune_plateskirt", 3, 3, 14), d("death_rune", 500, 500, 14),
                d("soul_rune", 450, 450, 14), d("blood_rune", 450, 450, 14), d("adamantite_ore", 75, 75, 14),
                d("grimy_snapdragon", 20, 20, 14), d("grimy_torstol", 20, 20, 14), d("raw_anglerfish", 60, 60, 14),
                d("runite_bar", 20, 20, 14), d("mahogany_plank", 150, 150, 14), d("battlestaff", 25, 25, 14),
                d("onyx_bolt_tips", 40, 40, 14), d("shield_left_half", 1, 1, 100), d("ensouled_demon_head", 1, 1, 10),
                d("dark_claw", 1, 1, 25), d("dark_totem_base", 1, 1, 43), d("dark_totem_middle", 1, 1, 43),
                d("dark_totem_top", 1, 1, 43), d("dark_totem", 1, 1, 128), d("jar_of_darkness", 1, 1, 200));
        // hespori <- wiki:Hespori(284)
        p("hespori", rm("#herbs", "#seeds"),
                d("attas_seed", 1, 2, 3), d("iasor_seed", 1, 2, 3), d("kronos_seed", 1, 2, 3),
                d("bottomless_compost_bucket", 1, 1, 35), d("watermelon_seed", 10, 20, 40), d("snape_grass_seed", 6, 16, 40),
                d("white_lily_seed", 8, 18, 16), d("limpwurt_seed", 6, 14, 40), d("wildblood_seed", 10, 20, 40),
                d("whiteberry_seed", 10, 16, 40), d("poison_ivy_seed", 8, 16, 40), d("irit_seed", 2, 8, 27),
                d("avantoe_seed", 2, 5, 27), d("kwuarm_seed", 2, 5, 27), d("toadflax_seed", 2, 5, 27),
                d("cadantine_seed", 2, 5, 27), d("lantadyme_seed", 2, 5, 27), d("dwarf_weed_seed", 2, 5, 27),
                d("ranarr_seed", 1, 2, 40), d("snapdragon_seed", 1, 1, 40), d("torstol_seed", 1, 1, 40),
                d("maple_seed", 2, 4, 20), d("willow_seed", 2, 5, 27), d("yew_seed", 1, 1, 40), d("magic_seed", 1, 1, 80),
                d("pineapple_seed", 3, 6, 27), d("papaya_seed", 1, 3, 27), d("palm_seed", 1, 3, 27),
                d("dragonfruit_seed", 1, 1, 40), d("teak_seed", 2, 5, 20), d("mahogany_seed", 1, 3, 27),
                d("cactus_seed", 4, 14, 40), d("potato_cactus_seed", 4, 14, 40), d("celastrus_seed", 1, 1, 40),
                d("spirit_seed", 1, 1, 80), d("redwood_tree_seed", 1, 1, 80));
        // scurrius <- wiki:Scurrius(250)
        p("scurrius", rm("iron_bar", "mithril_bar"),
                d("big_bones", 1, 1, 1), d("adamant_platebody", 1, 1, 17), d("rune_med_helm", 1, 1, 17),
                d("rune_full_helm", 1, 1, 17), d("rune_sq_shield", 1, 1, 17), d("rune_chainbody", 1, 1, 17),
                d("rune_battleaxe", 1, 1, 17), d("adamant_arrow", 20, 50, 17), d("rune_arrow", 20, 50, 17),
                d("chaos_rune", 70, 125, 17), d("death_rune", 40, 90, 33), d("law_rune", 10, 30, 33), d("trout", 1, 3, 17),
                d("tuna", 1, 3, 17), d("shark", 1, 1, 33), d("lobster", 1, 3, 17), d("ranging_potion(1)", 1, 1, 33),
                d("super_strength(1)", 1, 1, 33), d("cheese", 1, 1, 100), d("prayer_potion", 1, 1, 17),
                d("coins", 1000, 9000, 17), d("scurrius_spine", 1, 1, 33), d("long_bone", 1, 1, 400),
                d("curved_bone", 1, 1, 5012), d("adamant_arrow", 20, 35, 11), d("rune_arrow", 20, 35, 11),
                d("chaos_rune", 40, 70, 11), d("death_rune", 30, 50, 11), d("law_rune", 5, 15, 11), d("trout", 1, 3, 11),
                d("tuna", 1, 3, 11), d("lobster", 1, 3, 11), d("ranging_potion(1)", 1, 1, 11), d("super_strength(1)", 1, 1, 11),
                d("cheese", 1, 1, 33));
        // zulrah <- wiki:Zulrah(725)
        p("zulrah", rm("antifire_potion", "magic_logs"),
                d("zulrahs_scales", 100, 299, 1), d("tanzanite_fang", 1, 1, 1024), d("magic_fang", 1, 1, 1024),
                d("serpentine_visage", 1, 1, 1024), d("uncut_onyx", 1, 1, 1024), d("flax", 1000, 1000, 25),
                d("tanzanite_mutagen", 1, 1, 13107), d("magma_mutagen", 1, 1, 13107), d("battlestaff", 10, 10, 25),
                d("dragon_halberd", 1, 1, 124), d("chaos_rune", 400, 400, 21), d("snapdragon", 10, 10, 124),
                d("dwarf_weed", 30, 30, 124), d("toadflax", 25, 25, 124), d("torstol", 10, 10, 124), d("palm_seed", 1, 1, 42),
                d("papaya_seed", 3, 3, 42), d("calquat_tree_seed", 2, 2, 42), d("magic_seed", 1, 1, 62),
                d("toadflax_seed", 2, 2, 124), d("snapdragon_seed", 1, 1, 124), d("dwarf_weed_seed", 2, 2, 124),
                d("torstol_seed", 1, 1, 124), d("spirit_seed", 1, 1, 249), d("snakeskin", 35, 35, 23), d("runite_ore", 2, 2, 23),
                d("pure_essence", 1500, 1500, 25), d("yew_logs", 35, 35, 25), d("adamantite_bar", 20, 20, 31),
                d("coal", 200, 200, 31), d("dragon_bones", 12, 12, 31), d("mahogany_logs", 50, 50, 31),
                d("raw_shark", 35, 35, 55), d("manta_ray", 35, 35, 83), d("zul_andra_teleport", 4, 4, 17),
                d("antidote_plus_plus", 10, 10, 28), d("dragonstone_bolt_tips", 12, 12, 31), d("grapes", 250, 250, 42),
                d("coconut", 20, 20, 42), d("swamp_tar", 1000, 1000, 50), d("zulrahs_scales", 500, 500, 50),
                d("jar_of_swamp", 1, 1, 3000), d("#rare", 1, 1, 12));
        // vorkath <- wiki:Vorkath(732)
        p("vorkath", rm("runite_ore"),
                d("superior_dragon_bones", 2, 2, 1), d("blue_dragonhide", 2, 2, 1), d("rune_longsword", 2, 3, 30),
                d("battlestaff", 5, 15, 38), d("dragon_battleaxe", 1, 1, 75), d("dragon_longsword", 1, 1, 75),
                d("dragon_platelegs", 1, 1, 75), d("dragon_plateskirt", 1, 1, 75), d("chaos_rune", 250, 350, 25),
                d("wrath_rune", 30, 60, 50), d("blue_dragonhide", 25, 30, 19), d("green_dragonhide", 25, 30, 21),
                d("red_dragonhide", 20, 25, 21), d("black_dragonhide", 15, 25, 21), d("dragon_bolts_unf", 50, 100, 19),
                d("dragonstone_bolt_tips", 11, 25, 30), d("onyx_bolt_tips", 5, 10, 38), d("rune_dart_tip", 75, 100, 50),
                d("dragon_dart_tip", 10, 50, 25), d("dragon_arrowtips", 25, 50, 50), d("diamond_bolt_tips", 25, 30, 109),
                d("emerald_bolt_tips", 25, 30, 136), d("ruby_bolt_tips", 25, 30, 136), d("dragonstone_bolt_tips", 25, 30, 195),
                d("onyx_bolt_tips", 25, 30, 390), d("sapphire_bolt_tips", 25, 30, 546), d("raw_shark", 35, 55, 100),
                d("manta_ray", 35, 55, 150), d("adamantite_ore", 10, 30, 21), d("grapes", 250, 300, 30),
                d("magic_logs", 50, 50, 30), d("dragon_bones", 15, 20, 38), d("diamond", 10, 20, 38),
                d("wrath_talisman", 1, 1, 50), d("vorkaths_head", 1, 1, 50), d("dragonbone_necklace", 1, 1, 1000),
                d("jar_of_decay", 1, 1, 3000), d("draconic_visage", 1, 1, 5000), d("skeletal_visage", 1, 1, 5000),
                d("#rare", 1, 1, 15));
        // kraken <- wiki:Kraken(291)
        p("kraken", rm(),
                d("mystic_water_staff", 1, 1, 43), d("rune_warhammer", 1, 1, 64), d("rune_longsword", 1, 1, 64),
                d("mystic_robe_top", 1, 1, 128), d("mystic_robe_bottom", 1, 1, 128), d("trident_of_the_seas", 1, 1, 512),
                d("water_rune", 400, 400, 13), d("mist_rune", 100, 100, 32), d("chaos_rune", 200, 200, 13),
                d("death_rune", 150, 150, 13), d("blood_rune", 60, 60, 13), d("soul_rune", 50, 50, 18),
                d("watermelon_seed", 24, 24, 43), d("torstol_seed", 2, 2, 128), d("magic_seed", 1, 1, 128),
                d("seaweed", 125, 125, 43), d("battlestaff", 10, 10, 32), d("unpowered_orb", 50, 50, 64),
                d("diamond", 8, 8, 128), d("oak_plank", 60, 60, 43), d("runite_bar", 2, 2, 128), d("raw_shark", 50, 50, 64),
                d("raw_monkfish", 100, 100, 64), d("grimy_snapdragon", 6, 6, 64), d("coins", 10000, 19999, 9),
                d("shark", 5, 5, 18), d("sanfew_serum", 2, 2, 32), d("edible_seaweed", 5, 5, 43), d("harpoon", 1, 1, 128),
                d("crystal_key", 1, 1, 128), d("rusty_sword", 1, 1, 73), d("antidote_plus_plus", 2, 2, 64),
                d("dragonstone_ring", 1, 1, 128), d("kraken_tentacle", 1, 1, 400), d("jar_of_dirt", 1, 1, 1000),
                d("#gems", 1, 1, 64));
        // thermonuclear_smoke_devil <- wiki:Thermonuclear smoke devil(301)
        p("thermonuclear_smoke_devil", rm("rune_platelegs", "runite_bar"),
                d("ashes", 1, 1, 1), d("occult_necklace", 1, 1, 350), d("smoke_battlestaff", 1, 1, 512),
                d("dragon_chainbody", 1, 1, 2000), d("rune_dagger", 1, 1, 21), d("rune_chainbody", 1, 1, 27),
                d("red_dhide_body", 1, 1, 36), d("rune_battleaxe", 1, 1, 36), d("mystic_air_staff", 1, 1, 36),
                d("mystic_fire_staff", 1, 1, 36), d("rune_scimitar", 1, 1, 54), d("dragon_scimitar", 1, 1, 107),
                d("ancient_staff", 1, 1, 107), d("smoke_rune", 100, 100, 11), d("air_rune", 300, 300, 13),
                d("soul_rune", 100, 100, 13), d("rune_arrow", 100, 100, 54), d("ugthanki_kebab", 3, 3, 107),
                d("tuna_potato", 3, 3, 36), d("sanfew_serum", 2, 2, 36), d("prayer_potion", 2, 2, 107),
                d("pure_essence", 300, 300, 54), d("molten_glass", 100, 100, 54), d("mithril_bar", 20, 20, 54),
                d("magic_logs", 20, 20, 54), d("gold_ore", 200, 200, 54), d("diamond", 10, 10, 107), d("raw_shark", 30, 30, 171),
                d("coins", 10000, 19999, 7), d("tinderbox", 1, 1, 107), d("fire_talisman", 1, 1, 107),
                d("goat_horn", 50, 50, 54), d("grimy_toadflax", 15, 15, 54), d("onyx_bolt_tips", 12, 12, 54),
                d("snapdragon_seed", 2, 2, 54), d("ranarr_seed", 2, 2, 54), d("grapes", 100, 100, 107),
                d("magic_seed", 1, 1, 107), d("dragonstone_ring", 1, 1, 107), d("crystal_key", 1, 1, 107),
                d("jar_of_smoke", 1, 1, 2000), d("#gems", 1, 1, 54));
        // cerberus <- wiki:Cerberus(318)
        p("cerberus", rm(),
                d("infernal_ashes", 1, 1, 1), d("primordial_crystal", 1, 1, 520), d("pegasian_crystal", 1, 1, 520),
                d("eternal_crystal", 1, 1, 520), d("smouldering_stone", 1, 1, 520), d("rune_platebody", 1, 1, 26),
                d("rune_chainbody", 1, 1, 32), d("black_dhide_body", 1, 1, 43), d("rune_axe", 1, 1, 43),
                d("rune_pickaxe", 1, 1, 43), d("battlestaff", 6, 6, 43), d("rune_full_helm", 1, 1, 43),
                d("lava_battlestaff", 1, 1, 65), d("rune_halberd", 1, 1, 65), d("fire_rune", 300, 300, 22),
                d("soul_rune", 100, 100, 22), d("pure_essence", 300, 300, 26), d("blood_rune", 60, 60, 32),
                d("cannonball", 50, 50, 32), d("rune_bolts_unf", 40, 40, 32), d("coal", 120, 120, 22),
                d("super_restore", 2, 2, 22), d("summer_pie", 3, 3, 22), d("dragon_bones", 20, 20, 26),
                d("unholy_symbol", 1, 1, 26), d("wine_of_zamorak", 15, 15, 26), d("ashes", 50, 50, 32),
                d("fire_orb", 20, 20, 32), d("grimy_torstol", 6, 6, 32), d("runite_ore", 5, 5, 43), d("torstol_seed", 3, 3, 65),
                d("ranarr_seed", 2, 2, 65), d("key_master_teleport", 7, 7, 65), d("ensouled_hellhound_head", 1, 1, 15),
                d("jar_of_souls", 1, 1, 2000), d("#rare", 1, 1, 43));
        // abyssal_sire <- wiki:Abyssal Sire(350)
        p("abyssal_sire", rm(),
                d("unsired", 1, 1, 100), d("abyssal_ashes", 1, 1, 1), d("battlestaff", 10, 10, 23),
                d("rune_full_helm", 3, 3, 35), d("rune_sword", 3, 3, 35), d("rune_platebody", 2, 2, 35),
                d("mystic_air_staff", 2, 2, 70), d("air_battlestaff", 6, 6, 70), d("blood_rune", 190, 210, 28),
                d("soul_rune", 225, 275, 28), d("cosmic_rune", 350, 350, 35), d("cannonball", 300, 300, 35),
                d("grimy_kwuarm", 25, 25, 89), d("grimy_dwarf_weed", 25, 25, 111), d("grimy_cadantine", 25, 25, 111),
                d("grimy_lantadyme", 25, 25, 148), d("earth_orb", 47, 53, 28), d("pure_essence", 600, 600, 28),
                d("magic_logs", 50, 70, 28), d("coal", 380, 420, 35), d("runite_ore", 6, 6, 35), d("onyx_bolt_tips", 10, 10, 46),
                d("super_restore", 4, 4, 28), d("binding_necklace", 25, 25, 35), d("saradomin_brew(3)", 6, 6, 70),
                d("jug_of_water", 250, 350, 70), d("#rare", 1, 1, 46));
    }

    private static void rows3() {
        // alchemical_hydra <- wiki:Alchemical Hydra(426)
        p("alchemical_hydra", rm("magic_logs"),
                d("hydra_bones", 2, 2, 1), d("dragon_thrownaxe", 500, 1000, 2000), d("mystic_fire_staff", 1, 1, 13),
                d("mystic_water_staff", 1, 1, 13), d("battlestaff", 8, 12, 20), d("black_dhide_body", 1, 1, 20),
                d("dragon_longsword", 1, 1, 34), d("rune_platebody", 1, 1, 34), d("dragon_battleaxe", 1, 1, 50),
                d("rune_platelegs", 1, 1, 67), d("rune_plateskirt", 1, 1, 67), d("mystic_robe_top_light", 1, 1, 101),
                d("mystic_robe_bottom_light", 1, 1, 101), d("chaos_rune", 150, 300, 17), d("blood_rune", 150, 300, 17),
                d("astral_rune", 150, 300, 17), d("dragonstone_bolts_e", 100, 120, 50), d("onyx_bolts_e", 35, 50, 101),
                d("shark", 2, 4, 14), d("ranging_potion(3)", 1, 1, 14), d("super_restore(3)", 2, 2, 14),
                d("dragon_bones", 30, 30, 17), d("crystal_key", 1, 1, 101), d("alchemical_hydra_heads", 1, 1, 256),
                d("jar_of_chemicals", 1, 1, 2000), d("#rare", 1, 1, 50));
        // the_nightmare <- wiki:The Nightmare(814)
        p("the_nightmare", rm("rune_platebody"),
                d("big_bones", 1, 1, 1), d("nightmare_staff", 1, 1, 300), d("inquisitors_great_helm", 1, 1, 420),
                d("inquisitors_hauberk", 1, 1, 420), d("inquisitors_plateskirt", 1, 1, 420), d("inquisitors_mace", 1, 1, 750),
                d("eldritch_orb", 1, 1, 960), d("harmonised_orb", 1, 1, 960), d("volatile_orb", 1, 1, 960),
                d("cosmic_rune", 11, 242, 25), d("nature_rune", 8, 176, 25), d("death_rune", 7, 176, 25),
                d("blood_rune", 16, 440, 25), d("soul_rune", 8, 132, 25), d("cannonball", 6, 220, 25),
                d("rune_arrow", 31, 547, 33), d("mithril_ore", 11, 176, 25), d("coal", 15, 264, 25), d("gold_ore", 9, 176, 25),
                d("adamantite_ore", 40, 95, 25), d("magic_logs", 1, 55, 25), d("grimy_cadantine", 1, 16, 25),
                d("grimy_torstol", 1, 16, 25), d("snapdragon_seed", 1, 7, 33), d("uncut_emerald", 2, 44, 33),
                d("uncut_ruby", 27, 60, 33), d("runite_ore", 1, 16, 50), d("bass", 1, 18, 17), d("shark", 1, 16, 17),
                d("prayer_potion(3)", 1, 10, 20), d("sanfew_serum(3)", 1, 8, 20), d("saradomin_brew(3)", 1, 10, 20),
                d("zamorak_brew(3)", 1, 10, 20), d("coins", 2291, 43958, 50), d("jar_of_dreams", 1, 1, 1900));
        // phosanis_nightmare <- wiki:Phosani's Nightmare(1024)
        p("phosanis_nightmare", rm(),
                d("big_bones", 1, 1, 1), d("nightmare_staff", 1, 1, 507), d("inquisitors_great_helm", 1, 1, 700),
                d("inquisitors_hauberk", 1, 1, 700), d("inquisitors_plateskirt", 1, 1, 700), d("inquisitors_mace", 1, 1, 1129),
                d("eldritch_orb", 1, 1, 1600), d("harmonised_orb", 1, 1, 1600), d("volatile_orb", 1, 1, 1600),
                d("cosmic_rune", 247, 420, 25), d("nature_rune", 165, 305, 25), d("death_rune", 165, 305, 25),
                d("blood_rune", 343, 765, 25), d("soul_rune", 110, 228, 25), d("cannonball", 137, 382, 25),
                d("rune_arrow", 412, 957, 33), d("mithril_ore", 165, 305, 20), d("coal", 220, 458, 25),
                d("gold_ore", 165, 305, 25), d("adamantite_ore", 40, 95, 25), d("magic_logs", 40, 95, 25),
                d("grimy_cadantine", 13, 26, 25), d("grimy_torstol", 13, 26, 25), d("snapdragon_seed", 5, 10, 33),
                d("uncut_emerald", 33, 75, 33), d("uncut_ruby", 27, 60, 33), d("runite_ore", 11, 26, 50), d("bass", 16, 29, 17),
                d("shark", 13, 26, 17), d("prayer_potion(3)", 8, 15, 20), d("sanfew_serum(3)", 6, 12, 20),
                d("saradomin_brew(3)", 8, 15, 20), d("zamorak_brew(3)", 8, 15, 20), d("coins", 41417, 74500, 50),
                d("parasitic_egg", 1, 1, 200), d("jar_of_dreams", 1, 1, 4000));
        // phantom_muspah <- wiki:Phantom Muspah(741)
        p("phantom_muspah", rm("blood_rune"),
                d("ancient_essence", 540, 599, 2), d("ancient_essence", 885, 995, 4), d("ancient_essence", 1970, 2060, 10),
                d("ancient_icon", 1, 1, 50), d("venator_shard", 1, 1, 100), d("shark", 4, 6, 9), d("summer_pie", 4, 6, 9),
                d("ancient_brew(3)", 1, 2, 9), d("ranging_potion(3)", 1, 3, 9), d("super_restore(3)", 2, 3, 9),
                d("prayer_potion(3)", 2, 3, 9), d("dragon_plateskirt", 1, 1, 47), d("rune_platelegs", 3, 3, 47),
                d("black_dhide_body", 1, 1, 47), d("dragon_platelegs", 2, 2, 59), d("rune_sword", 1, 1, 235),
                d("soul_rune", 380, 380, 24), d("smoke_rune", 314, 314, 16), d("chaos_rune", 480, 480, 47),
                d("fire_rune", 1964, 1964, 47), d("cannonball", 670, 670, 47), d("grimy_toadflax", 40, 40, 78),
                d("grimy_kwuarm", 6, 6, 150), d("grimy_dwarf_weed", 6, 6, 188), d("grimy_cadantine", 6, 6, 188),
                d("grimy_lantadyme", 6, 6, 251), d("yew_seed", 2, 2, 47), d("torstol_seed", 2, 2, 47), d("palm_seed", 2, 2, 47),
                d("ranarr_seed", 3, 3, 47), d("snapdragon_seed", 5, 5, 59), d("ranarr_seed", 5, 5, 78),
                d("spirit_seed", 1, 1, 118), d("adamantite_ore", 22, 22, 24), d("gold_ore", 180, 180, 24),
                d("teak_plank", 22, 22, 24), d("molten_glass", 89, 89, 16), d("pure_essence", 2314, 2314, 47),
                d("coal", 163, 163, 47), d("runite_ore", 18, 18, 78), d("water_orb", 21, 21, 16),
                d("dragon_bolts_unf", 89, 89, 24), d("limpwurt_root", 21, 21, 78), d("raw_shark", 28, 28, 63),
                d("manta_ray", 28, 28, 94), d("#rare", 1, 1, 24));
        // duke_sucellus <- wiki:Duke Sucellus(758)
        p("duke_sucellus", rm("coins", "death_rune", "runite_bar"),
                d("magus_vestige", 1, 1, 720), d("chromium_ingot", 1, 1, 240), d("eye_of_the_duke", 1, 1, 720),
                d("virtus_mask", 1, 1, 2160), d("virtus_robe_top", 1, 1, 2160), d("virtus_robe_bottom", 1, 1, 2160),
                d("bronze_chainbody", 11, 11, 100), d("mithril_chainbody", 5, 5, 100),
                d("adamant_chainbody", 6, 6, 100), d("dragon_platelegs", 5, 5, 100),
                d("pure_essence", 200, 200, 100), d("iron_ore", 63, 63, 100), d("coal", 216, 216, 12),
                d("mithril_ore", 33, 33, 100), d("adamantite_ore", 75, 75, 12), d("runite_ore", 30, 30, 50),
                d("sapphire", 28, 28, 100), d("emerald", 28, 28, 100), d("ruby", 28, 28, 100),
                d("uncut_ruby", 41, 41, 20), d("uncut_diamond", 41, 41, 20), d("bronze_bar", 63, 63, 100),
                d("dragon_arrowtips", 166, 166, 50), d("rune_javelin_heads", 60, 60, 12),
                d("dragon_javelin_heads", 60, 60, 12), d("raw_sea_turtle", 200, 200, 100),
                d("air_rune", 200, 200, 100), d("mist_rune", 100, 100, 12), d("chaos_rune", 100, 100, 11),
                d("soul_rune", 666, 666, 50));
        // the_leviathan <- wiki:The Leviathan(798)
        p("the_leviathan", rm("coins", "death_rune", "dragon_platelegs", "runite_bar"),
                d("venator_vestige", 1, 1, 768), d("chromium_ingot", 1, 1, 256), d("leviathans_lure", 1, 1, 768),
                d("virtus_mask", 1, 1, 2304), d("virtus_robe_top", 1, 1, 2304), d("virtus_robe_bottom", 1, 1, 2304),
                d("iron_ore", 57, 57, 100), d("silver_ore", 57, 57, 100), d("coal", 195, 195, 12),
                d("gold_ore", 67, 67, 12), d("adamantite_ore", 57, 57, 100), d("runite_ore", 27, 27, 50),
                d("sapphire", 25, 25, 100), d("emerald", 25, 25, 100), d("ruby", 25, 25, 100),
                d("uncut_ruby", 37, 37, 20), d("uncut_diamond", 37, 37, 20), d("dragon_javelin_heads", 54, 54, 12),
                d("dragon_bolts_unf", 150, 150, 50), d("onyx_bolt_tips", 90, 90, 100),
                d("raw_manta_ray", 180, 180, 100), d("anglerfish", 4, 4, 12), d("bronze_arrow", 63, 63, 100),
                d("mithril_arrow", 63, 63, 100), d("adamant_arrow", 63, 63, 100), d("rune_arrow", 54, 54, 12),
                d("body_rune", 180, 180, 100), d("earth_rune", 180, 180, 100), d("smoke_rune", 300, 300, 12),
                d("soul_rune", 600, 600, 50));
        // the_whisperer <- wiki:The Whisperer(791)
        p("the_whisperer", rm("coins", "dragon_platelegs", "runite_bar"),
                d("bellator_vestige", 1, 1, 512), d("chromium_ingot", 1, 1, 171), d("sirens_staff", 1, 1, 512),
                d("virtus_mask", 1, 1, 1536), d("virtus_robe_top", 1, 1, 1536), d("virtus_robe_bottom", 1, 1, 1536),
                d("bronze_longsword", 16, 16, 100), d("mithril_longsword", 7, 7, 100),
                d("adamant_longsword", 9, 9, 100), d("battlestaff", 70, 70, 50), d("dragon_plateskirt", 7, 7, 100),
                d("pure_essence", 280, 420, 100), d("iron_ore", 88, 88, 100), d("coal", 303, 303, 12),
                d("gold_ore", 88, 88, 100), d("mithril_ore", 88, 88, 100), d("adamantite_ore", 105, 105, 12),
                d("runite_ore", 42, 42, 50), d("sapphire", 39, 39, 100), d("emerald", 39, 39, 100),
                d("ruby", 39, 39, 100), d("uncut_ruby", 58, 58, 20), d("uncut_diamond", 58, 58, 20),
                d("dragon_javelin_heads", 84, 84, 12), d("rune_bolts_unf", 84, 84, 12),
                d("raw_monkfish", 700, 700, 100), d("water_rune", 280, 280, 100),
                d("steam_rune", 466, 466, 12), d("chaos_rune", 140, 140, 100), d("death_rune", 466, 466, 12),
                d("soul_rune", 933, 933, 50));
        // vardorvis <- wiki:Vardorvis(784)
        p("vardorvis", rm("coins", "death_rune", "dragon_platelegs", "runite_bar"),
                d("ultor_vestige", 1, 1, 1088), d("chromium_ingot", 1, 1, 363), d("executioners_axe_head", 1, 1, 1088),
                d("virtus_mask", 1, 1, 3264), d("virtus_robe_top", 1, 1, 3264), d("virtus_robe_bottom", 1, 1, 3264),
                d("pure_essence", 120, 120, 100), d("iron_ore", 38, 38, 100), d("silver_ore", 38, 38, 100),
                d("coal", 130, 130, 12), d("mithril_ore", 38, 38, 100), d("adamantite_ore", 45, 45, 12),
                d("runite_ore", 18, 18, 50), d("sapphire", 17, 17, 100), d("emerald", 17, 17, 100),
                d("ruby", 17, 17, 100), d("uncut_ruby", 25, 25, 20), d("uncut_diamond", 25, 25, 20),
                d("rune_javelin_heads", 36, 36, 12), d("dragon_javelin_heads", 36, 36, 12),
                d("dragon_dart_tip", 100, 100, 50), d("raw_shark", 200, 200, 100),
                d("bronze_javelin", 42, 42, 100), d("mithril_javelin", 42, 42, 100),
                d("adamant_javelin", 42, 42, 100), d("onyx_bolts_e", 60, 60, 100), d("mind_rune", 120, 120, 100),
                d("fire_rune", 120, 120, 100), d("lava_rune", 200, 200, 12), d("blood_rune", 200, 200, 12),
                d("soul_rune", 400, 400, 50));
        // the_hueycoatl <- wiki:The Hueycoatl(642)
        p("the_hueycoatl", rm("coins", "runite_ore"),
                d("big_bones", 1, 1, 1), d("dragon_hunter_wand", 1, 1, 105), d("hueycoatl_hide", 3, 3, 29),
                d("tome_of_earth", 1, 1, 90), d("rune_mace", 1, 26, 23), d("rune_scimitar", 1, 20, 23),
                d("rune_plateskirt", 1, 20, 34), d("adamant_platebody", 1, 30, 23), d("death_rune", 22, 450, 23),
                d("earth_rune", 150, 3000, 34), d("cosmic_rune", 20, 400, 34), d("nature_rune", 20, 400, 34),
                d("lantadyme_seed", 3, 60, 34), d("huasca_seed", 1, 20, 34), d("toadflax_seed", 2, 40, 34),
                d("torstol_seed", 1, 20, 34), d("ranarr_seed", 1, 20, 34), d("avantoe_seed", 1, 20, 23),
                d("kwuarm_seed", 1, 20, 23), d("adamant_bolts_unf", 15, 300, 23), d("air_orb", 4, 80, 23),
                d("raw_shark", 9, 180, 23), d("sunfire_splinters", 17, 350, 23), d("dragon_bones", 2, 40, 23),
                d("adamantite_ore", 6, 120, 34), d("cannonball", 60, 1200, 23), d("rune_dart_tip", 6, 120, 34),
                d("limpwurt_root", 10, 200, 34));
        // amoxliatl <- wiki:Amoxliatl(263)
        p("amoxliatl", rm("adamantite_bar", "coins"),
                d("frozen_tear", 2, 4, 2), d("frozen_tear", 5, 10, 3), d("frozen_tear", 10, 20, 10), d("rune_mace", 1, 1, 29),
                d("rune_pickaxe", 1, 1, 29), d("rune_platebody", 1, 1, 58), d("rune_platelegs", 1, 1, 58),
                d("water_rune", 200, 400, 12), d("chaos_rune", 30, 60, 14), d("death_rune", 20, 40, 14),
                d("blood_rune", 15, 30, 14), d("soul_rune", 30, 45, 14), d("nature_rune", 30, 60, 14), d("coal", 20, 30, 12),
                d("gold_ore", 20, 30, 12), d("adamantite_ore", 5, 10, 14), d("prayer_potion(3)", 1, 1, 29),
                d("runite_ore", 1, 1, 29), d("water_orb", 10, 20, 29), d("water_talisman", 1, 1, 29), d("huasca_seed", 1, 1, 58),
                d("pendant_of_ates", 1, 1, 25), d("glacial_temotli", 1, 1, 100), d("#gems", 1, 1, 14));
        // branda_the_fire_queen <- wiki:Branda the Fire Queen(350)
        p("branda_the_fire_queen", rm("big_bones", "runite_bar"),
                d("fire_element_staff_crown", 1, 1, 75), d("mystic_vigour_prayer_scroll", 1, 1, 75),
                d("giantsoul_amulet", 1, 1, 16), d("mystic_fire_staff", 4, 4, 28), d("fire_battlestaff", 6, 6, 28),
                d("rune_plateskirt", 2, 2, 28), d("rune_scimitar", 2, 2, 28), d("rune_pickaxe", 2, 2, 28),
                d("fire_rune", 1500, 3000, 28), d("chaos_rune", 200, 400, 28), d("death_rune", 150, 300, 28),
                d("nature_rune", 100, 150, 28), d("law_rune", 100, 150, 28), d("soul_rune", 100, 150, 28),
                d("blood_rune", 100, 150, 28), d("rune_arrow", 100, 300, 28), d("coal", 280, 320, 28),
                d("gold_ore", 60, 100, 28), d("fire_orb", 20, 40, 28), d("grimy_avantoe", 6, 8, 28),
                d("grimy_cadantine", 6, 8, 28), d("grimy_dwarf_weed", 6, 8, 28), d("grimy_irit_leaf", 6, 8, 28),
                d("grimy_kwuarm", 6, 8, 28), d("grimy_lantadyme", 6, 8, 28), d("grimy_ranarr_weed", 6, 8, 28),
                d("maple_seed", 4, 6, 55), d("palm_seed", 2, 2, 55), d("yew_seed", 2, 2, 55), d("coins", 10000, 30000, 28),
                d("prayer_potion", 2, 4, 28));
        // gemstone_crab <- wiki:Gemstone Crab(160)
        p("gemstone_crab", rm(),
                d("uncut_opal", 1, 1, 4), d("uncut_jade", 1, 1, 4), d("uncut_red_topaz", 1, 1, 5), d("uncut_sapphire", 1, 1, 11),
                d("uncut_emerald", 1, 1, 16), d("uncut_ruby", 1, 1, 16), d("uncut_diamond", 1, 1, 32),
                d("uncut_dragonstone", 1, 1, 500));
    }

    private static void rows4() {
        // shellbane_gryphon <- wiki:Shellbane gryphon(235)
        p("shellbane_gryphon", rm("#herbs", "#seeds", "adamantite_bar", "coins", "rune_arrow", "rune_full_helm"),
                d("belles_folly_tarnished", 1, 1, 75), d("big_bones", 1, 1, 1), d("rune_claws", 1, 1, 13),
                d("rune_scimitar", 1, 1, 16), d("raw_swordfish", 1, 1, 9), d("sweetcorn", 1, 1, 13), d("watermelon", 1, 1, 13),
                d("raw_sea_turtle", 1, 1, 32), d("basket", 1, 1, 43), d("long_bone", 1, 1, 400),
                d("jar_of_feathers", 1, 1, 2000), d("curved_bone", 1, 1, 5012), d("#gems", 1, 1, 64));
        // brutus <- wiki:Brutus(30)
        p("brutus", rm("big_bones"),
                d("mooleta", 1, 1, 30), d("bottomless_milk_bucket", 1, 1, 38), d("cow_slippers", 1, 1, 150),
                d("iron_full_helm", 1, 1, 40), d("iron_platebody", 1, 1, 40), d("iron_platelegs", 1, 1, 81),
                d("iron_plateskirt", 1, 1, 81), d("iron_arrow", 14, 14, 8), d("air_rune", 29, 29, 8), d("mind_rune", 18, 18, 10),
                d("chaos_rune", 12, 12, 40), d("potato_seed", 3, 3, 8), d("acorn", 2, 2, 16), d("cowhide", 1, 1, 8),
                d("oak_logs", 2, 2, 16), d("logs", 2, 2, 16), d("coins", 60, 80, 5), d("coins", 80, 100, 16),
                d("coins", 100, 120, 16));
        // deranged_archaeologist <- wiki:Deranged archaeologist(276)
        p("deranged_archaeologist", rm(),
                d("bones", 1, 1, 1), d("steel_ring", 1, 1, 44), d("black_dhide_body", 1, 1, 22), d("rune_sword", 1, 1, 33),
                d("water_rune", 100, 100, 22), d("mud_rune", 40, 40, 22), d("rune_knife", 25, 25, 22),
                d("dragon_arrow", 60, 60, 131), d("cannonball", 80, 80, 33), d("grimy_dwarf_weed", 4, 4, 16),
                d("white_berries", 10, 10, 19), d("rune_limbs", 1, 1, 26), d("black_dragonhide", 8, 8, 26),
                d("gold_ore", 10, 10, 22), d("onyx_bolt_tips", 6, 6, 22), d("anchovy_pizza", 2, 2, 33),
                d("prayer_potion(3)", 1, 1, 16), d("potato_with_cheese", 3, 3, 16), d("shark", 2, 2, 16),
                d("crystal_key", 1, 1, 19), d("long_bone", 1, 1, 66), d("#rare", 1, 1, 22), d("#gems", 1, 1, 22));
    }

    private static void rows5() {
        // Round 27: the Grotesque Guardians and Araxxor from their wiki DropsLine tables (uniques the modules already
        // roll, pets, keys and conditional rows left alone; shared tables by marker). Obor and Bryophyta stay
        // unpatched: their dump rows are the hill giant and moss giant tables.
        // dusk <- wiki:Grotesque Guardians(328) DropsLine table
        p("dusk", rm("rune_platebody"),
                d("granite_dust", 50, 100, 1), d("rune_pickaxe", 1, 1, 24), d("rune_full_helm", 1, 1, 28),
                d("rune_platelegs", 1, 1, 28), d("rune_2h_sword", 1, 1, 36), d("rune_battleaxe", 1, 1, 47),
                d("dragon_longsword", 1, 1, 142), d("adamant_boots", 1, 1, 142), d("dragon_med_helm", 1, 1, 142),
                d("saradomin_brew", 2, 2, 18), d("magic_potion(2)", 1, 1, 24), d("ranging_potion(2)", 1, 1, 24),
                d("super_combat_potion(2)", 1, 1, 24), d("prayer_potion", 1, 2, 36), d("gold_ore", 40, 50, 20),
                d("adamantite_bar", 25, 40, 24), d("gold_bar", 35, 50, 24), d("mithril_bar", 35, 45, 24),
                d("coal", 180, 250, 28), d("runite_ore", 3, 6, 36), d("runite_bar", 3, 5, 47),
                d("raw_shark", 20, 20, 95), d("raw_sea_turtle", 20, 20, 142), d("coins", 10000, 20000, 14),
                d("chaos_rune", 100, 150, 18), d("coins", 25000, 25000, 28), d("crystal_key", 1, 1, 28),
                d("death_rune", 90, 130, 28), d("dragon_dart_tip", 15, 25, 28), d("diamond_bolt_tips", 100, 150, 47),
                d("dragonstone_bolt_tips", 20, 40, 71), d("onyx_bolt_tips", 10, 15, 71), d("dragon_arrowtips", 50, 150, 142),
                d("soul_rune", 20, 40, 142));
        // araxxor <- wiki:Araxxor(890) DropsLine table
        p("araxxor", rm("rune_platebody", "runite_bar", "super_restore", "coins"),
                d("araxyte_venom_sac", 1, 1, 16), d("super_combat_potion(1)", 1, 1, 16), d("prayer_potion", 1, 1, 8),
                d("shark", 2, 3, 16), d("wild_pie", 2, 3, 16), d("rune_kiteshield", 2, 2, 14),
                d("rune_platelegs", 2, 2, 14), d("dragon_mace", 2, 2, 19), d("rune_2h_sword", 5, 5, 115),
                d("dragon_platelegs", 2, 2, 115), d("death_rune", 250, 250, 23), d("nature_rune", 80, 80, 58),
                d("mud_rune", 100, 100, 115), d("blood_rune", 180, 180, 115), d("yew_seed", 1, 1, 29),
                d("toadflax_seed", 4, 4, 38), d("ranarr_seed", 3, 3, 115), d("snapdragon_seed", 3, 3, 115),
                d("magic_seed", 2, 2, 115), d("coal", 120, 120, 29), d("adamantite_ore", 85, 85, 29),
                d("raw_shark", 21, 21, 29), d("yew_logs", 70, 70, 38), d("runite_ore", 12, 12, 58),
                d("raw_shark", 100, 100, 115), d("raw_monkfish", 120, 120, 115), d("pure_essence", 1200, 1200, 115),
                d("araxyte_venom_sac", 12, 12, 58), d("earth_orb", 45, 45, 19), d("araxyte_venom_sac", 6, 6, 23), d("mort_myre_fungus", 24, 24, 29),
                d("antidote_plus_plus(3)", 6, 6, 29), d("wine_of_zamorak", 8, 8, 38), d("red_spiders_eggs", 40, 40, 58),
                d("bark", 15, 15, 115), d("#tree_herb_seeds", 1, 1, 115),
                d("#rare", 1, 1, 115));
        // Rows of the snapshot that were "not in game" until round 27 added the item. The Wilderness bosses' looting
        // bag row is the generic Wilderness drop (JewelleryService.onKill), so it is not on their tables.
        // Chaos Elemental: Blighted ancient ice sack (was "not in game")
        add("chaos_elemental", d("blighted_ancient_ice_sack", 20, 40, 26));
        // Scurrius: Raw rat meat (was "not in game")
        add("scurrius", d("raw_rat_meat", 1, 1, 1));
        // Vorkath: Scaly blue dragonhide (was "not in game")
        add("vorkath", d("scaly_blue_dragonhide", 1, 1, 10));
        // Abyssal Sire: Mystic lava staff (was "not in game")
        add("abyssal_sire", d("mystic_lava_staff", 2, 2, 35));
        // Amoxliatl: Blessed bone shards (was "not in game")
        add("amoxliatl", d("blessed_bone_shards", 60, 100, 29));
        // Shellbane gryphon: Adamant cannonball (was "not in game")
        add("shellbane_gryphon", d("adamant_cannonball", 35, 50, 16));
        // Shellbane gryphon: Rune cannonball (was "not in game")
        add("shellbane_gryphon", d("rune_cannonball", 20, 30, 21));
        // Shellbane gryphon: Elkhorn frag (was "not in game")
        add("shellbane_gryphon", d("elkhorn_frag", 1, 1, 128));
        // Shellbane gryphon: Pillar frag (was "not in game")
        add("shellbane_gryphon", d("pillar_frag", 1, 1, 128));
    }

    private static void add(String key, Drop... put) {
        MonsterDropsTable.Patch old = PATCHES.get(key);
        List<Drop> lines = new ArrayList<>(old == null ? List.of() : old.put());
        lines.addAll(List.of(put));
        PATCHES.put(key, new MonsterDropsTable.Patch(List.copyOf(lines), old == null ? Set.of() : old.remove()));
    }
}
