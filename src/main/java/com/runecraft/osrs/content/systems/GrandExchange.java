package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.runecraft.osrs.data.GePrices;

/**
 * The Grand Exchange order book (pure logic). Buy and sell offers are matched like OSRS: the offer that
 * was placed first sets the price, buyers get refunded the difference. Sellers pay a 2% tax (items under
 * 50 gp are exempt; capped at 5M per item). An NPC market fills offers at the guide price with a margin
 * so solo players can trade. Guide prices are the OSRS Grand Exchange prices ({@link GePrices}), and each
 * player can only buy an item's OSRS buy limit every four hours; a bigger buy offer stays open and fills
 * the rest once the limit resets.
 */
public final class GrandExchange {
    public static final int MAX_OFFERS = 8;
    public static final double TAX = 0.02;
    public static final int TAX_FREE_BELOW = 50;
    public static final long TAX_CAP = 5_000_000;
    /** NPC market margin: buys at guide +5%, sells at guide -5%. */
    public static final double NPC_MARGIN = 0.05;
    /** Buy limits reset four hours after the first purchase in a window (4 h = 24,000 game ticks). */
    public static final long BUY_LIMIT_TICKS = 24_000;

    /** Buy limit per item (0 = unlimited). Defaults to the OSRS limits. */
    private java.util.function.ToIntFunction<String> buyLimits = GePrices::buyLimit;
    /** owner|item -> {window start tick, bought in window}. Saved with the GE state ({@link #saveBuyLimits}). */
    private final java.util.Map<String, long[]> bought = new java.util.HashMap<>();

    /** A buy-limit window as saved: how many game ticks of the window have passed and how many were bought. */
    public record LimitWindow(String owner, String item, long elapsedTicks, long bought) {
    }

    /**
     * The buy-limit windows still running at game tick {@code now}, with their elapsed time (game ticks restart
     * at 0 with the server, so absolute ticks can't be saved).
     */
    public List<LimitWindow> saveBuyLimits(long now) {
        List<LimitWindow> out = new ArrayList<>();
        for (java.util.Map.Entry<String, long[]> e : bought.entrySet()) {
            long elapsed = Math.max(0, now - e.getValue()[0]);
            int bar = e.getKey().indexOf('|');
            if (elapsed < BUY_LIMIT_TICKS && e.getValue()[1] > 0 && bar > 0) {
                out.add(new LimitWindow(e.getKey().substring(0, bar), e.getKey().substring(bar + 1), elapsed, e.getValue()[1]));
            }
        }
        return out;
    }

    /**
     * Restores saved buy-limit windows at game tick {@code now}. {@code offlineTicks} (real time the server was
     * down, in game ticks) also counts towards the four hours, as OSRS limits run in real time.
     */
    public void loadBuyLimits(List<LimitWindow> windows, long now, long offlineTicks) {
        bought.clear();
        for (LimitWindow w : windows) {
            long elapsed = Math.max(0, w.elapsedTicks()) + Math.max(0, offlineTicks);
            if (elapsed < BUY_LIMIT_TICKS && w.bought() > 0) {
                bought.put(w.owner() + "|" + w.item(), new long[]{now - elapsed, w.bought()});
            }
        }
    }

    public static final class Offer {
        public final long id;
        public final String owner;
        public final String item;
        public final boolean buy;
        public final int quantity;
        public final int price;
        public final long created;
        public int filled;
        /** Coins and items waiting in the collection box. */
        public long coinsToCollect;
        public int itemsToCollect;
        public boolean aborted;

        public Offer(long id, String owner, String item, boolean buy, int quantity, int price, long created) {
            this.id = id;
            this.owner = owner;
            this.item = item;
            this.buy = buy;
            this.quantity = quantity;
            this.price = price;
            this.created = created;
        }

        public int remaining() {
            return aborted ? 0 : quantity - filled;
        }

        public boolean complete() {
            return remaining() <= 0;
        }

