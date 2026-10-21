package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.islands.IslandsTravel;
import com.runecraft.osrs.content.npcs.Travel;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 40: the diary tasks wired to real mechanics (transport trips, fairy rings, altars, the memoirs, Tears of Guthix,
 * pumps and wells, the five shortcuts, gear/prayer/weapon conditions) and the reward fixes (teleport charges, the Karamja
 * lamps, the taskmasters, travel fares).
 */
class DiaryRound40Test {
    private static Diaries.Task byDesc(String prefix) {
        return Diaries.tasks().stream().filter(t -> t.description().startsWith(prefix)).findFirst().orElseThrow(() ->
                new AssertionError("no task " + prefix));
    }

    /** Every "travel:" key the game can fire: NPC routes, player-grown spirit trees and the diary transport objects. */
    static Set<String> travelKeys() {
        Set<String> keys = new HashSet<>();
        for (Travel.Route r : Travel.all()) {
            for (Travel.Dest d : r.dests()) {
                keys.add("travel:" + r.id() + ":" + Diaries.slug(d.name()));
            }
        }
        for (Travel.Dest d : Travel.spiritTreeDests(null)) {
            keys.add("travel:spirit_tree_grown:" + Diaries.slug(d.name()));
        }
        keys.addAll(DiaryTransport.keys());
        return keys;
    }

    @Test
    void eventTargetsGlobAndNeedEveryPart() {
        assertTrue(Diaries.eventMatches("travel:balloon_*:castle_wars", "travel:balloon_varrock:castle_wars"));
        assertFalse(Diaries.eventMatches("travel:balloon_*:castle_wars", "travel:balloon_varrock:entrana"));
        assertTrue(Diaries.eventMatches("travel:*:lands_end", "travel:charter_port_sarim:lands_end"));
        assertTrue(Diaries.eventMatches("a|b*", "bcd"));
        assertFalse(Diaries.eventMatches("ab", "abc"));
        assertEquals(0b101, Diaries.eventPartsMatched("x&y|z&x", "x"));
        assertEquals(0b111, Diaries.eventAllParts("x&y|z&x"));
        assertTrue(Diaries.globMatches("*", ""));
        assertTrue(Diaries.globMatches("a*b*c", "aXbYc"));
        assertFalse(Diaries.globMatches("a*b*c", "aXcYb"));
    }

    @Test
    void travelTasksNameTripsThatExist() {
        Set<String> keys = travelKeys();
        int n = 0;
        for (Diaries.Task t : Diaries.tasks()) {
            if (t.type() != Diaries.Type.EVENT || !t.target().contains("travel:")) {
                continue;
            }
            for (String part : t.target().split("&")) {
                boolean any = false;
                for (String alt : part.split("\\|")) {
                    any |= keys.stream().anyMatch(k -> Diaries.globMatches(alt, k));
                }
                assertTrue(any, t.id() + " (" + t.description() + "): no trip fires " + part);
            }
            n++;
        }
        assertTrue(n >= 18, "travel tasks: " + n);
        // Faithful now: the cart service, the gliders and the spirit tree stand-ins.
        for (String d : new String[]{"Use Vigroy and Hajedy's cart service", "Use the gnome glider to travel to Karamja",
                "Travel to the Gnome Stronghold by Spirit Tree", "Travel to the Feldip Hills by Gnome Glider"}) {
            assertFalse(Diaries.isApprox(byDesc(d)), d);
        }
        assertTrue(Diaries.eventMatches(byDesc("Travel to the Gnome Stronghold by Spirit Tree").target(),
                "travel:spirit_tree_grown:gnome_stronghold"));
    }

    @Test
    void fairyRingTasksNameRealCodes() {
        int n = 0;
        for (Diaries.Task t : Diaries.tasks()) {
            if (t.type() == Diaries.Type.EVENT && t.target().startsWith("fairy_ring:")) {
                String code = t.target().substring("fairy_ring:".length()).toUpperCase(java.util.Locale.ROOT);
                assertNotNull(IslandsTravel.FAIRY_RINGS.stop(code), t.id() + ": no fairy ring " + code);
                n++;
            }
        }
        assertEquals(5, n, "unicorn pen, Miscellania, McGrubor's Wood, Mount Karuulm, Wizards' Tower");
    }

    /** Altar tasks: a prayer altar block stands inside each restriction circle (structures as built). */
    @Test
    void altarTasksHaveAnAltar() {
        List<int[]> altars = new ArrayList<>();
        for (StructureDef s : ContentRegistry.structures()) {
            for (StructureDef.Op op : s.ops()) {
                if (op.block() != null && op.block().endsWith("prayer_altar") && op.type() != StructureDef.Op.Type.CLEAR) {
                    altars.add(new int[]{GielinorMap.toOsrsX(op.x1()), GielinorMap.toOsrsY(op.z1())});
                }
            }
        }
        // The Altar of Nature is a scripted block placed at its tile (DiaryActivities).
        altars.add(new int[]{DiaryActivities.NATURE_ALTAR_X, DiaryActivities.NATURE_ALTAR_Y});
        altars.add(new int[]{DiaryActivities.DEMONIC_RUINS_X, DiaryActivities.DEMONIC_RUINS_Y});
        altars.add(new int[]{DiaryActivities.SARIM_CHURCH_X, DiaryActivities.SARIM_CHURCH_Y});
        int n = 0;
        for (Diaries.Task t : Diaries.tasks()) {
            if (t.type() == Diaries.Type.EVENT && (t.target().equals("altar_pray") || t.target().startsWith("altar_restore"))) {
                assertNotNull(t.restrict(), t.id());
                int[] c = Diaries.visitTarget(t.restrict());
                assertTrue(altars.stream().anyMatch(a -> Teleports.within(a[0], a[1], c[0], c[1], c[2])),
                        t.id() + " (" + t.description() + "): no altar within " + t.restrict());
                n++;
            }
        }
        assertTrue(n >= 7, "altar tasks: " + n);
    }

