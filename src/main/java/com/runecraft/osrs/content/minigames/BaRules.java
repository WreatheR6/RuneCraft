package com.runecraft.osrs.content.minigames;

import java.util.List;
import java.util.Locale;

/**
 * Barbarian Assault's role mechanics (OSRS Wiki, Barbarian Assault and its strategy guides): the four calls and who makes
 * them, the attacker's styles, arrows and spells, the eggs and the egg launcher, the defender's bait and runner traps, the
 * healer's poisoned food and healing, the omega egg made by the whole team on wave 10, and the honour points of a wave.
 * Role indices follow {@link MinigameRules.BarbarianAssault#ROLES}: attacker, defender, collector, healer.
 */
public final class BaRules {
    public static final int ATTACKER = 0, DEFENDER = 1, COLLECTOR = 2, HEALER = 3;

    private BaRules() {
    }

    // ------------------------------------------------------------------ calls

    /** What can be called. Every call changes every 30 seconds ({@link BaWaves#CALL_TICKS}). */
    public enum Call {
        /** The attacker's style, called by the collector: controlled/bullet/wind, accurate/field/water, ... */
        STYLE(COLLECTOR, ATTACKER, List.of("controlled", "accurate", "aggressive", "defensive")),
        /** The egg colour the collector may pick up, called by the attacker. */
        EGG(ATTACKER, COLLECTOR, List.of("red", "green", "blue")),
        /** The bait that lures runners, called by the healer to the defender. */
        BAIT(HEALER, DEFENDER, List.of("tofu", "crackers", "worms")),
        /** The poisoned food that hurts Penance healers, called by the defender to the healer. */
        POISON(DEFENDER, HEALER, List.of("tofu", "worms", "meat"));

        private final int caller;
        private final int receiver;
        private final List<String> options;

        Call(int caller, int receiver, List<String> options) {
            this.caller = caller;
            this.receiver = receiver;
            this.options = options;
        }

        /** The role whose horn makes this call. */
        public int caller() {
            return caller;
        }

        /** The role told what to do. */
        public int receiver() {
            return receiver;
        }

        public List<String> options() {
            return options;
        }

        /** The index of a called word, or -1. */
        public int index(String word) {
            return options.indexOf(word == null ? "" : word.toLowerCase(Locale.ROOT));
        }

        /** How the call reads on the HUD, e.g. "Controlled/Bullet/Wind", "Red egg", "Tofu", "Poisoned meat". */
        public String label(int option) {
            if (option < 0 || option >= options.size()) {
                return "-";
            }
            switch (this) {
                case STYLE:
                    return STYLE_LABELS[option];
                case EGG:
                    return cap(options.get(option)) + " egg";
                case BAIT:
                    return cap(options.get(option));
                default:
                    return "Poisoned " + options.get(option);
            }
        }

        /** The call a role makes with its horn. */
        public static Call madeBy(int role) {
            for (Call c : values()) {
                if (c.caller == role) {
                    return c;
                }
            }
            throw new IllegalArgumentException("role " + role);
        }

        /** The call a role acts on. */
        public static Call heardBy(int role) {
            for (Call c : values()) {
                if (c.receiver == role) {
                    return c;
                }
            }
            throw new IllegalArgumentException("role " + role);
        }
    }

    private static final String[] STYLE_LABELS = {"Controlled/Bullet/Wind", "Accurate/Field/Water", "Aggressive/Blunt/Earth",
            "Defensive/Barbed/Fire"};

    static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------ attacker

    /** The four Barbarian Assault arrows, in style-call order: bullet, field, blunt, barbed (+125 ranged strength each). */
    public static final List<String> ARROWS = List.of("bullet_arrow", "field_arrow", "blunt_arrow", "barbed_arrow");
    public static final int ARROW_RANGED_STRENGTH = 125;
    /** Spell elements in style-call order: wind, water, earth, fire. */
    public static final List<String> ELEMENTS = List.of("air", "water", "earth", "fire");
    /** Damage an attacker takes for each hit with the wrong style (the Penance takes none). */
    public static final int WRONG_STYLE_RECOIL = 2;

