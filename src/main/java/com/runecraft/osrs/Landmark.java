package com.runecraft.osrs;

import java.util.List;

/**
 * Fixed locations in Gielinor, placed at their real Old School RuneScape world-map coordinates
 * (converted with {@link GielinorMap#toBlockX}/{@link GielinorMap#toBlockZ}). When the chunk
 * containing a landmark first loads, the mod builds a small settlement or altar there.
 */
public enum Landmark {
    LUMBRIDGE("Lumbridge", 3222, 3218, Station.PORTAL, Station.BANK_BOOTH, Station.RANGE, Station.GENERAL_STORE,
            Station.TOOL_SHOP, Station.PRAYER_ALTAR, Station.SPINNING_WHEEL, Station.FURNACE, Station.ANVIL),
    VARROCK("Varrock", 3213, 3428, Station.BANK_BOOTH, Station.ANVIL, Station.ANVIL, Station.RUNE_SHOP,
            Station.WEAPON_SHOP, Station.RANGE, Station.TANNER, Station.WORKBENCH),
    FALADOR("Falador", 2965, 3380, Station.BANK_BOOTH, Station.FURNACE, Station.ANVIL, Station.PRAYER_ALTAR,
            Station.GENERAL_STORE, Station.ALLOTMENT_PATCH, Station.HERB_PATCH),
    DRAYNOR_VILLAGE("Draynor Village", 3093, 3244, Station.BANK_BOOTH, Station.HERB_PATCH, Station.ALLOTMENT_PATCH),
    AL_KHARID("Al Kharid", 3293, 3174, Station.BANK_BOOTH, Station.FURNACE, Station.RANGE, Station.TANNER,
            Station.GENERAL_STORE),
    PORT_SARIM("Port Sarim", 3023, 3208, Station.GENERAL_STORE, Station.TOOL_SHOP, Station.RANGE),
    RIMMINGTON("Rimmington", 2957, 3214, Station.RANGE, Station.WORKBENCH, Station.ARCHERY_SHOP),
    EDGEVILLE("Edgeville", 3093, 3493, Station.BANK_BOOTH, Station.FURNACE, Station.PRAYER_ALTAR,
            Station.GENERAL_STORE, Station.SLAYER_MASTER),
    BARBARIAN_VILLAGE("Barbarian Village", 3082, 3420, Station.RANGE, Station.TOOL_SHOP),
    TAVERLEY("Taverley", 2895, 3445, Station.HERBLORE_TABLE, Station.SLAYER_MASTER),
    BURTHORPE("Burthorpe", 2899, 3544, Station.BANK_BOOTH, Station.SLAYER_MASTER, Station.RANGE),
    CATHERBY("Catherby", 2813, 3447, Station.BANK_BOOTH, Station.RANGE, Station.ALLOTMENT_PATCH,
            Station.HERB_PATCH, Station.ARCHERY_SHOP),
    CAMELOT("Camelot", 2757, 3477, Station.BANK_BOOTH, Station.PRAYER_ALTAR),
    SEERS_VILLAGE("Seers' Village", 2726, 3485, Station.BANK_BOOTH, Station.SPINNING_WHEEL, Station.RANGE,
            Station.ARCHERY_SHOP),
    ARDOUGNE("East Ardougne", 2662, 3305, Station.BANK_BOOTH, Station.CAKE_STALL, Station.SILK_STALL,
            Station.FUR_STALL, Station.SILVER_STALL, Station.SPICE_STALL, Station.GEM_STALL),
    YANILLE("Yanille", 2606, 3093, Station.BANK_BOOTH, Station.HERBLORE_TABLE, Station.ANVIL, Station.RANGE),
    GNOME_STRONGHOLD("Tree Gnome Stronghold", 2461, 3444, Station.BANK_BOOTH, Station.ARCHERY_SHOP),
    RELLEKKA("Rellekka", 2660, 3657, Station.FURNACE, Station.ANVIL, Station.RANGE, Station.FUR_STALL),
    TROLLHEIM("Trollheim", 2890, 3676, Station.PRAYER_ALTAR),
    CANIFIS("Canifis", 3496, 3488, Station.BANK_BOOTH, Station.RANGE, Station.SLAYER_MASTER),
    PORT_PHASMATYS("Port Phasmatys", 3687, 3502, Station.BANK_BOOTH, Station.PRAYER_ALTAR, Station.GENERAL_STORE),
    BRIMHAVEN("Brimhaven", 2760, 3178, Station.RANGE, Station.GENERAL_STORE),
    MUSA_POINT("Musa Point", 2914, 3176, Station.GENERAL_STORE),
    SHILO_VILLAGE("Shilo Village", 2852, 2955, Station.BANK_BOOTH, Station.FURNACE, Station.RANGE),
    POLLNIVNEACH("Pollnivneach", 3359, 2970, Station.GENERAL_STORE, Station.RANGE),
    NARDAH("Nardah", 3428, 2892, Station.BANK_BOOTH, Station.GENERAL_STORE, Station.PRAYER_ALTAR),
    LLETYA("Lletya", 2340, 3170, Station.BANK_BOOTH, Station.GENERAL_STORE, Station.RANGE),
    WIZARDS_TOWER("Wizards' Tower", 3109, 3162, Station.RUNE_SHOP),
    DRAYNOR_MANOR("Draynor Manor", 3109, 3353),
    STONE_CIRCLE("Stone circle", 3227, 3369),
    BARROWS("Barrows", 3565, 3288, Station.BANK_BOOTH, Station.PRAYER_ALTAR),
    GOD_WARS_DUNGEON("God Wars Dungeon", 2917, 3745, Station.PRAYER_ALTAR),
    MOR_UL_REK("Mor Ul Rek", 2845, 3170, Station.BANK_BOOTH, Station.PRAYER_ALTAR),

