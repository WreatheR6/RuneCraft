package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.skilling.BlastFurnaceData;
import com.runecraft.osrs.content.skilling.BlastFurnaceData.Furnace;
import com.runecraft.osrs.data.Bar;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Blast Furnace: Keldagrim stairs down to the furnace room, the conveyor belt (deposit ore; the
 * dwarves smelt it straight away with half the coal), the bar dispenser and the coffer. Each
 * player's furnace (ore, bars, coffer, Foreman permit) is saved in {@link SkillingState} under
 * "blast_furnace".
 */
final class BlastFurnaceHooks {
    private static final String SECTION = "blast_furnace";
    /** How often (game ticks) the coffer is charged for players in the furnace room. */
    private static final int DRAIN_INTERVAL = 10;
    private static final int ROOM_RADIUS = 24;

    private BlastFurnaceHooks() {
    }

    static void init() {
        BlockHooks.register("blast_furnace_stairs", (player, pos, state) -> travel(player, BlastFurnaceData.FURNACE_X,
                BlastFurnaceData.FURNACE_Y, "You climb down the stairs to the Blast Furnace."));
        BlockHooks.register("blast_furnace_exit", (player, pos, state) -> {
            PlaceDef keldagrim = place("keldagrim_stalls");
            if (keldagrim != null) {
                travel(player, keldagrim.osrsX(), keldagrim.osrsY() + 3, "You climb back up to Keldagrim.");
            }
        });
        BlockHooks.register("blast_furnace_conveyor", (player, pos, state) -> conveyor(player));
        BlockHooks.register("blast_furnace_dispenser", (player, pos, state) -> dispenser(player));
        BlockHooks.register("blast_furnace_coffer", (player, pos, state) -> coffer(player));
    }

    /** Charges the coffer of every player standing in the furnace room (called each game tick). */
    static void tick(ServerPlayer player, long gameTick) {
        if (gameTick % DRAIN_INTERVAL != 0 || !inRoom(player)) {
            return;
        }
        Furnace f = load(player);
        if (f.coffer <= 0) {
            return;
        }
        if (!BlastFurnaceData.drain(f, DRAIN_INTERVAL)) {
            Msg.red(player, "The coffer is empty. The dwarves stop working until it is refilled.");
        }
        save(player, f);
    }

    private static boolean inRoom(ServerPlayer player) {
        if (!Gielinor.is(player)) {
            return false;
        }
        int x = GielinorMap.toOsrsX(player.blockPosition().getX());
        int y = GielinorMap.toOsrsY(player.blockPosition().getZ());
        return Math.abs(x - BlastFurnaceData.FURNACE_X) <= ROOM_RADIUS && Math.abs(y - BlastFurnaceData.FURNACE_Y) <= ROOM_RADIUS;
    }

