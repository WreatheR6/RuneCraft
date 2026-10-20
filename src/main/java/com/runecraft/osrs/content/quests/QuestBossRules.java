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

    /** Haunted Mine: Treus Dayth's carts and cranes hit up to this every few game ticks (approx; the wiki gives no numbers). */
    public static final int TREUS_MACHINERY_TICKS = 6;
    public static final int TREUS_MACHINERY_MAX = 4;

    /** Haunted Mine (wiki: more often the closer he is to dying): one hit in this many makes Treus Dayth shift (approx). */
    public static int treusShiftOneIn(float healthLeft) {
        return healthLeft > 0.5f ? 4 : healthLeft > 0.25f ? 3 : 2;
    }

    /** Legends' Quest (wiki): Nezikchened drains Prayer to a tenth of the player's Prayer level as the fight begins. */
    public static int nezikchenedPrayerCap(int prayerLevel) {
        return prayerLevel / 10;
    }

    /** Recipe for Disaster (wiki): Karamel's ice spell hits twice for up to 7. */
    public static final int KARAMEL_SPELL_HIT = 7;
    /** Recipe for Disaster (wiki, Mod Ash): the Culinaromancer's magic hits up to 25 with a flat 1-in-3 chance. */
    public static final int CULINAROMANCER_MAGIC_MAX = 25;

    /** Whether the Culinaromancer's spell lands, from a roll of 0-2. */
    public static boolean culinaromancerHits(int roll0to2) {
        return roll0to2 == 0;
    }

    /** The Culinaromancer's minions fought in order before him (wiki, quest-helper RFDFinal). */
    public static final java.util.List<String> CULINAROMANCER_FIGHTS = java.util.List.of("agrith_na_na", "flambeed", "karamel", "dessourt",
            "gelatinnoth_mother", "culinaromancer");

    /** Dragon Slayer I (wiki): Elvarg's dragonfire hits up to 70 (10 behind an anti-dragon shield) and drains 10% of Prayer. */
    public static final int ELVARG_DRAGONFIRE_MAX = 70;
    public static final int ELVARG_PRAYER_DRAIN_PERCENT = 10;
}
