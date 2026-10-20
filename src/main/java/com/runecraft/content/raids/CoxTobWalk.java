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

    private CoxTobWalk(RaidInstance raid, ServerPlayer player) {
        this.raid = raid;
        this.player = player;
    }

    /** Walks a whole Chambers of Xeric raid as {@code p}; returns a summary or throws {@link AssertionError}. */
    public static String coxRun(ServerPlayer p, BlockPos entrance) {
        return run(p, RaidType.COX, entrance);
    }

    /** Walks a whole Theatre of Blood raid as {@code p}; returns a summary or throws {@link AssertionError}. */
    public static String tobRun(ServerPlayer p, BlockPos entrance) {
        return run(p, RaidType.TOB, entrance);
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
        RaidManager.enter(p, type, entrance);
        RaidInstance raid = RaidManager.of(p);
        expect(raid != null, "RaidManager.enter did not start a raid (see the log for \"Failed to start a raid\")");
        CoxTobWalk w = new CoxTobWalk(raid, p);
        try {
            return w.walk();
        } catch (AssertionError e) {
            throw new AssertionError("[" + w.room + "] " + e.getMessage() + " (after " + w.ticks + " ticks; done: " + String.join(", ", w.notes) + ")");
        } finally {
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

    private String walk() {
        begin();
        int last = raid.rooms.size() - 1;
        boolean diedOnce = false;
        for (int i = 1; i <= last; i++) {
            RoomDef def = raid.rooms.get(i);
            room = def.id() + " (room " + i + ")";
            // The previous room's exit is open: walk down the corridor into this room.
            moveTo(raid.spawn(i));
            tick(1);
            expect(raid.currentRoom() == i && raid.logic(i).started, "walking in did not start the room");
            if (def.kind() == RoomDef.Kind.REWARD) {
                break;
            }
            RoomLogic logic = raid.logic(i);
            String note = logic instanceof Walkable wk ? wk.walk(this) : killAll(logic);
            until(() -> logic.done, 40, "the room did not complete");
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

    /** CoX deaths (OSRS): 40% of your points are lost and you respawn at the start of the floor. */
    private String coxDeath(int room) {
        int before = raid.points(player.getUUID());
        raid.onDeath(player);
        int after = raid.points(player.getUUID());
        expect(after == com.runecraft.osrs.content.raids.RaidScaling.coxDeathPenalty(before), "a CoX death took " + before + " -> " + after
                + " points (expected -40%)");
        expect(raid.inRoom(player.position(), 0), "a CoX death did not send the player back to the start");
        expect(RaidManager.of(player) == raid, "a CoX death removed the player from the raid");
        moveTo(raid.spawn(room));
        return "death: " + before + " -> " + after + " points";
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
            RaidEffects.advance(1);
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

    static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }
}
