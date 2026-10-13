package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.ItemValues;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Follow-ups of the Agility/Thieving audit: underground courses, indoor and grapple shortcuts, course rewards, more thieving. */
class AgilityThievingFollowupTest {

    @Test
    void werewolfAndPenguinCourses() {
        AgilityCourse w = AgilityCourse.WEREWOLF;
        assertEquals(60, w.level());
        assertEquals(350.0, w.lapXp(), 1e-9);
        assertEquals(730.0, w.lapXp() + AgilityCourse.STICK_XP, 1e-9);
        assertEquals(7, w.obstacles());
        assertEquals(50.0, w.obstacleXp(0));
        assertEquals(200.0, w.lapBonusXp(), 1e-9);
        assertEquals("creature_of_fenkenstrain", w.requiredQuest());
        assertFalse(w.questStartedEnough());
        assertEquals("ring_of_charos", w.requiredItem());
        assertTrue(w.canFail(6) && !w.canFail(0), "only the deathslide can be failed");
        assertEquals(0, w.markChance(99));
        AgilityCourse p = AgilityCourse.PENGUIN;
        assertEquals(30, p.level());
        assertEquals(540.0, p.lapXp(), 1e-9);
        assertEquals("cold_war", p.requiredQuest());
        assertTrue(p.questStartedEnough());
        assertNull(p.requiredItem());
        for (String q : new String[]{w.requiredQuest(), p.requiredQuest()}) {
            assertNotNull(ContentRegistry.quest(q), q);
        }
        assertNotNull(ItemCatalog.get("ring_of_charos"));
        assertNotNull(ItemCatalog.get(AgilityCourse.WEREWOLF_STICK));
        // Appended after the original courses (block states and saves use the ordinal).
        assertTrue(w.ordinal() > AgilityCourse.PRIFDDINAS.ordinal() && p.ordinal() > w.ordinal());
    }

    @Test
    void indoorAndGrappleShortcuts() {
        Map<String, Integer> wiki = Map.of("taverley_dungeon_pipe", 70, "edgeville_dungeon_monkeybars", 15,
                "brimhaven_stepping_stones", 12, "brimhaven_pipe_moss_giants", 22, "brimhaven_log_balance", 30,
                "slayer_tower_chain_low", 61, "slayer_tower_chain_high", 71, "fremennik_slayer_crevice", 62,
                "stronghold_slayer_tunnel", 72, "asgarnian_ice_tunnel", 72);
        for (Map.Entry<String, Integer> e : wiki.entrySet()) {
            assertNotNull(AgilityShortcuts.indoor(e.getKey()), e.getKey());
            assertEquals(e.getValue(), AgilityShortcuts.indoor(e.getKey()).level(), e.getKey());
        }
        assertEquals(AgilityShortcuts.ROPE, AgilityShortcuts.indoor("kalphite_lair_rope").needs());
        int grapples = 0;
        for (AgilityShortcuts.Shortcut s : AgilityShortcuts.SHORTCUTS) {
            if (s.id().contains("grapple")) {
                grapples++;
                assertEquals(AgilityShortcuts.GRAPPLE, s.needs(), s.id());
                assertTrue(s.other().containsKey(Skill.RANGED) && s.other().containsKey(Skill.STRENGTH), s.id());
            }
        }
        assertEquals(5, grapples);
        assertEquals(37, (int) AgilityShortcuts.at(3246, 3179).other().get(Skill.RANGED));
        assertTrue(AgilityShortcuts.isCrossbow("rune_crossbow") && AgilityShortcuts.isCrossbow("bronze_crossbow"));
        assertFalse(AgilityShortcuts.isCrossbow("karils_crossbow") || AgilityShortcuts.isCrossbow("magic_shortbow"));
        assertNotNull(ItemCatalog.get(AgilityShortcuts.MITH_GRAPPLE));
        assertNotNull(ItemCatalog.get("rope"));
        // The mith grapple: tip (59 Smithing, 50 XP), fletched onto a mithril bolt (59 Fletching, 11 XP), then a rope.
        Recipe tip = recipe("smith_mith_grapple_tip");
        assertEquals(59, tip.level());
        assertEquals(500, tip.xpTenths());
        Recipe unf = recipe("fletch_mith_grapple_unf");
        assertEquals(59, unf.level());
        assertEquals(110, unf.xpTenths());
        assertNotNull(recipe("fletch_mith_grapple"));
    }

