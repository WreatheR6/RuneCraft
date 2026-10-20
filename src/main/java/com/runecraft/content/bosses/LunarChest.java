package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.BossLoot;
import com.runecraft.osrs.content.bosses.MoonsRules;
import com.runecraft.osrs.data.Drop;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The Lunar Chest of the Moons of Peril (OSRS Wiki "Lunar Chest", {@link MoonsRules#chest}): a Moon's kill drops nothing; it
 * is recorded for its killer, and the chest pays out for every Moon defeated since the last claim (a 1/56 unique roll per
 * Moon, a piece not owned yet first; else the standard table 1, 3 or 6 times). It first opens once all three Moons have been
 * defeated, after Perilous Moons. The record is kept in the world save, so leaving between the kill and the chest loses
 * nothing.
 */
final class LunarChest {
    private LunarChest() {
    }

    static final String KEY = "runecraft_bosses_moons";

    /** The Moons each player has defeated since their last claim. */
    static final class State extends SavedData {
        final Map<UUID, Set<String>> defeated = new HashMap<>();

        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::fromNbt, null), KEY);
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
            defeated.forEach((uuid, moons) -> {
                if (!moons.isEmpty()) {
                    nbt.putString(uuid.toString(), String.join(",", moons));
                }
            });
            return nbt;
        }

        static State fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
            State s = new State();
            for (String key : nbt.getAllKeys()) {
                try {
                    Set<String> moons = new LinkedHashSet<>();
                    for (String m : nbt.getString(key).split(",")) {
                        if (MoonsRules.MOONS.contains(m)) {
                            moons.add(m);
                        }
                    }
                    s.defeated.put(UUID.fromString(key), moons);
                } catch (IllegalArgumentException ignored) {
                    // not a player entry
                }
            }
            return s;
        }
    }

    static void init() {
        for (String moon : MoonsRules.MOONS) {
            DataBossService.loot(moon, (killer, boss, table) -> {
                if (killer != null) {
                    record(killer, moon);
                }
                return List.of();
            });
        }
        com.runecraft.content.BlockHooks.register(MoonsRules.LUNAR_CHEST, LunarChest::claim);
    }

    static void record(ServerPlayer p, String moon) {
        State s = State.get(p.getServer());
        if (s.defeated.computeIfAbsent(p.getUUID(), k -> new LinkedHashSet<>()).add(moon)) {
            s.setDirty();
        }
        Msg.game(p, "The Moon is pacified. Your reward awaits in the Lunar Chest.");
    }

    /** The self-test keeps a real player's record apart from its test kills (TestContext's module states). */
    static Object selfTestSave(MinecraftServer server, UUID player) {
        Set<String> d = State.get(server).defeated.get(player);
        return d == null ? null : new LinkedHashSet<>(d);
    }

    @SuppressWarnings("unchecked")
    static void selfTestRestore(MinecraftServer server, UUID player, Object saved) {
        State s = State.get(server);
        if (saved instanceof Set<?> set) {
            s.defeated.put(player, new LinkedHashSet<>((Set<String>) set));
        } else {
            s.defeated.remove(player);
        }
        s.setDirty();
    }

    /** How many Moons a player has defeated since their last claim. */
    static int pending(ServerPlayer p) {
        Set<String> d = State.get(p.getServer()).defeated.get(p.getUUID());
        return d == null ? 0 : d.size();
    }

    private static void claim(ServerPlayer player, BlockPos pos, BlockState state) {
        claim(player);
    }

    /** Claims the chest; returns the items handed over (empty when it stayed shut). Public to the package for the self-test. */
    static List<String> claim(ServerPlayer player) {
        var quest = ContentRegistry.quest("perilous_moons");
        PlayerData data = Players.data(player);
        if (quest != null && !data.questComplete(quest)) {
            Msg.game(player, "The chest is sealed by Eyatlalli's magic. (Complete Perilous Moons.)");
            return List.of();
        }
        for (String moon : MoonsRules.MOONS) {
            if (data.killCount(moon) <= 0) {
                Msg.game(player, "The chest won't open until you have defeated all three Moons.");
                return List.of();
            }
        }
        State s = State.get(player.getServer());
        Set<String> defeated = s.defeated.getOrDefault(player.getUUID(), Set.of());
        if (defeated.isEmpty()) {
            Msg.game(player, "You haven't pacified a Moon since you last claimed from the chest.");
            return List.of();
        }
        Map<String, List<Drop>> tables = new LinkedHashMap<>();
        for (String moon : defeated) {
            BossDef def = ContentRegistry.boss(moon);
            if (def != null) {
                tables.put(moon, def.drops);
            }
        }
        Set<String> owned = new HashSet<>(data.collectionLog());
        MoonsRules.ChestRoll roll = MoonsRules.chest(defeated, MoonsRules.sets(tables), owned, player.getRandom()::nextInt);
        List<String> got = new ArrayList<>();
        for (String unique : roll.uniques()) {
            Inv.give(player, unique, 1);
            CollectionLog.obtain(player, unique);
            got.add(unique);
            player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                    + " received a drop: " + name(unique) + " (Lunar Chest)").withStyle(ChatFormatting.GOLD), false);
        }
        if (roll.standardRolls() > 0) {
            List<Drop> lines = MoonsRules.standardLines(tables.values().iterator().next());
            for (Drops.Rolled r : Drops.resolve(BossLoot.pick(lines, roll.standardRolls(), player.getRandom()::nextInt),
                    player.getRandom())) {
                give(player, r.item(), r.count());
                CollectionLog.obtain(player, r.item());
                got.add(r.item());
            }
        }
        s.defeated.remove(player.getUUID());
        s.setDirty();
        Msg.game(player, "You claim the Lunar Chest's reward for " + defeated.size() + (defeated.size() == 1 ? " Moon." : " Moons."));
        return got;
    }

    /** Loot the wiki lists as noted comes noted when there is more than one. */
    private static void give(ServerPlayer player, String item, int n) {
        com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(item);
        if (n > 1 && def != null && def.maxStack() <= 1 && com.runecraft.osrs.bank.BankNotes.canNote(item)) {
            net.minecraft.world.item.ItemStack note = com.runecraft.bank.Notes.note(item, "", 1);
            if (!note.isEmpty()) {
                int in = com.runecraft.bank.Notes.insert(player, note, n);
                if (in < n) {
                    Inv.drop(player, note.copyWithCount(n - in), false);
                }
                return;
            }
        }
        Inv.give(player, item, n);
    }

    private static String name(String item) {
        com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(item);
        return def == null ? item : def.name();
    }
}
