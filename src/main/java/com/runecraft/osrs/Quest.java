package com.runecraft.osrs;

import java.util.List;
import java.util.Map;

/**
 * Quests: quest points, requirements, rewards and the journal text for each stage. The
 * Minecraft layer ({@code com.runecraft.quest}) scripts the dialogue and stage changes.
 * Stage 0 = not started; stage {@link #finalStage()} = complete.
 */
public enum Quest implements QuestInfo {
    COOKS_ASSISTANT("Cook's Assistant", Difficulty.NOVICE, 1, Map.of(), Map.of(Skill.COOKING, 300.0), List.of(),
            List.of("I can start this quest by speaking to the Cook in the kitchen of Lumbridge Castle.",
                    "The Cook needs a bucket of milk, an egg and a pot of flour for the Duke's birthday cake.")),
    SHEEP_SHEARER("Sheep Shearer", Difficulty.NOVICE, 1, Map.of(), Map.of(Skill.CRAFTING, 150.0), List.of("coins:60"),
            List.of("I can start this quest by speaking to Fred the Farmer north of Lumbridge.",
                    "Fred wants 20 balls of wool. Shear sheep and spin the wool on a spinning wheel.")),
    DORICS_QUEST("Doric's Quest", Difficulty.NOVICE, 1, Map.of(), Map.of(Skill.MINING, 1300.0), List.of("coins:180"),
            List.of("I can start this quest by speaking to Doric near Falador.",
                    "Doric wants 6 clay, 4 copper ore and 2 iron ore.")),
    RUNE_MYSTERIES("Rune Mysteries", Difficulty.NOVICE, 1, Map.of(), Map.of(), List.of("air_talisman"),
            List.of("I can start this quest by speaking to Duke Horacio in Lumbridge Castle.",
                    "I must take the strange talisman to Sedridor at the Wizards' Tower.",
                    "Sedridor wants me to deliver a research package to Aubury in Varrock.",
                    "I should return Aubury's research notes to Sedridor.")),
    RESTLESS_GHOST("The Restless Ghost", Difficulty.NOVICE, 1, Map.of(), Map.of(Skill.PRAYER, 1125.0), List.of(),
            List.of("I can start this quest by speaking to Father Aereck in Lumbridge.",
                    "Father Aereck told me to visit Father Urhney in the Lumbridge Swamp.",
                    "Wearing the Amulet of ghostspeak, I should talk to the ghost in Lumbridge's graveyard.",
                    "The ghost has lost his skull. Skeletons in Gielinor might have it.",
                    "I have the skull. I should return it to the ghost.")),
    IMP_CATCHER("Imp Catcher", Difficulty.NOVICE, 1, Map.of(), Map.of(Skill.MAGIC, 875.0), List.of("amulet_of_accuracy"),
            List.of("I can start this quest by speaking to Wizard Mizgog in the Wizards' Tower.",
                    "Mizgog needs his black, red, yellow and white beads back. The imps that stole them still have them.")),
    WITCHS_POTION("Witch's Potion", Difficulty.NOVICE, 1, Map.of(), Map.of(Skill.MAGIC, 325.0), List.of(),
            List.of("I can start this quest by speaking to Hetty in Rimmington.",
                    "Hetty needs a rat's tail, an eye of newt, some burnt meat and an onion.",
                    "I should drink from Hetty's cauldron.")),
    ROMEO_AND_JULIET("Romeo & Juliet", Difficulty.NOVICE, 5, Map.of(), Map.of(), List.of(),
            List.of("I can start this quest by speaking to Romeo in Varrock.",
                    "Romeo wants me to find Juliet.",
                    "Juliet gave me a message for Romeo.",
                    "Romeo told me to speak to Father Lawrence.",
                    "Father Lawrence wants the Apothecary to brew a Cadava potion. It needs cadava berries.",
                    "I should take the Cadava potion to Juliet.",
                    "I should tell Romeo the plan.")),
    DRUIDIC_RITUAL("Druidic Ritual", Difficulty.NOVICE, 4, Map.of(), Map.of(Skill.HERBLORE, 250.0), List.of(),
            List.of("I can start this quest by speaking to Kaqemeex in Taverley. (Unlocks the Herblore skill.)",
                    "Kaqemeex told me to speak to Sanfew.",
                    "Sanfew needs raw beef, raw chicken, raw porkchop and raw mutton for the ritual.",
                    "I should tell Kaqemeex the ritual is complete.")),
    VAMPYRE_SLAYER("Vampyre Slayer", Difficulty.NOVICE, 3, Map.of(), Map.of(Skill.ATTACK, 4825.0), List.of(),
            List.of("I can start this quest by speaking to Morgan in Draynor Village.",
                    "Morgan's friend Dr Harlow in Varrock knows how to slay vampyres.",
                    "Dr Harlow wants a beer before he'll help.",
                    "I have a stake. With a hammer, I can slay Count Draynor in Draynor Manor.")),
    DEMON_SLAYER("Demon Slayer", Difficulty.NOVICE, 3, Map.of(), Map.of(), List.of("silverlight"),
            List.of("I can start this quest by speaking to Gypsy Aris in Varrock Square.",
                    "Aris says I need Silverlight to defeat Delrith. Sir Prysin has it.",
                    "Sir Prysin's keys are lost. Wizard Traiborn will help in exchange for 25 bones.",
                    "I should take Traiborn's key back to Sir Prysin.",
                    "With Silverlight, I must defeat Delrith at the stone circle south of Varrock.")),
    BARROWS_INTRO("The Barrows (miniquest)", Difficulty.EXPERIENCED, 0, Map.of(Skill.PRAYER, 43), Map.of(), List.of(),
            List.of("Six brothers lie restless in their crypts in Morytania.",
                    "Defeat all six Barrows brothers.")),
    FIGHT_CAVES("The Fight Caves", Difficulty.GRANDMASTER, 0, Map.of(Skill.PRAYER, 43, Skill.RANGED, 70), Map.of(), List.of(),
            List.of("TzTok-Jad awaits challengers at Mor Ul Rek, Karamja.",
                    "Defeat TzTok-Jad to earn a fire cape."));

