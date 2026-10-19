package com.runecraft.content.zeah;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.osrs.content.zeah.ZeahBlocks;
import com.runecraft.osrs.content.zeah.ZeahHunter;
import com.runecraft.osrs.content.zeah.ZeahItems;
import com.runecraft.osrs.content.zeah.ZeahNpcs;
import com.runecraft.osrs.content.zeah.ZeahTravel;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

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
            } else if (npc.id().equals("zeah_shayzien_quartermaster")) {
                QuestScripts.onTalk(npc.id(), (player, entity) -> quartermaster(player, npc.name()));
            } else if (npc.id().equals("zeah_guildmaster_jane")) {
                QuestScripts.onTalk(npc.id(), (player, entity) -> com.runecraft.content.skilling.FarmingContractService.talk(player, npc.name()));
            } else if (npc.id().startsWith("zeah_guild_hunter_")) {
                QuestScripts.onTalk(npc.id(), (player, entity) -> rumour(player, npc.id(), npc.name()));
            } else {
                QuestScripts.onTalk(npc.id(), (player, entity) -> talk(player, npc.id(), npc.name()));
            }
        }
        BlockHooks.register(ZeahBlocks.MINECART, (player, pos, state) -> ZeahTravelService.minecart(player, pos));
        ZeahTravelService.registerMinecartStations();
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
        BlockHooks.register(ZeahBlocks.CATACOMBS_ALTAR, (player, pos, state) -> catacombsAltar(player));
        ZeahCamps.register();
        GameEvents.onObtain(ZeahHooks::rumourCatch);
    }

    /** The Catacombs altar: Darklight and 3 ancient shards become Arclight (with its starting 1,000 charges). */
    private static void catacombsAltar(ServerPlayer player) {
        if (!Inv.has(player, "darklight", 1)) {
            Msg.game(player, "The altar hums with the power of the ancient shards. Perhaps a demonic sword could channel it.");
            return;
        }
        if (!Inv.has(player, "ancient_shard", ZeahBlocks.ARCLIGHT_SHARDS)) {
            Msg.game(player, "You need " + ZeahBlocks.ARCLIGHT_SHARDS + " ancient shards to infuse Darklight.");
            return;
        }
        Inv.remove(player, "darklight", 1);
        Inv.remove(player, "ancient_shard", ZeahBlocks.ARCLIGHT_SHARDS);
        Inv.give(player, "arclight", 1);
        CollectionLog.obtain(player, "arclight");
        Msg.action(player, "The ancient shards' power flows into Darklight, forging Arclight!");
    }

    /** Sneak-talk to buy the next missing piece of Shayzien armour (5). */
    private static void quartermaster(ServerPlayer player, String name) {
        if (!player.isShiftKeyDown()) {
            talk(player, "zeah_shayzien_quartermaster", name);
            Msg.game(player, "(Sneak while talking to the quartermaster to buy Shayzien armour (5).)");
            return;
        }
        for (ZeahItems.QuartermasterOffer offer : ZeahItems.QUARTERMASTER) {
            String piece = offer.item();
            if (Inv.has(player, piece, 1) || wearing(player, piece)) {
                continue;
            }
            int price = offer.price();
            if (!Inv.has(player, Inv.COINS, price)) {
                QuestApi.say(player, name, "That piece costs " + price + " coins.");
                return;
            }
            Inv.remove(player, Inv.COINS, price);
            Inv.give(player, piece, 1);
            CollectionLog.obtain(player, piece);
            QuestApi.say(player, name, "Here you are, soldier. Wear it with honour.");
            return;
        }
        QuestApi.say(player, name, "You already have a full set of Shayzien armour (5).");
    }

    private static boolean wearing(ServerPlayer player, String id) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (Inv.idOf(player.getItemBySlot(slot)).equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Hunters' Rumours: a guild hunter assigns a creature; catch it a few times, then report back for a loot sack. */
    private static void rumour(ServerPlayer player, String npcId, String name) {
        PlayerData data = Players.data(player);
        int level = data.skills().currentLevel(Skill.HUNTER);
        if (level < ZeahHunter.GUILD_LEVEL) {
            QuestApi.say(player, name, "Come back when you have level " + ZeahHunter.GUILD_LEVEL + " Hunter.");
            return;
        }
        int target = data.killCount(ZeahHunter.RUMOUR_TARGET);
        if (target <= 0 || target > ZeahHunter.SPOTS.size()) {
            if (!player.isShiftKeyDown()) {
                talk(player, npcId, name);
                Msg.game(player, "(Sneak while talking to a guild hunter to take a rumour.)");
                return;
            }
            ZeahHunter.Spot spot = ZeahHunter.rumourTarget(level, new java.util.Random(player.getRandom().nextLong()));
            if (spot == null) {
                QuestApi.say(player, name, "I've nothing for you yet.");
                return;
            }
            data.killCounts().put(ZeahHunter.RUMOUR_TARGET, ZeahHunter.SPOTS.indexOf(spot) + 1);
            data.killCounts().put(ZeahHunter.RUMOUR_CAUGHT, 0);
            QuestApi.say(player, name, "I've heard rumours of " + spot.creature().toLowerCase(java.util.Locale.ROOT)
                    + "s. Catch some and bring me proof.");
            return;
        }
        ZeahHunter.Spot spot = ZeahHunter.SPOTS.get(target - 1);
        int caught = data.killCount(ZeahHunter.RUMOUR_CAUGHT);
        if (caught < ZeahHunter.RUMOUR_CATCHES) {
            QuestApi.say(player, name, "Keep hunting " + spot.creature().toLowerCase(java.util.Locale.ROOT) + "s ("
                    + caught + "/" + ZeahHunter.RUMOUR_CATCHES + ").");
            return;
        }
        data.killCounts().put(ZeahHunter.RUMOUR_TARGET, 0);
        data.killCounts().put(ZeahHunter.RUMOUR_CAUGHT, 0);
        int done = data.incrementKillCount(ZeahHunter.RUMOURS_DONE);
        QuestApi.say(player, name, "Excellent work! Here's your reward.");
        Inv.give(player, "hunters_loot_sack", 1);
        if (player.getRandom().nextInt(ZeahHunter.OUTFIT_ONE_IN) == 0) {
            List<String> outfit = ZeahHunter.outfitPieces();
            String piece = outfit.get(player.getRandom().nextInt(outfit.size()));
            Inv.give(player, piece, 1);
            CollectionLog.obtain(player, piece);
            Msg.action(player, "The loot sack holds a piece of the guild hunter outfit!");
        }
        String whistle = ZeahHunter.whistleAt(done);
        if (whistle != null) {
            Inv.give(player, whistle, 1);
            CollectionLog.obtain(player, whistle);
            Msg.action(player, "Soar Leader Pitri sends you a quetzal whistle for your " + done + " rumours.");
        }
        Msg.game(player, "You have completed " + done + " Hunters' Rumour" + (done == 1 ? "" : "s") + ".");
    }

    /** Counts a catch of the player's rumour creature. */
    private static void rumourCatch(ServerPlayer player, String item, int count) {
        PlayerData data = Players.data(player);
        int target = data.killCount(ZeahHunter.RUMOUR_TARGET);
        if (target <= 0 || target > ZeahHunter.SPOTS.size()) {
            return;
        }
        ZeahHunter.Spot spot = ZeahHunter.SPOTS.get(target - 1);
        if (!spot.loot().get(spot.loot().size() > 1 ? 1 : 0).equals(item)) {
            return;
        }
        int caught = data.killCount(ZeahHunter.RUMOUR_CAUGHT) + 1;
        data.killCounts().put(ZeahHunter.RUMOUR_CAUGHT, caught);
        if (caught == ZeahHunter.RUMOUR_CATCHES) {
            Msg.action(player, "You find proof of the rumour. Report back to a guild hunter.");
        }
    }

    private static String key(ServerPlayer player, String npc) {
        UUID id = player.getUUID();
        return id + "|" + npc;
    }

    /** Says the NPC's next dialogue line. */
    static void talk(ServerPlayer player, String npcId, String name) {
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
    private static void thirus(ServerPlayer player, String name) {
        if (!player.isShiftKeyDown()) {
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

    static boolean wearsFullShayzien(ServerPlayer player) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            if (!Inv.idOf(player.getItemBySlot(slots[i])).equals(ZeahItems.SHAYZIEN_ARMOUR.get(i))) {
                return false;
            }
        }
        return true;
    }
}
