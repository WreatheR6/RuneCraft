package com.runecraft.osrs.content.dungeons;

import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Two small dungeons carved from the cache like {@link CacheDungeons}: the Shaman Caves under the Kharazi Jungle (Legends'
 * Quest) with their death wings, and the Clock Tower dungeon's rat pen with its dungeon rats. Each keeps its cache tile
 * layout under one shift that puts the cache landing under the real surface entrance (skretzo/shortest-path transports);
 * the rooms are boxes around the spawns and the passages between them are approximate.
 */
final class SmallCacheDungeons {
    static final int RESPAWN = 25; // approx

    /** Shaman Caves: the mossy rock (2781, 2935) leads to the cache tile (2773, 9341). */
    static final int SHAMAN_DX = 2781 - 2773;
    static final int SHAMAN_DY = 2935 - 9341;
    /** The death wings' cache tiles (wiki LocLine, mapID 10174; the cache has the same 8). */
    static final int[] DEATH_WINGS = {2796, 9304, 2799, 9308, 2800, 9301, 2803, 9296, 2806, 9292, 2808, 9308, 2811, 9296, 2811, 9301};

    /** Clock Tower dungeon: the cave entrance (2587, 3237) leads to the cache tile (2570, 9682). */
    static final int CLOCK_DX = 2587 - 2570;
    static final int CLOCK_DY = 3237 - 9682;
    /** The dungeon rats' cache tiles: the rat pen's 11 and the one in the blue cog's cell. */
    static final int[] DUNGEON_RATS = {2579, 9655, 2581, 9656, 2581, 9659, 2583, 9659, 2584, 9656, 2586, 9659, 2587, 9656,
            2589, 9654, 2589, 9658, 2591, 9656, 2591, 9659, 2573, 9630};

    private SmallCacheDungeons() {
    }

    static List<Dungeons.Dungeon> all() {
        return List.of(shamanCaves(), clockTowerDungeon());
    }

    /**
     * The Shaman Caves' upper floor: the landing below the mossy rock, Ungadulu's cavern north of it (up to the climbing
     * rocks and the well-stacked rocks, 2760-2765, 9376-9389) and the death wings' cavern to the south-east. In OSRS the
     * caves need Legends' Quest started and the mossy rock 50 Agility (systems module's gates); the deeper Viyeldi caves
     * are not built. The mossy rock sits on the jungle's western shore, which the mod's coast puts in the sea, so the hut
     * stands 9 tiles east of it (approx).
     */
    static Dungeons.Dungeon shamanCaves() {
        Cave c = new Cave("dng_shaman_caves", "Shaman Caves", mc("mossy_cobblestone"), mc("coarse_dirt"), mc("ochre_froglight"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, SHAMAN_DX, SHAMAN_DY);
        int f = 16;
        int[] landing = {2766, 9334, 2782, 9348};
        int[] north = {2760, 9360, 2782, 9392};
        int[] wings = {2792, 9288, 2815, 9312};
        int[][] rooms = {landing, north, wings};
        for (int[] r : rooms) {
            s.room(r, f, 6);
        }
        s.hallZ(2773, 9348, 2773, 9360, f);
        s.hall(2782, 9341, 2800, 9312, f);
        c.carve();
        // Ungadulu's fire wall (approx: a ring of fire in the north cavern) and the cavern's rocks.
        c.fill(s.x(2769), f + 1, s.z(9372), s.x(2775), f + 1, s.z(9372), mc("magma_block"));
        c.fill(s.x(2769), f + 1, s.z(9366), s.x(2775), f + 1, s.z(9366), mc("magma_block"));
        for (int[] p : new int[][]{{2796, 9294}, {2809, 9305}, {2764, 9384}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 6, mc("mossy_cobblestone"));
        }
        c.entrance(s.x(2782), s.z(9341), f, mc("mossy_cobblestone"), mc("coarse_dirt"), 'e');
        s.camps(rooms, f, RESPAWN, "death_wing", DEATH_WINGS);
        return c.build();
    }

    /**
     * The Clock Tower dungeon (Clock Tower quest): the landing below the cave entrance south-east of the tower, the
     * corridor to the rat pen north-west of the tile room (where the white cog lies) and the blue cog's cell with its
     * lone rat. In OSRS the pen and the cell are separate zones reached from other ladders, and the pen opens with two
     * levers; here one passage joins them (approx). The cogs, tiles and prison cells are not built. The hut stands 4 tiles
     * east of the real cave entrance, clear of the world module's Clock Tower grounds (approx).
     */
    static Dungeons.Dungeon clockTowerDungeon() {
        Cave c = new Cave("dng_clock_tower_dungeon", "Clock Tower dungeon", mc("stone_bricks"), mc("cobblestone"), mc("lantern"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, CLOCK_DX, CLOCK_DY);
        int f = 16;
        int[] landing = {2565, 9677, 2578, 9687};
        int[] pen = {2577, 9652, 2593, 9661};
        int[] cell = {2570, 9627, 2576, 9633};
        int[][] rooms = {landing, pen, cell};
        for (int[] r : rooms) {
            s.room(r, f, 5);
        }
        s.hallZ(2570, 9677, 2585, 9661, f);
        s.hallZ(2580, 9652, 2573, 9633, f);
        c.carve();
        // The white cog's corner of the pen (approx: a cog-coloured block).
        c.set(s.x(2592), f + 1, s.z(9660), mc("white_concrete"));
        c.entrance(s.x(2574), s.z(9682), f, mc("stone_bricks"), mc("cobblestone"), 'e');
        s.camps(rooms, f, RESPAWN, "dungeon_rat", DUNGEON_RATS);
        return c.build();
    }
}
