package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Agility courses underground: the Werewolf Agility Course below Canifis and the Penguin Agility
 * Course below the Iceberg. Each obstacle is a numbered agility obstacle block let into the floor
 * (step onto them in order, as on every course); the requirements (level, quest, ring of charos)
 * are checked by the course itself. No monsters live here.
 */
final class DungeonsAgility {
    /** Dungeons that hold only an agility course (no monster camps). */
    static final List<String> COURSES = List.of("dng_werewolf_agility_course", "dng_penguin_agility_course");

    private DungeonsAgility() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(werewolf());
        out.add(penguin());
        return out;
    }

    // ------------------------------------------------------------------ Werewolf Agility Course

    /**
     * Below the trapdoor south-east of Canifis (tile 3543, 3463, approx.): a long torch-lit cavern
     * with the stepping stones over a pool, three hurdles, the obstacle pipe, the skull-lined slope
     * up to a ledge and the deathslide back down (into a pool, beside where the Agility Trainer
     * takes the stick).
     */
    static Dungeons.Dungeon werewolf() {
        Cave c = new Cave("dng_werewolf_agility_course", "Werewolf Agility Course", mc("cobbled_deepslate"), mc("coarse_dirt"),
                mc("shroomlight"));
        int course = AgilityCourse.WEREWOLF.ordinal();
        int f = 20;
        int ex = StructureDef.bx(3543);
        int ez = StructureDef.bz(3463);
        int top = f + 6;
        c.room(ex - 5, ez - 5, ex + 5, ez + 5, f, 5);
        c.hallZ(ex, ez + 5, ex, ez + 12, f, 3, 4);
        c.room(ex - 10, ez + 12, ex + 32, ez + 40, f, 10);
        c.stairs(ex + 26, ez + 21, 's', f, top, 3, 4);                      // the skull slope
        c.room(ex + 18, ez + 29, ex + 30, ez + 36, top, 4);                 // the ledge at its top
        c.carve();
        // Stepping stones over a pool.
        c.pool(ex - 8, ez + 16, ex + 2, ez + 20, f, mc("water"));
        for (int x = ex - 7; x <= ex + 1; x += 2) {
            c.set(x, f, ez + 18, mc("mossy_cobblestone"));
        }
        c.obstacle(ex - 3, ez + 18, f, course, 0);
        // Three hurdles (a low slab beyond each).
        for (int i = 0; i < 3; i++) {
            int x = ex + 6 + 4 * i;
            c.obstacle(x, ez + 18, f, course, 1 + i);
            c.set(x + 1, f + 1, ez + 17, mc("spruce_slab")).set(x + 1, f + 1, ez + 19, mc("spruce_slab"));
        }
        // The obstacle pipe: a one-wide, two-high tube.
        c.obstacle(ex + 18, ez + 18, f, course, 4);
        c.fill(ex + 19, f + 1, ez + 16, ex + 23, f + 3, ez + 20, mc("polished_deepslate"));
        c.b.clear(ex + 19, f + 1, ez + 18, ex + 23, f + 2, ez + 18);
        // The skull slope and the deathslide from the ledge.
        c.obstacle(ex + 26, ez + 21, f, course, 5);
        c.set(ex + 24, f + 1, ez + 22, mc("skeleton_skull[rotation=4]")).set(ex + 28, f + 1, ez + 23, mc("skeleton_skull[rotation=12]"));
        c.obstacle(ex + 19, ez + 33, top, course, 6);
        c.fill(ex + 19, top + 3, ez + 33, ex + 19, top + 4, ez + 33, mc("chain"));
        c.pool(ex + 14, ez + 31, ex + 17, ez + 35, f, mc("water"));
        c.entrance(ex, ez, f, mc("spruce_planks"), mc("coarse_dirt"), 'e');
        return c.build();
    }

    // ------------------------------------------------------------------ Penguin Agility Course

    /**
     * Below the Iceberg (entrance tile 2666, 4012, approx.): an ice cavern with stepping stones over
     * freezing water, four icicles to duck past and the ice slope to slide down at the end.
     */
    static Dungeons.Dungeon penguin() {
        Cave c = new Cave("dng_penguin_agility_course", "Penguin Agility Course", mc("packed_ice"), mc("snow_block"), mc("sea_lantern"));
        int course = AgilityCourse.PENGUIN.ordinal();
        int f = 24;
        int ex = StructureDef.bx(2666);
        int ez = StructureDef.bz(4012);
        c.room(ex - 5, ez - 5, ex + 5, ez + 5, f, 5);
        c.hall(ex + 5, ez, ex + 12, ez, f, 3, 4);
        c.room(ex + 12, ez - 12, ex + 48, ez + 12, f, 7);
        c.carve();
        // Stepping stones across the water.
        c.pool(ex + 14, ez - 4, ex + 22, ez + 4, f, mc("water"));
        for (int x = ex + 14; x <= ex + 22; x += 2) {
            c.set(x, f, ez, mc("packed_ice"));
        }
        c.obstacle(ex + 18, ez, f, course, 0);
        // Four icicles hanging over the way.
        for (int i = 0; i < 4; i++) {
            int x = ex + 26 + 4 * i;
            c.obstacle(x, ez, f, course, 1 + i);
            c.set(x, f + 7, ez - 1, mc("pointed_dripstone[vertical_direction=down,thickness=tip]"));
            c.set(x, f + 7, ez + 1, mc("pointed_dripstone[vertical_direction=down,thickness=tip]"));
        }
        // The ice slope.
        c.fill(ex + 42, f, ez - 1, ex + 46, f, ez + 1, mc("blue_ice"));
        c.obstacle(ex + 46, ez, f, course, 5);
        c.entrance(ex, ez, f, mc("packed_ice"), mc("snow_block"), 'w');
        return c.build();
    }
}
