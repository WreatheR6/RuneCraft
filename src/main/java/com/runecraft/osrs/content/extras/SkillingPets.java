package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.Herb;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base chances for the skilling pets ({@code 1/(base - level*25)} per action). Gathering pets are
 * keyed by the item the action produced; Thieving by the XP of the pickpocket/stall; Agility by the
 * course whose lap bonus was just awarded; Runecraft per craft. Values follow the OSRS wiki where
 * known; entries marked approx are estimates.
 */
public final class SkillingPets {
    /** A pet roll: which pet, its skill and base chance. */
    public record Roll(String pet, Skill skill, int base) {
    }

    private static final Map<String, Roll> BY_ITEM = new LinkedHashMap<>();
    private static final Map<Integer, Integer> THIEVING_BY_XP_TENTHS = new HashMap<>();
    private static final Map<Integer, Integer> AGILITY_BY_LAP_TENTHS = new HashMap<>();

    public static final String HERON = "pet_heron";
    public static final String ROCK_GOLEM = "pet_rock_golem";
    public static final String BEAVER = "pet_beaver";
    public static final String BABY_CHINCHOMPA = "pet_baby_chinchompa";
    public static final String GIANT_SQUIRREL = "pet_giant_squirrel";
    public static final String TANGLEROOT = "pet_tangleroot";
    public static final String ROCKY = "pet_rocky";
    public static final String RIFT_GUARDIAN = "pet_rift_guardian";

    /** Default base chances when an activity isn't listed. */
    public static final int THIEVING_DEFAULT = 257_211;
    public static final int RUNECRAFT_BASE = 1_795_758;
    public static final int AGILITY_DEFAULT = 35_000; // approx

    private static void item(String pet, Skill skill, int base, String... items) {
        for (String i : items) {
            BY_ITEM.put(i, new Roll(pet, skill, base));
        }
    }

