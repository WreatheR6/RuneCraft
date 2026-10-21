package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Small activities behind achievement diary tasks (round 40, OSRS wiki): chompy bird hunting and Rantz's chompy bird hats,
 * Gertrude's kittens, the Desert Phoenix's feathers, Varrock's stray dogs, the white tree in Varrock Palace's garden, the
 * Altar of Nature and the Stronghold of Security's emotes.
 */
public final class DiaryActivities {
    private DiaryActivities() {
    }

    // ---------------- Chompy bird hunting ----------------

    /** A chompy bird hat Rantz hands out: kills registered, item id and name (wiki "Chompy bird hunting"). */
    public record Hat(int kills, String item, String name) {
    }

    public static final List<Hat> HATS;

    static {
        Object[][] rows = {{30, "ogre bowman"}, {40, "bowman"}, {50, "ogre yeoman"}, {70, "yeoman"}, {95, "ogre marksman"},
                {125, "marksman"}, {170, "ogre woodsman"}, {225, "woodsman"}, {300, "ogre forester"}, {400, "forester"},
                {550, "ogre bowmaster"}, {700, "bowmaster"}, {1000, "ogre expert"}, {1300, "expert"}, {1700, "ogre dragon archer"},
                {2250, "dragon archer"}, {3000, "expert ogre dragon archer"}, {4000, "expert dragon archer"}};
        List<Hat> hats = new ArrayList<>();
        for (Object[] r : rows) {
            String n = (String) r[1];
            hats.add(new Hat((Integer) r[0], "chompy_bird_hat_" + n.replace(' ', '_'), "Chompy bird hat (" + n + ")"));
        }
        HATS = List.copyOf(hats);
    }

    public static final String CHOMPY = "chompy_bird";
    /** Where a player's registered chompy kills are kept (a persisted per-player counter). */
    public static final String CHOMPY_KILLS = "chompy_kills";
    /** Swamp gas loads a pair of bellows holds (wiki "Ogre bellows"). */
    public static final int BELLOWS_LOADS = 3;
    public static final String BELLOWS = "ogre_bellows";
    public static final String BLOATED_TOAD = "bloated_toad";
    /** Bows whose kills count (OSRS: the ogre bow or the comp ogre bow; the latter doesn't exist here). */
    public static final List<String> CHOMPY_BOWS = List.of("ogre_bow", "comp_ogre_bow");
    /** Bloated toads carried at once (wiki: at most 3). */
    public static final int MAX_TOADS = 3;
    /** A chompy despawns 60 seconds after it appears (wiki), in game ticks. */
    public static final int CHOMPY_TICKS = 100;
    /** Game ticks after a toad is put down until its chompy arrives. // approx (OSRS: a random wait) */
    public static final int CHOMPY_ARRIVAL_TICKS = 5;
    /** The one-time Ranged XP for the 4,000th chompy (wiki). */
    public static final int CHOMPY_4000_XP = 30000;

    /** The swamp ponds where toads are caught and chompies hunted: the Feldip Hills west of Rantz and the Chompy Marsh (wiki). */
    public static final int[][] PONDS = {{2549, 2985}, {2527, 2978}, {2598, 2966}, {2337, 3061}, {2395, 3048}};
    /** How far from a pond's centre (tiles) its swamp and toads reach. // approx */
    public static final int POND_RADIUS = 12;

    /** The pond (index) within reach of an OSRS tile, or -1. */
    public static int pondAt(int x, int y) {
        for (int i = 0; i < PONDS.length; i++) {
            if (Teleports.within(x, y, PONDS[i][0], PONDS[i][1], POND_RADIUS)) {
                return i;
            }
        }
        return -1;
    }

    /** Hats a kill count unlocks, cheapest first. */
    public static List<Hat> hatsFor(int kills) {
        List<Hat> out = new ArrayList<>();
        for (Hat h : HATS) {
            if (kills >= h.kills()) {
                out.add(h);
            }
        }
        return out;
    }

    /** The best hat a kill count unlocks, or null below 30 kills. */
    public static Hat bestHat(int kills) {
        List<Hat> hats = hatsFor(kills);
        return hats.isEmpty() ? null : hats.get(hats.size() - 1);
    }

    public static boolean isHat(String item) {
        return HATS.stream().anyMatch(h -> h.item().equals(item));
    }

    /**
     * Activity keys a hat claim reports: "chompy_hat" and "chompy_hat:N" for each diary threshold reached (the Western
     * Provinces tasks: any hat, then 125, 300 and 1,000 kills registered).
     */
    public static List<String> hatKeys(int kills) {
        List<String> out = new ArrayList<>(List.of("chompy_hat"));
        for (int n : new int[]{125, 300, 1000}) {
            if (kills >= n) {
                out.add("chompy_hat:" + n);
            }
        }
        return out;
    }

