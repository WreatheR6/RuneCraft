package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.bosses.ColosseumRewards;
import com.runecraft.osrs.content.minigames.FortisColosseum.Modifier;
import com.runecraft.osrs.content.minigames.FortisColosseum.Offer;
import com.runecraft.osrs.content.systems.CombatAchievementRules;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Fortis Colosseum against the wiki and monsters.json snapshots in {@code src/test/resources/osrs/minigames/}. */
class FortisColosseumTest {
    private static List<String[]> rows(String name) throws IOException {
        String res = "osrs/minigames/" + name;
        List<String> lines;
        Path p = Path.of("src/test/resources", res);
        if (Files.exists(p)) {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = FortisColosseumTest.class.getClassLoader().getResourceAsStream(res)) {
                assertNotNull(in, "missing " + res);
                lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }
        List<String[]> out = new ArrayList<>();
        for (String l : lines) {
            if (!l.isBlank() && !l.startsWith("#")) {
                out.add(l.split("\t", -1));
            }
        }
        return out;
    }

    private static String creatureId(String wikiName) {
        return switch (wikiName.toLowerCase(Locale.ROOT)) {
            case "serpent shaman" -> FortisColosseum.SHAMAN;
            case "jaguar warrior" -> FortisColosseum.JAGUAR;
            case "javelin colossus" -> FortisColosseum.JAVELIN;
            case "manticore" -> FortisColosseum.MANTICORE;
            case "shockwave colossus" -> FortisColosseum.SHOCKWAVE;
            case "minotaur", "minotaur (fortis colosseum)" -> FortisColosseum.MINOTAUR;
            case "sol heredit" -> FortisColosseum.SOL_HEREDIT;
            case "fremennik warband berserker" -> FortisColosseum.BERSERKER;
            case "fremennik warband archer" -> FortisColosseum.ARCHER;
            case "fremennik warband seer" -> FortisColosseum.SEER;
            default -> throw new AssertionError("unknown monster " + wikiName);
        };
    }

    /** "Fremennik Warband, 2x Javelin Colossus" -> creature ids (the warband is its three members). */
    private static List<String> monsters(String cell) {
        List<String> out = new ArrayList<>();
        if (cell.isBlank()) {
            return out;
        }
        for (String part : cell.split(",\\s*")) {
            int n = 1;
            if (part.matches("\\d+x .*")) {
                n = Integer.parseInt(part.substring(0, part.indexOf('x')));
                part = part.substring(part.indexOf(' ') + 1);
            }
            for (int i = 0; i < n; i++) {
                if (part.equalsIgnoreCase("Fremennik Warband")) {
                    out.addAll(FortisColosseum.WARBAND);
                } else {
                    out.add(creatureId(part));
                }
            }
        }
        return out;
    }

    @Test
    void wavesMatchTheWiki() throws IOException {
        List<String[]> rows = rows("colosseum_waves.tsv");
        assertEquals(FortisColosseum.WAVES, rows.size());
        for (String[] r : rows) {
            FortisColosseum.Wave w = FortisColosseum.wave(Integer.parseInt(r[0]));
            assertEquals(monsters(r[1]), w.monsters(), "wave " + r[0]);
            assertEquals(monsters(r[2]), w.reinforcements(), "wave " + r[0] + " reinforcements");
        }
        for (FortisColosseum.Wave w : FortisColosseum.waves()) {
            for (String id : w.monsters()) {
                assertTrue(ContentRegistry.monster(id) != null || ContentRegistry.boss(id) != null, id);
            }
            for (String id : w.reinforcements()) {
                assertNotNull(ContentRegistry.monster(id), id);
            }
        }
        assertEquals(67, FortisColosseum.REINFORCEMENT_TICKS); // 40 seconds
        assertEquals(1824, Minigame.FORTIS_COLOSSEUM.realX());
        assertEquals(Minigame.FORTIS_COLOSSEUM, Minigame.values()[Minigame.values().length - 1]);
    }