    private static Recipe recipe(String id) {
        for (Recipe r : com.runecraft.osrs.Recipes.all()) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        throw new AssertionError("no recipe " + id);
    }

    /** The Slayer Tower chains go into open air of the tower's storeys (no structure block on their spots). */
    @Test
    void slayerTowerChainsStandInTheOpen() {
        StructureDef tower = ContentRegistry.structures().stream().filter(s -> s.id().equals("slayer_tower")).findFirst().orElseThrow();
        for (AgilityShortcuts.BlockEnds e : AgilityShortcuts.TOWER_ENDS) {
            for (int[] p : new int[][]{{e.x1(), e.y1(), e.z1()}, {e.x2(), e.y2(), e.z2()}}) {
                String last = "air";
                String below = null;
                for (StructureDef.Op op : tower.ops()) {
                    if (op.type() == StructureDef.Op.Type.FLATTEN || op.type() == StructureDef.Op.Type.REPLACE) {
                        continue;
                    }
                    boolean xz = p[0] >= op.x1() && p[0] <= op.x2() && p[2] >= op.z1() && p[2] <= op.z2();
                    if (op.type() == StructureDef.Op.Type.WALLS) {
                        xz &= p[0] == op.x1() || p[0] == op.x2() || p[2] == op.z1() || p[2] == op.z2();
                    }
                    if (xz && p[1] >= op.y1() && p[1] <= op.y2()) {
                        last = op.type() == StructureDef.Op.Type.CLEAR ? "air" : String.valueOf(op.block());
                    }
                    if (xz && p[1] - 1 >= op.y1() && p[1] - 1 <= op.y2() && op.type() != StructureDef.Op.Type.CLEAR) {
                        below = op.block();
                    }
                }
                assertTrue(last.equals("air") || last.equals("minecraft:air") || last.contains("cobweb"), e.id() + " blocked by " + last);
                assertNotNull(below, e.id() + " has no floor");
            }
        }
    }

    @Test
    void courseRewards() {
        assertEquals(1, AgilityRewards.crystalShards(AgilityCourse.PRIFDDINAS));
        assertEquals(0, AgilityRewards.crystalShards(AgilityCourse.SEERS));
        assertTrue(AgilityCourse.PRIFDDINAS.markChance(75) > 0, "Prifddinas gives marks of grace");
        assertTrue(AgilityRewards.termites(AgilityCourse.COLOSSAL_WYRM_ADVANCED)[0] > AgilityRewards.termites(AgilityCourse.COLOSSAL_WYRM_BASIC)[0]);
        assertEquals(0, AgilityRewards.termites(AgilityCourse.WILDERNESS)[1]);
        assertEquals(650, AgilityRewards.VARLAMORE_RECOLOUR_TERMITES);
        assertEquals(100, AgilityRewards.AMYLASE_PACK_TERMITES);
        assertEquals(150_000, AgilityRewards.WILDERNESS_FEE);
        assertEquals(200, AgilityRewards.ticketXp(1));
        assertEquals(230, AgilityRewards.ticketXp(101));
        assertEquals(1, AgilityRewards.streakMultiplier(15));
        assertEquals(2, AgilityRewards.streakMultiplier(16));
        assertEquals(3, AgilityRewards.streakMultiplier(31));
        assertEquals(4, AgilityRewards.streakMultiplier(61));
        for (Loot l : AgilityRewards.WILDERNESS_LOOT) {
            assertNotNull(ItemCatalog.get(l.item()), l.item());
        }
        for (String id : new String[]{AgilityRewards.TERMITES, AgilityRewards.WILDERNESS_TICKET, "crystal_shard", "amylase_crystal"}) {
            assertNotNull(ItemCatalog.get(id), id);
        }
        assertTrue(ContentRegistry.npcs().stream().anyMatch(n -> n.id().equals(AgilityRewards.WORM_TONGUE)));
    }

