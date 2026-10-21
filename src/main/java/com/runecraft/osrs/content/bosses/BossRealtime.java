package com.runecraft.osrs.content.bosses;

/**
 * Real-time adaptations of the solo and group bosses' scripted sequences (round 42, docs/PLAYABILITY.md "Later", CONTENT_GUIDE
 * rule 5). Every OSRS number stays: max hits, patterns, cycle lengths and prayer block amounts. Only when a hit of a sequence lands
 * relative to the cue that announces it changes, so a real-time player can read each cue and switch prayers or step away. The
 * shared floors are the round 41 ones: a cue at least {@value #CUE_FLOOR_MC} Minecraft ticks (1.8 s, Jad's 3 game ticks) before
 * the hit it announces, ground strikes {@value #STRIKE_FLOOR_TICKS} game ticks after they appear, and hits that need different
 * protection prayers at least {@value #STYLE_SPACING_MC} MC apart on one target. All // playability unless marked otherwise.
 */
public final class BossRealtime {
    /** Minecraft ticks per OSRS game tick. */
    public static final int MC_PER_GAME_TICK = 12;
    /** Shortest time (MC) from a hit's first visible cue to the hit (ThreatTiming.TELEGRAPH_FLOOR_MC). */
    public static final int CUE_FLOOR_MC = 36;
    /** Damaging ground strikes land at least this many game ticks after they appear (ArenaTiming.STRIKE_FLOOR_TICKS). */
    public static final int STRIKE_FLOOR_TICKS = 3;
    /** Hits that need different protection prayers land at least this far apart (MC) on one target (ThreatTiming). */
    public static final int STYLE_SPACING_MC = 20;

    private BossRealtime() {
    }

    // ------------------------------------------------------------------ the Whisperer's volleys

    /** OSRS: her volley's projectiles leave one game tick apart. */
    public static final int WHISPERER_VOLLEY_GAP_TICKS = 1;
    /**
     * A projectile of the other style leaves two game ticks (24 MC) after the one before it, so the two land at least
     * {@value #STYLE_SPACING_MC} MC apart and each can be prayed against. // playability: OSRS one tick (a one-tick prayer flick)
     */
    public static final int WHISPERER_VOLLEY_SWITCH_GAP_TICKS = 2;

    /** Game ticks between two projectiles of a volley ({@code true}: magic). */
    public static int volleyGapTicks(boolean prevMagic, boolean nextMagic) {
        return prevMagic == nextMagic ? WHISPERER_VOLLEY_GAP_TICKS : WHISPERER_VOLLEY_SWITCH_GAP_TICKS;
    }

    /** When each projectile of a volley leaves, in game ticks after the first. */
    public static int[] volleyLaunchTicks(boolean[] volley) {
        int[] at = new int[volley.length];
        for (int i = 1; i < volley.length; i++) {
            at[i] = at[i - 1] + volleyGapTicks(volley[i - 1], volley[i]);
        }
        return at;
    }

    /** The game ticks the style switches add to a volley over the OSRS one-tick spacing. */
    public static int volleyExtraTicks(boolean[] volley) {
        return volley.length == 0 ? 0 : volleyLaunchTicks(volley)[volley.length - 1] - (volley.length - 1) * WHISPERER_VOLLEY_GAP_TICKS;
    }

    /**
     * Game ticks from a volley's last projectile to her next attack: her attack speed less what the switches added inside the
     * volley (at least 1), so a whole cycle (volley and rest) lasts exactly as long as in OSRS.
     */
    public static int volleyRestTicks(int attackSpeed, int extraTicks) {
        return Math.max(1, attackSpeed - extraTicks);
    }

    /** A volley's whole cycle in game ticks: from its first projectile to her next attack. */
    public static int volleyCycleTicks(boolean[] volley, int attackSpeed) {
        int[] at = volleyLaunchTicks(volley);
        return (volley.length == 0 ? 0 : at[volley.length - 1]) + volleyRestTicks(attackSpeed, volleyExtraTicks(volley));
    }

    // ------------------------------------------------------------------ Zulrah

    /**
     * Game ticks from a Jad-phase attack's cue (Zulrah glows in the style's colour) to its hit: TzTok-Jad's 3, so every
     * alternating attack can be read and prayed. Its 3-tick attack speed is unchanged, so the hits stay 36 MC apart.
     * // playability: OSRS shows only the projectile, which lands within its short hit delay
     */
    public static final int ZULRAH_JAD_LEAD_TICKS = 3;

    // ------------------------------------------------------------------ Cerberus

    /** OSRS: the first summoned soul attacks 6 game ticks after the souls appear, the others 2 ticks apart, west to east. */
    public static final int CERB_SOUL_FIRST_TICKS = 6;
    public static final int CERB_SOUL_GAP_TICKS = 2;
    /**
     * Each soul glows in its style's colour (and its place in the order is shown) this many game ticks before it attacks, so its
     * cue leads its hit by the 36 MC floor; the next soul starts glowing while the one before it is still due.
     * // playability: OSRS shows only the soul's attack animation on the tick it hits
     */
    public static final int CERB_SOUL_CUE_TICKS = 3;

