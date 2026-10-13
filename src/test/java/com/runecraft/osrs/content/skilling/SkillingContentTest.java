package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Tree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Data checks for the skilling module. */
class SkillingContentTest {
    private static List<Recipe> moduleRecipes() {
        List<Recipe> list = new ArrayList<>();
        new SkillingContent().recipes(list::add);
        return list;
    }

    private static void exists(String item, String context) {
        assertTrue(ItemCatalog.exists(item), context + " references unknown item " + item);
    }

    private static void loot(List<Loot> table, String context) {
        assertTrue(!table.isEmpty(), context + " has an empty table");
        for (Loot l : table) {
            exists(l.item(), context);
            assertTrue(l.min() >= 1 && l.max() >= l.min() && l.weight() > 0, context + " bad loot line " + l);
        }
    }

    private static Recipe recipe(String id) {
        Recipe r = Recipes.get(id);
        assertNotNull(r, "missing recipe " + id);
        return r;
    }

    private static boolean blockExists(String id) {
        return ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(id));
    }

    @Test
    void recipesAreSane() {
        Set<String> ids = new HashSet<>();
        for (Recipe r : moduleRecipes()) {
            assertTrue(ids.add(r.id()), "duplicate recipe " + r.id());
            assertTrue(Recipes.get(r.id()) != null, r.id() + " not registered");
            r.inputs().keySet().forEach(i -> exists(i, r.id()));
            r.outputs().keySet().forEach(i -> exists(i, r.id()));
            if (r.requiredTool() != null) {
                exists(r.requiredTool(), r.id() + " tool");
            }
            if (r.failItem() != null) {
                exists(r.failItem(), r.id() + " burnt item");
            }
            assertTrue(!r.outputs().isEmpty(), r.id() + " produces nothing");
            if (r.skill() != null) {
                assertTrue(r.level() >= 1 && r.level() <= 99, r.id() + " level " + r.level());
                double xp = r.xpTenths() / 10.0;
                assertTrue(xp >= 0 && xp <= (r.skill() == Skill.CONSTRUCTION ? 10000 : 2500), r.id() + " xp " + xp);
                if (r.stopBurn() > 0) {
                    assertTrue(r.stopBurn() >= r.level(), r.id() + " stops burning below its level");
                }
            } else if (r.inputs().containsKey("coins")) {
                assertTrue(r.ticks() == 0, r.id() + " purchases are instant");
            }
        }
        assertTrue(ids.size() > 400, "only " + ids.size() + " recipes");
    }

    @Test
    void faithfulSpotChecks() {
        Recipe sc = recipe("mix_super_combat_potion");
        assertEquals(90, sc.level());
        assertEquals(1500, sc.xpTenths());
        Recipe body = recipe("craft_black_dhide_body");
        assertEquals(84, body.level());
        assertEquals(2580, body.xpTenths());
        assertEquals(3, body.inputs().get("black_dragon_leather"));
        Recipe cannon = recipe("smith_cannonball");
        assertEquals(35, cannon.level());
        assertEquals(256, cannon.xpTenths());
        assertEquals(4, cannon.outputs().get("cannonball"));
        assertEquals(91, recipe("cook_manta_ray").level());
        assertEquals(2162, recipe("cook_manta_ray").xpTenths());
        assertEquals(98, recipe("craft_zenyte_amulet_u").level());
        assertEquals(66, recipe("craft_air_battlestaff").level());
        assertEquals(1375, recipe("craft_air_battlestaff").xpTenths());
        assertEquals(69, recipe("string_rune_crossbow").level());
        assertEquals(99, recipe("smith_rune_plateskirt").level());
        assertEquals(88, recipe("smith_rune_bolts_unf").level());
        assertEquals(75, recipe("flatpack_gilded_altar").level());
        assertEquals(22300, recipe("flatpack_gilded_altar").xpTenths());
        assertEquals(1, recipe("cook_lava_eel").stopBurn() == 0 ? 1 : 0, "lava eels never burn");
        assertEquals(43, recipe("craft_gold_bracelet").level() + 36);
    }

    @Test
    void lootTablesExist() {
        for (HunterData.Impling i : HunterData.IMPLINGS) {
            loot(i.loot(), i.id());
            exists(i.jar(), i.id());
            assertNotNull(ContentRegistry.monster(i.id()), i.id() + " monster");
        }
        for (HunterData.TrapSpot t : HunterData.TRAPS) {
            t.loot().forEach(l -> exists(l, t.blockId()));
            t.tools().forEach(l -> exists(l, t.blockId()));
            if (t.bait() != null) {
                exists(t.bait(), t.blockId());
            }
            assertTrue(blockExists(t.blockId()), t.blockId());
        }
        for (ThievingData.Target t : ThievingData.TARGETS) {
            if (!t.table().isEmpty()) {
                loot(t.table(), t.npcId());
            }
            t.always().forEach(l -> exists(l.item(), t.npcId()));
            assertNotNull(ContentRegistry.npc(t.npcId()), t.npcId());
        }
        for (ThievingData.Container c : ThievingData.CONTAINERS) {
            if (!c.table().isEmpty()) {
                loot(c.table(), c.blockId());
            }
            c.always().forEach(l -> exists(l.item(), c.blockId()));
            assertTrue(!c.table().isEmpty() || !c.always().isEmpty(), c.blockId());
        }
        loot(GatheringData.CASKET, "casket");
        loot(GatheringData.OYSTER, "oyster");
        loot(GatheringData.INFERNAL_EEL, "infernal eel");
        for (SailingData.Wreck w : SailingData.WRECKS) {
            loot(w.loot(), w.blockId());
            exists(w.salvage(), w.blockId());
        }
        for (int lvl : new int[]{30, 40, 55, 70, 85, 99}) {
            loot(MiningData.payDirtTable(lvl), "pay-dirt " + lvl);
        }
        assertEquals(5, MiningData.payDirtTable(99).size());
        for (int tier = 1; tier <= 3; tier++) {
            ConstructionData.larderItems(tier).forEach(i -> exists(i, "larder"));
        }
    }

    @Test
    void gatheringData() {
        for (GatheringData.Spot s : GatheringData.SPOTS) {
            exists(s.tool(), s.blockId());
            if (s.bait() != null) {
                exists(s.bait(), s.blockId());
            }
            for (GatheringData.Catch c : s.catches()) {
                exists(c.item(), s.blockId());
                assertTrue(c.level() >= 1 && c.level() <= 99 && c.xp() > 0 && c.high() >= c.low(), s.blockId() + " " + c.item());
            }
            if (!s.junk().isEmpty()) {
                loot(s.junk(), s.blockId());
            }
            assertTrue(blockExists(s.blockId()), s.blockId());
        }
        for (GatheringData.ModTree t : GatheringData.TREES) {
            exists(t.log(), t.blockId());
            assertTrue(blockExists(t.blockId()), t.blockId());
        }
        for (MiningData.MineRock r : MiningData.ROCKS) {
            r.yields().forEach(y -> exists(y.item(), r.blockId()));
            assertTrue(blockExists(r.blockId()), r.blockId());
        }
        for (String p : MiningData.PROSPECTOR) {
            assertNotNull(ItemCatalog.get(p).equipment(), p);
        }
        for (CookingData.NewFish f : CookingData.FISH) {
            exists(f.raw(), f.id());
            assertEquals(f.heal(), ItemCatalog.get(f.cooked()).heal(), f.id());
        }
        for (CookingData.Bite b : CookingData.BITES) {
            exists(b.full(), "bite");
            if (b.next() != null) {
                exists(b.next(), "bite");
            }
            assertEquals(ItemDef.Kind.FOOD, ItemCatalog.get(b.full()).kind(), b.full());
        }
    }

    @Test
    void farmingData() {
        Set<String> crops = new HashSet<>();
        for (FarmingData.Crop c : FarmingData.CROPS) {
            assertTrue(crops.add(c.id()), "duplicate crop " + c.id());
            exists(c.seed(), c.id());
            exists(c.planted(), c.id());
            if (c.harvest() != FarmingData.Harvest.SCARECROW) {
                assertEquals(ItemDef.Kind.SEED, ItemCatalog.get(c.seed()).kind(), c.seed());
            }
            if (c.harvest() == FarmingData.Harvest.CHOP) {
                assertNotNull(Tree.valueOf(c.produce()), c.id());
            } else if (c.produce() != null) {
                exists(c.produce(), c.id());
            }
            if (c.roots() != null) {
                exists(c.roots(), c.id());
            }
            assertTrue(c.level() >= 1 && c.level() <= 99 && c.growthMinutes() > 0, c.id());
            assertTrue(blockExists(c.patch().blockId), c.patch().blockId);
        }
        FarmingData.COMPOSTABLE.forEach(i -> exists(i, "compostable"));
        FarmingData.SUPER_COMPOSTABLE.forEach(i -> exists(i, "super compostable"));
        for (FarmingData.Compost c : FarmingData.Compost.values()) {
            exists(c.item, "compost");
        }
        for (CraftingData.BirdHouse b : CraftingData.BIRD_HOUSES) {
            exists(b.id(), "bird house");
            assertTrue(b.hunterXp() > 0, b.id());
        }
    }

    @Test
    void herbloreAndRunecraft() {
        Set<String> made = new HashSet<>();
        Recipes.all().forEach(r -> made.addAll(r.outputs().keySet()));
        for (HerbloreData.ModPotion p : HerbloreData.POTIONS) {
            assertEquals(ItemDef.Kind.POTION, ItemCatalog.get(p.id()).kind(), p.id());
            assertTrue(made.contains(p.id()), p.id() + " can't be made");
        }
        assertTrue(made.contains("antifire_potion") && made.contains("extended_antifire"));
        for (RunecraftData.Combination c : RunecraftData.COMBINATIONS) {
            exists(c.id(), c.id());
            exists(c.talisman(), c.id());
            exists(c.rune(), c.id());
        }
        for (com.runecraft.osrs.data.RuneAltar a : com.runecraft.osrs.data.RuneAltar.values()) {
            exists(RunecraftData.talisman(a), a.name());
            exists(RunecraftData.tiara(a), a.name());
            assertTrue(RunecraftData.tiaraXp(a) > 0);
        }
        for (RunecraftData.Pouch p : RunecraftData.POUCHES) {
            assertEquals(1, ItemCatalog.get(p.id()).maxStack(), p.id());
        }
        for (CraftingData.Jewel j : CraftingData.JEWELLERY) {
            String product = j.type().equals("amulet") ? j.id() + "_u" : j.id();
            assertTrue(made.contains(product), product);
            if (j.type().equals("amulet")) {
                assertTrue(made.contains(j.id()), j.id() + " can't be strung");
            }
        }
    }

    @Test
    void blocksAndPlaces() {
        for (ConstructionData.Furniture f : ConstructionData.FURNITURE) {
            exists(f.flatpack(), f.id());
            assertTrue(blockExists(f.block()), f.block());
            f.materials().keySet().forEach(m -> exists(m, f.id()));
            if (f.use() == ConstructionData.Use.PORTAL) {
                assertNotNull(com.runecraft.osrs.Landmark.valueOf(f.destination()));
            }
        }
        SkillingContent module = new SkillingContent();
        for (PlaceDef p : module.places()) {
            for (String b : p.blocks()) {
                assertTrue(blockExists(b), p.id() + " block " + b);
            }
        }
        for (BlockDef b : module.blocks()) {
            assertTrue(b.id().matches("[a-z0-9_]+"), b.id());
            assertNotNull(b.look(), b.id());
        }
        for (SailingTraining.ChartTask c : SailingTraining.CHARTS) {
            if (!c.underground()) {
                assertNotNull(ContentRegistry.place(c.placeId()), c.placeId());
            }
        }
        for (SailingData.Port p : SailingData.PORTS) {
            assertTrue(blockExists(p.board()), p.board());
        }
        for (SailingData.Boat b : SailingData.BOATS) {
            exists(b.kit(), b.name());
        }
        for (String t : FletchingData.THROWN) {
            ItemDef def = ItemCatalog.get(t);
            assertEquals(ItemDef.Kind.ARROW, def.kind(), t);
            assertTrue(def.rangedStrength() > 0, t);
        }
    }

    @Test
    void lootPickCoversWeights() {
        List<Loot> t = List.of(Loot.of("a", 1), Loot.of("b", 3));
        assertEquals(4, Loot.totalWeight(t));
        assertEquals("a", Loot.pick(t, 0).item());
        assertEquals("b", Loot.pick(t, 1).item());
        assertEquals("b", Loot.pick(t, 3).item());
        assertEquals(5, Loot.of("x", 5, 9, 1).amount(0.0));
        assertEquals(9, Loot.of("x", 5, 9, 1).amount(0.999));
    }
}
