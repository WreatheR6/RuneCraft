package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.content.GameEvents;
import com.runecraft.content.ModuleHooks;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.HerbloreData;
import com.runecraft.osrs.content.skilling.MiningData;
import com.runecraft.osrs.content.skilling.RunecraftData;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft-side behaviour for the skilling content module. Each skill area registers its own
 * hooks; this class wires them up and drives the periodic work (wine fermentation, divine potions,
 * boat tiers) and the prospector kit's Mining XP bonus.
 */
public final class SkillingHooks implements ModuleHooks {
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, Double> PROSPECTOR_REMAINDER = new HashMap<>();
    private static boolean awarding;

    @Override
    public void init() {
        ItemHooks.init();
        AgilityHooks.init();
        GatheringHooks.init();
        AerialFishingHooks.init();
        ShootingStarHooks.init();
        CamdozaalHooks.init();
        ResourceSitePlacer.init();
        FarmingHooks.init();
        RunecraftHooks.init();
        SkillingExtras.init();
        ConstructionHooks.init();
        ThievingHooks.init();
        SailingHooks.init();
        // Kharazi Jungle leafy palm trees: shake one for a palm leaf (oomlie wraps; Karamja hard diary).
        com.runecraft.content.BlockHooks.register(com.runecraft.osrs.content.skilling.CookingData.LEAFY_PALM_TREE, (player, pos, state) -> {
            if (!Inv.hasRoomFor(player, "palm_leaf")) {
                Msg.game(player, "You don't have enough inventory space.");
                return;
            }
            Msg.action(player, "You shake the tree and a palm leaf falls to the ground. You pick it up.");
            SkillingUtil.give(player, "palm_leaf", 1);
        });
        BlastFurnaceHooks.init();
        BrewingHooks.init();
        // Barbarian mixes are taught by Otto Godblessed in Barbarian Training.
        MenuService.REQUIREMENTS.add((player, recipe) -> HerbloreData.isBarbarianMix(recipe.id())
                && !com.runecraft.quest.QuestApi.isComplete(player, HerbloreData.BARBARIAN_TRAINING)
                ? "You need to learn barbarian Herblore from Otto Godblessed (Barbarian Training) to make mixes." : null);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ActionManager.mcTick() % ActionManager.MC_TICKS_PER_GAME_TICK != 0) {
                return;
            }
            long gameTick = ActionManager.gameTick();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemHooks.divineTick(player, gameTick);
                ItemHooks.prayerRegenTick(player, gameTick);
                ItemHooks.goadingTick(player, gameTick);
                SailingHooks.boatTick(player);
                BlastFurnaceHooks.tick(player, gameTick);
                AgilityHooks.wildernessTick(player, gameTick);
                if (gameTick % 20 == 0) {
                    RunecraftHooks.fermentWine(player);
                }
            }
        });

        // The Dark Mage repairs essence pouches (NPC Contact reaches him too, see the systems module).
        QuestScripts.onTalk(RunecraftData.DARK_MAGE, (player, npc) -> EssencePouches.darkMage(player));
        // Gielinor's looms (Minecraft looms in the Crafting Guild, Rimmington...) are for weaving.
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND || !world.getBlockState(hit.getBlockPos()).isOf(Blocks.LOOM) || !Gielinor.is(player)) {
                return ActionResult.PASS;
            }
            if (player instanceof ServerPlayerEntity sp) {
                MenuService.open(sp, MenuType.SPINNING, hit.getBlockPos(), false);
            }
            return ActionResult.SUCCESS;
        });

        GameEvents.onXp((player, skill, tenths) -> {
            if (skill != Skill.MINING || awarding) {
                return;
            }
            double pct = prospectorBonus(player);
            if (pct <= 0) {
                return;
            }
            double bonus = tenths * pct / 100.0 + PROSPECTOR_REMAINDER.getOrDefault(player.getUuid(), 0.0);
            int whole = (int) Math.floor(bonus);
            PROSPECTOR_REMAINDER.put(player.getUuid(), bonus - whole);
            if (whole > 0) {
                awarding = true;
                try {
                    XpService.addTenths(player, Skill.MINING, whole);
                } finally {
                    awarding = false;
                }
            }
        });
    }

    /** Empties the farming patch at {@code pos}: its saved crop and the decoration above it (for the self-test). */
    public static void clearPatch(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.BlockPos pos) {
        FarmingHooks.forget(world, pos);
    }

    /** Prospector kit Mining XP bonus in percent (0.4/0.8/0.6/0.2, +0.5 for the full set = 2.5%). */
    static double prospectorBonus(ServerPlayerEntity player) {
        double pct = 0;
        int pieces = 0;
        for (int i = 0; i < ARMOUR.length; i++) {
            ItemStack worn = player.getEquippedStack(ARMOUR[i]);
            if (!worn.isEmpty() && Inv.idOf(worn).equals(MiningData.PROSPECTOR[i])) {
                pct += MiningData.PROSPECTOR_BONUS[i];
                pieces++;
            }
        }
        return pieces == 4 ? pct + 0.5 : pct;
    }
}