    /** Game ticks after the summon at which soul {@code i} (0 west to 2 east) attacks (OSRS). */
    public static int soulAttackTick(int i) {
        return CERB_SOUL_FIRST_TICKS + i * CERB_SOUL_GAP_TICKS;
    }

    /** Game ticks after the summon at which soul {@code i}'s cue starts. */
    public static int soulCueTick(int i) {
        return soulAttackTick(i) - CERB_SOUL_CUE_TICKS;
    }

    /** OSRS: the triple attack fires magic, ranged, then the bite, 2 game ticks apart. */
    public static final int CERB_TRIPLE_GAP_TICKS = 2;

    // ------------------------------------------------------------------ Vardorvis

    /** OSRS: his head's gaze lands 3 game ticks after it appears; an awakened pair lands one tick apart. */
    public static final int VARDORVIS_GAZE_FLIGHT_TICKS = BossMechanics.VARDORVIS_HEAD_FLIGHT_TICKS;
    /**
     * A second gaze of the other style lands two game ticks (24 MC) after the first, so each can be prayed against.
     * // playability: OSRS one tick ("two actions in the span of one tick")
     */
    public static final int VARDORVIS_GAZE_SWITCH_GAP_TICKS = 2;

    /** When each gaze of a set lands, in game ticks after it appears ({@code true}: the blue magic gaze). */
    public static int[] gazeLandTicks(boolean[] magic) {
        int[] at = new int[magic.length];
        for (int i = 0; i < magic.length; i++) {
            at[i] = i == 0 ? VARDORVIS_GAZE_FLIGHT_TICKS : at[i - 1] + (magic[i] == magic[i - 1] ? 1 : VARDORVIS_GAZE_SWITCH_GAP_TICKS);
        }
        return at;
    }

    /**
     * Game ticks he holds his melee after a gaze set appears (OSRS: the flight, one tick per extra gaze, then 2), unchanged by
     * the spacing above: his attack cadence stays the OSRS one.
     */
    public static int gazeHoldTicks(int gazes) {
        return BossMechanics.VARDORVIS_HEAD_FLIGHT_TICKS + Math.max(0, gazes - 1) + BossMechanics.VARDORVIS_HEAD_AFTER_TICKS;
    }

    /**
     * Blocks a bleeding player must cover in one game tick, sprinting through all of it, for the bleed to hit every tick. Above
     * Minecraft's walking pace (4.317 blocks/s, 2.59 a game tick) and below its sprint (5.612, 3.37), so walking never counts.
     */
    public static final double BLEED_RUN_BLOCKS_PER_TICK = 2.8;

    /**
     * Whether a bleeding player ran this game tick, which makes the bleed hit every tick: sprinting both at the start and at the
     * end of the tick and covering a sprint's distance. A sprint tapped for a moment to dodge an axe, or toggled on while
     * standing, doesn't count. // playability: OSRS counts any tick the player runs (two tiles)
     */
    public static boolean bleedRunning(boolean sprintingBefore, boolean sprintingNow, double movedBlocks) {
        return sprintingBefore && sprintingNow && movedBlocks >= BLEED_RUN_BLOCKS_PER_TICK;
    }

    // ------------------------------------------------------------------ The Nightmare and Phosani's Nightmare

    /**
     * Game ticks from one of her standard attacks' wind-up (its cue) to its landing. OSRS: melee 2, magic and ranged 3. Real time:
     * never under the 3-tick floor; and in Phosani's phase three, where a protection prayer already up as the attack starts is
     * switched off (so the player prays only after reading the wind-up), melee 3 and magic/ranged 4.
     * // playability: OSRS melee 2 (and 2/3 in phase three)
     */
    public static int nightmareWindUp(boolean phosani, int phase, boolean desperate, boolean melee) {
        int osrs = melee ? NightmareRules.MELEE_WIND_UP : NightmareRules.RANGED_WIND_UP;
        int floor = STRIKE_FLOOR_TICKS;
        if (phosani && phase >= 2 && !desperate) {
            floor = melee ? PHOSANI_P3_MELEE_WIND_UP : PHOSANI_P3_RANGED_WIND_UP;
        }
        return Math.max(osrs, floor);
    }

    /** Phosani's phase-three wind-ups (game ticks): read the attack, then pray. */
    public static final int PHOSANI_P3_MELEE_WIND_UP = 3;
    public static final int PHOSANI_P3_RANGED_WIND_UP = 4;

    /**
     * Game ticks after the magic husk's attack that the ranged one attacks (each still every {@link NightmareRules#HUSK_SPEED}
     * ticks). // playability: OSRS one tick, a one-tick prayer flick
     */
    public static final int HUSK_RANGED_OFFSET_TICKS = 2;

