package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.ItemSink;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Emir's Arena (OSRS Wiki "Emir's Arena", "PvP Arena Rewards", "Scroll of imbuing"): the Al Kharid PvP arena that replaced the
 * Duel Arena in July 2022. Fights are safe: nothing is lost on death.
 *
 * <ul>
 * <li><b>Legacy duels</b>: a player challenges another inside the arena and the challenged player accepts. The challenger's
 * duel options (the twelve rules and the disabled equipment slots, or the Whip / Boxing presets) apply. Staking was removed with
 * the Duel Arena, and unofficial duels give no rank or reward points ("As this is an unofficial duel, you will not receive rank
 * or reward points for participating."); wins and losses are counted separately.</li>
 * <li><b>Ranked 1v1 duels</b>: the game pairs two players who signed up at the arena recruitment board. The winner gets 16
 * reward points plus one per win of their current streak (up to +10, so 26), the loser 12; rank moves up or down. In OSRS these
 * run on separate Arena worlds where the player's own levels and items stay behind: both fighters play the week's account
 * build ({@link Build}, rotating Max/Med, Zerker, 1 Def Pure every Wednesday) with a kit from the supplies chest. Here the
 * player's belongings and stats are stashed for the fight and everyone gets the build's starter kit (the Last Man Standing
 * kits of the same builds; the supplies chest's free choice isn't modelled). // approx</li>
 * <li><b>PvP Arena Rewards</b> (Mubariz, at the entrance): the scroll of imbuing (200 points) and the blighted surge sack (50
 * for 10 points). Reward points are capped at 15,000.</li>
 * </ul>
 */
public final class EmirsArena {
    private EmirsArena() {
    }

    // ---------------------------------------------------------------- places and ids
    /** The entrance by the gate, where the ring of dueling lands (3315, 3235) and Mubariz stands (3314, 3243). // approx tile */
    public static final String PLACE = "mg_emirs_arena";
    public static final int ENTRANCE_X = 3316;
    public static final int ENTRANCE_Y = 3238;
    /** The arena itself (the wiki's map centre); fights take place in an instance. */
    public static final int ARENA_X = 3360;
    public static final int ARENA_Y = 3233;
    /** The arena grounds where players may challenge each other (the old Duel Arena's walls plus the entrance). // approx */
    public static final int MIN_X = 3305;
    public static final int MAX_X = 3392;
    public static final int MIN_Y = 3195;
    public static final int MAX_Y = 3292;

    public static final String MUBARIZ = "mg_mubariz";
    public static final String GUARD = "mg_arena_guard_yon";
    public static final String SHOP = "emirs_arena";
    public static final String POINTS = "pts:emirs_arena";
    /** Variant of the ranked queue (the minigame's entrance block, the arena recruitment board). */
    public static final String RANKED = "emirs_arena_entrance";
    /** Variant prefix of a legacy duel: "duel:" + the encoded options. */
    public static final String DUEL_PREFIX = "duel:";
    public static final String SCROLL_OF_IMBUING = "scroll_of_imbuing";
    public static final String BLIGHTED_SURGE_SACK = "blighted_surge_sack";

    /** MinigameData stats. */
    public static final String RANK = "emir_rank";
    public static final String WINS = "emir_wins";
    public static final String LOSSES = "emir_losses";
    public static final String STREAK = "emir_streak";
    public static final String LEGACY_WINS = "emir_legacy_wins";
    public static final String LEGACY_LOSSES = "emir_legacy_losses";

    public static boolean inArena(int osrsX, int osrsY) {
        return osrsX >= MIN_X && osrsX <= MAX_X && osrsY >= MIN_Y && osrsY <= MAX_Y;
    }

    // ---------------------------------------------------------------- duel options
    /** The twelve duel options of the Duel Options screen, in the wiki's order. */
    public enum Rule {
        NO_RANGED("No Ranged"),
        NO_MELEE("No Melee"),
        NO_MAGIC("No Magic"),
        NO_SPECIAL_ATTACKS("No Special Attacks"),
        FUN_WEAPONS("Fun Weapons"),
        NO_FORFEIT("No Forfeit"),
        NO_DRINKS("No Drinks"),
        NO_FOOD("No Food"),
        NO_PRAYER("No Prayer"),
        NO_MOVEMENT("No Movement"),
        NO_WEAPON_SWITCH("No Weapon Switch"),
        SHOW_INVENTORIES("Show Inventories");

        public final String displayName;

        Rule(String displayName) {
            this.displayName = displayName;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Rule byKey(String key) {
            for (Rule r : values()) {
                if (r.key().equals(key.toLowerCase(Locale.ROOT)) || r.displayName.equalsIgnoreCase(key)) {
                    return r;
                }
            }
            return null;
        }
    }

    /** Equipment slots that can be disabled on the Duel Options screen. */
    public enum DuelSlot {
        HEAD, CAPE, NECK, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static DuelSlot byKey(String key) {
            for (DuelSlot s : values()) {
                if (s.key().equals(key.toLowerCase(Locale.ROOT))) {
                    return s;
                }
            }
            return null;
        }
    }

    /** One duel's options. */
    public record Options(Set<Rule> rules, Set<DuelSlot> slots) {
        public Options {
            rules = rules.isEmpty() ? EnumSet.noneOf(Rule.class) : EnumSet.copyOf(rules);
            slots = slots.isEmpty() ? EnumSet.noneOf(DuelSlot.class) : EnumSet.copyOf(slots);
        }

        public static Options none() {
            return new Options(EnumSet.noneOf(Rule.class), EnumSet.noneOf(DuelSlot.class));
        }

        public boolean has(Rule r) {
            return rules.contains(r);
        }

        public boolean disabled(DuelSlot s) {
            return slots.contains(s);
        }

        public Options toggle(Rule r) {
            Set<Rule> set = EnumSet.noneOf(Rule.class);
            set.addAll(rules);
            if (!set.remove(r)) {
                set.add(r);
            }
            return new Options(set, slots);
        }

        public Options toggle(DuelSlot s) {
            Set<DuelSlot> set = EnumSet.noneOf(DuelSlot.class);
            set.addAll(slots);
            if (!set.remove(s)) {
                set.add(s);
            }
            return new Options(rules, set);
        }

        /** Bits 0-11 the rules, bits 16-26 the disabled slots. */
        public long encode() {
            long bits = 0;
            for (Rule r : rules) {
                bits |= 1L << r.ordinal();
            }
            for (DuelSlot s : slots) {
                bits |= 1L << (16 + s.ordinal());
            }
            return bits;
        }

        public static Options decode(long bits) {
            Set<Rule> rules = EnumSet.noneOf(Rule.class);
            for (Rule r : Rule.values()) {
                if ((bits & (1L << r.ordinal())) != 0) {
                    rules.add(r);
                }
            }
            Set<DuelSlot> slots = EnumSet.noneOf(DuelSlot.class);
            for (DuelSlot s : DuelSlot.values()) {
                if ((bits & (1L << (16 + s.ordinal()))) != 0) {
                    slots.add(s);
                }
            }
            return new Options(rules, slots);
        }

        public String describe() {
            if (rules.isEmpty() && slots.isEmpty()) {
                return "no restrictions";
            }
            StringBuilder sb = new StringBuilder();
            for (Rule r : rules) {
                sb.append(sb.length() == 0 ? "" : ", ").append(r.displayName);
            }
            if (!slots.isEmpty()) {
                sb.append(sb.length() == 0 ? "" : ", ").append("disabled slots: ");
                boolean first = true;
                for (DuelSlot s : slots) {
                    sb.append(first ? "" : " ").append(s.key());
                    first = false;
                }
            }
            return sb.toString();
        }
    }

    /**
     * The two built-in presets: "Whip" allows only a one-handed weapon and no armour; "Boxing" allows no weapons or armour.
     * Both also turn off Ranged and Magic. // approx: the wiki only lists the equipment part
     */
    public static Options preset(String name) {
        Set<DuelSlot> armour = EnumSet.allOf(DuelSlot.class);
        armour.remove(DuelSlot.WEAPON);
        Set<Rule> rules = EnumSet.of(Rule.NO_RANGED, Rule.NO_MAGIC);
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "whip" -> new Options(rules, armour);
            case "boxing" -> new Options(rules, EnumSet.allOf(DuelSlot.class));
            default -> null;
        };
    }

    /** Why these options can't be used for a duel, or null when they can. */
    public static String problem(Options o) {
        if (o.has(Rule.NO_RANGED) && o.has(Rule.NO_MELEE) && o.has(Rule.NO_MAGIC)) {
            return "You can't have No Ranged, No Melee and No Magic: how would you fight?";
        }
        if (o.has(Rule.FUN_WEAPONS) && (o.has(Rule.NO_MELEE) || o.disabled(DuelSlot.WEAPON))) {
            return "Fun Weapons duels need melee and the weapon slot.";
        }
        if (o.has(Rule.NO_MOVEMENT) && o.has(Rule.NO_MELEE) && o.has(Rule.NO_RANGED) && o.has(Rule.NO_MAGIC)) {
            return "That duel can't be fought.";
        }
        return null;
    }

    /** True if this attack style may be used under the options. */
    public static boolean styleAllowed(Options o, AttackType type) {
        if (type == AttackType.MAGIC) {
            return !o.has(Rule.NO_MAGIC);
        }
        if (type == AttackType.RANGED) {
            return !o.has(Rule.NO_RANGED);
        }
        return !o.has(Rule.NO_MELEE);
    }

    /**
     * Fun weapons: the only weapons allowed under Fun Weapons (OSRS Wiki "Fun weapons": wieldable objects with negative bonuses;
     * bare fists are not allowed). The ids are the wiki's item names; the cursed goblin hammer, hunting knife, nutcracker staff
     * and skis are excluded, as in OSRS. Of these RuneCraft has the crier bell (a Treasure Trails reward), so Fun Weapons duels
     * are fought with crier bells until more of the holiday items exist.
     */
    public static final List<String> FUN_WEAPONS = List.of(
            // quest items
            "trollweiss", "severed_leg", "undead_chicken",
            // holiday items, one-handed
            "rubber_chicken", "easter_basket", "birthday_balloons", "hand_fan", "fine_mesh_net", "prop_sword", "eek",
            "candy_cane", "carrot_sword", "24_carat_sword", "haunted_wine_bottle", "dragon_candle_dagger", "10th_birthday_balloons",
            // holiday items, two-handed
            "giant_present", "birthday_cake", "giant_easter_egg", "skeleton_lantern", "greggs_eastdoor", "giant_boulder",
            "cursed_banana", "love_crossbow", "archibald",
            // Treasure Trails
            "crier_bell", "gilded_spade", "large_spade", "clueless_scroll",
            // flowers
            "red_flowers", "blue_flowers", "yellow_flowers", "purple_flowers", "orange_flowers", "mixed_flowers",
            "assorted_flowers", "black_flowers", "white_flowers",
            // Leagues and speedrunning trophies
            "twisted_adamant_trophy", "twisted_rune_trophy", "twisted_dragon_trophy", "trailblazer_adamant_trophy",
            "trailblazer_rune_trophy", "trailblazer_dragon_trophy", "trailblazer_reloaded_adamant_trophy",
            "trailblazer_reloaded_rune_trophy", "trailblazer_reloaded_dragon_trophy", "raging_echoes_adamant_trophy",
            "raging_echoes_rune_trophy", "raging_echoes_dragon_trophy", "bronze_speedrun_trophy", "silver_speedrun_trophy",
            "gold_speedrun_trophy", "platinum_speedrun_trophy", "diamond_speedrun_trophy",
            // other
            "golden_tench", "mouse_toy", "stale_baguette", "giant_stopwatch", "mystic_cards");

    public static boolean weaponAllowed(Options o, String weaponId) {
        if (o.has(Rule.FUN_WEAPONS)) {
            return weaponId != null && FUN_WEAPONS.contains(weaponId);
        }
        return !(o.disabled(DuelSlot.WEAPON) && weaponId != null && !weaponId.isEmpty());
    }

    /** Food (No Food covers every food, including Saradomin brews) and drinks (No Drinks: potions). */
    public static boolean blockedConsumable(Options o, Kind kind, String id) {
        boolean brew = id.startsWith("saradomin_brew");
        if (o.has(Rule.NO_FOOD) && (kind == Kind.FOOD || brew)) {
            return true;
        }
        return o.has(Rule.NO_DRINKS) && kind == Kind.POTION && !brew;
    }

    // ---------------------------------------------------------------- ranked rewards
    public static final int START_RANK = 2500;
    public static final int MAX_RANK = 15000;
    public static final int MAX_POINTS = 15000;
    public static final int WIN_POINTS = 16;
    public static final int LOSS_POINTS = 12;
    public static final int MAX_STREAK_BONUS = 10;

    /** Reward points for a ranked duel: a win gives 16 plus one per win of the streak before it (up to 10); a loss 12. */
    public static int rewardPoints(boolean won, int streakBefore) {
        return won ? WIN_POINTS + Math.min(MAX_STREAK_BONUS, Math.max(0, streakBefore)) : LOSS_POINTS;
    }

    /** Points the player can actually receive with {@code balance} already held (15,000 at most). */
    public static int cappedGain(int balance, int gain) {
        return Math.max(0, Math.min(gain, MAX_POINTS - balance));
    }

    /**
     * Rank change after a ranked duel: an Elo update (K 64) on the 0-15,000 rank scale. // approx: Jagex hasn't published the
     * formula
     */
    public static int rankChange(int rank, int opponentRank, boolean won) {
        double expected = 1.0 / (1.0 + Math.pow(10, (opponentRank - rank) / 400.0));
        int delta = (int) Math.round(64 * ((won ? 1.0 : 0.0) - expected));
        int next = Math.max(0, Math.min(MAX_RANK, rank + delta));
        return next - rank;
    }

    private static final String[] METALS = {"Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune"};
    private static final String[] ROMAN = {"I", "II", "III", "IV"};

    /** The wiki's rank titles: Bronze I (0) up to Rune IV (5,999) in steps of 250, then the Dragon tiers. */
    public static String rankTitle(int rank) {
        if (rank >= 9000) {
            return "Dragon Grandmaster";
        }
        if (rank >= 8000) {
            return "Dragon Master";
        }
        if (rank >= 7000) {
            return "Dragon Elite";
        }
        if (rank >= 6000) {
            return "Dragon";
        }
        int r = Math.max(0, rank);
        return METALS[r / 1000] + " " + ROMAN[(r % 1000) / 250];
    }

    // ---------------------------------------------------------------- ranked account builds
    /**
     * The account builds ranked duels rotate through (OSRS Wiki "Emir's Arena", Account Builds, with the 25 March 2026 change:
     * Zerker 75 Attack / 50 Defence / 63 Prayer, Max/Med 99 Attack, Defence and Prayer). Strength, Hitpoints, Ranged and Magic
     * are 99 for every build. {@link #kit} names the Last Man Standing build whose starter kit is handed out.
     */
    public enum Build {
        MAX_MED("Max/Med", 99, 99, 99, 99, LastManStanding.Build.MAX_MED),
        ZERKER("Zerker", 75, 99, 50, 63, LastManStanding.Build.ZERKER),
        PURE("1 Def Pure", 60, 99, 1, 52, LastManStanding.Build.PURE);

        public final String displayName;
        public final int attack;
        public final int strength;
        public final int defence;
        public final int prayer;
        public final LastManStanding.Build kit;

        Build(String displayName, int attack, int strength, int defence, int prayer, LastManStanding.Build kit) {
            this.displayName = displayName;
            this.attack = attack;
            this.strength = strength;
            this.defence = defence;
            this.prayer = prayer;
            this.kit = kit;
        }

        /** The level this build fights at in one of {@link LastManStanding#BUILD_SKILLS} (0 for other skills). */
        public int level(com.runecraft.osrs.Skill skill) {
            return switch (skill) {
                case ATTACK -> attack;
                case STRENGTH -> strength;
                case DEFENCE -> defence;
                case PRAYER -> prayer;
                case HITPOINTS, RANGED, MAGIC -> 99;
                default -> 0;
            };
        }
    }

    /** The rotation's first day, Wednesday 27 July 2022 (a Max/Med week; the wiki's "weekly" counter starts here). */
    public static final long ROTATION_START_EPOCH_DAY = java.time.LocalDate.of(2022, 7, 27).toEpochDay();

    /**
     * The build in rotation on {@code epochDay} (days since 1970-01-01, UTC): floor(days since 27 July 2022 / 7) mod 3 picks
     * Max/Med, Zerker, Pure, as the wiki's rotation template computes it. Builds change with the Wednesday game update.
     */
    public static Build weeklyBuild(long epochDay) {
        long weeks = Math.floorDiv(epochDay - ROTATION_START_EPOCH_DAY, 7);
        return Build.values()[(int) Math.floorMod(weeks, Build.values().length)];
    }

    /** The starter kit a ranked fighter gets for the build: the matching Last Man Standing loadout. // approx */
    public static LastManStanding.Loadout loadout(Build build) {
        return LastManStanding.loadout(build.kit);
    }

    // ---------------------------------------------------------------- scroll of imbuing
    /**
     * The item the scroll of imbuing turns {@code item} into (the same items Nightmare Zone and Soul Wars imbue), or null. One
     * scroll imbues any of them.
     */
    public static String imbued(String item) {
        for (Object[] row : MinigameRules.Imbues.ITEMS) {
            if (row[0].equals(item)) {
                return (String) row[1];
            }
        }
        return null;
    }

    /** Mubariz's PvP Arena Rewards (points; OSRS Wiki). */
    static void shop(RewardShop shop) {
        shop.item(SCROLL_OF_IMBUING, POINTS, 200)
                .item(BLIGHTED_SURGE_SACK, 50, POINTS, 10);
    }

    public static void items(ItemSink items) {
        items.add(SCROLL_OF_IMBUING, "Scroll of imbuing", Kind.SIMPLE, "note", 0xB070E0).value(100)
                .examine("A magical scroll used to imbue a variety of items.");
        // Pays for one surge or wave spell in the Wilderness, Castle Wars or Soul Wars (osrs.data.BlightedSacks).
        items.add(BLIGHTED_SURGE_SACK, "Blighted surge sack", Kind.SIMPLE, "package", 0x6A3A8A).value(1)
                .examine("A sack of runes for surge and wave spells, usable in the Wilderness.");
    }
}
