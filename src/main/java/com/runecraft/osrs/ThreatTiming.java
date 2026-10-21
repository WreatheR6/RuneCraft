package com.runecraft.osrs;

import com.runecraft.osrs.content.AttackDef;

/**
 * Real-time delivery of monster attacks (round 41, CONTENT_GUIDE rule 5: keep OSRS numbers, adapt timing for real time).
 *
 * <p>OSRS resolves a monster's attack on a 0.6 s game tick: a melee hit lands on the tick of the swing, a projectile after its
 * hit delay, a telegraph a few ticks after the animation. Minecraft players move continuously and read cues in real time, so
 * every hit a monster or boss lands on a player follows these shared floors instead (all // playability):
 * <ul>
 *   <li>a telegraphed attack lands at least {@value #TELEGRAPH_FLOOR_MC} Minecraft ticks (1.8 s, Jad's 3 game ticks) after its
 *   first visible cue, its OSRS lead when that is longer;</li>
 *   <li>an untelegraphed ranged or magic attack is a travelling projectile judged on impact, flying
 *   clamp(distance / {@value #FLIGHT_BLOCKS_PER_MC}, 10, 24) MC (at least 16 from a boss), and attacks of different styles land at
 *   least {@value #STYLE_SPACING_MC} MC apart on one target (a volley is spread, never thinned); a projectile always lands before
 *   its caster's next attack starts;</li>
 *   <li>a melee attack winds up for 6, 8 or 10 MC by the monster's width before it resolves, and only lands when the target is still
 *   within reach (the bounding-box gap, not the 3D centre distance);</li>
 *   <li>ground hazards arm after {@value #HAZARD_ARM_MC} MC and then hit a player standing in them at most once per
 *   {@value #HAZARD_HIT_MC} MC, the OSRS once-a-tick rate.</li>
 * </ul>
 * Attack cadence is still stamped when an attack starts, so these delays move a hit later but never add or remove one: attacks per
 * minute, max hits, accuracy and prayer rules are unchanged.
 */
