package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Varrock Museum kudos, after the OSRS wiki:
 * <ul>
 *   <li>Natural history quiz (Orlando Smith): 14 displays with six questions each; three right answers on a
 *       display's plaque complete it for 2 kudos (28 in all), and finishing every display earns 1,000 Hunter
 *       and Slayer XP from Orlando.</li>
 *   <li>Cleaning finds (after The Dig Site): take uncleaned finds from the specimen rocks, clean them at a
 *       specimen table wearing leather boots and gloves with a trowel, rock pick and specimen brush (from the
 *       tool rack). 14% crumble; the rest roll the wiki's table. The five display artefacts (pottery, old and
 *       ancient symbols and coins) are worth 10 kudos each in their display cases; pottery, jewellery, old
 *       chipped vases and arrowheads go in the storage crate for its reward table.</li>
 *   <li>Historian Minas: 5 kudos for each of 14 quests and 10 for Curse of the Empty Lord (80 in all), some
 *       with antique lamps.</li>
 *   <li>The information clerk's XP rewards at 51, 101 and 151 kudos; Haig Halen (50) and Orlando (153) for the
 *       Varrock diary ({@code kudos:haig_halen_50} and {@code kudos:orlando_153} activities).</li>
 * </ul>
 * The museum's basement isn't built, so Orlando and the plaque stand on the ground floor. Fossil displays
 * (72 kudos), Camdozaal relics (5) and salvaged relics are not wired to kudos here.
 */
public final class VarrockMuseum {
    private VarrockMuseum() {
    }

    public static final String PLACE = "mg_varrock_museum";
    /** Inside the museum's south end, the cleaning area (OSRS tile). */
    public static final int X = 3258;
    public static final int Y = 3442;
    public static final String DIG_SITE = "the_dig_site";

    public static final String ORLANDO = "mg_orlando_smith";
    public static final String CLERK = "mg_information_clerk";
    public static final String ARCHAEOLOGIST = "mg_museum_archaeologist";
    public static final String PLAQUE = "museum_plaque";
    public static final String SPECIMEN_ROCKS = "museum_specimen_rocks";
    public static final String SPECIMEN_TABLE = "museum_specimen_table";
    public static final String TOOL_RACK = "museum_tool_rack";
    public static final String STORAGE_CRATE = "museum_storage_crate";
    public static final String DISPLAY_CASE = "museum_display_case";

    public static final int KUDOS_PER_DISPLAY = 2;
    public static final int QUIZ_RIGHT_ANSWERS = 3;
    public static final int QUIZ_XP = 1000;
    public static final int ARTEFACT_KUDOS = 10;
    public static final double CRUMBLE_CHANCE = 0.14;
    public static final int HAIG_HALEN_KUDOS = 50;
    public static final int ORLANDO_KUDOS = 153;

    /** One quiz question: its display, the question, three answers and the right one's index. */
    public record Question(String display, String question, List<String> answers, int correct) {
    }

