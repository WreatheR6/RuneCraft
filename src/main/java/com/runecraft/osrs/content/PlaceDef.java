package com.runecraft.osrs.content;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Station;

import java.util.List;

/**
 * A module-defined location in Gielinor at OSRS coordinates. The landmark builder generates it the
 * first time its chunk loads: a plaza with the listed stations and module blocks, plus anything a
 * module registers with {@code LandmarkBuilder.registerCustom(id, builder)}.
 */
public record PlaceDef(String id, String name, int osrsX, int osrsY, Style style, List<Station> stations,
                       List<String> blocks) {

    public enum Style { TOWN, RUINS, CAVE, ARENA, CAMP, NONE }

    public static PlaceDef town(String id, String name, int osrsX, int osrsY, Station... stations) {
        return new PlaceDef(id, name, osrsX, osrsY, Style.TOWN, List.of(stations), List.of());
    }

    public static PlaceDef of(String id, String name, int osrsX, int osrsY, Style style, List<String> blocks) {
        return new PlaceDef(id, name, osrsX, osrsY, style, List.of(), blocks);
    }

    public int blockX() {
        return ((GielinorMap.toBlockX(osrsX) >> 4) << 4) + 8;
    }

    public int blockZ() {
        return ((GielinorMap.toBlockZ(osrsY) >> 4) << 4) + 8;
    }
}