    /** The style call a melee attack style name answers ("Controlled", "Accurate", ...), or -1. */
    public static int meleeStyle(String styleName) {
        return Call.STYLE.index(styleName);
    }

    /** The style call a Barbarian Assault arrow answers, or -1 for any other ammunition (which can't hurt the Penance). */
    public static int arrowStyle(String arrowId) {
        return ARROWS.indexOf(arrowId);
    }

    /** The style call an elemental spell ("air", "water", ...) answers, or -1. */
    public static int spellStyle(String element) {
        return ELEMENTS.indexOf(element);
    }

    /** An attacker's bonus damage on every correct-style hit, even a miss: +1 at level 1 up to +5 at level 5. */
    public static int attackerBonus(int level) {
        return MinigameRules.BarbarianAssault.attackerBonus(level);
    }

    // ------------------------------------------------------------------ collector and eggs

    /** The three eggs every Penance drops (two of each colour), in egg-call order. */
    public static final List<String> EGGS = List.of("red_egg", "green_egg", "blue_egg");
    public static final int EGGS_PER_COLOUR = 2;
    public static final String YELLOW_EGG = "yellow_egg";
    public static final String POISONED_EGG = "poisoned_egg";
    public static final String SPIKED_EGG = "spiked_poisoned_egg";
    public static final String OMEGA_EGG = "omega_egg";
    /** An egg picked up with the wrong colour called (or with no room) explodes for 5 damage and counts against the collector. */
    public static final int EXPLODE_DAMAGE = 5;
    /** Red eggs hit for 3 and splash 0-1 on Penance within 3 squares. */
    public static final int RED_DAMAGE = 3, RED_SPLASH_MAX = 1, RED_SPLASH_RANGE = 3;
    /** Green eggs hit for 1 and poison for 1 five more times. */
    public static final int GREEN_DAMAGE = 1, GREEN_POISON = 1, GREEN_POISON_HITS = 5;
    /**
     * Green egg poison hits every 3 seconds on a runner, fighter or ranger but every 18 seconds on a Penance healer (OSRS Wiki,
     * Green egg), and runs beside a poisoned food's poison without resetting, reducing or delaying it.
     */
    public static final int GREEN_POISON_INTERVAL_TICKS = 5, GREEN_POISON_HEALER_INTERVAL_TICKS = 30;
    /** Blue eggs stun for 6 seconds (the Penance can't move or be hurt meanwhile). */
    public static final int BLUE_STUN_TICKS = 10;
    /** A poisoned food's poison on a Penance healer hits every 5 ticks (3 seconds; OSRS Wiki, Penance Healer). */
    public static final int POISON_INTERVAL_TICKS = 5;
    /** The egg hopper holds at most five red, five green and five blue eggs (OSRS Wiki, Egg launcher); omega eggs have no cap. */
    public static final int HOPPER_MAX_PER_COLOUR = 5;
    /**
     * A player fires one egg at a time, once every 3-4 seconds depending on how far it flies (OSRS Wiki, Egg launcher); omega
     * eggs have no delay. // approx: 5 ticks whatever the distance
     */
    public static final int EGG_FIRE_DELAY_TICKS = 5;
    /** Collector converter success chance by collector level: none at 1, then ~20 / 40 / 60 / 80 %; a failed egg is destroyed. */
    private static final int[] CONVERT_PERCENT = {0, 0, 20, 40, 60, 80}; // approx: the main page says ~85% at level 5

    /** The egg colour index of an egg item, or -1. */
    public static int eggColour(String item) {
        return EGGS.indexOf(item);
    }

    /** How often a green egg's poison hits: every 18 seconds on a Penance healer, every 3 seconds on the others. */
    public static int greenPoisonInterval(boolean penanceHealer) {
        return penanceHealer ? GREEN_POISON_HEALER_INTERVAL_TICKS : GREEN_POISON_INTERVAL_TICKS;
    }

    /** The green egg poison hit after {@code hitsTaken} of its hits: 1 five times, then 0. */
    public static int greenPoisonHit(int hitsTaken) {
        return hitsTaken < GREEN_POISON_HITS ? GREEN_POISON : 0;
    }

