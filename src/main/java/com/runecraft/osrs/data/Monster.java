package com.runecraft.osrs.data;

import com.runecraft.osrs.AttackType;

import java.util.List;

/**
 * Monster definitions using OSRS combat stats. {@code entity} is the Minecraft entity id the stats
 * apply to: either a RuneCraft creature or a vanilla mob (vanilla mobs only use these stats in
 * the Gielinor dimension). Speeds are in game ticks.
 */
public enum Monster {
    // RuneCraft creatures (biped models with their own textures)
    MAN("man", "Man", 2, 7, 1, 1, 1, 1, 1, 4, AttackType.CRUSH, 0, false, 0, 0xC8A080,
            List.of(Drop.always("bones"), Drop.of("coins", 1, 15, 3), Drop.seeds(40), Drop.herbs(60))),
    GOBLIN("goblin", "Goblin", 2, 5, 1, 1, 1, 1, 1, 4, AttackType.CRUSH, -15, true, 0, 0x6A9A3A,
            List.of(Drop.always("bones"), Drop.of("coins", 1, 9, 4), Drop.of("bronze_scimitar", 1, 1, 64),
                    Drop.of("air_rune", 4, 8, 16), Drop.of("water_rune", 4, 8, 16), Drop.herbs(90))),
    AL_KHARID_WARRIOR("al_kharid_warrior", "Al-Kharid warrior", 9, 19, 7, 5, 4, 1, 1, 4, AttackType.SLASH, 0, false, 0, 0xD0C080,
            List.of(Drop.always("bones"), Drop.of("coins", 5, 30, 3), Drop.of("iron_scimitar", 1, 1, 80), Drop.herbs(30))),
    BARBARIAN("barbarian", "Barbarian", 10, 18, 8, 8, 6, 1, 1, 5, AttackType.CRUSH, 0, false, 0, 0xB08060,
            List.of(Drop.always("bones"), Drop.of("coins", 5, 25, 4), Drop.of("iron_battleaxe", 1, 1, 60),
                    Drop.of("fire_rune", 3, 6, 15), Drop.herbs(40))),
    DARK_WIZARD("dark_wizard", "Dark wizard", 7, 12, 5, 5, 4, 12, 1, 4, AttackType.MAGIC, 0, true, 0, 0x2A2A4A,
            List.of(Drop.always("bones"), Drop.of("mind_rune", 3, 10, 4), Drop.of("chaos_rune", 1, 5, 8),
                    Drop.of("body_rune", 2, 8, 8), Drop.of("nature_rune", 1, 3, 25), Drop.of("staff", 1, 1, 50), Drop.herbs(40))),
    GUARD("guard", "Guard", 21, 22, 19, 18, 14, 1, 1, 4, AttackType.STAB, 18, false, 0, 0x8A8A9A,
            List.of(Drop.always("bones"), Drop.of("coins", 10, 40, 3), Drop.of("iron_dagger", 1, 1, 30),
                    Drop.of("steel_arrow", 2, 8, 20), Drop.herbs(30), Drop.seeds(30))),
    HILL_GIANT("hill_giant", "Hill Giant", 28, 35, 18, 22, 26, 1, 1, 6, AttackType.CRUSH, 0, true, 0, 0xC0A080,
            List.of(Drop.always("big_bones"), Drop.of("coins", 5, 60, 3), Drop.of("limpwurt_root", 1, 1, 12),
                    Drop.of("steel_longsword", 1, 1, 64), Drop.of("law_rune", 2, 2, 25), Drop.herbs(18), Drop.gems(128))),
    MOSS_GIANT("moss_giant", "Moss giant", 42, 60, 30, 30, 30, 1, 1, 6, AttackType.CRUSH, 0, true, 0, 0x5A7A3A,
            List.of(Drop.always("big_bones"), Drop.of("coins", 10, 200, 3), Drop.of("nature_rune", 3, 6, 12),
                    Drop.of("cosmic_rune", 2, 4, 16), Drop.of("mithril_scimitar", 1, 1, 128), Drop.herbs(16), Drop.gems(64), Drop.seeds(25))),
    GHOUL("ghoul", "Ghoul", 42, 50, 40, 40, 40, 1, 1, 4, AttackType.CRUSH, 0, true, 0, 0x7A8A7A,
            List.of(Drop.always("bones"), Drop.of("coins", 10, 100, 3), Drop.herbs(20))),
    ICE_GIANT("ice_giant", "Ice giant", 53, 70, 40, 40, 40, 1, 1, 6, AttackType.CRUSH, 0, true, 0, 0x90B8D8,
            List.of(Drop.always("big_bones"), Drop.of("coins", 10, 300, 3), Drop.of("adamant_arrow", 5, 10, 25),
                    Drop.of("mithril_ore", 1, 1, 20), Drop.herbs(16), Drop.gems(64))),
    LESSER_DEMON("lesser_demon", "Lesser demon", 82, 79, 68, 67, 71, 1, 1, 4, AttackType.SLASH, 0, true, 0, 0xA02020,
            List.of(Drop.always("bones"), Drop.of("coins", 50, 450, 3), Drop.of("fire_rune", 30, 60, 8),
                    Drop.of("death_rune", 3, 6, 25), Drop.of("rune_full_helm", 1, 1, 128), Drop.of("mithril_ore", 1, 1, 16),
                    Drop.herbs(12), Drop.gems(32))),
    GREATER_DEMON("greater_demon", "Greater demon", 92, 87, 76, 78, 81, 1, 1, 4, AttackType.SLASH, 0, true, 0, 0x801818,
            List.of(Drop.always("bones"), Drop.of("coins", 100, 1300, 3), Drop.of("fire_rune", 40, 80, 8),
                    Drop.of("chaos_rune", 10, 20, 16), Drop.of("rune_full_helm", 1, 1, 64), Drop.of("adamant_platelegs", 1, 1, 80),
                    Drop.herbs(10), Drop.gems(32))),
    BLACK_DEMON("black_demon", "Black demon", 172, 157, 145, 148, 152, 1, 1, 4, AttackType.SLASH, 0, true, 0, 0x202020,
            List.of(Drop.always("bones"), Drop.of("coins", 100, 2500, 3), Drop.of("blood_rune", 7, 7, 16),
                    Drop.of("rune_platelegs", 1, 1, 128), Drop.of("adamantite_bar", 2, 2, 20), Drop.herbs(8), Drop.gems(16))),
    // Vanilla mobs (stats used inside Gielinor)
    CHICKEN("minecraft:chicken", "Chicken", 1, 3, 1, 1, 1, 1, 1, 4, AttackType.CRUSH, -42, false, 0, 0,
            List.of(Drop.always("bones"), Drop.of("minecraft:feather", 5, 15, 2))),
    COW("minecraft:cow", "Cow", 2, 8, 1, 1, 1, 1, 1, 4, AttackType.CRUSH, -21, false, 0, 0,
            List.of(Drop.always("bones"), Drop.always("cowhide"))),
    SHEEP("minecraft:sheep", "Sheep", 0, 5, 1, 1, 1, 1, 1, 4, AttackType.CRUSH, 0, false, 0, 0,
            List.of(Drop.always("bones"))),
    PIG("minecraft:pig", "Pig", 1, 6, 1, 1, 1, 1, 1, 4, AttackType.CRUSH, 0, false, 0, 0,
            List.of(Drop.always("bones"))),
    GIANT_RAT("minecraft:silverfish", "Giant rat", 3, 5, 2, 3, 2, 1, 1, 4, AttackType.STAB, 0, true, 0, 0,
            List.of(Drop.always("bones"))),
    SPIDER("minecraft:cave_spider", "Spider", 1, 2, 1, 1, 1, 1, 1, 4, AttackType.STAB, 0, false, 0, 0,
            List.of()),
    GIANT_SPIDER("minecraft:spider", "Giant spider", 27, 33, 20, 20, 20, 1, 1, 4, AttackType.STAB, 0, true, 0, 0,
            List.of(Drop.of("red_spiders_eggs", 1, 1, 8), Drop.herbs(40))),
    ZOMBIE("minecraft:zombie", "Zombie", 13, 22, 10, 10, 10, 1, 1, 4, AttackType.CRUSH, 0, true, 0, 0,
            List.of(Drop.always("bones"), Drop.of("coins", 1, 20, 4), Drop.herbs(40), Drop.of("eye_of_newt", 1, 1, 16))),
    SKELETON("minecraft:skeleton", "Skeleton", 22, 24, 17, 17, 17, 1, 17, 4, AttackType.RANGED, 0, true, 0, 0,
            List.of(Drop.always("bones"), Drop.of("coins", 2, 30, 4), Drop.of("iron_arrow", 2, 10, 6), Drop.herbs(40))),
    WOLF("minecraft:wolf", "Wolf", 25, 34, 20, 20, 20, 1, 1, 4, AttackType.STAB, 0, false, 0, 0,
            List.of(Drop.always("bones"), Drop.of("grey_wolf_fur", 1, 1, 4))),
    VILLAGER("minecraft:villager", "Man", 2, 7, 1, 1, 1, 1, 1, 4, AttackType.CRUSH, 0, false, 0, 0,
            List.of(Drop.always("bones"), Drop.of("coins", 1, 15, 3))),
    WITCH("minecraft:witch", "Dark wizard", 20, 24, 15, 15, 15, 20, 1, 4, AttackType.MAGIC, 0, true, 0, 0,
            List.of(Drop.always("bones"), Drop.of("chaos_rune", 2, 6, 6), Drop.of("eye_of_newt", 1, 2, 4), Drop.herbs(20)));

