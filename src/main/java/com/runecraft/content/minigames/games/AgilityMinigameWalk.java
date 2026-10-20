package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.HallowedSepulchre;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Kind;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Real-player self-test walks of the agility minigames (round 39; {@code com.runecraft.selftest} "minigame:sepulchre_run",
 * "minigame:sepulchre_timeout" and "minigame:pyramid_plunder_run", real-player mode, since minigame sessions only see players in
 * the server's player list). Each plays the game through the blocks a player clicks ({@link BlockHooks}) and the moves a
 * player makes, ticking the session by hand, and throws an {@link AssertionError} naming any broken step:
 * <ul>
 *   <li>Hallowed Sepulchre: the entry requirements, all five floors in a row by whatever entrance each picks: every trap
 *       corridor crossed tile by tile at OSRS run speed with the pure rules' planner (a hit fails the walk), one deliberate hit
 *       without and one with the hallowed ring, every coffin's challenge (bridge, brazier, portal with the hallowed focus,
 *       pillar) unlocked and crossed both ways, every coffin looted, the middle stairs and platforms, the Agility gate on the
 *       stairs down, a hallowed token, the Grand Hallowed Coffin and the obelisk out with the floor XP; a hallowed sack.</li>
 *   <li>Out of time: the clock runs out, coffins and challenges refuse, the stairs are sealed and the obelisk still pays.</li>
 * </ul>
 */
public final class AgilityMinigameWalk {
    private final ServerPlayer player;
    private final ServerLevel world;
    private final List<String> notes = new ArrayList<>();
    private String step = "start";
    private MinigameSession session;
    private int ticks;

    private AgilityMinigameWalk(ServerPlayer player) {
        this.player = player;
        this.world = (ServerLevel) player.level();
    }

    private interface Body {
        void run(AgilityMinigameWalk w);
    }

    private static String run(ServerPlayer p, Body body) {
        AgilityMinigameWalk w = new AgilityMinigameWalk(p);
        try {
            body.run(w);
            return w.ticks + " game ticks: " + String.join("; ", w.notes);
        } catch (AssertionError e) {
            throw new AssertionError("[" + w.step + "] " + e.getMessage() + " (after " + w.ticks + " ticks; done: "
                    + String.join("; ", w.notes) + ")");
        } finally {
            ActionManager.cancel(p);
            if (w.session != null && !w.session.isEnded()) {
                w.session.end(false);
            }
        }
    }

    public static String sepulchre(ServerPlayer p) {
        return run(p, AgilityMinigameWalk::sepulchreRun);
    }

    public static String sepulchreTimeout(ServerPlayer p) {
        return run(p, AgilityMinigameWalk::sepulchreOutOfTime);
    }

