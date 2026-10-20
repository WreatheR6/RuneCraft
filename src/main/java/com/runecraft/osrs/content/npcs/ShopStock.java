package com.runecraft.osrs.content.npcs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live stock of one shop (shared by every player, as in OSRS). Pure logic: the Minecraft side moves
 * items and coins and calls {@link #tick()} once per game tick.
 */
public final class ShopStock {
    /** A line shown in the shop: normal stock first, then anything players have sold to it. */
    public record Line(String item, int count, int base, int value) {
    }

    private final ShopDef shop;
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    /** Value of items players sold that the shop doesn't normally stock. */
    private final Map<String, Integer> extraValues = new LinkedHashMap<>();
    private int ticks;

    public ShopStock(ShopDef shop) {
        this.shop = shop;
        for (ShopDef.Stock s : shop.stock()) {
            counts.put(s.item(), s.base());
        }
    }

    public ShopDef shop() {
        return shop;
    }

    public int count(String item) {
        return counts.getOrDefault(item, 0);
    }

    public int base(String item) {
        ShopDef.Stock s = shop.entry(item);
        return s == null ? 0 : s.base();
    }

    /** Value of an item in this shop (stock value, value it was sold at, or 0 if unknown). */
    public int value(String item) {
        ShopDef.Stock s = shop.entry(item);
        if (s != null) {
            return s.resolvedValue();
        }
        return extraValues.getOrDefault(item, 0);
    }

    public List<Line> lines() {
        List<Line> out = new ArrayList<>();
        for (ShopDef.Stock s : shop.stock()) {
            out.add(new Line(s.item(), count(s.item()), s.base(), s.resolvedValue()));
        }
        for (Map.Entry<String, Integer> e : extraValues.entrySet()) {
            int c = count(e.getKey());
            if (c > 0) {
                out.add(new Line(e.getKey(), c, 0, e.getValue()));
            }
        }
        return out;
    }

    /** Price of the next one of {@code item} bought from the shop. */
    public int priceToBuy(String item) {
        return ShopPricing.sellPrice(shop, value(item), base(item), count(item));
    }

    /** What the shop pays for the next one of {@code item} (value must be supplied for unknown items). */
    public int priceToSell(String item, int itemValue) {
        int v = value(item) > 0 ? value(item) : itemValue;
        return ShopPricing.buyPrice(shop, v, base(item), count(item));
    }

    /** Whether this shop accepts {@code item} (general stores accept anything valuable). */
    public boolean accepts(String item, int itemValue) {
        if (item.equals(shop.currency()) || shop.buyPm() <= 0) {
            return false; // a buy rate of 0 means the shop never buys (OSRS: e.g. Mage Arena staffs)
        }
        // general stores "accept all tradeable items" (wiki): never an untradeable such as an imbued god cape
        return shop.stocks(item) || (shop.general() && itemValue > 0 && !com.runecraft.osrs.data.Untradeables.untradeable(item));
    }

    /**
     * Removes up to {@code wanted} items the buyer can afford with {@code coins}. Returns
     * {count bought, total cost}.
     */
    public long[] buy(String item, int wanted, long coins) {
        int value = value(item);
        int stock = count(item);
        int n = ShopPricing.affordable(shop, value, base(item), stock, wanted, coins);
        long cost = ShopPricing.costToBuy(shop, value, base(item), stock, n);
        if (n > 0) {
            counts.put(item, stock - n);
        }
        return new long[]{n, cost};
    }

    /** Adds {@code n} sold items to the stock. Returns the coins paid (0 if not accepted). */
    public long sell(String item, int n, int itemValue) {
        if (n <= 0 || !accepts(item, itemValue)) {
            return 0;
        }
        int value = value(item) > 0 ? value(item) : itemValue;
        if (!shop.stocks(item)) {
            extraValues.putIfAbsent(item, value);
        }
        long paid = ShopPricing.payoutForSelling(shop, value, base(item), count(item), n);
        counts.put(item, count(item) + n);
        return paid;
    }

    /** Advances one game tick; returns true if any stock changed (a restock step happened). */
    public boolean tick() {
        if (++ticks < shop.restockTicks()) {
            return false;
        }
        ticks = 0;
        return restockStep();
    }

    /** Moves every line one item towards its normal stock (items players sold slowly disappear). */
    public boolean restockStep() {
        boolean changed = false;
        Iterator<Map.Entry<String, Integer>> it = counts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            int base = base(e.getKey());
            int c = e.getValue();
            if (c < base) {
                e.setValue(c + 1);
                changed = true;
            } else if (c > base) {
                e.setValue(c - 1);
                changed = true;
            }
            if (!shop.stocks(e.getKey()) && e.getValue() <= 0) {
                it.remove();
                extraValues.remove(e.getKey());
            }
        }
        return changed;
    }
}
