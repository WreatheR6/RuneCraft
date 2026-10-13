package com.runecraft.content.minigames;

import com.runecraft.content.systems.SysState;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Last Man Standing puts a player's own things aside for the game: inventory, armour, off-hand, the extra worn slots, skill
 * experience and boosts, prayer points and health. They are saved with the world (so a crash or restart mid-game still gives
 * everything back on the next login) and restored when the player leaves the island, wiping every LMS item they carried.
 */
public final class LmsStash extends PersistentState {
    public static final String KEY = "runecraft_minigames_lms";

    private final Map<UUID, NbtCompound> stashed = new HashMap<>();

    public static LmsStash get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(LmsStash::fromNbt, LmsStash::new, KEY);
    }

    public static boolean has(ServerPlayerEntity player) {
        return get(player.getServer()).stashed.containsKey(player.getUuid());
    }

    /** Saves and clears the player's belongings and stats. Does nothing if they already have a stash. */
    public static void stash(ServerPlayerEntity player) {
        LmsStash state = get(player.getServer());
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
        PlayerData data = Players.data(player);
        c.putIntArray("Xp", data.skills().rawXp());
        c.putIntArray("Boosts", data.skills().rawBoosts());
        c.putInt("Prayer", data.prayerPoints());
        c.putInt("Spec", sp.specEnergy);
        c.putFloat("Health", player.getHealth());
        state.stashed.put(player.getUuid(), c);
        state.markDirty();
        player.getInventory().clear();
        sp.worn.clear();
    }

    /** Gives back everything saved by {@link #stash}, throwing away whatever the player holds now. */
    public static void restore(ServerPlayerEntity player) {
        LmsStash state = get(player.getServer());
        NbtCompound c = state.stashed.remove(player.getUuid());
        if (c == null) {
            return;
        }
        state.markDirty();
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
        sp.specEnergy = c.getInt("Spec");
        PlayerData data = Players.data(player);
        data.skills().load(c.getIntArray("Xp"), c.getIntArray("Boosts"));
        data.activePrayers().clear();
        data.setPrayerPoints(c.getInt("Prayer"));
        XpService.updateMaxHealth(player);
        player.setHealth(Math.max(1f, Math.min(player.getMaxHealth(), c.getFloat("Health"))));
        ServerNetworking.sync(player);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound root = new NbtCompound();
        stashed.forEach((uuid, c) -> root.put(uuid.toString(), c));
        nbt.put("Stashed", root);
        return nbt;
    }

    public static LmsStash fromNbt(NbtCompound nbt) {
        LmsStash state = new LmsStash();
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
