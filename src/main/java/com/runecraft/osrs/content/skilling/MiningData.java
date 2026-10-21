package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mining: sandstone, granite, blurite, limestone, volcanic ash, lead, volcanic sulphur, barronite (with the
 * Barronite Crusher) and the Motherlode Mine (pay-dirt veins, hopper,
 * sack, golden nuggets and the prospector kit, which boosts Mining XP by up to 2.5%).
 * Levels/XP are OSRS values; success rates are approximations on the usual 1-99 interpolation.
 */
public final class MiningData extends SkillingPart {

    /**
     * A rock added by this module. Each mined ore picks one line of {@code yields} by weight (with its
     * own XP, so sandstone/granite sizes give different XP like in-game).
     */
    public record MineRock(String blockId, String name, int level, int low, int high, int respawnTicks, List<Yield> yields,
                           int color) {
        /** True when each yield rolls its own low/high in turn (sandstone and granite sizes). */
        public boolean cascades() {
            return yields.get(0).high() > 0;
        }

        /**
         * One mining attempt at a Mining level: the yield mined, or null for a miss. Sandstone and
         * granite try each size in turn, biggest first, with that size's own low/high (the OSRS
         * "cascade"); other rocks make one roll with the rock's low/high and then pick a yield by weight.
         */
        public Yield attempt(int level, java.util.function.DoubleSupplier random) {
            if (cascades()) {
                for (Yield y : yields) {
                    if (random.getAsDouble() < com.runecraft.osrs.Chance.lerp(level, y.low(), y.high())) {
                        return y;
                    }
                }
                return null;
            }
            if (random.getAsDouble() >= com.runecraft.osrs.Chance.lerp(level, low, high)) {
                return null;
            }
            int total = 0;
            for (Yield y : yields) {
                total += y.weight();
            }
            int r = (int) Math.floor(random.getAsDouble() * total);
            for (Yield y : yields) {
                r -= y.weight();
                if (r < 0) {
                    return y;
                }
            }
            return yields.get(yields.size() - 1);
        }
    }

    /**
     * A possible yield: item, weight (single-roll rocks), XP, its own low/high for cascading rocks (0/0 otherwise) and
     * how many are given (barronite shards come 4-6 at a time).
     */
    public record Yield(String item, int weight, double xp, int low, int high, int min, int max) {
        public Yield(String item, int weight, double xp, int low, int high) {
            this(item, weight, xp, low, high, 1, 1);
        }

        public Yield(String item, int weight, double xp) {
            this(item, weight, xp, 0, 0, 1, 1);
        }

        /** Amount for a uniform roll in [0, 1). */
        public int amount(double u) {
            return min + (int) Math.floor(u * (max - min + 1));
        }
    }

    /** Mining XP of a rubium geode (wiki Rubium deposit: average 40); declared before ROCKS, which reads it. */
    public static final double[] RUBIUM_DEPOSIT_XP = {10.0, 16.7, 18.9, 20.8, 26.0, 32.5, 38.3, 40.0, 56.8, 80.0, 100.0};

