package com.runecraft.content.bosses;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossVariant;
import com.runecraft.osrs.content.bosses.DoomDelves;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Doom of Mokhaiotl's delves. Each defeat sends the Doom one level deeper and holds that level's loot for the
 * player: using the summoning stone again follows it down (the next level is harder), sneaking and using the stone
 * claims the loot and ends the run. Dying on a delve run forfeits the loot. The deepest level reached is kept as a
 * personal best ({@link DoomDelves#DEEPEST_KEY} in the kill counts), as is the number of deep delves (9+) completed.
 */
final class DoomDelveService {
    private DoomDelveService() {
    }

    /** One player's run: the last level completed and the loot waiting to be claimed. */
    private static final class Run {
        int completed;
        final List<DoomDelves.Loot> pending = new ArrayList<>();
    }

    // approx: runs are kept in memory (a restart ends the run; OSRS keeps unclaimed loot in a chest by the scoreboard)
    private static final Map<UUID, Run> RUNS = new HashMap<>();

    static void init() {
        BossScripts.register(DoomDelves.BOSS, DoomScript::new);
        DataBossService.requirement(DoomDelves.BOSS, DoomDelveService::beforeSummon);
        DataBossService.variant(DoomDelves.BOSS, DoomDelveService::nextLevel);
        DataBossService.onKilled(DoomDelves.BOSS, DoomDelveService::completed);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) {
                Run run = RUNS.remove(p.getUuid());
                if (run != null && !run.pending.isEmpty()) {
                    Msg.red(p, "Your delve run is over; the Doom's loot is lost.");
                }
            }
        });
    }

    /** Sneak-using the stone claims the loot held from the run instead of descending. */
    private static boolean beforeSummon(ServerPlayerEntity player, BossDef boss) {
        Run run = RUNS.get(player.getUuid());
        if (player.isSneaking() && run != null) {
            claim(player, run);
            return false;
        }
        return true;
    }

    private static BossVariant nextLevel(ServerPlayerEntity player, BossDef boss) {
        Run run = RUNS.computeIfAbsent(player.getUuid(), u -> new Run());
        int level = run.completed + 1;
        if (level > 1) {
            Msg.game(player, "You follow the Doom of Mokhaiotl down the burrow hole to delve level " + level + ".");
        }
        return DoomDelves.variant(boss, level);
    }

    private static void completed(ServerPlayerEntity player, DataBossEntity boss) {
        int level = Math.max(1, boss.delve());
        Run run = RUNS.computeIfAbsent(player.getUuid(), u -> new Run());
        run.completed = level;
        List<DoomDelves.Loot> loot = DoomDelves.roll(level, new java.util.Random(player.getRandom().nextLong()));
        run.pending.addAll(loot);
        PlayerData data = Players.data(player);
        int best = data.killCount(DoomDelves.DEEPEST_KEY);
        if (level > best) {
            data.killCounts().put(DoomDelves.DEEPEST_KEY, level);
            Msg.game(player, "New personal best: delve level " + level + ".");
        }
        if (level >= DoomDelves.DEEPEST) {
            data.incrementKillCount(DoomDelves.DEEP_DELVES_KEY);
        }
        boolean unique = loot.stream().anyMatch(l -> DoomDelves.UNIQUES.contains(l.item()));
        Msg.game(player, "Delve level " + level + " complete. Loot held: " + summary(run.pending) + ".");
        if (unique) {
            Msg.red(player, "A light shines from the burrow hole... delving deeper would risk your unique reward!");
        }
        Msg.game(player, "Use the summoning stone to delve deeper (level " + (level + 1) + "), or sneak and use it to claim your loot.");
    }

    private static void claim(ServerPlayerEntity player, Run run) {
        RUNS.remove(player.getUuid());
        if (run.pending.isEmpty()) {
            Msg.game(player, "You have no loot waiting from the Doom.");
            return;
        }
        for (DoomDelves.Loot l : run.pending) {
            Inv.give(player, l.item(), l.count());
            CollectionLog.obtain(player, l.item());
        }
        Msg.game(player, "You claim your rewards from " + run.completed + " delve level" + (run.completed == 1 ? "" : "s") + ".");
    }

    private static String summary(List<DoomDelves.Loot> loot) {
        Map<String, Integer> merged = new java.util.LinkedHashMap<>();
        for (DoomDelves.Loot l : loot) {
            merged.merge(l.item(), l.count(), Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        merged.forEach((item, n) -> parts.add(n + " x " + item.replace('_', ' ')));
        return String.join(", ", parts);
    }

    /**
     * Delve mechanics on top of the Doom's attack list: from delve 3 it sprays acid blood around itself when damaged
     * (2-3 splats, 4 from delve 8), up to 7 damage a tick to anyone standing in it.
     */
    static final class DoomScript implements BossScript {
        private long lastSplat;

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            int level = boss.delve();
            long now = com.runecraft.action.ActionManager.gameTick();
            if (level >= 3 && damage > 0 && now - lastSplat >= 3) { // approx: at most one spray every 3 ticks
                lastSplat = now;
                int splats = level >= 8 ? 4 : 2 + boss.getRandom().nextInt(2);
                for (int i = 0; i < splats; i++) {
                    double angle = boss.getRandom().nextDouble() * Math.PI * 2;
                    double r = 2 + boss.getRandom().nextDouble() * 3;
                    Vec3d at = new Vec3d(boss.getX() + Math.cos(angle) * r, boss.getY(), boss.getZ() + Math.sin(angle) * r);
                    Mechanics.addHazard(BossFx.world(boss), at, 7, 30, boss);
                }
                BossFx.burst(boss, ParticleTypes.SNEEZE, 12, 1.5);
            }
            return damage;
        }
    }
}
