package com.runecraft.content.bosses;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.BossLairs;
import com.runecraft.osrs.world.ArenaLayout;
import com.runecraft.util.Msg;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The underground lairs' way in and out ({@link BossLairs}): the entrance in each surface hut takes a player down to their
 * lair's floor, south of the arena's opening; the exit portal in the lair takes them back in front of the hut (or beside the
 * door they came in by: Mokhaiotl's, in the Ruins of Mokhaiotl).
 */
public final class BossLairHooks {
    private BossLairHooks() {
    }

    static void register() {
        com.runecraft.content.BlockHooks.register(BossLairs.ENTRANCE, (p, pos, state) -> enter(p, pos));
        com.runecraft.content.BlockHooks.register(BossLairs.EXIT, (p, pos, state) -> leave(p, pos));
    }

    private static int radius(String bossId) {
        var b = ContentRegistry.boss(bossId);
        return b == null ? 12 : b.arenaRadius;
    }

    /** Takes the player into the lair whose entrance stands at {@code pos}. Returns whether they went down. */
    public static boolean enter(ServerPlayer p, BlockPos pos) {
        BossLairs.Lair lair = BossLairs.byEntrance(pos.getX(), pos.getY(), pos.getZ());
        if (lair == null || !(p.level() instanceof ServerLevel world)) {
            Msg.game(p, "Nothing interesting happens.");
            return false;
        }
        return enter(p, world, lair);
    }

    /** Takes the player down into a lair (the cave is built first if its chunks never were). */
    public static boolean enter(ServerPlayer p, ServerLevel world, BossLairs.Lair lair) {
        ArenaLayout.Arena a = ArenaLayout.of(lair.bossId());
        if (a == null || !a.underground()) {
            Msg.game(p, "The way is blocked.");
            return false;
        }
        int r = a.radius();
        com.runecraft.world.StructurePlacer.applyAround(world, a.x(), a.z(), lair.half(r) + 2);
        int[] to = lair.arrival(r);
        Msg.game(p, "You make your way down into " + lair.note() + ".");
        p.teleportTo(world, to[0] + 0.5, to[1], to[2] + 0.5, 180f, 0f);
        return true;
    }

    /** Takes the player out of the lair whose exit stands at {@code pos}. Returns whether they left. */
    public static boolean leave(ServerPlayer p, BlockPos pos) {
        BossLairs.Lair lair = BossLairs.at(pos.getX(), pos.getY(), pos.getZ(), BossLairHooks::radius);
        if (lair == null || !(p.level() instanceof ServerLevel world)) {
            Msg.game(p, "Nothing interesting happens.");
            return false;
        }
        int[] to = lair.exitTo();
        com.runecraft.world.StructurePlacer.applyAround(world, to[0], to[2], 8);
        Msg.game(p, "You leave " + lair.note() + ".");
        p.teleportTo(world, to[0] + 0.5, to[1], to[2] + 0.5, 0f, 0f);
        return true;
    }
}
