package com.runecraft.content.raids;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.RealReaction;
import com.runecraft.selftest.RealPlayer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The no-heal pass's raid rooms (round 44 playability, {@code raid:noheal:<raid>}): one combat room of each raid fought by a
 * player who is NOT invulnerable and is never topped up, with {@value #FOOD} sharks and {@value #PRAYER_POTIONS} prayer potions
 * ({@link RealPlayer#noHeal}). It prays on the cues (the raid projectiles' cast-bar rows), walks out of the strikes and floor
 * marks, kills the room's adds a reaction time after they appear and attacks the room's boss at a player's pace. A hit that
 * would have killed it fails the check.
 */
public final class RaidNoHealWalk {
    static final int FOOD = 10;
    static final int PRAYER_POTIONS = 4;
    /** The player's attack speed (game ticks). */
    private static final int PLAYER_SPEED = 4;
    /** Game ticks a room is fought at most. */
    static final int ROOM_TICKS = 220;
    /** Attacks that took nothing off an add before the player leaves it be (a Hard mode blood spawn can't be hurt). */
    private static final int ADD_TRIES = 2;
    private static final int REACT_TICKS = (RealReaction.REACTION_MC + 11) / 12;

    private RaidNoHealWalk() {
    }

    /** The walk the room is fought in: its game ticks (the real player ticked every Minecraft tick) and the player's hits. */
    interface Room {
        RaidInstance raid();

        ServerPlayer player();

        void tick(int n);

        int hit(LivingEntity target, int damage, AttackType type);
    }

    /** Fights room {@code index} (just walked into) without healing; returns a summary or throws {@link AssertionError}. */
    static String fight(Room r, int index, String what) {
        RaidInstance raid = r.raid();
        ServerPlayer p = r.player();
        RoomLogic logic = raid.logic(index);
        DataBossEntity first = logic.hudBoss();
        if (first == null || !first.isAlive()) {
            throw new AssertionError(what + ": no boss in the room to fight");
        }
        com.runecraft.skill.XpService.updateMaxHealth(p);
        p.setHealth(p.getMaxHealth());
        com.runecraft.player.Players.data(p).setPrayerPoints(99);
        // dressed as a raider (melee armour: Ba-Ba, Tekton and the Maiden are meleed), the pass's own hits don't use it
        RealPlayer real = RealPlayer.start(p).autoPray(true).autoDodge(true).noHeal(FOOD, PRAYER_POTIONS)
                .wear(RealPlayer.MELEE_ARMOUR);
        Map<LivingEntity, int[]> adds = new HashMap<>(); // {first seen tick, attacks without damage}
        int planned = Math.max(4, ROOM_TICKS / PLAYER_SPEED - 8);
        double boost = 1.0;
        int nextAttack = 0;
        int attacks = 0;
        int killedAdds = 0;
        int tick = 0;
        try {
            for (; tick < ROOM_TICKS && !logic.done && real.death() == null; tick++) {
                r.tick(1);
                if (tick < nextAttack || logic.done || real.death() != null) {
                    continue;
                }
                DataBossEntity boss = logic.hudBoss();
                // the room's adds first, each a reaction time after it appears, unless it can't be hurt
                LivingEntity add = null;
                for (LivingEntity e : raid.world.getEntitiesOfClass(LivingEntity.class, room(raid, index),
                        e -> e.isAlive() && e instanceof com.runecraft.entity.OsrsMonsterEntity && !e.isInvulnerable()
                                && raid.inRoom(e.position(), index))) {
                    if (e == boss || e instanceof DataBossEntity) {
                        continue;
                    }
                    int[] seen = adds.computeIfAbsent(e, k -> new int[]{0, 0});
                    if (seen[0] == 0) {
                        seen[0] = tick + 1;
                    }
                    if (tick + 1 - seen[0] >= REACT_TICKS && seen[1] < ADD_TRIES) {
                        add = e;
                        break;
                    }
                }
                if (add != null) {
                    int dealt = r.hit(add, (int) Math.ceil(add.getHealth()) + 1, AttackType.MAGIC);
                    adds.get(add)[1] += dealt > 0 ? 0 : 1;
                    killedAdds += add.isAlive() ? 0 : 1;
                    nextAttack = tick + PLAYER_SPEED;
                    attacks++;
                    continue;
                }
                if (boss == null || !boss.isAlive()) {
                    continue;
                }
                double share = 1.15 / planned;
                int max = Math.max(1, raid.maxHitpoints(boss));
                int before = raid.hitpoints(boss);
                int want = Math.max(5, (int) Math.ceil(max * share * boost));
                for (AttackType type : new AttackType[]{AttackType.SLASH, AttackType.MAGIC, AttackType.RANGED}) {
                    if (r.hit(boss, want, type) > 0) {
                        break;
                    }
                }
                nextAttack = tick + PLAYER_SPEED;
                attacks++;
                double took = (before - (boss.isAlive() ? raid.hitpoints(boss) : 0)) / (double) max;
                boost = took < share / 3 ? Math.min(8, boost * 2) : took > share * 1.5 ? Math.max(1, boost / 2) : boost;
            }
            real.assertFair(what);
            DataBossEntity boss = logic.hudBoss();
            String left = boss != null && boss.isAlive() ? String.format(Locale.ROOT, ", boss at %d/%d", raid.hitpoints(boss),
                    raid.maxHitpoints(boss)) : "";
            return (logic.done ? "cleared" : "survived") + " in " + tick + " game ticks (" + attacks + " attacks, " + killedAdds
                    + " adds killed" + left + "); " + real.summary();
        } finally {
            real.close();
        }
    }

    private static AABB room(RaidInstance raid, int index) {
        Vec3 c = Vec3.atCenterOf(raid.center(index));
        return new AABB(c, c).inflate(RaidBuilder.HALF + 2, 12, RaidBuilder.HALF + 2);
    }
}
