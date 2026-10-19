package com.runecraft.entity;

import com.runecraft.osrs.Quest;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

/** Summoning bosses at their lairs. */
public final class BossService {
    private BossService() {
    }

    public static void summon(ServerPlayer player, ServerLevel world, BlockPos pos, Boss boss) {
        EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(boss);
        if (!world.getEntities(type, new AABB(pos).inflate(48), e -> e.isAlive()).isEmpty()) {
            Msg.game(player, boss.displayName() + " is already here!");
            return;
        }
        PlayerData data = Players.data(player);
        if (boss == Boss.COUNT_DRAYNOR && data.questStage(Quest.VAMPYRE_SLAYER) != 3) {
            Msg.game(player, "The coffin is sealed shut. Perhaps Morgan in Draynor Village knows something about it.");
            return;
        }
        if (boss == Boss.DELRITH && data.questStage(Quest.DEMON_SLAYER) != 4) {
            Msg.game(player, "The stones hum faintly. Gypsy Aris in Varrock might know their meaning.");
            return;
        }
        if (boss.key() != null) {
            if (!Inv.has(player, boss.key(), 1)) {
                Msg.game(player, "The gate is locked. You need " + Msg.article(boss.key().replace('_', ' ')) + " to enter.");
                return;
            }
            Inv.remove(player, boss.key(), 1);
            Msg.game(player, "You unlock the gate with your " + boss.key().replace('_', ' ').toLowerCase(Locale.ROOT) + ".");
        }
        BossEntity entity = type.create(world);
        if (entity == null) {
            return;
        }
        BlockPos spawn = pos.above().relative(player.getDirection().getOpposite(), 0);
        entity.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0);
        entity.finalizeSpawn(world, world.getCurrentDifficultyAt(spawn), MobSpawnType.EVENT, null, null);
        entity.setTarget(player);
        world.addFreshEntity(entity);
        spawnBodyguards(world, boss, spawn, player);
        world.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 1.2f);
        Msg.red(player, boss.displayName() + " (level-" + boss.combatLevel() + ") emerges!");
    }

    /** Spawns the boss's bodyguards around it (skipping any still alive from an earlier fight). */
    private static void spawnBodyguards(ServerLevel world, Boss boss, BlockPos center, ServerPlayer player) {
        int i = 0;
        for (String id : boss.bodyguards()) {
            double angle = Math.PI * 2 * i++ / Math.max(1, boss.bodyguards().size());
            EntityType<?> type = ModEntities.typeById(id);
            if (type == null || !world.getEntities(type, new AABB(center).inflate(48), e -> e.isAlive()).isEmpty()) {
                continue;
            }
            if (type.create(world) instanceof net.minecraft.world.entity.Mob mob) {
                mob.moveTo(center.getX() + 0.5 + Math.cos(angle) * 4, center.getY(),
                        center.getZ() + 0.5 + Math.sin(angle) * 4, 0, 0);
                mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
                mob.setTarget(player);
                world.addFreshEntity(mob);
            }
        }
    }
}
