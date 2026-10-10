package com.runecraft.content.systems;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.systems.GrandExchange;
import com.runecraft.osrs.content.systems.GrandExchange.Offer;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Grand Exchange: buy/sell offers matched between players (persisted), plus an NPC market that
 * trades at the guide price (item value) with a 5% margin so solo players can always trade.
 */
public final class GeService {
    /** Where (and when, in game ticks) each player last opened the exchange at a booth or clerk. */
    private static final Map<UUID, Object[]> AT_BOOTH = new HashMap<>();

    private GeService() {
    }

    public static void openAtBooth(ServerPlayerEntity p) {
        AT_BOOTH.put(p.getUuid(), new Object[]{p.getPos(), SysTicks.now()});
        open(p, null, null);
    }

    static boolean atBooth(ServerPlayerEntity p) {
        Object[] at = AT_BOOTH.get(p.getUuid());
        return at != null && SysTicks.now() - (Long) at[1] < 500 && p.getPos().squaredDistanceTo((Vec3d) at[0]) < 24 * 24;
    }

    static int guide(String item) {
        ItemDef d = ItemCatalog.get(item);
        return d == null ? 0 : d.value();
    }

    /** Resolves an item id or display name. */
    static ItemDef resolve(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim().toLowerCase(Locale.ROOT);
        ItemDef d = ItemCatalog.get(t.replace(' ', '_'));
        if (d != null) {
            return d;
        }
        for (ItemDef def : ItemCatalog.all()) {
            if (def.name().equalsIgnoreCase(t)) {
                return def;
            }
        }
        for (ItemDef def : ItemCatalog.all()) {
            if (def.name().toLowerCase(Locale.ROOT).contains(t)) {
                return def;
            }
        }
        return null;
    }

    public static void open(ServerPlayerEntity p, String item, String message) {
        SysState state = SysState.get(p.getServer());
        boolean booth = atBooth(p);
        ItemStack held = p.getMainHandStack();
        String defItem = item != null ? item : held.isEmpty() || ItemCatalog.get(Inv.idOf(held)) == null ? "" : Inv.idOf(held);
        ItemDef def = defItem.isEmpty() ? null : ItemCatalog.get(defItem);
        SysNet.Screen s = new SysNet.Screen("ge", "Grand Exchange");
        if (message != null) {
            s.line(message, 0x80FF80);
        }
        if (def != null) {
            int g = guide(def.id());
            Integer last = state.lastPrices().get(def.id());
            s.line(def.name() + " - guide price " + GrandExchange.formatGp(g) + " gp" + (last != null ? ", last traded " + GrandExchange.formatGp(last) + " gp" : "")
                    + ". Instant: buy " + GrandExchange.npcBuyPrice(g) + " / sell " + GrandExchange.npcSellPrice(g), 0xFFD700);
        }
        List<Offer> mine = state.ge().offersOf(p.getUuidAsString());
        s.line("Your offers (" + mine.size() + "/" + GrandExchange.MAX_OFFERS + "):", 0xC0C0C0);
        long collectCoins = 0;
        int collectItems = 0;
        for (Offer o : mine) {
            ItemDef od = ItemCatalog.get(o.item);
            String name = od == null ? o.item : od.name();
            String status = o.aborted ? "aborted" : o.complete() ? "complete" : o.filled + "/" + o.quantity;
            s.line("#" + o.id + " " + (o.buy ? "Buy " : "Sell ") + o.quantity + " x " + name + " @ " + GrandExchange.formatGp(o.price)
                    + " gp - " + status, o.complete() ? 0x80FF80 : 0xFFFFFF);
            collectCoins += o.coinsToCollect;
            collectItems += o.itemsToCollect;
        }
        if (collectCoins > 0 || collectItems > 0) {
            s.line("To collect: " + String.format(Locale.ROOT, "%,d", collectCoins) + " coins, " + collectItems + " items.", 0x80FF80);
        }
        if (!booth) {
            s.line("Visit a Grand Exchange booth or clerk (Varrock, 3165 3487) to place offers.", 0xFF8080);
        }
        s.field("item", "Item", def == null ? "" : def.id());
        s.field("qty", "Quantity", held.isEmpty() || def == null || !Inv.idOf(held).equals(def.id()) ? "1" : Integer.toString(held.getCount()));
        s.field("price", "Price each", def == null ? "" : Integer.toString(guide(def.id())));
        s.button("lookup", "Look up item", "Show the guide price of the item typed above.", true);
        s.button("buy", "Place buy offer", "Coins are held until the offer completes.", booth);
        s.button("sell", "Place sell offer", "Items are held until the offer completes. 2% tax.", booth);
        s.button("ibuy", "Instant buy (+5%)", "Buy from the NPC market at the guide price +5%.", booth);
        s.button("isell", "Instant sell (-5%)", "Sell to the NPC market at the guide price -5% (minus tax).", booth);
        s.button("collect", "Collect all", "", collectCoins > 0 || collectItems > 0);
        for (Offer o : mine) {
            if (!o.complete()) {
                s.button("abort:" + o.id, "Abort #" + o.id, "", true);
            }
        }
        s.send(p);
    }

