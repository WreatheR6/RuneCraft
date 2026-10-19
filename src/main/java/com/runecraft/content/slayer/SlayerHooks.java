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
        TormentedDemonCombat.register();
        SlayerCommands.register();
        BlockHooks.register("brimstone_chest", (player, pos, state) -> openChest(player, "brimstone_key",
                "brimstone chest", SlayerChests.BRIMSTONE_MAIN, SlayerChests.BRIMSTONE_RARE));
        BlockHooks.register("larrans_big_chest", (player, pos, state) -> openChest(player, "larran_key",
                "Larran's big chest", SlayerChests.LARRAN_MAIN, SlayerChests.LARRAN_RARE));
    }

    private static final java.util.Set<String> WRAPPED = new java.util.HashSet<>();

    private static void wrapQuestMasters() {
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

    private static void openChest(ServerPlayer player, String key, String name, List<Drop> main, List<Drop> rares) {
        if (!Inv.has(player, key, 1)) {
            Msg.game(player, "The " + name + " is locked. You need " + Msg.article(SlayerCommands.name(key)) + ".");
            return;
        }
        Inv.remove(player, key, 1);
        Random random = new Random(player.getRandom().nextLong());
        int total = 0;
        for (Drop d : main) {
            total += d.rarity();
        }
        int roll = random.nextInt(Math.max(1, total));
        for (Drop d : main) {
            roll -= d.rarity();
            if (roll < 0) {
                give(player, d, random);
                break;
            }
        }
        for (Drop d : rares) {
            if (random.nextInt(d.rarity()) == 0) {
                give(player, d, random);
                Msg.red(player, "You find something special in the " + name + "!");
            }
        }
        Msg.game(player, "You unlock the " + name + ".");
    }

    private static void give(ServerPlayer player, Drop d, Random random) {
        int count = d.min() + (d.max() > d.min() ? random.nextInt(d.max() - d.min() + 1) : 0);
        Inv.give(player, d.item(), count);
        CollectionLog.obtain(player, d.item());
    }
}
