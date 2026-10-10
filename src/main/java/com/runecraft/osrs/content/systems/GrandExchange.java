package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Grand Exchange order book (pure logic). Buy and sell offers are matched like OSRS: the offer that
 * was placed first sets the price, buyers get refunded the difference. Sellers pay a 2% tax (items under
 * 50 gp are exempt; capped at 5M per item). An NPC market fills offers at the guide price with a margin
 * so solo players can trade.
 */
public final class GrandExchange {
    public static final int MAX_OFFERS = 8;
    public static final double TAX = 0.02;
    public static final int TAX_FREE_BELOW = 50;
    public static final long TAX_CAP = 5_000_000;
    /** NPC market margin: buys at guide +5%, sells at guide -5%. */
    public static final double NPC_MARGIN = 0.05;

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
            int qty = Math.min(offer.remaining(), other.remaining());
            // The older offer sets the price.
            int price = other.price;
            Offer buyer = offer.buy ? offer : other;
            Offer seller = offer.buy ? other : offer;
            settle(buyer, seller, qty, price);
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
                int qty = o.remaining();
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
