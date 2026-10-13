package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Chance;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Hunter: implings (caught with a butterfly net into impling jars, looted for their OSRS-style loot
 * tables), salamanders (net traps), deadfall kebbits/pyre foxes, pitfall larupias/graahks/kyatts and
 * the low-level kebbits and weasels tracked down with a noose wand (polar, common, Feldip weasel,
 * desert devil, razor-backed), butterflies caught with a butterfly net into butterfly jars, falconry
 * kebbits at Piscatoris, and the Hunter gear sold at the Varrock fancy dress shop (camouflage outfits,
 * larupia/graahk/kyatt sets, spotted and spottier capes).
 * Also Puro-Puro's impling spawn weights and Elnock Inquisitor's exchange (the maze itself is the dungeons
 * module's {@code PuroPuro}), maniacal monkeys (Monkey Madness II), gloves of silence and imp boxes.
 * Levels and XP are OSRS values (implings award the open-world XP outside Puro-Puro, {@code puroXp} inside).
 * Catch chances: {@link com.runecraft.osrs.data.HunterRates}.
 */
public final class HunterData extends SkillingPart {

    /**
     * An impling: level, open-world XP, Puro-Puro XP (0 = not found there), region weights and loot (one
     * roll per jar). OSRS: Puro-Puro catches give less XP (e.g. dragon 65 there, 300 in Gielinor).
     */
    public record Impling(String id, String name, int level, double xp, double puroXp, int color, List<Region> regions, List<Loot> loot) {
        public String jar() {
            return id + "_jar";
        }

        /** XP for a catch, inside or outside Puro-Puro. */
        public double xpFor(boolean inPuroPuro) {
            return inPuroPuro && puroXp > 0 ? puroXp : xp;
        }
    }

    // Loot weights: the wiki's 1/x rarities scaled to integer weights. Where the wiki's exact rarity for a
    // low-tier line is not known the common/uncommon/rare tiers are approximated as 1/10, 1/50 and 1/250.
    private static final int C = 100;   // ~1/10
    private static final int U = 20;    // ~1/50
    private static final int R = 4;     // ~1/250

    public static final List<Impling> IMPLINGS = List.of(
            new Impling("baby_impling", "Baby impling", 17, 20.0, 18.0, 0xC0A060, List.of(Region.MISTHALIN, Region.ASGARNIA, Region.KANDARIN),
                    List.of(Loot.of("chisel", C), Loot.of("thread", C), Loot.of("needle", C), Loot.of("knife", C),
                            Loot.of("cheese", C), Loot.of("hammer", C), Loot.of("ball_of_wool", C), Loot.of("anchovies", C),
                            Loot.of("spice", U), Loot.of("flax", U), Loot.of("kebab", U), Loot.of("seaweed", U), Loot.of("air_talisman", U),
                            Loot.of("silver_bar", R), Loot.of("sapphire", R), Loot.of("hard_leather", R), Loot.of("lobster", R),
                            Loot.of("soft_clay", R))),   // approx: the mud pie / bucket of milk lines (not in this world) -> kebab / omitted
            new Impling("young_impling", "Young impling", 22, 22.0, 20.0, 0xA0B060, List.of(Region.MISTHALIN, Region.ASGARNIA, Region.KANDARIN),
                    List.of(Loot.of("steel_nails", 5, 5, C), Loot.of("lockpick", C), Loot.of("pure_essence", C), Loot.of("tuna", C),
                            Loot.of("chocolate_slice", C), Loot.of("steel_axe", U), Loot.of("meat_pizza", U), Loot.of("coal", U),
                            Loot.of("bow_string", U), Loot.of("snape_grass", R), Loot.of("soft_clay", R), Loot.of("studded_chaps", R),
                            Loot.of("steel_full_helm", R), Loot.of("oak_plank", R), Loot.of("defence_potion", R), Loot.of("mithril_bar", R),
                            Loot.of("yew_longbow", 1), Loot.of("garden_pie", 1), Loot.of("jangerberries", 1))),
            new Impling("gourmet_impling", "Gourmet impling", 28, 24.0, 22.0, 0xE0A060, List.of(Region.KANDARIN, Region.KARAMJA, Region.MISTHALIN),
                    List.of(Loot.of("tuna", C), Loot.of("bass", C), Loot.of("curry", C), Loot.of("meat_pie", C), Loot.of("chocolate_cake", C),
                            Loot.of("spice", U), Loot.of("curry_leaf", U), Loot.of("kebab", U),
                            Loot.of("lobster", 4, 4, R), Loot.of("shark", 3, 3, R), Loot.of("fish_pie", R), Loot.of("chocolate_bomb", R),
                            Loot.of("cooked_karambwan", 2, 2, R), Loot.of("swordfish", 3, 3, R), Loot.of("strawberry", 5, 5, R),
                            Loot.of("sea_turtle", 1), Loot.of("garden_pie", 1))),
            new Impling("earth_impling", "Earth impling", 36, 27.0, 25.0, 0x8A5A2A, List.of(Region.ASGARNIA, Region.FREMENNIK_PROVINCE, Region.WILDERNESS),
                    List.of(Loot.of("fire_talisman", C), Loot.of("earth_talisman", C), Loot.of("earth_tiara", C),
                            Loot.of("earth_rune", 32, 32, C), Loot.of("mithril_ore", C), Loot.of("bucket_of_sand", C),
                            Loot.of("unicorn_horn_dust", U), Loot.of("compost", 6, 6, U), Loot.of("gold_ore", U), Loot.of("steel_bar", U),
                            Loot.of("mithril_pickaxe", U), Loot.of("wildblood_seed", 2, 2, R), Loot.of("jangerberry_seed", 2, 2, R),
                            Loot.of("supercompost", 2, 2, R), Loot.of("mithril_ore", 3, 3, R), Loot.of("harralander_seed", 2, 2, R),
                            Loot.of("coal", 6, 6, R), Loot.of("emerald", 2, 2, R), Loot.of("uncut_ruby", 1), Loot.of("runite_ore", 1))),
            new Impling("essence_impling", "Essence impling", 42, 29.0, 27.0, 0xD0D0E0, List.of(Region.MISTHALIN, Region.WILDERNESS, Region.KHARIDIAN_DESERT),
                    List.of(Loot.of("pure_essence", 20, 20, C), Loot.of("water_rune", 30, 30, C), Loot.of("air_rune", 30, 30, C),
                            Loot.of("fire_rune", 50, 50, C), Loot.of("mind_rune", 25, 25, C), Loot.of("body_rune", 28, 28, C),
                            Loot.of("chaos_rune", 4, 4, U), Loot.of("cosmic_rune", 4, 4, U), Loot.of("mind_talisman", U),
                            Loot.of("pure_essence", 35, 35, R), Loot.of("nature_rune", 13, 13, R), Loot.of("law_rune", 13, 13, R),
                            Loot.of("death_rune", 13, 13, R), Loot.of("blood_rune", 7, 7, R))),
            new Impling("eclectic_impling", "Eclectic impling", 50, 32.0, 30.0, 0x6A9AA0, List.of(Region.KANDARIN, Region.TIRANNWN, Region.WILDERNESS),
                    List.of(Loot.of("mithril_pickaxe", C), Loot.of("curry_leaf", C), Loot.of("snape_grass", C),
                            Loot.of("air_rune", 30, 58, C), Loot.of("oak_plank", 4, 4, U), Loot.of("empty_candle_lantern", U),
                            Loot.of("gold_ore", U), Loot.of("gold_bar", 5, 5, U), Loot.of("unicorn_horn_dust", U),
                            Loot.of("adamant_kiteshield", R), Loot.of("blue_dhide_chaps", R), Loot.of("red_spiders_eggs", R),
                            Loot.of("rune_dagger", R), Loot.of("battlestaff", R), Loot.of("adamantite_ore", 10, 10, R),
                            Loot.of("wild_pie", 1), Loot.of("watermelon_seed", 3, 3, 1), Loot.of("diamond", 1))),
            new Impling("nature_impling", "Nature impling", 58, 36.0, 34.0, 0x3AA03A, List.of(Region.KANDARIN, Region.FELDIP_HILLS, Region.TIRANNWN),
                    List.of(Loot.of("limpwurt_seed", C), Loot.of("jangerberry_seed", C), Loot.of("belladonna_seed", C),
                            Loot.of("harralander_seed", C), Loot.of("cactus_spine", C), Loot.of("magic_logs", U), Loot.of("tarromin", 4, 4, U),
                            Loot.of("coconut", U), Loot.of("irit_seed", U), Loot.of("curry_seed", R), Loot.of("orange_seed", R),
                            Loot.of("snapdragon", R), Loot.of("kwuarm_seed", R), Loot.of("avantoe_seed", 5, 5, R), Loot.of("willow_seed", R),
                            Loot.of("torstol_seed", 1), Loot.of("ranarr_seed", 1), Loot.of("torstol", 2, 2, 1),
                            Loot.of("dwarf_weed_seed", 1))),
            // Wiki: black dragonhide 2/21, the other lines 1/21 (the key halves and snapdragon seed rarer, approx);
            // splitbark gauntlets and the sinister key aren't in this world.
            new Impling("magpie_impling", "Magpie impling", 65, 216.0, 44.0, 0x2A2A3A, List.of(Region.KARAMJA, Region.KANDARIN, Region.WILDERNESS),
                    List.of(Loot.of("black_dragonhide", 6, 6, 4), Loot.of("diamond_amulet", 3, 3, 2), Loot.of("amulet_of_power", 3, 3, 2),
                            Loot.of("ring_of_forging", 3, 3, 2), Loot.of("mystic_boots_light", 2), Loot.of("mystic_gloves_light", 2),
                            Loot.of("rune_warhammer", 2), Loot.of("ring_of_life", 4, 4, 2), Loot.of("rune_sq_shield", 2),
                            Loot.of("dragon_dagger", 2), Loot.of("nature_tiara", 2), Loot.of("runite_bar", 2, 2, 2), Loot.of("diamond", 4, 4, 2),
                            Loot.of("pineapple_seed", 2), Loot.of("loop_half_of_key", 1), Loot.of("tooth_half_of_key", 1),
                            Loot.of("snapdragon_seed", 1))),
            // Wiki: 19 lines at 1/19 each (splitbark helm, the opal machete and dagannoth hide aren't in this
            // world: a plain machete stands in, the other two are dropped; dragon dagger (p+) -> dragon dagger).
            new Impling("ninja_impling", "Ninja impling", 74, 240.0, 50.0, 0x1A1A1A, List.of(Region.KHARIDIAN_DESERT, Region.MORYTANIA, Region.WILDERNESS),
                    List.of(Loot.of("snakeskin_boots", 1), Loot.of("mystic_boots_dark", 1), Loot.of("rune_chainbody", 1),
                            Loot.of("mystic_gloves_dark", 1), Loot.of("rune_claws", 1), Loot.of("rune_scimitar", 1), Loot.of("dragon_dagger", 1),
                            Loot.of("rune_arrow", 70, 70, 1), Loot.of("rune_dart", 70, 70, 1), Loot.of("rune_knife", 40, 40, 1),
                            Loot.of("rune_thrownaxe", 50, 50, 1), Loot.of("onyx_bolts", 2, 2, 1), Loot.of("onyx_bolt_tips", 4, 4, 1),
                            Loot.of("black_dragonhide", 10, 10, 1), Loot.of("prayer_potion", 4, 4, 1), Loot.of("weapon_poison_plus", 4, 4, 1),
                            Loot.of("machete", 1))),
            // Prifddinas only (no Puro-Puro spawn). Wiki: every line 1/18; the first nine lines are the wiki's,
            // the rest (and the bolt/arrowtip quantities) approx.
            new Impling("crystal_impling", "Crystal impling", 80, 280.0, 0, 0x9AD8F0, List.of(Region.TIRANNWN),
                    List.of(Loot.of("amulet_of_power", 5, 7, 1), Loot.of("crystal_acorn", 1), Loot.of("crystal_shard", 30, 40, 1),
                            Loot.of("dragonstone_amulet", 1), Loot.of("dragonstone", 2, 2, 1), Loot.of("rune_arrowtips", 50, 100, 1),
                            Loot.of("ruby_bolt_tips", 50, 125, 1), Loot.of("onyx_bolt_tips", 6, 8, 1), Loot.of("rune_arrow", 400, 750, 1),
                            Loot.of("dragonstone_bolt_tips", 25, 35, 1), Loot.of("rune_javelin_heads", 25, 35, 1),
                            Loot.of("rune_dart_tip", 50, 100, 1), Loot.of("uncut_dragonstone", 1), Loot.of("rune_pickaxe", 1),
                            Loot.of("magic_seed", 1), Loot.of("ranarr_seed", 2, 3, 1), Loot.of("magic_logs", 25, 25, 1),
                            Loot.of("runite_ore", 3, 3, 1))),
            // Wiki: 19 lines at 1/19 each (babydragon bones 100-300, dragon bones 50-100, magic seed, 6 snapdragon
            // seeds, 15 summer pies, dragonstone bolt tips 10-30/36 confirmed; the other quantities approx).
            new Impling("dragon_impling", "Dragon impling", 83, 300.0, 65.0, 0xC02020, List.of(Region.WILDERNESS, Region.KARAMJA, Region.MORYTANIA),
                    List.of(Loot.of("dragonstone_bolt_tips", 10, 30, 1), Loot.of("dragonstone_bolt_tips", 36, 36, 1),
                            Loot.of("dragon_bolts_unf", 10, 20, 1), Loot.of("dragon_arrowtips", 100, 350, 1), Loot.of("dragon_dart_tip", 100, 350, 1),
                            Loot.of("dragon_arrow", 100, 250, 1), Loot.of("dragon_dart", 100, 250, 1), Loot.of("babydragon_bones", 100, 300, 1),
                            Loot.of("dragon_bones", 50, 100, 1), Loot.of("magic_seed", 1), Loot.of("snapdragon_seed", 6, 6, 1),
                            Loot.of("summer_pie", 15, 15, 1), Loot.of("dragonstone", 2, 3, 1), Loot.of("dragon_dagger", 1),
                            Loot.of("uncut_dragonstone", 1), Loot.of("onyx_bolt_tips", 5, 10, 1), Loot.of("amulet_of_power", 3, 3, 1),
                            Loot.of("dragonstone_amulet", 1), Loot.of("ranarr_seed", 2, 3, 1))),
            // Wiki: one roll on the easy/medium/hard/elite/master clue reward table, 1/5 each (never a master clue
            // scroll or a pet). The casket ids only mark the tier: the Minecraft side rolls ClueRewards.singleRoll.
            new Impling(HunterData.LUCKY, "Lucky impling", 89, 380.0, 80.0, 0x30C060, List.of(Region.WILDERNESS, Region.KANDARIN, Region.TIRANNWN),
                    List.of(Loot.of("reward_casket_easy", 1), Loot.of("reward_casket_medium", 1), Loot.of("reward_casket_hard", 1),
                            Loot.of("reward_casket_elite", 1), Loot.of("reward_casket_master", 1))));

    /** The lucky impling, whose jar holds one clue reward roll. */
    public static final String LUCKY = "lucky_impling";

    /**
     * Puro-Puro's invisible spawn points (wiki): a low-tier spawn rolls baby, young, gourmet and earth 20/100
     * each and essence and eclectic 10/100; a high-tier spawn rolls nature 150/301, magpie 114/301, ninja
     * 27/301, dragon 9/301 and lucky 1/301. Crystal implings live only in Prifddinas.
     */
    public static final Map<String, Integer> PURO_LOW_TIER = weights("baby_impling", 20, "young_impling", 20,
            "gourmet_impling", 20, "earth_impling", 20, "essence_impling", 10, "eclectic_impling", 10);
    public static final Map<String, Integer> PURO_HIGH_TIER = weights("nature_impling", 150, "magpie_impling", 114,
            "ninja_impling", 27, "dragon_impling", 9, "lucky_impling", 1);

    private static Map<String, Integer> weights(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    /** A spawn table expanded to one entry per unit of weight (camps pick uniformly). */
    public static List<String> expand(Map<String, Integer> weights) {
        List<String> out = new ArrayList<>();
        weights.forEach((id, w) -> {
            for (int i = 0; i < w; i++) {
                out.add(id);
            }
        });
        return out;
    }

    /** Barehanded catches need this many levels above the impling's (wiki: e.g. nature 58, or 68 barehanded). */
    public static final int BAREHANDED_EXTRA_LEVELS = 10;
    /** Catch-chance bonus barehanded or with a magic butterfly net (wiki: +7.8%, i.e. 20/256). */
    public static final double MAGIC_NET_BONUS = 20 / 256.0;

    /**
     * Chance to catch an impling. The wiki's nature impling chart (about 49% at its level, 79% at 99) is
     * used for every impling (approx); barehanded or with a magic butterfly net it is 20/256 higher.
     */
    public static double catchChance(Impling imp, int hunterLevel, boolean boosted) {
        if (hunterLevel < imp.level()) {
            return 0.0;
        }
        int req = Math.max(1, Math.min(97, imp.level()));
        double high = 0.79 * 256 - 1;
        double atReq = 0.49 * 256 - 1;
        double low = (atReq * 98 - high * (req - 1)) / (99 - req);
        return Math.min(1.0, Chance.lerp(hunterLevel, low, high) + (boosted ? MAGIC_NET_BONUS : 0));
    }

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

    // ------------------------------------------------------------------ Elnock Inquisitor (Puro-Puro)

    /** Elnock Inquisitor's exchange counter, at the centre of Puro-Puro (dungeons module). */
    public static final String ELNOCKS_EXCHANGE = "elnocks_exchange";

    /** Price of an impling jar from Elnock (wiki: 2,000 coins; his 10-a-day limit isn't modelled). */
    public static final int ELNOCK_JAR_PRICE = 2_000;
    /** Charges of a new jar generator, and the charges an impling jar / butterfly jar uses (wiki). */
    public static final int JAR_GENERATOR_CHARGES = 100;
    public static final int JAR_GENERATOR_IMPLING_COST = 3;
    public static final int JAR_GENERATOR_BUTTERFLY_COST = 1;

    /**
     * Elnock's exchange (wiki): 3 baby + 2 young + 1 gourmet impling jar for imp repellent; 3 gourmet + 2
     * earth + 1 essence for a magic butterfly net; 3 essence + 2 eclectic + 1 nature for a jar generator; any
     * single impling jar for 3 empty impling jars. Served as the {@link MenuType#IMPLING_EXCHANGE} menu.
     */
    public record Exchange(String id, String name, String reward, int rewardCount, Map<String, Integer> jars) {
    }

    public static final List<Exchange> ELNOCK_EXCHANGES = exchanges();

    private static List<Exchange> exchanges() {
        List<Exchange> out = new ArrayList<>();
        out.add(new Exchange("elnock_imp_repellent", "Imp repellent (3 baby, 2 young, 1 gourmet)", "imp_repellent", 1,
                weights("baby_impling_jar", 3, "young_impling_jar", 2, "gourmet_impling_jar", 1)));
        out.add(new Exchange("elnock_magic_butterfly_net", "Magic butterfly net (3 gourmet, 2 earth, 1 essence)", "magic_butterfly_net", 1,
                weights("gourmet_impling_jar", 3, "earth_impling_jar", 2, "essence_impling_jar", 1)));
        out.add(new Exchange("elnock_jar_generator", "Jar generator (3 essence, 2 eclectic, 1 nature)", "jar_generator", 1,
                weights("essence_impling_jar", 3, "eclectic_impling_jar", 2, "nature_impling_jar", 1)));
        for (Impling i : IMPLINGS) {
            out.add(new Exchange("elnock_jars_" + i.id(), "3 impling jars (1 " + lower(i.name()) + " jar)", "impling_jar", 3,
                    weights(i.jar(), 1)));
        }
        return List.copyOf(out);
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
                    List.of("bones", "dashing_kebbit_fur"), 0),
            // Maniacal monkeys (Kruk's Dungeon, Monkey Madness II): a large boulder deadfall baited with a banana;
            // the monkey sheds its tail (damaged monkey tail, or an intact one 1/5,000) and escapes (wiki).
            new TrapSpot(HunterData.MANIACAL_MONKEY_BOULDER, "Maniacal monkey", TrapKind.DEADFALL, 60, 1000.0, List.of(), "banana",
                    List.of("damaged_monkey_tail"), 20, "monkey_tail", 5000));

    /** The maniacal monkeys' boulder trap (needs Monkey Madness II, see {@link #MANIACAL_MONKEY_QUEST}). */
    public static final String MANIACAL_MONKEY_BOULDER = "maniacal_monkey_boulder";
    public static final String MANIACAL_MONKEY_QUEST = "monkey_madness_ii";
    /** Imps caught in a magic box (wiki: 71 Hunter, 450 XP; a bead in the box adds 3% to the catch chance). */
    public static final int IMP_LEVEL = 71;
    public static final double IMP_XP = 450.0;
    public static final double IMP_BEAD_BONUS = 0.03;
    /** Gloves of silence: pickpocketing success multiplier while worn (wiki: +5%, approx as a multiplier). */
    public static final double GLOVES_OF_SILENCE_BONUS = 1.05;

    /** Imp-in-a-box charges by item id: each use banks one item stack; the empty box is a magic box again. */
    public static String impBoxAfterUse(String id) {
        return switch (id) {
            case "imp_in_a_box_2" -> "imp_in_a_box_1";
            case "imp_in_a_box_1" -> "magic_box";
            default -> null;
        };
    }

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
        // Elnock's exchange (Puro-Puro). The magic butterfly net is held like the plain one (OSRS: 17 Hunter to wield).
        tool(s, "magic_butterfly_net", "Magic butterfly net", "net", 0xA0C0F0, 1000)
                .examine("It's a magic butterfly net: implings are easier to catch with it.");
        simple(s, "jar_generator", "Jar generator", "jar", 0x8AC0A0, 600).stack(1)
                .examine("Makes impling jars (3 charges) and butterfly jars (1 charge). " + JAR_GENERATOR_CHARGES + " charges.");
        simple(s, "imp_repellent", "Imp repellent", "vial", 0xD0B0E0, 50).stack(1)
                .examine("Implings can't stand the smell. Dip a butterfly jar in it to make an impling jar.");
        // Gloves of silence (Crafting, dark kebbit fur): +5% pickpocketing (worn in the hands slot, see systems/Jewellery).
        simple(s, "gloves_of_silence", "Gloves of silence", "gloves", 0x3A2A1A, 600).stack(1)
                .examine("Made from dark kebbit fur, these are perfect for tasks of a stealthier nature.");
        // Imp boxes: a magic box catches an imp (71 Hunter); the imp banks two item stacks for you.
        simple(s, "magic_box", "Magic box", "box_trap", 0x6A4A8A, 600).stack(1).examine("A magical box for trapping imps.");
        simple(s, "imp_in_a_box_2", "Imp-in-a-box(2)", "box_trap", 0x8A4A6A, 600).stack(1)
                .examine("An imp in a box: it will take two items to your bank.");
        simple(s, "imp_in_a_box_1", "Imp-in-a-box(1)", "box_trap", 0x8A4A6A, 600).stack(1)
                .examine("An imp in a box: it will take one more item to your bank.");
        // Maniacal monkey tails (Kruk's Dungeon).
        simple(s, "damaged_monkey_tail", "Damaged monkey tail", "tail", 0x6A4A2A, 1).examine("It's been bitten off.");
        // monkey_tail (the rare maniacal monkey catch) is in osrs/content/CommonItems: demonic gorillas drop it too.
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
        // Elnock Inquisitor's exchange in Puro-Puro.
        for (Exchange e : ELNOCK_EXCHANGES) {
            Recipe.Builder b = plain(e.id(), MenuType.IMPLING_EXCHANGE, e.name());
            e.jars().forEach(b::in);
            out.accept(b.out(e.reward(), e.rewardCount()).ticks(0).build());
        }
        out.accept(plain("elnock_buy_impling_jar", MenuType.IMPLING_EXCHANGE, "Impling jar (" + ELNOCK_JAR_PRICE + " coins)")
                .in("coins", ELNOCK_JAR_PRICE).out("impling_jar", 1).ticks(0).build());
        // Dipping a butterfly jar into imp repellent makes an impling jar (the repellent is kept, approx).
        out.accept(plain("dip_impling_jar", MenuType.IMPLING_EXCHANGE, "Impling jar (dip a butterfly jar in imp repellent)")
                .in("butterfly_jar", 1).tool("imp_repellent").out("impling_jar", 1).ticks(0).build());
        // Gloves of silence (wiki: 54 Crafting, 60 XP, two dark kebbit furs, needle and thread).
        out.accept(r("craft_gloves_of_silence", MenuType.LEATHERWORKING, "Gloves of silence", Skill.CRAFTING, 54, 60.0)
                .in("dark_kebbit_fur", 2).in("minecraft:string", 1).tool("needle").out("gloves_of_silence", 1).ticks(3).build());
        // A damaged monkey tail cut into sinew with a knife (wiki: 10 Crafting, 15 XP).
        out.accept(r("cut_monkey_tail_sinew", MenuType.FLETCHING, "Sinew (damaged monkey tail)", Skill.CRAFTING, 10, 15.0)
                .in("damaged_monkey_tail", 1).tool("knife").out("sinew", 1).ticks(2).build());
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
        list.add(BlockDef.menu(ELNOCKS_EXCHANGE, "Elnock's Exchange", MenuType.IMPLING_EXCHANGE, 0x8A6AA0, "crate"));
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
        // The maniacal monkey boulders stand in Glough's laboratory inside Kruk's Dungeon (dungeons module).
    }
}
