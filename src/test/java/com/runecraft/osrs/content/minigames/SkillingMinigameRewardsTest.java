package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
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

/** Wiki numbers behind Wintertodt, Tempoross, Guardians of the Rift and the Volcanic Mine. */
class SkillingMinigameRewardsTest {
    private static final WintertodtRewards.Player MAXED = new WintertodtRewards.Player(99, 99, 99, 99, 99, 99, false, false, false);

    // ------------------------------------------------------------------ Wintertodt

    @Test
    void wintertodtRollCount() {
        assertEquals(0, WintertodtRewards.rolls(499));
        assertEquals(2, WintertodtRewards.rolls(500));
        assertEquals(2, WintertodtRewards.rolls(999));
        assertEquals(3, WintertodtRewards.rolls(1000));
        assertEquals(28, WintertodtRewards.rolls(13_500));
        // No cap since the reward cart (the old supply crate stopped at 28).
        assertEquals(81, WintertodtRewards.rolls(40_000));
        // 750 points: 2 rolls and a 50% chance of a third (1% per 5 points past a milestone).
        assertEquals(3, WintertodtRewards.rolls(750, 0.49));
        assertEquals(2, WintertodtRewards.rolls(750, 0.51));
        assertEquals(3, WintertodtRewards.rolls(1000, 0.99));
        assertEquals(29, WintertodtRewards.rolls(13_750, 0.0));
        assertEquals(3.0, WintertodtRewards.expectedRolls(1000), 1e-9);
        assertEquals(0.0, WintertodtRewards.expectedRolls(400), 1e-9);
    }

    @Test
    void wintertodtSkillTablesFollowTheWikiInterpolation() {
        WintertodtRewards.Table crafting = WintertodtRewards.TABLES.get(0);
        assertEquals(Skill.CRAFTING, crafting.skill());
        // Uncut diamond: low 10, high 120 -> (floor(10 + 110 * 98 / 98) + 1) / 256 at level 99.
        assertEquals(121 / 256.0, WintertodtRewards.lineChance(crafting.lines().get(0), 99), 1e-9);
        assertEquals(11 / 256.0, WintertodtRewards.lineChance(crafting.lines().get(0), 1), 1e-9);
        assertEquals(1.0, WintertodtRewards.lineChance(crafting.lines().get(3), 50), 1e-9);
        for (WintertodtRewards.Table t : WintertodtRewards.TABLES) {
            for (int level : new int[]{1, 30, 50, 75, 99}) {
                double sum = 0;
                for (double c : WintertodtRewards.tableChances(t, level)) {
                    assertTrue(c >= 0, t.skill() + " " + level);
                    sum += c;
                }
                assertEquals(1.0, sum, 1e-9, t.skill() + " " + level);
            }
        }
        assertEquals(6, WintertodtRewards.TABLES.size());
        assertEquals(25, WintertodtRewards.TABLES.size() * WintertodtRewards.SKILL_WEIGHT + WintertodtRewards.COINS_WEIGHT
                + WintertodtRewards.DYNAMITE_WEIGHT + WintertodtRewards.SALTPETRE_WEIGHT);
        for (String id : WintertodtRewards.allItems()) {
            assertTrue(ItemCatalog.exists(id), id);
        }
    }

