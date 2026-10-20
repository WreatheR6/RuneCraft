package com.runecraft.content.raids;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidLayouts;
import com.runecraft.osrs.content.raids.RaidRewards;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.ToaSupplies;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Self-test driver for whole raids ({@code com.runecraft.selftest}, "raid:" checks in real-player mode): walks a raid the
 * way a player does, through the real blocks, room logic and hit path, and fast-forwards the raid clock
 * ({@link RaidEffects#advance}) so a full run fits in one check. Every step that doesn't work throws an
 * {@link AssertionError} saying which.
 */
public final class RaidSelfTest {
    private final RaidInstance raid;
    private final ServerPlayer player;
    private final StringBuilder log = new StringBuilder();
    private int ticks;
    /** Delayed raid tasks that had already failed before this run (they are global). */
    private final int taskFailuresBefore = RaidEffects.failures();

    private RaidSelfTest(RaidInstance raid, ServerPlayer player) {
        this.raid = raid;
        this.player = player;
    }

    /**
     * Fails when a room tick or a delayed raid task threw since the run began: the raid logs those and carries on (a
     * room that throws is passed as cleared), which would otherwise hide a broken room behind a passing walk.
     */
    void healthy(String where) {
        expect(raid.roomFailures() == 0, where + ": a room tick threw (" + raid.lastRoomFailure() + ")");
        expect(RaidEffects.failures() == taskFailuresBefore, where + ": a delayed raid task threw (" + RaidEffects.lastFailure() + ")");
    }

    // ------------------------------------------------------------------ driver API (used by the rooms' solvers)

    RaidInstance raid() {
        return raid;
    }

    ServerPlayer player() {
        return player;
    }

    /** Advances the raid by {@code n} game ticks: delayed effects, strikes and the current room's logic. */
    void tick(int n) {
        for (int i = 0; i < n && !raid.closed(); i++) {
            RaidEffects.advance(1);
            RaidEffects.gameTick();
            if (!raid.closed()) {
                raid.gameTick(RaidEffects.now());
            }
            ticks++;
            heal();
        }
    }

    /** Keeps the player topped up (the real-player self-test player is invulnerable, but drains still apply). */
    void heal() {
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        var data = Players.data(player);
        if (data.prayerPoints() < 50) {
            data.setPrayerPoints(99);
        }
    }

    /** A player hit through the real path (vulnerability, immunities, room hooks, points), without the accuracy roll. */
    int hit(LivingEntity target, int damage) {
        return hit(target, damage, AttackType.STAB);
    }

    int hit(LivingEntity target, int damage, AttackType type) {
        int d = damage;
        if (target instanceof OsrsMonsterEntity m) {
            d = m.modifyIncoming(player, d, type);
        }
        int dealt = CombatHandler.applyDamage(player, target, d);
        CombatHandler.hookAfterHit(player, target, type, dealt);
        return dealt;
    }

    /** Right-clicks a raid block (the block at {@code pos} must be a raid block). */
    void use(BlockPos pos) {
        BlockState state = raid.world.getBlockState(pos);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        RaidManager.useBlock(player, id, pos);
    }

    /** Stands the player on the block at {@code floor} (its top) and lets a tick pass. */
    void stepOn(BlockPos floor) {
        player.teleportTo(raid.world, floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5, player.getYRot(), 0);
        tick(1);
    }

    /** Moves the player to a spot (standing position) without ticking. */
    void moveTo(BlockPos stand) {
        player.teleportTo(raid.world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(), 0);
    }

    void note(String s) {
        log.append(log.length() == 0 ? "" : "; ").append(s);
    }

    static AssertionError fail(String message) {
        return new AssertionError(message);
    }

    static void expect(boolean ok, String message) {
        if (!ok) {
            throw fail(message);
        }
    }

    private boolean isAir(BlockPos pos) {
        return raid.world.getBlockState(pos).isAir();
    }

    private String blockAt(BlockPos pos) {
        Block b = raid.world.getBlockState(pos).getBlock();
        return BuiltInRegistries.BLOCK.getKey(b).getPath();
    }

    // ------------------------------------------------------------------ the Tombs of Amascut

    /**
     * Walks a whole Tombs of Amascut raid as the real player: invocations at the obelisk, the barrier, the Nexus, every
     * path's puzzle solved and boss killed (in a random order), the Helpful Spirit, the Wardens' three phases, and the
     * chest. Returns a summary; throws on the first step that doesn't work.
     */
    public static String toaRun(ServerPlayer p, BlockPos entrance) {
        RaidManager.enter(p, RaidType.TOA, entrance);
        RaidInstance raid = RaidManager.of(p);
        expect(raid != null, "RaidManager.enter did not start a raid");
        RaidSelfTest t = new RaidSelfTest(raid, p);
        try {
            return t.walkToa();
        } finally {
            if (RaidManager.of(p) == raid) {
                RaidManager.selfTestClose(raid);
            }
        }
    }

    private String walkToa() {
        expect(raid.players().contains(player), "the player is not in the raid's online party (fake player?)");
        // --- the lobby: invocations at the obelisk, then the barrier.
        expect(blockAt(raid.center(0).offset(6, 1, 0)).equals(RaidsContent.INVOCATION_OBELISK), "no invocation obelisk in the lobby");
        expect(ToaInvocationMenu.preset(player, 1) == 1 && raid.raidLevel() == 150, "the Normal preset did not set raid level 150");
        int level = raid.raidLevel();
        expect(ToaInvocationMenu.toggle(player, "walk the path") == 1 && raid.raidLevel() == level + 50,
                "turning on Walk the Path did not add 50 raid levels (" + raid.raidLevel() + ")");
        expect(ToaInvocationMenu.toggle(player, "WALK_THE_PATH") == 1 && raid.raidLevel() == level,
                "turning Walk the Path off did not restore the raid level");
        raid.toggleInvocation(Invocation.NOT_JUST_A_HEAD);
        expect(raid.toggleInvocation(Invocation.BLOOD_THINNERS) != null && !raid.invocations().contains(Invocation.BLOOD_THINNERS),
                "Blood Thinners turned on without Not Just a Head");
        raid.toggleInvocation(Invocation.NOT_JUST_A_HEAD);
        expect(raid.toggleInvocation(Invocation.PATHFINDER) == null && !raid.invocations().contains(Invocation.PATHSEEKER),
                "Pathfinder did not replace Pathseeker (same group)");
        raid.toggleInvocation(Invocation.PATHSEEKER);
        // Entry-level attempts (no attempts invocation) so a wipe would only reset the room.
        raid.toggleInvocation(Invocation.HARDCORE_RUN);
        note("raid level " + raid.raidLevel());
        use(raid.center(0).offset(RaidBuilder.HALF + 1, 1, 0));
        expect(raid.begun(), "the start barrier did not begin the raid");
        expect(isAir(raid.center(0).offset(RaidBuilder.HALF + 1, 2, 0)), "the lobby's exit did not open");
        expect(raid.toggleInvocation(Invocation.INSANITY) != null, "the invocations were not locked in after the barrier");
        // --- the Nexus.
        int nexusIndex = raid.roomIndex("toa_nexus");
        moveTo(raid.spawn(nexusIndex));
        tick(2);
        expect(raid.currentRoom() == nexusIndex, "walking into the Nexus did not make it the current room");
        ToaNexus nexus = raid.nexus();
        expect(nexus != null, "no Nexus logic");
        expect(raid.elapsedTicks() == 0, "the raid timer started before the first path");
        moveTo(raid.spawn(nexusIndex));
        use(nexus.entrance(4));
        expect(raid.currentRoom() == nexusIndex, "the Wardens' way opened before the paths were walked");
        // --- the four paths, in a random order.
        List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(order, raid.random);
        int attacks = raid.attacksOnParty();
        for (int path : order) {
            walkPath(nexus, nexusIndex, path);
            if (raid.pathsCompleted() == 2 || raid.pathsCompleted() == 4) {
                expect(ToaSupplyService.hasOffer(player), "the Helpful Spirit offered nothing after " + raid.pathsCompleted() + " paths");
                use(nexus.at(-12, 0));
                String bundle = raid.pathsCompleted() == 2 ? "power" : "life";
                expect(ToaSupplyService.claim(player, bundle) == 1, "claiming the " + bundle + " bundle failed");
                expect(Inv.has(player, ToaSupplies.SMELLING_SALTS, 1) || Inv.has(player, ToaSupplies.NECTAR, 1),
                        "the " + bundle + " bundle gave no supplies");
            }
        }
        expect(raid.elapsedTicks() > 0, "the raid timer did not run during the paths");
        expect(raid.attacksOnParty() > attacks, "nothing attacked the player during the four paths");
        // --- the Wardens.
        int wardens = raid.roomIndex("wardens");
        moveTo(raid.spawn(nexusIndex));
        use(nexus.entrance(4));
        expect(raid.currentRoom() == wardens && raid.logic(wardens).started, "the Wardens' way did not open after four paths");
        expect(player.blockPosition().closerThan(raid.spawn(wardens), 3), "the Wardens' way did not take the player down");
        note(raid.logic(wardens).selfTestSolve(this));
        healthy("wardens");
        expect(raid.logic(wardens).done, "the Wardens are not defeated");
        expect(isAir(raid.center(wardens).offset(RaidBuilder.HALF + 1, 2, 0)), "the Wardens' exit did not open");
        // --- the burial chamber.
        int reward = raid.roomIndex("toa_reward");
        int kcBefore = RaidsState.get(raid.world.getServer()).completions(player.getUUID(), RaidType.TOA.id());
        moveTo(raid.spawn(reward));
        tick(2);
        expect(raid.finished(), "entering the burial chamber did not complete the raid");
        int kc = RaidsState.get(raid.world.getServer()).completions(player.getUUID(), RaidType.TOA.id());
        expect(kc == kcBefore + 1, "the completion count went " + kcBefore + " -> " + kc);
        RaidRewards.Result result = raid.result();
        expect(result != null && !result.lootFor(player.getUUID().toString()).isEmpty(), "the chest rolled no loot for the player");
        String first = result.lootFor(player.getUUID().toString()).get(0).item();
        int had = Inv.count(player, first);
        use(raid.center(reward).offset(6, 1, 0));
        expect(raid.hasClaimed(player), "opening the chest did not claim the loot");
        expect(Inv.count(player, first) > had || first.equals("coins"), "the chest did not hand over " + first);
        note("chest: " + result.lootFor(player.getUUID().toString()).size() + " items, " + raid.points(player.getUUID()) + " points");
        healthy("the burial chamber");
        return log + "; " + ticks + " raid ticks";
    }

    private void walkPath(ToaNexus nexus, int nexusIndex, int path) {
        List<String> ids = RaidLayouts.TOA_PATHS.get(path);
        int puzzle = raid.roomIndex(ids.get(0));
        int boss = puzzle + 1;
        moveTo(raid.spawn(nexusIndex));
        use(nexus.entrance(path));
        expect(raid.currentRoom() == puzzle && raid.logic(puzzle).started, ids.get(0) + ": the path entrance did not start the puzzle room");
        expect(player.blockPosition().closerThan(raid.spawn(puzzle), 3), ids.get(0) + ": the entrance did not take the player in");
        expect(raid.pathInProgress() == path, ids.get(0) + ": the path is not marked as in progress");
        // Another path is blocked while this one is walked.
        int other = (path + 1) % 4;
        if (!raid.pathDone(other)) {
            moveTo(raid.spawn(nexusIndex));
            use(nexus.entrance(other));
            expect(raid.currentRoom() == puzzle, ids.get(0) + ": a second path could be started while one was in progress");
            moveTo(raid.spawn(puzzle));
        }
        tick(1);
        note(raid.logic(puzzle).selfTestSolve(this));
        healthy(ids.get(0));
        expect(raid.logic(puzzle).done, ids.get(0) + ": the puzzle is not solved");
        expect(isAir(raid.center(puzzle).offset(RaidBuilder.HALF + 1, 2, 0)), ids.get(0) + ": the puzzle's exit did not open");
        // The boss.
        moveTo(raid.spawn(boss));
        tick(2);
        expect(raid.currentRoom() == boss && raid.logic(boss).started, ids.get(1) + ": walking in did not start the boss room");
        DataBossEntity b = raid.logic(boss).hudBoss();
        expect(b != null && b.isAlive(), ids.get(1) + ": no boss spawned");
        expect(raid.maxHitpoints(b) > 0, ids.get(1) + ": the boss has no hitpoints");
        note(raid.logic(boss).selfTestSolve(this));
        healthy(ids.get(1));
        expect(raid.logic(boss).done, ids.get(1) + ": the boss is not dead");
        expect(raid.pathDone(path), ids.get(1) + ": the path is not marked as done");
        BlockPos crystal = raid.center(boss).offset(RaidBuilder.HALF - 3, 1, 0);
        expect(blockAt(crystal).equals(RaidsContent.TOA_EXIT_CRYSTAL), ids.get(1) + ": no teleport crystal after the boss");
        use(crystal);
        expect(raid.inRoom(player.position(), nexusIndex), ids.get(1) + ": the teleport crystal did not lead back to the Nexus");
        expect(raid.currentRoom() == nexusIndex, ids.get(1) + ": the Nexus is not the current room after the crystal");
        moveTo(raid.spawn(nexusIndex));
        use(nexus.entrance(path));
        expect(raid.currentRoom() == nexusIndex, ids.get(0) + ": a finished path could be entered again");
    }

    /**
     * Deaths and wipes in the Tombs: a downed solo player wipes the party, which (without an attempts invocation) resets
     * the room and hands out honey locusts; with Hardcore Run the wipe ends the raid.
     */
    public static String toaDeaths(ServerPlayer p, BlockPos entrance) {
        StringBuilder out = new StringBuilder();
        for (boolean hardcore : new boolean[]{false, true}) {
            RaidManager.enter(p, RaidType.TOA, entrance);
            RaidInstance raid = RaidManager.of(p);
            expect(raid != null, "RaidManager.enter did not start a raid");
            RaidSelfTest t = new RaidSelfTest(raid, p);
            try {
                ToaInvocationMenu.preset(p, 0);
                if (hardcore) {
                    raid.toggleInvocation(Invocation.HARDCORE_RUN);
                }
                t.use(raid.center(0).offset(RaidBuilder.HALF + 1, 1, 0));
                int nexus = raid.roomIndex("toa_nexus");
                t.moveTo(raid.spawn(nexus));
                t.tick(2);
                t.use(raid.nexus().entrance(1));
                int room = raid.currentRoom();
                expect(raid.logic(room).started, "the Path of Het did not start");
                int before = raid.toaTotal(p.getUUID());
                raid.onDeath(p);
                expect(raid.toaTotal(p.getUUID()) == before - Math.max(1000, before / 5), "the death penalty was "
                        + (before - raid.toaTotal(p.getUUID())) + " of " + before);
                if (hardcore) {
                    expect(RaidManager.of(p) == null, "a Hardcore Run wipe did not end the raid");
                    out.append("hardcore wipe ends the raid");
                } else {
                    expect(RaidManager.of(p) == raid && !raid.isDowned(p), "the wipe did not bring the solo player back");
                    expect(raid.logic(room).started && !raid.logic(room).done, "the room was not reset after the wipe");
                    expect(Inv.count(p, ToaSupplies.HONEY_LOCUST) == ToaSupplies.honeyLocustsOnWipe(raid.invocations()),
                            "the wipe handed out " + Inv.count(p, ToaSupplies.HONEY_LOCUST) + " honey locusts");
                    out.append("wipe resets the room with honey locusts; ");
                }
            } finally {
                if (RaidManager.of(p) == raid) {
                    RaidManager.selfTestClose(raid);
                }
            }
        }
        return out.toString();
    }
}
