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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Scripts for the Slayer bosses. */
final class SlayerScripts {
    private SlayerScripts() {
    }

    /** Kraken: starts as a whirlpool; disturb all four tentacles before the Kraken itself surfaces. */
    static final class Kraken extends ScriptedBoss {
        private final List<Mob> tentacles = new ArrayList<>();
        private boolean risen;

        @Override
        public void onSpawn(DataBossEntity boss) {
            Vec3 c = boss.position();
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + Math.PI / 2 * i;
                Mob t = BossFx.spawnMinion(boss, "enormous_tentacle",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 6, c.z + Math.sin(a) * 6, c.y), null);
                if (t != null) {
                    tentacles.add(t);
                }
            }
            boss.setInvisible(true);
            BossFx.say(boss, "Four whirlpools churn around a larger one... Disturb the tentacles first!");
        }

        private boolean disturbed() {
            for (Mob t : tentacles) {
                if (t.isAlive() && t.getHealth() >= t.getMaxHealth()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return risen ? null : IDLE_RANGED;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return risen;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (!risen) {
                BossFx.ring(BossFx.world(boss), boss.position(), 2.0, ParticleTypes.BUBBLE);
                if (disturbed()) {
                    risen = true;
                    boss.setInvisible(false);
                    BossFx.burst(boss, ParticleTypes.SPLASH, 60, 2.0);
                    BossFx.say(boss, "The Kraken rises from the whirlpool!");
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            tentacles.forEach(Mob::discard);
        }
    }

    /** Thermonuclear smoke devil: without a face mask or slayer helmet its smoke chokes and drains you. */
    static final class Thermy extends ScriptedBoss {
        private static final Set<String> MASKS = Set.of("facemask", "face_mask", "gas_mask", "slayer_helmet", "slayer_helmet_i",
                "black_slayer_helmet", "green_slayer_helmet", "red_slayer_helmet", "purple_slayer_helmet");

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            BossFx.burst(boss, ParticleTypes.LARGE_SMOKE, 6, 1.5);
            if (now % 4 != 0) {
                return;
            }
            for (ServerPlayer p : BossFx.players(boss, 14)) {
                if (!MASKS.contains(Inv.idOf(p.getItemBySlot(EquipmentSlot.HEAD)))) {
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
        private final Mob[] soulMobs = new Mob[3];

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
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
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
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
                Vec3 p = target.position();
                namedStrike("lava_pool", boss, p, 4, 15, 1.4, ParticleTypes.LAVA);
                namedStrike("lava_pool", boss, around(boss, p, 3), 4, 15, 1.4, ParticleTypes.LAVA);
                namedStrike("lava_pool", boss, around(boss, p, 3), 4, 15, 1.4, ParticleTypes.LAVA);
            }
        }

        private void summonSouls(DataBossEntity boss, ServerPlayer target) {
            BossFx.say(boss, "Cerberus: Aaarrrooooooo! Summoned souls float in - ranged, then magic, then melee!");
            CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_CERBERUS_SOULS);
            String[] ids = {"summoned_soul_ranged", "summoned_soul_magic", "summoned_soul_melee"};
            Vec3 c = homeVec();
            for (int i = 0; i < 3; i++) {
                soulMobs[i] = BossFx.spawnMinion(boss, ids[i], BossFx.ground(BossFx.world(boss), c.x - 3 + i * 3, c.z - 9, c.y), null);
            }
            soulStep = 0;
            cooldownSouls = 6;
        }

        private int cooldownSouls;

        private void tickSouls(DataBossEntity boss, ServerPlayer target) {
            if (--cooldownSouls > 0) {
                return;
            }
            Prayer[] prayers = {Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MELEE};
            String[] names = {"ranged", "magic", "melee"};
            Mob soul = soulMobs[soulStep];
            if (soul != null && soul.isAlive()) {
                BossFx.world(boss).sendParticles(ParticleTypes.SOUL, soul.getX(), soul.getY() + 1, soul.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                if (BossFx.protects(target, prayers[soulStep])) {
                    boolean spectral = "spectral_spirit_shield".equals(Inv.idOf(target.getOffhandItem()));
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
        private final List<Mob> systems = new ArrayList<>();
        private final List<Vec3> tentacles = new ArrayList<>();
        private int stage = 1;
        private boolean awake;
        private int disoriented;
        private int disorientDamage;
        private int spawnTimer = 10;
        private int explodeTimer = -1;
        private int miasmaTimer = 6;

        @Override
        public void onSpawn(DataBossEntity boss) {
            Vec3 c = boss.position();
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + Math.PI / 2 * i;
                Mob m = BossFx.spawnMinion(boss, "respiratory_system",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 7, c.z + Math.sin(a) * 7, c.y), null);
                if (m != null) {
                    systems.add(m);
                    // Protected by the tentacles while the Sire is awake and not disoriented; open (always hitting) otherwise.
                    MinionRules.set(m, new MinionRules.Rule() {
                        @Override
                        public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type) {
                            return systemsOpen() ? 10.0 : 1.0; // approx: OSRS deals at least half the max hit when open
                        }

                        @Override
                        public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type, int d) {
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
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
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
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (stage != 1) {
                return damage;
            }
            if (!awake) {
                awake = true;
                BossFx.say(boss, "The Abyssal Sire stirs and its tentacles awaken!");
                BossFx.sound(boss, SoundEvents.WITHER_AMBIENT, 0.6f);
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
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (distance > 4) {
                return SlayerBosses.SIRE_MIASMA;
            }
            int r = boss.getRandom().nextInt(8);
            return r < 4 ? SlayerBosses.SIRE_TENDRIL : r < 6 ? SlayerBosses.SIRE_SLAM : SlayerBosses.SIRE_MIASMA;
        }

        private void tickTentacles(DataBossEntity boss) {
            boolean active = tentaclesActive();
            for (Vec3 t : tentacles) {
                BossFx.world(boss).sendParticles(active ? ParticleTypes.SQUID_INK : ParticleTypes.SMOKE, t.x, t.y + 1.0, t.z,
                        active ? 3 : 1, 0.2, 0.6, 0.2, 0);
            }
            if (!active || now % 4 != 0) {
                return;
            }
            for (ServerPlayer p : BossFx.players(boss, 24)) {
                for (Vec3 t : tentacles) {
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
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
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
                if (systems.stream().noneMatch(Mob::isAlive)) {
                    stage = 2;
                    disoriented = 0;
                    BossFx.say(boss, "The Abyssal Sire awakens in a fury!");
                    BossFx.sound(boss, SoundEvents.WITHER_SPAWN, 0.6f);
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
                        BossFx.spawnMinion(boss, "abyssal_spawn", around(boss, boss.position(), 3), target);
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
                BossFx.spawnMinion(boss, stage >= 3 ? "abyssal_scion" : "abyssal_spawn", around(boss, boss.position(), 3), target);
                if (stage >= 3) {
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_SIRE_SCION);
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            systems.forEach(Mob::discard);
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
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
            return true;
        }

        @Override
        protected String creditAs() {
            return phase == HydraPhase.GREEN ? null : "alchemical_hydra";
        }

        private Vec3 vent(String colour) {
            Vec3 c = homeVec();
            return switch (colour) {
                case "red" -> c.add(-7, 0, -6);
                case "green" -> c.add(7, 0, -6);
                default -> c.add(0, 0, 8);
            };
        }

        private ParticleOptions ventParticle(String colour) {
            return switch (colour) {
                case "red" -> ParticleTypes.FLAME;
                case "green" -> ParticleTypes.SNEEZE;
                default -> ParticleTypes.ELECTRIC_SPARK;
            };
        }

        private void placeVents(DataBossEntity boss) {
            for (String c : new String[]{"red", "green", "blue"}) {
                Vec3 v = BossFx.ground(BossFx.world(boss), vent(c).x, vent(c).z, home.getY());
                BlockPos p = BlockPos.containing(v.x, v.y - 1, v.z);
                var state = switch (c) {
                    case "red" -> Blocks.RED_CONCRETE.defaultBlockState();
                    case "green" -> Blocks.LIME_CONCRETE.defaultBlockState();
                    default -> Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
                };
                BossFx.world(boss).setBlockAndUpdate(p, state);
            }
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
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

        private void special(DataBossEntity boss, ServerPlayer target) {
            Vec3 p = target.position();
            switch (phase) {
                case GREEN, ENRAGED -> {
                    BossFx.warn(boss, "The Alchemical Hydra spits poison!");
                    for (int i = 0; i < 5; i++) {
                        Vec3 at = i == 0 ? p : around(boss, p, 3);
                        namedStrike("poison_pool", boss, at, 2, 12, 1.2, ParticleTypes.SNEEZE, (hit, pos) ->
                                com.runecraft.boss.Mechanics.addHazard(BossFx.world(boss), pos, 4, 10, boss));
                    }
                }
                case BLUE -> {
                    BossFx.warn(boss, "The Alchemical Hydra calls down lightning - keep moving!");
                    for (int i = 0; i < 4; i++) {
                        final int delay = 2 + i * 2;
                        namedStrike("lightning", boss, around(boss, p, 2 + i), delay, 20, 1.3, ParticleTypes.ELECTRIC_SPARK, (hit, pos) -> {
                            for (ServerPlayer h : hit) {
                                BossFx.stun(h, 2);
                            }
                        });
                    }
                }
                case RED -> {
                    BossFx.warn(boss, "The Alchemical Hydra breathes walls of fire - get between them!");
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_HYDRA_FLAME_WALL);
                    Vec3 dir = p.subtract(boss.position()).normalize();
                    Vec3 side = new Vec3(-dir.z, 0, dir.x);
                    for (int i = -5; i <= 5; i++) {
                        for (int s : new int[]{-2, 2}) {
                            Vec3 at = p.add(dir.scale(i * 1.2)).add(side.scale(s));
                            namedStrike("fire_wall", boss, BossFx.ground(BossFx.world(boss), at.x, at.z, p.y), 3, 20, 1.0, ParticleTypes.FLAME);
                        }
                    }
                    namedStrike("fire_wall", boss, p, 6, 20, 1.0, ParticleTypes.FLAME);
                }
            }
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (shielded && damage > 0) {
                double scaled = damage * SlayerBosses.HYDRA_SHIELD_MULTIPLIER;
                int whole = (int) scaled;
                return whole + (boss.getRandom().nextDouble() < scaled - whole ? 1 : 0);
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
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
                    Vec3 v = vent(c);
                    BossFx.column(BossFx.world(boss), new Vec3(v.x, home.getY(), v.z), ventParticle(c), 8);
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
                BossFx.swapForm(boss, next.bossId, boss.blockPosition(), false,
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
            ServerPlayer target = boss.getTarget() instanceof ServerPlayer p ? p : null;
            dawn = BossFx.spawnBoss(boss, "dawn", boss.blockPosition().offset(3, 0, 0), target);
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
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
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

        private void rockfall(DataBossEntity boss, ServerPlayer target) {
            if (!dawnAlive()) {
                return;
            }
            BossFx.warn(boss, "Rocks fall from above!");
            for (int i = 0; i < 4; i++) {
                namedStrike("falling_rocks", boss, i == 0 ? target.position() : around(boss, target.position(), 4), 3, 22, 1.3, ParticleTypes.ASH);
            }
        }

        private void lightning(DataBossEntity boss, ServerPlayer target) {
            BossFx.warn(boss, "Lightning crackles across the rooftop!");
            for (int i = 0; i < 5; i++) {
                // One vortex always targets the player's tile; each deals 5-10.
                Vec3 at = i == 0 ? target.position() : around(boss, homeVec(), 10);
                namedStrike("lightning", boss, at, 3, 0, 1.5, ParticleTypes.ELECTRIC_SPARK, (hit, pos) -> {
                    for (ServerPlayer h : hit) {
                        com.runecraft.combat.CombatHooks.nameAttack("lightning", null);
                        CombatHandler.applyDamage(boss, h, 5 + h.getRandom().nextInt(6));
                    }
                });
            }
        }

        /** Dusk's missing wing glows: anyone next to him is knocked back and hit for the high 20s. */
        private void blinding(DataBossEntity boss) {
            BossFx.warn(boss, "Dusk's missing wing begins to glow... step away from him!");
            double reach = boss.getBbWidth() / 2 + 1.8;
            namedStrike("blinding", boss, boss.position(), 3, 0, reach, ParticleTypes.END_ROD, (hit, pos) -> {
                for (ServerPlayer h : hit) {
                    com.runecraft.combat.CombatHooks.nameAttack("blinding", null);
                    CombatHandler.applyDamage(boss, h, 22 + h.getRandom().nextInt(8)); // approx: "damage in the high 20s"
                    h.knockback(1.2, boss.getX() - h.getX(), boss.getZ() - h.getZ());
                    h.hurtMarked = true;
                    h.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0, false, false));
                }
            });
        }

        /** Drags the player into a cell of blue flames; still inside after 5 ticks: up to 65, healing Dusk. */
        private void prison(DataBossEntity boss, ServerPlayer target) {
            CombatAchievementService.countNear(boss, CombatAchievements.COUNT_DUSK_PRISON);
            Vec3 cell = around(boss, homeVec(), 6);
            target.teleportTo(cell.x, cell.y, cell.z);
            BossFx.say(boss, "Dusk drags you into a prison of flames! Escape the cell!");
            namedStrike("prison", boss, cell, 5, 0, 1.4, ParticleTypes.SOUL_FIRE_FLAME, (hit, pos) -> {
                firstPrison = false;
                for (ServerPlayer h : hit) {
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
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
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
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (target == null || --spheres > 0) {
                return;
            }
            spheres = 18;
            if (dusk != null && dusk.stage != 3) {
                return;
            }
            BossFx.warn(boss, "Dawn fires energy spheres - stand on them to absorb them!");
            for (int i = 0; i < 3; i++) {
                strike(boss, around(boss, target.position(), 4), 5, 0, 1.0, ParticleTypes.END_ROD, (hit, pos) -> {
                    if (hit.isEmpty()) {
                        boss.heal(mcHp(boss, 90));
                        CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_DAWN_HEALED);
                        BossFx.world(boss).sendParticles(ParticleTypes.HEART, boss.getX(), boss.getY() + 2, boss.getZ(), 3, 0.5, 0.3, 0.5, 0);
                    }
                    // Absorbing a sphere has no penalty (OSRS Wiki).
                });
            }
        }
    }

    /** Araxxor: hatches araxytes from eggs until it enrages at 25% (then cleaves and stops hatching). */
    /**
     * Self-test (boss:araxxor_eggs): nine eggs ring the lair, the third standard attack hatches the first, and a destroyed
     * egg is dropped from the rotation.
     */
    static String selfTestAraxxor(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Araxxor a)) {
            throw new IllegalStateException("Araxxor has no Araxxor script");
        }
        try {
            a.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
            if (a.eggsLeft() != com.runecraft.osrs.content.bosses.BossMechanics.ARAXXOR_EGGS) {
                throw new IllegalStateException(a.eggsLeft() + " eggs placed, expected 9");
            }
            for (int i = 0; i < com.runecraft.osrs.content.bosses.BossMechanics.ARAXXOR_FIRST_HATCH; i++) {
                a.onHitPlayer(boss, p, 0, AttackType.RANGED);
            }
            a.araxytes.removeIf(m -> !m.isAlive());
            if (a.eggsLeft() != 8 || a.araxytes.size() != 1) {
                throw new IllegalStateException("after 3 attacks: " + a.eggsLeft() + " eggs, " + a.araxytes.size() + " araxytes");
            }
            Mob egg = a.eggs.stream().filter(e -> e != null && e.isAlive()).findFirst().orElseThrow();
            egg.setHealth(0f);
            a.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
            if (a.eggsLeft() != 7) {
                throw new IllegalStateException(a.eggsLeft() + " eggs after destroying one, expected 7");
            }
            return "9 eggs, first hatched into " + a.eggKinds.get(0) + ", destroyed egg skipped";
        } finally {
            a.onDeath(boss, p);
        }
    }

    /**
     * Araxxor (OSRS Wiki "Araxxor/Strategies"): nine eggs ring the lair, three of each colour in one of the three wiki
     * patterns; the first hatches after three standard attacks, clockwise from the south-easternmost, then one every six.
     * Players may destroy eggs (65 hitpoints; a destroyed egg is skipped); damage on an egg carries over to its araxyte, and
     * an egg that took more than the araxyte's 58 hitpoints just crumbles when its turn comes.
     */
    static final class Araxxor extends ScriptedBoss {
        private final List<Mob> araxytes = new ArrayList<>();
        /** The hatched ruptura araxytes (they explode instead of attacking). */
        private final List<Mob> rupturas = new ArrayList<>();
        private final List<Mob> eggs = new ArrayList<>();
        private List<String> eggKinds = List.of();
        private int nextEgg;
        private int standardAttacks;

        private void placeEggs(DataBossEntity boss) {
            eggKinds = com.runecraft.osrs.content.bosses.BossMechanics.araxxorEggs(boss.getRandom().nextInt(3));
            Vec3 c = homeVec();
            for (int i = 0; i < eggKinds.size(); i++) {
                // Clockwise from the south-east (Minecraft +x east, +z south: increasing angle turns clockwise from above).
                double a = Math.PI / 4 + i * 2 * Math.PI / eggKinds.size();
                eggs.add(BossFx.spawnMinion(boss, com.runecraft.osrs.content.bosses.BossMechanics.ARAXYTE_EGG,
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 9, c.z + Math.sin(a) * 9, c.y), null));
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (eggs.isEmpty() && target != null) {
                placeEggs(boss);
            }
            tickRupturas(boss);
            for (int i = 0; i < eggs.size(); i++) {
                Mob egg = eggs.get(i);
                if (egg != null && !egg.isAlive()) {
                    eggs.set(i, null);
                    if (egg.isDeadOrDying()) {
                        java.util.Set<ServerPlayer> near = new java.util.LinkedHashSet<>(BossFx.players(boss, 24));
                        if (target != null) {
                            near.add(target);
                        }
                        for (ServerPlayer p : near) {
                            CombatAchievementService.count(p, com.runecraft.osrs.content.bosses.BossMechanics.COUNT_ARAXXOR_EGGS, 1);
                        }
                    }
                }
            }
        }

        /** Counts Araxxor's standard attacks (before it enrages) and hatches the next egg on the 3rd, 9th, 15th... */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (boss.phase() < 0 && com.runecraft.osrs.content.bosses.BossMechanics.araxxorHatches(++standardAttacks)) {
                hatch(boss, player);
            }
            return damage;
        }

        private void hatch(DataBossEntity boss, ServerPlayer target) {
            araxytes.removeIf(m -> !m.isAlive());
            for (int k = 0; k < eggs.size(); k++) {
                int i = (nextEgg + k) % eggs.size();
                Mob egg = eggs.get(i);
                if (egg == null || !egg.isAlive()) {
                    continue; // destroyed: the next egg in the rotation hatches instead
                }
                nextEgg = i + 1;
                int eggDamage = (int) Math.round((egg.getMaxHealth() - egg.getHealth()) / Math.max(1f, egg.getMaxHealth())
                        * com.runecraft.osrs.content.bosses.BossMechanics.ARAXYTE_EGG_HP);
                int hp = com.runecraft.osrs.content.bosses.BossMechanics.araxyteHitpoints(eggDamage);
                Vec3 at = egg.position();
                egg.discard();
                eggs.set(i, null);
                if (hp <= 0) {
                    BossFx.say(boss, "A damaged egg crumbles before it can hatch.");
                    return;
                }
                String kind = eggKinds.get(i);
                Mob m = BossFx.spawnMinion(boss, kind, at, target);
                if (m != null) {
                    m.setHealth(Math.max(1f, m.getMaxHealth() * hp / com.runecraft.osrs.content.bosses.BossMechanics.ARAXYTE_HP));
                    araxytes.add(m);
                    if ("ruptura_araxyte".equals(kind)) {
                        rupturas.add(m);
                    }
                    BossFx.say(boss, "An egg hatches into " + com.runecraft.util.Msg.article(kind.replace('_', ' ')) + "!");
                }
                return;
            }
        }

        /** Ruptura araxytes walk up to a player and explode once in melee range. */
        private void tickRupturas(DataBossEntity boss) {
            rupturas.removeIf(m -> !m.isAlive());
            for (Mob r : new ArrayList<>(rupturas)) {
                for (ServerPlayer p : BossFx.players(r, 3)) {
                    if (p.distanceToSqr(r) <= 1.6 * 1.6) {
                        explode(boss, r);
                        break;
                    }
                }
            }
        }

        /**
         * A ruptura araxyte explodes (OSRS Wiki "Araxxor/Strategies"): players take up to 70 (21 less per tile away, scaled by
         * its health); Araxxor, other araxytes and eggs take 80 under it, 64 next to it and at least 33 up to 3 tiles away.
         * Three araxytes killed by one explosion is "Araxyte Betrayal". Returns how many araxytes it killed.
         */
        int explode(DataBossEntity boss, Mob r) {
            Vec3 at = r.position();
            double hp = r.getHealth() / Math.max(1f, r.getMaxHealth());
            rupturas.remove(r);
            r.discard();
            BossFx.world(boss).sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 4, 1.2, 0.4, 1.2, 0);
            BossFx.sound(r, SoundEvents.GENERIC_EXPLODE.value(), 0.8f);
            for (ServerPlayer p : BossFx.players(r, BossMechanics.RUPTURA_RANGE + 1)) {
                int dmg = BossMechanics.rupturaPlayerDamage(tiles(at, p.position()), hp);
                if (dmg > 0) {
                    com.runecraft.combat.CombatHooks.nameAttack("ruptura_explosion", AttackType.CRUSH);
                    CombatHandler.applyDamage(r, p, dmg);
                }
            }
            int killed = 0;
            List<Mob> victims = new ArrayList<>(araxytes);
            eggs.forEach(e -> {
                if (e != null) {
                    victims.add(e);
                }
            });
            for (Mob v : victims) {
                if (v == r || !v.isAlive()) {
                    continue;
                }
                int dmg = BossMechanics.rupturaEnemyDamage(tiles(at, v.position()));
                if (dmg > 0) {
                    CombatHandler.applyDamage(null, v, dmg);
                    if (!v.isAlive() && araxytes.contains(v)) {
                        killed++;
                    }
                }
            }
            int bossDmg = BossMechanics.rupturaEnemyDamage(tiles(at, boss.position()) - 1); // Araxxor is a large creature
            if (bossDmg > 0 && boss.isAlive()) {
                CombatHandler.applyDamage(null, boss, bossDmg);
            }
            if (killed >= BossMechanics.ARAXYTE_BETRAYAL_KILLS) {
                CombatAchievementService.flagNear(boss, BossMechanics.FLAG_ARAXYTE_BETRAYAL);
            }
            return killed;
        }

        private static int tiles(Vec3 a, Vec3 b) {
            return (int) Math.floor(Math.max(Math.abs(a.x - b.x), Math.abs(a.z - b.z)));
        }

        /** The eggs still standing (for the self-test). */
        long eggsLeft() {
            return eggs.stream().filter(e -> e != null && e.isAlive()).count();
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            araxytes.forEach(Mob::discard);
            araxytes.clear();
            rupturas.clear();
        }

        /** "Perfect Araxxor 2": no damage on it once it has enraged. */
        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (boss.phase() >= 0 && damage > 0 && attacker != null) {
                CombatAchievementService.flag(attacker, CombatAchievements.FLAG_ARAXXOR_ENRAGE_HIT);
            }
            return damage;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            araxytes.forEach(Mob::discard);
            eggs.forEach(e -> {
                if (e != null) {
                    e.discard();
                }
            });
        }
    }
}