    /** The natural history quiz from the OSRS wiki (display|question|answer|answer|answer|right index). */
    private static final String QUIZ_DATA = """
            Lizard|How does a lizard regulate body heat?|Chilli sauce|Fire|Sunlight|2
            Lizard|Who discovered how to kill lizards?|Admiral Bake|The Slayer Masters|The Wise Old Man|1
            Lizard|How many eyes does a lizard have?|Two|Three|Six|1
            Lizard|What order do lizards belong to?|Mammal|Squamata|Insecta|1
            Lizard|What happens when a lizard becomes cold?|It becomes sleepy|It becomes aggressive|It explodes|0
            Lizard|Lizard skin is made of the same substance as?|Hair|Bone|Wood|0
            Battle tortoise|What is the name of the oldest tortoise ever recorded?|Healthorg|Mibbiwocket|Snookums|1
            Battle tortoise|What is a tortoise's favourite food?|Sharks|Kebabs|Vegetables|2
            Battle tortoise|Name the explorer who discovered the world's oldest tortoise.|Admiral Bake|Captain Cook|Ensign Chef|0
            Battle tortoise|How does the tortoise protect itself?|Thick skin|Magic|Hard shell|2
            Battle tortoise|If a tortoise had twenty rings on its shell, how old would it be?|Ten years|Twenty years|Sixty years|1
            Battle tortoise|Which race breeds tortoises for battle?|Gnomes|Orks|Elves|0
            Dragon|What is considered a delicacy by dragons?|Adventurers|Runite|Cheese|1
            Dragon|What is the best defence against a dragon's attack?|Anti-dragon-breath shield|Rune plate|Cats|0
            Dragon|How long do dragons live?|Fifty years|Forever|Unknown|2
            Dragon|Which of these is not a type of dragon?|Coloured|Elemental|Ferrous|1
            Dragon|What is the favoured territory of a dragon?|Swamps|Old battle sites|Lumbridge Castle|1
            Dragon|Approximately how many feet tall do dragons stand?|Twelve|Six|Ten|0
            Wyvern|How did the wyverns die out?|Climate change|Disease|Marauding adventurers|0
            Wyvern|How many legs does a wyvern have?|Two|Five|Six|0
            Wyvern|Where have wyvern bones been found?|Ape Atoll|Al Kharid|Asgarnia|2
            Wyvern|Which genus does the wyvern theoretically belong to?|Invertebrates|Reptiles|Mammals|1
            Wyvern|What are the wyverns' closest relations?|Lizards|Snakes|Dragons|2
            Wyvern|What is the ambient temperature of wyvern bones?|Room temperature|Below room temperature|Above room temperature|1
            Snail|What is special about the shell of the giant Morytanian snail?|It glows in the dark|It is very tasty|It is resistant to acid|2
            Snail|How do Morytanian snails capture their prey?|Aggressive marketing|Spitting acid|Hex them until they glow|1
            Snail|Which of these is a snail byproduct?|Fireproof oil|A musical instrument|Snail-a-cola|0
            Snail|What does 'Achatina Acidia' mean?|Giant acid-spitting snail|Acid-spitting snail|Giant cheese-spitting snail|1
            Snail|How do snails move?|Flapping their wings|Contracting and stretching|Hopping about|1
            Snail|What is the 'trapdoor', which snails use to cover the entrance to their shells called?|An operculum|A caldarium|A tavernium|0
            Snake|What is snake venom adapted from?|Ear wax|Stomach acid|Saliva|1
            Snake|Aside from their noses, what do snakes use to smell?|Tongue|Eyes|Ears|0
            Snake|If a snake sticks its tongue out at you, what is it doing?|Being rude|Seeing how you taste|Seeing how you smell|2
            Snake|If some snakes use venom to kill their prey, what do other snakes use?|Fire|Constriction|Magic|1
            Snake|Lizards and snakes belong to the same order - what is it?|Squamata|Insecta|Sarcopterghii|0
            Snake|Which habitat do snakes prefer?|The desert|The ocean|Anywhere|2
            Sea slug|We assume that sea slugs have a stinging organ on their soft skin - what is it called?|Nematocysts|Hematocysts|Pematocysts|0
            Sea slug|Why has the museum never examined a live sea slug?|The sea slugs are shy|The researchers keep vanishing|Sea slugs are extinct|1
            Sea slug|What do we think the sea slug feeds upon?|Blood|Seaweed|Insects|1
            Sea slug|What are the two fangs presumed to be used for?|Defence or display|Attacking prey|Latching on to objects|0
            Sea slug|Off of which coastline would you find sea slugs?|Karamja|Miscellania|Ardougne|2
            Sea slug|In what way are sea slugs similar to snails?|They eat lettuce|They move very slowly|They have a hard shell|2
            Monkey|Which type of primates do monkeys belong to?|Simian|Hominid|Lagomorpha|0
            Monkey|Which have the lighter colour: Karamjan or Harmless monkeys?|They are the same|Karamjan|Harmless|2
            Monkey|Monkeys love bananas. What else do they like to eat?|Chili|Seaweed|Bitternuts|2
            Monkey|There are two known families of monkeys. One is Karamjan, the other is...?|Pointless|Harmless|Hairless|1
            Monkey|What colour mohawk do Karamjan monkeys have?|Red|Blue|Green|0
            Monkey|What have Karamjan monkeys taken a deep dislike to?|Seaweed|Chili|Taxes|0
            Kalphite Queen|Kalphites are ruled by a...?|King|Pasha|Caliph|1
            Kalphite Queen|What is the lowest caste in kalphite society?|Prince|Soldier|Worker|2
            Kalphite Queen|What are the armoured plates on a kalphite called?|Hauberk|Lamellae|Shield|1
            Kalphite Queen|Are kalphites carnivores, herbivores, or omnivores?|Omnivores|Herbivores|Carnivores|2
            Kalphite Queen|What are kalphites assumed to have evolved from?|Scarab beetles|Penguins|Dragons|0
            Kalphite Queen|Name the prominent figure in kalphite mythology?|Postie Pete|Scabaras|Jorral the Historian|1
            Terrorbird|What is a terrorbird's preferred food?|Anything|Pizza|Vegetables|0
            Terrorbird|Who use terrorbirds as mounts?|Dwarves|Gnomes|Goblins|1
            Terrorbird|Where do terrorbirds get most of their water?|Streams and rivers|Licking trees|Eating plants|2
            Terrorbird|How many claws do terrorbirds have?|Two|Four|Six|1
            Terrorbird|What do terrorbirds eat to aid digestion?|Blurite|Stones|Paper|1
            Terrorbird|How many teeth do terrorbirds have?|28|14|0|2
            Penguin|Which sense do penguins rely on when hunting?|Smell|Taste|Sight|2
            Penguin|Which skill seems unusual for the penguins to possess?|Planning|Fishing|Sleeping|0
            Penguin|How do penguins keep warm?|A layer of fat|Vigorous exercise|Magic|0
            Penguin|What is the preferred climate for penguins?|Cold|Hot|Damp|0
            Penguin|Describe the behaviour of penguins?|Social|Asocial|Hive|0
            Penguin|When do penguins fast?|Just before dawn|During breeding|During the night|1
            Mole|What habitat do moles prefer?|Subterranean|Any|Subaquatic|0
            Mole|Why are moles considered to be an agricultural pest?|They dig holes|They eat food meant for livestock|They attack farmers|0
            Mole|Who discovered giant moles?|Sir Tiffy Cashien|Ak-Haranu|Wyson the Gardener|2
            Mole|What would you call a group of young moles?|A litter|A mess|A labour|2
            Mole|What is a mole's favourite food?|Insects and other invertebrates|Humans and gnomes|Seeds and plant roots|0
            Mole|Which family do moles belong to?|The Malpidae family|The Talpidae family|The Falpidae family|1
            Camel|What is produced by feeding chilli to a camel?|An irate camel|Toxic dung|An undead bactrian|1
            Camel|If an ugthanki has one, how many does a bactrian have?|One|Two|Four|1
            Camel|Camels:herbivore, carnivore or omnivore?|Herbivore|Carnivore|Omnivore|2
            Camel|What is the usual mood for a camel?|Annoyed|Bemused|Cheerful|0
            Camel|Where would you find an ugthanki?|Al Kharid|Morytania|Nowhere|0
            Camel|Which camel byproduct is known to be very nutritious?|Milk|Dung|Meat|0
            Leech|What is the favoured habitat of leeches?|Water|Desert|Trees|0
            Leech|What shape is the inside of a leech's mouth?|Star-shaped|'X'-shaped|'Y'-shaped|2
            Leech|Which of these is not eaten by leeches?|Meat|Apples|Blood|1
            Leech|What contributed to the giant growth of Morytanian leeches?|Environment|Healthy living|Magic|0
            Leech|What is special about the Morytanian leeches?|They sing|They attack by jumping|They have legs|1
            Leech|How does a leech change when it feeds?|It turns blue|It starts singing|It doubles in size|2
            """;

