package com.runecraft.osrs.content.minigames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runecraft.osrs.ArenaTiming;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round 42 (playability, CONTENT_GUIDE rule 5): the wave minigames' real-time adaptations. Every OSRS number stays (max hits,
 * attack speeds, wave cadence, prayer rules); only cue leads, landing spacing and hit areas change, and each one gives a human
 * the round 41 floors: a telegraph at least 36 Minecraft ticks, a strike at least 3 game ticks, different styles at least 20
 * Minecraft ticks apart.
 */
class WaveMinigameRealtimeTest {
    private static final int MC = 12;

    private static AttackDef attack(MonsterDef m, AttackType style) {
        for (AttackDef a : m.attacks) {
            if (a.style() == style) {
                return a;
            }
        }
        throw new AssertionError(m.id + " has no " + style + " attack");
    }

    // ------------------------------------------------------------------ the Inferno

    /** Max hits and attack speeds of the Inferno's attackers are the wiki's, cues or not. */
    @Test
    void infernoNumbersUnchanged() {
        String[][] expected = {
                {"jal_xil", "4", "RANGED 46", "CRUSH 19"},
                {"jal_zek", "4", "MAGIC 70", "STAB 52"},
                {"jal_mejrah", "3", "RANGED 19"},
                {"jaltok_jad", "8", "MAGIC 113", "RANGED 113", "STAB 113"},
                {"jal_ak", "6", "CRUSH 29", "RANGED 29", "MAGIC 29"},
                {"jal_imkot", "4", "SLASH 49"},
        };
        for (String[] e : expected) {
            MonsterDef m = ContentRegistry.monster(e[0]);
            assertEquals(Integer.parseInt(e[1]), m.attackSpeed, e[0] + " attack speed");
            List<String> got = new ArrayList<>();
            for (AttackDef a : m.attacks) {
                got.add(a.style() + " " + a.maxHit());
            }
            List<String> want = new ArrayList<>(List.of(e).subList(2, e.length));
            assertEquals(want, got, e[0]);
        }
        BossDef zuk = ContentRegistry.boss(TzHaarWaves.ZUK);
        assertEquals(TzHaarWaves.ZUK_SPEED, zuk.attackSpeed);
        assertEquals(1, zuk.attacks.size());
        assertEquals(148, zuk.attacks.get(0).maxHit());
        assertEquals(AttackDef.Special.PRAYER_PIERCING, zuk.attacks.get(0).special());
        assertEquals(10, TzHaarWaves.ZUK_SPEED);
        assertEquals(7, TzHaarWaves.ZUK_ENRAGED_SPEED);
        assertEquals(9, TzHaarWaves.TRIPLE_JAD_SPEED);
        assertEquals(3, TzHaarWaves.TRIPLE_JAD_STAGGER);
        assertEquals(95, TzHaarWaves.JAD_MAGIC_MAX);
        assertEquals(3, TzHaarWaves.BLOB_READ_TICKS);
    }

    /**
     * Jal-Xil, Jal-Zek and Jal-MejRah: every ranged or magic attack glows in its style for at least the 36 MC floor; their
     * melee stays a wind-up. Jal-Ak's attacks stay untelegraphed (its prayer scan, 36 MC ahead, is the cue).
     */
    @Test
    void infernoStyleCues() {
        for (String id : new String[]{TzHaarWaves.JAL_XIL, TzHaarWaves.JAL_ZEK, TzHaarWaves.JAL_MEJRAH}) {
            for (AttackDef a : ContentRegistry.monster(id).attacks) {
                if (a.melee()) {
                    assertEquals(0, a.telegraphTicks(), id + " melee is a wind-up, not a telegraph");
                    continue;
                }
                assertTrue(ThreatTiming.telegraphed(a.telegraphTicks(), a.special()), id + " " + a.style() + " has no cue");
                assertTrue(ThreatTiming.leadMc(a.telegraphTicks(), a.special()) >= ThreatTiming.TELEGRAPH_FLOOR_MC, id);
            }
        }
        for (AttackDef a : ContentRegistry.monster(TzHaarWaves.JAL_AK).attacks) {
            assertEquals(0, a.telegraphTicks(), "Jal-Ak's attack is cued by its scan");
        }
        assertTrue(TzHaarWaves.blobCueMc() >= ThreatTiming.TELEGRAPH_FLOOR_MC);
        // A Jal-MejRah (speed 3) is cued for exactly its whole cycle: its attacks never overlap their own cues.
        int bat = ThreatTiming.leadMc(TzHaarWaves.STYLE_CUE_TICKS, AttackDef.Special.NONE);
        assertTrue(bat <= ContentRegistry.monster(TzHaarWaves.JAL_MEJRAH).attackSpeed * MC);
    }

