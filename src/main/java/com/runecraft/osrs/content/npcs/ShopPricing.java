package com.runecraft.osrs.content.npcs;

/**
 * OSRS shop price formula. Prices are a percentage of the item's value that moves with stock:
 * <ul>
 *   <li>buying from the shop costs {@code value * (sell% - change% * (stock - normalStock))}, so each
 *   item bought (stock falling) costs more and an overstocked item is cheaper;</li>
 *   <li>selling to the shop pays {@code value * (buy% - change% * (stock - normalStock))}, so each item
 *   sold (stock rising) is worth less.</li>
 * </ul>
 * Multi-item trades are priced one item at a time. Percentages never drop below 10% and the shop never
 * pays more for an item than it would immediately sell it back for. All rates are per mille.
 */
public final class ShopPricing {
    /** Lowest rate (per mille of value) either price can fall to. */
    public static final int MIN_PM = 100;

    private ShopPricing() {
    }

    /** Rate (per mille) the shop sells at when it holds {@code stock} of an item it normally holds {@code base} of. */
    public static int sellRate(ShopDef shop, int base, int stock) {
        return Math.max(MIN_PM, shop.sellPm() - shop.deltaPm() * (stock - base));
    }

    /** Rate (per mille) the shop buys at when it holds {@code stock} (before the sale). */
    public static int buyRate(ShopDef shop, int base, int stock) {
        int rate = Math.max(MIN_PM, shop.buyPm() - shop.deltaPm() * (stock - base));
        return Math.min(rate, sellRate(shop, base, stock + 1));
    }

    /** Price of the next single item bought from the shop (stock must be at least 1). */
    public static int sellPrice(ShopDef shop, int value, int base, int stock) {
        if (value <= 0) {
            return 0;
        }
        return (int) Math.max(1L, (long) value * sellRate(shop, base, stock) / 1000L);
    }

    /** Coins paid for the next single item sold to the shop. */
    public static int buyPrice(ShopDef shop, int value, int base, int stock) {
        if (value <= 0) {
            return 0;
        }
        return (int) ((long) value * buyRate(shop, base, stock) / 1000L);
    }

    /** Total cost of buying {@code n} items one at a time starting from {@code stock}. */
    public static long costToBuy(ShopDef shop, int value, int base, int stock, int n) {
        long total = 0;
        for (int i = 0; i < n && stock - i > 0; i++) {
            total += sellPrice(shop, value, base, stock - i);
        }
        return total;
    }

    /** Total paid for selling {@code n} items one at a time starting from {@code stock}. */
    public static long payoutForSelling(ShopDef shop, int value, int base, int stock, int n) {
        long total = 0;
        for (int i = 0; i < n; i++) {
            total += buyPrice(shop, value, base, stock + i);
        }
        return total;
    }

    /** How many items (up to {@code wanted} and the stock) can be bought with {@code coins}. */
    public static int affordable(ShopDef shop, int value, int base, int stock, int wanted, long coins) {
        int n = 0;
        long spent = 0;
        while (n < wanted && stock - n > 0) {
            long next = sellPrice(shop, value, base, stock - n);
            if (spent + next > coins) {
                break;
            }
            spent += next;
            n++;
        }
        return n;
    }
}