    static void action(ServerPlayerEntity p, String action, Map<String, String> fields) {
        SysState state = SysState.get(p.getServer());
        GrandExchange ge = state.ge();
        String uuid = p.getUuidAsString();
        if (action.equals("collect")) {
            collect(p);
            open(p, null, "Collected.");
            return;
        }
        if (action.startsWith("abort:")) {
            try {
                Offer o = ge.byId(Long.parseLong(action.substring(6)));
                if (o != null && o.owner.equals(uuid)) {
                    ge.abort(o);
                    open(p, o.item, "Offer aborted; collect the remainder.");
                }
            } catch (NumberFormatException ignored) {
                // bad id
            }
            return;
        }
        ItemDef def = resolve(fields.get("item"));
        if (def == null) {
            open(p, null, "Unknown item: " + fields.getOrDefault("item", ""));
            return;
        }
        if (action.equals("lookup")) {
            open(p, def.id(), null);
            return;
        }
        if (!atBooth(p)) {
            open(p, def.id(), "You need to be at the Grand Exchange to trade.");
            return;
        }
        if (def.id().equals("coins")) {
            open(p, null, "You can't trade coins.");
            return;
        }
        int qty = parse(fields.get("qty"), 1);
        int price = parse(fields.get("price"), guide(def.id()));
        if (qty <= 0 || price <= 0) {
            open(p, def.id(), "Enter a quantity and price above 0.");
            return;
        }
        long total = (long) qty * price;
        switch (action) {
            case "ibuy" -> {
                int each = GrandExchange.npcBuyPrice(guide(def.id()));
                long cost = (long) each * qty;
                if (!Inv.has(p, "coins", (int) Math.min(Integer.MAX_VALUE, cost)) || cost > Integer.MAX_VALUE) {
                    open(p, def.id(), "You need " + String.format(Locale.ROOT, "%,d", cost) + " coins.");
                    return;
                }
                Inv.remove(p, "coins", (int) cost);
                Inv.give(p, def.id(), qty);
                state.lastPrices().put(def.id(), each);
                traded(p.getServer(), uuid);
                ServerNetworking.sync(p);
                open(p, def.id(), "You buy " + qty + " x " + def.name() + " for " + String.format(Locale.ROOT, "%,d", cost) + " coins.");
            }
            case "isell" -> {
                if (Inv.count(p, def.id()) < qty) {
                    open(p, def.id(), "You don't have " + qty + " x " + def.name() + ".");
                    return;
                }
                int each = GrandExchange.npcSellPrice(guide(def.id()));
                long gain = (each - GrandExchange.taxPerItem(each)) * (long) qty;
                Inv.remove(p, def.id(), qty);
                Inv.give(p, "coins", (int) Math.min(Integer.MAX_VALUE, gain));
                state.lastPrices().put(def.id(), each);
                traded(p.getServer(), uuid);
                ServerNetworking.sync(p);
                open(p, def.id(), "You sell " + qty + " x " + def.name() + " for " + String.format(Locale.ROOT, "%,d", gain) + " coins.");
            }
            case "buy", "sell" -> {
                if (ge.offersOf(uuid).stream().filter(o -> !o.complete()).count() >= GrandExchange.MAX_OFFERS) {
                    open(p, def.id(), "You can only have " + GrandExchange.MAX_OFFERS + " active offers.");
                    return;
                }
                boolean buy = action.equals("buy");
                if (buy) {
                    if (total > Integer.MAX_VALUE || !Inv.has(p, "coins", (int) total)) {
                        open(p, def.id(), "You need " + String.format(Locale.ROOT, "%,d", total) + " coins for this offer.");
                        return;
                    }
                    Inv.remove(p, "coins", (int) total);
                } else {
                    if (Inv.count(p, def.id()) < qty) {
                        open(p, def.id(), "You don't have " + qty + " x " + def.name() + ".");
                        return;
                    }
                    Inv.remove(p, def.id(), qty);
                }
                Offer offer = ge.create(uuid, def.id(), buy, qty, price, SysTicks.now());
                List<GrandExchange.Trade> trades = ge.place(offer);
                handleTrades(p.getServer(), trades);
                ge.npcFill(GeService::guide);
                if (offer.complete() && trades.isEmpty()) {
                    state.lastPrices().put(def.id(), buy ? GrandExchange.npcBuyPrice(guide(def.id())) : GrandExchange.npcSellPrice(guide(def.id())));
                    traded(p.getServer(), uuid);
                }
                ServerNetworking.sync(p);
                open(p, def.id(), offer.complete() ? "Your offer completed!" : "Offer placed.");
            }
            default -> {
            }
        }
    }

