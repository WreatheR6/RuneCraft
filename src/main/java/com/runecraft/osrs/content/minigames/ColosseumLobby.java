package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

/**
 * The lobby under the Fortis Colosseum (OSRS Wiki "Fortis Colosseum": "players will arrive at a lobby area under the
 * Colosseum itself"): a sandstone hall entered by the stairs at its west end (1799, 9506), with Minimus, Gloria and the
 * Colosseum scoreboard to the north, the Brawlers' bank chest to the south, the doorway into the arena at 1810, 9506,
 * gladiators and guards, and the shrine of Ueman, Teoki of Ralos and Seia, Teoki of Ranul to the south-east. Tiles are the
 * wiki's map pins; the hall outline is approximate (// approx: the lobby's real walls aren't in our data).
 *
 * <p>Underground tiles are carved under their surface tile ({@code y - 6400}), well below the Colosseum's floor, so the
 * terrain keeps its rock cover over the hall ({@code TerrainAnchors}).
 */
public final class ColosseumLobby {
    private ColosseumLobby() {
    }

    public static final String ID = "fortis_colosseum_lobby";
    /** The hall's floor block y; players and NPCs stand one block higher. */
    public static final int FLOOR = 40;
    public static final int HEIGHT = 8;
    public static final int FEET = FLOOR + 1;

    /** The carved halls as underground tile rectangles {x1, y1, x2, y2}: the main hall and the Teokis' shrine. */
    public static final int[][] HALLS = {{1797, 9496, 1832, 9516}, {1819, 9480, 1832, 9495}};

    /** NPC ids standing in the lobby (minigames module) and their tiles; Minimus is the zeah module's NPC. */
    public static final String GLORIA = "colosseum_gloria";
    public static final String UEMAN = "colosseum_ueman";
    public static final String SEIA = "colosseum_seia";
    public static final String GLADIATOR = "colosseum_gladiator";
    public static final String GLADIATOR_2 = "colosseum_gladiator_2";
    public static final String GUARD = "colosseum_guard";
    public static final String GUARD_2 = "colosseum_guard_2";
    /** Ueman and Seia (wiki map pins) and the gladiators and guards (the guards' own pins, 1848, lie past our hall). */
    public static final int[][] NPC_TILES = {
            {FortisColosseum.GLORIA_X, FortisColosseum.GLORIA_Y}, {1825, 9487}, {1828, 9483}, {1815, 9511}, {1818, 9500},
            {1831, 9502}, {1831, 9510}};
    public static final String[] NPC_IDS = {GLORIA, UEMAN, SEIA, GLADIATOR, GLADIATOR_2, GUARD, GUARD_2};

    /** Minimus (the zeah module's NPC), who stands in the lobby beside its own NPCs. */
    public static final String MINIMUS = FortisColosseum.MINIMUS;

    /** The feet y of an NPC standing in the lobby, or null for any other NPC ({@code NpcStoreys.undergroundFeetY}). */
    public static Integer feetY(String npcId) {
        if (npcId.equals(MINIMUS)) {
            return FEET;
        }
        for (String id : NPC_IDS) {
            if (id.equals(npcId)) {
                return FEET;
            }
        }
        return null;
    }

    /** First block x of an underground tile. */
    public static int x(int tx) {
        return StructureDef.bx(tx);
    }

    /** First block z of an underground tile (carved under its surface tile). */
    public static int z(int ty) {
        return StructureDef.bz(ty - FortisColosseum.UNDERGROUND_OFFSET);
    }

    /** The block a player or NPC stands in at an underground tile: {x, feet y, z}. */
    public static int[] stand(int tx, int ty) {
        return new int[]{x(tx), FEET, z(ty)};
    }

    /**
     * The lobby place (style NONE, above the hall) its NPCs belong to; its blocks are the ones the lobby structure carves in
     * (a style-NONE place builds nothing itself).
     */
    public static PlaceDef place() {
        return PlaceDef.of(FortisColosseum.LOBBY_PLACE, "Fortis Colosseum lobby", placeX(), placeY(), PlaceDef.Style.NONE,
                List.of(FortisColosseum.ARENA_ENTRANCE, FortisColosseum.LOBBY_EXIT, FortisColosseum.SCOREBOARD, FortisColosseum.BANK_CHEST));
    }

    /** Over the lobby's north half (1810, 9510): a tile whose block lies at a chunk centre, where places are built. */
    static int placeX() {
        return 1810;
    }

    static int placeY() {
        return 9510 - FortisColosseum.UNDERGROUND_OFFSET;
    }

    /** Block offsets from the lobby place (its chunk-centred block) to an underground tile (NpcDef dx/dz). */
    public static int[] offset(int tx, int ty) {
        PlaceDef p = place();
        return new int[]{x(tx) - p.blockX(), z(ty) - p.blockZ()};
    }

