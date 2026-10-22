package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.BossLairs;
import com.runecraft.osrs.world.ArenaLayout;
import com.runecraft.util.Inv;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A real player's way into an underground lair and out again ({@link BossLairs}), for the boss walks: in by its entrance
 * (the surface hut's, or the Ruins of Mokhaiotl's door in the Tonali Cavern), onto the lair floor, the boss summoned at the
 * lair's stone onto the arena floor, and out by the exit portal back to where they came in.
 */
public final class LairWalk {
    private LairWalk() {
    }

    private static IllegalStateException fail(String message) {
        return new IllegalStateException(message);
    }

    private static String id(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }

    /** Walks a player into the boss's lair, summons it there and walks out. Returns a summary or throws. */
    public static String visit(ServerPlayer p, String bossId) {
        ServerLevel world = p.serverLevel();
        BossLairs.Lair lair = BossLairs.of(bossId);
        BossDef def = ContentRegistry.boss(bossId);
        ArenaLayout.Arena a = ArenaLayout.of(bossId);
        if (lair == null || def == null || a == null || !a.underground()) {
            throw fail(bossId + " has no underground lair");
        }
        int r = a.radius();
        String way;
        if (lair.hasHut()) {
            int[] e = lair.entranceBlock();
            com.runecraft.world.StructurePlacer.applyAround(world, e[0], e[2], 12);
            BlockPos at = new BlockPos(e[0], e[1], e[2]);
            BlockState state = world.getBlockState(at);
            if (!BossLairs.ENTRANCE.equals(id(state))) {
                throw fail("no entrance in the hut of " + lair.note() + " (" + id(state) + ")");
            }
            int[] yard = lair.exitTo();
            p.teleportTo(world, yard[0] + 0.5, yard[1], yard[2] + 0.5, 180f, 0f);
            com.runecraft.content.BlockHooks.use(p, BossLairs.ENTRANCE, at, state);
            way = "in by the hut";
        } else {
            // Mokhaiotl: the door in the Ruins of Mokhaiotl's lobby (the dungeons module's), beside where the exit leads
            int[] to = lair.exitTo();
            com.runecraft.world.StructurePlacer.applyAround(world, to[0], to[2], 12);
            BlockPos door = null;
            for (BlockPos q : BlockPos.betweenClosed(to[0] - 3, to[1] - 1, to[2] - 4, to[0] + 3, to[1] + 1, to[2] + 4)) {
                if (id(world.getBlockState(q)).endsWith("mokhaiotl_entrance")) {
                    door = q.immutable();
                    break;
                }
            }
            if (door == null) {
                throw fail("no entrance in the Ruins of Mokhaiotl beside " + to[0] + "," + to[1] + "," + to[2]);
            }
            p.teleportTo(world, to[0] + 0.5, to[1], to[2] + 0.5, 180f, 0f);
            BlockState state = world.getBlockState(door);
            com.runecraft.content.BlockHooks.use(p, id(state), door, state);
            way = "in through the ruins";
        }
        BlockPos feet = p.blockPosition();
        if (BossLairs.at(feet.getX(), feet.getY(), feet.getZ(), b -> r) != lair || feet.getY() != lair.floorY() + 1) {
            throw fail("the way in left the player at " + feet.toShortString() + ", not on the floor of " + lair.note());
        }
        if (!world.getBlockState(feet.below()).blocksMotion() || !world.getBlockState(feet).isAir()
                || !world.getBlockState(feet.above()).isAir()) {
            throw fail("no room to stand where " + lair.note() + " is entered (" + feet.toShortString() + ")");
        }
        // the summoning stone by the arena's north opening
        int[] s = lair.stoneBlock(r);
        BlockPos stone = new BlockPos(s[0], s[1], s[2]);
        if (world.getBlockState(stone).getBlock() != com.runecraft.registry.ModBlocks.DATA_BOSS_STONES.get(bossId)) {
            throw fail("no summoning stone in " + lair.note() + " (" + id(world.getBlockState(stone)) + ")");
        }
        if (def.key != null) {
            Inv.give(p, def.key, 1);
        }
        p.teleportTo(world, s[0] + 0.5, s[1], s[2] + 1.5, 180f, 0f);
        AABB cave = new AABB(a.x() - lair.half(r), lair.floorY() - 1, a.z() - lair.half(r),
                a.x() + lair.half(r) + 1, lair.floorY() + BossLairs.HEADROOM + 2, a.z() + lair.half(r) + 1);
        String how = "summoned at the stone";
        try {
            DataBossService.summon(p, world, stone, def);
            List<DataBossEntity> found = world.getEntitiesOfClass(DataBossEntity.class, cave,
                    b -> b.isAlive() && b.def() != null && b.def().id.equals(bossId));
            if (found.isEmpty()) {
                // a requirement the test player lacks (a quest): raised where the stone would have put it
                how = "raised on the arena floor";
                DataBossEntity raised = DataBossService.spawn(world, new BlockPos(a.x(), lair.floorY() + 1, a.z()), def, p, r + 8);
                found = raised == null ? List.of() : List.of(raised);
            }
            if (found.isEmpty()) {
                throw fail(def.name + " didn't rise in " + lair.note());
            }
            DataBossEntity boss = found.get(0);
            BlockPos bp = boss.blockPosition();
            if (bp.getY() != lair.floorY() + 1 || Math.hypot(bp.getX() - a.x(), bp.getZ() - a.z()) > 2.5
                    || !world.getBlockState(bp.below()).blocksMotion() || !world.noCollision(boss)) {
                throw fail(def.name + " rose at " + bp.toShortString() + ", not on the arena floor at " + a.x() + "," + (lair.floorY() + 1)
                        + "," + a.z());
            }
        } finally {
            for (Mob m : world.getEntitiesOfClass(Mob.class, cave.inflate(4), Mob::isAlive)) {
                m.discard(); // the boss and any companion it raised (Eldric beside Branda)
            }
            if (def.key != null) {
                Inv.remove(p, def.key, 1);
            }
        }
        // out by the exit portal behind the arrival spot
        int[] x = lair.exitBlock(r);
        BlockPos exit = new BlockPos(x[0], x[1], x[2]);
        BlockState exitState = world.getBlockState(exit);
        if (!BossLairs.EXIT.equals(id(exitState))) {
            throw fail("no exit portal in " + lair.note() + " (" + id(exitState) + ")");
        }
        p.teleportTo(world, x[0] + 0.5, x[1], x[2] - 0.5, 0f, 0f);
        com.runecraft.content.BlockHooks.use(p, BossLairs.EXIT, exit, exitState);
        int[] out = lair.exitTo();
        if (p.blockPosition().distManhattan(new BlockPos(out[0], out[1], out[2])) > 1) {
            throw fail("the exit portal of " + lair.note() + " left the player at " + p.blockPosition().toShortString());
        }
        return lair.note() + ": " + way + ", " + def.name + " " + how + " (floor y " + (lair.floorY() + 1) + "), out by the exit";
    }
}
