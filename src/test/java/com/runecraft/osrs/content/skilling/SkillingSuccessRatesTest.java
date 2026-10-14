package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.BurnRates;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Skilling success-rate (low/high) values against the OSRS Wiki's skilling success chart data. */
class SkillingSuccessRatesTest {

    private static Recipe recipe(String id) {
        Recipe r = Recipes.get(id);
        assertNotNull(r, "missing recipe " + id);
        return r;
    }

    private static int stop(String recipe, boolean range, boolean gauntlets) {
        Recipe r = recipe(recipe);
        for (int level = r.level(); level <= 99; level++) {
            if (r.cookingSuccess(level, range, gauntlets) >= 1.0) {
                return level;
            }
        }
        return 100;
    }

    @Test
    void cookingUsesLowHighBurnValues() {
        // Lobster 38/332 on a fire or range (stops at 74), 55/368 with cooking gauntlets (stops at 64).
        assertEquals(Chance.lerp(40, 38, 332), recipe("cook_lobster").cookingSuccess(40, false), 1e-12);
        assertEquals(74, stop("cook_lobster", false, false));
        assertEquals(64, stop("cook_lobster", false, true));
        // Swordfish 18/292 on a fire (86) and 30/310 on a range.
        assertEquals(86, stop("cook_swordfish", false, false));
        assertTrue(stop("cook_swordfish", true, false) < 86);
        // Shark: 1/202 fire, 1/232 range, 15/270 with gauntlets (stops at 94).
        assertEquals(203 / 256.0, recipe("cook_shark").cookingSuccess(99, false), 1e-12);
        assertEquals(233 / 256.0, recipe("cook_shark").cookingSuccess(99, true), 1e-12);
        assertEquals(94, stop("cook_shark", false, true));
        // Old-model regression: shark at level 80 on a fire is ~64%, not 54%.
        assertEquals(Chance.lerp(80, 1, 202), recipe("cook_shark").cookingSuccess(80, false), 1e-12);
        // Classic foods stop where the wiki says.
        assertEquals(34, stop("cook_shrimps", false, false));
        assertEquals(58, stop("cook_stew", false, false));
        assertEquals(68, stop("cook_plain_pizza", false, false));
        assertEquals(74, stop("cook_cake", false, false));
    }

    @Test
    void derivedBurnCurvesStopAtTheStopBurnLevel() {
        for (int stop = 2; stop <= 99; stop++) {
            int[] lh = BurnRates.fromStopLevel(stop);
            assertEquals(2 * lh[0] + 256, lh[1]);
            assertTrue(BurnRates.stopLevel(lh[0], lh[1]) <= stop, "stop " + stop);
        }
        // Monkfish has no table entry: its curve stops exactly at 92 (90 with gauntlets).
        assertNull(BurnRates.of("monkfish"));
        assertEquals(92, stop("cook_monkfish", false, false));
        assertEquals(90, stop("cook_monkfish", false, true));
        // Never-stop food keeps burning.
        assertEquals(100, stop("cook_anglerfish", true, false));
    }

    @Test
    void fishingRates() {
        assertEquals(48, Fish.SHRIMPS.low());
        assertEquals(256, Fish.SHRIMPS.high());
        assertEquals(6, Fish.LOBSTER.low());
        assertEquals(95, Fish.LOBSTER.high());
        // Harpoon fish: plain harpoon values; dragon (x1.2) and crystal (x1.35) harpoons scale low/high, rounded down.
        assertEquals(8, Fish.TUNA.low());
        assertEquals(64, Fish.TUNA.high());
        assertEquals(Chance.lerp(60, 9, 76), Fish.TUNA.chance(60, 1.2), 1e-12);
        assertEquals(Chance.lerp(60, 10, 86), Fish.TUNA.chance(60, 1.35), 1e-12);
        assertEquals(4, Fish.SWORDFISH.low());
        assertEquals(48, Fish.SWORDFISH.high());
        assertEquals(Chance.lerp(70, 4, 57), Fish.SWORDFISH.chance(70, 1.2), 1e-12);
        assertEquals(3, Fish.SHARK.low());
        assertEquals(40, Fish.SHARK.high());
        assertEquals(Chance.lerp(80, 3, 48), Fish.SHARK.chance(80, 1.2), 1e-12);
        assertEquals(Chance.lerp(80, 4, 54), Fish.SHARK.chance(80, 1.35), 1e-12);
        assertEquals(3, Fish.ANGLERFISH.low());
        assertEquals(36, Fish.ANGLERFISH.high());
        assertEquals(48, Fish.MONKFISH.low());
        assertEquals(90, Fish.MONKFISH.high());
        GatheringData.Spot karambwan = GatheringData.spot("karambwan_fishing_spot");
        assertEquals(5, karambwan.catches().get(0).low());
        assertEquals(160, karambwan.catches().get(0).high());
        GatheringData.Spot barb = GatheringData.spot("barbarian_fishing_spot");
        assertEquals(8, barb.catches().get(0).low());
        assertEquals(64, barb.catches().get(0).high());
    }

