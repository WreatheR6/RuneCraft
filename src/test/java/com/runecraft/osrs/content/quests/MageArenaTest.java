package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.EquipmentBonuses;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mage Arena I and II against the OSRS Wiki: Kolodion's forms, the battle mages, the god followers and the miniquests. */
class MageArenaTest {
    private static List<String[]> snapshot() throws IOException {
        String fixture = "osrs/quests/mage_arena_monsters.tsv";
        Path p = Path.of("src/test/resources", fixture);
        List<String> lines;
        if (Files.exists(p)) {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = MageArenaTest.class.getClassLoader().getResourceAsStream(fixture)) {
                assertNotNull(in, "missing " + fixture);
                lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }
        List<String[]> out = new ArrayList<>();
        for (String l : lines) {
            if (!l.isBlank() && !l.startsWith("#") && !l.startsWith("id\t")) {
                out.add(l.split("\t", -1));
            }
        }
        return out;
    }

    private static CombatProfile<?> creature(String id) {
        CombatProfile<?> p = ContentRegistry.boss(id);
        if (p == null) {
            p = ContentRegistry.monster(id);
        }
        assertNotNull(p, id);
        return p;
    }

    private static int maxOf(CombatProfile<?> p, boolean magic) {
        int max = 0;
        for (AttackDef a : p.attacks) {
            if (a.name().equals("ice_barrage")) {
                continue; // the followers' Ice Barrage freeze (its max isn't on the wiki)
            }
            if ((a.style() == AttackType.MAGIC) == magic) {
                max = Math.max(max, a.maxHit());
            }
        }
        return max;
    }

