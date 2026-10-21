package com.runecraft.content.raids;

import com.runecraft.combat.CombatHandler;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Self-test walk of a whole Chambers of Xeric and Theatre of Blood run (round 33; {@code com.runecraft.selftest} "raid:"
 * checks, real-player mode): the player enters at the entrance, passes the start barrier, walks into every room in turn
 * and plays it the way a player would, through the room's blocks, the real player hit path (vulnerability, immunities,
 * room hooks, points) and the raid clock (fast-forwarded with {@link RaidEffects#advance}, so a run fits in one check),
 * then claims the chest and leaves. Each room's {@link Walkable#walk} checks its own mechanics; anything that doesn't work
 * throws an {@link AssertionError} naming the room and the step.
 */
public final class CoxTobWalk {
    /** A room that knows how a player clears it. */
    interface Walkable {
        /** Clears the room through its own interactions; returns a short note or throws {@link AssertionError}. */
        String walk(CoxTobWalk w);
    }

    private final RaidInstance raid;
    private final ServerPlayer player;
    private final List<String> notes = new ArrayList<>();
    private int ticks;
    private String room = "lobby";
    /** CoX: walk Challenge Mode (every room, in the fixed order) instead of a random normal layout. */
    private boolean challenge;
    /**
     * Every raid strike and projectile that lands during the walk must have been fair (round 42: drawn on creation, the
     * real-time floors); checked after each room.
     */
    private RaidStrikeRecorder recorder;

    private CoxTobWalk(RaidInstance raid, ServerPlayer player) {
        this.raid = raid;
        this.player = player;
    }

    /**
     * Walks a whole Chambers of Xeric raid as {@code p}: scouts and reloads the layout in the lobby, then plays the random
     * normal layout. Returns a summary or throws {@link AssertionError}.
     */
    public static String coxRun(ServerPlayer p, BlockPos entrance) {
        return run(p, RaidType.COX, entrance, false);
    }

    /**
     * Walks a whole Chambers of Xeric Challenge Mode raid as {@code p}: the fixed layout holds every combat and puzzle room,
     * so one run exercises them all. Returns a summary or throws {@link AssertionError}.
     */
    public static String coxChallengeRun(ServerPlayer p, BlockPos entrance) {
        return run(p, RaidType.COX, entrance, true);
    }

    /**
     * The no-heal pass (round 44 playability, {@code raid:noheal:cox} and {@code raid:noheal:tob}): the raid's first combat room
     * (Tekton, Challenge Mode's fixed first room; the Maiden of Sugadinti) fought by a mortal player who is never topped up,
     * on a fixed food and prayer potion budget, reacting to the cues alone ({@link RaidNoHealWalk}). Returns a summary or throws
     * {@link AssertionError}.
     */
    public static String noHeal(ServerPlayer p, BlockPos entrance, RaidType type) {
        RaidManager.enter(p, type, entrance);
        RaidInstance raid = RaidManager.of(p);
        expect(raid != null, "RaidManager.enter did not start a raid (see the log for \"Failed to start a raid\")");
        CoxTobWalk w = new CoxTobWalk(raid, p);
        w.challenge = type == RaidType.COX;
        w.recorder = RaidStrikeRecorder.start();
        try {
            if (type == RaidType.COX) {
                w.coxLobby();
            }
            w.begin();
            String id = raid.rooms.get(1).id();
            w.room = id + " (room 1)";
            w.settle(1);
            w.moveTo(raid.spawn(1));
            w.tick(1);
            expect(raid.currentRoom() == 1 && raid.logic(1).started, "walking in did not start the room");
            String out = RaidNoHealWalk.fight(w.noHealRoom(), 1, "raid:noheal:" + type.id() + " (" + id + ")");
            return id + ": " + out + "; " + w.recorder.summary();
        } catch (AssertionError e) {
            throw new AssertionError("[" + w.room + "] " + e.getMessage(), e);
        } finally {
            w.recorder.close();
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (RaidManager.of(p) == raid) {
                RaidManager.selfTestClose(raid);
            }
        }
    }

    private RaidNoHealWalk.Room noHealRoom() {
        CoxTobWalk w = this;
        return new RaidNoHealWalk.Room() {
            @Override
            public RaidInstance raid() {
                return raid;
            }

            @Override
            public ServerPlayer player() {
                return player;
            }

            @Override
            public void tick(int n) {
                w.tick(n);
            }

            @Override
            public int hit(LivingEntity target, int damage, AttackType type) {
                return w.hit(target, damage, type);
            }
        };
    }

    /** Walks a whole Theatre of Blood raid as {@code p}; returns a summary or throws {@link AssertionError}. */
    public static String tobRun(ServerPlayer p, BlockPos entrance) {
        return run(p, RaidType.TOB, entrance);
    }

    /**
     * Walks a whole Theatre of Blood Hard mode raid as {@code p}: every room's Hard mode changes (Maiden's untouchable
     * blood spawns, the reflecting nylocas and the Prinkipas, Xarpus's opening poison and forced stare, Verzik's debris,
     * urn pools, cycling nylocas and last stand) are checked by the rooms' walks. Returns a summary or throws.
     */
    public static String tobHardRun(ServerPlayer p, BlockPos entrance) {
        return run(p, RaidType.TOB, entrance, com.runecraft.osrs.content.raids.RaidMode.HARD);
    }

    /**
     * ToB Entry mode deaths (OSRS, March 2023): unlimited attempts, so a solo wipe resets the room instead of ending the
     * raid, and hands out ten bandages, which heal, restore prayer and boost combat stats. Returns a summary or throws.
     */
    public static String tobEntryWipe(ServerPlayer p, BlockPos entrance) {
        RaidManager.enter(p, RaidType.TOB, entrance);
        RaidInstance raid = RaidManager.of(p);
        expect(raid != null, "RaidManager.enter did not start a raid");
        CoxTobWalk w = new CoxTobWalk(raid, p);
        w.recorder = RaidStrikeRecorder.start();
        try {
            expect(raid.setMode(com.runecraft.osrs.content.raids.RaidMode.ENTRY), "could not choose Entry mode in the lobby");
            w.begin();
            w.moveTo(raid.spawn(1));
            w.tick(1);
            expect(raid.currentRoom() == 1 && raid.logic(1).started, "walking in did not start the Maiden");
            w.take(com.runecraft.osrs.content.raids.TobRules.BANDAGES);
            for (int attempt = 1; attempt <= 2; attempt++) {
                // Each wipe adds ten bandages to those carried (OSRS wiki: dying on purpose stacks them).
                int carried = w.count(com.runecraft.osrs.content.raids.TobRules.BANDAGES);
                int seeded = RaidSelfTest.seedPending(raid, 1, p);
                raid.onDeath(p);
                expect(RaidManager.of(p) == raid && raid.contains(p), "an Entry mode wipe (attempt " + attempt + ") ended the raid");
                expect(!raid.isDowned(p) && raid.logic(1).started && !raid.logic(1).done, "the Maiden's room did not reset after the wipe");
                // round 43: nothing of the failed attempt is left to land on the retry
                expect(seeded >= 3, "only " + seeded + " test strikes/projectiles/floor marks were pending before the wipe");
                RaidSelfTest.expectCleared(raid, 1, "the ToB Entry wipe (attempt " + attempt + ")");
                int now = w.count(com.runecraft.osrs.content.raids.TobRules.BANDAGES);
                expect(now == carried + com.runecraft.osrs.content.raids.TobRules.ENTRY_BANDAGES,
                        "the wipe left " + now + " bandages (carried " + carried + ", expected ten more)");
                w.tick(1);
            }
            // A bandage heals 20 and boosts Attack like a (slightly weaker) super attack.
            var skills = Players.data(p).skills();
            skills.setBoost(com.runecraft.osrs.Skill.ATTACK, 0);
            p.setHealth(p.getMaxHealth() / 2);
            float before = p.getHealth();
            w.hold(com.runecraft.osrs.content.raids.TobRules.BANDAGES);
            int held = w.count(com.runecraft.osrs.content.raids.TobRules.BANDAGES);
            com.runecraft.platform.UseItemCallback.EVENT.invoker().interact(p, raid.world, InteractionHand.MAIN_HAND);
            expect(p.getHealth() > before, "a bandage did not heal");
            expect(skills.boost(com.runecraft.osrs.Skill.ATTACK) == com.runecraft.osrs.content.raids.TobRules.bandageMeleeBoost(
                    skills.level(com.runecraft.osrs.Skill.ATTACK)), "a bandage did not boost Attack");
            expect(w.count(com.runecraft.osrs.content.raids.TobRules.BANDAGES) == held - 1, "using a bandage did not use it up");
            skills.setBoost(com.runecraft.osrs.Skill.ATTACK, 0);
            // the retried Maiden fights on for a while: whatever she throws must still be fair
            w.tick(15);
            w.recorder.check("the retried Maiden");
            return "two Entry mode wipes reset the Maiden's room and stacked ten bandages each (" + w.recorder.summary() + ")";
        } finally {
            w.recorder.close();
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (RaidManager.of(p) == raid) {
                RaidManager.selfTestClose(raid);
            }
        }
    }

    /**
     * ToB deaths (OSRS): a solo party dying in a fight is a wipe; the Theatre is lost and the player is sent back out.
     * Returns a summary or throws {@link AssertionError}.
     */
    public static String tobWipe(ServerPlayer p, BlockPos entrance) {
        RaidManager.enter(p, RaidType.TOB, entrance);
        RaidInstance raid = RaidManager.of(p);
        expect(raid != null, "RaidManager.enter did not start a raid");
        CoxTobWalk w = new CoxTobWalk(raid, p);
        try {
            w.begin();
            w.moveTo(raid.spawn(1));
            w.tick(1);
            expect(raid.currentRoom() == 1 && raid.logic(1).started, "walking in did not start the Maiden");
            raid.onDeath(p);
            expect(RaidManager.of(p) == null, "a solo death in the Maiden's room did not wipe the party");
            expect(!raid.contains(p), "the wiped player was not sent out of the Theatre");
            return "solo death wiped the party and sent the player out";
        } finally {
            if (RaidManager.of(p) == raid) {
                RaidManager.selfTestClose(raid);
            }
        }
    }

    private static String run(ServerPlayer p, RaidType type, BlockPos entrance) {
        return run(p, type, entrance, com.runecraft.osrs.content.raids.RaidMode.NORMAL);
    }

    private static String run(ServerPlayer p, RaidType type, BlockPos entrance, boolean challenge) {
        return run(p, type, entrance, challenge ? com.runecraft.osrs.content.raids.RaidMode.CHALLENGE
                : com.runecraft.osrs.content.raids.RaidMode.NORMAL);
    }

    private static String run(ServerPlayer p, RaidType type, BlockPos entrance, com.runecraft.osrs.content.raids.RaidMode mode) {
        RaidManager.enter(p, type, entrance);
        RaidInstance raid = RaidManager.of(p);
        expect(raid != null, "RaidManager.enter did not start a raid (see the log for \"Failed to start a raid\")");
        CoxTobWalk w = new CoxTobWalk(raid, p);
        w.challenge = mode == com.runecraft.osrs.content.raids.RaidMode.CHALLENGE;
        w.recorder = RaidStrikeRecorder.start();
        try {
            // Challenge Mode is picked by the leader in the CoX lobby (coxLobby); the other modes before the walk.
            expect(w.challenge || (raid.setMode(mode) && raid.mode() == mode), "could not choose " + mode.displayName() + " in the lobby");
            return w.walk() + "; " + w.recorder.summary();
        } catch (AssertionError e) {
            throw new AssertionError("[" + w.room + "] " + e.getMessage() + " (after " + w.ticks + " ticks; done: " + String.join(", ", w.notes) + ")");
        } finally {
            w.recorder.close();
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (RaidManager.of(p) == raid) {
                RaidManager.selfTestClose(raid);
            }
        }
    }

    // ------------------------------------------------------------------ the walk

    private void begin() {
        expect(raid.currentRoom() == 0 && raid.inRoom(player.position(), 0), "the party did not arrive in the lobby");
        BlockPos barrier = raid.center(0).offset(RaidBuilder.HALF + 1, 1, 0);
        expect(RaidsContent.START_BARRIER.equals(blockId(barrier)), "no start barrier in the lobby's exit (found " + blockId(barrier) + ")");
        moveTo(barrier.offset(-1, 0, 0));
        use(barrier);
        expect(raid.begun(), "the start barrier did not begin the raid");
        expect(raid.world.getBlockState(barrier).isAir(), "the start barrier did not open");
    }

    /**
     * The CoX lobby (OSRS): the party scouts the layout; the leader can reload it, or pick Challenge Mode, whose layout is
     * fixed and holds every room.
     */
    private void coxLobby() {
        String scouted = raid.scout();
        expect(scouted != null && scouted.startsWith("Floor 1: "), "the lobby did not scout the layout (" + scouted + ")");
        if (challenge) {
            expect(raid.setMode(com.runecraft.osrs.content.raids.RaidMode.CHALLENGE), "the leader could not pick Challenge Mode");
            List<String> ids = new ArrayList<>();
            for (RoomDef r : raid.rooms) {
                ids.add(r.id().equals("cox_hunting") || r.id().equals("cox_farming") ? "F" : r.id());
            }
            List<String> want = new ArrayList<>(List.of("cox_lobby"));
            for (List<String> floor : com.runecraft.osrs.content.raids.RaidLayouts.COX_CM_FLOORS) {
                want.addAll(floor);
            }
            want.add("olm");
            want.add("cox_reward");
            expect(ids.equals(want), "Challenge Mode did not lay out its fixed rooms: " + ids);
            expect(!raid.reloadLayout(), "Challenge Mode's fixed layout could be reloaded");
            notes.add("Challenge Mode, " + raid.rooms.size() + " rooms");
        } else {
            boolean changed = false;
            for (int i = 0; i < 10 && !changed; i++) {
                expect(raid.reloadLayout(), "the leader could not reload the layout");
                changed = !raid.scout().equals(scouted);
            }
            expect(changed, "reloading the layout ten times never changed it");
            expect(raid.logic(1).def.id().equals(raid.rooms.get(1).id()), "the reloaded first room has the old room's logic");
            expect("red_stained_glass".equals(blockId(raid.center(1).offset(RaidBuilder.HALF + 1, 1, 0))),
                    "the reloaded first room was not built");
            notes.add("layout reloaded: " + raid.scout());
        }
    }

    private String walk() {
        if (raid.type == RaidType.COX) {
            coxLobby();
        }
        begin();
        int last = raid.rooms.size() - 1;
        boolean diedOnce = false;
        for (int i = 1; i <= last; i++) {
            RoomDef def = raid.rooms.get(i);
            room = def.id() + " (room " + i + ")";
            int pointsBefore = raid.points(player.getUUID());
            settle(i);
            // The previous room's exit is open: walk down the corridor into this room.
            moveTo(raid.spawn(i));
            tick(1);
            expect(raid.currentRoom() == i && raid.logic(i).started, "walking in did not start the room");
            if (def.kind() == RoomDef.Kind.REWARD) {
                if (raid.type == RaidType.COX && challenge) {
                    // OSRS: Challenge Mode within the target time adds 5,000 points per player.
                    boolean within = raid.elapsedTicks() <= com.runecraft.osrs.content.raids.RaidScaling.targetTicks(RaidType.COX,
                            raid.mode(), raid.partySize());
                    int bonus = raid.points(player.getUUID()) - pointsBefore;
                    expect(bonus == (within ? com.runecraft.osrs.content.raids.RaidScaling.COX_CM_TARGET_BONUS : 0),
                            "Challenge Mode " + (within ? "within" : "over") + " the target time gave " + bonus + " bonus points");
                    notes.add("CM " + raid.elapsedTicks() + " ticks (" + (within ? "within" : "over") + " target, +" + bonus + ")");
                }
                break;
            }
            RoomLogic logic = raid.logic(i);
            String note = logic instanceof Walkable wk ? wk.walk(this) : killAll(logic);
            until(() -> logic.done, 40, "the room did not complete");
            recorder.check(def.id());
            expect(raid.world.getBlockState(raid.center(i).offset(RaidBuilder.HALF + 1, 1, 0)).isAir(), "the room's exit did not open");
            notes.add(def.id() + " (" + note + ")");
            if (raid.type == RaidType.COX && !diedOnce && def.combat()) {
                diedOnce = true;
                notes.add(coxDeath(i));
            }
        }
        room = "reward room";
        expect(raid.finished(), "walking into the reward room did not complete the raid");
        player.getInventory().clearContent();
        BlockPos chest = raid.center(last).offset(6, 1, 0);
        expect(RaidsContent.REWARD_CHEST.equals(blockId(chest)), "no reward chest (found " + blockId(chest) + ")");
        moveTo(chest.offset(-1, 0, 0));
        use(chest);
        int loot = 0;
        for (int s = 0; s < player.getInventory().getContainerSize(); s++) {
            loot += player.getInventory().getItem(s).getCount();
        }
        expect(raid.hasClaimed(player) && loot > 0, "the reward chest gave nothing");
        BlockPos exit = raid.center(last).offset(RaidBuilder.HALF - 1, 1, 0);
        expect(RaidsContent.EXIT.equals(blockId(exit)), "no exit in the reward room (found " + blockId(exit) + ")");
        use(exit);
        expect(RaidManager.of(player) == null, "the exit did not take the player out of the raid");
        return raid.rooms.size() + " rooms in " + ticks + " ticks: " + String.join("; ", notes) + "; chest gave " + loot + " items";
    }

    /**
     * CoX deaths (OSRS): 40% of your points are lost (5% of the team's if you had almost none) and you respawn at the start
     * of the floor you have reached.
     */
    private String coxDeath(int room) {
        int before = raid.points(player.getUUID());
        int team = raid.totalPoints();
        raid.onDeath(player);
        int after = raid.points(player.getUUID());
        boolean teamHit = before <= 0 || com.runecraft.osrs.content.raids.RaidScaling.coxDeathHitsTeam(before, team);
        int want = teamHit ? com.runecraft.osrs.content.raids.RaidScaling.coxTeamDeathPenalty(before)
                : com.runecraft.osrs.content.raids.RaidScaling.coxDeathPenalty(before);
        expect(after == want, "a CoX death took " + before + " -> " + after + " points (expected " + want + ")");
        int floor = raid.floorStart(raid.currentRoom());
        expect(raid.inRoom(player.position(), floor), "a CoX death did not send the player back to the start of the floor (room "
                + floor + ")");
        expect(RaidManager.of(player) == raid, "a CoX death removed the player from the raid");
        moveTo(raid.spawn(room));
        return "death: " + before + " -> " + after + " points, respawned in room " + floor;
    }

    /**
     * Gives the server the moment a party walking in real time always gives it before entering room {@code room}. The walk
     * plays a whole raid inside one server tick, but a chunk the raid force-loads only becomes visible to entity searches
     * (and its creatures and items only tick) once the server confirms the chunk on its main thread, normally between
     * ticks. A room is built as the party enters the one before it (a re-used instance slot re-loads chunks it released a
     * tick ago), so it can still be hidden when the walk arrives: an item dropped there (the Dawnbringer) or a creature
     * spawned there is then left out of every area search. This runs the server's queued chunk work until every chunk of
     * the room is entity-ticking ({@link com.runecraft.selftest.ChunkSettle}); a room still loading after that is noted.
     */
    private void settle(int room) {
        if (!com.runecraft.selftest.ChunkSettle.settle(raid.world, raid.center(room), RaidBuilder.HALF + 1)) {
            notes.add("room " + room + "'s chunks were still loading after "
                    + com.runecraft.selftest.ChunkSettle.MAX_NANOS / 1_000_000_000L + " s");
        }
    }

    /** The default solve: kill every creature the room tracks. */
    private String killAll(RoomLogic logic) {
        int n = 0;
        for (LivingEntity e : new ArrayList<>(logic.spawned)) {
            if (logic.alive(e)) {
                kill(e, AttackType.SLASH, RaidInstance.creatureId(e));
                n++;
            }
        }
        return n + " killed";
    }

    // ------------------------------------------------------------------ driver API for the rooms

    RaidInstance raid() {
        return raid;
    }

    ServerPlayer player() {
        return player;
    }

    /** Advances the raid by {@code n} game ticks: the raid clock, delayed effects and strikes, and the raid's rooms. */
    void tick(int n) {
        for (int i = 0; i < n; i++) {
            // one game tick of the world clock (the raid clock follows it) with the monster hits in flight moved on: since round
            // 41 they land after their wind-up, flight or telegraph (Mechanics), as the real server's ticks would land them
            for (int m = 0; m < com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK; m++) {
                com.runecraft.action.ActionManager.advanceForSelfTest();
                com.runecraft.boss.Mechanics.tickForSelfTest();
                // round 44: a player played like a real one (the no-heal pass) moves, prays and eats every Minecraft tick
                com.runecraft.selftest.RealPlayer real = com.runecraft.selftest.RealPlayer.of(player);
                if (real != null) {
                    real.tick();
                }
            }
            RaidEffects.gameTick();
            if (RaidManager.of(player) == raid) {
                raid.gameTick(RaidEffects.now());
            }
            ticks++;
            heal();
        }
    }

    /** Keeps the player topped up (the real-player self-test player is invulnerable, but drains still apply). */
    void heal() {
        com.runecraft.selftest.RealPlayer real = com.runecraft.selftest.RealPlayer.of(player);
        if (real != null && real.mortal()) {
            return; // the no-heal pass: the player lives on its own food and potions
        }
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        var data = Players.data(player);
        if (data.prayerPoints() < 50) {
            data.setPrayerPoints(99);
        }
    }

    /** Ticks until {@code cond} holds (at most {@code max} ticks), else fails with {@code what}. */
    void until(BooleanSupplier cond, int max, String what) {
        for (int i = 0; i < max && !cond.getAsBoolean(); i++) {
            tick(1);
        }
        expect(cond.getAsBoolean(), what + " (waited " + max + " ticks)");
    }

    /** A hit by the player through the real path after the roll (hooks, vulnerability, immunities, room rules, points). */
    int hit(LivingEntity target, int damage, AttackType type) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return 0;
        }
        int d = CombatHandler.hookAfterRoll(player, target, type, damage, damage);
        if (target instanceof OsrsMonsterEntity m) {
            d = m.modifyIncoming(player, d, type);
        }
        d = CombatHandler.protectionAgainstPlayer(target, type, d);
        int dealt = CombatHandler.applyDamage(player, target, d);
        CombatHandler.hookAfterHit(player, target, type, dealt);
        return dealt;
    }

    /** Hits a creature with {@code type} (a game tick between hits) until it dies; fails if it won't. */
    void kill(LivingEntity target, AttackType type, String what) {
        expect(target != null, what + " is missing");
        int guard = 0;
        while (target.isAlive() && !target.isRemoved() && guard++ < 500) {
            int max = raid.maxHitpoints(target);
            hit(target, Math.max(5, Math.min(Math.max(1, raid.hitpoints(target)), max / 5 + 1)), type);
            tick(1);
        }
        expect(!target.isAlive() || target.isRemoved(), what + " did not die (" + raid.hitpoints(target) + "/" + raid.maxHitpoints(target)
                + " hitpoints after " + guard + " hits)");
    }

    /** Right-clicks the raid block at {@code pos}, as the player. */
    void use(BlockPos pos) {
        RaidManager.useBlock(player, blockId(pos), pos);
    }

    String blockId(BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(raid.world.getBlockState(pos).getBlock()).getPath();
    }

    /** Every block of a raid block type standing in a room. */
    List<BlockPos> blocks(RoomLogic room, String blockId) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos c = room.center();
        for (int dx = -RaidBuilder.HALF; dx <= RaidBuilder.HALF; dx++) {
            for (int dz = -RaidBuilder.HALF; dz <= RaidBuilder.HALF; dz++) {
                for (int dy = 0; dy <= RaidBuilder.HEIGHT; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (blockId.equals(blockId(p))) {
                        found.add(p);
                    }
                }
            }
        }
        return found;
    }

    /** The first block of a raid block type in a room; fails if there is none. */
    BlockPos block(RoomLogic room, String blockId) {
        List<BlockPos> found = blocks(room, blockId);
        expect(!found.isEmpty(), "no " + blockId + " in the room");
        return found.get(0);
    }

    /** Stands the player at a standing position (the block above the floor). */
    void moveTo(BlockPos stand) {
        player.teleportTo(raid.world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(), 0);
        player.fallDistance = 0;
    }

    void moveTo(Vec3 at) {
        player.teleportTo(raid.world, at.x, at.y, at.z, player.getYRot(), 0);
        player.fallDistance = 0;
    }

    void give(String item, int n) {
        Inv.give(player, item, n);
    }

    /** Removes every one of an item the player carries. */
    void take(String item) {
        Inv.remove(player, item, Inv.count(player, item));
    }

    int count(String item) {
        return Inv.count(player, item);
    }

    /** Puts an item in the player's main hand (null: empty-handed). */
    void hold(String item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, item == null ? ItemStack.EMPTY : Inv.stack(item, 1));
    }

    void note(String s) {
        notes.add(s);
    }

    /** Whether a prayer is switched on (its toggle state, as the prayer book shows it). */
    boolean prayerOn(com.runecraft.osrs.Prayer prayer) {
        return Players.data(player).isActive(prayer);
    }

    /**
     * Switches a prayer through the path the prayer keys use ({@code PrayerService.set}), then lets a game tick pass, longer
     * than the switch grace, so the protection read afterwards sees only the new state. // playability (round 42): walks
     * switch prayers like a player instead of setting them
     */
    void pray(com.runecraft.osrs.Prayer prayer, boolean on) {
        com.runecraft.combat.PrayerService.set(player, prayer, on);
        expect(prayerOn(prayer) == on, "could not switch " + prayer + (on ? " on" : " off") + " ("
                + com.runecraft.combat.PrayerService.lastRefusal + ")");
        tick(1);
    }

    static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }
}
