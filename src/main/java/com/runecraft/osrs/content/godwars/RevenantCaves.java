package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Revenant Caves (OSRS Wiki "Revenant Caves", the cavern/crevice/stairs/pillar/trapdoor pages and skretzo/shortest-path's
 * transports), carved at their real tiles under the Wilderness: OSRS counts an underground tile (x, y) as Wilderness level
 * {@code (y - 9920) / 8 + 1}, the level of surface tile (x, y - 6400), so the caves lie under that surface tile and the
 * Wilderness level comes out as in the game (17 by the southern landing, 40 by the northern one). The floor plan is the
 * cache's ({@link RevenantCaveMap}); the floor is deep (y {@value #FLOOR}) so the Wilderness Slayer Cave can lie above
 * the eastern part.
 *
 * <ul>
 *   <li>Three one-way ways in: the caverns in level 17 (east of the Dark Warriors' Fortress) and level 40 (south-east of the
 *   Lava Maze) and the crevice in level 26 (north of the Bandit Camp). Each asks a 100,000 coin fee once (from the
 *   inventory, else the bank); dying in the caves, or to another player anywhere in the Wilderness, loses it.</li>
 *   <li>Two one-way ways out: the southern and northern stairs, to the trapdoors in level 17 and level 36.</li>
 *   <li>Five Agility pillars: easy (65), three medium (75) and hard (89).</li>
 *   <li>Thirty revenants on the wiki's spawn tiles, the Emblem Trader, and the eleven spots the maledictus may appear at.</li>
 * </ul>
 */
public final class RevenantCaves {
    private RevenantCaves() {
    }

    /** Surface tile row above an underground tile row: OSRS's 6,400-tile offset. */
    public static final int DY = -6400;
    /** Floor block height (the first air block is one above). approx: deep enough to stay under the Wilderness Slayer Cave */
    public static final int FLOOR = 4;
    /** Air above the floor. */
    public static final int HEIGHT = 6;
    /** The entry fee (coins). */
    public static final int FEE = 100_000;

    public static final String BLOCK_CAVERN = "rev_cave_cavern";
    public static final String BLOCK_CREVICE = "rev_cave_crevice";
    public static final String BLOCK_STAIRS = "rev_cave_stairs";
    public static final String BLOCK_PILLAR = "rev_cave_pillar";
    public static final String BLOCK_TRAPDOOR = "rev_cave_trapdoor";

    // ------------------------------------------------------------------ the floor plan

    /** True if a player can stand on the cave tile. */
    public static boolean walkable(int x, int y) {
        int j = RevenantCaveMap.TOP - y;
        int i = x - RevenantCaveMap.X0;
        if (j < 0 || j >= RevenantCaveMap.ROWS.length || i < 0 || i >= RevenantCaveMap.ROWS[j].length()) {
            return false;
        }
        return RevenantCaveMap.ROWS[j].charAt(i) == 'o';
    }

    /** West block column of a cave tile. */
    public static int blockX(int x) {
        return StructureDef.bx(x);
    }

    /** North block row of a cave tile (under the surface tile 6,400 rows south). */
    public static int blockZ(int y) {
        return StructureDef.bz(y + DY);
    }

    /** Feet height of a player standing in the caves. */
    public static int feetY() {
        return FLOOR + 1;
    }

    /** The cave tile at a block column, or null when the block isn't over the cave map. */
    public static int[] tileAt(int blockX, int blockZ) {
        int x = com.runecraft.osrs.GielinorMap.toOsrsX(blockX);
        int y = com.runecraft.osrs.GielinorMap.toOsrsY(blockZ) - DY;
        return new int[]{x, y};
    }

    /** True if the block is in the caves' air (or their floor). */
    public static boolean contains(int blockX, int blockY, int blockZ) {
        if (blockY < FLOOR || blockY > FLOOR + HEIGHT) {
            return false;
        }
        int[] t = tileAt(blockX, blockZ);
        return walkable(t[0], t[1]);
    }

    /** The caves' block box {x1, z1, x2, z2}. */
    public static int[] blockBox() {
        int x1 = blockX(RevenantCaveMap.X0);
        int x2 = blockX(RevenantCaveMap.X0 + RevenantCaveMap.ROWS[0].length() - 1) + 1;
        int z1 = blockZ(RevenantCaveMap.TOP);
        int z2 = blockZ(RevenantCaveMap.TOP - RevenantCaveMap.ROWS.length + 1) + 1;
        return new int[]{x1, z1, x2, z2};
    }

    // ------------------------------------------------------------------ ways in and out