        public boolean collected() {
            return complete() && coinsToCollect == 0 && itemsToCollect == 0;
        }
    }

    /** A completed trade between two offers. */
    public record Trade(Offer buyer, Offer seller, int quantity, int price) {
    }

    private final List<Offer> offers = new ArrayList<>();
    private long nextId = 1;

    public List<Offer> offers() {
        return offers;
    }

    public long nextId() {
        return nextId;
    }

    public void setNextId(long id) {
        nextId = id;
    }

    public List<Offer> offersOf(String owner) {
        List<Offer> out = new ArrayList<>();
        for (Offer o : offers) {
            if (o.owner.equals(owner)) {
                out.add(o);
            }
        }
        return out;
    }

    /** Tax taken from a seller for one item sold at {@code price}. */
    public static long taxPerItem(int price) {
        if (price < TAX_FREE_BELOW) {
            return 0;
        }
        return Math.min(TAX_CAP, (long) Math.floor(price * TAX));
    }

    /** OSRS guide price of an item: its Grand Exchange price, or its store value if the GE has none. */
    public static int guidePrice(String item, int storeValue) {
        int g = GePrices.guide(item);
        return g > 0 ? g : storeValue;
    }

    public void setBuyLimits(java.util.function.ToIntFunction<String> limits) {
        this.buyLimits = limits;
    }

    /** How many more of {@code item} {@code owner} may buy right now (Integer.MAX_VALUE if unlimited). */
    public int buyLimitRemaining(String owner, String item, long now) {
        int limit = buyLimits.applyAsInt(item);
        if (limit <= 0) {
            return Integer.MAX_VALUE;
        }
        long[] w = bought.get(owner + "|" + item);
        if (w == null || now - w[0] >= BUY_LIMIT_TICKS) {
            return limit;
        }
        return (int) Math.max(0, limit - w[1]);
    }

    /** Records a purchase against the owner's 4-hour buy limit. */
    public void recordBought(String owner, String item, int qty, long now) {
        if (buyLimits.applyAsInt(item) <= 0 || qty <= 0) {
            return;
        }
        long[] w = bought.computeIfAbsent(owner + "|" + item, k -> new long[]{now, 0});
        if (now - w[0] >= BUY_LIMIT_TICKS) {
            w[0] = now;
            w[1] = 0;
        }
        w[1] += qty;
    }

    /** Tick at which the owner's buy limit for an item resets (or {@code now} if it isn't in use). */
    public long buyLimitResets(String owner, String item, long now) {
        long[] w = bought.get(owner + "|" + item);
        return w == null || now - w[0] >= BUY_LIMIT_TICKS ? now : w[0] + BUY_LIMIT_TICKS;
    }

    public static int npcBuyPrice(int guide) {
        return Math.max(1, (int) Math.ceil(guide * (1 + NPC_MARGIN)));
    }

    public static int npcSellPrice(int guide) {
        return Math.max(1, (int) Math.floor(guide * (1 - NPC_MARGIN)));
    }

    /** Adds a new offer (escrow must already be taken) and matches it; returns the trades made. */
    public List<Trade> place(Offer offer) {
        offers.add(offer);
        nextId = Math.max(nextId, offer.id + 1);
        return match(offer);
    }

    public Offer create(String owner, String item, boolean buy, int quantity, int price, long now) {
        return new Offer(nextId++, owner, item, buy, quantity, price, now);
    }