    /** The converter's chance (percent) for a collector level (an egg that fails is destroyed, OSRS Wiki). */
    public static int convertPercent(int level) {
        return CONVERT_PERCENT[Math.max(1, Math.min(5, level))];
    }

    /** What the converter turns an egg into: green to red, red to green, blue to green; null for others. */
    public static String converted(String egg) {
        switch (egg) {
            case "green_egg":
                return "red_egg";
            case "red_egg":
            case "blue_egg":
                return "green_egg";
            default:
                return null;
        }
    }

    /** An omega egg hits the queen for 30-39 (OSRS Wiki, Omega egg; the Egg launcher page says 31-39). */
    public static final int OMEGA_EGG_MIN = 30, OMEGA_EGG_MAX = 39;

    /** One omega egg's hit on the queen for a uniform roll in [0, 1): 30 to 39. */
    public static int omegaEggDamage(double roll) {
        int spread = OMEGA_EGG_MAX - OMEGA_EGG_MIN + 1;
        return OMEGA_EGG_MIN + Math.min(spread - 1, (int) (roll * spread));
    }

    /**
     * The chance that the queen ({@code hitpoints}) dies to exactly {@code hits} omega eggs, each a uniform 30-39: the wiki
     * works it out as 14.9568% for 7, 85.01889% for 8 and 0.02431% for 9.
     */
    public static double queenDiesInExactly(int hits, int hitpoints) {
        int spread = OMEGA_EGG_MAX - OMEGA_EGG_MIN + 1;
        // alive[d]: chance the queen has taken d damage (d < hitpoints) and is still alive.
        double[] alive = new double[hitpoints];
        alive[0] = 1;
        double before = 1;
        for (int k = 1; k <= hits; k++) {
            double[] next = new double[hitpoints];
            for (int d = 0; d < hitpoints; d++) {
                if (alive[d] == 0) {
                    continue;
                }
                for (int v = OMEGA_EGG_MIN; v <= OMEGA_EGG_MAX; v++) {
                    if (d + v < hitpoints) {
                        next[d + v] += alive[d] / spread;
                    }
                }
            }
            double still = 0;
            for (double p : next) {
                still += p;
            }
            if (k == hits) {
                return before - still;
            }
            before = still;
            alive = next;
        }
        return 0;
    }

    /**
     * The omega egg is made by the whole team (C-H-A-D-C): the collector picks up a yellow egg and passes it to the healer, who
     * dips it in a poison crater; the attacker attaches spikes from a petrified mushroom; the defender dips it in a lava crater
     * and hands the omega egg back to the collector to load. One step of it: who does it, where, and what comes out.
     */
    public record OmegaStep(String egg, int role, String block, String result) {
    }

    public static final String POISON_CRATER = "ba_poison_crater";
    public static final String PETRIFIED_MUSHROOM = "ba_petrified_mushroom";
    public static final String LAVA_CRATER = "ba_lava_crater";

    public static final List<OmegaStep> OMEGA_STEPS = List.of(
            new OmegaStep(YELLOW_EGG, HEALER, POISON_CRATER, POISONED_EGG),
            new OmegaStep(POISONED_EGG, ATTACKER, PETRIFIED_MUSHROOM, SPIKED_EGG),
            new OmegaStep(SPIKED_EGG, DEFENDER, LAVA_CRATER, OMEGA_EGG));

    /** The step done at {@code block}, or null. */
    public static OmegaStep stepAt(String block) {
        for (OmegaStep s : OMEGA_STEPS) {
            if (s.block().equals(block)) {
                return s;
            }
        }
        return null;
    }

    /** Who an egg of the omega chain is handed to next: yellow to the healer, poisoned to the attacker, ... omega to the collector. */
    public static int handTo(String egg) {
        switch (egg) {
            case YELLOW_EGG:
                return HEALER;
            case POISONED_EGG:
                return ATTACKER;
            case SPIKED_EGG:
                return DEFENDER;
            case OMEGA_EGG:
                return COLLECTOR;
            default:
                return -1;
        }
    }

    // ------------------------------------------------------------------ defender

