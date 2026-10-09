package com.runecraft.osrs;

import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.Tree;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every item referenced by a recipe, drop, stall or trap must exist. */
class DataIntegrityTest {
    @Test
    void recipeItemsExist() {
        for (Recipe r : Recipes.all()) {
            r.inputs().keySet().forEach(i -> assertTrue(ItemCatalog.exists(i), r.id() + " input " + i));
            r.outputs().keySet().forEach(i -> assertTrue(ItemCatalog.exists(i), r.id() + " output " + i));
            if (r.requiredTool() != null) {
                assertTrue(ItemCatalog.exists(r.requiredTool()), r.id() + " tool");
            }
            if (r.failItem() != null) {
                assertTrue(ItemCatalog.exists(r.failItem()), r.id() + " fail");
            }
        }
    }

    @Test
    void dropsExist() {
        for (Monster m : Monster.values()) {
            for (Drop d : m.drops()) {
                assertTrue(ItemCatalog.exists(d.item()), m + " drops " + d.item());
            }
        }
        for (Thieving.Stall s : Thieving.Stall.values()) {
            s.loot().forEach(l -> assertTrue(ItemCatalog.exists(l.item()), s + " " + l.item()));
        }
        for (Thieving.Pickpocket p : Thieving.Pickpocket.values()) {
            p.loot().forEach(l -> assertTrue(ItemCatalog.exists(l.item()), p + " " + l.item()));
        }
        for (HunterCreature c : HunterCreature.values()) {
            c.loot().forEach(l -> assertTrue(ItemCatalog.exists(l), c + " " + l));
        }
        for (Tree t : Tree.values()) {
            assertNotNull(ItemCatalog.get(t.logItem()));
        }
    }

    @Test
    void uniqueIdsAndNames() {
        Set<String> ids = new HashSet<>();
        for (ItemDef d : ItemCatalog.all()) {
            assertTrue(ids.add(d.id()));
            assertTrue(d.id().matches("[a-z0-9_]+"), d.id());
        }
    }

    @Test
    void runecraftingMultiples() {
        assertEquals(1, RuneAltar.AIR.runesPerEssence(1));
        assertEquals(2, RuneAltar.AIR.runesPerEssence(11));
        assertEquals(10, RuneAltar.AIR.runesPerEssence(99));
        assertEquals(2, RuneAltar.NATURE.runesPerEssence(91));
    }

    @Test
    void mapPlacesKingdoms() {
        assertEquals(Region.MISTHALIN, GielinorMap.regionAt(3222, 3218));
        assertEquals(Region.WILDERNESS, GielinorMap.regionAt(3100, 3700));
        assertEquals(Region.ASGARNIA, GielinorMap.regionAt(2965, 3380));
        assertEquals(Region.KANDARIN, GielinorMap.regionAt(2757, 3477));
        assertEquals(Region.KHARIDIAN_DESERT, GielinorMap.regionAt(3293, 3174));
        assertEquals(Region.MORYTANIA, GielinorMap.regionAt(3496, 3488));
        assertEquals(Region.KARAMJA, GielinorMap.regionAt(2852, 2955));
        assertEquals(0, GielinorMap.toBlockX(3222));
        assertEquals(1, GielinorMap.wildernessLevel(0, GielinorMap.toBlockZ(3521)));
        assertEquals(Landmark.LUMBRIDGE.blockX(), 8);
    }

    @Test
    void cookingBurnRates() {
        Recipe shrimp = Recipes.get("cook_shrimps");
        assertEquals(1.0, shrimp.cookingSuccess(34, false), 1e-9);
        assertTrue(shrimp.cookingSuccess(1, false) < 0.5);
        Recipe shark = Recipes.get("cook_shark");
        assertTrue(shark.cookingSuccess(99, false) < 1.0);
    }

    @Test
    void gatherChanceFormula() {
        // Bronze axe on a normal tree at level 1: (1 + 64) / 256
        assertEquals(65 / 256.0, Chance.lerp(1, 64, 200), 1e-9);
        assertEquals(201 / 256.0, Chance.lerp(99, 64, 200), 1e-9);
    }
}
