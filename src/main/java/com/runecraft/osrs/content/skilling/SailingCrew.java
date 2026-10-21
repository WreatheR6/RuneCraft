package com.runecraft.osrs.content.skilling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Sailing, part 2 (OSRS Wiki, July 2026 dump): crewmates and their Helmsmanship / Privateering / Deckhandiness,
 * cargo holds and their capacity by boat, repair kits, chainshot and incendiary cannonballs, hull speed and the shark
 * paint port-task reward. Snapshots: {@code src/test/resources/osrs/sailing/crewmates.tsv}, {@code cargo_holds.tsv},
 * {@code repair_kits.tsv} and {@code special_cannonballs.tsv}.
 */
public final class SailingCrew {
    private SailingCrew() {
    }

    // ------------------------------------------------------------------ crew

    /**
     * A crewmate: Sailing level to recruit, where they wait (OSRS tile, approx: the town's dock or centre), their three
     * crew skills (1-4), the courier crates they carry and the quest they need done (or null).
     */
    public record Crewmate(String id, String name, int level, String location, int x, int y, int helmsmanship, int privateering,
                           int deckhandiness, int crates, String quest) {
        public String placeId() {
            return "crew_" + id;
        }
    }

    public static final List<Crewmate> CREW = List.of(
            new Crewmate("jobless_jim", "Jobless Jim", 40, "Port Sarim", 3047, 3197, 2, 1, 3, 2, null),
            // Ex-Captain Siad also needs The Tourist Trap; Cabin Boy Jenkins a ghostspeak amulet (checked), Bosun Zarah the
            // hard Fremennik Diary and Spotter Virginia 100 Tempoross subdues (both not checked, approx).
            new Crewmate("ex_captain_siad", "Ex-Captain Siad", 50, "Desert Mining Camp", 3287, 3028, 3, 2, 3, 2, "the_tourist_trap"),
            new Crewmate("adventurer_ada", "Adventurer Ada", 55, "The Pandemonium", 3055, 2988, 3, 2, 1, 1, null),
            new Crewmate("cabin_boy_jenkins", "Cabin Boy Jenkins", 60, "Crandor", 2852, 3236, 2, 2, 4, 2, "dragon_slayer_i"),
            new Crewmate("oarswoman_olga", "Oarswoman Olga", 65, "Burgh de Rott", 3497, 3207, 2, 4, 2, 1, "in_aid_of_the_myreque"),
            new Crewmate("jittery_jim", "Jittery Jim", 70, "Port Khazard", 2677, 3157, 1, 4, 1, 1, null),
            new Crewmate("bosun_zarah", "Bosun Zarah", 80, "Etceteria", 2614, 3852, 3, 3, 2, 1, "royal_trouble"),
            new Crewmate("jolly_jim", "Jolly Jim", 85, "Brimhaven", 2772, 3232, 1, 1, 4, 2, null),
            new Crewmate("spotter_virginia", "Spotter Virginia", 90, "Ruins of Unkah", 3148, 2826, 4, 2, 2, 1, null),
            new Crewmate("sailor_jakob", "Sailor Jakob", 95, "Catherby", 2803, 3428, 4, 1, 1, 1, null));