    private static void conveyor(ServerPlayer player) {
        Furnace f = load(player);
        int smithing = SkillingUtil.level(player, Skill.SMITHING);
        long now = ActionManager.gameTick();
        if (BlastFurnaceData.needsFee(smithing, f, now)) {
            if (!Inv.has(player, "coins", BlastFurnaceData.FOREMAN_FEE)) {
                Msg.game(player, "The Foreman wants " + BlastFurnaceData.FOREMAN_FEE
                        + " coins for ten minutes on his furnace (free from 60 Smithing).");
                return;
            }
            Inv.remove(player, "coins", BlastFurnaceData.FOREMAN_FEE);
            f.permitUntil = now + BlastFurnaceData.FOREMAN_TICKS;
            Msg.game(player, "You pay the Foreman " + BlastFurnaceData.FOREMAN_FEE + " coins for ten minutes on the furnace.");
        }
        if (f.coffer <= 0) {
            Msg.game(player, "The dwarves won't work for free. Put some coins in the coffer first.");
            save(player, f);
            return;
        }
        int deposited = 0;
        for (String ore : List.of("copper_ore", "tin_ore", "iron_ore", "silver_ore", "gold_ore", "mithril_ore", "adamantite_ore",
                "runite_ore", "coal")) {
            int carried = Inv.count(player, ore);
            int in = BlastFurnaceData.deposit(f, ore, carried);
            if (in > 0) {
                Inv.remove(player, ore, in);
                deposited += in;
            }
        }
        if (deposited == 0) {
            Msg.game(player, "You have no ore the furnace can take (it holds 28 of each ore and 254 coal).");
            save(player, f);
            return;
        }
        Msg.action(player, "You put " + deposited + " ore on the conveyor belt.");
        player.level().playSound(null, player.blockPosition(), SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
        for (BlastFurnaceData.Smelted s : BlastFurnaceData.smelt(f, smithing)) {
            XpService.add(player, Skill.SMITHING, s.xp());
            Msg.game(player, "The dwarves smelt " + s.count() + " " + s.bar().displayName().toLowerCase() + (s.count() > 1 ? "s" : "")
                    + ". Collect them from the bar dispenser.");
        }
        save(player, f);
        ServerNetworking.sync(player);
    }

    private static void dispenser(ServerPlayer player) {
        Furnace f = load(player);
        if (f.bars.isEmpty()) {
            Msg.game(player, "The bar dispenser is empty.");
            return;
        }
        int total = 0;
        for (Bar bar : Bar.values()) {
            int n = f.bars(bar.itemId());
            if (n <= 0) {
                continue;
            }
            int room = roomFor(player, bar.itemId(), n);
            int got = BlastFurnaceData.collect(f, bar.itemId(), room);
            if (got > 0) {
                Inv.give(player, bar.itemId(), got);
                com.runecraft.collection.CollectionLog.obtain(player, bar.itemId());
                total += got;
            }
        }
        save(player, f);
        if (total == 0) {
            Msg.game(player, "You don't have enough inventory space.");
            return;
        }
        Msg.action(player, "You take " + total + " bar" + (total > 1 ? "s" : "") + " from the dispenser.");
        ServerNetworking.sync(player);
    }

    /** Bars don't stack: one free inventory slot per bar. */
    private static int roomFor(ServerPlayer player, String item, int wanted) {
        return Inv.hasRoomFor(player, item) ? Math.min(wanted, Math.max(1, freeSlots(player))) : 0;
    }

    private static int freeSlots(ServerPlayer player) {
        int free = 0;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    private static void coffer(ServerPlayer player) {
        Furnace f = load(player);
        int coins = Inv.count(player, "coins");
        int amount = Math.min(coins, BlastFurnaceData.COFFER_PER_HOUR);
        if (amount <= 0) {
            Msg.game(player, "The coffer holds " + f.coffer + " coins. It pays the workers "
                    + BlastFurnaceData.COFFER_PER_HOUR + " coins an hour.");
            return;
        }
        Inv.remove(player, "coins", amount);
        f.coffer += amount;
        save(player, f);
        Msg.game(player, "You put " + amount + " coins in the coffer. It now holds " + f.coffer + " coins.");
        ServerNetworking.sync(player);
    }

    private static void travel(ServerPlayer player, int osrsX, int osrsY, String message) {
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        PlaceDef target = nearestPlace(osrsX, osrsY);
        if (target != null) {
            LandmarkBuilder.buildNow(world, target);
        }
        BlockPos pos = Gielinor.safeSpot(world, GielinorMap.toBlockX(osrsX) + 2, GielinorMap.toBlockZ(osrsY) + 2);
        player.stopRiding();
        player.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        Msg.game(player, message);
    }

    private static PlaceDef nearestPlace(int osrsX, int osrsY) {
        for (PlaceDef p : ContentRegistry.places()) {
            if (Math.abs(p.osrsX() - osrsX) <= 4 && Math.abs(p.osrsY() - osrsY) <= 4) {
                return p;
            }
        }
        return null;
    }

    private static PlaceDef place(String id) {
        for (PlaceDef p : ContentRegistry.places()) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private static Furnace load(ServerPlayer player) {
        CompoundTag nbt = SkillingState.get(player.getServer()).entry(SECTION, player.getStringUUID());
        Furnace f = new Furnace();
        CompoundTag ore = nbt.getCompound("ore");
        for (String k : ore.getAllKeys()) {
            f.ore.put(k, ore.getInt(k));
        }
        CompoundTag bars = nbt.getCompound("bars");
        for (String k : bars.getAllKeys()) {
            f.bars.put(k, bars.getInt(k));
        }
        f.coffer = nbt.getInt("coffer");
        f.permitUntil = nbt.getLong("permit");
        return f;
    }

    private static void save(ServerPlayer player, Furnace f) {
        CompoundTag nbt = SkillingState.get(player.getServer()).entry(SECTION, player.getStringUUID());
        CompoundTag ore = new CompoundTag();
        for (Map.Entry<String, Integer> e : f.ore.entrySet()) {
            ore.putInt(e.getKey(), e.getValue());
        }
        CompoundTag bars = new CompoundTag();
        for (Map.Entry<String, Integer> e : f.bars.entrySet()) {
            bars.putInt(e.getKey(), e.getValue());
        }
        nbt.put("ore", ore);
        nbt.put("bars", bars);
        nbt.putInt("coffer", f.coffer);
        nbt.putLong("permit", f.permitUntil);
    }
}
