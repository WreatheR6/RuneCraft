package com.runecraft.content.bosses;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.ModernBosses;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

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
    private ServerPlayerEntity volleyTarget;
    private final List<AttackDef> volley = new ArrayList<>();

    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        if (!started) {
            started = true;
            stompNext = boss.getRandom().nextBoolean();
        }
        if (basics >= ModernBosses.DEMONIC_BASICS_PER_SPECIAL) {
            basics = 0;
            AttackDef special = stompNext ? ModernBosses.DEMONIC_STOMP : ModernBosses.DEMONIC_CHARGE;
            stompNext = !stompNext;
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
            return special;
        }
        basics++;
        return distance <= ModernBosses.DEMONIC_HORNS.range() ? ModernBosses.DEMONIC_HORNS : ModernBosses.DEMONIC_FIRE;
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
            if (volleyTarget.isAlive() && volleyTarget.squaredDistanceTo(boss) <= 24 * 24) {
                Mechanics.perform(boss, volleyTarget, next, 1.0);
            }
        }
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
        if (!secondPhase && damage >= boss.getHealth()) {
            secondPhase = true;
            revivePending = true;
            return Math.max(0, (int) Math.ceil(boss.getHealth()) - 1);
        }
        return damage;
    }
}
