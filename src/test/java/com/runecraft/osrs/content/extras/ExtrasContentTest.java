package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtrasContentTest {

    // ------------------------------------------------------------------ pets

    @Test
    void everyPetIsAnItemAndInTheAllPetsPage() {
        CollectionLogData.Page page = CollectionLogData.pages().stream().filter(p -> p.name().equals("All Pets")).findFirst()
                .orElseThrow();
        Set<String> ids = new HashSet<>();
        for (Pets.PetDef p : Pets.all()) {
            assertTrue(ids.add(p.id()), "duplicate pet " + p.id());
            assertNotNull(ItemCatalog.get(p.id()), "pet item missing: " + p.id());
            assertTrue(page.items().contains(p.id()), p.id());
            assertTrue(ContentRegistry.models().containsKey(p.look()), p.id() + " look " + p.look());
            assertTrue(CollectionLogData.tracked(p.id()));
        }
        assertEquals(Pets.all().size(), page.items().size());
    }

    @Test
    void bossPetsMatchTheirBossDefinitions() {
        for (Pets.PetDef p : Pets.all()) {
            if (p.source() != Pets.Source.BOSS) {
                continue;
            }
            for (String boss : p.sources()) {
                BossDef def = ContentRegistry.boss(boss);
                if (def != null) {
                    assertEquals(p.id(), def.pet, boss + " pet");
                }
            }
        }
    }

    @Test
    void bossPetRates() {
        assertEquals(5000, Pets.engineRate("dagannoth_rex"));
        assertEquals(5000, Pets.engineRate("corporeal_beast"));
        assertEquals(4000, Pets.engineRate("zulrah"));
        assertEquals(2560, Pets.engineRate("abyssal_sire"));
        assertEquals(800, Pets.engineRate("corrupted_hunllef"));
        assertEquals(1400, Pets.engineRate("phosanis_nightmare"));
        assertEquals(5000, Pets.engineRate("kreearra"));
        assertEquals(0, Pets.engineRate("skotizo"));
        // scripted top-ups assume the default engine roll
        assertEquals(Pets.DEFAULT_ENGINE_RATE, Pets.engineRate("callisto"));
        assertEquals(Pets.DEFAULT_ENGINE_RATE, Pets.engineRate("nex"));
        assertEquals(Pets.DEFAULT_ENGINE_RATE, Pets.engineRate("some_unlisted_boss"));
        assertEquals(1500, Pets.rate("callisto_cub", "callisto"));
        assertEquals(6500, Pets.rate("pet_herbi", "herbiboar"));
        assertEquals(500, Pets.rate("pet_chompy_chick", "chompy_bird"));
    }

    @Test
    void skillingPetFormula() {
        // 1 / (base - level * 25)
        assertEquals(1.0 / (317_647 - 99 * 25), Pets.skillingChance(317_647, 99), 1e-15);
        assertEquals(1.0 / (42_377 - 85 * 25), Pets.skillingChance(42_377, 85), 1e-15);
        assertTrue(Pets.skillingChance(82_243, 99) > Pets.skillingChance(82_243, 76));
        SkillingPets.Roll magic = SkillingPets.forItem("magic_logs");
        assertEquals(SkillingPets.BEAVER, magic.pet());
        assertEquals(Skill.WOODCUTTING, magic.skill());
        assertEquals(72_321, magic.base());
        assertEquals(42_377, SkillingPets.forItem("runite_ore").base());
        assertEquals(82_243, SkillingPets.forItem("raw_shark").base());
        assertEquals(82_758, SkillingPets.forItem("black_chinchompa").base());
        assertEquals(98_364, SkillingPets.forItem("grimy_ranarr_weed").base());
        assertNull(SkillingPets.forItem("abyssal_whip"));
        assertEquals(36_490, SkillingPets.thievingBase(Experience.tenths(408.0)));
        assertEquals(36_490, SkillingPets.thievingBase("gem_stall", Experience.tenths(408.0)));
        // Same XP, different rates: a watchman and a Menaphite thug both give 137.5 XP.
        assertEquals(134_625, SkillingPets.thievingBase("Watchman", Experience.tenths(137.5)));
        assertEquals(SkillingPets.THIEVING_DEFAULT, SkillingPets.thievingBase("menaphite thug", Experience.tenths(137.5)));
        assertEquals(206_777, SkillingPets.thievingBase("vegetable_stall", Experience.tenths(10.0)));
        assertEquals(36_490, SkillingPets.thievingBase("seed_stall", Experience.tenths(10.0)));
        assertEquals(0, SkillingPets.thievingBase("ardougne_castle_chest", Experience.tenths(500.0)));
        assertEquals(SkillingPets.THIEVING_DEFAULT, SkillingPets.thievingBase(Experience.tenths(8.0)));
        assertTrue(SkillingPets.agilityLapBase(Experience.tenths(com.runecraft.osrs.data.AgilityCourse.ARDOUGNE.lapBonusXp())) > 0);
        for (SkillingPets.Roll r : SkillingPets.items().values()) {
            assertNotNull(Pets.get(r.pet()), r.pet());
        }
    }

    // ------------------------------------------------------------------ house

    @Test
    void roomsHaveOsrsLevelsAndCosts() {
        Map<String, int[]> expected = Map.ofEntries(
                Map.entry("parlour", new int[]{1, 1000}), Map.entry("garden", new int[]{1, 1000}),
                Map.entry("kitchen", new int[]{5, 5000}), Map.entry("dining_room", new int[]{10, 5000}),
                Map.entry("workshop", new int[]{15, 10000}), Map.entry("bedroom", new int[]{20, 10000}),
                Map.entry("combat_room", new int[]{32, 25000}), Map.entry("menagerie", new int[]{37, 30000}),
                Map.entry("study", new int[]{40, 50000}), Map.entry("costume_room", new int[]{42, 50000}),
                Map.entry("chapel", new int[]{45, 50000}), Map.entry("portal_chamber", new int[]{50, 100000}),
                Map.entry("throne_room", new int[]{60, 150000}), Map.entry("oubliette", new int[]{65, 150000}),
                Map.entry("superior_garden", new int[]{65, 75000}), Map.entry("dungeon", new int[]{70, 7500}),
                Map.entry("treasure_room", new int[]{75, 250000}), Map.entry("achievement_gallery", new int[]{80, 200000}),
                Map.entry("portal_nexus", new int[]{72, 200000}));
        assertEquals(expected.size(), Poh.ROOMS.size());
        expected.forEach((id, lc) -> {
            Poh.Room r = Poh.room(id);
            assertNotNull(r, id);
            assertEquals(lc[0], r.level(), id);
            assertEquals(lc[1], r.cost(), id);
        });
    }

    @Test
    void furnitureIsValid() {
        for (Poh.Room r : Poh.ROOMS) {
            Set<String> spots = new HashSet<>();
            for (Poh.Hotspot h : r.hotspots()) {
                assertTrue(spots.add(h.dx() + "," + h.dz()), r.id() + " overlapping hotspots");
                assertTrue(h.dx() >= 1 && h.dx() <= Poh.ROOM - 2 && h.dz() >= 1 && h.dz() <= Poh.ROOM - 2, r.id() + " " + h.id());
                assertTrue(!(h.dx() >= 5 && h.dx() <= 6 && (h.dz() == 1 || h.dz() == Poh.ROOM - 2)), r.id() + " blocks a door");
                assertTrue(!(h.dz() >= 5 && h.dz() <= 6 && (h.dx() == 1 || h.dx() == Poh.ROOM - 2)), r.id() + " blocks a door");
                assertTrue(!h.options().isEmpty(), h.id());
                for (Poh.Furniture f : h.options()) {
                    assertTrue(f.level() >= 1 && f.level() <= 99, f.id());
                    assertTrue(f.xp() > 0, f.id());
                    assertTrue(!f.materials().isEmpty(), f.id());
                    f.materials().keySet().forEach(m -> assertTrue(ItemCatalog.exists(m), f.id() + " material " + m));
                }
            }
        }
        // spot checks against OSRS
        Poh.Furniture larder = Poh.furniture("oak_larder");
        assertEquals(33, larder.level());
        assertEquals(480.0, larder.xp());
        assertEquals(8, larder.materials().get("oak_plank"));
        assertEquals(75, Poh.furniture("gilded_altar").level());
        assertEquals(2230.0, Poh.furniture("gilded_altar").xp());
        assertEquals(90, Poh.furniture("ornate_rejuvenation_pool").level());
        assertEquals(Skill.FARMING, Poh.furniture("spirit_tree").otherSkill());
        assertEquals(83, Poh.furniture("spirit_tree").otherLevel());
        assertEquals(3.5, Poh.altarMultiplier(Poh.furniture("gilded_altar"), 2), 1e-9);
        assertEquals(1.1, Poh.altarMultiplier(Poh.furniture("oak_altar"), 0), 1e-9);
        assertEquals(3.5, Poh.altarMultiplier(Poh.furniture("gilded_altar"), 5), 1e-9);
        for (Poh.Tablet t : Poh.TABLETS) {
            assertTrue(ItemCatalog.exists(t.item()), t.item());
            t.runes().keySet().forEach(r -> assertTrue(ItemCatalog.exists(r), r));
        }
        for (Poh.Dest d : Poh.PORTAL_DESTS) {
            d.runes().keySet().forEach(r -> assertTrue(ItemCatalog.exists(r), r));
        }
        assertEquals(1, Poh.tablets(1).size());
        assertEquals(Poh.TABLETS.size(), Poh.tablets(4).size());
        assertTrue(Poh.jewelleryDests(3).size() > Poh.jewelleryDests(1).size());
    }

    @Test
    void houseLayoutRules() {
        HouseLayout h = HouseLayout.starter();
        assertEquals("garden", h.roomAt(4, 4));
        assertEquals("parlour", h.roomAt(4, 3));
        assertEquals("exit_portal", h.furnitureAt(4, 4, "centrepiece"));
        Poh.Room kitchen = Poh.room("kitchen");
        assertNotNull(h.canBuildRoom(kitchen, 5, 3, 4), "needs level 5");
        assertNull(h.canBuildRoom(kitchen, 5, 3, 5));
        assertNotNull(h.canBuildRoom(kitchen, 7, 7, 99), "must be adjacent");
        assertNotNull(h.canBuildRoom(kitchen, 4, 3, 99), "occupied");
        assertNotNull(h.canBuildRoom(kitchen, 9, 3, 99), "outside grid");
        h.buildRoom(kitchen, 5, 3);
        assertNotNull(h.canBuildFurniture(5, 3, "larder", "oak_larder", 32, 1));
        assertNull(h.canBuildFurniture(5, 3, "larder", "oak_larder", 33, 1));
        assertNotNull(h.canBuildFurniture(5, 3, "larder", "gilded_altar", 99, 1), "wrong hotspot");
        h.buildFurniture(5, 3, "larder", "oak_larder");
        assertNotNull(h.canBuildFurniture(5, 3, "larder", "teak_larder", 99, 1), "occupied");
        // removing the parlour would cut off the kitchen (only joined through it)
        assertNotNull(h.canRemoveRoom(4, 3));
        assertNotNull(h.canRemoveRoom(4, 4));
        assertNull(h.canRemoveRoom(5, 3));
        // spirit tree needs 83 Farming
        h.buildRoom(Poh.room("superior_garden"), 3, 4);
        assertNotNull(h.canBuildFurniture(3, 4, "teleport", "spirit_tree", 99, 82));
        assertNull(h.canBuildFurniture(3, 4, "teleport", "spirit_tree", 75, 83));
        // persistence round trip
        h.buildRoom(Poh.room("portal_chamber"), 4, 5);
        h.buildFurniture(4, 5, "portal_1", "teak_portal");
        h.setExtra(4, 5, "portal_1", "varrock");
        HouseLayout back = HouseLayout.decode(h.encode());
        assertEquals(h.encode(), back.encode());
        assertEquals("varrock", back.extraAt(4, 5, "portal_1"));
        assertEquals("teak_portal", back.furnitureAt(4, 5, "portal_1"));
        assertEquals("oak_larder", back.furnitureAt(5, 3, "larder"));
        h.removeRoom(5, 3);
        assertNull(h.furnitureAt(5, 3, "larder"));
        assertEquals(20, Poh.maxRooms(1));
        assertEquals(30, Poh.maxRooms(99));
        assertEquals("garden", HouseLayout.decode("garbage").roomAt(4, 4));
        Map<String, Integer> missing = HouseLayout.missing(Poh.furniture("teak_larder"), item -> item.equals("teak_plank") ? 5 : 0);
        assertEquals(3, missing.get("teak_plank"));
        assertEquals(2, missing.get("bolt_of_cloth"));
    }

    @Test
    void housePortalsCoverEveryOsrsLocation() {
        List<String> towns = ExtrasContent.PORTAL_TOWNS.stream().map(ExtrasContent.PortalTown::name).toList();
        assertEquals(List.of("Rimmington", "Taverley", "Pollnivneach", "Rellekka", "Brimhaven", "Yanille", "Hosidius", "Prifddinas"),
                towns);
    }

    // ------------------------------------------------------------------ sailing

    @Test
    void sailingBoatsPortsAndXp() {
        assertEquals(List.of("raft", "skiff", "sloop"), Seafaring.BOATS.stream().map(Seafaring.BoatTier::id).toList());
        int last = 0;
        double speed = 0;
        for (Seafaring.BoatTier b : Seafaring.BOATS) {
            assertTrue(b.level() >= last, b.id());
            assertTrue(Seafaring.terminalSpeed(b.speed()) > speed, b.id() + " should be faster");
            last = b.level();
            speed = Seafaring.terminalSpeed(b.speed());
            assertTrue(ItemCatalog.exists(b.deed()), b.deed());
            b.build().keySet().forEach(m -> assertTrue(ItemCatalog.exists(m), b.id() + " material " + m));
        }
        assertEquals(31, Seafaring.PORTS.size());
        Set<String> names = new HashSet<>();
        Seafaring.PORTS.forEach(p -> names.add(p.name()));
        for (String n : List.of("Port Sarim", "Catherby", "Brimhaven", "Port Khazard", "Rellekka", "Port Piscarilius", "Land's End",
                "Corsair Cove", "Port Tyras", "Port Phasmatys", "Sunset Coast", "Aldarin", "Port Roberts", "Deepfin Point", "The Summer Shore")) {
            assertTrue(names.contains(n), n);
        }
        Seafaring.Port sarim = Seafaring.port("port_sarim");
        Seafaring.Port catherby = Seafaring.port("catherby");
        Seafaring.Port aldarin = Seafaring.port("aldarin");
        Seafaring.BoatTier raft = Seafaring.boat("raft");
        Seafaring.BoatTier sloop = Seafaring.boat("sloop");
        assertTrue(Seafaring.voyageXp(sarim, catherby, sloop) > Seafaring.voyageXp(sarim, catherby, raft));
        assertTrue(Seafaring.voyageXp(sarim, aldarin, raft) > Seafaring.voyageXp(sarim, catherby, raft));
        assertNull(Seafaring.canVoyage(20, raft, sarim, catherby));
        assertNotNull(Seafaring.canVoyage(19, raft, sarim, catherby));
        assertNotNull(Seafaring.canVoyage(99, null, sarim, catherby));
        assertNotNull(Seafaring.canVoyage(30, sloop, sarim, catherby));
        assertNotNull(Seafaring.canVoyage(99, sloop, sarim, sarim));
        assertTrue(Seafaring.charterFare(sarim, aldarin) > Seafaring.charterFare(sarim, catherby));
        assertEquals(50, Seafaring.FEATURES.get("Sail a sloop"));
    }
}
