package com.runecraft.content.npcs;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.npcs.ShopDef;
import com.runecraft.osrs.content.npcs.ShopStock;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Live NPC shops: one shared stock per shop (as in OSRS), restocking every game tick, plus the
 * buy/sell handling for the shop screen. Players must stay near the shopkeeper while trading.
 */
public final class ShopService {
    private static final double MAX_DISTANCE_SQ = 12 * 12;
    private static final Map<String, ShopStock> STOCK = new HashMap<>();
    private static final Map<UUID, Session> OPEN = new HashMap<>();
    private static int mcTicks;

    private record Session(String shopId, Vec3d where) {
    }

    /** A sellable stack summary for the screen. */
    private record Offer(String item, String name, int count, int price) {
    }

    private ShopService() {
    }

    static ShopStock stock(String shopId) {
        ShopDef def = Shops.get(shopId);
        return def == null ? null : STOCK.computeIfAbsent(shopId, k -> new ShopStock(def));
    }

    /** Opens a shop for a player standing at {@code where} (the shopkeeper's position). */
    public static void open(ServerPlayerEntity player, String shopId, Vec3d where) {
        if (stock(shopId) == null) {
            return;
        }
        ActionManager.cancel(player);
        OPEN.put(player.getUuid(), new Session(shopId, where));
        send(player, shopId, true);
    }

    static void close(ServerPlayerEntity player, String shopId) {
        Session s = OPEN.get(player.getUuid());
        if (s != null && s.shopId().equals(shopId)) {
            OPEN.remove(player.getUuid());
        }
    }

    static void forget(UUID player) {
        OPEN.remove(player);
    }

    private static boolean near(ServerPlayerEntity player, String shopId) {
        Session s = OPEN.get(player.getUuid());
        if (s == null || !s.shopId().equals(shopId)) {
            return false;
        }
        if (player.getPos().squaredDistanceTo(s.where()) > MAX_DISTANCE_SQ) {
            Msg.game(player, "You are too far away from the shopkeeper.");
            OPEN.remove(player.getUuid());
            return false;
        }
        return true;
    }

    private static String icon(String item) {
        return RuneCraft.resolve(item).toString();
    }

    static String name(String item) {
        ItemDef def = ItemCatalog.get(item);
        if (def != null) {
            return def.name();
        }
        ItemStack stack = Inv.stack(item, 1);
        return stack.isEmpty() ? item : stack.getName().getString();
    }

    private static String currencyName(ShopDef def) {
        return def.currency().equals(Inv.COINS) ? "coins" : name(def.currency());
    }

    /** Value of a stack the player might sell: catalog value, or the shop's own value for vanilla items. */
    private static int valueOf(ShopStock stock, ItemStack stack, String id) {
        int shopValue = stock.value(id);
        if (shopValue > 0) {
            return shopValue;
        }
        ItemDef def = ModItems.def(stack);
        return def == null ? 0 : def.value();
    }

