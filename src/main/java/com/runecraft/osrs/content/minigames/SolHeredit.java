package com.runecraft.osrs.content.minigames;

import java.util.List;
import java.util.Random;

/**
 * Sol Heredit's fight rules (OSRS Wiki: "Sol Heredit" and "Fortis Colosseum/Strategies#Sol Heredit", July 2026 dump).
 *
 * <p>His regular attacks are dodgeable typeless AoEs (up to 44, prayer doesn't help) centred on him: two spear stabs and two
 * shield slams. Two of the same style in a row use the second pattern; a spear after a shield (or the reverse), or anything
 * after a special attack, uses the first. He opens with Spear 1, always uses a spear first after a phase transition, and must
 * be next to the player to start one; he can't move for 4 ticks afterwards. Spears are 7-tick attacks and shields 6-tick,
 * both one tick faster below 75%. Specials: the triple parry (below 90%: three spear hits 3 ticks apart, the third 4 ticks
 * after the second from 50%, each blocked only by Protect from Melee switched on just before it lands; here: on as it lands,
 * see {@link #parried}) and the grapple
 * (below 75%: he calls out one of five gear slots, which must be defended within 4 ticks; defending on the last tick is a
 * perfect parry that makes the player's next attack within 5 ticks a max hit). Phase transitions at 90/75/50/25/10% drop
 * six beams of light around the player that leave molten sand, and add a light-beam crystal (up to four) that circles the
 * arena and fires a laser at the player's tile; at 10% he enrages: more sand every 3 ticks, faster lasers.
 *
 * <p>Arena tiles here are blocks: the Colosseum arena is built one block per OSRS tile (the pillars stand 16 tiles apart).
 */
public final class SolHeredit {
    private SolHeredit() {
    }

    /** His attacks (ordinals are not saved). */
    public enum Attack {
        SPEAR_1, SPEAR_2, SHIELD_1, SHIELD_2, TRIPLE, GRAPPLE;

        public boolean spear() {
            return this == SPEAR_1 || this == SPEAR_2;
        }

        public boolean shield() {
            return this == SHIELD_1 || this == SHIELD_2;
        }

        public boolean special() {
            return this == TRIPLE || this == GRAPPLE;
        }
    }

    /** Hitpoint percentages where a new phase starts (phase 1 is the opening, phase 6 the enrage). */
    public static final List<Integer> PHASES = List.of(90, 75, 50, 25, 10);
    public static final String OPENING_LINE = "Let's start by testing your footwork.";
    /** What he says entering phases 2 to 6. */
    public static final List<String> PHASE_LINES = List.of("Not bad. Let's try something else...",
            "Impressive. Let's see how you handle this...", "You can't win!", "Ralos guides my hand!", "LET'S END THIS!");

    public static final int AOE_MAX_HIT = 44;
    public static final int GRAPPLE_MAX_HIT = 44;
    /** The light beam's sphere ("up to 75 damage"). */
    public static final int LASER_MAX_HIT = 75;
    /** Ticks he stays rooted from the tick he starts a regular attack. */
    public static final int AOE_ROOT_TICKS = 4;
    /**
     * Ticks from a regular attack's wind-up to the dust landing. // approx: the wiki gives the cues, not the delay (2 here
     * before). // playability: 3 (1.8 s) to read the safe tiles drawn at the wind-up and step onto one
     */
    public static final int AOE_LAND_TICKS = 3;
    /**
     * Blocks of leniency at the edges of the AoE patterns: a player whose position is within this of a safe tile counts as
     * standing on it. // playability: OSRS is tile-exact; a Minecraft player stands anywhere on a block and moves smoothly
     */
    public static final double AOE_EDGE_LENIENCY = 0.3;
    /**
     * The triple parry's spear glint and "1/3" call come this many game ticks (24 Minecraft ticks) before each hit.
     * // playability: OSRS gives the spear animation only
     */
    public static final int TRIPLE_CUE_TICKS = 2;
    /** Chebyshev blocks from his centre within which he may start a regular attack (next to his 5x5 body). */
    public static final int MELEE_REACH = 3;

    /** Molten sand: six beams per phase transition in a 9x9 around the player (one on the player's tile), sand 2 ticks later. */
    public static final int SAND_PER_PHASE = 6;
    public static final int SAND_RADIUS = 4;
    public static final int SAND_DELAY_TICKS = 2;
    public static final int ENRAGE_INITIAL_SAND = 5;
    public static final int ENRAGE_SAND_TICKS = 3;
    /** "~6-8 damage per tick" while standing on Sol's molten sand. */
    public static final int SAND_MIN_DAMAGE = 6;
    public static final int SAND_MAX_DAMAGE = 8;

