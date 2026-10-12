package com.runecraft.osrs;

import com.runecraft.osrs.content.quests.QuestUnlocks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The standard prayer book against the OSRS wiki: levels, drain effects, bonuses, exclusivity and special effects. */
class PrayerAuditTest {
    /** name | level | drain effect | attack% | strength% | defence% | ranged acc% | ranged str% | magic acc% | magic dmg%. */
    private static final String[] WIKI = {
            "Thick Skin|1|1|0|0|5|0|0|0|0",
            "Burst of Strength|4|1|0|5|0|0|0|0|0",
            "Clarity of Thought|7|1|5|0|0|0|0|0|0",
            "Sharp Eye|8|1|0|0|0|5|5|0|0",
            "Mystic Will|9|1|0|0|0|0|0|5|0",
            "Rock Skin|10|6|0|0|10|0|0|0|0",
            "Superhuman Strength|13|6|0|10|0|0|0|0|0",
            "Improved Reflexes|16|6|10|0|0|0|0|0|0",
            "Rapid Restore|19|1|0|0|0|0|0|0|0",
            "Rapid Heal|22|2|0|0|0|0|0|0|0",
            "Protect Item|25|2|0|0|0|0|0|0|0",
            "Hawk Eye|26|6|0|0|0|10|10|0|0",
            "Mystic Lore|27|6|0|0|0|0|0|10|1",
            "Steel Skin|28|12|0|0|15|0|0|0|0",
            "Ultimate Strength|31|12|0|15|0|0|0|0|0",
            "Incredible Reflexes|34|12|15|0|0|0|0|0|0",
            "Protect from Magic|37|12|0|0|0|0|0|0|0",
            "Protect from Missiles|40|12|0|0|0|0|0|0|0",
            "Protect from Melee|43|12|0|0|0|0|0|0|0",
            "Eagle Eye|44|12|0|0|0|15|15|0|0",
            "Mystic Might|45|12|0|0|0|0|0|15|2",
            "Retribution|46|3|0|0|0|0|0|0|0",
            "Redemption|49|6|0|0|0|0|0|0|0",
            "Smite|52|18|0|0|0|0|0|0|0",
            "Preserve|55|2|0|0|0|0|0|0|0",
            "Chivalry|60|24|15|18|20|0|0|0|0",
            "Deadeye|62|12|0|0|5|18|18|0|0",
            "Mystic Vigour|63|12|0|0|5|0|0|18|3",
            "Piety|70|24|20|23|25|0|0|0|0",
            "Rigour|74|24|0|0|25|20|23|0|0",
            "Augury|77|24|0|0|25|0|0|25|4",
    };

