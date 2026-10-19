package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.Equipment;
import com.runecraft.combat.HitCalcBridge;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * The Moons of Peril (zeah module bosses; OSRS Wiki "Moons of Peril/Strategies"): the Moons' flat armour, and the Eclipse
 * Moon's Clone special. The Moons' attacks are their own boss definitions' (looked up by name).
 */
final class MoonScripts {
    private MoonScripts() {
    }

    private static AttackDef attack(DataBossEntity boss, String name) {
        BossDef def = boss.def();
        if (def == null) {
            return null;
        }
        for (AttackDef a : def.attacks) {
            if (a.name().equals(name)) {
                return a;
            }
        }
        return null;
    }

    /**
     * A hit after a Moon's flat armour (OSRS Wiki "Flat armour"): only successful melee and ranged hits change; player magic
     * (powered staves too) and thralls ignore it.
     */
    static int flatArmour(int damage, int armour, AttackType type) {
        if (type == AttackType.MAGIC || com.runecraft.content.systems.SpellService.thrallHitting()) {
            return damage;
        }
        return BossMechanics.flatArmour(damage, armour);
    }

    /** Blue Moon: -5 flat armour, so every melee or ranged hit that lands on her deals 5 more. */
    static final class BlueMoon extends ScriptedBoss {
        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            return flatArmour(damage, BossMechanics.BLUE_MOON_FLAT_ARMOUR, type);
        }
    }

    /**
     * Eclipse Moon: 6 flat armour (4 during the Clone special). Her specials alternate: the eclipse (standing in for the
     * Searing Rays) and the Clones - the player is bound in place and fifteen clones (five cycles of three) appear around
     * them one at a time; facing a clone as it attacks parries it, which sends a melee hit back at the Moon that always
     * lands; a clone that isn't faced hits the player. approx: the player is bound where they stand rather than shoved to
     * the centre of the arena; clone timing is not documented.
     */
    static final class EclipseMoon extends ScriptedBoss {
        private static final int SPECIAL_EVERY = 7; // approx: a special every 7 attacks (OSRS: on the full/new moon sigil)
        private int cloneStep = -1;
        private long cloneNext;
        private Vec3d clone;
        private ServerPlayerEntity bound;
        private boolean nextClones = true;
        private boolean parrying;

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayerEntity target) {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return cloneStep < 0;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (attackCount % SPECIAL_EVERY == SPECIAL_EVERY - 1) {
                boolean clones = nextClones;
                nextClones = !nextClones;
                if (clones) {
                    startClones(boss, target);
                    attackCount++;
                    cooldown = 2;
                    return null;
                }
                AttackDef eclipse = attack(boss, "eclipse");
                if (eclipse != null) {
                    return eclipse;
                }
            }
            AttackDef bash = attack(boss, "shield bash");
            AttackDef dart = attack(boss, "atlatl dart");
            if (bash != null && distance <= bash.range() && (dart == null || boss.getRandom().nextInt(3) > 0)) {
                return bash;
            }
            return dart != null ? dart : bash;
        }

        void startClones(DataBossEntity boss, ServerPlayerEntity target) {
            bound = target;
            cloneStep = 0;
            clone = null;
            cloneNext = now + 2;
            int ticks = BossMechanics.ECLIPSE_CLONE_CYCLES * BossMechanics.ECLIPSE_CLONES_PER_CYCLE * (BossMechanics.ECLIPSE_CLONE_INTERVAL + 1)
                    + 2 * BossMechanics.ECLIPSE_CLONE_CYCLES + 4;
            BossFx.stun(target, ticks);
            CombatAchievementService.countNear(boss, "used:moonlight_clones");
            BossFx.warn(boss, "The Eclipse Moon binds you in place as her clones surround you... face each one to parry it!");
        }

        private void endClones() {
            if (bound != null) {
                BossFx.unstun(bound);
            }
            bound = null;
            clone = null;
            cloneStep = -1;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (cloneStep < 0) {
                return;
            }
            if (bound == null || !bound.isAlive() || !boss.isAlive() || bound.squaredDistanceTo(boss) > 32 * 32) {
                endClones();
                return;
            }
            if (clone != null) {
                BossFx.world(boss).spawnParticles(ParticleTypes.LARGE_SMOKE, clone.x, clone.y + 1.0, clone.z, 4, 0.2, 0.6, 0.2, 0);
                BossFx.world(boss).spawnParticles(ParticleTypes.END_ROD, clone.x, clone.y + 1.8, clone.z, 1, 0.1, 0.1, 0.1, 0);
            }
            if (now < cloneNext) {
                return;
            }
            if (clone == null) {
                double a = boss.getRandom().nextInt(8) * Math.PI / 4;
                clone = BossFx.ground(BossFx.world(boss), bound.getX() + Math.cos(a) * 2.5, bound.getZ() + Math.sin(a) * 2.5, bound.getY());
                cloneNext = now + BossMechanics.ECLIPSE_CLONE_INTERVAL;
                return;
            }
            resolveClone(boss, bound, clone);
            clone = null;
            cloneStep++;
            if (cloneStep >= BossMechanics.ECLIPSE_CLONE_CYCLES * BossMechanics.ECLIPSE_CLONES_PER_CYCLE) {
                BossFx.say(boss, "The Eclipse Moon's clones fade away.");
                endClones();
                return;
            }
            cloneNext = now + (cloneStep % BossMechanics.ECLIPSE_CLONES_PER_CYCLE == 0 ? 3 : 1);
        }

        /** A clone attacks: parried (the player faces it) for a free, always-hitting melee hit on the Moon, else it hits. */
        boolean resolveClone(DataBossEntity boss, ServerPlayerEntity p, Vec3d at) {
            if (BossMechanics.faces(p.getYaw(), at.x - p.getX(), at.z - p.getZ())) {
                AttackType type = Equipment.meleeType(p, Players.data(p).skills());
                com.runecraft.osrs.PlayerHitCalc.Input in = HitCalcBridge.input(p, boss, type, null, 0);
                int max = Math.max(0, HitCalcBridge.maxHit(p, boss, in));
                int damage = p.getRandom().nextInt(max + 1);
                parrying = true;
                try {
                    damage = boss.modifyIncoming(p, damage, type);
                } finally {
                    parrying = false;
                }
                int dealt = CombatHandler.applyDamage(p, boss, damage);
                CombatHandler.awardXp(p, Players.data(p).style().forMelee(), dealt, false);
                BossFx.world(boss).spawnParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 1.0, at.z, 1, 0, 0, 0, 0);
                Msg.action(p, "You parry the clone and strike back at the Eclipse Moon!");
                return true;
            }
            AttackDef c = attack(boss, "moonlight clones");
            int max = c == null ? 28 : maxHit(boss, c);
            CombatHooks.nameAttack("moonlight_clones", null);
            CombatHandler.applyDamage(boss, p, p.getRandom().nextInt(max + 1));
            return false;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (attacker != null && !parrying) {
                CombatAchievementService.flag(attacker, BossMechanics.FLAG_ECLIPSE_DIRECT_HIT);
            }
            int armour = cloneStep >= 0 ? BossMechanics.ECLIPSE_FLAT_ARMOUR_CLONES : BossMechanics.ECLIPSE_FLAT_ARMOUR;
            return flatArmour(damage, armour, type);
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            endClones();
        }
    }

    /**
     * Self-test (boss:eclipse_clones): a clone the player faces is parried and damages the Moon; one behind them hits the
     * player.
     */
    static String selfTestEclipse(DataBossEntity boss, ServerPlayerEntity p) {
        if (!(boss.script() instanceof EclipseMoon e)) {
            throw new IllegalStateException("the Eclipse Moon has no EclipseMoon script");
        }
        e.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
        e.startClones(boss, p);
        try {
            p.setYaw(0f); // facing +z
            Vec3d ahead = p.getPos().add(0, 0, 2.5);
            Vec3d behind = p.getPos().add(0, 0, -2.5);
            if (!e.resolveClone(boss, p, ahead)) {
                throw new IllegalStateException("a clone in front of the player wasn't parried");
            }
            if (e.resolveClone(boss, p, behind)) {
                throw new IllegalStateException("a clone behind the player was parried");
            }
            return "clone ahead parried, clone behind hit";
        } finally {
            e.endClones();
        }
    }
}