    public static final List<Question> QUIZ = parseQuiz();
    /** The 14 displays, in the wiki's order. */
    public static final List<String> DISPLAYS = QUIZ.stream().map(Question::display).distinct().toList();

    private static List<Question> parseQuiz() {
        List<Question> out = new ArrayList<>();
        for (String line : QUIZ_DATA.strip().split("\n")) {
            String[] f = line.strip().split("\\|");
            out.add(new Question(f[0], f[1], List.of(f[2], f[3], f[4]), Integer.parseInt(f[5])));
        }
        return List.copyOf(out);
    }

    /** The questions of display {@code d} (index into {@link #DISPLAYS}). */
    public static List<Question> questions(int d) {
        String name = DISPLAYS.get(d);
        return QUIZ.stream().filter(q -> q.display().equals(name)).toList();
    }

    /** A quest Historian Minas rewards: kudos and an optional antique lamp (XP, minimum skill level). */
    public record QuestKudos(String quest, int kudos, int lampXp, int lampLevel) {
    }

    /** Historian Minas' timeline displays (OSRS wiki): 80 kudos and 18,000 lamp XP in all. */
    public static final List<QuestKudos> QUEST_KUDOS = List.of(
            new QuestKudos("DEMON_SLAYER", 5, 0, 0),
            new QuestKudos("RUNE_MYSTERIES", 5, 0, 0),
            new QuestKudos("shield_of_arrav", 5, 1000, 20),
            new QuestKudos("a_tail_of_two_cats", 5, 0, 0),
            new QuestKudos("hazeel_cult", 5, 0, 0),
            new QuestKudos("in_aid_of_the_myreque", 5, 0, 0),
            new QuestKudos("making_history", 5, 1000, 20),
            new QuestKudos("merlins_crystal", 5, 1000, 20),
            new QuestKudos("observatory_quest", 5, 0, 0),
            new QuestKudos("priest_in_peril", 5, 0, 0),
            new QuestKudos("temple_of_ikov", 5, 0, 0),
            new QuestKudos("the_grand_tree", 5, 0, 0),
            // Needs the Dagon'hai history book from the Varrock Palace library. // approx: not required
            new QuestKudos("what_lies_below", 5, 0, 0),
            // Needs a complete set of ghostly robes. // approx: not required
            new QuestKudos("curse_of_the_empty_lord", 10, 10000, 50),
            new QuestKudos("defender_of_varrock", 5, 5000, 30));

