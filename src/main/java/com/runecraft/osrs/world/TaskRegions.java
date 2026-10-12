package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.dungeons.Dungeons;

import java.util.Map;

/**
 * The kingdom a kill counts in for Konar quo Maten's location-restricted tasks. On the surface it is
 * {@link GielinorMap#regionAtBlock}; inside a carved dungeon it is the kingdom that dungeon belongs to in
 * OSRS (Konar names the dungeon, e.g. "the Fremennik Slayer Dungeon"), because several dungeons are
 * carved beneath a neighbouring kingdom's surface: the Fremennik Slayer Dungeon lies under Burthorpe's
 * Warriors' Guild, and Taverley Dungeon reaches west under White Wolf Mountain. Zanaris counts as
 * Lumbridge Swamp.
 */
public final class TaskRegions {
    /** Dungeon id to its OSRS kingdom. Dungeons not listed (Edgeville, the Abyss, Zeah...) use the surface map. */
    private static final Map<String, Region> DUNGEONS = Map.ofEntries(
            Map.entry("dng_varrock_sewers", Region.MISTHALIN),
            Map.entry("dng_stronghold_of_security", Region.MISTHALIN),
            Map.entry("dng_lumbridge_cellar_caves", Region.MISTHALIN),
            Map.entry("dng_lumbridge_swamp_caves", Region.LUMBRIDGE_SWAMP),
            Map.entry("dng_taverley_dungeon", Region.ASGARNIA),
            Map.entry("dng_dwarven_mine", Region.ASGARNIA),
            Map.entry("dng_asgarnian_ice_dungeon", Region.ASGARNIA),
            Map.entry("dng_stronghold_slayer_cave", Region.KANDARIN),
            Map.entry("dng_waterfall_dungeon", Region.KANDARIN),
            Map.entry("dng_ancient_cavern", Region.KANDARIN),
            Map.entry("dng_kraken_cove", Region.KANDARIN),
            Map.entry("dng_smoke_devil_dungeon", Region.KANDARIN),
            Map.entry("dng_fremennik_slayer_dungeon", Region.FREMENNIK_PROVINCE),
            Map.entry("dng_iorwerth_dungeon", Region.TIRANNWN),
            Map.entry("dng_brimhaven_dungeon", Region.KARAMJA),
            Map.entry("dng_tzhaar_city", Region.KARAMJA),
            Map.entry("dng_kalphite_lair", Region.KHARIDIAN_DESERT),
            Map.entry("dng_smoke_dungeon", Region.KHARIDIAN_DESERT),
            Map.entry("dng_slayer_tower_basement", Region.MORYTANIA),
            Map.entry("dng_barrows", Region.MORYTANIA),
            Map.entry("dng_tarns_lair", Region.MORYTANIA),
            Map.entry("dng_shade_catacombs", Region.MORYTANIA),
            Map.entry("dng_brine_rat_cavern", Region.FREMENNIK_PROVINCE),
            Map.entry("dng_jormungands_prison", Region.FREMENNIK_PROVINCE),
            Map.entry("dng_sourhog_cave", Region.MISTHALIN),
            Map.entry("dng_legends_guild_dungeon", Region.KANDARIN),
            Map.entry("dng_wilderness_god_wars", Region.WILDERNESS));

    /** Blocks around a camp's box that still count as that dungeon. */
    private static final int MARGIN = 8;

    private TaskRegions() {
    }

    /** The OSRS kingdom of a built dungeon, or null when the surface map decides. */
    public static Region ofDungeon(String dungeonId) {
        return DUNGEONS.get(dungeonId);
    }

    /** Kingdom at a block position, counting the inside of a carved dungeon as that dungeon's kingdom. */
    public static Region at(int blockX, int blockY, int blockZ) {
        for (Dungeons.Camp c : Dungeons.camps()) {
            Region r = DUNGEONS.get(c.dungeon());
            if (r != null && blockX >= Math.min(c.x1(), c.x2()) - MARGIN && blockX <= Math.max(c.x1(), c.x2()) + MARGIN
                    && blockZ >= Math.min(c.z1(), c.z2()) - MARGIN && blockZ <= Math.max(c.z1(), c.z2()) + MARGIN
                    && blockY >= c.feetY() - 6 && blockY <= c.feetY() + 16) {
                return r;
            }
        }
        if (inZanaris(GielinorMap.toOsrsX(blockX), GielinorMap.toOsrsY(blockZ))) {
            return Region.LUMBRIDGE_SWAMP;
        }
        return GielinorMap.regionAtBlock(blockX, blockZ);
    }

    /**
     * Zanaris lies at its OSRS instance coordinates far north of the map; Konar counts it with Lumbridge
     * Swamp, where its door (the shed) is.
     */
    static boolean inZanaris(double osrsX, double osrsY) {
        double dx = (osrsX - 2412) / 60.0;
        double dy = (osrsY - 4432) / 56.0;
        return dx * dx + dy * dy <= 1;
    }

    /** Kingdom a camp's monsters count in: its dungeon's kingdom, else the map at the camp's centre. */
    public static Region ofCamp(SpawnTable.CampRef camp) {
        Region r = DUNGEONS.get(camp.source());
        if (r != null) {
            return r;
        }
        return inZanaris(camp.x(), camp.y()) ? Region.LUMBRIDGE_SWAMP : GielinorMap.regionAt(camp.x(), camp.y());
    }
}
