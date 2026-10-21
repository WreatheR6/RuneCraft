package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.Elevation;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 30: lost schematic lockboxes, the eternal brazier's salts, lost crates and the salvage tables' skipped items. */
class SailingIslandsRound30Test {
    private static boolean land(int x, int y) {
        return !Elevation.isWater(StructureDef.bx(x), StructureDef.bz(y));
    }

    @Test
    void lockboxesKeysAndSchematics() {
        ContentRegistry.places();
        Set<String> ids = new HashSet<>();
        assertEquals(10, SailingLockboxes.LOCKBOXES.size());
        for (SailingLockboxes.Lockbox l : SailingLockboxes.LOCKBOXES) {
            assertTrue(ids.add(l.keyId()) && ids.add(l.schematic()), l.island());
            assertTrue(ItemCatalog.exists(l.keyId()), l.keyId());
            assertTrue(ItemCatalog.exists(l.schematic()), l.schematic());
            // Anglers' Retreat and the Tear of the Soul are not traced (round 40: floating lockbox, sand bar key spot).
            assertTrue(SailingLockboxes.UNTRACED.contains(l.island()) || land(l.x(), l.y()), l.island() + " lockbox at sea");
            assertTrue(SailingLockboxes.UNTRACED.contains(l.keyIsland()) || land(l.keyX(), l.keyY()), l.keyIsland() + " key spot at sea");
            assertEquals(l, SailingLockboxes.keyAt(l.keyX() + 1, l.keyY() - 1));
            assertEquals(l, SailingLockboxes.nearestLockbox(l.x(), l.y()));
            assertTrue(ContentRegistry.places().stream().map(PlaceDef::id).anyMatch(l.placeId()::equals), l.placeId());
            // Each schematic unlocks a real facility or the top tier of a boat part.
            if (l.partType() == null) {
                assertTrue(SailingCrew.allFacilities().stream().anyMatch(f -> f.id().equals(l.unlock())), l.unlock());
                assertEquals(l, SailingLockboxes.forFacility(l.unlock()));
            } else {
                SailingTraining.Part p = SailingTraining.part("sloop", l.partType(), l.partTier());
                assertNotNull(p, l.unlock());
                assertNull(SailingTraining.part("sloop", l.partType(), l.partTier() + 1), l.unlock() + " is not the top tier");
                assertTrue(p.name().contains("Rosewood") || p.name().contains("Dragon"), p.name());
                assertEquals(l, SailingLockboxes.forPart(l.partType(), l.partTier()));
            }
        }
        // Wiki pairs: the Chinchompa Island lockbox opens with the Pandemonium's key and holds the salvaging station schematic.
        SailingLockboxes.Lockbox chin = SailingLockboxes.byIsland("chinchompa_island");
        assertEquals("small_key_the_pandemonium", chin.keyId());
        assertEquals(42, chin.level());
        assertEquals("salvaging_station", chin.unlock());
        assertEquals("dragon_cannon", SailingLockboxes.byIsland("deepfin_point").unlock());
        assertEquals(87, SailingLockboxes.byIsland("grimstone").level());
        assertNull(SailingLockboxes.forPart(SailingTraining.PartType.HULL, 5));
        assertNull(SailingLockboxes.forFacility("bronze_cannon"));
        assertNull(SailingLockboxes.missingSchematic(chin, Set.of(chin.schematic())));
        assertNotNull(SailingLockboxes.missingSchematic(chin, Set.of()));
        assertNull(SailingLockboxes.missingSchematic(null, Set.of()));
    }

    @Test
    void eternalBrazierTakesTheWeissSalts() {
        SailingTraining.Facility brazier = SailingSea.FACILITIES.stream().filter(f -> f.id().equals("eternal_brazier")).findFirst()
                .orElseThrow();
        for (String salt : List.of("te_salt", "efh_salt", "urt_salt")) {
            assertEquals(250, brazier.materials().get(salt), salt);
            assertTrue(ItemCatalog.exists(salt), salt);
        }
        assertEquals(1373, brazier.conXp());
    }