    @Test
    void gracefulRecolours() {
        for (String suffix : AgilityData.RECOLOURS) {
            for (AgilityData.Piece p : AgilityData.GRACEFUL) {
                assertNotNull(ItemCatalog.get(p.id() + suffix), p.id() + suffix);
                assertEquals(p.id(), AgilityData.plain(p.id() + suffix));
                assertEquals(ItemValues.wornWeight(p.id()), ItemValues.wornWeight(p.id() + suffix), 1e-9);
            }
        }
        assertTrue(AgilityData.fullGraceful("graceful_hood_varlamore", "graceful_top", "graceful_legs_agility_arena",
                "graceful_boots", "graceful_cape_varlamore", "graceful_gloves"));
        assertFalse(AgilityData.fullGraceful("graceful_hood", "graceful_top", "graceful_legs", "graceful_boots", "", "graceful_gloves"));
        assertEquals("rune_crossbow", AgilityData.plain("rune_crossbow"));
    }

    @Test
    void brimhavenAgilityArena() {
        RewardShop shop = RewardShops.get("brimhaven_agility_arena");
        boolean xp = false;
        boolean recolour = false;
        for (RewardShop.Entry e : shop.entries()) {
            if (e.reward() == RewardShop.Reward.XP) {
                xp = true;
                int n = e.costs().get(0).amount();
                assertEquals(345.0 * n, RewardShop.xpFor(e, 1), 1e-9);
                assertEquals(345.0 * n, RewardShop.xpFor(e, 99), 1e-9);
                assertEquals(MinigameCurrencies.ARENA_TICKETS, e.costs().get(0).currency());
            } else {
                assertEquals(MinigameCurrencies.BRIMHAVEN_VOUCHERS, e.costs().get(0).currency(), e.item());
            }
            if (e.item().equals(RewardShops.GRACEFUL_RECOLOUR + AgilityData.AGILITY_ARENA)) {
                recolour = true;
                assertEquals(250, e.costs().get(0).amount());
            }
            if (e.item().equals("pirates_hook")) {
                assertEquals(800, e.costs().get(0).amount());
            }
        }
        assertTrue(xp && recolour);
        assertNotNull(ItemCatalog.get("brimhaven_voucher"));
        // Pest Control's formula is unchanged.
        RewardShop pc = RewardShops.get("pest_control");
        RewardShop.Entry first = pc.entries().stream().filter(e -> e.reward() == RewardShop.Reward.XP).findFirst().orElseThrow();
        assertTrue(RewardShop.xpFor(first, 99) > RewardShop.xpFor(first, 50));
    }

    @Test
    void newPickpocketTargets() {
        ThievingData.Target ham = ThievingData.target("ham_member");
        assertEquals(15, ham.level());
        assertEquals(22.2, ham.xp());
        assertEquals(50, ThievingData.HAM_CLUE_CHANCE);
        ThievingData.Target fremennik = ThievingData.target("fremennik_citizen");
        assertEquals(45, fremennik.level());
        assertEquals(65.0, fremennik.xp());
        assertEquals(40, Thieving.pouch(Thieving.pouchForNpc("fremennik_citizen")).min());
        ThievingData.Target wealthy = ThievingData.target("wealthy_citizen");
        assertEquals(50, wealthy.level());
        assertEquals(96.0, wealthy.xp());
        assertEquals(17, Loot.totalWeight(wealthy.table()));
        assertEquals(ThievingData.HOUSE_KEY, wealthy.table().get(0).item());
        TownMonsters.Pickpocket goblin = TownMonsters.pickpocket("cave_goblin");
        assertEquals(36, goblin.level());
        assertEquals(40.0, goblin.xp());
        assertNotNull(ContentRegistry.monster("cave_goblin"));
        for (ThievingData.Target t : ThievingData.TARGETS) {
            assertNotNull(ContentRegistry.place(t.place()) != null ? "" : com.runecraft.osrs.Landmark.byName(t.place()), t.place());
            for (Loot l : t.table()) {
                assertNotNull(ItemCatalog.get(l.item()), t.npcId() + " " + l.item());
            }
            for (Loot l : t.always()) {
                assertNotNull(ItemCatalog.get(l.item()), t.npcId() + " " + l.item());
            }
        }
        for (TownMonsters.Loot l : goblin.table()) {
            assertNotNull(ItemCatalog.get(l.item()), l.item());
        }
    }

