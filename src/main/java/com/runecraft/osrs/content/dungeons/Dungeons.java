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
     * {x1, y1, z1, x2, y2, z2} of the parts that are multicombat in OSRS where the rest is single-way; {@code taskOnly}
     * are its Slayer task-only parts ({@link TaskOnlyArea}).
     */
    public record Dungeon(String id, String name, StructureDef structure, List<Entrance> entrances, List<Camp> camps,
                          List<AgilityShortcuts.BlockEnds> shortcuts, List<Passage> passages, List<int[]> multi,
                          List<TaskOnlyArea> taskOnly) {
    }

    /**
     * A part of a dungeon whose monsters can only be fought on a Slayer task to kill them (OSRS Wiki "Slayer guardians":
     * the guardian, e.g. Peeve or Garbek quo Maten, stops anyone else attacking them; entering is free). {@code task} is
     * the category the area is known for ({@link #ANY_TASK} when it holds several kinds, like the Stronghold Slayer Cave);
     * the gate itself lets a player hurt whatever counts towards their own task. The box is block coordinates of the
     * area's air, {@code y1} its floor.
     */
    public record TaskOnlyArea(String dungeon, String task, String guardian, int x1, int y1, int z1, int x2, int y2, int z2) {
        public boolean contains(int x, int y, int z) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
        }

        public int centreX() {
            return (x1 + x2) / 2;
        }

        public int centreZ() {
            return (z1 + z2) / 2;
        }
    }

    /** {@link TaskOnlyArea#task()} of an area whose monsters count for whatever task the player has. */
    public static final String ANY_TASK = "any";

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
     * basement, the Lizardman Caves' shamans and every monster of the Stronghold Slayer Cave ("players can only attack the
     * Slayer monster they were assigned") can only be attacked on a matching task. Parts of other dungeons are task-only
     * too: see {@link Dungeon#taskOnly()}.
     */
    public static final java.util.Map<String, String> TASK_ONLY = java.util.Map.of("dng_kalphite_cave", "kalphite",
            "dng_kraken_cove", "cave_kraken", "dng_smoke_devil_dungeon", "smoke_devils", "dng_lizardman_caves", "lizardmen",
            "dng_slayer_tower_basement", "nechryael", "dng_stronghold_slayer_cave", ANY_TASK);

    /** The Slayer guardian of each {@link #TASK_ONLY} dungeon (OSRS Wiki "Slayer guardians"). */
    public static final java.util.Map<String, String> GUARDIANS = java.util.Map.of("dng_kalphite_cave", "Entomologist",
            "dng_kraken_cove", "Lieve McCracken", "dng_smoke_devil_dungeon", "Breive", "dng_lizardman_caves", "Captain Cleive",
            "dng_slayer_tower_basement", "Raulyn", "dng_stronghold_slayer_cave", "Nieve");

    /** The Slayer task category needed to fight at this block (a task-only area or dungeon), or null. */
    public static String taskOnly(int x, int y, int z) {
        TaskOnlyArea a = taskOnlyArea(x, y, z);
        return a == null ? null : a.task();
    }

    /**
     * The task-only area at this block: one of a dungeon's task-only parts ({@link Dungeon#taskOnly()}), else the carved air
     * of a {@link #TASK_ONLY} dungeon (as an area spanning the whole dungeon), or null.
     */
    public static TaskOnlyArea taskOnlyArea(int x, int y, int z) {
        if (y > DEEP_TOP) {
            return null;
        }
        for (Dungeon d : all()) {
            for (TaskOnlyArea a : d.taskOnly()) {
                if (a.contains(x, y, z)) {
                    return a;
                }
            }
        }
        for (Dungeon d : all()) {
            String task = TASK_ONLY.get(d.id());
            if (task != null && carved(d, x, y, z)) {
                StructureDef s = d.structure();
                return new TaskOnlyArea(d.id(), task, GUARDIANS.get(d.id()), s.minX(), -64, s.minZ(), s.maxX(), DEEP_TOP, s.maxZ());
            }
        }
        return null;
    }

    /** Every task-only part of a dungeon ({@link Dungeon#taskOnly()}; whole {@link #TASK_ONLY} dungeons are not listed). */
    public static List<TaskOnlyArea> taskOnlyAreas() {
        List<TaskOnlyArea> out = new ArrayList<>();
        for (Dungeon d : all()) {
            out.addAll(d.taskOnly());
        }
        return out;
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

    /**
     * True if block column (x, z) lies in the Karuulm Slayer Dungeon's entrance chamber (the Tasakaal's), the only part of
     * the dungeon whose volcanic floor doesn't burn.
     */
    public static boolean karuulmEntranceChamber(int x, int z) {
        return DungeonsZeah.inKaruulmChamber(x, z);
    }

    /** The Abyss rifts (positions are filled in when the Abyss is built). */
    public static List<Rift> rifts() {
        all();
        return DungeonsMisthalin.RIFTS;
    }
}
