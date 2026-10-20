package com.runecraft.content.bosses;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.ModernBosses;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;

/**
 * Demonic Brutus (OSRS Wiki): three basic attacks, then a special, stomp and charge alternating (the first one random).
 * At 0 hitpoints he cries "Moo moo!" and heals 487 to start phase 2, where every special is accompanied by four ghost
 * projectiles: magic, melee and ranged, then a random style.
 */
final class DemonicBrutusScript implements BossScript {
    private boolean started;
    private boolean stompNext;
    private int basics;
    private boolean secondPhase;
    private boolean revivePending;
    private ServerPlayer volleyTarget;
    private final List<AttackDef> volley = new ArrayList<>();

    /**
     * The next attack. The attack goal asks for one every tick (to know whether to chase), so nothing is counted here: the
     * attack actually made is counted in {@link #beforeAttack}. (Counting here made a special every few ticks and, in phase 2,
     * a fresh ghost volley every few ticks: a ghost hit every game tick.)
     */
    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        if (!started) {
            started = true;
            stompNext = boss.getRandom().nextBoolean();
        }
        if (basics >= ModernBosses.DEMONIC_BASICS_PER_SPECIAL) {
            return stompNext ? ModernBosses.DEMONIC_STOMP : ModernBosses.DEMONIC_CHARGE;
        }
        return distance <= ModernBosses.DEMONIC_HORNS.range() ? ModernBosses.DEMONIC_HORNS : ModernBosses.DEMONIC_FIRE;
    }

    @Override
    public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
        if (attack != ModernBosses.DEMONIC_STOMP && attack != ModernBosses.DEMONIC_CHARGE) {
            basics++;
            return attack;
        }
        basics = 0;
        stompNext = attack != ModernBosses.DEMONIC_STOMP;
        if (secondPhase) {
            volley.clear();
            volley.add(ModernBosses.DEMONIC_GHOST_MAGIC);
            volley.add(ModernBosses.DEMONIC_GHOST_MELEE);
            volley.add(ModernBosses.DEMONIC_GHOST_RANGED);
            int r = boss.getRandom().nextInt(3);
            volley.add(r == 0 ? ModernBosses.DEMONIC_GHOST_MAGIC : r == 1 ? ModernBosses.DEMONIC_GHOST_MELEE : ModernBosses.DEMONIC_GHOST_RANGED);
            volleyTarget = target;
            BossFx.warn(boss, "Brutus' ghosts gather around you: magic, melee, ranged...");
        }
        return attack;
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        if (revivePending) {
            revivePending = false;
            boss.setHealth(Math.min(boss.getMaxHealth(), ModernBosses.DEMONIC_REVIVE_HP));
            BossFx.say(boss, "Demonic Brutus: Moo moo!");
            BossFx.burst(boss, ParticleTypes.FLAME, 40, 1.2);
        }
        if (!volley.isEmpty() && volleyTarget != null) {
            AttackDef next = volley.remove(0);
            if (volleyTarget.isAlive() && volleyTarget.distanceToSqr(boss) <= 24 * 24) {
                Mechanics.perform(boss, volleyTarget, next, 1.0);
            }
        }
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (!secondPhase && damage >= boss.getHealth()) {
            secondPhase = true;
            revivePending = true;
            return Math.max(0, (int) Math.ceil(boss.getHealth()) - 1);
        }
        return damage;
    }
}