    public static final List<MineRock> ROCKS = List.of(
            // Sandstone and granite: each size has its own success curve (OSRS Wiki skilling success chart),
            // tried from the heaviest down; the rock's own low/high is unused.
            new MineRock("sandstone_rock", "Sandstone rocks", 35, 25, 200, 9, List.of(new Yield("sandstone_10kg", 1, 60.0, 4, 50),
                    new Yield("sandstone_5kg", 1, 50.0, 8, 75), new Yield("sandstone_2kg", 1, 40.0, 16, 100),
                    new Yield("sandstone_1kg", 1, 30.0, 25, 200)), 0xD8C080),
            new MineRock("granite_rock", "Granite rocks", 45, 16, 100, 9, List.of(new Yield("granite_5kg", 1, 75.0, 6, 64),
                    new Yield("granite_2kg", 1, 60.0, 8, 75), new Yield("granite_500g", 1, 50.0, 16, 100)), 0x8A7A70),
            new MineRock("blurite_rock", "Blurite rocks", 10, 90, 350, 42, List.of(new Yield("blurite_ore", 1, 17.5)), 0x4A6AC0),
            // Limestone (10 Mining, 26.5 XP); chiselled into limestone bricks for Construction. Respawn approx.
            new MineRock("limestone_rock", "Limestone rocks", 10, 95, 350, 15, List.of(new Yield("limestone", 1, 26.5)), 0xD8D0B0),
            new MineRock("ash_pile", "Ash pile", 22, 64, 256, 50, List.of(new Yield("volcanic_ash", 1, 10.0)), 0x5A5A5A),
            new MineRock("pay_dirt_vein", "Ore vein", 30, 60, 105, 0, List.of(new Yield("pay_dirt", 1, 60.0)), 0x6A5A3A),
            // The lower level's inner ring (around the central pillar): respawns after 150 ticks instead of 100.
            new MineRock("inner_pay_dirt_vein", "Ore vein (inner ring)", 30, 60, 105, 0, List.of(new Yield("pay_dirt", 1, 60.0)), 0x5A4A30),
            // The Motherlode Mine's upper level (57 Mining and 100 nuggets paid to Prospector Percy; veins last longer).
            new MineRock("upper_pay_dirt_vein", "Ore vein (upper level)", 30, 60, 105, 0, List.of(new Yield("pay_dirt", 1, 60.0)), 0x7A6A4A),
            // Lead rocks (Sailing islands; Deepfin Point): 25 Mining, 40.5 XP, 112/278 (Mod Ash). Respawn approx.
            new MineRock("lead_rock", "Lead rocks", 25, 112, 278, 10, List.of(new Yield("lead_ore", 1, 40.5)), 0x5A5A68),
            // Nickel rocks (Sailing islands: Deepfin Point, Ynysdail, Grimstone): 74 Mining, 80.5 XP. The wiki has no success
            // chart yet (approx 40/140), several ores per rock (approx: depletes 1 in 3), respawn approx.
            new MineRock("nickel_rock", "Nickel rocks", 74, 40, 140, 20, List.of(new Yield("nickel_ore", 1, 80.5)), 0xA8A890),
            // Rubium rocks (Charred Dungeon, 60 Sailing): 48 Mining, 30 XP, stackable rubium splinters. Rates approx.
            new MineRock("rubium_rock", "Rubium rocks", 48, 80, 200, 15, List.of(new Yield("rubium_splinters", 1, 30.0)), 0xC03A2A),
            // Rubium deposits (Charred Dungeon, Kragin's two): 68 Mining, a rubium geode for one of the wiki's eleven XP values
            // (10 to 100, average 40). Success rates approx (no wiki chart); OSRS keeps one of the two active, each lasting 4-5
            // minutes; here they never deplete (approx).
            new MineRock("rubium_deposit", "Rubium deposit", 68, 60, 160, 0, rubiumDeposit(), 0xA02A2A),
            // Volcanic sulphur (Lovakengj sulphur mine): 42 Mining, 35 XP, 100/220, respawns in 25.2 seconds; needs a
            // breathing filter (gas mask, facemask or slayer helmet) worn.
            new MineRock("volcanic_sulphur_rock", "Volcanic sulphur", 42, 100, 220, 42, List.of(new Yield("volcanic_sulphur", 1, 35.0)), 0xC8B830),
            // Barronite rocks (Ruins of Camdozaal): 14 Mining, 80/100; 76% 4-6 barronite shards (16 XP), 24% a barronite
            // deposit (32 XP). A rock depletes 20 seconds after it starts being mined and respawns after a minute.
            new MineRock("barronite_rock", "Barronite rocks", 14, 80, 100, 100, List.of(new Yield("barronite_shards", 76, 16.0, 0, 0, 4, 6),
                    new Yield("barronite_deposit", 24, 32.0)), 0x7A6A8A));

