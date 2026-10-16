package com.runecraft.osrs.data;

import com.runecraft.osrs.content.CombatProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OSRS Wiki drop-table corrections for regular (non-boss) monsters, applied by
 * {@link com.runecraft.osrs.content.ContentRegistry} after every module has added its creatures and by
 * {@link Monster#drops()} for the built-in monsters (the same idea as {@link MonsterStatsTable} for stats).
 *
 * <p>Generated from the wiki's drop tables (Flipping-Utilities/parsed-osrs {@code all-monsters.json}, with
 * osrsreboxed-db for the allotment/rare seed tables) by a sweep that matched every monster by name and combat
 * level. Shared tables are referenced by marker ({@code #gems}, {@code #rare}, {@code #herbs},
 * {@code #allotment_seeds}, {@code #rare_seeds}, {@code #uncommon_seeds}, {@code #tree_herb_seeds}) with the
 * access rate the wiki's expanded rows imply. Each patch replaces every existing line of the items it names
 * (wiki rarity 1/N rounded to a whole N, one line per wiki quantity), removes the listed items and appends
 * new notable drops (runes, ammunition, ores/bars, seeds, herbs, coins, bones, valuable equipment). Items that
 * are not in RuneCraft are skipped; clue scrolls, champion scrolls, ensouled heads and wilderness/catacomb
 * extras are never touched. Fix a wrong rate here rather than in the module file.
 */
public final class MonsterDropsTable {
    /** One monster's correction: lines that replace (or add) their items, and items to drop entirely. */
    public record Patch(List<Drop> put, Set<String> remove) {
    }

    private static final Map<String, Patch> PATCHES = new LinkedHashMap<>();

    static {
        rows0();
        rows1();
        rows2();
        rows3();
        rows4();
        rows5();
        rows6();
        rows7();
        rows8();
        rows9();
        rows10();
        rows11();
        rows12();
    }

    private MonsterDropsTable() {
    }

    /** Patch key of a built-in {@link Monster}. */
    public static String key(Monster m) {
        return "Monster." + m.name();
    }

    /** The patch for a creature id (or {@code Monster.<NAME>}), or null when it has none. */
    public static Patch patch(String key) {
        return PATCHES.get(key);
    }

    /** Every patch, keyed by creature id. */
    public static Map<String, Patch> patches() {
        return Collections.unmodifiableMap(PATCHES);
    }

    /** {@code drops} with the patch for {@code key} applied (the same list when there is none). */
    public static List<Drop> patched(String key, List<Drop> drops) {
        Patch p = PATCHES.get(key);
        return p == null ? drops : applied(p, drops);
    }

    /** {@code drops} with {@code p} applied (also used for the boss tables, {@link BossDropsTable}). */
    public static List<Drop> applied(Patch p, List<Drop> drops) {
        Map<String, List<Drop>> put = new LinkedHashMap<>();
        // Lines are grouped by base item, so prayer_potion(2) replaces a module's prayer_potion line too.
        for (Drop d : p.put()) {
            put.computeIfAbsent(d.baseItem(), k -> new ArrayList<>()).add(d);
        }
        Set<String> touched = new LinkedHashSet<>(p.remove());
        touched.addAll(put.keySet());
        List<Drop> out = new ArrayList<>();
        for (Drop d : drops) {
            if (touched.contains(d.baseItem())) {
                List<Drop> replacement = put.remove(d.baseItem());
                if (replacement != null) {
                    out.addAll(replacement);
                }
                continue;
            }
            out.add(d);
        }
        put.values().forEach(out::addAll);
        return out;
    }

    /** Applies the patch for a module creature in place; returns false when it has none. */
    public static boolean apply(CombatProfile<?> profile) {
        if (!PATCHES.containsKey(profile.id)) {
            return false;
        }
        List<Drop> next = patched(profile.id, new ArrayList<>(profile.drops));
        profile.drops.clear();
        profile.drops.addAll(next);
        return true;
    }

    private static void p(String key, String[] remove, Drop... put) {
        PATCHES.put(key, new Patch(List.of(put), Set.of(remove)));
    }

    private static String[] rm(String... items) {
        return items;
    }

    private static Drop d(String item, int min, int max, int rarity) {
        return new Drop(item, min, max, rarity);
    }

    private static void rows0() {
        // Giant bat <- parsed:Giant bat(27)
        p("giant_bat", rm("bones"), d("bat_bones", 1, 1, 1));
        // Grizzly bear <- parsed:Grizzly bear(21)
        p("grizzly_bear", rm(), d("bear_fur", 1, 1, 1));
        // Minotaur <- parsed:Minotaur(27)
        p("minotaur", rm("#herbs"), d("#gems", 1, 1, 101), d("iron_arrow", 5, 14, 10), d("bronze_arrow", 3, 3, 34),
                d("mind_rune", 1, 1, 101), d("coins", 7, 7, 5), d("coins", 2, 2, 9), d("coins", 5, 84, 14),
                d("copper_ore", 1, 1, 17), d("tin_ore", 1, 1, 17), d("pure_essence", 15, 15, 20),
                d("rune_essence", 5, 5, 20), d("bronze_spear", 1, 1, 10));
        // Icefiend <- parsed:Icefiend(18)
        p("icefiend", rm("#herbs"), d("#gems", 1, 1, 43), d("air_rune", 1, 5, 128), d("water_rune", 1, 5, 32),
                d("coins", 1, 30, 32), d("coins", 1, 20, 32), d("frozen_tear", 5, 5, 150));
        // Hobgoblin <- parsed:Hobgoblin(28)+rebox seeds+rebox seeds
        p("hobgoblin", rm("iron_dagger"), d("#gems", 1, 1, 64), d("#rare_seeds", 1, 1, 11),
                d("#allotment_seeds", 1, 1, 7), d("law_rune", 2, 2, 43), d("water_rune", 2, 2, 64),
                d("fire_rune", 7, 7, 64), d("body_rune", 6, 6, 64), d("chaos_rune", 3, 3, 64),
                d("nature_rune", 4, 4, 64), d("cosmic_rune", 2, 2, 128), d("coins", 5, 5, 4), d("coins", 15, 15, 8),
                d("coins", 28, 28, 8), d("coins", 10, 10, 18), d("coins", 62, 62, 32), d("coins", 1, 42, 32),
                d("limpwurt_root", 1, 1, 6), d("bronze_spear", 1, 1, 43), d("iron_spear", 1, 1, 64), d("steel_spear", 1, 1, 64));
        // Earth warrior <- parsed:Earth warrior(51)+rebox seeds
        p("earth_warrior", rm(), d("#gems", 1, 1, 64), d("#herbs", 1, 1, 9), d("#rare_seeds", 1, 1, 7),
                d("earth_rune", 12, 12, 10), d("earth_rune", 60, 60, 43), d("nature_rune", 3, 3, 14),
                d("chaos_rune", 3, 3, 18), d("law_rune", 2, 2, 21), d("death_rune", 2, 2, 32),
                d("blood_rune", 2, 2, 128), d("coins", 12, 12, 7), d("steel_spear", 1, 1, 43));
        // Ice warrior <- parsed:Ice warrior(57)+rebox seeds
        p("ice_warrior", rm("water_rune", "mithril_ore"), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 13),
                d("#rare_seeds", 1, 1, 7), d("nature_rune", 4, 4, 13), d("chaos_rune", 3, 3, 16),
                d("law_rune", 2, 2, 18), d("cosmic_rune", 2, 2, 26), d("mithril_arrow", 3, 3, 26),
                d("adamant_arrow", 2, 2, 43), d("death_rune", 2, 2, 43), d("blood_rune", 2, 2, 128),
                d("coins", 15, 15, 3), d("coins", 5, 5, 7), d("coins", 10, 10, 13));
        // Crawling Hand <- parsed:Crawling Hand(8)
        p("crawling_hand", rm("leather"), d("#gems", 1, 1, 64), d("gold_ring", 1, 1, 43), d("sapphire_ring", 1, 1, 64),
                d("emerald_ring", 1, 1, 64), d("coins", 8, 8, 6), d("coins", 5, 5, 6));
        // Cave bug <- parsed:Cave bug(6)
        p("cave_bug", rm("#seeds"), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 6), d("earth_rune", 1, 10, 9),
                d("earth_rune", 6, 6, 64), d("earth_rune", 1, 170, 64), d("water_rune", 1, 15, 9),
                d("water_rune", 8, 8, 26), d("water_rune", 1, 170, 64), d("nature_rune", 1, 1, 26),
                d("nature_rune", 2, 2, 128), d("limpwurt_root", 1, 1, 128), d("snape_grass", 1, 1, 128),
                d("red_spiders_eggs", 1, 1, 64), d("coins", 1, 40, 9), d("coins", 3, 3, 16), d("coins", 8, 8, 43),
                d("water_battlestaff", 1, 1, 128), d("fire_rune", 1, 20, 10), d("fire_rune", 1, 170, 64));
        // Cave crawler <- parsed:Cave crawler(23)+rebox seeds
        p("cave_crawler", rm("#seeds"), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 6), d("#allotment_seeds", 1, 1, 4),
                d("nature_rune", 3, 4, 21), d("fire_rune", 12, 12, 26), d("earth_rune", 9, 9, 64),
                d("coins", 3, 3, 26), d("coins", 8, 8, 43), d("coins", 29, 29, 43), d("coins", 10, 10, 128),
                d("white_berries", 1, 1, 26), d("unicorn_horn_dust", 1, 1, 64), d("red_spiders_eggs", 1, 1, 128),
                d("limpwurt_root", 1, 1, 128), d("snape_grass", 1, 1, 128), d("bronze_boots", 1, 1, 128));
        // Banshee <- parsed:Banshee(23)
        p("banshee", rm("#seeds"), d("#gems", 1, 1, 64), d("air_rune", 3, 3, 43), d("cosmic_rune", 2, 2, 43),
                d("chaos_rune", 3, 3, 64), d("chaos_rune", 7, 7, 128), d("fire_rune", 7, 7, 128),
                d("coins", 13, 13, 13), d("coins", 26, 26, 16), d("coins", 35, 35, 16), d("pure_essence", 13, 13, 6),
                d("iron_ore", 1, 1, 128), d("mystic_gloves_dark", 1, 1, 512));
        // Cave slime <- parsed:Cave slime(23)
        p("cave_slime", rm("iron_ore"), d("#gems", 1, 1, 32), d("water_rune", 15, 15, 26), d("earth_rune", 5, 5, 43),
                d("coins", 10, 10, 3), d("coins", 4, 4, 4), d("coins", 22, 22, 13), d("coins", 1, 1, 18),
                d("coins", 46, 46, 64), d("gold_bar", 1, 1, 64), d("iron_boots", 1, 1, 128));
        // Rockslug <- parsed:Rockslug(29)+rebox seeds
        p("rockslug", rm("#herbs"), d("#gems", 1, 1, 21), d("#allotment_seeds", 1, 1, 14), d("earth_rune", 5, 5, 4),
                d("earth_rune", 42, 42, 32), d("chaos_rune", 2, 2, 32), d("coal", 1, 1, 10), d("iron_ore", 1, 1, 6),
                d("bronze_bar", 1, 1, 64), d("iron_bar", 1, 1, 43), d("copper_ore", 1, 1, 43), d("tin_ore", 1, 1, 16),
                d("mithril_ore", 1, 1, 128), d("mystic_gloves_light", 1, 1, 512));
        // Lizard <- parsed:Lizard(42)+rebox seeds
        p("lizard", rm(), d("#gems", 1, 1, 21), d("#herbs", 1, 1, 13), d("#allotment_seeds", 1, 1, 14),
                d("big_bones", 1, 1, 1), d("fire_rune", 5, 5, 4), d("fire_rune", 42, 42, 9),
                d("nature_rune", 5, 5, 32), d("coal", 1, 1, 10), d("copper_ore", 1, 1, 43), d("iron_ore", 1, 1, 6),
                d("silver_bar", 1, 1, 64), d("silver_ore", 1, 1, 43), d("tin_ore", 1, 1, 32),
                d("mithril_ore", 1, 1, 128), d("mystic_gloves_light", 1, 1, 512));
        // Desert Lizard <- parsed:Desert Lizard(24)+rebox seeds
        p("desert_lizard", rm(), d("#gems", 1, 1, 21), d("#herbs", 1, 1, 13), d("#allotment_seeds", 1, 1, 14),
                d("bones", 1, 1, 1), d("fire_rune", 5, 5, 4), d("fire_rune", 42, 42, 9), d("nature_rune", 5, 5, 32),
                d("coal", 1, 1, 10), d("copper_ore", 1, 1, 43), d("iron_ore", 1, 1, 6), d("silver_bar", 1, 1, 64),
                d("silver_ore", 1, 1, 43), d("tin_ore", 1, 1, 32), d("mithril_ore", 1, 1, 128), d("mystic_gloves_light", 1, 1, 512));
        // Cockatrice <- parsed:Cockatrice(37)+rebox seeds
        p("cockatrice", rm("#seeds"), d("#gems", 1, 1, 64), d("#herbs", 1, 1, 13), d("#allotment_seeds", 1, 1, 7),
                d("iron_javelin", 5, 5, 128), d("nature_rune", 2, 2, 21), d("nature_rune", 4, 4, 32),
                d("nature_rune", 6, 6, 64), d("law_rune", 2, 2, 43), d("water_rune", 2, 2, 64),
                d("fire_rune", 7, 7, 64), d("coins", 15, 15, 8), d("coins", 5, 5, 11), d("coins", 28, 28, 11),
                d("coins", 62, 62, 32), d("coins", 42, 42, 43), d("coins", 1, 1, 128), d("limpwurt_root", 1, 1, 6), d("iron_boots", 1, 1, 128), d("mystic_boots_light", 1, 1, 512));
        // Pyrefiend <- parsed:Pyrefiend(43)
        p("pyrefiend", rm("#herbs"), d("#gems", 1, 1, 43), d("fiendish_ashes", 1, 1, 1),
                d("steel_full_helm", 1, 1, 32), d("fire_rune", 30, 30, 6), d("fire_rune", 60, 60, 16),
                d("chaos_rune", 12, 12, 26), d("death_rune", 3, 3, 43), d("coins", 40, 40, 5), d("coins", 120, 120, 6),
                d("coins", 200, 200, 13), d("coins", 10, 10, 18), d("coins", 450, 450, 64), d("gold_ore", 1, 1, 16), d("steel_boots", 1, 1, 128));
        // Mogre <- parsed:Mogre(60)+rebox seeds
        p("mogre", rm("#herbs", "raw_lobster"), d("#rare_seeds", 1, 1, 10), d("water_rune", 5, 5, 32),
                d("water_rune", 7, 7, 32), d("water_rune", 14, 14, 32), d("raw_swordfish", 1, 1, 6),
                d("raw_tuna", 1, 1, 14), d("raw_pike", 1, 1, 18), d("raw_salmon", 1, 1, 32),
                d("raw_herring", 1, 1, 43), d("raw_sardine", 1, 1, 43), d("raw_shark", 1, 1, 43), d("flippers", 1, 1, 64));
        // Harpie Bug Swarm <- parsed:Harpie Bug Swarm(46)
        p("harpie_bug_swarm", rm("#herbs", "#seeds"), d("#gems", 1, 1, 43), d("fire_rune", 30, 30, 6),
                d("fire_rune", 60, 60, 16), d("chaos_rune", 15, 15, 26), d("death_rune", 3, 3, 43),
                d("coins", 40, 40, 5), d("coins", 160, 160, 6), d("coins", 230, 230, 13), d("coins", 30, 30, 18),
                d("coins", 490, 490, 64), d("gold_ore", 1, 1, 16), d("steel_boots", 1, 1, 128));
        // Wall beast <- parsed:Wall beast(49)
        p("wall_beast", rm("#gems", "steel_longsword"), d("air_rune", 3, 3, 43), d("cosmic_rune", 2, 2, 43),
                d("chaos_rune", 3, 3, 64), d("chaos_rune", 7, 7, 128), d("grimy_guam_leaf", 1, 1, 4),
                d("coins", 15, 15, 13), d("mystic_hat_light", 1, 1, 512));
        // Killerwatt <- parsed:Killerwatt(55)
        p("killerwatt", rm("cosmic_rune"), d("#gems", 1, 1, 28), d("#herbs", 1, 1, 4),
                d("fire_battlestaff", 1, 1, 140), d("air_battlestaff", 1, 1, 140), d("air_rune", 5, 5, 47),
                d("air_rune", 17, 17, 70), d("fire_rune", 45, 45, 18), d("fire_rune", 18, 18, 28),
                d("nature_rune", 8, 8, 13), d("nature_rune", 37, 37, 140), d("chaos_rune", 4, 4, 16),
                d("death_rune", 2, 2, 47), d("steam_rune", 2, 2, 70), d("coins", 44, 44, 5), d("coins", 11, 11, 6),
                d("coins", 200, 200, 13), d("coins", 76, 76, 140), d("coins", 127, 127, 140),
                d("energy_potion", 1, 1, 16), d("energy_potion", 10, 10, 40), d("mystic_fire_staff", 1, 1, 512));
        // Molanisk <- parsed:Molanisk(51)
        p("molanisk", rm("coal"), d("#gems", 1, 1, 18), d("#herbs", 1, 1, 4), d("bones", 1, 1, 1),
                d("water_rune", 1, 14, 13), d("earth_rune", 1, 20, 13), d("nature_rune", 1, 5, 43),
                d("cosmic_rune", 1, 7, 26), d("mud_rune", 1, 15, 64), d("coins", 1, 75, 13));
        // Basilisk <- parsed:Basilisk(61)
        p("basilisk", rm("iron_ore", "mithril_ore"), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 4),
                d("rune_dagger", 1, 1, 128), d("water_rune", 75, 75, 16), d("nature_rune", 15, 15, 26),
                d("nature_rune", 37, 37, 128), d("law_rune", 3, 3, 43), d("adamantite_ore", 1, 1, 43),
                d("coins", 44, 44, 4), d("coins", 200, 200, 8), d("coins", 132, 132, 26), d("coins", 11, 11, 26),
                d("coins", 440, 440, 128), d("mithril_spear", 1, 1, 64), d("mystic_hat_light", 1, 1, 512));
        // Sea Snake Young <- parsed:Sea Snake Young(90)+rebox seeds
        p("sea_snake_young", rm("bones"), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 26), d("#rare_seeds", 1, 1, 9),
                d("big_bones", 1, 1, 1), d("water_rune", 15, 15, 64), d("mist_rune", 1, 1, 64),
                d("broad_arrows", 4, 4, 64), d("coins", 44, 44, 2), d("coins", 32, 32, 12), d("coins", 24, 24, 14),
                d("coins", 23, 23, 18), d("raw_bass", 2, 2, 32), d("raw_lobster", 1, 1, 64));
        // Terror dog <- parsed:Terror dog(100)
        p("terror_dog", rm(), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 9), d("rune_dagger", 1, 1, 64),
                d("water_rune", 75, 75, 8), d("nature_rune", 15, 15, 13), d("law_rune", 3, 3, 21),
                d("fire_rune", 37, 37, 64), d("coins", 44, 44, 2), d("coins", 132, 132, 3), d("coins", 200, 200, 6),
                d("coins", 11, 11, 13), d("coins", 440, 440, 64), d("adamantite_ore", 1, 1, 21),
                d("big_bones", 1, 1, 1));
    }

    private static void rows1() {
        // Fever spider <- parsed:Fever spider(49)
        p("fever_spider", rm("#herbs", "#seeds"), d("#gems", 1, 1, 36), d("red_dhide_chaps", 1, 1, 12),
                d("grimy_kwuarm", 1, 1, 6), d("coins", 200, 600, 4), d("pure_essence", 100, 200, 18),
                d("limpwurt_root", 1, 1, 9));
        // Infernal Mage <- parsed:Infernal Mage(66)
        p("infernal_mage", rm("#herbs", "chaos_rune"), d("earth_rune", 10, 10, 21), d("earth_rune", 36, 36, 32),
                d("earth_rune", 18, 18, 64), d("fire_rune", 10, 10, 21), d("fire_rune", 18, 18, 64),
                d("air_rune", 10, 10, 43), d("air_rune", 18, 18, 64), d("water_rune", 10, 10, 43),
                d("water_rune", 18, 18, 64), d("mind_rune", 18, 18, 64), d("body_rune", 18, 18, 64),
                d("blood_rune", 4, 4, 64), d("death_rune", 7, 7, 7), d("coins", 1, 1, 6), d("coins", 2, 2, 8),
                d("coins", 4, 4, 14), d("coins", 29, 29, 43), d("mystic_boots_dark", 1, 1, 512), d("mystic_hat_dark", 1, 1, 512), d("lava_battlestaff", 1, 1, 1000));
        // Brine rat <- parsed:Brine rat(70)
        p("brine_rat", rm(), d("death_rune", 7, 7, 7), d("air_rune", 18, 18, 64), d("earth_rune", 10, 10, 21),
                d("earth_rune", 36, 36, 32), d("earth_rune", 18, 18, 64), d("water_rune", 10, 10, 43),
                d("water_rune", 18, 18, 64), d("blood_rune", 4, 4, 64), d("raw_lobster", 10, 10, 21),
                d("raw_shark", 3, 3, 21), d("raw_shark", 8, 8, 64), d("raw_shark", 1, 1, 128),
                d("raw_pike", 18, 18, 64), d("raw_swordfish", 9, 9, 64), d("coins", 1, 1, 6), d("coins", 2, 2, 8),
                d("coins", 4, 4, 14), d("coins", 29, 29, 43), d("water_talisman", 1, 1, 43));
        // Bloodveld <- parsed:Bloodveld(76)
        p("bloodveld", rm(), d("#gems", 1, 1, 32), d("#herbs", 1, 1, 128), d("steel_full_helm", 1, 1, 32),
                d("rune_med_helm", 1, 1, 128), d("fire_rune", 60, 60, 16), d("blood_rune", 10, 10, 26),
                d("blood_rune", 3, 3, 43), d("blood_rune", 30, 30, 128), d("coins", 120, 120, 4),
                d("coins", 40, 40, 4), d("coins", 200, 200, 13), d("coins", 10, 10, 18), d("coins", 450, 450, 128),
                d("bones", 1, 1, 13), d("big_bones", 1, 1, 18), d("big_bones", 3, 3, 43), d("gold_ore", 1, 1, 64), d("black_boots", 1, 1, 128));
        // Mutated Bloodveld <- parsed:Mutated Bloodveld(123)
        p("mutated_bloodveld", rm("#herbs", "rune_full_helm"), d("#gems", 1, 1, 64), d("#rare", 1, 1, 43),
                d("adamant_knife", 2, 2, 43), d("mithril_platebody", 1, 1, 64), d("rune_med_helm", 1, 1, 64),
                d("rune_dagger", 1, 1, 128), d("air_rune", 105, 105, 18), d("fire_rune", 75, 75, 14),
                d("blood_rune", 30, 30, 10), d("blood_rune", 7, 7, 13), d("soul_rune", 10, 10, 18),
                d("gold_ore", 1, 1, 18), d("mithril_bar", 1, 1, 26), d("ruby_amulet", 1, 1, 64));
        // Jelly <- parsed:Jelly(78)
        p("jelly", rm("#herbs"), d("rune_full_helm", 1, 1, 43), d("coins", 102, 102, 3), d("coins", 44, 44, 4),
                d("coins", 220, 220, 13), d("coins", 11, 11, 18), d("coins", 460, 460, 64),
                d("chaos_rune", 15, 15, 26), d("death_rune", 5, 5, 43), d("gold_bar", 1, 1, 64), d("mithril_boots", 1, 1, 128));
        // Turoth <- parsed:Turoth(87)
        p("turoth", rm("#seeds"), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 2), d("#uncommon_seeds", 1, 1, 7),
                d("bones", 1, 1, 1), d("leaf_bladed_sword", 1, 1, 500), d("rune_dagger", 1, 1, 129),
                d("law_rune", 3, 3, 22), d("nature_rune", 15, 15, 26), d("nature_rune", 37, 37, 129),
                d("limpwurt_root", 1, 1, 18), d("coins", 44, 44, 4), d("coins", 132, 132, 11),
                d("coins", 440, 440, 129), d("mystic_robe_bottom_light", 1, 1, 512));
        // Mutated Zygomite <- parsed:Zygomite(74)
        p("mutated_zygomite", rm("#seeds", "#herbs"), d("#gems", 1, 1, 29), d("#allotment_seeds", 1, 1, 52),
                d("nature_rune", 2, 2, 5), d("earth_rune", 15, 15, 29), d("grimy_ranarr_weed", 2, 2, 24),
                d("grimy_tarromin", 1, 1, 72), d("grimy_harralander", 1, 1, 143), d("coins", 64, 64, 4),
                d("coins", 120, 120, 14), d("coins", 24, 24, 48), d("coins", 250, 250, 72));
        // Cave horror <- parsed:Cave horror(80)+rebox seeds
        p("cave_horror", rm("#seeds"), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 10), d("#uncommon_seeds", 1, 1, 7),
                d("#allotment_seeds", 1, 1, 6), d("rune_dagger", 1, 1, 128), d("nature_rune", 6, 6, 21),
                d("nature_rune", 4, 4, 26), d("nature_rune", 3, 3, 128), d("coins", 44, 44, 5),
                d("coins", 132, 132, 11), d("coins", 440, 440, 128), d("limpwurt_root", 1, 1, 18),
                d("teak_logs", 4, 4, 18), d("mahogany_logs", 2, 2, 43));
        // Aberrant spectre <- parsed:Aberrant spectre(96)
        p("aberrant_spectre", rm("#seeds", "steel_longsword", "lantadyme"), d("#gems", 1, 1, 26),
                d("#uncommon_seeds", 1, 1, 7), d("adamant_platelegs", 1, 1, 128), d("rune_full_helm", 1, 1, 128),
                d("coins", 460, 460, 128), d("lava_battlestaff", 1, 1, 128), d("mystic_robe_bottom_dark", 1, 1, 512));
        // Spiritual ranger <- parsed:Spiritual ranger(115)
        p("spiritual_ranger", rm("#herbs"), d("#gems", 1, 1, 128), d("mithril_arrow", 1, 1, 12),
                d("steel_arrow", 12, 12, 32), d("iron_arrow", 12, 12, 9), d("bronze_arrow", 16, 16, 10),
                d("bronze_arrow", 1, 1, 128), d("body_rune", 12, 12, 32), d("rune_arrow", 5, 5, 64),
                d("rune_arrow", 12, 12, 64), d("rune_arrow", 50, 50, 64), d("adamant_arrow", 3, 3, 43),
                d("adamant_arrow", 12, 12, 128), d("headless_arrow", 12, 12, 13), d("adamant_bolts", 15, 15, 18),
                d("chaos_rune", 1, 1, 43), d("nature_rune", 1, 1, 32), d("coins", 500, 500, 14),
                d("coins", 1300, 1337, 21), d("coins", 1, 1, 64), d("prayer_potion(2)", 1, 1, 14),
                d("ranging_potion(2)", 1, 1, 12), d("adamantite_bar", 1, 4, 16), d("blue_dragon_scale", 2, 2, 18),
                d("coal", 1, 10, 16), d("green_dhide_body", 1, 1, 18), d("pure_essence", 23, 23, 16), d("super_defence(1)", 1, 1, 64));
        // Spiritual warrior <- parsed:Spiritual warrior(123)
        p("spiritual_warrior", rm("#herbs", "rune_full_helm", "adamant_platebody"), d("#gems", 1, 1, 32),
                d("adamant_2h_sword", 1, 1, 127), d("rune_longsword", 1, 1, 127), d("rune_kiteshield", 1, 1, 127),
                d("air_rune", 150, 150, 64), d("chaos_rune", 1, 1, 43), d("mind_rune", 1, 1, 18),
                d("mud_rune", 15, 15, 18), d("nature_rune", 1, 1, 32), d("coins", 400, 499, 14),
                d("coins", 1300, 1337, 21), d("coins", 1, 1, 64), d("prayer_potion(2)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("pure_essence", 23, 23, 16), d("rune_halberd", 1, 1, 127), d("super_attack(1)", 1, 1, 12), d("super_defence(1)", 1, 1, 64), d("super_strength(1)", 1, 1, 64));
        // Spiritual mage <- parsed:Spiritual mage(120)
        p("spiritual_mage", rm("#herbs"), d("#gems", 1, 1, 64), d("air_rune", 30, 30, 14), d("air_rune", 250, 250, 18),
                d("air_rune", 50, 50, 64), d("water_rune", 15, 15, 16), d("water_rune", 50, 50, 18),
                d("fire_rune", 65, 65, 64), d("dust_rune", 15, 15, 12), d("mud_rune", 20, 20, 18),
                d("mud_rune", 40, 70, 18), d("mist_rune", 20, 20, 21), d("chaos_rune", 10, 10, 7),
                d("chaos_rune", 25, 25, 128), d("astral_rune", 38, 98, 9), d("astral_rune", 15, 15, 14),
                d("death_rune", 15, 15, 16), d("death_rune", 25, 25, 32), d("nature_rune", 25, 25, 18),
                d("nature_rune", 18, 18, 32), d("mind_rune", 50, 50, 32), d("body_rune", 35, 35, 43),
                d("body_rune", 25, 25, 64), d("law_rune", 15, 15, 64), d("blood_rune", 10, 10, 64),
                d("blood_rune", 25, 25, 64), d("pure_essence", 46, 46, 16), d("pure_essence", 90, 90, 21),
                d("dragon_boots", 1, 1, 64), d("lava_rune", 30, 60, 14), d("smoke_rune", 100, 150, 64),
                d("soul_rune", 25, 25, 43), d("grimy_avantoe", 1, 1, 41), d("grimy_ranarr_weed", 1, 1, 51),
                d("grimy_snapdragon", 1, 1, 51), d("grimy_torstol", 1, 1, 14), d("coins", 1300, 1337, 21),
                d("coins", 6900, 6942, 64), d("super_restore(3)", 1, 1, 14), d("adamantite_bar", 1, 4, 16),
                d("coal", 1, 10, 16), d("antipoison(3)", 5, 5, 26), d("super_defence(3)", 1, 1, 64), d("ancient_brew(2)", 1, 1, 12), d("ancient_brew(3)", 1, 1, 32));
        // Dust devil <- parsed:Dust devil(93)
        p("dust_devil", rm("adamant_dagger"), d("#gems", 1, 1, 12), d("#herbs", 1, 1, 7), d("bones", 1, 1, 1),
                d("rune_dagger", 1, 1, 64), d("red_dhide_vambraces", 1, 1, 64), d("black_dhide_vambraces", 1, 1, 128),
                d("air_battlestaff", 1, 1, 64), d("earth_battlestaff", 1, 1, 64), d("dragon_dagger", 1, 1, 128),
                d("dust_rune", 200, 200, 13), d("earth_rune", 300, 300, 13), d("fire_rune", 300, 300, 13),
                d("fire_rune", 50, 50, 128), d("chaos_rune", 65, 65, 18), d("rune_arrow", 12, 12, 26),
                d("soul_rune", 15, 15, 32), d("soul_rune", 50, 50, 128), d("coins", 2000, 4000, 5),
                d("mithril_bar", 10, 10, 43), d("adamantite_bar", 4, 4, 128), d("mystic_air_staff", 1, 1, 128), d("mystic_earth_staff", 1, 1, 128));
        // Spitting wyvern <- parsed:Spitting Wyvern(139)
        p("spitting_wyvern", rm("#herbs", "#gems", "rune_battleaxe"), d("air_battlestaff", 1, 1, 31),
                d("battlestaff", 3, 5, 41), d("rune_pickaxe", 1, 1, 61), d("adamant_platebody", 1, 1, 61),
                d("granite_longsword", 1, 1, 512), d("granite_boots", 1, 1, 2560), d("adamant_arrow", 38, 42, 20),
                d("water_rune", 50, 50, 31), d("chaos_rune", 15, 15, 31), d("law_rune", 15, 15, 31),
                d("death_rune", 15, 15, 31), d("blood_rune", 15, 15, 31), d("soul_rune", 10, 10, 123),
                d("rune_bolts", 12, 30, 123), d("grimy_ranarr_weed", 1, 1, 61), d("grimy_torstol", 1, 1, 61),
                d("seaweed_spore", 12, 12, 61), d("pure_essence", 150, 150, 15), d("adamantite_bar", 2, 4, 20),
                d("teak_logs", 35, 35, 20), d("snape_grass", 10, 15, 41), d("runite_ore", 1, 2, 41),
                d("coins", 3000, 3000, 11), d("prayer_potion", 1, 1, 18), d("adamant_crossbow_u", 1, 1, 61));
        // Taloned wyvern <- parsed:Taloned Wyvern(147)
        p("taloned_wyvern", rm("#herbs", "#gems", "rune_battleaxe"), d("air_battlestaff", 1, 1, 31),
                d("battlestaff", 3, 5, 41), d("rune_pickaxe", 1, 1, 61), d("adamant_platebody", 1, 1, 61),
                d("granite_longsword", 1, 1, 512), d("granite_boots", 1, 1, 2560), d("adamant_arrow", 38, 42, 20),
                d("water_rune", 50, 50, 31), d("chaos_rune", 15, 15, 31), d("law_rune", 15, 15, 31),
                d("death_rune", 15, 15, 31), d("blood_rune", 15, 15, 31), d("soul_rune", 10, 10, 123),
                d("rune_bolts", 12, 30, 123), d("grimy_ranarr_weed", 1, 1, 61), d("grimy_torstol", 1, 1, 61),
                d("seaweed_spore", 12, 12, 61), d("pure_essence", 150, 150, 15), d("adamantite_bar", 2, 4, 20),
                d("teak_logs", 35, 35, 20), d("snape_grass", 10, 15, 41), d("runite_ore", 1, 2, 41),
                d("coins", 3000, 3000, 11), d("prayer_potion", 1, 1, 18), d("adamant_crossbow_u", 1, 1, 61));
        // Ankou <- parsed:Ankou(75)+rebox seeds
        p("ankou", rm(), d("#gems", 1, 1, 50), d("#herbs", 1, 1, 33), d("#rare_seeds", 1, 1, 100),
                d("death_rune", 10, 10, 10), d("blood_rune", 11, 11, 17), d("blood_rune", 5, 5, 33),
                d("adamant_arrow", 5, 14, 25), d("law_rune", 2, 2, 14), d("pure_essence", 15, 15, 20),
                d("mithril_ore", 3, 7, 50), d("coins", 8, 8, 3), d("coins", 5, 204, 10), d("black_knife", 1, 1, 100));
        // Hellhound <- parsed:Hellhound(122)
        p("hellhound", rm(), d("death_rune", 10, 10, 8), d("death_rune", 20, 20, 17), d("death_rune", 50, 50, 50));
        // Fire giant <- parsed:Fire giant(86)
        p("fire_giant", rm(), d("#gems", 1, 1, 12), d("#rare", 1, 1, 140), d("#herbs", 1, 1, 7),
                d("steel_axe", 1, 1, 43), d("fire_battlestaff", 1, 1, 128), d("fire_rune", 150, 150, 13),
                d("fire_rune", 37, 37, 128), d("chaos_rune", 5, 5, 18), d("steel_bar", 1, 1, 64),
                d("strength_potion(2)", 1, 1, 128));
        // Kalphite Worker <- parsed:Kalphite Worker(28)
        p("kalphite_worker", rm(), d("#gems", 1, 1, 64), d("#herbs", 1, 1, 18), d("iron_javelin", 5, 5, 128),
                d("body_rune", 6, 6, 64), d("cosmic_rune", 2, 2, 128), d("chaos_rune", 3, 3, 64),
                d("fire_rune", 7, 7, 64), d("water_rune", 2, 2, 64), d("law_rune", 2, 2, 43),
                d("nature_rune", 4, 4, 64), d("coins", 15, 15, 4), d("coins", 5, 5, 11), d("coins", 28, 28, 11),
                d("coins", 1, 1, 16), d("coins", 62, 62, 32), d("coins", 42, 42, 43));
        // Kalphite Soldier <- parsed:Kalphite Soldier(85)
        p("kalphite_soldier", rm(), d("#gems", 1, 1, 32), d("#herbs", 1, 1, 128), d("fire_rune", 60, 60, 16),
                d("fire_rune", 30, 30, 128), d("chaos_rune", 12, 12, 26), d("death_rune", 3, 3, 43),
                d("nature_rune", 1, 4, 64), d("coins", 120, 120, 3), d("coins", 40, 40, 4), d("coins", 200, 200, 13),
                d("coins", 10, 10, 18), d("coins", 450, 450, 128));
        // Kalphite Guardian <- parsed:Kalphite Guardian(141)
        p("kalphite_guardian", rm(), d("#gems", 1, 1, 26), d("#rare", 1, 1, 133), d("#herbs", 1, 1, 6),
                d("rune_med_helm", 1, 1, 128), d("rune_chainbody", 1, 1, 128), d("air_rune", 50, 50, 16),
                d("chaos_rune", 10, 10, 18), d("blood_rune", 7, 7, 32), d("fire_rune", 37, 37, 128),
                d("defence_potion(3)", 1, 1, 128));
        // Mountain troll <- parsed:Mountain troll(69)+rebox seeds
        p("mountain_troll", rm(), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 9), d("#rare_seeds", 1, 1, 7),
                d("earth_rune", 60, 60, 16), d("earth_rune", 45, 45, 128), d("earth_rune", 25, 25, 128),
                d("nature_rune", 7, 7, 26), d("law_rune", 2, 2, 43), d("coins", 35, 35, 4), d("coins", 100, 100, 13),
                d("coins", 8, 8, 18), d("coins", 50, 50, 21), d("coins", 250, 250, 128), d("coal", 3, 3, 43),
                d("raw_mackerel", 3, 3, 64));
        // Ogre <- parsed:Ogre(53)+rebox seeds
        p("ogre", rm("#herbs"), d("#rare_seeds", 1, 1, 7));
        // Cyclops <- parsed:Cyclops(76)
        p("cyclops", rm(), d("#gems", 1, 1, 50), d("#rare", 1, 1, 50), d("#herbs", 1, 1, 33),
                d("adamant_2h_sword", 1, 1, 100), d("rune_defender", 1, 1, 50), d("black_knife", 4, 13, 6));
    }

    private static void rows2() {
        // Dagannoth <- parsed:Dagannoth(74)
        p("dagannoth", rm("#herbs", "raw_shark"), d("#gems", 1, 1, 128), d("#uncommon_seeds", 1, 1, 7),
                d("water_rune", 15, 15, 32), d("steel_arrow", 15, 15, 64), d("mithril_javelin", 3, 3, 128),
                d("raw_herring", 3, 3, 32), d("raw_sardine", 5, 5, 32), d("raw_lobster", 1, 1, 64),
                d("raw_tuna", 1, 1, 64), d("coins", 56, 56, 4), d("coins", 25, 25, 14), d("coins", 44, 44, 16),
                d("coins", 41, 41, 21), d("iron_spear", 1, 1, 21), d("bronze_spear", 1, 1, 26), d("mithril_spear", 1, 1, 128));
        // Elf warrior <- parsed:Elf Warrior(108)
        p("elf_warrior", rm(), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 9), d("green_dhide_body", 1, 1, 32),
                d("water_rune", 70, 70, 16), d("nature_rune", 12, 12, 26), d("law_rune", 2, 2, 43),
                d("fire_rune", 37, 37, 43), d("coins", 44, 44, 4), d("coins", 180, 180, 13), d("coins", 20, 20, 26),
                d("adamantite_ore", 1, 1, 128));
        // Lizardman <- parsed:Lizardman(62)
        p("lizardman", rm("#herbs"));
        // Lizardman shaman <- parsed:Lizardman shaman(150)
        p("lizardman_shaman", rm("#herbs", "#gems", "bones"), d("#rare", 1, 1, 25), d("big_bones", 1, 1, 1),
                d("rune_med_helm", 1, 1, 28), d("earth_battlestaff", 1, 1, 29), d("rune_warhammer", 1, 1, 31),
                d("rune_chainbody", 1, 1, 42), d("red_dhide_vambraces", 1, 1, 50), d("air_rune", 60, 80, 20),
                d("chaos_rune", 40, 60, 20), d("fire_rune", 60, 80, 20), d("coal", 20, 25, 25),
                d("iron_ore", 30, 35, 25), d("runite_ore", 3, 5, 62));
        // Waterfiend <- parsed:Waterfiend(115)
        p("waterfiend", rm(), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 14), d("#uncommon_seeds", 1, 1, 32),
                d("water_rune", 1, 1, 1), d("water_rune", 150, 150, 21), d("fiendish_ashes", 1, 1, 1),
                d("rune_med_helm", 1, 1, 64), d("water_battlestaff", 1, 1, 32), d("blue_dhide_vambraces", 1, 1, 128),
                d("rune_full_helm", 1, 1, 128), d("sapphire_bolts", 15, 15, 26), d("mithril_arrow", 90, 90, 13),
                d("blood_rune", 17, 17, 43), d("death_rune", 23, 23, 26), d("mist_rune", 25, 75, 64),
                d("mud_rune", 75, 100, 64), d("steam_rune", 40, 60, 64), d("mithril_ore", 10, 20, 32),
                d("raw_lobster", 18, 18, 43), d("raw_shark", 8, 8, 43), d("mithril_bar", 10, 15, 128),
                d("coins", 2000, 3000, 9), d("water_talisman", 1, 1, 32), d("snape_grass", 20, 30, 64), d("mystic_water_staff", 1, 1, 128));
        // Wyrm <- parsed:Wyrm(97)
        p("wyrm", rm("death_rune", "chaos_rune"), d("#gems", 1, 1, 76), d("#herbs", 1, 1, 19),
                d("#uncommon_seeds", 1, 1, 25), d("dragon_knife", 75, 150, 10000),
                d("dragon_thrownaxe", 75, 150, 10000), d("red_dhide_chaps", 1, 1, 25), d("adamant_2h_sword", 1, 1, 38),
                d("earth_battlestaff", 1, 1, 76), d("rune_med_helm", 1, 1, 38), d("rune_battleaxe", 1, 1, 76),
                d("dragon_dagger", 1, 1, 76), d("fire_rune", 200, 200, 8), d("earth_rune", 75, 150, 8),
                d("soul_rune", 15, 20, 15), d("blood_rune", 25, 30, 15), d("coins", 950, 1450, 10),
                d("pure_essence", 200, 300, 25));
        // Kurask <- parsed:Kurask(106)
        p("kurask", rm("#seeds", "#rare"), d("#gems", 1, 1, 21), d("#herbs", 1, 1, 2), d("#uncommon_seeds", 1, 1, 8),
                d("bones", 1, 1, 1), d("rune_longsword", 1, 1, 41), d("adamant_platebody", 1, 1, 41),
                d("rune_axe", 1, 1, 41), d("nature_rune", 10, 10, 12), d("nature_rune", 15, 15, 18),
                d("nature_rune", 30, 30, 31), d("coins", 2000, 3000, 8), d("coins", 10000, 10000, 25),
                d("white_berries", 12, 12, 21), d("big_bones", 20, 20, 25), d("mystic_robe_top_light", 1, 1, 512));
        // Skeletal Wyvern <- parsed:Skeletal Wyvern(140)
        p("skeletal_wyvern", rm("#gems", "runite_ore"), d("#rare", 1, 1, 43), d("#herbs", 1, 1, 18),
                d("dragon_plateskirt", 1, 1, 512), d("earth_battlestaff", 1, 1, 32), d("battlestaff", 10, 10, 43),
                d("rune_axe", 1, 1, 43), d("rune_warhammer", 1, 1, 64), d("rune_full_helm", 1, 1, 64),
                d("air_rune", 225, 225, 21), d("water_rune", 150, 150, 32), d("chaos_rune", 80, 80, 32),
                d("blood_rune", 25, 25, 32), d("adamant_bolts", 75, 99, 43), d("rune_bolts", 35, 44, 43),
                d("soul_rune", 20, 20, 128), d("pure_essence", 250, 250, 16), d("magic_logs", 35, 35, 21),
                d("adamantite_bar", 10, 10, 21), d("iron_ore", 200, 200, 43), d("prayer_potion", 2, 2, 18),
                d("ranarr_seed", 3, 3, 64), d("snapdragon_seed", 1, 1, 64));
        // Gargoyle <- parsed:Gargoyle(111)
        p("gargoyle", rm("#rare"), d("#gems", 1, 1, 26), d("granite_maul", 1, 1, 256),
                d("adamant_platelegs", 1, 1, 32), d("rune_full_helm", 1, 1, 43), d("rune_2h_sword", 1, 1, 64),
                d("rune_battleaxe", 1, 1, 128), d("rune_platelegs", 1, 1, 128), d("fire_rune", 75, 75, 13),
                d("fire_rune", 150, 150, 21), d("chaos_rune", 30, 30, 16), d("death_rune", 15, 15, 26),
                d("gold_ore", 10, 20, 13), d("pure_essence", 150, 150, 21), d("steel_bar", 15, 15, 21),
                d("gold_bar", 10, 15, 43), d("mithril_bar", 15, 15, 64), d("runite_ore", 1, 1, 64),
                d("coins", 400, 800, 5), d("coins", 500, 1000, 6), d("coins", 10000, 10000, 26), d("adamant_boots", 1, 1, 128));
        // Nechryael <- parsed:Nechryael(115)
        p("nechryael", rm("#herbs"), d("#gems", 1, 1, 23), d("#rare", 1, 1, 121), d("#uncommon_seeds", 1, 1, 3),
                d("adamant_platelegs", 1, 1, 29), d("adamant_kiteshield", 1, 1, 58), d("chaos_rune", 37, 37, 14),
                d("blood_rune", 15, 20, 29));
        // Greater Nechryael <- parsed:Greater Nechryael(200)
        p("greater_nechryael", rm("rune_kiteshield"), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 18),
                d("#uncommon_seeds", 1, 1, 13), d("adamant_kiteshield", 1, 1, 18), d("rune_axe", 1, 1, 18),
                d("rune_sq_shield", 1, 1, 18), d("rune_med_helm", 1, 1, 32), d("rune_chainbody", 1, 1, 128),
                d("death_rune", 23, 23, 11), d("blood_rune", 20, 20, 13), d("chaos_rune", 50, 50, 13),
                d("air_rune", 150, 150, 21), d("soul_rune", 25, 25, 26), d("coins", 2000, 2500, 16),
                d("gold_bar", 5, 5, 18), d("wine_of_zamorak", 3, 3, 64));
        // Abyssal demon <- parsed:Abyssal demon(124)
        p("abyssal_demon", rm("rune_full_helm"), d("#gems", 1, 1, 26), d("#rare", 1, 1, 65), d("#herbs", 1, 1, 7),
                d("rune_chainbody", 1, 1, 128), d("rune_med_helm", 1, 1, 128), d("air_rune", 50, 50, 16),
                d("chaos_rune", 10, 10, 18), d("blood_rune", 7, 7, 32), d("pure_essence", 60, 60, 26),
                d("adamantite_bar", 1, 1, 64), d("cosmic_talisman", 1, 1, 128), d("defence_potion(3)", 1, 1, 128), d("defence_potion(3)", 1, 1, 68));
        // Cave kraken <- parsed:Cave kraken(127)
        p("cave_kraken", rm("raw_shark", "rune_kiteshield"), d("#gems", 1, 1, 33), d("#herbs", 1, 1, 33),
                d("#uncommon_seeds", 1, 1, 33), d("rune_med_helm", 1, 1, 31), d("rune_warhammer", 1, 1, 50),
                d("battlestaff", 1, 1, 50), d("water_battlestaff", 1, 1, 50), d("steam_rune", 7, 7, 33),
                d("water_rune", 15, 15, 20), d("water_rune", 30, 30, 20), d("water_rune", 75, 75, 20),
                d("fire_rune", 30, 30, 20), d("death_rune", 30, 30, 12), d("chaos_rune", 50, 50, 12),
                d("blood_rune", 5, 5, 25), d("coins", 120, 300, 20), d("raw_lobster", 3, 3, 50),
                d("water_talisman", 1, 1, 50), d("kraken_tentacle", 1, 1, 1200), d("adamant_spear", 1, 1, 50), d("mystic_water_staff", 1, 1, 100));
        // Dark beast <- parsed:Dark beast(182)
        p("dark_beast", rm(), d("#gems", 1, 1, 43), d("#rare", 1, 1, 43), d("#herbs", 1, 1, 7),
                d("#uncommon_seeds", 1, 1, 43), d("rune_chainbody", 1, 1, 128), d("rune_med_helm", 1, 1, 128),
                d("rune_full_helm", 1, 1, 128), d("chaos_rune", 30, 30, 18), d("blood_rune", 15, 15, 32),
                d("adamantite_bar", 3, 3, 64), d("adamantite_ore", 5, 5, 128), d("death_talisman", 1, 1, 128),
                d("runite_ore", 1, 1, 128));
        // Drake <- parsed:Drake(192)
        p("drake", rm("runite_ore"), d("#gems", 1, 1, 93), d("#herbs", 1, 1, 9), d("#uncommon_seeds", 1, 1, 93),
                d("drakes_tooth", 1, 1, 2560), d("drakes_claw", 1, 1, 2560), d("dragon_thrownaxe", 100, 200, 10000),
                d("dragon_knife", 100, 200, 10000), d("rune_full_helm", 1, 1, 31), d("red_dhide_body", 1, 1, 46),
                d("black_dhide_vambraces", 1, 1, 93), d("dragon_mace", 1, 1, 93), d("fire_rune", 100, 200, 9),
                d("nature_rune", 30, 60, 9), d("law_rune", 25, 50, 9), d("death_rune", 20, 40, 9),
                d("rune_arrow", 35, 65, 9), d("raw_shark", 4, 6, 62), d("raw_anglerfish", 4, 6, 93),
                d("coins", 1000, 2000, 23), d("coins", 5000, 7000, 93), d("diamond", 3, 6, 23));
        // Smoke devil <- parsed:Smoke devil(160)
        p("smoke_devil", rm(), d("#gems", 1, 1, 32), d("#rare", 1, 1, 32), d("#herbs", 1, 1, 5),
                d("rune_dagger", 1, 1, 43), d("air_battlestaff", 1, 1, 43), d("black_dhide_vambraces", 1, 1, 43),
                d("fire_battlestaff", 1, 1, 43), d("rune_full_helm", 1, 1, 64), d("red_dhide_body", 1, 1, 128),
                d("smoke_rune", 15, 15, 12), d("smoke_rune", 40, 40, 26), d("rune_bolts", 15, 15, 26),
                d("fire_rune", 37, 37, 32), d("fire_rune", 150, 150, 64), d("air_rune", 37, 37, 32),
                d("soul_rune", 10, 10, 32), d("steel_bar", 2, 2, 43), d("magic_logs", 5, 5, 43), d("coal", 15, 15, 43),
                d("adamantite_bar", 1, 1, 64));
        // Hydra <- parsed:Hydra(194)
        p("hydra", rm("runite_bar"), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 32), d("#uncommon_seeds", 1, 1, 32),
                d("hydra_tail", 1, 1, 5001), d("dragon_thrownaxe", 200, 400, 10000),
                d("dragon_knife", 200, 400, 10001), d("black_dhide_chaps", 1, 1, 43), d("battlestaff", 2, 3, 64),
                d("fire_battlestaff", 1, 1, 26), d("water_battlestaff", 1, 1, 26), d("rune_kiteshield", 1, 1, 43),
                d("rune_platebody", 1, 1, 128), d("dragon_longsword", 1, 1, 128), d("blood_rune", 15, 45, 14),
                d("chaos_rune", 20, 50, 14), d("death_rune", 30, 60, 14), d("fire_rune", 70, 90, 14),
                d("law_rune", 30, 60, 14), d("water_rune", 70, 90, 14), d("coins", 500, 3500, 8),
                d("super_combat_potion(1)", 1, 1, 32), d("dragon_bones", 3, 5, 43), d("super_restore(1)", 1, 2, 21),
                d("hydras_eye", 1, 1, 1801), d("hydras_fang", 1, 1, 1801), d("hydras_heart", 1, 1, 1801));
        // Basilisk Knight <- parsed:Basilisk Knight(204)
        p("basilisk_knight", rm("#herbs", "rune_full_helm", "runite_bar"), d("#gems", 1, 1, 6),
                d("adamant_platelegs", 1, 1, 52), d("adamant_kiteshield", 1, 1, 52), d("rune_axe", 1, 1, 26),
                d("rune_battleaxe", 1, 1, 52), d("rune_dagger", 1, 1, 52), d("rune_scimitar", 1, 1, 52),
                d("rune_med_helm", 1, 1, 52), d("astral_rune", 15, 35, 9), d("nature_rune", 15, 30, 9),
                d("law_rune", 20, 30, 9), d("death_rune", 10, 25, 17), d("blood_rune", 8, 20, 17),
                d("coins", 500, 2500, 7), d("adamantite_ore", 1, 2, 52), d("basilisk_jaw", 1, 1, 5000));
        // Baby blue dragon <- parsed:Baby blue dragon(48)
        p("baby_blue_dragon", rm("#herbs"), d("frozen_tear", 6, 6, 100));
        // Baby red dragon <- parsed:Baby red dragon(48)
        p("baby_red_dragon", rm("#herbs"));
        // Green dragon <- parsed:Green dragon(79)
        p("green_dragon", rm(), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 9), d("adamant_full_helm", 1, 1, 128),
                d("water_rune", 75, 75, 16), d("fire_rune", 37, 37, 128), d("nature_rune", 15, 15, 26),
                d("law_rune", 3, 3, 43), d("coins", 44, 44, 4), d("coins", 132, 132, 5), d("coins", 200, 200, 13),
                d("coins", 11, 11, 26), d("coins", 440, 440, 128), d("adamantite_ore", 1, 1, 43), d("mithril_spear", 1, 1, 64));
        // Blue dragon <- parsed:Blue dragon(111)
        p("blue_dragon", rm(), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 9), d("adamant_full_helm", 1, 1, 128),
                d("water_rune", 75, 75, 16), d("nature_rune", 15, 15, 26), d("law_rune", 3, 3, 43),
                d("fire_rune", 37, 37, 128), d("coins", 44, 44, 4), d("coins", 132, 132, 5), d("coins", 200, 200, 13),
                d("coins", 11, 11, 26), d("coins", 440, 440, 128), d("adamantite_ore", 1, 1, 43),
                d("frozen_tear", 8, 8, 100), d("mithril_spear", 1, 1, 64));
        // Red dragon <- parsed:Red dragon(152)
        p("red_dragon", rm("rune_full_helm"), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 64), d("rune_dart", 8, 8, 43),
                d("mithril_javelin", 20, 20, 128), d("adamant_platebody", 1, 1, 128), d("rune_arrow", 4, 4, 16),
                d("law_rune", 4, 4, 26), d("blood_rune", 2, 2, 32), d("death_rune", 5, 5, 43), d("coins", 196, 196, 3),
                d("coins", 66, 66, 4), d("coins", 330, 330, 13), d("coins", 690, 690, 128),
                d("adamantite_bar", 1, 1, 128));
        // Black dragon <- parsed:Black dragon(227)
        p("black_dragon", rm("#herbs", "rune_full_helm"), d("#gems", 1, 1, 43), d("#rare", 1, 1, 65),
                d("rune_knife", 2, 2, 43), d("adamant_platebody", 1, 1, 128), d("adamant_dart", 16, 16, 18),
                d("fire_rune", 50, 50, 16), d("blood_rune", 15, 15, 43), d("air_rune", 75, 75, 128),
                d("adamantite_bar", 1, 1, 43));
        // Brutal green dragon <- parsed:Brutal green dragon(227)
        p("brutal_green_dragon", rm("rune_platelegs"), d("#gems", 1, 1, 64), d("#rare", 1, 1, 43),
                d("#herbs", 1, 1, 9), d("adamant_dart", 25, 25, 26), d("adamant_2h_sword", 1, 1, 32),
                d("adamant_knife", 8, 8, 43), d("adamant_kiteshield", 1, 1, 128), d("adamant_platelegs", 1, 1, 128),
                d("rune_full_helm", 1, 1, 128), d("rune_chainbody", 1, 1, 128), d("blood_rune", 20, 20, 4),
                d("lava_rune", 35, 35, 16), d("steam_rune", 37, 37, 21), d("adamant_arrow", 8, 8, 43),
                d("mithril_ore", 5, 5, 43), d("mithril_hasta", 1, 1, 43), d("rune_thrownaxe", 8, 8, 43), d("adamant_spear", 1, 1, 64));
    }

    private static void rows3() {
        // Brutal blue dragon <- parsed:Brutal blue dragon(271)
        p("brutal_blue_dragon", rm("#herbs", "rune_platelegs"), d("#gems", 1, 1, 32), d("#rare", 1, 1, 87),
                d("adamant_platelegs", 1, 1, 18), d("rune_longsword", 1, 1, 26), d("blue_dhide_body", 1, 1, 64),
                d("blue_dhide_vambraces", 1, 1, 128), d("dragon_dagger", 1, 1, 128), d("dragon_longsword", 1, 1, 128),
                d("rune_full_helm", 1, 1, 128), d("rune_platebody", 1, 1, 128), d("chaos_rune", 18, 18, 16),
                d("air_rune", 50, 50, 18), d("adamant_dart", 10, 10, 26), d("rune_knife", 5, 5, 64),
                d("blue_dragon_scale", 5, 5, 32), d("runite_ore", 1, 1, 64), d("frozen_tear", 14, 14, 32));
        // Brutal red dragon <- parsed:Brutal red dragon(289)
        p("brutal_red_dragon", rm("#herbs"), d("#gems", 1, 1, 43), d("#rare", 1, 1, 65),
                d("adamant_platelegs", 1, 1, 18), d("rune_longsword", 1, 1, 26), d("red_dhide_body", 1, 1, 64),
                d("rune_full_helm", 2, 2, 64), d("red_dhide_vambraces", 1, 1, 128), d("dragon_dagger", 1, 1, 128),
                d("dragon_longsword", 1, 1, 128), d("rune_platebody", 1, 1, 128), d("air_rune", 105, 105, 18),
                d("blood_rune", 12, 12, 18), d("adamant_dart", 20, 20, 26), d("rune_knife", 10, 10, 64),
                d("white_berries", 5, 5, 32), d("runite_ore", 2, 2, 64));
        // Brutal black dragon <- parsed:Brutal black dragon(318)
        p("brutal_black_dragon", rm("#herbs"), d("#gems", 1, 1, 43), d("#rare", 1, 1, 65),
                d("dragon_plateskirt", 1, 1, 512), d("uncut_dragonstone", 1, 1, 512), d("rune_platelegs", 1, 1, 18),
                d("rune_full_helm", 2, 2, 21), d("rune_dart", 20, 20, 26), d("rune_longsword", 1, 1, 26),
                d("black_dhide_body", 1, 1, 64), d("rune_knife", 25, 25, 64), d("black_dhide_vambraces", 1, 1, 128),
                d("dragon_longsword", 1, 1, 128), d("dragon_dagger", 1, 1, 128), d("blood_rune", 50, 50, 16),
                d("soul_rune", 50, 50, 16), d("lava_scale", 5, 5, 32), d("runite_ore", 3, 3, 64));
        // Lava dragon <- parsed:Lava dragon(252)
        p("lava_dragon", rm(), d("#gems", 1, 1, 26), d("#rare", 1, 1, 43), d("#herbs", 1, 1, 13),
                d("rune_dart", 12, 12, 21), d("rune_knife", 8, 8, 32), d("adamant_2h_sword", 1, 1, 64),
                d("adamant_platebody", 1, 1, 64), d("rune_axe", 1, 1, 64), d("rune_longsword", 1, 1, 64),
                d("rune_med_helm", 1, 1, 128), d("fire_rune", 75, 75, 18), d("blood_rune", 20, 20, 18),
                d("rune_bolts", 30, 30, 21), d("lava_rune", 15, 15, 32), d("lava_rune", 30, 30, 32),
                d("fire_talisman", 1, 1, 128), d("adamantite_bar", 2, 2, 26), d("onyx_bolt_tips", 12, 12, 26));
        // Bronze dragon <- parsed:Bronze dragon(131)
        p("bronze_dragon", rm("#herbs", "rune_full_helm", "adamant_platelegs"), d("#rare", 1, 1, 132),
                d("dragon_plateskirt", 1, 1, 2048), d("dragon_platelegs", 1, 1, 2048), d("adamant_dart", 16, 16, 18),
                d("rune_knife", 2, 2, 43), d("adamant_platebody", 1, 1, 128), d("rune_longsword", 1, 1, 128),
                d("fire_rune", 50, 50, 16), d("mithril_bolts", 2, 12, 21), d("blood_rune", 15, 15, 43),
                d("adamantite_bar", 1, 1, 43));
        // Iron dragon <- parsed:Iron dragon(189)
        p("iron_dragon", rm("#herbs", "rune_full_helm", "rune_dagger"), d("#gems", 1, 1, 43), d("#rare", 1, 1, 65),
                d("dragon_plateskirt", 1, 1, 1024), d("dragon_platelegs", 1, 1, 1024), d("rune_dart", 9, 9, 18),
                d("adamant_2h_sword", 1, 1, 32), d("rune_knife", 5, 5, 43), d("rune_med_helm", 1, 1, 128),
                d("blood_rune", 15, 15, 7), d("adamant_bolts", 2, 12, 21), d("soul_rune", 3, 3, 26),
                d("rune_limbs", 1, 1, 26), d("adamantite_bar", 2, 2, 43), d("super_strength(1)", 1, 1, 16));
        // Steel dragon <- parsed:Steel dragon(246)
        p("steel_dragon", rm("#herbs"), d("#rare", 1, 1, 32), d("dragon_plateskirt", 1, 1, 512),
                d("dragon_platelegs", 1, 1, 512), d("rune_dart", 12, 12, 18), d("rune_mace", 1, 1, 32),
                d("adamant_kiteshield", 1, 1, 64), d("rune_knife", 7, 7, 43), d("rune_axe", 1, 1, 64),
                d("rune_full_helm", 1, 1, 128), d("blood_rune", 20, 20, 7), d("rune_bolts", 2, 12, 21),
                d("soul_rune", 5, 5, 26), d("rune_limbs", 1, 1, 16), d("super_attack(3)", 1, 1, 10), d("super_defence(2)", 1, 1, 43));
        // Mithril dragon <- parsed:Mithril dragon(304)
        p("mithril_dragon", rm("#herbs"), d("#gems", 1, 1, 32), d("#rare", 1, 1, 132), d("rune_dart", 14, 14, 18),
                d("rune_knife", 8, 8, 43), d("rune_mace", 1, 1, 43), d("rune_full_helm", 1, 1, 128),
                d("blood_rune", 27, 27, 7), d("rune_bolts", 10, 21, 21), d("soul_rune", 10, 10, 26));
        // Adamant dragon <- parsed:Adamant dragon(338)
        p("adamant_dragon", rm("#herbs", "#gems", "rune_platelegs"), d("#rare", 1, 1, 110),
                d("adamantite_bar", 2, 2, 1), d("adamantite_bar", 5, 35, 28), d("adamant_platebody", 1, 1, 12),
                d("rune_mace", 1, 1, 16), d("rune_scimitar", 1, 1, 16), d("dragon_platelegs", 1, 1, 110),
                d("dragon_plateskirt", 1, 1, 110), d("adamant_arrow", 30, 40, 14), d("wrath_rune", 10, 30, 14),
                d("chaos_rune", 60, 120, 16), d("adamant_bolts", 20, 40, 10), d("diamond", 1, 3, 16),
                d("adamantite_ore", 8, 20, 18), d("wrath_talisman", 1, 1, 110), d("dragon_metal_slice", 1, 1, 5000));
        // Rune dragon <- parsed:Rune dragon(380)
        p("rune_dragon", rm("#herbs", "#gems"), d("#rare", 1, 1, 127), d("rune_platebody", 1, 1, 14),
                d("rune_longsword", 1, 1, 16), d("rune_mace", 1, 1, 18), d("rune_scimitar", 1, 1, 18),
                d("rune_warhammer", 1, 1, 18), d("rune_platelegs", 1, 1, 21), d("dragon_platelegs", 1, 1, 127),
                d("dragon_plateskirt", 1, 1, 127), d("wrath_rune", 30, 50, 16), d("chaos_rune", 75, 150, 18),
                d("runite_ore", 2, 5, 21), d("wrath_talisman", 1, 1, 127));
        // Sourhog <- parsed:Sourhog(37)
        p("sourhog", rm("bones"), d("#gems", 1, 1, 22), d("#rare", 1, 1, 114), d("#herbs", 1, 1, 11),
                d("big_bones", 1, 1, 1), d("adamant_kiteshield", 1, 1, 110), d("mind_rune", 3, 10, 18),
                d("air_rune", 5, 15, 18), d("earth_rune", 5, 15, 18), d("chaos_rune", 1, 5, 22),
                d("limpwurt_root", 1, 1, 22));
        // Crocodile <- parsed:Crocodile(63)
        p("crocodile", rm("#gems"), d("#herbs", 1, 1, 20));
        // Shadow warrior <- parsed:Shadow warrior(48)
        p("shadow_warrior", rm("black_full_helm", "black_kiteshield"), d("#gems", 1, 1, 16), d("#herbs", 1, 1, 7),
                d("black_longsword", 1, 1, 128), d("cosmic_rune", 3, 3, 14), d("blood_rune", 2, 2, 21),
                d("air_rune", 45, 45, 32), d("death_rune", 2, 2, 32), d("coins", 8, 8, 3), d("mithril_bar", 1, 1, 32), d("adamant_spear", 1, 1, 128), d("black_knife", 1, 1, 128));
        // Otherworldly being <- parsed:Otherworldly being(64)
        p("otherworldly_being", rm("#seeds"), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 13), d("nature_rune", 5, 5, 14),
                d("chaos_rune", 4, 4, 16), d("law_rune", 2, 2, 18), d("cosmic_rune", 2, 2, 26),
                d("death_rune", 2, 2, 32), d("blood_rune", 2, 2, 128), d("coins", 15, 15, 2), d("ruby_ring", 1, 1, 64));
        // Mammoth <- parsed:Mammoth(80)
        p("mammoth", rm("#seeds"), d("#gems", 1, 1, 43), d("#rare", 1, 1, 65), d("#herbs", 1, 1, 16),
                d("#rare_seeds", 1, 1, 4), d("#allotment_seeds", 1, 1, 8), d("redberry_seed", 2, 2, 21),
                d("cadavaberry_seed", 2, 2, 32), d("dwellberry_seed", 2, 2, 40), d("apple_seed", 1, 1, 36),
                d("banana_seed", 1, 1, 53), d("orange_seed", 1, 1, 64), d("curry_seed", 1, 1, 107),
                d("pineapple_seed", 1, 1, 213), d("prayer_potion(1)", 1, 1, 26));
        // Bandit <- parsed:Bandit(22)
        p("bandit", rm(), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 3), d("chaos_rune", 6, 6, 43),
                d("water_rune", 9, 9, 43), d("air_rune", 10, 10, 64), d("death_rune", 2, 2, 64),
                d("law_rune", 3, 3, 64), d("blood_rune", 2, 2, 128), d("mind_rune", 2, 2, 128),
                d("nature_rune", 2, 2, 128), d("coins", 10, 10, 3), d("coins", 35, 35, 5), d("coins", 12, 12, 10),
                d("coins", 53, 53, 13), d("coins", 1, 1, 18), d("coins", 80, 80, 64), d("coal", 1, 1, 21));
        // Bandit <- parsed:Bandit(130)
        p("wilderness_bandit", rm("adamant_platebody"), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 3),
                d("chaos_rune", 6, 6, 43), d("water_rune", 9, 9, 43), d("air_rune", 10, 10, 64),
                d("death_rune", 2, 2, 64), d("law_rune", 3, 3, 64), d("blood_rune", 2, 2, 128),
                d("mind_rune", 2, 2, 128), d("nature_rune", 2, 2, 128), d("coins", 10, 10, 3), d("coins", 35, 35, 5),
                d("coins", 12, 12, 10), d("coins", 53, 53, 13), d("coins", 1, 1, 18), d("coins", 80, 80, 64),
                d("coal", 1, 1, 21));
        // Crushing hand <- parsed:Crushing hand(45)
        p("crushing_hand", rm("leather"), d("#gems", 1, 1, 21), d("gold_ring", 1, 1, 43), d("sapphire_ring", 1, 1, 64),
                d("emerald_ring", 1, 1, 64), d("coins", 8, 8, 6), d("coins", 5, 5, 6));
        // Chasm Crawler <- parsed:Chasm Crawler(68)
        p("chasm_crawler", rm(), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 2), d("nature_rune", 3, 4, 21),
                d("fire_rune", 12, 12, 26), d("earth_rune", 9, 9, 64), d("coins", 3, 3, 26), d("coins", 8, 8, 43),
                d("coins", 29, 29, 43), d("coins", 10, 10, 128), d("white_berries", 1, 1, 26),
                d("unicorn_horn_dust", 1, 1, 64), d("red_spiders_eggs", 1, 1, 128), d("limpwurt_root", 1, 1, 128),
                d("snape_grass", 1, 1, 128));
        // Screaming banshee <- parsed:Screaming banshee(70)
        p("screaming_banshee", rm(), d("#gems", 1, 1, 21), d("#herbs", 1, 1, 1), d("air_rune", 3, 3, 43),
                d("cosmic_rune", 2, 2, 43), d("chaos_rune", 3, 3, 64), d("chaos_rune", 7, 7, 128),
                d("fire_rune", 7, 7, 128), d("coins", 13, 13, 13), d("coins", 26, 26, 16), d("coins", 35, 35, 16),
                d("pure_essence", 13, 13, 6), d("iron_ore", 1, 1, 128));
        // Giant rockslug <- parsed:Giant rockslug(86)
        p("giant_rockslug", rm("#herbs"), d("#gems", 1, 1, 7), d("earth_rune", 5, 5, 4), d("earth_rune", 42, 42, 32),
                d("chaos_rune", 2, 2, 32), d("coal", 1, 1, 10), d("iron_ore", 1, 1, 6), d("bronze_bar", 1, 1, 64),
                d("iron_bar", 1, 1, 43), d("copper_ore", 1, 1, 43), d("tin_ore", 1, 1, 16),
                d("mithril_ore", 1, 1, 128));
        // Cockathrice <- parsed:Cockathrice(89)
        p("cockathrice", rm(), d("#gems", 1, 1, 21), d("iron_javelin", 5, 5, 128), d("nature_rune", 2, 2, 21),
                d("nature_rune", 4, 4, 32), d("nature_rune", 6, 6, 64), d("law_rune", 2, 2, 43),
                d("water_rune", 2, 2, 64), d("fire_rune", 7, 7, 64), d("coins", 15, 15, 8), d("coins", 5, 5, 11),
                d("coins", 28, 28, 11), d("coins", 62, 62, 32), d("coins", 42, 42, 43), d("coins", 1, 1, 128),
                d("limpwurt_root", 1, 1, 6));
        // Flaming pyrelord <- parsed:Flaming pyrelord(97)
        p("flaming_pyrelord", rm("#herbs"), d("#gems", 1, 1, 14), d("fiendish_ashes", 1, 1, 1),
                d("steel_full_helm", 1, 1, 32), d("fire_rune", 30, 30, 6), d("fire_rune", 60, 60, 16),
                d("chaos_rune", 12, 12, 26), d("death_rune", 3, 3, 43), d("coins", 40, 40, 5), d("coins", 120, 120, 6),
                d("coins", 200, 200, 13), d("coins", 10, 10, 18), d("coins", 450, 450, 64), d("gold_ore", 1, 1, 16));
        // Monstrous basilisk <- parsed:Monstrous basilisk(135)
        p("monstrous_basilisk", rm("iron_ore", "mithril_ore"), d("#gems", 1, 1, 9), d("#herbs", 1, 1, 1),
                d("rune_dagger", 1, 1, 128), d("water_rune", 75, 75, 16), d("nature_rune", 15, 15, 26),
                d("nature_rune", 37, 37, 128), d("law_rune", 3, 3, 43), d("adamantite_ore", 1, 1, 43),
                d("coins", 44, 44, 4), d("coins", 200, 200, 8), d("coins", 132, 132, 26), d("coins", 11, 11, 26),
                d("coins", 440, 440, 128));
        // Malevolent Mage <- parsed:Malevolent Mage(162)
        p("malevolent_mage", rm("#herbs", "chaos_rune"), d("earth_rune", 10, 10, 21), d("earth_rune", 36, 36, 32),
                d("earth_rune", 18, 18, 64), d("fire_rune", 10, 10, 21), d("fire_rune", 18, 18, 64),
                d("air_rune", 10, 10, 43), d("air_rune", 18, 18, 64), d("water_rune", 10, 10, 43),
                d("water_rune", 18, 18, 64), d("mind_rune", 18, 18, 64), d("body_rune", 18, 18, 64),
                d("blood_rune", 4, 4, 64), d("death_rune", 7, 7, 7), d("coins", 1, 1, 7), d("coins", 2, 2, 9),
                d("coins", 4, 4, 16), d("coins", 29, 29, 43));
    }

    private static void rows4() {
        // Insatiable Bloodveld <- parsed:Insatiable Bloodveld(202)
        p("insatiable_bloodveld", rm(), d("#gems", 1, 1, 11), d("#herbs", 1, 1, 43), d("steel_full_helm", 1, 1, 32),
                d("rune_med_helm", 1, 1, 128), d("fire_rune", 60, 60, 16), d("blood_rune", 10, 10, 26),
                d("blood_rune", 3, 3, 43), d("blood_rune", 30, 30, 128), d("coins", 120, 120, 4),
                d("coins", 40, 40, 4), d("coins", 200, 200, 13), d("coins", 10, 10, 18), d("coins", 450, 450, 128),
                d("bones", 1, 1, 13), d("big_bones", 1, 1, 18), d("big_bones", 3, 3, 43), d("gold_ore", 1, 1, 64));
        // Insatiable mutated Bloodveld <- parsed:Insatiable mutated Bloodveld(278)
        p("insatiable_mutated_bloodveld", rm("#herbs", "rune_full_helm"), d("#gems", 1, 1, 21), d("#rare", 1, 1, 14),
                d("mithril_platebody", 1, 1, 64), d("adamant_knife", 2, 2, 43), d("rune_med_helm", 1, 1, 64),
                d("rune_dagger", 1, 1, 128), d("air_rune", 105, 105, 18), d("fire_rune", 75, 75, 14),
                d("blood_rune", 30, 30, 10), d("blood_rune", 7, 7, 13), d("soul_rune", 4, 4, 18),
                d("gold_ore", 1, 1, 18), d("mithril_bar", 1, 1, 26), d("ruby_amulet", 1, 1, 64));
        // Vitreous Jelly <- parsed:Vitreous Jelly(206)
        p("vitreous_jelly", rm("#herbs"), d("#gems", 1, 1, 11), d("rune_full_helm", 1, 1, 128),
                d("coins", 102, 102, 3), d("coins", 44, 44, 4), d("coins", 220, 220, 13), d("coins", 11, 11, 18),
                d("coins", 460, 460, 64), d("chaos_rune", 15, 15, 26), d("death_rune", 5, 5, 43),
                d("gold_bar", 1, 1, 64));
        // Spiked Turoth <- parsed:Spiked Turoth(244)
        p("spiked_turoth", rm("#seeds"), d("#gems", 1, 1, 9), d("#herbs", 1, 1, 1), d("#uncommon_seeds", 1, 1, 2),
                d("bones", 1, 1, 1), d("rune_dagger", 1, 1, 128), d("leaf_bladed_sword", 1, 1, 500),
                d("law_rune", 3, 3, 21), d("nature_rune", 15, 15, 26), d("nature_rune", 37, 37, 128),
                d("limpwurt_root", 1, 1, 18), d("coins", 44, 44, 4), d("coins", 132, 132, 11),
                d("coins", 440, 440, 128));
        // Cave abomination <- parsed:Cave abomination(206)
        p("cave_abomination", rm("#seeds"), d("#gems", 1, 1, 9), d("#herbs", 1, 1, 3), d("#uncommon_seeds", 1, 1, 2),
                d("rune_dagger", 1, 1, 128), d("nature_rune", 6, 6, 21), d("nature_rune", 4, 4, 26),
                d("nature_rune", 3, 3, 128), d("coins", 44, 44, 5), d("coins", 132, 132, 11),
                d("coins", 440, 440, 128), d("limpwurt_root", 1, 1, 18), d("teak_logs", 4, 4, 18),
                d("mahogany_logs", 2, 2, 43));
        // Abhorrent spectre <- parsed:Abhorrent spectre(253)
        p("abhorrent_spectre", rm("#seeds", "steel_longsword", "lantadyme"), d("#gems", 1, 1, 9), d("#herbs", 1, 1, 1),
                d("#uncommon_seeds", 1, 1, 2), d("adamant_platelegs", 1, 1, 128), d("rune_full_helm", 1, 1, 128),
                d("coins", 460, 460, 128));
        // Choke devil <- parsed:Choke devil(264)
        p("choke_devil", rm("adamant_dagger"), d("#gems", 1, 1, 5), d("#herbs", 1, 1, 5), d("bones", 1, 1, 1),
                d("rune_dagger", 1, 1, 64), d("red_dhide_vambraces", 1, 1, 64), d("black_dhide_vambraces", 1, 1, 128),
                d("air_battlestaff", 1, 1, 64), d("earth_battlestaff", 1, 1, 64), d("dragon_dagger", 1, 1, 128),
                d("dust_rune", 200, 200, 13), d("earth_rune", 300, 300, 13), d("fire_rune", 300, 300, 13),
                d("fire_rune", 50, 50, 128), d("chaos_rune", 80, 80, 18), d("rune_arrow", 12, 12, 26),
                d("soul_rune", 20, 20, 32), d("soul_rune", 50, 50, 128), d("coins", 2000, 4000, 4),
                d("mithril_bar", 10, 10, 43), d("adamantite_bar", 4, 4, 128));
        // Shadow Wyrm <- parsed:Shadow Wyrm(259)
        p("shadow_wyrm", rm("death_rune", "chaos_rune"), d("#gems", 1, 1, 25), d("#herbs", 1, 1, 6),
                d("#uncommon_seeds", 1, 1, 8), d("dragon_sword", 1, 1, 2000), d("dragon_harpoon", 1, 1, 2000),
                d("dragon_thrownaxe", 75, 150, 2000), d("red_dhide_chaps", 1, 1, 25), d("adamant_2h_sword", 1, 1, 38),
                d("earth_battlestaff", 1, 1, 76), d("rune_med_helm", 1, 1, 38), d("rune_battleaxe", 1, 1, 76),
                d("dragon_dagger", 1, 1, 76), d("fire_rune", 200, 200, 8), d("earth_rune", 75, 150, 8),
                d("soul_rune", 15, 20, 15), d("blood_rune", 25, 30, 15), d("coins", 950, 1450, 10),
                d("pure_essence", 200, 300, 25));
        // King kurask <- parsed:King kurask(295)
        p("king_kurask", rm("#seeds", "#rare"), d("#gems", 1, 1, 7), d("#herbs", 1, 1, 2),
                d("#uncommon_seeds", 1, 1, 3), d("bones", 1, 1, 1), d("rune_longsword", 1, 1, 41),
                d("adamant_platebody", 1, 1, 41), d("rune_axe", 1, 1, 41), d("nature_rune", 10, 10, 12),
                d("nature_rune", 15, 15, 18), d("nature_rune", 30, 30, 31), d("coins", 2000, 3000, 8),
                d("coins", 10000, 10000, 25), d("white_berries", 12, 12, 21), d("big_bones", 20, 20, 25));
        // Marble gargoyle <- parsed:Marble gargoyle(349)
        p("marble_gargoyle", rm("#rare"), d("#gems", 1, 1, 9), d("granite_maul", 1, 1, 256),
                d("adamant_platelegs", 1, 1, 32), d("rune_full_helm", 1, 1, 43), d("rune_2h_sword", 1, 1, 64),
                d("rune_battleaxe", 1, 1, 128), d("rune_platelegs", 1, 1, 128), d("fire_rune", 75, 75, 13),
                d("fire_rune", 150, 150, 21), d("chaos_rune", 30, 30, 16), d("death_rune", 15, 15, 26),
                d("gold_ore", 10, 20, 13), d("pure_essence", 150, 150, 21), d("steel_bar", 15, 15, 21),
                d("gold_bar", 10, 15, 43), d("mithril_bar", 15, 15, 64), d("runite_ore", 1, 1, 64),
                d("coins", 400, 800, 5), d("coins", 500, 1000, 6), d("coins", 10000, 10000, 26));
        // Nechryarch <- parsed:Nechryarch(300)
        p("nechryarch", rm(), d("#gems", 1, 1, 8), d("#rare", 1, 1, 40), d("#herbs", 1, 1, 6),
                d("#uncommon_seeds", 1, 1, 2), d("adamant_platelegs", 1, 1, 29), d("adamant_kiteshield", 1, 1, 58),
                d("chaos_rune", 37, 37, 14), d("blood_rune", 15, 20, 29));
        // Greater abyssal demon <- parsed:Greater abyssal demon(342)
        p("greater_abyssal_demon", rm("rune_full_helm"), d("#gems", 1, 1, 9), d("#rare", 1, 1, 22),
                d("#herbs", 1, 1, 2), d("rune_chainbody", 1, 1, 128), d("rune_med_helm", 1, 1, 128),
                d("air_rune", 50, 50, 16), d("chaos_rune", 10, 10, 21), d("pure_essence", 60, 60, 26),
                d("blood_rune", 7, 7, 32), d("adamantite_bar", 1, 1, 64), d("cosmic_talisman", 1, 1, 128),
                d("defence_potion(3)", 1, 1, 128));
        // Night beast <- parsed:Night beast(374)
        p("night_beast", rm(), d("#gems", 1, 1, 14), d("#rare", 1, 1, 14), d("#herbs", 1, 1, 2),
                d("#uncommon_seeds", 1, 1, 11), d("rune_chainbody", 1, 1, 128), d("rune_med_helm", 1, 1, 128),
                d("rune_full_helm", 1, 1, 128), d("chaos_rune", 30, 30, 18), d("blood_rune", 15, 15, 32),
                d("adamantite_bar", 3, 3, 64), d("adamantite_ore", 5, 5, 128), d("runite_ore", 1, 1, 128),
                d("death_talisman", 1, 1, 128));
        // Guardian Drake <- parsed:Guardian Drake(376)
        p("guardian_drake", rm("runite_ore"), d("#gems", 1, 1, 31), d("#herbs", 1, 1, 3),
                d("#uncommon_seeds", 1, 1, 31), d("dragon_knife", 100, 200, 2000), d("rune_full_helm", 1, 1, 31),
                d("red_dhide_body", 1, 1, 46), d("black_dhide_vambraces", 1, 1, 93), d("dragon_mace", 1, 1, 93),
                d("fire_rune", 100, 200, 9), d("nature_rune", 30, 60, 9), d("law_rune", 25, 50, 9),
                d("death_rune", 20, 40, 9), d("rune_arrow", 35, 65, 9), d("raw_shark", 4, 6, 62),
                d("raw_anglerfish", 4, 6, 93), d("coins", 1000, 2000, 23), d("coins", 5000, 7000, 93),
                d("diamond", 3, 6, 23));
        // Nuclear smoke devil <- parsed:Nuclear smoke devil(280)
        p("nuclear_smoke_devil", rm(), d("#gems", 1, 1, 11), d("#rare", 1, 1, 11), d("#herbs", 1, 1, 2),
                d("rune_dagger", 1, 1, 43), d("air_battlestaff", 1, 1, 43), d("black_dhide_vambraces", 1, 1, 43),
                d("fire_battlestaff", 1, 1, 43), d("rune_full_helm", 1, 1, 64), d("red_dhide_body", 1, 1, 128),
                d("smoke_rune", 15, 15, 12), d("smoke_rune", 40, 40, 26), d("rune_bolts", 15, 15, 26),
                d("fire_rune", 37, 37, 32), d("fire_rune", 150, 150, 64), d("air_rune", 37, 37, 32),
                d("soul_rune", 10, 10, 32), d("steel_bar", 2, 2, 43), d("magic_logs", 5, 5, 43), d("coal", 15, 15, 43),
                d("adamantite_bar", 1, 1, 64));
        // Colossal Hydra <- parsed:Colossal Hydra(309)
        p("colossal_hydra", rm("runite_bar"), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 11),
                d("#uncommon_seeds", 1, 1, 11), d("hydra_tail", 1, 1, 1001), d("dragon_knife", 200, 400, 2001),
                d("black_dhide_chaps", 1, 1, 43), d("battlestaff", 2, 3, 64), d("fire_battlestaff", 1, 1, 26),
                d("water_battlestaff", 1, 1, 26), d("rune_kiteshield", 1, 1, 43), d("rune_platebody", 1, 1, 128),
                d("dragon_longsword", 1, 1, 128), d("blood_rune", 15, 45, 14), d("chaos_rune", 20, 50, 14),
                d("death_rune", 30, 60, 14), d("fire_rune", 70, 90, 14), d("law_rune", 30, 60, 14),
                d("water_rune", 70, 90, 14), d("coins", 500, 3500, 8), d("super_combat_potion(1)", 1, 1, 32),
                d("dragon_bones", 3, 5, 43), d("super_restore(1)", 1, 2, 21), d("hydras_eye", 1, 1, 361),
                d("hydras_fang", 1, 1, 361), d("hydras_heart", 1, 1, 361));
        // Basilisk Sentinel <- parsed:Basilisk Sentinel(358)
        p("basilisk_sentinel", rm("#herbs", "rune_full_helm", "runite_bar"), d("#gems", 1, 1, 2),
                d("adamant_platelegs", 1, 1, 52), d("adamant_kiteshield", 1, 1, 52), d("rune_axe", 1, 1, 26),
                d("rune_battleaxe", 1, 1, 52), d("rune_dagger", 1, 1, 52), d("rune_scimitar", 1, 1, 52),
                d("rune_med_helm", 1, 1, 52), d("astral_rune", 15, 35, 9), d("nature_rune", 15, 30, 9),
                d("law_rune", 20, 30, 9), d("death_rune", 10, 25, 17), d("blood_rune", 8, 20, 17),
                d("coins", 500, 2498, 7), d("adamantite_ore", 1, 2, 52));
        // Aviansie <- parsed:Aviansie(79)
        p("gwd_aviansie", rm("rune_arrow", "coins"), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 9),
                d("rune_dagger", 1, 1, 12), d("air_rune", 15, 15, 7), d("water_rune", 30, 30, 10),
                d("law_rune", 2, 2, 32), d("nature_rune", 9, 9, 32), d("chaos_rune", 3, 3, 43),
                d("chaos_rune", 16, 16, 128), d("body_rune", 12, 12, 64), d("blood_rune", 11, 11, 64),
                d("mind_rune", 5, 5, 128), d("adamantite_bar", 4, 4, 4), d("silver_ore", 1, 1, 13),
                d("rune_limbs", 1, 1, 128), d("antipoison(3)", 5, 5, 26));
        // Aviansie <- parsed:Aviansie(131)
        p("gwd_aviansie_elite", rm("runite_bar", "coins"), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 9),
                d("rune_dagger", 1, 1, 12), d("air_rune", 15, 15, 7), d("water_rune", 30, 30, 10),
                d("law_rune", 2, 2, 32), d("nature_rune", 9, 9, 32), d("chaos_rune", 3, 3, 43),
                d("chaos_rune", 16, 16, 128), d("body_rune", 12, 12, 64), d("blood_rune", 11, 11, 64),
                d("mind_rune", 5, 5, 128), d("adamantite_bar", 4, 4, 4), d("silver_ore", 1, 1, 13),
                d("rune_limbs", 1, 1, 128), d("antipoison(3)", 5, 5, 26));
        // Spiritual warrior (Armadyl) <- parsed:Spiritual warrior(123)
        p("gwd_spiritual_warrior_armadyl", rm("#herbs", "rune_full_helm", "adamant_platebody"), d("#gems", 1, 1, 32),
                d("adamant_2h_sword", 1, 1, 127), d("rune_longsword", 1, 1, 127), d("rune_kiteshield", 1, 1, 127),
                d("air_rune", 150, 150, 64), d("chaos_rune", 1, 1, 43), d("mind_rune", 1, 1, 18),
                d("mud_rune", 15, 15, 18), d("nature_rune", 1, 1, 32), d("coins", 400, 499, 14),
                d("coins", 1300, 1337, 21), d("coins", 1, 1, 64), d("prayer_potion(2)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("pure_essence", 23, 23, 16), d("rune_halberd", 1, 1, 127), d("super_attack(1)", 1, 1, 12), d("super_defence(1)", 1, 1, 64), d("super_strength(1)", 1, 1, 64));
        // Spiritual ranger (Armadyl) <- parsed:Spiritual ranger(127)
        p("gwd_spiritual_ranger_armadyl", rm("#herbs"), d("#gems", 1, 1, 128), d("magic_shortbow", 1, 2, 128),
                d("mithril_arrow", 1, 1, 12), d("steel_arrow", 12, 12, 32), d("iron_arrow", 12, 12, 9),
                d("bronze_arrow", 16, 16, 10), d("bronze_arrow", 1, 1, 128), d("body_rune", 12, 12, 32),
                d("rune_arrow", 5, 5, 64), d("rune_arrow", 12, 12, 64), d("rune_arrow", 50, 50, 64),
                d("adamant_arrow", 3, 3, 43), d("adamant_arrow", 12, 12, 128), d("headless_arrow", 12, 12, 13),
                d("adamant_bolts", 15, 15, 18), d("chaos_rune", 1, 1, 43), d("nature_rune", 1, 1, 32),
                d("coins", 500, 500, 14), d("coins", 1300, 1337, 21), d("coins", 1, 1, 64),
                d("prayer_potion(2)", 1, 1, 14), d("ranging_potion(2)", 1, 1, 12), d("adamantite_bar", 1, 4, 16),
                d("blue_dragon_scale", 2, 2, 18), d("coal", 1, 10, 16), d("green_dhide_body", 1, 1, 18),
                d("pure_essence", 23, 23, 16), d("super_defence(1)", 1, 1, 64));
        // Spiritual mage (Armadyl) <- parsed:Spiritual mage(123)
        p("gwd_spiritual_mage_armadyl", rm("#herbs"), d("#gems", 1, 1, 64), d("air_rune", 30, 30, 14),
                d("air_rune", 250, 250, 18), d("air_rune", 50, 50, 64), d("water_rune", 15, 15, 16),
                d("water_rune", 50, 50, 18), d("fire_rune", 65, 65, 64), d("dust_rune", 15, 15, 12),
                d("mud_rune", 20, 20, 18), d("mud_rune", 40, 70, 18), d("mist_rune", 20, 20, 21),
                d("chaos_rune", 10, 10, 7), d("chaos_rune", 25, 25, 128), d("astral_rune", 38, 98, 9),
                d("astral_rune", 15, 15, 14), d("death_rune", 15, 15, 16), d("death_rune", 25, 25, 32),
                d("nature_rune", 25, 25, 18), d("nature_rune", 18, 18, 32), d("mind_rune", 50, 50, 32),
                d("body_rune", 35, 35, 43), d("body_rune", 25, 25, 64), d("law_rune", 15, 15, 64),
                d("blood_rune", 10, 10, 64), d("blood_rune", 25, 25, 64), d("pure_essence", 46, 46, 16),
                d("pure_essence", 90, 90, 21), d("dragon_boots", 1, 1, 64), d("lava_rune", 30, 60, 14),
                d("smoke_rune", 100, 150, 64), d("soul_rune", 25, 25, 43), d("grimy_avantoe", 1, 1, 41),
                d("grimy_ranarr_weed", 1, 1, 51), d("grimy_snapdragon", 1, 1, 51), d("grimy_torstol", 1, 1, 14),
                d("coins", 1300, 1337, 21), d("coins", 6900, 6942, 64), d("super_restore(3)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("antipoison(3)", 5, 5, 26), d("super_defence(3)", 1, 1, 64), d("ancient_brew(2)", 1, 1, 12), d("ancient_brew(3)", 1, 1, 32));
        // Hobgoblin <- parsed:Hobgoblin(47)+rebox seeds+rebox seeds
        p("gwd_hobgoblin", rm(), d("#gems", 1, 1, 64), d("#herbs", 1, 1, 18), d("#rare_seeds", 1, 1, 11),
                d("#allotment_seeds", 1, 1, 7), d("law_rune", 2, 2, 43), d("water_rune", 2, 2, 64),
                d("fire_rune", 7, 7, 64), d("body_rune", 6, 6, 64), d("chaos_rune", 3, 3, 64),
                d("nature_rune", 4, 4, 64), d("cosmic_rune", 2, 2, 128), d("coins", 5, 5, 4), d("coins", 15, 15, 8),
                d("coins", 28, 28, 8), d("coins", 10, 10, 18), d("coins", 62, 62, 32), d("coins", 1, 42, 32),
                d("limpwurt_root", 1, 1, 6), d("bronze_spear", 1, 1, 43), d("iron_spear", 1, 1, 64), d("steel_spear", 1, 1, 64));
        // Ork <- parsed:Ork(107)
        p("gwd_ork", rm("#herbs", "big_bones", "rune_full_helm"), d("bones", 1, 1, 1), d("bronze_dart", 2, 2, 26),
                d("iron_dart", 2, 2, 26), d("coins", 10, 10, 16), d("coins", 5, 5, 26), d("coins", 8, 8, 26),
                d("coins", 12, 12, 26), d("coins", 23, 23, 26), d("iron_spear", 1, 1, 32), d("bronze_halberd", 1, 1, 26));
        // Ogre <- parsed:Ogre(58)+rebox seeds
        p("gwd_ogre", rm("#herbs"), d("#rare_seeds", 1, 1, 7));
    }

    private static void rows5() {
        // Jogre <- parsed:Jogre(58)+rebox seeds
        p("gwd_jogre", rm("coins"), d("#gems", 1, 1, 129), d("#herbs", 1, 1, 22), d("#rare_seeds", 1, 1, 9),
                d("nature_rune", 2, 2, 13), d("nature_rune", 10, 10, 64), d("nature_rune", 5, 5, 64),
                d("steel_javelin", 5, 5, 64), d("bones", 1, 1, 26), d("big_bones", 1, 1, 43), d("big_bones", 3, 3, 64), d("bronze_spear", 1, 1, 4), d("iron_spear", 1, 1, 32));
        // Cyclops <- parsed:Cyclops(76)
        p("gwd_cyclops", rm(), d("#gems", 1, 1, 50), d("#rare", 1, 1, 50), d("#herbs", 1, 1, 33),
                d("adamant_2h_sword", 1, 1, 100), d("rune_defender", 1, 1, 50), d("black_knife", 4, 13, 6));
        // Spiritual warrior (Bandos) <- parsed:Spiritual warrior(134)
        p("gwd_spiritual_warrior_bandos", rm("#herbs", "rune_full_helm", "adamant_platebody"), d("#gems", 1, 1, 32),
                d("adamant_2h_sword", 1, 1, 127), d("rune_longsword", 1, 1, 127), d("rune_kiteshield", 1, 1, 127),
                d("air_rune", 150, 150, 64), d("chaos_rune", 1, 1, 43), d("mind_rune", 1, 1, 18),
                d("mud_rune", 15, 15, 18), d("nature_rune", 1, 1, 32), d("coins", 400, 499, 14),
                d("coins", 1300, 1337, 21), d("coins", 1, 1, 64), d("prayer_potion(2)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("pure_essence", 23, 23, 16), d("rune_halberd", 1, 1, 127), d("super_attack(1)", 1, 1, 12), d("super_defence(1)", 1, 1, 64), d("super_strength(1)", 1, 1, 64));
        // Spiritual ranger (Bandos) <- parsed:Spiritual ranger(115)
        p("gwd_spiritual_ranger_bandos", rm("#herbs"), d("#gems", 1, 1, 128), d("magic_shortbow", 1, 2, 128),
                d("mithril_arrow", 1, 1, 12), d("steel_arrow", 12, 12, 32), d("iron_arrow", 12, 12, 9),
                d("bronze_arrow", 16, 16, 10), d("bronze_arrow", 1, 1, 128), d("body_rune", 12, 12, 32),
                d("rune_arrow", 5, 5, 64), d("rune_arrow", 12, 12, 64), d("rune_arrow", 50, 50, 64),
                d("adamant_arrow", 3, 3, 43), d("adamant_arrow", 12, 12, 128), d("headless_arrow", 12, 12, 13),
                d("adamant_bolts", 15, 15, 18), d("chaos_rune", 1, 1, 43), d("nature_rune", 1, 1, 32),
                d("coins", 500, 500, 14), d("coins", 1300, 1337, 21), d("coins", 1, 1, 64),
                d("prayer_potion(2)", 1, 1, 14), d("ranging_potion(2)", 1, 1, 12), d("adamantite_bar", 1, 4, 16),
                d("blue_dragon_scale", 2, 2, 18), d("coal", 1, 10, 16), d("green_dhide_body", 1, 1, 18),
                d("pure_essence", 23, 23, 16), d("super_defence(1)", 1, 1, 64));
        // Spiritual mage (Bandos) <- parsed:Spiritual mage(121)
        p("gwd_spiritual_mage_bandos", rm("#herbs"), d("#gems", 1, 1, 64), d("air_rune", 30, 30, 14),
                d("air_rune", 250, 250, 18), d("air_rune", 50, 50, 64), d("water_rune", 15, 15, 16),
                d("water_rune", 50, 50, 18), d("fire_rune", 65, 65, 64), d("dust_rune", 15, 15, 12),
                d("mud_rune", 20, 20, 18), d("mud_rune", 40, 70, 18), d("mist_rune", 20, 20, 21),
                d("chaos_rune", 10, 10, 7), d("chaos_rune", 25, 25, 128), d("astral_rune", 38, 98, 9),
                d("astral_rune", 15, 15, 14), d("death_rune", 15, 15, 16), d("death_rune", 25, 25, 32),
                d("nature_rune", 25, 25, 18), d("nature_rune", 18, 18, 32), d("mind_rune", 50, 50, 32),
                d("body_rune", 35, 35, 43), d("body_rune", 25, 25, 64), d("law_rune", 15, 15, 64),
                d("blood_rune", 10, 10, 64), d("blood_rune", 25, 25, 64), d("pure_essence", 46, 46, 16),
                d("pure_essence", 90, 90, 21), d("dragon_boots", 1, 1, 64), d("lava_rune", 30, 60, 14),
                d("smoke_rune", 100, 150, 64), d("soul_rune", 25, 25, 43), d("grimy_avantoe", 1, 1, 41),
                d("grimy_ranarr_weed", 1, 1, 51), d("grimy_snapdragon", 1, 1, 51), d("grimy_torstol", 1, 1, 14),
                d("coins", 1300, 1337, 21), d("coins", 6900, 6942, 64), d("super_restore(3)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("antipoison(3)", 5, 5, 26), d("super_defence(3)", 1, 1, 64), d("ancient_brew(2)", 1, 1, 12), d("ancient_brew(3)", 1, 1, 32));
        // Spiritual warrior (Saradomin) <- parsed:Spiritual warrior(125)
        p("gwd_spiritual_warrior_saradomin", rm("#herbs", "rune_full_helm", "adamant_platebody"), d("#gems", 1, 1, 32),
                d("adamant_2h_sword", 1, 1, 127), d("rune_longsword", 1, 1, 127), d("rune_kiteshield", 1, 1, 127),
                d("air_rune", 150, 150, 64), d("chaos_rune", 1, 1, 43), d("mind_rune", 1, 1, 18),
                d("mud_rune", 15, 15, 18), d("nature_rune", 1, 1, 32), d("coins", 400, 499, 14),
                d("coins", 1300, 1337, 21), d("coins", 1, 1, 64), d("prayer_potion(2)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("pure_essence", 23, 23, 16), d("rune_halberd", 1, 1, 127), d("super_attack(1)", 1, 1, 12), d("super_defence(1)", 1, 1, 64), d("super_strength(1)", 1, 1, 64));
        // Spiritual ranger (Saradomin) <- parsed:Spiritual ranger(122)
        p("gwd_spiritual_ranger_saradomin", rm("#herbs"), d("#gems", 1, 1, 128), d("magic_shortbow", 1, 2, 128),
                d("mithril_arrow", 1, 1, 12), d("steel_arrow", 12, 12, 32), d("iron_arrow", 12, 12, 9),
                d("bronze_arrow", 16, 16, 10), d("bronze_arrow", 1, 1, 128), d("body_rune", 12, 12, 32),
                d("rune_arrow", 5, 5, 64), d("rune_arrow", 12, 12, 64), d("rune_arrow", 50, 50, 64),
                d("adamant_arrow", 3, 3, 43), d("adamant_arrow", 12, 12, 128), d("headless_arrow", 12, 12, 13),
                d("adamant_bolts", 15, 15, 18), d("chaos_rune", 1, 1, 43), d("nature_rune", 1, 1, 32),
                d("coins", 500, 500, 14), d("coins", 1300, 1337, 21), d("coins", 1, 1, 64),
                d("prayer_potion(2)", 1, 1, 14), d("ranging_potion(2)", 1, 1, 12), d("adamantite_bar", 1, 4, 16),
                d("blue_dragon_scale", 2, 2, 18), d("coal", 1, 10, 16), d("green_dhide_body", 1, 1, 18),
                d("pure_essence", 23, 23, 16), d("super_defence(1)", 1, 1, 64));
        // Spiritual mage (Saradomin) <- parsed:Spiritual mage(120)
        p("gwd_spiritual_mage_saradomin", rm("#herbs"), d("#gems", 1, 1, 64), d("air_rune", 30, 30, 14),
                d("air_rune", 250, 250, 18), d("air_rune", 50, 50, 64), d("water_rune", 15, 15, 16),
                d("water_rune", 50, 50, 18), d("fire_rune", 65, 65, 64), d("dust_rune", 15, 15, 12),
                d("mud_rune", 20, 20, 18), d("mud_rune", 40, 70, 18), d("mist_rune", 20, 20, 21),
                d("chaos_rune", 10, 10, 7), d("chaos_rune", 25, 25, 128), d("astral_rune", 38, 98, 9),
                d("astral_rune", 15, 15, 14), d("death_rune", 15, 15, 16), d("death_rune", 25, 25, 32),
                d("nature_rune", 25, 25, 18), d("nature_rune", 18, 18, 32), d("mind_rune", 50, 50, 32),
                d("body_rune", 35, 35, 43), d("body_rune", 25, 25, 64), d("law_rune", 15, 15, 64),
                d("blood_rune", 10, 10, 64), d("blood_rune", 25, 25, 64), d("pure_essence", 46, 46, 16),
                d("pure_essence", 90, 90, 21), d("dragon_boots", 1, 1, 64), d("lava_rune", 30, 60, 14),
                d("smoke_rune", 100, 150, 64), d("soul_rune", 25, 25, 43), d("grimy_avantoe", 1, 1, 41),
                d("grimy_ranarr_weed", 1, 1, 51), d("grimy_snapdragon", 1, 1, 51), d("grimy_torstol", 1, 1, 14),
                d("coins", 1300, 1337, 21), d("coins", 6900, 6942, 64), d("super_restore(3)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("antipoison(3)", 5, 5, 26), d("super_defence(3)", 1, 1, 64), d("ancient_brew(2)", 1, 1, 12), d("ancient_brew(3)", 1, 1, 32));
        // Imp <- parsed:Imp(7)
        p("gwd_imp", rm("#herbs", "fire_rune", "coins"), d("bronze_bolts", 1, 1, 16), d("mind_talisman", 1, 1, 18), d("blue_wizard_hat", 1, 1, 16));
        // Werewolf <- parsed:Werewolf(93)
        p("gwd_werewolf", rm(), d("#gems", 1, 1, 256), d("#herbs", 1, 1, 171), d("rune_med_helm", 1, 1, 171),
                d("coins", 10, 10, 6), d("coins", 90, 90, 26), d("coins", 120, 120, 26), d("coins", 222, 222, 26),
                d("coins", 364, 364, 26), d("grey_wolf_fur", 1, 1, 5));
        // Bloodveld <- parsed:Bloodveld(81)
        p("gwd_bloodveld", rm(), d("#gems", 1, 1, 32), d("#herbs", 1, 1, 128), d("rune_med_helm", 1, 1, 128),
                d("fire_rune", 60, 60, 16), d("blood_rune", 10, 10, 26), d("blood_rune", 3, 3, 43),
                d("blood_rune", 30, 30, 128), d("coins", 120, 120, 4), d("coins", 40, 40, 4), d("coins", 200, 200, 13),
                d("coins", 10, 10, 18), d("coins", 450, 450, 128), d("bones", 1, 1, 13), d("big_bones", 1, 1, 18),
                d("big_bones", 3, 3, 43), d("gold_ore", 1, 1, 64), d("black_boots", 1, 1, 128));
        // Hellhound <- parsed:Hellhound(127)
        p("gwd_hellhound", rm("#herbs", "coins"), d("death_rune", 10, 10, 8), d("death_rune", 20, 20, 17),
                d("death_rune", 50, 50, 50), d("smouldering_stone", 1, 1, 32768));
        // Gorak <- parsed:Gorak(149)
        p("gwd_gorak", rm("bones"), d("#gems", 1, 1, 1), d("#herbs", 1, 1, 8), d("big_bones", 1, 1, 1),
                d("iron_halberd", 1, 1, 43), d("steel_thrownaxe", 1, 1, 43), d("mithril_thrownaxe", 1, 1, 43));
        // Vampire <- parsed:Feral Vampyre(72)
        p("gwd_vampire", rm(), d("#gems", 1, 1, 32), d("#herbs", 1, 1, 13), d("earth_rune", 4, 4, 13),
                d("death_rune", 2, 2, 13), d("chaos_rune", 3, 3, 16), d("blood_rune", 1, 1, 26),
                d("blood_rune", 2, 2, 128), d("coins", 15, 15, 3), d("earth_talisman", 1, 1, 64));
        // Pyrefiend <- parsed:Pyrefiend(48)
        p("gwd_pyrefiend", rm("#herbs"), d("#gems", 1, 1, 43), d("fiendish_ashes", 1, 1, 1), d("fire_rune", 30, 30, 6),
                d("fire_rune", 60, 60, 16), d("chaos_rune", 12, 12, 26), d("death_rune", 3, 3, 43),
                d("coins", 40, 40, 5), d("coins", 120, 120, 6), d("coins", 200, 200, 13), d("coins", 10, 10, 18),
                d("coins", 450, 450, 64), d("gold_ore", 1, 1, 16), d("steel_boots", 1, 1, 128));
        // Spiritual warrior (Zamorak) <- parsed:Spiritual warrior(115)
        p("gwd_spiritual_warrior_zamorak", rm("#herbs", "rune_full_helm", "adamant_platebody"), d("#gems", 1, 1, 32),
                d("adamant_2h_sword", 1, 1, 127), d("rune_longsword", 1, 1, 127), d("rune_kiteshield", 1, 1, 127),
                d("air_rune", 150, 150, 64), d("chaos_rune", 1, 1, 43), d("mind_rune", 1, 1, 18),
                d("mud_rune", 15, 15, 18), d("nature_rune", 1, 1, 32), d("coins", 400, 499, 14),
                d("coins", 1300, 1337, 21), d("coins", 1, 1, 64), d("prayer_potion(2)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("pure_essence", 23, 23, 16), d("super_attack(1)", 1, 1, 12), d("super_defence(1)", 1, 1, 64), d("super_strength(1)", 1, 1, 64));
        // Spiritual ranger (Zamorak) <- parsed:Spiritual ranger(118)
        p("gwd_spiritual_ranger_zamorak", rm("#herbs"), d("#gems", 1, 1, 128), d("magic_shortbow", 1, 2, 128),
                d("mithril_arrow", 1, 1, 12), d("steel_arrow", 12, 12, 32), d("iron_arrow", 12, 12, 9),
                d("bronze_arrow", 16, 16, 10), d("bronze_arrow", 1, 1, 128), d("body_rune", 12, 12, 32),
                d("rune_arrow", 5, 5, 64), d("rune_arrow", 12, 12, 64), d("rune_arrow", 50, 50, 64),
                d("adamant_arrow", 3, 3, 43), d("adamant_arrow", 12, 12, 128), d("headless_arrow", 12, 12, 13),
                d("adamant_bolts", 15, 15, 18), d("chaos_rune", 1, 1, 43), d("nature_rune", 1, 1, 32),
                d("coins", 500, 500, 14), d("coins", 1300, 1337, 21), d("coins", 1, 1, 64),
                d("prayer_potion(2)", 1, 1, 14), d("ranging_potion(2)", 1, 1, 12), d("adamantite_bar", 1, 4, 16),
                d("blue_dragon_scale", 2, 2, 18), d("coal", 1, 10, 16), d("green_dhide_body", 1, 1, 18),
                d("pure_essence", 23, 23, 16), d("super_defence(1)", 1, 1, 64));
        // Spiritual mage (Zamorak) <- parsed:Spiritual mage(121)
        p("gwd_spiritual_mage_zamorak", rm("#herbs"), d("#gems", 1, 1, 64), d("air_rune", 30, 30, 14),
                d("air_rune", 250, 250, 18), d("air_rune", 50, 50, 64), d("water_rune", 15, 15, 16),
                d("water_rune", 50, 50, 18), d("fire_rune", 65, 65, 64), d("dust_rune", 15, 15, 12),
                d("mud_rune", 20, 20, 18), d("mud_rune", 40, 70, 18), d("mist_rune", 20, 20, 21),
                d("chaos_rune", 10, 10, 7), d("chaos_rune", 25, 25, 128), d("astral_rune", 38, 98, 9),
                d("astral_rune", 15, 15, 14), d("death_rune", 15, 15, 16), d("death_rune", 25, 25, 32),
                d("nature_rune", 25, 25, 18), d("nature_rune", 18, 18, 32), d("mind_rune", 50, 50, 32),
                d("body_rune", 35, 35, 43), d("body_rune", 25, 25, 64), d("law_rune", 15, 15, 64),
                d("blood_rune", 10, 10, 64), d("blood_rune", 25, 25, 64), d("pure_essence", 46, 46, 16),
                d("pure_essence", 90, 90, 21), d("dragon_boots", 1, 1, 64), d("lava_rune", 30, 60, 14),
                d("smoke_rune", 100, 150, 64), d("soul_rune", 25, 25, 43), d("grimy_avantoe", 1, 1, 41),
                d("grimy_ranarr_weed", 1, 1, 51), d("grimy_snapdragon", 1, 1, 51), d("grimy_torstol", 1, 1, 14),
                d("coins", 1300, 1337, 21), d("coins", 6900, 6942, 64), d("super_restore(3)", 1, 1, 14),
                d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16), d("antipoison(3)", 5, 5, 26), d("super_defence(3)", 1, 1, 64), d("ancient_brew(2)", 1, 1, 12), d("ancient_brew(3)", 1, 1, 32));
        // Flight Kilisa <- parsed:Flight Kilisa(159)
        p("flight_kilisa", rm("super_restore", "rune_arrow"), d("armadyl_helmet", 1, 1, 16129),
                d("armadyl_chestplate", 1, 1, 16129), d("armadyl_chainskirt", 1, 1, 16129),
                d("godsword_shard_1", 1, 1, 1524), d("godsword_shard_2", 1, 1, 1524),
                d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 91, 101, 18), d("steel_dart", 91, 101, 16),
                d("smoke_rune", 10, 15, 16), d("coins", 1000, 1100, 2), d("grimy_kwuarm", 1, 1, 16));
        // Wingman Skree <- parsed:Wingman Skree(143)
        p("wingman_skree", rm("super_restore", "rune_arrow"), d("armadyl_helmet", 1, 1, 16129),
                d("armadyl_chestplate", 1, 1, 16129), d("armadyl_chainskirt", 1, 1, 16129),
                d("godsword_shard_1", 1, 1, 1524), d("godsword_shard_2", 1, 1, 1524),
                d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 91, 101, 18), d("steel_dart", 91, 101, 16),
                d("smoke_rune", 10, 15, 16), d("coins", 1000, 1100, 2), d("grimy_kwuarm", 1, 1, 16));
        // Flockleader Geerin <- parsed:Flockleader Geerin(149)
        p("flockleader_geerin", rm("super_restore", "rune_arrow"), d("armadyl_helmet", 1, 1, 16129),
                d("armadyl_chestplate", 1, 1, 16129), d("armadyl_chainskirt", 1, 1, 16129),
                d("godsword_shard_1", 1, 1, 1524), d("godsword_shard_2", 1, 1, 1524),
                d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 91, 101, 18), d("steel_dart", 91, 101, 16),
                d("smoke_rune", 10, 15, 16), d("coins", 1000, 1100, 2), d("grimy_kwuarm", 1, 1, 16));
        // Starlight <- parsed:Starlight(149)
        p("starlight", rm("super_restore", "rune_arrow"), d("godsword_shard_1", 1, 1, 1524),
                d("godsword_shard_2", 1, 1, 1524), d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 16),
                d("steel_dart", 95, 100, 16), d("law_rune", 5, 10, 16), d("coins", 1300, 1400, 2),
                d("grimy_ranarr_weed", 1, 1, 16), d("snape_grass", 5, 5, 18));
        // Growler <- parsed:Growler(139)
        p("growler", rm("super_restore", "rune_arrow"), d("godsword_shard_1", 1, 1, 1524),
                d("godsword_shard_2", 1, 1, 1524), d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 16),
                d("steel_dart", 95, 100, 16), d("law_rune", 5, 10, 16), d("coins", 1300, 1400, 2),
                d("grimy_ranarr_weed", 1, 1, 16), d("snape_grass", 5, 5, 18));
        // Bree <- parsed:Bree(146)
        p("bree", rm("super_restore", "rune_arrow"), d("godsword_shard_1", 1, 1, 1524),
                d("godsword_shard_2", 1, 1, 1524), d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 16),
                d("steel_dart", 95, 100, 16), d("law_rune", 5, 10, 16), d("coins", 1300, 1400, 2),
                d("grimy_ranarr_weed", 1, 1, 16), d("snape_grass", 5, 5, 18));
        // Sergeant Strongstack <- parsed:Sergeant Strongstack(141)
        p("sergeant_strongstack", rm("super_restore", "rune_arrow"), d("bandos_chestplate", 1, 1, 16256),
                d("bandos_tassets", 1, 1, 16256), d("bandos_boots", 1, 1, 16256), d("godsword_shard_1", 1, 1, 1524),
                d("godsword_shard_2", 1, 1, 1524), d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 18),
                d("steel_dart", 95, 100, 16), d("nature_rune", 15, 20, 16), d("cosmic_rune", 25, 30, 16),
                d("coins", 1400, 1500, 2), d("limpwurt_root", 5, 5, 16), d("combat_potion(3)", 1, 1, 64), d("super_strength(3)", 1, 1, 64));
    }

    private static void rows6() {
        // Sergeant Steelwill <- parsed:Sergeant Steelwill(142)
        p("sergeant_steelwill", rm("super_restore", "rune_arrow"), d("bandos_chestplate", 1, 1, 16256),
                d("bandos_tassets", 1, 1, 16256), d("bandos_boots", 1, 1, 16256), d("godsword_shard_1", 1, 1, 1524),
                d("godsword_shard_2", 1, 1, 1524), d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 18),
                d("steel_dart", 95, 100, 16), d("nature_rune", 15, 20, 16), d("cosmic_rune", 25, 30, 16),
                d("coins", 1400, 1500, 2), d("limpwurt_root", 5, 5, 16), d("combat_potion(3)", 1, 1, 64), d("super_strength(3)", 1, 1, 64));
        // Sergeant Grimspike <- parsed:Sergeant Grimspike(142)
        p("sergeant_grimspike", rm("super_restore", "rune_arrow"), d("bandos_chestplate", 1, 1, 16256),
                d("bandos_tassets", 1, 1, 16256), d("bandos_boots", 1, 1, 16256), d("godsword_shard_1", 1, 1, 1524),
                d("godsword_shard_2", 1, 1, 1524), d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 18),
                d("steel_dart", 95, 100, 16), d("nature_rune", 15, 20, 16), d("cosmic_rune", 25, 30, 16),
                d("coins", 1400, 1500, 2), d("limpwurt_root", 5, 5, 16), d("combat_potion(3)", 1, 1, 64), d("super_strength(3)", 1, 1, 64));
        // Tstanon Karlak <- parsed:Tstanon Karlak(145)
        p("tstanon_karlak", rm("bones", "super_restore", "rune_arrow"), d("malicious_ashes", 1, 1, 1),
                d("godsword_shard_1", 1, 1, 1524), d("godsword_shard_2", 1, 1, 1524),
                d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 18), d("steel_dart", 95, 100, 16),
                d("death_rune", 5, 10, 16), d("blood_rune", 5, 10, 16), d("coins", 1300, 1400, 2),
                d("wine_of_zamorak", 5, 10, 16), d("super_attack(3)", 1, 1, 64), d("super_strength(3)", 1, 1, 64));
        // Zakl'n Gritch <- parsed:Zakl'n Gritch(142)
        p("zakln_gritch", rm("bones", "super_restore", "rune_arrow"), d("malicious_ashes", 1, 1, 1),
                d("godsword_shard_1", 1, 1, 1524), d("godsword_shard_2", 1, 1, 1524),
                d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 18), d("steel_dart", 95, 100, 16),
                d("death_rune", 5, 10, 16), d("blood_rune", 5, 10, 16), d("coins", 1300, 1400, 2),
                d("wine_of_zamorak", 5, 10, 16), d("super_attack(3)", 1, 1, 64), d("super_strength(3)", 1, 1, 64));
        // Balfrug Kreeyath <- parsed:Balfrug Kreeyath(151)
        p("balfrug_kreeyath", rm("bones", "super_restore", "rune_arrow"), d("malicious_ashes", 1, 1, 1),
                d("godsword_shard_1", 1, 1, 1524), d("godsword_shard_2", 1, 1, 1524),
                d("godsword_shard_3", 1, 1, 1524), d("steel_arrow", 95, 100, 18), d("steel_dart", 95, 100, 16),
                d("death_rune", 5, 10, 16), d("blood_rune", 5, 10, 16), d("coins", 1300, 1400, 2),
                d("wine_of_zamorak", 5, 10, 16), d("super_attack(3)", 1, 1, 64), d("super_strength(3)", 1, 1, 64));
        // Blood reaver <- parsed:Blood Reaver(161)
        p("blood_reaver", rm("bones"), d("#gems", 1, 1, 128), d("malicious_ashes", 1, 1, 1),
                d("air_rune", 250, 250, 18), d("air_rune", 150, 150, 64), d("astral_rune", 25, 25, 9),
                d("blood_rune", 15, 15, 64), d("chaos_rune", 15, 15, 43), d("mind_rune", 20, 20, 32),
                d("mud_rune", 15, 15, 18), d("nature_rune", 15, 15, 32), d("coins", 500, 500, 14),
                d("coins", 1300, 1337, 21), d("coins", 1, 1, 64), d("magic_potion(1)", 1, 1, 12),
                d("prayer_potion(2)", 1, 1, 14), d("adamantite_bar", 1, 4, 16), d("coal", 1, 10, 16),
                d("nihil_shard", 2, 7, 128), d("pure_essence", 23, 23, 16), d("super_defence(1)", 1, 1, 64));
        // Woman <- parsed:Woman(2)
        p("woman", rm(), d("#herbs", 1, 1, 6), d("iron_dagger", 1, 1, 128), d("bronze_bolts", 2, 12, 6),
                d("bronze_arrow", 7, 7, 43), d("earth_rune", 4, 4, 64), d("fire_rune", 6, 6, 64),
                d("mind_rune", 9, 9, 64), d("chaos_rune", 2, 2, 128), d("coins", 3, 3, 3), d("coins", 10, 10, 6),
                d("coins", 5, 5, 14), d("coins", 15, 15, 32), d("coins", 25, 25, 128), d("fishing_bait", 1, 1, 26),
                d("copper_ore", 1, 1, 64), d("earth_talisman", 1, 1, 64));
        // Farmer <- parsed:Farmer(7)+rebox seeds
        p("farmer", rm("#seeds", "raw_bird_meat"), d("#herbs", 1, 1, 12), d("#allotment_seeds", 1, 1, 3),
                d("bronze_arrow", 7, 7, 43), d("earth_rune", 4, 4, 64), d("fire_rune", 6, 6, 64),
                d("mind_rune", 9, 9, 64), d("chaos_rune", 2, 2, 128), d("coins", 3, 3, 3), d("coins", 10, 10, 6),
                d("coins", 5, 5, 14), d("coins", 15, 15, 32), d("coins", 25, 25, 128), d("copper_ore", 1, 1, 64),
                d("earth_talisman", 1, 1, 64));
        // White Knight <- parsed:White Knight(36)+rebox seeds
        p("white_knight", rm("iron_bolts", "steel_full_helm", "steel_longsword"), d("#gems", 1, 1, 128),
                d("#herbs", 1, 1, 26), d("#allotment_seeds", 1, 1, 2), d("mind_rune", 5, 5, 12),
                d("nature_rune", 4, 4, 32), d("body_rune", 9, 13, 43), d("chaos_rune", 2, 2, 43),
                d("water_rune", 25, 30, 43), d("mithril_arrow", 5, 5, 64), d("adamant_arrow", 2, 2, 128),
                d("blood_rune", 2, 2, 128), d("law_rune", 2, 2, 128), d("coins", 1, 120, 8), d("coins", 48, 48, 9),
                d("coins", 15, 15, 9), d("coins", 5, 5, 9), d("coins", 8, 8, 12), d("coins", 45, 53, 13),
                d("iron_bar", 2, 2, 21), d("iron_bar", 1, 1, 64), d("iron_ore", 1, 1, 128));
        // Dwarf <- parsed:Dwarf(7)
        p("dwarf", rm(), d("#gems", 1, 1, 128), d("bronze_pickaxe", 1, 1, 10), d("bronze_bolts", 2, 12, 18),
                d("chaos_rune", 2, 2, 32), d("nature_rune", 2, 2, 32), d("coins", 4, 4, 6), d("coins", 10, 10, 9),
                d("coins", 30, 30, 64), d("hammer", 1, 1, 13), d("bronze_bar", 1, 1, 18), d("iron_ore", 1, 1, 32),
                d("tin_ore", 1, 1, 43), d("copper_ore", 1, 1, 43), d("iron_bar", 1, 1, 43), d("coal", 1, 1, 64));
        // Highwayman <- parsed:Highwayman(5)
        p("highwayman", rm("coins", "bronze_dagger"), d("iron_bolts", 2, 12, 20), d("black_cape", 1, 1, 1));
        // Mugger <- parsed:Mugger(6)
        p("mugger", rm(), d("#herbs", 1, 1, 10), d("bronze_bolts", 2, 12, 5), d("mind_rune", 9, 9, 43),
                d("water_rune", 6, 6, 64), d("earth_rune", 5, 5, 64), d("coins", 10, 10, 10), d("coins", 5, 5, 11),
                d("coins", 15, 15, 43), d("coins", 25, 25, 128), d("rope", 1, 1, 3), d("copper_ore", 1, 1, 64));
        // Paladin <- parsed:Paladin(62)
        p("paladin", rm("iron_ore", "law_rune"), d("#gems", 1, 1, 64), d("steel_full_helm", 1, 1, 128),
                d("water_rune", 30, 30, 10), d("blood_rune", 1, 1, 128), d("iron_bar", 1, 1, 14),
                d("mithril_bar", 1, 1, 128), d("steel_bar", 1, 1, 128), d("coins", 48, 48, 3), d("coins", 15, 15, 7),
                d("coins", 2, 2, 8), d("coins", 8, 8, 13), d("coins", 120, 120, 64), d("chaos_rune", 2, 2, 1));
        // Gnome <- parsed:Gnome(1)
        p("gnome", rm(), d("coins", 300, 300, 4), d("gold_ore", 1, 1, 16), d("earth_rune", 1, 1, 26),
                d("fire_orb", 1, 1, 64));
        // Pirate <- parsed:Pirate(23)
        p("pirate", rm("#herbs", "iron_scimitar", "karamjan_rum"), d("#gems", 1, 1, 128),
                d("bronze_scimitar", 1, 1, 32), d("iron_bolts", 2, 12, 13), d("chaos_rune", 2, 2, 21),
                d("nature_rune", 2, 2, 26), d("bronze_arrow", 9, 9, 43), d("bronze_arrow", 12, 12, 64),
                d("air_rune", 10, 10, 64), d("earth_rune", 9, 9, 64), d("fire_rune", 5, 5, 64),
                d("law_rune", 2, 2, 128), d("coins", 4, 4, 4), d("coins", 25, 25, 10), d("coins", 7, 7, 16),
                d("coins", 12, 12, 21), d("coins", 35, 35, 32), d("coins", 55, 55, 128), d("iron_bar", 1, 1, 128));
        // TzHaar-Hur <- parsed:TzHaar-Hur(74)
        p("tzhaar_hur_citizen", rm(), d("uncut_diamond", 1, 1, 178), d("tokkul", 1, 16, 2));
        // TzHaar-Ket <- parsed:TzHaar-Ket(149)
        p("tzhaar_ket", rm(), d("#gems", 1, 1, 4), d("tzhaar_ket_om", 1, 1, 256), d("toktz_ket_xil", 1, 1, 256),
                d("obsidian_cape", 1, 1, 256), d("tokkul", 20, 83, 3), d("tokkul", 1, 64, 3),
                d("obsidian_helmet", 1, 1, 2000), d("obsidian_platebody", 1, 1, 2000),
                d("obsidian_platelegs", 1, 1, 2000), d("onyx_bolt_tips", 1, 4, 51));
        // TzHaar-Xil <- parsed:TzHaar-Xil(133)
        p("tzhaar_xil", rm(), d("toktz_xil_ak", 1, 1, 683), d("toktz_xil_ek", 1, 1, 683),
                d("obsidian_cape", 1, 1, 341), d("uncut_diamond", 1, 1, 512), d("toktz_xil_ul", 9, 29, 512));
        // TzHaar-Mej <- parsed:TzHaar-Mej(103)
        p("tzhaar_mej", rm(), d("obsidian_cape", 1, 1, 4096), d("toktz_mej_tal", 1, 1, 4096),
                d("earth_rune", 25, 25, 32), d("fire_rune", 25, 25, 32), d("air_rune", 25, 25, 32),
                d("chaos_rune", 5, 5, 64), d("nature_rune", 2, 2, 64), d("death_rune", 2, 2, 64),
                d("uncut_diamond", 1, 1, 2048), d("tokkul", 1, 21, 2), d("tokkul", 22, 22, 64));
        // Rock crab <- parsed:Rock Crab(13)
        p("rock_crab", rm(), d("#gems", 1, 1, 128), d("seaweed", 1, 1, 32), d("seaweed", 2, 2, 32),
                d("seaweed", 5, 5, 64), d("tin_ore", 3, 3, 32), d("iron_ore", 1, 1, 64), d("coal", 2, 2, 64),
                d("copper_ore", 3, 3, 64), d("oyster", 2, 2, 11), d("oyster", 1, 1, 14), d("coins", 4, 4, 4),
                d("coins", 36, 36, 16), d("coins", 8, 8, 21), d("fishing_bait", 10, 10, 64));
        // Imp <- parsed:Imp(2)
        p("imp", rm("empty_cup", "needle"), d("black_bead", 1, 1, 26), d("red_bead", 1, 1, 26),
                d("white_bead", 1, 1, 26), d("yellow_bead", 1, 1, 26), d("bronze_bolts", 1, 1, 16),
                d("burnt_bread", 1, 1, 32), d("burnt_meat", 1, 1, 32), d("cabbage", 1, 1, 64),
                d("bread_dough", 1, 1, 64), d("hammer", 1, 1, 16), d("tinderbox", 1, 1, 26), d("jug", 1, 1, 64),
                d("pot_of_flour", 1, 1, 64), d("ball_of_wool", 1, 1, 16), d("mind_talisman", 1, 1, 18), d("blue_wizard_hat", 1, 1, 16));
        // Sand crab <- parsed:Sand Crab(15)
        p("sand_crab", rm(), d("#gems", 1, 1, 128), d("coins", 4, 4, 4), d("coins", 36, 36, 16), d("coins", 8, 8, 21),
                d("tin_ore", 3, 3, 32), d("iron_ore", 1, 1, 64), d("coal", 2, 2, 64), d("copper_ore", 3, 3, 64));
        // King sand crab <- parsed:King Sand Crab(107)
        p("king_sand_crab", rm(), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 16), d("#uncommon_seeds", 1, 1, 128),
                d("bones", 1, 1, 1), d("water_rune", 1, 40, 13), d("earth_rune", 1, 40, 13), d("coins", 1, 140, 13),
                d("raw_lobster", 1, 1, 13), d("raw_bass", 1, 1, 13));
        // Lizardman brute <- parsed:Lizardman brute(75)
        p("lizardman_brute", rm("#herbs"), d("lizardman_fang", 1, 1, 5));
        // Deviant spectre <- parsed:Deviant spectre(169)
        p("deviant_spectre", rm("coins"), d("#gems", 1, 1, 4), d("#herbs", 1, 1, 3), d("#uncommon_seeds", 1, 1, 8),
                d("battlestaff", 1, 1, 26), d("rune_full_helm", 1, 1, 64), d("rune_chainbody", 1, 1, 128),
                d("adamantite_ore", 1, 1, 26));
    }

    private static void rows7() {
        // Twisted banshee <- parsed:Twisted Banshee(89)
        p("twisted_banshee", rm(), d("#gems", 1, 1, 64), d("#herbs", 1, 1, 4), d("#uncommon_seeds", 1, 1, 10),
                d("battlestaff", 1, 1, 26), d("adamant_kiteshield", 1, 1, 128), d("air_battlestaff", 1, 1, 64),
                d("rune_dagger", 1, 1, 64), d("rune_med_helm", 1, 1, 64), d("rune_full_helm", 1, 1, 128),
                d("cosmic_rune", 20, 20, 43), d("chaos_rune", 30, 30, 64), d("chaos_rune", 17, 17, 128),
                d("fire_rune", 35, 35, 128), d("pure_essence", 65, 65, 6), d("mithril_ore", 1, 1, 128),
                d("coins", 130, 130, 14), d("coins", 35, 35, 18), d("coins", 260, 260, 18));
        // Warped jelly <- parsed:Warped Jelly(112)
        p("warped_jelly", rm(), d("adamant_2h_sword", 1, 1, 64), d("rune_kiteshield", 1, 1, 64),
                d("rune_full_helm", 1, 1, 128), d("chaos_rune", 45, 45, 26), d("death_rune", 15, 15, 43),
                d("coins", 44, 44, 5), d("coins", 102, 102, 5), d("coins", 220, 220, 14), d("coins", 11, 11, 21),
                d("coins", 460, 460, 64), d("gold_bar", 1, 1, 64));
        // Abyssal demon <- parsed:Abyssal demon(124)
        p("catacomb_abyssal_demon", rm("malicious_ashes", "vile_ashes"), d("#gems", 1, 1, 26), d("#rare", 1, 1, 65),
                d("#herbs", 1, 1, 7), d("rune_chainbody", 1, 1, 128), d("rune_med_helm", 1, 1, 128),
                d("abyssal_dagger", 1, 1, 32000), d("air_rune", 50, 50, 16), d("chaos_rune", 10, 10, 18),
                d("blood_rune", 7, 7, 32), d("pure_essence", 60, 60, 26), d("adamantite_bar", 1, 1, 64),
                d("cosmic_talisman", 1, 1, 128), d("defence_potion(3)", 1, 1, 128), d("abyssal_head", 1, 1, 6000), d("defence_potion(3)", 1, 1, 68));
        // Greater demon <- parsed:Greater demon(92)
        p("catacomb_greater_demon", rm("malicious_ashes"), d("#gems", 1, 1, 26), d("vile_ashes", 1, 2, 1),
                d("adamant_platelegs", 1, 1, 128), d("fire_rune", 75, 75, 16), d("fire_rune", 37, 37, 128),
                d("chaos_rune", 15, 15, 43), d("death_rune", 5, 5, 43), d("coins", 132, 132, 3), d("coins", 44, 44, 4),
                d("coins", 220, 220, 13), d("coins", 11, 11, 18), d("coins", 460, 460, 128), d("gold_bar", 1, 1, 64));
        // Black demon <- parsed:Black demon(172)
        p("catacomb_black_demon", rm("vile_ashes"), d("#gems", 1, 1, 26), d("#rare", 1, 1, 133), d("#herbs", 1, 1, 6),
                d("malicious_ashes", 1, 2, 1), d("rune_chainbody", 1, 1, 128), d("air_rune", 50, 50, 16),
                d("chaos_rune", 10, 10, 18), d("fire_rune", 37, 37, 128), d("adamantite_bar", 1, 1, 64),
                d("defence_potion(3)", 1, 1, 128));
        // Hellhound <- parsed:Hellhound(122)
        p("catacomb_hellhound", rm("malicious_ashes"), d("vile_ashes", 1, 1, 1), d("death_rune", 10, 10, 8),
                d("death_rune", 20, 20, 17), d("death_rune", 50, 50, 50), d("smouldering_stone", 1, 1, 32768));
        // Fire giant <- parsed:Fire giant(86)
        p("catacomb_fire_giant", rm("malicious_ashes", "vile_ashes"), d("#gems", 1, 1, 12), d("#rare", 1, 1, 140),
                d("#herbs", 1, 1, 7), d("fire_battlestaff", 1, 1, 128), d("fire_rune", 150, 150, 13),
                d("fire_rune", 37, 37, 128), d("chaos_rune", 5, 5, 18), d("blood_rune", 5, 5, 32),
                d("steel_bar", 1, 1, 64), d("strength_potion(2)", 1, 1, 128));
        // Moss giant <- parsed:Moss giant(42)+rebox seeds
        p("catacomb_moss_giant", rm("#seeds", "malicious_ashes", "vile_ashes"), d("#gems", 1, 1, 32),
                d("#herbs", 1, 1, 26), d("#rare_seeds", 1, 1, 4), d("magic_staff", 1, 1, 64),
                d("air_rune", 18, 18, 43), d("earth_rune", 27, 27, 43), d("chaos_rune", 7, 7, 43),
                d("nature_rune", 6, 6, 43), d("cosmic_rune", 3, 3, 64), d("iron_arrow", 15, 15, 64),
                d("steel_arrow", 30, 30, 128), d("death_rune", 3, 3, 128), d("blood_rune", 1, 1, 128),
                d("coins", 5, 5, 4), d("coins", 37, 37, 7), d("coins", 2, 2, 12), d("coins", 119, 119, 13),
                d("coins", 10, 10, 26), d("coins", 300, 300, 64), d("steel_bar", 1, 1, 21), d("coal", 1, 1, 128),
                d("mossy_key", 1, 1, 150), d("mithril_spear", 1, 1, 64));
        // Hill giant <- parsed:Hill Giant(28)+rebox seeds
        p("catacomb_hill_giant", rm("malicious_ashes", "vile_ashes"), d("#gems", 1, 1, 43), d("#herbs", 1, 1, 18),
                d("#allotment_seeds", 1, 1, 7), d("iron_arrow", 3, 3, 21), d("fire_rune", 15, 15, 43),
                d("water_rune", 7, 7, 43), d("law_rune", 2, 2, 43), d("steel_arrow", 10, 10, 64),
                d("mind_rune", 3, 3, 64), d("cosmic_rune", 2, 2, 64), d("nature_rune", 6, 6, 64),
                d("chaos_rune", 2, 2, 128), d("death_rune", 2, 2, 128), d("coins", 5, 5, 7), d("coins", 38, 38, 9),
                d("coins", 52, 52, 13), d("coins", 15, 15, 16), d("coins", 8, 88, 16), d("coins", 10, 10, 18),
                d("limpwurt_root", 1, 1, 12), d("body_talisman", 1, 1, 64));
        // Dagannoth <- parsed:Dagannoth(74)
        p("catacomb_dagannoth", rm("#herbs", "malicious_ashes", "vile_ashes"), d("#gems", 1, 1, 128),
                d("#uncommon_seeds", 1, 1, 7), d("water_rune", 15, 15, 32), d("steel_arrow", 15, 15, 64),
                d("mithril_javelin", 3, 3, 128), d("raw_herring", 3, 3, 32), d("raw_sardine", 5, 5, 32),
                d("raw_lobster", 1, 1, 64), d("raw_tuna", 1, 1, 64), d("coins", 56, 56, 4), d("coins", 25, 25, 14),
                d("coins", 44, 44, 16), d("coins", 41, 41, 21), d("iron_spear", 1, 1, 21), d("bronze_spear", 1, 1, 26), d("mithril_spear", 1, 1, 128));
        // Ankou <- parsed:Ankou(75)+rebox seeds
        p("catacomb_ankou", rm("malicious_ashes", "vile_ashes"), d("#gems", 1, 1, 50), d("#herbs", 1, 1, 33),
                d("#rare_seeds", 1, 1, 100), d("blood_rune", 11, 11, 17), d("blood_rune", 5, 5, 33),
                d("adamant_arrow", 5, 14, 25), d("law_rune", 2, 2, 14), d("pure_essence", 15, 15, 20),
                d("mithril_ore", 3, 7, 50), d("coins", 8, 8, 3), d("coins", 5, 204, 10), d("black_knife", 1, 1, 100));
        // Dust devil <- parsed:Dust devil(93)
        p("catacomb_dust_devil", rm("malicious_ashes", "vile_ashes"), d("#gems", 1, 1, 12), d("#herbs", 1, 1, 7),
                d("bones", 1, 1, 1), d("rune_dagger", 1, 1, 64), d("red_dhide_vambraces", 1, 1, 64),
                d("black_dhide_vambraces", 1, 1, 128), d("air_battlestaff", 1, 1, 64),
                d("earth_battlestaff", 1, 1, 64), d("dragon_dagger", 1, 1, 128), d("dust_rune", 200, 200, 13),
                d("earth_rune", 300, 300, 13), d("fire_rune", 300, 300, 13), d("fire_rune", 50, 50, 128),
                d("chaos_rune", 65, 65, 18), d("rune_arrow", 12, 12, 26), d("soul_rune", 15, 15, 32),
                d("soul_rune", 50, 50, 128), d("coins", 2000, 4000, 5), d("mithril_bar", 10, 10, 43),
                d("adamantite_bar", 4, 4, 128), d("mystic_air_staff", 1, 1, 128), d("mystic_earth_staff", 1, 1, 128));
        // Sulphur nagua <- parsed:Sulphur Nagua(98)
        p("sulphur_nagua", rm("#herbs", "coins"), d("#gems", 1, 1, 21), d("chaos_rune", 40, 60, 32),
                d("death_rune", 40, 60, 128), d("fire_rune", 10, 40, 5), d("fire_rune", 40, 60, 128),
                d("nature_rune", 5, 10, 32), d("coal", 1, 1, 6), d("coal", 5, 10, 10), d("copper_ore", 10, 15, 43),
                d("iron_ore", 5, 10, 5), d("silver_ore", 5, 10, 11), d("tin_ore", 10, 15, 26),
                d("mithril_ore", 5, 10, 128));
        // Chaos druid <- parsed:Chaos druid(13)
        p("chaos_druid", rm("limpwurt_root"), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 3), d("law_rune", 2, 2, 18),
                d("mithril_bolts", 2, 12, 32), d("air_rune", 36, 36, 43), d("body_rune", 9, 9, 64),
                d("earth_rune", 9, 9, 64), d("mind_rune", 12, 12, 64), d("nature_rune", 3, 3, 128),
                d("coins", 3, 3, 26), d("coins", 8, 8, 26), d("coins", 29, 29, 43), d("coins", 35, 35, 128),
                d("snape_grass", 1, 1, 128));
        // Flesh Crawler <- parsed:Flesh Crawler(41)
        p("flesh_crawler", rm(), d("#gems", 1, 1, 50), d("#herbs", 1, 1, 3), d("body_rune", 3, 12, 3),
                d("fire_rune", 42, 42, 25), d("dust_rune", 3, 9, 10), d("nature_rune", 5, 5, 25),
                d("iron_ore", 3, 7, 8), d("iron_ore", 1, 1, 10), d("silver_bar", 1, 1, 50), d("coins", 5, 84, 7),
                d("coins", 10, 10, 25));
        // Catablepon <- parsed:Catablepon(64)+rebox seeds
        p("catablepon", rm(), d("#gems", 1, 1, 50), d("#herbs", 1, 1, 34), d("#allotment_seeds", 1, 1, 74),
                d("fire_rune", 15, 15, 14), d("water_rune", 7, 7, 17), d("chaos_rune", 7, 7, 101),
                d("law_rune", 2, 2, 25), d("cosmic_rune", 2, 2, 34), d("mithril_arrow", 5, 14, 25),
                d("rune_essence", 15, 15, 20), d("pure_essence", 15, 15, 20), d("coal", 3, 7, 50),
                d("coins", 44, 44, 8), d("coins", 5, 104, 10), d("coins", 15, 15, 17), d("coins", 10, 10, 25));
        // Cave goblin guard <- parsed:Cave goblin guard(24)
        p("cave_goblin_guard", rm("iron_dagger"), d("coins", 12, 12, 6));
        // Abyssal leech <- parsed:Abyssal leech(41)
        p("abyssal_leech", rm(), d("air_talisman", 1, 1, 51), d("water_talisman", 1, 1, 51),
                d("earth_talisman", 1, 1, 51), d("fire_talisman", 1, 1, 51), d("mind_talisman", 1, 1, 51),
                d("body_talisman", 1, 1, 51), d("cosmic_talisman", 1, 1, 128), d("chaos_talisman", 1, 1, 128),
                d("nature_talisman", 1, 1, 128), d("elemental_talisman", 1, 1, 256), d("giant_pouch", 1, 1, 42),
                d("pure_essence", 1, 1, 4), d("pure_essence", 5, 5, 64), d("pure_essence", 10, 10, 64),
                d("pure_essence", 15, 15, 64), d("binding_necklace", 1, 1, 85));
        // Abyssal guardian <- parsed:Abyssal guardian(59)
        p("abyssal_guardian", rm(), d("air_talisman", 1, 1, 51), d("water_talisman", 1, 1, 51),
                d("earth_talisman", 1, 1, 51), d("fire_talisman", 1, 1, 51), d("mind_talisman", 1, 1, 51),
                d("body_talisman", 1, 1, 51), d("cosmic_talisman", 1, 1, 128), d("chaos_talisman", 1, 1, 128),
                d("nature_talisman", 1, 1, 128), d("elemental_talisman", 1, 1, 256), d("giant_pouch", 1, 1, 8),
                d("pure_essence", 1, 1, 4), d("pure_essence", 5, 5, 64), d("pure_essence", 10, 10, 64),
                d("pure_essence", 15, 15, 64), d("binding_necklace", 1, 1, 85));
        // Abyssal walker <- parsed:Abyssal walker(81)
        p("abyssal_walker", rm(), d("air_talisman", 1, 1, 51), d("water_talisman", 1, 1, 51),
                d("earth_talisman", 1, 1, 51), d("fire_talisman", 1, 1, 51), d("mind_talisman", 1, 1, 51),
                d("body_talisman", 1, 1, 51), d("cosmic_talisman", 1, 1, 128), d("chaos_talisman", 1, 1, 128),
                d("nature_talisman", 1, 1, 128), d("elemental_talisman", 1, 1, 256), d("giant_pouch", 1, 1, 5),
                d("pure_essence", 1, 1, 4), d("pure_essence", 5, 5, 64), d("pure_essence", 10, 10, 64),
                d("pure_essence", 15, 15, 64), d("binding_necklace", 1, 1, 85));
        // Ammonite crab <- parsed:Ammonite Crab(25)
        p("ammonite_crab", rm(), d("#gems", 1, 1, 128), d("numulite", 4, 4, 4), d("numulite", 2, 2, 16),
                d("numulite", 8, 8, 21), d("iron_ore", 1, 1, 64), d("tin_ore", 3, 3, 32), d("coal", 2, 2, 64),
                d("copper_ore", 3, 3, 64), d("seaweed_spore", 2, 2, 128));
        // Long-tailed wyvern <- parsed:Long-tailed Wyvern(152)
        p("long_tailed_wyvern", rm("#herbs", "#gems", "rune_battleaxe"), d("air_battlestaff", 1, 1, 31),
                d("battlestaff", 3, 5, 41), d("rune_pickaxe", 1, 1, 61), d("adamant_platebody", 1, 1, 61),
                d("granite_longsword", 1, 1, 512), d("granite_boots", 1, 1, 2560), d("adamant_arrow", 38, 42, 20),
                d("water_rune", 50, 50, 31), d("chaos_rune", 15, 15, 31), d("law_rune", 15, 15, 31),
                d("death_rune", 15, 15, 31), d("blood_rune", 15, 15, 31), d("soul_rune", 10, 10, 123),
                d("rune_bolts", 12, 30, 123), d("grimy_ranarr_weed", 1, 1, 61), d("grimy_torstol", 1, 1, 61),
                d("seaweed_spore", 12, 12, 61), d("pure_essence", 150, 150, 15), d("adamantite_bar", 2, 4, 20),
                d("teak_logs", 35, 35, 20), d("snape_grass", 10, 15, 41), d("runite_ore", 1, 2, 41),
                d("coins", 3000, 3000, 11), d("prayer_potion", 1, 1, 18), d("adamant_crossbow_u", 1, 1, 61));
        // Ancient wyvern <- parsed:Ancient Wyvern(210)
        p("ancient_wyvern", rm("#herbs", "#gems"), d("#rare", 1, 1, 66), d("air_battlestaff", 2, 2, 33),
                d("rune_pickaxe", 1, 1, 44), d("rune_full_helm", 1, 1, 66), d("battlestaff", 6, 6, 33),
                d("granite_longsword", 1, 1, 600), d("granite_boots", 1, 1, 600), d("adamant_arrow", 50, 76, 44),
                d("blood_rune", 27, 33, 33), d("rune_bolts", 30, 60, 132), d("grimy_torstol", 1, 1, 33),
                d("grimy_ranarr_weed", 2, 4, 33), d("seaweed_spore", 16, 24, 66), d("adamantite_bar", 3, 3, 22),
                d("adamantite_ore", 20, 20, 26), d("runite_ore", 2, 3, 44), d("diamond", 3, 5, 44),
                d("onyx_bolt_tips", 10, 15, 33), d("mahogany_logs", 25, 30, 26), d("super_combat_potion(2)", 1, 1, 26),
                d("super_restore", 2, 2, 16));
        // Monkey Zombie <- parsed:Monkey Zombie(98)
        p("monkey_zombie", rm("#herbs", "coins"));
        // Suqah <- parsed:Suqah(111)
        p("suqah", rm(), d("#gems", 1, 1, 26), d("#herbs", 1, 1, 1), d("suqah_tooth", 1, 1, 2));
    }

    private static void rows8() {
        // Yak <- parsed:Yak(22)
        p("yak", rm(), d("yak_hair", 1, 1, 1));
        // Wallasalki <- parsed:Wallasalki(98)
        p("wallasalki", rm("raw_shark"), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 13), d("#uncommon_seeds", 1, 1, 128),
                d("air_rune", 1, 10, 13), d("mind_rune", 1, 10, 13), d("water_rune", 1, 10, 13),
                d("earth_rune", 1, 10, 13), d("chaos_rune", 1, 10, 13), d("death_rune", 1, 10, 13),
                d("blood_rune", 1, 5, 13), d("steel_arrow", 1, 10, 13), d("pure_essence", 9, 9, 13),
                d("raw_herring", 1, 2, 64), d("raw_mackerel", 1, 2, 43), d("raw_tuna", 1, 2, 64));
        // Rock lobster <- parsed:Rock lobster(127)
        p("rock_lobster", rm("coins"), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 43), d("raw_lobster", 1, 1, 1),
                d("earth_rune", 1, 20, 13), d("water_rune", 1, 20, 13), d("steel_arrow", 1, 10, 13),
                d("iron_arrow", 1, 20, 13), d("pure_essence", 4, 4, 13), d("snape_grass", 1, 1, 13));
        // Jungle horror <- parsed:Jungle horror(70)+rebox seeds
        p("jungle_horror", rm("#seeds", "coins"), d("#gems", 1, 1, 129), d("#herbs", 1, 1, 22),
                d("#allotment_seeds", 1, 1, 6), d("iron_javelin", 5, 5, 64), d("nature_rune", 4, 4, 13),
                d("nature_rune", 2, 2, 18), d("nature_rune", 3, 3, 64), d("death_rune", 1, 1, 26),
                d("iron_ore", 1, 1, 5), d("teak_logs", 3, 3, 14), d("mahogany_logs", 1, 1, 43),
                d("big_bones", 1, 1, 43), d("big_bones", 3, 3, 64), d("iron_spear", 1, 1, 32));
        // Jogre <- parsed:Jogre(53)+rebox seeds
        p("jogre", rm("coins", "iron_ore"), d("#gems", 1, 1, 129), d("#herbs", 1, 1, 22), d("#rare_seeds", 1, 1, 9),
                d("nature_rune", 2, 2, 13), d("nature_rune", 10, 10, 64), d("nature_rune", 5, 5, 64),
                d("steel_javelin", 5, 5, 64), d("bones", 1, 1, 26), d("big_bones", 1, 1, 43), d("big_bones", 3, 3, 64), d("bronze_spear", 1, 1, 4), d("iron_spear", 1, 1, 32));
        // Ogress Warrior <- parsed:Ogress Warrior(82)
        p("ogress_warrior", rm("#herbs"), d("#gems", 1, 1, 7), d("mithril_arrow", 5, 15, 17),
                d("chaos_rune", 15, 30, 17), d("law_rune", 8, 15, 17), d("nature_rune", 8, 15, 17),
                d("death_rune", 8, 15, 19), d("air_rune", 10, 20, 23), d("cosmic_rune", 10, 15, 23),
                d("earth_rune", 10, 20, 23), d("fire_rune", 10, 20, 23), d("mind_rune", 10, 20, 23),
                d("water_rune", 10, 20, 23), d("steel_arrow", 10, 30, 23), d("iron_arrow", 20, 40, 23),
                d("limpwurt_root", 1, 1, 23), d("coins", 500, 1000, 13), d("coins", 5, 5, 23),
                d("rune_med_helm", 1, 1, 40), d("rune_full_helm", 1, 1, 100), d("rune_battleaxe", 1, 1, 100));
        // Ogress Shaman <- parsed:Ogress Shaman(82)
        p("ogress_shaman", rm("#herbs"), d("#gems", 1, 1, 7), d("mithril_arrow", 5, 15, 17),
                d("chaos_rune", 15, 30, 17), d("law_rune", 8, 15, 17), d("nature_rune", 8, 15, 17),
                d("death_rune", 8, 15, 19), d("air_rune", 10, 20, 23), d("cosmic_rune", 10, 15, 23),
                d("earth_rune", 10, 20, 23), d("fire_rune", 10, 20, 23), d("mind_rune", 10, 20, 23),
                d("water_rune", 10, 20, 23), d("steel_arrow", 10, 30, 23), d("iron_arrow", 20, 40, 23),
                d("limpwurt_root", 1, 1, 23), d("coins", 500, 1000, 13), d("coins", 5, 5, 23),
                d("rune_med_helm", 1, 1, 40), d("rune_full_helm", 1, 1, 100), d("rune_battleaxe", 1, 1, 100));
        // Man <- parsed:Man(2)
        p("Monster.MAN", rm("#seeds"), d("#herbs", 1, 1, 6), d("bronze_bolts", 2, 12, 6), d("bronze_arrow", 7, 7, 43),
                d("earth_rune", 4, 4, 64), d("fire_rune", 6, 6, 64), d("mind_rune", 9, 9, 64),
                d("chaos_rune", 2, 2, 128), d("coins", 3, 3, 3), d("coins", 10, 10, 6), d("coins", 5, 5, 14),
                d("coins", 15, 15, 32), d("coins", 25, 25, 128), d("copper_ore", 1, 1, 64),
                d("earth_talisman", 1, 1, 64));
        // Goblin <- parsed:Goblin(2)
        p("Monster.GOBLIN", rm("air_rune"), d("#herbs", 1, 1, 64), d("body_rune", 7, 7, 26), d("water_rune", 6, 6, 21),
                d("earth_rune", 4, 4, 43), d("bronze_bolts", 8, 8, 43), d("coins", 5, 5, 5), d("coins", 9, 9, 43),
                d("coins", 15, 15, 43), d("coins", 20, 20, 64), d("coins", 1, 1, 128), d("air_talisman", 1, 1, 128));
        // Al-Kharid warrior <- parsed:Al Kharid warrior(9)
        p("Monster.AL_KHARID_WARRIOR", rm("iron_scimitar"), d("#herbs", 1, 1, 6), d("bronze_bolts", 2, 12, 6),
                d("bronze_arrow", 7, 7, 43), d("earth_rune", 4, 4, 64), d("fire_rune", 6, 6, 64),
                d("mind_rune", 9, 9, 64), d("chaos_rune", 2, 2, 128), d("coins", 3, 3, 3), d("coins", 10, 10, 6),
                d("coins", 5, 5, 14), d("coins", 15, 15, 32), d("coins", 25, 25, 128), d("copper_ore", 1, 1, 64),
                d("earth_talisman", 1, 1, 64));
        // Barbarian <- parsed:Barbarian(10)
        p("Monster.BARBARIAN", rm("#herbs", "iron_battleaxe"), d("#gems", 1, 1, 128), d("chaos_rune", 2, 2, 32),
                d("bronze_arrow", 15, 15, 43), d("earth_rune", 2, 2, 43), d("fire_rune", 5, 5, 64),
                d("mind_rune", 5, 5, 64), d("law_rune", 2, 2, 128), d("coins", 5, 5, 3), d("coins", 8, 8, 14),
                d("coins", 17, 17, 26), d("coins", 27, 27, 43), d("tin_ore", 1, 1, 128), d("bear_fur", 1, 1, 128));
        // Dark wizard <- parsed:Dark wizard(7)
        p("Monster.DARK_WIZARD", rm("#herbs"), d("staff", 1, 1, 16), d("earth_rune", 36, 36, 32),
                d("earth_rune", 10, 10, 43), d("earth_rune", 18, 18, 64), d("air_rune", 10, 10, 43),
                d("air_rune", 18, 18, 64), d("water_rune", 10, 10, 43), d("water_rune", 18, 18, 64),
                d("fire_rune", 10, 10, 43), d("fire_rune", 18, 18, 64), d("nature_rune", 4, 4, 18),
                d("chaos_rune", 5, 5, 21), d("mind_rune", 10, 10, 43), d("mind_rune", 18, 18, 64),
                d("body_rune", 10, 10, 43), d("body_rune", 18, 18, 64), d("blood_rune", 2, 2, 64),
                d("cosmic_rune", 2, 2, 128), d("law_rune", 3, 3, 128), d("coins", 1, 1, 8), d("coins", 2, 2, 8),
                d("coins", 4, 4, 18), d("coins", 29, 29, 43), d("coins", 30, 30, 128), d("water_talisman", 1, 1, 128),
                d("fire_talisman", 1, 1, 128));
        // Guard <- parsed:Guard(21)+rebox seeds
        p("Monster.GUARD", rm("#seeds", "#herbs"), d("#allotment_seeds", 1, 1, 7), d("iron_bolts", 2, 12, 13),
                d("steel_arrow", 1, 1, 32), d("steel_arrow", 5, 5, 128), d("bronze_arrow", 1, 1, 43),
                d("bronze_arrow", 2, 2, 64), d("air_rune", 6, 6, 64), d("earth_rune", 3, 3, 64),
                d("fire_rune", 2, 2, 64), d("blood_rune", 1, 1, 128), d("chaos_rune", 1, 1, 128),
                d("nature_rune", 1, 1, 128), d("coins", 1, 1, 7), d("coins", 5, 5, 7), d("coins", 7, 7, 8),
                d("coins", 17, 30, 13), d("coins", 12, 12, 14), d("coins", 4, 4, 16), d("iron_dagger", 1, 1, 21),
                d("body_talisman", 1, 1, 43), d("iron_ore", 1, 1, 128));
        // Hill Giant <- parsed:Hill Giant(28)+rebox seeds
        p("Monster.HILL_GIANT", rm(), d("#gems", 1, 1, 43), d("#allotment_seeds", 1, 1, 7), d("iron_arrow", 3, 3, 21),
                d("fire_rune", 15, 15, 43), d("water_rune", 7, 7, 43), d("law_rune", 2, 2, 43),
                d("steel_arrow", 10, 10, 64), d("mind_rune", 3, 3, 64), d("cosmic_rune", 2, 2, 64),
                d("nature_rune", 6, 6, 64), d("chaos_rune", 2, 2, 128), d("death_rune", 2, 2, 128),
                d("coins", 5, 5, 7), d("coins", 38, 38, 9), d("coins", 52, 52, 13), d("coins", 15, 15, 16),
                d("coins", 8, 88, 16), d("coins", 10, 10, 18), d("body_talisman", 1, 1, 64));
        // Moss giant <- parsed:Moss giant(42)+rebox seeds
        p("Monster.MOSS_GIANT", rm("#seeds", "mithril_scimitar"), d("#gems", 1, 1, 32), d("#herbs", 1, 1, 26),
                d("#rare_seeds", 1, 1, 4), d("magic_staff", 1, 1, 64), d("law_rune", 3, 3, 32),
                d("air_rune", 18, 18, 43), d("earth_rune", 27, 27, 43), d("chaos_rune", 7, 7, 43),
                d("nature_rune", 6, 6, 43), d("cosmic_rune", 3, 3, 64), d("iron_arrow", 15, 15, 64),
                d("steel_arrow", 30, 30, 128), d("death_rune", 3, 3, 128), d("blood_rune", 1, 1, 128),
                d("coins", 5, 5, 4), d("coins", 37, 37, 7), d("coins", 2, 2, 12), d("coins", 119, 119, 13),
                d("coins", 10, 10, 26), d("coins", 300, 300, 64), d("steel_bar", 1, 1, 21), d("coal", 1, 1, 128));
        // Ice giant <- parsed:Ice giant(53)+rebox seeds
        p("Monster.ICE_GIANT", rm("#herbs"), d("#gems", 1, 1, 32), d("#rare_seeds", 1, 1, 10),
                d("adamant_arrow", 5, 5, 21), d("nature_rune", 6, 6, 32), d("mind_rune", 24, 24, 43),
                d("body_rune", 37, 37, 43), d("law_rune", 3, 3, 64), d("water_rune", 12, 12, 128),
                d("cosmic_rune", 4, 4, 128), d("death_rune", 3, 3, 128), d("blood_rune", 2, 2, 128),
                d("coins", 117, 117, 4), d("coins", 53, 53, 11), d("coins", 196, 196, 13), d("coins", 5, 5, 16),
                d("coins", 22, 400, 16), d("coins", 8, 8, 18), d("mithril_ore", 1, 1, 128));
        // Lesser demon <- parsed:Lesser demon(82)
        p("Monster.LESSER_DEMON", rm("rune_full_helm", "mithril_ore"), d("#herbs", 1, 1, 128),
                d("vile_ashes", 1, 2, 1), d("rune_med_helm", 1, 1, 128), d("fire_rune", 60, 60, 16),
                d("fire_rune", 30, 30, 128), d("chaos_rune", 12, 12, 26), d("death_rune", 3, 3, 43),
                d("coins", 120, 120, 3), d("coins", 40, 40, 4), d("coins", 200, 200, 13), d("coins", 10, 10, 16),
                d("coins", 450, 450, 128), d("gold_ore", 1, 1, 64));
        // Greater demon <- parsed:Greater demon(92)
        p("Monster.GREATER_DEMON", rm("#herbs"), d("#gems", 1, 1, 26), d("vile_ashes", 1, 2, 1),
                d("adamant_platelegs", 1, 1, 128), d("rune_full_helm", 1, 1, 128), d("fire_rune", 75, 75, 16),
                d("fire_rune", 37, 37, 128), d("chaos_rune", 15, 15, 43), d("death_rune", 5, 5, 43),
                d("coins", 132, 132, 3), d("coins", 44, 44, 4), d("coins", 220, 220, 13), d("coins", 11, 11, 18),
                d("coins", 460, 460, 128), d("gold_bar", 1, 1, 64));
        // Black demon <- parsed:Black demon(172)
        p("Monster.BLACK_DEMON", rm("rune_platelegs"), d("#gems", 1, 1, 26), d("#rare", 1, 1, 133),
                d("#herbs", 1, 1, 6), d("malicious_ashes", 1, 2, 1), d("rune_med_helm", 1, 1, 128),
                d("rune_chainbody", 1, 1, 128), d("air_rune", 50, 50, 16), d("chaos_rune", 10, 10, 18),
                d("blood_rune", 7, 7, 32), d("fire_rune", 37, 37, 128), d("adamantite_bar", 1, 1, 64),
                d("defence_potion", 1, 1, 128));
        // Zombie <- parsed:Zombie(13)
        p("Monster.ZOMBIE", rm(), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 5), d("iron_arrow", 5, 5, 18),
                d("iron_arrow", 8, 8, 32), d("body_rune", 6, 6, 26), d("mind_rune", 5, 5, 26),
                d("air_rune", 13, 13, 32), d("steel_arrow", 5, 5, 64), d("nature_rune", 6, 6, 128),
                d("coins", 10, 10, 4), d("coins", 4, 4, 32), d("coins", 18, 18, 43), d("coins", 13, 13, 64),
                d("coins", 28, 28, 64), d("copper_ore", 1, 1, 64));
        // Skeleton <- parsed:Skeleton(25)
        p("Monster.SKELETON", rm(), d("#gems", 1, 1, 128), d("#herbs", 1, 1, 6));
        // Man <- parsed:Man(2)
        p("Monster.VILLAGER", rm(), d("#herbs", 1, 1, 6), d("bronze_bolts", 2, 12, 6), d("bronze_arrow", 7, 7, 43),
                d("earth_rune", 4, 4, 64), d("fire_rune", 6, 6, 64), d("mind_rune", 9, 9, 64),
                d("chaos_rune", 2, 2, 128), d("coins", 3, 3, 3), d("coins", 10, 10, 6), d("coins", 5, 5, 14),
                d("coins", 15, 15, 32), d("coins", 25, 25, 128), d("copper_ore", 1, 1, 64),
                d("earth_talisman", 1, 1, 64));
        // Dark wizard <- parsed:Dark wizard(20)
        p("Monster.WITCH", rm("#herbs", "eye_of_newt"), d("earth_rune", 36, 36, 32), d("earth_rune", 10, 10, 43),
                d("earth_rune", 18, 18, 64), d("air_rune", 10, 10, 43), d("air_rune", 18, 18, 64),
                d("water_rune", 10, 10, 43), d("water_rune", 18, 18, 64), d("fire_rune", 10, 10, 43),
                d("fire_rune", 18, 18, 64), d("nature_rune", 4, 4, 18), d("chaos_rune", 5, 5, 21),
                d("mind_rune", 10, 10, 43), d("mind_rune", 18, 18, 64), d("body_rune", 10, 10, 43),
                d("body_rune", 18, 18, 64), d("blood_rune", 2, 2, 64), d("cosmic_rune", 2, 2, 128),
                d("law_rune", 3, 3, 128), d("coins", 1, 1, 8), d("coins", 2, 2, 8), d("coins", 4, 4, 18),
                d("coins", 29, 29, 43), d("coins", 30, 30, 128), d("water_talisman", 1, 1, 128),
                d("fire_talisman", 1, 1, 128));
    }

    /** Dungeon and lair monsters added after the sweep, matched by hand against the wiki pages. */
    private static void rows9() {
        // Dagannoth (Waterbirth Island) Level 88 <- wiki "Drops (Level 88)"
        p("dagannoth_lvl88", rm(), d("mithril_javelin", 1, 3, 64), d("archer_helm", 1, 1, 2048),
                d("farseer_helm", 1, 1, 2048), d("casket", 1, 1, 26), d("oyster_pearls", 1, 1, 26),
                d("opal_bolt_tips", 1, 20, 13), d("seaweed", 1, 10, 4), d("raw_bass", 1, 1, 26),
                d("berserker_helm", 1, 1, 2048), d("warrior_helm", 1, 1, 2048), d("fremennik_gloves", 1, 1, 64),
                d("fremennik_brown_shirt", 1, 1, 64), d("fremennik_brown_cloak", 1, 1, 64), d("spined_boots", 1, 1, 64),
                d("spined_gloves", 1, 1, 64));
        // Dagannoth (Waterbirth Island) Level 90 <- wiki "Drops (Level 90)"
        p("dagannoth_lvl90", rm(), d("mithril_javelin", 3, 3, 13), d("steel_arrow", 15, 15, 26),
                d("archer_helm", 1, 1, 2048), d("farseer_helm", 1, 1, 2048), d("tinderbox", 1, 1, 13),
                d("oyster_pearl", 1, 1, 18), d("coins", 3, 3, 64), d("coins", 26, 26, 64), d("coins", 38, 38, 64),
                d("coins", 47, 47, 64), d("coins", 81, 81, 64), d("berserker_helm", 1, 1, 2048),
                d("warrior_helm", 1, 1, 2048), d("fremennik_gloves", 1, 1, 64), d("fremennik_brown_shirt", 1, 1, 64),
                d("fremennik_brown_cloak", 1, 1, 43));
        // Dagannoth (Level 92) shares the wiki's Dagannoth table with level 74
        PATCHES.put("dagannoth_lvl92", PATCHES.get("dagannoth"));
        // Giant Rock Crab <- wiki
        p("giant_rock_crab", rm(), d("black_claws", 1, 1, 26), d("raw_bass", 1, 1, 13), d("casket", 1, 1, 64));
        // Maniacal monkey / archer <- wiki (2/20 prayer potion(1) and antipoison(2))
        p("maniacal_monkey", rm(), d("prayer_potion(1)", 1, 1, 10), d("antipoison(2)", 1, 1, 10));
        p("maniacal_monkey_archer", rm(), d("prayer_potion(1)", 1, 1, 10), d("antipoison(2)", 1, 1, 10));
        // Tortured gorilla (Level 141) <- wiki (rates out of 1000)
        p("tortured_gorilla", rm(), d("shark", 1, 1, 11), d("coins", 1000, 2000, 10), d("prayer_potion(1)", 1, 1, 16),
                d("javelin_shaft", 100, 300, 12), d("adamantite_bar", 1, 2, 19), d("ruby", 2, 3, 19),
                d("rune_javelin_heads", 10, 10, 22), d("dragon_javelin_heads", 5, 5, 22), d("#rare", 1, 1, 143));
    }

    /**
     * Drops whose items were missing from the catalog when the sweep ran (rows0-rows9 skipped them), added once the items
     * existed: long and curved bones, monster bones (monkey, jogre, wyrm, drake), Fremennik-set gloves and boots,
     * ceremonial robes, granite helm, black robe, coloured gloves, oyster pearls and unicorn horns, plus lines the sweep
     * missed because RuneCraft names the item differently (javelin "tips" are javelin heads, "steel cannonball" is
     * cannonball, "runite crossbow (u)" is rune_crossbow_u). Generated from the same parsed-osrs tables; each line adds
     * to the monster's existing patch.
     */
    private static void rows10() {
        // Earth impling <- parsed:Earth impling(0)
        add("earth_impling", rm(), d("unicorn_horn", 1, 1, 10));
        // Eclectic impling <- parsed:Eclectic impling(0)
        add("eclectic_impling", rm(), d("unicorn_horn", 1, 1, 10));
        // Crystal impling <- parsed:Crystal impling(0)
        add("crystal_impling", rm(), d("rune_javelin_heads", 20, 60, 18));
        // Dragon impling <- parsed:Dragon impling(0)
        add("dragon_impling", rm(), d("dragon_javelin_heads", 25, 35, 19));
        // Monkey <- parsed:Monkey (monster)(3)
        add("monkey", rm("bones"), d("monkey_bones", 1, 1, 1));
        // Crawling Hand <- parsed:Crawling Hand(8)
        add("crawling_hand", rm(), d("purple_gloves", 1, 1, 64), d("yellow_gloves", 1, 1, 64), d("red_gloves", 1, 1, 64),
                d("teal_gloves", 1, 1, 64));
        // Lizard <- parsed:Lizard(42)
        add("lizard", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Mogre <- parsed:Mogre(60)
        add("mogre", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Killerwatt <- parsed:Killerwatt(55)
        add("killerwatt", rm(), d("cannonball", 3, 3, 70));
        // Steel cannonballs (OSRS Wiki drop tables): zombie pirates 20-100 at 12/378, custodian stalkers 10-15 at 20/160
        // (juvenile), 15-30 at 15/107 (mature) and 20-30 at 15/124 (elder).
        add("zombie_pirate", rm(), d("cannonball", 20, 100, 32));
        add("juvenile_custodian_stalker", rm(), d("cannonball", 10, 15, 8));
        add("mature_custodian_stalker", rm(), d("cannonball", 15, 30, 7));
        add("elder_custodian_stalker", rm(), d("cannonball", 20, 30, 8));
        // Sea Snake Young <- parsed:Sea Snake Young(90)
        add("sea_snake_young", rm(), d("oyster_pearls", 1, 1, 128), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Terror dog <- parsed:Terror dog(100)
        add("terror_dog", rm(), d("granite_helm", 1, 1, 128), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Cave horror <- parsed:Cave horror(80)
        add("cave_horror", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // (The wiki's spiritual creature pages list the ancient ceremonial robes, but only the Ancient Prison's drop them;
        // the slayer module's spiritual creatures are the Wilderness God Wars ones, so they get none.)
        // Ankou <- parsed:Ankou(75)
        add("ankou", rm(), d("black_robe", 1, 1, 100));
        // Stronghold of Security sceptre pieces ("roughly 3%" each, Mod Ash; 1/33 approx) and the chaos dwarves' muddy key
        // (7/128 on the wiki, about 1/18).
        add("minotaur", rm(), d("right_skull_half", 1, 1, 33));
        add("flesh_crawler", rm(), d("bottom_of_sceptre", 1, 1, 33));
        add("catablepon", rm(), d("top_of_sceptre", 1, 1, 33));
        add("ankou", rm(), d("left_skull_half", 1, 1, 33));
        add("chaos_dwarf", rm(), d("muddy_key", 1, 1, 18));
        // Fire giant <- parsed:Fire giant(86)
        add("fire_giant", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Mountain troll <- parsed:Mountain troll(69)
        add("mountain_troll", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Ice troll <- parsed:Ice troll(120)
        add("ice_troll", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Ogre <- parsed:Ogre(53)
        add("ogre", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Cyclops <- parsed:Cyclops(76)
        add("cyclops", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Dagannoth <- parsed:Dagannoth(74)
        add("dagannoth", rm(), d("oyster_pearls", 1, 1, 128));
        // Lizardman shaman <- parsed:Lizardman shaman(150)
        add("lizardman_shaman", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Wyrm <- parsed:Wyrm(97)
        add("wyrm", rm("bones"), d("wyrm_bones", 1, 1, 1));
        // Skeletal Wyvern <- parsed:Skeletal Wyvern(140)
        add("skeletal_wyvern", rm(), d("rune_crossbow_u", 1, 1, 64));
        // Cave kraken <- parsed:Cave kraken(127)
        add("cave_kraken", rm(), d("antidote_plus_plus", 1, 1, 33));
        // Dark beast <- parsed:Dark beast(182)
        add("dark_beast", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Drake <- parsed:Drake(192)
        add("drake", rm(), d("drake_bones", 1, 1, 1));
        // Basilisk Knight <- parsed:Basilisk Knight(204)
        add("basilisk_knight", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Red dragon <- parsed:Red dragon(152)
        add("red_dragon", rm(), d("dragon_javelin_heads", 10, 10, 13));
        // Black dragon <- parsed:Black dragon(227)
        add("black_dragon", rm(), d("dragon_javelin_heads", 10, 10, 21));
        // Brutal green dragon <- parsed:Brutal green dragon(227)
        add("brutal_green_dragon", rm(), d("dragon_javelin_heads", 12, 12, 13));
        // Brutal blue dragon <- parsed:Brutal blue dragon(271)
        add("brutal_blue_dragon", rm(), d("dragon_javelin_heads", 12, 12, 128));
        // Brutal red dragon <- parsed:Brutal red dragon(289)
        add("brutal_red_dragon", rm(), d("dragon_javelin_heads", 25, 25, 64));
        // Brutal black dragon <- parsed:Brutal black dragon(318)
        add("brutal_black_dragon", rm(), d("dragon_javelin_heads", 40, 40, 64));
        // Lava dragon <- parsed:Lava dragon(252)
        add("lava_dragon", rm(), d("dragon_javelin_heads", 15, 15, 18));
        // Steel dragon <- parsed:Steel dragon(246)
        add("steel_dragon", rm(), d("dragon_javelin_heads", 12, 12, 26));
        // Mithril dragon <- parsed:Mithril dragon(304)
        add("mithril_dragon", rm(), d("dragon_javelin_heads", 15, 15, 18));
        // Adamant dragon <- parsed:Adamant dragon(338)
        add("adamant_dragon", rm(), d("adamant_javelin_heads", 40, 50, 14), d("dragon_javelin_heads", 20, 30, 16));
        // Rune dragon <- parsed:Rune dragon(380)
        add("rune_dragon", rm(), d("rune_javelin_heads", 20, 30, 8), d("dragon_javelin_heads", 30, 40, 25));
        // Sourhog <- parsed:Sourhog(37)
        add("sourhog", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Shadow warrior <- parsed:Shadow warrior(48)
        add("shadow_warrior", rm(), d("black_robe", 1, 1, 128));
        // Mammoth <- parsed:Mammoth(80)
        add("mammoth", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Ogre <- parsed:Ogre(63)
        add("ogre_lvl63", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Tortured gorilla <- parsed:Tortured gorilla(141)
        add("tortured_gorilla", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Crushing hand <- parsed:Crushing hand(45)
        add("crushing_hand", rm(), d("purple_gloves", 1, 1, 64), d("yellow_gloves", 1, 1, 64), d("red_gloves", 1, 1, 64),
                d("teal_gloves", 1, 1, 64));
        // Cave abomination <- parsed:Cave abomination(206)
        add("cave_abomination", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Basilisk Sentinel <- parsed:Basilisk Sentinel(358)
        add("basilisk_sentinel", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Shadow Wyrm <- parsed:Shadow Wyrm(259)
        add("shadow_wyrm", rm("bones"), d("wyrm_bones", 1, 1, 1));
        // Guardian Drake <- parsed:Guardian Drake(376)
        add("guardian_drake", rm(), d("drake_bones", 1, 1, 1));
        // Night beast <- parsed:Night beast(374)
        add("night_beast", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Ogre <- parsed:Ogre(58)
        add("gwd_ogre", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Jogre <- parsed:Jogre(58)
        add("gwd_jogre", rm(), d("jogre_bones", 1, 1, 1));
        // Cyclops <- parsed:Cyclops(76)
        add("gwd_cyclops", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Gorak <- parsed:Gorak(149)
        add("gwd_gorak", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Starlight <- parsed:Starlight(149)
        add("starlight", rm(), d("unicorn_horn", 6, 6, 16));
        // Growler <- parsed:Growler(139)
        add("growler", rm(), d("unicorn_horn", 6, 6, 16));
        // Bree <- parsed:Bree(146)
        add("bree", rm(), d("unicorn_horn", 6, 6, 16));
        // Blood reaver <- parsed:Blood Reaver(161)
        add("blood_reaver", rm(), d("ancient_ceremonial_mask", 1, 1, 640), d("ancient_ceremonial_top", 1, 1, 640),
                d("ancient_ceremonial_legs", 1, 1, 640), d("ancient_ceremonial_gloves", 1, 1, 640),
                d("ancient_ceremonial_boots", 1, 1, 640));
        // Cyclops <- parsed:Cyclops(56)
        add("warriors_guild_cyclops", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Cyclops <- parsed:Cyclops(106)
        add("cyclops_elite", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Dad <- parsed:Dad(101)
        add("nmz_dad", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Glod <- parsed:Glod(138)
        add("nmz_glod", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Forgotten Soul <- parsed:Forgotten Soul(20)
        add("forgotten_soul", rm(), d("black_robe", 1, 1, 128));
        // Nail beast <- parsed:Nail beast(69)
        add("trek_nail_beast_1", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Nail beast <- parsed:Nail beast(98)
        add("trek_nail_beast_2", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Nail beast <- parsed:Nail beast(141)
        add("trek_nail_beast_3", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Dark wizard <- parsed:Dark wizard(11)
        add("dark_wizard_lvl11", rm(), d("black_robe", 1, 1, 43));
        // Dark wizard <- parsed:Dark wizard(23)
        add("dark_wizard_lvl23", rm(), d("black_robe", 1, 1, 43));
        // King sand crab <- parsed:King Sand Crab(107)
        add("king_sand_crab", rm(), d("rock_shell_gloves", 1, 1, 64), d("rock_shell_boots", 1, 1, 64));
        // Fire giant <- parsed:Fire giant(86)
        add("catacomb_fire_giant", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Moss giant <- parsed:Moss giant(42)
        add("catacomb_moss_giant", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Hill giant <- parsed:Hill Giant(28)
        add("catacomb_hill_giant", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Dagannoth <- parsed:Dagannoth(74)
        add("catacomb_dagannoth", rm(), d("oyster_pearls", 1, 1, 128));
        // Ankou <- parsed:Ankou(75)
        add("catacomb_ankou", rm(), d("black_robe", 1, 1, 100));
        // Maniacal monkey <- parsed:Maniacal monkey(140)
        add("maniacal_monkey", rm("bones"), d("small_ninja_monkey_bones", 1, 1, 1));
        // Maniacal Monkey Archer <- parsed:Maniacal Monkey Archer(132)
        add("maniacal_monkey_archer", rm("bones"), d("small_ninja_monkey_bones", 1, 1, 1));
        // Giant Rock Crab <- parsed:Giant Rock Crab(137)
        add("giant_rock_crab", rm(), d("rock_shell_gloves", 1, 1, 64), d("rock_shell_boots", 1, 1, 64));
        // Dagannoth <- parsed:Dagannoth(92)
        add("dagannoth_lvl92", rm(), d("oyster_pearls", 1, 1, 128));
        // Ancient wyvern <- parsed:Ancient Wyvern(210)
        add("ancient_wyvern", rm(), d("rune_crossbow_u", 1, 1, 44));
        // Monkey Archer <- parsed:Monkey Archer(86)
        add("monkey_archer", rm("bones"), d("small_ninja_monkey_bones", 1, 1, 1));
        // Suqah <- parsed:Suqah(111)
        add("suqah", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Wallasalki <- parsed:Wallasalki(98)
        add("wallasalki", rm(), d("skeletal_gloves", 1, 1, 64), d("skeletal_boots", 1, 1, 64));
        // Jogre <- parsed:Jogre(53)
        add("jogre", rm(), d("jogre_bones", 1, 1, 1));
        // Ogress Warrior <- parsed:Ogress Warrior(82)
        add("ogress_warrior", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Ogress Shaman <- parsed:Ogress Shaman(82)
        add("ogress_shaman", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Dark wizard <- parsed:Dark wizard(7)
        add("Monster.DARK_WIZARD", rm(), d("black_robe", 1, 1, 43));
        // Hill Giant <- parsed:Hill Giant(28)
        add("Monster.HILL_GIANT", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Moss giant <- parsed:Moss giant(42)
        add("Monster.MOSS_GIANT", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Ice giant <- parsed:Ice giant(53)
        add("Monster.ICE_GIANT", rm(), d("long_bone", 1, 1, 400), d("curved_bone", 1, 1, 5012));
        // Dark wizard <- parsed:Dark wizard(20)
        add("Monster.WITCH", rm(), d("black_robe", 1, 1, 43));
        // Monkey Zombie: the small and large versions drop small or large zombie monkey bones (approx: by level).
        add("monkey_zombie_lvl82", rm("bones"), d("small_zombie_monkey_bones", 1, 1, 1));
        add("monkey_zombie", rm("bones"), d("small_zombie_monkey_bones", 1, 1, 1));
        add("monkey_zombie_lvl129", rm("big_bones"), d("large_zombie_monkey_bones", 1, 1, 1));
        // Monkey Guard: gorilla or bearded gorilla bones by appearance (approx: always plain gorilla bones).
        add("monkey_guard", rm("big_bones", "bones"), d("gorilla_bones", 1, 1, 1));
    }

    /** Adds lines (and removals) to the patch for {@code key}, creating it when there is none. */
    /**
     * Lines for items added after the sweep (round 17): Fremennik armour materials (dagannoth, circular, flattened and
     * stretched hide; rock-shell chunk/shard/splinter; skull, ribcage and fibula pieces), edible seaweed, ugthanki kebabs,
     * the stuffable Slayer heads, Xerician fabric and Yama's contracts, plus vanilla stand-ins for OSRS items RuneCraft
     * maps onto Minecraft ones (feather, grain = wheat, raw chicken/beef, raw bear and rat meat = beef (approx), cooked
     * meat = cooked beef, egg, bucket, bucket of water, shears). Generated from the same parsed-osrs snapshot.
     */
    private static void rows11() {
        // Dagannoth (Waterbirth Island) Level 88 <- wiki "Drops (Level 88)": the spined armour hides
        add("dagannoth_lvl88", rm(), d("circular_hide", 1, 1, 64), d("flattened_hide", 1, 1, 64),
                d("stretched_hide", 1, 1, 64), d("edible_seaweed", 1, 5, 16));
        // Dagannoth (Waterbirth Island) Level 90 <- wiki "Drops (Level 90)": 10/128 dagannoth hide
        add("dagannoth_lvl90", rm(), d("dagannoth_hide", 1, 1, 13));
        // Gourmet impling <- parsed:Gourmet impling(0)
        add("gourmet_impling", rm(), d("ugthanki_kebab", 1, 1, 100));
        // Ninja impling <- parsed:Ninja impling(0)
        add("ninja_impling", rm(), d("dagannoth_hide", 3, 3, 19));
        // Grizzly bear <- parsed:Grizzly bear(21)
        add("grizzly_bear", rm(), d("minecraft:beef", 1, 1, 1));
        // Minotaur <- parsed:Minotaur(27)
        add("minotaur", rm(), d("minecraft:cooked_beef", 1, 1, 34));
        // Crawling Hand <- parsed:Crawling Hand(8)
        add("crawling_hand", rm(), d("crawling_hand", 1, 1, 500));
        // Cockatrice <- parsed:Cockatrice(37)
        add("cockatrice", rm(), d("cockatrice_head", 1, 1, 1000));
        // Basilisk <- parsed:Basilisk(61)
        add("basilisk", rm(), d("basilisk_head", 1, 1, 2000));
        // Sea Snake Young <- parsed:Sea Snake Young(90)
        add("sea_snake_young", rm(), d("edible_seaweed", 5, 5, 64));
        // Brine rat <- parsed:Brine rat(70)
        add("brine_rat", rm(), d("minecraft:beef", 1, 1, 1), d("minecraft:beef", 18, 18, 64));
        // Cave horror <- parsed:Cave horror(80)
        add("cave_horror", rm(), d("black_mask", 1, 1, 512));
        // Dust devil <- parsed:Dust devil(93)
        add("dust_devil", rm(), d("ugthanki_kebab", 4, 4, 64));
        // Dagannoth <- parsed:Dagannoth(74)
        add("dagannoth", rm(), d("minecraft:feather", 15, 15, 64));
        // Lizardman <- parsed:Lizardman(62)
        add("lizardman", rm(), d("xerician_fabric", 1, 1, 14), d("xerician_fabric", 1, 1, 10));
        // Lizardman shaman <- parsed:Lizardman shaman(150)
        add("lizardman_shaman", rm(), d("xerician_fabric", 2, 2, 12));
        // Kurask <- parsed:Kurask(106)
        add("kurask", rm(), d("kurask_head", 1, 1, 3000));
        // Cave kraken <- parsed:Cave kraken(127)
        add("cave_kraken", rm(), d("minecraft:bucket", 1, 1, 240));
        // Smoke devil <- parsed:Smoke devil(160)
        add("smoke_devil", rm(), d("ugthanki_kebab", 3, 3, 64));
        // Basilisk Knight <- parsed:Basilisk Knight(204)
        add("basilisk_knight", rm(), d("basilisk_head", 1, 1, 1000));
        // Skeleton <- parsed:Skeleton(25)
        add("skeleton_lvl25", rm(), d("minecraft:wheat", 1, 1, 128));
        // Black bear <- parsed:Black bear(19)
        add("black_bear", rm(), d("minecraft:beef", 1, 1, 1));
        // Bear Cub <- parsed:Bear Cub(15)
        add("bear_cub", rm(), d("minecraft:beef", 1, 1, 1));
        // Grizzly bear cub <- parsed:Grizzly bear cub(36)
        add("grizzly_bear_cub", rm(), d("minecraft:beef", 1, 1, 1));
        // Undead chicken <- parsed:Undead chicken(1)
        add("undead_chicken", rm(), d("minecraft:feather", 5, 5, 2), d("minecraft:feather", 15, 15, 4));
        // Black Guard <- parsed:Black Guard(25)
        add("black_guard", rm(), d("minecraft:water_bucket", 1, 1, 128));
        // Crushing hand <- parsed:Crushing hand(45)
        add("crushing_hand", rm(), d("crawling_hand", 1, 1, 500));
        // Cockathrice <- parsed:Cockathrice(89)
        add("cockathrice", rm(), d("cockatrice_head", 1, 1, 1000));
        // Monstrous basilisk <- parsed:Monstrous basilisk(135)
        add("monstrous_basilisk", rm(), d("basilisk_head", 1, 1, 2000));
        // Cave abomination <- parsed:Cave abomination(206)
        add("cave_abomination", rm(), d("black_mask", 1, 1, 512));
        // Basilisk Sentinel <- parsed:Basilisk Sentinel(358)
        add("basilisk_sentinel", rm(), d("basilisk_head", 1, 1, 1000));
        // Choke devil <- parsed:Choke devil(264)
        add("choke_devil", rm(), d("ugthanki_kebab", 4, 4, 64));
        // King kurask <- parsed:King kurask(295)
        add("king_kurask", rm(), d("kurask_head", 1, 1, 3000));
        // Nuclear smoke devil <- parsed:Nuclear smoke devil(280)
        add("nuclear_smoke_devil", rm(), d("ugthanki_kebab", 3, 3, 64));
        // Aviansie <- parsed:Aviansie(79)
        add("gwd_aviansie", rm(), d("minecraft:feather", 1, 6, 1));
        // Aviansie <- parsed:Aviansie(131)
        add("gwd_aviansie_elite", rm(), d("minecraft:feather", 1, 6, 1));
        // Imp <- parsed:Imp(7)
        add("gwd_imp", rm(), d("minecraft:egg", 1, 1, 26), d("minecraft:chicken", 1, 1, 26), d("minecraft:cooked_beef", 1, 1, 128), d("minecraft:shears", 1, 1, 32), d("minecraft:bucket", 1, 1, 32), d("minecraft:water_bucket", 1, 1, 64), d("minecraft:wheat", 1, 1, 43));
        // Werewolf <- parsed:Werewolf(93)
        add("gwd_werewolf", rm(), d("minecraft:chicken", 5, 5, 51), d("minecraft:beef", 5, 5, 51));
        // Flight Kilisa <- parsed:Flight Kilisa(159)
        add("flight_kilisa", rm(), d("minecraft:feather", 1, 11, 1));
        // Wingman Skree <- parsed:Wingman Skree(143)
        add("wingman_skree", rm(), d("minecraft:feather", 1, 11, 1));
        // Flockleader Geerin <- parsed:Flockleader Geerin(149)
        add("flockleader_geerin", rm(), d("minecraft:feather", 1, 11, 1));
        // White Knight <- parsed:White Knight(36)
        add("white_knight", rm(), d("half_a_apple_pie", 1, 1, 128));
        // Rock crab <- parsed:Rock Crab(13)
        add("rock_crab", rm(), d("edible_seaweed", 2, 2, 64));
        // Imp <- parsed:Imp(2)
        add("imp", rm(), d("minecraft:egg", 1, 1, 26), d("minecraft:chicken", 1, 1, 26), d("minecraft:cooked_beef", 1, 1, 128), d("minecraft:shears", 1, 1, 32), d("minecraft:bucket", 1, 1, 32), d("minecraft:water_bucket", 1, 1, 64), d("minecraft:wheat", 1, 1, 43));
        // Sand crab <- parsed:Sand Crab(15)
        add("sand_crab", rm(), d("edible_seaweed", 2, 2, 64));
        // King sand crab <- parsed:King Sand Crab(107)
        add("king_sand_crab", rm(), d("rock_shell_splinter", 1, 1, 64), d("rock_shell_chunk", 1, 1, 64), d("rock_shell_shard", 1, 1, 64));
        // Lizardman brute <- parsed:Lizardman brute(75)
        add("lizardman_brute", rm(), d("xerician_fabric", 1, 1, 8));
        // Greater demon <- parsed:Greater demon(92)
        add("catacomb_greater_demon", rm(), d("contract_of_glyphic_attenuation", 1, 1, 81), d("contract_of_bloodied_blows", 1, 1, 81), d("contract_of_divine_severance", 1, 1, 81), d("contract_of_forfeit_breath", 1, 1, 81), d("contract_of_sensory_clouding", 1, 1, 81));
        // Black demon <- parsed:Black demon(172)
        add("catacomb_black_demon", rm(), d("contract_of_glyphic_attenuation", 1, 1, 53), d("contract_of_bloodied_blows", 1, 1, 53), d("contract_of_divine_severance", 1, 1, 53), d("contract_of_forfeit_breath", 1, 1, 53), d("contract_of_sensory_clouding", 1, 1, 53));
        // Dagannoth <- parsed:Dagannoth(74)
        add("catacomb_dagannoth", rm(), d("minecraft:feather", 15, 15, 64));
        // Dust devil <- parsed:Dust devil(93)
        add("catacomb_dust_devil", rm(), d("ugthanki_kebab", 4, 4, 64));
        // Giant rat <- parsed:Giant rat(6)
        add("giant_rat", rm(), d("minecraft:beef", 1, 1, 1));
        // Dagannoth spawn <- parsed:Dagannoth spawn(42)
        add("dagannoth_spawn", rm(), d("minecraft:feather", 2, 2, 3));
        // Giant Rock Crab <- parsed:Giant Rock Crab(137)
        add("giant_rock_crab", rm(), d("rock_shell_splinter", 1, 1, 64), d("rock_shell_chunk", 1, 1, 64), d("rock_shell_shard", 1, 1, 64));
        // Dagannoth <- parsed:Dagannoth(92)
        add("dagannoth_lvl92", rm(), d("minecraft:feather", 15, 15, 64));
        // Ammonite crab <- parsed:Ammonite Crab(25)
        add("ammonite_crab", rm(), d("edible_seaweed", 2, 2, 64));
        // Wallasalki <- parsed:Wallasalki(98)
        add("wallasalki", rm(), d("fibula_piece", 1, 1, 64), d("ribcage_piece", 1, 1, 64), d("skull_piece", 1, 1, 64));
        // Rock lobster <- parsed:Rock lobster(127)
        add("rock_lobster", rm(), d("edible_seaweed", 1, 3, 13));
    }

    private static void rows12() {
        // Round 27: the slayer monsters that dropped nothing and drop items that were missing (parsed-osrs all-monsters.json).
        // Crypt spiders, giant crypt spiders, bloodworms, death wings and the grave/poison/pit scorpions drop nothing in
        // OSRS either (only ensouled heads); Konar's brimstone keys come from SlayerService.
        // Vampyre Juvenile <- parsed:Vampyre Juvenile(45)
        add("vampyre_juvenile", rm(), d("vampyre_dust", 1, 1, 1));
        // Zogre / Skogre <- parsed:Zogre(44), Skogre(44)
        add("zogre", rm(), d("zogre_bones", 1, 1, 1), d("ogre_coffin_key", 1, 1, 1));
        add("skogre", rm(), d("zogre_bones", 1, 1, 1), d("ogre_coffin_key", 1, 1, 1));
        // Wyrmling <- parsed:Wyrmling(55)
        add("wyrmling", rm(), d("wyrmling_bones", 1, 1, 1));
        // Cave lizard <- parsed:Cave lizard(37)
        add("cave_lizard", rm(), d("bones", 1, 1, 1));
        // Vampyres <- parsed:Feral Vampyre(61), Vyrewatch Sentinel(151), Vampyre Juvinate(59/90/119): always vampyre dust
        add("feral_vampyre", rm(), d("vampyre_dust", 1, 1, 1));
        add("vyrewatch_sentinel", rm(), d("vampyre_dust", 1, 1, 1));
        add("trek_vampyre_juvinate_1", rm(), d("vampyre_dust", 1, 1, 1));
        add("trek_vampyre_juvinate_2", rm(), d("vampyre_dust", 1, 1, 1));
        add("trek_vampyre_juvinate_3", rm(), d("vampyre_dust", 1, 1, 1));
        // Araxyte <- parsed:Araxyte(96/146): one venom sac always, 2 or 4 at 5/127 each
        add("araxyte", rm(), d("araxyte_venom_sac", 1, 1, 1), d("araxyte_venom_sac", 2, 2, 25), d("araxyte_venom_sac", 4, 4, 25));
        add("araxyte_lvl146", rm(), d("araxyte_venom_sac", 1, 1, 1), d("araxyte_venom_sac", 2, 2, 25),
                d("araxyte_venom_sac", 4, 4, 25));
        // Nagua <- parsed:Earthen Nagua(128) 2/32, Frost Nagua(104) 1/34
        add("earthen_nagua", rm(), d("blessed_bone_shards", 20, 40, 16));
        add("frost_nagua", rm(), d("blessed_bone_shards", 30, 50, 34));
        // Blue dragons <- parsed:Blue dragon(111) 1/50, Baby blue dragon(48) 1/200, Brutal blue dragon(271) 1/33
        add("blue_dragon", rm(), d("scaly_blue_dragonhide", 1, 1, 50));
        add("baby_blue_dragon", rm(), d("scaly_blue_dragonhide", 1, 1, 200));
        add("brutal_blue_dragon", rm(), d("scaly_blue_dragonhide", 1, 1, 33));
        // Elves (Lletya/Prifddinas; the Iorwerth Camp elves don't drop it) <- parsed:Elf Warrior(108), Elf Archer(90) 25/128
        add("elf_warrior", rm(), d("crystal_teleport_seed", 1, 1, 5));
        add("elf_archer", rm(), d("crystal_teleport_seed", 1, 1, 5));
        // Prifddinas guards <- parsed:Guard (Prifddinas) crystal teleport seed 25/128, enhanced crystal teleport seed 1/1024
        add("prifddinas_guard", rm(), d("crystal_teleport_seed", 1, 1, 5), d("enhanced_crystal_teleport_seed", 1, 1, 1024));
    }

    private static void add(String key, String[] remove, Drop... put) {
        Patch old = PATCHES.get(key);
        List<Drop> lines = new ArrayList<>(old == null ? List.of() : old.put());
        lines.addAll(List.of(put));
        Set<String> removed = new LinkedHashSet<>(old == null ? Set.of() : old.remove());
        removed.addAll(List.of(remove));
        PATCHES.put(key, new Patch(List.copyOf(lines), Set.copyOf(removed)));
    }
}
