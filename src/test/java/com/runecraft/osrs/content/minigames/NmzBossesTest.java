package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

/** The Nightmare Zone's fifty dream bosses (round 40) against the wiki boss table and the infobox snapshot. */
class NmzBossesTest {
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
    void everyFormMatchesTheInfoboxSnapshot() throws IOException {
        List<String[]> rows = tsv("nmz_boss_stats.tsv");
        assertEquals(rows.size(), NmzBosses.forms().size());
        for (String[] r : rows) {
            NmzBosses.Form f = NmzBosses.form(r[0]);
            assertNotNull(f, r[0]);
            int[] expect = new int[r.length];
            for (int i = 3; i < r.length; i++) {
                if (i != 8 && i != 16 && i != 32) {
                    expect[i] = Integer.parseInt(r[i]);
                }
            }
            int[] got = {f.level(), f.hardLevel(), f.size(), f.speed(), f.hardSpeed(), 0, f.hitpoints(), f.attack(), f.strength(),
                    f.defence(), f.magic(), f.ranged(), f.maxHit(), 0, f.hardHitpoints(), f.hardAttack(), f.hardStrength(),
                    f.hardDefence(), f.hardMagic(), f.hardRanged(), f.hardMaxHit(), f.stabDefence(), f.slashDefence(),
                    f.crushDefence(), f.magicDefence(), f.rangedDefence(), f.meleeAttackBonus(), f.magicAttackBonus(),
                    f.rangedAttackBonus()};
            for (int i = 0; i < got.length; i++) {
                assertEquals(expect[i + 3], got[i], r[0] + " column " + (i + 3));
            }
            assertEquals(r[8], String.join("|", f.styles()), r[0] + " styles");
            assertEquals(r[16], f.hitStyle(), r[0] + " max hit style");
            assertEquals(r[32], f.attributes().isEmpty() ? "-" : String.join(",", f.attributes()), r[0] + " attributes");
        }
    }

    @Test
    void fiftyBossesWithTheWikiPointsAndQuests() throws IOException {
        assertEquals(50, NmzBosses.ALL.size());
        assertEquals(50, NightmareZone.BOSSES.length);
        List<String[]> wiki = tsv("nmz_bosses.tsv");
        assertEquals(50, wiki.size());
        long hardTotal = 0;
        for (String[] w : wiki) {
            NmzBosses.Boss boss = NmzBosses.ALL.stream().filter(b -> b.name().equalsIgnoreCase(w[0])).findFirst().orElse(null);
            assertNotNull(boss, w[0]);
            assertEquals(Integer.parseInt(w[1]), boss.points(), w[0]);
            assertEquals(Integer.parseInt(w[2]), boss.hardPoints(), w[0]);
            assertEquals(Integer.parseInt(w[4]), boss.first().hardHitpoints(), w[0] + " hard hitpoints");
            assertNotNull(ContentRegistry.quest(boss.quest()), boss.quest());
            assertTrue(NightmareZone.QUESTS.contains(boss.quest()), boss.quest() + " counts for entry");
            hardTotal += boss.hardPoints();
        }
        // "Beating every boss in hard endurance mode awards a total of 794,770" (OSRS Wiki, Endurance).
        assertEquals(794_770, hardTotal);
    }

    @Test
    void everyFormIsACreatureWithItsStats() {
        Set<String> ids = new HashSet<>();
        for (NmzBosses.Form f : NmzBosses.forms()) {
            assertTrue(ids.add(f.id()), "duplicate " + f.id());
            MonsterDef m = ContentRegistry.monster(f.id());
            assertNotNull(m, f.id());
            assertEquals(f.hitpoints(), m.hitpoints, f.id());
            assertEquals(f.attack(), m.attack, f.id());
            assertEquals(f.defence(), m.defence, f.id());
            assertEquals(f.level(), m.combatLevel, f.id());
            assertEquals(f.speed(), m.attackSpeed, f.id());
            assertEquals(f.maxHit(), m.attacks.get(0).maxHit(), f.id() + " main attack");
            assertTrue(ContentRegistry.models().containsKey(f.id()), f.id() + " model");
        }
        // Main attacks: Arrg's melee (his infobox max hit), Elvarg's slash and dragonfire, Moss Guardian's prayer-piercing magic.
        MonsterDef arrg = ContentRegistry.monster("nmz_arrg");
        assertEquals(AttackType.CRUSH, arrg.attacks.get(0).style());
        assertTrue(arrg.attacks.stream().anyMatch(a -> a.style() == AttackType.RANGED));
        MonsterDef elvarg = ContentRegistry.monster("nmz_elvarg");
        assertEquals(AttackType.SLASH, elvarg.attacks.get(0).style());
        assertTrue(elvarg.attacks.stream().anyMatch(a -> a.name().equals("dragonfire")));
        MonsterDef moss = ContentRegistry.monster("nmz_moss_guardian");
        assertTrue(moss.attacks.stream().anyMatch(a -> a.style() == AttackType.MAGIC));
        assertTrue(ContentRegistry.monster("nmz_bouncer").attributes.contains("demon"));
        assertTrue(ContentRegistry.monster("nmz_giant_scarab").attributes.contains("kalphite"));
    }

