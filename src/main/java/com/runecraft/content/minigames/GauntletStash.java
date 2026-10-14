package com.runecraft.content.minigames;

import com.runecraft.content.systems.SysState;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.content.minigames.Gauntlet;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Gauntlet stores everything a player carries and wears for the run (OSRS: "Players cannot bring any items into the
 * Gauntlet") and removes pre-applied boosts. Unlike Last Man Standing, skill experience gained inside is real and kept. The
 * stash is saved with the world, so a crash mid-run still gives everything back on the next login; on the way out every
 * Gauntlet-only item is thrown away and anything else picked up (the Hunllef's drops) is kept.
 */
public final class GauntletStash extends PersistentState {
    public static final String KEY = "runecraft_minigames_gauntlet";
    private static final Set<String> RUN_ITEMS = new HashSet<>(Gauntlet.runItems());

    private final Map<UUID, NbtCompound> stashed = new HashMap<>();

    public static GauntletStash get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(GauntletStash::fromNbt, GauntletStash::new, KEY);
    }

    public static boolean has(ServerPlayerEntity player) {
        return get(player.getServer()).stashed.containsKey(player.getUuid());
    }

    /** Saves and clears the player's belongings and resets boosted or drained skills. Does nothing if already stashed. */
    public static void stash(ServerPlayerEntity player) {
        GauntletStash state = get(player.getServer());
        if (state.stashed.containsKey(player.getUuid())) {
            return;
        }
        NbtCompound c = new NbtCompound();
        c.put("Inventory", player.getInventory().writeNbt(new NbtList()));
        NbtCompound worn = new NbtCompound();
        SysState.SysPlayer sp = SysState.of(player);
        for (Map.Entry<Jewellery.Slot, ItemStack> e : sp.worn.entrySet()) {
            if (!e.getValue().isEmpty()) {
                worn.put(e.getKey().name(), e.getValue().writeNbt(new NbtCompound()));
            }
        }
        c.put("Worn", worn);
        state.stashed.put(player.getUuid(), c);
        state.markDirty();
        player.getInventory().clear();
        sp.worn.clear();
        // "Any pre-applied skill boosts will also be removed upon entry."
        var skills = Players.data(player).skills();
        for (com.runecraft.osrs.Skill s : com.runecraft.osrs.Skill.values()) {
            if (s != com.runecraft.osrs.Skill.HITPOINTS && s != com.runecraft.osrs.Skill.PRAYER) {
                skills.setBoost(s, 0);
            }
        }
        ServerNetworking.sync(player);
    }

    /** Whether an item exists only inside the Gauntlet. */
    public static boolean runItem(String id) {
        return RUN_ITEMS.contains(id);
    }

    /**
     * Gives back everything saved by {@link #stash}. Gauntlet items are thrown away; anything else the player holds now (the
     * Hunllef's drops, a pet) is kept and handed back after the stash.
     */
    public static void restore(ServerPlayerEntity player) {
        GauntletStash state = get(player.getServer());
        NbtCompound c = state.stashed.remove(player.getUuid());
        if (c == null) {
            return;
        }
        state.markDirty();
        List<ItemStack> keep = new ArrayList<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (!s.isEmpty() && !runItem(Inv.idOf(s))) {
                keep.add(s.copy());
            }
        }
        player.getInventory().clear();
        player.getInventory().readNbt(c.getList("Inventory", NbtElement.COMPOUND_TYPE));
        SysState.SysPlayer sp = SysState.of(player);
        sp.worn.clear();
        NbtCompound worn = c.getCompound("Worn");
        for (Jewellery.Slot slot : Jewellery.Slot.values()) {
            if (worn.contains(slot.name())) {
                ItemStack stack = ItemStack.fromNbt(worn.getCompound(slot.name()));
                if (!stack.isEmpty()) {
                    sp.worn.put(slot, stack);
                }
            }
        }
        for (ItemStack s : keep) {
            Inv.give(player, s);
        }
        ServerNetworking.sync(player);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound root = new NbtCompound();
        stashed.forEach((uuid, c) -> root.put(uuid.toString(), c));
        nbt.put("Stashed", root);
        return nbt;
    }

    public static GauntletStash fromNbt(NbtCompound nbt) {
        GauntletStash state = new GauntletStash();
        NbtCompound root = nbt.getCompound("Stashed");
        for (String key : root.getKeys()) {
            try {
                state.stashed.put(UUID.fromString(key), root.getCompound(key));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return state;
    }
}
