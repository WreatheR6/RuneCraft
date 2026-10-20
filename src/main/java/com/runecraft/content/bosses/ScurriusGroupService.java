package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.bosses.BossLoot;
import com.runecraft.osrs.content.bosses.ScurriusGroup;
import com.runecraft.osrs.data.Drop;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Scurrius's public-room (group) version ({@link ScurriusGroup}): summoned with another player in his lair he has the
 * group stats; every player who damaged him rolls loot, the MVP (most damage) on his regular table and the others on the
 * non-MVP table. Alone he is the solo version and drops his table as usual.
 */
final class ScurriusGroupService {
    private ScurriusGroupService() {
    }

    /** Damage each player dealt to each group Scurrius, and the players themselves. */
    private static final Map<LivingEntity, Map<UUID, Integer>> DAMAGE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingEntity, Map<UUID, ServerPlayer>> FIGHTERS = Collections.synchronizedMap(new WeakHashMap<>());

    static void register() {
        DataBossService.variant(ScurriusGroup.BOSS, (player, def) ->
                ScurriusGroup.group(playersNear(player)) ? ScurriusGroup.variant(def) : null);
        DataBossService.loot(ScurriusGroup.BOSS, ScurriusGroupService::roll);
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                if (dealt > 0 && isGroup(target)) {
                    credit(target, player, dealt);
                }
            }
        });
    }

    /** Players (the summoner included) within the lair's presence range of the summoner. */
    private static int playersNear(ServerPlayer summoner) {
        double range = com.runecraft.osrs.content.bosses.BossLair.PRESENCE_RANGE;
        int n = 0;
        for (ServerPlayer p : ((ServerLevel) summoner.level()).players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(summoner) < range * range) {
                n++;
            }
        }
        return n;
    }

    static boolean isGroup(LivingEntity e) {
        return e instanceof DataBossEntity d && d.variant() != null && ScurriusGroup.GROUP.equals(d.variant().kind())
                && d.def() != null && ScurriusGroup.BOSS.equals(d.def().id);
    }

    /** Records a fighter's damage (also the self-test's partner). */
    static void credit(LivingEntity boss, ServerPlayer p, int dealt) {
        DAMAGE.computeIfAbsent(boss, k -> new LinkedHashMap<>()).merge(p.getUUID(), dealt, Integer::sum);
        FIGHTERS.computeIfAbsent(boss, k -> new LinkedHashMap<>()).put(p.getUUID(), p);
    }

    /** The kill's loot: the killer's comes back to drop; the other fighters' go to them. */
    static List<Drops.Rolled> roll(ServerPlayer killer, DataBossEntity boss, List<Drop> table) {
        net.minecraft.util.RandomSource random = boss.getRandom();
        if (!isGroup(boss)) {
            return Drops.resolve(BossLoot.pick(ScurriusGroup.BOSS, table, random::nextInt), random);
        }
        Map<UUID, Integer> damage = DAMAGE.getOrDefault(boss, Map.of());
        UUID mvp = ScurriusGroup.mvp(damage);
        if (mvp == null) {
            mvp = killer.getUUID();
        }
        List<Drops.Rolled> mine = rollFor(killer.getUUID().equals(mvp), table, random);
        Map<UUID, ServerPlayer> fighters = FIGHTERS.getOrDefault(boss, Map.of());
        for (ServerPlayer p : new ArrayList<>(fighters.values())) {
            if (p == killer || !p.isAlive() || p.level() != boss.level() || p.distanceToSqr(boss) > 48 * 48) {
                continue;
            }
            for (Drops.Rolled r : rollFor(p.getUUID().equals(mvp), table, random)) {
                Inv.give(p, Drops.withDoses(CombatHooks.dropStack(p, boss, r.item(), r.count()), r.doses()));
                CollectionLog.obtain(p, r.item());
            }
            int kc = com.runecraft.player.Players.data(p).incrementKillCount(ScurriusGroup.BOSS);
            Msg.game(p, "Your Scurrius kill count is: " + kc + ".");
            if (boss.def() != null) {
                com.runecraft.content.GameEvents.fireKill(p, boss, com.runecraft.combat.MonsterStats.from(boss.def()));
            }
        }
        DAMAGE.remove(boss);
        FIGHTERS.remove(boss);
        return mine;
    }

    /** The MVP's roll on his regular table, or one roll on the non-MVP table. */
    private static List<Drops.Rolled> rollFor(boolean mvp, List<Drop> table, net.minecraft.util.RandomSource random) {
        List<Drop> lines = mvp ? BossLoot.pick(ScurriusGroup.BOSS, table, random::nextInt)
                : BossLoot.pick(ScurriusGroup.NON_MVP, 1, random::nextInt);
        return Drops.resolve(lines, random);
    }
}
