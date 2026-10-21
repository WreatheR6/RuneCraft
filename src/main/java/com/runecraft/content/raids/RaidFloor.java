package com.runecraft.content.raids;

import com.runecraft.boss.FloorMarkers;
import com.runecraft.boss.HazardKind;
import com.runecraft.osrs.content.raids.RaidRealtime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Raid floor marks (round 43, // playability: OSRS tints the tiles of a raid's floor mechanics; particles alone are easy to miss
 * in real time): groups of temporary coloured floor blocks ({@link FloorMarkers}) under Olm's acid and crystal bombs, Akkha's
 * safe quadrant, the Wardens' lightning and slam rows, the Maiden's blood, Xarpus's exhumes, the Sotetseg maze, Kephri's dung.
 * A group lasts its game ticks on the raid clock, or until its strike lands or its mechanic releases it; every group in a room
 * goes when the room resets (a ToA or ToB Entry wipe) or the raid closes, and with its owner (the boss) when that leaves the
 * world. Every block goes back as {@link FloorMarkers} promises (a server stop, a crash).
 */
final class RaidFloor {
    // The colour of each meaning, in one place, the same as the bosses' floor marks (round 43: red strike, cyan safe, dark red blood).
    /** Where a strike will land (red). */
    static final HazardKind DANGER = HazardKind.STRIKE;
    /** Where to stand (cyan). */
    static final HazardKind SAFE = HazardKind.SAFE;
    /** The Maiden's blood (dark red). */
    static final HazardKind BLOOD = HazardKind.BLOOD;
    /** Kephri's dung (grey). */
    static final HazardKind DUNG = HazardKind.GAS;
    /** Olm's acid and Xarpus's exhumes (lime). */
    static final HazardKind ACID = HazardKind.ACID;
    /** Poison pools (green). */
    static final HazardKind POISON = HazardKind.POISON;
    /** Lightning about to strike (light blue). */
    static final HazardKind LIGHTNING = HazardKind.ICE;

    /** One mechanic's marked floor blocks. */
    static final class Group {
        private final ServerLevel world;
        private final List<FloorMarkers.Mark> marks = new ArrayList<>();
        /** Where it is (room clearing). */
        private final Vec3 anchor;
        /** The raid game tick it goes back on, or Long.MAX_VALUE until released. */
        private final long until;
        private boolean released;

        private Group(ServerLevel world, Vec3 anchor, long until) {
            this.world = world;
            this.anchor = anchor;
            this.until = until;
        }

        /** Puts its blocks back (repeats are ignored). */
        void release() {
            if (!released) {
                released = true;
                marks.forEach(FloorMarkers::release);
                marks.clear();
            }
        }

        boolean released() {
            return released;
        }

        /** Floor blocks it marked (some may be unmarkable: a wall, a chest, an unloaded chunk). */
        int size() {
            return marks.size();
        }
    }

    private static final List<Group> GROUPS = new ArrayList<>();

    private RaidFloor() {
    }

    /**
     * Marks the floor columns {@code columns} ({x, z}) under feet height {@code y} with {@code kind} for {@code gameTicks} game
     * ticks (0 or less: until released). {@code owner}: the boss whose mechanic it is (its marks go when it leaves), or null.
     */
    static Group columns(ServerLevel world, Collection<int[]> columns, double y, HazardKind kind, int gameTicks, Entity owner, Vec3 anchor) {
        long until = gameTicks > 0 ? RaidEffects.now() + gameTicks : Long.MAX_VALUE;
        Group g = new Group(world, anchor, until);
        for (int[] c : columns) {
            FloorMarkers.Mark m = FloorMarkers.place(world, new Vec3(c[0] + 0.5, y, c[1] + 0.5), kind, owner);
            if (m != null) {
                g.marks.add(m);
            }
        }
        GROUPS.add(g);
        return g;
    }

    /** A circle of {@code radius} round {@code centre} (a standing position). */
    static Group disc(ServerLevel world, Vec3 centre, double radius, HazardKind kind, int gameTicks, Entity owner) {
        return columns(world, RaidRealtime.discColumns(centre.x, centre.z, radius), centre.y, kind, gameTicks, owner, centre);
    }

    /** A lane {@code halfWidth} either side of the line from {@code from} to {@code to}. */
    static Group lane(ServerLevel world, Vec3 from, Vec3 to, double halfWidth, HazardKind kind, int gameTicks, Entity owner) {
        return columns(world, RaidRealtime.laneColumns(from.x, from.z, to.x, to.z, halfWidth), from.y, kind, gameTicks, owner,
                from.lerp(to, 0.5));
    }

    /** The block rectangle between two columns (block x, z), at feet height {@code y}. */
    static Group rect(ServerLevel world, int x0, int z0, int x1, int z1, double y, HazardKind kind, int gameTicks, Entity owner) {
        return columns(world, RaidRealtime.rectColumns(x0, z0, x1, z1), y, kind, gameTicks, owner,
                new Vec3((x0 + x1) / 2.0 + 0.5, y, (z0 + z1) / 2.0 + 0.5));
    }

    /** Every raid game tick (RaidEffects): groups whose time is up go back. */
    static void tick(long now) {
        if (GROUPS.isEmpty()) {
            return;
        }
        GROUPS.removeIf(g -> {
            if (g.released || now >= g.until) {
                g.release();
                return true;
            }
            return false;
        });
    }

    /** Puts back every group inside a box (a room reset, a raid closing). */
    static void clear(ServerLevel world, AABB box) {
        GROUPS.removeIf(g -> {
            if (g.world == world && box.contains(g.anchor)) {
                g.release();
                return true;
            }
            return false;
        });
    }

    /** Self-test: groups still marking blocks inside a box. */
    static int liveIn(ServerLevel world, AABB box) {
        int n = 0;
        for (Group g : GROUPS) {
            if (!g.released && g.world == world && box.contains(g.anchor)) {
                n++;
            }
        }
        return n;
    }
}