    /** Matches an offer against the opposite side of the book (best price, then oldest). */
    public List<Trade> match(Offer offer) {
        List<Trade> trades = new ArrayList<>();
        List<Offer> candidates = new ArrayList<>();
        for (Offer o : offers) {
            if (o != offer && o.buy != offer.buy && o.item.equals(offer.item) && !o.complete() && !o.owner.equals(offer.owner)) {
                if (offer.buy ? o.price <= offer.price : o.price >= offer.price) {
                    candidates.add(o);
                }
            }
        }
        Comparator<Offer> byPrice = offer.buy ? Comparator.comparingInt(o -> o.price) : Comparator.comparingInt((Offer o) -> o.price).reversed();
        candidates.sort(byPrice.thenComparingLong(o -> o.created));
        for (Offer other : candidates) {
            if (offer.complete()) {
                break;
            }
            Offer buyer = offer.buy ? offer : other;
            Offer seller = offer.buy ? other : offer;
            long now = offer.created;
            int qty = Math.min(Math.min(offer.remaining(), other.remaining()), buyLimitRemaining(buyer.owner, buyer.item, now));
            if (qty <= 0) {
                if (offer.buy) {
                    break;
                }
                continue;
            }
            // The older offer sets the price.
            int price = other.price;
            settle(buyer, seller, qty, price);
            recordBought(buyer.owner, buyer.item, qty, now);
            trades.add(new Trade(buyer, seller, qty, price));
        }
        return trades;
    }

    /** Moves goods for a trade: buyer gets items and a refund of the price difference; seller gets coins minus tax. */
    static void settle(Offer buyer, Offer seller, int qty, int price) {
        buyer.filled += qty;
        seller.filled += qty;
        buyer.itemsToCollect += qty;
        buyer.coinsToCollect += (long) (buyer.price - price) * qty;
        seller.coinsToCollect += (long) (price - taxPerItem(price)) * qty;
    }

    /**
     * Lets the NPC market fill offers priced at or beyond the NPC margin around the guide price.
     *
     * @param guide guide price lookup (item value)
     * @return number of offers touched
     */
    public int npcFill(java.util.function.ToIntFunction<String> guide) {
        return npcFill(guide, Long.MIN_VALUE);
    }

    /**
     * As {@link #npcFill(java.util.function.ToIntFunction)}, with buy offers limited by each player's 4-hour
     * buy limit at game tick {@code now} ({@code Long.MIN_VALUE} ignores limits).
     */
    public int npcFill(java.util.function.ToIntFunction<String> guide, long now) {
        int touched = 0;
        for (Offer o : offers) {
            if (o.complete()) {
                continue;
            }
            int g = guide.applyAsInt(o.item);
            if (g <= 0) {
                continue;
            }
            if (o.buy && o.price >= npcBuyPrice(g)) {
                int qty = now == Long.MIN_VALUE ? o.remaining() : Math.min(o.remaining(), buyLimitRemaining(o.owner, o.item, now));
                if (qty <= 0) {
                    continue;
                }
                if (now != Long.MIN_VALUE) {
                    recordBought(o.owner, o.item, qty, now);
                }
                int price = npcBuyPrice(g);
                o.filled += qty;
                o.itemsToCollect += qty;
                o.coinsToCollect += (long) (o.price - price) * qty;
                touched++;
            } else if (!o.buy && o.price <= npcSellPrice(g)) {
                int qty = o.remaining();
                int price = npcSellPrice(g);
                o.filled += qty;
                o.coinsToCollect += (long) (price - taxPerItem(price)) * qty;
                touched++;
            }
        }
        return touched;
    }

    /** Aborts an offer: the unfilled part goes back to the collection box. */
    public void abort(Offer o) {
        if (o.complete()) {
            return;
        }
        int left = o.remaining();
        if (o.buy) {
            o.coinsToCollect += (long) left * o.price;
        } else {
            o.itemsToCollect += left;
        }
        o.aborted = true;
    }

    /** Removes offers that are complete and fully collected. */
    public void prune() {
        offers.removeIf(Offer::collected);
    }

    public Offer byId(long id) {
        for (Offer o : offers) {
            if (o.id == id) {
                return o;
            }
        }
        return null;
    }

    /** Latest trade prices per item (for display). */
    public static String formatGp(long gp) {
        if (gp >= 10_000_000) {
            return (gp / 1_000_000) + "M";
        }
        if (gp >= 100_000) {
            return (gp / 1000) + "K";
        }
        return String.format(java.util.Locale.ROOT, "%,d", gp);
    }
}
