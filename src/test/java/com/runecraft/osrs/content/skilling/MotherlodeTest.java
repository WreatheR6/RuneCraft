package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.extras.SkillingPets;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Motherlode Mine numbers from the OSRS Wiki (Motherlode Mine, Pay-dirt, Sack). */
class MotherlodeTest {
    @Test
    void levelNinetyNineMatchesTheWiki() {
        Map<String, Double> d = Motherlode.distribution(99);
        // The wiki's cascading chart (low/high per ore) reproduces its level-99 row exactly; its prose has
        // 18.18% for adamantite, a typo for 18.85% (the row then sums to 100%).
        assertEquals(0.2460, d.get("coal"), 0.0001);
        assertEquals(0.03125, d.get("golden_nugget"), 1e-9);
        assertEquals(0.2422, d.get("gold_ore"), 0.0001);
        assertEquals(0.2693, d.get("mithril_ore"), 0.0001);
        assertEquals(0.1885, d.get("adamantite_ore"), 0.0001);
        assertEquals(0.0227, d.get("runite_ore"), 0.0001);
    }

    @Test
    void oresUnlockAtTheirLevels() {
        for (int level = 30; level <= 99; level++) {
            Map<String, Double> d = Motherlode.distribution(level);
            double sum = 0;
            for (double v : d.values()) {
                assertTrue(v >= 0);
                sum += v;
            }
            assertEquals(1.0, sum, 1e-9, "level " + level);
        }
        assertEquals(0.0, Motherlode.distribution(39).get("gold_ore"), 1e-12);
        assertTrue(Motherlode.distribution(40).get("gold_ore") > 0);
        assertEquals(0.0, Motherlode.distribution(54).get("mithril_ore"), 1e-12);
        assertTrue(Motherlode.distribution(55).get("mithril_ore") > 0);
        assertEquals(0.0, Motherlode.distribution(69).get("adamantite_ore"), 1e-12);
        assertEquals(0.0, Motherlode.distribution(84).get("runite_ore"), 1e-12);
        assertTrue(Motherlode.distribution(85).get("runite_ore") > 0);
        assertTrue(Motherlode.distribution(99).get("runite_ore") > Motherlode.distribution(85).get("runite_ore"));
        assertEquals(1, MiningData.payDirtTable(30).size());
        assertEquals(5, MiningData.payDirtTable(99).size());
    }

    @Test
    void collectionExperience() {
        assertEquals(60.0, Motherlode.PAY_DIRT_XP);
        // Coal gives 15 XP since the May 2024 rebalance.
        assertEquals(15.0, Motherlode.collectXp("coal"));
        assertEquals(15.0, Motherlode.collectXp("gold_ore"));
        assertEquals(30.0, Motherlode.collectXp("mithril_ore"));
        assertEquals(45.0, Motherlode.collectXp("adamantite_ore"));
        assertEquals(75.0, Motherlode.collectXp("runite_ore"));
        assertEquals(0.0, Motherlode.collectXp("golden_nugget"));
        // Wiki: average bonus XP per pay-dirt is 16.84 at 85 and 21.9 at 99, from before coal gave 15 XP.
        Map<String, Double> d85 = Motherlode.distribution(85);
        assertEquals(16.84, Motherlode.averageCollectXp(85) - 15 * d85.get("coal"), 0.3);
        assertEquals(21.9, Motherlode.averageCollectXp(99) - 15 * Motherlode.distribution(99).get("coal"), 0.05);
        MiningData.MineRock vein = MiningData.rock("pay_dirt_vein");
        assertNotNull(vein);
        assertEquals(30, vein.level());
        assertEquals(60.0, vein.yields().get(0).xp());
    }

    @Test
    void sackAndUpperLevel() {
        assertEquals(108, Motherlode.sackCapacity(false));
        assertEquals(189, Motherlode.sackCapacity(true));
        assertEquals(108, MiningData.SACK_CAPACITY);
        assertEquals(200, Motherlode.SACK_UPGRADE_NUGGETS);
        assertEquals(8, Motherlode.deposit(28, 100, false));
        assertEquals(28, Motherlode.deposit(28, 100, true));
        assertEquals(0, Motherlode.deposit(28, 108, false));
        // 57 Mining since the 2022 change (72 before).
        assertFalse(Motherlode.canUseUpperLevel(56, true));
        assertTrue(Motherlode.canUseUpperLevel(57, true));
        assertEquals(50, Motherlode.UPPER_HOPPER_NUGGETS);
        assertNotNull(MiningData.rock(MiningData.UPPER_VEIN));
        // Veins last 23-27 s (38-45 ticks) below and 36-40 s (60-67 ticks) on the upper level.
        assertEquals(38, Motherlode.veinLife(false, 0.0));
        assertEquals(45, Motherlode.veinLife(false, 0.9999));
        assertEquals(60, Motherlode.veinLife(true, 0.0));
        assertEquals(67, Motherlode.veinLife(true, 0.9999));
        assertEquals(3, Motherlode.nuggetsKept(5));
        assertFalse(Motherlode.canUseUpperLevel(99, false));
        assertEquals(100, Motherlode.UPPER_LEVEL_NUGGETS);
    }

