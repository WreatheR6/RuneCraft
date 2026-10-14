package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * The Rune essence mine: a sealed mine reached only by being teleported there after Rune Mysteries by Aubury (Varrock),
 * Archmage Sedridor (Wizards' Tower basement), Wizard Distentor (Wizards' Guild, Yanille), Wizard Cromperty (East
 * Ardougne) or Brimstail (Tree Gnome Stronghold). Its four outcrops are rune essence rocks, which give rune essence
 * below 30 Mining and pure essence from 30 ({@link com.runecraft.osrs.data.Rock#minedItem}); the portal by each outcrop
 * returns the player to the NPC who sent them.
 *
 * <p>Like the Blast Furnace, the mine sits at its OSRS instance tiles (map square 45, 75: the hub around 2911, 4832
 * and four lobes), carved deep under the sea there so the sea bed is untouched. The outline follows the cache map
 * tile (mejrs/layers_osrs); the lobes are boxes and the passages 3 tiles wide (approx).
 */
public final class EssenceMine {
    /** Structure id (built by the dungeons module, not a {@link Dungeons.Dungeon}: the mine has no entrance). */
    public static final String ID = "dng_rune_essence_mine";
    /** Floor block height (the room tops stay below {@code TerrainAnchors.DEEP_UNDERGROUND_BELOW}). */
    public static final int FLOOR = 20;
    public static final int HEIGHT = 7;
    /** Where the teleport lands: the middle of the hub (OSRS tile). */
    public static final int ARRIVE_X = 2911;
    public static final int ARRIVE_Y = 4832;
    /** The mine's OSRS tile bounds (inclusive). */
    public static final int X1 = 2880;
    public static final int Y1 = 4800;
    public static final int X2 = 2943;
    public static final int Y2 = 4863;

    /** The incantation every teleporter shouts. */
    public static final String INCANTATION = "Senventior disthine molenko!";

    /**
     * The NPCs that teleport players to the mine (NPC id as the game stores it: a {@code QuestNpc} name or a module
     * {@code NpcDef} id) and their names.
     */
    public static final Map<String, String> TELEPORTERS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("AUBURY", "Aubury");
        m.put("SEDRIDOR", "Archmage Sedridor");
        m.put("npc_wizard_distentor", "Wizard Distentor");
        m.put("wizard_cromperty", "Wizard Cromperty");
        m.put("brimstail", "Brimstail");
        TELEPORTERS = java.util.Collections.unmodifiableMap(m);
    }

    /** Quest that must be complete before any of them will teleport you. */
    public static final String QUEST = "RUNE_MYSTERIES";

    /** Room boxes {x1, y1, x2, y2} in OSRS tiles: the hub and the four lobes (from the cache map tile, approx). */
    static final int[] HUB = {2905, 4826, 2920, 4840};
    static final int[] NW = {2884, 4841, 2901, 4858};
    static final int[] NE = {2920, 4840, 2939, 4859};
    static final int[] SW = {2885, 4806, 2900, 4826};
    static final int[] SE = {2918, 4805, 2937, 4824};
    static final List<int[]> ROOMS = List.of(HUB, NW, NE, SW, SE);

    /** The four outcrops (OSRS tiles of their south-west corners; each is 3x3 tiles of rune essence rock, approx). */
    public static final List<int[]> OUTCROPS = List.of(new int[]{2895, 4853}, new int[]{2929, 4843}, new int[]{2887, 4813},
            new int[]{2930, 4818});
    /** The portal out beside each outcrop (OSRS tiles, approx). */
    public static final List<int[]> PORTALS = List.of(new int[]{2899, 4850}, new int[]{2926, 4846}, new int[]{2891, 4817},
            new int[]{2927, 4821});

    private EssenceMine() {
    }

    /** True if an OSRS tile and block height lie inside the mine. */
    public static boolean contains(int osrsX, int osrsY, int blockY) {
        return osrsX >= X1 && osrsX <= X2 && osrsY >= Y1 && osrsY <= Y2 && blockY > FLOOR - 3 && blockY <= FLOOR + HEIGHT + 2;
    }

    /** True if an OSRS tile is walkable mine floor (inside a room). */
    public static boolean inRoom(int osrsX, int osrsY) {
        for (int[] r : ROOMS) {
            if (osrsX >= r[0] && osrsX <= r[2] && osrsY >= r[1] && osrsY <= r[3]) {
                return true;
            }
        }
        return false;
    }

    public static StructureDef structure() {
        Cave c = new Cave(ID, "Rune essence mine", mc("stone"), mc("smooth_stone"), mc("sea_lantern"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, 0, 0);
        int f = FLOOR;
        for (int[] r : ROOMS) {
            s.room(r, f, HEIGHT);
        }
        // Passages from the hub's corners out to each lobe.
        s.hall(2906, 4839, 2896, 4842, f);
        s.hall(2919, 4839, 2926, 4841, f);
        s.hall(2906, 4827, 2894, 4825, f);
        s.hall(2919, 4827, 2925, 4823, f);
        c.carve();
        String rock = rc("rune_essence_rock");
        for (int[] o : OUTCROPS) {
            // A 3x3 outcrop, two blocks high in the middle.
            c.fill(s.x(o[0]), f + 1, s.z(o[1] + 2), s.x(o[0] + 2) + 1, f + 1, s.z(o[1]) + 1, rock);
            c.fill(s.x(o[0] + 1), f + 2, s.z(o[1] + 1), s.x(o[0] + 1) + 1, f + 2, s.z(o[1] + 1) + 1, rock);
        }
        for (int[] p : PORTALS) {
            c.set(s.x(p[0]), f + 1, s.z(p[1]), rc(DungeonBlocks.ESSENCE_PORTAL));
            c.set(s.x(p[0]), f + 2, s.z(p[1]), rc(DungeonBlocks.ESSENCE_PORTAL));
        }
        // A few pale crystals around the hub, as in OSRS.
        for (int[] p : new int[][]{{2908, 4829}, {2917, 4837}, {2909, 4836}, {2916, 4830}}) {
            c.set(s.x(p[0]), f + 1, s.z(p[1]), mc("amethyst_cluster"));
        }
        return c.b.build();
    }
}
