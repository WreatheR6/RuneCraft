package com.runecraft.content.systems;

import com.runecraft.bank.BankService;
import com.runecraft.bank.Notes;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.bank.BankModel;
import com.runecraft.osrs.content.systems.LootingBag;
import com.runecraft.osrs.data.Untradeables;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.ServerLivingEntityEvents;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Map;

/**
 * Minecraft side of {@link LootingBag}: right-click the bag to see its contents and Open/Close it or store tradeable
 * items from the inventory (Wilderness only); while it is open, tradeable items picked up in the Wilderness go into it.
 * Opening a bank or deposit box while holding the bag banks everything in it (BankService). The contents live in the
 * player's {@link SysState} (one bag per player), so they survive logging out and restarts; a death in the Wilderness drops
 * them all where the player died, as in OSRS.
 */
public final class LootingBagService {
    private static final String SCREEN = "looting_bag";

    private LootingBagService() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !LootingBag.ID.equals(Inv.idOf(stack))) {
                return InteractionResultHolder.pass(stack);
            }
            if (world.isClientSide()) {
                return InteractionResultHolder.sidedSuccess(stack, true);
            }
            if (player instanceof ServerPlayer sp) {
                open(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        });
        SysNet.onAction(SCREEN, (p, action, fields) -> {
            if (!Inv.has(p, LootingBag.ID, 1)) {
                return;
            }
            SysState.SysPlayer sp = SysState.of(p);
            if (action.equals("toggle")) {
                sp.lootingBagOpen = !sp.lootingBagOpen;
                Msg.game(p, sp.lootingBagOpen ? "You open your looting bag. Tradeable items you pick up in the Wilderness will go into it."
                        : "You close your looting bag.");
            } else if (action.equals("store_all")) {
                storeAll(p, false);
            } else if (action.startsWith("store:")) {
                try {
                    store(p, Integer.parseInt(action.substring(6)), false);
                } catch (NumberFormatException ignored) {
                    // bad slot
                }
            }
            open(p);
        });
        ServerTickEvents.END_WORLD_TICK.register(LootingBagService::collect);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p && Gielinor.wildernessLevel(p) > 0) {
                dropOnDeath(p);
            }
        });
    }

    /** Wilderness death: everything in the bag drops where the player died (the contents are not protected). */
    private static void dropOnDeath(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        sp.lootingBagOpen = false;
        if (sp.lootingBag.isEmpty()) {
            return;
        }
        for (LootingBag.Stack st : sp.lootingBag.takeAll()) {
            ItemStack proto = st.noted ? Notes.note(st.item, st.tag, 1) : Notes.stackOf(p.registryAccess(), st.item, st.tag, 1);
            if (proto.isEmpty()) {
                continue;
            }
            int max = Math.max(1, proto.getMaxStackSize());
            for (long left = st.count(); left > 0; ) {
                int n = (int) Math.min(left, max);
                p.drop(proto.copyWithCount(n), true, false);
                left -= n;
            }
        }
        Msg.game(p, "The contents of your looting bag are dropped where you died.");
    }

    /** The check/deposit screen: contents, Open/Close and one button per tradeable inventory stack. */
    public static void open(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        LootingBag.Contents bag = sp.lootingBag;
        SysNet.Screen s = new SysNet.Screen(SCREEN, "Looting bag (" + bag.slotsUsed() + "/" + LootingBag.CAPACITY + ")");
        long value = 0;
        if (bag.isEmpty()) {
            s.line("The bag is empty.", 0x808080);
        }
        for (LootingBag.Stack st : bag.stacks()) {
            ItemDef def = ItemCatalog.get(st.item);
            String name = def == null ? st.item : def.name();
            value += BankService.priceOf(st.item) * st.count();
            s.line(String.format("%,d", st.count()) + " x " + name + (st.noted ? " (noted)" : ""), 0xFFFFFF);
        }
        s.line("Value: " + String.format("%,d", value) + " coins. Hold the bag when you open a bank to deposit everything.", 0xC0C0C0);
        boolean wild = fillable(p);
        s.button("toggle", sp.lootingBagOpen ? "Close bag" : "Open bag",
                sp.lootingBagOpen ? "Picked-up items go to your inventory." : "Tradeable items you pick up in the Wilderness go into the bag.", true);
        s.button("store_all", "Deposit all tradeables", wild ? "" : "You can only fill the bag in the Wilderness.", wild);
        Inventory inv = p.getInventory();
        int shown = 0;
        for (int i = 0; i < inv.items.size() && shown < 12; i++) {
            ItemStack st = inv.items.get(i);
            if (st.isEmpty() || !storable(st)) {
                continue;
            }
            s.button("store:" + i, "Store " + st.getCount() + " x " + st.getHoverName().getString(), "", wild);
            shown++;
        }
        s.send(p);
    }

    /** Whether a stack may go in the bag at all (tradeable, not coins, not the bag itself). */
    static boolean storable(ItemStack st) {
        String id = Notes.isNote(st) ? Notes.notedId(st) : Inv.idOf(st);
        return !id.isEmpty() && !id.equals(LootingBag.ID) && !id.equals(Inv.COINS) && ItemCatalog.get(id) != null
                && LootingBag.canStore(!Untradeables.untradeable(id), 1);
    }

    /** Puts one inventory stack in the bag; {@code anywhere} skips the Wilderness check (self-test). Returns items stored. */
    /** The bag fills only in the Wilderness, and not in the Ferox Enclave's safe zone (wiki: the enclave isn't Wilderness for it). */
    static boolean fillable(ServerPlayer p) {
        return Gielinor.wildernessLevel(p) > 0 && !Gielinor.inSafeZone(p);
    }

    public static int store(ServerPlayer p, int slot, boolean anywhere) {
        Inventory inv = p.getInventory();
        if (slot < 0 || slot >= inv.items.size()) {
            return 0;
        }
        if (!anywhere && !fillable(p)) {
            Msg.game(p, "You can't put items in the looting bag unless you're in the Wilderness.");
            return 0;
        }
        ItemStack st = inv.items.get(slot);
        if (st.isEmpty() || !storable(st)) {
            Msg.game(p, "Only tradeable items can be put in the looting bag.");
            return 0;
        }
        int put = put(p, SysState.of(p).lootingBag, st);
        if (put == 0) {
            Msg.game(p, "The bag's too full.");
        }
        p.getInventory().setChanged();
        ServerNetworking.sync(p);
        return put;
    }

    /** Puts every tradeable inventory stack in the bag (as far as it fits). Returns items stored. */
    public static int storeAll(ServerPlayer p, boolean anywhere) {
        if (!anywhere && !fillable(p)) {
            Msg.game(p, "You can't put items in the looting bag unless you're in the Wilderness.");
            return 0;
        }
        LootingBag.Contents bag = SysState.of(p).lootingBag;
        int total = 0;
        boolean full = false;
        for (ItemStack st : p.getInventory().items) {
            if (!st.isEmpty() && storable(st)) {
                int before = st.getCount();
                total += put(p, bag, st);
                full |= st.getCount() > 0 && st.getCount() == before;
            }
        }
        if (full) {
            Msg.game(p, "The bag's too full.");
        }
        p.getInventory().setChanged();
        ServerNetworking.sync(p);
        return total;
    }

    /** Moves as much of a stack as fits into the bag; returns the amount moved. */
    private static int put(ServerPlayer p, LootingBag.Contents bag, ItemStack st) {
        boolean noted = Notes.isNote(st);
        String item = noted ? Notes.notedId(st) : Inv.idOf(st);
        String tag = noted ? Notes.notedTag(st) : Notes.tagOf(p.registryAccess(), st);
        int put = bag.store(item, tag, noted, st.isStackable(), st.getCount());
        st.shrink(put);
        return put;
    }

    /**
     * Banks everything in the bag (bank or deposit box opened while holding it). What the bank has no room for goes to the
     * inventory, and what doesn't fit there stays in the bag. Returns the number of items banked.
     */
    public static long bankAll(ServerPlayer p) {
        LootingBag.Contents bag = SysState.of(p).lootingBag;
        if (bag.isEmpty()) {
            Msg.game(p, "Your looting bag is empty.");
            return 0;
        }
        BankModel bank = Players.data(p).bank();
        long banked = 0;
        int toInventory = 0;
        for (LootingBag.Stack st : bag.takeAll()) {
            long left = st.count();
            if (bank.hasRoomFor(st.item, st.tag)) {
                long put = bank.deposit(st.item, st.tag, left, 0);
                banked += put;
                left -= put;
            }
            if (left > 0) {
                ItemStack proto = st.noted ? Notes.note(st.item, st.tag, 1) : Notes.stackOf(p.registryAccess(), st.item, st.tag, 1);
                int in = proto.isEmpty() ? 0 : Notes.insert(p, proto, (int) Math.min(Integer.MAX_VALUE, left));
                toInventory += in;
                left -= in;
            }
            if (left > 0) {
                bag.restore(new LootingBag.Stack(st.item, st.tag, st.noted, st.stackable, left));
            }
        }
        Msg.game(p, "You empty your looting bag into your bank" + (banked == 0 ? "" : " (" + String.format("%,d", banked) + " items)")
                + (toInventory > 0 ? "; " + toInventory + " went to your inventory." : ".") + (bag.isEmpty() ? "" : " Some items didn't fit."));
        p.getInventory().setChanged();
        ServerNetworking.sync(p);
        return banked;
    }

    /** While the bag is open, tradeable items a player in the Wilderness walks over go into it (before vanilla pickup). */
    private static void collect(ServerLevel world) {
        for (ServerPlayer p : world.players()) {
            if (p.isSpectator() || !p.isAlive() || !fillable(p)) {
                continue;
            }
            SysState.SysPlayer sp = SysState.of(p);
            if (!sp.lootingBagOpen || !Inv.has(p, LootingBag.ID, 1)) {
                continue;
            }
            List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(1.0, 0.5, 1.0),
                    e -> e.isAlive() && !e.hasPickUpDelay() && storable(e.getItem())
                            && (e.getTarget() == null || e.getTarget().equals(p.getUUID()))); // not someone else's private drop
            for (ItemEntity e : items) {
                ItemStack st = e.getItem();
                int before = st.getCount();
                int put = put(p, sp.lootingBag, st);
                if (put <= 0) {
                    continue;
                }
                Msg.game(p, "You put " + put + " x " + st.getHoverName().getString() + " in your looting bag.");
                if (put >= before || st.isEmpty()) {
                    e.discard();
                } else {
                    e.setItem(st);
                }
            }
        }
    }

    /** Self-test hook: the player's bag contents as item -> total count. */
    public static Map<String, Long> contents(ServerPlayer p) {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        for (LootingBag.Stack st : SysState.of(p).lootingBag.stacks()) {
            out.merge(st.item, st.count(), Long::sum);
        }
        return out;
    }
}
