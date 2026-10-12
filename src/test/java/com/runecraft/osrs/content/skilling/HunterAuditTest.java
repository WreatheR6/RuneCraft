package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.FishingMethod;
import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.HunterRates;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.Tree;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hunter creatures, catch rates, trap limits, gear and the gathering resource sites against OSRS. */
class HunterAuditTest {

    @Test
    void trapLimitFollowsOsrs() {
        assertEquals(1, HunterRates.trapLimit(1, false));
        assertEquals(1, HunterRates.trapLimit(19, false));
        assertEquals(2, HunterRates.trapLimit(20, false));
        assertEquals(3, HunterRates.trapLimit(40, false));
        assertEquals(4, HunterRates.trapLimit(60, false));
        assertEquals(5, HunterRates.trapLimit(80, false));
        assertEquals(5, HunterRates.trapLimit(99, false));
        assertEquals(6, HunterRates.trapLimit(99, true));
        assertEquals(2, HunterRates.trapLimit(1, true));
    }

    @Test
    void catchChancesMatchTheWikiCharts() {
        // Grey chinchompa: ~145/255 at 53, 268/255 (always) at 99.
        assertEquals(146 / 256.0, HunterRates.catchChance(HunterCreature.GREY_CHINCHOMPA, 53), 0.01);
        assertEquals(1.0, HunterRates.catchChance(HunterCreature.GREY_CHINCHOMPA, 99), 1e-9);
        // Black chinchompa: ~148/255 at 73, 228/255 at 99; carnivorous ~115/255 at 63.
        assertEquals(148 / 255.0, HunterRates.catchChance(HunterCreature.BLACK_CHINCHOMPA, 73), 0.01);
        assertEquals(229 / 256.0, HunterRates.catchChance(HunterCreature.BLACK_CHINCHOMPA, 99), 0.01);
        assertEquals(115 / 255.0, HunterRates.catchChance(HunterCreature.RED_CHINCHOMPA, 63), 0.01);
        // Salamanders: red 56% at 59 and 94% at 99; black 56% at 67, 83% at 99; orange 100% from 88.
        assertEquals(0.56, HunterRates.catchChance("Red salamander", 59, 59), 0.01);
        assertEquals(0.94, HunterRates.catchChance("Red salamander", 59, 99), 0.01);
        assertEquals(0.56, HunterRates.catchChance("Black salamander", 67, 67), 0.01);
        assertEquals(0.83, HunterRates.catchChance("Black salamander", 67, 99), 0.01);
        assertEquals(1.0, HunterRates.catchChance("Orange salamander", 47, 89), 1e-9);
        assertEquals(0.55, HunterRates.catchChance("Swamp lizard", 29, 29), 0.015);
        // Crimson swift: 39% at level 1.
        assertEquals(0.39, HunterRates.catchChance(HunterCreature.CRIMSON_SWIFT, 1), 0.01);
        // Below the requirement nothing is caught; the fallback curve starts at ~56%.
        assertEquals(0.0, HunterRates.catchChance("Prickly kebbit", 37, 36));
        assertEquals(143 / 256.0, HunterRates.catchChance("Prickly kebbit", 37, 37), 0.01);
        assertEquals(1.0, HunterRates.catchChance("Prickly kebbit", 37, 99), 1e-9);
    }

    @Test
    void catchChanceNeverDropsWithLevel() {
        List<Object[]> all = new java.util.ArrayList<>();
        for (HunterCreature c : HunterCreature.values()) {
            all.add(new Object[]{c.displayName(), c.level()});
        }
        for (HunterData.TrapSpot t : HunterData.TRAPS) {
            all.add(new Object[]{t.creature(), t.level()});
        }
        for (Object[] c : all) {
            double last = 0;
            for (int lvl = (Integer) c[1]; lvl <= 99; lvl++) {
                double p = HunterRates.catchChance((String) c[0], (Integer) c[1], lvl);
                assertTrue(p >= last && p > 0.3, c[0] + " at " + lvl + ": " + p);
                last = p;
            }
        }
    }

