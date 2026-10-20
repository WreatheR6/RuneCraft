package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.PlaceDef.Style;

import java.util.List;

/**
 * Lairs at OSRS world coordinates. The God Wars Dungeon itself is carved at its real tiles ({@link GwdDungeon}); its only
 * place is the Ancient Prison's refuge, where the quests module's Ashuelot Reis stands (style NONE: the dungeon builds
 * the room). Wilderness lairs sit at their real (or, where the boss lives in a cave, the cave entrance's) coordinates;
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
                // The Ancient Prison's refuge before Nex's chamber (Ashuelot Reis stands 3 blocks east, 3 north).
                PlaceDef.of("gwd_ancient_prison", "The Ancient Prison", 2902, 5202, Style.NONE, List.of()),
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
                PlaceDef.of("revenant_caves", "Revenant Caves", 3127, 3833, Style.CAVE, List.of()));
    }

    /** Place camps: the revenants (the God Wars followers stand at their OSRS tiles, {@link GwdSpawns}). */
    public static List<Camp> camps() {
        return List.of(
                new Camp("revenant_caves", 0, 0, 16, 8, List.of("revenant_imp", "revenant_goblin", "revenant_pyrefiend",
                        "revenant_hobgoblin", "revenant_cyclops", "revenant_hellhound", "revenant_demon", "revenant_ork",
                        "revenant_dark_beast", "revenant_knight", "revenant_dragon")));
    }
}
