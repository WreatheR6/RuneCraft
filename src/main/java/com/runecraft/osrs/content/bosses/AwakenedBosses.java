package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The awakened Desert Treasure II bosses (OSRS Wiki, osrs-dps-calc "Awakened" versions). Consuming an awakener's orb at
 * a boss's summoning stone starts its awakened fight: much higher stats and max hits, stronger mechanics, three times
 * the chance of the tradeable unique table and a separate kill count. Killing the last of the four awakened bosses for
 * the first time gives the ancient blood ornament kit.
 */
public final class AwakenedBosses {
    private AwakenedBosses() {
    }

    public static final String ORB = "awakeners_orb";
    public static final String BLOOD_KIT = "ancient_blood_ornament_kit";
    public static final List<String> BOSSES = List.of("duke_sucellus", "the_leviathan", "the_whisperer", "vardorvis");
    /** Items on the tradeable unique table (rolled three times as often when awakened). */
    public static final Set<String> UNIQUE_TABLE = Set.of("chromium_ingot", "magus_vestige", "venator_vestige", "bellator_vestige",
            "ultor_vestige", "eye_of_the_duke", "leviathans_lure", "sirens_staff", "executioners_axe_head", "virtus_mask",
            "virtus_robe_top", "virtus_robe_bottom");

    /**
     * OSRS: a 1/n roll for the tradeable unique table (3/n awakened), then the awakener's orb (1/orb) if that failed.
     * Duke 1/90 and 1/48, Leviathan 1/96 and 1/53, Whisperer 1/64 and 1/34, Vardorvis 1/136 and 1/80.
     */
    public static int uniqueTableRate(String bossId) {
        return switch (bossId) {
            case "duke_sucellus" -> 90;
            case "the_leviathan" -> 96;
            case "the_whisperer" -> 64;
            case "vardorvis" -> 136;
            default -> throw new IllegalArgumentException(bossId);
        };
    }

    public static int orbRate(String bossId) {
        return switch (bossId) {
            case "duke_sucellus" -> 48;
            case "the_leviathan" -> 53;
            case "the_whisperer" -> 34;
            case "vardorvis" -> 80;
            default -> throw new IllegalArgumentException(bossId);
        };
    }

    public static boolean isDt2(String bossId) {
        return BOSSES.contains(bossId);
    }

    /**
     * Whether a boss is outside Slayer: an awakened Desert Treasure II boss never counts as the player's task, so the black
     * mask and slayer helmet give no bonus and its kill takes nothing off a boss task (OSRS Wiki, each boss's "Awakened
     * mode": "The black mask and Slayer helmet (i) do not apply their usual on-task damage and accuracy bonuses";
     * news 3 August 2023: "Slayer tasks no longer apply to the Awakened versions of the Desert Treasure II bosses").
     */
    public static boolean offTask(boolean awakened) {
        return awakened;
    }

    /** Awakened bosses are immune to stat drains (OSRS Wiki, each boss's "Awakened mode"). */
    public static boolean drainImmune(boolean awakened) {
        return awakened;
    }

    /**
     * An awakened boss's defence bonus against a style where it differs from the post-quest one (osrs-dps-calc "Awakened"
     * versions: only Duke Sucellus's slash defence, +65 instead of +45), or null.
     */
    public static Integer defenceBonus(String bossId, com.runecraft.osrs.AttackType type) {
        return "duke_sucellus".equals(bossId) && type == com.runecraft.osrs.AttackType.SLASH ? 65 : null;
    }

    /** Kill-count key of a boss's awakened kills. */
    public static String kcKey(String bossId) {
        return bossId + "_awakened";
    }

    /** The awakened boss's display name. */
    public static String name(String bossId) {
        return switch (bossId) {
            case "duke_sucellus" -> "Awakened Duke Sucellus";
            case "the_leviathan" -> "The Awakened Leviathan";
            case "the_whisperer" -> "The Awakened Whisperer";
            case "vardorvis" -> "Awakened Vardorvis";
            default -> throw new IllegalArgumentException(bossId);
        };
    }

