package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.minigames.LastManStanding.Build;
import com.runecraft.osrs.content.minigames.LastManStanding.Stack;
import com.runecraft.osrs.content.minigames.LastManStanding.Table;
import com.runecraft.osrs.content.systems.Jewellery;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LastManStandingTest {
    @Test
    void minigameIsLastAndEnteredAtFerox() {
        Minigame[] all = Minigame.values();
        assertEquals(Minigame.LAST_MAN_STANDING.ordinal() + 1, Minigame.EMIRS_ARENA.ordinal());
        assertEquals(Minigame.EMIRS_ARENA.ordinal() + 1, Minigame.GAUNTLET.ordinal());
        assertEquals(Minigame.GAUNTLET, all[all.length - 1]);
        assertEquals(3142, Minigame.LAST_MAN_STANDING.entranceX());
        assertEquals(3636, Minigame.LAST_MAN_STANDING.entranceY());
        assertEquals(24, Minigame.LAST_MAN_STANDING.maxPlayers());
    }

    @Test
    void buildStatsFollowTheWiki() {
        assertEquals(85, Build.MAX_MED.level(com.runecraft.osrs.Skill.DEFENCE));
        assertEquals(80, Build.ZERKER.level(com.runecraft.osrs.Skill.ATTACK));
        assertEquals(50, Build.ZERKER.level(com.runecraft.osrs.Skill.DEFENCE));
        assertEquals(75, Build.PURE.level(com.runecraft.osrs.Skill.ATTACK));
        assertEquals(1, Build.PURE.level(com.runecraft.osrs.Skill.DEFENCE));
        assertEquals(99, Build.PURE.level(com.runecraft.osrs.Skill.HITPOINTS));
    }

    @Test
    void everyLoadoutAndLootItemExists() {
        List<String> ids = new ArrayList<>();
        for (Build b : Build.values()) {
            LastManStanding.Loadout l = LastManStanding.loadout(b);
            ids.addAll(l.worn());
            l.carried().forEach(s -> ids.add(s.item()));
            for (Table t : Table.values()) {
                LastManStanding.table(b, t).forEach(bundle -> bundle.forEach(s -> ids.add(s.item())));
            }
        }
        LastManStanding.SUPPLIES.forEach(s -> ids.add(s.item()));
        for (String id : ids) {
            assertNotNull(ItemCatalog.get(id), id);
        }
    }

    @Test
    void lmsCopiesTakeTheRealBonuses() {
        ItemDef whip = ItemCatalog.get("lms_abyssal_whip");
        assertEquals(82, whip.equipment().stats().slashAttack());
        assertEquals(4, whip.equipment().attackSpeed());
        ItemDef zuriel = ItemCatalog.get("lms_zuriels_staff");
        assertEquals(10, zuriel.equipment().stats().magicDamagePercent());
        // Worn-slot copies are registered with the systems module's extra slots.
        assertEquals(Jewellery.Slot.RING, Jewellery.worn("lms_berserker_ring_i").slot());
        assertEquals(Jewellery.Slot.HANDS, Jewellery.worn("lms_barrows_gloves").slot());
        assertEquals(Jewellery.Perk.WEALTH, Jewellery.worn("ring_of_wealth_i_5").perk());
        assertEquals("ring_of_wealth_i_4", Jewellery.afterCharge(Jewellery.worn("ring_of_wealth_i_5")));
        assertEquals("ring_of_wealth_i", Jewellery.afterCharge(Jewellery.worn("ring_of_wealth_i_1")));
    }

    @Test
    void keysAndPoints() {
        assertEquals(LastManStanding.BLOODY_KEY, LastManStanding.keyFor(6));
        assertEquals(LastManStanding.BLOODIER_KEY, LastManStanding.keyFor(5));
        assertEquals(9, LastManStanding.points(1, 5));
        assertEquals(8, LastManStanding.points(1, 3));
        assertEquals(0, LastManStanding.points(1, 0));
        assertEquals(4, LastManStanding.points(4, 1));
        assertEquals(3, LastManStanding.points(9, 1));
        assertEquals(1, LastManStanding.points(19, 1));
        assertEquals(0, LastManStanding.points(20, 1));
        assertEquals(9, LastManStanding.rankChange(1));
        assertEquals(0, LastManStanding.rankChange(12));
        assertEquals(-8, LastManStanding.rankChange(24));
        assertEquals("Veteran", LastManStanding.rankTitle(600));
        assertEquals("Legend", LastManStanding.rankTitle(1000));
        assertTrue(LastManStanding.competitiveAllowed(750, 30));
        assertFalse(LastManStanding.competitiveAllowed(1499, 29));
    }

    @Test
    void fogClosesAndHurts() {
        Random r = new Random(1);
        assertEquals(0, LastManStanding.fogDamage(LastManStanding.FOG_TICK - 1, 50, 0, r));
        assertTrue(LastManStanding.safeRadius(LastManStanding.FOG_TICK) > LastManStanding.safeRadius(600));
        assertEquals(LastManStanding.FOG_FINAL_RADIUS, LastManStanding.safeRadius(LastManStanding.FINAL_TICK));
        for (int i = 0; i < 50; i++) {
            int light = LastManStanding.fogDamage(400, 5, 6, r);
            assertTrue(light >= 1 && light <= 3, "light " + light);
            int greater = LastManStanding.fogDamage(400, 20, 6, r);
            assertTrue(greater >= 1 && greater <= 10);
        }
        assertEquals(0, LastManStanding.fogDamage(400, 5, 1, r)); // every other tick
        assertEquals(10, LastManStanding.fogDamage(LastManStanding.FINAL_TICK + 200, 1, 1, r));
    }

    @Test
    void chestsRollBuildTables() {
        Random r = new Random(7);
        for (int i = 0; i < 200; i++) {
            List<Stack> loot = LastManStanding.openChest(Build.PURE, i % 2 == 0, r);
            assertTrue(loot.size() >= 2);
            for (Stack s : loot) {
                assertTrue(s.item().startsWith("lms_"), s.item());
            }
        }
        assertEquals(Build.ZERKER, LastManStanding.chooseBuild(Map.of(Build.ZERKER, 1_000_000), r));
    }

    @Test
    void imbueScrolls() {
        assertEquals("magic_shortbow_i", LastManStanding.imbued("magic_shortbow_scroll", "magic_shortbow"));
        assertEquals("ring_of_wealth_i_3", LastManStanding.imbued("ring_of_wealth_scroll", "ring_of_wealth_3"));
        assertEquals("ring_of_wealth_i", LastManStanding.imbued("ring_of_wealth_scroll", "ring_of_wealth"));
        assertNull(LastManStanding.imbued("ring_of_wealth_scroll", "magic_shortbow"));
        assertNotNull(ItemCatalog.get("magic_shortbow"));
        assertNotNull(ItemCatalog.get("ring_of_wealth_5"));
        RewardShop shop = RewardShops.get(LastManStanding.SHOP);
        assertTrue(shop.entries().stream().anyMatch(e -> e.item().equals("ring_of_wealth_scroll")));
        for (RewardShop.Entry e : shop.entries()) {
            assertNotNull(ItemCatalog.get(e.item()), e.item());
        }
        assertEquals(10, LastManStanding.winsNeeded("victors_cape_10"));
        assertEquals(1, LastManStanding.winsNeeded("magic_shortbow_scroll"));
    }

    @Test
    void islandFitsTheArena() {
        for (int[] c : LastManStanding.CHESTS) {
            int[] o = LastManStanding.offset(c[0], c[1]);
            assertTrue(Math.abs(o[0]) <= 33 && Math.abs(o[1]) <= 33, c[0] + "," + c[1]);
        }
        for (LastManStanding.Landmark l : LastManStanding.LANDMARKS) {
            int[] o = LastManStanding.offset(l.x(), l.y());
            assertTrue(Math.abs(o[0]) <= 33 && Math.abs(o[1]) <= 33, l.name());
        }
    }
}
