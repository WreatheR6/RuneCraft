package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hunter: implings (caught with a butterfly net into impling jars, looted for their OSRS-style loot
 * tables), salamanders (net traps), deadfall kebbits/pyre foxes, pitfall larupias/graahks/kyatts and
 * the low-level kebbits and weasels tracked down with a noose wand (polar, common, Feldip weasel,
 * desert devil, razor-backed), butterflies caught with a butterfly net into butterfly jars, falconry
 * kebbits at Piscatoris, and the Hunter gear sold at the Varrock fancy dress shop (camouflage outfits,
 * larupia/graahk/kyatt sets, spotted and spottier capes).
 * Levels and XP are OSRS values (implings award the open-world XP; {@code puroXp} is Puro-Puro's).
 * Catch chances: {@link com.runecraft.osrs.data.HunterRates}.
 */
public final class HunterData extends SkillingPart {

    /**
     * An impling: level, open-world XP, Puro-Puro XP (0 = not found there), region weights and loot (one
     * roll per jar).
     */
    public record Impling(String id, String name, int level, double xp, double puroXp, int color, List<Region> regions, List<Loot> loot) {
        public String jar() {
            return id + "_jar";
        }
    }

    public static final List<Impling> IMPLINGS = List.of(
            new Impling("baby_impling", "Baby impling", 17, 18.0, 20.0, 0xC0A060, List.of(Region.MISTHALIN, Region.ASGARNIA, Region.KANDARIN),
                    List.of(Loot.of("chisel", 10), Loot.of("minecraft:string", 10), Loot.of("needle", 10), Loot.of("knife", 10),
                            Loot.of("cheese", 10), Loot.of("hammer", 10), Loot.of("ball_of_wool", 10), Loot.of("raw_anchovies", 10),
                            Loot.of("spice", 5), Loot.of("flax", 10), Loot.of("seaweed", 5), Loot.of("air_talisman", 5),
                            Loot.of("silver_bar", 3), Loot.of("sapphire", 3), Loot.of("hard_leather", 4), Loot.of("raw_lobster", 3),
                            Loot.of("soft_clay", 5))),
            new Impling("young_impling", "Young impling", 22, 20.0, 22.0, 0xA0B060, List.of(Region.MISTHALIN, Region.ASGARNIA, Region.KANDARIN),
                    List.of(Loot.of("steel_nails", 5, 5, 10), Loot.of("pure_essence", 10), Loot.of("raw_tuna", 10),
                            Loot.of("chocolate_slice", 10), Loot.of("steel_axe", 6), Loot.of("meat_pizza", 6), Loot.of("coal", 10),
                            Loot.of("bow_string", 10), Loot.of("snape_grass", 6), Loot.of("soft_clay", 10), Loot.of("studded_chaps", 4),
                            Loot.of("steel_full_helm", 4), Loot.of("oak_plank", 6), Loot.of("defence_potion", 4),
                            Loot.of("mithril_bar", 4), Loot.of("garden_pie", 5), Loot.of("jangerberries", 5))),
            new Impling("gourmet_impling", "Gourmet impling", 28, 22.0, 24.0, 0xE0A060, List.of(Region.KANDARIN, Region.KARAMJA, Region.MISTHALIN),
                    List.of(Loot.of("tuna", 12), Loot.of("bass", 10), Loot.of("curry", 6), Loot.of("meat_pie", 10),
                            Loot.of("chocolate_cake", 8), Loot.of("spice", 6), Loot.of("curry_leaf", 8), Loot.of("lobster", 10),
                            Loot.of("shark", 4), Loot.of("fish_pie", 6), Loot.of("garden_pie", 8), Loot.of("swordfish", 6),
                            Loot.of("strawberry", 8), Loot.of("sea_turtle", 2), Loot.of("cooked_karambwan", 4))),
            new Impling("earth_impling", "Earth impling", 36, 25.0, 27.0, 0x8A5A2A, List.of(Region.ASGARNIA, Region.FREMENNIK_PROVINCE, Region.WILDERNESS),
                    List.of(Loot.of("fire_talisman", 8), Loot.of("earth_talisman", 10), Loot.of("earth_tiara", 4),
                            Loot.of("earth_rune", 32, 32, 12), Loot.of("mithril_ore", 10), Loot.of("supercompost", 6),
                            Loot.of("compost", 8), Loot.of("gold_ore", 8), Loot.of("unicorn_horn_dust", 6), Loot.of("emerald", 4),
                            Loot.of("uncut_emerald", 6), Loot.of("wildblood_seed", 4), Loot.of("jangerberry_seed", 4),
                            Loot.of("harralander_seed", 4), Loot.of("coal", 6))),
            new Impling("essence_impling", "Essence impling", 42, 27.0, 29.0, 0xD0D0E0, List.of(Region.MISTHALIN, Region.WILDERNESS, Region.KHARIDIAN_DESERT),
                    List.of(Loot.of("pure_essence", 20, 20, 15), Loot.of("water_rune", 30, 30, 10), Loot.of("air_rune", 30, 30, 10),
                            Loot.of("fire_rune", 50, 50, 10), Loot.of("mind_rune", 25, 25, 10), Loot.of("body_rune", 28, 28, 8),
                            Loot.of("cosmic_rune", 4, 4, 6), Loot.of("chaos_rune", 4, 4, 6), Loot.of("nature_rune", 13, 13, 5),
                            Loot.of("law_rune", 13, 13, 5), Loot.of("death_rune", 13, 13, 4), Loot.of("blood_rune", 7, 7, 3))),
            new Impling("eclectic_impling", "Eclectic impling", 50, 30.0, 32.0, 0x6A9AA0, List.of(Region.KANDARIN, Region.TIRANNWN, Region.WILDERNESS),
                    List.of(Loot.of("mithril_pickaxe", 8), Loot.of("curry_leaf", 8), Loot.of("snape_grass", 8), Loot.of("oak_plank", 8),
                            Loot.of("empty_candle_lantern", 6), Loot.of("gold_ore", 8), Loot.of("gold_bar", 6),
                            Loot.of("unicorn_horn_dust", 6), Loot.of("adamant_kiteshield", 4), Loot.of("blue_dhide_chaps", 4),
                            Loot.of("red_spiders_eggs", 6), Loot.of("rune_dagger", 3), Loot.of("battlestaff", 3),
                            Loot.of("adamantite_ore", 6), Loot.of("wild_pie", 3), Loot.of("watermelon_seed", 4), Loot.of("diamond", 2))),
            new Impling("nature_impling", "Nature impling", 58, 34.0, 34.0, 0x3AA03A, List.of(Region.KANDARIN, Region.FELDIP_HILLS, Region.TIRANNWN),
                    List.of(Loot.of("jangerberry_seed", 8), Loot.of("belladonna_seed", 6), Loot.of("harralander_seed", 8),
                            Loot.of("cactus_spine", 8), Loot.of("magic_logs", 6), Loot.of("tarromin", 8), Loot.of("coconut", 8),
                            Loot.of("irit_seed", 6), Loot.of("curry_seed", 4), Loot.of("orange_seed", 4), Loot.of("snapdragon", 4),
                            Loot.of("kwuarm_seed", 5), Loot.of("avantoe_seed", 5), Loot.of("willow_seed", 6), Loot.of("torstol_seed", 1),
                            Loot.of("ranarr_seed", 2), Loot.of("torstol", 2), Loot.of("dwarf_weed_seed", 3))),
            new Impling("magpie_impling", "Magpie impling", 65, 44.0, 216.0, 0x2A2A3A, List.of(Region.KARAMJA, Region.KANDARIN, Region.WILDERNESS),
                    List.of(Loot.of("black_dragonhide", 6, 6, 8), Loot.of("diamond_amulet_u", 6), Loot.of("runite_bar", 6),
                            Loot.of("dragonstone_bolt_tips", 12, 12, 6), Loot.of("uncut_dragonstone", 4), Loot.of("rune_sq_shield", 6),
                            Loot.of("rune_med_helm", 6), Loot.of("diamond_bracelet", 6), Loot.of("rune_bolts", 10, 10, 8),
                            Loot.of("nature_rune", 20, 20, 6), Loot.of("dragonstone_ring", 3), Loot.of("onyx_bolt_tips", 6, 6, 2))),
            new Impling("ninja_impling", "Ninja impling", 74, 52.0, 240.0, 0x1A1A1A, List.of(Region.KHARIDIAN_DESERT, Region.MORYTANIA, Region.WILDERNESS),
                    List.of(Loot.of("snakeskin_boots", 8), Loot.of("rune_dart", 70, 70, 6), Loot.of("rune_arrow", 70, 70, 6),
                            Loot.of("rune_dagger", 6), Loot.of("rune_scimitar", 5), Loot.of("rune_knife", 40, 40, 6),
                            Loot.of("onyx_bolt_tips", 4, 4, 4), Loot.of("dragon_dagger", 2), Loot.of("prayer_potion", 6),
                            Loot.of("black_dragonhide", 4, 4, 6), Loot.of("rune_crossbow", 2), Loot.of("weapon_poison", 6))),
            // Prifddinas only (no Puro-Puro spawn); loot approx.
            new Impling("crystal_impling", "Crystal impling", 80, 280.0, 0, 0x9AD8F0, List.of(Region.TIRANNWN),
                    List.of(Loot.of("crystal_shard", 1, 5, 30), Loot.of("rune_arrow", 25, 25, 8), Loot.of("amulet_of_power", 8),
                            Loot.of("dragonstone_bolt_tips", 10, 10, 8), Loot.of("uncut_dragonstone", 6), Loot.of("rune_pickaxe", 6),
                            Loot.of("rune_javelin", 20, 20, 6), Loot.of("dragonstone_amulet", 4), Loot.of("magic_seed", 2),
                            Loot.of("dragon_dart_tip", 25, 25, 4))),
            new Impling("dragon_impling", "Dragon impling", 83, 65.0, 300.0, 0xC02020, List.of(Region.WILDERNESS, Region.KARAMJA, Region.MORYTANIA),
                    List.of(Loot.of("dragon_arrowtips", 30, 60, 10), Loot.of("dragonstone_bolt_tips", 10, 30, 10),
                            Loot.of("dragon_limbs", 2), Loot.of("babydragon_bones", 25, 35, 8), Loot.of("dragon_bones", 10, 15, 10),
                            Loot.of("magic_seed", 3), Loot.of("snapdragon_seed", 6), Loot.of("uncut_dragonstone", 4),
                            Loot.of("dragon_dagger", 4), Loot.of("onyx_bolt_tips", 5, 10, 4), Loot.of("dragon_arrow", 100, 250, 4),
                            Loot.of("dragon_dart_tip", 25, 35, 8))),
            new Impling("lucky_impling", "Lucky impling", 89, 80.0, 380.0, 0x30C060, List.of(Region.WILDERNESS, Region.KANDARIN, Region.TIRANNWN),
                    List.of(Loot.of("clue_scroll_easy", 30), Loot.of("clue_scroll_medium", 25), Loot.of("clue_scroll_hard", 20),
                            Loot.of("clue_scroll_elite", 10), Loot.of("clue_scroll_master", 2), Loot.of("coins", 5000, 30000, 10),
                            Loot.of("uncut_onyx", 1))));

    public static Impling impling(String id) {
        for (Impling i : IMPLINGS) {
            if (i.id().equals(id)) {
                return i;
            }
        }
        return null;
    }

    public static Impling implingByJar(String jar) {
        for (Impling i : IMPLINGS) {
            if (i.jar().equals(jar)) {
                return i;
            }
        }
        return null;
    }

    /**
     * NET, DEADFALL and PITFALL traps are laid and checked later (they count towards the trap limit);
     * TRACKING (noose wand), BUTTERFLY (butterfly net, one butterfly jar per catch) and FALCONRY (a
     * falcon lent by Matthias) are immediate catches.
     */
    public enum TrapKind { NET, DEADFALL, PITFALL, TRACKING, BUTTERFLY, FALCONRY;

        /** True for traps that are laid and count towards the trap limit. */
        public boolean laid() {
            return this == NET || this == DEADFALL || this == PITFALL;
        }
    }

    /**
     * A hunting spot (scripted block placed at the creature's real location). {@code bait} is consumed
     * when laying the trap (logs for deadfalls/pitfalls, a butterfly jar per butterfly); {@code tools}
     * must be in the inventory. {@code rareLoot} is added to one catch in {@code rareOneIn} (0 = never).
     */
    public record TrapSpot(String blockId, String creature, TrapKind kind, int level, double xp, List<String> tools, String bait,
                           List<String> loot, int catchTicks, String rareLoot, int rareOneIn) {
        public TrapSpot(String blockId, String creature, TrapKind kind, int level, double xp, List<String> tools, String bait,
                        List<String> loot, int catchTicks) {
            this(blockId, creature, kind, level, xp, tools, bait, loot, catchTicks, null, 0);
        }
    }

    public static final List<TrapSpot> TRAPS = List.of(
            new TrapSpot("swamp_lizard_tree", "Swamp lizard", TrapKind.NET, 29, 152.0, List.of("small_fishing_net", "rope"), null,
                    List.of("swamp_lizard"), 25),
            new TrapSpot("orange_salamander_tree", "Orange salamander", TrapKind.NET, 47, 224.0, List.of("small_fishing_net", "rope"), null,
                    List.of("orange_salamander"), 25),
            new TrapSpot("red_salamander_tree", "Red salamander", TrapKind.NET, 59, 272.0, List.of("small_fishing_net", "rope"), null,
                    List.of("red_salamander"), 25),
            new TrapSpot("black_salamander_tree", "Black salamander", TrapKind.NET, 67, 319.2, List.of("small_fishing_net", "rope"), null,
                    List.of("black_salamander"), 25),
            new TrapSpot("tecu_salamander_tree", "Tecu salamander", TrapKind.NET, 79, 344.0, List.of("small_fishing_net", "rope"), null,
                    List.of("tecu_salamander"), 25),
            // Tracking (noose wand): follow the tracks to the burrow and catch the creature.
            new TrapSpot("polar_kebbit_burrow", "Polar kebbit", TrapKind.TRACKING, 1, 30.0, List.of("noose_wand"), null,
                    List.of("bones", "polar_kebbit_fur"), 0),
            new TrapSpot("common_kebbit_burrow", "Common kebbit", TrapKind.TRACKING, 3, 36.0, List.of("noose_wand"), null,
                    List.of("bones", "common_kebbit_fur"), 0),
            new TrapSpot("feldip_weasel_burrow", "Feldip weasel", TrapKind.TRACKING, 7, 48.0, List.of("noose_wand"), null,
                    List.of("bones", "feldip_weasel_fur"), 0),
            new TrapSpot("desert_devil_burrow", "Desert devil", TrapKind.TRACKING, 13, 66.0, List.of("noose_wand"), null,
                    List.of("bones", "desert_devil_fur"), 0),
            new TrapSpot("razor_backed_kebbit_burrow", "Razor-backed kebbit", TrapKind.TRACKING, 49, 348.0, List.of("noose_wand"), null,
                    List.of("bones", "long_kebbit_spike"), 0),
            new TrapSpot("wild_kebbit_deadfall", "Wild kebbit", TrapKind.DEADFALL, 23, 128.0, List.of("knife"), "logs",
                    List.of("bones", "raw_wild_kebbit"), 20),
            new TrapSpot("barb_tailed_kebbit_deadfall", "Barb-tailed kebbit", TrapKind.DEADFALL, 33, 168.0, List.of("knife"), "logs",
                    List.of("bones", "raw_barb_tailed_kebbit", "barb_tail_harpoon_part"), 20),
            new TrapSpot("prickly_kebbit_deadfall", "Prickly kebbit", TrapKind.DEADFALL, 37, 204.0, List.of("knife"), "logs",
                    List.of("bones", "kebbit_spike"), 20),
            new TrapSpot("sabre_toothed_kebbit_deadfall", "Sabre-toothed kebbit", TrapKind.DEADFALL, 51, 200.0, List.of("knife"), "logs",
                    List.of("bones", "kebbit_teeth"), 20),
            new TrapSpot("pyre_fox_deadfall", "Pyre fox", TrapKind.DEADFALL, 57, 222.0, List.of("knife"), "logs",
                    List.of("bones", "raw_pyre_fox", "fox_fur"), 20),
            // Pitfalls: the fur is sometimes tatty (rate approx).
            new TrapSpot("spined_larupia_pitfall", "Spined larupia", TrapKind.PITFALL, 31, 180.0, List.of("knife"), "logs",
                    List.of("big_bones", "raw_larupia", "larupia_fur"), 20, "tatty_larupia_fur", 4),
            new TrapSpot("horned_graahk_pitfall", "Horned graahk", TrapKind.PITFALL, 41, 240.0, List.of("knife"), "logs",
                    List.of("big_bones", "raw_graahk", "graahk_fur"), 20, "tatty_graahk_fur", 4),
            new TrapSpot("sabre_toothed_kyatt_pitfall", "Sabre-toothed kyatt", TrapKind.PITFALL, 55, 300.0, List.of("knife"), "logs",
                    List.of("big_bones", "raw_kyatt", "kyatt_fur"), 20, "tatty_kyatt_fur", 4),
            // Butterflies (butterfly net + butterfly jar).
            new TrapSpot("ruby_harvest_bush", "Ruby harvest", TrapKind.BUTTERFLY, 15, 24.0, List.of("butterfly_net"), "butterfly_jar",
                    List.of("ruby_harvest"), 0),
            new TrapSpot("sapphire_glacialis_bush", "Sapphire glacialis", TrapKind.BUTTERFLY, 25, 34.0, List.of("butterfly_net"),
                    "butterfly_jar", List.of("sapphire_glacialis"), 0),
            new TrapSpot("snowy_knight_bush", "Snowy knight", TrapKind.BUTTERFLY, 35, 44.0, List.of("butterfly_net"), "butterfly_jar",
                    List.of("snowy_knight"), 0),
            new TrapSpot("black_warlock_bush", "Black warlock", TrapKind.BUTTERFLY, 45, 54.0, List.of("butterfly_net"), "butterfly_jar",
                    List.of("black_warlock"), 0),
            // Falconry (Piscatoris falconry area; Matthias lends the falcon).
            new TrapSpot("spotted_kebbit_falconry", "Spotted kebbit", TrapKind.FALCONRY, 43, 104.0, List.of(), null,
                    List.of("bones", "spotted_kebbit_fur"), 0),
            new TrapSpot("dark_kebbit_falconry", "Dark kebbit", TrapKind.FALCONRY, 57, 132.0, List.of(), null,
                    List.of("bones", "dark_kebbit_fur"), 0),
            new TrapSpot("dashing_kebbit_falconry", "Dashing kebbit", TrapKind.FALCONRY, 69, 156.0, List.of(), null,
                    List.of("bones", "dashing_kebbit_fur"), 0));

    public static TrapSpot trap(String blockId) {
        for (TrapSpot t : TRAPS) {
            if (t.blockId().equals(blockId)) {
                return t;
            }
        }
        return null;
    }

    /** Hunter meats: raw id, cooked id, cooking level, XP, heal (OSRS). */
    private static final Object[][] MEATS = {
            {"wild_kebbit", "Wild kebbit", 23, 73.0, 4}, {"larupia", "Larupia", 31, 92.0, 6},
            {"barb_tailed_kebbit", "Barb-tailed kebbit", 32, 106.0, 7}, {"graahk", "Graahk", 41, 124.0, 8},
            {"kyatt", "Kyatt", 51, 143.0, 9}, {"pyre_fox", "Pyre fox", 59, 154.0, 11}};

    @Override
    public void items(ItemSink s) {
        tool(s, "butterfly_net", "Butterfly net", "net", 0xE0E0C0, 24);
        simple(s, "impling_jar", "Impling jar", "jar", 0xC0D8E0, 50);
        for (Impling i : IMPLINGS) {
            simple(s, i.jar(), i.name() + " jar", "jar", i.color(), i.level() * 40).examine("An impling is trapped in this jar.");
        }
        Object[][] salamanders = {{"swamp_lizard", "Swamp lizard", 0x6A8A3A, 30, 1}, {"orange_salamander", "Orange salamander", 0xE08030, 50, 3},
                {"red_salamander", "Red salamander", 0xC03020, 60, 5}, {"black_salamander", "Black salamander", 0x2A2A2A, 70, 7},
                {"tecu_salamander", "Tecu salamander", 0x3A6AA0, 80, 9}};
        for (Object[] sal : salamanders) {
            int lvl = (Integer) sal[3];
            int f = (Integer) sal[4];
            s.add((String) sal[0], (String) sal[1], Kind.MELEE_WEAPON, "salamander", (Integer) sal[2]).value(lvl * 20).stack(1)
                    .tier("salamander").equipment(weapon(stats(0, 10 + f * 6, 0, 10 + f * 4, 10 + f * 6, 0, 0, 0, 0, 0,
                            10 + f * 6, 10 + f * 6, 0, 0), 5, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE,
                            Skill.ATTACK, lvl, Skill.RANGED, lvl, Skill.MAGIC, lvl));
        }
        for (Object[] m : MEATS) {
            simple(s, "raw_" + m[0], "Raw " + lower((String) m[1]), "meat_raw", 0xC07060, 10);
            food(s, "cooked_" + m[0], "Cooked " + lower((String) m[1]), "meat_cooked", 0x8A5030, 20, (Integer) m[4]);
        }
        simple(s, "kebbit_spike", "Kebbit spike", "secondary", 0xE0D8C0, 20);
        simple(s, "kebbit_teeth", "Kebbit teeth", "secondary", 0xF0E8D0, 20);
        simple(s, "barb_tail_harpoon_part", "Barb-tail", "secondary", 0x8A6A4A, 10);
        simple(s, "fox_fur", "Fox fur", "fur", 0xD06030, 50);
        simple(s, "polar_kebbit_fur", "Polar kebbit fur", "fur", 0xF0F0F0, 12);
        simple(s, "common_kebbit_fur", "Common kebbit fur", "fur", 0xA08060, 12);
        simple(s, "feldip_weasel_fur", "Feldip weasel fur", "fur", 0x8A6A4A, 12);
        simple(s, "desert_devil_fur", "Desert devil fur", "fur", 0xD0B080, 12);
        simple(s, "long_kebbit_spike", "Long kebbit spike", "secondary", 0xE8E0C8, 40);
        simple(s, "larupia_fur", "Larupia fur", "fur", 0xC09040, 100);
        simple(s, "graahk_fur", "Graahk fur", "fur", 0xA08060, 150);
        simple(s, "kyatt_fur", "Kyatt fur", "fur", 0xE0E0E0, 200);
        simple(s, "tatty_larupia_fur", "Tatty larupia fur", "fur", 0xA08040, 50);
        simple(s, "tatty_graahk_fur", "Tatty graahk fur", "fur", 0x8A7060, 75);
        simple(s, "tatty_kyatt_fur", "Tatty kyatt fur", "fur", 0xC8C8C8, 100);
        simple(s, "spotted_kebbit_fur", "Spotted kebbit fur", "fur", 0xB08A50, 40);
        simple(s, "dark_kebbit_fur", "Dark kebbit fur", "fur", 0x3A2A1A, 50);
        simple(s, "dashing_kebbit_fur", "Dashing kebbit fur", "fur", 0xC06A2A, 60);
        for (String[] b : BUTTERFLIES) {
            simple(s, b[0], b[1], "jar", Integer.parseInt(b[2], 16), 20).examine("A butterfly in a jar.");
        }
        // Fancy dress shop gear (OSRS bonuses: the camouflage outfits have none).
        for (String[] c : CAMO) {
            s.add(c[0] + "_camo_top", title(c[0]) + " camo top", Kind.ARMOUR, "leather_body", Integer.parseInt(c[1], 16)).value(20).stack(1)
                    .equipment(wear(EquipmentDef.Slot.CHEST, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
            s.add(c[0] + "_camo_legs", title(c[0]) + " camo legs", Kind.ARMOUR, "chaps", Integer.parseInt(c[1], 16)).value(20).stack(1)
                    .equipment(wear(EquipmentDef.Slot.LEGS, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        }
        Object[][] sets = {{"larupia", "Larupia", "hat", 0xC09040, 28}, {"graahk", "Graahk", "headdress", 0xA08060, 38},
                {"kyatt", "Kyatt", "hat", 0xE0E0E0, 52}};
        for (Object[] set : sets) {
            String id = (String) set[0];
            int color = (Integer) set[3];
            int lvl = (Integer) set[4];
            s.add(id + "_" + set[2], set[1] + " " + set[2], Kind.ARMOUR, "cowl", color).value(lvl * 10).stack(1)
                    .equipment(wear(EquipmentDef.Slot.HEAD, stats(0, 0, 0, 0, -1, 4, 5, 3, 0, 4, 0, 0, 0, 0), Skill.HUNTER, lvl));
            s.add(id + "_top", set[1] + " top", Kind.ARMOUR, "leather_body", color).value(lvl * 15).stack(1)
                    .equipment(wear(EquipmentDef.Slot.CHEST, stats(0, 0, 0, 0, 0, 10, 15, 19, 0, 12, 0, 0, 0, 0), Skill.HUNTER, lvl));
            s.add(id + "_legs", set[1] + " legs", Kind.ARMOUR, "chaps", color).value(lvl * 15).stack(1)
                    .equipment(wear(EquipmentDef.Slot.LEGS, stats(0, 0, 0, 0, -7, 11, 10, 10, 0, 10, 0, 0, 0, 0), Skill.HUNTER, lvl));
        }
        // Capes (worn in the cape slot, see systems/Jewellery): OSRS weight-reducing capes.
        simple(s, "spotted_cape", "Spotted cape", "cape", 0xB08A50, 400).stack(1).examine("A cape made from spotted kebbit fur.");
        simple(s, "spottier_cape", "Spottier cape", "cape", 0xC06A2A, 800).stack(1).examine("A cape made from dashing kebbit fur.");
    }

    /** Jarred butterflies: id, name, colour. */
    private static final String[][] BUTTERFLIES = {{"ruby_harvest", "Ruby harvest", "C02040"},
            {"sapphire_glacialis", "Sapphire glacialis", "3060C0"}, {"snowy_knight", "Snowy knight", "F0F0F8"},
            {"black_warlock", "Black warlock", "202020"}};

    /** Camouflage outfits: prefix, colour, fur (two per piece, 20 coins; OSRS fancy dress shop). */
    private static final String[][] CAMO = {{"polar", "F0F0F0", "polar_kebbit_fur"}, {"wood", "8A6A4A", "common_kebbit_fur"},
            {"jungle", "4A7A3A", "feldip_weasel_fur"}, {"desert", "D0B080", "desert_devil_fur"}};

    @Override
    public void recipes(Consumer<Recipe> out) {
        for (Object[] m : MEATS) {
            out.accept(r("cook_cooked_" + m[0], MenuType.COOKING, "Cooked " + lower((String) m[1]), Skill.COOKING, (Integer) m[2],
                    (Double) m[3]).in("raw_" + m[0], 1).out("cooked_" + m[0], 1).burn((Integer) m[2] + 30, "burnt_meat").ticks(4).build());
        }
        // The Varrock fancy dress shop's fur exchanges (served at the tanner's counter).
        for (String[] c : CAMO) {
            for (String piece : new String[]{"top", "legs"}) {
                out.accept(plain("fancy_" + c[0] + "_camo_" + piece, MenuType.TANNING, title(c[0]) + " camo " + piece + " (2 furs, 20 coins)")
                        .in(c[2], 2).in("coins", 20).out(c[0] + "_camo_" + piece, 1).ticks(0).build());
            }
        }
        Object[][] sets = {{"larupia", "Larupia", "hat", 500, 100}, {"graahk", "Graahk", "headdress", 750, 150},
                {"kyatt", "Kyatt", "hat", 1000, 200}};   // coin costs approx
        for (Object[] set : sets) {
            String id = (String) set[0];
            String[][] pieces = {{(String) set[2], String.valueOf(set[3])}, {"top", String.valueOf(set[4])}, {"legs", String.valueOf(set[4])}};
            for (String[] p : pieces) {
                out.accept(plain("fancy_" + id + "_" + p[0], MenuType.TANNING, set[1] + " " + p[0] + " (" + p[1] + " coins)")
                        .in(id + "_fur", 1).in("tatty_" + id + "_fur", 1).in("coins", Integer.parseInt(p[1])).out(id + "_" + p[0], 1)
                        .ticks(0).build());
            }
        }
        out.accept(plain("fancy_spotted_cape", MenuType.TANNING, "Spotted cape (2 spotted kebbit furs, 400 coins)")
                .in("spotted_kebbit_fur", 2).in("coins", 400).out("spotted_cape", 1).ticks(0).build());
        out.accept(plain("fancy_spottier_cape", MenuType.TANNING, "Spottier cape (2 dashing kebbit furs, 800 coins)")
                .in("dashing_kebbit_fur", 2).in("coins", 800).out("spottier_cape", 1).ticks(0).build());
    }

    @Override
    public List<MonsterDef> monsters() {
        List<MonsterDef> list = new ArrayList<>();
        for (Impling i : IMPLINGS) {
            MonsterDef m = MonsterDef.of(i.id(), i.name()).level(1).stats(3, 1, 1, 1, 1, 1)
                    .attack(AttackDef.of("melee", AttackType.CRUSH, 0, 1.0, 1)).passive().flying().moveSpeed(0.35)
                    .skin(i.color(), i.color(), i.color(), 0xF0F0F0).size(0.5f, 0.9f, 0.5f);
            for (Region r : i.regions()) {
                m.spawn(r, i.level() >= 80 ? 1 : i.level() >= 50 ? 2 : 3, 1, 1);
            }
            list.add(m);
        }
        return list;
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (TrapSpot t : TRAPS) {
            String look = t.kind() == TrapKind.NET ? "young_tree" : t.kind() == TrapKind.DEADFALL ? "boulder" : "pit";
            if (t.kind() == TrapKind.TRACKING) {
                list.add(BlockDef.scripted(t.blockId(), t.creature() + " burrow", 0x6A5A3A, look));
                continue;
            }
            if (t.kind() == TrapKind.BUTTERFLY) {
                list.add(BlockDef.scripted(t.blockId(), t.creature() + " butterflies", 0x5A8A3A, "bush"));
                continue;
            }
            if (t.kind() == TrapKind.FALCONRY) {
                list.add(BlockDef.scripted(t.blockId(), t.creature() + " hiding spot", 0x7A6A3A, "bush"));
                continue;
            }
            list.add(BlockDef.scripted(t.blockId(), t.creature() + " hunting spot", 0x5A6A3A, look));
        }
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(
                PlaceDef.of("swamp_lizard_hunting", "Canifis swamp lizards", 3536, 3451, PlaceDef.Style.CAMP,
                        List.of("swamp_lizard_tree", "swamp_lizard_tree", "swamp_lizard_tree")),
                PlaceDef.of("orange_salamander_hunting", "Uzer orange salamanders", 3410, 3080, PlaceDef.Style.ARENA,
                        List.of("orange_salamander_tree", "orange_salamander_tree", "orange_salamander_tree")),
                PlaceDef.of("red_salamander_hunting", "Ourania red salamanders", 2448, 3220, PlaceDef.Style.CAMP,
                        List.of("red_salamander_tree", "red_salamander_tree", "red_salamander_tree")),
                PlaceDef.of("black_salamander_hunting", "Boneyard black salamanders", 3293, 3673, PlaceDef.Style.RUINS,
                        List.of("black_salamander_tree", "black_salamander_tree", "black_salamander_tree")),
                PlaceDef.of("tecu_salamander_hunting", "Varlamore tecu salamanders", 1477, 3093, PlaceDef.Style.CAMP,
                        List.of("tecu_salamander_tree", "tecu_salamander_tree")),
                PlaceDef.of("piscatoris_hunter_area", "Piscatoris Hunter area", 2340, 3580, PlaceDef.Style.CAMP,
                        List.of("wild_kebbit_deadfall", "prickly_kebbit_deadfall", "wild_kebbit_deadfall", "common_kebbit_burrow",
                                "common_kebbit_burrow", "razor_backed_kebbit_burrow", "ruby_harvest_bush")),
                PlaceDef.of("piscatoris_falconry", "Piscatoris falconry area", 2375, 3590, PlaceDef.Style.CAMP,
                        List.of("spotted_kebbit_falconry", "spotted_kebbit_falconry", "dark_kebbit_falconry",
                                "dashing_kebbit_falconry")),
                PlaceDef.of("feldip_hunter_area", "Feldip Hunter area", 2550, 2910, PlaceDef.Style.CAMP,
                        List.of("barb_tailed_kebbit_deadfall", "spined_larupia_pitfall", "horned_graahk_pitfall",
                                "barb_tailed_kebbit_deadfall")),
                // Jungle butterflies at the west edge of the Feldip Hunter area (whose structure is the islands module's).
                PlaceDef.of("feldip_butterflies", "Feldip jungle butterflies", 2520, 2905, PlaceDef.Style.CAMP,
                        List.of("ruby_harvest_bush", "black_warlock_bush", "black_warlock_bush")),
                // Feldip weasels live in the fields north of the Feldip Hunter area (its structure is the islands module's).
                PlaceDef.of("feldip_weasel_hunting", "Feldip weasel burrows", 2518, 2952, PlaceDef.Style.CAMP,
                        List.of("feldip_weasel_burrow", "feldip_weasel_burrow")),
                PlaceDef.of("rellekka_hunter_area", "Rellekka Hunter area", 2730, 3780, PlaceDef.Style.CAMP,
                        List.of("sabre_toothed_kebbit_deadfall", "sabre_toothed_kyatt_pitfall", "sabre_toothed_kyatt_pitfall",
                                "polar_kebbit_burrow", "polar_kebbit_burrow", "sapphire_glacialis_bush", "snowy_knight_bush")),
                PlaceDef.of("uzer_hunter_area", "Uzer Hunter area", 3440, 3060, PlaceDef.Style.CAMP,
                        List.of("desert_devil_burrow", "desert_devil_burrow")),
                PlaceDef.of("avium_savannah", "Avium Savannah", 1630, 2990, PlaceDef.Style.CAMP,
                        List.of("pyre_fox_deadfall", "pyre_fox_deadfall")));
    }
}
