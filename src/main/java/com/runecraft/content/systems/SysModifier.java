package com.runecraft.content.systems;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.AttackType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The systems module's attack modifier: defence drained by specials/spells, and special attacks. (Items worn in the extra
 * slots are part of the gear totals {@link com.runecraft.combat.HitCalcBridge} feeds PlayerHitCalc.)
 */
public final class SysModifier implements CombatHooks.PlayerAttackModifier {
    @Override
    public double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        // Items in the extra slots are part of the gear totals HitCalcBridge feeds PlayerHitCalc (and of SysCombat's rolls); stat
        // drains lower the target's levels in HitCalcBridge's input, so they are not an accuracy multiplier any more.
        return SpecialAttackService.accuracy(player, target, type);
    }

    @Override
    public double maxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        return SpecialAttackService.maxHit(player, target, type);
    }

    @Override
    public int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
        return SpecialAttackService.afterRoll(player, target, type, damage);
    }

    @Override
    public void afterHit(ServerPlayerEntity player, LivingEntity target, AttackType type, int dealt) {
        SysState.SysPlayer sp = SysState.of(player);
        sp.lastCombatTick = ActionManager.gameTick();
        sp.lastTargetId = target.getId();
        SpecialAttackService.afterHit(player, target, type, dealt);
        SpellService.corruptOnHit(player, target, dealt);
    }
}
