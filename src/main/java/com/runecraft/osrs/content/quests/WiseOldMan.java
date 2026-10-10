package com.runecraft.osrs.content.quests;

/**
 * The Wise Old Man in Draynor Village sells the quest point cape to adventurers who have completed
 * every quest (the shop's quest id {@code "*"} means "all quests").
 */
final class WiseOldMan {
    /** Shop quest id meaning "every quest in the game is complete". */
    static final String ALL_QUESTS = "*";

    private WiseOldMan() {
    }

    static void register(QuestBook b) {
        b.npc("wise_old_man", "Wise Old Man", "DRAYNOR_VILLAGE", 7, 3, 0xE8D0C0, 0x3A3AA0, 0x2A2A7A, 0xF0F0F0);
        b.shop("wise_old_man", "quest_point_cape", 99000, ALL_QUESTS, null, null, 0,
                "You have completed every quest in Gielinor! You may buy the quest point cape for 99,000 coins.");
    }
}
