package com.runecraft.osrs;

/** Quest characters, where they stand (landmark + offset in blocks) and how they look. */
public enum QuestNpc {
    COOK("Cook", Landmark.LUMBRIDGE, -4, -5, Look.COOK),
    DUKE_HORACIO("Duke Horacio", Landmark.LUMBRIDGE, 4, -5, Look.NOBLE),
    FATHER_AERECK("Father Aereck", Landmark.LUMBRIDGE, 5, 4, Look.PRIEST),
    RESTLESS_GHOST("Restless ghost", Landmark.LUMBRIDGE, -5, 5, Look.GHOST),
    FRED_THE_FARMER("Fred the Farmer", Landmark.LUMBRIDGE, 0, 9, Look.PEASANT),
    FATHER_URHNEY("Father Urhney", Landmark.WATER_ALTAR, 4, -4, Look.PRIEST),
    SEDRIDOR("Sedridor", Landmark.WIZARDS_TOWER, 3, 3, Look.WIZARD),
    WIZARD_MIZGOG("Wizard Mizgog", Landmark.WIZARDS_TOWER, -3, 3, Look.WIZARD),
    WIZARD_TRAIBORN("Wizard Traiborn", Landmark.WIZARDS_TOWER, 0, -4, Look.WIZARD),
    AUBURY("Aubury", Landmark.VARROCK, 5, -3, Look.WIZARD),
    ROMEO("Romeo", Landmark.VARROCK, -2, 6, Look.NOBLE),
    JULIET("Juliet", Landmark.VARROCK, -6, -6, Look.LADY),
    FATHER_LAWRENCE("Father Lawrence", Landmark.VARROCK, 6, 6, Look.PRIEST),
    APOTHECARY("Apothecary", Landmark.VARROCK, -6, 2, Look.PEASANT),
    DR_HARLOW("Dr Harlow", Landmark.VARROCK, 2, -6, Look.PEASANT),
    GYPSY_ARIS("Gypsy Aris", Landmark.VARROCK, 0, 4, Look.LADY),
    SIR_PRYSIN("Sir Prysin", Landmark.VARROCK, 4, 2, Look.KNIGHT),
    DORIC("Doric", Landmark.FALADOR, 5, 5, Look.DWARF),
    HETTY("Hetty", Landmark.RIMMINGTON, 3, -3, Look.WITCH),
    KAQEMEEX("Kaqemeex", Landmark.TAVERLEY, 3, 3, Look.DRUID),
    SANFEW("Sanfew", Landmark.TAVERLEY, -3, 3, Look.DRUID),
    MORGAN("Morgan", Landmark.DRAYNOR_VILLAGE, 4, 4, Look.PEASANT);

    /** Texture variants for quest characters (assets/runecraft/textures/entity/npc_&lt;look&gt;.png). */
    public enum Look { PEASANT, COOK, WIZARD, PRIEST, NOBLE, DWARF, KNIGHT, WITCH, DRUID, GHOST, LADY }

    private final String displayName;
    private final Landmark landmark;
    private final int dx;
    private final int dz;
    private final Look look;

    QuestNpc(String displayName, Landmark landmark, int dx, int dz, Look look) {
        this.displayName = displayName;
        this.landmark = landmark;
        this.dx = dx;
        this.dz = dz;
        this.look = look;
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
