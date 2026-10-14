package com.runecraft.combat;

import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.data.Monster;
import com.runecraft.world.Gielinor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;

/** OSRS combat stats for any living entity: from a {@link Monster} definition or derived from vanilla attributes. */
public record MonsterStats(String name, int combatLevel, int hitpoints, int attack, int strength, int defence,
                           int magic, int ranged, int defenceBonus, int attackSpeed, AttackType style, int maxHit,
                           Monster monster, com.runecraft.osrs.content.CombatProfile<?> profile,
                           com.runecraft.osrs.data.Boss boss) {

    public MonsterStats(String name, int combatLevel, int hitpoints, int attack, int strength, int defence, int magic,
                        int ranged, int defenceBonus, int attackSpeed, AttackType style, int maxHit, Monster monster,
                        com.runecraft.osrs.content.CombatProfile<?> profile) {
        this(name, combatLevel, hitpoints, attack, strength, defence, magic, ranged, defenceBonus, attackSpeed, style, maxHit,
                monster, profile, null);
    }

    /** OSRS monster attack bonus for a style, added to the NPC attack roll (0 unless the data sets one). */
    public int attackBonus(AttackType type) {
        if (profile != null) {
            return profile.attackBonus(type);
        }
        return boss != null ? boss.attackBonus(type) : 0;
    }

    /** Defence bonus against a specific style (module creatures have per-style bonuses). */
    public int defenceBonus(AttackType type) {
        return profile != null ? profile.defenceBonus(type) : defenceBonus;
    }

    /** {light, standard, heavy} ranged defence (module creatures), or null when only one ranged defence is known. */
    public int[] rangedDefences() {
        if (profile == null) {
            return null;
        }
        int standard = profile.defenceBonus(AttackType.RANGED);
        return new int[]{profile.lightRangedDefence != null ? profile.lightRangedDefence : standard, standard,
                profile.heavyRangedDefence != null ? profile.heavyRangedDefence : standard};
    }

    /** Elemental weakness ("fire", "water", "earth", "air") or null. */
    public String weakness() {
        return profile != null ? profile.weakness : null;
    }

    /** Elemental weakness severity in percent (0 for none). */
    public int weaknessPercent() {
        return profile != null ? profile.weaknessPercent : 0;
    }

    public static MonsterStats from(com.runecraft.osrs.content.CombatProfile<?> p) {
        return new MonsterStats(p.name, p.combatLevel, p.hitpoints, p.attack, p.strength, p.defence, p.magic, p.ranged,
                0, p.attackSpeed, p.primaryStyle(), p.maxHit(), null, p);
    }

    public static MonsterStats of(LivingEntity entity) {
        if (entity instanceof OsrsMonsterEntity custom) {
            return custom.stats();
        }
        Monster m = Monster.byEntity(Registries.ENTITY_TYPE.getId(entity.getType()).toString());
        if (m != null && Gielinor.is(entity)) {
            return from(m);
        }
        // Derive something sensible from vanilla attributes.
        int hp = Math.max(1, Math.round(entity.getMaxHealth()));
        double dmg = entity.getAttributes().hasAttribute(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                ? entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) : 1.0;
        int level = Math.max(1, hp / 2 + (int) dmg * 2);
        return new MonsterStats(entity.getName().getString(), level, hp, level, level, level, 1, 1, 0, 4,
                AttackType.CRUSH, Math.max(1, (int) Math.round(dmg)), m, null);
    }

    public static MonsterStats from(Monster m) {
        return new MonsterStats(m.displayName(), m.combatLevel(), m.hitpoints(), m.attack(), m.strength(), m.defence(),
                m.magic(), m.ranged(), m.defenceBonus(), m.attackSpeed(), m.style(), m.maxHit(), m, null);
    }
}
