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

    /**
     * Kraken (OSRS Wiki "Kraken/Strategies"): it lies under a large whirlpool among four small ones. Disturbing a small whirlpool
     * (any hit) brings up its Enormous Tentacle, which then attacks; once all four are up, disturbing the large whirlpool (a
     * ranged or magic attack, melee can't reach it) wakes the Kraken. Its blast and the tentacles' splashes are typeless.
     * Ranged attacks deal a seventh of their damage to it.
     */
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
                    t.setNoAi(true); // a whirlpool until it is disturbed
                    tentacles.add(t);
                }
            }
            boss.setInvisible(true);
            BossFx.say(boss, "Four whirlpools churn around a larger one... Disturb the small ones first!");
        }

        /** Self-test (boss:fight:kraken): a player disturbs every whirlpool with one hit each. Returns whether one was left. */
        boolean selfTestDisturb(ServerPlayer p) {
            boolean any = false;
            for (Mob t : tentacles) {
                if (t.isAlive() && t.getHealth() >= t.getMaxHealth()) {
                    CombatHandler.applyDamage(p, t, 1);
                    any = true;
                }
            }
            return any;
        }

        /** Whether all four small whirlpools have been disturbed (or their tentacles killed). */
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

        /** The large whirlpool can be disturbed once the four small ones are; then the Kraken itself. */
        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return risen || disturbed();
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (!risen) {
                if (!type.isMelee()) {
                    rise(boss); // the disturbing attack only wakes it
                }
                return 0;
            }
            return type == AttackType.RANGED ? SlayerBosses.krakenRangedDamage(damage) : damage;
        }

        private void rise(DataBossEntity boss) {
            risen = true;
            boss.setInvisible(false);
            BossFx.burst(boss, ParticleTypes.SPLASH, 60, 2.0);
            BossFx.say(boss, "The Kraken rises from the whirlpool!");
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            for (Mob t : tentacles) {
                if (t.isAlive() && t.isNoAi()) {
                    if (t.getHealth() < t.getMaxHealth()) {
                        t.setNoAi(false); // disturbed: the Enormous Tentacle surfaces and attacks
                        if (target != null) {
                            t.setTarget(target);
                        }
                    } else {
                        BossFx.ring(BossFx.world(boss), t.position(), 1.0, ParticleTypes.BUBBLE);
                    }
                }
            }
            if (!risen) {
                BossFx.ring(BossFx.world(boss), boss.position(), 2.0, ParticleTypes.BUBBLE);
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            tentacles.forEach(Mob::discard);
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            tentacles.forEach(Mob::discard);
        }
    }

    /**
     * Thermonuclear smoke devil (OSRS Wiki, "Smoke Devil Dungeon"): its typeless blast is in the data. Without a facemask, gas
     * mask or slayer helmet the cave's smoke makes you cough (it can't kill, approx damage), and the devil's own smoke drains
     * every combat stat and Agility to 1 when it hits you (the Slayer module adds the smoke devils' stronger hits).
     */
    static final class Thermy extends ScriptedBoss {
        private static final Set<String> MASKS = Set.of("facemask", "gas_mask", "slayer_helmet", "slayer_helmet_i");
        private static final Skill[] DRAINED = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.AGILITY};
        /** How often the cave's smoke makes an unmasked player cough (approx: the wiki guesses every 20 ticks). */
        private static final int COUGH_EVERY = 20;
        private static final int COUGH_MAX = 6; // approx

        static boolean masked(ServerPlayer p) {
            return MASKS.contains(Inv.idOf(p.getItemBySlot(EquipmentSlot.HEAD)));
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            BossFx.burst(boss, ParticleTypes.LARGE_SMOKE, 6, 1.5);
            if (now % COUGH_EVERY != 0) {
                return;
            }
            for (ServerPlayer p : BossFx.players(boss, 16)) {
                if (!masked(p)) {
                    int hp = (int) Math.ceil(p.getHealth());
                    int cough = Math.min(1 + p.getRandom().nextInt(COUGH_MAX), Math.max(0, hp - 1)); // the smoke can't kill
                    Msg.red(p, cough > 0 && hp - cough <= 1 ? "*wheeze*" : "*cough* The thick smoke burns your lungs! You need a facemask.");
                    if (cough > 0) {
                        com.runecraft.combat.CombatHooks.nameAttack("smoke", null);
                        CombatHandler.applyDamage(null, p, cough);
                    }
                }
            }
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (!masked(player)) {
                com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(player);
                boolean drained = false;
                for (Skill s : DRAINED) {
                    int to = 1 - data.skills().level(s);
                    if (data.skills().boost(s) > to) {
                        data.skills().setBoost(s, to);
                        drained = true;
                    }
                }
                if (drained) {
                    com.runecraft.network.ServerNetworking.sync(player);
                    Msg.red(player, "You inhale the devil's smoke and your strength leaves you!");
                }
            }
            return damage;
        }
    }

    /**
     * Cerberus (OSRS Wiki "Cerberus/Strategies"), on her attack pattern ({@link SlayerBosses#cerberusMove}): the triple attack
     * (magic, ranged, then a bite at any distance, 2 ticks apart) first and every tenth attack; under 400 hitpoints the
     * summoned souls every seventh (they appear in a random order and attack west to east: unprotected 30, 22 with an
     * elysian; protected a 30 prayer drain, 15 with a spectral); under 200 three lava pools every fifth (one under the player,
     * 10 as they form and 15 every two ticks on them). Auto attacks bite in melee range, else ranged or magic.
     */
    static final class Cerberus extends ScriptedBoss {
        private static final String[] SOUL_IDS = {"summoned_soul_ranged", "summoned_soul_magic", "summoned_soul_melee"};
        private static final Prayer[] SOUL_PRAYERS = {Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MELEE};
        private static final String[] SOUL_NAMES = {"ranged", "magic", "melee"};

        private record LavaPool(Vec3 pos, long until) {
        }

        private final List<AttackDef> queue = new ArrayList<>();
        private final List<LavaPool> pools = new ArrayList<>();
        /** Her attacks so far in her pattern (the triple attack counts as one). */
        private int pattern;
        private int soulStep = -1;
        private int soulWait;
        private final Mob[] soulMobs = new Mob[3];
        /** The style (index into SOUL_IDS) of the soul in each place, west to east. */
        private final int[] soulStyle = {0, 1, 2};

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
            if (!queue.isEmpty()) {
                return queue.remove(0);
            }
            pattern++;
            SlayerBosses.CerberusMove move = SlayerBosses.cerberusMove(pattern, (int) Math.ceil(boss.getHealth()),
                    boss.getRandom().nextInt(10) == 0);
            switch (move) {
                case TRIPLE -> {
                    queue.add(SlayerBosses.CERB_RANGED);
                    queue.add(SlayerBosses.CERB_TRIPLE_MELEE);
                    return SlayerBosses.CERB_MAGIC;
                }
                case SOULS -> {
                    if (soulStep < 0) {
                        summonSouls(boss);
                        cooldown = attackDelay(boss, SlayerBosses.CERB_MAGIC) - 1;
                        attackCount++;
                        return null;
                    }
                }
                case LAVA -> {
                    lava(boss, target);
                    cooldown = attackDelay(boss, SlayerBosses.CERB_MAGIC) - 1;
                    attackCount++;
                    return null;
                }
                default -> {
                }
            }
            if (distance < SlayerBosses.CERB_MELEE.range() && boss.getRandom().nextInt(3) > 0) {
                return SlayerBosses.CERB_MELEE;
            }
            return boss.getRandom().nextBoolean() ? SlayerBosses.CERB_RANGED : SlayerBosses.CERB_MAGIC;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            // Auto-attacks every 6 ticks (wiki attack speed); the triple attack fires its three styles 2 ticks apart.
            return queue.isEmpty() ? super.attackDelay(boss, attack) : 2;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (soulStep >= 0 && target != null) {
                tickSouls(boss, target);
            }
            tickLava(boss);
        }

        private void summonSouls(DataBossEntity boss) {
            BossFx.say(boss, "Cerberus: Aaarrrooooooo!");
            CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_CERBERUS_SOULS);
            // A random order, standing west to east behind the archaic skull (north of the arena centre).
            List<Integer> order = new ArrayList<>(List.of(0, 1, 2));
            java.util.Collections.shuffle(order, new java.util.Random(boss.getRandom().nextLong()));
            Vec3 c = homeVec();
            for (int i = 0; i < 3; i++) {
                soulStyle[i] = order.get(i);
                soulMobs[i] = BossFx.spawnMinion(boss, SOUL_IDS[soulStyle[i]],
                        BossFx.ground(BossFx.world(boss), c.x - 3 + i * 3, c.z - 9, c.y), null);
            }
            BossFx.warn(boss, "Summoned souls: " + SOUL_NAMES[soulStyle[0]] + ", " + SOUL_NAMES[soulStyle[1]] + ", "
                    + SOUL_NAMES[soulStyle[2]] + " (west to east)");
            soulStep = 0;
            soulWait = 6;
        }

        private void tickSouls(DataBossEntity boss, ServerPlayer target) {
            if (--soulWait > 0) {
                return;
            }
            int style = soulStyle[soulStep];
            Mob soul = soulMobs[soulStep];
            if (soul != null && soul.isAlive()) {
                BossFx.world(boss).sendParticles(ParticleTypes.SOUL, soul.getX(), soul.getY() + 1, soul.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                if (BossFx.protects(target, SOUL_PRAYERS[style])) {
                    boolean spectral = "spectral_spirit_shield".equals(Inv.idOf(target.getOffhandItem()));
                    BossFx.drainPrayer(target, spectral ? SlayerBosses.CERB_SOUL_DRAIN_SPECTRAL : SlayerBosses.CERB_SOUL_DRAIN);
                    CombatAchievementService.count(target, CombatAchievements.COUNT_SOUL_NEGATED, 1);
                    Msg.game(target, "The " + SOUL_NAMES[style] + " soul drains your prayer.");
                } else {
                    boolean elysian = "elysian_spirit_shield".equals(Inv.idOf(target.getOffhandItem()));
                    com.runecraft.combat.CombatHooks.nameAttack("summoned_soul", null);
                    CombatHandler.applyDamage(boss, target, elysian ? SlayerBosses.CERB_SOUL_HIT_ELYSIAN : SlayerBosses.CERB_SOUL_HIT);
                    Msg.red(target, "The " + SOUL_NAMES[style] + " soul strikes you!");
                }
                soul.discard();
            }
            soulStep++;
            soulWait = 2;
            if (soulStep >= 3) {
                soulStep = -1;
            }
        }

        /** "Grrrrrrrrrrrrrr!": three single-tile pools, one under the player and two elsewhere in the arena. */
        private void lava(DataBossEntity boss, ServerPlayer target) {
            BossFx.say(boss, "Cerberus: Grrrrrrrrrrrrrr!");
            Vec3 p = target.position();
            Vec3[] at = {p, around(boss, homeVec(), 8), around(boss, homeVec(), 8)};
            for (Vec3 v : at) {
                namedStrike("lava_pool", boss, v, 3, SlayerBosses.CERB_LAVA_LANDING, 0.8, ParticleTypes.LAVA,
                        (hit, pos) -> pools.add(new LavaPool(pos, now + SlayerBosses.CERB_LAVA_TICKS)));
            }
        }

        private void tickLava(DataBossEntity boss) {
            pools.removeIf(l -> now > l.until());
            for (LavaPool l : pools) {
                BossFx.world(boss).sendParticles(ParticleTypes.LAVA, l.pos().x, l.pos().y + 0.1, l.pos().z, 2, 0.3, 0.02, 0.3, 0);
                if ((l.until() - now) % 2 != 0) {
                    continue;
                }
                for (ServerPlayer p : BossFx.world(boss).getPlayers(pl -> pl.isAlive() && pl.position().distanceToSqr(l.pos()) <= 0.64)) {
                    com.runecraft.combat.CombatHooks.nameAttack("lava_pool", null);
                    CombatHandler.applyDamage(boss, p, SlayerBosses.CERB_LAVA_TICK);
                }
            }
        }

        private void clearSouls() {
            for (Mob m : soulMobs) {
                if (m != null) {
                    m.discard();
                }
            }
            soulStep = -1;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            clearSouls(); // they despawn when she dies; her pools stop burning
            pools.clear();
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            clearSouls();
        }
    }

    /**
     * Abyssal Sire (OSRS Wiki "Abyssal Sire" and its Strategies). Phase 1: it sleeps until attacked; awake, its six tentacles
     * lash anyone near them (up to 30) and shield the four respiratory systems (0-3 damage) while it throws miasma pools and
     * spawns. Disorient it - 75 ranged/magic damage or a shadow spell (Rush 25%, Burst 50%, Blitz 75%, Barrage always) - and
     * for 30 seconds (the tentacles wake after 27) the systems are open; damage to the Sire itself in this phase only counts
     * towards disorienting it, and each system gives 50 Slayer XP. With all four systems gone it awakens in a fury (phase 2):
     * melee swipes and flicks (6, 6 and 26 through Protect from Melee), miasma and spawns from range, and attacking it from
     * more than two tiles away pulls the player in for a portal blast of up to 60. Under 210 hitpoints (phase 3) it walks to
     * the middle of the room and stops, its tentacles wake, four spawns come and miasma pools are its attacks; under 140 it
     * teleports the player next to itself and explodes two ticks later for up to 96, then spawns pour out until 15 minions
     * are alive. A spawn left 15 seconds matures into a scion; every spawn and scion dies with the Sire.
     */
    static final class Sire extends ScriptedBoss {
        private final List<Mob> systems = new ArrayList<>();
        private final List<Vec3> tentacles = new ArrayList<>();
        /** Its spawns and the game tick each appeared (they mature into scions), then its scions. */
        private final java.util.Map<Mob, Long> spawns = new java.util.LinkedHashMap<>();
        private final List<Mob> scions = new ArrayList<>();
        /** 1 systems, 2 melee, 3 latched in the middle, 4 after the explosion. */
        private int stage = 1;
        private boolean awake;
        private int disoriented;
        private int disorientDamage;
        private int spawnTimer = 10;
        private int miasmaTimer = 6;
        private int miasmasAfterExplosion;
        private long portalReadyAt;
        /** A hit took it to 0 before its next phase: it starts the next phase instead (at 20% for phase 3). */
        private boolean clamped;

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

        /** The tentacles lash out while the Sire is awake in phase 1 (from 27 s into a disorientation) and again from phase 3. */
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
            return stage >= 2;
        }

        /** Distance from the Sire's edge, in blocks. */
        private static double edge(DataBossEntity boss, net.minecraft.world.entity.Entity e) {
            return Math.sqrt(boss.distanceToSqr(e)) - boss.getBbWidth() / 2;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (stage != 1) {
                if (stage == 2 && attacker != null && edge(boss, attacker) > 2.5 && now >= portalReadyAt) {
                    portalBlast(boss, attacker); // no safespotting it in phase 2
                }
                if ((stage == 2 || stage == 3) && damage >= boss.getHealth()) {
                    clamped = true; // it can't die before its next phase: the phase change comes first
                    return Math.max(0, (int) Math.ceil(boss.getHealth()) - 1);
                }
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

        /** Pulls a player attacking from afar next to it and discharges its portal two ticks later. */
        private void portalBlast(DataBossEntity boss, ServerPlayer p) {
            portalReadyAt = now + 8;
            pullIn(boss, p);
            perform(boss, p, SlayerBosses.SIRE_PORTAL_BLAST);
        }

        /** Teleports a player to the Sire's edge, on their side of it. */
        private static void pullIn(DataBossEntity boss, ServerPlayer p) {
            Vec3 dir = p.position().subtract(boss.position()).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 0.01 ? new Vec3(-1, 0, 0) : dir.normalize(); // OSRS: the western tile of the row
            Vec3 at = boss.position().add(dir.scale(boss.getBbWidth() / 2 + 0.6));
            Vec3 ground = BossFx.ground(BossFx.world(boss), at.x, at.z, boss.getY());
            p.teleportTo(ground.x, ground.y, ground.z);
            Msg.red(p, "The Abyssal Sire drags you towards it!");
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (stage >= 3) {
                // Latched to the floor: its "attacks" are miasma pools (after the explosion, until three more pools and 15 minions).
                if (stage == 4 && miasmasAfterExplosion >= SlayerBosses.SIRE_MIASMAS_AFTER_EXPLOSION
                        && minions() >= SlayerBosses.SIRE_MAX_MINIONS) {
                    return null;
                }
                if (stage == 4) {
                    miasmasAfterExplosion++;
                }
                return SlayerBosses.SIRE_MIASMA;
            }
            if (distance > 2.5) {
                // Out of its reach: spawns or a miasma pool.
                if (boss.getRandom().nextBoolean()) {
                    spawn(boss, target, around(boss, boss.position(), 3));
                    attackCount++;
                    cooldown = attackDelay(boss, SlayerBosses.SIRE_MIASMA) - 1;
                    return null;
                }
                return SlayerBosses.SIRE_MIASMA;
            }
            return boss.getRandom().nextInt(3) < 2 ? SlayerBosses.SIRE_TENDRIL : SlayerBosses.SIRE_SLAM;
        }

        /** Protect from Melee doesn't fully block its melee: 6, 6 and 26 get through. */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if ((last == SlayerBosses.SIRE_TENDRIL || last == SlayerBosses.SIRE_SLAM) && type.isMelee()
                    && BossFx.protects(player, Prayer.PROTECT_FROM_MELEE)) {
                return SlayerBosses.sireThroughPrayer(last, damage);
            }
            return damage;
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

        private int minions() {
            spawns.keySet().removeIf(m -> !m.isAlive());
            scions.removeIf(m -> !m.isAlive());
            return spawns.size() + scions.size();
        }

        private void spawn(DataBossEntity boss, ServerPlayer target, Vec3 at) {
            Mob m = BossFx.spawnMinion(boss, "abyssal_spawn", at, target);
            if (m != null) {
                spawns.put(m, now);
            }
        }

        /** A spawn left alive 15 seconds matures into a scion ("They Grow Up Too Fast"). */
        private void tickSpawns(DataBossEntity boss, ServerPlayer target) {
            for (java.util.Iterator<java.util.Map.Entry<Mob, Long>> it = spawns.entrySet().iterator(); it.hasNext(); ) {
                java.util.Map.Entry<Mob, Long> e = it.next();
                Mob s = e.getKey();
                if (!s.isAlive()) {
                    it.remove();
                } else if (now - e.getValue() >= SlayerBosses.SIRE_SPAWN_MATURES) {
                    it.remove();
                    Vec3 at = s.position();
                    s.discard();
                    Mob scion = BossFx.spawnMinion(boss, "abyssal_scion", at, target);
                    if (scion != null) {
                        scions.add(scion);
                    }
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_SIRE_SCION);
                }
            }
        }

        /** Respiratory systems that just died give their 50 Slayer XP. */
        private void tickSystems(ServerPlayer target) {
            for (java.util.Iterator<Mob> it = systems.iterator(); it.hasNext(); ) {
                Mob m = it.next();
                if (!m.isAlive()) {
                    it.remove();
                    if (m.isDeadOrDying() && target != null) {
                        com.runecraft.skill.XpService.add(target, Skill.SLAYER, SlayerBosses.SIRE_SYSTEM_SLAYER_XP);
                    }
                }
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            tickTentacles(boss);
            tickSpawns(boss, target);
            if (stage == 1) {
                tickSystems(target);
                if (disoriented > 0) {
                    disoriented--;
                    BossFx.burst(boss, ParticleTypes.ENCHANTED_HIT, 4, 1.0);
                    if (disoriented == 0) {
                        BossFx.say(boss, "The Abyssal Sire regains its senses.");
                    }
                } else {
                    BossFx.burst(boss, ParticleTypes.SQUID_INK, 3, 1.0);
                }
                if (systems.isEmpty()) {
                    stage = 2;
                    disoriented = 0;
                    boss.setHealth(boss.getMaxHealth()); // phase 1 damage only ever counted towards disorienting it
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
                        spawn(boss, target, around(boss, boss.position(), 3));
                    }
                }
                return;
            }
            if (target == null) {
                return;
            }
            if (stage == 2 && (clamped || boss.getHealth() < SlayerBosses.SIRE_PHASE3_BELOW)) {
                stage = 3;
                if (clamped) {
                    clamped = false;
                    boss.setHealth(Math.max(boss.getHealth(), (float) (boss.getMaxHealth() * SlayerBosses.SIRE_PHASE3_FLOOR)));
                }
                BossFx.say(boss, "The Abyssal Sire retreats to the middle of the chamber and latches onto the floor! Its tentacles thrash again!");
                boss.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
                for (int i = 0; i < 4; i++) {
                    spawn(boss, target, around(boss, homeVec(), 3));
                }
                spawnTimer = 12;
            } else if (stage == 3 && (clamped || boss.getHealth() < SlayerBosses.SIRE_EXPLODE_BELOW)) {
                stage = 4;
                clamped = false;
                pullIn(boss, target);
                perform(boss, target, SlayerBosses.SIRE_EXPLOSION);
                cooldown = Math.max(cooldown, SlayerBosses.SIRE_EXPLOSION.telegraphTicks() + 1);
                for (int i = 0; i < 6; i++) {
                    spawn(boss, target, around(boss, boss.position(), 4));
                }
                spawnTimer = 4;
            }
            if (--spawnTimer <= 0) {
                if (stage == 4) {
                    spawnTimer = 4; // spawns pour out of its portal until 15 minions are alive
                    if (minions() < SlayerBosses.SIRE_MAX_MINIONS) {
                        spawn(boss, target, around(boss, boss.position(), 2));
                    }
                } else {
                    spawnTimer = stage == 3 ? 12 : 15;
                    spawn(boss, target, around(boss, boss.position(), 3));
                }
            }
        }

        /** Its spawns, scions and respiratory systems all die with it (and go when it resets). */
        private void clearMinions() {
            systems.forEach(Mob::discard);
            spawns.keySet().forEach(Mob::discard);
            scions.forEach(Mob::discard);
            spawns.clear();
            scions.clear();
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            clearMinions();
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            clearMinions();
        }
    }

    /**
     * Alchemical Hydra (OSRS Wiki "Alchemical Hydra/Strategies"). Each colour phase is a separate form (one head is lost per
     * phase, at 75/50/25%). Its chemical shield cuts damage by 75% until it is lured over the vent whose colour beats its
     * carapace while the vents erupt (green to the red vent in the south-east, blue to the green vent in the north-east, red to
     * the blue vent in the north-west); a wrong vent empowers it (26x2 serpentine and electric, 52 flame). It switches between
     * ranged and magic after every three basic attacks, carried across the phases; each phase opens with three basic attacks
     * and then its special (poison blobs, lightning currents from the corners converging on the player, a stun and walls of
     * fire with a tracking flame), then a special after every nine. The extinguished phase has no shield, attacks every four
     * ticks for up to 55, alternates styles every attack starting with the one it didn't use last, and spits poison on its
     * fourth attack and every ninth after.
     */
    static final class Hydra extends ScriptedBoss {
        /** A lightning current or the tracking flame, chasing the player one block a tick until it strikes or fades. */
        private static final class Chaser {
            Vec3 pos;
            final long until;
            final boolean fire;

            Chaser(Vec3 pos, long until, boolean fire) {
                this.pos = pos;
                this.until = until;
                this.fire = fire;
            }
        }

        private final HydraPhase phase;
        private boolean shielded;
        private boolean empowered;
        private int ventTimer = 8;
        private int styleCount;
        private boolean magicStyle;
        private int autos;
        private int specials;
        private boolean restored;
        private final List<Chaser> chasers = new ArrayList<>();

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
                case "red" -> c.add(7, 0, 6); // south-east (Minecraft +x east, +z south)
                case "green" -> c.add(7, 0, -6); // north-east
                default -> c.add(-7, 0, -6); // blue: north-west
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

        /** Max hits by phase ({@link SlayerBosses#hydraMaxHit}): 17 (x2), 35, 35 and 55 once extinguished; more when empowered. */
        @Override
        protected double multiplier(DataBossEntity boss) {
            return super.multiplier(boss) * SlayerBosses.hydraMaxHit(phase, empowered) / (double) SlayerBosses.HYDRA_RANGED.maxHit();
        }

        @Override
        protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
            super.perform(boss, target, a);
            if (SlayerBosses.hydraHitsTwice(phase, empowered) && (a == SlayerBosses.HYDRA_RANGED || a == SlayerBosses.HYDRA_MAGIC)) {
                super.perform(boss, target, a); // "17x2", "26x2 (empowered)"
            }
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (SlayerBosses.hydraSpecialDue(autos, specials)) {
                autos = 0;
                specials++;
                attackCount++;
                special(boss, target);
                return null;
            }
            if (phase == HydraPhase.ENRAGED) {
                magicStyle = !magicStyle; // every attack, starting with the style it didn't use last
            } else {
                if (styleCount >= SlayerBosses.HYDRA_STYLE_EVERY) {
                    styleCount = 0;
                    magicStyle = !magicStyle;
                    BossFx.warn(boss, "The Alchemical Hydra sways its heads back... it switches to " + (magicStyle ? "magic" : "ranged") + "!");
                }
                styleCount++;
            }
            autos++;
            return magicStyle ? SlayerBosses.HYDRA_MAGIC : SlayerBosses.HYDRA_RANGED;
        }

        private void special(DataBossEntity boss, ServerPlayer target) {
            cooldown = attackDelay(boss, SlayerBosses.HYDRA_MAGIC) - 1;
            Vec3 p = target.position();
            switch (phase) {
                case GREEN, ENRAGED -> {
                    BossFx.warn(boss, "The Alchemical Hydra spits poison!");
                    for (int i = 0; i < 5; i++) {
                        boolean aimed = i == 0;
                        Vec3 at = aimed ? p : around(boss, p, 3);
                        namedStrike("poison_pool", boss, at, 2, SlayerBosses.HYDRA_SPLATTER_MAX, 1.5, ParticleTypes.SNEEZE, (hit, pos) -> {
                            for (ServerPlayer h : hit) {
                                com.runecraft.combat.PoisonService.poison(h, aimed ? 6 : 4, now);
                            }
                            com.runecraft.boss.Mechanics.addHazard(BossFx.world(boss), pos, SlayerBosses.HYDRA_POOL_MAX, 10, boss);
                        });
                    }
                }
                case BLUE -> {
                    BossFx.warn(boss, "The Alchemical Hydra calls down lightning - the currents converge on you!");
                    Vec3 c = homeVec();
                    for (int[] corner : new int[][]{{-9, -9}, {9, -9}, {-9, 9}, {9, 9}}) {
                        chasers.add(new Chaser(BossFx.ground(BossFx.world(boss), c.x + corner[0], c.z + corner[1], c.y), now + 24, false));
                    }
                }
                case RED -> fire(boss, target);
            }
        }

        /**
         * The flame phase's special: the Hydra stuns the player, breathes 5x5 walls of fire to either side of them, then
         * sends a tracking flame after them, and doesn't attack for about seven ticks.
         */
        private void fire(DataBossEntity boss, ServerPlayer target) {
            BossFx.warn(boss, "The Alchemical Hydra breathes walls of fire - walk away from the tracking flame!");
            CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_HYDRA_FLAME_WALL);
            BossFx.stun(target, 2);
            Vec3 p = target.position();
            Vec3 dir = p.subtract(boss.position()).multiply(1, 0, 1).normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            for (int s : new int[]{-1, 1}) {
                for (int i = -2; i <= 2; i++) {
                    for (int j = 2; j <= 4; j++) { // approx: 5 long and 3 wide (the wiki's 5x5 layers)
                        Vec3 at = p.add(dir.scale(i)).add(side.scale(s * j));
                        namedStrike("fire_wall", boss, BossFx.ground(BossFx.world(boss), at.x, at.z, p.y), 3,
                                SlayerBosses.HYDRA_FIRE.maxHit(), 0.8, ParticleTypes.FLAME, (hit, pos) -> hit.forEach(h -> burn(boss, h)));
                    }
                }
            }
            chasers.add(new Chaser(boss.position(), now + 16, true));
            cooldown += SlayerBosses.HYDRA_FIRE_PAUSE;
        }

        /** "A subsequent burn of 5 damage for 5 hits": five stacks of a five-hit burn. */
        private static void burn(DataBossEntity boss, ServerPlayer p) {
            for (int i = 0; i < 5; i++) {
                BossDots.burn(boss, p, 5);
            }
        }

        private void tickChasers(DataBossEntity boss, ServerPlayer target) {
            if (chasers.isEmpty()) {
                return;
            }
            java.util.Iterator<Chaser> it = chasers.iterator();
            while (it.hasNext()) {
                Chaser ch = it.next();
                if (now > ch.until || target == null) {
                    it.remove();
                    continue;
                }
                Vec3 to = target.position().subtract(ch.pos).multiply(1, 0, 1);
                // the tracking flame speeds up the further away the player is (OSRS: it travels faster when you run)
                double step = ch.fire ? Math.min(2.0, 0.8 + to.length() / 8) : 1.0;
                ch.pos = to.length() <= step ? new Vec3(target.getX(), ch.pos.y, target.getZ()) : ch.pos.add(to.normalize().scale(step));
                BossFx.world(boss).sendParticles(ch.fire ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK, ch.pos.x, ch.pos.y + 0.3,
                        ch.pos.z, 6, 0.2, 0.2, 0.2, 0.01);
                if (target.position().distanceToSqr(ch.pos.x, target.getY(), ch.pos.z) <= 0.8) {
                    com.runecraft.combat.CombatHooks.nameAttack(ch.fire ? "fire_wall" : "lightning", null);
                    int max = ch.fire ? SlayerBosses.HYDRA_FIRE.maxHit() : SlayerBosses.HYDRA_LIGHTNING.maxHit();
                    CombatHandler.applyDamage(boss, target, target.getRandom().nextInt(max + 1));
                    if (ch.fire) {
                        burn(boss, target);
                    } else {
                        BossFx.stun(target, 3); // bound
                    }
                    it.remove();
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
                    if (p.length >= 6) {
                        magicStyle = "magic".equals(p[4]); // the style rotation carries on from the last phase
                        styleCount = Integer.parseInt(p[5]);
                    }
                } else {
                    placeVents(boss);
                    magicStyle = boss.getRandom().nextBoolean(); // "will begin the fight with either combat style"
                }
                if (phase == HydraPhase.ENRAGED) {
                    BossFx.say(boss, "The Alchemical Hydra becomes enraged!");
                } else if (phase != HydraPhase.GREEN) {
                    BossFx.say(boss, "The Alchemical Hydra's chemical shield reforms as it turns " + phase.colour + "!");
                }
            }
            tickChasers(boss, target);
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
                                BossFx.say(boss, "Roaaaaaaaaaaar! The chemicals neutralise the Alchemical Hydra's defences!");
                            }
                        } else if (!empowered) {
                            // A wrong vent doesn't restore the shield: it empowers the Hydra's attacks (OSRS Wiki).
                            empowered = true;
                            BossFx.say(boss, "The Alchemical Hydra absorbs the wrong chemicals and grows more powerful!");
                            CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_HYDRA_EMPOWERED);
                        }
                    }
                }
            }
            if (phase != HydraPhase.ENRAGED && percent(boss) <= phase.endsAtPercent) {
                HydraPhase next = phase.next();
                chasers.clear(); // currents dissipate at a phase change
                BossFx.swapForm(boss, next.bossId, boss.blockPosition(), false,
                        "hydra;" + home.getX() + ";" + home.getY() + ";" + home.getZ() + ";" + (magicStyle ? "magic" : "ranged") + ";"
                                + styleCount);
            }
        }
    }

    /**
     * The Grotesque Guardians (OSRS Wiki, Grotesque Guardians/Strategies). Phase 1: Dusk is immune and Dawn is fought
     * until 55%, when she flies up. Phase 2: Dusk is attackable; after he attacks he charges his blinding attack (step
     * away or be knocked back for the high 20s) while Dawn throws rockfalls. Phase 3 (Dusk at 55%): lightning across
     * the rooftop, then Dawn is attackable again and Dusk immune; her energy spheres heal her 90 each unless absorbed.
     * Phase 4 (Dawn dead): Dusk absorbs her essence and immediately uses his prison attack (immune until it ends):
     * escape the cell within 5 ticks or take up to 65, which heals him; he repeats it. His claws hit up to 15 in phases 1-3
     * and 26 in phase 4, where he also fires two orbs of up to 15 (1/6 chance to switch style, 1/3 if prayed against), and
     * he tramples anyone standing under him (phases 2 and 4) for up to 40.
     */
    static final class Dusk extends ScriptedBoss {
        private DataBossEntity dawn;
        private int special = 15;
        /** 1-4 as above. */
        int stage = 1;
        private boolean firstPrison;
        private boolean blindNext;
        /** Phase 4: his current style (orbs instead of claws) and the tick he last trampled someone. */
        private boolean ranged;
        private long trampledAt = -10;

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
            return stage < 4 || !ranged;
        }

        /** Claws up to 15 in the first three phases, 26 once he has absorbed Dawn (OSRS Wiki). */
        @Override
        protected double multiplier(DataBossEntity boss) {
            return stage < 4 && last == SlayerBosses.DUSK_MELEE ? SlayerBosses.DUSK_MELEE_EARLY / (double) SlayerBosses.DUSK_MELEE.maxHit()
                    : super.multiplier(boss);
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (stage == 4 && attackCount > 0) {
                // Phase 4 (OSRS Wiki): a 1/6 chance to switch style each attack, 1/3 while the player prays against it.
                boolean prayed = BossFx.protects(target, ranged ? com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES
                        : com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE);
                if (boss.getRandom().nextInt(prayed ? 3 : 6) == 0) {
                    ranged = !ranged;
                }
            }
            if (stage == 4 && ranged) {
                return SlayerBosses.DUSK_RANGED;
            }
            last = SlayerBosses.DUSK_MELEE; // for multiplier()
            return SlayerBosses.DUSK_MELEE;
        }

        @Override
        protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
            super.perform(boss, target, a);
            if (a == SlayerBosses.DUSK_RANGED) {
                super.perform(boss, target, a); // two orbs
            }
        }

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
            if ((stage == 2 || stage == 4) && target != null && now - trampledAt >= 4 && target.getBoundingBox().intersects(boss.getBoundingBox())) {
                // Standing under Dusk in phases 2 and 4: trampled for up to 40 (OSRS Wiki).
                trampledAt = now;
                com.runecraft.combat.CombatHooks.nameAttack("trample", null);
                CombatHandler.applyDamage(boss, target, boss.getRandom().nextInt(SlayerBosses.DUSK_TRAMPLE_MAX + 1));
            }
            if (stage < 4 && !dawnAlive()) {
                stage = 4;
                special = 1;
                firstPrison = true;
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
                    prison(boss, target); // approx: how often he repeats it (was alternated with an invented explosion)
                    special = 24;
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
        protected boolean driven() {
            return true;
        }

        /**
         * Two rocks of up to 9 (ranged); a kick for up to 15 when next to the player; now and then a stone ball (3x3, up to
         * 15 and encased in stone for 10 ticks). approx: how often she kicks or throws the stone ball.
         */
        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (distance <= SlayerBosses.DAWN_KICK.range() && boss.getRandom().nextInt(3) == 0) {
                return SlayerBosses.DAWN_KICK;
            }
            if (boss.getRandom().nextInt(6) == 0) {
                last = SlayerBosses.DAWN_STONE_BALL;
                attackCount++;
                BossFx.warn(boss, "Dawn launches a ball of stone!");
                namedStrike("stone_ball", boss, target.position(), SlayerBosses.DAWN_STONE_BALL.telegraphTicks(),
                        maxHit(boss, SlayerBosses.DAWN_STONE_BALL), 1.5, ParticleTypes.ASH, (hit, pos) -> {
                            for (ServerPlayer h : hit) {
                                BossFx.stun(h, SlayerBosses.DAWN_STONE_STUN_TICKS);
                                Msg.red(h, "You are encased in stone!");
                            }
                        });
                cooldown = Math.max(1, attackDelay(boss, SlayerBosses.DAWN_STONE_BALL)) - 1;
                return null;
            }
            return SlayerBosses.DAWN_RANGED;
        }

        @Override
        protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
            super.perform(boss, target, a);
            if (a == SlayerBosses.DAWN_RANGED) {
                super.perform(boss, target, a); // two separate hits
            }
        }

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
