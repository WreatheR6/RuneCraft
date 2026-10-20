package com.runecraft.content.minigames.games;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.util.Msg;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * TzKal-Zuk: his typeless fire blast (max 148) is blocked only when the player stands behind the Ancestral Glyph, which the
 * Inferno session slides back and forth in front of him; nothing else stops it. He never moves, attacks every 10 ticks and
 * every 7 once enraged at 240 hitpoints. His 1,200 hitpoints are above Minecraft's 1,024 health cap, so incoming damage is
 * scaled to his health ({@link TzHaarWaves#zukHitpoints} reads them back).
 */
public class ZukScript implements BossScript, DataBossEntity.HpScaled {
    private boolean enraged;

    static LivingEntity glyph(DataBossEntity boss) {
        List<LivingEntity> list = ((ServerLevel) boss.level()).getEntitiesOfClass(LivingEntity.class,
                boss.getBoundingBox().inflate(30), e -> e.isAlive() && MinigameManager.creatureId(e).equals("ancestral_glyph"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** Whether the glyph stands between Zuk and the player (lined up east-west, and the player on its far side). */
    static boolean shielded(DataBossEntity boss, LivingEntity glyph, LivingEntity player) {
        if (glyph == null) {
            return false;
        }
        boolean lined = Math.abs(player.getX() - glyph.getX()) < glyph.getBbWidth() / 2 + 0.9;
        boolean behind = Math.abs(player.getZ() - boss.getZ()) > Math.abs(glyph.getZ() - boss.getZ());
        return lined && behind;
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        if (!enraged && TzHaarWaves.zukHitpoints(boss.getHealth(), boss.getMaxHealth()) <= TzHaarWaves.ZUK_HEALER_HP) {
            enraged = true;
            boss.overrideStats(s -> TzHaarSession.withSpeed(s, TzHaarWaves.ZUK_ENRAGED_SPEED));
        }
    }

    /** Whether Zuk attacks at his enraged speed. */
    public boolean enraged() {
        return enraged;
    }

    @Override
    public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (type != null && type.isMelee()) {
            // He floats in the lava beyond the platform: only ranged and magic reach him (OSRS Wiki "TzKal-Zuk").
            Msg.action(attacker, "TzKal-Zuk is out of reach of melee attacks.");
            return 0;
        }
        if (damage <= 0 || boss.getMaxHealth() >= TzHaarWaves.ZUK_HP) {
            return damage;
        }
        double scaled = damage * (double) boss.getMaxHealth() / TzHaarWaves.ZUK_HP;
        int whole = (int) scaled;
        return whole + (boss.getRandom().nextDouble() < scaled - whole ? 1 : 0);
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (shielded(boss, glyph(boss), player)) {
            Msg.action(player, "The Ancestral Glyph absorbs TzKal-Zuk's blast.");
            return 0;
        }
        return damage;
    }

    @Override
    public void onSpawn(DataBossEntity boss) {
        boss.setNoAi(false);
    }
}