    /**
     * Chance (percent) that a toad brings two chompies (wiki, Western Provinces rewards): 25% after the easy diary, 50% after
     * the medium, always after the elite.
     */
    public static int twoChompiesPercent(int westernTiersDone) {
        return westernTiersDone >= 4 ? 100 : westernTiersDone >= 2 ? 50 : westernTiersDone >= 1 ? 25 : 0;
    }

    /** Ogre arrows Rantz hands out each day (wiki): 25 / 50 / 100 / 150 after the easy / medium / hard / elite diary. */
    public static int rantzDailyArrows(int westernTiersDone) {
        return switch (Math.max(0, Math.min(4, westernTiersDone))) {
            case 1 -> 25;
            case 2 -> 50;
            case 3 -> 100;
            case 4 -> 150;
            default -> 0;
        };
    }

    // ---------------- Gertrude's kittens ----------------

    /** Kitten colours (wiki "Kitten"); with an activated ring of charos Gertrude lets the player choose one. */
    public static final List<String> KITTEN_COLOURS = List.of("Grey and black", "White", "Brown", "Black", "Grey and brown",
            "Grey and blue");
    public static final String KITTEN = "gertrudes_kitten";
    /** Gertrude's price for each kitten after the first (wiki). */
    public static final int KITTEN_PRICE = 100;

    // ---------------- The Desert Phoenix ----------------

    public static final String PHOENIX = "desert_phoenix";
    public static final String PHOENIX_FEATHER = "phoenix_feather";
    /** Thieving level to grab a feather (wiki; boosts count). */
    public static final int PHOENIX_THIEVING = 25;
    /** The phoenix's wandering spot north-west of the Ruins of Uzer (wiki map 3419, 3155). */
    public static final int PHOENIX_X = 3419;
    public static final int PHOENIX_Y = 3155;
    /** Chance of grabbing a feather at a Thieving level. // approx (the wiki gives no rate) */
    public static double phoenixGrabChance(int thieving) {
        return thieving < PHOENIX_THIEVING ? 0 : Math.min(0.95, 0.6 + (thieving - PHOENIX_THIEVING) / 100.0);
    }

    /** Thieving XP for a feather (wiki "Phoenix feather"); a failed grab stuns the player and hits for a little. */
    public static final double PHOENIX_XP = 26;
    /** Stun after a failed grab, in game ticks, and its damage. // approx (the wiki says "a small amount") */
    public static final int PHOENIX_STUN_TICKS = 8;
    public static final int PHOENIX_FAIL_DAMAGE = 2;

    // ---------------- Varrock's stray dogs ----------------

    public static final String STRAY_DOG = "stray_dog";
    /** Bones a stray dog refuses (wiki: "That's disgusting - you can't give it to the poor dog!"). */
    public static final List<String> DOG_REFUSES = List.of("shaikahan_bones", "marinated_j_bones");
    /** The stray dogs around Varrock (mejrs' NPC list). */
    public static final int[][] STRAY_DOGS = {{3200, 3399}, {3233, 3394}, {3262, 3410}, {3168, 3440}, {3277, 3381}};

    // ---------------- The white tree ----------------

    public static final String WHITE_TREE = "white_tree";
    public static final String WHITE_TREE_FRUIT = "white_tree_fruit";
    /** The white tree planted in Varrock Palace's garden in Garden of Tranquillity (wiki map 3230, 3475). */
    public static final int WHITE_TREE_X = 3230;
    public static final int WHITE_TREE_Y = 3475;
    /** Picking a fruit: Farming level, XP, the tree's fruit and its regrowth (wiki: one fruit about every 5 minutes, at most 4). */
    public static final int WHITE_TREE_FARMING = 25;
    public static final double WHITE_TREE_XP = 12;
    public static final int WHITE_TREE_MAX_FRUIT = 4;
    public static final int WHITE_TREE_REGROW_TICKS = 500;

    /** Fruit on the tree for a player who left {@code fruit} on it {@code ticksSince} game ticks ago (regrowing to the cap). */
    public static int whiteTreeFruit(int fruit, long ticksSince) {
        return (int) Math.min(WHITE_TREE_MAX_FRUIT, fruit + Math.max(0, ticksSince) / WHITE_TREE_REGROW_TICKS);
    }

    // ---------------- The Altar of Nature ----------------

    /**
     * The Altar of Nature in Filliman Tarlock's grotto (wiki: 3441, 9740, after Nature Spirit). The grotto isn't carved, so
     * the altar stands in the swamp above it (OSRS y - 6400). // approx
     */
    public static final String NATURE_ALTAR = "altar_of_nature";
    public static final int NATURE_ALTAR_X = 3441;
    public static final int NATURE_ALTAR_Y = 3340;