    @Test
    void skippedSalvageItemsReallyAreMissing() {
        for (String name : SailingSea.SKIPPED) {
            assertFalse(ItemCatalog.exists(SailingSea.itemId(name)), name + " exists now: take it off SailingSea.SKIPPED");
        }
        // The Sailing repair kits and giant seaweed exist now, so the salvage tables hand them out.
        Set<String> loot = new HashSet<>();
        SailingSea.SALVAGE.values().forEach(lines -> lines.forEach(l -> loot.add(l.item())));
        assertTrue(loot.stream().anyMatch(i -> i.endsWith("_repair_kit")), "no repair kit in any salvage table");
    }

    @Test
    void lanternHarpooningForSquid() {
        GatheringData.Spot spot = GatheringData.spot(GatheringData.LANTERN_SPOT);
        assertNotNull(spot);
        assertEquals("harpoon", spot.tool());
        assertEquals(List.of("raw_jumbo_squid", "raw_swordtip_squid"), spot.catches().stream().map(GatheringData.Catch::item).toList());
        assertEquals(69, spot.catches().get(0).level());
        assertEquals(75.0, spot.catches().get(0).xp());
        assertEquals(52, spot.catches().get(1).level());
        assertEquals(55.0, spot.catches().get(1).xp());
        for (String id : List.of("raw_swordtip_squid", "swordtip_squid", "raw_jumbo_squid", "jumbo_squid", "bug_lantern")) {
            assertTrue(ItemCatalog.exists(id), id);
        }
        CookingData.NewFish swordtip = CookingData.FISH.stream().filter(f -> f.id().equals("swordtip_squid")).findFirst().orElseThrow();
        assertEquals(56, swordtip.cookLevel());
        assertEquals(150.0, swordtip.cookXp());
        assertEquals(15, swordtip.heal());
        assertEquals(91, swordtip.stopBurn());
        int tiles = 0;
        for (ResourceSites.Site s : ResourceSites.SITES) {
            if (s.block().equals(GatheringData.LANTERN_SPOT)) {
                tiles += s.tiles().size();
            }
        }
        assertEquals(18, tiles); // Onyx Crest 5, Laguna Aurorae 6, Wintumber Island 5, Buccaneers' Haven 2
        // About 0.3 squid per roll at 78 Fishing with a plain harpoon, 40-45% of them jumbo.
        double jumbo = spot.catches().get(0).chance(78, 1.0);
        double any = jumbo + (1 - jumbo) * spot.catches().get(1).chance(78, 1.0);
        assertTrue(any > 0.25 && any < 0.35, "per roll " + any);
    }

    @Test
    void lostCratesFollowTheWikiTiers() {
        assertEquals(-1, SailingSea.maxCrateTier(4));
        assertEquals(0, SailingSea.maxCrateTier(5));
        assertEquals(1, SailingSea.maxCrateTier(26));
        assertEquals(2, SailingSea.maxCrateTier(70));
        assertEquals(5, SailingSea.maxCrateTier(99));
        assertEquals(450, SailingSea.CRATE_XP[5]);
        Random r = new Random(3);
        Set<String> wooden = new HashSet<>();
        for (int i = 0; i < 400; i++) {
            assertEquals(0, SailingSea.lostCrateTier(10, r));
            SailingSea.Got g = SailingSea.lostCrateLoot(0, r);
            wooden.add(g.item() + ":" + g.count());
            int t = SailingSea.lostCrateTier(99, r);
            assertTrue(t >= 0 && t <= 5);
        }
        // All eight kinds of wooden-crate loot in their wiki amounts.
        assertEquals(8, wooden.size(), wooden.toString());
        assertTrue(wooden.contains("steel_arrow:40") && wooden.contains("mind_rune:80") && wooden.contains("sweetcorn_seed:6"), wooden.toString());
        SailingSea.lostCrateItems().forEach(i -> assertTrue(ItemCatalog.exists(i), i));
    }
}
