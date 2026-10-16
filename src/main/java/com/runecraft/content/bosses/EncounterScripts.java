package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.Dt2Bosses;
import com.runecraft.osrs.content.bosses.MinigameBosses;
import com.runecraft.osrs.content.bosses.VarlamoreBosses;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import com.runecraft.osrs.Prayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.Map;

/** Scripts for the Gauntlet, Nightmare, Muspah, Desert Treasure II and Varlamore bosses. */
final class EncounterScripts {
    private EncounterScripts() {
    }

    private enum Category { MELEE, RANGED, MAGIC }

    private static Category category(AttackType t) {
        return t.isMelee() ? Category.MELEE : t == AttackType.RANGED ? Category.RANGED : Category.MAGIC;
    }

    /**
     * Crystalline/Corrupted Hunllef: switches between ranged and magic every 4 attacks (announced),
     * protects itself from one combat style and only switches after 6 hits from another style, stomps
     * anyone underneath it, sends chasing tornadoes and (corrupted) corrupts floor tiles.
     */
    static final class Hunllef extends ScriptedBoss {
        private final boolean corrupted;
        private boolean magic;
        private int styleAttacks;
        private Category protectedStyle = Category.MELEE;
        private int hitsTaken;
        private final List<MobEntity> tornadoes = new ArrayList<>();
        private int tornadoLife;
        private int tornadoTimer = 20;
        private int tileTimer = 10;