    /** The information clerk's rewards: kudos needed and XP per skill. */
    public record KudosReward(int kudos, Map<Skill, Integer> xp) {
    }

    public static final List<KudosReward> KUDOS_REWARDS = List.of(
            new KudosReward(51, Map.of(Skill.MINING, 1000)),
            new KudosReward(101, Map.of(Skill.CRAFTING, 2500, Skill.MINING, 2500)),
            new KudosReward(151, Map.of(Skill.CRAFTING, 4000, Skill.HUNTER, 4000, Skill.PRAYER, 4000, Skill.SLAYER, 4000,
                    Skill.SMITHING, 4000)));

    /** The five display artefacts, worth 10 kudos each once put on display. */
    public static final List<String> ARTEFACTS = List.of("pottery", "old_symbol", "ancient_symbol", "ancient_coin", "old_coin");
    /** Finds the storage crate takes. */
    public static final List<String> CRATE_FINDS = List.of("pottery", "museum_jewellery", "old_chipped_vase", "museum_arrowheads");
    public static final List<String> TOOLS = List.of("trowel", "rock_pick", "specimen_brush");

    /** Kudos available from what this module models: 28 quiz + 50 artefacts + 80 quests. */
    public static int modelledKudos() {
        return DISPLAYS.size() * KUDOS_PER_DISPLAY + ARTEFACTS.size() * ARTEFACT_KUDOS
                + QUEST_KUDOS.stream().mapToInt(QuestKudos::kudos).sum();
    }

