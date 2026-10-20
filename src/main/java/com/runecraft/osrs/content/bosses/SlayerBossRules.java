package com.runecraft.osrs.content.bosses;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Who may fight the Slayer bosses (OSRS Wiki, each boss's page and "Slayer task"): every one of them can only be fought
 * while the player is assigned a Slayer task it counts towards: its creature category, or the boss itself as a boss task
 * ("Like a Boss"). The Slayer level alone is not enough. The Grotesque Guardians' roof also has to be unlocked once with a
 * brittle key, which only gargoyles drop while they are the player's task (1/150, OSRS Wiki "Grotesque
 * Guardians/Strategies").
 */
public final class SlayerBossRules {
    /** The task categories ({@code SlayerCategories} ids, then the boss task id) that let a player fight a boss. */
    public record Access(String bossId, String bossName, List<String> tasks, String refusal) {
        /** Whether a player whose current task is {@code task} (null: none) may fight the boss. */
        public boolean allows(String task) {
            return task != null && tasks.contains(task);
        }
    }

    /** The key that unlocks the Slayer Tower's roof for good (OSRS Wiki "Brittle key"). */
    public static final String BRITTLE_KEY = "brittle_key";
    /** Gargoyles drop the brittle key 1/150 of the time while they are the player's task, until the roof is unlocked. */
    public static final int BRITTLE_KEY_RATE = 150;
    /** The task categories whose kills can drop the brittle key (gargoyles and their superior, the marble gargoyle). */
    public static final String BRITTLE_KEY_TASK = "gargoyles";

    private static final Map<String, Access> ALL = new LinkedHashMap<>();

    private static void add(String bossId, String bossName, String refusal, String... tasks) {
        ALL.put(bossId, new Access(bossId, bossName, List.of(tasks), refusal));
    }

    static {
        // OSRS Wiki: "it can only be attacked if the player has it, or cave krakens as a Slayer task".
        add("kraken", "Kraken", "The Kraken only stirs for those hunting cave kraken for a Slayer master.",
                "cave_kraken", "boss_kraken");
        // "can only be attacked on a smoke devil task" (the Western Provinces diary kill is not modelled).
        add("thermonuclear_smoke_devil", "Thermonuclear smoke devil",
                "You can only fight the thermonuclear smoke devil while you are assigned smoke devils.",
                "smoke_devils", "boss_thermonuclear_smoke_devil");
        // "A Slayer level of 91 and a slayer task of Cerberus or hellhounds is required".
        add("cerberus", "Cerberus", "Only those assigned to kill hellhounds may enter Cerberus' lair.",
                "hellhounds", "boss_cerberus");
        // "can only be killed when assigned Abyssal demons or the Sire itself".
        add("abyssal_sire", "Abyssal Sire", "The Abyssal Sire ignores you: you need to be assigned abyssal demons.",
                "abyssal_demons", "boss_abyssal_sire");
        // "can only be killed while on a hydra Slayer task" (Konar assigns hydras and the boss task).
        add("alchemical_hydra", "Alchemical Hydra", "Orrvor quo Maten: Only those Konar sends after hydras may enter the chamber.",
                "hydras", "boss_alchemical_hydra");
        // "can only be killed while the player is assigned gargoyles or the Guardians themselves as a Slayer task".
        add("dusk", "Grotesque Guardians", "You need to be assigned gargoyles to go up onto the roof.",
                "gargoyles", "boss_grotesque_guardians");
        // "he may only be killed while on a spider or araxyte Slayer task" (or the Araxxor boss task).
        add("araxxor", "Araxxor", "You need to be assigned araxytes or spiders to disturb Araxxor.",
                "araxytes", "spiders", "boss_araxxor");
    }

    private SlayerBossRules() {
    }

    /** The access rule of a Slayer boss (its main form's id), or null for a boss with none. */
    public static Access of(String bossId) {
        return bossId == null ? null : ALL.get(bossId);
    }

    /** Every Slayer boss's rule, keyed by boss id. */
    public static Map<String, Access> all() {
        return java.util.Collections.unmodifiableMap(ALL);
    }

    /** Whether a player whose current task is {@code task} may fight {@code bossId} (true for bosses with no rule). */
    public static boolean allows(String bossId, String task) {
        Access a = of(bossId);
        return a == null || a.allows(task);
    }
}