    @Test
    void wintertodtUniqueRates() {
        assertEquals(5000, WintertodtRewards.PHOENIX_CHANCE);
        assertEquals(10_000, WintertodtRewards.DRAGON_AXE_CHANCE);
        assertEquals(1000, WintertodtRewards.TOME_CHANCE);
        assertEquals(150, WintertodtRewards.PYROMANCER_CHANCE);
        assertEquals(150, WintertodtRewards.WARM_GLOVES_CHANCE);
        assertEquals(150, WintertodtRewards.BRUMA_TORCH_CHANCE);
        assertEquals(45, WintertodtRewards.BURNT_PAGE_CHANCE);
        Random r = new Random(7);
        Map<String, Integer> seen = new HashMap<>();
        int n = 300_000;
        WintertodtRewards.Player owner = new WintertodtRewards.Player(99, 99, 99, 99, 99, 99, true, true, true);
        for (int i = 0; i < n; i++) {
            MinigameReward rw = WintertodtRewards.roll(owner, r::nextDouble);
            assertTrue(rw.amount() >= 1, rw.item());
            seen.merge(rw.item(), 1, Integer::sum);
        }
        assertFalse(seen.containsKey("phoenix"));
        assertFalse(seen.containsKey("warm_gloves"));
        assertFalse(seen.containsKey("bruma_torch"));
        assertTrue(seen.getOrDefault("magic_seed", 0) > 0);
        // Burnt pages ~1/45, a pyromancer piece ~1/150 in total, coins ~5/25 of the rest.
        assertEquals(1 / 45.0, seen.getOrDefault("burnt_page", 0) / (double) n, 0.003);
        int pyro = 0;
        for (String piece : WintertodtRewards.PYROMANCER) {
            pyro += seen.getOrDefault(piece, 0);
        }
        assertEquals(1 / 150.0, pyro / (double) n, 0.0015);
        assertEquals(0.2 * 0.95, seen.getOrDefault("coins", 0) / (double) n, 0.01);
        int rewards = WintertodtRewards.rewards(13_500, MAXED, new Random(3)::nextDouble).size();
        assertEquals(28, rewards);
    }

    // ------------------------------------------------------------------ Tempoross

    @Test
    void temporossPermits() {
        assertEquals(0, TemporossRewards.permits(1999));
        assertEquals(1, TemporossRewards.permits(2000));
        assertEquals(1, TemporossRewards.permits(2699));
        assertEquals(2, TemporossRewards.permits(2700));
        assertEquals(3, TemporossRewards.permits(3400));
        assertEquals(11, TemporossRewards.permits(9000));
        assertEquals(18, TemporossRewards.permits(13_900));
        // No cap of 8: the 32,000-point maximum gives 43.
        assertEquals(43, TemporossRewards.permits(32_000));
        assertEquals(43, TemporossRewards.permits(50_000));
        // 350 leftover points = 50% chance of an extra permit.
        assertEquals(2, TemporossRewards.permits(2350, 0.49));
        assertEquals(1, TemporossRewards.permits(2350, 0.51));
        assertEquals(0, TemporossRewards.permits(1500, 0.0));
        assertEquals(2, MinigameRules.Tempoross.permits(2700));
    }

    @Test
    void temporossExperience() {
        assertEquals(450, TemporossRewards.xpModifier(35));
        assertEquals(742, TemporossRewards.xpModifier(69));
        assertEquals(890, TemporossRewards.xpModifier(70));
        assertEquals(1000, TemporossRewards.xpModifier(99));
        assertEquals(10.0, TemporossRewards.catchXp(99), 1e-9);
        assertEquals(4.5, TemporossRewards.catchXp(35), 1e-9);
        assertEquals(100.0, TemporossRewards.cannonXp(99), 1e-9);
        assertEquals(110.0, TemporossRewards.spiritPoolXp(99), 1e-9);
        assertEquals(10.0, MinigameRules.Tempoross.catchXp(99), 1e-9);
    }

