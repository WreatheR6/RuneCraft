package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.PlaceDef.Style;

import java.util.List;

/**
 * Lairs at OSRS world coordinates. The God Wars Dungeon itself is carved at its real tiles ({@link GwdDungeon}); its only
 * place is the frozen door to the Ancient Prison, where the quests module's Ashuelot Reis starts The Frozen Door (style
 * NONE: the dungeon builds the room). Wilderness lairs sit at their real (or, where the boss lives in a cave, the cave entrance's) coordinates;
 * all are north of the ditch (y &gt; 3520).
 */
public final class GwdPlaces {
    private GwdPlaces() {
    }

    /**
     * A follower camp: the module keeps up to {@code cap} of these creatures alive within
     * {@code radius} blocks of the place centre (+dx/dz blocks) while a player is nearby.
     */
    public record Camp(String place, int dx, int dz, int radius, int cap, List<String> creatures) {
    }

    public static List<PlaceDef> all() {
        return List.of(
                // Before the frozen door to the Ancient Prison, where The Frozen Door starts (Ashuelot Reis stands 3 blocks
                // east, 3 north: tile 2884, 5282 of the main dungeon).
                PlaceDef.of("gwd_ancient_prison", "The Frozen Door", 2883, 5280, Style.NONE, List.of()),
                // Wilderness.
                PlaceDef.of("callisto_den", "Callisto's Den", 3292, 3848, Style.RUINS, List.of()),
                PlaceDef.of("hunters_end", "Hunter's End", 3165, 3705, Style.CAVE, List.of()), // approx. (cave entrance)
                PlaceDef.of("venenatis_nest", "Venenatis' Nest", 3330, 3742, Style.RUINS, List.of()),
                PlaceDef.of("silk_chasm", "Silk Chasm", 3285, 3730, Style.CAVE, List.of()), // approx. (cave entrance)
                PlaceDef.of("vetion_ruins", "Vet'ion's Ruins", 3220, 3790, Style.RUINS, List.of()),
                PlaceDef.of("skeletal_tomb", "Skeletal Tomb", 3250, 3820, Style.CAVE, List.of()), // approx. (tomb entrance)
                PlaceDef.of("scorpias_cave", "Scorpia's Cave", 3232, 3942, Style.CAVE, List.of()),
                PlaceDef.of("rogues_castle", "Rogues' Castle", 3266, 3912, Style.RUINS, List.of()),
                PlaceDef.of("chaos_fanatic_altar", "Chaos Fanatic's Altar", 2980, 3848, Style.RUINS, List.of()),
                PlaceDef.of("crazy_archaeologist_ruins", "Crazy Archaeologist's Dig", 2975, 3700, Style.RUINS, List.of()),
                PlaceDef.of("kbd_lair", "King Black Dragon Lair", 3065, 3860, Style.CAVE, List.of()), // Lava Maze entrance
                // The level 40 cavern into the Revenant Caves (the caves and their entrances are RevenantCaves' structures).
                PlaceDef.of("revenant_caves", "Revenant Caves", 3127, 3833, Style.NONE, List.of()),
                // Above the Emblem Trader's spot in the caves (he stands on the cave floor below, NpcStoreys).
                PlaceDef.of(RevenantCaves.EMBLEM_TRADER_PLACE, "Revenant Caves (Emblem Trader)", RevenantCaves.EMBLEM_TRADER[0],
                        RevenantCaves.EMBLEM_TRADER[1] + RevenantCaves.DY, Style.NONE, List.of()));
    }

    /**
     * Place camps (none any more: the God Wars followers stand at their OSRS tiles, {@link GwdSpawns}, and the revenants on
     * theirs in the carved Revenant Caves, {@link RevenantCaves#SPAWNS}).
     */
    public static List<Camp> camps() {
        return List.of();
    }
}