    @Test
    void creaturesLiveInTheirOsrsHuntingAreas() {
        assertEquals(Set.of(Region.KANDARIN), HunterCreature.FERRET.regions());
        assertTrue(HunterCreature.GOLDEN_WARBLER.regions().contains(Region.KHARIDIAN_DESERT));
        assertTrue(HunterCreature.CERULEAN_TWITCH.regions().contains(Region.FREMENNIK_PROVINCE));
        assertTrue(HunterCreature.BLACK_CHINCHOMPA.regions().contains(Region.WILDERNESS));
        assertFalse(HunterCreature.CRIMSON_SWIFT.regions().contains(Region.MISTHALIN));
        // Hunter areas are in the right kingdoms.
        for (com.runecraft.osrs.content.PlaceDef p : new HunterData().places()) {
            Region r = GielinorMap.regionAt(p.osrsX(), p.osrsY());
            String id = p.id();
            if (id.startsWith("piscatoris")) {
                assertEquals(Region.KANDARIN, r, id);
            } else if (id.startsWith("feldip")) {
                assertEquals(Region.FELDIP_HILLS, r, id);
            } else if (id.startsWith("rellekka")) {
                assertEquals(Region.FREMENNIK_PROVINCE, r, id);
            } else if (id.startsWith("uzer")) {
                assertEquals(Region.KHARIDIAN_DESERT, r, id);
            } else if (id.startsWith("black_salamander")) {
                assertEquals(Region.WILDERNESS, r, id);
            } else if (id.startsWith("swamp_lizard")) {
                assertEquals(Region.MORYTANIA, r, id);
            }
        }
    }

    @Test
    void newCreaturesHaveOsrsLevelsAndXp() {
        Object[][] expect = {{"ruby_harvest_bush", 15, 24.0}, {"sapphire_glacialis_bush", 25, 34.0}, {"snowy_knight_bush", 35, 44.0},
                {"black_warlock_bush", 45, 54.0}, {"spotted_kebbit_falconry", 43, 104.0}, {"dark_kebbit_falconry", 57, 132.0},
                {"dashing_kebbit_falconry", 69, 156.0}};
        for (Object[] e : expect) {
            HunterData.TrapSpot t = HunterData.trap((String) e[0]);
            assertNotNull(t, (String) e[0]);
            assertEquals(e[1], t.level(), (String) e[0]);
            assertEquals((Double) e[2], t.xp(), 1e-9, (String) e[0]);
            assertFalse(t.kind().laid());
        }
        assertEquals("butterfly_jar", HunterData.trap("black_warlock_bush").bait());
        assertTrue(HunterData.trap("spined_larupia_pitfall").kind().laid());
        assertEquals("tatty_kyatt_fur", HunterData.trap("sabre_toothed_kyatt_pitfall").rareLoot());
        HunterData.Impling crystal = HunterData.impling("crystal_impling");
        assertEquals(80, crystal.level());
        assertEquals(280.0, crystal.xp());
        assertEquals(216.0, HunterData.impling("magpie_impling").puroXp());
        assertEquals(20.0, HunterData.impling("baby_impling").puroXp());
        assertEquals(18.0, HunterData.impling("baby_impling").xp());
        assertEquals(380.0, HunterData.impling("lucky_impling").puroXp());
        // Every spot's tools, bait, loot and every impling's loot exist; every spot is placed somewhere.
        Set<String> placed = new HashSet<>();
        for (com.runecraft.osrs.content.PlaceDef p : new HunterData().places()) {
            placed.addAll(p.blocks());
        }
        for (HunterData.TrapSpot t : HunterData.TRAPS) {
            for (String i : t.tools()) {
                assertTrue(ItemCatalog.exists(i), i);
            }
            assertTrue(t.bait() == null || ItemCatalog.exists(t.bait()), t.bait());
            for (String i : t.loot()) {
                assertTrue(ItemCatalog.exists(i), i);
            }
            assertTrue(t.rareLoot() == null || ItemCatalog.exists(t.rareLoot()), t.rareLoot());
            assertTrue(placed.contains(t.blockId()), t.blockId() + " has no hunting area");
        }
        for (HunterData.Impling i : HunterData.IMPLINGS) {
            for (Loot l : i.loot()) {
                assertTrue(ItemCatalog.exists(l.item()), i.id() + ": " + l.item());
            }
        }
    }

