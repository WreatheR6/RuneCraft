package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.content.systems.ClueScrolls.Tier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Per-monster clue scroll drop rates (1 in n; 0 = that tier never drops) from the OSRS wiki drop tables
 * (osrsreboxed-db dump, Oct 2024), keyed by the monster's lower-case display name. Only monsters whose
 * wiki tables list at least one clue tier are in the table; everything else falls back to
 * {@link ClueScrolls#dropOneIn(Tier, int)}'s combat-level formula. Master clues never drop from monsters.
 */
public final class ClueDrops {
    /** name -> {beginner, easy, medium, hard, elite}. */
    private static final Map<String, int[]> TABLE = new HashMap<>();

    private static void e(String name, int beginner, int easy, int medium, int hard, int elite) {
        TABLE.put(name, new int[]{beginner, easy, medium, hard, elite});
    }

    static {
        e("aberrant spectre", 0, 0, 0, 128, 0);
        e("abyssal demon", 0, 0, 0, 128, 1200);
        e("abyssal guardian", 0, 0, 256, 0, 0);
        e("abyssal leech", 0, 0, 256, 0, 0);
        e("abyssal walker", 0, 0, 256, 0, 0);
        e("ankou", 0, 0, 0, 512, 0);
        e("balfrug kreeyath", 0, 0, 0, 128, 0);
        e("banshee", 0, 128, 0, 0, 0);
        e("barbarian", 75, 128, 0, 0, 0);
        e("black demon", 0, 0, 0, 128, 0);
        e("black dragon", 0, 0, 0, 128, 500);
        e("bloodveld", 0, 0, 0, 256, 0);
        e("blue dragon", 0, 0, 0, 128, 0);
        e("bree", 0, 0, 0, 128, 0);
        e("bronze dragon", 0, 0, 0, 128, 0);
        e("brutal green dragon", 0, 0, 0, 128, 0);
        e("catablepon", 0, 0, 101, 0, 0);
        e("cave bug", 0, 128, 0, 0, 0);
        e("cave horror", 0, 0, 0, 128, 0);
        e("cave kraken", 0, 0, 0, 100, 1200);
        e("cave slime", 0, 128, 0, 0, 0);
        e("chaos elemental", 0, 0, 0, 0, 200);
        e("chicken", 300, 0, 0, 0, 0);
        e("cockatrice", 0, 0, 128, 0, 0);
        e("commander zilyana", 0, 0, 0, 0, 250);
        e("corporeal beast", 0, 0, 0, 0, 200);
        e("cow", 128, 0, 0, 0, 0);
        e("cyclops", 0, 0, 0, 256, 0);
        e("dagannoth", 0, 0, 128, 0, 0);
        e("dagannoth prime", 0, 0, 0, 42, 750);
        e("dagannoth rex", 0, 0, 0, 42, 750);
        e("dagannoth supreme", 0, 0, 0, 42, 750);
        e("dark wizard", 35, 0, 0, 0, 0);
        e("dwarf", 100, 0, 0, 0, 0);
        e("farmer", 90, 128, 0, 0, 0);
        e("gargoyle", 0, 0, 0, 128, 0);
        e("ghost", 90, 0, 0, 0, 0);
        e("giant rat", 128, 0, 0, 0, 0);
        e("giant sea snake", 0, 0, 128, 0, 0);
        e("giant spider", 128, 0, 0, 0, 0);
        e("goblin", 64, 128, 0, 0, 0);
        e("gorak", 0, 0, 0, 128, 0);
        e("greater demon", 0, 0, 0, 128, 0);
        e("green dragon", 0, 0, 0, 128, 0);
        e("grizzly bear", 90, 0, 0, 0, 0);
        e("growler", 0, 0, 0, 128, 0);
        e("guard", 0, 0, 128, 0, 0);
        e("harpie bug swarm", 0, 0, 128, 0, 0);
        e("hellhound", 0, 0, 0, 64, 0);
        e("hill giant", 50, 0, 0, 0, 0);
        e("hobgoblin", 70, 0, 0, 0, 0);
        e("ice spider", 128, 0, 0, 0, 0);
        e("ice warrior", 0, 0, 128, 0, 0);
        e("icefiend", 0, 128, 0, 0, 0);
        e("iron dragon", 0, 0, 0, 128, 0);
        e("jelly", 0, 0, 0, 128, 0);
        e("jogre", 0, 0, 129, 0, 0);
        e("jungle horror", 0, 0, 128, 0, 0);
        e("kalphite queen", 0, 0, 0, 0, 100);
        e("king black dragon", 0, 0, 0, 0, 450);
        e("king scorpion", 100, 0, 0, 0, 0);
        e("knight of saradomin", 0, 0, 0, 128, 0);
        e("kraken", 0, 0, 0, 0, 500);
        e("kree'arra", 0, 0, 0, 0, 250);
        e("kurask", 0, 0, 0, 128, 0);
        e("man", 90, 128, 0, 0, 0);
        e("minotaur", 60, 101, 0, 0, 0);
        e("mithril dragon", 0, 0, 0, 0, 350);
        e("molanisk", 0, 128, 0, 0, 0);
        e("moss giant", 45, 0, 0, 0, 0);
        e("mugger", 80, 128, 0, 0, 0);
        e("nechryael", 0, 0, 0, 128, 0);
        e("ork", 0, 128, 0, 128, 0);
        e("paladin", 0, 0, 128, 0, 0);
        e("poison spider", 128, 0, 0, 0, 0);
        e("pyrefiend", 0, 0, 128, 0, 0);
        e("red dragon", 0, 0, 0, 128, 0);
        e("rock crab", 0, 128, 0, 0, 0);
        e("saradomin priest", 0, 0, 0, 128, 0);
        e("scorpion", 100, 0, 0, 0, 0);
        e("sea snake young", 0, 0, 128, 0, 0);
        e("sergeant grimspike", 0, 0, 0, 128, 0);
        e("sergeant steelwill", 0, 0, 0, 128, 0);
        e("sergeant strongstack", 0, 0, 0, 128, 0);
        e("skeletal wyvern", 0, 0, 0, 0, 350);
        e("skeleton", 100, 0, 0, 0, 0);
        e("smoke devil", 0, 0, 0, 128, 750);
        e("spider", 128, 0, 0, 0, 0);
        e("spiritual mage", 0, 0, 0, 128, 0);
        e("spiritual ranger", 0, 0, 0, 128, 0);
        e("spiritual warrior", 0, 0, 0, 128, 0);
        e("starlight", 0, 0, 0, 128, 0);
        e("steel dragon", 0, 0, 0, 64, 500);
        e("suqah", 0, 0, 0, 129, 0);
        e("thermonuclear smoke devil", 0, 0, 0, 96, 500);
        e("tstanon karlak", 0, 0, 0, 128, 0);
        e("turoth", 0, 0, 0, 128, 0);
        e("wall beast", 0, 0, 128, 0, 0);
        e("waterfiend", 0, 0, 0, 128, 0);
        e("werewolf", 0, 128, 512, 0, 0);
        e("wolf", 128, 0, 0, 0, 0);
        e("woman", 90, 128, 0, 0, 0);
        e("zakl'n gritch", 0, 0, 0, 128, 0);
        e("zulrah", 0, 0, 0, 0, 75);

        // Monsters whose dumped wiki tables lost their tertiary clue rows (OSRS knowledge).
        e("fire giant", 0, 0, 0, 128, 0); // approx
        e("lesser demon", 0, 0, 0, 128, 0); // approx
        e("ice giant", 0, 0, 128, 0, 0); // approx
        e("dust devil", 0, 0, 0, 128, 0); // approx
    }

    private ClueDrops() {
    }

    /** Whether the wiki table knows this monster's clue drops. */
    public static boolean known(String monsterName) {
        return monsterName != null && TABLE.containsKey(monsterName.toLowerCase(Locale.ROOT));
    }

    /** The wiki clue rate (1 in n, 0 = none) for a monster in the table; -1 if the monster isn't in it. */
    public static int oneIn(Tier tier, String monsterName) {
        if (monsterName == null) {
            return -1;
        }
        int[] r = TABLE.get(monsterName.toLowerCase(Locale.ROOT));
        if (r == null) {
            return -1;
        }
        return tier == Tier.MASTER ? 0 : r[tier.ordinal()];
    }

    /** Number of monsters in the table (for tests). */
    public static int size() {
        return TABLE.size();
    }
}
