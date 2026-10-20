package com.runecraft.content.minigames.games;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.util.Msg;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Penance Queen can only be harmed by omega eggs fired from the egg launcher (OSRS Wiki): weapons, spells, poison and
 * recoil do nothing. The eggs' damage comes from {@link BarbarianAssaultSession}, past the session's damage gate; this script
 * also stops a weapon hit that reaches her some other way.
 */
public class PenanceQueenScript implements BossScript {
    private long lastWarning = -1000;

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (boss.tickCount - lastWarning > 200) {
            lastWarning = boss.tickCount;
            Msg.action(attacker, "The Penance Queen can only be harmed by omega eggs. Make them and load the egg launcher!");
        }
        return 0;
    }
}
