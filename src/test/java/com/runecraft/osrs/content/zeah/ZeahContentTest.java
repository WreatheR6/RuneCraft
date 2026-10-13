package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.Modules;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Data integrity of the Zeah (Great Kourend and Varlamore) module. */
class ZeahContentTest {
    private final ZeahContent module = new ZeahContent();

    private static boolean placeExists(String id) {
        return ContentRegistry.place(id) != null || Landmark.byName(id) != null;
    }

    private static boolean creatureExists(String id) {
        return ContentRegistry.monster(id) != null || ContentRegistry.boss(id) != null
                || com.runecraft.osrs.data.Monster.byEntity(id) != null;
    }

    private static int rarity(List<Drop> drops, String item) {
        for (Drop d : drops) {
            if (d.item().equals(item)) {
                return d.rarity();
            }
        }
        return -1;
    }

    private static boolean blockExists(String id) {
        return id.contains(":") || ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(id));
    }

    @Test
    void moduleIsRegisteredAndIdsArePrefixed() {
        assertTrue(Modules.ALL.stream().anyMatch(m -> m.id().equals("zeah")));
        for (PlaceDef p : module.places()) {
            assertTrue(p.id().startsWith("zeah_"), p.id());
            assertNotNull(ContentRegistry.place(p.id()), p.id());
        }
        for (NpcDef n : module.npcs()) {
            assertTrue(n.id().startsWith("zeah_"), n.id());
            assertTrue(placeExists(n.place()), n.id() + " at " + n.place());
        }
        for (BlockDef b : module.blocks()) {
            assertTrue(b.id().startsWith("zeah_"), b.id());
        }
        for (StructureDef s : module.structures()) {
            assertTrue(s.id().startsWith("zeah_"), s.id());
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
    void placesAreAtTheirRealCoordinatesOnLand() {
        for (PlaceDef p : module.places()) {
            assertTrue(p.osrsX() >= 1150 && p.osrsX() <= 1850, p.id() + " x");
            assertTrue(p.osrsY() >= 2840 && p.osrsY() <= 3960, p.id() + " y");
            Region r = GielinorMap.regionAt(p.osrsX(), p.osrsY());
            assertTrue(r != Region.WILDERNESS && r != Region.KHARIDIAN_DESERT, p.id() + " in " + r);
            for (String block : p.blocks()) {
                assertTrue(blockExists(block), p.id() + " block " + block);
            }
        }
        PlaceDef castle = ContentRegistry.place(ZeahPlaces.KOUREND_CASTLE);
        assertTrue(Math.abs(castle.osrsX() - 1639) < 10 && Math.abs(castle.osrsY() - 3680) < 25);
        PlaceDef civitas = ContentRegistry.place(ZeahPlaces.CIVITAS);
        assertEquals(1680, civitas.osrsX());
        assertEquals(3130, civitas.osrsY());
        // Kourend lies north of Varlamore.
        assertTrue(ContentRegistry.place(ZeahPlaces.SHAYZIEN).osrsY() > ContentRegistry.place(ZeahPlaces.AUBURNVALE).osrsY());
    }

    @Test
    void placesHaveRoomForTheirPlazas() {
        List<PlaceDef> mine = module.places();
        Set<String> replaced = new HashSet<>();
        module.structures().forEach(s -> replaced.add(s.replacesLandmark()));
        for (PlaceDef a : mine) {
            for (PlaceDef b : ContentRegistry.places()) {
                if (a.id().equals(b.id()) || a.style() == PlaceDef.Style.NONE || b.style() == PlaceDef.Style.NONE) {
                    continue;
                }
                double d = Math.hypot(a.blockX() - b.blockX(), a.blockZ() - b.blockZ());
                assertTrue(d >= 32, a.id() + " too close to " + b.id() + " (" + d + ")");
            }
        }
    }

    @Test
    void campsAndTravelReferenceRealThings() {
        for (ZeahPlaces.Camp c : ZeahPlaces.camps()) {
            assertTrue(placeExists(c.place()), c.place());
            assertTrue(c.cap() > 0 && c.radius() > 0);
            for (String id : c.creatures()) {
                assertTrue(creatureExists(id), id);
            }
        }
        for (ZeahTravel.Network n : ZeahTravel.networks()) {
            assertTrue(n.stops().size() >= 2, n.id());
            Set<String> seen = new HashSet<>();
            for (ZeahTravel.Stop s : n.stops()) {
                assertTrue(placeExists(s.place()), n.id() + " stop " + s.place());
                assertTrue(seen.add(s.place()), n.id() + " duplicate stop " + s.place());
            }
        }
        for (ZeahTravel.Agent a : ZeahTravel.agents()) {
            NpcDef npc = ContentRegistry.npc(a.npc());
            assertNotNull(npc, a.npc());
            assertEquals(a.at(), npc.place(), a.npc());
            assertNotNull(a.network().stop(a.at()), a.npc() + " stands at a stop");
        }
        // Every minecart stop has a minecart.
        for (ZeahTravel.Stop s : ZeahTravel.MINECART.stops()) {
            PlaceDef p = ContentRegistry.place(s.place());
            assertTrue(p.blocks().contains(ZeahBlocks.MINECART), s.place() + " has no minecart");
        }
        // Veos sails from Port Sarim to both Kourend ports.
        assertEquals(List.of("PORT_SARIM", ZeahPlaces.PORT_PISCARILIUS, ZeahPlaces.LANDS_END),
                ZeahTravel.SHIP.stops().stream().map(ZeahTravel.Stop::place).toList());
        assertEquals(14, ZeahTravel.QUETZAL.stops().size());
    }

    @Test
    void npcDialogueExists() {
        for (NpcDef n : module.npcs()) {
            boolean travel = ZeahTravel.agent(n.id()) != null;
            assertTrue(travel || !ZeahNpcs.dialogue(n.id()).isEmpty(), n.id() + " has nothing to say");
        }
        assertFalse(ZeahNpcs.dialogue(ZeahNpcs.LADY_VULCANA).isEmpty());
    }

    @Test
    void hunterSpotsAreFaithful() {
        assertEquals(5, ZeahHunter.SPOTS.size());
        for (ZeahHunter.Spot s : ZeahHunter.SPOTS) {
            assertTrue(module.blocks().stream().anyMatch(b -> b.id().equals(s.blockId())), s.blockId());
            assertTrue(module.places().stream().anyMatch(p -> p.blocks().contains(s.blockId())), s.blockId() + " is never placed");
            for (String item : s.loot()) {
                assertTrue(ItemCatalog.exists(item), item);
            }
            for (String item : s.tools()) {
                assertTrue(ItemCatalog.exists(item), item);
            }
            assertTrue(s.consumed() == null || ItemCatalog.exists(s.consumed()), s.consumed());
        }
        ZeahHunter.Spot moon = ZeahHunter.spot(ZeahBlocks.MOONLIGHT_ANTELOPE_PIT);
        assertEquals(91, moon.level());
        assertEquals(450.0, moon.xp());
        assertEquals(72, ZeahHunter.spot(ZeahBlocks.SUNLIGHT_ANTELOPE_PIT).level());
        assertEquals(1.0, ZeahHunter.catchChance(99, ZeahHunter.spot(ZeahBlocks.JERBOA_BURROW)), 1e-9);
        assertEquals(143 / 256.0, ZeahHunter.catchChance(91, moon), 0.01);
    }

    @Test
    void catacombMonstersDropTotemPiecesAndShards() {
        for (String id : ZeahMonsters.CATACOMB_CREATURES) {
            MonsterDef m = ContentRegistry.monster(id);
            assertNotNull(m, id);
            assertTrue(m.underground, id);
            assertEquals(ZeahMonsters.shardRarity(m.combatLevel), rarity(m.drops, "ancient_shard"), id);
            for (String piece : new String[]{"dark_totem_base", "dark_totem_middle", "dark_totem_top"}) {
                assertEquals(ZeahMonsters.totemPieceRarity(m.combatLevel), rarity(m.drops, piece), id + " " + piece);
            }
        }
        assertEquals(1200, ZeahMonsters.totemPieceRarity(100));
        assertEquals(267, ZeahMonsters.shardRarity(100));
        // The dark totem assembled from the pieces is Skotizo's key (bosses module).
        assertEquals("dark_totem", ContentRegistry.boss("skotizo").key);
    }

    @Test
    void lizardmenAndCrabs() {
        MonsterDef brute = ContentRegistry.monster("lizardman_brute");
        assertEquals(75, brute.combatLevel); // OSRS Wiki (DPS calculator data)
        assertEquals(60, brute.hitpoints);
        assertEquals(250, rarity(brute.drops, "xerics_talisman_inert"));
        assertEquals("lizardmen", brute.slayerCategory);
        assertEquals(15, ContentRegistry.monster("sand_crab").combatLevel);
        assertEquals(60, ContentRegistry.monster("sand_crab").hitpoints);
        assertEquals(107, ContentRegistry.monster("king_sand_crab").combatLevel);
        Recipe charge = null;
        for (Recipe r : Recipes.all()) {
            if (r.outputs().containsKey("xerics_talisman") && r.inputs().containsKey("xerics_talisman_inert")) {
                charge = r;
            }
        }
        assertNotNull(charge);
        assertEquals(100, (int) charge.inputs().get("lizardman_fang"));
    }

    @Test
    void moonsOfPerilHaveTheirSets() {
        Map<String, List<String>> sets = Map.of("blue_moon", ZeahItems.BLUE_MOON, "eclipse_moon", ZeahItems.ECLIPSE_MOON,
                "blood_moon", ZeahItems.BLOOD_MOON);
        for (Map.Entry<String, List<String>> e : sets.entrySet()) {
            BossDef b = ContentRegistry.boss(e.getKey());
            assertNotNull(b, e.getKey());
            assertEquals(ZeahPlaces.MOONS_OF_PERIL, b.place);
            for (String item : e.getValue()) {
                assertTrue(rarity(b.drops, item) >= 24, item + " must reach the collection log");
                assertNotNull(ItemCatalog.get(item).equipment(), item);
            }
        }
        assertNotNull(ContentRegistry.monster(ContentRegistry.boss("blood_moon").minion));
        // Not duplicated: these bosses come from the bosses module.
        for (String id : new String[]{"sol_heredit", "the_hueycoatl", "amoxliatl", "skotizo", "sarachnis", "alchemical_hydra"}) {
            assertTrue(module.bosses().stream().noneMatch(b -> b.id.equals(id)), id);
            assertNotNull(ContentRegistry.boss(id), id);
        }
    }

    @Test
    void recipesUseKnownItems() {
        List<Recipe> mine = new ArrayList<>();
        module.recipes(mine::add);
        assertFalse(mine.isEmpty());
        for (Recipe r : mine) {
            r.inputs().keySet().forEach(i -> assertTrue(ItemCatalog.exists(i), r.id() + " input " + i));
            r.outputs().keySet().forEach(i -> assertTrue(ItemCatalog.exists(i), r.id() + " output " + i));
        }
    }

    // ------------------------------------------------------------------ structures

    /** A square XZ zone (block coordinates) owned by someone else's plaza or boss arena. */
    private record Zone(String owner, int cx, int cz, int r) {
        boolean intersects(int x1, int z1, int x2, int z2) {
            return x2 >= cx - r && x1 <= cx + r && z2 >= cz - r && z1 <= cz + r;
        }
    }

    private static List<Zone> foreignZones(Set<String> replaced) {
        List<Zone> zones = new ArrayList<>();
        for (PlaceDef p : ContentRegistry.places()) {
            if (!replaced.contains(p.id()) && p.style() != PlaceDef.Style.NONE) {
                int r = Math.max(5, 3 + (p.stations().size() + p.blocks().size()) / 2 + 2) + 2;
                zones.add(new Zone(p.id(), p.blockX(), p.blockZ(), r));
            }
        }
        for (Landmark l : Landmark.values()) {
            if (!replaced.contains(l.name())) {
                zones.add(new Zone(l.name(), l.blockX(), l.blockZ(), l.isAltar() ? 5 : 10));
            }
        }
        Map<String, Integer> arenaIndex = new HashMap<>();
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.raidOnly || b.place == null) {
                continue;
            }
            int cx;
            int cz;
            PlaceDef p = ContentRegistry.place(b.place);
            if (p != null) {
                cx = p.blockX();
                cz = p.blockZ();
            } else {
                Landmark l = Landmark.byName(b.place);
                if (l == null) {
                    continue;
                }
                cx = l.blockX();
                cz = l.blockZ();
            }
            int i = arenaIndex.merge(b.place, 1, Integer::sum) - 1;
            int r = b.arenaRadius;
            zones.add(new Zone(b.id + " arena", cx + (i % 2 == 0 ? 1 : -1) * (14 + r), cz + (i / 2) * (2 * r + 6), r + 2));
        }
        return zones;
    }

    @Test
    void structuresLeaveOtherPlazasAndArenasAlone() {
        Set<String> replaced = new HashSet<>();
        module.structures().forEach(s -> replaced.add(s.replacesLandmark()));
        List<Zone> zones = foreignZones(replaced);
        for (StructureDef s : module.structures()) {
            for (StructureDef.Op op : s.ops()) {
                boolean flatten = op.type() == StructureDef.Op.Type.FLATTEN;
                if (!flatten && op.y2() < StructureDef.GROUND - 1) {
                    continue; // underground (the Catacombs)
                }
                int m = op.margin();
                for (Zone z : zones) {
                    assertFalse(z.intersects(op.x1() - m, op.z1() - m, op.x2() + m, op.z2() + m),
                            s.id() + " " + op.type() + " at tiles " + GielinorMap.toOsrsX(op.x1()) + "," + GielinorMap.toOsrsY(op.z2())
                                    + ".." + GielinorMap.toOsrsX(op.x2()) + "," + GielinorMap.toOsrsY(op.z1())
                                    + " overlaps " + z.owner());
                }
            }
        }
    }

    @Test
    void structuresReplaceOwnPlacesAndContainTheirStations() {
        Set<String> mine = new HashSet<>();
        module.places().forEach(p -> mine.add(p.id()));
        Set<String> replaced = new HashSet<>();
        for (StructureDef s : module.structures()) {
            String target = s.replacesLandmark();
            assertNotNull(target, s.id());
            assertTrue(mine.contains(target), s.id() + " may only replace its own place, not " + target);
            assertTrue(replaced.add(target), target + " replaced twice");
            assertEquals(StructureDef.Op.Type.FLATTEN, s.ops().get(0).type(), s.id() + " must start by flattening");
            PlaceDef p = ContentRegistry.place(target);
            assertTrue(p.blockX() >= s.minX() && p.blockX() <= s.maxX() && p.blockZ() >= s.minZ() && p.blockZ() <= s.maxZ(),
                    s.id() + " does not cover its place");
            Set<String> used = new HashSet<>();
            for (StructureDef.Op op : s.ops()) {
                if (op.block() != null) {
                    String b = op.block();
                    used.add(b.contains("[") ? b.substring(0, b.indexOf('[')) : b);
                }
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
                    String id = b.substring("runecraft:".length());
                    boolean station = false;
                    for (Station st : Station.values()) {
                        station |= st.blockId().equals(id);
                    }
                    assertTrue(station || blockExists(id) || id.endsWith("_tree_log"), s.id() + " uses unknown block " + b);
                }
            }
        }
        for (String id : new String[]{ZeahPlaces.KOUREND_CASTLE, ZeahPlaces.ARCEUUS, ZeahPlaces.HOSIDIUS,
                ZeahPlaces.PORT_PISCARILIUS, ZeahPlaces.SHAYZIEN, ZeahPlaces.LOVAKENGJ, ZeahPlaces.CIVITAS}) {
            assertTrue(replaced.contains(id), id + " has no hand-built town centre");
        }
    }

    @Test
    void catacombsLieBeneathTheCastleAndHoldTheirCamp() {
        StructureDef castle = module.structures().stream().filter(s -> s.id().equals("zeah_kourend_castle")).findFirst().orElseThrow();
        ZeahPlaces.Camp camp = ZeahPlaces.camps().stream().filter(c -> !c.surface()).findFirst().orElseThrow();
        assertEquals(ZeahStructures.CATACOMBS_FLOOR_Y + 1, camp.floorY());
        assertTrue(camp.floorY() < StructureDef.GROUND - 15, "catacombs are deep underground");
        PlaceDef p = ContentRegistry.place(camp.place());
        int cx = p.blockX() + camp.dx();
        int cz = p.blockZ() + camp.dz();
        boolean covered = false;
        for (StructureDef.Op op : castle.ops()) {
            if (op.type() == StructureDef.Op.Type.CLEAR && op.y1() == camp.floorY() && op.x1() <= cx - camp.radius()
                    && op.x2() >= cx + camp.radius() && op.z1() <= cz - camp.radius() && op.z2() >= cz + camp.radius()) {
                covered = true;
            }
        }
        assertTrue(covered, "the catacomb camp must lie inside the carved crypt: " + cx + "," + cz + " r" + camp.radius() + " y" + camp.floorY());
        assertEquals(ZeahMonsters.CATACOMB_CREATURES, camp.creatures());
    }
}
