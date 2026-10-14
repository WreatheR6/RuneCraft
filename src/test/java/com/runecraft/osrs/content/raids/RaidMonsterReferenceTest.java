package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs every raid creature's base (solo, normal mode) stats against a snapshot of the OSRS wiki DPS calculator's
 * monsters.json ({@code src/test/resources/osrs/raids/raid_monster_reference.tsv}). Party, mode and raid level
 * scaling is applied on top of these by {@link RaidScaling}.
 */
class RaidMonsterReferenceTest {
    /** Creatures whose attack speed is scripted rather than a regular auto-attack. */
    private static final Set<String> SCRIPTED_SPEED = Set.of(
            "pestilent_bloat", // flies every tick while someone stays in line of sight; handled by the room
            "vespula_portal", // never attacks
            "vasa_crystal", // a passive object
            "cox_crystal_crab");

    private static List<String[]> rows() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/raids/raid_monster_reference.tsv"),
                StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                rows.add(line.split("\t", -1));
            }
        }
        return rows;
    }

    private static CombatProfile<?> profile(String id) {
        CombatProfile<?> p = ContentRegistry.boss(id);
        return p != null ? p : ContentRegistry.monster(id);
    }

    @Test
    void raidCreaturesMatchTheDpsCalculator() throws Exception {
        List<String[]> rows = rows();
        assertTrue(rows.size() >= 50, "rows " + rows.size());
        List<String> diffs = new ArrayList<>();
        for (String[] r : rows) {
            String id = r[0];
            CombatProfile<?> p = profile(id);
            if (p == null) {
                diffs.add(id + ": not registered");
                continue;
            }
            int level = Integer.parseInt(r[3]);
            if (level > 0 && p.combatLevel != level) {
                diffs.add(id + " level " + p.combatLevel + " != " + level);
            }
            int[] want = new int[12];
            for (int i = 0; i < 12; i++) {
                want[i] = Integer.parseInt(r[4 + i]);
            }
            int[] have = {p.hitpoints, p.attack, p.strength, p.defence, p.magic, p.ranged,
                    p.defenceBonus(AttackType.STAB), p.defenceBonus(AttackType.SLASH), p.defenceBonus(AttackType.CRUSH),
                    p.defenceBonus(AttackType.MAGIC), p.defenceBonus(AttackType.RANGED), p.attackSpeed};
            String[] names = {"hp", "atk", "str", "def", "magic", "ranged", "dstab", "dslash", "dcrush", "dmagic", "dranged", "speed"};
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals("speed") && (want[i] <= 0 || SCRIPTED_SPEED.contains(id))) {
                    continue;
                }
                if (have[i] != want[i]) {
                    diffs.add(id + " " + names[i] + " " + have[i] + " != " + want[i]);
                }
            }
        }
        assertEquals(List.of(), diffs);
    }

    @Test
    void coxGuardianDamageAndHitpoints() {
        // OSRS: damage x (50 + Mining + pickaxe level) / 150, the pickaxe level capped at the dragon pickaxe's 61.
        assertEquals(42, RaidScaling.coxGuardianDamage(30, 99, 61));
        assertEquals(42, RaidScaling.coxGuardianDamage(30, 99, 71), "crystal pickaxe capped at dragon");
        assertEquals(10, RaidScaling.coxGuardianDamage(30, 1, 1));
        assertEquals(61, RaidScaling.pickaxeLevel("dragon_pickaxe"));
        assertEquals(41, RaidScaling.pickaxeLevel("rune_pickaxe"));
        assertEquals(71, RaidScaling.pickaxeLevel("crystal_pickaxe"));
        assertEquals(1, RaidScaling.pickaxeLevel("bronze_pickaxe"));
        // 151 x (1 + floor(team / 2)) + floor(average Mining) x team: 250 for a maxed solo.
        assertEquals(250, RaidScaling.coxGuardianHp(1, 99));
        assertEquals(948, RaidScaling.coxGuardianHp(5, 99));
        assertEquals(152, RaidScaling.coxGuardianHp(1, 1));
    }

    @Test
    void toaRaidLevelScaling() {
        // OSRS wiki: +2% damage per 5 raid levels, capped at +150%.
        assertEquals(1.0, RaidScaling.toaDamage(0), 1e-9);
        assertEquals(1.4, RaidScaling.toaDamage(100), 1e-9);
        assertEquals(1.4, RaidScaling.toaDamage(104), 1e-9);
        assertEquals(2.2, RaidScaling.toaDamage(300), 1e-9);
        assertEquals(2.5, RaidScaling.toaDamage(500), 1e-9);
        assertEquals(3.0, RaidScaling.toaAccuracy(500), 1e-9);
    }

    @Test
    void invocationEffects() {
        assertEquals(10, Invocation.attempts(EnumSet.of(Invocation.TRY_AGAIN)));
        assertEquals(5, Invocation.attempts(EnumSet.of(Invocation.PERSISTENCE)));
        assertEquals(3, Invocation.attempts(EnumSet.of(Invocation.SOFTCORE_RUN)));
        assertEquals(1, Invocation.attempts(EnumSet.of(Invocation.HARDCORE_RUN)));
        assertEquals(-1, Invocation.attempts(EnumSet.noneOf(Invocation.class)));
        assertEquals(9, Invocation.roomRetries(EnumSet.of(Invocation.TRY_AGAIN)));
        assertEquals(40, Invocation.timeLimitMinutes(EnumSet.of(Invocation.WALK_FOR_IT)));
        assertEquals(25, Invocation.timeLimitMinutes(EnumSet.of(Invocation.SPRINT_FOR_IT)));
        assertEquals(30, Invocation.timeLimitPenalty(EnumSet.of(Invocation.JOG_FOR_IT)));
        assertEquals(0, Invocation.timeLimitMinutes(EnumSet.noneOf(Invocation.class)));
        assertEquals(0.1, Invocation.protectionLeak(EnumSet.of(Invocation.QUIET_PRAYERS)), 1e-9);
        assertEquals(0.2, Invocation.prayerDrainPerDamage(EnumSet.of(Invocation.DEADLY_PRAYERS)), 1e-9);
        assertFalse(Invocation.foodAllowed(EnumSet.of(Invocation.ON_A_DIET)));
        assertTrue(Invocation.healingPotionsAllowed(EnumSet.of(Invocation.ON_A_DIET)));
        assertFalse(Invocation.healingPotionsAllowed(EnumSet.of(Invocation.DEHYDRATION)));
        assertEquals(3, Invocation.zebakBloodClouds(EnumSet.of(Invocation.NOT_JUST_A_HEAD, Invocation.BLOOD_THINNERS)));
        assertEquals(0, Invocation.zebakBloodClouds(EnumSet.of(Invocation.BLOOD_THINNERS)));
        assertEquals(4, Invocation.kephriEggs(EnumSet.of(Invocation.LIVELY_LARVAE)));
        assertEquals(6, Invocation.akkhaSpecialEvery(EnumSet.of(Invocation.STAY_VIGILANT)));
    }
}
