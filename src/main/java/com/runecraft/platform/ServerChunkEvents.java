package com.runecraft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Fabric's chunk load/unload events, fired from NeoForge's {@code ChunkEvent.Load} / {@code Unload} on server levels. */
public final class ServerChunkEvents {
    private ServerChunkEvents() {
    }

    @FunctionalInterface
    public interface Load {
        void onChunkLoad(ServerLevel world, LevelChunk chunk);
    }

    @FunctionalInterface
    public interface Unload {
        void onChunkUnload(ServerLevel world, LevelChunk chunk);
    }

    public static final Event<Load> CHUNK_LOAD = new Event<>(listeners -> (world, chunk) -> {
        for (Load l : listeners) {
            l.onChunkLoad(world, chunk);
        }
    });

    public static final Event<Unload> CHUNK_UNLOAD = new Event<>(listeners -> (world, chunk) -> {
        for (Unload l : listeners) {
            l.onChunkUnload(world, chunk);
        }
    });
}
