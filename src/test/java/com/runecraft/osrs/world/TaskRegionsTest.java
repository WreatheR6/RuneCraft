package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.slayer.SlayerBossTasks;
import com.runecraft.osrs.content.slayer.SlayerCategories;
import com.runecraft.osrs.content.slayer.SlayerMasters;
import com.runecraft.osrs.content.slayer.SlayerTasks;
import com.runecraft.osrs.data.Monster;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Konar quo Maten's location restrictions against the kingdom map and the spawn table. */
class TaskRegionsTest {
    /** Border wobble of {@link GielinorMap#regionAtBlock}, in tiles. */
    private static final int WOBBLE = 12;

    @Test
    void kingdomsFollowTheOsrsMap() {
        assertEquals(Region.KHARIDIAN_DESERT, GielinorMap.regionAt(3228, 3108));   // Kalphite Lair entrance
        assertEquals(Region.KHARIDIAN_DESERT, GielinorMap.regionAt(3490, 3090));   // Uzer
        assertEquals(Region.KHARIDIAN_DESERT, GielinorMap.regionAt(3300, 3305));   // Al Kharid mine
        assertEquals(Region.LUMBRIDGE_SWAMP, GielinorMap.regionAt(3169, 3172));    // swamp cave entrance
        assertEquals(Region.MISTHALIN, GielinorMap.regionAt(3109, 3162));          // Wizards' Tower
        assertEquals(Region.MISTHALIN, GielinorMap.regionAt(3081, 3421));          // Barbarian Village
        assertEquals(Region.ASGARNIA, GielinorMap.regionAt(2995, 3115));           // Mudskipper Point
        assertEquals(Region.ASGARNIA, GielinorMap.regionAt(3025, 3217));           // Port Sarim
        assertEquals(Region.ASGARNIA, GielinorMap.regionAt(2899, 3544));           // Burthorpe
        assertEquals(Region.ASGARNIA, GielinorMap.regionAt(2862, 3546));           // Warriors' Guild
        assertEquals(Region.ASGARNIA, GielinorMap.regionAt(2834, 3335));           // Entrana
        assertEquals(Region.ASGARNIA, GielinorMap.regionAt(2884, 3398));           // Taverley Dungeon entrance area
        assertEquals(Region.KARAMJA, GielinorMap.regionAt(2915, 3175));            // Musa Point
        assertEquals(Region.KARAMJA, GielinorMap.regionAt(2760, 3178));            // Brimhaven
        assertEquals(Region.KANDARIN, GielinorMap.regionAt(2810, 3440));           // Catherby
        assertEquals(Region.KANDARIN, GielinorMap.regionAt(2465, 3495));           // Gnome Stronghold
        assertEquals(Region.KANDARIN, GielinorMap.regionAt(2340, 3690));           // Piscatoris
        assertEquals(Region.KANDARIN, GielinorMap.regionAt(2440, 3090));           // Castle Wars
        assertEquals(Region.KANDARIN, GielinorMap.regionAt(2606, 3093));           // Yanille
        assertEquals(Region.TIRANNWN, GielinorMap.regionAt(2240, 3320));           // Prifddinas' valley
        assertEquals(Region.TIRANNWN, GielinorMap.regionAt(2330, 3170));           // Lletya
        assertEquals(Region.FELDIP_HILLS, GielinorMap.regionAt(2525, 2915));       // Feldip hunter area
        assertEquals(Region.FREMENNIK_PROVINCE, GielinorMap.regionAt(2660, 3680)); // Rellekka
        assertEquals(Region.FREMENNIK_PROVINCE, GielinorMap.regionAt(2509, 3635)); // the Lighthouse
        assertEquals(Region.FREMENNIK_PROVINCE, GielinorMap.regionAt(2530, 3748)); // Waterbirth Island
        assertEquals(Region.FREMENNIK_PROVINCE, GielinorMap.regionAt(2406, 3803)); // Jatizso
        assertEquals(Region.TROLL_COUNTRY, GielinorMap.regionAt(2916, 3746));      // God Wars Dungeon
        assertEquals(Region.MORYTANIA, GielinorMap.regionAt(3428, 3537));          // Slayer Tower
        assertEquals(Region.MORYTANIA, GielinorMap.regionAt(3680, 3480));          // Port Phasmatys
        // Konar's regions off the continent
        assertEquals(Region.KOUREND, GielinorMap.regionAt(1640, 3680));            // Kourend Castle
        assertEquals(Region.KOUREND, GielinorMap.regionAt(1311, 3807));            // Mount Karuulm
        assertEquals(Region.KOUREND, GielinorMap.regionAt(1290, 3610));            // Lizardman Settlement
        assertEquals(Region.KOUREND, GielinorMap.regionAt(1580, 3480));            // Kourend Woodland
        assertEquals(Region.VARLAMORE, GielinorMap.regionAt(1680, 3125));          // Civitas illa Fortis
        assertEquals(Region.VARLAMORE, GielinorMap.regionAt(1450, 3115));          // Ralos' Rise
        assertEquals(Region.FOSSIL_ISLAND, GielinorMap.regionAt(3740, 3800));      // Fossil Island
        assertEquals(Region.FOSSIL_ISLAND, GielinorMap.regionAt(3683, 3706));      // the Tar Swamp's south end
        assertEquals(Region.LITHKREN, GielinorMap.regionAt(3548, 3998));           // Lithkren
        assertEquals(Region.UNCHARTED_ISLES, GielinorMap.regionAt(2110, 3905));    // Lunar Isle
        assertEquals(Region.UNCHARTED_ISLES, GielinorMap.regionAt(3712, 2985));    // Mos Le'Harmless
    }

