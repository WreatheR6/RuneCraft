package com.runecraft.content.npcs;

import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.npcs.NpcsContent;
import com.runecraft.osrs.content.npcs.TownNpc;
import com.runecraft.osrs.content.npcs.Townsfolk;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import java.util.HashSet;
import java.util.Set;

/**
 * Keeps NPCs on the floor of the building they work in. Module NPCs are spawned on top of the
 * highest block of their column, which is the roof when a hand-built shop stands there; this moves
 * them down to the indoor floor (a standable spot below the roof whose floor isn't natural ground).
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

    private static boolean natural(BlockState s) {
        return s.isIn(BlockTags.DIRT) || s.isIn(BlockTags.BASE_STONE_OVERWORLD) || s.isIn(BlockTags.SAND)
                || s.isOf(Blocks.GRAVEL) || !s.getFluidState().isEmpty();
    }

    private static boolean standable(ServerWorld world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        return !below.getCollisionShape(world, pos.down()).isEmpty()
                && feet.getCollisionShape(world, pos).isEmpty() && feet.getFluidState().isEmpty()
                && head.getCollisionShape(world, pos.up()).isEmpty();
    }

    /**
     * The indoor floor of column (x, z): the first standable spot below the highest block whose floor
     * isn't natural ground (i.e. the floor of a building under its roof), or -1 if there is none.
     */
    static int indoorY(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        int floorLimit = Math.max(world.getBottomY() + 1, Math.max(world.getSeaLevel() - 2, top - 16));
        for (int y = top - 1; y > floorLimit; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (standable(world, p)) {
                return natural(world.getBlockState(p.down())) ? -1 : y;
            }
        }
        return -1;
    }

    /** Y an NPC should stand at in column (x, z): the indoor floor under a roof, else the surface. */
    static int standY(ServerWorld world, int x, int z) {
        int indoor = indoorY(world, x, z);
        return indoor >= 0 ? indoor : world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** Moves an NPC down to the floor of the building around it (off the roof), or out of a block it's stuck in. */
    static void settle(ServerWorld world, QuestNpcEntity e) {
        BlockPos cur = e.getBlockPos();
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cur.getX(), cur.getZ());
        if (cur.getY() < top && standable(world, cur) && !natural(world.getBlockState(cur.down()))) {
            return; // already on a floor inside a building
        }
        int indoor = indoorY(world, cur.getX(), cur.getZ());
        int y;
        if (indoor >= 0) {
            y = indoor;
        } else if (!standable(world, cur)) {
            y = top;
        } else {
            return;
        }
        if (Math.abs(y - e.getY()) >= 1.0) {
            e.requestTeleport(cur.getX() + 0.5, y, cur.getZ() + 0.5);
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