        Hunllef(boolean corrupted) {
            this.corrupted = corrupted;
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            BossFx.say(boss, "The Hunllef protects itself from melee. Land 6 hits with another style to make it switch.");
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            if (dx * dx + dz * dz < boss.getWidth() * boss.getWidth() / 4) {
                return MinigameBosses.HUNLLEF_STOMP;
            }
            if (styleAttacks >= MinigameBosses.HUNLLEF_ATTACKS_PER_STYLE) {
                styleAttacks = 0;
                magic = !magic;
                BossFx.warn(boss, "The Hunllef switches to " + (magic ? "magic" : "ranged") + " attacks!");
                BossFx.burst(boss, magic ? ParticleTypes.WITCH : ParticleTypes.CRIT, 20, 1.0);
            }
            styleAttacks++;
            if (magic) {
                if (boss.getRandom().nextInt(6) == 0) {
                    return MinigameBosses.HUNLLEF_PRAYER_DISABLE;
                }
                return corrupted ? MinigameBosses.CORRUPTED_MAGIC : MinigameBosses.HUNLLEF_MAGIC;
            }
            return corrupted ? MinigameBosses.CORRUPTED_RANGED : MinigameBosses.HUNLLEF_RANGED;
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
            if (last == MinigameBosses.HUNLLEF_PRAYER_DISABLE) {
                BossFx.disablePrayers(player, "The Hunllef's magic attack switches off your prayers!");
            }
            return damage;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            Category c = category(type);
            if (c == protectedStyle) {
                CombatAchievementService.flag(attacker, CombatAchievements.FLAG_HUNLLEF_WRONG_WEAPON);
                Msg.action(attacker, "The Hunllef's prayer protects it from " + c.name().toLowerCase() + "!");
                return 0;
            }
            if (++hitsTaken >= MinigameBosses.HUNLLEF_HITS_PER_PRAYER) {
                hitsTaken = 0;
                protectedStyle = c;
                BossFx.say(boss, "The Hunllef switches its prayer to protect from " + c.name().toLowerCase() + "!");
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (target == null) {
                return;
            }
            BossFx.world(boss).spawnParticles(switch (protectedStyle) {
                case MELEE -> ParticleTypes.CRIT;
                case RANGED -> ParticleTypes.HAPPY_VILLAGER;
                case MAGIC -> ParticleTypes.WITCH;
            }, boss.getX(), boss.getY() + boss.getHeight() + 0.5, boss.getZ(), 3, 0.3, 0.1, 0.3, 0);
            int count = corrupted ? 2 : 1;
            if (percent(boss) < 66) {
                count++;
            }
            if (percent(boss) < 33) {
                count++;
            }
            if (--tornadoTimer <= 0) {
                tornadoTimer = 25;
                tornadoLife = 14;
                BossFx.warn(boss, "The Hunllef summons tornadoes!");
                for (int i = 0; i < count; i++) {
                    MobEntity t = BossFx.spawnMinion(boss, "hunllef_tornado", around(boss, homeVec(), 8), null);
                    if (t != null) {
                        tornadoes.add(t);
                    }
                }
            }
            if (tornadoLife > 0) {
                tornadoLife--;
                for (MobEntity t : tornadoes) {
                    Vec3d d = target.getPos().subtract(t.getPos());
                    double len = d.horizontalLength();
                    if (len < 1.2) {
                        com.runecraft.combat.CombatHooks.nameAttack("tornado", null);
                        CombatHandler.applyDamage(boss, target, (corrupted ? 15 : 10) + boss.getRandom().nextInt(11));
                    } else {
                        Vec3d step = new Vec3d(d.x / len, 0, d.z / len);
                        t.requestTeleport(t.getX() + step.x, t.getY(), t.getZ() + step.z);
                    }
                }
                if (tornadoLife == 0) {
                    tornadoes.forEach(MobEntity::discard);
                    tornadoes.clear();
                }
            }
            if (corrupted && --tileTimer <= 0) {
                tileTimer = 12;
                BossFx.warn(boss, "The floor begins to corrupt!");
                for (int i = 0; i < 7; i++) {
                    namedStrike("damaging_floor", boss, around(boss, homeVec(), 10), 4, 20, 1.4, ParticleTypes.CRIMSON_SPORE);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            tornadoes.forEach(MobEntity::discard);
        }
    }

    /**
     * The Nightmare / Phosani's Nightmare: shielded at the start of each phase until four totems are
     * charged (destroyed); husks bind you until killed, parasites hatch from players, and the curse
     * switches off protection prayers. Phosani's Nightmare summons sleepwalkers at the end of each phase (2, 3, then 4):
     * every one that reaches her strengthens her power blast (5 + 144 x unkilled / total); then, in her desperation
     * phase, sleepwalkers keep coming and each one that arrives deals 15.
     */
    static final class Nightmare extends ScriptedBoss {
        private final List<MobEntity> totems = new ArrayList<>();
        private final List<MobEntity> husks = new ArrayList<>();
        private boolean shielded;
        private int parasiteIn = -1;
        private ServerPlayerEntity host;
        private int curseTimer = 40;
        private final boolean phosani;
        private final List<MobEntity> walkers = new ArrayList<>();
        private long walkersMoveAt;
        private int setTotal;
        private int setReached;
        private boolean finalSet;
        private boolean desperate;
        private int walkerTimer;

        Nightmare() {
            this(false);
        }

        Nightmare(boolean phosani) {
            this.phosani = phosani;
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            home = boss.getBlockPos();
            raiseShield(boss);
        }

        private void raiseShield(DataBossEntity boss) {
            totems.forEach(MobEntity::discard);
            totems.clear();
            Vec3d c = homeVec();
            for (int[] o : new int[][]{{-9, -9}, {9, -9}, {-9, 9}, {9, 9}}) {
                MobEntity t = BossFx.spawnMinion(boss, "nightmare_totem", BossFx.ground(BossFx.world(boss), c.x + o[0], c.z + o[1], c.y), null);
                if (t != null) {
                    totems.add(t);
                }
            }
            shielded = true;
            BossFx.say(boss, "The Nightmare is shielded! Charge the four totems to break it.");
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return !shielded;
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            if (phosani) {
                summonWalkers(boss, BossMechanics.sleepwalkers(phase + 1));
            }
            raiseShield(boss);
            ServerPlayerEntity target = target(boss);
            if (target != null) {
                husks.clear();
                for (int i = 0; i < 2; i++) {
                    MobEntity h = BossFx.spawnMinion(boss, "nightmare_husk", around(boss, target.getPos(), 2), target);
                    if (h != null) {
                        husks.add(h);
                    }
                }
                BossFx.stun(target, 30);
                Msg.red(target, "Husks grab hold of you! Kill them to break free.");
            }
        }

        /** Sleepwalkers walk in from the edges of the arena; any hit kills one. */
        private void summonWalkers(DataBossEntity boss, int count) {
            Vec3d c = homeVec();
            for (int i = 0; i < count; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                MobEntity m = BossFx.spawnMinion(boss, BossMechanics.PHOSANI_SLEEPWALKER,
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 11, c.z + Math.sin(a) * 11, c.y), null);
                if (m == null) {
                    continue;
                }
                walkers.add(m);
                MinionRules.set(m, new MinionRules.Rule() {
                    @Override
                    public double accuracy(ServerPlayerEntity p, net.minecraft.entity.LivingEntity mob, AttackType type) {
                        return MinionRules.ALWAYS_HITS;
                    }

                    @Override
                    public int damage(ServerPlayerEntity p, net.minecraft.entity.LivingEntity mob, AttackType type, int d) {
                        return MinionRules.lethal(mob);
                    }
                });
            }
            if (!desperate) {
                setTotal = count;
                setReached = 0;
                walkersMoveAt = now + 4;
                BossFx.warn(boss, "Sleepwalkers stumble towards the Nightmare - stop them before they reach her!");
            }
        }

        private void tickWalkers(DataBossEntity boss, ServerPlayerEntity target) {
            java.util.Iterator<MobEntity> it = walkers.iterator();
            while (it.hasNext()) {
                MobEntity m = it.next();
                if (!m.isAlive()) {
                    it.remove();
                    continue;
                }
                if (now < walkersMoveAt) {
                    continue;
                }
                Vec3d d = boss.getPos().subtract(m.getPos());
                double len = d.horizontalLength();
                if (len <= boss.getWidth() / 2 + 1.0) {
                    m.discard();
                    it.remove();
                    if (desperate) {
                        CombatAchievementService.flagNear(boss, BossMechanics.FLAG_PHOSANI_SLEEPWALKER);
                        for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                            CombatHooks.nameAttack("sleepwalker", null);
                            CombatHandler.applyDamage(boss, p, BossMechanics.DESPERATION_SLEEPWALKER_DAMAGE);
                        }
                    } else {
                        setReached++;
                    }
                    BossFx.burst(boss, ParticleTypes.WITCH, 10, 1.0);
                    continue;
                }
                double step = Math.min(0.35, len);
                m.requestTeleport(m.getX() + d.x / len * step, m.getY(), m.getZ() + d.z / len * step);
            }
            if (setTotal > 0 && walkers.isEmpty()) {
                // Every sleepwalker of the set is dead or arrived: the power blast.
                int dmg = BossMechanics.sleepwalkerBlast(setReached, setTotal);
                setTotal = 0;
                BossFx.say(boss, setReached == 0 ? "The Nightmare's power blast fizzles." : "The Nightmare unleashes a power blast!");
                for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                    CombatHooks.nameAttack("power_blast", null);
                    CombatHandler.applyDamage(boss, p, dmg);
                }
                if (finalSet && !desperate) {
                    desperate = true;
                    walkerTimer = 20;
                    BossFx.warn(boss, "Phosani's Nightmare lashes out in desperation!");
                }
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (phosani) {
                tickWalkers(boss, target);
                if (!finalSet && !shielded && percent(boss) <= 10) { // approx: the end of her third phase
                    finalSet = true;
                    summonWalkers(boss, BossMechanics.sleepwalkers(3));
                }
                if (desperate && target != null && --walkerTimer <= 0) {
                    walkerTimer = 8; // approx: how often they spawn in the desperation phase
                    summonWalkers(boss, 1);
                    walkersMoveAt = now;
                }
            }
            if (shielded) {
                BossFx.ring(BossFx.world(boss), boss.getPos(), 2.0, ParticleTypes.WITCH);
                if (totems.stream().noneMatch(MobEntity::isAlive)) {
                    shielded = false;
                    BossFx.say(boss, "The totems blast the Nightmare, shattering her shield!");
                    float blast = mcHp(boss, 800);
                    boss.setHealth(Math.max(1, boss.getHealth() - Math.min(blast, boss.getMaxHealth() * 0.1f)));
                    BossFx.sound(boss, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.7f);
                }
            }
            if (!husks.isEmpty() && husks.stream().noneMatch(MobEntity::isAlive)) {
                husks.clear();
                if (target != null) {
                    BossFx.unstun(target);
                }
            }
            if (target == null) {
                return;
            }
            if (boss.phase() >= 0 && parasiteIn < 0 && boss.getRandom().nextInt(45) == 0) {
                parasiteIn = 18;
                host = target;
                Msg.red(target, "The Nightmare has impregnated you with a deadly parasite!");
            }
            if (parasiteIn > 0 && --parasiteIn == 0) {
                if (host != null && host.isAlive()) {
                    CombatHandler.applyDamage(boss, host, 10 + boss.getRandom().nextInt(16));
                    BossFx.spawnMinion(boss, "nightmare_parasite", host.getPos(), host);
                    Msg.red(host, "The parasite bursts out of you!");
                }
                parasiteIn = -1;
            }
            if (--curseTimer <= 0) {
                curseTimer = 45 + boss.getRandom().nextInt(20);
                BossFx.disablePrayers(target, "The Nightmare curses you, shuffling your prayers!");
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            totems.forEach(MobEntity::discard);
            husks.forEach(MobEntity::discard);
            walkers.forEach(MobEntity::discard);
        }
    }

    /** Phantom Muspah: ranged form, then a melee charge (faster), then a frozen shield that soaks damage. */
    static final class Muspah extends ScriptedBoss {
        private int shield;

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            if (phase == 0) {
                var speed = boss.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(0.3);
                }
            } else if (phase == 1) {
                shield = 75;
                var speed = boss.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(0.18);
                }
            }
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (shield > 0) {
                // OSRS Wiki: the shielded form is immune to damage; its 75-point prayer shield loses 2 per attack, and
                // prayer drains speed it up (approx: Smite drains a quarter of the hit, as on players).
                shield -= 2;
                if (attacker != null && com.runecraft.player.Players.data(attacker).isActive(com.runecraft.osrs.Prayer.SMITE)) {
                    shield -= damage / 4;
                }
                BossFx.burst(boss, ParticleTypes.SNOWFLAKE, 8, 1.0);
                if (shield <= 0) {
                    shield = 0;
                    BossFx.say(boss, "The Phantom Muspah's prayer shield shatters!");
                } else if (attacker != null) {
                    Msg.action(attacker, "The Phantom Muspah's prayer shield absorbs your attack. (" + shield + " left)");
                }
                return 0;
            }
            return damage;
        }