    private static Prayer byName(String name) {
        for (Prayer p : Prayer.values()) {
            if (p.displayName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    private static int pct(double multiplier) {
        return (int) Math.round((multiplier - 1.0) * 100);
    }

    @Test
    void everyPrayerMatchesTheWiki() {
        List<String> problems = new ArrayList<>();
        for (String row : WIKI) {
            String[] f = row.split("\\|");
            Prayer p = byName(f[0]);
            if (p == null) {
                problems.add(f[0] + " missing");
                continue;
            }
            int[] want = new int[f.length - 1];
            for (int i = 1; i < f.length; i++) {
                want[i - 1] = Integer.parseInt(f[i]);
            }
            int defence = pct(p.defenceMultiplier()) + (int) Math.round(p.stackingDefenceBonus() * 100);
            int[] got = {p.level(), p.drainEffect(), pct(p.attackMultiplier()), pct(p.strengthMultiplier()), defence,
                    pct(p.rangedAttackMultiplier()), pct(p.rangedStrengthMultiplier()), pct(p.magicMultiplier()), p.magicDamagePercent()};
            for (int i = 0; i < want.length; i++) {
                if (want[i] != got[i]) {
                    problems.add(f[0] + " column " + i + ": " + got[i] + " != " + want[i]);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
        assertEquals(WIKI.length, Prayer.values().length, "29 classic prayers + Deadeye and Mystic Vigour");
    }

    @Test
    void ruinousPowersIsTrackedAsUnimplemented() {
        assertTrue(PrayerEffects.UNIMPLEMENTED.containsKey("Ruinous Powers"));
    }

    @Test
    void ordinalsStayStable() {
        // Saved in player data as a bit mask and sent to the client by ordinal: never reorder.
        assertEquals(0, Prayer.THICK_SKIN.ordinal());
        assertEquals(28, Prayer.AUGURY.ordinal());
        assertEquals(29, Prayer.DEADEYE.ordinal());
        assertEquals(30, Prayer.MYSTIC_VIGOUR.ordinal());
        assertTrue(Prayer.values().length <= 32, "prayer mask is an int");
    }

    @Test
    void exclusivity() {
        assertTrue(Prayer.DEADEYE.conflictsWith(Prayer.EAGLE_EYE));
        assertTrue(Prayer.DEADEYE.conflictsWith(Prayer.RIGOUR));
        assertTrue(Prayer.DEADEYE.conflictsWith(Prayer.MYSTIC_VIGOUR));
        assertTrue(Prayer.DEADEYE.conflictsWith(Prayer.PIETY));
        assertTrue(Prayer.DEADEYE.conflictsWith(Prayer.ULTIMATE_STRENGTH));
        assertFalse(Prayer.DEADEYE.conflictsWith(Prayer.STEEL_SKIN));
        assertFalse(Prayer.MYSTIC_VIGOUR.conflictsWith(Prayer.STEEL_SKIN));
        assertTrue(Prayer.MYSTIC_VIGOUR.conflictsWith(Prayer.AUGURY));
        assertTrue(Prayer.RIGOUR.conflictsWith(Prayer.STEEL_SKIN));
        assertTrue(Prayer.AUGURY.conflictsWith(Prayer.ROCK_SKIN));
        assertTrue(Prayer.PIETY.conflictsWith(Prayer.RIGOUR));
        // Overheads: one at a time.
        Prayer[] overheads = {Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE,
                Prayer.RETRIBUTION, Prayer.REDEMPTION, Prayer.SMITE};
        for (Prayer a : overheads) {
            assertTrue(a.isOverhead());
            for (Prayer b : overheads) {
                assertEquals(a != b, a.conflictsWith(b), a + " vs " + b);
            }
            assertFalse(a.conflictsWith(Prayer.PIETY));
            assertFalse(a.conflictsWith(Prayer.PROTECT_ITEM));
        }
        for (Prayer p : new Prayer[]{Prayer.RAPID_RESTORE, Prayer.RAPID_HEAL, Prayer.PROTECT_ITEM, Prayer.PRESERVE}) {
            for (Prayer q : Prayer.values()) {
                assertFalse(p.conflictsWith(q), p + " vs " + q);
            }
        }
    }

    @Test
    void defenceStacking() {
        assertEquals(1.15, Prayer.defenceMultiplier(EnumSet.of(Prayer.STEEL_SKIN)), 1e-9);
        assertEquals(1.20, Prayer.defenceMultiplier(EnumSet.of(Prayer.STEEL_SKIN, Prayer.DEADEYE)), 1e-9);
        assertEquals(1.05, Prayer.defenceMultiplier(EnumSet.of(Prayer.MYSTIC_VIGOUR)), 1e-9);
        assertEquals(1.25, Prayer.defenceMultiplier(EnumSet.of(Prayer.PIETY)), 1e-9);
        assertEquals(1.0, Prayer.defenceMultiplier(Set.of()), 1e-9);
        assertEquals(3, Prayer.magicDamagePercent(EnumSet.of(Prayer.MYSTIC_VIGOUR, Prayer.STEEL_SKIN)));
    }

    @Test
    void unlockRequirements() {
        assertEquals("dexterous_prayer_scroll", Prayer.RIGOUR.unlockScroll());
        assertEquals("arcane_prayer_scroll", Prayer.AUGURY.unlockScroll());
        assertEquals("torn_prayer_scroll", Prayer.PRESERVE.unlockScroll());
        assertEquals(Prayer.DEADEYE, Prayer.taughtBy("deadeye_prayer_scroll"));
        assertEquals(Prayer.MYSTIC_VIGOUR, Prayer.taughtBy("mystic_vigour_prayer_scroll"));
        assertNull(Prayer.PIETY.unlockScroll());
        assertNull(Prayer.taughtBy("bones"));
        for (Prayer p : Prayer.values()) {
            if (p.unlockScroll() != null) {
                assertNotNull(ItemCatalog.get(p.unlockScroll()), p.unlockScroll());
            }
        }
        // Chivalry/Piety: King's Ransom (Knight Waves) plus 65/70 Defence; Rigour/Augury: 70 Defence.
        SkillSet low = new SkillSet();
        assertNotNull(QuestUnlocks.prayer(Prayer.PIETY, low, q -> true));
        assertNotNull(QuestUnlocks.prayer(Prayer.RIGOUR, low, q -> true));
        assertNotNull(QuestUnlocks.prayer(Prayer.AUGURY, low, q -> true));
        assertNull(QuestUnlocks.prayer(Prayer.SMITE, low, q -> false));
    }

    @Test
    void specialPrayerEffects() {
        // Smite: 1 prayer point per 4 damage.
        assertEquals(0, PrayerEffects.smiteDrain(3));
        assertEquals(10, PrayerEffects.smiteDrain(43));
        // Redemption: below 10% HP, heal 25% of Prayer level.
        assertTrue(PrayerEffects.redemptionTriggers(9, 99));
        assertFalse(PrayerEffects.redemptionTriggers(10, 99));
        assertFalse(PrayerEffects.redemptionTriggers(0, 99));
        assertEquals(24, PrayerEffects.redemptionHeal(99));
        assertEquals(12, PrayerEffects.redemptionHeal(49));
        // Retribution: up to 25% of Prayer level.
        assertEquals(24, PrayerEffects.retributionMaxHit(99));
        // Items kept on death.
        assertEquals(3, PrayerEffects.itemsKept(false, false));
        assertEquals(4, PrayerEffects.itemsKept(false, true));
        assertEquals(0, PrayerEffects.itemsKept(true, false));
        assertEquals(1, PrayerEffects.itemsKept(true, true));
        // Rapid Heal / Rapid Restore halve the regen interval, Preserve makes boosts last 50% longer.
        assertEquals(50, PrayerEffects.hitpointsRegenTicks(true));
        assertEquals(100, PrayerEffects.hitpointsRegenTicks(false));
        assertEquals(50, PrayerEffects.statRestoreTicks(true));
        assertEquals(150, PrayerEffects.boostDecayTicks(true));
        assertEquals(100, PrayerEffects.boostDecayTicks(false));
    }

    @Test
    void keptOnDeathPicksTheMostValuableItems() {
        List<PrayerEffects.Carried> inv = List.of(new PrayerEffects.Carried(0, 1, 100), new PrayerEffects.Carried(1, 5, 5000),
                new PrayerEffects.Carried(2, 1, 2_000_000), new PrayerEffects.Carried(3, 1, 300));
        List<PrayerEffects.Kept> kept = PrayerEffects.keptOnDeath(inv, 4);
        assertEquals(List.of(new PrayerEffects.Kept(2, 1), new PrayerEffects.Kept(1, 3)), kept);
        assertEquals(List.of(), PrayerEffects.keptOnDeath(inv, 0));
        assertEquals(4, PrayerEffects.keptOnDeath(List.of(new PrayerEffects.Carried(0, 1, 1), new PrayerEffects.Carried(1, 1, 1),
                new PrayerEffects.Carried(2, 1, 1), new PrayerEffects.Carried(3, 1, 1), new PrayerEffects.Carried(4, 1, 1)), 4).size());
    }

    @Test
    void drainResistanceModel() {
        // A drain effect of 12 against resistance 60 (no prayer bonus) loses a point every 5 ticks = 1 per 3 s.
        int counter = 0;
        int points = 0;
        for (int tick = 0; tick < 100; tick++) {
            counter += Prayer.PROTECT_FROM_MELEE.drainEffect();
            while (counter > 60) {
                counter -= 60;
                points++;
            }
        }
        assertEquals(19, points); // ~20 per minute
    }
}
