package com.runecraft.content.minigames.games;

import com.runecraft.entity.BossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import net.minecraft.server.level.ServerPlayer;

/**
 * TzTok-Jad (the built-in boss of the Fight Cave): his bite is a stab, used only next to his target, and his magic fireball
 * hits up to 95 where his melee and ranged attacks reach 97 (OSRS Wiki "TzTok-Jad"). The telegraphs (rearing up for magic,
 * stomping for ranged, the hit landing 3 ticks later) are {@code BossEntity}'s Jad mechanic.
 */
public final class TzTokJadScript implements BossEntity.Script {
    @Override
    public AttackType chooseStyle(BossEntity boss, ServerPlayer target, double distanceSq, AttackType chosen) {
        return chosen.isMelee() ? AttackType.STAB : chosen;
    }

    @Override
    public int maxHit(BossEntity boss, ServerPlayer player, AttackType type, int max) {
        return type == AttackType.MAGIC ? TzHaarWaves.JAD_MAGIC_MAX : max;
    }
}