    /** A one-way entrance: its surface tiles {x1, y1, x2, y2}, the cave tile it lands on, and the Wilderness level. */
    public record Entrance(String id, String name, String block, int level, int x1, int y1, int x2, int y2, int toX, int toY) {
        public boolean at(int x, int y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    public static final List<Entrance> ENTRANCES = List.of(
            new Entrance("cavern_17", "Cavern (level 17)", BLOCK_CAVERN, 17, 3073, 3653, 3076, 3654, 3197, 10056),
            new Entrance("crevice_26", "Crevice (level 26)", BLOCK_CREVICE, 26, 3066, 3740, 3069, 3743, 3187, 10127),
            new Entrance("cavern_40", "Cavern (level 40)", BLOCK_CAVERN, 40, 3124, 3831, 3126, 3834, 3241, 10233));

    /** A one-way exit: the stairs' cave tile and the trapdoor's surface tile it leads to. */
    public record Exit(String id, int x, int y, int toX, int toY) {
    }

    public static final List<Exit> EXITS = List.of(
            new Exit("south", 3218, 10058, 3103, 3655),
            new Exit("north", 3244, 10215, 3122, 3805));

    /** The entrance whose surface tiles hold (x, y), or null. */
    public static Entrance entranceAt(int x, int y) {
        for (Entrance e : ENTRANCES) {
            if (e.at(x, y)) {
                return e;
            }
        }
        return null;
    }

    public static Entrance entrance(String id) {
        for (Entrance e : ENTRANCES) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }

    /** The exit stairs on or next to cave tile (x, y), or null. */
    public static Exit exitAt(int x, int y) {
        for (Exit e : EXITS) {
            if (Math.abs(e.x() - x) <= 1 && Math.abs(e.y() - y) <= 1) {
                return e;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Agility pillars (wiki "Pillar (Revenant Caves)")

    /** A pillar shortcut: the pillar's tile, the floor tiles at its two ends, the level and XP (fail XP approx). */
    public record Pillar(String id, int level, double xp, double failXp, int px, int py, int ax, int ay, int bx, int by) {
    }

    public static final List<Pillar> PILLARS = List.of(
            new Pillar("easy", 65, 6.5, 3.5, 3220, 10086, 3220, 10089, 3220, 10083),
            new Pillar("medium_west_ork", 75, 7.5, 3.8, 3200, 10136, 3197, 10136, 3203, 10136),
            new Pillar("medium_north", 75, 7.5, 3.8, 3202, 10196, 3199, 10196, 3205, 10196),
            new Pillar("medium_north_west", 75, 7.5, 3.8, 3180, 10209, 3180, 10212, 3180, 10206),
            new Pillar("hard", 89, 8.9, 4.5, 3241, 10145, 3238, 10145, 3244, 10145));

    /** Fewest and most damage a failed jump deals (wiki: 4-11; the player still crosses). */
    public static final int FAIL_MIN = 4;
    public static final int FAIL_MAX = 11;

    /** The pillar on or beside cave tile (x, y) (its block or an end), or null. */
    public static Pillar pillarAt(int x, int y) {
        for (Pillar p : PILLARS) {
            if ((Math.abs(p.px() - x) <= 1 && Math.abs(p.py() - y) <= 1) || (p.ax() == x && p.ay() == y) || (p.bx() == x && p.by() == y)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Chance of failing a pillar jump at an Agility level. approx: the wiki only says it can be failed; 15% at the
     * requirement, falling by 1% a level to 2%.
     */
    public static double failChance(Pillar p, int agility) {
        return Math.max(0.02, 0.15 - 0.01 * Math.max(0, agility - p.level()));
    }

    // ------------------------------------------------------------------ spawns (the wiki's map pins, two per area)

    /** A revenant's spawn tile. */
    public record Spawn(String creature, int x, int y) {
    }

    public static final List<Spawn> SPAWNS = List.of(
            new Spawn("revenant_imp", 3212, 10196), new Spawn("revenant_imp", 3218, 10188),
            new Spawn("revenant_imp", 3197, 10070), new Spawn("revenant_imp", 3203, 10075),
            new Spawn("revenant_goblin", 3221, 10065), new Spawn("revenant_goblin", 3226, 10074),
            new Spawn("revenant_pyrefiend", 3169, 10158), new Spawn("revenant_pyrefiend", 3184, 10151),
            new Spawn("revenant_pyrefiend", 3248, 10152), new Spawn("revenant_pyrefiend", 3254, 10141),
            new Spawn("revenant_hobgoblin", 3240, 10096), new Spawn("revenant_hobgoblin", 3246, 10104),
            new Spawn("revenant_cyclops", 3167, 10195), new Spawn("revenant_cyclops", 3175, 10183),
            new Spawn("revenant_hellhound", 3240, 10160), new Spawn("revenant_hellhound", 3249, 10175),
            new Spawn("revenant_demon", 3152, 10112), new Spawn("revenant_demon", 3169, 10115),
            new Spawn("revenant_demon", 3179, 10201), new Spawn("revenant_demon", 3186, 10191),
            new Spawn("revenant_ork", 3216, 10128), new Spawn("revenant_ork", 3230, 10133),
            new Spawn("revenant_ork", 3207, 10093), new Spawn("revenant_ork", 3221, 10095),
            new Spawn("revenant_dark_beast", 3200, 10166), new Spawn("revenant_dark_beast", 3214, 10164),
            new Spawn("revenant_knight", 3209, 10216), new Spawn("revenant_knight", 3228, 10221),
            new Spawn("revenant_dragon", 3230, 10201), new Spawn("revenant_dragon", 3244, 10198));

    /** Where the Emblem Trader wanders (wiki: around 3196, 10224). */
    public static final int[] EMBLEM_TRADER = {3196, 10224};
    /** The caves' Emblem Trader (the NPC module's one in Edgeville only skulls). */
    public static final String EMBLEM_TRADER_NPC = "rev_emblem_trader";
    /** His place: the surface tile above his spot (style NONE; he stands in the caves below it). */
    public static final String EMBLEM_TRADER_PLACE = "revenant_caves_trader";

    /** A spot the revenant maledictus may appear at, and which part of the caves the announcement names. */
    public record MaledictusSpot(String name, String part, int x, int y) {
    }

    /** Revenants/Strategies: the eleven possible spawn locations of the maledictus. */
    public static final List<MaledictusSpot> MALEDICTUS_SPOTS = List.of(
            new MaledictusSpot("Revenant dragon chamber", "northern", 3238, 10200),
            new MaledictusSpot("Revenant demon and cyclopes chamber", "northern", 3177, 10194),
            new MaledictusSpot("Northern revenant imp chamber", "northern", 3215, 10195),
            new MaledictusSpot("Revenant hellhound chamber", "middle", 3246, 10171),
            new MaledictusSpot("Eastern revenant pyrefiend chamber", "middle", 3252, 10144),
            new MaledictusSpot("Western revenant pyrefiend chamber", "middle", 3175, 10154),
            new MaledictusSpot("Revenant dark beast chamber", "middle", 3207, 10165),
            new MaledictusSpot("Northern revenant ork chamber", "middle", 3225, 10132),
            new MaledictusSpot("Southern revenant ork chamber", "southern", 3215, 10096),
            new MaledictusSpot("Revenant demon chamber", "southern", 3162, 10114),
            new MaledictusSpot("Outside the revenant demon chamber", "southern", 3188, 10117));

    // ------------------------------------------------------------------ the structure

    private static final String WALL = "minecraft:deepslate";
    private static final String FLOOR_BLOCK = "minecraft:cobbled_deepslate";
    private static final String FLOOR_ALT = "minecraft:tuff";
    private static final String LIGHT = "minecraft:sea_lantern";

    /** One row run of floor tiles {x1, x2, y}. */
    static List<int[]> runs() {
        List<int[]> out = new ArrayList<>();
        String[] r = RevenantCaveMap.ROWS;
        for (int j = 0; j < r.length; j++) {
            int y = RevenantCaveMap.TOP - j;
            int i = 0;
            while (i < r[j].length()) {
                if (r[j].charAt(i) != 'o') {
                    i++;
                    continue;
                }
                int s = i;
                while (i < r[j].length() && r[j].charAt(i) == 'o') {
                    i++;
                }
                out.add(new int[]{RevenantCaveMap.X0 + s, RevenantCaveMap.X0 + i - 1, y});
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * The carved caves: a shell of rock round every row run of floor, the runs cleared and floored (deepslate with tuff
     * patches), sea lanterns set in the ceiling, the exit stairs, the pillars standing in their chasms (lava below the
     * gap between the ends), and the three surface entrances and two trapdoors at their real tiles.
     */
    public static StructureDef structure() {
        StructureDef.Builder b = StructureDef.builder("revenant_caves_dungeon", "Revenant Caves");
        List<int[]> runs = runs();
        int f = FLOOR;
        for (int[] run : runs) {
            b.fill(blockX(run[0]) - 1, f - 1, blockZ(run[2]) - 1, blockX(run[1]) + 2, f + HEIGHT + 1, blockZ(run[2]) + 2, WALL);
        }
        for (int[] run : runs) {
            b.clear(blockX(run[0]), f + 1, blockZ(run[2]), blockX(run[1]) + 1, f + HEIGHT, blockZ(run[2]) + 1);
            b.fill(blockX(run[0]), f, blockZ(run[2]), blockX(run[1]) + 1, f, blockZ(run[2]) + 1,
                    Math.floorMod(run[2] * 7 + run[0], 5) == 0 ? FLOOR_ALT : FLOOR_BLOCK);
            for (int x = run[0]; x <= run[1]; x++) {
                if (Math.floorMod(x, 5) == 0 && Math.floorMod(run[2], 5) == 0) {
                    b.set(blockX(x), f + HEIGHT + 1, blockZ(run[2]), LIGHT);
                }
            }
        }
        // The pillar shortcuts: a chasm of lava between the two ends (walled off by invisible barriers), the pillar block
        // standing in it on a stone column.
        for (Pillar p : PILLARS) {
            int x1 = Math.min(p.ax(), p.bx());
            int x2 = Math.max(p.ax(), p.bx());
            int y1 = Math.min(p.ay(), p.by());
            int y2 = Math.max(p.ay(), p.by());
            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    boolean end = (x == p.ax() && y == p.ay()) || (x == p.bx() && y == p.by());
                    if (end) {
                        continue;
                    }
                    b.fill(blockX(x), f - 2, blockZ(y), blockX(x) + 1, f - 2, blockZ(y) + 1, WALL);
                    b.clear(blockX(x), f, blockZ(y), blockX(x) + 1, f + HEIGHT, blockZ(y) + 1);
                    b.fill(blockX(x), f - 1, blockZ(y), blockX(x) + 1, f - 1, blockZ(y) + 1, "minecraft:lava");
                    if (x != p.px() || y != p.py()) {
                        b.fill(blockX(x), f + 1, blockZ(y), blockX(x) + 1, f + 3, blockZ(y) + 1, "minecraft:barrier");
                    }
                }
            }
            b.fill(blockX(p.px()), f - 1, blockZ(p.py()), blockX(p.px()) + 1, f, blockZ(p.py()) + 1, "minecraft:polished_deepslate");
            b.fill(blockX(p.px()), f + 1, blockZ(p.py()), blockX(p.px()) + 1, f + 1, blockZ(p.py()) + 1, "runecraft:" + BLOCK_PILLAR);
            b.fill(blockX(p.px()), f + 2, blockZ(p.py()), blockX(p.px()) + 1, f + 3, blockZ(p.py()) + 1, "minecraft:barrier");
        }
        for (Exit e : EXITS) {
            b.set(blockX(e.x()), f + 1, blockZ(e.y()), "runecraft:" + BLOCK_STAIRS);
            b.set(blockX(e.x()), f + 2, blockZ(e.y()), "runecraft:" + BLOCK_STAIRS);
        }
        return b.build();
    }

    /**
     * The surface works: each cave mouth (a ring of blackstone round the cavern or crevice block on levelled ground) and
     * each trapdoor in the ground where the stairs come out.
     */
    public static List<StructureDef> surface() {
        List<StructureDef> out = new ArrayList<>();
        for (Entrance e : ENTRANCES) {
            StructureDef.Builder b = StructureDef.builder("revenant_caves_" + e.id(), "Revenant Caves " + e.name().toLowerCase());
            b.tFlatten(e.x1() - 3, e.y1() - 3, e.x2() + 3, e.y2() + 3, "minecraft:coarse_dirt");
            // A low mound of dark rock round the mouth, open to the south, with the cavern (or crevice) blocks on its tiles.
            b.tFill(e.x1() - 1, e.y1(), e.x2() + 1, e.y2() + 1, 0, 2, "minecraft:blackstone");
            b.tFill(e.x1() - 1, e.y1(), e.x2() + 1, e.y2() + 1, 3, 3, "minecraft:polished_blackstone_slab");
            b.tClear(e.x1(), e.y1(), e.x2(), e.y2(), 0, 1);
            b.tFill(e.x1(), e.y1(), e.x2(), e.y2(), -1, -1, "minecraft:polished_blackstone");
            for (int x = e.x1(); x <= e.x2(); x++) {
                for (int y = e.y1(); y <= e.y2(); y++) {
                    b.tSetTile(x, y, 0, "runecraft:" + e.block());
                }
            }
            out.add(b.build());
        }
        for (Exit e : EXITS) {
            StructureDef.Builder b = StructureDef.builder("revenant_caves_trapdoor_" + e.id(), "Revenant Caves trapdoor (" + e.id() + ")");
            b.tFlatten(e.toX() - 2, e.toY() - 2, e.toX() + 2, e.toY() + 2, "minecraft:coarse_dirt");
            b.tSetTile(e.toX(), e.toY(), -1, "runecraft:" + BLOCK_TRAPDOOR);
            out.add(b.build());
        }
        return out;
    }

    /** Where a player who climbs an exit's stairs stands: the tile south of its trapdoor. */
    public static int[] exitArrival(Exit e) {
        return new int[]{e.toX(), e.toY() - 1};
    }

    /** Where a player lands from an entrance: the landing tile in the caves. */
    public static int[] landing(Entrance e) {
        return new int[]{e.toX(), e.toY()};
    }
}
