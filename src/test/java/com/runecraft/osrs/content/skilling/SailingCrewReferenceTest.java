package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sailing part 2 diffed against the OSRS Wiki snapshots in src/test/resources/osrs/sailing (July 2026 dump): crewmates,
 * cargo holds, repair kits, chainshot and incendiary cannonballs; plus hull speed, crew formulas and island materials.
 */
class SailingCrewReferenceTest {

    private static List<String[]> tsv(String name) throws Exception {
        List<String[]> out = new ArrayList<>();
        Path p = Path.of("src/test/resources/osrs/sailing", name);
        assertTrue(Files.exists(p), p.toString());
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                out.add(line.split("\t", -1));
            }
        }
        return out;
    }

    @Test
    void crewmatesMatchTheWiki() throws Exception {
        List<String[]> rows = tsv("crewmates.tsv");
        assertEquals(rows.size(), SailingCrew.CREW.size());
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            SailingCrew.Crewmate c = SailingCrew.CREW.get(i);
            assertEquals(r[0], c.name());
            assertEquals(Integer.parseInt(r[1]), c.level(), r[0]);
            assertEquals(r[2], c.location(), r[0]);
            assertEquals(Integer.parseInt(r[3]), c.helmsmanship(), r[0]);
            assertEquals(Integer.parseInt(r[4]), c.privateering(), r[0]);
            assertEquals(Integer.parseInt(r[5]), c.deckhandiness(), r[0]);
            assertEquals(Integer.parseInt(r[6]), c.crates(), r[0]);
            // Quest requirements are checked; Spotter Virginia's 100 Tempoross subdues are not (approx).
            assertEquals(r[7].equals("None") || r[7].contains("Tempoross"), c.quest() == null, r[0]);
        }
        assertEquals(0, SailingCrew.crewSlots(39));
        assertEquals(1, SailingCrew.crewSlots(40));
        assertEquals(2, SailingCrew.crewSlots(55));
        assertEquals(3, SailingCrew.crewSlots(70));
        assertEquals(4, SailingCrew.crewSlots(85));
        assertEquals(5, SailingCrew.crewSlots(99));
        assertEquals(0, SailingCrew.boatCrew("raft"));
        assertEquals(2, SailingCrew.boatCrew("skiff"));
        assertEquals(5, SailingCrew.boatCrew("sloop"));
        List<String> order = List.of("jobless_jim", "adventurer_ada", "jittery_jim");
        assertEquals(2, SailingCrew.aboard(order, "skiff", 99).size());
        assertEquals(1, SailingCrew.aboard(order, "sloop", 54).size());
        assertEquals(0, SailingCrew.aboard(order, "raft", 99).size());
    }

    @Test
    void crewFormulasFollowTheWiki() {
        assertEquals(0.4, SailingCrew.trimShare(1), 1e-9);
        assertEquals(0.7, SailingCrew.trimShare(4), 1e-9);
        assertEquals(0.1, SailingCrew.salvageShare(1), 1e-9);
        assertEquals(0.4, SailingCrew.salvageShare(4), 1e-9);
        assertEquals(2, SailingCrew.bestHook(1)); // steel
        assertEquals(6, SailingCrew.bestHook(4)); // dragon
        SailingSea.Cannon rune = SailingSea.CANNONS.get(5);
        SailingSea.Ball ball = SailingSea.ball("rune_cannonball");
        assertFalse(SailingCrew.canMan(rune, 3));
        assertTrue(SailingCrew.canMan(rune, 4));
        int base = SailingSea.cannonMaxHit(90, rune, ball, false);
        assertEquals(base * 17 / 20, SailingCrew.crewMaxHit(90, rune, ball, 4));
        assertEquals(base * 14 / 20, SailingCrew.crewMaxHit(90, rune, ball, 1));
        SailingCrew.Kit kit = SailingCrew.kit("oak_repair_kit");
        Random random = new Random(1);
        for (int i = 0; i < 50; i++) {
            int heal = SailingCrew.crewRepair(kit, 4, random);
            assertTrue(heal >= 10 && heal <= 13, "heal " + heal);
            assertEquals(10, SailingCrew.crewRepair(kit, 1, random));
        }
    }

    @Test
    void cargoHoldsMatchTheWiki() throws Exception {
        List<String[]> rows = tsv("cargo_holds.tsv");
        assertEquals(rows.size(), SailingCrew.HOLDS.size());
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            SailingCrew.Hold h = SailingCrew.HOLDS.get(i);
            assertEquals(r[0], h.name());
            assertEquals(Integer.parseInt(r[1]), h.sailing(), r[0]);
            assertEquals(Integer.parseInt(r[2]), h.construction(), r[0]);
            assertEquals(Double.parseDouble(r[3]), h.conXp(), 1e-9, r[0]);
            Map<String, Integer> mats = new LinkedHashMap<>();
            for (String kv : r[4].split(";")) {
                String[] p = kv.split(":");
                mats.put(p[0], Integer.parseInt(p[1]));
            }
            assertEquals(mats, h.materials(), r[0]);
            assertEquals(Integer.parseInt(r[5]), h.capacity("raft"), r[0]);
            assertEquals(Integer.parseInt(r[6]), h.capacity("skiff"), r[0]);
            assertEquals(Integer.parseInt(r[7]), h.capacity("sloop"), r[0]);
            mats.keySet().forEach(m -> assertTrue(ItemCatalog.exists(m), m));
        }
        assertEquals("teak_cargo_hold", SailingCrew.bestHold(Set.of("basic_cargo_hold", "teak_cargo_hold")).id());
        assertNull(SailingCrew.bestHold(Set.of()));
        // Every facility id is unique and the cargo holds are buildable facilities.
        List<String> ids = SailingCrew.allFacilities().stream().map(SailingTraining.Facility::id).toList();
        assertEquals(ids.size(), Set.copyOf(ids).size());
        assertTrue(ids.contains("rosewood_cargo_hold"));
        // Capacity: stackables take one slot for the whole stack.
        Map<String, Integer> hold = new HashMap<>(Map.of("bronze_cannonball", 500, "repair_kit", 18));
        assertEquals(19, SailingCrew.slotsUsed(hold, i -> i.endsWith("cannonball")));
        assertEquals(1, SailingCrew.room(hold, "repair_kit", 5, 20, i -> i.endsWith("cannonball")));
        assertEquals(100, SailingCrew.room(hold, "bronze_cannonball", 100, 19, i -> i.endsWith("cannonball")));
        assertTrue(SailingCrew.storable("oak_repair_kit") && SailingCrew.storable("iron_chainshot_cannonball"));
        assertFalse(SailingCrew.storable("logs"));
        assertTrue(SailingCrew.lostOnSinking("cargo_crate"));
        assertFalse(SailingCrew.lostOnSinking("repair_kit"));
        assertFalse(SailingCrew.lostOnSinking("rune_cannonball"));
    }

    @Test
    void repairKitsMatchTheWiki() throws Exception {
        List<String[]> rows = tsv("repair_kits.tsv");
        assertEquals(rows.size(), SailingCrew.KITS.size());
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            SailingCrew.Kit k = SailingCrew.KITS.get(i);
            assertEquals(r[0], k.name());
            assertEquals(Integer.parseInt(r[1]), k.heals(), r[0]);
            Recipe rec = Recipes.get("build_" + k.id());
            assertNotNull(rec, k.id());
            assertEquals(Integer.parseInt(r[2]), rec.level(), r[0]);
            assertEquals(Double.parseDouble(r[3]), rec.xpTenths() / 10.0, 1e-9, r[0]);
            assertEquals(Integer.parseInt(r[5]), rec.inputs().get(r[4]), r[0]);
            assertEquals(Integer.parseInt(r[7]), rec.inputs().get(r[6]), r[0]);
            assertEquals(Integer.parseInt(r[8]), rec.inputs().get("swamp_paste"), r[0]);
            assertEquals(Integer.parseInt(r[9]), rec.outputs().get(k.id()), r[0]);
            assertTrue(ItemCatalog.exists(k.id()), k.id());
        }
    }

    @Test
    void specialCannonballsMatchTheWiki() throws Exception {
        List<String[]> rows = tsv("special_cannonballs.tsv");
        assertEquals(rows.size(), SailingCrew.SPECIALS.size());
        for (String[] r : rows) {
            String id = r[0].toLowerCase().replace(' ', '_');
            SailingCrew.Special s = SailingCrew.special(id);
            assertNotNull(s, id);
            Recipe rec = Recipes.get("smith_" + id);
            assertNotNull(rec, id);
            assertEquals(Integer.parseInt(r[1]), rec.level(), id);
            assertEquals(Double.parseDouble(r[2]), rec.xpTenths() / 10.0, 1e-9, id);
            assertEquals(Integer.parseInt(r[4]), rec.inputs().get(s.base()), id);
            assertEquals(Integer.parseInt(r[6]), rec.inputs().get(r[5].toLowerCase().replace(' ', '_')), id);
            assertEquals(Integer.parseInt(r[7]), rec.outputs().get(id), id);
            assertEquals(r[0].contains("chainshot"), s.chainshot(), id);
            // Same damage as the plain ball (wiki).
            assertEquals(SailingSea.ball(s.base()), SailingCrew.ballStats(id), id);
        }
        Recipe chain = Recipes.get("smith_chain");
        assertEquals(34, chain.level());
        assertEquals(37.5, chain.xpTenths() / 10.0, 1e-9);
    }

    @Test
    void hullSpeedFollowsTheHullTable() {
        double[] wiki = {1.5, 1.5, 2.0, 2.0, 2.5, 2.5, 3.0};
        for (String boat : SailingTraining.BOATS) {
            for (int t = 0; t < wiki.length; t++) {
                assertEquals(wiki[t], SailingCrew.hullSpeed(boat, t), 1e-9, boat + " " + t);
            }
        }
        assertEquals(1.0, SailingCrew.velocityMultiplier(SailingCrew.speedFactor("skiff", 0)), 1e-9);
        double m = SailingCrew.velocityMultiplier(SailingCrew.speedFactor("skiff", 6));
        // A vanilla boat (momentum 0.9, thrust 0.04) with this multiplier settles at twice its top speed.
        double v = 0;
        for (int i = 0; i < 2000; i++) {
            v = 0.9 * m * v + 0.04;
        }
        assertEquals(0.8, v, 1e-6);
    }

    @Test
    void islandMaterialsAreGatheredNotSold() {
        for (String id : List.of("camphor_logs", "ironwood_logs", "rosewood_logs", "camphor_plank", "ironwood_plank", "rosewood_plank",
                "lead_bar", "cupronickel_bar")) {
            assertNull(Recipes.get("shop_tools_" + id), id);
        }
        assertEquals(2500, Recipes.get("saw_camphor_plank").inputs().get("coins"));
        assertEquals(5000, Recipes.get("saw_ironwood_plank").inputs().get("coins"));
        assertEquals(7500, Recipes.get("saw_rosewood_plank").inputs().get("coins"));
        Recipe cupro = Recipes.get("smelt_cupronickel_bar");
        assertEquals(74, cupro.level());
        assertEquals(2, cupro.inputs().get("copper_ore"));
        assertEquals(1, cupro.inputs().get("nickel_ore"));
        assertTrue(MiningData.ROCKS.stream().anyMatch(r -> r.blockId().equals("nickel_rock") && r.level() == 74));
        assertTrue(ResourceSites.SITES.stream().anyMatch(s -> s.block().equals("nickel_rock")));
        assertTrue(ResourceSites.SITES.stream().anyMatch(s -> s.block().equals("rubium_rock")));
        assertEquals(36, SailingCrew.SHARK_PAINT_RATE);
    }
}
