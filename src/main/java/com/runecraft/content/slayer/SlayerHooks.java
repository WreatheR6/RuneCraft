package com.runecraft.content.slayer;

import com.runecraft.collection.CollectionLog;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.content.slayer.SlayerChests;
import com.runecraft.osrs.content.slayer.SlayerMasters;
import com.runecraft.osrs.data.Drop;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.platform.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

/** Minecraft-side behaviour for the slayer content module. */
public final class SlayerHooks implements ModuleHooks {
    @Override
    public void init() {
        for (SlayerMasters.Master m : SlayerMasters.all()) {
            QuestScripts.onTalk(m.npcId(), (player, npc) -> SlayerService.talk(player, m));
        }
        // Spria's NPC belongs to "A Porcine of Interest", whose dialogue is registered after this module:
        // once the server starts, she runs the quest until it is complete and gives Slayer tasks after.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> wrapQuestMasters());
        SlayerCombat.register();
        // Broad arrows and bolts can only be fletched after learning "Broader fletching" (300 points).
        com.runecraft.menu.MenuService.REQUIREMENTS.add((player, recipe) ->
                com.runecraft.osrs.content.slayer.SlayerRewards.BROADER_FLETCHING_RECIPES.contains(recipe.id())
                        && !SlayerService.profile(player).unlocks.contains("broader_fletching")
                        ? "You need to learn Broader Fletching from a Slayer master first." : null);
        TormentedDemonCombat.register();
        SlayerItemService.register();
        SlayerPlaces.register();
        SlayerCommands.register();
        com.runecraft.combat.CombatHooks.addDropTransformer(SlayerService::dulyNoted);
        BlockHooks.register("brimstone_chest", (player, pos, state) -> openChest(player, "brimstone_key",
                "brimstone chest", SlayerChests.BRIMSTONE_MAIN, SlayerChests.BRIMSTONE_RARE));
        BlockHooks.register("larrans_big_chest", (player, pos, state) -> openChest(player, "larran_key",
                "Larran's big chest", SlayerChests.LARRAN_MAIN, SlayerChests.LARRAN_RARE));
    }

    private static final java.util.Set<String> WRAPPED = new java.util.HashSet<>();

    private static void wrapQuestMasters() {
        wrapOtto();
        for (SlayerMasters.Master m : SlayerMasters.all()) {
            if (m.quest() == null || !WRAPPED.add(m.npcId())) {
                continue;
            }
            BiConsumer<ServerPlayer, QuestNpcEntity> quest = QuestScripts.handler(m.npcId());
            QuestScripts.onTalk(m.npcId(), (player, npc) -> {
                if (quest != null && ContentRegistry.quest(m.quest()) != null && !QuestApi.isComplete(player, m.quest())) {
                    quest.accept(player, npc);
                } else {
                    SlayerService.talk(player, m);
                }
            });
        }
    }

    /**
     * Otto Godblessed (Otto's Grotto) repairs every broken dragon hasta the player carries for free (OSRS Wiki "Broken
     * dragon hasta"); otherwise he talks as before (Barbarian Training).
     */
    private static void wrapOtto() {
        String otto = com.runecraft.osrs.content.slayer.SlayerRewards.OTTO;
        if (!WRAPPED.add(otto)) {
            return;
        }
        BiConsumer<ServerPlayer, QuestNpcEntity> before = QuestScripts.handler(otto);
        QuestScripts.onTalk(otto, (player, npc) -> {
            String broken = com.runecraft.osrs.content.slayer.SlayerRewards.BROKEN_HASTA;
            int n = Inv.count(player, broken);
            if (n > 0) {
                Inv.remove(player, broken, n);
                Inv.give(player, com.runecraft.osrs.content.slayer.SlayerRewards.DRAGON_HASTA, n);
                QuestApi.say(player, "Otto Godblessed", "A fine weapon, broken by careless hands. There - it is whole again.");
                Msg.game(player, "Otto repairs your broken dragon hasta" + (n > 1 ? "s" : "") + ".");
            } else if (before != null) {
                before.accept(player, npc);
            } else {
                QuestApi.say(player, "Otto Godblessed", "Greetings, outerlander.");
            }
        });
    }

    /**
     * Opens a slayer chest with its key (OSRS Wiki): one pre-roll for a unique, one weighted roll on the main table (its
     * fish line rolls the fish table by Fishing level); main-table loot comes noted.
     */
    private static void openChest(ServerPlayer player, String key, String name, List<Drop> main, List<Drop> rares) {
        if (!Inv.has(player, key, 1)) {
            Msg.game(player, "The " + name + " is locked. You need " + Msg.article(SlayerCommands.name(key)) + ".");
            return;
        }
        Inv.remove(player, key, 1);
        Random random = new Random(player.getRandom().nextLong());
        Drop unique = SlayerChests.preRoll(rares, random);
        if (unique != null) {
            Inv.give(player, unique.item(), 1);
            CollectionLog.obtain(player, unique.item());
            Msg.red(player, "You find something special in the " + name + "!");
        }
        Drop d = SlayerChests.mainRoll(main, random);
        if (d.item().equals(SlayerChests.FISH)) {
            SlayerChests.Fish fish = SlayerChests.fish(com.runecraft.player.Players.data(player).skills()
                    .level(com.runecraft.osrs.Skill.FISHING), key.equals("larran_key"), random);
            com.runecraft.bank.Notes.giveNotedIfMany(player, fish.item(), fish.count());
        } else {
            int count = d.min() + (d.max() > d.min() ? random.nextInt(d.max() - d.min() + 1) : 0);
            com.runecraft.bank.Notes.giveNotedIfMany(player, d.item(), count);
            CollectionLog.obtain(player, d.item());
        }
        Msg.game(player, "You unlock the " + name + ".");
    }
}
