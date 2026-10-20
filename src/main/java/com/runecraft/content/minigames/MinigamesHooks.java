package com.runecraft.content.minigames;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.boss.BossScripts;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.content.ModuleHooks;
import com.runecraft.content.minigames.games.GuardiansSession;
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
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.platform.ServerLivingEntityEvents;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;

/**
 * Minecraft-side behaviour for the minigames module: entrances, lobbies and instanced arenas
 * (see {@link MinigameManager}), reward shops, combat/XP effects of rewards, reward containers,
 * boss scripts for TzKal-Zuk and the Penance Queen, NPC dialogue and the /minigame command.
 */
public final class MinigamesHooks implements ModuleHooks {
    /** Rolls for the Shade Catacombs chests opened outside the minigame's instance. */
    private static final java.util.Random SHADE_CHEST_RANDOM = new java.util.Random();

    @Override
    public void init() {
        MinigameNet.register();
        MinigameCombat.register();
        MinigameItemUse.register();
        SorceressGardenService.register();
        PitChampionService.register();
        LmsService.register();
        EmirsArenaService.register();
        TaiBwoWannaiService.register();
        MuseumService.register();
        GauntletService.register();
        BlockHooks.register(MinigameWorld.BA_BLACKBOARD, (player, pos, state) ->
                com.runecraft.content.minigames.games.BarbarianAssaultSession.readBlackboard(player));
        BossScripts.register("tzkal_zuk", ZukScript::new);
        com.runecraft.entity.BossEntity.registerScript(com.runecraft.osrs.data.Boss.TZTOK_JAD,
                com.runecraft.content.minigames.games.TzTokJadScript::new);
        com.runecraft.content.minigames.games.InfernoSession.register();
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
                    world.setBlockAndUpdate(centre.above().east(i * 2), Arena.module(blocks.get(i)));
                }
            });
        }
        // The Burgh de Rott Ramble's portal outside Paterdomus (a style-NONE place of its own).
        com.runecraft.world.LandmarkBuilder.registerCustom(com.runecraft.osrs.content.minigames.TempleTrekking.RAMBLE_PLACE,
                (world, centre) -> world.setBlockAndUpdate(centre.above(), Arena.module(com.runecraft.osrs.content.minigames.TempleTrekking.RAMBLE_ENTRANCE)));
        BlockHooks.register(com.runecraft.osrs.content.minigames.TempleTrekking.RAMBLE_ENTRANCE, (player, pos, state) -> Msg.game(player,
                "Talk to one of the mercenaries here to choose who to escort to Burgh de Rott and which route to take."));
        for (com.runecraft.osrs.content.minigames.TempleTrekking.Follower f : com.runecraft.osrs.content.minigames.TempleTrekking.Follower.values()) {
            QuestScripts.onTalk(f.npc(), (player, npc) -> com.runecraft.content.minigames.games.TempleTrekkingSession.talk(player, f));
        }
        // TzHaar-Ket-Rak and his scoreboard.
        QuestScripts.onTalk("mg_tzhaar_ket_rak", (player, npc) -> ketRak(player));
        BlockHooks.register(MinigameWorld.KET_RAK_SCOREBOARD, (player, pos, state) -> ketRakScoreboard(player));
        // The Fortis Colosseum: its combat hooks, scoreboard, Minimus (the zeah module's NPC) and Sol Heredit's stone, which
        // only points the way (in OSRS Sol Heredit is fought only as the Colosseum's twelfth wave).
        com.runecraft.content.minigames.games.ColosseumSession.register();
        BlockHooks.register(com.runecraft.osrs.content.minigames.FortisColosseum.SCOREBOARD, (player, pos, state) ->
                com.runecraft.content.minigames.games.ColosseumSession.scoreboard(player));
        QuestScripts.onTalk(com.runecraft.osrs.content.minigames.FortisColosseum.MINIMUS, (player, npc) ->
                com.runecraft.content.minigames.games.ColosseumSession.minimus(player));
        // Glory unlocks: the lobby bank chest (Brawler) and the ring of dueling teleport (Hero).
        BlockHooks.register(com.runecraft.osrs.content.minigames.FortisColosseum.BANK_CHEST, (player, pos, state) ->
                com.runecraft.content.minigames.games.ColosseumSession.bankChest(player));
        com.runecraft.content.systems.SysTeleport.addGate((player, dest) ->
                dest.equals(com.runecraft.osrs.content.systems.Teleports.DUELING.get(3))
                        ? com.runecraft.content.minigames.games.ColosseumSession.duelingGate(player) : null);
        // The Gauntlet's lobby reward chest; the Hunllefs' old summoning stones send the player into the Gauntlet itself (in
        // OSRS the Hunllef is only fought at the end of a Gauntlet run).
        BlockHooks.register(com.runecraft.osrs.content.minigames.Gauntlet.REWARD_CHEST, (player, pos, state) ->
                com.runecraft.content.minigames.games.GauntletSession.rewardChest(player));
        for (com.runecraft.osrs.content.minigames.Gauntlet.Mode mode : com.runecraft.osrs.content.minigames.Gauntlet.Mode.values()) {
            com.runecraft.boss.DataBossService.requirement(mode.boss, (player, def) -> {
                Msg.game(player, "The Hunllef only fights at the end of the Gauntlet. You step into the Gauntlet.");
                MinigameManager.enter(player, Minigame.GAUNTLET, mode == com.runecraft.osrs.content.minigames.Gauntlet.Mode.NORMAL
                        ? Minigame.GAUNTLET.entranceBlock() : com.runecraft.osrs.content.minigames.Gauntlet.CORRUPTED_ENTRANCE);
                return false;
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
        // Dominic Onion offers the four dream modes as well as his shop; the lobby's potion barrels hold bought doses.
        QuestScripts.onTalk("mg_dominic_onion", (player, npc) -> com.runecraft.content.minigames.games.NightmareZoneSession.dominic(player));
        BlockHooks.register(MinigameWorld.NMZ_BARRELS, (player, pos, state) ->
                com.runecraft.content.minigames.games.NightmareZoneSession.barrels(player));
        com.runecraft.content.minigames.games.NightmareZoneSession.register();
        BlockHooks.register("pyramid_plunder_artefact_trader", (player, pos, state) -> PyramidPlunderSession.sellArtefacts(player));
        BlockHooks.register("rogues_den_jewel_trader", (player, pos, state) -> RoguesDenSession.tradeJewel(player));
        BlockHooks.register("tempoross_reward_pool", (player, pos, state) -> TemporossSession.rewardPool(player));
        BlockHooks.register("gotr_rift_search", (player, pos, state) -> GuardiansSession.rewardsGuardian(player));
        QuestScripts.onTalk("mg_simon_templeton", (player, npc) -> PyramidPlunderSession.sellArtefacts(player));
        QuestScripts.onTalk("mg_brian_orichard", (player, npc) -> RoguesDenSession.tradeJewel(player));
        talk("mg_tzhaar_mej_jal", "TzHaar-Mej-Jal", "You want to challenge the Fight Cave, JalYt? Sixty-three waves of TzHaar await. "
                + "Survive, and TzTok-Jad himself will face you. The fire cape is the prize.");
        talk("mg_tzhaar_ket_keh", "TzHaar-Ket-Keh", "The Inferno is no place for JalYt. Sacrifice a fire cape and face TzKal-Zuk... "
                + "if you dare. The infernal cape will be yours if you survive sixty-nine waves.");
        talk("mg_tzhaar_mej_kah", "TzHaar-Mej-Kah", "The Fight Pit is where JalYt fight each other. Last one standing is champion: "
                + "tokkul for every fighter beaten and the red skull. Fight Cave creatures join if you are slow, then the gas.");
        talk("mg_ignisia", "Ignisia", "The Wintertodt stirs. Chop bruma roots, fletch them into kindling and feed the braziers. "
                + "Fix broken braziers with a hammer and keep the pyromancers alive. Earn 500 points for a supply crate.");
        talk("mg_first_mate_deri", "First Mate Deri", "Tempoross brews a storm! Harpoon the harpoonfish, load the cannons, "
                + "tether yourself when a wave comes, and strike the spirit pool when it falls. Permits buy from the reward pool.");
        talk("mg_harrallak_menarous", "Harrallak Menarous", "Welcome to the Warriors' Guild. Animate armour on the magical animators "
                + "to earn tokens, then spend them in the cyclops room for defenders.");
        QuestScripts.onTalk("mg_guardian_mummy", (player, entity) -> guardianMummy(player));
        talk("mg_murphy", "Murphy", "The Fishing Trawler sails soon. Patch leaks, bail water and keep her afloat for the net to fill.");
        talk("mg_mortton_priest", "Ulsquire Shauncy", "The shades plague Mort'ton. Repair the temple and slay the shades to earn "
                + "sanctity, bless olive oil in the sacred fire and coat logs with it. Burn shade remains on a pyre of those logs: "
                + "the spirits leave keys that open the chests in the catacombs.");

        // Arena objects.
        for (String[] b : MinigameWorld.ARENA_BLOCKS) {
            if (b[0].equals("tempoross_reward_pool")) {
                continue;
            }
            BlockHooks.register(b[0], (player, pos, state) -> MinigameManager.onBlock(player, b[0], pos));
        }

        // The Shade Catacombs chests standing in the dungeons module's catacombs below Mort'ton open outside the instance too.
        for (int i = 0; i < com.runecraft.content.minigames.games.ShadesSession.CHESTS.length; i++) {
            String metal = com.runecraft.osrs.content.minigames.ShadesOfMortton.METALS[i];
            BlockHooks.registerFallback(com.runecraft.content.minigames.games.ShadesSession.CHESTS[i], (player, pos, state) -> {
                java.util.List<String> uniques = com.runecraft.content.minigames.games.ShadesSession.openChest(player, metal, SHADE_CHEST_RANDOM);
                if (uniques != null) {
                    for (String u : uniques) {
                        Msg.game(player, "You find " + com.runecraft.osrs.ItemCatalog.get(u).name() + " in the " + metal + " chest!");
                    }
                }
            });
        }
        for (String[] b : com.runecraft.osrs.content.minigames.Gauntlet.blocks()) {
            BlockHooks.register(b[0], (player, pos, state) -> MinigameManager.onBlock(player, b[0], pos));
        }

        // Mort'ton's town objects (the funeral pyre too, which used to live in the arena) are handled outside the instance.
        ShadesService.register();

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
                entity.spawnAtLocation(com.runecraft.util.Inv.stack(scroll, 1));
                com.runecraft.collection.CollectionLog.obtain(player, scroll);
            }
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer player) || MinigameManager.allowDeath(player));
        // Ectoplasmator (Soul Wars): Prayer XP of 20% of a spectral creature's hitpoints on each kill while carried.
        com.runecraft.content.GameEvents.onKill((player, entity, stats) -> {
            if (stats != null && stats.attributes().contains("spectral")
                    && com.runecraft.util.Inv.has(player, "ectoplasmator", 1)) {
                int xp = stats.hitpoints() / 5;
                if (xp > 0) {
                    com.runecraft.skill.XpService.add(player, com.runecraft.osrs.Skill.PRAYER, xp);
                }
            }
        });
        // Nightmare Zone absorption soaks up monster damage before it lands.
        com.runecraft.combat.CombatHooks.addIncomingModifier(com.runecraft.content.minigames.games.NightmareZoneSession::absorb);
        ServerTickEvents.END_SERVER_TICK.register(MinigameManager::serverTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            MinigameManager.onDisconnect(handler.getPlayer());
            MinigameNet.forget(handler.getPlayer());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> MinigameManager.onJoin(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("minigame")
                        .then(Commands.literal("leave").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                MinigameManager.leave(p);
                            }
                            return 1;
                        }))
                        .then(Commands.literal("colosseum")
                                .then(Commands.literal("pick").then(Commands.argument("choice", IntegerArgumentType.integer(1, 3))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            if (p != null && MinigameManager.session(p)
                                                    instanceof com.runecraft.content.minigames.games.ColosseumSession c) {
                                                c.pick(p, IntegerArgumentType.getInteger(ctx, "choice"));
                                            }
                                            return 1;
                                        })))
                                .then(Commands.literal("leave").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null && MinigameManager.session(p) instanceof com.runecraft.content.minigames.games.ColosseumSession c) {
                                        c.cashOut(p);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("salute").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        com.runecraft.content.minigames.games.ColosseumSession.salute(p);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("respawn").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        com.runecraft.content.minigames.games.ColosseumSession.setRespawn(p);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("quiver").then(Commands.argument("for", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            if (p != null) {
                                                com.runecraft.content.minigames.games.ColosseumSession.tradeQuiver(p,
                                                        StringArgumentType.getString(ctx, "for").equals("pet"));
                                            }
                                            return 1;
                                        }))))
                        .then(Commands.literal("redirect").then(Commands.argument("town", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        MinigameItemUse.redirect(p, StringArgumentType.getString(ctx, "town"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("trek").then(Commands.argument("follower", StringArgumentType.word())
                                .then(Commands.argument("route", IntegerArgumentType.integer(1, 3)).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        com.runecraft.content.minigames.games.TempleTrekkingSession.choose(p,
                                                StringArgumentType.getString(ctx, "follower"), IntegerArgumentType.getInteger(ctx, "route"));
                                    }
                                    return 1;
                                }))))
                        .then(Commands.literal("nmz")
                                .then(Commands.literal("bosses").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        com.runecraft.content.minigames.games.NightmareZoneSession.toggle(p, null);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("shop").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null && nearNpc(p, "mg_dominic_onion")) {
                                        com.runecraft.content.minigames.games.NightmareZoneSession.shop(p);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("toggle").then(Commands.argument("boss", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            if (p != null) {
                                                com.runecraft.content.minigames.games.NightmareZoneSession.toggle(p,
                                                        StringArgumentType.getString(ctx, "boss"));
                                            }
                                            return 1;
                                        })))
                                .then(Commands.literal("barrel").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null && nearBarrels(p)) {
                                        com.runecraft.content.minigames.games.NightmareZoneSession.barrels(p);
                                    }
                                    return 1;
                                }).then(Commands.argument("potion", StringArgumentType.word())
                                        .then(Commands.argument("doses", IntegerArgumentType.integer(1, 255)).executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            if (p != null && nearBarrels(p)) {
                                                com.runecraft.content.minigames.games.NightmareZoneSession.withdraw(p,
                                                        StringArgumentType.getString(ctx, "potion"), IntegerArgumentType.getInteger(ctx, "doses"));
                                            }
                                            return 1;
                                        }))))
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .then(Commands.argument("difficulty", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayer();
                                                    if (p != null && nearNpc(p, "mg_dominic_onion")) {
                                                        com.runecraft.content.minigames.games.NightmareZoneSession.choose(p,
                                                                StringArgumentType.getString(ctx, "mode"),
                                                                StringArgumentType.getString(ctx, "difficulty"), null);
                                                    }
                                                    return 1;
                                                })
                                                .then(Commands.argument("boss", StringArgumentType.word()).executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayer();
                                                    if (p != null && nearNpc(p, "mg_dominic_onion")) {
                                                        com.runecraft.content.minigames.games.NightmareZoneSession.choose(p,
                                                                StringArgumentType.getString(ctx, "mode"),
                                                                StringArgumentType.getString(ctx, "difficulty"),
                                                                StringArgumentType.getString(ctx, "boss"));
                                                    }
                                                    return 1;
                                                })))))
                        .then(Commands.literal("token").then(Commands.argument("reward", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        com.runecraft.content.minigames.games.TempleTrekkingSession.claim(p,
                                                StringArgumentType.getString(ctx, "reward"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("balevel").then(Commands.argument("role", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        com.runecraft.content.minigames.games.BarbarianAssaultSession.buyLevel(p,
                                                StringArgumentType.getString(ctx, "role"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("museum").then(Commands.argument("answer", IntegerArgumentType.integer(0, 2))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        MuseumService.answer(p, IntegerArgumentType.getInteger(ctx, "answer"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("sing").then(Commands.argument("recipe", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        com.runecraft.content.minigames.games.GauntletSession.sing(p,
                                                StringArgumentType.getString(ctx, "recipe"));
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("points").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                showPoints(p);
                            }
                            return 1;
                        }))
                        .then(Commands.literal("shop").then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null && ctx.getSource().hasPermission(2)) {
                                        RewardShopService.open(p, StringArgumentType.getString(ctx, "id"));
                                    }
                                    return 1;
                                })))));
    }

    /**
     * The guardian mummy: explains Pyramid Plunder, and recharges a carried pharaoh's sceptre (wiki: 24 ivory/pottery, 12
     * stone or 6 gold artefacts for a full recharge; 3 charges, up to 100 with the Desert Diary tiers).
     */
    private static void guardianMummy(ServerPlayer p) {
        net.minecraft.world.item.ItemStack sceptre = net.minecraft.world.item.ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = p.getInventory().getItem(i);
            if (com.runecraft.osrs.content.systems.Charges.PHARAOHS_SCEPTRE.equals(com.runecraft.util.Inv.idOf(s))) {
                sceptre = s;
                break;
            }
        }
        if (sceptre.isEmpty()) {
            QuestApi.say(p, "Guardian mummy", "You may plunder the pyramid for five minutes. Urns, chests and the sarcophagus "
                    + "hold treasure... and the Pharaoh's sceptre itself, for the lucky.");
            return;
        }
        java.util.Set<String> done = com.runecraft.content.systems.SysState.of(p).diaryDone;
        int tiers = 0;
        for (com.runecraft.osrs.content.systems.Diaries.Tier t : com.runecraft.osrs.content.systems.Diaries.Tier.values()) {
            if (com.runecraft.osrs.content.systems.Diaries.tierComplete(done, "desert", t)) {
                tiers = t.ordinal() + 1;
            }
        }
        int cap = com.runecraft.osrs.content.systems.Charges.pharaohsSceptreCap(tiers);
        int now = com.runecraft.item.ItemCharges.get(sceptre);
        if (now >= cap) {
            QuestApi.say(p, "Guardian mummy", "Your sceptre is already fully charged, thief.");
            return;
        }
        Map<String, Integer> carried = new java.util.HashMap<>();
        for (String[] material : com.runecraft.osrs.content.minigames.MinigameRules.PyramidPlunder.SCEPTRE_ARTEFACTS) {
            for (String id : material) {
                carried.put(id, com.runecraft.util.Inv.count(p, id));
            }
        }
        Map<String, Integer> cost = com.runecraft.osrs.content.minigames.MinigameRules.PyramidPlunder.sceptreRechargeCost(carried);
        if (cost == null) {
            QuestApi.say(p, "Guardian mummy", "You dare carry the Pharaoh's sceptre before me? Bring me 24 ivory or pottery "
                    + "artefacts, 12 of stone or 6 of gold, and I will restore its power.");
            return;
        }
        for (Map.Entry<String, Integer> e : cost.entrySet()) {
            com.runecraft.util.Inv.remove(p, e.getKey(), e.getValue());
        }
        com.runecraft.item.ItemCharges.set(sceptre, cap);
        QuestApi.say(p, "Guardian mummy", "Grrr... very well. Your sceptre now holds " + cap + " charges.");
    }

    /** Whether the player stands near one of an NPC's entities (commands behind clickable dialogue options). */
    static boolean nearNpc(ServerPlayer p, String npcId) {
        boolean near = !p.level().getEntitiesOfClass(com.runecraft.entity.QuestNpcEntity.class, p.getBoundingBox().inflate(12),
                e -> npcId.equals(e.npcId())).isEmpty();
        if (!near) {
            Msg.game(p, "You need to be next to them to do that.");
        }
        return near;
    }

    /**
     * Whether the player is by the Nightmare Zone potion barrels, or by Dominic Onion: a lobby built before the barrels were
     * added has no barrel block (places are built once), so the doses bought there are withdrawn through him.
     */
    static boolean nearBarrels(ServerPlayer p) {
        net.minecraft.world.level.block.state.BlockState want = Arena.module(MinigameWorld.NMZ_BARRELS);
        net.minecraft.core.BlockPos at = p.blockPosition();
        for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(at.offset(-6, -3, -6), at.offset(6, 3, 6))) {
            if (p.level().getBlockState(q).is(want.getBlock())) {
                return true;
            }
        }
        if (!p.level().getEntitiesOfClass(com.runecraft.entity.QuestNpcEntity.class, p.getBoundingBox().inflate(12),
                e -> "mg_dominic_onion".equals(e.npcId())).isEmpty()) {
            return true;
        }
        Msg.game(p, "You need to be next to the potion barrels or Dominic Onion.");
        return false;
    }

    private static void talk(String npc, String name, String line) {
        QuestScripts.onTalk(npc, (player, entity) -> QuestApi.say(player, name, line));
    }

    /** TzHaar-Ket-Rak: the challenges, what unlocks them and the JalRek-Jad metamorphosis. */
    private static void ketRak(ServerPlayer p) {
        MinigameData data = MinigameData.of(p);
        boolean inferno = data.stat(p.getUUID(), "kc:" + Minigame.INFERNO.id()) > 0;
        QuestApi.say(p, "TzHaar-Ket-Rak", "The JalTok-Jads grow in number, JalYt. Help me thin them out: my first challenge is one Jad, "
                + "the second two, and so on up to six at once. " + (inferno ? "You have bested TzKal-Zuk, so all my challenges are open to you,"
                + " even the final one, where they never stop coming."
                : "Defeat TzKal-Zuk in the Inferno and I will offer you the third challenge and beyond."));
        if (data.flag(p.getUUID(), com.runecraft.osrs.content.minigames.KetRakChallenges.METAMORPHOSIS)) {
            QuestApi.say(p, "TzHaar-Ket-Rak", "You have completed every challenge. Your TzRek-Jad may take the form of a JalRek-Jad.");
        }
    }

    /** The scoreboard beside TzHaar-Ket-Rak: completions, best time and deaths per challenge, and the endurance record. */
    private static void ketRakScoreboard(ServerPlayer p) {
        MinigameData data = MinigameData.of(p);
        java.util.UUID u = p.getUUID();
        Msg.game(p, "--- TzHaar-Ket-Rak's Challenges ---");
        for (int c = 1; c <= com.runecraft.osrs.content.minigames.KetRakChallenges.CHALLENGES; c++) {
            int best = data.stat(u, com.runecraft.osrs.content.minigames.KetRakChallenges.BEST_PREFIX + c);
            long seconds = best * 6L / 10;
            Msg.game(p, "Challenge " + c + ": " + data.stat(u, com.runecraft.osrs.content.minigames.KetRakChallenges.KC_PREFIX + c)
                    + " completed, best " + (best > 0 ? String.format("%d:%02d", seconds / 60, seconds % 60) : "-") + ", "
                    + data.stat(u, com.runecraft.osrs.content.minigames.KetRakChallenges.DEATHS_PREFIX + c) + " deaths");
        }
        int f = com.runecraft.osrs.content.minigames.KetRakChallenges.FINAL;
        Msg.game(p, "Final challenge: most JalTok-Jads slain " + data.stat(u, com.runecraft.osrs.content.minigames.KetRakChallenges.FINAL_RECORD)
                + ", " + data.stat(u, com.runecraft.osrs.content.minigames.KetRakChallenges.DEATHS_PREFIX + f) + " attempts");
    }

    private static void showPoints(ServerPlayer p) {
        MinigameData data = MinigameData.of(p);
        Msg.game(p, "--- Minigame points ---");
        for (Map.Entry<String, String> e : MinigameCurrencies.all().entrySet()) {
            if (e.getKey().startsWith("pts:")) {
                int v = data.points(p.getUUID(), MinigameCurrencies.key(e.getKey()));
                if (v != 0) {
                    Msg.game(p, e.getValue() + ": " + String.format("%,d", v));
                }
            }
        }
        for (Minigame m : Minigame.values()) {
            int kc = data.stat(p.getUUID(), "kc:" + m.id());
            if (kc > 0) {
                Msg.game(p, m.displayName() + " completions: " + kc);
            }
        }
    }
}