    private final String entity;
    private final String name;
    private final int combatLevel;
    private final int hitpoints;
    private final int attack;
    private final int strength;
    private final int defence;
    private final int magic;
    private final int ranged;
    private final int attackSpeed;
    private final AttackType style;
    private final int defenceBonus;
    private final boolean aggressive;
    private final int slayerLevel;
    private final int color;
    private final List<Drop> drops;

    Monster(String entity, String name, int combatLevel, int hitpoints, int attack, int strength, int defence,
            int magic, int ranged, int attackSpeed, AttackType style, int defenceBonus, boolean aggressive,
            int slayerLevel, int color, List<Drop> drops) {
        this.entity = entity;
        this.name = name;
        this.combatLevel = combatLevel;
        this.hitpoints = hitpoints;
        this.attack = attack;
        this.strength = strength;
        this.defence = defence;
        this.magic = magic;
        this.ranged = ranged;
        this.attackSpeed = attackSpeed;
        this.style = style;
        this.defenceBonus = defenceBonus;
        this.aggressive = aggressive;
        this.slayerLevel = slayerLevel;
        this.color = color;
        this.drops = drops;
    }

    /** Entity id: unqualified ids are RuneCraft creatures, "minecraft:" ids are vanilla mobs. */
    public String entity() {
        return entity;
    }

