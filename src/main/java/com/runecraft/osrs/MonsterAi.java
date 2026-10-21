package com.runecraft.osrs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Open-world monster AI rules (round 42): who a monster attacks unprovoked, how far it hunts and chases from its spawn point,
 * when it switches target, how long a single-way lock holds against a monster that wandered off, and who gets the kill.
 * Distances are in blocks (one OSRS tile is two blocks), times in Minecraft ticks (MC, 1/20 s) unless named in game ticks.
 *
 * <p>OSRS (wiki, Aggressiveness and Retreat): an aggressive monster attacks a player whose combat level is at most twice its own
 * (any level for bosses and most Wilderness monsters), searching its <i>hunt range</i> around itself; it can only attack a player
 * standing inside its <i>aggression range</i>, its max range (almost always 7 tiles from its static spawn point) plus its attack
 * range; after 10 minutes in the same area it becomes tolerant (see {@link CombatBonuses.Tolerance}). Real-time adaptations are
 * marked {@code // playability}.
 */
public final class MonsterAi {
    /** One OSRS tile in blocks. */
    public static final double BLOCKS_PER_TILE = 2.0;
    /**
     * How far an open-world aggressive monster looks for a player to attack. // approx: the wiki gives no per-monster hunt ranges
     * (only that jungle horrors hunt from many squares away and mountain trolls on a small range); 5 tiles.
     */
    public static final double HUNT_BLOCKS = 10.0;
    /** The hunt range of encounter monsters (bosses, raids, minigames, quest spawns): the old follow range, so arenas keep working. */
    public static final double ENCOUNTER_HUNT_BLOCKS = 24.0;
    /** OSRS max range: a monster's static spawn point plus 7 tiles. */
    public static final double MAX_RANGE_BLOCKS = 7 * BLOCKS_PER_TILE;
    /** A melee attack range of one tile. */
    public static final double MELEE_ATTACK_RANGE_BLOCKS = BLOCKS_PER_TILE;
    /**
     * How far from its spawn point a monster gives up a chase and walks home (// playability: OSRS confines it to its 7-tile max
     * range, 14 blocks; a real-time chase needs some slack, so 20).
     */
    public static final double LEASH_BLOCKS = 20.0;
    /** Slack (blocks) beyond the aggression range a fleeing target may reach before the monster gives up. */
    public static final double GIVE_UP_SLACK_BLOCKS = 4.0;
    /** A returning monster is home (and back to full health) within this distance of its spawn point. */
    public static final double HOME_BLOCKS = 3.0;
    /** A monster still not home after this long is put back on its spawn point (stuck on terrain). */
    public static final int RETURN_TIMEOUT_MC = 200;
    /**
     * A monster that may not attack its target under the single-way rules drops it after this long (// playability: OSRS monsters
     * stand next to a player who is fighting something else; hovering at the player's side reads as a bug in real time).
     */
    public static final int BLOCKED_DROP_MC = 20;
    /** After dropping a blocked target the monster won't pick that player again for this long. */
    public static final int BLOCKED_FORGET_MC = 60;
    /**
     * A monster keeps its single-way lock on a player only while it is still after them and within this distance (or within its own
     * attack range) with a clear line of sight (// playability: OSRS holds the lock for 8 game ticks after the last hit wherever the
     * monster is; a monster that wandered off or went home no longer stops the player fighting something else).
     */
    public static final double ENGAGED_LOCK_BLOCKS = 6.0;
    /** How often (MC) an idle aggressive monster looks for a player to attack. */
    public static final int HUNT_SCAN_MC = 10;
    /** How long (MC) a target may stay out of reach before an attacker the monster can reach takes over. */
    public static final int UNREACHABLE_MC = 60;
    /**
     * A monster counts as under fire from its target while that target hit (or missed) it within this long: it doesn't give up a
     * target that keeps attacking it from beyond its range, and doesn't heal on the way home (5 s, // playability: OSRS has no
     * reset and regenerates a monster 1 hitpoint a minute).
     */
    public static final int UNDER_FIRE_MC = 100;
    /** Drops stay the winner's alone for 60 seconds (wiki, Drops: invisible to other players for the first 100 game ticks). */
    public static final int PRIVATE_LOOT_MC = 60 * 20;
    /** Tradeable drops disappear 2 minutes after the kill (wiki, Drops: they remain on the ground for 200 game ticks). */
    public static final int TRADEABLE_LOOT_LIFESPAN_MC = 120 * 20;
    /** Untradeable drops disappear 3 minutes after the kill (wiki, Drops: 300 game ticks). */
    public static final int UNTRADEABLE_LOOT_LIFESPAN_MC = 180 * 20;

    /**
     * Monsters that never become tolerant (wiki, Tolerance: Exceptions). Bosses ignore tolerance too, and the God Wars Dungeon's
     * followers are never tolerant of a player without their god's items (a module rule calms them otherwise). // approx:
     * Vyrewatch Sentinels tolerate a player in vyre noble clothing, which isn't checked.
     */
    public static final Set<String> NEVER_TOLERANT = Set.of("dark_beast", "lizardman_shaman", "vyrewatch_sentinel",
            "warped_terrorbird", "gorak");

    /** Hunt ranges the wiki describes (// approx: no tile counts given). */
    private static final Map<String, Double> HUNT = Map.of(
            "jungle_horror", 16.0, // "will seek you out from many game squares away"
            "mountain_troll", 6.0); // "aggressive on a smaller range"

    private MonsterAi() {
    }

    /** Hunt range (blocks) of an open-world creature. */
    public static double huntBlocks(String creatureId) {
        Double d = creatureId == null ? null : HUNT.get(creatureId);
        return d != null ? d : HUNT_BLOCKS;
    }

    /**
     * OSRS aggression by combat level: a player above twice the monster's level is ignored, unless the monster is aggressive
     * regardless of level (bosses, most Wilderness monsters). Monsters of level 63 and above attack every player.
     */
    public static boolean aggressiveTo(int monsterLevel, int playerCombat, boolean regardlessOfLevel) {
        return regardlessOfLevel || CombatBonuses.aggressiveTo(monsterLevel, playerCombat);
    }

    /**
     * The aggression range (blocks from the monster's spawn point): its 7-tile max range plus its attack range (at least the one-tile
     * melee range).
     */
    public static double aggressionRange(double attackReach) {
        return MAX_RANGE_BLOCKS + Math.max(MELEE_ATTACK_RANGE_BLOCKS, attackReach);
    }

    /** Whether a player at {@code fromHome} blocks (horizontally) from the spawn point may be picked as a new target. */
    public static boolean inAggressionRange(double fromHome, double attackReach) {
        return fromHome <= aggressionRange(attackReach);
    }

    /**
     * Whether a chasing monster gives up and walks home: it is past its leash, or its target fled beyond the aggression range plus a
     * little slack and isn't attacking it ({@code underFire}: the target hit it within {@link #UNDER_FIRE_MC}). A target that keeps
     * attacking from beyond the range is held at the edge instead ({@link #holdAtEdge}): OSRS lets a player safespot a monster at
     * its max range, never resets it.
     */
    public static boolean giveUp(double mobFromHome, double targetFromHome, double attackReach, boolean underFire) {
        return mobFromHome > LEASH_BLOCKS || !underFire && targetFromHome > aggressionRange(attackReach) + GIVE_UP_SLACK_BLOCKS;
    }

    /**
     * Whether a monster under fire from a target beyond its range (and the slack) stands at the edge of its max range instead of
     * walking on: it is at least {@link #MAX_RANGE_BLOCKS} from its spawn point and the way to its target leads further out
     * ({@code outward}). It still attacks if its target is within its own attack range (OSRS: a monster stops at its max range;
     * the player can attack it from beyond, and it hits back only if it can reach them from there).
     */
    public static boolean holdAtEdge(double mobFromHome, double targetFromHome, double attackReach, boolean underFire,
            boolean outward) {
        return underFire && outward && mobFromHome >= MAX_RANGE_BLOCKS
                && targetFromHome > aggressionRange(attackReach) + GIVE_UP_SLACK_BLOCKS;
    }

    /**
     * Whether a monster back at its spawn point regains full health: only once nothing has hit it for {@link #UNDER_FIRE_MC}
     * ({@code sinceHitMc}; // playability: a reset heal stands in for OSRS's 1 hitpoint a minute, and must not erase hits that are
     * still coming).
     */
    public static boolean healsAtHome(long sinceHitMc) {
        return sinceHitMc >= UNDER_FIRE_MC;
    }

    /**
     * Retaliation stickiness: a monster hit by someone other than its target switches only when it has no target, its target is no
     * longer valid (dead, gone, another level, out of range), or its target is out of reach while the new attacker isn't
     * (// playability: vanilla's HurtByTargetGoal switched to every new attacker, so two players flip-flopped it).
     */
    public static boolean switchTarget(boolean hasTarget, boolean targetValid, boolean targetReachable, boolean attackerReachable) {
        return !hasTarget || !targetValid || !targetReachable && attackerReachable;
    }

    /**
     * Whether a monster still holds a single-way lock on the fighter it was engaged with: it is still after them, near enough to fight
     * them (within {@link #ENGAGED_LOCK_BLOCKS} or its own attack range) and in sight.
     */
    public static boolean stillEngaged(boolean targetsFighter, double distance, double attackReach, boolean lineOfSight) {
        return targetsFighter && lineOfSight && distance <= Math.max(ENGAGED_LOCK_BLOCKS, attackReach);
    }

    /**
     * Whether a monster's drop is still the winner's alone: OSRS hides it from other players for 60 s, in the Wilderness too (wiki,
     * Drops: only an item a player drops there by hand shows at once); an untradeable drop never shows for anyone else.
     */
    public static boolean lootPrivate(long ageMc, boolean tradeable) {
        if (!tradeable) {
            return true; // untradeable drops never show for anyone else (OSRS)
        }
        return ageMc < PRIVATE_LOOT_MC;
    }

    /** How long (MC) a monster's drop lies on the ground before it disappears. */
    public static int lootLifespanMc(boolean tradeable) {
        return tradeable ? TRADEABLE_LOOT_LIFESPAN_MC : UNTRADEABLE_LOOT_LIFESPAN_MC;
    }

    /**
     * Damage dealt to one monster by each attacker, for kill credit: OSRS gives the drop (and the kill) to whoever dealt the most
     * damage. Ties go to whoever hit first (// approx: the wiki doesn't say).
     */
    public static final class DamageLedger<K> {
        private final LinkedHashMap<K, Long> dealt = new LinkedHashMap<>();

        public void add(K who, int damage) {
            if (who != null && damage > 0) {
                dealt.merge(who, (long) damage, Long::sum);
            }
        }

        /** Whoever dealt the most damage (the earliest on a tie), or null. */
        public K top() {
            K best = null;
            long most = 0;
            for (Map.Entry<K, Long> e : dealt.entrySet()) {
                if (e.getValue() > most) {
                    most = e.getValue();
                    best = e.getKey();
                }
            }
            return best;
        }

        public long of(K who) {
            return dealt.getOrDefault(who, 0L);
        }

        public long total() {
            long t = 0;
            for (long v : dealt.values()) {
                t += v;
            }
            return t;
        }

        public Map<K, Long> entries() {
            return java.util.Collections.unmodifiableMap(dealt);
        }
    }
}
