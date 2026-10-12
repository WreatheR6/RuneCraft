package com.runecraft.osrs.data;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.npcs.ShopDef;
import com.runecraft.osrs.content.npcs.ShopPricing;
import com.runecraft.osrs.content.npcs.ShopStock;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.osrs.content.systems.GrandExchange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Item values, alchemy, weights, Grand Exchange prices/limits and shop data against OSRS. */
class ItemValuesTest {
    private static ItemDef item(String id) {
        ItemDef d = ItemCatalog.get(id);
        assertNotNull(d, id);
        return d;
    }

    private static void assertValue(String id, int value, int highAlch, int lowAlch) {
        ItemDef d = item(id);
        assertEquals(value, d.value(), id + " value");
        assertEquals(highAlch, d.highAlch(), id + " high alch");
        assertEquals(lowAlch, d.lowAlch(), id + " low alch");
    }

    @Test
    void referenceStoreValuesAndAlchemy() {
        assertValue("shark", 170, 102, 68);
        assertValue("lobster", 70, 42, 28);
        assertValue("rune_platebody", 65000, 39000, 26000);
        assertValue("rune_scimitar", 25600, 15360, 10240);
        assertValue("abyssal_whip", 120001, 72000, 48000);
        assertValue("rune_pickaxe", 32000, 19200, 12800);
        assertValue("magic_logs", 320, 192, 128);
        assertValue("coal", 45, 27, 18);
        assertValue("nature_rune", 180, 108, 72);
        assertValue("law_rune", 240, 144, 96);
        assertValue("dragon_bones", 160, 96, 64);
        assertValue("bones", 1, 0, 0);
        assertValue("prayer_potion", 190, 114, 76); // 4-dose value
    }

    @Test
    void alchemyIsFloorOfSixtyAndFortyPercent() {
        for (ItemDef d : ItemCatalog.all()) {
            long v = d.value();
            assertEquals((int) Math.floor(v * 0.6), d.highAlch(), d.id());
            assertEquals((int) Math.floor(v * 0.4), d.lowAlch(), d.id());
        }
        ItemDef probe = new ItemDef("probe", "Probe", ItemDef.Kind.SIMPLE, "gem", 0).value(5);
        assertEquals(3, probe.highAlch());
        assertEquals(2, probe.lowAlch());
        probe.value(Integer.MAX_VALUE);
        assertEquals(1288490188, probe.highAlch()); // no overflow
    }

    @Test
    void referenceDataIsApplied() {
        assertTrue(ItemValues.size() > 2000, "reference items: " + ItemValues.size());
        assertEquals(9.979, item("rune_platebody").weight(), 1e-6);
        assertEquals(0.0, item("nature_rune").weight(), 1e-6);
        assertTrue(ItemValues.wornWeight("graceful_top") < 0);
        // Examine text filled from the wiki where the defining module had none.
        assertEquals("I'd better be careful eating this.", item("shark").examine());
        // OSRS-stackable items registered as single items now stack.
        assertTrue(item("reward_casket_easy").maxStack() > 1);
        for (ItemDef d : ItemCatalog.all()) {
            assertTrue(d.value() >= 0, d.id());
        }
    }

    @Test
    void untradeablesAreOffTheGrandExchange() {
        assertTrue(ItemValues.offGrandExchange("fire_cape"));
        assertTrue(ItemValues.offGrandExchange("dragon_defender"));
        assertFalse(ItemValues.offGrandExchange("shark"));
        assertFalse(ItemValues.offGrandExchange("abyssal_whip"));
        for (String id : Untradeables.all()) {
            assertFalse(GePrices.has(id), id + " has a GE price but is untradeable");
        }
    }

    @Test
    void grandExchangeGuidePricesAndLimits() {
        assertTrue(GePrices.size() > 1500, "GE items: " + GePrices.size());
        assertEquals(10000, GePrices.buyLimit("shark"));
        assertEquals(70, GePrices.buyLimit("abyssal_whip"));
        assertEquals(18000, GePrices.buyLimit("nature_rune"));
        // Guide prices are market prices, not store values (approx bands: the snapshot is July 2026).
        int whip = GrandExchange.guidePrice("abyssal_whip", item("abyssal_whip").value());
        assertTrue(whip > 500_000 && whip < 5_000_000, "whip " + whip);
        int shark = GrandExchange.guidePrice("shark", 170);
        assertTrue(shark > 300 && shark < 3000, "shark " + shark);
        // Untradeables fall back to their store value.
        assertEquals(65007, GrandExchange.guidePrice("fire_cape", item("fire_cape").value()));
        for (ItemDef d : ItemCatalog.all()) {
            if (GePrices.has(d.id())) {
                assertTrue(GePrices.guide(d.id()) > 0, d.id());
                assertFalse(ItemValues.offGrandExchange(d.id()), d.id());
            }
        }
    }

