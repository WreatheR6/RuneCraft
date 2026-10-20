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
import com.runecraft.osrs.Prayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
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
     * Crystalline/Corrupted Hunllef (wiki): ranged first, switching between ranged and magic after every 4 attacks (standard,
     * prayer-disabling or tornado summons; the stomp doesn't count), announced before the switch. Its standard attacks go
     * through protection prayers, which only lower them (12 / 16 at most without armour, 50 / 68 unprayed). It protects
     * itself from one combat style (picked at random per run) and switches to the style of every sixth off-prayer attack it
     * takes, misses included. It stomps anyone underneath it (typeless), summons tornadoes that chase the player for 20 ticks
     * (1-3 crystalline, 2-4 corrupted, more as it weakens), and lights the floor: tiles turn blue (crimson) and then orange,
     * hurting 10-20 a tick, faster in each third of its health.
     */
    static final class Hunllef extends ScriptedBoss {
        private static final String[] STYLE_NAMES = {"melee", "ranged", "magic"};
        private final boolean corrupted;
        private final com.runecraft.osrs.content.minigames.Gauntlet.Mode mode;
        /** Its attack and protection-prayer cycles (pure rules, unit-tested). */
        private com.runecraft.osrs.content.minigames.HunllefCycle cycle =
                new com.runecraft.osrs.content.minigames.HunllefCycle(com.runecraft.osrs.content.minigames.HunllefCycle.Style.MELEE);
        private final List<Mob> tornadoes = new ArrayList<>();
        private int tornadoLife;
        /** The current floor pattern: 2x2-block tiles (north-west block), blue until {@link #floorOrangeAt}, then orange. */
        private final List<BlockPos> floor = new ArrayList<>();
        private long floorOrangeAt;
        private long floorEndAt;
        private long nextFloorAt = -1;

        Hunllef(boolean corrupted) {
            this.corrupted = corrupted;
            this.mode = corrupted ? com.runecraft.osrs.content.minigames.Gauntlet.Mode.CORRUPTED
                    : com.runecraft.osrs.content.minigames.Gauntlet.Mode.NORMAL;
        }

        /** Self-test view: the attack style in use, the protected style, the cycle counters, tornadoes and floor. */
        BossSelfTests.HunllefView view() {
            AttackType prays = switch (cycle.prays()) {
                case MELEE -> AttackType.SLASH;
                case RANGED -> AttackType.RANGED;
                case MAGIC -> AttackType.MAGIC;
            };
            return new BossSelfTests.HunllefView(cycle.magic(), prays, cycle.styleAttacks(), cycle.offPrayerHits(), tornadoes.size(),
                    floor.size(),
                    !floor.isEmpty() && now >= floorOrangeAt, attackCount);
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            com.runecraft.osrs.content.minigames.HunllefCycle.Style[] styles = com.runecraft.osrs.content.minigames.HunllefCycle.Style.values();
            cycle = new com.runecraft.osrs.content.minigames.HunllefCycle(styles[boss.getRandom().nextInt(styles.length)]);
            BossFx.say(boss, "The Hunllef protects itself from " + STYLE_NAMES[cycle.prays().ordinal()]
                    + ". Land 6 hits with another style to make it switch.");
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            boolean underneath = dx * dx + dz * dz < boss.getBbWidth() * boss.getBbWidth() / 4;
            var attack = cycle.next(underneath, boss.getRandom().nextInt(MinigameBosses.HUNLLEF_PRAYER_DISABLE_CHANCE) == 0);
            if (cycle.switchedStyle()) {
                BossFx.warn(boss, "The Hunllef switches to " + (cycle.magic() ? "magic" : "ranged") + " attacks!");
                BossFx.burst(boss, cycle.magic() ? ParticleTypes.WITCH : ParticleTypes.CRIT, 20, 1.0);
            }
            return switch (attack) {
                case STOMP -> MinigameBosses.HUNLLEF_STOMP;
                case TORNADO -> MinigameBosses.HUNLLEF_TORNADO;
                case PRAYER_DISABLE -> corrupted ? MinigameBosses.CORRUPTED_PRAYER_DISABLE : MinigameBosses.HUNLLEF_PRAYER_DISABLE;
                case MAGIC -> corrupted ? MinigameBosses.CORRUPTED_MAGIC : MinigameBosses.HUNLLEF_MAGIC;
                case RANGED -> corrupted ? MinigameBosses.CORRUPTED_RANGED : MinigameBosses.HUNLLEF_RANGED;
            };
        }

        @Override
        protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
            if (a == MinigameBosses.HUNLLEF_TORNADO) {
                last = a;
                attackCount++;
                summonTornadoes(boss);
                return;
            }
            if (a == MinigameBosses.HUNLLEF_STOMP) {
                // Typeless: no protection prayer stops it.
                last = a;
                attackCount++;
                BossFx.burst(boss, ParticleTypes.EXPLOSION, 6, 1.0);
                Msg.red(target, "The Hunllef stomps on you!");
                CombatHooks.nameAttack("stomp", null);
                CombatHandler.applyDamage(boss, target, boss.getRandom().nextInt(MinigameBosses.HUNLLEF_STOMP.maxHit() + 1));
                return;
            }
            super.perform(boss, target, a);
        }

        private void summonTornadoes(DataBossEntity boss) {
            tornadoes.forEach(Mob::discard);
            tornadoes.clear();
            int count = com.runecraft.osrs.content.minigames.Gauntlet.tornadoes(mode, percent(boss));
            tornadoLife = com.runecraft.osrs.content.minigames.Gauntlet.TORNADO_TICKS;
            BossFx.warn(boss, "The Hunllef summons tornadoes!");
            for (int i = 0; i < count; i++) {
                // On the boss's own floor (a ground search would find the roof of an enclosed arena), around it.
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                double r = 2 + boss.getRandom().nextDouble() * 2;
                Mob t = BossFx.spawnMinion(boss, "hunllef_tornado",
                        new Vec3(boss.getX() + Math.cos(a) * r, boss.getY(), boss.getZ() + Math.sin(a) * r), null);
                if (t != null) {
                    if (boss.instanceTag != null) {
                        t.addTag(boss.instanceTag); // part of the run: not a stray its minigame would clear
                    }
                    tornadoes.add(t);
                }
            }
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            // The right protection prayer lowers the hit (top roll 50 -> 12, 68 -> 16) instead of blocking it. Decided before
            // the prayer-disabling orb switches prayers off: that orb is a magic attack like the others (wiki), so Protect from
            // Magic still lowers it.
            boolean prayed = type != null && !type.isMelee()
                    && com.runecraft.osrs.NpcHitCalc.protectedFrom(com.runecraft.player.Players.data(player).activePrayers(), type);
            if (last == MinigameBosses.HUNLLEF_PRAYER_DISABLE || last == MinigameBosses.CORRUPTED_PRAYER_DISABLE) {
                BossFx.disablePrayers(player, "The Hunllef's magic attack switches off your prayers!");
            }
            lastHitThrough = prayed ? com.runecraft.osrs.content.minigames.Gauntlet.prayedDamage(mode, damage) : damage;
            return lastHitThrough;
        }

        /** Self-test: what the last hit on a player came through as, after the protection prayer (-1 before any). */
        private int lastHitThrough = -1;

        /**
         * Self-test: the prayer-disabling orb hits the target, through the real attack path (accuracy roll and all) when
         * {@code rolled} is negative, else only this script's on-hit rule for that roll. Returns the damage let through.
         */
        int selfTestPrayerDisable(DataBossEntity boss, ServerPlayer target, int rolled) {
            AttackDef orb = corrupted ? MinigameBosses.CORRUPTED_PRAYER_DISABLE : MinigameBosses.HUNLLEF_PRAYER_DISABLE;
            if (rolled < 0) {
                lastHitThrough = -1;
                perform(boss, target, orb);
                return lastHitThrough;
            }
            AttackDef was = last;
            last = orb;
            try {
                return onHitPlayer(boss, target, rolled, AttackType.MAGIC);
            } finally {
                last = was;
            }
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            Category c = category(type);
            if (cycle.playerAttack(com.runecraft.osrs.content.minigames.HunllefCycle.Style.values()[c.ordinal()])) {
                CombatAchievementService.flag(attacker, CombatAchievements.FLAG_HUNLLEF_WRONG_WEAPON);
                Msg.action(attacker, "The Hunllef's prayer protects it from " + STYLE_NAMES[c.ordinal()] + "!");
                return 0;
            }
            // Every sixth off-prayer attack (zero hits too) makes it pray against that attack's style.
            if (cycle.prayerSwitched()) {
                BossFx.say(boss, "The Hunllef switches its prayer to protect from " + STYLE_NAMES[c.ordinal()] + "!");
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (target == null) {
                return;
            }
            BossFx.world(boss).sendParticles(switch (cycle.prays()) {
                case MELEE -> ParticleTypes.CRIT;
                case RANGED -> ParticleTypes.HAPPY_VILLAGER;
                case MAGIC -> ParticleTypes.WITCH;
            }, boss.getX(), boss.getY() + boss.getBbHeight() + 0.5, boss.getZ(), 3, 0.3, 0.1, 0.3, 0);
            tickTornadoes(boss, target);
            tickFloor(boss, target);
        }

        private void tickTornadoes(DataBossEntity boss, ServerPlayer target) {
            if (tornadoLife <= 0) {
                return;
            }
            tornadoLife--;
            int[] dmg = com.runecraft.osrs.content.minigames.Gauntlet.tornadoDamage(mode);
            tornadoes.removeIf(t -> !t.isAlive() || t.isRemoved());
            for (Mob t : tornadoes) {
                Vec3 d = target.position().subtract(t.position());
                double len = d.horizontalDistance();
                if (len < 1.2) {
                    CombatHooks.nameAttack("tornado", null);
                    CombatHandler.applyDamage(boss, target, dmg[0] + boss.getRandom().nextInt(dmg[1] - dmg[0] + 1));
                } else {
                    Vec3 step = new Vec3(d.x / len, 0, d.z / len);
                    t.teleportTo(t.getX() + step.x, t.getY(), t.getZ() + step.z);
                }
            }
            if (tornadoLife == 0) {
                tornadoes.forEach(Mob::discard);
                tornadoes.clear();
            }
        }

        /** The damaging floor: a pattern of tiles around the player turns blue (crimson), then orange (approx patterns). */
        private void tickFloor(DataBossEntity boss, ServerPlayer target) {
            int phase = com.runecraft.osrs.content.minigames.Gauntlet.floorPhase(percent(boss));
            ServerLevel w = BossFx.world(boss);
            if (nextFloorAt < 0) {
                nextFloorAt = now + com.runecraft.osrs.content.minigames.Gauntlet.FLOOR_INTERVAL_TICKS[0];
            }
            if (floor.isEmpty() && now >= nextFloorAt) {
                int tiles = 6 + 3 * phase;
                BlockPos at = target.blockPosition();
                for (int i = 0; i < tiles; i++) {
                    int ox = (boss.getRandom().nextInt(7) - 3) * 2;
                    int oz = (boss.getRandom().nextInt(7) - 3) * 2;
                    floor.add(new BlockPos(at.getX() + ox, at.getY(), at.getZ() + oz));
                }
                floorOrangeAt = now + com.runecraft.osrs.content.minigames.Gauntlet.FLOOR_WARN_TICKS[phase];
                floorEndAt = floorOrangeAt + com.runecraft.osrs.content.minigames.Gauntlet.FLOOR_ACTIVE_TICKS;
                nextFloorAt = floorEndAt + com.runecraft.osrs.content.minigames.Gauntlet.FLOOR_INTERVAL_TICKS[phase];
            }
            if (floor.isEmpty()) {
                return;
            }
            boolean orange = now >= floorOrangeAt;
            for (BlockPos t : floor) {
                w.sendParticles(orange ? ParticleTypes.FLAME : corrupted ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.SOUL_FIRE_FLAME,
                        t.getX() + 1.0, t.getY() + 0.1, t.getZ() + 1.0, 4, 0.5, 0.02, 0.5, 0);
            }
            if (orange) {
                BlockPos p = target.blockPosition();
                for (BlockPos t : floor) {
                    if (p.getX() >= t.getX() && p.getX() <= t.getX() + 1 && p.getZ() >= t.getZ() && p.getZ() <= t.getZ() + 1
                            && Math.abs(p.getY() - t.getY()) <= 1) {
                        CombatHooks.nameAttack("damaging_floor", null);
                        CombatHandler.applyDamage(boss, target, com.runecraft.osrs.content.minigames.Gauntlet.FLOOR_MIN + boss.getRandom()
                                .nextInt(com.runecraft.osrs.content.minigames.Gauntlet.FLOOR_MAX - com.runecraft.osrs.content.minigames.Gauntlet.FLOOR_MIN + 1));
                        break;
                    }
                }
            }
            if (now >= floorEndAt) {
                floor.clear();
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            tornadoes.forEach(Mob::discard);
            tornadoes.clear();
            floor.clear();
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            tornadoes.forEach(Mob::discard);
            tornadoes.clear();
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
        /** Players who drank a sanfew serum or relicym's balm since their parasite landed (it bursts out weakened). */
        private static final java.util.Set<java.util.UUID> CURED = java.util.concurrent.ConcurrentHashMap.newKeySet();

        /**
         * Weary players' slower attacks: uuid -> {extra game ticks, Minecraft tick the weariness wears off}. The attack a
         * weary player starts pushes their next one back (approx: melee and magic only; bows have no attack timer here).
         */
        private static final Map<UUID, long[]> SLOWED = new java.util.concurrent.ConcurrentHashMap<>();

        static {
            com.runecraft.content.skilling.ItemHooks.DISEASE_CURE.add(p -> CURED.add(p.getUUID()));
            MinionRules.watch((p, t, type, d) -> {
                long[] slow = SLOWED.get(p.getUUID());
                if (slow == null) {
                    return;
                }
                long now = com.runecraft.action.ActionManager.mcTick();
                if (now >= slow[1]) {
                    SLOWED.remove(p.getUUID());
                    return;
                }
                com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
                if (data.lastAttackTick == now) { // the attack just started (pushed back once, even for multi-hit specials)
                    data.lastAttackTick = now + slow[0] * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK;
                }
            });
        }

        private final List<Mob> parasites = new ArrayList<>();
        private int parasiteHealTimer = BossMechanics.PARASITE_HEAL_INTERVAL;
        private final List<Mob> totems = new ArrayList<>();
        private final List<Mob> husks = new ArrayList<>();
        private boolean shielded;
        private int parasiteIn = -1;
        private ServerPlayer host;
        private int curseTimer = 40;
        private final boolean phosani;
        private final List<Mob> walkers = new ArrayList<>();
        private long walkersMoveAt;
        private int setTotal;
        private int setReached;
        private boolean finalSet;
        private boolean desperate;
        private int walkerTimer;
        /** Spores on the arena floor (OSRS Wiki: within one tile one bursts and makes the player weary). */
        private final List<Vec3> spores = new ArrayList<>();
        private int sporeTimer = 50;
        private int sporesLeft;
        /** Weary players: game ticks left (no running, slower attacks; Phosani's yawns drain prayer). */
        private final Map<UUID, Integer> weary = new HashMap<>();
        /** Phosani's Prayer Deactivation (phase three): game ticks until one of her standard attacks can switch a prayer off. */
        private int deactivateTimer = 20;
        /** Her claw swipe when it deactivates a prayer: it lands 2 ticks after the wind-up, like her magic and ranged. */
        private static final AttackDef PHOSANI_MELEE_WIND_UP = MinigameBosses.PHOSANI_MELEE.telegraph(2, null);

        Nightmare() {
            this(false);
        }

        Nightmare(boolean phosani) {
            this.phosani = phosani;
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            home = boss.blockPosition();
            raiseShield(boss);
        }

        private void raiseShield(DataBossEntity boss) {
            totems.forEach(Mob::discard);
            totems.clear();
            Vec3 c = homeVec();
            for (int[] o : new int[][]{{-9, -9}, {9, -9}, {-9, 9}, {9, 9}}) {
                Mob t = BossFx.spawnMinion(boss, "nightmare_totem", BossFx.ground(BossFx.world(boss), c.x + o[0], c.z + o[1], c.y), null);
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
            ServerPlayer target = target(boss);
            if (target != null) {
                husks.clear();
                for (int i = 0; i < 2; i++) {
                    Mob h = BossFx.spawnMinion(boss, "nightmare_husk", around(boss, target.position(), 2), target);
                    if (h != null) {
                        husks.add(h);
                        watchOneHit(h);
                    }
                }
                BossFx.stun(target, 30);
                Msg.red(target, "Husks grab hold of you! Kill them to break free.");
            }
        }

        /** Phosani's "Crush Hour": every parasite and husk must die to the first hit it takes. */
        private void watchOneHit(Mob m) {
            if (!phosani) {
                return;
            }
            MinionRules.set(m, (p, mob, type, d) -> {
                if (d > 0 && d < mob.getHealth()) {
                    CombatAchievementService.flag(p, BossMechanics.FLAG_PHOSANI_MINION_SURVIVED);
                }
                return d;
            });
        }

        /**
         * Parasites support the Nightmare (OSRS Wiki "Parasite"): every few ticks each one restores 31-50 of her health, or
         * of the weakest totem while she is shielded; counted for "Terrible Parent".
         */
        private void tickParasites(DataBossEntity boss, ServerPlayer target) {
            parasites.removeIf(m -> !m.isAlive());
            if (parasites.isEmpty() || --parasiteHealTimer > 0) {
                return;
            }
            parasiteHealTimer = BossMechanics.PARASITE_HEAL_INTERVAL;
            for (Mob parasite : parasites) {
                int heal = BossMechanics.parasiteHeal(new java.util.Random(boss.getRandom().nextLong()));
                net.minecraft.world.entity.LivingEntity healed = boss;
                if (shielded) {
                    Mob weakest = null;
                    for (Mob t : totems) {
                        if (t.isAlive() && t.getHealth() < t.getMaxHealth() && (weakest == null || t.getHealth() < weakest.getHealth())) {
                            weakest = t;
                        }
                    }
                    if (weakest == null) {
                        continue; // nothing to support: it attacks the player instead (its own AttackDef)
                    }
                    weakest.heal(heal);
                    healed = weakest;
                } else if (boss.getHealth() < boss.getMaxHealth()) {
                    boss.heal(mcHp(boss, heal));
                } else {
                    continue;
                }
                BossFx.burst(parasite, ParticleTypes.HEART, 4, 0.4);
                BossFx.burst(healed, ParticleTypes.HEART, 6, 0.8);
                java.util.Set<ServerPlayer> fighters = new java.util.LinkedHashSet<>(BossFx.players(boss, 24));
                if (target != null) {
                    fighters.add(target);
                }
                for (ServerPlayer p : fighters) {
                    CombatAchievementService.count(p, BossMechanics.COUNT_NIGHTMARE_PARASITE_HEAL, heal);
                }
            }
        }

        /** Sleepwalkers walk in from the edges of the arena; any hit kills one. */
        private void summonWalkers(DataBossEntity boss, int count) {
            Vec3 c = homeVec();
            for (int i = 0; i < count; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                Mob m = BossFx.spawnMinion(boss, BossMechanics.PHOSANI_SLEEPWALKER,
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 11, c.z + Math.sin(a) * 11, c.y), null);
                if (m == null) {
                    continue;
                }
                walkers.add(m);
                MinionRules.set(m, new MinionRules.Rule() {
                    @Override
                    public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type) {
                        return MinionRules.ALWAYS_HITS;
                    }

                    @Override
                    public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type, int d) {
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

        private void tickWalkers(DataBossEntity boss, ServerPlayer target) {
            java.util.Iterator<Mob> it = walkers.iterator();
            while (it.hasNext()) {
                Mob m = it.next();
                if (!m.isAlive()) {
                    it.remove();
                    continue;
                }
                if (now < walkersMoveAt) {
                    continue;
                }
                Vec3 d = boss.position().subtract(m.position());
                double len = d.horizontalDistance();
                if (len <= boss.getBbWidth() / 2 + 1.0) {
                    m.discard();
                    it.remove();
                    if (desperate) {
                        CombatAchievementService.flagNear(boss, BossMechanics.FLAG_PHOSANI_SLEEPWALKER);
                        for (ServerPlayer p : BossFx.players(boss, 24)) {
                            CombatHooks.nameAttack("sleepwalker", null);
                            CombatHandler.applyDamage(boss, p, BossMechanics.DESPERATION_SLEEPWALKER_DAMAGE);
                        }
                    } else {
                        setReached++;
                    }
                    BossFx.burst(boss, ParticleTypes.WITCH, 10, 1.0);
                    continue;
                }
                // Desperation sleepwalkers reach her ~20 ticks after appearing at the arena's edge (11 blocks away).
                double step = Math.min(desperate ? 11.0 / BossMechanics.DESPERATION_WALKER_TICKS : 0.35, len);
                m.teleportTo(m.getX() + d.x / len * step, m.getY(), m.getZ() + d.z / len * step);
            }
            if (setTotal > 0 && walkers.isEmpty()) {
                // Every sleepwalker of the set is dead or arrived: the power blast.
                int dmg = BossMechanics.sleepwalkerBlast(setReached, setTotal);
                setTotal = 0;
                BossFx.say(boss, setReached == 0 ? "The Nightmare's power blast fizzles." : "The Nightmare unleashes a power blast!");
                for (ServerPlayer p : BossFx.players(boss, 24)) {
                    CombatHooks.nameAttack("power_blast", null);
                    CombatHandler.applyDamage(boss, p, dmg);
                }
                if (finalSet && !desperate) {
                    desperate = true;
                    walkerTimer = 3; // approx: she moves to the centre first
                    BossFx.warn(boss, "Phosani's Nightmare lashes out in desperation!");
                }
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            tickSpores(boss);
            if (phosani) {
                tickWalkers(boss, target);
                // The end of her third phase: 150 of her 1,350 physical health left (OSRS Wiki).
                if (!finalSet && !shielded && percent(boss) <= BossMechanics.phosaniPhaseEnd(3)) {
                    finalSet = true;
                    summonWalkers(boss, BossMechanics.sleepwalkers(3));
                }
                // Desperation: one sleepwalker at a time, the next as soon as the last is gone (approx: a 2-tick gap).
                if (desperate && target != null && walkers.isEmpty() && --walkerTimer <= 0) {
                    walkerTimer = 2;
                    summonWalkers(boss, 1);
                    walkersMoveAt = now;
                }
            }
            if (shielded) {
                BossFx.ring(BossFx.world(boss), boss.position(), 2.0, ParticleTypes.WITCH);
                if (totems.stream().noneMatch(Mob::isAlive)) {
                    shielded = false;
                    BossFx.say(boss, "The totems blast the Nightmare, shattering her shield!");
                    float blast = mcHp(boss, 800);
                    boss.setHealth(Math.max(1, boss.getHealth() - Math.min(blast, boss.getMaxHealth() * 0.1f)));
                    BossFx.sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 0.7f);
                }
            }
            if (!husks.isEmpty() && husks.stream().noneMatch(Mob::isAlive)) {
                husks.clear();
                if (target != null) {
                    BossFx.unstun(target);
                }
            }
            if (target == null) {
                return;
            }
            tickParasites(boss, target);
            if (boss.phase() >= 0 && parasiteIn < 0 && boss.getRandom().nextInt(45) == 0) {
                parasiteIn = BossMechanics.PARASITE_BURST_TICKS;
                host = target;
                CURED.remove(target.getUUID());
                Msg.red(target, "The Nightmare has impregnated you with a deadly parasite! Drink a sanfew serum or relicym's balm.");
            }
            if (parasiteIn > 0 && --parasiteIn == 0) {
                if (host != null && host.isAlive()) {
                    boolean weakened = CURED.remove(host.getUUID());
                    CombatHooks.nameAttack(weakened ? BossMechanics.WEAKENED_PARASITE_BURST : BossMechanics.PARASITE_BURST, null);
                    CombatHandler.applyDamage(boss, host, BossMechanics.parasiteBurst(new java.util.Random(boss.getRandom().nextLong()), weakened));
                    Mob parasite = BossFx.spawnMinion(boss, "nightmare_parasite", host.position(), host);
                    if (parasite != null) {
                        if (!weakened) {
                            // 80 hitpoints instead of the weakened parasite's 40.
                            var hp = parasite.getAttribute(Attributes.MAX_HEALTH);
                            if (hp != null) {
                                hp.setBaseValue(hp.getBaseValue() * BossMechanics.PARASITE_HP / BossMechanics.PARASITE_HP_WEAKENED);
                                parasite.setHealth(parasite.getMaxHealth());
                            }
                        }
                        parasites.add(parasite);
                        watchOneHit(parasite);
                    }
                    Msg.red(host, weakened ? "The weakened parasite bursts out of you!" : "The parasite bursts out of you!");
                }
                parasiteIn = -1;
            }
            if (--curseTimer <= 0) {
                curseTimer = 45 + boss.getRandom().nextInt(20);
                BossFx.disablePrayers(target, "The Nightmare curses you, shuffling your prayers!");
            }
            // Spores: the Nightmare only uses them in her third phase; Phosani in any phase (her A special sets). approx rate
            if (!shielded && (phosani || boss.phase() >= 1) && spores.isEmpty() && --sporeTimer <= 0) {
                sporeTimer = 60 + boss.getRandom().nextInt(30);
                placeSpores(boss);
            }
            if (phosani && boss.phase() >= 1 && !desperate && deactivateTimer > 0) {
                deactivateTimer--;
            }
        }

        void placeSpores(DataBossEntity boss) {
            spores.clear();
            Vec3 c = homeVec();
            for (int i = 0; i < BossMechanics.SPORES; i++) {
                spores.add(around(boss, c, 8));
            }
            sporesLeft = 30; // approx: how long they stay
            BossFx.warn(boss, "Spores sprout around the arena! Don't stand next to them.");
        }

        /** Spores burst when a player comes within a tile; weary players can't run and their yawns drain prayer (Phosani). */
        void tickSpores(DataBossEntity boss) {
            if (!spores.isEmpty()) {
                List<ServerPlayer> near = BossFx.players(boss, 32);
                java.util.Iterator<Vec3> it = spores.iterator();
                while (it.hasNext()) {
                    Vec3 sp = it.next();
                    BossFx.world(boss).sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, sp.x, sp.y + 0.4, sp.z, 2, 0.2, 0.2, 0.2, 0);
                    for (ServerPlayer p : near) {
                        if (Math.abs(p.getX() - sp.x) <= 1.5 && Math.abs(p.getZ() - sp.z) <= 1.5) {
                            burstSpore(boss, p, sp);
                            it.remove();
                            break;
                        }
                    }
                }
                if (--sporesLeft <= 0) {
                    spores.clear();
                }
            }
            if (weary.isEmpty()) {
                return;
            }
            java.util.Iterator<Map.Entry<UUID, Integer>> wi = weary.entrySet().iterator();
            while (wi.hasNext()) {
                Map.Entry<UUID, Integer> e = wi.next();
                ServerPlayer p = BossFx.world(boss).getServer().getPlayerList().getPlayer(e.getKey());
                int left = e.getValue() - 1;
                if (p == null || left <= 0) {
                    SLOWED.remove(e.getKey());
                    wi.remove();
                    continue;
                }
                e.setValue(left);
                if (p.isSprinting()) {
                    // No running (as when out of run energy): about walking pace while they try to sprint (renewed each
                    // game tick they do, so it wears off as they stop); walking itself is not slowed.
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK + 2, 1, false, false));
                    p.setSprinting(false);
                }
                if (phosani && left % 5 == 0) {
                    BossFx.drainPrayer(p, BossMechanics.SPORE_YAWN_PRAYER);
                    Msg.action(p, "You yawn...");
                }
            }
        }

        void burstSpore(DataBossEntity boss, ServerPlayer p, Vec3 at) {
            BossFx.world(boss).sendParticles(ParticleTypes.SNEEZE, at.x, at.y + 0.6, at.z, 20, 0.6, 0.4, 0.6, 0.02);
            weary.put(p.getUUID(), BossMechanics.SPORE_WEARY_TICKS);
            SLOWED.put(p.getUUID(), new long[]{phosani ? BossMechanics.SPORE_SLOW_TICKS_PHOSANI : BossMechanics.SPORE_SLOW_TICKS,
                    com.runecraft.action.ActionManager.mcTick()
                            + (long) BossMechanics.SPORE_WEARY_TICKS * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK});
            CombatAchievementService.flag(p, BossMechanics.FLAG_NIGHTMARE_SPORED);
            Msg.red(p, "A spore bursts in your face and you feel weary.");
        }

        /**
         * Phosani's Prayer Deactivation (OSRS Wiki, phase three only): as she starts one of her standard attacks, a player
         * already praying the matching protection prayer has that prayer switched off; praying it only after the wind-up is
         * the counter. It acts on the attack her normal rotation picked (in range, at her normal speed) and leaves her
         * other prayers alone. approx: the wind-up is announced in chat and the attack lands 2 ticks later; at most every
         * 16-25 ticks (rate guessed); not in her desperation phase.
         */
        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (!phosani || desperate || boss.phase() < 1 || deactivateTimer > 0 || target == null) {
                return attack;
            }
            AttackDef[] styles = {MinigameBosses.PHOSANI_MELEE, MinigameBosses.PHOSANI_MAGIC, MinigameBosses.PHOSANI_RANGED};
            Prayer[] counters = {Prayer.PROTECT_FROM_MELEE, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES};
            int i = java.util.Arrays.asList(styles).indexOf(attack);
            if (i < 0) {
                return attack;
            }
            deactivateTimer = 16 + boss.getRandom().nextInt(10);
            BossFx.warn(boss, "Phosani's Nightmare winds up a " + (i == 0 ? "melee" : i == 1 ? "magic" : "ranged") + " attack...");
            if (BossFx.protects(target, counters[i])) {
                BossFx.disablePrayer(target, counters[i], "Phosani's Nightmare saw your prayer coming and switches it off!");
                CombatAchievementService.flag(target, BossMechanics.FLAG_PHOSANI_PRAYER_DISABLED);
            }
            return i == 0 ? PHOSANI_MELEE_WIND_UP : attack;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            spores.clear();
            weary.keySet().forEach(SLOWED::remove);
            weary.clear();
            totems.forEach(Mob::discard);
            husks.forEach(Mob::discard);
            walkers.forEach(Mob::discard);
            parasites.forEach(Mob::discard);
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            onDeath(boss, null); // reset: totems, husks, sleepwalkers and parasites go, and nobody stays slowed
        }
    }

    /**
     * Self-test (boss:nightmare_parasites): a parasite that lands on a player who then drinks a cure bursts out weakened
     * (40 hitpoints), and a living parasite heals the unshielded Nightmare.
     */
    static String selfTestNightmare(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Nightmare n)) {
            throw new IllegalStateException("the Nightmare has no Nightmare script");
        }
        n.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
        try {
            n.parasiteIn = 1;
            n.host = p;
            com.runecraft.content.skilling.ItemHooks.DISEASE_CURE.forEach(l -> l.accept(p)); // a sanfew serum dose
            n.onGameTick(boss, p);
            n.parasites.removeIf(m -> !m.isAlive());
            if (n.parasites.size() != 1) {
                throw new IllegalStateException(n.parasites.size() + " parasites after the burst, expected 1");
            }
            float weakHp = n.parasites.get(0).getMaxHealth();
            n.shielded = false;
            boss.setHealth(boss.getMaxHealth() * 0.5f);
            float before = boss.getHealth();
            n.parasiteHealTimer = 1;
            n.tickParasites(boss, p);
            if (boss.getHealth() <= before) {
                throw new IllegalStateException("the parasite didn't heal the Nightmare (" + before + " -> " + boss.getHealth() + ")");
            }
            return "weakened parasite " + weakHp + " hp, healed the Nightmare " + before + " -> " + boss.getHealth();
        } finally {
            Nightmare.CURED.remove(p.getUUID());
            n.onDeath(boss, p);
        }
    }

    /**
     * Self-test (boss:muspah_shield): Smite and a corruption spell drain the prayer shield beyond its 2 per attack, and a
     * spike raised under a player hits them and stays.
     */
    static String selfTestMuspah(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Muspah m)) {
            throw new IllegalStateException("the Phantom Muspah has no Muspah script");
        }
        m.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
        var data = com.runecraft.player.Players.data(p);
        var sp = com.runecraft.content.systems.SysState.of(p);
        boolean hadSmite = data.activePrayers().contains(Prayer.SMITE);
        try {
            m.onPhase(boss, 1);
            data.activePrayers().add(Prayer.SMITE);
            int absorbed = m.incoming(boss, p, 20, AttackType.RANGED);
            int afterSmite = m.shield;
            int expected = BossMechanics.MUSPAH_SHIELD - BossMechanics.MUSPAH_SHIELD_PER_ATTACK - BossMechanics.muspahSmiteDrain(20);
            if (absorbed != 0 || afterSmite > expected) {
                throw new IllegalStateException("Smite hit: took " + absorbed + ", shield " + afterSmite + " (expected <= " + expected + ")");
            }
            data.activePrayers().remove(Prayer.SMITE);
            sp.corruptionPending = 2;
            sp.corruptionPendingUntil = com.runecraft.content.systems.SysTicks.now() + 100;
            m.incoming(boss, p, 0, AttackType.MAGIC);
            int afterCorruption = m.shield;
            if (afterSmite - afterCorruption != BossMechanics.MUSPAH_SHIELD_PER_ATTACK + BossMechanics.muspahCorruptionDrain(true)) {
                throw new IllegalStateException("Greater Corruption drained " + (afterSmite - afterCorruption) + " of the shield");
            }
            if (!m.drains.containsAll(java.util.Set.of(BossMechanics.DRAIN_SMITE, BossMechanics.DRAIN_CORRUPTION))) {
                throw new IllegalStateException("drain sources " + m.drains);
            }
            long under = BossMechanics.tile(p.getBlockX(), p.getBlockZ());
            m.raiseSpikes(boss, p, List.of());
            if (!m.spikes.contains(under)) {
                throw new IllegalStateException("no spike raised under the player");
            }
            return "shield 75 -> " + afterSmite + " (Smite) -> " + afterCorruption + " (Greater Corruption), " + m.spikes.size() + " spikes";
        } finally {
            if (!hadSmite) {
                data.activePrayers().remove(Prayer.SMITE);
            }
            sp.corruptionPending = 0;
        }
    }

    /**
     * Phantom Muspah: ranged form, then a melee charge (faster), then a frozen prayer shield that soaks damage. Every form
     * change raises spikes (one under the player) that knock a player standing there aside and heal it 75% of the damage;
     * once the shield breaks, spikes grow from the existing clusters every four of its attacks until they surround it.
     * The shield loses 2 per attack plus whatever prayer drains take: Smite, an enchanted sapphire bolt, a corruption spell.
     */
    static final class Muspah extends ScriptedBoss {
        private int shield;
        private boolean shieldBroken;
        private final Set<String> drains = new HashSet<>();
        private final Set<Long> spikes = new HashSet<>();
        private int attacks;
        private int spikeFx;

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            ServerPlayer target = target(boss);
            if (phase == 0) {
                var speed = boss.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(0.3);
                }
                raiseSpikes(boss, target, randomTiles(boss, 6));
            } else if (phase == 1) {
                shield = BossMechanics.MUSPAH_SHIELD;
                drains.clear();
                var speed = boss.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(0.18);
                }
                raiseSpikes(boss, target, randomTiles(boss, 4));
            }
        }

        private List<Long> randomTiles(DataBossEntity boss, int n) {
            List<Long> tiles = new ArrayList<>();
            Vec3 c = homeVec();
            for (int i = 0; i < n; i++) {
                Vec3 v = around(boss, c, 8);
                tiles.add(BossMechanics.tile((int) Math.floor(v.x), (int) Math.floor(v.z)));
            }
            return tiles;
        }

        /** New spikes (plus one under the target): players standing on one are hit, knocked aside, and heal the Muspah. */
        private void raiseSpikes(DataBossEntity boss, ServerPlayer target, List<Long> tiles) {
            List<Long> fresh = new ArrayList<>(tiles);
            if (target != null) {
                fresh.add(BossMechanics.tile(target.getBlockX(), target.getBlockZ()));
            }
            spikes.addAll(fresh);
            Set<ServerPlayer> near = new java.util.LinkedHashSet<>(BossFx.players(boss, 24));
            if (target != null) {
                near.add(target);
            }
            for (ServerPlayer p : near) {
                if (!fresh.contains(BossMechanics.tile(p.getBlockX(), p.getBlockZ()))) {
                    continue;
                }
                int dmg = 1 + boss.getRandom().nextInt(BossMechanics.MUSPAH_SPIKE_MAX);
                CombatHooks.nameAttack("spike", null);
                int dealt = CombatHandler.applyDamage(boss, p, dmg);
                boss.heal(mcHp(boss, BossMechanics.muspahSpikeHeal(dealt)));
                Vec3 away = p.position().subtract(boss.position()).normalize();
                p.knockback(0.8, -away.x, -away.z);
                p.hurtMarked = true;
                Msg.red(p, "A spike erupts beneath you!");
            }
            BossFx.sound(boss, SoundEvents.GLASS_BREAK, 0.6f);
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (shield > 0) {
                // OSRS Wiki: the shielded form is immune to damage; its 75-point prayer shield loses 2 per attack, and
                // prayer drains speed it up.
                shield -= BossMechanics.MUSPAH_SHIELD_PER_ATTACK;
                if (attacker != null) {
                    shield -= drainShield(attacker, damage, type);
                    if (BossMechanics.versatileDrainer(drains)) {
                        CombatAchievementService.flag(attacker, BossMechanics.FLAG_MUSPAH_THREE_DRAINS);
                    }
                }
                BossFx.burst(boss, ParticleTypes.SNOWFLAKE, 8, 1.0);
                if (shield <= 0) {
                    shield = 0;
                    shieldBroken = true;
                    BossFx.say(boss, "The Phantom Muspah's prayer shield shatters!");
                    raiseSpikes(boss, attacker, randomTiles(boss, 10));
                } else if (attacker != null) {
                    Msg.action(attacker, "The Phantom Muspah's prayer shield absorbs your attack. (" + shield + " left)");
                }
                return 0;
            }
            return damage;
        }

        /** Extra shield points this attack's prayer drains take: Smite, an enchanted sapphire bolt, a pending corruption spell. */
        private int drainShield(ServerPlayer attacker, int damage, AttackType type) {
            int drained = 0;
            if (com.runecraft.player.Players.data(attacker).isActive(Prayer.SMITE) && damage > 0) {
                drained += BossMechanics.muspahSmiteDrain(damage);
                drains.add(BossMechanics.DRAIN_SMITE);
            }
            if (type == AttackType.RANGED && damage > 0) {
                String ammo = com.runecraft.content.systems.BoltProcs.takeShot();
                var en = com.runecraft.osrs.content.systems.BoltEffects.ofItem(ammo);
                if (en != null && en.effect() == com.runecraft.osrs.content.systems.BoltEffects.Effect.CLEAR_MIND) {
                    boolean kandarin = com.runecraft.osrs.content.systems.Diaries.tierComplete(
                            com.runecraft.content.systems.SysState.of(attacker).diaryDone, "kandarin",
                            com.runecraft.osrs.content.systems.Diaries.Tier.HARD);
                    double chance = BossMechanics.MUSPAH_SAPPHIRE_CHANCE
                            * (kandarin ? com.runecraft.osrs.content.systems.BoltEffects.KANDARIN_FACTOR : 1.0);
                    if (attacker.getRandom().nextDouble() < chance) {
                        drained += BossMechanics.MUSPAH_SAPPHIRE_DRAIN;
                        drains.add(BossMechanics.DRAIN_SAPPHIRE);
                    }
                }
            }
            var sp = com.runecraft.content.systems.SysState.of(attacker);
            if (sp.corruptionPending > 0 && sp.corruptionPendingUntil >= com.runecraft.content.systems.SysTicks.now()) {
                drained += BossMechanics.muspahCorruptionDrain(sp.corruptionPending == 2);
                sp.corruptionPending = 0;
                drains.add(BossMechanics.DRAIN_CORRUPTION);
            }
            return drained;
        }

        /** While shielded it also has Soul Split: it heals half the damage it deals. */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (shield > 0 && damage > 1) {
                boss.heal(mcHp(boss, damage / 2));
            }
            if (shieldBroken && ++attacks % BossMechanics.MUSPAH_SPIKE_EVERY == 0) {
                Vec3 c = homeVec();
                raiseSpikes(boss, player, BossMechanics.muspahSpikeGrowth(spikes, (int) Math.floor(c.x), (int) Math.floor(c.z), 9,
                        0.35, new java.util.Random(boss.getRandom().nextLong()))); // approx: growth rate
            }
            return damage;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (shield > 0) {
                BossFx.ring(BossFx.world(boss), boss.position(), 2.2, ParticleTypes.SNOWFLAKE);
            }
            if (!spikes.isEmpty() && ++spikeFx % 3 == 0) {
                ServerLevel w = BossFx.world(boss);
                int shown = 0;
                for (long t : spikes) {
                    if (++shown > 120) {
                        break;
                    }
                    w.sendParticles(ParticleTypes.SNOWFLAKE, (int) (t >> 32) + 0.5, boss.getY() + 0.3, (int) t + 0.5, 2, 0.15, 0.4, 0.15, 0.0);
                }
            }
            if (shieldBroken) {
                // "Space is Tight": spikes on every tile around its footprint.
                int size = Math.max(1, (int) Math.ceil(boss.getBbWidth()));
                int x = (int) Math.floor(boss.getX() - size / 2.0);
                int z = (int) Math.floor(boss.getZ() - size / 2.0);
                boolean surrounded = BossMechanics.muspahSurrounded(spikes, x, z, size);
                Set<ServerPlayer> near = new java.util.LinkedHashSet<>(BossFx.players(boss, 24));
                if (target != null) {
                    near.add(target);
                }
                for (ServerPlayer p : near) {
                    if (surrounded) {
                        CombatAchievementService.flag(p, BossMechanics.FLAG_MUSPAH_SURROUNDED);
                    } else {
                        CombatAchievementService.unflag(p, BossMechanics.FLAG_MUSPAH_SURROUNDED);
                    }
                }
            }
        }

        /** The spikes it stands in now (for the self-test). */
        Set<Long> spikes() {
            return spikes;
        }

        int shield() {
            return shield;
        }
    }

    /**
     * Duke Sucellus (OSRS Wiki "Duke Sucellus/Strategies"): fist slams up close and magic otherwise on a 5-tick cycle (4
     * once enraged at 25%). Every fifth standard attack he turns his Freezing Gaze on everyone he can see (up to 101, 5 ticks
     * to hide behind a pillar); gas flares follow every 5/4/3 standard attacks (above 75%/25%/below) and leave gas on the
     * vents near the player (up to 12 a tick), one flare above 50% and two below. Awakened: 4 flares below 76%, gas up to 27,
     * the gaze up to 121, enraged at 25.9%, and from 32% an Orb of Darkness every ~10 seconds (27, drains combat stats by 8).
     */
    static final class Duke extends ScriptedBoss {
        private static final String NBT_PILLARS_UP = "DukePillarsUp";
        private static final String NBT_PILLARS = "DukePillars";
        private boolean pillars;
        /**
         * The pillar blocks raised around a Duke standing away from his arena (the Desert Treasure II quest version, a boss
         * spawned elsewhere): there they are his, not the arena's scenery, and go when he does instead of staying in the world.
         */
        private final List<BlockPos> pillarBlocks = new ArrayList<>();
        /** Standard attacks since the last gaze and since the last gas flares. */
        private int sinceGaze;
        private int sinceFlare;
        /** The flares never come straight after a gaze (he makes one more standard attack first). */
        private boolean justGazed;
        /** Game tick the current gaze lands, or -1. */
        private long gazeAt = -1;
        private int orbTimer = BossMechanics.DUKE_ORB_INTERVAL;
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
            return gazeAt < 0;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            return BossMechanics.dukeEnraged(percent(boss), boss.awakened()) ? 4 : 5;
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            double pct = percent(boss);
            if (sinceGaze >= BossMechanics.DUKE_GAZE_EVERY) {
                sinceGaze = 0;
                justGazed = true;
                gazeAt = now + BossMechanics.DUKE_GAZE_TICKS;
                for (ServerPlayer p : BossFx.players(boss, 24)) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("Duke Sucellus turns his gaze upon you...")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), false);
                }
                return null;
            }
            if (!justGazed && sinceFlare >= BossMechanics.dukeFlareInterval(pct)) {
                sinceFlare = 0;
                int flares = BossMechanics.dukeFlares(pct, boss.awakened());
                int gas = (int) Math.round((boss.awakened() ? BossMechanics.DUKE_GAS_MAX_AWAKENED : BossMechanics.DUKE_GAS_MAX) * scale);
                BossFx.warn(boss, "Duke Sucellus spits gas at the vents!");
                for (int i = 0; i < flares; i++) {
                    Vec3 at = i == 0 ? target.position() : around(boss, target.position(), 3);
                    strike(boss, at, 2 + i, 0, 1.3, ParticleTypes.SNEEZE, (hit, pos) ->
                            com.runecraft.boss.Mechanics.addHazard(BossFx.world(boss), pos, gas, BossMechanics.DUKE_GAS_TICKS, boss));
                }
                cooldown = 2;
                return null;
            }
            justGazed = false;
            sinceGaze++;
            sinceFlare++;
            return distance < 5 ? Dt2Bosses.DUKE_SLAM : Dt2Bosses.DUKE_MAGIC;
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (!pillars) {
                pillars = true;
                // Four pillars to hide from his gaze, standing on the floor around the arena's centre: the same blocks on every
                // summon (earlier versions stacked a new floating set around each spawn point), and never over scenery. In his
                // arena they stay as its scenery; anywhere else they are cleared with him (onRemoved).
                BlockPos c = arenaCentre(boss);
                boolean scenery = inOwnArena(boss);
                ServerLevel w = BossFx.world(boss);
                for (int[] o : new int[][]{{-5, -4}, {5, -4}, {-5, 4}, {5, 4}}) {
                    for (int y = 0; y < 3; y++) {
                        BlockPos at = c.offset(o[0], y, o[1]);
                        if (w.getBlockState(at).isAir()) {
                            w.setBlockAndUpdate(at, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
                            if (!scenery) {
                                pillarBlocks.add(at.immutable());
                            }
                        }
                    }
                }
                BossFx.say(boss, "Duke Sucellus stirs from his slumber...");
            }
            if (gazeAt >= 0) {
                BossFx.burst(boss, ParticleTypes.SNOWFLAKE, 10, 1.5);
                if (now >= gazeAt) {
                    gazeAt = -1;
                    cooldown = Math.max(cooldown, 1); // the gaze takes 6 ticks in all
                    int max = (int) Math.round(maxHit(boss, Dt2Bosses.DUKE_GAZE) * scale);
                    for (ServerPlayer p : BossFx.players(boss, 24)) {
                        if (boss.hasLineOfSight(p)) {
                            BossFx.stun(p, 6);
                            CombatHooks.nameAttack("freezing_gaze", null);
                            CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(max + 1));
                            Msg.red(p, "The Duke's freezing gaze locks onto you!");
                        } else {
                            Msg.game(p, "You manage to avoid Duke Sucellus' gaze.");
                        }
                    }
                }
            }
            if (target != null && boss.awakened() && percent(boss) <= BossMechanics.DUKE_ORB_PERCENT && --orbTimer <= 0) {
                orbTimer = BossMechanics.DUKE_ORB_INTERVAL;
                BossFx.warn(boss, "An orb of darkness rolls out in front of Duke Sucellus!");
                namedStrike("orb_of_darkness", boss, target.position(), 3, (int) Math.round(BossMechanics.DUKE_ORB_MAX * scale), 1.3,
                        ParticleTypes.SQUID_INK, (hit, pos) -> {
                            for (ServerPlayer h : hit) {
                                for (com.runecraft.osrs.Skill s : new com.runecraft.osrs.Skill[]{com.runecraft.osrs.Skill.ATTACK,
                                        com.runecraft.osrs.Skill.STRENGTH, com.runecraft.osrs.Skill.DEFENCE,
                                        com.runecraft.osrs.Skill.RANGED, com.runecraft.osrs.Skill.MAGIC}) {
                                    BossFx.drainStat(h, s, BossMechanics.DUKE_ORB_DRAIN);
                                }
                            }
                        });
            }
        }

        /** Discarded, killed or reset away from his arena: the pillars raised around him go too. */
        @Override
        public void onRemoved(DataBossEntity boss) {
            ServerLevel w = BossFx.world(boss);
            for (BlockPos at : pillarBlocks) {
                if (w.getBlockState(at).is(Blocks.CHISELED_DEEPSLATE)) {
                    w.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                }
            }
            pillarBlocks.clear();
        }

        /** The pillars outlive a chunk unload: a reloaded Duke neither raises a second set nor forgets to clear his own. */
        @Override
        public void writeNbt(DataBossEntity boss, CompoundTag nbt) {
            if (pillars) {
                nbt.putBoolean(NBT_PILLARS_UP, true);
            }
            if (!pillarBlocks.isEmpty()) {
                nbt.putLongArray(NBT_PILLARS, pillarBlocks.stream().mapToLong(BlockPos::asLong).toArray());
            }
        }

        @Override
        public void readNbt(DataBossEntity boss, CompoundTag nbt) {
            pillars = nbt.getBoolean(NBT_PILLARS_UP);
            pillarBlocks.clear();
            for (long at : nbt.getLongArray(NBT_PILLARS)) {
                pillarBlocks.add(BlockPos.of(at));
            }
        }
    }

    /**
     * The Leviathan: volleys of 3-6 projectiles, each announced, of boulders (ranged), orbs (magic) and, from the third
     * volley, shadows (melee), 3 ticks apart at first and faster later (OSRS Wiki) - switch prayers for every one. An
     * awakened Leviathan sends a tornado after the player below 50%. Debris and lightning fall between volleys; below 20%
     * it enrages: magic and ranged only, 2 ticks apart (awakened 1).
     */
    static final class Leviathan extends ScriptedBoss {
        private final List<AttackDef> volley = new ArrayList<>();
        private int pause;
        /** Game ticks left of a shadow-spell stun (OSRS Wiki: 15 ticks, halved when awakened). */
        private int stunned;
        /** Volleys fired so far (they speed up) and the ticks between this volley's projectiles. */
        private int volleys;
        private int spacing = 3;
        /** The awakened tornado's position, or null between tornadoes. */
        private Vec3 tornado;
        private int tornadoPause;

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
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
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
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (volley.isEmpty()) {
                int n = (boss.awakened() ? 5 : 3) + boss.getRandom().nextInt(4); // approx: awakened volleys are longer
                boolean enraged = percent(boss) < 20;
                // The volleys grow faster and mix in the melee shadow as the fight goes on (OSRS Wiki); enraged: magic/ranged.
                AttackDef[] styles = BossMechanics.leviathanVolleyMelee(volleys, enraged)
                        ? new AttackDef[]{Dt2Bosses.LEVI_RANGED, Dt2Bosses.LEVI_MAGIC, Dt2Bosses.LEVI_MELEE}
                        : new AttackDef[]{Dt2Bosses.LEVI_RANGED, Dt2Bosses.LEVI_MAGIC};
                spacing = BossMechanics.leviathanVolleyTicks(volleys, enraged, boss.awakened());
                volleys++;
                for (int i = 0; i < n; i++) {
                    volley.add(styles[boss.getRandom().nextInt(styles.length)]);
                }
                BossFx.warn(boss, "The Leviathan prepares a barrage of " + n + " projectiles!");
                for (int i = 0; i < 3; i++) {
                    strike(boss, around(boss, target.position(), 5), 3, (int) Math.round(maxHit(boss, Dt2Bosses.LEVI_DEBRIS) * scale), 1.3, ParticleTypes.ASH);
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
            return percent(boss) < 20 ? BossMechanics.leviathanVolleyTicks(volleys, true, boss.awakened()) : spacing;
        }

        /**
         * Awakened, from 50% health: a tornado drifts after the player and hits for 40+ on contact (OSRS Wiki), then reforms
         * at the edge of the arena (approx: its speed and the pause after a hit).
         */
        private void tickTornado(DataBossEntity boss, ServerPlayer target) {
            if (!boss.awakened() || target == null || percent(boss) > 50) {
                return;
            }
            ServerLevel w = BossFx.world(boss);
            if (tornado == null) {
                if (--tornadoPause > 0) {
                    return;
                }
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                tornado = BossFx.ground(w, homeVec().x + Math.cos(a) * 10, homeVec().z + Math.sin(a) * 10, homeVec().y);
                BossFx.warn(boss, "A tornado forms in the cavern!");
            }
            Vec3 d = target.position().subtract(tornado);
            double len = d.horizontalDistance();
            if (len <= 0.9) {
                CombatHooks.nameAttack("tornado", null);
                CombatHandler.applyDamage(boss, target, (int) Math.round((BossMechanics.LEVIATHAN_TORNADO_MIN
                        + boss.getRandom().nextInt(BossMechanics.LEVIATHAN_TORNADO_MAX - BossMechanics.LEVIATHAN_TORNADO_MIN + 1)) * scale));
                Msg.red(target, "The tornado tears into you!");
                tornado = null;
                tornadoPause = 5;
                return;
            }
            double step = Math.min(0.5, len); // approx: half a tile a tick, slower than a running player
            tornado = BossFx.ground(w, tornado.x + d.x / len * step, tornado.z + d.z / len * step, tornado.y);
            w.sendParticles(ParticleTypes.CLOUD, tornado.x, tornado.y + 0.5, tornado.z, 8, 0.3, 0.8, 0.3, 0.02);
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            tickTornado(boss, target);
            if (stunned > 0) {
                stunned--;
                BossFx.burst(boss, ParticleTypes.SMOKE, 6, 1.5);
                return;
            }
            if (target != null && boss.getRandom().nextInt(boss.awakened() ? 18 : 30) == 0) {
                strike(boss, target.position(), 3, (int) Math.round(maxHit(boss, Dt2Bosses.LEVI_LIGHTNING) * scale), 1.3, ParticleTypes.ELECTRIC_SPARK);
                BossFx.warn(boss, "The Leviathan's shell crackles with lightning!");
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
        private final List<Mob> spores = new ArrayList<>();
        private long strangleDue = -1;
        private ServerPlayer strangled;
        private boolean lastStrangle;
        /** Strangles so far this fight (each adds a spore, up to six). */
        private int strangles;
        /** Head gazes in the air: {game tick it lands, 1 for the blue magic gaze / 0 for the green ranged one}. */
        private final List<long[]> gazes = new ArrayList<>();
        private ServerPlayer gazed;

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
        protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
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
        protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
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
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (damage > 1) {
                boss.heal(mcHp(boss, damage / 2)); // heals 50% of the damage he deals, rounded down
            }
            return damage;
        }

        /**
         * Swinging axes: each axe hits a player once for up to 35 (awakened 96; Protect from Melee halves it) and
         * starts a bleed (3 every 3 ticks for 14 ticks, every tick while running; awakened 5 a hit). Awakened axes also hit
         * whoever stands where they appear, and that hit does not bleed (OSRS Wiki "Vardorvis/Strategies").
         */
        private void axes(DataBossEntity boss, ServerPlayer target) {
            int n = BossMechanics.vardorvisAxes(hp(boss), boss.awakened());
            BossFx.warn(boss, n == 1 ? "An axe swings out from the walls!" : n + " axes swing out from the walls!");
            Vec3 p = target.position();
            Vec3 toward = p.subtract(boss.position());
            double base = Math.atan2(toward.z, toward.x);
            int max = (int) Math.round((boss.awakened() ? BossMechanics.VARDORVIS_AXE_MAX_AWAKENED : BossMechanics.VARDORVIS_AXE_MAX)
                    * scale);
            boolean awake = boss.awakened();
            int bleed = awake ? BossMechanics.VARDORVIS_BLEED_DAMAGE_AWAKENED : BossMechanics.VARDORVIS_BLEED_DAMAGE;
            for (int a = 0; a < n; a++) {
                double ang = base + a * Math.PI / 4 * (a % 2 == 0 ? 1 : -1);
                Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                Set<UUID> struck = new HashSet<>();
                if (awake) {
                    // The axe hits on appearing at the start of its path (no bleed from this hit).
                    Vec3 from = p.add(dir.scale(-4 * 1.5));
                    for (ServerPlayer h : BossFx.players(boss, 24)) {
                        if (h.position().distanceToSqr(from.x, h.getY(), from.z) <= 1.1 * 1.1) {
                            int roll = boss.getRandom().nextInt(max + 1);
                            if (BossFx.protects(h, Prayer.PROTECT_FROM_MELEE)) {
                                roll /= 2;
                            }
                            CombatHooks.nameAttack("swinging_axe", null);
                            CombatHandler.applyDamage(boss, h, roll);
                            struck.add(h.getUUID());
                        }
                    }
                    BossFx.world(boss).sendParticles(ParticleTypes.SWEEP_ATTACK, from.x, p.y + 1.0, from.z, 3, 0.4, 0.3, 0.4, 0);
                }
                for (int i = -4; i <= 4; i++) {
                    Vec3 at = p.add(dir.scale(i * 1.5));
                    namedStrike("swinging_axe", boss, BossFx.ground(BossFx.world(boss), at.x, at.z, p.y), 3, 0, 1.1,
                            ParticleTypes.SWEEP_ATTACK, (hit, pos) -> {
                                for (ServerPlayer h : hit) {
                                    if (!struck.add(h.getUUID())) {
                                        continue;
                                    }
                                    int roll = boss.getRandom().nextInt(max + 1);
                                    if (BossFx.protects(h, Prayer.PROTECT_FROM_MELEE)) {
                                        roll /= 2;
                                    }
                                    CombatHooks.nameAttack("swinging_axe", null);
                                    CombatHandler.applyDamage(boss, h, roll);
                                    BossDots.bleed(boss, h, bleed);
                                }
                            });
                }
            }
        }

        /**
         * Strangle (below 570 hitpoints, never twice in a row): tendrils root the target and spores appear around them; any
         * spore left after 5 ticks (awakened 4) tightens the tendrils for ~25 (awakened ~40).
         */
        private void strangle(DataBossEntity boss, ServerPlayer target) {
            boolean awake = boss.awakened();
            int ticks = awake ? BossMechanics.VARDORVIS_STRANGLE_TICKS_AWAKENED : BossMechanics.VARDORVIS_STRANGLE_TICKS;
            BossFx.warn(boss, "Vardorvis strikes the ground and tendrils entangle you - destroy the spores!");
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    ticks * 12, 10, false, true));
            spores.forEach(Mob::discard);
            spores.clear();
            int count = BossMechanics.vardorvisStrangleSpores(strangles++);
            for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count + boss.getRandom().nextDouble();
                Vec3 at = BossFx.ground(BossFx.world(boss), target.getX() + Math.cos(a) * 1.6, target.getZ() + Math.sin(a) * 1.6,
                        target.getY());
                Mob spore = BossFx.spawnMinion(boss, BossMechanics.STRANGLER_SPORE, at, null);
                if (spore != null) {
                    MinionRules.set(spore, new MinionRules.Rule() {
                        @Override
                        public double accuracy(ServerPlayer pl, net.minecraft.world.entity.LivingEntity m, AttackType t) {
                            return MinionRules.ALWAYS_HITS;
                        }

                        @Override
                        public int damage(ServerPlayer pl, net.minecraft.world.entity.LivingEntity m, AttackType t, int d) {
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
            boolean left = spores.stream().anyMatch(Mob::isAlive);
            spores.forEach(Mob::discard);
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
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            spores.forEach(Mob::discard);
            spores.clear();
            gazes.clear(); // a gaze still in the air when he dies does nothing
        }

        /**
         * Head gaze (OSRS Wiki "Vardorvis' Head"): a projectile that deals no damage; unblocked it drains 10 Prayer and keeps
         * overhead prayers off for 3 ticks. Post-quest it's the green (ranged) gaze; awakened also the blue (magic) one, in
         * pairs below half health. He holds his melee while it flies and attacks 2 ticks after it lands.
         */
        private void gaze(DataBossEntity boss, ServerPlayer target) {
            boolean[] magic = BossMechanics.vardorvisGazes(boss.awakened(), hpFraction(boss), boss.getRandom().nextBoolean());
            Msg.red(target, "Vardorvis' head gazes upon you...");
            for (int i = 0; i < magic.length; i++) {
                gazes.add(new long[]{now + BossMechanics.VARDORVIS_HEAD_FLIGHT_TICKS + i, magic[i] ? 1 : 0});
            }
            gazed = target;
            BossFx.burst(boss, magic[0] ? ParticleTypes.WITCH : ParticleTypes.HAPPY_VILLAGER, 12, 1.0);
            cooldown = Math.max(cooldown, BossMechanics.VARDORVIS_HEAD_FLIGHT_TICKS + magic.length - 1
                    + BossMechanics.VARDORVIS_HEAD_AFTER_TICKS);
        }

        private void tickGazes() {
            Iterator<long[]> it = gazes.iterator();
            while (it.hasNext()) {
                long[] g = it.next();
                if (g[0] > now) {
                    continue;
                }
                it.remove();
                ServerPlayer p = gazed;
                if (p == null || !p.isAlive()) {
                    continue;
                }
                Prayer block = g[1] == 1 ? Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MISSILES;
                BossFx.world(p).sendParticles(g[1] == 1 ? ParticleTypes.WITCH : ParticleTypes.HAPPY_VILLAGER,
                        p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                if (!BossFx.protects(p, block)) {
                    BossFx.drainPrayer(p, BossMechanics.VARDORVIS_HEAD_PRAYER_DRAIN);
                    BossDots.lockOverheads(p, BossMechanics.VARDORVIS_HEAD_LOCK_TICKS);
                    Msg.red(p, "The gaze disables your overhead prayers!");
                }
            }
        }

        private void darts(DataBossEntity boss, ServerPlayer target) {
            int n = BossMechanics.vardorvisDarts(hp(boss));
            BossFx.warn(boss, "Vardorvis darts around you, leaving cracks in the ground!");
            int max = (int) Math.round(maxHit(boss, Dt2Bosses.VARD_SPIKES) * scale); // 25, awakened 40
            for (int i = 0; i < n; i++) {
                Vec3 at = i == 0 ? target.position() : around(boss, target.position(), 3);
                namedStrike("darting_spikes", boss, at, 4 + i, max, 1.3, ParticleTypes.DAMAGE_INDICATOR, (hit, pos) -> {
                    for (ServerPlayer h : hit) {
                        boss.heal(mcHp(boss, max / 4.0)); // heals half the (average) damage dealt
                    }
                });
            }
            cooldown += n; // each dart delays his next auto-attack by a tick
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
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
            if (hp < BossMechanics.VARDORVIS_HEAD_HP && gazes.isEmpty() && boss.getRandom().nextInt(enraged ? 3 : 6) == 0) { // approx rate
                gaze(boss, target);
                return null;
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
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            // Defence follows his hitpoints (defence-draining specials make no difference on top).
            int lost = (int) Math.round((1 - hpFraction(boss)) * BossMechanics.VARDORVIS_HITPOINTS);
            com.runecraft.content.systems.SysCombat.drain(boss).defence = BossMechanics.vardorvisDefenceReduction(lost);
            tickStrangle(boss);
            tickGazes();
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

    /**
     * The Hueycoatl (OSRS Wiki "The Hueycoatl/Strategies"). Phase one: five body parts (250 hitpoints each, they don't
     * attack) block the way; the head can't be harmed until all are down. Phase two: the head; the seer charges the three
     * pillars from the players' protection prayers (Melee/Missiles/Magic, enough of each for the team size) and, when its bar
     * fills with all three charged, the braziers light: +30% damage for everyone until a check finds a pillar draining. At
     * 50% she shields herself and her tail (300 hitpoints, hits capped at 4, or 9 with crush as the best attack bonus)
     * must be broken; it changes sides every ~20 s with a 20-25 shockwave. Final phase: the head again, attacking faster,
     * with a shockwave every ~15 s. Throughout: glowing symbols (lightning) and the coloured feather volleys.
     * approx: the body parts stand in an arc in front of her rather than along her coils; the shockwave is a dodgeable
     * strike at the player's tile.
     */
    static final class Hueycoatl extends ScriptedBoss {
        private static final int BODY = 1;
        private static final int HEAD = 2;
        private static final int TAIL = 3;
        private static final int FINAL = 4;
        private int stage;
        private final List<Mob> segments = new ArrayList<>();
        private Mob tail;
        private boolean lit;
        private int seerTimer = BossMechanics.HUEY_SEER_CHARGE_TICKS;
        private int shockTimer;
        /**
         * Body parts and tail saved with her when she was unloaded mid-fight (the reloaded boss has a fresh script): found
         * again by uuid as their chunks load, then adopted while her stage still needs them and discarded otherwise.
         */
        private final List<UUID> lostSegments = new ArrayList<>();
        private UUID lostTail;
        /** Game ticks left to find the saved minions before the fight carries on without them. */
        private int relinkTicks;
        private static final int RELINK_GRACE_TICKS = 20; // approx: time for the lair's chunks to load
        private static final String NBT_STAGE = "HueyStage";
        private static final String NBT_SEGMENT = "HueySegment";
        private static final String NBT_TAIL = "HueyTail";

        @Override
        public void onSpawn(DataBossEntity boss) {
            home = boss.blockPosition();
            spawnSegments(boss);
        }

        @Override
        public void writeNbt(DataBossEntity boss, CompoundTag nbt) {
            if (stage == 0) {
                return;
            }
            nbt.putInt(NBT_STAGE, stage);
            nbt.putBoolean("HueyLit", lit);
            nbt.putInt("HueySeer", seerTimer);
            nbt.putInt("HueyShock", shockTimer);
            // Not dead rather than alive: a minion whose chunk was saved just before hers is already removed but comes back.
            int i = 0;
            for (Mob m : segments) {
                if (!m.isDeadOrDying()) {
                    nbt.putUUID(NBT_SEGMENT + i++, m.getUUID());
                }
            }
            for (UUID id : lostSegments) {
                nbt.putUUID(NBT_SEGMENT + i++, id);
            }
            UUID tailId = tail != null && !tail.isDeadOrDying() ? tail.getUUID() : lostTail;
            if (tailId != null) {
                nbt.putUUID(NBT_TAIL, tailId);
            }
        }

        @Override
        public void readNbt(DataBossEntity boss, CompoundTag nbt) {
            if (!nbt.contains(NBT_STAGE)) {
                return;
            }
            stage = nbt.getInt(NBT_STAGE);
            lit = nbt.getBoolean("HueyLit");
            seerTimer = nbt.getInt("HueySeer");
            shockTimer = nbt.getInt("HueyShock");
            lostSegments.clear();
            for (int i = 0; nbt.hasUUID(NBT_SEGMENT + i); i++) {
                lostSegments.add(nbt.getUUID(NBT_SEGMENT + i));
            }
            lostTail = nbt.hasUUID(NBT_TAIL) ? nbt.getUUID(NBT_TAIL) : null;
            relinkTicks = RELINK_GRACE_TICKS;
        }

        /** Looks for the minions saved before an unload: adopted while the stage still needs them, discarded otherwise. */
        private void relink(DataBossEntity boss) {
            ServerLevel w = BossFx.world(boss);
            java.util.Iterator<UUID> it = lostSegments.iterator();
            while (it.hasNext()) {
                if (w.getEntity(it.next()) instanceof Mob m) {
                    it.remove();
                    if (stage == BODY && m.isAlive() && segments.size() < BossMechanics.HUEY_BODY_SEGMENTS) {
                        segments.add(m);
                    } else {
                        m.discard();
                    }
                }
            }
            if (lostTail != null && w.getEntity(lostTail) instanceof Mob m) {
                lostTail = null;
                if (stage == TAIL && tail == null && m.isAlive()) {
                    tail = m;
                    capTail(m);
                } else {
                    m.discard();
                }
            }
        }

        private void spawnSegments(DataBossEntity boss) {
            segments.forEach(Mob::discard);
            segments.clear();
            Vec3 c = boss.position();
            for (int i = 0; i < BossMechanics.HUEY_BODY_SEGMENTS; i++) {
                double a = Math.PI * 0.6 + i * Math.PI * 0.2;
                Mob m = BossFx.spawnMinion(boss, BossMechanics.HUEYCOATL_BODY,
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 6, c.z + Math.sin(a) * 6, c.y), null);
                if (m != null) {
                    segments.add(m);
                }
            }
            stage = BODY;
            BossFx.say(boss, "The Hueycoatl's coils block the way to the summit. Cut through her body!");
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return stage == HEAD || stage == FINAL;
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected int attackDelay(DataBossEntity boss, AttackDef attack) {
            int base = super.attackDelay(boss, attack);
            return stage == FINAL ? Math.max(2, base - 2) : base; // approx: "significantly" faster in the final phase
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (attackCount % 6 == 5 || (stage == FINAL && attackCount % 3 == 2)) {
                BossFx.warn(boss, "Glowing symbols appear around you!");
                for (int i = 0; i < 5; i++) {
                    namedStrike("thunderstorm", boss, i == 0 ? target.position() : around(boss, target.position(), 5), 4,
                            maxHit(boss, VarlamoreBosses.HUEY_LIGHTNING), 1.4, ParticleTypes.ELECTRIC_SPARK);
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

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (!isVulnerable(boss)) {
                if (attacker != null) {
                    Msg.action(attacker, stage == BODY ? "The Hueycoatl is out of reach. Cut through her body first."
                            : "The Hueycoatl's tail shields her!");
                }
                return 0;
            }
            int d = lit && damage > 0 ? (int) Math.floor(damage * BossMechanics.HUEY_BRAZIER_DAMAGE) : damage;
            if (lit && d > 0 && mcHp(boss, d) >= boss.getHealth()) {
                // "Pillar Lover": the killing blow lands while the braziers are lit.
                CombatAchievementService.flagNear(boss, BossMechanics.FLAG_HUEY_VULNERABLE_KILL);
            }
            return d;
        }

        /** The seer's bar: every player near her counts towards the team size and the pillar their protection prayer charges. */
        void seerCheck(DataBossEntity boss) {
            List<ServerPlayer> team = BossFx.players(boss, 32);
            int melee = 0;
            int missiles = 0;
            int magic = 0;
            for (ServerPlayer p : team) {
                if (BossFx.protects(p, Prayer.PROTECT_FROM_MELEE)) {
                    melee++;
                } else if (BossFx.protects(p, Prayer.PROTECT_FROM_MISSILES)) {
                    missiles++;
                } else if (BossFx.protects(p, Prayer.PROTECT_FROM_MAGIC)) {
                    magic++;
                }
            }
            boolean charged = !team.isEmpty() && BossMechanics.hueyPillarsCharged(melee, missiles, magic, team.size());
            if (charged != lit) {
                lit = charged;
                for (ServerPlayer p : team) {
                    if (lit) {
                        Msg.game(p, "The braziers atop the pillars ignite! The Hueycoatl is vulnerable.");
                    } else {
                        Msg.red(p, "The braziers are extinguished.");
                    }
                }
            }
        }

        private void shockwave(DataBossEntity boss, ServerPlayer target) {
            BossFx.warn(boss, "The Hueycoatl's tail slams the ground!");
            for (ServerPlayer p : BossFx.players(boss, 32)) {
                namedStrike("tail_shockwave", boss, p.position(), 3, 0, 1.2, ParticleTypes.CLOUD, (hit, pos) -> {
                    for (ServerPlayer h : hit) {
                        CombatHooks.nameAttack("tail_shockwave", null);
                        CombatHandler.applyDamage(boss, h, BossMechanics.HUEY_SHOCKWAVE_MIN
                                + h.getRandom().nextInt(BossMechanics.HUEY_SHOCKWAVE_MAX - BossMechanics.HUEY_SHOCKWAVE_MIN + 1));
                    }
                });
            }
        }

        private void spawnTail(DataBossEntity boss, ServerPlayer target) {
            Vec3 c = boss.position();
            double a = boss.getRandom().nextBoolean() ? 0 : Math.PI;
            tail = BossFx.spawnMinion(boss, BossMechanics.HUEYCOATL_TAIL,
                    BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 5, c.z + Math.sin(a) * 5, c.y), null);
            if (tail != null) {
                capTail(tail);
            }
        }

        /** Hits on the tail cap at 4, or 9 with crush as the attacker's best attack bonus. */
        private static void capTail(Mob tail) {
            MinionRules.set(tail, new MinionRules.Rule() {
                @Override
                public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType t, int d) {
                    com.runecraft.osrs.EquipmentStats g = com.runecraft.combat.HitCalcBridge.gearStats(p);
                    boolean crushBest = g.crushAttack() > g.stabAttack() && g.crushAttack() > g.slashAttack()
                            && g.crushAttack() > g.magicAttack() && g.crushAttack() > g.rangedAttack();
                    return BossMechanics.hueyTailDamage(d, crushBest);
                }
            });
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (stage == 0) {
                spawnSegments(boss);
            }
            if (relinkTicks > 0) {
                relink(boss);
                if ((!lostSegments.isEmpty() || lostTail != null) && --relinkTicks > 0) {
                    return; // give the saved minions' chunks time to load before judging the stage
                }
                relinkTicks = 0;
                // Saved minions that never turned up (e.g. despawned): bring back what the stage still needs.
                if (stage == BODY && segments.isEmpty() && !lostSegments.isEmpty()) {
                    spawnSegments(boss);
                }
                if (stage == TAIL && tail == null && lostTail != null) {
                    spawnTail(boss, target);
                }
            } else if (!lostSegments.isEmpty() || lostTail != null) {
                relink(boss); // late arrivals: discarded unless the stage still needs them
            }
            if (stage == BODY) {
                segments.removeIf(m -> !m.isAlive());
                if (segments.isEmpty()) {
                    stage = HEAD;
                    BossFx.say(boss, "The Hueycoatl's tail withdraws: the path to the summit is open!");
                }
            }
            if (stage == HEAD || stage == TAIL || stage == FINAL) {
                if (--seerTimer <= 0) {
                    seerTimer = BossMechanics.HUEY_SEER_CHARGE_TICKS;
                    seerCheck(boss);
                }
            }
            if (stage == HEAD && percent(boss) <= 50) {
                stage = TAIL;
                shockTimer = BossMechanics.HUEY_TAIL_SWAP_TICKS;
                spawnTail(boss, target);
                BossFx.say(boss, "The Hueycoatl shields herself and her tail bursts from the cave!");
            }
            if (stage == TAIL) {
                if (tail == null || !tail.isAlive()) {
                    tail = null;
                    stage = FINAL;
                    shockTimer = BossMechanics.HUEY_FINAL_SHOCKWAVE_TICKS;
                    BossFx.say(boss, "The tail breaks! The Hueycoatl is exposed once more.");
                } else if (--shockTimer <= 0 && target != null) {
                    // The tail changes sides and slams out a shockwave.
                    shockTimer = BossMechanics.HUEY_TAIL_SWAP_TICKS;
                    Vec3 c = boss.position();
                    Vec3 now0 = tail.position();
                    tail.teleportTo(2 * c.x - now0.x, now0.y, 2 * c.z - now0.z);
                    shockwave(boss, target);
                }
            }
            if (stage == FINAL && target != null && --shockTimer <= 0) {
                shockTimer = BossMechanics.HUEY_FINAL_SHOCKWAVE_TICKS;
                shockwave(boss, target);
            }
            if (lit) {
                BossFx.ring(BossFx.world(boss), boss.position(), 2.5, ParticleTypes.FLAME);
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            segments.forEach(Mob::discard);
            segments.clear();
            if (tail != null) {
                tail.discard();
            }
            ServerLevel w = BossFx.world(boss);
            if (lostTail != null) {
                lostSegments.add(lostTail);
                lostTail = null;
            }
            for (UUID id : lostSegments) {
                var e = w.getEntity(id);
                if (e != null) {
                    e.discard();
                }
            }
            lostSegments.clear();
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            onDeath(boss, null);
        }
    }

    /**
     * Self-test (boss:hueycoatl_phases): the head can't be harmed while her body parts stand; with them down she is; a
     * team of three praying all three protections lights the braziers.
     */
    static String selfTestHueycoatl(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Hueycoatl h)) {
            throw new IllegalStateException("the Hueycoatl has no Hueycoatl script");
        }
        h.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
        if (h.segments.size() != BossMechanics.HUEY_BODY_SEGMENTS) {
            throw new IllegalStateException(h.segments.size() + " body parts, expected " + BossMechanics.HUEY_BODY_SEGMENTS);
        }
        if (h.isVulnerable(boss)) {
            throw new IllegalStateException("the head is vulnerable while her body parts stand");
        }
        h.segments.forEach(m -> m.setHealth(0f));
        h.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
        if (!h.isVulnerable(boss)) {
            throw new IllegalStateException("the head is still protected with every body part down");
        }
        h.seerCheck(boss);
        boolean soloLit = h.lit;
        return "body parts block the head, then open it; solo braziers lit: " + soloLit;
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
            final List<Vec3> cells = new ArrayList<>();
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

        private record Elemental(Mob mob, boolean fire, long explodeAt) {
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
        private Vec3 safe;
        private boolean subdued;
        private int subduedCharge = -2;
        private int eldricDeadCharge = -3;
        private boolean eldricDead;
        private MinionRules.AttackWatcher watcher;
        /**
         * Wall blocks saved with Branda when she was unloaded mid-fight (the reloaded boss has a fresh script with no
         * walls): cleared on her first game tick back so they never stay in the shared lair. Null when there are none.
         */
        private long[] savedBlocks;
        private String savedWorld;
        private static final String NBT_WALL_BLOCKS = "TitanWallBlocks";
        private static final String NBT_WALL_WORLD = "TitanWallWorld";

        @Override
        public void writeNbt(DataBossEntity boss, CompoundTag nbt) {
            List<Long> all = new ArrayList<>();
            for (Wall wall : walls) {
                for (List<BlockPos> placed : wall.blocks.values()) {
                    for (BlockPos pos : placed) {
                        all.add(pos.asLong());
                    }
                }
            }
            if (savedBlocks != null) {
                for (long pos : savedBlocks) {
                    all.add(pos);
                }
            }
            if (!all.isEmpty()) {
                nbt.putLongArray(NBT_WALL_BLOCKS, all.stream().mapToLong(Long::longValue).toArray());
                nbt.putString(NBT_WALL_WORLD, boss.level().dimension().location().toString());
            }
        }

        @Override
        public void readNbt(DataBossEntity boss, CompoundTag nbt) {
            if (nbt.contains(NBT_WALL_BLOCKS)) {
                savedBlocks = nbt.getLongArray(NBT_WALL_BLOCKS);
                savedWorld = nbt.getString(NBT_WALL_WORLD);
            }
        }

        /** Turns the walls saved before an unload back into air (only in the world they were saved in). */
        private void clearSavedWalls(ServerLevel w) {
            if (w.dimension().location().toString().equals(savedWorld)) {
                for (long pos : savedBlocks) {
                    clearWallBlock(w, BlockPos.of(pos));
                }
            }
            savedBlocks = null;
            savedWorld = null;
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            ServerPlayer target = boss.getTarget() instanceof ServerPlayer p ? p : null;
            eldric = BossFx.spawnBoss(boss, "eldric_the_ice_king", boss.blockPosition().offset(5, 0, 0), target);
            BossFx.say(boss, "Branda the Fire Queen and Eldric the Ice King rise! Douse the fires and melt the icicles.");
            watcher = (p, t, type, d) -> {
                if (!boss.isRemoved() && type == AttackType.MAGIC && p.distanceToSqr(boss) < 32 * 32) {
                    String spell = MinionRules.spell(p);
                    if (BossMechanics.dousesFire(spell)) {
                        clearWall(boss, p.position(), true);
                    } else if (BossMechanics.meltsIce(spell)) {
                        clearWall(boss, p.position(), false);
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
        private void clearWall(DataBossEntity boss, Vec3 from, boolean fire) {
            Wall best = null;
            int bestCell = -1;
            double bestD = 12 * 12;
            for (Wall w : walls) {
                if (w.fire != fire) {
                    continue;
                }
                for (int i = 0; i < w.cells.size(); i++) {
                    double d = w.cells.get(i).distanceToSqr(from);
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
                Vec3 c = best.cells.get(i);
                BossFx.world(boss).sendParticles(fire ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, c.x, c.y + 0.6, c.z, 8, 0.3, 0.4, 0.3, 0.02);
            }
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return subdued || charging() ? IDLE_MELEE : null;
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return !subdued;
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
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
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            ServerLevel w = BossFx.world(boss);
            if (savedBlocks != null) {
                clearSavedWalls(w);
            }
            if (!eldricDead && eldric != null && !eldric.isAlive()) {
                eldricDead = true;
                eldricDeadCharge = charging() ? chargeId : -3;
                if (subdued) {
                    if (subduedCharge == eldricDeadCharge) {
                        CombatAchievementService.flagNear(boss, BossMechanics.FLAG_TITANS_CHARGE_KILL);
                    }
                    ServerPlayer killer = eldric.getLastHurtByMob() instanceof ServerPlayer p ? p : target;
                    if (killer != null) {
                        boss.setHealth(0f);
                        boss.die(CombatHandler.source(killer));
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

        private void phaseTwoTick(DataBossEntity boss, ServerLevel w, ServerPlayer target) {
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
                    for (ServerPlayer p : BossFx.players(boss, 24)) {
                        if (safe == null || ModernScriptsAccess.flat(p.position(), safe) > 1.5) {
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

        private void conjureWall(DataBossEntity boss, ServerLevel w, boolean fire) {
            Vec3 c = homeVec().add(2.5, 0, 0);
            double x = c.x + (boss.getRandom().nextInt(9) - 4) * 2;
            Wall wall = new Wall(fire, now + BossMechanics.TITAN_WALL_FORM_TICKS);
            for (int i = -2; i <= 2; i++) {
                wall.cells.add(BossFx.ground(w, x, c.z + i * 2, c.y));
            }
            wall.gone = new boolean[wall.cells.size()];
            walls.add(wall);
        }

        private void tickWalls(DataBossEntity boss, ServerLevel w) {
            List<ServerPlayer> players = BossFx.players(boss, 24);
            java.util.Iterator<Wall> it = walls.iterator();
            while (it.hasNext()) {
                Wall wall = it.next();
                boolean any = false;
                for (int i = 0; i < wall.cells.size(); i++) {
                    if (wall.gone[i]) {
                        continue;
                    }
                    any = true;
                    Vec3 c = wall.cells.get(i);
                    w.sendParticles(wall.fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, c.x, c.y + (wall.formed ? 0.8 : 0.1), c.z,
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
                        Vec3 c = wall.cells.get(i);
                        for (ServerPlayer p : players) {
                            if (ModernScriptsAccess.flat(p.position(), c) < 1.1) {
                                CombatHooks.nameAttack(name, null);
                                CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(BossMechanics.TITAN_WALL_MAX + 1));
                                wall.gone[i] = true; // standing on the tile stops it forming
                            }
                        }
                        for (Elemental e : elementals) {
                            if (!wall.gone[i] && e.fire() != wall.fire && e.mob().isAlive() && ModernScriptsAccess.flat(e.mob().position(), c) < 1.3) {
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
                        for (ServerPlayer p : players) {
                            if (ModernScriptsAccess.flat(p.position(), wall.cells.get(i)) < 0.9) {
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
        private static void block(ServerLevel w, Wall wall, int cell) {
            Vec3 c = wall.cells.get(cell);
            List<BlockPos> placed = new ArrayList<>();
            net.minecraft.world.level.block.state.BlockState state = (wall.fire ? Blocks.MAGMA_BLOCK : Blocks.PACKED_ICE).defaultBlockState();
            int y = (int) Math.floor(c.y + 0.01);
            for (int bx : new int[]{(int) Math.floor(c.x - 0.5), (int) Math.floor(c.x + 0.5)}) {
                for (int bz : new int[]{(int) Math.floor(c.z - 0.5), (int) Math.floor(c.z + 0.5)}) {
                    for (int dy = 0; dy < 2; dy++) {
                        BlockPos pos = new BlockPos(bx, y + dy, bz);
                        if (placed.contains(pos) || !w.getBlockState(pos).isAir()
                                || !w.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(pos),
                                e -> e.isAlive()).isEmpty()) {
                            continue;
                        }
                        w.setBlockAndUpdate(pos, state);
                        placed.add(pos);
                    }
                }
            }
            wall.blocks.put(cell, placed);
        }

        private static void unblock(ServerLevel w, Wall wall, int cell) {
            List<BlockPos> placed = wall.blocks.remove(cell);
            if (placed == null) {
                return;
            }
            for (BlockPos pos : placed) {
                clearWallBlock(w, pos);
            }
        }

        private static void clearWallBlock(ServerLevel w, BlockPos pos) {
            if (w.getBlockState(pos).is(Blocks.MAGMA_BLOCK) || w.getBlockState(pos).is(Blocks.PACKED_ICE)) {
                w.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }

        private static void unblockAll(ServerLevel w, Wall wall) {
            for (Integer cell : new ArrayList<>(wall.blocks.keySet())) {
                unblock(w, wall, cell);
            }
        }

        private void summonElementals(DataBossEntity boss, ServerPlayer target) {
            elementals.removeIf(e -> !e.mob().isAlive());
            if (elementals.size() >= 6) {
                return;
            }
            for (int i = 0; i < 3; i++) {
                Mob f = BossFx.spawnMinion(boss, "titan_fire_elemental", around(boss, boss.position(), 3), target);
                if (f != null) {
                    elementals.add(new Elemental(f, true, now + 20)); // approx: when they self-destruct
                }
                if (eldricAlive()) {
                    Mob ice = BossFx.spawnMinion(boss, "titan_ice_elemental", around(boss, eldric.position(), 3), target);
                    if (ice != null) {
                        elementals.add(new Elemental(ice, false, now + 20));
                    }
                }
            }
            BossFx.warn(boss, "The Royal Titans summon elementals!");
        }

        private void tickElementals(DataBossEntity boss, ServerLevel w) {
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
                for (ServerPlayer p : BossFx.players(boss, 24)) {
                    if (ModernScriptsAccess.flat(p.position(), e.mob().position()) <= 3.0) {
                        CombatHooks.nameAttack("elemental_explosion", null);
                        CombatHandler.applyDamage(boss, p, BossMechanics.TITAN_ELEMENTAL_BLAST_MIN
                                + boss.getRandom().nextInt(BossMechanics.TITAN_ELEMENTAL_BLAST_MAX - BossMechanics.TITAN_ELEMENTAL_BLAST_MIN + 1));
                    }
                }
                w.sendParticles(ParticleTypes.EXPLOSION, e.mob().getX(), e.mob().getY() + 0.5, e.mob().getZ(), 1, 0, 0, 0, 0);
                e.mob().discard();
                it.remove();
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            if (charging() && eldricDead && eldricDeadCharge == chargeId) {
                CombatAchievementService.flagNear(boss, BossMechanics.FLAG_TITANS_CHARGE_KILL);
            }
            cleanUp(boss);
        }

        /** Discarded or killed without a player kill: the walls and elementals still go. */
        @Override
        public void onRemoved(DataBossEntity boss) {
            cleanUp(boss);
        }

        private void cleanUp(DataBossEntity boss) {
            elementals.forEach(e -> e.mob().discard());
            elementals.clear();
            if (watcher != null) {
                MinionRules.unwatch(watcher);
                watcher = null;
            }
            ServerLevel w = BossFx.world(boss);
            for (Wall wall : walls) {
                unblockAll(w, wall);
            }
            walls.clear();
            if (savedBlocks != null) {
                clearSavedWalls(w);
            }
        }
    }

    /** Small helpers shared by the scripts in this file. */
    private static final class ModernScriptsAccess {
        private ModernScriptsAccess() {
        }

        static double flat(Vec3 a, Vec3 b) {
            double dx = a.x - b.x;
            double dz = a.z - b.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        static void speed(Mob mob, double speed) {
            var attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null && attr.getBaseValue() != speed) {
                attr.setBaseValue(speed);
            }
        }

        static void stop(Mob mob) {
            speed(mob, 0.0);
            mob.getNavigation().stop();
        }
    }
}