    public static Crewmate crewmate(String id) {
        for (Crewmate c : CREW) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /** Crew slots unlocked by Sailing level (wiki: 1 at 40, 2 at 55, 3 at 70, 4 at 85, 5 at 95; boosts don't count). */
    public static int crewSlots(int sailingLevel) {
        int[] at = {40, 55, 70, 85, 95};
        int n = 0;
        for (int l : at) {
            if (sailingLevel >= l) {
                n++;
            }
        }
        return n;
    }

    /** Crew a boat holds: a raft none, a skiff 2, a sloop 5. */
    public static int boatCrew(String boat) {
        return switch (boat) {
            case "skiff" -> 2;
            case "sloop" -> 5;
            default -> 0;
        };
    }

    /** The crewmates aboard: the recruited ones in crew order, up to the unlocked slots and the boat's room. */
    public static List<Crewmate> aboard(List<String> crewOrder, String boat, int sailingLevel) {
        int room = Math.min(crewSlots(sailingLevel), boatCrew(boat));
        List<Crewmate> out = new ArrayList<>();
        for (String id : crewOrder) {
            Crewmate c = crewmate(id);
            if (c != null && out.size() < room) {
                out.add(c);
            }
        }
        return out;
    }

    /** Helmsmanship: the share of the trimming XP a crewmate earns you when they trim (wiki: 40% at 1 .. 70% at 4). */
    public static double trimShare(int helmsmanship) {
        return 0.3 + 0.1 * Math.max(1, Math.min(4, helmsmanship));
    }

    /** Privateering: a crewmate can man a cannon whose privateering requirement they meet. */
    public static boolean canMan(SailingSea.Cannon cannon, int privateering) {
        return privateering >= cannon.privateering();
    }

    /**
     * A crewmate's cannon max hit: the cannon max hit with the player's Sailing level, times (Privateering + 13) / 20,
     * rounded down (wiki).
     */
    public static int crewMaxHit(int sailingLevel, SailingSea.Cannon cannon, SailingSea.Ball ball, int privateering) {
        return SailingSea.cannonMaxHit(sailingLevel, cannon, ball, false) * (privateering + 13) / 20;
    }

    /** Deckhandiness: the share of salvaging XP a crewmate's haul gives you (10% at 1 .. 40% at 4). */
    public static double salvageShare(int deckhandiness) {
        return 0.1 * Math.max(1, Math.min(4, deckhandiness));
    }

    /** Crewmates roll to salvage every 5 ticks (players every 4; OSRS Wiki, Shipwreck salvaging). */
    public static final int CREW_SALVAGE_TICKS = 5;

    /**
     * A crewmate's chance to haul salvage: the wreck and hook's low/high times Deckhandiness x 10% on the player's level
     * (wiki: about 12% for Jobless Jim (3) where the player has 40%; see {@link SailingSea#crewSalvageChance}). The haul
     * gives {@link #salvageShare} of the player's XP.
     */
    public static double crewSalvageChance(SailingData.Wreck wreck, int sailingLevel, int hookTier, int deckhandiness) {
        return SailingSea.crewSalvageChance(wreck, sailingLevel, hookTier, deckhandiness);
    }

    /** The crewmate aboard with the best Deckhandiness who can work a hook of this tier (SailingSea metal index), or null. */
    public static Crewmate salvager(List<Crewmate> aboard, int hookTier) {
        Crewmate best = null;
        for (Crewmate c : aboard) {
            if (bestHook(c.deckhandiness()) >= hookTier && (best == null || c.deckhandiness() > best.deckhandiness())) {
                best = c;
            }
        }
        return best;
    }

    /** Deckhandiness: the best salvaging hook tier a crewmate can work (1 steel, 2 adamant, 3 rune, 4 dragon). */
    public static int bestHook(int deckhandiness) {
        return switch (Math.max(1, Math.min(4, deckhandiness))) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 5;
            default -> 6;
        };
    }

    /** A crewmate applying a repair kit heals the kit's amount plus 0 to (Deckhandiness - 1) more (wiki table). */
    public static int crewRepair(Kit kit, int deckhandiness, Random random) {
        int bonus = Math.max(0, deckhandiness - 1);
        return kit.heals() + (bonus == 0 ? 0 : random.nextInt(bonus + 1));
    }

    // ------------------------------------------------------------------ cargo holds

    /** A cargo hold facility tier with its capacity on a raft, skiff and sloop (wiki Cargo hold tables). */
    public record Hold(int tier, String id, String name, int sailing, int construction, double conXp, Map<String, Integer> materials,
                       int raft, int skiff, int sloop) {
        public int capacity(String boat) {
            return switch (boat) {
                case "skiff" -> skiff;
                case "sloop" -> sloop;
                default -> raft;
            };
        }
    }

