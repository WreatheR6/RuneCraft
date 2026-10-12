package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.ItemCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Player-owned house values and mechanics checked against the OSRS Wiki. */
class PohFidelityTest {

    private static void furniture(String id, int level, double xp, Map<String, Integer> mats) {
        Poh.Furniture f = Poh.furniture(id);
        assertNotNull(f, id);
        assertEquals(level, f.level(), id + " level");
        assertEquals(xp, f.xp(), 1e-9, id + " xp");
        if (mats != null) {
            assertEquals(mats, f.materials(), id + " materials");
        }
    }

    @Test
    void furnitureMatchesTheWiki() {
        furniture("mahogany_bench", 52, 560, Map.of("mahogany_plank", 4));
        furniture("gilded_bench", 61, 1760, Map.of("mahogany_plank", 4, "gold_leaf", 4));
        furniture("mahogany_wardrobe", 75, 420, Map.of("mahogany_plank", 3));
        furniture("gilded_wardrobe", 87, 720, Map.of("mahogany_plank", 3, "gold_leaf", 1));
        furniture("mahogany_altar", 60, 590, Map.of("mahogany_plank", 4, "bolt_of_cloth", 2));
        furniture("limestone_altar", 64, 910, Map.of("mahogany_plank", 6, "bolt_of_cloth", 2, "limestone_brick", 2));
        furniture("gilded_altar", 75, 2230, Map.of("marble_block", 2, "bolt_of_cloth", 2, "gold_leaf", 4));
        furniture("restoration_pool", 65, 706, Map.of("limestone_brick", 5, "minecraft:water_bucket", 5, "soul_rune", 1000,
                "body_rune", 1000));
        furniture("revitalisation_pool", 70, 850, Map.of("stamina_potion", 10));
        furniture("rejuvenation_pool", 80, 900, Map.of("prayer_potion", 10));
        furniture("fancy_rejuvenation_pool", 85, 1950, Map.of("marble_block", 2, "super_restore", 10));
        furniture("ornate_rejuvenation_pool", 90, 3107, null);
        furniture("basic_jewellery_box", 81, 605, null);
        furniture("fancy_jewellery_box", 86, 1215, null);
        furniture("ornate_jewellery_box", 91, 2680, Map.of("gold_leaf", 2, "ring_of_wealth_5", 8, "amulet_of_glory_4", 8));
        furniture("teak_portal", 50, 270, Map.of("teak_plank", 3));
        furniture("marble_portal", 80, 1500, Map.of("marble_block", 3));
        for (Poh.Furniture f : Poh.FURNITURE.values()) {
            assertTrue(f.xp() >= 30, f.id() + " xp " + f.xp());
        }
    }

    @Test
    void altarMultipliersMatchTheChapelTable() {
        double[][] table = {{1.1, 1.6, 2.1}, {1.2, 1.7, 2.2}, {1.3, 1.8, 2.3}, {1.4, 1.9, 2.4}, {1.5, 2.0, 2.5}, {2.0, 2.5, 3.0},
                {2.5, 3.0, 3.5}};
        String[] altars = {"oak_altar", "teak_altar", "cloth_covered_altar", "mahogany_altar", "limestone_altar", "marble_altar",
                "gilded_altar"};
        for (int i = 0; i < altars.length; i++) {
            for (int burners = 0; burners <= 2; burners++) {
                assertEquals(table[i][burners], Poh.altarMultiplier(Poh.furniture(altars[i]), burners), 1e-9, altars[i] + " " + burners);
            }
        }
        assertEquals(1.0, Poh.altarMultiplier(Poh.furniture("oak_chair"), 2), 1e-9);
    }

    @Test
    void poolTiersRestoreWhatOsrsPoolsDo() {
        Poh.PoolEffects restoration = Poh.poolEffects(Poh.furniture("restoration_pool").power());
        assertTrue(restoration.special());
        assertFalse(restoration.run());
        Poh.PoolEffects revital = Poh.poolEffects(Poh.furniture("revitalisation_pool").power());
        assertTrue(revital.special() && revital.run() && !revital.prayer());
        Poh.PoolEffects rejuv = Poh.poolEffects(Poh.furniture("rejuvenation_pool").power());
        assertTrue(rejuv.prayer() && !rejuv.stats());
        Poh.PoolEffects fancy = Poh.poolEffects(Poh.furniture("fancy_rejuvenation_pool").power());
        assertTrue(fancy.stats() && !fancy.hitpoints());
        Poh.PoolEffects ornate = Poh.poolEffects(Poh.furniture("ornate_rejuvenation_pool").power());
        assertTrue(ornate.special() && ornate.run() && ornate.prayer() && ornate.stats() && ornate.hitpoints());
    }

