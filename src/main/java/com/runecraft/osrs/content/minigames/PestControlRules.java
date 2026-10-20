package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.MinigameRules.Loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Pest Control after the OSRS Wiki ("Pest Control", "Pest Control/Strategies", "Portal (Pest Control)", the pest pages,
 * "Void Knights' Reward Options", "Void seal"; dump July 2026) and the DPS calculator's monster data: the pests' level
 * variants on each lander, their spawn chances and roles, the portal shields' timing and rotations, the portals' and the
 * Void Knight's defences, the gates and barricades, the activity bar and the reward packs. The lander, points and coins
 * rules stay in {@link MinigameRules.PestControl}. Values the wiki doesn't give are marked approx.
 */
public final class PestControlRules {
    private PestControlRules() {
    }

    // ------------------------------------------------------------------ pests

    /**
     * One level of a pest (weirdgloop/osrs-dps-calc {@code monsters.json} "&lt;Pest&gt; Level N"): its stats and max hit, and
     * the landers it appears on ({@code "NIV"} letters, from the wiki pest pages' level tables).
     */
    public record Variant(String pest, int level, int hitpoints, int attack, int strength, int defence, int magic, int ranged,
                          int maxHit, String landers) {
        /** True when this variant spawns in games of lander {@code tier} (0 novice, 1 intermediate, 2 veteran). */
        public boolean on(int tier) {
            return landers.indexOf("NIV".charAt(tier)) >= 0;
        }
    }

    /** Every pest level that spawns on some lander (the level 158 brawler exists in the cache but never appears). */
    public static final List<Variant> VARIANTS = List.of(
            new Variant("brawler", 51, 53, 27, 57, 43, 0, 0, 7, "N"),
            new Variant("brawler", 76, 83, 37, 82, 67, 0, 0, 9, "NI"),
            new Variant("brawler", 101, 113, 47, 107, 93, 0, 0, 12, "NIV"),
            new Variant("brawler", 129, 143, 67, 132, 118, 0, 0, 14, "IV"),
            new Variant("defiler", 33, 27, 0, 0, 17, 0, 46, 6, "N"),
            new Variant("defiler", 50, 45, 0, 0, 29, 0, 66, 8, "NI"),
            new Variant("defiler", 66, 62, 0, 0, 41, 0, 84, 9, "I"),
            new Variant("defiler", 67, 62, 0, 0, 41, 0, 85, 9, "I"),
            new Variant("defiler", 80, 78, 0, 0, 53, 0, 98, 11, "IV"),
            new Variant("defiler", 97, 97, 0, 0, 65, 0, 117, 13, "V"),
            new Variant("ravager", 36, 23, 27, 57, 13, 0, 0, 7, "N"),
            new Variant("ravager", 53, 38, 37, 82, 23, 0, 0, 9, "NI"),
            new Variant("ravager", 71, 53, 47, 107, 33, 0, 0, 12, "NIV"),
            new Variant("ravager", 89, 68, 57, 132, 43, 0, 0, 14, "IV"),
            new Variant("ravager", 106, 83, 67, 157, 53, 0, 0, 17, "V"),
            new Variant("shifter", 38, 23, 47, 37, 23, 27, 0, 5, "N"),
            new Variant("shifter", 57, 38, 67, 52, 38, 37, 0, 6, "NI"),
            new Variant("shifter", 76, 53, 87, 67, 53, 47, 0, 8, "I"),
            new Variant("shifter", 90, 68, 100, 75, 68, 50, 0, 8, "IV"),
            new Variant("shifter", 104, 83, 110, 83, 83, 60, 0, 9, "V"),
            new Variant("spinner", 36, 33, 17, 17, 43, 37, 0, 3, "N"),
            new Variant("spinner", 55, 53, 22, 22, 68, 52, 0, 3, "NI"),
            new Variant("spinner", 74, 73, 27, 27, 93, 67, 0, 4, "NIV"),
            new Variant("spinner", 88, 93, 37, 37, 143, 37, 0, 5, "IV"),
            new Variant("spinner", 92, 101, 32, 32, 118, 82, 0, 4, "IV"),
            new Variant("splatter", 22, 13, 17, 17, 33, 17, 0, 3, "N"),
            new Variant("splatter", 33, 23, 22, 22, 53, 22, 0, 3, "NI"),
            new Variant("splatter", 44, 33, 27, 27, 73, 27, 0, 4, "NIV"),
            new Variant("splatter", 54, 43, 32, 32, 93, 32, 0, 4, "IV"),
            new Variant("splatter", 65, 53, 37, 37, 113, 37, 0, 5, "V"),
            new Variant("torcher", 33, 18, 0, 0, 25, 46, 0, 6, "N"),
            new Variant("torcher", 49, 30, 0, 0, 41, 66, 0, 8, "NI"),
            new Variant("torcher", 66, 45, 0, 0, 57, 85, 0, 9, "I"),
            new Variant("torcher", 79, 57, 0, 0, 73, 97, 0, 11, "IV"),
            new Variant("torcher", 91, 70, 0, 0, 89, 107, 0, 12, "V"),
            new Variant("torcher", 92, 71, 0, 0, 89, 107, 0, 12, "V"));