    @Test
    void newThievingChests() {
        assertEquals(52, ThievingData.container("dorgesh_kaan_average_chest").level());
        assertEquals(78, ThievingData.container("dorgesh_kaan_rich_chest").level());
        for (String id : ThievingData.HOUSE_CONTAINERS) {
            ThievingData.Container c = ThievingData.container(id);
            assertEquals(50, c.level(), id);
            assertEquals(45.0, c.xp(), id);
            assertEquals(ThievingData.VALUABLES, c.always().get(0).item());
        }
        for (ThievingData.Container c : ThievingData.CONTAINERS) {
            for (Loot l : c.always()) {
                assertNotNull(ItemCatalog.get(l.item()), c.blockId() + " " + l.item());
            }
            for (Loot l : c.table()) {
                if (!l.item().startsWith("minecraft:")) {
                    assertNotNull(ItemCatalog.get(l.item()), c.blockId() + " " + l.item());
                }
            }
        }
        // Dorgesh-Kaan chests come back after 90 s and 300 s (OSRS Wiki).
        assertEquals(150, ThievingData.container("dorgesh_kaan_average_chest").respawnTicks());
        assertEquals(500, ThievingData.container("dorgesh_kaan_rich_chest").respawnTicks());
        // Isle of Souls chest: 28 Thieving, 150 XP, a 200-weight table with a 1/200 dark key; the Dark Chest's table is out of 600.
        ThievingData.Container soul = ThievingData.container(ThievingData.SOUL_CHEST);
        assertEquals(28, soul.level());
        assertEquals(150.0, soul.xp(), 1e-9);
        assertEquals(200, Loot.totalWeight(soul.table()));
        assertTrue(soul.table().stream().anyMatch(l -> l.item().equals(ThievingData.DARK_KEY) && l.weight() == 1));
        ThievingData.Container dark = ThievingData.container(ThievingData.DARK_CHEST);
        assertEquals(1500.0, dark.xp(), 1e-9);
        assertEquals(603, Loot.totalWeight(dark.table())); // the wiki's x/600 lines add up to 603
        assertEquals(ThievingData.DARK_KEY, ThievingData.KEY_CONTAINERS.get(ThievingData.DARK_CHEST));
        assertNotNull(ItemCatalog.get(ThievingData.DARK_KEY));
        // Picklock: -23/153 on the 1-99 curve, -9% at 1 and 60% at 99 per the wiki prose; a lockpick adds 26.
        assertEquals(0.60, ThievingData.openChance(ThievingData.SOUL_CHEST, 99, false), 0.01);
        assertTrue(ThievingData.openChance(ThievingData.SOUL_CHEST, 50, true) > ThievingData.openChance(ThievingData.SOUL_CHEST, 50, false));
        assertEquals(1.0, ThievingData.openChance("ten_coin_chest", 13, false), 1e-9);
        assertTrue(ContentRegistry.places().stream().anyMatch(p -> p.blocks().contains(ThievingData.SOUL_CHEST)));
        assertTrue(ContentRegistry.places().stream().anyMatch(p -> p.blocks().contains(ThievingData.DARK_CHEST)));
        // The Wilderness course keeps its state across logouts but loses ten laps of streak.
        assertEquals(15, AgilityRewards.streakAfterLogout(25));
        assertEquals(0, AgilityRewards.streakAfterLogout(4));
        // The Dorgesh-Kaan chests stand in Dorgesh-Kaan (the dungeons module's city below Lumbridge).
        StructureDef city = ContentRegistry.structures().stream().filter(s -> s.id().equals("dng_lumbridge_cellar_caves"))
                .findFirst().orElseThrow();
        for (String id : ThievingData.LOCKPICK_CONTAINERS) {
            assertTrue(city.ops().stream().anyMatch(op -> ("runecraft:" + id).equals(op.block())), id);
        }
        assertNotNull(ItemCatalog.get(ThievingData.HOUSE_KEY));
        assertNotNull(ItemCatalog.get(ThievingData.VALUABLES));
        assertTrue(ContentRegistry.places().stream().anyMatch(p -> p.blocks().containsAll(ThievingData.HOUSE_CONTAINERS)));
    }
}
