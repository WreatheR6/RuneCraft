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
 *   <li>Path of Scabaras: two branches of two puzzle rooms each (the tile sequence, the obelisks, the lights and the
 *   matching symbols), then the addition tablet where they meet - a group clears both branches, a solo raider two rooms -
 *   while scarabs pester the party;</li>
 *   <li>Path of Het: place and turn the movable mirrors, clean the dirty ones and break the worn barriers so the caster
 *   statue's beam strikes the shielded statue, then mine Het's seal while it is weakened, dodging the orbs of darkness;
 *   the statues keep a deposited pickaxe between raids;</li>
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

    /**
     * Path of Scabaras (OSRS wiki): five puzzles between two branched paths. The north and south branches each hold two
     * puzzle rooms (the tile sequence, the obelisks, the lights and the matching symbols, shuffled between the four); the
     * branches meet at the addition tablet by the exit. A group clears both branches; solo, clearing a branch's first room
     * opens the other branch's second room, so a solo raider solves two of the four (and should avoid the obelisks). Scarabs
     * pester the party from the start of the first rooms.
     */
    static final class Scabaras extends Challenge {
        enum Puzzle { SEQUENCE, OBELISKS, LIGHTS, MATCHING, ADDITION }

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
        private static final String[] ROOM_NAMES = {"north branch's first room", "north branch's second room", "south branch's first room",
                "south branch's second room"};
        /** The four puzzle rooms' centres (north first, north second, south first, south second). */
        static final int[][] ROOMS = {{-10, -10}, {3, -10}, {-10, 10}, {3, 10}};
        /** The walls: the branches split at x = -15 and meet at x = 9; each branch's rooms part at x = -3. */
        static final int SPLIT_X = -15;
        static final int MIDDLE_X = -3;
        static final int MEET_X = 9;
        static final int[] ADDITION_AT = {14, 0};
        static final int[] TABLET_AT = {14, 5};
        /** The obelisks around their room's centre (z mirrored for the south branch): an arc away from the divider. */
        static final int[][] OBELISK_SPOTS = {{-4, -3}, {-2, -5}, {0, -6}, {2, -5}, {4, -3}};
        /** Damage a wrong tile, an overshoot or a falling rock does. // approx: the wiki only says they hurt */
        static final int PUZZLE_DAMAGE_MIN = 5;
        static final int PUZZLE_DAMAGE_SPREAD = 8;

        List<ToaPuzzles.ScabarasPuzzle> layout = new ArrayList<>();
        final boolean[] solved = new boolean[4];
        boolean additionSolved;
        /** Whether anyone took damage from a puzzle (Perfect Scabaras). */
        boolean puzzleDamage;
        private long startedAt;
        private final Map<UUID, Long> standing = new HashMap<>();
        // Sequence
        List<Integer> sequence = new ArrayList<>();
        int input = -1;
        private int showIndex = -1;
        // Lights
        int lights;
        // Matching
        int[][] symbols;
        final boolean[] matched = new boolean[9];
        private int pendingBoard = -1;
        private int pendingTile = -1;
        // Obelisks
        List<Integer> obeliskOrder = new ArrayList<>();
        int obelisksLit;
        // Addition
        int[] values;
        int target = -1;
        private int sum;
        private int used;

        Scabaras(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        boolean solo() {
            return raid.partySize() <= 1;
        }

        /** The room (0-3) holding a puzzle. */
        int roomOf(ToaPuzzles.ScabarasPuzzle p) {
            return layout.indexOf(p);
        }

        int roomOf(Puzzle p) {
            return p == Puzzle.ADDITION ? -1 : roomOf(ToaPuzzles.ScabarasPuzzle.valueOf(p.name()));
        }

        boolean open(int room) {
            return ToaPuzzles.scabarasRoomOpen(room, solved, solo());
        }

        boolean additionOpen() {
            return ToaPuzzles.scabarasAdditionOpen(solved, solo());
        }

        /** Whether a puzzle can be worked on now: its room's door is open and it isn't solved yet. */
        boolean active(Puzzle p) {
            if (p == Puzzle.ADDITION) {
                return additionOpen() && !additionSolved;
            }
            int room = roomOf(p);
            return room >= 0 && open(room) && !solved[room];
        }

        /** The centre of a puzzle's board (the matching puzzle has two, {@code board} 0 or 1). */
        int[] boardAt(Puzzle p, int board) {
            if (p == Puzzle.ADDITION) {
                return ADDITION_AT;
            }
            int[] c = ROOMS[roomOf(p)];
            if (p == Puzzle.MATCHING) {
                return new int[]{c[0], c[1] + (board == 0 ? -4 : 4)};
            }
            return c;
        }

        /** Floor position of tile {@code i} (row by row) of the 3x3 board centred on {@code area}. */
        BlockPos tile(int[] area, int i) {
            return floor(area[0] + (i % 3 - 1) * 2, area[1] + (i / 3 - 1) * 2);
        }

        BlockPos tile(Puzzle p, int board, int i) {
            return tile(boardAt(p, board), i);
        }

        /** The sequence puzzle's button: between its board and the divider. */
        BlockPos button() {
            int[] c = ROOMS[roomOf(Puzzle.SEQUENCE)];
            return at(c[0], c[1] + (c[1] < 0 ? 5 : -5));
        }

        BlockPos tablet() {
            return at(TABLET_AT[0], TABLET_AT[1]);
        }

        /** Standing position of obelisk {@code i}. */
        BlockPos obelisk(int i) {
            int[] c = ROOMS[roomOf(Puzzle.OBELISKS)];
            int sign = c[1] < 0 ? 1 : -1;
            return at(c[0] + OBELISK_SPOTS[i][0], c[1] + sign * OBELISK_SPOTS[i][1]);
        }

        /** The door cell into a second room or the tablet's chamber ({@code which}: 1 north second, 3 south second, 4/5 the
         * tablet's north and south doors). */
        BlockPos door(int which) {
            return switch (which) {
                case 1 -> at(MIDDLE_X, ROOMS[1][1]);
                case 3 -> at(MIDDLE_X, ROOMS[3][1]);
                case 4 -> at(MEET_X, ROOMS[1][1]);
                default -> at(MEET_X, ROOMS[3][1]);
            };
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

        private void wall(int x1, int z1, int x2, int z2) {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + x1, c.getY() + 1, c.getZ() + z1, c.getX() + x2, c.getY() + 3, c.getZ() + z2,
                    Blocks.SANDSTONE.defaultBlockState());
        }

        private void setDoor(int which, boolean open) {
            BlockPos d = door(which);
            BlockState s = open ? Blocks.AIR.defaultBlockState() : Blocks.CHISELED_SANDSTONE.defaultBlockState();
            RaidBuilder.set(world(), d, s);
            RaidBuilder.set(world(), d.above(), s);
        }

        private void refreshDoors() {
            setDoor(1, open(ToaPuzzles.NORTH_SECOND));
            setDoor(3, open(ToaPuzzles.SOUTH_SECOND));
            setDoor(4, additionOpen());
            setDoor(5, additionOpen());
        }

        @Override
        public void decorate() {
            layout = ToaPuzzles.scabarasLayout(rnd());
            BlockState plate = RaidBuilder.module(RaidsContent.SCABARAS_TILE);
            // The branches: a divider down the middle, each branch split into two rooms, and the tablet's chamber.
            wall(SPLIT_X, 0, MEET_X, 0);
            wall(MIDDLE_X, -RaidBuilder.HALF, MIDDLE_X, -1);
            wall(MIDDLE_X, 1, MIDDLE_X, RaidBuilder.HALF);
            wall(MEET_X, -RaidBuilder.HALF, MEET_X, RaidBuilder.HALF);
            for (ToaPuzzles.ScabarasPuzzle p : layout) {
                Puzzle q = Puzzle.valueOf(p.name());
                switch (q) {
                    case MATCHING -> {
                        board(boardAt(q, 0), plate);
                        board(boardAt(q, 1), plate);
                    }
                    case OBELISKS -> {
                        for (int i = 0; i < ToaPuzzles.OBELISKS; i++) {
                            BlockPos o = obelisk(i);
                            RaidBuilder.set(world(), o, RaidBuilder.module(RaidsContent.TOA_SCABARAS_OBELISK));
                            RaidBuilder.set(world(), o.above(), Blocks.CHISELED_SANDSTONE.defaultBlockState());
                            RaidBuilder.set(world(), o.above(2), Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                        }
                    }
                    case SEQUENCE -> {
                        board(boardAt(q, 0), plate);
                        RaidBuilder.set(world(), button(), RaidBuilder.module(RaidsContent.TOA_SCABARAS_PANEL));
                    }
                    default -> board(boardAt(q, 0), plate);
                }
            }
            board(ADDITION_AT, plate);
            RaidBuilder.set(world(), tablet(), RaidBuilder.module(RaidsContent.TOA_SCABARAS_PANEL));
            RaidBuilder.pillar(world(), center().offset(16, 0, -14), 1, 5, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(16, 0, 14), 1, 5, Blocks.CHISELED_SANDSTONE.defaultBlockState());
        }

        @Override
        public void start() {
            java.util.Arrays.fill(solved, false);
            additionSolved = false;
            puzzleDamage = false;
            startedAt = now();
            standing.clear();
            input = -1;
            showIndex = -1;
            java.util.Arrays.fill(matched, false);
            pendingBoard = -1;
            target = -1;
            sum = 0;
            used = 0;
            obelisksLit = 0;
            obeliskOrder = ToaPuzzles.obeliskOrder(rnd());
            lights = ToaPuzzles.lightsStart(rnd());
            symbols = ToaPuzzles.matchingBoards(rnd());
            values = ToaPuzzles.additionValues(rnd());
            BlockState plate = RaidBuilder.module(RaidsContent.SCABARAS_TILE);
            for (int i = 0; i < 9; i++) {
                RaidBuilder.set(world(), tile(Puzzle.SEQUENCE, 0, i), plate);
                RaidBuilder.set(world(), tile(Puzzle.ADDITION, 0, i), VALUE_COLOURS[values[i] - 1]);
            }
            // OSRS: in solo raids four of the nine symbol sets start matched.
            List<Integer> syms = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
            java.util.Collections.shuffle(syms, rnd());
            for (int k = 0; k < ToaPuzzles.matchingPresolved(raid.partySize()); k++) {
                matched[syms.get(k)] = true;
            }
            renderMatching();
            renderLights();
            renderObelisks();
            refreshDoors();
            say("The Path of Scabaras splits in two. " + (solo()
                    ? "Solve a puzzle in either branch's first room: it opens the other branch's second room."
                    : "Clear both branches, then read the tablet where they meet."));
            for (int r = 0; r < 4; r++) {
                if (open(r)) {
                    announce(r);
                }
            }
        }

        private void announce(int room) {
            say("The " + ROOM_NAMES[room] + ": " + switch (layout.get(room)) {
                case SEQUENCE -> "press the button, watch the tiles light up, then step on them in that order.";
                case LIGHTS -> "light every plate. Stepping on one flips it and its neighbours.";
                case MATCHING -> "step on matching symbols, one on each board.";
                case OBELISKS -> "strike the obelisks in the right order. A wrong one brings the ceiling down.";
            });
        }

        private void renderLights() {
            for (int i = 0; i < 9; i++) {
                boolean lit = (lights & (1 << i)) != 0;
                RaidBuilder.set(world(), tile(Puzzle.LIGHTS, 0, i), lit ? Blocks.OCHRE_FROGLIGHT.defaultBlockState()
                        : RaidBuilder.module(RaidsContent.SCABARAS_TILE));
            }
        }

        private void renderMatching() {
            for (int b = 0; b < 2; b++) {
                for (int i = 0; i < 9; i++) {
                    boolean shown = matched[symbols[b][i]] || (pendingBoard == b && pendingTile == i);
                    RaidBuilder.set(world(), tile(Puzzle.MATCHING, b, i), shown ? SYMBOLS[symbols[b][i]] : RaidBuilder.module(RaidsContent.SCABARAS_TILE));
                }
            }
        }

        private void renderObelisks() {
            for (int i = 0; i < ToaPuzzles.OBELISKS; i++) {
                boolean lit = obeliskOrder.subList(0, obelisksLit).contains(i);
                RaidBuilder.set(world(), obelisk(i).above(2), lit ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState());
            }
        }

        @Override
        public void gameTick(long tick) {
            if (showIndex >= 0) {
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
            // OSRS: scarabs pester the party from the start of the first rooms. // approx: one, plus one a player, at a time
            if (tick % 20 == 0 && living("toa_path_scarab").size() < 1 + raid.partySize()) {
                spawnMob("toa_path_scarab", at(-17, rnd().nextBoolean() ? -6 : 6), false);
            }
            keepAggressive("toa_path_scarab");
        }

        /** Which active puzzle tile a floor block is (puzzle ordinal * 100 + board * 9 + tile), or -1. */
        private long stepKey(BlockPos floor) {
            for (Puzzle p : Puzzle.values()) {
                if (p == Puzzle.OBELISKS || !active(p)) {
                    continue;
                }
                int boards = p == Puzzle.MATCHING ? 2 : 1;
                for (int b = 0; b < boards; b++) {
                    for (int i = 0; i < 9; i++) {
                        if (tile(p, b, i).equals(floor)) {
                            return ToaPuzzles.scabarasStepKey(p.ordinal(), b, i);
                        }
                    }
                }
            }
            return -1;
        }

        private void step(ServerPlayer p, long key) {
            // The tile comes from the board part of the key: key % 9 would shift it by the puzzle's ordinal (100 % 9 != 0),
            // so the lights flipped, the matching symbols showed and the addition tiles counted the wrong tiles.
            Puzzle puzzle = Puzzle.values()[ToaPuzzles.scabarasStepPuzzle(key)];
            int board = ToaPuzzles.scabarasStepBoard(key);
            int i = ToaPuzzles.scabarasStepTile(key);
            switch (puzzle) {
                case SEQUENCE -> stepSequence(p, i);
                case LIGHTS -> {
                    lights = ToaPuzzles.lightsPress(lights, i);
                    renderLights();
                    world().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6f, 1.2f);
                    if (lights == ToaPuzzles.LIGHTS_SOLVED) {
                        solved(Puzzle.LIGHTS, "Every plate is lit!");
                    }
                }
                case MATCHING -> stepMatching(board, i);
                case ADDITION -> stepAddition(p, i);
                default -> {
                }
            }
        }

        /** Damage from a puzzle mistake (Perfect Scabaras fails). */
        private void puzzleHurt(ServerPlayer p, int damage) {
            puzzleDamage = true;
            hurt(p, damage);
        }

        private void stepSequence(ServerPlayer p, int i) {
            if (showIndex >= 0) {
                return;
            }
            if (input < 0) {
                Msg.game(p, "Press the button to see the sequence first.");
                return;
            }
            BlockPos t = tile(Puzzle.SEQUENCE, 0, i);
            if (sequence.get(input) == i) {
                input++;
                world().sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0);
                if (input >= sequence.size()) {
                    solved(Puzzle.SEQUENCE, "The tiles of Scabaras glow in unison!");
                }
            } else {
                warn("Wrong tile! Scarabs swarm out of the floor.");
                puzzleHurt(p, PUZZLE_DAMAGE_MIN + rnd().nextInt(PUZZLE_DAMAGE_SPREAD));
                input = -1;
            }
        }

        private void showSequence() {
            int step = showIndex / 2;
            if (showIndex % 2 == 0 && step < sequence.size()) {
                BlockPos t = tile(Puzzle.SEQUENCE, 0, sequence.get(step));
                RaidBuilder.set(world(), t, Blocks.GLOWSTONE.defaultBlockState());
                world().sendParticles(ParticleTypes.END_ROD, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.02);
                world().playSound(null, t, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1f, 0.6f + sequence.get(step) * 0.1f);
            } else if (step < sequence.size() + 1) {
                for (int i = 0; i < 9; i++) {
                    RaidBuilder.set(world(), tile(Puzzle.SEQUENCE, 0, i), RaidBuilder.module(RaidsContent.SCABARAS_TILE));
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
                    solved(Puzzle.MATCHING, "Every symbol is matched!");
                }
            } else {
                // Show both for a moment, then hide them again.
                pendingBoard = -1;
                RaidBuilder.set(world(), tile(Puzzle.MATCHING, board, i), SYMBOLS[sym]);
                later(2, () -> {
                    if (!done && active(Puzzle.MATCHING)) {
                        renderMatching();
                    }
                });
                say("The symbols don't match.");
            }
        }

        /**
         * Striking an obelisk (OSRS): the next in the order lights up; a wrong one stays dark, puts the lit ones out and rocks
         * fall from the ceiling around the striker (they can be dodged).
         */
        private void strikeObelisk(ServerPlayer p, int i) {
            int lit = ToaPuzzles.obeliskStrike(obeliskOrder, obelisksLit, i);
            if (lit < 0) {
                obelisksLit = 0;
                renderObelisks();
                warn("The obelisk stays dark - rocks fall from the ceiling!");
                List<Vec3> rocks = new ArrayList<>();
                for (ServerPlayer q : players()) {
                    if (q.distanceToSqr(Vec3.atCenterOf(obelisk(i))) < 12 * 12) {
                        for (int k = 0; k < 3; k++) {
                            rocks.add(k == 0 ? q.position() : q.position().add(rnd().nextInt(5) - 2, 0, rnd().nextInt(5) - 2));
                        }
                    }
                }
                for (Vec3 r : rocks) {
                    world().sendParticles(ParticleTypes.CLOUD, r.x, r.y + 0.15, r.z, 8, 0.4, 0.02, 0.4, 0);
                }
                // The rocks land a few ticks later: step out from under the shadows.
                later(3, () -> {
                    for (ServerPlayer q : players()) {
                        for (Vec3 r : rocks) {
                            if (RaidEffects.horizontalDistSq(q.position(), r) < 0.9 * 0.9) {
                                world().sendParticles(ParticleTypes.EXPLOSION, r.x, r.y + 0.5, r.z, 1, 0, 0, 0, 0);
                                Msg.red(q, "A falling rock hits you!");
                                puzzleHurt(q, PUZZLE_DAMAGE_MIN + rnd().nextInt(PUZZLE_DAMAGE_SPREAD));
                                break;
                            }
                        }
                    }
                });
                return;
            }
            if (lit > obelisksLit) {
                obelisksLit = lit;
                renderObelisks();
                world().playSound(null, obelisk(i), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8f, 0.8f + 0.2f * lit);
                if (obelisksLit >= ToaPuzzles.OBELISKS) {
                    solved(Puzzle.OBELISKS, "Every obelisk is lit!");
                }
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
            BlockPos t = tile(Puzzle.ADDITION, 0, i);
            RaidBuilder.set(world(), t, Blocks.GLOWSTONE.defaultBlockState());
            if (sum == target) {
                solved(Puzzle.ADDITION, "The tiles add up to " + target + "!");
            } else if (sum > target) {
                warn("You went over " + target + "! The floor lashes out.");
                puzzleHurt(p, PUZZLE_DAMAGE_MIN + 3 + rnd().nextInt(PUZZLE_DAMAGE_SPREAD));
                resetAddition();
            } else {
                Msg.action(p, "Total: " + sum + " / " + target);
            }
        }

        private void resetAddition() {
            sum = 0;
            used = 0;
            for (int i = 0; i < 9; i++) {
                RaidBuilder.set(world(), tile(Puzzle.ADDITION, 0, i), VALUE_COLOURS[values[i] - 1]);
            }
        }

        private void solved(Puzzle p, String message) {
            say(message);
            pointsToAll(ToaLoot.SCABARAS_PUZZLE_POINTS);
            if (p == Puzzle.ADDITION) {
                additionSolved = true;
                say("The Path of Scabaras is open!");
                // OSRS CA "Perfect Scabaras": under a minute without anyone taking damage from the puzzles.
                if (!puzzleDamage && now() - startedAt < ToaLoot.PERFECT_SCABARAS_TICKS) {
                    for (ServerPlayer pl : players()) {
                        com.runecraft.content.systems.CombatAchievementService.flag(pl, ToaLoot.FLAG_PERFECT_SCABARAS);
                    }
                }
                complete();
                return;
            }
            int room = roomOf(p);
            boolean[] before = solved.clone();
            solved[room] = true;
            refreshDoors();
            for (int r = 0; r < 4; r++) {
                if (!ToaPuzzles.scabarasRoomOpen(r, before, solo()) && open(r)) {
                    say("A door grinds open: the " + ROOM_NAMES[r] + ".");
                    announce(r);
                }
            }
            if (!ToaPuzzles.scabarasAdditionOpen(before, solo()) && additionOpen()) {
                say("The way to the tablet opens: read it, then step on tiles adding up to its number.");
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            if (blockId.equals(RaidsContent.TOA_SCABARAS_OBELISK)) {
                for (int i = 0; i < ToaPuzzles.OBELISKS; i++) {
                    if (obelisk(i).equals(pos)) {
                        if (!active(Puzzle.OBELISKS)) {
                            Msg.game(player, solved[roomOf(Puzzle.OBELISKS)] ? "The obelisks are already lit." : "The door to the obelisks is shut.");
                        } else if (!cooldown(player, 2)) {
                            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                            strikeObelisk(player, i);
                        }
                        return true;
                    }
                }
                return true;
            }
            if (blockId.equals(RaidsContent.TOA_SCABARAS_PANEL)) {
                if (layout.contains(ToaPuzzles.ScabarasPuzzle.SEQUENCE) && pos.equals(button()) && active(Puzzle.SEQUENCE)) {
                    if (showIndex < 0) {
                        // OSRS: pressing the button again shows a new order.
                        sequence = ToaPuzzles.sequence(ToaPuzzles.SEQUENCE_LENGTH, rnd());
                        input = -1;
                        showIndex = 0;
                        say("Watch the tiles of Scabaras...");
                    }
                    return true;
                }
                if (pos.equals(tablet())) {
                    if (!additionOpen()) {
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
            if (blockId.equals(RaidsContent.SCABARAS_TILE) && active(Puzzle.LIGHTS)) {
                // OSRS: a single plate can be flipped by hand, for 20 unavoidable damage.
                for (int i = 0; i < 9; i++) {
                    if (tile(Puzzle.LIGHTS, 0, i).equals(pos)) {
                        puzzleHurt(player, 20);
                        lights ^= 1 << i;
                        renderLights();
                        if (lights == ToaPuzzles.LIGHTS_SOLVED) {
                            solved(Puzzle.LIGHTS, "Every plate is lit!");
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

        /** Self-test: solves one puzzle as a player would, through the real tiles, button and obelisks. */
        private void selfTestSolve(RaidSelfTest t, Puzzle p) {
            BlockPos neutral = at(-17, 0);
            switch (p) {
                case SEQUENCE -> {
                    t.use(button());
                    t.tick(ToaPuzzles.SEQUENCE_LENGTH * 2 + 2);
                    RaidSelfTest.expect(input == 0, "Scabaras: the sequence was not shown");
                    for (int i : new ArrayList<>(sequence)) {
                        t.stepOn(tile(Puzzle.SEQUENCE, 0, i));
                        t.moveTo(neutral);
                        t.tick(1);
                    }
                }
                case LIGHTS -> {
                    List<Integer> presses = ToaPuzzles.lightsSolution(lights);
                    RaidSelfTest.expect(presses != null, "Scabaras: the lights board can't be solved");
                    for (int i : presses) {
                        // Each step must flip that plate and its neighbours, and the board must show it.
                        int expected = ToaPuzzles.lightsPress(lights, i);
                        t.stepOn(tile(Puzzle.LIGHTS, 0, i));
                        RaidSelfTest.expect(lights == expected, "Scabaras: stepping on light plate " + i + " gave board "
                                + Integer.toBinaryString(lights) + ", not " + Integer.toBinaryString(expected));
                        for (int j = 0; j < 9; j++) {
                            boolean shownLit = world().getBlockState(tile(Puzzle.LIGHTS, 0, j)).is(Blocks.OCHRE_FROGLIGHT);
                            RaidSelfTest.expect(shownLit == ((lights & (1 << j)) != 0), "Scabaras: light plate " + j + " shows "
                                    + (shownLit ? "lit" : "dark") + " against the board");
                        }
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
                                    t.stepOn(tile(Puzzle.MATCHING, b, i));
                                    t.moveTo(neutral);
                                    t.tick(1);
                                }
                            }
                        }
                        RaidSelfTest.expect(matched[sym], "Scabaras: stepping on both " + SYMBOL_NAMES[sym] + " symbols did not match them");
                    }
                }
                case OBELISKS -> {
                    // A wrong first strike stays dark and brings rocks down; then the right order lights them all.
                    int wrong = obeliskOrder.get(1);
                    t.use(obelisk(wrong));
                    RaidSelfTest.expect(obelisksLit == 0, "Scabaras: a wrong obelisk lit up");
                    t.tick(4);
                    for (int i : new ArrayList<>(obeliskOrder)) {
                        t.use(obelisk(i));
                        t.tick(2);
                    }
                }
                case ADDITION -> {
                    t.use(tablet());
                    RaidSelfTest.expect(target >= 20 && target <= 45, "Scabaras: the tablet gave no number");
                    List<Integer> tiles = ToaPuzzles.additionSolution(values, target);
                    RaidSelfTest.expect(tiles != null, "Scabaras: no tiles add up to " + target);
                    int total = 0;
                    for (int i : tiles) {
                        total += values[i];
                        t.stepOn(tile(Puzzle.ADDITION, 0, i));
                        RaidSelfTest.expect(sum == total, "Scabaras: stepping on the " + VALUE_NAMES[values[i] - 1] + " tile (" + values[i]
                                + ") made the total " + sum + ", not " + total);
                        t.moveTo(neutral);
                        t.tick(1);
                    }
                }
            }
        }

        /**
         * Self-test (solo): a first room opens only the other branch's second room, and those two open the tablet; then the
         * other two rooms are solved as well, so every puzzle is exercised, and the tablet last.
         */
        @Override
        String selfTestSolve(RaidSelfTest t) {
            RaidSelfTest.expect(solo(), "Scabaras: the self-test expects a solo raid");
            RaidSelfTest.expect(!open(ToaPuzzles.NORTH_SECOND) && !open(ToaPuzzles.SOUTH_SECOND) && !additionOpen(),
                    "Scabaras: a second room or the tablet was open from the start");
            RaidSelfTest.expect(!world().getBlockState(door(1)).isAir() && !world().getBlockState(door(4)).isAir(),
                    "Scabaras: the second rooms' doors were not shut");
            // OSRS advice: start in the first room whose partner isn't the obelisks.
            int first = layout.get(ToaPuzzles.SOUTH_SECOND) == ToaPuzzles.ScabarasPuzzle.OBELISKS ? ToaPuzzles.SOUTH_FIRST : ToaPuzzles.NORTH_FIRST;
            int partner = first == ToaPuzzles.NORTH_FIRST ? ToaPuzzles.SOUTH_SECOND : ToaPuzzles.NORTH_SECOND;
            int otherFirst = first == ToaPuzzles.NORTH_FIRST ? ToaPuzzles.SOUTH_FIRST : ToaPuzzles.NORTH_FIRST;
            int otherSecond = partner == ToaPuzzles.SOUTH_SECOND ? ToaPuzzles.NORTH_SECOND : ToaPuzzles.SOUTH_SECOND;
            List<String> order = new ArrayList<>();
            for (int room : new int[]{first, partner, otherFirst, otherSecond}) {
                Puzzle p = Puzzle.valueOf(layout.get(room).name());
                RaidSelfTest.expect(active(p), "Scabaras: the " + ROOM_NAMES[room] + " (" + p + ") could not be worked on");
                selfTestSolve(t, p);
                RaidSelfTest.expect(solved[room], "Scabaras: the " + p + " puzzle in the " + ROOM_NAMES[room] + " was not solved");
                order.add(p.name().toLowerCase(java.util.Locale.ROOT));
                if (room == first) {
                    RaidSelfTest.expect(open(partner) && world().getBlockState(door(partner)).isAir(),
                            "Scabaras: solving the " + ROOM_NAMES[first] + " did not open the " + ROOM_NAMES[partner]);
                    RaidSelfTest.expect(!open(otherSecond), "Scabaras: solving one first room opened its own branch's second room solo");
                    RaidSelfTest.expect(!additionOpen(), "Scabaras: the tablet opened after one puzzle");
                }
                if (room == partner) {
                    RaidSelfTest.expect(additionOpen() && world().getBlockState(door(4)).isAir(),
                            "Scabaras: two puzzles did not open the tablet solo");
                }
            }
            selfTestSolve(t, Puzzle.ADDITION);
            RaidSelfTest.expect(done, "Scabaras: the room did not open after the tablet");
            return "Scabaras: " + String.join(", ", order) + ", addition";
        }
    }

    // ================================================================== Path of Het

    /**
     * Path of Het (OSRS wiki): the caster statue fires a beam every few seconds; the party leads it to the shielded statue
     * with mirrors - three movable mirrors they pick up, place and rotate, plus fixed ones (dirty in groups until cleaned) -
     * and by breaking the worn barriers with a pickaxe. A beam on the shielded statue weakens Het's seal for 15 ticks, when
     * it can be mined. Orbs of darkness cross the room (and dirty the mirrors they hit in groups). The statues lend a bronze
     * pickaxe, or hold the one a player deposited, from one raid to the next.
     */
    static final class Het extends Challenge {
        static final int HALF = 7;
        static final int[] SEAL_AT = {15, 0};
        /** The statues holding pickaxes: at the entrance, in the room and by the exit (OSRS). */
        static final int[][] STATUES = {{-18, 4}, {-10, -10}, {13, 5}};
        ToaPuzzles.HetPuzzle puzzle;
        final Set<Long> broken = new HashSet<>();
        int sealHp;
        int sealMax;
        long weakenedUntil = -1;
        private long nextBeam;
        /** The movable mirrors each player carries (OSRS: picked up as an item, then placed). */
        final Map<UUID, List<int[]>> carried = new HashMap<>();
        /** Each movable mirror's starting cell: a mirror whose carrier is downed or leaves goes back there. */
        private final List<int[]> homes = new ArrayList<>();
        /** Orbs of darkness: x, z (block, relative to the centre), dx, dz. */
        private final List<double[]> orbs = new ArrayList<>();
        /** Perfect Het: anyone hit by the beam or an orb, and how many times the seal was weakened. */
        boolean hurtByRoom;
        int exposures;

        Het(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        BlockPos cell(int x, int z) {
            return at(x, z);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() - HALF - 1, c.getY(), c.getZ() - HALF - 1, c.getX() + HALF + 1, c.getY(),
                    c.getZ() + HALF + 1, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            // The grid's floor: a carried mirror is placed by using one of its tiles.
            RaidBuilder.fill(world(), c.getX() - HALF, c.getY(), c.getZ() - HALF, c.getX() + HALF, c.getY(), c.getZ() + HALF,
                    RaidBuilder.module(RaidsContent.TOA_HET_FLOOR));
            RaidBuilder.set(world(), at(SEAL_AT[0], SEAL_AT[1]), RaidBuilder.module(RaidsContent.HET_SEAL));
            RaidBuilder.set(world(), at(SEAL_AT[0], SEAL_AT[1]).above(), Blocks.GOLD_BLOCK.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(SEAL_AT[0], 0, -3), 0, 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.pillar(world(), center().offset(SEAL_AT[0], 0, 3), 0, 4, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            for (int[] s : STATUES) {
                RaidBuilder.set(world(), at(s[0], s[1]), RaidBuilder.module(RaidsContent.TOA_PICKAXE_STATUE));
            }
        }

        /** Clears the grid's mirrors, barriers and obstacles (before a new puzzle is drafted). */
        private void clearGrid() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() - HALF, c.getY() + 1, c.getZ() - HALF, c.getX() + HALF, c.getY() + 2, c.getZ() + HALF,
                    Blocks.AIR.defaultBlockState());
            RaidBuilder.fill(world(), c.getX() - HALF, c.getY(), c.getZ() - HALF, c.getX() + HALF, c.getY(), c.getZ() + HALF,
                    RaidBuilder.module(RaidsContent.TOA_HET_FLOOR));
            for (int z = -HALF; z <= HALF; z++) {
                RaidBuilder.set(world(), cell(-HALF - 2, z), Blocks.AIR.defaultBlockState());
                RaidBuilder.set(world(), cell(-HALF - 2, z).above(), Blocks.AIR.defaultBlockState());
            }
        }

        private void placePuzzle() {
            // Caster statue west of the grid, in line with the beam's row.
            BlockPos caster = cell(-HALF - 2, puzzle.startZ());
            RaidBuilder.set(world(), caster, Blocks.CHISELED_SANDSTONE.defaultBlockState());
            RaidBuilder.set(world(), caster.above(), Blocks.GLOWSTONE.defaultBlockState());
            BlockPos target = cell(puzzle.target()[0], puzzle.target()[1]);
            RaidBuilder.set(world(), target, Blocks.LAPIS_BLOCK.defaultBlockState());
            RaidBuilder.set(world(), target.above(), Blocks.SEA_LANTERN.defaultBlockState());
            for (int[] m : puzzle.fixed()) {
                // Fixed mirrors stand on carved stone; movable ones on the grid's tiles.
                RaidBuilder.set(world(), cell(m[0], m[1]).below(), Blocks.CHISELED_SANDSTONE.defaultBlockState());
                RaidBuilder.set(world(), cell(m[0], m[1]), RaidBuilder.module(RaidsContent.TOA_MIRROR));
            }
            for (int[] m : puzzle.movable()) {
                RaidBuilder.set(world(), cell(m[0], m[1]), RaidBuilder.module(RaidsContent.TOA_MIRROR));
            }
            for (int[] b : puzzle.barriers()) {
                RaidBuilder.set(world(), cell(b[0], b[1]), RaidBuilder.module(RaidsContent.TOA_WORN_BARRIER));
            }
            for (int[] o : puzzle.obstacles()) {
                RaidBuilder.set(world(), cell(o[0], o[1]), Blocks.SANDSTONE_WALL.defaultBlockState());
            }
        }

        /** OSRS: a wipe resets the statue's progress and drafts a new puzzle, so each attempt gets a fresh grid. */
        @Override
        public void start() {
            clearGrid();
            puzzle = ToaPuzzles.hetPuzzle(HALF, raid.partySize(), rnd());
            placePuzzle();
            homes.clear();
            for (int[] m : puzzle.movable()) {
                homes.add(new int[]{m[0], m[1]});
            }
            broken.clear();
            orbs.clear();
            carried.clear();
            hurtByRoom = false;
            exposures = 0;
            sealMax = ToaPuzzles.sealHitpoints(raid.partySize());
            sealHp = sealMax;
            weakenedUntil = -1;
            nextBeam = now() + 6;
            warnedPath = null;
            RaidBuilder.set(world(), at(SEAL_AT[0], SEAL_AT[1]), RaidBuilder.module(RaidsContent.HET_SEAL));
            RaidBuilder.set(world(), at(SEAL_AT[0], SEAL_AT[1]).above(), Blocks.GOLD_BLOCK.defaultBlockState());
            say("Het's seal is shielded. Lead the caster statue's beam to the shielded statue: use a movable mirror to rotate it, "
                    + "sneak and use it to pick it up, use a floor tile to place it; break the worn barriers with a pickaxe.");
        }

        boolean weakened() {
            return now() < weakenedUntil;
        }

        /** The beam path shown before it fires (round 43): cells from the caster on, or null while none is shown. */
        private List<int[]> warnedPath;
        /** Self-test: beams fired, how many game ticks the last one's path glowed first, and players burnt. */
        int beamsFired;
        long lastBeamLead = -1;
        private long beamWarnedAt;
        int beamBurns;
        private static final DustParticleOptions BEAM_WARN = new DustParticleOptions(new Vector3f(1.0f, 0.75f, 0.2f), 1.0f);

        @Override
        public void gameTick(long tick) {
            returnStrandedMirrors();
            int lead = com.runecraft.osrs.content.raids.RaidRealtime.HET_BEAM_LEAD_TICKS;
            if (warnedPath == null && now() >= nextBeam - lead) {
                warnBeam(lead);
            }
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
            // OSRS: orbs of darkness cross the room in straight lines, now and then two on one tile. // approx: one every 7 ticks
            if (tick % 7 == 0) {
                boolean alongX = rnd().nextBoolean();
                int lane = rnd().nextInt(2 * RaidBuilder.HALF - 3) - RaidBuilder.HALF + 2;
                int side = rnd().nextBoolean() ? 1 : -1;
                int n = rnd().nextInt(6) == 0 ? 2 : 1;
                for (int k = 0; k < n; k++) {
                    double[] o = alongX ? new double[]{-side * RaidBuilder.HALF, lane, side, 0} : new double[]{lane, -side * RaidBuilder.HALF, 0, side};
                    orbs.add(o);
                    drawOrb(o);
                }
            }
            moveOrbs();
            for (ServerPlayer p : players()) {
                int n = carried(p);
                if (n > 0 && tick % 10 == 0) {
                    Msg.action(p, "Carrying " + n + " mirror" + (n == 1 ? "" : "s") + ": use a floor tile to place one.");
                }
            }
        }

        /**
         * Mirrors carried by someone who is no longer in the room - downed, dead, logged out or gone - go back to the grid,
         * or the rest of the party could never finish the beam's path.
         */
        private void returnStrandedMirrors() {
            if (carried.isEmpty()) {
                return;
            }
            Set<UUID> here = new HashSet<>();
            for (ServerPlayer p : players()) {
                here.add(p.getUUID());
            }
            java.util.Iterator<Map.Entry<UUID, List<int[]>>> it = carried.entrySet().iterator();
            boolean returned = false;
            while (it.hasNext()) {
                Map.Entry<UUID, List<int[]>> e = it.next();
                if (here.contains(e.getKey())) {
                    continue;
                }
                e.getValue().removeIf(this::returnMirror);
                returned = true;
                if (e.getValue().isEmpty()) {
                    it.remove();
                }
            }
            if (returned) {
                say("A dropped mirror returns to the grid.");
                showMirrors();
            }
        }

        /** Stands a carried mirror back on its starting cell, or on the first free cell when that one is taken. */
        private boolean returnMirror(int[] m) {
            int i = puzzle.movable().indexOf(m);
            int[] spot = null;
            if (i >= 0 && i < homes.size() && freeForMirror(homes.get(i)[0], homes.get(i)[1])) {
                spot = homes.get(i);
            }
            for (int z = -HALF; spot == null && z <= HALF; z++) {
                for (int x = -HALF; spot == null && x <= HALF; x++) {
                    if (freeForMirror(x, z)) {
                        spot = new int[]{x, z};
                    }
                }
            }
            if (spot == null) {
                return false;
            }
            m[0] = spot[0];
            m[1] = spot[1];
            RaidBuilder.set(world(), cell(m[0], m[1]), RaidBuilder.module(RaidsContent.TOA_MIRROR));
            return true;
        }

        /** A grid cell a mirror may stand on: placeable, empty and with nobody standing in it. */
        private boolean freeForMirror(int x, int z) {
            BlockPos pos = cell(x, z);
            if (!puzzle.canPlace(x, z, broken) || !world().getBlockState(pos).isAir()) {
                return false;
            }
            for (ServerPlayer p : raid.players()) {
                if (p.blockPosition().equals(pos)) {
                    return false;
                }
            }
            return true;
        }

        /** Mirror orientations as a short diagonal of particles ('/' or '\' seen from above); dirty ones smoke. */
        private void showMirrors() {
            for (List<int[]> list : List.of(puzzle.fixed(), puzzle.movable())) {
                for (int[] m : list) {
                    if (m[0] == ToaPuzzles.CARRIED) {
                        continue;
                    }
                    BlockPos p = cell(m[0], m[1]);
                    if (m[3] != 0) {
                        world().sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.01);
                        continue;
                    }
                    for (double s = -0.4; s <= 0.41; s += 0.2) {
                        double z = m[2] == ToaPuzzles.SLASH ? -s : s;
                        world().sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5 + s, p.getY() + 1.1, p.getZ() + 0.5 + z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        /** The beam's cells as it would fire now: the one in front of the caster, then its traced path. */
        private List<int[]> beamPath(ToaPuzzles.Beam beam) {
            List<int[]> cells = new ArrayList<>();
            cells.add(new int[]{-HALF - 1, puzzle.startZ()});
            cells.addAll(beam.cells());
            return cells;
        }

        /**
         * Whether a player at {@code p} stands on a beam path: within {@link com.runecraft.osrs.content.raids.RaidRealtime#HET_BEAM_HALF_WIDTH}
         * of the line through its cells' centres. // playability: OSRS burns the tiles it crosses
         */
        private boolean onBeam(Vec3 p, List<int[]> cells) {
            double cx = center().getX() + 0.5;
            double cz = center().getZ() + 0.5;
            for (int i = 0; i < cells.size(); i++) {
                int[] a = cells.get(i);
                int[] b = cells.get(Math.min(cells.size() - 1, i + 1));
                if (com.runecraft.osrs.content.raids.RaidRealtime.inLane(p.x, p.z, cx + a[0], cz + a[1], cx + b[0], cz + b[1],
                        com.runecraft.osrs.content.raids.RaidRealtime.HET_BEAM_HALF_WIDTH)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * The caster statue charges: the path its beam will take glows along the floor for {@code lead} game ticks before it
         * fires (round 43; it fired with no warning). // playability: OSRS shows the statue's animation
         */
        private void warnBeam(int lead) {
            List<int[]> path = beamPath(ToaPuzzles.trace(puzzle, broken));
            warnedPath = path;
            beamWarnedAt = now();
            BlockPos caster = cell(-HALF - 2, puzzle.startZ());
            world().playSound(null, caster, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.8f);
            double y = center().getY() + 1.05;
            RaidEffects.cue(lead, center().getCenter(), () -> {
                if (warnedPath != path || done) {
                    return false;
                }
                for (int i = 0; i + 1 < path.size(); i++) {
                    int[] a = path.get(i);
                    int[] b = path.get(i + 1);
                    for (double f = 0; f < 1; f += 0.34) {
                        world().sendParticles(BEAM_WARN, center().getX() + 0.5 + a[0] + (b[0] - a[0]) * f, y,
                                center().getZ() + 0.5 + a[1] + (b[1] - a[1]) * f, 1, 0, 0, 0, 0);
                    }
                }
                return true;
            });
        }

        private void fireBeam() {
            ToaPuzzles.Beam beam = ToaPuzzles.trace(puzzle, broken);
            double y = center().getY() + 1.6;
            BlockPos caster = cell(-HALF - 2, puzzle.startZ());
            world().playSound(null, caster, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
            DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.3f), 1.2f);
            List<int[]> cells = beamPath(beam);
            List<int[]> warned = warnedPath == null ? cells : warnedPath;
            lastBeamLead = warnedPath == null ? 0 : now() - beamWarnedAt;
            warnedPath = null;
            beamsFired++;
            for (int[] c : cells) {
                world().sendParticles(gold, center().getX() + 0.5 + c[0], y, center().getZ() + 0.5 + c[1], 4, 0.2, 0.05, 0.2, 0);
            }
            // It burns whoever stands on its path, as long as that path glowed before it fired (a mirror turned meanwhile can't
            // burn anyone on a tile that never glowed).
            for (ServerPlayer p : players()) {
                if (onBeam(p.position(), cells) && onBeam(p.position(), warned)) {
                    Msg.red(p, "The beam of light burns you!");
                    hurtByRoom = true;
                    beamBurns++;
                    hurt(p, 4 + rnd().nextInt(5)); // approx: "lower than the orbs"
                }
            }
            if (beam.hit()) {
                weakenedUntil = now() + ToaPuzzles.SEAL_WEAKENED_TICKS;
                exposures++;
                say("The shielded statue absorbs the beam - Het's seal is weakened! Mine it now.");
            }
        }

        /**
         * An orb of darkness drawn between game ticks, gliding along its line with a short trail ahead showing where it heads
         * (round 43; it was drawn once a game tick, jumping a block at a time). // playability
         */
        private void drawOrb(double[] o) {
            RaidEffects.cue(2 * RaidBuilder.HALF + 4, center().getCenter(), () -> {
                if (!orbs.contains(o) || done) {
                    return false;
                }
                double f = (RaidEffects.clockMc() - RaidEffects.now() * com.runecraft.osrs.ArenaTiming.MC_PER_GAME_TICK)
                        / (double) com.runecraft.osrs.ArenaTiming.MC_PER_GAME_TICK;
                Vec3 at = vec(o[0] + o[2] * f, o[1] + o[3] * f);
                world().sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 0.8, at.z, 3, 0.12, 0.12, 0.12, 0);
                for (int k = 1; k <= 3; k++) {
                    Vec3 ahead = vec(o[0] + o[2] * (f + k), o[1] + o[3] * (f + k));
                    world().sendParticles(ORB_AHEAD, ahead.x, ahead.y + 0.15, ahead.z, 1, 0, 0, 0, 0);
                }
                return true;
            });
        }

        private static final DustParticleOptions ORB_AHEAD = new DustParticleOptions(new Vector3f(0.35f, 0.1f, 0.45f), 0.9f);

        private void moveOrbs() {
            java.util.Iterator<double[]> it = orbs.iterator();
            boolean group = raid.partySize() > 1;
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
                if (group) {
                    // OSRS: in groups an orb that runs into a mirror can leave it dirty.
                    int[] m = puzzle.mirrorAt((int) Math.floor(o[0]), (int) Math.floor(o[1]));
                    if (m != null && m[3] == 0 && rnd().nextInt(ToaPuzzles.ORB_DIRTIES_ONE_IN) == 0) {
                        m[3] = 1;
                        say("An orb of darkness smears a mirror - it must be cleaned before it reflects.");
                    }
                }
                for (ServerPlayer p : players()) {
                    if (RaidEffects.horizontalDistSq(p.position(), at) < 0.8 * 0.8) {
                        Msg.red(p, "An orb of darkness slams into you!");
                        hurtByRoom = true;
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

        /**
         * A statue (OSRS "Take-pickaxe" / "Deposit-pickaxe"): it holds the pickaxe the player deposited in an earlier visit -
         * kept between raids - or a bronze one. Sneak and use it to deposit your best pickaxe.
         */
        private void statue(ServerPlayer player) {
            RaidsState state = RaidsState.get(world().getServer());
            net.minecraft.world.item.ItemStack stored = state.storedPickaxe(player.getUUID());
            if (player.isShiftKeyDown()) {
                String best = pickaxe(player);
                if (best == null) {
                    Msg.game(player, "You have no pickaxe to deposit.");
                } else if (!stored.isEmpty()) {
                    Msg.game(player, "The statue already holds your " + stored.getHoverName().getString() + ".");
                } else {
                    net.minecraft.world.item.ItemStack taken = takeStack(player, best);
                    state.setStoredPickaxe(player.getUUID(), taken);
                    Msg.game(player, "You deposit your pickaxe in the statue. It will keep it for your next raid.");
                }
                return;
            }
            if (!stored.isEmpty()) {
                if (!player.getInventory().add(stored.copy())) {
                    Msg.game(player, "You need a free inventory slot to take your pickaxe.");
                    return;
                }
                state.setStoredPickaxe(player.getUUID(), net.minecraft.world.item.ItemStack.EMPTY);
                Msg.game(player, "You take your pickaxe back from the statue.");
            } else if (pickaxe(player) != null) {
                Msg.game(player, "You already have a pickaxe.");
            } else {
                Inv.give(player, "bronze_pickaxe", 1);
                Msg.game(player, "You take a bronze pickaxe from the statue.");
            }
        }

        /** Removes one of an item from the main hand or inventory, keeping its data (charges), and returns it. */
        private static net.minecraft.world.item.ItemStack takeStack(ServerPlayer p, String id) {
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && id.equals(Inv.idOf(s))) {
                    return inv.removeItem(i, 1);
                }
            }
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        int carried(ServerPlayer p) {
            List<int[]> mine = carried.get(p.getUUID());
            return mine == null ? 0 : mine.size();
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            int x = pos.getX() - center().getX();
            int z = pos.getZ() - center().getZ();
            switch (blockId) {
                case RaidsContent.TOA_PICKAXE_STATUE -> {
                    statue(player);
                    return true;
                }
                case RaidsContent.TOA_MIRROR -> {
                    useMirror(player, pos, x, z);
                    return true;
                }
                case RaidsContent.TOA_HET_FLOOR -> {
                    placeMirror(player, x, z);
                    return true;
                }
                case RaidsContent.TOA_WORN_BARRIER -> {
                    if (pickaxe(player) == null) {
                        Msg.game(player, "You need a pickaxe to break the barrier. The statues hold one.");
                        return true;
                    }
                    if (!cooldown(player, 2)) {
                        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        broken.add(ToaPuzzles.cellKey(x, z));
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

        /** Using a mirror: clean it if dirty; else rotate a movable one, or pick it up when sneaking. */
        private void useMirror(ServerPlayer player, BlockPos pos, int x, int z) {
            int[] fixed = puzzle.fixedAt(x, z);
            int[] movable = puzzle.movableAt(x, z);
            int[] m = fixed != null ? fixed : movable;
            if (m == null || cooldown(player, 1)) {
                return;
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (m[3] != 0) {
                m[3] = 0;
                world().sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0);
                Msg.action(player, "You wipe the mirror clean.");
                showMirrors();
                return;
            }
            if (fixed != null) {
                Msg.game(player, "This mirror is fixed in place.");
                return;
            }
            if (player.isShiftKeyDown()) {
                movable[0] = ToaPuzzles.CARRIED;
                movable[1] = ToaPuzzles.CARRIED;
                carried.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(movable);
                RaidBuilder.set(world(), pos, Blocks.AIR.defaultBlockState());
                Msg.action(player, "You pick up the mirror.");
                return;
            }
            movable[2] = movable[2] == ToaPuzzles.SLASH ? ToaPuzzles.BACKSLASH : ToaPuzzles.SLASH;
            world().playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
            showMirrors();
            Msg.action(player, "You turn the mirror: it now faces " + (movable[2] == ToaPuzzles.SLASH ? "'/'" : "'\\'") + ".");
        }

        /** Places a carried mirror on the grid tile used (facing '/'). */
        private void placeMirror(ServerPlayer player, int x, int z) {
            if (carried(player) <= 0) {
                Msg.game(player, "Pick up one of the movable mirrors (sneak and use it) to place it here.");
                return;
            }
            if (!puzzle.canPlace(x, z, broken) || !world().getBlockState(cell(x, z)).isAir()) {
                Msg.game(player, "You can't place a mirror there.");
                return;
            }
            List<int[]> mine = carried.get(player.getUUID());
            int[] m = mine.remove(mine.size() - 1);
            if (mine.isEmpty()) {
                carried.remove(player.getUUID());
            }
            m[0] = x;
            m[1] = z;
            m[2] = ToaPuzzles.SLASH;
            RaidBuilder.set(world(), cell(x, z), RaidBuilder.module(RaidsContent.TOA_MIRROR));
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            showMirrors();
            Msg.action(player, "You place the mirror, facing '/'.");
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
                // OSRS CA "Perfect Het": no damage from the beam or the orbs, and the seal broken in one exposure.
                if (!hurtByRoom && exposures <= 1) {
                    for (ServerPlayer pl : players()) {
                        com.runecraft.content.systems.CombatAchievementService.flag(pl, ToaLoot.FLAG_PERFECT_HET);
                    }
                }
                complete();
            }
        }

        @Override
        public void cleanup() {
            super.cleanup();
            carried.clear();
        }

        @Override
        String selfTestSolve(RaidSelfTest t) {
            ServerPlayer p = t.player();
            RaidsState state = RaidsState.get(world().getServer());
            net.minecraft.world.item.ItemStack storedBefore = state.storedPickaxe(p.getUUID());
            // The test takes the player's own bronze and rune pickaxes away for a while: they get them all back after.
            Map<String, Integer> picksBefore = new HashMap<>();
            for (String pick : List.of("bronze_pickaxe", "rune_pickaxe")) {
                picksBefore.put(pick, Inv.count(p, pick));
            }
            String note;
            try {
                state.setStoredPickaxe(p.getUUID(), net.minecraft.world.item.ItemStack.EMPTY);
                for (String pick : List.of("bronze_pickaxe", "rune_pickaxe")) {
                    Inv.remove(p, pick, Inv.count(p, pick));
                }
                BlockPos statue = at(STATUES[0][0], STATUES[0][1]);
                // Deposit a rune pickaxe (sneaking), take it back: the statue keeps it between visits.
                Inv.give(p, "rune_pickaxe", 1);
                // The statue takes the best pickaxe carried (a real player may carry a better one than the rune pickaxe).
                String best = pickaxe(p);
                int bestBefore = Inv.count(p, best);
                p.setShiftKeyDown(true);
                t.use(statue);
                p.setShiftKeyDown(false);
                RaidSelfTest.expect(Inv.count(p, best) == bestBefore - 1 && best.equals(Inv.idOf(state.storedPickaxe(p.getUUID()))),
                        "Het: the statue did not take the deposited " + best);
                t.use(statue);
                RaidSelfTest.expect(Inv.count(p, best) == bestBefore && state.storedPickaxe(p.getUUID()).isEmpty(),
                        "Het: the statue did not hand back the deposited pickaxe");
                Inv.remove(p, "rune_pickaxe", 1);
                t.use(statue);
                RaidSelfTest.expect(pickaxe(p) != null, "Het: the statue gave no pickaxe");
                // The beam can't reach the statue before the mirrors are placed.
                RaidSelfTest.expect(!ToaPuzzles.trace(puzzle, broken).hit(), "Het: the beam reached the shielded statue before any work");
                // A mirror carried by a party member who falls (here: one no longer in the room) goes back to its cell.
                int[] first = puzzle.movable().get(0);
                int homeX = first[0];
                int homeZ = first[1];
                t.tick(1);
                p.setShiftKeyDown(true);
                t.use(cell(homeX, homeZ));
                p.setShiftKeyDown(false);
                RaidSelfTest.expect(first[0] == ToaPuzzles.CARRIED && carried(p) == 1, "Het: sneaking and using a mirror did not pick it up");
                carried.put(UUID.randomUUID(), carried.remove(p.getUUID()));
                t.tick(1);
                RaidSelfTest.expect(first[0] == homeX && first[1] == homeZ && carried.isEmpty()
                                && world().getBlockState(cell(homeX, homeZ)).is(RaidBuilder.module(RaidsContent.TOA_MIRROR).getBlock()),
                        "Het: a mirror carried by a fallen player did not return to the grid");
                List<int[]> plan = ToaPuzzles.hetSolution(puzzle);
                for (int[] step : plan) {
                    int[] m = puzzle.movable().get(step[0]);
                    t.tick(1);
                    p.setShiftKeyDown(true);
                    t.use(cell(m[0], m[1]));
                    p.setShiftKeyDown(false);
                    RaidSelfTest.expect(m[0] == ToaPuzzles.CARRIED && carried(p) == 1, "Het: sneaking and using a mirror did not pick it up");
                    t.tick(1);
                    t.use(cell(step[1], step[2]).below());
                    RaidSelfTest.expect(m[0] == step[1] && m[1] == step[2] && carried(p) == 0, "Het: using a floor tile did not place the mirror");
                    int turns = 0;
                    while (m[2] != step[3] && turns++ < 3) {
                        t.tick(1);
                        t.use(cell(m[0], m[1]));
                    }
                    RaidSelfTest.expect(m[2] == step[3], "Het: a placed mirror would not turn");
                }
                for (int[] f : puzzle.fixed()) {
                    if (f[3] != 0) {
                        t.tick(1);
                        t.use(cell(f[0], f[1]));
                        RaidSelfTest.expect(f[3] == 0, "Het: a dirty mirror was not cleaned");
                    }
                }
                for (int[] b : puzzle.barriers()) {
                    t.tick(2);
                    t.use(cell(b[0], b[1]));
                }
                RaidSelfTest.expect(ToaPuzzles.trace(puzzle, broken).hit(), "Het: the beam does not reach the shielded statue after the fix");
                BlockPos seal = at(SEAL_AT[0], SEAL_AT[1]);
                int guard = 0;
                while (!done && guard++ < 600) {
                    if (weakened()) {
                        t.use(seal);
                        t.tick(2);
                    } else {
                        t.tick(1);
                    }
                }
                RaidSelfTest.expect(done, "Het: the seal did not break (" + sealHp + "/" + sealMax + " left)");
                // round 43: every beam's path glowed for at least the strike floor before it fired
                RaidSelfTest.expect(beamsFired > 0 && lastBeamLead >= com.runecraft.osrs.content.raids.RaidRealtime.HET_BEAM_LEAD_TICKS,
                        "Het: the beam fired " + lastBeamLead + " game ticks after its path glowed (" + beamsFired + " fired)");
                note = "Het: " + plan.size() + " mirror(s) placed, seal broken in " + exposures + " exposure(s), the statue keeps a pickaxe, "
                        + beamsFired + " beam(s) shown " + lastBeamLead + " ticks ahead";
            } finally {
                p.setShiftKeyDown(false);
                // A pickaxe left in the statue by a failed step goes back to the player before the statue is restored.
                net.minecraft.world.item.ItemStack left = state.storedPickaxe(p.getUUID());
                if (!left.isEmpty()) {
                    Inv.give(p, left.copy());
                }
                state.setStoredPickaxe(p.getUUID(), storedBefore);
                for (Map.Entry<String, Integer> e : picksBefore.entrySet()) {
                    int now = Inv.count(p, e.getKey());
                    if (now > e.getValue()) {
                        Inv.remove(p, e.getKey(), now - e.getValue());
                    } else if (now < e.getValue()) {
                        Inv.give(p, e.getKey(), e.getValue() - now);
                    }
                }
            }
            return note;
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
        /** Perfect Apmeken: a danger triggered (missed or wrong fix), someone stood in venom or a volatile baboon hit. */
        boolean imperfect;
        private long startedAt;

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
            imperfect = false;
            startedAt = now();
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
                // OSRS CA "Perfect Apmeken": a group, no danger triggered, no venom, no volatile hit, under three minutes.
                if (!imperfect && raid.partySize() >= 2 && now() - startedAt < ToaLoot.PERFECT_APMEKEN_TICKS) {
                    for (ServerPlayer p : players()) {
                        com.runecraft.content.systems.CombatAchievementService.flag(p, ToaLoot.FLAG_PERFECT_APMEKEN);
                    }
                }
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
                    imperfect = true;
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
                        imperfect = true;
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
                    imperfect = true;
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
                imperfect = true;
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
                        imperfect = true;
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
        /** Perfect Crondis: a crocodile reached the tree, or someone lost water. */
        boolean imperfect;
        private long startedAt;

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
            imperfect = false;
            startedAt = now();
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
            later(2, () -> {
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
                imperfect = true;
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
                    imperfect = true;
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
            // OSRS: the crocodiles don't scale with raid level (or path level).
            RaidEffects.hitRaw(croc, p, dmg);
            int w = water(p);
            if (w > 0) {
                imperfect = true;
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
                    // OSRS CA "Perfect Crondis": no crocodile at the tree, no water lost, under a minute.
                    if (!imperfect && now() - startedAt < ToaLoot.PERFECT_CRONDIS_TICKS) {
                        for (ServerPlayer pl : players()) {
                            com.runecraft.content.systems.CombatAchievementService.flag(pl, ToaLoot.FLAG_PERFECT_CRONDIS);
                        }
                    }
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
