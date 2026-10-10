package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.Modules;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.slayer.SlayerCategories;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Data integrity of the islands module. */
class IslandsContentTest {
    private final IslandsContent module = new IslandsContent();

    private static boolean placeExists(String id) {
        return ContentRegistry.place(id) != null || Landmark.byName(id) != null;
    }

    private static int[] centre(String place) {
        PlaceDef p = ContentRegistry.place(place);
        if (p != null) {
            return new int[]{p.blockX(), p.blockZ()};
        }
        Landmark l = Landmark.byName(place);
        return l == null ? null : new int[]{l.blockX(), l.blockZ()};
    }

    private static boolean creatureExists(String id) {
        return ContentRegistry.monster(id) != null || ContentRegistry.boss(id) != null
                || com.runecraft.osrs.data.Monster.byEntity(id) != null;
    }

    private static boolean runecraftBlockExists(String id) {
        if (ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(id)) || id.endsWith("_tree_log")) {
            return true;
        }
        for (Station s : Station.values()) {
            if (s.blockId().equals(id)) {
                return true;
            }
        }
        for (Rock r : Rock.values()) {
            if (r.blockId().equals(id)) {
                return true;
            }
        }
        for (FishingSpot f : FishingSpot.values()) {
            if (f.blockId().equals(id)) {
                return true;
            }
        }
        for (Thieving.Stall s : Thieving.Stall.values()) {
            if (s.blockId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String bare(String block) {
        return block.contains("[") ? block.substring(0, block.indexOf('[')) : block;
    }

    @Test
    void moduleIsRegisteredAndIdsArePrefixed() {
        assertTrue(Modules.ALL.stream().anyMatch(m -> m.id().equals("islands")));
        for (PlaceDef p : module.places()) {
            assertTrue(p.id().startsWith("isl_"), p.id());
            assertNotNull(ContentRegistry.place(p.id()), p.id());
        }
        for (NpcDef n : module.npcs()) {
            assertTrue(n.id().startsWith("isl_"), n.id());
            assertTrue(placeExists(n.place()), n.id() + " at " + n.place());
            assertTrue(Math.abs(n.dx()) <= 140 && Math.abs(n.dz()) <= 140, n.id() + " offset");
        }
        for (BlockDef b : module.blocks()) {
            assertTrue(b.id().startsWith("isl_"), b.id());
        }
        for (StructureDef s : module.structures()) {
            assertTrue(s.id().startsWith("isl_"), s.id());
        }
        List<String> items = new ArrayList<>();
        module.items((id, name, kind, shape, color) -> {
            items.add(id);
            return new ItemDef(id, name, kind, shape, color);
        });
        assertEquals(items.size(), new HashSet<>(items).size(), "duplicate item ids");
        for (String id : items) {
            assertNotNull(ItemCatalog.get(id), id);
        }
    }

    @Test
    void placesAreAtTheirRealCoordinatesWithRoomForTheirPlazas() {
        for (PlaceDef p : module.places()) {
            assertTrue(p.osrsX() > 2000 && p.osrsX() < 3900, p.id() + " x");
            assertTrue(p.osrsY() > 2600 && p.osrsY() < 4500, p.id() + " y");
            assertTrue(GielinorMap.wildernessLevel(p.blockX(), p.blockZ()) == 0, p.id() + " in the Wilderness");
            assertNull(Landmark.byName(p.id()), p.id());
            for (PlaceDef o : ContentRegistry.places()) {
                if (o.id().equals(p.id()) || p.style() == PlaceDef.Style.NONE || o.style() == PlaceDef.Style.NONE) {
                    continue;
                }
                double d = Math.hypot(p.blockX() - o.blockX(), p.blockZ() - o.blockZ());
                assertTrue(d >= 32, p.id() + " too close to " + o.id() + " (" + d + ")");
            }
        }
        assertEquals(3094, ContentRegistry.place(IslandsPlaces.TUTORIAL_ISLAND).osrsX());
        assertEquals(4434, ContentRegistry.place(IslandsPlaces.ZANARIS).osrsY());
        // Lunar Isle lies north-west of Miscellania, which lies north of Ape Atoll.
        assertTrue(ContentRegistry.place(IslandsPlaces.LUNAR_ISLE).osrsX() < ContentRegistry.place(IslandsPlaces.MISCELLANIA).osrsX());
        assertTrue(ContentRegistry.place(IslandsPlaces.MISCELLANIA).osrsY() > ContentRegistry.place(IslandsPlaces.APE_ATOLL).osrsY());
    }

    @Test
    void npcDialogueExists() {
        for (NpcDef n : module.npcs()) {
            assertFalse(IslandsNpcs.dialogue(n.id()).isEmpty(), n.id() + " has nothing to say");
        }
        // Quest characters stay with the quests module.
        for (String id : new String[]{"king_awowogei", "king_vargas", "advisor_ghrim", "oneiromancer", "captain_braindeath"}) {
            assertTrue(module.npcs().stream().noneMatch(n -> n.id().equals(id)), id);
        }
    }

    @Test
    void monstersAreFaithfulAndKnown() {
        Map<String, Integer> levels = Map.of("ammonite_crab", 25, "long_tailed_wyvern", 152, "ancient_wyvern", 210, "monkey_guard", 167,
                "monkey_archer", 86, "suqah", 111, "yak", 22, "wallasalki", 98, "rock_lobster", 127, "jogre", 53);
        levels.forEach((id, lvl) -> {
            MonsterDef m = ContentRegistry.monster(id);
            assertNotNull(m, id);
            assertEquals(lvl, m.combatLevel, id);
        });
        assertEquals(130, ContentRegistry.monster("monkey_guard").hitpoints);
        assertEquals(105, ContentRegistry.monster("suqah").hitpoints);
        for (MonsterDef m : module.monsters()) {
            assertTrue(m.spawns.isEmpty(), m.id + " spawns through camps only");
            assertTrue(m.slayerCategory == null || SlayerCategories.get(m.slayerCategory) != null, m.id + " " + m.slayerCategory);
            for (Drop d : m.drops) {
                assertTrue(ItemCatalog.exists(d.item()), m.id + " drops " + d.item());
            }
        }
        // Reused, not duplicated.
        for (String id : new String[]{"spitting_wyvern", "taloned_wyvern", "dagannoth", "cave_horror", "ice_troll", "mutated_zygomite"}) {
            assertTrue(module.monsters().stream().noneMatch(m -> m.id.equals(id)), id);
            assertNotNull(ContentRegistry.monster(id), id);
        }
    }

    @Test
    void campsReferenceRealThings() {
        for (IslandsPlaces.Camp c : IslandsPlaces.camps()) {
            assertTrue(placeExists(c.place()), c.place());
            assertTrue(c.cap() > 0 && c.radius() > 0);
            for (String id : c.creatures()) {
                assertTrue(creatureExists(id), id);
            }
        }
        for (MonsterDef m : module.monsters()) {
            assertTrue(IslandsPlaces.camps().stream().anyMatch(c -> c.creatures().contains(m.id)), m.id + " never spawns");
        }
    }

    @Test
    void caveCampsLieInsideCarvedCaves() {
        // Ours (the Corsair Cove Dungeon) or the dungeons module's (the Wyvern Cave).
        List<StructureDef> all = ContentRegistry.structures();
        int caves = 0;
        for (IslandsPlaces.Camp camp : IslandsPlaces.camps()) {
            if (camp.surface()) {
                continue;
            }
            caves++;
            assertTrue(camp.floorY() < StructureDef.GROUND - 15, "caves are deep underground");
            int[] c = centre(camp.place());
            int cx = c[0] + camp.dx();
            int cz = c[1] + camp.dz();
            boolean covered = false;
            for (StructureDef s : all) {
                for (StructureDef.Op op : s.ops()) {
                    if (op.type() == StructureDef.Op.Type.CLEAR && op.y1() == camp.floorY() && op.x1() <= cx - camp.radius()
                            && op.x2() >= cx + camp.radius() && op.z1() <= cz - camp.radius() && op.z2() >= cz + camp.radius()) {
                        covered = true;
                    }
                }
            }
            assertTrue(covered, camp.place() + " camp must lie inside a carved cave: " + cx + "," + cz + " r" + camp.radius());
        }
        assertEquals(2, caves);
    }

    // ------------------------------------------------------------------ structures

    /** A rectangle of blocks owned by someone else's plaza, boss arena or structure. */
    private record Zone(String owner, int x1, int z1, int x2, int z2) {
        boolean intersects(int ax1, int az1, int ax2, int az2) {
            return ax2 >= x1 && ax1 <= x2 && az2 >= z1 && az1 <= z2;
        }
    }

    private static Zone square(String owner, int cx, int cz, int r) {
        return new Zone(owner, cx - r, cz - r, cx + r, cz + r);
    }

    /** Every other module's plazas (unless replaced by this module), arenas and structures. */
    private static List<Zone> foreignZones(Set<String> replaced) {
        List<Zone> zones = new ArrayList<>();
        for (PlaceDef p : ContentRegistry.places()) {
            if (!replaced.contains(p.id()) && p.style() != PlaceDef.Style.NONE) {
                int r = Math.max(5, 3 + (p.stations().size() + p.blocks().size()) / 2 + 2) + 2;
                zones.add(square(p.id(), p.blockX(), p.blockZ(), r));
            }
        }
        for (Landmark l : Landmark.values()) {
            if (!replaced.contains(l.name())) {
                zones.add(square(l.name(), l.blockX(), l.blockZ(), l.isAltar() ? 5 : 10));
            }
        }
        Map<String, Integer> arenaIndex = new HashMap<>();
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.raidOnly || b.place == null) {
                continue;
            }
            int[] c = centre(b.place);
            if (c == null) {
                continue;
            }
            int i = arenaIndex.merge(b.place, 1, Integer::sum) - 1;
            int r = b.arenaRadius;
            zones.add(square(b.id + " arena", c[0] + (i % 2 == 0 ? 1 : -1) * (14 + r), c[1] + (i / 2) * (2 * r + 6), r + 2));
        }
        for (ContentModule m : Modules.ALL) {
            if (!m.id().equals("islands")) {
                for (StructureDef s : m.structures()) {
                    // Only what reaches the surface: dungeons' caves lie far below.
                    int x1 = Integer.MAX_VALUE;
                    int z1 = Integer.MAX_VALUE;
                    int x2 = Integer.MIN_VALUE;
                    int z2 = Integer.MIN_VALUE;
                    for (StructureDef.Op op : s.ops()) {
                        if (underground(op)) {
                            continue;
                        }
                        int mg = op.margin();
                        x1 = Math.min(x1, op.x1() - mg);
                        z1 = Math.min(z1, op.z1() - mg);
                        x2 = Math.max(x2, op.x2() + mg);
                        z2 = Math.max(z2, op.z2() + mg);
                    }
                    if (x1 <= x2) {
                        zones.add(new Zone(s.id() + " structure", x1, z1, x2, z2));
                    }
                }
            }
        }
        return zones;
    }

