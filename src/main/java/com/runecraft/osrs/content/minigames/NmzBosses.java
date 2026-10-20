package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Nightmare Zone's fifty quest bosses (OSRS Wiki "Nightmare Zone" boss table): the quest that unlocks each, the points a
 * kill pays in a normal and a hard dream with every boss enabled, and every form's Normal and Hard Mode stats from the
 * "&lt;boss&gt; (Nightmare Zone)" infoboxes (weirdgloop/osrs-dps-calc {@code monsters.json}). Witch's experiment (four forms),
 * Nazastarool (three) and Damis (two) are fought form after form; only the last form's death counts as the kill.
 * Locked by {@code NmzBossesTest} against {@code src/test/resources/osrs/minigames/nmz_boss_stats.tsv} and
 * {@code nmz_bosses.tsv}.
 */
public final class NmzBosses {
    /**
     * One form's stats, normal and hard. Hard mode raises hitpoints, Attack, Strength, Magic and Ranged and the max hit; the
     * Defence and defensive bonuses stay (wiki "Game modes"). {@code styles} are the infobox's attack styles ("Crush",
     * "Magic", "Ranged", "Dragonfire", "Magic_melee"...); {@code size} is in tiles.
     */
    public record Form(String id, int level, int hardLevel, int size, int speed, int hardSpeed, List<String> styles,
                       int hitpoints, int attack, int strength, int defence, int magic, int ranged, int maxHit, String hitStyle,
                       int hardHitpoints, int hardAttack, int hardStrength, int hardDefence, int hardMagic, int hardRanged,
                       int hardMaxHit, int stabDefence, int slashDefence, int crushDefence, int magicDefence, int rangedDefence,
                       int meleeAttackBonus, int magicAttackBonus, int rangedAttackBonus, List<String> attributes) {
        /** Whether the attack the infobox max hit is given for is melee ({@link #hitStyle} Stab, Slash or Crush). */
        public boolean meleeMain() {
            return !hitStyle.equals("Magic") && !hitStyle.equals("Ranged");
        }
    }

    /** A boss: the quest that unlocks it, its points (normal / hard, every boss enabled) and its forms in fighting order. */
    public record Boss(String id, String name, String quest, int points, int hardPoints, List<Form> forms) {
        public Form first() {
            return forms.get(0);
        }

        public Form last() {
            return forms.get(forms.size() - 1);
        }
    }

