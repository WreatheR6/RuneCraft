package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Tombs of Amascut reward points and the chest (OSRS wiki "Chest (Tombs of Amascut)" and "Module:Tombs of Amascut loot"):
 * every raider starts on 5,000 reward points; damage earns room points (capped at 20,000 a room) that join the total
 * (capped at 64,000) when the room is cleared, the room's MVP adds 300 x team size, and a death costs 20% of the total
 * (at least 1,000). The starting 5,000 are taken off again for the loot. One purple roll per raid; an item rolled below
 * its raid level only survives a further 1/50 roll. Fewer than 1,500 points earn fossilised dung instead of loot.
 * Tertiaries (thread of Elidinis, the four keris partisan jewels) have bad luck mitigation; deathless raids at high raid
 * levels guarantee the challenge rewards.
 */
public final class ToaLoot {
    private ToaLoot() {
    }

    /** Reward points every raider starts with (taken off again for the loot). */
    public static final int START_POINTS = 5_000;
    /** Room points a raider can earn in one room. */
    public static final int ROOM_POINTS_CAP = 20_000;
    /** Total reward points cap (OSRS: 64,000). */
    public static final int TOTAL_POINTS_CAP = RaidScaling.TOA_POINTS_CAP;
    /** The room's MVP (most room points) gets this times the team size. */
    public static final int MVP_POINTS_PER_MEMBER = 300;
    /** A death costs 20% of the total, but never less than this. */
    public static final int DEATH_MIN_LOSS = 1_000;
    /** Raiders with fewer reward points than this find fossilised dung instead of loot. */
    public static final int DUNG_BELOW = 1_500;
    /** Room points a Path of Scabaras puzzle gives everyone in the room. // approx: Module:Tombs of Amascut loot assumes 3 x 100 */
    public static final int SCABARAS_PUZZLE_POINTS = 100;
    /** Room points for every Path of Apmeken problem fixed in time (Module:Tombs of Amascut loot: 75 a cycle). */
    public static final int APMEKEN_FIX_POINTS = 75;
    /** Room points per point of growth given to the Palm of Resourcefulness (Module:Tombs of Amascut loot: 2). */
    public static final int CRONDIS_POINTS_PER_GROWTH = 2;
    /** Room points per damage to Het's seal (OSRS: x2.5). */
    public static final double HET_SEAL_POINTS = 2.5;

    /**
     * Combat Achievement flags the challenge rooms report on a perfect clear (checked when the raid completes, see
     * CombatAchievementRules): Perfect Scabaras (under a minute, no puzzle damage), Perfect Het (no beam or orb damage, the
     * seal broken in one exposure), Perfect Crondis (no crocodile at the tree, no water spilled, under a minute) and
     * Perfect Apmeken (a group, no danger triggered, no venom, no volatile baboon hit, under three minutes).
     */
    public static final String FLAG_PERFECT_SCABARAS = "toa_perfect_scabaras";
    public static final String FLAG_PERFECT_HET = "toa_perfect_het";
    public static final String FLAG_PERFECT_CRONDIS = "toa_perfect_crondis";
    public static final String FLAG_PERFECT_APMEKEN = "toa_perfect_apmeken";
    /** Perfect boss kills (the fight's damage conditions plus the room's), for the "Perfection of ..." tasks. */
    public static final String FLAG_PERFECT_AKKHA = "toa_perfect_akkha";
    public static final String FLAG_PERFECT_BABA = "toa_perfect_baba";
    public static final String FLAG_PERFECT_ZEBAK = "toa_perfect_zebak";
    public static final String FLAG_PERFECT_KEPHRI = "toa_perfect_kephri";
    /** Fight flags that fail a task: a trailing orb dropped (Dropped the ball), a cracked boulder hit (No skipping allowed),
     * Kephri healed above 25% after the first down (All out of medics). */
    public static final String FLAG_AKKHA_ORB_DROPPED = "akkha_orb_dropped";
    public static final String FLAG_BABA_CRACKED_HIT = "baba_cracked_hit";
    public static final String FLAG_KEPHRI_OVER_25 = "kephri_over_25";
    /** Game ticks the timed perfect rooms allow: a minute (Scabaras, Crondis), three minutes (Apmeken). */
    public static final int PERFECT_SCABARAS_TICKS = 100;
    public static final int PERFECT_CRONDIS_TICKS = 100;
    public static final int PERFECT_APMEKEN_TICKS = 300;