    private static List<Offer> offers(ServerPlayerEntity player, ShopStock stock) {
        Map<String, Offer> map = new LinkedHashMap<>();
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack s = player.getInventory().main.get(i);
            boolean note = com.runecraft.bank.Notes.isNote(s) && com.runecraft.bank.Notes.notedTag(s).isEmpty();
            if (s.isEmpty() || (s.hasNbt() && !note)) {
                continue;
            }
            // Shops buy bank notes as the item they stand for.
            String id = note ? com.runecraft.bank.Notes.notedId(s) : Inv.idOf(s);
            int amount = s.getCount();
            if (note) {
                s = Inv.stack(id, 1);
                if (s.isEmpty()) {
                    continue;
                }
            }
            int value = valueOf(stock, s, id);
            if (!stock.accepts(id, value)) {
                continue;
            }
            Offer prev = map.get(id);
            int count = (prev == null ? 0 : prev.count()) + amount;
            map.put(id, new Offer(id, name(id), count, stock.priceToSell(id, value)));
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Packet: open flag, shop id, title, currency name, balance, general flag, stock lines (icon,
     * item id, name, count, price) and sell offers (icon, item id, name, count, price each).
     */
    static void send(ServerPlayerEntity player, String shopId, boolean open) {
        ShopStock stock = stock(shopId);
        if (stock == null) {
            return;
        }
        ShopDef def = stock.shop();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(open);
        buf.writeString(def.id());
        buf.writeString(def.name());
        buf.writeString(currencyName(def));
        buf.writeVarInt(Inv.count(player, def.currency()));
        buf.writeBoolean(def.general());
        List<ShopStock.Line> lines = stock.lines();
        buf.writeVarInt(lines.size());
        for (ShopStock.Line l : lines) {
            buf.writeString(icon(l.item()));
            buf.writeString(l.item());
            buf.writeString(name(l.item()));
            buf.writeVarInt(l.count());
            buf.writeVarInt(l.count() > 0 ? stock.priceToBuy(l.item()) : 0);
        }
        List<Offer> offers = offers(player, stock);
        buf.writeVarInt(offers.size());
        for (Offer o : offers) {
            buf.writeString(icon(o.item()));
            buf.writeString(o.item());
            buf.writeString(o.name());
            buf.writeVarInt(o.count());
            buf.writeVarInt(o.price());
        }
        ServerPlayNetworking.send(player, NpcsNet.SHOP, buf);
    }

    static void buy(ServerPlayerEntity player, String shopId, String item, int quantity) {
        ShopStock stock = stock(shopId);
        if (stock == null || !near(player, shopId)) {
            return;
        }
        ShopDef def = stock.shop();
        int wanted = Math.max(1, Math.min(quantity, 1000));
        if (stock.count(item) <= 0) {
            Msg.game(player, "The shop has run out of stock.");
            send(player, shopId, false);
            return;
        }
        long balance = Inv.count(player, def.currency());
        long[] result = stock.buy(item, wanted, balance);
        int bought = (int) result[0];
        if (bought <= 0) {
            Msg.game(player, "You don't have enough " + currencyName(def) + ".");
            send(player, shopId, false);
            return;
        }
        Inv.remove(player, def.currency(), (int) result[1]);
        Inv.give(player, item, bought);
        if (bought < wanted) {
            Msg.game(player, stock.count(item) <= 0 ? "The shop has run out of stock." : "You don't have enough " + currencyName(def) + ".");
        }
        Msg.action(player, "You buy " + bought + " x " + name(item) + " for " + String.format("%,d", result[1]) + " " + currencyName(def) + ".");
        ServerNetworking.sync(player);
        refresh(shopId);
    }

    static void sell(ServerPlayerEntity player, String shopId, String item, int quantity) {
        ShopStock stock = stock(shopId);
        if (stock == null || !near(player, shopId)) {
            return;
        }
        ShopDef def = stock.shop();
        ItemStack sample = Inv.stack(item, 1);
        int value = sample.isEmpty() ? 0 : valueOf(stock, sample, item);
        if (!stock.accepts(item, value)) {
            Msg.game(player, "You can't sell this item to this shop.");
            return;
        }
        int have = com.runecraft.bank.Notes.countWithNotes(player, item);
        int n = Math.min(have, Math.max(1, Math.min(quantity, 10_000)));
        if (n <= 0) {
            return;
        }
        int removed = com.runecraft.bank.Notes.removeWithNotes(player, item, n);
        long paid = stock.sell(item, removed, value);
        if (paid > 0) {
            Inv.give(player, def.currency(), (int) Math.min(Integer.MAX_VALUE, paid));
        }
        Msg.action(player, "You sell " + removed + " x " + name(item) + " for " + String.format("%,d", paid) + " " + currencyName(def) + ".");
        ServerNetworking.sync(player);
        refresh(shopId);
    }

    /** Re-sends a shop to everyone who has it open (stock is shared). */
    private static void refresh(String shopId) {
        if (OPEN.isEmpty()) {
            return;
        }
        for (ServerPlayerEntity p : players()) {
            Session s = OPEN.get(p.getUuid());
            if (s != null && s.shopId().equals(shopId)) {
                send(p, shopId, false);
            }
        }
    }

    private static MinecraftServer server;

    private static List<ServerPlayerEntity> players() {
        return server == null ? List.of() : server.getPlayerManager().getPlayerList();
    }

    /** Called every server tick: restocks shops once per game tick (0.6s). */
    static void tick(MinecraftServer srv) {
        server = srv;
        if (++mcTicks < ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        mcTicks = 0;
        for (ShopStock s : STOCK.values()) {
            if (s.tick()) {
                refresh(s.shop().id());
            }
        }
    }
}
