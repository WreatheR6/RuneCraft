package com.runecraft.osrs.data;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Landmark;

import java.util.List;

/**
 * Bosses with OSRS stats and their signature mechanics. Each is summoned at its lair (see
 * {@link #lair()}), some requiring a key dropped by regular monsters, just like in-game.
 */
public enum Boss {
    OBOR("obor", "Obor", 106, 120, 90, 80, 60, 1, 1, 6, AttackType.CRUSH, 12, 0, Mechanic.NONE, 2.6f, 0xB89070,
            Landmark.EDGEVILLE, "giant_key",
            List.of(Drop.always("big_bones"), Drop.of("coins", 500, 3000, 1), Drop.of("law_rune", 15, 30, 4),
                    Drop.of("rune_full_helm", 1, 1, 12), Drop.of("hill_giant_club", 1, 1, 118), Drop.herbs(4))),
    BRYOPHYTA("bryophyta", "Bryophyta", 128, 115, 100, 100, 100, 100, 1, 6, AttackType.CRUSH, 16, 0, Mechanic.MIXED_MAGIC,
            2.4f, 0x4A7A2A, Landmark.VARROCK, "mossy_key",
            List.of(Drop.always("big_bones"), Drop.of("coins", 500, 4000, 1), Drop.of("nature_rune", 20, 40, 4),
                    Drop.of("rune_platelegs", 1, 1, 16), Drop.of("bryophytas_essence", 1, 1, 118), Drop.herbs(3))),
    AHRIM("ahrim", "Ahrim the Blighted", 98, 100, 1, 1, 100, 100, 1, 6, AttackType.MAGIC, 25, 0, Mechanic.AHRIM, 1.0f,
            0x3A3A5A, Landmark.BARROWS, null, barrows("ahrims_hood", "ahrims_robetop", "ahrims_robeskirt", "ahrims_staff")),
    DHAROK("dharok", "Dharok the Wretched", 115, 100, 100, 100, 100, 1, 1, 7, AttackType.SLASH, 29, 0, Mechanic.DHAROK, 1.0f,
            0x5A5A5A, Landmark.BARROWS, null, barrows("dharoks_helm", "dharoks_platebody", "dharoks_platelegs", "dharoks_greataxe")),
    GUTHAN("guthan", "Guthan the Infested", 115, 100, 100, 100, 100, 1, 1, 5, AttackType.STAB, 24, 0, Mechanic.GUTHAN, 1.0f,
            0x6A6A5A, Landmark.BARROWS, null, barrows("guthans_helm", "guthans_platebody", "guthans_chainskirt", "guthans_warspear")),
    KARIL("karil", "Karil the Tainted", 98, 100, 1, 1, 100, 1, 100, 4, AttackType.RANGED, 20, 0, Mechanic.KARIL, 1.0f,
            0x4A3A2A, Landmark.BARROWS, null, barrows("karils_coif", "karils_leathertop", "karils_leatherskirt", "karils_crossbow")),
    TORAG("torag", "Torag the Corrupted", 115, 100, 100, 100, 100, 1, 1, 5, AttackType.CRUSH, 23, 0, Mechanic.TORAG, 1.0f,
            0x5A4A3A, Landmark.BARROWS, null, barrows("torags_helm", "torags_platebody", "torags_platelegs", "torags_hammers")),
    VERAC("verac", "Verac the Defiled", 115, 100, 100, 100, 100, 1, 1, 5, AttackType.CRUSH, 23, 0, Mechanic.VERAC, 1.0f,
            0x6A5A4A, Landmark.BARROWS, null, barrows("veracs_helm", "veracs_brassard", "veracs_plateskirt", "veracs_flail")),
    GENERAL_GRAARDOR("general_graardor", "General Graardor", 624, 255, 280, 350, 250, 80, 350, 6, AttackType.CRUSH, 60, 90,
            Mechanic.GRAARDOR, 2.2f, 0x5A6A3A, Landmark.GOD_WARS_DUNGEON, null,
            List.of(Drop.always("big_bones"), Drop.of("coins", 5000, 20000, 1), Drop.of("rune_platebody", 1, 1, 8),
                    Drop.of("bandos_chestplate", 1, 1, 381), Drop.of("bandos_tassets", 1, 1, 381), Drop.of("bandos_boots", 1, 1, 381),
                    Drop.of("snapdragon_seed", 1, 2, 16))),
    KRIL_TSUTSAROTH("kril_tsutsaroth", "K'ril Tsutsaroth", 650, 255, 340, 300, 270, 200, 1, 6, AttackType.SLASH, 47, 80,
            Mechanic.KRIL, 2.0f, 0x801818, Landmark.GOD_WARS_DUNGEON, null,
            List.of(Drop.always("infernal_ashes"), Drop.of("coins", 5000, 20000, 1), Drop.of("death_rune", 100, 150, 4),
                    Drop.of("zamorakian_spear", 1, 1, 127), Drop.of("steam_battlestaff", 1, 1, 127),
                    Drop.of("staff_of_the_dead", 1, 1, 508), Drop.of("wine_of_zamorak", 3, 6, 8))),
    TZTOK_JAD("tztok_jad", "TzTok-Jad", 702, 250, 640, 960, 480, 480, 960, 8, AttackType.MAGIC, 97, 0, Mechanic.JAD, 2.8f,
            0xC04010, Landmark.MOR_UL_REK, null,
            List.of(Drop.always("fire_cape"), Drop.of("tokkul", 8000, 16000, 1), Drop.of("tzrek_jad", 1, 1, 200))),
    COUNT_DRAYNOR("count_draynor", "Count Draynor", 34, 35, 30, 25, 30, 1, 1, 4, AttackType.CRUSH, 5, 0, Mechanic.VAMPIRE,
            1.0f, 0x202030, Landmark.DRAYNOR_MANOR, null, List.of(Drop.always("bones"))),
    DELRITH("delrith", "Delrith", 27, 30, 30, 30, 20, 1, 1, 4, AttackType.SLASH, 4, 0, Mechanic.DEMON, 1.4f, 0xA02020,
            Landmark.STONE_CIRCLE, null, List.of(Drop.always("bones")));