    // ---------------- Safta Doc's gem machetes ----------------

    /** Safta Doc, by Tai Bwo Wannai's calquat patch (mejrs' NPC list 2790, 3100). */
    public static final String SAFTA_DOC = "diary_safta_doc";
    public static final int SAFTA_DOC_X = 2790;
    public static final int SAFTA_DOC_Y = 3100;
    public static final String GOUT_TUBER = "gout_tuber";
    public static final String TRADING_STICKS = "trading_sticks";
    /** Gems Safta Doc sets in each machete (three, cut or uncut). */
    public static final int MACHETE_GEMS = 3;

    /** A gem machete: the gem (cut id; "uncut_" + gem also counts) and its price in trading sticks, plus a gout tuber (wiki). */
    public record Machete(String item, String gem, int sticks) {
    }

    public static final List<Machete> MACHETES = List.of(new Machete("opal_machete", "opal", 300),
            new Machete("jade_machete", "jade", 600), new Machete("red_topaz_machete", "red_topaz", 1200));

    // ---------------- The Stronghold of Security's emotes ----------------

    /** The emotes the Stronghold's four reward chests unlock, in floor order (wiki). */
    public static final List<String> STRONGHOLD_EMOTES = List.of("Flap", "Slap Head", "Idea", "Stamp");

    /** Emotes anyone can perform, plus the Stronghold's once unlocked. */
    public static final List<String> BASIC_EMOTES = List.of("Yes", "No", "Bow", "Angry", "Think", "Wave", "Shrug", "Cheer", "Beckon",
            "Laugh", "Jump for Joy", "Yawn", "Dance", "Jig", "Spin", "Headbang", "Cry", "Blow Kiss", "Panic", "Raspberry", "Clap",
            "Salute");

    /** The activity key of an emote ("emote:slap_head"). */
    public static String emoteKey(String emote) {
        return "emote:" + Diaries.slug(emote);
    }

    /** The '&' target of the Varrock task: each Stronghold emote performed. */
    public static String strongholdEmotesTarget() {
        StringBuilder b = new StringBuilder();
        for (String e : STRONGHOLD_EMOTES) {
            b.append(b.length() == 0 ? "" : "&").append(emoteKey(e));
        }
        return b.toString();
    }

    // ---------------- Items and creatures ----------------

    public static void items(ItemSink items) {
        for (Hat h : HATS) {
            items.add(h.item(), h.name(), ItemDef.Kind.ARMOUR, "cowl", 0x6A8A3A).value(1).stack(1)
                    .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, EquipmentStats.NONE, 1))
                    .examine("A hat for those who have killed " + h.kills() + " chompy birds.");
        }
        Defs.simple(items, BELLOWS, "Ogre bellows", "pot", 0x8A6A3A, 4).stack(1)
                .examine("A large pair of ogre bellows.");
        Defs.simple(items, PHOENIX_FEATHER, "Phoenix feather", "secondary", 0xE06020, 1)
                .examine("A long feather patterned like a flame.");
        items.add(WHITE_TREE_FRUIT, "White tree fruit", ItemDef.Kind.FOOD, "produce", 0xF0F0E8).value(1).heal(3)
                .examine("Looks delicious.");
    }

    /** The stray dogs (unattackable in OSRS: passive here) and the Desert Phoenix, with existing models. */
    public static List<MonsterDef> monsters() {
        return List.of(
                MonsterDef.of(STRAY_DOG, "Stray dog").level(1).stats(5, 1, 1, 1, 1, 1).speed(4).passive()
                        .attack(com.runecraft.osrs.content.AttackDef.melee(1)).model("wild_dog").size(0.6f, 0.8f, 0.8f)
                        .skin(0x8A6A4A, 0x5A4430, 0x5A4430, 0xC8A880).spawn(com.runecraft.osrs.Region.MISTHALIN, 5, 1, 1),
                MonsterDef.of(PHOENIX, "Desert Phoenix").level(1).stats(50, 1, 1, 1, 1, 1).speed(4).passive()
                        .attack(com.runecraft.osrs.content.AttackDef.melee(1)).model("ext_pet_phoenix").size(0.9f, 1.4f, 1.8f)
                        .skin(0xE06020, 0xF0D040, 0xE06020, 0xF0D040).spawn(com.runecraft.osrs.Region.KHARIDIAN_DESERT, 1, 1, 1)
                        // the region map puts the clay mines north of Uzer on Morytania's side of the River Salve's latitude
                        .spawn(com.runecraft.osrs.GielinorMap.regionAt(PHOENIX_X, PHOENIX_Y), 1, 1, 1));
    }
}
