package com.runecraft.osrs.content.godwars;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * God Wars Dungeon factions: which followers count towards which god's kill count, which general
 * belongs to which god, and the kill count needed to enter a general's chamber (40, as in OSRS).
 */
public final class GodWarsFactions {
    /** Kill count needed before a general's door opens (OSRS: 40). Consumed on entry. */
    public static final int REQUIRED_KILLCOUNT = 40;
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

    static {
        follow(God.ARMADYL, "gwd_aviansie", "gwd_aviansie_elite", "gwd_spiritual_warrior_armadyl",
                "gwd_spiritual_ranger_armadyl", "gwd_spiritual_mage_armadyl");
        follow(God.BANDOS, "gwd_goblin", "gwd_hobgoblin", "gwd_ork", "gwd_ogre", "gwd_jogre", "gwd_cyclops",
                "gwd_spiritual_warrior_bandos", "gwd_spiritual_ranger_bandos", "gwd_spiritual_mage_bandos");
        follow(God.SARADOMIN, "gwd_knight_of_saradomin", "gwd_saradomin_priest", "gwd_spiritual_warrior_saradomin",
                "gwd_spiritual_ranger_saradomin", "gwd_spiritual_mage_saradomin");
        follow(God.ZAMORAK, "gwd_imp", "gwd_werewolf", "gwd_bloodveld", "gwd_hellhound", "gwd_gorak", "gwd_vampire",
                "gwd_pyrefiend", "gwd_spiritual_warrior_zamorak", "gwd_spiritual_ranger_zamorak", "gwd_spiritual_mage_zamorak");
        GENERALS.put("kreearra", God.ARMADYL);
        GENERALS.put("general_graardor", God.BANDOS);
        GENERALS.put("commander_zilyana", God.SARADOMIN);
        GENERALS.put("kril_tsutsaroth", God.ZAMORAK);
    }

    private GodWarsFactions() {
    }

    private static void follow(God god, String... ids) {
        for (String id : ids) {
            FOLLOWERS.put(id, god);
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

    public static Map<String, God> followers() {
        return Collections.unmodifiableMap(FOLLOWERS);
    }

    public static Map<String, God> generals() {
        return Collections.unmodifiableMap(GENERALS);
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