    @Test
    void monsterStatsMatchMonstersJson() throws IOException {
        for (String[] r : rows("colosseum_monsters.tsv")) {
            String id = creatureId(r[0]);
            var def = ContentRegistry.monster(id) != null ? ContentRegistry.monster(id) : ContentRegistry.boss(id);
            assertNotNull(def, id);
            int[] want = new int[16];
            for (int i = 0; i < 16; i++) {
                want[i] = Integer.parseInt(r[i + 1]);
            }
            assertEquals(want[0], def.combatLevel, id + " level");
            assertEquals(want[1], def.hitpoints, id + " hp");
            assertEquals(want[2], def.attack, id + " attack");
            assertEquals(want[3], def.strength, id + " strength");
            assertEquals(want[4], def.defence, id + " defence");
            assertEquals(want[5], def.magic, id + " magic");
            assertEquals(want[6], def.ranged, id + " ranged");
            if (want[7] > 0) {
                assertEquals(want[7], def.attackSpeed, id + " speed");
            }
            assertEquals(List.of(want[8], want[9], want[10], want[11], want[12]),
                    List.of(d(def, AttackType.STAB), d(def, AttackType.SLASH), d(def, AttackType.CRUSH), d(def, AttackType.MAGIC), d(def, AttackType.RANGED)), id + " defences");
            assertEquals(List.of(want[13], want[14], want[15]),
                    List.of(def.meleeAttackBonus, def.magicAttackBonus, def.rangedAttackBonus), id + " attack bonuses");
            int max = Integer.parseInt(r[17].replaceAll("[^0-9].*", ""));
            if (!id.equals(FortisColosseum.SOL_HEREDIT)) {
                assertEquals(max, def.attacks.stream().mapToInt(a -> a.maxHit()).max().orElse(0)
                        - (id.equals(FortisColosseum.MANTICORE) ? 5 : 0), id + " max hit");
            }
        }
    }

    private static int d(com.runecraft.osrs.content.CombatProfile<?> def, AttackType t) {
        return def.defenceBonus.getOrDefault(t, 0);
    }

    @Test
    void modifiersMatchTheWiki() throws IOException {
        Map<String, Modifier> byName = new HashMap<>();
        for (Modifier m : Modifier.values()) {
            byName.put(m.displayName, m);
        }
        Map<Modifier, Integer> tiers = new HashMap<>();
        for (String[] r : rows("colosseum_modifiers.tsv")) {
            Modifier m = byName.get(r[0]);
            assertNotNull(m, r[0]);
            int tier = Integer.parseInt(r[1]);
            assertEquals(Integer.parseInt(r[2]), m.glory(tier), r[0] + " " + tier);
            tiers.merge(m, tier, Math::max);
        }
        for (Modifier m : Modifier.values()) {
            assertEquals(tiers.get(m), m.maxTier(), m.name());
        }
        assertEquals("Bees! (II)", Modifier.BEES.label(2));
        assertEquals(List.of(15, 10, 5), List.of(FortisColosseum.doomLimit(1), FortisColosseum.doomLimit(2), FortisColosseum.doomLimit(3)));
        assertEquals(90, FortisColosseum.frailtyCap(99, 1)); // 99 - 9
        assertEquals(60, FortisColosseum.frailtyCap(99, 3)); // 99 - 39
        assertEquals(6, FortisColosseum.relentlessMaxHitBonus(3));
        assertEquals(2, FortisColosseum.volatilityRadius(3, 1)); // 3x3 manticore explodes 5x5
        assertEquals(3, FortisColosseum.volatilityRadius(3, 2)); // 7x7
    }

    @Test
    void offersFollowTheRules() {
        Random random = new Random(7);
        Map<Modifier, Integer> active = FortisColosseum.newModifiers();
        List<Offer> first = FortisColosseum.offers(1, active, random);
        assertEquals(List.of(Modifier.RELENTLESS, Modifier.BLASPHEMY, Modifier.FRAILTY), first.stream().map(Offer::modifier).toList());
        for (int wave = 2; wave <= FortisColosseum.WAVES; wave++) {
            for (int trial = 0; trial < 50; trial++) {
                List<Offer> offers = FortisColosseum.offers(wave, active, random);
                assertEquals(3, offers.size());
                assertEquals(3, new HashSet<>(offers.stream().map(Offer::modifier).toList()).size());
                for (Offer o : offers) {
                    assertEquals(active.getOrDefault(o.modifier(), 0) + 1, o.tier());
                    assertTrue(o.tier() <= o.modifier().maxTier());
                    if (wave < 7) {
                        assertFalse(o.modifier() == Modifier.RED_FLAG || o.modifier() == Modifier.DYNAMIC_DUO, o.label());
                    }
                    if (wave == 12) {
                        assertFalse(o.modifier().notForSol(), o.label());
                    }
                }
            }
            FortisColosseum.choose(active, FortisColosseum.offers(wave, active, random).get(0));
        }
    }

