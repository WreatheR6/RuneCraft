package com.runecraft.osrs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Objectives of the hand-scripted quests ({@link Quest}, run by {@code com.runecraft.quest.QuestManager})
 * that happen away from an NPC: places the player must reach (at their OSRS tiles, from the wiki and
 * Zoinkwiz/quest-helper step locations), quest drops from kills, and the items each hand-in takes.
 */
public final class FreeQuestObjectives {
    private FreeQuestObjectives() {
    }

    /** What reaching a {@link Spot} does. */
    public enum Action {
        /** Gives {@code item} (once: not while it, or anything in {@code unless}, is held). */
        GIVE,
        /** Turns each item of {@link #DRUIDIC_MEATS} the player carries into its enchanted form. */
        ENCHANT_MEATS,
        /** Completes the quest. */
        COMPLETE
    }

    /**
     * A place objective active while the quest stage is in [{@code fromStage}, {@code toStage}].
     * {@code x, y} is the OSRS tile; {@code radius} is in tiles.
     */
    public record Spot(Quest quest, int fromStage, int toStage, int x, int y, int radius, Action action, String item,
                       List<String> unless, String message, String spawn) {
        public boolean active(int stage) {
            return stage >= fromStage && stage <= toStage;
        }

        /** Whether OSRS tile (tx, ty) is within the spot. */
        public boolean contains(int tx, int ty) {
            int dx = tx - x;
            int dy = ty - y;
            return dx * dx + dy * dy <= radius * radius;
        }

        /** Whether the objective still has something to do for a player holding {@code has}. */
        public boolean applies(Predicate<String> has) {
            if (action == Action.GIVE) {
                if (has.test(item)) {
                    return false;
                }
                for (String u : unless) {
                    if (has.test(u)) {
                        return false;
                    }
                }
                return true;
            }
            if (action == Action.ENCHANT_MEATS) {
                return DRUIDIC_MEATS.keySet().stream().anyMatch(has);
            }
            return true;
        }
    }

    /** Druidic Ritual: raw meat -> enchanted meat (Cauldron of Thunder). Raw beef/chicken are the vanilla items. */
    public static final Map<String, String> DRUIDIC_MEATS = new LinkedHashMap<>();

    static {
        DRUIDIC_MEATS.put("minecraft:beef", "enchanted_beef");
        DRUIDIC_MEATS.put("raw_rat_meat", "enchanted_rat");
        DRUIDIC_MEATS.put("raw_bear_meat", "enchanted_bear");
        DRUIDIC_MEATS.put("minecraft:chicken", "enchanted_chicken");
    }

    public static final List<Spot> SPOTS = List.of(
            // The Restless Ghost: the skull lies on the altar in the Wizards' Tower basement (3120, 9567); searching it
            // raises a skeleton (level 13). The mod has no basement, so going into the tower (3109, 3162) stands in for
            // climbing down to the altar. // approx
            new Spot(Quest.RESTLESS_GHOST, 3, 3, 3109, 3162, 5, Action.GIVE, "ghosts_skull", List.of(),
                    "You climb down to the tower basement and search the altar: a skull! A skeleton rises to guard it.", "minecraft:skeleton"),
            // Romeo & Juliet: wild cadava bushes south-east of Varrock (wiki map pins 3264,3366 / 3268,3370 / 3277,3374).
            new Spot(Quest.ROMEO_AND_JULIET, 1, 4, 3270, 3370, 9, Action.GIVE, "cadava_berries", List.of("cadava_potion"),
                    "You pick some cadava berries from the bush.", null),
            // Druidic Ritual: the Cauldron of Thunder is in Taverley Dungeon (2893, 9831); the dungeon is a separate cave
            // here, so the meats are dipped at the dungeon entrance ladder (2884, 3397). // approx
            new Spot(Quest.DRUIDIC_RITUAL, 2, 2, 2884, 3397, 4, Action.ENCHANT_MEATS, null, List.of(),
                    "You dip the meat into the Cauldron of Thunder. It glows with a strange light.", null),
            // Witch's Potion: drink from Hetty's cauldron (2967, 3205) to finish.
            new Spot(Quest.WITCHS_POTION, 2, 2, 2967, 3205, 2, Action.COMPLETE, null, List.of(),
                    "You drink from the cauldron. You feel yourself imbued with power.", null));

    /** The spots active for a quest at a stage. */
    public static List<Spot> spots(Quest quest, int stage) {
        return SPOTS.stream().filter(s -> s.quest() == quest && s.active(stage)).toList();
    }

    /** A drop that a kill gives while a quest is at {@code stage}: {@code names} are monster names. */
    public record QuestDrop(Quest quest, int stage, List<String> names, String item, int oneIn) {
        public boolean matches(int questStage, String monsterName) {
            return questStage == stage && names.contains(monsterName);
        }
    }

    public static final List<QuestDrop> DROPS = List.of(
            // Witch's Potion: any standard rat drops the tail (100% while the quest needs it); giant rats don't.
            new QuestDrop(Quest.WITCHS_POTION, 1, List.of("Rat"), "rats_tail", 1),
            // Druidic Ritual: giant rats always drop raw rat meat, bears raw bear meat.
            new QuestDrop(Quest.DRUIDIC_RITUAL, 2, List.of("Giant rat"), "raw_rat_meat", 1),
            new QuestDrop(Quest.DRUIDIC_RITUAL, 2, List.of("Black bear", "Grizzly bear", "Grizzly bear cub", "Bear Cub"),
                    "raw_bear_meat", 1));

    /** Items each hand-in takes (OSRS amounts). */
    public static final Map<String, Integer> COOKS_ASSISTANT = Map.of("minecraft:milk_bucket", 1, "minecraft:egg", 1, "pot_of_flour", 1);
    public static final Map<String, Integer> SHEEP_SHEARER = Map.of("ball_of_wool", 20);
    public static final Map<String, Integer> DORICS_QUEST = Map.of("clay", 6, "copper_ore", 4, "iron_ore", 2);
    public static final Map<String, Integer> WITCHS_POTION = Map.of("rats_tail", 1, "eye_of_newt", 1, "burnt_meat", 1, "onion", 1);
    public static final Map<String, Integer> DRUIDIC_RITUAL = Map.of("enchanted_beef", 1, "enchanted_rat", 1, "enchanted_bear", 1,
            "enchanted_chicken", 1);
    /** Vampyre Slayer: Count Draynor only stays dead if staked with a hammer; the stake is used up. */
    public static final List<String> VAMPYRE_SLAYER_KILL = List.of("stake", "hammer");
}
