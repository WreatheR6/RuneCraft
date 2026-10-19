package com.runecraft.bank;

import com.runecraft.RuneCraft;
import com.runecraft.content.BlockHooks;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.bank.BankModel;
import com.runecraft.osrs.bank.BankQuantity;
import com.runecraft.osrs.bank.DepositBoxes;
import com.runecraft.osrs.content.systems.GrandExchange;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The OSRS bank, server side: an 800-slot bank with unlimited stacks, tabs, placeholders, withdraw-as-note,
 * 1/5/10/X/All quantities, deposit inventory / worn items, plus deposit boxes (deposit only). The client screen
 * only sends requests; every change happens here and the new state is sent back.
 */
public final class BankService {
    /** Server -> client: the bank state (and open the screen). */
    public static final ResourceLocation STATE = RuneCraft.id("bank/state");
    /** Client -> server: a bank request (see OP_*). */
    public static final ResourceLocation ACTION = RuneCraft.id("bank/action");

    public static final int OP_WITHDRAW = 0;
    public static final int OP_DEPOSIT = 1;
    public static final int OP_DEPOSIT_INVENTORY = 2;
    public static final int OP_DEPOSIT_WORN = 3;
    public static final int OP_SET_NOTED = 4;
    public static final int OP_SET_PLACEHOLDERS = 5;
    public static final int OP_SET_QUANTITY = 6;
    public static final int OP_MOVE_TAB = 7;
    public static final int OP_SWAP = 8;
    public static final int OP_RELEASE_PLACEHOLDERS = 9;
    public static final int OP_CLOSE = 10;
    public static final int OP_DEPOSIT_POUCH = 11;

    /** How far (blocks) a player may walk from where they opened the bank before it closes. */
    private static final double MAX_DISTANCE_SQ = 8 * 8;

    private record Session(Vec3 where, boolean depositOnly) {
    }

    private static final Map<UUID, Session> OPEN = new HashMap<>();

