package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Bosses fought in a cave of their own below ground rather than in an arena beside their place on the surface. In OSRS these
 * lairs are instanced areas off the world map (Yama's Domain at 1503, 10080 below the Chasm of Fire, the Maggot King's lair on
 * Vampyrium, the Royal Titans' cave off the western end of the Asgarnian Ice Dungeon, Mokhaiotl's delve below his ruins);
 * here each is carved at a fixed spot underground near its OSRS tiles ({@link BossDef#lair}), with its arena, summoning stone,
 * lights and an exit portal laid by the bosses module's structure, and a way in: a small entrance hut on the surface near the
 * OSRS way in, or (Mokhaiotl) the Ruins of Mokhaiotl's entrance inside the Tonali Cavern, which the dungeons module puts there.
 */
public final class BossLairs {
    private BossLairs() {
    }

    /** The entrance hut's scripted block: takes the player down into its lair. */
    public static final String ENTRANCE = "boss_lair_entrance";
    /** The exit in each lair: takes the player back to where they came in. */
    public static final String EXIT = "boss_lair_exit";
    /** Air above an arena floor (the summoning stone's arena build clears 8 blocks too). */
    public static final int HEADROOM = 9;
    /** Blocks of cave floor between an arena's centre radius and the cave wall, so the exit stands outside the ring. */
    public static final int APRON = 4;
    /** Surface level the entrance huts are levelled to (their first air block). */
    public static final int SURFACE = StructureDef.GROUND;

    /**
     * A boss's underground lair: its arena centred on OSRS tile (tileX, tileY) with the floor at block height {@code floorY},
     * an entrance hut at surface tile (hutX, hutY) when {@code hutX > 0}, and {@code exitTo}: the block the lair's exit takes
     * players back to (in front of the hut, or beside the door they came in by).
     */
    public record Lair(String bossId, int tileX, int tileY, int floorY, int hutX, int hutY, int[] exitTo, String cave,
                       String hutWall, String note) {
        public int x() {
            return GielinorMap.toBlockX(tileX);
        }

        public int z() {
            return GielinorMap.toBlockZ(tileY);
        }

        public boolean hasHut() {
            return hutX > 0;
        }

        /** The hut's entrance block (surface level). */
        public int[] entranceBlock() {
            return hasHut() ? new int[]{StructureDef.bx(hutX), SURFACE, StructureDef.bz(hutY) - 1} : null;
        }

        /** Where a player arrives in the lair: south of the arena's southern opening, facing it. */
        public int[] arrival(int radius) {
            return new int[]{x(), floorY + 1, z() + radius + 2};
        }

        /** The exit portal, behind the arrival spot. */
        public int[] exitBlock(int radius) {
            return new int[]{x(), floorY + 1, z() + radius + APRON - 1};
        }

        /** The summoning stone, inside the arena by its north opening (where the surface arenas have theirs). */
        public int[] stoneBlock(int radius) {
            return new int[]{x(), floorY + 1, z() - radius + 2};
        }

        /** Half the width of the carved cave (air) around the centre. */
        public int half(int radius) {
            return radius + APRON;
        }
    }

    // Yama's Domain is entered from the bottom of the Chasm of Fire (wiki: speak to the Voice of Yama there; leave by the glowing
    // portal to the south). His cave lies at the domain's tiles (1503, 10080 = 1503, 3680) as deep as the chasm's bottom floor,
    // its hut beside the chasm's entrance. approx: the way in is a hut, not the Voice of Yama.
    public static final Lair YAMA = new Lair("yama", 1503, 3680, -10, 1446, 3672,
            yard(1446, 3672), "minecraft:blackstone",
            "minecraft:polished_blackstone_bricks", "Yama's Domain");
    // The Royal Titans' cave below the Asgarnian Ice Dungeon (wiki: its entrance tunnel is in the dungeon's western part; the
    // Evil Eye drops players at 2952, 9574 outside it). approx: here a hut north-west of the dungeon's trapdoor on Mudskipper Point
    // leads down to it, beneath the ice dungeon's floor.
    public static final Lair ROYAL_TITANS = new Lair(RoyalTitansLoot.BRANDA, 3008, 3150, 4, 2991, 3168,
            yard(2991, 3168), "minecraft:packed_ice",
            "minecraft:stone_bricks", "Royal Titans' cave");
    // The Maggot King's lair (an instance on Vampyrium, entered east of Sangvesti): below its hive place by Darkmeyer. approx
    public static final Lair MAGGOT_KING = new Lair("maggot_king", 3660, 3310, 8, 3660, 3295,
            yard(3660, 3295), "minecraft:mud",
            "minecraft:mud_bricks", "Maggot King's lair");
    // Mokhaiotl's delve below the Ruins of Mokhaiotl (1311, 9520): the dungeons module's entrance in the ruins' lobby (the
    // Tonali Cavern's middle floor, y 24, its tiles shifted by -6350 to 1311, 3177) leads down; the exit returns beside it.
    public static final Lair DOOM = new Lair(DoomDelves.BOSS, 1311, 3164, 4, 0, 0,
            new int[]{StructureDef.bx(1311), 25, StructureDef.bz(3177) + 2}, "minecraft:tuff", null,
            "Mokhaiotl's delve");

    public static final List<Lair> ALL = List.of(YAMA, ROYAL_TITANS, MAGGOT_KING, DOOM);

    /** In front of the hut at surface tile (hutX, hutY): where its lair's exit leads, on the hut's levelled yard. */
    private static int[] yard(int hutX, int hutY) {
        return new int[]{StructureDef.bx(hutX), SURFACE, StructureDef.bz(hutY) + 4};
    }

    /** The lair of a boss, or null. */
    public static Lair of(String bossId) {
        for (Lair l : ALL) {
            if (l.bossId().equals(bossId)) {
                return l;
            }
        }
        return null;
    }

    /** Sets each lair boss's {@link BossDef#lair}. */
    public static void apply(List<BossDef> bosses) {
        for (BossDef b : bosses) {
            Lair l = of(b.id);
            if (l != null) {
                b.lair(l.tileX(), l.tileY(), l.floorY());
            }
        }
    }

    /** The lair whose entrance block is at (x, y, z) (or within a block of it), or null. */
    public static Lair byEntrance(int x, int y, int z) {
        for (Lair l : ALL) {
            int[] e = l.entranceBlock();
            if (e != null && Math.abs(e[0] - x) <= 1 && Math.abs(e[1] - y) <= 1 && Math.abs(e[2] - z) <= 1) {
                return l;
            }
        }
        return null;
    }

    /** The lair whose cave holds (x, y, z), or null; {@code radius} gives each boss's arena radius. */
    public static Lair at(int x, int y, int z, java.util.function.ToIntFunction<String> radius) {
        for (Lair l : ALL) {
            int h = l.half(radius.applyAsInt(l.bossId())) + 1;
            if (Math.abs(x - l.x()) <= h && Math.abs(z - l.z()) <= h && y >= l.floorY() - 1 && y <= l.floorY() + HEADROOM + 1) {
                return l;
            }
        }
        return null;
    }

    /** The arena floor, wall and accent blocks of a style, as the summoning stone arenas use them ({@code DataBossService}). */
    static String[] arenaBlocks(String style) {
        return switch (style == null ? "stone" : style) {
            case "lava" -> new String[]{"minecraft:blackstone", "minecraft:polished_blackstone_bricks", "minecraft:magma_block"};
            case "ice" -> new String[]{"minecraft:packed_ice", "minecraft:blue_ice", "minecraft:snow_block"};
            case "crypt" -> new String[]{"minecraft:deepslate_tiles", "minecraft:chiseled_deepslate", "minecraft:cracked_deepslate_tiles"};
            case "jungle" -> new String[]{"minecraft:mossy_cobblestone", "minecraft:mossy_stone_bricks", "minecraft:moss_block"};
            case "void" -> new String[]{"minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:purpur_block"};
            case "water" -> new String[]{"minecraft:prismarine_bricks", "minecraft:dark_prismarine", "minecraft:sea_lantern"};
            case "sand" -> new String[]{"minecraft:smooth_sandstone", "minecraft:cut_sandstone", "minecraft:chiseled_sandstone"};
            default -> new String[]{"minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:chiseled_stone_bricks"};
        };
    }

    /** One structure per lair: the cave, its arena and stone, lights, the exit, and the entrance hut on the surface. */
    public static List<StructureDef> structures(List<BossDef> bosses) {
        List<StructureDef> out = new ArrayList<>();
        for (Lair l : ALL) {
            BossDef b = null;
            for (BossDef d : bosses) {
                if (d.id.equals(l.bossId())) {
                    b = d;
                }
            }
            if (b != null) {
                out.add(structure(l, b));
            }
        }
        return out;
    }

    /** Structure id of a lair. */
    public static String structureId(Lair l) {
        return "boss_lair_" + l.bossId();
    }

    static StructureDef structure(Lair l, BossDef b) {
        StructureDef.Builder s = StructureDef.builder(structureId(l), l.note());
        int r = b.arenaRadius;
        int h = l.half(r);
        int cx = l.x();
        int cz = l.z();
        int f = l.floorY();
        int top = f + HEADROOM;
        // the cave: a sealed shell, its air, a rough floor
        s.fill(cx - h - 1, f - 3, cz - h - 1, cx + h + 1, top + 1, cz + h + 1, l.cave());
        s.clear(cx - h, f + 1, cz - h, cx + h, top, cz + h);
        s.fill(cx - h, f, cz - h, cx + h, f, cz + h, l.cave());
        // the arena, laid as the summoning-stone arenas are (DataBossService.buildArena): a disc of floor ringed by a wall two
        // blocks high, open at the four cardinal points
        String[] st = arenaBlocks(b.arenaStyle);
        double outer = (r + 1.5) * (r + 1.5);
        double ring = (r - 0.5) * (r - 0.5);
        for (int dz = -r - 1; dz <= r + 1; dz++) {
            String run = null;
            int from = 0;
            String wallRun = null;
            int wallFrom = 0;
            for (int dx = -r - 2; dx <= r + 2; dx++) {
                int d2 = dx * dx + dz * dz;
                String kind = null;
                String wall = null;
                if (d2 <= outer) {
                    boolean isRing = d2 > ring;
                    kind = isRing ? st[1] : (dx + dz) % 7 == 0 ? st[2] : st[0];
                    if (isRing && Math.abs(dx) > 1 && Math.abs(dz) > 1) {
                        wall = st[1];
                    }
                }
                if (!java.util.Objects.equals(kind, run)) {
                    if (run != null) {
                        s.fill(cx + from, f, cz + dz, cx + dx - 1, f, cz + dz, run);
                    }
                    run = kind;
                    from = dx;
                }
                if (!java.util.Objects.equals(wall, wallRun)) {
                    if (wallRun != null) {
                        s.fill(cx + wallFrom, f + 1, cz + dz, cx + dx - 1, f + 2, cz + dz, wallRun);
                    }
                    wallRun = wall;
                    wallFrom = dx;
                }
            }
        }
        // pillars in the cave's corners and lights in its ceiling
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                s.fill(cx + sx * (h - 1), f + 1, cz + sz * (h - 1), cx + sx * (h - 1), top, cz + sz * (h - 1), st[1]);
            }
        }
        for (int x = cx - h + 2; x <= cx + h - 2; x += 6) {
            for (int z = cz - h + 2; z <= cz + h - 2; z += 6) {
                s.set(x, top + 1, z, "minecraft:shroomlight");
            }
        }
        int[] stone = l.stoneBlock(r);
        s.set(stone[0], stone[1], stone[2], "runecraft:" + b.summonBlockId());
        int[] exit = l.exitBlock(r);
        s.set(exit[0], exit[1], exit[2], "runecraft:" + EXIT);
        if (l.hasHut()) {
            hut(s, l);
        }
        return s.build();
    }

    /** The entrance hut: a levelled yard, a small stone hut open to the south, the entrance block against its north wall. */
    private static void hut(StructureDef.Builder s, Lair l) {
        int x = StructureDef.bx(l.hutX());
        int z = StructureDef.bz(l.hutY());
        int g = SURFACE;
        s.flatten(x - 4, z - 4, x + 4, z + 5, "minecraft:coarse_dirt");
        s.walls(x - 2, g, z - 3, x + 2, g + 2, z + 1, l.hutWall());
        s.fill(x - 2, g + 3, z - 3, x + 2, g + 3, z + 1, l.hutWall());
        s.clear(x, g, z + 1, x, g + 1, z + 1);
        s.set(x - 1, g + 2, z - 1, "minecraft:lantern[hanging=true]");
        int[] e = l.entranceBlock();
        s.set(e[0], e[1], e[2], "runecraft:" + ENTRANCE);
    }
}