    /** The rubium deposit's geode at each Mining XP value of the wiki's table, equally likely. */
    private static List<Yield> rubiumDeposit() {
        List<Yield> out = new java.util.ArrayList<>();
        for (double xp : RUBIUM_DEPOSIT_XP) {
            out.add(new Yield("rubium_geode", 1, xp));
        }
        return List.copyOf(out);
    }

    /** Rubium splinters in a cracked geode (wiki: 3-7 at 68-75 Mining, 4-8 at 76-83, 5-9 at 84-91, 6-10 from 92). */
    public static int rubiumSplinters(int miningLevel, java.util.Random random) {
        int min = miningLevel >= 92 ? 6 : miningLevel >= 84 ? 5 : miningLevel >= 76 ? 4 : 3;
        return min + random.nextInt(5);
    }

    public static final String LEAD = "lead_rock";
    public static final String SULPHUR = "volcanic_sulphur_rock";
    public static final String BARRONITE = "barronite_rock";
    public static final String BARRONITE_CRUSHER = "barronite_crusher";
    /** Game ticks a barronite rock lasts once someone starts mining it (20 seconds). */
    public static final int BARRONITE_LIFE_TICKS = 33;
    /** Worn head items that filter the sulphur fumes (any slayer helmet counts). */
    public static final List<String> BREATHING_FILTERS = List.of("gas_mask", "facemask", "slayer_helmet", "slayer_helmet_i");

    /** Whether the worn head item filters the sulphur fumes: a {@link #BREATHING_FILTERS} item or any recoloured slayer helmet. */
    public static boolean filtersFumes(String head) {
        return head != null && (BREATHING_FILTERS.contains(head) || head.endsWith("_slayer_helmet") || head.endsWith("_slayer_helmet_i"));
    }

    /** Smithing level and XP for crushing a barronite deposit at the Barronite Crusher with a hammer. */
    public static final int CRUSH_LEVEL = 14;
    public static final double CRUSH_XP = 30.0;
    /**
     * What a crushed barronite deposit gives (OSRS Wiki drop log, weights per 4,200): 6-8 shards 12/13, 15-35 shards
     * 1/20, barronite head 1/200, broken Imcando hammer about 1/300 and each ancient relic 1/350. // approx
     */
    public static final List<Loot> CRUSH_TABLE = List.of(Loot.of("barronite_shards", 6, 8, 3780), Loot.of("barronite_shards", 15, 35, 210),
            Loot.of("barronite_head", 21), Loot.of("imcando_hammer_broken", 14), Loot.of("ancient_astroscope", 12),
            Loot.of("ancient_carcanet", 12), Loot.of("ancient_globe", 12), Loot.of("ancient_ledger", 12), Loot.of("ancient_treatise", 12));

    /** Block ids of the upper-level vein and hopper of the Motherlode Mine. */
    public static final String UPPER_VEIN = "upper_pay_dirt_vein";
    public static final String UPPER_HOPPER = "upper_pay_dirt_hopper";
    /** Block id of the lower level's inner-ring vein (150-tick respawn). */
    public static final String INNER_VEIN = "inner_pay_dirt_vein";

    /** True for any Motherlode Mine pay-dirt vein block. */
    public static boolean isPayDirtVein(String blockId) {
        return blockId.equals("pay_dirt_vein") || blockId.equals(UPPER_VEIN) || blockId.equals(INNER_VEIN);
    }

    public static MineRock rock(String blockId) {
        for (MineRock r : ROCKS) {
            if (r.blockId().equals(blockId)) {
                return r;
            }
        }
        return null;
    }

    /** Motherlode sack capacity (before the 200-nugget upgrade to 189). See {@link Motherlode}. */
    public static final int SACK_CAPACITY = Motherlode.SACK_CAPACITY;

    /**
     * Ore found in one pay-dirt for a Mining level, as weights per 10,000 (golden nuggets excluded):
     * coal, gold, mithril, adamantite, runite as unlocked. Derived from {@link Motherlode#distribution(int)}.
     */
    public static List<Loot> payDirtTable(int miningLevel) {
        List<Loot> t = new ArrayList<>();
        java.util.Map<String, Double> d = Motherlode.distribution(miningLevel);
        double ore = 1.0 - d.get("golden_nugget");
        for (String item : new String[]{"coal", "gold_ore", "mithril_ore", "adamantite_ore", "runite_ore"}) {
            int w = (int) Math.round(d.get(item) / ore * 10_000);
            if (w > 0) {
                t.add(Loot.of(item, w));
            }
        }
        return t;
    }

