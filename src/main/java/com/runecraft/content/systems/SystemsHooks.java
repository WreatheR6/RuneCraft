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
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

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
        CombatAchievementService.register();
        CaMenus.registerHandlers();

        CombatHooks.addModifier(new SysModifier());
        CombatHooks.addHurtListener(JewelleryService::onHurt);

        GameEvents.onKill((p, e, stats) -> {
            ClueService.onKill(p, e, stats);
            DiaryService.onKill(p, e, stats);
            SpellService.onKill(p, e);
        });
        GameEvents.onXp(DiaryService::onXp);
        GameEvents.onMake(DiaryService::onMake);
        GameEvents.onObtain(DiaryService::onObtain);
        GameEvents.onCast(DiaryService::onCast);

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
            ServerPlayerEntity p = handler.getPlayer();
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
        QuestScripts.onTalk("watson", SystemsHooks::watson);
        QuestScripts.onTalk(SystemsContent.GHOMMAL, (p, npc) -> {
            QuestApi.say(p, "Ghommal", "Ghommal tests your strength. Show Ghommal what you have achieved in combat!");
            CaMenus.overview(p);
        });
        // Combat Achievement rewards that live in other systems' mechanics.
        com.runecraft.content.godwars.GwdKillcount.required = p -> CombatAchievements.gwdKillcount(CombatAchievementService.tier(p));
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
            net.minecraft.item.ItemStack neck = sp.worn(Jewellery.Slot.NECK);
            if (neck.isEmpty() || !com.runecraft.util.Inv.idOf(neck).equals("dodgy_necklace")
                    || p.getRandom().nextDouble() >= com.runecraft.osrs.data.Thieving.DODGY_NECKLACE_CHANCE) {
                return false;
            }
            net.minecraft.nbt.NbtCompound nbt = neck.getOrCreateNbt();
            int used = nbt.getInt("DodgyUsed") + 1;
            if (used >= com.runecraft.osrs.data.Thieving.DODGY_NECKLACE_CHARGES) {
                sp.worn.remove(Jewellery.Slot.NECK);
                Msg.red(p, "Your dodgy necklace crumbles to dust.");
            } else {
                nbt.putInt("DodgyUsed", used);
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
                Diaries.tierComplete(SysState.of(p).diaryDone, "morytania", Diaries.Tier.ELITE));
        com.runecraft.content.minigames.RewardShopService.costAdjuster = (p, shop, amount) -> shop.equals("tzhaar")
                ? DiaryRewards.tzhaarPrice(amount, SysState.of(p).wornId(Jewellery.Slot.HANDS).startsWith("karamja_gloves_")) : amount;
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
                    : net.minecraft.registry.Registries.ENTITY_TYPE.getId(mob.getType()).getPath();
            boolean wild = com.runecraft.world.Gielinor.wildernessLevel(mob) > 0;
            return DiaryRewards.notedDrop(monster, item, wild, (area, tier) -> diaryDone(killer, area, tier))
                    ? com.runecraft.bank.Notes.note(item, "", count) : null;
        });
        QuestScripts.onTalk(CluePuzzles.FALO, (p, npc) -> QuestApi.say(p, "Falo the Bard",
                "I'm Falo, the bard. Bring me what my songs ask for and I'll help you on your treasure trail."));

        UseItemCallback.EVENT.register(SystemsHooks::useItem);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND || world.isClient() || !(entity instanceof QuestNpcEntity npc) || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            return ClueService.talk(sp, npc) ? ActionResult.SUCCESS : ActionResult.PASS;
        });
    }

    private static boolean diaryDone(ServerPlayerEntity p, String area, Diaries.Tier tier) {
        return Diaries.tierComplete(SysState.of(p).diaryDone, area, tier);
    }

    private static void watson(ServerPlayerEntity p, QuestNpcEntity npc) {
        String[] needed = {"clue_scroll_easy", "clue_scroll_medium", "clue_scroll_hard", "clue_scroll_elite"};
        for (String n : needed) {
            if (!Inv.has(p, n, 1)) {
                QuestApi.say(p, "Watson", "Bring me an easy, medium, hard and elite clue scroll and I'll give you a master clue in return.");
                return;
            }
        }
        SysState.SysPlayer sp = SysState.of(p);
        for (String n : needed) {
            Inv.remove(p, n, 1);
            ClueScrolls.Tier t = ClueScrolls.Tier.byScroll(n);
            sp.clues.remove(t);
        }
        Inv.give(p, "clue_scroll_master", 1);
        QuestApi.say(p, "Watson", "Splendid! Here is a master clue scroll. Good luck!");
    }

    /** Whether an item's right-click is decided purely by its id (so the client can predict it). */
    static boolean deterministic(String id, boolean sneaking) {
        if (ClueScrolls.Tier.byScroll(id) != null || ClueScrolls.Tier.byCasket(id) != null || Spellbooks.tablet(id) != null
                || id.startsWith("antique_lamp_") || id.startsWith(CombatAchievements.LAMP_PREFIX) || RunEnergyService.isEnergyPotion(id) || id.equals("ectophial")
                || id.equals(CluePuzzles.PUZZLE_BOX) || id.equals(CluePuzzles.LIGHT_BOX)) {
            return true;
        }
        if (Jewellery.worn(id) != null && ItemCatalog.get(id) != null) {
            return true;
        }
        if (id.startsWith("falador_shield_")) {
            return true;
        }
        return sneaking && (!diaryItemTeleports(id).isEmpty() || CombatAchievements.hiltTier(id) > 0);
    }

    private static TypedActionResult<ItemStack> useItem(PlayerEntity player, net.minecraft.world.World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return TypedActionResult.pass(stack);
        }
        String id = Inv.idOf(stack);
        boolean sneaking = player.isSneaking();
        if (world.isClient()) {
            return deterministic(id, sneaking) ? TypedActionResult.success(stack, true) : TypedActionResult.pass(stack);
        }
        if (!(player instanceof ServerPlayerEntity p)) {
            return TypedActionResult.pass(stack);
        }
        if (handle(p, stack, hand, id, sneaking)) {
            return TypedActionResult.success(p.getStackInHand(hand), false);
        }
        return TypedActionResult.pass(stack);
    }

    private static boolean handle(ServerPlayerEntity p, ItemStack stack, Hand hand, String id, boolean sneaking) {
        if (p.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            return deterministic(id, sneaking);
        }
        ClueScrolls.Tier clue = ClueScrolls.Tier.byScroll(id);
        if (clue != null) {
            if (!ClueService.dig(p, clue, false)) {
                ClueService.read(p, clue);
            }
            return true;
        }
        ClueScrolls.Tier casket = ClueScrolls.Tier.byCasket(id);
        if (casket != null) {
            ClueService.openCasket(p, stack, casket);
            return true;
        }
        if (id.equals(CluePuzzles.PUZZLE_BOX) || id.equals(CluePuzzles.LIGHT_BOX)) {
            return ClueService.useBox(p, id);
        }
        Spellbooks.Tablet tab = Spellbooks.tablet(id);
        if (tab != null) {
            if (SysTeleport.teleport(p, tab.destination(), SysTeleport.SPELL_WILDERNESS)) {
                stack.decrement(1);
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
            SysTeleport.teleport(p, Teleports.ECTOPHIAL.get(0), SysTeleport.JEWELLERY_WILDERNESS);
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
        Jewellery.Worn worn = Jewellery.worn(id);
        if (worn != null && ItemCatalog.get(id) != null) {
            boolean explorer = id.startsWith("explorers_ring");
            if (hand == Hand.MAIN_HAND && !sneaking && (explorer || !worn.teleports().isEmpty()
                    || (worn.chargeBase() != null && worn.charges() == 0 && Jewellery.recharged(worn.chargeBase()) != null))) {
                JewelleryService.openTeleports(p, "hand");
            } else {
                if (hand != Hand.MAIN_HAND) {
                    JewelleryService.wear(p, hand);
                } else {
                    JewelleryService.wear(p, Hand.MAIN_HAND);
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
    static void diaryTeleAction(ServerPlayerEntity p, String action, java.util.Map<String, String> fields) {
        int bar = action.lastIndexOf('|');
        if (bar < 0) {
            return;
        }
        String id = action.substring(0, bar);
        if (!Inv.idOf(p.getMainHandStack()).equals(id)) {
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
