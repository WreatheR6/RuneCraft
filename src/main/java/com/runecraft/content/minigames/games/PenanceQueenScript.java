package com.runecraft.content.minigames.games;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The Penance Queen shrugs off most weapon damage: in OSRS she is brought down with omega eggs
 * fired from the egg launcher. Here weapons deal a quarter damage and the launcher's eggs do the
 * rest (see {@link BarbarianAssaultSession}).
 */
public class PenanceQueenScript implements BossScript {
    private long lastWarning;

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
        if (boss.age - lastWarning > 200) {
            lastWarning = boss.age;
            Msg.action(attacker, "The Penance Queen barely notices. Load eggs into the egg launcher!");
        }
        return damage / 4;
    }
}