    @Test
    void conditionsNameRealGearPrayersAndWeapons() {
        Set<String> prayers = new HashSet<>();
        for (Prayer p : Prayer.values()) {
            prayers.add(p.name());
        }
        int n = 0;
        for (Diaries.Task t : Diaries.tasks()) {
            for (String worn : Diaries.wornReq(t)) {
                for (String piece : worn.split("&")) {
                    assertTrue(DiaryCompletabilityTest.itemTargetExists(piece), t.id() + " wears no item " + piece);
                }
                n++;
            }
            if (Diaries.prayerReq(t) != null) {
                assertTrue(prayers.contains(Diaries.prayerReq(t)), t.id() + " prays " + Diaries.prayerReq(t));
                n++;
            }
            if (Diaries.wieldReq(t) != null) {
                assertTrue(DiaryCompletabilityTest.itemTargetExists(Diaries.wieldReq(t)), t.id() + " wields " + Diaries.wieldReq(t));
                n++;
            }
        }
        assertTrue(n >= 15, "conditions: " + n);
        Diaries.Task barrows = byDesc("Loot the Barrows chest");
        assertEquals(6, Diaries.wornReq(barrows).size());
        assertTrue(Diaries.wearingAll(Diaries.wornReq(barrows).get(1),
                List.of("dharoks_helm", "dharoks_platebody", "dharoks_platelegs", "dharoks_greataxe")));
        assertTrue(Diaries.itemMatches(Diaries.wieldReq(byDesc("Kill an Elf with a Crystal bow")), "crystal_bow_attuned"));
        assertTrue(Diaries.itemMatches(Diaries.wieldReq(byDesc("Kill an Elf with a Crystal bow")), "bow_of_faerdhinen"));
        assertTrue(Diaries.itemMatches(Diaries.wieldReq(byDesc("Defeat a Locust Rider with Keris")), "keris_partisan"));
        assertEquals("SMITE", Diaries.prayerReq(byDesc("Recharge your prayer at the Emir's Arena")));
        assertEquals("PIETY", Diaries.prayerReq(byDesc("Enter the Seers' Village courthouse")));
    }

    @Test
    void memoirsNeedAllFiveCities() {
        Diaries.Task memoirs = byDesc("Use Kharedst's memoirs to teleport to all five cities");
        assertFalse(Diaries.isApprox(memoirs));
        assertEquals((1 << Teleports.MEMOIRS.size()) - 1, Diaries.eventAllParts(memoirs.target()));
        int seen = 0;
        for (Teleports.Destination d : Teleports.MEMOIRS) {
            String book = seen == 0 ? "book_of_the_dead" : "kharedsts_memoirs";
            seen |= Diaries.eventPartsMatched(memoirs.target(), Diaries.teleportKey(book, null, d.name()));
        }
        assertEquals(Diaries.eventAllParts(memoirs.target()), seen);
    }

    @Test
    void approxStandInsShrank() {
        long approx = Diaries.tasks().stream().filter(Diaries::isApprox).count();
        assertTrue(approx <= 12, "approx tasks: " + approx);
        // The Arceuus lectern: every tablet it makes is a real tablet of an Arceuus teleport (Ape Atoll: 90 Magic, 100 XP, wiki).
        for (Map.Entry<String, String> e : DiaryActivities.LECTERN_TABLETS.entrySet()) {
            assertNotNull(Spellbooks.tablet(e.getKey()), e.getKey());
            Spellbooks.Spell spell = Spellbooks.get(e.getValue());
            assertNotNull(spell, e.getValue());
            assertEquals(Spellbooks.Book.ARCEUUS, spell.book(), e.getValue());
        }
        Spellbooks.Spell ape = Spellbooks.get(DiaryActivities.LECTERN_TABLETS.get("ape_atoll_teleport_tablet"));
        assertEquals(90, ape.level());
        assertEquals(100, ape.xp());
        Diaries.Task tablet = byDesc("Create an Ape Atoll teleport tablet");
        assertEquals("lectern:ape_atoll_teleport_tablet", tablet.target());
        assertFalse(Diaries.isApprox(tablet));
        assertNotNull(ItemCatalog.get(DiaryActivities.DARK_ESSENCE_BLOCK));
        assertFalse(Diaries.isApprox(byDesc("Collect at least 100 Tears of Guthix")));
        assertEquals("tears_of_guthix:100", byDesc("Collect at least 100 Tears of Guthix").target());
        assertFalse(Diaries.isApprox(byDesc("Have the Mage of Zamorak teleport you to the Abyss")));
        assertTrue(Diaries.questReqs(byDesc("Have the Mage of Zamorak teleport you to the Abyss")).contains("enter_the_abyss"));
        for (String d : new String[]{"Fill a bucket from the pump", "Fill a bucket with water at the Rellekka well"}) {
            Diaries.Task t = byDesc(d);
            assertFalse(Diaries.isApprox(t));
            assertEquals("fill:water_bucket", t.target());
        }
        assertTrue(Diaries.restrictContains(byDesc("Fill a bucket from the pump").restrict(), null, DiaryExtras.FALADOR_PUMP_X,
                DiaryExtras.FALADOR_PUMP_Y));
        assertTrue(Diaries.restrictContains(byDesc("Fill a bucket with water at the Rellekka well").restrict(), null,
                DiaryExtras.RELLEKKA_WELL_X, DiaryExtras.RELLEKKA_WELL_Y));
        assertEquals("minecraft:water_bucket", DiaryExtras.waterFill("minecraft:bucket"));
        assertNull(DiaryExtras.waterFill("bucket_of_sand"));
    }

