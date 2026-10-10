package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;

import java.util.ArrayList;
import java.util.List;

/**
 * A data-driven OSRS shop. {@code npc} is the shopkeeper's NPC id (a module NpcDef id or a built-in
 * QuestNpc enum name). Prices follow {@link ShopPricing}: the shop sells at {@code sellPm} per mille of
 * an item's value and buys at {@code buyPm}, both moving by {@code deltaPm} per item of stock above or
 * below the item's normal stock. Stock drifts back towards normal by one every {@code restockTicks}
 * game ticks. General stores buy anything with a value; specialist shops only buy what they stock.
 */
public record ShopDef(String id, String name, String npc, String currency, boolean general, int sellPm, int buyPm,
                      int deltaPm, int restockTicks, List<Stock> stock, String greeting) {

    /** One line of a shop's normal stock. {@code value} 0 means "use the item catalog's value". */
    public record Stock(String item, int base, int value) {
        /** The item's OSRS value (the price basis). */
        public int resolvedValue() {
            if (value > 0) {
                return value;
            }
            ItemDef def = ItemCatalog.get(item);
            return def == null ? 0 : def.value();
        }
    }

    public Stock entry(String item) {
        for (Stock s : stock) {
            if (s.item().equals(item)) {
                return s;
            }
        }
        return null;
    }

    public boolean stocks(String item) {
        return entry(item) != null;
    }

    // ------------------------------------------------------------------ builder

    /** General store: sells at 130%, buys at 40%, 3% per item (OSRS). */
    public static Builder general(String id, String name, String npc) {
        return new Builder(id, name, npc, true, 1300, 400, 30);
    }

    /**
     * Specialist shop: sells at 100%, buys at 60%, 2% per item. (OSRS specialist shops vary between
     * about 55-70% buy and 1-3% change; these are the common values.)
     */
    public static Builder specialist(String id, String name, String npc) {
        return new Builder(id, name, npc, false, 1000, 600, 20);
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private final String npc;
        private final boolean general;
        private int sellPm;
        private int buyPm;
        private int deltaPm;
        private String currency = "coins";
        private int restockTicks = 100;
        private String greeting = "Can I help you at all?";
        private final List<Stock> stock = new ArrayList<>();

        Builder(String id, String name, String npc, boolean general, int sellPm, int buyPm, int deltaPm) {
            this.id = id;
            this.name = name;
            this.npc = npc;
            this.general = general;
            this.sellPm = sellPm;
            this.buyPm = buyPm;
            this.deltaPm = deltaPm;
        }

        public Builder prices(int sellPm, int buyPm, int deltaPm) {
            this.sellPm = sellPm;
            this.buyPm = buyPm;
            this.deltaPm = deltaPm;
            return this;
        }

        public Builder currency(String item) {
            this.currency = item;
            return this;
        }

        public Builder restock(int ticks) {
            this.restockTicks = ticks;
            return this;
        }

        public Builder greeting(String line) {
            this.greeting = line;
            return this;
        }

        /** Stocks an item at its catalog value. */
        public Builder sell(String item, int base) {
            stock.add(new Stock(item, base, 0));
            return this;
        }

        /** Stocks an item at an explicit OSRS value (needed for vanilla items, or where the catalog differs). */
        public Builder sell(String item, int base, int value) {
            stock.add(new Stock(item, base, value));
            return this;
        }

        public ShopDef build() {
            return new ShopDef(id, name, npc, currency, general, sellPm, buyPm, deltaPm, restockTicks, List.copyOf(stock), greeting);
        }
    }
}
