package com.runecraft.osrs.data;

import java.util.List;
import java.util.Locale;

/**
 * Blighted spell sacks (OSRS Wiki "Blighted surge sack", "Blighted entangle sack", "Blighted ancient ice sack", "Blighted
 * teleport spell sack", "Blighted vengeance sack"): each one pays for one cast of its spells instead of the runes and is used
 * up by the cast, hit or miss. They only work in the Wilderness (which here includes the Ferox Enclave and the Mage Arena
 * bank), Castle Wars and Soul Wars; the teleport spell sack only in the Wilderness. The Magic level and spellbook rules
 * still apply.
 * <ul>
 * <li>surge sack: the four surge spells and, since the wave sack was merged into it (20 September 2023), the wave spells;</li>
 * <li>entangle sack: Bind, Snare and Entangle (the bind and snare sacks were merged into it);</li>
 * <li>ancient ice sack: Ice Rush, Ice Burst, Ice Blitz and Ice Barrage;</li>
 * <li>teleport spell sack: Tele Block and Teleport to Target;</li>
 * <li>vengeance sack: Vengeance and Vengeance Other.</li>
 * </ul>
 */
public final class BlightedSacks {
    public static final String SURGE = "blighted_surge_sack";
    public static final String ENTANGLE = "blighted_entangle_sack";
    public static final String ANCIENT_ICE = "blighted_ancient_ice_sack";
    public static final String TELEPORT = "blighted_teleport_spell_sack";
    public static final String VENGEANCE = "blighted_vengeance_sack";
    public static final List<String> ALL = List.of(SURGE, ENTANGLE, ANCIENT_ICE, TELEPORT, VENGEANCE);

    /** Minigame ids (see {@code Minigame.id()}) where the sacks other than the teleport sack work. */
    public static final List<String> MINIGAMES = List.of("castle_wars", "soul_wars");

    private BlightedSacks() {
    }

    /**
     * The sack that pays for a spell, by its id ("fire_surge", "ice_barrage", "tele_block"; enum names such as
     * {@code WIND_WAVE} work too), or null when no sack covers it.
     */
    public static String sackFor(String spellId) {
        if (spellId == null) {
            return null;
        }
        String s = spellId.toLowerCase(Locale.ROOT);
        for (String element : new String[]{"wind", "water", "earth", "fire"}) {
            if (s.equals(element + "_surge") || s.equals(element + "_wave")) {
                return SURGE;
            }
        }
        return switch (s) {
            case "bind", "snare", "entangle" -> ENTANGLE;
            case "ice_rush", "ice_burst", "ice_blitz", "ice_barrage" -> ANCIENT_ICE;
            case "tele_block", "teleport_to_target" -> TELEPORT;
            case "vengeance", "vengeance_other" -> VENGEANCE;
            default -> null;
        };
    }

    /**
     * True if {@code sack} works for a player at {@code wildernessLevel} (0 outside the Wilderness) who is in the minigame
     * {@code minigameId} (null if none).
     */
    public static boolean usable(String sack, int wildernessLevel, String minigameId) {
        if (sack == null) {
            return false;
        }
        if (wildernessLevel > 0) {
            return true;
        }
        return !sack.equals(TELEPORT) && minigameId != null && MINIGAMES.contains(minigameId);
    }
}