    /** Prospector kit pieces and their XP bonus (percent). The full set adds another 0.5%. */
    public static final String[] PROSPECTOR = {"prospector_helmet", "prospector_jacket", "prospector_legs", "prospector_boots"};
    public static final double[] PROSPECTOR_BONUS = {0.4, 0.8, 0.6, 0.2};

    @Override
    public void items(ItemSink s) {
        for (MineRock r : ROCKS) {
            for (Yield y : r.yields()) {
                if (y.item().startsWith("sandstone") || y.item().startsWith("granite")) {
                    simple(s, y.item(), (y.item().startsWith("sandstone") ? "Sandstone (" : "Granite (")
                            + y.item().substring(y.item().indexOf('_') + 1) + ")", "ore", r.color(), (int) y.xp());
                }
            }
        }
        simple(s, "limestone", "Limestone", "ore", 0xD8D0B0, 10);
        simple(s, "lead_ore", "Lead ore", "ore", 0x5A5A68, 30).examine("This needs refining.");   // value approx
        simple(s, "volcanic_sulphur", "Volcanic sulphur", "ore", 0xC8B830, 10)
                .examine("Minerals from an unstable area, very rich in sulphur.");
        simple(s, "barronite_shards", "Barronite shards", "shard", 0x7A6A8A, 1).stack(Integer.MAX_VALUE)
                .examine("Shards of barronite. The Camdozaal dwarves trade with these.");
        simple(s, "barronite_deposit", "Barronite deposit", "ore", 0x6A5A7A, 1).examine("Something might be hidden inside.");
        simple(s, "barronite_head", "Barronite head", "headless", 0x7A6A8A, 1).examine("The head of a barronite mace.");
        simple(s, "imcando_hammer_broken", "Imcando hammer (broken)", "hammer", 0x6A5A5A, 1).examine("A broken dwarven hammer.");
        for (String relic : new String[]{"ancient_astroscope", "ancient_carcanet", "ancient_globe", "ancient_ledger", "ancient_treatise"}) {
            simple(s, relic, title(relic), "package", 0x9A8A6A, 1).examine("A relic of Camdozaal. Ramarno would be interested.");
        }
        simple(s, "pay_dirt", "Pay-dirt", "ore", 0x6A5A3A, 1);
        simple(s, "golden_nugget", "Golden nugget", "nugget", 0xE8C030, 1).examine("Prospector Percy accepts these.");
        simple(s, "bucket_of_sand", "Bucket of sand", "bucket", 0xE0D090, 3);
        int c = 0x8A6A3A;
        s.add("prospector_helmet", "Prospector helmet", Kind.ARMOUR, "full_helm", c).value(1).stack(1).tier("prospector")
                .equipment(wear(Slot.HEAD, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        s.add("prospector_jacket", "Prospector jacket", Kind.ARMOUR, "leather_body", c).value(1).stack(1).tier("prospector")
                .equipment(wear(Slot.CHEST, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        s.add("prospector_legs", "Prospector legs", Kind.ARMOUR, "chaps", c).value(1).stack(1).tier("prospector")
                .equipment(wear(Slot.LEGS, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        s.add("prospector_boots", "Prospector boots", Kind.ARMOUR, "boots", c).value(1).stack(1).tier("prospector")
                .equipment(wear(Slot.FEET, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        // Prospector Percy's reward shop (golden nuggets).
        Object[][] kit = {{"prospector_helmet", 40}, {"prospector_jacket", 60}, {"prospector_legs", 50}, {"prospector_boots", 30}};
        for (Object[] k : kit) {
            out.accept(plain("shop_tools_" + k[0], MenuType.SHOP_TOOLS, title((String) k[0]) + " - " + k[1] + " golden nuggets")
                    .in("golden_nugget", (Integer) k[1]).out((String) k[0], 1).ticks(0).build());
        }
        // Desert quarry grinder: sandstone to buckets of sand (sand = 1 per kg); bucket of sand to vanilla sand.
        int[] kg = {1, 2, 5, 10};
        for (int k : kg) {
            out.accept(plain("grind_sandstone_" + k, MenuType.SMELTING, k + " bucket" + (k > 1 ? "s" : "") + " of sand (sandstone)")
                    .in("sandstone_" + k + "kg", 1).in("minecraft:bucket", k).out("bucket_of_sand", k).ticks(2).build());
        }
        // Limestone is chiselled into limestone bricks (12 Crafting, 6 XP).
        out.accept(r("chisel_limestone_brick", MenuType.GEM_CUTTING, "Limestone brick", Skill.CRAFTING, 12, 6.0).in("limestone", 1)
                .out("limestone_brick", 1).tool("chisel").ticks(2).build());
        // Lead bar: two lead ore at a furnace (25 Smithing, 15.5 XP).
        out.accept(r("smelt_lead_bar", MenuType.SMELTING, "Lead bar", Skill.SMITHING, 25, 15.5).in("lead_ore", 2).out("lead_bar", 1)
                .ticks(5).build());
        out.accept(plain("empty_bucket_of_sand", MenuType.SMELTING, "Sand (empty bucket of sand)").in("bucket_of_sand", 1)
                .out("minecraft:sand", 1).out("minecraft:bucket", 1).ticks(1).build());
        out.accept(r("smelt_molten_glass_bucket", MenuType.SMELTING, "Molten glass (bucket of sand)", Skill.CRAFTING, 1, 20.0)
                .in("bucket_of_sand", 1).in("soda_ash", 1).out("molten_glass", 1).out("minecraft:bucket", 1).ticks(3).build());
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (MineRock r : ROCKS) {
            list.add(BlockDef.scripted(r.blockId(), r.name(), r.color(), "rock"));
        }
        list.add(BlockDef.scripted("pay_dirt_hopper", "Hopper", 0x7A6A5A, "machine"));
        list.add(BlockDef.scripted(UPPER_HOPPER, "Hopper (upper level)", 0x8A7A5A, "machine"));
        list.add(BlockDef.scripted("pay_dirt_sack", "Sack", 0xA08060, "crate"));
        list.add(BlockDef.scripted(BARRONITE_CRUSHER, "Barronite Crusher", 0x6A6A70, "machine"));
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(
                PlaceDef.of("motherlode_mine", "Motherlode Mine", 3045, 3340, PlaceDef.Style.CAVE,
                        List.of("pay_dirt_vein", "pay_dirt_vein", "pay_dirt_vein", "pay_dirt_vein", "pay_dirt_vein", "pay_dirt_hopper",
                                "pay_dirt_sack")),
                PlaceDef.of("desert_quarry", "Desert Quarry", 3170, 2910, PlaceDef.Style.ARENA,
                        List.of("sandstone_rock", "sandstone_rock", "granite_rock", "granite_rock", "sandstone_rock", "granite_rock")),
                PlaceDef.of("paterdomus_limestone", "Limestone mine", 3372, 3498, PlaceDef.Style.CAMP,
                        List.of("limestone_rock", "limestone_rock", "limestone_rock")),
                PlaceDef.of("asgarnian_ice_dungeon_blurite", "Blurite rocks", 3049, 3170, PlaceDef.Style.CAVE,
                        List.of("blurite_rock", "blurite_rock")),
                PlaceDef.of("fossil_island_ash", "Fossil Island ash piles", 3793, 3770, PlaceDef.Style.RUINS,
                        List.of("ash_pile", "ash_pile", "ash_pile", "ash_pile")));
        // The Camdozaal Mines' barronite rocks and the Barronite Crusher stand in the dungeons module's Ruins of
        // Camdozaal under Ice Mountain (dng_ruins_of_camdozaal).
    }
}
