package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The dwarf multicannon against the OSRS Wiki ("Dwarf multicannon", "Steel cannonball", "Combat Achievements"). */
class MulticannonTest {
    @Test
    void capacityFollowsCombatAchievementTiers() {
        assertEquals(30, Multicannon.capacity(null));
        assertEquals(30, Multicannon.capacity(Tier.EASY));
        assertEquals(35, Multicannon.capacity(Tier.MEDIUM));
        assertEquals(45, Multicannon.capacity(Tier.HARD));
        assertEquals(60, Multicannon.capacity(Tier.ELITE));
        assertEquals(60, Multicannon.capacity(Tier.MASTER));
        assertEquals(60, Multicannon.capacity(Tier.GRANDMASTER));
    }

    @Test
    void ecumenicalKeyChanceFollowsTiers() {
        assertEquals(60, CombatAchievements.ecumenicalKeyChance(null));
        assertEquals(60, CombatAchievements.ecumenicalKeyChance(Tier.MEDIUM));
        assertEquals(55, CombatAchievements.ecumenicalKeyChance(Tier.HARD));
        assertEquals(50, CombatAchievements.ecumenicalKeyChance(Tier.ELITE));
        assertEquals(45, CombatAchievements.ecumenicalKeyChance(Tier.MASTER));
        assertEquals(40, CombatAchievements.ecumenicalKeyChance(Tier.GRANDMASTER));
    }

    @Test
    void damageXpAndTimers() {
        assertEquals(30, Multicannon.maxHit(false));
        assertEquals(35, Multicannon.maxHit(true));
        assertEquals(20, Multicannon.XP_TENTHS_PER_DAMAGE);
        assertEquals(Multicannon.Life.OK, Multicannon.life(25 * 100 - 1));
        assertEquals(Multicannon.Life.BREAKS, Multicannon.life(25 * 100));
        assertEquals(Multicannon.Life.DECAYS, Multicannon.life(35 * 100));
        assertEquals(750_000, Multicannon.SET_PRICE);
        assertEquals(200_625, Multicannon.PART_PRICE);
    }

    @Test
    void rotatesThroughEightDirectionsOnePerTick() {
        int f = 0;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            int[] s = Multicannon.step(f);
            seen.add(s[0] + "," + s[1]);
            f = Multicannon.next(f);
        }
        assertEquals(0, f);
        assertEquals(8, seen.size());
    }

    /** RuneLite's cannon overlay marks the double-hit spots within 3 tiles: a coordinate of +-1, outside the 3x3. */
    @Test
    void doubleHitSpotsMatchRuneLite() {
        for (int x = -Multicannon.RANGE - 1; x <= Multicannon.RANGE + 1; x++) {
            for (int y = -Multicannon.RANGE - 1; y <= Multicannon.RANGE + 1; y++) {
                int shots = Multicannon.shotsPerRevolution(x, y);
                boolean under = Math.abs(x) <= 1 && Math.abs(y) <= 1;
                boolean outOfRange = Math.max(Math.abs(x), Math.abs(y)) > Multicannon.RANGE;
                if (under || outOfRange) {
                    assertEquals(0, shots, x + "," + y);
                } else {
                    boolean doubleSpot = Math.abs(x) == 1 || Math.abs(y) == 1;
                    assertEquals(doubleSpot ? 2 : 1, shots, x + "," + y);
                }
            }
        }
        assertTrue(Multicannon.inArea(0, 0, 3));
        assertFalse(Multicannon.inArea(4, 0, 3));
        assertTrue(Multicannon.inArea(1, 3, 3));
    }

    @Test
    void prohibitedZones() {
        assertNotNull(Multicannon.zone(3165, 3487)); // Grand Exchange
        assertNotNull(Multicannon.zone(3012, 3452)); // Nulodion's base
        assertNotNull(Multicannon.zone(2860, 3545)); // Warriors' Guild
        assertNull(Multicannon.zone(3222, 3218));    // Lumbridge
        for (String[] a : Multicannon.AROUND) {
            assertEquals(3, a.length);
        }
    }

    @Test
    void itemsRecipesAndQuest() {
        for (String id : new String[]{Multicannon.BASE, Multicannon.STAND, Multicannon.BARRELS, Multicannon.FURNACE,
                Multicannon.GRANITE_BALL, Multicannon.STEEL_BALL, Multicannon.AMMO_MOULD, "double_ammo_mould", Multicannon.GRANITE_DUST}) {
            assertTrue(ItemCatalog.exists(id), id);
        }
        assertEquals(187_500, ItemCatalog.get(Multicannon.BASE).value());
        Recipe single = Recipes.get("smith_cannonball");
        assertEquals(Skill.SMITHING, single.skill());
        assertEquals(35, single.level());
        assertEquals(256, single.xpTenths());
        assertEquals(8, single.ticks());
        assertEquals(Map.of("cannonball", 4), single.outputs());
        assertEquals("ammo_mould", single.requiredTool());
        Recipe dbl = Recipes.get("smith_cannonball_double");
        assertEquals(512, dbl.xpTenths());
        assertEquals(Map.of("steel_bar", 2), dbl.inputs());
        assertEquals(Map.of("cannonball", 8), dbl.outputs());
        assertEquals("double_ammo_mould", dbl.requiredTool());
        assertNotNull(ContentRegistry.quest(Multicannon.QUEST));
        assertTrue(ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(Multicannon.BLOCK)));
        assertTrue(ContentRegistry.npcs().stream().anyMatch(n -> n.id().equals(Multicannon.NULODION)));
    }
}