public final class ThreatTiming {
    /** Minecraft ticks per OSRS game tick. */
    public static final int MC_PER_GAME_TICK = 12;
    /** Shortest time (MC) from a telegraphed attack's first cue to its landing: Jad's 3 game ticks. */
    public static final int TELEGRAPH_FLOOR_MC = 36;
    /** A sky strike's telegraph in game ticks when its attack names none (unchanged from before round 41). */
    public static final int SKY_STRIKE_TICKS = 3;
    /** Projectile speed: blocks travelled per Minecraft tick. */
    public static final double FLIGHT_BLOCKS_PER_MC = 0.8;
    /** Shortest projectile flight (MC) from an ordinary monster. */
    public static final int FLIGHT_MIN_MC = 10;
    /** Shortest projectile flight (MC) from a boss. */
    public static final int FLIGHT_MIN_BOSS_MC = 16;
    /** Longest projectile flight (MC). */
    public static final int FLIGHT_MAX_MC = 24;
    /** Two hits of styles that need different protection prayers land at least this far apart (MC) on one target. */
    public static final int STYLE_SPACING_MC = 20;
    /** The style spacing never pushes a hit more than this (MC) past its natural landing, so a dense volley can't pile up. */
    public static final int MAX_SPACING_PUSH_MC = 20;
    /** Telegraph markers (halo, ground rings, hazard rings) are redrawn this often (MC). */
    public static final int REDRAW_MC = 4;
    /** A projectile's impact visual and sound play this long (MC) before it resolves. */
    public static final int IMPACT_LEAD_MC = 2;
    /** A telegraphed melee attack swings this long (MC) before it lands. */
    public static final int MELEE_SWING_LEAD_MC = 6;
    /** The rising warning note plays in the last this-many MC of a telegraph. */
    public static final int FINAL_NOTE_MC = 10;
    /** Melee reach: the largest horizontal gap (blocks) between the two bounding boxes (OSRS: an adjacent tile). */
    public static final double MELEE_REACH = 1.5;
    /** Largest height difference (blocks, feet to feet) a monster attacks across in melee. */
    public static final double MELEE_REACH_DY = 1.5;
    /** A melee monster keeps closing in until the gap is at most this (blocks). */
    public static final double CHASE_GAP = 0.8;
    /** A melee monster its path left short of its target walks the rest straight in from at most this gap (blocks). // approx */
    public static final double CLOSE_IN_MAX_GAP = 4.0;
    /** Extra gap (blocks) a wound-up melee hit still lands across (the target stepped back a little). */
    public static final double MELEE_LAND_SLACK = 1.0;
    /** Largest height difference (blocks) a wound-up melee hit still lands across. */
    public static final double MELEE_LAND_DY = 2.0;
    /** Extra distance (blocks) past an attack's range at which a projectile or telegraphed attack still lands. */
    public static final double RANGE_SLACK = 4.0;
    /** A sky strike hits within this horizontal radius (blocks) of the marked spot. */
    public static final double SKY_STRIKE_RADIUS = 1.5;
    /** Area tests (sky strikes, AoE rings) are horizontal; a player this far (blocks) above or below the ring is outside it. */
    public static final double AREA_DY = 2.0;
    /** Ground hazards hurt nobody for this long (MC) after they are placed. */
    public static final int HAZARD_ARM_MC = 12;
    /** A hazard hits one player at most once per this many MC (OSRS: once a game tick). */
    public static final int HAZARD_HIT_MC = 12;
    /** Hazard contact: horizontal distance (blocks) from its centre. */
    public static final double HAZARD_RADIUS = 1.0;
    /** Hazard contact: largest height difference (blocks). */
    public static final double HAZARD_DY = 1.5;
    /** A monster's first hit on a newly engaged target, or on one that just came into melee reach, lands at least this late (MC). */
    public static final int ENGAGE_MIN_MC = 10;
    /** "Never hit yet" marker for {@link #hazardShouldHit}. */
    public static final long NEVER = Long.MIN_VALUE;

    private ThreatTiming() {
    }

    /** Whether an attack lands after a telegraph (its own lead, or a sky strike's marker). */
    public static boolean telegraphed(int telegraphTicks, AttackDef.Special special) {
        return telegraphTicks > 0 || special == AttackDef.Special.SKY_STRIKE;
    }

    /**
     * Minecraft ticks from a telegraphed attack's first cue to its landing: its OSRS telegraph (a sky strike's default
     * {@value #SKY_STRIKE_TICKS} ticks), but never under {@value #TELEGRAPH_FLOOR_MC}. 0 for an untelegraphed attack.
     */
    public static int leadMc(int telegraphTicks, AttackDef.Special special) {
        if (!telegraphed(telegraphTicks, special)) {
            return 0;
        }
        int ticks = Math.max(telegraphTicks, special == AttackDef.Special.SKY_STRIKE ? SKY_STRIKE_TICKS : 0);
        return Math.max(ticks * MC_PER_GAME_TICK, TELEGRAPH_FLOOR_MC); // playability: OSRS lands telegraphs after 1-2 ticks
    }

    /** Projectile flight (MC) over {@code distance} blocks: clamp(distance / 0.8, 10 or 16 for a boss, 24). */
    public static int flightMc(double distance, boolean boss) {
        long mc = Math.round(Math.max(0, distance) / FLIGHT_BLOCKS_PER_MC);
        return (int) Math.max(boss ? FLIGHT_MIN_BOSS_MC : FLIGHT_MIN_MC, Math.min(FLIGHT_MAX_MC, mc));
    }

    /**
     * The latest a projectile may land after its launch for a caster attacking every {@code periodMc} MC: 2 MC before its next
     * attack starts (never under {@value #FLIGHT_MIN_MC}), so one monster's hits resolve in the order it threw them (the scripts'
     * on-hit hooks read the attack they last made).
     */
    public static long latestLandingMc(int periodMc) {
        return Math.max(FLIGHT_MIN_MC, periodMc - 2L);
    }