    /**
     * Wave 68: three Jads every 9 ticks, 3 apart. Each telegraph lasts its 36 MC and ends as the next Jad's begins, so one Jad
     * glows at a time; over a minute each attacks 9-tick-often, exactly as many times as in OSRS.
     */
    @Test
    void tripleJadCuesTakeTurns() {
        MonsterDef jad = ContentRegistry.monster(TzHaarWaves.JALTOK_JAD);
        AttackDef magic = attack(jad, AttackType.MAGIC);
        AttackDef ranged = attack(jad, AttackType.RANGED);
        int lead = ThreatTiming.leadMc(magic.telegraphTicks(), magic.special());
        assertEquals(lead, ThreatTiming.leadMc(ranged.telegraphTicks(), ranged.special()));
        assertTrue(lead >= ThreatTiming.TELEGRAPH_FLOOR_MC);
        assertTrue(TzHaarWaves.tripleJadCuesApart(lead));
        assertEquals(3, TzHaarWaves.TRIPLE_JAD_NAMES.length);
        // Simulate the session's cadence (first attacks at 2, 5, 8 ticks) over 100 ticks: cue windows never overlap.
        List<long[]> windows = new ArrayList<>();
        int[] counts = new int[3];
        for (int j = 0; j < 3; j++) {
            for (long t = 2 + (long) j * TzHaarWaves.TRIPLE_JAD_STAGGER; t < 100; t += TzHaarWaves.TRIPLE_JAD_SPEED) {
                windows.add(new long[]{t * MC, t * MC + lead});
                counts[j]++;
            }
        }
        for (int a = 0; a < windows.size(); a++) {
            for (int b = a + 1; b < windows.size(); b++) {
                long[] x = windows.get(a);
                long[] y = windows.get(b);
                assertTrue(x[1] <= y[0] || y[1] <= x[0], "two Jads glow at once");
            }
        }
        for (int c : counts) {
            assertTrue(c == 11 || c == 10, "a Jad attacked " + c + " times in 100 ticks (OSRS: every 9 ticks)");
        }
    }

    /** Zuk's charge is the 36 MC floor; the glyph's shadow is exactly the area it blocks. */
    @Test
    void zukChargeAndGlyphShadow() {
        assertTrue(TzHaarWaves.ZUK_CHARGE_MC >= ThreatTiming.TELEGRAPH_FLOOR_MC);
        // The charge fits his enraged cadence with time to spare.
        assertTrue(TzHaarWaves.ZUK_CHARGE_MC < TzHaarWaves.ZUK_ENRAGED_SPEED * MC);
        double zukZ = -19;
        double gx = 2;
        double gz = -14;
        double w = 1.6;
        double half = TzHaarWaves.glyphShadowHalfWidth(w);
        assertEquals(0.8 + TzHaarWaves.GLYPH_COVER_SLACK, half, 1e-9);
        assertTrue(TzHaarWaves.glyphShields(gx, gz + 3, gx, gz, w, zukZ), "straight behind the glyph");
        assertTrue(TzHaarWaves.glyphShields(gx + half - 0.01, gz + 8, gx, gz, w, zukZ), "inside the shadow's edge");
        assertFalse(TzHaarWaves.glyphShields(gx + half + 0.01, gz + 3, gx, gz, w, zukZ), "beside the shadow");
        assertFalse(TzHaarWaves.glyphShields(gx, gz - 1, gx, gz, w, zukZ), "between Zuk and the glyph");
    }

    /** Pillars: the warning ring's radius is the collapse's reach; wave 66's collapse warns for the strike floor. */
    @Test
    void pillarWarnings() {
        assertTrue(TzHaarWaves.pillarWarns(TzHaarWaves.PILLAR_WARN_HP));
        assertTrue(TzHaarWaves.pillarWarns(1));
        assertFalse(TzHaarWaves.pillarWarns(0), "a fallen pillar warns no more");
        assertFalse(TzHaarWaves.pillarWarns(TzHaarWaves.PILLAR_WARN_HP + 1));
        assertTrue(TzHaarWaves.PILLAR_WARN_HP < TzHaarWaves.PILLAR_HP);
        assertEquals(255, TzHaarWaves.PILLAR_HP);
        // its 3x3 body (1.5) plus a tile, measured to a player's centre
        assertTrue(TzHaarWaves.PILLAR_COLLAPSE_REACH >= 2.5);
        assertTrue(TzHaarWaves.PILLAR_COLLAPSE_WARNING_TICKS >= ArenaTiming.STRIKE_FLOOR_TICKS);
        // the pillars still fall before wave 67 (8 ticks after the wave ends)
        assertTrue(TzHaarWaves.PILLAR_COLLAPSE_WARNING_TICKS < 8);
        assertEquals(50, TzHaarWaves.pillarCollapseDamage(100));
    }

