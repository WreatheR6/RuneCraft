package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Treasure Trails characters this world had no NPC for: the anagram, cipher and cryptic clue targets, Charlie the
 * Tramp, Cecilia and Sherlock, each standing at the tile RuneLite's cluescrolls plugin gives for the clue (Cecilia at
 * the wiki's tile). Like the npcs module's townsfolk, each 16x16 block chunk with clue NPCs gets an invisible
 * ({@link PlaceDef.Style#NONE}) place, so the NPC spawns when its chunk is generated. (Generated from the clue step
 * snapshot: the clue NPCs no module defined, on the surface.)
 */
public final class ClueNpcs {
    /** A clue NPC: id, name, OSRS tile, colours (skin, top, bottom, hat) and the clue that needs it. */
    public record ClueNpc(String id, String name, int x, int y, int skin, int top, int bottom, int hat, String clue) {
    }

    public static final String CHARLIE = "clue_charlie_the_tramp";
    public static final String CECILIA = "clue_cecilia";
    public static final String SHERLOCK = "clue_sherlock";

    public static final List<ClueNpc> ALL = List.of(
            new ClueNpc("clue_abbot_langley", "Abbot Langley", 3058, 3487, 0xA07850, 0x8A6A3A, 0x3A2A1A, 0x3A6A3A, "CRYPTIC/HARD"),
            new ClueNpc("clue_ali_the_kebab_seller", "Ali the Kebab seller", 3354, 2974, 0xC8A080, 0x5A3A6A, 0x3A2A1A, 0x2A2A3A, "CIPHER/MEDIUM"),
            new ClueNpc("clue_ali_the_leaflet_dropper", "Ali the Leaflet Dropper", 3283, 3329, 0xC8A080, 0x2A2A3A, 0x5A3A2A, 0x2A2A3A, "CRYPTIC/EASY"),
            new ClueNpc("clue_ambassador_spanfipple", "Ambassador Spanfipple", 2979, 3340, 0xA07850, 0x8A2A2A, 0x3A3A4A, 0x5A3A6A, "CRYPTIC/EASY"),
            new ClueNpc("clue_aris", "Aris", 3203, 3424, 0xA07850, 0x8A2A2A, 0x3A2A1A, 0x8A6A3A, "CRYPTIC/ELITE"),
            // Awowogei: Monkey Madness II's king on his throne (2803,2765, quests module) answers this anagram.
            new ClueNpc("clue_bolkoy", "Bolkoy", 2526, 3162, 0xA07850, 0x8A2A2A, 0x3A3A4A, 0xA08040, "ANAGRAM/HARD"),
            new ClueNpc("clue_brambickle", "Brambickle", 2783, 3861, 0xC8A080, 0x6A4A2A, 0x3A3A4A, 0x8A6A3A, "ANAGRAM/HARD"),
            new ClueNpc("clue_brother_tranquility", "Brother Tranquility", 3681, 2963, 0xC8A080, 0x8A6A3A, 0x5A3A2A, 0xA08040, "ANAGRAM/HARD"),
            new ClueNpc("clue_cam_the_camel", "Cam the Camel", 3300, 3231, 0xC8A080, 0x8A2A2A, 0x2A2A2A, 0x8A2A2A, "ANAGRAM/ELITE"),
            new ClueNpc("clue_cap_n_izzy_no_beard", "Cap'n Izzy No-Beard", 2807, 3191, 0xE8C0A0, 0xA08040, 0x3A3A4A, 0x3A6A9A, "ANAGRAM/HARD"),
            new ClueNpc("clue_captain_bruce", "Captain Bruce", 1529, 3567, 0xA07850, 0x3A6A3A, 0x2A2A2A, 0x8A2A2A, "ANAGRAM/MASTER"),
            new ClueNpc("clue_captain_khaled", "Captain Khaled", 1845, 3754, 0xC8A080, 0x2A2A3A, 0x3A2A1A, 0x6A4A2A, "CIPHER/MEDIUM"),
            new ClueNpc("clue_cecilia", "Cecilia", 2991, 3384, 0xE8C0A0, 0x6A4A2A, 0x5A3A2A, 0x8A6A3A, "MUSIC/EASY"),
            new ClueNpc("clue_charles", "Charles", 1821, 3690, 0xE8C0A0, 0x5A3A6A, 0x3A3A4A, 0x6A4A2A, "CRYPTIC/EASY"),
            new ClueNpc("clue_charlie_the_tramp", "Charlie the Tramp", 3209, 3392, 0xA07850, 0x3A6A9A, 0x3A3A4A, 0x2A2A3A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_clerris", "Clerris", 1761, 3850, 0xE8C0A0, 0x6A4A2A, 0x5A3A2A, 0x5A3A6A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_cook", "Cook", 3230, 3401, 0xA07850, 0x8A6A3A, 0x3A3A4A, 0x8A6A3A, "CRYPTIC/EASY"),
            new ClueNpc("clue_daer_krand", "Daer Krand", 3728, 3302, 0xE0B090, 0x8A2A2A, 0x5A3A2A, 0x2A2A3A, "ANAGRAM/HARD"),
            new ClueNpc("clue_dockmaster", "Dockmaster", 1822, 3739, 0xE8C0A0, 0x8A6A3A, 0x3A2A1A, 0xA08040, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_donovan_the_family_handyman", "Donovan the Family Handyman", 2737, 3580, 0xE8C0A0, 0x2A2A3A, 0x4A4A2A, 0xA08040, "CRYPTIC/MEDIUM"),
            new ClueNpc("clue_doris", "Doris", 3079, 3493, 0xE8C0A0, 0x8A2A2A, 0x2A2A2A, 0x5A3A6A, "CRYPTIC/EASY"),
            new ClueNpc("clue_drunken_soldier", "Drunken soldier", 1551, 3565, 0xE0B090, 0x6A4A2A, 0x3A3A4A, 0x3A6A9A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_dugopul", "Dugopul", 2803, 2744, 0xA07850, 0xA08040, 0x3A2A1A, 0x8A2A2A, "ANAGRAM/MASTER"),
            new ClueNpc("clue_elder_torgan", "Elder Torgan", 3143, 2423, 0xA07850, 0x8A2A2A, 0x3A2A1A, 0x6A4A2A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_eohric", "Eohric", 2897, 3565, 0xA07850, 0xA08040, 0x3A3A4A, 0x3A6A3A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_ermin", "Ermin", 2488, 3409, 0xC8A080, 0x2A2A3A, 0x3A3A4A, 0x3A6A9A, "CRYPTIC/EASY"),
            new ClueNpc("clue_ewesey", "Ewesey", 1646, 3631, 0xC8A080, 0x5A3A6A, 0x5A3A2A, 0x6A4A2A, "CRYPTIC/MASTER"),
            new ClueNpc("clue_father_jean", "Father Jean", 1734, 3576, 0xA07850, 0xA08040, 0x3A2A1A, 0x5A3A6A, "CRYPTIC/EASY"),
            new ClueNpc("clue_femi", "Femi", 2461, 3382, 0xE0B090, 0x5A3A6A, 0x5A3A2A, 0x3A6A3A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_flax_keeper", "Flax keeper", 2744, 3444, 0xE8C0A0, 0x8A2A2A, 0x5A3A2A, 0x6A4A2A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_fycie", "Fycie", 2630, 2997, 0xA07850, 0x5A3A6A, 0x3A3A4A, 0x6A4A2A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_gallow", "Gallow", 1805, 3566, 0xA07850, 0x6A4A2A, 0x5A3A2A, 0x8A6A3A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_gnome_coach", "Gnome Coach", 2395, 3486, 0xC8A080, 0xA08040, 0x4A4A2A, 0x5A3A6A, "ANAGRAM/HARD"),
            new ClueNpc("clue_gnome_ball_referee", "Gnome ball referee", 2386, 3487, 0xC8A080, 0xA08040, 0x3A2A1A, 0x3A6A9A, "CRYPTIC/MEDIUM"),
            new ClueNpc("clue_gnome_trainer", "Gnome trainer", 2476, 3428, 0xA07850, 0xA08040, 0x5A3A2A, 0x2A2A3A, "CRYPTIC/HARD"),
            new ClueNpc("clue_goreu", "Goreu", 2335, 3162, 0xA07850, 0xA08040, 0x5A3A2A, 0x2A2A3A, "ANAGRAM/MASTER"),
            new ClueNpc("clue_guard_vemmeldo", "Guard Vemmeldo", 2447, 3418, 0xC8A080, 0xA08040, 0x5A3A2A, 0x8A2A2A, "ANAGRAM/ELITE"),
            new ClueNpc("clue_guildmaster_lars", "Guildmaster Lars", 1649, 3498, 0xC8A080, 0x5A3A6A, 0x3A2A1A, 0x8A6A3A, "ANAGRAM/MASTER"),
            new ClueNpc("clue_hairdresser", "Hairdresser", 2944, 3381, 0xA07850, 0x8A2A2A, 0x5A3A2A, 0x8A6A3A, "ANAGRAM/BEGINNER"),
            new ClueNpc("clue_hajedy", "Hajedy", 2779, 3211, 0xE0B090, 0x3A6A9A, 0x3A2A1A, 0x3A6A9A, "CRYPTIC/MEDIUM"),
            new ClueNpc("clue_hamid", "Hamid", 3376, 3284, 0xE8C0A0, 0x8A6A3A, 0x5A3A2A, 0x3A6A3A, "CRYPTIC/HARD"),
            new ClueNpc("clue_head_chef", "Head chef", 3143, 3445, 0xC8A080, 0x3A6A9A, 0x2A2A2A, 0x5A3A6A, "CRYPTIC/HARD"),
            new ClueNpc("clue_horacio", "Horacio", 2635, 3310, 0xE0B090, 0xA08040, 0x3A2A1A, 0x5A3A6A, "CRYPTIC/ELITE"),
            new ClueNpc("clue_hunding", "Hunding", 3097, 3432, 0xE8C0A0, 0x8A2A2A, 0x2A2A2A, 0x8A2A2A, "CRYPTIC/BEGINNER"),
            new ClueNpc("clue_ironman_tutor", "Ironman tutor", 3227, 3227, 0xA07850, 0x3A6A3A, 0x2A2A2A, 0x8A2A2A, "CIPHER/MEDIUM"),
            new ClueNpc("clue_jaraah", "Jaraah", 3359, 3276, 0xE0B090, 0x3A6A3A, 0x3A2A1A, 0x8A6A3A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_jeed", "Jeed", 3367, 3221, 0xC8A080, 0x8A6A3A, 0x4A4A2A, 0x8A2A2A, "CRYPTIC/EASY"),
            new ClueNpc("clue_kamfreena", "Kamfreena", 2840, 3538, 0xC8A080, 0x6A4A2A, 0x2A2A2A, 0x6A4A2A, "CRYPTIC/ELITE"),
            new ClueNpc("clue_kaylee", "Kaylee", 2957, 3370, 0xA07850, 0x8A2A2A, 0x3A2A1A, 0x8A2A2A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_konoo", "Konoo", 1703, 3524, 0xE8C0A0, 0x3A6A3A, 0x4A4A2A, 0x8A2A2A, "CRYPTIC/EASY"),
            new ClueNpc("clue_lammy_langle", "Lammy Langle", 1688, 3540, 0xC8A080, 0x2A2A3A, 0x3A3A4A, 0x8A2A2A, "ANAGRAM/HARD"),
            new ClueNpc("clue_lisse_isaakson", "Lisse Isaakson", 2351, 3801, 0xA07850, 0x6A4A2A, 0x3A2A1A, 0x8A6A3A, "ANAGRAM/ELITE"),
            new ClueNpc("clue_louisa", "Louisa", 2736, 3578, 0xA07850, 0x8A2A2A, 0x3A2A1A, 0x8A2A2A, "CRYPTIC/EASY"),
            new ClueNpc("clue_lucy", "Lucy", 3046, 3382, 0xA07850, 0x3A6A3A, 0x3A3A4A, 0x5A3A6A, "CRYPTIC/EASY"),
            new ClueNpc("clue_luminata", "Luminata", 3508, 3237, 0xE0B090, 0x5A3A6A, 0x3A2A1A, 0x2A2A3A, "ANAGRAM/MASTER"),
            new ClueNpc("clue_madame_caldarium", "Madame Caldarium", 2553, 2868, 0xC8A080, 0x2A2A3A, 0x3A3A4A, 0x8A2A2A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_mandrith", "Mandrith", 3182, 3946, 0xE8C0A0, 0x6A4A2A, 0x3A2A1A, 0xA08040, "ANAGRAM/ELITE"),
            new ClueNpc("clue_martin_thwait", "Martin Thwait", 2906, 3537, 0xA07850, 0x3A6A3A, 0x3A2A1A, 0x3A6A9A, "ANAGRAM/HARD"),
            new ClueNpc("clue_mawrth", "Mawrth", 2333, 3165, 0xE8C0A0, 0x8A6A3A, 0x3A2A1A, 0x8A2A2A, "CRYPTIC/ELITE"),
            new ClueNpc("clue_metla", "Metla", 1742, 2977, 0xC8A080, 0xA08040, 0x3A2A1A, 0x3A6A3A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_new_recruit_tony", "New Recruit Tony", 1503, 3553, 0xC8A080, 0x3A6A3A, 0x4A4A2A, 0x5A3A6A, "ANAGRAM/MASTER"),
            new ClueNpc("clue_nicholas", "Nicholas", 1841, 3803, 0xC8A080, 0x3A6A9A, 0x5A3A2A, 0x8A6A3A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_nurse_wooned", "Nurse Wooned", 1511, 3619, 0xC8A080, 0x3A6A9A, 0x2A2A2A, 0x6A4A2A, "ANAGRAM/ELITE"),
            new ClueNpc("clue_old_man_ral", "Old Man Ral", 3602, 3209, 0xA07850, 0x3A6A3A, 0x3A3A4A, 0x8A6A3A, "ANAGRAM/MASTER"),
            new ClueNpc("clue_old_crone", "Old crone", 3462, 3557, 0xC8A080, 0x5A3A6A, 0x4A4A2A, 0xA08040, "ANAGRAM/ELITE"),
            new ClueNpc("clue_oneiromancer", "Oneiromancer", 2150, 3866, 0xE0B090, 0xA08040, 0x2A2A2A, 0xA08040, "ANAGRAM/ELITE"),
            new ClueNpc("clue_party_pete", "Party Pete", 3047, 3376, 0xC8A080, 0x3A6A9A, 0x3A3A4A, 0x6A4A2A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_piles", "Piles", 3186, 3936, 0xE8C0A0, 0xA08040, 0x3A2A1A, 0x3A6A9A, "CRYPTIC/MASTER"),
            new ClueNpc("clue_primula", "Primula", 2454, 2853, 0xE8C0A0, 0xA08040, 0x2A2A2A, 0xA08040, "ANAGRAM/MASTER"),
            new ClueNpc("clue_prospector_percy", "Prospector Percy", 3061, 3377, 0xA07850, 0x5A3A6A, 0x4A4A2A, 0x5A3A6A, "ANAGRAM/HARD"),
            new ClueNpc("clue_regath", "Regath", 1719, 3723, 0xE8C0A0, 0x8A2A2A, 0x4A4A2A, 0x3A6A9A, "ANAGRAM/ELITE"),
            new ClueNpc("clue_roavar", "Roavar", 3494, 3474, 0xC8A080, 0x2A2A3A, 0x5A3A2A, 0x5A3A6A, "CRYPTIC/MEDIUM"),
            new ClueNpc("clue_robin", "Robin", 3673, 3492, 0xE0B090, 0x3A6A3A, 0x5A3A2A, 0x3A6A9A, "CRYPTIC/MASTER"),
            new ClueNpc("clue_rusty", "Rusty", 2979, 3435, 0xE8C0A0, 0xA08040, 0x5A3A2A, 0x3A6A3A, "CRYPTIC/EASY"),
            new ClueNpc("clue_saba", "Saba", 2858, 3577, 0xE0B090, 0x8A6A3A, 0x5A3A2A, 0x6A4A2A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_sacrifice", "Sacrifice", 2209, 3056, 0xC8A080, 0x2A2A3A, 0x3A3A4A, 0x8A2A2A, "ANAGRAM/MASTER"),
            new ClueNpc("clue_saniboch", "Saniboch", 2745, 3151, 0xE0B090, 0x8A2A2A, 0x5A3A2A, 0x2A2A3A, "CRYPTIC/HARD"),
            new ClueNpc("clue_sarah", "Sarah", 3038, 3292, 0xA07850, 0x8A2A2A, 0x3A2A1A, 0x3A6A3A, "CRYPTIC/EASY"),
            new ClueNpc("clue_sherlock", "Sherlock", 2733, 3415, 0xA07850, 0x3A6A3A, 0x3A3A4A, 0x6A4A2A, "SHERLOCK/ELITE"),
            new ClueNpc("clue_sir_kay", "Sir Kay", 2760, 3496, 0xE0B090, 0x3A6A9A, 0x2A2A2A, 0x3A6A3A, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_soar_leader_pitri", "Soar Leader Pitri", 1559, 3045, 0xE8C0A0, 0x3A6A3A, 0x5A3A2A, 0xA08040, "CIPHER/HARD"),
            new ClueNpc("clue_squire_veteran", "Squire (Veteran)", 2638, 2656, 0xE8C0A0, 0x3A6A9A, 0x2A2A2A, 0x6A4A2A, "CRYPTIC/ELITE"),
            new ClueNpc("clue_teicuh", "Teicuh", 1212, 3119, 0xA07850, 0x8A2A2A, 0x3A2A1A, 0xA08040, "ANAGRAM/MEDIUM"),
            new ClueNpc("clue_the_face", "The Face", 3019, 3232, 0xE8C0A0, 0xA08040, 0x3A2A1A, 0x3A6A3A, "CRYPTIC/EASY"),
            new ClueNpc("clue_tool_leprechaun", "Tool Leprechaun", 3088, 3357, 0xE0B090, 0x8A6A3A, 0x4A4A2A, 0x6A4A2A, "CRYPTIC/EASY"),
            new ClueNpc("clue_uglug_nar", "Uglug Nar", 2444, 3049, 0xC8A080, 0x3A6A9A, 0x3A2A1A, 0x8A6A3A, "CRYPTIC/MEDIUM"),
            new ClueNpc("clue_ulizius", "Ulizius", 3444, 3461, 0xC8A080, 0x8A2A2A, 0x3A3A4A, 0x3A6A3A, "CRYPTIC/MEDIUM"),
            new ClueNpc("clue_weird_old_man", "Weird Old Man", 3224, 3112, 0xC8A080, 0xA08040, 0x4A4A2A, 0x3A6A3A, "CIPHER/HARD"),
            new ClueNpc("clue_wilough", "Wilough", 3221, 3435, 0xC8A080, 0xA08040, 0x3A3A4A, 0x8A2A2A, "CRYPTIC/HARD"),
            new ClueNpc("clue_wingstone", "Wingstone", 3389, 2877, 0xE0B090, 0x8A6A3A, 0x5A3A2A, 0xA08040, "ANAGRAM/MASTER"),
            new ClueNpc("clue_zoo_keeper", "Zoo keeper", 2613, 3269, 0xA07850, 0xA08040, 0x3A3A4A, 0x3A6A3A, "ANAGRAM/MEDIUM"));

    private ClueNpcs() {
    }

    /** Place id of the chunk an OSRS tile falls in. */
    public static String placeId(int osrsX, int osrsY) {
        int cx = GielinorMap.toBlockX(osrsX) >> 4;
        int cz = GielinorMap.toBlockZ(osrsY) >> 4;
        return "clue_spot_" + (cx < 0 ? "m" + -cx : String.valueOf(cx)) + "_" + (cz < 0 ? "m" + -cz : String.valueOf(cz));
    }

    public static List<PlaceDef> places() {
        Map<String, PlaceDef> places = new LinkedHashMap<>();
        for (ClueNpc n : ALL) {
            String id = placeId(n.x(), n.y());
            places.putIfAbsent(id, new PlaceDef(id, n.name(), n.x(), n.y(), PlaceDef.Style.NONE, List.of(), List.of()));
        }
        return new ArrayList<>(places.values());
    }

    public static List<NpcDef> npcs() {
        Map<String, PlaceDef> byId = new LinkedHashMap<>();
        for (PlaceDef p : places()) {
            byId.put(p.id(), p);
        }
        List<NpcDef> list = new ArrayList<>();
        for (ClueNpc n : ALL) {
            PlaceDef p = byId.get(placeId(n.x(), n.y()));
            int dx = GielinorMap.toBlockX(n.x()) - p.blockX();
            int dz = GielinorMap.toBlockZ(n.y()) - p.blockZ();
            list.add(NpcDef.of(n.id(), n.name(), p.id(), dx, dz, n.skin(), n.top(), n.bottom(), n.hat()));
        }
        return list;
    }
}
