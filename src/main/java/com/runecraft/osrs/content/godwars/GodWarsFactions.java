package com.runecraft.osrs.content.godwars;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * God Wars Dungeon factions: which followers count towards which god's kill count ("essence"), which general and
 * bodyguards belong to which god, the kill count needed to enter a general's chamber (40, as in OSRS) and the Ancient
 * Prison's Zarosian essence (OSRS Wiki "Kill count": spiritual warriors, rangers and Nex's mages 2, blood reavers 3,
 * spiritual mages and Nex 5; 40 to enter the refuge before Nex).
 */
public final class GodWarsFactions {
    /** Kill count needed before a general's door opens (OSRS: 40). Consumed on entry. */
    public static final int REQUIRED_KILLCOUNT = 40;
    /** Most essence a player can hold for one god (OSRS: the counter then shows MAX). */
    public static final int MAX_KILLCOUNT = 4000;
    /** Zarosian essence needed to pass the door into the refuge before Nex's chamber (lowered like the generals' doors). */
    public static final int REQUIRED_ZAROS_ESSENCE = 40;
    /** Pet roll used by the generic boss engine (1/3000); see {@link #extraPetChance}. */
    public static final int ENGINE_PET_RATE = 3000;

    public enum God {
        ARMADYL("Armadyl", 0x5A7AA0),
        BANDOS("Bandos", 0x6A7A3A),
        SARADOMIN("Saradomin", 0x3A5AD0),
        ZAMORAK("Zamorak", 0xB02020);

        private final String displayName;
        private final int color;

