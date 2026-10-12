package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.content.GameEvents;
import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.MiningData;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
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
        FarmingHooks.init();
        RunecraftHooks.init();
        ConstructionHooks.init();
        ThievingHooks.init();
        SailingHooks.init();
        BlastFurnaceHooks.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ActionManager.mcTick() % ActionManager.MC_TICKS_PER_GAME_TICK != 0) {
                return;
            }
            long gameTick = ActionManager.gameTick();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemHooks.divineTick(player, gameTick);
                SailingHooks.boatTick(player);
                BlastFurnaceHooks.tick(player, gameTick);
                if (gameTick % 20 == 0) {
                    RunecraftHooks.fermentWine(player);
                }
            }
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