    /** The levels of {@code pest} that spawn on lander {@code tier}. */
    public static List<Variant> variants(String pest, int tier) {
        List<Variant> out = new ArrayList<>();
        for (Variant v : VARIANTS) {
            if (v.pest().equals(pest) && v.on(tier)) {
                out.add(v);
            }
        }
        return out;
    }

    /** The variant of {@code pest} at combat level {@code level}, or null. */
    public static Variant variant(String pest, int level) {
        for (Variant v : VARIANTS) {
            if (v.pest().equals(pest) && v.level() == level) {
                return v;
            }
        }
        return null;
    }

    /** A level of {@code pest} for lander {@code tier}, picked evenly among the levels that appear there (approx). */
    public static Variant rollVariant(String pest, int tier, double random) {
        List<Variant> list = variants(pest, tier);
        return list.get(Math.min(list.size() - 1, (int) Math.floor(random * list.size())));
    }

    /**
     * Spawn chances in percent (Mod Ash, 18 February 2025: "5% brawler, 20% defiler, 5% ravager, 20% shifter, 7% spinner,
     * 23% splatter, 20% torcher").
     */
    public static final Map<String, Integer> SPAWN_PERCENT = new LinkedHashMap<>();

    static {
        SPAWN_PERCENT.put("brawler", 5);
        SPAWN_PERCENT.put("defiler", 20);
        SPAWN_PERCENT.put("ravager", 5);
        SPAWN_PERCENT.put("shifter", 20);
        SPAWN_PERCENT.put("spinner", 7);
        SPAWN_PERCENT.put("splatter", 23);
        SPAWN_PERCENT.put("torcher", 20);
    }