    /** Whether husk {@code i} (0 magic, 1 ranged) attacks on the {@code huskTick}-th game tick of the bind (from 1). */
    public static boolean huskAttacks(int i, int huskTick) {
        int offset = i == 0 ? 0 : HUSK_RANGED_OFFSET_TICKS;
        int t = huskTick - 1 - offset;
        return t >= 0 && t % NightmareRules.HUSK_SPEED == 0;
    }

    // ------------------------------------------------------------------ Araxxor

    /**
     * Game ticks every tile of the acid ball's path waits beyond the OSRS travel, so the floor (3 ticks) doesn't make the first
     * seven tiles land together: the ball still sweeps out a tile group a tick. approx // playability: OSRS lands tile i after
     * 1 + i/3 ticks
     */
    public static final int ARAXXOR_ACID_BALL_SHIFT_TICKS = 2;

    /** Game ticks until the acid ball's path tile {@code i} (2-12 tiles out) lands. */
    public static int acidBallDelay(int tile) {
        return 1 + tile / 3 + ARAXXOR_ACID_BALL_SHIFT_TICKS;
    }

    /** Game ticks until the splatter at the end of an acid ball path {@code length} tiles long lands (after the path's last tile). */
    public static int acidBallSplatterDelay(int length) {
        return 2 + length / 3 + ARAXXOR_ACID_BALL_SHIFT_TICKS;
    }

    // ------------------------------------------------------------------ Nex

    /**
     * Containment's reach from Nex's centre (blocks): the 5x5 tiles around her 3x3 body (OSRS Wiki, Nex/Strategies), whose
     * edges are 2.5 tiles from her centre and corners 3.5. approx: a circle of 3.
     */
    public static final double NEX_CONTAINMENT_RADIUS = 3.0;

    /**
     * Containment and Ice Prison through Protect from Missiles: "praying Protect from Missiles will reduce the damage by half"
     * (OSRS Wiki, Nex/Strategies).
     */
    public static int nexIceDamage(int rolled, boolean protectFromMissiles) {
        return protectFromMissiles ? rolled / 2 : rolled;
    }

    // ------------------------------------------------------------------ Moons of Peril

    /**
     * After Eyatlalli's glyph moves on, standing off it doesn't count for this many game ticks: the player sees the new glyph
     * (now marked on the floor) and walks the ~3 blocks to it. // playability: OSRS counts from the next tick; the run there
     * takes 1-2 ticks
     */
    public static final int GLYPH_MOVE_GRACE_TICKS = 1;

    /** Whether being off the glyph on game tick {@code now} counts, the glyph having moved on game tick {@code movedAt}. */
    public static boolean offGlyphCounts(long now, long movedAt) {
        return now - movedAt > GLYPH_MOVE_GRACE_TICKS;
    }

    // ------------------------------------------------------------------ Doom of Mokhaiotl

    /**
     * The shockwave's countdown (a closing ring on the floor and the earthen shield's safe circle) starts this many game ticks
     * before the first shockwave lands, so the last call reaches the 36 MC floor. The volatile earth still rises 20 ticks before
     * (OSRS). // playability: OSRS shows the volatile earth only
     */
    public static final int DOOM_SHOCKWAVE_CUE_TICKS = 3;

    /** Whether the shockwave countdown shows on game tick {@code now} for shockwaves starting at {@code due}. */
    public static boolean shockwaveCountdown(long now, long due) {
        return due >= 0 && now <= due && due - now <= DOOM_SHOCKWAVE_CUE_TICKS;
    }

    // ------------------------------------------------------------------ Phantom Muspah

    /**
     * Game ticks from a spike's floor marker to its eruption: the strike floor. // playability: OSRS
     * {@link MuspahRules#SPIKE_ERUPT_TICKS} (2)
     */
    public static int muspahSpikeEruptTicks() {
        return Math.max(MuspahRules.SPIKE_ERUPT_TICKS, STRIKE_FLOOR_TICKS);
    }

    // ------------------------------------------------------------------ Vardorvis's swinging axes (round 43)

    /** Steps of an axe's path: nine 1.5-block steps, the player's spot the middle one. */
    public static final int AXE_PATH_STEPS = 9;
    /**
     * Path steps (1.5 blocks, three quarters of a tile) an axe sweeps a game tick. // approx: about 1.5 tiles a tick, from the
     * wiki's axe skip (an axe from the adjacent quadrant passes about 2 ticks after it swings, one from the opposite quadrant
     * about 5)
     */
    public static final int AXE_STEPS_PER_TICK = 2;

    /**
     * Game ticks from an axe set's swing to the axe reaching step {@code step} of its path (0: where it appears): the first on
     * the strike floor, each later one as the axe sweeps on. // playability: every step used to land on the same tick; OSRS
     * moves the axe across the arena tile by tile
     */
    public static int axeStepDelay(int step) {
        return STRIKE_FLOOR_TICKS + Math.max(0, Math.min(AXE_PATH_STEPS - 1, step)) / AXE_STEPS_PER_TICK;
    }
}
