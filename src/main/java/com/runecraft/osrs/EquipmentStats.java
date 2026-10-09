package com.runecraft.osrs;

/** OSRS equipment bonuses (the numbers on the "Equipment Stats" screen). */
public record EquipmentStats(
        int stabAttack, int slashAttack, int crushAttack, int magicAttack, int rangedAttack,
        int stabDefence, int slashDefence, int crushDefence, int magicDefence, int rangedDefence,
        int meleeStrength, int rangedStrength, int magicDamagePercent, int prayer) {

    public static final EquipmentStats NONE = new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public int attack(AttackType type) {
        return switch (type) {
            case STAB -> stabAttack;
            case SLASH -> slashAttack;
            case CRUSH -> crushAttack;
            case MAGIC -> magicAttack;
            case RANGED -> rangedAttack;
        };
    }

    public int defence(AttackType type) {
        return switch (type) {
            case STAB -> stabDefence;
            case SLASH -> slashDefence;
            case CRUSH -> crushDefence;
            case MAGIC -> magicDefence;
            case RANGED -> rangedDefence;
        };
    }

    public EquipmentStats plus(EquipmentStats o) {
        return new EquipmentStats(
                stabAttack + o.stabAttack, slashAttack + o.slashAttack, crushAttack + o.crushAttack,
                magicAttack + o.magicAttack, rangedAttack + o.rangedAttack,
                stabDefence + o.stabDefence, slashDefence + o.slashDefence, crushDefence + o.crushDefence,
                magicDefence + o.magicDefence, rangedDefence + o.rangedDefence,
                meleeStrength + o.meleeStrength, rangedStrength + o.rangedStrength,
                magicDamagePercent + o.magicDamagePercent, prayer + o.prayer);
    }

    public static EquipmentStats attack(int stab, int slash, int crush, int str) {
        return new EquipmentStats(stab, slash, crush, 0, 0, 0, 0, 0, 0, 0, str, 0, 0, 0);
    }

    public static EquipmentStats defence(int stab, int slash, int crush, int magic, int ranged, int magicAtk, int rangedAtk) {
        return new EquipmentStats(0, 0, 0, magicAtk, rangedAtk, stab, slash, crush, magic, ranged, 0, 0, 0, 0);
    }
}
