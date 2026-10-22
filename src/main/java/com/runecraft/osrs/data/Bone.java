package com.runecraft.osrs.data;

/** Bones and the Prayer XP for burying them. */
public enum Bone {
    BONES("bones", "Bones", 4.5, 0xE8E0D0),
    BIG_BONES("big_bones", "Big bones", 15.0, 0xE0D8C0),
    BABYDRAGON_BONES("babydragon_bones", "Babydragon bones", 30.0, 0xD8D0B0),
    DRAGON_BONES("dragon_bones", "Dragon bones", 72.0, 0xD0C8A8),
    WYVERN_BONES("wyvern_bones", "Wyvern bones", 72.0, 0xD8D0C0),
    LAVA_DRAGON_BONES("lava_dragon_bones", "Lava dragon bones", 85.0, 0xC08060),
    HYDRA_BONES("hydra_bones", "Hydra bones", 110.0, 0xB0C0A0),
    DAGANNOTH_BONES("dagannoth_bones", "Dagannoth bones", 125.0, 0xC8C0B0),
    SUPERIOR_DRAGON_BONES("superior_dragon_bones", "Superior dragon bones", 150.0, 0xC8C0A0),
    // Monster-specific bones (OSRS Wiki Prayer XP), dropped by Ape Atoll monkeys, jogres, wyrms and drakes.
    MONKEY_BONES("monkey_bones", "Monkey bones", 5.0, 0xD8CCB0),
    SMALL_NINJA_MONKEY_BONES("small_ninja_monkey_bones", "Small ninja monkey bones", 16.0, 0xD0C4A8),
    MEDIUM_NINJA_MONKEY_BONES("medium_ninja_monkey_bones", "Medium ninja monkey bones", 18.0, 0xC8BCA0),
    GORILLA_BONES("gorilla_bones", "Gorilla bones", 18.0, 0xC8C0A8),
    BEARDED_GORILLA_BONES("bearded_gorilla_bones", "Bearded gorilla bones", 18.0, 0xC0B8A0),
    SMALL_ZOMBIE_MONKEY_BONES("small_zombie_monkey_bones", "Small zombie monkey bones", 5.0, 0xB8C0A0),
    LARGE_ZOMBIE_MONKEY_BONES("large_zombie_monkey_bones", "Large zombie monkey bones", 5.0, 0xB0B898),
    JOGRE_BONES("jogre_bones", "Jogre bones", 15.0, 0xD0C8B0),
    WYRM_BONES("wyrm_bones", "Wyrm bones", 50.0, 0xC8C8B8),
    DRAKE_BONES("drake_bones", "Drake bones", 80.0, 0xC0B8A8),
    // Round 27 (OSRS Wiki Prayer XP). Bat and wolf bones were plain module items before; append only (ordinal-sensitive).
    BAT_BONES("bat_bones", "Bat bones", 5.3, 0x8A8070),
    WOLF_BONES("wolf_bones", "Wolf bones", 4.5, 0xE0D8C8),
    WYRMLING_BONES("wyrmling_bones", "Wyrmling bones", 21.0, 0xC8C0B0),
    ZOGRE_BONES("zogre_bones", "Zogre bones", 22.5, 0xB8C0A0),
    // Round 44: frost dragons (Grimstone Dungeon, slayer module); OSRS Wiki "Frost dragon bones": 100 Prayer XP.
    FROST_DRAGON_BONES("frost_dragon_bones", "Frost dragon bones", 100.0, 0xD8E8F0);

    /** Best bone-offering multiplier at a gilded altar (both incense burners lit); see {@link PrayerAltars}. */
    public static final double ALTAR_MULTIPLIER = 3.5;

    private final String id;
    private final String name;
    private final double xp;
    private final int color;

    Bone(String id, String name, double xp, int color) {
        this.id = id;
        this.name = name;
        this.xp = xp;
        this.color = color;
    }

    public String itemId() {
        return id;
    }

    public String displayName() {
        return name;
    }

    public double xp() {
        return xp;
    }

    public int color() {
        return color;
    }

    public static Bone byItem(String id) {
        for (Bone b : values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }
}
