package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;

/**
 * The Moons of Peril (OSRS Wiki "Moons of Peril/Strategies", "Blue Moon", "Eclipse Moon", "Blood Moon", "Lunar Chest") as
 * pure rules the fight scripts apply.
 *
 * <ul>
 *   <li><b>Standard attack</b> (every 6 ticks): three typeless hits, maxing 4, 8 and 20, rolled against the player's melee
 *   defence; once one misses the rest miss. Each landed hit adds 1, 2 and 5 stacks of the Curse of the Moon (up to 127,
 *   losing 2 every 5 ticks). Protection prayers do nothing.</li>
 *   <li><b>Eyatlalli's glyph</b>: one of eight sigils round the Moon glows; off it during standard attacks the player takes
 *   1, 2, 4, 8 then 16 a tick, a curse stack each. It moves on clockwise after every two standard attacks; reaching the full
 *   or the new moon starts that moon's special (so one every six standard attacks, alternating).</li>
 *   <li><b>Curses</b>: Blue - 18 stacks lock the player's joints, cancelling their next attack (and taking the 18); Eclipse - a
 *   chance (stacks %) that an attack glances, its max hit 12 lower (8 during the Clones); Blood - her standard hits heal her
 *   1x, 2x and 3x their damage (the third 30-50), her mechanics 2x or 3x.</li>
 *   <li><b>Flat armour</b>: Blue -5, Eclipse 6 (4 during her Clones), Blood -2.</li>
 *   <li><b>Specials</b>: Blue - Weapon Freeze (full) and Frost Storm (new); Eclipse - Clones (full) and Searing Rays (new);
 *   Blood - Blood Rain (full) and Blood Jaguars (new).</li>
 *   <li><b>Lunar Chest</b>: each Moon defeated since the last claim gives a 1/56 roll at a piece of its set (a piece not yet
 *   owned while any is missing); with no unique, the standard table rolls once for one Moon, 3 times for two, 6 for three.
 *   The chest opens for the first time once every Moon has been defeated, after Perilous Moons.</li>
 * </ul>
 */
public final class MoonsRules {
    private MoonsRules() {
    }

    public static final String BLUE = "blue_moon";
    public static final String ECLIPSE = "eclipse_moon";
    public static final String BLOOD = "blood_moon";
    public static final List<String> MOONS = List.of(BLUE, ECLIPSE, BLOOD);

    /** The Lunar Chest in the Ancient Shrine. */
    public static final String LUNAR_CHEST = "zeah_lunar_chest";
    /** A Neypotzli supply crate (Hunter, Fishing and Herblore supplies). */
    public static final String SUPPLY_CRATE = "neypotzli_supply_crate";
    /** A camp's cooking stove: Make-cuppa restores run energy. */
    public static final String COOKING_STOVE = "neypotzli_cooking_stove";

    public static final int ATTACK_SPEED = 6;
    public static final int[] STANDARD_MAX = {4, 8, 20};
    public static final int[] CURSE_PER_HIT = {1, 2, 5};
    public static final int CURSE_MAX = 127;
    public static final int CURSE_DECAY = 2;
    public static final int CURSE_DECAY_EVERY = 5;

    public static final int GLYPHS = 8;
    public static final int FULL_MOON = 0;
    public static final int NEW_MOON = 4;
    /** The first glyph lit (the waxing crescent after the full moon): six standard attacks to the new moon. */
    public static final int FIRST_GLYPH = 1;
    public static final int STANDARD_PER_GLYPH = 2;
    public static final int OFF_GLYPH_MAX = 16;

    public static final int BLUE_FLAT_ARMOUR = BossMechanics.BLUE_MOON_FLAT_ARMOUR;
    public static final int ECLIPSE_FLAT_ARMOUR = BossMechanics.ECLIPSE_FLAT_ARMOUR;
    public static final int ECLIPSE_FLAT_ARMOUR_CLONES = BossMechanics.ECLIPSE_FLAT_ARMOUR_CLONES;
    public static final int BLOOD_FLAT_ARMOUR = -2;

    /** Blue: the curse locks the joints at 18 stacks (warnings at 3 and 9). */
    public static final int BLUE_LOCK = 18;
    public static final int[] BLUE_WARNINGS = {3, 9};
    /** Eclipse: a glancing attack's max hit is 12 lower (8 during the Clones). */
    public static final int ECLIPSE_GLANCE = 12;
    public static final int ECLIPSE_GLANCE_CLONES = 8;
    /** Eclipse Searing Rays: a ray hit is minor damage but a large increase in the curse. approx: both numbers */
    public static final int RAY_MAX = 5;
    public static final int RAY_CURSE = 20;
    /** Blood: the third standard hit heals 3x, at least 30 and at most 50. */
    public static final int BLOOD_THIRD_MIN = 30;
    public static final int BLOOD_THIRD_MAX = 50;