    /** The defender's bait, in bait-call order. */
    public static final List<String> BAIT = List.of("tofu", "crackers", "worms");
    /** A runner trap kills two runners, then needs repairing: a hammer and one log repair it to full. */
    public static final int TRAP_CHARGES = 2;
    /** Logs reappear on the log pile every 6 seconds. */
    public static final int LOG_RESPAWN_TICKS = 10;
    /** A runner's state timer: every 5 ticks it either hunts for bait or walks up to 5 squares. */
    public static final int RUNNER_CYCLE_TICKS = 5;
    public static final int RUNNER_STEP = 5;

    /**
     * Where a runner with no bait in sight walks (wiki, Penance Runner mechanics): 4/6 south towards the lure cave, 1/6 west,
     * 1/6 east; one already at the south wall heads for the cave. Returns {dx, dz} in squares (+z is south).
     */
    public static int[] runnerWalk(double roll, boolean atSouthWall, int dxToCave) {
        if (atSouthWall) {
            return new int[]{Integer.signum(dxToCave) * Math.min(RUNNER_STEP, Math.abs(dxToCave)), 0};
        }
        if (roll < 4 / 6.0) {
            return new int[]{0, RUNNER_STEP};
        }
        return roll < 5 / 6.0 ? new int[]{-RUNNER_STEP, 0} : new int[]{RUNNER_STEP, 0};
    }

    /** Squares a runner sees bait from: 4, or 5 once the defender is level 2 (higher levels add nothing). */
    public static int lureRange(int defenderLevel) {
        return MinigameRules.BarbarianAssault.defenderLure(defenderLevel);
    }

    // ------------------------------------------------------------------ healer

    /** The healer's poisoned food, in poison-call order. */
    public static final List<String> POISON_FOOD = List.of("poisoned_tofu", "poisoned_worms", "poisoned_meat");
    /** A correct poisoned food hits a Penance healer for 4 and poisons it; the poison starts at 4. */
    public static final int FOOD_DAMAGE = 4;
    /** Poison on a Penance healer drops by 1 every 5 hits (4, 3, 2, 1): one food deals 54 in all. */
    public static final int FOOD_POISON_START = 4, FOOD_POISON_HITS_PER_STEP = 5;
    public static final String HEALING_VIAL = "healing_vial";
    /** A filled healing vial holds four doses. // approx: the wiki only names the empty vial and the spring */
    public static final int VIAL_DOSES = 4;

    /** The poison hit after {@code hitsTaken} hits of one food's poison (4 x5, 3 x5, 2 x5, 1 x5), 0 once it wore off. */
    public static int foodPoisonHit(int hitsTaken) {
        return Math.max(0, FOOD_POISON_START - hitsTaken / FOOD_POISON_HITS_PER_STEP);
    }

    /** Everything one correct poisoned food deals to a Penance healer: the hit and the poison. */
    public static int foodTotalDamage() {
        int total = FOOD_DAMAGE;
        for (int i = 0; foodPoisonHit(i) > 0; i++) {
            total += foodPoisonHit(i);
        }
        return total;
    }

    /** A healer drinking from the spring: level + 10% of max hitpoints per level (Mod Ash: "1 + 10% for tier 1"). */
    public static int springSelfHeal(int healerLevel, int maxHitpoints) {
        int level = Math.max(1, Math.min(5, healerLevel));
        return level + maxHitpoints * level / 10;
    }

    /** A healing vial dose used on a teammate: 10 / 15 / 20 / 25 / 35 hitpoints by healer level. */
    public static int vialHeal(int healerLevel) {
        return MinigameRules.BarbarianAssault.healerHeal(healerLevel);
    }

    // ------------------------------------------------------------------ honour points

