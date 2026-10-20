package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.GielinorMap;

/**
 * The Ferox Enclave (OSRS Wiki "Ferox Enclave", "Pool of Refreshment", "Barrier (Ferox Enclave)"): a safe zone inside level
 * 13-16 Wilderness where players can't fight each other. A tele-blocked player can't pass its barriers until the spell wears
 * off. Its chapel's pools of refreshment restore hitpoints, prayer points and run energy, cure poison and venom and reset
 * every boosted or lowered skill, and turn off active prayers, but (unlike the Bounty Hunter crater's) never restore special
 * attack energy. The looting bag can't be filled inside it.
 */
public final class FeroxEnclave {
    private FeroxEnclave() {
    }

    /** The pool block in the chapel (EasternWilderness's Ferox Enclave structure). */
    public static final String BLOCK_POOL = "ferox_pool_of_refreshment";
    public static final String POOL_MESSAGE = "You feel reinvigorated after drinking from the pool.";
    public static final String BARRIER_MESSAGE = "A magical barrier stops you entering the enclave while you are tele-blocked.";

    /** What the enclave's pools do (the crater's pool also restores special attack energy; these don't). */
    public record PoolEffect(boolean hitpoints, boolean prayer, boolean runEnergy, boolean resetStats, boolean curesPoison,
                             boolean curesVenom, boolean disablesPrayers, boolean specialAttack) {
    }

    public static final PoolEffect POOL = new PoolEffect(true, true, true, true, true, true, true, false);

    /** Inside the enclave's walls (the bounds are {@link GielinorMap#inFeroxEnclave}). */
    public static boolean contains(int x, int y) {
        return GielinorMap.inFeroxEnclaveTile(x, y);
    }

    /** A tele-blocked player can't pass a barrier into the enclave (one already in combat may: wiki). */
    public static boolean barrierLetsIn(boolean teleBlocked) {
        return !teleBlocked;
    }

    /** No player-versus-player fighting while either player is inside the enclave. */
    public static boolean pvpAllowed(boolean attackerInside, boolean targetInside) {
        return !attackerInside && !targetInside;
    }
}
