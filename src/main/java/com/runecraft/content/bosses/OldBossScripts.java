package com.runecraft.content.bosses;

import com.runecraft.action.ActionManager;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.entity.BossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.data.Boss;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Msg;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Scripts for the old-style {@link BossEntity} bosses: Bryophyta's growthlings and Obor's ranged attack and knockback
 * (OSRS Wiki). Registered from {@link BossesHooks}.
 */
public final class OldBossScripts {
    private OldBossScripts() {
    }

    static void register() {
        BossEntity.registerScript(Boss.BRYOPHYTA, Bryophyta::new);
        BossEntity.registerScript(Boss.OBOR, Obor::new);
    }

    /**
     * Self-test (boss:bryophyta_growthlings): a batch of three growthlings appears, Bryophyta takes no damage while any
     * lives, takes damage again once they're dead, and the next batch waits for the cooldown. Throws on a failure.
     */
    public static String selfTestGrowthlings(BossEntity boss, ServerPlayerEntity p) {
        if (!(boss.script() instanceof Bryophyta b)) {
            throw new IllegalStateException("Bryophyta has no growthling script");
        }
        b.summon(boss, p);
        int n = b.growthlings.size();
        if (n != BossMechanics.GROWTHLINGS) {
            throw new IllegalStateException(n + " growthlings summoned, expected " + BossMechanics.GROWTHLINGS);
        }
        int blocked = boss.modifyIncoming(p, 10, AttackType.SLASH);
        if (blocked != 0) {
            throw new IllegalStateException("Bryophyta took " + blocked + " while her growthlings lived");
        }
        for (MobEntity g : new ArrayList<>(b.growthlings)) {
            g.discard();
        }
        b.gameTick(boss);
        if (!b.growthlings.isEmpty()) {
            throw new IllegalStateException(b.growthlings.size() + " growthlings still tracked after dying");
        }
        int hit = boss.modifyIncoming(p, 10, AttackType.SLASH);
        if (hit != 10) {
            throw new IllegalStateException("Bryophyta took " + hit + " (expected 10) after her growthlings died");
        }
        if (b.nextBatch <= ActionManager.gameTick()) {
            throw new IllegalStateException("no cooldown before the next batch");
        }
        return n + " growthlings, immune while alive, cooldown " + (b.nextBatch - ActionManager.gameTick()) + " ticks";
    }

    /** Self-test (boss:obor_knockback): the slam pushes a player standing in the open straight away from Obor. */
    public static String selfTestKnockback(BossEntity boss, ServerPlayerEntity p) {
        double before = Math.hypot(p.getX() - boss.getX(), p.getZ() - boss.getZ());
        int tiles = Obor.knockback(boss, p);
        double after = Math.hypot(p.getX() - boss.getX(), p.getZ() - boss.getZ());
        if (tiles > 0 && after <= before) {
            throw new IllegalStateException("knockback reported " + tiles + " tiles but the player came no further away");
        }
        if (boss.maxHitAgainst(p, AttackType.RANGED) != BossMechanics.OBOR_RANGED_MAX) {
            throw new IllegalStateException("Obor's ranged max hit is " + boss.maxHitAgainst(p, AttackType.RANGED));
        }
        return "pushed " + tiles + " tiles (" + String.format(java.util.Locale.ROOT, "%.1f", before) + " -> "
                + String.format(java.util.Locale.ROOT, "%.1f", after) + " blocks)";
    }

    /** Whether the player's held weapon finishes growthlings: an axe (not the blessed axe) or magic secateurs. */
    static boolean cutsGrowthling(ServerPlayerEntity p) {
        ItemDef def = ModItems.def(p.getMainHandStack());
        return BossMechanics.cutsGrowthling(BossFx.heldId(p), def != null && def.kind() == ItemDef.Kind.AXE);
    }

    /**
     * Bryophyta: each attack has a 1/5 chance to summon three growthlings (one batch at a time, 20 ticks after the last one
     * dies). While any lives she takes no damage except poison and venom; only an axe or magic secateurs finishes one.
     */
    static final class Bryophyta implements BossEntity.Script {
        private final List<MobEntity> growthlings = new ArrayList<>();
        private long nextBatch;
        private long firstDeath = -1;
        private long lastDeath = -1;
        private int dead;
        private boolean warned;

        boolean protectedByGrowthlings() {
            return growthlings.stream().anyMatch(MobEntity::isAlive);
        }

        @Override
        public void afterAttack(BossEntity boss, ServerPlayerEntity target, AttackType type) {
            long now = ActionManager.gameTick();
            if (!growthlings.isEmpty() || now < nextBatch || boss.getRandom().nextInt(BossMechanics.GROWTHLING_CHANCE) != 0) {
                return;
            }
            summon(boss, target);
        }

