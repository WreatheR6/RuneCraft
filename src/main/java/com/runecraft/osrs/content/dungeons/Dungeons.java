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

    /** One dungeon. */
    public record Dungeon(String id, String name, StructureDef structure, List<Entrance> entrances, List<Camp> camps,
                          List<AgilityShortcuts.BlockEnds> shortcuts) {
    }

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
            list.addAll(VarlamoreDungeons.all());
            list.add(PoisonWasteDungeon.dungeon());
            list.add(CharredDungeon.dungeon());
            list.add(ShayzienCrypts.dungeon());
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

    /**
     * Dungeons whose monsters can only be fought on a Slayer task, by dungeon id: the task category the player must
     * have. OSRS: the Kalphite Cave's kalphites can only be attacked on a kalphite task.
     */
    public static final java.util.Map<String, String> TASK_ONLY = java.util.Map.of("dng_kalphite_cave", "kalphite");

    /** The Slayer task category needed to fight at this block (inside a {@link #TASK_ONLY} dungeon's carved air), or null. */
    public static String taskOnly(int x, int y, int z) {
        if (y > DEEP_TOP) {
            return null;
        }
        for (Dungeon d : all()) {
            String task = TASK_ONLY.get(d.id());
            if (task == null) {
                continue;
            }
            for (StructureDef.Op op : d.structure().ops()) {
                if (op.type() == StructureDef.Op.Type.CLEAR && op.y2() <= DEEP_TOP && x >= op.x1() && x <= op.x2()
                        && z >= op.z1() && z <= op.z2() && y >= op.y1() - 1 && y <= op.y2()) {
                    return task;
                }
            }
        }
        return null;
    }

    /** The Abyss rifts (positions are filled in when the Abyss is built). */
    public static List<Rift> rifts() {
        all();
        return DungeonsMisthalin.RIFTS;
    }
}
