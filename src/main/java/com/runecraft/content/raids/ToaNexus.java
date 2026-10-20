package com.runecraft.content.raids;

import com.runecraft.osrs.content.raids.RaidLayouts;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Tombs of Amascut's Nexus (OSRS): four path entrances, one per avatar of Tumeken, the Helpful Spirit (after the
 * second and fourth path) and, once every path is walked, the way down to the Wardens. The party leader enters a path
 * first and the others follow; the other entrances stay blocked until that path's boss is dead. The Nexus is a safe area
 * with no challenge of its own, so it counts as cleared as soon as the party walks in.
 */
final class ToaNexus extends RoomLogic {
    /** Entrance spots (dx, dz from the room centre) in {@link RaidLayouts#TOA_PATHS} order, then the Wardens. */
    static final int[][] ENTRANCES = {{-8, -20}, {8, -20}, {-8, 20}, {8, 20}, {20, 0}};
    private static final String[] NAMES = {"Scabaras", "Het", "Apmeken", "Crondis", "the Wardens"};
    /** Each path's colours: frame and beacon. */
    private static final BlockState[][] COLOURS = {
            {Blocks.LAPIS_BLOCK.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState()},
            {Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.GLOWSTONE.defaultBlockState()},
            {Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.VERDANT_FROGLIGHT.defaultBlockState()},
            {Blocks.PRISMARINE_BRICKS.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState()},
            {Blocks.CRYING_OBSIDIAN.defaultBlockState(), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState()}};
    private final Map<BlockPos, Integer> entrances = new HashMap<>();

    ToaNexus(RaidInstance raid, RoomDef def, int index) {
        super(raid, def, index);
    }

    @Override
    public boolean exitsEast() {
        return false;
    }

    @Override
    public void decorate() {
        entrances.clear();
        for (int i = 0; i < ENTRANCES.length; i++) {
            int dx = ENTRANCES[i][0];
            int dz = ENTRANCES[i][1];
            BlockPos at = at(dx, dz);
            entrances.put(at.immutable(), i);
            boolean ns = Math.abs(dz) == RaidBuilder.HALF;
            // A frame of the path's colours around the entrance, a beacon over it and a sign naming the path.
            for (int s = -2; s <= 2; s++) {
                for (int up = 0; up <= 4; up++) {
                    BlockPos f = ns ? at.offset(s, up, 0) : at.offset(0, up, s);
                    if (Math.abs(s) == 2 || up == 4) {
                        RaidBuilder.set(world(), f, COLOURS[i][0]);
                    } else if (s != 0 || up > 1) {
                        RaidBuilder.set(world(), f, Blocks.AIR.defaultBlockState());
                    }
                }
            }
            RaidBuilder.set(world(), at, RaidBuilder.module(RaidsContent.TOA_PATH_ENTRANCE));
            RaidBuilder.set(world(), at.above(), Blocks.AIR.defaultBlockState());
            RaidBuilder.set(world(), at.above(3), COLOURS[i][1]);
            Direction facing = ns ? (dz < 0 ? Direction.SOUTH : Direction.NORTH) : Direction.WEST;
            BlockPos sign = ns ? at.offset(1, 1, dz < 0 ? 1 : -1) : at.offset(-1, 1, 1);
            sign(sign, facing, i == 4 ? "To the" : "Path of", i == 4 ? "Wardens" : NAMES[i]);
        }
        RaidBuilder.set(world(), at(-12, 0), RaidBuilder.module(RaidsContent.TOA_HELPFUL_SPIRIT));
        RaidBuilder.set(world(), at(-12, 0).above(), Blocks.SOUL_LANTERN.defaultBlockState());
        // OSRS: a deposit pot beside the Helpful Spirit works as a bank deposit box.
        RaidBuilder.set(world(), at(-12, 3), RaidBuilder.module(RaidsContent.SUPPLIES));
        RaidBuilder.pillar(world(), center().offset(0, 0, -6), 0, 6, Blocks.CHISELED_SANDSTONE.defaultBlockState());
        RaidBuilder.pillar(world(), center().offset(0, 0, 6), 0, 6, Blocks.CHISELED_SANDSTONE.defaultBlockState());
        refresh();
    }

    private void sign(BlockPos pos, Direction facing, String line1, String line2) {
        RaidBuilder.set(world(), pos, Blocks.SANDSTONE.defaultBlockState());
        BlockPos front = pos.relative(facing);
        world().setBlock(front, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), 3);
        if (world().getBlockEntity(front) instanceof SignBlockEntity s) {
            s.setText(new SignText().setMessage(1, Component.literal(line1)).setMessage(2, Component.literal(line2)), true);
            s.setChanged();
        }
    }

    /** Lights each open entrance's beacon; a walked path goes dark, and the Wardens' way lights up once all four are done. */
    void refresh() {
        for (int i = 0; i < ENTRANCES.length; i++) {
            BlockPos at = at(ENTRANCES[i][0], ENTRANCES[i][1]);
            boolean open = i < 4 ? !raid.pathDone(i) : raid.pathsCompleted() >= RaidLayouts.TOA_PATHS.size();
            RaidBuilder.set(world(), at.above(3), open ? COLOURS[i][1] : Blocks.COAL_BLOCK.defaultBlockState());
        }
    }

    /** Which entrance stands at {@code pos}: a path (0-3), the Wardens (4), or -1. */
    int entranceAt(BlockPos pos) {
        Integer i = entrances.get(pos.immutable());
        return i == null ? -1 : i;
    }

    /** The position of an entrance (0-3 paths, 4 the Wardens). */
    BlockPos entrance(int i) {
        return at(ENTRANCES[i][0], ENTRANCES[i][1]);
    }

    @Override
    public void start() {
        complete();
    }

    @Override
    public void gameTick(long tick) {
    }

    @Override
    String selfTestSolve(RaidSelfTest t) {
        return "the Nexus";
    }

    @Override
    public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
        return false;
    }
}
