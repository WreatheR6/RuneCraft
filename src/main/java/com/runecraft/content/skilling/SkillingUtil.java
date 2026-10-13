package com.runecraft.content.skilling;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.Loot;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

/** Small helpers shared by the skilling hooks. */
final class SkillingUtil {
    private SkillingUtil() {
    }

    static int level(ServerPlayerEntity player, Skill skill) {
        return Players.data(player).skills().currentLevel(skill);
    }

    /** Checks a level requirement and tells the player if it isn't met. */
    static boolean need(ServerPlayerEntity player, Skill skill, int level, String what) {
        if (level(player, skill) < level) {
            Msg.game(player, "You need " + Msg.article(skill.displayName()) + " level of " + level + " to " + what + ".");
            return false;
        }
        return true;
    }

    static boolean needItem(ServerPlayerEntity player, String item, String message) {
        if (!Inv.has(player, item, 1)) {
            Msg.game(player, message);
            return false;
        }
        return true;
    }

    /** Gives an item, logs it in the collection log and fires the obtain event. */
    static void give(ServerPlayerEntity player, String item, int count) {
        if (count <= 0) {
            return;
        }
        Inv.give(player, item, count);
        if (!item.equals(Inv.COINS)) {
            CollectionLog.record(player, item);
        }
        GameEvents.fireObtain(player, item, count);
    }

    /** Rolls one line of a loot table and gives it; returns the line (or null for an empty table). */
    static Loot roll(ServerPlayerEntity player, List<Loot> table, Random random) {
        if (table.isEmpty()) {
            return null;
        }
        Loot l = Loot.pick(table, random.nextInt(Loot.totalWeight(table)));
        give(player, l.item(), l.amount(random.nextDouble()));
        return l;
    }

    static void giveAll(ServerPlayerEntity player, List<Loot> lines, Random random) {
        for (Loot l : lines) {
            give(player, l.item(), l.amount(random.nextDouble()));
        }
    }

    static String name(String item) {
        ItemDef def = ItemCatalog.get(item);
        if (def != null) {
            return def.name();
        }
        String path = item.substring(item.indexOf(':') + 1).replace('_', ' ');
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    static String blockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).getPath();
    }

    static BlockState moduleBlock(String id) {
        Block block = ModBlocks.MODULE_BLOCKS.get(id);
        return block == null ? null : block.getDefaultState();
    }

    /** Key for per-position storage, unique across dimensions. */
    static String key(World world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "|" + pos.asLong();
    }

    static String heldId(ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        return held.isEmpty() ? "" : Inv.idOf(held);
    }

    /**
     * A repeating action bound to a block: every {@code interval} game ticks {@link #roll} runs while
     * the player stays within reach. Returning false from roll stops it.
     */
    abstract static class BlockAction implements Action {
        private final BlockPos pos;
        private final String blockId;
        private int counter;
        private final int interval;

        BlockAction(BlockPos pos, String blockId, int interval) {
            this.pos = pos;
            this.blockId = blockId;
            this.interval = interval;
        }

        abstract boolean roll(ServerPlayerEntity player, Random random);

        @Override
        public boolean tick(ServerPlayerEntity player, long gameTick) {
            if (player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 36.0) {
                return false;
            }
            if (!blockId(player.getWorld().getBlockState(pos)).equals(blockId)) {
                return false;
            }
            if (++counter < interval) {
                return true;
            }
            counter = 0;
            player.swingHand(Hand.MAIN_HAND, true);
            return roll(player, player.getRandom());
        }
    }

    static void start(ServerPlayerEntity player, Action action) {
        ActionManager.start(player, action);
    }
}