    private BankService() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, sender) -> {
            int op = buf.readVarInt();
            String key = buf.readUtf(1024);
            String key2 = buf.readUtf(1024);
            int slot = buf.readVarInt();
            int mode = buf.readVarInt();
            long x = buf.readVarLong();
            int tab = buf.readVarInt();
            server.execute(() -> {
                try {
                    action(player, op, key, key2, slot, BankQuantity.Mode.byOrdinal(mode), x, tab);
                } catch (RuntimeException e) {
                    RuneCraft.LOGGER.error("Bank action {} failed", op, e);
                }
            });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> OPEN.remove(handler.getPlayer().getUUID()));
        BlockHooks.register(DepositBoxes.BLOCK, (player, pos, state) -> openDepositBox(player));
        for (DepositBoxes.Spot spot : DepositBoxes.SPOTS) {
            LandmarkBuilder.registerCustom(spot.id(), BankService::placeDepositBox);
        }
    }

    // ------------------------------------------------------------------ opening

    /** Opens the bank (booth or banker). Holding bank notes exchanges them for items instead, as in OSRS. */
    public static void open(ServerPlayer player) {
        if (Notes.isNote(player.getMainHandItem())) {
            unnote(player);
            return;
        }
        bankLootingBag(player);
        OPEN.put(player.getUUID(), new Session(player.position(), false));
        send(player, true);
    }

    public static void openDepositBox(ServerPlayer player) {
        bankLootingBag(player);
        OPEN.put(player.getUUID(), new Session(player.position(), true));
        send(player, true);
    }

    /** Holding the looting bag when the bank opens banks everything in it (OSRS: the bank's looting bag "deposit all"). */
    private static void bankLootingBag(ServerPlayer player) {
        if (com.runecraft.osrs.content.systems.LootingBag.ID.equals(Inv.idOf(player.getMainHandItem()))) {
            com.runecraft.content.systems.LootingBagService.bankAll(player);
        }
    }

    /** Exchanges the notes in hand for items, one per free inventory slot (or a stack per slot for stacking items). */
    static void unnote(ServerPlayer player) {
        ItemStack notes = player.getMainHandItem();
        ItemStack proto = Notes.unnoted(notes, 1);
        if (proto.isEmpty()) {
            Msg.game(player, "This note is worthless.");
            notes.setCount(0);
            return;
        }
        int have = notes.getCount();
        int roomWith = Notes.room(player, proto);
        notes.setCount(0);
        int roomWithout = Notes.room(player, proto);
        // Exchanging every note frees the notes' own slot; otherwise only the free space counts.
        int n = roomWithout >= have ? have : Math.min(have, roomWith);
        notes.setCount(have - n);
        if (n <= 0) {
            Msg.game(player, "You don't have enough inventory space.");
            return;
        }
        int put = Notes.insert(player, proto, n);
        if (put < n) {
            Inv.give(player, proto.copyWithCount(n - put));
        }
        Msg.action(player, "The banker exchanges your " + (n == 1 ? "note" : n + " notes") + " for " + proto.getHoverName().getString() + ".");
    }

    private static Session session(ServerPlayer player) {
        Session s = OPEN.get(player.getUUID());
        if (s == null) {
            return null;
        }
        if (player.position().distanceToSqr(s.where()) > MAX_DISTANCE_SQ) {
            OPEN.remove(player.getUUID());
            Msg.game(player, "You are too far away from the bank.");
            return null;
        }
        return s;
    }

    // ------------------------------------------------------------------ requests

    /** Self-test hook: runs one bank request as if the client had sent it (the bank must be open). */
    public static void selfTestAction(ServerPlayer player, int op, String key, int slot, BankQuantity.Mode mode, long x) {
        action(player, op, key, "", slot, mode, x, 0);
    }

    static void action(ServerPlayer player, int op, String key, String key2, int slot, BankQuantity.Mode mode, long x, int tab) {
        if (op == OP_CLOSE) {
            OPEN.remove(player.getUUID());
            return;
        }
        Session s = session(player);
        if (s == null) {
            return;
        }
        BankModel bank = Players.data(player).bank();
        boolean changed = true;
        switch (op) {
            case OP_WITHDRAW -> {
                if (s.depositOnly()) {
                    return;
                }
                withdraw(player, bank, key, mode, x);
            }
            case OP_DEPOSIT -> depositSlot(player, bank, slot, mode, x, tab);
            case OP_DEPOSIT_POUCH -> depositPouch(player, bank, mode, x, tab);
            case OP_DEPOSIT_INVENTORY -> depositInventory(player, bank, tab);
            case OP_DEPOSIT_WORN -> depositWorn(player, bank, tab);
            case OP_SET_NOTED -> bank.noted = slot != 0;
            case OP_SET_PLACEHOLDERS -> bank.placeholders = slot != 0;
            case OP_SET_QUANTITY -> {
                bank.quantity = mode;
                if (x > 0) {
                    bank.x = Math.min(x, BankQuantity.MAX);
                }
            }
            case OP_MOVE_TAB -> changed = !s.depositOnly() && bank.moveToTab(key, tab);
            case OP_SWAP -> changed = !s.depositOnly() && bank.swap(key, key2);
            case OP_RELEASE_PLACEHOLDERS -> {
                int n = bank.releasePlaceholders();
                Msg.game(player, n == 0 ? "You have no placeholders to release." : "Released " + n + " placeholder" + (n == 1 ? "." : "s."));
            }
            default -> changed = false;
        }
        if (changed) {
            player.getInventory().setChanged();
            ServerNetworking.sync(player);
        }
        send(player, false);
    }

    static void withdraw(ServerPlayer player, BankModel bank, String key, BankQuantity.Mode mode, long x) {
        BankModel.Entry e = bank.byKey(key);
        if (e == null || e.count <= 0) {
            return;
        }
        long want = BankQuantity.amount(mode, x, e.count);
        if (want <= 0) {
            return;
        }
        if (e.item.equals(Inv.COINS)) {
            PlayerData data = Players.data(player);
            long n = Math.min(want, Integer.MAX_VALUE - data.coins());
            if (n <= 0) {
                Msg.game(player, "You can't carry any more coins.");
                return;
            }
            data.setCoins(data.coins() + bank.withdraw(e.item, e.tag, n));
            return;
        }
        boolean asNote = bank.noted && Notes.canNote(e.item, e.tag);
        if (bank.noted && !asNote) {
            Msg.game(player, "This item cannot be withdrawn as a note.");
        }
        ItemStack proto = asNote ? Notes.note(e.item, e.tag, 1) : Notes.stackOf(e.item, e.tag, 1);
        if (proto.isEmpty()) {
            Msg.game(player, "That item no longer exists.");
            return;
        }
        int room = Notes.room(player, proto);
        int n = (int) Math.min(want, room);
        if (n <= 0) {
            Msg.game(player, "You don't have enough inventory space.");
            return;
        }
        long taken = bank.withdraw(e.item, e.tag, n);
        Notes.insert(player, proto, (int) taken);
        if (taken < want) {
            Msg.game(player, "You don't have enough inventory space to withdraw that many.");
        }
    }

    /** Bank key parts for a stack: notes deposit as the item they stand for. */
    private static String[] keyOf(ItemStack stack) {
        if (Notes.isNote(stack)) {
            return new String[]{Notes.notedId(stack), Notes.notedTag(stack)};
        }
        return new String[]{Inv.idOf(stack), Notes.tagOf(stack)};
    }

    /** Deposits up to {@code n} of a stack's item into the bank model (no inventory change); returns the amount. */
    public static long depositStack(BankModel bank, ItemStack stack, long n, int tab) {
        String[] k = keyOf(stack);
        return bank.deposit(k[0], k[1], n, tab);
    }

    static void depositSlot(ServerPlayer player, BankModel bank, int slot, BankQuantity.Mode mode, long x, int tab) {
        Inventory inv = player.getInventory();
        if (slot < 0 || slot >= inv.items.size() || inv.items.get(slot).isEmpty()) {
            return;
        }
        ItemStack target = inv.items.get(slot);
        String[] k = keyOf(target);
        boolean noted = Notes.isNote(target);
        // Every stack of the same item counts towards the quantity, as in OSRS; notes and unnoted items are
        // different items there, so only stacks of the clicked kind count.
        long available = 0;
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && sameBankItem(s, k) && Notes.isNote(s) == noted) {
                available += s.getCount();
            }
        }
        long n = BankQuantity.amount(mode, x, available);
        depositMatching(player, bank, k, n, tab, slot, noted);
    }

    private static boolean sameBankItem(ItemStack s, String[] k) {
        String[] o = keyOf(s);
        return o[0].equals(k[0]) && o[1].equals(k[1]);
    }

    /** Moves {@code n} matching items (clicked slot first) into the bank. */
    private static void depositMatching(ServerPlayer player, BankModel bank, String[] k, long n, int tab, int first,
                                        boolean noted) {
        if (n <= 0) {
            return;
        }
        if (!bank.hasRoomFor(k[0], k[1])) {
            Msg.game(player, "Your bank is full.");
            return;
        }
        long deposited = bank.deposit(k[0], k[1], n, tab);
        long left = deposited;
        Inventory inv = player.getInventory();
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (int i = 0; i < inv.items.size() && left > 0; i++) {
                if ((pass == 0) != (i == first)) {
                    continue;
                }
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && sameBankItem(s, k) && Notes.isNote(s) == noted) {
                    int take = (int) Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
        }
        if (deposited < n) {
            Msg.game(player, "Your bank can't hold any more of that item.");
        }
    }

    static void depositPouch(ServerPlayer player, BankModel bank, BankQuantity.Mode mode, long x, int tab) {
        PlayerData data = Players.data(player);
        long n = BankQuantity.amount(mode, x, data.coins());
        long put = bank.deposit(Inv.COINS, "", n, tab);
        data.setCoins(data.coins() - put);
    }

    static void depositInventory(ServerPlayer player, BankModel bank, int tab) {
        Inventory inv = player.getInventory();
        boolean full = false;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) {
                continue;
            }
            long put = depositStack(bank, s, s.getCount(), tab);
            s.shrink((int) put);
            full |= !s.isEmpty();
        }
        depositPouch(player, bank, BankQuantity.Mode.ALL, 0, tab);
        if (full) {
            Msg.game(player, "Your bank is full.");
        }
    }

    static void depositWorn(ServerPlayer player, BankModel bank, int tab) {
        Inventory inv = player.getInventory();
        boolean any = false;
        for (List<ItemStack> list : List.of(inv.armor, inv.offhand)) {
            for (ItemStack s : list) {
                if (!s.isEmpty()) {
                    long put = depositStack(bank, s, s.getCount(), tab);
                    s.shrink((int) put);
                    any |= put > 0;
                }
            }
        }
        // Jewellery, capes, gloves and ammo worn in the OSRS equipment slots (systems module).
        var worn = com.runecraft.content.systems.SysState.of(player).worn;
        for (var it = worn.entrySet().iterator(); it.hasNext(); ) {
            ItemStack s = it.next().getValue();
            if (s.isEmpty()) {
                it.remove();
                continue;
            }
            long put = depositStack(bank, s, s.getCount(), tab);
            s.shrink((int) put);
            any |= put > 0;
            if (s.isEmpty()) {
                it.remove();
            }
        }
        if (any) {
            com.runecraft.content.systems.SysNet.sendState(player);
        } else {
            Msg.game(player, "You have nothing equipped to deposit.");
        }
    }

    // ------------------------------------------------------------------ state

    /** Grand Exchange guide price of a bank item (coins are worth 1; non-RuneCraft items 0). */
    public static long priceOf(String item) {
        if (item.equals(Inv.COINS)) {
            return 1;
        }
        ItemDef def = ItemCatalog.get(item);
        return def == null ? 0 : GrandExchange.guidePrice(def.id(), def.value());
    }

    /**
     * Packet: open flag, deposit-only flag, settings (noted, placeholders, quantity mode, X), total value and
     * the slots in display order (key, item id, NBT string, count, tab).
     */
    public static void send(ServerPlayer player, boolean open) {
        Session s = OPEN.get(player.getUUID());
        BankModel bank = Players.data(player).bank();
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(open);
        buf.writeBoolean(s != null && s.depositOnly());
        buf.writeBoolean(bank.noted);
        buf.writeBoolean(bank.placeholders);
        buf.writeVarInt(bank.quantity.ordinal());
        buf.writeVarLong(bank.x);
        buf.writeVarLong(bank.value(BankService::priceOf));
        List<BankModel.Entry> entries = bank.entries();
        buf.writeVarInt(entries.size());
        for (BankModel.Entry e : entries) {
            buf.writeUtf(e.item);
            buf.writeUtf(e.tag);
            buf.writeVarLong(e.count);
            buf.writeVarInt(e.tab);
        }
        ServerPlayNetworking.send(player, STATE, buf);
    }

    // ------------------------------------------------------------------ persistence

    public static CompoundTag write(BankModel bank) {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (BankModel.Entry e : bank.entries()) {
            CompoundTag c = new CompoundTag();
            c.putString("Item", e.item);
            if (!e.tag.isEmpty()) {
                c.putString("Tag", e.tag);
            }
            c.putLong("Count", e.count);
            if (e.tab != 0) {
                c.putByte("Tab", (byte) e.tab);
            }
            list.add(c);
        }
        nbt.put("Items", list);
        nbt.putBoolean("Noted", bank.noted);
        nbt.putBoolean("Placeholders", bank.placeholders);
        nbt.putString("Quantity", bank.quantity.name());
        nbt.putLong("X", bank.x);
        return nbt;
    }

    public static void read(BankModel bank, CompoundTag nbt) {
        bank.clear();
        ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            bank.load(c.getString("Item"), c.getString("Tag"), c.getLong("Count"), c.getByte("Tab"));
        }
        bank.finishLoad();
        bank.noted = nbt.getBoolean("Noted");
        bank.placeholders = nbt.getBoolean("Placeholders");
        try {
            bank.quantity = BankQuantity.Mode.valueOf(nbt.getString("Quantity"));
        } catch (IllegalArgumentException e) {
            bank.quantity = BankQuantity.Mode.ONE;
        }
        bank.x = Math.max(1, nbt.getLong("X"));
    }

    // ------------------------------------------------------------------ deposit boxes

    /** Puts a deposit box on the ground at a spot (on a dock's planks or the beach), unless something is there. */
    private static void placeDepositBox(ServerLevel world, BlockPos centre) {
        Block box = BuiltInRegistries.BLOCK.get(RuneCraft.id(DepositBoxes.BLOCK));
        if (box == Blocks.AIR) {
            return;
        }
        int x = centre.getX();
        int z = centre.getZ();
        com.runecraft.world.StructurePlacer.applyIfLoaded(world, x >> 4, z >> 4);
        int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos at = new BlockPos(x, top, z);
        BlockState below = world.getBlockState(at.below());
        if (!world.getBlockState(at).isAir() || !below.isRedstoneConductor(world, at.below())) {
            RuneCraft.LOGGER.info("No room for a deposit box at {}", at);
            return;
        }
        world.setBlock(at, box.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