    private static Map<String, Integer> mats(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    public static final List<Hold> HOLDS = List.of(
            new Hold(0, "basic_cargo_hold", "Basic cargo hold", 1, 1, 232, mats("plank", 8, "bronze_nails", 32), 20, 30, 40),
            new Hold(1, "oak_cargo_hold", "Oak cargo hold", 18, 11, 491, mats("oak_plank", 8, "iron_nails", 32), 30, 45, 60),
            new Hold(2, "teak_cargo_hold", "Teak cargo hold", 29, 21, 738, mats("teak_plank", 8, "steel_nails", 32, "lead_bar", 3),
                    45, 60, 90),
            new Hold(3, "mahogany_cargo_hold", "Mahogany cargo hold", 46, 41, 1153, mats("mahogany_plank", 8, "mithril_nails", 32,
                    "lead_bar", 3), 60, 90, 120),
            new Hold(4, "camphor_cargo_hold", "Camphor cargo hold", 60, 53, 1326, mats("camphor_plank", 8, "adamant_nails", 32,
                    "lead_bar", 3), 80, 120, 160),
            new Hold(5, "ironwood_cargo_hold", "Ironwood cargo hold", 80, 77, 1492, mats("ironwood_plank", 8, "rune_nails", 32,
                    "cupronickel_bar", 3), 105, 150, 210),
            new Hold(6, "rosewood_cargo_hold", "Rosewood cargo hold", 89, 84, 1634, mats("rosewood_plank", 8, "dragon_nails", 32,
                    "cupronickel_bar", 3), 120, 180, 240));

    /** The cargo holds as boat facilities (built at the boat schematics like every other facility). */
    public static final List<SailingTraining.Facility> HOLD_FACILITIES = holdFacilities();

    private static List<SailingTraining.Facility> holdFacilities() {
        List<SailingTraining.Facility> out = new ArrayList<>();
        for (Hold h : HOLDS) {
            out.add(new SailingTraining.Facility(h.id(), h.name(), h.sailing(), h.construction(), h.conXp(), h.materials(),
                    "Stores " + h.raft() + "/" + h.skiff() + "/" + h.sloop() + " items (raft/skiff/sloop) at sea."));
        }
        return List.copyOf(out);
    }

    /** Every boat facility: the round-8 ones, salvaging hooks, cannons, the eternal brazier and the cargo holds. */
    public static List<SailingTraining.Facility> allFacilities() {
        List<SailingTraining.Facility> all = new ArrayList<>(SailingTraining.FACILITIES);
        all.addAll(SailingSea.FACILITIES);
        all.addAll(HOLD_FACILITIES);
        return all;
    }

    /** The best cargo hold among a boat's facilities, or null. */
    public static Hold bestHold(Set<String> facilities) {
        Hold best = null;
        for (Hold h : HOLDS) {
            if (facilities.contains(h.id())) {
                best = h;
            }
        }
        return best;
    }

    /**
     * Items the cargo hold accepts (a RuneCraft subset of the wiki list): cannonballs, repair kits, salvage, cargo crates,
     * bounty items, trawled fish and fish offcuts.
     */
    public static boolean storable(String item) {
        return SailingSea.ball(item) != null || special(item) != null || kit(item) != null || SailingData.wreckBySalvage(item) != null
                || item.equals("cargo_crate") || SailingSea.bountyParts().contains(item) || trawledFish(item)
                || item.equals(SailingShoals.OFFCUTS) || item.equals(SailingShoals.FINE_OFFCUTS);
    }

    /**
     * Cargo lost when the boat sinks (wiki: courier crates, bounty items, salvage and trawled fish; cannonballs, repair kits
     * and fish offcuts are kept).
     */
    public static boolean lostOnSinking(String item) {
        return SailingData.wreckBySalvage(item) != null || item.equals("cargo_crate") || SailingSea.bountyParts().contains(item)
                || trawledFish(item);
    }

    /** A raw fish a trawling shoal gives. */
    public static boolean trawledFish(String item) {
        for (SailingShoals.Shoal s : SailingShoals.SHOALS) {
            if (item.equals("raw_" + s.fish())) {
                return true;
            }
        }
        return false;
    }

    /** Slots used: a stackable item takes one slot for the whole stack, anything else one slot each (wiki). */
    public static int slotsUsed(Map<String, Integer> contents, Predicate<String> stackable) {
        int n = 0;
        for (Map.Entry<String, Integer> e : contents.entrySet()) {
            if (e.getValue() > 0) {
                n += stackable.test(e.getKey()) ? 1 : e.getValue();
            }
        }
        return n;
    }

    /** How many of {@code count} more items fit in a hold of {@code capacity} slots. */
    public static int room(Map<String, Integer> contents, String item, int count, int capacity, Predicate<String> stackable) {
        int free = capacity - slotsUsed(contents, stackable);
        if (stackable.test(item)) {
            return contents.getOrDefault(item, 0) > 0 || free > 0 ? count : 0;
        }
        return Math.max(0, Math.min(count, free));
    }

    // ------------------------------------------------------------------ repair kits

    /** A repair kit: hitpoints it restores and its Construction recipe (wiki). */
    public record Kit(String id, String name, int heals, int construction, double xp, String plank, int planks, String nails, int nailQty,
                      int paste, int made) {
    }

    public static final List<Kit> KITS = List.of(
            new Kit("repair_kit", "Repair kit", 5, 1, 43.5, "plank", 2, "bronze_nails", 10, 5, 2),
            new Kit("oak_repair_kit", "Oak repair kit", 10, 19, 90, "oak_plank", 2, "iron_nails", 10, 5, 2),
            new Kit("teak_repair_kit", "Teak repair kit", 20, 30, 135, "teak_plank", 2, "steel_nails", 10, 5, 2),
            new Kit("mahogany_repair_kit", "Mahogany repair kit", 30, 47, 210, "mahogany_plank", 2, "mithril_nails", 10, 5, 2),
            new Kit("camphor_repair_kit", "Camphor repair kit", 40, 66, 255, "camphor_plank", 2, "adamant_nails", 10, 5, 2),
            new Kit("ironwood_repair_kit", "Ironwood repair kit", 45, 80, 300, "ironwood_plank", 1, "rune_nails", 10, 5, 3),
            new Kit("rosewood_repair_kit", "Rosewood repair kit", 50, 92, 330, "rosewood_plank", 1, "dragon_nails", 5, 5, 3));

    public static Kit kit(String id) {
        for (Kit k : KITS) {
            if (k.id().equals(id)) {
                return k;
            }
        }
        return null;
    }

    /** Shipwright Seb repairs boats for 50 coins per hitpoint (wiki); a wrecked boat is recovered at that rate for its full hitpoints. */
    public static final int SHIPWRIGHT_GP_PER_HP = 50;

    /** Repairs at a crewmate's command once the boat drops below this share of its hitpoints (approx). */
    public static final double CREW_REPAIR_BELOW = 0.5;

    // ------------------------------------------------------------------ chainshot and incendiary cannonballs

    /** A chainshot or incendiary cannonball: the plain ball it hits like, and its Smithing recipe (wiki). */
    public record Special(String item, String name, String base, boolean chainshot, int smithing, double xp) {
    }

    public static final List<Special> SPECIALS = buildSpecials();

    private static List<Special> buildSpecials() {
        String[] metals = {"bronze", "iron", "steel", "mithril", "adamant", "rune"};
        int[] level = {5, 20, 35, 55, 75, 90};
        double[] chainXp = {10, 16, 24, 30, 40, 48};
        double[] fireXp = {5, 10, 15, 20, 25, 30};
        List<Special> out = new ArrayList<>();
        for (int k = 0; k < 2; k++) {
            boolean chain = k == 0;
            for (int i = 0; i < metals.length; i++) {
                String base = metals[i].equals("steel") ? "cannonball" : metals[i] + "_cannonball";
                String kind = chain ? "chainshot" : "incendiary";
                out.add(new Special(metals[i] + "_" + kind + "_cannonball", SailingSea.title(metals[i]) + " " + kind + " cannonball", base,
                        chain, level[i], chain ? chainXp[i] : fireXp[i]));
            }
        }
        return List.copyOf(out);
    }

    public static Special special(String item) {
        for (Special s : SPECIALS) {
            if (s.item().equals(item)) {
                return s;
            }
        }
        return null;
    }

    /** The cannon stats of any cannonball: plain balls as they are, chainshot and incendiary ones as their plain ball. */
    public static SailingSea.Ball ballStats(String item) {
        SailingSea.Ball b = SailingSea.ball(item);
        if (b != null) {
            return b;
        }
        Special s = special(item);
        return s == null ? null : SailingSea.ball(s.base());
    }

    /** Chainshot halves the target's movement speed for 8 ticks (wiki); here it delays the creature's next attack (approx). */
    public static final int CHAINSHOT_TICKS = 8;
    /** Burn: 1 damage every 4 ticks, 10 times; at most 5 burns stack (wiki Burn). */
    public static final int BURN_INTERVAL = 4;
    public static final int BURN_HITS = 10;
    public static final int BURN_MAX_STACKS = 5;

    // ------------------------------------------------------------------ hull speed

    /** The hull's boat speed (wiki Hull table: 1.5 wooden/oak, 2.0 teak/mahogany, 2.5 camphor/ironwood, 3.0 rosewood). */
    public static double hullSpeed(String boat, int hullTier) {
        SailingTraining.Part p = SailingTraining.part(boat, SailingTraining.PartType.HULL, Math.max(0, hullTier));
        return p == null ? 1.5 : Double.parseDouble(p.stat());
    }

    /** Top speed relative to a wooden hull (the vanilla boat's speed is the wooden hull's). */
    public static double speedFactor(String boat, int hullTier) {
        return hullSpeed(boat, hullTier) / 1.5;
    }

    /**
     * Per-tick velocity multiplier that makes a vanilla boat (momentum 0.9, 0.04 thrust a tick) reach {@code factor}
     * times its top speed: v = 0.9 * m * v + 0.04 at terminal speed, so m = (1 - 0.04 / (0.4 * factor)) / 0.9.
     */
    public static double velocityMultiplier(double factor) {
        return factor <= 1 ? 1 : (1 - 0.1 / factor) / 0.9;
    }

    // ------------------------------------------------------------------ rewards

    /** Shark paint: 1/36 on completing a port task (wiki). */
    public static final int SHARK_PAINT_RATE = 36;

    // ------------------------------------------------------------------ island materials

    /** Nickel rocks (74 Mining, 80.5 XP); cupronickel bar: 2 copper ore + 1 nickel ore, 74 Smithing, 42 XP (wiki). */
    public static final double NICKEL_XP = 80.5;
    public static final double CUPRONICKEL_XP = 42;
    /** Sawmill fees for the Sailing woods (wiki: camphor 2,500, ironwood 5,000, rosewood 7,500 coins). */
    public static final Object[][] SAWMILL = {{"camphor_plank", "camphor_logs", 2500}, {"ironwood_plank", "ironwood_logs", 5000},
            {"rosewood_plank", "rosewood_logs", 7500}};
}