    /** The keris partisan jewels. */
    public static final List<String> JEWELS = List.of("eye_of_the_corruptor", "jewel_of_the_sun", "breach_of_the_scarab",
            "jewel_of_amascut");
    public static final String THREAD = "thread_of_elidinis";
    public static final String DUNG = "fossilised_dung";

    // ------------------------------------------------------------------ points

    /** Total reward points after a death: 20% lost, at least 1,000. */
    public static int afterDeath(int total) {
        return Math.max(0, total - Math.max(DEATH_MIN_LOSS, total * 20 / 100));
    }

    /** Total reward points after a cleared room adds its room points (each capped). */
    public static int addRoom(int total, int roomPoints) {
        return Math.min(TOTAL_POINTS_CAP, total + Math.max(0, Math.min(ROOM_POINTS_CAP, roomPoints)));
    }

    /** The MVP bonus for a team of this size. */
    public static int mvpBonus(int teamSize) {
        return MVP_POINTS_PER_MEMBER * Math.max(1, teamSize);
    }

    /** Points that count for the loot: the total minus the starting 5,000. */
    public static int rewardPoints(int total) {
        return Math.max(0, total - START_POINTS);
    }

    // ------------------------------------------------------------------ uniques

    /** Lowest raid level an item drops at without the extra 1/50 roll (OSRS: 50 for the fang and Lightbearer, else 150). */
    public static int minLevel(String item) {
        return item.equals("osmumtens_fang") || item.equals("lightbearer") ? 50 : 150;
    }

    /** A unique's weight at a raid level (out of the reweighted total; the 1/50 below its level is rolled separately). */
    public static int weight(RaidRewards.Unique u, int raidLevel) {
        return RaidRewards.toaWeight(u, Math.max(minLevel(u.item()), raidLevel)) / RaidRewards.TOA_WEIGHT_SCALE;
    }

    /** Picks a unique by weight at this raid level (before the 1/50 roll). */
    public static String pickUnique(int raidLevel, Random random) {
        int total = 0;
        for (RaidRewards.Unique u : RaidRewards.TOA_UNIQUES) {
            total += weight(u, raidLevel);
        }
        int r = random.nextInt(total);
        for (RaidRewards.Unique u : RaidRewards.TOA_UNIQUES) {
            r -= weight(u, raidLevel);
            if (r < 0) {
                return u.item();
            }
        }
        return RaidRewards.TOA_UNIQUES.get(0).item();
    }

    /** The unique a successful purple roll gives, or null when an item outside its raid level fails the 1/50 roll. */
    public static String rollUnique(int raidLevel, Random random) {
        String item = pickUnique(raidLevel, random);
        return raidLevel >= minLevel(item) || random.nextInt(50) == 0 ? item : null;
    }

    /** Chance that a raid at this level and with these team reward points really yields a unique (Module:Tombs of Amascut loot). */
    public static double trueUniqueChance(int teamPoints, int raidLevel) {
        double base = RaidScaling.toaPurpleChance(teamPoints, raidLevel);
        int total = 0;
        for (RaidRewards.Unique u : RaidRewards.TOA_UNIQUES) {
            total += weight(u, raidLevel);
        }
        double chance = 0;
        for (RaidRewards.Unique u : RaidRewards.TOA_UNIQUES) {
            double c = base * weight(u, raidLevel) / total;
            chance += raidLevel < minLevel(u.item()) ? c / 50 : c;
        }
        return chance;
    }

    // ------------------------------------------------------------------ tertiaries

