package com.runecraft.osrs.content.quests;

import java.util.List;

/**
 * Mage Arena I and II rules (OSRS Wiki: Mage Arena I, Mage Arena II, Kolodion, Battle mage, Enchanted symbol, Porazdir,
 * Derwen, Justiciar Zachariah). Kolodion is fought in five forms that only magic harms; afterwards the god statues give
 * god capes and the chamber guardian the matching staff. A god spell works outside the arena once it has been cast 100
 * times inside it (on the battle mages). Mage Arena II's god followers roam the deep Wilderness, are tracked with the
 * enchanted symbol (hot and cold, up to 16 damage a use), can only be harmed by their own god spell and drop the remains
 * Kolodion turns into an imbued god cape.
 */
public final class MageArena {
    private MageArena() {
    }

    /** The arena place (its middle, x 3105 y 3934 in level 56 Wilderness). */
    public static final String PLACE = "quest_mage_arena";
    public static final String KOLODION_NPC = "kolodion";
    /** The final (demon) form's id: the one the miniquest's kill step counts. */
    public static final String KOLODION = "quest_kolodion";

    /** One of Kolodion's forms: wiki DPS-calculator stats (only the demon has a combat level). */
    public record Form(String id, String name, int level, int hitpoints, int attack, int strength, int defence, int magic,
                       int speed, int size, String model) {
    }

    /** Human, ogre, frost spider, ghost, then the level 112 black demon (wiki "Mage Arena I"). */
    public static final List<Form> KOLODION_FORMS = List.of(
            new Form("quest_kolodion_human", "Kolodion", 0, 3, 15, 10, 20, 15, 7, 1, "quest_kolodion"),
            new Form("quest_kolodion_ogre", "Kolodion", 0, 65, 55, 70, 72, 50, 7, 1, "ogre"),
            new Form("quest_kolodion_spider", "Kolodion", 0, 78, 69, 78, 47, 70, 7, 3, "ice_spider"),
            new Form("quest_kolodion_ghost", "Kolodion", 0, 78, 28, 23, 58, 30, 7, 1, "ghost"),
            new Form(KOLODION, "Kolodion", 112, 107, 85, 98, 105, 400, 4, 3, "black_demon"));
    /** Every form casts the god spells, up to 20 (wiki infobox). */
    public static final int KOLODION_MAX_HIT = 20;