    /** The protection prayer group of a style: 0 melee, 1 ranged, 2 magic. */
    public static int prayerGroup(AttackType style) {
        if (style == null || style.isMelee()) {
            return 0;
        }
        return style == AttackType.RANGED ? 1 : 2;
    }

    /** Whether one protection prayer blocks both styles. */
    public static boolean samePrayer(AttackType a, AttackType b) {
        return prayerGroup(a) == prayerGroup(b);
    }

    /**
     * A hit's landing tick after another hit on the same target: a style that needs a different protection prayer landing within
     * {@value #STYLE_SPACING_MC} MC of it (either side) is pushed to {@code prevDue + 20}; the same prayer group keeps its tick.
     */
    public static long styleSpacedDue(long prevDue, AttackType prevStyle, long due, AttackType style) {
        if (samePrayer(prevStyle, style) || Math.abs(due - prevDue) >= STYLE_SPACING_MC) {
            return due;
        }
        return prevDue + STYLE_SPACING_MC;
    }

    /**
     * A new hit's landing tick against every hit already scheduled on its target ({@code dues[i]} with {@code styles[i]}), checked in
     * landing order, pushed at most {@value #MAX_SPACING_PUSH_MC} MC past {@code natural}.
     */
    public static long spacedDue(long natural, AttackType style, long[] dues, AttackType[] styles) {
        Integer[] order = new Integer[dues.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> Long.compare(dues[a], dues[b]));
        long due = natural;
        for (int i : order) {
            due = styleSpacedDue(dues[i], styles[i], due, style);
        }
        return Math.min(due, natural + MAX_SPACING_PUSH_MC);
    }

    /** A melee wind-up (MC) by the monster's width: 6, 8 from 1.5 blocks wide, 10 from 3 (a big swing is slower to land). */
    public static int meleeWindupMc(double width) {
        return width >= 3 ? 10 : width >= 1.5 ? 8 : 6;
    }

    /** The horizontal gap (blocks) between two bounding boxes {@code w1} and {@code w2} wide whose centres are dx, dz apart. */
    public static double gap(double dx, double dz, double w1, double w2) {
        return Math.max(0, Math.sqrt(dx * dx + dz * dz) - w1 / 2 - w2 / 2);
    }

    /**
     * {@link #gap} for two bounding boxes whose feet are {@code sqrt(distanceSq)} apart, {@code dy} of it in height (a squared
     * distance shorter than the height difference is a gap of 0).
     */
    public static double gapAcross(double distanceSq, double dy, double w1, double w2) {
        return Math.max(0, Math.sqrt(Math.max(0, distanceSq - dy * dy)) - w1 / 2 - w2 / 2);
    }

    /** Whether a monster can start a melee attack across this gap and height difference. */
    public static boolean inMeleeReach(double gap, double dy) {
        return gap <= MELEE_REACH && Math.abs(dy) <= MELEE_REACH_DY;
    }

    /** Whether a melee monster should keep walking at its target. */
    public static boolean keepChasing(double gap) {
        return gap > CHASE_GAP;
    }

    /**
     * Whether {@code attack} can only be started in melee reach: a melee-styled attack, except the specials made to reach a target
     * from range, which {@code Mechanics} delivers anywhere in their range: BLINK (the monster appears beside its target before the
     * hit: the abyssal demon's teleport, which makes "safespotting not viable"), SKY_STRIKE (it lands on the target's tile: the
     * lizardman shaman's jump "targets a small area where the player was previously standing", Brutus's charge) and PULL (the
     * target is dragged beside the monster: the Fragment of Seren's "Come closer!"). The attack goal starts the others only in melee
     * reach, and walks in for every melee-styled one.
     */
    public static boolean needsMeleeReach(AttackDef attack) {
        AttackDef.Special sp = attack.special();
        return attack.melee() && sp != AttackDef.Special.BLINK && sp != AttackDef.Special.SKY_STRIKE && sp != AttackDef.Special.PULL;
    }

