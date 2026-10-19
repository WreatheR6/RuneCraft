package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.osrs.content.raids.TobNylocas;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Theatre of Blood rooms. */
final class TobRooms {
    private TobRooms() {
    }

    /** Moves passive crabs/spawn toward a point. */
    static void walk(LivingEntity e, Vec3 to) {
        if (e instanceof Mob mob) {
            mob.setTarget(null);
            if (mob.tickCount % 10 == 0 || mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(to.x, to.y, to.z, 1.0);
            }
        }
    }

    /** The Maiden of Sugadinti: nylocas heal her, blood spawn leave trails, blood splats everywhere. */
    static final class Maiden extends RoomLogic {
        private DataBossEntity maiden;
        private final Set<Integer> thresholds = new HashSet<>();
        /** Nylocas Matomenos that reached her (each makes her tornadoes hit harder; Hard mode: the first five faster). */
        private int fed;
        /** Blood trails left by blood spawn, with the tick they dry (never in Hard mode). */
        private final Map<Vec3, Long> trail = new HashMap<>();
        private final Set<BlockPos> trailTiles = new HashSet<>();

        Maiden(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int i = 0; i < 18; i++) {
                RaidBuilder.set(world(), center().offset(rnd().nextInt(36) - 18, 0, rnd().nextInt(36) - 18),
                        Blocks.RED_CONCRETE.defaultBlockState());
            }
            for (int z = -16; z <= 16; z += 8) {
                RaidBuilder.fill(world(), center().getX() + 20, center().getY() + 6, center().getZ() + z,
                        center().getX() + 20, center().getY() + 9, center().getZ() + z, Blocks.CHAIN.defaultBlockState());
            }
        }