    @Test
    void buyLimitCapsPurchasesForFourHours() {
        GrandExchange ge = new GrandExchange();
        ge.setBuyLimits(id -> id.equals("abyssal_whip") ? 70 : 0);
        ge.place(ge.create("seller", "abyssal_whip", false, 200, 1_000_000, 1));
        GrandExchange.Offer buy = ge.create("buyer", "abyssal_whip", true, 100, 1_000_000, 2);
        List<GrandExchange.Trade> trades = ge.place(buy);
        assertEquals(1, trades.size());
        assertEquals(70, buy.filled);
        assertFalse(buy.complete());
        assertEquals(0, ge.buyLimitRemaining("buyer", "abyssal_whip", 3));
        assertEquals(70, ge.buyLimitRemaining("someone_else", "abyssal_whip", 3));
        // The NPC market respects the limit too, until the four hours are up.
        assertEquals(0, ge.npcFill(id -> 900_000, 100));
        long later = 2 + GrandExchange.BUY_LIMIT_TICKS;
        assertEquals(70, ge.buyLimitRemaining("buyer", "abyssal_whip", later));
        assertEquals(1, ge.npcFill(id -> 900_000, later));
        assertTrue(buy.complete());
        assertEquals(100, buy.itemsToCollect);
        assertEquals(40, ge.buyLimitRemaining("buyer", "abyssal_whip", later));
    }

    @Test
    void shopStockExistsAndRatesMatchOsrs() {
        for (ShopDef shop : Shops.all()) {
            for (ShopDef.Stock s : shop.stock()) {
                assertTrue(ItemCatalog.exists(s.item()), shop.id() + " sells unknown " + s.item());
                for (com.runecraft.osrs.Skill skill : com.runecraft.osrs.Skill.values()) {
                    assertFalse(s.item().startsWith(skill.id() + "_cape"), shop.id() + " sells a skill cape");
                }
            }
        }
        ShopDef lumbridge = Shops.get("lumbridge_general_store");
        assertEquals(1300, lumbridge.sellPm());
        assertEquals(400, lumbridge.buyPm());
        assertEquals(30, lumbridge.deltaPm());
        ShopDef aubury = Shops.get("auburys_rune_shop");
        assertEquals(1000, aubury.sellPm());
        assertEquals(600, aubury.buyPm());
        assertEquals(1, aubury.deltaPm());
        assertEquals(5000, aubury.entry("air_rune").base());
        ShopDef lowe = Shops.get("lowes_archery_emporium");
        assertEquals(550, lowe.buyPm());
        assertEquals(10, lowe.deltaPm());
        ShopDef westArdy = Shops.get("west_ardougne_general_store");
        assertEquals(1200, westArdy.sellPm());
        assertEquals(550, westArdy.buyPm());
    }

    @Test
    void shopPriceFormulaUsesCorrectedValues() {
        ShopDef aubury = Shops.get("auburys_rune_shop");
        // Rune shops change price by only 0.1% per rune: an air rune (value 4) costs 4 gp at normal stock
        // and 8 gp once 1,000 have been bought out of the 5,000.
        assertEquals(4, ShopPricing.sellPrice(aubury, 4, 5000, 5000));
        assertEquals(8, ShopPricing.sellPrice(aubury, 4, 5000, 4000));
        assertEquals(4, ShopPricing.sellPrice(aubury, item("air_rune").value(), 5000, 5000));
        ShopDef lumbridge = Shops.get("lumbridge_general_store");
        ShopStock stock = new ShopStock(lumbridge);
        // Hammer (value 1): 130% rounds down to 1 gp, never below 1.
        assertEquals(1, stock.priceToBuy("hammer"));
        // General stores buy any valued item at 40%, minus 3% per item already held.
        assertEquals(68, stock.priceToSell("shark", item("shark").value()));
        ShopStock never = new ShopStock(ShopDef.specialist("t_nobuy", "No buy", "x").prices(1000, 0, 20)
                .sell("hammer", 5).build());
        assertFalse(never.accepts("hammer", 1));
    }
}
