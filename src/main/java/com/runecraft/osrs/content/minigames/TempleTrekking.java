package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * Temple Trekking and the Burgh de Rott Ramble (OSRS Wiki, "Temple Trekking"). The player escorts a follower between Burgh de
 * Rott and Paterdomus (the Ramble, the other way, needs Darkness of Hallowvale; both need In Aid of the Myreque). The follower's
 * difficulty sets how many monsters a combat event brings (easy 3, medium 4, hard 5-6); the route sets their level and how many
 * events can happen (route 1 through Mort Myre: 0-9 events, usually 3, monsters ~level 80, combat events evadable; route 2 along
 * the River Salve: 0-4 events, ~level 110, evadable once half the monsters are dead; route 3 through Canifis and the Hollows:
 * 0-5 events, ~level 140, no evading). Events: combat (vampyre juvinates, ghasts, shades, swamp snakes, giant snails, nail
 * beasts), the friendly Abidor Crank (heals, +Defence) and puzzles (bridge repair with logs, the undead lumberjack bridge,
 * the vine swing over a river). Killing a monster is worth 325/625/1,125 points by route, most puzzles 500; a trek scores
 * 325-8,000 points. The follower hands over a blue/yellow/red reward token whose reward scales with the points:
 * {@code low + floor((high - low) * (points - 325) / 7675)}, or a tome of 1,100-1,650 / 2,035-3,025 / 4,015-5,005 XP.
 * Undead lumberjacks can drop pieces of the lumberjack outfit (+2.5% Woodcutting XP).
 */
public final class TempleTrekking {
    private TempleTrekking() {
    }

    /** The trek's map centre (wiki infobox) and the two starting points. */
    public static final int MAP_X = 3480;
    public static final int MAP_Y = 3240;
    /** Burgh de Rott: the followers wait by the north gate. // approx tile */
    public static final int BURGH_X = 3496;
    public static final int BURGH_Y = 3240;
    /** The Ramble's mercenaries wait on the east bank of the Salve outside Paterdomus. // approx tile */
    public static final int PATERDOMUS_X = 3441;
    public static final int PATERDOMUS_Y = 3485;
    public static final String PLACE = "mg_burgh_de_rott_trek";
    public static final String RAMBLE_PLACE = "mg_paterdomus_ramble";
    public static final String RAMBLE_ENTRANCE = "burgh_de_rott_ramble_entrance";
    public static final String QUEST = "in_aid_of_the_myreque";
    public static final String RAMBLE_QUEST = "darkness_of_hallowvale";

    public static final int MIN_POINTS = 325;
    public static final int MAX_POINTS = 8000;
    /** Most puzzle events; the undead lumberjack bridge and Abidor Crank give none. */
    public static final int PUZZLE_POINTS = 500;
    /** Logs (or planks) needed to repair the broken bridge. // approx */
    public static final int BRIDGE_GAPS = 5;
    /** Vines cut and wound into a long vine for the river swing. */
    public static final int VINES = 3;
    /** Chance an undead lumberjack drops a piece of the outfit you don't have. // approx: the wiki gives no rate */
    public static final int LUMBERJACK_PIECE_CHANCE = 8;
    /** Abidor Crank: +15 Hitpoints and up to +12 Defence. */
    public static final int ABIDOR_HEAL = 15;

    public static final List<String> LUMBERJACK = List.of("lumberjack_hat", "lumberjack_top", "lumberjack_legs", "lumberjack_boots");

    /** Abidor Crank's Defence boost: up to 12, depending on the Defence level. // approx: 3 + 10% */
    public static int abidorDefenceBoost(int defenceLevel) {
        return Math.min(12, 3 + defenceLevel / 10);
    }

    // ------------------------------------------------------------------ followers

    /** Follower difficulty: monsters per combat event and the follower's hitpoints (// approx: stats aren't documented). */
    public enum Difficulty {
        EASY(3, 3, 90), MEDIUM(4, 4, 65), HARD(5, 6, 40);

        public final int minMonsters;
        public final int maxMonsters;
        public final int hitpoints;