    /** Elite clue chance (OSRS): 1% for every 2,000 personal reward points, at most 25%. */
    public static double eliteClueChance(int personalPoints) {
        return Math.min(0.25, Math.max(0, personalPoints) / 2_000 / 100.0);
    }

    /**
     * Bad luck mitigation (OSRS): a base rate of 1/{@code denominator} rises linearly to three times as likely at
     * 1.5 x {@code denominator} raid completions.
     */
    public static double mitigated(double base, int denominator, int completions) {
        double progress = Math.min(1.0, Math.max(0, completions) / (1.5 * denominator));
        return base * (1 + 2 * progress);
    }

    /** Thread of Elidinis chance: 1/10 with bad luck mitigation until one has been received, then 1/50. */
    public static double threadChance(int completions, boolean hadThread) {
        return hadThread ? 1 / 50.0 : mitigated(1 / 10.0, 10, completions);
    }

    /**
     * The jewel a raider gets this raid, or null (OSRS): 4/50 for any jewel, rising to 12/50 at 75 completions while one
     * is missing, and then always a missing one; with all four owned, 1/50 each without mitigation.
     */
    public static String rollJewel(Set<String> owned, int completions, Random random) {
        List<String> missing = new ArrayList<>();
        for (String j : JEWELS) {
            if (!owned.contains(j)) {
                missing.add(j);
            }
        }
        double chance = missing.isEmpty() ? 4 / 50.0 : mitigated(4 / 50.0, 50, completions);
        if (random.nextDouble() >= chance) {
            return null;
        }
        List<String> pool = missing.isEmpty() ? JEWELS : missing;
        return pool.get(random.nextInt(pool.size()));
    }

    // ------------------------------------------------------------------ Icthlarin's shroud

    /** Icthlarin's shroud, tiers 1-5. */
    public static final List<String> SHROUDS = List.of("icthlarins_shroud_tier_1", "icthlarins_shroud_tier_2",
            "icthlarins_shroud_tier_3", "icthlarins_shroud_tier_4", "icthlarins_shroud_tier_5");
    /** Normal or Expert mode completions each shroud tier needs (OSRS: 100, 500, 1,000, 1,500, 2,000). */
    public static final int[] SHROUD_COMPLETIONS = {100, 500, 1_000, 1_500, 2_000};

    /** The highest shroud tier (1-5) these Normal/Expert completions unlock, or 0. */
    public static int shroudTier(int normalOrExpertCompletions) {
        int tier = 0;
        for (int i = 0; i < SHROUD_COMPLETIONS.length; i++) {
            if (normalOrExpertCompletions >= SHROUD_COMPLETIONS[i]) {
                tier = i + 1;
            }
        }
        return tier;
    }

    /**
     * The shroud the lobby chest hands out: the highest tier these completions unlock, or null when none is unlocked or
     * the player already owns one of the tiers anywhere ({@code owns}: inventory, bank or the worn cape slot).
     */
    public static String shroudFromChest(int normalOrExpertCompletions, java.util.function.Predicate<String> owns) {
        int tier = shroudTier(normalOrExpertCompletions);
        if (tier == 0) {
            return null;
        }
        for (String s : SHROUDS) {
            if (owns.test(s)) {
                return null;
            }
        }
        return SHROUDS.get(tier - 1);
    }

    /**
     * The shroud's "Change" option (OSRS wiki: "alter the cape's appearance to other tiers, providing that they have met the
     * completion count required for them"): the next unlocked tier after {@code current}, wrapping round to tier 1, or
     * null when {@code current} isn't a shroud or no other tier is unlocked.
     */
    public static String shroudChange(String current, int normalOrExpertCompletions) {
        int at = SHROUDS.indexOf(current);
        int unlocked = shroudTier(normalOrExpertCompletions);
        if (at < 0 || unlocked <= 1) {
            return null;
        }
        int next = (at + 1) % unlocked;
        return next == at ? null : SHROUDS.get(next);
    }

    /** The completion-count key of a raid's mode (OSRS keeps Entry, Normal and Expert mode counts apart). */
    public static String modeKey(int raidLevel) {
        return raidLevel < 150 ? "tombs_of_amascut_entry" : raidLevel < 300 ? "tombs_of_amascut_normal" : "tombs_of_amascut_expert";
    }

