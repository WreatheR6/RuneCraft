package com.runecraft.content.zeah;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.zeah.ZeahBlocks;
import com.runecraft.osrs.content.zeah.ZeahHunter;
import com.runecraft.osrs.content.zeah.ZeahItems;
import com.runecraft.osrs.content.zeah.ZeahNpcs;
import com.runecraft.osrs.content.zeah.ZeahTravel;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft-side behaviour for the zeah module: NPC dialogue, Veos' ship / Primio / the quetzals /
 * the Lovakengj minecarts, Thirus' dynamite, lovakite and Blast Mine mining, the Varlamore Hunter
 * spots, Shayzien armour's protection from lizardman shaman poison and the creature camps
 * (including the Catacombs of Kourend beneath the castle).
 */
public final class ZeahHooks implements ModuleHooks {
    /** Price of one stick of dynamite from Thirus (approx.). */
    static final int DYNAMITE_PRICE = 40;
    /** Which dialogue line each player hears next, per NPC. */
    private static final Map<String, Integer> LINE = new HashMap<>();

    @Override
    public void init() {
        for (NpcDef npc : ZeahNpcs.all()) {
            ZeahTravel.Agent agent = ZeahTravel.agent(npc.id());
            if (agent != null) {
                QuestScripts.onTalk(npc.id(), (player, entity) -> ZeahTravelService.talk(player, npc.name(), agent));
            } else if (npc.id().equals(ZeahNpcs.THIRUS)) {
                QuestScripts.onTalk(npc.id(), (player, entity) -> thirus(player, npc.name()));
            } else {
                QuestScripts.onTalk(npc.id(), (player, entity) -> talk(player, npc.id(), npc.name()));
            }
        }
        BlockHooks.register(ZeahBlocks.MINECART, (player, pos, state) -> ZeahTravelService.minecart(player, pos));
        BlockHooks.register(ZeahBlocks.LOVAKITE_ROCK, (player, pos, state) -> ZeahSkilling.mineLovakite(player, pos));
        BlockHooks.register(ZeahBlocks.HARD_ROCK, (player, pos, state) -> ZeahSkilling.blastMine(player, pos));
        for (ZeahHunter.Spot spot : ZeahHunter.SPOTS) {
            BlockHooks.register(spot.blockId(), (player, pos, state) -> ZeahSkilling.hunt(player, pos, spot));
        }
        CombatHooks.addHurtListener((player, attacker, damage) -> {
            if (attacker instanceof OsrsMonsterEntity monster && monster.stats() != null && monster.stats().profile() != null
                    && monster.stats().profile().id.equals("lizardman_shaman") && wearsFullShayzien(player)) {
                PoisonService.cure(player, false, 5, ActionManager.gameTick());
            }
        });
        ZeahCamps.register();
    }

    private static String key(ServerPlayerEntity player, String npc) {
        UUID id = player.getUuid();
        return id + "|" + npc;
    }

    /** Says the NPC's next dialogue line. */
    static void talk(ServerPlayerEntity player, String npcId, String name) {
        List<String> lines = ZeahNpcs.dialogue(npcId);
        if (lines.isEmpty()) {
            QuestApi.say(player, name, "Hello there, adventurer.");
            return;
        }
        String k = key(player, npcId);
        int i = LINE.getOrDefault(k, 0);
        QuestApi.say(player, name, lines.get(i % lines.size()));
        LINE.put(k, i + 1);
    }

    /** Thirus explains the Blast Mine; sneak-talk to buy a stick of dynamite. */
    private static void thirus(ServerPlayerEntity player, String name) {
        if (!player.isSneaking()) {
            talk(player, ZeahNpcs.THIRUS, name);
            Msg.game(player, "(Sneak while talking to Thirus to buy dynamite for " + DYNAMITE_PRICE + " coins.)");
            return;
        }
        if (!Inv.has(player, Inv.COINS, DYNAMITE_PRICE)) {
            QuestApi.say(player, name, "Dynamite costs " + DYNAMITE_PRICE + " coins a stick. Come back when you can pay.");
            return;
        }
        Inv.remove(player, Inv.COINS, DYNAMITE_PRICE);
        Inv.give(player, "dynamite", 1);
        QuestApi.say(player, name, "Here you go. Don't light it in your hand.");
    }

    static boolean wearsFullShayzien(ServerPlayerEntity player) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            if (!Inv.idOf(player.getEquippedStack(slots[i])).equals(ZeahItems.SHAYZIEN_ARMOUR.get(i))) {
                return false;
            }
        }
        return true;
    }
}