    @Test
    void rewardTeleportsAndLamps() {
        // Morytania legs 1: two daily Ectofuntus teleports; Desert amulet 3 keeps amulet 2's single Nardah teleport.
        assertEquals(2, DiaryRewards.teleports("morytania", 1).get(0).perDay(1));
        DiaryRewards.Tele nardah = DiaryRewards.teleports("desert", 3).get(0);
        assertEquals("Nardah", nardah.dest().name());
        assertEquals(1, nardah.perDay(3));
        assertEquals(0, nardah.perDay(4));
        // Karamja's easy/medium/hard lamps: 1,000 XP at any level, 5,000 at 30, 10,000 at 40; elite is the standard 50,000.
        assertEquals("antique_lamp_karamja_easy", DiaryRewards.lampFor("karamja", Diaries.Tier.EASY));
        assertEquals("antique_lamp_elite", DiaryRewards.lampFor("karamja", Diaries.Tier.ELITE));
        assertEquals("antique_lamp_hard", DiaryRewards.lampFor("varrock", Diaries.Tier.HARD));
        assertEquals(1000, DiaryRewards.lamp("karamja_easy")[0]);
        assertEquals(1, DiaryRewards.lamp("karamja_easy")[1]);
        assertEquals(5000, DiaryRewards.lamp("karamja_medium")[0]);
        assertEquals(30, DiaryRewards.lamp("karamja_medium")[1]);
        assertEquals(10000, DiaryRewards.lamp("karamja_hard")[0]);
        assertEquals(40, DiaryRewards.lamp("karamja_hard")[1]);
        assertEquals(50000, DiaryRewards.lamp("elite")[0]);
        assertEquals(70, DiaryRewards.lamp("elite")[1]);
        assertNull(DiaryRewards.lamp("nonsense"));
        for (String key : DiaryRewards.LAMP_KEYS) {
            assertNotNull(ItemCatalog.get(DiaryRewards.LAMP_PREFIX + key), key);
        }
    }

    @Test
    void travelFaresAfterDiaryPerks() {
        DiaryRewards.TierDone none = (a, t) -> false;
        DiaryRewards.TierDone all = (a, t) -> true;
        assertEquals(15, DiaryRewards.travelFare("boat_brimhaven_ardougne", "Ardougne", 30, true, none));
        assertEquals(30, DiaryRewards.travelFare("boat_brimhaven_ardougne", "Rimmington", 30, true, none));
        assertEquals(15, DiaryRewards.travelFare("boat_port_sarim_karamja", "Musa Point, Karamja", 30, true, none));
        assertEquals(30, DiaryRewards.travelFare("boat_port_sarim_karamja", "Musa Point, Karamja", 30, false, none));
        assertEquals(0, DiaryRewards.travelFare("carpet_shantay", "Pollnivneach", 200, false, all));
        assertEquals(200, DiaryRewards.travelFare("carpet_shantay", "Pollnivneach", 200, false, none));
        assertEquals(0, DiaryRewards.travelFare("cart_brimhaven_shilo", "Shilo Village", 200, false, all));
        assertEquals(200, DiaryRewards.travelFare("cart_brimhaven_shilo", "Shilo Village", 200, false, none));
        assertNotNull(Travel.get("cart_brimhaven_shilo"));
        assertNotNull(Travel.get("boat_swamp_hollows"));
        assertNotNull(Travel.get("boat_lady_of_the_waves"));
    }

    @Test
    void taskmastersStandAtTheirTiles() {
        Map<String, NpcDef> npcs = new java.util.HashMap<>();
        for (NpcDef n : ContentRegistry.npcs()) {
            npcs.put(n.id(), n);
        }
        Set<String> areas = new HashSet<>();
        for (DiaryExtras.Taskmaster t : DiaryExtras.TASKMASTERS) {
            assertNotNull(Diaries.area(t.area()), t.area());
            areas.add(t.area());
            assertNotNull(npcs.get(t.npcId()), "no NPC " + t.npcId());
            assertEquals(t.area(), DiaryExtras.taskmasterArea(t.npcId()));
            assertEquals(t.npcId(), DiaryExtras.taskmaster(t.area()));
            if (!t.npcId().startsWith("mg_")) {
                PlaceDef p = ContentRegistry.place(t.npcId());
                assertNotNull(p, "no place for " + t.npcId());
                assertEquals(t.x(), p.osrsX());
                assertEquals(t.y(), p.osrsY());
            }
        }
        assertEquals(12, areas.size());
    }

