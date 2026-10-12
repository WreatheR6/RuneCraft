package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Boss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Boss tasks ("Like a Boss", 200 points): Konar, Nieve, Steve and Duradel may assign any of the bosses
 * below (never the Corporeal Beast, Nex, the Nightmare or Yama), Krystilia only the Wilderness ones (Callisto,
 * the Chaos Elemental and Fanatic, the Crazy archaeologist, Scorpia, Venenatis and Vet'ion; the King Black
 * Dragon's lair is not in the Wilderness). A boss task is drawn from the masters' "bosses" line (weight
 * 8, Duradel 12), then one boss is picked with equal chance; each boss is its own task category
 * ({@code boss_<id>}) that counts kills of the listed creature ids (the boss's final form). In OSRS the
 * player picks the amount (3-35); here it is rolled in that range (approx).
 */
public final class SlayerBossTasks {
    /** The meta category on masters' tables that turns into one of the boss tasks. */
    public static final String CATEGORY = "bosses";
    public static final String UNLOCK = "like_a_boss";

    /**
     * @param id          task category id ({@code boss_<id>})
     * @param creatures   module boss or built-in boss ids whose kills count
     * @param slayerLevel Slayer level the boss itself needs (Sire 85, Cerberus 91...)
     * @param wilderness  Krystilia may assign it
     */
    public record BossTask(String id, String name, List<String> creatures, int slayerLevel, boolean wilderness, Region region,
                           int min, int max) {
    }

    private static final Map<String, BossTask> ALL = new LinkedHashMap<>();

    private static void b(String key, String name, int slayer, boolean wildy, Region region, String... creatures) {
        String id = "boss_" + key;
        ALL.put(id, new BossTask(id, name, List.of(creatures), slayer, wildy, region, 3, 35));
    }

    static {
        b("abyssal_sire", "Abyssal Sire", 85, false, Region.MISTHALIN, "abyssal_sire");
        b("alchemical_hydra", "Alchemical Hydra", 95, false, Region.UNCHARTED_ISLES, "alchemical_hydra");
        b("araxxor", "Araxxor", 92, false, Region.MORYTANIA, "araxxor");
        b("callisto", "Callisto and Artio", 1, true, Region.WILDERNESS, "callisto", "artio");
        b("cerberus", "Cerberus", 91, false, Region.ASGARNIA, "cerberus");
        b("chaos_elemental", "Chaos Elemental", 1, true, Region.WILDERNESS, "chaos_elemental");
        b("chaos_fanatic", "Chaos Fanatic", 1, true, Region.WILDERNESS, "chaos_fanatic");
        b("commander_zilyana", "Commander Zilyana", 1, false, Region.TROLL_COUNTRY, "commander_zilyana");
        b("crazy_archaeologist", "Crazy Archaeologist", 1, true, Region.WILDERNESS, "crazy_archaeologist");
        b("dagannoth_kings", "Dagannoth Kings", 1, false, Region.FREMENNIK_PROVINCE, "dagannoth_rex", "dagannoth_prime",
                "dagannoth_supreme");
        b("duke_sucellus", "Duke Sucellus", 1, false, Region.FREMENNIK_PROVINCE, "duke_sucellus");
        b("general_graardor", "General Graardor", 1, false, Region.TROLL_COUNTRY, "general_graardor");
        b("giant_mole", "Giant Mole", 1, false, Region.ASGARNIA, "giant_mole");
        b("grotesque_guardians", "Grotesque Guardians", 75, false, Region.MORYTANIA, "dusk");
        b("kalphite_queen", "Kalphite Queen", 1, false, Region.KHARIDIAN_DESERT, "kalphite_queen");
        b("king_black_dragon", "King Black Dragon", 1, false, Region.WILDERNESS, "king_black_dragon");
        b("kraken", "Kraken", 87, false, Region.KANDARIN, "kraken");
        b("kreearra", "Kree'arra", 1, false, Region.TROLL_COUNTRY, "kreearra");
        b("kril_tsutsaroth", "K'ril Tsutsaroth", 1, false, Region.TROLL_COUNTRY, "kril_tsutsaroth");
        b("phantom_muspah", "Phantom Muspah", 1, false, Region.FREMENNIK_PROVINCE, "phantom_muspah");
        b("sarachnis", "Sarachnis", 1, false, Region.KANDARIN, "sarachnis");
        b("scorpia", "Scorpia", 1, true, Region.WILDERNESS, "scorpia");
        b("the_leviathan", "The Leviathan", 1, false, Region.KHARIDIAN_DESERT, "the_leviathan");
        b("the_whisperer", "The Whisperer", 1, false, Region.MISTHALIN, "the_whisperer");
        b("thermonuclear_smoke_devil", "Thermonuclear smoke devil", 93, false, Region.KANDARIN, "thermonuclear_smoke_devil");
        b("vardorvis", "Vardorvis", 1, false, Region.MORYTANIA, "vardorvis");
        b("venenatis", "Venenatis and Spindel", 1, true, Region.WILDERNESS, "venenatis", "spindel");
        b("vetion", "Vet'ion and Calvar'ion", 1, true, Region.WILDERNESS, "vetion", "calvarion");
        b("vorkath", "Vorkath", 1, false, Region.FREMENNIK_PROVINCE, "vorkath");
        b("zulrah", "Zulrah", 1, false, Region.TIRANNWN, "zulrah");
    }

    private SlayerBossTasks() {
    }

    public static List<BossTask> all() {
        return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
    }

    /** The boss task with this category id, or null. */
    public static BossTask get(String category) {
        return category == null ? null : ALL.get(category);
    }

    /** True when killing {@code creatureId} counts towards the boss task {@code category}. */
    public static boolean counts(String category, String creatureId) {
        BossTask t = get(category);
        return t != null && creatureId != null && t.creatures().contains(creatureId);
    }

    /** True if at least one of the task's bosses exists in the game. */
    public static boolean exists(BossTask t) {
        for (String c : t.creatures()) {
            BossDef def = ContentRegistry.boss(c);
            if (def != null || Boss.byEntity(c) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks one boss task with equal chance among those the player can do: present in the game, Slayer
     * level met, not blocked, not the previous task, Wilderness-only for Krystilia. Null when none qualify.
     */
    public static BossTask pick(Random random, int slayerLevel, boolean wildernessOnly, java.util.Collection<String> blocked,
                                String previous) {
        List<BossTask> ok = choices(slayerLevel, wildernessOnly, blocked, previous);
        return ok.isEmpty() ? null : ok.get(random.nextInt(ok.size()));
    }

    public static List<BossTask> choices(int slayerLevel, boolean wildernessOnly, java.util.Collection<String> blocked,
                                         String previous) {
        List<BossTask> ok = new ArrayList<>();
        for (BossTask t : ALL.values()) {
            if (slayerLevel >= t.slayerLevel() && (!wildernessOnly || t.wilderness()) && !blocked.contains(t.id())
                    && !t.id().equals(previous) && exists(t)) {
                ok.add(t);
            }
        }
        return ok;
    }
}
