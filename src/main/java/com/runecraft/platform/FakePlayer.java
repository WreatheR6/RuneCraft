package com.runecraft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Fabric's fake player lookup: the level's default fake player (NeoForge's {@code FakePlayerFactory}). */
public final class FakePlayer {
    private FakePlayer() {
    }

    public static ServerPlayer get(ServerLevel world) {
        return FakePlayerFactory.getMinecraft(world);
    }
}