    /** The block a dungeon's ops leave at a point ("air" if cleared last) and whether something solid lies under it. */
    private static String[] blockAt(int x, int y, int z) {
        String last = null;
        String below = null;
        for (com.runecraft.osrs.content.dungeons.Dungeons.Dungeon d : com.runecraft.osrs.content.dungeons.Dungeons.all()) {
            for (StructureDef.Op op : d.structure().ops()) {
                if (op.type() == StructureDef.Op.Type.FLATTEN || op.type() == StructureDef.Op.Type.REPLACE) {
                    continue;
                }
                boolean xz = x >= op.x1() && x <= op.x2() && z >= op.z1() && z <= op.z2();
                if (op.type() == StructureDef.Op.Type.WALLS) {
                    xz &= x == op.x1() || x == op.x2() || z == op.z1() || z == op.z2();
                }
                if (xz && y >= op.y1() && y <= op.y2()) {
                    last = op.type() == StructureDef.Op.Type.CLEAR ? "air" : op.block();
                }
                if (xz && y - 1 >= op.y1() && y - 1 <= op.y2()) {
                    below = op.type() == StructureDef.Op.Type.CLEAR ? null : op.block();
                }
            }
        }
        return new String[]{last, below};
    }

    /** Round 40's dungeon shortcut ends and the Dorgesh-Kaan train stand in carved air on a floor. */
    @Test
    void dungeonSpotsStandInCarvedAir() {
        List<int[]> spots = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (com.runecraft.osrs.content.skilling.AgilityShortcuts.BlockEnds e
                : com.runecraft.osrs.content.skilling.AgilityShortcuts.DUNGEON_ENDS) {
            spots.add(new int[]{e.x1(), e.y1(), e.z1()});
            spots.add(new int[]{e.x2(), e.y2(), e.z2()});
            names.add(e.id() + " a");
            names.add(e.id() + " b");
        }
        DiaryTransport.Stop dk = DiaryTransport.stop("dorgesh_kaan");
        spots.add(dk.under());
        names.add("Dorgesh-Kaan train");
        for (int i = 0; i < spots.size(); i++) {
            int[] p = spots.get(i);
            String[] at = blockAt(p[0], p[1], p[2]);
            assertEquals("air", at[0], names.get(i) + " at " + java.util.Arrays.toString(p) + " is in " + at[0]);
            assertNotNull(at[1], names.get(i) + " has no floor");
            String[] head = blockAt(p[0], p[1] + 1, p[2]);
            assertEquals("air", head[0], names.get(i) + " has no head room");
        }
    }

    @Test
    void transportStopsAndTrips() {
        for (DiaryTransport.Stop s : DiaryTransport.STOPS) {
            if (s.under() == null) {
                assertTrue(DiaryCompletabilityTest.onLand(s.x(), s.y(), 2), s.id() + " stands at sea");
            }
            assertTrue(!DiaryTransport.from(s.id()).isEmpty(), s.id() + " goes nowhere");
            assertEquals(s, DiaryTransport.at(s.block(), s.blockX(), s.under() != null ? s.under()[1] : 64, s.blockZ()));
        }
        for (DiaryTransport.Link l : DiaryTransport.LINKS) {
            assertNotNull(ContentRegistry.quest(l.quest()), l.quest());
            assertTrue(l.item() == null || ItemCatalog.get(l.item()) != null, l.item());
        }
        assertTrue(DiaryTransport.keys().contains("travel:eagle_eagles_peak:feldip_hills"));
        assertTrue(DiaryTransport.keys().contains("travel:eagle_eagles_peak:desert"));
        assertTrue(DiaryTransport.keys().contains("travel:eagle_eagles_peak:rellekka_hunter_area"));
        assertTrue(DiaryTransport.keys().contains("travel:train_dorgesh_kaan:keldagrim"));
        for (String d : new String[]{"Travel to the Desert via the Eagle", "Travel to the Snowy Hunter Area via Eagle",
                "Travel from Eagles' Peak to the Feldip Hills by Eagle", "Take the train from Dorgesh-Kaan to Keldagrim"}) {
            assertFalse(Diaries.isApprox(byDesc(d)), d);
        }
    }

