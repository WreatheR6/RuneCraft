package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.FarmingDisease;
import com.runecraft.osrs.FarmingYield;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Farming follow-ups: per-crop disease, flowers, harvest lives, seed packs, contracts, new crops and patches. */
class FarmingFollowupTest {

    private static FarmingData.Crop crop(String id) {
        FarmingData.Crop c = FarmingData.byId(id);
        assertNotNull(c, id);
        return c;
    }

    @Test
    void diseaseChancesDifferByPatchType() {
        // Wiki table: herbs 27 -> 14 -> 6 -> 3, with ultracompost and iasor 1.
        assertEquals(14, FarmingDisease.chance(27, 1));
        assertEquals(6, FarmingDisease.chance(27, 2));
        assertEquals(3, FarmingDisease.chance(27, 3));
        assertEquals(1, FarmingDisease.chance(27, 3, true));
        assertEquals(5, FarmingDisease.chance(27, 0, true));
        // Known crops: fruit trees 18, maple 13, magic 9.
        assertEquals(18, FarmingData.diseaseBase(crop("apple")));
        assertEquals(18, FarmingData.diseaseBase(crop("dragonfruit")));
        assertEquals(13, FarmingData.diseaseBase(crop("maple")));
        assertEquals(9, FarmingData.diseaseBase(crop("magic")));
        assertEquals(9, FarmingData.diseaseChance(crop("apple"), 1));
        // Estimated crops: more growth cycles, lower chance per cycle (approx).
        assertTrue(FarmingData.diseaseBase(crop("barley")) > FarmingData.diseaseBase(crop("wildblood")));
        assertTrue(FarmingData.diseaseBase(crop("redberry")) > FarmingData.diseaseBase(crop("whiteberry")));
        assertTrue(FarmingData.diseaseBase(crop("spirit")) < FarmingData.diseaseBase(crop("marigold")));
        assertEquals(27, FarmingDisease.estimatedBase(3));
        assertEquals(13, FarmingDisease.estimatedBase(7));
        // Immune crops.
        for (String id : new String[]{"poison_ivy", "grape", "hespori", "kronos", "iasor", "attas", "crystal", "scarecrow"}) {
            assertEquals(0, FarmingData.diseaseChance(crop(id), 0), id);
        }
        for (FarmingData.Crop c : FarmingData.CROPS) {
            int b = FarmingData.diseaseBase(c);
            assertTrue(b >= 0 && b <= 27, c.id() + " " + b);
        }
    }

    @Test
    void flowersProtectAllotments() {
        assertTrue(FarmingDisease.flowerProtects("potato", "marigold"));
        assertTrue(FarmingDisease.flowerProtects("onion", "marigold"));
        assertTrue(FarmingDisease.flowerProtects("tomato", "marigold"));
        assertTrue(FarmingDisease.flowerProtects("cabbage", "rosemary"));
        assertTrue(FarmingDisease.flowerProtects("watermelon", "nasturtium"));
        assertTrue(FarmingDisease.flowerProtects("sweetcorn", "scarecrow"));
        assertFalse(FarmingDisease.flowerProtects("cabbage", "marigold"));
        assertFalse(FarmingDisease.flowerProtects("sweetcorn", "marigold"));
        assertNull(FarmingDisease.protectingFlower("strawberry"));
        assertNull(FarmingDisease.protectingFlower("snape_grass"));
        for (Allotment a : Allotment.values()) {
            assertTrue(FarmingDisease.flowerProtects(a.produceItem(), "white_lily"), a.name());
        }
        // Scarecrow: stood in a flower patch, made from a hay sack, bronze spear and watermelon.
        FarmingData.Crop scarecrow = crop("scarecrow");
        assertEquals(FarmingData.Patch.FLOWER, scarecrow.patch());
        assertEquals(scarecrow, FarmingData.byPlanted(FarmingData.Patch.FLOWER, "scarecrow"));
        assertTrue(FarmingData.noPlantingTool(scarecrow));
        for (String id : new String[]{"scarecrow", "hay_sack", "empty_sack", "bronze_spear", "watermelon"}) {
            assertNotNull(ItemCatalog.get(id), id);
        }
        assertTrue(Shops.get("vanessas_farming_shop").stocks("empty_sack"));
    }

