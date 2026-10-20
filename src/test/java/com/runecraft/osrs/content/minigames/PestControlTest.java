package com.runecraft.osrs.content.minigames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.minigames.MinigameRules.Loot;
import com.runecraft.osrs.content.minigames.MinigameRules.PestControl;
import com.runecraft.osrs.content.minigames.PestControlRules.Variant;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pest Control against the OSRS Wiki and the DPS calculator (round 37). */
class PestControlTest {
    private static List<String[]> tsv(String name) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/minigames/" + name), StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                rows.add(line.split("\t"));
            }
        }
        return rows;
    }

    @Test
    void pestLevelsMatchTheDpsCalculatorAndTheWikiLevelTables() throws IOException {
        List<String[]> rows = tsv("pest_control_pests.tsv");
        assertEquals(rows.size(), PestControlRules.VARIANTS.size());
        for (String[] r : rows) {
            Variant v = PestControlRules.variant(r[0], Integer.parseInt(r[1]));
            assertNotNull(v, r[0] + " level " + r[1]);
            String what = r[0] + " " + r[1];
            assertEquals(Integer.parseInt(r[2]), v.hitpoints(), what + " hitpoints");
            assertEquals(Integer.parseInt(r[3]), v.attack(), what + " attack");
            assertEquals(Integer.parseInt(r[4]), v.strength(), what + " strength");
            assertEquals(Integer.parseInt(r[5]), v.defence(), what + " defence");
            assertEquals(Integer.parseInt(r[6]), v.magic(), what + " magic");
            assertEquals(Integer.parseInt(r[7]), v.ranged(), what + " ranged");
            assertEquals(Integer.parseInt(r[8]), v.maxHit(), what + " max hit");
            assertEquals(r[9], v.landers(), what + " landers");
        }
        // Every pest has levels on every lander, and harder landers bring stronger pests.
        for (String pest : PestControl.PESTS) {
            for (int tier = 0; tier < 3; tier++) {
                assertFalse(PestControlRules.variants(pest, tier).isEmpty(), pest + " on lander " + tier);
                Variant rolled = PestControlRules.rollVariant(pest, tier, 0.999);
                assertTrue(rolled.on(tier));
            }
            int noviceTop = PestControlRules.variants(pest, 0).stream().mapToInt(Variant::level).max().orElseThrow();
            int veteranTop = PestControlRules.variants(pest, 2).stream().mapToInt(Variant::level).max().orElseThrow();
            assertTrue(veteranTop > noviceTop, pest);
            assertNotNull(ContentRegistry.monster(pest), pest);
        }
        // The level 158 brawler is in the cache but never spawns.
        assertEquals(null, PestControlRules.variant("brawler", 158));
    }

    @Test
    void spawnChancesAreModAshs() {
        assertEquals(List.of("brawler", "defiler", "ravager", "shifter", "spinner", "splatter", "torcher"),
                new ArrayList<>(PestControlRules.SPAWN_PERCENT.keySet()));
        assertEquals(List.of(5, 20, 5, 20, 7, 23, 20), new ArrayList<>(PestControlRules.SPAWN_PERCENT.values()));
        assertEquals("brawler", PestControlRules.rollPest(0.0));
        assertEquals("brawler", PestControlRules.rollPest(0.0499));
        assertEquals("defiler", PestControlRules.rollPest(0.05));
        assertEquals("spinner", PestControlRules.rollPest(0.50));
        assertEquals("splatter", PestControlRules.rollPest(0.58));
        assertEquals("torcher", PestControlRules.rollPest(0.9999));
        // Who attacks the Void Knight (wiki "Void Knight (Pest Control)": defilers, shifters and torchers).
        for (String pest : PestControl.PESTS) {
            boolean knight = pest.equals("defiler") || pest.equals("shifter") || pest.equals("torcher");
            assertEquals(knight, PestControlRules.role(pest) == PestControlRules.Role.KNIGHT, pest);
        }
        assertEquals(PestControlRules.Role.STRUCTURES, PestControlRules.role("ravager"));
        assertEquals(PestControlRules.Role.HEALER, PestControlRules.role("spinner"));
        assertEquals(PestControlRules.Role.BOMBER, PestControlRules.role("splatter"));
        assertEquals(PestControlRules.Role.PLAYERS, PestControlRules.role("brawler"));
    }

    @Test
    void shieldsDropOnTheWikiScheduleInARotation() {
        assertEquals(25, PestControlRules.shieldDropTick(0)); // 15 seconds
        assertEquals(175, PestControlRules.shieldDropTick(3)); // all down by 1:45
        assertEquals(0, PestControlRules.shieldsDown(24));
        assertEquals(1, PestControlRules.shieldsDown(25));
        assertEquals(2, PestControlRules.shieldsDown(75));
        assertEquals(4, PestControlRules.shieldsDown(175));
        assertEquals(6, PestControlRules.SHIELD_ROTATIONS.length);
        Set<String> seen = new HashSet<>();
        for (int[] r : PestControlRules.SHIELD_ROTATIONS) {
            int[] sorted = r.clone();
            Arrays.sort(sorted);
            assertTrue(Arrays.equals(new int[]{0, 1, 2, 3}, sorted), Arrays.toString(r));
            assertNotEquals(3, r[0], "the red shield never drops first");
            assertTrue(seen.add(Arrays.toString(r)));
        }
        // Wiki: blue, red, yellow, purple is the first rotation.
        assertTrue(Arrays.equals(new int[]{1, 3, 2, 0}, PestControlRules.SHIELD_ROTATIONS[0]));
        assertEquals("purple_portal", PestControl.PORTALS[0]);
        assertEquals("red_portal", PestControl.PORTALS[3]);
    }

    @Test
    void portalsAreWeakToTheirStyleOnly() {
        AttackType[] weak = {AttackType.RANGED, AttackType.MAGIC, AttackType.STAB, AttackType.CRUSH};
        for (int i = 0; i < 4; i++) {
            MonsterDef portal = ContentRegistry.monster(PestControl.PORTALS[i]);
            assertNotNull(portal);
            assertEquals(120, portal.defence, PestControl.PORTALS[i]);
            assertEquals(0, (int) portal.defenceBonus.get(weak[i]), PestControl.PORTALS[i]);
            int armoured = 0;
            for (AttackType t : new AttackType[]{AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED}) {
                if (portal.defenceBonus.get(t) == 100) {
                    armoured++;
                }
            }
            assertEquals(i == 2 ? 3 : 4, armoured, PestControl.PORTALS[i]); // yellow is weak to stab and slash
        }
        assertEquals(0, (int) ContentRegistry.monster("yellow_portal").defenceBonus.get(AttackType.SLASH));
        assertEquals(List.of(200, 250, 250), List.of(PestControl.PORTAL_HP[0], PestControl.PORTAL_HP[1], PestControl.PORTAL_HP[2]));
        assertEquals(50, PestControlRules.KNIGHT_HEAL_PER_PORTAL);
        assertEquals(200, PestControl.VOID_KNIGHT_HP);
        assertEquals(2000, PestControl.GAME_TICKS); // 20 minutes
    }

    @Test
    void gatesAndBarricadesStandOnTheirWikiTiles() {
        // Wiki map pins (absolute tiles); the rules keep them relative to the Void Knight at 2656, 2592.
        int[][] gates = {{2643, 2592}, {2643, 2593}, {2656, 2585}, {2657, 2585}, {2670, 2592}, {2670, 2593}};
        int[][] barricades = {{2648, 2578}, {2649, 2578}, {2667, 2578}, {2668, 2578}, {2673, 2591}, {2673, 2592}, {2650, 2578},
                {2669, 2578}, {2673, 2593}, {2647, 2578}, {2666, 2578}, {2673, 2590}, {2676, 2587}, {2676, 2586}, {2676, 2585},
                {2676, 2584}, {2676, 2574}, {2676, 2573}, {2676, 2572}, {2676, 2571}, {2656, 2575}, {2657, 2575}, {2658, 2575},
                {2659, 2575}, {2636, 2591}, {2636, 2590}, {2636, 2589}, {2636, 2588}, {2637, 2574}, {2637, 2573}, {2637, 2572},
                {2637, 2571}};
        Set<String> wikiGates = new HashSet<>();
        Set<String> wikiBarricades = new HashSet<>();
        for (int[] g : gates) {
            wikiGates.add(g[0] + "," + g[1]);
        }
        for (int[] b : barricades) {
            wikiBarricades.add(b[0] + "," + b[1]);
        }
        Set<String> ourGates = new HashSet<>();
        Set<String> ourBarricades = new HashSet<>();
        for (PestControlRules.Structure s : PestControlRules.STRUCTURES) {
            for (int[] t : s.tiles()) {
                (s.gate() ? ourGates : ourBarricades).add((PestControlRules.KNIGHT_X + t[0]) + "," + (PestControlRules.KNIGHT_Y + t[1]));
            }
        }
        assertEquals(wikiGates, ourGates);
        assertEquals(wikiBarricades, ourBarricades);
        assertEquals(3, PestControlRules.STRUCTURES.stream().filter(PestControlRules.Structure::gate).count());
        // The gates sit in the fort's walls.
        assertEquals(PestControlRules.FORT_WEST, PestControlRules.STRUCTURES.get(0).tiles().get(0)[0]);
        assertEquals(PestControlRules.FORT_SOUTH, PestControlRules.STRUCTURES.get(1).tiles().get(0)[1]);
        assertEquals(PestControlRules.FORT_EAST, PestControlRules.STRUCTURES.get(2).tiles().get(0)[0]);
        // Damage stages: healthy, damaged, broken, destroyed.
        assertEquals(0, PestControlRules.damageStage(200, 200));
        assertEquals(1, PestControlRules.damageStage(120, 200));
        assertEquals(2, PestControlRules.damageStage(50, 200));
        assertEquals(3, PestControlRules.damageStage(0, 200));
        assertEquals(5.0, PestControlRules.REPAIR_XP);
    }

    @Test
    void activityBar() {
        int bar = PestControlRules.activityAfterHit(0, 50, false);
        assertEquals(PestControlRules.ACTIVITY_MAX, bar); // 50 damage fills it
        assertEquals(PestControlRules.ACTIVITY_MAX / 2, PestControlRules.activityAfterHit(0, 25, false));
        assertEquals(PestControlRules.activityAfterHit(0, 20, false), PestControlRules.activityAfterHit(0, 10, true)); // portals count double
        for (int t = 0; t < 199; t++) {
            bar = PestControlRules.activityDecay(bar);
        }
        assertTrue(bar > 0);
        assertEquals(0, PestControlRules.activityDecay(bar)); // two minutes idle empties it
        assertEquals(0, PestControlRules.activityDecay(0));
    }

    @Test
    void rewardShopMatchesVoidKnightsRewardOptions() {
        RewardShop pc = RewardShops.get("pest_control");
        // Experience by 1, 10 and 100 points for every combat skill at level 25+.
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC, Skill.PRAYER}) {
            for (int points : new int[]{1, 10, 100}) {
                assertTrue(pc.entries().stream().anyMatch(e -> e.reward() == RewardShop.Reward.XP && e.item().equals(s.name())
                        && e.costs().get(0).amount() == points && e.requiredLevel() == 25), s + " x" + points);
            }
        }
        // The wiki's tables: 10 points at 25-34 Attack = 353.5, 100 points at 98-99 = 61,600; 1 point at 99 Prayer = 288.
        assertEquals(3535, PestControlRules.xpTenths(25, 35, 10));
        assertEquals(3232, PestControlRules.xpTenths(25, 32, 10));
        assertEquals(616000, PestControlRules.xpTenths(99, 35, 100));
        assertEquals(2880, PestControlRules.xpTenths(99, 18, 1));
        assertEquals(29088, PestControlRules.xpTenths(98, 18, 10)); // 2,908.8
        RewardShop.Entry att10 = pc.entries().stream().filter(e -> e.reward() == RewardShop.Reward.XP && e.item().equals("ATTACK")
                && e.costs().get(0).amount() == 10).findFirst().orElseThrow();
        assertEquals(3535, RewardShop.xpRewardTenths(att10, 30));
        // Elite void: an upgrade of the carried top / robe (200 points each), not a second copy.
        for (String[] up : new String[][]{{"void_knight_top", "elite_void_top"}, {"void_knight_robe", "elite_void_robe"}}) {
            RewardShop.Entry e = pc.entries().stream().filter(x -> x.item().equals(up[1])).findFirst().orElseThrow();
            assertEquals(RewardShop.Reward.UPGRADE, e.reward());
            assertEquals(up[0], e.requiresItem());
            assertEquals(200, e.costs().get(0).amount());
        }
        // Total for top, robe, gloves and one helm: 850 points (wiki "Set costs").
        int set = 0;
        for (String id : List.of("void_knight_top", "void_knight_robe", "void_knight_gloves", "void_melee_helm")) {
            set += pc.entries().stream().filter(x -> x.item().equals(id)).findFirst().orElseThrow().costs().get(0).amount();
        }
        assertEquals(850, set);
        assertEquals(100, ItemCatalog.get("void_seal").value());
    }

    @Test
    void westernDiaryNeedsAGameWonOnEachLander() {
        // The Western Provinces diary's Pest Control tasks are games won on the novice / intermediate / veteran lander (the
        // hard one unlocks the elite void upgrade), not combat levels.
        String[][] tasks = {{"EASY", "minigame_win:pest_control_novice"}, {"MEDIUM", "minigame_win:pest_control_intermediate"},
                {"HARD", "minigame_win:pest_control_veteran"}};
        for (String[] t : tasks) {
            assertTrue(com.runecraft.osrs.content.systems.Diaries.tasks("western",
                    com.runecraft.osrs.content.systems.Diaries.Tier.valueOf(t[0])).stream()
                    .anyMatch(x -> x.type() == com.runecraft.osrs.content.systems.Diaries.Type.EVENT && x.target().equals(t[1])), t[1]);
        }
        assertEquals("minigame_win:pest_control_" + PestControl.TIERS[2].toLowerCase(java.util.Locale.ROOT), tasks[2][1]);
    }

    @Test
    void packsMatchTheWiki() {
        assertEquals(List.of("grimy_harralander", "grimy_ranarr_weed", "grimy_toadflax", "grimy_irit_leaf", "grimy_avantoe", "grimy_kwuarm"),
                PestControlRules.HERB_PACK.stream().map(Loot::item).toList());
        assertEquals(List.of(2, 2, 1, 2, 1, 1), PestControlRules.HERB_PACK.stream().map(Loot::min).toList());
        assertEquals(List.of(4, 3, 4, 4, 4, 5), PestControlRules.HERB_PACK.stream().map(Loot::max).toList());
        assertEquals(2 * 1061, PestControlRules.SEED_PACK.stream().mapToInt(Loot::weight).sum());
        assertEquals(3, PestControlRules.SEED_PACK_ROLLS);
        Loot potato = PestControlRules.SEED_PACK.stream().filter(l -> l.item().equals("potato_seed")).findFirst().orElseThrow();
        assertEquals(14, potato.min());
        assertEquals(400, potato.weight()); // 200/1061
        assertEquals(List.of(25, 18), PestControlRules.MINERAL_PACK.stream().map(Loot::min).toList());
        List<Loot> all = new ArrayList<>(PestControlRules.HERB_PACK);
        all.addAll(PestControlRules.SEED_PACK);
        all.addAll(PestControlRules.MINERAL_PACK);
        for (Loot l : all) {
            assertTrue(ItemCatalog.exists(l.item()), l.item());
        }
    }
}
