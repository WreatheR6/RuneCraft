package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.Elevation;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 31 skilling: wiki item values, Sailing hops-crop cycles, coral compost, the coral camp, drift nets, crew salvaging. */
class SkillingRound31Test {

    /** Natural (anchor-free) land over a 9x9-tile square: a place's own terrace would otherwise count as land. */
    private static boolean naturalLand(int x, int y) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                if (Elevation.naturalHeight(StructureDef.bx(x + dx), StructureDef.bz(y + dy)) <= Elevation.SEA_LEVEL) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void itemValuesMatchTheWikiSnapshot() throws Exception {
        List<String> errors = new ArrayList<>();
        int rows = 0;
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/skilling/round31_item_values.tsv"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] r = line.split("\t");
            ItemDef d = ItemCatalog.get(r[0]);
            assertNotNull(d, r[0]);
            assertEquals(r[1], d.name(), r[0]);
            if (d.value() != Integer.parseInt(r[2])) {
                errors.add(r[0] + ": " + d.value() + " (wiki " + r[2] + ")");
            }
            rows++;
        }
        assertEquals(34, rows);
        assertTrue(errors.isEmpty(), String.join(", ", errors));
    }

    @Test
    void sailingHopsCropsGrowInTwentyMinuteCycles() {
        // OSRS Wiki: flax 3 stages over 60 minutes, hemp 4 over 80, cotton 5 over 100; hops keep 10-minute cycles.
        Map<String, Integer> stages = Map.of("flax", 3, "hemp", 4, "cotton", 5);
        stages.forEach((id, n) -> {
            FarmingData.Crop c = FarmingData.byId(id);
            assertEquals(FarmingData.Patch.HOPS, c.patch(), id);
            assertEquals(20, FarmingData.cycleMinutes(c), id);
            assertEquals(n, FarmingData.stages(c), id);
        });
        for (FarmingData.Crop c : FarmingData.CROPS) {
            if (c.patch() == FarmingData.Patch.HOPS && !FarmingData.SAILING_HOPS.contains(c.id())) {
                assertEquals(10, FarmingData.cycleMinutes(c), c.id());
            }
        }
    }

    @Test
    void coralNurseriesTakeNoCompostAndStandOnLand() {
        assertFalse(FarmingData.compostable(FarmingData.Patch.CORAL));
        for (FarmingData.Patch p : FarmingData.Patch.values()) {
            if (p != FarmingData.Patch.CORAL) {
                assertTrue(FarmingData.compostable(p), p.name());
            }
        }
        PlaceDef camp = ContentRegistry.place("coral_nurseries");
        assertNotNull(camp);
        assertTrue(naturalLand(camp.osrsX(), camp.osrsY()), "Coral Nurseries camp at sea: " + camp.osrsX() + "," + camp.osrsY());
        // Still on the Great Conch's east side, near the real steps (3272,2463).
        assertTrue(Math.abs(camp.osrsX() - 3272) <= 40 && Math.abs(camp.osrsY() - 2463) <= 10);
    }

    @Test
    void driftNetXpFollowsTheWikiTable() {
        // OSRS Wiki "Drift net fishing" experience table (Hunter, Fishing per shoal).
        assertEquals(52.3, DriftNetFishing.hunterXp(44), 1e-9);
        assertEquals(62.5, DriftNetFishing.hunterXp(50), 1e-9);
        assertEquals(71.5, DriftNetFishing.hunterXp(55), 1e-9);
        assertEquals(81.0, DriftNetFishing.hunterXp(60), 1e-9);
        assertEquals(91.0, DriftNetFishing.hunterXp(65), 1e-9);
        assertEquals(101.5, DriftNetFishing.hunterXp(70), 1e-9);
        assertEquals(101.5, DriftNetFishing.hunterXp(99), 1e-9);
        assertEquals(46.2, DriftNetFishing.fishingXp(47), 1e-9);
        assertEquals(50.0, DriftNetFishing.fishingXp(50), 1e-9);
        assertEquals(56.3, DriftNetFishing.fishingXp(55), 1e-9);
        assertEquals(63.0, DriftNetFishing.fishingXp(60), 1e-9);
        assertEquals(69.8, DriftNetFishing.fishingXp(65), 1e-9);
        assertEquals(77.0, DriftNetFishing.fishingXp(70), 1e-9);
        assertEquals(77.0, DriftNetFishing.fishingXp(99), 1e-9);
    }

    @Test
    void driftNetFishTableGrowsEveryTenLevels() {
        assertEquals(List.of("oyster", "pufferfish", "raw_anchovies", "raw_sardine", "raw_tuna"), DriftNetFishing.fishTable(47));
        assertEquals(6, DriftNetFishing.fishTable(50).size());
        assertEquals("raw_swordfish", DriftNetFishing.fishTable(60).get(6));
        assertEquals("raw_shark", DriftNetFishing.fishTable(79).get(7));
        assertEquals("raw_sea_turtle", DriftNetFishing.fishTable(80).get(8));
        assertEquals(10, DriftNetFishing.fishTable(99).size());
        assertEquals("raw_manta_ray", DriftNetFishing.fishTable(99).get(9));
        // Every loot item exists; the fish are equally likely, and the 1/25 fossil roll gives a fossil 3/175, numulites
        // 27/175 and nothing 145/175 (wiki "Fossil Island fossil": 3/4375, 27/4375 and 145/4375 per shoal).
        for (DriftNetFishing.Fish f : DriftNetFishing.FISH) {
            assertTrue(ItemCatalog.exists(f.item()), f.item());
        }
        for (Object[] l : DriftNetFishing.FOSSIL_TABLE) {
            assertTrue(ItemCatalog.exists((String) l[0]), (String) l[0]);
        }
        assertTrue(ItemCatalog.exists(DriftNetFishing.NUMULITE));
        assertTrue(ItemCatalog.exists(DriftNetFishing.CLUE));
        for (String t : DriftNetFishing.CHASE_TOOLS) {
            assertTrue(ItemCatalog.exists(t), t);
        }
        Random random = new Random(31);
        Map<String, Integer> counts = new HashMap<>();
        int n = 1_000_000;
        for (int i = 0; i < n; i++) {
            Object[] loot = DriftNetFishing.roll(99, true, random);
            counts.merge(loot == null ? "nothing" : (String) loot[0], 1, Integer::sum);
        }
        double fish = n * (24.0 / 25) * (599.0 / 600) / 10;
        assertEquals(fish, counts.get("raw_manta_ray"), fish * 0.03);
        assertEquals(fish, counts.get("oyster"), fish * 0.03);
        int fossils = counts.getOrDefault("unidentified_small_fossil", 0) + counts.getOrDefault("unidentified_medium_fossil", 0)
                + counts.getOrDefault("unidentified_large_fossil", 0) + counts.getOrDefault("unidentified_rare_fossil", 0);
        assertEquals(n * 3.0 / 4375, fossils, n * 3.0 / 4375 * 0.15);
        assertEquals(n * 27.0 / 4375, counts.getOrDefault("numulite", 0), n * 27.0 / 4375 * 0.06);
        assertEquals(n * 145.0 / 4375, counts.getOrDefault("nothing", 0), n * 145.0 / 4375 * 0.03);
        assertEquals(10, DriftNetFishing.CAPACITY);
        assertTrue(DriftNetFishing.chaseChance(true) > DriftNetFishing.chaseChance(false));
    }

    @Test
    void driftNetIsWovenAndItsAreaStandsOnFossilIsland() {
        Recipe r = Recipes.get("weave_drift_net");
        assertNotNull(r);
        assertEquals(MenuType.SPINNING, r.menu());
        assertEquals(Skill.CRAFTING, r.skill());
        assertEquals(26, r.level());
        assertEquals(550, r.xpTenths());
        assertEquals(2, r.inputs().get("jute_fibre"));
        assertEquals(1, r.outputs().get(DriftNetFishing.NET));
        PlaceDef area = ContentRegistry.place(DriftNetFishing.PLACE);
        assertNotNull(area);
        assertTrue(naturalLand(area.osrsX(), area.osrsY()), "drift net area at sea");
        assertTrue(area.blocks().contains(DriftNetFishing.ANCHOR) && area.blocks().contains(DriftNetFishing.SHOAL));
        // Clear of the underwater seaweed stand-in and the House on the Hill.
        PlaceDef seaweed = ContentRegistry.place("underwater_seaweed");
        assertTrue(Math.abs(area.osrsX() - seaweed.osrsX()) + Math.abs(area.osrsY() - seaweed.osrsY()) >= 12);
    }

    @Test
    void crewSalvageAtDeckhandinessTimesTenPercent() {
        // Wiki example: the player at 40%, Jobless Jim (Deckhandiness 3) about 12%; crew roll every 5 ticks.
        assertEquals(0.12, SailingCrew.crewSalvageChance(0.40, 3), 1e-9);
        assertEquals(0.04, SailingCrew.crewSalvageChance(0.40, 1), 1e-9);
        assertEquals(5, SailingCrew.CREW_SALVAGE_TICKS);
        List<SailingCrew.Crewmate> all = new ArrayList<>(SailingCrew.CREW);
        SailingCrew.Crewmate best = SailingCrew.salvager(all, 2);
        assertNotNull(best);
        for (SailingCrew.Crewmate c : all) {
            assertTrue(c.deckhandiness() <= best.deckhandiness(), c.id());
        }
        // A dragon hook needs Deckhandiness 4.
        SailingCrew.Crewmate dragon = SailingCrew.salvager(all, 6);
        if (dragon != null) {
            assertTrue(dragon.deckhandiness() >= 4);
        }
        assertEquals(null, SailingCrew.salvager(List.of(), 0));
    }
}
