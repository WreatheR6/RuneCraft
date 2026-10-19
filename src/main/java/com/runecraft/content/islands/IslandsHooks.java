package com.runecraft.content.islands;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.islands.IslandsBlocks;
import com.runecraft.osrs.content.islands.IslandsNpcs;
import com.runecraft.osrs.content.islands.IslandsTravel;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Minecraft-side behaviour for the islands module: NPC dialogue, the fairy rings and Magic
 * Mushtrees, the Ape Atoll Agility Course, managing the Kingdom of Miscellania and the creature
 * camps (including the caves carved under the Wyvern Cave, the Mos Le'Harmless Caves, the Lithkren
 * Vault and the Corsair Cove Dungeon).
 */
public final class IslandsHooks implements ModuleHooks {
    /** Which dialogue line each player hears next, per NPC. */
    private static final Map<String, Integer> LINE = new HashMap<>();

    @Override
    public void init() {
        for (NpcDef npc : IslandsNpcs.all()) {
            QuestScripts.onTalk(npc.id(), (player, entity) -> talk(player, npc.id(), npc.name()));
        }
        for (IslandsTravel.Network network : IslandsTravel.networks()) {
            BlockHooks.register(network.blockId(), (player, pos, state) -> IslandsTravelService.use(player, pos, network));
        }
        IslandsTravelService.registerRingBuilders();
        BlockHooks.register(IslandsBlocks.APE_OBSTACLE, (player, pos, state) -> IslandsAgilityService.use(player, pos));
        BlockHooks.register(IslandsBlocks.KINGDOM_LEDGER, (player, pos, state) -> IslandsKingdomService.ledger(player));
        BlockHooks.register(IslandsBlocks.KINGDOM_WORKFORCE, (player, pos, state) -> IslandsKingdomService.workforce(player));
        IslandsKingdomService.register();
        IslandsCamps.register();
    }

    /** Says the NPC's next dialogue line. */
    static void talk(ServerPlayer player, String npcId, String name) {
        List<String> lines = IslandsNpcs.dialogue(npcId);
        if (lines.isEmpty()) {
            QuestApi.say(player, name, "Hello there, adventurer.");
            return;
        }
        String k = player.getUUID() + "|" + npcId;
        int i = LINE.getOrDefault(k, 0);
        QuestApi.say(player, name, lines.get(i % lines.size()));
        LINE.put(k, i + 1);
    }
}
