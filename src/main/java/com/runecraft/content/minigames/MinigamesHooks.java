package com.runecraft.content.minigames;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.boss.BossScripts;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.content.ModuleHooks;
import com.runecraft.content.minigames.games.PenanceQueenScript;
import com.runecraft.content.minigames.games.PyramidPlunderSession;
import com.runecraft.content.minigames.games.RoguesDenSession;
import com.runecraft.content.minigames.games.TemporossSession;
import com.runecraft.content.minigames.games.ZukScript;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameWorld;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

/**
 * Minecraft-side behaviour for the minigames module: entrances, lobbies and instanced arenas
 * (see {@link MinigameManager}), reward shops, combat/XP effects of rewards, reward containers,
 * boss scripts for TzKal-Zuk and the Penance Queen, NPC dialogue and the /minigame command.
 */
public final class MinigamesHooks implements ModuleHooks {
    @Override
    public void init() {
        MinigameNet.register();
        MinigameCombat.register();
        MinigameItemUse.register();
        BossScripts.register("tzkal_zuk", ZukScript::new);
        BossScripts.register("penance_queen", PenanceQueenScript::new);

        // Entrances.
        for (Minigame m : Minigame.values()) {
            BlockHooks.register(m.entranceBlock(), (player, pos, state) -> MinigameManager.enter(player, m, m.entranceBlock()));
        }
        // Style-NONE entrances (Volcanic Mine beside the islands module's lobby, Nightmare Zone by Yanille, the Chasm of
        // Tears beside Juna) build nothing but their portals and reward chests, in a row.
        java.util.Map<String, java.util.List<String>> placeBlocks = MinigameWorld.placeBlocks();
        for (Minigame m : Minigame.values()) {
            if (m.style() != com.runecraft.osrs.content.PlaceDef.Style.NONE) {
                continue;
            }
            java.util.List<String> blocks = placeBlocks.get(m.place());
            com.runecraft.world.LandmarkBuilder.registerCustom(m.place(), (world, centre) -> {
                for (int i = 0; i < blocks.size(); i++) {
                    world.setBlockState(centre.up().east(i * 2), Arena.module(blocks.get(i)));
                }
            });
        }
        for (Map.Entry<String, Minigame> e : MinigameWorld.EXTRA_ENTRANCES.entrySet()) {
            BlockHooks.register(e.getKey(), (player, pos, state) -> MinigameManager.enter(player, e.getValue(), e.getKey()));
        }
        // Reward shops.
        for (Map.Entry<String, String> e : MinigameWorld.SHOP_BLOCKS.entrySet()) {
            BlockHooks.register(e.getKey(), (player, pos, state) -> RewardShopService.open(player, e.getValue()));
        }
        for (Map.Entry<String, String> e : MinigameWorld.NPC_SHOPS.entrySet()) {
            QuestScripts.onTalk(e.getKey(), (player, npc) -> {
                QuestApi.say(player, npc.getName().getString(), "Have a look at my rewards.");
                RewardShopService.open(player, e.getValue());
            });
        }
        BlockHooks.register("pyramid_plunder_artefact_trader", (player, pos, state) -> PyramidPlunderSession.sellArtefacts(player));
        BlockHooks.register("rogues_den_jewel_trader", (player, pos, state) -> RoguesDenSession.tradeJewel(player));
        BlockHooks.register("tempoross_reward_pool", (player, pos, state) -> TemporossSession.rewardPool(player));
        QuestScripts.onTalk("mg_simon_templeton", (player, npc) -> PyramidPlunderSession.sellArtefacts(player));
        QuestScripts.onTalk("mg_brian_orichard", (player, npc) -> RoguesDenSession.tradeJewel(player));
        talk("mg_tzhaar_mej_jal", "TzHaar-Mej-Jal", "You want to challenge the Fight Cave, JalYt? Sixty-three waves of TzHaar await. "
                + "Survive, and TzTok-Jad himself will face you. The fire cape is the prize.");
        talk("mg_tzhaar_ket_keh", "TzHaar-Ket-Keh", "The Inferno is no place for JalYt. Sacrifice a fire cape and face TzKal-Zuk... "
                + "if you dare. The infernal cape will be yours if you survive sixty-nine waves.");
        talk("mg_ignisia", "Ignisia", "The Wintertodt stirs. Chop bruma roots, fletch them into kindling and feed the braziers. "
                + "Fix broken braziers with a hammer and keep the pyromancers alive. Earn 500 points for a supply crate.");
        talk("mg_first_mate_deri", "First Mate Deri", "Tempoross brews a storm! Harpoon the harpoonfish, load the cannons, "
                + "tether yourself when a wave comes, and strike the spirit pool when it falls. Permits buy from the reward pool.");
        talk("mg_harrallak_menarous", "Harrallak Menarous", "Welcome to the Warriors' Guild. Animate armour on the magical animators "
                + "to earn tokens, then spend them in the cyclops room for defenders.");
        talk("mg_guardian_mummy", "Guardian mummy", "You may plunder the pyramid for five minutes. Urns, chests and the sarcophagus "
                + "hold treasure... and the Pharaoh's sceptre itself, for the lucky.");
        talk("mg_murphy", "Murphy", "The Fishing Trawler sails soon. Patch leaks, bail water and keep her afloat for the net to fill.");
        talk("mg_mortton_priest", "Ulsquire Shauncy", "The shades plague the catacombs. Burn their remains on a funeral pyre to lay "
                + "them to rest, and their keys may open the old chests.");

        // Arena objects.
        for (String[] b : MinigameWorld.ARENA_BLOCKS) {
            if (b[0].equals("tempoross_reward_pool")) {
                continue;
            }
            BlockHooks.register(b[0], (player, pos, state) -> MinigameManager.onBlock(player, b[0], pos));
        }

        GameEvents.onKill((player, entity, stats) -> MinigameManager.onKill(player, entity));
        // Champions' Challenge: Larxus, the champions' rules and the 1/5,000 scrolls for kin whose tables lack one.
        QuestScripts.onTalk(com.runecraft.osrs.content.minigames.ChampionsChallenge.LARXUS,
                (player, npc) -> com.runecraft.content.minigames.games.ChampionsSession.larxus(player));
        com.runecraft.combat.CombatHooks.addAttackGate(com.runecraft.content.minigames.games.ChampionsSession::deniedAttack);
        GameEvents.onKill((player, entity, stats) -> {
            if (stats == null) {
                return;
            }
            String scroll = com.runecraft.osrs.content.minigames.ChampionsChallenge.scrollFor(stats.name());
            java.util.List<com.runecraft.osrs.data.Drop> table = stats.monster() != null ? stats.monster().drops()
                    : stats.profile() instanceof com.runecraft.osrs.content.MonsterDef md ? md.drops
                    : stats.profile() instanceof com.runecraft.osrs.content.BossDef bd ? bd.drops : null;
            if (scroll != null && !com.runecraft.osrs.content.minigames.ChampionsChallenge.tableHas(table, scroll)
                    && entity.getRandom().nextInt(com.runecraft.osrs.content.minigames.ChampionsChallenge.SCROLL_RATE) == 0) {
                entity.dropStack(com.runecraft.util.Inv.stack(scroll, 1));
                com.runecraft.collection.CollectionLog.obtain(player, scroll);
            }
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !(entity instanceof ServerPlayerEntity player) || MinigameManager.allowDeath(player));
        ServerTickEvents.END_SERVER_TICK.register(MinigameManager::serverTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            MinigameManager.onDisconnect(handler.getPlayer());
            MinigameNet.forget(handler.getPlayer());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> MinigameManager.onJoin(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("minigame")
                        .then(CommandManager.literal("leave").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p != null) {
                                MinigameManager.leave(p);
                            }
                            return 1;
                        }))
                        .then(CommandManager.literal("points").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p != null) {
                                showPoints(p);
                            }
                            return 1;
                        }))
                        .then(CommandManager.literal("shop").then(CommandManager.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p != null && ctx.getSource().hasPermissionLevel(2)) {
                                        RewardShopService.open(p, StringArgumentType.getString(ctx, "id"));
                                    }
                                    return 1;
                                })))));
    }

    private static void talk(String npc, String name, String line) {
        QuestScripts.onTalk(npc, (player, entity) -> QuestApi.say(player, name, line));
    }

    private static void showPoints(ServerPlayerEntity p) {
        MinigameData data = MinigameData.of(p);
        Msg.game(p, "--- Minigame points ---");
        for (Map.Entry<String, String> e : MinigameCurrencies.all().entrySet()) {
            if (e.getKey().startsWith("pts:")) {
                int v = data.points(p.getUuid(), MinigameCurrencies.key(e.getKey()));
                if (v != 0) {
                    Msg.game(p, e.getValue() + ": " + String.format("%,d", v));
                }
            }
        }
        for (Minigame m : Minigame.values()) {
            int kc = data.stat(p.getUuid(), "kc:" + m.id());
            if (kc > 0) {
                Msg.game(p, m.displayName() + " completions: " + kc);
            }
        }
    }
}
