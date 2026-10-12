package com.runecraft.osrs.data;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentStats;

/** Smithable melee weapon types. Rune and dragon stats are the in-game values. */
public enum WeaponType {
    DAGGER("dagger", "dagger", AttackType.STAB, 4, 1, 0, 0.5,
            EquipmentStats.attack(25, 25, -4, 24), EquipmentStats.attack(40, 25, -4, 40)),
    SCIMITAR("scimitar", "scimitar", AttackType.SLASH, 4, 2, 5, 1.0,
            EquipmentStats.attack(7, 45, -2, 44), EquipmentStats.attack(8, 67, -2, 66)),
    LONGSWORD("longsword", "longsword", AttackType.SLASH, 5, 2, 6, 1.25,
            EquipmentStats.attack(38, 47, -2, 49), EquipmentStats.attack(58, 69, -2, 71)),
    WARHAMMER("warhammer", "warhammer", AttackType.CRUSH, 6, 3, 9, 1.3,
            EquipmentStats.attack(-4, -4, 53, 48), EquipmentStats.attack(-4, -4, 95, 85)),
    BATTLEAXE("battleaxe", "battleaxe", AttackType.SLASH, 6, 3, 10, 1.6,
            EquipmentStats.attack(-2, 48, 43, 64), EquipmentStats.attack(-2, 70, 65, 85)),
    TWO_HANDED("2h_sword", "2h sword", AttackType.SLASH, 7, 3, 14, 2.0,
            EquipmentStats.attack(-4, 69, 50, 70), EquipmentStats.attack(-4, 92, 80, 93));

    private final String idSuffix;
    private final String nameSuffix;
    private final AttackType attackType;
    private final int speed;
    private final int bars;
    private final int levelOffset;
    private final double valueFactor;
    private final EquipmentStats rune;
    private final EquipmentStats dragon;

    WeaponType(String idSuffix, String nameSuffix, AttackType attackType, int speed, int bars, int levelOffset,
               double valueFactor, EquipmentStats rune, EquipmentStats dragon) {
        this.idSuffix = idSuffix;
        this.nameSuffix = nameSuffix;
        this.attackType = attackType;
        this.speed = speed;
        this.bars = bars;
        this.levelOffset = levelOffset;
        this.valueFactor = valueFactor;
        this.rune = rune;
        this.dragon = dragon;
    }

    public String itemId(MetalTier tier) {
        return tier.id() + "_" + idSuffix;
    }

    public String displayName(MetalTier tier) {
        return tier.displayName() + " " + nameSuffix;
    }

    public AttackType attackType() {
        return attackType;
    }

    /** Attack speed in game ticks (0.6s each). */
    public int speed() {
        return speed;
    }

    public int bars() {
        return bars;
    }

    public int smithingLevel(MetalTier tier) {
        return tier.smithingLevel(levelOffset);
    }

    public double valueFactor() {
        return valueFactor;
    }

    public String shape() {
        return idSuffix;
    }

    public EquipmentStats stats(MetalTier tier) {
        if (tier == MetalTier.RUNE) {
            return rune;
        }
        if (tier == MetalTier.DRAGON) {
            return dragon;
        }
        return Scaling.scale(rune, tier.statFactor());
    }
}
