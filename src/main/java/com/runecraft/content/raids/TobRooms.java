package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Theatre of Blood rooms. */
final class TobRooms {
    private TobRooms() {
    }

    /** Moves passive crabs/spawn toward a point. */
    static void walk(LivingEntity e, Vec3d to) {
        if (e instanceof MobEntity mob) {
            mob.setTarget(null);
            if (mob.age % 10 == 0 || mob.getNavigation().isIdle()) {
                mob.getNavigation().startMovingTo(to.x, to.y, to.z, 1.0);
            }
        }
    }

    /** The Maiden of Sugadinti: nylocas heal her, blood spawn leave trails, blood splats everywhere. */
    static final class Maiden extends RoomLogic {
        private DataBossEntity maiden;
        private final Set<Integer> thresholds = new HashSet<>();

        Maiden(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int i = 0; i < 18; i++) {
                RaidBuilder.set(world(), center().add(rnd().nextInt(36) - 18, 0, rnd().nextInt(36) - 18),
                        Blocks.RED_CONCRETE.getDefaultState());
            }
            for (int z = -16; z <= 16; z += 8) {
                RaidBuilder.fill(world(), center().getX() + 20, center().getY() + 6, center().getZ() + z,
                        center().getX() + 20, center().getY() + 9, center().getZ() + z, Blocks.CHAIN.getDefaultState());
            }
        }

        @Override
        public void start() {
            thresholds.clear();
            maiden = spawnBoss("the_maiden_of_sugadinti", at(15, 0));
            anchor(maiden);
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(maiden)) {
                return;
            }
            autoAttack(maiden, tick, 10);
            double hp = raid.healthFraction(maiden);
            for (int t : new int[]{70, 50, 30}) {
                if (hp * 100 <= t && thresholds.add(t)) {
                    int n = Math.min(8, 2 + raid.partySize());
                    if (hard()) {
                        // Hard mode: the Maiden summons more Nylocas Matomenos. // approx: +50%
                        n = Math.min(12, n + n / 2);
                    }
                    for (int i = 0; i < n; i++) {
                        int z = (i % 2 == 0 ? -1 : 1) * 18;
                        spawnMob("nylocas_matomenos", at(4 + (i / 2) * 3, z), false);
                    }
                    warn("The Maiden calls upon the Nylocas! (" + t + "%)");
                }
            }
            healers("nylocas_matomenos", maiden, 1.0, "A Nylocas Matomenos reaches the Maiden and heals her!");
            List<LivingEntity> spawns = living("blood_spawn");
            if (tick % 20 == 0 && spawns.size() < (hard() ? 5 : 3)) {
                spawnMob("blood_spawn", at(11, rnd().nextInt(13) - 6), false);
            }
            for (LivingEntity s : spawns) {
                if (tick % 3 == 0) {
                    Mechanics.addHazard(world(), s.getPos(), 6, 10, maiden);
                }
                if (s.age % 60 == 0) {
                    walk(s, vec(rnd().nextInt(30) - 15, rnd().nextInt(30) - 15));
                }
            }
            if (tick % 8 == 4) {
                for (ServerPlayerEntity p : players()) {
                    if (rnd().nextInt(3) == 0) {
                        Vec3d at = p.getPos();
                        RaidEffects.strike(world(), at, 1.3, 25, 3, maiden, ParticleTypes.DAMAGE_INDICATOR, AttackType.MAGIC, false);
                        RaidEffects.later(3, () -> Mechanics.addHazard(world(), at, 5, 8, maiden));
                    }
                }
            }
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
        private int stateLength = 24;
        private int waypoint;

