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
    SUPERIOR_DRAGON_BONES("superior_dragon_bones", "Superior dragon bones", 150.0, 0xC8C0A0);

    /** Using bones on a (lit) altar in a settlement gives 350% XP, like a gilded altar. */
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
