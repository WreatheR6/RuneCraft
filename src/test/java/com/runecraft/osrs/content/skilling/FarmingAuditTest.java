package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.FarmingDisease;
import com.runecraft.osrs.FarmingYield;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.data.Herb;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OSRS wiki checks of Farming: growth cycles, harvest lives, flowers, payments, patches and contracts. */
class FarmingAuditTest {

    @Test
    void growthCyclesMatchTheWiki() {
        // Trees grow in 40-minute cycles: oak 4, willow 6, maple 8, yew 10, magic 12.
        assertEquals(160, FarmingData.byId("oak").growthMinutes());
        assertEquals(240, FarmingData.byId("willow").growthMinutes());
        assertEquals(320, FarmingData.byId("maple").growthMinutes());
        assertEquals(400, FarmingData.byId("yew").growthMinutes());
        assertEquals(6, FarmingData.stages(FarmingData.byId("willow")));
        // Spirit tree: 12 x 320 minutes.
        assertEquals(12, FarmingData.stages(FarmingData.byId("spirit")));
        // Fruit trees regrow a fruit every 45 minutes, potato cacti every 5.
        assertEquals(45, FarmingData.byId("apple").regrowMinutes());
        assertEquals(5, FarmingData.byId("potato_cactus").regrowMinutes());
        assertEquals(20, FarmingData.byId("redberry").regrowMinutes());
        // Allotments: 10-minute cycles, 4 to 8 of them.
        assertEquals(40, Allotment.POTATO.growthMinutes());
        assertEquals(60, Allotment.SWEETCORN.growthMinutes());
        assertEquals(60, Allotment.STRAWBERRY.growthMinutes());
        assertEquals(80, Allotment.WATERMELON.growthMinutes());
        assertEquals(70, Allotment.SNAPE_GRASS.growthMinutes());
        // 80 minutes over 3 visible steps at one random tick per ~68 s.
        assertEquals(23, FarmingYield.randomTickOdds(80, 3));
        assertEquals(12, FarmingYield.randomTickOdds(40, 3));
    }

    @Test
    void harvestLivesFormula() {
        assertEquals(3, FarmingYield.lives(0));
        assertEquals(6, FarmingYield.lives(3));
        // Wiki: guam 10.2% at 1 -> 31.6% at 99; ranarr 15.6%; torstol 28.1%.
        assertEquals(26, FarmingYield.saveChance(Herb.GUAM.harvestLow(), Herb.GUAM.harvestHigh(), 1, 0, 0));
        assertEquals(81, FarmingYield.saveChance(Herb.GUAM.harvestLow(), Herb.GUAM.harvestHigh(), 99, 0, 0));
        assertEquals(40, FarmingYield.saveChance(Herb.RANARR.harvestLow(), Herb.RANARR.harvestHigh(), 1, 0, 0));
        assertEquals(72, FarmingYield.saveChance(Herb.TORSTOL.harvestLow(), Herb.TORSTOL.harvestHigh(), 1, 0, 0));
        // Potato 39.8% -> 70.7%, snape grass 58.2% -> 76.6%, watermelon 49.6% at level 1.
        assertEquals(102, FarmingYield.saveChance(Allotment.POTATO.harvestLow(), Allotment.POTATO.harvestHigh(), 1, 0, 0));
        assertEquals(181, FarmingYield.saveChance(Allotment.POTATO.harvestLow(), Allotment.POTATO.harvestHigh(), 99, 0, 0));
        assertEquals(196, FarmingYield.saveChance(Allotment.SNAPE_GRASS.harvestLow(), Allotment.SNAPE_GRASS.harvestHigh(), 99, 0, 0));
        assertEquals(127, FarmingYield.saveChance(Allotment.WATERMELON.harvestLow(), Allotment.WATERMELON.harvestHigh(), 1, 0, 0));
        // Magic secateurs + Farming cape: 81 * 1.15 = 93.
        assertEquals(93, FarmingYield.saveChance(25, 80, 99, FarmingYield.MAGIC_SECATEURS + FarmingYield.FARMING_CAPE, 0));
        // A harvest never saves when the roll always fails; always 3 lives -> 3 herbs.
        assertEquals(3, FarmingYield.harvest(3, 81, n -> 255));
        // Saved twice then lost every life.
        int[] rolls = {0, 0, 255, 255, 255};
        int[] i = {0};
        assertEquals(5, FarmingYield.harvest(3, 81, n -> rolls[i[0]++]));
        // Average ultracompost torstol at 99 with secateurs + cape: about 6 / (1 - 93/256) = 9.4.
        assertEquals(9.4, FarmingYield.expected(6, 93), 0.05);
        Random r = new Random(7);
        double total = 0;
        for (int k = 0; k < 20000; k++) {
            total += FarmingYield.harvest(6, 93, r::nextInt);
        }
        assertEquals(9.4, total / 20000, 0.15);
    }

