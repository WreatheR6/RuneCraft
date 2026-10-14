package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.extras.Pets;
import com.runecraft.osrs.content.minigames.Gauntlet.Foe;
import com.runecraft.osrs.content.minigames.Gauntlet.Mode;
import com.runecraft.osrs.content.minigames.Gauntlet.Node;
import com.runecraft.osrs.content.minigames.Gauntlet.Piece;
import com.runecraft.osrs.content.minigames.Gauntlet.Room;
import com.runecraft.osrs.content.minigames.Gauntlet.RoomKind;
import com.runecraft.osrs.content.minigames.Gauntlet.Tier;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Gauntlet against the wiki snapshot ({@code osrs/minigames/gauntlet.tsv}): singing costs, bonuses and the reward chest. */
class GauntletTest {
    private static final String FIXTURE = "osrs/minigames/gauntlet.tsv";

    private static List<String[]> rows() throws Exception {
        List<String[]> rows = new ArrayList<>();
        java.nio.file.Path path = java.nio.file.Path.of("src/test/resources", FIXTURE);
        if (java.nio.file.Files.exists(path)) {
            for (String line : java.nio.file.Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    rows.add(line.split("\t"));
                }
            }
            return rows;
        }
        try (InputStream in = GauntletTest.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            assertNotNull(in, FIXTURE);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    rows.add(line.split("\t"));
                }
            }
        }
        return rows;
    }

    private static int i(String s) {
        return Integer.parseInt(s);
    }

    @Test
    void equipmentMatchesTheWiki() throws Exception {
        int seen = 0;
        for (String[] r : rows()) {
            if (r.length != 25) {
                continue;
            }
            Mode m = r[0].equals("crystal") ? Mode.NORMAL : Mode.CORRUPTED;
            Piece p = Piece.valueOf(r[1].toUpperCase(Locale.ROOT));
            Tier t = Tier.valueOf(r[2].toUpperCase(Locale.ROOT));
            String what = String.join(" ", r[0], r[1], r[2]);
            Gauntlet.Gear g = Gauntlet.gear(p, t);
            assertEquals(i(r[3]), g.shards(), what + " shards");
            assertEquals(i(r[4]), g.ore(), what + " ore");
            assertEquals(i(r[5]), g.bark(), what + " bark");
            assertEquals(i(r[6]), g.linum(), what + " linum");
            Gauntlet.Sing s = Gauntlet.recipe(m, p.key() + "_" + t.key());
            assertNotNull(s, what);
            String other = r[7];
            if (other.startsWith("Weapon_frame")) {
                assertEquals(Gauntlet.frame(m), s.needs(), what);
            } else if (!other.equals("-")) {
                assertEquals(Gauntlet.component(m, p), s.component(), what);
                assertTrue(other.toLowerCase(Locale.ROOT).contains(p == Piece.HALBERD ? "spike" : p == Piece.STAFF ? "orb" : "bowstring"));
            } else {
                assertNull(s.component(), what);
            }
            EquipmentStats st = g.stats();
            int[] want = new int[14];
            for (int k = 0; k < 14; k++) {
                want[k] = i(r[9 + k]);
            }
            int[] have = {st.stabAttack(), st.slashAttack(), st.crushAttack(), st.magicAttack(), st.rangedAttack(), st.stabDefence(),
                    st.slashDefence(), st.crushDefence(), st.magicDefence(), st.rangedDefence(), st.meleeStrength(), st.rangedStrength(),
                    st.magicDamagePercent(), st.prayer()};
            assertEquals(java.util.Arrays.toString(want), java.util.Arrays.toString(have), what + " bonuses");
            assertEquals(i(r[23]), g.speed(), what + " speed");
            assertEquals(i(r[24]), g.staffMaxHit(), what + " staff max hit");
            if (p == Piece.STAFF) {
                assertEquals(i(r[24]), Gauntlet.staffMaxHit(Gauntlet.equipment(m, p, t)));
            }
            ItemDef def = ItemCatalog.get(Gauntlet.equipment(m, p, t));
            assertNotNull(def, what);
            assertEquals(st, def.equipment().stats(), what + " catalog bonuses");
            seen++;
        }
        assertEquals(36, seen);
    }

    @Test
    void rewardChestMatchesTheWiki() throws Exception {
        Map<String, Gauntlet.Table> tables = Map.of("Regular", Gauntlet.REGULAR, "Corrupted", Gauntlet.CORRUPTED,
                "Partial completion", Gauntlet.INCOMPLETE, "Failure", Gauntlet.JUNK);
        Map<String, Integer> mainRows = new HashMap<>();
        Map<String, String> ids = Map.of("Youngllef", "pet_youngllef", "Clue scroll (elite)", "clue_scroll_elite",
                "Iwan's flyer", Gauntlet.IWANS_FLYER, "Potion (Apothecary)", Gauntlet.APOTHECARY_POTION, "Rotten tomato", Gauntlet.ROTTEN_TOMATO);
        for (String[] r : rows()) {
            if (r.length != 7) {
                continue;
            }
            String table = r[0];
            String id = ids.getOrDefault(r[1], r[1].toLowerCase(Locale.ROOT).replace("'", "").replace(' ', '_'));
            assertNotNull(ItemCatalog.get(id), r[1] + " -> " + id);
            double rarity = Double.parseDouble(r[4]) / Double.parseDouble(r[5]);
            boolean main = !table.equals("Regular") && !table.equals("Corrupted") || i(r[6]) > 1;
            if (main) {
                Gauntlet.Table t = tables.get(table);
                Gauntlet.Loot l = t.rows().stream().filter(x -> x.item().equals(id)).findFirst().orElse(null);
                assertNotNull(l, table + " " + id);
                assertEquals(i(r[2]), l.min(), table + " " + id);
                assertEquals(i(r[3]), l.max(), table + " " + id);
                assertEquals(rarity, l.weight() / (double) t.total(), 1e-9, table + " " + id);
                assertEquals(i(r[6]), t.rolls(), table + " rolls");
                mainRows.merge(table, 1, Integer::sum);
                continue;
            }
            // 100% and tertiary rows drop from the Hunllef itself (bosses module), the pet from Pets.
            String boss = table.equals("Regular") ? "crystalline_hunllef" : "corrupted_hunllef";
            if (id.equals("pet_youngllef")) {
                assertEquals(Math.round(1 / rarity), Pets.engineRate(boss), table + " pet");
                continue;
            }
            BossDef b = ContentRegistry.boss(boss);
            Drop d = b.drops.stream().filter(x -> x.item().equals(id)).findFirst().orElse(null);
            assertNotNull(d, boss + " drops " + id);
            assertEquals(i(r[2]), d.min(), boss + " " + id);
            assertEquals(i(r[3]), d.max(), boss + " " + id);
            assertEquals(Math.round(1 / rarity), d.rarity(), boss + " " + id);
        }
        for (Map.Entry<String, Gauntlet.Table> e : tables.entrySet()) {
            Gauntlet.Table t = e.getValue();
            assertEquals(t.rows().size(), (int) mainRows.get(e.getKey()), e.getKey() + " rows");
            assertEquals(t.total(), t.rows().stream().mapToInt(Gauntlet.Loot::weight).sum(), e.getKey() + " weights");
        }
        // The Hunllefs drop nothing but the wiki's 100% and tertiary rows.
        assertEquals(5, ContentRegistry.boss("crystalline_hunllef").drops.size());
        assertEquals(6, ContentRegistry.boss("corrupted_hunllef").drops.size());
        assertNull(Gauntlet.unfinished(0));
        assertEquals(Gauntlet.JUNK, Gauntlet.unfinished(49));
        assertEquals(Gauntlet.INCOMPLETE, Gauntlet.unfinished(50));
    }

    @Test
    void singingBowl() {
        for (Mode m : Mode.values()) {
            for (Gauntlet.Sing s : Gauntlet.recipes(m)) {
                assertNotNull(ItemCatalog.get(s.out()), s.id());
                for (String in : s.inputs(m).keySet()) {
                    assertNotNull(ItemCatalog.get(in), s.id() + " needs " + in);
                }
                assertEquals(s.shards() / 2.0, s.xpEach());
            }
            assertEquals(10, Gauntlet.recipe(m, "vial").shards());
            assertEquals(50, Gauntlet.recipe(m, "teleport").shards());
            assertEquals(10, Gauntlet.recipe(m, "paddlefish").shards());
            // Wiki: a full perfected set costs 13 ore, 13 bark, 13 linum and 650 shards.
            int shards = 0;
            int ore = 0;
            for (Piece p : new Piece[]{Piece.HELM, Piece.BODY, Piece.LEGS}) {
                for (Tier t : Tier.values()) {
                    shards += Gauntlet.gear(p, t).shards();
                    ore += Gauntlet.gear(p, t).ore();
                }
            }
            assertEquals(650, shards);
            assertEquals(13, ore);
        }
        assertEquals(17, Gauntlet.egniolPrayer(40));
        assertEquals(31, Gauntlet.egniolPrayer(99));
    }

    @Test
    void armourReducesTheHunllefsHits() {
        int[] sums = {0, 3, 6, 9};
        int[] normal = {12, 10, 8, 6};
        int[] corrupted = {16, 13, 10, 8};
        for (int k = 0; k < 4; k++) {
            assertEquals(normal[k], Gauntlet.hunllefMaxHit(Mode.NORMAL, sums[k]));
            assertEquals(corrupted[k], Gauntlet.hunllefMaxHit(Mode.CORRUPTED, sums[k]));
        }
        assertEquals(3, Gauntlet.armourTier("corrupted_helm_perfected"));
        assertEquals(1, Gauntlet.armourTier("crystal_legs_basic"));
        assertEquals(0, Gauntlet.armourTier("crystal_halberd_basic"));
        assertEquals(0.5, Gauntlet.hunllefDamageScale(Mode.NORMAL, 9), 1e-9);
        // The staves are powered staves with a fixed max hit.
        assertEquals(39, com.runecraft.osrs.CombatBonuses.poweredStaffMaxHit("corrupted_staff_perfected", 1));
        assertEquals(23, com.runecraft.osrs.CombatBonuses.poweredStaffMaxHit("crystal_staff_basic", 99));
        assertEquals(-1, com.runecraft.osrs.CombatBonuses.poweredStaffMaxHit("crystal_halberd_basic", 99));
    }

    @Test
    void creaturesMatchTheWiki() {
        Map<String, int[]> wiki = Map.of("crystalline_spider", new int[]{22, 12, 4}, "crystalline_bear", new int[]{172, 100, 28},
                "crystalline_wolf", new int[]{69, 38, 8}, "corrupted_bat", new int[]{48, 12, 11}, "corrupted_dragon", new int[]{258, 100, 48},
                "corrupted_scorpion", new int[]{89, 38, 17});
        for (Map.Entry<String, int[]> e : wiki.entrySet()) {
            MonsterDef d = ContentRegistry.monsters().stream().filter(x -> x.id.equals(e.getKey())).findFirst().orElse(null);
            assertNotNull(d, e.getKey());
            assertEquals(e.getValue()[0], d.combatLevel, e.getKey());
            assertEquals(e.getValue()[1], d.hitpoints, e.getKey());
            assertEquals(e.getValue()[2], d.attacks.get(0).maxHit(), e.getKey());
        }
        assertEquals(Gauntlet.Foe.DEMI, Gauntlet.Foe.of(Gauntlet.kindOf("corrupted_dark_beast")));
        assertNull(Gauntlet.kindOf("corrupted_hunllef"));
        assertEquals(10, Gauntlet.killPoints(Foe.DEMI));
    }

    @Test
    void mazeFollowsTheWikiRules() {
        for (Mode m : Mode.values()) {
            for (int seed = 0; seed < 200; seed++) {
                Room[][] grid = Gauntlet.generate(m, new Random(seed));
                assertEquals(RoomKind.BOSS, grid[3][3].kind());
                assertEquals(RoomKind.START, grid[Gauntlet.START_X][Gauntlet.START_Y].kind());
                int demis = 0;
                for (Room[] col : grid) {
                    for (Room r : col) {
                        assertNotNull(r);
                        long d = r.creatures().stream().filter(c -> Foe.of(c) == Foe.DEMI).count();
                        if (d > 0) {
                            assertTrue(r.edge(), "demi-bosses only in edge rooms");
                            assertEquals(1, r.creatures().size(), "demi-bosses spawn alone");
                        }
                        demis += (int) d;
                    }
                }
                assertEquals(Gauntlet.DEMI_BOSSES, demis);
                for (int[] p : new int[][]{{3, 0}, {6, 3}, {3, 6}, {0, 3}}) {
                    assertEquals(Foe.DEMI, Foe.of(grid[p[0]][p[1]].creatures().get(0)));
                }
                if (m == Mode.NORMAL) {
                    for (Node n : new Node[]{Node.ORE, Node.LINUM, Node.BARK}) {
                        assertTrue(inner(grid).stream().anyMatch(r -> Collections.frequency(r.nodes(), n) >= 3), n + " seed " + seed);
                    }
                    assertTrue(inner(grid).stream().anyMatch(r -> r.creatures().stream().filter(c -> Foe.of(c) == Foe.WEAK).count() >= 3));
                } else {
                    assertTrue(inner(grid).stream().anyMatch(r -> Collections.frequency(r.nodes(), Node.FISH) >= 3));
                }
            }
        }
        assertFalse(Gauntlet.runItems().contains("crystal_shard"), "reward shards are kept");
    }

    private static List<Room> inner(Room[][] grid) {
        List<Room> list = new ArrayList<>();
        for (Room[] col : grid) {
            for (Room r : col) {
                if (r.inner() && r.kind() == RoomKind.RESOURCE) {
                    list.add(r);
                }
            }
        }
        return list;
    }
}
