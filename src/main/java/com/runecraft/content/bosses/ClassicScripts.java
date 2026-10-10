package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.ClassicBosses;
import com.runecraft.skill.XpService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Scripts for the classic bosses (Mole, Kalphite Queen, Dagannoth Kings, Corp, Skotizo, Hespori, Scurrius, Zalcano, Sol). */
final class ClassicScripts {
    private ClassicScripts() {
    }

    /** Giant Mole: below 50% it may burrow away to another part of its lair, snuffing out light sources. */
    static final class GiantMole extends ScriptedBoss {
        private int burrowIn = -1;
        private int burrowCooldown;

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (percent(boss) < 50 && burrowIn < 0 && burrowCooldown == 0 && boss.getRandom().nextInt(4) == 0) {
                burrowIn = 2;
                BossFx.warn(boss, "The Giant Mole starts to dig...");
            }
            return damage;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return burrowIn < 0;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (burrowCooldown > 0) {
                burrowCooldown--;
            }
            if (burrowIn > 0) {
                burrowIn--;
                BossFx.burst(boss, ParticleTypes.CAMPFIRE_COSY_SMOKE, 12, 1.0);
                if (burrowIn == 0) {
                    Vec3d to = around(boss, homeVec(), 10);
                    boss.requestTeleport(to.x, to.y, to.z);
                    BossFx.sound(boss, SoundEvents.BLOCK_GRAVEL_BREAK, 0.6f);
                    for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false));
                    }
                    BossFx.say(boss, "The Giant Mole digs away! Your light source is extinguished.");
                    burrowIn = -1;
                    burrowCooldown = 15;
                }
            }
        }
    }

    /** Kalphite Queen (crawling): magic and ranged are halved by her chitin; at 0 hp she takes flight. */
    static final class KalphiteQueen extends ScriptedBoss {
        private boolean transforming;
        private int wait;

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (transforming) {
                return 0;
            }
            if (type == AttackType.MAGIC || type == AttackType.RANGED) {
                damage /= 2;
            }
            if (damage >= boss.getHealth()) {
                damage = (int) Math.ceil(boss.getHealth()) - 1;
                transforming = true;
                wait = 3;
                BossFx.say(boss, "The Kalphite Queen's shell splits open...");
            }
            return damage;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return !transforming;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (transforming) {
                BossFx.burst(boss, ParticleTypes.CRIT, 20, 1.2);
                if (--wait <= 0) {
                    BossFx.say(boss, "The Kalphite Queen emerges from her shell and takes to the air!");
                    BossFx.swapForm(boss, "kalphite_queen_airborne", boss.getBlockPos(), true, null);
                }
            }
        }
    }

    /** Kalphite Queen (airborne): melee is halved; her lightning chains to every player nearby. */
    static final class KalphiteQueenAirborne extends ScriptedBoss {
        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            return type.isMelee() ? damage / 2 : damage;
        }

        @Override
        protected String creditAs() {
            return "kalphite_queen";
        }
    }

    /** Dagannoth Rex brings his brothers: Prime and Supreme spawn alongside him. */
    static final class DagannothRex extends ScriptedBoss {
        @Override
        public void onSpawn(DataBossEntity boss) {
            ServerPlayerEntity target = boss.getTarget() instanceof ServerPlayerEntity p ? p : null;
            BossFx.spawnBoss(boss, "dagannoth_prime", boss.getBlockPos().add(-4, 0, 3), target);
            BossFx.spawnBoss(boss, "dagannoth_supreme", boss.getBlockPos().add(4, 0, 3), target);
            BossFx.say(boss, "The Dagannoth Kings rise! Rex resists melee, Prime resists magic and Supreme resists ranged.");
        }
    }

    /**
     * Corporeal Beast: only stab attacks with a spear or halberd deal full damage (everything else is
     * halved); stomps anyone standing beneath it and releases dark energy cores.
     */
    static final class Corp extends ScriptedBoss {
        private MobEntity core;

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            return dx * dx + dz * dz < boss.getWidth() * boss.getWidth() / 4 ? ClassicBosses.CORP_STOMP : null;
        }

        static boolean spearOrHalberd(String id) {
            return id.contains("spear") || id.contains("halberd") || id.contains("hasta") || id.endsWith("_lance");
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (type == AttackType.STAB && spearOrHalberd(BossFx.heldId(attacker))) {
                return damage;
            }
            return damage / 2;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (target == null || percent(boss) > 75) {
                return;
            }
            if ((core == null || !core.isAlive()) && boss.getRandom().nextInt(16) == 0) {
                BossFx.say(boss, "The Corporeal Beast releases a dark energy core!");
                core = BossFx.spawnMinion(boss, "dark_energy_core", around(boss, target.getPos(), 2), target);
            }
            if (core != null && core.isAlive() && core.squaredDistanceTo(target) > 64) {
                Vec3d to = target.getPos().add(1, 0, 1);
                core.requestTeleport(to.x, to.y, to.z);
                BossFx.burst(core, ParticleTypes.SOUL, 10, 0.5);
            }
        }
    }

    /** Skotizo: each awakened altar strengthens it; it reawakens altars over time. */
    static final class Skotizo extends ScriptedBoss {
        private final MobEntity[] altars = new MobEntity[4];
        private int reawaken = 30;

        private static final int[][] SPOTS = {{0, -9}, {9, 0}, {0, 9}, {-9, 0}};

        private int awake() {
            int n = 0;
            for (MobEntity a : altars) {
                if (a != null && a.isAlive()) {
                    n++;
                }
            }
            return n;
        }

        private void awaken(DataBossEntity boss, int i) {
            Vec3d c = homeVec();
            altars[i] = BossFx.spawnMinion(boss, "awakened_altar", BossFx.ground(BossFx.world(boss), c.x + SPOTS[i][0], c.z + SPOTS[i][1],
                    c.y), null);
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            home = boss.getBlockPos();
            for (int i = 0; i < 4; i++) {
                awaken(boss, i);
            }
            BossFx.say(boss, "Four altars awaken around Skotizo, empowering it. Destroy them!");
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            int n = awake();
            return (int) Math.round(damage * (1 - 0.18 * n));
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (--reawaken <= 0) {
                reawaken = 30;
                for (int i = 0; i < 4; i++) {
                    if (altars[i] == null || !altars[i].isAlive()) {
                        awaken(boss, i);
                        BossFx.say(boss, "Skotizo reawakens one of its altars!");
                        break;
                    }
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            for (MobEntity a : altars) {
                if (a != null && a.isAlive()) {
                    a.discard();
                }
            }
            if (boss.getRandom().nextInt(65) == 0) {
                Inv.give(killer, "pet_skotos", 1);
                CollectionLog.obtain(killer, "pet_skotos");
                Msg.red(killer, "You have a funny feeling like you're being followed.");
            }
        }
    }

    /** Hespori: at 2/3 and 1/3 health it becomes invulnerable until its flowers are cut down. */
    static final class Hespori extends ScriptedBoss {
        private final List<MobEntity> flowers = new ArrayList<>();

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            flowers.clear();
            Vec3d c = boss.getPos();
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                MobEntity f = BossFx.spawnMinion(boss, "hesporis_flower",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 5, c.z + Math.sin(a) * 5, c.y), null);
                if (f != null) {
                    flowers.add(f);
                }
            }
            BossFx.say(boss, "Hespori's flowers bloom, shielding it! Cut them down.");
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return flowers.stream().noneMatch(MobEntity::isAlive);
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            flowers.forEach(MobEntity::discard);
        }
    }

    /** Scurrius: once below 40% he runs to his food pile and eats. */
    static final class Scurrius extends ScriptedBoss {
        private boolean ate;

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (!ate && percent(boss) < 40) {
                ate = true;
                BossFx.say(boss, "Scurrius scurries to his food pile and starts to eat!");
                boss.heal(30);
                BossFx.burst(boss, ParticleTypes.HAPPY_VILLAGER, 15, 1.0);
            }
        }
    }

    /** Zalcano: her armour can only be broken by mining it; damage scales with Mining level. */
    static final class Zalcano extends ScriptedBoss {
        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            String held = BossFx.heldId(attacker);
            if (!held.endsWith("pickaxe")) {
                Msg.action(attacker, "Zalcano's rocky armour can only be broken with a pickaxe.");
                return 0;
            }
            int mining = com.runecraft.player.Players.data(attacker).skills().currentLevel(Skill.MINING);
            int dealt = 1 + boss.getRandom().nextInt(Math.max(1, mining / 6));
            XpService.add(attacker, Skill.MINING, dealt * 2.5);
            BossFx.burst(boss, ParticleTypes.LAVA, 4, 0.6);
            return dealt;
        }
    }

    /** Sol Heredit: sweeping solar flares in lines across the arena in later phases. */
    static final class SolHeredit extends ScriptedBoss {
        private int flare = 20;

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (target == null || boss.phase() < 0 || --flare > 0) {
                return;
            }
            flare = boss.phase() >= 1 ? 14 : 20;
            BossFx.warn(boss, "Sol Heredit raises his shield to the sun - beams sweep the arena!");
            Vec3d dir = target.getPos().subtract(boss.getPos()).normalize();
            Vec3d side = new Vec3d(-dir.z, 0, dir.x);
            for (int i = -4; i <= 4; i++) {
                Vec3d p = target.getPos().add(side.multiply(i * 1.5));
                strike(boss, BossFx.ground(BossFx.world(boss), p.x, p.z, target.getY()), 3 + Math.abs(i) / 2, 40, 1.2, ParticleTypes.END_ROD);
            }
        }
    }
}