    public boolean isCustom() {
        return !entity.contains(":");
    }

    public String displayName() {
        return name;
    }

    public int combatLevel() {
        return combatLevel;
    }

    public int hitpoints() {
        return hitpoints;
    }

    public int attack() {
        return attack;
    }

    public int strength() {
        return strength;
    }

    public int defence() {
        return defence;
    }

    public int magic() {
        return magic;
    }

    public int ranged() {
        return ranged;
    }

    public int attackSpeed() {
        return attackSpeed;
    }

    public AttackType style() {
        return style;
    }

    /** Flat defence bonus applied against every attack style. */
    public int defenceBonus() {
        return defenceBonus;
    }

    public boolean aggressive() {
        return aggressive;
    }

    public int slayerLevel() {
        return slayerLevel;
    }

    /** Primary skin colour for generated textures (custom creatures only). */
    public int color() {
        return color;
    }

    public List<Drop> drops() {
        return drops;
    }

    /** NPC max hit from strength level (NPCs have no equipment strength bonus by default). */
    public int maxHit() {
        if (style == AttackType.MAGIC) {
            return Math.max(1, magic / 5);
        }
        int level = style == AttackType.RANGED ? ranged : strength;
        int effective = level + 9;
        return Math.max(1, (effective * 64 + 320) / 640);
    }

    /** Monsters are aggressive to players whose combat level is at most double theirs (+1). */
    public boolean isAggressiveTo(int playerCombatLevel) {
        return aggressive && playerCombatLevel <= combatLevel * 2 + 1;
    }

    public static Monster byEntity(String entityId) {
        String bare = entityId.startsWith("runecraft:") ? entityId.substring("runecraft:".length()) : entityId;
        for (Monster m : values()) {
            if (m.entity.equals(bare)) {
                return m;
            }
        }
        return null;
    }
}