        Difficulty(int minMonsters, int maxMonsters, int hitpoints) {
            this.minMonsters = minMonsters;
            this.maxMonsters = maxMonsters;
            this.hitpoints = hitpoints;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public String displayName() {
            return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
        }

        /** Monsters a combat event brings. */
        public int monsters(DoubleSupplier rng) {
            return minMonsters + (int) (rng.getAsDouble() * (maxMonsters - minMonsters + 1));
        }

        /** The follower creature (its hitpoints differ by difficulty). */
        public String creature() {
            return "trek_follower_" + id();
        }
    }

    /** The twelve followers: Burgh de Rott's citizens (Temple Trekking) and the Misthalin mercenaries (Ramble). */
    public enum Follower {
        FYIONA_FRAY("Fyiona Fray", Difficulty.EASY, false, 0x8A2A2A),
        DALCIAN_FANG("Dalcian Fang", Difficulty.EASY, false, 0x6A2A2A),
        JAYENE_KLIYN("Jayene Kliyn", Difficulty.MEDIUM, false, 0x2A6A2A),
        VALANTAY_EPPEL("Valantay Eppel", Difficulty.MEDIUM, false, 0x2A5A3A),
        ROLAYNE_TWICKIT("Rolayne Twickit", Difficulty.HARD, false, 0x2A3A8A),
        SMIDDI_RYAK("Smiddi Ryak", Difficulty.HARD, false, 0x3A3A7A),
        MAGE("Mage", Difficulty.EASY, true, 0x3A3AA0),
        ADVENTURER("Adventurer", Difficulty.EASY, true, 0x8A8A9A),
        RANGER("Ranger", Difficulty.MEDIUM, true, 0x4A6A2A),
        APPRENTICE("Apprentice", Difficulty.MEDIUM, true, 0x5A3A8A),
        WOMAN_AT_ARMS("Woman-at-arms", Difficulty.HARD, true, 0x7A7A80),
        FORESTER("Forester", Difficulty.HARD, true, 0x3A5A2A);

        public final String displayName;
        public final Difficulty difficulty;
        /** True for the Ramble's mercenaries (from Paterdomus), false for Temple Trekking's citizens. */
        public final boolean ramble;
        public final int colour;

