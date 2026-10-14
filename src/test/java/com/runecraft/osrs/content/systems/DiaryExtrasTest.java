package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.npcs.ShopDef;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.osrs.content.skilling.Motherlode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** NPC services, chests, joins and shops behind the round-12 diary tasks (OSRS Wiki values). */
class DiaryExtrasTest {
    @Test
    void chestsAndTindelRollWikiTables() {
        ContentRegistry.monsters();
        Random r = new Random(7);
        int dragonstones = 0;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 4000; i++) {
            List<DiaryExtras.Loot> crystal = DiaryExtras.crystalChest(r::nextDouble);
            assertEquals("uncut_dragonstone", crystal.get(0).item());
            dragonstones++;
            crystal.forEach(l -> seen.add(l.item()));
            List<DiaryExtras.Loot> muddy = DiaryExtras.muddyChest(r::nextDouble);
            assertTrue(muddy.size() >= 5);
            assertEquals(15, muddy.get(4).count(), "15 chaos runes");
        }
        assertEquals(4000, dragonstones);
        for (String item : List.of("runite_bar", "loop_half_of_key", "tooth_half_of_key", "coal", "iron_ore")) {
            assertTrue(seen.contains(item), item);
        }
        int swords = 0;
        for (int i = 0; i < 10000; i++) {
            DiaryExtras.Loot l = DiaryExtras.tindel(r::nextDouble);
            if (DiaryExtras.identified(l)) {
                swords++;
                assertNotNull(ItemCatalog.get(l.item()), l.item());
            }
        }
        assertTrue(swords > 9600 && swords < 9900, "98/100 come back a sword: " + swords);
    }

    @Test
    void joinsShopsAndRecipesExist() {
        ContentRegistry.monsters();
        for (String[] j : DiaryExtras.JOINS) {
            for (String id : j) {
                assertNotNull(ItemCatalog.get(id), id);
            }
        }
        assertEquals("skull_sceptre", DiaryExtras.join("runed_sceptre")[2]);
        for (String shop : List.of("warrens_general_store", "keldagrim_stonemason", "white_knight_armoury", "cape_merchant_william")) {
            ShopDef def = Shops.get(shop);
            assertNotNull(def, shop);
            def.stock().forEach(s -> assertTrue(s.item().startsWith("minecraft:") || ItemCatalog.get(s.item()) != null, shop + " " + s.item()));
        }
        Set<String> capes = new HashSet<>();
        for (ShopDef s : Shops.all()) {
            if (s.id().startsWith("cape_merchant_")) {
                s.stock().forEach(st -> capes.add(st.item()));
            }
        }
        assertEquals(50, capes.size(), "the ten merchants sell all 50 team capes");
        boolean gold = false;
        boolean shield = false;
        for (Recipe rec : Recipes.all()) {
            gold |= rec.menu() == MenuType.SMITHING && rec.outputs().containsKey("gold_helmet");
            shield |= rec.outputs().containsKey("neitiznot_shield");
        }
        assertTrue(gold && shield);
        assertNotNull(Jewellery.worn("camulet"));
        assertFalse(Jewellery.worn("camulet").teleports().isEmpty());
        assertEquals("enakhras_temple", Diaries.slug(Teleports.CAMULET.get(0).name()));
        assertEquals("barbarian_village", Diaries.slug(Teleports.SKULL_SCEPTRE.name()));
    }

    @Test
    void strutsBreakTwoToFourRoundsAfterRepair() {
        assertEquals(1000 + 2 * 97, Motherlode.strutBreaksAt(1000, 0));
        assertEquals(1000 + 4 * 97, Motherlode.strutBreaksAt(1000, 2));
        assertEquals(1485, Motherlode.strutRepairXpTenths(99), "1.5 x Smithing level");
    }

    @Test
    void undergroundRestrictionParses() {
        assertTrue(Diaries.restrictContains("3028,3415,40,u", "ASGARNIA", 3030, 3420));
        int[] c = Diaries.visitTarget("3028,3415,40,u");
        assertEquals(40, c[2]);
    }
}
