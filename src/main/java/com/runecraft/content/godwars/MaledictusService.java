package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.SkullService;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.godwars.RevenantRewards;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * The revenant maledictus (OSRS wiki): every revenant kill adds its combat level to a running total, and the chance of
 * the maledictus appearing next to the slain revenant is that total over a flat rate. When it appears the total resets
 * and it can't appear again for 45 minutes; it leaves after 10 minutes if not killed. Its killer gets an ancient emblem
 * or totem and two rolls of the revenant dragon's unique table.
 */
final class MaledictusService {
    private MaledictusService() {
    }

    private static long accumulated;
    private static long cooldownUntil;
    private static long lastSeen = -1;
    private static DataBossEntity active;

    static void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats) {
        if (stats == null || stats.profile() == null || !RevenantRewards.countsForMaledictus(stats.profile().id)) {
            return;
        }
        long now = ActionManager.gameTick();
        if (lastSeen < 0) {
            lastSeen = now;
        }
        if ((active != null && active.isAlive()) || now < cooldownUntil) {
            return;
        }
        accumulated += stats.combatLevel();
        boolean failsafe = now - lastSeen >= RevenantRewards.MALEDICTUS_FAILSAFE_TICKS;
        if (!failsafe && entity.getRandom().nextDouble() >= RevenantRewards.maledictusChance(accumulated)) {
            return;
        }
        BossDef def = ContentRegistry.boss(RevenantRewards.MALEDICTUS);
        if (def == null || !(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }
        BlockPos at = entity.getBlockPos().add(2, 0, 0);
        DataBossEntity boss = DataBossService.spawn(world, at, def, player);
        if (boss == null) {
            return;
        }
        active = boss;
        accumulated = 0;
        lastSeen = now;
        cooldownUntil = now + RevenantRewards.MALEDICTUS_COOLDOWN_TICKS;
        for (ServerPlayerEntity p : world.getPlayers(o -> o.squaredDistanceTo(entity) < 96 * 96)) {
            p.sendMessage(Text.literal("A revenant maledictus has appeared nearby!").formatted(Formatting.DARK_PURPLE), false);
        }
    }

    /** Leaves after 10 minutes; rewards its killer. */
    static final class Script implements BossScript {
        private long born = -1;

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (born < 0) {
                born = gameTick;
            } else if (gameTick - born >= RevenantRewards.MALEDICTUS_DESPAWN_TICKS) {
                for (ServerPlayerEntity p : ((ServerWorld) boss.getWorld()).getPlayers(o -> o.squaredDistanceTo(boss) < 48 * 48)) {
                    Msg.game(p, "The revenant maledictus fades back into the shadows.");
                }
                boss.discard();
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            if (killer == null) {
                return;
            }
            String artefact = boss.getRandom().nextBoolean() ? "ancient_emblem" : "ancient_totem";
            give(boss, killer, artefact);
            boolean skulled = SkullService.isSkulled(killer);
            for (int i = 0; i < RevenantRewards.MALEDICTUS_DRAGON_ROLLS; i++) {
                for (String item : RevenantRewards.roll(RevenantRewards.REVENANT_DRAGON_LEVEL, skulled, false,
                        boss.getRandom()::nextInt)) {
                    give(boss, killer, item);
                }
            }
        }

        private static void give(DataBossEntity boss, ServerPlayerEntity killer, String item) {
            boss.dropStack(CombatHooks.dropStack(killer, boss, item, 1));
            CollectionLog.obtain(killer, item);
        }
    }
}
