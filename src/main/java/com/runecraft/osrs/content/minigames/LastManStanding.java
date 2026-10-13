package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Rune;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.osrs.data.EquipmentBonuses;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Last Man Standing (OSRS Wiki "Last Man Standing", "Chest (Last Man Standing)/Loot table", "Justine's stuff for the Last
 * Shopper Standing"): a battle royale entered by talking to Lisa in the Ferox Enclave lobby. The players' own items are put
 * aside for the game and every player gets the same account build (Max/Med, Zerker or 1 Def Pure) with its starting loadout.
 * Kills give a bloody key (a bloodier key with five or fewer players left) that opens the island's chests, a full restore of
 * health and supplies and 20 seconds of immunity. Lethal fog closes in on a safe zone at one of the Deserted Island's
 * landmarks. Dying is safe: nothing of the player's own is lost. Competitive games award points and change the player's rank;
 * points buy rewards from Justine, including the ring of wealth and magic shortbow imbue scrolls.
 *
 * <p>Items found on the island are separate items in OSRS ("(Last Man Standing)" versions with their own ids); here they are
 * the {@code lms_} copies defined below, taking the reference bonuses of the real item. They never leave the minigame: the
 * player's inventory is replaced by their own items when they leave.
 */
public final class LastManStanding {
    private LastManStanding() {
    }

    // ---------------------------------------------------------------- lobby (Ferox Enclave)
    /** The lobby inside the Ferox Enclave: Lisa and Justine stand at 3142, 3635 (OSRS Wiki maps). */
    public static final String PLACE = "mg_ferox_enclave_lms";
    public static final int LOBBY_X = 3142;
    public static final int LOBBY_Y = 3636;
    /** The Deserted Island instance (centre of the wiki's chest locations, mapID 36). */
    public static final int ISLAND_X = 3455;
    public static final int ISLAND_Y = 5822;
    public static final String LISA = "mg_lisa";
    public static final String JUSTINE = "mg_justine";
    public static final String SHOP = "last_man_standing";
    public static final String POINTS = "pts:last_man_standing";
    public static final String COMPETITIVE_ENTRANCE = "lms_competitive_entrance";
    public static final String CHEST = "lms_chest";
    public static final String CRATE = "lms_crate";
    public static final String ROWBOAT = "lms_rowboat";
    public static final String FOG_MARKER = "lms_fog_marker";
    public static final String BLOODY_KEY = "bloody_key";
    public static final String BLOODIER_KEY = "bloodier_key";
    /** MinigameData stats. */
    public static final String RANK = "lms_rank";
    public static final String WINS = "lms_wins";
    public static final String CASUAL_WINS = "lms_casual_wins";
    public static final String KILLS = "lms_kills";
    public static final String BUILD_PREFERENCE = "lms_build_pref";

    /** Every rank starts at 500. */
    public static final int START_RANK = 500;

    public enum Mode {
        /** Casual: no rewards, starts with four players. */
        CASUAL(4),
        /** Competitive: points and rank. OSRS needs 24 players; a Minecraft server rarely has that many. // approx */
        COMPETITIVE(4);

        public final int minPlayers;

        Mode(int minPlayers) {
            this.minPlayers = minPlayers;
        }

        public static Mode of(String entrance) {
            return COMPETITIVE_ENTRANCE.equals(entrance) ? COMPETITIVE : CASUAL;
        }
    }

    /** OSRS needs at most 24 players a game. */
    public static final int MAX_PLAYERS = 24;
    /** Competitive and high stakes: total level 750 and 30 quest points, or total level 1,500. */
    public static boolean competitiveAllowed(int totalLevel, int questPoints) {
        return totalLevel >= 1500 || (totalLevel >= 750 && questPoints >= 30);
    }

    // ---------------------------------------------------------------- timings (game ticks, 0.6 s)
    /** The game begins 12 seconds after the players arrive. */
    public static final int START_TICKS = 20;
    /** After a minute the safe zone is announced. */
    public static final int FOG_WARNING_TICK = 100;
    /** Fog damages players outside the safe zone from 2 minutes 30. */
    public static final int FOG_TICK = 250;
    /** From 10 minutes, anyone outside the final safe zone takes rapid damage. */
    public static final int FINAL_TICK = 1000;
    /** The game is forcibly ended after 20 minutes (since April 2026). */
    public static final int GAME_LIMIT_TICKS = 2000;
    /** 20 seconds unattackable after a kill. */
    public static final int IMMUNITY_TICKS = 33;
    /** Players this many tiles or more into the fog take greater fog damage. */
    public static final int GREATER_FOG_DEPTH = 16;
    /** Safe zone radius in tiles at the start of the fog and at its smallest. // approx */
    public static final int FOG_START_RADIUS = 90;
    public static final int FOG_FINAL_RADIUS = 8;
    /** A loot crate appears every 90 seconds while fewer than two are on the island. // approx: the wiki gives no rate */
    public static final int CRATE_INTERVAL = 150;
    /** Chance a crate rolls the upgraded offensive table. // approx */
    public static final double CRATE_UPGRADED_CHANCE = 0.25;

    /** Safe zone radius (tiles) {@code tick} game ticks into the game: it shrinks steadily from the fog's start to 10 minutes. */
    public static int safeRadius(long tick) {
        if (tick < FOG_TICK) {
            return FOG_START_RADIUS + 40;
        }
        if (tick >= FINAL_TICK) {
            return FOG_FINAL_RADIUS;
        }
        double f = (double) (tick - FOG_TICK) / (FINAL_TICK - FOG_TICK);
        return (int) Math.round(FOG_START_RADIUS + (FOG_FINAL_RADIUS - FOG_START_RADIUS) * f);
    }

    /**
     * Fog damage for one game tick (OSRS Wiki "Fog"): players less than 16 tiles into the fog take 1-3 every other tick, ramping
     * up over 6 ticks; 16 or more tiles in, greater fog deals up to 10 every other tick; after 10 minutes anyone outside the safe
     * zone takes about 1 a tick, rising steadily to 10 a tick.
     *
     * @param depth      tiles into the fog (0 or less: safe)
     * @param ticksInFog consecutive ticks spent in the fog
     */
    public static int fogDamage(long tick, int depth, int ticksInFog, Random random) {
        if (depth <= 0 || tick < FOG_TICK) {
            return 0;
        }
        if (tick >= FINAL_TICK) {
            return (int) Math.min(10, 1 + (tick - FINAL_TICK) / 10); // approx: the ramp's pace is not documented
        }
        if (ticksInFog % 2 != 0) {
            return 0;
        }
        if (depth >= GREATER_FOG_DEPTH) {
            return 1 + random.nextInt(10);
        }
        int cap = 1 + Math.min(2, ticksInFog / 3);
        return 1 + random.nextInt(cap);
    }

    // ---------------------------------------------------------------- keys, points and rank
    /** A kill with five or fewer players left gives a bloodier key, otherwise a bloody key. */
    public static String keyFor(int playersRemaining) {
        return playersRemaining <= 5 ? BLOODIER_KEY : BLOODY_KEY;
    }

    /** Competitive points for a placement (since April 2026): 1st 6, 2nd 5, 3rd-4th 4, 5th-9th 3, 10th-19th 1. */
    public static int placementPoints(int place) {
        if (place == 1) {
            return 6;
        }
        if (place == 2) {
            return 5;
        }
        if (place <= 4) {
            return 4;
        }
        if (place <= 9) {
            return 3;
        }
        return place <= 19 ? 1 : 0;
    }

    /** Kill bonus (they don't stack): 3 kills 2 points, 5 kills 3 points. */
    public static int killPoints(int kills) {
        return kills >= 5 ? 3 : kills >= 3 ? 2 : 0;
    }

    /** Points for a competitive game: nothing without at least one kill. At most 9. */
    public static int points(int place, int kills) {
        return kills < 1 ? 0 : placementPoints(place) + killPoints(kills);
    }

    /** Rank change: 1st-9th +(10 - place), 10th-16th none, 17th-24th -(place - 16). */
    public static int rankChange(int place) {
        if (place <= 9) {
            return 10 - place;
        }
        return place <= 16 ? 0 : -(place - 16);
    }

    public static String rankTitle(int rank) {
        return rank >= 1000 ? "Legend" : rank >= 750 ? "Master" : rank >= 600 ? "Veteran" : "Novice";
    }

    // ---------------------------------------------------------------- account builds and loadouts
    /** A build's combat stats (Attack, Strength, Defence; Magic, Ranged and Hitpoints are always 99). */
    public enum Build {
        MAX_MED("Max/Med", 99, 99, 85),
        ZERKER("Zerker", 80, 99, 50),
        PURE("1 Def Pure", 75, 99, 1);

        public final String displayName;
        public final int attack;
        public final int strength;
        public final int defence;

        Build(String displayName, int attack, int strength, int defence) {
            this.displayName = displayName;
            this.attack = attack;
            this.strength = strength;
            this.defence = defence;
        }

        /**
         * Whether the build may use {@code prayer}. No build may use Rapid Restore, Rapid Heal, Protect Item, Retribution,
         * Redemption, Smite or Preserve; Zerker and 1 Def Pure can't use Chivalry, Piety, Rigour or Augury (their Defence is
         * too low). Everything else is open without the usual quest or scroll unlocks.
         */
        public boolean allows(Prayer prayer) {
            if (BANNED_PRAYERS.contains(prayer)) {
                return false;
            }
            return this == MAX_MED || !DEFENCE_PRAYERS.contains(prayer);
        }

        /** The level this build plays at. Prayer is 99 too (with infinite prayer; see {@link #allows(Prayer)}). */
        public int level(Skill skill) {
            return switch (skill) {
                case ATTACK -> attack;
                case STRENGTH -> strength;
                case DEFENCE -> defence;
                case MAGIC, RANGED, HITPOINTS, PRAYER -> 99;
                default -> 0;
            };
        }

        public static Build of(int ordinal) {
            Build[] v = values();
            return v[Math.floorMod(ordinal, v.length)];
        }
    }

    /** Prayers no Last Man Standing build may use (OSRS Wiki). */
    public static final java.util.Set<Prayer> BANNED_PRAYERS = java.util.EnumSet.of(Prayer.RAPID_RESTORE, Prayer.RAPID_HEAL,
            Prayer.PROTECT_ITEM, Prayer.RETRIBUTION, Prayer.REDEMPTION, Prayer.SMITE, Prayer.PRESERVE);
    /** Prayers only the Max/Med build may use (Zerker and 1 Def Pure lack the Defence). */
    public static final java.util.Set<Prayer> DEFENCE_PRAYERS = java.util.EnumSet.of(Prayer.CHIVALRY, Prayer.PIETY, Prayer.RIGOUR,
            Prayer.AUGURY);

    /** The message shown when the build can't use {@code prayer}, or null if it can. */
    public static String prayerBlocked(Build build, Prayer prayer) {
        if (build.allows(prayer)) {
            return null;
        }
        return BANNED_PRAYERS.contains(prayer) ? prayer.displayName() + " can't be used in Last Man Standing."
                : "The " + build.displayName + " build can't use " + prayer.displayName() + ".";
    }

    /** The combat skills a build sets for the game. */
    public static final List<Skill> BUILD_SKILLS = List.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.MAGIC, Skill.RANGED,
            Skill.HITPOINTS, Skill.PRAYER);

    /**
     * The same build is chosen for everyone, with a higher chance for builds more players prefer: each build weighs one plus its
     * votes. // approx: the weighting isn't published
     */
    public static Build chooseBuild(Map<Build, Integer> votes, Random random) {
        int total = 0;
        for (Build b : Build.values()) {
            total += 1 + votes.getOrDefault(b, 0);
        }
        int roll = random.nextInt(total);
        for (Build b : Build.values()) {
            roll -= 1 + votes.getOrDefault(b, 0);
            if (roll < 0) {
                return b;
            }
        }
        return Build.MAX_MED;
    }

    /** An item and amount ("id" or "id:count"). */
    public record Stack(String item, int count) {
        static Stack parse(String s) {
            int i = s.indexOf(':');
            return i < 0 ? new Stack(s, 1) : new Stack(s.substring(0, i), Integer.parseInt(s.substring(i + 1)));
        }
    }

    private static List<Stack> stacks(String... items) {
        List<Stack> list = new ArrayList<>();
        for (String s : items) {
            list.add(Stack.parse(s));
        }
        return List.copyOf(list);
    }

    /**
     * Supplies (refilled after every kill): 10 sharks, 3 Saradomin brews, 2 sanfew serums and one of every other potion. Sanfew
     * serums are super restores here and the super combat potion is a super attack plus a super strength. // approx
     * The rune pouch's runes (Ice/Blood Barrage, Vengeance) are given loose.
     */
    public static final List<Stack> SUPPLIES = stacks("shark:10", "saradomin_brew:3", "super_restore:3", "ranging_potion",
            "super_attack", "super_strength", "lms_cooked_karambwan:2", Rune.DEATH.itemId() + ":400", Rune.BLOOD.itemId() + ":400",
            Rune.WATER.itemId() + ":600", Rune.SOUL.itemId() + ":100", Rune.ASTRAL.itemId() + ":100", Rune.EARTH.itemId() + ":500");

    /** Items that are supplies (not dropped for the killer, refilled after kills). */
    public static boolean isSupply(String item) {
        for (Stack s : SUPPLIES) {
            if (s.item().equals(item)) {
                return true;
            }
        }
        return item.equals(BLOODY_KEY) || item.equals(BLOODIER_KEY);
    }

    /** Starting gear: what is worn (armour slots, weapon, shield, cape, neck, hands, ring) and what is carried. */
    public record Loadout(List<String> worn, List<Stack> carried) {
    }

    /** Starting loadouts since the June 2026 update (OSRS Wiki "Last Man Standing", Equipment). */
    public static Loadout loadout(Build build) {
        return switch (build) {
            case MAX_MED -> new Loadout(List.of("lms_helm_of_neitiznot", "lms_imbued_guthix_cape", "lms_amulet_of_glory",
                    "lms_abyssal_whip", "lms_karils_leathertop", "lms_torags_platelegs", "lms_dragon_defender", "lms_barrows_gloves",
                    "lms_avernic_treads", "lms_berserker_ring_i"),
                    stacks("lms_opal_dragon_bolts_e:500", "lms_dragon_claws", "lms_dragon_crossbow", "lms_staff_of_the_dead",
                            "lms_spirit_shield", "lms_ahrims_robetop", "lms_ahrims_robeskirt"));
            case ZERKER -> new Loadout(List.of("lms_berserker_helm", "lms_imbued_guthix_cape", "lms_amulet_of_glory",
                    "lms_abyssal_whip", "lms_black_dhide_body", "lms_rune_platelegs", "lms_rune_defender", "lms_barrows_gloves",
                    "lms_climbing_boots", "lms_berserker_ring_i"),
                    stacks("lms_diamond_bolts_e:500", "lms_dragon_dagger", "lms_rune_crossbow", "lms_ahrims_staff", "lms_spirit_shield",
                            "lms_mystic_robe_top", "lms_mystic_robe_bottom"));
            case PURE -> new Loadout(List.of("lms_ghostly_hood", "lms_imbued_guthix_cape", "lms_occult_necklace", "lms_ancient_staff",
                    "lms_ghostly_robe_top", "lms_ghostly_robe_bottom", "lms_unholy_book", "lms_mithril_gloves", "lms_climbing_boots",
                    "lms_berserker_ring_i"),
                    stacks("lms_diamond_bolts_e:500", "lms_dragon_dagger", "lms_rune_crossbow", "lms_dragon_scimitar",
                            "lms_black_dhide_chaps", "lms_amulet_of_glory"));
        };
    }

    // ---------------------------------------------------------------- chests and crates
    public enum Table { OFFENSIVE, DEFENSIVE, UPGRADED }

    private static final Map<Build, Map<Table, List<List<Stack>>>> LOOT = new LinkedHashMap<>();

    private static void loot(Build b, Table t, String... bundles) {
        List<List<Stack>> list = new ArrayList<>();
        for (String bundle : bundles) {
            list.add(stacks(bundle.split(",")));
        }
        LOOT.computeIfAbsent(b, k -> new LinkedHashMap<>()).put(t, List.copyOf(list));
    }

    static {
        // Max/Med (the Oathplate helm and the moon armour sets aren't in RuneCraft).
        loot(Build.MAX_MED, Table.OFFENSIVE, "lms_armadyl_crossbow,lms_opal_dragon_bolts_e:100", "lms_armadyl_godsword",
                "lms_bow_of_faerdhinen,lms_crystal_helm,lms_crystal_body,lms_crystal_legs", "lms_dragon_claws",
                "lms_heavy_ballista,lms_dragon_javelin:100", "lms_magus_ring", "lms_noxious_halberd", "lms_occult_necklace",
                "lms_osmumtens_fang", "lms_purging_staff", "lms_ultor_ring", "lms_volatile_nightmare_staff");
        loot(Build.MAX_MED, Table.DEFENSIVE, "lms_amulet_of_fury", "lms_blessed_spirit_shield",
                "lms_dharoks_helm,lms_dharoks_platebody,lms_dharoks_platelegs,lms_dharoks_greataxe", "lms_dragonfire_shield",
                "lms_infernal_cape", "lms_masori_body_f", "lms_masori_chaps_f", "lms_mithril_seeds:10", "lms_torva_platelegs",
                "lms_twisted_buckler", "lms_virtus_robe_top", "lms_virtus_robe_bottom");
        loot(Build.MAX_MED, Table.UPGRADED, "lms_lightbearer", "lms_morrigans_javelin:1000", "lms_statiuss_warhammer",
                "lms_vestas_longsword", "lms_voidwaker", "lms_zuriels_staff", "lms_ancient_godsword",
                "lms_zaryte_crossbow,lms_opal_dragon_bolts_e:100");
        // Zerker (moon armour sets and the eclipse atlatl aren't in RuneCraft as LMS items).
        loot(Build.ZERKER, Table.OFFENSIVE, "lms_armadyl_crossbow,lms_opal_dragon_bolts_e:100", "lms_armadyl_godsword",
                "lms_burning_claws", "lms_dark_bow,lms_dragon_arrow:100", "lms_dragon_claws", "lms_elder_maul", "lms_granite_maul",
                "lms_heavy_ballista,lms_dragon_javelin:100", "lms_kodai_wand", "lms_staff_of_the_dead", "lms_magus_ring",
                "lms_noxious_halberd", "lms_ultor_ring", "lms_volatile_nightmare_staff");
        loot(Build.ZERKER, Table.DEFENSIVE, "lms_amulet_of_torture", "lms_mithril_seeds:10", "lms_necklace_of_anguish",
                "lms_tormented_bracelet", "lms_amulet_of_fury", "lms_aranea_boots", "lms_mages_book", "lms_occult_necklace",
                "lms_infernal_cape");
        loot(Build.ZERKER, Table.UPGRADED, "lms_lightbearer", "lms_morrigans_javelin:1000", "lms_vestas_longsword", "lms_voidwaker",
                "lms_zuriels_staff", "lms_dragon_claws", "lms_armadyl_crossbow,lms_opal_dragon_bolts_e:100", "lms_armadyl_godsword",
                "lms_kodai_wand");
        // 1 Def Pure.
        loot(Build.PURE, Table.OFFENSIVE, "lms_armadyl_crossbow,lms_opal_dragon_bolts_e:100", "lms_armadyl_godsword",
                "lms_burning_claws", "lms_dark_bow,lms_dragon_arrow:100", "lms_dragon_claws", "lms_elder_maul", "lms_granite_maul",
                "lms_kodai_wand", "lms_light_ballista,lms_dragon_javelin:100", "lms_magus_ring", "lms_staff_of_the_dead",
                "lms_ultor_ring", "lms_volatile_nightmare_staff");
        loot(Build.PURE, Table.DEFENSIVE, "lms_amulet_of_torture", "lms_aranea_boots",
                "lms_elder_chaos_hood,lms_elder_chaos_top,lms_elder_chaos_robe", "lms_guthix_chaps", "lms_infernal_cape",
                "lms_lightbearer", "lms_mages_book", "lms_necklace_of_anguish", "lms_tormented_bracelet");
        loot(Build.PURE, Table.UPGRADED, "lms_armadyl_crossbow,lms_opal_dragon_bolts_e:100", "lms_armadyl_godsword",
                "lms_dragon_claws", "lms_dragon_knife:1000", "lms_granite_maul", "lms_kodai_wand", "lms_morrigans_javelin:1000",
                "lms_staff_of_the_dead", "lms_vestas_longsword", "lms_voidwaker", "lms_zuriels_staff");
    }

    public static List<List<Stack>> table(Build build, Table table) {
        return LOOT.get(build).get(table);
    }

    /**
     * A chest opened with a bloody key rolls once on the offensive and once on the defensive table; a bloodier key rolls the
     * upgraded offensive table instead of the offensive one. // approx: the wiki only says bloodier keys "access" the upgraded table
     */
    public static List<Stack> openChest(Build build, boolean bloodier, Random random) {
        List<Stack> out = new ArrayList<>(pick(table(build, bloodier ? Table.UPGRADED : Table.OFFENSIVE), random));
        out.addAll(pick(table(build, Table.DEFENSIVE), random));
        return out;
    }

    /** A loot crate: one roll on the offensive or defensive table, sometimes the upgraded one. */
    public static List<Stack> openCrate(Build build, Random random) {
        if (random.nextDouble() < CRATE_UPGRADED_CHANCE) {
            return pick(table(build, Table.UPGRADED), random);
        }
        return pick(table(build, random.nextBoolean() ? Table.OFFENSIVE : Table.DEFENSIVE), random);
    }

    private static List<Stack> pick(List<List<Stack>> table, Random random) {
        return table.get(random.nextInt(table.size()));
    }

    // ---------------------------------------------------------------- the Deserted Island
    /** A named place on the island (OSRS tiles). Safe zones close in on one of them. */
    public record Landmark(String name, int x, int y) {
    }

    /** // approx: positions are the centres of the wiki's chest clusters; names from the in-game island map */
    public static final List<Landmark> LANDMARKS = List.of(
            new Landmark("Moser settlement", 3402, 5874),
            new Landmark("Trinity outpost", 3500, 5870),
            new Landmark("the mountain", 3452, 5840),
            new Landmark("the swamp", 3423, 5768),
            new Landmark("rocky outcrop", 3512, 5770),
            new Landmark("Debtor hideout", 3473, 5793),
            new Landmark("the western ruins", 3405, 5805));

    /** Chest tiles on the Deserted Island (OSRS Wiki "Chest (Last Man Standing)", mapID 36). */
    public static final int[][] CHESTS = {
            {3453, 5870}, {3448, 5816}, {3479, 5798}, {3454, 5840}, {3404, 5878}, {3516, 5770}, {3489, 5865}, {3396, 5843},
            {3472, 5799}, {3499, 5881}, {3408, 5807}, {3493, 5860}, {3465, 5788}, {3400, 5847}, {3415, 5810}, {3470, 5789},
            {3431, 5857}, {3479, 5790}, {3452, 5843}, {3458, 5870}, {3407, 5874}, {3431, 5770}, {3413, 5808}, {3446, 5810},
            {3501, 5881}, {3469, 5798}, {3416, 5767}, {3397, 5858}, {3489, 5863}, {3421, 5854}, {3427, 5774}, {3416, 5764},
            {3503, 5878}, {3451, 5814}, {3514, 5773}, {3510, 5768}, {3428, 5849}, {3482, 5789}, {3467, 5785}, {3497, 5878},
            {3402, 5872}, {3491, 5860}, {3397, 5796}, {3509, 5879}, {3511, 5870}, {3509, 5866}, {3398, 5800}, {3495, 5863},
            {3450, 5843}, {3394, 5854}, {3476, 5799}, {3509, 5831}, {3448, 5840}, {3512, 5773}, {3419, 5763}};

    /** Arena block offset (x, z) of an OSRS island tile: one block per two tiles, north is -z. // approx: the island is compacted */
    public static int[] offset(int x, int y) {
        return new int[]{Math.floorDiv(x - ISLAND_X, 2), -Math.floorDiv(y - ISLAND_Y, 2)};
    }

    /** The landmark nearest an OSRS island tile (for crate broadcasts). */
    public static Landmark nearest(int x, int y) {
        Landmark best = LANDMARKS.get(0);
        long bestD = Long.MAX_VALUE;
        for (Landmark l : LANDMARKS) {
            long d = (long) (l.x() - x) * (l.x() - x) + (long) (l.y() - y) * (l.y() - y);
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- rewards (Justine)
    /** Victor's capes are claimed free from Justine once the player has this many competitive wins. */
    public static final int[] VICTOR_WINS = {1, 10, 50, 100, 500, 1000};
    /** Tradeable rewards need at least one won game (since May 2026). */
    public static final List<String> WIN_LOCKED = List.of("ring_of_wealth_scroll", "magic_shortbow_scroll", "adamant_arrow",
            "rune_arrow");

    /** Competitive wins needed to buy a reward (0 for none). */
    public static int winsNeeded(String item) {
        for (int w : VICTOR_WINS) {
            if (item.equals("victors_cape_" + w)) {
                return w;
            }
        }
        return WIN_LOCKED.contains(item) ? 1 : 0;
    }

    /** Justine's stuff for the Last Shopper Standing (the items RuneCraft has; prices in points). */
    static void shop(RewardShop shop) {
        shop.item("ring_of_wealth_scroll", POINTS, 5)
                .item("magic_shortbow_scroll", POINTS, 5)
                .item("adamant_arrow", 350, POINTS, 1)
                .item("rune_arrow", 300, POINTS, 3)
                .item("deadmans_chest", POINTS, 160)
                .item("deadmans_legs", POINTS, 160)
                .item("deadmans_cape", POINTS, 160);
        for (String halo : HALOS) {
            shop.item(halo + "_halo", POINTS, 450);
        }
    }

    /** The god halos sold by Justine. */
    public static final List<String> HALOS = List.of("armadyl", "bandos", "seren", "ancient", "brassica");

    /** Imbue scrolls: scroll, item (any charge state of it) and the imbued result. Used directly on the item. */
    public static final Map<String, String[]> IMBUE_SCROLLS = Map.of(
            "ring_of_wealth_scroll", new String[]{"ring_of_wealth", "ring_of_wealth_i"},
            "magic_shortbow_scroll", new String[]{"magic_shortbow", "magic_shortbow_i"});

    /**
     * The imbued id for {@code item} under {@code scroll}, or null. Charged rings keep their charges: ring_of_wealth_3 becomes
     * ring_of_wealth_i_3.
     */
    public static String imbued(String scroll, String item) {
        String[] pair = IMBUE_SCROLLS.get(scroll);
        if (pair == null) {
            return null;
        }
        if (item.equals(pair[0])) {
            return pair[1];
        }
        if (item.startsWith(pair[0] + "_") && item.substring(pair[0].length() + 1).matches("[1-9]")) {
            return pair[1] + item.substring(pair[0].length());
        }
        return null;
    }

    public static List<String> collectionLog() {
        List<String> list = new ArrayList<>(List.of("deadmans_chest", "deadmans_legs", "deadmans_cape"));
        for (String halo : HALOS) {
            list.add(halo + "_halo");
        }
        for (int w : VICTOR_WINS) {
            list.add("victors_cape_" + w);
        }
        list.addAll(List.of("ring_of_wealth_scroll", "magic_shortbow_scroll"));
        return list;
    }

    // ---------------------------------------------------------------- items
    private static EquipmentStats s(int sa, int sl, int cr, int ma, int ra, int sd, int sld, int cd, int md, int rd,
                                    int str, int rstr, int mdmg, int prayer) {
        return new EquipmentStats(sa, sl, cr, ma, ra, sd, sld, cd, md, rd, str, rstr, mdmg, prayer);
    }

    /**
     * Bonuses of items RuneCraft's reference table lacks (weirdgloop osrs-dps-calc equipment.json; the (bh) activated
     * versions for Vesta's and Zuriel's).
     */
    private static final Map<String, EquipmentStats> FALLBACK = Map.ofEntries(
            Map.entry("masori_body_f", s(0, 0, 0, -4, 43, 59, 52, 64, 74, 60, 0, 4, 0, 1)),
            Map.entry("masori_chaps_f", s(0, 0, 0, -2, 27, 35, 30, 39, 46, 37, 0, 2, 0, 1)),
            Map.entry("elder_chaos_hood", s(0, 0, 0, 5, 0, 0, 0, 0, 4, 0, 0, 0, 1, 0)),
            Map.entry("elder_chaos_top", s(0, 0, 0, 10, 0, 0, 0, 0, 8, 0, 0, 0, 1, 0)),
            Map.entry("elder_chaos_robe", s(0, 0, 0, 6, 0, 0, 0, 0, 6, 0, 0, 0, 1, 0)),
            Map.entry("aranea_boots", s(0, 0, 0, 5, 6, 0, 0, 0, 0, 0, 4, 0, 0, 1)),
            Map.entry("mithril_gloves", s(6, 6, 6, 3, 6, 6, 6, 6, 3, 6, 6, 0, 0, 0)),
            Map.entry("zuriels_staff", s(13, -1, 65, 18, 0, 5, 7, 4, 18, 0, 72, 0, 10, 0)),
            Map.entry("vestas_longsword", s(106, 121, -2, 0, 0, 1, 4, 3, 0, 0, 118, 0, 0, 0)),
            Map.entry("statiuss_warhammer", s(-4, -4, 123, 0, 0, 0, 0, 0, 0, 0, 114, 0, 0, 0))); // approx (Statius's)

    /**
     * LMS copies: base id | name | kind | shape | colour | slot (H C L F S, W weapon, cape/neck/ring/hands, A ammo) | attack
     * type | speed. Bonuses (and weapon speed when known) come from the base item's reference row.
     */
    private static final String[] COPIES = {
            "helm_of_neitiznot|Helm of neitiznot|ARMOUR|full_helm|8A8AA0|H", "berserker_helm|Berserker helm|ARMOUR|full_helm|8A8A9A|H",
            "ghostly_hood|Ghostly hood|ARMOUR|cowl|D8E8F0|H", "crystal_helm|Crystal helm|ARMOUR|full_helm|9AD8F0|H",
            "dharoks_helm|Dharok's helm|ARMOUR|full_helm|6A6A60|H", "elder_chaos_hood|Elder chaos hood|ARMOUR|cowl|3A3A4A|H",
            "karils_leathertop|Karil's leathertop|ARMOUR|leather_body|4A3A2A|C", "black_dhide_body|Black d'hide body|ARMOUR|leather_body|2A2A2A|C",
            "ahrims_robetop|Ahrim's robetop|ARMOUR|leather_body|3A3A4A|C", "mystic_robe_top|Mystic robe top|ARMOUR|leather_body|3A5AB0|C",
            "ghostly_robe_top|Ghostly robe top|ARMOUR|leather_body|D8E8F0|C", "crystal_body|Crystal body|ARMOUR|platebody|9AD8F0|C",
            "dharoks_platebody|Dharok's platebody|ARMOUR|platebody|6A6A60|C", "masori_body_f|Masori body (f)|ARMOUR|leather_body|C0A060|C",
            "virtus_robe_top|Virtus robe top|ARMOUR|leather_body|3A2A5A|C", "elder_chaos_top|Elder chaos top|ARMOUR|leather_body|3A3A4A|C",
            "torags_platelegs|Torag's platelegs|ARMOUR|platelegs|5A4A3A|L", "rune_platelegs|Rune platelegs|ARMOUR|platelegs|4FB8C8|L",
            "ahrims_robeskirt|Ahrim's robeskirt|ARMOUR|chaps|3A3A4A|L", "mystic_robe_bottom|Mystic robe bottom|ARMOUR|chaps|3A5AB0|L",
            "ghostly_robe_bottom|Ghostly robe bottom|ARMOUR|chaps|D8E8F0|L", "black_dhide_chaps|Black d'hide chaps|ARMOUR|chaps|2A2A2A|L",
            "crystal_legs|Crystal legs|ARMOUR|platelegs|9AD8F0|L", "dharoks_platelegs|Dharok's platelegs|ARMOUR|platelegs|6A6A60|L",
            "masori_chaps_f|Masori chaps (f)|ARMOUR|chaps|C0A060|L", "torva_platelegs|Torva platelegs|ARMOUR|platelegs|6A2A2A|L",
            "virtus_robe_bottom|Virtus robe bottom|ARMOUR|chaps|3A2A5A|L", "elder_chaos_robe|Elder chaos robe|ARMOUR|chaps|3A3A4A|L",
            "guthix_chaps|Guthix chaps|ARMOUR|chaps|30A030|L",
            "avernic_treads|Avernic treads|ARMOUR|boots|8A1A2A|F", "climbing_boots|Climbing boots|ARMOUR|boots|6A4A2A|F",
            "aranea_boots|Aranea boots|ARMOUR|boots|4A3A5A|F",
            "dragon_defender|Dragon defender|SHIELD|kiteshield|C03020|S", "rune_defender|Rune defender|SHIELD|kiteshield|4FB8C8|S",
            "spirit_shield|Spirit shield|SHIELD|spirit_shield|C0C0D0|S", "blessed_spirit_shield|Blessed spirit shield|SHIELD|spirit_shield|E0E0F0|S",
            "unholy_book|Unholy book|SHIELD|book|A02020|S", "dragonfire_shield|Dragonfire shield|SHIELD|kiteshield|8A3A2A|S",
            "twisted_buckler|Twisted buckler|SHIELD|buckler|6A5A3A|S", "mages_book|Mage's book|SHIELD|book|6A4AA0|S",
            "abyssal_whip|Abyssal whip|MELEE_WEAPON|whip|5A2A2A|W|SLASH|4", "dragon_claws|Dragon claws|MELEE_WEAPON|claws|B02A1A|W|SLASH|4",
            "dragon_dagger|Dragon dagger|MELEE_WEAPON|dagger|B02020|W|STAB|4", "dragon_scimitar|Dragon scimitar|MELEE_WEAPON|scimitar|B02020|W|SLASH|4",
            "armadyl_godsword|Armadyl godsword|MELEE_WEAPON|godsword|C0C8D8|W|SLASH|6", "noxious_halberd|Noxious halberd|MELEE_WEAPON|halberd|6A8A3A|W|SLASH|5",
            "osmumtens_fang|Osmumten's fang|MELEE_WEAPON|fang|E0D8B0|W|STAB|5", "dharoks_greataxe|Dharok's greataxe|MELEE_WEAPON|battleaxe|6A6A60|W|SLASH|7",
            "vestas_longsword|Vesta's longsword|MELEE_WEAPON|longsword|C0A040|W|SLASH|5", "statiuss_warhammer|Statius's warhammer|MELEE_WEAPON|warhammer|C0A040|W|CRUSH|6",
            "voidwaker|Voidwaker|MELEE_WEAPON|longsword|6A4AB0|W|SLASH|4", "ancient_godsword|Ancient godsword|MELEE_WEAPON|godsword|6A3A8A|W|SLASH|6",
            "burning_claws|Burning claws|MELEE_WEAPON|claws|E05020|W|SLASH|4", "elder_maul|Elder maul|MELEE_WEAPON|maul|8A8A7A|W|CRUSH|6",
            "granite_maul|Granite maul|MELEE_WEAPON|warhammer|7A7A7A|W|CRUSH|7",
            "dragon_crossbow|Dragon crossbow|BOW|crossbow|B02020|W|RANGED|6", "rune_crossbow|Rune crossbow|BOW|crossbow|4FB8C8|W|RANGED|6",
            "armadyl_crossbow|Armadyl crossbow|BOW|crossbow|C0C8D8|W|RANGED|6", "zaryte_crossbow|Zaryte crossbow|BOW|crossbow|7A4A9A|W|RANGED|6",
            "heavy_ballista|Heavy ballista|BOW|crossbow|5A4A3A|W|RANGED|7", "light_ballista|Light ballista|BOW|crossbow|8A6A3A|W|RANGED|7",
            "dark_bow|Dark bow|BOW|longbow|2A2A3A|W|RANGED|9", "bow_of_faerdhinen|Bow of faerdhinen|BOW|crystal_bow|9AD8F0|W|RANGED|5",
            "staff_of_the_dead|Staff of the dead|STAFF|staff_orb|205020|W|MAGIC|4", "ahrims_staff|Ahrim's staff|STAFF|staff_orb|3A3A4A|W|MAGIC|6",
            "ancient_staff|Ancient staff|STAFF|staff_orb|6A3A8A|W|MAGIC|4", "kodai_wand|Kodai wand|STAFF|wand|3A8AC0|W|MAGIC|4",
            "zuriels_staff|Zuriel's staff|STAFF|staff_orb|C0A040|W|MAGIC|5", "purging_staff|Purging staff|STAFF|staff_orb|E05020|W|MAGIC|4",
            "volatile_nightmare_staff|Volatile nightmare staff|STAFF|staff_orb|C03A3A|W|MAGIC|5",
            "imbued_guthix_cape|Imbued guthix cape|SIMPLE|cape|2A8A2A|cape", "infernal_cape|Infernal cape|SIMPLE|cape|FF6A10|cape",
            "amulet_of_glory|Amulet of glory|SIMPLE|amulet|A040C0|neck", "occult_necklace|Occult necklace|SIMPLE|necklace|8A2A8A|neck",
            "amulet_of_fury|Amulet of fury|SIMPLE|amulet|E0A030|neck", "amulet_of_torture|Amulet of torture|SIMPLE|amulet|C03020|neck",
            "necklace_of_anguish|Necklace of anguish|SIMPLE|necklace|30A040|neck",
            "berserker_ring_i|Berserker ring (i)|SIMPLE|ring|8A2A2A|ring", "magus_ring|Magus ring|SIMPLE|ring|3A3AA0|ring",
            "ultor_ring|Ultor ring|SIMPLE|ring|A02020|ring", "lightbearer|Lightbearer|SIMPLE|ring|F0E080|ring",
            "barrows_gloves|Barrows gloves|SIMPLE|gloves|7A4A2A|hands", "mithril_gloves|Mithril gloves|SIMPLE|gloves|9090C0|hands",
            "tormented_bracelet|Tormented bracelet|SIMPLE|bracelet|F0A020|hands",
            "opal_dragon_bolts_e|Opal dragon bolts (e)|ARROW|bolts|E0E0F0|A|122", "diamond_bolts_e|Diamond bolts (e)|ARROW|bolts|F0F0FF|A|105",
            "dragon_arrow|Dragon arrow|ARROW|arrow|B02020|A|60", "dragon_javelin|Dragon javelin|ARROW|javelin|B02020|A|150",
            "dragon_knife|Dragon knife|ARROW|throwing_knife|B02020|A|35", "morrigans_javelin|Morrigan's javelin|ARROW|javelin|C0A040|A|145",
    };

    /** The base item an LMS copy stands for ("lms_abyssal_whip" -> "abyssal_whip"), or the id itself. */
    public static String base(String id) {
        return id.startsWith("lms_") ? id.substring(4) : id;
    }

    public static void items(ItemSink items) {
        for (String row : COPIES) {
            String[] f = row.split("\\|");
            String base = f[0];
            String id = "lms_" + base;
            String name = f[1] + " (Last Man Standing)";
            Kind kind = Kind.valueOf(f[2]);
            int color = Integer.parseInt(f[4], 16);
            String slot = f[5];
            String examine = "A Last Man Standing item. It would vanish outside the minigame.";
            EquipmentStats stats = EquipmentBonuses.statsOr(base, FALLBACK.getOrDefault(base, EquipmentStats.NONE));
            switch (slot) {
                case "H", "C", "L", "F", "S" -> {
                    Slot eqSlot = switch (slot) {
                        case "H" -> Slot.HEAD;
                        case "C" -> Slot.CHEST;
                        case "L" -> Slot.LEGS;
                        case "F" -> Slot.FEET;
                        default -> Slot.SHIELD;
                    };
                    items.add(id, name, kind, f[3], color).value(1).stack(1).examine(examine)
                            .equipment(new EquipmentDef(eqSlot, stats, Map.of(), 0, AttackType.CRUSH, WeaponClass.NONE));
                }
                case "W" -> {
                    AttackType type = AttackType.valueOf(f[6]);
                    EquipmentBonuses.Entry ref = EquipmentBonuses.get(base);
                    int speed = ref != null && ref.speed() > 0 ? ref.speed() : Integer.parseInt(f[7]);
                    WeaponClass cls = kind == Kind.BOW ? WeaponClass.BOW : kind == Kind.STAFF ? WeaponClass.STAFF : WeaponClass.MELEE;
                    items.add(id, name, kind, f[3], color).value(1).stack(1).examine(examine)
                            .equipment(new EquipmentDef(Slot.WEAPON, stats, Map.of(), speed, type, cls));
                }
                case "A" -> {
                    Integer ref = EquipmentBonuses.ammoStrength(base);
                    items.add(id, name, kind, f[3], color).value(1).examine(examine)
                            .rangedStrength(ref != null ? ref : Integer.parseInt(f[6]));
                }
                default -> {
                    items.add(id, name, kind, f[3], color).value(1).stack(1).examine(examine);
                    Jewellery.Slot worn = switch (slot) {
                        case "cape" -> Jewellery.Slot.CAPE;
                        case "neck" -> Jewellery.Slot.NECK;
                        case "ring" -> Jewellery.Slot.RING;
                        default -> Jewellery.Slot.HANDS;
                    };
                    Jewellery.Perk perk = base.equals("lightbearer") ? Jewellery.Perk.LIGHTBEARER : Jewellery.Perk.NONE;
                    Jewellery.register(new Jewellery.Worn(id, name, worn, stats, perk, null, -1, List.of(), false));
                }
            }
        }
        items.add("lms_cooked_karambwan", "Cooked karambwan (Last Man Standing)", Kind.FOOD, "fish_cooked", 0x4A8A3A).value(1)
                .heal(18).examine("Cooked octopus. It looks very nutritious.");
        items.add("lms_mithril_seeds", "Mithril seeds (Last Man Standing)", Kind.SIMPLE, "seed", 0x9090C0).value(1)
                .examine("Magical seeds in a mithril case.");
        items.add(BLOODY_KEY, "Bloody key", Kind.SIMPLE, "key", 0x8A1A1A).value(1).stack(1)
                .examine("A key stained in the blood of a fallen foe.");
        items.add(BLOODIER_KEY, "Bloodier key", Kind.SIMPLE, "key", 0xC01A1A).value(1).stack(1)
                .examine("A key stained in the blood of a worthy opponent.");

        // Rewards.
        items.add("ring_of_wealth_scroll", "Ring of wealth scroll", Kind.SIMPLE, "note", 0xE0C040).value(5000).stack(1)
                .examine("Use on a Ring of Wealth to imbue the ring.");
        items.add("magic_shortbow_scroll", "Magic shortbow scroll", Kind.SIMPLE, "note", 0x40A060).value(5000).stack(1)
                .examine("Use on a Magic shortbow to imbue the bow.");
        items.add("magic_shortbow_i", "Magic shortbow (i)", Kind.BOW, "shortbow", 0x3A6A9A).value(2000).stack(1)
                .equipment(new EquipmentDef(Slot.WEAPON, s(0, 0, 0, 0, 75, 0, 0, 0, 0, 0, 0, 0, 0, 0), Map.of(Skill.RANGED, 50), 4,
                        AttackType.RANGED, WeaponClass.BOW))
                .examine("Short and magical, but still effective. It has been imbued.");
        // The imbued ring of wealth keeps the ring's five teleport charges and the wealth perk.
        for (int n = 5; n >= 0; n--) {
            String id = n == 0 ? "ring_of_wealth_i" : "ring_of_wealth_i_" + n;
            String name = n == 0 ? "Ring of wealth (i)" : "Ring of wealth (i" + n + ")";
            items.add(id, name, Kind.SIMPLE, "ring", 0xB050D0).value(n == 0 ? 8812 : 17625).stack(1)
                    .examine(n == 0 ? "An enchanted ring with no charges left." : "An imbued ring of wealth with " + n + " charge"
                            + (n == 1 ? "" : "s") + " left.");
            Jewellery.register(new Jewellery.Worn(id, name, Jewellery.Slot.RING, EquipmentStats.NONE, Jewellery.Perk.WEALTH,
                    "ring_of_wealth_i", n, n == 0 ? List.of() : Teleports.WEALTH, false));
        }
        items.add("deadmans_chest", "Deadman's chest", Kind.ARMOUR, "platebody", 0x5A2A2A).value(1).stack(1)
                .equipment(new EquipmentDef(Slot.CHEST, s(0, 0, 0, 0, 0, 21, 20, 12, 3, 20, 0, 0, 0, 0), Map.of(), 0, AttackType.CRUSH, WeaponClass.NONE))
                .examine("A cosmetic Deadman Mode chestplate.");
        items.add("deadmans_legs", "Deadman's legs", Kind.ARMOUR, "platelegs", 0x5A2A2A).value(1).stack(1)
                .equipment(new EquipmentDef(Slot.LEGS, s(0, 0, 0, 0, 0, 11, 10, 10, -2, 10, 0, 0, 0, 0), Map.of(), 0, AttackType.CRUSH, WeaponClass.NONE))
                .examine("Cosmetic Deadman Mode legs.");
        items.add("deadmans_cape", "Deadman's cape", Kind.SIMPLE, "cape", 0x3A1A1A).value(1).stack(1)
                .examine("A cosmetic Deadman Mode cape.");
        Jewellery.register(new Jewellery.Worn("deadmans_cape", "Deadman's cape", Jewellery.Slot.CAPE,
                s(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0),
                Jewellery.Perk.NONE, null, -1, List.of(), false));
        for (int w : VICTOR_WINS) {
            String id = "victors_cape_" + w;
            items.add(id, "Victor's cape (" + w + ")", Kind.SIMPLE, "cape", 0xC0A040).value(1).stack(1)
                    .examine("Awarded for " + w + " Last Man Standing win" + (w == 1 ? "" : "s") + ".");
            Jewellery.register(new Jewellery.Worn(id, "Victor's cape (" + w + ")", Jewellery.Slot.CAPE, EquipmentStats.NONE,
                    Jewellery.Perk.NONE, null, -1, List.of(), false));
        }
        int[] haloColours = {0xE0E0F0, 0x6A5A3A, 0xE0F0FF, 0x8A3A6A, 0x6AA040};
        for (int i = 0; i < HALOS.size(); i++) {
            String god = HALOS.get(i);
            // Halo bonuses from equipment.json (all five share them). // approx: their Defence/Prayer requirements are not applied
            items.add(god + "_halo", Character.toUpperCase(god.charAt(0)) + god.substring(1) + " halo", Kind.ARMOUR, "full_helm",
                            haloColours[i]).value(1).stack(1)
                    .equipment(new EquipmentDef(Slot.HEAD, s(0, 0, 0, 0, 0, 11, 12, 10, 11, -1, 0, 0, 0, 3), Map.of(), 0,
                            AttackType.CRUSH, WeaponClass.NONE))
                    .examine("A halo from Last Man Standing.");
        }
    }
}
