package com.runecraft.entity;

import com.runecraft.osrs.Quest;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Locale;

/** Summoning bosses at their lairs. */
public final class BossService {
    private BossService() {
    }

    public static void summon(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Boss boss) {
        EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(boss);
        if (!world.getEntitiesByType(type, new Box(pos).expand(48), e -> e.isAlive()).isEmpty()) {
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
        BlockPos spawn = pos.up().offset(player.getHorizontalFacing().getOpposite(), 0);
        entity.refreshPositionAndAngles(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0);
        entity.initialize(world, world.getLocalDifficulty(spawn), SpawnReason.EVENT, null, null);
        entity.setTarget(player);
        world.spawnEntity(entity);
        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.6f, 1.2f);
        Msg.red(player, boss.displayName() + " (level-" + boss.combatLevel() + ") emerges!");
    }
}
