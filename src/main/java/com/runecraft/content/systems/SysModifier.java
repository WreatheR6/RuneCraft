package com.runecraft.content.systems;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.PrayerService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Skill;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The systems module's attack modifier: bonuses of items worn in the extra slots (converted exactly into
 * accuracy/max-hit multipliers), defence drained by specials/spells, and special attacks.
 */
public final class SysModifier implements CombatHooks.PlayerAttackModifier {
    private static final double EPS = 1e-9;

    /** Accuracy multiplier equivalent to adding the worn items' attack bonus to the equipment total. */
    public static double wornAccuracyRatio(ServerPlayerEntity p, AttackType type) {
        EquipmentStats worn = SysCombat.wornStats(p);
        int add = worn.attack(type);
        if (add == 0) {
            return 1.0;
        }
        int base = SysCombat.coreStats(p).attack(type) + 64;
        if (base <= 0) {
            return 1.0;
        }
        return Math.max(0.0, (base + add) / (double) base);
    }

    /** Max-hit multiplier equivalent to adding the worn items' strength bonus. */
    public static double wornMaxRatio(ServerPlayerEntity p, AttackType type) {
        EquipmentStats worn = SysCombat.wornStats(p);
        EquipmentStats core = SysCombat.coreStats(p);
        PlayerData d = Players.data(p);
        if (type.isMelee()) {
            if (worn.meleeStrength() == 0) {
                return 1.0;
            }
            int eff = CombatFormulas.effectiveStrength(d.skills().currentLevel(Skill.STRENGTH), PrayerService.strength(d), d.style().forMelee());
            int before = CombatFormulas.meleeMaxHit(eff, core.meleeStrength());
            int after = CombatFormulas.meleeMaxHit(eff, core.meleeStrength() + worn.meleeStrength());
            return before <= 0 ? 1.0 : after / (double) before + EPS;
        }
        if (type == AttackType.RANGED) {
            if (worn.rangedStrength() == 0) {
                return 1.0;
            }
            int ammo = SysCombat.ammoStrength(p);
            int eff = CombatFormulas.effectiveRanged(d.skills().currentLevel(Skill.RANGED), PrayerService.rangedStrength(d), d.style().forRanged());
            int before = CombatFormulas.rangedMaxHit(eff, ammo + core.rangedStrength());
            int after = CombatFormulas.rangedMaxHit(eff, ammo + core.rangedStrength() + worn.rangedStrength());
            return before <= 0 ? 1.0 : after / (double) before + EPS;
        }
        if (worn.magicDamagePercent() == 0) {
            return 1.0;
        }
        return (1.0 + (core.magicDamagePercent() + worn.magicDamagePercent()) / 100.0) / (1.0 + core.magicDamagePercent() / 100.0) + EPS;
    }

    @Override
    public double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        double m = wornAccuracyRatio(player, type) * SysCombat.drainAccuracy(target, type);
        return m * SpecialAttackService.accuracy(player, target, type);
    }

    @Override
    public double maxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        return wornMaxRatio(player, type) * SpecialAttackService.maxHit(player, target, type);
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
    }
}