    @Test
    void groundSpawnsFeedTheirTasks() {
        // Seaweed: at least five spots on Karamja's land (spots this world puts in the sea stay empty).
        Diaries.Area karamja = Diaries.area("karamja");
        int land = 0;
        for (int[] t : DiaryExtras.SEAWEED.tiles()) {
            boolean onLand = !com.runecraft.osrs.world.Elevation.isOcean(GielinorMap.toBlockX(t[0]), GielinorMap.toBlockZ(t[1]));
            String region = GielinorMap.regionAtBlock(GielinorMap.toBlockX(t[0]), GielinorMap.toBlockZ(t[1])).name();
            if (onLand && karamja.contains(region, t[0], t[1])) {
                land++;
            }
        }
        assertTrue(land >= 5, "seaweed spots on Karamja's land: " + land);
        Diaries.Task seaweed = byDesc("Collect 5 seaweed from anywhere on Karamja");
        assertFalse(Diaries.isApprox(seaweed));
        assertEquals("seaweed", seaweed.target());
        // Potato cacti: in the carved Kalphite Lair, inside the task's underground circle.
        Diaries.Task cacti = byDesc("Collect 5 potato cacti from the Kalphite Hive");
        assertFalse(Diaries.isApprox(cacti));
        assertEquals(Diaries.Type.OBTAIN, cacti.type());
        assertEquals("potato_cactus", cacti.target());
        assertTrue(cacti.restrict().endsWith(",u"));
        for (int[] b : DiaryExtras.POTATO_CACTUS.blocks()) {
            assertTrue(Diaries.restrictContains(cacti.restrict(), null, GielinorMap.toOsrsX(b[0]), GielinorMap.toOsrsY(b[2])),
                    java.util.Arrays.toString(b));
            assertTrue(b[1] < Diaries.UNDERGROUND_BELOW_Y);
            String[] at = blockAt(b[0], b[1], b[2]);
            assertEquals("air", at[0], "cactus spot " + java.util.Arrays.toString(b) + " is in " + at[0]);
            assertNotNull(at[1], "cactus spot " + java.util.Arrays.toString(b) + " has no floor");
        }
        for (DiaryExtras.GroundSpawn g : DiaryExtras.GROUND_SPAWNS) {
            assertNotNull(ItemCatalog.get(g.item()), g.item());
            assertEquals(g, DiaryExtras.groundSpawn(g.block()));
            assertEquals(100, g.respawnTicks());
        }
    }

    @Test
    void newShortcutsExist() {
        for (String id : new String[]{"dwarven_mine_narrow_crevice", "taverley_dungeon_strange_floor", "edgeville_dungeon_pipe",
                "zanaris_jutting_wall", "brimhaven_ropeswing"}) {
            boolean surface = com.runecraft.osrs.content.skilling.AgilityShortcuts.SHORTCUTS.stream().anyMatch(s -> s.id().equals(id));
            boolean indoor = com.runecraft.osrs.content.skilling.AgilityShortcuts.indoor(id) != null
                    && com.runecraft.osrs.content.skilling.AgilityShortcuts.DUNGEON_ENDS.stream().anyMatch(e -> e.id().equals(id));
            assertTrue(surface || indoor, id);
            assertTrue(Diaries.tasks().stream().anyMatch(t -> ("shortcut:" + id).equals(t.target())), "no task uses " + id);
        }
    }

    @Test
    void chompyHatsAndTheirTasks() {
        // Wiki "Chompy bird hunting": 18 hats from 30 to 4,000 kills, in order.
        assertEquals(18, DiaryActivities.HATS.size());
        assertEquals(30, DiaryActivities.HATS.get(0).kills());
        assertEquals("chompy_bird_hat_ogre_bowman", DiaryActivities.HATS.get(0).item());
        assertEquals(4000, DiaryActivities.HATS.get(17).kills());
        for (int i = 1; i < DiaryActivities.HATS.size(); i++) {
            assertTrue(DiaryActivities.HATS.get(i).kills() > DiaryActivities.HATS.get(i - 1).kills());
        }
        for (DiaryActivities.Hat h : DiaryActivities.HATS) {
            assertNotNull(ItemCatalog.get(h.item()), h.item());
            assertNotNull(ItemCatalog.get(h.item()).equipment(), h.item() + " is worn on the head");
        }
        assertNull(DiaryActivities.bestHat(29));
        assertEquals("chompy_bird_hat_marksman", DiaryActivities.bestHat(125).item());
        assertEquals("chompy_bird_hat_ogre_forester", DiaryActivities.bestHat(300).item());
        assertEquals("chompy_bird_hat_ogre_expert", DiaryActivities.bestHat(1000).item());
        assertEquals(List.of("chompy_hat"), DiaryActivities.hatKeys(124));
        assertEquals(List.of("chompy_hat", "chompy_hat:125", "chompy_hat:300"), DiaryActivities.hatKeys(999));
        // The four Western Provinces tasks: any hat (30 kills), then 125, 300 and 1,000 kills.
        String[][] tiers = {{"Claim any Chompy bird hat", "chompy_hat"}, {"Claim a Chompy bird hat from Rantz after registering at least 125",
                "chompy_hat:125"}, {"Claim a Chompy bird hat from Rantz after registering at least 300", "chompy_hat:300"},
                {"Claim a Chompy bird hat from Rantz after registering at least 1,000", "chompy_hat:1000"}};
        for (int i = 0; i < tiers.length; i++) {
            Diaries.Task t = byDesc(tiers[i][0]);
            assertEquals("western", t.area());
            assertEquals(Diaries.Tier.values()[i], t.tier());
            assertEquals(tiers[i][1], t.target());
            assertFalse(Diaries.isApprox(t));
            assertTrue(Diaries.questReqs(t).contains("big_chompy_bird_hunting"));
        }
        // Rewards: two chompies 25% / 50% (medium and hard) / always (elite); Rantz's daily arrows 25 / 50 / 100 / 150.
        assertEquals(List.of(0, 25, 50, 50, 100), List.of(DiaryActivities.twoChompiesPercent(0), DiaryActivities.twoChompiesPercent(1),
                DiaryActivities.twoChompiesPercent(2), DiaryActivities.twoChompiesPercent(3), DiaryActivities.twoChompiesPercent(4)));
        assertEquals(List.of(0, 25, 50, 100, 150), List.of(DiaryActivities.rantzDailyArrows(0), DiaryActivities.rantzDailyArrows(1),
                DiaryActivities.rantzDailyArrows(2), DiaryActivities.rantzDailyArrows(3), DiaryActivities.rantzDailyArrows(4)));
        // The five hunting ponds lie on land in the Feldip Hills and the Chompy Marsh.
        for (int[] pond : DiaryActivities.PONDS) {
            assertTrue(DiaryActivities.pondAt(pond[0], pond[1]) >= 0);
            assertFalse(com.runecraft.osrs.world.Elevation.isOcean(GielinorMap.toBlockX(pond[0]), GielinorMap.toBlockZ(pond[1])));
        }
        assertEquals(-1, DiaryActivities.pondAt(3222, 3218));
        assertNotNull(ContentRegistry.boss(DiaryActivities.CHOMPY), "the chompy bird");
        for (String item : new String[]{DiaryActivities.BELLOWS, DiaryActivities.BLOATED_TOAD, "ogre_bow", "ogre_arrow"}) {
            assertNotNull(ItemCatalog.get(item), item);
        }
    }