    /**
     * Whether a monster's attack goal can start {@code attack} now, {@code gap} blocks from its target: one that needs melee reach
     * ({@link #needsMeleeReach}) only in melee reach ({@code inMeleeReach}: {@link #inMeleeReach} on the horizontal gap and height
     * difference, where the goal starts it), whatever its nominal range; any other attack within its range. OSRS: an NPC uses its
     * melee attack only from a tile next to its target and attacks from further away only with ranged, magic or a special made for
     * it (Skotizo "does not approach to use melee attacks").
     */
    public static boolean usable(AttackDef attack, double gap, boolean inMeleeReach) {
        return needsMeleeReach(attack) ? inMeleeReach : gap <= attack.range();
    }

    /** Whether every one of {@code attacks} is melee-styled (a creature with nothing to cast or shoot). */
    public static boolean meleeOnly(java.util.List<AttackDef> attacks) {
        for (AttackDef a : attacks) {
            if (!a.melee()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a monster's attack goal may roll {@code attack} now: one it can start from here ({@link #usable}) or, for a melee-only
     * creature ({@code meleeOnly}, {@link #meleeOnly}) out of melee reach, a melee attack it walks in for. So a melee creature's
     * specials made for range are rolled against its melee attacks by weight, as in OSRS (Brutus charges about one attack in five,
     * an abyssal demon now and then teleports beside a player standing back), not made on every attack while its target stays out
     * of reach; a creature that also casts or shoots attacks from range with those and its specials.
     */
    public static boolean pickable(AttackDef attack, double gap, boolean inMeleeReach, boolean meleeOnly) {
        return usable(attack, gap, inMeleeReach) || meleeOnly && needsMeleeReach(attack);
    }

    /**
     * Whether a monster keeps the attack it rolled ({@code kept}, one of {@code attacks}) until it makes it: while it may still roll
     * it ({@link #pickable}), or while it may roll none of them and {@code kept} is still within its nominal range (the fallback
     * roll by nominal range it walks in with). // playability: the attack goal asks every Minecraft tick, and rolling afresh every
     * tick made a mixed creature stop and start walking and churned the fallback among the attacks in range.
     */
    public static boolean keeps(AttackDef kept, java.util.List<AttackDef> attacks, double gap, boolean inMeleeReach) {
        boolean meleeOnly = meleeOnly(attacks);
        if (pickable(kept, gap, inMeleeReach, meleeOnly)) {
            return true;
        }
        if (gap > kept.range()) {
            return false;
        }
        for (AttackDef a : attacks) {
            if (pickable(a, gap, inMeleeReach, meleeOnly)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a melee monster whose path has reached its target's block should walk the rest straight at it: still farther than
     * {@value #CHASE_GAP} (it keeps closing in), at most {@value #CLOSE_IN_MAX_GAP} and on its level. // playability: Minecraft's path
     * follower counts a node reached within half the mob's width of it, and a mob two or more blocks wide stands on the corner of
     * its node, so a 2-3 block wide boss coming from the east or south "arrived" 1.6-2 blocks from its target, out of melee reach,
     * and every new path ended there again (OSRS: it walks onto the tile next to its target).
     */
    public static boolean closeInDirectly(double gap, double dy) {
        return keepChasing(gap) && gap <= CLOSE_IN_MAX_GAP && Math.abs(dy) <= MELEE_REACH_DY;
    }

    /** Whether a wound-up melee hit of reach {@code reach} (a gap) lands on a target that is now this far away. */
    public static boolean meleeLands(double gap, double dy, double reach) {
        return gap <= reach + MELEE_LAND_SLACK && Math.abs(dy) <= MELEE_LAND_DY;
    }

    /** Whether a projectile or telegraphed attack of range {@code range} still reaches a target {@code distance} away. */
    public static boolean inRange(double distance, double range) {
        return distance <= range + RANGE_SLACK;
    }

    /** Whether a player this far from a sky strike's marked spot (horizontally and in height) is hit. */
    public static boolean inSkyStrike(double horizontal, double dy) {
        return horizontal <= SKY_STRIKE_RADIUS && Math.abs(dy) < AREA_DY;
    }

    /** Whether a player this far from an area attack's centre is inside its ring of {@code radius}. */
    public static boolean inArea(double horizontal, double dy, double radius) {
        return horizontal <= radius && Math.abs(dy) < AREA_DY;
    }

    /** Whether a player this far from a hazard's centre stands in it. */
    public static boolean hazardContact(double horizontal, double dy) {
        return horizontal < HAZARD_RADIUS && Math.abs(dy) < HAZARD_DY;
    }

    /** Whether an armed hazard may hit a player now, who it last hit at {@code lastHitMc} ({@link #NEVER} for never). */
    public static boolean hazardShouldHit(long lastHitMc, long nowMc, long armAtMc) {
        return nowMc >= armAtMc && (lastHitMc == NEVER || nowMc - lastHitMc >= HAZARD_HIT_MC);
    }

    /**
     * The "last attack" a monster's attack goal stamps when it engages a new target: its next attack is ready
     * max({@value #ENGAGE_MIN_MC}, wind-up) MC later, so it never hits on the tick it picks a target or arrives.
     */
    public static long engageLastAttack(long nowMc, int speedTicks, int windupMc) {
        return nowMc - (long) speedTicks * MC_PER_GAME_TICK + Math.max(ENGAGE_MIN_MC, windupMc);
    }

    /**
     * A monster's "last attack" after it engages a new target: the engage floor only ever postpones its next attack, never brings
     * it forward (a boss switching targets right after an attack still waits its full attack speed).
     */
    public static long engage(long lastAttackMc, long nowMc, int speedTicks, int windupMc) {
        return Math.max(lastAttackMc, engageLastAttack(nowMc, speedTicks, windupMc));
    }

    /** The earliest launch after a melee monster came into reach at {@code arrivedMc}, so its hit lands at least 10 MC later. */
    public static long arrivalReadyMc(long arrivedMc, int windupMc) {
        return arrivedMc + Math.max(0, ENGAGE_MIN_MC - windupMc);
    }

    /** Whether an attack whose cadence is stamped at launch may start now. */
    public static boolean cadenceReady(long nowMc, long lastLaunchMc, int speedTicks) {
        return nowMc - lastLaunchMc >= (long) speedTicks * MC_PER_GAME_TICK;
    }

    /** Whether a marker created at {@code createdMc} is redrawn at {@code nowMc}. */
    public static boolean redraw(long createdMc, long nowMc) {
        return nowMc >= createdMc && (nowMc - createdMc) % REDRAW_MC == 0;
    }

    /** The rising warning note's pitch at {@code nowMc} of a telegraph landing at {@code dueMc}, or 0 when none plays. */
    public static float finalNotePitch(long nowMc, long dueMc) {
        long left = dueMc - nowMc;
        if (left <= 0 || left > FINAL_NOTE_MC || left % 3 != 0) {
            return 0f;
        }
        return 1.0f + (FINAL_NOTE_MC - left) * 0.1f;
    }

    /**
     * A small simulator for the cadence tests: a monster whose cadence is stamped at launch starts an attack every
     * {@code speedTicks} game ticks from MC 0, each landing {@code delayMc[i % delayMc.length]} MC later. Returns {launched within
     * [0, windowMc], landed within [0, windowMc + 44]} for the attacks launched in the window.
     */
    public static int[] simulate(int speedTicks, int windowMc, int[] delayMc) {
        long last = Long.MIN_VALUE / 4;
        int launched = 0;
        int landed = 0;
        for (long now = 0; now <= windowMc; now++) {
            if (cadenceReady(now, last, speedTicks)) {
                last = now;
                long due = now + delayMc[launched % delayMc.length];
                launched++;
                if (due <= windowMc + FLIGHT_MAX_MC + MAX_SPACING_PUSH_MC) {
                    landed++;
                }
            }
        }
        return new int[]{launched, landed};
    }
}
