package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every pet in RuneCraft with its OSRS source and drop rate. Boss and raid pets already defined by
 * other modules are only referenced here (for the "All Pets" collection log page, the follower's
 * look and the exact engine roll); this module defines the skilling pets and the few others that
 * were missing (Herbi, Chompy chick).
 *
 * <p>Boss pets: 1/{@code rate} per kill. Skilling pets: {@code 1/(base - level * 25)} per action,
 * with the base chance depending on the activity ({@link SkillingPets}).
 */
public final class Pets {
    public enum Source { BOSS, RAID, MINIGAME, SKILLING, OTHER }

    /**
     * A pet. {@code sources} are creature/activity ids (boss ids for boss pets), {@code rates} the
     * matching 1/x chances (0 = rolled by another module's own mechanic, see {@code note}).
     * {@code look} is a model spec id (a boss's model, or one of this module's pet models) and
     * {@code lookHeight} the unscaled height in blocks of that model's creature (used to shrink it).
     */
    public record PetDef(String id, String name, Source source, List<String> sources, List<Integer> rates, Skill skill,
                         String look, float lookHeight, boolean defined, int color, String note) {
        /** Render scale so the follower is about {@code target} blocks tall. */
        public float renderScale(float target) {
            return Math.max(0.08f, Math.min(1.0f, target / Math.max(0.3f, lookHeight)));
        }
    }

    private static final List<PetDef> ALL = new ArrayList<>();
    private static final Map<String, PetDef> BY_ID = new LinkedHashMap<>();
    /** Boss id -> 1/x pet rate rolled by the generic boss engine. */
    private static final Map<String, Integer> ENGINE = new LinkedHashMap<>();

    /** Rate the generic boss engine used before this module (still used for bosses not listed here). */
    public static final int DEFAULT_ENGINE_RATE = 3000;

    private static void boss(String id, String name, String look, float lookHeight, Object... bossRates) {
        add(id, name, Source.BOSS, look, lookHeight, false, 0, null, null, bossRates);
    }

    private static void add(String id, String name, Source source, String look, float lookHeight, boolean defined, int color,
                            Skill skill, String note, Object... sourceRates) {
        List<String> sources = new ArrayList<>();
        List<Integer> rates = new ArrayList<>();
        for (int i = 0; i < sourceRates.length; i += 2) {
            sources.add((String) sourceRates[i]);
            rates.add((Integer) sourceRates[i + 1]);
        }
        PetDef p = new PetDef(id, name, source, List.copyOf(sources), List.copyOf(rates), skill, look, lookHeight, defined, color, note);
        ALL.add(p);
        BY_ID.put(id, p);
    }

