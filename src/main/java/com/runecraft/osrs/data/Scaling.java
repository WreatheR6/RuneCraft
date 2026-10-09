package com.runecraft.osrs.data;

import com.runecraft.osrs.EquipmentStats;

final class Scaling {
    private Scaling() {
    }

    /** Scales positive bonuses by the tier factor; penalties (negative values) are kept as-is like in-game. */
    static EquipmentStats scale(EquipmentStats s, double f) {
        return new EquipmentStats(
                sc(s.stabAttack(), f), sc(s.slashAttack(), f), sc(s.crushAttack(), f), s.magicAttack(), s.rangedAttack(),
                sc(s.stabDefence(), f), sc(s.slashDefence(), f), sc(s.crushDefence(), f), s.magicDefence(), sc(s.rangedDefence(), f),
                sc(s.meleeStrength(), f), sc(s.rangedStrength(), f), s.magicDamagePercent(), s.prayer());
    }

    private static int sc(int v, double f) {
        return v > 0 ? Math.max(1, (int) Math.round(v * f)) : v;
    }
}