    @Test
    void templeKnightArmourTasks() {
        // Wiki: Initiate 20 Defence / 10 Prayer, Proselyte 30 / 20; values 10,000 / 8,000 and 8,000 / 12,000 / 10,000.
        assertEquals(6, DiaryActivities.TEMPLE_ARMOUR.size());
        for (DiaryActivities.TempleArmour a : DiaryActivities.TEMPLE_ARMOUR) {
            com.runecraft.osrs.ItemDef def = ItemCatalog.get(a.item());
            assertNotNull(def, a.item());
            assertNotNull(def.equipment(), a.item());
            assertEquals(a.item().startsWith("initiate") ? 20 : 30, def.equipment().requirements().get(com.runecraft.osrs.Skill.DEFENCE));
            assertEquals(a.item().startsWith("initiate") ? 10 : 20, def.equipment().requirements().get(com.runecraft.osrs.Skill.PRAYER));
            assertTrue(a.stats().prayer() >= 4, a.item());
        }
        assertEquals(12000, DiaryActivities.TEMPLE_ARMOUR.stream().filter(a -> a.item().equals("proselyte_hauberk")).findFirst()
                .orElseThrow().value());
        Diaries.Task initiate = byDesc("Pray at the Altar of Guthix in Taverley whilst wearing full Initiate");
        Diaries.Task proselyte = byDesc("Recharge your prayer in the Port Sarim church while wearing full Proselyte");
        for (Diaries.Task t : List.of(initiate, proselyte)) {
            assertFalse(Diaries.isApprox(t), t.id());
            assertEquals(Diaries.Type.EVENT, t.type());
            assertEquals("altar_pray", t.target());
        }
        assertEquals(List.of(DiaryActivities.INITIATE_SET), Diaries.wornReq(initiate));
        assertEquals(List.of(DiaryActivities.PROSELYTE_SET), Diaries.wornReq(proselyte));
        assertTrue(Diaries.wearingAll(DiaryActivities.PROSELYTE_SET, List.of("proselyte_sallet", "proselyte_hauberk", "proselyte_tasset")));
        assertFalse(Diaries.wearingAll(DiaryActivities.PROSELYTE_SET, List.of("proselyte_sallet", "proselyte_hauberk")));
        assertTrue(Diaries.wearingAll(DiaryActivities.INITIATE_SET, List.of("initiate_sallet", "initiate_hauberk", "initiate_cuisse")));
        assertNotNull(ItemCatalog.get("initiate_sallet"));
        assertTrue(Diaries.restrictContains(proselyte.restrict(), null, DiaryActivities.SARIM_CHURCH_X, DiaryActivities.SARIM_CHURCH_Y));
        assertTrue(Diaries.questReqs(proselyte).contains(DiaryActivities.SLUG_MENACE));
        assertNotNull(ContentRegistry.npc(DiaryActivities.SIR_TIFFY));
    }