    static {
        // --- God Wars Dungeon and Wilderness (godwars module; its scripts top up the 1/3000 engine roll where
        // the real rate is better, so those keep the default engine rate) ---
        boss("pet_general_graardor", "Pet general graardor", "general_graardor", 1.95f, "general_graardor", 5000);
        boss("pet_kril_tsutsaroth", "Pet k'ril tsutsaroth", "kril_tsutsaroth", 1.95f, "kril_tsutsaroth", 5000);
        boss("pet_kreearra", "Pet kree'arra", "kreearra", 2.2f, "kreearra", 5000);
        boss("pet_zilyana", "Pet zilyana", "commander_zilyana", 2.0f, "commander_zilyana", 5000);
        boss("nexling", "Nexling", "nex", 2.2f, "nex", 500);
        boss("callisto_cub", "Callisto cub", "callisto", 1.4f, "callisto", 1500, "artio", 2800);
        boss("pet_venenatis_spiderling", "Venenatis spiderling", "venenatis_spiderling", 0.6f, "venenatis", 1500, "spindel", 2800);
        boss("vetion_jr", "Vet'ion jr.", "vetion", 2.2f, "vetion", 1500, "calvarion", 2800);
        boss("pet_scorpias_offspring", "Scorpia's offspring", "scorpias_offspring", 0.6f, "scorpia", 2016);
        boss("pet_chaos_elemental", "Pet chaos elemental", "chaos_elemental", 2.0f, "chaos_elemental", 300, "chaos_fanatic", 1000);
        boss("prince_black_dragon", "Prince black dragon", "king_black_dragon", 1.6f, "king_black_dragon", 3000);
        // --- Bosses module ---
        boss("pet_baby_mole", "Baby mole", "giant_mole", 1.2f, "giant_mole", 3000);
        boss("pet_kalphite_princess", "Kalphite princess", "kalphite_queen", 1.6f, "kalphite_queen", 3000);
        boss("pet_dagannoth_rex", "Pet dagannoth rex", "dagannoth_rex", 2.2f, "dagannoth_rex", 5000);
        boss("pet_dagannoth_prime", "Pet dagannoth prime", "dagannoth_prime", 2.2f, "dagannoth_prime", 5000);
        boss("pet_dagannoth_supreme", "Pet dagannoth supreme", "dagannoth_supreme", 2.2f, "dagannoth_supreme", 5000);
        boss("pet_sraracha", "Sraracha", "sarachnis", 1.2f, "sarachnis", 3000);
        boss("pet_dark_core", "Pet dark core", "dark_energy_core", 0.8f, "corporeal_beast", 5000);
        // Skotos is rolled 1/65 by the Skotizo script; the engine roll is switched off for it (rate 0).
        boss("pet_skotos", "Skotos", "skotizo", 2.0f, "skotizo", 0);
        boss("pet_scurry", "Scurry", "scurrius", 1.2f, "scurrius", 3000);
        boss("pet_snakeling", "Pet snakeling", "snakeling", 1.0f, "zulrah", 4000);
        boss("pet_vorki", "Vorki", "vorkath", 1.6f, "vorkath", 3000);
        boss("pet_kraken", "Pet kraken", "kraken", 1.5f, "kraken", 3000);
        boss("pet_smoke_devil", "Pet smoke devil", "thermonuclear_smoke_devil", 1.6f, "thermonuclear_smoke_devil", 3000);
        boss("pet_hellpuppy", "Hellpuppy", "cerberus", 1.4f, "cerberus", 3000);
        boss("pet_abyssal_orphan", "Abyssal orphan", "abyssal_sire", 2.2f, "abyssal_sire", 2560);
        boss("pet_ikkle_hydra", "Ikkle hydra", "alchemical_hydra", 2.0f, "alchemical_hydra", 3000);
        boss("pet_noon", "Noon", "dusk", 2.0f, "dusk", 3000);
        boss("pet_nid", "Nid", "araxxor", 1.4f, "araxxor", 3000);
        boss("pet_youngllef", "Youngllef", "crystalline_hunllef", 2.0f, "crystalline_hunllef", 2000, "corrupted_hunllef", 800);
        boss("pet_smolcano", "Smolcano", "zalcano", 2.4f, "zalcano", 2250);
        // The Nightmare: 1/800 a player, 1/4,000 from five players (the engine rolls 1/4,000, its script tops it up); Phosani 1/1,400.
        boss("pet_little_nightmare", "Little nightmare", "the_nightmare", 2.4f, "the_nightmare", 4000, "phosanis_nightmare", 1400);
        boss("pet_muphin", "Muphin", "phantom_muspah", 2.4f, "phantom_muspah", 2500);
        boss("pet_smol_heredit", "Smol heredit", "sol_heredit", 2.2f, "sol_heredit", 200); // approx: per wave-12 completion
        boss("pet_baron", "Baron", "duke_sucellus", 2.4f, "duke_sucellus", 2500);
        boss("pet_lilviathan", "Lil'viathan", "the_leviathan", 2.0f, "the_leviathan", 2500);
        boss("pet_wisp", "Wisp", "the_whisperer", 2.4f, "the_whisperer", 2000);
        boss("pet_butch", "Butch", "vardorvis", 2.2f, "vardorvis", 3000);
        boss("pet_huberte", "Huberte", "the_hueycoatl", 2.6f, "the_hueycoatl", 400);
        boss("pet_moxi", "Moxi", "amoxliatl", 1.4f, "amoxliatl", 3000);
        boss("pet_bran", "Bran", "branda_the_fire_queen", 2.6f, "branda_the_fire_queen", 3000);
        boss("pet_yami", "Yami", "yama", 3.4f, "yama", 2500);
        boss("pet_dom", "Dom", "doom_of_mokhaiotl", 2.4f, "doom_of_mokhaiotl", 250); // delve 9+ rate
        boss("pet_gull", "Gull", "shellbane_gryphon", 2.0f, "shellbane_gryphon", 3000); // approx
        boss("pet_beef", "Beef", "brutus", 1.8f, "brutus", 1000, "demonic_brutus", 400);
        boss("pet_maggot_marquess", "Maggot marquess", "maggot_king", 2.6f, "maggot_king", 3500);
        boss("tzrek_jad", "TzRek-Jad", "tztok_jad", 1.8f, "tztok_jad", 200);
        // --- Raids (raids module rolls these from its own reward logic) ---
        add("olmlet", "Olmlet", Source.RAID, "great_olm", 2.6f, false, 0, null, "1/53 of a Chambers of Xeric purple", "great_olm", 0);
        add("lil_zik", "Lil' Zik", Source.RAID, "verzik_vitur_p3", 2.6f, false, 0, null, "1/650 per Theatre of Blood completion",
                "verzik_vitur", 0);
        add("tumekens_guardian", "Tumeken's guardian", Source.RAID, "tumekens_warden", 2.6f, false, 0, null,
                "Tombs of Amascut completion (rate scales with raid level)", "tumekens_warden", 0);
        // --- Minigames (minigames module) ---
        add("jal_nib_rek", "Jal-nib-rek", Source.MINIGAME, "ext_pet_nibbler", 0.7f, false, 0, null, "1/100 per Inferno completion",
                "tzkal_zuk", 0);
        add("phoenix", "Phoenix", Source.MINIGAME, "ext_pet_phoenix", 0.7f, false, 0, null, "1/5000 per Wintertodt reward roll",
                "wintertodt", 0);
        add("tiny_tempor", "Tiny tempor", Source.MINIGAME, "ext_pet_tempor", 0.6f, false, 0, null, "1/8000 per Tempoross reward roll",
                "tempoross", 0);
        add("abyssal_protector", "Abyssal protector", Source.MINIGAME, "ext_pet_rift_guardian", 0.7f, false, 0, null,
                "1/4000 per Guardians of the Rift search", "guardians_of_the_rift", 0);
        add("pet_penance_queen", "Pet penance queen", Source.MINIGAME, "penance_queen", 2.4f, false, 0, null,
                "1/1000 per Barbarian Assault high-level gamble", "barbarian_assault", 0);
        // approx look: the rift guardian's floating spirit stands in for a tiny Avatar of Creation.
        add("lil_creator", "Lil' creator", Source.MINIGAME, "ext_pet_rift_guardian", 0.7f, false, 0, null,
                "1/400 per spoils of war opened", "soul_wars", 0);
        add("bloodhound", "Bloodhound", Source.OTHER, "ext_pet_bloodhound", 0.7f, false, 0, null, "1/1000 per master clue casket",
                "clue_scroll_master", 0);
        // --- Skilling pets (this module) ---
        add("pet_heron", "Heron", Source.SKILLING, "ext_pet_heron", 0.8f, true, 0x8A9AA8, Skill.FISHING, null);
        add("pet_rock_golem", "Rock golem", Source.SKILLING, "ext_pet_rock_golem", 0.8f, true, 0x7A7A7A, Skill.MINING, null);
        add("pet_beaver", "Beaver", Source.SKILLING, "ext_pet_beaver", 0.6f, true, 0x7A5A3A, Skill.WOODCUTTING, null);
        add("pet_baby_chinchompa", "Baby chinchompa", Source.SKILLING, "ext_pet_chinchompa", 0.5f, true, 0x9A9A9A, Skill.HUNTER, null);
        add("pet_giant_squirrel", "Giant squirrel", Source.SKILLING, "ext_pet_squirrel", 0.7f, true, 0xA0603A, Skill.AGILITY, null);
        add("pet_tangleroot", "Tangleroot", Source.SKILLING, "ext_pet_tangleroot", 0.7f, true, 0x4A7A2A, Skill.FARMING, null);
        add("pet_rocky", "Rocky", Source.SKILLING, "ext_pet_rocky", 0.6f, true, 0x6A6A6A, Skill.THIEVING, null);
        add("pet_rift_guardian", "Rift guardian", Source.SKILLING, "ext_pet_rift_guardian", 0.7f, true, 0xC03030, Skill.RUNECRAFT,
                null);
        add("pet_herbi", "Herbi", Source.SKILLING, "ext_pet_herbi", 0.6f, true, 0x6A5A3A, Skill.HUNTER, null, "herbiboar", 6500);
        add("pet_chompy_chick", "Chompy chick", Source.OTHER, "ext_pet_chompy", 0.6f, true, 0x8A9A5C, null, null, "chompy_bird", 500);

        for (PetDef p : ALL) {
            if (p.source() == Source.BOSS) {
                for (int i = 0; i < p.sources().size(); i++) {
                    ENGINE.put(p.sources().get(i), p.rates().get(i));
                }
            }
        }
        // Bosses whose scripts top up the engine roll to reach their real rate: keep the engine at 1/3000.
        for (String id : List.of("nex", "callisto", "artio", "venenatis", "spindel", "vetion", "calvarion", "scorpia",
                "chaos_elemental", "chaos_fanatic")) {
            ENGINE.put(id, DEFAULT_ENGINE_RATE);
        }
        // Built-in generals and Jad are rolled outside the data boss engine (GwdKillcount / Jad's drop table).
        ENGINE.remove("general_graardor");
        ENGINE.remove("kril_tsutsaroth");
        ENGINE.remove("tztok_jad");
    }

