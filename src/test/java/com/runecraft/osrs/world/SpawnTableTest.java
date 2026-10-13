package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.data.Monster;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnTableTest {
    /** Landmarks that are not towns (no plaza full of players). */
    private static final Set<Landmark> NOT_TOWNS = Set.of(Landmark.WIZARDS_TOWER, Landmark.DRAYNOR_MANOR,
            Landmark.STONE_CIRCLE, Landmark.BARROWS, Landmark.GOD_WARS_DUNGEON, Landmark.MOR_UL_REK, Landmark.TROLLHEIM);

    private static Set<String> placed() {
        Set<String> ids = new HashSet<>();
        SpawnTable.all().forEach(p -> ids.add(p.monster()));
        SpawnTable.camps().forEach(c -> ids.add(c.creature()));
        return ids;
    }

    @Test
    void everyHostileMonsterHasASpawnPointOrCamp() {
        Set<String> placed = placed();
        Map<String, String> encounter = SpawnTable.encounterOnly();
        List<String> missing = new ArrayList<>();
        for (Monster m : Monster.values()) {
            if (m.aggressive() && !placed.contains(m.entity()) && !encounter.containsKey(m.entity())) {
                missing.add(m.entity());
            }
        }
        for (MonsterDef d : ContentRegistry.monsters()) {
            if (d.aggressive && !placed.contains(d.id) && !encounter.containsKey(d.id)) {
                missing.add(d.id);
            }
        }
        assertTrue(missing.isEmpty(), "hostile monsters that never spawn: " + missing);
    }

    /** Every regular (non-boss, non-raid, non-minigame) monster, hostile or not, lives somewhere. */
    @Test
    void everyRegularMonsterHasASpawnPointOrCamp() {
        Set<String> placed = placed();
        Map<String, String> encounter = SpawnTable.encounterOnly();
        List<String> missing = new ArrayList<>();
        for (Monster m : Monster.values()) {
            if (!placed.contains(m.entity()) && !encounter.containsKey(m.entity())) {
                missing.add(m.entity());
            }
        }
        for (MonsterDef d : ContentRegistry.monsters()) {
            if (!placed.contains(d.id) && !encounter.containsKey(d.id)) {
                missing.add(d.id);
            }
        }
        assertTrue(missing.isEmpty(), "monsters that never spawn: " + missing);
        for (String id : encounter.keySet()) {
            assertTrue(ContentRegistry.boss(id) == null, id + " is a boss, not a monster");
        }
    }

    /** A point lies in one of the kingdoms its monster is recorded in (islands and Zeah excepted). */
    @Test
    void pointsLieInTheirMonstersRegions() {
        List<String> wrong = new ArrayList<>();
        for (SpawnPoint p : SpawnTable.all()) {
            MonsterDef d = ContentRegistry.monster(p.monster());
            if (d == null || d.spawns.isEmpty() || p.island()) {
                continue;
            }
            Region region = GielinorMap.regionAt(p.x(), p.y());
            if (d.spawns.stream().noneMatch(s -> s.region() == region)) {
                wrong.add(p.key() + " in " + region);
            }
        }
        assertTrue(wrong.isEmpty(), "points outside their monster's regions: " + wrong);
    }

    /** Underground monsters stay in carved dungeons, except the listed surface stand-ins. */
    @Test
    void undergroundMonstersOnlySpawnInDungeons() {
        Map<String, String> standIns = SpawnTable.surfaceStandIns();
        List<String> wrong = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (SpawnPoint p : SpawnTable.all()) {
            MonsterDef d = ContentRegistry.monster(p.monster());
            if (d == null || !d.underground || !p.surface()) {
                continue;
            }
            if (standIns.containsKey(d.id)) {
                used.add(d.id);
            } else {
                wrong.add(p.key());
            }
        }
        assertTrue(wrong.isEmpty(), "underground monsters on the surface: " + wrong);
        for (String id : standIns.keySet()) {
            MonsterDef d = ContentRegistry.monster(id);
            assertNotNull(d, id);
            assertTrue(d.underground && used.contains(id), id + " needs no surface stand-in entry");
        }
    }

    /** Spawn counts roughly follow OSRS: a field holds about a dozen cows, never fifty. */
    @Test
    void densitiesRoughlyMatchOsrs() {
        assertBetween("minecraft:cow", 3256, 3274, 20, 8, 16);      // Lumbridge east cow field
        assertBetween("goblin", 3252, 3239, 20, 8, 16);             // Lumbridge goblins east of the Lum
        assertBetween("minecraft:chicken", 3233, 3297, 12, 6, 14);  // Lumbridge chicken farm
        assertBetween("minecraft:sheep", 3200, 3266, 12, 6, 14);    // Fred's sheep
        assertBetween("rock_crab", 2700, 3716, 30, 12, 24);         // Rellekka rock crabs
        assertBetween("zombie_lvl18", 3165, 3700, 50, 10, 24);      // Graveyard of Shadows
        for (SpawnPoint p : SpawnTable.all()) {
            double area = Math.PI * (p.radius() + 1) * (p.radius() + 1);
            assertTrue(p.count() / area <= 0.35, p.key() + " is packed too densely");
        }
    }

    private static void assertBetween(String id, int x, int y, int within, int min, int max) {
        int n = 0;
        for (SpawnPoint p : SpawnTable.all()) {
            if (p.monster().equals(id) && Math.hypot(p.x() - x, p.y() - y) <= within) {
                n += p.count();
            }
        }
        assertTrue(n >= min && n <= max, id + " near " + x + "," + y + ": " + n + " (expected " + min + "-" + max + ")");
    }

    @Test
    void farmAnimalsAndTownsfolkLiveSomewhere() {
        Set<String> placed = placed();
        for (String id : List.of("minecraft:chicken", "minecraft:cow", "minecraft:sheep", "man", "woman", "guard",
                "goblin", "barbarian", "al_kharid_warrior", "dark_wizard", "scorpion", "giant_rat", "sand_crab")) {
            assertTrue(placed.contains(id), id);
        }
    }

    @Test
    void everyPointReferencesARealMonster() {
        for (SpawnPoint p : SpawnTable.all()) {
            assertNotNull(SpawnTable.info(p.monster()), p.key());
        }
        for (SpawnTable.CampRef c : SpawnTable.camps()) {
            assertNotNull(SpawnTable.info(c.creature()), c.source() + " " + c.creature());
        }
        for (String id : SpawnTable.encounterOnly().keySet()) {
            assertNotNull(SpawnTable.info(id), "encounter-only " + id);
        }
    }

    @Test
    void pointsHaveSaneValuesAndUniqueKeys() {
        Set<String> keys = new HashSet<>();
        for (SpawnPoint p : SpawnTable.all()) {
            assertTrue(keys.add(p.key()), "duplicate " + p.key());
            assertTrue(p.count() >= 1 && p.count() <= 12, p.key());
            assertTrue(p.radius() >= 1 && p.radius() <= 32, p.key());
            assertTrue(p.respawnTicks() >= 10 && p.respawnTicks() <= 6000, p.key());
            assertEquals(p.x(), GielinorMap.toOsrsX(p.blockX()), p.key());
            assertEquals(p.y(), GielinorMap.toOsrsY(p.blockZ()), p.key());
        }
        assertTrue(SpawnTable.all().size() >= 80, "the table covers the world");
    }

    @Test
    void pointsLieOnTheMappedLand() {
        for (SpawnPoint p : SpawnTable.all()) {
            Region region = GielinorMap.regionAt(p.x(), p.y());
            if (p.island()) {
                assertFalse(region.mainland(), p.key() + " is inside a kingdom: drop the island flag");
            } else {
                assertTrue(region.mainland(), p.key() + " is off the mapped kingdoms");
            }
            // Gielinor's bounds; only islands (Zanaris at y 4400) lie north of the Wilderness.
            assertTrue(p.x() > 1000 && p.x() < 4000 && p.y() > 2500 && p.y() < (p.island() ? 4500 : 4000), p.key());
        }
    }

    @Test
    void wildernessMonstersStayInTheWilderness() {
        for (String id : List.of("green_dragon", "lava_dragon", "greater_demon", "king_scorpion", "ent", "mammoth")) {
            for (SpawnPoint p : SpawnTable.all()) {
                if (p.monster().equals(id) && p.surface()) {
                    assertTrue(GielinorMap.wildernessLevel(p.blockX(), p.blockZ()) > 0, p.key());
                }
            }
        }
    }

    @Test
    void fixedFloorsAreInsideBuiltDungeonsOrBuildings() {
        List<SpawnTable.CampRef> camps = SpawnTable.camps();
        for (SpawnPoint p : SpawnTable.all()) {
            if (p.surface()) {
                continue;
            }
            if (p.floorY() >= StructureDef.GROUND) {
                assertTrue(p.floorY() <= StructureDef.GROUND + 40, p.key() + " floor is too high");
                continue;
            }
            assertTrue(p.floorY() <= Dungeons.DEEP_TOP && p.floorY() > -64, p.key());
            boolean inside = false;
            for (SpawnTable.CampRef c : camps) {
                if (c.floorY() == p.floorY() && Math.abs(c.x() - p.x()) <= c.radius() + 6
                        && Math.abs(c.y() - p.y()) <= c.radius() + 6) {
                    inside = true;
                    break;
                }
            }
            assertTrue(inside, p.key() + " is not inside a carved dungeon camp area");
        }
    }

    @Test
    void noPointDoublesAnExistingCamp() {
        List<SpawnTable.CampRef> camps = SpawnTable.camps();
        for (SpawnPoint p : SpawnTable.all()) {
            for (SpawnTable.CampRef c : camps) {
                if (!c.creature().equals(p.monster())) {
                    continue;
                }
                boolean sameLevel = p.surface() ? c.floorY() == SpawnPoint.SURFACE || c.floorY() > 50
                        : c.floorY() == p.floorY();
                double d = Math.hypot(c.x() - p.x(), c.y() - p.y());
                assertFalse(sameLevel && d < 24, p.key() + " doubles " + c.source() + "'s camp");
            }
        }
    }

    @Test
    void aggressiveMonstersStayOutOfTownPlazas() {
        List<int[]> towns = new ArrayList<>();
        for (Landmark l : Landmark.values()) {
            if (!l.isAltar() && !NOT_TOWNS.contains(l)) {
                towns.add(new int[]{l.osrsX(), l.osrsY()});
            }
        }
        for (PlaceDef pd : ContentRegistry.places()) {
            if (pd.style() == PlaceDef.Style.TOWN) {
                towns.add(new int[]{pd.osrsX(), pd.osrsY()});
            }
        }
        for (SpawnPoint p : SpawnTable.all()) {
            SpawnTable.Info info = SpawnTable.info(p.monster());
            if (!info.aggressive() || info.level() < 20 || (!p.surface() && p.floorY() < StructureDef.GROUND)) {
                continue;
            }
            for (int[] t : towns) {
                double d = Math.hypot(t[0] - p.x(), t[1] - p.y());
                assertTrue(d >= 12 + p.radius(), p.key() + " is " + (int) d + " tiles from the town at " + t[0] + "," + t[1]);
            }
        }
    }

    @Test
    void campReferencesResolve() {
        assertFalse(SpawnTable.camps().isEmpty());
        for (SpawnTable.CampRef c : SpawnTable.camps()) {
            assertTrue(c.radius() >= 1, c.source());
        }
        assertNotNull(SpawnTable.placeTile("LUMBRIDGE"));
        assertNotNull(SpawnTable.placeTile("zeah_kourend_castle"));
    }
}
