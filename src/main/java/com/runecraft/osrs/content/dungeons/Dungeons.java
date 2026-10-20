package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.content.skilling.AgilityShortcuts;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every dungeon of the module: its structure, its entrances and its monster camps. The dungeons are
 * carved into the rock at y between about -40 and {@link #DEEP_TOP}, beneath or near their real
 * surface entrances.
 */
public final class Dungeons {
    /** Highest block a dungeon touches outside its entrances. */
    public static final int DEEP_TOP = 50;

    /**
     * An entrance column (x/z footprint, all heights up to {@code yTop}). {@code surface} entrances
     * have a hut on open ground; the others continue a ladder down through the floor of an entrance
     * pit built by another module.
     */
    public record Entrance(int x1, int z1, int x2, int z2, int yTop, boolean surface) {
        public boolean contains(StructureDef.Op op) {
            return op.x1() >= x1 && op.x2() <= x2 && op.z1() >= z1 && op.z2() <= z2 && op.y2() <= yTop;
        }
    }

    /**
     * Up to {@code cap} of the creatures are kept alive in the box (spawned with their feet at
     * {@code feetY}) while a player is near; a killed monster comes back after {@code respawnTicks}
     * game ticks (0.6 s each), as in OSRS.
     */
    public record Camp(String dungeon, int x1, int z1, int x2, int z2, int feetY, int cap, int respawnTicks,
                       List<String> creatures) {
        public int centreX() {
            return (x1 + x2) / 2;
        }

        public int centreZ() {
            return (z1 + z2) / 2;
        }
    }

    /**
     * One dungeon. {@code passages} are its lifts, doors and exits ({@link Passage}); {@code multi} are block boxes
     * {x1, y1, z1, x2, y2, z2} of the parts that are multicombat in OSRS where the rest is single-way.
     */
    public record Dungeon(String id, String name, StructureDef structure, List<Entrance> entrances, List<Camp> camps,
                          List<AgilityShortcuts.BlockEnds> shortcuts, List<Passage> passages, List<int[]> multi) {
    }

    /**
     * A lift, door or exit: using the scripted {@code block} at (x, y, z) takes the player to stand at feet block
     * (toX, toY, toZ); when {@code toY} is {@link #SURFACE} to the open ground of column (toX, toZ); when {@code arena}
     * is set, into that boss's arena (the bosses module's, {@code ArenaLayout}). {@code message} is shown on use.
     */
    public record Passage(String block, int x, int y, int z, int toX, int toY, int toZ, String arena, String message) {
        /** True if a block at (bx, by, bz) is this passage's block (or the block above it). */
        public boolean at(int bx, int by, int bz) {
            return bx == x && bz == z && (by == y || by == y + 1);
        }
    }

    /** {@link Passage#toY()} of a passage that comes out on the surface. */
    public static final int SURFACE = Integer.MIN_VALUE;

    /** An Abyss rift: using it takes the player to that rune altar (a {@code Landmark} name). */
    public record Rift(int x, int y, int z, String altar, String rune) {
    }

    private static List<Dungeon> all;

    private Dungeons() {
    }

    public static synchronized List<Dungeon> all() {
        if (all == null) {
            List<Dungeon> list = new ArrayList<>();
            list.addAll(DungeonsMisthalin.all());
            list.addAll(DungeonsAsgarnia.all());
            list.addAll(DungeonsKandarin.all());
            list.addAll(DungeonsKaramjaDesert.all());
            list.addAll(DungeonsEast.all());
            list.addAll(DungeonsZeah.all());
            list.addAll(DungeonsLairs.all());
            list.addAll(DungeonsAgility.all());
            list.add(PuroPuro.dungeon());
            list.add(WaterbirthDungeon.build());
            list.addAll(ApeAtollDungeons.all());
            list.addAll(CacheDungeons.all());
            list.addAll(SmallCacheDungeons.all());
            list.addAll(VarlamoreDungeons.all());
            list.add(PoisonWasteDungeon.dungeon());
            list.add(CharredDungeon.dungeon());
            list.add(ShayzienCrypts.dungeon());
            list.add(CamdozaalRuins.build());
            all = Collections.unmodifiableList(list);
        }
        return all;
    }

    public static List<StructureDef> structures() {
        List<StructureDef> out = new ArrayList<>();
        for (Dungeon d : all()) {
            out.add(d.structure());
        }
        return out;
    }

    public static List<Camp> camps() {
        List<Camp> out = new ArrayList<>();
        for (Dungeon d : all()) {
            out.addAll(d.camps());
        }
        return out;
    }

    /** The ends of every Agility shortcut carved into a dungeon. */
    public static List<AgilityShortcuts.BlockEnds> shortcutEnds() {
        List<AgilityShortcuts.BlockEnds> out = new ArrayList<>();
        for (Dungeon d : all()) {
            out.addAll(d.shortcuts());
        }
        return out;
    }

    /** Every dungeon passage (lifts, doors, exits). */
    public static List<Passage> passages() {
        List<Passage> out = new ArrayList<>();
        for (Dungeon d : all()) {
            out.addAll(d.passages());
        }
        return out;
    }

    /** The passage whose block is at (x, y, z), or null. */
    public static Passage passageAt(int x, int y, int z) {
        for (Dungeon d : all()) {
            for (Passage p : d.passages()) {
                if (p.at(x, y, z)) {
                    return p;
                }
            }
        }
        return null;
    }

    /** Block boxes {x1, y1, z1, x2, y2, z2} of the multicombat parts of otherwise single-way dungeons. */
    public static List<int[]> multiBoxes() {
        List<int[]> out = new ArrayList<>();
        for (Dungeon d : all()) {
            out.addAll(d.multi());
        }
        return out;
    }

    /**
     * Dungeons whose monsters can only be fought on a Slayer task, by dungeon id: the task they are known for (the gate
     * itself lets a player hurt whatever counts towards their own task, so a waterfiend task works in the Kraken Cove).
     * OSRS Wiki: the Kalphite Cave's kalphites, everything in the Kraken Cove, the Smoke Devil Dungeon and the Slayer Tower
     * basement, and the Lizardman Caves' shamans can only be attacked on a matching task.
     */
    public static final java.util.Map<String, String> TASK_ONLY = java.util.Map.of("dng_kalphite_cave", "kalphite",
            "dng_kraken_cove", "cave_kraken", "dng_smoke_devil_dungeon", "smoke_devils", "dng_lizardman_caves", "lizardmen",
            "dng_slayer_tower_basement", "nechryael");

    /** The Slayer task category needed to fight at this block (inside a {@link #TASK_ONLY} dungeon's carved air), or null. */
    public static String taskOnly(int x, int y, int z) {
        if (y > DEEP_TOP) {
            return null;
        }
        for (Dungeon d : all()) {
            String task = TASK_ONLY.get(d.id());
            if (task != null && carved(d, x, y, z)) {
                return task;
            }
        }
        return null;
    }

    /** True if the block lies in the carved air of the dungeon {@code id} (its rooms and halls below {@link #DEEP_TOP}). */
    public static boolean inside(String id, int x, int y, int z) {
        if (y > DEEP_TOP) {
            return false;
        }
        for (Dungeon d : all()) {
            if (d.id().equals(id)) {
                return carved(d, x, y, z);
            }
        }
        return false;
    }

    private static boolean carved(Dungeon d, int x, int y, int z) {
        for (StructureDef.Op op : d.structure().ops()) {
            if (op.type() == StructureDef.Op.Type.CLEAR && op.y2() <= DEEP_TOP && x >= op.x1() && x <= op.x2()
                    && z >= op.z1() && z <= op.z2() && y >= op.y1() - 1 && y <= op.y2()) {
                return true;
            }
        }
        return false;
    }

    /** The Abyss rifts (positions are filled in when the Abyss is built). */
    public static List<Rift> rifts() {
        all();
        return DungeonsMisthalin.RIFTS;
    }
}
