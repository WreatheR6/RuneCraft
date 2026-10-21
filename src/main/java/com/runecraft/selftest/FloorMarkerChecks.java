package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.FloorMarkers;
import com.runecraft.boss.HazardKind;
import com.runecraft.boss.Mechanics;
import com.runecraft.content.bosses.BossRealtimeHooks;
import com.runecraft.content.bosses.ScriptedBoss;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.ArenaTiming;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.BossRealtime;
import com.runecraft.osrs.data.Monster;
import com.runecraft.registry.ModEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The round 43 floor markers ({@code boss:playability_floor_*}; they work with the real player too and read the world's blocks,
 * never health): a scripted strike's disc of coloured floor blocks from the moment it is made until it lands, put back as it
 * lands and when its boss dies; a layer's blocks changing kind and going back; a chest and molten sand (magma) never covered; a
 * server stop putting everything back; a monster's sky strike marking its tiles; Vardorvis's axes marking their whole path at
 * once and sweeping it a step at a time; the Hunllef's lit tiles light blue, then orange, exactly where they burn.
 */
final class FloorMarkerChecks {
    private static final int MC = ActionManager.MC_TICKS_PER_GAME_TICK;

    /** A scripted boss with nothing of its own: the checks make its strikes. */
    static final class Probe extends ScriptedBoss {
        void fire(DataBossEntity host, Vec3 at, int delay, double radius, ParticleOptions particle) {
            strike(host, at, delay, 0, radius, particle, (hit, pos) -> {
            });
        }

        boolean pending() {
            return strikesPending();
        }
    }

    private final TestContext c;

