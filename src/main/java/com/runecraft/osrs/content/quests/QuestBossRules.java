package com.runecraft.osrs.content.quests;

import java.util.Map;

/**
 * The rules of the master quests' boss fights that their scripts (the quests module's QuestBossScripts)
 * apply, kept here so they are unit-tested (OSRS Wiki and the DPS calculator's monster data).
 */
public final class QuestBossRules {
    private QuestBossRules() {
    }

    /**
     * Bosses only magic of one element hurts (other spells do nothing): Fareed (water), Kamil (fire). Other styles follow
     * each boss's own rules.
     */
    public static final Map<String, String> SPELL_ELEMENT = Map.of("fareed", "water", "kamil", "fire");

    /** Whether a spell of {@code element} (null: not an elemental spell) can hurt the boss. */
    public static boolean spellHurts(String bossId, String element) {
        String only = SPELL_ELEMENT.get(bossId);
        return only == null || only.equals(element);
    }

    /** Fareed's melee max hit, and against a player wearing ice gloves. */
    public static final int FAREED_MAX = 40;
    public static final int FAREED_MAX_ICE_GLOVES = 37;

    /** Damis's two forms (DPS calculator): level, Hitpoints, Attack, Strength, Defence, attack speed, max hit. */
    public static final int[] DAMIS_FIRST = {103, 90, 90, 90, 90, 4, 22};
    public static final int[] DAMIS_SECOND = {174, 200, 160, 100, 160, 3, 28};

    /** Times Dessous teleports to a player hitting him from afar before he grows bored and returns to his grave. */
    public static final int DESSOUS_TELEPORTS = 3;
    /** Dessous's bat swarm: half Magic, half Ranged, each up to 5. */
    public static final int DESSOUS_BATS_MAX = 10;
    /** Dessous's 3-tick melee. */
    public static final int DESSOUS_MELEE_MAX = 19;

    /**
     * Monkey Madness I: the 10th squad's gnomes hit the Jungle demon for up to {@link #GNOME_MAX_HIT} every
     * {@link #GNOME_HIT_TICKS} game ticks (approx), and it regenerates to 20% of its hitpoints (wiki) when anyone but the
     * player would deal the final blow.
     */
    public static final int GNOME_HIT_TICKS = 4;
    public static final int GNOME_MAX_HIT = 4;
    public static final int JUNGLE_DEMON_REGEN_PERCENT = 20;
}