    /**
     * What a cleaned find turns into (after the 14% crumble roll). {@code r} is uniform in [0, 1);
     * {@code owned} holds the uniques the player is carrying or has displayed (those aren't rolled
     * again: the unremarkable table is rolled instead).
     */
    public static String cleanedFind(double r, double r2, Collection<String> owned) {
        // Uniques: 3/51 each for the symbols and coins, 1/51 the clean necklace; else 38/51 common.
        String[] uniques = {"old_symbol", "ancient_symbol", "old_coin", "ancient_coin"};
        double x = r * 51;
        for (String u : uniques) {
            if (x < 3) {
                return owned.contains(u) ? unremarkable(r2) : u;
            }
            x -= 3;
        }
        if (x < 1) {
            return owned.contains("clean_necklace") ? unremarkable(r2) : "clean_necklace";
        }
        // Common artefacts: 38/204 each of pottery, jewellery, old chipped vase and arrowheads.
        return CRATE_FINDS.get(Math.min(3, (int) Math.floor(r2 * 4)));
    }

    /** The unremarkable table (26/459 broken arrow, glass, iron dagger; 13/459 uncut jade, bones, 5 coins). */
    static String unremarkable(double r) {
        double x = r * 117;
        String[] items = {"broken_arrow", "broken_glass", "iron_dagger", "uncut_jade", "bones", "coins"};
        int[] w = {26, 26, 26, 13, 13, 13};
        for (int i = 0; i < items.length; i++) {
            if (x < w[i]) {
                return items[i];
            }
            x -= w[i];
        }
        return "coins";
    }

    /** A storage crate reward line: item, count range and weight (out of 195). */
    public record CrateLoot(String item, int min, int max, int weight) {
    }

    /** Storage crate rewards (OSRS wiki). The 21/195 pot isn't an item here, so its weight goes to the bowl. // approx */
    public static final List<CrateLoot> CRATE = List.of(
            new CrateLoot("coins", 17, 69, 19), new CrateLoot("bowl", 1, 1, 42), new CrateLoot("iron_bolts", 1, 10, 10),
            new CrateLoot("iron_dagger", 1, 1, 10), new CrateLoot("bronze_limbs", 1, 1, 10), new CrateLoot("wooden_stock", 1, 1, 10),
            new CrateLoot("tin_ore", 1, 1, 10), new CrateLoot("copper_ore", 1, 1, 10), new CrateLoot("iron_ore", 1, 1, 7),
            new CrateLoot("coal", 1, 1, 10), new CrateLoot("mithril_ore", 1, 1, 5), new CrateLoot("iron_knife", 1, 1, 10),
            new CrateLoot("iron_dart", 1, 50, 10), new CrateLoot("iron_arrowtips", 1, 49, 10), new CrateLoot("uncut_jade", 1, 1, 5),
            new CrateLoot("uncut_opal", 1, 1, 5), new CrateLoot("big_bones", 1, 1, 10));
    /** The 2/195 antique lamp (no lamp item: {@link #crateRoll} returns null for it). */
    public static final int CRATE_LAMP_WEIGHT = 2;

    public static CrateLoot crateRoll(double r) {
        int total = CRATE.stream().mapToInt(CrateLoot::weight).sum() + CRATE_LAMP_WEIGHT;
        int x = (int) Math.floor(r * total);
        for (CrateLoot l : CRATE) {
            x -= l.weight();
            if (x < 0) {
                return l;
            }
        }
        return null;
    }

    /** Antique lamp from the storage crate: 500 XP in a skill of level 10 or more. */
    public static final int CRATE_LAMP_XP = 500;

    /**
     * The antique lamp item (CommonItems) for a museum reward of {@code xp}: the crate's (500), the three timeline quests'
     * (1,000), Defender of Varrock's (5,000) or Curse of the Empty Lord's (10,000). The player rubs it to pick the skill.
     */
    public static String lampItem(int xp) {
        return switch (xp) {
            case CRATE_LAMP_XP -> com.runecraft.osrs.content.CommonItems.MUSEUM_LAMP_FINDS;
            case 1000 -> com.runecraft.osrs.content.CommonItems.MUSEUM_LAMP_TIMELINE;
            case 5000 -> com.runecraft.osrs.content.CommonItems.MUSEUM_LAMP_DEFENDER;
            case 10000 -> com.runecraft.osrs.content.CommonItems.MUSEUM_LAMP_EMPTY_LORD;
            default -> throw new IllegalArgumentException("no museum lamp of " + xp + " XP");
        };
    }

