package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.world.ArenaLayout;
import com.runecraft.osrs.world.Elevation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every main boss needs before a player can fight it end to end (the in-game {@code boss:fight:<id>} checks then fight
 * them for real): an arena on dry ground near its OSRS lair, an attack that hits, drops, and its pet where OSRS has one.
 */
class MainBossesFightableTest {
    private static final Set<String> CATEGORIES = Set.of("Bosses", "Wilderness", "God Wars Dungeon");
    /** Main bosses without a pet in OSRS. */
    private static final Set<String> NO_PET = Set.of("crazy_archaeologist", "hespori", "the_mimic", "gemstone_crab",
            "deranged_archaeologist", "blue_moon", "eclipse_moon", "blood_moon");
    /** Fought from a summoning stone in a hand-built lair rather than an arena (the Dagannoth Kings in Waterbirth). */
    private static final Set<String> LAIR_ONLY = Set.of("dagannoth_rex");

    private static List<BossDef> mainBosses() {
        List<BossDef> out = new ArrayList<>();
        for (BossDef b : ContentRegistry.bosses()) {
            if (!b.raidOnly && CATEGORIES.contains(b.category) && !"revenant_maledictus".equals(b.id)) {
                out.add(b);
            }
        }
        return out;
    }

    @Test
    void everyMainBossHasAnArenaOnDryGroundNearItsLair() {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (BossDef b : mainBosses()) {
            if (LAIR_ONLY.contains(b.id)) {
                continue;
            }
            ArenaLayout.Arena a = ArenaLayout.of(b.id);
            if (a == null) {
                problems.add(b.id + ": no arena (place " + b.place + ")");
                continue;
            }
            checked++;
            int water = 0;
            int total = 0;
            for (int dx = -a.radius(); dx <= a.radius(); dx += 2) {
                for (int dz = -a.radius(); dz <= a.radius(); dz += 2) {
                    if (dx * dx + dz * dz <= a.radius() * a.radius()) {
                        total++;
                        water += Elevation.isWater(a.x() + dx, a.z() + dz) ? 1 : 0;
                    }
                }
            }
            if (water * 2 > total) {
                problems.add(b.id + ": arena mostly over water (" + water + "/" + total + ")");
            }
            if (Elevation.isOcean(a.x(), a.z())) {
                problems.add(b.id + ": arena centre in the ocean");
            }
        }
        assertTrue(checked >= 50, "checked " + checked);
        assertEquals(List.of(), problems);
    }

    @Test
    void everyMainBossAttacksDropsAndHasItsPet() {
        List<String> problems = new ArrayList<>();
        for (BossDef b : mainBosses()) {
            boolean hits = false;
            for (AttackDef a : b.attacks) {
                hits |= a.maxHit() > 0;
            }
            for (BossDef.Phase p : b.phases) {
                for (AttackDef a : p.attacks()) {
                    hits |= a.maxHit() > 0;
                }
            }
            if (!hits && !"gemstone_crab".equals(b.id)) { // the Gemstone Crab never attacks (OSRS)
                problems.add(b.id + ": no attack that hits");
            }
            if (b.drops.isEmpty()) {
                problems.add(b.id + ": no drops");
            }
            if (b.pet == null && !NO_PET.contains(b.id)) {
                problems.add(b.id + ": no pet");
            }
            if (b.pet != null && NO_PET.contains(b.id)) {
                problems.add(b.id + ": has a pet it doesn't have in OSRS");
            }
        }
        assertEquals(List.of(), problems);
    }
}