    @Test
    void temporossRewardPool() {
        assertEquals(0.25, TemporossRewards.uniqueChance("spirit_flakes"), 1e-9);
        assertEquals(1 / 20.0, TemporossRewards.uniqueChance(TemporossRewards.CASKET), 1e-9);
        assertEquals(1 / 400.0, TemporossRewards.uniqueChance("fish_barrel"), 1e-9);
        assertEquals(1 / 400.0, TemporossRewards.uniqueChance("tackle_box"), 1e-9);
        assertEquals(1 / 1600.0, TemporossRewards.uniqueChance("big_harpoonfish"), 1e-9);
        assertEquals(1 / 1600.0, TemporossRewards.uniqueChance("tome_of_water"), 1e-9);
        assertEquals(1 / 8000.0, TemporossRewards.uniqueChance("tiny_tempor"), 1e-9);
        assertEquals(1 / 8000.0, TemporossRewards.uniqueChance("dragon_harpoon"), 1e-9);
        assertEquals(149 / 8000.0, TemporossRewards.uniqueChance("soaked_page"), 1e-9);
        assertTrue(TemporossRewards.fishFor(34).isEmpty());
        assertEquals(5, TemporossRewards.fishFor(35).size());
        assertEquals("raw_herring", TemporossRewards.fishFor(39).get(0).item());
        assertEquals("raw_pike", TemporossRewards.fishFor(46).get(0).item());
        assertEquals("raw_manta_ray", TemporossRewards.fishFor(81).get(4).item());
        for (TemporossRewards.FishBracket b : TemporossRewards.FISH_BRACKETS) {
            int weight = 0;
            for (TemporossRewards.Fish f : b.fish()) {
                assertTrue(ItemCatalog.exists(f.item()), f.item());
                weight += f.weight();
            }
            assertEquals(TemporossRewards.FISH_WEIGHT, weight, "bracket " + b.level());
        }
        Random r = new Random(11);
        int barrels = 0;
        int pages25 = 0;
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            MinigameReward rw = TemporossRewards.roll(99, List.of("fish_barrel"), r::nextDouble);
            assertTrue(ItemCatalog.exists(rw.item()), rw.item());
            if (rw.item().equals("fish_barrel")) {
                barrels++;
            }
            if (rw.item().equals("soaked_page") && rw.amount() == 25) {
                pages25++;
            }
        }
        assertEquals(0, barrels);
        assertEquals(1 / 400.0, pages25 / (double) n, 0.0008);
    }

    // ------------------------------------------------------------------ Guardians of the Rift

    @Test
    void guardiansEnergyAndPoints() {
        assertEquals(1, GuardiansRewards.STONES_PER_ESSENCE);
        assertEquals(26, GuardiansRewards.stoneEnergy(10, 2));
        assertEquals(0.4, GuardiansRewards.powerPerStone(1), 1e-9);
        assertEquals(100.0, 250 * GuardiansRewards.powerPerStone(1), 1e-9);
        assertEquals(100.0, 2500 * GuardiansRewards.powerPerStone(10), 1e-9);
        // 300 total energy since November 2024 (150 before).
        assertFalse(GuardiansRewards.qualifies(200, 99));
        assertTrue(GuardiansRewards.qualifies(200, 100));
        // 356 energy: 3 points and a 56% chance of a 4th.
        assertEquals(3, GuardiansRewards.points(356));
        assertEquals(4, GuardiansRewards.points(356, 0.55));
        assertEquals(3, GuardiansRewards.points(356, 0.57));
        assertEquals(10, GuardiansRewards.points(5000));
        assertEquals(2, GuardiansRewards.searches(2, 7));
        assertEquals(0, GuardiansRewards.searches(-1, 7));
        assertEquals(4455.0, GuardiansRewards.completionXp(99), 1e-9);
        // Energy caps: 1,000 per type and 1,200 in all.
        int[] e = new int[2];
        assertTrue(GuardiansRewards.addEnergy(e, 990, 0));
        assertTrue(GuardiansRewards.addEnergy(e, 50, 150));
        assertEquals(1000, e[0]);
        assertEquals(150, e[1]);
        assertTrue(GuardiansRewards.addEnergy(e, 0, 500));
        assertEquals(200, e[1]);
        assertFalse(GuardiansRewards.addEnergy(e, 10, 10));
    }

    @Test
    void guardiansRewardsGuardianTable() {
        List<String> all = GuardiansRewards.POUCHES;
        assertEquals(140, GuardiansRewards.mainTotal(List.of()));
        assertEquals(125, GuardiansRewards.mainTotal(all));
        assertEquals(125, GuardiansRewards.mainTotal(List.of("colossal_pouch")));
        assertEquals(4 / 125.0, GuardiansRewards.chance("air_rune", all), 1e-9);
        assertEquals(10 / 125.0, GuardiansRewards.chance("blood_rune", all), 1e-9);
        assertEquals(16 / 140.0, GuardiansRewards.chance(GuardiansRewards.TALISMANS, List.of()), 1e-9);
        assertEquals(15 / 140.0, GuardiansRewards.chance(GuardiansRewards.POUCH, List.of()), 1e-9);
        assertEquals(5 / 140.0, GuardiansRewards.chance("intricate_pouch", List.of()), 1e-9);
        // Pearls 18/140: one search in 7.78 without pouches owned, 6.94 with them all.
        assertEquals(7.778, 1 / GuardiansRewards.chance("abyssal_pearls", List.of()), 0.01);
        assertEquals(6.944, 1 / GuardiansRewards.chance("abyssal_pearls", all), 0.01);
        assertEquals(0.0, GuardiansRewards.chance(GuardiansRewards.POUCH, all), 1e-9);
        int talismanWeight = 0;
        for (Object[] row : GuardiansRewards.TALISMAN_TABLE) {
            talismanWeight += (Integer) row[1];
        }
        assertEquals(35, talismanWeight);
        assertEquals(Integer.valueOf(77), GuardiansRewards.runeLevel("blood_rune"));
        assertNull(GuardiansRewards.runeLevel("abyssal_pearls"));
        for (String id : GuardiansRewards.allItems()) {
            assertTrue(ItemCatalog.exists(id), id);
        }
        Random r = new Random(5);
        Map<String, Integer> seen = new HashMap<>();
        int n = 400_000;
        List<String> owned = List.of("small_pouch", "medium_pouch", "large_pouch", "giant_pouch", "atlaxs_diary");
        for (int i = 0; i < n; i++) {
            for (MinigameReward rw : GuardiansRewards.search(99, owned, r::nextDouble)) {
                assertFalse(GuardiansRewards.POUCHES.contains(rw.item()), rw.item());
                seen.merge(rw.item(), 1, Integer::sum);
                if (rw.item().equals("air_rune")) {
                    assertTrue(rw.amount() >= 400 && rw.amount() <= 500);
                }
                if (rw.item().equals("abyssal_pearls")) {
                    assertTrue(rw.amount() >= 14 && rw.amount() <= 16);
                }
            }
        }
        assertEquals(0, seen.getOrDefault("atlaxs_diary", 0));
        assertEquals(1 / 300.0, seen.getOrDefault("abyssal_needle", 0) / (double) n, 0.0006);
        assertEquals(1 / 4000.0, seen.getOrDefault("abyssal_protector", 0) / (double) n, 0.0001);
        assertEquals(1 / 200.0, seen.getOrDefault("catalytic_talisman", 0) / (double) n, 0.0006);
        // Elemental talismans only come from the talisman table: 16/125 x 1/35.
        assertEquals(16 / 125.0 / 35 * (1 - 1 / 200.0 - 1 / 300.0), seen.getOrDefault("elemental_talisman", 0) / (double) n, 0.0002);
        // A new player gets their first missing pouch (small first) and Atlax's diary.
        Random fresh = new Random(3);
        Map<String, Integer> first = new HashMap<>();
        for (int i = 0; i < 20_000; i++) {
            for (MinigameReward rw : GuardiansRewards.search(99, List.of(), fresh::nextDouble)) {
                first.merge(rw.item(), 1, Integer::sum);
            }
        }
        assertTrue(first.getOrDefault("small_pouch", 0) > 0);
        assertEquals(0, first.getOrDefault("medium_pouch", 0));
        assertEquals(1 / 20.0, first.getOrDefault("atlaxs_diary", 0) / 20_000.0, 0.005);
        // Runes above the player's level: amount x (level / required)^4, at least a tenth.
        Random low = new Random(1);
        for (int i = 0; i < 5000; i++) {
            for (MinigameReward rw : GuardiansRewards.search(27, all, low::nextDouble)) {
                if (rw.item().equals("death_rune")) {
                    assertTrue(rw.amount() <= 36, "" + rw.amount());
                }
            }
        }
        assertFalse(GuardiansRewards.openIntricatePouch(new Random(2)::nextDouble).isEmpty());
    }

    @Test
    void guardiansCellsAndBarriers() {
        assertEquals(GuardiansRift.Cell.WEAK, GuardiansRift.cellFor("air"));
        assertEquals(GuardiansRift.Cell.WEAK, GuardiansRift.cellFor("body"));
        assertEquals(GuardiansRift.Cell.MEDIUM, GuardiansRift.cellFor("chaos"));
        assertEquals(GuardiansRift.Cell.STRONG, GuardiansRift.cellFor("law"));
        assertEquals(GuardiansRift.Cell.OVERCHARGED, GuardiansRift.cellFor("blood"));
        for (Object[] a : MinigameRules.Guardians.ALTARS) {
            assertNotNull(GuardiansRift.cellFor((String) a[0]));
        }
        for (GuardiansRift.Cell c : GuardiansRift.Cell.values()) {
            assertTrue(ItemCatalog.exists(c.item), c.item);
            // Creating a guardian gives 2x+1 energy, x being the strengthening energy (weak: 2.5).
            if (c != GuardiansRift.Cell.WEAK) {
                assertEquals(2 * c.strengthenEnergy + 1, c.guardianEnergy);
            }
        }
        assertTrue(ItemCatalog.exists(GuardiansRift.UNCHARGED_CELL));
        GuardiansRift rift = new GuardiansRift();
        // Any cell on an inactive tile raises a weak barrier, for 17/20/24/30 energy of each type by the cell (wiki).
        GuardiansRift.Use u = rift.useCell(0, GuardiansRift.Cell.STRONG, 0, 0);
        assertEquals(24, u.elemental());
        assertEquals(24, u.catalytic());
        assertEquals(180.0, u.xp(), 1e-9);
        assertEquals(50, rift.hp(0));
        // A higher-tier cell strengthens one tier at a time.
        u = rift.useCell(0, GuardiansRift.Cell.OVERCHARGED, 0, 0);
        assertEquals(22, u.elemental());
        assertEquals(GuardiansRift.Cell.MEDIUM.ordinal(), rift.tier(0));
        assertEquals(100, rift.hp(0));
        // A lower or equal cell recharges: weak heals 10 for 2 energy, within the per-game caps.
        for (int t = 0; t < 1000; t++) {
            rift.tick(GuardiansRift.PREP_TICKS + t * GuardiansRift.ATTACK_INTERVAL, 1, 0, () -> 0.0);
            if (rift.hp(0) <= 80) {
                break;
            }
        }
        int before = rift.hp(0);
        u = rift.useCell(0, GuardiansRift.Cell.WEAK, 0, 0);
        assertEquals(2, u.elemental());
        assertEquals(Math.min(100, before + 10), rift.hp(0));
        assertEquals(0, rift.useCell(0, GuardiansRift.Cell.WEAK, 0, GuardiansRift.RECHARGE_ENERGY_CAP).elemental());
        assertEquals(0.0, rift.useCell(0, GuardiansRift.Cell.WEAK, GuardiansRift.RECHARGE_XP_CAP, 0).xp(), 1e-9);
        // Rift guardians give energy of one type only.
        u = rift.assemble(GuardiansRift.Cell.MEDIUM, true);
        assertEquals(0, u.elemental());
        assertEquals(15, u.catalytic());
        assertEquals(1, rift.activeRiftGuardians());
        // No damage during the two-minute preparation.
        GuardiansRift calm = new GuardiansRift();
        calm.useCell(1, GuardiansRift.Cell.WEAK, 0, 0);
        for (long t = 0; t < GuardiansRift.PREP_TICKS; t++) {
            calm.tick(t, 5, 0, () -> 0.0);
        }
        assertEquals(50, calm.hp(1));
        // Unprotected, the barrier breaks, the tile needs 12 fragments, then the Great Guardian takes the hits.
        long t = GuardiansRift.PREP_TICKS;
        while (calm.tier(1) != -2) {
            calm.tick(t++, 5, 0, () -> 0.0);
        }
        assertEquals("broken", calm.describe(1));
        assertFalse(calm.useCell(1, GuardiansRift.Cell.WEAK, 0, 0).ok());
        assertEquals(GuardiansRift.REPAIR_ENERGY, calm.repair(1).elemental());
        assertEquals("inactive", calm.describe(1));
        while (calm.guardianHealth() > 0) {
            calm.tick(t++, 5, 0, () -> 0.0);
        }
        // The 60% surge damages every barrier by 50.
        GuardiansRift surge = new GuardiansRift();
        surge.useCell(2, GuardiansRift.Cell.WEAK, 0, 0);
        surge.useCell(2, GuardiansRift.Cell.STRONG, 0, 0);
        surge.tick(1, 1, 60, () -> 0.0);
        assertEquals(50, surge.hp(2));
    }

    // ------------------------------------------------------------------ Volcanic Mine

    @Test
    void volcanicMine() {
        assertEquals(50, VolcanicMine.MINING_LEVEL);
        assertEquals(30, VolcanicMine.ENTRY_NUMULITE);
        assertEquals(3000, VolcanicMine.PERMANENT_ACCESS_NUMULITE);
        assertEquals(1000, VolcanicMine.GAME_TICKS);
        assertEquals(-1, VolcanicMine.maxTier(49));
        assertEquals(3, VolcanicMine.maxTier(50));
        assertEquals(4, VolcanicMine.maxTier(69));
        assertEquals(6, VolcanicMine.maxTier(85));
        assertEquals(6, VolcanicMine.maxTier(99));
        // Fragment table (OSRS Wiki): iron 15/10, silver 20/20, coal 25/30, gold 34/40, mithril 44/60, adamantite 55/100, runite 70/150.
        assertEquals(List.of(15, 20, 25, 34, 44, 55, 70), java.util.Arrays.stream(VolcanicMine.FRAGMENT_POINTS).boxed().toList());
        assertEquals(10.0, VolcanicMine.FRAGMENT_XP[0], 1e-9);
        assertEquals(150.0, VolcanicMine.FRAGMENT_XP[6], 1e-9);
        VolcanicMine.Hit miss = VolcanicMine.hit(49, 5, () -> 0.0);
        assertEquals(-1, miss.tier());
        assertEquals(1, miss.points());
        assertEquals(5.0, miss.xp(), 1e-9);
        // Fragment chance by boulder size: 1/20, 1/12, 1/8, 1/5, 1/3.
        assertEquals(1 / 20.0, VolcanicMine.fragmentChance(1), 1e-9);
        assertEquals(1 / 3.0, VolcanicMine.fragmentChance(5), 1e-9);
        assertEquals(40, VolcanicMine.stageHp(1, 1));
        assertEquals(120, VolcanicMine.stageHp(4, 3));
        assertEquals(180, VolcanicMine.stageHp(5, 3));
        Random r = new Random(9);
        int[] seen = new int[7];
        for (int i = 0; i < 200_000; i++) {
            VolcanicMine.Hit h = VolcanicMine.hit(60, 1 + i % 5, r::nextDouble);
            assertTrue(h.tier() <= VolcanicMine.maxTier(60));
            if (h.tier() >= 0) {
                seen[h.tier()]++;
            }
        }
        assertEquals(0, seen[5]);
        assertTrue(seen[0] > 0 && seen[4] > 0);
        // At 99 a fragment is runite with the runite roll's chance (low/high -10/80).
        Random r99 = new Random(2);
        int runite = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            if (VolcanicMine.fragmentTier(99, r99::nextDouble) == 6) {
                runite++;
            }
        }
        assertEquals(com.runecraft.osrs.Chance.lerp(99, -10, 80), runite / (double) n, 0.004);
        assertTrue(VolcanicMine.fragmentChance(5) > VolcanicMine.fragmentChance(1));
        assertEquals(57_525, VolcanicMine.petChance(99));
        // Pressure within 40-60% keeps the mine stable; each 10% outside costs a point.
        assertEquals(1, VolcanicMine.stabilityDelta(new int[]{40, 50, 60}));
        assertEquals(-1, VolcanicMine.stabilityDelta(new int[]{35, 50, 50}));
        assertEquals(-8, VolcanicMine.stabilityDelta(new int[]{0, 100, 50}));
        // The last 30 seconds: no mining, stability pinned at 5%.
        assertTrue(VolcanicMine.workable(949));
        assertFalse(VolcanicMine.workable(950));
        assertEquals(5, VolcanicMine.stability(80, new int[]{50, 50, 50}, 960));
        assertEquals(81, VolcanicMine.stability(80, new int[]{50, 50, 50}, 100));
        // Capping: 50 points a time, 300 a game.
        assertEquals(50, VolcanicMine.capPoints(0));
        assertEquals(0, VolcanicMine.capPoints(300));
        assertEquals(100, VolcanicMine.COMPLETION_POINTS);
        assertEquals(100, VolcanicMine.ventStep(99, true));
        assertEquals(0, VolcanicMine.ventStep(1, false));
        // Petrified Pete's Ore Shop prices.
        RewardShop pete = RewardShops.get("volcanic_mine");
        assertNotNull(pete);
        Map<String, Integer> prices = new HashMap<>();
        for (RewardShop.Entry e : pete.entries()) {
            assertTrue(ItemCatalog.exists(e.item()), e.item());
            prices.put(e.item(), e.costs().get(0).amount());
            assertEquals(MinigameCurrencies.VOLCANIC_MINE, e.costs().get(0).currency());
        }
        assertEquals(855, prices.get("runite_ore"));
        assertEquals(30, prices.get("iron_ore"));
        assertEquals(39_000, prices.get("prospector_jacket"));
        Minigame vm = Minigame.VOLCANIC_MINE;
        assertEquals(Skill.MINING, vm.skill());
        assertEquals(50, vm.level());
        assertEquals(PlaceDef.Style.NONE, vm.style());
        assertNotNull(ContentRegistry.blocks().stream().filter(b -> b.id().equals("vm_boulder")).findFirst().orElse(null));
        assertTrue(ItemCatalog.exists("numulite"));
        assertTrue(ItemCatalog.exists("pet_rock_golem"));
        assertEquals("Volcanic Mine points", MinigameCurrencies.name(MinigameCurrencies.VOLCANIC_MINE));
    }

    @Test
    void rewardsGuardianPortalTalismansAndVolcanicExtras() {
        // Searches: a point of each type first, else 25 abyssal pearls; big-search does five.
        assertEquals(GuardiansRewards.Payment.POINTS, GuardiansRewards.payment(1, 1, 0));
        assertEquals(GuardiansRewards.Payment.PEARLS, GuardiansRewards.payment(3, 0, 25));
        assertEquals(GuardiansRewards.Payment.NONE, GuardiansRewards.payment(0, 5, 24));
        assertEquals(5, GuardiansRewards.BIG_SEARCH);
        // Polyelemental stones give 3 energy; portal talismans 1/250 per essence.
        assertEquals(3, GuardiansRewards.stoneEnergy(0, 1));
        assertEquals(250, GuardiansRewards.PORTAL_TALISMAN_CHANCE);
        for (Object[] a : MinigameRules.Guardians.ALTARS) {
            assertTrue(ItemCatalog.exists(GuardiansRewards.portalTalisman((String) a[0])), (String) a[0]);
        }
        assertTrue(ItemCatalog.exists(GuardiansRewards.POLY_STONE));
        for (Object[] c : GuardiansRewards.COMBINATIONS) {
            assertTrue(ItemCatalog.exists((String) c[3]), (String) c[3]);
        }
        // Lava beast: 60 hp -> 6 points + 3 for the kill; Petrified Pete's ores come noted.
        assertEquals(9, VolcanicMine.lavaBeastPoints());
        assertEquals(0.0, VolcanicMine.earthquakeChance(80), 1e-9);
        assertTrue(VolcanicMine.earthquakeChance(10) > VolcanicMine.earthquakeChance(40));
        assertTrue(RewardShops.noted("volcanic_mine", "runite_ore"));
        assertFalse(RewardShops.noted("volcanic_mine", "prospector_helmet"));
    }
}