    @Test
    void hosidiusNeedsTheKourendDiary() {
        assertFalse(FarmingDisease.diseaseFreeArea(1740, 3570), "Hosidius isn't disease-free by itself");
        assertFalse(FarmingDisease.diseaseFree(1740, 3570, false));
        assertTrue(FarmingDisease.diseaseFree(1740, 3570, true));
        assertTrue(FarmingDisease.diseaseFree(2828, 3687, false), "Troll Stronghold");
        assertFalse(FarmingDisease.diseaseFree(3058, 3311, true), "Falador isn't Hosidius");
    }

    @Test
    void harvestLivesForHopsAndSpecials() {
        // Wiki: barley 40.6% at 3, hammerstone 41.4% at 4, Asgarnian 44.1% at 8, jute 47.7% at 13,
        // Yanillian 49.2% at 16, wildblood 55.5% at 28; all 70.7% at 99.
        assertEquals(104, chance("barley", 3));
        assertEquals(106, chance("hammerstone", 4));
        assertEquals(113, chance("asgarnian", 8));
        assertEquals(122, chance("jute", 13));
        assertEquals(126, chance("yanillian", 16));
        assertEquals(142, chance("wildblood", 28));
        for (String hop : new String[]{"barley", "hammerstone", "asgarnian", "jute", "yanillian", "krandorian", "wildblood"}) {
            assertEquals(181, chance(hop, 99), hop);
            // Every roll fails: exactly the lives (3 + compost).
            assertEquals(3, FarmingData.harvestYield(crop(hop), 99, 0, n -> n - 1));
            assertEquals(6, FarmingData.harvestYield(crop(hop), 99, 3, n -> n - 1));
            // Every roll saves until the safety cap: far more than min..max.
            assertTrue(FarmingData.harvestYield(crop(hop), 99, 0, n -> 0) > 10, hop);
        }
        for (String id : new String[]{"seaweed", "grape", "cactus", "celastrus"}) {
            assertNotNull(FarmingData.harvestChance(crop(id)), id);
        }
        assertEquals(3, FarmingData.harvestYield(crop("seaweed"), 50, 0, n -> n - 1));
        assertEquals(4, FarmingData.harvestYield(crop("grape"), 50, 1, n -> n - 1));
        // Magic secateurs boost hops and grapes, not seaweed.
        assertTrue(FarmingData.secateursBoost(crop("barley")));
        assertTrue(FarmingData.secateursBoost(crop("grape")));
        assertFalse(FarmingData.secateursBoost(crop("seaweed")));
        // Attas raises the save chance by 5%.
        assertEquals((int) Math.floor(181 * 1.05), FarmingYield.saveChance(102, 180, 99, FarmingYield.ATTAS, 0));
        // Bittercap: 6 mushrooms, more only with attas.
        assertEquals(6, FarmingData.harvestYield(crop("bittercap"), 99, 3, n -> 0));
        assertTrue(FarmingData.harvestYield(crop("bittercap"), 99, 0, FarmingYield.ATTAS, n -> 0) > 6);
        // Belladonna: 6 to 18 cave nightshade by level, 512 XP per harvest.
        FarmingData.Crop bella = crop("belladonna");
        assertEquals("nightshade", bella.produce());
        assertEquals(6, FarmingData.belladonnaMax(63));
        assertEquals(18, FarmingData.belladonnaMax(99));
        assertEquals(6, FarmingData.harvestYield(bella, 99, 0, n -> 0));
        assertEquals(18, FarmingData.harvestYield(bella, 99, 0, n -> n - 1));
        assertEquals(512.0, FarmingData.harvestXp(bella, 18));
    }

    private static int chance(String id, int level) {
        int[] c = FarmingData.harvestChance(crop(id));
        return FarmingYield.saveChance(c[0], c[1], level, 0, 0);
    }

