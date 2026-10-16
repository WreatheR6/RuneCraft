package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.FarmingDisease;
import com.runecraft.osrs.FarmingYield;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.minigames.GuardiansRewards;
import com.runecraft.osrs.content.minigames.GuardiansRift;
import com.runecraft.osrs.content.minigames.VolcanicMine;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 28 skilling depth: Sailing crops, coral, giant seaweed, aether runes, daeyalt, celestial ring, jewellery, VM, GotR. */
class SkillingDepthRound28Test {

    private static List<String[]> rows(String resource) throws Exception {
        List<String[]> out = new ArrayList<>();
        for (String line : java.nio.file.Files.readAllLines(java.nio.file.Path.of("src/test/resources" + resource), StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                out.add(line.split("\t"));
            }
        }
        return out;
    }

    @Test
    void farmingCropsMatchTheWikiSnapshot() throws Exception {
        List<String[]> rows = rows("/osrs/skilling/round28_farming.tsv");
        assertEquals(10, rows.size());
        for (String[] r : rows) {
            FarmingData.Crop c = FarmingData.byId(r[0]);
            assertNotNull(c, r[0]);
            assertEquals(r[1], c.patch().name(), r[0]);
            assertEquals(Integer.parseInt(r[2]), c.level(), r[0]);
            assertEquals(Integer.parseInt(r[3]), c.count(), r[0]);
            assertEquals(Double.parseDouble(r[4]), c.plantXp(), 1e-9, r[0]);
            assertEquals(Double.parseDouble(r[5]), c.checkXp(), 1e-9, r[0]);
            assertEquals(Double.parseDouble(r[6]), c.harvestXp(), 1e-9, r[0]);
            assertEquals(Integer.parseInt(r[7]), c.growthMinutes(), r[0]);
            assertEquals(r[8], c.planted(), r[0]);
            assertEquals(r[9], c.produce(), r[0]);
            FarmingDisease.Payment pay = FarmingDisease.payment(r[0]);
            if (r[10].equals("-")) {
                assertNull(pay, r[0]);
            } else {
                assertNotNull(pay, r[0]);
                assertEquals(r[10], pay.item() + ":" + pay.count(), r[0]);
                assertTrue(ItemCatalog.exists(pay.item()), pay.item());
            }
            assertTrue(ItemCatalog.exists(c.planted()), c.planted());
            if (c.harvest() == FarmingData.Harvest.CHOP) {
                assertNotNull(GatheringData.tree(c.produce()), c.produce());
            } else {
                assertTrue(ItemCatalog.exists(c.produce()), c.produce());
            }
        }
        // The Sailing seeds and frags stay SailingData's items (seeds, not redefined as saplings).
        for (String seed : List.of("flax_seed", "hemp_seed", "cotton_seed", "camphor_seed", "elkhorn_frag")) {
            assertTrue(ItemCatalog.exists(seed), seed);
        }
        assertTrue(FarmingData.noPlantingTool(FarmingData.byId("elkhorn")));
        assertFalse(FarmingData.noPlantingTool(FarmingData.byId("hemp")));
    }

