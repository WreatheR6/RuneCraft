package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.BurnRates;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * The Ruins of Camdozaal beneath Ice Mountain (OSRS wiki "Ruins of Camdozaal", "Fishing spot (Camdozaal)",
 * "Preparation Table", "Altar (Camdozaal)", the golem pages). The cave itself is carved by the dungeons module
 * ({@code dng_ruins_of_camdozaal}); this part holds the skilling data it uses:
 *
 * <ul>
 *   <li>Fishing: seven spots with a small net (guppy 7, cavefish 20, tetra 33) and a big net (tetra 33, catfish 46);
 *       a catch may be 3 barronite shards instead, and 1/512 a barronite handle.</li>
 *   <li>The preparation table: with a knife, each raw fish is prepared (Cooking at the fish's Fishing level) or ruined;
 *       ruining stops at 42 (guppy), 53 (cavefish), 61 (tetra) and 78 (catfish) Cooking. 4 ticks per fish.</li>
 *   <li>The altar: prepared fish are offered for Prayer XP (4, 7, 10, 16), 3 ticks per fish.</li>
 *   <li>The golems (flawed 13, mind 30, body 50, chaos 70) that lie dormant as rubble; their drop tables, with the
 *       barronite guard and the mind/body/chaos cores.</li>
 *   <li>The barronite mace: the head, guard and handle plus 1,500 barronite shards, handed to Ramarno at the Sacred
 *       Forge (here: used on the forge).</li>
 * </ul>
 */
public final class Camdozaal extends SkillingPart {
    public static final String FISHING_SPOT = "camdozaal_fishing_spot";
    public static final String PREPARATION_TABLE = "camdozaal_preparation_table";
    public static final String ALTAR = "camdozaal_altar";
    public static final String SACRED_FORGE = "camdozaal_sacred_forge";

    public static final String SHARDS = "barronite_shards";
    public static final String HANDLE = "barronite_handle";
    public static final String GUARD = "barronite_guard";
    public static final String HEAD = "barronite_head";
    public static final String MACE = "barronite_mace";
    /** Shards Ramarno takes to forge the mace from its three parts. */
    public static final int MACE_SHARDS = 1500;

    /** Game ticks to prepare one fish at the table and to offer one at the altar. */
    public static final int PREPARE_TICKS = 4;
    public static final int OFFER_TICKS = 3;

    /** One Camdozaal fish: Fishing level/XP, Cooking XP for preparing it, the level it stops being ruined, Prayer XP. */
    public record Fish(String id, String name, int level, double fishingXp, double cookingXp, int stopRuin, double prayerXp,
                       int low, int high) {
        public String raw() {
            return "raw_" + id;
        }

        public String prepared() {
            return id;
        }

        public String ruined() {
            return "ruined_" + id;
        }

        /** Chance to prepare the fish rather than ruin it (the burn-rate curve ending at its stop level). */
        public double prepareChance(int cooking) {
            return cooking >= stopRuin ? 1.0 : BurnRates.success(id, stopRuin, 0, cooking, false, false);
        }
    }

    /**
     * Wiki "Ruins of Camdozaal" fishing table and the fish pages' ruin levels. The catch curves (low/high out of 256) are
     * not on the wiki. // approx
     */
    public static final List<Fish> FISH = List.of(
            new Fish("guppy", "Guppy", 7, 8.0, 12.0, 42, 4.0, 96, 256),
            new Fish("cavefish", "Cavefish", 20, 16.0, 23.0, 53, 7.0, 64, 224),
            new Fish("tetra", "Tetra", 33, 24.0, 31.0, 61, 10.0, 48, 192),
            new Fish("catfish", "Catfish", 46, 33.0, 43.0, 78, 16.0, 32, 160));

    public static Fish fish(String id) {
        for (Fish f : FISH) {
            if (f.id().equals(id)) {
                return f;
            }
        }
        return null;
    }

    /** The fish a raw item prepares into, or null. */
    public static Fish byRaw(String rawId) {
        for (Fish f : FISH) {
            if (f.raw().equals(rawId)) {
                return f;
            }
        }
        return null;
    }

    /** The fish a prepared item is, or null. */
    public static Fish byPrepared(String preparedId) {
        return fish(preparedId);
    }

    /** Share of catches that are barronite shards (3) instead of a fish (wiki: "Common"). // approx */
    public static final int JUNK_PERCENT = 20;
    /** Junk table: 3 shards, or a barronite handle at about 1/512 of all catches (Mod Ash). */
    static final List<Loot> JUNK = List.of(Loot.of(SHARDS, 3, 3, 100), Loot.of(HANDLE, 1, 1, 1)); // 0.2 x 1/101 ~ 1/505

    private static GatheringData.Catch c(Fish f) {
        return GatheringData.Catch.of(f.raw(), f.level(), f.fishingXp(), f.low(), f.high());
    }

    /** The spot as fished with a small net (guppy, cavefish, tetra; highest first). */
    public static final GatheringData.Spot SMALL_NET = new GatheringData.Spot(FISHING_SPOT, "Fishing spot (Camdozaal)",
            "small_fishing_net", null, List.of(c(FISH.get(2)), c(FISH.get(1)), c(FISH.get(0))), JUNK, JUNK_PERCENT);
    /** The same spot fished with a big net (catfish, tetra). */
    public static final GatheringData.Spot BIG_NET = new GatheringData.Spot(FISHING_SPOT, "Fishing spot (Camdozaal)",
            "big_fishing_net", null, List.of(c(FISH.get(3)), c(FISH.get(2))), JUNK, JUNK_PERCENT);

    /** The other net's spot for a spot block that takes two nets, or null. */
    public static GatheringData.Spot otherNet(GatheringData.Spot spot) {
        if (spot == SMALL_NET) {
            return BIG_NET;
        }
        return spot == BIG_NET ? SMALL_NET : null;
    }

    // ------------------------------------------------------------------ golems

    /** A Camdozaal golem: id, name, level, cache/wiki spawn tiles {x, y, ...}. */
    public record Golem(String id, String name, int level, int[] tiles) {
    }

    /** Wiki LocLine spawns (the cache's rubble NPCs match the flawed and mind golems). */
    public static final List<Golem> GOLEMS = List.of(
            new Golem("flawed_golem", "Flawed Golem", 13, new int[]{2976, 5775, 2978, 5781, 2979, 5770, 2979, 5784, 2984, 5773, 2984, 5780,
                    2991, 5780, 2992, 5777}),
            new Golem("mind_golem", "Mind Golem", 30, new int[]{2998, 5775, 3001, 5774, 3001, 5788, 3002, 5785, 3003, 5779, 3003, 5782}),
            new Golem("body_golem", "Body Golem", 50, new int[]{3015, 5808, 3017, 5810, 3021, 5812, 3022, 5808, 3025, 5802, 3025, 5805}),
            new Golem("chaos_golem", "Chaos Golem", 70, new int[]{3014, 5777, 3015, 5772, 3017, 5784, 3022, 5772, 3024, 5785, 3025, 5774,
                    3025, 5780}));

    @Override
    public List<MonsterDef> monsters() {
        List<MonsterDef> list = new ArrayList<>();
        // osrs-dps-calc stats; the wiki's single-roll tables (out of 52) as independent 1/n lines. // approx
        list.add(golem("flawed_golem", "Flawed Golem", 13, 25, 7, 12, 6, AttackType.STAB, 3, 0x8A8070)
                .drops(Drop.of("clay", 1, 3, 3), Drop.of("rune_essence", 1, 4, 2), Drop.of("tin_ore", 1, 2, 9),
                        Drop.of("copper_ore", 1, 2, 9), Drop.of("uncut_sapphire", 1, 1, 26), Drop.of("uncut_emerald", 1, 1, 52),
                        Drop.of(SHARDS, 6, 12, 16), Drop.of(GUARD, 1, 1, 800), Drop.of("clue_scroll_beginner", 1, 1, 100)));
        list.add(golem("mind_golem", "Mind Golem", 30, 40, 14, 31, 25, AttackType.STAB, 5, 0xB07A50)
                .drops(Drop.of("clay", 2, 3, 9), Drop.of("copper_ore", 2, 3, 9), Drop.of("tin_ore", 2, 3, 9),
                        Drop.of("iron_ore", 1, 2, 9), Drop.of("rune_essence", 1, 5, 8), Drop.of("mind_rune", 1, 10, 4),
                        Drop.of("uncut_sapphire", 1, 1, 13), Drop.of("uncut_emerald", 1, 1, 26), Drop.of("uncut_ruby", 1, 1, 52),
                        Drop.of(SHARDS, 8, 16, 8), Drop.of("mind_core", 1, 1, 8), Drop.of(GUARD, 1, 1, 500),
                        Drop.of("clue_scroll_beginner", 1, 1, 50)));
        list.add(golem("body_golem", "Body Golem", 50, 60, 33, 43, 45, AttackType.SLASH, 7, 0x6A7AA0)
                .drops(Drop.of("clay", 3, 4, 9), Drop.of("copper_ore", 3, 4, 9), Drop.of("tin_ore", 3, 4, 9),
                        Drop.of("iron_ore", 2, 3, 9), Drop.of("rune_essence", 1, 6, 8), Drop.of("body_rune", 1, 10, 4),
                        Drop.of("uncut_sapphire", 1, 1, 13), Drop.of("uncut_emerald", 1, 1, 26), Drop.of("uncut_ruby", 1, 1, 52),
                        Drop.of(SHARDS, 10, 20, 5), Drop.of("body_core", 1, 1, 8), Drop.of(GUARD, 1, 1, 250),
                        Drop.of("clue_scroll_beginner", 1, 1, 62)));
        list.add(golem("chaos_golem", "Chaos Golem", 70, 94, 45, 55, 59, AttackType.CRUSH, 10, 0x6A5A5A)
                .drops(Drop.of("clay", 3, 4, 9), Drop.of("iron_ore", 3, 4, 9), Drop.of("silver_ore", 3, 4, 9),
                        Drop.of("gold_ore", 2, 3, 9), Drop.of("rune_essence", 4, 6, 9), Drop.of("chaos_rune", 5, 10, 7),
                        Drop.of("uncut_sapphire", 1, 1, 9), Drop.of("uncut_emerald", 1, 1, 13), Drop.of("uncut_ruby", 1, 1, 26),
                        Drop.of("uncut_diamond", 1, 1, 52), Drop.of("chaos_talisman", 1, 1, 52), Drop.of(SHARDS, 12, 24, 3),
                        Drop.of("chaos_core", 1, 1, 8), Drop.of(GUARD, 1, 1, 150), Drop.of("clue_scroll_beginner", 1, 1, 100)));
        return list;
    }

    private static MonsterDef golem(String id, String name, int level, int hp, int att, int str, int def, AttackType style, int max,
                                    int glow) {
        // Dormant rubble until awakened, so they never attack first; size 2, attack speed 5, weak to earth spells (35%).
        return MonsterDef.of(id, name).level(level).stats(hp, att, str, def, 1, 1).speed(5)
                .attack(AttackDef.of("melee", style, max, 2.5, 10)).defenceBonus(5, 5, 0, 2, 10).rangedDefence(10, 10)
                .attributes("golem").weakness("earth", 35).passive().underground()
                .skin(0x7A7268, 0x6A6258, 0x5A5248, glow).size(1.4f, 2.0f, 1.0f);
    }

    // ------------------------------------------------------------------ items, blocks

    @Override
    public void items(ItemSink s) {
        for (Fish f : FISH) {
            simple(s, f.raw(), "Raw " + lower(f.name()), "fish_raw", 0x8AA0B0, 5).examine("I should prepare this at the table.");
            simple(s, f.prepared(), f.name(), "fish_cooked", 0xB0A080, 5).examine("This should be offered up at the altar.");
            simple(s, f.ruined(), "Ruined " + lower(f.name()), "fish_burnt", 0x4A4038, 1).examine("I've ruined this one.");
        }
        simple(s, HANDLE, "Barronite handle", "shaft", 0x7A6A8A, 8532).examine("A chunky piece of barronite.");
        simple(s, GUARD, "Barronite guard", "shard", 0x7A6A8A, 8532).examine("A robust piece of barronite.");
        simple(s, "mind_core", "Mind core", "orb", 0xC08040, 8).examine("The corrupted core of a Mind Golem.");
        simple(s, "body_core", "Body core", "orb", 0x4060B0, 8).examine("The corrupted core of a Body Golem.");
        simple(s, "chaos_core", "Chaos core", "orb", 0xB0A030, 8).examine("The corrupted core of a Chaos Golem.");
        // Wiki bonuses: +20 stab, -2 slash, +40 crush, +40 strength, +2 prayer, speed 4, 40 Attack; blunt (crush only).
        s.add(MACE, "Barronite mace", Kind.MELEE_WEAPON, "mace", 0x7A6A8A).value(10_500).stack(1)
                .equipment(weapon(stats(20, -2, 40, 0, 0, 0, 0, 0, 0, 0, 40, 0, 0, 2), 4, AttackType.CRUSH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 40))
                .examine("A barronite mace, forged in Camdozaal.");
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(
                BlockDef.scripted(PREPARATION_TABLE, "Preparation Table", 0x8A8478, "table"),
                BlockDef.scripted(ALTAR, "Altar", 0x7A7A8A, "altar"),
                BlockDef.scripted(SACRED_FORGE, "Sacred Forge", 0x8A5A3A, "furnace"));
    }
}