    @Test
    void formsTurnIntoTheNextOne() {
        assertEquals("nmz_witchs_experiment_spider", NmzBosses.nextForm("nmz_witchs_experiment"));
        assertEquals("nmz_witchs_experiment_wolf", NmzBosses.nextForm("nmz_witchs_experiment_bear"));
        assertNull(NmzBosses.nextForm("nmz_witchs_experiment_wolf"));
        assertEquals("nmz_damis_second", NmzBosses.nextForm("nmz_damis"));
        assertNull(NmzBosses.nextForm("nmz_glod"));
        assertFalse(NightmareZone.finalForm("nmz_nazastarool_skeleton"));
        assertTrue(NightmareZone.finalForm("nmz_nazastarool_ghost"));
        assertTrue(NightmareZone.isBoss("nmz_nazastarool_skeleton"));
        assertEquals("nmz_nazastarool", NightmareZone.bossOf("nmz_nazastarool_ghost"));
        assertEquals(1752, NightmareZone.points("nmz_nazastarool_ghost", false));
        assertEquals(63, NightmareZone.hardHitpoints("nmz_witchs_experiment"));
        assertEquals(113, NightmareZone.hardHitpoints("nmz_witchs_experiment_wolf"));
    }

    @Test
    void customRumblesAndThePenalty() {
        // Quest groups switch together: Damis takes the other Desert Treasure I bosses with him.
        assertEquals(List.of("nmz_dessous", "nmz_damis", "nmz_fareed", "nmz_kamil"), NightmareZone.questGroup("nmz_damis_second"));
        assertEquals(7, NightmareZone.questGroup("nmz_karamel").size());
        List<String> dm = new ArrayList<>(NightmareZone.questGroup("nmz_the_untouchable"));
        dm.addAll(List.of("nmz_count_draynor", "nmz_king_roald"));
        assertTrue(NightmareZone.penalised(dm));
        int full = NightmareZone.points("nmz_the_inadequacy", true, dm.size());
        assertEquals(full / 6, NightmareZone.points("nmz_the_inadequacy", true, dm));
        dm.add("nmz_kamil");
        assertFalse(NightmareZone.penalised(dm));
        // Several forms of one boss count once.
        assertEquals(NightmareZone.points("nmz_glod", false, 2),
                NightmareZone.points("nmz_glod", false, List.of("nmz_glod", "nmz_damis", "nmz_damis_second")));
    }

    @Test
    void powerUpsAndSlayer() {
        // Zapper: 3% of the boss's hitpoints, at least 1, at most 8, every two ticks; not on immune bosses.
        assertEquals(1, NightmareZone.zapDamage(35));
        assertEquals(4, NightmareZone.zapDamage(140));
        assertEquals(8, NightmareZone.zapDamage(255));
        assertEquals(2, NightmareZone.ZAPPER_INTERVAL);
        assertFalse(NightmareZone.zappable("nmz_the_everlasting"));
        assertFalse(NightmareZone.zappable("nmz_tanglefoot"));
        assertTrue(NightmareZone.zappable("nmz_glod"));
        // Slayer training table: categories and XP (normal / hard).
        assertEquals("trolls", ContentRegistry.monster("nmz_ice_troll_king").slayerCategory);
        assertEquals("vampyres", ContentRegistry.monster("nmz_dessous").slayerCategory);
        assertNull(ContentRegistry.monster("nmz_bouncer").slayerCategory, "the Bouncer doesn't count for hellhounds");
        assertEquals(146, NightmareZone.slayerXp("nmz_kamil", false), 1e-9);
        assertEquals(280.5, NightmareZone.slayerXp("nmz_giant_scarab", true), 1e-9);
        assertEquals(-1, NightmareZone.slayerXp("nmz_glod", false), 1e-9);
        // Hard max hit bonus goes to the main attack only: Arrg's melee, not his ranged.
        assertEquals(36, NightmareZone.hardMaxHitBonus("nmz_arrg", true, false));
        assertEquals(0, NightmareZone.hardMaxHitBonus("nmz_arrg", false, true));
        assertEquals(13, NightmareZone.hardMaxHitBonus("nmz_corsair_traitor", false, false));
    }
}
