package com.runecraft.osrs.data;

import com.runecraft.osrs.EquipmentStats;

/** Smithable armour pieces. Rune and dragon stats are the in-game values. */
public enum ArmourType {
    FULL_HELM("full_helm", "full helm", Slot.HEAD, 2, 7, 1.1,
            EquipmentStats.defence(30, 32, 27, -1, 30, -6, -2), EquipmentStats.defence(45, 48, 41, -1, 46, -6, -2)),
    PLATEBODY("platebody", "platebody", Slot.CHEST, 5, 18, 2.6,
            EquipmentStats.defence(82, 80, 72, -6, 80, -30, -10), EquipmentStats.defence(109, 107, 97, -6, 106, -30, -10)),
    PLATELEGS("platelegs", "platelegs", Slot.LEGS, 3, 16, 1.6,
            EquipmentStats.defence(51, 49, 47, -4, 49, -21, -11), EquipmentStats.defence(68, 66, 63, -4, 66, -21, -11)),
    KITESHIELD("kiteshield", "kiteshield", Slot.SHIELD, 3, 12, 1.7,
            EquipmentStats.defence(44, 48, 46, -1, 46, -8, -2), EquipmentStats.defence(63, 65, 63, -1, 65, -8, -2));

    public enum Slot { HEAD, CHEST, LEGS, FEET, SHIELD }

    private final String idSuffix;
    private final String nameSuffix;
    private final Slot slot;
    private final int bars;
    private final int levelOffset;
    private final double valueFactor;
    private final EquipmentStats rune;
    private final EquipmentStats dragon;

    ArmourType(String idSuffix, String nameSuffix, Slot slot, int bars, int levelOffset, double valueFactor,
               EquipmentStats rune, EquipmentStats dragon) {
        this.idSuffix = idSuffix;
        this.nameSuffix = nameSuffix;
        this.slot = slot;
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

    public Slot slot() {
        return slot;
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
