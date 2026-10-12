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
        assertEquals(28, WintertodtRewards.rolls(40_000));
        // 750 points: 2 rolls and a 50% chance of a third (1% per 5 points past a milestone).
        assertEquals(3, WintertodtRewards.rolls(750, 0.49));
        assertEquals(2, WintertodtRewards.rolls(750, 0.51));
        assertEquals(3, WintertodtRewards.rolls(1000, 0.99));
        assertEquals(28, WintertodtRewards.rolls(13_750, 0.0));
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
        assertEquals(1 / 20.0, TemporossRewards.uniqueChance("casket"), 1e-9);
        assertEquals(1 / 400.0, TemporossRewards.uniqueChance("fish_barrel"), 1e-9);
        assertEquals(1 / 400.0, TemporossRewards.uniqueChance("tackle_box"), 1e-9);
        assertEquals(1 / 1600.0, TemporossRewards.uniqueChance("big_harpoonfish"), 1e-9);
        assertEquals(1 / 1600.0, TemporossRewards.uniqueChance("tome_of_water"), 1e-9);
        assertEquals(1 / 8000.0, TemporossRewards.uniqueChance("tiny_tempor"), 1e-9);
        assertEquals(1 / 8000.0, TemporossRewards.uniqueChance("dragon_harpoon"), 1e-9);
        assertEquals(149 / 8000.0, TemporossRewards.uniqueChance("soaked_page"), 1e-9);
        assertTrue(TemporossRewards.fishFor(34).isEmpty());
        assertEquals(1, TemporossRewards.fishFor(35).size());
        assertEquals(7, TemporossRewards.fishFor(81).size());
        for (TemporossRewards.Fish f : TemporossRewards.FISH) {
            assertTrue(ItemCatalog.exists(f.item()), f.item());
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
        assertFalse(GuardiansRewards.qualifies(100, 49));
        assertTrue(GuardiansRewards.qualifies(100, 50));
        // 356 energy: 3 points and a 56% chance of a 4th.
        assertEquals(3, GuardiansRewards.points(356));
        assertEquals(4, GuardiansRewards.points(356, 0.55));
        assertEquals(3, GuardiansRewards.points(356, 0.57));
        assertEquals(10, GuardiansRewards.points(5000));
        assertEquals(2, GuardiansRewards.searches(2, 7));
        assertEquals(0, GuardiansRewards.searches(-1, 7));
        assertEquals(4455.0, GuardiansRewards.completionXp(99), 1e-9);
    }

    @Test
    void guardiansRewardsGuardianTable() {
        assertEquals(140, GuardiansRewards.mainTotal(List.of()));
        assertEquals(125, GuardiansRewards.mainTotal(GuardiansRewards.POUCHES));
        assertEquals(4 / 125.0, GuardiansRewards.chance("air_rune", GuardiansRewards.POUCHES), 1e-9);
        assertEquals(10 / 125.0, GuardiansRewards.chance("blood_rune", GuardiansRewards.POUCHES), 1e-9);
        // Pearls 18/140: one search in 7.78 without pouches owned, 6.94 with them all.
        assertEquals(7.778, 1 / GuardiansRewards.chance("abyssal_pearls", List.of()), 0.01);
        assertEquals(6.944, 1 / GuardiansRewards.chance("abyssal_pearls", GuardiansRewards.POUCHES), 0.01);
        assertEquals(0.0, GuardiansRewards.chance("giant_pouch", GuardiansRewards.POUCHES), 1e-9);
        assertEquals(Integer.valueOf(77), GuardiansRewards.runeLevel("blood_rune"));
        assertNull(GuardiansRewards.runeLevel("abyssal_pearls"));
        for (String id : GuardiansRewards.allItems()) {
            assertTrue(ItemCatalog.exists(id), id);
        }
        Random r = new Random(5);
        int needles = 0;
        int pets = 0;
        int n = 400_000;
        for (int i = 0; i < n; i++) {
            MinigameReward rw = GuardiansRewards.search(99, GuardiansRewards.POUCHES, r::nextDouble);
            assertFalse(GuardiansRewards.POUCHES.contains(rw.item()), rw.item());
            if (rw.item().equals("abyssal_needle")) {
                needles++;
            }
            if (rw.item().equals("abyssal_protector")) {
                pets++;
            }
            if (rw.item().equals("air_rune")) {
                assertTrue(rw.amount() >= 400 && rw.amount() <= 500);
            }
        }
        assertEquals(1 / 300.0, needles / (double) n, 0.0006);
        assertEquals(1 / 4000.0, pets / (double) n, 0.0001);
        // Runes above the player's level come in smaller stacks.
        Random low = new Random(1);
        for (int i = 0; i < 5000; i++) {
            MinigameReward rw = GuardiansRewards.search(27, GuardiansRewards.POUCHES, low::nextDouble);
            if (rw.item().equals("death_rune")) {
                assertTrue(rw.amount() <= 60, "" + rw.amount());
            }
        }
        assertFalse(GuardiansRewards.openIntricatePouch(new Random(2)::nextDouble).isEmpty());
    }

    // ------------------------------------------------------------------ Volcanic Mine

    @Test
    void volcanicMine() {
        assertEquals(50, VolcanicMine.MINING_LEVEL);
        assertEquals(30, VolcanicMine.ENTRY_NUMULITE);
        assertEquals(3000, VolcanicMine.PERMANENT_ACCESS_NUMULITE);
        assertEquals(1000, VolcanicMine.GAME_TICKS);
        assertEquals(-1, VolcanicMine.maxTier(49));
        assertEquals(0, VolcanicMine.maxTier(50));
        assertEquals(7, VolcanicMine.maxTier(85));
        assertEquals(7, VolcanicMine.maxTier(99));
        assertEquals(15, VolcanicMine.FRAGMENT_POINTS[0]);
        assertEquals(10.0, VolcanicMine.FRAGMENT_XP[0], 1e-9);
        assertEquals(70, VolcanicMine.FRAGMENT_POINTS[7]);
        assertEquals(150.0, VolcanicMine.FRAGMENT_XP[7], 1e-9);
        VolcanicMine.Hit miss = VolcanicMine.hit(49, 6, () -> 0.0);
        assertEquals(-1, miss.tier());
        assertEquals(1, miss.points());
        assertEquals(5.0, miss.xp(), 1e-9);
        Random r = new Random(9);
        for (int i = 0; i < 2000; i++) {
            VolcanicMine.Hit h = VolcanicMine.hit(60, 1 + i % 6, r::nextDouble);
            assertTrue(h.tier() <= VolcanicMine.maxTier(60));
        }
        assertTrue(VolcanicMine.fragmentChance(6) > VolcanicMine.fragmentChance(1));
        assertEquals(57_525, VolcanicMine.petChance(99));
        assertEquals(2, VolcanicMine.stabilityDelta(new int[]{50, 50, 50}));
        assertEquals(-1, VolcanicMine.stabilityDelta(new int[]{0, 100, 50}));
        assertEquals(100, VolcanicMine.ventStep(99, true));
        assertEquals(0, VolcanicMine.ventStep(1, false));
        Minigame vm = Minigame.VOLCANIC_MINE;
        assertEquals(Skill.MINING, vm.skill());
        assertEquals(50, vm.level());
        assertEquals(PlaceDef.Style.NONE, vm.style());
        assertNotNull(ContentRegistry.blocks().stream().filter(b -> b.id().equals("vm_boulder")).findFirst().orElse(null));
        assertTrue(ItemCatalog.exists("numulite"));
        assertTrue(ItemCatalog.exists("pet_rock_golem"));
        assertEquals("Volcanic Mine points", MinigameCurrencies.name(MinigameCurrencies.VOLCANIC_MINE));
    }
}
