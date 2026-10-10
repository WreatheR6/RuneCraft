package com.runecraft.osrs.content.world;

import static com.runecraft.osrs.content.world.AsgarniaKit.mc;
import static com.runecraft.osrs.content.world.AsgarniaKit.opposite;
import static com.runecraft.osrs.content.world.AsgarniaKit.stair;

/**
 * The interior of a building storey in local coordinates: {@code u} runs along the back wall
 * (the wall opposite the door) and {@code v} is the depth away from it, so furnishing code works
 * the same whichever way the building faces. Heights are relative to the storey floor.
 */
final class AsgarniaRoom {
    final AsgarniaKit k;
    final int x1;
    final int z1;
    final int x2;
    final int z2;
    final int y;
    final String door;

    AsgarniaRoom(AsgarniaKit k, int x1, int z1, int x2, int z2, int y, String door) {
        this.k = k;
        this.x1 = x1;
        this.z1 = z1;
        this.x2 = x2;
        this.z2 = z2;
        this.y = y;
        this.door = door;
    }

    /** The same footprint on storey {@code level} (0 = ground). */
    AsgarniaRoom level(int level) {
        return new AsgarniaRoom(k, x1, z1, x2, z2, level * 4, door);
    }

    boolean ns() {
        return door.equals("north") || door.equals("south");
    }

    int len() {
        return ns() ? x2 - x1 + 1 : z2 - z1 + 1;
    }

    int depth() {
        return ns() ? z2 - z1 + 1 : x2 - x1 + 1;
    }

    int wx(int u, int v) {
        return switch (door) {
            case "south" -> x1 + u;
            case "north" -> x2 - u;
            case "east" -> x1 + v;
            default -> x2 - v;
        };
    }

    int wz(int u, int v) {
        return switch (door) {
            case "south" -> z1 + v;
            case "north" -> z2 - v;
            case "east" -> z2 - u;
            default -> z1 + u;
        };
    }

    /** World direction of increasing u. */
    String plusU() {
        return switch (door) {
            case "south" -> "east";
            case "north" -> "west";
            case "east" -> "north";
            default -> "south";
        };
    }

    String minusU() {
        return opposite(plusU());
    }

    String front() {
        return door;
    }

    String back() {
        return opposite(door);
    }

    void set(int u, int v, int dy, String block) {
        if (u < 0 || v < 0 || u >= len() || v >= depth()) {
            return;
        }
        k.s(wx(u, v), y + dy, wz(u, v), block);
    }

    void fill(int u1, int v1, int u2, int v2, int dy1, int dy2, String block) {
        u1 = Math.max(0, u1);
        v1 = Math.max(0, v1);
        u2 = Math.min(len() - 1, u2);
        v2 = Math.min(depth() - 1, v2);
        if (u1 > u2 || v1 > v2) {
            return;
        }
        k.f(Math.min(wx(u1, v1), wx(u2, v2)), y + dy1, Math.min(wz(u1, v1), wz(u2, v2)),
                Math.max(wx(u1, v1), wx(u2, v2)), y + dy2, Math.max(wz(u1, v1), wz(u2, v2)), block);
    }

    /** True for the cells in front of the doorway, which furnishing keeps clear. */
    boolean doorway(int u, int v) {
        int mid = len() / 2;
        return v >= depth() - 2 && u >= mid - 2 && u <= mid + 1;
    }

    // ------------------------------------------------------------------ furniture

    /** A table (top slab with a candle) with chairs on both u sides. */
    void table(int u, int v, String wood) {
        set(u, v, 0, mc(wood + "_slab[type=top]"));
        set(u, v, 1, mc("candle[candles=1,lit=true]"));
        set(u - 1, v, 0, stair(mc(wood + "_stairs"), minusU()));
        set(u + 1, v, 0, stair(mc(wood + "_stairs"), plusU()));
    }

    /** A long table along u with chairs on both v sides. */
    void longTable(int u1, int u2, int v, String wood, String cloth) {
        fill(u1, v, u2, v, 0, 0, mc(wood + "_slab[type=top]"));
        for (int u = u1; u <= u2; u += 2) {
            set(u, v - 1, 0, stair(mc(wood + "_stairs"), back()));
            set(u, v + 1, 0, stair(mc(wood + "_stairs"), front()));
            set(u, v, 1, cloth);
        }
    }