    @Test
    void miningRates() {
        assertEquals(100, Rock.COPPER.low());
        assertEquals(350, Rock.COPPER.high());
        assertEquals(28, Rock.GEM.low());
        assertEquals(70, Rock.GEM.high());
        assertEquals(-18, Rock.AMETHYST.low());
        assertEquals(10, Rock.AMETHYST.high());
        // Amethyst at 92 Mining: (1 + floor(-18*7/98 + 10*91/98 + 0.5)) / 256 = 9/256.
        assertEquals(9 / 256.0, Chance.lerp(92, Rock.AMETHYST.low(), Rock.AMETHYST.high()), 1e-12);
        assertEquals(60, MiningData.rock("pay_dirt_vein").low());
        assertEquals(105, MiningData.rock("pay_dirt_vein").high());
        assertEquals(95, MiningData.rock("limestone_rock").low());
    }

    @Test
    void sandstoneCascadesFromTheHeaviestSize() {
        MiningData.MineRock sandstone = MiningData.rock("sandstone_rock");
        assertTrue(sandstone.cascades());
        assertEquals("sandstone_10kg", sandstone.yields().get(0).item());
        // A roll of 0 always lands the first (10kg) size; rolls of 0.999 miss every size.
        assertEquals("sandstone_10kg", sandstone.attempt(35, () -> 0.0).item());
        assertNull(sandstone.attempt(35, () -> 0.999));
        // Missing 10kg/5kg/2kg but hitting 1kg.
        Iterator<Double> rolls = List.of(0.99, 0.99, 0.99, 0.0).iterator();
        assertEquals("sandstone_1kg", sandstone.attempt(35, rolls::next).item());
        // Single-roll rocks still use their own low/high.
        MiningData.MineRock limestone = MiningData.rock("limestone_rock");
        assertEquals("limestone", limestone.attempt(10, () -> 0.0).item());
        assertNull(limestone.attempt(10, () -> 0.999));
    }

    @Test
    void woodcuttingUsesExactAxeValues() {
        assertEquals(6, Tree.YEW.low(ToolTier.IRON.woodcuttingMultiplier()));
        assertEquals(19, Tree.YEW.high(ToolTier.IRON.woodcuttingMultiplier()));
        assertEquals(44, Tree.YEW.high(ToolTier.RUNE.woodcuttingMultiplier()));
        assertEquals(5, Tree.MAGIC.low(ToolTier.BLACK.woodcuttingMultiplier()));
        assertEquals(13, Tree.MAGIC.high(ToolTier.BLACK.woodcuttingMultiplier()));
        assertEquals(224, Tree.TREE.low(ToolTier.RUNE.woodcuttingMultiplier()));
        assertEquals(750, Tree.TREE.high(ToolTier.DRAGON.woodcuttingMultiplier()));
        // Teak with a steel axe is 31/93 on the wiki (not 30/92); unknown multipliers scale the bronze values.
        assertEquals(31, Tree.TEAK.low(2.0));
        assertEquals((int) Math.floor(Tree.TEAK.low() * 1.25), Tree.TEAK.low(1.25));
    }

    @Test
    void pickpocketRates() {
        // Paladin 40/170 and hero 39/160 (the wiki's current charts); the full diff is in SuccessRateSnapshotTest.
        assertEquals(170, Thieving.Pickpocket.PALADIN.high());
        assertEquals(160, Thieving.Pickpocket.HERO.high());
        for (TownMonsters.Pickpocket p : TownMonsters.pickpockets()) {
            if (p.name().equals("Paladin")) {
                assertEquals(40, p.low());
                assertEquals(170, p.high());
            }
        }
        for (ThievingData.Target t : ThievingData.TARGETS) {
            if (t.name().equals("Paladin")) {
                assertEquals(170, t.high());
            }
        }
    }
}