    /** Weapon Freeze: fail to free the weapon for up to 28; no weapon in hand 3-15. approx: punches needed and time */
    public static final int FREEZE_FAIL_MAX = 28;
    public static final int[] FREEZE_NO_WEAPON = {3, 15};
    public static final int FREEZE_PUNCHES = 3;
    public static final int FREEZE_TICKS = 30;
    public static final int FREEZE_BLOCKS = 3;
    /** Frost Storm: five heals of 10 per unlit brazier (two braziers). approx: the storm's length between heals */
    public static final int STORM_HEALS = 5;
    public static final int STORM_HEAL_PER_BRAZIER = 10;
    public static final int STORM_HEAL_EVERY = 4;
    public static final int BRAZIERS = 2;
    /** Blood Rain: pools up to 9 (heal 2x). approx: how long it rains */
    public static final int RAIN_POOL_MAX = 9;
    public static final int RAIN_TICKS = 14;
    /** Blood Jaguars: eight jaguars (summoning them deals up to 15), six cleaves up to 9 (heal 3x), seven sets of pools up to
     *  6 (heal 2x); hitting the jaguar restores up to 25 of the player's hitpoints. */
    public static final int JAGUARS = 8;
    public static final int JAGUAR_SUMMON_MAX = 15;
    public static final int JAGUAR_ATTACKS = 6;
    public static final int JAGUAR_CLEAVE_MAX = 9;
    public static final int JAGUAR_POOL_SETS = 7;
    public static final int JAGUAR_POOL_MAX = 6;
    public static final int JAGUAR_RESTORE_MAX = 25;

    /** Lunar Chest: a 1/56 unique roll per Moon defeated. */
    public static final int UNIQUE_CHANCE = 56;

    public enum Special {
        WEAPON_FREEZE("frozen_weapons"), FROST_STORM("winter_storm"), CLONES("moonlight_clones"), SEARING_RAYS("eclipse"),
        BLOOD_RAIN("blood_rain"), BLOOD_JAGUARS("call_jaguar");

        /** The key Combat Achievements count it under ("used:<key>": High Hitter, CA 527). */
        public final String key;

        Special(String key) {
            this.key = key;
        }
    }

    /** The special a Moon starts on the full ({@code full}) or new moon. */
    public static Special special(String moon, boolean full) {
        return switch (moon) {
            case BLUE -> full ? Special.WEAPON_FREEZE : Special.FROST_STORM;
            case ECLIPSE -> full ? Special.CLONES : Special.SEARING_RAYS;
            case BLOOD -> full ? Special.BLOOD_RAIN : Special.BLOOD_JAGUARS;
            default -> throw new IllegalArgumentException(moon);
        };
    }

    public static int flatArmour(String moon, boolean clones) {
        return switch (moon) {
            case BLUE -> BLUE_FLAT_ARMOUR;
            case ECLIPSE -> clones ? ECLIPSE_FLAT_ARMOUR_CLONES : ECLIPSE_FLAT_ARMOUR;
            case BLOOD -> BLOOD_FLAT_ARMOUR;
            default -> 0;
        };
    }

    /**
     * One standard attack's three hits: {@code chance} to hit (her attack roll against the player's melee defence), each
     * landed hit's damage rolled by {@code roll} (bound exclusive); a miss makes the rest miss. Returns {damage or -1 for a miss}.
     */
    public static int[] standardHits(double chance, DoubleSupplier random, IntUnaryOperator roll) {
        int[] out = {-1, -1, -1};
        for (int i = 0; i < 3; i++) {
            if (random.getAsDouble() >= chance) {
                break;
            }
            out[i] = roll.applyAsInt(STANDARD_MAX[i] + 1);
        }
        return out;
    }

    /** The glyph after {@code glyph} (clockwise). */
    public static int nextGlyph(int glyph) {
        return (glyph + 1) % GLYPHS;
    }

    /** Whether the glyph is the full or new moon (its Moon's special). */
    public static boolean specialGlyph(int glyph) {
        return glyph == FULL_MOON || glyph == NEW_MOON;
    }

    /** Damage of the {@code n}th (from 1) tick in a row off the glyph: 1, 2, 4, 8, then 16. */
    public static int offGlyphDamage(int n) {
        return n <= 0 ? 0 : Math.min(OFF_GLYPH_MAX, 1 << Math.min(n - 1, 5));
    }

    /** Eclipse: the chance an attack glances at {@code stacks} of the curse (100% from 100). */
    public static double glanceChance(int stacks) {
        return Math.min(1.0, Math.max(0, stacks) / 100.0);
    }

    /** Eclipse: a glancing hit, its roll rescaled from {@code max} to {@code max - penalty}. */
    public static int glance(int damage, int max, boolean clones) {
        int penalty = clones ? ECLIPSE_GLANCE_CLONES : ECLIPSE_GLANCE;
        if (max <= 0 || damage <= 0) {
            return Math.max(0, damage);
        }
        int lowered = Math.max(0, max - penalty);
        return (int) Math.round(damage * (double) lowered / max);
    }

