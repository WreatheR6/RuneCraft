package com.runecraft.osrs;

import java.util.Locale;
import java.util.Set;

/**
 * The race of a humanoid NPC, from its id, name and home. It picks the generated skin's anatomy (beards, ears,
 * rock skin) in tools/gen_assets.py (via the catalog dump) and the client render proportions. Hitboxes stay the same.
 */
public enum NpcRace {
    HUMAN(1.0f, 1.0f),
    /** OSRS dwarves stand about chest-high to a human and are stocky. */
    DWARF(0.84f, 0.70f),
    /** OSRS gnomes are about waist-high and slight. */
    GNOME(0.62f, 0.60f),
    /** Elves are a little taller and slimmer than humans. */
    ELF(0.94f, 1.07f),
    /** TzHaar: rock-skinned, human-sized. */
    TZHAAR(1.04f, 1.04f);

    private final float width;
    private final float height;

    NpcRace(float width, float height) {
        this.width = width;
        this.height = height;
    }

    /** Render scale across (x and z). */
    public float width() {
        return width;
    }

    /** Render scale upwards (y). */
    public float height() {
        return height;
    }

    private static final Set<String> DWARVES = Set.of("doric", "thurgo", "rowdy_dwarf", "gauss", "kjut",
            "commander_veldaban", "vermundi", "dromund", "santiri", "riki_the_sculptor", "quest_gd_secretary",
            "quest_gd_director", "rohak", "dondakan", "engineer_rolad", "dwarven_engineer", "npc_drogo",
            "npc_cart_conductor_keldagrim", "npc_cart_conductor_ice_mountain", "npc_keldagrim_stonemason", "nulodion",
            "npc_nurmof", "nurmof", "hura", "boot", "npc_hura");
    private static final Set<String> GNOMES = Set.of("king_bolren", "golrie", "elkoy", "king_narnode", "glough", "femi",
            "gardener_prissy_scilla", "gardener_bolongo", "gardener_gileth", "mg_aluft_gianne", "brimstail", "hazelmere",
            "npc_hazelmere");
    private static final Set<String> ELVES = Set.of("arianwyn", "oronwen", "lord_iorwerth", "baxtorian_elf", "islwyn",
            "eluned", "gardener_liliwen", "gardener_oswallt", "glarial", "elena_elf", "lady_trahaearn", "lord_crwys",
            "lord_cadarn", "lord_meilyr", "lord_hefin", "lord_amlodd", "lord_ithell");
    /** Characters whose id or home suggests a race they don't have (humans in the Gnome Stronghold, objects). */
    private static final Set<String> HUMANS = Set.of("nieve", "steve", "charlie_prisoner", "oracle", "willow_bim");

    /** The race of a character, from its id, display name and place (a Landmark name or PlaceDef id; may be null). */
    public static NpcRace of(String id, String name, String place) {
        String key = id == null ? "" : id.toLowerCase(Locale.ROOT);
        if (HUMANS.contains(key) || key.contains("spirit_tree") || key.contains("canoe_station")
                || key.contains("leprechaun")) {
            return HUMAN;
        }
        if (DWARVES.contains(key)) {
            return DWARF;
        }
        if (GNOMES.contains(key)) {
            return GNOME;
        }
        if (ELVES.contains(key)) {
            return ELF;
        }
        String text = " " + (key + " " + (name == null ? "" : name)).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ") + " ";
        String home = place == null ? "" : place.toLowerCase(Locale.ROOT);
        if (text.contains(" tzhaar ")) {
            return TZHAAR;
        }
        if (text.contains(" dwarf ") || text.contains(" dwarven ") || text.contains(" dwarves ")
                || home.startsWith("keldagrim")) {
            return DWARF;
        }
        if (text.contains(" gnome ") || text.contains(" gnomes ")) {
            return GNOME;
        }
        if (text.contains(" elf ") || text.contains(" elves ") || text.contains(" elven ") || home.equals("lletya")
                || key.contains("lletya") || home.startsWith("prifddinas") || home.equals("elf_camp")) {
            return ELF;
        }
        return HUMAN;
    }
}
