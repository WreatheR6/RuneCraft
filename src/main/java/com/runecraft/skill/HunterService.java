package com.runecraft.skill;

import com.runecraft.block.TrapBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Bird snares and box traps. A trap checks every ~15s whether a creature walked into it. */
public final class HunterService {
    public static final int CHECK_INTERVAL = 300;

    private HunterService() {
    }

    public static int levelFor(HunterCreature.Trap trap) {
        return trap == HunterCreature.Trap.BIRD_SNARE ? 1 : 27;
    }

    public static void checkTrap(ServerWorld world, BlockPos pos, BlockState state, TrapBlock block) {
        if (state.get(TrapBlock.CAUGHT)) {
            return;
        }
        PlayerEntity hunter = world.getClosestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 24, false);
        if (hunter instanceof ServerPlayerEntity sp) {
            int level = Players.data(sp).skills().currentLevel(Skill.HUNTER);
            Region region = Gielinor.is(world) ? com.runecraft.osrs.GielinorMap.regionAtBlock(pos.getX(), pos.getZ()) : Region.MISTHALIN;
            HunterCreature creature = HunterCreature.best(block.trap(), region, level);
            if (creature != null && world.getRandom().nextDouble() < Chance.lerp(level - creature.level() + 1, 80, 240)) {
                world.setBlockState(pos, state.with(TrapBlock.CAUGHT, true), Block.NOTIFY_ALL);
                return;
            }
        }
        world.scheduleBlockTick(pos, block, CHECK_INTERVAL);
    }

    public static void collect(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, TrapBlock block) {
        String trapItem = block.trap() == HunterCreature.Trap.BIRD_SNARE ? "bird_snare" : "box_trap";
        world.removeBlock(pos, false);
        Inv.give(player, trapItem, 1);
        if (!state.get(TrapBlock.CAUGHT)) {
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