    // ------------------------------------------------------------------ helpers

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private String blockId(BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).getPath();
    }

    /** Clicks a block from where the player stands; checks the block is there. */
    private void click(BlockPos pos, String id) {
        expect(id.equals(blockId(pos)), "no " + id + " at " + pos + " (found " + blockId(pos) + ")");
        BlockHooks.use(player, id, pos, world.getBlockState(pos));
    }

    private void moveTo(BlockPos stand) {
        player.teleportTo(world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(), 0);
        player.fallDistance = 0;
    }

    /** One game tick: the player's action, then the session; {@code heal} keeps the player alive. */
    private void tick(boolean heal) {
        for (int m = 0; m < ActionManager.MC_TICKS_PER_GAME_TICK; m++) {
            ActionManager.advanceForSelfTest();
        }
        ActionManager.tickNow(player);
        if (session != null && !session.isEnded()) {
            MinigameManager.selfTestTick(session);
        }
        if (heal && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        ticks++;
    }

    private void tick() {
        tick(true);
    }

    private boolean until(int max, BooleanSupplier done) {
        for (int i = 0; i < max; i++) {
            if (done.getAsBoolean()) {
                return true;
            }
            tick();
        }
        return done.getAsBoolean();
    }

    private double xp(Skill s) {
        return Players.data(player).skills().xpTenths(s) / 10.0;
    }

    private void setLevel(Skill s, int level) {
        Players.data(player).skills().setXpTenths(s, Experience.xpForLevel(level) * 10);
    }

    private MinigameSession start(Minigame game) {
        session = MinigameManager.selfTestStart(game, game.entranceBlock(), player);
        expect(session != null && MinigameManager.session(player) == session, game.displayName() + " didn't start a session");
        return session;
    }

    // ------------------------------------------------------------------ Hallowed Sepulchre

    private SepulchreSession sep() {
        return (SepulchreSession) session;
    }

    private void supplies() {
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("rune_crossbow", 1));
        player.setItemInHand(InteractionHand.OFF_HAND, Inv.stack("hallowed_focus", 1));
        for (String[] item : new String[][]{{"strange_old_lockpick", "1"}, {"hammer", "1"}, {"mith_grapple", "1"},
                {"mahogany_plank", "10"}, {"rune_nails", "60"}, {"vampyre_dust", "12"}, {"cosmic_rune", "20"},
                {"water_rune", "20"}, {"hallowed_token", "2"}, {"shark", "5"}}) {
            Inv.give(player, item[0], Integer.parseInt(item[1]));
        }
    }

    private void sepulchreRun() {
        step = "requirements";
        com.runecraft.osrs.QuestInfo sotf = ContentRegistry.quest("sins_of_the_father");
        if (sotf != null) {
            Players.data(player).setQuestStage(sotf, 0);
            expect(SepulchreSession.requirement(player) != null, "entry allowed without Sins of the Father");
            Players.data(player).setQuestStage(sotf, sotf.finalStage());
        }
        setLevel(Skill.AGILITY, 51);
        expect(SepulchreSession.requirement(player) != null, "entry allowed with 51 Agility");
        setLevel(Skill.AGILITY, 99);
        expect(SepulchreSession.requirement(player) == null, "99 Agility and Sins of the Father: " + SepulchreSession.requirement(player));
        int kc = MinigameData.of(player).stat(player.getUUID(), "kc:hallowed_sepulchre");
        supplies();
        start(Minigame.HALLOWED_SEPULCHRE);
        expect(sep().selfTestFloor() == 1 && sep().selfTestTimeLeft() == HallowedSepulchre.START_TICKS,
                "a new run: floor " + sep().selfTestFloor() + ", " + sep().selfTestTimeLeft() + " ticks on the clock");
        int marksBefore = Inv.count(player, "hallowed_mark");
        for (int floor = 1; floor <= HallowedSepulchre.FLOORS; floor++) {
            walkFloor(floor);
        }
        expect(session.isEnded() && MinigameManager.session(player) == null, "the obelisk on floor 5 didn't end the run");
        int marks = Inv.count(player, "hallowed_mark") - marksBefore;
        expect(marks >= 8 + 1 + 2 + 3 + 3 + 4, "the run gave only " + marks + " hallowed marks");
        expect(MinigameData.of(player).stat(player.getUUID(), "kc:hallowed_sepulchre") == kc + 1, "the run didn't count");
        expect(MinigameData.of(player).stat(player.getUUID(), "sepulchre_pb_total") > 0, "no total time recorded");
        notes.add(marks + " marks from the run");

        step = "hallowed sack";
        int kinds = distinctItems();
        Inv.give(player, "hallowed_sack", 1);
        SepulchreSession.openSack(player);
        expect(distinctItems() > kinds, "the hallowed sack gave nothing");
        notes.add("a hallowed sack opened");
    }

    private int distinctItems() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                ids.add(Inv.idOf(player.getInventory().getItem(i)));
            }
        }
        return ids.size();
    }

    private void walkFloor(int floor) {
        SepulchreSession s = sep();
        step = "floor " + floor;
        expect(s.selfTestFloor() == floor, "expected floor " + floor + ", the session is on " + s.selfTestFloor());
        List<SepulchreSession.SelfTestSection> sections = s.selfTestSections();
        String route = s.selfTestEntrance();
        int coffins = 0;
        boolean hitTested = false;
        for (int i = 0; i < sections.size(); i++) {
            SepulchreSession.SelfTestSection sec = sections.get(i);
            step = "floor " + floor + " (" + route + ") section " + i + " " + sec.section().kind()
                    + (sec.section().isTrap() ? " " + sec.section().traps() : "");
            switch (sec.section().kind()) {
                case TRAPS -> {
                    if (!hitTested && floor <= 2) {
                        hitTested = deliberateHit(sec, floor == 2);
                    }
                    cross(sec);
                }
                case COFFIN -> {
                    coffin(i, sec, floor);
                    coffins++;
                }
                case STAIRS -> {
                    moveTo(s.selfTestSpot(sec.u0(), 0));
                    double before = xp(Skill.AGILITY);
                    click(sec.block(), "sep_stairs");
                    expect(s.selfTestWhere(player)[0] == sec.u0() + 2, "the middle stairs didn't take the player down");
                    expect(xp(Skill.AGILITY) - before == HallowedSepulchre.MIDDLE_STAIRS_XP, "middle stairs XP "
                            + (xp(Skill.AGILITY) - before));
                }
                case UPPER_PLATFORMS, PLATFORM -> {
                    moveTo(s.selfTestSpot(sec.u0(), 0));
                    double before = xp(Skill.AGILITY);
                    click(sec.block(), "sep_platform");
                    int gap = sec.section().kind() == Kind.PLATFORM ? 3 : 4;
                    expect(s.selfTestWhere(player)[0] == sec.u0() + gap + 1, "the platform jump didn't land across");
                    double want = sec.section().kind() == Kind.PLATFORM ? HallowedSepulchre.PLATFORM_XP[floor - 1]
                            : HallowedSepulchre.UPPER_PLATFORM_XP * HallowedSepulchre.UPPER_PLATFORMS;
                    expect(xp(Skill.AGILITY) - before == want, "platform XP " + (xp(Skill.AGILITY) - before) + ", wanted " + want);
                }
                case GRAND_COFFIN -> grandCoffin(sec);
            }
            if (floor == 2 && i == 0 && Inv.has(player, "hallowed_token", 1)) {
                int before = s.selfTestTimeLeft();
                expect(SepulchreSession.useToken(player), "a hallowed token didn't work inside");
                Inv.remove(player, "hallowed_token", 1);
                expect(s.selfTestTimeLeft() == before + HallowedSepulchre.TOKEN_TICKS, "the token added "
                        + (s.selfTestTimeLeft() - before) + " ticks");
                notes.add("a hallowed token (+1 minute)");
            }
        }
        step = "floor " + floor + " end";
        expect(s.selfTestPaused(), "the timer isn't paused past the floor's last trap");
        expect(!s.selfTestTimeUp(), "ran out of time on floor " + floor + " (" + s.selfTestTimeLeft() + ")");
        moveTo(s.selfTestSpot(s.selfTestEndU() + 2, 0));
        double before = xp(Skill.AGILITY);
        if (floor < HallowedSepulchre.FLOORS) {
            if (floor == 1) {
                setLevel(Skill.AGILITY, HallowedSepulchre.FLOOR_LEVELS[1] - 1);
                click(s.selfTestEndStairs(), "sep_stairs");
                expect(s.selfTestFloor() == 1, "61 Agility went down to floor 2");
                setLevel(Skill.AGILITY, 99);
                before = xp(Skill.AGILITY);
            }
            int time = s.selfTestTimeLeft();
            click(s.selfTestEndStairs(), "sep_stairs");
            expect(s.selfTestFloor() == floor + 1, "the stairs didn't lead to floor " + (floor + 1));
            expect(s.selfTestTimeLeft() == time + HallowedSepulchre.FLOOR_TICKS, "descending added "
                    + (s.selfTestTimeLeft() - time) + " ticks");
            expect(xp(Skill.AGILITY) - before == HallowedSepulchre.FLOOR_XP[floor - 1], "floor " + floor + " XP "
                    + (xp(Skill.AGILITY) - before));
        } else {
            SysState.of(player).runEnergy = 1000;
            click(s.selfTestObelisk(), "sep_obelisk");
            expect(xp(Skill.AGILITY) - before >= HallowedSepulchre.FLOOR_XP[4], "floor 5's obelisk paid " + (xp(Skill.AGILITY) - before));
            expect(SysState.of(player).runEnergy == 10000, "the obelisk didn't restore run energy with Saradomin items on");
        }
        notes.add("floor " + floor + " (" + route + ", " + coffins + " coffin" + (coffins == 1 ? "" : "s") + ")");
    }

    /** Crosses a trap corridor tile by tile on the pure rules' safe plan at OSRS run speed; a hit fails the walk. */
    private void cross(SepulchreSession.SelfTestSection sec) {
        SepulchreSession s = sep();
        int t0 = sec.trapStart();
        moveTo(s.selfTestSpot(t0 - 1, 0));
        int hits = s.selfTestHits();
        int lane = 0;
        int z = -1;
        for (int guard = 0; guard < 200 && z < HallowedSepulchre.LEN; guard++) {
            List<HallowedSepulchre.Step> plan = HallowedSepulchre.plan(sec.layout(), lane, z, s.selfTestTicks(), 2, 120);
            expect(plan != null && !plan.isEmpty(), "no safe way across from lane " + lane + " tile " + z + " at tick "
                    + s.selfTestTicks());
            HallowedSepulchre.Step next = plan.get(0);
            moveTo(s.selfTestSpot(t0 + next.z(), next.lane()));
            tick();
            int[] where = s.selfTestWhere(player);
            expect(s.selfTestHits() == hits, "a trap hit the player at lane " + next.lane() + " tile " + next.z() + " on the planned path");
            lane = where[1];
            z = where[0] - t0;
        }
        expect(z >= HallowedSepulchre.LEN, "stuck in the corridor at tile " + z);
    }

    /**
     * Steps into a trap on purpose: the player is sent back to the corridor's start, hurt without the hallowed ring and
     * unhurt with it. Returns false if no trap could be found to step into soon.
     */
    private boolean deliberateHit(SepulchreSession.SelfTestSection sec, boolean ring) {
        SepulchreSession s = sep();
        int t0 = sec.trapStart();
        if (ring) {
            SysState.of(player).worn.put(Jewellery.Slot.RING, Inv.stack("hallowed_ring", 1));
        }
        moveTo(s.selfTestSpot(t0 - 1, 0));
        tick();
        for (int wait = 0; wait < 40; wait++) {
            long next = s.selfTestTicks() + 1;
            for (int z = 0; z <= 2; z++) {
                for (int lane = -1; lane <= 1; lane++) {
                    if (HallowedSepulchre.hazard(sec.layout(), lane, -1, z, next) != HallowedSepulchre.Hazard.HIT) {
                        continue;
                    }
                    int hits = s.selfTestHits();
                    float hp = player.getHealth();
                    moveTo(s.selfTestSpot(t0 + z, lane));
                    tick(false);
                    float after = player.getHealth();
                    player.setHealth(player.getMaxHealth());
                    expect(s.selfTestHits() == hits + 1, "stepping into the trap at lane " + lane + " tile " + z + " didn't hit");
                    expect(s.selfTestWhere(player)[0] == t0 - 1, "the hit didn't send the player back to the corridor's start");
                    if (ring) {
                        expect(after >= hp, "the hallowed ring didn't stop the trap's damage (" + hp + " -> " + after + ")");
                        notes.add("a trap with the hallowed ring: sent back, no damage");
                    } else {
                        expect(after < hp, "the trap did no damage (" + hp + " -> " + after + ")");
                        notes.add("a trap without the ring: " + Math.round(hp - after) + " damage, sent back");
                    }
                    until(HallowedSepulchre.RECOVER_TICKS + 1, () -> false);
                    return true;
                }
            }
            tick();
        }
        return false;
    }

    private void coffin(int index, SepulchreSession.SelfTestSection sec, int floor) {
        SepulchreSession s = sep();
        HallowedSepulchre.Challenge c = sec.section().challenge();
        String obstacle = switch (c) {
            case BRIDGE -> "sep_bridge";
            case BRAZIER -> "sep_brazier";
            case PORTAL -> "sep_portal";
            case PILLAR -> "sep_pillar";
        };
        int u = sec.u0() + 2;
        double agility = xp(Skill.AGILITY);
        double skill = xp(c.skill);
        int water = Inv.count(player, "water_rune");
        for (int attempt = 0; attempt < 12 && s.selfTestWhere(player)[1] < SepulchreSession.selfTestAlcoveLane(); attempt++) {
            moveTo(s.selfTestSpot(u, 1));
            click(sec.block(), obstacle);
            until(HallowedSepulchre.RECOVER_TICKS, () -> false);
        }
        expect(s.selfTestWhere(player)[1] == SepulchreSession.selfTestAlcoveLane(), c.title + " didn't let the player across");
        if (c != HallowedSepulchre.Challenge.PILLAR) {
            expect(xp(c.skill) > skill, c.title + " gave no " + c.skill.displayName() + " XP");
        }
        if (c == HallowedSepulchre.Challenge.PORTAL) {
            expect(Inv.count(player, "water_rune") < water, "with the hallowed focus the portal should take Lvl-1 Enchant's runes");
        }
        int marks = Inv.count(player, "hallowed_mark");
        double thieving = xp(Skill.THIEVING);
        for (int attempt = 0; attempt < 20 && !s.selfTestLooted(index); attempt++) {
            moveTo(s.selfTestSpot(u, SepulchreSession.selfTestAlcoveLane() + 1));
            click(sec.coffin(), "sep_coffin");
            tick();
        }
        expect(s.selfTestLooted(index), "the coffin couldn't be looted in 20 tries");
        int got = Inv.count(player, "hallowed_mark") - marks;
        expect(got >= HallowedSepulchre.MARKS[floor - 1][0] && got <= HallowedSepulchre.MARKS[floor - 1][1],
                "floor " + floor + " coffin gave " + got + " marks");
        expect(xp(Skill.THIEVING) - thieving >= HallowedSepulchre.COFFIN_XP, "the coffin gave " + (xp(Skill.THIEVING) - thieving)
                + " Thieving XP");
        moveTo(s.selfTestSpot(u, SepulchreSession.selfTestAlcoveLane()));
        click(sec.block(), obstacle);
        expect(s.selfTestWhere(player)[1] == 1, c.title + " didn't bring the player back");
        double crossing = xp(Skill.AGILITY) - agility;
        expect(crossing >= 2 * HallowedSepulchre.CROSSING_XP[floor - 1], c.title + " crossings gave " + crossing + " Agility XP");
    }

    private void grandCoffin(SepulchreSession.SelfTestSection sec) {
        SepulchreSession s = sep();
        int marks = Inv.count(player, "hallowed_mark");
        for (int attempt = 0; attempt < 20 && !s.selfTestGrandLooted(); attempt++) {
            moveTo(s.selfTestSpot(sec.u0() + 2, 0));
            click(sec.block(), "sep_grand_coffin");
            tick();
        }
        expect(s.selfTestGrandLooted(), "the Grand Hallowed Coffin couldn't be looted");
        int got = Inv.count(player, "hallowed_mark") - marks;
        expect(got >= HallowedSepulchre.GRAND_MARKS[0] && got <= HallowedSepulchre.GRAND_MARKS[1], "the Grand Coffin gave " + got + " marks");
        notes.add("the Grand Hallowed Coffin (" + got + " marks)");
    }

    private void sepulchreOutOfTime() {
        step = "out of time";
        supplies();
        start(Minigame.HALLOWED_SEPULCHRE);
        SepulchreSession s = sep();
        s.selfTestSetTime(2);
        until(4, s::selfTestTimeUp);
        expect(s.selfTestTimeUp(), "the clock didn't run out");
        SepulchreSession.SelfTestSection coffin = null;
        for (SepulchreSession.SelfTestSection sec : s.selfTestSections()) {
            if (sec.section().kind() == Kind.COFFIN && sec.section().challenge() != HallowedSepulchre.Challenge.PILLAR) {
                coffin = sec;
                break;
            }
        }
        if (coffin != null) {
            String id = blockId(coffin.block());
            moveTo(s.selfTestSpot(coffin.u0() + 2, 1));
            click(coffin.block(), id);
            expect(s.selfTestWhere(player)[1] == 1, "a challenge worked after the clock ran out");
        }
        expect(!SepulchreSession.useToken(player), "a hallowed token worked after the clock ran out");
        moveTo(s.selfTestSpot(s.selfTestEndU() + 2, 0));
        click(s.selfTestEndStairs(), "sep_stairs");
        expect(s.selfTestFloor() == 1, "the stairs weren't sealed out of time");
        double before = xp(Skill.AGILITY);
        click(s.selfTestObelisk(), "sep_obelisk");
        expect(session.isEnded(), "the obelisk didn't let the player out");
        expect(xp(Skill.AGILITY) - before == HallowedSepulchre.FLOOR_XP[0], "out of time the obelisk paid " + (xp(Skill.AGILITY) - before));
        notes.add("out of time: challenges, tokens and the stairs refuse; the obelisk still pays 500 XP");
    }
}