    /**
     * id, level, hard level, size, speed, hard speed, styles, hitpoints, attack, strength, defence, magic, ranged, max hit,
     * the style the max hit is for, the same seven hard, stab/slash/crush/magic/ranged defence, melee/magic/ranged attack
     * bonus, attributes (comma-separated, "-" for none; the vampyre tiers as "vampyre").
     */
    private static final String[] FORMS = {
            "nmz_trapped_soul 30 101 1 4 4 Crush 30 25 29 20 1 1 4 Crush 98 100 120 20 1 1 13 0 0 0 0 0 0 0 0 undead",
            "nmz_count_draynor 34 177 1 4 4 Crush 35 30 25 30 1 1 3 Crush 210 198 165 30 1 1 17 2 1 3 0 0 0 0 0 vampyre",
            "nmz_corsair_traitor 35 103 1 5 3 Magic 55 1 1 30 30 1 6 Magic 160 1 1 80 90 1 19 0 0 0 0 0 0 0 0 -",
            "nmz_sand_snake 36 154 3 4 4 Crush 60 30 20 20 1 1 3 Crush 180 180 140 20 1 1 15 0 0 0 0 0 0 0 0 -",
            "nmz_corrupt_lizardman 46 152 1 4 4 Crush|Ranged 50 38 38 38 1 38 6 Crush 150 162 162 38 1 162 22 -10 25 0 0 0 22 0 22 -",
            "nmz_king_roald 47 188 1 4 4 Crush 75 35 30 30 1 1 3 Crush 150 140 120 30 1 1 9 0 0 0 0 0 0 0 0 -",
            "nmz_witchs_experiment 19 47 1 4 4 Crush 21 18 10 19 1 1 2 Crush 63 54 30 19 1 1 4 0 0 0 0 0 0 0 0 -",
            "nmz_witchs_experiment_spider 30 77 1 4 4 Crush 31 28 20 29 1 1 3 Crush 93 84 60 29 1 1 7 0 0 0 0 0 0 0 0 -",
            "nmz_witchs_experiment_bear 42 90 2 4 4 Slash 41 38 30 39 1 1 4 Slash 102 95 75 39 1 1 8 0 0 0 0 0 0 0 0 -",
            "nmz_witchs_experiment_wolf 53 103 2 4 4 Stab 51 48 40 49 1 1 5 Stab 113 105 88 49 1 1 10 0 0 0 0 0 0 0 0 -",
            "nmz_the_kendal 70 210 2 4 4 Crush 50 65 65 60 0 0 9 Crush 150 195 195 60 0 0 24 10 10 10 -10 20 5 0 0 -",
            "nmz_me 79 201 1 4 4 Crush 45 75 78 74 1 1 9 Crush 135 225 234 74 1 1 24 0 0 0 0 0 0 0 0 -",
            "nmz_headless_beast 82 233 3 4 4 Slash 100 60 86 42 42 1 10 Slash 320 200 240 42 42 1 25 18 18 18 6 52 0 0 0 undead",
            "nmz_elvarg 83 214 4 4 4 Slash|Magic|Dragonfire 80 70 70 70 70 1 8 Slash 240 210 210 70 210 1 22 20 40 40 30 20 0 0 0 -",
            "nmz_moss_guardian 84 182 2 6 6 Crush|Magic_melee 120 60 60 60 1 1 14 Crush 240 165 165 60 1 1 34 0 0 0 0 0 66 0 0 -",
            "nmz_slagilith 92 202 2 4 4 Crush 60 60 120 75 1 1 13 Crush 150 150 300 75 1 1 31 50 50 5 5 50 10 0 0 -",
            "nmz_nazastarool 91 176 2 4 4 Slash 70 85 80 80 0 0 9 Slash 154 187 176 80 0 0 19 0 0 0 0 0 0 0 0 -",
            "nmz_nazastarool_skeleton 68 153 2 4 4 Slash 70 58 54 58 0 0 8 Slash 180 150 140 58 0 0 18 5 5 5 5 5 15 0 0 -",
            "nmz_nazastarool_ghost 93 181 2 4 4 Slash 80 85 80 80 0 0 9 Slash 176 187 176 80 0 0 19 0 0 0 0 0 0 0 0 spectral",
            "nmz_treus_dayth 95 194 1 4 4 Crush 100 70 70 100 1 1 8 Crush 240 168 168 100 1 1 18 5 5 5 -5 5 0 0 0 spectral",
            "nmz_skeleton_hellhound 97 198 2 4 4 Crush 55 70 110 100 1 1 12 Crush 132 168 264 100 1 1 27 0 0 0 0 0 0 0 0 demon",
            "nmz_dagannoth_mother 100 201 3 4 4 Stab|Ranged 120 78 78 81 1 50 9 Stab 240 186 186 81 1 120 20 150 150 150 50 50 0 0 0 -",
            "nmz_agrith_naar 100 196 3 4 4 Crush|Magic 95 83 90 82 100 100 10 Crush 209 182 198 82 220 220 21 0 0 0 0 0 0 0 0 demon",
            "nmz_tree_spirit 101 199 1 4 4 Crush 85 90 95 80 1 1 10 Crush 187 200 209 80 1 1 22 0 0 0 0 0 0 0 0 spectral",
            "nmz_dad 101 201 3 8 8 Crush 120 60 120 50 0 0 27 Crush 240 132 264 50 0 0 57 25 25 40 200 200 40 0 0 -",
            "nmz_tanglefoot 111 199 1 4 4 Slash 102 97 97 91 1 1 11 Slash 204 194 194 91 1 1 20 0 0 0 0 0 0 0 0 -",
            "nmz_khazard_warlord 112 192 1 4 4 Crush 170 75 78 80 1 1 10 Crush 255 165 170 80 1 1 18 0 0 0 0 0 0 0 0 -",
            "nmz_arrg 113 210 2 5 5 Ranged 140 70 140 40 0 70 38 Crush 255 140 280 40 0 140 74 35 60 35 200 200 60 0 0 -",
            "nmz_black_knight_titan 120 210 1 7 7 Crush 142 91 100 91 1 1 15 Crush 255 182 200 91 1 1 28 18 27 18 1000 1000 27 0 0 -",
            "nmz_ice_troll_king 122 213 2 4 4 Crush 150 100 100 80 1 1 21 Crush 255 200 200 80 1 1 40 45 45 45 2000 2000 60 0 0 -",
            "nmz_bouncer 137 244 2 4 4 Stab 116 120 120 120 1 1 13 Stab 232 240 240 120 1 1 25 0 0 0 0 0 0 0 0 demon",
            "nmz_glod 138 276 4 4 4 Crush 160 115 120 110 1 1 13 Crush 255 230 240 110 1 1 25 105 110 130 125 80 0 0 0 -",
            "nmz_evil_chicken 159 286 1 4 4 Magic 120 1 1 126 200 1 21 Magic 240 0 0 126 400 1 21 0 0 0 0 0 0 0 0 -",
            "nmz_agrith_na_na 146 235 3 4 4 Magic 200 83 150 82 100 100 16 Crush 255 166 300 82 200 200 31 100 100 60 100 100 100 0 0 -",
            "nmz_flambeed 149 238 5 4 4 Crush|Magic 210 120 120 75 1 1 13 Crush 255 240 240 75 1 1 25 50 50 5 5 30 100 0 0 -",
            "nmz_karamel 136 186 1 3 3 Magic|Ranged 250 1 1 100 1 100 7 Magic 255 1 1 100 1 200 7 150 150 150 150 150 0 0 134 -",
            "nmz_dessourt 121 217 1 3 3 Crush|Magic|Ranged 130 99 99 99 1 1 19 Crush 255 198 198 99 1 1 37 10 150 150 0 0 50 0 0 -",
            "nmz_gelatinnoth_mother 130 201 3 4 4 Ranged|Magic|Stab 240 78 78 81 1 50 9 Stab 240 186 186 81 1 120 20 150 150 150 50 50 0 0 0 -",
            "nmz_culinaromancer 75 209 1 4 4 Magic 150 10 100 10 1 1 25 Magic 255 40 400 10 1 1 25 -10 -10 -10 -10 -10 0 0 0 -",
            "nmz_chronozon 170 297 3 4 4 Slash 60 173 172 173 1 1 18 Slash 120 346 344 173 1 1 35 0 0 0 0 0 0 0 0 demon",
            "nmz_black_demon 172 292 3 4 4 Slash 157 145 148 152 1 1 16 Slash 255 290 296 152 1 1 31 0 0 0 -10 0 0 0 0 demon",
            "nmz_giant_roc 172 257 5 6 6 Crush|Ranged 250 130 130 100 1 130 14 Crush 255 260 260 100 1 260 27 0 0 0 150 0 0 0 0 -",
            "nmz_dessous 139 217 1 3 3 Crush|Magic|Ranged 200 99 99 99 1 1 19 Crush 255 198 198 99 1 1 37 10 150 150 0 0 50 0 0 vampyre",
            "nmz_damis 103 200 1 4 4 Crush 90 90 90 90 1 1 22 Crush 198 198 198 90 1 1 47 60 60 60 60 60 0 0 0 -",
            "nmz_damis_second 174 272 1 3 3 Crush 200 160 100 160 1 1 28 Crush 255 320 200 160 1 1 54 100 100 100 80 120 0 0 0 -",
            "nmz_fareed 167 299 1 4 4 Slash|Magic 130 190 120 135 1 1 37 Slash 255 380 240 135 1 1 72 100 100 100 0 0 0 0 0 -",
            "nmz_kamil 154 273 1 4 4 Crush|Magic 130 190 80 135 1 1 23 Crush 255 380 160 135 1 1 43 35 60 35 0 0 60 0 0 -",
            "nmz_nezikchened 187 295 3 5 5 Magic 150 165 168 167 160 160 18 Magic 150 330 336 167 320 320 35 0 0 0 0 0 0 0 0 demon",
            "nmz_barrelchest 190 380 3 5 5 Crush 134 170 145 140 90 1 35 Crush 255 306 261 140 162 1 61 0 0 0 0 0 80 0 0 -",
            "nmz_giant_scarab 191 316 3 4 4 Stab|Ranged 130 169 190 169 1 190 20 Stab 255 305 342 169 1 342 35 70 99 99 159 103 0 0 0 kalphite",
            "nmz_jungle_demon 195 327 3 6 6 Slash|Magic 170 170 170 170 170 1 32 Slash 255 340 340 170 340 1 62 0 50 0 50 0 50 0 0 demon",
            "nmz_elven_traitor 212 378 1 5 5 Ranged 300 0 0 102 102 368 38 Ranged 300 0 0 102 102 552 56 80 80 80 260 180 0 0 84 -",
            "nmz_essyllt 236 395 1 4 4 Stab|Ranged 320 268 268 104 104 342 40 Stab 320 408 408 104 104 512 60 40 40 20 30 120 0 0 0 -",
            "nmz_the_untouchable 274 440 2 6 6 Crush 90 187 255 434 1 1 26 Crush 180 374 510 434 1 1 52 0 0 0 0 0 0 0 0 -",
            "nmz_the_everlasting 223 365 3 6 6 Crush 230 187 231 120 1 1 24 Crush 255 374 462 120 1 1 47 0 0 0 0 0 0 0 0 -",
            "nmz_the_inadequacy 343 600 5 4 4 Crush|Ranged 180 564 170 240 1 170 18 Crush 255 1128 340 240 1 340 35 0 0 0 0 0 0 0 0 -"
    };

