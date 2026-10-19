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
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Small helpers shared by the skilling hooks. */
final class SkillingUtil {
    private SkillingUtil() {
    }

    static int level(ServerPlayer player, Skill skill) {
        return Players.data(player).skills().currentLevel(skill);
    }

    /** Checks a level requirement and tells the player if it isn't met. */
    static boolean need(ServerPlayer player, Skill skill, int level, String what) {
        if (level(player, skill) < level) {
            Msg.game(player, "You need " + Msg.article(skill.displayName()) + " level of " + level + " to " + what + ".");
            return false;
        }
        return true;
    }

    static boolean needItem(ServerPlayer player, String item, String message) {
        if (!Inv.has(player, item, 1)) {
            Msg.game(player, message);
            return false;
        }
        return true;
    }

    /** Gives an item, logs it in the collection log and fires the obtain event. */
    static void give(ServerPlayer player, String item, int count) {
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
    static Loot roll(ServerPlayer player, List<Loot> table, RandomSource random) {
        if (table.isEmpty()) {
            return null;
        }
        Loot l = Loot.pick(table, random.nextInt(Loot.totalWeight(table)));
        give(player, l.item(), l.amount(random.nextDouble()));
        return l;
    }

    static void giveAll(ServerPlayer player, List<Loot> lines, RandomSource random) {
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
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }

    static BlockState moduleBlock(String id) {
        Block block = ModBlocks.MODULE_BLOCKS.get(id);
        return block == null ? null : block.defaultBlockState();
    }

    /** Key for per-position storage, unique across dimensions. */
    static String key(Level world, BlockPos pos) {
        return world.dimension().location() + "|" + pos.asLong();
    }

    static String heldId(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
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

        abstract boolean roll(ServerPlayer player, RandomSource random);

        @Override
        public boolean tick(ServerPlayer player, long gameTick) {
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 36.0) {
                return false;
            }
            if (!blockId(player.level().getBlockState(pos)).equals(blockId)) {
                return false;
            }
            if (++counter < interval) {
                return true;
            }
            counter = 0;
            player.swing(InteractionHand.MAIN_HAND, true);
            return roll(player, player.getRandom());
        }
    }

    static void start(ServerPlayer player, Action action) {
        ActionManager.start(player, action);
    }
}