    public static void items(ItemSink items) {
        items.add("trowel", "Trowel", Kind.TOOL, "trowel", 0x8A8A8A).value(1).stack(1).examine("A small hand-held digging tool.");
        items.add("rock_pick", "Rock pick", Kind.TOOL, "pickaxe", 0x7A7A7A).value(1).stack(1).examine("A small pick for digging.");
        items.add("specimen_brush", "Specimen brush", Kind.TOOL, "chisel", 0xB08A50).value(1).stack(1)
                .examine("Used to clean specimens.");
        items.add("uncleaned_find", "Uncleaned find", Kind.SIMPLE, "ore", 0x8A6A4A).value(1).stack(1)
                .examine("This needs cleaning.");
        items.add("old_symbol", "Old symbol", Kind.SIMPLE, "amulet", 0x9A9AA0).value(1).stack(1).examine("An old symbol.");
        items.add("ancient_symbol", "Ancient symbol", Kind.SIMPLE, "amulet", 0xC0A050).value(1).stack(1).examine("An ancient symbol.");
        items.add("old_coin", "Old coin", Kind.SIMPLE, "coins", 0xB09060).value(1).stack(1).examine("An old coin.");
        items.add("ancient_coin", "Ancient coin", Kind.SIMPLE, "coins", 0xD0B040).value(1).stack(1).examine("An ancient coin.");
        items.add("clean_necklace", "Clean necklace", Kind.SIMPLE, "necklace", 0xC0C0C0).value(1).stack(1)
                .examine("A clean necklace from the Digsite.");
        items.add("pottery", "Pottery", Kind.SIMPLE, "pot_item", 0xB0703A).value(1).stack(1).examine("A piece of old pottery.");
        items.add("museum_jewellery", "Jewellery", Kind.SIMPLE, "necklace", 0xC0A060).value(1).stack(1)
                .examine("An old piece of jewellery.");
        items.add("old_chipped_vase", "Old chipped vase", Kind.SIMPLE, "pot_item", 0x9A7A5A).value(1).stack(1)
                .examine("An old chipped vase.");
        items.add("museum_arrowheads", "Arrowheads", Kind.SIMPLE, "arrowtips", 0x8A8A8A).value(1).stack(1)
                .examine("Some old arrowheads.");
        items.add("broken_arrow", "Broken arrow", Kind.SIMPLE, "arrow", 0x8A6A3A).value(1).stack(1).examine("A broken arrow.");
        items.add("broken_glass", "Broken glass", Kind.SIMPLE, "gem", 0xA0C0C0).value(1).stack(1).examine("Smashed glass.");
    }

    public static List<BlockDef> blocks() {
        return List.of(
                BlockDef.scripted(PLAQUE, "Natural history plaque", 0xC0A050, "notice_board"),
                BlockDef.scripted(SPECIMEN_ROCKS, "Specimen rocks", 0x8A7A6A, "rock"),
                BlockDef.scripted(SPECIMEN_TABLE, "Specimen table", 0x8A6A3A, "table"),
                BlockDef.scripted(TOOL_RACK, "Tools", 0x6A5A4A, "crate"),
                BlockDef.scripted(STORAGE_CRATE, "Storage crate", 0x9A7A4A, "crate"),
                BlockDef.scripted(DISPLAY_CASE, "Display case", 0xC0D0D0, "chest"));
    }

    public static List<PlaceDef> places() {
        return List.of(PlaceDef.of(PLACE, "Varrock Museum (cleaning area)", X, Y, PlaceDef.Style.NONE, blocks().stream().map(BlockDef::id).distinct().toList()));
    }

    public static List<NpcDef> npcs() {
        return List.of(
                NpcDef.of(ORLANDO, "Orlando Smith", PLACE, 4, -6, 0xD8B090, 0x8A6A3A, 0x4A3A2A, 0x6A4A2A),
                NpcDef.of(CLERK, "Information clerk", PLACE, 8, -10, 0xD8B090, 0x3A5A8A, 0x2A2A3A, 0x2A2A2A),
                NpcDef.of(ARCHAEOLOGIST, "Archaeologist", PLACE, -4, -2, 0xD8B090, 0xA08A5A, 0x5A4A3A, 0xC0A060));
    }
}
