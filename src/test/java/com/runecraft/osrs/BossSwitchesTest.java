package com.runecraft.osrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runecraft.osrs.content.bosses.MinigameBosses;
import com.runecraft.osrs.content.bosses.SlayerBosses;
import com.runecraft.osrs.content.bosses.ZulrahVorkath;
import com.runecraft.osrs.content.minigames.HunllefCycle;
import com.runecraft.osrs.content.raids.TobRules;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round 43: the boss bar's "switches in N" counts reach zero exactly when the boss switches, read from the same state the
 * scripts act on, and the rules they count (4 Hunllef attacks a style, 3 Hydra attacks a style, Verzik's every-fifth
 * lightning and four attacks between phase 3 specials) are the OSRS numbers.
 */
class BossSwitchesTest {
    @Test
    void osrsNumbersUnchanged() {
        assertEquals(4, MinigameBosses.HUNLLEF_ATTACKS_PER_STYLE);
        assertEquals(6, MinigameBosses.HUNLLEF_HITS_PER_PRAYER);
        assertEquals(3, SlayerBosses.HYDRA_STYLE_EVERY);
        assertEquals(5, TobRules.VERZIK_LIGHTNING_EVERY);
        assertEquals(4, TobRules.VERZIK_P3_ATTACKS_PER_SPECIAL);
        assertEquals(14, TobRules.VERZIK_P1_BLAST_TICKS);
    }

    @Test
    void hunllefCountdownMeetsTheSwitch() {
        HunllefCycle cycle = new HunllefCycle(HunllefCycle.Style.MELEE);
        int switches = 0;
        for (int i = 0; i < 60; i++) {
            int left = BossSwitches.hunllefLeft(cycle);
            boolean nextMagic = BossSwitches.hunllefNextMagic(cycle);
            cycle.next(false, false);
            assertEquals(left == 0 && i > 0, cycle.switchedStyle(), "attack " + i + ": the count said " + left);
            assertEquals(nextMagic, cycle.magic(), "attack " + i + ": the bar's colour named the wrong style");
            switches += cycle.switchedStyle() ? 1 : 0;
        }
        assertEquals(14, switches, "60 attacks at 4 a style: 14 switches after the first style");
        // a stomp (the player under it) is outside the cycle: the count doesn't move
        int before = BossSwitches.hunllefLeft(cycle);
        cycle.next(true, false);
        assertEquals(before, BossSwitches.hunllefLeft(cycle));
    }

    @Test
    void hunllefPrayerCount() {
        HunllefCycle cycle = new HunllefCycle(HunllefCycle.Style.MAGIC);
        for (int i = 0; i < 5; i++) {
            assertEquals(6 - i, BossSwitches.hunllefPrayerLeft(cycle));
            cycle.playerAttack(HunllefCycle.Style.MELEE);
            assertFalse(cycle.prayerSwitched());
        }
        assertEquals(1, BossSwitches.hunllefPrayerLeft(cycle));
        cycle.playerAttack(HunllefCycle.Style.MELEE);
        assertTrue(cycle.prayerSwitched());
        assertEquals(HunllefCycle.Style.MELEE, cycle.prays());
        assertEquals(6, BossSwitches.hunllefPrayerLeft(cycle));
    }

    /** The Hydra's counters as SlayerScripts.Hydra.nextAttack moves them (phases 1-3 and enraged). */
    @Test
    void hydraCountdownMeetsTheSwitchAndSpecial() {
        for (boolean enraged : new boolean[]{false, true}) {
            int styleCount = 0;
            boolean magic = false;
            int autos = 0;
            int specials = 0;
            for (int i = 0; i < 40; i++) {
                int specialIn = BossSwitches.hydraSpecialIn(autos, specials);
                boolean predictedMagic = BossSwitches.hydraNextMagic(magic, styleCount, enraged);
                int left = BossSwitches.hydraLeft(styleCount);
                if (SlayerBosses.hydraSpecialDue(autos, specials)) {
                    assertEquals(1, specialIn, "attack " + i + ": the special came while the bar said " + specialIn);
                    autos = 0;
                    specials++;
                    continue;
                }
                assertTrue(specialIn > 1, "attack " + i + ": the bar said the special was next");
                boolean switched;
                if (enraged) {
                    magic = !magic;
                    switched = true;
                } else {
                    switched = styleCount >= SlayerBosses.HYDRA_STYLE_EVERY;
                    if (switched) {
                        styleCount = 0;
                        magic = !magic;
                    }
                    styleCount++;
                    assertEquals(left == 0, switched, "attack " + i + ": the count said " + left);
                }
                assertEquals(predictedMagic, magic, "attack " + i + (enraged ? " (enraged)" : "") + ": wrong next style");
                autos++;
            }
        }
    }

