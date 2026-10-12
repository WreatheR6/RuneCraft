package com.runecraft.osrs.content.extras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The layout of a player-owned house: which room is in each cell of the 9x9 grid and which
 * furniture is built in each hotspot (with optional extra data such as a portal's destination).
 * Pure data with OSRS building rules; serialised as a compact string for world-save storage.
 */
public final class HouseLayout {
    public static final int GRID = 9;
    /** The starting garden (with its exit portal) sits in the middle of the grid. */
    public static final int START_X = 4;
    public static final int START_Z = 4;

    /** cell key -> room id. */
    private final Map<Integer, String> rooms = new LinkedHashMap<>();
    /** "cellKey:hotspotId" -> furniture id (optionally "furniture|extra"). */
    private final Map<String, String> furniture = new LinkedHashMap<>();

    public static int key(int x, int z) {
        return x * GRID + z;
    }

    public static boolean inGrid(int x, int z) {
        return x >= 0 && z >= 0 && x < GRID && z < GRID;
    }

    /** A new house: a garden with an exit portal and a parlour north of it, like OSRS. */
    public static HouseLayout starter() {
        HouseLayout h = new HouseLayout();
        h.rooms.put(key(START_X, START_Z), "garden");
        h.rooms.put(key(START_X, START_Z - 1), "parlour");
        h.furniture.put(spot(START_X, START_Z, "centrepiece"), "exit_portal");
        return h;
    }

    private static String spot(int x, int z, String hotspot) {
        return key(x, z) + ":" + hotspot;
    }

    public String roomAt(int x, int z) {
        return inGrid(x, z) ? rooms.get(key(x, z)) : null;
    }

    public Poh.Room room(int x, int z) {
        String id = roomAt(x, z);
        return id == null ? null : Poh.room(id);
    }

    public int roomCount() {
        return rooms.size();
    }

    public Map<Integer, String> rooms() {
        return Collections.unmodifiableMap(rooms);
    }

    /** Built furniture id in a hotspot (without extra data), or null. */
    public String furnitureAt(int x, int z, String hotspot) {
        String v = furniture.get(spot(x, z, hotspot));
        if (v == null) {
            return null;
        }
        int bar = v.indexOf('|');
        return bar < 0 ? v : v.substring(0, bar);
    }

    /** Extra data stored with the furniture in a hotspot (e.g. portal destination), or null. */
    public String extraAt(int x, int z, String hotspot) {
        String v = furniture.get(spot(x, z, hotspot));
        if (v == null) {
            return null;
        }
        int bar = v.indexOf('|');
        return bar < 0 ? null : v.substring(bar + 1);
    }

    public boolean setExtra(int x, int z, String hotspot, String extra) {
        String f = furnitureAt(x, z, hotspot);
        if (f == null) {
            return false;
        }
        furniture.put(spot(x, z, hotspot), extra == null ? f : f + "|" + extra);
        return true;
    }

    /** Hotspot -> furniture id for one room. */
    public Map<String, String> furnitureIn(int x, int z) {
        Map<String, String> out = new LinkedHashMap<>();
        String prefix = key(x, z) + ":";
        for (Map.Entry<String, String> e : furniture.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                String v = e.getValue();
                int bar = v.indexOf('|');
                out.put(e.getKey().substring(prefix.length()), bar < 0 ? v : v.substring(0, bar));
            }
        }
        return out;
    }

    private boolean adjacentToRoom(int x, int z) {
        return roomAt(x + 1, z) != null || roomAt(x - 1, z) != null || roomAt(x, z + 1) != null || roomAt(x, z - 1) != null;
    }

    /** Why a room can't be built in a cell, or null if it can. */
    public String canBuildRoom(Poh.Room room, int x, int z, int constructionLevel) {
        if (room == null) {
            return "There is no such room.";
        }
        if (!inGrid(x, z)) {
            return "Your house can't extend that far.";
        }
        if (roomAt(x, z) != null) {
            return "There is already a room there.";
        }
        if (!adjacentToRoom(x, z)) {
            return "A new room has to join on to an existing room.";
        }
        if (constructionLevel < room.level()) {
            return "You need a Construction level of " + room.level() + " to build " + room.name().toLowerCase() + ".";
        }
        if (roomCount() >= Poh.maxRooms(constructionLevel)) {
            return "You can't have more than " + Poh.maxRooms(constructionLevel) + " rooms at your Construction level.";
        }
        return null;
    }

    public void buildRoom(Poh.Room room, int x, int z) {
        rooms.put(key(x, z), room.id());
    }

    /** Why a room can't be removed, or null. */
    public String canRemoveRoom(int x, int z) {
        if (roomAt(x, z) == null) {
            return "There is no room there.";
        }
        if (x == START_X && z == START_Z) {
            return "You can't remove the garden your house portal leads to.";
        }
        if (!connectedWithout(x, z)) {
            return "Removing that room would cut off other rooms.";
        }
        return null;
    }

    /** True if every room stays reachable from the starting garden without (x, z). */
    private boolean connectedWithout(int rx, int rz) {
        List<Integer> stack = new ArrayList<>();
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        stack.add(key(START_X, START_Z));
        seen.add(key(START_X, START_Z));
        while (!stack.isEmpty()) {
            int k = stack.remove(stack.size() - 1);
            int x = k / GRID;
            int z = k % GRID;
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int nx = x + d[0];
                int nz = z + d[1];
                if ((nx == rx && nz == rz) || roomAt(nx, nz) == null) {
                    continue;
                }
                if (seen.add(key(nx, nz))) {
                    stack.add(key(nx, nz));
                }
            }
        }
        return seen.size() == rooms.size() - 1;
    }

    public void removeRoom(int x, int z) {
        rooms.remove(key(x, z));
        String prefix = key(x, z) + ":";
        furniture.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /** Why a piece of furniture can't be built in a hotspot, or null. Materials are checked by the caller. */
    public String canBuildFurniture(int x, int z, String hotspotId, String furnitureId, int constructionLevel, int otherSkillLevel) {
        Poh.Room room = room(x, z);
        if (room == null) {
            return "There is no room there.";
        }
        Poh.Hotspot h = room.hotspot(hotspotId);
        if (h == null) {
            return "There is no such hotspot in your " + room.name().toLowerCase() + ".";
        }
        Poh.Furniture f = h.option(furnitureId);
        if (f == null) {
            return "You can't build that there.";
        }
        String built = furnitureAt(x, z, hotspotId);
        String base = Poh.upgradeOf(furnitureId);
        if (base == null && built != null) {
            return "There is already something built there.";
        }
        if (base != null && !base.equals(built)) {
            return built == null || !built.equals(furnitureId)
                    ? "The " + f.name().toLowerCase(java.util.Locale.ROOT) + " is built as an upgrade of " + Poh.article(
                    Poh.furniture(base).name().toLowerCase(java.util.Locale.ROOT)) + "."
                    : "There is already something built there.";
        }
        if (constructionLevel < f.level()) {
            return "You need a Construction level of " + f.level() + " to build " + Poh.article(f.name().toLowerCase(java.util.Locale.ROOT)) + ".";
        }
        if (f.otherSkill() != null && otherSkillLevel < f.otherLevel()) {
            return "You need " + Poh.article(f.otherSkill().displayName()) + " level of " + f.otherLevel() + " to build that.";
        }
        return null;
    }

    public void buildFurniture(int x, int z, String hotspotId, String furnitureId) {
        // An upgrade keeps the old piece's extra data (e.g. the teleports stored in a portal nexus).
        String extra = furnitureId.equals(furnitureAt(x, z, hotspotId)) || Poh.upgradeOf(furnitureId) == null ? null
                : extraAt(x, z, hotspotId);
        furniture.put(spot(x, z, hotspotId), extra == null ? furnitureId : furnitureId + "|" + extra);
    }

    /** How many built pieces of furniture in the whole house have the given effect (e.g. beds for servants). */
    public int count(Poh.Effect effect) {
        int n = 0;
        for (String v : furniture.values()) {
            int bar = v.indexOf('|');
            Poh.Furniture f = Poh.furniture(bar < 0 ? v : v.substring(0, bar));
            if (f != null && f.effect() == effect) {
                n++;
            }
        }
        return n;
    }

    public boolean removeFurniture(int x, int z, String hotspotId) {
        return furniture.remove(spot(x, z, hotspotId)) != null;
    }

    /** Items still missing to build {@code f}, given an inventory count lookup. */
    public static Map<String, Integer> missing(Poh.Furniture f, java.util.function.ToIntFunction<String> have) {
        Map<String, Integer> out = new LinkedHashMap<>();
        f.materials().forEach((item, n) -> {
            int got = have.applyAsInt(item);
            if (got < n) {
                out.put(item, n - got);
            }
        });
        return out;
    }

    // ------------------------------------------------------------------ persistence

    /** "R" entries "x,z,room" and "F" entries "x,z,hotspot=furniture[|extra]", separated by ';'. */
    public String encode() {
        StringBuilder sb = new StringBuilder("v1");
        for (Map.Entry<Integer, String> e : rooms.entrySet()) {
            sb.append(";R").append(e.getKey() / GRID).append(',').append(e.getKey() % GRID).append(',').append(e.getValue());
        }
        for (Map.Entry<String, String> e : furniture.entrySet()) {
            int colon = e.getKey().indexOf(':');
            int k = Integer.parseInt(e.getKey().substring(0, colon));
            sb.append(";F").append(k / GRID).append(',').append(k % GRID).append(',').append(e.getKey().substring(colon + 1))
                    .append('=').append(e.getValue());
        }
        return sb.toString();
    }

    public static HouseLayout decode(String s) {
        if (s == null || !s.startsWith("v1")) {
            return starter();
        }
        HouseLayout h = new HouseLayout();
        for (String part : s.split(";")) {
            if (part.length() < 2) {
                continue;
            }
            try {
                if (part.charAt(0) == 'R') {
                    String[] a = part.substring(1).split(",", 3);
                    int x = Integer.parseInt(a[0]);
                    int z = Integer.parseInt(a[1]);
                    if (inGrid(x, z) && Poh.room(a[2]) != null) {
                        h.rooms.put(key(x, z), a[2]);
                    }
                } else if (part.charAt(0) == 'F') {
                    String[] a = part.substring(1).split(",", 3);
                    int x = Integer.parseInt(a[0]);
                    int z = Integer.parseInt(a[1]);
                    int eq = a[2].indexOf('=');
                    if (eq > 0 && inGrid(x, z)) {
                        h.furniture.put(spot(x, z, a[2].substring(0, eq)), a[2].substring(eq + 1));
                    }
                }
            } catch (RuntimeException ignored) {
                // skip corrupt entries
            }
        }
        if (h.rooms.isEmpty()) {
            return starter();
        }
        return h;
    }
}