        /** While shielded it also has Soul Split: it heals half the damage it deals. */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
            if (shield > 0 && damage > 1) {
                boss.heal(mcHp(boss, damage / 2));
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (shield > 0) {
                BossFx.ring(BossFx.world(boss), boss.getPos(), 2.2, ParticleTypes.SNOWFLAKE);
            }
        }
    }

    /**
     * Duke Sucellus: fist slams and magic; every few attacks gas vents erupt; periodically his freezing
     * gaze hits everyone he can see - hide behind one of the pillars.
     */
    static final class Duke extends ScriptedBoss {
        private int gaze = 16;
        private boolean pillars;
        /** Max hit scale: 1 for the boss, below 1 for the weaker Desert Treasure II quest version. */
        private final double scale;

        Duke() {
            this(1.0);
        }

        Duke(double scale) {
            this.scale = scale;
        }

        @Override
        protected double multiplier(DataBossEntity boss) {
            return super.multiplier(boss) * scale;
        }


        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return gaze > 5;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            int vents = boss.awakened() ? 4 : 5;
            if (attackCount % vents == vents - 1) {
                BossFx.warn(boss, "Poisonous gas erupts from the vents!");
                for (int i = 0; i < (boss.awakened() ? 5 : 3); i++) {
                    strike(boss, around(boss, target.getPos(), 4), 2, (int) Math.round(20 * scale), 1.3, ParticleTypes.SNEEZE, (hit, pos) ->
                            com.runecraft.boss.Mechanics.addHazard(BossFx.world(boss), pos, 5, 8, boss));
                }
                attackCount++;
                cooldown = 3;
                return null;
            }
            return distance < 5 ? Dt2Bosses.DUKE_SLAM : Dt2Bosses.DUKE_MAGIC;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (!pillars) {
                pillars = true;
                BlockPos c = home;
                for (int[] o : new int[][]{{-5, -3}, {5, -3}, {-5, 5}, {5, 5}}) {
                    for (int y = 1; y <= 3; y++) {
                        BossFx.world(boss).setBlockState(c.add(o[0], y, o[1]), Blocks.CHISELED_DEEPSLATE.getDefaultState());
                    }
                }
                BossFx.say(boss, "Duke Sucellus stirs from his slumber...");
            }
            if (target == null) {
                return;
            }
            gaze--;
            if (gaze == 5) {
                BossFx.say(boss, "Duke Sucellus' eyes begin to glow... break his line of sight!");
            }
            if (gaze > 0 && gaze <= 5) {
                BossFx.burst(boss, ParticleTypes.SNOWFLAKE, 10, 1.5);
            }
            if (gaze <= 0) {
                gaze = boss.awakened() ? 14 : 20; // approx: the awakened Duke gazes more often
                for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                    if (boss.canSee(p)) {
                        BossFx.stun(p, 6);
                        CombatHandler.applyDamage(boss, p, (int) Math.round((40 + boss.getRandom().nextInt(36))
                                * boss.variantHitScale(Dt2Bosses.DUKE_GAZE) * scale));
                        Msg.red(p, "The Duke's freezing gaze locks onto you!");
                    } else {
                        Msg.game(p, "You hide from the Duke's gaze.");
                    }
                }
            }
        }
    }

    /**
     * The Leviathan: volleys of 3-6 projectiles, each announced, alternating between boulders (ranged),
     * orbs (magic) and shadows (melee) - switch prayers for every one. Debris and lightning fall between
     * volleys; below 20% it enrages and fires faster.
     */
    static final class Leviathan extends ScriptedBoss {
        private final List<AttackDef> volley = new ArrayList<>();
        private int pause;
        /** Game ticks left of a shadow-spell stun (OSRS Wiki: 15 ticks, halved when awakened). */
        private int stunned;

        /** Max hit scale: 1 for the boss, below 1 for the weaker Desert Treasure II quest version. */
        private final double scale;

        Leviathan() {
            this(1.0);
        }

        Leviathan(double scale) {
            this.scale = scale;
        }

        @Override
        protected double multiplier(DataBossEntity boss) {
            return super.multiplier(boss) * scale;
        }


        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return stunned <= 0;
        }

        /**
         * A shadow spell during a volley stuns the Leviathan: the volley ends and it stands still for 15 ticks, taking at
         * most 10 per hit (approx: the gaping wound on its back, which takes full damage, isn't modelled).
         */
        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (stunned > 0) {
                return Math.min(damage, 10);
            }
            if (attacker != null && type == AttackType.MAGIC && !volley.isEmpty()
                    && String.valueOf(com.runecraft.content.systems.SysState.of(attacker).autocast).startsWith("shadow_")) {
                stunned = boss.awakened() ? 7 : 15;
                volley.clear();
                CombatAchievementService.count(attacker, CombatAchievements.COUNT_LEVIATHAN_STUN, 1);
                BossFx.say(boss, "The shadow spell stuns the Leviathan!");
            }
            return damage;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (volley.isEmpty()) {
                int n = (boss.awakened() ? 5 : 3) + boss.getRandom().nextInt(4); // approx: awakened volleys are longer
                AttackDef[] styles = {Dt2Bosses.LEVI_RANGED, Dt2Bosses.LEVI_MAGIC, Dt2Bosses.LEVI_MELEE};
                for (int i = 0; i < n; i++) {
                    volley.add(styles[boss.getRandom().nextInt(3)]);
                }
                BossFx.warn(boss, "The Leviathan prepares a barrage of " + n + " projectiles!");
                for (int i = 0; i < 3; i++) {
                    strike(boss, around(boss, target.getPos(), 5), 3, (int) Math.round(maxHit(boss, Dt2Bosses.LEVI_DEBRIS) * scale), 1.3, ParticleTypes.ASH);
                }
                cooldown = 2;
                return null;
            }
            AttackDef a = volley.remove(0);
            Msg.action(target, a == Dt2Bosses.LEVI_RANGED ? "Boulder! (ranged)" : a == Dt2Bosses.LEVI_MAGIC ? "Abyssal orb! (magic)"
                    : "Shadow! (melee)");
            if (volley.isEmpty()) {
                pause = boss.awakened() ? (percent(boss) < 20 ? 2 : 4) : percent(boss) < 20 ? 3 : 6;
            }
            return a;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            if (pause > 0) {
                int p = pause;
                pause = 0;
                return p;
            }
            return percent(boss) < 20 ? 1 : 2;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (stunned > 0) {
                stunned--;
                BossFx.burst(boss, ParticleTypes.SMOKE, 6, 1.5);
                return;
            }
            if (target != null && boss.getRandom().nextInt(boss.awakened() ? 18 : 30) == 0) {
                strike(boss, target.getPos(), 3, (int) Math.round(maxHit(boss, Dt2Bosses.LEVI_LIGHTNING) * scale), 1.3, ParticleTypes.ELECTRIC_SPARK);
                BossFx.warn(boss, "The Leviathan's shell crackles with lightning!");
            }
        }
    }

    /**
     * The Whisperer: soul bolts and tentacle lashes, a piercing screech every tenth attack (run!) and,
     * below 50%, the Shadow Realm: she becomes untouchable until you destroy her tentacles in time.
     */
    static final class Whisperer extends ScriptedBoss {
        private final List<MobEntity> tentacles = new ArrayList<>();
        private int shadowTicks = -1;
        /** Max hit scale: 1 for the boss, below 1 for the weaker Desert Treasure II quest version. */
        private final double scale;

        Whisperer() {
            this(1.0);
        }

        Whisperer(double scale) {
            this.scale = scale;
        }

        @Override
        protected double multiplier(DataBossEntity boss) {
            return super.multiplier(boss) * scale;
        }


        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return shadowTicks < 0;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return shadowTicks < 0;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            int screech = boss.awakened() ? 7 : 10;
            if (attackCount > 0 && attackCount % screech == 0) {
                return Dt2Bosses.WHISPER_SCREECH;
            }
            if (boss.getRandom().nextInt(boss.awakened() ? 5 : 8) == 0) {
                return Dt2Bosses.WHISPER_SIPHON;
            }
            return attackCount % 2 == 0 ? Dt2Bosses.WHISPER_MAGIC : Dt2Bosses.WHISPER_RANGED;
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            ServerPlayerEntity target = target(boss);
            if (target == null) {
                return;
            }
            shadowTicks = boss.awakened() ? 24 : 30; // approx: less time to escape when awakened
            for (int i = 0; i < 3; i++) {
                MobEntity t = BossFx.spawnMinion(boss, "whisperer_tentacle", around(boss, target.getPos(), 5), target);
                if (t != null) {
                    tentacles.add(t);
                }
            }
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 30 * 12, 0, false, false));
            BossFx.say(boss, "You are dragged into the Shadow Realm! Destroy the tentacles before your soul is consumed!");
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (shadowTicks < 0) {
                return;
            }
            shadowTicks--;
            if (target != null) {
                CombatAchievementService.count(target, CombatAchievements.COUNT_SHADOW_REALM_TICKS, 1);
            }
            boolean cleared = tentacles.stream().noneMatch(MobEntity::isAlive);
            if (cleared || shadowTicks == 0) {
                if (!cleared && target != null) {
                    CombatHandler.applyDamage(boss, target, (int) Math.round((30 + boss.getRandom().nextInt(21))
                            * boss.variantHitScale(Dt2Bosses.WHISPER_SIPHON) * scale));
                    Msg.red(target, "The Whisperer consumes part of your soul!");
                }
                tentacles.forEach(MobEntity::discard);
                tentacles.clear();
                shadowTicks = -1;
                if (target != null) {
                    target.removeStatusEffect(StatusEffects.DARKNESS);
                }
                BossFx.say(boss, "You escape the Shadow Realm.");
            }
        }
    }

    /**
     * Vardorvis: relentless melee that heals him for half the damage dealt. His Strength and Defence follow his hitpoints
     * (OSRS Wiki): the max hit climbs from 32 to 43 and his Defence drops by round(0.1021 x damage taken - 0.2075). Swinging
     * axes (1 above 690 hitpoints, 2 down to 231, 3 below), darting spikes (1/2/3 darts), and below 570 hitpoints his
     * parasitic head fires a ranged or magic projectile. Below 33% he enrages: a new set of axes as soon as the last is
     * thrown and the head comes more often.
     */
    static final class Vardorvis extends ScriptedBoss {
        /** Max hit scale: 1 for the boss, below 1 for the weaker Desert Treasure II quest version. */
        private final double scale;
        private int autos;
        private boolean enraged;
        // Strangle: the spores to destroy, when the tendrils tighten, and whether the last special was a Strangle.
        private final List<MobEntity> spores = new ArrayList<>();
        private long strangleDue = -1;
        private ServerPlayerEntity strangled;
        private boolean lastStrangle;

        Vardorvis() {
            this(1.0);
        }

        Vardorvis(double scale) {
            this.scale = scale;
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayerEntity target) {
            return true;
        }

        private double hpFraction(DataBossEntity boss) {
            return boss.getHealth() / boss.getMaxHealth();
        }

        /** His hitpoints on the normal 700 scale (awakened: half of the real ones). */
        private int hp(DataBossEntity boss) {
            return (int) Math.round(hpFraction(boss) * BossMechanics.VARDORVIS_HITPOINTS);
        }

        /** The max hit ramp as a factor (1 at full hitpoints) on his other attacks; the axe swing gets the ramp itself. */
        @Override
        protected double multiplier(DataBossEntity boss) {
            boolean awake = boss.awakened();
            double base = awake ? 46 : 32;
            return BossMechanics.vardorvisMaxHit(hpFraction(boss), awake) / base * scale;
        }

        /**
         * The axe swing's max hit is the wiki ramp (32 to 43, awakened 46 to 61) whatever the attack's listed max (35, or
         * the awakened variant's 61), so the multiplier is taken against that listed max.
         */
        @Override
        protected void perform(DataBossEntity boss, ServerPlayerEntity target, AttackDef a) {
            if (a != Dt2Bosses.VARD_SLASH || a.maxHit() <= 0) {
                super.perform(boss, target, a);
                return;
            }
            last = a;
            attackCount++;
            int max = BossMechanics.vardorvisMaxHit(hpFraction(boss), boss.awakened());
            Mechanics.perform(boss, target, a, max * scale / a.maxHit());
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
            if (damage > 1) {
                boss.heal(mcHp(boss, damage / 2)); // heals 50% of the damage he deals, rounded down
            }
            return damage;
        }

        /**
         * Swinging axes: each axe hits a player once for up to 35 (awakened approx 50; Protect from Melee halves it) and
         * starts a bleed (3 every 3 ticks for 14 ticks, every tick while running).
         */
        private void axes(DataBossEntity boss, ServerPlayerEntity target) {
            int n = BossMechanics.vardorvisAxes(hp(boss), boss.awakened());
            BossFx.warn(boss, n == 1 ? "An axe swings out from the walls!" : n + " axes swing out from the walls!");
            Vec3d p = target.getPos();
            Vec3d toward = p.subtract(boss.getPos());
            double base = Math.atan2(toward.z, toward.x);
            int max = (int) Math.round((boss.awakened() ? BossMechanics.VARDORVIS_AXE_MAX * 61.0 / 43 : BossMechanics.VARDORVIS_AXE_MAX)
                    * scale); // awakened: approx, scaled like his melee ramp
            for (int a = 0; a < n; a++) {
                double ang = base + a * Math.PI / 4 * (a % 2 == 0 ? 1 : -1);
                Vec3d dir = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
                Set<UUID> struck = new HashSet<>();
                for (int i = -4; i <= 4; i++) {
                    Vec3d at = p.add(dir.multiply(i * 1.5));
                    namedStrike("swinging_axe", boss, BossFx.ground(BossFx.world(boss), at.x, at.z, p.y), 3, 0, 1.1,
                            ParticleTypes.SWEEP_ATTACK, (hit, pos) -> {
                                for (ServerPlayerEntity h : hit) {
                                    if (!struck.add(h.getUuid())) {
                                        continue;
                                    }
                                    int roll = boss.getRandom().nextInt(max + 1);
                                    if (BossFx.protects(h, Prayer.PROTECT_FROM_MELEE)) {
                                        roll /= 2;
                                    }
                                    CombatHooks.nameAttack("swinging_axe", null);
                                    CombatHandler.applyDamage(boss, h, roll);
                                    BossDots.bleed(h);
                                }
                            });
                }
            }
        }

        /**
         * Strangle (below 570 hitpoints, never twice in a row): tendrils root the target and spores appear around them; any
         * spore left after 5 ticks (awakened 4) tightens the tendrils for ~25 (awakened ~40).
         */
        private void strangle(DataBossEntity boss, ServerPlayerEntity target) {
            boolean awake = boss.awakened();
            int ticks = awake ? BossMechanics.VARDORVIS_STRANGLE_TICKS_AWAKENED : BossMechanics.VARDORVIS_STRANGLE_TICKS;
            BossFx.warn(boss, "Vardorvis strikes the ground and tendrils entangle you - destroy the spores!");
            target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,
                    ticks * 12, 10, false, true));
            spores.forEach(MobEntity::discard);
            spores.clear();
            for (int i = 0; i < BossMechanics.VARDORVIS_STRANGLE_SPORES; i++) {
                double a = Math.PI * 2 * i / BossMechanics.VARDORVIS_STRANGLE_SPORES + boss.getRandom().nextDouble();
                Vec3d at = BossFx.ground(BossFx.world(boss), target.getX() + Math.cos(a) * 1.6, target.getZ() + Math.sin(a) * 1.6,
                        target.getY());
                MobEntity spore = BossFx.spawnMinion(boss, BossMechanics.STRANGLER_SPORE, at, null);
                if (spore != null) {
                    MinionRules.set(spore, new MinionRules.Rule() {
                        @Override
                        public double accuracy(ServerPlayerEntity pl, net.minecraft.entity.LivingEntity m, AttackType t) {
                            return MinionRules.ALWAYS_HITS;
                        }

                        @Override
                        public int damage(ServerPlayerEntity pl, net.minecraft.entity.LivingEntity m, AttackType t, int d) {
                            return MinionRules.lethal(m);
                        }
                    });
                    spores.add(spore);
                }
            }
            strangled = target;
            strangleDue = now + ticks;
            cooldown = Math.max(cooldown, ticks + 1);
        }

        private void tickStrangle(DataBossEntity boss) {
            if (strangleDue < 0 || now < strangleDue) {
                return;
            }
            strangleDue = -1;
            boolean left = spores.stream().anyMatch(MobEntity::isAlive);
            spores.forEach(MobEntity::discard);
            spores.clear();
            if (left && strangled != null && strangled.isAlive()) {
                int dmg = (int) Math.round((boss.awakened() ? BossMechanics.VARDORVIS_STRANGLE_DAMAGE_AWAKENED
                        : BossMechanics.VARDORVIS_STRANGLE_DAMAGE) * scale);
                CombatHooks.nameAttack("strangle", null);
                CombatHandler.applyDamage(boss, strangled, dmg - boss.getRandom().nextInt(4)); // approx: "~25"
                Msg.red(strangled, "The tendrils tighten around you!");
            } else if (strangled != null) {
                Msg.game(strangled, "You break free of the tendrils.");
            }
            strangled = null;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            spores.forEach(MobEntity::discard);
            spores.clear();
        }

        private void darts(DataBossEntity boss, ServerPlayerEntity target) {
            int n = BossMechanics.vardorvisDarts(hp(boss));
            BossFx.warn(boss, "Vardorvis darts around you, leaving cracks in the ground!");
            int max = (int) Math.round((boss.awakened() ? 40 : 25) * scale);
            for (int i = 0; i < n; i++) {
                Vec3d at = i == 0 ? target.getPos() : around(boss, target.getPos(), 3);
                namedStrike("darting_spikes", boss, at, 4 + i, max, 1.3, ParticleTypes.DAMAGE_INDICATOR, (hit, pos) -> {
                    for (ServerPlayerEntity h : hit) {
                        boss.heal(mcHp(boss, max / 4.0)); // heals half the (average) damage dealt
                    }
                });
            }
            cooldown += n; // each dart delays his next auto-attack by a tick
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            boolean awake = boss.awakened();
            int hp = hp(boss);
            if (!enraged && hp < BossMechanics.VARDORVIS_ENRAGE_HP) {
                enraged = true; // no visual tell in OSRS; announced here
                BossFx.warn(boss, "Vardorvis' attacks grow frantic!");
            }
            autos++;
            // Three autos before the first axes; enraged (or awakened), a new set after every other attack.
            int axeEvery = enraged || awake ? 2 : 5;
            if (autos > 3 && autos % axeEvery == 0) {
                axes(boss, target);
            }
            if (hp < BossMechanics.VARDORVIS_HEAD_HP && boss.getRandom().nextInt(enraged ? 3 : 6) == 0) {
                return boss.getRandom().nextBoolean() ? Dt2Bosses.VARD_HEAD_RANGED : Dt2Bosses.VARD_HEAD_MAGIC;
            }
            if (hp < BossMechanics.VARDORVIS_HEAD_HP && !lastStrangle && strangleDue < 0 && boss.getRandom().nextInt(8) == 0) { // approx rate
                lastStrangle = true;
                strangle(boss, target);
                return null;
            }
            lastStrangle = false;
            if (autos > 3 && boss.getRandom().nextInt(enraged ? 5 : 8) == 0) {
                darts(boss, target);
                cooldown = Math.max(cooldown, 2);
                return null;
            }
            return Dt2Bosses.VARD_SLASH;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            // Defence follows his hitpoints (defence-draining specials make no difference on top).
            int lost = (int) Math.round((1 - hpFraction(boss)) * BossMechanics.VARDORVIS_HITPOINTS);
            com.runecraft.content.systems.SysCombat.drain(boss).defence = BossMechanics.vardorvisDefenceReduction(lost);
            tickStrangle(boss);
            BossDots.tick(boss, BossFx.players(boss, 24));
            if (target == null || home == null) {
                return;
            }
            // "Axe Enthusiast": time survived at his max speed, within the centre 25 tiles (5x5) of the arena.
            double dx = target.getX() - (home.getX() + 0.5);
            double dz = target.getZ() - (home.getZ() + 0.5);
            if (Math.max(Math.abs(dx), Math.abs(dz)) > 5.5) {
                CombatAchievementService.flag(target, BossMechanics.FLAG_VARDORVIS_LEFT_CENTRE);
            }
            if (enraged) {
                CombatAchievementService.count(target, BossMechanics.COUNT_VARDORVIS_ENRAGE, 1);
            }
        }
    }

    /** The Hueycoatl: every attack is announced by its glowing head colour; thunderstorms and tail sweeps. */
    static final class Hueycoatl extends ScriptedBoss {
        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (attackCount % 6 == 5) {
                BossFx.warn(boss, "Thunder rumbles overhead!");
                for (int i = 0; i < 5; i++) {
                    strike(boss, i == 0 ? target.getPos() : around(boss, target.getPos(), 5), 3, VarlamoreBosses.HUEY_LIGHTNING.maxHit(),
                            1.4, ParticleTypes.ELECTRIC_SPARK);
                }
                attackCount++;
                cooldown = 3;
                return null;
            }
            if (distance < 6 && boss.getRandom().nextInt(5) == 0) {
                return VarlamoreBosses.HUEY_TAIL;
            }
            AttackDef[] styles = {VarlamoreBosses.HUEY_MELEE, VarlamoreBosses.HUEY_RANGED, VarlamoreBosses.HUEY_MAGIC};
            return styles[boss.getRandom().nextInt(3)];
        }
    }

    /**
     * Royal Titans (Branda the Fire Queen's script also runs Eldric the Ice King). Phase 1: the Titans alternate between
     * keeping their distance - conjuring fire and icicle walls that form after 4 ticks (12 to anyone standing in them, which
     * stops that part forming) and, once a Titan has taken 100 damage, elementals that self-destruct - and closing in to
     * stomp. Water Blast or ice spells douse fire and Fire Blast or stronger melts icicles (approx: the spell clears the wall
     * nearest the caster rather than a clicked tile). Phase 2 (either Titan below 210): Elemental Blast - a charge, then
     * three blasts of up to 30 everywhere but one safe spot. Elementals caught in an opposing wall or blast die. A Titan
     * brought down while the other still stands is subdued; both fall together.
     */
    static final class RoyalTitans extends ScriptedBoss {
        private static final class Wall {
            final List<Vec3d> cells = new ArrayList<>();
            final boolean fire;
            final long formsAt;
            long fadesAt;
            boolean[] gone;
            boolean formed;
            /** The blocks standing in each formed cell (walls block movement as in OSRS); restored to air when it goes. */
            final Map<Integer, List<BlockPos>> blocks = new HashMap<>();

            Wall(boolean fire, long formsAt) {
                this.fire = fire;
                this.formsAt = formsAt;
            }
        }

        private record Elemental(MobEntity mob, boolean fire, long explodeAt) {
        }

        private DataBossEntity eldric;
        private final List<Elemental> elementals = new ArrayList<>();
        private final List<Wall> walls = new ArrayList<>();
        private boolean rangedMode = true;
        private int modeTimer = 33;
        private int wallTimer = 6;
        private int elementalTimer;
        private boolean phaseTwo;
        private long chargeUntil = -1;
        private int chargeId;
        private int blastsLeft;
        private long nextBlast;
        private int blastCycle = 10;
        private Vec3d safe;
        private boolean subdued;
        private int subduedCharge = -2;
        private int eldricDeadCharge = -3;
        private boolean eldricDead;
        private MinionRules.AttackWatcher watcher;

        @Override
        public void onSpawn(DataBossEntity boss) {
            ServerPlayerEntity target = boss.getTarget() instanceof ServerPlayerEntity p ? p : null;
            eldric = BossFx.spawnBoss(boss, "eldric_the_ice_king", boss.getBlockPos().add(5, 0, 0), target);
            BossFx.say(boss, "Branda the Fire Queen and Eldric the Ice King rise! Douse the fires and melt the icicles.");
            watcher = (p, t, type, d) -> {
                if (!boss.isRemoved() && type == AttackType.MAGIC && p.squaredDistanceTo(boss) < 32 * 32) {
                    String spell = MinionRules.spell(p);
                    if (BossMechanics.dousesFire(spell)) {
                        clearWall(boss, p.getPos(), true);
                    } else if (BossMechanics.meltsIce(spell)) {
                        clearWall(boss, p.getPos(), false);
                    }
                }
            };
            MinionRules.watch(watcher);
        }

        private boolean eldricAlive() {
            return eldric != null && eldric.isAlive();
        }

        private boolean charging() {
            return chargeUntil >= 0 && now <= chargeUntil;
        }

        /** Removes the wall cell nearest {@code from} and the two next to it. */
        private void clearWall(DataBossEntity boss, Vec3d from, boolean fire) {
            Wall best = null;
            int bestCell = -1;
            double bestD = 12 * 12;
            for (Wall w : walls) {
                if (w.fire != fire) {
                    continue;
                }
                for (int i = 0; i < w.cells.size(); i++) {
                    double d = w.cells.get(i).squaredDistanceTo(from);
                    if (!w.gone[i] && d < bestD) {
                        bestD = d;
                        best = w;
                        bestCell = i;
                    }
                }
            }
            if (best == null) {
                return;
            }
            for (int i = Math.max(0, bestCell - 1); i <= Math.min(best.cells.size() - 1, bestCell + 1); i++) {
                best.gone[i] = true;
                unblock(BossFx.world(boss), best, i);
                Vec3d c = best.cells.get(i);
                BossFx.world(boss).spawnParticles(fire ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, c.x, c.y + 0.6, c.z, 8, 0.3, 0.4, 0.3, 0.02);
            }
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            return subdued || charging() ? IDLE_MELEE : null;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return !subdued;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (eldricAlive() && attacker != null) {
                // "One by one": attacking Branda while Eldric stands.
                CombatAchievementService.flag(attacker, CombatAchievements.FLAG_TITANS_BOTH);
            }
            if (eldricAlive() && damage >= boss.getHealth()) {
                subdued = true;
                subduedCharge = charging() ? chargeId : -2;
                ModernScriptsAccess.stop(boss);
                BossFx.say(boss, "Branda the Fire Queen is subdued! She falls when her brother does.");
                return Math.max(0, (int) Math.ceil(boss.getHealth()) - 1);
            }
            return damage;
        }

        private static int trueHp(DataBossEntity e) {
            return (int) Math.round(e.getHealth() / e.getMaxHealth() * 600);
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            ServerWorld w = BossFx.world(boss);
            if (!eldricDead && eldric != null && !eldric.isAlive()) {
                eldricDead = true;
                eldricDeadCharge = charging() ? chargeId : -3;
                if (subdued) {
                    if (subduedCharge == eldricDeadCharge) {
                        CombatAchievementService.flagNear(boss, BossMechanics.FLAG_TITANS_CHARGE_KILL);
                    }
                    ServerPlayerEntity killer = eldric.getAttacker() instanceof ServerPlayerEntity p ? p : target;
                    if (killer != null) {
                        boss.setHealth(0f);
                        boss.onDeath(CombatHandler.source(killer));
                        return;
                    }
                }
            }
            tickWalls(boss, w);
            tickElementals(boss, w);
            if (target == null || subdued) {
                return;
            }
            if (!phaseTwo && (trueHp(boss) < BossMechanics.TITAN_PHASE_TWO_HP || eldricAlive() && trueHp(eldric) < BossMechanics.TITAN_PHASE_TWO_HP)) {
                phaseTwo = true;
                BossFx.warn(boss, "The Royal Titans take their places at either end of the arena!");
                blastCycle = 8;
            }
            if (phaseTwo) {
                phaseTwoTick(boss, w, target);
                return;
            }
            // Phase 1: ranged (walls and elementals) for ~20 seconds, then melee (stomps).
            if (--modeTimer <= 0) {
                rangedMode = !rangedMode;
                modeTimer = rangedMode ? 33 : 25;
                BossFx.warn(boss, rangedMode ? "The Royal Titans back away and raise their staves." : "The Royal Titans close in to stomp!");
            }
            double speed = rangedMode ? 0.0 : 0.2;
            ModernScriptsAccess.speed(boss, speed);
            if (eldricAlive()) {
                ModernScriptsAccess.speed(eldric, speed);
            }
            if (rangedMode && --wallTimer <= 0) {
                wallTimer = 9;
                conjureWall(boss, w, true);
                if (eldricAlive()) {
                    conjureWall(boss, w, false);
                }
            }
            boolean hurtEnough = trueHp(boss) <= 600 - BossMechanics.TITAN_ELEMENTAL_DAMAGE
                    || eldricAlive() && trueHp(eldric) <= 600 - BossMechanics.TITAN_ELEMENTAL_DAMAGE;
            if (rangedMode && hurtEnough && --elementalTimer <= 0) {
                elementalTimer = 30;
                summonElementals(boss, target);
            }
        }

        private void phaseTwoTick(DataBossEntity boss, ServerWorld w, ServerPlayerEntity target) {
            ModernScriptsAccess.speed(boss, 0.0);
            if (eldricAlive()) {
                ModernScriptsAccess.speed(eldric, 0.0);
            }
            if (blastsLeft > 0) {
                if (safe != null) {
                    BossFx.ring(w, safe, 1.0, ParticleTypes.END_ROD);
                }
                if (now >= nextBlast) {
                    blastsLeft--;
                    nextBlast = now + 2;
                    for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                        if (safe == null || ModernScriptsAccess.flat(p.getPos(), safe) > 1.5) {
                            CombatHooks.nameAttack("elemental_blast", null);
                            CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(BossMechanics.TITAN_BLAST_MAX + 1));
                        }
                    }
                    for (Elemental e : elementals) {
                        if (e.mob().isAlive()) {
                            e.mob().discard(); // caught in the opposing Titan's blast
                            CombatAchievementService.countNear(boss, BossMechanics.COUNT_TITAN_ELEMENTAL_KILLS);
                        }
                    }
                    BossFx.burst(boss, ParticleTypes.FLAME, 30, 6.0);
                    if (eldricAlive()) {
                        BossFx.burst(eldric, ParticleTypes.SNOWFLAKE, 30, 6.0);
                    }
                    if (blastsLeft == 0) {
                        chargeUntil = now; // the charging window closes with the last blast
                        blastCycle = 20;
                    }
                }
                return;
            }
            if (charging()) {
                BossFx.burst(boss, ParticleTypes.FLAME, 6, 1.0);
                if (now >= chargeUntil - 1) {
                    blastsLeft = 3;
                    nextBlast = now;
                }
                return;
            }
            if (--blastCycle <= 0) {
                chargeId++;
                chargeUntil = now + 8;
                safe = around(boss, homeVec().add(2.5, 0, 0), 5);
                BossFx.warn(boss, "The Royal Titans charge an Elemental Blast! Find the safe spot!");
                return;
            }
            if (blastCycle == 12) {
                summonElementals(boss, target);
            }
        }

        private void conjureWall(DataBossEntity boss, ServerWorld w, boolean fire) {
            Vec3d c = homeVec().add(2.5, 0, 0);
            double x = c.x + (boss.getRandom().nextInt(9) - 4) * 2;
            Wall wall = new Wall(fire, now + BossMechanics.TITAN_WALL_FORM_TICKS);
            for (int i = -2; i <= 2; i++) {
                wall.cells.add(BossFx.ground(w, x, c.z + i * 2, c.y));
            }
            wall.gone = new boolean[wall.cells.size()];
            walls.add(wall);
        }

        private void tickWalls(DataBossEntity boss, ServerWorld w) {
            List<ServerPlayerEntity> players = BossFx.players(boss, 24);
            java.util.Iterator<Wall> it = walls.iterator();
            while (it.hasNext()) {
                Wall wall = it.next();
                boolean any = false;
                for (int i = 0; i < wall.cells.size(); i++) {
                    if (wall.gone[i]) {
                        continue;
                    }
                    any = true;
                    Vec3d c = wall.cells.get(i);
                    w.spawnParticles(wall.fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, c.x, c.y + (wall.formed ? 0.8 : 0.1), c.z,
                            wall.formed ? 4 : 1, 0.3, wall.formed ? 0.5 : 0.05, 0.3, 0);
                }
                if (!any) {
                    unblockAll(w, wall);
                    it.remove();
                    continue;
                }
                String name = wall.fire ? "fire_wall" : "icicle_wall";
                if (!wall.formed && now >= wall.formsAt) {
                    wall.formed = true;
                    wall.fadesAt = now + 30; // approx: how long a wall lasts by itself
                    for (int i = 0; i < wall.cells.size(); i++) {
                        Vec3d c = wall.cells.get(i);
                        for (ServerPlayerEntity p : players) {
                            if (ModernScriptsAccess.flat(p.getPos(), c) < 1.1) {
                                CombatHooks.nameAttack(name, null);
                                CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(BossMechanics.TITAN_WALL_MAX + 1));
                                wall.gone[i] = true; // standing on the tile stops it forming
                            }
                        }
                        for (Elemental e : elementals) {
                            if (!wall.gone[i] && e.fire() != wall.fire && e.mob().isAlive() && ModernScriptsAccess.flat(e.mob().getPos(), c) < 1.3) {
                                e.mob().discard();
                                CombatAchievementService.countNear(boss, BossMechanics.COUNT_TITAN_ELEMENTAL_KILLS);
                            }
                        }
                        if (!wall.gone[i]) {
                            block(w, wall, i);
                        }
                    }
                } else if (wall.formed) {
                    if (now >= wall.fadesAt) {
                        CombatAchievementService.flagNear(boss, BossMechanics.FLAG_TITANS_WALL_DISSIPATED);
                        unblockAll(w, wall);
                        it.remove();
                        continue;
                    }
                    for (int i = 0; i < wall.cells.size(); i++) {
                        if (wall.gone[i]) {
                            continue;
                        }
                        for (ServerPlayerEntity p : players) {
                            if (ModernScriptsAccess.flat(p.getPos(), wall.cells.get(i)) < 0.9) {
                                CombatHooks.nameAttack(name, null);
                                CombatHandler.applyDamage(boss, p, 2); // approx: walls block the tile in OSRS
                            }
                        }
                    }
                }
            }
        }

        /**
         * Raises a formed wall cell: a 2x2-block (one tile), 2-high column of magma (fire) or packed ice (icicles) in the
         * air blocks of the tile that no creature stands in, so players and elementals have to go around it.
         */
        private static void block(ServerWorld w, Wall wall, int cell) {
            Vec3d c = wall.cells.get(cell);
            List<BlockPos> placed = new ArrayList<>();
            net.minecraft.block.BlockState state = (wall.fire ? Blocks.MAGMA_BLOCK : Blocks.PACKED_ICE).getDefaultState();
            int y = (int) Math.floor(c.y + 0.01);
            for (int bx : new int[]{(int) Math.floor(c.x - 0.5), (int) Math.floor(c.x + 0.5)}) {
                for (int bz : new int[]{(int) Math.floor(c.z - 0.5), (int) Math.floor(c.z + 0.5)}) {
                    for (int dy = 0; dy < 2; dy++) {
                        BlockPos pos = new BlockPos(bx, y + dy, bz);
                        if (placed.contains(pos) || !w.getBlockState(pos).isAir()
                                || !w.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, new net.minecraft.util.math.Box(pos),
                                e -> e.isAlive()).isEmpty()) {
                            continue;
                        }
                        w.setBlockState(pos, state);
                        placed.add(pos);
                    }
                }
            }
            wall.blocks.put(cell, placed);
        }

        private static void unblock(ServerWorld w, Wall wall, int cell) {
            List<BlockPos> placed = wall.blocks.remove(cell);
            if (placed == null) {
                return;
            }
            for (BlockPos pos : placed) {
                if (w.getBlockState(pos).isOf(Blocks.MAGMA_BLOCK) || w.getBlockState(pos).isOf(Blocks.PACKED_ICE)) {
                    w.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
        }

        private static void unblockAll(ServerWorld w, Wall wall) {
            for (Integer cell : new ArrayList<>(wall.blocks.keySet())) {
                unblock(w, wall, cell);
            }
        }

        private void summonElementals(DataBossEntity boss, ServerPlayerEntity target) {
            elementals.removeIf(e -> !e.mob().isAlive());
            if (elementals.size() >= 6) {
                return;
            }
            for (int i = 0; i < 3; i++) {
                MobEntity f = BossFx.spawnMinion(boss, "titan_fire_elemental", around(boss, boss.getPos(), 3), target);
                if (f != null) {
                    elementals.add(new Elemental(f, true, now + 20)); // approx: when they self-destruct
                }
                if (eldricAlive()) {
                    MobEntity ice = BossFx.spawnMinion(boss, "titan_ice_elemental", around(boss, eldric.getPos(), 3), target);
                    if (ice != null) {
                        elementals.add(new Elemental(ice, false, now + 20));
                    }
                }
            }
            BossFx.warn(boss, "The Royal Titans summon elementals!");
        }

        private void tickElementals(DataBossEntity boss, ServerWorld w) {
            java.util.Iterator<Elemental> it = elementals.iterator();
            while (it.hasNext()) {
                Elemental e = it.next();
                if (!e.mob().isAlive()) {
                    it.remove();
                    continue;
                }
                if (now < e.explodeAt()) {
                    continue;
                }
                for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                    if (ModernScriptsAccess.flat(p.getPos(), e.mob().getPos()) <= 3.0) {
                        CombatHooks.nameAttack("elemental_explosion", null);
                        CombatHandler.applyDamage(boss, p, BossMechanics.TITAN_ELEMENTAL_BLAST_MIN
                                + boss.getRandom().nextInt(BossMechanics.TITAN_ELEMENTAL_BLAST_MAX - BossMechanics.TITAN_ELEMENTAL_BLAST_MIN + 1));
                    }
                }
                w.spawnParticles(ParticleTypes.EXPLOSION, e.mob().getX(), e.mob().getY() + 0.5, e.mob().getZ(), 1, 0, 0, 0, 0);
                e.mob().discard();
                it.remove();
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            if (charging() && eldricDead && eldricDeadCharge == chargeId) {
                CombatAchievementService.flagNear(boss, BossMechanics.FLAG_TITANS_CHARGE_KILL);
            }
            elementals.forEach(e -> e.mob().discard());
            if (watcher != null) {
                MinionRules.unwatch(watcher);
            }
            ServerWorld w = BossFx.world(boss);
            for (Wall wall : walls) {
                unblockAll(w, wall);
            }
            walls.clear();
        }
    }

    /** Small helpers shared by the scripts in this file. */
    private static final class ModernScriptsAccess {
        private ModernScriptsAccess() {
        }

        static double flat(Vec3d a, Vec3d b) {
            double dx = a.x - b.x;
            double dz = a.z - b.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        static void speed(MobEntity mob, double speed) {
            var attr = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (attr != null && attr.getBaseValue() != speed) {
                attr.setBaseValue(speed);
            }
        }

        static void stop(MobEntity mob) {
            speed(mob, 0.0);
            mob.getNavigation().stop();
        }
    }
}