    @Test
    void poolsAndJewelleryBoxesAreUpgrades() {
        HouseLayout h = HouseLayout.starter();
        h.buildRoom(Poh.room("superior_garden"), 3, 4);
        assertNotNull(h.canBuildFurniture(3, 4, "pool", "ornate_rejuvenation_pool", 99, 99), "needs the fancy pool first");
        assertNull(h.canBuildFurniture(3, 4, "pool", "restoration_pool", 65, 1));
        h.buildFurniture(3, 4, "pool", "restoration_pool");
        assertNotNull(h.canBuildFurniture(3, 4, "pool", "restoration_pool", 99, 1), "occupied");
        assertNotNull(h.canBuildFurniture(3, 4, "pool", "revitalisation_pool", 69, 1), "level");
        assertNull(h.canBuildFurniture(3, 4, "pool", "revitalisation_pool", 70, 1));
        assertNotNull(h.canBuildFurniture(3, 4, "pool", "rejuvenation_pool", 99, 1), "skips a tier");
        h.buildFurniture(3, 4, "pool", "revitalisation_pool");
        assertEquals("revitalisation_pool", h.furnitureAt(3, 4, "pool"));
        assertNull(h.canBuildFurniture(3, 4, "pool", "rejuvenation_pool", 80, 1));

        h.buildRoom(Poh.room("achievement_gallery"), 5, 4);
        assertNotNull(h.canBuildFurniture(5, 4, "jewellery_box", "ornate_jewellery_box", 99, 1));
        h.buildFurniture(5, 4, "jewellery_box", "basic_jewellery_box");
        h.buildFurniture(5, 4, "jewellery_box", "fancy_jewellery_box");
        assertNull(h.canBuildFurniture(5, 4, "jewellery_box", "ornate_jewellery_box", 91, 1));
        for (Map.Entry<String, String> e : Poh.UPGRADE_OF.entrySet()) {
            assertTrue(Poh.furniture(e.getKey()).level() > Poh.furniture(e.getValue()).level(), e.getKey());
            assertEquals(Poh.furniture(e.getKey()).effect(), Poh.furniture(e.getValue()).effect(), e.getKey());
        }
    }

    @Test
    void jewelleryBoxTiersAddTheirJewellery() {
        assertEquals(8, Poh.jewelleryDests(1).size());
        assertEquals(18, Poh.jewelleryDests(2).size());
        assertEquals(Poh.JEWELLERY_DESTS.size(), Poh.jewelleryDests(3).size());
        assertTrue(Poh.jewelleryDests(1).stream().anyMatch(d -> d.id().equals("castle_wars")));
        assertTrue(Poh.jewelleryDests(3).stream().anyMatch(d -> d.id().equals("edgeville")));
        assertFalse(Poh.jewelleryDests(2).stream().anyMatch(d -> d.id().equals("edgeville")));
    }

    @Test
    void lecternTabletsNeedTheRightLectern() {
        assertEquals(List.of("varrock_teleport_tablet"), Poh.tablets(1).stream().map(Poh.Tablet::item).toList());
        List<String> teak = Poh.tablets(3).stream().map(Poh.Tablet::item).toList();
        assertTrue(teak.contains("camelot_teleport_tablet") && teak.contains("ardougne_teleport_tablet"));
        assertFalse(teak.contains(Poh.HOUSE_TABLET), "house tablets need a mahogany eagle lectern");
        assertFalse(teak.contains("watchtower_teleport_tablet"));
        assertTrue(Poh.tablets(4).stream().anyMatch(t -> t.item().equals(Poh.HOUSE_TABLET)));
        // tablet XP = the spell's XP
        Map<String, Double> xp = Map.of("varrock_teleport_tablet", 35.0, "lumbridge_teleport_tablet", 41.0, "falador_teleport_tablet",
                48.0, Poh.HOUSE_TABLET, 30.0, "camelot_teleport_tablet", 55.5, "ardougne_teleport_tablet", 61.0,
                "watchtower_teleport_tablet", 68.0);
        for (Poh.Tablet t : Poh.TABLETS) {
            assertEquals(xp.get(t.item()), t.xp(), 1e-9, t.item());
        }
    }

    @Test
    void portalNexusStoresTeleports() {
        assertEquals(72, Poh.room("portal_nexus").level());
        assertEquals(200000, Poh.room("portal_nexus").cost());
        assertEquals(1000, Poh.NEXUS_CASTS);
        assertTrue(Poh.NEXUS_DESTS.containsAll(Poh.PORTAL_DESTS));
        List<String> ids = new ArrayList<>();
        for (Poh.Dest d : Poh.NEXUS_DESTS) {
            assertTrue(!ids.contains(d.id()), "duplicate " + d.id());
            ids.add(d.id());
            d.runes().keySet().forEach(r -> assertTrue(ItemCatalog.exists(r), r));
        }
        List<String> stored = new ArrayList<>();
        assertNull(Poh.canAddToNexus(1, stored, "varrock", 25));
        assertNotNull(Poh.canAddToNexus(1, stored, "varrock", 24), "magic level");
        assertNotNull(Poh.canAddToNexus(1, stored, "nowhere", 99));
        for (String id : List.of("varrock", "lumbridge", "falador", "camelot", "ardougne", "yanille", "trollheim")) {
            assertNull(Poh.canAddToNexus(1, stored, id, 99), id);
            stored.add(id);
        }
        assertNotNull(Poh.canAddToNexus(1, stored, "kharyrll", 99), "a marble nexus is full");
        assertNull(Poh.canAddToNexus(2, stored, "kharyrll", 99));
        assertNotNull(Poh.canAddToNexus(3, stored, "varrock", 99), "already stored");
        assertEquals(List.of("varrock", "camelot"), Poh.nexusList("varrock,bogus,camelot,varrock"));
        assertTrue(Poh.nexusList(null).isEmpty());

        // An upgrade keeps the stored teleports.
        HouseLayout h = HouseLayout.starter();
        h.buildRoom(Poh.room("portal_nexus"), 4, 5);
        h.buildFurniture(4, 5, "nexus", "marble_portal_nexus");
        h.setExtra(4, 5, "nexus", "varrock,camelot");
        assertNull(h.canBuildFurniture(4, 5, "nexus", "gilded_portal_nexus", 82, 1));
        h.buildFurniture(4, 5, "nexus", "gilded_portal_nexus");
        assertEquals("gilded_portal_nexus", h.furnitureAt(4, 5, "nexus"));
        assertEquals("varrock,camelot", h.extraAt(4, 5, "nexus"));
        assertEquals("varrock,camelot", HouseLayout.decode(h.encode()).extraAt(4, 5, "nexus"));
    }

