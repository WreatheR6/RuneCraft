package com.runecraft.content.npcs;

import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.npcs.NpcsContent;
import com.runecraft.osrs.content.npcs.TownNpc;
import com.runecraft.osrs.content.npcs.Townsfolk;
import com.runecraft.world.NpcFloor;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import java.util.HashSet;
import java.util.Set;

/**
 * Keeps NPCs on the floor of the building they work in ({@link NpcFloor}): a building may be placed
 * around an NPC after it spawned, so bankers are re-seated beside their bank booth and anyone left on
 * a roof is moved down to the column's lowest floor.
 */
final class NpcPlacement {
    private static final Set<String> MINE = new HashSet<>();

    static {
        for (TownNpc n : Townsfolk.all()) {
            MINE.add(n.id());
        }
    }

    private NpcPlacement() {
    }

    static boolean isMine(QuestNpcEntity e) {
        return MINE.contains(e.npcId());
    }

    /** Y a town resident should spawn at in column (x, z): the lowest floor there (not the roof). */
    static int standY(ServerWorld world, int x, int z) {
        return NpcFloor.lowestY(world, x, z);
    }

    /**
     * Puts an NPC back on its floor when the world changed around it: bankers go beside the nearest
     * bank booth (the bank may have been built after they spawned); anyone standing on top of their
     * column (a roof) or stuck inside blocks moves to the column's lowest floor. NPCs already standing
     * on a floor under a roof are left alone.
     */
    static void settle(ServerWorld world, QuestNpcEntity e) {
        BlockPos cur = e.getBlockPos();
        if (NpcFloor.isBanker(e.npcId(), e.getName().getString())) {
            if (NpcFloor.standable(world, cur) && NpcFloor.nextToBooth(world, cur)) {
                return;
            }
            BlockPos booth = NpcFloor.besideBooth(world, cur.getX(), cur.getZ());
            if (booth != null) {
                e.requestTeleport(booth.getX() + 0.5, NpcFloor.feetY(world, booth), booth.getZ() + 0.5);
                return;
            }
        }
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cur.getX(), cur.getZ());
        if (cur.getY() < top && NpcFloor.standable(world, cur)) {
            return; // already on a floor inside a building
        }
        int y = NpcFloor.lowestY(world, cur.getX(), cur.getZ());
        if (y != cur.getY() || !NpcFloor.standable(world, cur)) {
            BlockPos to = new BlockPos(cur.getX(), y, cur.getZ());
            double feet = NpcFloor.feetY(world, to);
            if (Math.abs(feet - e.getY()) >= 0.5) {
                e.requestTeleport(cur.getX() + 0.5, feet, cur.getZ() + 0.5);
            }
        }
    }

    /** Settles every one of this module's NPCs in a place right after the place is built. */
    static void settlePlace(ServerWorld world, BlockPos centre, String placeId) {
        for (NpcDef def : ContentRegistry.npcs()) {
            if (!placeId.equals(def.place()) || !MINE.contains(def.id())) {
                continue;
            }
            int x = centre.getX() + def.dx();
            int z = centre.getZ() + def.dz();
            Box box = new Box(x - 2, world.getBottomY(), z - 2, x + 3, world.getTopY(), z + 3);
            for (QuestNpcEntity e : world.getEntitiesByClass(QuestNpcEntity.class, box, n -> def.id().equals(n.npcId()))) {
                settle(world, e);
            }
        }
    }

    /** Every place this module generated (one per chunk with NPCs). */
    static Set<String> placeIds() {
        Set<String> ids = new HashSet<>();
        for (TownNpc n : Townsfolk.all()) {
            ids.add(NpcsContent.placeId(n.x(), n.y()));
        }
        return ids;
    }
}