    @Test
    void hunterGearIsSoldAtTheFancyDressShop() {
        for (String id : List.of("polar_camo_top", "wood_camo_legs", "jungle_camo_top", "desert_camo_legs", "larupia_hat", "larupia_top",
                "graahk_headdress", "graahk_legs", "kyatt_hat", "kyatt_top", "spotted_cape", "spottier_cape")) {
            assertTrue(ItemCatalog.exists(id), id);
            boolean sold = false;
            for (Recipe r : Recipes.all()) {
                if (r.outputs().containsKey(id)) {
                    sold = true;
                    for (String in : r.inputs().keySet()) {
                        assertTrue(ItemCatalog.exists(in), id + " needs " + in);
                    }
                }
            }
            assertTrue(sold, id);
        }
        assertEquals(52, ItemCatalog.get("kyatt_top").equipment().requirements().get(com.runecraft.osrs.Skill.HUNTER));
        assertEquals(19, ItemCatalog.get("larupia_top").equipment().stats().crushDefence());
    }

    @Test
    void fishingSpotsCatchTheRightFish() {
        assertEquals(List.of(Fish.MONKFISH), FishingSpot.NET_HARPOON.catches().get(FishingMethod.NET));
        assertTrue(FishingSpot.NET_HARPOON.catches().get(FishingMethod.HARPOON).contains(Fish.SWORDFISH));
        assertEquals(List.of(Fish.SHARK), FishingSpot.SHARK.catches().get(FishingMethod.HARPOON));
    }

    private static boolean resourceBlock(String id) {
        for (Tree t : Tree.values()) {
            if (t.blockId().equals(id)) {
                return true;
            }
        }
        for (Rock r : Rock.values()) {
            if (r.blockId().equals(id)) {
                return true;
            }
        }
        for (FishingSpot f : FishingSpot.values()) {
            if (f.blockId().equals(id)) {
                return true;
            }
        }
        return GatheringData.spot(id) != null;
    }

    @Test
    void resourceSitesAreInTheirOsrsKingdoms() {
        Set<String> ids = new HashSet<>();
        for (ResourceSites.Site s : ResourceSites.SITES) {
            assertTrue(ids.add(s.id()), "duplicate " + s.id());
            assertTrue(resourceBlock(s.block()), s.id() + ": " + s.block());
            assertFalse(s.tiles().isEmpty(), s.id());
            for (int[] t : s.tiles()) {
                assertEquals(s.region(), GielinorMap.regionAt(t[0], t[1]), s.id() + " at " + t[0] + "," + t[1]);
            }
            if (s.kind() == ResourceSites.Kind.FISHING) {
                assertTrue(s.block().endsWith("_fishing_spot"), s.id());
            } else if (s.kind() == ResourceSites.Kind.TREE) {
                assertTrue(s.block().endsWith("_tree_log"), s.id());
            } else {
                assertTrue(s.block().endsWith("_rock"), s.id());
            }
        }
        for (String must : List.of("catherby_cage_harpoon", "catherby_shark", "fishing_guild_shark", "musa_point_cage_harpoon",
                "shilo_village_river", "piscatoris_monkfish", "sorcerers_tower_magics", "gnome_stronghold_magic", "varrock_palace_yews",
                "falador_yews", "seers_maples", "al_kharid_iron", "lumbridge_swamp_west_coal")) {
            assertTrue(ids.contains(must), must);
        }
        // The important fishing spot types are placed somewhere now.
        Set<String> blocks = new HashSet<>();
        for (ResourceSites.Site s : ResourceSites.SITES) {
            blocks.add(s.block());
        }
        for (FishingSpot f : List.of(FishingSpot.CAGE_HARPOON, FishingSpot.NET_HARPOON, FishingSpot.SHARK, FishingSpot.ANGLER,
                FishingSpot.DARK_CRAB)) {
            assertTrue(blocks.contains(f.blockId()), f.name());
        }
        // Catherby's whole shore is Kandarin.
        assertEquals(Region.KANDARIN, GielinorMap.regionAt(2859, 3426));
        assertEquals(Region.ASGARNIA, GielinorMap.regionAt(2895, 3445));
    }
}
