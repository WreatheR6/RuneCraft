package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.dungeons.PuroPuro;
import com.runecraft.osrs.content.slayer.TormentedDemons;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.osrs.content.systems.ClueRewards;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.zeah.ZeahBlocks;
import com.runecraft.osrs.content.zeah.ZeahHunter;
import com.runecraft.osrs.data.EquipmentBonuses;
import com.runecraft.osrs.data.Untradeables;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Puro-Puro and the impling follow-ups (wiki XP, spawn weights, Elnock's exchange, loot tables, catch chances),
 * maniacal monkeys, gloves of silence, imp boxes, Arclight/Emberlight and demonbane, and the Varlamore traps'
 * share of the trap limit.
 */
class HunterFollowupsTest {

    private static HunterData.Impling imp(String id) {
        HunterData.Impling i = HunterData.impling(id);
        assertNotNull(i, id);
        return i;
    }

    // ------------------------------------------------------------------ implings

    @Test
    void implingXpIsLowerInPuroPuro() {
        // Wiki: e.g. dragon 65 in Puro-Puro / 300 elsewhere, ninja 50 / 240, nature 34 / 36.
        Object[][] xp = {{"baby_impling", 20.0, 18.0}, {"young_impling", 22.0, 20.0}, {"gourmet_impling", 24.0, 22.0},
                {"earth_impling", 27.0, 25.0}, {"essence_impling", 29.0, 27.0}, {"eclectic_impling", 32.0, 30.0},
                {"nature_impling", 36.0, 34.0}, {"magpie_impling", 216.0, 44.0}, {"ninja_impling", 240.0, 50.0},
                {"dragon_impling", 300.0, 65.0}, {"lucky_impling", 380.0, 80.0}};
        for (Object[] row : xp) {
            HunterData.Impling i = imp((String) row[0]);
            assertEquals((Double) row[1], i.xp(), i.id());
            assertEquals((Double) row[2], i.puroXp(), i.id());
            assertEquals((Double) row[1], i.xpFor(false), i.id());
            assertEquals((Double) row[2], i.xpFor(true), i.id());
        }
        // Crystal implings only live in Prifddinas: the open-world XP wherever they're caught.
        assertEquals(280.0, imp("crystal_impling").xpFor(true));
    }

    @Test
    void puroPuroSpawnWeightsFollowTheWiki() {
        assertEquals(100, HunterData.PURO_LOW_TIER.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(301, HunterData.PURO_HIGH_TIER.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(20, HunterData.PURO_LOW_TIER.get("baby_impling"));
        assertEquals(10, HunterData.PURO_LOW_TIER.get("eclectic_impling"));
        assertEquals(150, HunterData.PURO_HIGH_TIER.get("nature_impling"));
        assertEquals(114, HunterData.PURO_HIGH_TIER.get("magpie_impling"));
        assertEquals(27, HunterData.PURO_HIGH_TIER.get("ninja_impling"));
        assertEquals(9, HunterData.PURO_HIGH_TIER.get("dragon_impling"));
        assertEquals(1, HunterData.PURO_HIGH_TIER.get("lucky_impling"));
        assertFalse(HunterData.PURO_HIGH_TIER.containsKey("crystal_impling"));
        assertEquals(301, HunterData.expand(HunterData.PURO_HIGH_TIER).size());
    }

    @Test
    void puroPuroIsAMazeBelowZanarisWithElnockAndImplingCamps() {
        Dungeons.Dungeon puro = Dungeons.all().stream().filter(d -> d.id().equals("dng_puro_puro")).findFirst().orElseThrow();
        assertTrue(PuroPuro.contains(PuroPuro.centreX(), PuroPuro.FLOOR + 1, PuroPuro.centreZ()));
        assertFalse(PuroPuro.contains(PuroPuro.centreX(), 64, PuroPuro.centreZ()), "the surface above isn't Puro-Puro");
        assertFalse(PuroPuro.contains(PuroPuro.centreX() + PuroPuro.HALF + 5, PuroPuro.FLOOR + 1, PuroPuro.centreZ()));
        // The crop circle lies in Zanaris (its OSRS instance coordinates).
        assertTrue(PuroPuro.CROP_CIRCLE_X > 2364 && PuroPuro.CROP_CIRCLE_X < 2460 && PuroPuro.CROP_CIRCLE_Y > 4388
                && PuroPuro.CROP_CIRCLE_Y < 4476);
        boolean elnock = false;
        boolean wheat = false;
        for (StructureDef.Op op : puro.structure().ops()) {
            elnock |= ("runecraft:" + HunterData.ELNOCKS_EXCHANGE).equals(op.block());
            wheat |= op.block() != null && op.block().startsWith("minecraft:hay_block") && op.y2() <= Dungeons.DEEP_TOP;
        }
        assertTrue(elnock, "Elnock's exchange is in the maze");
        assertTrue(wheat, "the maze has wheat walls");
        assertEquals(2, puro.camps().size());
        Map<String, Integer> high = new HashMap<>();
        for (String c : puro.camps().get(1).creatures()) {
            high.merge(c, 1, Integer::sum);
        }
        assertEquals(HunterData.PURO_HIGH_TIER, high);
        Map<String, Integer> low = new HashMap<>();
        for (String c : puro.camps().get(0).creatures()) {
            low.merge(c, 1, Integer::sum);
        }
        assertEquals(HunterData.PURO_LOW_TIER, low);
        assertTrue(ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(HunterData.ELNOCKS_EXCHANGE)));
    }

    @Test
    void elnocksExchangeTradesJarsForTheWikiRewards() {
        Map<String, Recipe> byId = new HashMap<>();
        for (Recipe r : Recipes.menu(MenuType.IMPLING_EXCHANGE)) {
            byId.put(r.id(), r);
        }
        assertEquals(Map.of("baby_impling_jar", 3, "young_impling_jar", 2, "gourmet_impling_jar", 1),
                byId.get("elnock_imp_repellent").inputs());
        assertEquals(Map.of("gourmet_impling_jar", 3, "earth_impling_jar", 2, "essence_impling_jar", 1),
                byId.get("elnock_magic_butterfly_net").inputs());
        assertEquals(Map.of("essence_impling_jar", 3, "eclectic_impling_jar", 2, "nature_impling_jar", 1),
                byId.get("elnock_jar_generator").inputs());
        assertEquals(1, byId.get("elnock_jar_generator").outputs().get("jar_generator"));
        assertEquals(3, byId.get("elnock_jars_dragon_impling").outputs().get("impling_jar"));
        assertEquals(HunterData.ELNOCK_JAR_PRICE, byId.get("elnock_buy_impling_jar").inputs().get("coins"));
        assertEquals("imp_repellent", byId.get("dip_impling_jar").requiredTool());
        for (Recipe r : byId.values()) {
            r.inputs().keySet().forEach(i -> assertTrue(i.equals("coins") || ItemCatalog.exists(i), r.id() + " " + i));
            r.outputs().keySet().forEach(i -> assertTrue(ItemCatalog.exists(i), r.id() + " " + i));
        }
        for (String id : List.of("magic_butterfly_net", "jar_generator", "imp_repellent")) {
            assertTrue(Untradeables.untradeable(id), id);
        }
    }

    @Test
    void implingCatchChanceFollowsTheNatureImplingChart() {
        HunterData.Impling nature = imp("nature_impling");
        assertEquals(0.0, HunterData.catchChance(nature, 57, true));
        assertEquals(0.49, HunterData.catchChance(nature, 58, false), 0.01);
        assertEquals(0.79, HunterData.catchChance(nature, 99, false), 0.01);
        assertEquals(20 / 256.0, HunterData.catchChance(nature, 80, true) - HunterData.catchChance(nature, 80, false), 1e-9);
        assertEquals(10, HunterData.BAREHANDED_EXTRA_LEVELS);
        for (HunterData.Impling i : HunterData.IMPLINGS) {
            double prev = 0;
            for (int lvl = i.level(); lvl <= 99; lvl++) {
                double p = HunterData.catchChance(i, lvl, false);
                assertTrue(p >= prev && p <= 1.0, i.id() + " " + lvl);
                prev = p;
            }
        }
    }

    @Test
    void implingLootTablesMatchTheWikiShape() {
        assertEquals(18, imp("crystal_impling").loot().size());
        assertEquals(19, imp("dragon_impling").loot().size());
        assertTrue(imp("crystal_impling").loot().stream().anyMatch(l -> l.item().equals("crystal_shard") && l.min() == 30 && l.max() == 40));
        assertTrue(imp("dragon_impling").loot().stream().anyMatch(l -> l.item().equals("babydragon_bones") && l.max() == 300));
        assertTrue(imp("ninja_impling").loot().stream().anyMatch(l -> l.item().equals("rune_dart") && l.min() == 70));
        HunterData.Impling magpie = imp("magpie_impling");
        int total = Loot.totalWeight(magpie.loot());
        Loot hide = magpie.loot().stream().filter(l -> l.item().equals("black_dragonhide")).findFirst().orElseThrow();
        assertEquals(2.0 / 21, (double) hide.weight() / total, 0.03);
        for (HunterData.Impling i : HunterData.IMPLINGS) {
            for (Loot l : i.loot()) {
                assertTrue(ItemCatalog.exists(l.item()), i.id() + ": " + l.item());
            }
        }
    }

    @Test
    void luckyImplingRollsOneClueRewardPerTier() {
        HunterData.Impling lucky = imp(HunterData.LUCKY);
        assertEquals(5, lucky.loot().size());
        Random random = new Random(7);
        for (Loot l : lucky.loot()) {
            ClueScrolls.Tier tier = ClueScrolls.Tier.byCasket(l.item());
            assertNotNull(tier, l.item());
            assertEquals(1, l.weight(), "each tier 1/5");
            for (int n = 0; n < 200; n++) {
                ClueRewards.Reward r = ClueRewards.singleRoll(tier, random);
                assertTrue(ItemCatalog.exists(r.item()), r.item());
                assertTrue(r.count() >= 1);
                assertFalse(r.item().equals("clue_scroll_master"), "lucky implings never give a master clue scroll");
            }
        }
    }

    // ------------------------------------------------------------------ monkeys, gloves, imps

    @Test
    void maniacalMonkeysAreHuntedWithBananaBoulders() {
        HunterData.TrapSpot t = HunterData.trap(HunterData.MANIACAL_MONKEY_BOULDER);
        assertNotNull(t);
        assertEquals(60, t.level());
        assertEquals(1000.0, t.xp());
        assertEquals("banana", t.bait());
        assertEquals(HunterData.TrapKind.DEADFALL, t.kind());
        assertTrue(t.kind().laid(), "counts towards the trap limit");
        assertEquals(List.of("damaged_monkey_tail"), t.loot());
        assertEquals("monkey_tail", t.rareLoot());
        assertEquals(5000, t.rareOneIn());
        assertEquals("monkey_madness_ii", HunterData.MANIACAL_MONKEY_QUEST);
        assertNotNull(ContentRegistry.quest(HunterData.MANIACAL_MONKEY_QUEST));
        boolean placed = false;
        for (PlaceDef p : new HunterData().places()) {
            placed |= p.blocks().contains(HunterData.MANIACAL_MONKEY_BOULDER);
        }
        assertTrue(placed);
        Recipe sinew = Recipes.get("cut_monkey_tail_sinew");
        assertEquals(10, sinew.level());
        assertEquals("knife", sinew.requiredTool());
        assertEquals(1, sinew.outputs().get("sinew"));
    }

    @Test
    void glovesOfSilenceAreCraftedFromDarkKebbitFur() {
        Recipe r = Recipes.get("craft_gloves_of_silence");
        assertEquals(Skill.CRAFTING, r.skill());
        assertEquals(54, r.level());
        assertEquals(600, r.xpTenths());
        assertEquals(2, r.inputs().get("dark_kebbit_fur"));
        assertEquals(Jewellery.Slot.HANDS, Jewellery.worn("gloves_of_silence").slot());
        assertEquals(1.05, HunterData.GLOVES_OF_SILENCE_BONUS);
    }

    @Test
    void magicBoxesCatchImpsThatBankTwoItems() {
        assertEquals(71, HunterData.IMP_LEVEL);
        assertEquals(450.0, HunterData.IMP_XP);
        assertEquals("imp_in_a_box_1", HunterData.impBoxAfterUse("imp_in_a_box_2"));
        assertEquals("magic_box", HunterData.impBoxAfterUse("imp_in_a_box_1"));
        assertNull(HunterData.impBoxAfterUse("magic_box"));
        assertNotNull(ContentRegistry.monster("imp"), "imps exist to be caught");
        for (String id : List.of("magic_box", "imp_in_a_box_2", "imp_in_a_box_1")) {
            assertTrue(ItemCatalog.exists(id), id);
        }
    }

    // ------------------------------------------------------------------ Arclight / Emberlight / demonbane

    @Test
    void arclightIsChargedWithAncientShards() {
        ItemDef arclight = ItemCatalog.get("arclight");
        assertNotNull(arclight);
        assertEquals(75, arclight.equipment().requirements().get(Skill.ATTACK));
        assertEquals(4, arclight.equipment().attackSpeed());
        assertTrue(EquipmentBonuses.get("arclight") != null);
        Charges.Spec spec = Charges.of(Charges.ARCLIGHT);
        assertEquals(1000, spec.initial());
        assertEquals(10_000, spec.max());
        assertEquals(1000, Charges.rechargeable(spec, 1000, Map.of("ancient_shard", 3)));
        assertEquals(0, Charges.rechargeable(spec, 1000, Map.of("ancient_shard", 2)));
        assertEquals(9000, Charges.rechargeable(spec, 1000, Map.of("ancient_shard", 300)), "capped at 10,000");
        assertEquals(Map.of("ancient_shard", 27), Charges.cost(spec, 9000));
        assertTrue(spec.weapon(), "one charge per hit");
        assertTrue(Untradeables.untradeable("arclight"));
        // The altar in the Catacombs of Kourend forges it.
        assertEquals(3, ZeahBlocks.ARCLIGHT_SHARDS);
        boolean altar = false;
        for (StructureDef s : ContentRegistry.structures()) {
            for (StructureDef.Op op : s.ops()) {
                altar |= ("runecraft:" + ZeahBlocks.CATACOMBS_ALTAR).equals(op.block());
            }
        }
        assertTrue(altar, "the catacombs altar is placed");
    }

    @Test
    void emberlightNeedsAFullyChargedArclight() {
        assertNotNull(TormentedDemons.emberlightProblem(73, 10_000, 10_000, true));
        assertNotNull(TormentedDemons.emberlightProblem(74, 9_999, 10_000, true));
        assertNotNull(TormentedDemons.emberlightProblem(74, 10_000, 10_000, false));
        assertNull(TormentedDemons.emberlightProblem(74, 10_000, 10_000, true));
        assertEquals(77, ItemCatalog.get("emberlight").equipment().requirements().get(Skill.ATTACK));
    }

    @Test
    void demonbaneWeaponsUseTheWikiBonuses() {
        assertEquals(1.60, TormentedDemons.DEMONBANE_BONUS.get("silverlight"));
        assertEquals(1.60, TormentedDemons.DEMONBANE_BONUS.get("darklight"));
        assertEquals(1.70, TormentedDemons.DEMONBANE_BONUS.get("arclight"));
        assertEquals(1.70, TormentedDemons.DEMONBANE_BONUS.get("emberlight"));
        assertEquals(1.30, TormentedDemons.DEMONBANE_BONUS.get("scorching_bow"));
        for (String id : TormentedDemons.DEMONBANE_BONUS.keySet()) {
            assertTrue(TormentedDemons.pierces(id), id);
            assertTrue(ItemCatalog.exists(id), id);
        }
    }

    // ------------------------------------------------------------------ Varlamore traps

    @Test
    void varlamoreLaidTrapsShareTheTrapLimit() {
        for (ZeahHunter.Spot s : ZeahHunter.SPOTS) {
            assertEquals(s.method() != ZeahHunter.Method.NET, s.laid(), s.blockId());
        }
        assertTrue(ZeahHunter.SPOTS.stream().anyMatch(s -> s.method() == ZeahHunter.Method.PITFALL && s.laid()));
        assertTrue(ZeahHunter.SPOTS.stream().anyMatch(s -> s.method() == ZeahHunter.Method.BOX_TRAP && s.laid()));
    }
}
