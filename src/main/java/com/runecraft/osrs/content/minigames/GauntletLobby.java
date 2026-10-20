package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.world.StructureDef;

/**
 * The Gauntlet's lobby (OSRS plane 1, reached only through the Gauntlet Portal in north-west Prifddinas): a sealed crystal
 * hall at its real tiles, built underground below them since the lobby is not part of the surface map. The Gauntlet Portal
 * (3228, 6116) leads to 3032, 6125; the Teleport Platform (3032, 6123) leads back (skretzo/shortest-path transports). The
 * object tiles come from the cache dump (mejrs/data_osrs: scoreboard, deposit box, platform, both bookstands) and the wiki
 * (Reward Chest pin 3027, 6123). The two entrances' tiles are not in either source (approx: side by side in the north wall).
 */
public final class GauntletLobby {
    private GauntletLobby() {
    }

    public static final String STRUCTURE = "gauntlet_lobby";

    /** Where the Gauntlet Portal puts a player (OSRS 3032, 6125). */
    public static final int ARRIVE_X = 3032;
    public static final int ARRIVE_Y = 6125;
    /** The Gauntlet Portal in Prifddinas (wiki map 3228, 6116), where the lobby's Teleport Platform leads back to. */
    public static final int PORTAL_X = 3228;
    public static final int PORTAL_Y = 6116;

    public static final int[] PLATFORM = {3032, 6123};
    public static final int[] REWARD_CHEST = {3027, 6123};
    public static final int[] RECIPES = {3031, 6118};
    public static final int[] EGNIOL_BOOK = {3034, 6118};
    public static final int[] SCOREBOARD = {3039, 6122};
    public static final int[] DEPOSIT_BOX = {3038, 6129};
    /** approx: the entrances' tiles are unknown. */
    public static final int[] ENTRANCE = {3030, 6131};
    public static final int[] CORRUPTED_ENTRANCE = {3035, 6131};

    /** The hall's tiles (inclusive) and its first air block (the floor is one below). */
    public static final int X1 = 3024;
    public static final int Y1 = 6114;
    public static final int X2 = 3041;
    public static final int Y2 = 6132;
    public static final int GROUND = 46;
    public static final int HEIGHT = 7;

    public static StructureDef structure() {
        StructureDef.Builder b = StructureDef.builder(STRUCTURE, "The Gauntlet (lobby)").ground(GROUND);
        // A solid shell, then the hall carved out of it: sealed from caves, water and the surface above.
        b.tFill(X1 - 1, Y1 - 1, X2 + 1, Y2 + 1, -2, HEIGHT, "minecraft:calcite");
        b.tClear(X1, Y1, X2, Y2, 0, HEIGHT - 1);
        b.tFloor(X1, Y1, X2, Y2, "minecraft:polished_diorite");
        // The crystal mural around the platform and a path to the entrances.
        b.tFloor(3029, 6120, 3035, 6126, "minecraft:smooth_quartz");
        b.tFloor(3031, 6122, 3033, 6124, "minecraft:prismarine_bricks");
        b.tFloor(3029, 6127, 3036, 6132, "minecraft:smooth_quartz");
        // Crystal pillars in the corners and lights in the ceiling.
        for (int[] c : new int[][]{{X1, Y1}, {X2, Y1}, {X1, Y2}, {X2, Y2}}) {
            b.tFill(c[0], c[1], c[0], c[1], 0, HEIGHT - 1, "minecraft:quartz_pillar");
        }
        for (int x = X1 + 2; x <= X2 - 1; x += 4) {
            for (int y = Y1 + 2; y <= Y2 - 1; y += 4) {
                b.tSet(x, y, HEIGHT - 1, "minecraft:sea_lantern");
            }
        }
        // The entrances' doorways: crystal frames in the north wall.
        for (int[] e : new int[][]{ENTRANCE, CORRUPTED_ENTRANCE}) {
            b.tFill(e[0] - 1, e[1], e[0] - 1, e[1], 0, 3, "minecraft:quartz_pillar");
            b.tFill(e[0] + 1, e[1], e[0] + 1, e[1], 0, 3, "minecraft:quartz_pillar");
            b.tFill(e[0] - 1, e[1], e[0] + 1, e[1], 4, 4, e == ENTRANCE ? "minecraft:light_blue_stained_glass"
                    : "minecraft:red_stained_glass");
        }
        b.tSet(ENTRANCE[0], ENTRANCE[1], 0, rc(Minigame.GAUNTLET.entranceBlock()));
        b.tSet(CORRUPTED_ENTRANCE[0], CORRUPTED_ENTRANCE[1], 0, rc(Gauntlet.CORRUPTED_ENTRANCE));
        b.tSet(PLATFORM[0], PLATFORM[1], 0, rc(Gauntlet.TELEPORT_PLATFORM));
        b.tSet(REWARD_CHEST[0], REWARD_CHEST[1], 0, rc(Gauntlet.REWARD_CHEST));
        b.tSet(RECIPES[0], RECIPES[1], 0, rc(Gauntlet.RECIPES_BOOK));
        b.tSet(EGNIOL_BOOK[0], EGNIOL_BOOK[1], 0, rc(Gauntlet.EGNIOL_BOOK));
        b.tSet(SCOREBOARD[0], SCOREBOARD[1], 0, rc(Gauntlet.SCOREBOARD));
        b.tSet(DEPOSIT_BOX[0], DEPOSIT_BOX[1], 0, rc("bank_deposit_box"));
        return b.build();
    }

    private static String rc(String id) {
        return "runecraft:" + id;
    }

    /** First block X / Z of a lobby tile and the y of the lobby's first air block (for the Minecraft side). */
    public static int blockX(int osrsX) {
        return StructureDef.bx(osrsX);
    }

    public static int blockZ(int osrsY) {
        return StructureDef.bz(osrsY);
    }
}
