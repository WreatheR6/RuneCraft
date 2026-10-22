package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.Modules;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Op;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DungeonsContentTest {
    private static final DungeonsContent MODULE = (DungeonsContent) Modules.ALL.stream()
            .filter(m -> m instanceof DungeonsContent).findFirst().orElseThrow();

    /** Structures of every other module. */
    private static List<StructureDef> others() {
        Set<String> mine = new HashSet<>();
        for (StructureDef s : MODULE.structures()) {
            mine.add(s.id());
        }
        List<StructureDef> out = new ArrayList<>();
        for (StructureDef s : ContentRegistry.structures()) {
            if (!mine.contains(s.id())) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean intersects3d(Op a, Op b) {
        int ma = a.margin();
        int mb = b.margin();
        return a.x1() - ma <= b.x2() + mb && a.x2() + ma >= b.x1() - mb && a.z1() - ma <= b.z2() + mb && a.z2() + ma >= b.z1() - mb
                && a.y1() <= b.y2() && a.y2() >= b.y1();
    }

    private static boolean intersectsXz(Dungeons.Entrance e, Op b) {
        int mb = b.margin();
        return e.x1() <= b.x2() + mb && e.x2() >= b.x1() - mb && e.z1() <= b.z2() + mb && e.z2() >= b.z1() - mb;
    }

    private static Dungeons.Entrance entranceOf(Dungeons.Dungeon d, Op op) {
        for (Dungeons.Entrance e : d.entrances()) {
            if (e.contains(op)) {
                return e;
            }
        }
        return null;
    }

    private static boolean creatureExists(String id) {
        return id.startsWith("minecraft:") || ContentRegistry.monster(id) != null || ContentRegistry.boss(id) != null
                || com.runecraft.osrs.data.Monster.byEntity(id) != null;
    }

    @Test
    void moduleIsRegisteredAndIdsAreUnique() {
        assertEquals("dungeons", MODULE.id());
        assertTrue(Modules.ALL.stream().anyMatch(m -> m instanceof DungeonsContent));
        Set<String> structureIds = new HashSet<>();
        for (StructureDef s : ContentRegistry.structures()) {
            assertTrue(structureIds.add(s.id()), "duplicate structure id " + s.id());
        }
        for (StructureDef s : MODULE.structures()) {
            assertTrue(s.id().startsWith("dng_"), s.id());
            assertTrue(s.replacesLandmark() == null, s.id() + " must not replace a place");
        }
        Set<String> blockIds = new HashSet<>();
        for (ContentModule m : Modules.ALL) {
            for (BlockDef b : m.blocks()) {
                assertTrue(blockIds.add(b.id()), "duplicate block id " + b.id());
            }
        }
        for (BlockDef b : MODULE.blocks()) {
            assertTrue(b.id().startsWith("dng_"), b.id());
        }
        assertEquals(72, Dungeons.all().size());
    }

    @Test
    void opsStayBelowTheSurfaceExceptEntrances() {
        for (Dungeons.Dungeon d : Dungeons.all()) {
            assertTrue(!d.entrances().isEmpty(), d.id() + " has no entrance");
            for (Op op : d.structure().ops()) {
                assertTrue(op.y1() >= -48, d.id() + " digs too deep: " + op);
                if (op.y2() > Dungeons.DEEP_TOP) {
                    assertNotNull(entranceOf(d, op), d.id() + " touches y > " + Dungeons.DEEP_TOP + " outside its entrances: " + op);
                }
            }
        }
    }

    @Test
    void surfaceEntrancesAreOnOpenGround() {
        List<StructureDef> others = others();
        // Plazas, boss arenas and landmark settlements built at runtime.
        List<int[]> keepOut = new ArrayList<>(); // x, z, radius
        Map<String, Integer> arenas = new HashMap<>();
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.raidOnly || b.place == null) {
                continue;
            }
            PlaceDef p = ContentRegistry.place(b.place);
            Landmark l = Landmark.byName(b.place);
            int cx = p != null ? p.blockX() : l != null ? l.blockX() : Integer.MIN_VALUE;
            int cz = p != null ? p.blockZ() : l != null ? l.blockZ() : 0;
            if (cx == Integer.MIN_VALUE) {
                continue;
            }
            int i = arenas.merge(b.place, 1, Integer::sum) - 1;
            int r = b.arenaRadius;
            int ax = cx + (i % 2 == 0 ? 1 : -1) * (14 + r);
            int az = cz + (i / 2) * (2 * r + 6);
            keepOut.add(new int[]{ax, az, r + 3});
        }
        Set<String> replaced = new HashSet<>();
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.replacesLandmark() != null) {
                replaced.add(s.replacesLandmark());
            }
        }
        // A style-NONE place that only marks a dungeon's entrance (same name) builds nothing: its hut may stand on it.
        Set<String> dungeonNames = new HashSet<>();
        for (Dungeons.Dungeon d : Dungeons.all()) {
            dungeonNames.add(d.name());
        }
        for (PlaceDef p : ContentRegistry.places()) {
            if (p.style() != PlaceDef.Style.NONE && !replaced.contains(p.id())) {
                int r = Math.max(5, 3 + (p.stations().size() + p.blocks().size()) / 2 + 2);
                keepOut.add(new int[]{p.blockX(), p.blockZ(), r + 3});
            } else if (!(p.style() == PlaceDef.Style.NONE && dungeonNames.contains(p.name()))) {
                keepOut.add(new int[]{p.blockX(), p.blockZ(), 6});
            }
        }
        for (Landmark l : Landmark.values()) {
            if (!replaced.contains(l.name())) {
                keepOut.add(new int[]{l.blockX(), l.blockZ(), l.isAltar() ? 12 : 24});
            }
        }
        for (Dungeons.Dungeon d : Dungeons.all()) {
            for (Dungeons.Entrance e : d.entrances()) {
                if (!e.surface()) {
                    continue;
                }
                for (StructureDef s : others) {
                    if (e.x2() < s.minX() || e.x1() > s.maxX() || e.z2() < s.minZ() || e.z1() > s.maxZ()) {
                        continue;
                    }
                    for (Op op : s.ops()) {
                        if (op.y2() >= 56 && intersectsXz(e, op)) {
                            fail(d.id() + " entrance " + e + " hits " + s.id() + " " + op);
                        }
                    }
                }
                for (int[] k : keepOut) {
                    int dx = Math.max(0, Math.max(e.x1() - k[0], k[0] - e.x2()));
                    int dz = Math.max(0, Math.max(e.z1() - k[1], k[1] - e.z2()));
                    assertTrue(dx * dx + dz * dz > k[2] * k[2],
                            d.id() + " entrance " + e + " is within " + k[2] + " of a plaza/arena at " + k[0] + "," + k[1]);
                }
            }
        }
    }

    @Test
    void linksOnlyCutTheFloorOfOtherEntrancePits() {
        List<StructureDef> others = others();
        for (Dungeons.Dungeon d : Dungeons.all()) {
            for (Op op : d.structure().ops()) {
                Dungeons.Entrance e = entranceOf(d, op);
                if (e == null || e.surface()) {
                    continue;
                }
                assertTrue(e.yTop() < 60, d.id() + " link reaches the surface " + e);
                for (StructureDef s : others) {
                    for (Op o : s.ops()) {
                        if (o.type() != Op.Type.FLATTEN && o.y2() >= StructureDef.GROUND && intersects3d(op, o)) {
                            fail(d.id() + " link op " + op + " cuts " + s.id() + " building " + o);
                        }
                    }
                }
            }
        }
    }

    @Test
    void dungeonsDoNotCutOtherStructures() {
        List<StructureDef> others = others();
        for (Dungeons.Dungeon d : Dungeons.all()) {
            StructureDef mine = d.structure();
            for (StructureDef s : others) {
                if (mine.maxX() < s.minX() || mine.minX() > s.maxX() || mine.maxZ() < s.minZ() || mine.minZ() > s.maxZ()) {
                    continue;
                }
                for (Op op : mine.ops()) {
                    Dungeons.Entrance e = entranceOf(d, op);
                    if (e != null) {
                        continue;
                    }
                    for (Op o : s.ops()) {
                        if (o.type() != Op.Type.FLATTEN && intersects3d(op, o)) {
                            fail(d.id() + " op " + op + " cuts " + s.id() + " " + o);
                        }
                    }
                }
            }
        }
    }

    @Test
    void dungeonsDoNotOverlapEachOther() {
        List<Dungeons.Dungeon> all = Dungeons.all();
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                StructureDef a = all.get(i).structure();
                StructureDef b = all.get(j).structure();
                if (a.maxX() < b.minX() || a.minX() > b.maxX() || a.maxZ() < b.minZ() || a.minZ() > b.maxZ()) {
                    continue;
                }
                for (Op x : a.ops()) {
                    for (Op y : b.ops()) {
                        if (intersects3d(x, y)) {
                            fail(a.id() + " and " + b.id() + " overlap: " + x + " / " + y);
                        }
                    }
                }
            }
        }
    }

    @Test
    void campsAreInsideTheirDungeonsAndReferenceRealMonsters() {
        for (Dungeons.Dungeon d : Dungeons.all()) {
            assertTrue(!d.camps().isEmpty() || DungeonsAgility.COURSES.contains(d.id()), d.id() + " has no monsters");
            for (Dungeons.Camp c : d.camps()) {
                assertTrue(c.feetY() <= Dungeons.DEEP_TOP, d.id() + " camp on the surface " + c);
                assertTrue(c.cap() > 0 && c.respawnTicks() > 0, c.toString());
                for (String id : c.creatures()) {
                    assertTrue(creatureExists(id), d.id() + " camp creature " + id);
                }
                // Every corner of the camp box and its centre are carved out at the camp's height.
                int[][] pts = {{c.x1(), c.z1()}, {c.x2(), c.z2()}, {c.x1(), c.z2()}, {c.x2(), c.z1()}, {c.centreX(), c.centreZ()}};
                for (int[] p : pts) {
                    boolean carved = false;
                    for (Op op : d.structure().ops()) {
                        if (op.type() == Op.Type.CLEAR && p[0] >= op.x1() && p[0] <= op.x2() && p[1] >= op.z1() && p[1] <= op.z2()
                                && c.feetY() >= op.y1() && c.feetY() + 1 <= op.y2()) {
                            carved = true;
                            break;
                        }
                    }
                    assertTrue(carved, d.id() + " camp point " + p[0] + "," + p[1] + " is not carved: " + c);
                }
            }
        }
        for (MonsterDef m : MODULE.monsters()) {
            assertTrue(m.underground, m.id + " should only appear in dungeons");
            assertTrue(m.spawns.isEmpty(), m.id + " should not spawn naturally");
        }
    }

    @Test
    void entranceLaddersReachTheDungeonFloor() {
        for (Dungeons.Dungeon d : Dungeons.all()) {
            for (Dungeons.Entrance e : d.entrances()) {
                Op ladder = null;
                for (Op op : d.structure().ops()) {
                    if (op.block() != null && op.block().startsWith("minecraft:ladder") && e.contains(op)) {
                        ladder = op;
                    }
                }
                if (ladder == null && d.passages().stream().anyMatch(p -> DungeonBlocks.LIFT_PLATFORM.equals(p.block())
                        && p.x() >= e.x1() && p.x() <= e.x2() && p.z() >= e.z1() && p.z() <= e.z2() && p.y() > Dungeons.DEEP_TOP)) {
                    continue; // a lift hut (its platform goes down instead of a ladder)
                }
                assertNotNull(ladder, d.id() + " entrance without a ladder " + e);
                // The ladder's bottom block is inside the carved dungeon.
                boolean landed = false;
                for (Op op : d.structure().ops()) {
                    if (op.type() == Op.Type.CLEAR && op.y2() <= Dungeons.DEEP_TOP && ladder.x1() >= op.x1() && ladder.x1() <= op.x2()
                            && ladder.z1() >= op.z1() && ladder.z1() <= op.z2() && ladder.y1() >= op.y1() && ladder.y1() <= op.y2()) {
                        landed = true;
                    }
                }
                assertTrue(landed, d.id() + " ladder does not land in the dungeon: " + ladder);
            }
        }
    }

    private static Map<String, Integer> caps(String dungeon) {
        Map<String, Integer> out = new HashMap<>();
        for (Dungeons.Camp c : Dungeons.camps()) {
            if (c.dungeon().equals(dungeon)) {
                for (String m : c.creatures()) {
                    out.merge(m, c.cap(), Integer::sum);
                }
            }
        }
        return out;
    }

    /** The cache-tile dungeons hold exactly the cache's spawns (mejrs/data_osrs NPCList_OSRS.json). */
    @Test
    void cacheDungeonsHoldTheCacheSpawnCounts() {
        assertEquals(Map.of("giant_skeleton", 43, "shadow_hound", 10, "giant_bat", 1, "giant_rat", 2, "rat", 12, "bat", 8),
                caps("dng_shadow_dungeon"));
        assertEquals(Map.of("locust_rider_ranger", 18, "locust_rider", 11, "scarab_mage", 28, "mummy", 1), caps("dng_sophanem_dungeon"));
        assertEquals(Map.of("dagannoth", 15, "dagannoth_lvl92", 7), caps("dng_lighthouse_dungeon"));
        assertEquals(Map.of("kalphite_worker", 18, "kalphite_soldier", 20, "kalphite_guardian", 5), caps("dng_kalphite_cave"));
        // The Wilderness Slayer Cave: the wiki's counts (more ice giants, ankou and hellhounds than the cache) and the
        // 2022 task-only rooms.
        assertEquals(Map.ofEntries(Map.entry("ice_giant", 14), Map.entry("ankou", 16), Map.entry("lesser_demon", 7),
                Map.entry("black_demon", 4), Map.entry("black_dragon", 3), Map.entry("green_dragon", 6), Map.entry("greater_demon", 10),
                Map.entry("hellhound", 9), Map.entry("rat", 3), Map.entry("wilderness_bandit", 1), Map.entry("greater_nechryael", 8),
                Map.entry("abyssal_demon", 8), Map.entry("jelly", 14), Map.entry("dust_devil", 9)), caps("dng_wilderness_slayer_cave"));
        assertEquals(Map.of("temple_spider", 21, "undead_druid", 18, "red_dragon", 6, "baby_red_dragon", 5), caps("dng_forthos_dungeon"));
        assertEquals(Map.of("moss_giant", 10, "hill_giant", 8, "fire_giant_lvl104", 4, "fire_giant_lvl109", 4), caps("dng_giants_den"));
        assertEquals(Map.of("blue_dragon", 4, "baby_blue_dragon", 3, "fire_giant", 8, "greater_demon", 7, "iron_dragon", 3,
                "lesser_demon", 4, "skeleton_lvl25", 4), caps("dng_isle_of_souls_dungeon"));
        assertEquals(Map.of("baby_black_dragon", 1, "baby_blue_dragon", 3, "baby_green_dragon", 1, "baby_red_dragon", 2,
                "black_dragon", 2, "blue_dragon", 5, "green_dragon", 2, "red_dragon", 3), caps("dng_myths_guild_dungeon"));
        assertEquals(Map.of("chaos_dwarf", 4, "fire_giant", 5, "hill_giant", 6, "monk_of_zamorak", 2, "shadow_spider", 12,
                "skeleton_lvl25", 8), caps("dng_deep_wilderness_dungeon"));
        assertEquals(Map.of("lesser_demon", 11, "deadly_red_spider", 5, "giant_bat", 3, "minecraft:skeleton", 5, "skeleton_lvl25", 7,
                "skeleton_lvl45", 7, "rat", 2), caps("dng_karamja_volcano_dungeon"));
        assertEquals(Map.of("ice_troll_runt", 10, "ice_troll_female", 12, "ice_troll", 9, "ice_troll_grunt", 6), caps("dng_ice_troll_caves"));
        assertEquals(Map.of("cave_lizard", 9, "zamorak_crafter", 2, "zamorak_mage", 3, "zamorak_ranger", 3, "zamorak_warrior", 3),
                caps("dng_ourania_cave"));
        // The level 63 shadow hound and the level 106/98 locust riders are real monsters now (diary tasks).
        assertEquals(63, ContentRegistry.monster("shadow_hound").combatLevel);
        assertEquals(106, ContentRegistry.monster("locust_rider").combatLevel);
        assertEquals(98, ContentRegistry.monster("locust_rider_ranger").combatLevel);
    }

    /** The Varlamore dungeons, the Poison Waste Dungeon and the Charred Dungeon hold the wiki's LocLine spawn counts. */
    @Test
    void varlamoreDungeonsHoldTheWikiSpawnCounts() {
        assertEquals(Map.of("wyrmling", 6, "grimy_lizard", 8, "moonlight_cockatrice", 9), caps("dng_neypotzli"));
        assertEquals(Map.ofEntries(Map.entry("cave_crawler", 7), Map.entry("frost_crab", 23), Map.entry("ice_warrior", 7),
                Map.entry("icefiend", 15), Map.entry("chilled_jelly", 10), Map.entry("frost_nagua", 11), Map.entry("ice_spider", 7),
                Map.entry("ice_giant", 14), Map.entry("blue_dragon", 7), Map.entry("baby_blue_dragon", 7),
                Map.entry("brutal_blue_dragon", 5)), caps("dng_ruins_of_tapoyauik"));
        assertEquals(Map.of("earthen_nagua", 25, "grimy_lizard", 15, "giant_rock_crab", 9, "moss_giant", 8, "minecraft:skeleton", 11,
                "rockslug", 6), caps("dng_tonali_cavern"));
        assertEquals(Map.of("juvenile_custodian_stalker", 10, "mature_custodian_stalker", 18, "elder_custodian_stalker", 20,
                "ancient_zygomite", 9), caps("dng_stalker_den"));
        // The Poison Waste Dungeon's warped creatures make warped creature tasks assignable.
        assertEquals(Map.of("warped_tortoise", 15, "warped_terrorbird_lvl96", 36), caps("dng_poison_waste_dungeon"));
        assertTrue(com.runecraft.osrs.content.slayer.SlayerTasks.available("warped_creatures"));
        assertEquals(121, ContentRegistry.monster("warped_tortoise").combatLevel);
        assertEquals(96, ContentRegistry.monster("warped_terrorbird_lvl96").combatLevel);
        assertEquals(Map.of("fire_giant", 6, "hellhound", 5, "lesser_demon", 6, "black_dragon", 5, "red_dragon", 4,
                "baby_black_dragon", 7, "nechryael", 11, "black_demon", 3, "lava_strykewyrm", 12), caps("dng_charred_dungeon"));
        assertEquals("wyrms", ContentRegistry.monster("lava_strykewyrm").slayerCategory);
        // The creatures no longer wait on the surface at the dungeons' entrances.
        for (com.runecraft.osrs.content.zeah.ZeahPlaces.Camp c : com.runecraft.osrs.content.zeah.ZeahPlaces.camps()) {
            for (String id : List.of("wyrmling", "grimy_lizard", "frost_crab", "chilled_jelly", "earthen_nagua", "frost_nagua",
                    "juvenile_custodian_stalker", "mature_custodian_stalker", "elder_custodian_stalker")) {
                assertFalse(c.creatures().contains(id), id + " still camps on the surface at " + c.place());
            }
        }
    }

    /**
     * Round 28: the Varlamore lifts and doors, the Troll Stronghold's generals, guards and secret exit, and the partly
     * multicombat Stalker Den and Tonali Cavern.
     */
    @Test
    void varlamorePassagesAndTrollStronghold() {
        Map<String, Integer> passages = new HashMap<>();
        for (Dungeons.Dungeon d : Dungeons.all()) {
            for (Dungeons.Passage p : d.passages()) {
                passages.merge(d.id() + ":" + p.block(), 1, Integer::sum);
                assertTrue(DungeonBlocks.PASSAGES.contains(p.block()), p.block());
                int found = 0;
                for (Op op : d.structure().ops()) {
                    if (("runecraft:" + p.block()).equals(op.block()) && op.x1() == p.x() && op.y1() == p.y() && op.z1() == p.z()) {
                        found++;
                    }
                }
                assertEquals(1, found, d.id() + " " + p + " block placed");
                assertEquals(p, Dungeons.passageAt(p.x(), p.y(), p.z()));
                if (p.arena() != null) {
                    assertNotNull(com.runecraft.osrs.world.ArenaLayout.of(p.arena()), p.arena() + " has an arena");
                } else if (p.toY() != Dungeons.SURFACE && p.toY() <= Dungeons.DEEP_TOP) {
                    assertTrue(d.structure().ops().stream().anyMatch(op -> op.type() == Op.Type.CLEAR && p.toX() >= op.x1()
                            && p.toX() <= op.x2() && p.toZ() >= op.z1() && p.toZ() <= op.z2() && p.toY() >= op.y1()
                            && p.toY() <= op.y2()), d.id() + " " + p + " arrives in carved air");
                }
            }
        }
        // Two lift ends each (Tapoyauik's on its own hut, Tonali's in the entrance hut), the two doors and the exit.
        assertEquals(Map.of("dng_ruins_of_tapoyauik:dng_lift_platform", 2, "dng_ruins_of_tapoyauik:dng_imposing_door", 1,
                "dng_tonali_cavern:dng_lift_platform", 2, "dng_tonali_cavern:dng_mokhaiotl_entrance", 1,
                "dng_troll_stronghold:dng_secret_exit", 1, "dng_temple_of_marimbo_dungeon:dng_marimbo_crevice", 2,
                "dng_karuulm_slayer_dungeon:dng_imposing_door", 1), passages);
        // The Tonali lift's surface platform stands in the hut on the crypt's entrance tile (1309, 3104); Tapoyauik's
        // lift hut is on the Tower of Ascension's tile (1641, 3221) and its entrance hut on (1693, 3232).
        Dungeons.Dungeon tap = Dungeons.all().stream().filter(x -> x.id().equals("dng_ruins_of_tapoyauik")).findFirst().orElseThrow();
        assertTrue(tap.entrances().stream().anyMatch(e -> e.x1() + 3 == StructureDef.bx(1693) && e.z1() + 3 == StructureDef.bz(3232)));
        assertTrue(tap.entrances().stream().anyMatch(e -> e.x1() + 3 == StructureDef.bx(1641) && e.z1() + 3 == StructureDef.bz(3221)));
        Dungeons.Dungeon ton = Dungeons.all().stream().filter(x -> x.id().equals("dng_tonali_cavern")).findFirst().orElseThrow();
        assertTrue(ton.entrances().stream().anyMatch(e -> e.x1() + 3 == StructureDef.bx(1309) && e.z1() + 3 == StructureDef.bz(3104)));
        // The Troll Stronghold: the three generals, Twig and Berry on top of the cache's mountain trolls; the boss stays.
        assertEquals(Map.of("mountain_troll", 16, "stronghold_troll_general", 3, "twig_troll", 1, "berry_troll", 1),
                caps("dng_troll_stronghold"));
        assertNotNull(ContentRegistry.boss("troll_general"), "the quest's Troll General");
        assertEquals(113, ContentRegistry.monster("stronghold_troll_general").combatLevel);
        assertEquals(71, ContentRegistry.monster("twig_troll").combatLevel);
        int[] godric = DungeonBlocks.trollPrisoner(0);
        assertEquals(StructureDef.bx(2827 + TrollStrongholdInterior.DX), godric[0]);
        assertEquals(TrollStrongholdInterior.F0 + 1, godric[1]);
        // Multicombat: the Stalker Den's south-western caverns and Tonali's Moon and Sun chambers, not the rest.
        int f = 18;
        int sx = StructureDef.bx(1258);
        int sz = StructureDef.bz(9770 + VarlamoreDungeons.SD_DY);
        assertTrue(com.runecraft.osrs.MultiCombat.isMultiAtBlock(sx, f + 1, sz), "Stalker Den south-west");
        assertFalse(com.runecraft.osrs.MultiCombat.isMultiAtBlock(StructureDef.bx(1305), f + 1, StructureDef.bz(9790 + VarlamoreDungeons.SD_DY)),
                "Stalker Den spring");
        assertTrue(com.runecraft.osrs.MultiCombat.isMultiAtBlock(StructureDef.bx(1290), 25, StructureDef.bz(9445 + VarlamoreDungeons.TON_DY)),
                "Tonali Moon chamber");
        assertFalse(com.runecraft.osrs.MultiCombat.isMultiAtBlock(StructureDef.bx(1310), 25, StructureDef.bz(9440 + VarlamoreDungeons.TON_DY)),
                "Tonali hub");
        assertEquals("cockatrice", ContentRegistry.monster("moonlight_cockatrice").slayerCategory);
    }

    /** The Kalphite Cave and the Shadow Dungeon are multicombat underground only; the Kalphite Cave is task-only. */
    @Test
    void kalphiteCaveAndShadowDungeonRules() {
        int sx = StructureDef.bx(2700 + CacheDungeons.SHADOW_DX);
        int sz = StructureDef.bz(5080 + CacheDungeons.SHADOW_DY);
        assertTrue(com.runecraft.osrs.MultiCombat.isMultiAtBlock(sx, 17, sz), "Shadow Dungeon");
        assertTrue(!com.runecraft.osrs.MultiCombat.isMultiAtBlock(sx, 64, sz), "the surface above the Shadow Dungeon");
        assertNull(Dungeons.taskOnly(sx, 17, sz));
        int kx = StructureDef.bx(3305 + CacheDungeons.KC_DX);
        int kz = StructureDef.bz(9500 + CacheDungeons.KC_DY);
        assertTrue(com.runecraft.osrs.MultiCombat.isMultiAtBlock(kx, 31, kz), "Kalphite Cave");
        assertEquals("kalphite", Dungeons.taskOnly(kx, 31, kz));
        assertNull(Dungeons.taskOnly(kx, 70, kz));
        // The Ourania altar stands in the cave's ZMI chamber.
        long altars = Dungeons.all().stream().filter(d -> d.id().equals("dng_ourania_cave"))
                .flatMap(d -> d.structure().ops().stream()).filter(op -> "runecraft:ourania_altar".equals(op.block())).count();
        assertEquals(1, altars);
    }

    /** Every indoor Agility shortcut has its two ends carved into a dungeon (or the Slayer Tower), once. */
    @Test
    void agilityShortcutsAreCarved() {
        Set<String> seen = new HashSet<>();
        for (Dungeons.Dungeon d : Dungeons.all()) {
            for (com.runecraft.osrs.content.skilling.AgilityShortcuts.BlockEnds e : d.shortcuts()) {
                assertTrue(seen.add(e.id()), "shortcut placed twice: " + e.id());
                assertTrue(e.y1() <= Dungeons.DEEP_TOP && e.y2() <= Dungeons.DEEP_TOP, e.toString());
                assertTrue(Math.abs(e.x1() - e.x2()) + Math.abs(e.z1() - e.z2()) >= 3, e.id() + " ends too close");
                int found = 0;
                for (Op op : d.structure().ops()) {
                    if ("runecraft:agility_shortcut".equals(op.block()) && op.x1() == op.x2() && op.y1() == op.y2()
                            && ((op.x1() == e.x1() && op.y1() == e.y1() && op.z1() == e.z1())
                            || (op.x1() == e.x2() && op.y1() == e.y2() && op.z1() == e.z2()))) {
                        found++;
                    }
                }
                assertEquals(2, found, e.id() + " blocks");
            }
        }
        for (com.runecraft.osrs.content.skilling.AgilityShortcuts.BlockEnds e
                : com.runecraft.osrs.content.skilling.AgilityShortcuts.TOWER_ENDS) {
            assertTrue(seen.add(e.id()), "shortcut placed twice: " + e.id());
        }
        // Round 40's dungeon shortcuts are placed by position when their chunk loads (DiaryRound40Test checks the spots).
        for (com.runecraft.osrs.content.skilling.AgilityShortcuts.BlockEnds e
                : com.runecraft.osrs.content.skilling.AgilityShortcuts.DUNGEON_ENDS) {
            assertTrue(seen.add(e.id()), "shortcut placed twice: " + e.id());
        }
        for (com.runecraft.osrs.content.skilling.AgilityShortcuts.Shortcut s
                : com.runecraft.osrs.content.skilling.AgilityShortcuts.INDOOR) {
            assertTrue(seen.contains(s.id()), s.id() + " is not placed anywhere");
        }
        assertEquals(seen.size(), Dungeons.shortcutEnds().size() + com.runecraft.osrs.content.skilling.AgilityShortcuts.TOWER_ENDS.size()
                + com.runecraft.osrs.content.skilling.AgilityShortcuts.DUNGEON_ENDS.size());
    }

    /** The two underground courses: every obstacle step is let into the floor of its cavern once, in reach of the entrance. */
    @Test
    void undergroundAgilityCourses() {
        for (com.runecraft.osrs.data.AgilityCourse course : List.of(com.runecraft.osrs.data.AgilityCourse.WEREWOLF,
                com.runecraft.osrs.data.AgilityCourse.PENGUIN, com.runecraft.osrs.data.AgilityCourse.DORGESH_KAAN)) {
            String id = switch (course) {
                case WEREWOLF -> "dng_werewolf_agility_course";
                case PENGUIN -> "dng_penguin_agility_course";
                default -> "dng_lumbridge_cellar_caves"; // the Dorgesh-Kaan course is on the city's upper level
            };
            Dungeons.Dungeon d = Dungeons.all().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
            int[] count = new int[course.obstacles()];
            for (Op op : d.structure().ops()) {
                String b = op.block();
                if (b != null && b.startsWith("runecraft:agility_obstacle[course=" + course.ordinal() + ",")) {
                    int step = Integer.parseInt(b.substring(b.indexOf("step=") + 5, b.indexOf(']')));
                    count[step]++;
                }
            }
            for (int i = 0; i < count.length; i++) {
                assertEquals(1, count[i], course + " step " + i);
            }
            assertTrue(course.handBuilt(), course.name());
        }
    }

    /** The Isle of Souls chest stands on its OSRS tile (2139, 9299) in the dungeon, shifted like the rest of it. */
    @Test
    void isleOfSoulsChestIsInTheDungeon() {
        Dungeons.Dungeon d = Dungeons.all().stream().filter(x -> x.id().equals("dng_isle_of_souls_dungeon")).findFirst().orElseThrow();
        int found = 0;
        for (Op op : d.structure().ops()) {
            if ("runecraft:isle_of_souls_chest".equals(op.block())) {
                found++;
                assertEquals(StructureDef.bx(2139 + CacheDungeons.IOS_DX), op.x1());
                assertTrue(op.y1() <= Dungeons.DEEP_TOP, "chest underground");
            }
        }
        assertEquals(1, found);
    }

    @Test
    void abyssRiftsLeadToAltars() {
        List<Dungeons.Rift> rifts = Dungeons.rifts();
        assertEquals(13, rifts.size());
        for (Dungeons.Rift r : rifts) {
            Landmark l = Landmark.byName(r.altar());
            assertNotNull(l, r.altar());
            assertTrue(l.isAltar(), r.altar());
        }
    }

    @Test
    void scriptedBlocksUsedInStructuresExist() {
        Set<String> ids = new HashSet<>();
        for (BlockDef b : MODULE.blocks()) {
            ids.add(b.id());
        }
        for (StructureDef s : MODULE.structures()) {
            for (Op op : s.ops()) {
                String b = op.block();
                if (b.startsWith("runecraft:dng_")) {
                    assertTrue(ids.contains(b.substring("runecraft:".length())), s.id() + " uses unknown " + b);
                }
            }
        }
    }

    private static Dungeons.Dungeon dungeon(String id) {
        return Dungeons.all().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }

    private static Map<String, Integer> caps(Dungeons.Dungeon d) {
        Map<String, Integer> out = new HashMap<>();
        for (Dungeons.Camp c : d.camps()) {
            for (String id : c.creatures()) {
                out.merge(id, c.cap(), Integer::sum);
            }
        }
        return out;
    }

    /** Waterbirth Island Dungeon keeps the cache's monster counts (mejrs/data_osrs, sublevels 1-5). */
    @Test
    void waterbirthDungeonHasTheCacheCounts() {
        Map<String, Integer> caps = caps(dungeon("dng_waterbirth_dungeon"));
        assertEquals(81, caps.get("dagannoth_lvl90"));
        assertEquals(58, caps.get("dagannoth_lvl88"));
        assertEquals(47, caps.get("wallasalki"));
        assertEquals(41, caps.get("giant_rock_crab"));
        assertEquals(24, caps.get("rock_lobster"));
        assertEquals(33, caps.get("dagannoth_spawn"));
        assertEquals(5, caps.get("dagannoth_fledgeling"));
        assertEquals(18, caps.get("rock_crab"));
        int ladders = 0;
        for (Op op : dungeon("dng_waterbirth_dungeon").structure().ops()) {
            if (op.block() != null && op.block().equals("runecraft:" + DungeonBlocks.KINGS_LADDER)) {
                ladders++;
            }
        }
        assertEquals(2, ladders, "the Kings' ladder and its other end in the lair");
        assertNotNull(ContentRegistry.boss("dagannoth_rex"));
        assertEquals(11, caps.get("spinolyp"));
        // The ladder links the great hall and the lair both ways; the lair holds the Kings' summoning stone.
        int[] lair = DungeonBlocks.kingsLadderTarget(WaterbirthDungeon.hallArrival()[0], WaterbirthDungeon.P0 + 1,
                WaterbirthDungeon.hallArrival()[2]);
        assertArrayEquals(WaterbirthDungeon.lairArrival(), lair);
        assertArrayEquals(WaterbirthDungeon.hallArrival(), DungeonBlocks.kingsLadderTarget(lair[0], lair[1], lair[2]));
        long stones = dungeon("dng_waterbirth_dungeon").structure().ops().stream()
                .filter(op -> "runecraft:dagannoth_rex_summoning_stone".equals(op.block())
                        && WaterbirthDungeon.inLair(op.x1(), op.y1(), op.z1())).count();
        assertEquals(1, stones);
        // Three circular doors and six door-support blocks on each of their two layers; one Lighthouse ladder.
        Map<String, Integer> blocks = new HashMap<>();
        for (Op op : dungeon("dng_waterbirth_dungeon").structure().ops()) {
            if (op.block() != null && op.block().startsWith("runecraft:dng_")) {
                blocks.merge(op.block().substring("runecraft:".length()), 1, Integer::sum);
            }
        }
        assertEquals(6, blocks.get(DungeonBlocks.CIRCULAR_DOOR));
        assertEquals(12, blocks.get(DungeonBlocks.DOOR_SUPPORT));
        assertEquals(1, blocks.get(DungeonBlocks.LIGHTHOUSE_LADDER));
        assertEquals(1, blocks.get(DungeonBlocks.ROCK_PILE));
        assertTrue(DungeonBlocks.outsideCircularDoor(WaterbirthDungeon.doorX() - 2));
        assertFalse(DungeonBlocks.outsideCircularDoor(WaterbirthDungeon.doorX() + 2));
        // The whole dungeon lies deep enough to leave the sea bed above it untouched.
        for (Op op : dungeon("dng_waterbirth_dungeon").structure().ops()) {
            if (op.y2() < com.runecraft.osrs.world.TerrainAnchors.UNDERGROUND_BELOW) {
                assertTrue(op.y2() < com.runecraft.osrs.world.TerrainAnchors.DEEP_UNDERGROUND_BELOW, op.toString());
            }
        }
    }

    @Test
    void apeAtollDungeonsHaveTheCacheCounts() {
        Map<String, Integer> caps = caps(dungeon("dng_ape_atoll_dungeon"));
        assertEquals(17, caps.get("skeleton_lvl142"));
        assertEquals(30, caps.get("monkey_zombie"));
        Map<String, Integer> temple = caps(dungeon("dng_temple_of_marimbo_dungeon"));
        assertEquals(29, temple.get("monkey_zombie_lvl82"));
        assertEquals(5, temple.get("monkey_zombie"));
        assertEquals(7, temple.get("monkey_zombie_lvl129"));
        assertEquals(22, temple.get("minecraft:cave_spider"));
        assertEquals(29, caps.get("minecraft:cave_spider"));
        // A camp counts its creatures in its box grown by 6-7 blocks (DungeonCamps): no other camp of the same creature
        // may lie in that area, or the two starve each other.
        for (String id : List.of("dng_ape_atoll_dungeon", "dng_temple_of_marimbo_dungeon")) {
            List<Dungeons.Camp> camps = dungeon(id).camps();
            for (Dungeons.Camp a : camps) {
                for (Dungeons.Camp b : camps) {
                    if (a != b && a.creatures().equals(b.creatures())) {
                        assertTrue(b.x1() > a.x2() + 7 || b.x2() < a.x1() - 7 || b.z1() > a.z2() + 7 || b.z2() < a.z1() - 7,
                                id + " camps count each other's monsters: " + a + " and " + b);
                    }
                }
            }
        }
        // The trapped tunnel's traps stand on its floor, and the rock ledges shelter from falling rocks.
        int traps = 0;
        for (Op op : dungeon("dng_temple_of_marimbo_dungeon").structure().ops()) {
            if ("minecraft:polished_blackstone_pressure_plate".equals(op.block())) {
                assertTrue(DungeonBlocks.apeAtollTrap(op.x1(), op.y1(), op.z1()), op.toString());
                traps++;
            }
        }
        assertEquals(1, traps); // one fill over the tunnel's whole width
        int[] ledge = ApeAtollDungeons.LEDGES[0];
        int lx = StructureDef.bx(ledge[0] + ApeAtollDungeons.DX);
        int lz = StructureDef.bz(ledge[1] + ApeAtollDungeons.DY);
        assertFalse(DungeonBlocks.apeAtollFallingRocks(lx + 1, ApeAtollDungeons.FLOOR + 1, lz));
        assertTrue(DungeonBlocks.apeAtollFallingRocks(lx + 8, ApeAtollDungeons.FLOOR + 1, lz));
        assertFalse(DungeonBlocks.apeAtollFallingRocks(lx + 8, ApeAtollDungeons.FLOOR + 30, lz));
        assertTrue(DungeonBlocks.breaksDoorSupport("magic_shortbow") && DungeonBlocks.breaksDoorSupport("staff_of_air"));
        assertFalse(DungeonBlocks.breaksDoorSupport("rune_scimitar"));
    }

    /** Air blocks of a dungeon's CLEAR ops, minus its later FILL/SET blocks, at one feet height (block x, z packed). */
    private static Set<Long> walkable(Dungeons.Dungeon d, int feetY) {
        Set<Long> air = new HashSet<>();
        for (Op op : d.structure().ops()) {
            if (op.y1() > feetY + 1 || op.y2() < feetY) {
                continue;
            }
            for (int x = op.x1(); x <= op.x2(); x++) {
                for (int z = op.z1(); z <= op.z2(); z++) {
                    long k = ((long) x << 32) ^ (z & 0xffffffffL);
                    if (op.type() == Op.Type.CLEAR && op.y1() <= feetY && op.y2() >= feetY + 1) {
                        air.add(k);
                    } else if (op.type() != Op.Type.CLEAR && op.block() != null && !op.block().contains("pressure_plate")) {
                        air.remove(k);
                    }
                }
            }
        }
        return air;
    }

    /** Blocks reachable on foot through {@code air} (4-connected) from the air blocks within 2 of (x, z). */
    private static Set<Long> reach(Set<Long> air, int x, int z) {
        Set<Long> seen = new HashSet<>();
        java.util.ArrayDeque<long[]> q = new java.util.ArrayDeque<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                q.add(new long[]{x + dx, z + dz});
            }
        }
        while (!q.isEmpty()) {
            long[] p = q.poll();
            long k = (p[0] << 32) ^ (p[1] & 0xffffffffL);
            if (!air.contains(k) || !seen.add(k)) {
                continue;
            }
            q.add(new long[]{p[0] + 1, p[1]});
            q.add(new long[]{p[0] - 1, p[1]});
            q.add(new long[]{p[0], p[1] + 1});
            q.add(new long[]{p[0], p[1] - 1});
        }
        return seen;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /**
     * The Slayer self-test (slayer:task_only:...) stands at a task-only camp's centre and spawns the creature a block east of
     * it: both must be open air, not a wall or a pillar.
     */
    @Test
    void taskOnlyCampCentresAreWalkable() {
        Map<String, Set<Long>> cache = new HashMap<>();
        List<String> bad = new ArrayList<>();
        for (Dungeons.Camp c : Dungeons.camps()) {
            Dungeons.TaskOnlyArea area = Dungeons.taskOnlyArea(c.centreX(), c.feetY() + 1, c.centreZ());
            if (c.creatures().isEmpty() || c.creatures().get(0).startsWith("minecraft:") || area == null
                    || Dungeons.TASK_ONLY.containsKey(area.dungeon()) && !area.dungeon().equals("dng_stronghold_slayer_cave")) {
                continue; // the older whole task-only dungeons have checks of their own
            }
            Set<Long> air = cache.computeIfAbsent(c.dungeon() + "@" + c.feetY(), k -> walkable(dungeon(c.dungeon()), c.feetY()));
            if (!air.contains(key(c.centreX(), c.centreZ())) || !air.contains(key(c.centreX() + 1, c.centreZ()))) {
                bad.add(c.dungeon() + " " + c.creatures() + " at " + c.centreX() + "," + c.feetY() + "," + c.centreZ());
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    /**
     * Ape Atoll's dungeons follow the map: from the bamboo ladder every monster's cache tile and Zooknock's cave are
     * reachable on foot through the traced tunnels; in the temple dungeon the first area reaches the wooden door but not
     * the great western cave behind it, and the southern cave is separate.
     */
    @Test
    void apeAtollDungeonsAreTracedAndReachable() {
        Dungeons.Dungeon ape = dungeon("dng_ape_atoll_dungeon");
        Set<Long> air = walkable(ape, ApeAtollDungeons.FLOOR + 1);
        Set<Long> from = reach(air, StructureDef.bx(2763) + 1, StructureDef.bz(9103 + ApeAtollDungeons.DY));
        assertTrue(from.size() > 4000 * 2, "only " + from.size() + " blocks reachable");
        for (Dungeons.Camp c : ape.camps()) {
            assertTrue(from.contains(key(c.x1(), c.z1())) || from.contains(key(c.x2(), c.z2())) || from.contains(key(c.x1(), c.z2()))
                    || from.contains(key(c.x2(), c.z1())), "unreachable camp " + c);
        }
        int[] zk = ApeAtollDungeons.ZOOKNOCK;
        assertTrue(from.contains(key(StructureDef.bx(zk[0]), StructureDef.bz(zk[1] + ApeAtollDungeons.DY)))
                || from.contains(key(StructureDef.bx(zk[0]) + 1, StructureDef.bz(zk[1] + ApeAtollDungeons.DY) + 1)));
        // The traced plans are far from boxes: rock between the tunnels.
        assertFalse(air.contains(key(StructureDef.bx(2700), StructureDef.bz(9120 + ApeAtollDungeons.DY))));
        Dungeons.Dungeon temple = dungeon("dng_temple_of_marimbo_dungeon");
        Set<Long> tAir = walkable(temple, ApeAtollDungeons.TEMPLE_FLOOR + 1);
        Set<Long> first = reach(tAir, StructureDef.bx(2790) + 1, StructureDef.bz(9191 + ApeAtollDungeons.TEMPLE_DY));
        int skullX = StructureDef.bx(2765);
        int skullZ = StructureDef.bz(9200 + ApeAtollDungeons.TEMPLE_DY);
        assertTrue(tAir.contains(key(skullX, skullZ)));
        assertFalse(first.contains(key(skullX, skullZ)), "the wooden door must close off the great western cave");
        assertTrue(first.contains(key(StructureDef.bx(ApeAtollDungeons.TRAP_X) + 1, StructureDef.bz(ApeAtollDungeons.TRAP_Y
                + ApeAtollDungeons.TEMPLE_DY))));
        assertTrue(first.contains(key(StructureDef.bx(2808), StructureDef.bz(9200 + ApeAtollDungeons.TEMPLE_DY))), "walls of flame");
        Set<Long> south = reach(tAir, StructureDef.bx(2790) + 1, StructureDef.bz(9165 + ApeAtollDungeons.TEMPLE_DY));
        assertTrue(south.size() > 500 && !south.contains(key(StructureDef.bx(2790) + 1, StructureDef.bz(9191 + ApeAtollDungeons.TEMPLE_DY))));
        assertEquals(2, temple.entrances().size());
        // The crevice leads to the hot-rock cave, whose hot rocks burn.
        long crevices = temple.passages().stream().filter(p -> DungeonBlocks.MARIMBO_CREVICE.equals(p.block())).count();
        assertEquals(2, crevices);
        int hx = StructureDef.bx(ApeAtollDungeons.HOT_LINE_X);
        int hz = StructureDef.bz(9169 + ApeAtollDungeons.TEMPLE_DY);
        assertEquals(DungeonBlocks.ApeHazard.HOT_ROCKS, DungeonBlocks.apeAtollHazard(hx, ApeAtollDungeons.TEMPLE_FLOOR + 1, hz));
        assertTrue(tAir.contains(key(hx, hz)));
        int[] spike = ApeAtollDungeons.SPIKE_TRAPS[0];
        assertEquals(DungeonBlocks.ApeHazard.SPIKE_TRAP, DungeonBlocks.apeAtollHazard(StructureDef.bx(spike[0]), ApeAtollDungeons.FLOOR + 1,
                StructureDef.bz(spike[1] + ApeAtollDungeons.DY)));
        assertTrue(DungeonBlocks.insideMarimboDoor(StructureDef.bx(ApeAtollDungeons.DOOR_X) - 1));
        assertFalse(DungeonBlocks.insideMarimboDoor(StructureDef.bx(ApeAtollDungeons.DOOR_X) + 1));
    }

    /**
     * Zooknock, Bunkwicket, Waymottin and Bardur stand in their dungeons, on carved floor at their cache tiles, and the
     * quests module's Zooknock is the one in the Ape Atoll Dungeon.
     */
    @Test
    void underGroundNpcsStandInCarvedAir() {
        Map<String, String> dungeonOf = Map.of("zooknock", "dng_ape_atoll_dungeon", "bunkwicket", "dng_ape_atoll_dungeon",
                "waymottin", "dng_ape_atoll_dungeon", "bardur", "dng_waterbirth_dungeon", "quest_viggora_3", "dng_edgeville_dungeon",
                "vannaka", "dng_edgeville_dungeon");
        for (Map.Entry<String, String> e : dungeonOf.entrySet()) {
            com.runecraft.osrs.content.NpcDef npc = com.runecraft.osrs.content.ContentRegistry.npc(e.getKey());
            assertNotNull(npc, e.getKey());
            PlaceDef place = com.runecraft.osrs.content.ContentRegistry.place(npc.place());
            assertNotNull(place, npc.place());
            Integer feet = com.runecraft.osrs.world.NpcStoreys.undergroundFeetY(npc.id());
            assertNotNull(feet, npc.id());
            int x = place.blockX() + npc.dx();
            int z = place.blockZ() + npc.dz();
            assertTrue(walkable(dungeon(e.getValue()), feet).contains(key(x, z)), npc.id() + " at " + x + "," + feet + "," + z);
        }
        PlaceDef zc = com.runecraft.osrs.content.ContentRegistry.place(DungeonsContent.ZOOKNOCK_CAVE);
        com.runecraft.osrs.content.NpcDef zooknock = com.runecraft.osrs.content.ContentRegistry.npc("zooknock");
        assertEquals(StructureDef.bx(ApeAtollDungeons.ZOOKNOCK[0]), zc.blockX() + zooknock.dx());
        assertEquals(StructureDef.bz(ApeAtollDungeons.ZOOKNOCK[1] + ApeAtollDungeons.DY), zc.blockZ() + zooknock.dz());
        // Waterbirth is multicombat underground only: no surface multi box around the island any more.
        assertTrue(com.runecraft.osrs.MultiCombat.AROUND.stream().noneMatch(a -> a.place().equals("waterbirth_island")));
        assertTrue(com.runecraft.osrs.MultiCombat.UNDERGROUND.contains("dng_waterbirth_dungeon"));
    }

    /** The Mort'ton catacombs' key doors stand in their halls, one per metal, each guarding a crypt with its shades. */
    @Test
    void shadeCatacombDoors() {
        Dungeons.Dungeon d = dungeon("dng_shade_catacombs");
        List<ShadeCatacombs.Door> doors = ShadeCatacombs.doors();
        assertEquals(5, doors.size());
        Set<String> blocks = new HashSet<>();
        for (BlockDef b : MODULE.blocks()) {
            blocks.add(b.id());
        }
        for (int i = 0; i < doors.size(); i++) {
            ShadeCatacombs.Door door = doors.get(i);
            assertEquals(com.runecraft.osrs.content.CommonItems.SHADE_KEY_METALS[i], door.metal());
            assertTrue(blocks.contains(door.block()), door.block());
            Op placed = null;
            for (Op op : d.structure().ops()) {
                if (op.block() != null && op.block().equals("runecraft:" + door.block())) {
                    placed = op;
                }
            }
            assertNotNull(placed, door.block());
            assertEquals(door.x(), placed.x1());
            assertEquals(door.z(), placed.z1());
            assertEquals(ShadeCatacombs.FLOOR + 1, placed.y1());
            assertTrue(door.inside(door.x() + 0.5 + door.dx() * 2, door.z() + 0.5 + door.dz() * 2));
            assertTrue(!door.inside(door.x() + 0.5 - door.dx() * 2, door.z() + 0.5 - door.dz() * 2));
            assertEquals(door, ShadeCatacombs.doorAt(door.x(), door.z()));
        }
        // Every shade of the rework lives in the catacombs, the urium shades behind the gold doors.
        Map<String, Integer> caps = caps(d);
        for (String shade : new String[]{"phrin_shade", "riyl_shade", "asyn_shade", "fiyr_shade", "urium_shade"}) {
            assertTrue(caps.getOrDefault(shade, 0) > 0, shade);
        }
        // The keys are shared items with unchanged ids.
        assertTrue(com.runecraft.osrs.ItemCatalog.exists("bronze_shade_key"));
        assertTrue(com.runecraft.osrs.ItemCatalog.exists("gold_shade_key_purple"));
    }

    /** The Ruins of Camdozaal hold the wiki's golem counts and the mine, fishing and altar features. */
    @Test
    void ruinsOfCamdozaal() {
        Dungeons.Dungeon d = dungeon("dng_ruins_of_camdozaal");
        assertEquals(Map.of("flawed_golem", 8, "mind_golem", 6, "body_golem", 6, "chaos_golem", 7), caps(d));
        Map<String, Integer> blocks = new HashMap<>();
        for (Op op : d.structure().ops()) {
            if (op.block() != null && op.block().startsWith("runecraft:")) {
                blocks.merge(op.block().substring("runecraft:".length()), 1, Integer::sum);
            }
        }
        assertEquals(18, blocks.get("barronite_rock"));
        assertEquals(3, blocks.get("clay_rock"));
        assertEquals(3, blocks.get("tin_rock"));
        assertEquals(3, blocks.get("copper_rock"));
        assertEquals(7, blocks.get("camdozaal_fishing_spot"));
        assertEquals(2, blocks.get("camdozaal_preparation_table"));
        assertEquals(1, blocks.get("camdozaal_altar"));
        assertEquals(1, blocks.get("barronite_crusher"));
        assertEquals(1, blocks.get("camdozaal_sacred_forge"));
        for (String id : blocks.keySet()) {
            assertTrue(ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(id)) || id.endsWith("_rock")
                    || id.equals("anvil") || id.equals("bank_booth"), "unknown block " + id);
        }
    }
}
