package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.slayer.DemonicGorillas;
import com.runecraft.osrs.content.slayer.DemonicGorillas.Style;
import com.runecraft.osrs.content.slayer.SlayerBossTasks;
import com.runecraft.osrs.data.AmmoRules;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.EquipmentBonuses;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bosses and dangerous monsters (demonic and tortured gorillas, the Deranged archaeologist, Phosani's Nightmare, Brutus,
 * Demonic Brutus, the Shellbane gryphon) diffed against a snapshot of the wiki DPS calculator's {@code monsters.json}
 * ({@code src/test/resources/osrs/bosses/monster_reference.tsv}), plus their mechanics, drops and spawns.
 */
class BossReferenceStatsTest {
    private static List<String[]> reference() throws Exception {
        String fixture = "osrs/bosses/monster_reference.tsv";
        java.nio.file.Path p = java.nio.file.Path.of("src/test/resources", fixture);
        List<String> lines;
        if (java.nio.file.Files.exists(p)) {
            lines = java.nio.file.Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = BossReferenceStatsTest.class.getClassLoader().getResourceAsStream(fixture)) {
                assertNotNull(in, "snapshot missing");
                lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank() && !line.startsWith("#")) {
                rows.add(line.split("\\|", -1));
            }
        }
        return rows;
    }

    private static CombatProfile<?> creature(String id) {
        MonsterDef m = ContentRegistry.monster(id);
        return m != null ? m : ContentRegistry.boss(id);
    }

    private static int rate(CombatProfile<?> c, String item) {
        return c.drops.stream().filter(d -> d.item().equals(item)).mapToInt(Drop::rarity).findFirst()
                .orElseThrow(() -> new AssertionError(c.id + " drops no " + item));
    }

    @Test
    void statsMatchTheDpsCalculator() throws Exception {
        List<String[]> rows = reference();
        assertEquals(8, rows.size());
        for (String[] r : rows) {
            CombatProfile<?> c = creature(r[0]);
            assertNotNull(c, r[0]);
            int[] v = Arrays.stream(Arrays.copyOfRange(r, 2, 18)).mapToInt(Integer::parseInt).toArray();
            String id = r[0];
            assertEquals(v[0], c.combatLevel, id + " level");
            assertEquals(v[1], c.hitpoints, id + " hitpoints");
            assertEquals(List.of(v[2], v[3], v[4], v[5], v[6]), List.of(c.attack, c.strength, c.defence, c.magic, c.ranged), id + " skills");
            assertEquals(v[7], c.attackSpeed, id + " attack speed");
            assertEquals(List.of(v[8], v[9], v[10], v[11], v[12]), List.of(c.defenceBonus(AttackType.STAB), c.defenceBonus(AttackType.SLASH),
                    c.defenceBonus(AttackType.CRUSH), c.defenceBonus(AttackType.MAGIC), c.defenceBonus(AttackType.RANGED)), id + " defence");
            assertEquals(List.of(v[13], v[14], v[15]), List.of(c.meleeAttackBonus, c.magicAttackBonus, c.rangedAttackBonus),
                    id + " attack bonuses");
            Set<String> attributes = new TreeSet<>(r[18].isEmpty() ? List.of() : List.of(r[18].split(",")));
            assertEquals(attributes, new TreeSet<>(c.attributes), id + " attributes");
            // The first max hit the calculator lists is the standard attack's.
            int max = Integer.parseInt(r[19].replaceAll("^(\\d+).*$", "$1"));
            assertTrue(c.attacks.stream().anyMatch(a -> a.maxHit() == max), id + " has no attack with max hit " + max);
        }
    }

    @Test
    void phosanisNightmareHitsHarderThanTheNightmare() {
        BossDef p = ContentRegistry.boss("phosanis_nightmare");
        assertEquals(73, p.attacks.get(0).maxHit());
        assertEquals(List.of(80, 80), p.attacks.stream().filter(a -> a.style() == AttackType.MAGIC || a.style() == AttackType.RANGED)
                .filter(a -> a.special() == AttackDef.Special.NONE).map(AttackDef::maxHit).toList());
        assertEquals(50, ContentRegistry.boss("the_nightmare").attacks.get(0).maxHit());
        assertEquals(507, rate(p, "nightmare_staff"));
        assertEquals(1129, rate(p, "inquisitors_mace"));
        assertEquals(1600, rate(p, "eldritch_orb"));
        assertEquals(35, rate(p, "clue_scroll_elite"));
    }

    @Test
    void brutusVariants() {
        BossDef demonic = ContentRegistry.boss("demonic_brutus");
        assertEquals(List.of(Drop.always("brutus_slippers")), demonic.drops);
        assertEquals("abyssal_potato", demonic.key);
        assertFalse(demonic.hasAttribute("demon"));
        BossDef brutus = ContentRegistry.boss("brutus");
        assertEquals(15, rate(brutus, "clue_scroll_beginner"));
        assertEquals(40, rate(brutus, "clue_scroll_easy"));
        assertEquals("gryphons", ContentRegistry.boss("shellbane_gryphon").slayerCategory);
        int[] pen = {ContentRegistry.place("brutus_pen").osrsX(), ContentRegistry.place("brutus_pen").osrsY()};
        assertEquals(List.of(3263, 3297), List.of(pen[0], pen[1]));
        PlaceDef cave = ContentRegistry.place("shellbane_gryphon_cave");
        assertEquals(List.of(3176, 2477), List.of(cave.osrsX(), cave.osrsY()));
    }

    @Test
    void derangedArchaeologist() {
        BossDef d = ContentRegistry.boss(IslandBosses.DERANGED);
        assertEquals(44, rate(d, "steel_ring"));
        assertEquals(200, rate(d, "clue_scroll_elite"));
        assertEquals(IslandBosses.BOOK_DIRECT_MAX / 3, IslandBosses.BOOK_SPLASH_MAX);
        assertTrue(SlayerBossTasks.counts("boss_crazy_archaeologist", IslandBosses.DERANGED));
        assertTrue(ItemCatalog.exists("steel_ring"));
        assertEquals(24, EquipmentBonuses.get("steel_ring").stats().crushDefence());
    }

    @Test
    void demonicGorillaDrops() {
        MonsterDef g = ContentRegistry.monster(DemonicGorillas.DEMONIC);
        assertEquals(300, rate(g, "zenyte_shard"));
        assertEquals(500, rate(g, "ballista_limbs"));
        assertEquals(500, rate(g, "ballista_spring"));
        assertEquals(750, rate(g, "light_frame"));
        assertEquals(1500, rate(g, "heavy_frame"));
        assertEquals(1500, rate(g, "monkey_tail"));
        assertEquals(100, rate(g, "clue_scroll_hard"));
        assertEquals("black_demons", g.slayerCategory);
        assertTrue(g.hasAttribute("demon"));
        MonsterDef t = ContentRegistry.monster(DemonicGorillas.TORTURED);
        assertEquals(3000, rate(t, "zenyte_shard"));
        assertEquals(15000, rate(t, "monkey_tail"));
        for (MonsterDef m : List.of(g, t)) {
            for (Drop d : m.drops) {
                assertTrue(d.item().startsWith("#") || ItemCatalog.exists(d.item()), m.id + " drops unknown " + d.item());
            }
        }
    }

    @Test
    void ballistaeAndZenyte() {
        Recipe light = Recipes.get("fletch_light_ballista");
        assertEquals(47, light.level());
        assertEquals(3000, light.xpTenths());
        Recipe heavy = Recipes.get("fletch_heavy_ballista");
        assertEquals(72, heavy.level());
        assertEquals(6000, heavy.xpTenths());
        assertEquals(70, Recipes.get("make_uncut_zenyte").level());
        assertTrue(EquipmentBonuses.twoHanded("heavy_ballista"));
        assertEquals(7, EquipmentBonuses.get("light_ballista").speed());
        assertEquals(125, EquipmentBonuses.get("heavy_ballista").stats().rangedAttack());
        assertTrue(AmmoRules.canFire("heavy_ballista", "dragon_javelin"));
        assertFalse(AmmoRules.canFire("light_ballista", "rune_arrow"));
        assertFalse(AmmoRules.canFire("light_ballista", null));
    }

    @Test
    void gorillaPrayerSwitching() {
        DemonicGorillas.Tracker t = new DemonicGorillas.Tracker(true, new Random(1));
        Style praying = t.prayer();
        Style other = praying == Style.MELEE ? Style.RANGED : Style.MELEE;
        assertTrue(t.blocks(praying));
        assertFalse(t.blocks(other));
        // 70 damage in three hits is not enough: it needs four separate hits too.
        assertFalse(t.damaged(other, 30));
        assertFalse(t.damaged(other, 30));
        assertFalse(t.damaged(other, 10));
        assertFalse(t.damaged(praying, 50)); // blocked hits never count
        assertTrue(t.damaged(other, 1));
        assertEquals(other, t.prayer());
        // Four hits but only 40 damage: no switch yet.
        Style third = Arrays.stream(Style.values()).filter(s -> s != other).findFirst().orElseThrow();
        for (int i = 0; i < 4; i++) {
            assertFalse(t.damaged(third, 10));
        }
        assertTrue(t.damaged(third, 30));
        assertEquals(third, t.prayer());
        // Tortured gorillas never pray.
        DemonicGorillas.Tracker tortured = new DemonicGorillas.Tracker(false, new Random(1));
        assertFalse(tortured.blocks(Style.MELEE) || tortured.blocks(Style.RANGED) || tortured.blocks(Style.MAGIC));
    }

    @Test
    void gorillaStyleSwitching() {
        Random random = new Random(7);
        DemonicGorillas.Tracker t = new DemonicGorillas.Tracker(true, random);
        Style start = t.attackStyle();
        assertFalse(t.attacked(true, random));
        assertFalse(t.attacked(false, random)); // hits don't reset the miss count
        assertFalse(t.attacked(true, random));
        assertTrue(t.attacked(true, random));
        assertTrue(t.attackStyle() != start);
        DemonicGorillas.Tracker tortured = new DemonicGorillas.Tracker(false, random);
        for (int i = 0; i < 3; i++) {
            assertFalse(tortured.attacked(true, random));
        }
        assertTrue(tortured.attacked(true, random));
        assertEquals(33, DemonicGorillas.boulderDamage(99));
    }

    @Test
    void crashSiteCavernHoldsTheCacheSpawns() {
        int demonic = 0;
        int tortured = 0;
        for (Dungeons.Camp c : Dungeons.camps()) {
            if (c.dungeon().equals("dng_crash_site_cavern")) {
                for (String id : c.creatures()) {
                    if (id.equals(DemonicGorillas.DEMONIC)) {
                        demonic += c.cap();
                    } else if (id.equals(DemonicGorillas.TORTURED)) {
                        tortured += c.cap();
                    }
                }
                assertEquals(100, c.respawnTicks());
            }
        }
        assertEquals(28, demonic);
        assertEquals(8, tortured);
    }
}
