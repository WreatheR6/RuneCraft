package com.runecraft.content;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Click handlers for {@code BlockDef.Kind.SCRIPTED} module blocks. */
public final class BlockHooks {
    @FunctionalInterface
    public interface Handler {
        void use(ServerPlayerEntity player, BlockPos pos, BlockState state);
    }

    private static final Map<String, Handler> HANDLERS = new HashMap<>();

    private BlockHooks() {
    }

    public static void register(String blockId, Handler handler) {
        HANDLERS.put(blockId, handler);
    }

    /**
     * Handlers used when a block's own handler has nothing to do (e.g. a minigame block clicked by a
     * player who isn't in that minigame: the Wilderness Agility Course's ticket dispenser).
     */
    private static final Map<String, Handler> FALLBACKS = new HashMap<>();

    public static void registerFallback(String blockId, Handler handler) {
        FALLBACKS.put(blockId, handler);
    }

    /** Runs the fallback handler of the block, if any; true if there was one. */
    public static boolean useFallback(ServerPlayerEntity player, String blockId, BlockPos pos, BlockState state) {
        Handler h = FALLBACKS.get(blockId);
        if (h == null) {
            return false;
        }
        h.use(player, pos, state);
        return true;
    }

    public static void use(ServerPlayerEntity player, String blockId, BlockPos pos, BlockState state) {
        Handler h = HANDLERS.get(blockId);
        if (h != null) {
            h.use(player, pos, state);
        } else {
            com.runecraft.util.Msg.game(player, "Nothing interesting happens.");
        }
    }
}
