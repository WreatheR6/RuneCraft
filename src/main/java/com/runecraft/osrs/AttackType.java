package com.runecraft.osrs;

/** The five OSRS attack/defence bonus categories. */
public enum AttackType {
    STAB, SLASH, CRUSH, MAGIC, RANGED;

    public boolean isMelee() {
        return this == STAB || this == SLASH || this == CRUSH;
    }
}