    AIR_ALTAR("Air Altar", 2985, 3292, Rune.AIR),
    MIND_ALTAR("Mind Altar", 2982, 3514, Rune.MIND),
    WATER_ALTAR("Water Altar", 3185, 3165, Rune.WATER),
    EARTH_ALTAR("Earth Altar", 3306, 3474, Rune.EARTH),
    FIRE_ALTAR("Fire Altar", 3313, 3255, Rune.FIRE),
    BODY_ALTAR("Body Altar", 3053, 3445, Rune.BODY),
    COSMIC_ALTAR("Cosmic Altar", 2405, 3380, Rune.COSMIC),
    CHAOS_ALTAR("Chaos Altar", 3060, 3591, Rune.CHAOS),
    ASTRAL_ALTAR("Astral Altar", 2156, 3864, Rune.ASTRAL),
    NATURE_ALTAR("Nature Altar", 2869, 3019, Rune.NATURE),
    LAW_ALTAR("Law Altar", 2858, 3381, Rune.LAW),
    DEATH_ALTAR("Death Altar", 2470, 3220, Rune.DEATH),
    BLOOD_ALTAR("Blood Altar", 3560, 3390, Rune.BLOOD),
    SOUL_ALTAR("Soul Altar", 3330, 3770, Rune.SOUL),
    WRATH_ALTAR("Wrath Altar", 2445, 2818, Rune.WRATH);

    private final String displayName;
    private final int osrsX;
    private final int osrsY;
    private final List<Station> stations;
    private final Rune altar;

    Landmark(String displayName, int osrsX, int osrsY, Station... stations) {
        this.displayName = displayName;
        this.osrsX = osrsX;
        this.osrsY = osrsY;
        this.stations = List.of(stations);
        this.altar = null;
    }

    Landmark(String displayName, int osrsX, int osrsY, Rune altar) {
        this.displayName = displayName;
        this.osrsX = osrsX;
        this.osrsY = osrsY;
        this.stations = List.of();
        this.altar = altar;
    }

    public String displayName() {
        return displayName;
    }

    public int osrsX() {
        return osrsX;
    }

    public int osrsY() {
        return osrsY;
    }

    /** Block X of the centre of the chunk the landmark is built in. */
    public int blockX() {
        return ((GielinorMap.toBlockX(osrsX) >> 4) << 4) + 8;
    }

    /** Block Z of the centre of the chunk the landmark is built in. */
    public int blockZ() {
        return ((GielinorMap.toBlockZ(osrsY) >> 4) << 4) + 8;
    }

    public List<Station> stations() {
        return stations;
    }

    /** The rune this runecrafting altar produces, or null for a settlement. */
    public Rune altar() {
        return altar;
    }

    public boolean isAltar() {
        return altar != null;
    }

    public static Landmark byName(String name) {
        for (Landmark l : values()) {
            if (l.name().equalsIgnoreCase(name) || l.displayName.equalsIgnoreCase(name)) {
                return l;
            }
        }
        return null;
    }
}