    /** The pest a portal spawns for a random value in [0,1). */
    public static String rollPest(double random) {
        int r = (int) Math.floor(random * 100);
        for (Map.Entry<String, Integer> e : SPAWN_PERCENT.entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return e.getKey();
            }
        }
        return "torcher";
    }

    /** What a pest does when no player keeps it busy (wiki pest pages). */
    public enum Role {
        /** Defilers, shifters and torchers attack the Void Knight (the only pests that do). */
        KNIGHT,
        /** Brawlers fight players and block the way; they never attack the Void Knight. */
        PLAYERS,
        /** Ravagers ignore players until attacked and tear down gates and barricades. */
        STRUCTURES,
        /** Spinners heal their portal and never fight back; they explode in poison when their portal falls. */
        HEALER,
        /** Splatters bounce to the nearest gate or barricade and detonate there (and when killed). */
        BOMBER
    }

    public static Role role(String pest) {
        return switch (pest) {
            case "defiler", "shifter", "torcher" -> Role.KNIGHT;
            case "ravager" -> Role.STRUCTURES;
            case "spinner" -> Role.HEALER;
            case "splatter" -> Role.BOMBER;
            default -> Role.PLAYERS;
        };
    }

    /**
     * Distance (blocks) a knight-attacking pest strikes the Void Knight from: defilers and torchers fire over the walls from
     * the south wall's line (approx: the wiki only says "long distance"); shifters strike from beside him, also diagonally.
     */
    public static double knightReach(String pest) {
        return pest.equals("shifter") ? 2.5 : 10.0;
    }

    /** Game ticks between a pest's attacks (every pest attacks every 4 ticks, osrs-dps-calc). */
    public static final int PEST_ATTACK_TICKS = 4;
    /** Shifters teleport beside the Void Knight this often when nothing holds them (approx; "constantly"). */
    public static final int SHIFTER_BLINK_TICKS = 10;
    /** Game ticks between a portal's spawns while it stands (approx). */
    public static final int SPAWN_TICKS = 8;

    /** Most pests alive at once: more players, more pests (wiki 2006 update; numbers approx, 10 for a lone player). */
    public static int pestCap(int players) {
        return 6 + 4 * Math.max(1, players);
    }

    // ------------------------------------------------------------------ portals and shields

    /** The first shield drops 15 seconds into the game (25 ticks), the rest every 30 seconds (50 ticks); all are down by 1:45. */
    public static final int FIRST_SHIELD_TICKS = 25;
    public static final int SHIELD_INTERVAL_TICKS = 50;

    /**
     * The six shield rotations (wiki "Portal shields drop rotation"), as indices into {@link MinigameRules.PestControl#PORTALS}
     * (0 purple W, 1 blue E, 2 yellow SE, 3 red SW). The red shield never drops first.
     */
    public static final int[][] SHIELD_ROTATIONS = {
            {1, 3, 2, 0}, // blue, red, yellow, purple
            {1, 0, 3, 2}, // blue, purple, red, yellow
            {0, 1, 2, 3}, // purple, blue, yellow, red
            {0, 2, 1, 3}, // purple, yellow, blue, red
            {2, 3, 0, 1}, // yellow, red, purple, blue
            {2, 0, 3, 1}}; // yellow, purple, red, blue

    /** Game tick on which the {@code n}th shield (0-3) drops. */
    public static int shieldDropTick(int n) {
        return FIRST_SHIELD_TICKS + n * SHIELD_INTERVAL_TICKS;
    }

    /** How many shields have dropped by game tick {@code tick}. */
    public static int shieldsDown(long tick) {
        int n = 0;
        while (n < 4 && tick >= shieldDropTick(n)) {
            n++;
        }
        return n;
    }

    /** Portals: level 120 Defence with +100 in every style they aren't weak to (Mod Ash, 8 April 2022). */
    public static final int PORTAL_DEFENCE = 120;
    public static final int PORTAL_ARMOUR = 100;

    /** A portal's defence bonuses {stab, slash, crush, magic, ranged}: purple is weak to ranged, blue to magic, yellow to stab and slash, red to crush. */
    public static int[] portalDefenceBonuses(int portal) {
        int a = PORTAL_ARMOUR;
        return switch (portal) {
            case 0 -> new int[]{a, a, a, a, 0};
            case 1 -> new int[]{a, a, a, 0, a};
            case 2 -> new int[]{0, 0, a, a, a};
            default -> new int[]{a, a, 0, a, a};
        };
    }

    /** A destroyed portal restores 50 of the Void Knight's hitpoints. */
    public static final int KNIGHT_HEAL_PER_PORTAL = 50;

    /** The Void Knight's Defence and Magic levels and his +100 defence bonuses (osrs-dps-calc "Void Knight (Pest Control)"). */
    public static final int KNIGHT_DEFENCE = 50;
    public static final int KNIGHT_MAGIC = 50;
    public static final int KNIGHT_ARMOUR = 100;

    /** Spinners heal their portal every few ticks while beside it (approx: the wiki says "at a rapid speed"). */
    public static final int SPINNER_HEAL_TICKS = 5;
    public static final int SPINNER_HEAL_MIN = 5;
    public static final int SPINNER_HEAL_MAX = 10;
    /** A spinner whose portal falls explodes: 5 poison damage at once, then poison starting at 1 (wiki "Spinner"). */
    public static final int SPINNER_EXPLOSION = 5;
    public static final int SPINNER_POISON = 1;
    /** Splatters detonate for 5-24 typeless damage on everything in a 3x3 area, portals and pests included (wiki "Splatter"). */
    public static final int SPLATTER_MIN = 5;
    public static final int SPLATTER_MAX = 24;

    // ------------------------------------------------------------------ gates and barricades

    /** The Void Knight's tile on Pest Control island (wiki "Void Knight (Pest Control)"). */
    public static final int KNIGHT_X = 2656;
    public static final int KNIGHT_Y = 2592;

    /**
     * A gate or a barricade section: its tiles relative to the Void Knight (dx east, dy north), from the wiki map pins of
     * "Gate (Pest Control)" and "Barricade (Pest Control)".
     */
    public record Structure(String name, boolean gate, List<int[]> tiles) {
    }

    /** The fort's three gates (west, south, east) and its eight barricade sections. */
    public static final List<Structure> STRUCTURES = List.of(
            new Structure("western gate", true, List.of(new int[]{-13, 0}, new int[]{-13, 1})),
            new Structure("southern gate", true, List.of(new int[]{0, -7}, new int[]{1, -7})),
            new Structure("eastern gate", true, List.of(new int[]{14, 0}, new int[]{14, 1})),
            new Structure("barricade", false, row(-9, -14, 4, true)),
            new Structure("barricade", false, row(10, -14, 4, true)),
            new Structure("barricade", false, row(17, -2, 4, false)),
            new Structure("barricade", false, row(20, -8, 4, false)),
            new Structure("barricade", false, row(20, -21, 4, false)),
            new Structure("barricade", false, row(0, -17, 4, true)),
            new Structure("barricade", false, row(-20, -4, 4, false)),
            new Structure("barricade", false, row(-19, -21, 4, false)));

    /** {@code n} tiles from (dx, dy) going east ({@code eastward}) or north. */
    private static List<int[]> row(int dx, int dy, int n, boolean eastward) {
        List<int[]> tiles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tiles.add(eastward ? new int[]{dx + i, dy} : new int[]{dx, dy + i});
        }
        return tiles;
    }

    /**
     * The fort's walls around the Void Knight, relative to his tile: west wall at dx -13, east wall at dx 14, south wall at
     * dy -7 (through the gates' tiles); the walls run north to dy 6 and the north side, facing the landing jetty, is open.
     */
    public static final int FORT_WEST = -13;
    public static final int FORT_EAST = 14;
    public static final int FORT_SOUTH = -7;
    public static final int FORT_NORTH = 6; // approx

    /** Hitpoints of a gate and of a barricade section (approx: the wiki gives none). */
    public static final int GATE_HP = 200;
    public static final int BARRICADE_HP = 100;
    /** Crafting experience for a repair (wiki "Pest Control/Strategies": 5 XP per repair). */
    public static final double REPAIR_XP = 5;

    /** Repair state shown by a gate or barricade: 0 healthy, 1 damaged, 2 broken, 3 destroyed (wiki "Gate (Pest Control)"). */
    public static int damageStage(int hp, int max) {
        if (hp <= 0) {
            return 3;
        }
        if (hp * 3 > max * 2) {
            return 0;
        }
        return hp * 3 > max ? 1 : 2;
    }

    // ------------------------------------------------------------------ activity bar

    /**
     * The activity bar in tenths of a percent (0-1000). It must be above 0 when the game ends for commendation points. Hitting
     * pests raises it, hitting portals and spinners raises it more and repairs raise it a lot; it drains while the player is
     * idle. The wiki gives no numbers: here 50 damage fills it (the pre-2017 rule's 50 damage), portal and spinner damage
     * counts double, a repair counts as 5 damage (the 2006 rule) and a full bar drains in two minutes (approx).
     */
    public static final int ACTIVITY_MAX = 1000;
    public static final int ACTIVITY_PER_DAMAGE = 20;
    public static final int ACTIVITY_PER_REPAIR = 5 * ACTIVITY_PER_DAMAGE;
    public static final int ACTIVITY_DECAY_PER_TICK = 5;

    /** The bar after dealing {@code damage} (portals and spinners count double). */
    public static int activityAfterHit(int activity, int damage, boolean portalOrSpinner) {
        int gain = Math.max(0, damage) * ACTIVITY_PER_DAMAGE * (portalOrSpinner ? 2 : 1);
        return Math.min(ACTIVITY_MAX, activity + gain);
    }

    /** The bar one game tick later. */
    public static int activityDecay(int activity) {
        return Math.max(0, activity - ACTIVITY_DECAY_PER_TICK);
    }

    // ------------------------------------------------------------------ void seal

    /** The Void seal: 8 charges, each rub hits nearby pests for 8-12 (wiki "Void seal"); "nearby" approx 3 tiles. */
    public static final int SEAL_CHARGES = 8;
    public static final int SEAL_MIN = 8;
    public static final int SEAL_MAX = 12;
    public static final double SEAL_RADIUS = 3.0;
    /** Game ticks between two rubs: each rub is its own action (approx; held use key repeats every 4 client ticks). */
    public static final int SEAL_RUB_TICKS = 3; // approx

    // ------------------------------------------------------------------ void knight equipment

    /**
     * Buying or wearing any Void Knight equipment but the seal needs 42 Attack, Strength, Defence, Hitpoints, Ranged and
     * Magic and 22 Prayer (wiki "Void Knight equipment"; "Void Knights' Reward Options" cbreqs on every store line).
     */
    public static final Map<Skill, Integer> VOID_REQUIREMENTS;

    static {
        Map<Skill, Integer> m = new LinkedHashMap<>();
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC}) {
            m.put(s, 42);
        }
        m.put(Skill.PRAYER, 22);
        VOID_REQUIREMENTS = Collections.unmodifiableMap(m);
    }

    /** True for the Void Knight equipment with the {@link #VOID_REQUIREMENTS} (everything but the Void seal). */
    public static boolean isVoidEquipment(String id) {
        return id.startsWith("void_knight_") || id.startsWith("elite_void_")
                || id.startsWith("void_") && id.endsWith("_helm");
    }

    /** Why {@code id} can't be bought or worn at these levels ("Requires level 42 Attack"), or null. */
    public static String voidLock(String id, ToIntFunction<Skill> level) {
        if (!isVoidEquipment(id)) {
            return null;
        }
        for (Map.Entry<Skill, Integer> req : VOID_REQUIREMENTS.entrySet()) {
            if (level.applyAsInt(req.getKey()) < req.getValue()) {
                return "Requires level " + req.getValue() + " " + req.getKey().displayName();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ reward packs

    /** Herb pack (30 points): always these noted grimy herbs (wiki "Void Knights' Reward Options#Herb pack"). */
    public static final List<Loot> HERB_PACK = List.of(
            new Loot("grimy_harralander", 2, 4, 1),
            new Loot("grimy_ranarr_weed", 2, 3, 1),
            new Loot("grimy_toadflax", 1, 4, 1),
            new Loot("grimy_irit_leaf", 2, 4, 1),
            new Loot("grimy_avantoe", 1, 4, 1),
            new Loot("grimy_kwuarm", 1, 5, 1));

    /** Mineral pack (15 points): 25 noted coal and 18 noted iron ore. */
    public static final List<Loot> MINERAL_PACK = List.of(new Loot("coal", 25, 25, 1), new Loot("iron_ore", 18, 18, 1));

    /** Seed pack (15 points): three rolls of this table, out of 1,061 (Mod Ash; weights doubled for the two half weights). */
    public static final int SEED_PACK_ROLLS = 3;
    public static final List<Loot> SEED_PACK = List.of(
            new Loot("spirit_seed", 1, 1, 2),
            new Loot("magic_seed", 1, 1, 2),
            new Loot("yew_seed", 1, 1, 16),
            new Loot("maple_seed", 1, 1, 20),
            new Loot("willow_seed", 1, 1, 40),
            new Loot("acorn", 1, 1, 60),
            new Loot("ranarr_seed", 1, 1, 40),
            new Loot("sweetcorn_seed", 3, 3, 240),
            new Loot("strawberry_seed", 2, 2, 180),
            new Loot("limpwurt_seed", 2, 2, 200),
            new Loot("watermelon_seed", 2, 2, 180),
            new Loot("tomato_seed", 6, 6, 220),
            new Loot("cabbage_seed", 9, 9, 200),
            new Loot("onion_seed", 11, 11, 200),
            new Loot("potato_seed", 14, 14, 400),
            new Loot("snape_grass_seed", 2, 2, 120),
            new Loot("celastrus_seed", 1, 1, 1),
            new Loot("redwood_tree_seed", 1, 1, 1));

    // ------------------------------------------------------------------ experience

    /**
     * Experience in tenths for {@code points} spent at once: floor(level^2 / 600) x N per point, +1% for 10 points and +10% for
     * 100 (wiki "Void Knights' Reward Options": 10 points at level 25-34 give 353.5 Attack XP).
     */
    public static int xpTenths(int level, int multiplier, int points) {
        long per = (long) (level * level / 600) * multiplier;
        int bonus = points >= 100 ? 10 : points >= 10 ? 1 : 0;
        return (int) (per * points * (100 + bonus) / 10);
    }
}
