package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;

/**
 * While Guthix Sleeps: the Balance Elemental's thresholds (wiki, Balance Elemental "thresholds" table). It has 750 hitpoints
 * and fights in one of three forms for four attacks at a time: fiery (ranged), airy (melee, crush) and watery (magic).
 * Below 550 HP it attacks every 5 ticks instead of 6 and protects against one style (fiery: melee, airy: magic, watery:
 * ranged); below 200 HP it attacks every 2 ticks and protects against two styles, so all three are needed (wiki).
 */
public final class BalanceElemental {
    private BalanceElemental() {
    }

    public static final int HITPOINTS = 750;
    /** Below this many hitpoints: attack speed 5 and one protection prayer. */
    public static final int ONE_PRAYER_BELOW = 550;
    /** Below this many hitpoints: attack speed 2 and two protection prayers. */
    public static final int TWO_PRAYERS_BELOW = 200;
    /** Attacks before it changes form (wiki). */
    public static final int ATTACKS_PER_FORM = 4;

    /** Style families: forms and the player's attacks. */
    public static final int MELEE = 0;
    public static final int RANGED = 1;
    public static final int MAGIC = 2;

    /** The BossDef attack each form uses (index = style family). */
    public static final String[] FORM_ATTACKS = {"melee_form", "ranged_form", "magic_form"};
    /**
     * The stat-draining special each form opens with (index = style family): the first attack after every change of form (wiki:
     * a drain, then four normal attacks, then the next form). Up to 89 instead of 40, and it lowers the player's combat stats
     * and Prayer, unless the matching protection prayer is up when it lands.
     */
    public static final String[] FORM_DRAINS = {"melee_drain", "ranged_drain", "magic_drain"};
    /** Max hits (wiki infobox): 40 standard, 89 for the stat-draining attack. */
    public static final int STANDARD_MAX = 40;
    public static final int DRAIN_MAX = 89;
    /**
     * Game ticks between the form change and the draining attack landing. // playability: OSRS gives about 2 ticks to switch
     * prayers once the head changes colour; 3 here, with the form named in chat and on the cast bar
     */
    public static final int DRAIN_WARN_TICKS = 3;

    /** Whether an attack name is one of the draining specials. */
    public static boolean isDrain(String attack) {
        for (String d : FORM_DRAINS) {
            if (d.equals(attack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Levels an unprotected draining attack takes off each combat stat (Attack, Strength, Defence, Ranged, Magic).
     * // approx: the wiki says only that the stats are "majorly drained"; a fifth of the current level plus 2
     */
    public static int statDrain(int currentLevel) {
        return currentLevel <= 0 ? 0 : currentLevel / 5 + 2;
    }

    /** Prayer points it takes. // approx: the wiki says "a large chunk"; a third of the points left, at least 10 */
    public static int prayerDrain(int points) {
        return Math.min(points, Math.max(10, points / 3));
    }
    /** How each form looks (index = style family). */
    public static final String[] FORM_NAMES = {"airy", "fiery", "watery"};

    public static int family(AttackType type) {
        return type.isMelee() ? MELEE : type == AttackType.RANGED ? RANGED : MAGIC;
    }

    /** Attack speed in game ticks at {@code hp} hitpoints. */
    public static int attackSpeed(int hp) {
        return hp < TWO_PRAYERS_BELOW ? 2 : hp < ONE_PRAYER_BELOW ? 5 : 6;
    }

    /** The style the form prays against below 550 HP (wiki table): fiery prays melee, airy magic, watery ranged. */
    public static int prayedStyle(int form) {
        return (form + 2) % 3;
    }

    /**
     * Whether the elemental in {@code form} protects against an attack of style family {@code incoming} at {@code hp}
     * hitpoints. Below 200 HP the second prayer is against its own form's style, leaving one style per form that hurts it,
     * a different one for each form. // approx: the wiki only says it prays against two styles and swaps them as it changes form
     */
    public static boolean protects(int form, int incoming, int hp) {
        if (hp >= ONE_PRAYER_BELOW) {
            return false;
        }
        return incoming == prayedStyle(form) || hp < TWO_PRAYERS_BELOW && incoming == form;
    }

    /** The only style that hurts the elemental in {@code form} below 200 HP. */
    public static int vulnerableStyle(int form) {
        return (form + 1) % 3;
    }

    public static String styleName(int family) {
        return switch (family) {
            case MELEE -> "melee";
            case RANGED -> "ranged";
            default -> "magic";
        };
    }
}