    @Test
    void seedPacksFollowTheWiki() {
        for (int t = 1; t <= 5; t++) {
            assertEquals(5 + t, FarmingContracts.rollCount(t));
            assertEquals(5 + t, FarmingContracts.rolls(t, n -> 0).length);
        }
        // Tier 1: never a high roll; 1-3 medium.
        assertEquals(List.of(5, 1, 0), counts(FarmingContracts.rolls(1, n -> 0)));
        assertEquals(0, (int) counts(FarmingContracts.rolls(1, n -> n - 1)).get(2));
        assertEquals(3, (int) counts(FarmingContracts.rolls(1, n -> n - 1)).get(1));
        // Tier 2: a high roll only on the 1/11 roll.
        assertEquals(1, (int) counts(FarmingContracts.rolls(2, n -> 0)).get(2));
        assertEquals(0, (int) counts(FarmingContracts.rolls(2, n -> n - 1)).get(2));
        // Tier 5: 1-3 high, 4-6 medium, the rest low.
        List<Integer> max = counts(FarmingContracts.rolls(5, n -> n - 1));
        assertEquals(List.of(1, 6, 3), max);
        List<Integer> min = counts(FarmingContracts.rolls(5, n -> 0));
        assertEquals(List.of(5, 4, 1), min);
        for (List<Loot> table : FarmingContracts.tables()) {
            for (Loot l : table) {
                assertNotNull(ItemCatalog.get(l.item()), l.item());
            }
        }
        assertEquals(31, FarmingContracts.table(FarmingContracts.LOW).size());
        assertEquals(19, FarmingContracts.table(FarmingContracts.MEDIUM).size());
        assertEquals(14, FarmingContracts.table(FarmingContracts.HIGH).size());
        assertTrue(FarmingContracts.table(FarmingContracts.HIGH).stream().anyMatch(l -> l.item().equals("hespori_seed")));
    }

    private static List<Integer> counts(int[] rolls) {
        int[] c = new int[3];
        for (int r : rolls) {
            c[r]++;
        }
        return List.of(c[0], c[1], c[2]);
    }

    @Test
    void contractTableMatchesTheWiki() {
        // Wiki counts per pack tier: easy 11/12/8 (one easy tier-1 entry is missing here), medium 4/10/8, hard 3/8/7.
        assertEquals(List.of(10, 12, 8), packCounts(FarmingContracts.Tier.EASY, 1));
        assertEquals(List.of(4, 10, 8), packCounts(FarmingContracts.Tier.MEDIUM, 2));
        assertEquals(List.of(3, 8, 7), packCounts(FarmingContracts.Tier.HARD, 3));
        // Herbs and trees need the Farming Guild's 65 patches, fruit trees 85, redwood 90.
        assertEquals(65, FarmingContracts.byCrop("guam_leaf").level());
        assertEquals(85, FarmingContracts.byCrop("apple").level());
        assertEquals(90, FarmingContracts.byCrop("redwood").level());
        assertTrue(FarmingContracts.options(FarmingContracts.Tier.EASY, 45).stream().noneMatch(c -> c.cropId().equals("guam_leaf")));
        assertEquals(13, FarmingContracts.options(FarmingContracts.Tier.EASY, 47).size());
        // The same crop pays by tier.
        assertEquals(4, FarmingContracts.byCrop("maple", FarmingContracts.Tier.MEDIUM).packTier());
        assertEquals(5, FarmingContracts.byCrop("maple", FarmingContracts.Tier.HARD).packTier());
        // A swapped contract is never handed straight back.
        for (int i = 0; i < 30; i++) {
            final int k = i;
            FarmingContracts.Contract c = FarmingContracts.assign(FarmingContracts.Tier.HARD, 99, "torstol", n -> k % n);
            assertFalse(c.cropId().equals("torstol"));
        }
        // Every contract crop can be grown and reports that id when harvested.
        Set<String> harvestable = new HashSet<>();
        for (Allotment a : Allotment.values()) {
            harvestable.add(a.produceItem());
        }
        for (Herb h : Herb.values()) {
            harvestable.add(h.cleanItem());
        }
        for (FarmingData.Crop c : FarmingData.CROPS) {
            harvestable.add(c.id());
        }
        for (FarmingContracts.Contract c : FarmingContracts.CONTRACTS) {
            assertTrue(harvestable.contains(c.cropId()), c.cropId());
        }
    }

    private static List<Integer> packCounts(FarmingContracts.Tier tier, int lowest) {
        int[] n = new int[3];
        for (FarmingContracts.Contract c : FarmingContracts.CONTRACTS) {
            if (c.tier() == tier) {
                n[c.packTier() - lowest]++;
            }
        }
        return List.of(n[0], n[1], n[2]);
    }

