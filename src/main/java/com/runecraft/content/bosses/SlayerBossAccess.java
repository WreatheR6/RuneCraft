package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.content.slayer.SlayerState;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.bosses.SlayerBossRules;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The Slayer bosses' entry rules ({@link SlayerBossRules}): each one is only fought while the player's Slayer task is one
 * it counts towards (its creature category or the boss itself as a boss task), on top of its Slayer level; the Grotesque
 * Guardians' roof also has to be unlocked once with a brittle key, which gargoyles drop while they are the task. The roof
 * unlocks are kept in the world save (key {@value State#KEY}).
 */
public final class SlayerBossAccess {
    private SlayerBossAccess() {
    }

    static void register() {
        for (SlayerBossRules.Access a : SlayerBossRules.all().values()) {
            DataBossService.requirement(a.bossId(), (player, def) -> mayFight(player, a.bossId(), true));
        }
        com.runecraft.content.GameEvents.onKill((player, entity, stats) -> {
            // OSRS Wiki "Brittle key": only gargoyles (and their superior) on a gargoyle task drop it, until the roof is open.
            if (stats == null || !(stats.profile() instanceof MonsterDef)
                    || !SlayerBossRules.BRITTLE_KEY_TASK.equals(SlayerService.categoryOf(stats))) {
                return;
            }
            SlayerState.Profile p = SlayerService.profile(player);
            if (!p.hasTask() || !SlayerBossRules.BRITTLE_KEY_TASK.equals(p.task) || roofUnlocked(player)
                    || Inv.has(player, SlayerBossRules.BRITTLE_KEY, 1)
                    || player.getRandom().nextInt(SlayerBossRules.BRITTLE_KEY_RATE) != 0) {
                return;
            }
            com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(SlayerBossRules.BRITTLE_KEY, 1), player);
            CollectionLog.obtain(player, SlayerBossRules.BRITTLE_KEY);
            Msg.red(player, "A brittle key falls to the floor.");
        });
    }

    /** The current Slayer task of a player (its category id), or null when they have none. */
    public static String task(ServerPlayer player) {
        SlayerState.Profile p = SlayerService.profile(player);
        return p.hasTask() ? p.task : null;
    }

    /**
     * Whether the player may fight a Slayer boss now: assigned a task it counts towards, and for the Grotesque Guardians the
     * roof unlocked (a brittle key in the pack unlocks it for good when {@code useKey}). Tells the player why not.
     */
    public static boolean mayFight(ServerPlayer player, String bossId, boolean useKey) {
        SlayerBossRules.Access a = SlayerBossRules.of(bossId);
        if (a == null) {
            return true;
        }
        if (!a.allows(task(player))) {
            Msg.game(player, a.refusal());
            return false;
        }
        if ("dusk".equals(bossId) && !roofUnlocked(player)) {
            if (!useKey || !Inv.has(player, SlayerBossRules.BRITTLE_KEY, 1)) {
                Msg.game(player, "The way up to the roof is locked. Gargoyles you're assigned to kill sometimes drop a brittle key.");
                return false;
            }
            Inv.remove(player, SlayerBossRules.BRITTLE_KEY, 1);
            State.get(player.getServer()).unlock(player.getUUID());
            Msg.game(player, "You unlock the door to the roof. The key crumbles in the lock; the way up stays open for good.");
        }
        return true;
    }

    /** Whether the player has opened the Slayer Tower's roof with a brittle key. */
    public static boolean roofUnlocked(ServerPlayer player) {
        return State.get(player.getServer()).unlocked(player.getUUID());
    }

    /** Self-test: locks or unlocks the roof for a player; returns whether it was unlocked before. */
    public static boolean setRoofUnlocked(ServerPlayer player, boolean unlocked) {
        State s = State.get(player.getServer());
        boolean before = s.unlocked(player.getUUID());
        if (unlocked) {
            s.unlock(player.getUUID());
        } else if (s.roof.remove(player.getUUID())) {
            s.setDirty();
        }
        return before;
    }

    /** Players who unlocked the Slayer Tower's roof. */
    static final class State extends SavedData {
        static final String KEY = "runecraft_bosses_slayer";
        private final Set<UUID> roof = new HashSet<>();

        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::fromNbt, null), KEY);
        }

        boolean unlocked(UUID uuid) {
            return roof.contains(uuid);
        }

        void unlock(UUID uuid) {
            if (roof.add(uuid)) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            roof.forEach(u -> list.add(StringTag.valueOf(u.toString())));
            nbt.put("Roof", list);
            return nbt;
        }

        static State fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
            State s = new State();
            ListTag list = nbt.getList("Roof", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    s.roof.add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException ignored) {
                    // not a player id
                }
            }
            return s;
        }
    }
}
