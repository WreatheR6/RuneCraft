package com.runecraft.combat;

import com.runecraft.content.systems.SysCombat;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatBonuses;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Equipment set effects and prayer damage bonuses that change accuracy and max hit: salve amulets vs undead,
 * Dharok's set, the berserker necklace with obsidian weapons, crystal armour with crystal bows, the twisted bow
 * and the magic damage of Mystic Lore / Mystic Might / Augury. (Void and the obsidian armour set live in the
 * minigames module, the black mask / slayer helmet in the slayer module.)
 */
public final class GearEffects implements CombatHooks.PlayerAttackModifier {
    private static final double EPS = 1e-9;

    static String worn(ServerPlayerEntity p, EquipmentSlot slot) {
        return Inv.idOf(p.getEquippedStack(slot));
    }

    static String neck(ServerPlayerEntity p) {
        return SysState.of(p).wornId(Jewellery.Slot.NECK);
    }

    /** Salve multiplier for this attack (1.0 when it does not apply). */
    public static double salve(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        if (target == null || target instanceof ServerPlayerEntity) {
            return 1.0;
        }
        double m = CombatBonuses.salve(neck(p), type);
        return m > 1.0 && SysCombat.isUndead(target) ? m : 1.0;
    }

    /** True when a salve amulet boosts this attack (the black mask / slayer helmet bonus is then skipped). */
    public static boolean salveActive(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        return salve(p, target, type) > 1.0;
    }

    static boolean dharoks(ServerPlayerEntity p) {
        return worn(p, EquipmentSlot.HEAD).equals("dharoks_helm") && worn(p, EquipmentSlot.CHEST).equals("dharoks_platebody")
                && worn(p, EquipmentSlot.LEGS).equals("dharoks_platelegs") && Inv.idOf(p.getMainHandStack()).equals("dharoks_greataxe");
    }

    static boolean obsidianArmour(ServerPlayerEntity p) {
        return worn(p, EquipmentSlot.HEAD).equals("obsidian_helmet") && worn(p, EquipmentSlot.CHEST).equals("obsidian_platebody")
                && worn(p, EquipmentSlot.LEGS).equals("obsidian_platelegs");
    }

    /** Magic level (or magic attack, whichever is higher) the twisted bow scales with. */
    static int targetMagic(LivingEntity target) {
        if (target instanceof ServerPlayerEntity other) {
            return Players.data(other).skills().currentLevel(Skill.MAGIC);
        }
        return MonsterStats.of(target).magic();
    }

    static boolean crystal(ServerPlayerEntity p) {
        return CombatBonuses.isCrystalBow(Inv.idOf(p.getMainHandStack()));
    }

    @Override
    public double accuracy(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        double m = salve(p, target, type);
        if (type == AttackType.RANGED) {
            String weapon = Inv.idOf(p.getMainHandStack());
            if (weapon.equals("twisted_bow")) {
                m *= CombatBonuses.twistedBowAccuracy(CombatBonuses.twistedBowMagic(targetMagic(target), 0, false)) / 100.0;
            } else if (crystal(p)) {
                m *= CombatBonuses.crystalAccuracy(worn(p, EquipmentSlot.HEAD).equals("crystal_helm"),
                        worn(p, EquipmentSlot.CHEST).equals("crystal_body"), worn(p, EquipmentSlot.LEGS).equals("crystal_legs"));
            }
        }
        return m;
    }

    @Override
    public double maxHit(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        double m = salve(p, target, type);
        if (type.isMelee()) {
            if (dharoks(p)) {
                int max = Math.max(1, Math.round(p.getMaxHealth()));
                int hp = Math.max(0, (int) Math.ceil(p.getHealth()));
                m *= CombatBonuses.dharok(max, hp);
            }
            if (neck(p).startsWith("berserker_necklace") && CombatBonuses.TZHAAR_MELEE.contains(Inv.idOf(p.getMainHandStack()))) {
                m *= CombatBonuses.berserkerOnTop(obsidianArmour(p));
            }
        } else if (type == AttackType.RANGED) {
            String weapon = Inv.idOf(p.getMainHandStack());
            if (weapon.equals("twisted_bow")) {
                m *= CombatBonuses.twistedBowDamage(CombatBonuses.twistedBowMagic(targetMagic(target), 0, false)) / 100.0;
            } else if (crystal(p)) {
                m *= CombatBonuses.crystalDamage(worn(p, EquipmentSlot.HEAD).equals("crystal_helm"),
                        worn(p, EquipmentSlot.CHEST).equals("crystal_body"), worn(p, EquipmentSlot.LEGS).equals("crystal_legs"));
            }
        } else if (type == AttackType.MAGIC) {
            PlayerData d = Players.data(p);
            int pct = 0;
            for (Prayer prayer : d.activePrayers()) {
                pct = Math.max(pct, prayer.magicDamagePercent());
            }
            m *= 1.0 + pct / 100.0;
        }
        return m == 1.0 ? 1.0 : m + EPS;
    }
}
