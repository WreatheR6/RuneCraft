package com.runecraft.content.raids;

import com.runecraft.combat.PoisonService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.osrs.content.raids.ToaLoot;
import com.runecraft.osrs.content.raids.ToaPuzzles;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Tombs of Amascut's four challenge rooms (OSRS wiki, "Tombs of Amascut/Strategies"), each entered by teleport from
 * the Nexus and opening onto its path's boss:
 * <ul>
 *   <li>Path of Scabaras: three of the five puzzles - the tile sequence or the lights, the matching symbols, then the
 *   addition tablet - while scarabs pester the party;</li>
 *   <li>Path of Het: turn the mirrors (and break the worn barriers) so the caster statue's beam strikes the shielded
 *   statue, then mine Het's seal while it is weakened, dodging the orbs of darkness;</li>
 *   <li>Path of Apmeken: eight baboon waves, and the problems Apmeken's sight warns of (roof supports, vents);</li>
 *   <li>Path of Crondis: carry water from the waterfalls past their traps to grow the Palm of Resourcefulness, while
 *   crocodiles go for the water and the palm.</li>
 * </ul>
 * The rules and numbers live in {@link ToaPuzzles}.
 */
final class ToaPuzzleRooms {
    private ToaPuzzleRooms() {
    }

    /** Neutralising potions (Path of Apmeken) are poured by using them. */
    static void init() {
        com.runecraft.platform.UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !(player instanceof ServerPlayer sp) || !"neutralising_potion".equals(Inv.idOf(stack))) {
                return net.minecraft.world.InteractionResultHolder.pass(stack);
            }
            RaidInstance raid = RaidManager.of(sp);
            int room = raid == null ? -1 : raid.roomAt(sp.position());
            if (room >= 0 && raid.logic(room) instanceof Apmeken a && a.started && !a.done) {
                a.pour(sp);
            } else {
                Msg.game(sp, "There is nothing here to neutralise.");
            }
            return net.minecraft.world.InteractionResultHolder.success(stack);
        });
    }

    /** Base of the challenge rooms: entered by teleport from the Nexus, so their west wall stays shut. */
    abstract static class Challenge extends RoomLogic {
        Challenge(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public boolean entersFromWest() {
            return false;
        }

        /** Room points to everyone in the room. */
        void pointsToAll(int amount) {
            for (ServerPlayer p : players()) {
                raid.awardActionPoints(p, amount);
            }
        }

        /** Typeless damage from a room mechanic. */
        int hurt(ServerPlayer p, int damage) {
            return RaidEffects.hit(null, p, damage, null, true);
        }

        long now() {
            return RaidEffects.now();
        }

        BlockPos floor(int dx, int dz) {
            return center().offset(dx, 0, dz);
        }
    }

    // ================================================================== Path of Scabaras

    /** Path of Scabaras: three puzzles in turn; the room's exit opens once all are solved. */
    static final class Scabaras extends Challenge {
        enum Puzzle { SEQUENCE, LIGHTS, MATCHING, ADDITION }

        private static final BlockState[] SYMBOLS = {Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.ORANGE_CONCRETE.defaultBlockState(),
                Blocks.MAGENTA_CONCRETE.defaultBlockState(), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                Blocks.YELLOW_CONCRETE.defaultBlockState(), Blocks.LIME_CONCRETE.defaultBlockState(), Blocks.PINK_CONCRETE.defaultBlockState(),
                Blocks.CYAN_CONCRETE.defaultBlockState(), Blocks.PURPLE_CONCRETE.defaultBlockState()};
        private static final String[] SYMBOL_NAMES = {"white", "orange", "magenta", "light blue", "yellow", "lime", "pink", "cyan",
                "purple"};
        /** The addition tiles' values by colour (the legend carved on the walls). */
        private static final BlockState[] VALUE_COLOURS = {Blocks.RED_TERRACOTTA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
                Blocks.YELLOW_TERRACOTTA.defaultBlockState(), Blocks.LIME_TERRACOTTA.defaultBlockState(), Blocks.GREEN_TERRACOTTA.defaultBlockState(),
                Blocks.CYAN_TERRACOTTA.defaultBlockState(), Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState(),
                Blocks.BLUE_TERRACOTTA.defaultBlockState(), Blocks.PURPLE_TERRACOTTA.defaultBlockState()};
        private static final String[] VALUE_NAMES = {"red", "orange", "yellow", "lime", "green", "cyan", "light blue", "blue", "purple"};
        static final int[] SEQUENCE_AT = {-11, -9};
        static final int[] LIGHTS_AT = {-11, 9};
        static final int[] MATCH_A = {4, -9};
        static final int[] MATCH_B = {12, -9};
        static final int[] ADDITION_AT = {8, 9};

        final List<Puzzle> puzzles = new ArrayList<>();
        int stage;
        private final Map<UUID, Long> standing = new HashMap<>();
        // Sequence
        List<Integer> sequence = new ArrayList<>();
        int input = -1;
        private int showIndex = -1;
        BlockPos button;
        // Lights
        int lights;
        // Matching
        int[][] symbols;
        final boolean[] matched = new boolean[9];
        private int pendingBoard = -1;
        private int pendingTile = -1;
        // Addition
        int[] values;
        int target = -1;
        private int sum;
        private int used;
        BlockPos tablet;

        Scabaras(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        /** Floor position of tile {@code i} (row by row) of the 3x3 board centred on {@code area}. */
        BlockPos tile(int[] area, int i) {
            return floor(area[0] + (i % 3 - 1) * 2, area[1] + (i / 3 - 1) * 2);
        }

        private void board(int[] area, BlockState tile) {
            RaidBuilder.fill(world(), center().getX() + area[0] - 3, center().getY(), center().getZ() + area[1] - 3,
                    center().getX() + area[0] + 3, center().getY(), center().getZ() + area[1] + 3, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.fill(world(), center().getX() + area[0] - 2, center().getY(), center().getZ() + area[1] - 2,
                    center().getX() + area[0] + 2, center().getY(), center().getZ() + area[1] + 2, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
            for (int i = 0; i < 9; i++) {
                RaidBuilder.set(world(), tile(area, i), tile);
            }
        }

        @Override
        public void decorate() {
            puzzles.clear();
            puzzles.add(rnd().nextBoolean() ? Puzzle.SEQUENCE : Puzzle.LIGHTS);
            puzzles.add(Puzzle.MATCHING);
            puzzles.add(Puzzle.ADDITION);
            BlockState plate = RaidBuilder.module(RaidsContent.SCABARAS_TILE);
            if (puzzles.contains(Puzzle.SEQUENCE)) {
                board(SEQUENCE_AT, plate);
                button = at(SEQUENCE_AT[0], SEQUENCE_AT[1] - 5);
                RaidBuilder.set(world(), button, RaidBuilder.module(RaidsContent.TOA_SCABARAS_PANEL));
            } else {
                board(LIGHTS_AT, plate);
            }
            board(MATCH_A, plate);
            board(MATCH_B, plate);
            board(ADDITION_AT, plate);
            tablet = at(ADDITION_AT[0], ADDITION_AT[1] + 5);
            RaidBuilder.set(world(), tablet, RaidBuilder.module(RaidsContent.TOA_SCABARAS_PANEL));
            RaidBuilder.pillar(world(), center().offset(16, 0, -14), 1, 5, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(16, 0, 14), 1, 5, Blocks.CHISELED_SANDSTONE.defaultBlockState());
        }

        @Override
        public void start() {
            stage = 0;
            standing.clear();
            input = -1;
            showIndex = -1;
            java.util.Arrays.fill(matched, false);
            pendingBoard = -1;
            target = -1;
            sum = 0;
            used = 0;
            lights = ToaPuzzles.lightsStart(rnd());
            symbols = ToaPuzzles.matchingBoards(rnd());
            values = ToaPuzzles.additionValues(rnd());
            BlockState plate = RaidBuilder.module(RaidsContent.SCABARAS_TILE);
            for (int i = 0; i < 9; i++) {
                RaidBuilder.set(world(), tile(MATCH_A, i), plate);
                RaidBuilder.set(world(), tile(MATCH_B, i), plate);
                RaidBuilder.set(world(), tile(ADDITION_AT, i), VALUE_COLOURS[values[i] - 1]);
            }
            // OSRS: in solo raids four of the nine symbol sets start matched.
            List<Integer> syms = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
            java.util.Collections.shuffle(syms, rnd());
            for (int k = 0; k < ToaPuzzles.matchingPresolved(raid.partySize()); k++) {
                matched[syms.get(k)] = true;
            }
            renderMatching();
            renderLights();
            announce();
        }

        private void announce() {
            if (stage >= puzzles.size()) {
                return;
            }
            say(switch (puzzles.get(stage)) {
                case SEQUENCE -> "Scabaras' first puzzle: press the button, watch the tiles light up, then step on them in that order.";
                case LIGHTS -> "Scabaras' first puzzle: light every plate. Stepping on one flips it and its neighbours.";
                case MATCHING -> "Scabaras' next puzzle: step on matching symbols, one on each board.";
                case ADDITION -> "Scabaras' last puzzle: read the tablet, then step on tiles adding up to its number.";
            });
        }

        private void renderLights() {
            if (!puzzles.contains(Puzzle.LIGHTS)) {
                return;
            }
            for (int i = 0; i < 9; i++) {
                boolean lit = (lights & (1 << i)) != 0;
                RaidBuilder.set(world(), tile(LIGHTS_AT, i), lit ? Blocks.OCHRE_FROGLIGHT.defaultBlockState()
                        : RaidBuilder.module(RaidsContent.SCABARAS_TILE));
            }
        }

        private void renderMatching() {
            for (int b = 0; b < 2; b++) {
                int[] area = b == 0 ? MATCH_A : MATCH_B;
                for (int i = 0; i < 9; i++) {
                    boolean shown = matched[symbols[b][i]] || (pendingBoard == b && pendingTile == i);
                    RaidBuilder.set(world(), tile(area, i), shown ? SYMBOLS[symbols[b][i]] : RaidBuilder.module(RaidsContent.SCABARAS_TILE));
                }
            }
        }

        private Puzzle current() {
            return stage < puzzles.size() ? puzzles.get(stage) : null;
        }

        @Override
        public void gameTick(long tick) {
            Puzzle p = current();
            if (p == Puzzle.SEQUENCE && showIndex >= 0) {
                showSequence();
            }
            for (ServerPlayer pl : players()) {
                BlockPos under = pl.blockPosition().below();
                long key = stepKey(under);
                Long before = standing.put(pl.getUUID(), key);
                if (key >= 0 && (before == null || before != key)) {
                    step(pl, key);
                }
            }
            // OSRS: scarabs pester the party from the first rooms. // approx: one, plus one a player, at a time
            if (tick % 20 == 0 && living("toa_path_scarab").size() < 1 + raid.partySize()) {
                spawnMob("toa_path_scarab", at(-17, rnd().nextBoolean() ? -6 : 6), false);
            }
            keepAggressive("toa_path_scarab");
        }

        /** Which puzzle tile a floor block is (board * 9 + tile), or -1. */
        private long stepKey(BlockPos floor) {
            Puzzle p = current();
            if (p == null) {
                return -1;
            }
            int[][] boards = switch (p) {
                case SEQUENCE -> new int[][]{SEQUENCE_AT};
                case LIGHTS -> new int[][]{LIGHTS_AT};
                case MATCHING -> new int[][]{MATCH_A, MATCH_B};
                case ADDITION -> new int[][]{ADDITION_AT};
            };
            for (int b = 0; b < boards.length; b++) {
                for (int i = 0; i < 9; i++) {
                    if (tile(boards[b], i).equals(floor)) {
                        return b * 9L + i;
                    }
                }
            }
            return -1;
        }

        private void step(ServerPlayer p, long key) {
            Puzzle puzzle = current();
            int board = (int) (key / 9);
            int i = (int) (key % 9);
            switch (puzzle) {
                case SEQUENCE -> stepSequence(p, i);
                case LIGHTS -> {
                    lights = ToaPuzzles.lightsPress(lights, i);
                    renderLights();
                    world().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6f, 1.2f);
                    if (lights == ToaPuzzles.LIGHTS_SOLVED) {
                        solved("Every plate is lit!");
                    }
                }
                case MATCHING -> stepMatching(board, i);
                case ADDITION -> stepAddition(p, i);
            }
        }

        private void stepSequence(ServerPlayer p, int i) {
            if (showIndex >= 0) {
                return;
            }
            if (input < 0) {
                Msg.game(p, "Press the button to see the sequence first.");
                return;
            }
            BlockPos t = tile(SEQUENCE_AT, i);
            if (sequence.get(input) == i) {
                input++;
                world().sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0);
                if (input >= sequence.size()) {
                    solved("The tiles of Scabaras glow in unison!");
                }
            } else {
                warn("Wrong tile! Scarabs swarm out of the floor.");
                hurt(p, 5 + rnd().nextInt(8)); // approx
                input = -1;
            }
        }

        private void showSequence() {
            int step = showIndex / 2;
            if (showIndex % 2 == 0 && step < sequence.size()) {
                BlockPos t = tile(SEQUENCE_AT, sequence.get(step));
                RaidBuilder.set(world(), t, Blocks.GLOWSTONE.defaultBlockState());
                world().sendParticles(ParticleTypes.END_ROD, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.02);
                world().playSound(null, t, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1f, 0.6f + sequence.get(step) * 0.1f);
            } else if (step < sequence.size() + 1) {
                for (int i = 0; i < 9; i++) {
                    RaidBuilder.set(world(), tile(SEQUENCE_AT, i), RaidBuilder.module(RaidsContent.SCABARAS_TILE));
                }
            }
            showIndex++;
            if (showIndex >= sequence.size() * 2) {
                showIndex = -1;
                input = 0;
                say("Now step on the tiles in the same order.");
            }
        }

        private void stepMatching(int board, int i) {
            int sym = symbols[board][i];
            if (matched[sym]) {
                return;
            }
            if (pendingBoard < 0 || pendingBoard == board) {
                pendingBoard = board;
                pendingTile = i;
                renderMatching();
                return;
            }
            if (symbols[pendingBoard][pendingTile] == sym) {
                matched[sym] = true;
                pendingBoard = -1;
                renderMatching();
                say("The " + SYMBOL_NAMES[sym] + " symbols match.");
                boolean all = true;
                for (boolean m : matched) {
                    all &= m;
                }
                if (all) {
                    solved("Every symbol is matched!");
                }
            } else {
                // Show both for a moment, then hide them again.
                pendingBoard = -1;
                RaidBuilder.set(world(), tile(board == 0 ? MATCH_A : MATCH_B, i), SYMBOLS[sym]);
                RaidEffects.later(2, () -> {
                    if (!done && current() == Puzzle.MATCHING) {
                        renderMatching();
                    }
                });
                say("The symbols don't match.");
            }
        }

        private void stepAddition(ServerPlayer p, int i) {
            if (target < 0) {
                Msg.game(p, "Read the tablet first.");
                return;
            }
            if ((used & (1 << i)) != 0) {
                return;
            }
            used |= 1 << i;
            sum += values[i];
            BlockPos t = tile(ADDITION_AT, i);
            RaidBuilder.set(world(), t, Blocks.GLOWSTONE.defaultBlockState());
            if (sum == target) {
                solved("The tiles add up to " + target + "!");
            } else if (sum > target) {
                warn("You went over " + target + "! The floor lashes out.");
                hurt(p, 8 + rnd().nextInt(8)); // approx
                resetAddition();
            } else {
                Msg.action(p, "Total: " + sum + " / " + target);
            }
        }

        private void resetAddition() {
            sum = 0;
            used = 0;
            for (int i = 0; i < 9; i++) {
                RaidBuilder.set(world(), tile(ADDITION_AT, i), VALUE_COLOURS[values[i] - 1]);
            }
        }

        private void solved(String message) {
            say(message);
            pointsToAll(ToaLoot.SCABARAS_PUZZLE_POINTS);
            stage++;
            if (stage >= puzzles.size()) {
                say("The Path of Scabaras is open!");
                complete();
            } else {
                announce();
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.TOA_SCABARAS_PANEL)) {
                if (pos.equals(button) && current() == Puzzle.SEQUENCE) {
                    if (showIndex < 0) {
                        // OSRS: pressing the button again shows a new order.
                        sequence = ToaPuzzles.sequence(ToaPuzzles.SEQUENCE_LENGTH, rnd());
                        input = -1;
                        showIndex = 0;
                        say("Watch the tiles of Scabaras...");
                    }
                    return true;
                }
                if (pos.equals(tablet)) {
                    if (current() != Puzzle.ADDITION) {
                        Msg.game(player, "The tablet's symbols are dark. Solve the other puzzles first.");
                        return true;
                    }
                    if (target < 0) {
                        target = ToaPuzzles.additionTarget(rnd());
                        resetAddition();
                    }
                    StringBuilder legend = new StringBuilder();
                    for (int v = 0; v < 9; v++) {
                        legend.append(v == 0 ? "" : ", ").append(VALUE_NAMES[v]).append(" ").append(v + 1);
                    }
                    say("The tablet reads " + target + ". The symbols carved on the walls: " + legend + ".");
                    return true;
                }
                return true;
            }
            if (blockId.equals(RaidsContent.SCABARAS_TILE) && current() == Puzzle.LIGHTS) {
                // OSRS: a single plate can be flipped by hand, for 20 unavoidable damage.
                for (int i = 0; i < 9; i++) {
                    if (tile(LIGHTS_AT, i).equals(pos)) {
                        hurt(player, 20);
                        lights ^= 1 << i;
                        renderLights();
                        if (lights == ToaPuzzles.LIGHTS_SOLVED) {
                            solved("Every plate is lit!");
                        }
                        return true;
                    }
                }
            }
            return blockId.equals(RaidsContent.SCABARAS_TILE);
        }

        @Override
        public void cleanup() {
            super.cleanup();
            living("toa_path_scarab").forEach(LivingEntity::discard);
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            BlockPos neutral = at(0, 0);
            List<String> solvedPuzzles = new ArrayList<>();
            int guard = 0;
            while (!done && guard++ < 8) {
                Puzzle p = current();
                switch (p) {
                    case SEQUENCE -> {
                        t.use(button);
                        t.tick(sequence.size() * 2 + 2);
                        RaidSelfTest.expect(input == 0, "Scabaras: the sequence was not shown");
                        for (int i : new ArrayList<>(sequence)) {
                            t.stepOn(tile(SEQUENCE_AT, i));
                            t.moveTo(neutral);
                            t.tick(1);
                        }
                    }
                    case LIGHTS -> {
                        List<Integer> presses = ToaPuzzles.lightsSolution(lights);
                        RaidSelfTest.expect(presses != null, "Scabaras: the lights board can't be solved");
                        for (int i : presses) {
                            t.stepOn(tile(LIGHTS_AT, i));
                            t.moveTo(neutral);
                            t.tick(1);
                        }
                    }
                    case MATCHING -> {
                        for (int sym = 0; sym < 9; sym++) {
                            if (matched[sym]) {
                                continue;
                            }
                            for (int b = 0; b < 2; b++) {
                                for (int i = 0; i < 9; i++) {
                                    if (symbols[b][i] == sym) {
                                        t.stepOn(tile(b == 0 ? MATCH_A : MATCH_B, i));
                                        t.moveTo(neutral);
                                        t.tick(1);
                                    }
                                }
                            }
                            RaidSelfTest.expect(matched[sym], "Scabaras: stepping on both " + SYMBOL_NAMES[sym] + " symbols did not match them");
                        }
                    }
                    case ADDITION -> {
                        t.use(tablet);
                        RaidSelfTest.expect(target >= 20 && target <= 45, "Scabaras: the tablet gave no number");
                        List<Integer> tiles = ToaPuzzles.additionSolution(values, target);
                        RaidSelfTest.expect(tiles != null, "Scabaras: no tiles add up to " + target);
                        for (int i : tiles) {
                            t.stepOn(tile(ADDITION_AT, i));
                            t.moveTo(neutral);
                            t.tick(1);
                        }
                    }
                }
                RaidSelfTest.expect(current() != p, "Scabaras: the " + p + " puzzle was not solved");
                solvedPuzzles.add(p.name().toLowerCase(java.util.Locale.ROOT));
            }
            return "Scabaras: " + String.join(", ", solvedPuzzles);
        }
    }

    // ================================================================== Path of Het

    /**
     * Path of Het: the caster statue fires a beam every few seconds; the mirrors (turned by hand) and the worn barriers
     * (broken with a pickaxe) must lead it to the shielded statue, which weakens Het's seal for 15 ticks. Orbs of darkness
     * cross the room.
     */
    static final class Het extends Challenge {
        static final int HALF = 7;
        static final int[] SEAL_AT = {15, 0};
        ToaPuzzles.HetPuzzle puzzle;
        final Set<Long> broken = new HashSet<>();
        int sealHp;
        int sealMax;
        long weakenedUntil = -1;
        private long nextBeam;
        /** Orbs of darkness: x, z (block, relative to the centre), dx, dz. */
        private final List<double[]> orbs = new ArrayList<>();

        Het(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        BlockPos cell(int x, int z) {
            return at(x, z);
        }

        @Override
        public void decorate() {
            puzzle = ToaPuzzles.hetPuzzle(HALF, rnd());
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() - HALF - 1, c.getY(), c.getZ() - HALF - 1, c.getX() + HALF + 1, c.getY(),
                    c.getZ() + HALF + 1, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.fill(world(), c.getX() - HALF, c.getY(), c.getZ() - HALF, c.getX() + HALF, c.getY(), c.getZ() + HALF,
                    Blocks.CUT_SANDSTONE.defaultBlockState());
            placePuzzle();
            RaidBuilder.set(world(), at(SEAL_AT[0], SEAL_AT[1]), RaidBuilder.module(RaidsContent.HET_SEAL));
            RaidBuilder.set(world(), at(SEAL_AT[0], SEAL_AT[1]).above(), Blocks.GOLD_BLOCK.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(SEAL_AT[0], 0, -3), 0, 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(SEAL_AT[0], 0, 3), 0, 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.set(world(), at(-18, 4), RaidBuilder.module(RaidsContent.TOA_PICKAXE_STATUE));
            RaidBuilder.set(world(), at(13, 5), RaidBuilder.module(RaidsContent.TOA_PICKAXE_STATUE));
        }

        private void placePuzzle() {
            // Caster statue west of the grid, in line with the beam's row.
            BlockPos caster = cell(-HALF - 2, puzzle.startZ());
            RaidBuilder.set(world(), caster, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.set(world(), caster.above(), Blocks.GLOWSTONE.defaultBlockState());
            BlockPos target = cell(puzzle.target()[0], puzzle.target()[1]);
            RaidBuilder.set(world(), target, Blocks.LAPIS_BLOCK.defaultBlockState());
            RaidBuilder.set(world(), target.above(), Blocks.SEA_LANTERN.defaultBlockState());
            for (int[] m : puzzle.mirrors()) {
                RaidBuilder.set(world(), cell(m[0], m[1]), RaidBuilder.module(RaidsContent.TOA_MIRROR));
            }
            for (int[] m : puzzle.decoys()) {
                RaidBuilder.set(world(), cell(m[0], m[1]), RaidBuilder.module(RaidsContent.TOA_MIRROR));
            }
            for (int[] b : puzzle.barriers()) {
                RaidBuilder.set(world(), cell(b[0], b[1]), RaidBuilder.module(RaidsContent.TOA_WORN_BARRIER));
            }
            for (int[] o : puzzle.obstacles()) {
                RaidBuilder.set(world(), cell(o[0], o[1]), Blocks.SANDSTONE_WALL.defaultBlockState());
            }
        }

        @Override
        public void start() {
            broken.clear();
            orbs.clear();
            sealMax = ToaPuzzles.sealHitpoints(raid.partySize());
            sealHp = sealMax;
            weakenedUntil = -1;
            nextBeam = now() + 6;
            for (int[] b : puzzle.barriers()) {
                RaidBuilder.set(world(), cell(b[0], b[1]), RaidBuilder.module(RaidsContent.TOA_WORN_BARRIER));
            }
            RaidBuilder.set(world(), at(SEAL_AT[0], SEAL_AT[1]), RaidBuilder.module(RaidsContent.HET_SEAL));
        }

        boolean weakened() {
            return now() < weakenedUntil;
        }

        @Override
        public void gameTick(long tick) {
            if (now() >= nextBeam) {
                fireBeam();
                nextBeam = now() + ToaPuzzles.BEAM_CYCLE_TICKS;
            }
            if (tick % 8 == 0) {
                showMirrors();
            }
            if (weakened()) {
                BlockPos s = at(SEAL_AT[0], SEAL_AT[1]);
                world().sendParticles(ParticleTypes.ENCHANTED_HIT, s.getX() + 0.5, s.getY() + 1, s.getZ() + 0.5, 6, 0.4, 0.6, 0.4, 0.05);
            }
            // OSRS: orbs of darkness cross the room in straight lines. // approx: one every 7 ticks
            if (tick % 7 == 0) {
                boolean alongX = rnd().nextBoolean();
                int lane = rnd().nextInt(2 * RaidBuilder.HALF - 3) - RaidBuilder.HALF + 2;
                int side = rnd().nextBoolean() ? 1 : -1;
                orbs.add(alongX ? new double[]{-side * RaidBuilder.HALF, lane, side, 0} : new double[]{lane, -side * RaidBuilder.HALF, 0, side});
            }
            moveOrbs();
        }

        /** Mirror orientations as a short diagonal of particles ('/' or '\' seen from above). */
        private void showMirrors() {
            for (List<int[]> list : List.of(puzzle.mirrors(), puzzle.decoys())) {
                for (int[] m : list) {
                    BlockPos p = cell(m[0], m[1]);
                    for (double s = -0.4; s <= 0.41; s += 0.2) {
                        double z = m[3] == ToaPuzzles.SLASH ? -s : s;
                        world().sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5 + s, p.getY() + 1.1, p.getZ() + 0.5 + z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        private void fireBeam() {
            ToaPuzzles.Beam beam = ToaPuzzles.trace(puzzle, broken);
            double y = center().getY() + 1.6;
            BlockPos caster = cell(-HALF - 2, puzzle.startZ());
            world().playSound(null, caster, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
            DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.3f), 1.2f);
            List<int[]> cells = new ArrayList<>();
            cells.add(new int[]{-HALF - 1, puzzle.startZ()});
            cells.addAll(beam.cells());
            for (int[] c : cells) {
                world().sendParticles(gold, center().getX() + 0.5 + c[0], y, center().getZ() + 0.5 + c[1], 4, 0.2, 0.05, 0.2, 0);
                for (ServerPlayer p : players()) {
                    if (p.blockPosition().getX() == center().getX() + c[0] && p.blockPosition().getZ() == center().getZ() + c[1]) {
                        Msg.red(p, "The beam of light burns you!");
                        hurt(p, 4 + rnd().nextInt(5)); // approx: "lower than the orbs"
                    }
                }
            }
            if (beam.hit()) {
                weakenedUntil = now() + ToaPuzzles.SEAL_WEAKENED_TICKS;
                say("The shielded statue absorbs the beam - Het's seal is weakened! Mine it now.");
            }
        }

        private void moveOrbs() {
            java.util.Iterator<double[]> it = orbs.iterator();
            while (it.hasNext()) {
                double[] o = it.next();
                o[0] += o[2];
                o[1] += o[3];
                if (Math.abs(o[0]) > RaidBuilder.HALF || Math.abs(o[1]) > RaidBuilder.HALF) {
                    it.remove();
                    continue;
                }
                Vec3 at = vec(o[0], o[1]);
                world().sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 0.8, at.z, 4, 0.15, 0.15, 0.15, 0);
                for (ServerPlayer p : players()) {
                    if (RaidEffects.horizontalDistSq(p.position(), at) < 0.8 * 0.8) {
                        Msg.red(p, "An orb of darkness slams into you!");
                        hurt(p, 8 + rnd().nextInt(8)); // approx: "moderate damage"
                        it.remove();
                        break;
                    }
                }
            }
        }

        /** The best pickaxe a player carries or wields, or null. */
        static String pickaxe(ServerPlayer p) {
            String best = null;
            int bestBonus = Integer.MIN_VALUE;
            List<String> ids = new ArrayList<>();
            ids.add(Inv.idOf(p.getMainHandItem()));
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ids.add(Inv.idOf(p.getInventory().getItem(i)));
            }
            for (String id : ids) {
                if (id != null && id.endsWith("_pickaxe") && ToaPuzzles.pickaxeBonus(id) > bestBonus) {
                    best = id;
                    bestBonus = ToaPuzzles.pickaxeBonus(id);
                }
            }
            return best;
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            switch (blockId) {
                case RaidsContent.TOA_PICKAXE_STATUE -> {
                    if (pickaxe(player) != null) {
                        Msg.game(player, "You already have a pickaxe.");
                    } else {
                        Inv.give(player, "bronze_pickaxe", 1);
                        Msg.game(player, "You take a bronze pickaxe from the statue.");
                    }
                    return true;
                }
                case RaidsContent.TOA_MIRROR -> {
                    int[] m = puzzle.mirrorAt(pos.getX() - center().getX(), pos.getZ() - center().getZ());
                    if (m != null && !cooldown(player, 1)) {
                        m[3] = m[3] == ToaPuzzles.SLASH ? ToaPuzzles.BACKSLASH : ToaPuzzles.SLASH;
                        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        world().playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
                        showMirrors();
                        Msg.action(player, "You turn the mirror: it now faces " + (m[3] == ToaPuzzles.SLASH ? "'/'" : "'\\'") + ".");
                    }
                    return true;
                }
                case RaidsContent.TOA_WORN_BARRIER -> {
                    if (pickaxe(player) == null) {
                        Msg.game(player, "You need a pickaxe to break the barrier. The statues hold one.");
                        return true;
                    }
                    if (!cooldown(player, 2)) {
                        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        broken.add(key(pos));
                        RaidBuilder.set(world(), pos, Blocks.AIR.defaultBlockState());
                        world().sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
                        Msg.action(player, "You break the worn barrier.");
                    }
                    return true;
                }
                case RaidsContent.HET_SEAL -> {
                    mine(player, pos);
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

        private long key(BlockPos pos) {
            return ((long) (pos.getX() - center().getX()) << 32) ^ ((pos.getZ() - center().getZ()) & 0xffffffffL);
        }

        private void mine(ServerPlayer player, BlockPos pos) {
            String pick = pickaxe(player);
            if (pick == null) {
                Msg.game(player, "You need a pickaxe to mine the seal. The statues hold one.");
                return;
            }
            if (!weakened()) {
                Msg.game(player, "Het's power protects the seal. Lead the beam to the shielded statue.");
                return;
            }
            if (cooldown(player, 2)) {
                return;
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            int dmg = Math.min(sealHp, ToaPuzzles.sealDamage(level(player, Skill.MINING), pick, rnd()));
            sealHp -= dmg;
            XpService.add(player, Skill.MINING, ToaPuzzles.SEAL_MINING_XP);
            raid.awardActionPoints(player, (int) Math.round(dmg * ToaLoot.HET_SEAL_POINTS));
            world().sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.1);
            Msg.action(player, "You hit Het's seal for " + dmg + ". (" + Math.max(0, sealHp) + "/" + sealMax + ")");
            if (sealHp <= 0) {
                RaidBuilder.set(world(), pos, Blocks.AIR.defaultBlockState());
                RaidBuilder.set(world(), pos.above(), Blocks.AIR.defaultBlockState());
                say("Het's seal shatters! The Path of Het is open.");
                complete();
            }
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            ServerPlayer p = t.player();
            Inv.remove(p, "bronze_pickaxe", Inv.count(p, "bronze_pickaxe"));
            BlockPos statue = at(-18, 4);
            t.use(statue);
            RaidSelfTest.expect(pickaxe(p) != null, "Het: the statue gave no pickaxe");
            for (int[] m : puzzle.mirrors()) {
                int turns = 0;
                while (m[3] != m[2] && turns++ < 3) {
                    t.tick(1);
                    t.use(cell(m[0], m[1]));
                }
                RaidSelfTest.expect(m[3] == m[2], "Het: a mirror would not turn");
            }
            for (int[] b : puzzle.barriers()) {
                t.tick(2);
                t.use(cell(b[0], b[1]));
            }
            RaidSelfTest.expect(ToaPuzzles.trace(puzzle, broken).hit(), "Het: the beam does not reach the shielded statue after the fix");
            BlockPos seal = at(SEAL_AT[0], SEAL_AT[1]);
            int guard = 0;
            int cycles = 0;
            while (!done && guard++ < 600) {
                if (weakened()) {
                    t.use(seal);
                    t.tick(2);
                } else {
                    t.tick(1);
                    if (weakened()) {
                        cycles++;
                    }
                }
            }
            RaidSelfTest.expect(done, "Het: the seal did not break (" + sealHp + "/" + sealMax + " left)");
            return "Het: beam via " + puzzle.mirrors().size() + " mirrors, seal broken in " + cycles + " cycles";
        }
    }

    // ================================================================== Path of Apmeken

    /**
     * Path of Apmeken: eight waves of baboons (the next wave comes once at most one of the last is left). Apmeken's sight
     * (the first player in) warns of problems: the roof supports need a hammer, the vents a neutralising potion; groups also
     * get corruption, which the sight holder cures by pouring a potion next to the corrupted.
     */
    static final class Apmeken extends Challenge {
        enum Problem { NONE, ROOF, VENT, CORRUPTION }

        static final String[] KINDS = {ToaPuzzles.BRAWLER, ToaPuzzles.THROWER, ToaPuzzles.MAGE, ToaPuzzles.SHAMAN, ToaPuzzles.THRALL,
                ToaPuzzles.VOLATILE, ToaPuzzles.CURSED};
        final List<BlockPos> supports = new ArrayList<>();
        final List<BlockPos> vents = new ArrayList<>();
        List<List<String>> waves = new ArrayList<>();
        int wave;
        final List<LivingEntity> baboons = new ArrayList<>();
        UUID sight;
        Problem problem = Problem.NONE;
        int fixes;
        private final Set<BlockPos> fixedThisTime = new HashSet<>();
        private final Set<UUID> corrupted = new HashSet<>();
        private long deadline;
        private long nextProblem;
        private final Map<LivingEntity, UUID> volatileTarget = new HashMap<>();
        private final Map<LivingEntity, Integer> fuse = new HashMap<>();
        /** Venom pools: x, z, expiry tick. */
        private final List<double[]> pools = new ArrayList<>();

        Apmeken(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            supports.clear();
            vents.clear();
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    BlockPos s = at(sx * 9, sz * 14);
                    RaidBuilder.set(world(), s, RaidBuilder.module(RaidsContent.APMEKEN_PILLAR));
                    RaidBuilder.fill(world(), s.getX(), s.getY() + 1, s.getZ(), s.getX(), s.getY() + 8, s.getZ(),
                            Blocks.STRIPPED_JUNGLE_LOG.defaultBlockState());
                    supports.add(s);
                    BlockPos v = at(sx * 16, sz * 16);
                    RaidBuilder.set(world(), v, RaidBuilder.module(RaidsContent.APMEKEN_VENT));
                    vents.add(v);
                }
            }
            RaidBuilder.set(world(), at(-17, -3), RaidBuilder.module(RaidsContent.TOA_APMEKEN_CRATE));
            RaidBuilder.set(world(), at(-17, 3), RaidBuilder.module(RaidsContent.TOA_APMEKEN_CRATE));
        }

        @Override
        public void start() {
            waves = ToaPuzzles.apmekenWaves(raid.partySize(), rnd());
            wave = -1;
            baboons.clear();
            volatileTarget.clear();
            fuse.clear();
            pools.clear();
            corrupted.clear();
            problem = Problem.NONE;
            ServerPlayer first = randomPlayer();
            sight = first == null ? null : first.getUUID();
            if (first != null) {
                Msg.game(first, "You are granted Apmeken's sight: you will see what needs fixing.");
            }
            nextProblem = now() + 16;
            nextWave();
        }

        private void nextWave() {
            wave++;
            if (wave >= waves.size()) {
                return;
            }
            say("Wave " + (wave + 1) + "/" + waves.size() + ": baboons pour into the room!");
            List<String> list = waves.get(wave);
            for (int i = 0; i < list.size(); i++) {
                String id = list.get(i);
                BlockPos at = at(8 + rnd().nextInt(8), (i % 2 == 0 ? -1 : 1) * (6 + rnd().nextInt(8)));
                LivingEntity b = spawnMob(id, at, false);
                if (b == null) {
                    continue;
                }
                baboons.add(b);
                if (id.equals(ToaPuzzles.VOLATILE)) {
                    // OSRS: a volatile baboon picks a random raider to chase.
                    ServerPlayer target = randomPlayer();
                    if (target != null) {
                        volatileTarget.put(b, target.getUUID());
                    }
                }
                if (id.equals(ToaPuzzles.CURSED) && b instanceof Mob m) {
                    m.setTarget(null);
                }
            }
        }

        private int aliveInWave() {
            int n = 0;
            for (LivingEntity b : baboons) {
                if (alive(b) && !RaidInstance.creatureId(b).equals(ToaPuzzles.THRALL)) {
                    n++;
                }
            }
            return n;
        }

        @Override
        public void gameTick(long tick) {
            baboons.removeIf(b -> !alive(b));
            keepAggressive(ToaPuzzles.BRAWLER, ToaPuzzles.THROWER, ToaPuzzles.MAGE, ToaPuzzles.SHAMAN, ToaPuzzles.THRALL);
            int alive = aliveInWave();
            if (wave < waves.size() - 1 && alive <= 1) {
                nextWave();
            } else if (wave >= waves.size() - 1 && alive == 0 && baboons.isEmpty()) {
                say("The last of Ba-Ba's troupe falls. The Path of Apmeken is open!");
                complete();
                return;
            }
            volatiles();
            cursed(tick);
            shamans(tick);
            problems();
        }

        /** Volatile baboons chase their raider and explode next to them, killing any baboon caught in the blast. */
        private void volatiles() {
            for (LivingEntity b : new ArrayList<>(living(ToaPuzzles.VOLATILE))) {
                UUID u = volatileTarget.get(b);
                ServerPlayer target = u == null ? null : world().getServer().getPlayerList().getPlayer(u);
                if (target == null || !inside(target.position()) || raid.isDowned(target)) {
                    target = randomPlayer();
                    if (target == null) {
                        continue;
                    }
                    volatileTarget.put(b, target.getUUID());
                }
                if (b instanceof Mob m && (m.tickCount % 10 == 0 || m.getNavigation().isDone())) {
                    m.getNavigation().moveTo(target, 1.2);
                }
                if (b.distanceToSqr(target) < 1.6 * 1.6) {
                    int f = fuse.merge(b, 1, Integer::sum);
                    world().sendParticles(ParticleTypes.SMOKE, b.getX(), b.getY() + 1, b.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
                    if (f >= 2) {
                        explode(b);
                    }
                }
            }
        }

        private void explode(LivingEntity b) {
            Vec3 at = b.position();
            world().sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.5, 0.5, 0.5, 0);
            world().playSound(null, b.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.2f);
            int dmg = 20 + 3 * raid.hitpoints(b); // approx: "based on its health", heavy
            for (ServerPlayer p : players()) {
                if (RaidEffects.horizontalDistSq(p.position(), at) <= 1.6 * 1.6) {
                    RaidEffects.hit(b, p, dmg, null, true);
                }
            }
            for (LivingEntity other : new ArrayList<>(baboons)) {
                if (other != b && alive(other) && RaidEffects.horizontalDistSq(other.position(), at) <= 1.6 * 1.6) {
                    other.kill();
                }
            }
            fuse.remove(b);
            volatileTarget.remove(b);
            b.discard();
        }

        /** Cursed baboons wander, leaving venom pools (6-8 damage a tick and venom). */
        private void cursed(long tick) {
            for (LivingEntity b : living(ToaPuzzles.CURSED)) {
                if (b instanceof Mob m) {
                    m.setTarget(null);
                    if (m.getNavigation().isDone() || m.tickCount % 40 == 0) {
                        m.getNavigation().moveTo(center().getX() + rnd().nextInt(30) - 15, center().getY() + 1,
                                center().getZ() + rnd().nextInt(30) - 15, 0.8);
                    }
                }
                if (tick % 3 == 0) {
                    pools.add(new double[]{b.getX(), b.getZ(), now() + 33});
                }
            }
            pools.removeIf(pool -> pool[2] < now());
            for (double[] pool : pools) {
                if (tick % 2 == 0) {
                    world().sendParticles(new DustParticleOptions(new Vector3f(0.2f, 0.6f, 0.1f), 1.0f), pool[0], center().getY() + 1.05,
                            pool[1], 2, 0.3, 0.02, 0.3, 0);
                }
            }
            for (ServerPlayer p : players()) {
                for (double[] pool : pools) {
                    if ((p.getX() - pool[0]) * (p.getX() - pool[0]) + (p.getZ() - pool[1]) * (p.getZ() - pool[1]) < 0.7 * 0.7) {
                        hurt(p, 6 + rnd().nextInt(3));
                        if (!PoisonService.isVenomImmune(p, com.runecraft.action.ActionManager.gameTick())) {
                            PoisonService.venom(p, com.runecraft.action.ActionManager.gameTick());
                        }
                        break;
                    }
                }
            }
        }

        /** Shamans summon thralls up to their limit, unless poisoned (OSRS). */
        private void shamans(long tick) {
            if (tick % 8 != 0) {
                return;
            }
            int thralls = living(ToaPuzzles.THRALL).size();
            for (LivingEntity s : living(ToaPuzzles.SHAMAN)) {
                if (thralls >= ToaPuzzles.SHAMAN_THRALLS * living(ToaPuzzles.SHAMAN).size() || PoisonService.isPoisoned(s)
                        || PoisonService.isEnvenomed(s)) {
                    continue;
                }
                LivingEntity t = spawnMob(ToaPuzzles.THRALL, s.blockPosition().offset(rnd().nextInt(3) - 1, 0, rnd().nextInt(3) - 1), false);
                if (t != null) {
                    baboons.add(t);
                    thralls++;
                }
            }
        }

        private void problems() {
            long now = now();
            if (problem == Problem.NONE) {
                if (now >= nextProblem) {
                    newProblem();
                }
                return;
            }
            ServerPlayer seer = sight == null ? null : world().getServer().getPlayerList().getPlayer(sight);
            if (seer != null) {
                List<BlockPos> show = problem == Problem.ROOF ? supports : problem == Problem.VENT ? vents : List.of();
                for (BlockPos b : show) {
                    if (!fixedThisTime.contains(b)) {
                        world().sendParticles(seer, ParticleTypes.LARGE_SMOKE, true, b.getX() + 0.5, b.getY() + 1.5, b.getZ() + 0.5, 3, 0.3,
                                0.4, 0.3, 0.01);
                    }
                }
            }
            if (now > deadline) {
                int missed = Math.max(0, needed() - fixes);
                if (problem == Problem.CORRUPTION) {
                    missed = corrupted.size();
                }
                if (missed > 0) {
                    warn("The problem was not fixed in time - the room lashes out!");
                    for (ServerPlayer p : players()) {
                        hurt(p, (8 + rnd().nextInt(8)) * missed); // approx: scaled by the actions missed
                    }
                }
                endProblem();
            }
        }

        private int needed() {
            return com.runecraft.osrs.content.raids.ToaPuzzles.apmekenFixesNeeded(players().size());
        }

        private void newProblem() {
            List<ServerPlayer> here = players();
            if (here.isEmpty()) {
                return;
            }
            // OSRS: in groups the sight passes between players at random.
            if (here.size() > 1 && rnd().nextInt(3) == 0) {
                sight = here.get(rnd().nextInt(here.size())).getUUID();
            }
            List<Problem> kinds = new ArrayList<>(List.of(Problem.ROOF, Problem.VENT));
            if (here.size() > 1) {
                kinds.add(Problem.CORRUPTION);
            }
            problem = kinds.get(rnd().nextInt(kinds.size()));
            fixes = 0;
            fixedThisTime.clear();
            corrupted.clear();
            deadline = now() + ToaPuzzles.APMEKEN_FIX_TICKS;
            for (ServerPlayer p : here) {
                Msg.red(p, "You sense an issue somewhere in the room.");
            }
            ServerPlayer seer = world().getServer().getPlayerList().getPlayer(sight);
            if (seer == null || !here.contains(seer)) {
                seer = here.get(0);
                sight = seer.getUUID();
            }
            if (problem == Problem.CORRUPTION) {
                for (ServerPlayer p : here) {
                    if (p != seer) {
                        corrupted.add(p.getUUID());
                    }
                }
            }
            Msg.red(seer, "Apmeken's sight: " + switch (problem) {
                case ROOF -> "the roof supports are giving way - repair " + needed() + " with a hammer!";
                case VENT -> "the vents are leaking poison - pour neutralising potion into " + needed() + "!";
                case CORRUPTION -> "your allies are corrupted - gather them and pour a neutralising potion!";
                case NONE -> "";
            });
        }

        private void endProblem() {
            problem = Problem.NONE;
            fixedThisTime.clear();
            corrupted.clear();
            nextProblem = now() + ToaPuzzles.APMEKEN_PROBLEM_EVERY;
        }

        private void fixed(ServerPlayer player) {
            fixes++;
            if (fixes >= needed()) {
                say(player.getName().getString() + " has fixed the problem.");
                pointsToAll(ToaLoot.APMEKEN_FIX_POINTS);
                endProblem();
            }
        }

        /** A neutralising potion poured (used): purges a leaking vent underfoot, or cures corruption next to the seer. */
        void pour(ServerPlayer player) {
            if (problem == Problem.CORRUPTION && player.getUUID().equals(sight)) {
                int cured = 0;
                for (ServerPlayer p : players()) {
                    if (corrupted.contains(p.getUUID()) && p.distanceToSqr(player) < 1.6 * 1.6) {
                        corrupted.remove(p.getUUID());
                        cured++;
                    }
                }
                if (cured > 0) {
                    Inv.remove(player, "neutralising_potion", 1);
                    say(player.getName().getString() + " purges the corruption.");
                    if (corrupted.isEmpty()) {
                        pointsToAll(ToaLoot.APMEKEN_FIX_POINTS);
                        endProblem();
                    }
                    return;
                }
            }
            for (BlockPos v : vents) {
                if (player.blockPosition().distSqr(v) <= 4) {
                    useVent(player, v);
                    return;
                }
            }
            Msg.game(player, "There is nothing here to neutralise.");
        }

        private void useVent(ServerPlayer player, BlockPos v) {
            if (problem != Problem.VENT || fixedThisTime.contains(v)) {
                Msg.red(player, "The vent was fine - the poison you stir up burns you.");
                hurt(player, 5 + rnd().nextInt(6)); // approx
                return;
            }
            if (!Inv.has(player, "neutralising_potion", 1)) {
                Msg.game(player, "You need a neutralising potion. The crates hold them.");
                return;
            }
            Inv.remove(player, "neutralising_potion", 1);
            fixedThisTime.add(v);
            world().sendParticles(ParticleTypes.SPLASH, v.getX() + 0.5, v.getY() + 1, v.getZ() + 0.5, 20, 0.4, 0.3, 0.4, 0);
            Msg.action(player, "You pour neutralising potion into the vent.");
            fixed(player);
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            switch (blockId) {
                case RaidsContent.TOA_APMEKEN_CRATE -> {
                    if (!hasTool(player, "hammer")) {
                        Inv.give(player, "hammer", 1);
                    }
                    int have = Inv.count(player, "neutralising_potion");
                    if (have < 100) {
                        Inv.give(player, "neutralising_potion", 100 - have);
                    }
                    Msg.game(player, "You take a hammer and neutralising potions from the crate.");
                    return true;
                }
                case RaidsContent.APMEKEN_PILLAR -> {
                    if (problem != Problem.ROOF || fixedThisTime.contains(pos)) {
                        Msg.red(player, "The support was sound - knocking at it brings dust down on you.");
                        hurt(player, 5 + rnd().nextInt(6)); // approx
                        return true;
                    }
                    if (!hasTool(player, "hammer")) {
                        Msg.game(player, "You need a hammer. The crates hold them.");
                        return true;
                    }
                    player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    fixedThisTime.add(pos);
                    XpService.add(player, Skill.CONSTRUCTION, 30); // approx
                    world().playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
                    Msg.action(player, "You repair the roof support.");
                    fixed(player);
                    return true;
                }
                case RaidsContent.APMEKEN_VENT -> {
                    useVent(player, pos);
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            for (String k : KINDS) {
                living(k).forEach(LivingEntity::discard);
            }
            pools.clear();
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            ServerPlayer p = t.player();
            t.use(at(-17, -3));
            RaidSelfTest.expect(hasTool(p, "hammer") && Inv.has(p, "neutralising_potion", 1), "Apmeken: the crate gave no hammer or potions");
            int problemsFixed = 0;
            int guard = 0;
            int maxWave = 0;
            while (!done && guard++ < 3000) {
                maxWave = Math.max(maxWave, wave + 1);
                if (problem == Problem.ROOF || problem == Problem.VENT) {
                    List<BlockPos> list = problem == Problem.ROOF ? supports : vents;
                    for (BlockPos b : list) {
                        if (problem == Problem.NONE) {
                            break;
                        }
                        if (!fixedThisTime.contains(b)) {
                            if (problem == Problem.VENT) {
                                t.moveTo(b.above());
                            }
                            t.use(b);
                        }
                    }
                    problemsFixed++;
                    RaidSelfTest.expect(problem == Problem.NONE, "Apmeken: fixing the " + list.size() + " fixtures did not clear the problem");
                }
                LivingEntity target = null;
                for (LivingEntity b : baboons) {
                    if (alive(b)) {
                        target = b;
                        break;
                    }
                }
                if (target != null) {
                    t.hit(target, 40, AttackType.RANGED);
                }
                t.tick(1);
            }
            RaidSelfTest.expect(done, "Apmeken: the waves were not cleared (wave " + (wave + 1) + ", " + aliveInWave() + " alive)");
            return "Apmeken: " + maxWave + " waves, " + problemsFixed + " problems fixed";
        }
    }

    // ================================================================== Path of Crondis

    /**
     * Path of Crondis: fill a water container at the waterfalls (each needs time to refill) and water the Palm of
     * Resourcefulness; the statues' spikes and poison clouds near the waterfalls spill half your water (and drain
     * Defence, Agility and Prayer), and crocodiles go for raiders carrying water, then the palm.
     */
    static final class Crondis extends Challenge {
        static final int[][] FALLS = {{-17, -16}, {17, -16}, {-17, 16}, {17, 16}};
        final List<BlockPos> waterfalls = new ArrayList<>();
        private final Map<BlockPos, Long> fallReady = new HashMap<>();
        final Map<UUID, Integer> water = new HashMap<>();
        private final Map<UUID, List<Long>> trapHits = new HashMap<>();
        private final Map<LivingEntity, Long> crocBite = new HashMap<>();
        int growth;
        int needed;
        BlockPos palm;

        Crondis(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            waterfalls.clear();
            for (int[] f : FALLS) {
                BlockPos w = at(f[0], f[1]);
                RaidBuilder.set(world(), w, RaidBuilder.module(RaidsContent.CRONDIS_WATER));
                // A curtain of falling water behind it (ice blocks, so nothing floods).
                int back = f[0] < 0 ? -1 : 1;
                RaidBuilder.fill(world(), w.getX() + back, w.getY(), w.getZ() - 1, w.getX() + back, w.getY() + 6, w.getZ() + 1,
                        Blocks.BLUE_ICE.defaultBlockState());
                // Guardian statues either side of the approach.
                RaidBuilder.set(world(), w.offset(-back * 2, 0, 2), Blocks.CHISELED_SANDSTONE.defaultBlockState());
                RaidBuilder.set(world(), w.offset(-back * 2, 0, -2), Blocks.CHISELED_SANDSTONE.defaultBlockState());
                waterfalls.add(w);
            }
            palm = at(0, 0);
            RaidBuilder.set(world(), palm, RaidBuilder.module(RaidsContent.CRONDIS_PALM));
            RaidBuilder.fill(world(), palm.getX(), palm.getY() + 1, palm.getZ(), palm.getX(), palm.getY() + 2, palm.getZ(),
                    Blocks.JUNGLE_LOG.defaultBlockState());
        }

        @Override
        public void start() {
            needed = ToaPuzzles.palmWaterNeeded(raid.partySize());
            growth = 0;
            water.clear();
            trapHits.clear();
            crocBite.clear();
            fallReady.clear();
            grow();
        }

        /** The palm's canopy grows with its water. */
        private void grow() {
            int stage = Math.min(3, growth * 4 / Math.max(1, needed));
            RaidBuilder.fill(world(), palm.getX() - 2, palm.getY() + 3, palm.getZ() - 2, palm.getX() + 2, palm.getY() + 6, palm.getZ() + 2,
                    Blocks.AIR.defaultBlockState());
            RaidBuilder.fill(world(), palm.getX(), palm.getY() + 1, palm.getZ(), palm.getX(), palm.getY() + 2 + stage, palm.getZ(),
                    Blocks.JUNGLE_LOG.defaultBlockState());
            int r = Math.min(2, stage);
            if (stage > 0) {
                RaidBuilder.fill(world(), palm.getX() - r, palm.getY() + 3 + stage, palm.getZ() - r, palm.getX() + r, palm.getY() + 3 + stage,
                        palm.getZ() + r, Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
            }
        }

        int water(ServerPlayer p) {
            return water.getOrDefault(p.getUUID(), 0);
        }

        @Override
        public void gameTick(long tick) {
            traps(tick);
            crocodiles(tick);
            for (BlockPos w : waterfalls) {
                if (fallReady.getOrDefault(w, 0L) <= now() && tick % 4 == 0) {
                    world().sendParticles(ParticleTypes.FALLING_WATER, w.getX() + 0.5, w.getY() + 3, w.getZ() + 0.5, 8, 0.4, 1.5, 0.4, 0);
                }
            }
        }

        /**
         * Each waterfall's approach is guarded: the side statues' spikes sweep the lane and the front statue's poison cloud
         * rolls down it, telegraphed for two ticks. // approx: timings and areas
         */
        private void traps(long tick) {
            for (int i = 0; i < waterfalls.size(); i++) {
                BlockPos w = waterfalls.get(i);
                int back = FALLS[i][0] < 0 ? -1 : 1;
                if ((tick + i * 3) % 12 == 0) {
                    // Spikes across the lane two blocks in front of the waterfall.
                    List<Vec3> spots = new ArrayList<>();
                    for (int dz = -2; dz <= 2; dz++) {
                        spots.add(Vec3.atBottomCenterOf(w.offset(-back * 2, 0, dz)));
                    }
                    trap(spots, ParticleTypes.CRIT, "The statue's spikes catch you!");
                } else if ((tick + i * 3) % 12 == 6) {
                    // A poison cloud rolling out of the waterfall.
                    List<Vec3> spots = new ArrayList<>();
                    for (int dx = 1; dx <= 5; dx++) {
                        spots.add(Vec3.atBottomCenterOf(w.offset(-back * dx, 0, 0)));
                    }
                    trap(spots, ParticleTypes.ITEM_SLIME, "The poison cloud chokes you!");
                }
            }
        }

        private void trap(List<Vec3> spots, net.minecraft.core.particles.ParticleOptions warn, String message) {
            for (Vec3 s : spots) {
                world().sendParticles(warn, s.x, s.y + 0.2, s.z, 4, 0.3, 0.05, 0.3, 0);
            }
            RaidEffects.later(2, () -> {
                if (done) {
                    return;
                }
                for (ServerPlayer p : players()) {
                    for (Vec3 s : spots) {
                        if (RaidEffects.horizontalDistSq(p.position(), s) < 0.7 * 0.7) {
                            Msg.red(p, message);
                            trapHit(p);
                            break;
                        }
                    }
                }
            });
        }

        /** An obstacle hit: half the water spills, Defence -6, Agility -3, Prayer -2 (OSRS). */
        void trapHit(ServerPlayer p) {
            hurt(p, 3 + rnd().nextInt(4)); // approx
            int w = water(p);
            if (w > 0) {
                water.put(p.getUUID(), ToaPuzzles.spillFromTrap(w, rnd()));
                Msg.action(p, "Water spills from your container. (" + water(p) + "%)");
            }
            PlayerData data = Players.data(p);
            data.skills().setBoost(Skill.DEFENCE, data.skills().boost(Skill.DEFENCE) - 6);
            data.skills().setBoost(Skill.AGILITY, data.skills().boost(Skill.AGILITY) - 3);
            data.setPrayerPoints(Math.max(0, data.prayerPoints() - 2));
            com.runecraft.network.ServerNetworking.sync(p);
            trapHits.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(now());
        }

        private int recentHits(ServerPlayer p) {
            List<Long> hits = trapHits.get(p.getUUID());
            if (hits == null) {
                return 0;
            }
            hits.removeIf(t -> now() - t > 50);
            return hits.size();
        }

        /**
         * Crocodiles (OSRS): from the corners, at most eight; they go for a raider carrying water, then the palm once it has
         * grown, then anyone who attacked them. A bite does 18 (+3 for each obstacle that hit the raider in the last 30
         * seconds, at most 36), a third through Protect from Melee for 12 prayer, and spills water.
         */
        private void crocodiles(long tick) {
            List<LivingEntity> crocs = living("toa_crocodile");
            int wantMax = Math.min(ToaPuzzles.MAX_CROCODILES, 2 + raid.partySize());
            // OSRS: the first crocodiles reach the palm about 40 seconds in. // approx: a pair every 25 ticks from tick 30
            if (raid.roomTicks() >= 30 && tick % 25 == 0 && crocs.size() < wantMax) {
                for (int i = 0; i < 2 && crocs.size() + i < wantMax; i++) {
                    int[] corner = FALLS[rnd().nextInt(FALLS.length)];
                    spawnMob("toa_crocodile", at(corner[0] > 0 ? 14 : -14, corner[1] > 0 ? 18 : -18), false);
                }
            }
            for (LivingEntity croc : crocs) {
                if (!(croc instanceof Mob m)) {
                    continue;
                }
                m.setTarget(null);
                ServerPlayer target = null;
                double best = 12 * 12;
                for (ServerPlayer p : players()) {
                    double d = p.distanceToSqr(croc);
                    if (water(p) > 0 && d < best) {
                        best = d;
                        target = p;
                    }
                }
                Vec3 goal = target != null ? target.position() : growth > 0 ? Vec3.atCenterOf(palm) : null;
                if (goal == null) {
                    for (ServerPlayer p : players()) {
                        if (croc.getLastHurtByMob() == p) {
                            goal = p.position();
                            target = p;
                        }
                    }
                }
                if (goal == null) {
                    continue;
                }
                if (m.tickCount % 10 == 0 || m.getNavigation().isDone()) {
                    m.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0);
                }
                long last = crocBite.getOrDefault(croc, -100L);
                if (now() - last < 7) {
                    continue;
                }
                if (target != null && croc.distanceToSqr(target) < 2.0 * 2.0) {
                    crocBite.put(croc, now());
                    bite(croc, target);
                } else if (target == null && croc.distanceToSqr(Vec3.atCenterOf(palm)) < 2.6 * 2.6) {
                    crocBite.put(croc, now());
                    growth = Math.max(0, growth - 10); // approx: a bite's worth of growth
                    world().sendParticles(ParticleTypes.DAMAGE_INDICATOR, palm.getX() + 0.5, palm.getY() + 1.5, palm.getZ() + 0.5, 6, 0.4, 0.4,
                            0.4, 0.1);
                    say("A crocodile tears at the palm! (" + growth + "/" + needed + ")");
                    grow();
                }
            }
        }

        private void bite(LivingEntity croc, ServerPlayer p) {
            int dmg = ToaPuzzles.crocodileDamage(recentHits(p));
            if (RaidEffects.protectedFrom(p, AttackType.CRUSH)) {
                dmg /= 3;
                PlayerData data = Players.data(p);
                data.setPrayerPoints(Math.max(0, data.prayerPoints() - 12));
            }
            RaidEffects.hit(croc, p, dmg, null, true);
            int w = water(p);
            if (w > 0) {
                water.put(p.getUUID(), ToaPuzzles.spillFromBite(w));
                Msg.red(p, "The crocodile's bite spills your water! (" + water(p) + "%)");
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.CRONDIS_WATER)) {
                if (!Inv.has(player, "crondis_water_container", 1)) {
                    Inv.give(player, "crondis_water_container", 1);
                    Msg.game(player, "You pick up a water container.");
                }
                long ready = fallReady.getOrDefault(pos, 0L);
                if (ready > now()) {
                    Msg.game(player, "The waterfall is still replenishing.");
                    return true;
                }
                if (water(player) >= ToaPuzzles.CONTAINER_WATER) {
                    Msg.game(player, "Your container is already full.");
                    return true;
                }
                water.put(player.getUUID(), ToaPuzzles.CONTAINER_WATER);
                // OSRS: a waterfall needs time to recover; larger groups get it back sooner. // approx
                fallReady.put(pos, now() + Math.max(6, 16 - 2 * raid.partySize()));
                world().playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8f, 1f);
                Msg.game(player, "You fill the container with water. (100%)");
                return true;
            }
            if (blockId.equals(RaidsContent.CRONDIS_PALM)) {
                int w = water(player);
                if (w <= 0) {
                    Msg.game(player, "The palm needs water. (" + growth + "/" + needed + ")");
                    return true;
                }
                water.put(player.getUUID(), 0);
                growth += w;
                raid.awardActionPoints(player, w * ToaLoot.CRONDIS_POINTS_PER_GROWTH);
                world().sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0);
                say(player.getName().getString() + " waters the palm. (" + Math.min(growth, needed) + "/" + needed + ")");
                grow();
                if (growth >= needed) {
                    say("The Palm of Resourcefulness flourishes! The Path of Crondis is open.");
                    complete();
                }
                return true;
            }
            return false;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            living("toa_crocodile").forEach(LivingEntity::discard);
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            ServerPlayer p = t.player();
            int trips = 0;
            int guard = 0;
            while (!done && guard++ < 400) {
                BlockPos fall = null;
                for (BlockPos w : waterfalls) {
                    if (fallReady.getOrDefault(w, 0L) <= now()) {
                        fall = w;
                        break;
                    }
                }
                if (fall == null) {
                    t.tick(1);
                    continue;
                }
                t.moveTo(fall.offset(FALLS[waterfalls.indexOf(fall)][0] < 0 ? 1 : -1, 0, 0));
                t.use(fall);
                RaidSelfTest.expect(water(p) == ToaPuzzles.CONTAINER_WATER, "Crondis: the waterfall did not fill the container");
                // Carrying it through a trap spills half.
                if (trips == 0) {
                    trapHit(p);
                    RaidSelfTest.expect(water(p) < ToaPuzzles.CONTAINER_WATER, "Crondis: a trap did not spill the water");
                }
                t.moveTo(palm.offset(1, 0, 0));
                t.use(palm);
                trips++;
                t.tick(2);
            }
            RaidSelfTest.expect(done, "Crondis: the palm did not grow (" + growth + "/" + needed + ")");
            return "Crondis: palm grown in " + trips + " trips";
        }
    }
}
