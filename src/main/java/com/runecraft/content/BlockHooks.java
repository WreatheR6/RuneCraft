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

    public static void use(ServerPlayerEntity player, String blockId, BlockPos pos, BlockState state) {
        Handler h = HANDLERS.get(blockId);
        if (h != null) {
            h.use(player, pos, state);
        } else {
            com.runecraft.util.Msg.game(player, "Nothing interesting happens.");
        }
    }
}
