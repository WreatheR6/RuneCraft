package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.AgilityLayouts;
import com.runecraft.osrs.data.Thieving;
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

/** Agility courses/shortcuts and Thieving against the OSRS wiki (lap XP from the wiki/RuneLite calculator). */
class AgilityThievingAuditTest {

    @Test
    void courseLevelsAndLapXp() {
        Object[][] expected = {
                {AgilityCourse.GNOME_STRONGHOLD, 1, 110.5}, {AgilityCourse.DRAYNOR, 1, 120.0}, {AgilityCourse.AL_KHARID, 20, 216.0},
                {AgilityCourse.VARROCK, 30, 269.7}, {AgilityCourse.CANIFIS, 40, 240.0}, {AgilityCourse.FALADOR, 50, 586.0},
                {AgilityCourse.SEERS, 60, 570.0}, {AgilityCourse.POLLNIVNEACH, 70, 890.0}, {AgilityCourse.RELLEKKA, 80, 780.0},
                {AgilityCourse.ARDOUGNE, 90, 889.0}, {AgilityCourse.BARBARIAN_OUTPOST, 35, 152.5},
                {AgilityCourse.WILDERNESS, 52, 571.4}, {AgilityCourse.SHAYZIEN_BASIC, 1, 153.5},
                {AgilityCourse.SHAYZIEN_ADVANCED, 45, 508.0}, {AgilityCourse.COLOSSAL_WYRM_BASIC, 50, 504.1},
                {AgilityCourse.COLOSSAL_WYRM_ADVANCED, 62, 749.6}, {AgilityCourse.PRIFDDINAS, 75, 1337.0}};
        for (Object[] e : expected) {
            AgilityCourse c = (AgilityCourse) e[0];
            assertEquals(e[1], c.level(), c.name());
            assertEquals((double) e[2], c.lapXp(), 1e-9, c.name());
        }
        // The rooftop rework kept the original ordinals: block states and saves use them.
        assertEquals(0, AgilityCourse.GNOME_STRONGHOLD.ordinal());
        assertEquals(9, AgilityCourse.ARDOUGNE.ordinal());
        assertEquals(12.5, AgilityCourse.WILDERNESS.obstacleXp(0));
        assertEquals(498.9, AgilityCourse.WILDERNESS.lapBonusXp(), 1e-9);
    }

    @Test
    void marksOfGraceAndFailures() {
        assertEquals(1.0 / 3, AgilityCourse.FALADOR.markChance(50), 1e-9);
        assertEquals(1.0 / 15, AgilityCourse.FALADOR.markChance(70), 1e-9);
        assertEquals(2.0 / 3, AgilityCourse.CANIFIS.markChance(99), 1e-9);
        assertEquals(0, AgilityCourse.GNOME_STRONGHOLD.markChance(1));
        assertEquals(0, AgilityCourse.WILDERNESS.markChance(60));
        // Draynor and the Gnome course can't be failed (Draynor since May 2024).
        for (int i = 0; i < AgilityCourse.DRAYNOR.obstacles(); i++) {
            assertEquals(0, AgilityCourse.DRAYNOR.failChance(i, 1));
            assertEquals(0, AgilityCourse.GNOME_STRONGHOLD.failChance(i, 1));
        }
        for (AgilityCourse c : AgilityCourse.values()) {
            double prev = 1;
            for (int lvl = c.level(); lvl <= 99; lvl++) {
                double f = 0;
                for (int i = 0; i < c.obstacles(); i++) {
                    f = Math.max(f, c.failChance(i, lvl));
                }
                assertTrue(f <= prev && f <= 0.15, c + " fail chance rises at " + lvl);
                prev = f;
            }
            assertEquals(0, c.failChance(c.obstacles() - 1 + 1, 99));
            if (c.failDamage() == 0) {
                for (int i = 0; i < c.obstacles(); i++) {
                    assertFalse(c.canFail(i), c + " fails without damage");
                }
            }
        }
    }

    @Test
    void everyCourseIsInTheWorld() {
        for (AgilityCourse c : AgilityCourse.values()) {
            List<int[]> tiles = AgilityLayouts.tiles(c);
            if (c.handBuilt()) {
                assertTrue(tiles.isEmpty(), c + " is built by hand and has tiles too");
            } else {
                assertEquals(c.obstacles(), tiles.size(), c.name());
            }
            assertTrue(c.obstacles() <= AgilityCourse.MAX_OBSTACLES, c.name());
        }
        // Spot checks near the real courses' starts.
        assertNear(AgilityLayouts.tiles(AgilityCourse.DRAYNOR).get(0), 3103, 3279);
        assertNear(AgilityLayouts.tiles(AgilityCourse.AL_KHARID).get(0), 3273, 3195);
        assertNear(AgilityLayouts.tiles(AgilityCourse.VARROCK).get(0), 3221, 3414);
        assertNear(AgilityLayouts.tiles(AgilityCourse.CANIFIS).get(0), 3507, 3488);
        assertNear(AgilityLayouts.tiles(AgilityCourse.FALADOR).get(0), 3036, 3341);
        assertNear(AgilityLayouts.tiles(AgilityCourse.POLLNIVNEACH).get(0), 3351, 2962);
        assertNear(AgilityLayouts.tiles(AgilityCourse.BARBARIAN_OUTPOST).get(0), 2551, 3554);
        assertNear(AgilityLayouts.tiles(AgilityCourse.WILDERNESS).get(0), 3004, 3930);
    }

