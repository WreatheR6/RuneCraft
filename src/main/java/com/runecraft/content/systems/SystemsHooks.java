package com.runecraft.content.systems;

import com.runecraft.combat.CombatHooks;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.content.ModuleHooks;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.systems.CluePuzzles;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.osrs.content.systems.DiaryRewards;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.content.systems.SystemsContent;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.UseEntityCallback;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Minecraft-side behaviour for the systems content module. */
public final class SystemsHooks implements ModuleHooks {
    @Override
    public void init() {
        SysNet.register();
        SysTicks.register();
        SysMenus.registerHandlers();
        CapeService.registerCommands();
        AccessGateService.register();
        ChargeService.register();
        LootingBagService.register();
        PrayerOfferings.register();
        CombatAchievementService.register();
        CaMenus.registerHandlers();
        DiaryExtrasService.register();
        CannonService.register();

        CombatHooks.addModifier(new SysModifier());
        // Special attacks: PlayerHitCalc applies their accuracy / damage changes when the attack is a special.
        com.runecraft.combat.HitCalcBridge.specialAttack = SpecialAttackService::isSpecial;
        com.runecraft.combat.HitCalcBridge.soulStacks = p -> SysState.of(p).soulStacks;
        CombatHooks.addHurtListener(JewelleryService::onHurt);
        CombatHooks.addIncomingModifier(JewelleryService::ethereum);

        GameEvents.onKill((p, e, stats) -> {
            ClueService.onKill(p, e, stats);
            DiaryService.onKill(p, e, stats);
            SpellService.onKill(p, e);
            JewelleryService.onKill(p, e, stats);
            boolean mimic = com.runecraft.osrs.content.systems.MimicRewards.MIMIC.equals(
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath())
                    || (e instanceof com.runecraft.boss.DataBossEntity b && b.def() != null
                    && com.runecraft.osrs.content.systems.MimicRewards.MIMIC.equals(b.def().id));
            if (mimic) {
                String unique = com.runecraft.osrs.content.systems.MimicRewards.roll(new java.util.Random(p.getRandom().nextLong()));
                if (unique != null) {
                    e.spawnAtLocation(Inv.stack(unique, 1));
                    com.runecraft.collection.CollectionLog.obtain(p, unique);
                }
                ClueService.onMimicKilled(p); // the mimic casket's own rewards
            }
        });
        GameEvents.onXp(DiaryService::onXp);
        GameEvents.onMake(DiaryService::onMake);
        GameEvents.onObtain(DiaryService::onObtain);
        GameEvents.onCast(DiaryService::onCast);
        GameEvents.onActivity(DiaryService::onActivity);
        // Ardougne Zoo: talking to the zookeeper as a monkey (greegree held) gets you put in the monkey cage.
        GameEvents.onActivity((p, key) -> {
            if (key.equals("talk:charlie_zookeeper") && "monkey_greegree".equals(Inv.idOf(p.getMainHandItem()))) {
                Msg.game(p, "Zookeeper: Hey! How did that monkey get out? Back in the cage with you!");
                GameEvents.fireActivity(p, "monkey_cage");
            }
            // Yanille: after The Hand in the Sand, Bert sends 84 buckets of sand to your bank once a day.
            if (key.equals("talk:bert_sandman")) {
                com.runecraft.osrs.QuestInfo sand = com.runecraft.osrs.content.ContentRegistry.quest("the_hand_in_the_sand");
                if (sand != null && com.runecraft.player.Players.data(p).questComplete(sand)
                        && DiaryService.useDaily(p, "bert_sand", 1)) {
                    long sent = com.runecraft.bank.BankService.depositStack(com.runecraft.player.Players.data(p).bank(),
                            Inv.stack("bucket_of_sand", 1), 84, 0);
                    Msg.game(p, "Bert: I've had " + sent + " buckets of sand sent to your bank. Come back tomorrow for more!");
                    GameEvents.fireActivity(p, "bert_sand");
                }
            }
        });

        SysTicks.everyPlayer((p, tick) -> {
            SpecialAttackService.gameTick(p, tick);
            RunEnergyService.gameTick(p, tick);
            JewelleryService.gameTick(p, tick);
            DiaryService.gameTick(p, tick);
            SpellService.thrallTick(p, tick);
            SpellService.corruptionTick(p, tick);
            if ((tick + p.getId()) % 200 == 0) {
                CapeService.snapshot(p);
            }
            if (tick % 50 == 0) {
                SysNet.sendState(p);
            }
        });
        SysTicks.everyTick((server, tick) -> {
            SysCombat.gameTick(tick);
            if (tick % 100 == 0) {
                GeService.minute(server);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            server.execute(() -> {
                SysNet.sendState(p);
                CapeService.snapshot(p);
            });
        });

        // Blocks
        BlockHooks.register(SystemsContent.GE_BOOTH, (p, pos, state) -> GeService.openAtBooth(p));
        BlockHooks.register(SystemsContent.CAPE_STAND, (p, pos, state) -> CapeService.open(p));
        BlockHooks.register(SystemsContent.FOUNTAIN, JewelleryService::fountain);
        BlockHooks.register(SystemsContent.ANCIENT_ALTAR, (p, pos, state) -> SysMenus.altar(p, Spellbooks.Book.ANCIENT));
        BlockHooks.register(SystemsContent.LUNAR_ALTAR, (p, pos, state) -> SysMenus.altar(p, Spellbooks.Book.LUNAR));
        BlockHooks.register(SystemsContent.DARK_ALTAR, (p, pos, state) -> SysMenus.altar(p, Spellbooks.Book.ARCEUUS));
        BlockHooks.register(SystemsContent.DIARY_BOARD, (p, pos, state) -> SysMenus.diaries(p));
        SysNet.onAction("dtele", SystemsHooks::diaryTeleAction);

        // NPCs
        QuestScripts.onTalk("grand_exchange_clerk", (p, npc) -> {
            QuestApi.say(p, "Grand Exchange Clerk", "Welcome to the Grand Exchange. Would you like to trade?");
            GeService.openAtBooth(p);
        });
        QuestScripts.onTalk("wise_old_man", (p, npc) -> {
            QuestApi.say(p, "Wise Old Man", "Greetings! Only those who have completed every quest may wear my cape.");
            CapeService.open(p);
        });
        QuestScripts.onTalk("achievement_diary_master", (p, npc) -> {
            QuestApi.say(p, "Diary master", "Let's see how your achievement diaries are coming along.");
            SysMenus.diaries(p);
        });
        QuestScripts.onTalk(SystemsContent.WATSON, (p, npc) -> ClueService.watson(p));
        SysNet.onAction("watson", ClueService::watsonAction);
        ClueService.register();
        QuestScripts.onTalk(SystemsContent.GHOMMAL, (p, npc) -> {
            QuestApi.say(p, "Ghommal", "Ghommal tests your strength. Show Ghommal what you have achieved in combat!");
            CaMenus.overview(p);
        });
        // Combat Achievement rewards that live in other systems' mechanics.
        com.runecraft.content.godwars.GwdKillcount.required = p -> CombatAchievements.gwdKillcount(CombatAchievementService.tier(p));
        com.runecraft.content.godwars.GwdKillcount.ecumenicalChance = p ->
                CombatAchievements.ecumenicalKeyChance(CombatAchievementService.tier(p));
        com.runecraft.content.minigames.games.PestControlSession.extraPoints = p ->
                CombatAchievements.pestControlBonus(CombatAchievementService.tier(p));
        com.runecraft.content.minigames.games.WarriorsGuildSession.tokenMultiplier = p ->
                CombatAchievements.warriorTokenMultiplier(CombatAchievementService.tier(p));

        // Spell effects that live in other systems' mechanics.
        com.runecraft.skill.ThievingService.failUnnoticed = p -> SysState.of(p).shadowVeilUntil > SysTicks.now()
                && p.getRandom().nextDouble() < com.runecraft.osrs.content.systems.Spellbooks.SHADOW_VEIL_CHANCE;
        com.runecraft.skill.Runecrafting.imbued = p -> SysState.of(p).magicImbueUntil > SysTicks.now();
        PrayerScrolls.register();

        // Diary perks that live in other systems' mechanics.
        com.runecraft.skill.ThievingService.pickpocketBonus = p -> {
            SysState.SysPlayer sp = SysState.of(p);
            double gloves = sp.wornId(Jewellery.Slot.HANDS).equals("gloves_of_silence")
                    ? com.runecraft.osrs.content.skilling.HunterData.GLOVES_OF_SILENCE_BONUS : 1.0;
            return gloves * DiaryRewards.pickpocketMultiplier(Diaries.tierComplete(sp.diaryDone, "ardougne", Diaries.Tier.MEDIUM),
                    Diaries.tierComplete(sp.diaryDone, "ardougne", Diaries.Tier.HARD), DiaryService.inArea(p, "ardougne"));
        };
        // Thieving gear in the extra slots: Thieving cape, rogue gloves, the dodgy necklace (25%, 10 charges kept
        // on the worn necklace) and the Ardougne diary's larger coin pouch stack.
        com.runecraft.skill.ThievingService.wearingExtra = (p, id) -> SysState.of(p).wearing(id);
        com.runecraft.skill.ThievingService.dodgyNecklace = p -> {
            SysState.SysPlayer sp = SysState.of(p);
            net.minecraft.world.item.ItemStack neck = sp.worn(Jewellery.Slot.NECK);
            if (neck.isEmpty() || !com.runecraft.util.Inv.idOf(neck).equals("dodgy_necklace")
                    || p.getRandom().nextDouble() >= com.runecraft.osrs.data.Thieving.DODGY_NECKLACE_CHANCE) {
                return false;
            }
            int used = com.runecraft.util.StackData.get(neck).getInt("DodgyUsed") + 1;
            if (used >= com.runecraft.osrs.data.Thieving.DODGY_NECKLACE_CHARGES) {
                sp.worn.remove(Jewellery.Slot.NECK);
                Msg.red(p, "Your dodgy necklace crumbles to dust.");
            } else {
                com.runecraft.util.StackData.update(neck, t -> t.putInt("DodgyUsed", used));
            }
            SysNet.sendState(p);
            return true;
        };
        com.runecraft.skill.ThievingService.coinPouchLimit = p -> {
            SysState.SysPlayer sp = SysState.of(p);
            int tier = Diaries.tierComplete(sp.diaryDone, "ardougne", Diaries.Tier.ELITE) ? 4
                    : Diaries.tierComplete(sp.diaryDone, "ardougne", Diaries.Tier.HARD) ? 3
                    : Diaries.tierComplete(sp.diaryDone, "ardougne", Diaries.Tier.MEDIUM) ? 2 : 0;
            return com.runecraft.osrs.data.Thieving.coinPouchLimit(tier);
        };
        com.runecraft.content.dungeons.BarrowsHooks.runeBonus = p ->DiaryRewards.barrowsRuneMultiplier(
                Diaries.tierComplete(SysState.of(p).diaryDone, "morytania", Diaries.Tier.HARD));
        com.runecraft.content.minigames.RewardShopService.costAdjuster = (p, shop, amount) -> shop.equals("tzhaar")
                ? DiaryRewards.tzhaarPrice(amount, SysState.of(p).wornId(Jewellery.Slot.HANDS).startsWith("karamja_gloves_")) : amount;
        com.runecraft.content.minigames.RewardShopService.imbueCost = (p, amount) ->
                CombatAchievements.imbueCost(amount, CombatAchievementService.tier(p));
        com.runecraft.content.minigames.RewardShopService.extraLock = (p, item) ->
                DiaryRewards.pestControlUnlocked(item, diaryDone(p, "western", Diaries.Tier.HARD)) ? null
                        : "Requires the hard Western Provinces diary";
        com.runecraft.skill.AgilityService.markChance = (p, course, chance) -> course == com.runecraft.osrs.data.AgilityCourse.SEERS
                ? DiaryRewards.seersMarkChance(chance, diaryDone(p, "kandarin", Diaries.Tier.HARD) ? 3
                : diaryDone(p, "kandarin", Diaries.Tier.MEDIUM) ? 2 : diaryDone(p, "kandarin", Diaries.Tier.EASY) ? 1 : 0)
                : chance;
        // Only the Hosidius kitchen range has a burn reduction; its size depends on the Kourend & Kebos diary.
        com.runecraft.menu.MenuService.rangeBurnReduction = (p, r) -> DiaryRewards.hosidiusBurnReduction(
                diaryDone(p, "kourend", Diaries.Tier.EASY), diaryDone(p, "kourend", Diaries.Tier.ELITE));
        com.runecraft.content.islands.IslandsTravelService.staffless = p ->
                DiaryRewards.fairyRingsWithoutStaff(diaryDone(p, "lumbridge", Diaries.Tier.ELITE));
        CombatHooks.addDropTransformer((killer, mob, item, count) -> {
            String monster = mob instanceof com.runecraft.boss.DataBossEntity b && b.def() != null ? b.def().id
                    : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
            boolean wild = com.runecraft.world.Gielinor.wildernessLevel(mob) > 0;
            return DiaryRewards.notedDrop(monster, item, wild, (area, tier) -> diaryDone(killer, area, tier))
                    ? com.runecraft.bank.Notes.note(item, "", count) : null;
        });
        QuestScripts.onTalk(CluePuzzles.FALO, (p, npc) -> QuestApi.say(p, "Falo the Bard",
                "I'm Falo, the bard. Bring me what my songs ask for and I'll help you on your treasure trail."));

        ItemSourcesService.register(); // after QuestsHooks: wraps Eluned's, the Lumbridge Guide's and Sigli's dialogue
        LyreService.register(); // after QuestsHooks: wraps the Fossegrimen (enchanted lyre charges)
        // Ring of pursuit: one per-player charge (Jewellery.PURSUIT_CHARGES) per noose-wand catch it speeds up.
        com.runecraft.content.skilling.GatheringHooks.pursuitTrack = p -> {
            if (!"ring_of_pursuit".equals(SysState.of(p).wornId(Jewellery.Slot.RING))) {
                return false;
            }
            JewelleryService.useCharge(p, Jewellery.Slot.RING, "ring_of_pursuit");
            return true;
        };
        UseItemCallback.EVENT.register(SystemsHooks::useItem);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || world.isClientSide() || !(entity instanceof QuestNpcEntity npc) || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            return ClueService.talk(sp, npc) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    private static boolean diaryDone(ServerPlayer p, String area, Diaries.Tier tier) {
        return Diaries.tierComplete(SysState.of(p).diaryDone, area, tier);
    }

    /** The clue tier of a scroll box item, or null. */
    static ClueScrolls.Tier scrollBoxTier(String id) {
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            if (com.runecraft.osrs.content.systems.ClueRewards.scrollBox(t).equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** Whether an item's right-click is decided purely by its id (so the client can predict it). */
    static boolean deterministic(String id, boolean sneaking) {
        if (ClueScrolls.Tier.byScroll(id) != null || ClueScrolls.Tier.byCasket(id) != null || Spellbooks.tablet(id) != null
                || id.startsWith("antique_lamp_") || id.startsWith(CombatAchievements.LAMP_PREFIX) || RunEnergyService.isEnergyPotion(id) || id.equals("ectophial")
                || id.equals(CluePuzzles.PUZZLE_BOX) || id.equals(CluePuzzles.LIGHT_BOX) || id.equals("basalt")
                || scrollBoxTier(id) != null || id.equals("sextant") || id.startsWith("strange_device")) {
            return true;
        }
        if (Jewellery.worn(id) != null && ItemCatalog.get(id) != null) {
            return true;
        }
        if (Jewellery.held(id) != null && !sneaking) {
            return true;
        }
        if (id.startsWith("falador_shield_") || DiaryExtrasService.handles(id, sneaking)) {
            return true;
        }
        return sneaking && (!diaryItemTeleports(id).isEmpty() || CombatAchievements.hiltTier(id) > 0);
    }

    private static InteractionResultHolder<ItemStack> useItem(Player player, net.minecraft.world.level.Level world, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        String id = Inv.idOf(stack);
        boolean sneaking = player.isShiftKeyDown();
        if (world.isClientSide()) {
            return deterministic(id, sneaking) ? InteractionResultHolder.sidedSuccess(stack, true) : InteractionResultHolder.pass(stack);
        }
        if (!(player instanceof ServerPlayer p)) {
            return InteractionResultHolder.pass(stack);
        }
        if (handle(p, stack, hand, id, sneaking)) {
            return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), false);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static boolean handle(ServerPlayer p, ItemStack stack, InteractionHand hand, String id, boolean sneaking) {
        if (p.getCooldowns().isOnCooldown(stack.getItem())) {
            return deterministic(id, sneaking);
        }
        ClueScrolls.Tier clue = ClueScrolls.Tier.byScroll(id);
        if (clue != null) {
            if (!ClueService.dig(p, clue, false) && !ClueService.search(p, clue, false)) {
                ClueService.read(p, clue);
            }
            return true;
        }
        ClueScrolls.Tier casket = ClueScrolls.Tier.byCasket(id);
        if (casket != null) {
            ClueService.openCasket(p, stack, casket);
            return true;
        }
        ClueScrolls.Tier box = scrollBoxTier(id);
        if (box != null) {
            return ClueService.openScrollBox(p, stack, box);
        }
        if (id.equals("sextant")) {
            ClueService.sextant(p);
            return true;
        }
        if (id.startsWith("strange_device")) {
            ClueService.feel(p, id.endsWith("master") ? ClueScrolls.Tier.MASTER : ClueScrolls.Tier.BEGINNER);
            return true;
        }
        if (id.equals(CluePuzzles.PUZZLE_BOX) || id.equals(CluePuzzles.LIGHT_BOX)) {
            return ClueService.useBox(p, id);
        }
        if (id.equals("basalt")) {
            return ItemSourcesService.combineBasalt(p);
        }
        if (DiaryExtrasService.handles(id, sneaking)) {
            return DiaryExtrasService.use(p, stack, hand, id, sneaking);
        }
        Spellbooks.Tablet tab = Spellbooks.tablet(id);
        if (tab != null) {
            if (SysTeleport.teleport(p, tab.destination(), SysTeleport.SPELL_WILDERNESS)) {
                stack.shrink(1);
            }
            return true;
        }
        if (id.startsWith("antique_lamp_")) {
            try {
                SysMenus.lamp(p, Diaries.Tier.valueOf(id.substring("antique_lamp_".length()).toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // unknown lamp
            }
            return true;
        }
        if (id.startsWith(CombatAchievements.LAMP_PREFIX)) {
            try {
                CaMenus.lamp(p, CombatAchievements.Tier.valueOf(id.substring(CombatAchievements.LAMP_PREFIX.length())
                        .toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // unknown lamp
            }
            return true;
        }
        if (sneaking && CombatAchievements.hiltTier(id) > 0) {
            CombatAchievementService.hiltMenu(p, id);
            return true;
        }
        if (RunEnergyService.isEnergyPotion(id)) {
            RunEnergyService.drink(p, stack, hand, id);
            return true;
        }
        if (id.equals("ectophial")) {
            SysTeleport.teleport(p, Teleports.ECTOPHIAL.get(0), Teleports.wildernessLimit("ectophial"));
            return true;
        }
        if (id.equals("spade")) {
            for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
                if (ClueService.dig(p, t, false)) {
                    return true;
                }
            }
            return false;
        }
        if (Jewellery.held(id) != null && hand == InteractionHand.MAIN_HAND && !sneaking) {
            JewelleryService.openTeleports(p, "hand"); // the pharaoh's sceptre's teleports
            return true;
        }
        Jewellery.Worn worn = Jewellery.worn(id);
        if (worn != null && ItemCatalog.get(id) != null) {
            boolean explorer = id.startsWith("explorers_ring");
            if (hand == InteractionHand.MAIN_HAND && !sneaking && (explorer || !worn.teleports().isEmpty()
                    || (worn.chargeBase() != null && worn.charges() == 0 && Jewellery.recharged(worn.chargeBase()) != null))) {
                JewelleryService.openTeleports(p, "hand");
            } else {
                if (hand != InteractionHand.MAIN_HAND) {
                    JewelleryService.wear(p, hand);
                } else {
                    JewelleryService.wear(p, InteractionHand.MAIN_HAND);
                }
            }
            return true;
        }
        if (id.startsWith("falador_shield_")) {
            int tier = JewelleryService.tierOf(id);
            if (!DiaryService.useDaily(p, "falador_shield", DiaryRewards.faladorRestoresPerDay(tier))) {
                Msg.game(p, "You have already used your shield's prayer restore today.");
                return true;
            }
            PlayerData d = Players.data(p);
            int max = d.skills().level(com.runecraft.osrs.Skill.PRAYER);
            int pct = DiaryRewards.faladorRestorePercent(tier);
            d.setPrayerPoints(Math.min(max, d.prayerPoints() + max * pct / 100));
            com.runecraft.network.ServerNetworking.sync(p);
            Msg.action(p, "Your shield recharges your prayer.");
            return true;
        }
        if (sneaking) {
            List<Teleports.Destination> dests = diaryItemTeleports(id);
            if (dests.size() == 1) {
                DiaryService.rewardTeleport(p, id, 0);
                return true;
            }
            if (!dests.isEmpty()) {
                SysNet.Screen s = new SysNet.Screen("dtele", ItemCatalog.get(id) == null ? id : ItemCatalog.get(id).name());
                s.line("Where would you like to teleport to?");
                for (int i = 0; i < dests.size(); i++) {
                    s.button(id + "|" + i, DiaryService.rewardTeleLabel(p, id, i));
                }
                s.send(p);
                return true;
            }
        }
        ItemDef def = ModItems.def(stack);
        if (def != null && def.kind() == ItemDef.Kind.STAFF) {
            return SpellService.staffCast(p);
        }
        return false;
    }

    /** Teleports of diary rewards that are vanilla-wearable armour/weapons (used with sneak + right-click). */
    static List<Teleports.Destination> diaryItemTeleports(String id) {
        Diaries.Area a = DiaryRewards.areaOf(id);
        if (a == null || Jewellery.worn(id) != null) {
            return List.of();
        }
        return DiaryRewards.destinations(a.id(), DiaryRewards.tierOf(id));
    }

    /** Sneak-teleport menu of a held diary reward: "itemId|index". */
    static void diaryTeleAction(ServerPlayer p, String action, java.util.Map<String, String> fields) {
        int bar = action.lastIndexOf('|');
        if (bar < 0) {
            return;
        }
        String id = action.substring(0, bar);
        if (!Inv.idOf(p.getMainHandItem()).equals(id)) {
            Msg.game(p, "You are no longer holding that item.");
            return;
        }
        try {
            DiaryService.rewardTeleport(p, id, Integer.parseInt(action.substring(bar + 1)));
        } catch (NumberFormatException ignored) {
            // bad index
        }
    }
}
