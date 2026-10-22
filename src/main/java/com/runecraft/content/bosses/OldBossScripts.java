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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

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
    public static String selfTestGrowthlings(BossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Bryophyta b)) {
            throw new IllegalStateException("Bryophyta has no growthling script");
        }
        // she only rolls for growthlings while a player is attacking her (wiki)
        b.lastAttacked = ActionManager.gameTick() - BossMechanics.BRYOPHYTA_ATTACKED_TICKS - 1;
        b.nextBatch = 0;
        for (int i = 0; i < 40; i++) {
            b.afterAttack(boss, p, AttackType.CRUSH);
        }
        if (!b.growthlings.isEmpty()) {
            throw new IllegalStateException("Bryophyta summoned growthlings while nobody attacked her");
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
        for (Mob g : new ArrayList<>(b.growthlings)) {
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
    public static String selfTestKnockback(BossEntity boss, ServerPlayer p) {
        double before = Math.hypot(p.getX() - boss.getX(), p.getZ() - boss.getZ());
        // the player has just swung (a 4-tick weapon): the slam adds to the weapon delay still running
        com.runecraft.combat.AttackTimer.accept(p, com.runecraft.osrs.AttackClock.Kind.MELEE, 4 * ActionManager.MC_TICKS_PER_GAME_TICK);
        long wait = com.runecraft.combat.AttackTimer.remaining(p);
        int tiles = Obor.knockback(boss, p);
        long delayed = com.runecraft.combat.AttackTimer.remaining(p) - wait;
        double after = Math.hypot(p.getX() - boss.getX(), p.getZ() - boss.getZ());
        if (tiles > 0 && after <= before) {
            throw new IllegalStateException("knockback reported " + tiles + " tiles but the player came no further away");
        }
        if (delayed < (long) BossMechanics.OBOR_SLAM_DELAY_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK) {
            throw new IllegalStateException("the slam delayed the player's next attack by " + delayed + " MC ticks");
        }
        if (boss.maxHitAgainst(p, AttackType.RANGED) != BossMechanics.OBOR_RANGED_MAX) {
            throw new IllegalStateException("Obor's ranged max hit is " + boss.maxHitAgainst(p, AttackType.RANGED));
        }
        if (boss.script() instanceof Obor o && !Obor.frozen(boss)) {
            double farSq = Math.pow(boss.getBbWidth() * 1.4 + 4, 2);
            long was = o.rangedFrom;
            o.rangedFrom = ActionManager.mcTick() + 100;
            AttackType walking = boss.chooseStyle(p, farSq);
            o.rangedFrom = 0;
            AttackType throwing = boss.chooseStyle(p, farSq);
            o.rangedFrom = was;
            if (walking != AttackType.CRUSH || throwing != AttackType.RANGED) {
                throw new IllegalStateException("out of reach Obor chose " + walking + " before his throw was due (expected CRUSH: "
                        + "walk to the player) and " + throwing + " once it was (expected RANGED)");
            }
        }
        return "pushed " + tiles + " tiles, next attack " + delayed + " MC later (" + String.format(java.util.Locale.ROOT, "%.1f", before) + " -> "
                + String.format(java.util.Locale.ROOT, "%.1f", after) + " blocks)";
    }

    /** Whether the player's held weapon finishes growthlings: an axe (not the blessed axe) or magic secateurs. */
    static boolean cutsGrowthling(ServerPlayer p) {
        ItemDef def = ModItems.def(p.getMainHandItem());
        return BossMechanics.cutsGrowthling(BossFx.heldId(p), def != null && def.kind() == ItemDef.Kind.AXE);
    }

    /**
     * Bryophyta: each attack has a 1/5 chance to summon three growthlings (one batch at a time, 20 ticks after the last one
     * dies). While any lives she takes no damage except poison and venom; only an axe or magic secateurs finishes one.
     */
    static final class Bryophyta implements BossEntity.Script {
        private final List<Mob> growthlings = new ArrayList<>();
        private long nextBatch;
        private long firstDeath = -1;
        private long lastDeath = -1;
        private int dead;
        private boolean warned;
        /** Game tick a player last attacked her: "She will only spawn growthlings if the player is attacking her" (wiki). */
        private long lastAttacked = -1000;

        boolean protectedByGrowthlings() {
            return growthlings.stream().anyMatch(Mob::isAlive);
        }

        @Override
        public void afterAttack(BossEntity boss, ServerPlayer target, AttackType type) {
            long now = ActionManager.gameTick();
            if (!growthlings.isEmpty() || now < nextBatch || !BossMechanics.bryophytaUnderAttack(now, lastAttacked)
                    || boss.getRandom().nextInt(BossMechanics.GROWTHLING_CHANCE) != 0) {
                return;
            }
            summon(boss, target);
        }

        /** Summons a batch of growthlings around her (also used by the self-test). */
        void summon(BossEntity boss, ServerPlayer target) {
            for (int i = 0; i < BossMechanics.GROWTHLINGS; i++) {
                double a = Math.PI * 2 * i / BossMechanics.GROWTHLINGS;
                Vec3 at = BossFx.ground(BossFx.world(boss), boss.getX() + Math.cos(a) * 3, boss.getZ() + Math.sin(a) * 3, boss.getY());
                Mob g = BossFx.spawnMinion(boss, BossMechanics.GROWTHLING, at, target);
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
            for (Mob g : new ArrayList<>(growthlings)) {
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
        public int modifyIncoming(BossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (attacker != null) {
                lastAttacked = ActionManager.gameTick();
            }
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
            growthlings.forEach(Mob::discard);
            growthlings.clear();
        }
    }

    /**
     * Obor: melee (22) in reach, a ranged attack (26, halved rather than blocked by Protect from Missiles) out of it, and a
     * knockback slam that pushes the target straight back unless a wall stops it.
     */
    static final class Obor implements BossEntity.Script {
        /** Game ticks (0-2, approx) Obor keeps walking after his attack is ready before throwing at a player out of reach. */
        static final int RANGED_WALK_TICKS = 2;
        /** Earliest Minecraft tick he may throw at a player out of melee reach (0: right away). */
        long rangedFrom;

        /**
         * Melee in reach. Out of reach he walks at the player (OSRS Wiki: snare him "to prevent Obor from reaching you"), and
         * only throws a rock once his attack is ready and he has walked a moment longer, or straight away while frozen.
         * A melee style makes the attack goal chase; a ranged one makes it stand and shoot.
         */
        @Override
        public AttackType chooseStyle(BossEntity boss, ServerPlayer target, double distanceSq, AttackType chosen) {
            if (boss.adjacent(distanceSq)) {
                return AttackType.CRUSH;
            }
            return frozen(boss) || ActionManager.mcTick() >= rangedFrom ? AttackType.RANGED : AttackType.CRUSH;
        }

        /** Whether a freeze or bind holds him in place (the same test as the "frozen" Combat Achievement rule). */
        static boolean frozen(BossEntity boss) {
            var slow = boss.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            return slow != null && slow.getAmplifier() >= 6;
        }

        @Override
        public int maxHit(BossEntity boss, ServerPlayer player, AttackType type, int max) {
            return type == AttackType.RANGED ? BossMechanics.OBOR_RANGED_MAX : BossMechanics.OBOR_MELEE_MAX;
        }

        @Override
        public int throughPrayer(BossEntity boss, AttackType type, int damage) {
            return type == AttackType.RANGED ? BossMechanics.oborRangedThroughPrayer(damage) : 0;
        }

        @Override
        public void afterAttack(BossEntity boss, ServerPlayer target, AttackType type) {
            rangedFrom = ActionManager.mcTick() + ((long) boss.stats().attackSpeed()
                    + boss.getRandom().nextInt(RANGED_WALK_TICKS + 1)) * ActionManager.MC_TICKS_PER_GAME_TICK;
            if (type.isMelee() && boss.getRandom().nextInt(BossMechanics.OBOR_KNOCKBACK_CHANCE) == 0) {
                knockback(boss, target);
            }
        }

        /** Pushes the target up to two tiles straight away from Obor; returns the whole tiles moved. */
        static int knockback(BossEntity boss, ServerPlayer target) {
            Vec3 dir = new Vec3(target.getX() - boss.getX(), 0, target.getZ() - boss.getZ());
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(1, 0, 0);
            }
            dir = dir.normalize();
            var w = BossFx.world(boss);
            Vec3 at = target.position();
            double moved = 0;
            int maxBlocks = BossMechanics.OBOR_KNOCKBACK_TILES * 2; // one OSRS tile is two blocks
            for (int step = 1; step <= maxBlocks; step++) {
                Vec3 next = target.position().add(dir.scale(step));
                BlockPos feet = BlockPos.containing(next);
                if (!w.getBlockState(feet).getCollisionShape(w, feet).isEmpty()
                        || !w.getBlockState(feet.above()).getCollisionShape(w, feet.above()).isEmpty()) {
                    break;
                }
                at = next;
                moved = step;
            }
            int tiles = (int) Math.floor(moved / 2.0);
            if (moved > 0) {
                target.teleportTo(at.x, at.y, at.z);
            }
            w.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            Msg.action(target, "Obor's slam knocks you back!");
            // OSRS Wiki: "your next attack will be slightly delayed whether you are standing next to a wall or not"
            com.runecraft.combat.AttackTimer.addDelay(target, BossMechanics.OBOR_SLAM_DELAY_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK);
            if (BossMechanics.oborPushedTooFar(tiles)) {
                CombatAchievementService.flag(target, BossMechanics.FLAG_OBOR_PUSHED);
            }
            return tiles;
        }
    }
}