    @Test
    void shopAndServiceTasksTrackTheAction() {
        // Former "stand near it" tasks now need the shop opened / the item bought / the service used.
        String[][] events = {{"View Aleck's Hunter Emporium in Yanille", "shop_open:alecks_hunter_emporium"},
                {"Browse Thessalia's store", "shop_open:thessalias_fine_clothes"},
                {"Buy a candle from the candle maker in Catherby", "shop_buy:candle_shop:candle"},
                {"Buy a stew from the Seers' Village pub", "shop_buy:foresters_arms:stew"}};
        for (String[] e : events) {
            Diaries.Task t = byDesc(e[0]);
            assertEquals(Diaries.Type.EVENT, t.type(), t.id());
            assertEquals(e[1], t.target(), t.id());
            String[] parts = e[1].split(":");
            com.runecraft.osrs.content.npcs.ShopDef shop = com.runecraft.osrs.content.npcs.Shops.get(parts[1]);
            assertNotNull(shop, parts[1]);
            if (parts.length > 2) {
                assertTrue(shop.stock().stream().anyMatch(s -> s.item().equals(parts[2])), parts[1] + " sells no " + parts[2]);
            }
        }
        // Talking to the NPC (it exists) and switching spellbooks at Tyss's altar.
        String[][] talks = {{"Learn your age from Hans in Lumbridge", "hans"},
                {"Speak to Sherlock between Seers' Village and McGrubor's Wood", "clue_sherlock"},
                {"Talk to the Emblem Trader in Edgeville about emblems", "npc_emblem_trader"}};
        for (String[] t : talks) {
            assertEquals("talk:" + t[1], byDesc(t[0]).target());
            assertNotNull(ContentRegistry.npc(t[1]), t[1]);
        }
        assertEquals("spellbook:arceuus", byDesc("Switch to the Necromancy spellbook at Tyss").target());
        assertEquals("travel:boat_port_sarim_entrana:entrana", byDesc("Take the boat to Entrana").target());
        Diaries.Task ruins = byDesc("Restore some Prayer points at the demonic ruins");
        assertEquals("altar_restore", ruins.target());
        assertTrue(ContentRegistry.places().stream().anyMatch(p -> p.id().equals(DiaryActivities.DEMONIC_RUINS)));
        Diaries.Task tan = byDesc("Have Sbott in Canifis tan something for you");
        assertEquals(Diaries.Type.MAKE, tan.type());
        assertTrue(Diaries.itemMatches(tan.target(), "hard_leather") && Diaries.itemMatches(tan.target(), "green_dragon_leather")
                && Diaries.itemMatches(tan.target(), "snakeskin"));
        Diaries.Task plank = byDesc("Make a normal plank at the Sawmill");
        assertEquals(Diaries.Type.OBTAIN, plank.type());
        assertEquals("plank", plank.target());
        assertTrue(Diaries.restrictContains(plank.restrict(), null, DiaryExtras.SAWMILL_X, DiaryExtras.SAWMILL_Y));
    }