    @Test
    void washingMatchesTheDistribution() {
        Random r = new Random(4);
        Map<String, Integer> seen = new HashMap<>();
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            seen.merge(Motherlode.wash(99, r::nextDouble), 1, Integer::sum);
        }
        Map<String, Double> d = Motherlode.distribution(99);
        for (Map.Entry<String, Double> e : d.entrySet()) {
            assertTrue(ItemCatalog.exists(e.getKey()), e.getKey());
            assertEquals(e.getValue(), seen.getOrDefault(e.getKey(), 0) / (double) n, 0.005, e.getKey());
        }
        SkillingPets.Roll pet = SkillingPets.forItem("pay_dirt");
        assertNotNull(pet);
        assertEquals(247_200, pet.base());
    }

    @Test
    void oreIsRolledWhenMinedAndInnerRingRespawnsSlower() {
        // Every roll result survives the per-player queue unchanged (rolled at mining, washed at the hopper).
        for (String item : new String[]{"golden_nugget", "coal", "gold_ore", "mithril_ore", "adamantite_ore", "runite_ore"}) {
            assertEquals(item, Motherlode.decode(Motherlode.code(item)));
        }
        assertEquals(100, Motherlode.veinRespawn(false));
        assertEquals(150, Motherlode.veinRespawn(true));
        assertEquals(64, Motherlode.UPPER_LEVEL_VEINS);
        assertNotNull(MiningData.rock(MiningData.INNER_VEIN));
        assertTrue(MiningData.isPayDirtVein("pay_dirt_vein") && MiningData.isPayDirtVein(MiningData.UPPER_VEIN)
                && MiningData.isPayDirtVein(MiningData.INNER_VEIN));
        assertFalse(MiningData.isPayDirtVein("coal_rock"));
    }

    @Test
    void mixesBrewsAndKegs() {
        // OSRS Wiki: ancient brew 85 / 190 XP (dwarf weed unf + nihil dust); magic essence 57 / 130 XP;
        // ancient mix 92 / 63 XP and magic essence mix 61 / 43 XP, both caviar, 6 hp.
        com.runecraft.osrs.Recipe brew = com.runecraft.osrs.Recipes.get("mix_ancient_brew");
        assertEquals(85, brew.level());
        assertEquals(190.0, brew.xpTenths() / 10.0, 1e-9);
        assertTrue(brew.inputs().containsKey("dwarf_weed_potion_unf") && brew.inputs().containsKey("nihil_dust"));
        assertEquals(130.0, com.runecraft.osrs.Recipes.get("mix_magic_essence").xpTenths() / 10.0, 1e-9);
        assertTrue(com.runecraft.osrs.Recipes.get("grind_nihil_shard") != null && com.runecraft.osrs.Recipes.get("grind_gorak_claws") != null);
        assertEquals(92, HerbloreData.mix("ancient_mix").level());
        assertEquals(63.0, HerbloreData.mix("ancient_mix").xp(), 1e-9);
        assertEquals(61, HerbloreData.mix("magic_essence_mix").level());
        assertEquals(43.0, HerbloreData.mix("magic_essence_mix").xp(), 1e-9);
        com.runecraft.osrs.SkillSet s = new com.runecraft.osrs.SkillSet();
        int prayer = HerbloreData.Effect.ANCIENT_BREW.apply(s);
        assertEquals(2, prayer); // 2 + 10% of Prayer 1
        // Calquat keg: a knife on a calquat fruit; two kegs take a vat, four pints each.
        com.runecraft.osrs.Recipe keg = com.runecraft.osrs.Recipes.get("cut_calquat_keg");
        assertEquals("knife", keg.requiredTool());
        assertEquals(BrewingData.PINTS, BrewingData.KEGS_PER_VAT * BrewingData.KEG_PINTS);
        assertEquals("dragon_bitter_m", BrewingData.pintOfKeg(BrewingData.kegOf("dragon_bitter_m")));
        assertTrue(ItemCatalog.exists(BrewingData.kegOf("asgarnian_ale")));
    }
}