    /** A bed along v with its head at (u, v) toward the back wall, and a rug at its foot. */
    void bed(int u, int v, String color) {
        set(u, v, 0, mc("air"));
        set(u, v + 1, 0, mc(color + "_bed[facing=" + back() + ",part=foot]"));
        set(u, v + 2, 0, mc(color + "_carpet"));
    }

    /** A cosy home: bedroll, chest, bookshelf, table and chairs, barrel, rug and plant. */
    void home(String wood, String rug) {
        int l = len();
        int d = depth();
        bed(l - 1, 0, rug);
        if (l > 4) {
            set(l - 2, 0, 0, mc("chest[facing=" + front() + "]"));
        }
        if (l > 5) {
            fill(1, 0, 2, 0, 0, 1, mc("bookshelf"));
            set(2, 0, 2, mc("potted_red_tulip"));
        }
        if (d >= 4 && l >= 5) {
            table(l / 2, d / 2 - (d > 5 ? 1 : 0), wood);
        }
        set(0, d - 1, 0, mc("barrel[facing=up]"));
        set(0, d - 2, 0, mc("crafting_table"));
        if (l > 6) {
            set(l - 1, d - 1, 0, mc("furnace[facing=" + minusU() + "]"));
        }
        int mid = l / 2;
        fill(mid - 1, d - 2, mid, d - 2, 0, 0, mc(rug + "_carpet"));
    }

    /** A shop: counter across the room with the shop block in the middle and goods behind. */
    void shop(String shopBlock, String counter, String... goods) {
        int l = len();
        int cv = depth() >= 5 ? 2 : 1;
        fill(1, cv, l - 2, cv, 0, 0, counter);
        set(l / 2, cv, 0, shopBlock);
        for (int u = 1; u < l; u++) {
            if (goods.length > 0) {
                set(u, 0, 0, goods[u % goods.length]);
                if (u % 2 == 0) {
                    set(u, 0, 1, goods[(u + 1) % goods.length]);
                }
            }
        }
        set(l / 2 - 2, cv, 1, mc("lantern"));
        set(l / 2 + 2, cv, 1, mc("flower_pot"));
    }

    /** A bank: a row of booths across the room, vault chests behind and a red carpet runner. */
    void bank(String divider) {
        int l = len();
        int d = depth();
        int bv = d >= 6 ? 2 : 1;
        for (int u = 1; u <= l - 2; u++) {
            set(u, bv, 0, u % 2 == 1 ? "runecraft:bank_booth" : divider);
            if (u % 2 == 0) {
                set(u, bv, 1, mc("lantern"));
            }
        }
        for (int u = 1; u < l; u++) {
            set(u, 0, 0, mc("chest[facing=" + front() + "]"));
            if (u % 3 == 0) {
                set(u, 0, 1, mc("gold_block"));
            }
        }
        int mid = l / 2;
        fill(mid - 1, bv + 1, mid, d - 1, 0, 0, mc("red_carpet"));
        set(1, d - 1, 0, mc("potted_fern"));
        set(l - 2, d - 1, 0, mc("potted_fern"));
    }

    /** A tavern: bar along the back with barrels, a cooking range, a hearth and tables. */
    void inn(String wood) {
        int l = len();
        int d = depth();
        fill(1, 1, l / 2, 1, 0, 0, mc(wood + "_slab[type=top]"));
        fill(1, 0, l / 2, 0, 0, 0, mc("barrel[facing=" + front() + "]"));
        fill(1, 0, l / 2, 0, 1, 1, mc("barrel[facing=up]"));
        set(2, 1, 1, mc("brewing_stand"));
        set(l - 1, 0, 0, "runecraft:cooking_range");
        set(l - 2, 0, 0, mc("smoker[facing=" + front() + "]"));
        set(l - 1, d / 2, 0, mc("campfire[lit=true]"));
        set(l - 1, d / 2 - 1, 0, mc("bricks"));
        set(l - 1, d / 2 + 1, 0, mc("bricks"));
        for (int u = 2; u < l - 2; u += 4) {
            for (int v = 3; v < d - 1; v += 3) {
                if (!doorway(u, v) && !doorway(u + 1, v)) {
                    table(u, v, wood);
                }
            }
        }
    }
}
