package com.runecraft.content.slayer;

import com.runecraft.osrs.content.slayer.RunePouch;
import com.runecraft.osrs.content.slayer.SlayerRewards;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Slayer reward and equipment items that do something when used: the rune pouch (runes stored on the pouch's stack, cast
 * from directly by {@code combat.MagicService} and {@code content.systems.SpellService}), the fungicide spray's ten
 * charges and its fungicide refills, the broad arrowhead and unfinished broad bolt packs, and slayer helmet disassembly.
 */
public final class SlayerItemService {
    private static final String RUNES = "PouchRunes";
    private static final String CHARGES = "Charges";
    /** A new fungicide spray holds 10 charges; a refill tops it back up to 10 (OSRS Wiki "Fungicide spray"). */
    public static final int SPRAY_CHARGES = 10;

    private SlayerItemService() {
    }

    static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            String id = Inv.idOf(stack);
            boolean ours = RunePouch.ID.equals(id) || "fungicide_spray".equals(id) || SlayerRewards.PACKS.containsKey(id);
            if (stack.isEmpty() || !ours) {
                return InteractionResultHolder.pass(stack);
            }
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) {
                return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
            }
            if (RunePouch.ID.equals(id)) {
                showPouch(sp);
            } else if ("fungicide_spray".equals(id)) {
                refillSpray(sp, stack);
            } else {
                openPack(sp, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        });
    }

    // ------------------------------------------------------------------ rune pouch

    /** The player's rune pouch stack (the first in the inventory), or null. */
    static ItemStack pouch(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (RunePouch.ID.equals(Inv.idOf(s))) {
                return s;
            }
        }
        return null;
    }

    static Map<String, Integer> contents(ItemStack pouch) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (pouch == null) {
            return out;
        }
        CompoundTag runes = StackData.get(pouch).getCompound(RUNES);
        for (String k : runes.getAllKeys()) {
            out.put(k, runes.getInt(k));
        }
        return out;
    }

    private static void store(ItemStack pouch, Map<String, Integer> contents) {
        CompoundTag runes = new CompoundTag();
        RunePouch.clean(contents).forEach(runes::putInt);
        StackData.update(pouch, t -> {
            if (runes.isEmpty()) {
                t.remove(RUNES);
            } else {
                t.put(RUNES, runes);
            }
        });
    }

    /** Runes of this kind in the player's rune pouch (0 without one): spells count them as carried. */
    public static int pouchCount(Player player, String rune) {
        return contents(pouch(player)).getOrDefault(rune, 0);
    }

    /** Takes up to {@code n} of a rune from the player's pouch; returns how many it took. */
    public static int takeFromPouch(Player player, String rune, int n) {
        ItemStack pouch = pouch(player);
        if (pouch == null || n <= 0) {
            return 0;
        }
        Map<String, Integer> c = contents(pouch);
        int taken = RunePouch.withdraw(c, rune, n);
        if (taken > 0) {
            store(pouch, c);
        }
        return taken;
    }

    /**
     * Takes {@code n} runes for a spell: from the inventory first, then the pouch. Returns how many could not be found
     * (0 when paid in full).
     */
    public static int payRunes(Player player, String rune, int n) {
        int fromInv = Math.min(n, Inv.count(player, rune));
        if (fromInv > 0) {
            Inv.remove(player, rune, fromInv);
        }
        return n - fromInv - takeFromPouch(player, rune, n - fromInv);
    }

    /** Puts every {@code rune} carried into the pouch, as far as it has room. */
    static void depositRunes(ServerPlayer player, String rune) {
        ItemStack pouch = pouch(player);
        if (pouch == null) {
            Msg.game(player, "You don't have a rune pouch.");
            return;
        }
        if (!RunePouch.accepts(rune)) {
            Msg.game(player, "Only runes can go in the rune pouch.");
            return;
        }
        Map<String, Integer> c = contents(pouch);
        int carried = Inv.count(player, rune);
        int n = RunePouch.deposit(c, rune, carried);
        if (n <= 0) {
            Msg.game(player, carried <= 0 ? "You have none of those runes." : "The rune pouch can't hold any more of those.");
            return;
        }
        Inv.remove(player, rune, n);
        store(pouch, c);
        Msg.game(player, "You put " + n + " x " + SlayerCommands.name(rune) + " in the rune pouch.");
    }

    static void withdrawRunes(ServerPlayer player, String rune, int amount) {
        int n = takeFromPouch(player, rune, amount);
        if (n <= 0) {
            Msg.game(player, "There are none of those in your rune pouch.");
            return;
        }
        Inv.give(player, rune, n);
        Msg.game(player, "You take " + n + " x " + SlayerCommands.name(rune) + " from the rune pouch.");
    }

    /** The pouch's contents with withdraw buttons, and a deposit button for every rune carried. */
    static void showPouch(ServerPlayer player) {
        ItemStack pouch = pouch(player);
        if (pouch == null) {
            Msg.game(player, "You don't have a rune pouch.");
            return;
        }
        Map<String, Integer> c = contents(pouch);
        player.displayClientMessage(Component.literal("--- Rune pouch (" + RunePouch.kinds(c) + "/" + RunePouch.TYPES + ") ---")
                .withStyle(ChatFormatting.GOLD), false);
        if (c.isEmpty()) {
            player.displayClientMessage(Component.literal("The pouch is empty.").withStyle(ChatFormatting.GRAY), false);
        }
        for (Map.Entry<String, Integer> e : c.entrySet()) {
            player.displayClientMessage(Component.literal(String.format("%,d", e.getValue()) + " x " + SlayerCommands.name(e.getKey()) + " ")
                    .append(SlayerService.button("[Take all]", "/slayer pouch withdraw " + e.getKey() + " " + e.getValue(), "Withdraw them all"))
                    .append(Component.literal(" "))
                    .append(SlayerService.button("[Take 100]", "/slayer pouch withdraw " + e.getKey() + " 100", "Withdraw 100")), false);
        }
        MutableComponent line = Component.literal("Store: ").withStyle(ChatFormatting.GRAY);
        boolean any = false;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            String id = Inv.idOf(player.getInventory().getItem(i));
            if (RunePouch.accepts(id) && seen.add(id) && RunePouch.room(c, id) > 0) {
                line.append(SlayerService.button("[" + SlayerCommands.name(id) + "]", "/slayer pouch deposit " + id, "Store them all"));
                line.append(Component.literal(" "));
                any = true;
            }
        }
        if (any) {
            player.displayClientMessage(line, false);
        }
    }

    // ------------------------------------------------------------------ fungicide spray

    /** Charges left in a fungicide spray (a new one is full). */
    public static int sprayCharges(ItemStack spray) {
        CompoundTag t = StackData.get(spray);
        return t.contains(CHARGES) ? t.getInt(CHARGES) : SPRAY_CHARGES;
    }

    /** Uses one charge of this spray. */
    static void spendSprayCharge(ItemStack spray) {
        int left = Math.max(0, sprayCharges(spray) - 1);
        StackData.update(spray, t -> t.putInt(CHARGES, left));
    }

    /**
     * Uses one charge of the carried fungicide spray with the fewest charges left (OSRS: the auto-kill prefers it); false
     * when every spray carried is empty.
     */
    static boolean useSprayCharge(Player player) {
        ItemStack best = null;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if ("fungicide_spray".equals(Inv.idOf(s)) && sprayCharges(s) > 0 && (best == null || sprayCharges(s) < sprayCharges(best))) {
                best = s;
            }
        }
        if (best == null) {
            return false;
        }
        spendSprayCharge(best);
        return true;
    }

    private static void refillSpray(ServerPlayer player, ItemStack spray) {
        int charges = sprayCharges(spray);
        if (charges >= SPRAY_CHARGES) {
            Msg.game(player, "Your fungicide spray is full (" + charges + " charges).");
        } else if (!Inv.has(player, "fungicide", 1)) {
            Msg.game(player, "Your fungicide spray has " + charges + " charges left. Refill it with fungicide.");
        } else {
            Inv.remove(player, "fungicide", 1);
            StackData.update(spray, t -> t.putInt(CHARGES, SPRAY_CHARGES));
            Msg.game(player, "You refill the fungicide spray.");
        }
    }

    // ------------------------------------------------------------------ packs and helmets

    private static void openPack(ServerPlayer player, ItemStack stack) {
        SlayerRewards.Input in = SlayerRewards.PACKS.get(Inv.idOf(stack));
        stack.shrink(1);
        Inv.give(player, in.item(), in.count());
        Msg.game(player, "You open the pack and find " + in.count() + " x " + SlayerCommands.name(in.item()) + ".");
    }

    /** Takes a slayer helmet apart into its components (and the recolour item), as OSRS's "Disassemble" option. */
    static boolean disassemble(ServerPlayer player, String helmet) {
        List<SlayerRewards.Input> parts = SlayerRewards.disassembly(helmet);
        if (parts.isEmpty()) {
            Msg.game(player, "That can't be disassembled.");
            return false;
        }
        if (!Inv.has(player, helmet, 1)) {
            Msg.game(player, "You need to carry the helmet to take it apart.");
            return false;
        }
        int free = 0;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (player.getInventory().items.get(i).isEmpty()) {
                free++;
            }
        }
        if (free + 1 < parts.size()) {
            Msg.game(player, "You need " + (parts.size() - 1) + " free inventory spaces to take the helmet apart.");
            return false;
        }
        Inv.remove(player, helmet, 1);
        for (SlayerRewards.Input in : parts) {
            Inv.give(player, in.item(), in.count());
        }
        Msg.game(player, "You disassemble the " + SlayerCommands.name(helmet).toLowerCase(java.util.Locale.ROOT) + ".");
        return true;
    }
}
