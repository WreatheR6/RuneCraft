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

    /** Umbra's Shadow Barrage lowers the Attack level of a player it hits by 15% (OSRS Wiki, Umbra). */
    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (damage > 0 && boss.def() != null && "umbra".equals(boss.def().id)) {
            var skills = com.runecraft.player.Players.data(player).skills();
            int drain = com.runecraft.osrs.content.godwars.GwdBosses.umbraDrain(skills.currentLevel(com.runecraft.osrs.Skill.ATTACK));
            if (drain > 0) {
                skills.setBoost(com.runecraft.osrs.Skill.ATTACK, skills.boost(com.runecraft.osrs.Skill.ATTACK) - drain);
                com.runecraft.network.ServerNetworking.sync(player);
            }
        }
        return damage;
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        if (nex == null || nex.isRemoved() || !nex.isAlive()) {
            boss.discard();
        }
    }
}
