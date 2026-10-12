package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Area/guild access gates and the degradation/charge table. */
class AccessAndChargesTest {
    /** A configurable fake player. */
    static final class Fake implements AccessGates.PlayerView {
        final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        final Map<String, Integer> stages = new HashMap<>();
        final Set<String> done = new HashSet<>();
        final Set<String> worn = new HashSet<>();
        final Set<String> carried = new HashSet<>();
        int qp;
        boolean combatGear;

        @Override
        public int level(Skill skill) {
            return levels.getOrDefault(skill, skill == Skill.HITPOINTS ? 10 : 1);
        }

        @Override
        public int questStage(String questId) {
            return stages.getOrDefault(questId, 0);
        }

        @Override
        public boolean questComplete(String questId) {
            return done.contains(questId);
        }

        @Override
        public int questPoints() {
            return qp;
        }

        @Override
        public boolean wearing(String itemId) {
            return worn.contains(itemId);
        }

        @Override
        public boolean carrying(String itemId) {
            return worn.contains(itemId) || carried.contains(itemId);
        }

        @Override
        public boolean hasCombatGear() {
            return combatGear;
        }
    }

    private static AccessGates.Gate gate(String id) {
        AccessGates.Gate g = AccessGates.byId(id);
        assertNotNull(g, id);
        return g;
    }

    // ---------------- gates: data

    @Test
    void everyRequestedGateExists() {
        for (String id : new String[]{"morytania", "heroes_guild", "legends_guild", "champions_guild", "fishing_guild",
                "crafting_guild", "mining_guild", "ranging_guild", "wizards_guild", "cooks_guild", "woodcutting_guild",
                "farming_guild", "warriors_guild", "myths_guild", "fossil_island", "zanaris", "entrana", "prifddinas",
                "ape_atoll", "lunar_isle"}) {
            gate(id);
        }
        Set<String> ids = new HashSet<>();
        for (AccessGates.Gate g : AccessGates.all()) {
            assertTrue(ids.add(g.id()), "duplicate gate " + g.id());
        }
    }

    @Test
    void gatesReferenceRealQuestsAndItems() {
        for (String q : new String[]{"priest_in_peril", "heroes_quest", "legends_quest", "dragon_slayer_ii", "bone_voyage",
                "lost_city", "song_of_the_elves", "monkey_madness_i", "lunar_diplomacy"}) {
            assertNotNull(ContentRegistry.quest(q), q);
        }
        for (String item : new String[]{"brown_apron", AccessGates.CHEFS_HAT, "dramen_staff", "golden_chef_hat"}) {
            assertNotNull(ItemCatalog.get(item), item);
        }
    }

    @Test
    void gatesCoverTheirPlacesAndStructures() {
        Map<String, String> structures = Map.of("heroes_guild", "heroes_guild", "legends_guild", "legends_guild",
                "crafting_guild", "crafting_guild", "fishing_guild", "fishing_guild", "woodcutting_guild", "zeah_woodcutting_guild",
                "warriors_guild", "warriors_guild", "prifddinas", "prifddinas", "zanaris", "isl_zanaris", "entrana", "entrana");
        for (Map.Entry<String, String> e : structures.entrySet()) {
            StructureDef s = null;
            for (StructureDef d : ContentRegistry.structures()) {
                if (d.id().equals(e.getValue())) {
                    s = d;
                }
            }
            assertNotNull(s, e.getValue());
            AccessGates.Gate g = gate(e.getKey());
            double cx = com.runecraft.osrs.GielinorMap.toOsrsX((s.minX() + s.maxX()) / 2);
            double cy = com.runecraft.osrs.GielinorMap.toOsrsY((s.minZ() + s.maxZ()) / 2);
            assertTrue(g.contains(cx + 0.5, cy + 0.5), e.getKey() + " should cover the middle of " + e.getValue() + " at " + cx + "," + cy);
        }
        Map<String, String> places = Map.of("farming_guild", "farming_guild", "myths_guild", "myths_guild",
                "lunar_isle", "lunar_isle", "ape_atoll", "ape_atoll", "fossil_island", "fossil_island", "zanaris", "quest_zanaris");
        for (Map.Entry<String, String> e : places.entrySet()) {
            PlaceDef p = ContentRegistry.place(e.getValue());
            assertNotNull(p, e.getValue());
            assertTrue(gate(e.getKey()).contains(p.osrsX() + 0.5, p.osrsY() + 0.5), e.getKey() + " covers place " + e.getValue());
        }
    }

