package com.runecraft.osrs;

import java.util.Map;

/** Wearable item data: slot, bonuses, level requirements and (for weapons) speed and style. */
public record EquipmentDef(Slot slot, EquipmentStats stats, Map<Skill, Integer> requirements, int attackSpeed,
                           AttackType attackType, WeaponClass weaponClass) {

    public enum Slot { HEAD, CHEST, LEGS, FEET, WEAPON, SHIELD }

    public enum WeaponClass { NONE, MELEE, BOW, STAFF }

    public static EquipmentDef armour(Slot slot, EquipmentStats stats, int defenceLevel) {
        return new EquipmentDef(slot, stats, defenceLevel > 1 ? Map.of(Skill.DEFENCE, defenceLevel) : Map.of(), 0,
                AttackType.CRUSH, WeaponClass.NONE);
    }

    public static EquipmentDef melee(EquipmentStats stats, int attackLevel, int speed, AttackType type) {
        return new EquipmentDef(Slot.WEAPON, stats, attackLevel > 1 ? Map.of(Skill.ATTACK, attackLevel) : Map.of(), speed,
                type, WeaponClass.MELEE);
    }

    /** Returns the first unmet requirement as "a Defence level of 40", or null if all are met. */
    public String unmetRequirement(SkillSet skills) {
        for (Map.Entry<Skill, Integer> e : requirements.entrySet()) {
            if (skills.level(e.getKey()) < e.getValue()) {
                return "a " + e.getKey().displayName() + " level of " + e.getValue();
            }
        }
        return null;
    }
}