    @Test
    void quartetAndDynamicDuoAddMonsters() {
        Map<Modifier, Integer> active = FortisColosseum.newModifiers();
        active.put(Modifier.QUARTET, 1);
        active.put(Modifier.DYNAMIC_DUO, 1);
        List<String> w11 = FortisColosseum.startingMonsters(11, active, new Random(1));
        assertEquals(4, w11.stream().filter(FortisColosseum.WARBAND::contains).count());
        assertEquals(2, w11.stream().filter(FortisColosseum.SHOCKWAVE::equals).count());
        assertEquals(List.of(FortisColosseum.SOL_HEREDIT), FortisColosseum.startingMonsters(12, active, new Random(1)));
    }

    @Test
    void gloryFollowsTheWikiFormula() {
        Map<Modifier, Integer> active = FortisColosseum.newModifiers();
        active.put(Modifier.FRAILTY, 1);
        // Wave 1 in 50 ticks with no damage: 100 + 100 + 200 + (500 - 50).
        assertEquals(850, FortisColosseum.glory(1, 50, true, active));
        assertEquals(300, FortisColosseum.glory(1, 600, false, active));
        assertEquals(0, FortisColosseum.timeBonus(3, 600));
        assertEquals(1496, FortisColosseum.timeBonus(3, 1)); // 1,500 - 3 -> rounded down to even
        assertEquals("Grand Champion", FortisColosseum.rank(20000));
        assertEquals("Brawler", FortisColosseum.rank(2000));
        // The wiki's minimum after 12 waves: completion 7,800 + the cheapest modifiers.
        assertEquals(7800, java.util.stream.IntStream.rangeClosed(1, 12).map(w -> FortisColosseum.glory(w, 10_000, false,
                FortisColosseum.newModifiers())).sum());
    }

    private static String itemId(String wikiName) {
        return switch (wikiName) {
            case "Steel cannonball" -> "cannonball";
            case "Dragon bolts (unf)" -> "dragon_bolts_unf";
            case "Tonalztics of ralos (uncharged)" -> ColosseumRewards.TONALZTICS;
            case "Dizana's quiver" -> ColosseumRewards.QUIVER;
            case "Smol heredit" -> ColosseumRewards.PET;
            default -> wikiName.toLowerCase(Locale.ROOT).replace("'", "").replace(' ', '_');
        };
    }