    /** Blood: what her {@code i}th (0-2) standard hit of {@code damage} heals her. */
    public static int bloodHeal(int i, int damage) {
        if (damage <= 0) {
            return 0;
        }
        return switch (i) {
            case 0 -> damage;
            case 1 -> 2 * damage;
            default -> Math.max(BLOOD_THIRD_MIN, Math.min(BLOOD_THIRD_MAX, 3 * damage));
        };
    }

    /** Frost Storm: one heal with {@code unlit} braziers out. */
    public static int stormHeal(int unlit) {
        return Math.max(0, unlit) * STORM_HEAL_PER_BRAZIER;
    }

    /**
     * The Moon's own wear (OSRS: she damages herself a little with each attack, less as her health falls). approx: 1% of
     * what she has left, at least 1.
     */
    public static int selfDamage(double hpLeft) {
        return hpLeft <= 1 ? 0 : Math.max(1, (int) Math.round(hpLeft / 100.0));
    }

    /** One player's Curse of the Moon. */
    public static final class Curse {
        private int stacks;
        private int sinceDecay;

        public int stacks() {
            return stacks;
        }

        /** Adds stacks (capped); returns the warning threshold crossed for the Blue Moon (3 or 9), else 0. */
        public int add(int n) {
            int before = stacks;
            stacks = Math.min(CURSE_MAX, stacks + Math.max(0, n));
            for (int w : BLUE_WARNINGS) {
                if (before < w && stacks >= w) {
                    return w;
                }
            }
            return 0;
        }

        public void remove(int n) {
            stacks = Math.max(0, stacks - Math.max(0, n));
        }

        /** One game tick: 2 stacks fall off every 5. */
        public void tick() {
            if (stacks > 0 && ++sinceDecay >= CURSE_DECAY_EVERY) {
                sinceDecay = 0;
                remove(CURSE_DECAY);
            }
        }

        /** Blue: the joints lock (the player's attack is cancelled) at 18 stacks, which the lock takes. */
        public boolean blueLocks() {
            if (stacks >= BLUE_LOCK) {
                stacks -= BLUE_LOCK;
                return true;
            }
            return false;
        }
    }

    // ---------------- the Lunar Chest ----------------

    /** What a claim gives: the uniques won (at most one per Moon) and the standard table's rolls (none after a unique). */
    public record ChestRoll(List<String> uniques, int standardRolls) {
    }

    /** Standard-table rolls for {@code moons} Moons defeated: 1, 3 or 6. */
    public static int standardRolls(int moons) {
        return switch (Math.min(3, Math.max(0, moons))) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 3;
            default -> 6;
        };
    }

    /**
     * A claim at the Lunar Chest for the Moons defeated since the last one: per Moon a 1/56 roll at a piece of its set
     * ({@code sets}: moon -> pieces), a piece the player doesn't own yet ({@code owned}) while one is missing; the standard
     * table only rolls when no unique came. {@code random} gives 0..bound-1.
     */
    public static ChestRoll chest(Collection<String> defeated, Map<String, List<String>> sets, Set<String> owned,
                                  IntUnaryOperator random) {
        List<String> uniques = new ArrayList<>();
        for (String moon : defeated) {
            List<String> set = sets.getOrDefault(moon, List.of());
            if (set.isEmpty() || random.applyAsInt(UNIQUE_CHANCE) != 0) {
                continue;
            }
            List<String> missing = new ArrayList<>();
            for (String piece : set) {
                if (!owned.contains(piece)) {
                    missing.add(piece);
                }
            }
            List<String> pool = missing.isEmpty() ? set : missing;
            uniques.add(pool.get(random.applyAsInt(pool.size())));
        }
        return new ChestRoll(uniques, uniques.isEmpty() ? standardRolls(defeated.size()) : 0);
    }

    /** A Moon's set pieces in its drop table: the lines as rare as a set piece (its four 1/224 lines). */
    public static List<String> setPieces(List<Drop> drops) {
        List<String> out = new ArrayList<>();
        for (Drop d : drops) {
            if (d.rarity() >= 100 && !out.contains(d.item())) {
                out.add(d.item());
            }
        }
        return out;
    }

    /** The chest's standard loot lines in a Moon's drop table (everything but its set). */
    public static List<Drop> standardLines(List<Drop> drops) {
        List<Drop> out = new ArrayList<>();
        for (Drop d : drops) {
            if (d.rarity() < 100) {
                out.add(d);
            }
        }
        return out;
    }

    /** Moon id -> set pieces, from each Moon's drop table. */
    public static Map<String, List<String>> sets(Map<String, List<Drop>> tables) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        tables.forEach((moon, drops) -> out.put(moon, setPieces(drops)));
        return out;
    }
}