    /** Index of a Kolodion form id, or -1. */
    public static int formIndex(String id) {
        for (int i = 0; i < KOLODION_FORMS.size(); i++) {
            if (KOLODION_FORMS.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** The form Kolodion takes after this one, or null after the demon. */
    public static String nextForm(String id) {
        int i = formIndex(id);
        return i < 0 || i + 1 >= KOLODION_FORMS.size() ? null : KOLODION_FORMS.get(i + 1).id();
    }

    /** The miniquest counter holding the form a player left the fight at ("Kolodion will be in the form you left off at"). */
    public static final String FORM_COUNTER = "ma1_form";

    // ------------------------------------------------------------------ god spells

    public static final String[] GODS = {"saradomin", "guthix", "zamorak"};
    public static final String[] GOD_SPELLS = {"saradomin_strike", "claws_of_guthix", "flames_of_zamorak"};
    /** Casts inside the arena before a god spell works outside it (wiki: "cast them 100 times on the battle mages"). */
    public static final int CASTS_TO_CHARGE = 100;
    /** God staves from the chamber guardian after the first (wiki "Chamber guardian": 80,000 coins). */
    public static final int STAFF_PRICE = 80_000;

    /** The god a god spell belongs to, or null for any other spell. */
    public static String spellGod(String spellId) {
        for (int i = 0; i < GOD_SPELLS.length; i++) {
            if (GOD_SPELLS[i].equals(spellId)) {
                return GODS[i];
            }
        }
        return null;
    }

    public static String godSpell(String god) {
        for (int i = 0; i < GODS.length; i++) {
            if (GODS[i].equals(god)) {
                return GOD_SPELLS[i];
            }
        }
        return null;
    }

    /** Quest flag of a god spell charged for use outside the arena. */
    public static String chargedFlag(String god) {
        return "ma_charged_" + god;
    }

    /** Quest counter of a god spell's casts inside the arena. */
    public static String castCounter(String god) {
        return "ma_casts_" + god;
    }

    /** Flag of the god whose cape a statue gave (the chamber guardian then hands over that god's staff once, free). */
    public static String staffOwedFlag(String god) {
        return "ma_staff_owed_" + god;
    }

    /**
     * Why a god spell can't be cast, or null: it needs Mage Arena I, and outside the arena its 100 casts inside it. (The
     * Charge spell's 80 Magic and the staff are checked by the spellbook itself.)
     */
    public static String castDenied(String spellId, boolean ma1Done, boolean inArena, boolean charged) {
        String god = spellGod(spellId);
        if (god == null) {
            return null;
        }
        if (!ma1Done) {
            return "You need to complete Mage Arena I to cast the god spells.";
        }
        if (!inArena && !charged) {
            return "You need to cast " + spellName(spellId) + " " + CASTS_TO_CHARGE
                    + " times in the Mage Arena before you can use it outside.";
        }
        return null;
    }

    static String spellName(String spellId) {
        return switch (spellId) {
            case "saradomin_strike" -> "Saradomin Strike";
            case "claws_of_guthix" -> "Claws of Guthix";
            default -> "Flames of Zamorak";
        };
    }

    // ------------------------------------------------------------------ battle mages

    public record Spawn(String creature, int x, int y) {
    }

    /** The arena's nine battle mages at their wiki tiles; which god stands where is approx (three of each). */
    public static final List<Spawn> BATTLE_MAGES = List.of(
            new Spawn("battle_mage_saradomin", 3098, 3925), new Spawn("battle_mage_zamorak", 3098, 3942),
            new Spawn("battle_mage_guthix", 3100, 3927), new Spawn("battle_mage_saradomin", 3100, 3940),
            new Spawn("battle_mage_zamorak", 3102, 3929), new Spawn("battle_mage_guthix", 3102, 3938),
            new Spawn("battle_mage_saradomin", 3110, 3934), new Spawn("battle_mage_zamorak", 3113, 3934),
            new Spawn("battle_mage_guthix", 3116, 3934));

    /** A god's battle mage creature id. */
    public static String battleMage(String god) {
        return "battle_mage_" + god;
    }

    /** The god a battle mage serves, or null. */
    public static String battleMageGod(String creature) {
        return creature != null && creature.startsWith("battle_mage_") ? creature.substring("battle_mage_".length()) : null;
    }

    /** Battle mages leave alone a player wearing their own god's cape (only that cape, not other god items: wiki). */
    public static boolean battleMagePeaceful(String creature, String wornCape) {
        String god = battleMageGod(creature);
        return god != null && wornCape != null && (wornCape.equals(god + "_cape") || wornCape.equals("imbued_" + god + "_cape"));
    }

    /** The arena's inner ring: the battle mages' circle (tiles 3093-3117, 3922-3946: approx from their pins). */
    public static boolean inArena(int x, int y) {
        return x >= 3093 && x <= 3118 && y >= 3921 && y <= 3947;
    }

    // ------------------------------------------------------------------ Mage Arena II

    /** A god follower: its god, the only spell that harms it and the remains it always drops. */
    public record Follower(String id, String name, String god, String spell, String remains, String imbuedCape, int magicMax,
                           int meleeMax) {
    }

    /** Wiki infoboxes: 43 magic / 16 melee, except Zachariah (26 magic, 43 melee). */
    public static final List<Follower> FOLLOWERS = List.of(
            new Follower("quest_justiciar_zachariah", "Justiciar Zachariah", "saradomin", "saradomin_strike", "justiciars_hand",
                    "imbued_saradomin_cape", 26, 43),
            new Follower("quest_derwen", "Derwen", "guthix", "claws_of_guthix", "ents_roots", "imbued_guthix_cape", 43, 16),
            new Follower("quest_porazdir", "Porazdir", "zamorak", "flames_of_zamorak", "demons_heart", "imbued_zamorak_cape", 43, 16));

    public static Follower follower(String id) {
        for (Follower f : FOLLOWERS) {
            if (f.id().equals(id)) {
                return f;
            }
        }
        return null;
    }

    /** The follower whose remains this is, or null. */
    public static Follower byRemains(String item) {
        for (Follower f : FOLLOWERS) {
            if (f.remains().equals(item)) {
                return f;
            }
        }
        return null;
    }

    /** Flag of remains Kolodion holds for the miniquest. */
    public static String handedFlag(Follower f) {
        return "ma2_" + f.remains();
    }

    /** Whether a hit counts: only the follower's own god spell harms it. */
    public static boolean harms(Follower f, String spellId) {
        return f != null && f.spell().equals(spellId);
    }

    public static final String SYMBOL = "enchanted_symbol";
    /** "Each use of the symbol can deal up to 16 damage, and can kill the player." */
    public static final int SYMBOL_MAX_DAMAGE = 16;
    /** The followers' spawn spots rotate about every 45 minutes of the player's time in the Wilderness (wiki). */
    public static final int ROTATION_SECONDS = 45 * 60;

    /**
     * Where the followers can appear: the deep Wilderness around Lava Dragon Isle (the wiki's map of the areas isn't in the
     * data, so these tiles are approx).
     */
    public static final int[][] FOLLOWER_SPOTS = {
            {3180, 3870}, {3215, 3872}, {3243, 3850}, {3246, 3815}, {3160, 3832}, // approx
            {3150, 3800}, {3203, 3790}, {3262, 3902}, {3110, 3868}, {3290, 3870}}; // approx

    /** The spot a player's follower is at during a rotation (different per player, follower and rotation; never shared). */
    public static int[] followerSpot(long playerSeed, int followerIndex, int rotation) {
        int n = FOLLOWER_SPOTS.length;
        int[] used = new int[followerIndex + 1];
        for (int f = 0; f <= followerIndex; f++) {
            long h = playerSeed * 0x9E3779B97F4A7C15L + (f + 1) * 0xC2B2AE3D27D4EB4FL + rotation * 0x165667B19E3779F9L;
            h ^= h >>> 29;
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 32;
            int spot = (int) Math.floorMod(h, (long) n);
            for (boolean clash = true; clash; ) {
                clash = false;
                for (int o = 0; o < f; o++) {
                    if (used[o] == spot) {
                        spot = (spot + 1) % n;
                        clash = true;
                    }
                }
            }
            used[f] = spot;
        }
        return FOLLOWER_SPOTS[used[followerIndex]];
    }

    /**
     * The symbol's reading at a distance in steps: the strange device's bands (ice cold 500+, very cold 200+, cold 150+,
     * warm 100+, hot 70+, very hot 30+, incredibly hot, visibly shaking within 4: the master device).
     */
    public static String temperature(int steps) {
        if (steps >= 500) {
            return "ice cold";
        }
        if (steps >= 200) {
            return "very cold";
        }
        if (steps >= 150) {
            return "cold";
        }
        if (steps >= 100) {
            return "warm";
        }
        if (steps >= 70) {
            return "hot";
        }
        if (steps >= 30) {
            return "very hot";
        }
        return steps > SHAKING ? "incredibly hot" : "visibly shaking";
    }

    /** Within this many steps the symbol shakes and the follower appears. */
    public static final int SHAKING = 4;

    public static int steps(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    /** Compass direction from one tile to another ("north-east"), or "here". */
    public static String direction(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        if (dx == 0 && dy == 0) {
            return "here";
        }
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        String[] names = {"east", "north-east", "north", "north-west", "west", "south-west", "south", "south-east"};
        return names[(int) Math.floorMod(Math.round(angle / 45.0), 8)];
    }

    /**
     * Porazdir's energy ball: hits through prayer for up to 30 next to him, less the farther away, nothing from 12 tiles
     * (wiki; the fall-off in between is approx linear).
     */
    public static int energyBallMax(int tiles) {
        if (tiles >= ENERGY_BALL_RANGE) {
            return 0;
        }
        return (int) Math.round(ENERGY_BALL_MAX * (ENERGY_BALL_RANGE - Math.max(1, tiles)) / (double) (ENERGY_BALL_RANGE - 1));
    }

    public static final int ENERGY_BALL_MAX = 30;
    public static final int ENERGY_BALL_RANGE = 12;
    /** Derwen's healing orbs: 20 hitpoints each, each heals him 5 "every few seconds" (approx every 5 ticks). */
    public static final String ORB = "quest_derwen_energy_ball";
    public static final int ORB_HITPOINTS = 20;
    public static final int ORB_HEAL = 5;
    public static final int ORB_HEAL_TICKS = 5; // approx
    /** Zachariah's shock wave pulls a player still on the targeted tile into melee and binds them; he then swings every 3 ticks. */
    public static final int BOUND_SWING_TICKS = 3;
    public static final int BIND_TICKS = 8; // approx
    /** A follower that isn't killed in time leaves (approx: the wiki gives no time). */
    public static final int DESPAWN_TICKS = 1000; // approx
}