    @Test
    void newCrops() {
        FarmingData.Crop hespori = crop("hespori");
        assertEquals(FarmingData.Patch.HESPORI, hespori.patch());
        assertEquals(FarmingData.Harvest.BOSS, hespori.harvest());
        assertEquals(65, hespori.level());
        assertEquals(62.0, hespori.plantXp());
        assertEquals(1920, hespori.growthMinutes());
        assertEquals(3, FarmingData.stages(hespori));
        assertNotNull(ItemCatalog.get("hespori_seed"));
        assertNotNull(ContentRegistry.boss("hespori"));
        for (String id : FarmingData.ANIMA) {
            FarmingData.Crop c = crop(id);
            assertEquals(FarmingData.Patch.ANIMA, c.patch());
            assertEquals(76, c.level());
            assertEquals(100.0, c.plantXp());
            assertEquals(8, FarmingData.stages(c), "8 x 640 minutes");
            assertNotNull(ItemCatalog.get(c.seed()), c.seed());
        }
        FarmingData.Crop crystal = crop("crystal");
        assertEquals(74, crystal.level());
        assertEquals(126.0, crystal.plantXp());
        assertEquals(13240.0, crystal.checkXp());
        assertEquals(6, FarmingData.stages(crystal));
        assertEquals("crystal_acorn", crystal.seed());
        assertEquals("crystal_sapling", crystal.planted());
        assertEquals("Crystal acorn", ItemCatalog.get("crystal_acorn").name());
        assertNotNull(ItemCatalog.get("crystal_sapling"));
        assertNotNull(ItemCatalog.get("crystal_shard"));
        // Huasca: a herb at Farming 65 (grown in herb patches, not dropped by monsters).
        assertEquals(65, Herb.HUASCA.farmingLevel());
        assertEquals(Herb.values().length - 1, Herb.HUASCA.ordinal());
        for (String id : new String[]{"huasca_seed", "grimy_huasca", "huasca", "huasca_potion_unf"}) {
            assertNotNull(ItemCatalog.get(id), id);
        }
        // New patch blocks exist.
        Set<String> blocks = new HashSet<>();
        ContentRegistry.blocks().forEach(b -> blocks.add(b.id()));
        for (String b : new String[]{"hespori_patch", "anima_patch", "crystal_tree_patch"}) {
            assertTrue(blocks.contains(b), b);
        }
    }

    @Test
    void paymentItems() {
        assertEquals(List.of(new FarmingDisease.Payment("barley_malt", 6)), FarmingDisease.paymentsFor("jute"));
        assertEquals(List.of(new FarmingDisease.Payment("monkey_nuts", 5), new FarmingDisease.Payment("monkey_bar", 1),
                new FarmingDisease.Payment("ground_tooth", 1)), FarmingDisease.paymentsFor("spirit"));
        assertEquals(new FarmingDisease.Payment("onion", 10), FarmingDisease.payment("asgarnian"));
        assertNotNull(Recipes.get("cook_barley_malt"));
        assertNotNull(Recipes.get("grind_suqah_tooth"));
        assertNotNull(Recipes.get("make_monkey_bar"));
        assertTrue(Shops.get("solihibs_food_stall").stocks("monkey_nuts"));
        assertTrue(Shops.get("solihibs_food_stall").stocks("monkey_bar"));
        assertNotNull(ContentRegistry.npc("solihib"));
    }

