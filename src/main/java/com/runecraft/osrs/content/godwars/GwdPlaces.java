package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.PlaceDef.Style;

import java.util.List;

/**
 * Lairs at OSRS world coordinates. The God Wars chambers are laid out on the surface around the
 * built-in God Wars Dungeon landmark near Trollheim (the real dungeon is an underground instance).
 * Wilderness lairs sit at their real (or, where the boss lives in a cave, the cave entrance's)
 * coordinates; all are north of the ditch (y &gt; 3520).
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
                // God Wars Dungeon chambers (Trollheim, around the landmark at 2917, 3745).
                PlaceDef.of("gwd_armadyl_eyrie", "Armadyl's Eyrie", 2865, 3765, Style.CAVE, List.of()),
                PlaceDef.of("gwd_saradomin_encampment", "Saradomin's Encampment", 2865, 3720, Style.CAVE, List.of()),
                PlaceDef.of("gwd_ancient_prison", "The Ancient Prison", 2915, 3705, Style.CAVE, List.of()),
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

    /** Followers around each chamber (Bandos and Zamorak sit around the built-in GWD landmark). */
    public static List<Camp> camps() {
        return List.of(
                new Camp("gwd_armadyl_eyrie", 0, 0, 14, 7, List.of("gwd_aviansie", "gwd_aviansie", "gwd_aviansie_elite",
                        "gwd_spiritual_warrior_armadyl", "gwd_spiritual_ranger_armadyl", "gwd_spiritual_mage_armadyl")),
                new Camp("gwd_saradomin_encampment", 0, 0, 14, 7, List.of("gwd_knight_of_saradomin", "gwd_knight_of_saradomin",
                        "gwd_saradomin_priest", "gwd_spiritual_warrior_saradomin", "gwd_spiritual_ranger_saradomin",
                        "gwd_spiritual_mage_saradomin")),
                new Camp("GOD_WARS_DUNGEON", -40, -30, 14, 8, List.of("gwd_goblin", "gwd_goblin", "gwd_hobgoblin", "gwd_ork",
                        "gwd_ogre", "gwd_jogre", "gwd_cyclops", "gwd_spiritual_warrior_bandos", "gwd_spiritual_ranger_bandos",
                        "gwd_spiritual_mage_bandos")),
                new Camp("GOD_WARS_DUNGEON", -40, 30, 14, 8, List.of("gwd_imp", "gwd_imp", "gwd_werewolf", "gwd_bloodveld",
                        "gwd_hellhound", "gwd_gorak", "gwd_vampire", "gwd_pyrefiend", "gwd_spiritual_warrior_zamorak",
                        "gwd_spiritual_ranger_zamorak", "gwd_spiritual_mage_zamorak")),
                // Graardor's and K'ril's bodyguards (the built-in generals don't summon them).
                new Camp("GOD_WARS_DUNGEON", 0, -40, 8, 3, List.of("sergeant_strongstack", "sergeant_steelwill", "sergeant_grimspike")),
                new Camp("GOD_WARS_DUNGEON", 0, 40, 8, 3, List.of("tstanon_karlak", "zakln_gritch", "balfrug_kreeyath")),
                new Camp("revenant_caves", 0, 0, 16, 8, List.of("revenant_imp", "revenant_goblin", "revenant_pyrefiend",
                        "revenant_hobgoblin", "revenant_cyclops", "revenant_hellhound", "revenant_demon", "revenant_ork",
                        "revenant_dark_beast", "revenant_knight", "revenant_dragon")));
    }
}