        Follower(String displayName, Difficulty difficulty, boolean ramble, int colour) {
            this.displayName = displayName;
            this.difficulty = difficulty;
            this.ramble = ramble;
            this.colour = colour;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The NPC standing at the start of the trek. */
        public String npc() {
            return "mg_trek_" + id();
        }

        public static Follower byId(String id) {
            for (Follower f : values()) {
                if (f.id().equals(id)) {
                    return f;
                }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------ routes

    /** The three routes: event slots and the chance each one holds an event, kill points, token, tome XP and evading. */
    public enum Route {
        /** Mort Myre: 0-9 events, usually 3. Combat events can always be evaded. */
        ROUTE_1(9, 1 / 3.0, 325, "reward_token_blue", 1100, 1650, 0.0),
        /** Along the River Salve: 0-4 events; evading needs half the monsters dead. // approx event chance */
        ROUTE_2(4, 0.6, 625, "reward_token_yellow", 2035, 3025, 0.5),
        /** Canifis, the Hollows and the boat to Mort'ton: 0-5 events, usually 3, no evading. */
        ROUTE_3(5, 0.6, 1125, "reward_token_red", 4015, 5005, -1);

        public final int slots;
        public final double eventChance;
        public final int killPoints;
        public final String token;
        public final int tomeMin;
        public final int tomeMax;
        /** Share of the event's monsters that must be dead before evading (negative: can't evade). */
        public final double evadeAfter;

        Route(int slots, double eventChance, int killPoints, String token, int tomeMin, int tomeMax, double evadeAfter) {
            this.slots = slots;
            this.eventChance = eventChance;
            this.killPoints = killPoints;
            this.token = token;
            this.tomeMin = tomeMin;
            this.tomeMax = tomeMax;
            this.evadeAfter = evadeAfter;
        }

        public int number() {
            return ordinal() + 1;
        }

        public static Route byNumber(int n) {
            return n >= 1 && n <= 3 ? values()[n - 1] : null;
        }

        /** Whether a combat event can be evaded with {@code killed} of {@code total} monsters dead. */
        public boolean canEvade(int killed, int total) {
            return evadeAfter >= 0 && killed >= Math.ceil(total * evadeAfter);
        }
    }

    // ------------------------------------------------------------------ events

    public enum EventType {
        COMBAT, FRIENDLY, PUZZLE
    }

    /** Trek events with their kind, creature (per route tier) and relative weight. // approx weights: the dev's aren't public */
    public enum Event {
        JUVINATES(EventType.COMBAT, "trek_vampyre_juvinate", 6, "Vampyre juvinates leap out of the swamp!"),
        GHASTS(EventType.COMBAT, "trek_ghast", 6, "Ghasts rise from the bog!"),
        SHADES(EventType.COMBAT, "trek_shade", 4, "Shades drift out of the mist!"),
        SWAMP_SNAKES(EventType.COMBAT, "trek_swamp_snake", 6, "Swamp snakes slither out of the water!"),
        GIANT_SNAILS(EventType.COMBAT, "trek_giant_snail", 6, "Giant snails crawl towards you!"),
        /** "This event does not occur often." */
        NAIL_BEASTS(EventType.COMBAT, "trek_nail_beast", 2, "Nail beasts shriek and charge!"),
        ABIDOR_CRANK(EventType.FRIENDLY, null, 3, "Abidor Crank tends to you and your follower."),
        BRIDGE(EventType.PUZZLE, "trek_zombie", 4, "The bridge is broken. Chop the trees and patch it with logs."),
        /** Since the bog event's removal (September 2024) this bridge has the extra weight. */
        LUMBERJACK_BRIDGE(EventType.PUZZLE, "trek_undead_lumberjack", 6, "The bridge is broken. Undead lumberjacks wade out of the river..."),
        RIVER(EventType.PUZZLE, null, 4, "A river blocks the path. Cut three vines from the small trees and swing across.");

        public final EventType kind;
        public final String creature;
        public final int weight;
        public final String message;

        Event(EventType kind, String creature, int weight, String message) {
            this.kind = kind;
            this.creature = creature;
            this.weight = weight;
            this.message = message;
        }

        /** The creature of this event on a route (combat monsters and lumberjacks come in three tiers). */
        public String creature(Route route) {
            if (creature == null || this == BRIDGE) {
                return creature;
            }
            return creature + "_" + route.number();
        }

        /** Points the event's completion is worth (combat events score per kill instead). */
        public int points() {
            return this == BRIDGE || this == RIVER ? PUZZLE_POINTS : 0;
        }
    }

    /** Rolls the events of a trek, in order. */
    public static List<Event> rollEvents(Route route, DoubleSupplier rng) {
        List<Event> out = new ArrayList<>();
        int total = 0;
        for (Event e : Event.values()) {
            total += e.weight;
        }
        for (int i = 0; i < route.slots; i++) {
            if (rng.getAsDouble() >= route.eventChance) {
                continue;
            }
            double r = rng.getAsDouble() * total;
            for (Event e : Event.values()) {
                r -= e.weight;
                if (r < 0) {
                    out.add(e);
                    break;
                }
            }
        }
        return out;
    }

    /** The trek's points, clamped to 325-8,000 (a trek with no points still counts as 325). */
    public static int clampPoints(int points) {
        return Math.max(MIN_POINTS, Math.min(MAX_POINTS, points));
    }

    // ------------------------------------------------------------------ rewards

    /** Reward token choices with their [low, high] amounts per route (OSRS Wiki reward table). */
    public enum Reward {
        PURE_ESSENCE("Pure essence", 50, 150, 120, 380, 370, 660),
        BOW_STRING("Bow strings", 100, 200, 150, 250, 200, 300),
        SILVER_BAR("Silver bars", 5, 50, 50, 100, 100, 150),
        /** Grimy tarromin or harralander plus toadflax: 2/3 low herbs, the rest toadflax. */
        HERBS("Herbs", 5, 15, 10, 20, 15, 30),
        /** Coal and iron ore: a third iron. */
        ORE("Coal and iron ore", 21, 72, 66, 165, 150, 216),
        WATERMELON_SEEDS("Watermelon seeds", 10, 20, 15, 30, 20, 40),
        RAW_LOBSTERS("Raw lobsters", 2, 35, 30, 65, 60, 80),
        TOME("Tome of experience", 0, 0, 0, 0, 0, 0);

        public final String displayName;
        private final int[] range;

        Reward(String displayName, int... range) {
            this.displayName = displayName;
            this.range = range;
        }

        public int low(Route route) {
            return range[route.ordinal() * 2];
        }

        public int high(Route route) {
            return range[route.ordinal() * 2 + 1];
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Reward byId(String id) {
            for (Reward r : values()) {
                if (r.id().equals(id)) {
                    return r;
                }
            }
            return null;
        }
    }

    /** Reward count: low + floor((high - low) * (points - 325) / (8000 - 325)). */
    public static int rewardCount(int low, int high, int points) {
        int p = clampPoints(points);
        return low + (int) Math.floor((double) (high - low) * (p - MIN_POINTS) / (MAX_POINTS - MIN_POINTS));
    }

    /**
     * The items a token pays for a reward choice. {@code harralander} picks harralander over tarromin for the herb reward.
     * The tome is handled by {@link #tomeXp}.
     */
    public static List<MinigameReward> reward(Reward reward, Route route, int points, boolean harralander) {
        int n = rewardCount(reward.low(route), reward.high(route), points);
        return switch (reward) {
            case PURE_ESSENCE -> List.of(new MinigameReward("pure_essence", n));
            case BOW_STRING -> List.of(new MinigameReward("bow_string", n));
            case SILVER_BAR -> List.of(new MinigameReward("silver_bar", n));
            case WATERMELON_SEEDS -> List.of(new MinigameReward("watermelon_seed", n));
            case RAW_LOBSTERS -> List.of(new MinigameReward("raw_lobster", n));
            case HERBS -> {
                int low = 2 * n / 3;
                yield List.of(new MinigameReward(harralander ? "grimy_harralander" : "grimy_tarromin", low),
                        new MinigameReward("grimy_toadflax", n - low));
            }
            case ORE -> {
                int iron = n / 3;
                yield List.of(new MinigameReward("coal", n - iron), new MinigameReward("iron_ore", iron));
            }
            case TOME -> List.of();
        };
    }

    /** The skills a tome of experience can be for (chosen at random). */
    public static final List<Skill> TOME_SKILLS = List.of(Skill.AGILITY, Skill.FIREMAKING, Skill.FISHING, Skill.MINING, Skill.SLAYER,
            Skill.THIEVING, Skill.WOODCUTTING);

    /** XP of a tome: uniform in the route's range. */
    public static int tomeXp(Route route, DoubleSupplier rng) {
        return route.tomeMin + (int) (rng.getAsDouble() * (route.tomeMax - route.tomeMin + 1));
    }

    /** The route of a reward token item, or null. */
    public static Route tokenRoute(String item) {
        for (Route r : Route.values()) {
            if (r.token.equals(item)) {
                return r;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ creatures

    private static AttackDef melee(AttackType type, int max) {
        return AttackDef.of("melee", type, max, 2.5, 10);
    }

    /**
     * One combat creature in its three route tiers. Stats per tier: level, hitpoints, attack, strength, defence, magic, ranged,
     * max hit (OSRS Wiki / osrs-dps-calc monsters.json, the Temple Trekking variants).
     */
    private static void tiers(List<MonsterDef> out, String id, String[] names, AttackType type, int speed, double range, String model,
                              String[] attributes, float w, float h, float scale, int skin, int accent, int[][] stats, Drop... drops) {
        for (int t = 0; t < 3; t++) {
            int[] s = stats[t];
            AttackDef attack = type == AttackType.RANGED
                    ? AttackDef.of("ranged", AttackType.RANGED, s[7], range, 10).particle("minecraft:item_slime")
                    : melee(type, s[7]);
            MonsterDef m = MonsterDef.of(id + "_" + (t + 1), names[t]).level(s[0]).stats(s[1], s[2], s[3], s[4], s[5], s[6])
                    .speed(speed).attack(attack).attributes(attributes).size(w, h, scale).skin(skin, skin, skin, accent).drops(drops);
            if (model != null) {
                m.model(model);
            }
            out.add(m);
        }
    }

    public static List<MonsterDef> monsters() {
        List<MonsterDef> list = new ArrayList<>();
        String[] none = {};
        tiers(list, "trek_vampyre_juvinate", new String[]{"Vampyre Juvinate", "Vampyre Juvinate", "Vampyre Juvinate"}, AttackType.SLASH, 4,
                0, "gwd_vampire", new String[]{"vampyre"}, 0.7f, 1.9f, 1.0f, 0x6A6A70, 0xA01010,
                new int[][]{{59, 50, 95, 16, 45, 50, 50, 6}, {90, 100, 113, 46, 55, 50, 50, 6}, {119, 150, 127, 76, 65, 50, 50, 9}});
        tiers(list, "trek_ghast", new String[]{"Ghast", "Ghast", "Ghast"}, AttackType.CRUSH, 8, 0, "ghost",
                new String[]{"spectral"}, 0.8f, 1.9f, 1.0f, 0x9AA080, 0x40A040,
                new int[][]{{79, 115, 80, 30, 60, 30, 30, 4}, {109, 135, 110, 70, 70, 30, 30, 8}, {139, 160, 135, 110, 80, 30, 30, 12}});
        tiers(list, "trek_shade", new String[]{"Riyl shadow", "Asyn shadow", "Shade"}, AttackType.CRUSH, 4, 0, "shade",
                new String[]{"undead", "spectral"}, 0.7f, 2.0f, 1.0f, 0x2A2A3A, 0x8080FF,
                new int[][]{{80, 76, 87, 56, 60, 50, 50, 7}, {110, 94, 110, 96, 80, 50, 50, 11}, {140, 115, 130, 136, 100, 50, 50, 15}});
        tiers(list, "trek_swamp_snake", new String[]{"Swamp snake", "Swamp snake", "Swamp snake"}, AttackType.CRUSH, 4, 0, "sea_snake",
                none, 1.4f, 1.0f, 1.4f, 0x3A4A2A, 0x6A8A3A,
                new int[][]{{80, 120, 50, 50, 70, 30, 30, 6}, {109, 125, 70, 110, 80, 30, 30, 12}, {139, 130, 90, 170, 90, 30, 30, 18}});
        tiers(list, "trek_giant_snail", new String[]{"Giant snail", "Giant snail", "Giant snail"}, AttackType.RANGED, 4, 8, "rockslug",
                none, 1.4f, 1.4f, 1.5f, 0x6A5A3A, 0xA08A5A,
                new int[][]{{80, 125, 30, 30, 60, 30, 70, 8}, {109, 150, 30, 30, 75, 30, 110, 13}, {139, 160, 30, 30, 86, 30, 160, 17}});
        // Nail beasts land three quick hits per attack (max 2/3/4 each); modelled as one hit of up to three times that. // approx
        tiers(list, "trek_nail_beast", new String[]{"Nail beast", "Nail beast", "Nail beast"}, AttackType.SLASH, 8, 0, "dark_beast",
                none, 1.2f, 1.4f, 1.1f, 0x5A4A3A, 0xE0D0B0,
                new int[][]{{69, 55, 150, 6, 2, 1, 1, 6}, {98, 65, 215, 16, 30, 1, 1, 9}, {141, 75, 320, 26, 40, 1, 1, 12}},
                Drop.always("big_bones"));
        // Undead lumberjacks (levels 30-70 in OSRS; one per route here) bring planks for the bridge. // approx tiers
        tiers(list, "trek_undead_lumberjack", new String[]{"Undead Lumberjack", "Undead Lumberjack", "Undead Lumberjack"}, AttackType.CRUSH,
                4, 0, "zombie", new String[]{"undead"}, 0.6f, 1.95f, 1.0f, 0x6A8A5A, 0xC03030,
                new int[][]{{30, 10, 73, 6, 10, 1, 1, 2}, {50, 14, 117, 16, 14, 1, 1, 3}, {70, 18, 163, 26, 18, 1, 1, 4}},
                Drop.always("plank"));
        list.add(MonsterDef.of("trek_zombie", "Zombie").level(13).model("zombie").stats(22, 8, 9, 10, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 2)).attributes("undead").drops(Drop.always("bronze_axe"))
                .skin(0x6A8A5A, 0x4A4A3A, 0x3A3A2A, 0x202020));
        // Shades drop their remains for the Shades of Mort'ton.
        String[] remains = {ShadesOfMortton.Shade.RIYL.remains(), ShadesOfMortton.Shade.ASYN.remains(),
                ShadesOfMortton.Shade.FIYR.remains()};
        for (int t = 0; t < 3; t++) {
            MonsterDef shade = list.stream().filter(d -> d.id.startsWith("trek_shade_")).toList().get(t);
            shade.drops(Drop.always(remains[t]));
            shade.flying();
        }
        // The escorted followers: they can't be attacked by players; monsters that reach them hurt them.
        for (Difficulty d : Difficulty.values()) {
            list.add(MonsterDef.of(d.creature(), "Follower").level(0).stats(d.hitpoints, 1, 1, 1, 1, 1).passive()
                    .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                    .attack(AttackDef.of("none", AttackType.CRUSH, 0, 0.5, 1)).skin(0xC8A080, 0x6A4A2A, 0x4A3A2A, 0x3A2A1A));
        }
        return list;
    }

    // ------------------------------------------------------------------ items

    public static void items(ItemSink items) {
        String[][] lj = {{"lumberjack_hat", "Lumberjack hat", "cowl", "HEAD", "+0.4% Woodcutting XP (2.5% with the full outfit)."},
                {"lumberjack_top", "Lumberjack top", "leather_body", "CHEST", "+0.8% Woodcutting XP."},
                {"lumberjack_legs", "Lumberjack legs", "chaps", "LEGS", "+0.6% Woodcutting XP."},
                {"lumberjack_boots", "Lumberjack boots", "boots", "FEET", "+0.2% Woodcutting XP."}};
        int[] colours = {0x6A4A2A, 0xC03030, 0x3A3A4A, 0x4A3A2A};
        for (int i = 0; i < lj.length; i++) {
            items.add(lj[i][0], lj[i][1], Kind.ARMOUR, lj[i][2], colours[i]).value(1).stack(1)
                    .equipment(new EquipmentDef(Slot.valueOf(lj[i][3]), EquipmentStats.NONE, Map.of(), 0, AttackType.CRUSH, WeaponClass.NONE))
                    .examine(lj[i][4]);
        }
        String[][] tokens = {{"reward_token_blue", "Reward token (route 1)", "3060E0"}, {"reward_token_yellow", "Reward token (route 2)", "E0C030"},
                {"reward_token_red", "Reward token (route 3)", "C03030"}};
        for (String[] t : tokens) {
            items.add(t[0], t[1], Kind.SIMPLE, "mark", Integer.parseInt(t[2], 16)).value(1).stack(1)
                    .examine("A reward from a grateful follower. Right-click it to choose your reward.");
        }
        items.add("trek_vine", "Vine", Kind.SIMPLE, "string", 0x3A6A2A).value(1).examine("A short piece of vine.");
        items.add("trek_long_vine", "Long vine", Kind.SIMPLE, "string", 0x2A5A1A).value(1).stack(1)
                .examine("Three vines wound together. Throw it over the big tree's branch to swing across.");
    }
}