    /** Jal-MejJak lava balls: 5-10 in a 3x3, landing the strike floor's 3 ticks after the throw. */
    @Test
    void mejJakBlasts() {
        assertTrue(TzHaarWaves.MEJJAK_BLAST_TICKS >= ArenaTiming.STRIKE_FLOOR_TICKS);
        assertTrue(TzHaarWaves.inMejJakBlast(1.4, 0, -1.4));
        assertFalse(TzHaarWaves.inMejJakBlast(1.6, 0, 0));
        assertFalse(TzHaarWaves.inMejJakBlast(0, 2.5, 0));
        java.util.Random r = new java.util.Random(4);
        for (int i = 0; i < 200; i++) {
            int d = TzHaarWaves.mejJakBlast(r);
            assertTrue(d >= 5 && d <= 10);
        }
    }

    // ------------------------------------------------------------------ the Colosseum

    /** One manticore's orbs keep 3/5/7; a second one's land at least 2 ticks after the first's last (the 1-tick gap). */
    @Test
    void manticoreOrbSpacing() {
        long[] alone = FortisColosseum.manticoreOrbTicks(100, Long.MIN_VALUE / 2);
        assertEquals(List.of(103L, 105L, 107L), List.of(alone[0], alone[1], alone[2]));
        long second = FortisColosseum.manticoreVolleyTick(100, 100);
        assertEquals(105, second, "the 5-tick stagger is the wiki's");
        long[] natural = {FortisColosseum.manticoreOrbTick(second, 0), FortisColosseum.manticoreOrbTick(second, 1),
                FortisColosseum.manticoreOrbTick(second, 2)};
        assertEquals(1, natural[0] - alone[2], "without the push the second volley's first orb lands a tick after the melee orb");
        long[] pushed = FortisColosseum.manticoreOrbTicks(second, alone[2]);
        assertEquals(List.of(109L, 111L, 113L), List.of(pushed[0], pushed[1], pushed[2]));
        List<Long> all = new ArrayList<>();
        for (long t : alone) {
            all.add(t);
        }
        for (long t : pushed) {
            all.add(t);
        }
        for (int i = 1; i < all.size(); i++) {
            long gapMc = (all.get(i) - all.get(i - 1)) * MC;
            assertTrue(gapMc >= ThreatTiming.STYLE_SPACING_MC, "orbs " + gapMc + " MC apart");
        }
        for (int i = 0; i < 3; i++) {
            assertTrue(pushed[i] - natural[i] <= 1, "a landing moved more than a tick");
        }
        assertEquals(36, FortisColosseum.manticoreOrbMaxHit(AttackType.RANGED));
        assertEquals(31, FortisColosseum.manticoreOrbMaxHit(AttackType.MAGIC));
        assertEquals(31, FortisColosseum.manticoreOrbMaxHit(AttackType.CRUSH));
        assertEquals(10, FortisColosseum.MANTICORE_CYCLE_TICKS);
    }

    /** Sol Heredit's grapple: about 3.5 s on the hotbar keys, the cadence and the perfect parry kept. */
    @Test
    void solGrappleWindow() {
        double seconds = SolHeredit.GRAPPLE_WINDOW_TICKS * 0.6;
        assertTrue(seconds >= 3.4 && seconds <= 3.7, "window " + seconds + " s");
        assertEquals(4, SolHeredit.OSRS_GRAPPLE_WINDOW_TICKS);
        for (int phase = 3; phase <= 6; phase++) {
            assertEquals(SolHeredit.OSRS_GRAPPLE_WINDOW_TICKS + 3, SolHeredit.attackDelay(SolHeredit.Attack.GRAPPLE, phase),
                    "his cadence after a grapple is OSRS's");
        }
        assertFalse(SolHeredit.perfectParry(SolHeredit.GRAPPLE_WINDOW_TICKS - 2));
        assertTrue(SolHeredit.perfectParry(SolHeredit.GRAPPLE_WINDOW_TICKS - 1));
        assertEquals(44, SolHeredit.GRAPPLE_MAX_HIT);
        assertEquals(5, SolHeredit.PERFECT_PARRY_TICKS);
        SolHeredit.Grapple[] order = {SolHeredit.Grapple.BODY, SolHeredit.Grapple.BACK, SolHeredit.Grapple.HANDS,
                SolHeredit.Grapple.LEGS, SolHeredit.Grapple.FEET};
        for (int i = 0; i < order.length; i++) {
            assertEquals(order[i], SolHeredit.grappleByHotbar(i));
            assertEquals(i + 1, SolHeredit.hotbarKey(order[i]));
        }
        assertNull(SolHeredit.grappleByHotbar(5));
        assertNull(SolHeredit.grappleByHotbar(-1));
    }

