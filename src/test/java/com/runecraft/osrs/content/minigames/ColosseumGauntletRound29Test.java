package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.minigames.Gauntlet.Mode;
import com.runecraft.osrs.content.minigames.Gauntlet.Node;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Manticore orbs and Mantimayhem (OSRS Wiki "Manticore"), and the Gauntlet's tools and egniol steps ("Egniol potion"). */
class ColosseumGauntletRound29Test {
    @Test
    void manticoreFiresThreeOrbsMeleeLast() {
        Random r = new Random(3);
        for (int tier = 0; tier <= 2; tier++) {
            assertEquals(List.of(AttackType.MAGIC, AttackType.RANGED, AttackType.CRUSH),
                    FortisColosseum.manticorePattern(tier, AttackType.MAGIC, r));
            assertEquals(List.of(AttackType.RANGED, AttackType.MAGIC, AttackType.CRUSH),
                    FortisColosseum.manticorePattern(tier, AttackType.RANGED, r));
        }
        // Mantimayhem III: the melee orb can come anywhere after the first.
        Set<List<AttackType>> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            List<AttackType> p = FortisColosseum.manticorePattern(3, AttackType.MAGIC, r);
            assertEquals(Set.of(AttackType.MAGIC, AttackType.RANGED, AttackType.CRUSH), new HashSet<>(p));
            seen.add(p);
        }
        assertEquals(2, seen.size());
        assertEquals(1, FortisColosseum.manticoreHitsPerOrb(0));
        assertEquals(2, FortisColosseum.manticoreHitsPerOrb(1)); // Mantimayhem I: each orb hits twice
        assertEquals(2, FortisColosseum.manticoreHitsPerOrb(3));
        assertEquals(36, FortisColosseum.manticoreOrbMaxHit(AttackType.RANGED));
        assertEquals(31, FortisColosseum.manticoreOrbMaxHit(AttackType.MAGIC));
        assertEquals(31, FortisColosseum.manticoreOrbMaxHit(AttackType.CRUSH));
        MonsterDef m = FortisColosseum.monsters().stream().filter(d -> d.id.equals(FortisColosseum.MANTICORE)).findFirst().orElseThrow();
        for (AttackDef a : m.attacks) {
            assertEquals(FortisColosseum.MANTICORE_RANGE, a.range(), 1e-9, a.name()); // 15-tile attack range
            assertEquals(a.style().isMelee() ? 0 : 10, a.weight(), a.name() + ": its own attack is never the melee orb");
        }
        assertEquals(10, FortisColosseum.MANTICORE_CYCLE_TICKS);
    }

    @Test
    void totemHealsEveryFourPointTwoSeconds() {
        assertEquals(7, FortisColosseum.TOTEM_HEAL_TICKS);
        assertEquals(30, FortisColosseum.TOTEM_HEAL_PERCENT);
    }

    @Test
    void gauntletToolsAndEgniolSteps() {
        for (Mode m : Mode.values()) {
            assertEquals(m.prefix + "_gauntlet_pickaxe", Node.ORE.tool(m));
            assertEquals(m.prefix + "_gauntlet_axe", Node.BARK.tool(m));
            assertEquals(m.prefix + "_gauntlet_harpoon", Node.FISH.tool(m));
            assertNull(Node.LINUM.tool(m));
            assertNull(Node.GRYM.tool(m));
            List<String> start = Gauntlet.startingItems(m);
            for (String id : start) {
                assertTrue(ItemCatalog.exists(id), id);
                assertTrue(Gauntlet.runItems().contains(id), id + " leaves the Gauntlet");
            }
            assertTrue(start.containsAll(List.of(Gauntlet.PESTLE, Node.ORE.tool(m), Node.BARK.tool(m), Node.FISH.tool(m))));
            assertTrue(ItemCatalog.exists(Gauntlet.dust(m)));
            assertTrue(Gauntlet.runItems().contains(Gauntlet.dust(m)));
            assertNull(Gauntlet.recipe(m, "egniol"), "egniol potions are mixed, not sung");
            assertNotNull(Gauntlet.recipe(m, "vial"));
        }
        assertEquals("Corrupted dust", ItemCatalog.get(Gauntlet.dust(Mode.CORRUPTED)).name());
        assertTrue(ItemCatalog.exists(Gauntlet.GRYM_UNF));
        assertTrue(Gauntlet.runItems().contains(Gauntlet.GRYM_UNF));
        assertEquals(10, Gauntlet.DUST_PER_GRIND);
        assertEquals(10, Gauntlet.EGNIOL_DUST);
        assertEquals(10.0, Gauntlet.EGNIOL_XP, 1e-9);
        assertEquals("crystal_grym_leaf", Gauntlet.grym(Mode.NORMAL));
        assertEquals("corrupted_grym_leaf", Gauntlet.grym(Mode.CORRUPTED));
    }
}
