package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Experience;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.extras.Seafaring;
import com.runecraft.osrs.ui.SkillPanel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sailing (November 2025) and the flatpack Construction furniture checked against the OSRS Wiki. */
class SailingFidelityTest {

    @Test
    void sailingIsATwentyFourthSkill() {
        assertEquals(24, Skill.count());
        assertEquals(Skill.SAILING, Skill.byId("sailing"));
        assertTrue(!Skill.SAILING.isCombat());
        assertEquals(0, Skill.SAILING.startingXpTenths());
        assertEquals(13_034_431, Experience.xpForLevel(99));
    }

    @Test
    void boatsMatchTheWiki() {
        assertEquals(List.of(1, 15, 50), SailingData.BOATS.stream().map(SailingData.Boat::level).toList());
        assertEquals(List.of(1, 15, 50), Seafaring.BOATS.stream().map(Seafaring.BoatTier::level).toList());
        assertEquals(List.of(1_000, 15_000, 200_000), Seafaring.BOATS.stream().map(Seafaring.BoatTier::price).toList());
        assertEquals("pandemonium", SailingData.SAILING_QUEST);
        assertEquals(SailingData.SAILING_QUEST, Seafaring.SAILING_QUEST);
    }

    @Test
    void sailingNeedsPandemonium() {
        SailingData.Boat raft = SailingData.boat("raft_kit");
        SailingData.Boat sloop = SailingData.boat("sloop_kit");
        assertNotNull(SailingData.canSail(false, 99, raft));
        assertNull(SailingData.canSail(true, 1, raft));
        assertNotNull(SailingData.canSail(true, 49, sloop));
        assertNull(SailingData.canSail(true, 50, sloop));
    }

    @Test
    void shipwrecksHaveWikiLevelsAndXp() {
        Object[][] expected = {
                {"small_shipwreck", 15, 10.0, 5.5, "Small salvage"},
                {"fishermans_shipwreck", 26, 17.0, 9.0, "Fishy salvage"},
                {"barracuda_shipwreck", 35, 31.0, 15.5, "Barracuda salvage"},
                {"large_shipwreck", 53, 48.0, 24.0, "Large salvage"},
                {"pirate_shipwreck", 64, 76.0, 31.5, "Plundered salvage"},
                {"mercenary_shipwreck", 73, 138.0, 63.5, "Martial salvage"},
                {"fremennik_shipwreck", 80, 162.0, 75.0, "Fremennik salvage"},
                {"merchant_shipwreck", 87, 200.0, 95.0, "Opulent salvage"}};
        assertEquals(expected.length, SailingData.WRECKS.size());
        for (Object[] e : expected) {
            SailingData.Wreck w = SailingData.wreck((String) e[0]);
            assertNotNull(w, (String) e[0]);
            assertEquals(e[1], w.level(), w.blockId());
            assertEquals((Double) e[2], w.xp(), 1e-9, w.blockId());
            assertEquals((Double) e[3], w.sortXp(), 1e-9, w.blockId());
            assertEquals(e[4], w.salvageName());
            assertEquals(e[4], ItemCatalog.get(w.salvage()).name());
            assertEquals(w, SailingData.wreckBySalvage(w.salvage()));
            assertEquals((Integer) e[1], Seafaring.FEATURES.get(w.name() + " salvaging"), w.name());
        }
    }

    @Test
    void portsUseWikiDockingLevelsInBothSailingSystems() {
        Map<String, Integer> docking = Map.ofEntries(Map.entry("port_sarim", 1), Map.entry("pandemonium", 1),
                Map.entry("lands_end", 5), Map.entry("musa_point", 10), Map.entry("port_piscarilius", 15), Map.entry("catherby", 20),
                Map.entry("brimhaven", 25), Map.entry("port_khazard", 30), Map.entry("corsair_cove", 40), Map.entry("sunset_coast", 44),
                Map.entry("aldarin", 46), Map.entry("rellekka", 62), Map.entry("etceteria", 65));
        docking.forEach((id, level) -> {
            Seafaring.Port p = Seafaring.port(id);
            assertNotNull(p, id);
            assertEquals(level, p.level(), id);
            SailingData.Port board = SailingData.port(id);
            if (board != null) {
                assertEquals(level, board.level(), id + " task board");
            }
        });
        // Every task-board port also has a charter dock with the same level.
        for (SailingData.Port p : SailingData.PORTS) {
            Seafaring.Port dock = Seafaring.port(p.id());
            assertNotNull(dock, p.id());
            assertEquals(dock.level(), p.level(), p.id());
            assertTrue(Math.abs(dock.x() - p.x()) <= 20 && Math.abs(dock.y() - p.y()) <= 20, p.id() + " is in the same place");
        }
    }

    @Test
    void trawledFishUseWikiFishingLevels() {
        Map<String, Integer> levels = Map.of("giant_krill", 69, "haddock", 73, "yellowfin", 79, "halibut", 83, "bluefin", 87,
                "marlin", 91);
        int found = 0;
        for (CookingData.NewFish f : CookingData.FISH) {
            if (levels.containsKey(f.id())) {
                assertEquals(levels.get(f.id()), f.fishLevel(), f.id());
                found++;
            }
        }
        assertEquals(levels.size(), found);
    }

    @Test
    void skillPanelListsSailing() {
        List<Skill> layout = SkillPanel.layout();
        assertEquals(Skill.SAILING, layout.get(layout.size() - 1), "Sailing is the last skill in the tab, after Hunter");
        assertEquals(2, SkillPanel.column(Skill.SAILING));
    }

    @Test
    void flatpackFurnitureMatchesTheWiki() {
        assertEquals(200, ConstructionData.byFlatpack("marble_altar_flatpack").power());
        assertEquals(250, ConstructionData.byFlatpack("gilded_altar_flatpack").power());
        assertEquals(2, ConstructionData.byFlatpack("mahogany_altar_flatpack").materials().get("bolt_of_cloth"));
        String[] pools = {"restoration_pool", "revitalisation_pool", "rejuvenation_pool", "fancy_rejuvenation_pool",
                "ornate_rejuvenation_pool"};
        double[] xp = {706, 850, 900, 1950, 3107};
        int[] level = {65, 70, 80, 85, 90};
        for (int i = 0; i < pools.length; i++) {
            ConstructionData.Furniture f = ConstructionData.byFlatpack(pools[i] + "_flatpack");
            assertEquals(level[i], f.level(), pools[i]);
            assertEquals(xp[i], f.xp(), 1e-9, pools[i]);
            assertEquals(i + 1, f.power(), pools[i]);
            if (i > 0) {
                assertEquals(1, f.materials().get(pools[i - 1] + "_flatpack"), pools[i] + " upgrades the previous pool");
            }
            String id = "flatpack_" + pools[i];
            Recipe r = Recipes.get(id);
            assertNotNull(r, id);
            r.inputs().keySet().forEach(item -> assertTrue(ItemCatalog.exists(item), id + " " + item));
        }
    }
}