    /** The name OSRS gives a mode's completion count ("Tombs of Amascut: Expert Mode"). */
    public static String modeCountName(int raidLevel) {
        return raidLevel < 150 ? "Tombs of Amascut: Entry Mode" : raidLevel < 300 ? "Tombs of Amascut" : "Tombs of Amascut: Expert Mode";
    }

    // ------------------------------------------------------------------ challenge rewards

    public static final Set<Invocation> AKKHA_INVOCATIONS = EnumSet.of(Invocation.DOUBLE_TROUBLE, Invocation.KEEP_BACK,
            Invocation.STAY_VIGILANT, Invocation.FEELING_SPECIAL);
    public static final Set<Invocation> BABA_INVOCATIONS = EnumSet.of(Invocation.MIND_THE_GAP, Invocation.GOTTA_HAVE_FAITH,
            Invocation.JUNGLE_JAPES, Invocation.SHAKING_THINGS_UP, Invocation.BOULDERDASH);
    public static final Set<Invocation> KEPHRI_INVOCATIONS = EnumSet.of(Invocation.LIVELY_LARVAE, Invocation.MORE_OVERLORDS,
            Invocation.BLOWING_MUD, Invocation.MEDIC, Invocation.AERIAL_ASSAULT);
    public static final Set<Invocation> ZEBAK_INVOCATIONS = EnumSet.of(Invocation.NOT_JUST_A_HEAD, Invocation.ARTERIAL_SPRAY,
            Invocation.BLOOD_THINNERS, Invocation.UPSET_STOMACH);
    public static final Set<Invocation> WARDEN_INVOCATIONS = EnumSet.of(Invocation.ANCIENT_HASTE, Invocation.ACCELERATION,
            Invocation.PENETRATION, Invocation.OVERCLOCKED, Invocation.OVERCLOCKED_2, Invocation.INSANITY);

    /**
     * Challenge rewards of a deathless raid (OSRS), in award order: the Masori crafting kit from raid level 350, the
     * Menaphite ornament kit from 425, one pet remnant from 450 (with every invocation of that boss and its path at level 4
     * when it died; checked Akkha, Zebak, Ba-Ba, Kephri, then the Wardens' ancient remnant) and the cursed phalanx from 500.
     * {@code pathLevelAtKill} maps a path boss id to its path's level when it died.
     */
    public static List<String> challengeRewards(int raidLevel, boolean deathless, Set<Invocation> invocations,
                                                Map<String, Integer> pathLevelAtKill) {
        List<String> out = new ArrayList<>();
        if (!deathless) {
            return out;
        }
        if (raidLevel >= 350) {
            out.add("masori_crafting_kit");
        }
        if (raidLevel >= 425) {
            out.add("menaphite_ornament_kit");
        }
        if (raidLevel >= 450) {
            Map<String, Object[]> remnants = new LinkedHashMap<>();
            remnants.put("remnant_of_akkha", new Object[]{"akkha", AKKHA_INVOCATIONS});
            remnants.put("remnant_of_zebak", new Object[]{"zebak", ZEBAK_INVOCATIONS});
            remnants.put("remnant_of_baba", new Object[]{"baba", BABA_INVOCATIONS});
            remnants.put("remnant_of_kephri", new Object[]{"kephri", KEPHRI_INVOCATIONS});
            String remnant = null;
            for (Map.Entry<String, Object[]> e : remnants.entrySet()) {
                @SuppressWarnings("unchecked")
                Set<Invocation> needed = (Set<Invocation>) e.getValue()[1];
                if (invocations.containsAll(needed) && pathLevelAtKill.getOrDefault((String) e.getValue()[0], 0) >= 4) {
                    remnant = e.getKey();
                    break;
                }
            }
            if (remnant == null && invocations.containsAll(WARDEN_INVOCATIONS)) {
                remnant = "ancient_remnant";
            }
            if (remnant != null) {
                out.add(remnant);
            }
        }
        if (raidLevel >= 500) {
            out.add("cursed_phalanx");
        }
        return out;
    }