    @Test
    void flowers() {
        FarmingData.Crop marigold = FarmingData.byId("marigold");
        assertEquals(FarmingData.Patch.FLOWER, marigold.patch());
        assertEquals(2, marigold.level());
        assertEquals(8.5, marigold.plantXp());
        assertEquals(47.0, marigold.harvestXp());
        assertEquals(20, marigold.growthMinutes());
        assertEquals(4, FarmingData.stages(marigold));
        assertEquals(58, FarmingData.byId("white_lily").level());
        assertEquals(250.0, FarmingData.byId("white_lily").harvestXp());
        assertEquals(26, FarmingData.byId("limpwurt").level());
        assertEquals("limpwurt_root", FarmingData.byId("limpwurt").produce());
        // Woad gives 3 leaves; limpwurt 3 plus up to floor((level - 1) / 10) more (12 at 91+).
        assertEquals(3, FarmingData.harvestYield(FarmingData.byId("woad"), 99, 3, n -> n - 1));
        assertEquals(12, FarmingData.harvestYield(FarmingData.byId("limpwurt"), 99, 0, n -> n - 1));
        assertEquals(5, FarmingData.harvestYield(FarmingData.byId("limpwurt"), 26, 0, n -> n - 1));
        assertEquals(1, FarmingData.harvestYield(marigold, 99, 3, n -> n - 1), "compost doesn't add flowers");
        // Flowers pay harvest XP once per harvest.
        assertEquals(120.0, FarmingData.harvestXp(FarmingData.byId("limpwurt"), 9));
        assertEquals(26.0 * 4, FarmingData.harvestXp(FarmingData.byId("wildblood"), 4));
        for (String id : new String[]{"marigolds", "rosemary", "nasturtiums", "woad_leaf", "white_lily", "marigold_seed",
                "limpwurt_seed", "white_lily_seed", "seed_pack"}) {
            assertNotNull(ItemCatalog.get(id), id);
        }
    }

    @Test
    void protectionPayments() {
        assertEquals(new FarmingDisease.Payment("marigolds", 1), FarmingDisease.payment("hammerstone"));
        assertEquals(new FarmingDisease.Payment("nasturtiums", 1), FarmingDisease.payment("wildblood"));
        assertNull(FarmingDisease.payment("marigold"), "flowers can't be protected");
        assertEquals(0, FarmingData.diseaseChance(FarmingData.byId("grape"), 0));
    }

    @Test
    void diseaseFreePatches() {
        assertTrue(FarmingDisease.diseaseFreeArea(2828, 3687), "Troll Stronghold");
        assertTrue(FarmingDisease.diseaseFreeArea(2847, 3931), "Weiss");
        assertFalse(FarmingDisease.diseaseFreeArea(3053, 3307), "Falador");
        assertFalse(FarmingDisease.diseaseFreeArea(2813, 3463), "Catherby");
    }

