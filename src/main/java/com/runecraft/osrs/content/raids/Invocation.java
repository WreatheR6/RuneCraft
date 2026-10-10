package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Tombs of Amascut invocations. The raid level is the sum of the selected invocations' values
 * (approximately the OSRS values). Invocations in the same {@link #group} are mutually exclusive.
 *
 * <p>Implemented effects: every 1 raid level scales boss hitpoints, damage and loot (see
 * {@link RaidScaling}); the attempts group limits how many times a wiped party may retry a room.
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

    /** Invocation that must also be active (Overclocked 2 needs Overclocked, Insanity needs Overclocked 2). */
    public Invocation requires() {
        return switch (this) {
            case OVERCLOCKED_2 -> OVERCLOCKED;
            case INSANITY -> OVERCLOCKED_2;
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

    /** Retries a wiped party gets per room: unlimited (-1) without an attempts invocation. */
    public static int roomRetries(Set<Invocation> set) {
        if (set.contains(HARDCORE_RUN)) {
            return 0;
        }
        if (set.contains(SOFTCORE_RUN)) {
            return 1;
        }
        if (set.contains(PERSISTENCE)) {
            return 2;
        }
        if (set.contains(TRY_AGAIN)) {
            return 3;
        }
        return -1;
    }

    private static final List<Preset> PRESETS = new ArrayList<>();

    static {
        Set<Invocation> normal = EnumSet.of(HARDCORE_RUN, WALK_FOR_IT, NEED_SOME_HELP, PATHSEEKER, QUIET_PRAYERS, ON_A_DIET,
                KEEP_BACK, STAY_VIGILANT, JUNGLE_JAPES, BLOOD_THINNERS, MIND_THE_GAP, LIVELY_LARVAE);
        Set<Invocation> expert = EnumSet.copyOf(normal);
        expert.remove(MIND_THE_GAP);
        expert.addAll(EnumSet.of(DOUBLE_TROUBLE, DEADLY_PRAYERS, OVERLY_DRAINING, FEELING_SPECIAL, SHAKING_THINGS_UP,
                BOULDERDASH, ANCIENT_HASTE, ACCELERATION, PENETRATION, BLOWING_MUD, MEDIC, AERIAL_ASSAULT));
        Set<Invocation> expertPlus = EnumSet.copyOf(expert);
        expertPlus.remove(PATHSEEKER);
        expertPlus.addAll(EnumSet.of(PATHFINDER, OVERCLOCKED, ARTERIAL_SPRAY, NOT_JUST_A_HEAD, UPSET_STOMACH, MORE_OVERLORDS,
                MIND_THE_GAP));
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
