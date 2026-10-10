package com.runecraft.content.minigames.games;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * TzKal-Zuk: the Ancestral Glyph slides back and forth in front of him; his typeless fire blast
 * is blocked only when the player stands behind the glyph. Zuk never moves.
 */
public class ZukScript implements BossScript {
    private double offset;
    private double direction = 0.5;

    private LivingEntity glyph(DataBossEntity boss) {
        List<LivingEntity> list = ((ServerWorld) boss.getWorld()).getEntitiesByClass(LivingEntity.class,
                boss.getBoundingBox().expand(30), e -> e.isAlive() && MinigameManager.creatureId(e).equals("ancestral_glyph"));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        LivingEntity glyph = glyph(boss);
        if (glyph == null) {
            return;
        }
        offset += direction;
        if (Math.abs(offset) >= 9) {
            direction = -direction;
        }
        glyph.requestTeleport(boss.getX() + offset, glyph.getY(), glyph.getZ());
        ((ServerWorld) boss.getWorld()).spawnParticles(ParticleTypes.FLAME, glyph.getX(), glyph.getY() + 2, glyph.getZ(), 2, 0.4, 0.6, 0.1, 0);
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
        LivingEntity glyph = glyph(boss);
        if (glyph != null) {
            boolean lined = Math.abs(player.getX() - glyph.getX()) < 1.8;
            boolean behind = Math.abs(player.getZ() - boss.getZ()) > Math.abs(glyph.getZ() - boss.getZ());
            if (lined && behind) {
                Msg.action(player, "The Ancestral Glyph absorbs TzKal-Zuk's blast.");
                return 0;
            }
        }
        return damage;
    }

    @Override
    public void onSpawn(DataBossEntity boss) {
        boss.setAiDisabled(false);
    }
}
