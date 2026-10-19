package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Wilderness levers and teleport obelisks, at their OSRS tiles (skretzo/shortest-path {@code teleportation_levers.tsv}
 * and {@code wilderness_obelisks.tsv}, checked by {@code TransportSnapshotTest}).
 *
 * <ul>
 *   <li>Levers: pulling one sends the player straight to the paired tile. The Ardougne and Edgeville levers lead to the
 *   Deserted Keep (level 53 Wilderness); the Deserted Keep lever leads back to Ardougne only; the Mage Arena lever and the
 *   Mage Bank lever lead to each other. The King Black Dragon's lever (in the small chamber at the foot of the Lava Maze's
 *   ladder) and the lever in his lair lead to each other. Teleport Block stops them; the Wilderness level does not.</li>
 *   <li>Obelisks (levels 13, 19, 27, 35, 44 and 50): four pillars around a 3x3 platform. Activating any pillar starts the
 *   obelisk; a few moments later everyone standing on the platform is sent to the same random obelisk (any of the six:
 *   "players may even be teleported to the same location", OSRS Wiki), keeping their place on the platform. With the hard
 *   Wilderness Diary a player can set the destination instead.</li>
 * </ul>
 *
 * The level 13 obelisk is moved {@link #OBELISK_13_EAST_SHIFT} tiles east, clear of this world's Ferox Enclave.
 * The Mage Bank is a separate area in OSRS (tile 2539, 4712); here it is a small cavern carved at that tile, deep under
 * the sea, with its bank booths and the lever back up (see {@link #mageBank()}). The King Black Dragon Lair (OSRS 2256-2287,
 * 4680-4711) is carved the same way at its own tiles ({@link #kbdLair()}); the Lava Maze dungeon around its entrance lever
 * (OSRS 3067, 10253) isn't built, so that lever stands in a small chamber dug off the foot of this world's Lava Maze KBD
 * ladder ({@link #kbdLeverRoom()}).
 */
public final class WildernessTransport {
    private WildernessTransport() {
    }

    /** A lever on tile (x, y) that sends whoever pulls it to tile (toX, toY). */
    public record Lever(String id, String name, int x, int y, int toX, int toY, String toName) {
        public String placeId() {
            return "wild_lever_" + id;
        }
    }

    /** An obelisk: the centre tile of its 3x3 platform (the pillars stand on the corner tiles two away). */
    public record Obelisk(int level, String name, int x, int y) {
        public String placeId() {
            return "wild_obelisk_" + level;
        }
    }

    public static final List<Lever> LEVERS = List.of(
            new Lever("ardougne", "Ardougne lever", 2561, 3311, 3154, 3924, "the Deserted Keep"),
            new Lever("deserted_keep", "Deserted Keep lever", 3153, 3923, 2562, 3311, "Ardougne"),
            new Lever("edgeville", "Edgeville lever", 3090, 3475, 3154, 3924, "the Deserted Keep"),
            new Lever("mage_arena", "Mage Arena lever", 3090, 3956, 2539, 4712, "the Mage Bank"),
            new Lever("mage_bank", "Mage Bank lever", 2539, 4712, 3090, 3956, "the Mage Arena"),
            // OSRS 3067,10253 -> 2271,4680 and back to 3067,10254: the entrance chamber's tiles are this world's (kbdLeverRoom).
            new Lever("kbd_lair_entrance", "King Black Dragon Lair lever", 3037, 3851, 2271, 4680, "the King Black Dragon's lair"),
            new Lever("kbd_lair", "King Black Dragon Lair lever", 2271, 4680, 3036, 3851, "the Lava Maze"));

    /** Whether a lever is part of a carved cavern's structure (the Mage Bank, the KBD lair and its entrance chamber). */
    public static boolean inCavern(Lever l) {
        return l.id().equals("mage_bank") || l.id().startsWith("kbd_lair");
    }

    /** The lever with this id, or null. */
    public static Lever lever(String id) {
        for (Lever l : LEVERS) {
            if (l.id().equals(id)) {
                return l;
            }
        }
        return null;
    }

    /**
     * Block (x, feet y, z) where a lever's traveller lands when its destination is a carved cavern (the Mage Bank, the KBD
     * lair, the KBD entrance chamber), or null for a surface destination.
     */
    public static int[] cavernArrival(Lever l) {
        return switch (l.id()) {
            case "mage_arena" -> mageBankArrival();
            case "kbd_lair_entrance" -> kbdLairArrival();
            case "kbd_lair" -> kbdRoomArrival();
            default -> null;
        };
    }

    /**
     * Tiles the level 13 obelisk stands east of its OSRS tile (3156, 3620): this world's Ferox Enclave (approx layout) has
     * its east wall, south-east tower and shop hall there, which laying the obelisk's square would cut through.
     */
    public static final int OBELISK_13_EAST_SHIFT = 4;

    public static final List<Obelisk> OBELISKS = List.of(
            new Obelisk(13, "south-east of the Ferox Enclave", 3156 + OBELISK_13_EAST_SHIFT, 3620),
            new Obelisk(19, "east of the Graveyard of Shadows", 3227, 3667),
            new Obelisk(27, "north of the Bandit Camp", 3035, 3732),
            new Obelisk(35, "south of the Lava Maze", 3106, 3794),
            new Obelisk(44, "west of the Lava Maze", 2980, 3866),
            new Obelisk(50, "south-east of the Rogues' Castle", 3307, 3916));

    /** Game ticks between activating an obelisk and the teleport. approx (OSRS: a few seconds) */
    public static final int OBELISK_DELAY_TICKS = 6;
    /** Game ticks an obelisk stays inert after it fires. approx */
    public static final int OBELISK_COOLDOWN_TICKS = 4;

    /** The lever standing on tile (x, y), or null. */
    public static Lever leverAt(int x, int y) {
        for (Lever l : LEVERS) {
            if (l.x() == x && l.y() == y) {
                return l;
            }
        }
        return null;
    }

    /** The obelisk whose 3x3 platform holds tile (x, y), or null. */
    public static Obelisk platformAt(int x, int y) {
        for (Obelisk o : OBELISKS) {
            if (Math.abs(o.x() - x) <= 1 && Math.abs(o.y() - y) <= 1) {
                return o;
            }
        }
        return null;
    }

    /** The obelisk one of whose four pillars stands on tile (x, y), or null. */
    public static Obelisk pillarAt(int x, int y) {
        for (Obelisk o : OBELISKS) {
            if (Math.abs(o.x() - x) == 2 && Math.abs(o.y() - y) == 2) {
                return o;
            }
        }
        return null;
    }

    /**
     * A random obelisk, each of the six equally likely: "The location that the obelisk will send the player(s) in is
     * completely random, and players may even be teleported to the same location" (OSRS Wiki).
     */
    public static Obelisk destination(Obelisk from, Random random) {
        return OBELISKS.get(random.nextInt(OBELISKS.size()));
    }

    /** The obelisk after {@code chosen} in level order (the hard Wilderness Diary's Set Destination), wrapping round. */
    public static Obelisk nextChoice(Obelisk chosen) {
        int i = chosen == null ? -1 : OBELISKS.indexOf(chosen);
        return OBELISKS.get((i + 1) % OBELISKS.size());
    }

    /** The obelisk of this Wilderness level, or null. */
    public static Obelisk obelisk(int level) {
        for (Obelisk o : OBELISKS) {
            if (o.level() == level) {
                return o;
            }
        }
        return null;
    }

    /** Bare places (built nothing; the Minecraft side lays the lever or obelisk when their chunk loads). */
    public static List<PlaceDef> places() {
        List<PlaceDef> out = new ArrayList<>();
        for (Lever l : LEVERS) {
            if (!inCavern(l)) { // the Mage Bank's and the KBD levers are part of their caverns' structures
                out.add(PlaceDef.of(l.placeId(), l.name(), l.x(), l.y(), PlaceDef.Style.NONE, List.of()));
            }
        }
        for (Obelisk o : OBELISKS) {
            out.add(PlaceDef.of(o.placeId(), "Level " + o.level() + " Wilderness obelisk", o.x(), o.y(), PlaceDef.Style.NONE, List.of()));
        }
        return out;
    }

    // ------------------------------------------------------------------ the Mage Bank

    /** Floor block height of the Mage Bank cavern (deep enough to leave the sea bed above it alone). */
    public static final int MAGE_BANK_FLOOR = 20;
    /** The cavern's tile box {x1, y1, x2, y2}: around the lever's landing tile. approx layout */
    public static final int[] MAGE_BANK_TILES = {2532, 4706, 2546, 4719};

    /** The Mage Bank: a stone cavern with bank booths along the north wall and the lever back to the Mage Arena. */
    public static StructureDef mageBank() {
        int[] t = MAGE_BANK_TILES;
        int x1 = StructureDef.bx(t[0]);
        int x2 = StructureDef.bx(t[2]) + 1;
        int z1 = StructureDef.bz(t[3]);
        int z2 = StructureDef.bz(t[1]) + 1;
        int f = MAGE_BANK_FLOOR;
        StructureDef.Builder b = StructureDef.builder("wild_mage_bank", "Mage Bank");
        b.fill(x1 - 1, f - 1, z1 - 1, x2 + 1, f + 6, z2 + 1, "minecraft:stone_bricks");
        b.clear(x1, f + 1, z1, x2, f + 5, z2);
        b.fill(x1, f, z1, x2, f, z2, "minecraft:polished_andesite");
        b.walls(x1, f + 1, z1, x2, f + 1, z2, "minecraft:mossy_stone_bricks");
        for (int x = x1 + 3; x <= x2 - 3; x += 4) {
            b.set(x, f + 1, z1, "runecraft:bank_booth");
        }
        for (int x = x1 + 2; x <= x2 - 2; x += 6) {
            b.set(x, f + 5, z1 + 2, "minecraft:glowstone");
            b.set(x, f + 5, z2 - 2, "minecraft:glowstone");
        }
        Lever back = lever("mage_bank");
        b.set(StructureDef.bx(back.x()), f + 1, StructureDef.bz(back.y()), "minecraft:lever[face=floor,facing=north]");
        return b.build();
    }

    /** Block (x, floor + 1, z) where a traveller lands in the Mage Bank: the lever's tile, beside the lever block. */
    public static int[] mageBankArrival() {
        Lever bank = lever("mage_bank");
        return new int[]{StructureDef.bx(bank.x()) + 1, MAGE_BANK_FLOOR + 1, StructureDef.bz(bank.y()) + 1};
    }

    // ------------------------------------------------------------------ the King Black Dragon Lair

    /** Floor block height of the KBD lair cavern (deep enough to leave the sea bed above it alone, like the Mage Bank). */
    public static final int KBD_LAIR_FLOOR = 20;
    /** The lair's tile box {x1, y1, x2, y2} (OSRS: the lair spans about 2256-2287, 4680-4711; the lever at 2271, 4680). */
    public static final int[] KBD_LAIR_TILES = {2256, 4679, 2287, 4711};
    /** Where the King Black Dragon's summoning stone stands: the middle of his lair (OSRS: he roams around 2273, 4698). */
    public static final int[] KBD_STONE_TILE = {2272, 4700};

    /**
     * The King Black Dragon Lair: a blackstone cavern with lava along its east and west sides, bones of past challengers,
     * the dragon's summoning stone in the middle and the lever back to the Lava Maze at its south end. approx layout
     */
    public static StructureDef kbdLair() {
        int[] t = KBD_LAIR_TILES;
        int x1 = StructureDef.bx(t[0]);
        int x2 = StructureDef.bx(t[2]) + 1;
        int z1 = StructureDef.bz(t[3]);
        int z2 = StructureDef.bz(t[1]) + 1;
        int f = KBD_LAIR_FLOOR;
        StructureDef.Builder b = StructureDef.builder("wild_kbd_lair", "King Black Dragon Lair");
        b.fill(x1 - 1, f - 2, z1 - 1, x2 + 1, f + 10, z2 + 1, "minecraft:blackstone");
        b.clear(x1, f + 1, z1, x2, f + 9, z2);
        b.fill(x1, f, z1, x2, f, z2, "minecraft:smooth_basalt");
        b.walls(x1, f + 1, z1, x2, f + 2, z2, "minecraft:basalt[axis=y]");
        // Lava channels along the east and west walls, stopping short of the lever's end.
        b.fill(x1 + 1, f - 1, z1 + 2, x1 + 4, f - 1, z2 - 12, "minecraft:magma_block");
        b.fill(x1 + 1, f, z1 + 2, x1 + 4, f, z2 - 12, "minecraft:lava");
        b.fill(x2 - 4, f - 1, z1 + 2, x2 - 1, f - 1, z2 - 12, "minecraft:magma_block");
        b.fill(x2 - 4, f, z1 + 2, x2 - 1, f, z2 - 12, "minecraft:lava");
        for (int x = x1 + 8; x <= x2 - 8; x += 12) {
            for (int z = z1 + 6; z <= z2 - 6; z += 12) {
                b.set(x, f + 10, z, "minecraft:shroomlight");
            }
        }
        for (int[] bone : new int[][]{{2262, 4706}, {2281, 4704}, {2265, 4690}, {2280, 4688}, {2268, 4709}}) {
            b.set(StructureDef.bx(bone[0]), f + 1, StructureDef.bz(bone[1]), "minecraft:bone_block[axis=x]");
            b.set(StructureDef.bx(bone[0]) + 1, f + 1, StructureDef.bz(bone[1]), "minecraft:skeleton_skull[rotation=4]");
        }
        b.set(StructureDef.bx(KBD_STONE_TILE[0]), f + 1, StructureDef.bz(KBD_STONE_TILE[1]), "runecraft:king_black_dragon_summoning_stone");
        Lever back = lever("kbd_lair");
        b.set(StructureDef.bx(back.x()), f + 1, StructureDef.bz(back.y()), "minecraft:lever[face=floor,facing=north]");
        return b.build();
    }

    /** Block (x, floor + 1, z) where a traveller lands in the KBD lair: the lever's tile, beside the lever block. */
    public static int[] kbdLairArrival() {
        Lever lair = lever("kbd_lair");
        return new int[]{StructureDef.bx(lair.x()) + 1, KBD_LAIR_FLOOR + 1, StructureDef.bz(lair.y()) + 1};
    }

    /** The Lava Maze's KBD ladder tile (EasternWilderness.lavaMaze: its ruin, 16 tiles east of OSRS's ladder at 3017, 3850). */
    public static final int[] LAVA_MAZE_LADDER_TILE = {3033, 3851};
    /** Floor block height at the foot of that ladder's shaft (the Lava Maze's ground 64, minus 7). */
    public static final int KBD_ROOM_FLOOR = StructureDef.GROUND - 7;

    /** Centre block (x, z) of the Lava Maze ladder's 3x3 shaft. */
    private static int[] shaft() {
        return new int[]{StructureDef.bx(LAVA_MAZE_LADDER_TILE[0]) + 1, StructureDef.bz(LAVA_MAZE_LADDER_TILE[1]) + 1};
    }

    /**
     * The King Black Dragon Lair's entrance chamber (OSRS: the room with the lever at 3067, 10253 at the foot of the Lava Maze
     * ladder): a small stone chamber dug east off the foot of this world's Lava Maze KBD ladder, two blocks high (its ceiling
     * stays under the Lava Maze's levelled ground), with the lever at its east end. It never touches the ladder's shaft.
     */
    public static StructureDef kbdLeverRoom() {
        int[] c = shaft();
        int f = KBD_ROOM_FLOOR;
        StructureDef.Builder b = StructureDef.builder("wild_kbd_lever_room", "King Black Dragon Lair entrance");
        b.fill(c[0] + 2, f - 1, c[1] - 3, c[0] + 10, f + 3, c[1] + 3, "minecraft:polished_blackstone_bricks");
        b.clear(c[0] + 3, f + 1, c[1] - 2, c[0] + 9, f + 2, c[1] + 2);
        b.clear(c[0] + 2, f + 1, c[1], c[0] + 2, f + 2, c[1]);  // the doorway into the ladder's shaft
        b.fill(c[0] + 3, f, c[1] - 2, c[0] + 9, f, c[1] + 2, "minecraft:blackstone");
        b.set(c[0] + 9, f + 2, c[1] - 2, "minecraft:soul_lantern[hanging=false]");
        b.set(c[0] + 9, f + 2, c[1] + 2, "minecraft:soul_lantern[hanging=false]");
        Lever lever = lever("kbd_lair_entrance");
        b.set(StructureDef.bx(lever.x()) + 1, f + 1, c[1], "minecraft:lever[face=floor,facing=east]");
        return b.build();
    }

    /** Block (x, z) of the entrance chamber's lever (tile {@code kbd_lair_entrance}). */
    public static int[] kbdLeverBlock() {
        Lever lever = lever("kbd_lair_entrance");
        return new int[]{StructureDef.bx(lever.x()) + 1, shaft()[1]};
    }

    /** Block (x, feet y, z) where a traveller from the lair lands in the entrance chamber: two blocks west of its lever. */
    public static int[] kbdRoomArrival() {
        int[] l = kbdLeverBlock();
        return new int[]{l[0] - 2, KBD_ROOM_FLOOR + 1, l[1]};
    }
}