    /** Awakened stat line: combat level, hitpoints, Attack, Strength, Defence, Magic, Ranged (osrs-dps-calc). */
    static int[] stats(String bossId) {
        return switch (bossId) {
            case "duke_sucellus" -> new int[]{1099, 1697, 435, 500, 316, 465, 1};
            case "the_leviathan" -> new int[]{1157, 2700, 525, 630, 287, 280, 280};
            case "the_whisperer" -> new int[]{1146, 2700, 378, 378, 300, 225, 243};
            // Vardorvis' Strength and Defence scale with his remaining hitpoints; these are the full-health values.
            case "vardorvis" -> new int[]{1136, 1400, 420, 391, 268, 215, 1};
            default -> throw new IllegalArgumentException(bossId);
        };
    }

    /** Awakened max hits by attack name (OSRS Wiki infobox "version2"); unnamed attacks scale with the melee ratio. */
    static Map<String, Integer> maxHits(String bossId) {
        Map<String, Integer> m = new LinkedHashMap<>();
        switch (bossId) {
            case "duke_sucellus" -> {
                m.put("fist_slam", 81);
                m.put("magic_blast", 71);
                m.put("freezing_gaze", 121);
                m.put("gas_vent", 27);
            }
            case "the_leviathan" -> {
                m.put("shadow_strike", 86);
                m.put("boulder", 41);
                m.put("abyssal_orb", 55);
                m.put("smoke_blast", 70); // "70+" (OSRS Wiki "The Leviathan/Strategies", awakened mode)
                m.put("falling_debris", 25); // approx: the wiki gives no awakened debris damage
                m.put("tornado", 45); // "40+"
            }
            case "the_whisperer" -> {
                m.put("soul_bolt", 45);
                m.put("tentacle_lash", 53);
                m.put("screech", 70);
                m.put("soul_siphon", 75);
                m.put("tentacle_whack", 72); // 50-72 off prayer (OSRS Wiki "The Whisperer/Strategies", awakened mode)
            }
            case "vardorvis" -> {
                m.put("axe_swing", 61);
                m.put("swinging_axes", 96);
                m.put("darting_spikes", 40);
            }
            default -> throw new IllegalArgumentException(bossId);
        }
        return m;
    }

    /** Scale for attacks without a listed awakened max hit: the awakened/normal melee max hit ratio. */
    static double otherScale(String bossId) {
        return switch (bossId) {
            case "duke_sucellus" -> 81.0 / 56;
            case "the_leviathan" -> 86.0 / 50;
            case "the_whisperer" -> 70.0 / 42;
            case "vardorvis" -> 61.0 / 43;
            default -> throw new IllegalArgumentException(bossId);
        };
    }

    /** The awakened drop table: the boss's own table with every tradeable unique three times as likely. */
    public static List<Drop> drops(List<Drop> base) {
        List<Drop> out = new ArrayList<>();
        for (Drop d : base) {
            out.add(UNIQUE_TABLE.contains(d.item()) ? new Drop(d.item(), d.min(), d.max(), Math.max(1, Math.round(d.rarity() / 3f))) : d);
        }
        return out;
    }

    /** The awakened version of a Desert Treasure II boss. */
    public static BossVariant variant(BossDef boss) {
        int[] s = stats(boss.id);
        return new BossVariant(boss.id, BossVariant.AWAKENED, 1, name(boss.id), kcKey(boss.id), s[0], s[1], s[2], s[3], s[4], s[5], s[6],
                0, maxHits(boss.id), otherScale(boss.id), drops(boss.drops), -1);
    }

    /**
     * Whether an awakened kill earns the ancient blood ornament kit: it was this boss's first awakened kill and the
     * other three awakened bosses have already been killed. {@code kc} holds the kill counts after this kill.
     */
    public static boolean earnsBloodKit(String bossId, Map<String, Integer> kc) {
        if (kc.getOrDefault(kcKey(bossId), 0) != 1) {
            return false;
        }
        for (String b : BOSSES) {
            if (kc.getOrDefault(kcKey(b), 0) < 1) {
                return false;
            }
        }
        return true;
    }
}