    @Test
    void rewardTablesMatchTheWiki() throws IOException {
        Map<Integer, List<String>> wiki = new HashMap<>();
        for (String[] r : rows("colosseum_rewards.tsv")) {
            int wave = Integer.parseInt(r[0]);
            if (r[1].equals("Token (Varlamore)") || r[1].equals("Dizana's quiver") || r[1].equals("Smol heredit")) {
                continue; // Varlamore tokens aren't modelled; the quiver is given separately; the pet comes with Sol's death
            }
            String[] q = r[2].split("-");
            String rarity = r[3].equals("Always") ? "1/1" : r[3];
            String[] f = rarity.split("/");
            long g = gcd(Long.parseLong(f[0]), Long.parseLong(f[1]));
            wiki.computeIfAbsent(wave, k -> new ArrayList<>()).add(itemId(r[1]) + " " + q[0] + "-" + q[q.length - 1] + " "
                    + Long.parseLong(f[0]) / g + "/" + Long.parseLong(f[1]) / g);
        }
        assertEquals(12, wiki.size());
        for (ColosseumRewards.Table t : ColosseumRewards.tables()) {
            assertEquals(t.denominator(), t.totalWeight(), "wave " + t.wave() + " weights add up");
            List<String> mod = new ArrayList<>();
            for (ColosseumRewards.Line l : t.lines()) {
                long g = gcd(l.weight(), t.denominator());
                mod.add(l.item() + " " + l.min() + "-" + l.max() + " " + l.weight() / g + "/" + t.denominator() / g);
                assertTrue(ItemCatalog.exists(l.item()), l.item());
            }
            assertEquals(wiki.get(t.wave()), mod, "wave " + t.wave());
        }
        assertTrue(ItemCatalog.exists(ColosseumRewards.QUIVER));
        assertTrue(ItemCatalog.exists(ColosseumRewards.PET));
        assertEquals(FortisColosseum.QUIVER_PET_RATE, com.runecraft.osrs.content.extras.Pets.engineRate(FortisColosseum.SOL_HEREDIT));
    }

    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    @Test
    void chestRollsAndDuplicateProtection() {
        Random random = new Random(3);
        assertEquals(List.of(new ColosseumRewards.Reward(ColosseumRewards.SPLINTERS, 80)), ColosseumRewards.roll(1, Set.of(), random));
        List<ColosseumRewards.Reward> w12 = ColosseumRewards.roll(12, Set.of(), random);
        assertEquals(ColosseumRewards.QUIVER, w12.get(w12.size() - 1).item());
        Set<String> owned = Set.of("sunfire_fanatic_helm", "sunfire_fanatic_cuirass");
        for (int i = 0; i < 20; i++) {
            assertEquals("sunfire_fanatic_chausses", ColosseumRewards.sunfirePiece("sunfire_fanatic_helm", owned, random));
        }
        assertEquals("sunfire_fanatic_helm", ColosseumRewards.sunfirePiece("sunfire_fanatic_helm",
                Set.copyOf(ColosseumRewards.SUNFIRE), random));
        // Unique chance per wave (wiki: 1/124 on wave 4, 1/12 on wave 12).
        assertEquals(350, ColosseumRewards.table(4).lines().stream().filter(l -> ColosseumRewards.unique(l.item()))
                .mapToInt(ColosseumRewards.Line::weight).sum()); // 350/43400 = 1/124
        assertEquals(400, ColosseumRewards.table(12).lines().stream().filter(l -> ColosseumRewards.unique(l.item()))
                .mapToInt(ColosseumRewards.Line::weight).sum()); // 400/4800 = 1/12
        assertTrue(ColosseumRewards.table(6).lines().stream().noneMatch(l -> l.item().equals(ColosseumRewards.TONALZTICS)));
        List<ColosseumRewards.Reward> chest = new ArrayList<>();
        ColosseumRewards.add(chest, new ColosseumRewards.Reward(ColosseumRewards.SPLINTERS, 80));
        ColosseumRewards.add(chest, new ColosseumRewards.Reward(ColosseumRewards.SPLINTERS, 150));
        assertEquals(List.of(new ColosseumRewards.Reward(ColosseumRewards.SPLINTERS, 230)), chest);
    }

    @Test
    void combatAchievementsAreTracked() {
        for (int id : new int[]{542, 545, 546, 547, 548, 549, 550}) {
            assertTrue(CombatAchievements.task("ca_" + id).track() != Track.UNTRACKED, "ca_" + id);
        }
        assertEquals("mg:fortis_colosseum", CombatAchievements.task("ca_542").target());
        CombatAchievementRules.Metrics m = new CombatAchievementRules.Metrics();
        m.stage = 4;
        String rule = CombatAchievements.task("ca_542").rule();
        assertFalse(CombatAchievementRules.meets(rule, m));
        m.flags.add(FortisColosseum.FLAG_NO_MANTICORE_DAMAGE);
        assertTrue(CombatAchievementRules.meets(rule, m));
        m.stage = 5;
        assertFalse(CombatAchievementRules.meets(rule, m));
        Map<Modifier, Integer> active = FortisColosseum.newModifiers();
        assertFalse(FortisColosseum.oneOff(active));
        active.put(Modifier.DOOM, 2);
        assertTrue(FortisColosseum.oneOff(active));
        active.put(Modifier.BEES, 2);
        active.put(Modifier.QUARTET, 1);
        assertFalse(FortisColosseum.reinforcements(active));
        active.put(Modifier.SOLARFLARE, 2);
        assertTrue(FortisColosseum.reinforcements(active));
    }

    @Test
    void creaturesAreRegistered() {
        for (MonsterDef m : FortisColosseum.monsters()) {
            assertNotNull(ContentRegistry.monster(m.id), m.id);
        }
        assertNotNull(ContentRegistry.boss(FortisColosseum.SOL_HEREDIT));
        assertNotNull(ContentRegistry.npc(FortisColosseum.MINIMUS));
    }
}
