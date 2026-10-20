package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.ClassicBosses;
import com.runecraft.osrs.content.bosses.SkotizoRules;
import com.runecraft.skill.XpService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Scripts for the classic bosses (Mole, Kalphite Queen, Dagannoth Kings, Corp, Skotizo, Hespori, Scurrius, Zalcano, Sol). */
final class ClassicScripts {
    private ClassicScripts() {
    }

    /** Giant Mole: below 50% it may burrow away to another part of its lair, snuffing out light sources. */
    static final class GiantMole extends ScriptedBoss {
        private int burrowIn = -1;
        private int burrowCooldown;
        /** Game ticks left of the "just resurfaced" window (Whack-a-Mole: a kill within 10 seconds). */
        private int surfacedFor;

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            // OSRS Wiki: each attack has a 1/4 chance to make her burrow while she is between 50% and 5% of her hitpoints.
            double hp = percent(boss);
            if (hp < 50 && hp > 5 && burrowIn < 0 && burrowCooldown == 0 && boss.getRandom().nextInt(4) == 0) {
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
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (burrowCooldown > 0) {
                burrowCooldown--;
            }
            if (surfacedFor > 0 && --surfacedFor == 0) {
                CombatAchievementService.unflagNear(boss, CombatAchievements.FLAG_MOLE_SURFACED);
            }
            if (burrowIn > 0) {
                burrowIn--;
                BossFx.burst(boss, ParticleTypes.CAMPFIRE_COSY_SMOKE, 12, 1.0);
                if (burrowIn == 0) {
                    Vec3 to = around(boss, homeVec(), 10);
                    boss.teleportTo(to.x, to.y, to.z);
                    BossFx.sound(boss, SoundEvents.GRAVEL_BREAK, 0.6f);
                    for (ServerPlayer p : BossFx.players(boss, 24)) {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
                    }
                    BossFx.say(boss, "The Giant Mole digs away! Your light source is extinguished.");
                    CombatAchievementService.countNear(boss, CombatAchievements.COUNT_MOLE_BURROW);
                    CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_MOLE_SURFACED);
                    surfacedFor = 17; // 10 seconds
                    burrowIn = -1;
                    burrowCooldown = 15;
                }
            }
        }
    }

    /** Kalphite Queen (crawling): Protect from Magic and Missiles (high magic/ranged defence); at 0 hp she takes flight. */
    static final class KalphiteQueen extends ScriptedBoss {
        private boolean transforming;
        private int wait;

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (transforming) {
                return 0;
            }
            // Her protection prayers only raise her defence (OSRS Wiki), which her defence bonuses already model: no
            // extra damage reduction.
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
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (transforming) {
                BossFx.burst(boss, ParticleTypes.CRIT, 20, 1.2);
                if (--wait <= 0) {
                    BossFx.say(boss, "The Kalphite Queen emerges from her shell and takes to the air!");
                    BossFx.swapForm(boss, "kalphite_queen_airborne", boss.blockPosition(), true, null);
                }
            }
        }
    }

    /** Kalphite Queen (airborne): Protect from Melee (high melee defence, no damage cut); lightning chains to everyone nearby. */
    static final class KalphiteQueenAirborne extends ScriptedBoss {

        @Override
        protected String creditAs() {
            return "kalphite_queen";
        }

        /** A fresh form: Defence lowered on the grounded form no longer counts for "Chitin Penetrator". */
        @Override
        public void onSpawn(DataBossEntity boss) {
            CombatAchievementService.unflagNear(boss, CombatAchievements.FLAG_DEFENCE_LOWERED);
        }
    }

    /** Dagannoth Rex brings his brothers: Prime and Supreme spawn alongside him. */
    static final class DagannothRex extends ScriptedBoss {
        @Override
        public void onSpawn(DataBossEntity boss) {
            ServerPlayer target = boss.getTarget() instanceof ServerPlayer p ? p : null;
            // The lair keeps the Kings present (dungeons module): only wake the brothers that aren't already there.
            for (String brother : new String[]{"dagannoth_prime", "dagannoth_supreme"}) {
                net.minecraft.world.entity.EntityType<DataBossEntity> type = com.runecraft.registry.ModEntities.DATA_BOSS_TYPES.get(brother);
                if (type != null && !boss.level().getEntities(type, boss.getBoundingBox().inflate(48), e -> e.isAlive()).isEmpty()) {
                    continue;
                }
                BossFx.spawnBoss(boss, brother, boss.blockPosition().offset(brother.endsWith("prime") ? -4 : 4, 0, 3), target);
            }
            BossFx.say(boss, "The Dagannoth Kings rise! Rex resists melee, Prime resists magic and Supreme resists ranged.");
        }
    }

    /**
     * Corporeal Beast: only stab attacks with a spear or halberd deal full damage (everything else is halved); stomps
     * anyone standing beneath it. The dark energy core (OSRS Wiki): a 1/8 chance after it takes a hit of 32 or more, or
     * when it attacks below 1,000 hitpoints; the core hits players next to it for 5-13 every 2 ticks, healing the Beast
     * for half, and jumps to a player when nobody is next to it.
     */
    static final class Corp extends ScriptedBoss {
        private Mob core;
        private boolean coreRoll;
        private int coreTick;

        /** The core is rolled (1/8) on each attack it makes below 1,000 hitpoints (chooseAttack runs every tick, not per attack). */
        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (boss.getHealth() < mcHp(boss, 1000)) {
                coreRoll = true;
            }
            return attack;
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            return dx * dx + dz * dz < boss.getBbWidth() * boss.getBbWidth() / 4 ? ClassicBosses.CORP_STOMP : null;
        }

        static boolean spearOrHalberd(String id) {
            return id.contains("spear") || id.contains("halberd") || id.contains("hasta") || id.endsWith("_lance");
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            int d = type == AttackType.STAB && spearOrHalberd(BossFx.heldId(attacker)) ? damage : damage / 2;
            if (d >= 32) {
                coreRoll = true;
            }
            return d;
        }

        private boolean coreAlive() {
            return core != null && core.isAlive();
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (coreRoll) {
                coreRoll = false;
                if (!coreAlive() && target != null && boss.getRandom().nextInt(8) == 0) {
                    BossFx.say(boss, "The Corporeal Beast releases a dark energy core!");
                    core = BossFx.spawnMinion(boss, "dark_energy_core", around(boss, target.position(), 2), target);
                }
            }
            if (!coreAlive() || ++coreTick % 2 != 0) {
                return;
            }
            List<ServerPlayer> adjacent = BossFx.world(boss).getPlayers(p -> p.isAlive() && !p.isSpectator()
                    && p.distanceToSqr(core) < 2.0 * 2.0);
            if (adjacent.isEmpty()) {
                ServerPlayer to = target != null ? target : null;
                for (ServerPlayer p : BossFx.players(boss, 24)) {
                    // The core prefers the northernmost player (then the easternmost).
                    if (to == null || p.getZ() < to.getZ() - 1 || Math.abs(p.getZ() - to.getZ()) <= 1 && p.getX() > to.getX()) {
                        to = p;
                    }
                }
                if (to != null) {
                    Vec3 at = to.position().add(1, 0, 0);
                    core.teleportTo(at.x, at.y, at.z);
                    BossFx.burst(core, ParticleTypes.SOUL, 10, 0.5);
                }
                return;
            }
            for (ServerPlayer p : adjacent) {
                int dmg = 5 + p.getRandom().nextInt(9);
                com.runecraft.combat.CombatHooks.nameAttack("dark_core", null);
                CombatHandler.applyDamage(core, p, dmg);
                if (boss.isAlive()) {
                    boss.heal(mcHp(boss, dmg / 2));
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            if (coreAlive()) {
                core.discard();
            }
        }
    }

    /**
     * Skotizo (OSRS wiki): he randomly awakens the four altars of his lair; each awakened altar cuts the damage he takes
     * by 25% (15% with Arclight/Emberlight, which also break an altar in one hit). Below a third of his hitpoints he
     * summons three reanimated demon spawns (again whenever they are all slain), and while an altar is active a random
     * timer summons a dark ankou, one at a time.
     */
    static final class Skotizo extends ScriptedBoss {
        private final Mob[] altars = new Mob[SkotizoRules.ALTARS];
        private final List<Mob> spawns = new ArrayList<>();
        private Mob ankou;
        private int altarTimer = -1;
        private int ankouTimer = -1;
        private int resummon = -1;
        private boolean summoned;

        private static final int[][] SPOTS = {{0, -9}, {9, 0}, {0, 9}, {-9, 0}};

        /** Arclight and Emberlight disable an awakened altar in one hit (registered once by the module). */
        static void registerAltarBreaker() {
            com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
                @Override
                public int afterRoll(ServerPlayer player, net.minecraft.world.entity.LivingEntity target, AttackType type, int damage) {
                    if (target != null && target.getType() == com.runecraft.registry.ModEntities.typeById("awakened_altar")
                            && SkotizoRules.altarBreaker(BossFx.heldId(player))) {
                        return Math.max(damage, (int) Math.ceil(target.getHealth()));
                    }
                    return damage;
                }
            });
        }

        int awake() {
            int n = 0;
            for (Mob a : altars) {
                if (a != null && a.isAlive()) {
                    n++;
                }
            }
            return n;
        }

        private void awaken(DataBossEntity boss, int i) {
            Vec3 c = homeVec();
            altars[i] = BossFx.spawnMinion(boss, "awakened_altar", BossFx.ground(BossFx.world(boss), c.x + SPOTS[i][0], c.z + SPOTS[i][1],
                    c.y), null);
        }

        private int between(DataBossEntity boss, int min, int max) {
            return min + boss.getRandom().nextInt(max - min + 1);
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            home = boss.blockPosition();
            altarTimer = between(boss, SkotizoRules.ALTAR_MIN_TICKS, SkotizoRules.ALTAR_MAX_TICKS);
            BossFx.say(boss, "Skotizo awakens! Destroy the altars he awakens, or they will shield him.");
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            boolean breaker = attacker != null && SkotizoRules.altarBreaker(BossFx.heldId(attacker));
            return SkotizoRules.damage(damage, awake(), breaker);
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            summoned = true;
            summonSpawns(boss);
        }

        private void summonSpawns(DataBossEntity boss) {
            spawns.removeIf(m -> !m.isAlive());
            ServerPlayer target = target(boss);
            Vec3 c = boss.position();
            for (int i = spawns.size(); i < SkotizoRules.DEMON_SPAWNS; i++) {
                double a = Math.PI * 2 * i / SkotizoRules.DEMON_SPAWNS;
                Mob m = BossFx.spawnMinion(boss, "reanimated_demon_spawn",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 2.5, c.z + Math.sin(a) * 2.5, c.y), target);
                if (m != null) {
                    spawns.add(m);
                }
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            int awake = awake();
            // "Demonic Weakening": the kill counts while no altar is active.
            if (awake == 0) {
                CombatAchievementService.flagNear(boss, CombatAchievements.FLAG_SKOTIZO_NO_ALTARS);
            } else {
                CombatAchievementService.unflagNear(boss, CombatAchievements.FLAG_SKOTIZO_NO_ALTARS);
            }
            if (altarTimer > 0 && --altarTimer == 0) {
                altarTimer = between(boss, SkotizoRules.ALTAR_MIN_TICKS, SkotizoRules.ALTAR_MAX_TICKS);
                List<Integer> dormant = new ArrayList<>();
                for (int i = 0; i < altars.length; i++) {
                    if (altars[i] == null || !altars[i].isAlive()) {
                        dormant.add(i);
                    }
                }
                if (!dormant.isEmpty()) {
                    awaken(boss, dormant.get(boss.getRandom().nextInt(dormant.size())));
                    BossFx.say(boss, "Skotizo awakens one of the altars!");
                }
            }
            // A dark ankou timer runs while an altar is active; only one ankou at a time.
            boolean ankouAlive = ankou != null && ankou.isAlive();
            if (awake == 0 || ankouAlive) {
                ankouTimer = -1;
            } else if (ankouTimer < 0) {
                ankouTimer = between(boss, SkotizoRules.ANKOU_MIN_TICKS, SkotizoRules.ANKOU_MAX_TICKS);
            } else if (--ankouTimer <= 0) {
                ankouTimer = -1;
                ankou = BossFx.spawnMinion(boss, "dark_ankou", around(boss, homeVec(), 7), null);
                BossFx.warn(boss, "A dark ankou rises from the shadows.");
            }
            if (summoned) {
                spawns.removeIf(m -> !m.isAlive());
                if (spawns.isEmpty()) {
                    if (resummon < 0) {
                        resummon = SkotizoRules.RESUMMON_TICKS;
                    } else if (--resummon <= 0) {
                        resummon = -1;
                        BossFx.say(boss, "Skotizo: Gar mulno ful taglo!");
                        summonSpawns(boss);
                    }
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            for (Mob a : altars) {
                if (a != null && a.isAlive()) {
                    a.discard();
                }
            }
            spawns.forEach(Mob::discard);
            if (ankou != null && ankou.isAlive()) {
                ankou.discard();
            }
            if (boss.getRandom().nextInt(65) == 0) {
                Inv.give(killer, "pet_skotos", 1);
                CollectionLog.obtain(killer, "pet_skotos");
                Msg.red(killer, "You have a funny feeling like you're being followed.");
            }
        }
    }

    /** Hespori: at 2/3 and 1/3 health it becomes invulnerable until its flowers are cut down. */
    /**
     * Hespori (OSRS Wiki): its four flower buds open at the start of the fight and at 66% and 33% health and keep it
     * invulnerable until cut down (any hit kills one). It throws green spores (two hits of up to 8, ranged) or a green wave
     * (up to 14, magic), and now and then entangles the player: rooted for 10 ticks, then 40+ damage unless they struggle
     * free first (here: sneak, the closest thing to clicking to move; the sixth struggle frees them).
     */
    static final class Hespori extends ScriptedBoss {
        private final List<Mob> flowers = new ArrayList<>();
        private boolean bloomed;
        private int entangleTimer = BossMechanics.HESPORI_ENTANGLE_INTERVAL;
        private ServerPlayer rooted;
        private long rootEnds;
        private int struggles;
        private boolean sneaking;

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return rooted == null;
        }

        @Override
        public void onPhase(DataBossEntity boss, int phase) {
            bloom(boss);
        }

        private void bloom(DataBossEntity boss) {
            flowers.forEach(Mob::discard);
            flowers.clear();
            Vec3 c = boss.position();
            for (int i = 0; i < BossMechanics.HESPORI_BUDS; i++) {
                double a = Math.PI * 2 * i / BossMechanics.HESPORI_BUDS + Math.PI / 4;
                Mob f = BossFx.spawnMinion(boss, "hesporis_flower",
                        BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 3, c.z + Math.sin(a) * 3, c.y), null);
                if (f != null) {
                    MinionRules.set(f, new MinionRules.Rule() {
                        @Override
                        public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity m, AttackType t) {
                            return MinionRules.ALWAYS_HITS;
                        }

                        @Override
                        public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity m, AttackType t, int d) {
                            return MinionRules.lethal(m); // a bud dies to one hit from any weapon
                        }
                    });
                    flowers.add(f);
                }
            }
            BossFx.say(boss, "Hespori's flowers bloom, shielding it! Cut them down.");
        }

        @Override
        public boolean isVulnerable(DataBossEntity boss) {
            return flowers.stream().noneMatch(Mob::isAlive);
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return boss.getRandom().nextBoolean() ? ClassicBosses.HESPORI_SPORES : ClassicBosses.HESPORI_WAVE;
        }

        @Override
        protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
            super.perform(boss, target, a);
            if (a == ClassicBosses.HESPORI_SPORES) {
                super.perform(boss, target, a); // the spores hit twice
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (!bloomed) {
                bloomed = true;
                bloom(boss); // the buds open as the fight starts
            }
            if (rooted != null) {
                boolean s = rooted.isShiftKeyDown();
                if (s && !sneaking && ++struggles < BossMechanics.HESPORI_STRUGGLES) {
                    Msg.game(rooted, "You feel the vines loosen slightly as you try to move.");
                }
                sneaking = s;
                if (struggles >= BossMechanics.HESPORI_STRUGGLES) {
                    BossFx.unstun(rooted);
                    Msg.game(rooted, "You break free of the vines.");
                    rooted = null;
                } else if (now >= rootEnds) {
                    if (rooted.isAlive()) {
                        CombatHooks.nameAttack("entangle", null);
                        CombatHandler.applyDamage(boss, rooted, BossMechanics.HESPORI_ENTANGLE_MIN + boss.getRandom().nextInt(5));
                    }
                    BossFx.unstun(rooted);
                    rooted = null;
                }
                return;
            }
            if (target != null && --entangleTimer <= 0) {
                entangleTimer = BossMechanics.HESPORI_ENTANGLE_INTERVAL;
                rooted = target;
                struggles = 0;
                sneaking = target.isShiftKeyDown();
                rootEnds = now + BossMechanics.HESPORI_ENTANGLE_TICKS;
                BossFx.stun(target, BossMechanics.HESPORI_ENTANGLE_TICKS);
                Msg.red(target, "Hespori entangles you in some vines! (Sneak repeatedly to struggle free.)");
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            flowers.forEach(Mob::discard);
            if (rooted != null) {
                BossFx.unstun(rooted);
            }
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            onDeath(boss, null); // reset: the flowers go and an entangled player is freed
        }
    }

    /** Scurrius: once below 40% he runs to his food pile and eats. */
    static final class Scurrius extends ScriptedBoss {
        private boolean ate;

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (!ate && percent(boss) < 40) {
                ate = true;
                BossFx.say(boss, "Scurrius scurries to his food pile and starts to eat!");
                boss.heal(30);
                BossFx.burst(boss, ParticleTypes.HAPPY_VILLAGER, 15, 1.0);
            }
        }
    }

    /** Zalcano: her armour can only be broken by mining it; damage scales with Mining level. */
    /**
     * Zalcano (OSRS Wiki "Zalcano", "Zalcano/Strategies"). Her 300 stone armour only takes imbued tephra: mine tephra at the
     * glowing rock formation in one of the prison's four corners (the others give it far more slowly), refine it at the
     * furnace (east), imbue it at the altar (west), then attack her with it in your inventory: up to 5 + (Smithing +
     * Runecraft) / 14, 50% more standing on a blue demonic symbol. With the armour gone she falls for 12-16 s and is mined
     * with a pickaxe (max 30-33 by Mining level plus the pickaxe's bonus, 35.2 Mining XP a hit); then she rises, throwing
     * back and hurting anyone beside her and destroying every tephra in the prison, armoured again. Golems walk to her and
     * restore up to 50 armour; killed with imbued tephra they leave the top attacker 16-24 imbued tephra ("Team Player").
     * Her attacks are typeless: falling boulders, orange (ramping damage) and blue (empowering) demonic symbols, the glowing
     * rock exploding when she moves its glow, and pebbles. approx: the rock formations, furnace and altar are marked spots
     * (stand on them; mining needs a pickaxe in hand) rather than objects, mining success is a flat chance per swing, damage
     * from other sources (poison, recoil) is undone while she is armoured, and hits on her roll 1 to the max.
     */
    static final class Zalcano extends ScriptedBoss {
        /**
         * Block offsets from the arena centre: rock formations NE, SE, SW, NW (the north-east glows first), furnace east, altar
         * west. {@link #spot} pulls each in to stay inside the ring wall.
         */
        private static final int[][] CORNERS = {{9, -9}, {9, 9}, {-9, 9}, {-9, -9}};
        private static final int[] FURNACE = {11, 0};
        private static final int[] ALTAR = {-11, 0};
        private static final double STATION_RANGE = 2.5;
        /** The boulders' max hit (MinigameBosses' rockfall: the infobox max hit). */
        private static final int BOULDER_MAX = 53;
        private int armour = BossMechanics.ZALCANO_ARMOUR;
        private long downUntil = -1;
        private int glowing;
        private long explodeAt = -1;
        private float armouredHealth = -1;
        private final Map<UUID, Long> nextSwing = new HashMap<>();

        private record Symbol(Vec3 pos, boolean blue, long activeAt, long until) {
        }

        private final List<Symbol> symbols = new ArrayList<>();

        private static final class Golem {
            final Mob mob;
            final Map<UUID, Integer> damage = new HashMap<>();
            ServerPlayer last;

            Golem(Mob mob) {
                this.mob = mob;
            }
        }

        private final List<Golem> golems = new ArrayList<>();

        /** How far from the centre stations, golems and symbols may sit: two blocks inside the ring wall. */
        private static double reach(DataBossEntity boss) {
            return Math.max(2.0, (boss.def() == null ? 13 : boss.def().arenaRadius) - 2.0);
        }

        /** A point at an offset from the arena centre, pulled in to stay at least two blocks inside the ring wall. */
        private Vec3 spot(DataBossEntity boss, double dx, double dz) {
            double max = reach(boss);
            double len = Math.hypot(dx, dz);
            if (len > max) {
                dx *= max / len;
                dz *= max / len;
            }
            Vec3 c = homeVec();
            return new Vec3(c.x + dx, c.y, c.z + dz);
        }

        private Vec3 spot(DataBossEntity boss, int[] off) {
            return spot(boss, off[0], off[1]);
        }

        private static double flat(Vec3 a, Vec3 b) {
            double dx = a.x - b.x;
            double dz = a.z - b.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        boolean down() {
            return downUntil >= 0;
        }

        private static int level(ServerPlayer p, Skill skill) {
            return com.runecraft.player.Players.data(p).skills().currentLevel(skill);
        }

        private boolean onBlue(ServerPlayer p) {
            for (Symbol s : symbols) {
                if (s.blue() && now <= s.until() && flat(p.position(), s.pos()) <= 1.5) {
                    return true;
                }
            }
            return false;
        }

        /** A thrown imbued tephra's damage (0 to the max). */
        private int tephraHit(ServerPlayer p) {
            int max = BossMechanics.tephraMaxHit(level(p, Skill.SMITHING), level(p, Skill.RUNECRAFT), onBlue(p));
            return p.getRandom().nextInt(max + 1);
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            // The stations are laid out around the arena centre (her spawn block sits by the summoning stone at the north
            // edge), and she stands in the middle of her prison.
            home = arenaCentre(boss);
            if (!home.equals(boss.blockPosition())) {
                Vec3 c = homeVec();
                boss.getNavigation().stop();
                boss.teleportTo(c.x, c.y, c.z);
            }
            BossFx.say(boss, "Zalcano rises in her prison! Mine tephra at the glowing rocks, refine it at the furnace (east) and "
                    + "imbue it at the altar (west) to break her armour.");
        }

        @Override
        protected boolean driven() {
            return true;
        }

        @Override
        protected boolean canAttack(DataBossEntity boss) {
            return !down();
        }

        @Override
        protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            int roll = boss.getRandom().nextInt(10);
            List<ServerPlayer> players = BossFx.players(boss, 28);
            if (roll < 3) {
                BossFx.warn(boss, "Zalcano stomps the ground... rocks fall from the ceiling!");
                for (ServerPlayer p : players) {
                    namedStrike("rockfall", boss, p.position(), 4, maxHit(boss, BOULDER_MAX), 1.0, ParticleTypes.ASH);
                }
            } else if (roll < 5) {
                summonSymbols(boss);
            } else if (roll < 7 && explodeAt < 0) {
                explodeAt = now + 3;
                BossFx.warn(boss, "Zalcano hurls a ball of energy at the glowing rock formation!");
            } else if (roll < 9 && golems.size() < 2) {
                spawnGolem(boss);
            } else {
                for (ServerPlayer p : players) {
                    CombatHooks.nameAttack("pebbles", null);
                    CombatHandler.applyDamage(boss, p, 1 + p.getRandom().nextInt(5)); // approx: "only a few hitpoints"
                }
            }
            attackCount++;
            cooldown = Math.max(1, attackDelay(boss, null)) - 1;
            return null;
        }

        private static int maxHit(DataBossEntity boss, int base) {
            return (int) Math.round(base * boss.variantHitScale(null));
        }

        private void summonSymbols(DataBossEntity boss) {
            symbols.clear();
            int blue = 2 + boss.getRandom().nextInt(4);
            for (int i = 0; i < 9; i++) {
                symbols.add(new Symbol(around(boss, homeVec(), reach(boss)), i < blue, now + 3, now + 18)); // approx duration
            }
            BossFx.warn(boss, "Zalcano summons demonic symbols! Blue ones empower your tephra; avoid the orange.");
        }

        /** Spawns a golem at the prison's wall; it walks to her. Package-private for the self-test. */
        Golem spawnGolem(DataBossEntity boss) {
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            Vec3 at = spot(boss, Math.cos(a) * 12, Math.sin(a) * 12);
            Mob m = BossFx.spawnMinion(boss, "zalcano_golem", BossFx.ground(BossFx.world(boss), at.x, at.z, at.y), null);
            if (m == null) {
                return null;
            }
            Golem g = new Golem(m);
            golems.add(g);
            MinionRules.set(m, new MinionRules.Rule() {
                @Override
                public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type) {
                    return MinionRules.ALWAYS_HITS;
                }

                @Override
                public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type, int d) {
                    if (!Inv.has(p, BossMechanics.IMBUED_TEPHRA, 1)) {
                        Msg.action(p, "Only imbued tephra can harm the golem.");
                        return 0;
                    }
                    Inv.remove(p, BossMechanics.IMBUED_TEPHRA, 1);
                    int hit = tephraHit(p);
                    g.damage.merge(p.getUUID(), hit, Integer::sum);
                    g.last = p;
                    return hit;
                }
            });
            BossFx.warn(boss, "Zalcano fires a ball of energy at the wall... a golem forms from the rubble!");
            return g;
        }

        private void tickGolems(DataBossEntity boss) {
            Iterator<Golem> it = golems.iterator();
            while (it.hasNext()) {
                Golem g = it.next();
                if (!g.mob.isAlive()) {
                    it.remove();
                    if (g.mob.getHealth() <= 0) {
                        rewardGolem(boss, g);
                    }
                    continue;
                }
                g.mob.setTarget(null);
                if (now % 5 == 0) {
                    g.mob.getNavigation().moveTo(boss.getX(), boss.getY(), boss.getZ(), 1.0);
                }
                if (g.mob.distanceToSqr(boss) < 3.0 * 3.0) {
                    if (!down()) {
                        int heal = BossMechanics.golemArmourHeal(g.mob.getHealth() / g.mob.getMaxHealth());
                        armour = Math.min(BossMechanics.ZALCANO_ARMOUR, armour + heal);
                        BossFx.say(boss, "The golem reaches Zalcano and restores " + heal + " of her armour (" + armour + "/"
                                + BossMechanics.ZALCANO_ARMOUR + ").");
                    }
                    BossFx.burst(g.mob, ParticleTypes.EXPLOSION, 2, 0.4);
                    g.mob.discard();
                    it.remove();
                }
            }
        }

        /** The golem's 16-24 imbued tephra go to whoever dealt it the most damage (or its last attacker). */
        private void rewardGolem(DataBossEntity boss, Golem g) {
            UUID best = null;
            int most = -1;
            for (Map.Entry<UUID, Integer> e : g.damage.entrySet()) {
                if (e.getValue() > most) {
                    most = e.getValue();
                    best = e.getKey();
                }
            }
            ServerPlayer to = g.last;
            if (best != null && (to == null || !to.getUUID().equals(best))) {
                for (ServerPlayer p : BossFx.players(boss, 48)) {
                    if (p.getUUID().equals(best)) {
                        to = p;
                    }
                }
            }
            if (to == null) {
                return;
            }
            int n = BossMechanics.ZALCANO_GOLEM_TEPHRA_MIN
                    + to.getRandom().nextInt(BossMechanics.ZALCANO_GOLEM_TEPHRA_MAX - BossMechanics.ZALCANO_GOLEM_TEPHRA_MIN + 1);
            Inv.give(to, BossMechanics.IMBUED_TEPHRA, n);
            CombatAchievementService.flag(to, BossMechanics.FLAG_ZALCANO_GOLEM_TEPHRA);
            Msg.game(to, "The golem crumbles, leaving you " + n + " imbued tephra.");
        }

        private void fall(DataBossEntity boss) {
            armour = 0;
            downUntil = now + BossMechanics.ZALCANO_DOWN_MIN_TICKS
                    + boss.getRandom().nextInt(BossMechanics.ZALCANO_DOWN_MAX_TICKS - BossMechanics.ZALCANO_DOWN_MIN_TICKS + 1);
            BossFx.say(boss, "Zalcano's armour shatters and she collapses! Mine her with a pickaxe!");
            BossFx.burst(boss, ParticleTypes.LAVA, 20, 1.2);
        }

        private void rise(DataBossEntity boss) {
            downUntil = -1;
            armour = BossMechanics.ZALCANO_ARMOUR;
            BossFx.say(boss, "Zalcano rises again, her armour restored! Her shockwave destroys all tephra in the prison.");
            for (ServerPlayer p : BossFx.players(boss, 32)) {
                if (p.distanceToSqr(boss) < 3.5 * 3.5) {
                    p.knockback(1.0, boss.getX() - p.getX(), boss.getZ() - p.getZ());
                    p.hurtMarked = true;
                    CombatHooks.nameAttack("zalcano_shockwave", null);
                    CombatHandler.applyDamage(boss, p, BossMechanics.ZALCANO_RISE_DAMAGE);
                    BossFx.stun(p, 1 + p.getRandom().nextInt(2));
                }
                destroyTephra(p);
            }
            BossFx.burst(boss, ParticleTypes.CLOUD, 30, 2.0);
        }

        private static void destroyTephra(ServerPlayer p) {
            for (String id : BossMechanics.TEPHRA_ITEMS) {
                int n = Inv.count(p, id);
                if (n > 0) {
                    Inv.remove(p, id, n);
                }
            }
        }

        private void mine(ServerPlayer p, boolean glowingRock) {
            Long due = nextSwing.get(p.getUUID());
            if (due != null && now < due) {
                return;
            }
            nextSwing.put(p.getUUID(), now + 3); // approx: one swing every 3 ticks
            int pick = com.runecraft.osrs.content.raids.RaidScaling.pickaxeLevel(BossFx.heldId(p));
            double chance = glowingRock ? Math.min(0.9, 0.3 + level(p, Skill.MINING) / 200.0 + pick / 400.0) : 0.08; // approx
            if (p.getRandom().nextDouble() >= chance) {
                return;
            }
            Inv.give(p, BossMechanics.TEPHRA, 1);
            XpService.add(p, Skill.MINING, BossMechanics.TEPHRA_MINING_XP);
            if (p.getRandom().nextInt(BossMechanics.TEPHRA_SHARD_CHANCE) == 0) {
                Inv.give(p, "crystal_shard", 1 + p.getRandom().nextInt(3));
            }
        }

        /** Mining at the rock formations, refining at the furnace and imbuing at the altar (one tephra a tick). */
        private void tephraLoop(DataBossEntity boss) {
            ServerLevel w = BossFx.world(boss);
            Vec3 furnace = spot(boss, FURNACE);
            Vec3 altar = spot(boss, ALTAR);
            if (now % 4 == 0) {
                for (int i = 0; i < CORNERS.length; i++) {
                    BossFx.column(w, spot(boss, CORNERS[i]), i == glowing ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
                            i == glowing ? 6 : 2);
                }
                BossFx.ring(w, furnace, 1.2, ParticleTypes.LAVA);
                BossFx.ring(w, altar, 1.2, ParticleTypes.ENCHANT);
            }
            for (ServerPlayer p : BossFx.players(boss, 28)) {
                Vec3 pos = p.position();
                int corner = -1;
                for (int i = 0; i < CORNERS.length; i++) {
                    if (flat(pos, spot(boss, CORNERS[i])) <= STATION_RANGE) {
                        corner = i;
                    }
                }
                if (corner >= 0 && BossFx.heldId(p).endsWith("pickaxe")) {
                    mine(p, corner == glowing);
                } else if (flat(pos, furnace) <= STATION_RANGE && Inv.has(p, BossMechanics.TEPHRA, 1)) {
                    Inv.remove(p, BossMechanics.TEPHRA, 1);
                    Inv.give(p, BossMechanics.REFINED_TEPHRA, 1);
                    XpService.add(p, Skill.SMITHING, BossMechanics.TEPHRA_REFINE_XP);
                } else if (flat(pos, altar) <= STATION_RANGE && Inv.has(p, BossMechanics.REFINED_TEPHRA, 1)) {
                    Inv.remove(p, BossMechanics.REFINED_TEPHRA, 1);
                    Inv.give(p, BossMechanics.IMBUED_TEPHRA, 1);
                    XpService.add(p, Skill.RUNECRAFT, BossMechanics.TEPHRA_IMBUE_XP);
                }
            }
        }

        private void tickSymbols(DataBossEntity boss) {
            ServerLevel w = BossFx.world(boss);
            symbols.removeIf(s -> now > s.until());
            for (Symbol s : symbols) {
                if (now % 2 == 0) {
                    BossFx.ring(w, s.pos(), 1.5, s.blue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME);
                }
                if (s.blue() || now < s.activeAt()) {
                    continue;
                }
                // Orange symbols hurt more the longer they've been active, up to 26.
                int dmg = (int) Math.min(BossMechanics.ZALCANO_SYMBOL_MAX, 4 + 3 * (now - s.activeAt())); // approx ramp
                for (ServerPlayer p : BossFx.players(boss, 32)) {
                    if (flat(p.position(), s.pos()) <= 1.5) {
                        CombatHooks.nameAttack("red_symbol", null);
                        CombatHandler.applyDamage(boss, p, dmg);
                    }
                }
            }
        }

        @Override
        protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
            if (!boss.isAlive()) {
                return;
            }
            // Only mining can hurt her: undo damage from other sources while she is armoured.
            if (!down() && armouredHealth > 0 && boss.getHealth() < armouredHealth) {
                boss.setHealth(armouredHealth);
            }
            if (down() && now >= downUntil) {
                rise(boss);
            }
            armouredHealth = boss.getHealth();
            tephraLoop(boss);
            tickSymbols(boss);
            tickGolems(boss);
            if (explodeAt >= 0 && now >= explodeAt) {
                explodeAt = -1;
                Vec3 rock = spot(boss, CORNERS[glowing]);
                BossFx.world(boss).sendParticles(ParticleTypes.EXPLOSION, rock.x, rock.y + 0.5, rock.z, 4, 0.8, 0.5, 0.8, 0);
                for (ServerPlayer p : BossFx.players(boss, 32)) {
                    if (flat(p.position(), rock) <= 3.0) {
                        CombatHooks.nameAttack("rock_explosion", null);
                        CombatHandler.applyDamage(boss, p, BossMechanics.ZALCANO_ROCK_MIN
                                + p.getRandom().nextInt(BossMechanics.ZALCANO_ROCK_MAX - BossMechanics.ZALCANO_ROCK_MIN + 1));
                    }
                }
                glowing = (glowing + 1 + boss.getRandom().nextInt(CORNERS.length - 1)) % CORNERS.length;
                BossFx.warn(boss, "The rock formation explodes! Another one starts to glow.");
            }
            if (down()) {
                for (ServerPlayer p : BossFx.players(boss, 4)) {
                    if (flat(p.position(), boss.position()) <= 1.6) {
                        CombatHooks.nameAttack("under_zalcano", null);
                        CombatHandler.applyDamage(boss, p, BossMechanics.ZALCANO_UNDER_MIN
                                + p.getRandom().nextInt(BossMechanics.ZALCANO_UNDER_MAX - BossMechanics.ZALCANO_UNDER_MIN + 1));
                    }
                }
            }
        }

        @Override
        protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (attacker == null) {
                return 0;
            }
            if (down()) {
                String held = BossFx.heldId(attacker);
                if (!held.endsWith("pickaxe")) {
                    Msg.action(attacker, "Zalcano has fallen: mine her with a pickaxe!");
                    return 0;
                }
                int max = BossMechanics.zalcanoMiningMaxHit(level(attacker, Skill.MINING),
                        com.runecraft.osrs.content.raids.RaidScaling.pickaxeLevel(held));
                XpService.add(attacker, Skill.MINING, BossMechanics.ZALCANO_MINING_XP);
                BossFx.burst(boss, ParticleTypes.LAVA, 4, 0.6);
                int dealt = 1 + boss.getRandom().nextInt(max);
                armouredHealth = Math.max(0, boss.getHealth() - dealt);
                return dealt;
            }
            if (!Inv.has(attacker, BossMechanics.IMBUED_TEPHRA, 1)) {
                Msg.action(attacker, "Zalcano's stone armour can only be broken with imbued tephra.");
                return 0;
            }
            Inv.remove(attacker, BossMechanics.IMBUED_TEPHRA, 1);
            int hit = tephraHit(attacker);
            armour = Math.max(0, armour - hit);
            Msg.action(attacker, "Your imbued tephra hits her armour for " + hit + " (" + armour + "/" + BossMechanics.ZALCANO_ARMOUR + ").");
            if (armour <= 0) {
                fall(boss);
            }
            return 0;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            golems.forEach(g -> g.mob.discard());
            golems.clear();
            symbols.clear();
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            onDeath(boss, null);
        }
    }

    /**
     * Self-test (boss:fight:zalcano): what a team does for the player: imbued tephra thrown at her armour until she falls,
     * then a pickaxe in hand to mine her. Null when there is nothing to do.
     */
    static String selfTestAssistZalcano(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Zalcano z)) {
            return null;
        }
        if (!z.down()) {
            Inv.give(p, BossMechanics.IMBUED_TEPHRA, 40);
            for (int i = 0; i < 40 && !z.down() && Inv.has(p, BossMechanics.IMBUED_TEPHRA, 1); i++) {
                z.incoming(boss, p, 0, AttackType.CRUSH);
            }
            Zalcano.destroyTephra(p);
        }
        if (z.down() && !BossFx.heldId(p).endsWith("pickaxe")) {
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("rune_pickaxe", 1));
        }
        return z.down() ? "tephra downed her, mined with a pickaxe" : "tephra thrown";
    }

    /**
     * Self-test (boss:zalcano_tephra): her armour ignores attacks without imbued tephra, tephra breaks it and she falls; a
     * golem killed with tephra leaves its attacker 16-24 imbued tephra.
     */
    static String selfTestZalcano(DataBossEntity boss, ServerPlayer p) {
        if (!(boss.script() instanceof Zalcano z)) {
            throw new IllegalStateException("Zalcano has no Zalcano script");
        }
        z.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
        Zalcano.destroyTephra(p);
        int before = z.armour;
        z.incoming(boss, p, 10, AttackType.CRUSH);
        if (z.armour != before || z.down()) {
            throw new IllegalStateException("her armour took damage without imbued tephra");
        }
        Inv.give(p, BossMechanics.IMBUED_TEPHRA, 20);
        z.armour = 1;
        for (int i = 0; i < 20 && !z.down(); i++) {
            z.incoming(boss, p, 0, AttackType.CRUSH);
        }
        if (!z.down()) {
            throw new IllegalStateException("20 imbued tephra didn't break her last point of armour");
        }
        String golem = "no golem spawned";
        Zalcano.Golem g = z.spawnGolem(boss);
        if (g != null) {
            Zalcano.destroyTephra(p);
            g.damage.put(p.getUUID(), BossMechanics.ZALCANO_GOLEM_HP);
            g.last = p;
            g.mob.setHealth(0f);
            z.gameTick(boss, com.runecraft.action.ActionManager.gameTick());
            int got = Inv.count(p, BossMechanics.IMBUED_TEPHRA);
            if (got < BossMechanics.ZALCANO_GOLEM_TEPHRA_MIN || got > BossMechanics.ZALCANO_GOLEM_TEPHRA_MAX) {
                throw new IllegalStateException("a killed golem left " + got + " imbued tephra");
            }
            g.mob.discard();
            golem = "golem left " + got + " imbued tephra";
        }
        Zalcano.destroyTephra(p);
        return "armour needs imbued tephra, tephra downs her; " + golem;
    }

}
