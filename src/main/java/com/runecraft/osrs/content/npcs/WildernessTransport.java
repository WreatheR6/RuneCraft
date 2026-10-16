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
 *   Mage Bank lever lead to each other. Teleport Block stops them; the Wilderness level does not.</li>
 *   <li>Obelisks (levels 13, 19, 27, 35, 44 and 50): four pillars around a 3x3 platform. Activating any pillar starts the
 *   obelisk; a few moments later everyone standing on the platform is sent to the same random other obelisk, keeping
 *   their place on the platform.</li>
 * </ul>
 *
 * The level 13 obelisk is moved {@link #OBELISK_13_EAST_SHIFT} tiles east, clear of this world's Ferox Enclave.
 * The Mage Bank is a separate area in OSRS (tile 2539, 4712); here it is a small cavern carved at that tile, deep under
 * the sea, with its bank booths and the lever back up (see {@link #mageBank()}).
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
            new Lever("mage_bank", "Mage Bank lever", 2539, 4712, 3090, 3956, "the Mage Arena"));

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

    /** A random obelisk other than {@code from} (each of the other five equally likely). */
    public static Obelisk destination(Obelisk from, Random random) {
        List<Obelisk> others = new ArrayList<>(OBELISKS);
        others.remove(from);
        return others.get(random.nextInt(others.size()));
    }

    /** Bare places (built nothing; the Minecraft side lays the lever or obelisk when their chunk loads). */
    public static List<PlaceDef> places() {
        List<PlaceDef> out = new ArrayList<>();
        for (Lever l : LEVERS) {
            if (!l.id().equals("mage_bank")) { // the Mage Bank's lever is part of its cavern
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
        Lever back = LEVERS.get(4);
        b.set(StructureDef.bx(back.x()), f + 1, StructureDef.bz(back.y()), "minecraft:lever[face=floor,facing=north]");
        return b.build();
    }

    /** Block (x, floor + 1, z) where a traveller lands in the Mage Bank: the lever's tile, beside the lever block. */
    public static int[] mageBankArrival() {
        Lever bank = LEVERS.get(4);
        return new int[]{StructureDef.bx(bank.x()) + 1, MAGE_BANK_FLOOR + 1, StructureDef.bz(bank.y()) + 1};
    }
}