    @Test
    void cropYields() {
        FarmingData.Crop coral = FarmingData.byId("umbral");
        for (int level : new int[]{77, 99}) {
            int min = Integer.MAX_VALUE;
            long total = 0;
            java.util.Random rnd = new java.util.Random(level);
            for (int i = 0; i < 2000; i++) {
                int y = FarmingData.harvestYield(coral, level, 0, rnd::nextInt);
                min = Math.min(min, y);
                total += y;
            }
            assertEquals(FarmingData.CORAL_MIN_YIELD, Math.min(min, FarmingData.CORAL_MIN_YIELD), "coral minimum");
            assertTrue(min >= FarmingData.CORAL_MIN_YIELD);
            assertTrue(total / 2000.0 > 6, "average coral harvest at " + level);
        }
        // Coral: 4 harvest lives compost can't raise, with the wiki yield calculator's chances (July 2026 dump).
        assertEquals(4, FarmingData.CORAL_LIVES);
        assertEquals(100, FarmingData.harvestChance(FarmingData.byId("elkhorn"))[0]);
        assertEquals(48, FarmingData.harvestChance(FarmingData.byId("pillar"))[0]);
        assertEquals(-110, FarmingData.harvestChance(FarmingData.byId("umbral"))[0]);
        int[] elkhorn = FarmingData.harvestChance(FarmingData.byId("elkhorn"));
        assertEquals(20.48, FarmingYield.expected(FarmingData.CORAL_LIVES, FarmingYield.saveChance(elkhorn[0], elkhorn[1], 99, 0, 0)),
                0.01);
        java.util.Random r = new java.util.Random(5);
        long elkhornTotal = 0;
        for (int i = 0; i < 4000; i++) {
            elkhornTotal += FarmingData.harvestYield(FarmingData.byId("elkhorn"), 99, 3, r::nextInt);
        }
        assertEquals(20.48, elkhornTotal / 4000.0, 1.0, "elkhorn at 99 (compost doesn't add lives)");
        // Hops-patch fibres use the wiki's harvest-lives chances.
        assertEquals(140, FarmingData.harvestChance(FarmingData.byId("flax"))[0]);
        assertEquals(178, FarmingData.harvestChance(FarmingData.byId("hemp"))[1]);
        assertEquals(82, FarmingData.harvestChance(FarmingData.byId("cotton"))[0]);
    }

    @Test
    void fibreAndSeaweedRecipes() {
        // OSRS Wiki: linen 12 / 16 + 20, canvas 39 / 60 + 75, cotton 73 / 105 + 132; giant seaweed -> 6 soda ash.
        Object[][] want = {{"spin_linen_yarn", 12, 16.0, "flax", "linen_yarn"}, {"weave_bolt_of_linen", 12, 20.0, "linen_yarn", "bolt_of_linen"},
                {"spin_hemp_yarn", 39, 60.0, "hemp", "hemp_yarn"}, {"weave_bolt_of_canvas", 39, 75.0, "hemp_yarn", "bolt_of_canvas"},
                {"spin_cotton_yarn", 73, 105.0, "cotton_boll", "cotton_yarn"}, {"weave_bolt_of_cotton", 73, 132.0, "cotton_yarn", "bolt_of_cotton"}};
        for (Object[] w : want) {
            Recipe r = Recipes.get((String) w[0]);
            assertNotNull(r, (String) w[0]);
            assertEquals(Skill.CRAFTING, r.skill());
            assertEquals((Integer) w[1], r.level(), (String) w[0]);
            assertEquals((Double) w[2], r.xpTenths() / 10.0, 1e-9, (String) w[0]);
            assertTrue(r.inputs().containsKey((String) w[3]), (String) w[0]);
            assertEquals(1, r.outputs().get((String) w[4]), (String) w[0]);
        }
        assertEquals(2, Recipes.get("weave_bolt_of_canvas").inputs().get("hemp_yarn"));
        assertEquals(6, Recipes.get("burn_giant_seaweed").outputs().get("soda_ash"));
    }

    @Test
    void runecraftExtras() {
        assertEquals(90, RunecraftData.AETHER_LEVEL);
        assertEquals(20.0, RunecraftData.AETHER_XP, 1e-9);
        assertEquals(30.0, RunecraftData.essenceXp(RunecraftData.AETHER_XP, 1, true), 1e-9);
        assertEquals(15.0, RunecraftData.essenceXp(10.0, 1, true), 1e-9);
        assertTrue(ItemCatalog.exists(RunecraftData.AETHER_RUNE));
        assertTrue(ItemCatalog.exists(RunecraftData.AETHER_CATALYST));
        assertTrue(ItemCatalog.exists("daeyalt_essence"));
        assertTrue(ItemCatalog.exists("daeyalt_shard"));
        assertEquals(60, RunecraftData.DAEYALT_MINING_LEVEL);
        assertEquals(5.0, RunecraftData.DAEYALT_MINING_XP, 1e-9);
        // Exactly one of the three daeyalt rocks is active at any time, switching every 100 ticks.
        for (long t = 0; t < 1000; t += 7) {
            int active = 0;
            for (int phase = 0; phase < RunecraftData.DAEYALT_ROCKS; phase++) {
                active += RunecraftData.daeyaltActive(t, phase) ? 1 : 0;
            }
            assertEquals(1, active);
        }
        assertFalse(RunecraftData.daeyaltActive(0, 1) == RunecraftData.daeyaltActive(RunecraftData.DAEYALT_SWITCH_TICKS, 1)
                && RunecraftData.daeyaltActive(0, 1) == RunecraftData.daeyaltActive(2L * RunecraftData.DAEYALT_SWITCH_TICKS, 1));
    }