    private Pets() {
    }

    /**
     * Metamorphosis forms (the OSRS "Metamorphosis" option): another look of a pet, a follower and item of its own but no
     * collection log entry. JalRek-Jad needs every TzHaar-Ket-Rak's challenge completed; TzRek-Zuk is free.
     */
    private static final Map<String, PetDef> VARIANTS = new LinkedHashMap<>();
    private static final Map<String, String> VARIANT_OF = new LinkedHashMap<>();

    private static void variant(String id, String name, String look, float lookHeight, String base) {
        PetDef b = BY_ID.get(base);
        VARIANTS.put(id, new PetDef(id, name, b.source(), b.sources(), Collections.nCopies(b.sources().size(), 0), b.skill(), look,
                lookHeight, false, b.color(), "Metamorphosis of " + b.name()));
        VARIANT_OF.put(id, base);
        VARIANT_OF.put(base, id);
    }

    static {
        variant("jalrek_jad", "JalRek-Jad", "jaltok_jad", 3.2f, "tzrek_jad");
        variant("tzrek_zuk", "TzRek-Zuk", "tzkal_zuk", 3.5f, "jal_nib_rek");
    }

    public static List<PetDef> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static PetDef get(String id) {
        PetDef p = BY_ID.get(id);
        return p != null ? p : VARIANTS.get(id);
    }

