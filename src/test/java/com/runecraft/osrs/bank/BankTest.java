package com.runecraft.osrs.bank;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.systems.GrandExchange;
import com.runecraft.osrs.data.GePrices;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bank storage, tabs, placeholders, quantities, notes, value and the persisted Grand Exchange buy limits. */
class BankTest {
    private static List<String> keys(List<BankModel.Entry> entries) {
        return entries.stream().map(BankModel.Entry::key).toList();
    }

    @Test
    void depositMergesAndStacksBeyondMinecraftLimits() {
        BankModel b = new BankModel();
        assertEquals(1000, b.deposit("lobster", "", 1000, 0));
        assertEquals(500, b.deposit("lobster", "", 500, 0));
        assertEquals(1500, b.count("lobster", ""));
        assertEquals(1, b.slotsUsed());
        // A charged/dosed variant is its own slot.
        b.deposit("prayer_potion", "{Damage:1}", 3, 0);
        b.deposit("prayer_potion", "", 2, 0);
        assertEquals(3, b.slotsUsed());
        // OSRS max stack.
        b.deposit("coins", "", Integer.MAX_VALUE, 0);
        assertEquals(0, b.deposit("coins", "", 5, 0));
        assertEquals(Integer.MAX_VALUE, b.count("coins", ""));
    }

    @Test
    void capacityIs800Slots() {
        BankModel b = new BankModel();
        for (int i = 0; i < BankModel.CAPACITY; i++) {
            assertEquals(1, b.deposit("item" + i, "", 1, 0));
        }
        assertEquals(0, b.deposit("one_more", "", 1, 0));
        assertFalse(b.hasRoomFor("one_more", ""));
        assertTrue(b.hasRoomFor("item5", ""));
        assertEquals(3, b.deposit("item5", "", 3, 0));
    }

    @Test
    void withdrawRemovesOrLeavesPlaceholder() {
        BankModel b = new BankModel();
        b.deposit("shark", "", 10, 0);
        assertEquals(4, b.withdraw("shark", "", 4));
        assertEquals(6, b.withdraw("shark", "", 100));
        assertEquals(0, b.slotsUsed());
        b.placeholders = true;
        b.deposit("shark", "", 10, 0);
        b.withdraw("shark", "", 10);
        assertEquals(1, b.slotsUsed());
        assertTrue(b.find("shark", "").placeholder());
        assertEquals(0, b.withdraw("shark", "", 1));
        // Depositing refills the placeholder in place.
        b.deposit("rune_scimitar", "", 1, 0);
        b.deposit("shark", "", 3, 0);
        assertEquals(List.of("shark", "rune_scimitar"), keys(b.entries()));
    }

    @Test
    void releasePlaceholdersRemovesOnlyEmptySlots() {
        BankModel b = new BankModel();
        b.placeholders = true;
        b.deposit("a", "", 1, 0);
        b.deposit("b", "", 1, 0);
        b.withdraw("a", "", 1);
        assertEquals(1, b.releasePlaceholders());
        assertEquals(List.of("b"), keys(b.entries()));
    }

    @Test
    void tabsOrderingAndDragToTab() {
        BankModel b = new BankModel();
        b.deposit("logs", "", 1, 0);
        b.deposit("coal", "", 1, 0);
        b.deposit("shark", "", 1, 0);
        // Tab numbers can only open one past the last tab.
        assertTrue(b.moveToTab("shark", 5));
        assertEquals(1, b.find("shark", "").tab);
        assertTrue(b.moveToTab("coal", 2));
        assertEquals(2, b.tabCount());
        // View all: tab 1, tab 2, then the main tab.
        assertEquals(List.of("shark", "coal", "logs"), keys(b.view(0, "", null)));
        assertEquals(List.of("coal"), keys(b.view(2, "", null)));
        // New items deposited while viewing a tab go to its end.
        b.deposit("tuna", "", 1, 1);
        assertEquals(List.of("shark", "tuna", "coal", "logs"), keys(b.entries()));
        // Emptying tab 1 closes it; tab 2 becomes tab 1.
        b.withdraw("shark", "", 1);
        b.withdraw("tuna", "", 1);
        assertEquals(1, b.tabCount());
        assertEquals(1, b.find("coal", "").tab);
        assertEquals(9, BankModel.MAX_TABS);
    }

    @Test
    void atMostNineTabs() {
        BankModel b = new BankModel();
        for (int i = 0; i < 12; i++) {
            b.deposit("i" + i, "", 1, 0);
            b.moveToTab("i" + i, b.tabCount() + 1);
        }
        assertEquals(9, b.tabCount());
        assertEquals(4, b.view(9, "", null).size());
    }

    @Test
    void swapAndInsert() {
        BankModel b = new BankModel();
        b.deposit("a", "", 1, 0);
        b.deposit("b", "", 1, 0);
        b.deposit("c", "", 1, 0);
        b.moveToTab("c", 1);
        assertEquals(List.of("c", "a", "b"), keys(b.entries()));
        // Swap across tabs trades tabs too, keeping display order consistent.
        assertTrue(b.swap("c", "b"));
        assertEquals(List.of("b", "a", "c"), keys(b.entries()));
        assertEquals(1, b.find("b", "").tab);
        assertEquals(0, b.find("c", "").tab);
        assertTrue(b.insert("c", "a"));
        assertEquals(List.of("b", "c", "a"), keys(b.entries()));
    }

    @Test
    void searchIgnoresTabs() {
        BankModel b = new BankModel();
        b.deposit("raw_shark", "", 1, 0);
        b.deposit("shark", "", 1, 0);
        b.deposit("logs", "", 1, 0);
        b.moveToTab("shark", 1);
        Map<String, String> names = Map.of("raw_shark", "Raw shark", "shark", "Shark", "logs", "Logs");
        assertEquals(List.of("shark", "raw_shark"), keys(b.view(1, "SHARK", names::get)));
    }

