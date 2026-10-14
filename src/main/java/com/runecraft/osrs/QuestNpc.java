package com.runecraft.osrs;

/** Quest characters, where they stand (landmark + offset in blocks) and how they look. */
public enum QuestNpc {
    // Characters with an OSRS tile stand there (mejrs/data_osrs NPC spawns; upstairs ones on the floor below,
    // except those given a storey, who stand on that floor of their rebuilt building);
    // those more than REMOTE blocks from their landmark spawn when their own chunk loads (LandmarkBuilder).
    COOK("Cook", Landmark.LUMBRIDGE, Look.COOK, 3209, 3215), // castle kitchen
    DUKE_HORACIO("Duke Horacio", Landmark.LUMBRIDGE, Look.NOBLE, 3212, 3220), // castle, 1st floor
    FATHER_AERECK("Father Aereck", Landmark.LUMBRIDGE, Look.PRIEST, 3244, 3206), // Lumbridge church
    RESTLESS_GHOST("Restless ghost", Landmark.LUMBRIDGE, Look.GHOST, 3250, 3195), // by his coffin in the graveyard
    FRED_THE_FARMER("Fred the Farmer", Landmark.LUMBRIDGE, Look.PEASANT, 3189, 3273), // his farmhouse
    FATHER_URHNEY("Father Urhney", Landmark.WATER_ALTAR, Look.PRIEST, 3146, 3174), // his shack in Lumbridge Swamp
    SEDRIDOR("Sedridor", Landmark.WIZARDS_TOWER, 3, 3, Look.WIZARD), // OSRS: the basement
    WIZARD_MIZGOG("Wizard Mizgog", Landmark.WIZARDS_TOWER, Look.WIZARD, 3103, 3163, 2), // top floor
    // OSRS 3112,3162 holds a chair in our 1st-floor room, so he stands one tile west // approx
    WIZARD_TRAIBORN("Wizard Traiborn", Landmark.WIZARDS_TOWER, Look.WIZARD, 3111, 3162, 1), // 1st floor
    AUBURY("Aubury", Landmark.VARROCK, Look.WIZARD, 3253, 3402), // his rune shop
    ROMEO("Romeo", Landmark.VARROCK, Look.NOBLE, 3211, 3425), // Varrock square
    JULIET("Juliet", Landmark.VARROCK, Look.LADY, 3158, 3425), // her house west of Varrock, 1st floor
    FATHER_LAWRENCE("Father Lawrence", Landmark.VARROCK, Look.PRIEST, 3254, 3484), // the church in north-east Varrock
    APOTHECARY("Apothecary", Landmark.VARROCK, Look.PEASANT, 3195, 3404), // his shop in south-west Varrock
    DR_HARLOW("Dr Harlow", Landmark.VARROCK, Look.PEASANT, 3222, 3397), // the Blue Moon Inn
    GYPSY_ARIS("Gypsy Aris", Landmark.VARROCK, Look.LADY, 3203, 3424), // her tent in Varrock square
    SIR_PRYSIN("Sir Prysin", Landmark.VARROCK, Look.KNIGHT, 3204, 3473), // Varrock Palace
    DORIC("Doric", Landmark.FALADOR, Look.DWARF, 2952, 3451), // his house north of Falador
    HETTY("Hetty", Landmark.RIMMINGTON, Look.WITCH, 2968, 3206), // her house in Rimmington
    KAQEMEEX("Kaqemeex", Landmark.TAVERLEY, Look.DRUID, 2925, 3486), // the stone circle north of Taverley
    SANFEW("Sanfew", Landmark.TAVERLEY, Look.DRUID, 2897, 3426), // herblore shop, 1st floor
    MORGAN("Morgan", Landmark.DRAYNOR_VILLAGE, Look.PEASANT, 3098, 3268); // his house in Draynor Village

    /** Texture variants for quest characters (assets/runecraft/textures/entity/npc_&lt;look&gt;.png). */
    public enum Look { PEASANT, COOK, WIZARD, PRIEST, NOBLE, DWARF, KNIGHT, WITCH, DRUID, GHOST, LADY }

    private final String displayName;
    private final Landmark landmark;
    private final int dx;
    private final int dz;
    private final Look look;
    private final boolean tile;
    private final int floor;

    /** Blocks from the landmark beyond which a character is spawned when its own chunk loads, not with the landmark. */
    public static final int REMOTE = 24;

    QuestNpc(String displayName, Landmark landmark, int dx, int dz, Look look) {
        this.displayName = displayName;
        this.landmark = landmark;
        this.dx = dx;
        this.dz = dz;
        this.look = look;
        this.tile = false;
        this.floor = 0;
    }

    /** A character standing on its OSRS tile. */
    QuestNpc(String displayName, Landmark landmark, Look look, int osrsX, int osrsY) {
        this(displayName, landmark, look, osrsX, osrsY, 0);
    }

    /** A character standing on its OSRS tile on the given storey (0 = ground floor) of the building there. */
    QuestNpc(String displayName, Landmark landmark, Look look, int osrsX, int osrsY, int floor) {
        this.displayName = displayName;
        this.landmark = landmark;
        this.dx = GielinorMap.toBlockX(osrsX) - landmark.blockX();
        this.dz = GielinorMap.toBlockZ(osrsY) - landmark.blockZ();
        this.look = look;
        this.tile = true;
        this.floor = floor;
    }

    /** Storey the character stands on (0 = ground floor; OSRS plane). */
    public int floor() {
        return floor;
    }

    /** Whether the character stands on its own OSRS tile (rather than an offset picked near the landmark). */
    public boolean onOsrsTile() {
        return tile;
    }

    /** Block column the character stands on. */
    public int blockX() {
        return landmark.blockX() + dx;
    }

    public int blockZ() {
        return landmark.blockZ() + dz;
    }

    /** Too far from the landmark to spawn with it: spawned when the chunk holding {@link #blockX()}/{@link #blockZ()} loads. */
    public boolean remote() {
        return Math.max(Math.abs(dx), Math.abs(dz)) > REMOTE;
    }

    public String displayName() {
        return displayName;
    }

    public Landmark landmark() {
        return landmark;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    public Look look() {
        return look;
    }
}
