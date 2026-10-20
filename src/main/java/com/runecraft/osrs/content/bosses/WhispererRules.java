package com.runecraft.osrs.content.bosses;

import java.util.ArrayList;
import java.util.List;

/**
 * The Whisperer's Shadow Realm and sanity (OSRS Wiki "The Whisperer/Strategies", "Sanity", "Blackstone fragment",
 * "Tentacle (The Whisperer)", "Lost Soul (The Whisperer)"), as pure rules the fight script applies.
 *
 * <ul>
 *   <li>The fight needs a {@link #FRAGMENT blackstone fragment} in the inventory (the player refuses to fight her without
 *   one; Ketla's workbench in the Lassar Undercity hands out another). It glows during her special attacks: activating it
 *   enters the Shadow Realm, activating it again leaves; the special's end brings everyone back.</li>
 *   <li>Sanity starts at 100%. Outside the realm it restores 1% every 4 ticks (awakened: never); inside it drains 3% and 3
 *   prayer points (awakened 4) every tick before her enrage. Her tentacles drain as much as they deal (10 quest, 20, 30
 *   awakened), a screech 45% (70), failed seeds 75%, a completed siphon 50%; stopping the seeds without a dark one and the
 *   Sanitas souls restore 15%. At 0% the darkness deals 5, 10, 20, 40, 80 and 160, one every 2 ticks from the second.</li>
 *   <li>Only in the realm can lost souls be damaged, the light seeds told apart from the dark ones and the floating columns'
 *   health seen. During a special she takes 70% less damage from outside the realm and 60% less from inside.</li>
 *   <li>At 0 hitpoints she pulls everyone into the realm for her enrage (no passive drain there: only tentacles hurt it);
 *   her death and leaving the fight (teleporting away) restore sanity fully.</li>
 * </ul>
 * approx: the wiki gives the realm drain without its interval (taken as every tick).
 */
public final class WhispererRules {
    private WhispererRules() {
    }

    /** The blackstone fragment: needed to fight her, activated to enter and leave the Shadow Realm. */
    public static final String FRAGMENT = "blackstone_fragment";
    /** Ketla's workbench in the Lassar Undercity: another fragment for a player who lost theirs (after Desert Treasure II). */
    public static final String WORKBENCH = "ketlas_workbench";

    public static final int SANITY_MAX = 100;
    /** Outside the realm: 1% back every this many game ticks (not when awakened). */
    public static final int RESTORE_EVERY = 4;
    /** Inside the realm before the enrage: sanity and prayer points lost every tick (awakened 4). approx: interval */
    public static final int REALM_DRAIN = 3;
    public static final int REALM_DRAIN_AWAKENED = 4;
    /** Stopping her seeds without stepping on a dark one, or killing the Sanitas souls: 15% back. */
    public static final int SEEDS_CLEAN_RESTORE = 15;
    public static final int SANITAS_RESTORE = 15;
    /** At 0% sanity the darkness hits every 2 ticks, from 5 doubling (the sixth, 160, kills). */
    public static final int INSANITY_EVERY = 2;
    public static final int INSANITY_FIRST = 5;
    /** Damage she takes during a special: 30% from outside the realm (70% less), 40% from inside (60% less). */
    public static final double SPECIAL_DAMAGE_OUTSIDE = 0.3;
    public static final double SPECIAL_DAMAGE_REALM = 0.4;
    /** Pre-enrage realm ticks Dark Memories (CA 502) allows: under 6 seconds. */
    public static final int DARK_MEMORIES_TICKS = 10;

    /** The damage of the {@code n}th (from 0) hit of insanity: 5, 10, 20, 40, 80, 160. */
    public static int insanityDamage(int n) {
        return INSANITY_FIRST << Math.min(n, 10);
    }

    /** Lost souls only take damage from players in the Shadow Realm. */
    public static boolean canHitSoul(boolean inRealm) {
        return inRealm;
    }