    /** Sol's lasers: a strike outline round the targeted tile, tested horizontally; a neighbouring tile's middle is safe. */
    @Test
    void solLaserOutline() {
        double r = SolHeredit.LASER_RADIUS;
        assertTrue(r > 0.5 && r < 1.0);
        assertTrue(ArenaTiming.inStrike(0.2, 0.4, -0.3, r), "on the tile");
        assertTrue(ArenaTiming.inStrike(0.5, 0, 0, r), "on the tile's edge");
        assertFalse(ArenaTiming.inStrike(1.0, 0, 0, r), "the middle of the next tile");
        assertFalse(ArenaTiming.inStrike(0.7, 0, 0.7, r), "the middle of the diagonal tile");
        assertTrue(Math.max(ArenaTiming.STRIKE_FLOOR_TICKS, SolHeredit.ENRAGE_LASER_WARNING_TICKS) >= ArenaTiming.STRIKE_FLOOR_TICKS);
        assertEquals(75, SolHeredit.LASER_MAX_HIT);
        assertEquals(3, SolHeredit.LASER_WARNING_TICKS);
        assertEquals(2, SolHeredit.ENRAGE_LASER_WARNING_TICKS);
    }

    // ------------------------------------------------------------------ the Gauntlet

    /** The Hunllef's floor: the fast phase warns for the strike floor, damage and timings otherwise the same. */
    @Test
    void hunllefFloorTiming() {
        assertEquals(5, Gauntlet.floorWarnTicks(0));
        assertEquals(4, Gauntlet.floorWarnTicks(1));
        assertEquals(3, Gauntlet.floorWarnTicks(2), "1.2 s became the 1.8 s strike floor");
        assertTrue(Gauntlet.floorWarnTicks(2) >= ArenaTiming.STRIKE_FLOOR_TICKS);
        assertEquals(10, Gauntlet.FLOOR_MIN);
        assertEquals(20, Gauntlet.FLOOR_MAX);
        assertEquals(4, Gauntlet.FLOOR_ACTIVE_TICKS);
        assertEquals(List.of(16, 12, 8), List.of(Gauntlet.FLOOR_INTERVAL_TICKS[0], Gauntlet.FLOOR_INTERVAL_TICKS[1],
                Gauntlet.FLOOR_INTERVAL_TICKS[2]));
    }

    /** What burns is what the tile outline shows, with a quarter block of leniency at its edge. */
    @Test
    void hunllefFloorTiles() {
        List<int[]> tiles = List.of(new int[]{10, 20}, new int[]{12, 20});
        assertTrue(Gauntlet.onBurningFloor(tiles, 11.0, 21.0), "the middle of a tile");
        assertTrue(Gauntlet.onBurningFloor(tiles, 12.0, 21.0), "across two lit tiles");
        assertFalse(Gauntlet.onBurningFloor(tiles, 10.1, 21.0), "within the leniency of an unlit spot");
        assertFalse(Gauntlet.onBurningFloor(tiles, 13.9, 21.0), "the far edge");
        assertFalse(Gauntlet.onBurningFloor(tiles, 11.0, 22.5), "off the tiles");
        assertTrue(Gauntlet.onBurningFloor(tiles, 10.3, 20.3), "just inside the corner");
    }

    /** Tornadoes chase a tile a game tick, spread over its Minecraft ticks, and never step into the player. */
    @Test
    void tornadoChase() {
        double moved = 0;
        double x = 0;
        for (int i = 0; i < MC; i++) {
            double[] s = Gauntlet.tornadoStep(10 - x, 0);
            x += s[0];
            moved += Math.hypot(s[0], s[1]);
        }
        assertEquals(Gauntlet.TORNADO_STEP_PER_TICK, moved, 1e-9, "one tile a game tick");
        double[] near = Gauntlet.tornadoStep(0.3, 0);
        assertEquals(0, near[0], 1e-9, "already on the player");
        assertEquals(20, Gauntlet.TORNADO_TICKS);
        assertEquals(List.of(10, 20), List.of(Gauntlet.tornadoDamage(Gauntlet.Mode.NORMAL)[0], Gauntlet.tornadoDamage(Gauntlet.Mode.NORMAL)[1]));
        assertEquals(List.of(15, 30), List.of(Gauntlet.tornadoDamage(Gauntlet.Mode.CORRUPTED)[0],
                Gauntlet.tornadoDamage(Gauntlet.Mode.CORRUPTED)[1]));
    }
}
