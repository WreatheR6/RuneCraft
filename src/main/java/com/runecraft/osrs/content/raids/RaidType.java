package com.runecraft.osrs.content.raids;

/**
 * The three OSRS raids. Each has an entrance block placed at a module place (real OSRS
 * coordinates where possible) and a maximum party size.
 */
public enum RaidType {
    /**
     * Chambers of Xeric. In OSRS the entrance is on Mount Quidamortem in Great Kourend (~1233, 3573),
     * which lies off this map; it is placed on the north-west coast of Tirannwn instead, looking out
     * west over the sea towards where Kourend would be.
     */
    COX("chambers_of_xeric", "Chambers of Xeric", "mount_quidamortem", "Mount Quidamortem", 2180, 3400, 100),
    /** Theatre of Blood, beneath Ver Sinhaza in Morytania. */
    TOB("theatre_of_blood", "Theatre of Blood", "ver_sinhaza", "Ver Sinhaza", 3650, 3220, 5),
    /** Tombs of Amascut, in the Jaldraocht Pyramid in the Kharidian Desert. */
    TOA("tombs_of_amascut", "Tombs of Amascut", "toa_jaldraocht_entrance", "Jaldraocht Pyramid", 3350, 2750, 8);

    private final String id;
    private final String displayName;
    private final String placeId;
    private final String placeName;
    private final int osrsX;
    private final int osrsY;
    private final int maxParty;

    RaidType(String id, String displayName, String placeId, String placeName, int osrsX, int osrsY, int maxParty) {
        this.id = id;
        this.displayName = displayName;
        this.placeId = placeId;
        this.placeName = placeName;
        this.osrsX = osrsX;
        this.osrsY = osrsY;
        this.maxParty = maxParty;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Module place (PlaceDef id) holding the entrance. */
    public String placeId() {
        return placeId;
    }

    public String placeName() {
        return placeName;
    }

    public int osrsX() {
        return osrsX;
    }

    public int osrsY() {
        return osrsY;
    }

    public int maxParty() {
        return maxParty;
    }

    /** Scripted block that forms a party and starts the raid. */
    public String entranceBlock() {
        return id + "_entrance";
    }

    public static RaidType byId(String id) {
        for (RaidType t : values()) {
            if (t.id.equals(id) || t.name().equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }
}