    /** Light beams: one more crystal each phase up to four; 3 ticks to step off the targeted tile (2 when enraged). */
    public static final int MAX_BEAMS = 4;
    public static final int LASER_WARNING_TICKS = 3;
    public static final int ENRAGE_LASER_WARNING_TICKS = 2;
    /** Ticks between a crystal's shots: about 7 seconds when enraged; slower before. // approx */
    public static final int LASER_INTERVAL_TICKS = 20;
    public static final int ENRAGE_LASER_INTERVAL_TICKS = 12;

    /** Grapple: 4 ticks to defend the called slot; on the last tick it is a perfect parry (next attack within 5 ticks maxes). */
    public static final int GRAPPLE_WINDOW_TICKS = 4;
    public static final int PERFECT_PARRY_TICKS = 5;
    /** Totemic on wave 12: totems from 50%, healing him 75 every 4.2 seconds (7 ticks), one more every two minutes. */
    public static final int TOTEM_HEAL = 75;
    public static final int TOTEM_HEAL_TICKS = 7;
    public static final int TOTEM_EVERY_TICKS = 200;

    /** Grapple targets: the slot to defend and his shout. */
    public enum Grapple {
        BODY("body", "I'LL CRUSH YOUR BODY!"),
        BACK("back", "I'LL BREAK YOUR BACK!"),
        HANDS("hands", "I'LL TWIST YOUR HANDS OFF!"),
        LEGS("legs", "I'LL BREAK YOUR LEGS!"),
        FEET("feet", "I'LL CUT YOUR FEET OFF!");

        public final String slot;
        public final String shout;

        Grapple(String slot, String shout) {
            this.slot = slot;
            this.shout = shout;
        }

        public static Grapple bySlot(String slot) {
            for (Grapple g : values()) {
                if (g.slot.equalsIgnoreCase(slot)) {
                    return g;
                }
            }
            return null;
        }
    }

    /**
     * The armour slot (Minecraft {@code EquipmentSlot} name) an undefended grapple tears into the inventory: body, legs and
     * feet; null for the back (cape) and hands (gloves), which RuneCraft wears as accessories.
     */
    public static String armourSlot(Grapple g) {
        return switch (g) {
            case BODY -> "CHEST";
            case LEGS -> "LEGS";
            case FEET -> "FEET";
            case BACK, HANDS -> null;
        };
    }

    /** The phase (1 = opening ... 6 = enrage) at a hitpoint percentage. */
    public static int phase(double percent) {
        int phase = 1;
        for (int threshold : PHASES) {
            if (percent <= threshold) {
                phase++;
            }
        }
        return phase;
    }

    /** Light-beam crystals in a phase: none in phase 1, then one more per phase up to four. */
    public static int beams(int phase) {
        return Math.max(0, Math.min(MAX_BEAMS, phase - 1));
    }

    public static boolean tripleUnlocked(int phase) {
        return phase >= 2;
    }

    public static boolean grappleUnlocked(int phase) {
        return phase >= 3;
    }

    /** The regular attack after {@code previous} when the next one is a spear (or a shield). */
    public static Attack regular(Attack previous, boolean spear) {
        if (spear) {
            return previous == Attack.SPEAR_1 ? Attack.SPEAR_2 : Attack.SPEAR_1;
        }
        return previous == Attack.SHIELD_1 ? Attack.SHIELD_2 : Attack.SHIELD_1;
    }

    /**
     * His next attack. {@code afterPhase}: a phase transition just happened (always a spear, keeping the alternation). The
     * specials replace a regular attack now and then once unlocked, never twice in a row. // approx: the wiki doesn't give
     * their frequency (here 1 in 5 each) or the spear/shield split (even)
     */
    public static Attack next(Attack previous, int phase, boolean afterPhase, Random random) {
        if (previous == null) {
            return Attack.SPEAR_1;
        }
        if (afterPhase) {
            return regular(previous.special() ? null : previous, true);
        }
        if (!previous.special()) {
            int roll = random.nextInt(5);
            if (roll == 0 && tripleUnlocked(phase)) {
                return Attack.TRIPLE;
            }
            if (roll == 1 && grappleUnlocked(phase)) {
                return Attack.GRAPPLE;
            }
        }
        return regular(previous.special() ? null : previous, random.nextBoolean());
    }

