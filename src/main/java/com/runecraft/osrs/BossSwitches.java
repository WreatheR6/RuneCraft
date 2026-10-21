package com.runecraft.osrs;

import com.runecraft.osrs.content.bosses.MinigameBosses;
import com.runecraft.osrs.content.bosses.SlayerBosses;
import com.runecraft.osrs.content.bosses.ZulrahVorkath;
import com.runecraft.osrs.content.minigames.HunllefCycle;
import com.runecraft.osrs.content.raids.TobRules;
import java.util.List;
import java.util.Locale;

/**
 * What the boss bar counts down for the bosses whose next switch the player must predict (round 43, {@code // playability};
 * the text goes through {@link BossBarStatus}). OSRS players count these themselves, attack by attack, on the 0.6 s tick; in
 * real time the boss bar counts for them. Every count here is read from the same state and rule the boss scripts act on (the
 * Hunllef's {@link HunllefCycle}, the Hydra's style and special counters, Zulrah's rotation, Verzik's attack counters), so the
 * countdown reaches zero exactly when the switch happens and no number of the fight changes.
 */
public final class BossSwitches {
    private BossSwitches() {
    }

    /** "special next!" for the very next attack, else "special in n" ({@code attacksUntil} counts the special itself). */
    public static String nextText(String what, int attacksUntil) {
        return attacksUntil <= 1 ? what + " next!" : what + " in " + attacksUntil;
    }

    /** A wait in game ticks as seconds ("in 2.4s"). */
    public static String inSeconds(String what, long gameTicks) {
        return what + " in " + String.format(Locale.ROOT, "%.1fs", Math.max(0, gameTicks) * 0.6);
    }

    // ------------------------------------------------------------------ the Crystalline / Corrupted Hunllef

    /** Attacks of the Hunllef's current style still to come before it switches (0: its next attack is the other style). */
    public static int hunllefLeft(HunllefCycle cycle) {
        return Math.max(0, MinigameBosses.HUNLLEF_ATTACKS_PER_STYLE - cycle.styleAttacks());
    }

    /** Whether the Hunllef's next attack is magic (the style after the switch once {@link #hunllefLeft} is 0). */
    public static boolean hunllefNextMagic(HunllefCycle cycle) {
        return hunllefLeft(cycle) == 0 ? !cycle.magic() : cycle.magic();
    }

    /** Off-prayer hits still to land before the Hunllef prays against that style (the sixth switches it). */
    public static int hunllefPrayerLeft(HunllefCycle cycle) {
        return Math.max(0, MinigameBosses.HUNLLEF_HITS_PER_PRAYER - cycle.offPrayerHits());
    }

    // ------------------------------------------------------------------ the Alchemical Hydra

    /**
     * Basic attacks of the Hydra's current style still to come before it switches (0: its next one is the other style), in the
     * first three phases ({@link SlayerBosses#HYDRA_STYLE_EVERY}); its enraged phase switches every attack.
     */
    public static int hydraLeft(int styleCount) {
        return Math.max(0, SlayerBosses.HYDRA_STYLE_EVERY - styleCount);
    }

    /** Whether the Hydra's next basic attack is magic: the other style once {@link #hydraLeft} is 0, or every attack enraged. */
    public static boolean hydraNextMagic(boolean magicNow, int styleCount, boolean enraged) {
        return enraged || hydraLeft(styleCount) == 0 ? !magicNow : magicNow;
    }

    /** Attacks until the Hydra's phase special ({@link SlayerBosses#hydraSpecialDue}), the special itself counted. */
    public static int hydraSpecialIn(int autos, int specialsThisPhase) {
        int at = specialsThisPhase == 0 ? SlayerBosses.HYDRA_FIRST_SPECIAL : SlayerBosses.HYDRA_SPECIAL_EVERY;
        return Math.max(1, at - autos + 1);
    }

    // ------------------------------------------------------------------ Zulrah

    /** The form Zulrah surfaces in next: the next phase of its rotation, or the green opening of the next rotation. */
    public static ZulrahVorkath.Form zulrahNextForm(int rotation, int step) {
        List<ZulrahVorkath.Step> rot = ZulrahVorkath.ROTATIONS.get(rotation);
        return step + 1 < rot.size() ? rot.get(step + 1).form() : ZulrahVorkath.LOOP_START.form();
    }

    /** A form's name on the bar: its colour and style. */
    public static String zulrahFormName(ZulrahVorkath.Form form) {
        return switch (form) {
            case SERPENTINE -> "green (ranged)";
            case MAGMA -> "crimson (melee)";
            case TANZANITE -> "blue (magic)";
            case JAD_RANGED -> "green (alternating, ranged first)";
            case JAD_MAGIC -> "green (alternating, magic first)";
        };
    }

    // ------------------------------------------------------------------ Verzik Vitur

    /** Game ticks until Verzik's phase 1 blast is charged (her room's clock {@code t} blasts on t % 14 == 6), at least 1. */
    public static int verzikBlastIn(int t) {
        return Math.floorMod(6 - t - 1, TobRules.VERZIK_P1_BLAST_TICKS) + 1;
    }

    /** Attacks until her phase 2 lightning (every {@link TobRules#VERZIK_LIGHTNING_EVERY}th attack), the lightning counted. */
    public static int verzikLightningIn(int attacks) {
        return TobRules.VERZIK_LIGHTNING_EVERY - Math.floorMod(attacks, TobRules.VERZIK_LIGHTNING_EVERY);
    }

    /** Attacks until her next phase 3 special (one after every four attacks), the special counted. */
    public static int verzikSpecialIn(int attacks) {
        int every = TobRules.VERZIK_P3_ATTACKS_PER_SPECIAL + 1;
        return every - Math.floorMod(attacks, every);
    }

    /** Her next phase 3 special, in the wiki's order. */
    public static TobRules.P3Special verzikNextSpecial(int specials) {
        return TobRules.P3_ROTATION.get(Math.floorMod(specials, TobRules.P3_ROTATION.size()));
    }

    /** A special's name on the bar. */
    public static String verzikSpecialName(TobRules.P3Special s) {
        return switch (s) {
            case NYLOCAS -> "crabs";
            case WEBS -> "webs";
            case YELLOWS -> "yellows";
            case GREEN_BALL -> "green ball";
        };
    }
}