    public enum Mechanic {
        NONE, MIXED_MAGIC, AHRIM, DHAROK, GUTHAN, KARIL, TORAG, VERAC, GRAARDOR, KRIL, JAD, VAMPIRE, DEMON
    }

    private final String id;
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
    private final int maxHit;
    private final int defenceBonus;
    private final Mechanic mechanic;
    private final float scale;
    private final int color;
    private final Landmark lair;
    private final String key;
    private final List<Drop> drops;

    Boss(String id, String name, int combatLevel, int hitpoints, int attack, int strength, int defence, int magic,
         int ranged, int attackSpeed, AttackType style, int maxHit, int defenceBonus, Mechanic mechanic, float scale,
         int color, Landmark lair, String key, List<Drop> drops) {
        this.id = id;
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
        this.maxHit = maxHit;
        this.defenceBonus = defenceBonus;
        this.mechanic = mechanic;
        this.scale = scale;
        this.color = color;
        this.lair = lair;
        this.key = key;
        this.drops = drops;
    }

    private static List<Drop> barrows(String... pieces) {
        List<Drop> list = new java.util.ArrayList<>();
        list.add(Drop.of("coins", 200, 1500, 1));
        list.add(Drop.of("death_rune", 10, 40, 3));
        list.add(Drop.of("blood_rune", 5, 25, 4));
        list.add(Drop.of("chaos_rune", 20, 80, 3));
        for (String p : pieces) {
            list.add(Drop.of(p, 1, 1, 24));
        }
        return list;
    }

    /**
     * OSRS monster attack bonus (accuracy) for an attack style, from the wiki's aggressive stats; 0 where the
     * boss has none or it isn't known.
     */
    public int attackBonus(AttackType type) {
        boolean melee = type.isMelee();
        return switch (this) {
            case GENERAL_GRAARDOR -> melee ? 120 : type == AttackType.RANGED ? 100 : 0;
            case KRIL_TSUTSAROTH -> melee ? 160 : 0;
            case TZTOK_JAD -> type == AttackType.MAGIC ? 60 : 0;
            default -> 0;
        };
    }

    public String entityId() {
        return id;
    }

    public String summonBlockId() {
        return id + "_summoning_stone";
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

    public int maxHit() {
        return maxHit;
    }

    public int defenceBonus() {
        return defenceBonus;
    }

    public Mechanic mechanic() {
        return mechanic;
    }

    public float scale() {
        return scale;
    }

    public int color() {
        return color;
    }

    public Landmark lair() {
        return lair;
    }

    /** Item consumed to summon the boss, or null. */
    public String key() {
        return key;
    }

    public List<Drop> drops() {
        return drops;
    }

    /**
     * Bodyguards that share the boss's chamber and are summoned with it (God Wars generals each fight
     * alongside three: a melee, a magic and a ranged bodyguard). Monster ids from the godwars module.
     */
    public List<String> bodyguards() {
        return switch (this) {
            case GENERAL_GRAARDOR -> List.of("sergeant_strongstack", "sergeant_steelwill", "sergeant_grimspike");
            case KRIL_TSUTSAROTH -> List.of("tstanon_karlak", "balfrug_kreeyath", "zakln_gritch");
            default -> List.of();
        };
    }

    /** TzTok-Jad also has a melee attack (max 97) that it only uses when its target is next to it. */
    public boolean meleeWhenAdjacent() {
        return this == TZTOK_JAD;
    }

    /** Whether this boss is fought as part of a quest rather than freely. */
    public boolean questBoss() {
        return this == COUNT_DRAYNOR || this == DELRITH;
    }

    public static Boss byEntity(String id) {
        for (Boss b : values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }
}
