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
    /** A returning monster is home within this distance of its spawn point (it keeps its damage: see {@link #REGEN_TICKS}). */
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
     * target that keeps attacking it from beyond its range (5 s, // playability: OSRS has no reset at all).
     */
    public static final int UNDER_FIRE_MC = 100;
    /**
     * Game ticks per hitpoint a monster regenerates: the default NPC rate, 1 hitpoint every 100 game ticks (one minute; OSRS Wiki,
     * Ice demon, quoting Mod Ash, 9 July 2026: "Otherwise default should be 100, as for outside CoX"). Round 43 replaced round 42's
     * full heal at home with it: a monster that gave up a chase keeps its damage, as in OSRS, so a player who ran off to eat comes
     * back to the fight they left.
     */
    public static final int REGEN_TICKS = 100;
    /** {@link #REGEN_TICKS} in Minecraft ticks. */
    public static final int REGEN_MC = REGEN_TICKS * 12;
    /**
     * An encounter monster (a boss's or quest's summon, a clue guardian, a Slayer superior, a Barrows tunnel monster: no spawn point)
     * drops a target further than this from where it appeared, or from its boss's lair, and doesn't pick one there (// playability:
     * OSRS keeps them in their room or instance; round 42 let them chase 48 blocks). The boss lair's presence range.
     */
    public static final double ENCOUNTER_LEASH_BLOCKS = 32.0;
    /** How often (MC) a monster after a player checks it can still walk to them (the single-way lock and the blocked path). */
    public static final int REACH_CHECK_MC = 10;
    /**
     * A monster that has had no way to its target for this long (a wall, a fence, water between them, even with a clear line of
     * sight) no longer holds the single-way lock on them (// playability: OSRS holds it 8 game ticks after the last hit).
     */
    public static final int PATH_BLOCKED_MC = 20;
    /**
     * Monsters that retreat when their hitpoints run low: the OSRS Wiki's "Monsters that retreat when low hitpoints" (July 2026)
     * that have a RuneCraft counterpart (keys as {@link com.runecraft.osrs.data.MonsterTraits}), locked by {@code MonsterAiTest}
     * against {@code src/test/resources/osrs/monster_retreat.tsv}. The imp is on the list too (the wiki's Retreat page also says it
     * retreats "at any amount of health": only the low-hitpoint retreat is modelled).
     */
    public static final Set<String> RETREATS = Set.of("Monster.CHICKEN", "Monster.GHOUL", "duck", "giant_rock_crab", "gwd_imp",
            "gwd_werewolf", "imp", "seagull", "wallasalki", "rooster");
    /** How far (blocks) a retreating monster tries to get from the player each time it moves off. */
    public static final double RETREAT_STEP_BLOCKS = 8.0;
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

    /** Whether a monster that has lived {@code ageMc} Minecraft ticks regenerates a hitpoint now ({@link #REGEN_TICKS}). */
    public static boolean regenDue(long ageMc) {
        return ageMc > 0 && ageMc % REGEN_MC == 0;
    }

    /** Health after one regeneration step: one hitpoint more, never above {@code max}. */
    public static float regenerated(float health, float max) {
        return Math.min(max, health + 1);
    }

    /** Whether a creature (its {@link com.runecraft.osrs.data.MonsterTraits} key) retreats at low hitpoints ({@link #RETREATS}). */
    public static boolean retreats(String key) {
        return key != null && RETREATS.contains(key);
    }

    /**
     * The hitpoints at or below which a retreating monster runs: a tenth of its hitpoints, at least 1 (OSRS Wiki, Chicken: "Chickens
     * will sometimes try to retreat when they have 1 hitpoint left", 1 of their 3; // approx for the others: the wiki only says
     * "low" or "critically low").
     */
    public static int retreatAtHp(int maxHp) {
        return Math.max(1, maxHp / 10);
    }

    /** Whether a monster with {@code health} of {@code maxHealth} hitpoints is low enough to retreat. */
    public static boolean lowEnoughToRetreat(double health, double maxHealth) {
        return health > 0 && health <= retreatAtHp((int) Math.round(maxHealth));
    }

    /** Whether a point {@code fromAnchor} blocks (horizontally) from an encounter monster's anchor is outside its arena. */
    public static boolean outsideArena(double fromAnchor) {
        return fromAnchor > ENCOUNTER_LEASH_BLOCKS;
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
     * {@link #stillEngaged(boolean, double, double, boolean)} that also lets go when the monster can't get at the fighter: no path
     * to them for {@link #PATH_BLOCKED_MC} and out of its attack range ({@code canReach} false; round 43: a melee monster behind a
     * fence or across water could see the player and kept the lock, so the player couldn't fight anything else).
     */
    public static boolean stillEngaged(boolean targetsFighter, double distance, double attackReach, boolean lineOfSight,
            boolean canReach) {
        return canReach && stillEngaged(targetsFighter, distance, attackReach, lineOfSight);
    }

    /** Whether a monster whose path to its target has been blocked since {@code blockedSinceMc} (-1: not blocked) has given up on it. */
    public static boolean pathBlocked(long blockedSinceMc, long nowMc) {
        return blockedSinceMc >= 0 && nowMc - blockedSinceMc >= PATH_BLOCKED_MC;
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

    /** Whether a monster this wide (blocks) takes up more than one tile, so its drops land on its south-westernmost tile. */
    public static boolean dropsInSouthWestTile(double widthBlocks) {
        return widthBlocks > BLOCKS_PER_TILE + 0.5;
    }

    /**
     * The centre of the south-westernmost tile of a monster whose bounding box starts at {@code minX} (west edge) and ends at
     * {@code maxZ} (south edge: Minecraft's south is +z), as {x, z} (wiki, Drops: a large monster's drops land there).
     */
    public static double[] southWestTileCentre(double minX, double maxZ) {
        return new double[]{minX + BLOCKS_PER_TILE / 2, maxZ - BLOCKS_PER_TILE / 2};
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