    // ------------------------------------------------------------------ the chest

    /** What the game knows about one raider when the chest is rolled. */
    public record Raider(int completions, boolean hadThread, Set<String> ownedJewels, Set<String> owned) {
        public static Raider fresh() {
            return new Raider(0, false, Set.of(), Set.of());
        }
    }

    /**
     * Rolls the chest. {@code points} are each raider's reward points (start points already taken off); {@code raiders}
     * what the game knows about each (missing entries count as a first raid with nothing owned).
     */
    public static RaidRewards.Result roll(Map<String, Integer> points, int raidLevel, Map<String, Raider> raiders, boolean deathless,
                                          Set<Invocation> invocations, Map<String, Integer> pathLevelAtKill, Random random) {
        Map<String, List<RaidRewards.Loot>> loot = new LinkedHashMap<>();
        List<String> purples = new ArrayList<>();
        List<String> pets = new ArrayList<>();
        if (points.isEmpty()) {
            return new RaidRewards.Result(loot, purples, pets, 0);
        }
        int team = 0;
        for (Map.Entry<String, Integer> e : points.entrySet()) {
            team += Math.max(0, e.getValue());
            loot.put(e.getKey(), new ArrayList<>());
        }
        double chance = RaidScaling.toaPurpleChance(team, raidLevel);
        if (random.nextDouble() < chance) {
            String who = RaidRewards.weightedPlayer(points, random);
            String item = rollUnique(raidLevel, random);
            if (who != null && item != null) {
                loot.get(who).add(new RaidRewards.Loot(item, 1));
                purples.add(who);
            }
        }
        if (random.nextDouble() < RaidScaling.toaPetChance(team, raidLevel)) {
            String who = RaidRewards.weightedPlayer(points, random);
            if (who != null) {
                loot.get(who).add(new RaidRewards.Loot(RaidRewards.pet(RaidType.TOA), 1));
                pets.add(who);
            }
        }
        List<String> challenge = challengeRewards(raidLevel, deathless, invocations, pathLevelAtKill);
        List<String> order = new ArrayList<>(points.keySet());
        Collections.sort(order);
        for (String p : order) {
            List<RaidRewards.Loot> mine = loot.get(p);
            int pts = Math.max(0, points.get(p));
            Raider raider = raiders.getOrDefault(p, Raider.fresh());
            if (!purples.contains(p)) {
                if (pts < DUNG_BELOW) {
                    mine.add(new RaidRewards.Loot(DUNG, 1));
                } else {
                    for (RaidRewards.PointsCommon c : pickCommons(random)) {
                        mine.add(new RaidRewards.Loot(c.item(), RaidScaling.toaCommonQuantity(pts, c.divisor(), raidLevel)));
                    }
                }
            }
            if (random.nextDouble() < eliteClueChance(pts)) {
                mine.add(new RaidRewards.Loot("clue_scroll_elite", 1));
            }
            if (random.nextDouble() < threadChance(raider.completions(), raider.hadThread())) {
                mine.add(new RaidRewards.Loot(THREAD, 1));
            }
            String jewel = rollJewel(raider.ownedJewels(), raider.completions(), random);
            if (jewel != null) {
                mine.add(new RaidRewards.Loot(jewel, 1));
            }
            for (String c : challenge) {
                if (!raider.owned().contains(c)) {
                    mine.add(new RaidRewards.Loot(c, 1));
                }
            }
        }
        return new RaidRewards.Result(loot, purples, pets, chance);
    }

    /** Three common rolls (OSRS: each of the 27 items equally likely; three different items here). */
    static List<RaidRewards.PointsCommon> pickCommons(Random random) {
        List<RaidRewards.PointsCommon> copy = new ArrayList<>(RaidRewards.TOA_COMMONS);
        Collections.shuffle(copy, random);
        return copy.subList(0, RaidRewards.TOA_COMMON_ROLLS);
    }
}
