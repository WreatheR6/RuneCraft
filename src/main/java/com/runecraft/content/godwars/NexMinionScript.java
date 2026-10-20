package com.runecraft.content.godwars;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fumus, Umbra, Cruor and Glacies: they wait in the corners of Nex's prison, immune and silent,
 * until Nex reaches their phase threshold. Then they attack and must be killed before Nex can be
 * damaged again. They vanish if Nex is gone.
 */
final class NexMinionScript implements BossScript {
    /** Movement-only placeholder used while dormant (the engine performs nothing for it). */
    private static final AttackDef IDLE = AttackDef.idle(AttackType.MAGIC);

    DataBossEntity nex;
    boolean active;

    @Override
    public boolean isVulnerable(DataBossEntity boss) {
        return active;
    }

    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        return active ? null : IDLE;
    }

    @Override
    public void tick(DataBossEntity boss) {
        boss.getNavigation().stop();
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        if (nex == null || nex.isRemoved() || !nex.isAlive()) {
            boss.discard();
        }
    }
}