    private static void assertNear(int[] tile, int x, int y) {
        assertTrue(Math.abs(tile[0] - x) <= 4 && Math.abs(tile[1] - y) <= 4, tile[0] + "," + tile[1] + " not near " + x + "," + y);
    }

    @Test
    void shortcuts() {
        Map<String, Integer> wiki = Map.of("falador_crumbling_wall", 5, "yanille_underwall_tunnel", 16,
                "grand_exchange_tunnel", 21, "lumbridge_swamp_stepping_stone", 66, "shilo_stepping_stones", 74,
                "lava_dragon_isle_stepping_stones", 74, "gwd_little_crack", 60, "draynor_manor_railing", 28,
                "coal_truck_log_balance", 20, "catherby_obelisk_grapple", 36);
        Set<String> ids = new HashSet<>();
        for (AgilityShortcuts.Shortcut s : AgilityShortcuts.SHORTCUTS) {
            assertTrue(ids.add(s.id()), "duplicate shortcut " + s.id());
            if (wiki.containsKey(s.id())) {
                assertEquals(wiki.get(s.id()), s.level(), s.id());
            }
            assertTrue(s.level() > 0 || !s.other().isEmpty() || s.needs() != null, s.id() + " has no requirement");
            // Each end finds this shortcut and only this one.
            assertEquals(s, AgilityShortcuts.at(s.ax(), s.ay()), s.id());
            assertEquals(s, AgilityShortcuts.at(s.bx(), s.by()), s.id());
            assertTrue(Math.abs(s.ax() - s.bx()) + Math.abs(s.ay() - s.by()) >= 3, s.id() + " ends too close together");
        }
        assertEquals(60, (int) AgilityShortcuts.at(2898, 3715).other().get(com.runecraft.osrs.Skill.STRENGTH));
        assertTrue(ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(AgilityShortcuts.BLOCK)));
    }

    /**
     * The obstacle and shortcut blocks go onto the ground at their tiles: none of those tiles may
     * lie under a structure block standing above floor level (a wall, a building, a fence...).
     */
    @Test
    void agilityTilesDontOverlapStructures() {
        List<int[]> tiles = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (AgilityCourse c : AgilityCourse.values()) {
            List<int[]> t = AgilityLayouts.tiles(c);
            for (int i = 0; i < t.size(); i++) {
                tiles.add(t.get(i));
                names.add(c + " obstacle " + i);
            }
        }
        for (AgilityShortcuts.Shortcut s : AgilityShortcuts.SHORTCUTS) {
            tiles.add(new int[]{s.ax(), s.ay()});
            names.add(s.id() + " end a");
            tiles.add(new int[]{s.bx(), s.by()});
            names.add(s.id() + " end b");
        }
        List<String> overlaps = new ArrayList<>();
        for (StructureDef def : ContentRegistry.structures()) {
            int ground = StructureDef.GROUND;
            for (StructureDef.Op op : def.ops()) {
                if (op.type() == StructureDef.Op.Type.FLATTEN) {
                    ground = op.y1();
                    break;
                }
            }
            for (StructureDef.Op op : def.ops()) {
                if (op.type() == StructureDef.Op.Type.FLATTEN || op.type() == StructureDef.Op.Type.CLEAR
                        || op.y2() < ground || op.y1() > ground + 2) {
                    continue;
                }
                for (int i = 0; i < tiles.size(); i++) {
                    int bx = StructureDef.bx(tiles.get(i)[0]);
                    int bz = StructureDef.bz(tiles.get(i)[1]);
                    if (op.x2() >= bx && op.x1() <= bx + 1 && op.z2() >= bz && op.z1() <= bz + 1) {
                        overlaps.add(names.get(i) + " under " + def.id() + " " + op);
                    }
                }
            }
        }
        assertTrue(overlaps.isEmpty(), String.join("\n", overlaps.subList(0, Math.min(40, overlaps.size()))));
    }

    // ------------------------------------------------------------------ thieving

    @Test
    void coinPouchesAndGear() {
        assertEquals(28, Thieving.COIN_POUCH_LIMIT);
        assertEquals(28, Thieving.coinPouchLimit(1));
        assertEquals(56, Thieving.coinPouchLimit(2));
        assertEquals(84, Thieving.coinPouchLimit(3));
        assertEquals(140, Thieving.coinPouchLimit(4));
        assertEquals(0.2, Thieving.rogueDoubleChance(1), 1e-9);
        assertEquals(1.0, Thieving.rogueDoubleChance(5), 1e-9);
        assertEquals(5, Thieving.ROGUE_OUTFIT.size());
        for (String piece : Thieving.ROGUE_OUTFIT) {
            assertNotNull(ItemCatalog.get(piece), piece);
        }
        assertEquals(10, Thieving.DODGY_NECKLACE_CHARGES);
        assertEquals(0.25, Thieving.DODGY_NECKLACE_CHANCE);
        assertNotNull(ItemCatalog.get("dodgy_necklace"));
        assertNotNull(ItemCatalog.get("thieving_cape"));
        assertEquals(1.0, Thieving.pickpocketChance(99, 200, 255, 1.5));
        // Bonuses scale low/high (wiki guard chart: 55/264 with the Ardougne diary, 60/290 with the diary and the cape).
        assertEquals(com.runecraft.osrs.Chance.lerp(50, 55, 264), Thieving.pickpocketChance(50, 50, 240, 1.1), 1e-9);
        assertEquals(60, Thieving.scaled(50, 1.1, 1.1));
        assertEquals(290, Thieving.scaled(240, 1.1, 1.1));
        Set<String> keys = new HashSet<>();
        for (Thieving.CoinPouch p : Thieving.COIN_POUCHES) {
            assertTrue(keys.add(p.key()), p.key());
            assertNotNull(ItemCatalog.get(p.itemId()), p.itemId());
            assertTrue(p.min() > 0 && p.max() >= p.min(), p.key());
            assertEquals(p, Thieving.pouchByItem(p.itemId()));
        }
        assertEquals(3, Thieving.pouch("man").min());
        assertEquals(50, Thieving.pouch("knight_of_ardougne").min());
        assertEquals(350, Thieving.pouch("elf").max());
    }

    /** Every pickpocket target in the game that carries coins hands them out as its coin pouch. */
    @Test
    void everyCoinTargetHasAPouch() {
        for (Thieving.Pickpocket p : Thieving.Pickpocket.values()) {
            assertNotNull(Thieving.pouch(p.pouch()), p.name());
        }
        for (TownMonsters.Pickpocket p : TownMonsters.pickpockets()) {
            boolean coins = p.always().stream().anyMatch(l -> l.item().equals("coins") && l.max() > 1)
                    || p.table().stream().anyMatch(l -> l.item().equals("coins") && l.max() > 1);
            if (coins && !Thieving.LOOSE_COINS.contains(p.creature())) {
                assertNotNull(Thieving.pouch(Thieving.pouchForNpc(p.creature())), p.creature());
            }
        }
        for (ThievingData.Target t : ThievingData.TARGETS) {
            boolean coins = t.always().stream().anyMatch(l -> l.item().equals("coins") && l.max() > 1)
                    || t.table().stream().anyMatch(l -> l.item().equals("coins") && l.max() > 1);
            if (coins && !Thieving.LOOSE_COINS.contains(t.npcId())) {
                assertNotNull(Thieving.pouch(Thieving.pouchForNpc(t.npcId())), t.npcId());
            } else {
                assertNull(Thieving.pouchForNpc(t.npcId()), t.npcId());
            }
        }
    }

    @Test
    void stallsAndBlackjacking() {
        Thieving.Stall gem = Thieving.Stall.GEM;
        assertEquals(128, gem.totalWeight());
        assertEquals("uncut_sapphire", gem.pick(0).item());
        assertEquals("uncut_diamond", gem.pick(127).item());
        for (Thieving.Stall s : Thieving.Stall.values()) {
            for (Thieving.Loot l : s.loot()) {
                assertNotNull(ItemCatalog.get(l.item()), s + " " + l.item());
            }
        }
        // The seed stall sells farming seeds, not the master farmer's herb seeds.
        ThievingData.Container seeds = ThievingData.container("seed_stall");
        assertEquals(27, seeds.level());
        assertEquals(10.0, seeds.xp());
        for (Loot l : seeds.table()) {
            assertFalse(l.item().equals("ranarr_seed") || l.item().equals("snapdragon_seed") || l.item().equals("torstol_seed"), l.item());
            assertNotNull(ItemCatalog.get(l.item()), l.item());
        }
        for (String id : ThievingData.BLACKJACK_TARGETS) {
            assertNotNull(ThievingData.target(id), id);
        }
        assertEquals(65, ThievingData.target("menaphite_thug").level());
        assertEquals(137.5, ThievingData.target("menaphite_thug").xp());
        assertEquals(55, ThievingData.target("pollnivneach_bandit").level());
        assertEquals(84.3, ThievingData.target("pollnivneach_bandit").xp());
        assertEquals(45, ThievingData.target("bearded_pollnivneach_bandit").level());
        for (String b : ThievingData.BLACKJACKS) {
            assertNotNull(ItemCatalog.get(b), b);
        }
        assertNotNull(ContentRegistry.npc(ThievingData.BLACKJACK_SELLER));
        assertTrue(ThievingData.knockoutChance(ThievingData.target("menaphite_thug"), 99)
                > ThievingData.knockoutChance(ThievingData.target("menaphite_thug"), 65));
    }
}
