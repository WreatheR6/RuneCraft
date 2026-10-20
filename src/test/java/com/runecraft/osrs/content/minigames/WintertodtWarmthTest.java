package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.minigames.MinigameRules.Wintertodt;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Wintertodt after its 9 October 2024 update (OSRS Wiki "Wintertodt", "Wintertodt/Strategies", "Warmth"): the warmth
 * meter's damage formulas, regeneration and restoration, the energy meter drained only by the pyromancers, the points, and
 * the warm clothing list diffed against {@code src/test/resources/osrs/minigames/wintertodt_warmth.tsv}.
 */
class WintertodtWarmthTest {
    @Test
    void warmClothingMatchesTheWiki() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("src/test/resources/osrs/minigames/wintertodt_warmth.tsv"), StandardCharsets.UTF_8);
        Set<String> wiki = new TreeSet<>();
        int rows = 0;
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            rows++;
            String[] r = line.split("\\|", -1);
            if (!r[2].equals("-")) {
                wiki.add(r[2]);
            }
        }
        assertTrue(rows > 150, "the wiki lists " + rows + " warm items");
        assertEquals(wiki, new TreeSet<>(Wintertodt.WARM_ITEMS));
        assertTrue(Wintertodt.WARM_ITEMS.contains("pyromancer_garb") && Wintertodt.WARM_ITEMS.contains("bruma_torch")
                && Wintertodt.WARM_ITEMS.contains("fire_cape") && Wintertodt.WARM_ITEMS.contains("warm_gloves"));
        assertEquals(4, Wintertodt.warmItems(List.of("pyromancer_hood", "pyromancer_garb", "pyromancer_robe", "pyromancer_boots",
                "warm_gloves")), "four count at most");
        assertEquals(1, Wintertodt.warmItems(List.of("abyssal_whip", "fire_cape", "rune_platebody")));
    }

    @Test
    void warmthDamageFormulas() {
        // Mod Ed: standard floor((16 - W - min(2B, 6)) x 100 / FM); brazier 2x and area 3x floor((10 - W) x 100 / FM).
        assertEquals(32, Wintertodt.standardDamage(50, 0, 0));
        assertEquals(16, Wintertodt.standardDamage(99, 0, 0));
        assertEquals(6, Wintertodt.standardDamage(99, 4, 3));
        assertEquals(6, Wintertodt.standardDamage(99, 4, 4), "B counts at most 3 braziers");
        assertEquals(12, Wintertodt.standardDamage(99, 4, 0), "about 10-15% with four warm items (wiki)");
        assertEquals(2 * (1000 / 99), Wintertodt.brazierDamage(99, 0));
        assertEquals(3 * (600 / 99), Wintertodt.areaDamage(99, 4));
        assertEquals(40, Wintertodt.brazierDamage(50, 0));
        assertEquals(60, Wintertodt.areaDamage(50, 0));
        // Regeneration: 8% a minute (100 ticks) plus 1% per warm item.
        assertEquals(0.08, Wintertodt.warmthRegenPerTick(0), 1e-9);
        assertEquals(0.12, Wintertodt.warmthRegenPerTick(4), 1e-9);
        assertEquals(0.12, Wintertodt.warmthRegenPerTick(7), 1e-9);
        assertEquals(35, Wintertodt.FOOD_WARMTH);
        assertEquals(30, Wintertodt.POTION_WARMTH);
        assertEquals(100, Wintertodt.WARMTH_MAX);
    }

    @Test
    void energyIsTheMeterThePyromancersDrain() {
        assertEquals(100, Wintertodt.energy(1));
        assertEquals(100, Wintertodt.energy(40), "the meter doesn't grow with the players");
        // 1% every 14 ticks per pyromancer: 3 minutes 30 seconds with all four braziers lit all game (wiki).
        assertEquals(350, Wintertodt.MAX_ENERGY * Wintertodt.PYROMANCER_DRAIN_TICKS / 4);
        assertEquals(35, Wintertodt.REGAIN_TICKS);
        // Standard attacks when a roll up to the max energy is lower than the energy left; brazier attacks when higher.
        assertTrue(Wintertodt.attacksPlayers(10, 90));
        assertFalse(Wintertodt.attacksPlayers(95, 90));
        assertTrue(Wintertodt.attacksBraziers(95, 90));
        assertFalse(Wintertodt.attacksBraziers(10, 90));
        assertEquals(14, Wintertodt.PYROMANCER_HP);
        assertEquals(2, Wintertodt.PYROMANCER_HP / Wintertodt.PYROMANCER_HIT, "two hits incapacitate a pyromancer");
    }

    @Test
    void pointsAndExperience() {
        assertEquals(75, Wintertodt.POINTS_HEAL_PYROMANCER, "75 since October 2024 (was 30)");
        assertEquals(25, Wintertodt.POINTS_LIGHT);
        assertEquals(10, Wintertodt.POINTS_ROOT);
        assertEquals(25, Wintertodt.POINTS_KINDLING);
        assertEquals(25, Wintertodt.POINTS_FIX);
        assertEquals(9.9, Wintertodt.potionXp(99), 1e-9);
        assertEquals(300.0, Wintertodt.lightXp(50), 1e-9);
        assertTrue(ItemCatalog.get("tinderbox") != null);
    }
}
