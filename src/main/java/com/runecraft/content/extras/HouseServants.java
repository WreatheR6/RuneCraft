package com.runecraft.content.extras;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.bank.BankModel;
import com.runecraft.osrs.content.extras.HouseLayout;
import com.runecraft.osrs.content.extras.Poh;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * House servants (Rick, the maid, cook, butler and demon butler): hired with the bell-pull once the house has two beds, they
 * fetch building materials from the bank and take logs to the sawmill, taking longer per trip the cheaper they are, and ask
 * for their wage every eight errands (OSRS).
 */
final class HouseServants {
    /** A servant on an errand: who it is for and what they bring back when the trip ends (server ticks). */
    private record Errand(UUID player, String item, int count, long doneAt) {
    }

    private static final List<Errand> ERRANDS = new ArrayList<>();

    private HouseServants() {
    }

    static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!ERRANDS.isEmpty()) {
                deliver(server);
            }
        });
    }

    private static void deliver(MinecraftServer server) {
        Iterator<Errand> it = ERRANDS.iterator();
        while (it.hasNext()) {
            Errand e = it.next();
            if (server.getTickCount() < e.doneAt()) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(e.player());
            if (player == null) {
                continue; // offline: the servant waits until they're back
            }
            it.remove();
            Inv.give(player, e.item(), e.count());
            ServerNetworking.sync(player);
            Poh.Servant s = servantOf(server, e.player());
            Msg.action(player, (s == null ? "Your servant" : "Your " + lower(s.name())) + " returns with " + e.count() + " x "
                    + ExtrasUtil.itemName(e.item()) + ".");
        }
    }

    private static String lower(String s) {
        return s.equals("Rick") ? s : s.toLowerCase(Locale.ROOT);
    }

    private static CompoundTag house(MinecraftServer server, UUID owner) {
        return ExtrasState.get(server).entry("houses", owner.toString());
    }

    static Poh.Servant servantOf(MinecraftServer server, UUID owner) {
        return Poh.servant(house(server, owner).getString("servant"));
    }

    /** The player's own house spot if they are in it, else a message and null. */
    private static HouseService.Spot inOwnHouse(ServerPlayer player) {
        HouseService.Spot here = HouseService.locate((ServerLevel) player.level(), player.blockPosition());
        if (here == null || !here.owner().equals(player.getUUID())) {
            Msg.game(player, "You need to be in your own house to call your servant.");
            return null;
        }
        return here;
    }

    private static boolean busy(ServerPlayer player) {
        for (Errand e : ERRANDS) {
            if (e.player().equals(player.getUUID())) {
                Msg.game(player, "Your servant is still out on an errand.");
                return true;
            }
        }
        return false;
    }

    /** The bell-pull: hire a servant, or give yours an errand. */
    static void ring(ServerPlayer player) {
        HouseService.Spot here = inOwnHouse(player);
        if (here == null) {
            return;
        }
        Poh.Servant s = servantOf(player.getServer(), player.getUUID());
        List<MutableComponent> links = new ArrayList<>();
        if (s == null) {
            int level = ExtrasUtil.level(player, Skill.CONSTRUCTION);
            int beds = here.layout().count(Poh.Effect.BED);
            for (Poh.Servant o : Poh.SERVANTS) {
                boolean ok = Poh.canHire(o, level, beds) == null;
                links.add(ExtrasUtil.link(o.name() + " (" + o.level() + ")", "/house servant hire " + o.id(), o.name() + "\nLevel "
                        + o.level() + " Construction\nWage " + ExtrasUtil.coins(o.wage()) + " coins every " + Poh.WAGE_TRIPS
                        + " errands\nCarries " + o.capacity() + " items" + (o.sawmill() ? ", goes to the sawmill" : ""),
                        ok ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            }
            ExtrasUtil.options(player, "Hire a servant:", links);
            return;
        }
        for (String item : Poh.FETCHABLE) {
            links.add(ExtrasUtil.link(ExtrasUtil.itemName(item), "/house servant fetch " + item, "Fetch up to " + s.capacity() + " "
                    + ExtrasUtil.itemName(item) + " from your bank", ChatFormatting.AQUA));
        }
        if (s.sawmill()) {
            links.add(ExtrasUtil.link("Sawmill", "/house servant sawmill", "Take the logs you carry to the sawmill (you pay the fee)",
                    ChatFormatting.YELLOW));
        }
        links.add(ExtrasUtil.link("Dismiss", "/house servant dismiss", "Send your servant away", ChatFormatting.RED));
        ExtrasUtil.options(player, s.name() + ":", links);
    }

    static void hire(ServerPlayer player, String id) {
        HouseService.Spot here = inOwnHouse(player);
        if (here == null) {
            return;
        }
        if (servantOf(player.getServer(), player.getUUID()) != null) {
            Msg.game(player, "You already have a servant.");
            return;
        }
        Poh.Servant s = Poh.servant(id);
        HouseLayout layout = here.layout();
        String why = Poh.canHire(s, ExtrasUtil.level(player, Skill.CONSTRUCTION), layout.count(Poh.Effect.BED));
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        if (!Inv.has(player, Inv.COINS, s.wage())) {
            Msg.game(player, "You need " + ExtrasUtil.coins(s.wage()) + " coins for your servant's first wage.");
            return;
        }
        Inv.remove(player, Inv.COINS, s.wage());
        CompoundTag h = house(player.getServer(), player.getUUID());
        h.putString("servant", s.id());
        h.putInt("servant_trips", 0);
        ServerNetworking.sync(player);
        Msg.action(player, s.name() + " moves into your house.");
    }

    static void dismiss(ServerPlayer player) {
        if (inOwnHouse(player) == null) {
            return;
        }
        house(player.getServer(), player.getUUID()).remove("servant");
        Msg.game(player, "You dismiss your servant.");
    }

    /** Counts an errand; every {@link Poh#WAGE_TRIPS} errands the wage is due. Returns false if the servant quit. */
    private static boolean payDay(ServerPlayer player, Poh.Servant s) {
        CompoundTag h = house(player.getServer(), player.getUUID());
        int trips = h.getInt("servant_trips") + 1;
        h.putInt("servant_trips", trips);
        if (trips % Poh.WAGE_TRIPS != 0) {
            return true;
        }
        if (!Inv.has(player, Inv.COINS, s.wage())) {
            h.remove("servant");
            Msg.red(player, "You can't pay your servant's wage of " + ExtrasUtil.coins(s.wage()) + " coins, so they leave.");
            return false;
        }
        Inv.remove(player, Inv.COINS, s.wage());
        Msg.game(player, "You pay your servant " + ExtrasUtil.coins(s.wage()) + " coins.");
        return true;
    }

    static void fetch(ServerPlayer player, String item) {
        if (inOwnHouse(player) == null || busy(player)) {
            return;
        }
        Poh.Servant s = servantOf(player.getServer(), player.getUUID());
        if (s == null) {
            Msg.game(player, "You don't have a servant.");
            return;
        }
        if (!Poh.FETCHABLE.contains(item)) {
            Msg.game(player, "Your servant can't fetch that.");
            return;
        }
        BankModel bank = Players.data(player).bank();
        long have = bank.count(item, "");
        if (have <= 0) {
            Msg.game(player, "You don't have any " + ExtrasUtil.itemName(item) + " in your bank.");
            return;
        }
        if (!payDay(player, s)) {
            return;
        }
        int n = (int) bank.withdraw(item, "", s.capacity());
        ERRANDS.add(new Errand(player.getUUID(), item, n, player.getServer().getTickCount() + s.tripTicks() * 12L));
        ServerNetworking.sync(player);
        Msg.game(player, s.name() + " heads off to the bank for " + n + " x " + ExtrasUtil.itemName(item) + ".");
    }

    static void sawmill(ServerPlayer player) {
        if (inOwnHouse(player) == null || busy(player)) {
            return;
        }
        Poh.Servant s = servantOf(player.getServer(), player.getUUID());
        if (s == null || !s.sawmill()) {
            Msg.game(player, s == null ? "You don't have a servant." : s.name() + " won't go to the sawmill.");
            return;
        }
        for (Poh.Sawmill mill : Poh.SAWMILL) {
            int logs = Math.min(s.capacity(), Inv.count(player, mill.logs()));
            if (logs <= 0) {
                continue;
            }
            int fee = logs * mill.fee();
            if (!Inv.has(player, Inv.COINS, fee)) {
                Msg.game(player, "You need " + ExtrasUtil.coins(fee) + " coins for the sawmill.");
                return;
            }
            if (!payDay(player, s)) {
                return;
            }
            Inv.remove(player, mill.logs(), logs);
            Inv.remove(player, Inv.COINS, fee);
            ERRANDS.add(new Errand(player.getUUID(), mill.plank(), logs, player.getServer().getTickCount() + s.tripTicks() * 12L));
            ServerNetworking.sync(player);
            Msg.game(player, s.name() + " takes " + logs + " x " + ExtrasUtil.itemName(mill.logs()) + " to the sawmill.");
            return;
        }
        Msg.game(player, "You aren't carrying any logs for the sawmill.");
    }
}
