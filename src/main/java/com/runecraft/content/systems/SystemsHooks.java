package com.runecraft.content.systems;

import com.runecraft.combat.CombatHooks;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.content.ModuleHooks;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.Diaries;
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

        CombatHooks.addModifier(new SysModifier());
        CombatHooks.addHurtListener(JewelleryService::onHurt);

        GameEvents.onKill((p, e, stats) -> {
            ClueService.onKill(p, e, stats);
            DiaryService.onKill(p, e, stats);
            SpellService.onKill(p, e);
        });
        GameEvents.onXp(DiaryService::onXp);
        GameEvents.onMake(DiaryService::onMake);

        SysTicks.everyPlayer((p, tick) -> {
            SpecialAttackService.gameTick(p, tick);
            RunEnergyService.gameTick(p, tick);
            JewelleryService.gameTick(p, tick);
            DiaryService.gameTick(p, tick);
            SpellService.thrallTick(p, tick);
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

        UseItemCallback.EVENT.register(SystemsHooks::useItem);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND || world.isClient() || !(entity instanceof QuestNpcEntity npc) || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            return ClueService.talk(sp, npc) ? ActionResult.SUCCESS : ActionResult.PASS;
        });
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
                || id.startsWith("antique_lamp_") || RunEnergyService.isEnergyPotion(id) || id.equals("ectophial")) {
            return true;
        }
        if (Jewellery.worn(id) != null && ItemCatalog.get(id) != null) {
            return true;
        }
        if (id.startsWith("falador_shield_")) {
            return true;
        }
        return sneaking && !diaryItemTeleports(id).isEmpty();
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
            if (!DiaryService.useDaily(p, "falador_shield", 1)) {
                Msg.game(p, "You have already used your shield's prayer restore today.");
                return true;
            }
            PlayerData d = Players.data(p);
            int max = d.skills().currentLevel(com.runecraft.osrs.Skill.PRAYER);
            int pct = tier == 1 ? 25 : tier == 2 ? 50 : 100;
            d.setPrayerPoints(d.prayerPoints() + max * pct / 100);
            com.runecraft.network.ServerNetworking.sync(p);
            Msg.action(p, "Your shield recharges your prayer.");
            return true;
        }
        if (sneaking) {
            List<Teleports.Destination> dests = diaryItemTeleports(id);
            if (!dests.isEmpty()) {
                SysTeleport.teleport(p, dests.get(dests.size() > 1 && JewelleryService.tierOf(id) >= 3 ? 1 : 0), SysTeleport.JEWELLERY_WILDERNESS);
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
        int tier = JewelleryService.tierOf(id);
        if (id.startsWith("fremennik_sea_boots_") && tier >= 2) {
            return List.of(Teleports.at("Rellekka", 2660, 3657));
        }
        if (id.startsWith("kandarin_headgear_") && tier >= 3) {
            return List.of(Teleports.at("Sherlock", 2733, 3415));
        }
        if (id.startsWith("morytania_legs_") && tier >= 2) {
            return List.of(Teleports.at("Ectofuntus", 3660, 3522), Teleports.at("Burgh de Rott", 3482, 3231));
        }
        if (id.startsWith("wilderness_sword_") && tier >= 3) {
            return List.of(Teleports.at("Fountain of Rune", 3375, 3893));
        }
        return List.of();
    }
}