    /** Game ticks until his next attack after this one (spear 7, shield 6, one faster below 75%). // approx for specials */
    public static int attackDelay(Attack a, int phase) {
        int faster = phase >= 3 ? 1 : 0;
        return switch (a) {
            case SPEAR_1, SPEAR_2 -> 7 - faster;
            case SHIELD_1, SHIELD_2 -> 6 - faster;
            case TRIPLE -> 3 + tripleGap(1, phase) + tripleGap(2, phase) + 3;
            case GRAPPLE -> GRAPPLE_WINDOW_TICKS + 3;
        };
    }

    /** Ticks from the triple attack's start (hit 0) or the previous hit to hit {@code hit} (0-2). */
    public static int tripleGap(int hit, int phase) {
        return hit == 2 && phase >= 4 ? 4 : 3;
    }

    /** Damage of an unblocked triple-attack hit (0-2): 15/25/35, or 15/30/45 from 50%. */
    public static int tripleDamage(int hit, int phase) {
        int[] d = phase >= 4 ? new int[]{15, 30, 45} : new int[]{15, 25, 35};
        return d[Math.max(0, Math.min(2, hit))];
    }

    /**
     * Whether Protect from Melee blocks a triple-attack hit landing on {@code landTick}: it is on as the hit lands.
     * // playability: in OSRS it must be switched on no earlier than the tick before each hit (a prayer left on early is
     * knocked off and the hit lands), three hits three ticks apart: a one-tick flick no real-time player can make. Kept: the
     * 15/25/35 (15/30/45) hits and their spacing; each hit is now called out 24 Minecraft ticks ahead
     * ({@link #TRIPLE_CUE_TICKS}). {@code activeSince} is no longer read.
     */
    public static boolean parried(boolean protectMelee, long activeSince, long landTick) {
        return protectMelee;
    }

    /**
     * Whether a regular attack hits a player standing at ({@code px}, {@code pz}) blocks from his centre block's north-west
     * corner (so the tile is the floor of each), aimed in the cardinal direction ({@code fx}, {@code fz}): only when every
     * tile within {@link #AOE_EDGE_LENIENCY} of the player is a hit tile ({@link #hits}). // playability: OSRS tests the one
     * tile the player stands on
     */
    public static boolean hitsLenient(Attack a, double px, double pz, int fx, int fz) {
        double e = AOE_EDGE_LENIENCY;
        for (double ox : new double[]{-e, e}) {
            for (double oz : new double[]{-e, e}) {
                if (!hits(a, (int) Math.floor(px + ox), (int) Math.floor(pz + oz), fx, fz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Whether a grapple defended {@code ticks} game ticks after he called it is perfect: the grapple lands
     * {@link #GRAPPLE_WINDOW_TICKS} ticks after the call, so the last tick it can still be defended on is the one before.
     */
    public static boolean perfectParry(int ticks) {
        return ticks >= GRAPPLE_WINDOW_TICKS - 1;
    }

    /**
     * Whether a regular attack hits the tile {@code (dx, dz)} blocks from his centre block, aimed at a player in the cardinal
     * direction {@code (fx, fz)}:
     * <ul>
     *   <li>Spear 1: the 5x5 under him plus the row in front (5x6), and two 4-long lines towards the player one tile either
     *   side of his centre line (safe: one tile back from his centre or corner tiles);</li>
     *   <li>Spear 2: the 5x5 under him and three 4-long lines from his front edge, down the centre and both edges (safe: the
     *   off-centre tiles next to him);</li>
     *   <li>Shield 1: everything within 7 tiles except the ring 4 tiles out (the 9x9's edge: one tile back from melee range);</li>
     *   <li>Shield 2: the same with the safe ring 5 tiles out (two tiles back).</li>
     * </ul>
     */
    public static boolean hits(Attack a, int dx, int dz, int fx, int fz) {
        int cheb = Math.max(Math.abs(dx), Math.abs(dz));
        int forward = dx * fx + dz * fz;
        int lateral = Math.abs(dx * fz - dz * fx);
        return switch (a) {
            case SPEAR_1 -> cheb <= 2 || forward == 3 && lateral <= 2 || forward >= 4 && forward <= 7 && lateral == 1;
            case SPEAR_2 -> cheb <= 2 || forward >= 3 && forward <= 6 && (lateral == 0 || lateral == 2);
            case SHIELD_1 -> cheb <= 7 && cheb != 4;
            case SHIELD_2 -> cheb <= 7 && cheb != 5;
            default -> false;
        };
    }

    /** The cardinal direction {east, south} (+x, +z) from his centre towards an offset, by its larger axis. */
    public static int[] facing(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return new int[]{dx >= 0 ? 1 : -1, 0};
        }
        return new int[]{0, dz >= 0 ? 1 : -1};
    }
}
