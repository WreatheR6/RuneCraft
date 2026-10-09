package com.runecraft.osrs;

/** The Old School RuneScape combat level formula. */
public final class CombatLevel {
    private CombatLevel() {
    }

    public static double exact(int attack, int strength, int defence, int hitpoints, int prayer, int ranged, int magic) {
        double base = 0.25 * (defence + hitpoints + Math.floor(prayer / 2.0));
        double melee = 0.325 * (attack + strength);
        double range = 0.325 * Math.floor(ranged * 3 / 2.0);
        double mage = 0.325 * Math.floor(magic * 3 / 2.0);
        return base + Math.max(melee, Math.max(range, mage));
    }

    public static int of(int attack, int strength, int defence, int hitpoints, int prayer, int ranged, int magic) {
        return (int) Math.floor(exact(attack, strength, defence, hitpoints, prayer, ranged, magic));
    }

    public static int of(SkillSet skills) {
        return of(skills.level(Skill.ATTACK), skills.level(Skill.STRENGTH), skills.level(Skill.DEFENCE),
                skills.level(Skill.HITPOINTS), skills.level(Skill.PRAYER), skills.level(Skill.RANGED),
                skills.level(Skill.MAGIC));
    }
}