    /** What a team did in one wave (waves 1-9). */
    public record WaveStats(int fightersKilled, int rangersKilled, int runnersKilled, int healersKilled, int eggsCollected,
                            int eggsExploded, int hitpointsHealed, int wrongAttacks, int runnersEscaped, int wrongPoison) {
        /** Every Penance of the wave killed, nothing collected or healed, no mistakes: the wave's base points. */
        public static WaveStats clean(int wave) {
            return new WaveStats(BaWaves.total(wave, BaWaves.FIGHTER), BaWaves.total(wave, BaWaves.RANGER),
                    BaWaves.total(wave, BaWaves.RUNNER), BaWaves.total(wave, BaWaves.HEALER), 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Each role's points for a wave (OSRS Wiki, "Points for wave 1-9"), before the team/role split: attacker (rangers killed - 1)
     * + (fighters killed - 1); defender floor(1.8 x runners killed); collector floor((eggs collected - exploded, at most 60) /
     * 4.35); healer floor(0.7 x healers killed) + floor(hitpoints healed / 18) (at most 28).
     */
    public static int[] rolePoints(WaveStats s) {
        int attacker = Math.max(0, s.rangersKilled() - 1) + Math.max(0, s.fightersKilled() - 1);
        int defender = (int) Math.floor(1.8 * s.runnersKilled() + 1e-9);
        int eggs = Math.max(0, Math.min(60, s.eggsCollected() - s.eggsExploded()));
        int collector = (int) Math.floor(eggs / 4.35 + 1e-9);
        int healer = (int) Math.floor(0.7 * s.healersKilled() + 1e-9) + Math.min(28, s.hitpointsHealed() / 18);
        int[] out = new int[4];
        out[ATTACKER] = attacker;
        out[DEFENDER] = defender;
        out[COLLECTOR] = collector;
        out[HEALER] = healer;
        return out;
    }

    /**
     * Each role's penalties for a wave: wrong-style hits (one free per wave, then 1 each, at most 10), escaped runners (3 each,
     * at most 10) and wrong poisoned food (1 per 4, at most 10). The collector has none of its own.
     */
    public static int[] rolePenalties(WaveStats s) {
        int[] out = new int[4];
        out[ATTACKER] = Math.min(10, Math.max(0, s.wrongAttacks() - 1));
        out[DEFENDER] = Math.min(10, 3 * s.runnersEscaped());
        out[HEALER] = Math.min(10, s.wrongPoison() / 4);
        return out;
    }

    /**
     * Honour points for one wave (1-9) in a role: everyone gets the whole team's points minus the team's penalties (never
     * below 0), plus their own role's points and minus their own role's penalties again (so a collector earns 2 for every
     * 4.35 eggs and the others 1, and an escaped runner costs the defender 6 and the others 3); the total is never below 0.
     * A completed hard Kandarin diary adds 10% (rounded down). Wave 10 pays {@link #wave10}.
     */
    public static int wavePoints(int role, int wave, WaveStats s, boolean kandarinHard) {
        if (wave >= BaWaves.WAVES) {
            return wave10(role, role, kandarinHard);
        }
        int[] pos = rolePoints(s);
        int[] pen = rolePenalties(s);
        int team = Math.max(0, pos[0] + pos[1] + pos[2] + pos[3] - pen[0] - pen[1] - pen[2] - pen[3]);
        int total = Math.max(0, team + pos[role] - pen[role]);
        return diary(total, kandarinHard);
    }

    /** Wave 10 (the queen): 80 points in the role played and 5 in each of the other three, no bonuses or penalties. */
    public static int wave10(int played, int role, boolean kandarinHard) {
        return diary(played == role ? MinigameRules.BarbarianAssault.WAVE_10_ROLE : MinigameRules.BarbarianAssault.WAVE_10_OTHER,
                kandarinHard);
    }

    /** The hard Kandarin diary's +10%, rounded down. // approx: applied to wave 10's fixed points too */
    public static int diary(int points, boolean kandarinHard) {
        return kandarinHard ? points * 11 / 10 : points;
    }

    /**
     * Penance gear used on Commander Connad is taken back for 80% of its honour point price in each role (375 -> 300,
     * 275 -> 220, 150 -> 120, 100 -> 80); queen kills spent aren't refunded.
     */
    public static int refund(int honourCost) {
        return honourCost * 4 / 5;
    }

    /** A full round's clean points in a role (waves 1-10): attacker 370, defender 357, collector 278, healer 305 (wiki). */
    public static int fullRound(int role, boolean kandarinHard) {
        int total = 0;
        for (int w = 1; w <= BaWaves.WAVES; w++) {
            total += wavePoints(role, w, WaveStats.clean(w), kandarinHard);
        }
        return total;
    }
}