    private static boolean underground(StructureDef.Op op) {
        return op.type() != StructureDef.Op.Type.FLATTEN && op.y2() < StructureDef.GROUND - 1;
    }

    @Test
    void structuresLeaveOtherPlazasArenasAndStructuresAlone() {
        List<StructureDef> raw = IslandsStructures.raw();
        Set<String> replaced = new HashSet<>();
        raw.forEach(s -> replaced.add(s.replacesLandmark()));
        List<Zone> zones = foreignZones(replaced);
        List<String> problems = new ArrayList<>();
        for (StructureDef s : raw) {
            for (StructureDef.Op op : s.ops()) {
                if (underground(op)) {
                    continue;
                }
                int m = op.margin();
                for (Zone z : zones) {
                    if (z.intersects(op.x1() - m, op.z1() - m, op.x2() + m, op.z2() + m)) {
                        problems.add(s.id() + " " + op.type() + " at tiles " + GielinorMap.toOsrsX(op.x1()) + "," + GielinorMap.toOsrsY(op.z2())
                                + ".." + GielinorMap.toOsrsX(op.x2()) + "," + GielinorMap.toOsrsY(op.z1()) + " overlaps " + z.owner());
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
        // Nothing had to be clipped by the safety net.
        List<StructureDef> all = IslandsStructures.all();
        assertEquals(raw.size(), all.size());
        for (int i = 0; i < raw.size(); i++) {
            assertEquals(raw.get(i).ops().size(), all.get(i).ops().size(), raw.get(i).id());
        }
    }

    /**
     * Structures are applied in list order, chunk by chunk: a flatten (or its blend ring) applied later
     * would wipe anything an earlier structure of this module built there.
     */
    @Test
    void ownStructuresDoNotFlattenEachOther() {
        List<StructureDef> list = IslandsStructures.raw();
        List<String> problems = new ArrayList<>();
        for (int j = 0; j < list.size(); j++) {
            for (StructureDef.Op f : list.get(j).ops()) {
                if (f.type() != StructureDef.Op.Type.FLATTEN) {
                    continue;
                }
                int m = f.margin();
                for (int i = 0; i < j; i++) {
                    for (StructureDef.Op op : list.get(i).ops()) {
                        if (op.type() == StructureDef.Op.Type.FLATTEN || op.y2() < StructureDef.GROUND) {
                            continue;
                        }
                        if (op.x2() >= f.x1() - m && op.x1() <= f.x2() + m && op.z2() >= f.z1() - m && op.z1() <= f.z2() + m) {
                            problems.add(list.get(j).id() + " flattens over " + list.get(i).id() + " at tile "
                                    + GielinorMap.toOsrsX(op.x1()) + "," + GielinorMap.toOsrsY(op.z1()));
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void structuresReplacePlacesAndContainTheirStations() {
        Set<String> replacedElsewhere = new HashSet<>();
        for (ContentModule m : Modules.ALL) {
            if (!m.id().equals("islands")) {
                m.structures().forEach(s -> replacedElsewhere.add(s.replacesLandmark()));
            }
        }
        Set<String> mine = new HashSet<>();
        module.places().forEach(p -> mine.add(p.id()));
        Set<String> replaced = new HashSet<>();
        for (StructureDef s : module.structures()) {
            String target = s.replacesLandmark();
            assertNotNull(target, s.id());
            assertNotNull(ContentRegistry.place(target), s.id() + " replaces unknown place " + target);
            assertTrue(replaced.add(target), target + " replaced twice");
            assertFalse(replacedElsewhere.contains(target), target + " is already replaced by another module");
            assertEquals(StructureDef.Op.Type.FLATTEN, s.ops().get(0).type(), s.id() + " must start by flattening");
            PlaceDef p = ContentRegistry.place(target);
            assertTrue(p.blockX() >= s.minX() && p.blockX() <= s.maxX() && p.blockZ() >= s.minZ() && p.blockZ() <= s.maxZ(),
                    s.id() + " does not cover its place");
            Set<String> used = new HashSet<>();
            for (StructureDef.Op op : s.ops()) {
                used.add(bare(op.block()));
            }
            for (Station st : p.stations()) {
                assertTrue(used.contains("runecraft:" + st.blockId()), s.id() + " lacks " + st);
            }
            for (String block : p.blocks()) {
                String full = block.contains(":") ? block : "runecraft:" + block;
                assertTrue(used.contains(full), s.id() + " lacks " + block);
            }
            for (String b : used) {
                if (b.startsWith("runecraft:")) {
                    assertTrue(runecraftBlockExists(b.substring("runecraft:".length())), s.id() + " uses unknown block " + b);
                }
            }
        }
        // Every new place has its structure, and the existing island places are built over.
        assertTrue(replaced.containsAll(mine.stream().filter(id -> ContentRegistry.place(id).style() != PlaceDef.Style.NONE).toList()));
        for (String id : new String[]{IslandsPlaces.FOSSIL_ISLAND, IslandsPlaces.APE_ATOLL, IslandsPlaces.APE_ATOLL_MARKET,
                IslandsPlaces.APE_ATOLL_TEMPLE, IslandsPlaces.MISCELLANIA, IslandsPlaces.LUNAR_ISLE, IslandsPlaces.NEITIZNOT,
                IslandsPlaces.JATIZSO, IslandsPlaces.WATERBIRTH, IslandsPlaces.UNGAEL, IslandsPlaces.MOS_LE_HARMLESS,
                IslandsPlaces.HARMONY_ISLAND, IslandsPlaces.CRANDOR, IslandsPlaces.KHARAZI_JUNGLE, IslandsPlaces.GU_TANOTH,
                IslandsPlaces.CASTLE_WARS, IslandsPlaces.CORSAIR_COVE, IslandsPlaces.CORSAIR_COVE_DOCKS, IslandsPlaces.FELDIP_HUNTER_AREA,
                IslandsPlaces.ZUL_ANDRA, IslandsPlaces.BRAINDEATH_ISLAND, IslandsPlaces.VOID_KNIGHTS_OUTPOST, IslandsPlaces.VERDANT_VALLEY}) {
            assertTrue(replaced.contains(id), id + " has no hand-built structure");
        }
    }

    @Test
    void doorsAndBedsGiveOneHalf() {
        for (StructureDef s : module.structures()) {
            for (StructureDef.Op op : s.ops()) {
                String b = op.block();
                if (b.contains("_door") && !b.contains("trapdoor")) {
                    assertTrue(b.contains("half=lower") || b.contains("half=upper"), s.id() + " " + b);
                }
                if (b.contains("_bed")) {
                    assertTrue(b.contains("part="), s.id() + " " + b);
                }
            }
        }
    }

    // ------------------------------------------------------------------ travel, agility, kingdom

    private static boolean blockAt(String block, int x, int z) {
        for (StructureDef s : IslandsStructures.all()) {
            for (StructureDef.Op op : s.ops()) {
                if (op.block().equals(block) && op.x1() <= x && op.x2() >= x && op.z1() <= z && op.z2() >= z
                        && op.type() != StructureDef.Op.Type.CLEAR) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void travelBlocksStandAtTheirStops() {
        for (IslandsTravel.Network n : IslandsTravel.networks()) {
            assertTrue(n.stops().size() >= 4, n.id());
            assertTrue(n.quest() == null || ContentRegistry.quest(n.quest()) != null, n.quest());
            Set<String> codes = new HashSet<>();
            for (IslandsTravel.Stop s : n.stops()) {
                assertTrue(codes.add(s.code()), s.code());
                int[] c = centre(s.place());
                assertNotNull(c, s.place());
                if (s.hasBlock()) {
                    assertTrue(blockAt("runecraft:" + n.blockId(), c[0] + s.dx(), c[1] + s.dz()),
                            n.id() + " " + s.code() + " has no block at its stop");
                }
            }
        }
        assertEquals("COSMIC_ALTAR", IslandsTravel.FAIRY_RINGS.stop("CKP").place());
        assertNotNull(Landmark.byName("COSMIC_ALTAR"));
        IslandsTravel.Stop next = IslandsTravel.next(IslandsTravel.FAIRY_RINGS, "AIQ", "AIQ");
        assertEquals("CIP", next.code());
        assertEquals("AIQ", IslandsTravel.next(IslandsTravel.FAIRY_RINGS, "ZANARIS", "CIP").code());
        assertEquals("AIQ", IslandsTravel.next(IslandsTravel.FAIRY_RINGS, "CKP", "ZANARIS").code());
    }

    @Test
    void apeAtollAgilityCourse() {
        assertEquals(IslandsApeAtoll.OBSTACLES.length, IslandsAgility.APE_ATOLL.size());
        double total = 0;
        for (IslandsAgility.Obstacle o : IslandsAgility.APE_ATOLL) {
            assertTrue(blockAt("runecraft:" + IslandsBlocks.APE_OBSTACLE, StructureDef.bx(o.tileX()), StructureDef.bz(o.tileY())),
                    o.name());
            total += o.xp();
        }
        assertEquals(580.0, total, 1e-9);
        assertEquals(48, IslandsAgility.APE_ATOLL_LEVEL);
    }

    @Test
    void kingdomManagement() {
        Kingdom.State s = Kingdom.deposit(Kingdom.State.fresh(), 10_000_000);
        assertEquals(Kingdom.MAX_COFFER, s.coffer());
        assertEquals(75_000, Kingdom.dailySpend(s.coffer(), true));
        assertEquals(50_000, Kingdom.dailySpend(s.coffer(), false));
        assertEquals(1_000, Kingdom.dailySpend(10_000, true));
        Kingdom.State full = new Kingdom.State(Kingdom.MAX_COFFER, Kingdom.MAX_APPROVAL, Kingdom.Work.MINING, Map.of());
        Kingdom.State day = Kingdom.advance(full, 1, true);
        assertEquals(Kingdom.MAX_COFFER - 75_000, day.coffer());
        assertEquals(320, day.stock().get("coal"));
        assertEquals(Kingdom.MAX_APPROVAL - Kingdom.DAILY_DECAY, day.approval());
        // Approval never decays below 25% and production scales with it.
        Kingdom.State month = Kingdom.advance(full, 400, true);
        assertEquals(Kingdom.MIN_APPROVAL, month.approval());
        assertTrue(month.stock().get("coal") < 320 * Kingdom.MAX_DAYS);
        assertEquals(Kingdom.Work.FISHING, Kingdom.reallocate(full).work());
        assertEquals(Kingdom.MAX_APPROVAL, Kingdom.work(full, 5).approval());
        for (Kingdom.Work w : Kingdom.Work.values()) {
            w.fullDay().keySet().forEach(item -> assertTrue(ItemCatalog.exists(item), w + " " + item));
        }
        assertTrue(Kingdom.collected(day).stock().isEmpty());
        assertNotNull(ContentRegistry.quest("throne_of_miscellania"));
        assertNotNull(ContentRegistry.quest("royal_trouble"));
    }
}