    /** The lobby's own NPCs (Minimus stays in the zeah module, moved onto this place). */
    public static List<NpcDef> npcs() {
        String[] names = {"Gloria", "Ueman, Teoki of Ralos", "Seia, Teoki of Ranul", "Gladiator", "Gladiator", "Guard", "Guard"};
        int[][] colours = {
                {0xC89870, 0xE0C060, 0x8A5A2A, 0x6A3A1A}, {0xA87850, 0xE0A030, 0xC08020, 0xF0D040},
                {0xB88860, 0x3A4A8A, 0x2A3A6A, 0xD0D0E0}, {0xC08860, 0x8A2A1A, 0x5A3A1A, 0xB0B0B8},
                {0x9A6A40, 0x8A2A1A, 0x5A3A1A, 0xB0B0B8}, {0xC89870, 0xC0A060, 0x6A4A2A, 0xC0C0C8},
                {0xB08060, 0xC0A060, 0x6A4A2A, 0xC0C0C8}};
        List<NpcDef> out = new ArrayList<>();
        for (int i = 0; i < NPC_IDS.length; i++) {
            int[] off = offset(NPC_TILES[i][0], NPC_TILES[i][1]);
            int[] c = colours[i];
            out.add(NpcDef.of(NPC_IDS[i], names[i], FortisColosseum.LOBBY_PLACE, off[0], off[1], c[0], c[1], c[2], c[3]));
        }
        return out;
    }

    private static final String SHELL = "minecraft:sandstone";
    private static final String FLOOR_BLOCK = "minecraft:smooth_sandstone";
    private static final String TRIM = "minecraft:red_sandstone";
    private static final String COLUMN = "minecraft:chiseled_sandstone";
    private static final String WALL = "minecraft:cut_sandstone";

    private static int[] box(int[] h) {
        return new int[]{x(h[0]), z(h[3]), x(h[2]) + 1, z(h[1]) + 1};
    }

    /** The carved lobby: shells first, then the halls cleared (so they join), floors, columns, lights and objects. */
    public static StructureDef structure() {
        StructureDef.Builder b = StructureDef.builder(ID, "Fortis Colosseum lobby");
        for (int[] h : HALLS) {
            int[] r = box(h);
            b.fill(r[0] - 1, FLOOR - 1, r[1] - 1, r[2] + 1, FLOOR + HEIGHT + 1, r[3] + 1, SHELL);
            b.walls(r[0] - 1, FLOOR + 1, r[1] - 1, r[2] + 1, FLOOR + 3, r[3] + 1, WALL);
        }
        // Cleared after every shell and wall face is laid, so the halls open into each other.
        for (int[] h : HALLS) {
            int[] r = box(h);
            b.clear(r[0], FLOOR + 1, r[1], r[2], FLOOR + HEIGHT, r[3]);
            b.fill(r[0], FLOOR, r[1], r[2], FLOOR, r[3], FLOOR_BLOCK);
        }
        for (int[] h : HALLS) {
            int[] r = box(h);
            // Columns along the walls and hanging lanterns down the hall.
            for (int x = r[0] + 3; x <= r[2] - 3; x += 8) {
                for (int z : new int[]{r[1], r[3]}) {
                    b.fill(x, FLOOR + 1, z, x, FLOOR + HEIGHT, z, COLUMN);
                }
                for (int z = r[1] + 4; z <= r[3] - 4; z += 8) {
                    b.set(x + 4, FLOOR + HEIGHT, z, "minecraft:lantern[hanging=true]");
                }
            }
        }
        // A red sandstone carpet from the stairs to the arena doorway (the wiki's pins sit on the 9506 row).
        int row = z(FortisColosseum.LOBBY_Y);
        b.fill(x(1797), FLOOR, row, x(FortisColosseum.ARENA_ENTRANCE_X) + 1, FLOOR, row + 1, TRIM);
        // The stairs up to the surface at the west end.
        int sx = x(1797);
        b.set(sx, FEET, row, "runecraft:" + FortisColosseum.LOBBY_EXIT);
        b.set(sx, FEET, row + 1, "minecraft:sandstone_stairs[facing=west]");
        b.fill(sx, FEET, row - 1, sx, FEET + 3, row - 1, COLUMN);
        b.fill(sx, FEET, row + 2, sx, FEET + 3, row + 2, COLUMN);
        // The doorway into the arena, framed by two columns with decorative shields (gold) and a lintel.
        int ex = x(FortisColosseum.ARENA_ENTRANCE_X);
        b.set(ex, FEET, row, "runecraft:" + FortisColosseum.ARENA_ENTRANCE);
        b.set(ex + 1, FEET, row, "runecraft:" + FortisColosseum.ARENA_ENTRANCE);
        for (int x : new int[]{ex - 1, ex + 2}) {
            b.fill(x, FEET, row, x, FEET + 3, row, COLUMN);
            b.set(x, FEET + 2, row + 1, "minecraft:gold_block");
        }
        b.fill(ex - 1, FEET + 4, row, ex + 2, FEET + 4, row, TRIM);
        // Minimus' corner, the scoreboard and the bank chest.
        b.set(x(FortisColosseum.SCOREBOARD_X), FEET, z(FortisColosseum.SCOREBOARD_Y), "runecraft:" + FortisColosseum.SCOREBOARD);
        b.set(x(FortisColosseum.BANK_CHEST_X), FEET, z(FortisColosseum.BANK_CHEST_Y), "runecraft:" + FortisColosseum.BANK_CHEST);
        // The Teokis' shrine: an altar of gold under candles.
        int ax = x(1826);
        int az = z(9481);
        b.fill(ax, FEET, az, ax + 2, FEET, az, "minecraft:smooth_sandstone_slab[type=top]");
        b.set(ax + 1, FEET + 1, az, "minecraft:gold_block");
        b.set(ax, FEET + 1, az, "minecraft:candle[lit=true,candles=3]");
        b.set(ax + 2, FEET + 1, az, "minecraft:candle[lit=true,candles=3]");
        return b.build();
    }
}
