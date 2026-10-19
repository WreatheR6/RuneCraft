package com.runecraft.skill;

import com.runecraft.block.TrapBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.HunterRates;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bird snares and box traps. A trap checks every ~15s whether a creature walked into it, rolling the
 * creature's OSRS catch chance ({@link HunterRates}) for the hunter who laid it. Also keeps the trap
 * limit (OSRS: 1 trap, +1 at 20/40/60/80 Hunter, +1 in the Wilderness) shared by every laid trap,
 * including the skilling module's net traps, deadfalls and pitfalls.
 */
public final class HunterService {
    public static final int CHECK_INTERVAL = 300;

    /** A laid trap: owner and whether it is a placed snare/box trap block (pruned when the block is gone). */
    private record Laid(UUID owner, ServerLevel world, BlockPos pos, boolean block) {
    }

    /** Laid traps by "world|pos" key. */
    private static final Map<String, Laid> LAID = new HashMap<>();

    private HunterService() {
    }

    public static int levelFor(HunterCreature.Trap trap) {
        return trap == HunterCreature.Trap.BIRD_SNARE ? 1 : 27;
    }

    private static String key(ServerLevel world, BlockPos pos) {
        return world.dimension().location() + "|" + pos.asLong();
    }

    /** Number of traps this player currently has laid (stale snare/box trap entries are dropped). */
    public static int laidCount(UUID owner) {
        int n = 0;
        for (Iterator<Laid> it = LAID.values().iterator(); it.hasNext(); ) {
            Laid l = it.next();
            if (l.block() && l.world().hasChunkAt(l.pos()) && !(l.world().getBlockState(l.pos()).getBlock() instanceof TrapBlock)) {
                it.remove();
                continue;
            }
            if (l.owner().equals(owner)) {
                n++;
            }
        }
        return n;
    }

    /** The player's trap limit where they stand. */
    public static int trapLimit(ServerPlayer player) {
        int level = Players.data(player).skills().currentLevel(Skill.HUNTER);
        return HunterRates.trapLimit(level, Gielinor.is(player.level()) && Gielinor.wildernessLevel(player) > 0);
    }

    /** True (after telling the player) when they already have as many traps laid as they may. */
    public static boolean atTrapLimit(ServerPlayer player) {
        int limit = trapLimit(player);
        if (laidCount(player.getUUID()) >= limit) {
            Msg.game(player, "You don't have a high enough Hunter level to set up more than " + limit
                    + (limit == 1 ? " trap." : " traps."));
            return true;
        }
        return false;
    }

    /** Records a laid trap. {@code block} = a placed snare/box trap block. */
    public static void claim(ServerPlayer player, ServerLevel world, BlockPos pos, boolean block) {
        LAID.put(key(world, pos), new Laid(player.getUUID(), world, pos.immutable(), block));
    }

    public static void release(ServerLevel world, BlockPos pos) {
        LAID.remove(key(world, pos));
    }

    /** Owner of a laid trap, or null. */
    public static UUID owner(ServerLevel world, BlockPos pos) {
        Laid l = LAID.get(key(world, pos));
        return l == null ? null : l.owner();
    }

    private static Player hunterFor(ServerLevel world, BlockPos pos) {
        UUID owner = owner(world, pos);
        if (owner != null) {
            Player p = world.getPlayerByUUID(owner);
            if (p != null) {
                return p;
            }
        }
        return world.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 24, false);
    }

    public static void checkTrap(ServerLevel world, BlockPos pos, BlockState state, TrapBlock block) {
        if (state.getValue(TrapBlock.CAUGHT)) {
            return;
        }
        Player hunter = hunterFor(world, pos);
        if (hunter instanceof ServerPlayer sp) {
            int level = Players.data(sp).skills().currentLevel(Skill.HUNTER);
            Region region = Gielinor.is(world) ? com.runecraft.osrs.GielinorMap.regionAtBlock(pos.getX(), pos.getZ()) : Region.MISTHALIN;
            HunterCreature creature = HunterCreature.best(block.trap(), region, level);
            if (creature != null && world.getRandom().nextDouble() < HunterRates.catchChance(creature, level)) {
                world.setBlock(pos, state.setValue(TrapBlock.CAUGHT, true), Block.UPDATE_ALL);
                return;
            }
        }
        world.scheduleTick(pos, block, CHECK_INTERVAL);
    }

    public static void collect(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state, TrapBlock block) {
        UUID owner = owner(world, pos);
        if (owner != null && !owner.equals(player.getUUID())) {
            Msg.game(player, "This isn't your trap.");
            return;
        }
        String trapItem = block.trap() == HunterCreature.Trap.BIRD_SNARE ? "bird_snare" : "box_trap";
        world.removeBlock(pos, false);
        release(world, pos);
        Inv.give(player, trapItem, 1);
        if (!state.getValue(TrapBlock.CAUGHT)) {
            Msg.game(player, "You dismantle the trap.");
            return;
        }
        int level = Players.data(player).skills().currentLevel(Skill.HUNTER);
        Region region = Gielinor.is(world) ? com.runecraft.osrs.GielinorMap.regionAtBlock(pos.getX(), pos.getZ()) : Region.MISTHALIN;
        HunterCreature creature = HunterCreature.best(block.trap(), region, level);
        if (creature == null) {
            Msg.game(player, "The creature escaped.");
            return;
        }
        for (String item : creature.loot()) {
            Inv.give(player, item, 1);
            CollectionLog.obtain(player, item);
        }
        XpService.add(player, Skill.HUNTER, creature.xp());
        Msg.game(player, "You've caught a " + creature.displayName().toLowerCase(java.util.Locale.ROOT) + ".");
    }
}
