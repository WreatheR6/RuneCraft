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
            String head = Inv.idOf(p.getItemBySlot(EquipmentSlot.HEAD));
            return MASKS.contains(head) || com.runecraft.osrs.content.slayer.SlayerGear.SLAYER_HELMETS.contains(head);
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
                if (styleCount >= SlayerBosses.HYDRA_STYLE_EVERY) {
                    // The last attack of this style: the switch is announced one attack early. // playability: OSRS players
                    // count the attacks and switch as the next one leaves; the cycle length stays
                    BossFx.warn(boss, "The Alchemical Hydra will switch to " + (magicStyle ? "ranged" : "magic") + " after this attack!");
                }
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
            // The vents spray in every phase: once extinguished (grey) the Hydra has no shield left to lose and every vent
            // empowers it ("Mixing Correctly": the grey phase "must never be touched by any of the three vents").
            if (--ventTimer <= 0) {
                ventTimer = 8;
                for (String c : new String[]{"red", "green", "blue"}) {
                    Vec3 v = vent(c);
                    BossFx.column(BossFx.world(boss), new Vec3(v.x, home.getY(), v.z), ventParticle(c), 8);
                    double dx = boss.getX() - v.x;
                    double dz = boss.getZ() - v.z;
                    if (dx * dx + dz * dz < 6.25) {
                        if (SlayerBosses.hydraVentWeakens(phase, c)) {
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

        /** Which of Dusk's two orbs is being thrown (1 or 2), for its damage rules. */
        private int orb;
        private boolean p4Opened;

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (stage == 4 && !p4Opened) {
                // His first attack of phase 4: twice as likely to be the style the player isn't praying against (Mod Ash).
                p4Opened = true;
                boolean meleePrayer = BossFx.protects(target, com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE);
                boolean rangedPrayer = BossFx.protects(target, com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES);
                boolean pick = boss.getRandom().nextInt(3) < 2;
                ranged = meleePrayer ? pick : rangedPrayer ? !pick : boss.getRandom().nextBoolean();
            } else if (stage == 4 && attackCount > 0) {
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

        /** Orbs of the last pair that have landed: they fly to the player now (round 41), so landing order tells them apart. */
        private int orbsLanded;

        @Override
        protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
            orb = a == SlayerBosses.DUSK_RANGED ? 1 : 0;
            orbsLanded = 0;
            super.perform(boss, target, a);
            if (a == SlayerBosses.DUSK_RANGED) {
                orb = 2;
                super.perform(boss, target, a); // two orbs
                orb = 0;
            }
        }

        /**
         * Dusk's orbs (Mod Ash): the first deals 60% of his max hit, unrolled, when it hits; the second rolls, then rolls again
         * between 0 and that; praying halves both. His claws go 80% through Protect from Melee.
         */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            int orb = this.orb;
            if (orb == 0 && com.runecraft.boss.Mechanics.resolvingAttack() == SlayerBosses.DUSK_RANGED) {
                orb = Math.min(2, ++orbsLanded); // an orb that flew: the first of the pair to land, then the second
            }
            if (orb == 1 && damage > 0) {
                damage = (int) Math.round(maxHit(boss, SlayerBosses.DUSK_RANGED) * SlayerBosses.DUSK_FIRST_ORB);
            } else if (orb == 2 && damage > 0) {
                damage = boss.getRandom().nextInt(damage + 1);
            }
            AttackDef a = orb > 0 ? SlayerBosses.DUSK_RANGED : last;
            if (a == SlayerBosses.DUSK_RANGED && BossFx.protects(player, com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES)
                    || a == SlayerBosses.DUSK_MELEE && BossFx.protects(player, com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE)) {
                damage = SlayerBosses.guardianThroughPrayer(a, damage);
            }
            return damage;
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
            BossFx.warn(boss, "Shadows loom overhead - rocks fall from above!");
            for (int i = 0; i < SlayerBosses.GUARDIAN_ROCKFALL_PIECES; i++) {
                // 3x3 each, at four different speeds; the edges hit for up to 22, a direct hit also knocks the player out.
                Vec3 at = i == 0 ? target.position() : around(boss, target.position(), 5);
                namedStrike("falling_rocks", boss, at, 3 + i % 4, SlayerBosses.GUARDIAN_ROCKFALL.maxHit(), 1.5, ParticleTypes.ASH,
                        (hit, pos) -> {
                            for (ServerPlayer h : hit) {
                                if (h.position().distanceToSqr(pos.x, h.getY(), pos.z) <= 0.36) {
                                    BossFx.stun(h, SlayerBosses.GUARDIAN_ROCKFALL_STUN);
                                    Msg.red(h, "You're knocked out by the falling rocks!");
                                }
                            }
                        });
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

        /** She flies: only a halberd's reach gets a melee attack to her (OSRS Wiki). */
        static boolean reaches(ServerPlayer p, AttackType type) {
            return !type.isMelee() || Inv.idOf(p.getMainHandItem()).contains("halberd");
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (attacker != null && !reaches(attacker, type)) {
                Msg.action(attacker, "Dawn flies out of reach of your weapon. (Use ranged, magic or a halberd.)");
                return 0;
            }
            return damage;
        }

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

        /** Prayer only partly blocks her: the kick gets 2 through Protect from Melee, the rocks part of their damage. */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (last == SlayerBosses.DAWN_KICK && BossFx.protects(player, com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE)
                    || last == SlayerBosses.DAWN_RANGED && BossFx.protects(player, com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES)) {
                return SlayerBosses.guardianThroughPrayer(last, damage);
            }
            return damage;
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

    /**
     * Self-test (boss:sire_spawns): a spawn left 15 seconds matures into a scion, and every spawn and scion dies with the Sire.
     */
    static String selfTestSire(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Sire s)) {
            throw new IllegalStateException("the Abyssal Sire has no Sire script");
        }
        long t0 = com.runecraft.action.ActionManager.gameTick();
        s.gameTick(boss, t0);
        s.spawn(boss, p, boss.position().add(2.5, 0, 0));
        s.spawn(boss, p, boss.position().add(-2.5, 0, 0));
        if (s.minions() != 2 || !s.scions.isEmpty()) {
            throw new IllegalStateException(s.minions() + " minions after two spawns");
        }
        s.gameTick(boss, t0 + SlayerBosses.SIRE_SPAWN_MATURES - 1);
        if (!s.scions.isEmpty()) {
            throw new IllegalStateException("a spawn matured before 15 seconds");
        }
        s.gameTick(boss, t0 + SlayerBosses.SIRE_SPAWN_MATURES);
        if (s.scions.size() != 2 || !s.spawns.isEmpty()) {
            throw new IllegalStateException("after 15 s: " + s.spawns.size() + " spawns, " + s.scions.size() + " scions");
        }
        List<Mob> all = new ArrayList<>(s.scions);
        s.onDeath(boss, p);
        if (all.stream().anyMatch(Mob::isAlive)) {
            throw new IllegalStateException("scions outlived the Sire");
        }
        return "2 spawns matured into scions after 15 s and died with the Sire";
    }

    /**
     * Self-test (boss:kraken_whirlpools): the whirlpools stay still until disturbed, the large one can only be disturbed after
     * the four small ones, the Kraken then rises, and ranged hits deal a seventh.
     */
    static String selfTestKraken(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Kraken k)) {
            throw new IllegalStateException("the Kraken has no Kraken script");
        }
        if (k.tentacles.size() != 4 || k.tentacles.stream().anyMatch(t -> !t.isNoAi())) {
            throw new IllegalStateException(k.tentacles.size() + " whirlpools, some already awake");
        }
        if (k.isVulnerable(boss)) {
            throw new IllegalStateException("the large whirlpool could be disturbed before the small ones");
        }
        k.selfTestDisturb(p);
        k.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
        if (k.tentacles.stream().anyMatch(Mob::isNoAi) || !k.isVulnerable(boss)) {
            throw new IllegalStateException("disturbed whirlpools didn't wake their tentacles");
        }
        int first = k.incoming(boss, p, 50, AttackType.MAGIC);
        int ranged = k.incoming(boss, p, 50, AttackType.RANGED);
        if (first != 0 || !k.risen || ranged != 7) {
            throw new IllegalStateException("disturbing hit dealt " + first + ", risen " + k.risen + ", ranged 50 -> " + ranged);
        }
        return "4 whirlpools disturbed, the Kraken rose, ranged 50 -> 7";
    }

    /**
     * Self-test (boss:araxxor_eggs): nine eggs ring the lair, the third standard attack hatches the first, a destroyed egg is
     * dropped from the rotation, the sixth standard attack brings the special of the south-easternmost egg, and a mirrorback
     * araxyte takes a fifth of the damage dealt to Araxxor.
     */
    static String selfTestAraxxor(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Araxxor a)) {
            throw new IllegalStateException("Araxxor has no Araxxor script");
        }
        try {
            a.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
            if (a.eggsLeft() != BossMechanics.ARAXXOR_EGGS) {
                throw new IllegalStateException(a.eggsLeft() + " eggs placed, expected 9");
            }
            for (int i = 0; i < BossMechanics.ARAXXOR_FIRST_HATCH; i++) {
                a.standard(boss, p);
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
            for (int i = BossMechanics.ARAXXOR_FIRST_HATCH; i < SlayerBosses.ARAXXOR_SPECIAL_EVERY; i++) {
                a.standard(boss, p);
            }
            if (!a.specialNext) {
                throw new IllegalStateException("no special after six standard attacks");
            }
            String special = SlayerBosses.araxxorSpecial(a.eggKinds.get(0));
            String mirror = "";
            // The egg pattern is random: when it starts with a white egg the first hatchling already is a mirrorback, and the
            // 20% goes to that one (OSRS: the living mirrorback takes the redirected damage), so it is the one checked; a
            // second mirrorback beside it would leave the check guessing which one absorbed the hit.
            Mob m = a.kinds.entrySet().stream()
                    .filter(e -> "mirrorback_araxyte".equals(e.getValue()) && e.getKey().isAlive())
                    .map(java.util.Map.Entry::getKey).findFirst().orElse(null);
            String which = "hatched";
            if (m == null) {
                which = "spawned";
                m = BossFx.spawnMinion(boss, "mirrorback_araxyte", ScriptedBoss.around(boss, boss.position(), 3), null);
                if (m != null) {
                    a.araxytes.add(m);
                    a.kinds.put(m, "mirrorback_araxyte");
                }
            }
            if (m != null) {
                float before = m.getHealth();
                int dealt = a.incoming(boss, null, 50, AttackType.SLASH);
                float taken = before - m.getHealth();
                if (dealt != 40 || taken <= 0) {
                    throw new IllegalStateException("a " + which + " mirrorback took " + taken + " and Araxxor " + dealt + " of 50");
                }
                mirror = ", " + which + " mirrorback took " + Math.round(taken) + " of 50";
            }
            return "9 eggs, first hatched into " + a.eggKinds.get(0) + ", destroyed egg skipped, special " + special + mirror;
        } finally {
            a.onDeath(boss, p);
        }
    }

    /**
     * Araxxor (OSRS Wiki "Araxxor" and its Strategies). It walks up to the player and bites (up to 38, 5 through prayer); out of
     * reach it uses magic or ranged, whichever the player's defence roll is lower against (prayer takes 50% / 45% off; a
     * ranged hit drains 9% Defence, a magic one 9% Prayer). Nine eggs ring the lair, three of each colour in one of the three
     * wiki patterns; the first hatches after three standard attacks, clockwise from the south-easternmost, then one every six.
     * Players may destroy eggs (65 hitpoints; a destroyed egg is skipped); damage on an egg carries over to its araxyte, and an
     * egg that took more than the araxyte's 58 hitpoints just crumbles when its turn comes. Every six standard attacks comes
     * the special of the south-easternmost egg's kind: an acid ball (acidic), an acid splatter (mirrorback) or an acid drip
     * (ruptura), all leaving acid pools. Mirrorback araxytes take a fifth of the damage dealt to Araxxor and recoil half of it;
     * acidic ones splatter acid as they die; rupturas walk up and explode. Under 255 hitpoints it enrages: every 4 ticks, +35
     * Defence and +28 Magic, and its melee becomes a cleave over three tiles ("Skree!"). The kill's loot waits on the corpse:
     * harvest or destroy it ({@link AraxxorCorpse}).
     */
    static final class Araxxor extends ScriptedBoss {
        private final List<Mob> araxytes = new ArrayList<>();
        /** The kind of each hatched araxyte. */
        private final java.util.Map<Mob, String> kinds = new java.util.HashMap<>();
        /** The hatched ruptura araxytes (they explode instead of attacking). */
        private final List<Mob> rupturas = new ArrayList<>();
        private final List<Mob> eggs = new ArrayList<>();
        private List<String> eggKinds = List.of();
        private int nextEgg;
        private int standardAttacks;
        private boolean specialNext;
        private int dripTicks;
        /** The game tick of the first player hit on Araxxor (-1: not fought yet), timing the coagulated venom kill. */
        private long firstHit = -1;

        private boolean enraged(DataBossEntity boss) {
            return boss.phase() >= 0;
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
            return true; // it closes the distance, using magic or ranged until it is in reach
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            return enraged(boss) ? SlayerBosses.ARAXXOR_ENRAGED_SPEED : super.attackDelay(boss, attack);
        }

        @Override
        public void adjustDefence(DataBossEntity boss, ServerPlayer attacker, AttackType type, com.runecraft.osrs.PlayerHitCalc.Input in) {
            if (enraged(boss)) {
                in.targetDefenceLevel += SlayerBosses.ARAXXOR_ENRAGE_DEFENCE;
                in.targetMagicLevel += SlayerBosses.ARAXXOR_ENRAGE_MAGIC;
            }
        }

        private void placeEggs(DataBossEntity boss) {
            eggKinds = BossMechanics.araxxorEggs(boss.getRandom().nextInt(3));
            Vec3 c = homeVec();
            for (int i = 0; i < eggKinds.size(); i++) {
                // Clockwise from the south-east (Minecraft +x east, +z south: increasing angle turns clockwise from above).
                double a = Math.PI / 4 + i * 2 * Math.PI / eggKinds.size();
                eggs.add(BossFx.spawnMinion(boss, BossMechanics.ARAXYTE_EGG,
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 9, c.z + Math.sin(a) * 9, c.y), null));
            }
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (specialNext && !eggKinds.isEmpty()) {
                specialNext = false;
                attackCount++;
                cooldown = attackDelay(boss, null) - 1;
                special(boss, target, SlayerBosses.araxxorSpecial(eggKinds.get(0)));
                return null;
            }
            if (distance <= SlayerBosses.ARAXXOR_MELEE.range()) {
                if (enraged(boss)) {
                    attackCount++;
                    cooldown = attackDelay(boss, null) - 1;
                    cleave(boss, target);
                    standard(boss, target);
                    return null;
                }
                standard(boss, target);
                return SlayerBosses.ARAXXOR_MELEE;
            }
            if (distance > SlayerBosses.ARAXXOR_RANGED.range()) {
                return null; // still walking up
            }
            standard(boss, target);
            // whichever of the player's defence rolls is lower
            long magic = CombatHandler.defenceRoll(target, AttackType.MAGIC);
            long ranged = CombatHandler.defenceRoll(target, AttackType.RANGED);
            return magic <= ranged ? SlayerBosses.ARAXXOR_MAGIC : SlayerBosses.ARAXXOR_RANGED;
        }

        /** One standard attack: the 3rd, 9th, 15th... hatch the next egg (until it enrages); every 6th brings the special. */
        void standard(DataBossEntity boss, ServerPlayer target) {
            standardAttacks++;
            if (!enraged(boss) && BossMechanics.araxxorHatches(standardAttacks)) {
                hatch(boss, target);
            }
            if (SlayerBosses.araxxorSpecialNext(standardAttacks)) {
                specialNext = true;
            }
        }

        /** Prayer only cuts its attacks; ranged and magic hits drain Defence and Prayer; any hit can envenom. */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            AttackDef a = last;
            if (a != SlayerBosses.ARAXXOR_MELEE && a != SlayerBosses.ARAXXOR_RANGED && a != SlayerBosses.ARAXXOR_MAGIC) {
                return damage;
            }
            com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(player);
            if (damage > 0) {
                if (a == SlayerBosses.ARAXXOR_RANGED) {
                    int drain = (int) Math.ceil(data.skills().currentLevel(Skill.DEFENCE) * SlayerBosses.ARAXXOR_DRAIN);
                    data.skills().setBoost(Skill.DEFENCE, data.skills().boost(Skill.DEFENCE) - drain);
                    com.runecraft.network.ServerNetworking.sync(player);
                } else if (a == SlayerBosses.ARAXXOR_MAGIC) {
                    BossFx.drainPrayer(player, (int) Math.ceil(data.skills().level(Skill.PRAYER) * SlayerBosses.ARAXXOR_DRAIN));
                }
                if (boss.getRandom().nextInt(4) == 0) {
                    com.runecraft.combat.PoisonService.venom(player, now); // approx: how often its hits envenom
                }
            }
            Prayer prayer = a == SlayerBosses.ARAXXOR_MELEE ? Prayer.PROTECT_FROM_MELEE
                    : a == SlayerBosses.ARAXXOR_RANGED ? Prayer.PROTECT_FROM_MISSILES : Prayer.PROTECT_FROM_MAGIC;
            return BossFx.protects(player, prayer) ? SlayerBosses.araxxorThroughPrayer(a, damage) : damage;
        }

        /** An acid pool (venom, 4 a tick) on a tile. */
        private void pool(DataBossEntity boss, Vec3 at) {
            com.runecraft.boss.Mechanics.addHazard(BossFx.world(boss), at, SlayerBosses.ARAXXOR_POOL_DAMAGE, 15, boss);
        }

        private void venomed(List<ServerPlayer> hit) {
            for (ServerPlayer h : hit) {
                com.runecraft.combat.PoisonService.venom(h, now);
            }
        }

        private void special(DataBossEntity boss, ServerPlayer target, String kind) {
            Vec3 from = boss.position();
            Vec3 p = target.position();
            switch (kind) {
                case "acid_ball" -> {
                    // A ball flies in a straight line at the player (3x3 along its path), leaving pools, and splatters at the end.
                    BossFx.warn(boss, "Araxxor spits a ball of acid - get out of its path!");
                    Vec3 dir = p.subtract(from).multiply(1, 0, 1);
                    dir = dir.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : dir.normalize();
                    int length = 12;
                    for (int i = 2; i <= length; i++) {
                        Vec3 at = from.add(dir.scale(i));
                        namedStrike("acid_ball", boss, BossFx.ground(BossFx.world(boss), at.x, at.z, from.y), 1 + i / 3,
                                SlayerBosses.ARAXXOR_ACID_BALL_MAX, 1.5, ParticleTypes.SNEEZE, (hit, pos) -> {
                                    venomed(hit);
                                    pool(boss, pos);
                                });
                    }
                    Vec3 end = from.add(dir.scale(length));
                    for (int i = 0; i < 6; i++) {
                        namedStrike("acid_ball", boss, around(boss, end, 3.5), 2 + length / 3, SlayerBosses.ARAXXOR_SPLATTER_MAX, 1.0,
                                ParticleTypes.SNEEZE, (hit, pos) -> {
                                    venomed(hit);
                                    pool(boss, pos);
                                });
                    }
                }
                case "acid_splatter" -> {
                    BossFx.warn(boss, "Araxxor sprays venom all around you!");
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8;
                        Vec3 at = BossFx.ground(BossFx.world(boss), p.x + Math.cos(a) * 2.2, p.z + Math.sin(a) * 2.2, p.y);
                        namedStrike("acid_ball", boss, at, 2, SlayerBosses.ARAXXOR_SPLATTER_MAX, 1.2, ParticleTypes.SNEEZE, (hit, pos) -> {
                            venomed(hit);
                            pool(boss, pos);
                        });
                    }
                }
                default -> {
                    // Acid drip: a homing blob; the player then drips venom for 6 ticks, leaving a pool wherever they stand.
                    BossFx.warn(boss, "Araxxor's venom drips onto you - keep moving!");
                    com.runecraft.combat.CombatHooks.nameAttack("acid_ball", null);
                    CombatHandler.applyDamage(boss, target, boss.getRandom().nextInt(SlayerBosses.ARAXXOR_DRIP_MAX + 1));
                    com.runecraft.combat.PoisonService.venom(target, now);
                    dripTicks = SlayerBosses.ARAXXOR_DRIP_TICKS;
                }
            }
        }

        /** Enraged melee: a cleave over the player's tile and the two beside it, 2 ticks after "Skree!", leaving acid. */
        private void cleave(DataBossEntity boss, ServerPlayer target) {
            BossFx.say(boss, "Araxxor: Skree!");
            Vec3 p = target.position();
            Vec3 dir = p.subtract(boss.position()).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : dir.normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            for (int k = -1; k <= 1; k++) {
                Vec3 at = BossFx.ground(BossFx.world(boss), p.x + side.x * k, p.z + side.z * k, p.y);
                namedStrike("cleave", boss, at, 2, 0, 0.8, ParticleTypes.SWEEP_ATTACK, (hit, pos) -> {
                    for (ServerPlayer h : hit) {
                        int dmg = h.getRandom().nextInt(SlayerBosses.ARAXXOR_CLEAVE.maxHit() + 1);
                        if (BossFx.protects(h, Prayer.PROTECT_FROM_MELEE)) {
                            dmg = SlayerBosses.araxxorThroughPrayer(SlayerBosses.ARAXXOR_CLEAVE, dmg);
                        }
                        com.runecraft.combat.CombatHooks.nameAttack("cleave", AttackType.SLASH);
                        CombatHandler.applyDamage(boss, h, dmg);
                    }
                    pool(boss, pos);
                });
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (eggs.isEmpty() && target != null) {
                placeEggs(boss);
            }
            if (dripTicks > 0 && target != null) {
                dripTicks--;
                pool(boss, target.position());
            }
            tickRupturas(boss);
            tickAraxytes(boss);
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
                            CombatAchievementService.count(p, BossMechanics.COUNT_ARAXXOR_EGGS, 1);
                        }
                    }
                }
            }
        }

        /** Acidic araxytes splatter acid around them as they die (a 7x7 area). */
        private void tickAraxytes(DataBossEntity boss) {
            for (java.util.Iterator<Mob> it = araxytes.iterator(); it.hasNext(); ) {
                Mob m = it.next();
                if (m.isAlive()) {
                    continue;
                }
                it.remove();
                if (m.isDeadOrDying() && "acidic_araxyte".equals(kinds.remove(m))) {
                    for (int i = 0; i < 5; i++) {
                        namedStrike("acid_ball", boss, around(boss, m.position(), 3.5), 1, SlayerBosses.ARAXXOR_SPLATTER_MAX, 1.0,
                                ParticleTypes.SNEEZE, (hit, pos) -> pool(boss, pos));
                    }
                } else {
                    kinds.remove(m);
                }
            }
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
                        * BossMechanics.ARAXYTE_EGG_HP);
                int hp = BossMechanics.araxyteHitpoints(eggDamage);
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
                    m.setHealth(Math.max(1f, m.getMaxHealth() * hp / BossMechanics.ARAXYTE_HP));
                    araxytes.add(m);
                    kinds.put(m, kind);
                    if ("ruptura_araxyte".equals(kind)) {
                        rupturas.add(m);
                    } else if ("mirrorback_araxyte".equals(kind)) {
                        // Melee on a mirrorback recoils half the damage back at the attacker (halberd reach not modelled).
                        MinionRules.set(m, (player, mob, type, d) -> {
                            if (type.isMelee() && d > 0) {
                                com.runecraft.combat.CombatHooks.nameAttack("mirrorback_recoil", null);
                                CombatHandler.applyDamage(mob, player, (int) Math.round(d * SlayerBosses.MIRRORBACK_RECOIL));
                            }
                            return d;
                        });
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
            kinds.remove(r);
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

        /** Game ticks from the first player hit to now (the kill, when asked for its loot); -1 when never hit. */
        long fightTicks() {
            return firstHit < 0 ? -1 : now - firstHit;
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
            kinds.clear();
        }

        /**
         * A living mirrorback araxyte takes a fifth of the damage dealt to Araxxor and recoils half of that to the attacker.
         * "Perfect Araxxor 2": no damage on it once it has enraged.
         */
        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (attacker != null && firstHit < 0) {
                firstHit = now;
            }
            if (enraged(boss) && damage > 0 && attacker != null) {
                CombatAchievementService.flag(attacker, CombatAchievements.FLAG_ARAXXOR_ENRAGE_HIT);
            }
            Mob mirror = null;
            for (java.util.Map.Entry<Mob, String> e : kinds.entrySet()) {
                if ("mirrorback_araxyte".equals(e.getValue()) && e.getKey().isAlive()) {
                    mirror = e.getKey();
                    break;
                }
            }
            if (mirror != null && damage > 0) {
                int redirected = (int) Math.round(damage * SlayerBosses.MIRRORBACK_REDIRECT);
                if (redirected > 0) {
                    CombatHandler.applyDamage(null, mirror, redirected);
                    int recoil = (int) Math.round(redirected * SlayerBosses.MIRRORBACK_RECOIL);
                    if (attacker != null && recoil > 0) {
                        com.runecraft.combat.CombatHooks.nameAttack("mirrorback_recoil", null);
                        CombatHandler.applyDamage(mirror, attacker, recoil);
                    }
                    return damage - redirected;
                }
            }
            return damage;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            araxytes.forEach(Mob::discard);
            araxytes.clear();
            kinds.clear();
            eggs.forEach(e -> {
                if (e != null) {
                    e.discard();
                }
            });
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            onDeath(boss, null);
        }
    }

}