    @Test
    void loadRestoresOrder() {
        BankModel b = new BankModel();
        b.load("x", "", 5, 0);
        b.load("y", "", 0, 3);
        b.load("x", "", 9, 0);
        b.finishLoad();
        assertEquals(List.of("y", "x"), keys(b.entries()));
        assertEquals(1, b.find("y", "").tab);
        assertEquals(5, b.count("x", ""));
    }

    @Test
    void quantities() {
        assertEquals(1, BankQuantity.amount(BankQuantity.Mode.ONE, 0, 100));
        assertEquals(5, BankQuantity.amount(BankQuantity.Mode.FIVE, 0, 100));
        assertEquals(3, BankQuantity.amount(BankQuantity.Mode.TEN, 0, 3));
        assertEquals(42, BankQuantity.amount(BankQuantity.Mode.X, 42, 100));
        assertEquals(100, BankQuantity.amount(BankQuantity.Mode.ALL, 0, 100));
        assertEquals(99, BankQuantity.amount(BankQuantity.Mode.ALL_BUT_ONE, 0, 100));
        assertEquals(0, BankQuantity.amount(BankQuantity.Mode.ALL, 0, 0));
        assertEquals(10_000, BankQuantity.parseX("10k"));
        assertEquals(1_500_000, BankQuantity.parseX("1.5m"));
        assertEquals(Integer.MAX_VALUE, BankQuantity.parseX("5b"));
        assertEquals(1234, BankQuantity.parseX("1,234"));
        assertEquals(-1, BankQuantity.parseX("abc"));
        assertEquals("99999", BankQuantity.shortCount(99_999));
        assertEquals("100K", BankQuantity.shortCount(100_000));
        assertEquals("10M", BankQuantity.shortCount(10_000_000));
    }

    @Test
    void noteRules() {
        assertNotNull(ItemCatalog.get(BankNotes.NOTE_ITEM));
        assertTrue(BankNotes.canNote("lobster"));
        assertTrue(BankNotes.canNote("rune_platebody"));
        assertTrue(BankNotes.canNote("logs"));
        // Stackable items have no note.
        assertFalse(BankNotes.canNote("coins"));
        assertFalse(BankNotes.canNote("air_rune"));
        // Untradeables can't be noted.
        assertFalse(BankNotes.canNote("fire_cape"));
        assertFalse(BankNotes.canNote("dragon_defender"));
        // A note can't be noted, and unknown ids can't either.
        assertFalse(BankNotes.canNote(BankNotes.NOTE_ITEM));
        assertFalse(BankNotes.canNote("no_such_item"));
        assertFalse(BankNotes.rule(true, false, false, false));
        assertFalse(BankNotes.rule(true, false, true, true));
        assertTrue(BankNotes.rule(true, false, true, false));
    }

    @Test
    void bankValueAtGuidePrices() {
        BankModel b = new BankModel();
        b.deposit("lobster", "", 100, 0);
        b.deposit("coins", "", 5000, 0);
        b.placeholders = true;
        b.deposit("shark", "", 1, 0);
        b.withdraw("shark", "", 1);
        long expected = 100L * GrandExchange.guidePrice("lobster", ItemCatalog.get("lobster").value()) + 5000;
        assertTrue(GePrices.guide("lobster") > 0);
        assertEquals(expected, b.value(id -> id.equals("coins") ? 1 : GrandExchange.guidePrice(id, ItemCatalog.get(id).value())));
        // Saturates instead of overflowing.
        BankModel big = new BankModel();
        big.deposit("a", "", Integer.MAX_VALUE, 0);
        big.deposit("b", "", Integer.MAX_VALUE, 0);
        assertEquals(Long.MAX_VALUE, big.value(id -> Integer.MAX_VALUE * 4L));
    }

    @Test
    void depositBoxesArePlaces() {
        for (DepositBoxes.Spot s : DepositBoxes.SPOTS) {
            PlaceDef p = ContentRegistry.places().stream().filter(x -> x.id().equals(s.id())).findFirst().orElse(null);
            assertNotNull(p, s.id());
            assertEquals(PlaceDef.Style.NONE, p.style());
        }
    }

    @Test
    void buyLimitsSurviveARestart() {
        GrandExchange ge = new GrandExchange();
        ge.setBuyLimits(id -> 100);
        ge.recordBought("p", "shark", 70, 1000);
        List<GrandExchange.LimitWindow> saved = ge.saveBuyLimits(1000 + 600);
        assertEquals(1, saved.size());
        assertEquals(600, saved.get(0).elapsedTicks());

        // Server restarts: game ticks start again from 0.
        GrandExchange after = new GrandExchange();
        after.setBuyLimits(id -> 100);
        after.loadBuyLimits(saved, 0, 0);
        assertEquals(30, after.buyLimitRemaining("p", "shark", 0));
        assertEquals(GrandExchange.BUY_LIMIT_TICKS - 600, after.buyLimitResets("p", "shark", 0));
        // Downtime counts towards the four hours.
        GrandExchange late = new GrandExchange();
        late.setBuyLimits(id -> 100);
        late.loadBuyLimits(saved, 0, GrandExchange.BUY_LIMIT_TICKS);
        assertEquals(100, late.buyLimitRemaining("p", "shark", 0));
        // Expired windows aren't saved.
        assertTrue(ge.saveBuyLimits(1000 + GrandExchange.BUY_LIMIT_TICKS).isEmpty());
    }
}
