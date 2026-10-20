package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.content.bosses.MinigameBosses;

/**
 * The Crystalline/Corrupted Hunllef's two cycles (wiki "Crystalline Hunllef", Attack style cycle and Protection prayer cycle),
 * as pure state the boss script drives:
 * <ul>
 *   <li>Attacks: ranged first, switching between ranged and magic after every 4 attacks; standard, prayer-disabling and
 *   tornado-summoning attacks all count, the stomp (a player underneath it) doesn't. A tornado summon takes every
 *   {@value MinigameBosses#HUNLLEF_TORNADO_EVERY}th attack (approx) and a magic attack may switch prayers off.</li>
 *   <li>Prayer: it protects from one combat style (a random one per run) and every sixth off-prayer attack it takes, misses
 *   included, makes it protect from that attack's style.</li>
 * </ul>
 */
public final class HunllefCycle {
    public enum Attack { RANGED, MAGIC, PRAYER_DISABLE, TORNADO, STOMP }

    public enum Style { MELEE, RANGED, MAGIC }

    private boolean magic;
    private int styleAttacks;
    private int sinceTornado;
    private Style prays;
    private int offPrayerHits;
    private boolean switched;

    public HunllefCycle(Style startingPrayer) {
        this.prays = startingPrayer;
    }

    /**
     * The next attack. {@code underneath}: the player stands under it (a stomp, outside the cycle); {@code disableRoll}: the
     * random roll for a prayer-disabling magic attack came up.
     */
    public Attack next(boolean underneath, boolean disableRoll) {
        switched = false;
        if (underneath) {
            return Attack.STOMP;
        }
        if (styleAttacks >= MinigameBosses.HUNLLEF_ATTACKS_PER_STYLE) {
            styleAttacks = 0;
            magic = !magic;
            switched = true;
        }
        styleAttacks++;
        if (++sinceTornado >= MinigameBosses.HUNLLEF_TORNADO_EVERY) {
            sinceTornado = 0;
            return Attack.TORNADO;
        }
        if (magic) {
            return disableRoll ? Attack.PRAYER_DISABLE : Attack.MAGIC;
        }
        return Attack.RANGED;
    }

    /** Whether the last {@link #next} began a new attack style (the script announces it). */
    public boolean switchedStyle() {
        return switched;
    }

    /**
     * A player's attack of this style: true when its prayer blocks it (no damage). Otherwise it counts as an off-prayer
     * attack, and the sixth makes it pray against that style ({@link #prayerSwitched} then says so).
     */
    public boolean playerAttack(Style style) {
        prayerSwitched = false;
        if (style == prays) {
            return true;
        }
        if (++offPrayerHits >= MinigameBosses.HUNLLEF_HITS_PER_PRAYER) {
            offPrayerHits = 0;
            prays = style;
            prayerSwitched = true;
        }
        return false;
    }

    private boolean prayerSwitched;

    public boolean prayerSwitched() {
        return prayerSwitched;
    }

    public boolean magic() {
        return magic;
    }

    public Style prays() {
        return prays;
    }

    public int styleAttacks() {
        return styleAttacks;
    }

    public int offPrayerHits() {
        return offPrayerHits;
    }
}