    @Test
    void morytaniaIsEastOfTheSalveOnly() {
        AccessGates.Gate m = gate("morytania");
        assertTrue(m.contains(3496, 3488), "Canifis");
        assertTrue(m.contains(3428, 3537), "Slayer Tower");
        assertTrue(m.contains(3660, 3480), "Port Phasmatys");
        assertTrue(m.contains(3490, 3230), "Burgh de Rott");
        assertFalse(m.contains(3413, 3487), "Paterdomus stays outside");
        assertFalse(m.contains(3222, 3218), "Lumbridge");
        assertFalse(m.contains(3365, 3440), "Digsite");
        assertFalse(m.contains(3300, 3180), "Al Kharid");
        assertFalse(m.contains(3390, 3600), "Wilderness east of the Salve's mouth is outside");
        assertSame(m, AccessGates.at(3496, 3488));
        double[] exit = m.exit(3496, 3488);
        assertFalse(m.contains(exit[0], exit[1]));
        assertNull(AccessGates.at(3222, 3218));
    }

    @Test
    void exitPointsAreOutside() {
        for (AccessGates.Gate g : AccessGates.all()) {
            for (AccessGates.Area a : g.areas()) {
                double x;
                double y;
                if (a instanceof AccessGates.Rect r) {
                    x = (r.x1() + r.x2()) / 2.0 + 0.5;
                    y = (r.y1() + r.y2()) / 2.0 + 0.5;
                } else if (a instanceof AccessGates.Circle c) {
                    x = c.cx();
                    y = c.cy();
                } else {
                    x = 3496;
                    y = 3488;
                }
                assertTrue(g.contains(x, y), g.id());
                double[] out = g.exit(x, y);
                assertFalse(g.contains(out[0], out[1]), g.id() + " exit " + out[0] + "," + out[1]);
            }
        }
    }

    // ---------------- gates: requirements

    @Test
    void skillGuildsNeedOsrsLevels() {
        Fake p = new Fake();
        Object[][] table = {{"fishing_guild", Skill.FISHING, 68}, {"mining_guild", Skill.MINING, 60},
                {"ranging_guild", Skill.RANGED, 40}, {"wizards_guild", Skill.MAGIC, 66},
                {"woodcutting_guild", Skill.WOODCUTTING, 60}, {"farming_guild", Skill.FARMING, 45}};
        for (Object[] row : table) {
            AccessGates.Gate g = gate((String) row[0]);
            p.levels.put((Skill) row[1], (Integer) row[2] - 1);
            assertNotNull(g.check(p), row[0] + " below level");
            p.levels.put((Skill) row[1], (Integer) row[2]);
            assertNull(g.check(p), row[0] + " at level");
        }
    }

    @Test
    void craftingAndCooksGuildsNeedTheirClothes() {
        Fake p = new Fake();
        p.levels.put(Skill.CRAFTING, 40);
        p.levels.put(Skill.COOKING, 32);
        assertTrue(gate("crafting_guild").check(p).contains("brown apron"));
        p.carried.add("brown_apron");
        assertNotNull(gate("crafting_guild").check(p), "carrying is not wearing");
        p.worn.add("brown_apron");
        assertNull(gate("crafting_guild").check(p));
        assertTrue(gate("cooks_guild").check(p).contains("chef's hat"));
        p.worn.add(AccessGates.CHEFS_HAT);
        assertNull(gate("cooks_guild").check(p));
        p.levels.put(Skill.COOKING, 31);
        assertTrue(gate("cooks_guild").check(p).contains("32"));
        p.worn.clear();
        p.carried.add("crafting_cape");
        assertNull(gate("crafting_guild").check(p), "Crafting cape works instead of the apron");
    }