    /** The fragment glows (can be activated) during her special attacks, before the enrage. */
    public static boolean fragmentGlows(boolean special, boolean enraged) {
        return special && !enraged;
    }

    /** Her damage taken during a special, by where the attacker stands. */
    public static double specialDamage(boolean attackerInRealm) {
        return attackerInRealm ? SPECIAL_DAMAGE_REALM : SPECIAL_DAMAGE_OUTSIDE;
    }

    /** One player's sanity over a fight. */
    public static final class Sanity {
        private int sanity = SANITY_MAX;
        private int sinceRestore;
        private int insaneHits;
        private int insaneTicks;
        private int realmTicks;

        public int value() {
            return sanity;
        }

        public boolean insane() {
            return sanity <= 0;
        }

        /** Pre-enrage game ticks spent in the realm (Dark Memories). */
        public int realmTicks() {
            return realmTicks;
        }

        public void drain(int amount) {
            sanity = Math.max(0, sanity - Math.max(0, amount));
        }

        public void restore(int amount) {
            sanity = Math.min(SANITY_MAX, sanity + Math.max(0, amount));
            if (sanity > 0) {
                insaneHits = 0;
                insaneTicks = 0;
            }
        }

        /**
         * One game tick: {@code inRealm} before the enrage drains, outside restores (not awakened); at 0% the darkness hits.
         * Returns the insanity damage to deal this tick (0 for none).
         */
        public int tick(boolean inRealm, boolean enraged, boolean awakened) {
            if (inRealm && !enraged) {
                realmTicks++;
                drain(awakened ? REALM_DRAIN_AWAKENED : REALM_DRAIN);
                sinceRestore = 0;
            } else if (!inRealm && !awakened && sanity < SANITY_MAX) {
                if (++sinceRestore >= RESTORE_EVERY) {
                    sinceRestore = 0;
                    restore(1);
                }
            }
            if (sanity > 0) {
                return 0;
            }
            if (++insaneTicks % INSANITY_EVERY != 0) {
                return 0;
            }
            return insanityDamage(insaneHits++);
        }
    }

    /**
     * Corrupted Seeds layouts (OSRS Wiki: two layouts, each with two patterns of light seeds, every pattern with two lines of
     * symmetry). Offsets in tiles from the arena centre, one quadrant each; mirrored across both axes. approx: the tiles.
     */
    private static final int[][][] SEED_QUADRANTS = {
            {{2, 2}, {4, 1}, {1, 4}, {5, 4}},
            {{3, 0}, {0, 3}, {3, 3}, {6, 2}},
    };
    /** Which quadrant seeds are light, per layout and pattern (indices into its quadrant). */
    private static final int[][][] LIGHT = {
            {{0, 3}, {1, 2}},
            {{2}, {0, 3}},
    };

    /** One seed: its tile offset from the centre and whether it is a light one (to step on) or dark (to avoid). */
    public record Seed(int dx, int dz, boolean light) {
    }

    public static int seedLayouts() {
        return SEED_QUADRANTS.length;
    }

    /** The seeds of a layout (0-1) and pattern (0-1): each quadrant tile mirrored across both axes (axis tiles once). */
    public static List<Seed> seeds(int layout, int pattern) {
        int[][] quadrant = SEED_QUADRANTS[layout];
        int[] light = LIGHT[layout][pattern];
        List<Seed> out = new ArrayList<>();
        for (int i = 0; i < quadrant.length; i++) {
            boolean isLight = false;
            for (int l : light) {
                isLight |= l == i;
            }
            int x = quadrant[i][0];
            int z = quadrant[i][1];
            for (int sx : x == 0 ? new int[]{1} : new int[]{1, -1}) {
                for (int sz : z == 0 ? new int[]{1} : new int[]{1, -1}) {
                    out.add(new Seed(x * sx, z * sz, isLight));
                }
            }
        }
        return out;
    }
}
