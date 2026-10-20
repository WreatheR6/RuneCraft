package com.runecraft.osrs.content.systems;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sherlock's skill challenges (elite and master clues; OSRS Wiki "Sherlock/Elite" and "Sherlock/Master"), mapped onto
 * the events this world fires: an item obtained (gathering, thieving, runecrafting), an item made at a menu, a monster
 * killed, Firemaking XP for a log, an item worn or a prayer active when the player returns to Sherlock. Challenges that
 * need a mechanic this world lacks (skullball, rooftop laps, Dorgesh-Kaan lamps, spirit trees, reanimation, cremation,
 * Ardougne Castle chests, elves, Shayzien armour, enchanting, watermelons, sacred eels) are not handed out.
 */
public final class ClueChallenges {
    public enum Kind { OBTAIN, MAKE, KILL, FIREMAKING, EQUIP, PRAYER }

    /** A challenge: what to do and the targets (item ids, a monster name, XP tenths, or a prayer name). */
    public record Challenge(String text, Kind kind, List<String> targets, int count) {
        /** Whether an event completes this challenge. */
        public boolean matches(Kind k, String target, int n) {
            if (k != kind) {
                return false;
            }
            if (kind == Kind.KILL) {
                for (String t : targets) {
                    if (ClueScrolls.norm(target).startsWith(ClueScrolls.norm(t))) {
                        return true;
                    }
                }
                return false;
            }
            return targets.contains(target) && n >= count;
        }
    }

    private static final Map<String, Challenge> ALL = new LinkedHashMap<>();

    private static void add(String text, Kind kind, int count, String... targets) {
        ALL.put(key(text), new Challenge(text, kind, List.of(targets), count));
    }

    static {
        // Elite (Sherlock/Elite).
        add("Equip a Dragon Scimitar.", Kind.EQUIP, 1, "dragon_scimitar");
        add("Craft a nature rune.", Kind.OBTAIN, 1, "nature_rune");
        add("Catch a mottled eel with aerial fishing in Lake Molch.", Kind.OBTAIN, 1, "mottled_eel");
        add("Create a super defence potion.", Kind.MAKE, 1, "super_defence");
        add("Craft a green dragonhide body.", Kind.MAKE, 1, "green_dhide_body");
        add("String a yew longbow.", Kind.MAKE, 1, "yew_longbow");
        add("Kill a Dust Devil.", Kind.KILL, 1, "Dust devil");
        add("Catch a black warlock.", Kind.OBTAIN, 1, "black_warlock");
        add("Catch a red chinchompa.", Kind.OBTAIN, 1, "red_chinchompa");
        add("Mine a mithril ore.", Kind.OBTAIN, 1, "mithril_ore");
        add("Smith a mithril 2h sword.", Kind.MAKE, 1, "mithril_2h_sword");
        add("Catch a raw shark.", Kind.OBTAIN, 1, "raw_shark");
        add("Cut a yew log.", Kind.OBTAIN, 1, "yew_logs");
        add("Burn a yew log.", Kind.FIREMAKING, 1, "2025");
        add("Cook a swordfish", Kind.MAKE, 1, "swordfish");
        add("Craft multiple cosmic runes from a single essence.", Kind.OBTAIN, 2, "cosmic_rune");
        add("Activate the Chivalry prayer.", Kind.PRAYER, 1, "CHIVALRY");
        add("Mine some nickel.", Kind.OBTAIN, 1, "nickel_ore");
        // Master (Sherlock/Master).
        add("Equip an abyssal whip in front of the abyssal demons of the Slayer Tower.", Kind.EQUIP, 1, "abyssal_whip",
                "abyssal_tentacle"); // approx: anywhere, not only in front of the Slayer Tower's demons
        add("Smith a runite med helm.", Kind.MAKE, 1, "rune_med_helm");
        add("Create a Barrows teleport tablet.", Kind.MAKE, 1, "barrows_teleport_tablet");
        add("Kill a Nechryael in the Slayer Tower.", Kind.KILL, 1, "Nechryael"); // approx: any nechryael
        add("Kill a Spiritual Mage while wearing something from their god.", Kind.KILL, 1,
                "Spiritual mage"); // approx: the god item is not checked
        add("Create an unstrung dragonstone amulet at a furnace.", Kind.MAKE, 1, "dragonstone_amulet_u");
        add("Burn a magic log.", Kind.FIREMAKING, 1, "3038");
        add("Burn a redwood log.", Kind.FIREMAKING, 1, "3500");
        add("Mix an anti-venom potion.", Kind.MAKE, 1, "anti_venom");
        add("Mine a piece of Runite ore", Kind.OBTAIN, 1, "runite_ore"); // approx: the golden prospector kit is not checked
        add("Steal a gem from the Ardougne market.", Kind.OBTAIN, 1, "uncut_sapphire", "uncut_emerald", "uncut_ruby",
                "uncut_diamond"); // approx: any uncut gem from a stall counts
        add("Bind a blood rune at the blood altar.", Kind.OBTAIN, 1, "blood_rune");
        add("Create a ranging mix potion.", Kind.MAKE, 1, "ranging_mix");
        add("Fletch a rune dart.", Kind.MAKE, 1, "rune_dart");
        add("Kill a lizardman shaman.", Kind.KILL, 1, "Lizardman shaman");
        add("Catch an Anglerfish.", Kind.OBTAIN, 1, "raw_anglerfish"); // approx: the spirit angler outfit is not checked
        add("Chop a redwood log.", Kind.OBTAIN, 1, "redwood_logs"); // approx: the lumberjack outfit is not checked
        add("Kill a Fiyr shade inside Mort'tons shade catacombs.", Kind.KILL, 1, "Fiyr shade");
        add("Catch a tecu salamander.", Kind.OBTAIN, 1, "tecu_salamander");
    }

    private ClueChallenges() {
    }

    static String key(String text) {
        return text.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** The modelled challenge for a Sherlock clue's text, or null. */
    public static Challenge of(String text) {
        return text == null ? null : ALL.get(key(text));
    }

    public static Map<String, Challenge> all() {
        return ALL;
    }
}
