package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.SkullService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.godwars.RevenantCaves;
import com.runecraft.osrs.content.godwars.RevenantRewards;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * The revenant maledictus (OSRS wiki): every revenant kill adds its combat level to a running total, and the chance of
 * the maledictus appearing is that total over a flat rate. It appears at one of eleven spots in the caves (players in the
 * caves are told whether in the northern, middle or southern part); the total resets and it can't appear again for 45
 * minutes; it leaves after 10 minutes if not killed. Its top damager gets an ancient emblem or totem, two rolls of the
 * revenant dragon's table and, wearing an amulet of avarice, the Forinthry Surge; everyone else who hurt it gets a
 * blighted super restore and two blighted foods.
 */
final class MaledictusService {
    private MaledictusService() {
    }

    private static long accumulated;
    private static long cooldownUntil;
    private static long lastSeen = -1;
    private static DataBossEntity active;

    static void onKill(ServerPlayer player, LivingEntity entity, MonsterStats stats) {
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
        if (entity.level() instanceof ServerLevel world) {
            appear(world, player, entity.getRandom().nextInt(RevenantCaves.MALEDICTUS_SPOTS.size()), entity);
        }
    }

    /**
     * Makes the maledictus appear at spot {@code spotIndex} (outside the carved caves, next to {@code near}); returns it, or
     * null when it can't (the self-test calls this directly).
     */
    static DataBossEntity appear(ServerLevel world, ServerPlayer target, int spotIndex, LivingEntity near) {
        BossDef def = ContentRegistry.boss(RevenantRewards.MALEDICTUS);
        if (def == null) {
            return null;
        }
        RevenantCaves.MaledictusSpot spot = RevenantCaves.MALEDICTUS_SPOTS.get(Math.floorMod(spotIndex, RevenantCaves.MALEDICTUS_SPOTS.size()));
        boolean inCaves = near == null || RevenantCavesService.inCaves(near);
        BlockPos at = inCaves ? new BlockPos(RevenantCaves.blockX(spot.x()) + 1, RevenantCaves.feetY(), RevenantCaves.blockZ(spot.y()) + 1)
                : near.blockPosition().offset(2, 0, 0);
        if (inCaves) {
            com.runecraft.world.StructurePlacer.applyAround(world, at.getX(), at.getZ(), 16);
        }
        DataBossEntity boss = DataBossService.spawn(world, at, def, target);
        if (boss == null) {
            return null;
        }
        long now = ActionManager.gameTick();
        active = boss;
        accumulated = 0;
        lastSeen = now;
        cooldownUntil = now + RevenantRewards.MALEDICTUS_COOLDOWN_TICKS;
        Component msg = Component.literal("A revenant maledictus has appeared in the " + spot.part() + " part of the caves!")
                .withStyle(ChatFormatting.DARK_PURPLE);
        for (ServerPlayer p : world.players()) {
            if (RevenantCavesService.inCaves(p) || p.distanceToSqr(boss) < 96 * 96) {
                p.displayClientMessage(msg, false);
            }
        }
        return boss;
    }

    /** Forgets the cooldown and running total (the self-test). */
    static void reset() {
        accumulated = 0;
        cooldownUntil = 0;
        lastSeen = -1;
        active = null;
    }

    /** Leaves after 10 minutes; rewards its top damager and everyone else who hurt it. */
    static final class Script implements BossScript {
        private long born = -1;
        private final Map<UUID, Integer> damage = new HashMap<>();

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (born < 0) {
                born = gameTick;
            } else if (gameTick - born >= RevenantRewards.MALEDICTUS_DESPAWN_TICKS) {
                for (ServerPlayer p : ((ServerLevel) boss.level()).getPlayers(o -> o.distanceToSqr(boss) < 48 * 48)) {
                    Msg.game(p, "The revenant maledictus fades back into the shadows.");
                }
                boss.discard();
            }
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int dmg, AttackType type) {
            if (attacker != null && dmg > 0) {
                damage.merge(attacker.getUUID(), dmg, Integer::sum);
            }
            return dmg;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            ServerLevel world = (ServerLevel) boss.level();
            ServerPlayer top = killer;
            int best = -1;
            for (Map.Entry<UUID, Integer> e : damage.entrySet()) {
                ServerPlayer p = world.getServer().getPlayerList().getPlayer(e.getKey());
                if (p != null && e.getValue() > best) {
                    best = e.getValue();
                    top = p;
                }
            }
            if (top == null) {
                return;
            }
            String artefact = boss.getRandom().nextBoolean() ? "ancient_emblem" : "ancient_totem";
            RevenantLoot.drop(top, boss, new RevenantRewards.Rolled(artefact, 1, false));
            boolean skulled = SkullService.isSkulled(top);
            RevenantRewards.Revenant dragon = RevenantRewards.revenant("revenant_dragon");
            for (int i = 0; i < RevenantRewards.MALEDICTUS_DRAGON_ROLLS; i++) {
                for (RevenantRewards.Rolled r : RevenantRewards.rollCommon(dragon, boss.getRandom()::nextInt)) {
                    RevenantLoot.drop(top, boss, r);
                }
                for (RevenantRewards.Rolled r : RevenantRewards.rollRares(RevenantRewards.REVENANT_DRAGON_LEVEL, skulled, false,
                        boss.getRandom()::nextInt)) {
                    RevenantLoot.drop(top, boss, r);
                }
            }
            if (RevenantLoot.wearingAvarice(top)) {
                RevenantBehaviour.grantSurge(top);
            }
            for (UUID id : damage.keySet()) {
                ServerPlayer p = world.getServer().getPlayerList().getPlayer(id);
                if (p != null && p != top) {
                    Inv.give(p, "blighted_super_restore", 1);
                    Inv.give(p, p.getRandom().nextBoolean() ? "blighted_manta_ray" : "blighted_anglerfish", 2);
                }
            }
        }
    }
}