    @Test
    void daeyaltMineInDarkmeyer() {
        // The rocks are part of the Darkmeyer structure; the place only puts Noranna there (no camp plaza in the city).
        PlaceDef mine = ContentRegistry.place("daeyalt_essence_mine");
        assertNotNull(mine);
        assertEquals(PlaceDef.Style.NONE, mine.style());
        StructureDef darkmeyer = ContentRegistry.structures().stream().filter(d -> d.id().equals("darkmeyer")).findFirst().orElse(null);
        assertNotNull(darkmeyer);
        String rock = "runecraft:" + RunecraftData.DAEYALT_ROCK;
        List<StructureDef.Op> rocks = darkmeyer.ops().stream().filter(o -> rock.equals(o.block())).toList();
        assertEquals(RunecraftData.DAEYALT_ROCKS, rocks.size());
        // Each rock gets its own turn, so exactly one is active at any time.
        Set<Integer> phases = new HashSet<>();
        for (StructureDef.Op o : rocks) {
            phases.add(RunecraftData.daeyaltPhase(o.x1(), o.z1()));
        }
        assertEquals(RunecraftData.DAEYALT_ROCKS, phases.size(), "daeyalt rock phases " + phases);
        // Nothing else of any structure stands on the rocks or on Noranna's spot.
        NpcDef noranna = ContentRegistry.npcs().stream().filter(n -> n.id().equals(RunecraftData.NORANNA)).findFirst().orElse(null);
        assertNotNull(noranna);
        assertEquals(mine.id(), noranna.place());
        List<int[]> spots = new ArrayList<>();
        for (StructureDef.Op o : rocks) {
            spots.add(new int[]{o.x1(), o.z1()});
        }
        spots.add(new int[]{mine.blockX() + noranna.dx(), mine.blockZ() + noranna.dz()});
        for (StructureDef s : ContentRegistry.structures()) {
            for (StructureDef.Op op : s.ops()) {
                if (rocks.contains(op) || op.type() == StructureDef.Op.Type.CLEAR || op.type() == StructureDef.Op.Type.FLATTEN
                        || op.y2() < StructureDef.GROUND) {
                    continue;
                }
                for (int[] at : spots) {
                    assertFalse(op.x1() <= at[0] && at[0] <= op.x2() && op.z1() <= at[1] && at[1] <= op.z2(),
                            s.id() + " op " + op + " covers the daeyalt mine at " + at[0] + "," + at[1]);
                }
            }
        }
    }