    private FloorMarkerChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        FloorMarkerChecks f = new FloorMarkerChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("boss:playability_floor_strike", f::strike));
        checks.add(new SelfTest.Check("boss:playability_floor_layer", f::layer));
        checks.add(new SelfTest.Check("boss:playability_floor_sky_strike", f::skyStrike));
        checks.add(new SelfTest.Check("boss:playability_floor_axes", f::axes));
        checks.add(new SelfTest.Check("boss:playability_floor_hunllef", f::hunllef));
        return checks;
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private ServerPlayer p() {
        return c.player;
    }

    /** The platform's floor block under spot ({@code dx}, {@code dz}). */
    private BlockPos floor(int dx, int dz) {
        return c.near(dx, dz).below();
    }

    private BlockState at(BlockPos pos) {
        return c.world.getBlockState(pos);
    }

    /** A data boss entity, alive, to own the probe's strikes; never added to the world, so it never ticks or acts. */
    private DataBossEntity host(Vec3 at) {
        List<String> ids = new ArrayList<>(ModEntities.DATA_BOSS_TYPES.keySet());
        java.util.Collections.sort(ids);
        for (String id : ids) {
            EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(id);
            if (ContentRegistry.boss(id) == null || type == null) {
                continue;
            }
            DataBossEntity e = type.create(c.world);
            if (e != null) {
                e.moveTo(at.x, at.y, at.z, 0, 0);
                return e;
            }
        }
        throw new AssertionError("no data boss type to host the probe");
    }

    /** One Minecraft tick of the probe: its tick, and its game tick on each boundary (as DataBossEntity runs a script). */
    private static void mc(BossScript probe, DataBossEntity host) {
        ActionManager.advanceForSelfTest();
        probe.tick(host);
        if (ActionManager.mcTick() % MC == 0) {
            probe.gameTick(host, ActionManager.gameTick());
        }
    }

    // ------------------------------------------------------------------ a scripted strike

    /**
     * A radius-1.5 fire strike centred on a block marks the 3x3 blocks under it orange the tick it is made, keeps them while it
     * waits and puts them back as it lands; an ink strike is purple; a strike whose boss dies goes back with it; a chest inside
     * a disc is never swapped.
     */
    private String strike() {
        Vec3 centre = Vec3.atBottomCenterOf(c.near(5, 5));
        DataBossEntity host = host(centre);
        Probe probe = new Probe();
        Map<BlockPos, BlockState> before = new HashMap<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                before.put(floor(5 + dx, 5 + dz), at(floor(5 + dx, 5 + dz)));
            }
        }
        int live = FloorMarkers.live();
        do { // to a game-tick boundary, the probe's game tick run there (as a boss's strikes are made in its game tick)
            ActionManager.advanceForSelfTest();
            probe.tick(host);
        } while (ActionManager.mcTick() % MC != 0);
        probe.gameTick(host, ActionManager.gameTick());
        probe.fire(host, centre, 0, 1.5, ParticleTypes.FLAME);
        int marked = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                expect(at(floor(5 + dx, 5 + dz)).is(Blocks.ORANGE_CONCRETE), "the fire strike's tile " + dx + "," + dz
                        + " isn't orange the tick it was made (" + at(floor(5 + dx, 5 + dz)) + ")");
                marked++;
            }
        }
        expect(at(floor(7, 5)) == before.get(floor(7, 5)), "a block outside the 1.5 disc was marked");
        expect(FloorMarkers.live() == live + 9, (FloorMarkers.live() - live) + " blocks marked for a 3x3 strike");
        int ran = 0;
        while (probe.pending() && ran < 200) {
            mc(probe, host);
            ran++;
            if (probe.pending()) {
                expect(at(floor(5, 5)).is(Blocks.ORANGE_CONCRETE), "the strike's tiles went back " + ran + " MC in, before it landed");
            }
        }
        expect(!probe.pending(), "the probe's strike never landed");
        expect(ran >= ArenaTiming.MIN_LEAD_MC, "the strike landed " + ran + " MC after it was made");
        for (Map.Entry<BlockPos, BlockState> e : before.entrySet()) {
            expect(at(e.getKey()) == e.getValue(), "the floor at " + e.getKey() + " wasn't put back as the strike landed");
        }
        expect(FloorMarkers.live() == live, "marks left after the strike landed: " + (FloorMarkers.live() - live));
        // an ink strike is purple; its boss dying puts its tiles back before it lands
        probe.fire(host, centre, 3, 0.5, ParticleTypes.SQUID_INK);
        expect(at(floor(5, 5)).is(Blocks.PURPLE_CONCRETE), "an ink strike's tile isn't purple (" + at(floor(5, 5)) + ")");
        host.setHealth(0);
        FloorMarkers.sweepOwners();
        expect(at(floor(5, 5)) == before.get(floor(5, 5)), "the strike's tile wasn't put back when its boss died");
        // a dead boss marks nothing
        probe.fire(host, centre, 3, 1.5, ParticleTypes.FLAME);
        expect(at(floor(5, 5)) == before.get(floor(5, 5)), "a dead boss's strike marked the floor");
        // a chest inside a living boss's disc stays a chest
        DataBossEntity alive = host(centre);
        BlockPos chest = c.place(floor(6, 5), Blocks.CHEST.defaultBlockState());
        Probe second = new Probe();
        second.fire(alive, centre, 0, 1.5, ParticleTypes.FLAME);
        expect(at(chest).is(Blocks.CHEST), "a chest was swapped for a floor mark");
        expect(at(floor(5, 5)).is(Blocks.ORANGE_CONCRETE), "the rest of the disc wasn't marked round the chest");
        for (int i = 0; i < 200 && second.pending(); i++) {
            mc(second, alive);
        }
        expect(at(floor(5, 5)) == before.get(floor(5, 5)), "the floor wasn't put back after the second strike");
        expect(FloorMarkers.live() == live, "marks left after the strikes: " + (FloorMarkers.live() - live));
        return marked + " blocks orange from creation to landing (" + ran + " MC), ink purple, put back on its boss's death, chests left alone";
    }

    // ------------------------------------------------------------------ layers

    /**
     * A layer shows exactly the blocks it is given: one changing kind is re-marked, one dropped goes back, magma is never
     * covered, a server stop puts everything back and the layer marks again after it.
     */
    private String layer() {
        BlockPos a = floor(-5, 4);
        BlockPos b = floor(-4, 4);
        BlockPos m = floor(-3, 4);
        BlockState was = at(a);
        BlockState wasB = at(b);
        c.place(m, Blocks.MAGMA_BLOCK.defaultBlockState());
        int live = FloorMarkers.live();
        FloorMarkers.Layer layer = new FloorMarkers.Layer(null);
        try {
            Map<BlockPos, HazardKind> want = new HashMap<>();
            want.put(a, HazardKind.STRIKE);
            want.put(b, HazardKind.STRIKE);
            want.put(m, HazardKind.STRIKE);
            layer.show(c.world, want);
            expect(at(a).is(Blocks.RED_CONCRETE) && at(b).is(Blocks.RED_CONCRETE), "the layer's blocks aren't red");
            expect(at(m).is(Blocks.MAGMA_BLOCK), "molten sand (magma) was covered");
            expect(layer.size() == 2, "the layer marks " + layer.size() + " blocks (2 expected: magma is skipped)");
            want.put(a, HazardKind.SAFE);
            want.remove(b);
            layer.show(c.world, want);
            expect(at(a).is(Blocks.CYAN_CONCRETE), "a block changed to safe isn't cyan (" + at(a) + ")");
            expect(at(b) == wasB, "a block dropped from the layer wasn't put back");
            FloorMarkers.stopForSelfTest();
            expect(at(a) == was && FloorMarkers.live() == 0, "a server stop left the layer's block");
            expect(layer.size() == 0, "the layer still counts its marks after the server stopped");
            layer.show(c.world, want);
            expect(at(a).is(Blocks.CYAN_CONCRETE), "the layer couldn't mark again after the stop");
            // shapes: a 3-wide line and a ring
            FloorMarkers.Shape line = new FloorMarkers.Shape(c.world).line(Vec3.atBottomCenterOf(c.near(-6, -6)),
                    Vec3.atBottomCenterOf(c.near(-2, -6)), 1.0, HazardKind.SHADOW);
            expect(line.blocks().size() >= 15, "a 5-long, 3-wide line covers " + line.blocks().size() + " floor blocks");
            layer.show(line);
            expect(at(floor(-4, -6)).is(Blocks.PURPLE_CONCRETE) && at(floor(-4, -5)).is(Blocks.PURPLE_CONCRETE),
                    "the line's blocks aren't purple");
            expect(at(a) == was, "the previous shape's block wasn't put back when the layer showed a new one");
        } finally {
            layer.clear();
        }
        expect(FloorMarkers.live() == live, "marks left after the layer was cleared: " + (FloorMarkers.live() - live));
        return "red, then cyan, dropped and put back, magma skipped, a stop puts it back and it marks again, lines purple";
    }

    // ------------------------------------------------------------------ a monster's sky strike

    /** A monster's sky strike marks the tiles of its 1.5-block ring from its cue until it lands, then puts them back. */
    private String skyStrike() {
        EntityType<?> type = ModEntities.typeById(Monster.HILL_GIANT.entity());
        Entity e = type == null ? null : type.create(c.world);
        if (!(e instanceof OsrsMonsterEntity mob)) {
            throw new SelfTest.Skip("no hill giant entity");
        }
        BlockPos base = TestContext.PLATFORM;
        mob.moveTo(base.getX() + 0.5 + 3, base.getY(), base.getZ() + 0.5, 0f, 0f);
        mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(base), MobSpawnType.SPAWNER, null);
        c.world.addFreshEntity(mob);
        try {
            AttackDef sky = AttackDef.of("selftest_sky", AttackType.MAGIC, 5, 10, 1).special(AttackDef.Special.SKY_STRIKE, 0)
                    .particle("minecraft:flame");
            BlockPos under = p().blockPosition().below();
            BlockState was = at(under);
            int live = FloorMarkers.live();
            long t0 = ActionManager.mcTick();
            Mechanics.perform(mob, p(), sky, 1.0);
            expect(at(under).is(Blocks.ORANGE_CONCRETE), "the sky strike's tile under the player isn't orange on its cue (" + at(under) + ")");
            int marked = FloorMarkers.live() - live;
            expect(marked >= 4 && marked <= 16, marked + " blocks marked for a 1.5-block sky strike");
            int lead = ThreatTiming.leadMc(0, AttackDef.Special.SKY_STRIKE);
            for (int i = 0; i < lead + 4; i++) {
                ActionManager.advanceForSelfTest();
                Mechanics.tickForSelfTest();
                p().setHealth(p().getMaxHealth());
                if (ActionManager.mcTick() - t0 < lead) {
                    expect(at(under).is(Blocks.ORANGE_CONCRETE), "the sky strike's tiles went back before it landed");
                }
            }
            expect(at(under) == was, "the sky strike's tile wasn't put back after it landed");
            expect(FloorMarkers.live() == live, "marks left after the sky strike: " + (FloorMarkers.live() - live));
            return marked + " tiles orange for " + lead + " MC, put back as it landed";
        } finally {
            mob.discard();
            c.heal();
        }
    }

    // ------------------------------------------------------------------ Vardorvis's axes

    private DataBossEntity spawn(String id, int dx, int dz) {
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            throw new SelfTest.Skip("no BossDef " + id);
        }
        DataBossEntity boss = DataBossService.spawn(c.world, c.near(dx, dz), def, p());
        expect(boss != null, "DataBossService.spawn returned null for " + id);
        boss.setTarget(p());
        return boss;
    }

    /** Minecraft ticks of a fight: the boss's own tick (its game ticks every 12) and the delivery; the player kept healthy. */
    private void mc(DataBossEntity boss, int n) {
        for (int i = 0; i < n; i++) {
            ActionManager.advanceForSelfTest();
            if (boss.isAlive()) {
                if (boss.getTarget() != p()) {
                    boss.setTarget(p());
                }
                boss.tickCount++;
                boss.tick();
            }
            Mechanics.tickForSelfTest();
            p().setHealth(p().getMaxHealth());
        }
    }

    /**
     * Vardorvis's axes: every step of each axe's path is marked red the moment they swing; the steps land in order, a step (two)
     * a game tick from the strike floor on (not all on one tick); every block is back once the last has passed.
     */
    private String axes() {
        DataBossEntity boss = spawn("vardorvis", 0, -6);
        List<ArenaTiming.StrikeTrace> traces = new ArrayList<>();
        Consumer<ArenaTiming.StrikeTrace> before = ScriptedBoss.strikeListener;
        ScriptedBoss.strikeListener = before.andThen(traces::add);
        try {
            mc(boss, MC);
            BossRealtimeHooks.hold(boss, 40);
            int red = FloorMarkers.live(HazardKind.STRIKE);
            long t0 = ActionManager.mcTick();
            BossRealtimeHooks.vardorvisAxes(boss, p());
            int marked = FloorMarkers.live(HazardKind.STRIKE) - red;
            expect(marked >= BossRealtime.AXE_PATH_STEPS, "only " + marked + " red floor blocks when an axe's " + BossRealtime.AXE_PATH_STEPS
                    + "-step path appeared");
            int landedBefore = traces.size();
            for (int i = 0; i < 12 * MC && FloorMarkers.live(HazardKind.STRIKE) > red; i++) {
                mc(boss, 1);
            }
            List<ArenaTiming.StrikeTrace> axe = traces.subList(landedBefore, traces.size());
            TreeSet<Long> landings = new TreeSet<>();
            for (ArenaTiming.StrikeTrace t : axe) {
                landings.add(t.leadMc());
            }
            expect(axe.size() >= BossRealtime.AXE_PATH_STEPS, axe.size() + " axe steps landed");
            expect(landings.first() >= ArenaTiming.MIN_LEAD_MC, "the first axe step landed " + landings.first() + " MC after it swung");
            expect(landings.size() >= 4, "the axe steps landed on " + landings.size() + " game ticks (a sweep, not one blow)");
            // the first steps may share the floor's tick (a strike made between game ticks keeps its 36 MC), the rest follow
            expect(landings.last() - landings.first() >= (long) (BossRealtime.axeStepDelay(BossRealtime.AXE_PATH_STEPS - 1)
                    - BossRealtime.axeStepDelay(0) - 1) * MC, "the sweep took " + (landings.last() - landings.first()) + " MC");
            expect(FloorMarkers.live(HazardKind.STRIKE) <= red, "the axes' red blocks weren't all put back");
            return marked + " path blocks red at once; steps landed at " + landings + " MC (" + (ActionManager.mcTick() - t0)
                    + " MC to clear)";
        } finally {
            ScriptedBoss.strikeListener = before;
            boss.discard();
            FloorMarkers.sweepOwners();
            c.heal();
        }
    }

    // ------------------------------------------------------------------ the Hunllef's floor

    /**
     * The Hunllef's lit floor: each 2x2-block tile light blue while it warns, orange while it burns (exactly the tiles it
     * tests), and every block back when the tiles go out.
     */
    private String hunllef() {
        DataBossEntity boss = spawn("crystalline_hunllef", 0, -7);
        try {
            mc(boss, MC);
            BossRealtimeHooks.hold(boss, 60);
            BossRealtimeHooks.hunllefFloorNow(boss);
            mc(boss, MC + 1); // its next game tick lights the floor; its tick after that marks it
            List<BlockPos> tiles = BossRealtimeHooks.hunllefFloorTiles(boss);
            expect(!tiles.isEmpty(), "the Hunllef's floor didn't light");
            expect(!BossRealtimeHooks.hunllefFloorBurning(boss), "the tiles burnt without warning");
            int warned = countTiles(tiles, Blocks.LIGHT_BLUE_CONCRETE);
            expect(warned > 0 && warned == BossRealtimeHooks.hunllefFloorMarked(boss), warned + " light blue floor blocks under "
                    + tiles.size() + " warning tiles (" + BossRealtimeHooks.hunllefFloorMarked(boss) + " marked)");
            for (int i = 0; i < 20 * MC && !BossRealtimeHooks.hunllefFloorBurning(boss); i++) {
                mc(boss, 1);
            }
            expect(BossRealtimeHooks.hunllefFloorBurning(boss), "the tiles never burnt");
            mc(boss, 1); // the floor is redrawn on the boss's tick, which runs before its game tick
            List<BlockPos> burning = BossRealtimeHooks.hunllefFloorTiles(boss);
            int orange = countTiles(burning, Blocks.ORANGE_CONCRETE);
            expect(orange == warned, orange + " orange blocks once burning (" + warned + " warned)");
            for (int i = 0; i < 20 * MC && !BossRealtimeHooks.hunllefFloorTiles(boss).isEmpty(); i++) {
                mc(boss, 1);
            }
            expect(BossRealtimeHooks.hunllefFloorTiles(boss).isEmpty(), "the tiles never went out");
            mc(boss, 1);
            expect(countTiles(burning, Blocks.ORANGE_CONCRETE) == 0 && countTiles(burning, Blocks.LIGHT_BLUE_CONCRETE) == 0,
                    "tile blocks left coloured after the floor went out");
            return tiles.size() + " tiles: " + warned + " blocks light blue, then orange, then put back";
        } finally {
            boss.discard();
            FloorMarkers.sweepOwners();
            c.heal();
        }
    }

    /** Floor blocks of {@code tiles} (2x2 blocks each, at the feet level) that are now {@code block}. */
    private int countTiles(List<BlockPos> tiles, net.minecraft.world.level.block.Block block) {
        int n = 0;
        int size = com.runecraft.osrs.content.minigames.Gauntlet.FLOOR_TILE_BLOCKS;
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (BlockPos t : tiles) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    BlockPos f = FloorMarkers.floorAt(c.world, t.getX() + x, t.getZ() + z, t.getY());
                    if (f != null && seen.add(f) && at(f).is(block)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }
}
