package com.runecraft.osrs.content;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared combat data for module monsters and bosses. */
public abstract class CombatProfile<T extends CombatProfile<T>> {
    public final String id;
    public final String name;
    public int combatLevel = 1;
    public int hitpoints = 10;
    public int attack = 1;
    public int strength = 1;
    public int defence = 1;
    public int magic = 1;
    public int ranged = 1;
    public int attackSpeed = 4;
    public final Map<AttackType, Integer> defenceBonus = new EnumMap<>(AttackType.class);
    public final List<AttackDef> attacks = new ArrayList<>();
    public final List<Drop> drops = new ArrayList<>();
    /** Attack styles this creature takes no damage from. */
    public final Set<AttackType> immuneTo = EnumSet.noneOf(AttackType.class);
    /** Custom model id (a ModelSpec in some module), or null to use the biped model. */
    public String model;
    /** Biped skin colours (skin, top, bottom, accent) for generated textures when there's no custom model. */
    public int[] skin = {0xC8A080, 0x6A4A2A, 0x4A3A2A, 0x202020};
    public float scale = 1.0f;
    public float width = 0.6f;
    public float height = 1.95f;
    public double moveSpeed = 0.25;
    public int slayerLevel = 1;
    public String slayerCategory;
    /** Item that must be wielded to damage it (e.g. "leaf_bladed_sword"), or null. */
    public String requiredWeapon;
    public boolean aggressive = true;
    public boolean flying;
    public boolean poisonous;

    protected CombatProfile(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    public T level(int combatLevel) {
        this.combatLevel = combatLevel;
        return self();
    }

    public T stats(int hitpoints, int attack, int strength, int defence, int magic, int ranged) {
        this.hitpoints = hitpoints;
        this.attack = attack;
        this.strength = strength;
        this.defence = defence;
        this.magic = magic;
        this.ranged = ranged;
        return self();
    }

    public T speed(int ticks) {
        this.attackSpeed = ticks;
        return self();
    }

    /** Defence bonuses: stab, slash, crush, magic, ranged. */
    public T defenceBonus(int stab, int slash, int crush, int magic, int ranged) {
        defenceBonus.put(AttackType.STAB, stab);
        defenceBonus.put(AttackType.SLASH, slash);
        defenceBonus.put(AttackType.CRUSH, crush);
        defenceBonus.put(AttackType.MAGIC, magic);
        defenceBonus.put(AttackType.RANGED, ranged);
        return self();
    }

    public T attack(AttackDef attack) {
        attacks.add(attack);
        return self();
    }

    public T drop(Drop drop) {
        drops.add(drop);
        return self();
    }

    public T drops(Drop... list) {
        drops.addAll(List.of(list));
        return self();
    }

    public T immune(AttackType... types) {
        immuneTo.addAll(List.of(types));
        return self();
    }

    public T model(String model) {
        this.model = model;
        return self();
    }

    public T skin(int skin, int top, int bottom, int accent) {
        this.skin = new int[]{skin, top, bottom, accent};
        return self();
    }

    /** Hitbox size in blocks (before scale) and render scale. */
    public T size(float width, float height, float scale) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        return self();
    }

    public T moveSpeed(double speed) {
        this.moveSpeed = speed;
        return self();
    }

    public T slayer(int level, String category) {
        this.slayerLevel = level;
        this.slayerCategory = category;
        return self();
    }

    public T requiresWeapon(String item) {
        this.requiredWeapon = item;
        return self();
    }

    public T passive() {
        this.aggressive = false;
        return self();
    }

    public T flying() {
        this.flying = true;
        return self();
    }

    public T poisonous() {
        this.poisonous = true;
        return self();
    }

    public int defenceBonus(AttackType type) {
        return defenceBonus.getOrDefault(type, 0);
    }

    /** Max hit of the strongest attack (used for NPC displays and defaults). */
    public int maxHit() {
        int max = 1;
        for (AttackDef a : attacks) {
            max = Math.max(max, a.maxHit());
        }
        return max;
    }

    /** Primary style (style of the first attack). */
    public AttackType primaryStyle() {
        return attacks.isEmpty() ? AttackType.CRUSH : attacks.get(0).style();
    }

    public boolean isAggressiveTo(int playerCombatLevel) {
        return aggressive && com.runecraft.osrs.CombatBonuses.aggressiveTo(combatLevel, playerCombatLevel);
    }
}