    @Test
    void questGates() {
        Fake p = new Fake();
        String[][] done = {{"morytania", "priest_in_peril"}, {"heroes_guild", "heroes_quest"}, {"legends_guild", "legends_quest"},
                {"fossil_island", "bone_voyage"}, {"prifddinas", "song_of_the_elves"}};
        for (String[] d : done) {
            assertNotNull(gate(d[0]).check(p), d[0]);
            p.stages.put(d[1], 1);
            assertNotNull(gate(d[0]).check(p), d[0] + " needs the quest finished");
            p.done.add(d[1]);
            assertNull(gate(d[0]).check(p), d[0]);
        }
        String[][] started = {{"ape_atoll", "monkey_madness_i"}, {"lunar_isle", "lunar_diplomacy"}, {"myths_guild", "dragon_slayer_ii"}};
        for (String[] s : started) {
            assertNotNull(gate(s[0]).check(p), s[0]);
            p.stages.put(s[1], 1);
            assertNull(gate(s[0]).check(p), s[0] + " opens once started");
        }
        p.qp = 31;
        assertNotNull(gate("champions_guild").check(p));
        p.qp = 32;
        assertNull(gate("champions_guild").check(p));
    }

    @Test
    void zanarisNeedsLostCityAndDramenStaff() {
        Fake p = new Fake();
        AccessGates.Gate z = gate("zanaris");
        p.carried.add("dramen_staff");
        assertNotNull(z.check(p));
        p.done.add("lost_city");
        assertNull(z.check(p));
        p.carried.clear();
        assertTrue(z.check(p).contains("Dramen"));
    }

    @Test
    void warriorsGuildCombinedOr99() {
        Fake p = new Fake();
        AccessGates.Gate w = gate("warriors_guild");
        p.levels.put(Skill.ATTACK, 65);
        p.levels.put(Skill.STRENGTH, 64);
        assertNotNull(w.check(p));
        p.levels.put(Skill.STRENGTH, 65);
        assertNull(w.check(p));
        p.levels.put(Skill.ATTACK, 1);
        p.levels.put(Skill.STRENGTH, 99);
        assertNull(w.check(p));
    }

    @Test
    void entranaBansWeaponsAndArmourOnly() {
        Fake p = new Fake();
        assertNull(gate("entrana").check(p));
        p.combatGear = true;
        assertNotNull(gate("entrana").check(p));
        assertTrue(AccessGates.entranaBanned(ItemCatalog.get("bronze_sword")));
        assertTrue(AccessGates.entranaBanned(ItemCatalog.get("dharoks_helm")));
        assertFalse(AccessGates.entranaBanned(ItemCatalog.get("bronze_axe")));
        assertFalse(AccessGates.entranaBanned(ItemCatalog.get("brown_apron")));
        assertFalse(AccessGates.entranaBanned(ItemCatalog.get("coins")));
        assertFalse(AccessGates.entranaBanned(null));
    }

    @Test
    void farmingGuildTiers() {
        assertEquals(45, AccessGates.farmingGuildTier("bush_patch"));
        assertEquals(45, AccessGates.farmingGuildTier("cactus_patch"));
        assertEquals(65, AccessGates.farmingGuildTier("tree_patch"));
        assertEquals(85, AccessGates.farmingGuildTier("redwood_patch"));
        assertEquals(85, AccessGates.farmingGuildTier("celastrus_patch"));
        assertEquals(85, AccessGates.farmingGuildTier("spirit_tree_patch"));
        assertEquals(85, AccessGates.farmingGuildTier("fruit_tree_patch"));
        assertEquals(0, AccessGates.farmingGuildTier("bank_booth"));
        PlaceDef guild = ContentRegistry.place("farming_guild");
        for (String block : guild.blocks()) {
            assertTrue(AccessGates.farmingGuildTier(block) > 0, block + " has a tier");
        }
    }

    // ---------------- charges

    @Test
    void chargeSpecsAreRealItems() {
        for (Charges.Spec s : Charges.all()) {
            assertNotNull(ItemCatalog.get(s.id()), s.id());
            for (String m : s.materials().keySet()) {
                assertNotNull(ItemCatalog.get(m), s.id() + " material " + m);
            }
            assertTrue(s.initial() >= 0 && s.initial() <= s.max());
        }
        assertEquals(24, Charges.ofKind(Charges.Kind.BARROWS).size());
    }

