package com.runecraft.osrs.data;

import com.runecraft.osrs.Skill;

/** Finished potions: unfinished potion + secondary. Effects are applied per dose. */
public enum Potion {
    ATTACK("attack_potion", "Attack potion", Herb.GUAM, "eye_of_newt", 3, 25.0, 0xC03030),
    ANTIPOISON("antipoison", "Antipoison", Herb.MARRENTILL, "unicorn_horn_dust", 5, 37.5, 0x90E090),
    STRENGTH("strength_potion", "Strength potion", Herb.TARROMIN, "limpwurt_root", 12, 50.0, 0x30A030),
    RESTORE("restore_potion", "Restore potion", Herb.HARRALANDER, "red_spiders_eggs", 22, 62.5, 0xE07080),
    ENERGY("energy_potion", "Energy potion", Herb.HARRALANDER, "chocolate_dust", 26, 67.5, 0xC08040),
    DEFENCE("defence_potion", "Defence potion", Herb.RANARR, "white_berries", 30, 75.0, 0x40A0A0),
    PRAYER("prayer_potion", "Prayer potion", Herb.RANARR, "snape_grass", 38, 87.5, 0x30C0A0),
    SUPER_ATTACK("super_attack", "Super attack", Herb.IRIT, "eye_of_newt", 45, 100.0, 0x3030C0),
    SUPER_STRENGTH("super_strength", "Super strength", Herb.KWUARM, "limpwurt_root", 55, 125.0, 0xE0E0E0),
    SUPER_RESTORE("super_restore", "Super restore", Herb.SNAPDRAGON, "red_spiders_eggs", 63, 142.5, 0xC03070),
    SUPER_DEFENCE("super_defence", "Super defence", Herb.CADANTINE, "white_berries", 66, 150.0, 0xC0A030),
    RANGING("ranging_potion", "Ranging potion", Herb.DWARF_WEED, "wine_of_zamorak", 72, 162.5, 0x3090C0),
    MAGIC("magic_potion", "Magic potion", Herb.LANTADYME, "potato_cactus", 76, 172.5, 0x804020),
    SARADOMIN_BREW("saradomin_brew", "Saradomin brew", Herb.TOADFLAX, "crushed_nest", 81, 180.0, 0xE0D060);

    private final String id;
    private final String name;
    private final Herb herb;
    private final String secondary;
    private final int level;
    private final double xp;
    private final int color;

    Potion(String id, String name, Herb herb, String secondary, int level, double xp, int color) {
        this.id = id;
        this.name = name;
        this.herb = herb;
        this.secondary = secondary;
        this.level = level;
        this.xp = xp;
        this.color = color;
    }

    public String itemId() {
        return id;
    }

    public String displayName() {
        return name;
    }

    public Herb herb() {
        return herb;
    }

    public String secondary() {
        return secondary;
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xp;
    }

    public int color() {
        return color;
    }

    /** Items that add 2% of the Prayer level to prayer restores (OSRS Wiki "Holy wrench"; the effects don't stack). */
    public static final String HOLY_WRENCH = "holy_wrench";
    public static final java.util.Set<String> WRENCH_WORN = java.util.Set.of("ring_of_the_gods_i", "prayer_cape", "prayer_cape_t", "max_cape");

    /**
     * Extra prayer points a holy wrench (or prayer cape / ring of the gods (i)) adds to one dose of a prayer-restoring potion
     * whose restore is {@code percent}% of the Prayer level (25: prayer potion, super restore and their mixes, blighted super
     * restore; 30: Sanfew serum): the restore becomes {@code percent + 2}% (OSRS Wiki "Holy wrench").
     */
    public static int wrenchBonus(int prayerLevel, int percent) {
        return prayerLevel * (percent + 2) / 100 - prayerLevel * percent / 100;
    }

    /** Applies one dose to a skill set; returns prayer points restored (applied by the caller). */
    public int apply(com.runecraft.osrs.SkillSet skills) {
        switch (this) {
            case ATTACK -> skills.applyBoost(Skill.ATTACK, 3, 0.10);
            case STRENGTH -> skills.applyBoost(Skill.STRENGTH, 3, 0.10);
            case DEFENCE -> skills.applyBoost(Skill.DEFENCE, 3, 0.10);
            case SUPER_ATTACK -> skills.applyBoost(Skill.ATTACK, 5, 0.15);
            case SUPER_STRENGTH -> skills.applyBoost(Skill.STRENGTH, 5, 0.15);
            case SUPER_DEFENCE -> skills.applyBoost(Skill.DEFENCE, 5, 0.15);
            case RANGING -> skills.applyBoost(Skill.RANGED, 4, 0.10);
            case MAGIC -> skills.applyBoost(Skill.MAGIC, 4, 0.0);
            case RESTORE -> {
                for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC}) {
                    skills.restore(s, 10, 0.30);
                }
            }
            case SUPER_RESTORE -> {
                for (Skill s : Skill.values()) {
                    if (s != Skill.HITPOINTS && s != Skill.PRAYER) {
                        skills.restore(s, 8, 0.25);
                    }
                }
                return 8 + skills.level(Skill.PRAYER) / 4;
            }
            case PRAYER -> {
                return 7 + skills.level(Skill.PRAYER) / 4;
            }
            case SARADOMIN_BREW -> {
                skills.applyBoost(Skill.DEFENCE, 2, 0.20);
                for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.RANGED, Skill.MAGIC}) {
                    int drain = com.runecraft.osrs.CombatEffects.brewDrain(skills.currentLevel(s));
                    skills.setBoost(s, Math.max(-skills.level(s), skills.boost(s) - drain));
                }
            }
            default -> {
            }
        }
        return 0;
    }

    /**
     * Hitpoints healed per dose (only the Saradomin brew heals). A brew can boost Hitpoints above the maximum,
     * up to {@link com.runecraft.osrs.CombatEffects#brewCap}.
     */
    public int heal(com.runecraft.osrs.SkillSet skills) {
        return this == SARADOMIN_BREW ? com.runecraft.osrs.CombatEffects.brewHeal(skills.level(Skill.HITPOINTS)) : 0;
    }

    /** True for potions whose heal may boost Hitpoints above the maximum. */
    public boolean overheals() {
        return this == SARADOMIN_BREW;
    }
}
