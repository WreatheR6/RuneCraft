package com.runecraft.osrs.data;

/**
 * Mining rocks. Level/XP/respawn (game ticks) are from OSRS; low/high success values out of 256 are
 * the OSRS Wiki's skilling success chart values (the pickaxe only changes how often you roll; a
 * negative low, as for amethyst, is part of the wiki curve). Essence rocks never deplete.
 */
public enum Rock {
    COPPER("copper", "Copper rocks", "copper_ore", "Copper ore", 1, 17.5, 100, 350, 4, 0xC87533, 5),
    TIN("tin", "Tin rocks", "tin_ore", "Tin ore", 1, 17.5, 100, 350, 4, 0xA8A8A0, 5),
    CLAY("clay", "Clay rocks", "clay", "Clay", 1, 5.0, 128, 400, 2, 0xC8B080, 2),
    RUNE_ESSENCE("rune_essence", "Rune essence", "rune_essence", "Rune essence", 1, 5.0, 256, 256, -1, 0xD8D8E0, 4),
    IRON("iron", "Iron rocks", "iron_ore", "Iron ore", 15, 35.0, 96, 350, 9, 0x6A4A3A, 17),
    SILVER("silver", "Silver rocks", "silver_ore", "Silver ore", 20, 40.0, 25, 200, 100, 0xD8D8E8, 75),
    PURE_ESSENCE("pure_essence", "Pure essence", "pure_essence", "Pure essence", 30, 5.0, 256, 256, -1, 0xF0F0FF, 6),
    COAL("coal", "Coal rocks", "coal", "Coal", 30, 50.0, 16, 100, 49, 0x202020, 45),
    GOLD("gold", "Gold rocks", "gold_ore", "Gold ore", 40, 65.0, 7, 75, 100, 0xE8C030, 150),
    GEM("gem", "Gem rocks", null, "Uncut gem", 40, 65.0, 28, 70, 99, 0xB040C0, 0),
    MITHRIL("mithril", "Mithril rocks", "mithril_ore", "Mithril ore", 55, 80.0, 4, 50, 200, 0x4A5A9C, 162),
    ADAMANTITE("adamantite", "Adamantite rocks", "adamantite_ore", "Adamantite ore", 70, 95.0, 2, 25, 400, 0x4C7A4C, 400),
    RUNITE("runite", "Runite rocks", "runite_ore", "Runite ore", 85, 125.0, 1, 18, 1200, 0x4FB8C8, 3200),
    AMETHYST("amethyst", "Amethyst crystals", "amethyst", "Amethyst", 92, 240.0, -18, 10, 125, 0xA070D0, 2000);

    private final String id;
    private final String name;
    private final String ore;
    private final String oreName;
    private final int level;
    private final double xp;
    private final int low;
    private final int high;
    private final int respawnTicks;
    private final int color;
    private final int oreValue;

    Rock(String id, String name, String ore, String oreName, int level, double xp, int low, int high,
         int respawnTicks, int color, int oreValue) {
        this.id = id;
        this.name = name;
        this.ore = ore;
        this.oreName = oreName;
        this.level = level;
        this.xp = xp;
        this.low = low;
        this.high = high;
        this.respawnTicks = respawnTicks;
        this.color = color;
        this.oreValue = oreValue;
    }

    public String blockId() {
        return id + "_rock";
    }

    public String displayName() {
        return name;
    }

    /** Item mined, or null for gem rocks (which roll the gem table). */
    public String oreItem() {
        return ore;
    }

    public String oreName() {
        return oreName;
    }

    /** Mining level from which a rune essence rock gives pure essence (OSRS: members with 30 Mining). */
    public static final int PURE_ESSENCE_LEVEL = 30;

    /**
     * The item mined at a Mining level: a rune essence rock (the Rune Essence mine's outcrops) gives pure essence
     * from {@link #PURE_ESSENCE_LEVEL} (every player counts as a member); other rocks give {@link #oreItem()}.
     */
    public String minedItem(int miningLevel) {
        if (this == RUNE_ESSENCE && miningLevel >= PURE_ESSENCE_LEVEL) {
            return PURE_ESSENCE.ore;
        }
        return ore;
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xp;
    }

    public int low() {
        return low;
    }

    public int high() {
        return high;
    }

    public boolean depletes() {
        return respawnTicks > 0;
    }

    public int respawnTicks() {
        return respawnTicks;
    }

    public int color() {
        return color;
    }

    public int oreValue() {
        return oreValue;
    }

    public String id() {
        return id;
    }
}