    /** id, name, quest, points normal, points hard, then the ids of the later forms. */
    private static final String[][] BOSSES = {
            {"nmz_trapped_soul", "Trapped Soul", "the_ascent_of_arceuus", "183", "2301"},
            {"nmz_count_draynor", "Count Draynor", "VAMPYRE_SLAYER", "224", "7135"},
            {"nmz_corsair_traitor", "Corsair Traitor", "the_corsair_curse", "244", "2301"},
            {"nmz_sand_snake", "Sand Snake", "the_depths_of_despair", "244", "5294"},
            {"nmz_corrupt_lizardman", "Corrupt Lizardman", "tale_of_the_righteous", "427", "5294"},
            {"nmz_king_roald", "King Roald", "what_lies_below", "448", "8056"},
            {"nmz_witchs_experiment", "Witch's experiment", "witchs_house", "570", "2301", "nmz_witchs_experiment_spider",
                    "nmz_witchs_experiment_bear", "nmz_witchs_experiment_wolf"},
            {"nmz_the_kendal", "The Kendal", "mountain_daughter", "998", "10127"},
            {"nmz_me", "Me", "lunar_diplomacy", "1263", "9207"},
            {"nmz_headless_beast", "Headless Beast", "getting_ahead", "1365", "12429"},
            {"nmz_elvarg", "Elvarg", "dragon_slayer_i", "1385", "10357"},
            {"nmz_moss_guardian", "Moss Guardian", "roving_elves", "1426", "7595"},
            {"nmz_slagilith", "Slagilith", "one_small_favour", "1711", "9207"},
            {"nmz_nazastarool", "Nazastarool", "shilo_village", "1752", "7365", "nmz_nazastarool_skeleton", "nmz_nazastarool_ghost"},
            {"nmz_treus_dayth", "Treus Dayth", "haunted_mine", "1834", "8516"},
            {"nmz_skeleton_hellhound", "Skeleton Hellhound", "in_search_of_the_myreque", "1915", "8976"},
            {"nmz_dagannoth_mother", "Dagannoth mother", "horror_from_the_deep", "2038", "9207"},
            {"nmz_agrith_naar", "Agrith-Naar", "shadow_of_the_storm", "2038", "8746"},
            {"nmz_tree_spirit", "Tree spirit", "lost_city", "2078", "8976"},
            {"nmz_dad", "Dad", "troll_stronghold", "2078", "9207"},
            {"nmz_tanglefoot", "Tanglefoot", "fairytale_i_growing_pains", "2506", "8976"},
            {"nmz_khazard_warlord", "Khazard warlord", "tree_gnome_village", "2547", "8286"},
            {"nmz_arrg", "Arrg", "troll_romance", "2588", "10127"},
            {"nmz_black_knight_titan", "Black Knight Titan", "holy_grail", "2934", "10127"},
            {"nmz_ice_troll_king", "Ice Troll King", "the_fremennik_isles", "3016", "10357"},
            {"nmz_bouncer", "Bouncer", "fight_arena", "3811", "13580"},
            {"nmz_glod", "Glod", "grim_tales", "3872", "17493"},
            // The Evil Chicken is unlocked by Freeing Sir Amik Varze, the others by the final battle; RuneCraft tracks Recipe
            // for Disaster as one quest, so all seven wait for it. approx
            {"nmz_evil_chicken", "Evil Chicken", "recipe_for_disaster", "5135", "18644"},
            {"nmz_agrith_na_na", "Agrith-Na-Na", "recipe_for_disaster", "4341", "12659"},
            {"nmz_flambeed", "Flambeed", "recipe_for_disaster", "4524", "12889"},
            {"nmz_karamel", "Karamel", "recipe_for_disaster", "3750", "7825"},
            {"nmz_dessourt", "Dessourt", "recipe_for_disaster", "2975", "10818"},
            {"nmz_gelatinnoth_mother", "Gelatinnoth Mother", "recipe_for_disaster", "3444", "9207"},
            {"nmz_culinaromancer", "Culinaromancer", "recipe_for_disaster", "1141", "9897"},
            {"nmz_chronozon", "Chronozon", "family_crest", "5890", "20255"},
            {"nmz_black_demon", "Black demon", "the_grand_tree", "6012", "19564"},
            {"nmz_giant_roc", "Giant Roc", "my_arms_big_adventure", "6012", "15191"},
            {"nmz_dessous", "Dessous", "desert_treasure_i", "3933", "10818"},
            {"nmz_damis", "Damis", "desert_treasure_i", "6155", "16802", "nmz_damis_second"},
            {"nmz_fareed", "Fareed", "desert_treasure_i", "5665", "20485"},
            {"nmz_kamil", "Kamil", "desert_treasure_i", "4830", "17032"},
            {"nmz_nezikchened", "Nezikchened", "legends_quest", "7112", "20025"},
            {"nmz_barrelchest", "Barrelchest", "the_great_brain_robbery", "7357", "33145"},
            {"nmz_giant_scarab", "Giant Scarab", "contact", "7418", "22787"},
            {"nmz_jungle_demon", "Jungle Demon", "monkey_madness_i", "7744", "24398"},
            {"nmz_elven_traitor", "Elven traitor", "song_of_the_elves", "6746", "31073"},
            {"nmz_essyllt", "Essyllt", "song_of_the_elves", "22662", "71814"},
            {"nmz_the_untouchable", "The Untouchable", "dream_mentor", "15285", "44423"},
            {"nmz_the_everlasting", "The Everlasting", "dream_mentor", "10129", "30613"},
            {"nmz_the_inadequacy", "The Inadequacy", "dream_mentor", "23967", "82863"}};