    @Test
    void patchLocations() {
        PlaceDef stronghold = place("gnome_stronghold_patches");
        assertFalse(stronghold.blocks().contains("spirit_tree_patch"), "the Gnome Stronghold has no spirit tree patch");
        assertTrue(place("port_sarim_spirit_patch").blocks().contains("spirit_tree_patch"));
        assertEquals(3059, place("port_sarim_spirit_patch").osrsX());
        assertTrue(place("hosidius_spirit_patch").blocks().contains("spirit_tree_patch"));
        assertTrue(place("farming_guild").stations().contains(com.runecraft.osrs.Station.HERB_PATCH));
        assertTrue(place("farming_guild").blocks().contains("flower_patch"));
        assertTrue(place("ardougne_farm").blocks().contains("flower_patch"));
        assertTrue(place("morytania_farm").stations().contains(com.runecraft.osrs.Station.ALLOTMENT_PATCH));
        assertEquals(2847, place("weiss_herb_patch").osrsX());
        for (FarmingData.Gardener g : FarmingData.GARDENERS) {
            assertNotNull(place(g.place()), g.id());
        }
        Set<String> ids = new HashSet<>();
        for (NpcDef n : new FarmingData().npcs()) {
            assertTrue(ids.add(n.id()), n.id());
        }
        for (FarmingData.Leprechaun l : FarmingData.LEPRECHAUNS) {
            assertNotNull(place(l.place()), l.id());
        }
    }

    @Test
    void leprechaunNotesProduceOnly() {
        assertTrue(FarmingData.leprechaunNotes("grimy_ranarr_weed"));
        assertTrue(FarmingData.leprechaunNotes("potato"));
        assertTrue(FarmingData.leprechaunNotes("limpwurt_root"));
        assertTrue(FarmingData.leprechaunNotes("cooking_apple"));
        assertFalse(FarmingData.leprechaunNotes("ranarr_seed"));
        assertFalse(FarmingData.leprechaunNotes("magic_sapling"));
        assertFalse(FarmingData.leprechaunNotes("spade"));
    }

    @Test
    void contracts() {
        assertNull(FarmingContracts.bestTier(44));
        assertEquals(FarmingContracts.Tier.EASY, FarmingContracts.bestTier(45));
        assertEquals(FarmingContracts.Tier.MEDIUM, FarmingContracts.bestTier(65));
        assertEquals(FarmingContracts.Tier.HARD, FarmingContracts.bestTier(99));
        assertEquals(FarmingContracts.Tier.MEDIUM, FarmingContracts.byCrop("watermelon").tier());
        assertEquals(FarmingContracts.Tier.EASY, FarmingContracts.byCrop("ranarr_weed").tier());
        assertEquals(FarmingContracts.Tier.HARD, FarmingContracts.byCrop("magic", FarmingContracts.Tier.HARD).tier());
        assertEquals(FarmingContracts.Tier.MEDIUM, FarmingContracts.byCrop("potato_cactus").tier());
        for (FarmingContracts.Contract c : FarmingContracts.options(FarmingContracts.Tier.HARD, 85)) {
            assertTrue(c.level() <= 85, c.cropId());
        }
        assertFalse(FarmingContracts.options(FarmingContracts.Tier.HARD, 85).contains(FarmingContracts.byCrop("redwood")));
        for (FarmingContracts.Contract c : FarmingContracts.CONTRACTS) {
            int t = FarmingContracts.packTier(c);
            assertTrue(t >= c.tier().ordinal() + 1 && t <= c.tier().ordinal() + 3, c.cropId() + " " + t);
        }
        assertEquals(1, FarmingContracts.packTier(FarmingContracts.byCrop("potato")));
        assertEquals(5, FarmingContracts.packTier(FarmingContracts.byCrop("redwood")));
        for (var table : FarmingContracts.tables()) {
            for (Loot l : table) {
                assertNotNull(ItemCatalog.get(l.item()), l.item());
                assertTrue(l.min() >= 1 && l.max() >= l.min() && l.weight() > 0, l.item());
            }
        }
        assertEquals(10, FarmingContracts.rolls(5, n -> 0).length);
    }

    private static PlaceDef place(String id) {
        for (PlaceDef p : ContentRegistry.places()) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        for (com.runecraft.osrs.Landmark l : com.runecraft.osrs.Landmark.values()) {
            if (l.name().equals(id)) {
                return PlaceDef.town(id, l.displayName(), 0, 0);
            }
        }
        return null;
    }
}