    @Test
    void celestialRingAndJewellery() {
        assertEquals(84, ShootingStars.celestialLevel(80, true));
        assertEquals(80, ShootingStars.celestialLevel(80, false));
        assertTrue(ShootingStars.celestialExtraOre("adamantite_ore", 1, 0.05));
        assertFalse(ShootingStars.celestialExtraOre("runite_ore", 1, 0.05));
        assertFalse(ShootingStars.celestialExtraOre("coal", 0, 0.05));
        assertFalse(ShootingStars.celestialExtraOre("coal", 5, 0.1));
        // Every ore from a suitable rock uses a charge, extra ore or not.
        assertTrue(ShootingStars.celestialUsesCharge("coal", 5));
        assertFalse(ShootingStars.celestialUsesCharge("runite_ore", 5));
        assertFalse(ShootingStars.celestialUsesCharge("coal", 0));
        assertEquals(100, ShootingStars.celestialCharge(9_900, 500));
        assertTrue(ItemCatalog.exists(ShootingStars.CELESTIAL_RING));
        assertNotNull(Jewellery.worn(ShootingStars.CELESTIAL_RING));
        assertNotNull(Jewellery.worn(ShootingStars.CELESTIAL_RING_UNCHARGED));
        // Binding necklace: 16 per-player charges; bounty 25% one seed; chemistry 5% a fourth dose.
        assertEquals(16, Jewellery.chargePool("binding_necklace"));
        assertEquals(1, Jewellery.bountySeeds(3, 0.24));
        assertEquals(3, Jewellery.bountySeeds(3, 0.25));
        assertEquals(4, Jewellery.chemistryDoses(3, 0.049));
        assertEquals(3, Jewellery.chemistryDoses(3, 0.05));
        assertEquals(2, Jewellery.chemistryDoses(2, 0.0));
    }

    @Test
    void volcanicMineAdditions() {
        assertEquals(36, VolcanicMine.vesselUses(false));
        assertEquals(72, VolcanicMine.vesselUses(true));
        assertEquals(150, VolcanicMine.PLATFORM_TICKS); // 90 seconds
        assertEquals(1, VolcanicMine.boulders(9));
        assertEquals(2, VolcanicMine.boulders(10));
        assertEquals(3, VolcanicMine.boulders(20));
        assertTrue(VolcanicMine.meteorChance(10) > VolcanicMine.meteorChance(60));
        assertTrue(ItemCatalog.exists(VolcanicMine.VESSEL));
    }

    @Test
    void guardiansAetherAndCreatures() {
        assertEquals(1.0, GuardiansRewards.combinationSuccess(true), 1e-9);
        assertEquals(0.5, GuardiansRewards.combinationSuccess(false), 1e-9);
        assertEquals(3 + 2 * 2, GuardiansRewards.catalyticStoneEnergy(2, 1));
        assertTrue(ItemCatalog.exists(GuardiansRewards.POLYCATALYTIC_STONE));
        assertEquals("cosmic", GuardiansRewards.AETHER_ALTAR);
        // Creatures walk to a standing barrier, attack it, then go on to the Great Guardian; rift guardians kill them.
        GuardiansRift rift = new GuardiansRift();
        rift.useCell(2, GuardiansRift.Cell.WEAK, 0, 0);
        long t = GuardiansRift.PREP_TICKS;
        for (int i = 0; i < GuardiansRift.WALK_TICKS + GuardiansRift.spawnInterval(1, 0) + 1; i++) {
            rift.tick(t++, 1, 0, () -> 0.0);
        }
        assertFalse(rift.creatures().isEmpty());
        assertEquals(2, rift.creatures().get(0).lane);
        assertEquals(GuardiansRift.Stage.AT_BARRIER, rift.creatures().get(0).stage());
        assertEquals(GuardiansRift.CreatureType.LEECH, GuardiansRift.creatureFor(0.0, 0));
        assertEquals(GuardiansRift.CreatureType.WALKER, GuardiansRift.creatureFor(0.95, 0));
        assertEquals(GuardiansRift.CreatureType.WALKER, GuardiansRift.creatureFor(0.75, 60));
        rift.seal();
        assertTrue(rift.creatures().isEmpty());
        // With rift guardians fighting, a lone leech dies before doing much.
        GuardiansRift guarded = new GuardiansRift();
        guarded.useCell(0, GuardiansRift.Cell.WEAK, 0, 0);
        guarded.assemble(GuardiansRift.Cell.WEAK, false);
        guarded.assemble(GuardiansRift.Cell.WEAK, false);
        for (long k = GuardiansRift.PREP_TICKS; k < GuardiansRift.PREP_TICKS + 60; k++) {
            guarded.tick(k, 1, 0, () -> 0.0);
        }
        assertTrue(guarded.hp(0) > 30, "barrier hp " + guarded.hp(0));
    }
}