    @Test
    void creaturesMatchTheWiki() throws IOException {
        List<String> diffs = new ArrayList<>();
        List<String[]> rows = snapshot();
        assertEquals(11, rows.size());
        for (String[] f : rows) {
            CombatProfile<?> p = creature(f[0]);
            String want = String.join(" ", f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], f[14],
                    f[15], f[16]);
            String got = p.combatLevel + " " + p.hitpoints + " " + p.attack + " " + p.strength + " " + p.defence + " " + p.magic + " "
                    + p.attackSpeed + " " + p.defenceBonus(AttackType.STAB) + " " + p.defenceBonus(AttackType.SLASH) + " "
                    + p.defenceBonus(AttackType.CRUSH) + " " + p.defenceBonus(AttackType.MAGIC) + " " + p.defenceBonus(AttackType.RANGED)
                    + " " + p.meleeAttackBonus + " " + p.magicAttackBonus + " " + maxOf(p, true) + " " + maxOf(p, false);
            if (!want.equals(got)) {
                diffs.add(f[0] + ": wiki " + want + ", RuneCraft " + got);
            }
            if (!f[17].equals("-")) {
                assertTrue(p.drops.stream().anyMatch(d -> d.item().equals(f[17]) && d.rarity() == 1), f[0] + " always drops " + f[17]);
                assertTrue(p.drops.stream().anyMatch(d -> d.item().equals("looting_bag") && d.rarity() == 3), f[0] + " looting bag 1/3");
                assertNotNull(ItemCatalog.get(f[17]), f[17]);
            }
        }
        assertTrue(diffs.isEmpty(), String.join("\n", diffs));
    }

    @Test
    void kolodionFightsInFiveFormsThatOnlyMagicHarms() {
        assertEquals(5, MageArena.KOLODION_FORMS.size());
        assertEquals("quest_kolodion_human", MageArena.KOLODION_FORMS.get(0).id());
        assertEquals(MageArena.KOLODION, MageArena.KOLODION_FORMS.get(4).id());
        assertEquals("quest_kolodion_ogre", MageArena.nextForm("quest_kolodion_human"));
        assertEquals(MageArena.KOLODION, MageArena.nextForm("quest_kolodion_ghost"));
        assertNull(MageArena.nextForm(MageArena.KOLODION));
        for (MageArena.Form f : MageArena.KOLODION_FORMS) {
            BossDef b = ContentRegistry.boss(f.id());
            assertNotNull(b, f.id());
            assertTrue(b.immuneTo.containsAll(Set.of(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.RANGED)), f.id());
            assertFalse(b.immuneTo.contains(AttackType.MAGIC), f.id());
            // only the demon has a stone and arena (the summoned fight starts at the player's form); the rest are forms
            assertEquals(!f.id().equals(MageArena.KOLODION), b.raidOnly, f.id());
            assertEquals("black_demon".equals(f.model()), f.id().equals(MageArena.KOLODION), f.id());
        }
        assertEquals("quest_mage_arena", ContentRegistry.boss(MageArena.KOLODION).place);
    }

    @Test
    void godSpellsNeedMageArenaAndAHundredCasts() {
        assertEquals(100, MageArena.CASTS_TO_CHARGE);
        assertEquals("guthix", MageArena.spellGod("claws_of_guthix"));
        assertNull(MageArena.spellGod("fire_wave"));
        assertNull(MageArena.castDenied("fire_wave", false, false, false));
        assertNotNull(MageArena.castDenied("saradomin_strike", false, true, false));
        assertNull(MageArena.castDenied("saradomin_strike", true, true, false), "always castable inside the arena");
        assertNotNull(MageArena.castDenied("saradomin_strike", true, false, false), "outside only once charged");
        assertNull(MageArena.castDenied("saradomin_strike", true, false, true));
        assertTrue(MageArena.battleMagePeaceful("battle_mage_zamorak", "zamorak_cape"));
        assertTrue(MageArena.battleMagePeaceful("battle_mage_zamorak", "imbued_zamorak_cape"));
        assertFalse(MageArena.battleMagePeaceful("battle_mage_zamorak", "saradomin_cape"));
        assertFalse(MageArena.battleMagePeaceful("battle_mage_zamorak", "zamorak_platebody"));
        // nine battle mages at their wiki tiles, three of each god, all inside the arena ring in level 54+ Wilderness
        assertEquals(9, MageArena.BATTLE_MAGES.size());
        Set<String> tiles = new HashSet<>();
        for (MageArena.Spawn s : MageArena.BATTLE_MAGES) {
            assertTrue(MageArena.inArena(s.x(), s.y()), s.toString());
            assertTrue(tiles.add(s.x() + "," + s.y()));
            assertTrue(GielinorMap.wildernessLevel(GielinorMap.toBlockX(s.x()), GielinorMap.toBlockZ(s.y())) >= 50, s.toString());
            assertNotNull(ContentRegistry.monster(s.creature()), s.creature());
        }
        for (String god : MageArena.GODS) {
            assertEquals(3, MageArena.BATTLE_MAGES.stream().filter(s -> s.creature().equals(MageArena.battleMage(god))).count(), god);
        }
    }

    @Test
    void followersAndTheEnchantedSymbol() {
        for (MageArena.Follower f : MageArena.FOLLOWERS) {
            assertTrue(MageArena.harms(f, f.spell()));
            for (String other : MageArena.GOD_SPELLS) {
                assertEquals(other.equals(f.spell()), MageArena.harms(f, other), f.id() + " " + other);
            }
            assertFalse(MageArena.harms(f, null));
            BossDef b = ContentRegistry.boss(f.id());
            assertTrue(b.raidOnly, "found with the symbol, not summoned at an arena stone: " + f.id());
            assertNull(b.place, f.id());
            assertEquals(f, MageArena.byRemains(f.remains()));
        }
        assertTrue(ContentRegistry.boss("quest_porazdir").hasAttribute("demon"));
        // the strange device's bands
        assertEquals("ice cold", MageArena.temperature(500));
        assertEquals("very cold", MageArena.temperature(200));
        assertEquals("cold", MageArena.temperature(150));
        assertEquals("warm", MageArena.temperature(100));
        assertEquals("hot", MageArena.temperature(70));
        assertEquals("very hot", MageArena.temperature(30));
        assertEquals("incredibly hot", MageArena.temperature(5));
        assertEquals("visibly shaking", MageArena.temperature(4));
        assertEquals(16, MageArena.SYMBOL_MAX_DAMAGE);
        assertEquals(45 * 60, MageArena.ROTATION_SECONDS);
        assertEquals("north-east", MageArena.direction(0, 0, 10, 10));
        assertEquals("south", MageArena.direction(0, 0, 0, -10));
        // every spot is above-ground deep Wilderness; the three followers never share one
        for (int[] s : MageArena.FOLLOWER_SPOTS) {
            int level = GielinorMap.wildernessLevel(GielinorMap.toBlockX(s[0]), GielinorMap.toBlockZ(s[1]));
            assertTrue(level >= 30 && level <= 56, s[0] + "," + s[1] + " level " + level);
        }
        for (long seed = 0; seed < 50; seed++) {
            for (int rot = 0; rot < 5; rot++) {
                Set<String> used = new HashSet<>();
                for (int f = 0; f < 3; f++) {
                    int[] s = MageArena.followerSpot(seed, f, rot);
                    assertTrue(used.add(s[0] + "," + s[1]), "seed " + seed + " rotation " + rot);
                }
            }
        }
        // Porazdir's energy ball: up to 30 next to him, nothing from 12 tiles
        assertEquals(30, MageArena.energyBallMax(1));
        assertEquals(0, MageArena.energyBallMax(12));
        assertTrue(MageArena.energyBallMax(6) > 0 && MageArena.energyBallMax(6) < 30);
        assertEquals(20, ContentRegistry.monster(MageArena.ORB).hitpoints);
    }

    @Test
    void miniquestsAndCapes() {
        QuestSpec ma1 = QuestBook.get().spec("mage_arena_i");
        QuestSpec ma2 = QuestBook.get().spec("mage_arena_ii");
        assertEquals(3, ma1.stages.size(), "stage count is saved with players");
        assertEquals(3, ma2.stages.size(), "stage count is saved with players");
        // the statues give the god capes (the chamber guardian the staff)
        for (QuestSpec.Trigger t : ma1.stages.get(2).triggers) {
            assertTrue(t.give.containsKey(t.target.replace("quest_statue_", "") + "_cape"), t.target);
            assertFalse(t.give.keySet().stream().anyMatch(i -> i.endsWith("_staff")), t.target);
        }
        // MA II starts with the three god spells charged and hands over the enchanted symbol
        QuestSpec.Trigger start = ma2.stages.get(0).triggers.get(0);
        assertTrue(ma2.stages.get(0).journal.contains("100 times"), "the casts are a start requirement");
        assertTrue(start.give.containsKey(MageArena.SYMBOL));
        assertTrue(QuestWalk.plan(ma1, id -> ItemCatalog.get(id) != null).completes());
        assertTrue(QuestWalk.plan(ma2, id -> ItemCatalog.get(id) != null).completes());
        for (String god : MageArena.GODS) {
            var stats = EquipmentBonuses.get("imbued_" + god + "_cape").stats();
            assertEquals(15, stats.magicAttack());
            assertEquals(15, stats.magicDefence());
            assertEquals(0, stats.rangedDefence());
            assertEquals(2, stats.magicDamagePercent());
            assertNotNull(ItemCatalog.get(god + "_cape"));
            assertNotNull(ItemCatalog.get(god + "_staff"));
        }
        for (String item : List.of(MageArena.SYMBOL, "justiciars_hand", "ents_roots", "demons_heart")) {
            assertNotNull(ItemCatalog.get(item), item);
        }
    }

    @Test
    void dropsAreCountedOnce() {
        for (MageArena.Follower f : MageArena.FOLLOWERS) {
            long remains = ContentRegistry.boss(f.id()).drops.stream().map(Drop::item).filter(f.remains()::equals).count();
            assertEquals(1, remains, f.id());
        }
    }
}