        God(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String displayName() {
            return displayName;
        }

        public int color() {
            return color;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Frozen key piece dropped by this god's general's bodyguards. */
        public String keyPiece() {
            return "frozen_key_piece_" + id();
        }

        /** Hilt dropped by this god's general. */
        public String hilt() {
            return id() + "_hilt";
        }

        /** Godsword made from this god's hilt. */
        public String godsword() {
            return id() + "_godsword";
        }
    }

    private static final Map<String, God> FOLLOWERS = new LinkedHashMap<>();
    private static final Map<String, God> GENERALS = new LinkedHashMap<>();
    private static final Map<String, God> BODYGUARDS = new LinkedHashMap<>();
    private static final Map<String, Integer> ZAROS_ESSENCE = new LinkedHashMap<>();

    static {
        follow(God.ARMADYL, "gwd_aviansie", "gwd_aviansie_elite", "gwd_spiritual_warrior_armadyl",
                "gwd_spiritual_ranger_armadyl", "gwd_spiritual_mage_armadyl");
        follow(God.BANDOS, "gwd_goblin", "gwd_hobgoblin", "gwd_ork", "gwd_ogre", "gwd_jogre", "gwd_cyclops",
                "gwd_spiritual_warrior_bandos", "gwd_spiritual_ranger_bandos", "gwd_spiritual_mage_bandos");
        follow(God.SARADOMIN, "gwd_knight_of_saradomin", "gwd_saradomin_priest", "gwd_spiritual_warrior_saradomin",
                "gwd_spiritual_ranger_saradomin", "gwd_spiritual_mage_saradomin");
        follow(God.ZAMORAK, "gwd_imp", "gwd_werewolf", "gwd_bloodveld", "gwd_hellhound", "gwd_gorak", "gwd_vampire",
                "gwd_pyrefiend", "gwd_spiritual_warrior_zamorak", "gwd_spiritual_ranger_zamorak", "gwd_spiritual_mage_zamorak",
                "gwd_icefiend");
        GENERALS.put("kreearra", God.ARMADYL);
        GENERALS.put("general_graardor", God.BANDOS);
        GENERALS.put("commander_zilyana", God.SARADOMIN);
        GENERALS.put("kril_tsutsaroth", God.ZAMORAK);
        guard(God.ARMADYL, "flight_kilisa", "wingman_skree", "flockleader_geerin");
        guard(God.BANDOS, "sergeant_strongstack", "sergeant_steelwill", "sergeant_grimspike");
        guard(God.SARADOMIN, "starlight", "growler", "bree");
        guard(God.ZAMORAK, "tstanon_karlak", "zakln_gritch", "balfrug_kreeyath");
        ZAROS_ESSENCE.put("gwd_spiritual_warrior_zaros", 2);
        ZAROS_ESSENCE.put("gwd_spiritual_ranger_zaros", 2);
        ZAROS_ESSENCE.put("gwd_spiritual_mage_zaros", 5);
        ZAROS_ESSENCE.put("gwd_blood_reaver", 3);
        ZAROS_ESSENCE.put("blood_reaver", 3);
        for (String mage : GwdBosses.NEX_MINIONS) {
            ZAROS_ESSENCE.put(mage, 2);
        }
        ZAROS_ESSENCE.put("nex", 5);
    }

    private GodWarsFactions() {
    }

    private static void follow(God god, String... ids) {
        for (String id : ids) {
            FOLLOWERS.put(id, god);
        }
    }

    private static void guard(God god, String... ids) {
        for (String id : ids) {
            BODYGUARDS.put(id, god);
        }
    }

    /** The god a follower creature belongs to, or null if it doesn't give God Wars kill count. */
    public static God followerGod(String creatureId) {
        return creatureId == null ? null : FOLLOWERS.get(creatureId);
    }

    /** The god whose kill count unlocks this general, or null. */
    public static God generalGod(String bossId) {
        return bossId == null ? null : GENERALS.get(bossId);
    }

    /** The god a general's bodyguard serves, or null. */
    public static God bodyguardGod(String creatureId) {
        return creatureId == null ? null : BODYGUARDS.get(creatureId);
    }

    /**
     * The god whose kill count a kill raises, or null: followers, bodyguards and the generals themselves all count (OSRS
     * Wiki: "The generals and their bodyguards are also counted as essence").
     */
    public static God essenceGod(String creatureId) {
        God g = followerGod(creatureId);
        if (g == null) {
            g = bodyguardGod(creatureId);
        }
        return g != null ? g : generalGod(creatureId);
    }

    /** Zarosian essence a kill in the Ancient Prison gives before the bonus for Zarosian items (0 for anything else). */
    public static int zarosEssence(String creatureId) {
        return creatureId == null ? 0 : ZAROS_ESSENCE.getOrDefault(creatureId, 0);
    }

    /**
     * Extra Zarosian essence for {@code zarosItems} Zarosian items worn (OSRS Wiki, Mod Ash: 20% per item, five items
     * guarantee one extra and the excess rolls for a second); {@code roll} is uniform in [0, 1).
     */
    public static int zarosBonus(int zarosItems, double roll) {
        int n = Math.max(0, zarosItems);
        return n / 5 + (roll < (n % 5) * 0.2 ? 1 : 0);
    }

    /** A counter after {@code amount} more essence, capped at {@link #MAX_KILLCOUNT}. */
    public static int addKill(int killcount, int amount) {
        return Math.min(MAX_KILLCOUNT, Math.max(0, killcount) + Math.max(0, amount));
    }

    public static Map<String, God> followers() {
        return Collections.unmodifiableMap(FOLLOWERS);
    }

    public static Map<String, God> generals() {
        return Collections.unmodifiableMap(GENERALS);
    }

    public static Map<String, God> bodyguards() {
        return Collections.unmodifiableMap(BODYGUARDS);
    }

    /** Creatures that give Zarosian essence, with the amount. */
    public static Map<String, Integer> zarosians() {
        return Collections.unmodifiableMap(ZAROS_ESSENCE);
    }

    /**
     * The god whose followers a follower creature leaves alone the player for ({@code "armadyl"} ... {@code "zamorak"},
     * {@code "zaros"} for the Ancient Prison's followers), or null for anything always aggressive (generals, bodyguards,
     * Nex and her mages).
     */
    public static String allegiance(String creatureId) {
        God g = followerGod(creatureId);
        if (g != null) {
            return g.id();
        }
        return creatureId != null && creatureId.startsWith("gwd_") && ZAROS_ESSENCE.containsKey(creatureId) ? "zaros" : null;
    }

    /**
     * The generic boss engine rolls a boss's pet at 1/{@value #ENGINE_PET_RATE}. For bosses whose real
     * pet rate is better, scripts roll an extra chance {@code p} so the combined rate is exact:
     * {@code 1 - (1 - 1/3000)(1 - p) = 1/realRate}. Returns 0 when the real rate is 1/3000 or rarer.
     */
    public static double extraPetChance(int realRate) {
        double engine = 1.0 / ENGINE_PET_RATE;
        double target = 1.0 / realRate;
        if (target <= engine) {
            return 0;
        }
        return (target - engine) / (1 - engine);
    }

    /** Remaining kill count after entering a chamber, or -1 if the player can't enter. */
    public static int afterEntry(int killcount) {
        return killcount >= REQUIRED_KILLCOUNT ? killcount - REQUIRED_KILLCOUNT : -1;
    }
}