        @Override
        public void start() {
            thresholds.clear();
            fed = 0;
            trail.clear();
            trailTiles.clear();
            maiden = spawnBoss("the_maiden_of_sugadinti", at(15, 0));
            anchor(maiden);
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(maiden)) {
                return;
            }
            // Hard mode (OSRS): each of the first five Matomenos to reach her speeds up her attacks. // approx: a tick each
            autoAttack(maiden, tick, hard() ? 10 - Math.min(5, fed) : 10);
            double hp = raid.healthFraction(maiden);
            for (int t : new int[]{70, 50, 30}) {
                if (hp * 100 <= t && thresholds.add(t)) {
                    // OSRS: two Nylocas Matomenos per player (up to 10; always 10 in Hard mode), from the north and south walls.
                    int n = com.runecraft.osrs.content.raids.RaidScaling.tobMaidenNylocas(raid.partySize(), hard());
                    for (int i = 0; i < n; i++) {
                        int z = (i % 2 == 0 ? -1 : 1) * 18;
                        spawnMob("nylocas_matomenos", at(4 + (i / 2) * 3, z), false);
                    }
                    warn("The Maiden calls upon the Nylocas! (" + t + "%)");
                }
            }
            // OSRS: a Matomenos that reaches her heals her for twice its remaining hitpoints.
            fed += healers("nylocas_matomenos", maiden, 2.0, "A Nylocas Matomenos reaches the Maiden and heals her!");
            List<LivingEntity> spawns = living("blood_spawn");
            for (LivingEntity sp : spawns) {
                if (tick % 2 == 0) {
                    trail.put(sp.position(), hard() ? Long.MAX_VALUE : tick + 16);
                    if (trailTiles.add(sp.blockPosition())) {
                        com.runecraft.content.systems.CombatAchievementService.countNear(maiden, com.runecraft.osrs.content.systems.CombatAchievements.COUNT_MAIDEN_TRAIL);
                    }
                }
                if (sp.tickCount > 200) {
                    com.runecraft.content.systems.CombatAchievementService.flagNear(maiden, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_BLOODSPAWN_OLD);
                }
                if (sp.tickCount % 60 == 0) {
                    walk(sp, vec(rnd().nextInt(30) - 15, rnd().nextInt(30) - 15));
                }
            }
            trail.entrySet().removeIf(e -> e.getValue() < tick);
            for (ServerPlayer p : players()) {
                for (Vec3 t : trail.keySet()) {
                    if (RaidEffects.horizontalDistSq(p.position(), t) < 0.8 * 0.8) {
                        if (tick % 2 == 0) {
                            world().sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.x, t.y + 0.3, t.z, 2, 0.2, 0.1, 0.2, 0);
                        }
                        if (!cooldown(p, 1)) {
                            // OSRS: standing in a blood trail hurts and heals the Maiden by the damage taken.
                            int dealt = RaidEffects.hit(maiden, p, 3 + rnd().nextInt(5), null, true); // approx: trail damage
                            raid.heal(maiden, dealt);
                        }
                        break;
                    }
                }
            }
            if (tick % 10 == 5) {
                bloodSpots(spawns.size());
            }
        }

        /**
         * Blood Splat (OSRS): a blood spot at every player and three more at the player furthest away; a spot that hits
         * heals the Maiden by the damage dealt, and spots may turn into blood spawn.
         */
        private void bloodSpots(int spawnCount) {
            List<ServerPlayer> players = players();
            if (players.isEmpty()) {
                return;
            }
            List<Vec3> spots = new ArrayList<>();
            ServerPlayer far = null;
            for (ServerPlayer p : players) {
                spots.add(p.position());
                if (far == null || p.distanceToSqr(maiden) > far.distanceToSqr(maiden)) {
                    far = p;
                }
            }
            for (int i = 0; i < 3; i++) {
                spots.add(far.position().add(rnd().nextInt(3) - 1, 0, rnd().nextInt(3) - 1));
            }
            int cap = hard() ? 5 : 3;
            int[] spawned = {spawnCount};
            for (Vec3 at : spots) {
                world().sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y + 0.2, at.z, 6, 0.4, 0.1, 0.4, 0);
                RaidEffects.later(3, () -> {
                    if (!alive(maiden)) {
                        return;
                    }
                    for (ServerPlayer p : players()) {
                        if (RaidEffects.horizontalDistSq(p.position(), at) < 0.9 * 0.9) {
                            int dealt = RaidEffects.hit(maiden, p, rnd().nextInt(26), null, true); // approx: blood spot up to 25
                            raid.heal(maiden, dealt);
                        }
                    }
                    if (spawned[0] < cap && rnd().nextInt(5) == 0) { // approx: 1 in 5 spots become blood spawn
                        spawned[0]++;
                        spawnMob("blood_spawn", BlockPos.containing(at), false);
                    }
                });
            }
        }

        @Override
        public int bossOutgoing(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            // OSRS: every Matomenos that reaches the Maiden increases her tornado damage. // approx: +10% each
            return boss == maiden ? (int) Math.round(damage * (1 + 0.1 * fed)) : damage;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            living("nylocas_matomenos").forEach(LivingEntity::discard);
            living("blood_spawn").forEach(LivingEntity::discard);
        }
    }

    /** The Pestilent Bloat: walks the room dropping limbs and flies; only harmed while it rests. */
    static final class Bloat extends RoomLogic {
        private DataBossEntity bloat;
        private boolean walking = true;
        private int stateTicks;
        private int stateLength = com.runecraft.osrs.content.raids.RaidScaling.TOB_BLOAT_FIRST_WALK;
        private int waypoint;
        /** +1 walks the corners clockwise, -1 after a turn. */
        private int direction = 1;
        private int turnTicks;

        Bloat(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        private Vec3 waypoint(int i) {
            int[][] corners = {{-11, -11}, {11, -11}, {11, 11}, {-11, 11}};
            int[] c = corners[Math.floorMod(i, 4)];
            return vec(c[0], c[1]);
        }

        @Override
        public void decorate() {
            RaidBuilder.pillar(world(), center(), 4, RaidBuilder.HEIGHT, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            for (int i = 0; i < 6; i++) {
                RaidBuilder.set(world(), center().offset(rnd().nextInt(36) - 18, RaidBuilder.HEIGHT, rnd().nextInt(36) - 18),
                        Blocks.CHAIN.defaultBlockState());
            }
        }

        @Override
        public void start() {
            walking = true;
            stateTicks = 0;
            direction = 1;
            turnTicks = 0;
            // OSRS: the first walk lasts 39-47 ticks.
            stateLength = com.runecraft.osrs.content.raids.RaidScaling.TOB_BLOAT_FIRST_WALK + rnd().nextInt(9);
            bloat = spawnBoss("pestilent_bloat", BlockPos.containing(waypoint(0)));
        }

        @Override
        public void bossTick(DataBossEntity boss) {
            boss.setTarget(null);
            if (!walking) {
                boss.getNavigation().stop();
                return;
            }
            Vec3 wp = waypoint(waypoint);
            if (boss.position().distanceToSqr(wp) < 2.0) {
                waypoint += direction;
                wp = waypoint(waypoint);
            }
            if (boss.tickCount % 10 == 0 || boss.getNavigation().isDone()) {
                boss.getNavigation().moveTo(wp.x, wp.y, wp.z, 1.3);
            }
        }

        /**
         * OSRS: the Bloat may turn around while it walks, once every 32 ticks at most (Hard mode: every 16 ticks, and it
         * turns more often); a turn near the end of a walk keeps it up for another 5 ticks.
         */
        private void maybeTurn() {
            int every = hard() ? com.runecraft.osrs.content.raids.RaidScaling.TOB_BLOAT_TURN_HARD
                    : com.runecraft.osrs.content.raids.RaidScaling.TOB_BLOAT_TURN;
            if (++turnTicks < every) {
                return;
            }
            turnTicks = 0;
            if (rnd().nextInt(hard() ? 2 : 4) != 0) { // approx: the turn chance isn't published
                return;
            }
            direction = -direction;
            waypoint += direction;
            if (stateLength - stateTicks < 5) {
                stateLength = stateTicks + 5;
            }
            warn("The Bloat turns around!");
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            // OSRS: the Bloat takes 50% less damage (rounded down) while it moves.
            return walking ? damage / 2 : damage;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(bloat)) {
                return;
            }
            stateTicks++;
            List<ServerPlayer> players = players();
            if (walking) {
                // OSRS: while it walks, its flies hit everyone in its line of sight for 10-20 every tick
                // (Protect from Missiles takes 25% off).
                for (ServerPlayer p : players) {
                    if (canSee(bloat, p)) {
                        int dmg = 10 + rnd().nextInt(11);
                        if (RaidEffects.protectedFrom(p, AttackType.RANGED)) {
                            dmg = dmg * 3 / 4;
                        }
                        RaidEffects.hit(bloat, p, dmg, null, true);
                    }
                }
                if (tick % 3 == 0 && !players.isEmpty()) {
                    // OSRS: mutilated flesh falls from the ceiling for 30-50 (Hard mode: all fight long).
                    ServerPlayer p = players.get(rnd().nextInt(players.size()));
                    Vec3 at = p.position().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2);
                    RaidEffects.strike(world(), at, 1.0, 50, 3, bloat, ParticleTypes.FALLING_LAVA);
                }
                maybeTurn();
                if (stateTicks >= stateLength) {
                    walking = false;
                    stateTicks = 0;
                    stateLength = com.runecraft.osrs.content.raids.RaidScaling.TOB_BLOAT_DOWN;
                    say("The Bloat shuts down. Attack it now!");
                    com.runecraft.content.systems.CombatAchievementService.countNear(bloat, com.runecraft.osrs.content.systems.CombatAchievements.COUNT_BLOAT_SHUTDOWN);
                }
            } else {
                world().sendParticles(ParticleTypes.SMOKE, bloat.getX(), bloat.getY() + 2, bloat.getZ(), 4, 0.8, 0.4, 0.8, 0.01);
                if (stateTicks == stateLength - 1) {
                    warn("The Bloat stirs...");
                }
                if (hard() && tick % 3 == 0 && !players.isEmpty()) {
                    ServerPlayer p = players.get(rnd().nextInt(players.size()));
                    RaidEffects.strike(world(), p.position().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2), 1.0, 50, 3, bloat,
                            ParticleTypes.FALLING_LAVA);
                }
                if (stateTicks >= stateLength) {
                    walking = true;
                    stateTicks = 0;
                    // OSRS: after the stomp it walks for 34-42 ticks.
                    stateLength = com.runecraft.osrs.content.raids.RaidScaling.TOB_BLOAT_WALK + rnd().nextInt(9);
                    world().sendParticles(ParticleTypes.EXPLOSION_EMITTER, bloat.getX(), bloat.getY(), bloat.getZ(), 1, 0, 0, 0, 0);
                    for (ServerPlayer p : players) {
                        if (p.distanceToSqr(bloat) < 4.5 * 4.5) {
                            RaidEffects.hit(bloat, p, 40 + rnd().nextInt(41), null, true); // OSRS: the stomp deals 40-80
                            Vec3 push = p.position().subtract(bloat.position()).normalize().scale(1.2);
                            p.push(push.x, 0.5, push.z);
                            p.hurtMarked = true;
                            Msg.red(p, "The Bloat stomps on you!");
                        }
                    }
                }
            }
        }
    }

    /**
     * The Nylocas (OSRS wiki): 31 fixed waves of nylocas walk in from the east, south and west lanes; most attack the
     * four supports, the aggressive ones the team. Each breed only takes damage from its own style; big nylocas split
     * into two smalls; every nylocas self-destructs 30 seconds after spawning. A collapsing support hits the whole team
     * for up to 50 and losing all four kills everyone. Waves stall while 12 (24 from wave 20) nylocas are alive. In Hard
     * mode a Nylocas Prinkipas drops in with waves 10, 20 and 30. Then the Nylocas Vasilias: it changes form every 10
     * ticks, and a hit of the wrong style is reflected onto the attacker and heals it.
     */
    static final class Nylocas extends RoomLogic {
        private static final String[] FORMS = {"grey (melee)", "green (ranged)", "blue (magic)"};
        /** Support positions (room offsets) and the east, south and west lane mouths. */
        private static final int[][] PILLARS = {{-5, -5}, {-5, 5}, {5, -5}, {5, 5}};
        private static final int[][] LANES = {{19, 0}, {0, 19}, {-19, 8}};

        /** One live wave nylocas. */
        private static final class Nylo {
            final Mob mob;
            final TobNylocas.Breed breed;
            final boolean big;
            final boolean aggressive;
            int age;

            Nylo(Mob mob, TobNylocas.Breed breed, boolean big, boolean aggressive) {
                this.mob = mob;
                this.breed = breed;
                this.big = big;
                this.aggressive = aggressive;
            }
        }

        private final List<Nylo> nylos = new ArrayList<>();
        private final int[] pillarHp = new int[TobNylocas.PILLARS];
        private int wave;
        private int waveTimer;
        private DataBossEntity vasilias;
        private int form;
        private int formTimer;
        private DataBossEntity prinkipas;
        private int prinkipasForm;
        private int prinkipasTimer;
        private boolean wrongStyle;
        private boolean pillarLost;
        private boolean prinkipasChanged;

        Nylocas(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        private static AttackDef formAttack(int form, int maxHit) {
            return switch (form) {
                case 0 -> AttackDef.of("bite", AttackType.STAB, maxHit, 3, 10);
                case 1 -> AttackDef.of("spit", AttackType.RANGED, maxHit, 12, 10).particle("minecraft:crit");
                default -> AttackDef.of("blast", AttackType.MAGIC, maxHit, 12, 10).particle("minecraft:witch");
            };
        }

        private BlockPos pillarBase(int i) {
            return center().offset(PILLARS[i][0], 0, PILLARS[i][1]);
        }

        @Override
        public void decorate() {
            for (int i = 0; i < PILLARS.length; i++) {
                RaidBuilder.pillar(world(), pillarBase(i), 1, 4, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                RaidBuilder.set(world(), pillarBase(i).above(5), Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }

        @Override
        public void start() {
            wave = 0;
            waveTimer = 2;
            vasilias = null;
            prinkipas = null;
            nylos.clear();
            wrongStyle = false;
            pillarLost = false;
            prinkipasChanged = false;
            java.util.Arrays.fill(pillarHp, TobNylocas.PILLAR_HP);
            say("Protect the supports! " + TobNylocas.WAVES + " waves of Nylocas approach.");
        }

        /** The Nylocas room a player is fighting in, if any. */
        static Nylocas of(ServerPlayer p) {
            RaidInstance raid = RaidManager.of(p);
            return raid != null && raid.type == com.runecraft.osrs.content.raids.RaidType.TOB && raid.logic() instanceof Nylocas n
                    ? n : null;
        }

        private int standing() {
            int n = 0;
            for (int hp : pillarHp) {
                if (hp > 0) {
                    n++;
                }
            }
            return n;
        }

        private void spawnNylo(TobNylocas.Breed breed, boolean big, boolean aggressive, BlockPos pos) {
            Mob m = spawnMob(breed.creatureId, pos, false);
            if (m == null) {
                return;
            }
            if (big) {
                raid.setHitpoints(m, com.runecraft.osrs.content.raids.RaidScaling.tobScaledHp(breed.creatureId, TobNylocas.BIG_HP,
                        raid.partySize(), raid.mode()));
            }
            if (!aggressive) {
                m.setTarget(null);
            }
            nylos.add(new Nylo(m, breed, big, aggressive));
        }

        private void spawnWave(int number) {
            TobNylocas.Wave w = TobNylocas.wave(number);
            for (TobNylocas.Lane lane : TobNylocas.Lane.values()) {
                int[] g = LANES[lane.ordinal()];
                boolean south = lane == TobNylocas.Lane.SOUTH;
                int k = 0;
                for (TobNylocas.Nylo n : w.lane(lane)) {
                    int off = k * 2 - 1;
                    spawnNylo(n.breed(), n.big(), n.aggressive(), at(g[0] + (south ? off : 0), g[1] + (south ? 0 : off)));
                    k++;
                }
            }
            say("Wave " + number + "/" + TobNylocas.WAVES + " of Nylocas approaches!");
            if (hard() && TobNylocas.prinkipasWave(number) && !alive(prinkipas)) {
                prinkipas = spawnBoss("nylocas_prinkipas", at(0, 0), false);
                prinkipasForm = 0;
                prinkipasTimer = TobNylocas.FORM_TICKS;
                if (prinkipas != null) {
                    anchor(prinkipas);
                    warn("A Nylocas Prinkipas drops down in its " + FORMS[0] + " form!");
                }
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            tickNylos();
            if (standing() == 0) {
                return;
            }
            if (alive(prinkipas) && --prinkipasTimer <= 0) {
                prinkipasTimer = TobNylocas.FORM_TICKS;
                prinkipasForm = TobNylocas.nextForm(prinkipasForm, rnd());
                prinkipasChanged = true;
                warn("The Nylocas Prinkipas changes to its " + FORMS[prinkipasForm] + " form!");
            }
            if (vasilias == null) {
                if (wave < TobNylocas.WAVES) {
                    if (--waveTimer <= 0 && nylos.size() < TobNylocas.cap(wave + 1)) {
                        wave++;
                        waveTimer = TobNylocas.WAVE_INTERVAL_TICKS;
                        spawnWave(wave);
                    }
                } else if (nylos.isEmpty() && !alive(prinkipas)) {
                    vasilias = spawnBoss("nylocas_vasilias", at(0, 0));
                    form = 0;
                    formTimer = formTicks();
                    warn("The Nylocas Vasilias emerges in its " + FORMS[form] + " form!");
                }
                return;
            }
            if (!alive(vasilias)) {
                return;
            }
            if (wrongStyle) {
                com.runecraft.content.systems.CombatAchievementService.flagNear(vasilias, CombatAchievements.FLAG_NYLO_WRONG_STYLE);
            }
            if (pillarLost) {
                com.runecraft.content.systems.CombatAchievementService.flagNear(vasilias, CombatAchievements.FLAG_NYLO_PILLAR_LOST);
            }
            if (prinkipasChanged) {
                com.runecraft.content.systems.CombatAchievementService.flagNear(vasilias, CombatAchievements.FLAG_PRINKIPAS_CHANGED);
            }
            if (--formTimer <= 0) {
                formTimer = formTicks();
                form = TobNylocas.nextForm(form, rnd());
                warn("The Nylocas Vasilias changes to its " + FORMS[form] + " form!");
            }
            ParticleOptions colour = switch (form) {
                case 0 -> ParticleTypes.CRIT;
                case 1 -> ParticleTypes.HAPPY_VILLAGER;
                default -> ParticleTypes.SOUL_FIRE_FLAME;
            };
            world().sendParticles(colour, vasilias.getX(), vasilias.getY() + 1, vasilias.getZ(), 6, 1, 0.5, 1, 0.01);
        }

        /** Entry mode Vasilias changes form more slowly. // approx: 14 ticks */
        private int formTicks() {
            return raid.mode() == com.runecraft.osrs.content.raids.RaidMode.ENTRY ? 14 : TobNylocas.FORM_TICKS;
        }

        private void tickNylos() {
            List<Nylo> split = new ArrayList<>();
            java.util.Iterator<Nylo> it = nylos.iterator();
            while (it.hasNext()) {
                Nylo n = it.next();
                if (!alive(n.mob)) {
                    it.remove();
                    if (n.big && n.mob.isDeadOrDying()) {
                        split.add(n);
                    }
                    continue;
                }
                if (++n.age >= TobNylocas.LIFETIME_TICKS) {
                    explode(n);
                    it.remove();
                    continue;
                }
                if (n.aggressive) {
                    if (!(n.mob.getTarget() instanceof ServerPlayer current) || !current.isAlive()) {
                        ServerPlayer t = randomPlayer();
                        if (t != null) {
                            n.mob.setTarget(t);
                        }
                    }
                    continue;
                }
                n.mob.setTarget(null);
                int p = nearestPillar(n.mob.position());
                if (p < 0) {
                    continue;
                }
                Vec3 pc = Vec3.atBottomCenterOf(pillarBase(p).above());
                double d2 = RaidEffects.horizontalDistSq(n.mob.position(), pc);
                if (d2 > 2.6 * 2.6) {
                    if (n.age % 8 == 1 || n.mob.getNavigation().isDone()) {
                        n.mob.getNavigation().moveTo(pc.x, pc.y, pc.z, 1.0);
                    }
                } else if (n.age % 3 == 0) {
                    // Nylocas attack every 3 ticks.
                    pillarHp[p] -= TobNylocas.pillarDamage(n.big, rnd());
                    world().sendParticles(ParticleTypes.CRIT, pc.x, pc.y + 1, pc.z, 2, 0.6, 0.6, 0.6, 0.01);
                    if (pillarHp[p] <= 0) {
                        collapse(p);
                    }
                }
            }
            for (Nylo n : split) {
                // OSRS: a slain big nylocas splits into two small nylocas of any breed.
                BlockPos at = n.mob.blockPosition();
                TobNylocas.Breed[] breeds = TobNylocas.Breed.values();
                for (int i = 0; i < 2; i++) {
                    spawnNylo(breeds[rnd().nextInt(breeds.length)], false, n.aggressive, at.offset(i, 0, 0));
                }
            }
        }

        private int nearestPillar(Vec3 pos) {
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < PILLARS.length; i++) {
                if (pillarHp[i] <= 0) {
                    continue;
                }
                double d = RaidEffects.horizontalDistSq(pos, Vec3.atBottomCenterOf(pillarBase(i)));
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            return best;
        }

        /** A nylocas self-destructs: players next to it take up to 18 (small) or 21 (big). */
        private void explode(Nylo n) {
            Vec3 pos = n.mob.position();
            world().sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 1, 0, 0, 0, 0);
            int max = n.big ? TobNylocas.BIG_EXPLOSION : TobNylocas.SMALL_EXPLOSION;
            double r = n.big ? 2.5 : 1.8;
            for (ServerPlayer p : players()) {
                if (p.position().distanceToSqr(pos) <= r * r) {
                    RaidEffects.hit(n.mob, p, rnd().nextInt(max + 1), null, true);
                }
            }
            n.mob.discard();
        }

        /** A support breaks: rocks fall on the whole team (up to 50); with all four gone the roof caves in. */
        private void collapse(int i) {
            pillarHp[i] = 0;
            pillarLost = true;
            BlockPos b = pillarBase(i);
            RaidBuilder.pillar(world(), b, 1, 5, Blocks.AIR.defaultBlockState());
            RaidBuilder.pillar(world(), b, 1, 1, Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState());
            world().sendParticles(ParticleTypes.EXPLOSION, b.getX() + 0.5, b.getY() + 2, b.getZ() + 0.5, 4, 1, 1, 1, 0);
            if (standing() == 0) {
                warn("The last support collapses and the roof caves in!");
                for (ServerPlayer p : players()) {
                    RaidEffects.hit(null, p, Math.round(p.getMaxHealth()) + 100, null, true);
                }
                return;
            }
            warn("A support collapses! Rocks fall from the roof! (" + standing() + " left)");
            for (ServerPlayer p : players()) {
                RaidEffects.hit(null, p, rnd().nextInt(TobNylocas.PILLAR_COLLAPSE_MAX_HIT + 1), null, true);
            }
        }

        /** A player attacked a wave nylocas: the wrong style can't harm it (ToB "Perfect Nylocas"). */
        void attacked(LivingEntity target, AttackType type) {
            TobNylocas.Breed b = TobNylocas.Breed.byCreature(RaidInstance.creatureId(target));
            if (b != null && type != null && !b.weakTo(type)) {
                wrongStyle = true;
            }
        }

        @Override
        public AttackDef bossAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss == prinkipas) {
                return formAttack(prinkipasForm, TobNylocas.PRINKIPAS_MAX_HIT);
            }
            return formAttack(form, TobNylocas.VASILIAS_MAX_HIT);
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            int f = boss == prinkipas ? prinkipasForm : form;
            if (TobNylocas.formWeakTo(f, type)) {
                return damage;
            }
            // OSRS: the wrong style is reflected onto the attacker and heals the boss by the same amount.
            wrongStyle = true;
            if (damage > 0 && attacker != null) {
                Msg.action(attacker, "It is in its " + FORMS[f] + " form - your attack is reflected!");
                RaidEffects.hit(boss, attacker, damage, null, true);
                raid.heal(boss, damage);
            }
            return 0;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (Nylo n : nylos) {
                if (n.mob.isAlive()) {
                    n.mob.discard();
                }
            }
            nylos.clear();
            if (prinkipas != null && prinkipas.isAlive()) {
                prinkipas.discard();
            }
            for (TobNylocas.Breed b : TobNylocas.Breed.values()) {
                living(b.creatureId).forEach(LivingEntity::discard);
            }
        }
    }

    /**
     * Sotetseg: shared death balls, and the shadow-realm maze at 66% and 33% (OSRS wiki). One player (Hard mode: all but
     * one) is dragged into the shadow realm and follows the red path; the players left in the arena must walk the same
     * path on the arena floor, seeing only the runner's position, and every wrong tile hurts.
     */
    static final class Sotetseg extends RoomLogic {
        private static final int COLS = 7;
        private static final int ROWS = 14;
        private DataBossEntity sotetseg;
        private final Set<Long> path = new HashSet<>();
        private final Set<UUID> runners = new HashSet<>();
        private int mazeTicks;
        private boolean maze66;
        private boolean maze33;
        private int ballCooldown = 20;

        Sotetseg(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        private BlockPos mazeOrigin() {
            return center().offset(-COLS, 0, RaidBuilder.MAZE_OFFSET_Z - ROWS);
        }

        /** The arena copy of the maze: the same grid laid on the arena floor, west of Sotetseg. */
        private BlockPos arenaOrigin() {
            return center().offset(-COLS, 0, -ROWS);
        }

        @Override
        public void decorate() {
            BlockPos o = mazeOrigin();
            int y = o.getY();
            RaidBuilder.loadChunks(world(), o.getX() - 4, o.getZ() - 6, o.getX() + COLS * 2 + 4, o.getZ() + ROWS * 2 + 6);
            RaidBuilder.fill(world(), o.getX() - 2, y - 1, o.getZ() - 5, o.getX() + COLS * 2 + 1, y - 1, o.getZ() + ROWS * 2 + 4,
                    Blocks.OBSIDIAN.defaultBlockState());
            RaidBuilder.fill(world(), o.getX() - 2, y, o.getZ() - 5, o.getX() + COLS * 2 + 1, y + 3, o.getZ() + ROWS * 2 + 4,
                    Blocks.RED_STAINED_GLASS.defaultBlockState());
            RaidBuilder.fill(world(), o.getX() - 1, y + 1, o.getZ() - 4, o.getX() + COLS * 2, y + 3, o.getZ() + ROWS * 2 + 3,
                    Blocks.AIR.defaultBlockState());
            RaidBuilder.fill(world(), o.getX() - 1, y, o.getZ() - 4, o.getX() + COLS * 2, y, o.getZ() - 1,
                    Blocks.CRIMSON_NYLIUM.defaultBlockState());
            RaidBuilder.fill(world(), o.getX() - 1, y, o.getZ() + ROWS * 2, o.getX() + COLS * 2, y, o.getZ() + ROWS * 2 + 3,
                    Blocks.RED_NETHER_BRICKS.defaultBlockState());
            RaidBuilder.fill(world(), o.getX(), y, o.getZ(), o.getX() + COLS * 2 - 1, y, o.getZ() + ROWS * 2 - 1,
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            // The arena's tiled maze floor (the path itself is invisible from the arena).
            BlockPos a = arenaOrigin();
            RaidBuilder.fill(world(), a.getX(), a.getY(), a.getZ(), a.getX() + COLS * 2 - 1, a.getY(), a.getZ() + ROWS * 2 - 1,
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(-12, 0, -8), 1, 2, Blocks.CRIMSON_HYPHAE.defaultBlockState()); // clear of the arena maze
            RaidBuilder.pillar(world(), center().offset(-12, 0, 8), 1, 2, Blocks.CRIMSON_HYPHAE.defaultBlockState());
        }

        @Override
        public void start() {
            maze66 = false;
            maze33 = false;
            runners.clear();
            sotetseg = spawnBoss("sotetseg", at(13, 0));
            anchor(sotetseg);
        }

        private static long key(int col, int row) {
            return ((long) col << 32) | (row & 0xffffffffL);
        }

        private ServerPlayer player(UUID id) {
            for (ServerPlayer p : raid.activePlayers()) {
                if (p.getUUID().equals(id)) {
                    return p;
                }
            }
            return world().getServer().getPlayerList().getPlayer(id);
        }

        private void startMaze() {
            List<ServerPlayer> party = players();
            if (party.isEmpty()) {
                return;
            }
            path.clear();
            int col = rnd().nextInt(COLS);
            for (int r = 0; r < ROWS; r++) {
                path.add(key(col, r));
                int next = Mth.clamp(col + rnd().nextInt(5) - 2, 0, COLS - 1);
                for (int c = Math.min(col, next); c <= Math.max(col, next); c++) {
                    path.add(key(c, r));
                }
                col = next;
            }
            // OSRS: one player is taken; in Hard mode all but one (they share the maze between them). // approx: each
            // runner walks the whole path
            java.util.Collections.shuffle(party, new java.util.Random(rnd().nextLong()));
            int taken = hard() && party.size() > 1 ? party.size() - 1 : 1;
            runners.clear();
            mazeTicks = 0;
            BlockPos o = mazeOrigin();
            for (int i = 0; i < taken; i++) {
                ServerPlayer p = party.get(i);
                runners.add(p.getUUID());
                raid.teleport(p, new BlockPos(o.getX() + COLS - taken / 2 + i, o.getY() + 1, o.getZ() - 2));
                warn("Sotetseg drags " + p.getName().getString() + " into the shadow realm! Find the hidden path.");
                Msg.game(p, "Follow the souls through the maze. Stepping off the path hurts.");
            }
            for (int i = taken; i < party.size(); i++) {
                Msg.game(party.get(i), "Follow the red glow across the tiled floor: only the shadow realm runner can see the path.");
            }
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return runners.isEmpty();
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(sotetseg)) {
                return;
            }
            if (!runners.isEmpty()) {
                mazeTick();
                return;
            }
            double hp = raid.healthFraction(sotetseg);
            if ((hp <= 0.66 && !maze66) || (hp <= 0.33 && !maze33)) {
                if (hp <= 0.33) {
                    maze33 = true;
                }
                maze66 = true;
                startMaze();
                return;
            }
            autoAttack(sotetseg, tick, 5);
            if (--ballCooldown <= 0) {
                // OSRS: the ball of death follows every 10 projectiles (one every 5 ticks); Hard mode launches two at a time.
                ballCooldown = 50;
                List<ServerPlayer> party = players();
                int balls = hard() && party.size() > 1 ? 2 : 1;
                java.util.Collections.shuffle(party, new java.util.Random(rnd().nextLong()));
                for (int i = 0; i < Math.min(balls, party.size()); i++) {
                    deathBall(party.get(i));
                }
            }
        }

        private void deathBall(ServerPlayer target) {
            warn("Sotetseg launches a ball of death at " + target.getName().getString() + "! Stand together to share it!");
            RaidEffects.later(4, () -> {
                if (!target.isAlive() || !alive(sotetseg)) {
                    return;
                }
                List<ServerPlayer> share = new ArrayList<>();
                for (ServerPlayer p : players()) {
                    if (p.distanceToSqr(target) <= 3.5 * 3.5) {
                        share.add(p);
                    }
                }
                if (!share.contains(target)) {
                    share.add(target);
                }
                world().sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 3, 1, 0.5, 1, 0);
                // OSRS: up to 188 split between everyone within 3x3 of the target. // approx: scaled like the HP by party
                int each = (int) (com.runecraft.osrs.content.raids.RaidScaling.TOB_SOTETSEG_BALL * com.runecraft.osrs.content.raids.RaidScaling.tobHp(raid.partySize())) / share.size();
                for (ServerPlayer p : share) {
                    RaidEffects.hit(sotetseg, p, each, null, true);
                }
            });
        }

        private void mazeTick() {
            BlockPos o = mazeOrigin();
            mazeTicks++;
            ServerPlayer lead = null;
            int leadRow = -1;
            for (UUID id : new ArrayList<>(runners)) {
                ServerPlayer p = player(id);
                if (p == null || !p.isAlive() || raid.isDowned(p)) {
                    runners.remove(id);
                    continue;
                }
                int col = (int) Math.floor((p.getX() - o.getX()) / 2);
                int row = (int) Math.floor((p.getZ() - o.getZ()) / 2);
                if (p.getZ() >= o.getZ() + ROWS * 2) {
                    raid.teleport(p, at(-14, 0));
                    say(p.getName().getString() + " escapes the shadow realm!");
                    runners.remove(id);
                    continue;
                }
                if (mazeTicks > 60) {
                    raid.teleport(p, at(-14, 0));
                    RaidEffects.hit(sotetseg, p, 30, null, true);
                    Msg.red(p, "The shadow realm spits you out.");
                    runners.remove(id);
                    continue;
                }
                if (mazeTicks % 7 == 0) {
                    // OSRS: the shadow realm chips the runner for 1-3 every 7 ticks.
                    RaidEffects.hit(sotetseg, p, 1 + rnd().nextInt(3), null, true);
                }
                if (col >= 0 && col < COLS && row >= 0 && row < ROWS && !path.contains(key(col, row)) && !cooldown(p, 2)) {
                    RaidEffects.hit(sotetseg, p, 15, null, true);
                    Msg.red(p, "The shadows lash at you!");
                }
                int from = Math.max(0, row);
                for (int r = from; r < Math.min(ROWS, from + 3); r++) {
                    for (int c = 0; c < COLS; c++) {
                        if (path.contains(key(c, r))) {
                            world().sendParticles(ParticleTypes.SOUL, o.getX() + c * 2 + 1, o.getY() + 1.1, o.getZ() + r * 2 + 1, 1, 0.3, 0,
                                    0.3, 0);
                        }
                    }
                }
                if (row > leadRow) {
                    lead = p;
                    leadRow = row;
                }
            }
            if (lead != null) {
                arenaTick(o, lead);
            }
        }

        /**
         * The players left in the arena see only the runner's position (a red glow on the arena's tiles) and must walk
         * the same path: a wrong tile hits for 15 plus 6.67% of current hitpoints every tick (OSRS).
         */
        private void arenaTick(BlockPos o, ServerPlayer lead) {
            BlockPos a = arenaOrigin();
            int col = (int) Math.floor((lead.getX() - o.getX()) / 2);
            int row = (int) Math.floor((lead.getZ() - o.getZ()) / 2);
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                world().sendParticles(ParticleTypes.CRIMSON_SPORE, a.getX() + col * 2 + 1, a.getY() + 1.1, a.getZ() + row * 2 + 1, 6, 0.5,
                        0.1, 0.5, 0);
            }
            for (ServerPlayer p : players()) {
                if (runners.contains(p.getUUID())) {
                    continue;
                }
                int c = (int) Math.floor((p.getX() - a.getX()) / 2);
                int r = (int) Math.floor((p.getZ() - a.getZ()) / 2);
                if (c >= 0 && c < COLS && r >= 0 && r < ROWS && !path.contains(key(c, r))) {
                    RaidEffects.hit(sotetseg, p, 15 + (int) (p.getHealth() * 0.0667), null, true);
                    if (!cooldown(p, 4)) {
                        Msg.red(p, "That tile isn't on the path!");
                    }
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (UUID id : runners) {
                ServerPlayer p = player(id);
                if (p != null && p.isAlive()) {
                    raid.teleport(p, at(-14, 0));
                }
            }
            runners.clear();
        }
    }

    /** Xarpus: exhumed acid to absorb, acid spit, then the reflecting screech phase. */
    static final class Xarpus extends RoomLogic {
        private record Exhumed(Vec3 pos, long expires) {
        }

        private DataBossEntity xarpus;
        private int phase;
        private float facing;
        /** Exhumed Xarpus absorbed in phase 1 (each makes his poison pools hurt more). */
        private int absorbed;
        private final List<Exhumed> exhumeds = new ArrayList<>();

        Xarpus(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() - 2, c.getY(), c.getZ() - 2, c.getX() + 2, c.getY(), c.getZ() + 2,
                    Blocks.DARK_PRISMARINE.defaultBlockState());
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.set(world(), c.offset(sx * 3, 1, sz * 3), Blocks.SEA_PICKLE.defaultBlockState());
                }
            }
        }

        @Override
        public void start() {
            phase = 1;
            absorbed = 0;
            exhumeds.clear();
            xarpus = spawnBoss("xarpus", at(0, 0));
            if (xarpus != null) {
                anchor(xarpus);
                xarpus.setNoAi(true);
                // OSRS: Xarpus starts severely weakened; the exhumeds can restore the missing quarter of his health.
                raid.setCurrentHitpoints(xarpus, raid.maxHitpoints(xarpus) * 3 / 4);
            }
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return phase > 1;
        }

        @Override
        public void bossTick(DataBossEntity boss) {
            if (phase == 3) {
                boss.setTarget(null);
                boss.setYRot(facing);
                boss.setYHeadRot(facing);
                boss.setYBodyRot(facing);
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(xarpus)) {
                return;
            }
            long now = ActionManager.gameTick();
            List<ServerPlayer> players = players();
            if (phase == 1) {
                if (tick % 3 == 0 && !players.isEmpty()) {
                    ServerPlayer p = players.get(rnd().nextInt(players.size()));
                    exhumeds.add(new Exhumed(p.position().add(rnd().nextInt(9) - 4, 0, rnd().nextInt(9) - 4), now + 8));
                }
                for (int i = exhumeds.size() - 1; i >= 0; i--) {
                    Exhumed e = exhumeds.get(i);
                    world().sendParticles(ParticleTypes.SNEEZE, e.pos().x, e.pos().y + 0.1, e.pos().z, 5, 0.3, 0.02, 0.3, 0);
                    if (now >= e.expires()) {
                        exhumeds.remove(i);
                        ServerPlayer absorber = null;
                        for (ServerPlayer p : players) {
                            if (RaidEffects.horizontalDistSq(p.position(), e.pos()) < 1.2) {
                                absorber = p;
                                break;
                            }
                        }
                        if (absorber != null) {
                            RaidEffects.hit(null, absorber, 3, null, true);
                            absorber.heal(3);
                        } else {
                            raid.heal(xarpus, Math.max(1, raid.maxHitpoints(xarpus) / 25));
                            absorbed++;
                            say("Xarpus absorbs the exhumed acid!");
                            if (raid.healthFraction(xarpus) >= 0.999) {
                                com.runecraft.content.systems.CombatAchievementService.flagNear(xarpus, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_XARPUS_FULL_HEAL);
                            }
                        }
                    }
                }
                if (raid.roomTicks() >= 30) {
                    phase = 2;
                    exhumeds.clear();
                    xarpus.setNoAi(false);
                    warn("Xarpus awakens!");
                }
                return;
            }
            if (phase == 2 && tick % 6 == 0 && !players.isEmpty()) {
                // OSRS: the poison splashes a 3x3 area lightly and then stays on the floor for the rest of the fight,
                // dealing 4-8 a tick, more for every exhumed Xarpus absorbed. // approx: 6 + 1 per exhumed, up to 16
                ServerPlayer p = players.get(rnd().nextInt(players.size()));
                Vec3 at = p.position();
                int pool = Math.min(16, 6 + absorbed);
                RaidEffects.strike(world(), at, 1.6, 8, 3, xarpus, ParticleTypes.ITEM_SLIME);
                RaidEffects.later(3, () -> Mechanics.addHazard(world(), at, pool, 3000, xarpus));
            }
            if (phase == 2 && raid.healthFraction(xarpus) <= 0.25) {
                phase = 3;
                warn("Xarpus shrieks and turns away! Do not attack him while he faces you!");
            }
            if (phase == 3) {
                if (raid.roomTicks() % 8 == 0) {
                    facing = new float[]{0, 90, 180, -90}[rnd().nextInt(4)];
                    say("Xarpus turns to face another direction.");
                }
                // OSRS: in this phase Xarpus stops spitting poison and only stares at one quadrant at a time.
            }
        }

        @Override
        protected void onComplete() {
            super.onComplete();
            // The poison pools last for the rest of the fight, not beyond it.
            Mechanics.clearHazards(world(), RaidBuilder.roomBox(raid.slot, index));
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (phase != 3) {
                return damage;
            }
            double dx = attacker.getX() - boss.getX();
            double dz = attacker.getZ() - boss.getZ();
            float angle = (float) Math.toDegrees(Math.atan2(-dx, dz));
            if (Math.abs(Mth.wrapDegrees(angle - facing)) < 50) {
                // OSRS: attacking from the quadrant he stares at draws a poison retaliation of 50-75.
                Msg.red(attacker, "Xarpus spits poison back at you!");
                com.runecraft.content.systems.CombatAchievementService.flagNear(boss, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_XARPUS_REFLECT);
                RaidEffects.hit(boss, attacker, 50 + rnd().nextInt(26), null, true);
                return damage;
            }
            return damage;
        }
    }

    /** Verzik Vitur: throne blasts (hide behind pillars), her second form, then the spider form. */
    static final class Verzik extends RoomLogic {
        private static final int[][] PILLARS = {{0, -7}, {0, 7}, {-8, -7}, {-8, 7}};
        private DataBossEntity verzik;
        private int form;
        private final Map<UUID, Vec3> lastPos = new HashMap<>();
        private boolean purpleSpawned;

        Verzik(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 12, c.getY(), c.getZ() - 4, c.getX() + 18, c.getY() + 1, c.getZ() + 4,
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            RaidBuilder.fill(world(), c.getX() + 12, c.getY() + 1, c.getZ() - 1, c.getX() + 13, c.getY() + 1, c.getZ() + 1,
                    Blocks.RED_CARPET.defaultBlockState());
            pillars(Blocks.STONE_BRICKS.defaultBlockState());
        }

        private void pillars(net.minecraft.world.level.block.state.BlockState state) {
            for (int[] p : PILLARS) {
                RaidBuilder.pillar(world(), center().offset(p[0], 0, p[1]), 1, 5, state);
            }
        }

        /** Phase 1 blasts each supporting pillar has absorbed; -1 once it collapsed. */
        private final int[] pillarBlasts = new int[PILLARS.length];
        private boolean pillarLost;

        private Vec3 pillarCentre(int i) {
            return Vec3.atBottomCenterOf(center().offset(PILLARS[i][0], 1, PILLARS[i][1]));
        }

        /** The standing pillar sheltering a player from Verzik (the nearest one between them), or -1. */
        private int shelter(ServerPlayer p) {
            int best = -1;
            double bestD = 3.5 * 3.5;
            for (int i = 0; i < PILLARS.length; i++) {
                if (pillarBlasts[i] < 0) {
                    continue;
                }
                double d = RaidEffects.horizontalDistSq(p.position(), pillarCentre(i));
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            return best;
        }

        /**
         * A pillar collapses (OSRS wiki): players next to it take up to 70 (50 in Entry mode) and are stunned and pushed
         * away.
         */
        private void collapse(int i) {
            pillarBlasts[i] = -1;
            Vec3 c = pillarCentre(i);
            RaidBuilder.pillar(world(), center().offset(PILLARS[i][0], 0, PILLARS[i][1]), 1, 5, Blocks.AIR.defaultBlockState());
            world().sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1, c.z, 4, 1, 1, 1, 0);
            int max = com.runecraft.osrs.content.raids.RaidScaling.verzikPillarCollapse(raid.mode());
            for (ServerPlayer p : players()) {
                if (RaidEffects.horizontalDistSq(p.position(), c) <= 2.6 * 2.6) {
                    RaidEffects.hit(verzik, p, rnd().nextInt(max + 1), null, true);
                    com.runecraft.content.systems.SysCombat.freeze(p, 3);
                    Vec3 push = p.position().subtract(c).multiply(1, 0, 1).normalize().scale(0.8);
                    p.push(push.x, 0.3, push.z);
                    p.hurtMarked = true;
                    Msg.red(p, "The pillar collapses on top of you!");
                }
            }
        }

        @Override
        public void start() {
            form = 1;
            purpleSpawned = false;
            java.util.Arrays.fill(pillarBlasts, 0);
            pillarLost = false;
            verzik = spawnBoss("verzik_vitur", at(15, 0).above());
            if (verzik != null) {
                // Phase 1 only attacks with the room's pillar-blocked blast (OSRS); the generic attack AI would hit through pillars.
                verzik.setNoAi(true);
            }
            anchor(verzik);
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            // Phase 1: without a Dawnbringer every hit is capped at 10 (as in OSRS).
            return form == 1 ? Math.min(damage, 10) : damage;
        }

        @Override
        public void gameTick(long tick) {
            if (verzik == null) {
                return;
            }
            Vec3 pos = verzik.position();
            if (!alive(verzik)) {
                if (form == 1) {
                    form = 2;
                    // Verzik leaves her throne and every remaining pillar collapses.
                    for (int i = 0; i < PILLARS.length; i++) {
                        if (pillarBlasts[i] >= 0) {
                            collapse(i);
                        }
                    }
                    pillars(Blocks.AIR.defaultBlockState());
                    verzik = spawnBoss("verzik_vitur_p2", BlockPos.containing(pos).offset(-3, 0, 0));
                    warn("Verzik Vitur: Let's see how you handle this! The pillars crumble!");
                } else if (form == 2) {
                    form = 3;
                    verzik = spawnBoss("verzik_vitur_p3", BlockPos.containing(pos));
                    warn("Verzik Vitur: Behold my true nature!");
                } else {
                    complete();
                }
                return;
            }
            List<ServerPlayer> players = players();
            switch (form) {
                case 1 -> phaseOne(tick, players);
                case 2 -> phaseTwo(tick, players);
                default -> phaseThree(tick, players);
            }
        }

        private void phaseOne(long tick, List<ServerPlayer> players) {
            if (pillarLost) {
                com.runecraft.content.systems.CombatAchievementService.flagNear(verzik, CombatAchievements.FLAG_VERZIK_PILLAR_LOST);
            }
            if (raid.roomTicks() % 14 == 6) {
                warn("Verzik charges a blast of energy! Hide behind a pillar!");
                RaidEffects.later(2, () -> {
                    if (!alive(verzik) || form != 1) {
                        return;
                    }
                    Set<Integer> struck = new HashSet<>();
                    for (ServerPlayer p : players()) {
                        if (canSee(verzik, p)) {
                            Vec3 a = verzik.getEyePosition();
                            Vec3 b = p.getEyePosition();
                            for (int i = 0; i <= 10; i++) {
                                Vec3 v = a.lerp(b, i / 10.0);
                                world().sendParticles(ParticleTypes.ELECTRIC_SPARK, v.x, v.y, v.z, 2, 0.1, 0.1, 0.1, 0);
                            }
                            // OSRS: an unshielded P1 blast hits up to 137 in normal mode.
                            RaidEffects.hit(verzik, p, rnd().nextInt(138), null, true);
                        } else {
                            Msg.action(p, "The pillar shields you from the blast.");
                            int pillar = shelter(p);
                            if (pillar >= 0) {
                                struck.add(pillar);
                            }
                        }
                    }
                    // Several players behind one pillar only damage it once.
                    for (int i : struck) {
                        if (pillarBlasts[i] >= 0 && ++pillarBlasts[i] >= com.runecraft.osrs.content.raids.RaidScaling.VERZIK_PILLAR_BLASTS) {
                            pillarLost = true;
                            warn("A supporting pillar crumbles under Verzik's blasts!");
                            collapse(i);
                        }
                    }
                });
            }
        }

        private void phaseTwo(long tick, List<ServerPlayer> players) {
            autoAttack(verzik, tick, 4);
            if (tick % 20 == 0) {
                say("Verzik summons lightning!");
                for (ServerPlayer p : players) {
                    RaidEffects.strike(world(), p.position(), 1.4, 30, 3, verzik, ParticleTypes.ELECTRIC_SPARK);
                }
            }
            if (tick % 30 == 15) {
                for (int i = 0; i < 2; i++) {
                    spawnMob("nylocas_matomenos", BlockPos.containing(verzik.position()).offset(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2),
                            false);
                }
                say("Verzik spawns explosive Nylocas!");
            }
            for (LivingEntity crab : living("nylocas_matomenos")) {
                ServerPlayer nearest = null;
                double best = Double.MAX_VALUE;
                for (ServerPlayer p : players) {
                    double d = p.distanceToSqr(crab);
                    if (d < best) {
                        best = d;
                        nearest = p;
                    }
                }
                if (nearest != null) {
                    walk(crab, nearest.position());
                }
                if (best < 1.8 * 1.8 || crab.tickCount > 20 * ActionManager.MC_TICKS_PER_GAME_TICK) {
                    world().sendParticles(ParticleTypes.EXPLOSION, crab.getX(), crab.getY() + 0.5, crab.getZ(), 2, 0.4, 0.4, 0.4, 0);
                    for (ServerPlayer p : players) {
                        if (p.distanceToSqr(crab) < 2.5 * 2.5) {
                            RaidEffects.hit(verzik, p, 10 + rnd().nextInt(16), null, true);
                        }
                    }
                    crab.discard();
                }
            }
            if (!purpleSpawned && raid.healthFraction(verzik) < 0.5) {
                purpleSpawned = true;
                spawnMob("nylocas_athanatos", at(-10, rnd().nextBoolean() ? -10 : 10), false);
                warn("A Nylocas Athanatos crawls towards Verzik to heal her - kill it!");
            }
            healers("nylocas_athanatos", verzik, 0.75, "The Nylocas Athanatos heals Verzik!");
        }

        private void phaseThree(long tick, List<ServerPlayer> players) {
            if (players.isEmpty()) {
                return;
            }
            long t = raid.roomTicks();
            // Hard mode: Verzik spins webs more often and in greater number. // approx
            if (t % (hard() ? 20 : 30) == 10) {
                say("Verzik spins sticky webs!");
                List<BlockPos> webs = new ArrayList<>();
                int count = 6 + players.size() * 2;
                for (int i = 0; i < (hard() ? count * 3 / 2 : count); i++) {
                    ServerPlayer p = players.get(rnd().nextInt(players.size()));
                    BlockPos w = p.blockPosition().offset(rnd().nextInt(7) - 3, 0, rnd().nextInt(7) - 3);
                    if (world().getBlockState(w).isAir() && inside(Vec3.atBottomCenterOf(w))) {
                        RaidBuilder.set(world(), w, Blocks.COBWEB.defaultBlockState());
                        webs.add(w);
                    }
                }
                RaidEffects.later(15, () -> {
                    for (BlockPos w : webs) {
                        if (world().getBlockState(w).is(Blocks.COBWEB)) {
                            RaidBuilder.set(world(), w, Blocks.AIR.defaultBlockState());
                        }
                    }
                });
            }
            if (t % 40 == 25) {
                warn("Verzik spits yellow pools! Stand in one to lessen the damage!");
                List<Vec3> pools = new ArrayList<>();
                for (ServerPlayer p : players) {
                    pools.add(p.position().add(rnd().nextInt(7) - 3, 0, rnd().nextInt(7) - 3));
                }
                for (int burst = 0; burst < 3; burst++) {
                    for (int s = 0; s < 2; s++) {
                        RaidEffects.later(1 + burst * 2 + s, () -> {
                            for (Vec3 v : pools) {
                                world().sendParticles(ParticleTypes.WAX_ON, v.x, v.y + 0.1, v.z, 8, 0.4, 0.05, 0.4, 0);
                            }
                        });
                    }
                    RaidEffects.later(4 + burst * 2, () -> {
                        if (!alive(verzik)) {
                            return;
                        }
                        for (ServerPlayer p : players()) {
                            boolean safe = false;
                            for (Vec3 v : pools) {
                                if (RaidEffects.horizontalDistSq(p.position(), v) < 1.4) {
                                    safe = true;
                                    break;
                                }
                            }
                            RaidEffects.hit(verzik, p, safe ? 8 : 30, null, true);
                        }
                    });
                }
            }
            if (t % 25 == 5) {
                ServerPlayer target = players.get(rnd().nextInt(players.size()));
                warn("Verzik hurls a green ball at " + target.getName().getString() + "!");
                RaidEffects.later(4, () -> {
                    if (!target.isAlive() || !alive(verzik)) {
                        return;
                    }
                    ServerPlayer buddy = null;
                    for (ServerPlayer p : players()) {
                        if (p != target && p.distanceToSqr(target) < 36) {
                            buddy = p;
                            break;
                        }
                    }
                    world().sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0);
                    if (buddy != null) {
                        RaidEffects.hit(verzik, target, 25, AttackType.RANGED, false);
                        RaidEffects.hit(verzik, buddy, 25, AttackType.RANGED, false);
                    } else {
                        RaidEffects.hit(verzik, target, 50, null, true);
                    }
                });
            }
            if (raid.healthFraction(verzik) < 0.2 && tick % 2 == 0) {
                for (ServerPlayer p : players) {
                    Vec3 last = lastPos.put(p.getUUID(), p.position());
                    if (last != null) {
                        RaidEffects.strike(world(), last, 1.2, 15, 2, verzik, ParticleTypes.CLOUD);
                    }
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            if (alive(verzik)) {
                verzik.discard();
            }
            living("nylocas_matomenos").forEach(LivingEntity::discard);
            living("nylocas_athanatos").forEach(LivingEntity::discard);
        }
    }
}