    static {
        // Woodcutting (Beaver): per log cut.
        item(BEAVER, Skill.WOODCUTTING, 317_647, "logs", "achey_tree_logs");
        item(BEAVER, Skill.WOODCUTTING, 361_146, "oak_logs");
        item(BEAVER, Skill.WOODCUTTING, 289_286, "willow_logs");
        item(BEAVER, Skill.WOODCUTTING, 264_336, "teak_logs");
        item(BEAVER, Skill.WOODCUTTING, 221_918, "maple_logs");
        item(BEAVER, Skill.WOODCUTTING, 220_623, "mahogany_logs");
        item(BEAVER, Skill.WOODCUTTING, 145_013, "yew_logs", "arctic_pine_logs");
        item(BEAVER, Skill.WOODCUTTING, 72_321, "magic_logs", "redwood_logs");
        // Mining (Rock golem): per ore mined.
        item(ROCK_GOLEM, Skill.MINING, 741_600, "clay", "copper_ore", "tin_ore", "iron_ore", "silver_ore");
        item(ROCK_GOLEM, Skill.MINING, 290_640, "coal");
        item(ROCK_GOLEM, Skill.MINING, 296_640, "gold_ore");
        item(ROCK_GOLEM, Skill.MINING, 148_320, "mithril_ore");
        item(ROCK_GOLEM, Skill.MINING, 59_328, "adamantite_ore");
        item(ROCK_GOLEM, Skill.MINING, 42_377, "runite_ore");
        item(ROCK_GOLEM, Skill.MINING, 46_350, "amethyst");
        item(ROCK_GOLEM, Skill.MINING, 247_200, "pay_dirt"); // Motherlode Mine (per pay-dirt mined)
        item(ROCK_GOLEM, Skill.MINING, 211_886, "uncut_opal", "uncut_jade", "uncut_red_topaz", "uncut_sapphire", "uncut_emerald",
                "uncut_ruby", "uncut_diamond"); // gem rocks
        // Fishing (Heron): per fish caught.
        item(HERON, Skill.FISHING, 435_165, "raw_shrimps", "raw_anchovies");
        item(HERON, Skill.FISHING, 528_000, "raw_sardine", "raw_herring");
        item(HERON, Skill.FISHING, 461_808, "raw_trout", "raw_salmon");
        item(HERON, Skill.FISHING, 305_792, "raw_pike");
        item(HERON, Skill.FISHING, 128_885, "raw_tuna", "raw_swordfish");
        item(HERON, Skill.FISHING, 116_129, "raw_lobster");
        item(HERON, Skill.FISHING, 138_583, "raw_monkfish");
        item(HERON, Skill.FISHING, 82_243, "raw_shark");
        item(HERON, Skill.FISHING, 170_874, "raw_karambwan");
        item(HERON, Skill.FISHING, 78_649, "raw_anglerfish");
        item(HERON, Skill.FISHING, 149_434, "raw_dark_crab");
        item(HERON, Skill.FISHING, 382_609, "raw_mackerel", "raw_cod", "raw_bass"); // big net
        item(HERON, Skill.FISHING, 160_000, "infernal_eel");
        item(HERON, Skill.FISHING, 300_000, "raw_cave_eel", "raw_slimy_eel", "raw_lava_eel"); // approx
        item(HERON, Skill.FISHING, 166_000, "raw_sea_turtle", "raw_manta_ray"); // approx (Fishing Trawler)
        // Hunter (Baby chinchompa): per chinchompa caught.
        item(BABY_CHINCHOMPA, Skill.HUNTER, 131_395, "grey_chinchompa");
        item(BABY_CHINCHOMPA, Skill.HUNTER, 98_373, "red_chinchompa");
        item(BABY_CHINCHOMPA, Skill.HUNTER, 82_758, "black_chinchompa");
        // Farming (Tangleroot): per harvest. Herbs per the wiki; allotment produce approx.
        for (Herb h : Herb.values()) {
            item(TANGLEROOT, Skill.FARMING, 98_364, h.grimyItem());
        }
        item(TANGLEROOT, Skill.FARMING, 220_000, "potato", "onion", "cabbage", "tomato", "sweetcorn", "strawberry", "watermelon",
                "snape_grass"); // approx

        // Thieving (Rocky): pickpockets default 257,211; stalls and high-level targets are better.
        thieving(16.0, 124_066);   // bakery stall
        thieving(24.0, 68_926);    // silk stall
        thieving(36.0, 36_490);    // fur stall
        thieving(54.0, 36_490);    // silver stall
        thieving(81.0, 36_490);    // spice stall
        thieving(160.0, 36_490);   // gem stall
        thieving(151.75, 127_056); // paladin
        thieving(198.5, 108_718);  // gnome
        thieving(275.0, 99_175);   // hero
        thieving(353.0, 99_175);   // elf
        thieving(103.5, 176_743);  // TzHaar-Hur

        // Agility (Giant squirrel): per lap, by course.
        for (AgilityCourse c : AgilityCourse.values()) {
            int base = switch (c) {
                case GNOME_STRONGHOLD -> 35_609;
                case DRAYNOR -> 33_005;
                case AL_KHARID -> 26_648;
                case VARROCK -> 24_410;
                case CANIFIS -> 36_842;
                case FALADOR -> 26_806;
                case SEERS -> 35_205;
                case POLLNIVNEACH -> 33_422;
                case RELLEKKA -> 31_554;
                case ARDOUGNE -> 34_440;
                default -> AGILITY_DEFAULT;
            };
            AGILITY_BY_LAP_TENTHS.putIfAbsent(Experience.tenths(c.lapBonusXp()), base);
        }
    }

    private static void thieving(double xp, int base) {
        THIEVING_BY_XP_TENTHS.put(Experience.tenths(xp), base);
    }

    private SkillingPets() {
    }

    /** Pet roll for an item a gathering action produced, or null. */
    public static Roll forItem(String item) {
        return BY_ITEM.get(item);
    }

    /** Base chance for a Thieving action that gave {@code tenths} XP. */
    public static int thievingBase(int tenths) {
        return THIEVING_BY_XP_TENTHS.getOrDefault(tenths, THIEVING_DEFAULT);
    }

    /** Base chance if {@code tenths} Agility XP is a course's lap bonus, else 0 (not a lap). */
    public static int agilityLapBase(int tenths) {
        return AGILITY_BY_LAP_TENTHS.getOrDefault(tenths, 0);
    }

    public static Map<String, Roll> items() {
        return BY_ITEM;
    }
}