    @Test
    void dungeonsCountAsTheirOwnKingdom() {
        // The Fremennik Slayer Dungeon is carved beneath the Warriors' Guild.
        Dungeons.Camp fsd = Dungeons.camps().stream().filter(c -> c.dungeon().equals("dng_fremennik_slayer_dungeon"))
                .filter(c -> c.creatures().contains("kurask")).findFirst().orElseThrow();
        assertEquals(Region.FREMENNIK_PROVINCE, TaskRegions.at(fsd.centreX(), fsd.feetY(), fsd.centreZ()));
        assertEquals(Region.ASGARNIA, TaskRegions.at(fsd.centreX(), 70, fsd.centreZ()), "the guild above is Asgarnia");
        assertEquals(Region.LUMBRIDGE_SWAMP, TaskRegions.at(GielinorMap.toBlockX(2412), 64, GielinorMap.toBlockZ(4420)));
        for (Dungeons.Camp c : Dungeons.camps()) {
            Region r = TaskRegions.ofDungeon(c.dungeon());
            if (r != null) {
                assertEquals(r, TaskRegions.at(c.centreX(), c.feetY(), c.centreZ()), c.dungeon());
            }
        }
    }

    /**
     * Every region Konar can name for a task holds at least one spawn point or camp of a creature that
     * counts for it, placed so the border wobble can't move it into a neighbouring kingdom.
     */
    @Test
    void everyKonarRegionHoldsTheTaskCreatures() {
        List<String> missing = new ArrayList<>();
        for (SlayerMasters.Assignment a : SlayerMasters.get("konar").assignments()) {
            String category = a.category();
            if (category.equals(SlayerBossTasks.CATEGORY) || !SlayerTasks.available(category)) {
                continue;
            }
            Set<String> ids = new HashSet<>();
            for (MonsterDef m : SlayerTasks.creatures(category)) {
                ids.add(m.id);
            }
            for (String builtIn : SlayerCategories.get(category).builtIns()) {
                ids.add(Monster.valueOf(builtIn).entity());
            }
            List<Region> regions = SlayerTasks.regions(category);
            assertFalse(regions.isEmpty(), category);
            assertFalse(regions.contains(Region.WILDERNESS), category + ": Konar never assigns the Wilderness");
            for (Region region : regions) {
                if (!holds(region, ids)) {
                    missing.add(category + " in " + region);
                }
            }
        }
        assertTrue(missing.isEmpty(), "Konar regions without any of the task's spawns: " + missing);
    }

    private static boolean holds(Region region, Set<String> ids) {
        for (SpawnPoint p : SpawnTable.all()) {
            if (ids.contains(p.monster()) && firmlyIn(region, p.x(), p.y())) {
                return true;
            }
        }
        for (SpawnTable.CampRef c : SpawnTable.camps()) {
            if (!ids.contains(c.creature())) {
                continue;
            }
            if (TaskRegions.ofDungeon(c.source()) != null ? TaskRegions.ofCamp(c) == region
                    : TaskRegions.inZanaris(c.x(), c.y()) ? region == Region.LUMBRIDGE_SWAMP : firmlyIn(region, c.x(), c.y())) {
                return true;
            }
        }
        return false;
    }

    /** In the region wherever the wobble puts the borders. */
    private static boolean firmlyIn(Region region, int x, int y) {
        for (int dx = -WOBBLE; dx <= WOBBLE; dx += WOBBLE) {
            for (int dy = -WOBBLE; dy <= WOBBLE; dy += WOBBLE) {
                if (GielinorMap.regionAt(x + dx, y + dy) != region) {
                    return false;
                }
            }
        }
        return true;
    }
}