        /** Summons a batch of growthlings around her (also used by the self-test). */
        void summon(BossEntity boss, ServerPlayerEntity target) {
            for (int i = 0; i < BossMechanics.GROWTHLINGS; i++) {
                double a = Math.PI * 2 * i / BossMechanics.GROWTHLINGS;
                Vec3d at = BossFx.ground(BossFx.world(boss), boss.getX() + Math.cos(a) * 3, boss.getZ() + Math.sin(a) * 3, boss.getY());
                MobEntity g = BossFx.spawnMinion(boss, BossMechanics.GROWTHLING, at, target);
                if (g == null) {
                    continue;
                }
                growthlings.add(g);
                // Other weapons can't take one below 1 hitpoint; an axe or magic secateurs cuts it down in one hit.
                MinionRules.set(g, (p, mob, t, d) -> cutsGrowthling(p) ? MinionRules.lethal(mob)
                        : Math.min(d, Math.max(0, (int) Math.ceil(mob.getHealth()) - 1)));
            }
            firstDeath = -1;
            lastDeath = -1;
            dead = 0;
            warned = false;
            if (!growthlings.isEmpty()) {
                BossFx.warn(boss, "Bryophyta summons growthlings! Cut them down with an axe.");
            }
        }

        @Override
        public void gameTick(BossEntity boss) {
            if (growthlings.isEmpty()) {
                return;
            }
            long now = ActionManager.gameTick();
            for (MobEntity g : new ArrayList<>(growthlings)) {
                if (!g.isAlive()) {
                    growthlings.remove(g);
                    dead++;
                    if (firstDeath < 0) {
                        firstDeath = now;
                    }
                    lastDeath = now;
                }
            }
            if (growthlings.isEmpty()) {
                if (BossMechanics.quickCut(firstDeath, lastDeath, dead)) {
                    CombatAchievementService.flagNear(boss, BossMechanics.FLAG_BRYOPHYTA_QUICK_CUT);
                }
                nextBatch = now + BossMechanics.GROWTHLING_COOLDOWN;
            }
        }

        @Override
        public int modifyIncoming(BossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (!protectedByGrowthlings()) {
                return damage;
            }
            if (attacker != null && !warned) {
                warned = true;
                Msg.game(attacker, "Bryophyta is protected by her growthlings!");
            }
            return 0;
        }

        @Override
        public void onRemoved(BossEntity boss) {
            growthlings.forEach(MobEntity::discard);
            growthlings.clear();
        }
    }

    /**
     * Obor: melee (22) in reach, a ranged attack (26, halved rather than blocked by Protect from Missiles) out of it, and a
     * knockback slam that pushes the target straight back unless a wall stops it.
     */
    static final class Obor implements BossEntity.Script {
        @Override
        public AttackType chooseStyle(BossEntity boss, ServerPlayerEntity target, double distanceSq, AttackType chosen) {
            return boss.adjacent(distanceSq) ? AttackType.CRUSH : AttackType.RANGED;
        }

        @Override
        public int maxHit(BossEntity boss, ServerPlayerEntity player, AttackType type, int max) {
            return type == AttackType.RANGED ? BossMechanics.OBOR_RANGED_MAX : BossMechanics.OBOR_MELEE_MAX;
        }

        @Override
        public int throughPrayer(BossEntity boss, AttackType type, int damage) {
            return type == AttackType.RANGED ? BossMechanics.oborRangedThroughPrayer(damage) : 0;
        }

        @Override
        public void afterAttack(BossEntity boss, ServerPlayerEntity target, AttackType type) {
            if (type.isMelee() && boss.getRandom().nextInt(BossMechanics.OBOR_KNOCKBACK_CHANCE) == 0) {
                knockback(boss, target);
            }
        }

        /** Pushes the target up to two tiles straight away from Obor; returns the whole tiles moved. */
        static int knockback(BossEntity boss, ServerPlayerEntity target) {
            Vec3d dir = new Vec3d(target.getX() - boss.getX(), 0, target.getZ() - boss.getZ());
            if (dir.lengthSquared() < 1.0E-4) {
                dir = new Vec3d(1, 0, 0);
            }
            dir = dir.normalize();
            var w = BossFx.world(boss);
            Vec3d at = target.getPos();
            double moved = 0;
            int maxBlocks = BossMechanics.OBOR_KNOCKBACK_TILES * 2; // one OSRS tile is two blocks
            for (int step = 1; step <= maxBlocks; step++) {
                Vec3d next = target.getPos().add(dir.multiply(step));
                BlockPos feet = BlockPos.ofFloored(next);
                if (!w.getBlockState(feet).getCollisionShape(w, feet).isEmpty()
                        || !w.getBlockState(feet.up()).getCollisionShape(w, feet.up()).isEmpty()) {
                    break;
                }
                at = next;
                moved = step;
            }
            int tiles = (int) Math.floor(moved / 2.0);
            if (moved > 0) {
                target.requestTeleport(at.x, at.y, at.z);
            }
            w.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            Msg.action(target, "Obor's slam knocks you back!");
            if (BossMechanics.oborPushedTooFar(tiles)) {
                CombatAchievementService.flag(target, BossMechanics.FLAG_OBOR_PUSHED);
            }
            return tiles;
        }
    }
}
