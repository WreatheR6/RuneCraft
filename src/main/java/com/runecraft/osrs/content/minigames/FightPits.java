package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.List;

/**
 * The TzHaar Fight Pit (Mor Ul Rek, 2398, 5151; OSRS Wiki "TzHaar Fight Pit"): a safe free-for-all
 * where the last player standing becomes champion, wins tokkul equal to the combined combat levels
 * of the players defeated that game and wears the champion's red skull for an hour. Fight Cave
 * monsters join the fight as it drags on (Tz-Kih from 90 seconds, Tz-Kek 3.5 minutes, Tok-Xil 5.5,
 * Ket-Zek 7.5, TzTok-Jad 9.5) and after 11.5 minutes TzHaar gas deals 1 damage every game tick.
 */
public final class FightPits {
    private FightPits() {
    }

    public static final int MIN_PLAYERS = 2;
    /** Game ticks (0.6 s) into a match when each Fight Cave monster starts spawning. */
    public static final int TZ_KIH_TICK = 150;
    public static final int TZ_KEK_TICK = 350;
    public static final int TOK_XIL_TICK = 550;
    public static final int KET_ZEK_TICK = 750;
    public static final int JAD_TICK = 950;
    /** From 11.5 minutes the arena fills with gas: 1 damage a tick to everyone. */
    public static final int GAS_TICK = 1150;
    public static final int GAS_DAMAGE = 1;
    /** The champion's red skull lasts an hour (6,000 game ticks). */
    public static final int SKULL_TICKS = 6000;
    /**
     * Moving further than this many blocks between two one-second checks (or changing dimension) counts as a teleport,
     * which removes the champion's skull. // approx: running covers about 6 blocks a second
     */
    public static final double TELEPORT_BLOCKS = 24;

    /** Why a champion loses the red skull (OSRS Wiki, "TzHaar Fight Pit" rewards), or null if they keep it. */
    public enum SkullLoss {
        EXPIRED("Your Fight Pit champion's skull has faded."),
        TELEPORT("Your Fight Pit champion's skull vanishes as you teleport."),
        GEAR("Your Fight Pit champion's skull vanishes as you change your equipment."),
        OVERHEAD_PRAYER("Your Fight Pit champion's skull vanishes as you activate an overhead prayer."),
        MONSTER_DAMAGE("Your Fight Pit champion's skull vanishes as a monster hurts you."),
        DEFEATED("You have lost your Fight Pit champion's skull.");

        public final String message;

        SkullLoss(String message) {
            this.message = message;
        }
    }

    /**
     * The champion's skull lasts an hour unless they teleport, change their weapons or armour after leaving the pit, turn on an
     * overhead prayer, or take damage from a monster (logging out and being defeated also remove it).
     */
    public static SkullLoss skullLoss(long now, long until, double movedBlocks, boolean sameWorld, boolean gearChanged,
                                      boolean overheadPrayer) {
        if (now >= until) {
            return SkullLoss.EXPIRED;
        }
        if (!sameWorld || movedBlocks > TELEPORT_BLOCKS) {
            return SkullLoss.TELEPORT;
        }
        if (gearChanged) {
            return SkullLoss.GEAR;
        }
        return overheadPrayer ? SkullLoss.OVERHEAD_PRAYER : null;
    }
    /** Ticks between monster spawns once they've started (approx: the wiki gives no rate). */
    public static final int SPAWN_INTERVAL = 25;

    /** The monsters that can spawn {@code tick} game ticks into a match, weakest first. */
    public static List<String> monsters(long tick) {
        List<String> out = new ArrayList<>();
        if (tick >= TZ_KIH_TICK) {
            out.add("tz_kih");
        }
        if (tick >= TZ_KEK_TICK) {
            out.add("tz_kek");
        }
        if (tick >= TOK_XIL_TICK) {
            out.add("tok_xil");
        }
        if (tick >= KET_ZEK_TICK) {
            out.add("ket_zek");
        }
        if (tick >= JAD_TICK) {
            out.add(TzHaarWaves.JAD);
        }
        return out;
    }

    /** Tokkul for the champion: the combined combat levels of the players defeated during the game. */
    public static int tokkul(List<Integer> defeatedCombatLevels) {
        return defeatedCombatLevels.stream().mapToInt(Integer::intValue).sum();
    }
}
