package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.SlayerBosses;
import com.runecraft.osrs.content.bosses.SlayerBosses.HydraPhase;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Scripts for the Slayer bosses. */
final class SlayerScripts {
    private SlayerScripts() {
    }

    /** Kraken: starts as a whirlpool; disturb all four tentacles before the Kraken itself surfaces. */
    static final class Kraken extends ScriptedBoss {
        private final List<MobEntity> tentacles = new ArrayList<>();
        private boolean risen;

        @Override
        public void onSpawn(DataBossEntity boss) {
            Vec3d c = boss.getPos();
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + Math.PI / 2 * i;
                MobEntity t = BossFx.spawnMinion(boss, "enormous_tentacle",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 6, c.z + Math.sin(a) * 6, c.y), null);
                if (t != null) {
                    tentacles.add(t);
                }
            }
            boss.setInvisible(true);
            BossFx.say(boss, "Four whirlpools churn around a larger one... Disturb the tentacles first!");
        }

        private boolean disturbed() {
            for (MobEntity t : tentacles) {
                if (t.isAlive() && t.getHealth() >= t.getMaxHealth()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            return risen ? null : IDLE_RANGED;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return risen;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (!risen) {
                BossFx.ring(BossFx.world(boss), boss.getPos(), 2.0, ParticleTypes.BUBBLE);
                if (disturbed()) {
                    risen = true;
                    boss.setInvisible(false);
                    BossFx.burst(boss, ParticleTypes.SPLASH, 60, 2.0);
                    BossFx.say(boss, "The Kraken rises from the whirlpool!");
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            tentacles.forEach(MobEntity::discard);
        }
    }

    /** Thermonuclear smoke devil: without a face mask or slayer helmet its smoke chokes and drains you. */
    static final class Thermy extends ScriptedBoss {
        private static final Set<String> MASKS = Set.of("facemask", "face_mask", "gas_mask", "slayer_helmet", "slayer_helmet_i",
                "black_slayer_helmet", "green_slayer_helmet", "red_slayer_helmet", "purple_slayer_helmet");

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            BossFx.burst(boss, ParticleTypes.LARGE_SMOKE, 6, 1.5);
            if (now % 4 != 0) {
                return;
            }
            for (ServerPlayerEntity p : BossFx.players(boss, 14)) {
                if (!MASKS.contains(Inv.idOf(p.getEquippedStack(EquipmentSlot.HEAD)))) {
                    Msg.red(p, "The thick smoke chokes you! You need a face mask.");
                    CombatHandler.applyDamage(boss, p, 5 + p.getRandom().nextInt(11));
                    for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC}) {
                        BossFx.drainStat(p, s, 1);
                    }
                }
            }
        }
    }

    /**
     * Cerberus: auto attacks by distance; a triple attack (magic, ranged, melee) opens the fight and
     * every tenth attack; summoned souls below 400 hp (pray correctly: an unprotected soul hits 30, a
     * protected one drains 30 prayer); lava pools below 200 hp.
     */
    static final class Cerberus extends ScriptedBoss {
        private final List<AttackDef> queue = new ArrayList<>();
        private int souls = 20;
        private int lava = 12;
        private int soulStep = -1;
        private final MobEntity[] soulMobs = new MobEntity[3];

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayerEntity target) {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (attackCount % 10 == 0 && queue.isEmpty()) {
                BossFx.say(boss, "Cerberus: Grrrrr!");
                queue.add(SlayerBosses.CERB_MAGIC);
                queue.add(SlayerBosses.CERB_RANGED);
                queue.add(distance < 3.5 ? SlayerBosses.CERB_MELEE : SlayerBosses.CERB_MAGIC);
            }
            if (!queue.isEmpty()) {
                return queue.remove(0);
            }
            if (distance < 3.5 && boss.getRandom().nextInt(3) > 0) {
                return SlayerBosses.CERB_MELEE;
            }
            return boss.getRandom().nextBoolean() ? SlayerBosses.CERB_RANGED : SlayerBosses.CERB_MAGIC;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            // Auto-attacks every 6 ticks (wiki attack speed); the "Grrrrr" triple attack fires its three styles 2 ticks apart.
            return queue.isEmpty() ? super.attackDelay(boss, attack) : 2;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (target == null) {
                return;
            }
            double hp = boss.getHealth();
            if (hp <= 400 && soulStep < 0 && --souls <= 0) {
                souls = 32;
                summonSouls(boss, target);
            }
            if (soulStep >= 0) {
                tickSouls(boss, target);
            }
            if (hp <= 200 && --lava <= 0) {
                lava = 20;
                BossFx.say(boss, "Cerberus: Grrrrrrrrrrrrrr!");
                Vec3d p = target.getPos();
                namedStrike("lava_pool", boss, p, 4, 15, 1.4, ParticleTypes.LAVA);
                namedStrike("lava_pool", boss, around(boss, p, 3), 4, 15, 1.4, ParticleTypes.LAVA);
                namedStrike("lava_pool", boss, around(boss, p, 3), 4, 15, 1.4, ParticleTypes.LAVA);
            }
        }

        private void summonSouls(DataBossEntity boss, ServerPlayerEntity target) {
            BossFx.say(boss, "Cerberus: Aaarrrooooooo! Summoned souls float in - ranged, then magic, then melee!");
            CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_CERBERUS_SOULS);
            String[] ids = {"summoned_soul_ranged", "summoned_soul_magic", "summoned_soul_melee"};
            Vec3d c = homeVec();
            for (int i = 0; i < 3; i++) {
                soulMobs[i] = BossFx.spawnMinion(boss, ids[i], BossFx.ground(BossFx.world(boss), c.x - 3 + i * 3, c.z - 9, c.y), null);
            }
            soulStep = 0;
            cooldownSouls = 6;
        }

        private int cooldownSouls;

        private void tickSouls(DataBossEntity boss, ServerPlayerEntity target) {
            if (--cooldownSouls > 0) {
                return;
            }
            Prayer[] prayers = {Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MELEE};
            String[] names = {"ranged", "magic", "melee"};
            MobEntity soul = soulMobs[soulStep];
            if (soul != null && soul.isAlive()) {
                BossFx.world(boss).spawnParticles(ParticleTypes.SOUL, soul.getX(), soul.getY() + 1, soul.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                if (BossFx.protects(target, prayers[soulStep])) {
                    boolean spectral = "spectral_spirit_shield".equals(Inv.idOf(target.getOffHandStack()));
                    BossFx.drainPrayer(target, spectral ? 15 : 30);
                    CombatAchievementService.count(target, CombatAchievements.COUNT_SOUL_NEGATED, 1);
                    Msg.game(target, "The " + names[soulStep] + " soul drains your prayer.");
                } else {
                    com.runecraft.combat.CombatHooks.nameAttack("summoned_soul", null);
                    CombatHandler.applyDamage(boss, target, 30);
                    Msg.red(target, "The " + names[soulStep] + " soul strikes you!");
                }
                soul.discard();
            }
            soulStep++;
            cooldownSouls = 2;
            if (soulStep >= 3) {
                soulStep = -1;
            }
        }
    }

    /**
     * Abyssal Sire. Phase 1: it sleeps until attacked; awake, its six tentacles lash anyone near them (up to 30) and shield
     * the four respiratory systems (0-3 damage) while it throws miasma pools and spawns. Disorient it - 75 ranged/magic
     * damage or a shadow spell (Rush 25%, Burst 50%, Blitz 75%, Barrage always) - and for 30 seconds (the tentacles wake
     * after 27) the systems are open; damage to the Sire itself in this phase only counts towards disorienting it. With all
     * four systems gone it awakens in a fury (phase 2: tendril swipes, slams and miasma); below 50% (phase 3) its
     * tentacles return, spawns grow into scions and it swells up and explodes - get far away.
     */
    static final class Sire extends ScriptedBoss {
        private final List<MobEntity> systems = new ArrayList<>();
        private final List<Vec3d> tentacles = new ArrayList<>();
        private int stage = 1;
        private boolean awake;
        private int disoriented;
        private int disorientDamage;
        private int spawnTimer = 10;
        private int explodeTimer = -1;
        private int miasmaTimer = 6;

        @Override
        public void onSpawn(DataBossEntity boss) {
            Vec3d c = boss.getPos();
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + Math.PI / 2 * i;
                MobEntity m = BossFx.spawnMinion(boss, "respiratory_system",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 7, c.z + Math.sin(a) * 7, c.y), null);
                if (m != null) {
                    systems.add(m);
                    // Protected by the tentacles while the Sire is awake and not disoriented; open (always hitting) otherwise.
                    MinionRules.set(m, new MinionRules.Rule() {
                        @Override
                        public double accuracy(ServerPlayerEntity p, net.minecraft.entity.LivingEntity mob, AttackType type) {
                            return systemsOpen() ? 10.0 : 1.0; // approx: OSRS deals at least half the max hit when open
                        }

                        @Override
                        public int damage(ServerPlayerEntity p, net.minecraft.entity.LivingEntity mob, AttackType type, int d) {
                            if (systemsOpen()) {
                                return d;
                            }
                            if (d > 0) {
                                Msg.action(p, "The Sire's tentacles shield the respiratory system!");
                            }
                            return Math.min(d, p.getRandom().nextInt(BossMechanics.SIRE_PROTECTED_MAX + 1));
                        }
                    });
                }
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI / 3 * i + Math.PI / 6;
                tentacles.add(BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 4.5, c.z + Math.sin(a) * 4.5, c.y));
            }
            BossFx.say(boss, "The Abyssal Sire slumbers, guarded by its tentacles and respiratory systems.");
        }

        /** The respiratory systems can only be hurt while the Sire is disoriented. */
        private boolean systemsOpen() {
            return disoriented > 0;
        }

        /** The tentacles lash out while the Sire is awake in phase 1 (from 27 s into a disorientation) and again in phase 3. */
        private boolean tentaclesActive() {
            if (stage == 1) {
                return awake && disoriented <= BossMechanics.SIRE_DISORIENT_TICKS - BossMechanics.SIRE_TENTACLE_WAKE_TICKS;
            }
            return stage >= 3;
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayerEntity target) {
            return stage == 2;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return stage >= 2 && explodeTimer < 0;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (stage != 1) {
                return damage;
            }
            if (!awake) {
                awake = true;
                BossFx.say(boss, "The Abyssal Sire stirs and its tentacles awaken!");
                BossFx.sound(boss, SoundEvents.ENTITY_WITHER_AMBIENT, 0.6f);
            }
            if (attacker != null && disoriented <= 0) {
                boolean stun = false;
                int chance = type == AttackType.MAGIC ? BossMechanics.sireStunChance(MinionRules.spell(attacker)) : 0;
                if (chance > 0 && boss.getRandom().nextInt(100) < chance) {
                    stun = true;
                } else if (!type.isMelee()) {
                    disorientDamage += damage;
                    stun = disorientDamage >= BossMechanics.SIRE_DISORIENT_DAMAGE;
                }
                if (stun) {
                    disorientDamage = 0;
                    disoriented = BossMechanics.SIRE_DISORIENT_TICKS;
                    CombatAchievementService.count(attacker, BossMechanics.COUNT_SIRE_STUN, 1);
                    BossFx.say(boss, "The Abyssal Sire is disoriented! Its respiratory systems are exposed.");
                }
            }
            return 0; // phase 1 damage only counts towards disorienting it
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (distance > 4) {
                return SlayerBosses.SIRE_MIASMA;
            }
            int r = boss.getRandom().nextInt(8);
            return r < 4 ? SlayerBosses.SIRE_TENDRIL : r < 6 ? SlayerBosses.SIRE_SLAM : SlayerBosses.SIRE_MIASMA;
        }

        private void tickTentacles(DataBossEntity boss) {
            boolean active = tentaclesActive();
            for (Vec3d t : tentacles) {
                BossFx.world(boss).spawnParticles(active ? ParticleTypes.SQUID_INK : ParticleTypes.SMOKE, t.x, t.y + 1.0, t.z,
                        active ? 3 : 1, 0.2, 0.6, 0.2, 0);
            }
            if (!active || now % 4 != 0) {
                return;
            }
            for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
                for (Vec3d t : tentacles) {
                    double dx = p.getX() - t.x;
                    double dz = p.getZ() - t.z;
                    if (dx * dx + dz * dz <= 2.5 * 2.5) {
                        int roll = boss.getRandom().nextInt(BossMechanics.SIRE_TENTACLE_MAX + 1);
                        if (BossFx.protects(p, Prayer.PROTECT_FROM_MELEE)) {
                            roll = roll * 3 / 4; // approx: "reduced slightly"
                        }
                        com.runecraft.combat.CombatHooks.nameAttack("tentacle", AttackType.CRUSH);
                        CombatHandler.applyDamage(boss, p, roll);
                        break;
                    }
                }
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            tickTentacles(boss);
            if (stage == 1) {
                if (disoriented > 0) {
                    disoriented--;
                    BossFx.burst(boss, ParticleTypes.ENCHANTED_HIT, 4, 1.0);
                    if (disoriented == 0) {
                        BossFx.say(boss, "The Abyssal Sire regains its senses.");
                    }
                } else {
                    BossFx.burst(boss, ParticleTypes.SQUID_INK, 3, 1.0);
                }
                if (systems.stream().noneMatch(MobEntity::isAlive)) {
                    stage = 2;
                    disoriented = 0;
                    BossFx.say(boss, "The Abyssal Sire awakens in a fury!");
                    BossFx.sound(boss, SoundEvents.ENTITY_WITHER_SPAWN, 0.6f);
                    return;
                }
                // Awake and not disoriented: miasma pools under the player and spawns.
                if (awake && disoriented <= 0 && target != null) {
                    if (--miasmaTimer <= 0) {
                        miasmaTimer = 8;
                        perform(boss, target, SlayerBosses.SIRE_MIASMA);
                    }
                    if (--spawnTimer <= 0) {
                        spawnTimer = 15;
                        BossFx.spawnMinion(boss, "abyssal_spawn", around(boss, boss.getPos(), 3), target);
                    }
                }
                return;
            }
            if (target == null) {
                return;
            }
            if (stage == 2 && percent(boss) <= 50) {
                stage = 3;
                explodeTimer = 8;
                BossFx.say(boss, "The Abyssal Sire begins to swell... RUN! Its tentacles thrash again!");
            }
            if (explodeTimer > 0) {
                explodeTimer--;
                BossFx.burst(boss, ParticleTypes.SQUID_INK, 25, 2.0);
                if (explodeTimer == 0) {
                    perform(boss, target, SlayerBosses.SIRE_EXPLOSION.telegraph(0, null));
                    explodeTimer = -1;
                }
            }
            if (--spawnTimer <= 0) {
                spawnTimer = stage >= 3 ? 12 : 15;
                BossFx.spawnMinion(boss, stage >= 3 ? "abyssal_scion" : "abyssal_spawn", around(boss, boss.getPos(), 3), target);
                if (stage >= 3) {
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_SIRE_SCION);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            systems.forEach(MobEntity::discard);
        }
    }

    /**
     * Alchemical Hydra. Each colour phase is a separate form (one head is lost per phase). Its chemical
     * shield cuts damage by 75% until it is lured next to the vent whose colour beats its carapace (green to
     * red, blue to green, red to blue) while the vents erupt.
     * Ranged and magic alternate every three attacks; every ninth attack is the phase special (poison
     * pools, chasing lightning, fire walls). The final enraged phase has no shield and attacks faster.
     */
    static final class Hydra extends ScriptedBoss {
        private final HydraPhase phase;
        private boolean shielded;
        private int ventTimer = 8;
        private int styleCount;
        private boolean magicStyle;
        private boolean restored;

        Hydra(HydraPhase phase) {
            this.phase = phase;
            this.shielded = phase.vent != null;
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayerEntity target) {
            return true;
        }

        @Override
        protected String creditAs() {
            return phase == HydraPhase.GREEN ? null : "alchemical_hydra";
        }

        private Vec3d vent(String colour) {
            Vec3d c = homeVec();
            return switch (colour) {
                case "red" -> c.add(-7, 0, -6);
                case "green" -> c.add(7, 0, -6);
                default -> c.add(0, 0, 8);
            };
        }

        private ParticleEffect ventParticle(String colour) {
            return switch (colour) {
                case "red" -> ParticleTypes.FLAME;
                case "green" -> ParticleTypes.SNEEZE;
                default -> ParticleTypes.ELECTRIC_SPARK;
            };
        }

        private void placeVents(DataBossEntity boss) {
            for (String c : new String[]{"red", "green", "blue"}) {
                Vec3d v = BossFx.ground(BossFx.world(boss), vent(c).x, vent(c).z, home.getY());
                BlockPos p = BlockPos.ofFloored(v.x, v.y - 1, v.z);
                var state = switch (c) {
                    case "red" -> Blocks.RED_CONCRETE.getDefaultState();
                    case "green" -> Blocks.LIME_CONCRETE.getDefaultState();
                    default -> Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
                };
                BossFx.world(boss).setBlockState(p, state);
            }
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            if (attackCount > 0 && attackCount % 9 == 0) {
                special(boss, target);
                attackCount++;
                cooldown = attackDelay(boss, SlayerBosses.HYDRA_MAGIC);
                return null;
            }
            if (styleCount >= 3) {
                styleCount = 0;
                magicStyle = !magicStyle;
                BossFx.warn(boss, "The Alchemical Hydra switches to " + (magicStyle ? "magic" : "ranged") + " attacks!");
            }
            styleCount++;
            return magicStyle ? SlayerBosses.HYDRA_MAGIC : SlayerBosses.HYDRA_RANGED;
        }

        private void special(DataBossEntity boss, ServerPlayerEntity target) {
            Vec3d p = target.getPos();
            switch (phase) {
                case GREEN, ENRAGED -> {
                    BossFx.warn(boss, "The Alchemical Hydra spits poison!");
                    for (int i = 0; i < 5; i++) {
                        Vec3d at = i == 0 ? p : around(boss, p, 3);
                        namedStrike("poison_pool", boss, at, 2, 12, 1.2, ParticleTypes.SNEEZE, (hit, pos) ->
                                com.runecraft.boss.Mechanics.addHazard(BossFx.world(boss), pos, 4, 10, boss));
                    }
                }
                case BLUE -> {
                    BossFx.warn(boss, "The Alchemical Hydra calls down lightning - keep moving!");
                    for (int i = 0; i < 4; i++) {
                        final int delay = 2 + i * 2;
                        namedStrike("lightning", boss, around(boss, p, 2 + i), delay, 20, 1.3, ParticleTypes.ELECTRIC_SPARK, (hit, pos) -> {
                            for (ServerPlayerEntity h : hit) {
                                BossFx.stun(h, 2);
                            }
                        });
                    }
                }
                case RED -> {
                    BossFx.warn(boss, "The Alchemical Hydra breathes walls of fire - get between them!");
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_HYDRA_FLAME_WALL);
                    Vec3d dir = p.subtract(boss.getPos()).normalize();
                    Vec3d side = new Vec3d(-dir.z, 0, dir.x);
                    for (int i = -5; i <= 5; i++) {
                        for (int s : new int[]{-2, 2}) {
                            Vec3d at = p.add(dir.multiply(i * 1.2)).add(side.multiply(s));
                            namedStrike("fire_wall", boss, BossFx.ground(BossFx.world(boss), at.x, at.z, p.y), 3, 20, 1.0, ParticleTypes.FLAME);
                        }
                    }
                    namedStrike("fire_wall", boss, p, 6, 20, 1.0, ParticleTypes.FLAME);
                }
            }
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (shielded && damage > 0) {
                double scaled = damage * SlayerBosses.HYDRA_SHIELD_MULTIPLIER;
                int whole = (int) scaled;
                return whole + (boss.getRandom().nextDouble() < scaled - whole ? 1 : 0);
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (!restored) {
                restored = true;
                if (boss.instanceTag != null && boss.instanceTag.startsWith("hydra;")) {
                    String[] p = boss.instanceTag.split(";");
                    home = new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                } else {
                    placeVents(boss);
                }
                if (phase == HydraPhase.ENRAGED) {
                    BossFx.say(boss, "The Alchemical Hydra becomes enraged!");
                } else if (phase != HydraPhase.GREEN) {
                    BossFx.say(boss, "The Alchemical Hydra's chemical shield reforms as it turns " + phase.colour + "!");
                }
            }
            if (phase.vent != null && --ventTimer <= 0) {
                ventTimer = 8;
                for (String c : new String[]{"red", "green", "blue"}) {
                    Vec3d v = vent(c);
                    BossFx.column(BossFx.world(boss), new Vec3d(v.x, home.getY(), v.z), ventParticle(c), 8);
                    double dx = boss.getX() - v.x;
                    double dz = boss.getZ() - v.z;
                    if (dx * dx + dz * dz < 6.25) {
                        if (c.equals(phase.vent)) {
                            if (shielded) {
                                shielded = false;
                                BossFx.say(boss, "The chemicals neutralise the Alchemical Hydra's defences!");
                            }
                        } else {
                            shielded = true;
                            boss.heal(mcHp(boss, 20));
                            BossFx.say(boss, "The Alchemical Hydra absorbs the wrong chemicals and its defences return!");
                            CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_HYDRA_EMPOWERED);
                        }
                    }
                }
            }
            if (phase != HydraPhase.ENRAGED && percent(boss) <= phase.endsAtPercent) {
                HydraPhase next = phase.next();
                BossFx.swapForm(boss, next.bossId, boss.getBlockPos(), false,
                        "hydra;" + home.getX() + ";" + home.getY() + ";" + home.getZ());
            }
        }
    }

    /**
     * The Grotesque Guardians (OSRS Wiki, Grotesque Guardians/Strategies). Phase 1: Dusk is immune and Dawn is fought
     * until 55%, when she flies up. Phase 2: Dusk is attackable; after he attacks he charges his blinding attack (step
     * away or be knocked back for the high 20s) while Dawn throws rockfalls. Phase 3 (Dusk at 55%): lightning across
     * the rooftop, then Dawn is attackable again and Dusk immune; her energy spheres heal her 90 each unless absorbed.
     * Phase 4 (Dawn dead): Dusk absorbs her essence and immediately uses his prison attack (immune until it ends):
     * escape the cell within 5 ticks or take up to 65, which heals him; he repeats it and explodes point blank.
     */
    static final class Dusk extends ScriptedBoss {
        private DataBossEntity dawn;
        private int special = 15;
        /** 1-4 as above. */
        int stage = 1;
        private boolean firstPrison;
        private boolean blindNext;
        private boolean prisonNext = true;

        @Override
        public void onSpawn(DataBossEntity boss) {
            ServerPlayerEntity target = boss.getTarget() instanceof ServerPlayerEntity p ? p : null;
            dawn = BossFx.spawnBoss(boss, "dawn", boss.getBlockPos().add(3, 0, 0), target);
            if (dawn != null && dawn.script() instanceof Dawn d) {
                d.dusk = this;
            }
            BossFx.say(boss, "The Grotesque Guardians awaken! Dusk is protected while Dawn fights. (Dusk: melee only, Dawn: ranged/magic only)");
        }

        private boolean dawnAlive() {
            return dawn != null && dawn.isAlive();
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return stage == 2 || stage == 4 && !firstPrison;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (stage < 4 && !dawnAlive()) {
                stage = 4;
                special = 1;
                firstPrison = true;
                prisonNext = true;
                BossFx.say(boss, "Dusk absorbs the remnants of Dawn's essence!");
                BossFx.burst(boss, ParticleTypes.SOUL_FIRE_FLAME, 30, 1.5);
            } else if (stage == 1 && dawn.getHealth() <= dawn.getMaxHealth() * 0.55f) {
                stage = 2;
                special = 6;
                BossFx.say(boss, "Dawn ascends into the sky! Dusk swoops down to attack!");
            } else if (stage == 2 && boss.getHealth() <= boss.getMaxHealth() * 0.55f) {
                stage = 3;
                special = 14;
                BossFx.say(boss, "Dawn descends. Dusk takes to his perch, and lightning crackles across the rooftop!");
                if (target != null) {
                    lightning(boss, target);
                }
            }
            if (target == null || --special > 0) {
                return;
            }
            special = 14;
            switch (stage) {
                case 1, 3 -> rockfall(boss, target);
                case 2 -> {
                    if (blindNext) {
                        blinding(boss);
                    } else {
                        rockfall(boss, target);
                    }
                    blindNext = !blindNext;
                    special = blindNext ? 8 : 10;
                }
                default -> {
                    if (prisonNext) {
                        prison(boss, target);
                    } else {
                        perform(boss, target, SlayerBosses.DUSK_EXPLOSION);
                    }
                    prisonNext = !prisonNext;
                    special = 12;
                }
            }
        }

        private void rockfall(DataBossEntity boss, ServerPlayerEntity target) {
            if (!dawnAlive()) {
                return;
            }
            BossFx.warn(boss, "Rocks fall from above!");
            for (int i = 0; i < 4; i++) {
                namedStrike("falling_rocks", boss, i == 0 ? target.getPos() : around(boss, target.getPos(), 4), 3, 22, 1.3, ParticleTypes.ASH);
            }
        }

        private void lightning(DataBossEntity boss, ServerPlayerEntity target) {
            BossFx.warn(boss, "Lightning crackles across the rooftop!");
            for (int i = 0; i < 5; i++) {
                // One vortex always targets the player's tile; each deals 5-10.
                Vec3d at = i == 0 ? target.getPos() : around(boss, homeVec(), 10);
                namedStrike("lightning", boss, at, 3, 0, 1.5, ParticleTypes.ELECTRIC_SPARK, (hit, pos) -> {
                    for (ServerPlayerEntity h : hit) {
                        com.runecraft.combat.CombatHooks.nameAttack("lightning", null);
                        CombatHandler.applyDamage(boss, h, 5 + h.getRandom().nextInt(6));
                    }
                });
            }
        }

        /** Dusk's missing wing glows: anyone next to him is knocked back and hit for the high 20s. */
        private void blinding(DataBossEntity boss) {
            BossFx.warn(boss, "Dusk's missing wing begins to glow... step away from him!");
            double reach = boss.getWidth() / 2 + 1.8;
            namedStrike("blinding", boss, boss.getPos(), 3, 0, reach, ParticleTypes.END_ROD, (hit, pos) -> {
                for (ServerPlayerEntity h : hit) {
                    com.runecraft.combat.CombatHooks.nameAttack("blinding", null);
                    CombatHandler.applyDamage(boss, h, 22 + h.getRandom().nextInt(8)); // approx: "damage in the high 20s"
                    h.takeKnockback(1.2, boss.getX() - h.getX(), boss.getZ() - h.getZ());
                    h.velocityModified = true;
                    h.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                            net.minecraft.entity.effect.StatusEffects.BLINDNESS, 60, 0, false, false));
                }
            });
        }

        /** Drags the player into a cell of blue flames; still inside after 5 ticks: up to 65, healing Dusk. */
        private void prison(DataBossEntity boss, ServerPlayerEntity target) {
            CombatAchievementService.countNear(boss, CombatAchievements.COUNT_DUSK_PRISON);
            Vec3d cell = around(boss, homeVec(), 6);
            target.requestTeleport(cell.x, cell.y, cell.z);
            BossFx.say(boss, "Dusk drags you into a prison of flames! Escape the cell!");
            namedStrike("prison", boss, cell, 5, 0, 1.4, ParticleTypes.SOUL_FIRE_FLAME, (hit, pos) -> {
                firstPrison = false;
                for (ServerPlayerEntity h : hit) {
                    int dmg = h.getRandom().nextInt(66);
                    com.runecraft.combat.CombatHooks.nameAttack("prison", null);
                    CombatHandler.applyDamage(boss, h, dmg);
                    if (boss.isAlive()) {
                        boss.heal(mcHp(boss, dmg));
                    }
                }
            });
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            if (dawnAlive()) {
                dawn.discard();
            }
        }
    }

    /** Dawn: immune while she is in the sky (phase 2); in phase 3 her energy spheres heal her 90 unless absorbed. */
    static final class Dawn extends ScriptedBoss {
        private int spheres = 12;
        Dusk dusk;

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return dusk == null || dusk.stage != 2;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (target == null || --spheres > 0) {
                return;
            }
            spheres = 18;
            if (dusk != null && dusk.stage != 3) {
                return;
            }
            BossFx.warn(boss, "Dawn fires energy spheres - stand on them to absorb them!");
            for (int i = 0; i < 3; i++) {
                strike(boss, around(boss, target.getPos(), 4), 5, 0, 1.0, ParticleTypes.END_ROD, (hit, pos) -> {
                    if (hit.isEmpty()) {
                        boss.heal(mcHp(boss, 90));
                        CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_DAWN_HEALED);
                        BossFx.world(boss).spawnParticles(ParticleTypes.HEART, boss.getX(), boss.getY() + 2, boss.getZ(), 3, 0.5, 0.3, 0.5, 0);
                    }
                    // Absorbing a sphere has no penalty (OSRS Wiki).
                });
            }
        }
    }

    /** Araxxor: hatches araxytes from eggs until it enrages at 25% (then cleaves and stops hatching). */
    static final class Araxxor extends ScriptedBoss {
        private final List<MobEntity> araxytes = new ArrayList<>();
        private int eggTimer = 20;

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
            if (target == null || boss.phase() >= 0 || --eggTimer > 0) {
                return;
            }
            eggTimer = 22;
            araxytes.removeIf(m -> !m.isAlive());
            if (araxytes.size() >= 3) {
                return;
            }
            String[] kinds = {"acidic_araxyte", "mirrorback_araxyte", "ruptura_araxyte"};
            String kind = kinds[boss.getRandom().nextInt(kinds.length)];
            MobEntity m = BossFx.spawnMinion(boss, kind, around(boss, homeVec(), 8), target);
            if (m != null) {
                araxytes.add(m);
                BossFx.say(boss, "An egg hatches into " + com.runecraft.util.Msg.article(kind.replace('_', ' ')) + "!");
            }
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            araxytes.forEach(MobEntity::discard);
            araxytes.clear();
        }

        /** "Perfect Araxxor 2": no damage on it once it has enraged. */
        @Override
        protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (boss.phase() >= 0 && damage > 0 && attacker != null) {
                CombatAchievementService.flag(attacker, CombatAchievements.FLAG_ARAXXOR_ENRAGE_HIT);
            }
            return damage;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
            araxytes.forEach(MobEntity::discard);
        }
    }
}
