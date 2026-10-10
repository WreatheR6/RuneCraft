package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.QuestNpc;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcsContentTest {
    private static final ShopDef GENERAL = ShopDef.general("t_general", "Test store", "x").sell("hammer", 5, 100).build();
    private static final ShopDef SPECIALIST = ShopDef.specialist("t_spec", "Test shop", "x").sell("iron_axe", 5, 100).build();

    // ------------------------------------------------------------------ pricing

    @Test
    void generalStoreSellsAt130PercentRisingThreePercentPerItem() {
        assertEquals(130, ShopPricing.sellPrice(GENERAL, 100, 5, 5));
        assertEquals(133, ShopPricing.sellPrice(GENERAL, 100, 5, 4));
        assertEquals(136, ShopPricing.sellPrice(GENERAL, 100, 5, 3));
        // Overstocked: cheaper.
        assertEquals(127, ShopPricing.sellPrice(GENERAL, 100, 5, 6));
        // Buying three in a row from full stock: 130 + 133 + 136.
        assertEquals(399, ShopPricing.costToBuy(GENERAL, 100, 5, 5, 3));
    }

    @Test
    void generalStoreBuysAt40PercentFallingThreePercentPerItem() {
        assertEquals(40, ShopPricing.buyPrice(GENERAL, 100, 0, 0));
        assertEquals(37, ShopPricing.buyPrice(GENERAL, 100, 0, 1));
        assertEquals(34, ShopPricing.buyPrice(GENERAL, 100, 0, 2));
        assertEquals(111, ShopPricing.payoutForSelling(GENERAL, 100, 0, 0, 3));
        // Never below 10% of value.
        assertEquals(10, ShopPricing.buyPrice(GENERAL, 100, 0, 500));
    }

    @Test
    void specialistShopPrices() {
        assertEquals(100, ShopPricing.sellPrice(SPECIALIST, 100, 5, 5));
        assertEquals(102, ShopPricing.sellPrice(SPECIALIST, 100, 5, 4));
        assertEquals(60, ShopPricing.buyPrice(SPECIALIST, 100, 5, 5));
        assertEquals(58, ShopPricing.buyPrice(SPECIALIST, 100, 5, 6));
        // Short of stock the shop pays more, but never more than it would sell the item back for.
        for (int stock = 0; stock < 10; stock++) {
            assertTrue(ShopPricing.buyPrice(SPECIALIST, 100, 5, stock) <= ShopPricing.sellPrice(SPECIALIST, 100, 5, stock + 1));
        }
    }

    @Test
    void cheapItemsCostAtLeastOneCoin() {
        assertEquals(1, ShopPricing.sellPrice(GENERAL, 1, 5, 5));
        assertEquals(0, ShopPricing.buyPrice(GENERAL, 1, 0, 0));
    }

    @Test
    void noArbitrageAcrossEveryShop() {
        for (ShopDef shop : Shops.all()) {
            for (ShopDef.Stock s : shop.stock()) {
                int v = s.resolvedValue();
                for (int stock = 0; stock <= s.base() + 3; stock++) {
                    int paid = ShopPricing.buyPrice(shop, v, s.base(), stock);
                    int cost = ShopPricing.sellPrice(shop, v, s.base(), stock + 1);
                    assertTrue(paid <= cost, shop.id() + " " + s.item() + " stock " + stock);
                }
            }
        }
    }

    // ------------------------------------------------------------------ live stock

    @Test
    void stockBuySellAndRestock() {
        ShopStock stock = new ShopStock(GENERAL);
        long[] bought = stock.buy("hammer", 10, 1_000_000);
        assertEquals(5, bought[0]); // only 5 in stock
        assertEquals(ShopPricing.costToBuy(GENERAL, 100, 5, 5, 5), bought[1]);
        assertEquals(0, stock.count("hammer"));
        // Can only afford two (130 + 133 = 263 <= 300 < 399).
        stock = new ShopStock(GENERAL);
        assertEquals(2, stock.buy("hammer", 5, 300)[0]);
        assertEquals(3, stock.count("hammer"));
        stock.restockStep();
        assertEquals(4, stock.count("hammer"));

        // General stores take anything valuable; sold extras show up and drift away.
        assertTrue(stock.accepts("iron_axe", 56));
        long paid = stock.sell("iron_axe", 2, 100);
        assertEquals(40 + 37, paid);
        assertEquals(2, stock.count("iron_axe"));
        assertTrue(stock.lines().stream().anyMatch(l -> l.item().equals("iron_axe") && l.count() == 2));
        stock.restockStep();
        stock.restockStep();
        assertEquals(0, stock.count("iron_axe"));
        assertFalse(stock.lines().stream().anyMatch(l -> l.item().equals("iron_axe")));
        assertFalse(stock.accepts("coins", 1));

        // Specialist shops only buy what they stock.
        ShopStock spec = new ShopStock(SPECIALIST);
        assertFalse(spec.accepts("hammer", 1));
        assertEquals(0, spec.sell("hammer", 1, 1));
        assertEquals(60, spec.sell("iron_axe", 1, 100));
        assertEquals(6, spec.count("iron_axe"));
        for (int i = 0; i < GENERAL.restockTicks() - 1; i++) {
            assertFalse(spec.tick());
        }
        assertTrue(spec.tick());
        assertEquals(5, spec.count("iron_axe"));
    }

    // ------------------------------------------------------------------ data integrity

    private static boolean npcExists(String id) {
        if (ContentRegistry.npc(id) != null) {
            return true;
        }
        try {
            QuestNpc.valueOf(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Test
    void shopsAreConsistent() {
        assertTrue(Shops.all().size() >= 80, "shops: " + Shops.all().size());
        Set<String> npcs = new HashSet<>();
        for (ShopDef shop : Shops.all()) {
            assertTrue(shop.id().matches("[a-z0-9_]+"), shop.id());
            assertTrue(npcs.add(shop.npc()), "npc runs two shops: " + shop.npc());
            assertTrue(npcExists(shop.npc()), shop.id() + " shopkeeper " + shop.npc());
            assertTrue(ItemCatalog.exists(shop.currency()), shop.id() + " currency");
            assertFalse(shop.stock().isEmpty(), shop.id());
            assertTrue(shop.sellPm() > shop.buyPm() && shop.deltaPm() > 0 && shop.restockTicks() > 0, shop.id());
            Set<String> items = new HashSet<>();
            for (ShopDef.Stock s : shop.stock()) {
                assertTrue(items.add(s.item()), shop.id() + " lists " + s.item() + " twice");
                assertTrue(ItemCatalog.exists(s.item()), shop.id() + " sells unknown " + s.item());
                assertTrue(s.item().contains(":") ? s.value() > 0 : ItemCatalog.get(s.item()) != null, shop.id() + " " + s.item());
                assertTrue(s.resolvedValue() > 0, shop.id() + " " + s.item() + " has no value");
                assertTrue(s.base() >= 0, shop.id() + " " + s.item());
            }
        }
        for (var e : Townsfolk.EXTRA_TRADERS.entrySet()) {
            assertTrue(npcExists(e.getKey()), "trader " + e.getKey());
            ShopDef shop = Shops.get(e.getValue());
            assertNotNull(shop, e.getValue());
            assertEquals(e.getKey(), shop.npc());
        }
    }

    @Test
    void townsfolkAreConsistent() {
        Set<String> ids = new HashSet<>();
        int bankers = 0;
        for (TownNpc n : Townsfolk.all()) {
            assertTrue(n.id().startsWith("npc_"), n.id());
            assertTrue(n.id().matches("[a-z0-9_]+"), n.id());
            assertTrue(ids.add(n.id()), "duplicate " + n.id());
            assertTrue(n.x() > 1000 && n.x() < 4000 && n.y() > 2500 && n.y() < 4100, n.id() + " tile");
            assertEquals(4, n.colors().length, n.id());
            switch (n.role()) {
                case SHOP -> {
                    ShopDef shop = Shops.get(n.shop());
                    assertNotNull(shop, n.id() + " shop " + n.shop());
                    assertEquals(n.id(), shop.npc(), n.shop());
                }
                case TRAVEL -> {
                    assertNotNull(Travel.get(n.travel()), n.id() + " route " + n.travel());
                    assertFalse(n.lines().isEmpty(), n.id());
                }
                case AMBIENT -> assertFalse(n.lines().isEmpty(), n.id());
                case BANKER -> bankers++;
                default -> {
                }
            }
        }
        assertTrue(bankers >= 60, "bankers " + bankers);
        // Every shop has a keeper who exists.
        for (ShopDef shop : Shops.all()) {
            TownNpc keeper = Townsfolk.get(shop.npc());
            assertTrue(keeper != null || Townsfolk.EXTRA_TRADERS.containsKey(shop.npc()), shop.id());
        }
    }

    @Test
    void npcsStandOnTheirExactTiles() {
        NpcsContent module = new NpcsContent();
        Set<String> places = new HashSet<>();
        for (PlaceDef p : module.places()) {
            assertTrue(places.add(p.id()));
            assertEquals(PlaceDef.Style.NONE, p.style());
            assertNotNull(ContentRegistry.place(p.id()), p.id());
            assertTrue(Landmark.byName(p.id()) == null, p.id());
        }
        for (NpcDef def : module.npcs()) {
            assertTrue(places.contains(def.place()), def.id());
            assertTrue(def.dx() >= -8 && def.dx() <= 7 && def.dz() >= -8 && def.dz() <= 7, def.id() + " offset");
            TownNpc n = Townsfolk.get(def.id());
            PlaceDef p = ContentRegistry.place(def.place());
            assertEquals(com.runecraft.osrs.GielinorMap.toBlockX(n.x()), p.blockX() + def.dx(), def.id());
            assertEquals(com.runecraft.osrs.GielinorMap.toBlockZ(n.y()), p.blockZ() + def.dz(), def.id());
            assertNotNull(ContentRegistry.npc(def.id()), def.id());
        }
    }

    @Test
    void travelRoutesAreValid() {
        for (Travel.Route r : Travel.all()) {
            assertFalse(r.dests().isEmpty(), r.id());
            for (Travel.Dest d : r.dests()) {
                assertTrue(d.price() >= 0, r.id());
                assertTrue(d.x() > 1000 && d.x() < 4000 && d.y() > 2500 && d.y() < 4100, r.id() + " " + d.name());
                assertTrue(d.quest() == null || ContentRegistry.quest(d.quest()) != null, r.id() + " quest " + d.quest());
            }
        }
        assertEquals(30, Travel.get("boat_port_sarim_karamja").dests().get(0).price());
        assertEquals(12, Travel.canoeLevel(0, 1));
        assertEquals(27, Travel.canoeLevel(0, 2));
        assertEquals(42, Travel.canoeLevel(0, 3));
        assertEquals(57, Travel.canoeLevel(3, 4));
        assertEquals(Travel.charterFare(1, 2), Travel.charterFare(2, 1));
        assertEquals(7, Travel.get(Travel.charterRoute(0)).dests().size());
    }

    @Test
    void townMonstersAreConsistent() {
        Set<String> mine = new HashSet<>();
        for (MonsterDef m : TownMonsters.monsters()) {
            mine.add(m.id);
            assertNotNull(ContentRegistry.monster(m.id), m.id);
            assertTrue(m.combatLevel >= 1 && m.hitpoints > 0, m.id);
            for (Drop d : m.drops) {
                assertTrue(ItemCatalog.exists(d.item()), m.id + " drops " + d.item());
            }
        }
        for (TownMonsters.Pickpocket p : TownMonsters.pickpockets()) {
            assertTrue(mine.contains(p.creature()), p.creature());
            assertTrue(p.level() >= 1 && p.level() <= 99 && p.xp() > 0 && p.stunDamage() >= 1, p.creature());
            assertTrue(p.low() < p.high(), p.creature());
            assertFalse(p.always().isEmpty() && p.table().isEmpty(), p.creature());
            for (TownMonsters.Loot l : p.always()) {
                assertTrue(ItemCatalog.exists(l.item()), p.creature() + " " + l.item());
            }
            for (TownMonsters.Loot l : p.table()) {
                assertTrue(ItemCatalog.exists(l.item()) && l.weight() > 0 && l.min() <= l.max(), p.creature() + " " + l.item());
            }
        }
        for (TownMonsters.TownSpawn s : TownMonsters.spawns()) {
            boolean exists = mine.contains(s.creature()) || com.runecraft.osrs.data.Monster.byEntity(s.creature()) != null
                    || ContentRegistry.monster(s.creature()) != null;
            assertTrue(exists, "spawn of unknown " + s.creature());
            assertTrue(s.count() > 0 && s.radius() > 0, s.creature());
        }
    }
}