    public static boolean isPet(String itemId) {
        return BY_ID.containsKey(itemId) || VARIANTS.containsKey(itemId);
    }

    /** The other form of a pet with a metamorphosis (TzRek-Jad and JalRek-Jad, Jal-nib-rek and TzRek-Zuk), or null. */
    public static String metamorphosis(String petId) {
        return VARIANT_OF.get(petId);
    }

    /** Whether a pet id is a metamorphosis form rather than the pet itself. */
    public static boolean isVariant(String petId) {
        return VARIANTS.containsKey(petId);
    }

    /** Pets this module defines as items. */
    public static List<PetDef> defined() {
        return ALL.stream().filter(PetDef::defined).toList();
    }

    /**
     * 1/x pet roll for the generic data-boss engine. 0 means "don't roll" (the boss's script rolls
     * it). Unlisted bosses keep {@link #DEFAULT_ENGINE_RATE}.
     */
    public static int engineRate(String bossId) {
        return ENGINE.getOrDefault(bossId, DEFAULT_ENGINE_RATE);
    }

    /** Pet of a creature/activity source id (boss id, "herbiboar", "chompy_bird"), or null. */
    public static PetDef bySource(String sourceId) {
        for (PetDef p : ALL) {
            if (p.sources().contains(sourceId)) {
                return p;
            }
        }
        return null;
    }

    /** Real 1/x rate for a pet from a given source, or 0 if unknown / rolled elsewhere. */
    public static int rate(String petId, String sourceId) {
        PetDef p = BY_ID.get(petId);
        if (p == null) {
            return 0;
        }
        int i = p.sources().indexOf(sourceId);
        return i < 0 ? 0 : p.rates().get(i);
    }

    /** OSRS skilling pet chance: 1 / (base - level * 25). */
    public static double skillingChance(int base, int level) {
        int denom = base - Math.max(1, Math.min(99, level)) * 25;
        return 1.0 / Math.max(1, denom);
    }

    /** Ids for the "All Pets" collection log page, in log order (bosses, raids, minigames, skilling, other). */
    public static List<String> logOrder() {
        List<String> out = new ArrayList<>();
        for (Source s : Source.values()) {
            for (PetDef p : ALL) {
                if (p.source() == s) {
                    out.add(p.id());
                }
            }
        }
        return out;
    }
}