    @Test
    void servantsFollowOsrs() {
        assertEquals(List.of("rick", "maid", "cook", "butler", "demon_butler"), Poh.SERVANTS.stream().map(Poh.Servant::id).toList());
        int[] levels = {20, 25, 30, 40, 50};
        int[] wages = {500, 1000, 3000, 5000, 10000};
        int[] capacity = {6, 10, 16, 20, 26};
        for (int i = 0; i < levels.length; i++) {
            Poh.Servant s = Poh.SERVANTS.get(i);
            assertEquals(levels[i], s.level(), s.id());
            assertEquals(wages[i], s.wage(), s.id());
            assertEquals(capacity[i], s.capacity(), s.id());
            if (i > 0) {
                assertTrue(s.tripTicks() < Poh.SERVANTS.get(i - 1).tripTicks(), s.id() + " is faster");
            }
        }
        assertFalse(Poh.servant("rick").sawmill());
        assertTrue(Poh.servant("demon_butler").sawmill());
        assertNotNull(Poh.canHire(Poh.servant("demon_butler"), 49, 2));
        assertNotNull(Poh.canHire(Poh.servant("demon_butler"), 50, 1), "needs two beds");
        assertNull(Poh.canHire(Poh.servant("demon_butler"), 50, 2));
        assertEquals(1500, Poh.SAWMILL.get(3).fee());
        Poh.SAWMILL.forEach(m -> assertTrue(ItemCatalog.exists(m.logs()) && ItemCatalog.exists(m.plank()), m.logs()));
        Poh.FETCHABLE.forEach(i -> assertTrue(ItemCatalog.exists(i), i));

        HouseLayout h = HouseLayout.starter();
        assertEquals(0, h.count(Poh.Effect.BED));
        h.buildRoom(Poh.room("bedroom"), 5, 4);
        h.buildRoom(Poh.room("bedroom"), 3, 4);
        h.buildFurniture(5, 4, "bed", "oak_bed");
        h.buildFurniture(3, 4, "bed", "teak_bed");
        assertEquals(2, h.count(Poh.Effect.BED));
        assertEquals(1, h.count(Poh.Effect.EXIT_PORTAL));
    }

    @Test
    void houseLocationsHaveEstateAgentLevelsAndFees() {
        Map<String, int[]> expected = Map.of("rimmington", new int[]{1, 5000}, "taverley", new int[]{10, 5000},
                "pollnivneach", new int[]{20, 7500}, "hosidius", new int[]{25, 8750}, "rellekka", new int[]{30, 10000},
                "brimhaven", new int[]{40, 15000}, "yanille", new int[]{50, 25000}, "prifddinas", new int[]{70, 50000});
        assertEquals(expected.size(), ExtrasContent.PORTAL_TOWNS.size());
        expected.forEach((id, v) -> {
            ExtrasContent.PortalTown t = ExtrasContent.portalTown(id);
            assertEquals(v[0], t.level(), id);
            assertEquals(v[1], t.moveCost(), id);
        });
        assertEquals("song_of_the_elves", ExtrasContent.portalTown("prifddinas").quest());
        ExtrasContent.PortalTown yanille = ExtrasContent.portalTown("yanille");
        assertNull(ExtrasContent.canMove(yanille, "rimmington", 50, true, 25000));
        assertNotNull(ExtrasContent.canMove(yanille, "rimmington", 49, true, 25000));
        assertNotNull(ExtrasContent.canMove(yanille, "rimmington", 50, true, 24999));
        assertNotNull(ExtrasContent.canMove(yanille, "yanille", 99, true, 99999));
        assertNotNull(ExtrasContent.canMove(ExtrasContent.portalTown("prifddinas"), "rimmington", 99, false, 99999));
        assertNotNull(ExtrasContent.portalTown(ExtrasContent.START_TOWN));
    }
}