        Bloat(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        private Vec3d waypoint(int i) {
            int[][] corners = {{-11, -11}, {11, -11}, {11, 11}, {-11, 11}};
            int[] c = corners[Math.floorMod(i, 4)];
            return vec(c[0], c[1]);
        }

        @Override
        public void decorate() {
            RaidBuilder.pillar(world(), center(), 4, RaidBuilder.HEIGHT, Blocks.DEEPSLATE_BRICKS.getDefaultState());
            for (int i = 0; i < 6; i++) {
                RaidBuilder.set(world(), center().add(rnd().nextInt(36) - 18, RaidBuilder.HEIGHT, rnd().nextInt(36) - 18),
                        Blocks.CHAIN.getDefaultState());
            }
        }

        @Override
        public void start() {
            walking = true;
            stateTicks = 0;
            bloat = spawnBoss("pestilent_bloat", BlockPos.ofFloored(waypoint(0)));
        }

        @Override
        public void bossTick(DataBossEntity boss) {
            boss.setTarget(null);
            if (!walking) {
                boss.getNavigation().stop();
                return;
            }
            Vec3d wp = waypoint(waypoint);
            if (boss.getPos().squaredDistanceTo(wp) < 2.0) {
                waypoint++;
                wp = waypoint(waypoint);
            }
            if (boss.age % 10 == 0 || boss.getNavigation().isIdle()) {
                boss.getNavigation().startMovingTo(wp.x, wp.y, wp.z, 1.3);
            }
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return !walking;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(bloat)) {
                return;
            }
            stateTicks++;
            List<ServerPlayerEntity> players = players();
            if (walking) {
                if (tick % 4 == 0) {
                    List<ServerPlayerEntity> visible = new ArrayList<>();
                    for (ServerPlayerEntity p : players) {
                        if (canSee(bloat, p)) {
                            visible.add(p);
                        }
                    }
                    if (!visible.isEmpty() && !attacks(bloat).isEmpty()) {
                        Mechanics.perform(bloat, visible.get(rnd().nextInt(visible.size())), attacks(bloat).get(0), 1.0);
                    }
                }
                if (tick % 3 == 0 && !players.isEmpty()) {
                    ServerPlayerEntity p = players.get(rnd().nextInt(players.size()));
                    Vec3d at = p.getPos().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2);
                    RaidEffects.strike(world(), at, 1.5, 22, 3, bloat, ParticleTypes.FALLING_LAVA);
                }
                if (stateTicks >= stateLength) {
                    walking = false;
                    stateTicks = 0;
                    stateLength = 10;
                    say("The Bloat shuts down. Attack it now!");
                    com.runecraft.content.systems.CombatAchievementService.countNear(bloat, com.runecraft.osrs.content.systems.CombatAchievements.COUNT_BLOAT_SHUTDOWN);
                }
            } else {
                world().spawnParticles(ParticleTypes.SMOKE, bloat.getX(), bloat.getY() + 2, bloat.getZ(), 4, 0.8, 0.4, 0.8, 0.01);
                if (stateTicks == stateLength - 1) {
                    warn("The Bloat stirs...");
                }
                if (stateTicks >= stateLength) {
                    walking = true;
                    stateTicks = 0;
                    stateLength = 20 + rnd().nextInt(12);
                    world().spawnParticles(ParticleTypes.EXPLOSION_EMITTER, bloat.getX(), bloat.getY(), bloat.getZ(), 1, 0, 0, 0, 0);
                    for (ServerPlayerEntity p : players) {
                        if (p.squaredDistanceTo(bloat) < 4.5 * 4.5) {
                            RaidEffects.hit(bloat, p, 10 + rnd().nextInt(30), null, true);
                            Vec3d push = p.getPos().subtract(bloat.getPos()).normalize().multiply(1.2);
                            p.addVelocity(push.x, 0.5, push.z);
                            p.velocityModified = true;
                            Msg.red(p, "The Bloat stomps on you!");
                        }
                    }
                }
            }
        }
    }

    /** The Nylocas: waves of coloured nylocas (matching style only), then the Nylocas Vasilias. */
    static final class Nylocas extends RoomLogic {
        private static final String[] TYPES = {"nylocas_ischyros", "nylocas_toxobolos", "nylocas_hagios"};
        private static final String[] FORMS = {"grey (melee)", "green (ranged)", "blue (magic)"};
        private static final AttackDef[] FORM_ATTACKS = {
                AttackDef.of("bite", AttackType.STAB, 30, 3, 10),
                AttackDef.of("spit", AttackType.RANGED, 30, 12, 10).particle("minecraft:crit"),
                AttackDef.of("blast", AttackType.MAGIC, 30, 12, 10).particle("minecraft:witch")};
        private int wave;
        private int waves;
        private DataBossEntity vasilias;
        private int form;

        Nylocas(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.pillar(world(), center().add(sx * 5, 0, sz * 5), 1, 4, Blocks.POLISHED_DEEPSLATE.getDefaultState());
                    RaidBuilder.set(world(), center().add(sx * 5, 5, sz * 5), Blocks.SOUL_LANTERN.getDefaultState());
                }
            }
        }

        @Override
        public void start() {
            wave = 0;
            waves = 12;
            vasilias = null;
        }

        private int nylos() {
            int n = 0;
            for (String t : TYPES) {
                n += living(t).size();
            }
            return n;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (vasilias == null) {
                if (wave < waves && tick % 8 == 0) {
                    wave++;
                    int n = Math.min(6, 2 + raid.partySize() / 2 + wave / 5);
                    int[][] gates = {{0, -19}, {0, 19}, {19, 0}};
                    for (int i = 0; i < n; i++) {
                        int[] g = gates[rnd().nextInt(gates.length)];
                        spawnMob(TYPES[rnd().nextInt(3)], at(g[0] + rnd().nextInt(3) - 1, g[1] + rnd().nextInt(3) - 1), false);
                    }
                    say("Wave " + wave + "/" + waves + " of Nylocas approaches!");
                }
                if (wave >= waves && nylos() == 0) {
                    vasilias = spawnBoss("nylocas_vasilias", at(0, 0));
                    form = 0;
                    warn("The Nylocas Vasilias emerges in its " + FORMS[form] + " form!");
                }
                return;
            }
            if (!alive(vasilias)) {
                return;
            }
            if (raid.roomTicks() % 10 == 0) {
                form = (form + 1 + rnd().nextInt(2)) % 3;
                warn("The Nylocas Vasilias changes to its " + FORMS[form] + " form!");
            }
            ParticleEffect colour = switch (form) {
                case 0 -> ParticleTypes.CRIT;
                case 1 -> ParticleTypes.HAPPY_VILLAGER;
                default -> ParticleTypes.SOUL_FIRE_FLAME;
            };
            world().spawnParticles(colour, vasilias.getX(), vasilias.getY() + 1, vasilias.getZ(), 6, 1, 0.5, 1, 0.01);
        }

        @Override
        public AttackDef bossAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
            return FORM_ATTACKS[form];
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            boolean ok = switch (form) {
                case 0 -> type.isMelee();
                case 1 -> type == AttackType.RANGED;
                default -> type == AttackType.MAGIC;
            };
            if (!ok) {
                Msg.action(attacker, "The Vasilias is in its " + FORMS[form] + " form - your attack has no effect.");
                return 0;
            }
            return damage;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (String t : TYPES) {
                living(t).forEach(LivingEntity::discard);
            }
        }
    }

    /** Sotetseg: shared death balls, and the shadow-realm maze at 66% and 33%. */
    static final class Sotetseg extends RoomLogic {
        private static final int COLS = 7;
        private static final int ROWS = 14;
        private DataBossEntity sotetseg;
        private final Set<Long> path = new HashSet<>();
        private UUID runner;
        private int mazeTicks;
        private boolean maze66;
        private boolean maze50;
        private boolean maze33;
        private int ballCooldown = 20;

        Sotetseg(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        private BlockPos mazeOrigin() {
            return center().add(-COLS, 0, RaidBuilder.MAZE_OFFSET_Z - ROWS);
        }

        @Override
        public void decorate() {
            BlockPos o = mazeOrigin();
            int y = o.getY();
            RaidBuilder.loadChunks(world(), o.getX() - 4, o.getZ() - 6, o.getX() + COLS * 2 + 4, o.getZ() + ROWS * 2 + 6);
            RaidBuilder.fill(world(), o.getX() - 2, y - 1, o.getZ() - 5, o.getX() + COLS * 2 + 1, y - 1, o.getZ() + ROWS * 2 + 4,
                    Blocks.OBSIDIAN.getDefaultState());
            RaidBuilder.fill(world(), o.getX() - 2, y, o.getZ() - 5, o.getX() + COLS * 2 + 1, y + 3, o.getZ() + ROWS * 2 + 4,
                    Blocks.RED_STAINED_GLASS.getDefaultState());
            RaidBuilder.fill(world(), o.getX() - 1, y + 1, o.getZ() - 4, o.getX() + COLS * 2, y + 3, o.getZ() + ROWS * 2 + 3,
                    Blocks.AIR.getDefaultState());
            RaidBuilder.fill(world(), o.getX() - 1, y, o.getZ() - 4, o.getX() + COLS * 2, y, o.getZ() - 1,
                    Blocks.CRIMSON_NYLIUM.getDefaultState());
            RaidBuilder.fill(world(), o.getX() - 1, y, o.getZ() + ROWS * 2, o.getX() + COLS * 2, y, o.getZ() + ROWS * 2 + 3,
                    Blocks.RED_NETHER_BRICKS.getDefaultState());
            RaidBuilder.fill(world(), o.getX(), y, o.getZ(), o.getX() + COLS * 2 - 1, y, o.getZ() + ROWS * 2 - 1,
                    Blocks.POLISHED_BLACKSTONE.getDefaultState());
            RaidBuilder.pillar(world(), center().add(-6, 0, -8), 1, 2, Blocks.CRIMSON_HYPHAE.getDefaultState());
            RaidBuilder.pillar(world(), center().add(-6, 0, 8), 1, 2, Blocks.CRIMSON_HYPHAE.getDefaultState());
        }

        @Override
        public void start() {
            maze66 = false;
            maze50 = false;
            maze33 = false;
            runner = null;
            sotetseg = spawnBoss("sotetseg", at(13, 0));
            anchor(sotetseg);
        }

        private static long key(int col, int row) {
            return ((long) col << 32) | (row & 0xffffffffL);
        }

        private void startMaze() {
            ServerPlayerEntity p = randomPlayer();
            if (p == null) {
                return;
            }
            path.clear();
            int col = rnd().nextInt(COLS);
            for (int r = 0; r < ROWS; r++) {
                path.add(key(col, r));
                int next = MathHelper.clamp(col + rnd().nextInt(5) - 2, 0, COLS - 1);
                for (int c = Math.min(col, next); c <= Math.max(col, next); c++) {
                    path.add(key(c, r));
                }
                col = next;
            }
            runner = p.getUuid();
            mazeTicks = 0;
            BlockPos o = mazeOrigin();
            raid.teleport(p, new BlockPos(o.getX() + COLS, o.getY() + 1, o.getZ() - 2));
            warn("Sotetseg drags " + p.getName().getString() + " into the shadow realm! Find the hidden path.");
            Msg.game(p, "Follow the souls through the maze. Stepping off the path hurts.");
        }

        @Override
        public boolean bossVulnerable(DataBossEntity boss) {
            return runner == null;
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(sotetseg)) {
                return;
            }
            if (runner != null) {
                mazeTick();
                return;
            }
            double hp = raid.healthFraction(sotetseg);
            if ((hp <= 0.66 && !maze66) || (hp <= 0.33 && !maze33)) {
                if (hp <= 0.33) {
                    maze33 = true;
                    maze50 = true;
                }
                maze66 = true;
                startMaze();
                return;
            }
            // Hard mode: Sotetseg drags a player into the shadow realm once more, at half health. // approx
            if (hard() && hp <= 0.5 && !maze50) {
                maze50 = true;
                startMaze();
                return;
            }
            autoAttack(sotetseg, tick, 5);
            if (--ballCooldown <= 0) {
                ballCooldown = 25;
                ServerPlayerEntity target = randomPlayer();
                if (target != null) {
                    warn("Sotetseg launches a ball of death at " + target.getName().getString() + "! Stand together to share it!");
                    RaidEffects.later(4, () -> {
                        if (!target.isAlive() || !alive(sotetseg)) {
                            return;
                        }
                        List<ServerPlayerEntity> share = new ArrayList<>();
                        for (ServerPlayerEntity p : players()) {
                            if (p.squaredDistanceTo(target) <= 3.5 * 3.5) {
                                share.add(p);
                            }
                        }
                        if (!share.contains(target)) {
                            share.add(target);
                        }
                        world().spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 3, 1, 0.5, 1, 0);
                        int each = 100 / share.size();
                        for (ServerPlayerEntity p : share) {
                            RaidEffects.hit(sotetseg, p, each, null, true);
                        }
                    });
                }
            }
        }

        private void mazeTick() {
            ServerPlayerEntity p = world().getServer().getPlayerManager().getPlayer(runner);
            BlockPos o = mazeOrigin();
            mazeTicks++;
            if (p == null || !p.isAlive() || raid.isDowned(p)) {
                runner = null;
                return;
            }
            int col = (int) Math.floor((p.getX() - o.getX()) / 2);
            int row = (int) Math.floor((p.getZ() - o.getZ()) / 2);
            if (p.getZ() >= o.getZ() + ROWS * 2) {
                raid.teleport(p, at(-14, 0));
                say(p.getName().getString() + " escapes the shadow realm!");
                runner = null;
                return;
            }
            if (mazeTicks > 60) {
                raid.teleport(p, at(-14, 0));
                RaidEffects.hit(sotetseg, p, 30, null, true);
                Msg.red(p, "The shadow realm spits you out.");
                runner = null;
                return;
            }
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS && !path.contains(key(col, row)) && !cooldown(p, 2)) {
                RaidEffects.hit(sotetseg, p, 15, null, true);
                Msg.red(p, "The shadows lash at you!");
            }
            int from = Math.max(0, row);
            for (int r = from; r < Math.min(ROWS, from + 3); r++) {
                for (int c = 0; c < COLS; c++) {
                    if (path.contains(key(c, r))) {
                        world().spawnParticles(ParticleTypes.SOUL, o.getX() + c * 2 + 1, o.getY() + 1.1, o.getZ() + r * 2 + 1, 1, 0.3, 0,
                                0.3, 0);
                    }
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            ServerPlayerEntity p = runner == null ? null : world().getServer().getPlayerManager().getPlayer(runner);
            if (p != null && p.isAlive()) {
                raid.teleport(p, at(-14, 0));
            }
            runner = null;
        }
    }

    /** Xarpus: exhumed acid to absorb, acid spit, then the reflecting screech phase. */
    static final class Xarpus extends RoomLogic {
        private record Exhumed(Vec3d pos, long expires) {
        }

        private DataBossEntity xarpus;
        private int phase;
        private float facing;
        private final List<Exhumed> exhumeds = new ArrayList<>();

        Xarpus(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() - 2, c.getY(), c.getZ() - 2, c.getX() + 2, c.getY(), c.getZ() + 2,
                    Blocks.DARK_PRISMARINE.getDefaultState());
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    RaidBuilder.set(world(), c.add(sx * 3, 1, sz * 3), Blocks.SEA_PICKLE.getDefaultState());
                }
            }
        }

        @Override
        public void start() {
            phase = 1;
            exhumeds.clear();
            xarpus = spawnBoss("xarpus", at(0, 0));
            if (xarpus != null) {
                anchor(xarpus);
                xarpus.setAiDisabled(true);
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
                boss.setYaw(facing);
                boss.setHeadYaw(facing);
                boss.setBodyYaw(facing);
            }
        }

        @Override
        public void gameTick(long tick) {
            super.gameTick(tick);
            if (!alive(xarpus)) {
                return;
            }
            long now = ActionManager.gameTick();
            List<ServerPlayerEntity> players = players();
            if (phase == 1) {
                if (tick % 3 == 0 && !players.isEmpty()) {
                    ServerPlayerEntity p = players.get(rnd().nextInt(players.size()));
                    exhumeds.add(new Exhumed(p.getPos().add(rnd().nextInt(9) - 4, 0, rnd().nextInt(9) - 4), now + 8));
                }
                for (int i = exhumeds.size() - 1; i >= 0; i--) {
                    Exhumed e = exhumeds.get(i);
                    world().spawnParticles(ParticleTypes.SNEEZE, e.pos().x, e.pos().y + 0.1, e.pos().z, 5, 0.3, 0.02, 0.3, 0);
                    if (now >= e.expires()) {
                        exhumeds.remove(i);
                        ServerPlayerEntity absorber = null;
                        for (ServerPlayerEntity p : players) {
                            if (RaidEffects.horizontalDistSq(p.getPos(), e.pos()) < 1.2) {
                                absorber = p;
                                break;
                            }
                        }
                        if (absorber != null) {
                            RaidEffects.hit(null, absorber, 3, null, true);
                            absorber.heal(3);
                        } else {
                            raid.heal(xarpus, Math.max(1, raid.maxHitpoints(xarpus) / 25));
                            say("Xarpus absorbs the exhumed acid!");
                        }
                    }
                }
                if (raid.roomTicks() >= 30) {
                    phase = 2;
                    exhumeds.clear();
                    xarpus.setAiDisabled(false);
                    warn("Xarpus awakens!");
                }
                return;
            }
            autoAttack(xarpus, tick, 4);
            if (tick % 6 == 0 && !players.isEmpty()) {
                ServerPlayerEntity p = players.get(rnd().nextInt(players.size()));
                Vec3d at = p.getPos();
                RaidEffects.strike(world(), at, 1.2, 12, 3, xarpus, ParticleTypes.ITEM_SLIME);
                RaidEffects.later(3, () -> Mechanics.addHazard(world(), at, 8, 15, xarpus));
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
                if (tick % 4 == 0 && !players.isEmpty() && !attacks(xarpus).isEmpty()) {
                    Mechanics.perform(xarpus, players.get(rnd().nextInt(players.size())), attacks(xarpus).get(0), 1.0);
                }
            }
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (phase != 3) {
                return damage;
            }
            double dx = attacker.getX() - boss.getX();
            double dz = attacker.getZ() - boss.getZ();
            float angle = (float) Math.toDegrees(Math.atan2(-dx, dz));
            if (Math.abs(MathHelper.wrapDegrees(angle - facing)) < 50) {
                Msg.red(attacker, "Xarpus reflects your attack back at you!");
                RaidEffects.hit(boss, attacker, damage, null, true);
                return 0;
            }
            return damage;
        }
    }

    /** Verzik Vitur: throne blasts (hide behind pillars), her second form, then the spider form. */
    static final class Verzik extends RoomLogic {
        private static final int[][] PILLARS = {{0, -7}, {0, 7}, {-8, -7}, {-8, 7}};
        private DataBossEntity verzik;
        private int form;
        private final Map<UUID, Vec3d> lastPos = new HashMap<>();
        private boolean purpleSpawned;

        Verzik(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 12, c.getY(), c.getZ() - 4, c.getX() + 18, c.getY() + 1, c.getZ() + 4,
                    Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
            RaidBuilder.fill(world(), c.getX() + 12, c.getY() + 1, c.getZ() - 1, c.getX() + 13, c.getY() + 1, c.getZ() + 1,
                    Blocks.RED_CARPET.getDefaultState());
            pillars(Blocks.STONE_BRICKS.getDefaultState());
        }

        private void pillars(net.minecraft.block.BlockState state) {
            for (int[] p : PILLARS) {
                RaidBuilder.pillar(world(), center().add(p[0], 0, p[1]), 1, 5, state);
            }
        }

        @Override
        public void start() {
            form = 1;
            purpleSpawned = false;
            verzik = spawnBoss("verzik_vitur", at(15, 0).up());
            anchor(verzik);
        }

        @Override
        public int bossIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            // Phase 1: without a Dawnbringer every hit is capped at 10 (as in OSRS).
            return form == 1 ? Math.min(damage, 10) : damage;
        }

        @Override
        public void gameTick(long tick) {
            if (verzik == null) {
                return;
            }
            Vec3d pos = verzik.getPos();
            if (!alive(verzik)) {
                if (form == 1) {
                    form = 2;
                    pillars(Blocks.AIR.getDefaultState());
                    verzik = spawnBoss("verzik_vitur_p2", BlockPos.ofFloored(pos).add(-3, 0, 0));
                    warn("Verzik Vitur: Let's see how you handle this! The pillars crumble!");
                } else if (form == 2) {
                    form = 3;
                    verzik = spawnBoss("verzik_vitur_p3", BlockPos.ofFloored(pos));
                    warn("Verzik Vitur: Behold my true nature!");
                } else {
                    complete();
                }
                return;
            }
            List<ServerPlayerEntity> players = players();
            switch (form) {
                case 1 -> phaseOne(tick, players);
                case 2 -> phaseTwo(tick, players);
                default -> phaseThree(tick, players);
            }
        }

        private void phaseOne(long tick, List<ServerPlayerEntity> players) {
            if (raid.roomTicks() % 14 == 6) {
                warn("Verzik charges a blast of energy! Hide behind a pillar!");
                RaidEffects.later(2, () -> {
                    if (!alive(verzik) || form != 1) {
                        return;
                    }
                    for (ServerPlayerEntity p : players()) {
                        if (canSee(verzik, p)) {
                            Vec3d a = verzik.getEyePos();
                            Vec3d b = p.getEyePos();
                            for (int i = 0; i <= 10; i++) {
                                Vec3d v = a.lerp(b, i / 10.0);
                                world().spawnParticles(ParticleTypes.ELECTRIC_SPARK, v.x, v.y, v.z, 2, 0.1, 0.1, 0.1, 0);
                            }
                            // OSRS: an unshielded P1 blast hits up to 137 in normal mode.
                            RaidEffects.hit(verzik, p, rnd().nextInt(138), null, true);
                        } else {
                            Msg.action(p, "The pillar shields you from the blast.");
                        }
                    }
                });
            }
        }

        private void phaseTwo(long tick, List<ServerPlayerEntity> players) {
            autoAttack(verzik, tick, 4);
            if (tick % 20 == 0) {
                say("Verzik summons lightning!");
                for (ServerPlayerEntity p : players) {
                    RaidEffects.strike(world(), p.getPos(), 1.4, 30, 3, verzik, ParticleTypes.ELECTRIC_SPARK);
                }
            }
            if (tick % 30 == 15) {
                for (int i = 0; i < 2; i++) {
                    spawnMob("nylocas_matomenos", BlockPos.ofFloored(verzik.getPos()).add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2),
                            false);
                }
                say("Verzik spawns explosive Nylocas!");
            }
            for (LivingEntity crab : living("nylocas_matomenos")) {
                ServerPlayerEntity nearest = null;
                double best = Double.MAX_VALUE;
                for (ServerPlayerEntity p : players) {
                    double d = p.squaredDistanceTo(crab);
                    if (d < best) {
                        best = d;
                        nearest = p;
                    }
                }
                if (nearest != null) {
                    walk(crab, nearest.getPos());
                }
                if (best < 1.8 * 1.8 || crab.age > 20 * ActionManager.MC_TICKS_PER_GAME_TICK) {
                    world().spawnParticles(ParticleTypes.EXPLOSION, crab.getX(), crab.getY() + 0.5, crab.getZ(), 2, 0.4, 0.4, 0.4, 0);
                    for (ServerPlayerEntity p : players) {
                        if (p.squaredDistanceTo(crab) < 2.5 * 2.5) {
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

        private void phaseThree(long tick, List<ServerPlayerEntity> players) {
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
                    ServerPlayerEntity p = players.get(rnd().nextInt(players.size()));
                    BlockPos w = p.getBlockPos().add(rnd().nextInt(7) - 3, 0, rnd().nextInt(7) - 3);
                    if (world().getBlockState(w).isAir() && inside(Vec3d.ofBottomCenter(w))) {
                        RaidBuilder.set(world(), w, Blocks.COBWEB.getDefaultState());
                        webs.add(w);
                    }
                }
                RaidEffects.later(15, () -> {
                    for (BlockPos w : webs) {
                        if (world().getBlockState(w).isOf(Blocks.COBWEB)) {
                            RaidBuilder.set(world(), w, Blocks.AIR.getDefaultState());
                        }
                    }
                });
            }
            if (t % 40 == 25) {
                warn("Verzik spits yellow pools! Stand in one to lessen the damage!");
                List<Vec3d> pools = new ArrayList<>();
                for (ServerPlayerEntity p : players) {
                    pools.add(p.getPos().add(rnd().nextInt(7) - 3, 0, rnd().nextInt(7) - 3));
                }
                for (int burst = 0; burst < 3; burst++) {
                    for (int s = 0; s < 2; s++) {
                        RaidEffects.later(1 + burst * 2 + s, () -> {
                            for (Vec3d v : pools) {
                                world().spawnParticles(ParticleTypes.WAX_ON, v.x, v.y + 0.1, v.z, 8, 0.4, 0.05, 0.4, 0);
                            }
                        });
                    }
                    RaidEffects.later(4 + burst * 2, () -> {
                        if (!alive(verzik)) {
                            return;
                        }
                        for (ServerPlayerEntity p : players()) {
                            boolean safe = false;
                            for (Vec3d v : pools) {
                                if (RaidEffects.horizontalDistSq(p.getPos(), v) < 1.4) {
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
                ServerPlayerEntity target = players.get(rnd().nextInt(players.size()));
                warn("Verzik hurls a green ball at " + target.getName().getString() + "!");
                RaidEffects.later(4, () -> {
                    if (!target.isAlive() || !alive(verzik)) {
                        return;
                    }
                    ServerPlayerEntity buddy = null;
                    for (ServerPlayerEntity p : players()) {
                        if (p != target && p.squaredDistanceTo(target) < 36) {
                            buddy = p;
                            break;
                        }
                    }
                    world().spawnParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0);
                    if (buddy != null) {
                        RaidEffects.hit(verzik, target, 25, AttackType.RANGED, false);
                        RaidEffects.hit(verzik, buddy, 25, AttackType.RANGED, false);
                    } else {
                        RaidEffects.hit(verzik, target, 50, null, true);
                    }
                });
            }
            if (raid.healthFraction(verzik) < 0.2 && tick % 2 == 0) {
                for (ServerPlayerEntity p : players) {
                    Vec3d last = lastPos.put(p.getUuid(), p.getPos());
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
