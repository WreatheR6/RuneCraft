package com.runecraft.content.minigames;

import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The plank sack (OSRS Wiki): holds 28 planks of any type, used straight from the sack (Mahogany Homes builds take planks
 * from it after the inventory). Using it fills it with the carried planks, or empties it when none are carried. The
 * contents live on the stack ({@link StackData}, key {@value #KEY}).
 */
public final class PlankSack {
    public static final String KEY = "PlankSack";

    private PlankSack() {
    }

    private static CompoundTag contents(ItemStack sack) {
        return StackData.get(sack).getCompound(KEY);
    }

    private static int total(ItemStack sack) {
        CompoundTag c = contents(sack);
        int n = 0;
        for (String k : c.getAllKeys()) {
            n += c.getInt(k);
        }
        return n;
    }

    /** The first plank sack the player carries, or EMPTY. */
    private static ItemStack sack(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (Inv.idOf(st).equals("plank_sack")) {
                return st;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Planks of a type in the player's plank sack. */
    public static int count(ServerPlayer p, String plank) {
        ItemStack s = sack(p);
        return s.isEmpty() ? 0 : contents(s).getInt(plank);
    }

    /** Takes up to {@code n} planks of a type from the sack; returns how many it took. */
    public static int take(ServerPlayer p, String plank, int n) {
        ItemStack s = sack(p);
        int have = s.isEmpty() ? 0 : contents(s).getInt(plank);
        int took = Math.min(have, Math.max(0, n));
        if (took > 0) {
            StackData.update(s, t -> {
                CompoundTag c = t.getCompound(KEY);
                if (have - took <= 0) {
                    c.remove(plank);
                } else {
                    c.putInt(plank, have - took);
                }
                t.put(KEY, c);
            });
        }
        return took;
    }

    /** Fills the sack from the inventory, or empties it into the inventory when no planks are carried. */
    public static void use(ServerPlayer p, ItemStack sack) {
        int room = MinigameRules.MahoganyHomes.PLANK_SACK - total(sack);
        boolean carrying = false;
        for (String plank : MinigameRules.MahoganyHomes.PLANKS) {
            carrying |= Inv.has(p, plank, 1);
        }
        if (carrying) {
            if (room <= 0) {
                Msg.game(p, "Your plank sack is full.");
                return;
            }
            int added = 0;
            for (String plank : MinigameRules.MahoganyHomes.PLANKS) {
                int n = Inv.remove(p, plank, Math.min(room - added, Inv.count(p, plank)));
                if (n > 0) {
                    int put = n;
                    StackData.update(sack, t -> {
                        CompoundTag c = t.getCompound(KEY);
                        c.putInt(plank, c.getInt(plank) + put);
                        t.put(KEY, c);
                    });
                    added += n;
                }
            }
            Msg.game(p, "You put " + added + " plank" + (added == 1 ? "" : "s") + " in the sack (" + total(sack) + " / "
                    + MinigameRules.MahoganyHomes.PLANK_SACK + ").");
            return;
        }
        CompoundTag c = contents(sack);
        if (c.isEmpty()) {
            Msg.game(p, "Your plank sack is empty.");
            return;
        }
        int out = 0;
        for (String plank : MinigameRules.MahoganyHomes.PLANKS) {
            int n = c.getInt(plank);
            int fits = 0;
            while (fits < n && Inv.hasRoomFor(p, plank)) {
                Inv.give(p, plank, 1);
                fits++;
            }
            if (fits > 0) {
                take(p, plank, fits);
                out += fits;
            }
        }
        Msg.game(p, out == 0 ? "You have no room for the planks." : "You empty " + out + " plank" + (out == 1 ? "" : "s")
                + " from the sack.");
    }
}
