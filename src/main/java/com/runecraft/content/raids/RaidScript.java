package com.runecraft.content.raids;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Boss script shared by every raid boss. Encounter mechanics live in the room's {@link RoomLogic}
 * (one place per encounter, with access to the raid); this script forwards the boss hooks there and
 * applies raid-wide rules: ToA damage scaling and downed players being untouchable.
 */
public final class RaidScript implements BossScript {
    private RoomLogic room(DataBossEntity boss) {
        RaidInstance raid = RaidManager.at(boss);
        if (raid == null) {
            return null;
        }
        int i = raid.roomAt(boss.getPos());
        return i >= 0 ? raid.logic(i) : raid.logic();
    }

    @Override
    public void tick(DataBossEntity boss) {
        RoomLogic r = room(boss);
        if (r != null) {
            r.keepAnchored(boss);
            r.bossTick(boss);
        }
    }

    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        RoomLogic r = room(boss);
        return r == null ? null : r.bossAttack(boss, target, distance);
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
        RoomLogic r = room(boss);
        return r == null ? damage : r.bossIncoming(boss, attacker, damage, type);
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
        RaidInstance raid = RaidManager.at(boss);
        if (raid == null) {
            return damage;
        }
        if (raid.isDowned(player)) {
            return 0;
        }
        RoomLogic r = room(boss);
        if (damage == 0 && type != null && raid.type == com.runecraft.osrs.content.raids.RaidType.TOA
                && RaidEffects.protectedFrom(player, type)) {
            // ToA Quiet Prayers (OSRS): protection prayers are 10% less effective. // approx: the blocked roll is redrawn
            double leak = com.runecraft.osrs.content.raids.Invocation.protectionLeak(raid.invocations());
            int max = 0;
            for (AttackDef a : RoomLogic.attacks(boss)) {
                if (a.style() == type) {
                    max = Math.max(max, a.maxHit());
                }
            }
            if (leak > 0 && max > 0) {
                damage = (int) Math.round(boss.getRandom().nextInt(max + 1) * leak);
            }
        }
        int d = r == null ? damage : r.bossOutgoing(boss, player, damage, type);
        int out = (int) Math.round(d * raid.damageMultiplier(boss));
        RaidManager.deadlyPrayers(player, out);
        return out;
    }

    @Override
    public boolean isVulnerable(DataBossEntity boss) {
        RoomLogic r = room(boss);
        return r == null || r.bossVulnerable(boss);
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
        RoomLogic r = room(boss);
        if (r != null) {
            r.bossDeath(boss, killer);
        }
    }
}