    private static final Map<String, Form> FORM_BY_ID = new LinkedHashMap<>();
    private static final Map<String, Boss> BY_ID = new LinkedHashMap<>();
    /** Every form id (later forms included) to its boss. */
    private static final Map<String, Boss> BY_FORM = new LinkedHashMap<>();
    /** Every boss, in the wiki table's order. */
    public static final List<Boss> ALL;

    static {
        for (String row : FORMS) {
            String[] f = row.split(" ");
            int[] n = new int[f.length];
            for (int i = 1; i < f.length; i++) {
                if (i != 6 && i != 14 && i != 30) {
                    n[i] = Integer.parseInt(f[i]);
                }
            }
            Form form = new Form(f[0], n[1], n[2], n[3], n[4], n[5], List.of(f[6].split("\\|")), n[7], n[8], n[9], n[10], n[11],
                    n[12], n[13], f[14], n[15], n[16], n[17], n[18], n[19], n[20], n[21], n[22], n[23], n[24], n[25], n[26], n[27],
                    n[28], n[29], f[30].equals("-") ? List.of() : List.of(f[30].split(",")));
            FORM_BY_ID.put(form.id(), form);
        }
        List<Boss> all = new ArrayList<>();
        for (String[] b : BOSSES) {
            List<Form> forms = new ArrayList<>();
            forms.add(requireForm(b[0]));
            for (int i = 5; i < b.length; i++) {
                forms.add(requireForm(b[i]));
            }
            Boss boss = new Boss(b[0], b[1], b[2], Integer.parseInt(b[3]), Integer.parseInt(b[4]), List.copyOf(forms));
            all.add(boss);
            BY_ID.put(boss.id(), boss);
            for (Form f : forms) {
                BY_FORM.put(f.id(), boss);
            }
        }
        ALL = Collections.unmodifiableList(all);
    }

    private static Form requireForm(String id) {
        Form f = FORM_BY_ID.get(id);
        if (f == null) {
            throw new IllegalStateException("No Nightmare Zone form " + id);
        }
        return f;
    }

    private NmzBosses() {
    }

    /** The boss with this id (its first form's id), or null. */
    public static Boss get(String id) {
        return BY_ID.get(id);
    }

    /** The boss a creature id (any of its forms) belongs to, or null. */
    public static Boss ofForm(String formId) {
        return formId == null ? null : BY_FORM.get(formId);
    }

    /** One form's stats, or null. */
    public static Form form(String formId) {
        return formId == null ? null : FORM_BY_ID.get(formId);
    }

    /** Every form, in table order. */
    public static List<Form> forms() {
        return List.copyOf(FORM_BY_ID.values());
    }

    /** The form fought after {@code formId}, or null for a boss's last (or only) form. */
    public static String nextForm(String formId) {
        Boss b = ofForm(formId);
        if (b == null) {
            return null;
        }
        for (int i = 0; i < b.forms().size() - 1; i++) {
            if (b.forms().get(i).id().equals(formId)) {
                return b.forms().get(i + 1).id();
            }
        }
        return null;
    }
}