    @Test
    void smallActivitiesFeedTheirTasks() {
        // Each former stand-in now fires the real action.
        Object[][] wired = {{"Steal a feather from the Desert Phoenix", "phoenix_feather"}, {"Give a dog a bone!", "dog_bone"},
                {"Select a colour for your kitten", "kitten_colour"}, {"Restore your prayer points at the nature altar", "altar_restore"},
                {"Pray at the Altar of Nature with Piety activated", "altar_pray"},
                {"Perform the 4 emotes from the Stronghold of Security", "emote:flap&emote:slap_head&emote:idea&emote:stamp"},
                {"Exchange gems for a machete", "machete_trade"}, {"Turn your small crystal seed into a Crystal saw", "crystal_saw"}};
        for (Object[] w : wired) {
            Diaries.Task t = byDesc((String) w[0]);
            assertEquals(Diaries.Type.EVENT, t.type(), t.id());
            assertEquals(w[1], t.target(), t.id());
            assertFalse(Diaries.isApprox(t), t.id());
        }
        assertEquals("PIETY", Diaries.prayerReq(byDesc("Pray at the Altar of Nature with Piety activated")));
        assertTrue(Diaries.questReqs(byDesc("Restore your prayer points at the nature altar")).contains("nature_spirit"));
        assertTrue(Diaries.restrictContains(byDesc("Restore your prayer points at the nature altar").restrict(), null,
                DiaryActivities.NATURE_ALTAR_X, DiaryActivities.NATURE_ALTAR_Y));
        Diaries.Task fruit = byDesc("Pick a White tree fruit");
        assertEquals(Diaries.Type.OBTAIN, fruit.type());
        assertEquals(DiaryActivities.WHITE_TREE_FRUIT, fruit.target());
        assertFalse(Diaries.isApprox(fruit));
        assertTrue(Diaries.restrictContains(fruit.restrict(), null, DiaryActivities.WHITE_TREE_X, DiaryActivities.WHITE_TREE_Y));
        // The phoenix's feather: 25 Thieving and 26 XP (wiki); the white tree: 12 Farming XP, 4 fruit, one per ~5 minutes.
        assertEquals(0, DiaryActivities.phoenixGrabChance(24));
        assertTrue(DiaryActivities.phoenixGrabChance(25) > 0);
        assertEquals(26, DiaryActivities.PHOENIX_XP);
        assertEquals(12, DiaryActivities.WHITE_TREE_XP);
        assertEquals(1, DiaryActivities.whiteTreeFruit(0, DiaryActivities.WHITE_TREE_REGROW_TICKS));
        assertEquals(4, DiaryActivities.whiteTreeFruit(2, 100L * DiaryActivities.WHITE_TREE_REGROW_TICKS));
        assertEquals(3, DiaryActivities.whiteTreeFruit(3, 10));
        // The creatures and objects exist where the tasks happen.
        for (String m : new String[]{DiaryActivities.STRAY_DOG, DiaryActivities.PHOENIX}) {
            assertNotNull(ContentRegistry.monster(m), m);
        }
        long dogs = com.runecraft.osrs.world.SpawnTable.all().stream().filter(p -> p.monster().equals(DiaryActivities.STRAY_DOG)).count();
        assertEquals(5, dogs, "Varrock's five stray dogs");
        assertTrue(com.runecraft.osrs.world.SpawnTable.all().stream().anyMatch(p -> p.monster().equals(DiaryActivities.PHOENIX)
                && Teleports.within(p.x(), p.y(), DiaryActivities.PHOENIX_X, DiaryActivities.PHOENIX_Y, 6)));
        Set<String> places = new HashSet<>();
        for (PlaceDef p : ContentRegistry.places()) {
            places.add(p.id());
        }
        assertTrue(places.contains(DiaryActivities.WHITE_TREE));
        assertTrue(places.contains(DiaryActivities.NATURE_ALTAR));
        for (String item : new String[]{DiaryActivities.PHOENIX_FEATHER, DiaryActivities.WHITE_TREE_FRUIT, DiaryActivities.KITTEN,
                "ring_of_charos"}) {
            assertNotNull(ItemCatalog.get(item), item);
        }
        assertEquals(6, DiaryActivities.KITTEN_COLOURS.size());
        // Safta Doc's machetes: 300 / 600 / 1,200 trading sticks with three gems and a gout tuber (wiki).
        assertNotNull(ContentRegistry.npc(DiaryActivities.SAFTA_DOC));
        assertTrue(places.contains(DiaryActivities.SAFTA_DOC));
        assertEquals(List.of(300, 600, 1200), DiaryActivities.MACHETES.stream().map(DiaryActivities.Machete::sticks).toList());
        for (DiaryActivities.Machete m : DiaryActivities.MACHETES) {
            assertNotNull(ItemCatalog.get(m.item()), m.item());
            assertNotNull(ItemCatalog.get("uncut_" + m.gem()), m.gem());
        }
        for (String item : new String[]{DiaryActivities.GOUT_TUBER, DiaryActivities.TRADING_STICKS}) {
            assertNotNull(ItemCatalog.get(item), item);
        }
        assertTrue(Diaries.questReqs(byDesc("Exchange gems for a machete")).contains("jungle_potion"));
        assertTrue(Diaries.questReqs(byDesc("Turn your small crystal seed into a Crystal saw")).contains("the_eyes_of_glouphrie"));
        for (String item : new String[]{DiaryActivities.CRYSTAL_SAW_SEED, DiaryActivities.CRYSTAL_SAW, DiaryActivities.SWAMP_TOAD,
                DiaryActivities.TOADS_LEGS}) {
            assertNotNull(ItemCatalog.get(item), item);
        }
        // Swamp toads: picked up at the Gnome Stronghold, every spot inside the task's circle and on land.
        Diaries.Task toad = byDesc("Collect a swamp toad at the Gnome Stronghold");
        assertEquals(Diaries.Type.OBTAIN, toad.type());
        assertEquals(DiaryActivities.SWAMP_TOAD, toad.target());
        assertEquals(DiaryActivities.SWAMP_TOAD, DiaryExtras.SWAMP_TOAD.item());
        assertEquals(25, DiaryExtras.SWAMP_TOAD.tiles().size());
        for (int[] t : DiaryExtras.SWAMP_TOAD.tiles()) {
            assertTrue(Diaries.restrictContains(toad.restrict(), null, t[0], t[1]), java.util.Arrays.toString(t));
        }
        assertEquals(List.of("Flap", "Slap Head", "Idea", "Stamp"), DiaryActivities.STRONGHOLD_EMOTES);
        for (String e : DiaryActivities.STRONGHOLD_EMOTES) {
            assertFalse(DiaryActivities.BASIC_EMOTES.contains(e), e + " must be unlocked");
        }
    }

    /** Project Rebalance (8 May 2024): the Falador hard Mining Guild task needs only the Prospector helmet. */
    @Test
    void miningGuildTaskNeedsOnlyTheProspectorHelmet() {
        Diaries.Task guild = byDesc("Enter the Mining Guild while wearing a Prospector helmet");
        assertEquals("falador", guild.area());
        assertEquals(Diaries.Tier.HARD, guild.tier());
        assertEquals(List.of("prospector_helmet"), Diaries.wornReq(guild));
        assertTrue(Diaries.wearingAll("prospector_helmet", List.of("prospector_helmet")));
        assertFalse(Diaries.wearingAll("prospector_helmet", List.of("prospector_jacket", "prospector_legs", "prospector_boots")));
    }

    /** The monks' boat and the balloon both land on Entrana, so the travel service searches the player before leaving. */
    @Test
    void entranaTripsAreSearchedBeforeLeaving() {
        Travel.Dest boat = Travel.get("boat_port_sarim_entrana").dests().get(0);
        assertTrue(AccessGates.onEntrana(boat.x(), boat.y()));
        Travel.Dest back = Travel.get("boat_entrana_port_sarim").dests().get(0);
        assertFalse(AccessGates.onEntrana(back.x(), back.y()));
        Travel.Dest balloon = Travel.get(Travel.balloonRoute(1)).dests().stream().filter(d -> d.name().equals("Entrana"))
                .findFirst().orElseThrow();
        assertTrue(AccessGates.onEntrana(balloon.x(), balloon.y()));
        assertTrue(AccessGates.entranaBanned(ItemCatalog.get("bronze_sword")));
    }
}
