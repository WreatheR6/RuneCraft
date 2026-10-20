package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Tombs of Amascut invocations. The raid level is the sum of the selected invocations' values
 * (approximately the OSRS values). Invocations in the same {@link #group} are mutually exclusive.
 *
 * <p>Implemented effects: the raid level scales creature hitpoints, accuracy, defence, damage and loot (see
 * {@link RaidScaling}); the attempts group limits the wipes over the whole raid; time limits lower the raid level when
 * missed; the path level invocations and Walk the Path's random level-ups scale the paths; Quiet Prayers, Deadly
 * Prayers, On a Diet, Dehydration and Overly Draining change the player's rules; every boss invocation changes its
 * encounter (see the static helpers below); and the Helpful Spirit group cuts the supplies (see {@link ToaSupplies}).
 */
public enum Invocation {
    TRY_AGAIN("Try Again", 5, "attempts"),
    PERSISTENCE("Persistence", 10, "attempts"),
    SOFTCORE_RUN("Softcore Run", 15, "attempts"),
    HARDCORE_RUN("Hardcore Run", 25, "attempts"),
    WALK_FOR_IT("Walk for It", 10, "time"),
    JOG_FOR_IT("Jog for It", 15, "time"),
    RUN_FOR_IT("Run for It", 20, "time"),
    SPRINT_FOR_IT("Sprint for It", 25, "time"),
    NEED_SOME_HELP("Need Some Help?", 15, "helpful_spirit"),
    NEED_LESS_HELP("Need Less Help?", 25, "helpful_spirit"),
    NO_HELP_NEEDED("No Help Needed", 40, "helpful_spirit"),
    WALK_THE_PATH("Walk the Path", 50, null),
    PATHSEEKER("Pathseeker", 15, "path_level"),
    PATHFINDER("Pathfinder", 40, "path_level"),
    PATHMASTER("Pathmaster", 50, "path_level"),
    QUIET_PRAYERS("Quiet Prayers", 20, null),
    DEADLY_PRAYERS("Deadly Prayers", 20, null),
    ON_A_DIET("On a Diet", 15, null),
    DEHYDRATION("Dehydration", 30, null),
    OVERLY_DRAINING("Overly Draining", 15, null),
    KEEP_BACK("Keep Back", 10, null),
    STAY_VIGILANT("Stay Vigilant", 15, null),
    FEELING_SPECIAL("Feeling Special?", 20, null),
    MIND_THE_GAP("Mind the Gap!", 10, null),
    GOTTA_HAVE_FAITH("Gotta Have Faith", 10, null),
    JUNGLE_JAPES("Jungle Japes", 5, null),
    SHAKING_THINGS_UP("Shaking Things Up", 10, null),
    BOULDERDASH("Boulderdash", 10, null),
    LIVELY_LARVAE("Lively Larvae", 5, null),
    MORE_OVERLORDS("More Overlords", 15, null),
    BLOWING_MUD("Blowing Mud", 10, null),
    MEDIC("Medic!", 15, null),
    AERIAL_ASSAULT("Aerial Assault", 10, null),
    NOT_JUST_A_HEAD("Not Just a Head", 15, null),
    ARTERIAL_SPRAY("Arterial Spray", 10, null),
    BLOOD_THINNERS("Blood Thinners", 5, null),
    UPSET_STOMACH("Upset Stomach", 15, null),
    DOUBLE_TROUBLE("Double Trouble", 20, null),
    ANCIENT_HASTE("Ancient Haste", 10, null),
    ACCELERATION("Acceleration", 10, null),
    PENETRATION("Penetration", 10, null),
    OVERCLOCKED("Overclocked", 10, null),
    OVERCLOCKED_2("Overclocked 2", 10, null),
    INSANITY("Insanity", 50, null);

    /** A named invocation preset selectable at the lobby obelisk. */
    public record Preset(String name, Set<Invocation> invocations) {
        public int raidLevel() {
            return Invocation.raidLevel(invocations);
        }
    }

    private final String displayName;
    private final int level;
    private final String group;

    Invocation(String displayName, int level, String group) {
        this.displayName = displayName;
        this.level = level;
        this.group = group;
    }

    public String displayName() {
        return displayName;
    }

    public int level() {
        return level;
    }

    /** Mutual-exclusion group, or null. */
    public String group() {
        return group;
    }

    /** The invocation interface's category (OSRS wiki table): what the invocation changes. */
    public String category() {
        return switch (this) {
            case TRY_AGAIN, PERSISTENCE, SOFTCORE_RUN, HARDCORE_RUN -> "Attempts";
            case WALK_FOR_IT, JOG_FOR_IT, RUN_FOR_IT, SPRINT_FOR_IT -> "Time limit";
            case NEED_SOME_HELP, NEED_LESS_HELP, NO_HELP_NEEDED -> "Helpful Spirit";
            case WALK_THE_PATH, PATHSEEKER, PATHFINDER, PATHMASTER -> "Paths";
            case QUIET_PRAYERS, DEADLY_PRAYERS -> "Prayer";
            case ON_A_DIET, DEHYDRATION, OVERLY_DRAINING -> "Restoration";
            case LIVELY_LARVAE, MORE_OVERLORDS, BLOWING_MUD, MEDIC, AERIAL_ASSAULT -> "Kephri";
            case NOT_JUST_A_HEAD, ARTERIAL_SPRAY, BLOOD_THINNERS, UPSET_STOMACH -> "Zebak";
            case DOUBLE_TROUBLE, KEEP_BACK, STAY_VIGILANT, FEELING_SPECIAL -> "Akkha";
            case MIND_THE_GAP, GOTTA_HAVE_FAITH, JUNGLE_JAPES, SHAKING_THINGS_UP, BOULDERDASH -> "Ba-Ba";
            default -> "The Wardens";
        };
    }

    /** The invocation by enum name or display name (case and punctuation ignored), or null. */
    public static Invocation byName(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (Invocation i : values()) {
            if (i.name().toLowerCase(java.util.Locale.ROOT).replace("_", "").equals(n)
                    || i.displayName.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "").equals(n)) {
                return i;
            }
        }
        return null;
    }

    /** The raid's mode by raid level (OSRS): Entry below 150, Normal below 300, then Expert. */
    public static String modeName(int raidLevel) {
        return raidLevel < 150 ? "Entry mode" : raidLevel < 300 ? "Normal mode" : "Expert mode";
    }

    /** Invocation that must also be active (Overclocked 2 needs Overclocked, Insanity needs Overclocked 2). */
    public Invocation requires() {
        return switch (this) {
            case OVERCLOCKED_2 -> OVERCLOCKED;
            case INSANITY -> OVERCLOCKED_2;
            // OSRS: Arterial Spray and Blood Thinners need Not Just a Head.
            case ARTERIAL_SPRAY, BLOOD_THINNERS -> NOT_JUST_A_HEAD;
            default -> null;
        };
    }

    public static int raidLevel(Set<Invocation> set) {
        int sum = 0;
        for (Invocation i : set) {
            sum += i.level;
        }
        return sum;
    }

    /** Whether a set obeys the exclusion groups and requirements. */
    public static boolean valid(Set<Invocation> set) {
        Set<String> groups = new java.util.HashSet<>();
        for (Invocation i : set) {
            if (i.group != null && !groups.add(i.group)) {
                return false;
            }
            if (i.requires() != null && !set.contains(i.requires())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts the party has for the whole raid (OSRS): Try Again 10, Persistence 5, Softcore Run 3, Hardcore Run 1;
     * unlimited (-1) without an attempts invocation. A wipe during a challenge uses one.
     */
    public static int attempts(Set<Invocation> set) {
        if (set.contains(HARDCORE_RUN)) {
            return 1;
        }
        if (set.contains(SOFTCORE_RUN)) {
            return 3;
        }
        if (set.contains(PERSISTENCE)) {
            return 5;
        }
        if (set.contains(TRY_AGAIN)) {
            return 10;
        }
        return -1;
    }

    /** Wipes a party can recover from over the whole raid: {@link #attempts} minus the first, or -1 (unlimited). */
    public static int roomRetries(Set<Invocation> set) {
        int a = attempts(set);
        return a < 0 ? -1 : a - 1;
    }

    /** Time limit in minutes of the active time-limit invocation (OSRS: 40/35/30/25), or 0 for none. */
    public static int timeLimitMinutes(Set<Invocation> set) {
        if (set.contains(SPRINT_FOR_IT)) {
            return 25;
        }
        if (set.contains(RUN_FOR_IT)) {
            return 30;
        }
        if (set.contains(JOG_FOR_IT)) {
            return 35;
        }
        return set.contains(WALK_FOR_IT) ? 40 : 0;
    }

    /** Raid levels lost when the time limit is missed (OSRS: 20/30/40/50). */
    public static int timeLimitPenalty(Set<Invocation> set) {
        if (set.contains(SPRINT_FOR_IT)) {
            return 50;
        }
        if (set.contains(RUN_FOR_IT)) {
            return 40;
        }
        if (set.contains(JOG_FOR_IT)) {
            return 30;
        }
        return set.contains(WALK_FOR_IT) ? 20 : 0;
    }

    /**
     * Share of a protected hit that still lands (OSRS: Quiet Prayers makes protection prayers 10% less effective).
     */
    public static double protectionLeak(Set<Invocation> set) {
        return set.contains(QUIET_PRAYERS) ? 0.10 : 0.0;
    }

    /** Prayer points drained per point of damage taken (OSRS: Deadly Prayers drains 20% of the damage). */
    public static double prayerDrainPerDamage(Set<Invocation> set) {
        return set.contains(DEADLY_PRAYERS) ? 0.20 : 0.0;
    }

    /** Whether food may be eaten (OSRS: not with On a Diet; health-restoring potions are still allowed). */
    public static boolean foodAllowed(Set<Invocation> set) {
        return !set.contains(ON_A_DIET);
    }

    /** Whether potions that restore hitpoints may be drunk (OSRS: not with Dehydration). */
    public static boolean healingPotionsAllowed(Set<Invocation> set) {
        return !set.contains(DEHYDRATION);
    }

    /** Special attack energy every special attack costs (OSRS: Overly Draining makes them all cost 100%), or -1. */
    public static int specialCost(Set<Invocation> set) {
        return set.contains(OVERLY_DRAINING) ? 100 : -1;
    }

    /** Kephri's Mass Incubation eggs (OSRS: 2, or 4 with Lively Larvae). */
    public static int kephriEggs(Set<Invocation> set) {
        return set.contains(LIVELY_LARVAE) ? 4 : 2;
    }

    /** Blood clouds Zebak's blood magic summons (OSRS: 1 with Not Just a Head, 3 with Blood Thinners too). */
    public static int zebakBloodClouds(Set<Invocation> set) {
        if (!set.contains(NOT_JUST_A_HEAD)) {
            return 0;
        }
        return set.contains(BLOOD_THINNERS) ? 3 : 1;
    }

    /**
     * Blood clouds (OSRS wiki): each loses 2 hitpoints per tile it moves and hurts an adjacent player, healing itself
     * by the damage. // approx: 40 hitpoints, 3-6 damage a tick
     */
    public static final int BLOOD_CLOUD_HP = 40;
    public static final int BLOOD_CLOUD_COST_PER_TILE = 2;

    /** Akkha's special attack interval in attacks (OSRS: every 7th attack, every 6th with Stay Vigilant). */
    public static int akkhaSpecialEvery(Set<Invocation> set) {
        return set.contains(STAY_VIGILANT) ? 6 : 7;
    }

    // ------------------------------------------------------------------ Akkha

    /** Akkha always cycles his styles melee, ranged, magic (OSRS; style 0, 1, 2), with or without Stay Vigilant. */
    public static int akkhaNextStyle(int style) {
        return (Math.floorMod(style, 3) + 1) % 3;
    }

    /**
     * Odds (one in n) that Akkha changes style after an attack, or 0 for never (OSRS wiki): normally only after each
     * special attack; with Stay Vigilant a 1/4 roll after an auto-attack and a 1/5 roll after a special.
     */
    public static int akkhaSwitchOneIn(Set<Invocation> set, boolean afterSpecial) {
        if (!set.contains(STAY_VIGILANT)) {
            return afterSpecial ? 1 : 0;
        }
        return afterSpecial ? 5 : 4;
    }

    /** Attacks Akkha makes with a style before Stay Vigilant may switch it again (OSRS: at least two). */
    public static final int AKKHA_MIN_STYLE_ATTACKS = 2;

    /** Keep Back (OSRS): Akkha also melees whoever stands next to him, just before each ranged or magic attack. */
    public static boolean akkhaKeepBack(Set<Invocation> set) {
        return set.contains(KEEP_BACK);
    }

    /** Special attacks Akkha performs at once (OSRS: two with Double Trouble). */
    public static int akkhaSpecialsAtOnce(Set<Invocation> set) {
        return set.contains(DOUBLE_TROUBLE) ? 2 : 1;
    }

    /**
     * Game ticks players get to move between Memory Blast's safe quadrants (OSRS: two with Feeling Special?).
     * // approx: 4 without it
     */
    public static int akkhaMemoryBlastTicks(Set<Invocation> set) {
        return set.contains(FEELING_SPECIAL) ? 2 : 4;
    }

    /** Orbs Trailing Orbs leaves per player step (OSRS: Feeling Special? adds one in front of the player). */
    public static int akkhaTrailingOrbs(Set<Invocation> set) {
        return set.contains(FEELING_SPECIAL) ? 2 : 1;
    }

    /**
     * Whether Detonate also explodes diagonally (OSRS: Feeling Special? makes it trigger in more directions, groups
     * only). // approx: the diagonals
     */
    public static boolean akkhaDetonateDiagonals(Set<Invocation> set, int partySize) {
        return set.contains(FEELING_SPECIAL) && partySize > 1;
    }

    /** Memory Blast iterations (OSRS: one more for every two path levels, from four path levels at most). */
    public static int akkhaMemoryBlastRounds(int pathLevel) {
        return 2 + Math.min(4, Math.max(0, pathLevel)) / 2; // approx: base two quadrants
    }

    // ------------------------------------------------------------------ Ba-Ba

    /** Banana peels each of Ba-Ba's baboons drops on death (OSRS: two with Jungle Japes). */
    public static int babaBananaPeels(Set<Invocation> set) {
        return set.contains(JUNGLE_JAPES) ? 2 : 0;
    }

    /** Game ticks a banana peel stuns the player who steps on it (OSRS: 3 seconds). */
    public static final int BANANA_STUN_TICKS = 5;

    /** Whether Ba-Ba's knockback into the bottom pit kills (OSRS: Mind the Gap!). */
    public static boolean babaMindTheGap(Set<Invocation> set) {
        return set.contains(MIND_THE_GAP);
    }

    /**
     * Extra damage of an open sarcophagus's energy blast with Gotta Have Faith (OSRS: based on the player's remaining
     * prayer points). // approx: a fifth of them
     */
    public static int babaFaithDamage(Set<Invocation> set, int prayerPoints) {
        return set.contains(GOTTA_HAVE_FAITH) ? Math.max(0, prayerPoints) / 5 : 0;
    }

    /** Radius in tiles of Ba-Ba's slam shockwave (OSRS: 5x5, 7x7 with Shaking Things Up). */
    public static int babaSlamRadius(Set<Invocation> set) {
        return set.contains(SHAKING_THINGS_UP) ? 3 : 2;
    }

    // ------------------------------------------------------------------ Kephri

    /** Players Kephri's Dung Strike targets (OSRS: two with Blowing Mud; no effect solo). */
    public static int kephriDungTargets(Set<Invocation> set, int partySize) {
        return Math.max(1, Math.min(Math.max(1, partySize), set.contains(BLOWING_MUD) ? 2 : 1));
    }

    /**
     * The extra overlord More Overlords summons in Kephri's shield phase {@code phase} (1 or 2): a Soldier Scarab,
     * then a Spitting Scarab (OSRS), or null.
     */
    public static String kephriExtraOverlord(Set<Invocation> set, int phase) {
        if (!set.contains(MORE_OVERLORDS)) {
            return null;
        }
        return phase <= 1 ? "toa_soldier_scarab" : "toa_spitting_scarab";
    }

    /**
     * The overlords Kephri summons when her shield breaks for the {@code phase}th time (1 or 2), OSRS wiki: a Spitting
     * Scarab first, then a Soldier and an Arcane Scarab; More Overlords adds a Soldier, then a Spitting Scarab.
     */
    public static List<String> kephriOverlords(Set<Invocation> set, int phase) {
        List<String> ids = new ArrayList<>(phase <= 1 ? List.of("toa_spitting_scarab") : List.of("toa_soldier_scarab", "toa_arcane_scarab"));
        String extra = kephriExtraOverlord(set, phase);
        if (extra != null) {
            ids.add(extra);
        }
        return ids;
    }

    /** Scarab swarms in Kephri's {@code phase}th intermission (OSRS wiki: 28, then 24). */
    public static int kephriSwarms(int phase) {
        return phase <= 1 ? 28 : 24;
    }

    /**
     * Game ticks before the {@code index}th swarm (0-based) of an intermission (OSRS wiki): the first four every 4
     * ticks, the next four every 3, the next four every 2, the rest every tick.
     */
    public static int kephriSwarmDelay(int index) {
        return index < 4 ? 4 : index < 8 ? 3 : index < 12 ? 2 : 1;
    }

    /** Dark (hatching) eggs in one Mass Incubation (OSRS wiki: 2, or 3-5 with Lively Larvae). */
    public static int kephriDarkEggs(Set<Invocation> set, java.util.Random random) {
        return set.contains(LIVELY_LARVAE) ? 3 + random.nextInt(3) : 2;
    }

    /** Hits (three, +1 per extra player) or one hit over 39 that make the Arcane Scarab relocate (OSRS wiki). */
    public static int arcaneScarabRelocateHits(int partySize) {
        return 3 + Math.max(0, partySize - 1);
    }

    /** Game ticks the Arcane Scarab charges before its explosive swarm goes off. // approx */
    public static final int ARCANE_SCARAB_CHARGE_TICKS = 16;

    /** Speed multiplier of the scarab swarms Medic! sends outside the shield phases (OSRS: 50% slower). */
    public static double kephriMedicSwarmSpeed(Set<Invocation> set) {
        return set.contains(MEDIC) ? 0.5 : 1.0;
    }

    /** Radius in tiles of Kephri's fireball (OSRS: one tile, 3x3 with Aerial Assault). */
    public static int kephriFireballRadius(Set<Invocation> set) {
        return set.contains(AERIAL_ASSAULT) ? 1 : 0;
    }

    /** Kephri's fireball damage multiplier (OSRS: Aerial Assault makes it hit harder). // approx: +25% */
    public static double kephriFireballDamage(Set<Invocation> set) {
        return set.contains(AERIAL_ASSAULT) ? 1.25 : 1.0;
    }

    // ------------------------------------------------------------------ the Wardens

    /** Game ticks between the obelisk's charges in phase 1 (OSRS: faster with Ancient Haste). // approx: 12 / 9 */
    public static int wardensChargeEvery(Set<Invocation> set) {
        return set.contains(ANCIENT_HASTE) ? 9 : 12;
    }

    /** A phase 2 Warden's attack interval in game ticks (OSRS: faster with Acceleration). // approx: 5 / 4 */
    public static int wardensAttackEvery(Set<Invocation> set) {
        return set.contains(ACCELERATION) ? 4 : 5;
    }

    /** Game ticks between the obelisk's phase 2 strikes (OSRS: Acceleration charges it faster). // approx: 15 / 11 */
    public static int wardensObeliskEvery(Set<Invocation> set) {
        return set.contains(ACCELERATION) ? 11 : 15;
    }

    /**
     * Damage multiplier of the obelisk's phase 2 strikes and whether a hit disables overhead prayers (OSRS:
     * Penetration). // approx: +50%
     */
    public static double wardensObeliskDamage(Set<Invocation> set) {
        return set.contains(PENETRATION) ? 1.5 : 1.0;
    }

    public static boolean wardensPenetration(Set<Invocation> set) {
        return set.contains(PENETRATION);
    }

    /** Game ticks the phase 3 slam comes sooner (OSRS: Overclocked 1, Overclocked 2 2, Insanity 3). */
    public static int wardensSlamFaster(Set<Invocation> set) {
        if (set.contains(INSANITY)) {
            return 3;
        }
        if (set.contains(OVERCLOCKED_2)) {
            return 2;
        }
        return set.contains(OVERCLOCKED) ? 1 : 0;
    }

    private static final List<Preset> PRESETS = new ArrayList<>();

    static {
        Set<Invocation> normal = EnumSet.of(HARDCORE_RUN, WALK_FOR_IT, NEED_SOME_HELP, PATHSEEKER, QUIET_PRAYERS, ON_A_DIET,
                NOT_JUST_A_HEAD, STAY_VIGILANT, JUNGLE_JAPES, MIND_THE_GAP, LIVELY_LARVAE);
        Set<Invocation> expert = EnumSet.copyOf(normal);
        expert.remove(MIND_THE_GAP);
        expert.addAll(EnumSet.of(DOUBLE_TROUBLE, DEADLY_PRAYERS, OVERLY_DRAINING, FEELING_SPECIAL, SHAKING_THINGS_UP,
                BOULDERDASH, ANCIENT_HASTE, ACCELERATION, PENETRATION, BLOWING_MUD, MEDIC, AERIAL_ASSAULT));
        Set<Invocation> expertPlus = EnumSet.copyOf(expert);
        expertPlus.remove(PATHSEEKER);
        expertPlus.addAll(EnumSet.of(PATHFINDER, OVERCLOCKED, ARTERIAL_SPRAY, KEEP_BACK, BLOOD_THINNERS, UPSET_STOMACH,
                MORE_OVERLORDS, MIND_THE_GAP));
        Set<Invocation> insanity = EnumSet.copyOf(expertPlus);
        insanity.remove(WALK_FOR_IT);
        insanity.remove(NEED_SOME_HELP);
        insanity.addAll(EnumSet.of(SPRINT_FOR_IT, OVERCLOCKED_2, INSANITY, NO_HELP_NEEDED));
        PRESETS.add(new Preset("Entry mode", EnumSet.noneOf(Invocation.class)));
        PRESETS.add(new Preset("Normal mode", normal));
        PRESETS.add(new Preset("Expert mode", expert));
        PRESETS.add(new Preset("Expert (400)", expertPlus));
        PRESETS.add(new Preset("Insanity (500)", insanity));
    }

    /** Presets in increasing raid level: Entry (0), Normal (150), Expert (300), 400 and 500. */
    public static List<Preset> presets() {
        return PRESETS;
    }
}