    @Test
    void zulrahNextForm() {
        // Rotation 1 (Crimson A): green opening, crimson, tanzanite, green south ...; after its last phase the next rotation
        // opens green in the middle
        assertEquals(ZulrahVorkath.Form.MAGMA, BossSwitches.zulrahNextForm(0, 0));
        assertEquals(ZulrahVorkath.Form.TANZANITE, BossSwitches.zulrahNextForm(0, 1));
        assertEquals(ZulrahVorkath.Form.SERPENTINE, BossSwitches.zulrahNextForm(0, 2));
        List<ZulrahVorkath.Step> rot = ZulrahVorkath.ROTATIONS.get(0);
        assertEquals(ZulrahVorkath.Form.MAGMA, BossSwitches.zulrahNextForm(0, rot.size() - 2));
        assertEquals(ZulrahVorkath.Form.SERPENTINE, BossSwitches.zulrahNextForm(0, rot.size() - 1));
        for (int r = 0; r < ZulrahVorkath.ROTATIONS.size(); r++) {
            List<ZulrahVorkath.Step> steps = ZulrahVorkath.ROTATIONS.get(r);
            for (int s = 0; s + 1 < steps.size(); s++) {
                assertEquals(steps.get(s + 1).form(), BossSwitches.zulrahNextForm(r, s));
            }
            assertEquals(ZulrahVorkath.LOOP_START.form(), BossSwitches.zulrahNextForm(r, steps.size() - 1));
        }
        assertEquals("crimson (melee)", BossSwitches.zulrahFormName(ZulrahVorkath.Form.MAGMA));
    }

    /** Verzik's counters as TobRooms.Verzik moves them: attacks++ then a lightning / special on the multiple. */
    @Test
    void verzikCountdowns() {
        int attacks = 0;
        int specials = 0;
        for (int i = 0; i < 30; i++) {
            int lightningIn = BossSwitches.verzikLightningIn(attacks);
            int specialIn = BossSwitches.verzikSpecialIn(attacks);
            TobRules.P3Special next = BossSwitches.verzikNextSpecial(specials);
            attacks++;
            assertEquals(lightningIn == 1, attacks % TobRules.VERZIK_LIGHTNING_EVERY == 0, "P2 attack " + attacks);
            boolean special = attacks % (TobRules.VERZIK_P3_ATTACKS_PER_SPECIAL + 1) == 0;
            assertEquals(specialIn == 1, special, "P3 attack " + attacks);
            if (special) {
                assertEquals(TobRules.P3_ROTATION.get(specials++ % TobRules.P3_ROTATION.size()), next);
            }
        }
        assertEquals(TobRules.P3Special.NYLOCAS, BossSwitches.verzikNextSpecial(0));
        assertEquals(TobRules.P3Special.GREEN_BALL, BossSwitches.verzikNextSpecial(3));
        // phase 1: the room's clock blasts on t % 14 == 6, checked after t moves on
        for (int t = 0; t < 40; t++) {
            int in = BossSwitches.verzikBlastIn(t);
            assertTrue(in >= 1 && in <= 14);
            assertEquals(6, (t + in) % 14, "t " + t);
            for (int k = 1; k < in; k++) {
                assertTrue((t + k) % 14 != 6, "an earlier blast at t " + (t + k));
            }
        }
    }

    @Test
    void texts() {
        assertEquals("lightning next!", BossSwitches.nextText("lightning", 1));
        assertEquals("special in 3", BossSwitches.nextText("special", 3));
        assertEquals("blast in 1.2s", BossSwitches.inSeconds("blast", 2));
    }
}
