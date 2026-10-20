package com.runecraft.osrs.content.minigames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Round 34: the Fight Cave's 15 spawn rotations and the Inferno's waves against OSRS Wiki snapshots
 * ({@code src/test/resources/osrs/minigames/fight_cave_rotations.tsv} from "TzHaar Fight Cave/Rotations/1..15", 945 waves;
 * {@code inferno_waves.tsv} from "Inferno"'s wave breakdown), plus the reward and mechanic numbers of {@link TzHaarWaves}.
 */
class TzHaarWikiTest {
    private static List<String[]> rows(String file) throws IOException {
        List<String[]> out = new ArrayList<>();
        boolean header = true;
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/minigames/" + file), StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            out.add(line.split("\t"));
        }
        return out;
    }

    @Test
    void fightCaveSpawnsFollowEveryWikiRotation() throws IOException {
        List<String[]> rows = rows("fight_cave_rotations.tsv");
        assertEquals(TzHaarWaves.FIGHT_CAVE_ROTATIONS * TzHaarWaves.FIGHT_CAVE_WAVES, rows.size());
        for (String[] r : rows) {
            int rotation = Integer.parseInt(r[0]);
            int wave = Integer.parseInt(r[1]);
            List<String> wiki = new ArrayList<>();
            for (String s : r[2].split(",")) {
                String[] kv = s.split("=");
                wiki.add(kv[0].replaceAll("\\d", "") + "=" + kv[1]);
            }
            List<String> mine = new ArrayList<>();
            List<String> monsters = TzHaarWaves.fightCave(wave);
            List<TzHaarWaves.CaveSpawn> at = TzHaarWaves.fightCaveSpawns(rotation, wave);
            assertEquals(monsters.size(), at.size());
            for (int i = 0; i < monsters.size(); i++) {
                mine.add(at.get(i).name().toLowerCase(Locale.ROOT) + "=" + TzHaarWaves.caveLevel(monsters.get(i)));
            }
            if (wave == TzHaarWaves.FIGHT_CAVE_WAVES) {
                // Jad, then his four healers at their fixed spawn points.
                for (TzHaarWaves.CaveSpawn h : TzHaarWaves.HEALER_SPAWNS) {
                    mine.add(h.name().toLowerCase(Locale.ROOT) + "=" + TzHaarWaves.caveLevel("yt_hurkot"));
                }
            }
            assertEquals(wiki, mine, "rotation " + rotation + " wave " + wave);
        }
    }

    @Test
    void jadSpawnsWhereWaveThreesTzKekDid() {
        for (int rotation = 1; rotation <= TzHaarWaves.FIGHT_CAVE_ROTATIONS; rotation++) {
            assertEquals(TzHaarWaves.fightCaveSpawns(rotation, 3).get(0), TzHaarWaves.fightCaveSpawns(rotation, 63).get(0));
        }
        Set<List<TzHaarWaves.CaveSpawn>> openings = new HashSet<>();
        for (int rotation = 1; rotation <= TzHaarWaves.FIGHT_CAVE_ROTATIONS; rotation++) {
            List<TzHaarWaves.CaveSpawn> first = new ArrayList<>(TzHaarWaves.fightCaveSpawns(rotation, 1));
            first.addAll(TzHaarWaves.fightCaveSpawns(rotation, 2));
            first.addAll(TzHaarWaves.fightCaveSpawns(rotation, 4));
            openings.add(first);
        }
        assertEquals(TzHaarWaves.FIGHT_CAVE_ROTATIONS, openings.size(), "every rotation is told apart by waves 1, 2 and 4");
    }

    @Test
    void infernoWavesMatchTheWiki() throws IOException {
        List<String[]> rows = rows("inferno_waves.tsv");
        assertEquals(68, rows.size());
        for (String[] r : rows) {
            int wave = Integer.parseInt(r[0]);
            Map<String, Integer> wiki = new LinkedHashMap<>();
            for (String s : r[1].split(",")) {
                String[] kv = s.split("=");
                wiki.merge(kv[0], Integer.parseInt(kv[1]), Integer::sum);
            }
            Map<String, Integer> mine = new LinkedHashMap<>();
            for (String id : TzHaarWaves.inferno(wave)) {
                mine.merge(id, 1, Integer::sum);
            }
            assertEquals(new java.util.TreeMap<>(wiki), new java.util.TreeMap<>(mine), "wave " + wave);
        }
        assertEquals(List.of(TzHaarWaves.ZUK), TzHaarWaves.inferno(69));
    }

    @Test
    void fightCaveTokkulIsTheWikiFormula() {
        assertEquals(0, TzHaarWaves.fightCaveTokkul(0, false));
        assertEquals(2, TzHaarWaves.fightCaveTokkul(1, false));
        assertEquals(6, TzHaarWaves.fightCaveTokkul(2, false));
        assertEquals(62 * 63, TzHaarWaves.fightCaveTokkul(62, false));
        assertEquals(TzHaarWaves.FIGHT_CAVE_TOKKUL, TzHaarWaves.fightCaveTokkul(63, true));
        assertEquals(16064, TzHaarWaves.diaryTokkul(TzHaarWaves.fightCaveTokkul(63, true), true));
        assertEquals(TzHaarWaves.INFERNO_TOKKUL, TzHaarWaves.infernoTokkul(69));
        int last = -1;
        for (int n = 0; n <= 69; n++) {
            int t = TzHaarWaves.infernoTokkul(n);
            assertTrue(t > last || n == 0, "Inferno tokkul grows with the waves (" + n + ")");
            last = t;
        }
    }

    @Test
    void healerRules() {
        assertTrue(TzHaarWaves.healersDue(125, 250));
        assertFalse(TzHaarWaves.healersDue(126, 250));
        assertFalse(TzHaarWaves.healersDue(0, 250));
        // Fight Cave healers: adjacent, or tagged within 4 tiles; JalTok-Jad's: untagged and adjacent only.
        assertTrue(TzHaarWaves.hurKotHeals(TzHaarWaves.JAD, 1, false));
        assertTrue(TzHaarWaves.hurKotHeals(TzHaarWaves.JAD, 4, true));
        assertFalse(TzHaarWaves.hurKotHeals(TzHaarWaves.JAD, 3, false));
        assertFalse(TzHaarWaves.hurKotHeals(TzHaarWaves.JAD, 5, true));
        assertTrue(TzHaarWaves.hurKotHeals(TzHaarWaves.JALTOK_JAD, 1, false));
        assertFalse(TzHaarWaves.hurKotHeals(TzHaarWaves.JALTOK_JAD, 1, true));
        assertTrue(TzHaarWaves.healersRespawn(TzHaarWaves.JAD));
        assertFalse(TzHaarWaves.healersRespawn(TzHaarWaves.JALTOK_JAD));
        assertEquals(List.of(TzHaarWaves.CaveSpawn.SE, TzHaarWaves.CaveSpawn.S, TzHaarWaves.CaveSpawn.NW, TzHaarWaves.CaveSpawn.SW),
                TzHaarWaves.HEALER_SPAWNS);
    }

    @Test
    void infernoMechanics() {
        assertEquals(255, TzHaarWaves.PILLAR_HP);
        assertEquals(50, TzHaarWaves.pillarCollapseDamage(100));
        assertFalse(TzHaarWaves.revivable(TzHaarWaves.JAL_NIB));
        assertFalse(TzHaarWaves.revivable("jal_akrek_ket"));
        assertTrue(TzHaarWaves.revivable(TzHaarWaves.JAL_AK));
        assertTrue(TzHaarWaves.revivable(TzHaarWaves.JAL_XIL));
        // Jal-Ak attacks with the style the read prayer doesn't block.
        assertEquals("ranged", TzHaarWaves.blobStyle(true, false, false, false, 0.9));
        assertEquals("magic", TzHaarWaves.blobStyle(false, true, false, false, 0.1));
        assertEquals("melee", TzHaarWaves.blobStyle(false, true, false, true, 0.1));
        assertEquals("magic", TzHaarWaves.blobStyle(false, true, true, true, 0.1));
        Random r = new Random(4);
        for (int i = 0; i < 200; i++) {
            int heal = TzHaarWaves.mejJakHeal(r);
            assertTrue(heal >= 15 && heal <= 24);
            int blast = TzHaarWaves.mejJakBlast(r);
            assertTrue(blast >= 5 && blast <= 10);
        }
    }

    /** Attack styles and max hits from the wiki infoboxes (melee styles were swapped and three melee max hits invented). */
    @Test
    void monsterAttacksMatchTheWiki() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("tok_xil", "RANGED 14, CRUSH 13");
        expected.put("ket_zek", "MAGIC 52, STAB 55");
        expected.put("jal_xil", "RANGED 46, CRUSH 19");
        expected.put("jal_zek", "MAGIC 70, STAB 52");
        expected.put("jaltok_jad", "MAGIC 113, RANGED 113, STAB 113");
        expected.put("jal_ak", "CRUSH 29, RANGED 29, MAGIC 29");
        expected.put("jal_imkot", "SLASH 49");
        expected.put("jal_mejrah", "RANGED 19");
        expected.put("jal_mejjak", "MAGIC 10");
        expected.put("yt_hurkot", "CRUSH 14");
        expected.put("yt_hurkot_inferno", "CRUSH 18");
        for (Map.Entry<String, String> e : expected.entrySet()) {
            com.runecraft.osrs.content.MonsterDef m = com.runecraft.osrs.content.ContentRegistry.monster(e.getKey());
            List<String> attacks = new ArrayList<>();
            for (com.runecraft.osrs.content.AttackDef a : m.attacks) {
                attacks.add(a.style() + " " + a.maxHit());
            }
            assertEquals(e.getValue(), String.join(", ", attacks), e.getKey());
        }
        assertEquals(141, com.runecraft.osrs.content.ContentRegistry.monster(TzHaarWaves.HURKOT_INFERNO).combatLevel);
        assertEquals(90, com.runecraft.osrs.content.ContentRegistry.monster(TzHaarWaves.HURKOT_INFERNO).hitpoints);
        assertEquals(TzHaarWaves.HURKOT, TzHaarWaves.healerOf(TzHaarWaves.JAD));
        assertEquals(TzHaarWaves.HURKOT_INFERNO, TzHaarWaves.healerOf(TzHaarWaves.JALTOK_JAD));
    }

    /** TzRek-Jad's metamorphosis after TzHaar-Ket-Rak's challenges, Jal-nib-rek's always; forms have no log entry. */
    @Test
    void petMetamorphoses() {
        assertFalse(TzHaarWaves.canMetamorphose("tzrek_jad", false));
        assertTrue(TzHaarWaves.canMetamorphose("tzrek_jad", true));
        assertTrue(TzHaarWaves.canMetamorphose("jalrek_jad", true));
        assertTrue(TzHaarWaves.canMetamorphose("jal_nib_rek", false));
        assertEquals("jalrek_jad", com.runecraft.osrs.content.extras.Pets.metamorphosis("tzrek_jad"));
        assertEquals("tzrek_jad", com.runecraft.osrs.content.extras.Pets.metamorphosis("jalrek_jad"));
        assertEquals("tzrek_zuk", com.runecraft.osrs.content.extras.Pets.metamorphosis("jal_nib_rek"));
        assertTrue(com.runecraft.osrs.content.extras.Pets.isPet("jalrek_jad"));
        assertTrue(com.runecraft.osrs.content.extras.Pets.isVariant("tzrek_zuk"));
        assertTrue(com.runecraft.osrs.content.extras.Pets.all().stream().noneMatch(p -> p.id().equals("jalrek_jad")));
        assertEquals("tztok_jad", com.runecraft.osrs.content.extras.Pets.get("tzrek_jad").look());
        assertEquals("jaltok_jad", com.runecraft.osrs.content.extras.Pets.get("jalrek_jad").look());
        com.runecraft.osrs.content.ContentRegistry.monsters();
        assertTrue(com.runecraft.osrs.ItemCatalog.get("jalrek_jad") != null);
        assertEquals(8000, TzHaarWaves.FIRE_CAPE_TOKKUL);
    }

    @Test
    void zukTimers() {
        // Counting down normally, paused at 600 hitpoints until the Jad spawns, then running again.
        assertEquals(99, TzHaarWaves.zukSetTick(100, 800, false));
        assertEquals(100, TzHaarWaves.zukSetTick(100, 600, false));
        assertEquals(99, TzHaarWaves.zukSetTick(100, 480, true));
        assertEquals(1200, TzHaarWaves.zukHitpoints(1024, 1024));
        assertEquals(480, TzHaarWaves.zukHitpoints(409.6f, 1024));
        assertEquals(240, TzHaarWaves.zukHitpoints(204.8f, 1024));
        assertEquals(350, TzHaarWaves.ZUK_SET_TICKS);
        assertEquals(175, TzHaarWaves.ZUK_SET_DELAY);
        assertEquals(7, TzHaarWaves.ZUK_ENRAGED_SPEED);
    }

    /** Logging out pauses a run between waves or 10 s out of combat; dropping out in combat is a death (no free retries). */
    @Test
    void loggingOut() {
        assertEquals(17, TzHaarWaves.LOGOUT_COMBAT_TICKS);
        assertEquals(TzHaarWaves.Logout.END, TzHaarWaves.logout(0, true, Long.MAX_VALUE, false));
        // Between waves: always a pause, even right after the last kill.
        assertEquals(TzHaarWaves.Logout.PAUSE_NEXT_WAVE, TzHaarWaves.logout(12, true, 0, false));
        // Mid-wave: only out of combat for 10 seconds and with nothing attacking.
        assertEquals(TzHaarWaves.Logout.PAUSE_REPEAT_WAVE, TzHaarWaves.logout(12, false, 17, false));
        assertEquals(TzHaarWaves.Logout.DEATH, TzHaarWaves.logout(12, false, 16, false));
        assertEquals(TzHaarWaves.Logout.DEATH, TzHaarWaves.logout(63, false, Long.MAX_VALUE, true));
        // Jad rearing up while the player prays wrong: the hit is 3 ticks off, the last one 8 ticks ago.
        assertEquals(TzHaarWaves.Logout.DEATH, TzHaarWaves.logout(63, false, 8, true));
    }
}
