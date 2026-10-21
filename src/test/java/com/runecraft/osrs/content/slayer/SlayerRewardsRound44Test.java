package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Untradeables;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 44's Slayer rewards and equipment (OSRS Wiki "Slayer Rewards", "Slayer Equipment (shop)", "Rune pouch", "Slayer
 * helmet", July 2026): Task Storage and Duly Noted, the rune pouch, the Twisted/Hooded/Oathplate recolours, helmet
 * disassembly, and the shop's explosives, refills, packs, thrownhammers and goggles.
 */
class SlayerRewardsRound44Test {
    @Test
    void newUnlocksAndBuys() {
        assertEquals(500, SlayerRewards.unlock(SlayerRewards.TASK_STORAGE).cost());
        assertEquals(200, SlayerRewards.unlock(SlayerRewards.DULY_NOTED).cost());
        assertEquals(200, SlayerRewards.unlock("twisted_vision").cost());
        assertEquals(200, SlayerRewards.unlock("oath_breaker").cost());
        assertEquals(1000, SlayerRewards.unlock("absolutely_slayin").cost());
        assertEquals(750, SlayerRewards.pointBuy("rune_pouch").cost());
        assertTrue(SlayerRewards.ONE_ONLY.contains("rune_pouch"));
        assertTrue(Untradeables.untradeable("rune_pouch"));
    }

    @Test
    void dulyNotedOnlyNotesMithrilDragonBars() {
        assertTrue(SlayerRewards.dulyNoted("mithril_dragon", "mithril_bar"));
        assertFalse(SlayerRewards.dulyNoted("mithril_dragon", "rune_battleaxe"));
        assertFalse(SlayerRewards.dulyNoted("adamant_dragon", "mithril_bar"));
        // mithril dragons really drop mithril bars
        assertTrue(ContentRegistry.monster("mithril_dragon").drops.stream().anyMatch(d -> d.item().equals("mithril_bar")));
    }

    @Test
    void shopStocksEveryWikiItem() {
        for (String id : List.of("fishing_explosive", "fungicide", "broad_arrowhead_pack", "unfinished_broad_bolt_pack",
                "rock_thrownhammer", "reinforced_goggles")) {
            assertNotNull(SlayerRewards.shopItem(id), id);
            assertTrue(ItemCatalog.exists(id), id);
        }
        assertEquals(5500, SlayerRewards.shopItem("broad_arrowhead_pack").price());
        assertEquals(200, SlayerRewards.shopItem("rock_thrownhammer").price());
        assertEquals("a_porcine_of_interest", SlayerRewards.SHOP_QUESTS.get("reinforced_goggles"));
        assertEquals(new SlayerRewards.Input("broad_arrowheads", 100), SlayerRewards.PACKS.get("broad_arrowhead_pack"));
        assertEquals(new SlayerRewards.Input("unfinished_broad_bolts", 100), SlayerRewards.PACKS.get("unfinished_broad_bolt_pack"));
        for (SlayerRewards.ShopItem s : SlayerRewards.SHOP) {
            assertTrue(ItemCatalog.exists(s.item()), s.item());
        }
        // the thrownhammer finishes gargoyles like the rock hammer
        assertEquals("rock_thrownhammer", SlayerGear.FINISHER_ALTERNATES.get("rock_hammer"));
    }

    @Test
    void newRecoloursAndDisassembly() {
        for (String c : List.of("twisted", "hooded", "oathplate")) {
            assertTrue(ItemCatalog.exists(c + "_slayer_helmet"), c);
            assertTrue(ItemCatalog.exists(c + "_slayer_helmet_i"), c);
            assertNotNull(SlayerRewards.craft(c + "_slayer_helmet"), c);
            assertTrue(SlayerGear.SLAYER_HELMETS.contains(c + "_slayer_helmet_i"), c);
        }
        assertEquals(com.runecraft.osrs.Skill.SLAYER, SlayerRewards.craft("hooded_slayer_helmet").skill());
        assertEquals(99, SlayerRewards.craft("hooded_slayer_helmet").level());
        // A plain helmet gives back its six components; an imbued one the imbued black mask; a recolour its head too.
        assertEquals(6, SlayerRewards.disassembly("slayer_helmet").size());
        assertTrue(SlayerRewards.disassembly("slayer_helmet_i").contains(new SlayerRewards.Input("black_mask_i", 1)));
        List<SlayerRewards.Input> black = SlayerRewards.disassembly("black_slayer_helmet");
        assertEquals(7, black.size());
        assertTrue(black.contains(new SlayerRewards.Input("kbd_heads", 1)));
        assertTrue(SlayerRewards.disassembly("twisted_slayer_helmet_i").contains(new SlayerRewards.Input("twisted_horns", 1)));
        assertEquals(6, SlayerRewards.disassembly("tztok_slayer_helmet").size(), "a Combat Achievement helmet gives nothing more");
        assertTrue(SlayerRewards.disassembly("black_mask").isEmpty());
        assertTrue(SlayerRewards.disassembly("rune_full_helm").isEmpty());
        for (String h : SlayerGear.SLAYER_HELMETS) {
            for (SlayerRewards.Input in : SlayerRewards.disassembly(h)) {
                assertTrue(ItemCatalog.exists(in.item()), h + " gives " + in.item());
            }
        }
    }

    @Test
    void ottoRepairsTheHastaNotTheSlayerMaster() {
        assertTrue(SlayerRewards.craft(SlayerRewards.DRAGON_HASTA) == null, "no longer a slayer craft");
        assertNotNull(ContentRegistry.npc(SlayerRewards.OTTO));
        assertTrue(ItemCatalog.exists(SlayerRewards.BROKEN_HASTA));
        assertTrue(ItemCatalog.exists(SlayerRewards.DRAGON_HASTA));
        // the broken hasta comes from the brimstone chest
        assertTrue(SlayerChests.BRIMSTONE_RARE.stream().anyMatch(d -> d.item().equals(SlayerRewards.BROKEN_HASTA)));
    }

    @Test
    void bellLuresWithinThreeTiles() {
        assertEquals(6, SlayerGear.BELL_RADIUS, "3 game tiles");
    }

    @Test
    void runePouchHoldsThreeKindsOf16000() {
        Map<String, Integer> pouch = new HashMap<>();
        assertEquals(16_000, RunePouch.deposit(pouch, "death_rune", 20_000));
        assertEquals(0, RunePouch.deposit(pouch, "death_rune", 5));
        assertEquals(100, RunePouch.deposit(pouch, "blood_rune", 100));
        assertEquals(50, RunePouch.deposit(pouch, "water_rune", 50));
        assertEquals(0, RunePouch.room(pouch, "soul_rune"), "a fourth kind doesn't fit");
        assertEquals(0, RunePouch.deposit(pouch, "coins", 5));
        assertEquals(50, RunePouch.withdraw(pouch, "water_rune", 60));
        assertEquals(2, RunePouch.kinds(pouch));
        assertEquals(16_000, RunePouch.room(pouch, "soul_rune"), "an emptied kind frees its slot");
        assertTrue(RunePouch.accepts("mist_rune"));
        assertTrue(ItemCatalog.exists(RunePouch.ID));
    }
}
