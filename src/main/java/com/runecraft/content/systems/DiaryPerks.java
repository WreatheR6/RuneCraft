package com.runecraft.content.systems;

import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.DiaryRewards;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Passive achievement diary reward effects: Varrock armour's extra ore (mining) and extra bar (smelting),
 * and Rada's blessing's extra fish in Kourend & Kebos. A gathered item is remembered until the skill's XP
 * drop of the same action arrives, so drops and rewards never trigger the bonus.
 */
final class DiaryPerks {
    private record Pending(String item, long tick) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private DiaryPerks() {
    }

    static void onObtain(ServerPlayerEntity p, String item) {
        PENDING.put(p.getUuid(), new Pending(item, SysTicks.now()));
    }

    private static boolean inBonusXp;

    /** Falador medium diary: 10% more Farming XP at the Falador farm patches (paid as a separate XP drop). */
    static void farmXp(ServerPlayerEntity p, Skill skill, int tenths) {
        if (skill != Skill.FARMING || inBonusXp || tenths <= 0 || !com.runecraft.world.Gielinor.is(p)) {
            return;
        }
        int[] at = SysTeleport.osrs(p);
        double mult = DiaryRewards.faladorFarmXp(
                com.runecraft.osrs.content.systems.Diaries.tierComplete(SysState.of(p).diaryDone, "falador",
                        com.runecraft.osrs.content.systems.Diaries.Tier.MEDIUM), at[0], at[1]);
        int bonus = (int) Math.round(tenths * (mult - 1.0));
        if (bonus <= 0) {
            return;
        }
        inBonusXp = true;
        try {
            com.runecraft.skill.XpService.addTenths(p, Skill.FARMING, bonus);
        } finally {
            inBonusXp = false;
        }
    }

    static void onXp(ServerPlayerEntity p, Skill skill) {
        if (skill != Skill.MINING && skill != Skill.FISHING) {
            return;
        }
        Pending pending = PENDING.remove(p.getUuid());
        if (pending == null || SysTicks.now() - pending.tick() > 1) {
            return;
        }
        if (skill == Skill.MINING) {
            int tier = DiaryRewards.tierOf(Inv.idOf(p.getEquippedStack(EquipmentSlot.CHEST)));
            if (Inv.idOf(p.getEquippedStack(EquipmentSlot.CHEST)).startsWith("varrock_armour_") && DiaryRewards.varrockOre(tier, pending.item())
                    && p.getRandom().nextInt(100) < DiaryRewards.VARROCK_BONUS_PERCENT) {
                Inv.give(p, pending.item(), 1);
                Msg.game(p, "Your Varrock armour allows you to mine an additional ore.");
            }
            return;
        }
        String ammo = SysState.of(p).wornId(Jewellery.Slot.AMMO);
        if (ammo.startsWith("radas_blessing_") && pending.item().startsWith("raw_") && DiaryService.inArea(p, "kourend")
                && p.getRandom().nextInt(100) < DiaryRewards.radasExtraFishPercent(DiaryRewards.tierOf(ammo))) {
            Inv.give(p, pending.item(), 1);
            Msg.game(p, "Rada's blessing grants you an extra fish.");
        }
    }

    static void onMake(ServerPlayerEntity p, Recipe r) {
        String chest = Inv.idOf(p.getEquippedStack(EquipmentSlot.CHEST));
        if (!chest.startsWith("varrock_armour_")) {
            return;
        }
        int tier = DiaryRewards.tierOf(chest);
        for (Map.Entry<String, Integer> out : r.outputs().entrySet()) {
            if (DiaryRewards.varrockBar(tier, out.getKey()) && r.skill() == Skill.SMITHING
                    && p.getRandom().nextInt(100) < DiaryRewards.VARROCK_BONUS_PERCENT) {
                Inv.give(p, out.getKey(), 1);
                Msg.game(p, "Your Varrock armour allows you to smelt an additional bar.");
            }
        }
    }
}
