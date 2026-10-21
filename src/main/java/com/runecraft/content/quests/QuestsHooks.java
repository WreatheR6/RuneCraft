package com.runecraft.content.quests;

import com.runecraft.content.ModuleHooks;

/**
 * Minecraft-side behaviour for the quests content module: the quest-script interpreter (dialogue,
 * objectives, quest bosses, shops), virtual gear slots and quest-locked weapons, quest boss scripts
 * and the /questgear, /questguide and /questcape commands.
 */
public final class QuestsHooks implements ModuleHooks {
    @Override
    public void init() {
        QuestInterpreter.init();
        QuestLampService.init();
        QuestGear.init();
        QuestBossScripts.init();
        MageArenaService.init();
        QuestCommands.init();
    }
}