    private static int parse(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        String t = s.trim().toLowerCase(Locale.ROOT).replace(",", "");
        try {
            if (t.endsWith("k")) {
                return (int) (Double.parseDouble(t.substring(0, t.length() - 1)) * 1000);
            }
            if (t.endsWith("m")) {
                return (int) (Double.parseDouble(t.substring(0, t.length() - 1)) * 1_000_000);
            }
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static void collect(ServerPlayerEntity p) {
        SysState state = SysState.get(p.getServer());
        for (Offer o : state.ge().offersOf(p.getUuidAsString())) {
            if (o.itemsToCollect > 0) {
                Inv.give(p, o.item, o.itemsToCollect);
                o.itemsToCollect = 0;
            }
            if (o.coinsToCollect > 0) {
                Inv.give(p, "coins", (int) Math.min(Integer.MAX_VALUE, o.coinsToCollect));
                o.coinsToCollect = 0;
            }
        }
        state.ge().prune();
        ServerNetworking.sync(p);
    }

    static void handleTrades(MinecraftServer server, List<GrandExchange.Trade> trades) {
        SysState state = SysState.get(server);
        for (GrandExchange.Trade t : trades) {
            state.lastPrices().put(t.buyer().item, t.price());
            traded(server, t.buyer().owner);
            traded(server, t.seller().owner);
            notify(server, t.buyer());
            notify(server, t.seller());
        }
    }

    private static void traded(MinecraftServer server, String owner) {
        try {
            UUID u = UUID.fromString(owner);
            SysState.get(server).player(u).geTrades++;
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(u);
            if (p != null) {
                DiaryService.onGeTrade(p);
            }
        } catch (IllegalArgumentException ignored) {
            // not a player
        }
    }

    private static void notify(MinecraftServer server, Offer o) {
        try {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(UUID.fromString(o.owner));
            if (p != null) {
                ItemDef d = ItemCatalog.get(o.item);
                p.sendMessage(Text.literal("Grand Exchange: " + (o.complete() ? "Finished " : "Updated ") + (o.buy ? "buying " : "selling ")
                        + o.filled + "/" + o.quantity + " x " + (d == null ? o.item : d.name()) + ".").formatted(Formatting.GREEN), false);
            }
        } catch (IllegalArgumentException ignored) {
            // not a player
        }
    }

    /** Every minute the NPC market fills offers priced beyond its margin. */
    public static void minute(MinecraftServer server) {
        SysState state = SysState.get(server);
        int before = (int) state.ge().offers().stream().filter(Offer::complete).count();
        state.ge().npcFill(GeService::guide);
        int after = (int) state.ge().offers().stream().filter(Offer::complete).count();
        if (after > before) {
            for (Offer o : state.ge().offers()) {
                if (o.complete() && !o.aborted && (o.itemsToCollect > 0 || o.coinsToCollect > 0)) {
                    notify(server, o);
                }
            }
        }
    }

    static void msg(ServerPlayerEntity p, String m) {
        Msg.game(p, m);
    }
}