    public enum Difficulty { NOVICE, INTERMEDIATE, EXPERIENCED, MASTER, GRANDMASTER }

    private final String displayName;
    private final Difficulty difficulty;
    private final int questPoints;
    private final Map<Skill, Integer> requirements;
    private final Map<Skill, Double> xpRewards;
    private final List<String> itemRewards;
    private final List<String> journal;

    Quest(String displayName, Difficulty difficulty, int questPoints, Map<Skill, Integer> requirements,
          Map<Skill, Double> xpRewards, List<String> itemRewards, List<String> journal) {
        this.displayName = displayName;
        this.difficulty = difficulty;
        this.questPoints = questPoints;
        this.requirements = requirements;
        this.xpRewards = xpRewards;
        this.itemRewards = itemRewards;
        this.journal = journal;
    }

    @Override
    public String questId() {
        return name();
    }

    @Override
    public String displayName() {
        return displayName;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public int questPoints() {
        return questPoints;
    }

    public Map<Skill, Integer> requirements() {
        return requirements;
    }

    public Map<Skill, Double> xpRewards() {
        return xpRewards;
    }

    /** Item rewards as "item" or "item:count". */
    public List<String> itemRewards() {
        return itemRewards;
    }

    public List<String> journal() {
        return journal;
    }

    /** Stage value meaning the quest is complete. */
    public int finalStage() {
        return journal.size();
    }

    public String journalFor(int stage) {
        if (stage >= finalStage()) {
            return "QUEST COMPLETE!";
        }
        return journal.get(Math.max(0, stage));
    }

    /** Total quest points across built-in and module quests. */
    public static int totalQuestPoints() {
        int total = 0;
        for (QuestInfo q : com.runecraft.osrs.content.ContentRegistry.quests()) {
            total += q.questPoints();
        }
        return total;
    }
}