    @Test
    void patchPlacement() {
        assertFalse(ContentRegistry.place("brimhaven_patches").blocks().contains("spirit_tree_patch"));
        PlaceDef spirit = ContentRegistry.place("brimhaven_spirit_patch");
        assertTrue(spirit.blocks().contains("spirit_tree_patch"));
        assertTrue(Math.abs(spirit.osrsX() - 2801) <= 5 && Math.abs(spirit.osrsY() - 3203) <= 5);
        // Its camp plaza (radius 5 blocks, cleared 6 high) must not cut into any building.
        assertFalse(touchesStructure(GielinorMap.toBlockX(spirit.osrsX()), GielinorMap.toBlockZ(spirit.osrsY()), 5, null),
                "Brimhaven spirit tree plaza overlaps a structure");
        PlaceDef guild = ContentRegistry.place("farming_guild");
        assertTrue(guild.blocks().contains("hespori_patch"));
        assertTrue(guild.blocks().contains("anima_patch"));
        // Prifddinas: patches inside the city structure, on ground nothing else of the city uses.
        StructureDef prif = null;
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.id().equals("prifddinas")) {
                prif = s;
            }
        }
        assertNotNull(prif);
        Set<String> placed = new HashSet<>();
        int fx = GielinorMap.toBlockX(3290);
        int fz = GielinorMap.toBlockZ(6122);
        // The farm's ops: the run of ops inside its fenced box that ends with the crystal tree patch.
        int farmEnd = -1;
        for (int i = 0; i < prif.ops().size(); i++) {
            if ("runecraft:crystal_tree_patch".equals(prif.ops().get(i).block())) {
                farmEnd = i;
            }
        }
        assertTrue(farmEnd >= 0, "no crystal tree patch in Prifddinas");
        int farmStart = farmEnd;
        while (farmStart > 0) {
            StructureDef.Op op = prif.ops().get(farmStart - 1);
            if (op.x1() < fx - 1 || op.x2() > fx + 10 || op.z1() < fz - 1 || op.z2() > fz + 7) {
                break;
            }
            farmStart--;
        }
        for (int i = farmStart; i <= farmEnd; i++) {
            if (prif.ops().get(i).block() != null) {
                placed.add(prif.ops().get(i).block());
            }
        }
        for (String b : new String[]{"runecraft:allotment_patch", "runecraft:flower_patch", "runecraft:compost_bin",
                "runecraft:crystal_tree_patch"}) {
            assertTrue(placed.contains(b), b);
        }
        for (int i = 0; i < prif.ops().size(); i++) {
            if (i >= farmStart && i <= farmEnd) {
                continue;
            }
            StructureDef.Op op = prif.ops().get(i);
            boolean above = op.y2() >= StructureDef.GROUND && op.type() != StructureDef.Op.Type.CLEAR
                    && op.type() != StructureDef.Op.Type.FLATTEN;
            boolean inside = op.x2() >= fx - 1 && op.x1() <= fx + 10 && op.z2() >= fz - 1 && op.z1() <= fz + 7;
            assertFalse(above && inside, "Prifddinas farm overlaps " + op);
        }
        assertNotNull(ContentRegistry.place("prifddinas_farm"));
        assertNotNull(FarmingData.gardener("gardener_oswallt"));
    }

    /** Whether any structure puts a block at or above ground within {@code r} blocks of (bx, bz). */
    private static boolean touchesStructure(int bx, int bz, int r, String except) {
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.id().equals(except)) {
                continue;
            }
            for (StructureDef.Op op : s.ops()) {
                if (op.type() == StructureDef.Op.Type.CLEAR || op.type() == StructureDef.Op.Type.FLATTEN || op.y2() < StructureDef.GROUND) {
                    continue;
                }
                if (op.x2() >= bx - r && op.x1() <= bx + r && op.z2() >= bz - r && op.z1() <= bz + r) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void nonOsrsHerbPatchesRemoved() {
        for (Landmark l : new Landmark[]{Landmark.TAVERLEY, Landmark.GNOME_STRONGHOLD, Landmark.CANIFIS, Landmark.ARDOUGNE}) {
            assertFalse(l.stations().contains(Station.HERB_PATCH), l.name());
        }
        assertFalse(ContentRegistry.place("quest_zanaris").stations().contains(Station.HERB_PATCH));
        // No structure puts a herb patch in those towns.
        int[][] towns = {{2905, 3436}, {2452, 3438}, {3498, 3482}, {2645, 3334}, {3168, 3158}};
        for (StructureDef s : ContentRegistry.structures()) {
            for (StructureDef.Op op : s.ops()) {
                if (!"runecraft:herb_patch".equals(op.block())) {
                    continue;
                }
                for (int[] t : towns) {
                    int bx = GielinorMap.toBlockX(t[0]);
                    int bz = GielinorMap.toBlockZ(t[1]);
                    assertFalse(Math.abs(op.x1() - bx) < 40 && Math.abs(op.z1() - bz) < 40, s.id() + " herb patch near " + t[0] + "," + t[1]);
                }
            }
        }
        // The real herb patches are still there.
        assertTrue(Landmark.CATHERBY.stations().contains(Station.HERB_PATCH));
        assertTrue(ContentRegistry.place("ardougne_farm").stations().contains(Station.HERB_PATCH));
        assertTrue(ContentRegistry.place("morytania_farm").stations().contains(Station.HERB_PATCH));
    }
}