    @Test
    void barrowsDegradesInFourStagesOver15Hours() {
        Charges.Spec helm = Charges.of("dharoks_helm");
        assertEquals(90_000, helm.max(), "15 hours of 0.6s ticks");
        assertEquals(100, Charges.barrowsStage(90_000));
        assertEquals(100, Charges.barrowsStage(67_501));
        assertEquals(75, Charges.barrowsStage(67_500));
        assertEquals(50, Charges.barrowsStage(45_000));
        assertEquals(25, Charges.barrowsStage(1));
        assertEquals(0, Charges.barrowsStage(0));
        assertTrue(Charges.depleted(helm, Charges.drain(helm, 5, 10)));
        assertEquals("Undamaged", Charges.describe(helm, 90_000));
        assertTrue(Charges.describe(helm, 0).startsWith("Broken"));
    }

    @Test
    void barrowsRepairCostsMatchOsrs() {
        int full = 0;
        int stand1 = 0;
        int stand99 = 0;
        for (String id : new String[]{"dharoks_helm", "dharoks_platebody", "dharoks_platelegs", "dharoks_greataxe"}) {
            Charges.Spec s = Charges.of(id);
            full += Charges.repairCost(s, 0);
            stand1 += Charges.armourStandCost(s, 0, 1);
            stand99 += Charges.armourStandCost(s, 0, 99);
            assertEquals(0, Charges.repairCost(s, s.max()));
        }
        assertEquals(330_000, full);
        assertEquals(328_350, stand1);
        assertEquals(166_650, stand99);
        assertEquals(30_000, Charges.repairCost(Charges.of("veracs_helm"), 45_000), "half worn, half price");
    }

    @Test
    void crystalRechargesWithShards() {
        Charges.Spec bow = Charges.of("bow_of_faerdhinen");
        assertEquals(20_000, bow.max());
        assertEquals(Charges.Drain.ATTACK, bow.drain());
        assertEquals(Charges.Drain.HIT_TAKEN, Charges.of("crystal_body").drain());
        assertEquals(500, Charges.rechargeable(bow, 0, Map.of("crystal_shard", 5)));
        assertEquals(Map.of("crystal_shard", 5), Charges.cost(bow, 500));
        assertEquals(0, Charges.rechargeable(bow, 19_950, Map.of("crystal_shard", 5)), "no partial shards");
        assertEquals(100, Charges.rechargeable(bow, 19_850, Map.of("crystal_shard", 5)));
    }

    @Test
    void tridentAndStaffChargeCosts() {
        Charges.Spec seas = Charges.of("trident_of_the_seas");
        assertEquals(2_500, seas.max());
        Map<String, Integer> inv = Map.of("death_rune", 100, "chaos_rune", 100, "fire_rune", 250, "coins", 10_000);
        assertEquals(50, Charges.rechargeable(seas, 0, inv), "fire runes limit to 50");
        assertEquals(Map.of("death_rune", 50, "chaos_rune", 50, "fire_rune", 250, "coins", 500), Charges.cost(seas, 50));
        assertEquals(10, Charges.rechargeable(seas, 2_490, inv), "capped at 2,500");
        Charges.Spec swamp = Charges.of("trident_of_the_swamp");
        assertEquals(0, Charges.rechargeable(swamp, 0, inv), "swamp needs scales");
        Charges.Spec sang = Charges.of("sanguinesti_staff");
        assertEquals(33, Charges.rechargeable(sang, 0, Map.of("blood_rune", 100)));
        assertEquals(0, sang.initial());
    }

    @Test
    void serpentineAndBlowpipeScaleUse() {
        assertEquals(10, Charges.serpentineDrain(0));
        assertEquals(0, Charges.serpentineDrain(1));
        assertEquals(0, Charges.serpentineDrain(89));
        assertEquals(10, Charges.serpentineDrain(90));
        long scales = 0;
        for (int t = 0; t < 6000; t++) {
            scales += Charges.serpentineDrain(t);
        }
        assertEquals(670, scales, "about 667 scales per hour of combat");
        int used = 0;
        for (int shot = 0; shot < 300; shot++) {
            used += Charges.blowpipeScales(shot);
        }
        assertEquals(200, used, "two scales per three shots");
        assertEquals(11_000, Charges.of("serpentine_helm").max());
        assertEquals(16_383, Charges.of("toxic_blowpipe").max());
    }
}
