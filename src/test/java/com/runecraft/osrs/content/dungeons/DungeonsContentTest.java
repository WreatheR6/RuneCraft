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
        assertEquals(64, Dungeons.all().size());
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
        for (PlaceDef p : ContentRegistry.places()) {
            if (p.style() != PlaceDef.Style.NONE && !replaced.contains(p.id())) {
                int r = Math.max(5, 3 + (p.stations().size() + p.blocks().size()) / 2 + 2);
                keepOut.add(new int[]{p.blockX(), p.blockZ(), r + 3});
            } else {
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
        assertEquals(Map.of("ice_giant", 6, "ankou", 10, "lesser_demon", 7, "black_demon", 4, "black_dragon", 3, "green_dragon", 6,
                "greater_demon", 10, "hellhound", 5, "rat", 3, "wilderness_bandit", 1), caps("dng_wilderness_slayer_cave"));
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
        assertEquals(Map.of("wyrmling", 6, "grimy_lizard", 8), caps("dng_neypotzli"));
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
        for (com.runecraft.osrs.content.skilling.AgilityShortcuts.Shortcut s
                : com.runecraft.osrs.content.skilling.AgilityShortcuts.INDOOR) {
            assertTrue(seen.contains(s.id()), s.id() + " is not placed anywhere");
        }
        assertEquals(seen.size(), Dungeons.shortcutEnds().size() + com.runecraft.osrs.content.skilling.AgilityShortcuts.TOWER_ENDS.size());
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
        // The trapped tunnel's traps stand on its floor, and the rock ledges shelter from falling rocks.
        int traps = 0;
        for (Op op : dungeon("dng_temple_of_marimbo_dungeon").structure().ops()) {
            if ("minecraft:polished_blackstone_pressure_plate".equals(op.block())) {
                assertTrue(DungeonBlocks.apeAtollTrap(op.x1(), op.y1(), op.z1()), op.toString());
                traps++;
            }
        }
        assertEquals(3, traps);
        int[] ledge = ApeAtollDungeons.LEDGES[0];
        int lx = StructureDef.bx(ledge[0] + ApeAtollDungeons.DX);
        int lz = StructureDef.bz(ledge[1] + ApeAtollDungeons.DY);
        assertFalse(DungeonBlocks.apeAtollFallingRocks(lx + 1, ApeAtollDungeons.FLOOR + 1, lz));
        assertTrue(DungeonBlocks.apeAtollFallingRocks(lx + 8, ApeAtollDungeons.FLOOR + 1, lz));
        assertFalse(DungeonBlocks.apeAtollFallingRocks(lx + 8, ApeAtollDungeons.FLOOR + 30, lz));
        assertTrue(DungeonBlocks.breaksDoorSupport("magic_shortbow") && DungeonBlocks.breaksDoorSupport("staff_of_air"));
        assertFalse(DungeonBlocks.breaksDoorSupport("rune_scimitar"));
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
}
